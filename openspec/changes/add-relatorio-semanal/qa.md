# QA: add-relatorio-semanal

Tarefa 4.1. Validação local feita em 05/11/2026 sobre o working tree da branch `feature/add-relatorio-semanal`, sem commit.

**Nada foi executado na AWS.** Nenhum comando `aws`, `sam deploy`, `sam build` ou `mvn install` foi rodado pelo QA nesta validação, e a conta não foi tocada. A stack `feedback-dev` ainda não recebeu este change. As tarefas 5.1 a 5.9 ficam para depois do merge.

## Resultado

| Status | Quantidade |
|---|---|
| ok (executado) | 40 |
| ok (por configuração) | 1 |
| não coberto | 1 |
| falha | 0 |
| **Total de cenários** | **42** |

**O change ainda não está conforme como um todo:** cenário sem cobertura conta como falha, e há 1 nessa condição, `Agendamento implantado`, que pede a inspeção da stack e só fecha com a tarefa 5.4.

**Para o deploy, não há impedimento local:** nenhum comportamento observado contraria a spec e o cenário em aberto não tem como fechar antes do deploy. A condição é integrar antes o código de `add-notificacao-critica`, porque esta branch sozinha não passa no `sam build` (ver "Limite deste worktree").

Significado dos status:
- **ok (executado):** o teste rodou em `mvn -B verify` e a asserção prova o que o cenário pede no nível do handler ou do domínio. Neste relatório, nenhum cenário foi executado contra a stack.
- **ok (por configuração):** o recurso garante o comportamento, mas ele não foi exercitado. Não equivale a teste.
- **não coberto:** não há evidência do que o cenário pede. Conta como falha. Declaração no `template.yaml` é indício, não evidência do recurso implantado, como em `add-fundacao-projeto`.
- **falha:** o comportamento observado contraria o cenário.

## Limite da evidência

Os 40 cenários com ok (executado) usam `DynamoFalso`, `S3Falso` e `SnsFalso`, de `ApoioTeste`, `Metrics` simulado com Mockito e `RastreadorFalso`. O `queryPaginator` e o `RequestBody` são os reais do SDK, sobre o cliente falso. Isso significa:

- **O que está provado:** dado o evento e o relógio, o handler consulta as partições esperadas, grava o texto esperado na chave esperada, publica a mensagem esperada ou deixa de chamar o serviço, e termina com a exceção esperada.
- **O que não está provado:** o disparo real pelo Scheduler, a leitura real do índice, a gravação real no S3, a publicação real no SNS, a chegada do e-mail, as permissões, o SnapStart e os logs no CloudWatch.

Na coluna Limite da matriz:
- **SIM:** só clientes falsos. Quando o cenário fala em "bucket" ou "tópico", a prova é a requisição capturada. A tarefa indicada repete o caminho na stack.
- **SIM, sem réplica:** só clientes falsos, e a stack não reproduz o caso de forma simples, porque depende de relógio fixo, de falha provocada ou de massa controlada. Sem previsão de execução real em `tasks.md`.
- **LOG:** captura em memória com o layout real do `log4j2.xml` e nível `ALL`, com clientes falsos. O SDK real não roda nesses testes. A tarefa 5.8 confere no CloudWatch.
- **STACK:** só a stack implantada fecha o cenário.

Legenda das classes de teste:
- `H`: `br.com.fiap.feedback.relatorio.GerarRelatorioSemanalHandlerTest`, com a classe aninhada no nome
- `L`: `br.com.fiap.feedback.relatorio.LogsGerarRelatorioSemanalTest`
- `A`: `br.com.fiap.feedback.dominio.AvisoRelatorioTest`
- `C`: `br.com.fiap.feedback.dominio.ConsolidadorRelatorioTest`
- `R`: `br.com.fiap.feedback.dominio.DataReferenciaTest`
- `F`: `br.com.fiap.feedback.dominio.FormatadorRelatorioTest`
- `J`: `br.com.fiap.feedback.dominio.JanelaApuracaoTest`

## Verificações locais

