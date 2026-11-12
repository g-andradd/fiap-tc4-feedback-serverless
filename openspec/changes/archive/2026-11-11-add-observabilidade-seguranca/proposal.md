# Change: Observabilidade e segurança da plataforma

## Why

As três funções já gravam logs estruturados em JSON com retenção de 14 dias, têm X-Ray ativo, emitem métricas do Powertools no namespace `FeedbackServerless` e rodam com roles mínimas sob o boundary do ambiente. Falta o que transforma esses dados em aviso e em visão: o `project.md` prevê CloudWatch Alarms e Dashboard, e nenhum change entregou os dois. Hoje uma falha só é percebida por quem lê o log.

Os QAs de dev deixaram quatro pendências que o backlog do `project.md` atribui a este change:

- A primeira chamada ao SDK depois da restauração do SnapStart levou 4,2 s no DynamoDB do registro e 4,3 s no SNS da notificação, perto dos limites de 8 s e 5 s dos clientes.
- A falha parcial de lote da notificação termina a invocação normalmente e não aparece na métrica `Errors`. O mesmo vale para a resposta 500 do registro, que o handler trata.
- A notificação grava `No metrics were emitted` em WARN a cada invocação bem-sucedida, o que inutiliza filtro ou alarme por nível de log.
- O CloudWatch não publica em tópico criptografado com `alias/aws/sns`, que é a chave do tópico de administradores. Os alarmes precisam de um tópico próprio.

A leitura do código trouxe mais um ponto: as métricas do Powertools levam só a dimensão `Service`, igual nos três ambientes. Sem separação, um alarme de prod reagiria a uma falha de dev, o que contraria o isolamento por ambiente da spec `fundacao-cloud`.

Na revisão do change, o Gabriel incluiu três ajustes de operação (05/11/2026): memória de 1024 MB nas funções, para atacar a lentidão depois da restauração também pela CPU; `sam build` no CI, porque hoje um template que referencia função sem código só falha no deploy; e retenção de logs maior em prod.

## What Changes

- Tópico de alarmes por ambiente, separado do tópico de administradores, com assinatura por e-mail para o mesmo `EmailAdmin`
- Tópico de alarmes sem criptografia em repouso, com política que nega acesso sem TLS e libera a publicação somente pelo CloudWatch da própria conta
- Um alarme de falha por função, que considera também as falhas tratadas: resposta 500 do registro, falha de publicação da notificação, inclusive parcial no lote, e execução com falha do relatório
- Entrada rejeitada e ausência de tráfego não disparam alarme
- Métricas, alarmes e dashboard de um ambiente refletem somente aquele ambiente, com as métricas separadas por namespace no `Globals`
- Um dashboard por ambiente com invocações, falhas, duração e duração de restauração do SnapStart de cada função, mais o estado dos alarmes
- Invocação sem falha deixa de gerar log em WARN, e nenhum alarme usa WARN como condição
- Aquecimento, antes do snapshot, dos clientes do SDK de cada função, sem gravar dado e sem impedir a publicação da versão
- Memória de 1024 MB nas três funções, no `Globals`, no lugar de 512 MB
- Nova medição da duração da restauração e da primeira chamada depois dela, comparada com uma meta que serve de referência e não reprova o change
- Retenção dos logs das funções em 30 dias em prod e 14 dias em dev e hml
- `sam build` no CI de pull request, para o PR falhar quando o template referenciar função sem código
- Alarmes e dashboard dentro do nível gratuito do CloudWatch com os três ambientes implantados, contando o alarme de cobrança que já existe na conta, sem folga
- Permissões novas restritas ao ambiente, sem escrita de dados nas roles das funções

### Método de prova

