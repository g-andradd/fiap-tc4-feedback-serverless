# Tasks: add-fundacao-projeto

Legenda de responsável: **[IA]** feito pelos agentes, **[Gabriel]** ação manual.

## 1. Preparação
- [x] 1.1 [IA] Confirmar suporte a SnapStart com `java25` em `sa-east-1` e registrar o resultado no `design.md` (D7)
- [x] 1.2 [Gabriel] Criar o repositório público no GitHub
- [x] 1.3 [Gabriel] Executar os comandos Git iniciais fornecidos (init, branch `main`, branch `develop`, primeiro push)

## 2. Estrutura do código
- [x] 2.1 [IA] Criar `pom.xml` parent com Java 25, versões centralizadas (AWS SDK v2, Powertools, JUnit 5, AssertJ, Mockito) e plugins de build
- [x] 2.2 [IA] Criar os módulos `dominio`, `registrar-avaliacao`, `notificar-critica` e `gerar-relatorio-semanal`, apenas com `pom.xml`
- [x] 2.3 [IA] Criar `.gitignore`, `.editorconfig` e `README.md` inicial
- [x] 2.4 [IA] Validar com `mvn -B verify`

## 3. Recursos de suporte (SAM)
- [x] 3.1 [IA] Criar `template.yaml` com parâmetros `Stage` e `EmailAdmin`
- [x] 3.2 [IA] Declarar a tabela DynamoDB conforme D6
- [x] 3.3 [IA] Declarar o tópico SNS criptografado com assinatura por e-mail
- [x] 3.4 [IA] Declarar o bucket S3 privado, criptografado, com Block Public Access e política que nega acesso sem TLS
- [x] 3.5 [IA] Exportar nomes e ARNs como outputs da stack
- [x] 3.6 [IA] Criar `samconfig.toml` com as seções `dev`, `hml` e `prod`
- [x] 3.7 [IA] Validar com `sam validate --lint`

## 4. Bootstrap da conta
- [x] 4.1 [IA] Criar `infra/bootstrap/github-oidc.yaml` com o provedor OIDC e as três roles de deploy
- [x] 4.2 [IA] Escrever no README o passo a passo de aplicação do bootstrap
- [x] 4.3 [Gabriel] Aplicar o bootstrap na conta AWS

## 5. Pipelines
- [x] 5.1 [IA] Criar `.github/workflows/ci.yml` (PR para `develop`, `release/**` e `main`: build, testes, `sam validate --lint`)
- [x] 5.2 [IA] Criar `.github/workflows/deploy.yml` (push em `develop`, `release/**` e `main`, com mapeamento branch para ambiente e autenticação OIDC)
- [x] 5.3 [IA] Configurar os ambientes `dev`, `hml` e `prod` no GitHub com a variável `AWS_ROLE_ARN`, e regra de aprovação manual em `prod`
- [x] 5.4 [IA] Proteger as branches `main` e `develop` (merge somente via PR com CI verde)

## 6. Validação
- [x] 6.1 [Gabriel] Abrir PR `feature/add-fundacao-projeto` para `develop` e conferir o CI verde
- [x] 6.2 [Gabriel] Fazer o merge e conferir o deploy da stack `feedback-dev`
- [ ] 6.3 [Gabriel] Confirmar a assinatura de e-mail do SNS de dev
- [x] 6.4 [IA] Agente QA confere os cenários das specs contra a stack implantada e registra o resultado
