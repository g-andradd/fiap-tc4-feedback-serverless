# QA: add-notificacao-critica

Tarefas 4.1, 4.2, 5.4 a 5.8 e 7.6. A validação local foi feita em 05/11/2026 sobre o working tree do worktree `fiap-tc4-notificacao`, branch `feature/add-notificacao-critica`, e repetida no mesmo dia sobre `c6bb879`, antes da conferência na stack. A conferência na stack `feedback-dev` foi feita em 05/11/2026, sobre a execução 42113144132 (merge `c6bb879`, região `sa-east-1`).

**Validade da evidência da stack:**
- **Observação:** das 21:38 às 22:32 UTC. Os horários citados vêm do CloudWatch, do S3, do DynamoDB e do CloudFormation. O relógio da máquina estava cerca de 7 s adiantado em relação ao cabeçalho `Date` da AWS. Por isso o marcador `QA-NOTIF-202611052214-8a6274` traz 22:14, embora os registros tenham chegado entre 22:14:53 e 22:15:01 UTC.
- **Ordem:** a conferência dos itens críticos antigos no destino de falha foi feita inteira antes de qualquer tráfego do QA.
- **Escritas na conta:** 4 avaliações registradas pela API, 1 `update-item` no atributo `nota` de um item crítico criado pelo QA, e 4 requisições recusadas com 404 pelo API Gateway às 22:14:40, por caminho errado no roteiro do QA, sem nada gravado. Nada foi apagado, e nenhuma configuração foi alterada. O restante foi leitura com o perfil `tc4-admin`.

## Resultado

| Status | Quantidade |
|---|---|
| ok (executado) | 21 |
| dos quais, executados também na stack | 14 |
| dos quais, executados só no handler, com cliente simulado | 7 |
| ok (por configuração) | 4 |
| não coberto | 0 |
| falha | 0 |
| **Total de cenários** | **25** |

**O change está conforme pelo critério desta matriz, com 0 não coberto e 0 falha.** `E-mail recebido pelo administrador` ficou pendente até o Gabriel conferir a caixa de entrada em 05/11/2026: chegaram exatamente os 2 e-mails esperados, com descrição, urgência e data de envio. O QA não lê a caixa de entrada; o resultado foi relatado pelo Gabriel por meio do orquestrador e está em "Conferência do e-mail pelo Gabriel".

Os 2 cenários antes não cobertos foram resolvidos: `Sem invocação no ambiente implantado` passou a ok com execução na stack, e `E-mail recebido pelo administrador` passou a ok com a conferência do Gabriel. A mesma conferência deu a réplica no e-mail de `Campos obrigatórios presentes` e `Descrição no tamanho máximo`, que passaram a contar como executados também na stack.

Dos 9 cenários antes por configuração, 5 passaram a ok com evidência da stack: os quatro do filtro e `Sem controle de deduplicação`. Os outros 4 continuam por configuração, porque nenhuma tarefa da seção 5 provoca falha de publicação nem indisponibilidade do tópico:
- `Evento reprocessado`
- `Falha em parte de um lote`
- `Item preservado após a falha`
- `Registro com tópico indisponível`

Estes 7 seguem com prova só em `mvn -B verify`, com cliente simulado: `Lote com várias avaliações críticas`, `Data de envio vem da avaliação`, `Alteração que torna o item crítico não gera aviso`, `Remoção não gera aviso`, `Publicação rejeitada`, `Mesmo evento entregue duas vezes` e `Processamento com falha`.

Significado dos status:
- **ok (executado):** o teste rodou em `mvn -B verify` e a asserção prova o que o cenário pede no nível do handler, ou a execução na stack prova o que o cenário pede. A coluna Limite diz qual dos dois casos vale.
- **ok (por configuração):** o template, o recurso implantado, o classpath ou a inspeção garantem o comportamento, mas ele não foi exercitado. Não equivale a teste. Quando só parte das cláusulas do cenário foi executada, vale o status da cláusula mais fraca.
- **não coberto:** não há evidência do que o cenário pede. Conta como falha.
- **falha:** o comportamento observado contraria o cenário.

## Limite da evidência

Os testes usam `SnsClient` simulado com Mockito, `Metrics` simulado, `RastreadorFalso` e eventos montados com setters. Isso significa:

- **O que os testes provam:** dado o registro do Stream, o handler monta o `PublishRequest` esperado ou não publica, e devolve o `batchItemFailures` esperado.
- **O que a stack acrescentou:** a publicação real no SNS, a entrega aceita para a assinatura de e-mail, o motor de filtro do Lambda, o destino de falha no S3 com registros reais, as permissões, o SnapStart, os logs no CloudWatch e a leitura do JSON real do evento pelo runtime, todos no caminho de sucesso.
- **O que o e-mail acrescentou:** a chegada dos 2 avisos e o texto deles, conferidos pelo Gabriel na caixa de entrada. O QA não viu os e-mails.
- **O que segue sem prova real:** a falha de publicação, a nova entrega do evento, a falha parcial de lote, o log em ERROR e um lote com mais de um registro crítico chegando à função.

Na coluna Limite da matriz:
- **STACK:** além do teste do handler, quando houver, o cenário foi repetido em `feedback-dev` e os logs, as métricas, o Stream ou o recurso implantado foram conferidos.
- **SIM, sem réplica:** só cliente simulado, e a stack não reproduziu o caso. O motivo está na própria linha.
- **LOG:** captura em memória com o layout real do `log4j2.xml` e nível `ALL`, com cliente simulado. O SDK real não roda nesses testes.
- **CONFIG:** leitura do template ou do recurso implantado, com a sonda abaixo quando indicada. Nada foi exercitado.
- **E-MAIL:** conferência feita pelo Gabriel na caixa de entrada em 05/11/2026, relatada ao QA pelo orquestrador. Vale junto com a evidência STACK da mesma linha.

A **sonda** é um programa local, fora do repositório e não versionado, executado com o Python do SAM CLI e com a rede bloqueada no próprio programa. Ela aplica o transform do SAM ao `template.yaml` e lê o padrão de filtro do recurso gerado. O avaliador do padrão foi escrito à mão, seguindo a semântica da documentação citada na D1. **Ele não é o motor do Lambda.** O motor real foi exercitado na stack, nas tarefas 5.5 e 5.6.