Por decisão do Gabriel (05/11/2026), nenhuma prova altera o `$LATEST` ou o código das funções na AWS. A stack prova o caminho filtro, métrica, alarme e notificação: uma linha que casa com o filtro, sem dado de aluno, é gravada com `aws logs put-log-events` num log stream de teste dentro do grupo de log de cada função. O caminho do alarme até o e-mail também é provado com `aws cloudwatch set-alarm-state`. Que o handler grava a linha certa em cada falha (falha interna, falha parcial do lote, execução com falha) é provado pelos testes unitários. O tempo esgotado não é provocado em nenhuma das três funções: a linha no formato de tempo esgotado do Lambda é gravada no stream de teste.

### Fora do escopo

- **Chave KMS gerenciada pelo cliente:** custo mensal por chave, e o tópico de alarmes não carrega dado de aluno (decisão de 05/11/2026 no `project.md`).
- **AWS WAF:** HTTP API não integra com WAF (`add-registro-avaliacao`, design D1), e o limite de taxa do Stage já existe.
- **GuardDuty, Security Hub, Inspector, Macie e AWS Config:** cobrança depois do período de teste e nenhum pedido no enunciado ou no `project.md`.
- **Trilha dedicada do CloudTrail:** o histórico de eventos de 90 dias, gratuito, já atendeu as investigações dos QAs.
- **Autenticação da API:** fora do escopo desde `add-registro-avaliacao`.
- **Política de TLS no tópico de administradores:** não está no backlog. Os publicadores são as próprias funções, pelo SDK em HTTPS.
- **Logs de acesso do API Gateway:** mais ingestão de log e gravação de IP do aluno, sem requisito que peça.
- **Alarme de retorno a OK, alarme composto, detecção de anomalia, canário do Synthetics e X-Ray Insights:** além do mínimo para perceber uma falha.
- **Canais de alarme além de e-mail:** o e-mail já é o canal do projeto.
- **Métricas de negócio no dashboard**, como avaliações por urgência: a consolidação é papel do relatório semanal.
- **Alarme para a execução semanal que não aconteceu:** exige regra própria para um evento semanal, e só uma segunda-feira cai antes do prazo.
- **Ciclo de vida do bucket de relatórios e do prefixo `aws/lambda/`:** já aceito como fora em `add-notificacao-critica` e `add-relatorio-semanal`.
- **Testes automatizados que leem o `template.yaml`:** observações de QA anteriores, não pedidas no backlog. O `sam build` no CI cobre só a referência a função sem código.
- **Provocar falha real na stack alterando função:** proibido pelo Gabriel em 05/11/2026. Ver "Método de prova".
- **Migração do X-Ray SDK para OpenTelemetry e remoção do `aspectjrt`:** riscos já aceitos em `add-registro-avaliacao`.
- **Seções de segurança e governança e de monitoramento do relatório da FIAP:** são escritas em `add-documentacao-entrega`. Este change entrega os recursos e as evidências que elas vão descrever.

## Impact

- Specs novas: `observabilidade`, `seguranca`
- Delta em spec existente: `entrega-continua` (`add-fundacao-projeto`), com o empacotamento das funções no CI
- Código afetado:
  - `Globals` do `template.yaml`: namespace das métricas por ambiente, que faz a separação das métricas sem mudança nos handlers (design D6), e memória de 1024 MB
  - `template.yaml`: tópico de alarmes, política e assinatura, filtros de métrica, alarmes e dashboard por ambiente, e retenção dos grupos de log por ambiente
  - Handlers das três funções, por outros motivos: marcador de falha para os alarmes (design D4), aquecimento antes do snapshot (design D9) e, na notificação, fim do WARN em invocação sem falha (design D11)
  - Boundary das funções, com a leitura que o aquecimento exige (design D10)
  - `.github/workflows/ci.yml`: passo de `sam build`
- Dependências:
  - `add-fundacao-projeto`: parâmetro `EmailAdmin`, tópico de administradores, boundary e roles de deploy, CI de pull request
  - `add-registro-avaliacao`: métricas `AvaliacaoRegistrada`, `EntradaRejeitada` e `ErroInterno`, limite de 8 s do cliente DynamoDB, grupos de log com retenção de 14 dias
  - `add-notificacao-critica`: métrica `ItemInvalido`, log em ERROR na falha de publicação, limite de 5 s do cliente SNS
  - `add-relatorio-semanal`: métricas `RelatorioGerado`, `DataReferenciaRejeitada` e `ErroInterno`, limite de 10 s dos clientes. **Esse change ainda não está na `develop`**, e a implementação deste depende do merge dele
