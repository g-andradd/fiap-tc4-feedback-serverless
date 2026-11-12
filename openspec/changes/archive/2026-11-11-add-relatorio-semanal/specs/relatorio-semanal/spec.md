# Delta: relatorio-semanal

## Purpose

Consolidação semanal das avaliações de aulas em um relatório guardado de forma privada, com aviso aos administradores, gerado automaticamente toda segunda-feira e acionável manualmente para a demonstração em vídeo.

## ADDED Requirements

### Requirement: Geração automática semanal
O sistema SHALL gerar o relatório automaticamente toda segunda-feira às 08:00 no fuso America/Sao_Paulo, usando como data de referência o dia da execução nesse fuso.

#### Scenario: Agendamento implantado
- **WHEN** o agendamento da stack implantada é inspecionado
- **THEN** a recorrência é semanal, às segundas-feiras, às 08:00
- **AND** o fuso configurado é America/Sao_Paulo

#### Scenario: Execução agendada
- **WHEN** a geração é disparada pelo agendamento em 09/11/2026 (segunda-feira) às 08:00 em America/Sao_Paulo
- **THEN** a data de referência do relatório é 2026-11-09

### Requirement: Janela de apuração
O sistema SHALL considerar somente as avaliações cuja data de envio esteja nos 7 dias civis anteriores à data de referência, em America/Sao_Paulo, com o dia da própria referência fora da janela.

#### Scenario: Janela de uma segunda-feira
- **WHEN** a data de referência é 2026-11-09 (segunda-feira)
- **THEN** a janela vai de 2026-11-02 (segunda-feira) a 2026-11-08 (domingo), com os dois extremos incluídos

#### Scenario: Avaliação no primeiro dia da janela
- **WHEN** a data de referência é 2026-11-09 e existe uma avaliação enviada em 2026-11-02
- **THEN** a avaliação entra no relatório

#### Scenario: Avaliação no último dia da janela
- **WHEN** a data de referência é 2026-11-09 e existe uma avaliação enviada em 2026-11-08
- **THEN** a avaliação entra no relatório

#### Scenario: Avaliação no dia da referência
- **WHEN** a data de referência é 2026-11-09 e existe uma avaliação enviada em 2026-11-09
- **THEN** a avaliação não aparece na lista e não é contada em nenhum total nem na média

#### Scenario: Avaliação oito dias antes da referência
- **WHEN** a data de referência é 2026-11-09 e existe uma avaliação enviada em 2026-11-01
- **THEN** a avaliação não aparece na lista e não é contada em nenhum total nem na média

#### Scenario: Limite final avaliado no fuso de São Paulo
- **WHEN** a data de referência é 2026-11-09 e existem avaliações enviadas às 23:59 de 08/11/2026 e às 00:00 de 09/11/2026, ambos os horários em America/Sao_Paulo
- **THEN** somente a avaliação das 23:59 de 08/11/2026 entra no relatório

#### Scenario: Limite inicial avaliado no fuso de São Paulo
- **WHEN** a data de referência é 2026-11-09 e existe uma avaliação enviada às 23:59 de 01/11/2026 em America/Sao_Paulo, que em UTC já é 02/11/2026
- **THEN** a avaliação não entra no relatório

### Requirement: Conteúdo do relatório
O sistema SHALL incluir no relatório a lista das avaliações da janela com descrição, urgência e data de envio, a quantidade de avaliações por dia, a quantidade de avaliações por urgência e a média das notas, apresentada com duas casas decimais.

#### Scenario: Semana com avaliações
- **WHEN** a janela contém três avaliações: nota 2 enviada em 2026-11-03, nota 5 enviada em 2026-11-03 e nota 8 enviada em 2026-11-05
- **THEN** a lista traz as três avaliações, cada uma com descrição, urgência e data de envio
- **AND** a quantidade por dia é 2 em 2026-11-03 e 1 em 2026-11-05
- **AND** a quantidade por urgência é 1 CRITICA, 1 MEDIA e 1 BAIXA
- **AND** a média das notas é 5,00

