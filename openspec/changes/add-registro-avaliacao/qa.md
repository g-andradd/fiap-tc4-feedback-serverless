# QA: add-registro-avaliacao

Tarefas 5.1 e 5.2. Validação local feita em 04/11/2026 sobre o working tree, sem commit. A stack ainda não recebeu este change: nenhum comando `aws`, `sam deploy` ou `sam build` foi executado e nada tocou a conta. As tarefas 6.3 a 6.8 ficam para depois do merge.

## Resultado

| Status | Quantidade |
|---|---|
| ok (executado) | 47 |
| ok (por configuração) | 0 |
| não coberto | 1 |
| falha | 0 |
| **Total de cenários** | **48** |

**O change ainda não está conforme:** cenário sem cobertura conta como falha, e há 1 nessa condição, `Avaliação encontrada no índice por data`, que depende da tarefa 6.6.

Significado dos status:
- **ok (executado):** o teste rodou em `mvn -B verify` e a asserção prova o que o cenário pede no nível do handler. Neste relatório, nenhum cenário foi executado contra a stack.
- **ok (por configuração):** o recurso garante o comportamento, mas ele não foi exercitado. Não equivale a teste.
- **não coberto:** não há evidência do que o cenário pede. Conta como falha.
- **falha:** o comportamento observado contraria o cenário.

## Limite da evidência

Todos os 47 cenários com ok usam `DynamoDbClient` simulado com Mockito, `Metrics` simulado e `RastreadorFalso`. Isso significa:

- **O que está provado:** dado o evento, o handler responde o status e o corpo esperados e monta o `PutItemRequest` esperado, ou não chama `putItem`.
- **O que não está provado:** a gravação real, a rota no API Gateway, o evento real gerado pela HTTP API, as permissões, o SnapStart e os logs no CloudWatch.

Na coluna Limite da matriz:
- **SIM:** só cliente simulado. Quando o cenário fala em "tabela" ou "item persistido", a prova é o `PutItemRequest` capturado, não um item na tabela. A tarefa 6.6 repete o caminho na stack.
- **SIM, sem réplica:** só cliente simulado, e a stack não reproduz o caso de forma simples, porque depende de relógio fixo ou de falha provocada. Esses cenários não têm previsão de execução real.
- **LOG:** captura em memória com o layout real do `log4j2.xml` e nível `ALL`, com cliente simulado. O SDK real não roda nesses testes. A tarefa 6.7 confere no CloudWatch.

Legenda das classes de teste:
- `H`: `br.com.fiap.feedback.registro.RegistrarAvaliacaoHandlerTest`, com a classe aninhada no nome
- `L`: `br.com.fiap.feedback.registro.LogsRegistrarAvaliacaoTest`
- `C`: `br.com.fiap.feedback.dominio.CarimboDeTempoTest`
- `U`: `br.com.fiap.feedback.dominio.UrgenciaTest`
- `V`: `br.com.fiap.feedback.dominio.ValidadorAvaliacaoTest`

## Verificações locais (tarefa 5.2)

| Verificação | Resultado |
|---|---|
| `mvn -B verify` | `BUILD SUCCESS`. `dominio`: `Tests run: 79, Failures: 0, Errors: 0, Skipped: 0`. `registrar-avaliacao`: `Tests run: 124, Failures: 0, Errors: 0, Skipped: 0`. Antes dos testes do QA, o segundo módulo tinha 113 |
| `sam validate --lint` | `template.yaml is a valid SAM Template` |
| `openspec validate add-registro-avaliacao --strict` | `Change 'add-registro-avaliacao' is valid` |
| Imports de `functions/dominio/src` | 0 ocorrências de `software.amazon`, `com.amazonaws`, `com.fasterxml`, `org.slf4j` e `org.apache`, em `main` e `test`. Os imports de `main` são só `java.*` |
| `dependency:tree` de `dominio` | Escopo `compile`: só o próprio módulo, sem dependências. Todos os escopos: só JUnit, AssertJ e o que eles trazem. 0 ocorrências de `software.amazon.awssdk`, `com.amazonaws` e `jackson` |

