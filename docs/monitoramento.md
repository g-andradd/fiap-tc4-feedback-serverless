# Monitoramento

Como a plataforma é observada em dev, hml e prod: logs, métricas, alarmes, dashboard e traces do X-Ray. Tudo é declarado no [template.yaml](../template.yaml) e criado pelo deploy de cada stack, com o sufixo do ambiente no nome. Nenhum recurso de monitoramento é criado à mão.

O comportamento de cada função está em [docs/funcoes.md](funcoes.md). A política e a criptografia do tópico de alarmes e o registro de riscos aceitos estão em [docs/seguranca.md](seguranca.md).

## Recursos por ambiente

Cada stack cria:

| Recurso | Nome | Quantidade | Output da stack |
|---|---|---|---|
| Grupos de log | `/aws/lambda/feedback-<função>-<amb>` | 3 | |
| Filtros de métrica | `feedback-falhas-<função>-<amb>`, um por grupo de log | 3 | |
| Alarmes | `feedback-falha-<função>-<amb>`, um por função | 3 | |
| Tópico de alarmes | `feedback-alarmes-<amb>`, com assinatura por e-mail | 1 | `TopicoAlarmesArn` |
| Dashboard | `feedback-painel-<amb>` | 1 | `PainelNome` |
| Tracing | X-Ray ativo nas três funções (`Tracing: Active` no `Globals`) | | |

Onde cada parte foi conferida:

- **dev:** tudo, em 06/11/2026, inclusive os testes de alarme, o dashboard e a medição pelo X-Ray, no [QA de add-observabilidade-seguranca](../openspec/changes/archive/2026-11-11-add-observabilidade-seguranca/qa.md), seções 6.4 a 6.16.
- **hml:** alarmes, filtros, retenção, um `set-alarm-state` e o isolamento em relação a dev, em 07/11/2026, no [QA de add-fundacao-projeto](../openspec/changes/archive/2026-11-11-add-fundacao-projeto/qa.md), seção "Release 1.0.0 em hml (07/11/2026)", e nas seções 8.1 e 8.2 do QA de add-observabilidade-seguranca.
- **prod:** só leitura, em 10 e 11/11/2026 (UTC): retenção de 30 dias nos três grupos, os três alarmes em OK com a configuração do template e ação só no tópico de prod, os três filtros com o mesmo padrão e o dashboard. Registrada em [openspec/changes/archive/2026-11-11-add-documentacao-entrega/qa.md](../openspec/changes/archive/2026-11-11-add-documentacao-entrega/qa.md). Nenhum teste de alarme foi feito em prod.

**Antes dos comandos.** Os comandos foram escritos para o Git Bash e usam o perfil administrativo `tc4-admin`; as roles de deploy não têm as permissões de teste. Rode `export MSYS_NO_PATHCONV=1` no terminal antes de qualquer comando com `/aws/lambda/...`. Sem isso, o Git Bash troca o nome do grupo por um caminho do Windows.

## Logs

### Formato

As três funções gravam com o Powertools Logging, pelo layout `LambdaJsonLayout.json` configurado no `log4j2.xml` de cada uma: uma linha JSON por evento, no grupo de log da função. O nível é INFO para as três, pela variável `POWERTOOLS_LOG_LEVEL` do `Globals`.

Campos que toda linha da aplicação traz: `level`, `message`, `cold_start`, `function_arn`, `function_memory_size`, `function_name`, `function_request_id`, `function_version`, `service`, `timestamp` e `xray_trace_id`. Depois deles vêm os campos da linha, como `id`, `urgencia`, `campos`, `classe` ou `codigoErro`. A lista de linhas de cada função, com nível e campos, está em [docs/funcoes.md](funcoes.md).

Exemplo, a linha `Relatório gerado` do relatório semanal, com os identificadores trocados por marcadores:

```json
{"level":"INFO","message":"Relatório gerado","cold_start":false,"function_arn":"arn:aws:lambda:sa-east-1:<ID_DA_CONTA>:function:feedback-gerar-relatorio-semanal-prod:live","function_memory_size":1024,"function_name":"feedback-gerar-relatorio-semanal-prod","function_request_id":"<ID_DA_REQUISICAO>","function_version":"1","service":"gerar-relatorio-semanal","timestamp":"2026-11-09T11:00:14.209Z","xray_trace_id":"<ID_DO_TRACE>","dataReferencia":"2026-11-09","quantidadeTotal":0,"totaisPorUrgencia":{"CRITICA":0,"MEDIA":0,"BAIXA":0},"bucket":"feedback-relatorios-<ID_DA_CONTA>-prod","chave":"relatorios/2026-11-09.txt"}
```

No mesmo grupo aparecem as linhas do Lambda, em texto:

