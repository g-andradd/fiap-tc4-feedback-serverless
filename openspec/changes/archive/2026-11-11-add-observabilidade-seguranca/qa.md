# QA: add-observabilidade-seguranca

Quatro rodadas:

- **Local, tarefas 5.1 a 5.3:** 06/11/2026, sobre o working tree do worktree `fiap-tc4-observabilidade`, branch `feature/add-observabilidade-seguranca`, sem commit. Nada foi escrito na AWS nessa rodada.
- **Dev, tarefas 6.4 a 6.19, exceto a 6.17, que é do Gabriel:** 06/11/2026, na stack `feedback-dev`, sobre a execução 42278391413 do workflow Deploy (merge `e15f842` do PR #13 em `develop`, `success`, das 19:18:00 às 19:22:06 UTC, região `sa-east-1`). Feita no worktree `fiap-tc4-qa-observabilidade`, branch `feature/add-observabilidade-seguranca-qa`.
- **Release, tarefas 8.1 e 8.2:** 07/11/2026, com `feedback-dev` e `feedback-hml` no ar ao mesmo tempo. `feedback-hml` foi criada pela execução 42335524756 do Deploy (push em `release/1.0.0`, commit `b730a59`, `success`, `CREATE_COMPLETE` às 03:51:37 UTC). Feita no checkout principal, branch `release/1.0.0`. Está em "Conferência na release (07/11/2026)". A validade da evidência dessa rodada vale só para ela e está descrita lá.
- **Prod, tarefas 8.3 e parte de prod da 8.1:** 10 e 11/11/2026, só leitura, na stack `feedback-prod`, criada pela execução 42438770120 do Deploy (push em `main`, commit `6eee512`, `success`, `CREATE_COMPLETE` às 03:01:58 UTC de 08/11). Repete a leitura de 08/11, que não tinha sido registrada. Está em "Prod, release 1.0.0 (leitura)".

**Validade da evidência da stack:**
- **Deploy:** a stack foi de `UPDATE_IN_PROGRESS` às 19:19:58 a `UPDATE_COMPLETE` às 19:21:59 UTC. O CloudTrail mostra `CreateChangeSet`, `ExecuteChangeSet`, `CreateTopic`, `Subscribe`, os 3 `PutMetricFilter`, os 3 `PutMetricAlarm` e o `PutDashboard` feitos pela role `github-deploy-feedback-dev`.
- **Observação:** das 19:31 às 22:25 UTC de 06/11/2026, ou seja, das 16:31 às 19:25 em Brasília. Horários em UTC, vindos do CloudWatch, do X-Ray, do DynamoDB e do CloudTrail. O relógio da máquina estava cerca de 7 s adiantado em relação à AWS: o `ingestionTime` de cada linha de teste é 7 s anterior ao `timestamp` que o QA gravou. Em todas as linhas, os dois caem no mesmo minuto, então o ponto da métrica não mudou de minuto.
- **Ordem:** a linha de base (configuração das funções, logs da publicação, tabela, bucket, tópicos, métricas, alarmes, dashboard, roles e políticas) foi lida antes de qualquer tráfego do QA. Até as 19:41, nenhuma das três funções tinha sido invocada depois do deploy.
- **Escritas na conta, todas permitidas pela regra da seção 6:** 3 avaliações registradas pela API (201) e 5 requisições rejeitadas (400); 4 invocações do alias `live` do relatório (3 com sucesso, que regravaram `relatorios/2026-11-06.txt`, e 1 rejeitada); 1 invocação do alias `live` da notificação com evento sintético; 3 `set-alarm-state`; 1 log stream `qa-observabilidade-20261106` em cada um dos três grupos, com 20 linhas no total. As 3 avaliações passaram pelo Stream e invocaram a notificação 3 vezes. Os avisos aos administradores foram 6: 3 de avaliação crítica e 3 de relatório. Nada foi apagado. Nenhuma configuração, código, versão, `$LATEST` ou agendamento foi alterado. Nenhum alarme, dashboard ou filtro foi criado. O restante foi leitura com o perfil `tc4-admin`.

## Resultado

Atualizado em 11/11/2026 com a leitura de prod e com as duas confirmações do Gabriel (6.17 e 6.19).

| Status | 06/11/2026 | 07/11/2026 | 11/11/2026, leitura de prod | 11/11/2026, com 6.17 e 6.19 |
|---|---|---|---|---|
| ok (executado) | 54 | 56 | 57 | 59 |
| dos quais, executados também na stack, na conta ou no CI | 50 | 52 | 53 | 55 |
| dos quais, executados só nos testes, com clientes simulados | 4 | 4 | 4 | 4 |
| ok (por configuração) | 5 | 4 | 4 | 4 |
| não coberto | 3 | 2 | 1 | 0 |
| pendente do Gabriel | 1 | 1 | 1 | 0 |
| falha | 0 | 0 | 0 | 0 |
| **Total de cenários** | **63** | **63** | **63** | **63** |

Mudanças de 07/11: `Falha em outro ambiente` saiu de não coberto e `Inspeção das definições` saiu de por configuração, as duas para ok (executado), com a stack `feedback-hml` implantada. A conclusão da 8.1 em hml, às 05:50, acrescentou evidência de hml a três cenários e não mudou nenhum status.

Mudança de 11/11: `Retenção em prod` saiu de não coberto para ok (executado), com os três grupos de `feedback-prod` em 30 dias (8.3). A leitura de prod acrescentou evidência de prod a outros cenários, sem mudar status.

Mudanças das confirmações do Gabriel, registradas em 11/11:
- `Alarme em ALARM notifica` saiu de pendente do Gabriel para ok (executado): os 12 e-mails de alarme das provas de dev chegaram, sem descrição nem nota (6.17).
- `Computação com 1024 MB` saiu de não coberto para ok (executado): leitura do console do nível gratuito em 07/11/2026 (6.19).

Por spec, em 11/11: `observabilidade` 47 (45 executados, 2 por configuração, 0 não cobertos, 0 pendentes do Gabriel), `seguranca` 13 (13, 0, 0, 0) e `entrega-continua` 3 (1, 2, 0, 0).

**O change está conforme pelo critério desta matriz, com 0 não coberto, 0 pendente e 0 falha.** Os 4 cenários ok (por configuração) não são falha: o template ou o workflow garantem o comportamento, e a linha de cada um diz por que não houve execução.

Em 07/11, dois cenários impediam a conformidade: `Retenção em prod`, fechado em 11/11 (8.3), e `Computação com 1024 MB`, fechado pela conferência do Gabriel no console (6.19).

**Tarefa 8.1 completa em hml e, pelo estado das assinaturas, em prod:** as duas assinaturas de prod estão confirmadas (ver "Prod, release 1.0.0 (leitura)"). Em prod, o caminho do alarme até o e-mail não foi exercitado, porque `set-alarm-state` estava proibido nesta rodada.

**Tarefa 8.1 em hml, registro de 07/11:** a assinatura do tópico de alarmes de hml, pendente até 05:18 UTC de 07/11, foi confirmada pelo Gabriel em 07/11/2026 e conferida às 05:49 UTC. O `set-alarm-state` de um alarme de hml às 05:50 publicou e entregou 1 mensagem no tópico de alarmes de hml, sem falha de entrega. Os cenários correspondentes, `Assinatura criada no deploy`, `Alarme em ALARM notifica` e `Publicação pelo CloudWatch mantida`, ganham a réplica em hml, mas os totais não mudam: os dois primeiros já tinham o status definido pela evidência de dev, e `Alarme em ALARM notifica` seguia pendente do Gabriel pela caixa de entrada de dev (6.17), fechada em 11/11. Ver "8.1".

**E-mails de alarme confirmados pelo Gabriel (6.17):** o QA confirmou cada publicação no tópico de alarmes e a entrega à assinatura, sem nenhuma falha de entrega, e o Gabriel confirmou que os 12 e-mails das provas de dev chegaram, sem descrição nem nota. A lista está em "E-mails para o Gabriel conferir (6.17)".

**Nenhuma falha.** Nenhum comportamento observado na stack contraria a spec. Nenhum defeito no código de produção.

Significado dos status:
- **ok (executado):** o teste rodou em `mvn -B install` e a asserção prova o que o cenário pede, ou a execução na stack ou no CI prova o que o cenário pede. Cenário cuja ação é inspecionar um recurso conta como executado quando a inspeção foi feita no recurso implantado, e não só no template. A coluna Limite diz qual caso vale.
- **ok (por configuração):** o template ou o workflow garantem o comportamento, mas ele não foi exercitado nem inspecionado num recurso implantado. Não equivale a teste.
- **não coberto:** não há evidência do que o cenário pede. Conta como falha.
- **pendente do Gabriel:** a evidência depende de uma conferência que só o Gabriel faz, como a caixa de entrada.
- **falha:** o comportamento observado contraria o cenário.

## Limite da evidência

Os testes usam clientes do SDK simulados com Mockito ou falsos (`DynamoFalso`, `S3Falso`, `SnsFalso`), `Metrics` simulado e `RastreadorFalso`. Os testes de log capturam em memória com o layout real do `log4j2.xml` e nível `ALL`.

- **O que os testes provam:** em cada caminho de falha, o handler grava uma linha em ERROR com `alarme` igual a `FALHA_FUNCAO`; nos caminhos sem falha e nas rejeições, nenhuma linha contém `FALHA_FUNCAO` e nenhuma está em WARN ou ERROR; o `Error` que escapa é registrado com o marcador e relançado; o hook de aquecimento chama só a leitura prevista em cada cliente, não grava nada e não propaga falha; a notificação só descarrega métricas quando contou alguma.
- **O que a stack acrescentou:** o casamento da linha com o filtro implantado, o ponto na métrica, a transição do alarme, a publicação no tópico de alarmes e a entrega à assinatura, a política do tópico negando HTTP, o hook rodando antes do snapshot real, a memória e a retenção aplicadas, o dashboard com os dados do período, a ausência do WARN real do Powertools e os tempos depois da restauração com 1024 MB. O descarte de item inválido foi a única falha real provocada, sem alterar a função.
- **O que segue sem prova real:** a falha interna do registro, a falha de publicação da notificação, a execução com falha do relatório e o tempo esgotado. Na stack, essas falhas chegaram ao alarme por linhas de teste gravadas com `put-log-events` (D14). Que o handler grava essas linhas é provado pelos testes. O texto real da linha de tempo esgotado do runtime `java25` continua não observado (observação I).
- **O que depende do e-mail:** o recebimento das notificações na caixa de entrada (6.17), confirmado pelo Gabriel para os 12 e-mails de dev.

Na coluna Limite da matriz:
- **LOG:** captura em memória com o layout real, clientes simulados.
- **SIM:** só clientes simulados, sem captura de log relevante.
- **CONFIG:** leitura do `template.yaml` ou do `ci.yml`, com a sonda descrita em "Inspeção do template". Nada exercitado.
- **LOCAL:** execução local de ferramenta (`sam build`) fora do CI.
- **STACK:** executado em `feedback-dev`. A linha diz como.
- **INSP:** inspeção do recurso implantado em `feedback-dev` ou na conta.
- **LINHA:** o caminho filtro, métrica, alarme e tópico foi exercitado na stack por linha de teste gravada com `put-log-events`, e não por falha real.
- **CI:** execução do workflow no GitHub.
- **E-MAIL:** depende da conferência do Gabriel.
- **RELEASE:** só fecha com hml ou prod implantados.

Legenda das classes de teste:
- `LR`: `br.com.fiap.feedback.registro.LogsRegistrarAvaliacaoTest`
- `AR`: `br.com.fiap.feedback.registro.AquecimentoTest`
- `LN`: `br.com.fiap.feedback.notificacao.LogsNotificarCriticaTest`
- `HN`: `br.com.fiap.feedback.notificacao.NotificarCriticaHandlerTest`, com a classe aninhada no nome
- `AN`: `br.com.fiap.feedback.notificacao.AquecimentoTest`
- `LG`: `br.com.fiap.feedback.relatorio.LogsGerarRelatorioSemanalTest`
- `AG`: `br.com.fiap.feedback.relatorio.AquecimentoTest`

## Conferência em dev

### 6.4. Versão publicada, memória e aquecimento: ok

`get-function-configuration` no alias `live`:

| Função | Versão | `State` | `OptimizationStatus` | `MemorySize` | `POWERTOOLS_METRICS_NAMESPACE` | `LastModified` |
|---|---|---|---|---|---|---|
| `feedback-registrar-avaliacao-dev` | 7 | `Active` | `On` | 1024 | `FeedbackServerless/dev` | 19:20:29 |
| `feedback-notificar-critica-dev` | 4 | `Active` | `On` | 1024 | `FeedbackServerless/dev` | 19:20:30 |
| `feedback-gerar-relatorio-semanal-dev` | 2 | `Active` | `On` | 1024 | `FeedbackServerless/dev` | 19:20:30 |

Log da publicação de cada versão, entre 19:20:40 e 19:20:49, com `function_request_id` nulo, porque o hook roda fora de invocação:

| Função | `Cliente aquecido` (serviço, operação, `duracaoMs`) | `INIT_REPORT` |
|---|---|---|
| registro | DynamoDB, `DescribeTable`, 1760 | 7.636,56 ms |
| notificação | SNS, `GetTopicAttributes`, 1602 | 7.350,11 ms |
| relatório | DynamoDB, `Query`, 1798; S3, `HeadBucket`, 317; SNS, `GetTopicAttributes`, 166 | 8.576,50 ms |

Nenhuma linha em WARN ou ERROR no log da publicação: 6, 6 e 8 linhas, todas `INIT_START`, INFO do X-Ray SDK, `Cliente aquecido` e `INIT_REPORT`. Nenhum `Falha no aquecimento`. O aquecimento do SNS passou, o que confirma o boundary com `sns:GetTopicAttributes` aplicado na seção 2.

### 6.5. Deploy sem efeito em dados e retenção: ok

Nenhuma invocação entre 04:00 UTC e o primeiro envio do QA às 19:41, pelo `Invocations` das três funções e pelos logs. Por isso, qualquer gravação nesse intervalo só poderia ter vindo do deploy.

| Conferência | Antes do QA (19:34) | Evidência de que o deploy não gravou |
|---|---|---|
| Itens da tabela | 40, a mesma contagem que o QA de `add-relatorio-semanal` deixou (34 + 4 + 2) | Maior `criadoEm` é 03:36:41 UTC de 06/11. `ConsumedWriteCapacityUnits` sem nenhum ponto diferente de zero de 03:40 a 19:40 |
| Tópico de administradores | | `NumberOfMessagesPublished`: só os 6 pontos do QA do relatório, de 03:25 a 03:40. Nenhum depois |
| Bucket | 9 objetos | O mais recente em `relatorios/` é de 03:44:21 UTC de 06/11. Nenhum objeto com `LastModified` na janela do deploy |

Depois do QA, a tabela tem 43 itens (as 3 avaliações do QA) e o bucket continua com 9 objetos: o relatório regravou `relatorios/2026-11-06.txt`, último `LastModified` 20:28:18.

`describe-log-groups`: retenção de 14 dias nos três grupos, `/aws/lambda/feedback-registrar-avaliacao-dev`, `/aws/lambda/feedback-notificar-critica-dev` e `/aws/lambda/feedback-gerar-relatorio-semanal-dev`.

### 6.6. Tópico de alarmes: ok

| Conferência | Resultado |
|---|---|
| Nome e ARN | `feedback-alarmes-dev`, igual ao output `TopicoAlarmesArn`, diferente de `feedback-admins-dev` |
| Atributos | `DisplayName`, `EffectiveDeliveryPolicy`, `Owner`, `Policy`, `SubscriptionsConfirmed`, `SubscriptionsDeleted`, `SubscriptionsPending` e `TopicArn`. **Sem `KmsMasterKeyId`**. O tópico de administradores segue com `alias/aws/sns` |
| Assinatura | 1 assinatura `email`, confirmada (`SubscriptionsConfirmed` 1, `SubscriptionsPending` 0). `NumberOfNotificationsDelivered` com 1 entrega às 19:20, a mensagem de confirmação |
| Política | `Id` `feedback-alarmes-dev-politica`. `NegaAcessoSemTls`: `Deny`, `Principal` `*`, `Bool` `aws:SecureTransport` `"false"`, as 11 ações da D2, no ARN do tópico. `PublicacaoPelosAlarmesDoAmbiente`: `Allow` só de `sns:Publish` para `cloudwatch.amazonaws.com`, com `StringEquals` `aws:SourceAccount` igual à conta e `ArnLike` `aws:SourceArn` igual a `arn:aws:cloudwatch:sa-east-1:<conta>:alarm:feedback-*-dev`. Nenhum outro statement |

**Requisição sem TLS.** O endpoint `http://sns.sa-east-1.amazonaws.com` aceita HTTP: uma requisição sem credencial recebeu `403 MissingAuthenticationToken` do próprio SNS. Com isso, o QA fez a requisição assinada da D14 às 19:37:11:

| Requisição | Resultado |
|---|---|
| `get-topic-attributes` no tópico de alarmes, `--endpoint-url http://sns.sa-east-1.amazonaws.com` | `AuthorizationError`: `... is not authorized to perform: SNS:GetTopicAttributes on resource: arn:aws:sns:sa-east-1:<conta>:feedback-alarmes-dev with an explicit deny in a resource-based policy` |
| Controle: a mesma leitura sem TLS no tópico de administradores, que não tem a negação | `InvalidSecurity: Requests to an encrypted topic must use HTTPS and Sigv4`. Outro motivo, do próprio tópico criptografado: mostra que o pedido HTTP chega ao serviço e que a recusa do tópico de alarmes vem da política |
| A mesma leitura com TLS no tópico de alarmes | Aceita |

A requisição assinada em HTTP expõe o token de sessão da credencial SSO no caminho de rede. Foram duas requisições, previstas na D14, com credencial de curta duração.

### 6.7. Alarmes e caminho até o e-mail: ok, e-mail confirmado pelo Gabriel (6.17)

**Inspeção antes do teste** (`describe-alarms` em `sa-east-1`):

| Alarme | Métrica | `Period` | Avaliação | Limiar | `TreatMissingData` | `AlarmActions` | `OKActions`, `InsufficientDataActions` |
|---|---|---|---|---|---|---|---|
| `feedback-falha-registrar-avaliacao-dev` | `FeedbackServerless/dev` `FalhasRegistrarAvaliacao`, sem dimensão, `Sum` | 60 | 5 períodos, 1 ponto | `>= 1` | `notBreaching` | só `feedback-alarmes-dev` | vazias |
| `feedback-falha-notificar-critica-dev` | `FalhasNotificarCritica`, idem | 60 | 5, 1 | `>= 1` | `notBreaching` | só `feedback-alarmes-dev` | vazias |
| `feedback-falha-gerar-relatorio-semanal-dev` | `FalhasGerarRelatorioSemanal`, idem | 60 | 5, 1 | `>= 1` | `notBreaching` | só `feedback-alarmes-dev` | vazias |

Nenhum alarme aponta para o tópico de administradores. Nenhum usa `Metrics`, `ExtendedStatistic` ou período abaixo de 60. Os três saíram de `INSUFFICIENT_DATA` para OK entre 19:21:21 e 19:21:44, sem dado.

**`set-alarm-state` para ALARM**, com o motivo `QA 6.7 set-alarm-state: teste do caminho do alarme ate o e-mail`:

| Alarme | ALARM | Ação | Volta a OK |
|---|---|---|---|
| registro | 19:42:09 | `Successfully executed action arn:aws:sns:sa-east-1:<conta>:feedback-alarmes-dev`, `Succeeded` | 19:43:29 |
| notificação | 19:42:10 | idem | 19:43:10 |
| relatório | 19:42:11 | idem | 19:43:24 |

`NumberOfMessagesPublished` do tópico de alarmes: 3 às 19:42. `NumberOfNotificationsDelivered`: 3 às 19:42. `NumberOfNotificationsFailed`: 0. Os três voltaram a OK sozinhos, entre 60 e 80 s depois, com o motivo `no datapoints were received for 5 periods and 5 missing datapoints were treated as [NonBreaching]`.

### 6.8 a 6.10. Janelas A, B e C da D14: ok

**Antes das janelas**, às 19:41:10, o QA registrou pela API a avaliação crítica `fe567141-6f28-4b4e-ba19-6343a701e7d8`, nota 1, com a descrição `QA-OBS-202611061941-f15ff8 Aula sem áudio, ação urgente`. Ela foi também o primeiro bloco de medição da 6.15. Os blocos seguintes registraram mais duas avaliações com marcador próprio: `QA-OBS-202611061958-764655` (nota 2) e `QA-OBS-202611062028-1745e9` (nota 0). A 6.12 procurou os três.

**Linhas de teste.** Stream `qa-observabilidade-20261106`, criado pelo QA em cada grupo às 19:45. As linhas da aplicação partem de uma linha real do mesmo grupo, gravada no bloco M1, com os campos do layout na mesma ordem (`level`, `message`, `cold_start`, `function_arn`, `function_memory_size`, `function_name`, `function_request_id`, `function_version`, `service`, `timestamp`, `xray_trace_id`), mais os campos da linha de falha do handler, no modelo da tarefa 3.10. `function_memory_size` é 1024, e `function_request_id` e `xray_trace_id` identificam o teste e a janela. O `timestamp` do evento é o horário do envio. Todas as linhas estão em "Linhas de teste usadas".

Cada janela começou com os três alarmes em OK, conferidos por `describe-alarms` no mesmo comando que gravou as linhas.

| Janela | Linhas, por grupo | Ponto na métrica | ALARM | Volta a OK | Publicações no tópico de alarmes |
|---|---|---|---|---|---|
| A, 19:45:44 a 19:45:47 | 1 linha da aplicação com `"alarme":"FALHA_FUNCAO"`: `Falha ao registrar avaliação`, `Falha ao publicar aviso`, `Falha ao gerar relatório` | 1,0 às 19:45 nas três | notificação 19:46:10, relatório 19:46:24, registro 19:46:29, motivo `1 out of the last 5 datapoints [1.0 (06/11/26 19:45:00)]` | 19:56:10, 19:56:24 e 19:56:29 | 3 às 19:46, ações `Succeeded` |
| B, 19:59:34 a 19:59:37 | `<timestamp> <id> Task timed out after N seconds` e `REPORT RequestId: <id>` com os campos separados por tabulação e `Status: timeout`, com o `Timeout` de cada função (10, 30 e 120 s) | 2,0 às 19:59 nas três, uma por linha | 20:00:10, 20:00:24 e 20:00:29, motivo `[2.0 (06/11/26 19:59:00)]` | 20:10:10, 20:10:24 e 20:10:29 | 3 às 20:00, ações `Succeeded` |
| C, 20:28:16 a 20:28:20 | Linhas reais sem falha reproduzidas: `Entrada rejeitada` em INFO, `Aviso publicado` em INFO, `Data de referência rejeitada` em ERROR sem `alarme`, e `REPORT` com restauração. Junto, de verdade: 5 requisições 400 pela API, `{"dataReferencia":"2026-02-30"}` no `live` do relatório e o bloco M3, com sucesso nas três funções | nenhum ponto nas três métricas de 20:11 a 20:40 | nenhuma transição de 20:10:29 a 21:02:10 | | nenhuma |
| A2, 21:12:51 e 21:12:52 | Repetição da janela A só no registro e no relatório, com o acento correto no campo `message` (observação L) | 1,0 às 21:12 nas duas | relatório 21:13:24, registro 21:13:29 | 21:23:24 e 21:23:29 | 2 às 21:13 |

**Isolamento de cada formato com `test-metric-filter`**, com o padrão implantado, sobre 32 linhas lidas em UTF-8: casaram as 9 linhas de falha (as 3 linhas da aplicação com o marcador, as 3 `Task timed out` e os 3 `REPORT` com `Status: timeout`), cada uma sozinha. Não casaram as 23 demais: as linhas da janela C, `RESTORE_REPORT` e `REPORT` reais das três funções, `Avaliação registrada`, `Aviso publicado`, `Relatório gravado` e `Relatório gerado` reais, os dois EMF reais, `Cliente aquecido` real e uma linha `Falha no aquecimento` no formato do handler. As mensagens devolvidas pelo serviço são idênticas às enviadas.

**Janela C, invocações reais:**

| Invocação | Resultado | Log |
|---|---|---|
| `{"descricao":"qa-observabilidade","nota":11}` | 400, `ENTRADA_INVALIDA`, `campos` `["nota"]` | `Entrada rejeitada` em INFO |
| nota `-1` | 400, `["nota"]` | idem |
| `descricao` vazia | 400, `["descricao"]` | idem |
| corpo malformado | 400, `CORPO_INVALIDO` | `Corpo rejeitado` em INFO |
| sem `nota` | 400, `["nota"]` | `Entrada rejeitada` em INFO |
| relatório com `2026-02-30` | `FunctionError` `Unhandled`, `DataReferenciaInvalidaException` | `Data de referência rejeitada` em ERROR, sem `alarme`, mais a linha de exceção do runtime, sem o marcador |

`EntradaRejeitada` com 5 e `DataReferenciaRejeitada` com 1 às 20:28, no namespace `FeedbackServerless/dev`. As três métricas de falha sem nenhum ponto.

**Invocações com sucesso sem WARN nem ERROR.** Nas 9 invocações com sucesso dos blocos M1, M2 e M3, os grupos de log trazem só `RESTORE_START`, `RESTORE_REPORT`, `START`, linhas INFO (`Avaliação registrada`, `Aviso publicado`, `Relatório gravado`, `Relatório gerado`), o EMF, `END` e `REPORT`. Nenhuma linha em WARN ou ERROR, e nenhum `No metrics were emitted` na notificação, inclusive com o Powertools real.

### 6.11. Item descartado por campo ausente: ok

Às 21:01:03, o QA invocou o alias `live` da notificação com um evento sintético do Stream: um registro `INSERT` com `urgencia` `CRITICA`, `nota` 1, `dataEnvio`, `criadoEm` e `id` `qa-observabilidade-6-11`, **sem `descricao`**, `eventID` `qa-observabilidade-6-11`, no ARN do Stream de dev. O item não existe na tabela.

| Conferência | Resultado |
|---|---|
| Resposta | `{"batchItemFailures":[]}`, sem `FunctionError` |
| Log real | `Item inválido descartado` em ERROR, com `eventID` `qa-observabilidade-6-11`, `campos` `["descricao"]` e `alarme` `FALHA_FUNCAO`; EMF `ItemInvalido` |
| Trace | Restauração e handler, **sem subsegmento SNS** |
| Aviso | Nenhuma publicação no tópico de administradores às 21:01. `ItemInvalido` com 1 às 21:01 |
| Alarme | Ponto 1,0 em `FalhasNotificarCritica` às 21:01. ALARM às 21:02:10, ação `Succeeded`, 1 publicação no tópico de alarmes às 21:02. Volta a OK às 21:12:10 |

É a única falha real provocada no QA, sem alterar a função, e prova com a linha real do runtime o caminho handler, filtro, métrica, alarme e tópico.

### 6.12. Mensagens de alarme sem dado do aluno: ok

`describe-alarm-history` dos três alarmes desde 19:00, todos os tipos: 42 itens, dos quais 12 ações com a mensagem publicada. Cada mensagem traz as variantes `default`, `email` e `sms` que o SNS envia. As chaves do `default` são só `AlarmName`, `AlarmDescription`, `AWSAccountId`, `AlarmConfigurationUpdatedTimestamp`, `NewStateValue`, `NewStateReason`, `StateChangeTime`, `Region`, `AlarmArn`, `OldStateValue`, `ActionExecutedAfterMuteWindow`, `OKActions`, `AlarmActions`, `InsufficientDataActions` e `Trigger`. O motivo traz só o nome do teste ou o valor do ponto, como `[1.0 (06/11/26 19:45:00)]`.

Busca, sem distinguir maiúsculas, nos itens e nos textos `default`, `email` e `sms`: 0 ocorrências de `QA-OBS-202611061941-f15ff8`, `QA-OBS-202611061958-764655`, `QA-OBS-202611062028-1745e9`, `QA-OBS`, `f15ff8`, `764655`, `1745e9`, `Aula`, `áudio`, `ação urgente`, `descricao`, `"nota"`, `nota:` e `Nota`.

Os marcadores também não chegam aos grupos de log: `filter-log-events` desde 19:15 com `QA-OBS`, os três sufixos e `"Aula"` devolveu 0 eventos nos três grupos. `descricao` só aparece como nome de campo em `"campos":["descricao"]`, nas rejeições e no descarte.

O texto do e-mail é a variante `email` da mensagem publicada. O QA leu essa variante nas 12 ações. Na caixa de entrada, o Gabriel confirmou os 12 e-mails sem descrição nem nota (6.17).

### 6.13. Uma hora sem tráfego: ok

Janela de 21:24:00 a 22:24:00 UTC (18:24 a 19:24 em Brasília), começando logo depois da volta a OK da janela A2. A última invocação foi às 21:01:03 (6.11), e a última linha de teste foi ingerida às 21:12:45 (A2). Durante a hora, o QA só fez leituras e validações locais.

| Conferência no período | Resultado |
|---|---|
| `Invocations` das três funções | nenhum ponto |
| Eventos de log nos três grupos (`filter-log-events`) | 0, 0 e 0 |
| Métricas de falha | nenhum ponto |
| `describe-alarm-history`, todos os tipos | 0 itens nos três alarmes |
| Estado dos alarmes | os três em OK durante toda a hora, desde 21:12:10 e 21:23:29 |
| `NumberOfMessagesPublished` do tópico de alarmes | nenhum ponto |
| `NumberOfMessagesPublished` do tópico de administradores | nenhum ponto |

No dia inteiro, o tópico de alarmes recebeu exatamente 12 publicações (3 às 19:42, 3 às 19:46, 3 às 20:00, 1 às 21:02 e 2 às 21:13), uma por transição para ALARM, e `NumberOfNotificationsFailed` ficou em 0.

### 6.14. Dashboard: ok

`list-dashboards` devolve um único dashboard na conta, `feedback-painel-dev`, criado às 19:20:41 (o CloudWatch lista o mesmo em `sa-east-1` e `us-east-1`, porque dashboard é global). `get-dashboard`:

| Widget | Definição implantada | Dados do período, pelas mesmas métricas, períodos e estatísticas (`get-metric-data`) |
|---|---|---|
| Alarmes de falha | `alarm`, com os ARNs dos três alarmes `feedback-falha-*-dev` | Estado atual pelo `describe-alarms`: os três em OK no fim do QA; as transições das janelas estão na 6.7 a 6.11 |
| Invocações | `AWS/Lambda` `Invocations`, `Sum`, 300 s, por `FunctionName` das três funções `-dev` | Registro: 1 às 19:40, 1 às 19:55, 6 às 20:25. Notificação: 1, 1, 1 e 1 às 21:00. Relatório: 1, 1 e 2 às 20:25 |
| Falhas, tratadas e de execução | `FeedbackServerless/dev`, as três métricas de filtro, `Sum`, 60 s | 1 às 19:45 (janela A) e 2 às 19:59 (janela B) nas três; notificação também 1 às 21:01 (6.11); registro e relatório também 1 às 21:12 (A2). Nada às 20:28 (janela C) |
| Duração (ms) | `AWS/Lambda` `Duration`, `Average` e `Maximum`, 300 s | Máximos de 1.362,58, 1.101,32 e 991,09 ms no registro; 770,58, 745,79, 733,64 e 266,79 ms na notificação; 1.927,59, 1.774,79 e 1.686,56 ms no relatório |
| Restaurações do SnapStart | Logs Insights nos três grupos `-dev`, filtrando `^RESTORE_REPORT`, exibindo `@timestamp`, `funcao` e `restauracaoMs` | 13 linhas de 19:41 a 21:01: as 10 restaurações reais do QA (M1, M2 e M3 nas três funções e a 6.11 na notificação, de 518,67 a 943,97 ms) e as 3 linhas sintéticas de 790,00 ms da janela C (observação L). 44 KB lidos |

- **Isolamento:** todas as métricas são `AWS/Lambda` por `FunctionName` com o sufixo `-dev` ou do namespace `FeedbackServerless/dev`. A consulta lê só os três grupos `-dev` e extrai a função com `feedback-(?<funcao>[a-z-]+)-dev$`.
- **Sem dado do aluno:** o corpo não cita `descricao`, `nota` nem corpo de mensagem. A consulta devolve só horário, nome da função e duração. Os marcadores não estão em nenhum grupo de log (6.12), então nenhum widget pode exibi-los.
- **Como a consulta foi rodada:** a API `StartQuery` recusou o texto do widget com `MalformedQueryException`, por causa do prefixo `SOURCE '...' |`, que é sintaxe do widget de dashboard. O QA rodou o mesmo texto a partir do `filter`, com `--log-group-names` dos três grupos. O console não foi aberto pelo QA (observação M).

### 6.15. Medição com 1024 MB e aquecimento: ok, as 15 chamadas abaixo da meta

Memória e aquecimento entraram no mesmo deploy, então a medição mostra o efeito somado dos dois (D13).

Três blocos, cada um com uma avaliação crítica pela API, que invoca o registro e, pelo Stream, a notificação, e uma invocação do `live` do relatório com `{}`, que relê a janela de 2026-10-30 a 2026-11-05 (38 avaliações), regrava `relatorios/2026-11-06.txt` e publica o aviso. Em cada bloco, a chamada medida foi a primeira de um ambiente restaurado: as três funções tinham `RESTORE_START` e `RESTORE_REPORT` logo antes do `START`, e o trace tem o subsegmento `Restore`. Os blocos ficaram a 16 e 31 minutos um do outro, e cada um caiu em ambiente novo. Todos os traces foram amostrados (`Sampled: true`).

**Registro, meta de 4 s no DynamoDB:**

| Bloco | Início (UTC) | Restauração | `PutItem` | Diferença para a meta | Duração | `Memory Size` |
|---|---|---|---|---|---|---|
| M1 | 19:41:10 | 796,37 ms | 169 ms | 3.831 ms abaixo | 1.362,58 ms | 1024 MB |
| M2 | 19:57:53 | 583,97 ms | 258 ms | 3.742 ms abaixo | 1.101,32 ms | 1024 MB |
| M3 | 20:28:13 | 694,94 ms | 200 ms | 3.800 ms abaixo | 991,09 ms | 1024 MB |
| Base 512 MB, sem aquecimento | | 788,80 e 441,53 ms | 4.259 ms | 259 ms abaixo | 8.027 ms | 512 MB |

**Notificação, meta de 2,5 s no SNS:**

| Bloco | Início (UTC) | Restauração | `Publish` | Diferença para a meta | Duração |
|---|---|---|---|---|---|
| M1 | 19:41:12 | 793,52 ms | 174 ms | 2.326 ms abaixo | 770,58 ms |
| M2 | 19:57:54 | 541,29 ms | 166 ms | 2.334 ms abaixo | 745,79 ms |
| M3 | 20:28:14 | 528,92 ms | 200 ms | 2.300 ms abaixo | 733,64 ms |
| Base 512 MB, sem aquecimento | | 774,81 e 521,67 ms | 4.301 ms | 1.801 ms acima | |

**Relatório, meta de 5 s em cada serviço:**

| Bloco | Início (UTC) | Restauração | 1ª `Query` | `PutObject` | `Publish` | Maior diferença para a meta | Duração |
|---|---|---|---|---|---|---|---|
| M1 | 19:41:13 | 943,97 ms | 128 ms | 339 ms | 105 ms | 4.661 ms abaixo | 1.927,59 ms |
| M2 | 19:57:55 | 540,81 ms | 163 ms | 238 ms | 125 ms | 4.762 ms abaixo | 1.774,79 ms |
| M3 | 20:28:15 | 660,50 ms | 147 ms | 260 ms | 167 ms | 4.740 ms abaixo | 1.686,56 ms |
| Base 512 MB, sem aquecimento | | 658,81, 861,08 e 428,47 ms | 2,60, 2,56 e 3,14 s | cerca de 0,7 s | cerca de 0,4 s | | 6.966, 8.031 e 8.357 ms |

As outras 6 `Query` de cada execução levaram de 19 a 56 ms.

- **Primeira chamada:** caiu de 4,3 s para 0,17 a 0,26 s no DynamoDB do registro e no SNS da notificação, e de 2,6 a 3,1 s para 0,13 a 0,16 s na `Query` do relatório. Todas as 15 chamadas medidas ficaram abaixo da meta, com a menor folga de 2.300 ms.
- **Duração da invocação:** caiu de 7 a 8 s para 1,0 a 1,9 s.
- **Restauração:** não mudou de faixa. Foi de 528,92 a 943,97 ms, contra 428,47 a 861,08 ms nas medições de 512 MB das tabelas. A 6.11 trouxe mais uma restauração da notificação, de 518,67 ms, sem chamada ao SNS.
- **Memória usada:** 194 a 221 MB, como com 512 MB.
- **Custo da medição:** cada bloco publicou 2 avisos aos administradores (6 e-mails no total) e regravou o objeto do relatório de 2026-11-06.

### 6.16. Roles, boundary e políticas de deploy: ok

**Roles das funções.** As políticas inline aplicadas (`get-role-policy`) são idênticas aos documentos do `PutRolePolicy` do deploy, às 19:20:03 e 19:20:04 no CloudTrail. A comparação com o `PutRolePolicy` anterior de cada role:

| Role | Documento anterior | Statements removidos | Statements novos | Mantidos |
|---|---|---|---|---|
| `RegistrarAvaliacaoRole` | 05/11, 04:37 UTC | 0 | `AquecerCliente`: `dynamodb:DescribeTable` em `table/feedback-avaliacoes-dev` | 1 |
| `NotificarCriticaRole` | 05/11, 21:31 UTC | 0 | `AquecerCliente`: `sns:GetTopicAttributes` em `feedback-admins-dev` | 4 |
| `GerarRelatorioSemanalRole` | 06/11, 02:40 UTC | 0 | `AquecerClienteSns`: `sns:GetTopicAttributes` em `feedback-admins-dev`; `AquecerClienteS3`: `s3:ListBucket` em `feedback-relatorios-<conta>-dev`, com `s3:ResourceAccount` | 4 |

Toda ação nova é de leitura e aponta para recurso com o sufixo `dev`. Nenhuma grava, altera ou remove item, mensagem ou objeto. As políticas gerenciadas não mudaram: nenhum `AttachRolePolicy` ou `DetachRolePolicy` desde 03:00 UTC de 06/11.

**Boundary.** As quatro roles da stack, inclusive a do agendamento, têm `arn:aws:iam::<conta>:policy/feedback-limite-funcoes-dev`. Nenhum `PutRolePermissionsBoundary` ou `DeleteRolePermissionsBoundary` desde 03:00 UTC. A política está na versão padrão `v2`, de 03:31:26 UTC de 06/11, a da tarefa 2.3, com 2.531 caracteres sem espaços e o statement `TopicosDoAmbiente` com `sns:GetTopicAttributes` e `sns:Publish` em `feedback-*-dev` e `feedback-dev-*`.

**Políticas de deploy de dev.** Nenhuma mudou neste change: `github-deploy-feedback-dev-aplicacao` na `v2` de 05/11, 04:16 UTC, e `github-deploy-feedback-dev-recursos` na `v2` de 04/11, 14:01 UTC. As ações de escrita que este change usa estão restritas ao sufixo `dev`:

| Statement | Ações de escrita | Recursos |
|---|---|---|
| `AlarmesDoAmbiente` (`-recursos`) | `PutMetricAlarm`, `DeleteAlarms`, `TagResource`, `UntagResource` | `alarm:feedback-*-dev`, `alarm:feedback-dev-*` |
| `DashboardsDoAmbiente` (`-recursos`) | `PutDashboard`, `DeleteDashboards`, `TagResource`, `UntagResource` | `dashboard/feedback-*-dev`, `dashboard/feedback-dev-*` |
| `TopicosDoAmbiente` (`-recursos`) | `CreateTopic`, `DeleteTopic`, `SetTopicAttributes`, `Subscribe`, `Unsubscribe` e afins | `feedback-*-dev`, `feedback-*-dev:*`, `feedback-dev-*` |
| `GruposDeLogDoAmbiente` (`-aplicacao`) | `PutMetricFilter`, `DeleteMetricFilter`, `PutRetentionPolicy` e afins | `log-group:/aws/lambda/feedback-*-dev`, com e sem `:*`, e `feedback-dev-*` |

Sem restrição por recurso, só leituras: `cloudwatch:DescribeAlarms`, `logs:DescribeLogGroups` e outras quatro de descrição.

### Contagem de alarmes e dashboards na conta

Pendente do QA local, feita agora:

| Região | Alarmes de métrica | Alarmes compostos | Detalhe |
|---|---|---|---|
| `sa-east-1` | 3 | 0 | os três de dev |
| `us-east-1` | 1 | 0 | `Alarme 10`, `AWS/Billing` `EstimatedCharges`, `Maximum`, 21600 s, sem alta resolução |
| `us-west-2`, `eu-west-1`, `ap-southeast-1` | 0 | 0 | conferência extra |

Dashboards: 1, `feedback-painel-dev`. Hoje a conta usa 4 de 10 métricas de alarme e 1 de 3 dashboards. Com hml e prod, que criam 3 alarmes e 1 dashboard cada, fecha em 10 de 10 e 3 de 3, como a D8 previa.

### 6.18. Este relatório

As linhas de teste estão em "Linhas de teste usadas". **O stream `qa-observabilidade-20261106` não foi apagado:** ele expira com a retenção de 14 dias dos grupos de dev, por volta de 20/11/2026. Até lá, as linhas aparecem para quem ler os grupos e no dashboard, como pontos de falha nos minutos das janelas e como três restaurações de 790,00 ms às 20:28 (observação L).

### 6.19. Computação com 1024 MB: ok, conferido pelo Gabriel no console

O QA trabalha só com o AWS CLI e não tem acesso ao console de cobrança. A API do Cost Explorer ficou fora por regra. O cenário ficou não coberto até o Gabriel abrir a página do nível gratuito.

**Leitura do Gabriel no console do Free Tier, em 07/11/2026,** informada ao QA pelo orquestrador e registrada em 11/11:

| Item do nível gratuito do Lambda | Uso no mês | Limite mensal | Percentual | Previsão do console para o mês |
|---|---|---|---|---|
| Computação | 235 GB-s | 400.000 GB-s | 0,06% | 1.009 GB-s, 0,25% |
| Requisições | 128 | 1.000.000 | 0,01% | |

Com isso, o uso com 1024 MB fica dentro do nível gratuito, com folga de mais de 99%, que é o que o cenário pede.

O QA não viu o console. Os números acima são os informados, e a data da leitura é a do Gabriel. O console mede o mês da conta inteira, inclusive as stacks apagadas antes de 05/11, e atualiza com atraso. Por isso não bate com o indício abaixo, que soma só os grupos de log atuais de dev até 06/11: 48,9 GB-s e 138 invocações. A diferença não muda o resultado, porque as duas contas ficam abaixo de 0,3% dos limites.

Indício do QA, anterior à leitura do console: soma das linhas `REPORT` dos três grupos de dev de 01/11 a 21:25 UTC de 06/11/2026, por Logs Insights. As três funções de dev são as únicas funções Lambda da conta em `sa-east-1`.

| Memória | Invocações | Duração cobrada | GB-s |
|---|---|---|---|
| 512 MB | 122 | 70.994 ms | 35,5 |
| 1024 MB | 16 | 13.414 ms | 13,4 |
| **Total** | **138** | | **48,9** |

- A soma exclui os 6 `REPORT` sintéticos do stream de teste, que não são invocação.
- O `Billed Duration` já inclui a restauração cobrada (D8).
- Cobre só o período desde a criação dos grupos de log atuais: 05/11 04:37 no registro, 05/11 21:31 na notificação e 06/11 02:40 no relatório. Invocações das stacks anteriores não entram.
- Mesmo assim, 48,9 GB-s e 138 requisições ficam na casa de 0,01% dos dois limites.

Limites mensais do nível gratuito do Lambda, pela D8: 1 milhão de requisições e 400.000 GB-s.

## Conferência na release (07/11/2026)

**Validade da evidência:** observação das 03:57 às 05:20 UTC de 07/11/2026 (00:57 às 02:20 em Brasília), com `feedback-dev` (`UPDATE_COMPLETE` desde 06/11 às 19:21) e `feedback-hml` (`CREATE_COMPLETE` às 03:51:37) no ar. O relógio da máquina seguia cerca de 7 s adiantado: a linha da 8.2 tem `timestamp` 04:48:45.377 e `ingestionTime` 04:48:38.717, no mesmo minuto. A fumaça da release em hml, com o registro, o relatório e o isolamento de recursos, está em `openspec/changes/archive/2026-11-11-add-fundacao-projeto/qa.md`, seção "Release 1.0.0 em hml (07/11/2026)". A 8.1 teve uma segunda rodada, das 05:50 às 05:54 UTC (02:50 às 02:54 em Brasília), depois da confirmação da assinatura de alarmes de hml. **Escritas desta seção:** 1 log stream `qa-observabilidade-20261107` no grupo do registro de dev, com 1 linha, e 1 `set-alarm-state` em `feedback-falha-registrar-avaliacao-hml`. Nenhum alarme, dashboard ou filtro criado, nada apagado.

### 8.1. Assinaturas de hml: ok em hml (prod em "Prod, release 1.0.0 (leitura)")

Tarefa do Gabriel. O QA conferiu o estado pela API:

| Tópico | Estado da assinatura `email` | `SubscriptionsConfirmed`, `Pending`, `Deleted` |
|---|---|---|
| `feedback-admins-hml` | confirmada, já na primeira leitura, às 04:07 UTC | 1, 0, 0 |
| `feedback-alarmes-hml` | `PendingConfirmation` em quatro leituras, às 04:07, 04:17, 04:54 e 05:18 UTC. **Confirmada pelo Gabriel em 07/11/2026** pelo e-mail original de confirmação, conferida pelo orquestrador às 05:49:52 e pelo QA às 05:50:12 UTC | 0, 1, 1 até 05:18; 1, 0, 0 a partir de 05:49 |

As duas assinaturas foram criadas às 03:48:52 pela sessão `github-deploy-feedback-hml/deploy-42335524756`, e a mensagem de confirmação do tópico de alarmes foi entregue às 03:48 (`NumberOfNotificationsDelivered` 1, `NumberOfNotificationsFailed` 0). O orquestrador tinha informado as duas como confirmadas antes da primeira rodada, mas só a de administradores estava. Com a confirmação, o QA rodou o `set-alarm-state` da fumaça em um único alarme de hml:

| Conferência | Resultado |
|---|---|
| Linha de base, 05:50:12 | Os seis alarmes de dev e hml em OK |
| Transição | `feedback-falha-registrar-avaliacao-hml` de OK para ALARM às 05:50:37.214, motivo `QA release 1.0.0 set-alarm-state: teste do caminho do alarme ate o e-mail` |
| Ação | `Successfully executed action arn:aws:sns:sa-east-1:<conta>:feedback-alarmes-hml`, `Succeeded`, às 05:50:37.247 |
| Tópico de alarmes de hml | `NumberOfMessagesPublished` 1, `NumberOfNotificationsDelivered` 1 e `NumberOfNotificationsFailed` 0 às 05:50 |
| Outros tópicos | Nenhuma publicação no tópico de alarmes de dev nem no de administradores de hml entre 05:30 e 05:53 |
| Mensagem | Variantes `default`, `email` e `sms`, só com dados do alarme. `AlarmActions` só com `feedback-alarmes-hml`. Nenhuma ocorrência dos marcadores da fumaça, de `Aula`, `descricao` ou `"nota"` |
| Volta a OK | Sozinha às 05:50:41.426, 4 s depois, `no datapoints were received for 5 periods`. Em dev, de 60 a 80 s |

Isso prova, em hml, a política `PublicacaoPelosAlarmesDoAmbiente` com `aws:SourceArn` em `alarm:feedback-*-hml`, a ação do alarme e a entrega à assinatura confirmada. Que o e-mail chegou na caixa de entrada, só o Gabriel confere. **A 8.1 fica completa em hml.** Falta prod, que ainda não existe. **Atualização de 11/11/2026:** prod foi implantado em 08/11, e as duas assinaturas de prod estão confirmadas. Ver "Prod, release 1.0.0 (leitura)".

### 8.2. Falha em outro ambiente: ok

**Linha de base**, às 04:47:47, antes da linha: os seis alarmes em OK. `FeedbackServerless/dev` e `FeedbackServerless/hml` sem nenhum ponto nas três métricas de falha desde 03:00. O histórico dos alarmes de hml só tinha a criação, às 03:48:54, e a passagem de `INSUFFICIENT_DATA` para OK, entre 03:49:46 e 03:50:16. O corpo de `feedback-painel-hml`, lido com `get-dashboard`, só cita os três alarmes `-hml`, `AWS/Lambda` por `FunctionName` `-hml`, `FeedbackServerless/hml` e os três grupos `-hml`, sem nenhum `-dev` ou `/dev`.

**Linha de teste.** Stream `qa-observabilidade-20261107`, criado pelo QA às 04:48 no grupo `/aws/lambda/feedback-registrar-avaliacao-dev`. Uma linha da aplicação no formato da janela A2 (`Falha ao registrar avaliação`, `"alarme":"FALHA_FUNCAO"`, `function_version` 7), com `function_request_id` e `xray_trace_id` `qa-observabilidade-8-2-registrar-avaliacao` e o horário do envio, gravada com `AWS_CLI_FILE_ENCODING=UTF-8`. A linha lida de volta do grupo é idêntica à enviada, com o acento correto. O texto está em "Linhas de teste usadas".

| Conferência | dev | hml |
|---|---|---|
| Métrica `FalhasRegistrarAvaliacao` | 1,0 às 04:48 em `FeedbackServerless/dev` | nenhum ponto em `FeedbackServerless/hml`, de 03:45 a 05:30 |
| Alarme do registro | `feedback-falha-registrar-avaliacao-dev`: ALARM às 04:49:29, motivo `1 out of the last 5 datapoints [1.0 (07/11/26 04:48:00)]`, ação `Successfully executed action arn:aws:sns:sa-east-1:<conta>:feedback-alarmes-dev`. OK às 04:59:29, `no datapoints were received for 5 periods` | `feedback-falha-registrar-avaliacao-hml` em OK do início ao fim, `StateUpdatedTimestamp` ainda 03:50:06. Nenhum item de histórico depois da criação nos três alarmes de hml |
| Outros alarmes | notificação e relatório de dev em OK, sem transição | notificação e relatório de hml em OK, sem transição |
| Tópico de alarmes | `feedback-alarmes-dev`: 1 publicada, 1 entregue e 0 falhas às 04:49 | `feedback-alarmes-hml`: nenhuma publicação |
| Tópico de administradores | nenhuma publicação | nenhuma publicação depois do relatório das 04:27 |
| Dashboard | | Widget de falhas, pelas mesmas três métricas, `Sum` e 60 s (`get-metric-data`): nenhum ponto de 03:45 a 05:30. Widget de invocações do registro: só as 2 requisições da fumaça, às 04:15 no período de 5 min. Consulta de restaurações, rodada a partir do `filter` nos três grupos `-hml`: 3 linhas, as restaurações reais da fumaça (617,70, 785,48 e 710,17 ms), sem nada de dev |

O alarme de dev voltou a OK 10 minutos depois de entrar em ALARM, como na observação K. O dashboard de hml foi lido pela API, sem console (observação M).

## Prod, release 1.0.0 (leitura)

**Validade da evidência:**
- **Deploy:** execução 42438770120 do workflow Deploy, evento `push` em `main`, commit `6eee512`, `success`, das 02:29:37 às 03:02:07 UTC de 08/11/2026. A stack foi de `REVIEW_IN_PROGRESS` às 02:58:15 a `CREATE_COMPLETE` às 03:01:58 UTC, com 118 eventos e nenhum `FAILED`. Parâmetro `Stage` `prod` e tags `projeto` `feedback` e `ambiente` `prod`.
- **Observação:** a leitura de 08/11 foi repetida e agora registrada. As leituras foram de 21:44 UTC de 10/11 a 02:55 UTC de 11/11, com uma pausa no meio. Por isso, funções, retenção, alarmes, agendamento, tabela e stack foram lidos de novo em bloco único, das 02:55:01 às 02:55:11 UTC de 11/11, com o mesmo resultado.
- **Escritas na conta:** nenhuma. Só `describe-*`, `get-*`, `list-*`, `get-metric-statistics`, `filter-log-events`, `head-object`, `get-object` e `scan --select COUNT`, com o perfil `tc4-admin`. Nenhuma invocação, nenhuma chamada à API, nenhum `set-alarm-state`, nenhuma publicação, nenhuma linha de log.
- **Tráfego em prod desde a criação:** só a execução agendada do relatório às 11:00 UTC de 09/11, registrada em `openspec/changes/archive/2026-11-11-add-relatorio-semanal/qa.md`, seção "Execução agendada real, 09/11/2026". Registro e notificação de prod sem nenhum ponto em `Invocations` de 08/11 a 10/11.

### Funções

`get-function-configuration` no alias `live`:

| Função | Versão do `live` | `State` | `LastUpdateStatus` | `OptimizationStatus` | `MemorySize` | `Timeout` | `POWERTOOLS_METRICS_NAMESPACE` | Boundary da role |
|---|---|---|---|---|---|---|---|---|
| `feedback-registrar-avaliacao-prod` | 1 | `Active` | `Successful` | `On` | 1024 | 10 | `FeedbackServerless/prod` | `feedback-limite-funcoes-prod` |
| `feedback-notificar-critica-prod` | 1 | `Active` | `Successful` | `On` | 1024 | 30 | `FeedbackServerless/prod` | `feedback-limite-funcoes-prod` |
| `feedback-gerar-relatorio-semanal-prod` | 1 | `Active` | `Successful` | `On` | 1024 | 120 | `FeedbackServerless/prod` | `feedback-limite-funcoes-prod` |

As três com `java25`, `TracingConfig` `Active`, `AMBIENTE` `prod`, `ApplyOn: PublishedVersions` e alias sem `RoutingConfig`. Cada função tem só `$LATEST`, com `OptimizationStatus: Off`, e a versão 1. A role do agendamento também tem o boundary `feedback-limite-funcoes-prod`.

### 8.3. Retenção em prod: ok

`describe-log-groups`:

| Grupo | `retentionInDays` | Criação (UTC) |
|---|---|---|
| `/aws/lambda/feedback-registrar-avaliacao-prod` | 30 | 02:58:32 de 08/11 |
| `/aws/lambda/feedback-notificar-critica-prod` | 30 | 02:58:32 de 08/11 |
| `/aws/lambda/feedback-gerar-relatorio-semanal-prod` | 30 | 02:58:32 de 08/11 |

Dev e hml seguem com 14 dias (6.5 e `add-fundacao-projeto/qa.md`), o que confirma, nos três ambientes implantados, o `!If [EhProd, 30, 14]` do template.

### Alarmes de prod

| Alarme | Estado | Métrica | `AlarmActions` | `OKActions`, `InsufficientDataActions` | Histórico |
|---|---|---|---|---|---|
| `feedback-falha-registrar-avaliacao-prod` | OK desde 03:00:27 UTC de 08/11 | `FeedbackServerless/prod` `FalhasRegistrarAvaliacao` | só `feedback-alarmes-prod` | vazias | só a criação, às 02:58:34, e a passagem de `INSUFFICIENT_DATA` para OK |
| `feedback-falha-notificar-critica-prod` | OK desde 02:59:57 | `FalhasNotificarCritica` | só `feedback-alarmes-prod` | vazias | idem |
| `feedback-falha-gerar-relatorio-semanal-prod` | OK desde 02:59:36 | `FalhasGerarRelatorioSemanal` | só `feedback-alarmes-prod` | vazias | idem |

Os três com `Sum`, `Period` 60, 5 períodos, 1 ponto, limiar 1, `notBreaching` e `ActionsEnabled`, iguais aos de dev e hml. Nenhum aponta para o tópico de administradores. O tópico de alarmes de prod não tem nenhuma publicação de 08/11 a 10/11. Na conta há 9 alarmes `feedback-*`, 3 por ambiente.

### Agendamento de prod

`feedback-relatorio-semanal-prod`: `ENABLED`, `cron(0 8 ? * MON *)`, `America/Sao_Paulo`, grupo `default`, `FlexibleTimeWindow` `OFF`, alvo `feedback-gerar-relatorio-semanal-prod:live`, `Input` `{}`, `RetryPolicy` 3 e 3600 s, `ActionAfterCompletion` `NONE`, criado às 03:01:55 UTC de 08/11. Disparou às 11:00 UTC de 09/11.

### 8.1. Assinaturas de prod: confirmadas

| Tópico | Assinatura | `SubscriptionsConfirmed`, `Pending`, `Deleted` | `KmsMasterKeyId` |
|---|---|---|---|
| `feedback-admins-prod` | 1 `email`, com ARN de assinatura, ou seja, confirmada | 1, 0, 0 | `alias/aws/sns` |
| `feedback-alarmes-prod` | 1 `email`, confirmada | 1, 0, 0 | nenhum |

O endereço não foi copiado. O tópico de administradores de prod já entregou o aviso do relatório de 09/11 (1 publicada, 1 entregue, 0 falhas), o que confirma a assinatura em uso. No tópico de alarmes de prod, a entrega não foi exercitada.

### Tabela e bucket de prod

| Item | Observado |
|---|---|
| `feedback-avaliacoes-prod`, `scan --select COUNT` | `Count` 0, `ScannedCount` 0 |
| Tabela | `ACTIVE`, `SSEType` `KMS`, Stream `NEW_IMAGE`, recuperação pontual `ENABLED`, como esperado em prod |
| Bucket de prod | Um único objeto, `relatorios/2026-11-09.txt`, 351 bytes, `LastModified` 11:00:14 UTC de 09/11, da execução agendada, com a semana vazia |

### O que a leitura de prod acrescenta à matriz

- `Retenção em prod`: não coberto para ok (executado).
- Evidência de prod, sem mudar status: `Assinatura criada no deploy`, `Destino das ações de alarme`, `Memória na stack`, `Versão publicada com aquecimento` e `Boundary mantido`.
- Não exercitado em prod, por regra desta rodada: o caminho do alarme até o e-mail e qualquer falha. `Alarme em ALARM notifica` foi fechado pela caixa de entrada de dev (6.17), confirmada pelo Gabriel.

## E-mails para o Gabriel conferir (6.17)

**Confirmado pelo Gabriel:** os 12 e-mails da tabela abaixo chegaram, sem descrição nem nota. A confirmação foi informada ao QA pelo orquestrador e registrada em 11/11/2026. Ela não cita os 2 e-mails extras da 8.2 e da 8.1, descritos no fim desta seção, que não fazem parte da 6.17.

Assinatura do tópico `feedback-alarmes-dev`. Devem ter chegado **exatamente 12 e-mails de alarme**, todos com o assunto `ALARM: "feedback-falha-<função>-dev" in South America (Sao Paulo)`, e nenhum de volta a OK, porque os alarmes não têm `OKActions`.

| # | UTC | Brasília | Alarme | Origem | Motivo no e-mail |
|---|---|---|---|---|---|
| 1 | 19:42:09 | 16:42:09 | `feedback-falha-registrar-avaliacao-dev` | 6.7 | `QA 6.7 set-alarm-state: teste do caminho do alarme ate o e-mail` |
| 2 | 19:42:10 | 16:42:10 | `feedback-falha-notificar-critica-dev` | 6.7 | idem |
| 3 | 19:42:11 | 16:42:11 | `feedback-falha-gerar-relatorio-semanal-dev` | 6.7 | idem |
| 4 | 19:46:10 | 16:46:10 | `feedback-falha-notificar-critica-dev` | janela A | `Threshold Crossed: 1 out of the last 5 datapoints [1.0 (06/11/26 19:45:00)] ...` |
| 5 | 19:46:24 | 16:46:24 | `feedback-falha-gerar-relatorio-semanal-dev` | janela A | idem |
| 6 | 19:46:29 | 16:46:29 | `feedback-falha-registrar-avaliacao-dev` | janela A | idem |
| 7 | 20:00:10 | 17:00:10 | `feedback-falha-notificar-critica-dev` | janela B | `... [2.0 (06/11/26 19:59:00)] ...` |
| 8 | 20:00:24 | 17:00:24 | `feedback-falha-gerar-relatorio-semanal-dev` | janela B | idem |
| 9 | 20:00:29 | 17:00:29 | `feedback-falha-registrar-avaliacao-dev` | janela B | idem |
| 10 | 21:02:10 | 18:02:10 | `feedback-falha-notificar-critica-dev` | 6.11 | `... [1.0 (06/11/26 21:01:00)] ...` |
| 11 | 21:13:24 | 18:13:24 | `feedback-falha-gerar-relatorio-semanal-dev` | janela A2 | `... [1.0 (06/11/26 21:12:00)] ...` |
| 12 | 21:13:29 | 18:13:29 | `feedback-falha-registrar-avaliacao-dev` | janela A2 | idem |

Em cada e-mail, conferir:
- Só dados do alarme: nome, descrição do alarme (`Falha da função ..., tratada ou de execução, inclusive tempo esgotado.`), mudança de estado, motivo, horário, conta, ARN, limiar e métrica.
- **Nenhuma descrição de avaliação:** nada com `QA-OBS`, `Aula`, `áudio` ou `ação urgente`.
- **Nenhuma nota.**
- Os acentos da descrição do alarme aparecem corretos.

No mesmo período chegam também, pelo tópico de administradores, 3 e-mails `Avaliação crítica registrada` (19:41, 19:57 e 20:28 UTC) e 3 `Relatório semanal 2026-11-06` (19:41, 19:57 e 20:28 UTC). Esses são da 6.15 e não fazem parte da 6.17.

Fora da lista da 6.17, a tarefa 8.2 gerou mais 1 e-mail de alarme na mesma assinatura de dev: `ALARM: "feedback-falha-registrar-avaliacao-dev" in South America (Sao Paulo)`, em 07/11/2026 às 04:49:29 UTC (01:49:29 em Brasília), com o motivo `... [1.0 (07/11/26 04:48:00)] ...`. A 8.1 gerou 1 e-mail de alarme na assinatura de hml: `ALARM: "feedback-falha-registrar-avaliacao-hml" in South America (Sao Paulo)`, em 07/11/2026 às 05:50:37 UTC (02:50:37 em Brasília), com o motivo `QA release 1.0.0 set-alarm-state: teste do caminho do alarme ate o e-mail`. Nenhum dos dois traz descrição ou nota.

O e-mail de alarme traz o ID da conta e o ARN, e o rodapé do SNS traz o link de cancelamento com o e-mail do assinante: não copiar para arquivo versionado e desfocar no vídeo.

## Linhas de teste usadas

Stream `qa-observabilidade-20261106` em cada grupo de dev. `function_arn` com a conta mascarada. `<TAB>` marca a tabulação.

**`/aws/lambda/feedback-registrar-avaliacao-dev`**, 7 linhas:

```
19:45:44.782 (A)  {"level":"ERROR","message":"Falha ao registrar avaliação","cold_start":false,"function_arn":"arn:aws:lambda:sa-east-1:<conta>:function:feedback-registrar-avaliacao-dev:live","function_memory_size":1024,"function_name":"feedback-registrar-avaliacao-dev","function_request_id":"qa-observabilidade-A-registrar-avaliacao","function_version":"7","service":"registrar-avaliacao","timestamp":"2026-11-06T19:45:44.782Z","xray_trace_id":"qa-observabilidade-A-registrar-avaliacao","classe":"software.amazon.awssdk.services.dynamodb.model.DynamoDbException","codigoErro":"qa-observabilidade","idRequisicao":"qa-observabilidade","alarme":"FALHA_FUNCAO"}
19:59:34.554 (B)  2026-11-06T19:59:34.554Z qa-observabilidade-B-registrar-avaliacao Task timed out after 10.01 seconds
19:59:34.554 (B)  REPORT RequestId: qa-observabilidade-B-registrar-avaliacao<TAB>Duration: 10000.00 ms<TAB>Billed Duration: 10000 ms<TAB>Memory Size: 1024 MB<TAB>Max Memory Used: 210 MB<TAB>Status: timeout<TAB>
20:28:16.110 (C)  {"level":"INFO","message":"Entrada rejeitada","cold_start":false,"function_arn":"arn:aws:lambda:sa-east-1:<conta>:function:feedback-registrar-avaliacao-dev:live","function_memory_size":1024,"function_name":"feedback-registrar-avaliacao-dev","function_request_id":"qa-observabilidade-C-registrar-avaliacao","function_version":"7","service":"registrar-avaliacao","timestamp":"2026-11-06T20:28:16.110Z","xray_trace_id":"qa-observabilidade-C-registrar-avaliacao","campos":["nota"]}
20:28:16.110 (C)  RESTORE_REPORT Restore Duration: 790.00 ms
20:28:16.110 (C)  REPORT RequestId: qa-observabilidade-C-registrar-avaliacao<TAB>Duration: 812.40 ms<TAB>Billed Duration: 1036 ms<TAB>Memory Size: 1024 MB<TAB>Max Memory Used: 210 MB<TAB>Restore Duration: 790.00 ms<TAB>Billed Restore Duration: 224 ms<TAB>
21:12:51.359 (A2) igual à linha A, com "function_request_id" e "xray_trace_id" iguais a "qa-observabilidade-A2-registrar-avaliacao" e o timestamp do envio
```

**`/aws/lambda/feedback-notificar-critica-dev`**, 6 linhas:

```
19:45:46.056 (A)  {"level":"ERROR","message":"Falha ao publicar aviso","cold_start":false,"function_arn":"arn:aws:lambda:sa-east-1:<conta>:function:feedback-notificar-critica-dev:live","function_memory_size":1024,"function_name":"feedback-notificar-critica-dev","function_request_id":"qa-observabilidade-A-notificar-critica","function_version":"4","service":"notificar-critica","timestamp":"2026-11-06T19:45:46.056Z","xray_trace_id":"qa-observabilidade-A-notificar-critica","id":"qa-observabilidade","eventID":"qa-observabilidade","sequencia":"qa-observabilidade","classe":"software.amazon.awssdk.services.sns.model.SnsException","codigoErro":"qa-observabilidade","idRequisicao":"qa-observabilidade","alarme":"FALHA_FUNCAO"}
19:59:35.815 (B)  2026-11-06T19:59:35.815Z qa-observabilidade-B-notificar-critica Task timed out after 30.03 seconds
19:59:35.815 (B)  REPORT RequestId: qa-observabilidade-B-notificar-critica<TAB>Duration: 30000.00 ms<TAB>Billed Duration: 30000 ms<TAB>Memory Size: 1024 MB<TAB>Max Memory Used: 210 MB<TAB>Status: timeout<TAB>
20:28:17.522 (C)  {"level":"INFO","message":"Aviso publicado","cold_start":false,"function_arn":"arn:aws:lambda:sa-east-1:<conta>:function:feedback-notificar-critica-dev:live","function_memory_size":1024,"function_name":"feedback-notificar-critica-dev","function_request_id":"qa-observabilidade-C-notificar-critica","function_version":"4","service":"notificar-critica","timestamp":"2026-11-06T20:28:17.522Z","xray_trace_id":"qa-observabilidade-C-notificar-critica","id":"qa-observabilidade","eventID":"qa-observabilidade","messageId":"qa-observabilidade"}
20:28:17.522 (C)  RESTORE_REPORT Restore Duration: 790.00 ms
20:28:17.522 (C)  REPORT RequestId: qa-observabilidade-C-notificar-critica<TAB>Duration: 812.40 ms<TAB>Billed Duration: 1036 ms<TAB>Memory Size: 1024 MB<TAB>Max Memory Used: 210 MB<TAB>Restore Duration: 790.00 ms<TAB>Billed Restore Duration: 224 ms<TAB>
```

**`/aws/lambda/feedback-gerar-relatorio-semanal-dev`**, 7 linhas:

```
19:45:47.344 (A)  {"level":"ERROR","message":"Falha ao gerar relatório","cold_start":false,"function_arn":"arn:aws:lambda:sa-east-1:<conta>:function:feedback-gerar-relatorio-semanal-dev:live","function_memory_size":1024,"function_name":"feedback-gerar-relatorio-semanal-dev","function_request_id":"qa-observabilidade-A-gerar-relatorio-semanal","function_version":"2","service":"gerar-relatorio-semanal","timestamp":"2026-11-06T19:45:47.344Z","xray_trace_id":"qa-observabilidade-A-gerar-relatorio-semanal","classe":"software.amazon.awssdk.services.dynamodb.model.DynamoDbException","codigoErro":"qa-observabilidade","idRequisicao":"qa-observabilidade","alarme":"FALHA_FUNCAO"}
19:59:37.060 (B)  2026-11-06T19:59:37.060Z qa-observabilidade-B-gerar-relatorio-semanal Task timed out after 120.12 seconds
19:59:37.060 (B)  REPORT RequestId: qa-observabilidade-B-gerar-relatorio-semanal<TAB>Duration: 120000.00 ms<TAB>Billed Duration: 120000 ms<TAB>Memory Size: 1024 MB<TAB>Max Memory Used: 210 MB<TAB>Status: timeout<TAB>
20:28:18.790 (C)  {"level":"ERROR","message":"Data de referência rejeitada","cold_start":false,"function_arn":"arn:aws:lambda:sa-east-1:<conta>:function:feedback-gerar-relatorio-semanal-dev:live","function_memory_size":1024,"function_name":"feedback-gerar-relatorio-semanal-dev","function_request_id":"qa-observabilidade-C-gerar-relatorio-semanal","function_version":"2","service":"gerar-relatorio-semanal","timestamp":"2026-11-06T20:28:18.790Z","xray_trace_id":"qa-observabilidade-C-gerar-relatorio-semanal","classe":"br.com.fiap.feedback.dominio.DataReferenciaInvalidaException"}
20:28:18.790 (C)  RESTORE_REPORT Restore Duration: 790.00 ms
20:28:18.790 (C)  REPORT RequestId: qa-observabilidade-C-gerar-relatorio-semanal<TAB>Duration: 812.40 ms<TAB>Billed Duration: 1036 ms<TAB>Memory Size: 1024 MB<TAB>Max Memory Used: 210 MB<TAB>Restore Duration: 790.00 ms<TAB>Billed Restore Duration: 224 ms<TAB>
21:12:52.742 (A2) igual à linha A, com "function_request_id" e "xray_trace_id" iguais a "qa-observabilidade-A2-gerar-relatorio-semanal" e o timestamp do envio
```

Os blocos acima mostram o texto como foi enviado. No grupo, a linha A do registro e as linhas A e C do relatório ficaram com o acento do campo `message` em dupla codificação, como `avaliaÃ§Ã£o`. As linhas A2 ficaram corretas (observação L).

**Tarefa 8.2, 07/11/2026.** Stream `qa-observabilidade-20261107`, só em `/aws/lambda/feedback-registrar-avaliacao-dev`, 1 linha, com o acento correto no grupo:

```
04:48:45.377 (8.2)  {"level":"ERROR","message":"Falha ao registrar avaliação","cold_start":false,"function_arn":"arn:aws:lambda:sa-east-1:<conta>:function:feedback-registrar-avaliacao-dev:live","function_memory_size":1024,"function_name":"feedback-registrar-avaliacao-dev","function_request_id":"qa-observabilidade-8-2-registrar-avaliacao","function_version":"7","service":"registrar-avaliacao","timestamp":"2026-11-07T04:48:45.377Z","xray_trace_id":"qa-observabilidade-8-2-registrar-avaliacao","classe":"software.amazon.awssdk.services.dynamodb.model.DynamoDbException","codigoErro":"qa-observabilidade","idRequisicao":"qa-observabilidade","alarme":"FALHA_FUNCAO"}
```

O stream expira com a retenção de 14 dias, por volta de 21/11/2026.

## Verificações locais (tarefa 5.3)

JDK definido só no comando: Microsoft OpenJDK 25.0.5.0.2. SAM CLI 1.171.0. A comprovação em Corretto 25 veio do CI do PR #13.

| Verificação | Resultado |
|---|---|
| `mvn -B install` | `BUILD SUCCESS` em 39 s. `dominio`: 171 testes. `registrar-avaliacao`: 140. `notificar-critica`: 78. `gerar-relatorio-semanal`: 95. Todos com 0 falhas, 0 erros e 0 ignorados. Os três `PrimingSnapStartTest` passaram, 3 testes cada |
| `sam build` | `Build Succeeded` em 64 s, com `Building codeuri` para `RegistrarAvaliacao`, `NotificarCritica` e `GerarRelatorioSemanal` |
| `lib/` das três funções em `.aws-sam/build` | `crac-1.5.0`, `aspectjrt-1.9.27`, `url-connection-client-2.57.8` e o `dominio` presentes nas três. 0 ocorrências de `apache-client`, `apache5-client`, `netty-nio-client`, `aspectjtools` e `aspectjweaver`. `httpclient` 4.5.14 vem do X-Ray, como antes. `Aquecimento.class` presente nas três |
| `sam validate --lint` e `sam validate --lint --region sa-east-1` | `template.yaml is a valid SAM Template` nos dois |
| `sam validate --lint --region sa-east-1 -t infra/bootstrap/github-oidc.yaml` | `github-oidc.yaml is a valid SAM Template` |
| `openspec validate add-observabilidade-seguranca --strict` | `Change 'add-observabilidade-seguranca' is valid` |
| Domínio sem SDK da AWS | Imports de `functions/dominio/src/main` só de `java.math`, `java.time` e `java.util`. 0 arquivos de `functions/dominio/src` com `software.amazon`, `com.amazonaws`, `org.crac`, `org.slf4j` ou `com.fasterxml`. `dependency:tree` do `dominio`: só JUnit e AssertJ, em escopo `test`. `functions/dominio` sem nenhuma alteração neste change |
| `sam build` com função sem código | Numa cópia no scratchpad, sem `functions/notificar-critica/src`: `Build Failed`, `JavaMavenWorkflow:MavenCopyArtifacts - Required target/classes directory was not produced from 'mvn package'`, saída 1 |

**Repetição sobre `e15f842`**, no worktree do QA de dev, com Microsoft OpenJDK 25.0.5.0.2: `mvn -B verify` com saída 0 e as mesmas contagens, 171, 140, 78 e 95 testes, 0 falhas, 0 erros e 0 ignorados. `openspec validate add-observabilidade-seguranca --strict` e `openspec validate add-relatorio-semanal --strict`: válidos. O template não mudou nesta rodada.

**CI do PR #13** (execução 42276798571, job `ci`, `success`, de 19:07:11 a 19:08:45 UTC): `Build, testes e install no repositório Maven local` em 28 s, `Empacotar as funções` em 45 s com `Building codeuri` para `RegistrarAvaliacao`, `NotificarCritica` e `GerarRelatorioSemanal` e `Build Succeeded`, e as duas validações do template. O job inteiro levou 1 min 34 s.

## Inspeção do template (tarefa 5.2)

Feita pela sonda e por leitura, sobre o `template.yaml` do working tree da rodada local. A sonda é um programa local, fora do repositório, que lê o template preservando as funções intrínsecas e confere 131 itens. Para provar que ela acusa erro, 9 mutações foram aplicadas a cópias do template (Period 10, `breaching`, KMS no tópico de alarmes, ação no tópico de administradores, memória 512, retenção fixa, uma ação a menos no `Deny`, conta literal e `PutItem` no aquecimento): as 9 foram acusadas.

| Item | Resultado |
|---|---|
| Alarmes | 3 `AWS::CloudWatch::Alarm`, cada um com uma métrica direta, sem `Metrics`, sem dimensão, sem `ExtendedStatistic`: 3 métricas de alarme por stack |
| `Period`, estatística e limiar | `Period` 60, `Sum`, `Threshold` 1, `GreaterThanOrEqualToThreshold`, `EvaluationPeriods` 5 e `DatapointsToAlarm` 1 nos três |
| `TreatMissingData` | `notBreaching` nos três |
| Ações | `AlarmActions` só com `!Ref TopicoAlarmes`. Sem `OKActions` nem `InsufficientDataActions` |
| Filtros | 3 `AWS::Logs::MetricFilter`, um por grupo, com o padrão `?FALHA_FUNCAO ?"Task timed out" ?"Status: timeout"`, `MetricValue` `"1"`, `Unit` `Count`, sem `DefaultValue` nem dimensão, no namespace `FeedbackServerless/${Stage}` |
| Dashboard | 1 `AWS::CloudWatch::Dashboard`, `feedback-painel-${Stage}`, com 5 widgets e 12 métricas. Com `Stage=hml`, o corpo vira JSON válido e todas as métricas são `AWS/Lambda` por `FunctionName` da stack ou `FeedbackServerless/hml`; a consulta lê só os três grupos `-hml` e só `RESTORE_REPORT` |
| Nomes | `feedback-falha-<função>-${Stage}`, `feedback-painel-${Stage}`, `feedback-alarmes-${Stage}` e `feedback-falhas-<função>-${Stage}`, no padrão do bootstrap |
| Tópico de alarmes | Sem `KmsMasterKeyId`. O tópico de administradores continua com `alias/aws/sns` |
| Política do tópico | `AWS::SNS::TopicInlinePolicy` como a D2 |
| `MemorySize` | 1024 no `Globals`. Nenhuma função sobrescreve |
| Namespace | `POWERTOOLS_METRICS_NAMESPACE: !Sub FeedbackServerless/${Stage}` |
| Retenção | `!If [EhProd, 30, 14]` nos três grupos |
| Statements de aquecimento | Uma ação de leitura por statement, em recursos da stack |
| ID de conta literal | Nenhum |
| Bootstrap | O `git diff` só troca `PublicarNoTopico` por `TopicosDoAmbiente`. Nada muda nas políticas `-recursos` e `-aplicacao` |
| `ci.yml` | `mvn -B install`, `setup-sam`, `sam build` e as duas validações. `permissions: contents: read`, sem `id-token` e sem `configure-aws-credentials`. Job `ci` |

A stack implantada confirmou cada item na 6.4 a 6.7, 6.14 e 6.16.

## Matriz: observabilidade

### Tópico de alarmes por ambiente

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Tópico criado no deploy | ok (executado) | `feedback-alarmes-dev`, criado às 19:20:02 pelo deploy, diferente de `feedback-admins-dev` (6.6) | INSP |
| Assinatura criada no deploy | ok (executado) | 1 assinatura `email` para `EmailAdmin`, confirmada pelo Gabriel na 6.3. A mensagem de confirmação foi entregue às 19:20 (6.6). Em hml, 8.1: assinatura criada às 03:48:52 de 07/11, mensagem de confirmação entregue às 03:48 e confirmada pelo Gabriel, conferida às 05:49 UTC. Em prod, em 11/11: 1 assinatura `email` confirmada em `feedback-alarmes-prod`, 1, 0, 0 | INSP em dev, hml e prod |

### Notificação dos alarmes

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Alarme em ALARM notifica | ok (executado) | `set-alarm-state` nos três: 3 ações `Succeeded`, 3 publicações e 3 entregas às 19:42, 0 falhas de entrega (6.7). Réplica em hml em 07/11 (8.1): 1 alarme, ação `Succeeded`, 1 publicação e 1 entrega às 05:50, 0 falhas. Caixa de entrada de dev: o Gabriel confirmou os 12 e-mails de alarme das provas, sem descrição nem nota (6.17) | STACK e E-MAIL confirmado |
| Destino das ações de alarme | ok (executado) | `describe-alarms`: `AlarmActions` só com `feedback-alarmes-dev`, `OKActions` e `InsufficientDataActions` vazias. As 12 mensagens publicadas trazem a mesma lista (6.7, 6.12). Em prod, os três alarmes só com `feedback-alarmes-prod` e as outras ações vazias | INSP em dev e prod |

### Alarme de falha do registro

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Handler marca a falha interna | ok (executado) | `LR.falhaDoServico` e `LR.falhaDoCliente`: status 500 e `Falha ao registrar avaliação` em ERROR com o marcador. Mutação M1. A linha no mesmo formato casou com o filtro implantado no `test-metric-filter` | LOG |
| Falha interna chega ao alarme | ok (executado) | Janelas A e A2: ponto 1,0 em `FalhasRegistrarAvaliacao` às 19:45 e às 21:12, ALARM às 19:46:29 e 21:13:29, publicação no tópico de alarmes com ação `Succeeded` | STACK, LINHA |
| Tempo esgotado chega ao alarme | ok (executado) | Janela B: as duas linhas de plataforma, ponto 2,0 às 19:59, ALARM às 20:00:29. Cada formato casou sozinho no `test-metric-filter` | STACK, LINHA. Texto real do `java25` não observado (observação I) |
| Erro de execução coberto pela definição | ok (executado) | Filtro implantado com `FALHA_FUNCAO`, `"Task timed out"` e `"Status: timeout"`, alarme com `Threshold` 1 e `DatapointsToAlarm` 1 (6.7). `LR.errorEscapadoComMarcador` prova a linha do `Error` (mutação M3) | INSP e LOG |
| Entrada rejeitada | ok (executado) | Janela C: 5 requisições 400 pela API, inclusive corpo malformado, e a linha `Entrada rejeitada` no stream de teste. `EntradaRejeitada` com 5, nenhum ponto de falha, alarme em OK de 20:10:29 a 21:13:29. `LR.rejeicaoSemMarcador` (mutação M2) | STACK |

### Alarme de falha da notificação

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Handler marca a falha parcial do lote | ok (executado) | `LN.falhaParcialDoLote`: `batchItemFailures` com 1 item, sem exceção, só `Falha ao publicar aviso` com o marcador. `LN.falhaDoServico`, `LN.falhaDoCliente`. Mutação M9 | LOG |
| Falha parcial do lote chega ao alarme | ok (executado) | Janela A: ponto 1,0 em `FalhasNotificarCritica` às 19:45, ALARM às 19:46:10, publicação com ação `Succeeded` | STACK, LINHA |
| Item descartado por campo ausente | ok (executado) | 6.11: `INSERT` crítico sem `descricao` no `live`. Nenhuma publicação no tópico de administradores, trace sem SNS. Linha real `Item inválido descartado` com o marcador, ponto às 21:01, ALARM às 21:02:10 | STACK, falha real |
| Tempo esgotado chega ao alarme | ok (executado) | Janela B: ponto 2,0 às 19:59, ALARM às 20:00:10 | STACK, LINHA. Observação I |
| Definição não depende só de Errors | ok (executado) | O alarme implantado usa só `FalhasNotificarCritica`, do filtro do grupo de log. Nenhuma referência a `AWS/Lambda` `Errors`. A 6.11 disparou o alarme com a invocação terminada sem erro de função | INSP e STACK |
| Lote publicado com sucesso | ok (executado) | M1, M2 e M3: `Aviso publicado` em INFO, nenhum ponto em `FalhasNotificarCritica` nos minutos 19:41, 19:57 e 20:28. Na janela C, alarme em OK até 21:02:10. `LN.loteComSucessoSemWarnNemMarcador` | STACK |

### Alarme de falha do relatório

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Handler marca a execução com falha | ok (executado) | `LG.falhaComMarcador` com leitura, gravação e publicação, e `LG.itemInvalidoMarcaSoAExecucao`. Mutação M14 | LOG |
| Execução com falha chega ao alarme | ok (executado) | Janelas A e A2: ponto 1,0 às 19:45 e às 21:12, ALARM às 19:46:24 e 21:13:24, publicação com ação `Succeeded` | STACK, LINHA |
| Tempo esgotado chega ao alarme | ok (executado) | Janela B: ponto 2,0 às 19:59, ALARM às 20:00:24 | STACK, LINHA. Observação I |
| Handler não marca a data inválida | ok (executado) | `LG.dataRejeitadaSemMarcador` (mutação M13). Na stack, a linha real `Data de referência rejeitada` das 20:28:20 não tem `alarme` e não gerou ponto | LOG e STACK |
| Data de referência inválida na stack | ok (executado) | Janela C: `2026-02-30` no `live`, `FunctionError` `Unhandled` com `DataReferenciaInvalidaException`. `DataReferenciaRejeitada` com 1, nenhum ponto em `FalhasGerarRelatorioSemanal`, alarme em OK de 20:10:24 a 21:13:24 | STACK |

### Ausência de tráfego não dispara alarme

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Ambiente sem tráfego | ok (executado) | De 21:24:00 a 22:24:00 UTC, depois da volta a OK da A2: nenhuma invocação, nenhum evento de log nos três grupos, nenhum item de histórico, os três alarmes em OK e nenhuma publicação no tópico de alarmes nem no de administradores (6.13) | STACK |

### Isolamento por ambiente

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Inspeção das definições | ok (executado) | Em 07/11, na stack `feedback-hml` implantada: os três alarmes em `FeedbackServerless/hml`, com ação só em `feedback-alarmes-hml`; os três filtros nos grupos `-hml`, publicando em `FeedbackServerless/hml`; o dashboard `feedback-painel-hml` só com os ARNs dos alarmes `-hml`, `AWS/Lambda` por `FunctionName` `-hml` e `FeedbackServerless/hml`, e a consulta só nos três grupos `-hml`. Nenhuma referência a `-dev` ou `/dev` no corpo do dashboard (8.2). Antes, por configuração, com a sonda em `Stage=hml` | INSP em hml |
| Falha em outro ambiente | ok (executado) | 8.2: linha com o marcador no grupo do registro de dev às 04:48. Ponto 1,0 em `FeedbackServerless/dev` `FalhasRegistrarAvaliacao`, ALARM de dev às 04:49:29 com publicação no tópico de alarmes de dev. Em hml: `feedback-falha-registrar-avaliacao-hml` em OK do início ao fim, sem item de histórico depois da criação; `FeedbackServerless/hml` `FalhasRegistrarAvaliacao` sem ponto; os widgets de hml, pelas mesmas métricas, sem a falha; tópico de alarmes de hml sem publicação | STACK, LINHA. Dashboard de hml lido pela API, sem console (observação M) |

### Invocação sem falha sem log em WARN

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Aviso crítico publicado | ok (executado) | M1, M2 e M3: o grupo da notificação não tem linha em WARN ou ERROR nessas invocações, nem `No metrics were emitted`, com o Powertools real. `LN.loteComSucessoSemWarnNemMarcador`, `HN.MetricasERastreamento.sucesso` (mutação M8) | STACK |
| Registro e relatório com sucesso | ok (executado) | M1, M2 e M3: nenhuma linha em WARN ou ERROR nas 6 invocações com sucesso dos dois grupos. `LR.sucessoSemWarnNemMarcador` (mutação M4), `LG.sucessoSemWarnNemMarcador` | STACK |
| Alarmes sem condição em WARN | ok (executado) | Os três filtros implantados não citam nível de log, e os alarmes só usam as métricas desses filtros | INSP |

### Dashboard por ambiente

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Dashboard criado no deploy | ok (executado) | Único dashboard na conta, `feedback-painel-dev`, criado pelo deploy às 19:20:41, com invocações, falhas, duração e restaurações das três funções (6.14) | INSP |
| Duração de restauração exibida | ok (executado) | A consulta do widget devolveu as restaurações reais do período, com função e duração (6.14) | STACK, pela API. Console não aberto pelo QA (observação M) |
| Falha exibida | ok (executado) | O widget de falhas, pelas mesmas métricas, período e estatística, tem a notificação com 1 às 19:45 e 21:01 e 2 às 19:59 (6.14) | STACK, LINHA e falha real. Observação M |
| Estado dos alarmes | ok (executado) | Widget `alarm` com os ARNs dos três alarmes de dev, que existem e mudaram de estado no QA (6.14) | INSP. Observação M |

### Retenção de logs por ambiente

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Retenção em dev | ok (executado) | `describe-log-groups`: 14 dias nos três grupos (6.5) | INSP |
| Retenção definida pelo ambiente no template | ok (por configuração) | `!If [EhProd, 30, 14]` nos três grupos; `EhProd` é `Stage` igual a `prod` | CONFIG |
| Retenção em prod | ok (executado) | Em 11/11, na stack `feedback-prod` implantada: `describe-log-groups` com `retentionInDays` 30 nos três grupos `/aws/lambda/feedback-*-prod` (8.3) | INSP em prod |

### Memória das funções

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Memória no template | ok (por configuração) | `MemorySize: 1024` no `Globals`; nenhuma função declara `MemorySize` | CONFIG |
| Memória na stack | ok (executado) | `MemorySize` 1024 no alias `live` das três (6.4), e `Memory Size: 1024 MB` em todos os `REPORT` do QA. Em prod, 1024 no `live` das três e no `REPORT` da execução agendada de 09/11 | INSP em dev e prod |

### Aquecimento dos clientes do SDK

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Clientes exercitados antes do snapshot | ok (executado) | `AR.chamaSoDescribeTable`, `AN.chamaSoGetTopicAttributes`, `AG.queryNaParticaoVazia`, `AG.leiturasNoBucketENoTopico`, `AG.nadaGravadoNemPublicado` (mutações M5, M7, M17, M18 e M19). Na stack, uma linha `Cliente aquecido` por cliente no log da publicação: DynamoDB no registro, SNS na notificação, DynamoDB, S3 e SNS no relatório (6.4) | SIM e STACK |
| Falha no aquecimento | ok (executado) | `falhaDoServico`, `falhaDoCliente` e `linkageError` em `AR` e `AN`; `AG.falhaDeUmClienteNaoImpedeOsOutros`, `AG.linkageErrorNaoImpedeOsOutros`, `AG.falhaDoServico` (mutações M6, M12 e M16). Na stack não houve falha | LOG |
| Versão publicada com aquecimento | ok (executado) | `State` `Active` e `OptimizationStatus` `On` nas versões 7, 4 e 2 do `live` (6.4). Em prod, o mesmo na versão 1 do `live` das três | INSP em dev e prod |

### Aquecimento sem efeito em dados

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Deploy sem tráfego | ok (executado) | Nenhuma invocação de 04:00 a 19:41. Tabela com 40 itens e nenhuma escrita, tópico de administradores sem publicação e bucket sem objeto novo na janela do deploy (6.5) | STACK |

### Medição da primeira chamada depois da restauração

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Registro depois da restauração | ok (executado) | `PutItem` de 169, 258 e 200 ms, contra a meta de 4 s, com restaurações de 796,37, 583,97 e 694,94 ms (6.15) | STACK |
| Notificação depois da restauração | ok (executado) | `Publish` de 174, 166 e 200 ms, contra a meta de 2,5 s, com restaurações de 793,52, 541,29 e 528,92 ms (6.15) | STACK |
| Relatório depois da restauração | ok (executado) | Primeira `Query` de 128, 163 e 147 ms, `PutObject` de 339, 238 e 260 ms e `Publish` de 105, 125 e 167 ms, contra a meta de 5 s, com restaurações de 943,97, 540,81 e 660,50 ms (6.15) | STACK |
| Amostra mínima | ok (executado) | 3 restaurações medidas por função, com 1024 MB no `REPORT`, a duração de cada restauração e de cada chamada (6.15) | STACK |
| Meta não atingida | ok (executado) | Nenhuma das 15 chamadas ficou acima da meta. As tabelas da 6.15 registram, para todas, o valor medido e a diferença para a meta, que é o que o cenário exige, e o change segue | STACK |

### Custo dentro do nível gratuito

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Contagem pelo template e pela conta | ok (executado) | Template: 3 métricas de alarme e 1 dashboard por stack. Conta: 3 alarmes em `sa-east-1`, 1 de cobrança em `us-east-1`, nenhum nas outras regiões consultadas, 1 dashboard. Com hml e prod, 10 de 10 e 3 de 3 | INSP e CONFIG |
| Alarmes sem alta resolução | ok (executado) | `Period` 60 nos três alarmes implantados e 21600 no de cobrança, sem `ExtendedStatistic` | INSP |
| Computação com 1024 MB | ok (executado) | Console do Free Tier lido pelo Gabriel em 07/11/2026: 235 de 400.000 GB-s no mês (0,06%), previsão de 1.009 GB-s (0,25%), e 128 de 1.000.000 requisições. Indício do QA na 6.19 | Console de cobrança, leitura do Gabriel |

## Matriz: seguranca

### Tópico de alarmes sem criptografia e sem dado do aluno

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Atributos do tópico | ok (executado) | `get-topic-attributes` sem `KmsMasterKeyId` (6.6) | INSP |
| Conteúdo da notificação | ok (executado) | Avaliações com 3 marcadores registradas antes e entre as janelas. As 12 mensagens publicadas, nas variantes `default`, `email` e `sms`, trazem só dados do alarme e nenhum marcador, descrição ou nota (6.12). Na caixa de entrada, o Gabriel confirmou os 12 e-mails sem descrição nem nota (6.17) | STACK e E-MAIL confirmado |
| Métrica do alarme é contagem | ok (executado) | Os três filtros implantados têm `metricValue` `"1"`, sem seletor `$` e sem dimensão (6.5) | INSP |

### Tópico de alarmes só aceita TLS

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Política do tópico | ok (executado) | `NegaAcessoSemTls` implantado com `Principal` `*`, `aws:SecureTransport` `"false"` e as 11 ações da D2 (6.6). A lista foi conferida contra a D2, não contra a página da referência do SNS (observação H) | INSP |
| Requisição sem TLS | ok (executado) | `get-topic-attributes` em HTTP negado com `explicit deny in a resource-based policy`; controle no tópico de administradores com outro motivo (6.6) | STACK |

### Publicação no tópico de alarmes restrita ao CloudWatch da conta

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Publicação pelo CloudWatch mantida | ok (executado) | 12 transições para ALARM, 12 ações `Succeeded` e 12 publicações no tópico de alarmes, 0 falhas de entrega. Em 07/11, mais 1 em dev (8.2) e 1 em hml (8.1), as duas `Succeeded`, entregues e sem falha | STACK em dev e hml |
| Permissão restrita à conta | ok (executado) | Único `Allow`: `sns:Publish` para `cloudwatch.amazonaws.com`, com `aws:SourceAccount` da conta e `aws:SourceArn` em `alarm:feedback-*-dev`. Nenhum `Allow` para `*` (6.6) | INSP |

### Dashboard sem dado do aluno

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Inspeção dos widgets | ok (executado) | `get-dashboard`: contagens, durações e estado de alarmes; a consulta exibe só `@timestamp`, `funcao` e `restauracaoMs`. O corpo não cita `descricao` nem `nota` (6.14) | INSP |
| Marcador depois do tráfego | ok (executado) | As consultas dos widgets no período das 3 avaliações com marcador não devolvem texto de avaliação, e nenhum grupo de log contém os marcadores (6.12, 6.14) | STACK, pela API. Observação M |

### Permissões das funções restritas ao ambiente

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Inspeção das roles | ok (executado) | Comparação com o `PutRolePolicy` anterior no CloudTrail: 0 statements removidos, 4 novos, todos de leitura e em recursos `dev` (6.16) | INSP |
| Boundary mantido | ok (executado) | As quatro roles da stack com `feedback-limite-funcoes-dev`, na `v2` (6.16). Em prod, as três roles das funções e a do agendamento com `feedback-limite-funcoes-prod` | INSP em dev e prod |

### Implantação pela role do ambiente

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Deploy em dev pela pipeline | ok (executado) | Execução 42278391413 a partir de `develop`; o CloudTrail mostra a role `github-deploy-feedback-dev` executando o change set e criando o tópico, a assinatura, os filtros, os alarmes e o dashboard | INSP |
| Permissões de deploy por ambiente | ok (executado) | `get-policy-version` das duas políticas de deploy de dev: escrita em alarmes, dashboards, tópicos e filtros só em nomes com o sufixo `dev` (6.16) | INSP |

## Matriz: entrega-continua

### Empacotamento das funções no CI

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Função sem código | ok (por configuração) | `sam build` numa cópia sem `functions/notificar-critica/src` terminou em `Build Failed` com saída 1. O passo do CI roda o mesmo comando, e o job `ci` é o check obrigatório. Nenhuma tarefa abre um PR quebrado de propósito | LOCAL e CONFIG |
| PR válido com empacotamento | ok (executado) | PR #13, execução 42276798571: check `ci` `success`, e o log mostra `Building codeuri` das três funções e `Build Succeeded` | CI |
| Passo no workflow sem credencial AWS | ok (por configuração) | Passo `Empacotar as funções` com `sam build`; `permissions: contents: read`, sem `id-token` e sem `configure-aws-credentials` | CONFIG |

## Cenários ainda em aberto

| Situação | Cenários |
|---|---|
| Nenhum (0) | |

Os três que estavam aqui fecharam em 11/11: `Retenção em prod` pela leitura de prod (8.3), `Computação com 1024 MB` pela leitura do console feita pelo Gabriel (6.19) e `Alarme em ALARM notifica` pela confirmação dos e-mails pelo Gabriel (6.17).

## Defeitos

Nenhum defeito encontrado no código de produção.

## Risco aceito

**`RuntimeException` que escapa do handler da notificação sai sem o marcador e não dispara alarme.** Aceito pelo Gabriel em 06/11/2026. O `catch (Error e)` cobre `Error`, e o tempo esgotado tem linhas próprias no filtro. No registro e no relatório, toda `RuntimeException` passa por um `catch` com marcador. Na notificação, só a publicação tem `catch`. Por leitura do domínio e do handler, hoje nenhuma `RuntimeException` sai de `processar`: `deAtributos` valida tudo antes do construtor, e `ehCriacaoCritica` garante a urgência que `AvisoCritico.de` exige. Uma regressão futura que lance ali terminaria a invocação com erro de função, sem marcador e sem alarme.

## Observações

Não reprovam cenário.

- **A. Consultas que o SSO bloqueou no QA local.** Feitas em dev: contagem de alarmes e dashboards e `test-metric-filter`. Resolvida.
- **B. WARN real fora da prova local.** Coberto na 6.10 e na 6.15 com o Powertools real. Resolvida.
- **C. `Computação com 1024 MB`.** Ganhou a tarefa 6.19 e fechou com a leitura do console feita pelo Gabriel em 07/11/2026. Resolvida.
- **D. `Destino das ações de alarme`.** Incluído na 6.7. Resolvida.
- **E. Registro do hook só por revisão.** A linha `Cliente aquecido` de cada cliente no log da publicação prova, na stack, que o hook foi registrado e rodou antes do snapshot. Resolvida.
- **F. `RuntimeException` que escapa da notificação.** Virou risco aceito.
- **G. Linha do `Error` fora do `withLogging`.** Continua sem verificação: só um `Error` real mostraria se a linha sai sem `function_request_id`. Não afeta o filtro.
- **H. Lista do `Deny` contra a referência.** As 11 ações implantadas foram conferidas contra a D2, não contra a página da referência do SNS.
- **I. Texto do tempo esgotado no `java25`.** As linhas da janela B seguem o formato do material de suporte, não uma linha real do runtime. Risco já registrado no design.
- **J. Arquivos de outro agente.** Só valia para a rodada local.
- **K. A volta a OK leva cerca de 10 minutos, não 5.** Nas janelas A, B e A2 e na 6.11, o alarme entrou em ALARM cerca de 1 minuto depois do ponto e voltou a OK exatamente 10 minutos depois de entrar em ALARM, ou cerca de 11 minutos depois do minuto do ponto. A D5, a D14 e o README ("O que cada alarme cobre" e "Testar um alarme") dizem cerca de 5 minutos. O comportamento não contraria a spec, porque cada episódio gerou um e-mail e o alarme voltou sozinho. Depois do `set-alarm-state`, sem ponto na métrica, a volta levou de 60 a 80 s, como o README diz. **Para o orquestrador:** o texto do README e o planejamento de testes (esperar a volta a OK entre janelas) precisam usar 10 a 11 minutos.
- **L. Linhas de teste com acento e `RESTORE_REPORT` sintético.** Duas falhas do QA na montagem das linhas, nenhuma do produto:
  - O AWS CLI no Windows lê `file://` em cp1252, mesmo com `PYTHONUTF8=1`. Na linha A do registro e nas linhas A e C do relatório, o campo `message` chegou ao grupo com o acento em dupla codificação (`Falha ao registrar avaliaÃ§Ã£o`, `Falha ao gerar relatÃ³rio`, `Data de referÃªncia rejeitada`). O marcador e os demais campos ficaram certos, e o filtro não lê `message`, então o resultado das janelas não muda. O QA repetiu a janela A no registro e no relatório (A2) com `AWS_CLI_FILE_ENCODING=UTF-8`, e as linhas A2 ficaram idênticas às reais. O `test-metric-filter` também foi refeito em UTF-8. A linha real `Data de referência rejeitada` das 20:28:20 cobre a janela C do relatório. **Para o orquestrador:** o exemplo do README em "Testar um alarme" usa uma linha sem acento e funciona, mas vale avisar que linhas com acento precisam de `AWS_CLI_FILE_ENCODING=UTF-8` no Windows.
  - Na janela C, o QA gravou em cada grupo, além do `REPORT` com restauração que a D14 pede, uma linha `RESTORE_REPORT Restore Duration: 790.00 ms`. A D7 contava com que nenhuma linha de teste começasse com `RESTORE_REPORT`. Por isso, o widget de restaurações mostra três restaurações falsas de 790,00 ms às 20:28:16, 20:28:17 e 20:28:18 de 06/11, uma por função, até a retenção apagar o stream. Elas não entram em métrica nem em alarme, e são identificáveis pelo valor redondo e pelo stream `qa-observabilidade-20261106`. Ninguém deve citá-las como medição.
- **M. Dashboard conferido pela API, sem console.** O QA leu a definição com `get-dashboard` e rodou as mesmas métricas e a mesma consulta dos widgets. A API `StartQuery` recusa o prefixo `SOURCE '...' |` que o widget de log usa, então a consulta foi rodada com `--log-group-names`. O desenho dos widgets no console não foi visto pelo QA. O Gabriel pode conferir ao abrir `feedback-painel-dev` no console, com o período de 19:30 a 21:30 UTC de 06/11, o que já está no roteiro do vídeo.
- **N. Relógio local cerca de 7 s adiantado.** Ver "Validade da evidência da stack".
- **O. Efeitos colaterais da 6.15.** 3 avaliações novas na tabela de dev (43 itens), `relatorios/2026-11-06.txt` regravado três vezes, com o mesmo conteúdo de 38 avaliações, e 6 e-mails aos administradores. A conferência da execução agendada de `add-relatorio-semanal`, que usa `relatorios/2026-11-09.txt`, não foi afetada. As 3 avaliações têm `dataEnvio` 2026-11-06 e entraram no relatório agendado de 2026-11-09, como as 2 do QA do relatório.
- **P. Aquecimento custa cerca de 1,6 a 1,8 s na publicação.** A primeira chamada de cada cliente durante o hook levou 1.602 a 1.798 ms, e o `INIT_REPORT` ficou entre 7,4 e 8,6 s, dentro do limite de 130 s. É o custo que saiu da primeira invocação.

## Divergências do design

- **D5 e D14, tempo de volta a OK:** cerca de 10 minutos depois do ALARM, e não cerca de 5 depois do último ponto (observação K).
- **D7, linhas de teste fora do widget de restaurações:** não valeu neste QA, por erro na montagem da janela C (observação L).
- **D16, tempo do CI:** o `sam build` levou 45 s no runner, dentro da estimativa de 45 a 60 s a mais por PR.

Fora isso, o que está implantado segue a D1 a D17.

## Prova das asserções

Rodada local. No próprio worktree, 19 mutações foram aplicadas ao código de produção, uma de cada vez, com `mvn -o -B test -pl functions/<módulo>` e JDK 25. As 19 foram detectadas. Cada arquivo foi restaurado e conferido por hash SHA-256.

| Mutação | Detectada por |
|---|---|
| M1. Registro: sem marcador na falha interna | `LR.falhaDoServico`, `LR.falhaDoCliente` |
| M2. Registro: marcador na entrada rejeitada | `LR.rejeicaoSemMarcador` |
| M3. Registro: sem o `catch (Error e)` | `LR.errorEscapadoComMarcador` |
| M4. Registro: linha de sucesso em WARN | `LR.sucessoSemWarnNemMarcador`, `LR.sucessoRegistraIdEUrgencia`, `LR.logsSaemEmJsonComContextoDaLambda` |
| M5. Registro: aquecimento grava item | `AR.chamaSoDescribeTable` |
| M6. Registro: aquecimento não pega `LinkageError` | `AR.linkageError` |
| M7. Notificação: aquecimento chama `publish` | `AN.chamaSoGetTopicAttributes` |
| M8. Notificação: `flush` incondicional, inclusive no sucesso | `HN.MetricasERastreamento.sucesso`, `.ignoradoNaoDescarrega`, `.falhaNaoDescarrega`, `.contagemPorInvocacao`, `.falhaAoContar` |
| M9. Notificação: sem marcador na falha de publicação | `LN.falhaDoServico`, `LN.falhaDoCliente`, `LN.falhaParcialDoLote` |
| M10. Notificação: sem marcador no descarte | `LN.itemInvalido`, nos 4 campos |
| M11. Notificação: sem o `catch (Error e)` | `LN.errorEscapadoComMarcador` |
| M12. Notificação: aquecimento passa a exceção ao logger | `AN.falhaDoCliente`, `AN.linkageError` |
| M13. Relatório: marcador na data rejeitada | `LG.dataRejeitadaSemMarcador` |
| M14. Relatório: sem marcador na execução com falha | `LG.falhaComMarcador`, nos 3 casos, `LG.itemInvalidoMarcaSoAExecucao` |
| M15. Relatório: `Error` engolido em vez de relançado | `LG.errorEscapadoComMarcador` |
| M16. Relatório: S3 e SNS no mesmo `try` do aquecimento | `AG.falhaDeUmClienteNaoImpedeOsOutros`, `AG.linkageErrorNaoImpedeOsOutros`, `AG.sucessoRegistraClientesAquecidos` |
| M17. Relatório: aquecimento grava objeto | `AG.nadaGravadoNemPublicado` |
| M18. Relatório: `Query` sem `Limit` 1 | `AG.queryNaParticaoVazia` |
| M19. Relatório: `Query` numa partição real | `AG.queryNaParticaoVazia` |

## Testes acrescentados

Nenhum. Os cenários que ficaram em aberto dependiam da release, do console de cobrança ou da caixa de entrada, e nenhum se resolvia com teste local. Os três fecharam em 11/11. Nenhum código de produção foi alterado.

## Coleção Postman

Não se aplica. O change não altera a API.
