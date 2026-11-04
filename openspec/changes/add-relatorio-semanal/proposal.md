# Change: Relatório semanal de avaliações

## Why

O projeto promete aos administradores um relatório semanal com a consolidação dos feedbacks: lista de avaliações, quantidade por dia, quantidade por urgência e média das notas. As funções de registro e de aviso crítico não entregam essa visão consolidada. O relatório também precisa aparecer no vídeo de demonstração, que não pode esperar a segunda-feira, então a geração tem de ser acionável manualmente.

## What Changes

- Geração automática do relatório toda segunda-feira às 08:00 em America/Sao_Paulo
- Janela de apuração: os 7 dias civis anteriores à data de referência, com o dia da referência fora
- Conteúdo: lista de avaliações com descrição, urgência e data de envio, quantidade por dia, quantidade por urgência e média das notas com duas casas decimais
- Totais completos: os 7 dias e as 3 urgências aparecem mesmo com quantidade zero
- Semana sem avaliações gera relatório válido com zeros e sem média, e os administradores são avisados do mesmo jeito, com os zeros e `sem avaliações na janela` no lugar da média
- Relatório guardado no bucket privado de relatórios, com aviso pelo tópico de administradores que traz a localização, a média, a quantidade por urgência e a quantidade por dia, sem descrições nem notas individuais
- Acionamento manual, com data de referência opcional e rejeição de data inválida
- Reexecução para a mesma data de referência substitui o relatório anterior
- Falha ao gravar o relatório não dispara o aviso e encerra a execução como falha
- Falha ao ler as avaliações, mesmo parcial, não grava relatório, não dispara o aviso e encerra a execução como falha
- Falha ao publicar o aviso mantém o relatório guardado e encerra a execução como falha
- Descrição e nota individual fora dos logs de nível INFO

### Fora do escopo

- Formato visual elaborado do relatório
- Filtros
- Relatórios de períodos diferentes de 7 dias
- Histórico ou listagem de relatórios
- Alarmes e dashboard, que são do change `add-observabilidade-seguranca`

## Impact

- Spec nova: `relatorio-semanal`
- Código afetado: função `gerar-relatorio-semanal` e o agendamento dela no template SAM
- Depende de `add-fundacao-projeto`: índice `por-data`, bucket de relatórios e tópico de administradores
- Depende de `add-registro-avaliacao`: formato do item, com os atributos `descricao`, `nota`, `urgencia`, `dataEnvio` e `criadoEm`
- A janela é apurada em dias civis de America/Sao_Paulo e o índice `por-data` particiona por `dataEnvio`. Os cenários de limite da janela dependem de `add-registro-avaliacao` gravar `dataEnvio` nesse mesmo fuso, como a spec daquele change define hoje

## Rastreabilidade

| Requisito | Origem |
|---|---|
| Geração automática semanal | `project.md`, Relatório semanal e Arquitetura; data de referência da execução: D1 |
| Janela de apuração | `project.md`, Relatório semanal ("7 dias anteriores"); detalhamento: D1 |
| Conteúdo do relatório | `project.md`, Relatório semanal; urgência persistida: `project.md`, Urgência; duas casas decimais: D12 |
| Totais completos | D2 |
| Semana sem avaliações | D3 |
| Guarda do relatório em bucket privado | `project.md`, Arquitetura; `add-fundacao-projeto`, spec `fundacao-cloud` |
| Aviso aos administradores | `project.md`, Arquitetura; conteúdo do aviso: D4 |
| Acionamento manual | `project.md`, Relatório semanal; referência padrão: D5 |
| Data de referência opcional no acionamento manual | D6 e D7 |
| Reexecução sem relatórios conflitantes | D8 |
| Falha na gravação do relatório | D9 |
| Falha na leitura das avaliações | D10 |
| Falha na publicação do aviso | D11 |
| Proteção dos dados do aluno nos logs | `project.md`, Convenções, Código |

## Decisões fora do project.md