Legenda das classes de teste:
- `H`: `br.com.fiap.feedback.notificacao.NotificarCriticaHandlerTest`, com a classe aninhada no nome
- `L`: `br.com.fiap.feedback.notificacao.LogsNotificarCriticaTest`
- `A`: `br.com.fiap.feedback.dominio.AvisoCriticoTest`

## Verificações locais

| Verificação | Resultado |
|---|---|
| `mvn -B verify` | `BUILD SUCCESS`. `dominio`: `Tests run: 97, Failures: 0, Errors: 0, Skipped: 0`. `registrar-avaliacao`: `Tests run: 127, Failures: 0, Errors: 0, Skipped: 0`. `notificar-critica`: `Tests run: 65, Failures: 0, Errors: 0, Skipped: 0` |
| `sam validate --lint --region sa-east-1` | `template.yaml is a valid SAM Template` |
| `openspec validate add-notificacao-critica --strict` | `Change 'add-notificacao-critica' is valid` |
| Template contra o `design.md` | Bloco `LogNotificarCritica` e `NotificarCritica` igual ao do design, descontado um comentário |
| `log4j2.xml` | Igual ao de `registrar-avaliacao` |
| `dependency:tree` de `notificar-critica`, escopo `runtime` | `sns`, `url-connection-client` e `aspectjrt` presentes. 0 ocorrências de `dynamodb`, `s3`, `idempotency`, `apache-client`, `apache5-client` e `netty-nio-client` |

O build local usa Microsoft OpenJDK 25 (25.0.5.0.2). A comprovação em Corretto 25 vem do CI.

Durante a validação, `README.md` apareceu alterado no worktree, com 74 linhas novas, por outro agente ou pessoa. É o único arquivo fora de `target` mais novo que o `mvn -B verify`, e este relatório não o avaliou.

### Repetição sobre `c6bb879`, antes da conferência na stack

Worktree `fiap-tc4-qa-notificacao`, com o JDK 25 definido só no comando, porque o `JAVA_HOME` do ambiente aponta para o JDK 21.0.2.

| Verificação | Resultado |
|---|---|
| Código testado | `HEAD` em `c6bb879`. No working tree, só `openspec/changes/archive/2026-11-11-add-fundacao-projeto/tasks.md` e `openspec/changes/archive/2026-11-11-add-notificacao-critica/tasks.md` estavam modificados, nenhum deles pelo QA. De `5cb6f4d` para `c6bb879` mudaram só `README.md`, `design.md`, `tasks.md` e 4 linhas do `template.yaml` |
| `mvn -B verify` | `BUILD SUCCESS`, com os mesmos totais: 97, 127 e 65 testes, 0 falhas, 0 erros e 0 ignorados. O módulo `gerar-relatorio-semanal`, que veio da `develop`, compilou sem testes executados |
| `sam validate --lint --region sa-east-1` | `template.yaml is a valid SAM Template` |
| `openspec validate add-notificacao-critica --strict` | `Change 'add-notificacao-critica' is valid` |

## Conferência na stack (tarefas 5.4 a 5.7)

### Estado da stack e da pipeline

| Item | Observado |
|---|---|
| Execução 42113144132 no `gh` | `success`, `develop`, `c6bb879`, das 21:29:45 às 21:33:29 UTC |
| Eventos do CloudFormation | `UPDATE_IN_PROGRESS` às 21:31:22, `NotificarCriticaAvaliacaoCriada` em `CREATE_COMPLETE` às 21:33:15 e `UPDATE_COMPLETE` às 21:33:19 UTC |
| Role da função | Política inline com as 4 declarações da D8, igual ao design: `sns:Publish` só em `feedback-admins-dev`; KMS em `key/*` com `kms:ViaService`; `s3:PutObject` em `<bucket>/*` e `s3:ListBucket` no bucket, os dois com `s3:ResourceAccount`. Gerenciadas: `AWSLambdaDynamoDBExecutionRole`, `AWSLambdaBasicExecutionRole` e `AWSXrayWriteOnlyAccess`. Boundary `feedback-limite-funcoes-dev` |
| `AWSLambdaDynamoDBExecutionRole`, versão padrão | Só `dynamodb:DescribeStream`, `GetRecords`, `GetShardIterator` e `ListStreams`, mais as três de `logs`. Nenhuma escrita na tabela |
| Variável `TOPICO_ADMINS_ARN` | Igual ao output `TopicoAdminsArn` |
| Tópicos SNS na conta, em `sa-east-1` | Só `feedback-admins-dev` |
| Atributos do tópico | `KmsMasterKeyId` igual a `alias/aws/sns`, sem `FifoTopic` e sem deduplicação. 1 assinatura `email` confirmada e 0 pendentes |
| Funções e mapeamentos | Só `feedback-registrar-avaliacao-dev` e `feedback-notificar-critica-dev` na região. O Stream tem um único mapeamento, o desta função |

### 5.4. SnapStart: ok

| Item | Observado |
|---|---|
| `get-function-configuration` em `feedback-notificar-critica-dev:live` | `Version: 2`, `State: Active`, `LastUpdateStatus: Successful`, `OptimizationStatus: On`, `ApplyOn: PublishedVersions`, `Runtime: java25`, 512 MB, `Timeout` de 30 s, `TracingConfig` `Active`, `LogFormat: Text` |
| `get-alias` | `live` aponta para a versão 2 |
| Versões | Só `$LATEST` e `2`, publicadas às 21:31:49 UTC, com o mesmo `CodeSha256`. `$LATEST` com `OptimizationStatus: Off`, como previsto |
| `INIT_START` e `INIT_REPORT` da versão 2 | Às 21:31:59 (`Init Duration: 13086.52 ms`) e às 21:50:13 UTC (`Init Duration: 14713.23 ms`), sem `START` depois. Não são invocações, ver a observação L |
| `RESTORE_REPORT` | `Restore Duration: 774.81 ms` e `521.67 ms`, nas duas invocações das 22:15 |
| Linhas `REPORT` | `Duration: 7248.47 ms`, com `Billed Restore Duration: 241 ms`; `Duration: 7156.70 ms`, com `Billed Restore Duration: 210 ms`. `Max Memory Used: 215 MB` nas duas |