| Verificação | Resultado |
|---|---|
| `mvn -B verify` | `BUILD SUCCESS`. `dominio`: `Tests run: 153, Failures: 0, Errors: 0, Skipped: 0`. `registrar-avaliacao`: `Tests run: 127, Failures: 0, Errors: 0, Skipped: 0`. `gerar-relatorio-semanal`: `Tests run: 76, Failures: 0, Errors: 0, Skipped: 0`. `notificar-critica` não tem código nem testes neste worktree |
| `sam validate --lint --region sa-east-1` | `template.yaml is a valid SAM Template` |
| `openspec validate add-relatorio-semanal --strict` | `Change 'add-relatorio-semanal' is valid` |
| Imports de `functions/dominio/src` | 0 arquivos com `software.amazon`, `com.amazonaws`, `com.fasterxml`, `org.slf4j` ou `org.apache`. Os imports de `main` são só `java.*` |
| Blocos do `design.md` no `template.yaml` | Os dois blocos da seção "Alterações no template.yaml" aparecem literalmente no arquivo. `Globals` não mudou |

O build local usa Microsoft OpenJDK 25 (25.0.5.0.2). O resultado local não comprova o build em Corretto 25: essa comprovação vem do CI, na tarefa 5.1.

## Limite deste worktree

O `template.yaml` contém de propósito os blocos `LogNotificarCritica` e `NotificarCritica`, conforme o plano de integração aprovado, mas `functions/notificar-critica` só tem o `pom.xml` nesta branch.

- **`sam build` falha neste worktree isolado.** Não é falha do change. O QA não rodou o comando, como pedido. A evidência é o log de outro agente, de 05/11/2026: `Required target/classes directory was not produced from 'mvn package'`, em `NotificarCritica`.
- **`sam build` passou com as duas branches juntas em diretório temporário.** Evidência do mesmo agente, só lida pelo QA: `Build Succeeded`, depois de um `mvn install` com 171, 127, 65 e 76 testes sem falha. Nesse diretório, `functions/gerar-relatorio-semanal/src` é igual ao deste worktree, e o `lib/` da função tem `aspectjrt` 1.9.27 e `url-connection-client`, sem `aspectjtools`, `aspectjweaver`, `apache-client`, `apache5-client` e `netty-nio-client`.
- **Consequência:** um merge só desta branch em `develop` faria o `sam build` do deploy falhar. O código de `add-notificacao-critica` precisa entrar antes ou junto.
- **O que esta validação não cobre:** os 76 testes acima rodaram sem o código da notificação. A combinação das duas branches só tem a evidência do outro agente até o CI da tarefa 5.1.

## Matriz: relatorio-semanal

### Geração automática semanal

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Agendamento implantado | **não coberto** | Nenhuma inspeção de stack. Só o indício: `template.yaml`, linhas 289 e 290, com `cron(0 8 ? * MON *)` e `America/Sao_Paulo`. Nenhum teste lê o template | STACK. Fecha com a tarefa 5.4 |
| Execução agendada | ok (executado) | `H.GeracaoAutomatica.execucaoAgendada`: relógio em 08:00 de 09/11/2026 em São Paulo e evento `{}`, igual ao `Input` do template. Resposta com `2026-11-09` e objeto na chave da data | SIM, sem réplica. O disparo real não está em `tasks.md` (observação B) |

### Janela de apuração

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Janela de uma segunda-feira | ok (executado) | `H.Janela.janelaDeUmaSegunda`, com os 7 dias consultados em ordem; `J.dias`, `J.limitesDaJanela`, `J.janelaDeUmaSegunda` | SIM. Tarefa 5.5 |
| Avaliação no primeiro dia da janela | ok (executado) | `H.Janela.extremosEntram`, `C.Janela.primeiroDia` | SIM, sem réplica |
| Avaliação no último dia da janela | ok (executado) | `H.Janela.extremosEntram`, `C.Janela.ultimoDia` | SIM, sem réplica |
| Avaliação no dia da referência | ok (executado) | `H.Janela.foraDaJanela`, que confere a ausência na lista, `Avaliações (1)`, média 8,00 e `CRITICA: 0`; `C.Janela.foraDaJanela` | SIM, sem réplica |
| Avaliação oito dias antes da referência | ok (executado) | `H.Janela.foraDaJanela`, `C.Janela.foraDaJanela` | SIM, sem réplica |
| Limite final avaliado no fuso de São Paulo | ok (executado) | `H.Janela.limiteFinal`, `C.Janela.limiteFinalEmSaoPaulo`. A `dataEnvio` vem de `Avaliacao.nova` com `02:59Z` e `03:00Z` de 09/11 | SIM, sem réplica. Usa instante fixo |
| Limite inicial avaliado no fuso de São Paulo | ok (executado) | `H.Janela.limiteInicial`, `C.Janela.limiteInicialEmSaoPaulo`, com `02:59Z` de 02/11 | SIM, sem réplica. Usa instante fixo |

