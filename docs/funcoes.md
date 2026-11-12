# Funções

As três funções Lambda da plataforma, com a configuração que elas compartilham e o contrato de cada uma. Tudo aqui sai do [`template.yaml`](../template.yaml), do código em `functions/` e das specs `registro-avaliacao`, `notificacao-critica` e `relatorio-semanal`. Quando um comportamento foi conferido numa stack, a frase cita o `qa.md` onde a conferência está registrada.

| Função | Nome implantado | Gatilho | Timeout |
|---|---|---|---|
| `registrar-avaliacao` | `feedback-registrar-avaliacao-<amb>` | API Gateway HTTP API, `POST /avaliacao` | 10 s |
| `notificar-critica` | `feedback-notificar-critica-<amb>` | DynamoDB Streams da tabela de avaliações, com filtro | 30 s |
| `gerar-relatorio-semanal` | `feedback-gerar-relatorio-semanal-<amb>` | EventBridge Scheduler, toda segunda às 08:00 em São Paulo | 120 s |

`<amb>` é `dev`, `hml` ou `prod`, o valor do parâmetro `Stage` da stack. A configuração dos alarmes está em [docs/monitoramento.md](monitoramento.md), e o boundary, as roles de deploy e o registro de riscos estão em [docs/seguranca.md](seguranca.md).

## Configuração comum

### Execução

Valores do bloco `Globals` do template, iguais nas três funções:

| Propriedade | Valor | Observação |
|---|---|---|
| `Runtime` | `java25` | |
| `Architectures` | `x86_64` | |
| `MemorySize` | 1024 | Nenhuma função sobrescreve. A CPU do Lambda cresce com a memória, e o que pesava na primeira chamada depois da restauração era CPU, não memória: as funções usam de 194 a 221 MB |
| `Timeout` | 30 | O registro sobrescreve com 10 e o relatório com 120. A notificação usa os 30 |
| `Tracing` | `Active` | X-Ray em toda invocação. Faz o SAM anexar `AWSXrayWriteOnlyAccess` à role |
| `AutoPublishAlias` | `live` | Cada deploy que muda a função publica uma versão e move o alias |
| `SnapStart.ApplyOn` | `PublishedVersions` | |
| `PermissionsBoundary` | `feedback-limite-funcoes-<amb>` | Teto de permissão de toda role de função. Sem ele, a role de deploy não consegue criar a role |

Cada função declara também `AutoPublishAliasAllProperties: true`. Sem isso, o SAM só publica versão quando mudam código, `Environment`, `MemorySize` ou `SnapStart`, e uma mudança só de `Timeout` ficaria em `$LATEST`. A propriedade fica na função, e não no `Globals`, porque o `sam validate --lint` a rejeita ali (erro E3724).

Cada função tem um grupo de log declarado no template, `/aws/lambda/feedback-<função>-<amb>`, e um `DependsOn` nele, porque a inicialização com SnapStart roda na publicação da versão e já escreve log. A retenção por ambiente está em [docs/monitoramento.md](monitoramento.md).

### Variáveis do `Globals`

| Variável | Origem no template | Uso |
|---|---|---|
| `AMBIENTE` | `!Ref Stage` | Lida pelo relatório, que a escreve na linha `Ambiente:` do relatório e do aviso. Registro e notificação não leem |
| `POWERTOOLS_LOG_LEVEL` | `INFO` | Nível dos loggers. O Powertools aplica o valor a todos, inclusive aos do SDK |
| `POWERTOOLS_METRICS_NAMESPACE` | `!Sub FeedbackServerless/${Stage}` | Namespace das métricas do Powertools |

Cada função acrescenta `POWERTOOLS_SERVICE_NAME`, com o nome curto da função, e as variáveis dos recursos que usa, listadas na seção dela.

### Alias `live` e SnapStart

O SnapStart só vale em versões publicadas e aliases. Os três gatilhos apontam para o alias `live`, nunca para `$LATEST`:

- a integração da rota `POST /avaliacao` e a permissão de invocação da API, conferidas em dev em 05/11/2026 ([qa.md de add-registro-avaliacao](../openspec/changes/archive/2026-11-11-add-registro-avaliacao/qa.md), seção 6.4);
- o mapeamento do Stream, conferido em dev em 05/11/2026 ([qa.md de add-notificacao-critica](../openspec/changes/archive/2026-11-11-add-notificacao-critica/qa.md), seção 5.5);
- o alvo do agendamento, conferido em dev em 06/11/2026 ([qa.md de add-relatorio-semanal](../openspec/changes/archive/2026-11-11-add-relatorio-semanal/qa.md), seção 5.4).

O acionamento manual do relatório também precisa do sufixo `:live`. Sem ele, a chamada vai para `$LATEST`, que tem `OptimizationStatus` `Off`, e nenhum erro aparece.

Em Java, a AWS apaga o snapshot de uma versão depois de 14 dias sem invocação, e a chamada seguinte falha com `SnapStartNotReadyException` até a versão voltar a `Active`. Por isso convém invocar as funções antes de uma demonstração.

Os hooks de priming do Powertools 2.11.0 rodam antes do snapshot e exigem o `aspectjrt` no pacote, mesmo com a API funcional, sem anotações. O motivo está no [incidente de 04/11/2026](incidentes/2026-11-04-snapstart-aspectjrt.md), e a condição para tirar o jar está em [docs/seguranca.md](seguranca.md).

Para conferir o alias depois de um deploy, troque o nome da função e o sufixo do ambiente:

Registro: `get-function-configuration` no alias `live` das três funções, em dev em 06/11/2026, seção 6.4 do [qa.md de add-observabilidade-seguranca](../openspec/changes/archive/2026-11-11-add-observabilidade-seguranca/qa.md), e em hml em 07/11/2026, item 1 da seção "Release 1.0.0 em hml (07/11/2026)" do [qa.md de add-fundacao-projeto](../openspec/changes/archive/2026-11-11-add-fundacao-projeto/qa.md).

```bash
aws lambda get-function-configuration \
  --function-name feedback-registrar-avaliacao-dev:live \
  --query "{Version:Version,State:State,OptimizationStatus:SnapStart.OptimizationStatus,MemorySize:MemorySize}" \
  --region sa-east-1 --profile tc4-admin
```

Resultado esperado: `State` igual a `Active`, `OptimizationStatus` igual a `On` e `MemorySize` igual a 1024. `Version` é o número da versão para a qual o alias aponta.

### Aquecimento antes do snapshot

Cada função registra um hook do SnapStart, a classe `Aquecimento`, com `org.crac`. Antes do snapshot, ele faz uma chamada de leitura com cada cliente do SDK, para que credenciais, assinatura e cliente HTTP já estejam carregados quando a versão for restaurada.

| Função | Chamada | Permissão usada |
|---|---|---|
| `registrar-avaliacao` | DynamoDB `DescribeTable` na tabela de avaliações | `AquecerCliente` |
| `notificar-critica` | SNS `GetTopicAttributes` no tópico de administradores | `AquecerCliente` |
| `gerar-relatorio-semanal` | DynamoDB `Query` no índice `por-data`, numa partição que nunca existe, com `Limit` 1 | `ConsultarIndicePorData` |
| `gerar-relatorio-semanal` | S3 `HeadBucket` no bucket de relatórios | `AquecerClienteS3` |
| `gerar-relatorio-semanal` | SNS `GetTopicAttributes` no tópico de administradores | `AquecerClienteSns` |

