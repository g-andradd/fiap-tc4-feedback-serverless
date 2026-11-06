# QA: add-observabilidade-seguranca

Tarefas 5.1 a 5.3. Validação local feita em 06/11/2026 sobre o working tree do worktree `fiap-tc4-observabilidade`, branch `feature/add-observabilidade-seguranca`, com as seções 3 e 4 implementadas e sem commit.

**Nada foi escrito na AWS.** As consultas só de leitura previstas na 5.2 (`describe-alarms` e `list-dashboards` em `sa-east-1` e `us-east-1`) e o `test-metric-filter` não rodaram: o token SSO do perfil `tc4-admin` expirou (`Token has expired and refresh failed`). Renovar exige `aws sso login`, que é interativo, e o QA não fez isso sem confirmação. Ver "Pendências bloqueadas". A stack `feedback-dev` ainda não recebeu este change.

## Resultado

| Status | Quantidade |
|---|---|
| ok (executado) | 8 |
| ok (por configuração) | 18 |
| não coberto | 37 |
| falha | 0 |
| **Total de cenários** | **63** |

Por spec: `observabilidade` 47 (8 executados, 10 por configuração, 29 não cobertos), `seguranca` 13 (0, 6, 7) e `entrega-continua` 3 (0, 2, 1).

**O change ainda não está conforme como um todo:** cenário sem cobertura conta como falha, e há 37 nessa condição. Dos 37, 35 fecham com tarefas das seções 6 e 8 (stack de dev, CI do PR ou release), 1, `Computação com 1024 MB`, não tem tarefa que o feche (observação C), e 1, `Contagem pelo template e pela conta`, fecha assim que o SSO for renovado.

**Para o deploy, não há impedimento local:** build, testes, empacotamento e validação passaram, a inspeção do template não achou divergência do design, as 19 mutações foram detectadas e nenhum comportamento observado contraria a spec.

Significado dos status:
- **ok (executado):** o teste rodou em `mvn -B install` e a asserção prova o que o cenário pede no nível do handler ou do hook. Neste relatório, nenhum cenário foi executado contra a stack.
- **ok (por configuração):** o template, o workflow ou o bootstrap garantem o comportamento, mas ele não foi exercitado. Não equivale a teste. Quando só parte das cláusulas foi executada, vale a cláusula mais fraca.
- **não coberto:** não há evidência do que o cenário pede. Conta como falha. Declaração no `template.yaml` é indício, não evidência do recurso implantado.
- **falha:** o comportamento observado contraria o cenário.

## Limite da evidência

Os testes usam clientes do SDK simulados com Mockito ou falsos (`DynamoFalso`, `S3Falso`, `SnsFalso`), `Metrics` simulado e `RastreadorFalso`. Os testes de log capturam em memória com o layout real do `log4j2.xml` e nível `ALL`. Isso significa:

- **O que está provado:** em cada caminho de falha, o handler grava uma linha em ERROR com `alarme` igual a `FALHA_FUNCAO`; nos caminhos sem falha e nas rejeições, nenhuma linha contém o texto `FALHA_FUNCAO` e nenhuma está em WARN ou ERROR; o `Error` que escapa é registrado com o marcador e relançado; o hook de aquecimento chama só a leitura prevista em cada cliente, não grava nada e não propaga falha; a notificação só descarrega métricas quando contou alguma.
- **O que não está provado:** o casamento real da linha com o filtro, a métrica, o alarme, o e-mail, a política do tópico aplicada, o dashboard, a retenção e a memória implantadas, o hook rodando antes do snapshot real, o WARN real do Powertools e os tempos depois da restauração.

Na coluna Limite da matriz:
- **LOG:** captura em memória com o layout real, clientes simulados. O SDK e o `Metrics` reais não rodam.
- **SIM:** só clientes simulados, sem captura de log relevante para o cenário.
- **CONFIG:** leitura do `template.yaml`, do `ci.yml` ou do bootstrap, com a sonda abaixo. Nada foi exercitado.
- **LOCAL:** execução local de ferramenta (`sam build`) fora do CI.
- **STACK:** só a stack implantada fecha o cenário. A tarefa indicada é a que fecha.
- **CI:** só a execução do workflow no GitHub fecha o cenário.