### Itens críticos antigos no destino de falha: conforme o esperado

O mapeamento nasceu às 21:33:15 UTC, com `TRIM_HORIZON` e `MaximumRecordAgeInSeconds` 3600. O Stream ainda tinha as avaliações do QA do registro, gravadas entre 04:57 e 05:00 UTC, mais de 16 horas antes.

| Origem | Observado antes do tráfego do QA |
|---|---|
| Tabela (`scan` completo) | 34 itens, todos com um dos dois marcadores. `QA-202611050457-3e4306`: 3 `CRITICA`, 10 `MEDIA` e 3 `BAIXA`. `QA-202611050500-85eb85`: 4 `CRITICA`, 11 `MEDIA` e 3 `BAIXA`. `criadoEm` de 04:57:32 a 05:00:40 UTC |
| Stream, lido shard a shard desde `TRIM_HORIZON` (28 shards, só leitura) | 34 registros, todos `INSERT`: 7 `CRITICA`, 21 `MEDIA` e 6 `BAIXA`, com os mesmos 34 `id` da tabela. Nenhum `MODIFY` ou `REMOVE`. Nenhum outro item crítico no Stream nas últimas 24 horas |
| Destino `aws/lambda/` no bucket de relatórios | 5 objetos com 7 registros, todos `INSERT` com `CRITICA`: 3 de `3e4306` e 4 de `85eb85`. Os 7 `id` são exatamente os 7 itens críticos da tabela |

| Objeto (shard, horário da chave, sufixo) | Tamanho | `LastModified` (UTC) | Registros |
|---|---|---|---|
| `...6e872201`, 21.34.29, `bc74a732` | 2309 bytes | 21:34:30 | 2, de `3e4306` |
| `...6e872201`, 21.34.29, `63c88fec` | 1517 bytes | 21:34:30 | 1, de `85eb85` |
| `...40ebc0d6`, 21.34.29, `ec1d3d8e` | 1520 bytes | 21:34:30 | 1, de `3e4306` |
| `...d6136ffb`, 21.34.31, `07d4ba11` | 2303 bytes | 21:34:32 | 2, de `85eb85` |
| `...3223b668`, 21.34.31, `6c78aae7` | 1521 bytes | 21:34:32 | 1, de `85eb85` |

As chaves seguem `aws/lambda/<UUID do mapeamento>/<shard>/2026/11/05/<horário>-<uuid>`, como a D4 cita.

**Estrutura do objeto:**
- `requestContext`: `requestId`, `functionArn` com o alias `live`, `condition` igual a `RecordAgeExceeded` e `approximateInvokeCount` igual a 1.
- `version` (`1.0`) e `timestamp`.
- `DDBStreamBatchInfo`: `shardId`, `startSequenceNumber`, `endSequenceNumber`, `approximateArrivalOfFirstRecord`, `approximateArrivalOfLastRecord`, `batchSize` e `streamArn`.
- `payload`: texto JSON com `Records` completos, no formato do evento do Stream: `eventID`, `eventName`, `dynamodb.Keys`, `NewImage` com os 6 atributos do item, inclusive `descricao`, `SequenceNumber`, `SizeBytes` e `StreamViewType`.
- Sem `responseContext`.

Isso confirma na prática o que a D4 previa: o destino S3 guarda o lote inteiro, com a descrição, e não só metadados.

**Só os críticos foram para o destino.** Os 27 registros `MEDIA` e `BAIXA` antigos não aparecem em nenhum objeto. O filtro foi aplicado antes do descarte por idade.

**A função não foi chamada pelos antigos:**
- `Invocations`, `Errors` e `Throttles`, com e sem a dimensão `Resource` do alias: nenhum ponto entre 21:00 e 22:14 UTC. O primeiro ponto é o das 22:15, do tráfego do QA.
- Logs da função até 22:14: só as linhas `INIT_START` e `INIT_REPORT` citadas na 5.4, nenhum `START`.
- `NumberOfMessagesPublished` do tópico: nenhum ponto entre 23/10 e 22:14 UTC de 05/11. Logo, nenhum e-mail saiu por esses itens.

**Surpresa:** `approximateInvokeCount` vem igual a 1 nos 5 objetos, embora métricas e logs mostrem que a função não foi chamada. Ver a observação J.

### 5.5. Mapeamento: ok, e o filtro por `eventName` funciona

`list-event-source-mappings --function-name feedback-notificar-critica-dev:live`:

| Campo | Observado |
|---|---|
| `UUID` | `fb85c3c6-8fa1-4cf2-b4de-4d2e95f88246` |
| `FunctionArn` | `arn:aws:lambda:sa-east-1:<conta>:function:feedback-notificar-critica-dev:live` |
| `EventSourceArn` | Stream de `feedback-avaliacoes-dev`, o mesmo do output `TabelaAvaliacoesStreamArn` |
| `FilterCriteria` | `{"eventName":["INSERT"],"dynamodb":{"NewImage":{"urgencia":{"S":["CRITICA"]}}}}`, igual ao template e à D1 |
| `BatchSize` e `MaximumBatchingWindowInSeconds` | 10 e 0 |
| `StartingPosition` e `ParallelizationFactor` | `TRIM_HORIZON` e 1 |
| `MaximumRetryAttempts` e `MaximumRecordAgeInSeconds` | 5 e 3600 |
| `BisectBatchOnFunctionError` | `true` |
| `FunctionResponseTypes` | `ReportBatchItemFailures` |
| `DestinationConfig.OnFailure` | `arn:aws:s3:::<bucket>`, o bucket de relatórios. Plano B da D4 não aplicado |
| `State` | `Enabled` |

**Alteração de item crítico:** às 22:18:42 UTC, `update-item` no item `92481971-d8ce-4eca-b705-c22ae3039fd5` (marcador `QA-NOTIF-202611052214-8a6274-CRIT`), com `SET nota = 0` e condição `urgencia = CRITICA AND nota = 1`. O item continua coerente, porque nota 0 também é `CRITICA`.