O build local usa Microsoft OpenJDK 25 (25.0.5.0.2). O resultado local não comprova o build em Corretto 25: essa comprovação vem do CI, na tarefa 6.1.

## Matriz: registro-avaliacao

### Registro de avaliação

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Avaliação válida registrada | ok (executado) | `H.Registro.avaliacaoValidaRegistrada` | SIM |
| Envios iguais geram avaliações distintas | ok (executado) | `H.Registro.enviosIguaisGeramAvaliacoesDistintas` | SIM. Dois `putItem` com os mesmos `id` das respostas, e os `id` são diferentes |

### Validação de descricao

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Descrição com 1 caractere aceita | ok (executado) | `H.ValidacaoDescricao.umCaractere`, `V.descricaoCom1CaractereAceita` | SIM |
| Descrição com 1000 caracteres aceita | ok (executado) | `H.ValidacaoDescricao.milCaracteres`, `V.descricaoCom1000CaracteresAceita` | SIM |
| Descrição com 1001 caracteres rejeitada | ok (executado) | `H.ValidacaoDescricao.milEUmCaracteres`, `V.descricaoCom1001CaracteresRejeitada` | SIM |
| Caractere fora do plano básico conta como um | ok (executado) | `H.ValidacaoDescricao.milEmojis`, `V.milEmojisContamComoMilCaracteres`, que confere 2000 unidades UTF-16 | SIM |
| Descrição ausente | ok (executado) | `H.ValidacaoDescricao.ausente` | SIM |
| Descrição nula | ok (executado) | `H.ValidacaoDescricao.nula` | SIM |
| Descrição vazia | ok (executado) | `H.ValidacaoDescricao.vazia` | SIM |
| Descrição só com espaços | ok (executado) | `H.ValidacaoDescricao.soEspacos` | SIM |
| Descrição só com tabulação e quebra de linha | ok (executado) | `H.ValidacaoDescricao.soTabulacaoEQuebra`, com `\t\n\r\n` | SIM |
| Descrição que não é texto | ok (executado) | `H.ValidacaoDescricao.naoTexto` | SIM |
| Descrição persistida como recebida | ok (executado) | `H.ValidacaoDescricao.persistidaComoRecebida` | SIM |

Os cenários de rejeição usam `assertRejeitadoPor`, que confere status 400, `erro` igual a `ENTRADA_INVALIDA`, `campos` exatamente com o nome esperado e nenhuma chamada a `putItem`.

### Validação de nota

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Nota mínima aceita | ok (executado) | `H.ValidacaoNota.minima` | SIM |
| Nota máxima aceita | ok (executado) | `H.ValidacaoNota.maxima` | SIM |
| Nota abaixo do mínimo | ok (executado) | `H.ValidacaoNota.rejeitada`, valor `-1` | SIM |
| Nota acima do máximo | ok (executado) | `H.ValidacaoNota.rejeitada`, valor `11` | SIM |
| Nota ausente | ok (executado) | `H.ValidacaoNota.ausente` | SIM |
| Nota nula | ok (executado) | `H.ValidacaoNota.rejeitada`, valor `null` | SIM |
| Nota decimal | ok (executado) | `H.ValidacaoNota.rejeitada`, valor `7.5` | SIM |
| Nota decimal com parte fracionária zero | ok (executado) | `H.ValidacaoNota.rejeitada`, valor `7.0` | SIM |
| Nota em notação de expoente | ok (executado) | `H.ValidacaoNota.rejeitada`, valor `1e1` | SIM |
| Nota como texto | ok (executado) | `H.ValidacaoNota.rejeitada`, valor `"7"` | SIM |
| Nota inteira muito grande | ok (executado) | `H.ValidacaoNota.rejeitada`, valor `99999999999999999999` | SIM |

### Rejeição de corpo malformado

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| JSON truncado | ok (executado) | `H.CorpoMalformado.truncado`, com o corpo literal da spec | SIM |
| Corpo vazio | ok (executado) | `H.CorpoMalformado.vazio` (corpo nulo), `H.CorpoMalformado.textoVazio` | SIM. O formato real de "sem corpo" no evento da HTTP API só é visto na 6.6 |
| JSON que não é objeto | ok (executado) | `H.CorpoMalformado.naoObjeto` | SIM |

