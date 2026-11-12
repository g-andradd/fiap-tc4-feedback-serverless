# Delta: seguranca

## Purpose

Controles de segurança dos recursos de observabilidade e das permissões acrescentadas por eles: o tópico de alarmes, o dashboard e as roles das funções e de deploy. Os controles de base já estão nas specs `fundacao-cloud` e `entrega-continua` e não se repetem aqui.

## ADDED Requirements

### Requirement: Tópico de alarmes sem criptografia e sem dado do aluno
O tópico de alarmes SHALL operar sem criptografia em repouso do SNS e SHALL receber somente notificações de alarme do CloudWatch, que SHALL NOT conter descrição, nota ou qualquer outro dado de entrada do aluno.

#### Scenario: Atributos do tópico
- **WHEN** os atributos do tópico de alarmes de dev são inspecionados
- **THEN** o tópico não tem chave KMS configurada

#### Scenario: Conteúdo da notificação
- **WHEN** um alarme de dev entra em ALARM pela linha de teste gravada com `aws logs put-log-events` no grupo de log da função, depois de avaliações com um texto marcador único na descrição terem sido registradas em dev
- **THEN** a notificação do alarme traz só dados do alarme, como nome, estado, motivo, métrica e limiar
- **AND** não contém o texto marcador, descrição nem nota de avaliação

#### Scenario: Métrica do alarme é contagem
- **WHEN** os filtros que alimentam os alarmes são inspecionados
- **THEN** cada um publica uma contagem de ocorrências, sem extrair valor do conteúdo da linha de log

### Requirement: Tópico de alarmes só aceita TLS
O tópico de alarmes SHALL ter uma política que nega a qualquer principal, quando a requisição não usa TLS, cada uma das ações que a referência do SNS lista como válidas em política de tópico. A lista é explícita, porque o SNS não documenta curinga de ação em política de tópico.

#### Scenario: Política do tópico
- **WHEN** a política do tópico de alarmes de dev é inspecionada
- **THEN** ela contém uma negação para todos os principais, com a condição `aws:SecureTransport` igual a `false`, que lista todas as ações válidas em política de tópico segundo a referência do SNS

#### Scenario: Requisição sem TLS
A prova executada depende de o endpoint do SNS aceitar requisição em HTTP. Se não aceitar, o cenário vale pela inspeção da política, no cenário anterior.

- **WHEN** uma requisição ao tópico de alarmes é feita sem TLS
- **THEN** a requisição é negada pela política do tópico

### Requirement: Publicação no tópico de alarmes restrita ao CloudWatch da conta
A política do tópico de alarmes SHALL permitir a publicação pelo serviço CloudWatch somente em nome de alarmes da própria conta, e SHALL NOT conceder publicação a outro serviço ou a principal de outra conta.

#### Scenario: Publicação pelo CloudWatch mantida
- **WHEN** um alarme de dev passa para o estado ALARM
- **THEN** a publicação no tópico de alarmes é aceita e o tópico registra uma mensagem publicada

#### Scenario: Permissão restrita à conta
- **WHEN** as permissões de publicação da política do tópico de alarmes são inspecionadas
- **THEN** a única permissão de publicação concedida a um serviço é para o CloudWatch, condicionada à conta dona do tópico
- **AND** não existe permissão de publicação para principal `*` sem condição

### Requirement: Dashboard sem dado do aluno
O dashboard de cada ambiente SHALL NOT exibir descrição, nota individual nem o corpo das mensagens publicadas nos tópicos.

#### Scenario: Inspeção dos widgets
- **WHEN** a definição do dashboard de dev é inspecionada
- **THEN** nenhum widget exibe atributos `descricao` ou `nota` de avaliação, nem o corpo de mensagem publicada

#### Scenario: Marcador depois do tráfego
- **WHEN** avaliações com um texto marcador único na descrição são registradas em dev e o dashboard de dev é aberto no período delas
- **THEN** o texto marcador não aparece em nenhum widget

### Requirement: Permissões das funções restritas ao ambiente
Toda permissão acrescentada por este change às roles das funções SHALL ficar restrita aos recursos do próprio ambiente e SHALL NOT incluir ação de escrita de dados, e as roles SHALL continuar com o boundary `feedback-limite-funcoes-<ambiente>`.

#### Scenario: Inspeção das roles
- **WHEN** as políticas das roles das três funções de dev são comparadas com as anteriores a este change
- **THEN** toda ação nova tem como recurso somente recursos com o sufixo `dev`
- **AND** nenhuma ação nova grava, altera ou remove item, mensagem ou objeto

#### Scenario: Boundary mantido
- **WHEN** as roles das três funções de dev são inspecionadas depois do deploy
- **THEN** cada uma tem o boundary `feedback-limite-funcoes-dev`

### Requirement: Implantação pela role do ambiente
O tópico de alarmes, a política e a assinatura dele, os alarmes e o dashboard SHALL ser implantados pela pipeline com a role de deploy do ambiente, via OIDC, e toda permissão de deploy acrescentada para eles SHALL ficar restrita a recursos nomeados com o sufixo do ambiente.

#### Scenario: Deploy em dev pela pipeline
- **WHEN** um commit com este change chega a `develop`
- **THEN** a stack `feedback-dev` é atualizada pela role `github-deploy-feedback-dev` com o tópico de alarmes, os alarmes e o dashboard

#### Scenario: Permissões de deploy por ambiente
- **WHEN** as políticas da role de deploy de dev são inspecionadas
- **THEN** as ações de escrita em alarmes, dashboards, tópicos e filtros de métrica de log têm como recurso somente nomes com o sufixo `dev`
