# Change: Aviso imediato de avaliação crítica aos administradores

## Why

Administradores precisam saber na hora quando um estudante avalia uma aula de forma crítica, sem esperar o relatório semanal. O `project.md` define que somente avaliações CRITICA geram aviso imediato e que o e-mail traz descrição, urgência e data de envio. Este change entrega esse fluxo, que é assíncrono em relação ao registro: a avaliação é gravada primeiro e o aviso sai a partir do evento gerado pela gravação.

## What Changes

- Função `notificar-critica`, acionada pelo DynamoDB Streams da tabela de avaliações, com responsabilidade única de avisar os administradores
- Filtro no gatilho para que somente avaliações com `urgencia = CRITICA` acionem a função
- Publicação de um aviso por avaliação crítica recém-registrada no tópico de administradores do ambiente
- Aviso com descrição completa (até 1000 caracteres), urgência e data de envio
- Alteração e remoção de item não geram aviso
- Falha ao publicar é sinalizada e leva a reprocessamento do evento, sem afetar a avaliação já registrada
- Entrega pelo menos uma vez, com duplicata eventual aceita
- Conteúdo da descrição fora dos logs em nível INFO

Fora do escopo deste change:

- Outros canais além de e-mail
- Template HTML no aviso
- Deduplicação de avisos
- Alarmes e dashboard, que pertencem ao change `add-observabilidade-seguranca`
- Validação da entrada, cálculo e persistência da urgência, que pertencem ao change `add-registro-avaliacao`
- Criação da tabela, do Streams, do tópico e da assinatura, que pertencem ao change `add-fundacao-projeto`

## Impact

- Spec nova: `notificacao-critica`
- Código afetado: nova função `notificar-critica` e seu gatilho no template SAM
- Dependências:
  - `add-fundacao-projeto`: Streams habilitado na tabela e tópico de administradores com assinatura por e-mail
  - `add-registro-avaliacao`: o item persistido traz `urgencia`, descrição e data de envio
- Pendências para o `design.md` deste change, a cargo do arquiteto:
  - Política de reprocessamento, incluindo o que acontece quando as tentativas se esgotam
  - Como excluir eventos de alteração. O Streams está configurado com a imagem nova do item, então o filtro `urgencia = CRITICA` sozinho descarta remoções, mas deixa passar a alteração de um item crítico
- Ações manuais do Gabriel: confirmar a assinatura do e-mail do tópico em cada ambiente, já prevista em `add-fundacao-projeto`, para que o aviso chegue ao destinatário

## Rastreabilidade

| Requisito | Origem |
|---|---|
| Aviso imediato de avaliação crítica | `project.md`, Regras de domínio (Urgência) e Arquitetura; D5 e D6 |
| Conteúdo obrigatório do aviso | `project.md`, Regras de domínio (Entrada e Aviso de urgência); D4 e D7 |
| Acionamento restrito a avaliações críticas | `project.md`, Arquitetura (filtro `urgencia = CRITICA`) |
| Aviso somente na criação da avaliação | D1 |
| Falha na publicação não é sucesso silencioso | D3 |
| Avaliação registrada não depende do aviso | `project.md`, Arquitetura (função de responsabilidade única acionada por evento); D3 |
| Entrega pelo menos uma vez, sem deduplicação | D2 |
| Descrição fora dos logs em nível INFO | `project.md`, Convenções (Código) |

## Decisões fora do project.md

Decisões derivadas para este change. Nenhuma delas está escrita no `project.md`.

- **D1. Só a criação gera aviso.** O `project.md` fixa o filtro por urgência, mas não diz nada sobre o tipo de evento. Alteração ou remoção de item não geram aviso, mesmo quando uma alteração leva o item para CRITICA.
- **D2. Entrega pelo menos uma vez.** A spec não promete exatamente uma vez. Duplicata eventual é aceita e não há deduplicação, para manter o escopo dentro do prazo. A garantia vale enquanto a política de reprocessamento não se esgotar.
- **D3. Tratamento de falha.** Falha ao publicar não é tratada como sucesso e o evento é reprocessado conforme a política que o arquiteto definir. A falha não afeta a avaliação já registrada.
- **D4. Data de envio é a da avaliação.** O aviso traz a data de envio persistida no item, não o momento em que o aviso foi publicado. Assim o valor não muda quando há reprocessamento.
- **D5. Significado de imediato.** O `project.md` não define prazo numérico. Imediato significa publicar na própria execução acionada pelo evento, sem agendamento e sem acumular avaliações. A spec não fixa tempo máximo em segundos.
- **D6. Um aviso por avaliação.** Avaliações críticas que chegam no mesmo lote geram um aviso cada, sem consolidação. O sucesso da função é a aceitação da mensagem pelo tópico. A entrega do e-mail a partir daí é responsabilidade do SNS e a função não a acompanha.
- **D7. Descrição completa.** O `project.md` limita a descrição a 1000 caracteres na entrada. A regra de que ela vai sem corte no corpo do aviso é decisão deste change.