Esses testes usam `assertCorpoInvalido`, que confere status 400, `erro` igual a `CORPO_INVALIDO`, a mensagem, `campos` vazio e nenhuma chamada a `putItem`.

### Entrada inválida não persiste

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Validação de campo reprovada | ok (executado) | `verify(cliente, never()).putItem(...)` em `assertRejeitadoPor`, usado por todos os testes de rejeição de campo de `H` | SIM. A verificação cobre o único caminho de escrita do código. A 6.6 pode conferir a contagem na tabela |
| Corpo malformado | ok (executado) | `verify(cliente, never()).putItem(...)` em `assertCorpoInvalido`, usado por `H.CorpoMalformado` | SIM. Mesma observação |

### Classificação de urgência

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Nota 0 | ok (executado) | `H.Classificacao.respostaEItemTrazemAUrgencia`, `0, CRITICA`; `U.classificaOsLimitesDaTabela` | SIM |
| Nota 3 | ok (executado) | `H.Classificacao.respostaEItemTrazemAUrgencia`, `3, CRITICA`; `U.classificaOsLimitesDaTabela` | SIM |
| Nota 4 | ok (executado) | `H.Classificacao.respostaEItemTrazemAUrgencia`, `4, MEDIA`; `U.classificaOsLimitesDaTabela` | SIM |
| Nota 6 | ok (executado) | `H.Classificacao.respostaEItemTrazemAUrgencia`, `6, MEDIA`; `U.classificaOsLimitesDaTabela` | SIM |
| Nota 7 | ok (executado) | `H.Classificacao.respostaEItemTrazemAUrgencia`, `7, BAIXA`; `U.classificaOsLimitesDaTabela` | SIM |
| Nota 10 | ok (executado) | `H.Classificacao.respostaEItemTrazemAUrgencia`, `10, BAIXA`; `U.classificaOsLimitesDaTabela` | SIM |
| Urgência informada pelo cliente é ignorada | ok (executado) | `H.Classificacao.urgenciaDoClienteIgnorada` | SIM |

### Data de envio atribuída pelo sistema

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Último segundo do dia em São Paulo | ok (executado) | `H.DataEnvio.ultimoSegundo`, `C.ultimoSegundoDoDiaEmSaoPauloFicaNoDiaAnterior` | SIM, sem réplica. Usa `Clock.fixed` |
| Primeiro segundo do dia em São Paulo | ok (executado) | `H.DataEnvio.primeiroSegundo`, `C.primeiroSegundoDoDiaEmSaoPauloJaEhONovoDia` | SIM, sem réplica. Usa `Clock.fixed` |
| Data informada pelo cliente é ignorada | ok (executado) | `H.DataEnvio.dataDoClienteIgnorada` | SIM |
| Avaliação encontrada no índice por data | **não coberto** | Nenhum teste. Existe só o indício: `dataEnvio` e `criadoEm` são gravados como texto, no formato do índice, e o `template.yaml` declara `por-data` com essas chaves | Fecha com a tarefa 6.6. A 6.6 consulta pela data gravada, porque na stack não dá para fixar o dia `2026-11-10` do cenário |

### Identificador, respostas e falha interna

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Identificador informado pelo cliente é ignorado | ok (executado) | `H.Identificador.idDoClienteIgnorado` | SIM |
| Corpo da resposta de sucesso | ok (executado) | `H.RespostasHttp.sucesso` | SIM |
| Um campo inválido | ok (executado) | `H.RespostasHttp.umCampoInvalido`, com o corpo exato. O status 400 foi acrescentado pelo QA, porque o teste não o conferia | SIM |
| Dois campos inválidos | ok (executado) | `H.RespostasHttp.doisCamposInvalidos` | SIM |
| Falha ao persistir | ok (executado) | `H.FalhaInterna.falhaDoServico`, com ARN na mensagem da exceção, `falhaDoCliente`, `tempoEsgotadoNoSdk` e `falhaInesperada` | SIM, sem réplica. A exceção é lançada pelo mock. A sonda abaixo mostra o mesmo 500 com o SDK real e HTTP falso |

