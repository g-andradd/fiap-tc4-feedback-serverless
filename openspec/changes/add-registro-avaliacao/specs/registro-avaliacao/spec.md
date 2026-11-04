# Delta: registro-avaliacao

## Purpose

Recebimento das avaliações de aula enviadas pelos estudantes em `POST /avaliacao`: validação da entrada, classificação da urgência, atribuição da data de envio e persistência do item que alimenta a notificação crítica e o relatório semanal.

## ADDED Requirements

### Requirement: Registro de avaliação
O sistema SHALL expor `POST /avaliacao`, que recebe um objeto JSON com `descricao` e `nota` e, quando a entrada é válida, persiste uma nova avaliação e responde com status HTTP 201.

#### Scenario: Avaliação válida registrada
- **WHEN** o cliente envia `POST /avaliacao` com `{"descricao": "Aula muito boa", "nota": 9}`
- **THEN** a resposta tem status 201
- **AND** a tabela de avaliações passa a ter um novo item com `descricao` igual a `Aula muito boa` e `nota` igual a 9

#### Scenario: Envios iguais geram avaliações distintas
- **WHEN** o cliente envia duas vezes o mesmo corpo válido
- **THEN** as duas respostas têm status 201 e identificadores diferentes
- **AND** a tabela de avaliações passa a ter dois novos itens

### Requirement: Validação de descricao
O sistema SHALL aceitar `descricao` somente quando for um texto com ao menos um caractere diferente de espaço, tabulação ou quebra de linha e com no máximo 1000 caracteres, e SHALL rejeitar os demais casos com status HTTP 400.

#### Scenario: Descrição com 1 caractere aceita
- **WHEN** o corpo traz `descricao` com 1 caractere não branco e `nota` válida
- **THEN** a resposta tem status 201

#### Scenario: Descrição com 1000 caracteres aceita
- **WHEN** o corpo traz `descricao` com exatamente 1000 caracteres e `nota` válida
- **THEN** a resposta tem status 201

#### Scenario: Descrição com 1001 caracteres rejeitada
- **WHEN** o corpo traz `descricao` com 1001 caracteres e `nota` válida
- **THEN** a resposta tem status 400 e o erro aponta o campo `descricao`

#### Scenario: Caractere fora do plano básico conta como um
- **WHEN** o corpo traz `descricao` formada por 1000 emojis, cada um representado por um par substituto em UTF-16, e `nota` válida
- **THEN** a resposta tem status 201

#### Scenario: Descrição ausente
- **WHEN** o corpo traz somente `nota` válida
- **THEN** a resposta tem status 400 e o erro aponta o campo `descricao`

#### Scenario: Descrição nula
- **WHEN** o corpo traz `descricao` igual a `null` e `nota` válida
- **THEN** a resposta tem status 400 e o erro aponta o campo `descricao`

#### Scenario: Descrição vazia
- **WHEN** o corpo traz `descricao` igual a `""` e `nota` válida
- **THEN** a resposta tem status 400 e o erro aponta o campo `descricao`

#### Scenario: Descrição só com espaços
- **WHEN** o corpo traz `descricao` igual a `"   "` e `nota` válida
- **THEN** a resposta tem status 400 e o erro aponta o campo `descricao`

#### Scenario: Descrição só com tabulação e quebra de linha
- **WHEN** o corpo traz `descricao` formada apenas por tabulação e quebra de linha e `nota` válida
- **THEN** a resposta tem status 400 e o erro aponta o campo `descricao`

#### Scenario: Descrição que não é texto
- **WHEN** o corpo traz `descricao` igual ao número `123` e `nota` válida
- **THEN** a resposta tem status 400 e o erro aponta o campo `descricao`

#### Scenario: Descrição persistida como recebida
- **WHEN** o corpo traz `descricao` igual a `"  boa aula  "` e `nota` válida
- **THEN** a resposta tem status 201
- **AND** o item persistido tem `descricao` igual a `"  boa aula  "`, sem remoção dos espaços

### Requirement: Validação de nota
O sistema SHALL aceitar `nota` somente quando for um número JSON inteiro, escrito sem parte fracionária nem expoente, de 0 a 10, e SHALL rejeitar os demais casos com status HTTP 400.

#### Scenario: Nota mínima aceita
- **WHEN** o corpo traz `descricao` válida e `nota` igual a 0
- **THEN** a resposta tem status 201

#### Scenario: Nota máxima aceita
- **WHEN** o corpo traz `descricao` válida e `nota` igual a 10
- **THEN** a resposta tem status 201

