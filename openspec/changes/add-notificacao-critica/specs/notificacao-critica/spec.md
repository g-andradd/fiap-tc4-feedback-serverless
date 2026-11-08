# Delta: notificacao-critica

## Purpose

Aviso imediato aos administradores quando uma avaliação crítica é registrada, publicado no tópico de administradores a partir do evento de criação da avaliação.

## ADDED Requirements

### Requirement: Aviso imediato de avaliação crítica
O sistema SHALL publicar um aviso no tópico de administradores do ambiente para cada avaliação CRITICA recém-registrada, na própria execução acionada pelo evento, sem agendamento e sem acumular avaliações.

#### Scenario: Aviso publicado no tópico do ambiente
- **WHEN** uma avaliação CRITICA é registrada na stack `feedback-dev`
- **THEN** a função `notificar-critica` publica uma mensagem no tópico de administradores da stack `feedback-dev`
- **AND** nenhuma mensagem é publicada no tópico de outro ambiente

#### Scenario: Publicação na execução acionada pelo evento
- **WHEN** a função `notificar-critica` recebe o evento de uma avaliação CRITICA
- **THEN** a publicação ocorre nessa mesma execução
- **AND** não depende de agendamento nem da chegada de outras avaliações

#### Scenario: Lote com várias avaliações críticas
- **WHEN** a função recebe um lote com três eventos de avaliações CRITICA diferentes
- **THEN** são publicadas três mensagens, uma para cada avaliação

#### Scenario: E-mail recebido pelo administrador
- **WHEN** uma avaliação CRITICA é registrada em um ambiente com assinatura de e-mail confirmada
- **THEN** o endereço assinante recebe um e-mail com a descrição, a urgência e a data de envio da avaliação

### Requirement: Conteúdo obrigatório do aviso
O sistema SHALL incluir em todo aviso a descrição, a urgência e a data de envio da avaliação, com os valores do item persistido, e SHALL enviar a descrição completa no corpo da mensagem, sem corte, até o limite de 1000 caracteres.

#### Scenario: Campos obrigatórios presentes
- **WHEN** a função publica o aviso de uma avaliação CRITICA
- **THEN** o corpo da mensagem contém a descrição, a urgência `CRITICA` e a data de envio do item persistido

#### Scenario: Descrição no tamanho máximo
- **WHEN** a avaliação tem uma descrição de 1000 caracteres com acentos
- **THEN** o corpo da mensagem contém os 1000 caracteres, na mesma ordem e sem alteração
- **AND** não há corte nem reticências

#### Scenario: Data de envio vem da avaliação
- **WHEN** o evento é processado em uma data diferente da data de envio persistida no item
- **THEN** a mensagem traz a data de envio persistida
- **AND** não traz a data do processamento no lugar dela

### Requirement: Acionamento restrito a avaliações críticas
O sistema SHALL declarar no gatilho do Streams da função `notificar-critica` um filtro que aceite somente itens com `urgencia = CRITICA`, de modo que avaliações MEDIA e BAIXA não acionem a função e não gerem aviso.

#### Scenario: Filtro declarado no template
- **WHEN** o gatilho do Streams da função `notificar-critica` é inspecionado no template SAM
- **THEN** ele declara um filtro pelo atributo `urgencia` com o valor `CRITICA`

#### Scenario: Filtro aceita avaliação crítica
- **WHEN** o filtro declarado no template é avaliado contra um evento de inserção com `urgencia = CRITICA`
- **THEN** o evento corresponde ao filtro

#### Scenario: Filtro rejeita avaliação média
- **WHEN** o filtro declarado no template é avaliado contra um evento de inserção com `urgencia = MEDIA`
- **THEN** o evento não corresponde ao filtro

#### Scenario: Filtro rejeita avaliação baixa
- **WHEN** o filtro declarado no template é avaliado contra um evento de inserção com `urgencia = BAIXA`
- **THEN** o evento não corresponde ao filtro

#### Scenario: Sem invocação no ambiente implantado
- **WHEN** avaliações MEDIA e BAIXA são registradas em um ambiente implantado
- **THEN** a função `notificar-critica` não é invocada por elas
- **AND** nenhuma mensagem é publicada no tópico de administradores

