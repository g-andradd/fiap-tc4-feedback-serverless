# Change: Registro de avaliações de aula

## Why

O registro é a porta de entrada da plataforma: sem ele não existe avaliação para notificar nem para consolidar. O `project.md` define a entrada de `POST /avaliacao`, as regras de `descricao` e `nota` e a tabela de urgência, e determina que a urgência seja calculada no registro e persistida com a avaliação. Este change transforma essas regras em comportamento verificável, inclusive nos limites, e fixa os atributos do item que os changes `add-notificacao-critica` e `add-relatorio-semanal` vão ler.

## What Changes

- Novo endpoint `POST /avaliacao`, que recebe `descricao` e `nota` em JSON
- Validação de `descricao`: obrigatória, não vazia, até 1000 caracteres; só com espaços conta como vazia
- Validação de `nota`: obrigatória, inteiro de 0 a 10
- Rejeição de JSON malformado; entrada inválida não persiste nada
- Classificação da urgência pela tabela do `project.md`, calculada no registro e persistida; o cliente não informa urgência
- Identificador e data de envio atribuídos pelo sistema e persistidos, com o dia em America/Sao_Paulo
- Respostas definidas para sucesso (identificador e urgência), erro de validação (campo problemático) e falha interna (sem detalhe técnico)
- Nenhum dado de entrada do aluno em log em nível INFO

### Fora do escopo

- Autenticação e autorização
- Consulta, alteração e exclusão de avaliações
- Alarmes e dashboard, que são do change `add-observabilidade-seguranca`
- Envio de notificação, que é do change `add-notificacao-critica`
- Relatório semanal, que é do change `add-relatorio-semanal`
- Deduplicação de envios repetidos e idempotência

## Impact

- Spec nova: `registro-avaliacao`
- Dependência: `add-fundacao-projeto`, que entrega a tabela de avaliações e o índice `por-data`
- Contrato com outros changes: `add-notificacao-critica` e `add-relatorio-semanal` leem do item os atributos `descricao`, `nota`, `urgencia`, `dataEnvio` e `criadoEm`, definidos aqui
- Código afetado: nova função de registro; nenhuma função existente é alterada
- Ações manuais do Gabriel: executar os comandos Git fornecidos

## Rastreabilidade

| Requisito | Origem |
|---|---|
| Registro de avaliação | `project.md`, Regras de domínio, Entrada; D2 e D5 abaixo |
| Validação de descricao | `project.md`, Regras de domínio, Entrada; D3 e D4 abaixo |
| Validação de nota | `project.md`, Regras de domínio, Entrada; D6 abaixo |
| Rejeição de corpo malformado | Pedido do orquestrador; D7 abaixo |
| Entrada inválida não persiste | Pedido do orquestrador |
| Classificação de urgência | `project.md`, Regras de domínio, Urgência; D8 abaixo |
| Data de envio atribuída pelo sistema | D1 abaixo |
| Identificador atribuído pelo sistema | D5 e D8 abaixo; chave `id` em `fundacao-cloud` |
| Resposta de sucesso | Pedido do orquestrador; D2 abaixo |
| Resposta de erro de validação | Pedido do orquestrador; D2 e D9 abaixo |
| Falha interna sem exposição de detalhe | Pedido do orquestrador; D2 abaixo |
| Dados do aluno fora dos logs em nível INFO | `project.md`, Convenções, Código; D10 abaixo |

## Decisões fora do project.md

O enunciado da FIAP não está no repositório. Estas decisões não constam da seção "Regras de domínio" do `project.md`.

- **D1. Data de envio:** é atribuída pelo sistema no registro. `dataEnvio` guarda o dia em America/Sao_Paulo (yyyy-MM-dd), para coincidir com o fuso do relatório semanal, e `criadoEm` guarda o instante em ISO-8601. Os nomes e formatos vêm do item D6 do `design.md` de `add-fundacao-projeto`, porque são as chaves do índice `por-data`: item sem esses atributos não aparece no índice.
- **D2. Códigos HTTP:** 201 para sucesso, 400 para entrada rejeitada e 500 para falha interna.
- **D3. Contagem de caracteres:** o limite de 1000 conta caracteres Unicode, não unidades UTF-16. Um emoji conta como um caractere.
- **D4. Espaços em `descricao`:** contam como vazio os textos formados só por espaço, tabulação e quebra de linha. Texto válido é persistido como recebido, sem remover espaços, e os espaços entram na contagem do limite.
- **D5. Envios repetidos:** cada requisição válida gera uma avaliação nova, mesmo com corpo idêntico.
- **D6. Formato de `nota`:** só vale número JSON escrito como inteiro. `7.0`, `1e1` e números fora do intervalo são rejeitados com 400, inclusive os grandes demais para um tipo inteiro comum. `null` é tratado como ausente, regra que vale também para `descricao`.
- **D7. Corpo inválido:** corpo vazio e JSON bem formado que não seja objeto contam como malformado.
- **D8. Campos extras:** `urgencia`, `id`, `dataEnvio`, `criadoEm` ou qualquer outro campo enviado pelo cliente são ignorados, sem causar rejeição.
- **D9. Mais de um campo inválido:** a resposta de erro cita todos os campos inválidos, não apenas o primeiro. O formato exato do corpo de erro fica para o `design.md`.
- **D10. Dados que podem ir para o log:** `id` e `urgencia` são gerados pelo sistema e não contam como dado de entrada do aluno, então podem aparecer em log.