| Id | Decisão | Motivo | Origem |
|---|---|---|---|
| D1 | "7 dias anteriores" significa 7 dias civis em America/Sao_Paulo anteriores à data de referência, com a referência fora. Na execução agendada, a referência é o dia da execução. Numa segunda, a janela vai de segunda a domingo da semana anterior | O `project.md` não define os limites da janela nem o fuso da apuração | Orquestrador |
| D2 | Os 7 dias e as 3 urgências aparecem nos totais mesmo com quantidade zero | Deixa claro que um dia ou uma urgência não teve avaliações, em vez de omitir a informação | Derivada, indicada pelo orquestrador |
| D3 | Semana sem avaliações gera relatório válido, com lista vazia, zeros e sem média, e os administradores são avisados do mesmo jeito. Média ausente não é o mesmo que média 0 | O aviso confirma que a rotina rodou. Média 0 sugeriria que houve avaliações com nota 0 | Derivada, indicada pelo orquestrador |
| D4 | O aviso traz a data de referência, a informação para localizar o relatório, a média das notas, a quantidade por urgência e a quantidade por dia. Descrições e notas individuais ficam de fora. Na semana vazia, o aviso mostra os zeros e `sem avaliações na janela` no lugar da média. Ele não torna o relatório público | O `project.md` só indica que o relatório vai para S3 e SNS. Sem localização, o aviso não leva o administrador ao relatório. Com o resumo, o administrador vê os números da semana sem abrir o bucket, e o conteúdo escrito pelos alunos continua só no relatório privado | Localização: orquestrador. Resumo e o que fica de fora: decisão do Gabriel em 04/11/2026 |
| D5 | No acionamento manual sem parâmetro, a data de referência é o dia atual em America/Sao_Paulo | O `project.md` prevê o acionamento manual, mas não define qual janela ele cobre | Orquestrador |
| D6 | O acionamento manual aceita uma data de referência opcional | Pela regra da janela, uma avaliação registrada no próprio dia da gravação do vídeo não entraria no relatório. Informando a data de amanhã como referência, ela entra | Decisão do orquestrador, fora do `project.md` |
| D7 | Data de referência válida é uma data que existe no calendário, no formato yyyy-MM-dd, sem limite de passado ou futuro. Qualquer outro valor é rejeitado sem gerar relatório e sem aviso | O orquestrador pediu a rejeição de data inválida sem definir o que é inválido. O formato é o mesmo usado na consulta do índice `por-data`. A referência de amanhã precisa ser aceita, e nenhum limite adicional foi pedido | Rejeição: orquestrador. Definição de válida: derivada pelo PO |
| D8 | Existe um único relatório por data de referência. A reexecução substitui o anterior pelo resultado mais recente e gera novo aviso. Datas de referência diferentes têm janelas diferentes e seus relatórios convivem | Evita dois relatórios conflitantes para a mesma janela e mantém o acionamento manual seguro de repetir. O novo aviso segue a regra de avisar a cada relatório guardado | Unicidade: derivada, indicada pelo orquestrador. Substituição e novo aviso: derivadas pelo PO |
| D9 | Falha ao gravar o relatório não dispara o aviso e encerra a execução como falha | Um aviso sem relatório levaria o administrador a um local vazio ou desatualizado | Orquestrador |
| D10 | Falha ao ler as avaliações, inclusive leitura parcial de um dos 7 dias, não grava relatório, não dispara o aviso e encerra a execução como falha | Como a semana vazia é um relatório válido (D3), uma leitura incompleta poderia sair como relatório com zeros falsos | Recomendação do PO, indicada pelo orquestrador. Aprovada pelo Gabriel em 04/11/2026 |
| D11 | Falha ao publicar o aviso depois de gravar mantém o relatório guardado e encerra a execução como falha | Sem a falha, o relatório ficaria guardado sem ninguém saber. A reexecução para a mesma data de referência é segura pela D8 | Recomendação do PO, indicada pelo orquestrador. Aprovada pelo Gabriel em 04/11/2026 |
| D12 | A média é apresentada com duas casas decimais. Notas 5, 6 e 6 resultam em 5,67 | Sem precisão definida, uma média não exata não pode ser verificada por teste | Recomendação do PO, indicada pelo orquestrador. Aprovada pelo Gabriel em 04/11/2026 |