A **sonda** é um programa local, fora do repositório e não versionado, executado com o Python do SAM CLI, sem rede. Ela lê o `template.yaml` preservando as funções intrínsecas e confere 131 itens da 5.2. Para provar que a sonda acusa erro, 9 mutações foram aplicadas a cópias do template (Period 10, `breaching`, KMS no tópico de alarmes, ação no tópico de administradores, memória 512, retenção fixa, uma ação a menos no `Deny`, conta literal e `PutItem` no aquecimento): as 9 foram acusadas. Ela não substitui o motor do CloudFormation nem o do CloudWatch Logs.

Legenda das classes de teste:
- `LR`: `br.com.fiap.feedback.registro.LogsRegistrarAvaliacaoTest`
- `AR`: `br.com.fiap.feedback.registro.AquecimentoTest`
- `LN`: `br.com.fiap.feedback.notificacao.LogsNotificarCriticaTest`
- `HN`: `br.com.fiap.feedback.notificacao.NotificarCriticaHandlerTest`, com a classe aninhada no nome
- `AN`: `br.com.fiap.feedback.notificacao.AquecimentoTest`
- `LG`: `br.com.fiap.feedback.relatorio.LogsGerarRelatorioSemanalTest`
- `AG`: `br.com.fiap.feedback.relatorio.AquecimentoTest`

## Verificações locais (tarefa 5.3)

JDK definido só no comando: Microsoft OpenJDK 25.0.5.0.2. SAM CLI 1.171.0. A comprovação em Corretto 25 vem do CI, na tarefa 6.1.

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

O `sam build` gerou `.aws-sam/` no worktree. A pasta é ignorada pelo Git.

## Inspeção do template (tarefa 5.2)

Feita pela sonda e por leitura. Linhas do `template.yaml` atual.

| Item | Resultado | Evidência |
|---|---|---|
| Alarmes | 3 `AWS::CloudWatch::Alarm`, cada um com uma métrica direta (`Namespace` e `MetricName`, sem `Metrics`, sem dimensão, sem `ExtendedStatistic`): 3 métricas de alarme por stack | Linhas 390, 419 e 448 |
| `Period`, estatística e limiar | `Period` 60, `Sum`, `Threshold` 1, `GreaterThanOrEqualToThreshold`, `EvaluationPeriods` 5 e `DatapointsToAlarm` 1 nos três, como a D5 | |
| `TreatMissingData` | `notBreaching` nos três | |
| Ações | `AlarmActions` só com `!Ref TopicoAlarmes`. Sem `OKActions` nem `InsufficientDataActions`. Nenhum alarme aponta para `TopicoAdmins` | |
| Filtros | 3 `AWS::Logs::MetricFilter`, um por grupo, com o padrão `?FALHA_FUNCAO ?"Task timed out" ?"Status: timeout"`, `MetricValue` `"1"`, `Unit` `Count`, sem `DefaultValue` nem dimensão, no namespace `FeedbackServerless/${Stage}`. Cada métrica de filtro é a métrica do alarme da mesma função | Linhas 378, 407 e 436 |
| Dashboard | 1 `AWS::CloudWatch::Dashboard`, `feedback-painel-${Stage}`, com 5 widgets (alarmes, invocações, falhas, duração e restaurações) e 12 métricas. Com `Stage=hml`, o corpo vira JSON válido e todas as métricas são `AWS/Lambda` por `FunctionName` da stack ou `FeedbackServerless/hml`; a consulta lê só os três grupos `-hml` e só `RESTORE_REPORT` | Linha 465 |
| Nomes no padrão do bootstrap | `feedback-falha-<função>-${Stage}`, `feedback-painel-${Stage}`, `feedback-alarmes-${Stage}` e `feedback-falhas-<função>-${Stage}`. Casam com `AlarmesDoAmbiente`, `DashboardsDoAmbiente`, `TopicosDoAmbiente` e `GruposDeLogDoAmbiente` | |
| Tópico de alarmes | Sem `KmsMasterKeyId`. O tópico de administradores continua com `alias/aws/sns` | Linhas 324 e 81 |
| Política do tópico (D2) | `AWS::SNS::TopicInlinePolicy` com `Id` `feedback-alarmes-${Stage}-politica` e dois statements. `NegaAcessoSemTls`: `Deny`, `Principal: "*"`, `aws:SecureTransport` `"false"`, as 11 ações da D2. `PublicacaoPelosAlarmesDoAmbiente`: `Allow` só de `sns:Publish` para `cloudwatch.amazonaws.com`, com `aws:SourceArn` em `ArnLike` com `alarm:feedback-*-${Stage}` e `aws:SourceAccount` igual a `AWS::AccountId`. Nenhum `Allow` para `*` | Linha 329 |
| Assinatura | `email` para `!Ref EmailAdmin` no `TopicoAlarmes` | Linha 369 |
| `MemorySize` | 1024 no `Globals`. Nenhuma das três funções sobrescreve | Linha 31 |
| Namespace | `POWERTOOLS_METRICS_NAMESPACE: !Sub FeedbackServerless/${Stage}` | Linha 41 |
| Retenção | `!If [EhProd, 30, 14]` nos três grupos, com `EhProd: !Equals [!Ref Stage, prod]`: 30 em prod, 14 em dev e hml | Linhas 21, 139, 178 e 252 |
| Statements de aquecimento | `AquecerCliente` com `dynamodb:DescribeTable` na tabela (registro) e `sns:GetTopicAttributes` no tópico de administradores (notificação); `AquecerClienteSns` com `sns:GetTopicAttributes` e `AquecerClienteS3` com `s3:ListBucket` no bucket e `s3:ResourceAccount` (relatório). Uma ação por statement, todas de leitura, todas em recursos da stack | Linhas 161, 224, 293 e 297 |
| ID de conta literal | Nenhum número de 12 dígitos no template | |
| Blocos do `design.md` | Os blocos da seção "Alterações no template.yaml" aparecem literalmente, descontados os comentários. O bloco das políticas inline aparece distribuído pelas três roles, como o design indica. O `git diff` do template remove só 5 linhas: `MemorySize: 512`, o namespace antigo e as três retenções fixas | |
| Bootstrap | O `git diff` de `infra/bootstrap/github-oidc.yaml` só troca `PublicarNoTopico` por `TopicosDoAmbiente`, com `sns:GetTopicAttributes` e `sns:Publish`, nos mesmos recursos. Nenhuma linha das políticas `-recursos` e `-aplicacao` mudou | |
| `ci.yml` | `mvn -B install`, `setup-sam`, passo `Empacotar as funções` com `sam build`, e as duas validações depois. `permissions: contents: read`, sem `id-token` e sem `configure-aws-credentials`. Job continua `ci` | Linhas 10, 11, 32 e 40 |

