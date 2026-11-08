# QA: add-fundacao-projeto

Tarefa 6.4. Conferência dos cenários das specs contra a stack implantada, feita em 04/11/2026, somente com leitura na AWS (describe, get, list) e no GitHub (`gh api` GET, `gh run view`, `gh secret list`, `gh variable list`). Nenhum item, objeto, PR ou configuração foi criado ou alterado. O ID da conta e o e-mail aparecem como `<conta>` e `<email>`.

## Resultado

Atualizado em 07/11/2026 pela seção "Release 1.0.0 em hml (07/11/2026)", no fim deste arquivo. As matrizes abaixo mantêm o texto de 04/11/2026, e a mudança de status de cada cenário está na seção nova.

| Status | 04/11/2026 | 07/11/2026 |
|---|---|---|
| ok (executado) | 5 | 8 |
| ok (por configuração) | 8 | 7 |
| não coberto | 2 | 0 |
| falha | 0 | 0 |
| **Total de cenários** | **15** | **15** |

**O change está conforme desde 07/11/2026:** nenhum cenário sem cobertura e nenhuma falha. Os 7 cenários por configuração seguem sem exercício, como descrito na coluna Limite de cada um.

Significado dos status:
- **ok (executado):** o comportamento foi observado de fato.
- **ok (por configuração):** o recurso real garante o comportamento, mas ele não foi exercitado. Não equivale a teste.
- **não coberto:** não há evidência do que o cenário pede. Conta como falha.
- **falha:** o comportamento observado contraria o cenário.

## Verificações locais

| Verificação | Resultado |
|---|---|
| `mvn -B verify` | BUILD SUCCESS, sem testes a executar: os módulos só têm `pom.xml` |
| `sam validate --lint` em `template.yaml` | válido |
| `sam validate --lint` em `infra/bootstrap/github-oidc.yaml` | válido |
| `openspec validate add-fundacao-projeto --strict` | válido |

O build local usa Microsoft OpenJDK 25 (25.0.5.0.2). O CI e o deploy usam Corretto 25 (25.0.5+0.10, conforme os logs). O resultado local não comprova o build em Corretto: essa comprovação vem dos logs do CI.

## Matriz: fundacao-cloud

| Cenário | Status | Evidência | Limite |
|---|---|---|---|
| Deploy de um ambiente | ok (executado) | Stack `feedback-dev` em `CREATE_COMPLETE`, parâmetro `Stage=dev`. Recursos nomeados: `feedback-avaliacoes-dev`, `feedback-admins-dev`, `feedback-relatorios-<conta>-dev`. Assinatura e política de bucket não têm nome próprio. | Só `Stage=dev` foi implantado. |
| Ambientes não compartilham recursos | **não coberto** | `feedback-hml` não existe (`describe-stacks` retorna `does not exist`), então a coexistência não pôde ser observada. Há apenas uma tabela, um tópico e um bucket na região, todos de dev. O template usa `${Stage}` nos três nomes, o que é indício, não evidência implantada. | Fecha com a primeira stack de hml. |
| Consulta por dia | ok (por configuração) | Tabela `ACTIVE` e GSI `por-data` `ACTIVE`, com partição `dataEnvio`, ordenação `criadoEm` e projeção `ALL`. A chave de partição garante o filtro por valor exato. | Nenhuma consulta foi executada e a tabela tem 0 itens. O formato yyyy-MM-dd e o dia gravado dependem de `add-registro-avaliacao`, que ainda não existe. |
| Evento de inserção | ok (por configuração) | `StreamEnabled: true`, `StreamViewType: NEW_IMAGE`. `describe-stream` retorna `ENABLED`, `NEW_IMAGE`. | Nenhum item foi inserido e nenhum registro do Stream foi lido. |
| Bucket público negado | ok (por configuração) | `get-public-access-block` com as quatro opções em `true` no bucket real. `get-bucket-policy-status` retorna `IsPublic: false`. | Nenhuma tentativa foi feita. Ver a observação O1: a negação de ACL pode não vir do Block Public Access. Na conta não há bloqueio configurado, só no bucket. |
| Acesso sem TLS negado | ok (por configuração) | `get-bucket-policy` retorna a declaração `NegaAcessoSemTls`: Deny, `Principal: *`, `s3:*`, no bucket e nos objetos, com `aws:SecureTransport: false`. | Nenhuma requisição sem TLS foi feita. |
| Assinatura criada no deploy | **não coberto** | Assinatura de protocolo `email` criada no deploy (`AssinaturaEmailAdmin` em `CREATE_COMPLETE`), estado `PendingConfirmation` em 04/11/2026, `SubscriptionsPending: 1`, `SubscriptionsConfirmed: 0`. Esse estado mostra que o SNS aceitou a assinatura. | O cenário pede que o endereço receba o e-mail, e só o Gabriel pode confirmar o recebimento (tarefa 6.3). Com a confirmação, passa a ok (executado). Ver a observação O2. |