- Nenhuma chamada grava item, publica mensagem ou cria objeto, e nenhuma devolve descrição ou nota.
- Sucesso: uma linha INFO `Cliente aquecido` por cliente, com `servico`, `operacao` e `duracaoMs`, no log da publicação da versão.
- Falha: uma linha WARN `Falha no aquecimento`, com `servico`, `classe` e, se o erro vier do serviço, `codigoErro`. A publicação da versão continua, e a falha de um cliente não impede os outros. A linha não tem marcador de alarme.
- O hook roda só quando há snapshot. Não roda em `$LATEST` nem no `sam local`.

Em dev, em 06/11/2026, o aquecimento somou de 1,6 a 1,8 s à publicação de cada versão, e a primeira chamada a cada serviço depois da restauração caiu de 2,6 a 4,3 s para 105 a 339 ms, com memória e aquecimento no mesmo deploy (seções 6.4 e 6.15 do [qa.md de add-observabilidade-seguranca](../openspec/changes/archive/2026-11-11-add-observabilidade-seguranca/qa.md)).

Registro: leitura das linhas `Cliente aquecido` do log da publicação das três versões em dev, em 06/11/2026, seção 6.4 do [qa.md de add-observabilidade-seguranca](../openspec/changes/archive/2026-11-11-add-observabilidade-seguranca/qa.md).

```bash
aws logs filter-log-events \
  --log-group-name /aws/lambda/feedback-registrar-avaliacao-dev \
  --filter-pattern '"Cliente aquecido"' \
  --start-time $(( ($(date +%s) - 3600) * 1000 )) \
  --query "events[].message" \
  --region sa-east-1 --profile tc4-admin
```

Com o relatório, aparecem três linhas por versão publicada.

### Permissões comuns

Toda role de função tem o boundary `feedback-limite-funcoes-<amb>`, definido no bootstrap. Além da política inline de cada função, o SAM anexa políticas gerenciadas da AWS:

| Política gerenciada | Ações | Recurso na política | Funções |
|---|---|---|---|
| `AWSLambdaBasicExecutionRole` | `logs:CreateLogGroup`, `logs:CreateLogStream`, `logs:PutLogEvents` | `*`, limitado pelo boundary aos grupos do ambiente | as três |
| `AWSXrayWriteOnlyAccess` | `xray:PutTraceSegments`, `xray:PutTelemetryRecords`, `xray:GetSamplingRules`, `xray:GetSamplingTargets`, `xray:GetSamplingStatisticSummaries` | `*` | as três |
| `AWSLambdaDynamoDBExecutionRole` | `dynamodb:DescribeStream`, `dynamodb:GetRecords`, `dynamodb:GetShardIterator`, `dynamodb:ListStreams` e as mesmas três de `logs` | `*`, limitado pelo boundary | só `notificar-critica`, por causa do gatilho do Stream |

As gerenciadas da notificação e do relatório foram conferidas nas roles de dev ([qa.md de add-notificacao-critica](../openspec/changes/archive/2026-11-11-add-notificacao-critica/qa.md), "Estado da stack e da pipeline"; [qa.md de add-relatorio-semanal](../openspec/changes/archive/2026-11-11-add-relatorio-semanal/qa.md), "Estado da stack"). As do registro vêm da D13 do [design de add-registro-avaliacao](../openspec/changes/archive/2026-11-11-add-registro-avaliacao/design.md).

Nas tabelas de cada função, `<ID_DA_CONTA>` e `<amb>` são marcadores, e a região do projeto é `sa-east-1`.

### Métricas e logs comuns

**Métricas do Powertools.** Contagens com valor 1, no formato EMF, no namespace `FeedbackServerless/<amb>` e com a dimensão `Service`, que o Powertools preenche com `POWERTOOLS_SERVICE_NAME`. Falha ao emitir métrica gera WARN `Falha ao emitir métrica`, só com a classe da exceção, e não muda a resposta nem o resultado da invocação. Os handlers chamam `captureColdStartMetric`, mas a métrica `ColdStart` não sai com SnapStart: o Powertools só a emite quando a inicialização é `on-demand` ([qa.md de add-registro-avaliacao](../openspec/changes/archive/2026-11-11-add-registro-avaliacao/qa.md), observação B). A duração da restauração é lida do log, como explica [docs/monitoramento.md](monitoramento.md).

**Formato.** As linhas da aplicação saem em JSON pelo layout `LambdaJsonLayout.json` do Powertools, com `level`, `message`, `cold_start`, `function_arn`, `function_memory_size`, `function_name`, `function_request_id`, `function_version`, `service`, `timestamp` e `xray_trace_id`, mais os campos de cada linha. As linhas de plataforma (`START`, `END`, `REPORT`, `INIT_*`, `RESTORE_*`) saem em texto, porque as funções usam `LogFormat: Text`.

**Dado do aluno.** O código não envia `descricao`, `nota`, média nem corpo bruto para log em nível nenhum, nem para anotação ou metadado de trace. Exceção do SDK é registrada pela classe, pelo código de erro e pelo ID da requisição, sem a mensagem. Exceção do Jackson, só pela classe. A spec exige isso em INFO; o código cumpre em todos os níveis, com a função em INFO.

**Marcador de alarme.** As linhas que representam falha da função levam o campo `alarme` com o valor `FALHA_FUNCAO`. O filtro de métrica de cada função procura esse marcador e as linhas de tempo esgotado do runtime. As três funções têm a mesma linha para `Error` que escapa da invocação, como `NoClassDefFoundError` ou `OutOfMemoryError`: ERROR `Erro na execução da função`, com `classe` e o marcador, e o mesmo `Error` é relançado ao runtime.

## registrar-avaliacao

### Responsabilidade

Recebe a avaliação de aula do aluno, valida, calcula a urgência e grava o item na tabela de avaliações. O item gravado alimenta a notificação, pelo Stream, e o relatório, pelo índice `por-data`. Código em [`RegistrarAvaliacaoHandler.java`](../functions/registrar-avaliacao/src/main/java/br/com/fiap/feedback/registro/RegistrarAvaliacaoHandler.java) e [`Respostas.java`](../functions/registrar-avaliacao/src/main/java/br/com/fiap/feedback/registro/Respostas.java).

### Gatilho

Evento `PostAvaliacao`, do tipo `HttpApi`, na API `feedback-api-<amb>` (`ApiAvaliacoes`): método `POST`, caminho `/avaliacao`, payload 2.0, estágio `$default`. A rota não tem autenticação, que está fora do escopo do projeto. A API limita as rotas a 10 requisições por segundo, com rajada de 20 (`DefaultRouteSettings`). O excesso recebe 429 do próprio API Gateway, sem invocar a função. O limite foi conferido na configuração do estágio em dev, mas não foi exercitado ([qa.md de add-registro-avaliacao](../openspec/changes/archive/2026-11-11-add-registro-avaliacao/qa.md), seção 6.5).

A URL fica no output `ApiAvaliacoesUrl` da stack, no formato `https://<ID_DA_API>.execute-api.sa-east-1.amazonaws.com/avaliacao`.

### Entrada

Corpo JSON, que pode chegar em Base64 quando o API Gateway marca `isBase64Encoded`:

| Campo | Obrigatório | Regra |
|---|---|---|
| `descricao` | sim | Texto com ao menos um caractere diferente de espaço, tabulação ou quebra de linha, e até 1000 caracteres Unicode. Um emoji conta como um. É gravada como recebida, sem remover espaços |
| `nota` | sim | Número JSON inteiro, sem parte fracionária nem expoente, de 0 a 10 |

Campos extras são ignorados. `id`, `urgencia`, `dataEnvio` e `criadoEm` enviados pelo cliente também são ignorados: quem preenche é a função.

### Saída

