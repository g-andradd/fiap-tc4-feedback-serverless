# Tasks: add-observabilidade-seguranca

Legenda de responsável: **[IA]** feito pelos agentes, **[Gabriel]** ação manual.

Depende de `add-relatorio-semanal` integrado na `develop`, com deploy verde em dev. A seção 2 pode começar logo depois da aprovação (1.1). As seções 3 e 4 só começam depois da 1.3 e rodam em paralelo: o dev-java edita só `functions/**` e o `pom.xml` parent, e o devops edita só `template.yaml` e `.github/workflows/ci.yml`. A 4.10 espera a seção 3, porque o `sam build` precisa do código. Enquanto houver agentes em paralelo, só a sessão principal marca este arquivo.

Regra da seção 6, decisão do Gabriel de 05/11/2026: nada é alterado nas funções na AWS fora do pipeline, nem `$LATEST`, nem configuração, nem código. Escrita na conta durante o QA só com `set-alarm-state`, `create-log-stream` e `put-log-events` nos streams de teste, e invocações normais dos aliases `live` e da API.

## 1. Preparação
- [x] 1.1 [Gabriel] Aprovar o `design.md` e este arquivo
- [x] 1.2 [Gabriel] Concluir `add-relatorio-semanal`: merge na `develop` e deploy verde em dev (tasks 5.1 e 5.2 daquele change)
- [x] 1.3 [Gabriel] Executar os comandos Git fornecidos para levar a `develop` atualizada à `feature/add-observabilidade-seguranca`

## 2. Bootstrap (depois da 1.1)
- [x] 2.1 [IA] devops: em `infra/bootstrap/github-oidc.yaml`, trocar o statement `PublicarNoTopico` do boundary por `TopicosDoAmbiente`, com `sns:GetTopicAttributes` e `sns:Publish`, conforme "Alterações no bootstrap" do `design.md`. Nada muda nas políticas `-recursos` e `-aplicacao`
- [x] 2.2 [IA] devops: validar com `sam validate --lint --region sa-east-1 -t infra/bootstrap/github-oidc.yaml` e medir o boundary sem espaços nos três ambientes. Esperado: 2.531 em dev e hml, 2.547 em prod
- [x] 2.3 [IA] com o perfil `tc4-admin`, gerar o change set pelo procedimento "Atualizar o bootstrap depois" do README (`--no-execute-changeset`) e conferir que só `LimiteFuncoesdev`, `LimiteFuncoeshml` e `LimiteFuncoesprod` aparecem, como `Modify` e sem substituição. Por pedido do Gabriel em 06/11/2026, ele executa o change set com o comando entregue. Change set com 3 `Modify` sem substituição; executado pelo Gabriel em 06/11/2026, stack `UPDATE_COMPLETE` às 03:31 UTC
- [x] 2.4 [IA] devops: conferir com `aws iam get-policy-version` a ação nova nos três boundaries e entregar o resultado ao qa. Conferido: versão v2 dos três boundaries com `sns:GetTopicAttributes` e `sns:Publish`, 2.531 caracteres em dev e hml e 2.547 em prod

## 3. Funções (dev-java, depois da 1.3)
- [x] 3.1 [IA] dev-java: acrescentar `org.crac:crac` ao `dependencyManagement` do `pom.xml` parent, na versão que o Powertools 2.11.0 traz (1.5.0), e declará-lo nos três módulos de função; conferir com `mvn dependency:tree`
- [x] 3.2 [IA] dev-java: `registrar-avaliacao`: criar `Aquecimento` conforme D9 (`DescribeTable` na tabela), guardar em `static final` de `Recursos` e registrar no contexto global ali; acrescentar o marcador `alarme=FALHA_FUNCAO` em `Falha ao registrar avaliação` e o `catch (Error e)` com marcador e relançamento (D4)
- [x] 3.3 [IA] dev-java: testes do registro: o aquecimento chama `describeTable` com a tabela e nenhuma operação de escrita; `RuntimeException` e `LinkageError` do cliente não propagam e geram WARN só com serviço, classe e código; o marcador aparece na falha ao gravar e no `Error` escapado, e não aparece no sucesso nem nas rejeições; nenhuma linha WARN ou ERROR no sucesso. Estes testes são a prova de qual linha o handler grava em cada falha (D4)
- [x] 3.4 [IA] dev-java: `notificar-critica`: criar `Aquecimento` (`GetTopicAttributes` no tópico de administradores), registrar em `Recursos`; marcador em `Falha ao publicar aviso` e em `Item inválido descartado`; `catch (Error e)`; `flush` só quando a invocação contou alguma métrica (D11)
- [x] 3.5 [IA] dev-java: testes da notificação: aquecimento chama `getTopicAttributes` e nunca `publish`; falha do cliente não propaga; lote publicado com sucesso e registro ignorado sem nenhuma linha WARN ou ERROR; `flush` chamado no descarte e não chamado no sucesso; marcador na falha de publicação, inclusive na falha parcial do lote, e no descarte, e ausente no sucesso
- [x] 3.6 [IA] dev-java: `gerar-relatorio-semanal`: levar os três clientes para campos de `Recursos`, compartilhados por `GeradorRelatorio` e `Aquecimento`; criar `Aquecimento` com `Query` no índice `por-data` (`dataEnvio = "aquecimento"`, `Limit` 1), `HeadBucket` e `GetTopicAttributes`; marcador só em `Falha ao gerar relatório`, nunca em `Data de referência rejeitada`; `catch (Error e)`
- [x] 3.7 [IA] dev-java: testes do relatório: aquecimento chama os três clientes e nunca `putObject` nem `publish`; a falha de um cliente não impede os outros dois; marcador presente na falha de leitura, gravação e publicação, e ausente na data rejeitada e no sucesso; nenhuma linha WARN ou ERROR no sucesso
- [x] 3.8 [IA] dev-java: conferir por revisão que cada `Aquecimento` tem referência forte, recebe as mesmas instâncias de cliente do handler, não grava nada e não registra mensagem nem pilha de exceção
- [x] 3.9 [IA] dev-java: validar com `mvn -B verify`, com os três `PrimingSnapStartTest` passando sem mudança
- [x] 3.10 [IA] dev-java: entregar ao qa uma linha de falha com marcador de cada função, copiada da saída dos testes, para servir de modelo às linhas de teste da 6.8