Criptografia em repouso, exigida pelo requisito de proteção de dados: tabela com `SSEType: KMS` e chave `KeyManager: AWS`, tópico com `alias/aws/sns` e chave `KeyManager: AWS`, bucket com `AES256`. Conferido nos recursos reais.

## Matriz: entrega-continua

| Cenário | Status | Evidência | Limite |
|---|---|---|---|
| PR com teste quebrado | ok (por configuração) | `ci.yml` roda `mvn -B verify` em PR para `develop`. A proteção de `develop` exige o check `ci`, com `enforce_admins: true`. | Nenhum PR com falha foi aberto, e ainda não existe teste no repositório. O caminho de falha nunca rodou. |
| PR válido | ok (executado) | PR #1, execução 41766232000, commit 3293da5: check `ci` com `success`, `BUILD SUCCESS` e os dois templates válidos nos logs, em Corretto 25. | "Passa nos testes" foi trivial: os logs mostram `No tests to run` em todos os módulos. |
| Merge em develop | ok (executado) | Merge ae31ed3 disparou a execução 41766334040 (evento `push`). A tentativa 2 terminou em `Successfully created/updated stack - feedback-dev`. Os eventos da stack mostram a sessão `github-deploy-feedback-dev/deploy-41766334040`, o que liga o recurso à execução. O conteúdo do template implantado corresponde ao de `template.yaml` em ae31ed3, normalizado pelo SAM. | A stack foi **criada**, não atualizada: o estado é `CREATE_COMPLETE`, e a atualização de uma stack existente só será vista no próximo merge. A tentativa 1, disparada pelo push, falhou por falta do secret `EMAIL_ADMIN`, antes do `sam deploy`, como previsto em D10. A tentativa 2 foi uma reexecução do mesmo commit. |
| Branch de release | ok (por configuração) | `deploy.yml` filtra `release/**` e mapeia `refs/heads/release/*` para `hml`. Ambiente `hml` com `AWS_ROLE_ARN` e `EMAIL_ADMIN`. Trust policy da role de hml conferida. Políticas de hml iguais às de dev, tirando o nome do ambiente. `samconfig.toml` tem a seção `hml`. | Não existe branch `release/*`, nenhuma execução e a stack `feedback-hml` não existe. Os filtros nunca foram exercitados para esse caminho. |
| Branch de feature | ok (executado) | Três pushes em `feature/add-fundacao-projeto` no log de atividade do repositório, o último com 3293da5, commit que já continha o mesmo `deploy.yml` de `develop`. A API retorna 0 execuções para `event=push` nessa branch, e a única execução de Deploy é a de `develop`. | Observado em uma branch. A ausência de deploy depende do filtro do arquivo no commit enviado. Ver a observação O3. |
| Merge em main | ok (por configuração) | `deploy.yml` mapeia `refs/heads/main` para `prod`. O ambiente `prod` tem a regra `required_reviewers` com `g-andradd` e `prevent_self_review: false`, como autorizado. Role de prod e `AWS_ROLE_ARN` conferidos. | Nenhum commit chegou a `main` depois da criação: ela ainda está em f977726, sem os workflows, que chegam com o merge da release. Nem a pendência nem a aprovação foram observadas, e `feedback-prod` não existe. Ver a observação O4. |
| Role de outro ambiente | ok (por configuração) | Trust policy real da role de prod: uma única declaração, `StringEquals` em `aud` e em `sub = repo:g-andradd@59920810/fiap-tc4-feedback-serverless@1445012951:environment:prod`. O formato imutável está confirmado em `actions/oidc/customization/sub`. A assunção da role correta foi observada: o deploy em dev rodou como `github-deploy-feedback-dev`. | A negação não foi exercitada: nenhum job tentou assumir a role de outro ambiente. |
| Repositório sem secrets AWS | ok (executado) | A inspeção é o próprio cenário. Secrets e variáveis no nível do repositório: nenhum. Secrets de Dependabot e Codespaces: 0. Nos três ambientes: secret `EMAIL_ADMIN` e variável `AWS_ROLE_ARN` (um ARN, não uma chave). Os workflows não usam `aws-access-key-id` nem `aws-secret-access-key`. Busca por chaves nos arquivos versionados: nenhuma ocorrência. A conta não tem usuários IAM. | O GitHub não expõe valores de secrets: a conferência é pelos nomes. |

## Defeitos

### D1. Role de deploy sem permissão de tag no Stream (severidade baixa)

O evento da stack `TabelaAvaliacoes` em 2026-11-04T03:22:54Z registra:

```
Encountered a permissions error performing a tagging operation, please add required tag permissions.
Retrying request without including tags. [...] is not authorized to perform: dynamodb:TagResource on resource:
arn:aws:dynamodb:sa-east-1:<conta>:table/feedback-avaliacoes-dev/stream/2026-11-04T03:22:31.182
```

