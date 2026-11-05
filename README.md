# Plataforma de feedback serverless

Tech Challenge da Fase 4 da pós-graduação em Arquitetura e Desenvolvimento Java (FIAP). Estudantes avaliam aulas com uma descrição e uma nota. Administradores recebem aviso imediato quando a avaliação é crítica e um relatório semanal com a consolidação dos feedbacks.

Estado atual: a função `registrar-avaliacao` está implantada e conferida em dev desde 05/11/2026. O relatório está em `openspec/changes/add-registro-avaliacao/qa.md`. O estado das demais funções está na seção de cada uma.

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
├── docs/postman/                 # coleção Postman de POST /avaliacao
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

Para empacotar as funções com o SAM, também é preciso o SAM CLI (testado com a versão 1.171.0), e o `install` vem antes:

```bash
mvn -B install
sam build
```

O `sam build` copia só o `CodeUri` da função para uma pasta temporária e resolve o parent e o módulo `dominio` pelo `~/.m2`. Sem o `install`, ele falha com `JavaMavenWorkflow:MavenBuild - Maven Failed`. Os testes do módulo rodam de novo a cada `sam build`. No Git Bash do Windows, o comando pode precisar ser `sam.cmd build`.

## Endpoint `POST /avaliacao`

Registra uma avaliação de aula. O endpoint não tem autenticação, que está fora do escopo do projeto.

Este endpoint foi conferido na stack de dev em 05/11/2026. As respostas abaixo vêm de `Respostas.java` e conferem com os testes do handler, que rodam em `mvn -B verify` com o DynamoDB simulado. A falha ao persistir (500) só tem prova nesses testes, porque não foi provocada na stack.

### Corpo da requisição

| Campo | Obrigatório | Regra |
|---|---|---|
| `descricao` | sim | Texto com ao menos um caractere diferente de espaço, tabulação ou quebra de linha, e até 1000 caracteres Unicode. Um emoji conta como um. É gravada como recebida, sem remover espaços |
| `nota` | sim | Número JSON inteiro de 0 a 10. `7.0`, `1e1` e `"7"` são rejeitados |

Campos extras são ignorados. O cliente não define `id`, `urgencia`, `dataEnvio` nem `criadoEm`: quem preenche é a função. Cada requisição válida gera uma avaliação nova, mesmo com corpo idêntico.

### Urgência

Calculada a partir da nota no registro, devolvida na resposta e gravada no item.

| Nota | Urgência |
|---|---|
| 0 a 3 | `CRITICA` |
| 4 a 6 | `MEDIA` |
| 7 a 10 | `BAIXA` |

### Respostas

Todas com `content-type: application/json`.

| Caso | Status | `erro` |
|---|---|---|
| Avaliação registrada | 201 | |
| Campo inválido | 400 | `ENTRADA_INVALIDA` |
| Corpo inválido | 400 | `CORPO_INVALIDO` |
| Falha interna | 500 | `ERRO_INTERNO` |

**201, avaliação registrada.** O `id` é um UUID gerado pela função.

Requisição:

```json
{"descricao": "Aula muito boa", "nota": 9}
```

Resposta:

```json
{"id":"<UUID>","urgencia":"BAIXA"}
```

**400, campo inválido.** `campos` lista o nome de todos os campos reprovados, nunca os valores. Nada é gravado.

Requisição:

```json
{"descricao": "", "nota": 11}
```

Resposta:

```json
{"erro":"ENTRADA_INVALIDA","mensagem":"Um ou mais campos são inválidos.","campos":["descricao","nota"]}
```

**400, corpo inválido.** Vale para corpo ausente ou vazio, JSON malformado, JSON que não é objeto, como `[1, 2, 3]`, e objeto seguido de outros tokens. Também cai aqui o corpo que ultrapassa os limites do parser JSON, como número com mais de 1000 dígitos, mesmo em campo extra. `campos` vem vazio e nada é gravado.

Requisição, com o JSON cortado:

```
{"descricao": "Aula boa", "nota": 5
```

Resposta:

```json
{"erro":"CORPO_INVALIDO","mensagem":"O corpo da requisição precisa ser um objeto JSON válido.","campos":[]}
```

**500, falha interna.** A requisição é válida, mas a gravação falhou. A mensagem é fixa, sem detalhe técnico.

Requisição:

```json
{"descricao": "Aula muito boa", "nota": 9}
```

Resposta:

```json
{"erro":"ERRO_INTERNO","mensagem":"Não foi possível registrar a avaliação. Tente novamente."}
```

O template também limita a rota a 10 requisições por segundo, com rajada de 20. O excesso recebe 429 do próprio API Gateway, sem invocar a função.

### Testar depois do deploy

A stack `feedback-dev` é publicada pelo pipeline `deploy.yml` a cada push em `develop`. Nos comandos com `aws`, acrescente `--profile tc4-admin` se a sua credencial padrão não for dessa conta.

**1. Obter a URL do endpoint**, no output `ApiAvaliacoesUrl` da stack:

```bash
aws cloudformation describe-stacks \
  --stack-name feedback-dev \
  --query "Stacks[0].Outputs[?OutputKey=='ApiAvaliacoesUrl'].OutputValue" \
  --output text \
  --region sa-east-1
```

O valor tem o formato `https://<ID_DA_API>.execute-api.sa-east-1.amazonaws.com/avaliacao`, já com o caminho.

**2. Enviar uma avaliação**, usando essa URL como está:

```bash
curl -i -X POST "https://<ID_DA_API>.execute-api.sa-east-1.amazonaws.com/avaliacao" \
  -H "Content-Type: application/json" \
  -d '{"descricao": "Aula muito boa", "nota": 9}'
```