### Conteúdo do relatório

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Semana com avaliações | ok (executado) | `H.Conteudo.semanaComAvaliacoes`, com o texto completo do objeto; `C.Conteudo.semanaComAvaliacoes` | SIM, sem réplica |
| Média não exata | ok (executado) | `H.Conteudo.mediaNaoExata`, `C.Media.naoExata` | SIM, sem réplica |
| Urgência vem do que foi persistido | ok (executado) | `H.Conteudo.urgenciaPersistida`, com nota 9 persistida como `CRITICA`; `C.Conteudo.urgenciaPersistida` | SIM, sem réplica. O registro real não grava essa combinação |

### Totais completos

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Dias e urgências sem avaliações | ok (executado) | `H.TotaisCompletos.diasEUrgenciasComZero`, com o bloco literal; `C.TotaisCompletos.diasEUrgenciasSemAvaliacoes` | SIM, sem réplica |

### Semana sem avaliações

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Janela vazia | ok (executado) | `H.SemanaVazia.janelaVazia`, com objeto gravado e aviso publicado. O teste do handler confere só parte dos dias, e os 7 dias em zero vêm de `F.semanaVazia`, com o texto completo, e `C.TotaisCompletos.janelaVazia` | SIM. Tarefa 5.5, se a janela estiver vazia |
| Média zero não se confunde com ausência de média | ok (executado) | `H.SemanaVazia.mediaZero`, `C.Media.soZeros`, `F.mediaZero` | SIM, sem réplica |

### Guarda do relatório em bucket privado

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Relatório guardado | ok (executado) | `H.Guarda.relatorioGuardado`: bucket configurado, chave, tipo de conteúdo e texto em UTF-8. O template liga `BUCKET_RELATORIOS` a `!Ref BucketRelatorios` e libera `s3:PutObject` em `relatorios/*`, o mesmo prefixo de `LocalizacaoRelatorio` | SIM. O bucket vem de constante do teste. Tarefa 5.5 |
| Acesso anônimo negado | ok (por configuração) | `template.yaml`, linhas 100 a 104, com as quatro opções de bloqueio público, e 106 a 122, com política só de `Deny` sem TLS. Bloco sem alteração neste change. O bucket real foi conferido em `add-fundacao-projeto/qa.md`, em 04/11/2026, com leitura feita naquela tarefa, não nesta. `H.Guarda.relatorioGuardado` confere gravação sem ACL | STACK. Nenhuma requisição anônima foi feita. Tarefa 5.7 |

### Aviso aos administradores

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Aviso permite localizar o relatório | ok (executado) | `H.Aviso.localizacaoCorresponde`, que monta a localização esperada a partir da requisição capturada no S3; `H.Aviso.topicoEAssunto`; `A.localizacaoCorrespondeAChave` | SIM. Tarefa 5.5 |
| Aviso traz o resumo da janela | ok (executado) | `H.Aviso.textoCompletoSemDescricaoNemNota`, `A.textoCompleto`, com o texto completo | SIM. Tarefa 5.5, com outra massa |
| Aviso sem descrições | ok (executado) | `H.Aviso.textoCompletoSemDescricaoNemNota`: objeto contém o marcador e aviso não contém; `A.semDescricao` | SIM. Tarefa 5.5 |
| Aviso sem notas individuais | ok (executado) | `H.Aviso.textoCompletoSemDescricaoNemNota`, por igualdade do texto inteiro com notas 2, 5 e 8; `A.textoCompleto`; `A.recebeSoResumoELocalizacao` | SIM. Tarefa 5.5 |
| Aviso da semana vazia | ok (executado) | `A.semanaVazia`, com o texto completo; `H.SemanaVazia.janelaVazia`, no caminho do handler | SIM. Tarefa 5.5, se a janela estiver vazia |

