# Design: add-observabilidade-seguranca

## Context

As três funções já gravam log em JSON com retenção de 14 dias, têm X-Ray ativo, emitem métricas do Powertools e rodam sob o boundary do ambiente. Este change acrescenta o que transforma isso em aviso e em visão: tópico de alarmes, um alarme de falha por função, um dashboard por ambiente, o aquecimento dos clientes do SDK antes do snapshot e o fim do WARN de métrica vazia. Por decisão do Gabriel de 05/11/2026, entram também a memória de 1024 MB, o `sam build` no CI e a retenção de log por ambiente.

Pendências herdadas dos QAs que este design resolve:
- Primeira chamada ao SDK depois da restauração: 4.259 ms no DynamoDB do registro e 4.301 ms no SNS da notificação (`add-registro-avaliacao`, observação I; `add-notificacao-critica`, observação I).
- Falha tratada fora de `Errors`: o 500 do registro e o lote parcial da notificação terminam a invocação sem erro de função (`add-notificacao-critica`, observação H).
- `No metrics were emitted` em WARN a cada invocação bem-sucedida da notificação (observação K).
- O CloudWatch não publica no tópico de administradores, que usa `alias/aws/sns` (fundação, Risks).
- Retenção por ambiente, deixada para este change em `add-registro-avaliacao`, D11.

**Dependência.** `add-relatorio-semanal` ainda não está na `develop`. Este design foi feito sobre o código e o `template.yaml` da branch `feature/add-relatorio-semanal` (commit `19ebb87`), que traz as três funções. A implementação só começa depois do merge dele (tasks 1.2 e 1.3).

**Fontes.** Páginas consultadas em 05/11/2026. "Verificado na conta" significa comando só de leitura com o perfil `tc4-admin` em `sa-east-1`, em 05/11/2026: `describe-type`, `get-policy-version`, `test-metric-filter`, `start-query` do Logs Insights, `describe-alarms`, `list-dashboards`, `list-metrics` e `get-role`. Nada foi criado nem alterado na conta. "Verificado localmente" significa `sam validate --lint` com o SAM CLI 1.171.0 numa cópia temporária do `template.yaml` de `19ebb87` com os trechos deste design, e leitura do bytecode com `javap` do Powertools 2.11.0 e do X-Ray SDK 2.21.1 que estão no repositório Maven local. Tempos de CI e deploy vêm da API do GitHub Actions (execuções 42112984375 e 42113144132).

## Goals / Non-Goals

**Goals**
- Toda falha de função, tratada ou não, chega por e-mail em poucos minutos, sem alarme falso por entrada rejeitada ou falta de tráfego
- Um dashboard por ambiente com o que o vídeo precisa: invocações, falhas, duração, restauração do SnapStart e estado dos alarmes
- Primeira chamada ao SDK depois da restauração com folga clara para os limites dos clientes
- Tudo dentro do nível gratuito com dev, hml e prod implantados juntos
- Nenhuma ação nova de deploy no bootstrap; só uma leitura nova no boundary
- Erro de empacotamento pego no PR, e não só no deploy
- Retenção maior em prod sem custo relevante

**Non-Goals**
- Os itens de "Fora do escopo" da proposta: KMS gerenciado pelo cliente, WAF, GuardDuty e afins, trilha do CloudTrail, alarme de retorno a OK, alarme composto, anomalia, Synthetics, métricas de negócio no dashboard
- Alterar o `$LATEST`, a configuração ou o código das funções na AWS fora do pipeline, inclusive para provar falha
- Remover ou renomear as métricas do Powertools que já existem
- Testes automatizados que leiam o `template.yaml`

## Decisões do Gabriel de 05/11/2026

| # | Decisão | Onde entra |
|---|---|---|
| 1 | Alarmes vão para o mesmo endereço de `EmailAdmin`, em um tópico de alarmes por ambiente | D1 |
| 2 | No máximo 3 métricas de alarme e 1 dashboard por stack | D3, D5, D7, D8 |
| 3 | Métricas separadas por ambiente neste change | D6 |
| 4 | Primeira chamada depois da restauração abaixo de 4 s no registro, 2,5 s na notificação e 5 s no relatório, em ao menos duas restaurações por função, como **meta**, não como critério de bloqueio | D13 |
| 5 | Proibido alterar o `$LATEST` ou o código das funções na AWS para provar falha. Filtro, métrica e alarme são provados com `aws logs put-log-events` num log stream de teste de cada grupo, e o caminho do alarme até o e-mail com `aws cloudwatch set-alarm-state` | D14 |
| 6 | `MemorySize: 1024` no `Globals`, com nova medição da primeira chamada e da restauração | D15 |
| 7 | `sam build` no CI, depois do build Maven | D16 |
| 8 | Retenção de log de 30 dias em prod e 14 em dev e hml | D17 |

## Decisions

### D1. Tópico de alarmes por ambiente, sem criptografia em repouso
`TopicoAlarmes`, nome `feedback-alarmes-<amb>`, sem `KmsMasterKeyId`, com assinatura `email` para `EmailAdmin` (decisão 1), numa `AWS::SNS::Subscription` separada, como no tópico de administradores. Nenhum secret novo no GitHub.

- **Por que sem KMS:** decisão do Gabriel de 05/11/2026 (`project.md`). O CloudWatch não publica em tópico com `alias/aws/sns`, a chave do cliente tem custo mensal, e a mensagem de alarme não carrega dado do aluno: a métrica é uma contagem (D3), e o [esquema da notificação](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Notify_Users_Alarm_Changes.html) traz nome, descrição, motivo, métrica e limiar.
- **Nome:** casa com `feedback-*-<amb>`, que `TopicosDoAmbiente` do bootstrap já cobre.

Alternativas descartadas:
- Publicar no tópico de administradores. O CloudWatch não consegue, por causa da chave.
- AWS User Notifications. Mais um serviço para configurar e explicar, e o e-mail já é o canal do projeto.
- Endereço próprio para alarmes. Exigiria parâmetro, secret e ajuste no `deploy.yml`, sem ganho para um projeto de uma pessoa.

### D2. Política do tópico de alarmes
`AWS::SNS::TopicInlinePolicy` com dois statements (texto em "Alterações no template.yaml"):