| Linha | Quando |
|---|---|
| `INIT_START`, `INIT_REPORT` | Na publicação de cada versão, quando o SnapStart cria o snapshot. É onde ficam as linhas `Cliente aquecido` do aquecimento |
| `RESTORE_START`, `RESTORE_REPORT` | Quando um ambiente é restaurado do snapshot. `RESTORE_REPORT` traz `Restore Duration` |
| `START`, `END`, `REPORT` | Em cada invocação. `REPORT` traz `Duration`, `Billed Duration`, `Memory Size`, `Max Memory Used` e, depois de uma restauração, `Restore Duration` e `Billed Restore Duration` |

As métricas do Powertools também saem no grupo, como linhas JSON no formato EMF, que o CloudWatch converte em métrica.

`cold_start` vem sempre `false`. O Powertools só considera cold start quando o ambiente é `on-demand`, e as funções rodam pelo alias `live` com SnapStart. A restauração aparece na linha `RESTORE_REPORT` e no widget de restaurações do dashboard.

### Sem dado do aluno

Nenhuma linha, em nível nenhum, leva a descrição ou a nota. As linhas trazem identificador, urgência, nome de campo rejeitado, classe e código de erro. Nos QAs, as avaliações de teste tinham marcadores na descrição, e a busca por esses marcadores nos grupos de log devolveu 0 eventos em dev (seção 6.12 do QA de add-observabilidade-seguranca) e em hml (item 5 da seção da release no QA de add-fundacao-projeto). A regra e o que muda se o nível sair de INFO estão em [docs/seguranca.md](seguranca.md).

### Retenção por ambiente

| Ambiente | Retenção dos três grupos | Conferido em |
|---|---|---|
| dev | 14 dias | 06/11/2026 (UTC), seção 6.5 do QA de add-observabilidade-seguranca |
| hml | 14 dias | 07/11/2026 (UTC), item 1 da seção da release no QA de add-fundacao-projeto |
| prod | 30 dias | 10 e 11/11/2026 (UTC), leitura de prod registrada no QA de `add-observabilidade-seguranca` |

O valor vem de `RetentionInDays: !If [EhProd, 30, 14]`, a mesma condição que liga a recuperação pontual da tabela só em prod. Os grupos são recursos da stack, criados antes das funções (`DependsOn`), então a retenção vale desde a primeira linha.

Os streams de teste dos alarmes ficam no grupo até a retenção apagá-los: `qa-observabilidade-20261106`, nos três grupos de dev, expira por volta de 20/11/2026, e `qa-observabilidade-20261107`, no grupo do registro de dev, por volta de 21/11/2026.

### Como consultar

- **Console:** CloudWatch, em `sa-east-1`, Grupos de logs, `/aws/lambda/feedback-<função>-<amb>`. Para buscar em vários grupos ao mesmo tempo, use o Logs Insights e selecione os grupos.
- **CLI:** `filter-log-events` busca no servidor por um termo, em todos os streams do grupo.

Registro: `filter-log-events` foi usado nos grupos de dev em 06/11/2026 ([QA de add-observabilidade-seguranca](../openspec/changes/archive/2026-11-11-add-observabilidade-seguranca/qa.md), seções 6.12 e 6.13), nos de hml em 07/11/2026 ([QA de add-fundacao-projeto](../openspec/changes/archive/2026-11-11-add-fundacao-projeto/qa.md), seção "Release 1.0.0 em hml", item 5) e no grupo do relatório de prod em 10 e 11/11/2026 (UTC), na leitura deste change ([openspec/changes/archive/2026-11-11-add-documentacao-entrega/qa.md](../openspec/changes/archive/2026-11-11-add-documentacao-entrega/qa.md)).

```bash
aws logs filter-log-events \
  --log-group-name /aws/lambda/feedback-registrar-avaliacao-dev \
  --filter-pattern '"Cliente aquecido"' \
  --start-time $(( ($(date +%s) - 3600) * 1000 )) \
  --query "events[].message" \
  --region sa-east-1 --profile tc4-admin
```

Troque o termo de `--filter-pattern` pela mensagem que procura e o nome do grupo pela função e pelo ambiente. `--start-time` é em milissegundos; o exemplo cobre a última hora. A saída traz o ARN da função, com o ID da conta: não copie para arquivo versionado.

## Métricas

Todas as métricas próprias ficam no namespace do ambiente, `FeedbackServerless/<amb>`. O `Globals` define `POWERTOOLS_METRICS_NAMESPACE: !Sub FeedbackServerless/${Stage}`, e os filtros de log usam o mesmo namespace em `MetricNamespace`. Assim um alarme de prod não reage a uma falha de dev: na seção 8.2 do QA de add-observabilidade-seguranca, uma linha de falha no grupo de dev gerou ponto só em `FeedbackServerless/dev`, e os alarmes de hml ficaram em OK.