### Dados do aluno fora dos logs em nível INFO

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Registro bem-sucedido | ok (executado) | `L.registroBemSucedido`, `L.sucessoRegistraIdEUrgencia` | LOG. Confere o marcador em todo o texto e a ausência dos atributos `nota` e `descricao` |
| Entrada rejeitada | ok (executado) | `L.entradaRejeitada`, com o marcador e `987654` | LOG |
| Corpo malformado | ok (executado) | `L.corpoMalformado`, com 5 corpos, e `L.motivoNaoTemDadoDeEntrada`, com 11 caminhos | LOG |

`L.capturaFunciona` prova que o appender enxerga todos os níveis e o texto de exceções. Sem isso, as verificações de ausência passariam com a captura quebrada.

## Defeitos

### D1. Número com mais de 1000 dígitos derruba o corpo inteiro (severidade baixa)

Com `{"descricao":"a","nota":5,"extra":<1001 dígitos>}`, a resposta observada é 400 com `CORPO_INVALIDO` e `motivo` igual a `com.fasterxml.jackson.core.exc.StreamConstraintsException`. O corpo é um objeto JSON bem formado, e a D8 do `proposal.md` diz que campos extras são ignorados "sem causar rejeição". Com o mesmo número em `nota`, a resposta também é `CORPO_INVALIDO`, com `campos` vazio, em vez de apontar o campo, como pedem a D6 do `proposal.md` e o requisito de erro de validação.

O limite vem do `StreamReadConstraints` padrão do Jackson. O aninhamento excessivo segue o mesmo caminho. Nenhum cenário da spec exercita isso: o valor do cenário de inteiro grande tem 20 dígitos e passa como esperado. A resposta continua 400, nada é gravado e nada vaza. `LeitorCorpoTest.numeroAcimaDoLimiteDoJacksonEhCorpoInvalido` fixa esse comportamento em `nota`.

**Situação:** aceito pelo Gabriel em 04/11/2026 como limite, registrado na D4 do `design.md`. O código de produção não foi alterado.

## Observações

Não reprovam cenário. As quatro primeiras foram relatadas pelo dev-java e conferidas aqui. O status "em aberto" em cada uma descreve o momento do QA.

**Decisões do Gabriel em 04/11/2026:**
- **A, mensagem do SDK no trace:** aceita como limite conhecido, registrada na D9 do `design.md`.
- **B, métrica `ColdStart`:** D6 corrigida. O dashboard de `add-observabilidade-seguranca` usa a duração de restauração do SnapStart.
- **C, DEBUG:** D9 ajustada. A regra vale para o nosso código, com a função em INFO.
- **D, versões do Jackson:** sem decisão registrada, continua em aberto.

As evidências marcadas como "sonda" vêm de um programa local fora do repositório, não versionado. Ele usa o handler real, o `DynamoDbClient` real com HTTP falso, sem rede, `Rastreador.XRAY` com emissor em memória e `Metrics` real, com as variáveis `AWS_LAMBDA_INITIALIZATION_TYPE` e `POWERTOOLS_METRICS_NAMESPACE` definidas à mão.

