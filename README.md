# Plataforma de feedback serverless

Tech Challenge da Fase 4 da pós-graduação em Arquitetura e Desenvolvimento Java (FIAP). Estudantes avaliam aulas com uma descrição e uma nota. Administradores recebem aviso imediato quando a avaliação é crítica e um relatório semanal com a consolidação dos feedbacks.

Projeto em fase de fundação: os módulos das funções existem, mas ainda não têm código.

## Stack

- Java 25 e Maven multi-módulo
- AWS Lambda com SnapStart, API Gateway, DynamoDB, SNS, S3 e EventBridge Scheduler
- CloudWatch e X-Ray, com Powertools for AWS Lambda (Java)
- AWS SAM para infraestrutura como código
- GitHub Actions com autenticação OIDC na AWS
- JUnit 5, AssertJ e Mockito nos testes

## Estrutura de pastas

```
.
├── .github/workflows/            # pipelines de CI e deploy
├── functions/
│   ├── dominio/                  # regras puras, sem SDK da AWS
│   ├── registrar-avaliacao/      # POST /avaliacao
│   ├── notificar-critica/        # aviso de avaliação crítica
│   └── gerar-relatorio-semanal/  # consolidação da semana
├── infra/bootstrap/              # recursos aplicados uma vez na conta AWS
├── openspec/                     # specs e changes
├── template.yaml                 # template SAM
├── samconfig.toml                # configuração por ambiente
└── pom.xml                       # parent Maven
```

## Como buildar

Requisitos: JDK 25 no `JAVA_HOME` e Maven 3.9 ou superior.

```bash
mvn -B verify
```

## Bootstrap da conta AWS

O template `infra/bootstrap/github-oidc.yaml` cria o provedor OIDC do GitHub e uma role de deploy por ambiente. Ele é aplicado uma vez, com credencial administrativa, e fica fora do pipeline de propósito: assim o pipeline não consegue alterar as próprias permissões.

Os comandos abaixo foram escritos para o Git Bash no Windows e usam o perfil `tc4-admin`. Rode a partir da raiz do repositório.

### 1. Conferir se a conta já tem o provedor OIDC

Uma conta só aceita um provedor por URL. Se ele já existir, o template precisa reaproveitá-lo.

```bash
aws iam list-open-id-connect-providers --profile tc4-admin
```

- Se a lista não trouxer `token.actions.githubusercontent.com`, siga para o passo 2A.
- Se trouxer, confira se a audiência `sts.amazonaws.com` está em `ClientIDList` e siga para o passo 2B. Troque `<ARN_DO_PROVEDOR>` pelo ARN listado:

```bash
aws iam get-open-id-connect-provider \
  --open-id-connect-provider-arn "<ARN_DO_PROVEDOR>" \
  --profile tc4-admin
```

### 2. Aplicar o template

O `CAPABILITY_AUTO_EXPAND` é necessário porque o template usa a transform `AWS::LanguageExtensions` para gerar os três ambientes. Aplique com `aws cloudformation deploy`, e não com `sam deploy`: a documentação da AWS se contradiz sobre o suporte do SAM CLI a `Fn::ForEach`, e a transform é processada pelo próprio CloudFormation.

O `AWS_CLI_FILE_ENCODING=UTF-8` evita que a CLI leia o template em CP1252, o padrão do Windows, e corrompa os acentos.

**2A. Conta sem o provedor:**

```bash
AWS_CLI_FILE_ENCODING=UTF-8 aws cloudformation deploy \
  --stack-name feedback-bootstrap-oidc \
  --template-file infra/bootstrap/github-oidc.yaml \
  --capabilities CAPABILITY_NAMED_IAM CAPABILITY_AUTO_EXPAND \
  --region sa-east-1 \
  --profile tc4-admin
```

**2B. Conta que já tem o provedor:**