A declaração `TabelasDoAmbiente` de `infra/bootstrap/github-oidc.yaml` cobre as tabelas, mas não `table/.../stream/*`. A criação da stack continuou. A tabela ficou com as tags `projeto` e `ambiente`, e o Stream ficou sem tags (`list-tags-of-resource` retorna vazio). As políticas de hml e prod são iguais, então o aviso deve se repetir lá. A correção passa pelo bootstrap, aplicado manualmente (D4). Nenhum cenário é afetado.

**Situação:** a statement `TagsDeStreamDoAmbiente` foi adicionada a `infra/bootstrap/github-oidc.yaml`. O defeito só fecha depois de reaplicar o bootstrap e de ver um deploy sem esse aviso. O Stream atual pode continuar sem tags, porque um deploy sem mudança na tabela não o atualiza.

**Fechado em 07/11/2026:** a criação de `feedback-hml` pela role `github-deploy-feedback-hml` não registrou o aviso, e o Stream de hml tem as tags `projeto` e `ambiente`. Ver "Release 1.0.0 em hml (07/11/2026)".

## Observações

Não reprovam cenário, mas ficam registradas para decisão.

- **O1. Mecanismo da negação no bucket.** O cenário diz que a operação é negada "pelo Block Public Access". O bucket também está com `BucketOwnerEnforced`, que desliga as ACLs. Em uma tentativa de ACL pública, o erro pode vir desse controle, e não do Block Public Access. A negação continua garantida, mas o texto do cenário pode não corresponder ao erro real. Só um teste de escrita esclarece.
- **O2. Assinatura de e-mail.** O tópico mostra `SubscriptionsDeleted: 1`, sem explicação nos dados consultados: a stack foi criada uma única vez e a tentativa 1 parou antes do `sam deploy`. O CloudTrail não foi consultado. Além disso, o SNS remove assinaturas pendentes depois de um prazo, e um novo deploy sem mudança no template não as recria. O prazo não foi conferido na documentação nesta rodada.
- **O3. Ambientes sem restrição de branch.** `deployment_branch_policy` é `null` nos três ambientes e a trust policy restringe só o ambiente. Quem tem escrita no repositório pode rodar um workflow de qualquer branch com `environment: dev` ou `hml` e assumir a role. Em `prod` resta a aprovação manual. O projeto tem um único autor, então o risco atual é baixo.
- **O4. Bypass em prod.** O ambiente `prod` está com `can_admins_bypass: true`. Um administrador pode implantar sem aguardar a aprovação. Hoje o único administrador é o próprio revisor, então continua sendo uma ação manual do Gabriel.
- **O5. Proteção com `strict: false`.** O check `ci` não exige que a branch do PR esteja atualizada com a base. Um PR verde e defasado pode ser mergeado.
- **O6. Drift não verificado.** Os recursos estão com `NOT_CHECKED`, porque a detecção de drift inicia uma operação na AWS. A comparação foi feita diretamente nos recursos.
- **O7. Acentos nas descrições.** Na leitura pela CLI, os textos acentuados do bootstrap e as descrições dos outputs vêm com `?`. Tirando esses caracteres, o template do bootstrap implantado é idêntico ao do repositório. A origem não foi apurada, e não há efeito funcional.

## Decisões do Gabriel sobre as observações (04/11/2026)

A matriz acima foi feita antes destas decisões e continua como estava. O texto de O3, O4 e O5 descreve o que foi observado na data do QA.

### O3: resolvida

O devops configurou a deployment branch policy de cada ambiente via `gh`:

| Ambiente | Branches aceitas | Regras depois da mudança |
|---|---|---|
| `dev` | `develop` | `branch_policy` |
| `hml` | `release/*` | `branch_policy` |
| `prod` | `main` | `required_reviewers` (`g-andradd`, `prevent_self_review: false`) e `branch_policy` |

Evidência: `gh api repos/g-andradd/fiap-tc4-feedback-serverless/environments/<ambiente>` retorna `custom_branch_policies: true` e `protected_branches: false` nos três, e `.../environments/<ambiente>/deployment-branch-policies` retorna a lista da tabela. A aprovação manual de `prod` foi preservada.

Limite: conferido pela API. Nenhum workflow rodou depois da mudança, então nem o caminho permitido nem a recusa de uma branch fora da lista foram observados. O próximo merge em `develop` exercita o caminho permitido em dev.

### O4 e O5: aceitas

Ficam como estão, por decisão consciente. O documentador leva as duas a `docs/seguranca.md` no change de documentação.

- **O4, bypass de administrador:** `can_admins_bypass` continua `true`. A conferência de 04/11/2026 mostra esse valor nos três ambientes, não só em `prod`. O único administrador é o próprio revisor, em um projeto individual.
- **O5, `strict: false`:** a proteção continua sem exigir que a branch do PR esteja atualizada com a base. Um PR verde e defasado pode ser mergeado.

## Testes e coleção Postman

