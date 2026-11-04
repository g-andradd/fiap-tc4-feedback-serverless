# Design: add-fundacao-projeto

## Context

Repositório novo, projeto individual, prazo de 10 dias e créditos de nuvem limitados. A conta AWS é pessoal. O deploy precisa funcionar em três ambientes sem que nenhuma credencial de longa duração fique no GitHub.

## Goals / Non-Goals

**Goals**
- Um único template SAM que sobe qualquer ambiente trocando só o parâmetro `Stage`
- Deploy automatizado e rastreável por branch
- Segurança de base desde o primeiro deploy (criptografia, privilégio mínimo, sem chaves estáticas)

**Non-Goals**
- Contas AWS separadas por ambiente (uma conta, isolamento por stack e por nome de recurso)
- Domínio customizado, WAF, Cognito
- Ambiente de preview por feature branch

## Decisions

### D1. Uma conta, três stacks
Os ambientes são stacks distintas (`feedback-dev`, `feedback-hml`, `feedback-prod`) na mesma conta. Todo recurso leva o sufixo do ambiente no nome.
Alternativa descartada: AWS Organizations com conta por ambiente. Mais correto para empresa, desproporcional para o prazo e para uma conta pessoal.

### D2. AWS SAM como IaC
SAM é CloudFormation com atalhos para serverless, valida localmente (`sam validate --lint`), roda funções localmente (`sam local`) e tem integração direta com Java e SnapStart.
Alternativas descartadas: CDK (mais código para explicar no vídeo) e Terraform (mais uma ferramenta e um state remoto para gerenciar).

### D3. OIDC entre GitHub Actions e AWS
O workflow assume uma role via `aws-actions/configure-aws-credentials` usando o token OIDC do GitHub. A trust policy aceita só o formato imutável do `sub`, restrito ao ambiente, então um job só assume a role do ambiente em que está rodando: `repo:g-andradd@59920810/fiap-tc4-feedback-serverless@1445012951:environment:<ambiente>`.