Status 201 com `{"id":"<UUID>","urgencia":"<URGENCIA>"}` e o item gravado na tabela `feedback-avaliacoes-<amb>`, com seis atributos e nenhum outro:

| Atributo | Tipo | Valor |
|---|---|---|
| `id` | S | UUID aleatório gerado no handler |
| `descricao` | S | Como recebida |
| `nota` | N | Inteiro de 0 a 10 |
| `urgencia` | S | `CRITICA`, `MEDIA` ou `BAIXA` |
| `dataEnvio` | S | Dia do registro em America/Sao_Paulo, `yyyy-MM-dd`. Chave de partição do índice `por-data` |
| `criadoEm` | S | Instante do registro em UTC, `yyyy-MM-ddTHH:mm:ss.SSSZ`, sempre com 24 caracteres. Chave de ordenação do índice |

Todas as respostas têm `content-type: application/json`. Os corpos completos de cada caso, com requisição e resposta, estão na seção Endpoints do [relatório do Tech Challenge](relatorio-tech-challenge.md).

### Regras de negócio

- **Urgência** calculada no registro, a partir da nota, devolvida na resposta e gravada no item:

  | Nota | Urgência |
  |---|---|
  | 0 a 3 | `CRITICA` |
  | 4 a 6 | `MEDIA` |
  | 7 a 10 | `BAIXA` |

- **Datas:** `criadoEm` e `dataEnvio` saem do mesmo instante, lido uma vez por requisição. `2026-11-10T02:59:59Z` gera `dataEnvio` `2026-11-09`, e `2026-11-10T03:00:00Z` gera `2026-11-10`. A largura fixa de `criadoEm` mantém a ordem do índice cronológica, porque o DynamoDB ordena texto por bytes.
- **Cada requisição válida gera uma avaliação nova**, mesmo com corpo idêntico. Não há deduplicação.
- **Entrada rejeitada não grava nada.**
- **Identificador e instante gerados dentro do handler**, nunca na inicialização, para não se repetirem entre ambientes restaurados do mesmo snapshot.

Conferido em dev em 05/11/2026, com 43 requisições e as três urgências, e em hml em 07/11/2026 ([qa.md de add-registro-avaliacao](../openspec/changes/archive/2026-11-11-add-registro-avaliacao/qa.md), seção 6.6; [qa.md de add-fundacao-projeto](../openspec/changes/archive/2026-11-11-add-fundacao-projeto/qa.md), item 2 da release em hml).

### Erros tratados e efeito

| Caso | Status | `erro` | `mensagem` | `campos` | Efeito |
|---|---|---|---|---|---|
| Campo inválido | 400 | `ENTRADA_INVALIDA` | `Um ou mais campos são inválidos.` | Nome de cada campo reprovado, nunca o valor | Nada gravado. INFO `Entrada rejeitada`. Métrica `EntradaRejeitada` |
| Corpo inválido | 400 | `CORPO_INVALIDO` | `O corpo da requisição precisa ser um objeto JSON válido.` | Vazio | Nada gravado. INFO `Corpo rejeitado`. Métrica `EntradaRejeitada` |
| Falha ao gravar | 500 | `ERRO_INTERNO` | `Não foi possível registrar a avaliação. Tente novamente.` | Ausente | ERROR `Falha ao registrar avaliação`, com o marcador. Métrica `ErroInterno` |

Casos que dão `ENTRADA_INVALIDA`, um por cenário da spec:

- `descricao`: ausente, `null`, `""`, só espaços, só tabulação e quebra de linha, número em vez de texto, ou mais de 1000 caracteres.
- `nota`: ausente, `null`, -1, 11, `7.5`, `7.0`, `1e1`, o texto `"7"`, ou o inteiro `99999999999999999999`.
- Os dois campos inválidos na mesma requisição: `campos` traz os dois.

Casos que dão `CORPO_INVALIDO`: corpo ausente ou vazio, JSON truncado como `{"descricao": "Aula boa", "nota": 5`, JSON que não é objeto como `[1, 2, 3]`, objeto seguido de outros tokens e Base64 inválido. Também cai aqui o corpo que passa dos limites do parser do Jackson, como um número com mais de 1000 dígitos, mesmo em campo extra. Esse limite foi aceito pelo Gabriel em 04/11/2026 ([qa.md de add-registro-avaliacao](../openspec/changes/archive/2026-11-11-add-registro-avaliacao/qa.md), defeito D1).

O 500 só tem prova nos testes do handler, com o DynamoDB simulado, porque não foi provocado numa stack. O corpo não traz mensagem de exceção, pilha nem nome de recurso.

`Error` que escapa da invocação não gera resposta da função: ela grava a linha `Erro na execução da função` e relança o `Error` ao runtime. O tempo esgotado também encerra a invocação sem resposta da função; por isso o cliente do DynamoDB desiste em 8 s, antes do `Timeout` de 10 s, e a falha comum passa pelo `catch` que devolve o 500.

### Variáveis de ambiente

| Variável | Origem no template | Uso |
|---|---|---|
| `POWERTOOLS_SERVICE_NAME` | `registrar-avaliacao` | Campo `service` do log e dimensão `Service` das métricas |
| `TABELA_AVALIACOES` | `!Ref TabelaAvaliacoes` | Tabela do `PutItem` e do aquecimento. Sem ela, a função não inicializa |

Mais as três do `Globals`.

### Permissões

Política inline da role `RegistrarAvaliacaoRole`, gerada pelo SAM:

| `Sid` | Ação | Recurso | Condição |
|---|---|---|---|
| `GravarAvaliacao` | `dynamodb:PutItem` | Tabela `feedback-avaliacoes-<amb>` (`!GetAtt TabelaAvaliacoes.Arn`) | |
| `AquecerCliente` | `dynamodb:DescribeTable` | Tabela `feedback-avaliacoes-<amb>` (`!GetAtt TabelaAvaliacoes.Arn`) | |

Mais `AWSLambdaBasicExecutionRole` e `AWSXrayWriteOnlyAccess`. A role não lê itens, não apaga e não publica em tópico: o registro não depende do SNS. A tabela usa a chave gerenciada `aws/dynamodb` (`SSEEnabled: true`), que não exige permissão de KMS na role.

Fora da role, o SAM gera a permissão de recurso que deixa a API invocar o alias: `lambda:InvokeFunction` para `execute-api` em `<ID_DA_API>/*/POST/avaliacao`, só em `feedback-registrar-avaliacao-<amb>:live`. Conferido em dev em 05/11/2026 ([qa.md de add-registro-avaliacao](../openspec/changes/archive/2026-11-11-add-registro-avaliacao/qa.md), seção 6.4).

### Execução

| Item | Valor |
|---|---|
| Memória | 1024 MB, do `Globals` |
| `Timeout` | 10 s, na função |
| Tempo máximo de cada chamada ao SDK | 8 s (`apiCallTimeout`), abaixo do `Timeout` |
| Cliente HTTP | `url-connection-client` |
| Alias e SnapStart | `live`, em versão publicada |
| Aquecimento | `DescribeTable` na tabela |

### Métricas

| Métrica | Quando |
|---|---|
| `AvaliacaoRegistrada` | Item gravado, resposta 201 |
| `EntradaRejeitada` | Resposta 400, por campo ou por corpo |
| `ErroInterno` | Resposta 500 |

Todo caminho conta uma métrica, então toda invocação descarrega métricas. Na stack de dev, em 05/11/2026, os registros EMF trouxeram `AvaliacaoRegistrada` e `EntradaRejeitada`; `ErroInterno` não foi exercitada.

