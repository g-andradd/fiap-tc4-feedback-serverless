# Segurança e governança

Controles de segurança da nuvem, governança do repositório e dos ambientes, e o registro único dos riscos aceitos. Cada seção diz o que está configurado e onde isso foi conferido.

`<amb>` é `dev`, `hml` ou `prod`, o valor do parâmetro `Stage`. O ID da conta aparece como `<ID_DA_CONTA>`. As permissões de cada função, statement por statement, estão em [docs/funcoes.md](funcoes.md). A configuração dos alarmes está em [docs/monitoramento.md](monitoramento.md).

Fontes principais: `infra/bootstrap/github-oidc.yaml` (provedor OIDC, roles de deploy e boundary), `template.yaml` (recursos de cada ambiente), `.github/workflows/ci.yml` e `deploy.yml`, e os `qa.md` dos cinco changes em `openspec/changes/`.

## Resumo

| Controle | Como | Conferido |
|---|---|---|
| Deploy sem credencial estática | OIDC do GitHub, uma role por ambiente | dev 04/11, hml 07/11, prod 08/11/2026 |
| Privilégio mínimo no deploy | Permissões só nos nomes do ambiente, negações explícitas no bootstrap | dev 04/11 e 06/11/2026 |
| Teto das roles de função | Boundary `feedback-limite-funcoes-<amb>` | dev 06/11, hml 07/11/2026 |
| Criptografia em repouso | Tabela com KMS, tópico de administradores com `alias/aws/sns`, bucket com SSE-S3 | dev 04/11, hml 07/11/2026 |
| TLS obrigatório | Política do bucket e do tópico de alarmes | dev 04/11 e 06/11/2026 |
| Dado do aluno fora dos logs | Regra no código, nível INFO | dev 05/11 e 06/11, hml 07/11/2026 |
| Isolamento entre ambientes | Stack, nomes, role, boundary e namespace por ambiente | dev e hml juntos, 07/11/2026 |
| Governança do GitHub | `ci` obrigatório, branch por ambiente, aprovação em prod | leitura de 10 e 11/11/2026 (UTC) |

## Acesso do GitHub Actions à AWS por OIDC

O pipeline não tem chave de acesso da AWS. O job de deploy pede ao GitHub um token OIDC e troca esse token por uma credencial temporária da role do ambiente.

**Provedor e roles.** O bootstrap (`infra/bootstrap/github-oidc.yaml`, stack `feedback-bootstrap-oidc`) cria o provedor `token.actions.githubusercontent.com`, com audiência `sts.amazonaws.com`, e uma role por ambiente, `github-deploy-feedback-<amb>`. Ele é aplicado com credencial administrativa, fora do pipeline, para o pipeline não conseguir alterar as próprias permissões.

**Trust policy.** Cada role tem uma única declaração, `sts:AssumeRoleWithWebIdentity`, com `StringEquals` em dois claims:

| Claim | Valor exigido |
|---|---|
| `aud` | `sts.amazonaws.com` |
| `sub` | `repo:g-andradd@59920810/fiap-tc4-feedback-serverless@1445012951:environment:<amb>` |

- O `sub` está no formato imutável do GitHub, com o dono e o repositório identificados pelo ID numérico, que é público. Um repositório recriado com o mesmo nome por outra pessoa não casa. A leitura de `actions/oidc/customization/sub` em 10 e 11/11/2026 (UTC) devolveu `use_immutable_subject: true`. O formato legado, só com nomes, não é aceito (decisão do Gabriel de 03/11/2026, design de `add-fundacao-projeto`, D3).
- O `sub` termina no ambiente do GitHub. Um job no ambiente `dev` só assume a role de dev. A trust policy real da role de prod foi conferida em 04/11/2026 (`qa.md` de `add-fundacao-projeto`, cenário "Role de outro ambiente").
- A sessão dura no máximo 1 hora (`MaxSessionDuration: 3600`) e se chama `deploy-<id da execução>`. No CloudTrail, a criação de hml aparece pela sessão `github-deploy-feedback-hml/deploy-42335524756`.

**Workflows.**
- `deploy.yml` pede só `id-token: write` e `contents: read`. O job que decide o ambiente pela branch roda com `permissions: {}`. O job de deploy declara `environment:` com o ambiente resolvido, e é isso que entra no `sub`.
- `ci.yml` roda nos PRs com `contents: read` e nenhuma credencial. `sam build` e `sam validate --lint` não chamam a AWS.
- Os dois fazem checkout com `persist-credentials: false`.

**Secrets e variáveis.** Leitura dos nomes em 10 e 11/11/2026 (UTC), com `gh api` GET:

| Escopo | Secrets | Variáveis |
|---|---|---|
| Repositório | nenhum | nenhuma |
| Ambientes `dev`, `hml` e `prod` | `EMAIL_ADMIN` | `AWS_ROLE_ARN`, que é um ARN, não uma chave |

Em 04/11/2026, a conta não tinha usuários IAM e os workflows não usavam `aws-access-key-id` nem `aws-secret-access-key` (`qa.md` de `add-fundacao-projeto`, cenário "Repositório sem secrets AWS").

**E-mail dos administradores.** O endereço chega pelo secret `EMAIL_ADMIN` do ambiente, passa ao passo de deploy por variável de ambiente, para ficar mascarado no log, e entra no parâmetro `EmailAdmin`, que tem `NoEcho`. Se o secret faltar, o deploy para antes do `sam deploy`, como aconteceu na primeira tentativa em dev em 04/11/2026. O endereço não está em nenhum arquivo do repositório.

## Roles de deploy por ambiente, negações e boundary

Cada role de deploy tem duas políticas gerenciadas, porque o conjunto passa da cota de 10.240 caracteres de política inline por role:

| Política | Cobre |
|---|---|
| `github-deploy-feedback-<amb>-recursos` | A stack `feedback-<amb>` e a stack gerenciada do SAM, artefatos no bucket do SAM, bucket, tabela, tópicos, alarmes, dashboards e chaves gerenciadas pela AWS |
| `github-deploy-feedback-<amb>-aplicacao` | Funções, versões e aliases, mapeamento do Stream, API, agendamento, roles de função e grupos de log |

**Escopo pelo nome.** Toda escrita vale só para recursos com o nome do ambiente: `feedback-<nome>-<amb>` ou o nome gerado pela stack, `feedback-<amb>-*`. As roles e políticas de deploy se chamam `github-deploy-feedback-<amb>` justamente para não casar com esses padrões. No bucket de artefatos do SAM, que é um só na conta, cada role grava só no próprio prefixo, `feedback-<amb>/`.

**Negações explícitas.** Um `Deny` vale mesmo que algum `Allow` seja ampliado por engano:

| Statement | Nega | Onde |
|---|---|---|
| `NegaAlterarBootstrap` | `iam:*` | As roles e políticas `github-deploy-feedback-*`, os boundaries `feedback-limite-funcoes-*` e o provedor OIDC |
| `NegaAlterarStackDeBootstrap` | `cloudformation:*` | A stack `feedback-bootstrap-oidc` |
| `NegaEscritaEmApiDeOutroAmbiente` | Escrita no API Gateway | APIs com a tag `ambiente` de outro ambiente |

