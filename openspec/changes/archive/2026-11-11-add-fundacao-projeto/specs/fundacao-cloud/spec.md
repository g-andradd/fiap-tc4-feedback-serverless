# Delta: fundacao-cloud

## Purpose

Recursos de nuvem compartilhados pelas funções da plataforma de feedback (persistência, notificação e armazenamento de relatórios), provisionados por IaC e isolados por ambiente.

## ADDED Requirements

### Requirement: Provisionamento por ambiente
O sistema SHALL provisionar todos os recursos de nuvem a partir de um único template SAM parametrizado por ambiente, gerando stacks isoladas para `dev`, `hml` e `prod`.

#### Scenario: Deploy de um ambiente
- **WHEN** o template é implantado com `Stage=dev`
- **THEN** é criada a stack `feedback-dev`
- **AND** todos os recursos nomeados levam o sufixo `dev`

#### Scenario: Ambientes não compartilham recursos
- **WHEN** as stacks `feedback-dev` e `feedback-hml` existem ao mesmo tempo
- **THEN** cada uma possui sua própria tabela, tópico e bucket

### Requirement: Armazenamento de avaliações
O sistema SHALL manter uma tabela DynamoDB de avaliações com chave `id`, índice secundário por data de envio e Streams habilitado com a imagem nova do item.

#### Scenario: Consulta por dia
- **WHEN** uma consulta é feita no índice `por-data` com uma data no formato yyyy-MM-dd
- **THEN** são retornadas somente as avaliações enviadas naquele dia

#### Scenario: Evento de inserção
- **WHEN** um item é inserido na tabela
- **THEN** o Stream publica um evento contendo o item completo

### Requirement: Proteção dos dados em repouso
O sistema SHALL criptografar em repouso a tabela, o tópico SNS e o bucket de relatórios, e SHALL bloquear todo acesso público ao bucket.

#### Scenario: Bucket público negado
- **WHEN** alguém tenta tornar público um objeto do bucket de relatórios
- **THEN** a operação é negada pelo Block Public Access

#### Scenario: Acesso sem TLS negado
- **WHEN** uma requisição ao bucket é feita sem TLS
- **THEN** a política do bucket nega a requisição

### Requirement: Canal de notificação dos administradores
O sistema SHALL disponibilizar, por ambiente, um tópico SNS com assinatura por e-mail para o endereço informado no parâmetro `EmailAdmin`.

#### Scenario: Assinatura criada no deploy
- **WHEN** a stack é implantada com um `EmailAdmin` válido
- **THEN** o endereço recebe o e-mail de confirmação de assinatura do tópico