A métrica do alarme, `FalhasRegistrarAvaliacao`, não vem do Powertools: é gerada pelo filtro de log descrito em [docs/monitoramento.md](monitoramento.md).

### Logs

| Mensagem | Nível | Campos além do layout | Marcador |
|---|---|---|---|
| `Avaliação registrada` | INFO | `id`, `urgencia` | não |
| `Entrada rejeitada` | INFO | `campos`, com nomes de campo | não |
| `Corpo rejeitado` | INFO | `motivo`: `CorpoAusente`, `CorpoNaoEhObjeto`, `Base64Invalido` ou a classe da exceção do Jackson | não |
| `Falha ao registrar avaliação` | ERROR | `classe`, `codigoErro`, `idRequisicao` | sim |
| `Erro na execução da função` | ERROR | `classe` | sim |
| `Falha ao emitir métrica` | WARN | `classe` | não |
| `Cliente aquecido` | INFO | `servico`, `operacao`, `duracaoMs`, na publicação da versão | não |
| `Falha no aquecimento` | WARN | `servico`, `classe`, `codigoErro` se houver | não |

No X-Ray, a gravação fica no subsegmento `gravarAvaliacao`, com a anotação `urgencia`, e o instrumentor do SDK acrescenta o subsegmento do DynamoDB.

Na leitura completa do grupo de dev, em 05/11/2026, nenhuma linha trouxe descrição, nota ou marcador de teste, e todas as linhas da aplicação estavam em INFO ([qa.md de add-registro-avaliacao](../openspec/changes/archive/2026-11-11-add-registro-avaliacao/qa.md), seção 6.7).

### Alarme

`feedback-falha-registrar-avaliacao-<amb>`. Cobre a resposta 500, o `Error` que escapa da invocação e o tempo esgotado. Entrada rejeitada com 400 não dispara. Configuração em [docs/monitoramento.md](monitoramento.md).

## notificar-critica

### Responsabilidade

Avisa os administradores, por e-mail, a cada avaliação crítica criada. Lê o Stream da tabela de avaliações e publica uma mensagem no tópico de administradores do ambiente, na mesma execução, sem agendamento e sem acumular avaliações. Código em [`NotificarCriticaHandler.java`](../functions/notificar-critica/src/main/java/br/com/fiap/feedback/notificacao/NotificarCriticaHandler.java).

### Gatilho

Evento `AvaliacaoCriada`, do tipo `DynamoDB`, no Stream da tabela `feedback-avaliacoes-<amb>`, que grava `NEW_IMAGE`. Filtro do mapeamento:

```text
{"eventName":["INSERT"],"dynamodb":{"NewImage":{"urgencia":{"S":["CRITICA"]}}}}
```

Só criação de item com `urgencia` `CRITICA` invoca a função. Avaliação `MEDIA` ou `BAIXA`, alteração e remoção não invocam. Configuração do mapeamento:

| Parâmetro | Valor | Efeito |
|---|---|---|
| `StartingPosition` | `TRIM_HORIZON` | O mapeamento começa pelo registro mais antigo ainda retido no Stream |
| `BatchSize` | 10 | Até 10 registros por invocação |
| `FunctionResponseTypes` | `ReportBatchItemFailures` | A função informa em qual registro o lote falhou |
| `BisectBatchOnFunctionError` | `true` | Em erro de função, como tempo esgotado, o Lambda divide o lote. Dividir não consome tentativa |
| `MaximumRetryAttempts` | 5 | O padrão da AWS é infinito |
| `MaximumRecordAgeInSeconds` | 3600 | Registro com mais de 1 hora não é mais tentado. O padrão da AWS é até expirar no Stream, em 24 horas |
| `DestinationConfig.OnFailure` | Bucket de relatórios (`!GetAtt BucketRelatorios.Arn`) | Recebe o que foi descartado. Sem `Type`, de propósito, para o SAM não gerar política própria |

Conferido em dev em 05/11/2026, com o filtro e todos os parâmetros iguais ao template e o mapeamento no alias `live`. Na mesma conferência, uma alteração de item crítico não invocou a função, o que mostra que o filtro por `eventName` funciona ([qa.md de add-notificacao-critica](../openspec/changes/archive/2026-11-11-add-notificacao-critica/qa.md), seção 5.5). Em hml, em 07/11/2026, o mapeamento estava `Enabled`, do Stream de hml para o alias `live` de hml, com destino no bucket de hml.

Registro: `list-event-source-mappings` no alias `live` de dev, em 05/11/2026, seção 5.5 do [qa.md de add-notificacao-critica](../openspec/changes/archive/2026-11-11-add-notificacao-critica/qa.md).

```bash
aws lambda list-event-source-mappings \
  --function-name feedback-notificar-critica-dev:live \
  --region sa-east-1 --profile tc4-admin
```

### Entrada

Lote do Stream, com até 10 registros. De cada registro a função lê `eventID`, `eventName`, `dynamodb.SequenceNumber` e os seis atributos de `dynamodb.NewImage`, cada um pelo tipo do contrato: `nota` como N e os outros como S. Valor de outro tipo conta como ausente.

### Saída

Para cada avaliação crítica, uma mensagem no tópico `feedback-admins-<amb>`, que chega por e-mail à assinatura confirmada:

```text
Assunto: Avaliação crítica registrada

Uma avaliação crítica foi registrada.

Urgência: CRITICA
Data de envio: <dataEnvio do item>

Descrição:
<descrição completa, sem corte>
```

A resposta ao Lambda é `{"batchItemFailures":[]}` quando todo o lote foi processado, ou a lista com o número de sequência do registro que falhou.

Conferido em dev em 05/11/2026, com uma descrição curta e uma de 1000 caracteres com acentos: 2 mensagens publicadas, 2 e-mails recebidos pelo Gabriel com o texto inteiro e nenhum e-mail para as avaliações média e baixa ([qa.md de add-notificacao-critica](../openspec/changes/archive/2026-11-11-add-notificacao-critica/qa.md), seção 5.6). Em hml, em 07/11/2026, a avaliação crítica gerou uma publicação e a baixa nenhuma.

### Regras de negócio

- **Segunda checagem no handler.** Registro que não seja `INSERT` com `NewImage.urgencia.S` igual a `CRITICA` é ignorado e conta como processado, mesmo que passe pelo filtro.
- **Conteúdo vem do item persistido.** A data é `dataEnvio`, nunca a data do processamento, e a descrição vai inteira, sem corte nem reticências, até os 1000 caracteres que o registro aceita. O texto sai igual em qualquer reprocessamento.
- **Lote percorrido em ordem.** Na primeira falha ao publicar, a função para e devolve o número de sequência daquele registro. O Lambda retoma dele, e publicar os seguintes só geraria duplicatas.
- **Entrega pelo menos uma vez, sem deduplicação.** O mesmo aviso pode sair mais de uma vez. O tópico não tem deduplicação.
- **A avaliação não depende do aviso.** A função não tem permissão de escrita na tabela, e o registro não tem permissão no tópico.

### Erros tratados e efeito

| Caso | Efeito |
|---|---|
| Falha ao publicar no tópico, inclusive em parte do lote | ERROR `Falha ao publicar aviso`, com o marcador. O registro volta em `batchItemFailures` e é tentado de novo pela política do mapeamento. O item continua na tabela |
| Item crítico sem campo obrigatório ou com campo inválido | Descartado na hora. ERROR `Item inválido descartado`, com `eventID`, os nomes dos campos e o marcador. Métrica `ItemInvalido`. Conta como processado, sem nova tentativa |
| Registro que não é criação crítica | INFO `Registro ignorado`. Conta como processado |
| `Error` que escapa | ERROR `Erro na execução da função`, com o marcador, e o `Error` é relançado. É erro de função, então o Lambda divide o lote e tenta de novo |
| Tempo esgotado | Erro de função, tratado pelo Lambda como o caso anterior. O cliente do SNS desiste em 5 s por chamada, dentro do `Timeout` de 30 s, para a falha de publicação passar pelo `catch` |
| Tentativas ou idade esgotadas | O Lambda descarta o registro, grava o lote no destino de falha e segue com o shard. O aviso daquela avaliação não sai sozinho |