- **A. Mensagem da exceção do SDK no trace do X-Ray. Status: confirmada, em aberto.** `powertools-tracing` traz `aws-xray-recorder-sdk-aws-sdk-v2-instrumentor`, que registra `TracingInterceptor` em todo cliente do SDK. Em `onExecutionFailure`, ele chama `subsegment.addException`. Na sonda, com falha 400 cuja mensagem levava um marcador e com log em INFO, o marcador apareceu em `cause.message` e `cause.exceptions[].message` do subsegmento `DynamoDb`, com a pilha, e não apareceu em nenhuma linha de log. O marcador enviado em `descricao` não apareceu no trace: a lista do instrumentor para `PutItem` só captura `TableName`. A D9 proíbe dado do aluno em anotações e metadados de trace. Não foi possível verificar, sem a conta, se uma mensagem real do DynamoDB cita valores do item.
- **B. Métrica `ColdStart` não sai com SnapStart. Status: confirmada, em aberto.** `LambdaHandlerProcessor.isColdStart()` só devolve verdadeiro quando `AWS_LAMBDA_INITIALIZATION_TYPE` é `on-demand`. Na sonda, com `snap-start`, houve 0 métricas `ColdStart` e `cold_start: false` no log. Com `on-demand`, houve 1 métrica e `cold_start: true`. A D6 do `design.md` diz que o cold start "fica registrado pela métrica `captureColdStartMetric`", o que não vale para este template. O valor real da variável na função só pode ser visto na tarefa 6.7. O `Metrics` é simulado nos testes, então eles não pegam isso.
- **C. Mensagem da exceção em nível DEBUG. Status: confirmada para o SDK e, por leitura do bytecode, para o X-Ray; em aberto.** Na sonda em DEBUG, o SDK emitiu 2 linhas com `error.message` e pilha, contendo o marcador da mensagem do serviço (`Request attempt 1 encountered non-retryable failure` e `Unable to acquire sufficient retry quota`). `UDPEmitter.sendSegment` chama `prettySerialize` quando o nível é DEBUG, o que incluiria a `cause` do ponto A. Esse trecho não rodou, porque a sonda usa emissor em memória. O marcador enviado em `descricao` teve 0 ocorrências em DEBUG e em TRACE. A produção está em INFO (`Root level="INFO"` no `log4j2.xml` e `POWERTOOLS_LOG_LEVEL: INFO` no template), e a spec só proíbe INFO. A D9 é mais estrita e cita DEBUG. Os testes de `L` usam nível `ALL` com cliente simulado, então não exercitam os logs do SDK.
- **D. `jackson-datatype-jsr310` 2.14.2 com `jackson-databind` 2.22.3. Status: confirmada, em aberto.** A árvore mostra `aws-embedded-metrics:4.1.2` trazendo `jackson-datatype-jsr310:2.14.2` em escopo `runtime`. Na sonda, a JVM carregou 69 classes do jsr310 e o `flush` real gerou EMF válido, sem erro de linkagem. Essa evidência cobre só esse caminho e não é garantia de compatibilidade entre as versões.
- **E. Entradas de borda sem cenário.** Uma `descricao` com substituto isolado (`"\ud83d"`) passa na validação e o SDK a envia. A resposta do DynamoDB real não é conhecida, e uma rejeição viraria 500 para um erro de entrada. A 6.6 pode exercitar. `nota` igual a `-0` é aceita como 0. A spec não trata nenhum dos dois.
- **F. Ligação de produção fora do `mvn verify`.** Os testes não executam `new RegistrarAvaliacaoHandler()`, `Rastreador.XRAY` nem o `Metrics` real. Na sonda, o construtor público rodou com `AWS_REGION` e `TABELA_AVALIACOES` definidas, só em caminhos de rejeição, sem chamada à AWS, e respondeu 400 nos dois. O caminho completo depende das tarefas 6.3 e 6.6.
- **G. Regra de credenciais só por revisão.** Um mutante que fixa `EnvironmentVariableCredentialsProvider` em `criarCliente` passa em todos os testes. A tarefa 3.7 prevê revisão, e a conferência abaixo não encontrou o problema, mas nada no build impede a regressão.
- **H. Nome de teste impreciso.** `L.base64Malformado` usa Base64 válido com JSON malformado. O Base64 realmente inválido passou a ser coberto por `L.motivoNaoTemDadoDeEntrada`.

## Regras de SnapStart (D7)

Conferido por leitura e busca em `functions/*/src/main/java`. A confirmação na conta é da tarefa 6.3.