Nenhum teste foi adicionado a `src/test`: este change não tem código Java, e os cenários dependem de infraestrutura real, cujo exercício exigiria escrita, fora do limite desta validação. Os cenários pendentes estão na tabela de status acima, e não foram tratados como cobertos.

Nenhuma requisição foi adicionada ao Postman: o change não toca a API.

## Release 1.0.0 em hml (07/11/2026)

QA de fumaça da release 1.0.0 na stack `feedback-hml`, com `feedback-dev` no ar ao mesmo tempo. Feito no checkout principal, branch `release/1.0.0`, commit `b730a59`, sem nenhuma escrita no Git.

**Validade da evidência:**
- **Deploy:** execução 42335524756 do workflow Deploy, evento `push` em `release/1.0.0`, commit `b730a59`, `success`, das 03:46:41 às 03:51:47 UTC. Jobs `resolver-ambiente` e `deploy` com `success`. A stack foi de `REVIEW_IN_PROGRESS` às 03:48:35 a `CREATE_COMPLETE` às 03:51:37 UTC. O CloudTrail mostra `CreateTopic`, `Subscribe` e `SetTopicAttributes` feitos pela sessão `github-deploy-feedback-hml/deploy-42335524756`.
- **Observação:** das 03:57 às 05:20 UTC de 07/11/2026, ou seja, das 00:57 às 02:20 em Brasília, e de novo das 05:50 às 05:54 UTC (02:50 às 02:54 em Brasília) para o item 4, depois da confirmação da assinatura de alarmes de hml. Horários em UTC, vindos do CloudWatch, do DynamoDB e do CloudTrail. O relógio da máquina estava cerca de 7 s adiantado em relação à AWS, como no QA de 06/11.
- **Escritas na conta, todas permitidas pela regra desta rodada:** 2 avaliações registradas pela API de hml (201), 1 invocação do alias `live` do relatório de hml e 1 log stream `qa-observabilidade-20261107` no grupo do registro de dev, com 1 linha (tarefa 8.2 de `add-observabilidade-seguranca`). A crítica passou pelo Stream e invocou a notificação de hml uma vez. 1 `set-alarm-state` no alarme do registro de hml, às 05:50:37 (item 4). Nada foi apagado. Nenhuma configuração, código, versão ou `$LATEST` foi alterado. Nenhum alarme, dashboard ou filtro foi criado. O restante foi leitura com o perfil `tc4-admin`.
- **Saídas brutas:** guardadas fora do repositório, no scratchpad da sessão. O ID da conta aparece como `<conta>` e a URL da API como `<url>`.

### Mudança de status dos cenários

| Cenário | Spec | Antes | Agora | Evidência |
|---|---|---|---|---|
| Ambientes não compartilham recursos | `fundacao-cloud` | não coberto | **ok (executado)** | Item 6 abaixo |
| Assinatura criada no deploy | `fundacao-cloud` | não coberto | **ok (executado)** | A assinatura `email` do tópico de administradores está confirmada nos dois ambientes: `feedback-admins-dev` (tarefa 6.3; `SubscriptionsConfirmed` 1 e `SubscriptionsPending` 0 em 07/11) e `feedback-admins-hml` (`SubscriptionsConfirmed` 1 e `SubscriptionsPending` 0; criada às 03:48:52 e já confirmada às 04:07). A confirmação só acontece pelo link do e-mail de confirmação, então o endereço recebeu esse e-mail. A observação O2 continua sem explicação para o `SubscriptionsDeleted` de 04/11 |
| Branch de release | `entrega-continua` | ok (por configuração) | **ok (executado)** | O push em `release/1.0.0` disparou a execução 42335524756, que criou `feedback-hml` pela role de hml. Limite: a stack foi **criada**, não atualizada, como aconteceu com dev em 04/11 |

### 1. Stack de hml: ok, com uma pendência na assinatura de alarmes

**Outputs:** os 17 outputs existem, e todos levam o sufixo `hml`: `ApiAvaliacoesUrl` (`<url>`), `TabelaAvaliacoesNome` `feedback-avaliacoes-hml`, `TabelaAvaliacoesArn`, `TabelaAvaliacoesStreamArn` (`.../table/feedback-avaliacoes-hml/stream/2026-11-07T03:48:52.669`), `TopicoAdminsNome` `feedback-admins-hml`, `TopicoAdminsArn`, `TopicoAlarmesArn` (`feedback-alarmes-hml`), `BucketRelatoriosNome` `feedback-relatorios-<conta>-hml`, `BucketRelatoriosArn`, `PainelNome` `feedback-painel-hml`, e o nome e o ARN do alias `live` de cada função. Parâmetros: `Stage` `hml` e `EmailAdmin`.

**Funções**, `get-function-configuration` no alias `live`:

