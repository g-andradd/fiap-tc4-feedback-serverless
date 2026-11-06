# Delta: observabilidade

## Purpose

Detecção e visualização do funcionamento das três funções por ambiente: alarmes que avisam por e-mail quando uma função falha, inclusive nas falhas que o handler trata, um dashboard por ambiente, retenção de logs por ambiente, memória das funções e o tempo da primeira chamada ao SDK depois da restauração do SnapStart medido contra uma meta.

**Método de prova das falhas.** Nenhum cenário altera o `$LATEST` nem o código das funções na AWS. A stack prova o caminho filtro, métrica, alarme e notificação: uma linha que casa com o filtro, sem dado de aluno, é gravada com `aws logs put-log-events` num log stream de teste dentro do grupo de log da função, e o caminho do alarme até o e-mail é exercitado também com `aws cloudwatch set-alarm-state`. Que o handler grava a linha certa em cada falha é provado pelos testes unitários.

## ADDED Requirements

### Requirement: Tópico de alarmes por ambiente
O sistema SHALL provisionar, em cada ambiente, um tópico SNS de alarmes separado do tópico de administradores, com assinatura por e-mail para o endereço informado no parâmetro `EmailAdmin`.

#### Scenario: Tópico criado no deploy
- **WHEN** a stack `feedback-dev` é implantada
- **THEN** existe um tópico de alarmes com o sufixo `dev` no nome
- **AND** ele é diferente do tópico de administradores da mesma stack

#### Scenario: Assinatura criada no deploy
- **WHEN** a stack é implantada com um `EmailAdmin` válido
- **THEN** o tópico de alarmes tem uma assinatura de protocolo `email` para esse endereço
- **AND** o endereço recebe o e-mail de confirmação da assinatura

### Requirement: Notificação dos alarmes
Todo alarme do ambiente SHALL publicar uma notificação no tópico de alarmes do próprio ambiente quando entra no estado ALARM, e SHALL NOT publicar no tópico de administradores nem no tópico de alarmes de outro ambiente.

#### Scenario: Alarme em ALARM notifica
- **WHEN** cada alarme da stack `feedback-dev` é levado ao estado ALARM com `aws cloudwatch set-alarm-state`
- **THEN** o tópico de alarmes de dev registra uma mensagem publicada para cada alarme
- **AND** o endereço assinante recebe o e-mail de cada alarme, se a assinatura estiver confirmada

#### Scenario: Destino das ações de alarme
- **WHEN** as ações de todos os alarmes da stack `feedback-dev` são inspecionadas
- **THEN** cada alarme tem como ação de ALARM somente o tópico de alarmes de dev
- **AND** nenhum alarme aponta para o tópico de administradores

### Requirement: Alarme de falha do registro
O sistema SHALL manter, por ambiente, um alarme que entra em ALARM a partir de uma única ocorrência de falha da função `registrar-avaliacao`, considerando tanto a falha interna tratada pelo handler, respondida com status 500, quanto o erro de execução da função, inclusive por tempo esgotado. Entrada rejeitada com status 400 SHALL NOT disparar o alarme.

#### Scenario: Handler marca a falha interna
- **WHEN** o teste unitário simula falha na gravação da tabela durante uma requisição válida
- **THEN** a resposta tem status 500
- **AND** o handler grava uma linha de log que casa com o filtro do alarme de falha do registro

#### Scenario: Falha interna chega ao alarme
- **WHEN** uma linha igual à que o handler grava na falha interna, sem dado de aluno, é gravada com `aws logs put-log-events` num log stream de teste do grupo de log do registro de dev
- **THEN** a métrica de falha do registro de dev recebe um ponto
- **AND** o alarme de falha do registro de dev entra em ALARM e publica no tópico de alarmes de dev

#### Scenario: Tempo esgotado chega ao alarme
- **WHEN** uma linha no formato da linha de tempo esgotado do Lambda, sem dado de aluno, é gravada com `aws logs put-log-events` num log stream de teste do grupo de log do registro de dev
- **THEN** a métrica de falha do registro de dev recebe um ponto
- **AND** o alarme de falha do registro de dev entra em ALARM

#### Scenario: Erro de execução coberto pela definição
- **WHEN** a definição do alarme de falha do registro e o filtro que o alimenta são inspecionados
- **THEN** eles consideram a falha interna tratada e também os erros de execução da função, inclusive o tempo esgotado
- **AND** o limiar é atingido com uma ocorrência

#### Scenario: Entrada rejeitada
- **WHEN** o cliente envia apenas requisições rejeitadas com status 400 durante o período de avaliação
- **THEN** o alarme de falha do registro não entra em ALARM