| Regra | Resultado | Evidência |
|---|---|---|
| Nada de ID, instante ou gerador em campo estático | ok | Todos os campos estáticos são constantes, formatadores, `ZoneId`, `Logger`, `ObjectMapper`, `Duration`, `JsonNodeFactory`, `Map` de cabeçalhos e os recursos de `Recursos`. Sem `Random` nem `SecureRandom`. `Clock` fica em campo de instância |
| ID e instante gerados no handler | ok | `UUID.randomUUID()` e `Instant.now(relogio)` em `registrar`, linhas 143 e 144. `H.DataEnvio.instanteCapturadoUmaVezPorRequisicao` confere 0 leituras do relógio antes da primeira requisição |
| Cliente criado fora do handler | ok | `Recursos.CLIENTE` é `static final`, carregado pelo construtor público. Criar o cliente não chama a AWS. Se o runtime instancia o handler antes do snapshot, só a conta confirma |
| Nenhum provedor de credenciais fixado | ok | 0 ocorrências de `credentialsProvider` e de `EnvironmentVariableCredentialsProvider` em `main`. Ver a observação G |
| Cache de DNS antes do logger | ok | `Security.setProperty` é a primeira linha do bloco estático, antes de `TracingUtils.init()` e de `LOG`. `H.Inicializacao.cacheDeDns` confere o valor |
| `TracingUtils.init()` na inicialização | ok | Bloco estático do handler |
| Hostname e conexões próprias | ok | Nenhum uso. Só o cliente do SDK abre conexões |

## Logs (D9)

A mensagem de `CorpoInvalidoException` vai para `motivo` em INFO. Todos os caminhos de `LeitorCorpo`:

| Caminho | `motivo` | Pode conter entrada? |
|---|---|---|
| Corpo nulo ou vazio | `CorpoAusente` | Não, texto fixo |
| Nó que não é objeto | `CorpoNaoEhObjeto` | Não, texto fixo |
| Base64 inválido | `Base64Invalido` | Não, texto fixo. A mensagem da `IllegalArgumentException` é descartada |
| JSON inválido em texto | `e.getClass().getName()` | Não, nome de classe |
| JSON inválido em bytes | `e.getClass().getName()` | Não, nome de classe |

`CorpoInvalidoException` não encadeia causa nem guarda pilha. Os demais logs do handler levam `id`, `urgencia`, nomes de campos, classe da exceção, código de erro e ID da requisição, todos permitidos pela D9. `withLogging` usa a forma sem `input`. A única anotação de trace é `urgencia`. `Avaliacao.toString()` não expõe `descricao` nem `nota`, e `AvaliacaoTest.toStringNaoExpoeDescricaoNemNota` confere.

## Trechos do template.yaml

Conferidos no texto, e iguais aos trechos do `design.md`. O recurso gerado na conta é das tarefas 6.3 a 6.5.

| Item | Resultado | Linhas |
|---|---|---|
| Alias | `AutoPublishAlias: live` em `Globals`, `SnapStart.ApplyOn: PublishedVersions` e `AutoPublishAliasAllProperties: true` na função | 34 a 36 e 149 |
| `PermissionsBoundary` | Vem de `Globals`. A função não sobrescreve | 27 |
| Política | Uma declaração inline, `dynamodb:PutItem`, em `!GetAtt TabelaAvaliacoes.Arn`. O SAM acrescenta as gerenciadas de log e X-Ray, conforme a D13 | 154 a 160 |
| Tag da API | `Tags: ambiente: !Ref Stage` | 129 e 130 |
| Rota | `POST /avaliacao`, `PayloadFormatVersion: "2.0"`, na `ApiAvaliacoes` | 161 a 168 |

## Dependências

`dependency:tree` com escopo `runtime` de `registrar-avaliacao`: sem `aspectjrt`, `apache-client`, `apache5-client` e `netty-nio-client`. `url-connection-client` presente. `httpclient` 4.5.14 vem do X-Ray, como a D12 prevê.

Atualização de 04/11/2026: esta validação local não executou os hooks de SnapStart, e o primeiro deploy falhou por isso. `aspectjrt` passou a entrar em escopo `runtime`, por decisão do Gabriel (D6 do `design.md` e `docs/incidentes/2026-11-04-snapstart-aspectjrt.md`). A nova conferência fica na tarefa 6.8.

## Testes acrescentados