| Função | Versão | `State` | `OptimizationStatus` | `MemorySize` | `Timeout` | `POWERTOOLS_METRICS_NAMESPACE` |
|---|---|---|---|---|---|---|
| `feedback-registrar-avaliacao-hml` | 1 | `Active` | `On` | 1024 | 10 | `FeedbackServerless/hml` |
| `feedback-notificar-critica-hml` | 1 | `Active` | `On` | 1024 | 30 | `FeedbackServerless/hml` |
| `feedback-gerar-relatorio-semanal-hml` | 1 | `Active` | `On` | 1024 | 120 | `FeedbackServerless/hml` |

O log da publicação tem uma linha `Cliente aquecido` por cliente, sem WARN: DynamoDB `DescribeTable` no registro (1.919 ms), SNS `GetTopicAttributes` na notificação (1.717 ms), e DynamoDB `Query`, S3 `HeadBucket` e SNS `GetTopicAttributes` no relatório (1.681, 243 e 162 ms). Ver a observação Q.

**Retenção:** 14 dias em `/aws/lambda/feedback-registrar-avaliacao-hml`, `/aws/lambda/feedback-notificar-critica-hml` e `/aws/lambda/feedback-gerar-relatorio-semanal-hml`.

**Alarmes**, `describe-alarms` antes do tráfego:

| Alarme | Estado | Métrica | `AlarmActions` | `OKActions`, `InsufficientDataActions` |
|---|---|---|---|---|
| `feedback-falha-registrar-avaliacao-hml` | OK desde 03:50:06 | `FeedbackServerless/hml` `FalhasRegistrarAvaliacao` | só `feedback-alarmes-hml` | vazias |
| `feedback-falha-notificar-critica-hml` | OK desde 03:49:46 | `FalhasNotificarCritica` | só `feedback-alarmes-hml` | vazias |
| `feedback-falha-gerar-relatorio-semanal-hml` | OK desde 03:50:16 | `FalhasGerarRelatorioSemanal` | só `feedback-alarmes-hml` | vazias |

Os três com `Period` 60, 5 períodos, 1 ponto, limiar 1 e `notBreaching`, iguais aos de dev. Os filtros dos três grupos de hml têm o padrão `?FALHA_FUNCAO ?"Task timed out" ?"Status: timeout"`, valor `1`, sem dimensão, no namespace `FeedbackServerless/hml`. A política do tópico de alarmes de hml tem `NegaAcessoSemTls` e `PublicacaoPelosAlarmesDoAmbiente`, esta com `aws:SourceArn` em `alarm:feedback-*-hml`. O tópico de alarmes não tem `KmsMasterKeyId`, e o de administradores usa `alias/aws/sns`.

**Assinaturas:**

| Tópico | Protocolo | Estado | `SubscriptionsConfirmed`, `Pending`, `Deleted` |
|---|---|---|---|
| `feedback-admins-hml` | `email` | confirmada | 1, 0, 0 |
| `feedback-alarmes-hml` | `email` | `PendingConfirmation` até 05:18; confirmada pelo Gabriel antes das 05:49 | 0, 1, 1 até 05:18; 1, 0, 0 às 05:49:52 e às 05:50:12 |

A assinatura do tópico de alarmes de hml seguia pendente em quatro leituras, às 04:07, 04:17, 04:54 e 05:18 UTC. A mensagem de confirmação foi entregue às 03:48 (`NumberOfNotificationsDelivered` 1, `NumberOfNotificationsFailed` 0). O padrão 0, 1, 1 é o mesmo que o tópico de administradores de dev mostrava em 04/11, antes da confirmação (observação O2). O Gabriel confirmou a assinatura pelo e-mail original de confirmação. O orquestrador conferiu às 05:49:52 e o QA às 05:50:12: `SubscriptionsConfirmed` 1, `SubscriptionsPending` 0 e `SubscriptionsDeleted` 0. Ver a observação R.

**Recursos e permissões de hml:** a criação não teve nenhum evento `FAILED` nem aviso de permissão ou de tag (D1). A tabela está `ACTIVE`, com `SSEType` `KMS`, Stream `NEW_IMAGE`, índice `por-data` e recuperação pontual desligada, como esperado fora de prod. O mapeamento do Stream está `Enabled`, do Stream de hml para `feedback-notificar-critica-hml:live`, com destino de falha no bucket de hml. O agendamento `feedback-relatorio-semanal-hml` está `ENABLED`, `cron(0 8 ? * MON *)` em America/Sao_Paulo, apontando para `feedback-gerar-relatorio-semanal-hml:live`. As três roles das funções de hml têm o boundary `feedback-limite-funcoes-hml`.

### 2. Registro em hml: ok

Envio com a URL do output `ApiAvaliacoesUrl` de hml (`<url>`). As descrições têm o marcador e um texto curto com acentos.

