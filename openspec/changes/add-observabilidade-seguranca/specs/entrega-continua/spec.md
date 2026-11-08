# Delta: entrega-continua

## Purpose

Caminho automatizado e seguro do código no GitHub até a AWS. Este delta acrescenta ao CI de pull request o empacotamento das funções declaradas no template, para que o erro apareça no PR e não no deploy.

## ADDED Requirements

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