## 4. Infraestrutura e CI (devops, depois da 1.3)
- [x] 4.1 [IA] devops: no `Globals`, trocar `MemorySize` para 1024 (D15) e `POWERTOOLS_METRICS_NAMESPACE` para `!Sub FeedbackServerless/${Stage}` (D6), sem tocar nas outras propriedades
- [x] 4.2 [IA] devops: trocar `RetentionInDays` dos três `AWS::Logs::LogGroup` para `!If [EhProd, 30, 14]` (D17)
- [x] 4.3 [IA] devops: acrescentar `AquecerCliente`, `AquecerClienteSns` e `AquecerClienteS3` às políticas inline das três funções, conforme o `design.md`
- [x] 4.4 [IA] devops: acrescentar `TopicoAlarmes`, `PoliticaTopicoAlarmes` e `AssinaturaEmailAlarmes`
- [x] 4.5 [IA] devops: acrescentar os três filtros e os três alarmes, com o mesmo padrão de filtro
- [x] 4.6 [IA] devops: acrescentar `PainelAmbiente` e os outputs `TopicoAlarmesArn` e `PainelNome`
- [x] 4.7 [IA] devops: no `.github/workflows/ci.yml`, trocar `mvn -B verify` por `mvn -B install` e acrescentar `sam build` depois do `setup-sam`, conforme "Alterações no ci.yml" do `design.md`, mantendo o nome do job `ci`
- [x] 4.8 [IA] devops: rodar `aws logs test-metric-filter` com o padrão do filtro contra as linhas de exemplo da D3 e as linhas de modelo da 3.10, e registrar quais casaram
- [x] 4.9 [IA] devops: validar com `sam validate --lint`
- [x] 4.10 [IA] devops: depois da seção 3, rodar a mesma sequência do CI novo (`mvn -B install` e `sam build`), usando JDK 25, conferir `aspectjrt` e `crac` em `lib/` das três funções e registrar o tempo de cada passo

## 5. Validação local
- [x] 5.1 [IA] qa: conferir a cobertura de cada `#### Scenario` das specs `observabilidade` e `seguranca` nos testes e relatar os que dependem da stack
- [x] 5.2 [IA] qa: inspecionar o template: 3 alarmes com uma métrica cada, `Period` 60, `TreatMissingData` `notBreaching`, ações só no tópico de alarmes; 1 dashboard; nomes no padrão do bootstrap; tópico sem `KmsMasterKeyId`; política do tópico conforme D2; `MemorySize` 1024; retenção 30 em prod e 14 nos demais
- [x] 5.3 [IA] qa: rodar `mvn -B install`, `sam build` e `sam validate --lint`, e confirmar que o domínio continua sem SDK da AWS