| Caso | Marcador | Nota | HTTP | `id` | `urgencia` | `dataEnvio` | `criadoEm` (UTC) |
|---|---|---|---|---|---|---|---|
| Crítica | `QA-HML-202611070417-5c8eeb` | 2 | 201 | `1649b50a-1b17-4108-a374-8337a9d1b1bc` | `CRITICA` | 2026-11-07 | 04:17:53.598 |
| Baixa | `QA-HML-202611070417-5ad554` | 9 | 201 | `e642f5ba-ba85-4e8c-8506-b9fff8e26848` | `BAIXA` | 2026-11-07 | 04:17:55.026 |

Uma primeira tentativa, às 04:17:44, não saiu da máquina: o curl recusou o caminho do arquivo do corpo. Nada foi enviado.

| Evidência | Resultado |
|---|---|
| Tabela de hml | 0 itens antes, 2 depois. Cada item tem os 6 atributos do contrato, e a descrição gravada é idêntica à enviada |
| Notificação | 1 invocação às 04:17, `Errors` 0. Uma única linha `Aviso publicado` em INFO, às 04:17:57.441, com o `id` da crítica e `messageId` `db5079e1-393d-5418-81d9-1a89918873f1`. O `id` da baixa não aparece no grupo da notificação |
| Restauração | `RESTORE_REPORT` de 617,70 ms no registro e 785,48 ms na notificação. A segunda requisição do registro reaproveitou o ambiente (36,30 ms) |
| Tópico de administradores de hml | `NumberOfMessagesPublished` 1, `NumberOfNotificationsDelivered` 1 e `NumberOfNotificationsFailed` 0 às 04:17. `PublishSize` de 161 bytes, exatamente o tamanho em UTF-8 do corpo montado com o formato de `AvisoCritico` e a descrição da crítica |

### 3. Relatório manual em hml: ok

Hoje em São Paulo era 2026-11-07. Invocação às 04:27:47 UTC, em `feedback-gerar-relatorio-semanal-hml:live`, com `{"dataReferencia":"2026-11-08"}`.

| Item | Observado |
|---|---|
| Resposta | `StatusCode` 200, sem `FunctionError`, `{"quantidadeAvaliacoes":2,"dataReferencia":"2026-11-08","chave":"relatorios/2026-11-08.txt"}`. Sem o bucket |
| Objeto | Único objeto do bucket de hml: `relatorios/2026-11-08.txt`, 568 bytes, `text/plain; charset=utf-8`, `AES256`, `LastModified` 04:27:44 |
| Conteúdo | `Ambiente: hml`, janela `2026-11-01 a 2026-11-07`, `Média das notas: 5,50`, `CRITICA: 1`, `MEDIA: 0`, `BAIXA: 1`, `2026-11-07: 2` e os outros 6 dias em 0, e `Avaliações (2)` com as duas descrições, urgências e datas. Cada marcador aparece uma vez |
| Comparação | **Idêntico byte a byte** ao texto montado com o formato do README e as duas avaliações |
| Aviso | `NumberOfMessagesPublished` 1, 1 entrega e 0 falhas às 04:27 no tópico de administradores de hml. `PublishSize` de 523 bytes, igual ao corpo esperado pelo formato do README com o bucket de hml |
| Log | `Relatório gravado` e `Relatório gerado` em INFO às 04:27:43, mais o EMF `RelatorioGerado` no namespace `FeedbackServerless/hml`. Restauração de 710,17 ms e duração de 1.695,38 ms |

### 4. Alarme de hml com `set-alarm-state`: ok

Na primeira rodada, a assinatura do tópico `feedback-alarmes-hml` estava `PendingConfirmation` (item 1), e o QA não forçou o estado: o alarme publicaria e ninguém receberia o e-mail. Depois da confirmação pelo Gabriel, o teste rodou com um único alarme. Antes, às 05:50:12, os seis alarmes de dev e hml estavam em OK.

```bash
aws cloudwatch set-alarm-state   --alarm-name feedback-falha-registrar-avaliacao-hml   --state-value ALARM   --state-reason "QA release 1.0.0 set-alarm-state: teste do caminho do alarme ate o e-mail"   --region sa-east-1
```

| Conferência | Resultado |
|---|---|
| Transição | `feedback-falha-registrar-avaliacao-hml` de OK para ALARM às 05:50:37.214 |
| Ação | `Successfully executed action arn:aws:sns:sa-east-1:<conta>:feedback-alarmes-hml` às 05:50:37.247, `actionState` `Succeeded` |
| Tópico de alarmes de hml | `NumberOfMessagesPublished` 1, `NumberOfNotificationsDelivered` 1 e `NumberOfNotificationsFailed` 0 às 05:50 |
| Outros tópicos | Nenhuma publicação no tópico de administradores de hml nem no tópico de alarmes de dev entre 05:30 e 05:53 |
| Mensagem publicada | Variantes `default`, `email` e `sms`. O `default` só tem as chaves do alarme (`AlarmName`, `AlarmDescription`, `AWSAccountId`, `NewStateValue`, `NewStateReason`, `OldStateValue`, `StateChangeTime`, `Region`, `AlarmArn`, `AlarmActions`, `OKActions`, `InsufficientDataActions`, `Trigger` e afins). `AlarmActions` só com `feedback-alarmes-hml`. O motivo é o texto do teste. Nenhuma ocorrência de `QA-HML`, dos sufixos, de `Aula`, `descricao` ou `"nota"` |
| Volta a OK | Sozinha às 05:50:41.426, 4 s depois, com o motivo `no datapoints were received for 5 periods and 5 missing datapoints were treated as [NonBreaching]`. Em dev, o mesmo teste levou de 60 a 80 s |
| Fim | Os seis alarmes em OK às 05:53 |