Repositórios criados depois de 15/07/2026 emitem esse formato ([changelog do GitHub](https://github.blog/changelog/2026-04-23-immutable-subject-claims-for-github-actions-oidc-tokens/)). Confirmado em 03/11/2026 com `gh api repos/g-andradd/fiap-tc4-feedback-serverless/actions/oidc/customization/sub`, que retorna `use_immutable_subject: true`.

Alternativas descartadas:
- Access key e secret key em GitHub Secrets. Credencial de longa duração, sem rotação, ruim de defender na avaliação de segurança.
- Aceitar também o formato legado, `repo:g-andradd/fiap-tc4-feedback-serverless:environment:<ambiente>`. Este repositório não o emite, e ele depende só de nomes, que podem ser reaproveitados por outro dono, como alerta o changelog. Decisão do Gabriel em 03/11/2026.

### D4. Bootstrap fora do pipeline
Provedor OIDC e roles de deploy ficam em `infra/bootstrap/github-oidc.yaml`, aplicado uma vez pelo Gabriel com credencial administrativa. O pipeline não consegue alterar as próprias permissões.

O bootstrap é aplicado com `aws cloudformation deploy`. O template é CloudFormation puro, sem recursos SAM nem artefatos para empacotar, e usa `Fn::ForEach` (transform `AWS::LanguageExtensions`).
Alternativa descartada: `sam deploy`. Não acrescenta nada a este template e dependeria de um ponto em que a documentação da AWS se contradiz em 03/11/2026: a [referência do CloudFormation](https://docs.aws.amazon.com/AWSCloudFormation/latest/TemplateReference/transform-aws-languageextensions.html) diz que o SAM CLI não suporta `Fn::ForEach`, enquanto o [guia do SAM](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-specification-language-extensions.html) e o [anúncio de 18/05/2026](https://aws.amazon.com/about-aws/whats-new/2026/05/aws-sam-cli-cloudformation/) dizem que suporta.

### D5. Proteção de produção
O ambiente `prod` do GitHub exige aprovação manual do Gabriel antes do job de deploy rodar. Dev e hml são automáticos.

### D6. Modelagem da tabela
- Chave primária: `id` (UUID)
- GSI `por-data`: partição `dataEnvio` (yyyy-MM-dd), ordenação `criadoEm` (ISO-8601). O relatório consulta 7 partições em vez de fazer Scan
- Streams com `NEW_IMAGE`
- Modo on-demand, criptografia com chave gerenciada pela AWS, Point-in-Time Recovery ligado em prod

### D7. Região
`sa-east-1`, pela proximidade e pelo argumento de residência de dados (LGPD).
Alternativa descartada: `us-east-1`, que só seria adotada se SnapStart com `java25` não estivesse disponível em São Paulo. A tarefa 1.1 confirmou a disponibilidade, então a região fica mantida.

Resultado da tarefa 1.1, com consulta em 03/11/2026 somente à documentação oficial:
- **`java25` é runtime gerenciado:** a tabela de runtimes suportados lista Java 25, identificador `java25`, Amazon Linux 2023, com depreciação prevista para 30/06/2029 ([Lambda runtimes](https://docs.aws.amazon.com/lambda/latest/dg/lambda-runtimes.html)). O anúncio do runtime diz que ele está disponível em todas as regiões ([What's New](https://aws.amazon.com/about-aws/whats-new/2025/11/aws-lambda-java-25/)).
- **SnapStart cobre `java25`:** a página de SnapStart declara suporte a "Java 11 and later", sem citar `java25` pelo nome ([SnapStart](https://docs.aws.amazon.com/lambda/latest/dg/snapstart.html)). O anúncio do runtime é explícito: "The runtime supports Lambda Snap Start (in supported Regions)".
- **`sa-east-1` tem SnapStart:** a página informa "available in all commercial Regions except Asia Pacific (New Zealand) and Asia Pacific (Taipei)" ([SnapStart, regiões suportadas](https://docs.aws.amazon.com/lambda/latest/dg/snapstart.html#snapstart-supported-regions)). A região não aparece pelo nome, a conclusão é por exclusão: regiões comerciais são as da partição `AWS` ([glossário](https://docs.aws.amazon.com/glossary/latest/reference/glos-chap.html)) e `sa-east-1` consta na tabela de regiões de uma conta AWS padrão, sem opt-in ([AWS Regions](https://docs.aws.amazon.com/global-infrastructure/latest/regions/aws-regions.html)).

A verificação é documental, nada foi testado na conta. A prova prática vem no primeiro deploy de função com SnapStart, conferindo `OptimizationStatus: On` e `State: Active` na versão publicada ([Activating SnapStart](https://docs.aws.amazon.com/lambda/latest/dg/snapstart-activate.html)).

### D8. Estrutura do repositório

```
.
├── .claude/                  # agentes e permissões do Claude Code
├── .github/workflows/        # ci.yml e deploy.yml
├── infra/bootstrap/          # github-oidc.yaml (aplicado manualmente)
├── openspec/                 # specs e changes
├── functions/
│   ├── dominio/              # regras puras, sem SDK AWS
│   ├── registrar-avaliacao/
│   ├── notificar-critica/
│   └── gerar-relatorio-semanal/
├── docs/
├── template.yaml             # SAM
├── samconfig.toml            # configuração por ambiente
└── pom.xml                   # parent
```

Neste change os módulos das funções são criados vazios, só com o `pom.xml`, para o build e o pipeline já validarem a estrutura final.

### D9. Permissions boundary e nomes das roles de deploy
A role de deploy precisa de `iam:CreateRole` e `iam:PutRolePolicy` porque o SAM cria as roles das funções. Sem contenção, ela poderia escalar privilégio criando uma role com política ampla.
- **Boundary:** as ações que criam ou alteram permissões de role exigem o boundary `feedback-limite-funcoes-<amb>`, e o `template.yaml` o aplica em `Globals`. Ele é o teto de toda role de função e cobre só os recursos do ambiente.
- **Nomes:** roles e políticas de deploy usam o prefixo `github-deploy-feedback-<amb>`, fora dos padrões `feedback-*-<amb>` e `feedback-<amb>-*` em que o deploy escreve. Um Deny de `iam:*` protege essas roles e políticas, os boundaries e o provedor OIDC.

Alternativa descartada: restringir só pelo nome da role, sem boundary. A role criada ainda poderia receber política inline com qualquer permissão.

### D10. E-mail do administrador fora do repositório
O e-mail chega pelo secret `EMAIL_ADMIN` do ambiente no GitHub, gravado pelo próprio Gabriel. O `deploy.yml` o repassa ao parâmetro `EmailAdmin`, que usa `NoEcho`, e falha antes do `sam deploy` se o secret estiver ausente.
Alternativa descartada: e-mail em `samconfig.toml`, porque o repositório é público.

## Risks / Trade-offs

- **Uma conta para tudo:** um erro em dev pode afetar prod. Mitigação: roles por ambiente com permissão restrita aos recursos com o sufixo do ambiente.
- **Assinatura de e-mail do SNS:** exige confirmação manual em cada ambiente. Mitigação: passo descrito no README e na lista de tarefas.
- **SnapStart e Java 25:** disponibilidade em `sa-east-1` confirmada na documentação (D7), ainda sem teste na conta. Além disso, em runtimes Java o snapshot é apagado após 14 dias sem invocação, e a chamada seguinte falha com `SnapStartNotReadyException` até a versão voltar a `Active`. Mitigação: conferir o estado da versão no primeiro deploy de função e invocar as funções antes de gravar o vídeo.
- **API Gateway sem restrição por nome:** o ARN usa o ID da API, então a role de deploy de um ambiente pode escrever nas APIs da região. O Deny por tag `ambiente` bloqueia só APIs marcadas com outro ambiente, e uma API sem a tag fica de fora. É proteção contra erro, não contra ataque.
- **Escalada residual via IAM (D9):** a role de deploy ainda pode criar role com política inline arbitrária, até o teto do boundary. A trust policy dessa role também não tem restrição.
- **Alarmes e tópico criptografado:** o tópico usa `alias/aws/sns`. Para o CloudWatch publicar em tópico criptografado, a [documentação do SNS](https://docs.aws.amazon.com/sns/latest/dg/sns-key-management.html) exige chave gerenciada pelo cliente, com o serviço liberado na política da chave. Com a chave atual, os alarmes não publicam neste tópico. Afeta o change `add-observabilidade-seguranca` e fica sem solução por enquanto.

## Open Questions

Nenhuma.