- Custo:
  - **CloudWatch:** com o alarme de cobrança que já existe em `us-east-1`, as três stacks fecham em 10 de 10 métricas de alarme e 3 de 3 dashboards, sem folga: qualquer alarme ou dashboard a mais, inclusive de teste, sai do nível gratuito (design D8). A separação das métricas por ambiente multiplica as métricas personalizadas pelo número de stacks ativas, cobradas só nas horas com dado, então hml e prod pesam pouco enquanto existirem. Os 16 dias a mais de retenção em prod cabem nos 5 GB de logs do nível gratuito com o volume da demonstração.
  - **Lambda:** a memória dobra, e com ela os GB-segundo de cada invocação. O nível gratuito do Lambda é de 1 milhão de requisições e 400.000 GB-segundo por mês, número que o arquiteto confirma na página de preços. Com 1 GB, isso equivale a 400.000 segundos de execução. Mesmo 2.000 invocações de 10 s, bem acima do que os QAs e o vídeo usam, somam 20.000 GB-segundo, cerca de 5% do limite. O cenário de custo confere o uso real do mês depois do QA.
- Ações manuais do Gabriel:
  - Confirmar a assinatura do e-mail do tópico de alarmes em cada ambiente
  - Executar os comandos Git fornecidos

### Restrições para o design

- **Limite da política de deploy em prod.** A política `github-deploy-feedback-prod-aplicacao` tem folga de cerca de 145 caracteres do limite de 6.144 de uma managed policy. As ações de deploy deste change (alarmes, dashboard, política do tópico, filtros de métrica de log) podem não caber e exigir uma terceira política. Por leitura do bootstrap em `4834875`, a política `-recursos` já traz `AlarmesDoAmbiente`, `DashboardsDoAmbiente` e `sns:SetTopicAttributes` nos tópicos do ambiente, e a `-aplicacao` já traz `logs:PutMetricFilter`. O design D12 conferiu ação por ação e concluiu que nenhuma ação nova entra nas políticas de deploy.
- **Bootstrap fora do pipeline.** Qualquer mudança nas roles de deploy ou no boundary `feedback-limite-funcoes-<amb>` continua fora do pipeline (`add-fundacao-projeto`, design D4). A reaplicação é feita pela IA com o perfil administrativo, por autorização do Gabriel, e não à mão por ele. O boundary hoje só libera `sns:Publish` nos tópicos e, no DynamoDB e no S3, as ações já usadas pelas funções. A leitura nova do aquecimento exige mudança nele (design D10).
- **Convenção de nomes do bootstrap.** Alarmes, dashboard e tópico precisam de nome explícito no padrão `feedback-<nome>-<ambiente>` ou `feedback-<ambiente>-*`, como manda a seção "Regras que os próximos templates precisam seguir" do README.
- **Prova sem alterar função.** Nenhuma prova altera o `$LATEST` ou o código das funções na AWS. Os cenários de falha seguem o "Método de prova" acima. A linha de tempo esgotado gravada no stream de teste segue o formato documentado (design D3), e o texto real dessa linha não é observado na stack.

## Rastreabilidade