O alarme não tem `OKActions`, então a volta a OK não gera e-mail.

### 5. Logs de hml sem os marcadores: ok

Leitura dos três grupos de hml desde 03:45 UTC, no servidor com `filter-log-events` e no cliente:

| Grupo | Eventos | Linhas da aplicação | `QA-HML`, `5c8eeb`, `5ad554`, `Aula` | WARN, ERROR, `FALHA_FUNCAO` |
|---|---|---|---|---|
| registro | 24 | 10 em INFO: `Avaliação registrada` (2), `Cliente aquecido` (2) e 6 do SDK do X-Ray | 0 | 0 |
| notificação | 18 | 9 em INFO: `Aviso publicado` (1), `Cliente aquecido` (2) e 6 do X-Ray | 0 | 0 |
| relatório | 24 | 14 em INFO: `Relatório gravado` (1), `Relatório gerado` (1), `Cliente aquecido` (6) e 6 do X-Ray | 0 | 0 |

O restante são linhas de plataforma (`INIT_START`, `INIT_REPORT`, `RESTORE_START`, `RESTORE_REPORT`, `START`, `END`, `REPORT`) e EMF. Nenhuma linha traz `descricao` nem nota. Nenhuma métrica de falha de hml recebeu ponto.

### 6. Ambientes não compartilham recursos: ok

Com `feedback-dev` (`UPDATE_COMPLETE`, 06/11 às 19:21) e `feedback-hml` (`CREATE_COMPLETE`, 07/11 às 03:51) existindo ao mesmo tempo, por output e por `list-stack-resources`, com 36 recursos em cada stack:

| Recurso | dev | hml |
|---|---|---|
| Tabela | `feedback-avaliacoes-dev` | `feedback-avaliacoes-hml` |
| Stream | `.../feedback-avaliacoes-dev/stream/2026-11-04T03:22:31.182` | `.../feedback-avaliacoes-hml/stream/2026-11-07T03:48:52.669` |
| Tópico de administradores | `arn:aws:sns:sa-east-1:<conta>:feedback-admins-dev` | `arn:aws:sns:sa-east-1:<conta>:feedback-admins-hml` |
| Tópico de alarmes | `arn:aws:sns:sa-east-1:<conta>:feedback-alarmes-dev` | `arn:aws:sns:sa-east-1:<conta>:feedback-alarmes-hml` |
| Bucket | `feedback-relatorios-<conta>-dev` | `feedback-relatorios-<conta>-hml` |
| Dashboard | `feedback-painel-dev` | `feedback-painel-hml` |
| Agendamento | `feedback-relatorio-semanal-dev` | `feedback-relatorio-semanal-hml` |
| API | `<url>` de dev | `<url>` de hml, com outro ID |

Nenhum `PhysicalResourceId` se repete entre as duas stacks, tirando `$default`, que é o nome do estágio dentro de cada API.

**As funções de hml apontam para os recursos de hml**, pelas variáveis de ambiente do alias `live`:

| Função de hml | Variáveis |
|---|---|
| registro | `TABELA_AVALIACOES` `feedback-avaliacoes-hml`, `AMBIENTE` `hml`, `POWERTOOLS_METRICS_NAMESPACE` `FeedbackServerless/hml` |
| notificação | `TOPICO_ADMINS_ARN` `...:feedback-admins-hml`, `AMBIENTE` `hml`, namespace `FeedbackServerless/hml` |
| relatório | `TABELA_AVALIACOES` `feedback-avaliacoes-hml`, `BUCKET_RELATORIOS` `feedback-relatorios-<conta>-hml`, `TOPICO_ADMINS_ARN` `...:feedback-admins-hml`, `AMBIENTE` `hml`, namespace `FeedbackServerless/hml` |

As funções de dev seguem com as variáveis `-dev` e o namespace `FeedbackServerless/dev`. As políticas inline das três roles de hml só citam recursos `-hml` (tabela, índice, bucket e tópico de administradores) e chaves KMS da conta, sem nenhum `-dev`. O mapeamento do Stream e o agendamento de hml apontam para as funções de hml.

**A avaliação de hml não aparece em dev.** A tabela de dev continua com 43 itens, a mesma contagem do fim do QA de 06/11. O `scan` com `contains(descricao, "QA-HML")` encontrou 0 de 43. O `get-item` com os dois `id` de hml não devolveu nada. A partição `2026-11-07` do índice `por-data` de dev tem 0 itens. As três funções de dev não tiveram nenhuma invocação de 03:00 a 05:00 UTC, e o tópico de administradores de dev nenhuma publicação.