### Acionamento manual

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Acionamento manual sem parâmetro | ok (executado) | `H.AcionamentoManual.semParametro`: referência, primeira e última partição, janela no objeto e aviso; `H.AcionamentoManual.eventoSemData` | SIM. Tarefa 5.5, em outra data |
| Dia atual calculado no fuso de São Paulo | ok (executado) | `H.AcionamentoManual.diaAtualEmSaoPaulo`, `R.ausenteUsaDiaAtualEmSaoPaulo`, `R.fusoDoRelogioNaoImporta` | SIM, sem réplica. Usa `Clock.fixed` |
| Avaliação do próprio dia fica fora sem parâmetro | ok (executado) | `H.AcionamentoManual.proprioDiaFora` | SIM. Tarefa 5.5 |

### Data de referência opcional no acionamento manual

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Referência de amanhã inclui a avaliação de hoje | ok (executado) | `H.DataOpcional.referenciaDeAmanha` | SIM. Tarefa 5.5. Ver a observação C |
| Referência no passado | ok (executado) | `H.DataOpcional.referenciaNoPassado`, `R.dataNoPassado`, `J.limitesDaJanela` | SIM. A tarefa 5.5 não prevê esta data |
| Formato inválido | ok (executado) | `H.DataOpcional.valorInvalido`, valor `09/11/2026`; `R.textoInvalido` | SIM. Tarefa 5.5 |
| Data inexistente no calendário | ok (executado) | `H.DataOpcional.valorInvalido`, valor `2026-02-30`; `R.textoInvalido`; `R.anoBissexto` | SIM. Tarefa 5.5, se escolher este valor |
| Valor vazio | ok (executado) | `H.DataOpcional.valorInvalido`, valor em texto vazio; `R.textoInvalido` | SIM. Tarefa 5.5, se escolher este valor |
| Rejeição preserva relatório existente | ok (executado) | `H.DataOpcional.rejeicaoPreservaRelatorio`, com o mesmo objeto, 1 gravação e 1 publicação depois da rejeição | SIM. A tarefa 5.5 pode conferir |

`H.DataOpcional.valorInvalido` confere a exceção, a mensagem `Data de referência inválida`, 0 consultas, 0 gravações, 0 tentativas de publicação e nenhum objeto.

### Reexecução sem relatórios conflitantes

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Reexecução com avaliação nova | ok (executado) | `H.Reexecucao.comAvaliacaoNova`: 2 gravações, uma única chave e o objeto final com a avaliação nova | SIM. O `S3Falso` substitui por chave, como o S3 sem versionamento. O template não declara versionamento. Tarefa 5.6. Ver a observação C |
| Manual seguida de agendada na mesma data | ok (executado) | `H.Reexecucao.manualSeguidaDeAgendada`: uma única chave e o objeto com a avaliação incluída antes da segunda execução | SIM, sem réplica. Ver a observação B |
| Aviso da reexecução | ok (executado) | `H.Reexecucao.avisoDaReexecucao`, com 2 publicações e a mesma localização | SIM. Tarefa 5.6 |
| Datas de referência diferentes | ok (executado) | `H.Reexecucao.datasDiferentes`, com 2 objetos, cada um com a sua janela | SIM. Tarefas 5.5 e 5.6 |

### Falhas

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Gravação falha | ok (executado) | `H.FalhaNaGravacao.gravacaoFalha`: exceção, 1 tentativa de gravação e 0 tentativas de publicação | SIM, sem réplica. A exceção vem do falso |
| Leitura falha em todos os dias | ok (executado) | `H.Leitura.falhaEmTodosOsDias` | SIM, sem réplica |
| Leitura falha em um dos 7 dias | ok (executado) | `H.Leitura.falhaEmUmDia`, em 2026-11-05 | SIM, sem réplica. Ver a observação D |
| Leitura interrompida no meio de um dia | ok (executado) | `H.Leitura.falhaNoMeioDeUmDia`, com falha na segunda página de 2026-11-05 | SIM, sem réplica |
| Publicação falha depois da gravação | ok (executado) | `H.FalhaNaPublicacao.publicacaoFalha`: exceção, objeto presente e 1 gravação antes da tentativa de publicar | SIM, sem réplica |
| Reexecução depois da falha na publicação | ok (executado) | `H.FalhaNaPublicacao.reexecucaoDepoisDaFalha`: uma única chave, 1 aviso com a localização e resposta normal | SIM, sem réplica |

Os três cenários de leitura usam `assertFalhouSemGravarNemAvisar`, que confere `GeracaoRelatorioException` sem causa, 0 gravações e 0 tentativas de publicação.