**Contagem na conta: não executada** (ver "Pendências bloqueadas"). Pelo template, as três stacks somam 9 métricas de alarme e 3 dashboards. A D8 registra, por consulta do arquiteto em 05/11/2026, 1 alarme de cobrança em `us-east-1` e nenhum dashboard, o que fecharia em 10 de 10 e 3 de 3. O QA não repetiu essa consulta.

## Matriz: observabilidade

### Tópico de alarmes por ambiente

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Tópico criado no deploy | **não coberto** | Indício: `TopicoAlarmes` com `feedback-alarmes-${Stage}`, distinto de `TopicoAdmins` | STACK. Fecha na 6.6 |
| Assinatura criada no deploy | **não coberto** | Indício: `AssinaturaEmailAlarmes` com `email` e `EmailAdmin` | STACK. Fecha na 6.3 e 6.6 |

### Notificação dos alarmes

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Alarme em ALARM notifica | **não coberto** | Nenhum alarme existe ainda | STACK. Fecha na 6.7 |
| Destino das ações de alarme | ok (por configuração) | `AlarmActions` dos três só com `TopicoAlarmes`, sem `OKActions` e sem `InsufficientDataActions` | CONFIG. Repetir com `describe-alarms` em dev (observação D) |

### Alarme de falha do registro

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Handler marca a falha interna | ok (executado) | `LR.falhaDoServico` e `LR.falhaDoCliente`: status 500, linha `Falha ao registrar avaliação` em ERROR com `alarme` igual a `FALHA_FUNCAO`. Mutação M1. O casamento com o filtro segue a busca por termo verificada na D3 e na 4.8; o QA não repetiu o `test-metric-filter` | LOG |
| Falha interna chega ao alarme | **não coberto** | | STACK. Fecha na 6.8 |
| Tempo esgotado chega ao alarme | **não coberto** | | STACK. Fecha na 6.9 |
| Erro de execução coberto pela definição | ok (por configuração) | Padrão com `FALHA_FUNCAO`, `"Task timed out"` e `"Status: timeout"`; `Threshold` 1 com `DatapointsToAlarm` 1. `LR.errorEscapadoComMarcador` prova a linha do `Error` (mutação M3). O texto real do tempo esgotado no `java25` segue sem prova (observação I) | CONFIG e LOG |
| Entrada rejeitada | ok (por configuração) | `LR.rejeicaoSemMarcador`, com 5 corpos rejeitados, inclusive malformado e vazio: status 400, nenhuma linha com `FALHA_FUNCAO`, nenhuma em WARN ou ERROR (mutação M2). Sem a linha, o filtro não publica ponto e `notBreaching` mantém o alarme em OK | LOG e CONFIG. Fecha na 6.10 |