| Origem | Namespace | Métricas | Quando recebem dado |
|---|---|---|---|
| Filtros de log | `FeedbackServerless/<amb>` | `FalhasRegistrarAvaliacao`, `FalhasNotificarCritica`, `FalhasGerarRelatorioSemanal` | Valor 1 a cada linha que casa com o filtro, sem dimensão e sem valor padrão. Sem falha, não há ponto |
| Powertools, registro | `FeedbackServerless/<amb>` | `AvaliacaoRegistrada`, `EntradaRejeitada`, `ErroInterno` | Nas invocações, pelo EMF |
| Powertools, notificação | `FeedbackServerless/<amb>` | `ItemInvalido` | Só quando um item crítico é descartado. Invocação sem métrica não grava nada |
| Powertools, relatório | `FeedbackServerless/<amb>` | `RelatorioGerado`, `DataReferenciaRejeitada`, `ErroInterno` | Nas invocações, pelo EMF |
| Lambda | `AWS/Lambda` | `Invocations`, `Duration`, por `FunctionName` | Publicadas pelo serviço. O dashboard usa as duas |

Em que caso cada métrica do Powertools é contada está em [docs/funcoes.md](funcoes.md).

- **Nenhum alarme usa `Errors` do Lambda.** O 500 do registro e a falha parcial do lote da notificação terminam a invocação sem erro de função, e `Errors` não as veria.
- **`ColdStart` não aparece.** O handler pede a métrica, mas o Powertools só a emite em ambiente `on-demand`, pelo mesmo motivo do `cold_start` dos logs.
- **Namespace antigo:** até `add-observabilidade-seguranca`, as métricas iam para `FeedbackServerless`, sem o ambiente. As que ficaram lá não recebem mais dado.

## Alarmes

### Filtro de falha

Cada função tem um filtro de métrica no próprio grupo de log. O padrão é o mesmo nas três:

```text
?FALHA_FUNCAO ?"Task timed out" ?"Status: timeout"
```

- `FALHA_FUNCAO` é o valor do campo `alarme` que os handlers gravam nas linhas que representam falha da função, inclusive as tratadas.
- `"Task timed out"` e `"Status: timeout"` são as duas formas conhecidas da linha de tempo esgotado do Lambda. O tempo esgotado encerra a invocação antes de qualquer `catch`, então não passa pelo marcador.
- O `?` faz o filtro casar com qualquer um dos três termos. A busca é por trecho de texto, e o filtro vale para todos os streams do grupo.

### Configuração

Esta é a única tabela de configuração dos alarmes do repositório. Os outros documentos apontam para ela.

| Alarme | Grupo do filtro | Métrica | Estatística e limiar | Período | Avaliação | Dado ausente | Ações |
|---|---|---|---|---|---|---|---|
| `feedback-falha-registrar-avaliacao-<amb>` | `/aws/lambda/feedback-registrar-avaliacao-<amb>` | `FeedbackServerless/<amb>` `FalhasRegistrarAvaliacao` | `Sum` maior ou igual a 1 | 60 s | 5 períodos, 1 ponto para alarmar | `notBreaching` | `AlarmActions`: só `feedback-alarmes-<amb>`. Sem `OKActions` e sem `InsufficientDataActions` |
| `feedback-falha-notificar-critica-<amb>` | `/aws/lambda/feedback-notificar-critica-<amb>` | `FeedbackServerless/<amb>` `FalhasNotificarCritica` | `Sum` maior ou igual a 1 | 60 s | 5 períodos, 1 ponto para alarmar | `notBreaching` | Idem |
| `feedback-falha-gerar-relatorio-semanal-<amb>` | `/aws/lambda/feedback-gerar-relatorio-semanal-<amb>` | `FeedbackServerless/<amb>` `FalhasGerarRelatorioSemanal` | `Sum` maior ou igual a 1 | 60 s | 5 períodos, 1 ponto para alarmar | `notBreaching` | Idem |

O que cada valor faz:

- **`Sum` maior ou igual a 1:** uma falha basta.
- **60 s, 5 períodos, 1 ponto:** uma falha em qualquer minuto de uma janela de 5 minutos. A janela absorve o atraso entre a linha de log e o ponto da métrica. Período abaixo de 60 s seria alta resolução, fora do nível gratuito.
- **`notBreaching`:** minuto sem ponto conta como minuto sem falha. Sem tráfego, o alarme fica em OK, e depois de uma falha ele volta a OK sozinho.
- **Só o tópico de alarmes:** nenhum alarme publica no tópico de administradores, e a volta a OK não gera e-mail.

Os nove alarmes implantados, três por ambiente, têm exatamente esses valores na leitura de 10 e 11/11/2026 (UTC), e os três filtros de prod têm o mesmo padrão, valor `1` e o namespace `FeedbackServerless/prod`. Em dev e hml, a mesma conferência está na seção 6.7 do QA de add-observabilidade-seguranca e no item 1 da seção da release no QA de add-fundacao-projeto.

### O que dispara e o que não dispara

