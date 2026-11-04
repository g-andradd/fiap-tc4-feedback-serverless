# Design: add-registro-avaliacao

## Context

Este change cria a primeira função do projeto, `registrar-avaliacao`, atrás de `POST /avaliacao`, e fixa três coisas que os changes seguintes reutilizam: o contrato do item na tabela, o bloco `Globals` do template e as regras de SnapStart e de logs. A fundação já entrega a tabela e o índice `por-data` (fundação D6), o bootstrap com boundary e roles de deploy (fundação D4 e D9) e os módulos Maven vazios (fundação D8).

Nenhum módulo tem código ainda e o bootstrap não foi aplicado na conta. Nada abaixo foi testado na AWS.

**Fontes.** Salvo indicação em contrário, as páginas citadas foram consultadas em 03/11/2026. As marcadas com (04/11/2026) foram consultadas depois da meia-noite. "Verificado localmente" significa executado em uma pasta temporária com o SAM CLI 1.171.0 e JDK 25, fora do repositório: `sam validate --lint`, o transform do SAM (`samtranslator`) e `sam build`. Isso prova o template gerado, não o comportamento na conta.

## Goals / Non-Goals

**Goals**
- Registrar avaliação válida e rejeitar entrada inválida exatamente como a spec descreve, inclusive nos limites
- Fixar o contrato do item para `add-notificacao-critica` e `add-relatorio-semanal`
- Deixar `Globals` definitivo, para os outros dois changes só acrescentarem recursos
- Primeira prova prática de SnapStart com `java25` em `sa-east-1` (fundação D7)

**Non-Goals**
- Autenticação, consulta, alteração e exclusão de avaliações
- Deduplicação e idempotência
- Alarmes e dashboard, que ficam em `add-observabilidade-seguranca`
- Otimização de cold start além do SnapStart, como priming com chamadas reais

## Decisions

### D1. HTTP API
A entrada usa HTTP API (`AWS::Serverless::HttpApi`), com payload 2.0 e estágio `$default`.