### Alarme de falha da notificação

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Handler marca a falha parcial do lote | ok (executado) | `LN.falhaParcialDoLote`: primeira publicação aceita, segunda falha; `batchItemFailures` com 1 item, sequência `101`, sem exceção; só a linha `Falha ao publicar aviso` leva o marcador. `LN.falhaDoServico` e `LN.falhaDoCliente` para lote de um item. Mutação M9 | LOG |
| Falha parcial do lote chega ao alarme | **não coberto** | | STACK. Fecha na 6.8 |
| Item descartado por campo ausente | **não coberto** | No handler: `LN.itemInvalido`, com os 4 campos, grava `Item inválido descartado` com o marcador (mutação M10), e `HN.ItemInvalido.itemSemCampo` não publica. Falta o alias `live` e o alarme | STACK. Fecha na 6.11 |
| Tempo esgotado chega ao alarme | **não coberto** | | STACK. Fecha na 6.9 |
| Definição não depende só de Errors | ok (por configuração) | O alarme usa só `FalhasNotificarCritica`, gerada pelo filtro do grupo de log, que casa o marcador da falha de publicação, o do descarte, o do `Error` e as duas linhas de tempo esgotado. Nenhuma referência a `AWS/Lambda` `Errors` | CONFIG |
| Lote publicado com sucesso | ok (por configuração) | `LN.loteComSucessoSemWarnNemMarcador`: 2 publicações, 2 linhas em INFO, nenhuma com `FALHA_FUNCAO`. Sem a linha, não há ponto e o alarme fica em OK | LOG e CONFIG. Fecha na 6.10 |

### Alarme de falha do relatório

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Handler marca a execução com falha | ok (executado) | `LG.falhaComMarcador` com `leitura`, `gravação` e `publicação`: `GeracaoRelatorioException` em cada caso e uma única linha com o marcador, `Falha ao gerar relatório` em ERROR. `LG.itemInvalidoMarcaSoAExecucao` confere que `Item de avaliação inválido` não leva o marcador. Mutação M14 | LOG |
| Execução com falha chega ao alarme | **não coberto** | | STACK. Fecha na 6.8 |
| Tempo esgotado chega ao alarme | **não coberto** | | STACK. Fecha na 6.9 |
| Handler não marca a data inválida | ok (executado) | `LG.dataRejeitadaSemMarcador`: `2026-02-30` rejeitada com `DataReferenciaInvalidaException`, `Data de referência rejeitada` em ERROR sem `alarme`, nenhuma linha com `FALHA_FUNCAO`. Mutação M13 | LOG |
| Data de referência inválida na stack | **não coberto** | | STACK. Fecha na 6.10 |

### Ausência de tráfego não dispara alarme

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Ambiente sem tráfego | **não coberto** | Indício: `TreatMissingData: notBreaching` e filtros sem `DefaultValue` | STACK. Fecha na 6.13 |

### Isolamento por ambiente

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Inspeção das definições | ok (por configuração) | Sonda com `Stage=hml`: alarmes e filtros em `FeedbackServerless/hml`, filtros nos grupos `-hml` por `!Ref`, widgets só com `AWS/Lambda` por `FunctionName` da stack ou `FeedbackServerless/hml`, consulta só nos três grupos `-hml`, widget de alarmes pelos ARNs da stack. O `aws:SourceArn` da política limita a publicação a `alarm:feedback-*-hml`. A stack `feedback-hml` não existe ainda; o template é o mesmo para os três ambientes | CONFIG |
| Falha em outro ambiente | **não coberto** | A própria spec adia a prova para a release | STACK. Fecha na 8.2 |