### Itens de teste deixados

| Onde | Item | Situação |
|---|---|---|
| Tabela `feedback-avaliacoes-hml` | 2 itens, `QA-HML-202611070417-5c8eeb` (nota 2) e `QA-HML-202611070417-5ad554` (nota 9), `dataEnvio` 2026-11-07 | Entram na janela do relatório agendado de hml de 09/11, se a stack ainda existir |
| Bucket `feedback-relatorios-<conta>-hml` | `relatorios/2026-11-08.txt` | Precisa ser esvaziado antes do `sam delete`, como diz o README |
| Grupo `/aws/lambda/feedback-registrar-avaliacao-dev` | stream `qa-observabilidade-20261107`, com 1 linha | Expira com a retenção de 14 dias, por volta de 21/11/2026 |

Nada foi apagado.

### E-mails que o Gabriel deve ter recebido nesta rodada

| # | UTC | Brasília | Tópico e ambiente | Assunto | Conteúdo esperado |
|---|---|---|---|---|---|
| 1 | 04:17:57 | 01:17:57 | `feedback-admins-hml`, hml | `Avaliação crítica registrada` | `Urgência: CRITICA`, `Data de envio: 2026-11-07` e a descrição com `QA-HML-202611070417-5c8eeb` |
| 2 | 04:27:43 | 01:27:43 | `feedback-admins-hml`, hml | `Relatório semanal 2026-11-08` | `Ambiente: hml`, janela `2026-11-01 a 2026-11-07`, média 5,50, `CRITICA: 1`, `MEDIA: 0`, `BAIXA: 1` e `2026-11-07: 2`. Sem descrição e sem nota |
| 3 | 04:49:29 | 01:49:29 | `feedback-alarmes-dev`, dev | `ALARM: "feedback-falha-registrar-avaliacao-dev" in South America (Sao Paulo)` | Motivo `Threshold Crossed: 1 out of the last 5 datapoints [1.0 (07/11/26 04:48:00)] ...`. Tarefa 8.2 de `add-observabilidade-seguranca` |
| 4 | 05:50:37 | 02:50:37 | `feedback-alarmes-hml`, hml | `ALARM: "feedback-falha-registrar-avaliacao-hml" in South America (Sao Paulo)` | Motivo `QA release 1.0.0 set-alarm-state: teste do caminho do alarme ate o e-mail`. Só dados do alarme, sem descrição e sem nota. Item 4 |

Nenhum e-mail de volta a OK, porque os alarmes não têm `OKActions`. Nenhum e-mail para a avaliação baixa. O e-mail 2 traz o nome do bucket, com o ID da conta, e todos trazem o rodapé do SNS com o e-mail do assinante: não copiar para arquivo versionado e desfocar no vídeo.

### Verificações locais

| Verificação | Resultado |
|---|---|
| `mvn -B verify` sobre `b730a59` | `BUILD SUCCESS`. `dominio` 171, `registrar-avaliacao` 140, `notificar-critica` 78 e `gerar-relatorio-semanal` 95 testes, com 0 falhas, 0 erros e 0 ignorados. Microsoft OpenJDK 25.0.5.0.2, definido só no comando, porque o `JAVA_HOME` do shell apontava para um JDK 21, com o qual o build falha |
| `openspec validate add-fundacao-projeto --strict` | válido |
| `sam validate --lint` | não rodado: o `template.yaml` não mudou nos commits da release |

### Observações desta rodada

- **Q. Segunda inicialização fora de invocação.** As três funções de hml rodaram o `INIT` duas vezes antes do primeiro tráfego: na publicação da versão, entre 03:49:47 e 03:50:09, e de novo entre 04:07:46 e 04:09:06, com `INIT_START` sem invocação e as mesmas linhas `Cliente aquecido`. O CloudTrail confirma as chamadas de leitura do aquecimento pelas roles das funções às 04:08:32 e 04:09:05. A versão continuou 1, nenhuma configuração mudou e nada foi gravado ou publicado: tabela com 0 itens e tópico de administradores sem publicação até 04:17. O motivo não foi apurado. Não contraria nenhum cenário, porque o aquecimento só lê.
- **R. Assinatura de alarmes de hml pendente.** O orquestrador informou que o Gabriel confirmou as assinaturas dos dois tópicos de hml, mas a de alarmes seguia `PendingConfirmation` às 05:18 UTC. A de administradores está confirmada. Pode ter sido confirmado só um dos dois e-mails. O SNS descarta a assinatura pendente depois de um prazo, e um deploy sem mudança no template não a recria. **Resolvida:** o Gabriel confirmou a assinatura pelo e-mail original, conferida às 05:49:52 UTC, e o item 4 rodou às 05:50.