- **Requisitos:** a [comparação oficial](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-vs-rest.html) lista como exclusivos de REST API chaves de API, limite por cliente, validação de requisição, WAF e tracing X-Ray. Nenhum é exigido aqui: autenticação está fora do escopo e a validação pela spec (`7.0` e `1e1` rejeitados) fica no domínio de qualquer forma.
- **Custo:** a [página de preços](https://aws.amazon.com/api-gateway/pricing/) dá 1 milhão de chamadas por mês nos dois tipos, por até 12 meses e só para clientes novos, e usa nos exemplos US$ 1,00 por milhão em HTTP API contra US$ 3,50 em REST API. A página não mostra o preço de São Paulo. O volume da demonstração torna a diferença irrelevante, então o preço pesa menos que a simplicidade.
- **Deny por tag do bootstrap:** a negação cobre as escritas de `apigateway` em `/*`, então vale para os dois tipos. Verificado localmente: o SAM põe `Tags` no corpo OpenAPI da API e no `AWS::ApiGatewayV2::Stage`. Declarar `ambiente` explicitamente fecha, para esta API, o risco registrado na fundação de API sem tag ficar fora da negação. A tarefa 6.5 confere a tag na conta.
- **Freio de custo:** como o endpoint é público e sem autenticação, `DefaultRouteSettings` limita a 10 requisições por segundo com rajada de 20. O excesso recebe 429 do API Gateway, sem invocar a função.

O payload precisa ser declarado: por CloudFormation, `payloadFormatVersion` é obrigatório ([integração Lambda](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-develop-integrations-lambda.html)).

Alternativa descartada: REST API. Custa mais e traz recursos que o escopo não usa. O único ganho real seria o X-Ray a partir do gateway, e o `project.md` pede X-Ray nas funções, o que `Tracing: Active` já entrega.

### D2. Contrato do item
Item único, sem atributos além destes:

| Atributo | Tipo DynamoDB | Formato | Origem |
|---|---|---|---|
| `id` | S | UUID aleatório em texto | gerado no handler |
| `descricao` | S | como recebida, sem remover espaços | cliente |
| `nota` | N | inteiro de 0 a 10 | cliente |
| `urgencia` | S | `CRITICA`, `MEDIA` ou `BAIXA` | calculada no registro |
| `dataEnvio` | S | `yyyy-MM-dd` em America/Sao_Paulo | derivada de `criadoEm` |
| `criadoEm` | S | `yyyy-MM-ddTHH:mm:ss.SSSZ` em UTC, sempre 24 caracteres | relógio do handler |

`criadoEm` e `dataEnvio` saem do mesmo `Instant`, capturado uma vez por requisição.

**Por que largura fixa.** `criadoEm` é a chave de ordenação do índice e o DynamoDB ordena texto por bytes. `Instant.toString()` omite a fração quando ela é zero, e `...:59Z` ordena depois de `...:59.123Z`, porque `Z` vem depois de `.` na tabela de caracteres. Logo, a ordem deixaria de ser cronológica. O padrão usa `DateTimeFormatter` com `uuuu-MM-dd'T'HH:mm:ss.SSS'Z'` em UTC e trunca a fração em milissegundos. O instante `2026-11-10T02:59:59Z` da spec vira `2026-11-10T02:59:59.000Z`, que representa o mesmo instante em ISO-8601.

Alternativas descartadas:
- Offset de São Paulo em `criadoEm` (`-03:00`). A ordem só seria cronológica enquanto o offset não mudar. UTC não depende disso.
- `Instant.toString()`, pelo motivo acima.
- Epoch numérico. A fundação declarou `criadoEm` como S no índice, e epoch não é ISO-8601 como pede a spec.

### D3. Domínio em `functions/dominio`
Pacote `br.com.fiap.feedback.dominio`, sem SDK da AWS e sem Jackson:

- `Urgencia`: enum com `deNota(int)`, a única tabela de classificação
- `Avaliacao`: record com os seis atributos, `Avaliacao.nova(descricao, nota, id, instante)` e `Avaliacao.deAtributos(Map<String, String>)`, que lança `ItemInvalidoException` com os nomes dos campos faltantes, nunca os valores
- `AtributosAvaliacao`: constantes com os nomes dos atributos
- `CarimboDeTempo`: `ZONA_SAO_PAULO`, `criadoEm(Instant)` e `dataEnvio(Instant)`
- `ValorEntrada`: tipo selado com `Ausente`, `Texto(String)`, `Inteiro(BigInteger)` e `Outro`
- `ValidadorAvaliacao`: recebe dois `ValorEntrada` e devolve a lista de campos inválidos

`deAtributos` recebe texto porque as duas funções leitoras usam tipos diferentes de `AttributeValue`: a notificação usa o de `aws-lambda-java-events` e o relatório, o do SDK. Cada adaptador converte para `Map<String, String>` e o contrato fica em um lugar só.

`id` e `instante` chegam como parâmetro: o domínio continua determinístico nos testes, e a geração fica no handler, como D7 exige.

Alternativa descartada: mapear o item em cada função. Duplicaria o contrato e a validação nos dois leitores.

### D4. Leitura e validação da entrada
O handler é `RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse>`:

1. Corpo nulo ou vazio é corpo inválido. Se `isBase64Encoded` for verdadeiro, decodifica antes; falha na decodificação também é corpo inválido.
2. `ObjectMapper.readTree` com `FAIL_ON_TRAILING_TOKENS` ligado, para `{"a":1} xx` não passar, e sem `INCLUDE_SOURCE_IN_LOCATION`, para a mensagem da exceção não carregar o corpo. Nó que não seja objeto é corpo inválido.
3. O adaptador converte cada campo em `ValorEntrada`: nó ausente ou `null` vira `Ausente`, texto vira `Texto`, `isIntegralNumber()` vira `Inteiro`, o resto vira `Outro`. `7.0` e `1e1` chegam ao Jackson como ponto flutuante e viram `Outro`. `99999999999999999999` vira `Inteiro` com `BigInteger` e reprova na faixa, sem estouro.
4. `ValidadorAvaliacao` aplica a spec. `descricao` precisa ter ao menos um caractere fora de espaço, tabulação, `\n` e `\r`, e no máximo 1000 pontos de código (`codePointCount`). `String.isBlank()` não serve, porque considera brancos outros caracteres Unicode que a spec não lista.

Campos extras não são lidos. O cliente não consegue influenciar `id`, `urgencia`, `dataEnvio` nem `criadoEm`.

Alternativa descartada: desserializar em um record com `int nota`. O Jackson faz coerções como `"7"` e `7.0` para inteiro conforme a configuração, e a spec precisa distinguir esses casos.

### D5. Respostas
Todas com `content-type: application/json`:

| Caso | Status | Corpo |
|---|---|---|
| Sucesso | 201 | `{"id":"...","urgencia":"MEDIA"}` |
| Campo inválido | 400 | `{"erro":"ENTRADA_INVALIDA","mensagem":"Um ou mais campos são inválidos.","campos":["descricao","nota"]}` |
| Corpo inválido | 400 | `{"erro":"CORPO_INVALIDO","mensagem":"O corpo da requisição precisa ser um objeto JSON válido.","campos":[]}` |
| Falha interna | 500 | `{"erro":"ERRO_INTERNO","mensagem":"Não foi possível registrar a avaliação. Tente novamente."}` |

`campos` traz só nomes, nunca valores. O 500 vem de um `catch` no handler, que registra o erro em log e devolve texto fixo.

Alternativa descartada: mensagem por campo, com o motivo. A spec exige só o nome do campo, e o motivo aumentaria o que precisa ser testado sem pedido.

### D6. Powertools 2.11.0 com API funcional, sem AspectJ
**Confirmado: a versão fixada no `pom.xml` serve, não é preciso subir.** A 2.11.0 é a última: consta como `latest` no [Maven Central](https://repo1.maven.org/maven2/software/amazon/lambda/powertools-tracing/maven-metadata.xml) e como Latest nas [releases](https://github.com/aws-powertools/powertools-lambda-java/releases). As mesmas releases registram suporte a Java 25 e abordagem funcional para todos os utilitários na v2.7.0.

| Utilitário | API usada | Documentação |
|---|---|---|
| Logs | `PowertoolsLogging.withLogging(context, () -> ...)` | [Logging](https://docs.aws.amazon.com/powertools/java/latest/core/logging/): "AspectJ configuration is not required" |
| Tracing | `TracingUtils.withSubsegment(...)`, `putAnnotation` e `TracingUtils.init()` | [Tracing](https://docs.aws.amazon.com/powertools/java/latest/core/tracing/) |
| Métricas | `MetricsBuilder.builder()...build()`, `captureColdStartMetric`, `addMetric` e `flush` | [Metrics](https://docs.aws.amazon.com/powertools/java/latest/core/metrics/): "eliminates the AspectJ runtime dependency" |

Verificado localmente: um módulo com as três APIs, sem `aspectjrt` no classpath, compila com JDK 25 e carrega `PowertoolsLogging`, `TracingUtils` e `MetricsBuilder`.

O que se perde em relação às anotações, segundo as mesmas páginas:
- `logError` e `flushBufferOnUncaughtError` só existem com `@Logging`. O handler captura a exceção e registra o erro por conta própria.
- A página de tracing só cita a anotação `ColdStart` junto de `@Tracing`. Sem ela, o cold start fica registrado pela métrica `captureColdStartMetric`.
- Captura de resposta e exceção no trace não é automática, o que também ajuda D9.
- `metrics.flush()` é manual e fica em `finally`. As métricas saem em formato EMF nos logs, então a role não precisa de `cloudwatch:PutMetricData` ([boas práticas](https://docs.aws.amazon.com/lambda/latest/dg/best-practices.html)).

**Limpeza do `pom.xml`, tarefa 1.1:** sai `aspectjrt`, `aspectj-maven-plugin`, as propriedades `aspectj.version` e `aspectj-maven-plugin.version` e os comentários sobre anotações. A documentação de logging lista `powertools-logging` junto de `powertools-logging-log4j`, então os dois entram em `dependencyManagement`.

Alternativa descartada: anotações com `aspectj-maven-plugin`. Mais um passo de build, sensível à versão do Java, por um ganho que o escopo não usa. Decisão pedida pelo Gabriel.

### D7. Limitações do SnapStart
Valem para as três funções. Fontes: [SnapStart](https://docs.aws.amazon.com/lambda/latest/dg/snapstart.html), [unicidade](https://docs.aws.amazon.com/lambda/latest/dg/snapstart-uniqueness.html), [boas práticas](https://docs.aws.amazon.com/lambda/latest/dg/snapstart-best-practices.html), [ativação](https://docs.aws.amazon.com/lambda/latest/dg/snapstart-activate.html) e [SDK e Lambda](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/lambda-optimize-starttime.html).

| Tema | O que a documentação diz | Regra no projeto |
|---|---|---|
| IDs únicos | Conteúdo único gerado na inicialização pode se repetir entre ambientes restaurados. Gerar no handler. `SecureRandom` é reconhecido como seguro nos runtimes gerenciados | `UUID.randomUUID()` dentro do handler. Nenhum campo estático guarda ID, UUID ou gerador pseudoaleatório com semente |
| Instante | Dados efêmeros, como timestamps guardados, precisam ser renovados no handler | `Instant.now(clock)` dentro do handler. Campo estático só para `Clock` e `ZoneId`, que não guardam instante |
| Credenciais | Com SnapStart, o runtime usa `AWS_CONTAINER_CREDENTIALS_FULL_URI` em vez das variáveis de chave. O guia do SDK avisa que `EnvironmentVariableCredentialsProvider` deixa de funcionar | Nunca fixar esse provedor. Os clientes usam a cadeia padrão, sem chamar `credentialsProvider` |
| Clientes do SDK | Criar fora do handler. Na maioria dos casos, conexões abertas pelo SDK retomam sozinhas | `static final` no handler, sem chamada à AWS durante a inicialização |
| Conexões | O estado de conexões da inicialização não é garantido. Não fixar porta de origem | Só o cliente do SDK abre conexões |
| Hostname | Todos os ambientes restaurados devolvem o mesmo valor | Não usar hostname como identificador |
| Cache de DNS | Evitar cache de DNS da JVM. Definir `networkaddress.cache.ttl` como 0 antes de criar o logger | Primeira linha do bloco estático do handler |
| Recursos incompatíveis | Sem provisioned concurrency, EFS, S3 Files e armazenamento efêmero acima de 512 MB | Nenhum é usado |
| Preço | Sem custo adicional em runtimes Java gerenciados | Cabe na premissa de custo |
| Inatividade | Em Java, snapshot é apagado depois de 14 dias sem invocação, e a chamada seguinte falha com `SnapStartNotReadyException` até voltar a `Active` | Invocar as funções antes de gravar o vídeo (fundação, Risks) |

A página de tracing pede `TracingUtils.init()` na inicialização para o priming rodar. As páginas de logging e de métricas não citam SnapStart.

Alternativas descartadas:
- `SdkWarmUp` e runtime hooks de CRaC. A própria documentação avisa que SnapStart rende menos em função pouco invocada, que é o caso da demonstração. Fica como melhoria se a latência medida incomodar.
- Plugin SpotBugs `aws-lambda-snapstart-java-rules`. Mais uma ferramenta no build para regras que cabem em revisão e em três handlers pequenos.

### D8. SnapStart só no alias `live`
[SnapStart só vale em versões publicadas e aliases](https://docs.aws.amazon.com/lambda/latest/dg/snapstart.html), e invocar `$LATEST` não o exercita. `AutoPublishAlias: live` em `Globals` cria versão e alias.

Verificado localmente no template gerado: a integração da API e a permissão apontam para `RegistrarAvaliacaoAliaslive`. O mesmo vale para o mapeamento do Stream e o Scheduler nos changes seguintes.

`AutoPublishAliasAllProperties: true` entra em cada função porque, com o padrão, [só `Environment`, `MemorySize`, `SnapStart` e mudanças de código publicam versão](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-resource-function.html) (04/11/2026). Uma mudança só de `Timeout` ficaria em `$LATEST`, e o alias continuaria na versão antiga.

Mesmo assim, a mesma página avisa que mudar só o valor de um parâmetro não publica versão, porque o SAM compara a referência sem resolver. Isso não pega `Stage`, que não muda dentro de uma stack.

**Por que na função, e não em `Globals`:** a [lista de `Globals`](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-specification-template-anatomy-globals.html) inclui a propriedade, mas o `sam validate --lint` do SAM CLI 1.171.0 a rejeita ali com o erro E3724. Verificado localmente. Como o CI roda esse comando, ela fica em cada função, e os três trechos a declaram.

Alternativa descartada: apontar os gatilhos para `$LATEST`. Desligaria o SnapStart sem nenhum erro aparente.

### D9. Nenhum dado do aluno em log, em nenhum nível
A spec proíbe em INFO. A decisão de implementação é mais estrita: `descricao`, `nota` e corpo bruto não vão para log em nível nenhum, inclusive WARN, ERROR e DEBUG, e nem para anotações ou metadados de trace. Não muda a spec, só cumpre com folga.

- Pode ir para log: `id`, `urgencia`, nomes de campos inválidos, nome da classe da exceção, ID da requisição.
- Exceção do Jackson: registrar só a classe, nunca a mensagem nem a pilha, porque podem citar trechos da entrada.
- Exceção do SDK: registrar a classe, o código de erro e o ID da requisição, sem a mensagem. Regra uniforme, sem precisar provar o que cada serviço devolve na mensagem. Para diagnóstico, o código de erro basta na maioria dos casos, como `AccessDeniedException`.
- `withLogging` usa a forma sem `input`, e nada de log do evento recebido.

Alternativa descartada: permitir descrição em ERROR para ajudar no diagnóstico. O caso típico de erro na função é falha de infraestrutura, em que o conteúdo não ajuda, e a regra única é mais fácil de testar e de defender na parte de segurança.

### D10. `Globals` definitivo
Este change é o único que altera `Globals`. O bloco já contempla as três funções:

| Propriedade | Valor | Motivo |
|---|---|---|
| `Runtime` | `java25` | Fundação D7 |
| `Architectures` | `x86_64` | Mesma arquitetura da máquina de desenvolvimento, para `sam local` sem emulação |
| `MemorySize` | 512 | [CPU proporcional à memória](https://docs.aws.amazon.com/lambda/latest/dg/best-practices.html), com folga para o SDK em Java. O volume da demonstração é baixo |
| `Timeout` | 30 | Padrão das funções. Cada função sobrescreve quando precisa |
| `Tracing` | `Active` | Faz o SAM anexar `AWSXrayWriteOnlyAccess`, que o bootstrap aceita |
| `AutoPublishAlias` | `live` | D8 |
| `SnapStart` | `PublishedVersions` | D7 |
| `PermissionsBoundary` | sem mudança | Fundação D9 |
| Variáveis | `AMBIENTE`, `POWERTOOLS_LOG_LEVEL` e `POWERTOOLS_METRICS_NAMESPACE` | Iguais nas três funções. O resto fica na própria função |

Todas essas propriedades constam na lista de `Globals` citada na D8, e mapas de variáveis são mesclados com os da função. Verificado localmente no template gerado.

Alternativa descartada: `arm64`. Sem ganho perceptível dentro do Free Tier e pior para `sam local` no Windows.

### D11. Grupo de log explícito, com retenção
Cada função declara `AWS::Logs::LogGroup` com retenção de 14 dias e a função usa `DependsOn` nele.

Sem isso, o [Lambda cria o grupo sozinho](https://docs.aws.amazon.com/lambda/latest/dg/monitoring-cloudwatchlogs.html), e declará-lo depois falharia por nome já existente. O `DependsOn` importa com SnapStart: a inicialização roda ao publicar a versão e já escreve log, então o grupo precisa existir antes.

Alternativa descartada: deixar para `add-observabilidade-seguranca`. Nessa altura os grupos já existiriam, criados pelo Lambda.

### D12. Build e cliente HTTP
**Confirmado, sem ajuste no `deploy.yml`:** `mvn -B install` seguido de `sam build` funciona em módulo que depende de `dominio`. Verificado localmente com o SAM CLI 1.171.0 e JDK 25, em uma cópia do parent com módulo `dominio` e função dependente:
- Com o install antes, o build passa e `lib/` traz o jar do `dominio`.
- Sem o install, falha com `JavaMavenWorkflow:MavenBuild - Maven Failed`, como o comentário do `deploy.yml` antecipa.
- O código do SAM mostra `mvn clean install` e `dependency:copy-dependencies` na pasta temporária. Logo, os testes do módulo rodam de novo em cada `sam build`.

**`java25` no SAM CLI:** a versão 1.171.0 registra `java25` no workflow de Maven, e o build acima rodou. A [lista de imagens do SAM](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-image-repositories.html) traz `build-java25`. Não encontrei página oficial que liste os runtimes aceitos pelo `sam build` com `java25` pelo nome. O workflow instala o SAM CLI mais recente, então a versão no CI não será menor.

**JDK na máquina:** `sam build` e `mvn` exigem um JDK 25 no `JAVA_HOME`. Localmente vale qualquer distribuição, por decisão do Gabriel em 04/11/2026, e o CI continua com Corretto 25. Ver Risks.

**Cliente HTTP:** `url-connection-client`, com `apache-client`, `apache5-client` e `netty-nio-client` excluídos de cada cliente de serviço. Verificado localmente: o SDK 2.57.8 traz `apache5-client` por transitividade, então excluir só os dois nomes da documentação não basta. O `httpclient` 4.5 que vem do X-Ray pelo Powertools fica. O pacote de teste, com as exclusões parciais, ficou em 19 MB.

Alternativa descartada: `aws-crt-client`, que é o que o [guia do SDK](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/lambda-optimize-starttime.html) recomenda para Lambda. O motivo do guia é tempo de inicialização, que o SnapStart já reduz. O CRT traz biblioteca nativa, e o `url-connection-client` é Java puro e suficiente para chamadas sequenciais.

### D13. IAM de `registrar-avaliacao`
Role gerada pelo SAM, logical ID `RegistrarAvaliacaoRole`, com boundary:

| Origem | Ações | Recurso |
|---|---|---|
| Inline | `dynamodb:PutItem` | ARN da tabela |
| `AWSLambdaBasicExecutionRole` | `logs:CreateLogGroup`, `logs:CreateLogStream` e `logs:PutLogEvents` | limitado pelo boundary aos grupos do ambiente |
| [`AWSXrayWriteOnlyAccess`](https://docs.aws.amazon.com/aws-managed-policy/latest/reference/AWSXrayWriteOnlyAccess.html) | `xray:PutTraceSegments`, `xray:PutTelemetryRecords`, `xray:GetSamplingRules`, `xray:GetSamplingTargets` e `xray:GetSamplingStatisticSummaries` | `*`, como no boundary |

Sem KMS na role: a tabela usa `SSEEnabled: true`, ou seja, [chave gerenciada `aws/dynamodb`](https://docs.aws.amazon.com/AWSCloudFormation/latest/TemplateReference/aws-properties-dynamodb-table-ssespecification.html) (04/11/2026), e a [política dessa chave](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/encryption.usagenotes.html) (04/11/2026) já autoriza os usuários da conta quando a chamada vem do DynamoDB. O boundary também libera `kms:Decrypt` e `kms:GenerateDataKey*` via DynamoDB.

**Conferência contra o bootstrap:** todas as ações acima cabem no boundary `feedback-limite-funcoes-<amb>`. Verificado localmente no template gerado:
- `iam:AttachRolePolicy` recebe só `AWSLambdaBasicExecutionRole` e `AWSXrayWriteOnlyAccess`, ambas na lista do bootstrap.
- Função: `feedback-registrar-avaliacao-<amb>`, casa com `feedback-*-<amb>`.
- Role: nome gerado no formato `feedback-<amb>-RegistrarAvaliacaoRole-<aleatório>`, casa com `feedback-<amb>-*`. A [documentação do CloudFormation](https://docs.aws.amazon.com/AWSCloudFormation/latest/TemplateReference/aws-properties-name.html) (04/11/2026) só mostra o formato em exemplo (`MyStack-MyBucket-abcdefghijk1`), sem prometê-lo.
- `iam:DeleteRole`, `iam:UpdateRole` e `iam:UpdateAssumeRolePolicy` estão sob condição de `iam:PermissionsBoundary` no bootstrap, e a [referência de serviço do IAM](https://servicereference.us-east-1.amazonaws.com/v1/iam/iam.json) lista essa chave para as três. Não há negação indevida ao substituir role.
- `samconfig.toml` usa `CAPABILITY_IAM`, suficiente porque nenhuma role tem nome explícito.

Nenhuma alteração no bootstrap é necessária para este change.

Alternativa descartada: política `DynamoDBCrudPolicy` do SAM. Dá leitura e exclusão que a função não usa.

## Risks / Trade-offs

- **JDK local:** o `java` do PATH é 21.0.2, e o build exige JDK 25. Mitigação: tarefa 1.2, de Gabriel, antes de implementar. Como a distribuição local pode ser outra, é o CI, com Corretto 25, que comprova o build na distribuição do projeto.
- **X-Ray SDK em manutenção:** desde 25/02/2026 o [X-Ray SDK para Java](https://docs.aws.amazon.com/xray/latest/devguide/xray-sdk-java.html) só recebe correções de segurança, e a AWS recomenda OpenTelemetry. O tracing do Powertools é um invólucro desse SDK. Aceito pelo prazo do projeto.
- **Nome da role gerado:** se o CloudFormation truncar ou mudar o formato, o `iam:CreateRole` é negado. Mitigação: os nomes ficam abaixo de 64 caracteres com folga; a falha aparece no primeiro deploy, com mensagem clara.
- **API pública sem autenticação:** qualquer um pode gravar. Fora do escopo por decisão. Mitigação: limite de taxa da D1 e possibilidade de `sam delete` depois do vídeo.
- **KMS na tabela:** a fundação usa chave gerenciada, com [cobrança por chamada](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/EncryptionAtRest.html) (04/11/2026). DynamoDB guarda a chave de tabela em cache, então o volume da demonstração é baixo. Registrado para o caso de a fatura surpreender.
- **`sam build` repete os testes:** soma tempo ao deploy. Aceito, o `timeout-minutes` é de 45.
- **Primeira publicação com SnapStart é lenta:** o deploy espera a versão sair de `Pending`. Aceito.

## Open Questions

Nenhuma.

## Alterações no template.yaml

Dependência: requer `add-fundacao-projeto` integrado. Este change vem antes de `add-notificacao-critica` e `add-relatorio-semanal`, que dependem deste `Globals`.

**1. Substitui o bloco `Globals` inteiro**, mantendo o comentário que vem antes dele:

```yaml
Globals:
  Function:
    PermissionsBoundary: !Sub arn:${AWS::Partition}:iam::${AWS::AccountId}:policy/feedback-limite-funcoes-${Stage}
    Runtime: java25
    Architectures:
      - x86_64
    MemorySize: 512
    Timeout: 30
    Tracing: Active
    AutoPublishAlias: live
    SnapStart:
      ApplyOn: PublishedVersions
    Environment:
      Variables:
        AMBIENTE: !Ref Stage
        POWERTOOLS_LOG_LEVEL: INFO
        POWERTOOLS_METRICS_NAMESPACE: FeedbackServerless
```

**2. Acrescenta ao fim de `Resources`**, depois de `PoliticaBucketRelatorios`:

```yaml
  ApiAvaliacoes:
    Type: AWS::Serverless::HttpApi
    Properties:
      Name: !Sub feedback-api-${Stage}
      FailOnWarnings: true
      Tags:
        ambiente: !Ref Stage
      DefaultRouteSettings:
        ThrottlingBurstLimit: 20
        ThrottlingRateLimit: 10

  LogRegistrarAvaliacao:
    Type: AWS::Logs::LogGroup
    Properties:
      LogGroupName: !Sub /aws/lambda/feedback-registrar-avaliacao-${Stage}
      RetentionInDays: 14

  RegistrarAvaliacao:
    Type: AWS::Serverless::Function
    DependsOn: LogRegistrarAvaliacao
    Properties:
      FunctionName: !Sub feedback-registrar-avaliacao-${Stage}
      CodeUri: functions/registrar-avaliacao
      Handler: br.com.fiap.feedback.registro.RegistrarAvaliacaoHandler::handleRequest
      Timeout: 10
      AutoPublishAliasAllProperties: true
      Environment:
        Variables:
          POWERTOOLS_SERVICE_NAME: registrar-avaliacao
          TABELA_AVALIACOES: !Ref TabelaAvaliacoes
      Policies:
        - Version: "2012-10-17"
          Statement:
            - Sid: GravarAvaliacao
              Effect: Allow
              Action: dynamodb:PutItem
              Resource: !GetAtt TabelaAvaliacoes.Arn
      Events:
        PostAvaliacao:
          Type: HttpApi
          Properties:
            ApiId: !Ref ApiAvaliacoes
            Method: POST
            Path: /avaliacao
            PayloadFormatVersion: "2.0"
```

**3. Acrescenta ao fim de `Outputs`:**

```yaml
  ApiAvaliacoesUrl:
    Description: URL do endpoint POST /avaliacao
    Value: !Sub https://${ApiAvaliacoes}.execute-api.${AWS::Region}.${AWS::URLSuffix}/avaliacao
  RegistrarAvaliacaoNome:
    Description: Nome da função de registro
    Value: !Ref RegistrarAvaliacao
  RegistrarAvaliacaoAliasArn:
    Description: ARN do alias live da função de registro
    Value: !Ref RegistrarAvaliacao.Alias
```

IDs lógicos novos: `ApiAvaliacoes`, `LogRegistrarAvaliacao` e `RegistrarAvaliacao`. O SAM também gera `RegistrarAvaliacaoRole`, `RegistrarAvaliacaoAliaslive`, `RegistrarAvaliacaoVersion<hash>`, `RegistrarAvaliacaoPostAvaliacaoPermission` e `ApiAvaliacoesApiGatewayDefaultStage`.

Os três trechos deste change, somados aos dos outros dois sobre o `template.yaml` atual, passaram em `sam validate --lint` em uma cópia temporária.

## Desvios do project.md

Nenhum. D2 refina o formato de `criadoEm` e D9 é mais estrita que a regra de logs, ambas sem contrariar o `project.md`.