```bash
AWS_CLI_FILE_ENCODING=UTF-8 aws cloudformation deploy \
  --stack-name feedback-bootstrap-oidc \
  --template-file infra/bootstrap/github-oidc.yaml \
  --capabilities CAPABILITY_NAMED_IAM CAPABILITY_AUTO_EXPAND \
  --parameter-overrides CriarProvedorOidc=false \
  --region sa-east-1 \
  --profile tc4-admin
```

Se a criação falhar, a stack fica em `ROLLBACK_COMPLETE` e precisa ser removida antes de tentar de novo. Veja o motivo nos eventos e depois apague a stack:

```bash
aws cloudformation describe-stack-events \
  --stack-name feedback-bootstrap-oidc \
  --query "StackEvents[?ResourceStatus=='CREATE_FAILED'].[LogicalResourceId,ResourceStatusReason]" \
  --output table \
  --region sa-east-1 \
  --profile tc4-admin

aws cloudformation delete-stack \
  --stack-name feedback-bootstrap-oidc \
  --region sa-east-1 \
  --profile tc4-admin
```

### 3. Obter os ARNs das roles

```bash
aws cloudformation describe-stacks \
  --stack-name feedback-bootstrap-oidc \
  --query "Stacks[0].Outputs[?starts_with(OutputKey, 'RoleDeploy')].[OutputKey,OutputValue]" \
  --output table \
  --region sa-east-1 \
  --profile tc4-admin
```

| Output | Ambiente do GitHub | Role |
|---|---|---|
| `RoleDeployDevArn` | `dev` | `github-deploy-feedback-dev` |
| `RoleDeployHmlArn` | `hml` | `github-deploy-feedback-hml` |
| `RoleDeployProdArn` | `prod` | `github-deploy-feedback-prod` |

### 4. Configurar os ambientes do GitHub

Cada ambiente precisa de duas configurações. Nenhuma delas vai para arquivo versionado.

| Nome | Tipo | Valor |
|---|---|---|
| `AWS_ROLE_ARN` | variável | ARN da role do ambiente, obtido no passo 3 |
| `EMAIL_ADMIN` | secret | E-mail que assina o tópico SNS do ambiente |

Sem `EMAIL_ADMIN`, o job de deploy falha antes de chamar o `sam deploy`, com uma mensagem indicando o secret ausente.

Para gravar o secret sem deixar o e-mail no histórico do terminal, digite o endereço quando o `read` aguardar a entrada. Repita para `hml` e `prod`, trocando `--env`:

```bash
read -rs EMAIL_ADMIN
printf '%s' "$EMAIL_ADMIN" | gh secret set EMAIL_ADMIN --env dev --repo g-andradd/fiap-tc4-feedback-serverless
unset EMAIL_ADMIN
```

### Atualizar o bootstrap depois

Reaplique com o mesmo comando do passo 2, usando o perfil administrativo. As roles de deploy têm negação explícita para alterar a si mesmas, as próprias políticas, o provedor OIDC e a stack `feedback-bootstrap-oidc`, então essa atualização nunca passa pelo pipeline.

### Regras que os próximos templates precisam seguir

As roles de deploy só alcançam o que segue a convenção abaixo. Fora dela, o deploy é negado.

- **Nomes:** `feedback-<nome>-<ambiente>` ou o nome gerado a partir da stack, `feedback-<ambiente>-*`. Alarmes e dashboards precisam de nome explícito.
- **Roles de função:** com o boundary `feedback-limite-funcoes-<ambiente>`. O `Globals` do `template.yaml` já aplica isso às funções. A role gerada por um evento `ScheduleV2` precisa declarar `PermissionsBoundary` no próprio evento.
- **Políticas gerenciadas:** só `AWSLambdaBasicExecutionRole`, `AWSLambdaDynamoDBExecutionRole` e `AWSXrayWriteOnlyAccess`. Para o resto, use as policy templates do SAM ou política inline.
- **Logs:** grupos em `/aws/lambda/`, seguindo a mesma convenção de nomes.