### Requirement: Alarme de falha da notificação
O sistema SHALL manter, por ambiente, um alarme que entra em ALARM a partir de uma única ocorrência de falha ao publicar o aviso crítico, inclusive quando só parte do lote falha e a invocação termina sem erro de função, e também quando um item é descartado por falta de campo obrigatório ou quando a função termina com erro de execução, inclusive por tempo esgotado. O alarme SHALL NOT depender apenas da métrica `Errors` da função.

#### Scenario: Handler marca a falha parcial do lote
- **WHEN** o teste unitário simula falha na publicação de um evento do lote
- **THEN** a função devolve esse evento em `batchItemFailures` e termina sem erro de função
- **AND** o handler grava uma linha de log que casa com o filtro do alarme de falha da notificação

#### Scenario: Falha parcial do lote chega ao alarme
- **WHEN** uma linha igual à que o handler grava na falha de publicação, sem dado de aluno, é gravada com `aws logs put-log-events` num log stream de teste do grupo de log da notificação de dev
- **THEN** a métrica de falha da notificação de dev recebe um ponto
- **AND** o alarme de falha da notificação de dev entra em ALARM e publica no tópico de alarmes de dev

#### Scenario: Item descartado por campo ausente
- **WHEN** a função `notificar-critica` de dev recebe, pelo alias `live`, um evento de inserção de item crítico sem campo obrigatório
- **THEN** nenhum aviso é publicado
- **AND** o alarme de falha da notificação de dev entra em ALARM

#### Scenario: Tempo esgotado chega ao alarme
- **WHEN** uma linha no formato da linha de tempo esgotado do Lambda, sem dado de aluno, é gravada com `aws logs put-log-events` num log stream de teste do grupo de log da notificação de dev
- **THEN** a métrica de falha da notificação de dev recebe um ponto
- **AND** o alarme de falha da notificação de dev entra em ALARM

#### Scenario: Definição não depende só de Errors
- **WHEN** a definição do alarme de falha da notificação e o filtro que o alimenta são inspecionados
- **THEN** eles consideram a falha de publicação registrada pela função, além dos erros de execução e do tempo esgotado

#### Scenario: Lote publicado com sucesso
- **WHEN** todas as publicações de um lote têm sucesso
- **THEN** o alarme de falha da notificação não entra em ALARM

### Requirement: Alarme de falha do relatório
O sistema SHALL manter, por ambiente, um alarme que entra em ALARM a partir de uma única execução da função `gerar-relatorio-semanal` terminada com falha, seja na leitura das avaliações, na gravação do relatório, na publicação do aviso ou por tempo esgotado. A rejeição de data de referência inválida no acionamento manual SHALL NOT disparar o alarme.

#### Scenario: Handler marca a execução com falha
- **WHEN** o teste unitário simula falha na leitura das avaliações, na gravação do relatório e na publicação do aviso, um caso de cada vez
- **THEN** a execução termina com falha em cada caso
- **AND** o handler grava uma linha de log que casa com o filtro do alarme de falha do relatório

#### Scenario: Execução com falha chega ao alarme
- **WHEN** uma linha igual à que o handler grava na execução com falha, sem dado de aluno, é gravada com `aws logs put-log-events` num log stream de teste do grupo de log do relatório de dev
- **THEN** a métrica de falha do relatório de dev recebe um ponto
- **AND** o alarme de falha do relatório de dev entra em ALARM e publica no tópico de alarmes de dev

#### Scenario: Tempo esgotado chega ao alarme
- **WHEN** uma linha no formato da linha de tempo esgotado do Lambda, sem dado de aluno, é gravada com `aws logs put-log-events` num log stream de teste do grupo de log do relatório de dev
- **THEN** a métrica de falha do relatório de dev recebe um ponto
- **AND** o alarme de falha do relatório de dev entra em ALARM

#### Scenario: Handler não marca a data inválida
- **WHEN** o teste unitário aciona o relatório com a data de referência `2026-02-30`
- **THEN** a execução é rejeitada
- **AND** nenhuma linha gravada pelo handler casa com o filtro do alarme de falha do relatório

#### Scenario: Data de referência inválida na stack
- **WHEN** o relatório de dev é acionado manualmente pelo alias `live` com a data de referência `2026-02-30` e a execução é rejeitada
- **THEN** o alarme de falha do relatório de dev não entra em ALARM

### Requirement: Ausência de tráfego não dispara alarme
O sistema SHALL tratar como ausência de falha o período em que uma função não é invocada, de modo que nenhum alarme entre em ALARM nem publique notificação por falta de dado.

#### Scenario: Ambiente sem tráfego
- **WHEN** nenhuma das três funções da stack `feedback-dev` é invocada durante uma hora e nenhuma linha de teste é gravada nos grupos de log
- **THEN** nenhum alarme da stack fica em ALARM nesse período
- **AND** o tópico de alarmes de dev não registra mensagem publicada nesse período