### Invocação sem falha sem log em WARN

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Aviso crítico publicado | ok (executado) | `LN.loteComSucessoSemWarnNemMarcador` e `LN.ignoradoSemWarnNemMarcador`: nenhuma linha em WARN ou ERROR. `HN.MetricasERastreamento.sucesso`, `.ignoradoNaoDescarrega` e `.contagemPorInvocacao`: `flush` não é chamado sem métrica, e é o `flush` vazio que grava o WARN pela leitura de bytecode da D11. Mutação M8 | LOG. O WARN real do Powertools não aparece com `Metrics` simulado. Nenhuma tarefa da seção 6 confere o grupo de log (observação B) |
| Registro e relatório com sucesso | ok (executado) | `LR.sucessoSemWarnNemMarcador` (mutação M4) e `LG.sucessoSemWarnNemMarcador`: nenhuma linha em WARN ou ERROR, nenhuma com `FALHA_FUNCAO` | LOG. Mesma ressalva da linha anterior (observação B) |
| Alarmes sem condição em WARN | ok (por configuração) | Os três padrões de filtro não citam nível de log, e os alarmes só usam as métricas desses filtros | CONFIG |

### Dashboard por ambiente

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Dashboard criado no deploy | **não coberto** | Indício: um único dashboard, `feedback-painel-${Stage}`, com invocações, falhas, duração e restauração das três funções | STACK. Fecha na 6.14 |
| Duração de restauração exibida | **não coberto** | | STACK. Fecha na 6.14 |
| Falha exibida | **não coberto** | | STACK. Fecha na 6.14 |
| Estado dos alarmes | **não coberto** | Indício: widget `alarm` com os ARNs dos três alarmes | STACK. Fecha na 6.14 |

### Retenção de logs por ambiente

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Retenção em dev | **não coberto** | | STACK. Fecha na 6.5 |
| Retenção definida pelo ambiente no template | ok (por configuração) | `!If [EhProd, 30, 14]` nos três grupos; `EhProd` é `Stage` igual a `prod` | CONFIG |
| Retenção em prod | **não coberto** | | STACK. Fecha na 8.3 |

### Memória das funções

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Memória no template | ok (por configuração) | `MemorySize: 1024` no `Globals`; nenhuma função declara `MemorySize` | CONFIG |
| Memória na stack | **não coberto** | | STACK. Fecha na 6.4 |

### Aquecimento dos clientes do SDK

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Clientes exercitados antes do snapshot | ok (executado) | `AR.chamaSoDescribeTable` (tabela certa, nenhuma outra interação); `AN.chamaSoGetTopicAttributes` (tópico certo, nunca `publish`); `AG.queryNaParticaoVazia`, `AG.leiturasNoBucketENoTopico` e `AG.nadaGravadoNemPublicado`. Mutações M5, M7, M17, M18 e M19 | SIM. O registro do hook no contexto global e o uso das mesmas instâncias do handler são só por revisão (observação E) |
| Falha no aquecimento | ok (executado) | `falhaDoServico`, `falhaDoCliente` e `linkageError` em `AR` e `AN`; `AG.falhaDeUmClienteNaoImpedeOsOutros`, `AG.linkageErrorNaoImpedeOsOutros` e `AG.falhaDoServico`: nada propaga, WARN `Falha no aquecimento` só com `servico`, `classe` e `codigoErro`, sem `error`, sem o texto da exceção e sem marcador. Mutações M6, M12 e M16 | LOG |
| Versão publicada com aquecimento | **não coberto** | | STACK. Fecha na 6.4 |

### Aquecimento sem efeito em dados

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Deploy sem tráfego | **não coberto** | No hook: só leituras, com `verifyNoMoreInteractions` em `AR` e `AN` e `never` em `putObject` e `publish` em `AG`. Falta a contagem na conta | STACK. Fecha na 6.5 |

### Medição da primeira chamada depois da restauração

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Registro depois da restauração | **não coberto** | | STACK. Fecha na 6.15 |
| Notificação depois da restauração | **não coberto** | | STACK. Fecha na 6.15 |
| Relatório depois da restauração | **não coberto** | | STACK. Fecha na 6.15 |
| Amostra mínima | **não coberto** | | STACK. Fecha na 6.15 e 6.18 |
| Meta não atingida | **não coberto** | Só se aplica depois da medição | STACK. Fecha na 6.15 |