O item inválido foi provocado em dev em 06/11/2026, com um evento sintético sem `descricao`: a resposta foi `{"batchItemFailures":[]}`, nada foi publicado e o alarme disparou e voltou a OK ([qa.md de add-observabilidade-seguranca](../openspec/changes/archive/2026-11-11-add-observabilidade-seguranca/qa.md), seção 6.11). A falha de publicação, a nova entrega do evento e o descarte por tentativas esgotadas não foram provocados numa stack; têm prova só nos testes ([qa.md de add-notificacao-critica](../openspec/changes/archive/2026-11-11-add-notificacao-critica/qa.md), observação C).

Uma `RuntimeException` fora da publicação escaparia do handler sem o marcador e não dispararia o alarme. Pela leitura do código, nenhuma sai hoje. É risco aceito, registrado em [docs/seguranca.md](seguranca.md).

### Variáveis de ambiente

| Variável | Origem no template | Uso |
|---|---|---|
| `POWERTOOLS_SERVICE_NAME` | `notificar-critica` | Campo `service` do log e dimensão `Service` das métricas |
| `TOPICO_ADMINS_ARN` | `!Ref TopicoAdmins` | Tópico do `Publish` e do aquecimento. Sem ela, a função não inicializa |

Mais as três do `Globals`.

### Permissões

Política inline da role `NotificarCriticaRole`, gerada pelo SAM:

| `Sid` | Ações | Recurso | Condição |
|---|---|---|---|
| `PublicarNoTopico` | `sns:Publish` | Tópico `feedback-admins-<amb>` (`!Ref TopicoAdmins`) | |
| `UsarChaveDoTopico` | `kms:Decrypt`, `kms:GenerateDataKey*` | `arn:aws:kms:sa-east-1:<ID_DA_CONTA>:key/*` | `kms:ViaService` igual a `sns.sa-east-1.amazonaws.com` |
| `GuardarLoteDescartado` | `s3:PutObject` | `feedback-relatorios-<ID_DA_CONTA>-<amb>/*` (`${BucketRelatorios.Arn}/*`) | `s3:ResourceAccount` igual à conta da stack |
| `ListarBucketDoDestino` | `s3:ListBucket` | Bucket `feedback-relatorios-<ID_DA_CONTA>-<amb>` (`!GetAtt BucketRelatorios.Arn`) | `s3:ResourceAccount` igual à conta da stack |
| `AquecerCliente` | `sns:GetTopicAttributes` | Tópico `feedback-admins-<amb>` (`!Ref TopicoAdmins`) | |

Mais as três gerenciadas: `AWSLambdaDynamoDBExecutionRole`, que dá a leitura do Stream ao mapeamento, `AWSLambdaBasicExecutionRole` e `AWSXrayWriteOnlyAccess`. A role não tem nenhuma ação sobre a tabela além da leitura do Stream.

- **KMS:** o tópico de administradores usa a chave gerenciada `alias/aws/sns`, cujo ID não é conhecido no template. Por isso o recurso é `key/*`, restrito pela condição `kms:ViaService` ao uso pelo SNS.
- **`s3:PutObject` no bucket inteiro:** quem grava o lote descartado é o próprio Lambda, sempre em `aws/lambda/`, e a função não tem código que use S3. Mas, ao criar o mapeamento, o Lambda exige `PutObject` no bucket inteiro e recusa o prefixo `aws/lambda/*`. Isso foi descoberto no primeiro deploy em dev, em 05/11/2026 ([qa.md de add-notificacao-critica](../openspec/changes/archive/2026-11-11-add-notificacao-critica/qa.md), "Tarefa 7.6"). É risco aceito, registrado em [docs/seguranca.md](seguranca.md).

### Execução

| Item | Valor |
|---|---|
| Memória | 1024 MB, do `Globals` |
| `Timeout` | 30 s, do `Globals` |
| Tempo máximo de cada chamada ao SDK | 5 s (`apiCallTimeout`) por publicação |
| Cliente HTTP | `url-connection-client` |
| Alias e SnapStart | `live`, em versão publicada. O mapeamento aponta para o alias |
| Aquecimento | `GetTopicAttributes` no tópico de administradores |

### Métricas

| Métrica | Quando |
|---|---|
| `ItemInvalido` | Item crítico descartado por campo ausente ou inválido |

É a única métrica do Powertools desta função. A função só descarrega métricas quando a invocação contou alguma, porque o Powertools grava `No metrics were emitted` em WARN a cada descarga vazia, e isso poria um WARN em toda invocação com sucesso.

A métrica do alarme, `FalhasNotificarCritica`, vem do filtro de log descrito em [docs/monitoramento.md](monitoramento.md). A falha de publicação não aparece na métrica `Errors` do Lambda, porque a invocação termina normalmente, com `batchItemFailures`; só o filtro de log a enxerga.

### Logs

| Mensagem | Nível | Campos além do layout | Marcador |
|---|---|---|---|
| `Aviso publicado` | INFO | `id`, `eventID`, `messageId` | não |
| `Registro ignorado` | INFO | `eventID`, `eventName` | não |
| `Falha ao publicar aviso` | ERROR | `id`, `eventID`, `sequencia`, `classe`, `codigoErro`, `idRequisicao` | sim |
| `Item inválido descartado` | ERROR | `eventID`, `campos`, com nomes de campo | sim |
| `Erro na execução da função` | ERROR | `classe` | sim |
| `Falha ao emitir métrica` | WARN | `classe` | não |
| `Cliente aquecido` | INFO | `servico`, `operacao`, `duracaoMs`, na publicação da versão | não |
| `Falha no aquecimento` | WARN | `servico`, `classe`, `codigoErro` se houver | não |

Nem o evento recebido nem a mensagem publicada vão para o log. No X-Ray, a publicação fica no subsegmento `publicarAviso`, com a anotação `id`, e o instrumentor acrescenta o subsegmento do SNS.

Na leitura completa do grupo de dev, em 05/11/2026, nenhuma linha trouxe a descrição, e o trace também não ([qa.md de add-notificacao-critica](../openspec/changes/archive/2026-11-11-add-notificacao-critica/qa.md), seção 5.7).

### Alarme

`feedback-falha-notificar-critica-<amb>`. Cobre a falha de publicação, inclusive parcial, o item descartado, o `Error` que escapa e o tempo esgotado. Configuração em [docs/monitoramento.md](monitoramento.md).

### Procedimentos: reprocessamento e lotes descartados

**Política.** Um registro com falha de publicação é tentado de novo até a menor das duas condições: 5 tentativas ou 1 hora de idade. A documentação da AWS consultada não informa o intervalo entre tentativas em stream, então não dá para garantir quanto tempo de indisponibilidade do SNS a política cobre.

**Lotes descartados.** Quando as tentativas ou a idade se esgotam, o Lambda grava o lote no bucket de relatórios, no prefixo `aws/lambda/`, definido pelo serviço:

```text
aws/lambda/<UUID do mapeamento>/<shard>/AAAA/MM/DD/<horário>-<uuid>
```