### Requirement: Isolamento por ambiente
O sistema SHALL garantir que toda métrica usada por um alarme ou exibida no dashboard de um ambiente reflita somente as funções e os recursos daquele ambiente.

#### Scenario: Inspeção das definições
- **WHEN** as definições dos alarmes, dos filtros que os alimentam e dos widgets do dashboard da stack `feedback-hml` são inspecionadas
- **THEN** todas as métricas, filtros e consultas referenciados são restritos a recursos ou ao namespace do ambiente `hml`

#### Scenario: Falha em outro ambiente
Prova executada só na release, quando hml é implantado (task 8.2). Antes disso, vale o cenário de inspeção acima.

- **WHEN** as stacks `feedback-dev` e `feedback-hml` estão implantadas e a métrica de falha de uma função de dev recebe um ponto
- **THEN** o alarme de falha da mesma função em hml não entra em ALARM
- **AND** o dashboard de hml não exibe essa falha

### Requirement: Invocação sem falha sem log em WARN
O sistema SHALL NOT gravar linha de log da aplicação em nível WARN ou ERROR em invocação concluída sem falha nas funções `registrar-avaliacao`, `notificar-critica` e `gerar-relatorio-semanal`, e nenhum alarme SHALL usar o nível WARN como condição.

#### Scenario: Aviso crítico publicado
- **WHEN** a função `notificar-critica` publica com sucesso os avisos de todas as avaliações do lote
- **THEN** o grupo de log da função não tem linha em WARN nem em ERROR referente a essa invocação

#### Scenario: Registro e relatório com sucesso
- **WHEN** uma avaliação é registrada com sucesso e um relatório é gerado e avisado com sucesso
- **THEN** os grupos de log das duas funções não têm linha em WARN nem em ERROR referente a essas invocações

#### Scenario: Alarmes sem condição em WARN
- **WHEN** as definições de todos os alarmes e dos filtros de log que os alimentam são inspecionadas
- **THEN** nenhuma delas usa o nível WARN como condição

### Requirement: Dashboard por ambiente
O sistema SHALL provisionar pelo template um dashboard por ambiente que exiba, para cada uma das três funções, invocações, falhas, incluindo as tratadas pelo handler que os alarmes consideram, duração e duração de restauração do SnapStart, e que exiba também o estado de cada alarme do ambiente.

#### Scenario: Dashboard criado no deploy
- **WHEN** a stack `feedback-dev` é implantada
- **THEN** existe um único dashboard com o sufixo `dev` no nome
- **AND** ele tem visualizações de invocações, falhas, duração e duração de restauração para `registrar-avaliacao`, `notificar-critica` e `gerar-relatorio-semanal`

#### Scenario: Duração de restauração exibida
- **WHEN** uma função de dev é invocada depois de uma restauração do SnapStart
- **THEN** o dashboard de dev exibe a duração dessa restauração no período correspondente

#### Scenario: Falha exibida
- **WHEN** a métrica de falha da notificação de dev recebe um ponto pela linha de teste gravada com `aws logs put-log-events`
- **THEN** o dashboard de dev exibe uma falha da notificação no período correspondente

#### Scenario: Estado dos alarmes
- **WHEN** o dashboard de dev é aberto
- **THEN** ele mostra o estado atual dos três alarmes de falha de dev

### Requirement: Retenção de logs por ambiente
O sistema SHALL reter os logs dos grupos de log das três funções por 30 dias em prod e por 14 dias em dev e em hml.

#### Scenario: Retenção em dev
- **WHEN** os grupos de log das três funções da stack `feedback-dev` são inspecionados
- **THEN** cada um tem retenção de 14 dias

#### Scenario: Retenção definida pelo ambiente no template
- **WHEN** o template é inspecionado
- **THEN** a retenção dos grupos de log das três funções resulta em 30 dias com `Stage=prod` e em 14 dias com `Stage=dev` e com `Stage=hml`

#### Scenario: Retenção em prod
- **WHEN** a stack `feedback-prod` é implantada e os grupos de log das três funções são inspecionados
- **THEN** cada um tem retenção de 30 dias

### Requirement: Memória das funções
As três funções SHALL ser configuradas com 1024 MB de memória, definidos uma única vez para todas no template.

#### Scenario: Memória no template
- **WHEN** o template é inspecionado
- **THEN** a memória de 1024 MB está definida no bloco comum às três funções
- **AND** nenhuma função sobrescreve esse valor

#### Scenario: Memória na stack
- **WHEN** a configuração do alias `live` das três funções de dev é consultada
- **THEN** cada uma tem `MemorySize` igual a 1024

### Requirement: Aquecimento dos clientes do SDK
Cada função SHALL exercitar, antes do snapshot do SnapStart, todos os clientes do SDK que usa no handler: DynamoDB em `registrar-avaliacao`, SNS em `notificar-critica` e DynamoDB, S3 e SNS em `gerar-relatorio-semanal`. Falha no aquecimento SHALL NOT impedir a publicação da versão.

