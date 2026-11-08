# Delta: entrega-continua

## Purpose

Caminho automatizado e seguro do código no GitHub até a AWS, seguindo o Gitflow do projeto, com um ambiente por branch e sem credenciais de longa duração.

## ADDED Requirements

### Requirement: Integração contínua em pull requests
O pipeline SHALL executar build, testes e validação do template SAM em todo pull request destinado a `develop`, `release/**` ou `main`.

#### Scenario: PR com teste quebrado
- **WHEN** um PR para `develop` contém um teste que falha
- **THEN** o check de CI falha
- **AND** o merge fica bloqueado pela proteção da branch

#### Scenario: PR válido
- **WHEN** um PR para `develop` compila, passa nos testes e o template é válido
- **THEN** o check de CI passa

### Requirement: Deploy por branch
O pipeline SHALL implantar automaticamente o ambiente correspondente à branch que recebeu o push: `develop` em dev, `release/**` em hml e `main` em prod.

#### Scenario: Merge em develop
- **WHEN** um commit chega a `develop`
- **THEN** a stack `feedback-dev` é atualizada

#### Scenario: Branch de release
- **WHEN** um commit chega a `release/1.0.0`
- **THEN** a stack `feedback-hml` é atualizada

#### Scenario: Branch de feature
- **WHEN** um commit chega a uma branch `feature/*`
- **THEN** nenhum deploy é executado

### Requirement: Aprovação para produção
O deploy em prod SHALL aguardar aprovação manual do Gabriel no ambiente `prod` do GitHub.

#### Scenario: Merge em main
- **WHEN** um commit chega a `main`
- **THEN** o job de deploy fica pendente até a aprovação
- **AND** após aprovado, a stack `feedback-prod` é atualizada

### Requirement: Autenticação sem credenciais estáticas
O pipeline SHALL autenticar na AWS exclusivamente via OIDC, assumindo a role do ambiente em execução, sem access keys armazenadas no GitHub.

#### Scenario: Role de outro ambiente
- **WHEN** um job executando no ambiente `dev` tenta assumir a role de `prod`
- **THEN** a AWS nega a operação pela trust policy

#### Scenario: Repositório sem secrets AWS
- **WHEN** os secrets e variáveis do repositório são inspecionados
- **THEN** não existe nenhuma access key ou secret key da AWS