| Evidência | Resultado |
|---|---|
| Stream | Registro `MODIFY` com `NewImage.urgencia` igual a `CRITICA`, criado às 22:18:41 UTC |
| Logs da função | Nenhum `START` depois de 22:15:09 até a leitura das 22:26 |
| `Invocations` | Só os 2 das 22:15. Nenhum ponto depois |
| `NumberOfMessagesPublished` | Só os 2 das 22:15 |
| `LastProcessingResult` do mapeamento | `OK`, com `LastModified` ainda em 22:15:00 UTC na leitura das 22:26 |

**Conclusão: foi o filtro por `eventName` que evitou a invocação.** O handler nem recebeu o `MODIFY`, então a segunda checagem da D2 não foi exercitada na stack e continua provada só nos testes. A contradição da documentação citada na D1 se resolve a favor do filtro: ele aceita `eventName` e não bloqueia `INSERT` crítico, como mostra a 5.6.

### 5.6. Ponta a ponta: ok, com o e-mail conferido pelo Gabriel

Envio com a URL do output `ApiAvaliacoesUrl` (`<url>`). Rodada `QA-NOTIF-202611052214-8a6274`:

| Caso | Marcador | Nota | Descrição | HTTP | `id` | `urgencia` | `criadoEm` (UTC) |
|---|---|---|---|---|---|---|---|
| Crítica curta | `QA-NOTIF-202611052214-8a6274-CRIT` | 1 | 65 caracteres, com acentos | 201 | `92481971-d8ce-4eca-b705-c22ae3039fd5` | `CRITICA` | 22:14:53.943 |
| Crítica no limite | `QA-NOTIF-202611052214-8a6274-CRIT1000` | 2 | 1000 caracteres, com acentos, terminando em `Ç` | 201 | `365f0605-133b-43ce-9c6f-e37f812d10be` | `CRITICA` | 22:15:00.758 |
| Média | `QA-NOTIF-202611052214-8a6274-MEDIA` | 5 | 48 caracteres | 201 | `dbfc559e-8532-4206-903d-7bc12e545da5` | `MEDIA` | 22:15:00.989 |
| Baixa | `QA-NOTIF-202611052214-8a6274-BAIXA` | 9 | 45 caracteres | 201 | `b7632e31-b65e-408f-8c24-960694fa4d97` | `BAIXA` | 22:15:01.222 |

Os 4 itens têm os 6 atributos do contrato e `dataEnvio` igual a `2026-11-05`. O Stream tem os 4 `INSERT`: 2 `CRITICA`, 1 `MEDIA` e 1 `BAIXA`.

| Evidência | Resultado |
|---|---|
| Invocações | 2, uma por avaliação crítica, cada uma com 1 registro, porque as duas caíram em shards diferentes. `START` às 22:15:02.101 (`3f3e5255`) e às 22:15:02.504 UTC (`d604adb9`), versão 2 |
| Logs da aplicação | 2 linhas `Aviso publicado` em INFO: `id` `92481971...` com `messageId` `1d5fd153-5409-506e-b28a-b2345625fda6` às 22:15:08.771; `id` `365f0605...` com `messageId` `8769c00a-ea34-558e-af7c-a36de819326f` às 22:15:09.521 UTC. Nenhuma linha cita os `id` da média e da baixa |
| `Invocations`, `Errors` e `Throttles` | 2, 0 e 0 às 22:15, iguais com e sem a dimensão do alias. Nenhum outro ponto no período |
| `NumberOfMessagesPublished` | 2 às 22:15 |
| `NumberOfNotificationsDelivered` e `NumberOfNotificationsFailed` | 2 e 0 às 22:15 |
| `PublishSize` | Soma 1873 bytes, máximo 1703. O tamanho em UTF-8 do corpo esperado pelo formato da D6 é 170 bytes para a crítica curta e 1703 para a de 1000 caracteres, e 170 mais 1703 dá 1873. A descrição de 1000 caracteres foi publicada inteira, ao menos em quantidade de bytes |
| Trace do X-Ray da primeira invocação | Segmento de 7,248 s: `Restore` 0,775 s, `## publicarAviso` 4,854 s, e dentro dele `SNS` `Publish` 4,301 s, sem erro. A segunda invocação não foi amostrada (`REPORT` sem linha `XRAY`) |
| Assunto com acentos | Aceito pelo SNS nas 2 publicações e exibido corretamente no e-mail, ver a observação E |

#### Conferência do e-mail pelo Gabriel

O Gabriel conferiu a caixa de entrada da assinatura de dev em 05/11/2026 e relatou o resultado ao QA por meio do orquestrador. O QA não viu os e-mails e não reproduz o corpo deles aqui.

| Item | Resultado relatado |
|---|---|
| Quantidade | Exatamente 2 e-mails, remetente AWS Notifications, às 19:15 de Brasília (22:15 UTC), o mesmo horário das 2 publicações |
| Assunto | `Avaliação crítica registrada`, com os acentos exibidos corretamente |
| Corpo dos dois | `Uma avaliação crítica foi registrada.`, `Urgência: CRITICA`, `Data de envio: 2026-11-05` e `Descrição:` seguida da descrição, no formato da D6 |
| `QA-NOTIF-202611052214-8a6274-CRIT` | Descrição curta com o marcador |
| `QA-NOTIF-202611052214-8a6274-CRIT1000` | Descrição de 1000 caracteres inteira, com acentos, terminando em `Ç`, sem corte |
| Ausências | Nenhum outro e-mail: nada com `-MEDIA`, `-BAIXA`, `3e4306` ou `85eb85` |
| Rodapé | O SNS acrescenta um link de cancelamento da assinatura que contém o ID da conta e o e-mail do assinante, ver a observação M |

Com isso, `E-mail recebido pelo administrador` passa a ok (executado), e `Campos obrigatórios presentes` e `Descrição no tamanho máximo` ganham a réplica no e-mail. A ausência de e-mail para a média, a baixa e os críticos antigos confirma, do lado do destinatário, o que as métricas do tópico já mostravam.

### 5.7. Logs: ok

Leitura completa do grupo `/aws/lambda/feedback-notificar-critica-dev`, feita entre 22:22 e 22:26 UTC, com 26 eventos.