#### Scenario: Média não exata
- **WHEN** a janela contém somente três avaliações, com notas 5, 6 e 6
- **THEN** a média das notas apresentada é 5,67

#### Scenario: Urgência vem do que foi persistido
- **WHEN** uma avaliação da janela foi persistida com uma urgência
- **THEN** o relatório apresenta essa mesma urgência, sem recalculá-la a partir da nota

### Requirement: Totais completos
O sistema SHALL apresentar nos totais os 7 dias da janela e as 3 urgências (CRITICA, MEDIA e BAIXA), inclusive os que tiverem quantidade zero.

#### Scenario: Dias e urgências sem avaliações
- **WHEN** a data de referência é 2026-11-09 e a janela contém somente uma avaliação, de nota 8, enviada em 2026-11-03
- **THEN** a quantidade por dia traz as 7 datas de 2026-11-02 a 2026-11-08, com 1 em 2026-11-03 e 0 nas demais
- **AND** a quantidade por urgência traz BAIXA com 1, CRITICA com 0 e MEDIA com 0

### Requirement: Semana sem avaliações
O sistema SHALL gerar um relatório válido quando a janela não tiver avaliações, com a lista vazia, todos os totais em zero e sem média, e SHALL avisar os administradores do mesmo jeito.

#### Scenario: Janela vazia
- **WHEN** a geração é executada para uma janela sem nenhuma avaliação
- **THEN** o relatório é guardado com a lista vazia, os 7 dias em zero e as 3 urgências em zero
- **AND** o relatório não apresenta valor de média
- **AND** os administradores recebem o aviso

#### Scenario: Média zero não se confunde com ausência de média
- **WHEN** a janela contém somente avaliações de nota 0
- **THEN** o relatório apresenta média igual a 0,00

### Requirement: Guarda do relatório em bucket privado
O sistema SHALL guardar cada relatório gerado no bucket privado de relatórios do ambiente em execução.

#### Scenario: Relatório guardado
- **WHEN** a geração termina com sucesso no ambiente `dev`
- **THEN** o relatório está no bucket de relatórios da stack `feedback-dev`

#### Scenario: Acesso anônimo negado
- **WHEN** uma requisição sem credenciais tenta ler um relatório guardado
- **THEN** o acesso é negado

### Requirement: Aviso aos administradores
O sistema SHALL publicar, a cada relatório guardado, um aviso no tópico de administradores do ambiente, contendo a data de referência, a informação necessária para localizar o relatório no bucket, a média das notas, a quantidade de avaliações por urgência e a quantidade de avaliações por dia, e SHALL NOT incluir no aviso descrições nem notas individuais.

#### Scenario: Aviso permite localizar o relatório
- **WHEN** um relatório é guardado com sucesso
- **THEN** o tópico de administradores recebe um aviso com a data de referência
- **AND** a localização informada no aviso corresponde ao relatório que acabou de ser guardado

#### Scenario: Aviso traz o resumo da janela
- **WHEN** um relatório é guardado para a data de referência 2026-11-09 e a janela contém três avaliações: nota 2 enviada em 2026-11-03, nota 5 enviada em 2026-11-03 e nota 8 enviada em 2026-11-05
- **THEN** o aviso traz a média das notas igual a 5,00
- **AND** o aviso traz a quantidade por urgência: 1 CRITICA, 1 MEDIA e 1 BAIXA
- **AND** o aviso traz a quantidade por dia, com as 7 datas de 2026-11-02 a 2026-11-08: 2 em 2026-11-03, 1 em 2026-11-05 e 0 nas demais

#### Scenario: Aviso sem descrições
- **WHEN** a janela contém uma avaliação com a descrição `TEXTO-MARCADOR-AVISO` e o relatório é guardado
- **THEN** o relatório guardado contém `TEXTO-MARCADOR-AVISO`
- **AND** o aviso publicado não contém `TEXTO-MARCADOR-AVISO`