**3. Rodar a coleção Postman**, em `docs/postman/feedback-serverless.postman_collection.json`. Importe o arquivo e defina a variável de coleção `baseUrl` com o valor de `ApiAvaliacoesUrl` **sem o `/avaliacao` do final**, porque cada requisição já monta a URL como `{{baseUrl}}/avaliacao`. Com a URL completa, a chamada iria para `/avaliacao/avaliacao`, rota que não existe.

```
baseUrl = https://<ID_DA_API>.execute-api.sa-east-1.amazonaws.com
```

A coleção traz 33 requisições, cada uma com testes de status e corpo: 12 de sucesso e limites aceitos, 7 de rejeição de `descricao`, 9 de `nota`, 1 com dois campos inválidos e 4 de corpo malformado. O valor padrão de `baseUrl` é `https://exemplo.invalid`, que não responde. Até aqui ela só rodou localmente, com Newman, contra uma ponte HTTP para o handler com o DynamoDB simulado. O caso de 500 não está na coleção, porque uma requisição não consegue provocar a falha de gravação.

## Conferir o SnapStart no alias

Passo a seguir depois de cada deploy. Em dev, a conferência de 05/11/2026 trouxe `State` igual a `Active` e `OptimizationStatus` igual a `On`.

O SnapStart só vale em versões publicadas e aliases. Por isso o comando consulta `:live`: sem o sufixo, a leitura seria de `$LATEST`, que não usa SnapStart.

```bash
aws lambda get-function-configuration --function-name feedback-registrar-avaliacao-dev:live --region sa-east-1 --query "{Version:Version,State:State,OptimizationStatus:SnapStart.OptimizationStatus}"
```

Resultado esperado: `State` igual a `Active` e `OptimizationStatus` igual a `On`. `Version` traz o número da versão publicada para a qual o alias aponta.

```json
{
    "Version": "<NUMERO_DA_VERSAO>",
    "State": "Active",
    "OptimizationStatus": "On"
}
```

Para `hml` e `prod`, troque o sufixo no nome da função. Acrescente `--profile tc4-admin` se a sua credencial padrão não for dessa conta. Em Java, o snapshot é apagado depois de 14 dias sem invocação, e a chamada seguinte falha com `SnapStartNotReadyException` até a versão voltar a `Active`.

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

### 5. Criar a service-linked role do API Gateway

Este é um passo manual de administrador, feito uma vez por conta e antes do primeiro deploy da aplicação, ou seja, antes do primeiro push em `develop`. A role `AWSServiceRoleForAPIGateway` não faz parte do template, e as roles de deploy não conseguem criá-la.

Confira antes se ela já existe. Se existir, este passo não é necessário:

```bash
aws iam get-role --role-name AWSServiceRoleForAPIGateway --profile tc4-admin
```

Se não existir, crie a role com o perfil administrativo:

```bash
aws iam create-service-linked-role --aws-service-name ops.apigateway.amazonaws.com --profile tc4-admin
```

O motivo é que as roles de deploy não têm `iam:CreateServiceLinkedRole`, de propósito, e a permissão não foi aberta no bootstrap. No deploy de 05/11/2026, o CloudTrail registrou essa ação negada à role de deploy de dev, pedida pelo API Gateway, 3 segundos antes da negação de `CreateStage`, que derrubou o `AWS::ApiGatewayV2::Stage`.

A falha do Stage em si foi por `apigateway:TagResource`. Não ficou provado que o Stage dependa da service-linked role, porque a role e a correção da tag entraram juntas antes do deploy verde. Criar a role antes elimina essa dúvida numa conta nova.

### Atualizar o bootstrap depois

Reaplique com o mesmo comando do passo 2, usando o perfil administrativo. As roles de deploy têm negação explícita para alterar a si mesmas, as próprias políticas, o provedor OIDC e a stack `feedback-bootstrap-oidc`, então essa atualização nunca passa pelo pipeline.

Antes de aplicar, recomendamos gerar um change set para revisar as mudanças. Acrescente `--no-execute-changeset` ao comando do passo 2: a CLI cria o change set e termina sem executá-lo. Depois, troque `<ARN_DO_CHANGE_SET>` pelo ARN mostrado na saída, revise e aplique:

```bash
aws cloudformation describe-change-set \
  --change-set-name "<ARN_DO_CHANGE_SET>" \
  --region sa-east-1 \
  --profile tc4-admin

aws cloudformation execute-change-set \
  --change-set-name "<ARN_DO_CHANGE_SET>" \
  --region sa-east-1 \
  --profile tc4-admin
```

### Regras que os próximos templates precisam seguir

As roles de deploy só alcançam o que segue a convenção abaixo. Fora dela, o deploy é negado.

- **Nomes:** `feedback-<nome>-<ambiente>` ou o nome gerado a partir da stack, `feedback-<ambiente>-*`. Alarmes e dashboards precisam de nome explícito.
- **Roles de função:** com o boundary `feedback-limite-funcoes-<ambiente>`. O `Globals` do `template.yaml` já aplica isso às funções. A role gerada por um evento `ScheduleV2` precisa declarar `PermissionsBoundary` no próprio evento.
- **Políticas gerenciadas:** só `AWSLambdaBasicExecutionRole`, `AWSLambdaDynamoDBExecutionRole` e `AWSXrayWriteOnlyAccess`. Para o resto, use as policy templates do SAM ou política inline.
- **Logs:** grupos em `/aws/lambda/`, seguindo a mesma convenção de nomes.