| Verificação | Resultado |
|---|---|
| `filter-log-events` no servidor com `QA-NOTIF`, `8a6274`, `QA-`, `3e4306`, `85eb85`, `Aula`, `descricao` e `ÁÉÍÓÚ` | 0 eventos em todos |
| Leitura completa, no cliente, com os mesmos textos e mais `áudio` e `nota` | 0 ocorrências |
| Formato | 10 linhas JSON da aplicação e 16 linhas de plataforma em texto (`INIT_START` 2, `INIT_REPORT` 2, `RESTORE_START` 3, `RESTORE_REPORT` 3, `START` 2, `END` 2 e `REPORT` 2), porque a função usa `LogFormat: Text`. 0 linhas de métricas EMF. 0 linhas de outro tipo |
| Níveis da aplicação | 8 INFO e 2 WARN. Nenhuma em ERROR |
| Mensagens | `Aviso publicado` (2), `No metrics were emitted` em WARN (2, uma por invocação, ver a observação K) e 6 mensagens do SDK do X-Ray sobre `AWS_XRAY_DAEMON_ADDRESS` e `contextMissingStrategy` |
| Campos das linhas JSON | `cold_start`, `eventID`, `function_arn`, `function_memory_size`, `function_name`, `function_request_id`, `function_version`, `id`, `level`, `message`, `messageId`, `service`, `timestamp` e `xray_trace_id`. Nenhum campo com a descrição |
| Logs de `registrar-avaliacao` desde 22:00 UTC | 0 eventos com `QA-NOTIF` e com `8a6274` |
| Trace do X-Ray | 0 ocorrências de `QA-`, `8a6274`, `Aula`, `descricao`, `ÁÉÍÓÚ` e `áudio`. Confirma a observação G na stack |
| Retenção | 14 dias |

### Itens de teste

Tabela `feedback-avaliacoes-dev`, com 38 itens depois da conferência. Nenhum foi apagado.

| Marcador | Itens | Situação |
|---|---|---|
| `QA-202611050457-3e4306` e `QA-202611050500-85eb85` | 34 | Do QA de `add-registro-avaliacao`. Iguais, atributo por atributo, entre o `scan` de antes e o de depois do tráfego. Os 7 críticos estão também no destino de falha |
| `QA-NOTIF-202611052214-8a6274` | 4 | Deste QA. O crítico `-CRIT` teve `nota` alterada de 1 para 0 na 5.5 |
| `QA-NOTIF-202611052214-6d85e8` | 0 | Rodada recusada com 404 pelo API Gateway, por caminho errado no roteiro. Nada chegou à função de registro |

Os objetos de `aws/lambda/` continuam 5, sem objeto novo depois do tráfego do QA.

## Tarefa 7.6: correção do escopo do destino S3