### Proteção dos dados do aluno nos logs

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Geração com avaliações | ok (executado) | `L.geracaoComAvaliacoes`, com o marcador no objeto e fora dos logs; `L.sucessoNaoDependeDasNotas`, `L.falhaNaGravacaoNaoDependeDasNotas`, `L.falhaNaPublicacaoNaoDependeDasNotas`, que comparam os logs de duas execuções com notas diferentes | LOG. Tarefa 5.8 |

A comparação entre execuções é o que prova a ausência da nota, porque um dígito solto também aparece em datas e totais. `L.capturaFunciona` prova que o appender enxerga todos os níveis e o texto de exceções.

## Cenários que dependem da stack implantada

| Dependência | Cenários |
|---|---|
| Só a stack fecha (2) | `Agendamento implantado` (5.4), `Acesso anônimo negado` (5.7) |
| Provados no handler e repetíveis na stack (19) | `Janela de uma segunda-feira`, `Janela vazia`, `Relatório guardado`, os 5 cenários do aviso, `Acionamento manual sem parâmetro`, `Avaliação do próprio dia fica fora sem parâmetro`, `Referência de amanhã inclui a avaliação de hoje`, `Referência no passado`, `Formato inválido`, `Data inexistente no calendário`, `Valor vazio`, `Rejeição preserva relatório existente` (5.5); `Reexecução com avaliação nova`, `Aviso da reexecução`, `Datas de referência diferentes` (5.6) |
| Provado em memória, com conferência no CloudWatch (1) | `Geração com avaliações` (5.8) |
| Provados só com falsos, sem execução real prevista (20) | Os marcados como `SIM, sem réplica` na matriz |

Alguns itens da segunda linha dependem da massa e do valor escolhidos na hora, como indicado na matriz. O texto de `tasks.md` só obriga "sem data, com a data de amanhã e com data inválida", então a repetição dos demais não está garantida.

## Defeitos

Nenhum defeito encontrado no código de produção.

## Observações

Não reprovam cenário.