### Custo dentro do nível gratuito

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Contagem pelo template e pela conta | **não coberto** | Parte do template feita: 3 métricas de alarme e 1 dashboard por stack, 12 métricas no dashboard. Parte da conta não executada: SSO expirado | CONFIG e conta. Fecha quando o SSO for renovado |
| Alarmes sem alta resolução | ok (por configuração) | `Period` 60 nos três alarmes, sem `ExtendedStatistic`; widgets com 60 e 300 s | CONFIG |
| Computação com 1024 MB | **não coberto** | | STACK. Nenhuma tarefa da seção 6 faz a consulta (observação C) |

## Matriz: seguranca

### Tópico de alarmes sem criptografia e sem dado do aluno

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Atributos do tópico | **não coberto** | Indício: `TopicoAlarmes` sem `KmsMasterKeyId` | STACK. Fecha na 6.6 |
| Conteúdo da notificação | **não coberto** | | STACK. Fecha na 6.8, 6.12 e 6.17 |
| Métrica do alarme é contagem | ok (por configuração) | `MetricValue: "1"` nos três filtros, sem seletor `$` e sem `Dimensions` | CONFIG |

### Tópico de alarmes só aceita TLS

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Política do tópico | ok (por configuração) | `NegaAcessoSemTls` com `Principal: "*"`, `aws:SecureTransport` `"false"` e as 11 ações da D2. A lista foi conferida contra a D2, não contra a página da referência do SNS (observação H) | CONFIG. Repetir na 6.6 |
| Requisição sem TLS | **não coberto** | | STACK. Fecha na 6.6 |

### Publicação no tópico de alarmes restrita ao CloudWatch da conta

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Publicação pelo CloudWatch mantida | **não coberto** | | STACK. Fecha na 6.7 |
| Permissão restrita à conta | ok (por configuração) | Único `Allow`: `sns:Publish` para `cloudwatch.amazonaws.com`, com `aws:SourceAccount` e `aws:SourceArn` da conta e do ambiente. Nenhum `Allow` para `*` | CONFIG. Repetir na 6.6 |

### Dashboard sem dado do aluno

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Inspeção dos widgets | ok (por configuração) | Widgets de métrica só com contagens e durações. A consulta filtra `^RESTORE_REPORT` e exibe só `@timestamp`, `funcao` e `restauracaoMs`. O corpo não cita `descricao` nem `nota` | CONFIG. Repetir com `get-dashboard` na 6.14 |
| Marcador depois do tráfego | **não coberto** | | STACK. Fecha na 6.14 |

### Permissões das funções restritas ao ambiente

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Inspeção das roles | ok (por configuração) | Pelo `git diff` do template, as únicas ações novas nas roles são `dynamodb:DescribeTable` na tabela, `sns:GetTopicAttributes` no tópico de administradores e `s3:ListBucket` no bucket com `s3:ResourceAccount`. Todas de leitura, todas em recursos com o sufixo do ambiente | CONFIG. Fecha com `get-role-policy` na 6.16 |
| Boundary mantido | **não coberto** | Indício: `PermissionsBoundary` do `Globals` sem mudança | STACK. Fecha na 6.16 |

### Implantação pela role do ambiente

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Deploy em dev pela pipeline | **não coberto** | | STACK. Fecha na 6.2 |
| Permissões de deploy por ambiente | ok (por configuração) | Leitura do bootstrap: escrita em alarmes só em `alarm:feedback-*-<amb>` e `feedback-<amb>-*`, em dashboards só em `dashboard/feedback-*-<amb>` e `feedback-<amb>-*`, em tópicos só em `feedback-*-<amb>`, com `:*`, e `feedback-<amb>-*`, em filtros só em `log-group:/aws/lambda/feedback-*-<amb>`, com `:*`, e `feedback-<amb>-*`. O change não mexeu nessas políticas. `DescribeAlarms` e `DescribeLogGroups` com `"*"` são leitura | CONFIG. Fecha com `get-policy-version` na 6.16 |

## Matriz: entrega-continua

