# QA: add-relatorio-semanal

Tarefas 4.1 e 5.3 a 5.9. A validação local foi feita em 05/11/2026 sobre o working tree da branch `feature/add-relatorio-semanal`, sem commit, e repetida em 06/11/2026 sobre `6d7553b`. A conferência na stack `feedback-dev` foi feita em 06/11/2026, sobre a execução 42151672828 do workflow Deploy (merge `6d7553b` do PR #11, `develop`, `success`, das 02:39:06 às 02:43:10 UTC, região `sa-east-1`).

**Validade da evidência da stack:**
- **Observação:** das 03:01 às 03:49 UTC de 06/11/2026, ou seja, das 00:01 às 00:49 de 06/11 em Brasília. Nesse intervalo, o dia civil era 2026-11-06 tanto em São Paulo quanto em UTC. Os horários citados vêm do CloudWatch, do X-Ray, do S3 e do DynamoDB. O relógio da máquina estava cerca de 6 s adiantado em relação à AWS: o `criadoEm` da primeira avaliação é 03:34:37.927 UTC, e o comando saiu às 03:34:43 pelo relógio local. Por isso o marcador traz 03:34.
- **Ordem:** a linha de base (configuração, agendamento, roles, bucket, tabela, logs e métricas) foi lida inteira antes de qualquer tráfego do QA. Antes do QA, o bucket não tinha nenhum objeto em `relatorios/`, a função não tinha nenhuma invocação e o tópico não tinha nenhuma publicação desde 22:30 UTC de 05/11.
- **Escritas na conta:** 2 avaliações registradas pela API e 9 invocações do alias `live` (6 com sucesso e 3 rejeitadas), que gravaram 4 objetos em `relatorios/`, 2 deles sobrescritos, e publicaram 6 avisos no tópico. Nada foi apagado, e nenhuma configuração, código, versão, `$LATEST` ou agendamento foi alterado. O restante foi leitura com o perfil `tc4-admin`.

## Resultado

| Status | Quantidade |
|---|---|
| ok (executado) | 42 |
| dos quais, executados também na stack | 27 |
| dos quais, executados só no handler ou no domínio, com clientes falsos | 15 |
| ok (por configuração) | 0 |
| não coberto | 0 |
| pendente do Gabriel | 0 |
| falha | 0 |
| **Total de cenários** | **42** |

**O change está conforme pelo critério desta matriz, com 0 não coberto e 0 falha.**

Os 2 cenários que o QA local deixou em aberto foram fechados na stack: `Agendamento implantado` passou de não coberto a ok (executado), pela inspeção do agendamento implantado, e `Acesso anônimo negado` passou de ok (por configuração) a ok (executado), com requisições anônimas recusadas.

**Conferência do e-mail pendente do Gabriel.** Nenhum cenário desta spec fala em e-mail: os cenários do aviso tratam do que é publicado no tópico, e o texto completo do aviso já está provado nos testes do handler. Por isso nenhum cenário fica com o status pendente. Na stack, o QA confirmou a publicação, a entrega à assinatura e o tamanho exato de cada mensagem, mas não leu o texto. A leitura dos 6 e-mails pelo Gabriel completa a réplica de 6 cenários: `Aviso permite localizar o relatório`, `Aviso traz o resumo da janela`, `Aviso sem descrições`, `Aviso sem notas individuais`, `Aviso da semana vazia` e `Aviso da reexecução`. O roteiro está em "Avisos para o Gabriel conferir no e-mail".

**Execução agendada fica para depois de 09/11/2026.** O agendamento está implantado, mas a primeira execução real é na segunda 09/11/2026 às 08:00 em São Paulo, 11:00 UTC. A conferência dessa execução está em "Conferência pendente da execução agendada".

Significado dos status:
- **ok (executado):** o teste rodou em `mvn -B verify` e a asserção prova o que o cenário pede no nível do handler ou do domínio, ou a execução na stack prova o que o cenário pede. A coluna Limite diz qual dos dois casos vale.
- **ok (por configuração):** o recurso garante o comportamento, mas ele não foi exercitado. Não equivale a teste.
- **não coberto:** não há evidência do que o cenário pede. Conta como falha.
- **pendente do Gabriel:** a evidência depende de uma conferência que só o Gabriel faz, como a caixa de entrada.
- **falha:** o comportamento observado contraria o cenário.

## Limite da evidência

Os testes usam `DynamoFalso`, `S3Falso` e `SnsFalso`, de `ApoioTeste`, `Metrics` simulado com Mockito e `RastreadorFalso`. O `queryPaginator` e o `RequestBody` são os reais do SDK, sobre o cliente falso. Isso significa:

- **O que os testes provam:** dado o evento e o relógio, o handler consulta as partições esperadas, grava o texto esperado na chave esperada, publica a mensagem esperada ou deixa de chamar o serviço, e termina com a exceção esperada.
- **O que a stack acrescentou:** a leitura real do índice `por-data`, a gravação real no S3 com tipo de conteúdo e criptografia, a publicação real no SNS e a entrega à assinatura, as permissões da role com boundary, o SnapStart com restauração, os logs e as métricas no CloudWatch, o trace do X-Ray, a rejeição pelo runtime com `FunctionError`, o agendamento implantado e a negação do acesso anônimo.
- **O que segue sem prova real:** o disparo pelo Scheduler, as falhas de leitura, de gravação e de publicação, o item inválido, o log em ERROR desses caminhos e a métrica `ErroInterno`. Na stack só o caminho de sucesso e o de rejeição da data foram exercitados.
- **O que depende do e-mail:** o texto dos avisos como chega ao administrador.

Na coluna Limite da matriz:
- **STACK:** além do teste, o cenário foi repetido em `feedback-dev`. Quando a massa ou a data da stack difere da do cenário, a linha diz qual foi usada.
- **SIM, sem réplica:** só clientes falsos, e a stack não reproduziu o caso. O motivo está na própria linha.
- **E-MAIL:** a réplica do texto publicado depende da conferência do Gabriel.

Legenda das classes de teste:
- `H`: `br.com.fiap.feedback.relatorio.GerarRelatorioSemanalHandlerTest`, com a classe aninhada no nome
- `L`: `br.com.fiap.feedback.relatorio.LogsGerarRelatorioSemanalTest`
- `A`: `br.com.fiap.feedback.dominio.AvisoRelatorioTest`
- `C`: `br.com.fiap.feedback.dominio.ConsolidadorRelatorioTest`
- `R`: `br.com.fiap.feedback.dominio.DataReferenciaTest`
- `F`: `br.com.fiap.feedback.dominio.FormatadorRelatorioTest`
- `J`: `br.com.fiap.feedback.dominio.JanelaApuracaoTest`

## Verificações locais

### Validação de 05/11/2026, na branch `feature/add-relatorio-semanal`

| Verificação | Resultado |
|---|---|
| `mvn -B verify` | `BUILD SUCCESS`. `dominio`: `Tests run: 153, Failures: 0, Errors: 0, Skipped: 0`. `registrar-avaliacao`: `Tests run: 127, Failures: 0, Errors: 0, Skipped: 0`. `gerar-relatorio-semanal`: `Tests run: 76, Failures: 0, Errors: 0, Skipped: 0`. `notificar-critica` não tinha código nem testes naquele worktree |
| `sam validate --lint --region sa-east-1` | `template.yaml is a valid SAM Template` |
| `openspec validate add-relatorio-semanal --strict` | `Change 'add-relatorio-semanal' is valid` |
| Imports de `functions/dominio/src` | 0 arquivos com `software.amazon`, `com.amazonaws`, `com.fasterxml`, `org.slf4j` ou `org.apache`. Os imports de `main` são só `java.*` |
| Blocos do `design.md` no `template.yaml` | Os dois blocos da seção "Alterações no template.yaml" aparecem literalmente no arquivo. `Globals` não mudou |

### Repetição de 06/11/2026, sobre `6d7553b`

Worktree `fiap-tc4-qa-relatorio`, branch `feature/add-relatorio-semanal-qa`, com o JDK 25 definido só no comando, porque o `JAVA_HOME` do ambiente aponta para o JDK 21.0.2. No working tree, só `tasks.md` deste change estava modificado, por outra sessão.

| Verificação | Resultado |
|---|---|
| `mvn -B verify`, Microsoft OpenJDK 25.0.5.0.2 | `BUILD SUCCESS`. `dominio`: 171 testes. `registrar-avaliacao`: 127. `notificar-critica`: 65. `gerar-relatorio-semanal`: 76. Todos com 0 falhas, 0 erros e 0 ignorados |
| `sam validate --lint --region sa-east-1`, SAM CLI 1.171.0 | `template.yaml is a valid SAM Template` |
| `openspec validate add-relatorio-semanal --strict` | `Change 'add-relatorio-semanal' is valid` |

A comprovação em Corretto 25 vem do CI. A execução 42151672828 terminou com `success`.

### Limite do worktree da validação de 05/11, resolvido

Na branch `feature/add-relatorio-semanal`, o `template.yaml` já continha os blocos `LogNotificarCritica` e `NotificarCritica`, mas `functions/notificar-critica` só tinha o `pom.xml`, e o `sam build` daquele worktree isolado falhava (`Required target/classes directory was not produced from 'mvn package'`). A condição era integrar antes o código de `add-notificacao-critica`. Ela foi cumprida: `6d7553b` tem os dois módulos, o `mvn -B verify` acima roda os 65 testes da notificação, e o deploy 42151672828 criou a função do relatório sem erro.

## Conferência na stack (tarefas 5.3 a 5.8)

### Estado da stack

| Item | Observado |
|---|---|
| Stack `feedback-dev` | `UPDATE_COMPLETE`. `UPDATE_IN_PROGRESS` às 02:40:42 e `UPDATE_COMPLETE` às 02:42:58 UTC |
| Recursos criados | `LogGerarRelatorioSemanal` às 02:40:54, `GerarRelatorioSemanalRole` às 02:41:04, `GerarRelatorioSemanal` às 02:41:16, `GerarRelatorioSemanalVersion2cfd30fe8c` às 02:42:30, `GerarRelatorioSemanalAliaslive` às 02:42:33, `GerarRelatorioSemanalAgendaRole` às 02:42:52 e `GerarRelatorioSemanalAgenda` às 02:42:54 UTC, todos em `CREATE_COMPLETE`. As funções de registro e de notificação ganharam versão nova no mesmo deploy |
| Outputs | `GerarRelatorioSemanalNome` igual a `feedback-gerar-relatorio-semanal-dev` e `GerarRelatorioSemanalAliasArn` no alias `live` |
| Role da função | Boundary `feedback-limite-funcoes-dev`. Política inline com as 4 declarações da D11, iguais ao template: `dynamodb:Query` só em `feedback-avaliacoes-dev/index/por-data`, `s3:PutObject` só em `<bucket>/relatorios/*`, `sns:Publish` só em `feedback-admins-dev` e KMS em `key/*` com `kms:ViaService` igual a `sns.sa-east-1.amazonaws.com`. Gerenciadas: `AWSLambdaBasicExecutionRole` e `AWSXrayWriteOnlyAccess`. Sem `GetObject`, `ListBucket`, `Scan` nem `GetItem` |
| Variáveis da função | `AMBIENTE` `dev`, `TABELA_AVALIACOES` `feedback-avaliacoes-dev`, `POWERTOOLS_SERVICE_NAME` `gerar-relatorio-semanal`, `POWERTOOLS_METRICS_NAMESPACE` `FeedbackServerless`, `POWERTOOLS_LOG_LEVEL` `INFO`, `BUCKET_RELATORIOS` e `TOPICO_ADMINS_ARN`. O bucket que aparece nos logs é o do output `BucketRelatoriosNome` |
| Tópico | `feedback-admins-dev`, com `KmsMasterKeyId` igual a `alias/aws/sns`, 1 assinatura confirmada e 0 pendentes |
| Tabela antes do tráfego | 38 itens, todos com `dataEnvio` 2026-11-05: os 34 do QA do registro (`3e4306` e `85eb85`) e os 4 do QA da notificação (`8a6274`). 9 `CRITICA`, 22 `MEDIA` e 7 `BAIXA`, soma das notas 183. Nenhum item em outras datas |

### 5.3. SnapStart: ok

| Item | Observado |
|---|---|
| `get-function-configuration` em `feedback-gerar-relatorio-semanal-dev:live` | `Version: 1`, `State: Active`, `LastUpdateStatus: Successful`, `OptimizationStatus: On`, `ApplyOn: PublishedVersions`, `Runtime: java25`, 512 MB, `Timeout` de 120 s, `x86_64`, `TracingConfig` `Active`, `LogFormat: Text` |
| `get-alias` | `live` aponta para a versão 1 |
| Versões | Só `$LATEST` e `1`, com o mesmo `CodeSha256`. `$LATEST` com `OptimizationStatus: Off`, como previsto |
| `INIT_START` e `INIT_REPORT` da versão 1 | Às 02:41:20 (`Init Duration: 16039.16 ms`) e às 02:58:35 UTC (`Init Duration: 14944.69 ms`), sem `START` depois. A segunda é anterior ao início do QA. Ver a observação L |
| `RESTORE_REPORT` | 3 restaurações, uma em cada ambiente de execução usado: `658.81 ms` às 03:27:54, `861.08 ms` às 03:35:25 e `428.47 ms` às 03:44:06 UTC |
| `ExecutedVersion` | `1` nas 9 invocações |
| Verificação final, às 03:48 UTC | `On`, `Active` e versão 1, sem mudança |

#### Medição da restauração e da primeira chamada a cada serviço, base de 512 MB

Tirada do X-Ray das 3 primeiras invocações depois de cada restauração. As 9 invocações foram amostradas (`Sampled: true`). "Antes da leitura" vai do início da invocação ao início de `## lerAvaliacoes`.

| Amostra | Restauração | `Duration` | Antes da leitura | 1ª `Query` | Outras 6 `Query` | 1º `PutObject` | 1º `Publish` | `Max Memory Used` |
|---|---|---|---|---|---|---|---|---|
| 1. `fe4100a8`, 03:27:54 UTC, referência de hoje | 658,81 ms (cobrados 215 ms) | 6.965,65 ms | 1,303 s | 2,600 s | 0,023 a 0,159 s | 0,699 s | 0,379 s | 215 MB |
| 2. `4ef51c7b`, 03:35:25 UTC, referência de hoje | 861,08 ms (cobrados 249 ms) | 8.030,69 ms | 1,867 s | 2,564 s | 0,039 a 0,096 s | 0,780 s | 0,442 s | 219 MB |
| 3. `a5d5b0d1`, 03:44:06 UTC, referência 2026-10-26 | 428,47 ms (cobrados 169 ms) | 8.356,50 ms | 1,616 s | 3,142 s | 0,063 a 0,135 s | 0,701 s | 0,439 s | 220 MB |

Para comparação, no mesmo ambiente já restaurado:

| Invocação | `Duration` | 1ª `Query` | `PutObject` | `Publish` |
|---|---|---|---|---|
| `59e84b5c`, 03:35:49, referência de amanhã | 979,90 ms | 0,186 s | 0,200 s | 0,161 s |
| `f15f43ad`, 03:37:09, reexecução | 878,76 ms | 0,148 s | 0,236 s | 0,180 s |
| `12dc51a2`, 03:44:19, referência 2026-11-09 | 1.123,82 ms | 0,209 s | 0,219 s | 0,177 s |
| Rejeições `08e3fa16`, `57c100f9` e `a0aa6218` | 244,54, 40,42 e 5,60 ms | sem chamada | sem chamada | sem chamada |

A primeira chamada de cada serviço depois da restauração ficou bem abaixo do `apiCallTimeout` de 10 s: a maior foi a `Query` de 3,142 s, 31% do limite. Ver a observação J.

### 5.4. Agendamento: ok

`aws scheduler get-schedule --name feedback-relatorio-semanal-dev`:

| Campo | Observado |
|---|---|
| `ScheduleExpression` | `cron(0 8 ? * MON *)` |
| `ScheduleExpressionTimezone` | `America/Sao_Paulo` |
| `State` | `ENABLED` |
| `GroupName` | `default`. É o único agendamento da região |
| `FlexibleTimeWindow` | `Mode: OFF` |
| `Target.Arn` | `arn:aws:lambda:sa-east-1:<conta>:function:feedback-gerar-relatorio-semanal-dev:live`, o alias, igual ao output `GerarRelatorioSemanalAliasArn` |
| `Target.Input` | `{}` |
| `Target.RetryPolicy` | `MaximumRetryAttempts: 3` e `MaximumEventAgeInSeconds: 3600` |
| `ActionAfterCompletion` | `NONE` |
| Criação | 02:42:54 UTC de 06/11/2026 |
| Role do alvo | `GerarRelatorioSemanalAgendaRole`, com boundary `feedback-limite-funcoes-dev`, confiança só em `scheduler.amazonaws.com`, nenhuma política gerenciada e uma inline com `lambda:InvokeFunction` só no ARN do alias `live` |

A próxima execução pela expressão é a segunda 09/11/2026 às 08:00 em São Paulo, 11:00 UTC. A API não informa a próxima execução, e o disparo real só pode ser provado depois dessa data.

### 5.5. Acionamento manual: ok

Todas as invocações pelo alias, com `aws lambda invoke --function-name feedback-gerar-relatorio-semanal-dev:live --cli-binary-format raw-in-base64-out --cli-read-timeout 150`, como na D1. Hoje em São Paulo era 2026-11-06, e amanhã, 2026-11-07.

A conferência de cada relatório foi feita assim: as 7 partições da janela foram lidas do índice `por-data` com `aws dynamodb query`, o texto esperado foi montado com o formato do README e da D5, com a média calculada em decimal exato e arredondamento para cima no empate, e comparado byte a byte com o objeto baixado por `aws s3 cp`. O mesmo programa conferiu que nenhuma linha do objeto foge dos blocos do formato, o que exclui a nota individual.

#### a. Sem data: `{}`

| Item | Observado |
|---|---|
| Resposta | `StatusCode` 200, sem `FunctionError`, `{"chave":"relatorios/2026-11-06.txt","quantidadeAvaliacoes":38,"dataReferencia":"2026-11-06"}`. Sem o bucket |
| Objeto | `relatorios/2026-11-06.txt`, 15.364 bytes, `ContentType: text/plain; charset=utf-8`, `ServerSideEncryption: AES256`, sem metadados, `LastModified` 03:28:02 UTC |
| Janela | `2026-10-30 a 2026-11-05` |
| Comparação com a tabela | **Idêntico byte a byte** ao texto montado a partir das 7 partições, com quebra de linha final |

#### Avaliação nova antes de (b)

Marcador `QA-REL-202611060334-3b9879`, registrado pela URL do output `ApiAvaliacoesUrl` (`<url>`):

| Caso | Marcador | Nota | HTTP | `id` | `urgencia` | `dataEnvio` | `criadoEm` (UTC) |
|---|---|---|---|---|---|---|---|
| Antes de (b) | `QA-REL-202611060334-3b9879-A` | 7 | 201 | `239d9eef-ef1f-4de4-a5c0-a49c0f8e0b86` | `BAIXA` | 2026-11-06 | 03:34:37.927 |
| Antes da reexecução (5.6) | `QA-REL-202611060334-3b9879-B` | 5 | 201 | `84f8c3a1-b246-42cc-b3ba-c513dafbcc8a` | `MEDIA` | 2026-11-06 | 03:36:41.088 |

As descrições têm o marcador e um texto curto com acentos. Nenhuma das duas é crítica, então a função de notificação não foi acionada: 0 eventos no grupo dela desde 03:16 UTC. Depois de cada registro, o QA esperou 15 s (D4) e confirmou o item na partição 2026-11-06 do índice.

**Prova de que a avaliação de hoje fica fora sem data.** Com a avaliação `-A` já no índice, o QA repetiu o acionamento `{}` às 03:35:25 UTC. A resposta voltou com `quantidadeAvaliacoes` 38, o objeto `relatorios/2026-11-06.txt` ficou com o mesmo ETag e o mesmo tamanho de (a), com `LastModified` novo em 03:35:34, e o marcador aparece 0 vezes. O texto continua idêntico ao montado a partir da janela de 2026-10-30 a 2026-11-05.

#### b. Com a data de amanhã: `{"dataReferencia":"2026-11-07"}`

| Item | Observado |
|---|---|
| Resposta | `{"quantidadeAvaliacoes":39,"dataReferencia":"2026-11-07","chave":"relatorios/2026-11-07.txt"}`, sem bucket |
| Objeto | `relatorios/2026-11-07.txt`, 15.503 bytes, mesmo tipo de conteúdo e criptografia, `LastModified` 03:35:50 UTC |
| Janela | `2026-10-31 a 2026-11-06`, que inclui hoje |
| Avaliação nova | Entra como a última da lista, com `Data de envio: 2026-11-06 | Urgência: BAIXA` e a descrição com o marcador `-A` |
| Comparação com a tabela | **Idêntico byte a byte** |

#### c. Datas inválidas

| Payload | Resposta | Duração |
|---|---|---|
| `{"dataReferencia":"2026-02-30"}` | `FunctionError: Unhandled`, `errorMessage` `Data de referência inválida. Informe uma data existente no formato yyyy-MM-dd.`, `errorType` `br.com.fiap.feedback.dominio.DataReferenciaInvalidaException` | 244,54 ms |
| `{"dataReferencia":"09/11/2026"}` | Igual | 40,42 ms |
| `{"dataReferencia":""}` | Igual. Acrescentado pelo QA para cobrir `Valor vazio` | 5,60 ms |

| Efeito | Observado |
|---|---|
| Objetos | A listagem de `relatorios/` antes e depois das 3 rejeições é igual: as chaves `2026-11-06` e `2026-11-07`, com os mesmos ETag, `LastModified` e tamanho. Nenhum objeto novo |
| Leitura | Os 3 traces não têm nenhum subsegmento de DynamoDB, S3 ou SNS, nem `## lerAvaliacoes` |
| Publicação | `NumberOfMessagesPublished` sem ponto às 03:36 UTC |
| Métrica | `DataReferenciaRejeitada` igual a 3 às 03:36 UTC, no namespace `FeedbackServerless` com a dimensão `Service` `gerar-relatorio-semanal` |
| Log | 3 linhas `Data de referência rejeitada` em ERROR, com o campo `classe`, sem o valor recebido |

A resposta da rejeição também traz `stackTrace`, ver a observação M.

#### Execuções adicionais do QA

Para repetir na stack exemplos literais da spec, o QA fez mais 2 invocações com sucesso, depois da 5.6:

| Payload | Resposta | Objeto | Janela | Comparação com a tabela |
|---|---|---|---|---|
| `{"dataReferencia":"2026-10-26"}` | `quantidadeAvaliacoes` 0 | `relatorios/2026-10-26.txt`, 350 bytes, `LastModified` 03:44:15 UTC | `2026-10-19 a 2026-10-25`, igual ao cenário `Referência no passado` | **Idêntico byte a byte**: `Média das notas: sem avaliações na janela`, 3 urgências em zero, 7 dias em zero e `Avaliações (0)` |
| `{"dataReferencia":"2026-11-09"}` | `quantidadeAvaliacoes` 40 | `relatorios/2026-11-09.txt`, 15.652 bytes, `LastModified` 03:44:21 UTC | `2026-11-02 a 2026-11-08`, igual ao cenário `Janela de uma segunda-feira` | **Idêntico byte a byte** |

O objeto de 2026-11-09 também prepara o cenário `Manual seguida de agendada`: a execução agendada de segunda deve substituí-lo.

#### Números de cada relatório contra a tabela

| Referência | Execução | Janela | Avaliações | Soma das notas | Média esperada | Média no objeto | CRITICA, MEDIA, BAIXA | Por dia (os demais em 0) |
|---|---|---|---|---|---|---|---|---|
| 2026-11-06 | (a) e repetição | 2026-10-30 a 2026-11-05 | 38 | 183 | 183/38 = 4,8157 → 4,82 | 4,82 | 9, 22, 7 | 2026-11-05: 38 |
| 2026-11-07 | (b) | 2026-10-31 a 2026-11-06 | 39 | 190 | 190/39 = 4,8718 → 4,87 | 4,87 | 9, 22, 8 | 2026-11-05: 38; 2026-11-06: 1 |
| 2026-11-07 | reexecução (5.6) | 2026-10-31 a 2026-11-06 | 40 | 195 | 195/40 = 4,875 → 4,88 | 4,88 | 9, 23, 8 | 2026-11-05: 38; 2026-11-06: 2 |
| 2026-10-26 | adicional | 2026-10-19 a 2026-10-25 | 0 | 0 | sem média | `sem avaliações na janela` | 0, 0, 0 | todos em 0 |
| 2026-11-09 | adicional | 2026-11-02 a 2026-11-08 | 40 | 195 | 4,88 | 4,88 | 9, 23, 8 | 2026-11-05: 38; 2026-11-06: 2 |

Os 7 dias aparecem em todos os relatórios, inclusive os de quantidade zero, e a lista traz descrição, urgência e data de envio de cada avaliação, na ordem do índice. A média de 4,875 é um empate exato e saiu como `4,88`, o que é coerente com a D6.

#### Avisos publicados

| Momento da publicação (UTC) | Referência | `NumberOfMessagesPublished` | `PublishSize` | Corpo esperado pela D7 |
|---|---|---|---|---|
| 03:28:02 | 2026-11-06 | 1 às 03:28 | 525 bytes | 525 bytes |
| 03:35:34 | 2026-11-06, repetição | 2 às 03:35 | 525 e 525 bytes | 525 bytes cada |
| 03:35:49 | 2026-11-07 | | | |
| 03:37:10 | 2026-11-07, reexecução | 1 às 03:37 | 525 bytes | 525 bytes |
| 03:44:15 | 2026-10-26, semana vazia | 2 às 03:44 | 545 e 525 bytes | 545 e 525 bytes |
| 03:44:20 | 2026-11-09 | | | |

- O momento da publicação é o da linha `Relatório gerado` da invocação, que sai depois do `Publish`.
- `NumberOfNotificationsDelivered` soma 6 e `NumberOfNotificationsFailed` soma 0 no período.
- O corpo esperado foi montado com o texto da D7 e do README, os números de cada relatório e o bucket real, mais a quebra de linha final. Os 6 tamanhos batem exatamente. Uma única descrição a mais no aviso acrescentaria dezenas de bytes, e as descrições da janela têm de 32 a 1000 caracteres. O tamanho não prova o texto, que só o e-mail mostra.
- O assunto `Relatório semanal <data>` tem 29 bytes em UTF-8 e foi aceito nas 6 publicações.

#### Métricas da função

| Métrica | Observado |
|---|---|
| `RelatorioGerado` | 1 às 03:27, 2 às 03:35, 1 às 03:37 e 2 às 03:44 UTC: 6, uma por sucesso |
| `DataReferenciaRejeitada` | 3 às 03:36 UTC |
| `ErroInterno` | Nenhum ponto |
| `Invocations` no alias | 1, 2, 3, 1 e 2: 9 |
| `Errors` no alias | 3, as 3 rejeições, às 03:36. Ver a observação M |
| `Throttles` | 0 |

### 5.6. Reexecução: ok

Entre (b) e a reexecução, o QA registrou a avaliação `-B` e esperou 15 s.

| Item | Observado |
|---|---|
| Reexecução às 03:37:09 UTC, `{"dataReferencia":"2026-11-07"}` | `quantidadeAvaliacoes` 40 |
| Objetos com a chave `relatorios/2026-11-07.txt` | `list-object-versions` mostra 1 versão, com `VersionId` `null` e `IsLatest` `true`. O bucket não tem versionamento. Nenhum marcador de exclusão |
| Substituição | ETag `7b075f1e...` para `33177362...`, tamanho de 15.503 para 15.652 bytes e `LastModified` de 03:35:50 para 03:37:11 UTC |
| Conteúdo | Inclui a avaliação `-B`, registrada entre as duas execuções, e é **idêntico byte a byte** ao texto montado da tabela naquele momento |
| Novo aviso | 1 publicação às 03:37 UTC, com 525 bytes, entregue |
| Outras chaves | `relatorios/2026-11-06.txt` continuou com o ETag e o `LastModified` anteriores |

A repetição de (a), às 03:35, também foi uma reexecução, para 2026-11-06: o objeto continuou único para a chave, com `LastModified` novo e conteúdo igual, porque a janela não mudou, e um novo aviso saiu às 03:35.

**Estado final do bucket:** 4 objetos em `relatorios/` (`2026-10-26`, `2026-11-06`, `2026-11-07` e `2026-11-09`), cada um com a sua janela, mais os 5 objetos de `aws/lambda/` do QA da notificação, sem alteração. 9 objetos no total.

### 5.7. Acesso anônimo: ok

`curl` sem credencial, no endereço virtual-hosted da região, `https://<bucket>.s3.sa-east-1.amazonaws.com/<chave>`:

| Requisição | HTTP | Corpo |
|---|---|---|
| `GET` de `relatorios/2026-11-07.txt` | 403 | `AccessDenied`, sem conteúdo do relatório |
| `GET` de `relatorios/2026-11-06.txt` | 403 | `AccessDenied` |
| `HEAD` de `relatorios/2026-11-07.txt` | 403 | |
| `GET` por `http`, sem TLS | 403 | |
| Listagem do bucket (`?list-type=2`) | 403 | `AccessDenied` |

Os corpos das respostas têm 0 ocorrências de `QA-`.

| Configuração do bucket | Observado |
|---|---|
| Block Public Access | `BlockPublicAcls`, `IgnorePublicAcls`, `BlockPublicPolicy` e `RestrictPublicBuckets`, os quatro `true` |
| Block Public Access da conta | Não configurado. O bloqueio vem só do bucket |
| `get-bucket-policy-status` | `IsPublic: false` |
| Política | Só a declaração `NegaAcessoSemTls`, com `Deny` para `aws:SecureTransport` igual a `false` |
| Propriedade dos objetos | `BucketOwnerEnforced`, com ACL desativada |
| Criptografia padrão | `AES256`, com `SSE-C` bloqueado |
| Versionamento | Não configurado |

### 5.8. Logs: ok

Leitura completa do grupo `/aws/lambda/feedback-gerar-relatorio-semanal-dev`, às 03:48 UTC, com 73 eventos. Retenção de 14 dias.

| Verificação | Resultado |
|---|---|
| `filter-log-events` no servidor com `QA-REL-202611060334-3b9879`, `3b9879`, `QA-REL`, `QA-`, `3e4306`, `85eb85`, `8a6274`, `Explicação`, `índices`, `Ritmo`, `práticos`, `Aula`, `descricao`, `nota`, `media`, `Média`, `4,82`, `4,87` e `4,88` | 0 eventos em todos |
| Controle positivo do mesmo filtro | `rejeitada` 3, `gravado` 4 e `quantidadeTotal` 4 nas primeiras leituras, o que mostra que o filtro enxerga as linhas |
| Leitura completa, no cliente | 0 ocorrências do início, do meio e do fim de cada uma das 40 descrições da janela, e 0 ocorrências dos 40 `id` |
| Campos numéricos nas linhas JSON | Só `cold_start`, `function_memory_size`, `quantidadeTotal` e `totaisPorUrgencia`. Nenhuma nota, nenhuma média |
| Campos das linhas JSON | `bucket`, `chave`, `classe`, `cold_start`, `dataReferencia`, `function_arn`, `function_memory_size`, `function_name`, `function_request_id`, `function_version`, `level`, `message`, `quantidadeTotal`, `service`, `timestamp`, `totaisPorUrgencia` e `xray_trace_id` |
| Mensagens da aplicação | `Relatório gravado` (6) e `Relatório gerado` (6) em INFO, `Data de referência rejeitada` (3) em ERROR, e 9 linhas INFO do SDK do X-Ray, 3 por restauração. 0 em WARN |
| Formato | Todas as linhas da aplicação em JSON. Fora delas: 9 linhas de métricas EMF, as linhas de plataforma em texto (`INIT_START` 2, `INIT_REPORT` 2, `RESTORE_START` 3, `RESTORE_REPORT` 3, `START` 9, `END` 9 e `REPORT` 9) e 3 linhas em texto escritas pelo runtime com a exceção da rejeição, ver a observação M |
| Trace do X-Ray das 9 invocações | 0 ocorrências de `QA-`, `3b9879`, `Explica`, `Ritmo`, `Aula`, `descricao` e das médias, e 0 trechos de descrição |
| Logs de `registrar-avaliacao` desde 03:16 UTC | 0 eventos com o marcador |

### Itens de teste

Tabela `feedback-avaliacoes-dev` com 40 itens depois da conferência. Nenhum foi apagado.

| Marcador | Itens | Situação |
|---|---|---|
| `QA-202611050457-3e4306` e `QA-202611050500-85eb85` | 34 | Do QA de `add-registro-avaliacao`. Mesma contagem por data e urgência antes e depois do QA; não comparados atributo por atributo |
| `QA-NOTIF-202611052214-8a6274` | 4 | Do QA de `add-notificacao-critica`, idem |
| `QA-REL-202611060334-3b9879` | 2 | Deste QA, `-A` com nota 7 e `-B` com nota 5, os dois com `dataEnvio` 2026-11-06 |

## Avisos para o Gabriel conferir no e-mail

O QA não lê a caixa de entrada. Na assinatura de dev devem ter chegado **exatamente 6 e-mails** com o assunto `Relatório semanal <data>`, entre 00:28 e 00:44 de 06/11/2026 em Brasília, e nenhum e-mail `Avaliação crítica registrada` nesse período. As 3 rejeições não geram e-mail.

| # | UTC | Brasília | Assunto | Janela | Média | CRITICA, MEDIA, BAIXA | Por dia | Localização |
|---|---|---|---|---|---|---|---|---|
| 1 | 03:28 | 00:28 | `Relatório semanal 2026-11-06` | 2026-10-30 a 2026-11-05 | 4,82 | 9, 22, 7 | 2026-10-30 a 2026-11-04 em 0; 2026-11-05: 38 | `s3://<bucket>/relatorios/2026-11-06.txt` |
| 2 | 03:35 | 00:35 | `Relatório semanal 2026-11-06` | igual ao 1 | 4,82 | 9, 22, 7 | igual ao 1 | igual ao 1 |
| 3 | 03:35 | 00:35 | `Relatório semanal 2026-11-07` | 2026-10-31 a 2026-11-06 | 4,87 | 9, 22, 8 | 2026-10-31 a 2026-11-04 em 0; 2026-11-05: 38; 2026-11-06: 1 | `s3://<bucket>/relatorios/2026-11-07.txt` |
| 4 | 03:37 | 00:37 | `Relatório semanal 2026-11-07` | 2026-10-31 a 2026-11-06 | 4,88 | 9, 23, 8 | 2026-10-31 a 2026-11-04 em 0; 2026-11-05: 38; 2026-11-06: 2 | `s3://<bucket>/relatorios/2026-11-07.txt` |
| 5 | 03:44 | 00:44 | `Relatório semanal 2026-10-26` | 2026-10-19 a 2026-10-25 | `sem avaliações na janela` | 0, 0, 0 | os 7 dias em 0 | `s3://<bucket>/relatorios/2026-10-26.txt` |
| 6 | 03:44 | 00:44 | `Relatório semanal 2026-11-09` | 2026-11-02 a 2026-11-08 | 4,88 | 9, 23, 8 | 2026-11-02 a 2026-11-04 em 0; 2026-11-05: 38; 2026-11-06: 2; 2026-11-07 e 2026-11-08 em 0 | `s3://<bucket>/relatorios/2026-11-09.txt` |

Em cada e-mail, conferir:
- O corpo começa com `Relatório semanal gerado.` e traz `Ambiente: dev`, a data de referência e a janela.
- As linhas `Bucket:`, `Chave:` e `Localização:` apontam para a chave da tabela, e o texto final é `O bucket é privado. Acesse com uma credencial da conta.`
- **Nenhuma descrição:** nada com `QA-`, `Aula`, `Explicação`, `Ritmo` ou qualquer texto de avaliação.
- **Nenhuma nota individual:** o único número derivado das notas é a média.
- Os acentos do assunto e do corpo aparecem corretos.

O corpo traz o nome do bucket, que inclui o ID da conta, e o SNS acrescenta um rodapé com link de cancelamento que contém o ID da conta e o e-mail do assinante, como em `add-notificacao-critica`, observação M. Não copiar o e-mail para arquivo versionado, e desfocar essas linhas no vídeo e nas capturas.

## Conferência pendente da execução agendada

Depois de 11:00 UTC de 09/11/2026, com comandos só de leitura:

1. `aws s3api head-object` em `relatorios/2026-11-09.txt`: `LastModified` igual ou posterior a 11:00 UTC de 09/11. Hoje ele é 03:44:21 UTC de 06/11, da execução manual.
2. Logs da função entre 11:00 e 12:00 UTC: um `START` com `Version: 1`, `Relatório gerado` com `dataReferencia` `2026-11-09`, e nenhuma invocação manual no período.
3. `Invocations` no alias e `RelatorioGerado` com 1 ponto às 11:00 UTC.
4. Baixar o objeto e comparar com as 7 partições de 2026-11-02 a 2026-11-08 lidas na hora.
5. Pedir ao Gabriel a conferência do e-mail `Relatório semanal 2026-11-09` das 08:00 em Brasília.

Isso fecha na stack `Execução agendada` e `Manual seguida de agendada`.

## Matriz: relatorio-semanal

### Geração automática semanal

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Agendamento implantado | ok (executado) | Stack: `get-schedule` com `cron(0 8 ? * MON *)`, `America/Sao_Paulo`, `ENABLED`, alvo no alias `live`, `Input` `{}`, `RetryPolicy` 3 e 3600 s, `FlexibleTimeWindow` `OFF` e role com boundary | STACK, 5.4. Inspeção do recurso implantado, que é o que o cenário pede. Nenhum teste lê o template (observação E) |
| Execução agendada | ok (executado) | `H.GeracaoAutomatica.execucaoAgendada`: relógio em 08:00 de 09/11/2026 em São Paulo e evento `{}`, igual ao `Input` implantado | SIM, sem réplica até 09/11/2026 11:00 UTC. Ver "Conferência pendente da execução agendada" |

### Janela de apuração

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Janela de uma segunda-feira | ok (executado) | `H.Janela.janelaDeUmaSegunda`, `J.dias`, `J.limitesDaJanela`, `J.janelaDeUmaSegunda`. Stack: referência 2026-11-09 gerou `Janela: 2026-11-02 a 2026-11-08`, com as 7 datas, e o objeto bate byte a byte com essas 7 partições | STACK, execução adicional |
| Avaliação no primeiro dia da janela | ok (executado) | `H.Janela.extremosEntram`, `C.Janela.primeiroDia` | SIM, sem réplica. Nenhuma avaliação real no primeiro dia de nenhuma janela usada |
| Avaliação no último dia da janela | ok (executado) | `H.Janela.extremosEntram`, `C.Janela.ultimoDia`. Stack: com referência 2026-11-07, as avaliações de 2026-11-06, último dia da janela, entraram | STACK, com outra data |
| Avaliação no dia da referência | ok (executado) | `H.Janela.foraDaJanela`, `C.Janela.foraDaJanela`. Stack: com referência 2026-11-06, a avaliação `-A` de 2026-11-06 ficou fora da lista, dos totais e da média, que seguiu 4,82 | STACK, com outra data |
| Avaliação oito dias antes da referência | ok (executado) | `H.Janela.foraDaJanela`, `C.Janela.foraDaJanela` | SIM, sem réplica. Nenhuma avaliação real oito dias antes de alguma referência usada |
| Limite final avaliado no fuso de São Paulo | ok (executado) | `H.Janela.limiteFinal`, `C.Janela.limiteFinalEmSaoPaulo` | SIM, sem réplica. Usa instante fixo. As avaliações reais não caíram entre 21:00 e 23:59 em São Paulo |
| Limite inicial avaliado no fuso de São Paulo | ok (executado) | `H.Janela.limiteInicial`, `C.Janela.limiteInicialEmSaoPaulo` | SIM, sem réplica. Usa instante fixo |

### Conteúdo do relatório

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Semana com avaliações | ok (executado) | `H.Conteudo.semanaComAvaliacoes`, `C.Conteudo.semanaComAvaliacoes`. Stack: 4 relatórios com 38 a 40 avaliações, cada um idêntico byte a byte ao texto montado da tabela, com descrição, urgência e data de envio de cada uma, totais por dia, por urgência e média | STACK, com outra massa |
| Média não exata | ok (executado) | `H.Conteudo.mediaNaoExata`, `C.Media.naoExata`. Stack: 4,82 para 183/38, 4,87 para 190/39 e 4,88 para o empate 195/40 | STACK, com outra massa |
| Urgência vem do que foi persistido | ok (executado) | `H.Conteudo.urgenciaPersistida`, com nota 9 persistida como `CRITICA`; `C.Conteudo.urgenciaPersistida`. Stack: as 40 urgências do objeto são as persistidas, mas o registro real grava urgência coerente com a nota, então a stack não distingue recálculo | SIM, sem réplica específica |

### Totais completos

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Dias e urgências sem avaliações | ok (executado) | `H.TotaisCompletos.diasEUrgenciasComZero`, `C.TotaisCompletos.diasEUrgenciasSemAvaliacoes`. Stack: os 7 dias aparecem em todos os relatórios, com 5 ou 6 dias em zero, e as 3 urgências em zero na janela vazia | STACK para os dias. Urgência em zero ao lado de outra não zero só no handler |

### Semana sem avaliações

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Janela vazia | ok (executado) | `H.SemanaVazia.janelaVazia`, `F.semanaVazia`, `C.TotaisCompletos.janelaVazia`. Stack: referência 2026-10-26 gravou o objeto com `Avaliações (0)`, os 7 dias e as 3 urgências em zero e `sem avaliações na janela` no lugar da média. O aviso foi publicado e entregue | STACK, execução adicional |
| Média zero não se confunde com ausência de média | ok (executado) | `H.SemanaVazia.mediaZero`, `C.Media.soZeros`, `F.mediaZero` | SIM, sem réplica. Nenhuma janela real só com notas 0 |

### Guarda do relatório em bucket privado

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Relatório guardado | ok (executado) | `H.Guarda.relatorioGuardado`. Stack: 6 gravações no bucket do output `BucketRelatoriosNome` da stack `feedback-dev`, com `text/plain; charset=utf-8` e `AES256` | STACK, 5.5 |
| Acesso anônimo negado | ok (executado) | Stack: `GET`, `HEAD`, `GET` sem TLS e listagem anônimos com 403 `AccessDenied`, sem conteúdo. Block Public Access com as 4 opções, `IsPublic: false` e `BucketOwnerEnforced` | STACK, 5.7 |

### Aviso aos administradores

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Aviso permite localizar o relatório | ok (executado) | `H.Aviso.localizacaoCorresponde`, `H.Aviso.topicoEAssunto`, `A.localizacaoCorrespondeAChave`. Stack: cada publicação saiu depois da gravação da mesma invocação, e o tamanho de cada mensagem é o do corpo com o bucket e a chave daquela gravação | STACK por tamanho. E-MAIL pendente |
| Aviso traz o resumo da janela | ok (executado) | `H.Aviso.textoCompletoSemDescricaoNemNota`, `A.textoCompleto`, com o texto completo. Stack: tamanho igual ao corpo com média, 3 urgências e 7 dias de cada relatório | STACK por tamanho, com outra massa. E-MAIL pendente |
| Aviso sem descrições | ok (executado) | `H.Aviso.textoCompletoSemDescricaoNemNota`, `A.semDescricao`. Stack: o objeto de 2026-11-07 contém o marcador `QA-REL-202611060334-3b9879`, e o aviso da mesma invocação tem 525 bytes, igual ao corpo sem nenhuma descrição | STACK por tamanho. E-MAIL pendente |
| Aviso sem notas individuais | ok (executado) | `H.Aviso.textoCompletoSemDescricaoNemNota`, `A.textoCompleto`, `A.recebeSoResumoELocalizacao`. Stack: tamanho igual ao corpo esperado | STACK por tamanho. E-MAIL pendente |
| Aviso da semana vazia | ok (executado) | `A.semanaVazia`, `H.SemanaVazia.janelaVazia`. Stack: o aviso de 2026-10-26 tem 545 bytes, igual ao corpo com os zeros, `sem avaliações na janela` e a localização | STACK por tamanho. E-MAIL pendente |

### Acionamento manual

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Acionamento manual sem parâmetro | ok (executado) | `H.AcionamentoManual.semParametro`, `H.AcionamentoManual.eventoSemData`. Stack: `{}` em 06/11/2026 deu referência 2026-11-06, janela de 2026-10-30 a 2026-11-05, objeto gravado e aviso entregue | STACK, 5.5a, em outra data |
| Dia atual calculado no fuso de São Paulo | ok (executado) | `H.AcionamentoManual.diaAtualEmSaoPaulo`, `R.ausenteUsaDiaAtualEmSaoPaulo`, `R.fusoDoRelogioNaoImporta` | SIM, sem réplica. Os acionamentos foram às 00:28 e 00:35 em São Paulo, quando a data civil era a mesma em UTC. Só um acionamento entre 21:00 e 23:59 em São Paulo distingue os fusos |
| Avaliação do próprio dia fica fora sem parâmetro | ok (executado) | `H.AcionamentoManual.proprioDiaFora`. Stack: a repetição de `{}` com a avaliação `-A` já no índice deu 38 avaliações e 0 ocorrências do marcador | STACK, 5.5 |

### Data de referência opcional no acionamento manual

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Referência de amanhã inclui a avaliação de hoje | ok (executado) | `H.DataOpcional.referenciaDeAmanha`. Stack: avaliação `-A` de 2026-11-06 e referência 2026-11-07, janela de 2026-10-31 a 2026-11-06, avaliação na lista e nos totais | STACK, 5.5b, em outra data |
| Referência no passado | ok (executado) | `H.DataOpcional.referenciaNoPassado`, `R.dataNoPassado`, `J.limitesDaJanela`. Stack: referência 2026-10-26 deu janela de 2026-10-19 a 2026-10-25 | STACK, execução adicional, com a data do cenário |
| Formato inválido | ok (executado) | `H.DataOpcional.valorInvalido`, `R.textoInvalido`. Stack: `09/11/2026` com `FunctionError` e a mensagem fixa, sem objeto, sem leitura e sem publicação | STACK, 5.5c |
| Data inexistente no calendário | ok (executado) | `H.DataOpcional.valorInvalido`, `R.textoInvalido`, `R.anoBissexto`. Stack: `2026-02-30`, mesmo resultado | STACK, 5.5c |
| Valor vazio | ok (executado) | `H.DataOpcional.valorInvalido`, `R.textoInvalido`. Stack: texto vazio, mesmo resultado | STACK, 5.5c |
| Rejeição preserva relatório existente | ok (executado) | `H.DataOpcional.rejeicaoPreservaRelatorio`. Stack: os objetos de 2026-11-06 e 2026-11-07 com ETag e `LastModified` iguais antes e depois das 3 rejeições | STACK, 5.5c |

`H.DataOpcional.valorInvalido` confere a exceção, a mensagem `Data de referência inválida`, 0 consultas, 0 gravações, 0 tentativas de publicação e nenhum objeto.

### Reexecução sem relatórios conflitantes

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Reexecução com avaliação nova | ok (executado) | `H.Reexecucao.comAvaliacaoNova`. Stack: duas execuções para 2026-11-07, com a avaliação `-B` registrada entre elas. Uma única versão da chave, e o objeto final traz a `-B` | STACK, 5.6, com outra data |
| Manual seguida de agendada na mesma data | ok (executado) | `H.Reexecucao.manualSeguidaDeAgendada` | SIM, sem réplica até 09/11/2026. O objeto manual de 2026-11-09 já existe, ver "Conferência pendente da execução agendada" |
| Aviso da reexecução | ok (executado) | `H.Reexecucao.avisoDaReexecucao`. Stack: novo aviso em cada reexecução, de 2026-11-06 às 03:35 e de 2026-11-07 às 03:37, entregues | STACK por métrica e tamanho. E-MAIL pendente |
| Datas de referência diferentes | ok (executado) | `H.Reexecucao.datasDiferentes`. Stack: 4 objetos convivem, `2026-10-26`, `2026-11-06`, `2026-11-07` e `2026-11-09`, cada um com a sua janela | STACK, 5.5 e 5.6 |

### Falhas

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Gravação falha | ok (executado) | `H.FalhaNaGravacao.gravacaoFalha`: exceção, 1 tentativa de gravação e 0 tentativas de publicação | SIM, sem réplica. Provocar a falha exigiria alterar a stack |
| Leitura falha em todos os dias | ok (executado) | `H.Leitura.falhaEmTodosOsDias` | SIM, sem réplica |
| Leitura falha em um dos 7 dias | ok (executado) | `H.Leitura.falhaEmUmDia`, em 2026-11-05 | SIM, sem réplica. Ver a observação D |
| Leitura interrompida no meio de um dia | ok (executado) | `H.Leitura.falhaNoMeioDeUmDia`, com falha na segunda página de 2026-11-05 | SIM, sem réplica |
| Publicação falha depois da gravação | ok (executado) | `H.FalhaNaPublicacao.publicacaoFalha`: exceção, objeto presente e 1 gravação antes da tentativa de publicar | SIM, sem réplica |
| Reexecução depois da falha na publicação | ok (executado) | `H.FalhaNaPublicacao.reexecucaoDepoisDaFalha`: uma única chave, 1 aviso com a localização e resposta normal | SIM, sem réplica |

Os três cenários de leitura usam `assertFalhouSemGravarNemAvisar`, que confere `GeracaoRelatorioException` sem causa, 0 gravações e 0 tentativas de publicação.

### Proteção dos dados do aluno nos logs

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Geração com avaliações | ok (executado) | `L.geracaoComAvaliacoes`, `L.sucessoNaoDependeDasNotas`, `L.falhaNaGravacaoNaoDependeDasNotas`, `L.falhaNaPublicacaoNaoDependeDasNotas`. Stack: 0 ocorrências do marcador, de trechos das 40 descrições, dos `id`, das notas e das médias, em todos os níveis, no CloudWatch e no X-Ray | STACK, 5.8. Caminho de falha só no teste |

A comparação entre execuções é o que prova a ausência da nota nos testes, porque um dígito solto também aparece em datas e totais. `L.capturaFunciona` prova que o appender enxerga todos os níveis e o texto de exceções.

## Cenários sem réplica na stack

| Situação | Cenários |
|---|---|
| Fecham na stack depois de 09/11/2026 (2) | `Execução agendada`, `Manual seguida de agendada` |
| Dependem de falha provocada, sem tarefa prevista (6) | Os 6 da seção Falhas |
| Dependem de massa ou horário que a stack não teve (7) | `Avaliação no primeiro dia da janela`, `Avaliação oito dias antes da referência`, `Limite final avaliado no fuso de São Paulo`, `Limite inicial avaliado no fuso de São Paulo`, `Urgência vem do que foi persistido`, `Média zero não se confunde com ausência de média`, `Dia atual calculado no fuso de São Paulo` |

Todos estão ok (executado) nos testes do handler ou do domínio.

## Defeitos

Nenhum defeito encontrado no código de produção. Nenhum código de produção, teste ou template foi alterado por este relatório.

## Observações

Não reprovam cenário. As letras A a I vêm do QA local, com o que a stack mostrou. J a P são novas.

- **A. Ligação de produção: confirmada no caminho de sucesso e de rejeição.** A stack executou o que os testes não executam: `new GerarRelatorioSemanalHandler()`, `Recursos`, os clientes reais, o `Metrics` real, o X-Ray real e as variáveis do template. Funcionou nas 9 invocações. Os caminhos de falha de leitura, gravação e publicação seguem sem execução real.
- **B. Execução agendada real.** Continua sem tarefa em `tasks.md`. O roteiro só de leitura está em "Conferência pendente da execução agendada". A decisão de incluí-lo fica com o orquestrador.
- **C. Consistência eventual: não apareceu.** Com 15 s de espera depois de cada registro, as duas avaliações entraram no primeiro acionamento. Esperas menores não foram testadas.
- **D. Leitura interrompe no primeiro dia com falha.** Sem mudança. A stack não provocou falha.
- **E. Nenhum teste lê o `template.yaml`.** Sem mudança. A stack confirmou o agendamento implantado hoje, mas uma regressão futura no template continua passando em `mvn -B verify`.
- **F. Nome de teste maior que a asserção.** Sem mudança.
- **G. Resposta sem bucket: confirmada na stack.** As 6 respostas de sucesso trouxeram só `dataReferencia`, `chave` e `quantidadeAvaliacoes`. Continua sem teste de ausência.
- **H. Observações herdadas de `add-registro-avaliacao`.** `cold_start` vem `false` em todas as linhas da aplicação, inclusive na primeira invocação depois de cada uma das 3 restaurações, e nenhuma métrica `ColdStart` foi emitida, como na observação B daquele change. O trace não trouxe mensagem de exceção do SDK, porque nenhuma chamada falhou. `jackson-datatype-jsr310` não foi conferido de novo.
- **I. Assunto com acentos: aceito.** O SNS aceitou `Relatório semanal <data>` nas 6 publicações, sem falha de entrega. A exibição no e-mail depende da conferência do Gabriel.
- **J. Primeira chamada depois da restauração: lenta, mas longe do limite.** Nas 3 amostras, a primeira `Query` levou de 2,564 a 3,142 s, contra 0,148 a 0,209 s no ambiente já restaurado. A primeira `PutObject` levou de 0,699 a 0,780 s, e a primeira `Publish`, de 0,379 a 0,442 s. Antes de qualquer leitura, a invocação já gasta de 1,303 a 1,867 s, e dentro de `## lerAvaliacoes` há de 0,583 a 0,762 s antes da primeira `Query`. A `Duration` ficou entre 6,97 e 8,36 s depois de restaurar e entre 0,88 e 1,12 s no ambiente quente. Tudo está abaixo do `apiCallTimeout` de 10 s por chamada, com a maior a 31% dele, e do `Timeout` de 120 s. Diferente da notificação, onde o primeiro `Publish` levou 4,3 s, aqui ele levou menos de 0,5 s, mas a primeira chamada ao DynamoDB concentra a demora. O QA não sabe a causa e não mediu o que acontece dentro desses intervalos. É a base de 512 MB para `add-observabilidade-seguranca`.
- **K. Nenhum WARN.** Diferente de `add-notificacao-critica`, observação K, não houve nenhuma linha WARN: toda invocação emite uma métrica, `RelatorioGerado` ou `DataReferenciaRejeitada`, então o `No metrics were emitted` não aparece. O SDK do X-Ray escreve 3 linhas INFO só na primeira invocação de cada ambiente restaurado, sem dado do aluno.
- **L. Inicializações sem invocação e ambientes não reaproveitados.** Houve 2 `INIT_START` com `INIT_REPORT` sem `START`, às 02:41:20 e às 02:58:35 UTC. A segunda é anterior ao início do QA. Além disso, cada um dos 3 blocos de invocações, separados por 7 a 8 minutos, foi atendido por um ambiente novo, com restauração. O QA não identificou a causa. Na prática, um acionamento manual isolado, como o do vídeo, tende a pagar a restauração e a primeira chamada lenta da observação J.
- **M. Rejeição de data aparece como erro da função, com stack trace.** Sem violar a spec, a rejeição tem três efeitos que convém saber:
  - `resposta.json` traz `errorType` e `stackTrace`, com nomes de classe, arquivo e linha, como `DataReferencia.java:47`. Não há dado do aluno nem da conta, mas esse arquivo aparece na tela no vídeo.
  - O runtime escreve no log uma linha em texto, fora do JSON, com a mensagem e a pilha da exceção, além da linha ERROR da aplicação. O X-Ray marca o segmento com `error` e guarda a pilha.
  - A métrica `Errors` do Lambda contou as 3 rejeições. Um alarme de `add-observabilidade-seguranca` baseado em `Errors` vai disparar por entrada inválida do administrador, e não só por falha do sistema. A métrica `DataReferenciaRejeitada` separa os dois casos.

  Devolver ao arquiteto para considerar nos alarmes. Severidade baixa.
- **N. Execuções adicionais deixam objetos e e-mails.** Os acionamentos com 2026-10-26 e 2026-11-09 e a repetição de `{}` foram feitos pelo QA para repetir cenários da spec. O objeto `relatorios/2026-10-26.txt` fica no bucket, e o de 2026-11-09 deve ser substituído na segunda. Os 4 objetos contam no esvaziamento antes de `sam delete`.
- **O. README desatualizado.** A seção "Relatório semanal: acionamento manual e download" ainda diz que a função não foi implantada e que nada foi executado na AWS. O comando, as mensagens, a regra da data, o formato do relatório e o formato do aviso conferem com o que a stack produziu. Devolver ao documentador.
- **P. Bucket e conta no aviso.** O corpo do aviso traz o nome do bucket, que inclui o ID da conta, como a D7 e os riscos do `design.md` já registram. Junto com o rodapé do SNS (observação M de `add-notificacao-critica`), isso pede atenção no vídeo e nas capturas.

## Divergências relatadas pelo dev-java

Registradas no QA local e incorporadas ao `design.md`, seção "Ajustes da implementação, 05/11/2026". Nenhuma contraria a spec. O `apiCallTimeout` de 10 s foi aprovado pelo Gabriel em 05/11/2026.

| Divergência | Parecer | Evidência |
|---|---|---|
| `ambiente` em `ResumoSemanal` | Não contraria. Sem o campo no resumo, o aviso não teria de onde tirar `Ambiente: dev`. O dado não é do aluno. Na stack, o objeto e o tamanho do aviso confirmam a linha | `ResumoSemanal`, linha 25; `FormatadorRelatorio.numeros`, linha 41; `A.textoCompleto` |
| `LocalizacaoRelatorio` novo | Não contraria. A chave segue a D5, e a classe não depende de AWS | `LocalizacaoRelatorio`; `A.localizacaoCorrespondeAChave`; `A.chavePorData` |
| Sequência em `GeradorRelatorio` em vez do handler | Não contraria a spec nem a D9. Na stack, os traces mostram a ordem ler, gravar e publicar | `GeradorRelatorio`, linhas 68 a 75; handler, linhas 129 e 130; `H.Ordem.leGravaPublica` |
| `apiCallTimeout` de 10 s | Não contraria. Na stack, a chamada mais lenta levou 3,142 s (observação J) | Handler, linhas 56 a 58 e 99; `H.Inicializacao.tempoMaximoDaChamada` |
| Métricas `RelatorioGerado`, `DataReferenciaRejeitada` e `ErroInterno` | Não contraria. Na stack, `RelatorioGerado` e `DataReferenciaRejeitada` saíram no namespace `FeedbackServerless`, com a dimensão `Service` `gerar-relatorio-semanal`. `ErroInterno` não foi exercitada | Handler, linhas 51 a 53; `H.Metricas` |
| Resposta sem o bucket | Não contraria. Confirmado na stack (observação G) | Handler, linhas 132 a 135 |
| Filtro de janela no consolidador | Não contraria. Na stack, os objetos batem byte a byte com as 7 partições lidas, sem item descartado | `ConsolidadorRelatorio`, linhas 36 a 46; `C.Janela` |

## Regras de SnapStart

Conferido no QA local por leitura e busca em `functions/gerar-relatorio-semanal/src/main` e nas classes novas do domínio, e na stack pela tarefa 5.3.

| Regra | Resultado | Evidência |
|---|---|---|
| Nada de data ou instante em campo estático | ok | Os campos estáticos são constantes, `Logger`, `Duration` e os recursos de `Recursos`. `Clock` fica em campo de instância. Na stack, invocações restauradas a partir do mesmo snapshot calcularam 2026-11-06 corretamente |
| Data calculada dentro do handler | ok | Handler, linha 129. `H.AcionamentoManual.dataCalculadaACadaInvocacao` |
| Cliente criado fora do handler | ok | `Recursos.GERADOR` é `static final`. A stack mostra que a primeira chamada a cada serviço depois da restauração é mais lenta (observação J), mas não mostra se o cliente foi criado antes do snapshot |
| Nenhum provedor de credenciais fixado | ok | 0 ocorrências de `credentialsProvider` e de `EnvironmentVariableCredentialsProvider` em `main`. Na stack, as 3 restaurações chamaram DynamoDB, S3 e SNS sem erro de credencial |
| Cache de DNS antes do logger | ok | Primeira linha do bloco estático. `H.Inicializacao.cacheDeDns` |
| Hooks de priming | ok | `PrimingSnapStartTest`, com 3 testes |
| SnapStart ativo no alias | ok | Tarefa 5.3: `On`, `Active`, versão 1, com `RESTORE_REPORT` nas restaurações |

## Trechos do template.yaml

Conferidos no texto pelo QA local. A stack confirmou os recursos gerados.

| Item | Resultado | Linhas |
|---|---|---|
| Blocos da notificação | Presentes, e o código da notificação está em `6d7553b` | 170 a 236 e 332 a 337 |
| Função | `Timeout: 120`, `AutoPublishAliasAllProperties: true` e as variáveis. Na stack, iguais | 244 a 258 |
| Política | `dynamodb:Query` em `index/por-data`, `s3:PutObject` em `relatorios/*`, `sns:Publish` no tópico e KMS com `kms:ViaService` do SNS. Na stack, iguais | 259 a 282 |
| Agendamento | `cron(0 8 ? * MON *)`, `America/Sao_Paulo`, `Input: '{}'`, `Mode: "OFF"`, 3 tentativas, 3600 s e `PermissionsBoundary` no evento. Na stack, iguais, com o boundary na role | 284 a 299 |
| Outputs | `GerarRelatorioSemanalNome` e `GerarRelatorioSemanalAliasArn`. Na stack, presentes | 338 a 343 |

**Conferência contra o bootstrap:** o deploy criou a role da função e a do agendamento com boundary, o agendamento no grupo `default` e o alias, sem erro de permissão. A previsão do QA local, feita só por leitura, se confirmou.

## Dependências

`dependency:tree` com escopo `runtime` de `gerar-relatorio-semanal`, no QA local: 0 ocorrências de `apache-client`, `apache5-client`, `netty-nio-client`, `aspectjtools` e `aspectjweaver`. Presentes: `aspectjrt` 1.9.27, `dynamodb`, `s3`, `sns` e `url-connection-client` 2.57.8. `httpclient` 4.5.14 vem do X-Ray, como em `add-registro-avaliacao`. Na stack, o pacote implantado tem 20.547.588 bytes.

## Testes acrescentados

Nenhum. O pedido limitou a edição a este arquivo, e os cenários sem réplica na stack já estão cobertos por testes do handler ou do domínio. Nenhum código de produção foi alterado.

## Prova das asserções

Em uma cópia temporária, fora do repositório, 24 mutações foram aplicadas ao código no QA local, uma de cada vez, com `mvn -o test` dos módulos `dominio` e `gerar-relatorio-semanal`. As 24 foram detectadas. A rodada não foi repetida em `6d7553b`.

| Mutação | Detectada por |
|---|---|
| Janela inclui a referência | 29 falhas, entre elas `J.limitesDaJanela`, `C.Janela.foraDaJanela` e `H.Janela.janelaDeUmaSegunda` |
| Consolidador sem filtro de janela | `C.Janela.foraDaJanela`, `C.Janela.limiteFinalEmSaoPaulo`, `C.Janela.limiteInicialEmSaoPaulo`, `C.Conteudo.ordem`. Nenhum teste do handler, porque o leitor já limita as partições |
| Dia atual em UTC | `R.ausenteUsaDiaAtualEmSaoPaulo`, `R.fusoDoRelogioNaoImporta`, `H.AcionamentoManual.diaAtualEmSaoPaulo` |
| Sem regex de formato | `R.textoInvalido`, `H.DataOpcional.valorInvalido` |
| `HALF_EVEN` na média | `C.Media.empateSobe` |
| Média zero na janela vazia | `A.semanaVazia`, `C.TotaisCompletos.janelaVazia`, `C.Media.janelaVazia`, `F.semanaVazia`, `H.SemanaVazia.janelaVazia` |
| Total por urgência recalculado da nota | `C.Conteudo.urgenciaPersistida`, `H.Conteudo.urgenciaPersistida` |
| Lista com urgência recalculada da nota | Só `H.Conteudo.urgenciaPersistida` |
| Publica antes de gravar | `H.FalhaNaGravacao.gravacaoFalha`, `H.FalhaNaPublicacao.publicacaoFalha`, `H.Ordem.leGravaPublica`, `L.falhaNaPublicacaoMostraAGravacao` |
| Lê só a primeira página | `H.Leitura.variasPaginas`, `H.Leitura.paginaVaziaNoMeio`, `H.Leitura.falhaNoMeioDeUmDia` |
| Falha de leitura ignorada | Os 3 cenários de `H.Leitura` com falha, `L.falhaDoServico` |
| Item inválido ignorado | Os testes de item inválido de `H.Leitura`, `L.itemInvalido`, `L.itemInvalidoSemId` |
| Falha de gravação ignorada | `H.FalhaNaGravacao.gravacaoFalha`, `H.Metricas.falha`, `L.falhaDoCliente` |
| Falha de publicação ignorada | `H.FalhaNaPublicacao.publicacaoFalha`, `H.FalhaNaPublicacao.reexecucaoDepoisDaFalha`, `L.falhaNaPublicacaoMostraAGravacao` |
| Chave diferente a cada execução | `A.chavePorData`, `H.Reexecucao.avisoDaReexecucao` e outros 32 |
| Objeto com ACL pública | `H.Guarda.relatorioGuardado` |
| Localização do aviso sem a chave certa | `A.localizacaoCorrespondeAChave`, `H.Aviso.localizacaoCorresponde` e outros 6 |
| Evento ignorado | `H.DataOpcional.valorInvalido`, `H.DataOpcional.rejeicaoPreservaRelatorio`, `H.Reexecucao.datasDiferentes` e outros |
| Exceção original relançada | `H.FalhaNaGravacao.gravacaoFalha`, `H.FalhaNaPublicacao.publicacaoFalha`, `L.falhaDoCliente` e outros |
| Descrição em log INFO | Só `L.geracaoComAvaliacoes` |
| Média em log INFO, sob outro nome de atributo | Só `L.sucessoNaoDependeDasNotas` |
| Notas em log INFO, sob outro nome de atributo | Só `L.sucessoNaoDependeDasNotas` |
| `apiCallTimeout` de 200 s | `H.Inicializacao.tempoMaximoDaChamada` |
| Sem `aspectjrt` no `pom.xml` | Os 3 testes de `PrimingSnapStartTest`, como a tarefa 2.8 pede |

A rodada só cobre o código Java. O `template.yaml` não passou por mutação, porque nenhum teste o lê (observação E).

## Coleção Postman

Não se aplica. O change não altera a API: o acionamento manual é uma invocação direta do Lambda (D1). As 2 avaliações deste QA usaram o endpoint existente.