**Primeiro deploy, com falha:**
- Execução 42103793110, do merge `5cb6f4d` (PR #8), das 20:29:04 às 20:35:57 UTC, com conclusão `failure`.
- `UPDATE_IN_PROGRESS` às 20:30:40. `NotificarCriticaAvaliacaoCriada` em `CREATE_FAILED` às 20:32:36 UTC, com a mensagem: `Resource handler returned message: "Invalid request provided: The function execution role does not have permissions to call PutObject on S3 (Service: Lambda, Status Code: 400, ...)" (..., HandlerErrorCode: InvalidRequest)`.
- O nome `InvalidParameterValueException` não aparece no evento do CloudFormation. O evento mostra só o status 400 do serviço Lambda e o código `InvalidRequest`.
- Rollback: `UPDATE_ROLLBACK_IN_PROGRESS` às 20:32:37, `NotificarCriticaAvaliacaoCriada` em `DELETE_COMPLETE` às 20:35:26 e `UPDATE_ROLLBACK_COMPLETE` às 20:35:46 UTC.
- Conclusão: o serviço aceitou S3 como destino. O que faltou foi o escopo do `PutObject`, que era `<bucket>/aws/lambda/*`.

**Teste com role e função temporárias (tarefa 7.1, feita fora deste QA e não repetida aqui):**

| Recurso de `s3:PutObject` | `s3:ResourceAccount` | Resultado |
|---|---|---|
| `<bucket>/aws/lambda/*` | sim | recusado |
| `<bucket>/aws/lambda/*` | não | recusado |
| `<bucket>/*` | sim | aceito, duas vezes |

Segundo a tarefa 7.1, os recursos temporários foram apagados. O QA confirmou só a parte do Lambda: na região há apenas as duas funções da stack, e o Stream tem um único mapeamento. Roles do IAM não foram listadas.

**Troca:** `GuardarLoteDescartado` passou a `${BucketRelatorios.Arn}/*`, mantendo `s3:ResourceAccount`. A role implantada confirma a troca, ver "Estado da stack e da pipeline".

**Segundo deploy, verde:**
- Execução 42113144132, do merge `c6bb879` (PR #9), das 21:29:45 às 21:33:29 UTC, com conclusão `success`.
- `UPDATE_IN_PROGRESS` às 21:31:22, `NotificarCriticaAvaliacaoCriada` em `CREATE_COMPLETE` às 21:33:15 e `UPDATE_COMPLETE` às 21:33:19 UTC. A stack terminou às 21:33:19. 21:31 é o horário de início da atualização.
- Prova de funcionamento: com essa permissão, o Lambda gravou os 5 objetos de `aws/lambda/` às 21:34:29 e 21:34:31 UTC, cerca de 75 s depois de o mapeamento nascer.

**Tarefa 5.9: não aplicável.** O deploy não recusou S3 como destino, e o plano B da D4 não foi aplicado. O mapeamento implantado tem o bucket de relatórios como destino.

## Matriz: notificacao-critica

### Aviso imediato de avaliação crítica

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Aviso publicado no tópico do ambiente | ok (executado) | `H.AvisoImediato.publicaNoTopicoConfigurado`. Stack: 2 críticas, 2 `Aviso publicado` com `messageId` e `NumberOfMessagesPublished` igual a 2 em `feedback-admins-dev`. `TOPICO_ADMINS_ARN` igual ao output da stack, e `sns:Publish` só nesse tópico, que é o único da conta em `sa-east-1` | STACK, 5.6 |
| Publicação na execução acionada pelo evento | ok (executado) | `H.AvisoImediato.publicaNoTopicoConfigurado`. Stack: o `Aviso publicado` de cada avaliação tem o mesmo `function_request_id` da invocação que recebeu o registro, cerca de 15 s e 9 s depois do `criadoEm`. Mapeamento com `MaximumBatchingWindowInSeconds` igual a 0 | STACK, 5.6 |
| Lote com várias avaliações críticas | ok (executado) | `H.AvisoImediato.loteComTresCriticas`: três publicações, cada uma com a sua descrição, em ordem | SIM, sem réplica. As 2 críticas da 5.6 caíram em shards diferentes e chegaram em invocações separadas |
| E-mail recebido pelo administrador | ok (executado) | Stack: assinatura `email` confirmada, `NumberOfNotificationsDelivered` igual a 2 e `NumberOfNotificationsFailed` igual a 0 às 22:15 UTC. E-mail: o Gabriel recebeu exatamente os 2 avisos, com descrição, urgência e data de envio | STACK e E-MAIL, ver "Conferência do e-mail pelo Gabriel" |

### Conteúdo obrigatório do aviso

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Campos obrigatórios presentes | ok (executado) | `H.Conteudo.camposObrigatorios`, `A.camposObrigatorios`, `A.formatoCompleto`, com o corpo exato. Stack: `PublishSize` de 170 bytes, igual ao corpo esperado para a crítica curta. E-mail: os 2 avisos trazem a descrição, `Urgência: CRITICA` e `Data de envio: 2026-11-05` | STACK e E-MAIL. O texto publicado não é visível em log, de propósito, e foi conferido no e-mail |
| Descrição no tamanho máximo | ok (executado) | `H.Conteudo.descricaoNoTamanhoMaximo`, `A.descricaoNoTamanhoMaximo`, `A.descricaoSemAlteracao`. Stack: descrição de 1000 caracteres com acentos aceita pela API, e `PublishSize` de 1703 bytes, igual ao corpo esperado com a descrição inteira. E-mail: os 1000 caracteres chegaram inteiros, com acentos, terminando em `Ç`, sem corte | STACK e E-MAIL. O tamanho vem da métrica, e o texto, da conferência do Gabriel |
| Data de envio vem da avaliação | ok (executado) | `H.Conteudo.dataEnvioPersistida`, `A.dataDeEnvioPersistida`: item de 2020-02-29, sem a data de hoje nem a de `criadoEm` | SIM, sem réplica. Na stack, data persistida e data de processamento são as duas 2026-11-05 e não se distinguem |

### Acionamento restrito a avaliações críticas

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Filtro declarado no template | ok (executado) | `template.yaml`, linha 236. Stack: o `FilterCriteria` do mapeamento implantado tem o mesmo padrão | STACK, 5.5. Inspeção do recurso implantado, que é o que o cenário pede. Nenhum teste lê o template, ver a observação B |
| Filtro aceita avaliação crítica | ok (executado) | Stack: os 2 `INSERT` com `CRITICA` invocaram a função. Antes, os 7 críticos antigos foram os únicos registros enviados ao destino de falha | STACK, 5.6, com o motor real do Lambda |
| Filtro rejeita avaliação média | ok (executado) | Stack: o `INSERT` `MEDIA` da rodada está no Stream e não gerou invocação. Os 21 `MEDIA` antigos não invocaram nem foram ao destino de falha | STACK, 5.6 |
| Filtro rejeita avaliação baixa | ok (executado) | Stack: o `INSERT` `BAIXA` da rodada está no Stream e não gerou invocação. Os 6 `BAIXA` antigos não invocaram nem foram ao destino de falha | STACK, 5.6 |
| Sem invocação no ambiente implantado | ok (executado) | Stack: com 4 registros, `Invocations` igual a 2 e `NumberOfMessagesPublished` igual a 2, os dois das críticas. Nenhum log cita os `id` da média e da baixa | STACK, 5.6 |

### Aviso somente na criação da avaliação

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Criação gera aviso | ok (executado) | `H.AvisoImediato.publicaNoTopicoConfigurado`, `H.SomenteCriacao.loteMisto`. Stack: os 2 `INSERT` críticos geraram aviso | STACK, 5.6 |
| Alteração de item crítico não gera aviso | ok (executado) | `H.SomenteCriacao.alteracaoDeItemCritico`. Stack: `MODIFY` crítico no Stream às 22:18:41, sem invocação e sem publicação. Quem barrou foi o filtro | STACK, 5.5 |
| Alteração que torna o item crítico não gera aviso | ok (executado) | `H.SomenteCriacao.alteracaoQueTornaCritico`. Com `StreamViewType: NEW_IMAGE` não existe imagem antiga, e o registro chega igual ao do cenário anterior. A 5.5 mostrou que o filtro barra esse tipo de `MODIFY` | SIM, sem réplica específica. A 5.5 alterou um item já crítico |
| Remoção não gera aviso | ok (executado) | `H.SomenteCriacao.remocao`, `H.SomenteCriacao.remocaoComImagem`. A sonda confirma que o padrão não casa | SIM, sem réplica. Nenhum item foi removido, por regra desta conferência |

### Falha na publicação não é sucesso silencioso

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Publicação rejeitada | ok (executado) | `H.FalhaNaPublicacao.publicacaoRejeitada`, com 4 tipos de exceção. `L.falhaDoServico` e `L.falhaDoCliente` conferem o nível ERROR | SIM, sem réplica. A exceção é lançada pelo mock |
| Evento reprocessado | ok (por configuração) | `H.FalhaNaPublicacao.eventoReprocessado` cobre o handler, mas o próprio teste o chama duas vezes. A nova entrega depende do mapeamento, conferido na stack: `ReportBatchItemFailures`, `MaximumRetryAttempts` 5 e `MaximumRecordAgeInSeconds` 3600 | CONFIG, sem réplica, ver a observação C |
| Falha em parte de um lote | ok (por configuração) | `H.FalhaNaPublicacao.falhaNoSegundoRegistro` cobre a primeira cláusula. O reprocessamento depende da mesma configuração | CONFIG, sem réplica, ver a observação C |

### Avaliação registrada não depende do aviso

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Item preservado após a falha | ok (por configuração) | `H.RegistroIndependente.semClienteDaTabela`. Stack: a role implantada não tem nenhuma ação de escrita no DynamoDB, nem na inline nem em `AWSLambdaDynamoDBExecutionRole`. Os 7 críticos que foram ao destino de falha por idade continuam iguais na tabela, mas isso não é falha de publicação | CONFIG, sem réplica. Nenhuma publicação falhou na stack |
| Registro com tópico indisponível | ok (por configuração) | `registrar-avaliacao` não cita SNS em `pom.xml` nem em `src/main`, e a política inline dele só tem `dynamodb:PutItem`. Stack: os registros responderam 201, e o aviso saiu de 9 a 15 s depois, por outra função | CONFIG, sem réplica. O tópico não foi tornado indisponível |

### Entrega pelo menos uma vez, sem deduplicação

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Mesmo evento entregue duas vezes | ok (executado) | `H.SemDeduplicacao.eventoRepetido`: duas publicações e nenhuma falha | SIM, sem réplica |
| Sem controle de deduplicação | ok (executado) | Inspeção da tarefa 4.2, repetida sobre `c6bb879`. Stack: o tópico implantado não é FIFO e não tem deduplicação, e a role não tem acesso a armazenamento de avisos | STACK, inspeção do recurso implantado, que é o que o cenário pede. Nenhum teste lê o template, ver a observação B |

### Descrição fora dos logs em nível INFO

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Processamento com sucesso | ok (executado) | `L.sucesso`, `L.registroIgnorado`, `L.capturaFunciona`. Stack: 0 ocorrências dos marcadores e de trechos das descrições, em todos os níveis, no CloudWatch e no X-Ray | STACK, 5.7 |
| Processamento com falha | ok (executado) | `L.falhaDoServico`, `L.falhaDoServicoSemDetalhes` e `L.falhaDoCliente`: marcador na descrição e na mensagem da exceção, ausente de todas as linhas | LOG, sem réplica. A stack só viu o caminho de sucesso, ver a observação C |

## Tarefa 4.2: inspeção

| Item | Resultado | Evidência |
|---|---|---|
| Armazenamento ou consulta de avisos | Não há | Busca por `S3Client`, `PutObject`, `idempot`, `dedup`, `ConcurrentHashMap`, `Set<` e `Cache` no código da função, no `pom.xml` e em `AvisoCritico`: a única ocorrência é `networkaddress.cache.ttl`, que é o cache de DNS. Os campos estáticos e de instância do handler não guardam estado de aviso. A árvore `runtime` não tem `s3` nem `idempotency`. Repetida sobre `c6bb879`, sem ocorrência nova |
| Escrita na tabela | Não há | Sem `DynamoDbClient` no classpath, sem as operações de escrita no código e sem `dynamodb` nas ações inline da role. Na conta, `AWSLambdaDynamoDBExecutionRole` só tem leitura do Stream e `logs` |
| Deduplicação no tópico | Não há | `TopicoAdmins` tem só `TopicName` e `KmsMasterKeyId`. 0 ocorrências de `fifo`, `ContentBasedDeduplication` e `dedup` no template. `PublishRequest` só preenche `topicArn`, `subject` e `message`. Na conta, o tópico implantado não tem `FifoTopic` |

O destino de falha `aws/lambda/` no bucket guarda o lote que esgotou as tentativas ou a idade, com a descrição (D4), como a stack confirmou. Quem grava é o serviço Lambda. O código da função não lê esse prefixo, então ele não funciona como controle de duplicata.

## Defeitos

Nenhum defeito encontrado no código de produção. Nenhum código de produção, teste ou template foi alterado por este relatório.

## Observações

Não reprovam cenário. As letras A a G vêm do QA local, com o que a stack e o e-mail mostraram. H a M são novas.

- **A. Filtro por `eventName`: resolvido na stack.** O QA local temia que o padrão nunca casasse e que não houvesse plano B. Na stack, o filtro casou com os `INSERT` críticos, que geraram aviso, e barrou `MEDIA`, `BAIXA` e o `MODIFY` crítico. O risco não se confirmou em dev. Hml e prod usam o mesmo template, mas só os deploys desses ambientes provam o mesmo comportamento lá.
- **B. Nenhum teste protege o filtro nem o tópico.** Sem mudança. 0 testes leem `template.yaml`. A stack confirmou o filtro implantado hoje, mas uma regressão futura no template continua passando em `mvn test`.
- **C. O caminho de falha segue sem execução real.** Nenhuma publicação falhou na stack. Seguem sem prova real: a nova entrega do evento, a falha parcial de lote, o log em ERROR no CloudWatch e a ausência do marcador nesse caminho. A stack provou uma parte do caminho de descarte: o destino S3 recebe o lote com `condition` igual a `RecordAgeExceeded`, com os registros completos. O descarte por tentativas esgotadas não foi visto. Uma tarefa que provoque a falha exige alteração temporária na stack, a critério do orquestrador e do Gabriel.
- **D. Ligação de produção.** A stack executou, no caminho de sucesso, o que os testes não executam: `new NotificarCriticaHandler()`, o SDK real, o `Metrics` real, o X-Ray real e a leitura do evento a partir do JSON do runtime. Funcionou nas 2 invocações.
- **E. Assunto com acentos: resolvido.** O SNS aceitou `Avaliação crítica registrada` nas 2 publicações, e o Gabriel confirmou que o assunto e a descrição de 1000 caracteres aparecem no e-mail com os acentos corretos.
- **F. Prazo da D3 vale por chamada, não por lote.** Sem mudança, e agora com um número, ver a observação I.
- **G. Trace do X-Ray.** Confirmado na stack: o trace da primeira invocação não tem marcador nem descrição.
- **H. `Errors` não deve mostrar a falha de publicação.** Com `ReportBatchItemFailures`, o handler devolve a falha em `batchItemFailures` e termina normalmente, então a invocação não é um erro de função e, pela definição da métrica, não entra em `Errors`. A falha só aparece no log em ERROR e no reprocessamento. Na stack, `Errors` ficou em 0 nas 2 invocações, mas nenhuma falha foi provocada, então isso continua sem observação real. Afeta os alarmes de `add-observabilidade-seguranca`, que não devem contar só com `Errors` para esta função.
- **I. Primeira publicação depois do restore levou 4,3 s, perto do `apiCallTimeout` de 5 s.** No único trace amostrado, `SNS` `Publish` durou 4,301 s, dentro de `## publicarAviso` com 4,854 s. As duas invocações tiveram `Duration` acima de 7 s. Se a primeira chamada depois de um restore passar de 5 s, a publicação falha, o evento volta pela D3 e o aviso atrasa, com chance de duplicata, que a spec aceita. É uma amostra só, e invocações seguintes em um ambiente já restaurado não foram medidas. Devolver ao arquiteto, para decidir se o priming do SnapStart deve cobrir o cliente SNS ou se o `apiCallTimeout` deve subir. Severidade média.
- **J. `approximateInvokeCount` igual a 1 sem invocação.** Os 5 objetos do destino trazem `approximateInvokeCount: 1`, mas `Invocations`, os logs e `NumberOfMessagesPublished` mostram que a função não foi chamada para esses registros. O QA não encontrou na conta nada que explique o valor. Quem usar esse campo para saber se houve tentativa de aviso vai concluir errado.
- **K. `WARN` em toda invocação com sucesso.** O Powertools registra `No metrics were emitted` em WARN quando a invocação não emite métrica, o que é o caso normal desta função, porque `ItemInvalido` só sai no descarte da D5. Isso põe uma linha WARN em toda invocação e atrapalha filtro ou alarme por nível WARN. Não viola a spec. Devolver ao dev-java, para avaliar com o arquiteto. Severidade baixa. Cada invocação também gera 3 linhas INFO do SDK do X-Ray, que são ruído e não têm dado do aluno.
- **L. Inicializações e restaurações sem invocação.** Além do `INIT` da publicação da versão às 21:31:59, houve um `INIT_START` às 21:50:13 e um `RESTORE_START` com `RESTORE_REPORT` às 22:22:15 UTC, os dois sem `START` e sem ponto em `Invocations`. O QA não identificou a causa. Não afetam métricas de invocação nem geram aviso, mas aparecem no log e devem ser lembrados ao ler o grupo.
- **M. Rodapé do e-mail expõe dados no vídeo.** O SNS acrescenta ao fim de todo e-mail um link para cancelar a assinatura, e esse link contém o ID da conta e o e-mail do assinante. O texto não vem do código da função e não viola a spec. Ao gravar o vídeo da entrega, desfocar o rodapé, ou rolar a tela para que ele não apareça. Vale também para capturas de tela no relatório da FIAP.

## Permissões

O QA local conferiu as permissões só por leitura do bootstrap e de referências públicas. A stack confirmou:

| Item | Resultado |
|---|---|
| Ações do CloudFormation para criar o mapeamento | O segundo deploy criou `NotificarCriticaAvaliacaoCriada` sem erro. O primeiro falhou pela permissão da role da função, não pela role de deploy |
| Condição `lambda:FunctionArn` | O mapeamento foi criado apontando para o alias `live` |
| Políticas gerenciadas | As três aceitas em `AnexarPoliticasDoSam` estão anexadas à role |
| Boundary | `feedback-limite-funcoes-dev` na role. Publicação no tópico com KMS e gravação no bucket funcionaram com ele |
| Mapeamento implantado | `FunctionArn` no alias `live`. Política de reprocessamento e destino iguais à D3 e à D4 |

## Prova das asserções

Em uma cópia temporária, fora do repositório, 20 mutações foram aplicadas, uma de cada vez, com `mvn test` e sem `install`. A cópia sem mutação passou nos 162 testes de `dominio` e `notificar-critica`. 18 mutações foram detectadas. As 2 que sobreviveram são as da observação B.

| Mutação | Detectada por |
|---|---|
| Publica em outro tópico | `H.AvisoImediato.publicaNoTopicoConfigurado` |
| Só publica com 2 ou mais registros no lote | `H.AvisoImediato.publicaNoTopicoConfigurado` e outros 20 testes |
| Publica só o primeiro do lote | `H.AvisoImediato.loteComTresCriticas` e outros 3 |
| Sem checagem de `eventName` | `H.SomenteCriacao.alteracaoDeItemCritico`, `alteracaoQueTornaCritico`, `remocaoComImagem` e `loteMisto`, e outros 2 |
| Sem checagem de urgência | `H.Urgencia.outraUrgencia`, `H.ItemInvalido.ignoradoNaoContaComoInvalido` |
| Descrição cortada em 999 caracteres | `H.Conteudo.descricaoNoTamanhoMaximo`, `A.descricaoNoTamanhoMaximo`, `A.descricaoComEmoji` |
| Reticências no fim | `H.Conteudo.descricaoNoTamanhoMaximo`, `A.descricaoNoTamanhoMaximo` e outros 7 |
| Data do processamento no lugar da persistida | `H.Conteudo.dataEnvioPersistida`, `A.dataDeEnvioPersistida` e outros 3 |
| Falha reportada como sucesso | `H.FalhaNaPublicacao.publicacaoRejeitada`, `eventoReprocessado`, `falhaNoPrimeiroRegistro` e `falhaNoSegundoRegistro` |
| Sequência do primeiro registro em vez da que falhou | `H.FalhaNaPublicacao.falhaNoSegundoRegistro` |
| Continua publicando depois da falha | `H.FalhaNaPublicacao.falhaNoPrimeiroRegistro`, `falhaNoSegundoRegistro` |
| Deduplicação por `eventID` | `H.SemDeduplicacao.eventoRepetido`, `H.FalhaNaPublicacao.eventoReprocessado` |
| Descrição em INFO no sucesso | `L.sucesso` |
| Evento recebido em INFO | `L.sucesso`, `L.falhaDoServico` e outros 6 |
| Exceção passada ao logger | `L.falhaDoServico`, `L.falhaDoCliente`, `L.falhaDoServicoSemDetalhes` |
| Falha em WARN em vez de ERROR | `L.falhaDoServico`, `L.falhaDoCliente`, `L.falhaDoServicoSemDetalhes` |
| Cliente DynamoDB no classpath | `H.RegistroIndependente.semClienteDaTabela` |
| `aspectjrt` removido do `pom.xml` (tarefa 2.6) | `PrimingSnapStartTest`: os 3 testes falham, com `NoClassDefFoundError: org/aspectj/lang/NoAspectBoundException` |
| Filtro do template sem a urgência | **Não detectada** |
| Tópico FIFO com deduplicação no template | **Não detectada** |

## Coleção Postman

Este change não altera a API, então a coleção em `docs/postman/` não mudou.