### Empacotamento das funções no CI

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Função sem código | ok (por configuração) | `sam build` numa cópia sem `functions/notificar-critica/src` terminou em `Build Failed` com saída 1. O passo do CI roda o mesmo comando, e o job `ci` é o check obrigatório da proteção de `develop`. Nenhuma tarefa abre um PR quebrado de propósito | LOCAL e CONFIG |
| PR válido com empacotamento | **não coberto** | Localmente, a mesma sequência passa e o log mostra as três funções. O check do PR ainda não rodou | CI. Fecha na 6.1 |
| Passo no workflow sem credencial AWS | ok (por configuração) | Passo `Empacotar as funções` com `sam build`; `permissions: contents: read`, sem `id-token` e sem `configure-aws-credentials` | CONFIG |

## Cenários que dependem da stack implantada

| Dependência | Cenários |
|---|---|
| Só a stack de dev fecha (32) | `Tópico criado no deploy`, `Atributos do tópico`, `Requisição sem TLS` (6.6); `Assinatura criada no deploy` (6.3 e 6.6); `Alarme em ALARM notifica`, `Publicação pelo CloudWatch mantida` (6.7); `Falha interna chega ao alarme`, `Falha parcial do lote chega ao alarme`, `Execução com falha chega ao alarme`, `Conteúdo da notificação` (6.8 e 6.12); os três `Tempo esgotado chega ao alarme` (6.9); `Data de referência inválida na stack` (6.10); `Item descartado por campo ausente` (6.11); `Ambiente sem tráfego` (6.13); `Dashboard criado no deploy`, `Duração de restauração exibida`, `Falha exibida`, `Estado dos alarmes`, `Marcador depois do tráfego` (6.14); `Memória na stack`, `Versão publicada com aquecimento` (6.4); `Retenção em dev`, `Deploy sem tráfego` (6.5); os 5 da medição (6.15); `Boundary mantido` (6.16); `Deploy em dev pela pipeline` (6.2) |
| Só o CI fecha (1) | `PR válido com empacotamento` (6.1) |
| Só a release fecha (2) | `Falha em outro ambiente` (8.2), `Retenção em prod` (8.3) |
| Sem tarefa que feche (1) | `Computação com 1024 MB` (observação C) |
| Bloqueado pelo SSO (1) | `Contagem pelo template e pela conta` |
| Por configuração, com repetição na stack prevista (8) | `Entrada rejeitada`, `Lote publicado com sucesso` (6.10); `Política do tópico`, `Permissão restrita à conta` (6.6); `Inspeção dos widgets` (6.14); `Inspeção das roles`, `Permissões de deploy por ambiente` (6.16); `Destino das ações de alarme` (observação D) |

**O que só a seção 6 prova:** que a linha real casa com o filtro e vira ponto na métrica, que o alarme publica no tópico e o e-mail chega sem dado do aluno, que a política aceita o CloudWatch e nega HTTP, que o hook roda antes do snapshot real sem impedir a versão e sem gravar dado, que a memória e a retenção estão aplicadas, que o dashboard mostra falhas e restaurações, os tempos depois da restauração com 1024 MB e o WARN real do Powertools ausente.

## Pendências bloqueadas

O SSO do perfil `tc4-admin` expirou. Depois de renovado (`aws sso login --profile tc4-admin`, interativo, pelo Gabriel ou com a confirmação dele), o QA roda, só leitura:

1. `aws cloudwatch describe-alarms` e `aws cloudwatch list-dashboards` em `sa-east-1` e `us-east-1`, para fechar `Contagem pelo template e pela conta`.
2. `aws logs test-metric-filter` com o padrão dos filtros contra as linhas de falha com marcador das três funções (modelo da 3.10), as linhas de tempo esgotado da D3 e as linhas sem falha (`Entrada rejeitada`, `Data de referência rejeitada`, `Aviso publicado`, `Falha no aquecimento`), para dar evidência própria do QA ao casamento citado em `Handler marca a falha interna` e nos outros cenários de handler.

## Defeitos

Nenhum defeito encontrado no código de produção.

## Observações

Não reprovam cenário.