1. **`NegaAcessoSemTls`:** `Deny` para `Principal: "*"` com `aws:SecureTransport` igual a `false`, sobre as 11 ações que a [referência do SNS](https://docs.aws.amazon.com/sns/latest/dg/sns-access-policy-language-api-permissions-reference.html) lista como válidas em política de tópico. A lista é explícita porque a página não documenta curinga em política de tópico, e o exemplo de [boas práticas](https://docs.aws.amazon.com/sns/latest/dg/sns-security-best-practices.html) nega só `SNS:Publish`, menos do que a spec pede.
2. **`PublicacaoPelosAlarmesDoAmbiente`:** `Allow` de `sns:Publish` para `cloudwatch.amazonaws.com`, com `aws:SourceAccount` igual à conta e `aws:SourceArn` em `ArnLike` com `arn:<partição>:cloudwatch:<região>:<conta>:alarm:feedback-*-<amb>`. É o formato que a [página de alarmes](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Notify_Users_Alarm_Changes.html) recomenda contra confused deputy, restrito aos alarmes do próprio ambiente: um alarme de hml não publica no tópico de dev.

A política tem `Id`, que a referência do SNS exige único por política. O acesso do deploy e do administrador continua pela política de identidade, porque é a mesma conta.

Alternativas descartadas:
- `AWS::SNS::TopicPolicy`. Funciona igual, mas o handler não tem `read`, então não há detecção de desvio. Verificado na conta: o handler de `TopicInlinePolicy` exige `sns:SetTopicAttributes` e `sns:GetTopicAttributes`, que o bootstrap já concede.
- `sns:*` no `Deny`. Não documentado para política de tópico; uma recusa só apareceria no deploy.
- Só `aws:SourceAccount`. Permitiria publicação por alarme de outro ambiente da mesma conta.

### D3. Uma métrica de falha por função, gerada por filtro de log
Cada função tem um `AWS::Logs::MetricFilter` no próprio grupo de log, com o padrão:

```
?FALHA_FUNCAO ?"Task timed out" ?"Status: timeout"
```

O filtro publica 1 por linha que casa, na métrica `Falhas<Função>` do namespace `FeedbackServerless/<amb>`, sem dimensão e sem `DefaultValue`. É a única métrica de cada alarme. O filtro vale para todos os log streams do grupo, o que permite a prova da D14.

- `FALHA_FUNCAO` é o marcador que o handler grava nas falhas (D4). Cobre o 500 do registro, a falha de publicação da notificação, inclusive parcial, o descarte de item inválido e a execução com falha do relatório.
- `"Task timed out"` e `"Status: timeout"` cobrem o tempo esgotado, que congela o processo antes de qualquer `catch`. A [telemetria do Lambda](https://docs.aws.amazon.com/lambda/latest/dg/telemetry-schema-reference.html) documenta `timeout` como valor de `status` do `platform.report`, mas não o texto da linha `REPORT` do log em texto. Material de suporte da AWS mostra os dois formatos: a linha antiga `<id> Task timed out after N seconds` e o `Status: timeout` no fim do `REPORT`. Os dois entram no padrão. Como o tempo esgotado não é provocado de verdade (decisão 5), o texto real do runtime `java25` fica sem prova (Risks).

**Verificado na conta** com `test-metric-filter`: de 10 linhas, casaram só a falha marcada, a linha `Task timed out` e o `REPORT` com `Status: timeout`. Não casaram a linha de sucesso, a data rejeitada em ERROR, o EMF de `ErroInterno`, o `REPORT` comum com restauração, o `RESTORE_REPORT` e o WARN de métrica vazia. O teste também mostrou que a busca por termo é por substring: `FALHA_FUNCAOX` casou.

| Opção | Métricas de alarme por função | Falha tratada | Tempo esgotado | Motivo |
|---|---|---|---|---|
| `Errors` do Lambda | 1 | não | sim | O 500 do registro e o lote parcial não são erro de função |
| Métrica EMF emitida no `catch` | 1 | sim | não | O tempo esgotado não passa pelo `catch` |
| Matemática `Errors` + métrica EMF | 2 | sim | sim | A [página de preços](https://aws.amazon.com/cloudwatch/pricing/) cobra cada métrica da expressão: 18 métricas de alarme na conta |
| Filtro em `level = ERROR` | 1 | sim | não | Padrão JSON não lê a linha de plataforma, que é texto. E pegaria a data rejeitada do relatório, que é ERROR |
| Alarme com consulta Metrics Insights | 1 | depende | depende | A página de preços tira do nível gratuito os alarmes com Metrics Insights |
| **Filtro por marcador e linhas de tempo esgotado** | **1** | **sim** | **sim** | Adotada |

A métrica do filtro é cobrada como [métrica personalizada](https://docs.aws.amazon.com/AmazonCloudWatchLogs/latest/APIReference/API_PutMetricFilter.html). Sem `DefaultValue`, ela só recebe dado quando há falha.

### D4. Marcador de falha nos handlers
O marcador é o campo `alarme` com valor `FALHA_FUNCAO`, acrescentado como as outras entradas estruturadas (`entry("alarme", "FALHA_FUNCAO")`), com uma constante por handler. Ele entra só nas linhas que representam falha da função:

| Função | Linha | Nível | Marcador |
|---|---|---|---|
| registrar-avaliacao | `Falha ao registrar avaliação` | ERROR | sim |
| registrar-avaliacao | `Entrada rejeitada`, `Corpo rejeitado` | INFO | não |
| notificar-critica | `Falha ao publicar aviso` | ERROR | sim |
| notificar-critica | `Item inválido descartado` | ERROR | sim (proposta, D10) |
| gerar-relatorio-semanal | `Falha ao gerar relatório` | ERROR | sim |
| gerar-relatorio-semanal | `Data de referência rejeitada` | ERROR | **não**: a spec diz que data inválida não dispara alarme |
| as três | `Error` que escapa do handler (novo) | ERROR | sim, e relança |
| as três | `Falha ao emitir métrica` | WARN | não: não é falha da função |
| as três | falha no aquecimento (D9) | WARN | não: ocorre no deploy |

**`Error` que escapa.** Os `catch` atuais pegam `RuntimeException`. Um `NoClassDefFoundError`, como o do incidente de 04/11/2026, ou um `OutOfMemoryError` terminariam a invocação sem marcador. Cada handler ganha um `catch (Error e)` no ponto mais externo da invocação, que registra em ERROR só a classe, com o marcador, e relança. O comportamento para quem chama não muda.

**Quem prova o quê.** Que cada caminho de falha grava a linha com o marcador, e que os caminhos sem falha e as rejeições não gravam, é provado pelos testes unitários com appender em memória (tasks 3.3, 3.5 e 3.7). Que a linha vira métrica, alarme e e-mail é provado na stack pela D14. As duas provas juntas cobrem o caminho inteiro sem provocar falha real.

**Por que o marcador não pode ser forjado:** a busca é por substring, mas nenhum dado de entrada do aluno vai para log, em nível nenhum (`add-registro-avaliacao`, D9). Nenhum texto enviado pelo aluno chega ao grupo de log.

Alternativas descartadas:
- Casar pela mensagem (`Falha ao ...`). Tem acento e está sujeita a mudança de redação; o marcador é um contrato explícito e testado.
- Rebaixar `Data de referência rejeitada` para INFO e filtrar por ERROR. Mudaria o relatório sem pedido e ainda não resolveria o tempo esgotado.

### D5. Definição dos alarmes
Três `AWS::CloudWatch::Alarm`, um por função:

| Propriedade | Valor | Motivo |
|---|---|---|
| `AlarmName` | `feedback-falha-<função>-<amb>` | Convenção do bootstrap; casa com `AlarmesDoAmbiente` e com o `aws:SourceArn` da D2 |
| Métrica | `FeedbackServerless/<amb>`, `Falhas<Função>`, sem dimensão | D3 |
| `Statistic`, `Threshold`, `ComparisonOperator` | `Sum`, 1, `GreaterThanOrEqualToThreshold` | Uma falha basta (proposta, D2) |
| `Period` | 60 | Resolução padrão. Alta resolução, abaixo de 60, fica fora do nível gratuito |
| `EvaluationPeriods`, `DatapointsToAlarm` | 5 e 1 | Uma falha em qualquer minuto de uma janela de 5 minutos. A janela dá margem para o ponto do filtro chegar depois do minuto em que a falha ocorreu |
| `TreatMissingData` | `notBreaching` | Sem invocação não há falha (proposta, D3) |
| `AlarmActions` | só `TopicoAlarmes` | Sem `OKActions` nem `InsufficientDataActions`: retorno a OK está fora do escopo |

O alarme volta a OK sozinho cerca de 5 minutos depois da última falha, porque minuto sem dado conta como "sem falha". Por isso nem a falha real nem a linha de teste da D14 deixam o alarme preso em ALARM, e cada novo episódio gera um novo e-mail.

Alternativas descartadas:
- `Period: 300` com uma avaliação. Mesmo efeito de janela, mas o gráfico de falhas do dashboard ficaria em blocos de 5 minutos.
- `EvaluationPeriods: 1` com `Period: 60`. Um ponto atrasado poderia cair fora da janela, e a falha passaria sem aviso.
- `TreatMissingData: missing` ou `breaching`. Com `missing`, o alarme ficaria em ALARM depois de uma falha até chegar um novo ponto, o que, sem `DefaultValue`, só acontece na próxima falha. Com `breaching`, ficaria em ALARM sem tráfego.

### D6. Métricas separadas por ambiente: namespace por ambiente (decisão 3)
`POWERTOOLS_METRICS_NAMESPACE` passa de `FeedbackServerless` para `!Sub FeedbackServerless/${Stage}` no `Globals`. As métricas de filtro da D3 usam o mesmo namespace.

- **Custo igual ao da dimensão:** uma métrica é a combinação de namespace, nome e dimensões. Namespace por ambiente ou dimensão `Ambiente` geram exatamente o mesmo número de métricas personalizadas.
- **Sem código:** o Powertools lê o namespace da variável. Os handlers não mudam por causa disso, e não há como esquecer a dimensão numa métrica nova.
- **Caracteres:** os validadores do Powertools 2.11.0 e do EMF aceitam `^[a-zA-Z0-9._#:/-]+$`. Verificado localmente no bytecode.
- **Métricas antigas:** as de `FeedbackServerless` param de receber dado e deixam de gerar custo.

Alternativa descartada: dimensão `Ambiente` com `withDefaultDimension` nos três handlers. Mesmo custo, código em três lugares e um teste a mais por função.

### D7. Dashboard por ambiente
`AWS::CloudWatch::Dashboard`, nome `feedback-painel-<amb>`, com cinco widgets:

| Widget | Fonte | Isolamento |
|---|---|---|
| Alarmes de falha | Estado dos três alarmes, por ARN | Alarmes da stack |
| Invocações | `AWS/Lambda` `Invocations`, `Sum`, 300 s, por `FunctionName` | Nome da função tem o sufixo |
| Falhas, tratadas e de execução | `FeedbackServerless/<amb>` `Falhas<Função>`, `Sum`, 60 s | Namespace do ambiente |
| Duração | `AWS/Lambda` `Duration`, média e máximo, 300 s | Nome da função |
| Restaurações do SnapStart | Logs Insights, tabela | Grupos de log do ambiente |

São 12 métricas, abaixo das 50 por dashboard do nível gratuito.

**Restauração pelo log.** Não há métrica do CloudWatch para a duração da restauração (`add-registro-avaliacao`, D6). A consulta lê só a linha de plataforma `RESTORE_REPORT`:

```
filter @message like /^RESTORE_REPORT/
| parse @message 'Restore Duration: * ms' as restauracaoMs
| parse @log /feedback-(?<funcao>[a-z-]+)-<amb>$/
| display @timestamp, funcao, restauracaoMs
| sort @timestamp desc | limit 50
```

**Verificado na conta** sobre os grupos de dev dos últimos 3 dias: 6 restaurações, de 403 a 903 ms, com o nome da função extraído e 155 KB lidos. Duas armadilhas encontradas no teste: `fields` depois do `parse` com grupo nomeado dá `MalformedQueryException` (campo efêmero repetido), por isso `display`; e `@log` traz o ID da conta, por isso só o nome da função é exibido.

**Sem dado do aluno:** nenhum widget lê linha da aplicação. A consulta filtra só `RESTORE_REPORT`, e o resto são contagens e durações. As linhas de teste da D14 não começam com `RESTORE_REPORT` e não entram na tabela.

**Custo de visualização:** pela página de preços, o `GetMetricData` que desenha o dashboard original não é cobrado. A consulta do Logs Insights roda a cada carga e conta nos 5 GB gratuitos de dados lidos; o volume medido é desprezível.

Alternativas descartadas:
- Métrica de restauração por filtro de log. Mais três métricas personalizadas por stack.
- Um widget de log por função. Três consultas por carga para o mesmo resultado.
- `Errors` do Lambda no dashboard. Não mostra a falha tratada, e o widget de falhas já cobre os dois tipos.

### D8. Custo e nível gratuito (decisão 2)
Números da [página de preços do CloudWatch](https://aws.amazon.com/cloudwatch/pricing/) e da [página de preços do Lambda](https://aws.amazon.com/lambda/pricing/), consultadas em 05/11/2026:

| Item do nível gratuito | Limite | Observação da página |
|---|---|---|
| Métricas de alarme | 10 | Só alarmes de resolução padrão que listam métricas diretamente, sem Metrics Insights. Alarme com matemática cobra cada métrica da expressão |
| Dashboards | 3, com até 50 métricas cada | |
| Métricas personalizadas | 10 | Soma de personalizadas e de monitoramento detalhado. Cobrança rateada por hora, só nas horas com dado |
| Requisições de API do CloudWatch | 1 milhão | Exceto `GetMetricData`, `GetInsightRuleReport` e `GetMetricWidgetImage` |
| Logs | 5 GB | Ingestão, armazenamento e dados lidos pelo Logs Insights |
| Filtros de métrica | sem item próprio | A métrica gerada é cobrada como personalizada ([PutMetricFilter](https://docs.aws.amazon.com/AmazonCloudWatchLogs/latest/APIReference/API_PutMetricFilter.html)) |
| Lambda | 1 milhão de requisições e 400.000 GB-s por mês | SnapStart não tem cobrança de cache nem de restauração nos runtimes Java gerenciados |

| Recurso | Por stack | Dev, hml e prod | Já existe na conta | Total |
|---|---|---|---|---|
| Métricas de alarme | 3 | 9 | 1 (alarme de cobrança em `us-east-1`) | **10 de 10** |
| Dashboards | 1 | 3 | 0 | **3 de 3** |
| Métricas no dashboard | 12 | | | 12 de 50 por dashboard |
| Métricas personalizadas distintas | até 10: 3 de filtro e 7 do Powertools | até 30 | 2, no namespace antigo | ver abaixo |
| Memória por invocação | 1 GB (D15) | | | ver abaixo |
| Retenção de log | 14 dias; 30 em prod (D17) | | | dentro dos 5 GB |

**Verificado na conta:** há um alarme de cobrança (`AWS/Billing`, `EstimatedCharges`) em `us-east-1`, criado em 04/11/2026, e nenhum dashboard. A página não diz se o limite vale por região ou por conta. O design assume o pior caso, por conta, e fecha em 10 de 10, **sem folga**: qualquer alarme a mais, inclusive de teste, sai do nível gratuito. A prova da D14 não cria alarme.

**Métricas personalizadas:** o total distinto passa de 10 com as três stacks, mas a cobrança é por hora com dado. As 3 de filtro só recebem dado quando há falha ou linha de teste. As 7 do Powertools (registro: `AvaliacaoRegistrada`, `EntradaRejeitada`, `ErroInterno`; notificação: `ItemInvalido`; relatório: `RelatorioGerado`, `DataReferenciaRejeitada`, `ErroInterno`) só nas horas com invocação. Com o tráfego da demonstração, a soma de métrica-hora fica muito abaixo do equivalente a 10 métricas no mês inteiro. A página não detalha como o nível gratuito se aplica ao rateio, então isso é estimativa, registrada em Risks.

**Lambda com 1024 MB:** cada segundo cobrado passa a valer 1 GB-s em vez de 0,5. Verificado na conta: no `REPORT`, o `Billed Duration` soma a duração da invocação e o `Billed Restore Duration` (por exemplo, 7.156,70 ms de duração e 210 ms de restauração cobrada deram 7.367 ms), então a restauração também conta. Mesmo no pior caso observado, cerca de 8 s por invocação, seriam 50.000 invocações por mês para chegar aos 400.000 GB-s. O volume da demonstração fica abaixo de 1% da franquia.

**Logs com 30 dias em prod:** prod só existe até o vídeo e recebe pouco tráfego. Os grupos de dev somavam centenas de KB em 05/11/2026; dobrar a retenção em prod não chega perto dos 5 GB.

### D9. Aquecimento dos clientes antes do snapshot
Cada função ganha uma classe `Aquecimento`, no próprio módulo, que implementa `org.crac.Resource`. O `beforeCheckpoint` faz uma chamada de leitura por cliente, sem gravar nada. O `afterRestore` fica vazio.

| Função | Cliente | Chamada | Permissão | Nova? |
|---|---|---|---|---|
| registrar-avaliacao | DynamoDB | `DescribeTable` na tabela | `dynamodb:DescribeTable` na tabela | sim, na role; o boundary já tem |
| notificar-critica | SNS | `GetTopicAttributes` no tópico de administradores | `sns:GetTopicAttributes` no tópico | sim, na role e no boundary |
| gerar-relatorio-semanal | DynamoDB | `Query` no índice `por-data` com `dataEnvio = "aquecimento"` e `Limit` 1, partição que nunca existe | `dynamodb:Query` no índice | não |
| gerar-relatorio-semanal | S3 | `HeadBucket` no bucket de relatórios | `s3:ListBucket` no bucket, [exigido pelo HeadBucket](https://docs.aws.amazon.com/AmazonS3/latest/API/API_HeadBucket.html) | sim, na role; o boundary já tem |
| gerar-relatorio-semanal | SNS | `GetTopicAttributes` no tópico de administradores | `sns:GetTopicAttributes` no tópico | sim, na role e no boundary |

Nenhuma dessas chamadas devolve descrição ou nota: `DescribeTable` e `HeadBucket` trazem metadados, `GetTopicAttributes` traz atributos do tópico, e a `Query` lê uma partição vazia.

**Regras de implementação:**
- **Mesmos clientes:** o `Aquecimento` recebe as mesmas instâncias que o handler usa. No relatório, os três clientes saem de dentro da construção do `GeradorRelatorio` para campos de `Recursos`, compartilhados pelo gerador e pelo aquecimento.
- **Registro e referência forte:** a [página de runtime hooks](https://docs.aws.amazon.com/lambda/latest/dg/snapstart-runtime-hooks-java.html) avisa que o contexto guarda só `WeakReference`. O `Aquecimento` fica em campo `static final` de `Recursos` e é registrado ali com `Core.getGlobalContext().register(...)`, depois de criar os clientes. Como `Recursos` só é carregada pelo construtor público, os testes do handler não registram nada. O construtor do `Aquecimento` não registra a si mesmo, para o teste poder chamá-lo direto.
- **Falha não derruba a publicação:** cada chamada fica no próprio `try`, que pega `RuntimeException` e `LinkageError`. A falha de um cliente não impede os outros. O log é WARN `Falha no aquecimento`, com `servico`, `classe` e, se vier do serviço, `codigoErro`, sem mensagem nem pilha, como na D9 do registro. Sem marcador de alarme.
- **Sucesso:** uma linha INFO `Cliente aquecido` por cliente, com `servico`, `operacao` e `duracaoMs`. Ela aparece no grupo de log na publicação da versão e prova, na stack, que o hook rodou.
- **Tempo:** pela mesma página, o `beforeCheckpoint` conta no limite de inicialização com SnapStart, de 130 s ou o `Timeout` da função, o que for maior. O pior caso é o relatório, com 3 chamadas de até 10 s.
- **X-Ray:** no hook não há trace. Verificado localmente no bytecode do X-Ray SDK 2.21.1: sem cabeçalho de trace, `LambdaSegmentContext` cria segmento no-op e só registra em DEBUG.
- **Compatibilidade com o desvio do `aspectjrt` (`add-registro-avaliacao`, D6):** o aquecimento não mexe no `ClassPreLoader` nem nos hooks do Powertools, e o `PrimingSnapStartTest` continua igual. Os hooks rodam na ordem inversa do registro; o aquecimento, registrado depois, roda antes dos hooks do Powertools. O `LinkageError` pego no aquecimento não esconde o problema do Powertools, que tem o próprio teste.
- **Dependência:** `org.crac:crac` declarado nos três módulos, na versão 1.5.0 que o Powertools 2.11.0 já traz, gerenciada no `pom.xml` parent.

Alternativas descartadas:
- Invocar o handler com evento falso antes do snapshot, como no exemplo da [página de boas práticas](https://docs.aws.amazon.com/lambda/latest/dg/snapstart-best-practices.html). Gravaria avaliação, publicaria aviso e criaria relatório; a própria página pede evitar efeito colateral.
- Só pré-carregar classes, sem rede. Não exercita credenciais, assinatura nem o cliente HTTP, que pesam na primeira chamada.
- Chamadas na inicialização estática. Também rodariam em toda inicialização sem SnapStart, como no `$LATEST` e no `sam local`. O hook só roda quando há snapshot.
- `GetItem` no registro. Daria à função leitura de itens com dado do aluno; `DescribeTable` só lê metadados.
- `HeadObject` no relatório. Exige `s3:GetObject`, que lê relatórios com descrições.
- Aumentar `apiCallTimeout`. Esconde a lentidão sem resolvê-la, e o do registro precisa ficar abaixo do `Timeout` de 10 s.

### D10. IAM das funções e boundary
Acréscimos nas políticas inline, todos de leitura e em recursos do ambiente:

| Role | Sid novo | Ação | Recurso |
|---|---|---|---|
| `RegistrarAvaliacaoRole` | `AquecerCliente` | `dynamodb:DescribeTable` | ARN da tabela |
| `NotificarCriticaRole` | `AquecerCliente` | `sns:GetTopicAttributes` | ARN do tópico de administradores |
| `GerarRelatorioSemanalRole` | `AquecerClienteSns` | `sns:GetTopicAttributes` | ARN do tópico de administradores |
| `GerarRelatorioSemanalRole` | `AquecerClienteS3` | `s3:ListBucket` | ARN do bucket, com `s3:ResourceAccount` |

- Tabela `feedback-avaliacoes-<amb>`, tópico `feedback-admins-<amb>` e bucket `feedback-relatorios-<conta>-<amb>`: todos com o sufixo do ambiente.
- O `s3:ListBucket` permite listar as chaves do bucket (`relatorios/<data>.txt` e `aws/lambda/...`), que não contêm dado do aluno. Leva `s3:ResourceAccount`, como na notificação.
- **Boundary:** `TabelaDoAmbiente` já tem `dynamodb:DescribeTable`, e `BucketDoAmbiente` já tem `s3:ListBucket`. Falta `sns:GetTopicAttributes`: o statement `PublicarNoTopico` vira `TopicosDoAmbiente`, com `sns:GetTopicAttributes` e `sns:Publish` nos mesmos recursos (D12).
- Sem o boundary novo, o deploy passa do mesmo jeito, porque a política inline pode exceder o boundary, mas o aquecimento do SNS recebe `AuthorizationError` e só registra WARN. Por isso o bootstrap vai antes do deploy (tasks, seção 2).

Alternativa descartada: `sns:ListTopics`. Exige `Resource: "*"`.

### D11. Fim do WARN de métrica vazia
Verificado localmente no bytecode do Powertools 2.11.0: `EmfMetricsLogger.flush()` registra `No metrics were emitted` em WARN quando nenhuma métrica foi acrescentada e `raiseOnEmptyMetrics` é falso, e lança exceção quando é verdadeiro.

Decisão: o handler da notificação só chama `flush` se a invocação contou alguma métrica. Hoje a única é `ItemInvalido`, no descarte. Registro e relatório não mudam, porque todo caminho deles conta uma métrica, mas os testes de log das três funções passam a afirmar que a invocação sem falha não tem linha em WARN nem em ERROR.

Alternativas descartadas:
- Emitir sempre uma métrica, como `AvisoPublicado`. Uma métrica personalizada a mais por ambiente só para calar um aviso.
- Subir o nível do logger do Powertools no `log4j2.xml`. O Powertools aplica `POWERTOOLS_LOG_LEVEL` a todos os loggers (`add-registro-avaliacao`, D9).
- `withRaiseOnEmptyMetrics(true)`. Troca o WARN por exceção.

### D12. Bootstrap: conferência ação por ação
Verificado na conta com `aws cloudformation describe-type --type RESOURCE --type-name <tipo>`, chave `handlers`, contra as políticas aplicadas (`get-policy-version`):

| Tipo | Ações dos handlers de criação, atualização e remoção | Coberto por |
|---|---|---|
| `AWS::CloudWatch::Alarm` | `PutMetricAlarm`, `DescribeAlarms`, `TagResource`, `UntagResource`, `ListTagsForResource`, `DeleteAlarms` | `AlarmesDoAmbiente` (alarm `feedback-*-<amb>`) e `LeiturasSemRestricaoPorRecurso` (`DescribeAlarms`) |
| `AWS::CloudWatch::Dashboard` | `PutDashboard`, `GetDashboard`, `TagResource`, `UntagResource`, `ListTagsForResource`, `DeleteDashboards` | `DashboardsDoAmbiente` (ARN sem região) |
| `AWS::SNS::Topic` | `CreateTopic`, `TagResource`, `Subscribe`, `GetTopicAttributes`, `SetTopicAttributes`, `Unsubscribe`, `ListTagsForResource`, `ListSubscriptionsByTopic`, `GetDataProtectionPolicy`, `UntagResource`, `DeleteTopic`. Condicionais: `PutDataProtectionPolicy`, `iam:GetRole`, `iam:PassRole` | `TopicosDoAmbiente`. As condicionais servem a proteção de dados e log de entrega, que o tópico não declara; o tópico de administradores foi criado com este mesmo bootstrap |
| `AWS::SNS::TopicInlinePolicy` | `SetTopicAttributes`, `GetTopicAttributes` | `TopicosDoAmbiente` |
| `AWS::SNS::Subscription` | `Subscribe`, `GetSubscriptionAttributes`, `Unsubscribe`. Condicionais: `iam:GetRole`, `iam:PassRole` | `TopicosDoAmbiente`, inclusive `feedback-*-<amb>:*` para o ARN da assinatura. As condicionais servem a assinatura com role, como Firehose, que não é o caso |
| `AWS::Logs::MetricFilter` | `PutMetricFilter`, `DescribeMetricFilters`, `DeleteMetricFilter` | `GruposDeLogDoAmbiente`, com e sem `:*` |
| `AWS::Logs::LogGroup`, atualização da retenção (D17) | `DescribeLogGroups`, `PutRetentionPolicy`, `DeleteRetentionPolicy`, `TagResource`, `UntagResource`, `ListTagsForResource`, `GetDataProtectionPolicy`. As demais do handler (KMS, proteção de dados, entrega, índice, política de recurso, proteção contra exclusão) dependem de propriedades que os grupos não declaram | `GruposDeLogDoAmbiente`, nos três ambientes pelo `Fn::ForEach`, e `LeiturasSemRestricaoPorRecurso` (`DescribeLogGroups`) |
| Funções (memória, variável nova, políticas inline) | `UpdateFunctionConfiguration`, `PublishVersion`, `UpdateAlias`, `iam:PutRolePolicy` com boundary | `FuncoesDoAmbiente` e `EscreverRolesComLimite` |

As tags de stack do `samconfig.toml` vão para alarme, dashboard e tópico, e as ações de tag estão nos statements acima. Assinatura, política de tópico e filtro não aceitam tag.

**Service-linked role:** a [página de service-linked roles do CloudWatch](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/using-service-linked-roles.html) só associa roles a ações de EC2, OpsCenter e Incident Manager, nenhuma a SNS. Verificado na conta: o alarme de cobrança existe, e `AWSServiceRoleForCloudWatchEvents` e `AWSServiceRoleForCloudWatchAlarms_ActionSSM` não existem, então criar alarme com ação SNS não dependeu delas. Fica como risco residual, com a lição do incidente do Stage: só o deploy prova.

**Conclusão: nenhuma ação nova nas políticas de deploy.** A única mudança é o `sns:GetTopicAttributes` no boundary (D10). Tamanhos medidos sem espaços, como o IAM conta, sobre os documentos aplicados:

| Política | Hoje | Depois | Folga até 6.144 |
|---|---|---|---|
| `feedback-limite-funcoes-dev` e `-hml` | 2.503 | 2.531 | 3.613 |
| `feedback-limite-funcoes-prod` | 2.519 | 2.547 | 3.597 |
| `github-deploy-feedback-prod-aplicacao` | 5.999 | sem mudança | 145 |
| `github-deploy-feedback-prod-recursos` | 5.860 | sem mudança | 284 |
| `github-deploy-feedback-dev-aplicacao` e `-hml-aplicacao` | 5.970 | sem mudança | 174 |
| `github-deploy-feedback-dev-recursos` e `-hml-recursos` | 5.844 | sem mudança | 300 |

Se o deploy revelar uma ação faltando em CloudWatch, SNS ou Logs, ela entra na `-recursos`, que já reúne observabilidade e tem 284 de folga em prod, e não na `-aplicacao`. Uma terceira política só entra se as duas estourarem.

Alternativa descartada: incluir `iam:CreateServiceLinkedRole` por precaução. Amplia o deploy sem evidência de necessidade.

### D13. Meta e método da nova medição (decisão 4)
**Meta, não critério de bloqueio:** a primeira chamada a cada serviço depois de uma restauração fica abaixo da metade do `apiCallTimeout` do cliente: 4 s no DynamoDB do registro, 2,5 s no SNS da notificação e 5 s em cada serviço do relatório, em ao menos duas restaurações por função. Se algum valor ficar acima, o QA registra o valor medido no `qa.md` e segue; o change não para por isso.

- **Medida:** duração do subsegmento do serviço (`DynamoDB`, `SNS`, `S3`) no trace do X-Ray da primeira invocação do ambiente restaurado. No relatório, a primeira das 7 `Query`, o `PutObject` e o `Publish`. Junto, o `Restore Duration` do `RESTORE_REPORT` e o `Memory Size` do `REPORT`, que deve mostrar 1024 (D15).
- **Base de comparação:** as medições com 512 MB e sem aquecimento dos QAs anteriores: 4.259 ms no DynamoDB do registro, 4.301 ms no SNS da notificação e restaurações de 403 a 903 ms. O relatório não tem base, porque o deploy dele ainda não foi medido.
- **Como forçar restauração:** primeira invocação depois do deploy, invocações concorrentes, que abrem ambientes novos, ou espera até o ambiente ser recolhido. A amostragem do X-Ray pode deixar uma restauração sem trace: só conta a que tem `RESTORE_REPORT` no log e trace com subsegmento `Restore`.
- **Notificação e relatório:** podem ser invocados direto no alias `live`, com evento sintético na notificação. Isso publica aviso real e, no relatório, regrava o objeto da data, o que é aceitável em dev. Invocar o alias não altera a função.

**Limite da medição:** aquecimento e memória entram no mesmo deploy, então a medição mostra o efeito somado, sem separar a parte de cada um.

### D14. Prova em dev (decisão 5)
Nada é alterado nas funções na AWS: nem `$LATEST`, nem configuração, nem código fora do pipeline. A prova se divide em três partes.

**1. Caminho do alarme até o e-mail: `set-alarm-state`.** Para cada alarme, `aws cloudwatch set-alarm-state --state-value ALARM` com motivo que identifique o teste. Prova a ação do alarme, a política do tópico aceitando o CloudWatch (`NumberOfMessagesPublished`) e o e-mail, que o Gabriel confirma. Na avaliação seguinte, cerca de um minuto depois, o alarme recalcula a partir dos dados; sem ponto na janela e com `notBreaching`, volta a OK sozinho.

**2. Caminho da linha de log até o alarme: `put-log-events`.** Com o perfil `tc4-admin`, o QA cria um log stream de teste em cada um dos três grupos, com nome que identifique o teste, `qa-observabilidade-<aaaammdd>`, e grava linhas com `aws logs put-log-events`. O filtro vale para todos os streams do grupo (D3). Regras das linhas:

- **Formato da aplicação:** linha JSON numa só linha, com os mesmos campos que o `LambdaJsonLayout.json` grava hoje (verificado na conta numa linha real: `level`, `message`, `cold_start`, `function_arn`, `function_memory_size`, `function_name`, `function_request_id`, `function_version`, `service`, `timestamp`, `xray_trace_id`), mais os campos da linha de falha do handler (`classe`, `codigoErro`, `idRequisicao` ou os da notificação) e `alarme` igual a `FALHA_FUNCAO`. `message` igual à da linha real de falha (D4). `function_request_id` com valor que identifique o teste.
- **Formato da plataforma, para o tempo esgotado:** as duas linhas de texto da D3, `<timestamp> <id> Task timed out after 10.01 seconds` e `REPORT RequestId: <id>` seguido dos campos separados por tabulação e de `Status: timeout`, no mesmo formato das linhas `REPORT` reais do grupo.
- **Sem dado de aluno:** nenhuma linha tem descrição, nota ou corpo de mensagem.
- **Horário atual:** o `timestamp` do evento é o momento do envio, em milissegundos. O ponto da métrica fica no minuto do evento; uma linha com horário antigo cairia fora da janela de 5 minutos e não dispararia, e uma com horário futuro prenderia o alarme em ALARM até lá.
- **Uma linha por caso:** sem rajadas, para o histórico ficar legível.

**Como o teste não deixa o alarme preso:** `Period` 60, janela de 5 períodos e `TreatMissingData` `notBreaching` (D5) fazem o alarme voltar a OK cerca de 5 minutos depois do último ponto. Não há `DefaultValue` que mantenha pontos chegando. O QA espera a volta a OK antes da janela seguinte e confere, ao fim, os três alarmes em OK.

**O stream de teste não é apagado:** expira com a retenção do grupo (D17). O nome identifica as linhas de teste para quem ler o grupo depois.

**3. Prova de qual linha o handler grava em cada falha:** testes unitários com appender em memória (D4, tasks 3.3, 3.5 e 3.7).

| Cenário | Método | Tipo |
|---|---|---|
| Tópico e assinatura criados; tópico sem KMS | `get-topic-attributes` e `list-subscriptions-by-topic` | execução |
| Política do tópico | leitura de `Policy` | inspeção |
| Requisição sem TLS negada | `aws sns get-topic-attributes --endpoint-url http://sns.sa-east-1.amazonaws.com` com o perfil administrativo; esperado `AuthorizationError` pela negação explícita | execução; se o endpoint não aceitar HTTP, inspeção |
| Alarme em ALARM notifica; publicação pelo CloudWatch aceita | `set-alarm-state` nos três alarmes, `NumberOfMessagesPublished` do tópico de alarmes; Gabriel confirma o e-mail | execução |
| Destino das ações; nenhum alarme no tópico de administradores | `describe-alarms` | inspeção |
| Falha interna do registro, falha parcial do lote, execução com falha do relatório | janela A: uma linha da aplicação com marcador em cada grupo; métrica com ponto 1, três alarmes em ALARM, três e-mails. Que o handler grava essa linha nessas falhas: testes unitários | execução e teste unitário |
| Tempo esgotado das três funções | janela B: as duas linhas de plataforma em cada grupo; métrica, ALARM e e-mail. Cada formato também isolado com `test-metric-filter` | execução sobre linha de teste |
| Erro de execução coberto pela definição | leitura do filtro e do alarme; `catch (Error e)` coberto por teste unitário | inspeção e teste unitário |
| Entrada rejeitada, data inválida, lote publicado com sucesso | janela C: linhas reais desses casos reproduzidas no stream de teste (`Entrada rejeitada` em INFO, `Data de referência rejeitada` em ERROR sem marcador, `Aviso publicado`, `REPORT` com restauração); alarmes ficam em OK e a métrica sem ponto. Também de verdade, sem alterar nada: requisições 400 pela API e `2026-02-30` no alias `live` do relatório | execução |
| Item descartado | invocação do alias `live` com `INSERT` crítico sem `descricao`: falha real, sem alterar a função; ALARM e e-mail | execução |
| Conteúdo da notificação sem dado do aluno | uma avaliação com texto marcador registrada antes das janelas; `describe-alarm-history` e os e-mails das janelas sem o marcador nem a nota | execução |
| Ausência de tráfego | uma hora sem invocação e sem linha de teste: histórico sem ALARM, tópico sem publicação | execução |
| Isolamento: inspeção das definições | namespace, nomes de função e grupos de log com o sufixo | inspeção |
| Isolamento: falha em outro ambiente | linha com marcador no grupo de dev com hml implantado; alarme e dashboard de hml sem mudança | execução na release (task 8.2) |
| Dashboard: widgets, restauração, falha tratada exibida, sem marcador | console e `get-dashboard`, no período das janelas | execução |
| Aquecimento com clientes simulados | testes unitários | execução local |
| Versão publicada com aquecimento | `State` `Active`, `OptimizationStatus` `On` e linhas `Cliente aquecido` no log da publicação | execução |
| Deploy sem efeito em dados | contagem de itens antes e depois, `NumberOfMessagesPublished` do tópico de administradores e objetos novos no bucket, na janela do deploy | execução |
| Primeira chamada depois da restauração | D13 | execução |
| Custo | contagem no template e `describe-alarms` nas regiões usadas | inspeção |
| Permissões das funções e de deploy | `get-role-policy`, `get-role` e `get-policy-version` | inspeção |
| Retenção por ambiente | `describe-log-groups` em dev: 14 dias; template: 30 em prod | execução e inspeção |

As janelas A, B e C usam os três grupos ao mesmo tempo, porque os alarmes são independentes, e uma só começa depois que os três alarmes voltaram a OK.

Alternativa descartada: provocar falha real alterando variáveis ou `Timeout` no `$LATEST`. Proibido pela decisão 5.

### D15. Memória de 1024 MB (decisão 6)
`MemorySize` no `Globals` passa de 512 para 1024, nas três funções.

- **Por quê:** a [página de memória](https://docs.aws.amazon.com/lambda/latest/dg/configuration-memory.html) diz que a CPU é proporcional à memória e que 1.769 MB equivalem a uma vCPU. 512 MB dão cerca de 0,29 vCPU; 1024, cerca de 0,58. O uso real de memória é de 215 MB (`Max Memory Used` dos `REPORT` de dev), então o ganho é só de CPU, que é o que pesa na primeira chamada: inicialização de classes, criptografia do TLS e assinatura.
- **Custo:** dobra os GB-s por invocação; ainda abaixo de 1% da franquia de 400.000 GB-s (D8).
- **SnapStart:** mudar a memória publica nova versão e gera novo snapshot nas três funções, o que já acontece neste deploy pelo código novo. O limite de 10 s para restaurar e para o `afterRestore` não muda. A duração da restauração passa a ser medida com a nova memória (D13); a página de SnapStart não diz como a memória configurada afeta a restauração, então não há expectativa numérica.
- **`Timeout` e `apiCallTimeout`:** sem mudança.

Alternativa descartada: só nas funções com primeira chamada lenta. Todas as três têm cliente do SDK chamado depois da restauração, e o `Globals` é o lugar da configuração comum (`add-registro-avaliacao`, D10).

### D16. `sam build` no CI (decisão 7)
No `.github/workflows/ci.yml`, o passo `mvn -B verify` vira `mvn -B install`, e entra o passo `sam build` depois de `aws-actions/setup-sam`, antes das validações. O JDK continua o Corretto 25 do `actions/setup-java`.

- **Por que `install`:** o `sam build` copia só o `CodeUri` de cada função para uma pasta temporária e resolve o módulo `dominio` pelo repositório Maven local. Sem o `install`, falha com `JavaMavenWorkflow:MavenBuild - Maven Failed` (`add-registro-avaliacao`, D12, verificado localmente). É o mesmo par de passos do `deploy.yml`. O `install` passa pelo `verify`, então os testes continuam rodando uma vez no passo Maven, e o artefato só vai para o `~/.m2` do runner.
- **O que ganha:** o PR passa a pegar erro de empacotamento, como dependência fora de `lib/`, que hoje só aparece no deploy.
- **Tempo a mais:** na última execução do CI, `mvn -B verify` levou 15 s e o job inteiro cerca de 30 s. No último deploy, com duas funções, `mvn -B install` levou 24 s e `sam build`, 29 s, porque o `sam build` roda `mvn clean install` de novo em cada função, testes inclusive. Com três funções, a estimativa é de 45 a 60 s a mais por PR. O `timeout-minutes` de 20 sobra.
- **Sem credencial:** `sam build` não chama a AWS, então o CI continua sem OIDC e sem `id-token`.
- **Nome do job:** continua `ci`, que é o check obrigatório da proteção de branch.

Alternativas descartadas:
- `mvn -B verify` seguido de `mvn -B install -DskipTests`. Dois passos para o mesmo resultado.
- `sam build --config-env dev`. O `samconfig.toml` não tem parâmetros de build, e o CI não é de ambiente nenhum.

### D17. Retenção de log por ambiente (decisão 8)
`RetentionInDays: !If [EhProd, 30, 14]` nos três `AWS::Logs::LogGroup`.

- **`Conditions`, e não `Mappings`:** a condição `EhProd` já existe e já separa prod dos outros dois no `PointInTimeRecoveryEnabled` da tabela. Um `Mappings` criaria um segundo mecanismo para a mesma distinção, com três entradas para um valor que só tem duas opções.
- **Valores válidos:** 14 e 30 estão na lista de valores aceitos de `RetentionInDays`.
- **Atualização sem substituição:** mudar a retenção é atualização do grupo existente; em dev e hml o valor nem muda.
- **Bootstrap:** verificado na conta, `GruposDeLogDoAmbiente` já concede `logs:PutRetentionPolicy` e `logs:DeleteRetentionPolicy` em `/aws/lambda/feedback-*-<amb>`, com e sem `:*`, nos três ambientes pelo `Fn::ForEach`. As demais ações do handler de atualização estão na D12.

Alternativa descartada: 30 dias em todos os ambientes. Dev recebe as linhas de teste da D14 e não precisa guardar mais.

## Risks / Trade-offs

- **Nível gratuito sem folga:** 10 de 10 métricas de alarme, contando o alarme de cobrança (D8). Mitigação: nenhum alarme de teste; hml e prod removidos depois do vídeo, como o `project.md` prevê.
- **Rateio das métricas personalizadas:** até 30 métricas distintas com as três stacks. A estimativa de que tudo cabe no gratuito depende de a franquia valer sobre o rateio por hora, o que a página não detalha. Mitigação: o alarme de cobrança que já existe.
- **Texto da linha de tempo esgotado sem prova real:** a D14 grava as linhas no formato do material de suporte, mas o runtime `java25` pode escrever outro texto. Se escrever, um tempo esgotado real não dispara o alarme e ninguém percebe. Mitigação: os dois formatos no padrão; se um tempo esgotado real aparecer em algum ambiente, o QA confere a linha com `test-metric-filter`.
- **Falha fora da JVM:** processo encerrado pelo sistema por memória, sem passar pelo `catch (Error e)` e sem tempo esgotado, não gera marcador. Com 1024 MB e 215 MB de uso, é improvável. Aceito.
- **Busca por substring:** um log futuro que contenha `FALHA_FUNCAO` ou `Task timed out` dispararia o alarme. Mitigação: constante única por handler, e os testes de log afirmam onde o marcador aparece.
- **Atraso do filtro:** a métrica do filtro chega depois da linha de log. A janela de 5 minutos da D5 absorve isso, mas o e-mail não é instantâneo.
- **Alarme no deploy:** uma inicialização que esgote o tempo na publicação da versão também casa o padrão. É falha real, e o deploy falha junto.
- **Linhas de teste no grupo de log:** ficam até a retenção expirar e aparecem em quem ler o grupo, inclusive como ponto na métrica de falhas do dashboard. Mitigação: stream com nome do teste e `function_request_id` identificável.
- **Credenciais e conexões no snapshot:** o aquecimento deixa credenciais e talvez conexões no snapshot. A página de boas práticas diz que, na maioria dos casos, as conexões do SDK retomam sozinhas, e a cadeia padrão renova credencial vencida. O QA observa a primeira invocação de cada restauração.
- **Meta pode não ser atingida:** parte da primeira chamada é rede e TLS depois da restauração, que o snapshot não guarda. Pela decisão 4, o QA registra o valor e o change segue.
- **Efeito somado:** aquecimento e memória entram juntos, e a medição não separa a contribuição de cada um (D13).
- **X-Ray no hook:** conferido no 2.21.1. Uma atualização do SDK pode mudar o comportamento; o QA olha o log da publicação.
- **CI mais lento:** cerca de um minuto a mais por PR (D16).
- **E-mails de prova:** cada janela e cada `set-alarm-state` gera e-mail. A notificação e o relatório invocados no `live` também geram aviso aos administradores.
- **Assinatura por ambiente:** sem confirmação, os alarmes publicam e ninguém recebe. Tasks 6.3 e 8.1.

## Open Questions

Nenhuma.

## Alterações no template.yaml

Dependência: integrar depois de `add-relatorio-semanal`. Só o devops edita o arquivo. Verificado localmente: os trechos abaixo, aplicados a uma cópia do `template.yaml` de `19ebb87`, passaram em `sam validate --lint`, e o corpo do dashboard é JSON válido depois da substituição das variáveis.

**1. `Globals`:** só estas duas linhas mudam.

```yaml
    MemorySize: 1024
```

```yaml
        POWERTOOLS_METRICS_NAMESPACE: !Sub FeedbackServerless/${Stage}
```

**2. Grupos de log:** nos três `AWS::Logs::LogGroup` (`LogRegistrarAvaliacao`, `LogNotificarCritica`, `LogGerarRelatorioSemanal`):

```yaml
      RetentionInDays: !If [EhProd, 30, 14]
```

**3. Políticas inline das funções**, acrescentadas aos statements existentes:

```yaml
            # RegistrarAvaliacao
            - Sid: AquecerCliente
              Effect: Allow
              Action: dynamodb:DescribeTable
              Resource: !GetAtt TabelaAvaliacoes.Arn
            # NotificarCritica
            - Sid: AquecerCliente
              Effect: Allow
              Action: sns:GetTopicAttributes
              Resource: !Ref TopicoAdmins
            # GerarRelatorioSemanal
            - Sid: AquecerClienteSns
              Effect: Allow
              Action: sns:GetTopicAttributes
              Resource: !Ref TopicoAdmins
            - Sid: AquecerClienteS3
              Effect: Allow
              Action: s3:ListBucket
              Resource: !GetAtt BucketRelatorios.Arn
              Condition:
                StringEquals:
                  s3:ResourceAccount: !Ref AWS::AccountId
```

**4. Ao fim de `Resources`**, depois de `GerarRelatorioSemanal`:

```yaml
  TopicoAlarmes:
    Type: AWS::SNS::Topic
    Properties:
      TopicName: !Sub feedback-alarmes-${Stage}

  PoliticaTopicoAlarmes:
    Type: AWS::SNS::TopicInlinePolicy
    Properties:
      TopicArn: !Ref TopicoAlarmes
      PolicyDocument:
        Version: "2012-10-17"
        Id: !Sub feedback-alarmes-${Stage}-politica
        Statement:
          - Sid: NegaAcessoSemTls
            Effect: Deny
            Principal: "*"
            Action:
              - sns:AddPermission
              - sns:DeleteTopic
              - sns:GetDataProtectionPolicy
              - sns:GetTopicAttributes
              - sns:ListSubscriptionsByTopic
              - sns:ListTagsForResource
              - sns:Publish
              - sns:PutDataProtectionPolicy
              - sns:RemovePermission
              - sns:SetTopicAttributes
              - sns:Subscribe
            Resource: !Ref TopicoAlarmes
            Condition:
              Bool:
                aws:SecureTransport: "false"
          - Sid: PublicacaoPelosAlarmesDoAmbiente
            Effect: Allow
            Principal:
              Service: cloudwatch.amazonaws.com
            Action: sns:Publish
            Resource: !Ref TopicoAlarmes
            Condition:
              ArnLike:
                aws:SourceArn: !Sub arn:${AWS::Partition}:cloudwatch:${AWS::Region}:${AWS::AccountId}:alarm:feedback-*-${Stage}
              StringEquals:
                aws:SourceAccount: !Ref AWS::AccountId

  AssinaturaEmailAlarmes:
    Type: AWS::SNS::Subscription
    Properties:
      TopicArn: !Ref TopicoAlarmes
      Protocol: email
      Endpoint: !Ref EmailAdmin

  FiltroFalhasRegistro:
    Type: AWS::Logs::MetricFilter
    Properties:
      FilterName: !Sub feedback-falhas-registrar-avaliacao-${Stage}
      LogGroupName: !Ref LogRegistrarAvaliacao
      FilterPattern: '?FALHA_FUNCAO ?"Task timed out" ?"Status: timeout"'
      MetricTransformations:
        - MetricNamespace: !Sub FeedbackServerless/${Stage}
          MetricName: FalhasRegistrarAvaliacao
          MetricValue: "1"
          Unit: Count

  AlarmeFalhaRegistro:
    Type: AWS::CloudWatch::Alarm
    Properties:
      AlarmName: !Sub feedback-falha-registrar-avaliacao-${Stage}
      AlarmDescription: Falha da função registrar-avaliacao, tratada ou de execução, inclusive tempo esgotado.
      Namespace: !Sub FeedbackServerless/${Stage}
      MetricName: FalhasRegistrarAvaliacao
      Statistic: Sum
      Period: 60
      EvaluationPeriods: 5
      DatapointsToAlarm: 1
      Threshold: 1
      ComparisonOperator: GreaterThanOrEqualToThreshold
      TreatMissingData: notBreaching
      AlarmActions:
        - !Ref TopicoAlarmes
```

`FiltroFalhasNotificacao` e `AlarmeFalhaNotificacao` repetem o par com `LogNotificarCritica`, `notificar-critica` e `FalhasNotificarCritica`. `FiltroFalhasRelatorio` e `AlarmeFalhaRelatorio` repetem com `LogGerarRelatorioSemanal`, `gerar-relatorio-semanal` e `FalhasGerarRelatorioSemanal`. O padrão do filtro é o mesmo nos três.

```yaml
  PainelAmbiente:
    Type: AWS::CloudWatch::Dashboard
    Properties:
      DashboardName: !Sub feedback-painel-${Stage}
      DashboardBody: !Sub |
        {"widgets":[
        {"type":"alarm","x":0,"y":0,"width":24,"height":3,"properties":{"title":"Alarmes de falha","alarms":["${AlarmeFalhaRegistro.Arn}","${AlarmeFalhaNotificacao.Arn}","${AlarmeFalhaRelatorio.Arn}"]}},
        {"type":"metric","x":0,"y":3,"width":8,"height":6,"properties":{"title":"Invocações","region":"${AWS::Region}","view":"timeSeries","stat":"Sum","period":300,"metrics":[["AWS/Lambda","Invocations","FunctionName","${RegistrarAvaliacao}"],["AWS/Lambda","Invocations","FunctionName","${NotificarCritica}"],["AWS/Lambda","Invocations","FunctionName","${GerarRelatorioSemanal}"]]}},
        {"type":"metric","x":8,"y":3,"width":8,"height":6,"properties":{"title":"Falhas, tratadas e de execução","region":"${AWS::Region}","view":"timeSeries","stat":"Sum","period":60,"metrics":[["FeedbackServerless/${Stage}","FalhasRegistrarAvaliacao"],["FeedbackServerless/${Stage}","FalhasNotificarCritica"],["FeedbackServerless/${Stage}","FalhasGerarRelatorioSemanal"]]}},
        {"type":"metric","x":16,"y":3,"width":8,"height":6,"properties":{"title":"Duração (ms)","region":"${AWS::Region}","view":"timeSeries","period":300,"metrics":[["AWS/Lambda","Duration","FunctionName","${RegistrarAvaliacao}",{"stat":"Average"}],["AWS/Lambda","Duration","FunctionName","${RegistrarAvaliacao}",{"stat":"Maximum"}],["AWS/Lambda","Duration","FunctionName","${NotificarCritica}",{"stat":"Average"}],["AWS/Lambda","Duration","FunctionName","${NotificarCritica}",{"stat":"Maximum"}],["AWS/Lambda","Duration","FunctionName","${GerarRelatorioSemanal}",{"stat":"Average"}],["AWS/Lambda","Duration","FunctionName","${GerarRelatorioSemanal}",{"stat":"Maximum"}]]}},
        {"type":"log","x":0,"y":9,"width":24,"height":6,"properties":{"title":"Restaurações do SnapStart","region":"${AWS::Region}","view":"table","query":"SOURCE '/aws/lambda/feedback-registrar-avaliacao-${Stage}' | SOURCE '/aws/lambda/feedback-notificar-critica-${Stage}' | SOURCE '/aws/lambda/feedback-gerar-relatorio-semanal-${Stage}' | filter @message like /^RESTORE_REPORT/ | parse @message 'Restore Duration: * ms' as restauracaoMs | parse @log /feedback-(?<funcao>[a-z-]+)-${Stage}$/ | display @timestamp, funcao, restauracaoMs | sort @timestamp desc | limit 50"}}
        ]}
```

**5. Ao fim de `Outputs`:**

```yaml
  TopicoAlarmesArn:
    Description: ARN do tópico de alarmes
    Value: !Ref TopicoAlarmes
  PainelNome:
    Description: Nome do dashboard do ambiente
    Value: !Ref PainelAmbiente
```

IDs lógicos novos: `TopicoAlarmes`, `PoliticaTopicoAlarmes`, `AssinaturaEmailAlarmes`, `FiltroFalhasRegistro`, `FiltroFalhasNotificacao`, `FiltroFalhasRelatorio`, `AlarmeFalhaRegistro`, `AlarmeFalhaNotificacao`, `AlarmeFalhaRelatorio` e `PainelAmbiente`.

## Alterações no ci.yml

Só o devops edita. O passo Maven muda e entra um passo depois do `setup-sam`:

```yaml
      - name: Build, testes e install no repositório Maven local
        run: mvn -B install

      - uses: aws-actions/setup-sam@v3
        with:
          use-installer: true

      - name: Empacotar as funções
        run: sam build
```

As duas validações com `sam validate --lint` continuam depois, sem mudança.

## Alterações no bootstrap

Em `infra/bootstrap/github-oidc.yaml`, só o statement `PublicarNoTopico` do boundary `LimiteFuncoes${Ambiente}` muda; o `Fn::ForEach` leva a mudança aos três ambientes:

```yaml
              - Sid: TopicosDoAmbiente
                Effect: Allow
                Action:
                  - sns:GetTopicAttributes
                  - sns:Publish
                Resource:
                  - !Sub arn:${AWS::Partition}:sns:${AWS::Region}:${AWS::AccountId}:feedback-*-${Ambiente}
                  - !Sub arn:${AWS::Partition}:sns:${AWS::Region}:${AWS::AccountId}:feedback-${Ambiente}-*
```

Aplicação pela IA com o perfil administrativo, depois da aprovação, pelo procedimento "Atualizar o bootstrap depois" do README: change set com `--no-execute-changeset`, revisão de que só as três `LimiteFuncoes*` mudam, sem substituição, e execução.

## Desvios

- **Do `project.md`:** nenhum.
- **De `add-registro-avaliacao`, D10:** aquele design diz que só ele altera o `Globals` e fixa `MemorySize: 512`. Este change muda `MemorySize` para 1024 (D15) e o valor de `POWERTOOLS_METRICS_NAMESPACE` (D6). Nenhuma outra propriedade do `Globals` muda.
- **De `add-registro-avaliacao`, D7:** os runtime hooks de CRaC, descartados ali "como melhoria se a latência medida incomodar", entram aqui, porque a latência medida ficou perto dos limites.
- **De `add-registro-avaliacao`, D11:** a retenção por ambiente, deixada para este change, passa a 30 dias em prod (D17).
- **De `add-fundacao-projeto`, D4, e da proposta:** a reaplicação do bootstrap é feita pela IA com o perfil administrativo, por autorização do Gabriel, e não à mão por ele.