O objeto traz `requestContext`, com a `condition` do descarte, `DDBStreamBatchInfo` e, em `payload`, os registros completos do Stream, com os seis atributos do item, **inclusive a descrição**. O bucket não tem regra de expiração, então o objeto fica até ser apagado à mão. Isso foi visto em dev em 05/11/2026: quando o mapeamento nasceu, as 7 avaliações críticas antigas do Stream, com mais de 1 hora, foram direto para 5 objetos com `condition` igual a `RecordAgeExceeded`, sem invocar a função e sem e-mail. As avaliações média e baixa antigas não foram para o destino, porque o filtro vem antes do descarte ([qa.md de add-notificacao-critica](../openspec/changes/archive/2026-11-11-add-notificacao-critica/qa.md), "Itens críticos antigos no destino de falha"). Nesses objetos, `approximateInvokeCount` veio igual a 1 mesmo sem invocação (observação J do mesmo arquivo), então o campo não serve para saber se houve tentativa de aviso.

Não vão para `aws/lambda/`: os registros que o filtro não aceita, os que a função ignora e o item crítico inválido, que é descartado pela própria função.

**Refazer um aviso perdido** é manual. O Stream guarda os registros por 24 horas, e o objeto de `aws/lambda/` é a fonte depois disso. Não há comando nem função para reenviar.

Registro: listagem do prefixo `aws/lambda/` do bucket de dev, em 05/11/2026, seção "Itens críticos antigos no destino de falha" do [qa.md de add-notificacao-critica](../openspec/changes/archive/2026-11-11-add-notificacao-critica/qa.md).

```bash
BUCKET=$(aws cloudformation describe-stacks \
  --stack-name feedback-dev \
  --query "Stacks[0].Outputs[?OutputKey=='BucketRelatoriosNome'].OutputValue" \
  --output text \
  --region sa-east-1 --profile tc4-admin)

aws s3 ls "s3://$BUCKET/aws/lambda/" --recursive --region sa-east-1 --profile tc4-admin
```

Os objetos de `aws/lambda/` têm dado do aluno. Baixe fora do repositório. Antes de remover uma stack, o bucket precisa ser esvaziado, como explica a seção de implantação do [README](../README.md).

## gerar-relatorio-semanal

### Responsabilidade

Consolida as avaliações dos 7 dias anteriores à data de referência num relatório em texto, guarda no bucket privado de relatórios e avisa os administradores com o resumo e a localização. Roda toda segunda-feira pelo agendamento e pode ser acionada à mão em qualquer dia. Código em [`GerarRelatorioSemanalHandler.java`](../functions/gerar-relatorio-semanal/src/main/java/br/com/fiap/feedback/relatorio/GerarRelatorioSemanalHandler.java) e [`GeradorRelatorio.java`](../functions/gerar-relatorio-semanal/src/main/java/br/com/fiap/feedback/relatorio/GeradorRelatorio.java).

### Gatilho

Evento `Agenda`, do tipo `ScheduleV2`, que cria o agendamento `feedback-relatorio-semanal-<amb>` no EventBridge Scheduler:

| Propriedade | Valor |
|---|---|
| `ScheduleExpression` | `cron(0 8 ? * MON *)` |
| `ScheduleExpressionTimezone` | `America/Sao_Paulo` |
| `State` | `ENABLED` |
| `Input` | `{}`, então a referência é o dia da execução em São Paulo |
| `FlexibleTimeWindow` | `Mode: OFF` |
| `RetryPolicy` | `MaximumRetryAttempts: 3`, `MaximumEventAgeInSeconds: 3600` |
| Alvo | Alias `live` da função |

São duas camadas de repetição, pela documentação da AWS citada na D8 do [design de add-relatorio-semanal](../openspec/changes/archive/2026-11-11-add-relatorio-semanal/design.md): o `RetryPolicy` cobre a entrega do Scheduler ao Lambda, e a idade de 1 hora mantém a repetição na própria segunda, com a mesma referência. Como o Scheduler invoca de forma assíncrona, um erro da função é repetido pelo Lambda mais duas vezes, com 1 e 2 minutos de espera. Cada repetição relê e regrava a mesma chave, e pode gerar um segundo aviso igual.

O agendamento implantado foi conferido em dev em 06/11/2026, com todos os valores acima e o alvo no alias `live` ([qa.md de add-relatorio-semanal](../openspec/changes/archive/2026-11-11-add-relatorio-semanal/qa.md), seção 5.4), e em hml em 07/11/2026, `ENABLED` e apontando para o alias de hml.

Registro: `get-schedule` em dev, em 06/11/2026, seção 5.4 do [qa.md de add-relatorio-semanal](../openspec/changes/archive/2026-11-11-add-relatorio-semanal/qa.md).

```bash
aws scheduler get-schedule --name feedback-relatorio-semanal-dev --region sa-east-1 --profile tc4-admin
```

O acionamento manual não é um segundo gatilho: é uma invocação direta do alias, descrita em "Procedimentos".

### Entrada

Objeto JSON com `dataReferencia` opcional:

| Valor de `dataReferencia` | Resultado |
|---|---|
| Ausente ou `null`, como no `{}` do agendamento | Dia atual em America/Sao_Paulo, calculado dentro do handler |
| Texto `yyyy-MM-dd` com data que existe, no passado ou no futuro | Usa a data informada |
| Vazio, outro formato (`09/11/2026`, `+12026-01-01`), data inexistente (`2026-02-30`) ou valor que não é texto | Rejeita antes de qualquer leitura |

### Saída

**Resposta da invocação**, no sucesso, com os campos em ordem não garantida e sem o nome do bucket, que tem o ID da conta:

| Campo | Exemplo |
|---|---|
| `dataReferencia` | `2026-11-04` |
| `chave` | `relatorios/2026-11-04.txt` |
| `quantidadeAvaliacoes` | `3` |

**Objeto no bucket** `feedback-relatorios-<ID_DA_CONTA>-<amb>`, na chave `relatorios/<dataReferencia>.txt`, em texto puro UTF-8, com `ContentType: text/plain; charset=utf-8` e criptografia `AES256` do bucket. Exemplo com notas 2, 6 e 9 e referência `2026-11-09`:

```text
RELATÓRIO SEMANAL DE AVALIAÇÕES
Ambiente: dev
Data de referência: 2026-11-09
Janela: 2026-11-02 a 2026-11-08

Média das notas: 5,67

Quantidade por urgência
CRITICA: 1
MEDIA: 1
BAIXA: 1

Quantidade por dia
2026-11-02: 0
2026-11-03: 2
2026-11-04: 0
2026-11-05: 1
2026-11-06: 0
2026-11-07: 0
2026-11-08: 0

Avaliações (3)

Data de envio: 2026-11-03 | Urgência: CRITICA
Descrição: Aula confusa

Data de envio: 2026-11-03 | Urgência: MEDIA
Descrição: Aula regular

Data de envio: 2026-11-05 | Urgência: BAIXA
Descrição: Aula ótima
```

**Aviso no tópico** `feedback-admins-<amb>`, a cada relatório guardado, com os mesmos números e sem descrições nem notas individuais. A mesma execução do exemplo gera:

```text
Assunto: Relatório semanal 2026-11-09

Relatório semanal gerado.

Ambiente: dev
Data de referência: 2026-11-09
Janela: 2026-11-02 a 2026-11-08

Média das notas: 5,67

Quantidade por urgência
CRITICA: 1
MEDIA: 1
BAIXA: 1

Quantidade por dia
2026-11-02: 0
2026-11-03: 2
2026-11-04: 0
2026-11-05: 1
2026-11-06: 0
2026-11-07: 0
2026-11-08: 0

Bucket: feedback-relatorios-<ID_DA_CONTA>-dev
Chave: relatorios/2026-11-09.txt
Localização: s3://feedback-relatorios-<ID_DA_CONTA>-dev/relatorios/2026-11-09.txt

O bucket é privado. Acesse com uma credencial da conta.
```

