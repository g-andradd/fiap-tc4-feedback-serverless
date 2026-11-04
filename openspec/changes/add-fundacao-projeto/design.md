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
O workflow assume uma role via `aws-actions/configure-aws-credentials` usando o token OIDC do GitHub. A trust policy aceita apenas `repo:<owner>/<repo>:environment:<ambiente>`, então um job só assume a role do ambiente em que está rodando.
Alternativa descartada: access key e secret key em GitHub Secrets. Credencial de longa duração, sem rotação, ruim de defender na avaliação de segurança.

### D4. Bootstrap fora do pipeline
Provedor OIDC e roles de deploy ficam em `infra/bootstrap/github-oidc.yaml`, aplicado uma vez pelo Gabriel com credencial administrativa. O pipeline não consegue alterar as próprias permissões.

### D5. Proteção de produção
O ambiente `prod` do GitHub exige aprovação manual do Gabriel antes do job de deploy rodar. Dev e hml são automáticos.

### D6. Modelagem da tabela
- Chave primária: `id` (UUID)
- GSI `por-data`: partição `dataEnvio` (yyyy-MM-dd), ordenação `criadoEm` (ISO-8601). O relatório consulta 7 partições em vez de fazer Scan
- Streams com `NEW_IMAGE`
- Modo on-demand, criptografia com chave gerenciada pela AWS, Point-in-Time Recovery ligado em prod

### D7. Região
`sa-east-1`, pela proximidade e pelo argumento de residência de dados (LGPD). A disponibilidade de SnapStart para `java25` na região é verificada na tarefa 1.1; se não houver, a região passa a ser `us-east-1` e este documento é atualizado.

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

## Risks / Trade-offs

- **Uma conta para tudo:** um erro em dev pode afetar prod. Mitigação: roles por ambiente com permissão restrita aos recursos com o sufixo do ambiente.
- **Assinatura de e-mail do SNS:** exige confirmação manual em cada ambiente. Mitigação: passo descrito no README e na lista de tarefas.
- **SnapStart e Java 25:** combinação recente. Mitigação: verificação logo na primeira tarefa.

## Open Questions

Nenhuma.