## 6. Deploy em dev e conferência
- [x] 6.1 [Gabriel] Executar os comandos Git fornecidos (commits na `feature/add-observabilidade-seguranca`, push, PR para `develop`) e conferir o CI verde, já com o `sam build`
- [x] 6.2 [Gabriel] Fazer o merge e conferir o deploy da stack `feedback-dev`
- [x] 6.3 [Gabriel] Confirmar a assinatura de e-mail do tópico `feedback-alarmes-dev`
- [x] 6.4 [IA] qa: conferir `State` `Active`, `OptimizationStatus` `On` e `MemorySize` 1024 no alias `live` das três funções, e as linhas `Cliente aquecido` no log da publicação de cada versão, sem WARN de aquecimento
- [x] 6.5 [IA] qa: conferir que o deploy não mudou a quantidade de itens da tabela, não publicou no tópico de administradores e não criou objeto no bucket; conferir a retenção de 14 dias nos três grupos de dev
- [x] 6.6 [IA] qa: conferir atributos, política e assinatura do tópico de alarmes, e tentar `get-topic-attributes` sem TLS, conforme D14
- [x] 6.7 [IA] qa: `set-alarm-state` para ALARM em cada alarme, conferir `NumberOfMessagesPublished` do tópico de alarmes, pedir ao Gabriel a confirmação dos três e-mails e conferir que os alarmes voltaram a OK sozinhos. Antes, conferir com `describe-alarms` que a única ação de cada alarme é o tópico de alarmes do ambiente
- [x] 6.8 [IA] qa: registrar uma avaliação com texto marcador na descrição; criar o stream `qa-observabilidade-<aaaammdd>` nos três grupos de dev e, pela D14, rodar a janela A (uma linha da aplicação com marcador por grupo, no formato JSON da aplicação, horário atual), conferindo ponto na métrica, os três alarmes em ALARM, os três e-mails e a volta a OK
- [x] 6.9 [IA] qa: janela B da D14: as duas linhas de plataforma de tempo esgotado em cada grupo, conferindo métrica, ALARM, e-mail e volta a OK; isolar cada formato com `test-metric-filter`
- [x] 6.10 [IA] qa: janela C da D14: linhas reais de rejeição e de sucesso reproduzidas no stream de teste, requisições 400 pela API e `2026-02-30` no alias `live` do relatório, conferindo que os alarmes ficam em OK e a métrica sem ponto. Conferir também que as invocações com sucesso não geram nenhuma linha WARN ou ERROR no grupo de log
- [x] 6.11 [IA] qa: item crítico sem `descricao` no alias `live` da notificação, conferindo ALARM e e-mail, e a volta a OK
- [x] 6.12 [IA] qa: conferir que nenhuma mensagem de alarme, nenhum histórico e nenhum e-mail das janelas contém o texto marcador da 6.8 ou a nota usada
- [x] 6.13 [IA] qa: conferir uma hora sem invocação e sem linha de teste em dev, sem ALARM e sem publicação no tópico de alarmes
- [x] 6.14 [IA] qa: conferir o dashboard de dev: widgets da D7, falhas das janelas e restaurações visíveis no período, isolamento por ambiente e ausência do marcador
- [x] 6.15 [IA] qa: medir, com 1024 MB, a primeira chamada depois da restauração e a duração da restauração em ao menos duas restaurações por função, conforme D13, e comparar com a meta e com a base de 512 MB. Valor acima da meta é registrado e não bloqueia
- [x] 6.16 [IA] qa: inspecionar as roles das três funções contra as anteriores, o boundary de cada uma e as políticas de deploy de dev
- [ ] 6.17 [Gabriel] Confirmar no e-mail o recebimento dos alarmes das provas e que nenhum traz descrição ou nota
- [x] 6.18 [IA] qa: registrar o relatório de conformidade em `qa.md` deste change, com as linhas de teste usadas, o ID da conta mascarado, e a observação de que o stream de teste expira com a retenção
- [ ] 6.19 [IA] qa: conferir no console de cobrança, sem a API do Cost Explorer, o uso de Lambda em GB-s do mês com 1024 MB, contra o nível gratuito (cenário "Computação com 1024 MB"). Não coberto no QA de 06/11/2026: sem acesso ao console de cobrança; indício pelas linhas `REPORT`: 138 invocações e 48,9 GB-s

## 7. Documentação
- [x] 7.1 [IA] documentador: registrar no README o tópico de alarmes e a confirmação da assinatura em cada ambiente, o que cada alarme cobre, o dashboard, o namespace por ambiente, o aquecimento, a memória de 1024 MB, a retenção por ambiente e a mudança no boundary
- [x] 7.2 [IA] documentador: registrar no README o limite de 10 métricas de alarme na conta, incluindo o alarme de cobrança, a regra de não criar alarme de teste e como testar um alarme com `put-log-events` num stream de teste
- [x] 7.3 [IA] documentador: registrar no README que o CI agora roda `mvn -B install` e `sam build`
- [x] 7.4 [IA] documentador: acrescentar ao roteiro do vídeo a abertura do dashboard e um alarme disparado com `set-alarm-state`, lembrando que ele gera e-mail

## 8. hml e prod (na release)
- [ ] 8.1 [Gabriel] Confirmar a assinatura do tópico de alarmes de hml e de prod quando cada stack for implantada
- [ ] 8.2 [IA] qa: com dev e hml implantados, gravar uma linha com marcador num stream de teste do grupo de dev e conferir que o alarme e o dashboard de hml não mudam (cenário "Falha em outro ambiente")
- [ ] 8.3 [IA] qa: conferir a retenção de 30 dias nos três grupos de prod