#### Scenario: Clientes exercitados antes do snapshot
- **WHEN** o hook executado antes do snapshot de cada função é testado com clientes simulados
- **THEN** cada cliente usado pelo handler da função recebe uma chamada durante o hook

#### Scenario: Falha no aquecimento
- **WHEN** um cliente lança exceção durante o aquecimento
- **THEN** o hook termina sem propagar a exceção
- **AND** a falha fica registrada em log sem dado do aluno

#### Scenario: Versão publicada com aquecimento
- **WHEN** a stack `feedback-dev` é implantada com o aquecimento nas três funções
- **THEN** a versão apontada pelo alias `live` de cada função tem `State` igual a `Active` e `OptimizationStatus` igual a `On`

### Requirement: Aquecimento sem efeito em dados
O aquecimento SHALL NOT gravar item na tabela de avaliações, publicar mensagem em tópico nem gravar objeto no bucket de relatórios.

#### Scenario: Deploy sem tráfego
- **WHEN** uma nova versão das três funções é publicada em dev e nenhuma requisição é feita até o fim do deploy
- **THEN** a quantidade de itens da tabela de avaliações não muda
- **AND** o tópico de administradores não registra mensagem publicada no período
- **AND** o bucket de relatórios não ganha objeto novo no período

### Requirement: Medição da primeira chamada depois da restauração
O sistema SHALL ter registradas, com o aquecimento e 1024 MB, a duração da restauração e a da primeira chamada de cada função a cada serviço da AWS depois dela, em ao menos duas restaurações por função, comparadas com a meta de metade do tempo máximo de chamada do cliente. Valor acima da meta SHALL ser registrado e SHALL NOT reprovar o change.

#### Scenario: Registro depois da restauração
- **WHEN** a primeira invocação de `registrar-avaliacao` depois de uma restauração em dev grava uma avaliação
- **THEN** o relatório de QA registra a duração da restauração e a do subsegmento do DynamoDB no trace do X-Ray
- **AND** compara a duração do subsegmento com a meta de 4 s

#### Scenario: Notificação depois da restauração
- **WHEN** a primeira invocação de `notificar-critica` depois de uma restauração em dev publica um aviso
- **THEN** o relatório de QA registra a duração da restauração e a do subsegmento do SNS no trace do X-Ray
- **AND** compara a duração do subsegmento com a meta de 2,5 s

#### Scenario: Relatório depois da restauração
- **WHEN** a primeira invocação de `gerar-relatorio-semanal` depois de uma restauração em dev gera e avisa o relatório
- **THEN** o relatório de QA registra a duração da restauração e a da primeira chamada ao DynamoDB, da chamada ao S3 e da chamada ao SNS no trace do X-Ray
- **AND** compara cada uma com a meta de 5 s

#### Scenario: Amostra mínima
- **WHEN** o relatório de QA deste change é inspecionado
- **THEN** ele traz ao menos duas restaurações medidas por função, com a memória de 1024 MB, a duração de cada restauração e de cada chamada

#### Scenario: Meta não atingida
- **WHEN** uma chamada medida fica acima da meta
- **THEN** o relatório de QA registra o valor medido e a diferença para a meta
- **AND** o cenário é considerado atendido, porque a meta é referência e não critério de aprovação

### Requirement: Custo dentro do nível gratuito
O conjunto de alarmes e dashboards SHALL caber no nível gratuito do CloudWatch com as stacks de dev, hml e prod implantadas ao mesmo tempo, somado aos alarmes e dashboards que já existem na conta, inclusive o alarme de cobrança em `us-east-1`. O limite fecha sem folga: nenhum alarme ou dashboard a mais, nem de teste, cabe no nível gratuito. O uso de computação das funções com 1024 MB SHALL continuar dentro do nível gratuito do Lambda no volume da demonstração.

#### Scenario: Contagem pelo template e pela conta
- **WHEN** o template é inspecionado e os alarmes e dashboards existentes na conta são listados em todas as regiões usadas
- **THEN** cada stack cria no máximo 3 métricas de alarme, e as três stacks somadas ao alarme de cobrança de `us-east-1` chegam a no máximo 10 métricas de alarme, o limite gratuito
- **AND** cada stack cria um único dashboard, e os três chegam a no máximo 3 dashboards, o limite gratuito

#### Scenario: Alarmes sem alta resolução
- **WHEN** as definições dos alarmes são inspecionadas
- **THEN** nenhum alarme usa período de alta resolução, que fica fora do nível gratuito

#### Scenario: Computação com 1024 MB
- **WHEN** o uso de computação do Lambda na conta no mês é consultado depois do QA deste change
- **THEN** o total em GB-segundo e o total de requisições ficam abaixo dos limites mensais do nível gratuito do Lambda