| Função | Dispara | Não dispara |
|---|---|---|
| `registrar-avaliacao` | Falha ao gravar, respondida com 500 (`Falha ao registrar avaliação`) | Requisição rejeitada com 400, por entrada ou por corpo inválido |
| `notificar-critica` | Falha ao publicar o aviso (`Falha ao publicar aviso`), inclusive quando só parte do lote falha e a invocação termina sem erro; item crítico descartado por falta de campo obrigatório (`Item inválido descartado`) | Lote publicado com sucesso; registro ignorado por não ser criação de item crítico |
| `gerar-relatorio-semanal` | Falha na leitura, na gravação ou na publicação (`Falha ao gerar relatório`) | Data de referência rejeitada no acionamento manual, apesar de ser gravada em ERROR, porque a linha não leva o marcador |
| as três | `Error` que escapa do handler, como `NoClassDefFoundError` (`Erro na execução da função`), e tempo esgotado | Falha no aquecimento, que acontece na publicação da versão e grava WARN; período sem invocação; qualquer linha em WARN |

Na stack de dev, cada linha da coluna "Dispara" chegou ao alarme por uma linha de teste com o mesmo texto (janelas A, B e A2), e o descarte de item inválido chegou por uma falha real, provocada sem alterar a função (seção 6.11). As linhas da coluna "Não dispara" foram reproduzidas e também feitas de verdade, com 400 pela API e a data `2026-02-30` no relatório, sem nenhum ponto na métrica (janela C). Uma hora sem tráfego não gerou transição (seção 6.13). Tudo nas seções 6.8 a 6.13 do [QA de add-observabilidade-seguranca](../openspec/changes/archive/2026-11-11-add-observabilidade-seguranca/qa.md).

### Tempos observados

- **Até o ALARM:** cerca de 1 minuto depois da linha de falha.
- **Volta a OK:** sozinha, 10 minutos depois de entrar em ALARM, ou 10 a 11 minutos depois do minuto da linha. Na falha real da seção 6.11, a invocação foi às 21:01:03, o ALARM às 21:02:10 e a volta a OK às 21:12:10 UTC.
- **E-mail:** um por transição para ALARM. Uma nova falha depois da volta a OK gera outro e-mail. Em 06/11/2026, as 12 transições de dev geraram 12 e-mails, todos recebidos pelo Gabriel, nenhum com descrição ou nota ([tarefas de add-observabilidade-seguranca](../openspec/changes/archive/2026-11-11-add-observabilidade-seguranca/tasks.md), 6.17).

### Tópico de alarmes e assinatura

Os alarmes publicam em `feedback-alarmes-<amb>`, separado do tópico de administradores. A assinatura vai para o mesmo endereço do secret `EMAIL_ADMIN` e precisa ser confirmada em cada ambiente pelo link do e-mail de confirmação; até lá, os alarmes publicam e ninguém recebe. O passo está na implantação do [README](../README.md).

A mensagem de alarme traz nome, descrição, estado, motivo, horário, métrica e limiar, sem dado do aluno, porque a métrica é só uma contagem de linhas (seção 6.12 do QA de add-observabilidade-seguranca). Ela traz o ID da conta e o ARN do alarme, e o rodapé do SNS traz o endereço do assinante: não copie o e-mail para arquivo versionado. Por que o tópico não tem criptografia em repouso e como a política nega acesso sem TLS e só aceita os alarmes do ambiente está em [docs/seguranca.md](seguranca.md).

### Limites conhecidos

Cada um tem linha no registro de riscos aceitos de [docs/seguranca.md](seguranca.md).

- **Busca por trecho de texto:** qualquer linha futura com `FALHA_FUNCAO` ou `Task timed out` dispara o alarme. Os testes de log dos handlers afirmam em quais linhas o marcador aparece, e nenhum texto do aluno vai para log, então ele não consegue forjar o marcador.
- **Texto do tempo esgotado no `java25`:** o tempo esgotado nunca foi provocado nas funções. O padrão cobre os dois formatos do material de suporte da AWS, e as linhas da janela B seguiram esses formatos, mas o texto real do runtime não foi observado.
- **`RuntimeException` na notificação:** uma `RuntimeException` que escape do handler da notificação sai sem o marcador e não dispara alarme. Pela leitura do código, hoje nenhuma sai de lá.
- **Falha fora da JVM:** um processo encerrado pelo sistema, sem passar pelo `catch (Error e)` e sem tempo esgotado, não grava o marcador.

### Ver o estado dos alarmes

Registro: rodou em dev em 06/11/2026 ([QA de add-observabilidade-seguranca](../openspec/changes/archive/2026-11-11-add-observabilidade-seguranca/qa.md), seções 6.7 a 6.11) e em hml em 07/11/2026 (mesmo QA, seções 8.1 e 8.2).

