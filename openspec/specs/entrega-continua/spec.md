# entrega-continua Specification

## Purpose
Caminho automatizado e seguro do código no GitHub até a AWS, seguindo o Gitflow do projeto, com um ambiente por branch e sem credenciais de longa duração.

## Requirements

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

### Requirement: Empacotamento das funções no CI
O pipeline de integração contínua SHALL empacotar, em todo pull request destinado a `develop`, `release/**` ou `main`, todas as funções declaradas no template SAM, e o check de CI SHALL falhar quando o template referenciar uma função cujo código não pode ser empacotado. O empacotamento SHALL rodar sem credenciais da AWS.

#### Scenario: Função sem código
- **WHEN** um PR para `develop` declara no template uma função cujo `CodeUri` aponta para um módulo sem código empacotável
- **THEN** o check de CI falha
- **AND** o merge fica bloqueado pela proteção da branch

#### Scenario: PR válido com empacotamento
- **WHEN** um PR para `develop` compila, passa nos testes, tem template válido e todas as funções do template são empacotadas
- **THEN** o check de CI passa
- **AND** o log da execução mostra o empacotamento das três funções

#### Scenario: Passo no workflow sem credencial AWS
- **WHEN** o arquivo `ci.yml` é inspecionado
- **THEN** ele tem um passo que empacota as funções do template da aplicação
- **AND** o workflow não configura credenciais da AWS