#### Scenario: Nota abaixo do mínimo
- **WHEN** o corpo traz `descricao` válida e `nota` igual a -1
- **THEN** a resposta tem status 400 e o erro aponta o campo `nota`

#### Scenario: Nota acima do máximo
- **WHEN** o corpo traz `descricao` válida e `nota` igual a 11
- **THEN** a resposta tem status 400 e o erro aponta o campo `nota`

#### Scenario: Nota ausente
- **WHEN** o corpo traz somente `descricao` válida
- **THEN** a resposta tem status 400 e o erro aponta o campo `nota`

#### Scenario: Nota nula
- **WHEN** o corpo traz `descricao` válida e `nota` igual a `null`
- **THEN** a resposta tem status 400 e o erro aponta o campo `nota`

#### Scenario: Nota decimal
- **WHEN** o corpo traz `descricao` válida e `nota` igual a 7.5
- **THEN** a resposta tem status 400 e o erro aponta o campo `nota`

#### Scenario: Nota decimal com parte fracionária zero
- **WHEN** o corpo traz `descricao` válida e `nota` igual a 7.0
- **THEN** a resposta tem status 400 e o erro aponta o campo `nota`

#### Scenario: Nota em notação de expoente
- **WHEN** o corpo traz `descricao` válida e `nota` igual a 1e1
- **THEN** a resposta tem status 400 e o erro aponta o campo `nota`

#### Scenario: Nota como texto
- **WHEN** o corpo traz `descricao` válida e `nota` igual ao texto `"7"`
- **THEN** a resposta tem status 400 e o erro aponta o campo `nota`

#### Scenario: Nota inteira muito grande
- **WHEN** o corpo traz `descricao` válida e `nota` igual a 99999999999999999999
- **THEN** a resposta tem status 400 e o erro aponta o campo `nota`

### Requirement: Rejeição de corpo malformado
O sistema SHALL rejeitar com status HTTP 400 todo corpo de requisição que não seja um objeto JSON bem formado.

#### Scenario: JSON truncado
- **WHEN** o cliente envia o corpo `{"descricao": "Aula boa", "nota": 5`
- **THEN** a resposta tem status 400 e o erro informa que o corpo é inválido

#### Scenario: Corpo vazio
- **WHEN** o cliente envia a requisição sem corpo
- **THEN** a resposta tem status 400 e o erro informa que o corpo é inválido

#### Scenario: JSON que não é objeto
- **WHEN** o cliente envia o corpo `[1, 2, 3]`
- **THEN** a resposta tem status 400 e o erro informa que o corpo é inválido

### Requirement: Entrada inválida não persiste
O sistema SHALL NOT gravar nenhum dado quando a entrada for rejeitada.

#### Scenario: Validação de campo reprovada
- **WHEN** uma requisição é rejeitada por `descricao` ou `nota` inválida
- **THEN** a tabela de avaliações permanece sem novo item

#### Scenario: Corpo malformado
- **WHEN** uma requisição é rejeitada por corpo malformado
- **THEN** a tabela de avaliações permanece sem novo item

### Requirement: Classificação de urgência
O sistema SHALL calcular a urgência a partir da nota no momento do registro, como `CRITICA` para notas de 0 a 3, `MEDIA` para notas de 4 a 6 e `BAIXA` para notas de 7 a 10, e SHALL persistir o resultado no atributo `urgencia` do item.

#### Scenario: Nota 0
- **WHEN** uma avaliação é registrada com `nota` igual a 0
- **THEN** a resposta e o item persistido trazem `urgencia` igual a `CRITICA`

#### Scenario: Nota 3
- **WHEN** uma avaliação é registrada com `nota` igual a 3
- **THEN** a resposta e o item persistido trazem `urgencia` igual a `CRITICA`

#### Scenario: Nota 4
- **WHEN** uma avaliação é registrada com `nota` igual a 4
- **THEN** a resposta e o item persistido trazem `urgencia` igual a `MEDIA`

#### Scenario: Nota 6
- **WHEN** uma avaliação é registrada com `nota` igual a 6
- **THEN** a resposta e o item persistido trazem `urgencia` igual a `MEDIA`

#### Scenario: Nota 7
- **WHEN** uma avaliação é registrada com `nota` igual a 7
- **THEN** a resposta e o item persistido trazem `urgencia` igual a `BAIXA`

#### Scenario: Nota 10
- **WHEN** uma avaliação é registrada com `nota` igual a 10
- **THEN** a resposta e o item persistido trazem `urgencia` igual a `BAIXA`

#### Scenario: Urgência informada pelo cliente é ignorada
- **WHEN** o cliente envia `{"descricao": "Aula ruim", "nota": 2, "urgencia": "BAIXA"}`
- **THEN** a resposta tem status 201
- **AND** a resposta e o item persistido trazem `urgencia` igual a `CRITICA`