```bash
aws cloudwatch describe-alarms \
  --alarm-names feedback-falha-registrar-avaliacao-dev feedback-falha-notificar-critica-dev feedback-falha-gerar-relatorio-semanal-dev \
  --query "MetricAlarms[].[AlarmName,StateValue]" \
  --output table \
  --region sa-east-1 --profile tc4-admin
```

Para outro ambiente, troque o sufixo dos três nomes. O histórico de transições de um alarme fica no console, na aba de histórico do alarme, ou em `aws cloudwatch describe-alarm-history`.

## Dashboard

O dashboard `feedback-painel-<amb>` fica no console do CloudWatch, em Dashboards. Ele tem cinco widgets, todos com o sufixo do ambiente:

| Widget | Tipo | Fonte |
|---|---|---|
| Alarmes de falha | Estado de alarme | Os três alarmes do ambiente, pelo ARN |
| Invocações | Série temporal | `AWS/Lambda` `Invocations` das três funções, `Sum`, 300 s |
| Falhas, tratadas e de execução | Série temporal | As três métricas dos filtros em `FeedbackServerless/<amb>`, `Sum`, 60 s |
| Duração (ms) | Série temporal | `AWS/Lambda` `Duration` das três funções, `Average` e `Maximum`, 300 s |
| Restaurações do SnapStart | Tabela do Logs Insights | Linhas `RESTORE_REPORT` dos três grupos de log, com horário, função e duração da restauração |

São 12 métricas no dashboard, abaixo do limite de 50 do nível gratuito.

**Restaurações pelo log.** O CloudWatch não tem métrica para a duração da restauração, e a métrica `ColdStart` não sai com SnapStart. Por isso o último widget lê o log. A consulta, em dev:

```text
SOURCE '/aws/lambda/feedback-registrar-avaliacao-dev' | SOURCE '/aws/lambda/feedback-notificar-critica-dev' | SOURCE '/aws/lambda/feedback-gerar-relatorio-semanal-dev'
| filter @message like /^RESTORE_REPORT/
| parse @message 'Restore Duration: * ms' as restauracaoMs
| parse @log /feedback-(?<funcao>[a-z-]+)-dev$/
| display @timestamp, funcao, restauracaoMs
| sort @timestamp desc | limit 50
```

- A consulta exibe só horário, nome da função e duração. O `@log` traz o ID da conta, por isso só o nome da função extraído aparece. Os outros widgets são contagens e durações. Nenhum widget mostra descrição, nota ou corpo de mensagem (seção 6.14 do QA de add-observabilidade-seguranca).
- A consulta roda a cada abertura do dashboard e conta na cota gratuita de dados lidos do Logs Insights. No QA de dev, ela leu 44 KB.
- No dashboard de dev, três restaurações de 790,00 ms às 20:28 UTC de 06/11/2026 são linhas de teste, gravadas por engano no stream `qa-observabilidade-20261106` (observação L do QA). Elas não são medição e somem com a retenção.

**A mesma consulta pela CLI.** O prefixo `SOURCE '...' |` só vale no widget. A API `StartQuery` recusa esse texto com `MalformedQueryException`. Pela CLI, a consulta começa em `filter`, e os grupos vão em `--log-group-names`.

Registro: a consulta foi rodada assim, a partir do `filter` e com `--log-group-names`, nos grupos de dev em 06/11/2026 e nos de hml em 07/11/2026 ([QA de add-observabilidade-seguranca](../openspec/changes/archive/2026-11-11-add-observabilidade-seguranca/qa.md), seções 6.14 e 8.2, observação M), e nos de dev em 10 e 11/11/2026 (UTC), na conferência deste documento ([openspec/changes/archive/2026-11-11-add-documentacao-entrega/qa.md](../openspec/changes/archive/2026-11-11-add-documentacao-entrega/qa.md)).

```bash
CONSULTA=$(cat <<'EOF'
filter @message like /^RESTORE_REPORT/
| parse @message 'Restore Duration: * ms' as restauracaoMs
| parse @log /feedback-(?<funcao>[a-z-]+)-dev$/
| display @timestamp, funcao, restauracaoMs
| sort @timestamp desc | limit 50
EOF
)

ID=$(aws logs start-query \
  --log-group-names /aws/lambda/feedback-registrar-avaliacao-dev /aws/lambda/feedback-notificar-critica-dev /aws/lambda/feedback-gerar-relatorio-semanal-dev \
  --start-time $(( $(date +%s) - 7 * 86400 )) --end-time $(date +%s) \
  --query-string "$CONSULTA" \
  --query queryId --output text \
  --region sa-east-1 --profile tc4-admin)

aws logs get-query-results --query-id "$ID" --region sa-east-1 --profile tc4-admin
```

- `--start-time` e `--end-time` são em segundos; o exemplo cobre os últimos 7 dias. Repita o `get-query-results` até `status` ser `Complete`.
- Para outro ambiente, troque `-dev` nos três grupos e na expressão do `parse @log`.
- O campo `@ptr` de cada resultado é um identificador em base64 que contém o ID da conta. Não copie a saída para arquivo versionado.