### Requirement: Aviso somente na criação da avaliação
O sistema SHALL gerar aviso somente a partir da criação da avaliação e SHALL NOT gerar aviso quando um item existente é alterado ou removido.

#### Scenario: Criação gera aviso
- **WHEN** um item com `urgencia = CRITICA` é inserido na tabela
- **THEN** um aviso é publicado no tópico de administradores

#### Scenario: Alteração de item crítico não gera aviso
- **WHEN** um item existente com `urgencia = CRITICA` é alterado
- **THEN** nenhuma mensagem é publicada no tópico de administradores

#### Scenario: Alteração que torna o item crítico não gera aviso
- **WHEN** um item existente com `urgencia = MEDIA` é alterado para `urgencia = CRITICA`
- **THEN** nenhuma mensagem é publicada no tópico de administradores

#### Scenario: Remoção não gera aviso
- **WHEN** um item existente com `urgencia = CRITICA` é removido
- **THEN** nenhuma mensagem é publicada no tópico de administradores

### Requirement: Falha na publicação não é sucesso silencioso
O sistema SHALL tratar a falha ao publicar o aviso como falha de processamento do evento e SHALL reprocessar o evento conforme a política de reprocessamento registrada no `design.md` deste change.

#### Scenario: Publicação rejeitada
- **WHEN** o tópico rejeita a publicação ou está indisponível
- **THEN** a função não reporta o evento como processado com sucesso
- **AND** a falha fica registrada em log em nível de erro

#### Scenario: Evento reprocessado
- **WHEN** a publicação de um evento falha e o tópico volta a aceitar publicações dentro dos limites da política de reprocessamento
- **THEN** o evento é entregue novamente à função
- **AND** o aviso é publicado

#### Scenario: Falha em parte de um lote
- **WHEN** a publicação falha para um dos eventos de um lote
- **THEN** o evento que falhou não é reportado como processado com sucesso
- **AND** esse evento é reprocessado conforme a política

### Requirement: Avaliação registrada não depende do aviso
O sistema SHALL manter a avaliação registrada inalterada quando a publicação do aviso falha e SHALL NOT condicionar o registro da avaliação ao resultado da publicação.

#### Scenario: Item preservado após a falha
- **WHEN** a publicação do aviso de uma avaliação falha
- **THEN** o item da avaliação continua na tabela com os mesmos valores
- **AND** a função `notificar-critica` não executa nenhuma escrita na tabela

#### Scenario: Registro com tópico indisponível
- **WHEN** o tópico de administradores está indisponível e um aluno envia uma avaliação válida de urgência CRITICA
- **THEN** o registro responde com sucesso
- **AND** o item é persistido na tabela

### Requirement: Entrega pelo menos uma vez, sem deduplicação
O sistema SHALL publicar o aviso pelo menos uma vez enquanto a política de reprocessamento não se esgotar, SHALL NOT prometer entrega exatamente uma vez e SHALL NOT aplicar deduplicação. Aviso duplicado eventual é comportamento aceito.

#### Scenario: Mesmo evento entregue duas vezes
- **WHEN** o mesmo evento de avaliação CRITICA é entregue duas vezes à função e as duas publicações têm sucesso
- **THEN** são publicadas duas mensagens
- **AND** nenhuma das execuções é tratada como falha

#### Scenario: Sem controle de deduplicação
- **WHEN** o código da função e o template SAM são inspecionados
- **THEN** não existe armazenamento nem consulta de avisos já publicados
- **AND** o tópico de administradores não é configurado com deduplicação

### Requirement: Descrição fora dos logs em nível INFO
O sistema SHALL NOT registrar conteúdo da descrição da avaliação em log em nível INFO, incluindo o evento recebido e a mensagem publicada.

#### Scenario: Processamento com sucesso
- **WHEN** a função processa uma avaliação cuja descrição contém um texto marcador único e a publicação tem sucesso
- **THEN** nenhuma entrada de log em nível INFO contém o texto marcador

#### Scenario: Processamento com falha
- **WHEN** a função processa uma avaliação cuja descrição contém um texto marcador único e a publicação falha
- **THEN** nenhuma entrada de log em nível INFO contém o texto marcador