- `LogsRegistrarAvaliacaoTest.motivoNaoTemDadoDeEntrada`: novo, com 11 execuções, uma por caminho de `LeitorCorpo` em texto e em Base64. Confere status 400, nível INFO, ausência do marcador e de `987654`, `motivo` exato e ausência do campo `error`.
- `RegistrarAvaliacaoHandlerTest.RespostasHttp.umCampoInvalido`: passou a conferir o status 400, exigido pelo cenário.

Nenhum código de produção foi alterado.

## Prova das asserções

Em uma cópia temporária, fora do repositório, 16 mutações foram aplicadas ao código, uma de cada vez. 15 foram detectadas pelos testes. A que sobreviveu é a da observação G.

| Mutação | Detectada por |
|---|---|
| `codePointCount` trocado por `length` | `V.milEmojisContamComoMilCaracteres` |
| `temConteudo` trocado por `isBlank` | `V.outrosBrancosUnicodeContamComoConteudo` |
| `7.0` aceito como inteiro | `LeitorCorpoTest`, `H.ValidacaoNota.rejeitada` |
| Tokens sobrando aceitos | `LeitorCorpoTest`, `H.CorpoMalformado.tokensSobrando` |
| `motivo` com a mensagem do Jackson | `L.excecaoDoJacksonSoPelaClasse`, `L.motivoNaoTemDadoDeEntrada` |
| `motivo` de Base64 com o corpo | Só `L.motivoNaoTemDadoDeEntrada`, o teste novo |
| `descricao` em log INFO | `L.registroBemSucedido` |
| UUID em campo estático | `H.Registro.enviosIguaisGeramAvaliacoesDistintas` |
| Gravação antes da resposta 400 | `assertRejeitadoPor` |
| `trim` em `descricao` | `H.ValidacaoDescricao.persistidaComoRecebida` |
| 500 com a mensagem da exceção | `H.FalhaInterna` |
| Limite de `MEDIA` trocado para 7 | `U.classificaOsLimitesDaTabela` |
| `dataEnvio` em UTC | `C.ultimoSegundoDoDiaEmSaoPauloFicaNoDiaAnterior`, `AvaliacaoTest` |
| Exceção passada ao logger | `L.falhaDoServico`, `L.falhaDoCliente`, `H.FalhaInterna.tempoEsgotadoNoSdk` |
| Urgência fixa no item | `H.Classificacao` |
| Provedor de credenciais fixado | **Não detectada** |

## Coleção Postman

`docs/postman/feedback-serverless.postman_collection.json`, formato v2.1, com 33 requisições: 12 de sucesso e limites aceitos, 7 de rejeição de `descricao`, 9 de `nota`, 1 com dois campos inválidos e 4 de corpo malformado. A URL base é a variável `baseUrl`, com o valor neutro `https://exemplo.invalid`. Não há ID de conta, e-mail nem URL real.

Validação: JSON válido e execução com Newman contra uma ponte HTTP local para o handler com cliente simulado, com 33 requisições, 67 asserções e 0 falhas. A ponte não passa pelo API Gateway. A execução contra a stack é da tarefa 6.6.

Em 04/11/2026, por decisão do Gabriel, quatro requisições foram renomeadas para o nome descrever o que elas conferem, sem teste novo: a coleção só lê a resposta, então não prova persistência nem o que foi gravado no item. A prova está nos testes do handler e na tarefa 6.6. A mesma execução com Newman, depois da troca, deu 33 requisições, 67 asserções e 0 falhas. A ponte e o script de conferência ficam fora do repositório, então o repositório não tem checagem automatizada da coleção.

| Nome anterior | Nome atual |
|---|---|
| Descrição com espaços nas pontas, persistida como recebida | Descrição com espaços nas pontas: 201 com id e urgência MEDIA |
| Urgência do cliente ignorada | Nota 2 com urgencia BAIXA enviada: 201 com id e urgência CRITICA |
| Datas do cliente ignoradas | dataEnvio e criadoEm enviados: 201 com id e urgência MEDIA |
| Identificador do cliente ignorado | id abc enviado: 201 com outro id e urgência MEDIA |