## Rastreamento no X-Ray

O `Globals` liga `Tracing: Active` nas três funções, e o SAM anexa a política gerenciada `AWSXrayWriteOnlyAccess` à role de cada uma. Cada invocação amostrada vira um trace no X-Ray. O trace começa na função: a API é uma HTTP API do API Gateway, que não envia trace ao X-Ray.

O Powertools Tracing cria os subsegmentos da aplicação, com o prefixo `##`, e o instrumentor do SDK cria um subsegmento por chamada à AWS:

| Função | Subsegmentos | Anotação |
|---|---|---|
| `registrar-avaliacao` | `## gravarAvaliacao`, com `DynamoDB` dentro | `urgencia` |
| `notificar-critica` | `## publicarAviso`, com `SNS` dentro | `id` da avaliação |
| `gerar-relatorio-semanal` | `## lerAvaliacoes`, com um `DynamoDB` para cada `Query`, uma por dia da janela; `## gravarRelatorio`, com `S3`; `## publicarAviso`, com `SNS` | `dataReferencia` |
| as três, depois de uma restauração | `Restore` | |

- **Sem dado do aluno no trace:** as anotações são só urgência, identificador e data de referência. No `PutItem`, o instrumentor só captura o nome da tabela. No QA de add-registro-avaliacao, 81 traces de dev foram lidos sem nenhuma ocorrência dos marcadores das descrições ([QA de add-registro-avaliacao](../openspec/changes/archive/2026-11-11-add-registro-avaliacao/qa.md), seção 6.7). O limite conhecido é a mensagem de exceção do SDK, que o instrumentor grava no subsegmento quando uma chamada falha; está no registro de riscos de [docs/seguranca.md](seguranca.md), junto com o X-Ray SDK em modo de manutenção.
- **Amostragem:** vale a regra padrão do X-Ray, então uma invocação pode ficar sem trace. Na medição da seção 6.15 do QA de add-observabilidade-seguranca, todos os traces foram amostrados.

### Abrir o trace de uma invocação

1. Ache a linha da invocação no grupo de log da função, pelo console ou com o `filter-log-events` da seção Logs, acima. Toda linha da aplicação traz `xray_trace_id`, no formato `1-<8 hexadecimais>-<24 hexadecimais>`.
2. No console do CloudWatch, em `sa-east-1`, abra X-Ray traces, Traces, e busque pelo ID. Ou leia pela CLI:

Registro: leitura de um trace de prod pelo `xray_trace_id` da linha `Relatório gerado`, em 10 e 11/11/2026 (UTC), na conferência deste documento ([openspec/changes/archive/2026-11-11-add-documentacao-entrega/qa.md](../openspec/changes/archive/2026-11-11-add-documentacao-entrega/qa.md)). O trace trouxe `## lerAvaliacoes` com sete `DynamoDB`, `## gravarRelatorio` com `S3`, `## publicarAviso` com `SNS` e `Restore`.

```bash
aws xray batch-get-traces \
  --trace-ids "<ID_DO_TRACE>" \
  --region sa-east-1 --profile tc4-admin
```

Cada segmento vem como texto JSON no campo `Document`, com os subsegmentos, os tempos de início e fim e as anotações. O documento traz ARNs com o ID da conta: não copie para arquivo versionado.

### Medição da primeira chamada depois da restauração

O X-Ray foi o instrumento da medição que justificou a memória de 1024 MB e o aquecimento dos clientes do SDK antes do snapshot. A medida foi a duração do subsegmento do serviço no trace da primeira invocação de um ambiente restaurado, três restaurações por função:

- No registro e na notificação, a primeira chamada ao serviço caiu de cerca de 4,3 s para 166 a 258 ms. No relatório, a primeira `Query` caiu de 2,6 a 3,1 s para 128 a 163 ms. As 15 chamadas medidas ficaram entre 105 e 339 ms, abaixo da meta de metade do tempo máximo de cada cliente.
- A duração da restauração não mudou de faixa: de 528,92 a 943,97 ms.
- O aquecimento soma de 1,6 a 1,8 s à publicação de cada versão. Esse tempo sai da primeira invocação e fica na linha `Cliente aquecido` do log da publicação.

Memória e aquecimento entraram no mesmo deploy, então o resultado é o efeito somado dos dois. Os números completos estão no [QA de add-observabilidade-seguranca](../openspec/changes/archive/2026-11-11-add-observabilidade-seguranca/qa.md), seção 6.15, e o custo do aquecimento na seção 6.4 e na observação P. Como o aquecimento funciona está em [docs/funcoes.md](funcoes.md).

## Teste de alarme

Há dois testes, e nenhum altera o `$LATEST`, a configuração ou o código das funções. Os dois usam os alarmes que já existem.