O aviso não traz link pré-assinado: o link expiraria junto com a credencial temporária da role e funcionaria para quem recebesse o e-mail encaminhado. O nome do bucket inclui o ID da conta, então o e-mail não vai para arquivo versionado.

Conferido em dev em 06/11/2026: o objeto ficou idêntico, byte a byte, ao texto montado a partir das 7 partições lidas na tabela ([qa.md de add-relatorio-semanal](../openspec/changes/archive/2026-11-11-add-relatorio-semanal/qa.md), seção 5.5). O mesmo em hml, em 07/11/2026 ([qa.md de add-fundacao-projeto](../openspec/changes/archive/2026-11-11-add-fundacao-projeto/qa.md), item 3 da release em hml).

### Regras de negócio

- **Janela:** os 7 dias civis anteriores à referência, em America/Sao_Paulo, com o dia da referência fora. Referência `2026-11-09` dá a janela de `2026-11-02` a `2026-11-08`; referência `2026-11-04`, de `2026-10-28` a `2026-11-03`. Por isso, uma avaliação enviada hoje só entra se a referência for amanhã.
- **Leitura:** uma `Query` no índice `por-data` por dia da janela, com o paginador percorrendo todas as páginas. A lista sai por dia e, dentro do dia, na ordem de `criadoEm`. Nunca há resultado parcial: a lista só fica pronta depois das 7 consultas.
- **Média:** duas casas, com vírgula, e o empate arredonda para cima: 5,125 vira `5,13`. Com só avaliações de nota 0, a média é `0,00`.
- **Semana vazia:** relatório e aviso saem do mesmo jeito, com `Média das notas: sem avaliações na janela`, `Avaliações (0)` e todos os totais em zero.
- **Totais completos:** sempre os 7 dias e as 3 urgências, mesmo com zero.
- **Urgência como gravada no item**, sem recálculo. A nota individual não aparece em nenhum dos dois textos.
- **Um relatório por data de referência.** Reexecutar para a mesma data substitui o objeto e publica um novo aviso. Datas diferentes geram objetos separados.
- **Ordem dos efeitos:** validar a data, ler, consolidar, gravar e publicar. Cada passo só roda se o anterior der certo.

### Erros tratados e efeito

Toda falha termina a invocação com `FunctionError`, e a mensagem vai para a resposta. As mensagens são fixas:

| Caso | Mensagem | Efeito |
|---|---|---|
| Data de referência rejeitada: vazia, em outro formato, inexistente ou que não é texto | `Data de referência inválida. Informe uma data existente no formato yyyy-MM-dd.` | Nada lido, gravado ou avisado. Um relatório já guardado fica como está. ERROR `Data de referência rejeitada`, **sem** o marcador. Métrica `DataReferenciaRejeitada` |
| Falha na leitura de qualquer um dos 7 dias, inclusive no meio de um dia | `Falha ao gerar o relatório semanal. Consulte os logs da função.` | Nada gravado nem avisado. ERROR `Falha ao gerar relatório`, com o marcador. Métrica `ErroInterno` |
| Item inválido na janela | A mesma | Tratado como falha de leitura: ERROR `Item de avaliação inválido`, com o `id` e os nomes dos campos, seguido de `Falha ao gerar relatório` com o marcador. O relatório daquela janela fica bloqueado até o item ser corrigido |
| Falha na gravação no bucket | A mesma | Nenhum aviso publicado. Mesmo log e mesma métrica |
| Falha na publicação do aviso | A mesma | O relatório fica guardado. Reexecutar para a mesma data corrige. Mesmo log e mesma métrica |
| `Error` que escapa | | ERROR `Erro na execução da função`, com o marcador, e o `Error` é relançado |
| Tempo esgotado | | A invocação termina sem passar pelo `catch`. Cada chamada ao SDK desiste em 10 s, e as 9 chamadas de uma execução sem página extra somam no máximo 90 s, abaixo do `Timeout` de 120 s |

Na data rejeitada, a resposta traz também `errorType` e `stackTrace`, com nomes de classe, arquivo e linha, sem dado do aluno nem da conta. É risco aceito, registrado em [docs/seguranca.md](seguranca.md). As três rejeições da spec foram conferidas em dev em 06/11/2026, sem objeto novo e sem publicação ([qa.md de add-relatorio-semanal](../openspec/changes/archive/2026-11-11-add-relatorio-semanal/qa.md), seção 5.5, item c). As falhas de leitura, gravação e publicação têm prova só nos testes.

### Variáveis de ambiente

| Variável | Origem no template | Uso |
|---|---|---|
| `POWERTOOLS_SERVICE_NAME` | `gerar-relatorio-semanal` | Campo `service` do log e dimensão `Service` das métricas |
| `TABELA_AVALIACOES` | `!Ref TabelaAvaliacoes` | Tabela das consultas ao índice `por-data` |
| `BUCKET_RELATORIOS` | `!Ref BucketRelatorios` | Bucket do relatório e do `HeadBucket` do aquecimento |
| `TOPICO_ADMINS_ARN` | `!Ref TopicoAdmins` | Tópico do aviso e do aquecimento |

Mais as três do `Globals`. Esta é a única função que lê `AMBIENTE`. Sem `TABELA_AVALIACOES`, `BUCKET_RELATORIOS`, `TOPICO_ADMINS_ARN` ou `AMBIENTE`, a função não inicializa.

### Permissões

Política inline da role `GerarRelatorioSemanalRole`, gerada pelo SAM:

| `Sid` | Ações | Recurso | Condição |
|---|---|---|---|
| `ConsultarIndicePorData` | `dynamodb:Query` | Índice `feedback-avaliacoes-<amb>/index/por-data` (`${TabelaAvaliacoes.Arn}/index/por-data`) | |
| `GravarRelatorio` | `s3:PutObject` | `feedback-relatorios-<ID_DA_CONTA>-<amb>/relatorios/*` (`${BucketRelatorios.Arn}/relatorios/*`) | |
| `PublicarNoTopico` | `sns:Publish` | Tópico `feedback-admins-<amb>` (`!Ref TopicoAdmins`) | |
| `UsarChaveDoTopico` | `kms:Decrypt`, `kms:GenerateDataKey*` | `arn:aws:kms:sa-east-1:<ID_DA_CONTA>:key/*` | `kms:ViaService` igual a `sns.sa-east-1.amazonaws.com` |
| `AquecerClienteSns` | `sns:GetTopicAttributes` | Tópico `feedback-admins-<amb>` (`!Ref TopicoAdmins`) | |
| `AquecerClienteS3` | `s3:ListBucket` | Bucket `feedback-relatorios-<ID_DA_CONTA>-<amb>` (`!GetAtt BucketRelatorios.Arn`) | `s3:ResourceAccount` igual à conta da stack |

Mais `AWSLambdaBasicExecutionRole` e `AWSXrayWriteOnlyAccess`. A role não tem `dynamodb:GetItem`, `dynamodb:Scan` nem `s3:GetObject`: grava relatórios, mas não lê nenhum. O `s3:ListBucket` existe só para o `HeadBucket` do aquecimento e lista chaves, que não têm dado do aluno.