| Requisito | Capability | Origem |
|---|---|---|
| Tópico de alarmes por ambiente | observabilidade | `project.md`, Stack (CloudWatch Alarms); decisão do tópico de alarmes de 05/11/2026; D1 |
| Notificação dos alarmes | observabilidade | `project.md`, Stack; `add-fundacao-projeto`, design (Risks, tópico criptografado); D1 |
| Alarme de falha do registro | observabilidade | `project.md`, Stack; `add-registro-avaliacao`, spec (falha interna) e `qa.md` (observação I); D2, D4, D14 |
| Alarme de falha da notificação | observabilidade | Backlog, achados do QA (alarme da notificação); `add-notificacao-critica`, `qa.md` (observação H) e design D5; D2, D10, D14 |
| Alarme de falha do relatório | observabilidade | `project.md`, Stack; `add-relatorio-semanal`, spec (falhas); D2, D4, D14 |
| Ausência de tráfego não dispara alarme | observabilidade | `project.md`, Restrições (demonstração com pouco uso); D3 |
| Isolamento por ambiente | observabilidade | `fundacao-cloud`, Provisionamento por ambiente; D5 |
| Invocação sem falha sem log em WARN | observabilidade | Backlog, achados do QA (WARN); `add-notificacao-critica`, `qa.md` (observação K) |
| Dashboard por ambiente | observabilidade | `project.md`, Stack (Dashboard); decisão do Gabriel de 04/11/2026 sobre a duração de restauração (`add-registro-avaliacao`, design D6); D9 |
| Retenção de logs por ambiente | observabilidade | `add-registro-avaliacao`, design D11 (retenção por ambiente adiada para este change); D13 |
| Memória das funções | observabilidade | Backlog, achados do QA (aquecimento e nova medição); D11 |
| Aquecimento dos clientes do SDK | observabilidade | Backlog, achados do QA (aquecimento); `add-registro-avaliacao`, design D7 e incidente de 04/11/2026; D7 |
| Aquecimento sem efeito em dados | observabilidade | Backlog, achados do QA (aquecimento); D7 |
| Medição da primeira chamada depois da restauração | observabilidade | Backlog, achados do QA ("medir de novo depois"); D8, D11 |
| Custo dentro do nível gratuito | observabilidade | `project.md`, Restrições (Free Tier); D6, D11 |
| Empacotamento das funções no CI | entrega-continua | `entrega-continua`, Integração contínua em pull requests; D12 |
| Tópico de alarmes sem criptografia e sem dado do aluno | seguranca | Decisão do tópico de alarmes de 05/11/2026; `project.md`, Convenções (dado do aluno) |
| Tópico de alarmes só aceita TLS | seguranca | Decisão do tópico de alarmes de 05/11/2026 |
| Publicação no tópico de alarmes restrita ao CloudWatch da conta | seguranca | Decisão do tópico de alarmes de 05/11/2026; `add-fundacao-projeto`, design (privilégio mínimo) |
| Dashboard sem dado do aluno | seguranca | `project.md`, Convenções (dado do aluno); `add-registro-avaliacao`, design D9 |
| Permissões das funções restritas ao ambiente | seguranca | `add-fundacao-projeto`, design D9 (boundary); backlog (aquecimento) |
| Implantação pela role do ambiente | seguranca | `entrega-continua`, Autenticação sem credenciais estáticas; `add-fundacao-projeto`, design D4 e D9 |

Requisitos que já existem e não se repetem aqui: logs em JSON sem dado do aluno em INFO (`registro-avaliacao`, `notificacao-critica`, `relatorio-semanal`), criptografia em repouso da tabela, do tópico de administradores e do bucket, bucket sem acesso público e sem TLS negado (`fundacao-cloud`), OIDC sem credencial estática e role por ambiente (`entrega-continua`). X-Ray ativo e boundary nas roles estão nos designs de `add-fundacao-projeto` e `add-registro-avaliacao` e continuam como estão.

## Decisões fora do project.md