**Os dois geram e-mail real** para o endereço assinante do tópico de alarmes, um por alarme que entrar em ALARM. Teste um alarme por vez e espere a volta a OK antes do próximo.

| Teste | O que prova | Volta a OK observada | Onde rodou |
|---|---|---|---|
| `set-alarm-state` | Ação do alarme, política do tópico e assinatura | 60 a 80 s em dev; 4 s em hml | dev, os três alarmes, 06/11/2026 (seção 6.7); hml, um alarme, 07/11/2026 (seção 8.1) |
| `put-log-events` num stream de teste | O caminho de uma falha real: filtro, métrica, alarme e e-mail | 10 a 11 minutos depois da linha | dev, janelas A, B e A2 de 06/11/2026 (seções 6.8 a 6.10) e seção 8.2 de 07/11/2026 |

### 1. Do alarme ao e-mail, com `set-alarm-state`

Força o estado do alarme, sem passar pela métrica.

Registro: rodou em dev nos três alarmes em 06/11/2026 ([QA de add-observabilidade-seguranca](../openspec/changes/archive/2026-11-11-add-observabilidade-seguranca/qa.md), seção 6.7) e em hml no alarme do registro em 07/11/2026 (mesmo QA, seção 8.1, e [QA de add-fundacao-projeto](../openspec/changes/archive/2026-11-11-add-fundacao-projeto/qa.md), seção "Release 1.0.0 em hml", item 4).

```bash
aws cloudwatch set-alarm-state \
  --alarm-name feedback-falha-registrar-avaliacao-dev \
  --state-value ALARM \
  --state-reason "Teste manual do caminho do alarme ate o e-mail" \
  --region sa-east-1 --profile tc4-admin
```

- Na avaliação seguinte, o alarme recalcula pela métrica. Sem falha na janela, volta a OK sozinho: de 60 a 80 s em dev e 4 s em hml. A volta não gera e-mail.
- O widget de falhas do dashboard continua vazio, porque a métrica não recebeu ponto. Só o widget de alarmes muda.
- O motivo vai no e-mail. Use texto sem acento e sem dado do aluno.

### 2. Da linha de log ao alarme, com `put-log-events`

Grava uma linha com o marcador num stream de teste dentro do grupo da função. O filtro vale para todos os streams do grupo, então a linha percorre o mesmo caminho de uma falha real.

Rode a partir de uma pasta fora do repositório, porque o arquivo do evento fica nela. O exemplo usa o grupo do registro em dev; para as outras funções, troque o nome do grupo.

Registro: os mesmos comandos rodaram em dev em 06/11/2026, nos três grupos ([QA de add-observabilidade-seguranca](../openspec/changes/archive/2026-11-11-add-observabilidade-seguranca/qa.md), seções 6.8 a 6.10, e "Linhas de teste usadas"), e em 07/11/2026, no grupo do registro (mesmo QA, seção 8.2). O QA gravou a linha completa da aplicação, com todos os campos do layout; o exemplo abaixo usa uma linha reduzida com o mesmo marcador, que o filtro casa do mesmo jeito.

```bash
GRUPO=/aws/lambda/feedback-registrar-avaliacao-dev
STREAM=qa-observabilidade-$(date +%Y%m%d)

aws logs create-log-stream \
  --log-group-name "$GRUPO" \
  --log-stream-name "$STREAM" \
  --region sa-east-1 --profile tc4-admin

AGORA=$(( $(date +%s) * 1000 ))

cat > evento-alarme.json <<EOF
[
  {
    "timestamp": $AGORA,
    "message": "{\"level\":\"ERROR\",\"message\":\"Teste de alarme\",\"function_request_id\":\"qa-observabilidade-manual\",\"alarme\":\"FALHA_FUNCAO\"}"
  }
]
EOF

AWS_CLI_FILE_ENCODING=UTF-8 aws logs put-log-events \
  --log-group-name "$GRUPO" \
  --log-stream-name "$STREAM" \
  --log-events file://evento-alarme.json \
  --region sa-east-1 --profile tc4-admin
```