- **A. Ligação de produção fora do `mvn verify`.** Nenhum teste executa `new GerarRelatorioSemanalHandler()`, `Recursos`, `Rastreador.XRAY`, o `Metrics` real nem os clientes reais em uma chamada. `H.Inicializacao.construtorPublico` só confere que o construtor existe. Por leitura, as variáveis `TABELA_AVALIACOES`, `AMBIENTE`, `BUCKET_RELATORIOS` e `TOPICO_ADMINS_ARN` existem no template com esses nomes. O caminho completo depende das tarefas 5.3 e 5.5.
- **B. Execução agendada real sem tarefa.** A 5.4 inspeciona a configuração e a 5.5 usa invocação manual, então nada em `tasks.md` observa o Scheduler chamando o alias. A segunda-feira 09/11/2026 está dentro do prazo. Se a stack estiver implantada antes das 08:00 desse dia, dá para conferir o objeto de `2026-11-09` e o aviso com comandos só de leitura. A decisão de incluir essa conferência fica com o orquestrador.
- **C. Consistência eventual do índice.** O `DynamoFalso` responde na hora. Na stack, `Referência de amanhã inclui a avaliação de hoje` e `Reexecução com avaliação nova` podem falhar por poucos segundos, como a D4 do `design.md` prevê. Nas tarefas 5.5 e 5.6, é preciso esperar alguns segundos antes de acionar. Uma ausência imediata não indica defeito, mas a reexecução precisa trazer a avaliação.
- **D. Leitura interrompe no primeiro dia com falha.** O cenário `Leitura falha em um dos 7 dias` diz que os outros 6 dias são lidos com sucesso. Na implementação, os dias depois de 2026-11-05 nem são consultados, e o teste confere isso com `endsWith`. O efeito pedido pela spec é o mesmo.
- **E. Nenhum teste lê o `template.yaml`.** Uma troca do cron, do fuso, da política de repetição ou do nome de uma variável passa em `mvn -B verify` e em `sam validate --lint`. Os trechos foram conferidos no texto.
- **F. Nome de teste maior que a asserção.** `H.Inicializacao.tempoMaximoDaChamada` diz que os três clientes "usam o cliente HTTP escolhido", mas só confere o `apiCallTimeout`. A escolha do cliente fica provada pela leitura de `criarCliente` e pela árvore de dependências.
- **G. Resposta sem bucket não tem teste de ausência.** Os testes usam `containsEntry`, então acrescentar o bucket à resposta não quebraria nenhum deles. Hoje a ausência é provada só por leitura do handler, linhas 132 a 135.
- **H. Observações herdadas de `add-registro-avaliacao`.** O padrão é o mesmo, e elas não foram medidas de novo para esta função: mensagem de exceção do SDK no trace do X-Ray (A), métrica `ColdStart` que não sai com SnapStart (B), mensagem do SDK em DEBUG (C) e `jackson-datatype-jsr310` 2.14.2 com `jackson-databind` 2.22.3 (D), que continua nesta árvore. Os testes de `L` usam clientes falsos, então não exercitam os logs do SDK real.
- **I. Assunto do aviso.** `Relatório semanal 2026-11-09` tem caractere fora do ASCII. O `SnsFalso` não valida isso, mas a [página do Publish](https://docs.aws.amazon.com/sns/latest/api/API_Publish.html), consultada em 05/11/2026, aceita UTF-8 sem quebra de linha com menos de 100 caracteres. A publicação real é da tarefa 5.5.

## Divergências relatadas pelo dev-java

Nenhuma contraria a spec nem o que o `design.md` decide. Os nomes `LocalizacaoRelatorio` e `GeradorRelatorio`, o `apiCallTimeout` e as três métricas não aparecem nos documentos deste change, então convém registrá-los no `design.md`. O QA não editou esse arquivo.

| Divergência | Parecer | Evidência |
|---|---|---|
| `ambiente` em `ResumoSemanal` | Não contraria. A lista da D7 omite o campo, mas os textos da D5 e da D7 trazem `Ambiente: dev`, e a D7 limita o aviso ao resumo e à localização. Sem o campo no resumo, o aviso não teria de onde tirar o valor. O dado não é do aluno. Ajustar o texto da D7 | `ResumoSemanal`, linha 25; `FormatadorRelatorio.numeros`, linha 41; `A.textoCompleto` |
| `LocalizacaoRelatorio` novo | Não contraria. A D7 e a tarefa 1.6 falam em "a localização" sem definir o tipo, e a D9 não lista a classe. A chave segue a D5, e a classe não depende de AWS. Acrescentar à lista da D9 | `LocalizacaoRelatorio`; `A.localizacaoCorrespondeAChave`; `A.chavePorData` |
| Sequência em `GeradorRelatorio` em vez do handler | Não contraria a spec nem a D9, que define a ordem e exige que a exceção suba para o handler. Isso continua valendo, e a data continua sendo calculada no handler, como pede a D2. Difere só da letra da tarefa 2.4 | `GeradorRelatorio`, linhas 68 a 75; handler, linhas 129 e 130; `H.Ordem.leGravaPublica` |
| `apiCallTimeout` de 10 s | Não contraria. A D9 pede que a falha passe pelo handler, e o tempo limite do SDK é o que garante isso, como na D5 de `add-registro-avaliacao`. A conta fecha sem paginação extra: 9 chamadas somam 90 s, abaixo dos 120 s da função. Com mais de 12 chamadas no pior caso, o Lambda pode encerrar antes do `catch`: a execução continua em falha, mas sem o log de ERROR. **É um valor novo:** o de 8 s do registro consta como aprovado pelo Gabriel, e este não consta em lugar nenhum. Pedir a confirmação e registrar | Handler, linhas 56 a 58 e 99; `H.Inicializacao.tempoMaximoDaChamada` |
| Métricas `RelatorioGerado`, `DataReferenciaRejeitada` e `ErroInterno` | Não contraria. O `project.md` prevê métricas pelo Powertools, a tarefa 2.1 declara a dependência e os nomes seguem o padrão do registro. Não são alarmes, que estão fora do escopo. Os testes usam `Metrics` simulado, então o EMF real desta função não foi visto. Registrar os nomes, porque `add-observabilidade-seguranca` vai depender deles | Handler, linhas 51 a 53; `H.Metricas` |
| Resposta sem o bucket | Não contraria. Nem a spec nem o design definem o corpo da resposta. A escolha evita o ID da conta em `resposta.json`, arquivo que aparece no acionamento manual. O log e o aviso continuam trazendo o bucket, como pedem a D10 e a D7. Sem teste de ausência (observação G) | Handler, linhas 132 a 135 |
| Filtro de janela no consolidador | Não contraria. O "sem filtro" da D3 trata da `Query`, e `H.Janela.janelaDeUmaSegunda` confere `filterExpression` nulo. O filtro em memória é redundante em produção, porque o leitor só traz as 7 partições da janela e a `dataEnvio` é a chave da partição. Por isso, ele não descarta item em silêncio, o que iria contra a D3. Ele é o que dá sentido aos testes de janela no domínio | `ConsolidadorRelatorio`, linhas 36 a 46; `C.Janela` |

## Regras de SnapStart

Conferido por leitura e busca em `functions/gerar-relatorio-semanal/src/main` e nas classes novas do domínio. A confirmação na conta é da tarefa 5.3.

| Regra | Resultado | Evidência |
|---|---|---|
| Nada de data ou instante em campo estático | ok | Os campos estáticos são constantes, `Logger`, `Duration` e os recursos de `Recursos`. `Clock` fica em campo de instância |
| Data calculada dentro do handler | ok | Handler, linha 129. `H.AcionamentoManual.dataCalculadaACadaInvocacao` |
| Cliente criado fora do handler | ok | `Recursos.GERADOR` é `static final`, carregado pelo construtor público. Se o runtime instancia o handler antes do snapshot, só a conta confirma |
| Nenhum provedor de credenciais fixado | ok | 0 ocorrências de `credentialsProvider` e de `EnvironmentVariableCredentialsProvider` em `main`. Só por revisão, porque o build não impede a regressão |
| Cache de DNS antes do logger | ok | Primeira linha do bloco estático. `H.Inicializacao.cacheDeDns` |
| Hooks de priming | ok | `PrimingSnapStartTest`, com 3 testes |

## Trechos do template.yaml

Conferidos no texto. O recurso gerado na conta é das tarefas 5.2 a 5.4.

| Item | Resultado | Linhas |
|---|---|---|
| Blocos da notificação | Presentes de propósito, conforme o plano de integração | 170 a 236 e 332 a 337 |
| Função | `Timeout: 120`, `AutoPublishAliasAllProperties: true` e as variáveis `TABELA_AVALIACOES`, `BUCKET_RELATORIOS` e `TOPICO_ADMINS_ARN`. `AMBIENTE` vem de `Globals` | 244 a 258 |
| Política | `dynamodb:Query` em `index/por-data`, `s3:PutObject` em `relatorios/*`, `sns:Publish` no tópico e KMS com `kms:ViaService` do SNS. Sem `Scan`, `GetItem`, `GetObject` nem `ListBucket` | 259 a 282 |
| Agendamento | `cron(0 8 ? * MON *)`, `America/Sao_Paulo`, `Input: '{}'`, `Mode: "OFF"`, 3 tentativas, 3600 s e `PermissionsBoundary` no evento | 284 a 299 |
| Outputs | `GerarRelatorioSemanalNome` e `GerarRelatorioSemanalAliasArn` | 338 a 343 |

**Conferência contra o bootstrap,** só por leitura de `infra/bootstrap/github-oidc.yaml`, sem consultar a conta: o boundary cobre as ações da função e `lambda:InvokeFunction` com `:*`. A role de deploy cobre `scheduler:CreateSchedule` em `schedule/default/feedback-*-<amb>`, `iam:PassRole` para `scheduler.amazonaws.com` e a criação de roles com boundary. O nome `feedback-relatorio-semanal-<amb>` casa com esses padrões. O transform de 04/11/2026, gerado por outro agente e só lido aqui, mostra o alvo no alias `live` e o boundary na role do agendamento. O bloco correspondente do template é igual ao do `design.md`.

## Dependências

`dependency:tree` com escopo `runtime` de `gerar-relatorio-semanal`: 0 ocorrências de `apache-client`, `apache5-client`, `netty-nio-client`, `aspectjtools` e `aspectjweaver`. Presentes: `aspectjrt` 1.9.27, `dynamodb`, `s3`, `sns` e `url-connection-client` 2.57.8. `httpclient` 4.5.14 vem do X-Ray, como em `add-registro-avaliacao`.

## Testes acrescentados

Nenhum. O pedido desta tarefa limitou a edição a este arquivo, e o único cenário não coberto não se resolve com teste local. Nenhum código de produção foi alterado.

## Prova das asserções

Em uma cópia temporária, fora do repositório, 24 mutações foram aplicadas ao código, uma de cada vez, com `mvn -o test` dos módulos `dominio` e `gerar-relatorio-semanal`. As 24 foram detectadas. A execução sem mutação deu 153 e 76 testes sem falha, e a cópia voltou a ficar igual ao worktree depois da rodada.

| Mutação | Detectada por |
|---|---|
| Janela inclui a referência | 29 falhas, entre elas `J.limitesDaJanela`, `C.Janela.foraDaJanela` e `H.Janela.janelaDeUmaSegunda` |
| Consolidador sem filtro de janela | `C.Janela.foraDaJanela`, `C.Janela.limiteFinalEmSaoPaulo`, `C.Janela.limiteInicialEmSaoPaulo`, `C.Conteudo.ordem`. Nenhum teste do handler, porque o leitor já limita as partições |
| Dia atual em UTC | `R.ausenteUsaDiaAtualEmSaoPaulo`, `R.fusoDoRelogioNaoImporta`, `H.AcionamentoManual.diaAtualEmSaoPaulo` |
| Sem regex de formato | `R.textoInvalido`, `H.DataOpcional.valorInvalido` |
| `HALF_EVEN` na média | `C.Media.empateSobe` |
| Média zero na janela vazia | `A.semanaVazia`, `C.TotaisCompletos.janelaVazia`, `C.Media.janelaVazia`, `F.semanaVazia`, `H.SemanaVazia.janelaVazia` |
| Total por urgência recalculado da nota | `C.Conteudo.urgenciaPersistida`, `H.Conteudo.urgenciaPersistida` |
| Lista com urgência recalculada da nota | Só `H.Conteudo.urgenciaPersistida` |
| Publica antes de gravar | `H.FalhaNaGravacao.gravacaoFalha`, `H.FalhaNaPublicacao.publicacaoFalha`, `H.Ordem.leGravaPublica`, `L.falhaNaPublicacaoMostraAGravacao` |
| Lê só a primeira página | `H.Leitura.variasPaginas`, `H.Leitura.paginaVaziaNoMeio`, `H.Leitura.falhaNoMeioDeUmDia` |
| Falha de leitura ignorada | Os 3 cenários de `H.Leitura` com falha, `L.falhaDoServico` |
| Item inválido ignorado | Os testes de item inválido de `H.Leitura`, `L.itemInvalido`, `L.itemInvalidoSemId` |
| Falha de gravação ignorada | `H.FalhaNaGravacao.gravacaoFalha`, `H.Metricas.falha`, `L.falhaDoCliente` |
| Falha de publicação ignorada | `H.FalhaNaPublicacao.publicacaoFalha`, `H.FalhaNaPublicacao.reexecucaoDepoisDaFalha`, `L.falhaNaPublicacaoMostraAGravacao` |
| Chave diferente a cada execução | `A.chavePorData`, `H.Reexecucao.avisoDaReexecucao` e outros 32 |
| Objeto com ACL pública | `H.Guarda.relatorioGuardado` |
| Localização do aviso sem a chave certa | `A.localizacaoCorrespondeAChave`, `H.Aviso.localizacaoCorresponde` e outros 6 |
| Evento ignorado | `H.DataOpcional.valorInvalido`, `H.DataOpcional.rejeicaoPreservaRelatorio`, `H.Reexecucao.datasDiferentes` e outros |
| Exceção original relançada | `H.FalhaNaGravacao.gravacaoFalha`, `H.FalhaNaPublicacao.publicacaoFalha`, `L.falhaDoCliente` e outros |
| Descrição em log INFO | Só `L.geracaoComAvaliacoes` |
| Média em log INFO, sob outro nome de atributo | Só `L.sucessoNaoDependeDasNotas` |
| Notas em log INFO, sob outro nome de atributo | Só `L.sucessoNaoDependeDasNotas` |
| `apiCallTimeout` de 200 s | `H.Inicializacao.tempoMaximoDaChamada` |
| Sem `aspectjrt` no `pom.xml` | Os 3 testes de `PrimingSnapStartTest`, como a tarefa 2.8 pede |

A rodada só cobre o código Java. O `template.yaml` não passou por mutação, porque nenhum teste o lê (observação E).

## Coleção Postman

Não se aplica. O change não altera a API: o acionamento manual é uma invocação direta do Lambda (D1).