#### Scenario: Aviso sem notas individuais
- **WHEN** a janela contém avaliações com as notas 2, 5 e 8 e o relatório é guardado
- **THEN** o aviso publicado não traz a nota de nenhuma avaliação
- **AND** a média é o único valor derivado das notas no aviso

#### Scenario: Aviso da semana vazia
- **WHEN** um relatório é guardado para uma janela sem nenhuma avaliação
- **THEN** o aviso traz as 3 urgências em zero e os 7 dias em zero
- **AND** o aviso traz `sem avaliações na janela` no lugar da média
- **AND** o aviso traz a localização do relatório

### Requirement: Acionamento manual
O sistema SHALL permitir acionar a geração manualmente em qualquer dia, sem depender do agendamento, e, quando nenhuma data de referência for informada, SHALL usar como referência o dia atual em America/Sao_Paulo.

#### Scenario: Acionamento manual sem parâmetro
- **WHEN** a geração é acionada manualmente em 03/11/2026 (terça-feira), sem data de referência
- **THEN** a data de referência é 2026-11-03
- **AND** a janela vai de 2026-10-27 a 2026-11-02
- **AND** o relatório é guardado e os administradores recebem o aviso, como na execução agendada

#### Scenario: Dia atual calculado no fuso de São Paulo
- **WHEN** a geração é acionada manualmente, sem data de referência, às 22:00 de 03/11/2026 em America/Sao_Paulo, quando em UTC já é 04/11/2026
- **THEN** a data de referência é 2026-11-03

#### Scenario: Avaliação do próprio dia fica fora sem parâmetro
- **WHEN** uma avaliação é enviada em 03/11/2026 e a geração é acionada manualmente no mesmo dia, sem data de referência
- **THEN** a avaliação não entra no relatório

### Requirement: Data de referência opcional no acionamento manual
O sistema SHALL aceitar no acionamento manual uma data de referência opcional no formato yyyy-MM-dd, que precisa existir no calendário, e SHALL rejeitar um valor inválido sem gerar relatório e sem avisar os administradores.

#### Scenario: Referência de amanhã inclui a avaliação de hoje
- **WHEN** uma avaliação é enviada em 03/11/2026 e a geração é acionada manualmente no mesmo dia com data de referência 2026-11-04
- **THEN** a janela vai de 2026-10-28 a 2026-11-03
- **AND** a avaliação entra no relatório

#### Scenario: Referência no passado
- **WHEN** a geração é acionada manualmente em 03/11/2026 com data de referência 2026-10-26
- **THEN** a janela vai de 2026-10-19 a 2026-10-25

#### Scenario: Formato inválido
- **WHEN** a geração é acionada manualmente com data de referência `09/11/2026`
- **THEN** a execução é rejeitada informando que a data de referência é inválida
- **AND** nenhum relatório é gravado e nenhum aviso é publicado

#### Scenario: Data inexistente no calendário
- **WHEN** a geração é acionada manualmente com data de referência `2026-02-30`
- **THEN** a execução é rejeitada informando que a data de referência é inválida
- **AND** nenhum relatório é gravado e nenhum aviso é publicado

#### Scenario: Valor vazio
- **WHEN** a geração é acionada manualmente com data de referência em texto vazio
- **THEN** a execução é rejeitada informando que a data de referência é inválida
- **AND** nenhum relatório é gravado e nenhum aviso é publicado

#### Scenario: Rejeição preserva relatório existente
- **WHEN** já existe um relatório guardado e a geração é acionada com uma data de referência inválida
- **THEN** o relatório existente permanece inalterado

### Requirement: Reexecução sem relatórios conflitantes
O sistema SHALL manter um único relatório por data de referência, de modo que uma nova execução para a mesma data substitua o relatório anterior pelo resultado da execução mais recente.