**Role do agendamento.** O SAM gera também `GerarRelatorioSemanalAgendaRole`, assumida só por `scheduler.amazonaws.com`, sem política gerenciada e com uma política inline de `lambda:InvokeFunction` no alias `live` da função. O `Globals` não alcança essa role, então o boundary `feedback-limite-funcoes-<amb>` é declarado no próprio evento (`PermissionsBoundary` do `ScheduleV2`). Sem ele, o deploy teria `iam:CreateRole` negado. Conferido em dev em 06/11/2026 ([qa.md de add-relatorio-semanal](../openspec/changes/archive/2026-11-11-add-relatorio-semanal/qa.md), seção 5.4).

### Execução

| Item | Valor |
|---|---|
| Memória | 1024 MB, do `Globals` |
| `Timeout` | 120 s, na função |
| Tempo máximo de cada chamada ao SDK | 10 s (`apiCallTimeout`), para DynamoDB, S3 e SNS |
| Cliente HTTP | `url-connection-client` |
| Alias e SnapStart | `live`, em versão publicada. O agendamento e o acionamento manual usam o alias |
| Aquecimento | `Query` numa partição vazia, `HeadBucket` e `GetTopicAttributes` |

Todas as avaliações da janela ficam em memória. Com o volume do projeto, isso é folgado. Cada página extra do paginador é mais uma chamada de até 10 s; com 12 ou mais chamadas, o pior caso alcança o `Timeout`. Página extra só aparece com mais de 1 MB de avaliações no mesmo dia.

### Métricas

| Métrica | Quando |
|---|---|
| `RelatorioGerado` | Relatório gravado e aviso publicado |
| `DataReferenciaRejeitada` | Data de referência rejeitada |
| `ErroInterno` | Qualquer outra falha |

`RelatorioGerado` e `DataReferenciaRejeitada` foram vistas em dev em 06/11/2026, com a dimensão `Service` igual a `gerar-relatorio-semanal`; `ErroInterno` não foi exercitada. Em hml, em 07/11/2026, `RelatorioGerado` saiu no namespace `FeedbackServerless/hml`.

A métrica `Errors` do Lambda conta também a data rejeitada, porque ela termina com `FunctionError`. `DataReferenciaRejeitada` separa a entrada inválida da falha do sistema. O alarme não usa `Errors`.

A métrica do alarme, `FalhasGerarRelatorioSemanal`, vem do filtro de log descrito em [docs/monitoramento.md](monitoramento.md).

### Logs

| Mensagem | Nível | Campos além do layout | Marcador |
|---|---|---|---|
| `Relatório gravado` | INFO | `dataReferencia`, `bucket`, `chave` | não |
| `Relatório gerado` | INFO | `dataReferencia`, `quantidadeTotal`, `totaisPorUrgencia`, `bucket`, `chave` | não |
| `Data de referência rejeitada` | ERROR | `classe` | não |
| `Item de avaliação inválido` | ERROR | `id`, `campos` | não; a linha seguinte tem |
| `Falha ao gerar relatório` | ERROR | `classe`, `codigoErro`, `idRequisicao` | sim |
| `Erro na execução da função` | ERROR | `classe` | sim |
| `Falha ao emitir métrica` | WARN | `classe` | não |
| `Cliente aquecido` | INFO | `servico`, `operacao`, `duracaoMs`, três por versão publicada | não |
| `Falha no aquecimento` | WARN | `servico`, `classe`, `codigoErro` se houver | não |

`Relatório gravado` sai logo depois do `PutObject`, separado do log final: se a publicação falhar, ele mostra que o objeto ficou guardado. A média fica fora dos logs, porque, com uma única avaliação na janela, ela revelaria a nota. O campo `bucket` traz o nome do bucket, com o ID da conta, que não é dado do aluno.

Na rejeição da data, o runtime também escreve no log uma linha em texto, fora do JSON, com a mensagem fixa e a pilha da exceção, sem o valor recebido ([qa.md de add-relatorio-semanal](../openspec/changes/archive/2026-11-11-add-relatorio-semanal/qa.md), observação M).

No X-Ray, as etapas ficam nos subsegmentos `lerAvaliacoes`, `gravarRelatorio` e `publicarAviso`, cada um com a anotação `dataReferencia`.

Na leitura completa do grupo de dev, em 06/11/2026, nenhuma linha trouxe descrição, nota ou média ([qa.md de add-relatorio-semanal](../openspec/changes/archive/2026-11-11-add-relatorio-semanal/qa.md), seção 5.8).

### Alarme

`feedback-falha-gerar-relatorio-semanal-<amb>`. Cobre a falha de leitura, de gravação ou de publicação, o `Error` que escapa e o tempo esgotado. A data rejeitada não dispara. Configuração em [docs/monitoramento.md](monitoramento.md).

### Procedimentos: acionamento manual e download

**Acionar pelo alias `live`**, em qualquer dia, sem depender do agendamento. Para hml e prod, troque o sufixo do nome da função.

Registro: invocações do alias `live` de dev com estas opções, em 06/11/2026, seção 5.5 do [qa.md de add-relatorio-semanal](../openspec/changes/archive/2026-11-11-add-relatorio-semanal/qa.md), e em hml em 07/11/2026, item 3 da seção "Release 1.0.0 em hml (07/11/2026)" do [qa.md de add-fundacao-projeto](../openspec/changes/archive/2026-11-11-add-fundacao-projeto/qa.md).

```bash
aws lambda invoke \
  --function-name feedback-gerar-relatorio-semanal-dev:live \
  --region sa-east-1 --profile tc4-admin \
  --cli-binary-format raw-in-base64-out \
  --cli-read-timeout 150 \
  --payload '{"dataReferencia":"2026-11-04"}' \
  resposta.json
```

- `:live` é o que garante a versão com SnapStart. Sem ele, a chamada vai para `$LATEST` sem nenhum aviso.
- `--cli-binary-format raw-in-base64-out` permite enviar o payload em texto no AWS CLI v2.
- `--cli-read-timeout 150` fica acima do `Timeout` de 120 s, porque o padrão do CLI é 60.
- Para usar o dia atual como referência, troque o payload por `'{}'`. Para incluir as avaliações de hoje, use a data de amanhã.
- Em caso de falha, a saída do comando traz `FunctionError`, e a mensagem fica em `resposta.json`.

**Baixar o relatório.** A resposta não traz o bucket. O nome está no output `BucketRelatoriosNome` da stack e no aviso. Troque a chave do exemplo pelo valor de `chave`:

Registro: objeto baixado com `aws s3 cp` e comparado byte a byte com a tabela, em dev em 06/11/2026, seção 5.5 do [qa.md de add-relatorio-semanal](../openspec/changes/archive/2026-11-11-add-relatorio-semanal/qa.md).

```bash
BUCKET=$(aws cloudformation describe-stacks \
  --stack-name feedback-dev \
  --query "Stacks[0].Outputs[?OutputKey=='BucketRelatoriosNome'].OutputValue" \
  --output text \
  --region sa-east-1 --profile tc4-admin)

aws s3 cp "s3://$BUCKET/relatorios/2026-11-04.txt" "$HOME/relatorio-2026-11-04.txt" --region sa-east-1 --profile tc4-admin
```

A credencial usada precisa de `s3:GetObject`, que a role da função não tem. O bucket bloqueia acesso público e nega acesso sem TLS; uma leitura anônima recebeu 403 em dev ([qa.md de add-relatorio-semanal](../openspec/changes/archive/2026-11-11-add-relatorio-semanal/qa.md), seção 5.7). O arquivo tem as descrições escritas pelos alunos, e o `.gitignore` não o cobre, então baixe fora do repositório.

Os objetos de `relatorios/` também impedem a remoção do bucket junto com a stack. O esvaziamento antes do `sam delete` está na seção de implantação do [README](../README.md).