### Requirement: Data de envio atribuída pelo sistema
O sistema SHALL atribuir, no momento do registro, o instante de criação em ISO-8601 ao atributo `criadoEm` e o dia correspondente a esse instante no fuso America/Sao_Paulo, no formato yyyy-MM-dd, ao atributo `dataEnvio`, sem aceitar esses valores do cliente.

#### Scenario: Último segundo do dia em São Paulo
- **WHEN** uma avaliação é registrada no instante `2026-11-10T02:59:59Z`
- **THEN** o item persistido traz `dataEnvio` igual a `2026-11-09`
- **AND** `criadoEm` representa o instante `2026-11-10T02:59:59Z` em ISO-8601

#### Scenario: Primeiro segundo do dia em São Paulo
- **WHEN** uma avaliação é registrada no instante `2026-11-10T03:00:00Z`
- **THEN** o item persistido traz `dataEnvio` igual a `2026-11-10`

#### Scenario: Data informada pelo cliente é ignorada
- **WHEN** o cliente envia um corpo válido com `dataEnvio` igual a `2020-01-01` e `criadoEm` igual a `2020-01-01T00:00:00Z`
- **THEN** a resposta tem status 201
- **AND** o item persistido traz `dataEnvio` e `criadoEm` calculados pelo sistema, sem os valores enviados

#### Scenario: Avaliação encontrada no índice por data
- **WHEN** uma avaliação é registrada com `dataEnvio` igual a `2026-11-10`
- **THEN** uma consulta ao índice `por-data` com a data `2026-11-10` retorna essa avaliação

### Requirement: Identificador atribuído pelo sistema
O sistema SHALL atribuir a cada avaliação um identificador único, persistido no atributo `id`, sem aceitar esse valor do cliente.

#### Scenario: Identificador informado pelo cliente é ignorado
- **WHEN** o cliente envia um corpo válido com `id` igual a `abc`
- **THEN** a resposta tem status 201
- **AND** o `id` da resposta e do item persistido é diferente de `abc`

### Requirement: Resposta de sucesso
O sistema SHALL responder ao registro bem-sucedido com um corpo JSON contendo o identificador em `id` e a urgência calculada em `urgencia`.

#### Scenario: Corpo da resposta de sucesso
- **WHEN** uma avaliação é registrada com `nota` igual a 5
- **THEN** a resposta tem status 201
- **AND** o corpo traz `id` igual ao `id` do item persistido e `urgencia` igual a `MEDIA`

### Requirement: Resposta de erro de validação
O sistema SHALL responder à entrada rejeitada com status HTTP 400 e um corpo JSON que identifique pelo nome cada campo inválido.

#### Scenario: Um campo inválido
- **WHEN** o corpo traz `descricao` válida e `nota` igual a 11
- **THEN** a resposta tem status 400
- **AND** o corpo cita o campo `nota` e não cita o campo `descricao` como inválido

#### Scenario: Dois campos inválidos
- **WHEN** o corpo traz `descricao` vazia e `nota` igual a 11
- **THEN** a resposta tem status 400
- **AND** o corpo cita os campos `descricao` e `nota`

### Requirement: Falha interna sem exposição de detalhe
O sistema SHALL responder com status HTTP 500 e uma mensagem genérica quando uma entrada válida não puder ser processada por falha interna, sem expor detalhe técnico.

#### Scenario: Falha ao persistir
- **WHEN** a gravação na tabela de avaliações falha durante uma requisição válida
- **THEN** a resposta tem status 500
- **AND** o corpo não contém mensagem de exceção, pilha de chamadas nem nome de recurso de nuvem

### Requirement: Dados do aluno fora dos logs em nível INFO
O sistema SHALL NOT registrar em log de nível INFO nenhum dado de entrada do aluno, incluindo `descricao`, `nota` e o corpo bruto da requisição.

#### Scenario: Registro bem-sucedido
- **WHEN** uma avaliação é registrada com uma `descricao` contendo um marcador único
- **THEN** nenhuma entrada de log em nível INFO contém o marcador nem um atributo com a nota

#### Scenario: Entrada rejeitada
- **WHEN** uma requisição é rejeitada com `descricao` válida contendo um marcador único e `nota` igual a 987654
- **THEN** nenhuma entrada de log em nível INFO contém o marcador nem o valor `987654`

#### Scenario: Corpo malformado
- **WHEN** uma requisição é rejeitada por corpo malformado contendo um marcador único
- **THEN** nenhuma entrada de log em nível INFO contém o marcador
