# Tasks: add-fundacao-projeto

Legenda de responsável: **[IA]** feito pelos agentes, **[Gabriel]** ação manual.

## 1. Preparação
- [ ] 1.1 [IA] Confirmar suporte a SnapStart com `java25` em `sa-east-1` e registrar o resultado no `design.md` (D7)
- [ ] 1.2 [Gabriel] Criar o repositório público no GitHub
- [ ] 1.3 [Gabriel] Executar os comandos Git iniciais fornecidos (init, branch `main`, branch `develop`, primeiro push)

## 2. Estrutura do código
- [ ] 2.1 [IA] Criar `pom.xml` parent com Java 25, versões centralizadas (AWS SDK v2, Powertools, JUnit 5, AssertJ, Mockito) e plugins de build
- [ ] 2.2 [IA] Criar os módulos `dominio`, `registrar-avaliacao`, `notificar-critica` e `gerar-relatorio-semanal`, apenas com `pom.xml`
- [ ] 2.3 [IA] Criar `.gitignore`, `.editorconfig` e `README.md` inicial
- [ ] 2.4 [IA] Validar com `mvn -B verify`

## 3. Recursos de suporte (SAM)
- [ ] 3.1 [IA] Criar `template.yaml` com parâmetros `Stage` e `EmailAdmin`
- [ ] 3.2 [IA] Declarar a tabela DynamoDB conforme D6
- [ ] 3.3 [IA] Declarar o tópico SNS criptografado com assinatura por e-mail
- [ ] 3.4 [IA] Declarar o bucket S3 privado, criptografado, com Block Public Access e política que nega acesso sem TLS
- [ ] 3.5 [IA] Exportar nomes e ARNs como outputs da stack
- [ ] 3.6 [IA] Criar `samconfig.toml` com as seções `dev`, `hml` e `prod`
- [ ] 3.7 [IA] Validar com `sam validate --lint`

## 4. Bootstrap da conta
- [ ] 4.1 [IA] Criar `infra/bootstrap/github-oidc.yaml` com o provedor OIDC e as três roles de deploy
- [ ] 4.2 [IA] Escrever no README o passo a passo de aplicação do bootstrap
- [ ] 4.3 [Gabriel] Aplicar o bootstrap na conta AWS

## 5. Pipelines
- [ ] 5.1 [IA] Criar `.github/workflows/ci.yml` (PR para `develop`, `release/**` e `main`: build, testes, `sam validate --lint`)
- [ ] 5.2 [IA] Criar `.github/workflows/deploy.yml` (push em `develop`, `release/**` e `main`, com mapeamento branch para ambiente e autenticação OIDC)
- [ ] 5.3 [IA] Configurar os ambientes `dev`, `hml` e `prod` no GitHub com a variável `AWS_ROLE_ARN`, e regra de aprovação manual em `prod`
- [ ] 5.4 [IA] Proteger as branches `main` e `develop` (merge somente via PR com CI verde)

## 6. Validação
- [ ] 6.1 [Gabriel] Abrir PR `feature/add-fundacao-projeto` para `develop` e conferir o CI verde
- [ ] 6.2 [Gabriel] Fazer o merge e conferir o deploy da stack `feedback-dev`
- [ ] 6.3 [Gabriel] Confirmar a assinatura de e-mail do SNS de dev
- [ ] 6.4 [IA] Agente QA confere os cenários das specs contra a stack implantada e registra o resultado