- **A. AWS sem consulta.** Ver "Pendências bloqueadas". A contagem de 10 de 10 métricas de alarme depende só da D8.
- **B. WARN real fora da prova local e sem tarefa na stack.** Os testes de log usam `Metrics` simulado, então o `No metrics were emitted` do Powertools real nunca apareceria neles. A prova local é indireta: `flush` não é chamado sem métrica, mais a leitura de bytecode da D11. Os cenários `Aviso crítico publicado` e `Registro e relatório com sucesso` falam no grupo de log, e nenhuma tarefa da seção 6 confere a ausência de WARN e ERROR em invocação com sucesso. Sugestão: incluir essa conferência na 6.15, cujas invocações publicam aviso, gravam avaliação e geram relatório, ou na 6.10.
- **C. `Computação com 1024 MB` sem tarefa.** Nenhuma tarefa da seção 6 consulta o uso de GB-segundo e de requisições do mês. Sugestão: incluir na 6.18, pela página de nível gratuito do console de cobrança. A API do Cost Explorer cobra por requisição.
- **D. `Destino das ações de alarme` sem tarefa explícita.** A D14 prevê `describe-alarms` como inspeção, mas nenhuma tarefa da seção 6 a nomeia. Sugestão: incluir na 6.7.
- **E. Registro do hook só por revisão.** Nenhum teste carrega `Recursos`, então o `Core.getGlobalContext().register(...)`, a referência forte em `static final` e o uso das mesmas instâncias de cliente do handler só foram conferidos por leitura, nas três funções. Na stack, a linha `Cliente aquecido` na publicação da versão (6.4) prova que o hook rodou.
- **F. `RuntimeException` que escapa da notificação sai sem marcador.** O `catch (Error e)` cobre `Error`, e o tempo esgotado tem linhas próprias no filtro. No registro e no relatório, toda `RuntimeException` passa por um `catch` com marcador. Na notificação, só a publicação tem `catch`; por leitura do domínio e do handler, hoje nenhuma `RuntimeException` sai de `processar`, porque `deAtributos` valida tudo antes do construtor e `ehCriacaoCritica` garante a urgência que `AvisoCritico.de` exige. Uma regressão futura que lance ali terminaria a invocação com erro de função, sem marcador e sem alarme. Fica como risco residual, para o orquestrador decidir se vira tarefa.
- **G. Linha do `Error` fora do `withLogging`.** `registrarErro` roda depois de sair do `PowertoolsLogging.withLogging`. Se o Powertools limpa o contexto ao sair, a linha `Erro na execução da função` vai sem `function_request_id`. Não afeta o filtro e não foi verificado; só uma falha real mostraria.
- **H. Lista do `Deny` contra a referência.** A sonda confere as 11 ações contra a D2. O QA não consultou a página da referência do SNS nesta sessão.
- **I. Texto do tempo esgotado no `java25`.** Risco já registrado no design: as linhas da 6.9 seguem o formato do material de suporte, não uma linha real do runtime.
- **J. Arquivos de outro agente.** Durante a validação, `README.md` e `docs/roteiro-video.md` estavam alterados no worktree pelo documentador. O QA não os avaliou nem os tocou.

## Divergências do design

Nenhuma. O template, o `ci.yml`, o bootstrap e os handlers seguem a D1 a D17 e as seções "Alterações no template.yaml", "Alterações no ci.yml" e "Alterações no bootstrap", descontados comentários. Localmente, o `sam build` levou 64 s, um pouco acima da estimativa da D16 de 45 a 60 s a mais por PR, e o `mvn -B install` levou 39 s. O `timeout-minutes` de 20 continua com folga. O tempo no runner sai da 6.1.

## Prova das asserções

No próprio worktree, 19 mutações foram aplicadas ao código de produção, uma de cada vez, com `mvn -o -B test -pl functions/<módulo>` e JDK 25. As 19 foram detectadas. Antes de cada mutação, o arquivo original foi guardado em memória e regravado depois do teste, com conferência de hash SHA-256 por arquivo: os 19 voltaram idênticos. Ao fim, o hash do `git diff` de `functions`, `pom.xml`, `template.yaml`, `.github` e `infra` era igual ao de antes da rodada. Como o `git diff` não cobre os arquivos novos ainda não rastreados (`Aquecimento.java` das três funções), a volta deles foi conferida pelo hash por arquivo.

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

A rodada cobre só o código Java. O template passou pelas 9 mutações da sonda, em cópias, descritas em "Limite da evidência".

## Testes acrescentados

Nenhum. O pedido desta tarefa limitou a edição a este arquivo. Os cenários não cobertos dependem da stack, do CI, da release ou do SSO, e nenhum se resolve com teste local. Nenhum código de produção foi alterado.

## Coleção Postman

Não se aplica. O change não altera a API.
