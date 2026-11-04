# QA: add-fundacao-projeto

Tarefa 6.4. Conferência dos cenários das specs contra a stack implantada, feita em 04/11/2026, somente com leitura na AWS (describe, get, list) e no GitHub (`gh api` GET, `gh run view`, `gh secret list`, `gh variable list`). Nenhum item, objeto, PR ou configuração foi criado ou alterado. O ID da conta e o e-mail aparecem como `<conta>` e `<email>`.

## Resultado

| Status | Quantidade |
|---|---|
| ok (executado) | 5 |
| ok (por configuração) | 8 |
| não coberto | 2 |
| falha | 0 |
| **Total de cenários** | **15** |

**O change ainda não está conforme:** cenário sem cobertura conta como falha, e há 2 nessa condição.

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