- **Stream de teste:** o nome segue `qa-observabilidade-<aaaammdd>`. Se o stream do dia já existir, o `create-log-stream` falha com `ResourceAlreadyExistsException`; siga direto para o `put-log-events`.
- **Codificação no Windows:** o AWS CLI lê o arquivo de `file://` em cp1252. O `AWS_CLI_FILE_ENCODING=UTF-8` na frente do `put-log-events` mantém os acentos. Sem ele, uma linha com acento, como `Falha ao registrar avaliação`, chega ao grupo com dupla codificação (observação L do QA).
- **Horário atual:** o `timestamp` é o momento do envio, em milissegundos. Uma linha com horário antigo cai fora da janela de 5 minutos e não dispara; uma com horário futuro deixa o alarme em ALARM até aquele horário.
- **Sem dado do aluno:** a linha não pode ter descrição, nota nem corpo de mensagem. O `function_request_id` com o nome do teste identifica a linha para quem ler o grupo depois.
- **Tempo esgotado:** para testar esse caminho, troque `message` por uma linha de texto com `Task timed out`, como `qa-observabilidade-manual Task timed out after 10.01 seconds`.
- **Tempos:** o alarme entra em ALARM cerca de 1 minuto depois e volta a OK de 10 a 11 minutos depois do minuto da linha. A linha aparece também no widget de falhas do dashboard.
- **O stream não é apagado.** Ele expira com a retenção do grupo: 14 dias em dev e hml, 30 em prod. Apague o arquivo `evento-alarme.json` ao terminar.
- **Só em dev.** Este teste nunca rodou em hml nem em prod. Em prod, a linha ficaria 30 dias no grupo e apareceria no dashboard; o [roteiro do vídeo](roteiro-video.md) usa só um `set-alarm-state`.

## Limites do nível gratuito

O nível gratuito do CloudWatch cobre 10 métricas de alarme e 3 dashboards. Com os três ambientes no ar, a conta fecha nesses números, sem folga:

| Recurso | Por stack | dev, hml e prod | Já existia na conta | Total |
|---|---|---|---|---|
| Métricas de alarme | 3 | 9, em `sa-east-1` | 1, o alarme de cobrança em `us-east-1` | **10 de 10** |
| Dashboards | 1 | 3 | 0 | **3 de 3** |

A conta foi lida em 10 e 11/11/2026 (UTC): 9 alarmes de métrica em `sa-east-1`, 1 em `us-east-1`, nenhum alarme composto e os dashboards `feedback-painel-dev`, `feedback-painel-hml` e `feedback-painel-prod`. Em 06/11/2026, só com dev, a conta usava 4 de 10 e 1 de 3 (QA de add-observabilidade-seguranca, "Contagem de alarmes e dashboards na conta"). A página de preços não diz se o limite é por região ou por conta, então a conta é feita no pior caso, somando o alarme de cobrança.

Registro: a contagem rodou em 06/11/2026 ([QA de add-observabilidade-seguranca](../openspec/changes/archive/2026-11-11-add-observabilidade-seguranca/qa.md), seção "Contagem de alarmes e dashboards na conta") e em 10 e 11/11/2026 (UTC), na conferência deste documento ([openspec/changes/archive/2026-11-11-add-documentacao-entrega/qa.md](../openspec/changes/archive/2026-11-11-add-documentacao-entrega/qa.md)).

```bash
for r in sa-east-1 us-east-1; do
  aws cloudwatch describe-alarms --region "$r" --profile tc4-admin \
    --query "MetricAlarms[].[AlarmName,StateValue]" --output text
done

aws cloudwatch list-dashboards --region sa-east-1 --profile tc4-admin \
  --query "DashboardEntries[].DashboardName" --output text
```

Esperado com os três ambientes: 9 alarmes `feedback-falha-*` em `sa-east-1`, o de cobrança em `us-east-1` e 3 dashboards.

**Regras que decorrem da conta:**

- **Não crie alarme de teste**, nem temporário, e nenhum dashboard a mais. Qualquer um sai do nível gratuito. Para testar, use os alarmes que existem, como na seção Teste de alarme, acima.
- Não troque a métrica de um alarme por matemática de métricas nem por consulta Metrics Insights. A primeira cobra cada métrica da expressão, e a segunda sai do nível gratuito.
- Não use período abaixo de 60 s, que é alta resolução e também sai do nível gratuito.
- hml pode ser removido depois da entrega, em 13/11/2026, e prod fica no ar até a divulgação da nota da FIAP. Cada remoção libera 3 métricas de alarme e 1 dashboard.

**Outros limites que o projeto usa:**

| Item | Limite gratuito | Uso conhecido |
|---|---|---|
| Métricas personalizadas | 10 | Até 10 por stack: 3 de filtro e 7 do Powertools. A cobrança é por hora com dado, e as de filtro só recebem dado quando há falha ou linha de teste. A página não detalha como a franquia se aplica a esse rateio |
| Logs | 5 GB de ingestão, armazenamento e dados lidos pelo Logs Insights | A consulta do dashboard leu 44 KB no QA de dev |
| Métricas por dashboard | 50 | 12 |
| Lambda | 400.000 GB-s e 1 milhão de requisições por mês | 235 GB-s no mês, conferidos pelo Gabriel no console do nível gratuito em 07/11/2026 ([tarefas de add-observabilidade-seguranca](../openspec/changes/archive/2026-11-11-add-observabilidade-seguranca/tasks.md), 6.19) |

O risco de ficar sem folga está no registro de riscos aceitos de [docs/seguranca.md](seguranca.md), como "Nível gratuito do CloudWatch sem folga". O alarme de cobrança que já existia na conta é a mitigação.