**Ficaram de fora de propósito:** `cloudformation:DeleteStack`, porque a remoção de uma stack é feita com o perfil administrativo; `iam:CreateServiceLinkedRole` ([R40](#registro-de-riscos-aceitos)); e qualquer permissão de SQS.

**Criação de roles de função.** O SAM cria as roles das funções, então a role de deploy precisa de `iam:CreateRole` e `iam:PutRolePolicy`. A contenção:
- `EscreverRolesComLimite`: criar, alterar política ou trocar a trust policy de uma role só é permitido com a condição `iam:PermissionsBoundary` igual a `feedback-limite-funcoes-<amb>`.
- `AnexarPoliticasDoSam`: só três políticas gerenciadas podem ser anexadas, `AWSLambdaBasicExecutionRole`, `AWSLambdaDynamoDBExecutionRole` e `AWSXrayWriteOnlyAccess`.
- `PassarRolesParaLambdaEScheduler`: `iam:PassRole` só para `lambda.amazonaws.com` e `scheduler.amazonaws.com`.
- A role de deploy não pode alterar nem remover o boundary, por causa do `NegaAlterarBootstrap`.

O que sobra está em [R03](#registro-de-riscos-aceitos).

**Onde a restrição por nome não alcança:**
- **API Gateway:** o ARN usa o ID da API, não o nome. A escrita vale para as APIs da região, e a negação por tag cobre as APIs marcadas com outro ambiente ([R02](#registro-de-riscos-aceitos)).
- **Mapeamento do Stream:** o ARN é um UUID. A criação e a alteração exigem `lambda:FunctionArn` de uma função do ambiente.
- **Leituras sem recurso:** `cloudwatch:DescribeAlarms`, `kms:DescribeKey`, `kms:ListAliases` e três descrições de logs, todas só de leitura.
- **KMS:** só chaves usadas via DynamoDB, Lambda, S3 ou SNS (`kms:ViaService`).

**Boundary das funções.** `feedback-limite-funcoes-<amb>` é o teto de toda role criada pelo deploy. Mesmo que uma role nova receba política ampla, o efeito não passa disto:

| Statement | Ações | Recursos |
|---|---|---|
| `Logs` | criar grupo e stream, gravar log | `/aws/lambda/feedback-*-<amb>` e `feedback-<amb>-*` |
| `Tracing` | as cinco de escrita do X-Ray | `*` |
| `TabelaDoAmbiente` | leitura e escrita de itens, `DescribeTable` | tabelas e índices do ambiente |
| `StreamDoAmbiente`, `ListarStreams` | leitura do Stream | Stream do ambiente; `ListStreams` em `*` |
| `TopicosDoAmbiente` | `sns:Publish` e `sns:GetTopicAttributes` | tópicos do ambiente |
| `BucketDoAmbiente`, `ObjetosDoAmbiente` | `ListBucket`, `GetBucketLocation`; `GetObject`, `PutObject`, `AbortMultipartUpload` | bucket e objetos do ambiente |
| `InvocarFuncaoDoAmbiente` | `lambda:InvokeFunction` | funções do ambiente, inclusive alias |
| `ChavesGerenciadasViaServico` | `kms:Decrypt` e `kms:GenerateDataKey*` | só via DynamoDB, S3 ou SNS |

O boundary não tem nenhuma ação de IAM. O `template.yaml` aplica o boundary no `Globals`, para as três funções, e no evento `ScheduleV2`, porque o `Globals` não alcança a role que o agendamento gera. Em dev, as quatro roles da stack têm o boundary, conferidas em 06/11/2026 (`qa.md` de `add-observabilidade-seguranca`, 6.16). Em hml, as três roles de função, em 07/11/2026 (`qa.md` de `add-fundacao-projeto`, "Release 1.0.0 em hml", item 1).

As políticas de cada role de função ficam abaixo do boundary e estão em [docs/funcoes.md](funcoes.md).

## Criptografia em repouso

| Recurso | Criptografia | Conferido |
|---|---|---|
| Tabela `feedback-avaliacoes-<amb>` | `SSEEnabled: true`, KMS com a chave gerenciada `aws/dynamodb` | dev em 04/11 (`SSEType: KMS`, `KeyManager: AWS`) e hml em 07/11/2026 |
| Backup contínuo da tabela | Recuperação pontual (PITR) só em prod: `PointInTimeRecoveryEnabled: !If [EhProd, true, false]` | desligado em hml, conferido em 07/11/2026 |
| Tópico de administradores `feedback-admins-<amb>` | `KmsMasterKeyId: alias/aws/sns` | dev em 04/11 e hml em 07/11/2026 |
| Bucket `feedback-relatorios-<ID_DA_CONTA>-<amb>` | SSE-S3 (`AES256`) por padrão, SSE-C bloqueado | dev em 04/11 e 06/11/2026; objeto de hml com `AES256` em 07/11/2026 |
| Tópico de alarmes `feedback-alarmes-<amb>` | **Nenhuma** | sem `KmsMasterKeyId` em dev em 06/11 e em hml em 07/11/2026 |

**Por que o tópico de alarmes não é criptografado.** Decisão do Gabriel de 05/11/2026, registrada no `openspec/project.md` e no design de `add-observabilidade-seguranca`, D1:
- O CloudWatch não publica em tópico criptografado com `alias/aws/sns`. A documentação do SNS exige chave gerenciada pelo cliente, com o serviço liberado na política da chave.
- A chave gerenciada pelo cliente tem custo mensal.
- A mensagem de alarme não carrega dado do aluno: a métrica é só uma contagem de linhas de log. Em dev, as 12 mensagens publicadas em 06/11/2026 foram lidas sem descrição nem nota (6.12), e em hml a de 07/11/2026 também (8.1).

Em troca, o tópico nega acesso sem TLS e só aceita publicação dos alarmes do próprio ambiente (próxima seção). O risco está em [R30](#registro-de-riscos-aceitos).

**Chave dos tópicos nas funções.** Notificação e relatório publicam no tópico de administradores, então as roles delas têm `kms:Decrypt` e `kms:GenerateDataKey*` em `key/*` da conta, restritos por `kms:ViaService` ao SNS da região. A tabela e o bucket não exigem permissão de KMS na role.

## Exigência de TLS

**Bucket de relatórios.** A política `NegaAcessoSemTls` nega `s3:*` a qualquer principal, no bucket e nos objetos, quando `aws:SecureTransport` é `false`. A política foi lida no bucket real de dev em 04/11/2026. Ninguém fez requisição autenticada sem TLS ao bucket. O acesso anônimo foi tentado em 06/11/2026: `GET`, `HEAD`, listagem e `GET` por `http` devolveram 403 (`qa.md` de `add-relatorio-semanal`, 5.7).

O bucket também tem as quatro opções de Block Public Access ligadas, `IsPublic: false` e ACL desativada (`BucketOwnerEnforced`). A conta não tem Block Public Access próprio: o bloqueio vem do bucket.

**Tópico de alarmes.** A política do tópico (`AWS::SNS::TopicInlinePolicy`) tem dois statements:

| Statement | Efeito |
|---|---|
| `NegaAcessoSemTls` | `Deny` a qualquer principal, com `aws:SecureTransport` igual a `false`, nas 11 ações aceitas em política de tópico. A lista é explícita porque a referência do SNS não documenta curinga nesse tipo de política |
| `PublicacaoPelosAlarmesDoAmbiente` | `Allow` de `sns:Publish` só para `cloudwatch.amazonaws.com`, com `aws:SourceAccount` igual à conta e `aws:SourceArn` em `alarm:feedback-*-<amb>`. Um alarme de hml não publica no tópico de dev |

A negação foi exercitada em dev em 06/11/2026: uma leitura assinada pelo endpoint HTTP foi recusada com `AuthorizationError`, "explicit deny in a resource-based policy", e a mesma leitura com TLS foi aceita. Como controle, a mesma leitura sem TLS no tópico de administradores, que não tem essa política, foi recusada pelo próprio tópico criptografado (`InvalidSecurity: Requests to an encrypted topic must use HTTPS and Sigv4`).

Registro: [qa.md de add-observabilidade-seguranca](../openspec/changes/archive/2026-11-11-add-observabilidade-seguranca/qa.md), seção 6.6, com `-dev`, em 06/11/2026, com o perfil administrativo.

```bash
aws sns get-topic-attributes \
  --topic-arn "arn:aws:sns:sa-east-1:<ID_DA_CONTA>:feedback-alarmes-dev" \
  --endpoint-url http://sns.sa-east-1.amazonaws.com \
  --region sa-east-1 \
  --profile tc4-admin
```

A requisição assinada por HTTP expõe o token de sessão no caminho de rede. No QA foram duas, com credencial de curta duração. Não repita com credencial de longa duração.

As 11 ações implantadas foram comparadas com a lista do design, não de novo com a página de referência do SNS (`qa.md` de `add-observabilidade-seguranca`, observação H).

**Tópico de administradores.** Não tem política de TLS própria. Por ser criptografado, ele mesmo recusa requisição sem HTTPS, como mostrou o controle acima.

**API.** O endpoint é o `https://` do output `ApiAvaliacoesUrl`. Nenhum QA tentou requisição por HTTP.

## Dado do aluno fora dos logs

A spec proíbe dado de entrada do aluno em log em nível INFO. O código é mais estrito (design de `add-registro-avaliacao`, D9): nenhuma das três funções envia `descricao`, `nota` ou o corpo bruto para log em nível nenhum, nem para anotação ou metadado de trace.

| Pode ir para log | Nunca vai |
|---|---|
| `id`, `urgencia`, `eventID`, `messageId`, nomes dos campos rejeitados | `descricao` e `nota` |
| Classe da exceção; do SDK, também o código de erro e o ID da requisição | Mensagem e pilha de exceção do Jackson ou do SDK, que podem citar a entrada |
| No relatório: data de referência, totais, bucket e chave | A média, porque com uma avaliação na janela ela revelaria a nota |

O nível é INFO em todas as funções (`POWERTOOLS_LOG_LEVEL: INFO` no `Globals` e `Root level="INFO"` no `log4j2.xml`). O handler não registra o evento recebido. A regra é coberta pelos testes de log de cada função, com o layout real e nível `ALL`.

**Conferido nos grupos de log e nos traces:**

| Onde | Data | Resultado | Fonte (`qa.md`) |
|---|---|---|---|
| registrar-avaliacao, dev | 05/11/2026 | 536 eventos e 81 traces, 0 ocorrências dos marcadores e de `"descricao":` | `add-registro-avaliacao`, 6.7 |
| notificar-critica, dev | 05/11/2026 | 26 eventos e o trace, 0 ocorrências | `add-notificacao-critica`, 5.7 |
| gerar-relatorio-semanal, dev | 06/11/2026 | 73 eventos e 9 traces, 0 trechos das 40 descrições e nenhuma nota ou média | `add-relatorio-semanal`, 5.8 |
| As três, hml | 07/11/2026 | 0 ocorrências dos marcadores, nenhuma linha com descrição ou nota | `add-fundacao-projeto`, "Release 1.0.0 em hml", item 5 |
| Mensagens de alarme, dev e hml | 06/11 e 07/11/2026 | Só dados do alarme | `add-observabilidade-seguranca`, 6.12 e 8.1 |

O dashboard também não lê linha da aplicação: a única consulta de log filtra as linhas de plataforma `RESTORE_REPORT`.

**Limites conhecidos**, que vêm de bibliotecas, não do código do projeto:
- Numa falha do SDK, o instrumentor do X-Ray grava a mensagem e a pilha da exceção no subsegmento ([R13](#registro-de-riscos-aceitos)).
- Em DEBUG, o SDK registra a mensagem da exceção ([R14](#registro-de-riscos-aceitos)).
- Na data de referência rejeitada do relatório, o runtime escreve no log uma linha em texto com a mensagem e a pilha. A mensagem é fixa e não tem dado do aluno ([R29](#registro-de-riscos-aceitos)).

## Onde a descrição do aluno fica guardada ou trafega

| Lugar | O que guarda | Criptografia em repouso | Expiração |
|---|---|---|---|
| Tabela `feedback-avaliacoes-<amb>` e índice `por-data` (projeção `ALL`) | O item inteiro: descrição, nota, urgência, `dataEnvio` e `criadoEm` | Sim, KMS com `aws/dynamodb` | Nenhuma. Sem TTL; fica até a remoção da stack |
| Stream da tabela (`NEW_IMAGE`) | A imagem de cada item gravado, com a descrição | Sim, pela criptografia da tabela, segundo a [documentação do DynamoDB](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/EncryptionAtRest.html) | 24 horas, pelo serviço |
| Backup contínuo do PITR, só em prod | O estado restaurável da tabela nos últimos 35 dias | Sim, como a tabela | Janela móvel de 35 dias enquanto a tabela existe. Na remoção, vira backup de sistema mantido por 35 dias |
| Relatórios em `relatorios/<data>.txt` no bucket | A lista da semana, com descrição, urgência e data de envio | Sim, SSE-S3 | Nenhuma. Sem regra de ciclo de vida; a reexecução para a mesma data substitui o objeto |
| Lotes descartados em `aws/lambda/` no bucket | O evento completo do Stream em `payload`, com a descrição. Confirmado em dev em 05/11/2026, com 5 objetos | Sim, SSE-S3 | Nenhuma. Sem regra de ciclo de vida |
| E-mail do aviso crítico | Descrição completa, urgência e data de envio | Na AWS, sim, enquanto a mensagem está no tópico (`alias/aws/sns`). Na caixa de entrada, depende do provedor de e-mail, fora do controle do projeto | Fora do controle do projeto |

**Por onde passa sem ficar guardada:** a requisição `POST /avaliacao`, pelo endpoint `https://`; a memória das funções de registro e de notificação durante a invocação; o tópico de administradores até a entrega do e-mail.

**Onde não fica,** conforme as conferências da seção anterior: logs, traces, métricas, dashboard, mensagens de alarme, e-mail do relatório semanal e resposta do acionamento manual. O e-mail do relatório traz só a média, os totais por urgência e por dia e a localização do objeto.

**Nada expira sozinho enquanto a stack existe.** O dado sai com a remoção:
- `dev` tem só itens gravados pelos QAs.
- `hml` pode ser removido depois da entrega, em 13/11/2026.
- `prod` fica no ar até a divulgação da nota da FIAP. As avaliações registradas no vídeo ficam na tabela, nos relatórios e nos e-mails até lá, e o relatório agendado das segundas-feiras continua incluindo essas avaliações.
- Nos dois casos, o bucket é esvaziado antes do `sam delete`, porque o CloudFormation só exclui bucket vazio. O procedimento está na implantação do [README](../README.md).
- A tabela de prod tem PITR. A remoção gera um backup de sistema mantido por 35 dias. Apagar esse backup antes é decisão do Gabriel.
- O bucket não deve ser apagado por fora da stack: a documentação da AWS alerta que, com o destino de falha ainda configurado, outra conta pode recriar o nome do bucket e passar a receber os lotes descartados. O `sam delete` remove o mapeamento e o bucket juntos.

Riscos: [R17](#registro-de-riscos-aceitos) e [R18](#registro-de-riscos-aceitos).

## Isolamento entre ambientes

Os três ambientes são stacks do mesmo template na mesma conta, `feedback-dev`, `feedback-hml` e `feedback-prod` ([R01](#registro-de-riscos-aceitos)). O isolamento vem de:

| Camada | Mecanismo |
|---|---|
| Recursos | Todo nome leva o sufixo do parâmetro `Stage`. O bucket leva também o ID da conta |
| Deploy | Uma role por ambiente, assumida só por job do ambiente do GitHub de mesmo nome, e escrita só nos nomes do ambiente |
| Funções | Boundary por ambiente; variáveis de ambiente apontam para a tabela, o tópico e o bucket do próprio ambiente |
| Métricas | Namespace `FeedbackServerless/<amb>` |
| Alarmes | O tópico de alarmes só aceita publicação de `alarm:feedback-*-<amb>` |
| API | Tag `ambiente` na API e no estágio, usada pela negação de escrita cruzada |
| GitHub | Cada ambiente aceita deploy de uma única branch (seção seguinte) |

**Conferido com dev e hml no ar ao mesmo tempo,** em 07/11/2026 (`qa.md` de `add-fundacao-projeto`, "Release 1.0.0 em hml", item 6, e `qa.md` de `add-observabilidade-seguranca`, 8.2):
- Nenhum `PhysicalResourceId` se repete entre as duas stacks, tirando `$default`, que é o nome do estágio dentro de cada API.
- As funções de hml apontam só para recursos de hml, e as políticas inline das roles de hml não citam nenhum recurso `-dev`.
- As avaliações registradas em hml não apareceram na tabela nem no índice de dev.
- Uma linha de falha gravada no grupo de log de dev disparou só o alarme de dev, e o alarme, o tópico e o dashboard de hml ficaram sem mudança.

**O que é compartilhado na conta:** o provedor OIDC, que é um por URL; o bucket de artefatos do SAM, com um prefixo por ambiente; o alarme de cobrança em `us-east-1`; e a cota do nível gratuito.

## Proteção de branch, ambientes do GitHub e aprovação em prod

Valores lidos com `gh api`, só GET, em 10 e 11/11/2026 (UTC), na escrita deste documento. A conferência formal é a leitura de governança do QA de `add-documentacao-entrega`.

**Repositório:** público, branch padrão `develop`, um único colaborador, `g-andradd`, que é administrador.

**Proteção de `main` e `develop`** (as duas iguais):

| Item | Valor |
|---|---|
| Check obrigatório | `ci`, o job de `ci.yml`, que roda nos PRs para `develop`, `release/**` e `main` |
| Branch atualizada com a base antes do merge | Não exigida (`strict: false`, [R07](#registro-de-riscos-aceitos)) |
| Merge | Só por PR, sem aprovação de revisor exigida (`required_approving_review_count: 0`) |
| Vale para administradores | Sim (`enforce_admins: true`) |
| Force push e exclusão da branch | Bloqueados |

**Ambientes do GitHub:**

| Ambiente | Branches aceitas | Regras | Bypass de administrador |
|---|---|---|---|
| `dev` | `develop` | política de branch | permitido ([R06](#registro-de-riscos-aceitos)) |
| `hml` | `release/*` | política de branch | permitido |
| `prod` | `main` | revisor obrigatório `g-andradd`, sem "prevent self-review", e política de branch | permitido |

- O revisor de prod sem "prevent self-review" foi autorizado pelo Gabriel em 03/11/2026, porque o projeto é individual.
- A restrição de branch foi configurada em 04/11/2026, quando o QA de `add-fundacao-projeto` mostrou que os ambientes aceitavam qualquer branch (observação O3).
- Com a restrição de branch e o `sub` da trust policy, a role de prod só é assumida por um job do ambiente `prod`, que só roda a partir de `main`, depois da aprovação, salvo o bypass de administrador ([R06](#registro-de-riscos-aceitos)).

**Aprovação em prod.** O `deploy.yml` mapeia `main` para `prod`, e o job espera a aprovação do revisor. A execução 42438770120, de 08/11/2026, que publicou a versão 1.0.0 em prod, tem a aprovação de `g-andradd` registrada para o ambiente `prod`. Os deploys de um mesmo ambiente não rodam em paralelo (`concurrency` por ambiente, sem cancelar o que está em andamento).

Registro: os dois comandos rodaram para cada ambiente em 04/11/2026 ([qa.md de add-fundacao-projeto](../openspec/changes/archive/2026-11-11-add-fundacao-projeto/qa.md), "Decisões do Gabriel sobre as observações", O3) e de novo em 10 e 11/11/2026 (UTC), na escrita deste documento.

```bash
gh api repos/g-andradd/fiap-tc4-feedback-serverless/environments/prod
gh api repos/g-andradd/fiap-tc4-feedback-serverless/environments/prod/deployment-branch-policies
```

## Roles de deploy e manutenção do bootstrap

**Atualizar o bootstrap.** As roles de deploy negam a si mesmas qualquer mudança no bootstrap, então ele só muda com o perfil administrativo, fora do pipeline. O procedimento é reaplicar o template com o mesmo comando da implantação, gerando antes um change set com `--no-execute-changeset`, revisando-o com `aws cloudformation describe-change-set` e só então executando com `aws cloudformation execute-change-set`. Os comandos estão no [README](../README.md).

Esse procedimento foi usado em 06/11/2026, para o aquecimento das funções: o statement `PublicarNoTopico` do boundary virou `TopicosDoAmbiente`, com `sns:GetTopicAttributes` além de `sns:Publish`. O change set trouxe só os três boundaries como `Modify`, sem substituição, e a versão `v2` de cada um foi conferida com `aws iam get-policy-version`. As políticas das roles de deploy não mudaram.

**Regras para um template novo.** As roles de deploy só alcançam o que segue esta convenção. Fora dela, o deploy é negado:
- **Nomes:** `feedback-<nome>-<amb>` ou o nome gerado pela stack, `feedback-<amb>-*`. Alarmes, dashboards e agendamentos precisam de nome explícito.
- **Roles de função:** com o boundary `feedback-limite-funcoes-<amb>`. A role gerada por um evento `ScheduleV2` declara `PermissionsBoundary` no próprio evento.
- **Políticas gerenciadas:** só `AWSLambdaBasicExecutionRole`, `AWSLambdaDynamoDBExecutionRole` e `AWSXrayWriteOnlyAccess`. O resto vai em política inline.
- **Logs:** grupos em `/aws/lambda/`, na mesma convenção de nomes.
- **API:** a tag `ambiente` vem das tags de stack do `samconfig.toml` e da própria API no template. Sem ela, a negação de escrita cruzada não se aplica.

**Tamanho das políticas.** Cada política gerenciada aceita até 6.144 caracteres, contados sem espaços. Em 05/11/2026, `github-deploy-feedback-prod-aplicacao` tinha 5.999, com 145 de folga, e `github-deploy-feedback-prod-recursos` tinha 5.860, com 284 (design de `add-observabilidade-seguranca`, D12). Uma ação nova de CloudWatch, SNS ou Logs vai para a `-recursos`. Uma terceira política só entra se as duas estourarem.

**Service-linked role do API Gateway.** `AWSServiceRoleForAPIGateway` é criada à mão, uma vez por conta, antes do primeiro deploy, porque as roles de deploy não têm `iam:CreateServiceLinkedRole` ([R40](#registro-de-riscos-aceitos)). O passo está na implantação do README, e o motivo, no [incidente de 04/11/2026](incidentes/2026-11-04-snapstart-aspectjrt.md).

## Pendência pós-entrega: desvio do `aspectjrt`

As três funções levam o `aspectjrt` em escopo `runtime`, sem plugin, sem weaving e sem anotações. É um desvio temporário, aprovado pelo Gabriel em 04/11/2026 (design de `add-registro-avaliacao`, D6). No Powertools 2.11.0, o priming do SnapStart inicializa classes que dependem do AspectJ e só trata `ClassNotFoundException`. Sem o jar, o `NoClassDefFoundError` derruba a publicação da versão, como aconteceu no primeiro deploy em dev ([incidente de 04/11/2026](incidentes/2026-11-04-snapstart-aspectjrt.md)).

O jar só sai quando as três condições valerem:
1. Existir uma release do Powertools com o commit `f628502` ([PR #2684](https://github.com/aws-powertools/powertools-lambda-java/pull/2684)), que passa a tratar `LinkageError`.
2. O `PrimingSnapStartTest` de cada função passar sem o jar.
3. Um deploy confirmar `State: Active` e `OptimizationStatus: On` no alias `live` das três funções.

A mesma pendência, com as mesmas condições, está na seção [Pendências pós-entrega do `openspec/project.md`](../openspec/project.md#pendências-pós-entrega). O risco correspondente é o [R08](#registro-de-riscos-aceitos).

## Registro de riscos aceitos

Registro único, com os riscos de segurança e os operacionais. Os ids seguem a ordem de origem, change a change, na ordem do backlog. Para citar um risco, use o id com o link para esta seção, como `[R30](seguranca.md#registro-de-riscos-aceitos)`.

Na coluna Decisão, "mantido em 10/11/2026" quer dizer que a proposta de `add-documentacao-entrega`, aprovada pelo Gabriel em 10/11/2026, deixa os riscos aceitos como estão, sem correção na entrega. A tabela que liga cada item de origem a um id, ou ao motivo de não ter id, está no `qa.md` de `add-documentacao-entrega`, seção "Cobertura dos riscos".

| Id | Risco | Origem | Motivo da aceitação | Mitigação | Decisão | Revisão |
|---|---|---|---|---|---|---|
| R01 | Os três ambientes na mesma conta AWS. Um erro em dev pode afetar prod | `add-fundacao-projeto`, design, D1 e Risks | Conta pessoal, projeto individual e prazo de 10 dias. Uma conta por ambiente com AWS Organizations é desproporcional para o escopo | Role de deploy por ambiente com `sub` restrito ao ambiente, escrita só nos nomes do ambiente, boundary por ambiente, negações do bootstrap, aprovação manual em prod. Isolamento conferido com dev e hml juntos em 07/11/2026 | Gabriel, 03/11/2026, na aprovação de `add-fundacao-projeto`; mantido em 10/11/2026 | Se a plataforma passar a ter usuários reais, separar prod em outra conta |
| R02 | A role de deploy escreve nas APIs do API Gateway da região sem restrição por nome | `add-fundacao-projeto`, design, Risks; bootstrap, `ApisNaRegiao` | O ARN da API usa o ID, não o nome, então não há como restringir pelo ambiente | `NegaEscritaEmApiDeOutroAmbiente` nega escrita em API com a tag `ambiente` de outro ambiente. A API e o estágio levam a tag, conferida em dev em 05/11/2026. Uma API sem a tag fica de fora: é proteção contra erro, não contra ataque | Aprovação de `add-fundacao-projeto`; mantido em 10/11/2026 | Nenhuma |
| R03 | Escalada residual: a role de deploy pode criar role de função com política inline arbitrária e qualquer trust policy, até o teto do boundary | `add-fundacao-projeto`, design, D9 e Risks | O SAM precisa criar as roles das funções, o que exige `iam:CreateRole` e `iam:PutRolePolicy` | Toda escrita de role exige o boundary do ambiente, que só cobre recursos do ambiente e não tem ação de IAM. A role de deploy não altera o boundary. `PassRole` só para Lambda e Scheduler. Só três políticas gerenciadas podem ser anexadas | Aprovação de `add-fundacao-projeto`; mantido em 10/11/2026 | Nenhuma |
| R04 | Em Java, o snapshot do SnapStart é apagado depois de 14 dias sem invocação, e a chamada seguinte falha com `SnapStartNotReadyException` até a versão voltar a `Active` | `add-fundacao-projeto`, design, Risks; `add-registro-avaliacao`, design, D7; `add-notificacao-critica`, design, Risks | Comportamento do serviço, sem configuração que o evite | O roteiro manda conferir `State: Active` no alias `live` das três funções antes de gravar. O agendamento invoca o relatório toda segunda-feira. A release 1.0.1 publica versões novas antes do vídeo | Aprovação dos designs; mantido em 10/11/2026 | Nenhuma |
| R05 | A assinatura de e-mail de cada tópico exige confirmação manual. Sem ela, funções e alarmes têm sucesso e nada chega, e o SNS descarta a assinatura pendente depois de um prazo | `add-fundacao-projeto`, design, Risks, e `qa.md`, O2 e R; `add-notificacao-critica`, design, Risks; `add-observabilidade-seguranca`, design, Risks | O protocolo `email` do SNS só entrega depois do clique no link de confirmação | Passo de confirmação na implantação do README. Os QAs conferem `SubscriptionsConfirmed` em cada ambiente; em hml a assinatura de alarmes estava pendente e foi confirmada em 07/11/2026. O roteiro confere as assinaturas de prod antes de gravar | Aprovação dos designs; mantido em 10/11/2026 | Nenhuma |
| R06 | O4: bypass de administrador ligado nos três ambientes do GitHub (`can_admins_bypass: true`). Um administrador pode implantar sem esperar as regras do ambiente, inclusive a aprovação em prod | `add-fundacao-projeto`, `qa.md`, O4 e decisões de 04/11/2026 | Projeto individual: o único administrador do repositório é o próprio revisor de prod | O bypass continua sendo ação manual do Gabriel. O deploy de prod de 08/11/2026 (execução 42438770120) teve aprovação registrada, sem bypass. A role de prod só aceita job do ambiente `prod` | Gabriel, 04/11/2026 | Desligar se outra pessoa receber acesso de administrador |
| R07 | O5: a proteção de `main` e `develop` não exige branch atualizada com a base (`strict: false`). Um PR verde e defasado pode ser mergeado | `add-fundacao-projeto`, `qa.md`, O5 e decisões de 04/11/2026 | Projeto individual, com um autor e um PR de cada vez, o que torna rara a branch defasada | O deploy roda `mvn -B install`, com os testes, sobre o commit que chegou à branch, antes do `sam deploy`: um merge que quebre build ou testes para antes de mudar a stack. O PR seguinte, para `release/*` ou `main`, roda o `ci` de novo | Gabriel, 04/11/2026 | Exigir branch atualizada se houver mais de um autor |
| R08 | Desvio do `aspectjrt` em escopo `runtime` nas três funções, contra a documentação do Powertools, que diz que a API funcional dispensa o AspectJ | `add-registro-avaliacao`, design, D6; [incidente de 04/11/2026](incidentes/2026-11-04-snapstart-aspectjrt.md) | Sem o jar, o priming do Powertools 2.11.0 derruba a publicação da versão com SnapStart, e a correção upstream ainda não tem release | Só o jar, sem plugin, weaving ou anotações. `PrimingSnapStartTest` em cada função roda o priming no CI | Gabriel, 04/11/2026 | Remover o jar quando as três condições valerem: (1) existir uma release do Powertools com o commit `f628502`; (2) o `PrimingSnapStartTest` de cada função passar sem o jar; (3) um deploy confirmar `State: Active` e `OptimizationStatus: On` no alias `live` das três funções. São as mesmas da seção [Pendências pós-entrega do `openspec/project.md`](../openspec/project.md#pendências-pós-entrega) e da [pendência pós-entrega](#pendência-pós-entrega-desvio-do-aspectjrt) |
| R09 | X-Ray SDK para Java em manutenção desde 25/02/2026: só correções de segurança, e a AWS recomenda OpenTelemetry. O tracing do Powertools usa esse SDK | `add-registro-avaliacao`, design, Risks | Prazo do projeto. Trocar o tracing contraria a stack definida no `project.md` | O SDK continua recebendo correções de segurança. O trace não leva dado do aluno, conferido nas três funções | Aprovação de `add-registro-avaliacao`; mantido em 10/11/2026 | Nenhuma |
| R10 | API `POST /avaliacao` sem autenticação: qualquer um pode gravar avaliação | `add-registro-avaliacao`, design, Non-Goals e Risks | Autenticação fora do escopo por decisão do change | Limite de 10 requisições por segundo e rajada de 20 no estágio, conferido em dev em 05/11/2026. Rota única, sem leitura nem alteração. Validação da entrada. A função só tem `PutItem` e `DescribeTable` na tabela. O identificador da API não está no repositório e é desfocado no vídeo. hml sai depois da entrega, em 13/11/2026, prod depois da nota | Aprovação de `add-registro-avaliacao`; mantido em 10/11/2026 | Nenhuma |
| R11 | A chave KMS gerenciada da tabela é cobrada por chamada | `add-registro-avaliacao`, design, Risks | Criptografia em repouso exigida sem chave do cliente | O DynamoDB guarda a chave de tabela em cache, e o volume da demonstração é baixo. A conta tem alarme de cobrança | Aprovação de `add-registro-avaliacao`; mantido em 10/11/2026 | Nenhuma |
| R12 | Deploy e CI mais lentos: o `sam build` repete os testes em cada função, a primeira publicação com SnapStart espera a versão sair de `Pending`, e o CI ganhou cerca de um minuto com o `sam build` | `add-registro-avaliacao`, design, Risks; `add-observabilidade-seguranca`, design, D16 e Risks | Pegar erro de empacotamento no PR e manter o SnapStart vale mais que o tempo | `timeout-minutes` de 45 no deploy e de 20 no CI. O deploy de hml de 07/11/2026 levou cerca de 5 minutos | Aprovação dos designs; mantido em 10/11/2026 | Nenhuma |
| R13 | Numa falha do SDK, o instrumentor do X-Ray grava a mensagem e a pilha da exceção no subsegmento. Não se sabe se uma mensagem real do DynamoDB cita valores do item | `add-registro-avaliacao`, design, D9, e `qa.md`, observação A; `add-relatorio-semanal`, `qa.md`, observação H | Excluir o instrumentor perderia o subsegmento automático dos serviços | O código não envia dado do aluno ao trace. No `PutItem`, o instrumentor só captura o nome da tabela. Os traces lidos em dev e hml não têm descrição. Nenhuma gravação falhou nos QAs | Gabriel, 04/11/2026 | Nenhuma |
| R14 | Em DEBUG e TRACE, o SDK registra a mensagem da exceção, que pode citar a entrada | `add-registro-avaliacao`, design, D9, e `qa.md`, observação C | O Powertools aplica o nível a todos os loggers, então o `log4j2.xml` não isola o SDK | As funções rodam em INFO, definido no `Globals` e no `log4j2.xml`. Mudar o nível exige mudar o template, por PR, CI e deploy | Gabriel, 04/11/2026 | Nenhuma |
| R15 | Um número com mais de 1000 dígitos, até num campo extra, faz o corpo inteiro virar `CORPO_INVALIDO`, em vez de ser ignorado ou apontar o campo | `add-registro-avaliacao`, `qa.md`, defeito D1 | Limite padrão do Jackson. A resposta continua 400, nada é gravado e nada vaza | Um teste fixa o comportamento em `nota` | Gabriel, 04/11/2026 | Nenhuma |
| R16 | `s3:PutObject` da notificação vale para o bucket inteiro, inclusive o prefixo dos relatórios | `add-notificacao-critica`, design, D4 e Risks, e `qa.md`, "Tarefa 7.6" | Ao criar o mapeamento do Stream, o Lambda valida `PutObject` no bucket inteiro e recusou `aws/lambda/*`, com e sem condição, em 05/11/2026 | Condição `s3:ResourceAccount` na própria conta. A função não tem código que use S3; quem grava é o serviço, sempre em `aws/lambda/`. O boundary já permitia os objetos do bucket do ambiente | Design de `add-notificacao-critica`, D4, depois da tarefa 7.6; mantido em 10/11/2026 | Nenhuma |
| R17 | Descrição do aluno sem expiração nos relatórios em `relatorios/` e nos lotes descartados em `aws/lambda/`, e na tabela enquanto a stack existir. Em prod, o PITR deixa um backup de sistema por 35 dias depois da remoção | `add-notificacao-critica`, design, Risks; `add-relatorio-semanal`, design, Risks; `add-documentacao-entrega`, design, Risks | Histórico e ciclo de vida fora do escopo. O lote descartado é a fonte para refazer um aviso perdido, porque o Stream guarda só 24 horas | Bucket privado, com Block Public Access, TLS obrigatório e SSE-S3, e acesso anônimo recusado em dev em 06/11/2026. A função de relatório não tem `s3:GetObject`. hml é removido depois da entrega, em 13/11/2026, e prod depois da nota, com o bucket esvaziado antes | Aprovação dos designs; mantido em 10/11/2026 | Na remoção de cada stack. Apagar o backup de sistema de prod é decisão do Gabriel |
| R18 | O `sam delete` falha com objeto no bucket, e um bucket apagado por fora da stack pode ter o nome recriado por outra conta, que receberia os lotes descartados | `add-notificacao-critica`, design, D4 e Risks; `add-relatorio-semanal`, design, Risks, e `qa.md`, observação N | O CloudFormation só exclui bucket vazio, e o template não tem versionamento nem ciclo de vida | Procedimento no README: listar, esvaziar e só então `sam delete`, com o perfil administrativo. A remoção do bucket é sempre pela stack, que remove o mapeamento junto | Aprovação dos designs; mantido em 10/11/2026 | Nenhuma |
| R19 | Com `TRIM_HORIZON`, a criação do mapeamento processa o que ainda está no Stream: críticas de até 1 hora antes geram aviso, e as mais antigas vão para o destino de falha | `add-notificacao-critica`, design, Risks, e `qa.md`, "Itens críticos antigos no destino de falha" | `LATEST` pode perder eventos durante a criação do mapeamento | Só acontece na criação do mapeamento. Em dev, em 05/11/2026, 7 críticas antigas foram para o destino sem aviso. hml e prod foram criadas do zero, com a tabela vazia | Aprovação de `add-notificacao-critica`; mantido em 10/11/2026 | Nenhuma |
| R20 | Avisos repetidos: o aviso crítico pode sair mais de uma vez, e a entrega assíncrona do relatório pode repetir um aviso igual | `add-notificacao-critica`, design, Risks; `add-relatorio-semanal`, design, D8 | A spec pede entrega pelo menos uma vez, sem deduplicação. Perder o aviso é pior que repeti-lo | O handler retorna na primeira falha do lote, para não republicar o que já saiu. Até 5 tentativas e 1 hora por registro. O relatório regrava a mesma chave | Aprovação dos designs; mantido em 10/11/2026 | Nenhuma |
| R21 | Nenhum teste automatizado lê o `template.yaml`: uma regressão no filtro do Stream, no tópico ou no agendamento passa no `mvn -B verify` | `add-notificacao-critica`, `qa.md`, observação B; `add-relatorio-semanal`, `qa.md`, observação E; `add-observabilidade-seguranca`, design, Non-Goals | Testes de template ficaram fora do escopo | O CI roda `sam validate --lint` e `sam build` nos dois templates. Cada QA conferiu o template implantado contra a spec | Aprovação de `add-observabilidade-seguranca`; mantido em 10/11/2026 | Nenhuma |
| R22 | O rodapé que o SNS acrescenta a todo e-mail tem um link de cancelamento com o ID da conta e o endereço do assinante | `add-notificacao-critica`, `qa.md`, observação M; `add-relatorio-semanal`, `qa.md`, observação P | O texto vem do SNS, não do código | Não copiar e-mail para arquivo versionado. O roteiro manda desfocar o rodapé no vídeo | Mantido em 10/11/2026 | Nenhuma |
| R23 | O índice `por-data` é eventualmente consistente: um relatório acionado logo depois de um registro pode sair sem ele | `add-relatorio-semanal`, design, D4 e Risks, e `qa.md`, observação C | Leitura fortemente consistente não existe em índice global, e ler pela tabela exigiria `Scan` | O roteiro espera alguns segundos entre registrar e acionar. Com 15 s de espera, as avaliações entraram no primeiro acionamento em dev. Reexecutar para a mesma data substitui o objeto | Aprovação de `add-relatorio-semanal`; mantido em 10/11/2026 | Nenhuma |
| R24 | Com uma única avaliação na janela, a média do e-mail do relatório é a nota dela | `add-relatorio-semanal`, design, D7 e Risks | O Gabriel quis a média no e-mail, e o destinatário são os administradores | A média fica fora dos logs. Descrições e notas individuais ficam fora do e-mail por construção: o aviso só recebe o resumo | Gabriel, 04/11/2026 | Nenhuma |
| R25 | Um item sem campo obrigatório, que só uma escrita manual criaria, bloqueia todos os relatórios daquela janela | `add-relatorio-semanal`, design, Risks | Falhar visível é melhor que publicar número errado | O único gravador da tabela sempre grava os seis atributos. O log aponta o `id`. A falha dispara o alarme do relatório | Aprovação de `add-relatorio-semanal`; mantido em 10/11/2026 | Nenhuma |
| R26 | Volume: o relatório carrega a semana inteira em memória, e com 12 ou mais páginas na leitura o pior caso alcança o `Timeout` de 120 s | `add-relatorio-semanal`, design, Risks e "Ajustes da implementação", item 1 | Volume do projeto muito abaixo disso; página extra só com mais de 1 MB de avaliações no mesmo dia | 1024 MB de memória. Cada chamada ao SDK tem limite de 10 s. Tempo esgotado casa o filtro do alarme pelas linhas do runtime | Aprovação de `add-relatorio-semanal`; mantido em 10/11/2026 | Nenhuma |
| R27 | Nome do bucket, com o ID da conta, no e-mail do relatório | `add-relatorio-semanal`, design, D7 e Risks, e `qa.md`, observação P | O administrador precisa da localização do objeto, e o link pré-assinado foi descartado por ser token de portador | Vai só para o e-mail dos administradores. A resposta do acionamento manual não traz o bucket, conferido em dev. Nada com o ID da conta no repositório. O roteiro manda desfocar | Aprovação de `add-relatorio-semanal`; mantido em 10/11/2026 | Nenhuma |
| R28 | O agendamento existe em todos os ambientes: cada stack no ar gera relatório e e-mail toda segunda-feira, e prod inclui as avaliações do vídeo até ser removido | `add-relatorio-semanal`, design, Risks | Um único template para os três ambientes | hml é removido depois da entrega, em 13/11/2026, e prod depois da nota | Aprovação de `add-relatorio-semanal`; mantido em 10/11/2026 | Na remoção de cada stack |
| R29 | `errorType` e `stackTrace` na resposta da data de referência rejeitada, com nomes de classe, arquivo e linha. O runtime também escreve a pilha no log, e o X-Ray marca erro | `add-relatorio-semanal`, `qa.md`, observação M | A rejeição precisa terminar como erro de função; um retorno normal se confundiria com sucesso | A mensagem é fixa, sem dado do aluno nem da conta, e o código é público. A linha da rejeição não tem marcador e não dispara alarme. O roteiro manda mostrar só a mensagem | Mantido em 10/11/2026 | Nenhuma |
| R30 | Tópico de alarmes sem criptografia em repouso | `add-observabilidade-seguranca`, design, D1; `openspec/project.md`, decisão de 05/11/2026 | O CloudWatch não publica em tópico com `alias/aws/sns`, a chave do cliente tem custo mensal, e a mensagem de alarme não carrega dado do aluno | Política nega acesso sem TLS e só aceita publicação dos alarmes do ambiente. Mensagens lidas sem dado do aluno em dev e hml | Gabriel, 05/11/2026 | Se a mensagem de alarme passar a carregar dado sensível |
| R31 | Texto da linha de tempo esgotado do runtime `java25` sem prova real: se o runtime escrever outro texto, um tempo esgotado real não dispara o alarme | `add-observabilidade-seguranca`, design, D3 e Risks, e `qa.md`, observação I | Provocar o tempo esgotado exigiria alterar a função na AWS, o que foi proibido | O filtro tem os dois formatos conhecidos, `Task timed out` e `Status: timeout`, provados com `test-metric-filter` e com linhas de teste | Mantido em 10/11/2026 | Se aparecer um tempo esgotado real, conferir a linha com `test-metric-filter` |
| R32 | Nível gratuito do CloudWatch sem folga: 10 de 10 métricas de alarme e 3 de 3 dashboards, com os três ambientes e o alarme de cobrança | `add-observabilidade-seguranca`, design, D8 e Risks | No máximo 3 métricas de alarme e 1 dashboard por stack, para caber no gratuito | Proibido criar alarme ou dashboard de teste; os testes usam `set-alarm-state` e linhas de log. A remoção de hml libera 3 métricas e 1 dashboard. Detalhes em [docs/monitoramento.md](monitoramento.md) | Gabriel, 05/11/2026 | Na remoção de hml |
| R33 | Até 30 métricas personalizadas distintas com as três stacks. Caber no gratuito depende de como a franquia se aplica à cobrança por hora com dado, o que a página de preços não detalha | `add-observabilidade-seguranca`, design, D8 e Risks | As métricas de filtro só recebem dado quando há falha, e as do Powertools só nas horas com invocação | Alarme de cobrança da conta | Aprovação de `add-observabilidade-seguranca`; mantido em 10/11/2026 | Nenhuma |
| R34 | Processo encerrado pelo sistema, sem passar pelo `catch (Error e)` e sem tempo esgotado, não gera marcador nem alarme | `add-observabilidade-seguranca`, design, Risks | Improvável: 1024 MB de memória para cerca de 215 MB de uso medido | Nenhuma além da folga de memória | Aprovação de `add-observabilidade-seguranca`; mantido em 10/11/2026 | Nenhuma |
| R35 | O filtro busca por trecho de texto: uma linha futura com `FALHA_FUNCAO` ou `Task timed out` dispara o alarme | `add-observabilidade-seguranca`, design, D3 e Risks | Padrão JSON não lê as linhas de plataforma, que são texto | Constante única por handler, e os testes de log afirmam onde o marcador aparece. Nenhum texto do aluno vai para log, então ele não forja o marcador | Aprovação de `add-observabilidade-seguranca`; mantido em 10/11/2026 | Nenhuma |
| R36 | O e-mail do alarme não é instantâneo: a métrica do filtro chega depois da linha, o alarme entra em ALARM cerca de 1 minuto depois do ponto e volta a OK cerca de 10 minutos depois | `add-observabilidade-seguranca`, design, Risks, e `qa.md`, observação K | Alarme de resolução padrão, dentro do nível gratuito | A janela de 5 períodos absorve o atraso, e cada episódio gera um e-mail | Aprovação de `add-observabilidade-seguranca`; mantido em 10/11/2026 | Nenhuma |
| R37 | O aquecimento antes do snapshot deixa credenciais temporárias e, talvez, conexões dentro do snapshot | `add-observabilidade-seguranca`, design, D9 e Risks | O aquecimento tirou da primeira invocação de 2,6 a 4,3 s de primeira chamada ao SDK | A cadeia padrão de credenciais renova a credencial vencida. Em dev, a primeira chamada depois de 3 restaurações por função funcionou nas três (6.15). As credenciais são da role da função, sob o boundary | Aprovação de `add-observabilidade-seguranca`; mantido em 10/11/2026 | Nenhuma |
| R38 | Linhas de teste nos grupos de log de dev: aparecem como falhas no dashboard e como três restaurações falsas de 790,00 ms | `add-observabilidade-seguranca`, design, Risks, e `qa.md`, observação L | Provar filtro e alarme sem alterar a função exigiu gravar linhas com `put-log-events` | Streams `qa-observabilidade-20261106` e `qa-observabilidade-20261107`, identificáveis pelo nome, só em dev. Expiram com a retenção de 14 dias, por volta de 20/11 e 21/11/2026 | Gabriel, 05/11/2026 | Nenhuma |
| R39 | Testes de alarme e o vídeo geram e-mail e dado real: cada `set-alarm-state` gera e-mail, e o vídeo registra avaliações em prod | `add-observabilidade-seguranca`, design, Risks; `add-documentacao-entrega`, design, Risks | O enunciado pede a aplicação funcionando em prod no vídeo | Descrições neutras, sem nome de pessoa. Um único `set-alarm-state` no vídeo. Nada é apagado à mão | Gabriel, 10/11/2026 | Nenhuma |
| R40 | Service-linked roles fora do bootstrap: `AWSServiceRoleForAPIGateway` foi criada à mão, e uma role desse tipo que um serviço peça no futuro só aparece como falha no deploy | `add-observabilidade-seguranca`, design, D12; [incidente de 04/11/2026](incidentes/2026-11-04-snapstart-aspectjrt.md), "Segunda falha" | Dar `iam:CreateServiceLinkedRole` ao deploy amplia a permissão sem evidência de necessidade | Passo manual na implantação do README, antes do primeiro deploy. Os alarmes com ação SNS não dependeram de service-linked role | Aprovação de `add-observabilidade-seguranca`; mantido em 10/11/2026 | Nenhuma |
| R41 | Caminhos de falha sem prova real: falha interna do registro, falha de publicação da notificação, execução com falha do relatório e tempo esgotado nunca aconteceram de verdade | `add-observabilidade-seguranca`, design, decisão 5 e D14; `add-notificacao-critica`, `qa.md`, observação C | Proibido alterar o `$LATEST`, a configuração ou o código das funções na AWS para provocar falha | Os testes unitários provam que cada caminho grava a linha com o marcador. As linhas de teste provaram filtro, alarme e e-mail na stack. O descarte de item inválido foi uma falha real, provocada sem alterar a função | Gabriel, 05/11/2026 | Nenhuma |
| R42 | `RuntimeException` que escapa do handler da notificação sai sem o marcador e não dispara alarme | `add-observabilidade-seguranca`, `qa.md`, "Risco aceito" e observação F | Pela leitura do código, nenhuma `RuntimeException` sai hoje do processamento: o domínio valida tudo antes | `Error` tem `catch` com marcador, e o tempo esgotado tem linhas próprias no filtro. No registro e no relatório, toda `RuntimeException` passa por `catch` com marcador | Gabriel, 06/11/2026 | Qualquer mudança no processamento da notificação |
| R43 | O e-mail do autor do Git aparece nos metadados dos commits do repositório público | `add-documentacao-entrega`, design, Risks | A regra de sigilo cobre arquivos versionados. Reescrever o histórico fica fora do escopo | Nenhuma | Gabriel, 10/11/2026 | Nenhuma |