| Id | Decisão | Motivo | Origem |
|---|---|---|---|
| D1 | Os alarmes publicam num tópico próprio por ambiente, assinado pelo mesmo endereço de `EmailAdmin`, e nunca no tópico de administradores | O CloudWatch não publica no tópico com `alias/aws/sns`. Reaproveitar o secret `EMAIL_ADMIN` evita configuração nova no GitHub | Tópico próprio: decisão de 05/11/2026. Assinatura: aprovada pelo Gabriel em 05/11/2026 |
| D2 | Uma única falha basta para o alarme entrar em ALARM | O volume da demonstração é baixo. Exigir várias falhas esconderia o caso real | Recomendação do PO |
| D3 | Período sem invocação não é falha | O relatório roda uma vez por semana e o tráfego de dev é esporádico. Sem isso, os alarmes ficariam em estado de falha por falta de dado | Recomendação do PO |
| D4 | Entrada rejeitada (400 no registro, data inválida no acionamento manual do relatório) não dispara alarme | É erro de quem chama, não falha da plataforma, e já tem métrica própria | Recomendação do PO |
| D5 | Métricas, alarmes e dashboard de um ambiente refletem só aquele ambiente | As métricas do Powertools levam só `Service`, igual nos três ambientes. Sem separação, uma falha em dev dispararia o alarme de prod. A separação é feita só no `Globals`, por namespace por ambiente, sem mudança nos handlers (design D6) | Aprovada pelo Gabriel em 05/11/2026 |
| D6 | No máximo 3 métricas de alarme e 1 dashboard por stack. Com dev, hml e prod implantados ao mesmo tempo e o alarme de cobrança que já existe em `us-east-1`, o total fecha em 10 de 10 métricas de alarme e 3 de 3 dashboards, sem folga (design D8) | O `project.md` exige Free Tier, e os três ambientes coexistem durante a gravação do vídeo | Aprovada pelo Gabriel em 05/11/2026 |
| D7 | O aquecimento não grava dado e, se falhar, não impede a publicação da versão | Gravar item, publicar mensagem ou criar objeto no deploy geraria dado falso e e-mail. A publicação já caiu uma vez por erro nos hooks de priming (incidente de 04/11/2026) | Recomendação do PO |
| D8 | Meta, não critério de aprovação: a primeira chamada a cada serviço depois da restauração fica abaixo da metade do tempo máximo configurado no cliente (4 s no registro, 2,5 s na notificação, 5 s no relatório), em ao menos duas restaurações por função. Se não atingir, o QA registra o valor medido e o change segue | O backlog pede nova medição sem meta. A metade deixa folga clara para os limites de 8 s, 5 s e 10 s já aprovados. Duas restaurações porque o registro mostrou 8 s de duração na primeira e 1,2 s na segunda | Aprovada pelo Gabriel como meta em 05/11/2026 |
| D9 | O dashboard mostra a duração de restauração do SnapStart no lugar do cold start | A métrica `ColdStart` do Powertools não sai com SnapStart | Decisão do Gabriel de 04/11/2026 |
| D10 | O descarte de item inválido na notificação também dispara o alarme | O descarte é registrado em ERROR e só acontece por escrita manual na tabela, o que merece atenção | Derivada de `add-notificacao-critica`, design D5 |
| D11 | Memória de 1024 MB no `Globals`, no lugar de 512 MB, com nova medição da restauração e da primeira chamada | A CPU do Lambda é proporcional à memória, e a primeira chamada depois da restauração ficou perto dos limites | Decisão do Gabriel de 05/11/2026 |
| D12 | O CI de pull request roda `sam build` | Hoje o CI roda `mvn -B verify` e `sam validate --lint`, e um template que referencia função sem código só falha no deploy | Decisão do Gabriel de 05/11/2026 |
| D13 | Retenção dos logs das funções em 30 dias em prod e 14 dias em dev e hml | Prod guarda mais histórico para investigação. Dev e hml mantêm os 14 dias atuais | Decisão do Gabriel de 05/11/2026 |
| D14 | Prova de filtro, métrica e alarme por linha gravada com `aws logs put-log-events` num log stream de teste, e do caminho até o e-mail com `aws cloudwatch set-alarm-state`, sem alterar o `$LATEST` nem o código das funções na AWS | Alterar função na conta para provocar falha foi vetado. A linha gravada pelo handler em cada falha é provada pelos testes unitários | Decisão do Gabriel de 05/11/2026 |