#### Scenario: Reexecução com avaliação nova
- **WHEN** a geração é executada duas vezes com a data de referência 2026-11-09 e, entre as duas execuções, uma avaliação com envio em 2026-11-08 é registrada
- **THEN** o bucket contém um único relatório para a data de referência 2026-11-09
- **AND** esse relatório inclui a avaliação registrada entre as execuções

#### Scenario: Manual seguida de agendada na mesma data
- **WHEN** a geração é acionada manualmente com data de referência 2026-11-09 e depois disparada pelo agendamento em 09/11/2026
- **THEN** o bucket contém um único relatório para a data de referência 2026-11-09, com o resultado da execução agendada

#### Scenario: Aviso da reexecução
- **WHEN** uma reexecução substitui o relatório de uma data de referência
- **THEN** os administradores recebem um novo aviso, com a localização do relatório vigente

#### Scenario: Datas de referência diferentes
- **WHEN** a geração é executada com as datas de referência 2026-11-04 e 2026-11-09
- **THEN** o bucket contém um relatório para cada data, cada um com a sua janela

### Requirement: Falha na gravação do relatório
O sistema SHALL encerrar a execução como falha quando não conseguir gravar o relatório, e SHALL NOT publicar o aviso aos administradores nesse caso.

#### Scenario: Gravação falha
- **WHEN** a gravação do relatório no bucket falha
- **THEN** nenhum aviso é publicado no tópico de administradores
- **AND** a execução termina com falha, e não com sucesso

### Requirement: Falha na leitura das avaliações
O sistema SHALL encerrar a execução como falha quando não conseguir ler por completo as avaliações de qualquer um dos 7 dias da janela, e SHALL NOT gravar relatório nem publicar aviso nesse caso.

#### Scenario: Leitura falha em todos os dias
- **WHEN** a leitura das avaliações falha para todos os dias da janela
- **THEN** nenhum relatório é gravado e nenhum aviso é publicado
- **AND** a execução termina com falha, e não com sucesso

#### Scenario: Leitura falha em um dos 7 dias
- **WHEN** a data de referência é 2026-11-09 e a leitura de 2026-11-05 falha, enquanto os outros 6 dias são lidos com sucesso
- **THEN** nenhum relatório é gravado e nenhum aviso é publicado
- **AND** a execução termina com falha, e não com sucesso

#### Scenario: Leitura interrompida no meio de um dia
- **WHEN** a leitura de um dos 7 dias retorna parte das avaliações e falha ao obter o restante
- **THEN** nenhum relatório é gravado e nenhum aviso é publicado
- **AND** a execução termina com falha, e não com sucesso

### Requirement: Falha na publicação do aviso
O sistema SHALL manter o relatório guardado e encerrar a execução como falha quando não conseguir publicar o aviso depois de gravar o relatório.

#### Scenario: Publicação falha depois da gravação
- **WHEN** o relatório é gravado no bucket e a publicação do aviso no tópico de administradores falha
- **THEN** o relatório permanece guardado no bucket
- **AND** a execução termina com falha, e não com sucesso

#### Scenario: Reexecução depois da falha na publicação
- **WHEN** a publicação do aviso falhou para a data de referência 2026-11-09 e a geração é executada de novo para a mesma data, com a publicação funcionando
- **THEN** o bucket contém um único relatório para a data de referência 2026-11-09
- **AND** os administradores recebem o aviso com a localização desse relatório
- **AND** a execução termina com sucesso

### Requirement: Proteção dos dados do aluno nos logs
O sistema SHALL NOT registrar em log de nível INFO o conteúdo da descrição nem a nota individual de uma avaliação durante a geração do relatório.

#### Scenario: Geração com avaliações
- **WHEN** a geração processa uma avaliação com a descrição `TEXTO-MARCADOR-LOG` e os logs de nível INFO da execução são inspecionados
- **THEN** nenhuma entrada de nível INFO contém `TEXTO-MARCADOR-LOG`
- **AND** nenhuma entrada de nível INFO traz a nota individual da avaliação
