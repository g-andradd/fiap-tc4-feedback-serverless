# QA: add-registro-avaliacao

Tarefas 5.1, 5.2 e 6.3 a 6.8. A validação local foi feita em 04/11/2026 sobre o working tree e repetida em 05/11/2026. A conferência na stack `feedback-dev` foi feita em 05/11/2026, sobre a execução 41968866186 (commit `09cee5d`, região `sa-east-1`).

**Validade da evidência da stack:**
- **Observação:** 05/11/2026, das 04:47 às 05:18 UTC. As leituras de configuração das tarefas 6.3 a 6.5 começaram às 04:47, as requisições de ponta a ponta rodaram das 04:57 às 05:00, e a coleção Postman e a última leitura de logs terminaram às 05:18. Os horários de início e de fim vêm dos arquivos de saída, gravados pelo relógio da máquina, que estava cerca de 8 s adiantado. Não foi registrado o horário exato de cada leitura das tarefas 6.3 a 6.5.
- **Sem reconferência final:** a sessão AWS expirou depois disso, e a tentativa de 14:36 UTC falhou com `Token has expired and refresh failed`. O horário exato da expiração não é conhecido. Nada na conta foi observado depois das 05:18 UTC.
- **Sem deploy novo até 14:39 UTC:** o orquestrador conferiu pelo `gh` que o último deploy continua sendo a execução 41968866186 e que a `develop` não mudou. O QA não repetiu essa conferência. Ela cobre deploys pela pipeline, não alterações feitas na conta fora dela.
- **Itens sem resposta HTTP salva:** os 16 itens do marcador `QA-202611050457-3e4306` só têm evidência na tabela e nos logs (ver "Itens de teste").

## Resultado

| Status | Quantidade |
|---|---|
| ok (executado) | 48 |
| dos quais, executados também na stack | 43 |
| dos quais, executados só no handler, com cliente simulado | 5 |
| ok (por configuração) | 0 |
| não coberto | 0 |
| falha | 0 |
| **Total de cenários** | **48** |

**O change está conforme pelo critério desta matriz, com 0 não coberto e 0 falha. Isso não quer dizer que os 48 cenários rodaram na stack.** Estes 5 seguem sem execução real e têm prova só em `mvn -B verify`, com cliente simulado:
- `Descrição com 1 caractere aceita`
- `Caractere fora do plano básico conta como um`, em que a stack executou só uma variante com marcador
- `Último segundo do dia em São Paulo`
- `Primeiro segundo do dia em São Paulo`
- `Falha ao persistir`

`Avaliação encontrada no índice por data`, antes não coberto, passou a ok com a consulta real ao índice `por-data`, pela data gravada.

Significado dos status:
- **ok (executado):** o teste rodou em `mvn -B verify` e a asserção prova o que o cenário pede no nível do handler, ou a execução na stack prova o que o cenário pede. A coluna Limite diz qual dos dois casos vale.
- **ok (por configuração):** o recurso garante o comportamento, mas ele não foi exercitado. Não equivale a teste.
- **não coberto:** não há evidência do que o cenário pede. Conta como falha.
- **falha:** o comportamento observado contraria o cenário.

## Limite da evidência

Os testes do handler usam `DynamoDbClient` simulado com Mockito, `Metrics` simulado e `RastreadorFalso`. Eles provam que, dado o evento, o handler responde o status e o corpo esperados e monta o `PutItemRequest` esperado, ou não chama `putItem`. A gravação real, a rota, o evento real da HTTP API, as permissões, o SnapStart e os logs no CloudWatch são provados só pela conferência na stack.

Na coluna Limite da matriz:
- **STACK:** além do teste do handler, o cenário foi repetido em `feedback-dev` e a resposta e o item real ou os logs reais foram conferidos. O nome em seguida é o caso da rodada `QA-202611050500-85eb85`. Quando o corpo do cenário traz um texto fixo, a descrição enviada leva o marcador `QA-` junto desse texto.
- **SIM, sem réplica:** só cliente simulado. A stack não reproduziu o caso, e o motivo está na própria linha. Esses cenários não têm previsão de execução real.

Legenda das classes de teste:
- `H`: `br.com.fiap.feedback.registro.RegistrarAvaliacaoHandlerTest`, com a classe aninhada no nome
- `L`: `br.com.fiap.feedback.registro.LogsRegistrarAvaliacaoTest`
- `C`: `br.com.fiap.feedback.dominio.CarimboDeTempoTest`
- `U`: `br.com.fiap.feedback.dominio.UrgenciaTest`
- `V`: `br.com.fiap.feedback.dominio.ValidadorAvaliacaoTest`

## Verificações locais (tarefa 5.2)

| Verificação | Resultado |
|---|---|
| `mvn -B verify` | `BUILD SUCCESS`. `dominio`: `Tests run: 79, Failures: 0, Errors: 0, Skipped: 0`. `registrar-avaliacao`: `Tests run: 124, Failures: 0, Errors: 0, Skipped: 0`. Antes dos testes do QA, o segundo módulo tinha 113 |
| `sam validate --lint` | `template.yaml is a valid SAM Template` |
| `openspec validate add-registro-avaliacao --strict` | `Change 'add-registro-avaliacao' is valid` |
| Imports de `functions/dominio/src` | 0 ocorrências de `software.amazon`, `com.amazonaws`, `com.fasterxml`, `org.slf4j` e `org.apache`, em `main` e `test`. Os imports de `main` são só `java.*` |
| `dependency:tree` de `dominio` | Escopo `compile`: só o próprio módulo, sem dependências. Todos os escopos: só JUnit, AssertJ e o que eles trazem. 0 ocorrências de `software.amazon.awssdk`, `com.amazonaws` e `jackson` |

O build local usa Microsoft OpenJDK 25 (25.0.5.0.2). O resultado local não comprova o build em Corretto 25: essa comprovação vem do CI, na tarefa 6.1.

## Conferência local de 05/11/2026

Com JDK Microsoft OpenJDK 25.0.5.0.2 definido só no comando, porque o `JAVA_HOME` do ambiente aponta para o JDK 21.0.2:

| Verificação | Resultado |
|---|---|
| Código testado | `HEAD` (`04e6add`) sem diferença de árvore para `09cee5d`. No working tree, só `README.md` e `openspec/project.md` estavam modificados, nenhum deles pelo QA |
| `mvn -B verify` | `BUILD SUCCESS`. `dominio`: `Tests run: 79, Failures: 0, Errors: 0, Skipped: 0`. `registrar-avaliacao`: `Tests run: 127, Failures: 0, Errors: 0, Skipped: 0`, incluídos os 3 de `PrimingSnapStartTest` |
| `sam validate --lint` | `template.yaml is a valid SAM Template` |
| `openspec validate add-registro-avaliacao --strict` | `Change 'add-registro-avaliacao' is valid` |

## Conferência na stack (tarefas 6.3 a 6.7)

Operações na conta: leituras com o perfil `tc4-admin` e registros pela API, sem exclusão e sem alteração de configuração. A stack estava em `UPDATE_COMPLETE`, e a execução 41968866186 apareceu no `gh` como `success`, em `develop`, com o commit `09cee5d`.

### 6.3. SnapStart no alias e prova prática de fundação D7: ok

| Item | Observado |
|---|---|
| `get-function-configuration` em `feedback-registrar-avaliacao-dev:live` | `Version: 3`, `State: Active`, `LastUpdateStatus: Successful`, `OptimizationStatus: On`, `ApplyOn: PublishedVersions`, `Runtime: java25` |
| `get-alias` | `live` aponta para a versão 3 |
| `$LATEST` | `OptimizationStatus: Off`, como previsto: o SnapStart só vale na versão publicada |
| `RESTORE_REPORT` no CloudWatch | `Restore Duration: 788.80 ms` e `Restore Duration: 441.53 ms` |
| Linhas `REPORT` dessas invocações | `Duration: 8027.41 ms`, com `Billed Restore Duration: 239 ms`; `Duration: 1155.28 ms`, com `Billed Restore Duration: 171 ms` |
| Pacote implantado da versão 3 | `lib/org.aspectj.aspectjrt-1.9.27.jar` presente. Sem `apache-client`, `apache5-client` e `netty-nio-client`. `url-connection-client` e o jar do `dominio` presentes |

**Prova de fundação D7:** `java25` com SnapStart funciona em `sa-east-1`. A versão publicada ficou ativa com a otimização ligada, e duas restaurações reais atenderam requisições.

**Prova da correção do `aspectjrt`:** a versão foi publicada com o jar no pacote e os hooks de priming não derrubaram a publicação, como aconteceu no primeiro deploy.

### 6.4. Integração no alias: ok

- `get-routes`: uma rota, `POST /avaliacao`, sem autorizador, como o escopo prevê.
- `get-integrations`: `AWS_PROXY`, payload 2.0, com URI `arn:aws:apigateway:sa-east-1:lambda:path/2015-03-31/functions/arn:aws:lambda:sa-east-1:<conta>:function:feedback-registrar-avaliacao-dev:live/invocations`. Não aponta para `$LATEST`.
- `get-policy` no alias: `lambda:InvokeFunction` para `arn:aws:execute-api:sa-east-1:<conta>:<id>/*/POST/avaliacao`. A função sem qualificador não tem política (`ResourceNotFoundException`).
- As 105 linhas `START` trazem `Version: 3`, e as 108 linhas da aplicação trazem `function_version` igual a 3.

### 6.5. Tags da API e do Stage: ok

- `get-api --api-id <id>` e `get-tags` na API: `ambiente=dev`, além de `projeto=feedback`.
- `get-stages` e `get-tags` em `/apis/<id>/stages/$default`: `ambiente=dev`, além de `projeto=feedback`. Essa é a tag do Stage que causou a segunda falha de deploy.
- Limite de taxa no Stage: `ThrottlingRateLimit: 10` e `ThrottlingBurstLimit: 20`, como a D1. O limite não foi exercitado.

### 6.6. Ponta a ponta: ok, com as exceções da matriz

Envio com a URL do output `ApiAvaliacoesUrl` (`<url>`). Rodada `QA-202611050500-85eb85`: 43 requisições, 18 respostas 201 e 25 respostas 400, sem outros status, todas com `content-type: application/json`.

| Grupo | Resultado |
|---|---|
| Três urgências | Notas 0 e 3 em `CRITICA`, 4 e 6 em `MEDIA`, 7 e 10 em `BAIXA`, na resposta e no item |
| Validação de `descricao` | 1001 caracteres, ausente, nula, vazia, só espaços, tabulação e quebra de linha, e número: 400 com `campos` igual a `descricao` |
| Validação de `nota` | -1, 11, ausente, `null`, 7.5, 7.0, 1e1, `"7"` e 99999999999999999999: 400 com `campos` igual a `nota` |
| Corpo malformado | Truncado, sem corpo, `[1, 2, 3]`, lista com marcador e tokens sobrando: 400 com `CORPO_INVALIDO` e `campos` vazio |
| Campos do cliente | `urgencia`, `dataEnvio`, `criadoEm` e `id` enviados e ignorados |
| Itens da rodada | 18 itens para 18 respostas 201, cada `id` da resposta igual ao do item, com os 6 atributos do contrato e nada além, `descricao` e `nota` iguais às enviadas, UUID v4, `criadoEm` com 24 caracteres e `dataEnvio` igual à data de `criadoEm` em UTC-3 |
| Não persistência | Tabela com 0 itens antes do primeiro envio, 16 depois da primeira rodada e 34 depois da segunda. A coleção Postman, com 21 rejeições, manteve 34, nenhum sem marcador |
| Índice `por-data` | `query` com `dataEnvio = 2026-11-05`: `Count` 34, 18 de 18 itens da rodada, ordem cronológica por `criadoEm` e todos os atributos projetados |

**Uma conferência do roteiro acusou diferença, e ela não é do sistema:** a comparação de `criadoEm` com o relógio da máquina saiu da janela em 18 de 18 itens, entre 8,62 s e 8,74 s atrás. A resposta `Date` da AWS mostrou a máquina 8 s adiantada. A comparação com o relógio da própria AWS passou: nos 34 itens, a linha de log do mesmo `id` vem de 9 ms a 6799 ms depois de `criadoEm`.

**Limite do cálculo:** o roteiro confere `dataEnvio` com deslocamento fixo de UTC-3, porque o Python local não tinha a base de fusos. Às 04:57 e às 05:00 UTC, as datas em UTC e em São Paulo são iguais, então esses itens não distinguem os fusos.

### 6.7. Logs: ok

Leitura completa do grupo `/aws/lambda/feedback-registrar-avaliacao-dev`, com 536 eventos e 105 linhas `REPORT`, igual ao total de requisições (41 + 43 + 21). Esse total inclui as 21 requisições da coleção Postman.

| Verificação | Resultado |
|---|---|
| Ocorrências de `QA-`, `3e4306`, `85eb85`, `987654`, `Aula`, `99999999999999999999` e `"descricao":` | 0 em todos |
| Filtro no servidor, às 05:16 UTC, antes da coleção Postman, com `QA-`, `987654`, `85eb85` e `3e4306` | 0 eventos em todos |
| Atributos `nota` ou `descricao` em linhas JSON | 0. `nota` só aparece como nome de campo em `campos`, em 34 linhas, o que a D9 permite |
| Formato | 213 linhas JSON: 108 da aplicação e 105 de métricas EMF. 323 linhas de plataforma em texto (`START`, `END`, `REPORT`, `INIT_*` e `RESTORE_*`), porque a função usa `LogFormat: Text`. 0 linhas de outro tipo |
| Nível | As 108 linhas da aplicação estão em INFO. Nenhuma em WARN ou ERROR |
| Conteúdo de `motivo` | Só `CorpoAusente`, `CorpoNaoEhObjeto`, `com.fasterxml.jackson.core.JsonParseException` e `com.fasterxml.jackson.core.io.JsonEOFException` |
| Retenção | 14 dias |
| Traces do X-Ray | 81 traces lidos às 05:16 UTC, antes da coleção Postman, com 0 ocorrências de `QA-`, `3e4306`, `85eb85`, `987654`, `Aula` e `descricao`. A anotação `urgencia` e os subsegmentos `Restore` e `DynamoDB` aparecem no trace mais longo |

### Itens de teste

Tabela `feedback-avaliacoes-dev`, com 34 itens, todos com marcador. Nenhum foi apagado.

| Marcador | Itens | `criadoEm` | Evidência |
|---|---|---|---|
| `QA-202611050457-3e4306` | 16 | 04:57:32 a 04:57:51 UTC | **Só na tabela e nos logs.** O roteiro do QA parou com erro próprio de codificação antes de salvar os resultados, e as respostas HTTP dessa rodada se perderam. Nenhum cenário da matriz depende só desta rodada |
| `QA-202611050500-85eb85` | 18 | 05:00:28 a 05:00:40 UTC | Respostas, tabela e logs. É a base da matriz |

Na primeira rodada, o mesmo erro do roteiro enviou o caso de tabulação com caracteres de controle crus, o que deu `Corpo rejeitado` nos logs, e não `Entrada rejeitada`. A segunda rodada corrigiu o corpo. Os itens não têm atributo próprio de teste: o marcador está em `descricao`. Duas requisições da segunda rodada são as bordas da observação E e não correspondem a cenário.

## Matriz: registro-avaliacao

### Registro de avaliação

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Avaliação válida registrada | ok (executado) | `H.Registro.avaliacaoValidaRegistrada` | STACK, `valida`: 201 e item com a descrição enviada e nota 9. A descrição leva o marcador junto de `Aula muito boa` |
| Envios iguais geram avaliações distintas | ok (executado) | `H.Registro.enviosIguaisGeramAvaliacoesDistintas` | STACK, `iguais1` e `iguais2`: dois 201, `id` diferentes e dois itens com a mesma descrição |

### Validação de descricao

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Descrição com 1 caractere aceita | ok (executado) | `H.ValidacaoDescricao.umCaractere`, `V.descricaoCom1CaractereAceita` | SIM, sem réplica. A descrição de 1 caractere não comporta o marcador `QA-`, então não foi enviada à stack |
| Descrição com 1000 caracteres aceita | ok (executado) | `H.ValidacaoDescricao.milCaracteres`, `V.descricaoCom1000CaracteresAceita` | STACK, `mil`: 201 e item com 1000 caracteres, marcador incluído |
| Descrição com 1001 caracteres rejeitada | ok (executado) | `H.ValidacaoDescricao.milEUmCaracteres`, `V.descricaoCom1001CaracteresRejeitada` | STACK, `milum`: 400 com `campos` igual a `descricao` |
| Caractere fora do plano básico conta como um | ok (executado) | `H.ValidacaoDescricao.milEmojis`, `V.milEmojisContamComoMilCaracteres`, que confere 2000 unidades UTF-16 | SIM, sem réplica. Por causa do marcador, a stack executou só uma variante, que não é o corpo de 1000 emojis do cenário: marcador e 971 emojis, com 1000 caracteres e 1971 unidades UTF-16, deram 201 (`emoji1000`); com 1001 caracteres, 400 (`emoji1001`) |
| Descrição ausente | ok (executado) | `H.ValidacaoDescricao.ausente` | STACK, `desc-ausente`: 400 com `campos` igual a `descricao` |
| Descrição nula | ok (executado) | `H.ValidacaoDescricao.nula` | STACK, `desc-nula`: 400 com `campos` igual a `descricao` |
| Descrição vazia | ok (executado) | `H.ValidacaoDescricao.vazia` | STACK, `desc-vazia`: 400 com `campos` igual a `descricao` |
| Descrição só com espaços | ok (executado) | `H.ValidacaoDescricao.soEspacos` | STACK, `desc-espacos`: 400 com `campos` igual a `descricao` |
| Descrição só com tabulação e quebra de linha | ok (executado) | `H.ValidacaoDescricao.soTabulacaoEQuebra`, com `\t\n\r\n` | STACK, `desc-tab`: 400 com `campos` igual a `descricao` |
| Descrição que não é texto | ok (executado) | `H.ValidacaoDescricao.naoTexto` | STACK, `desc-numero`: 400 com `campos` igual a `descricao` |
| Descrição persistida como recebida | ok (executado) | `H.ValidacaoDescricao.persistidaComoRecebida` | STACK, `espacos`: 201 e item com os dois espaços em cada ponta. A descrição leva o marcador junto de `boa aula` |

No handler, os cenários de rejeição usam `assertRejeitadoPor`, que confere status 400, `erro` igual a `ENTRADA_INVALIDA`, `campos` exatamente com o nome esperado e nenhuma chamada a `putItem`.

### Validação de nota

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Nota mínima aceita | ok (executado) | `H.ValidacaoNota.minima` | STACK, `nota0`: 201 |
| Nota máxima aceita | ok (executado) | `H.ValidacaoNota.maxima` | STACK, `nota10`: 201 |
| Nota abaixo do mínimo | ok (executado) | `H.ValidacaoNota.rejeitada`, valor `-1` | STACK, `n-1`: 400 com `campos` igual a `nota` |
| Nota acima do máximo | ok (executado) | `H.ValidacaoNota.rejeitada`, valor `11` | STACK, `n11`: 400 com `campos` igual a `nota` |
| Nota ausente | ok (executado) | `H.ValidacaoNota.ausente` | STACK, `n-ausente`: 400 com `campos` igual a `nota` |
| Nota nula | ok (executado) | `H.ValidacaoNota.rejeitada`, valor `null` | STACK, `n-null`: 400 com `campos` igual a `nota` |
| Nota decimal | ok (executado) | `H.ValidacaoNota.rejeitada`, valor `7.5` | STACK, `n7.5`: 400 com `campos` igual a `nota` |
| Nota decimal com parte fracionária zero | ok (executado) | `H.ValidacaoNota.rejeitada`, valor `7.0` | STACK, `n7.0`: 400 com `campos` igual a `nota` |
| Nota em notação de expoente | ok (executado) | `H.ValidacaoNota.rejeitada`, valor `1e1` | STACK, `n1e1`: 400 com `campos` igual a `nota` |
| Nota como texto | ok (executado) | `H.ValidacaoNota.rejeitada`, valor `"7"` | STACK, `n-texto`: 400 com `campos` igual a `nota` |
| Nota inteira muito grande | ok (executado) | `H.ValidacaoNota.rejeitada`, valor `99999999999999999999` | STACK, `n-grande`: 400 com `campos` igual a `nota` |

### Rejeição de corpo malformado

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| JSON truncado | ok (executado) | `H.CorpoMalformado.truncado`, com o corpo literal da spec | STACK, `truncado`: 400 com `CORPO_INVALIDO`. O corpo leva o marcador junto de `Aula boa` |
| Corpo vazio | ok (executado) | `H.CorpoMalformado.vazio` (corpo nulo), `H.CorpoMalformado.textoVazio` | STACK, `sem-corpo`: POST sem corpo, 400 com `CORPO_INVALIDO`, e `motivo` igual a `CorpoAusente` nos logs |
| JSON que não é objeto | ok (executado) | `H.CorpoMalformado.naoObjeto` | STACK, `array`: corpo `[1, 2, 3]`, 400 com `CORPO_INVALIDO` |

No handler, esses testes usam `assertCorpoInvalido`, que confere status 400, `erro` igual a `CORPO_INVALIDO`, a mensagem, `campos` vazio e nenhuma chamada a `putItem`.

### Entrada inválida não persiste

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Validação de campo reprovada | ok (executado) | `verify(cliente, never()).putItem(...)` em `assertRejeitadoPor`, usado por todos os testes de rejeição de campo de `H` | STACK: 20 rejeições por campo na rodada, e a tabela só ganhou os 18 itens das respostas 201, por leitura consistente |
| Corpo malformado | ok (executado) | `verify(cliente, never()).putItem(...)` em `assertCorpoInvalido`, usado por `H.CorpoMalformado` | STACK: 5 rejeições de corpo na rodada e 4 na coleção Postman, com a tabela sem item além das respostas 201 |

### Classificação de urgência

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Nota 0 | ok (executado) | `H.Classificacao.respostaEItemTrazemAUrgencia`, `0, CRITICA`; `U.classificaOsLimitesDaTabela` | STACK, `nota0`: resposta e item com `CRITICA` |
| Nota 3 | ok (executado) | `H.Classificacao.respostaEItemTrazemAUrgencia`, `3, CRITICA`; `U.classificaOsLimitesDaTabela` | STACK, `nota3`: resposta e item com `CRITICA` |
| Nota 4 | ok (executado) | `H.Classificacao.respostaEItemTrazemAUrgencia`, `4, MEDIA`; `U.classificaOsLimitesDaTabela` | STACK, `nota4`: resposta e item com `MEDIA` |
| Nota 6 | ok (executado) | `H.Classificacao.respostaEItemTrazemAUrgencia`, `6, MEDIA`; `U.classificaOsLimitesDaTabela` | STACK, `nota6`: resposta e item com `MEDIA` |
| Nota 7 | ok (executado) | `H.Classificacao.respostaEItemTrazemAUrgencia`, `7, BAIXA`; `U.classificaOsLimitesDaTabela` | STACK, `nota7`: resposta e item com `BAIXA` |
| Nota 10 | ok (executado) | `H.Classificacao.respostaEItemTrazemAUrgencia`, `10, BAIXA`; `U.classificaOsLimitesDaTabela` | STACK, `nota10`: resposta e item com `BAIXA` |
| Urgência informada pelo cliente é ignorada | ok (executado) | `H.Classificacao.urgenciaDoClienteIgnorada` | STACK, `urgencia`: 201, resposta e item com `CRITICA`, nota 2. A descrição leva o marcador junto de `Aula ruim` |

### Data de envio atribuída pelo sistema

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Último segundo do dia em São Paulo | ok (executado) | `H.DataEnvio.ultimoSegundo`, `C.ultimoSegundoDoDiaEmSaoPauloFicaNoDiaAnterior` | SIM, sem réplica. Usa `Clock.fixed`. Os itens da stack foram gravados quando UTC e São Paulo tinham a mesma data, então não exercitam a virada |
| Primeiro segundo do dia em São Paulo | ok (executado) | `H.DataEnvio.primeiroSegundo`, `C.primeiroSegundoDoDiaEmSaoPauloJaEhONovoDia` | SIM, sem réplica. Usa `Clock.fixed`. Mesma limitação da linha anterior |
| Data informada pelo cliente é ignorada | ok (executado) | `H.DataEnvio.dataDoClienteIgnorada` | STACK, `datas`: 201 e item com `dataEnvio` `2026-11-05` e `criadoEm` `2026-11-05T05:00:38.949Z` |
| Avaliação encontrada no índice por data | ok (executado) | `query` real no índice `por-data`, com `dataEnvio = 2026-11-05`: `Count` 34, com os 18 itens da rodada e todos os atributos | STACK. Consulta pela data gravada, como a tarefa 6.6 define, porque na stack não dá para fixar o dia `2026-11-10` do cenário. Sem teste no handler |

### Identificador, respostas e falha interna

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Identificador informado pelo cliente é ignorado | ok (executado) | `H.Identificador.idDoClienteIgnorado` | STACK, `id`: 201, `id` da resposta e do item é UUID, e não existe item `abc` |
| Corpo da resposta de sucesso | ok (executado) | `H.RespostasHttp.sucesso` | STACK, `resposta`: 201, corpo só com `id` e `urgencia`, `id` igual ao do item e urgência `MEDIA` |
| Um campo inválido | ok (executado) | `H.RespostasHttp.umCampoInvalido`, com o corpo exato. O status 400 foi acrescentado pelo QA, porque o teste não o conferia | STACK, `um-campo`: 400 com `campos` igual a `nota` |
| Dois campos inválidos | ok (executado) | `H.RespostasHttp.doisCamposInvalidos` | STACK, `dois-campos`: 400 com `campos` igual a `descricao` e `nota` |
| Falha ao persistir | ok (executado) | `H.FalhaInterna.falhaDoServico`, com ARN na mensagem da exceção, `falhaDoCliente`, `tempoEsgotadoNoSdk` e `falhaInesperada` | SIM, sem réplica. A exceção é lançada pelo mock. Provocar a falha na stack exigiria mudar configuração, o que este QA não faz. A sonda abaixo mostra o mesmo 500 com o SDK real e HTTP falso |

### Dados do aluno fora dos logs em nível INFO

| Cenário | Status | Teste | Limite |
|---|---|---|---|
| Registro bem-sucedido | ok (executado) | `L.registroBemSucedido`, `L.sucessoRegistraIdEUrgencia` | STACK: 34 linhas `Avaliação registrada` com `id` e `urgencia`, sem marcador e sem atributo `nota`, nos logs reais |
| Entrada rejeitada | ok (executado) | `L.entradaRejeitada`, com o marcador e `987654` | STACK, `log-rej`: `987654` e marcador com 0 ocorrências nos logs reais |
| Corpo malformado | ok (executado) | `L.corpoMalformado`, com 5 corpos, e `L.motivoNaoTemDadoDeEntrada`, com 11 caminhos | STACK, `truncado`, `array-marcador` e `tokens`: marcador com 0 ocorrências nos logs reais |

`L.capturaFunciona` prova que o appender enxerga todos os níveis e o texto de exceções. Sem isso, as verificações de ausência passariam com a captura quebrada.

## Defeitos

### D1. Número com mais de 1000 dígitos derruba o corpo inteiro (severidade baixa)

Com `{"descricao":"a","nota":5,"extra":<1001 dígitos>}`, a resposta observada é 400 com `CORPO_INVALIDO` e `motivo` igual a `com.fasterxml.jackson.core.exc.StreamConstraintsException`. O corpo é um objeto JSON bem formado, e a D8 do `proposal.md` diz que campos extras são ignorados "sem causar rejeição". Com o mesmo número em `nota`, a resposta também é `CORPO_INVALIDO`, com `campos` vazio, em vez de apontar o campo, como pedem a D6 do `proposal.md` e o requisito de erro de validação.

O limite vem do `StreamReadConstraints` padrão do Jackson. O aninhamento excessivo segue o mesmo caminho. Nenhum cenário da spec exercita isso: o valor do cenário de inteiro grande tem 20 dígitos e passa como esperado. A resposta continua 400, nada é gravado e nada vaza. `LeitorCorpoTest.numeroAcimaDoLimiteDoJacksonEhCorpoInvalido` fixa esse comportamento em `nota`.

**Situação:** aceito pelo Gabriel em 04/11/2026 como limite, registrado na D4 do `design.md`. O código de produção não foi alterado.

## Observações

Não reprovam cenário. As quatro primeiras foram relatadas pelo dev-java e conferidas aqui. O status "em aberto" em cada uma descreve o momento do QA de 04/11. A linha "Na stack" traz o que se viu em 05/11, e as observações I e J são novas.

**Decisões do Gabriel em 04/11/2026:**
- **A, mensagem do SDK no trace:** aceita como limite conhecido, registrada na D9 do `design.md`.
- **B, métrica `ColdStart`:** D6 corrigida. O dashboard de `add-observabilidade-seguranca` usa a duração de restauração do SnapStart.
- **C, DEBUG:** D9 ajustada. A regra vale para o nosso código, com a função em INFO.
- **D, versões do Jackson:** sem decisão registrada, continua em aberto.

As evidências marcadas como "sonda" vêm de um programa local fora do repositório, não versionado. Ele usa o handler real, o `DynamoDbClient` real com HTTP falso, sem rede, `Rastreador.XRAY` com emissor em memória e `Metrics` real, com as variáveis `AWS_LAMBDA_INITIALIZATION_TYPE` e `POWERTOOLS_METRICS_NAMESPACE` definidas à mão.

- **A. Mensagem da exceção do SDK no trace do X-Ray. Status: confirmada, em aberto.** `powertools-tracing` traz `aws-xray-recorder-sdk-aws-sdk-v2-instrumentor`, que registra `TracingInterceptor` em todo cliente do SDK. Em `onExecutionFailure`, ele chama `subsegment.addException`. Na sonda, com falha 400 cuja mensagem levava um marcador e com log em INFO, o marcador apareceu em `cause.message` e `cause.exceptions[].message` do subsegmento `DynamoDb`, com a pilha, e não apareceu em nenhuma linha de log. O marcador enviado em `descricao` não apareceu no trace: a lista do instrumentor para `PutItem` só captura `TableName`. A D9 proíbe dado do aluno em anotações e metadados de trace. Não foi possível verificar, sem a conta, se uma mensagem real do DynamoDB cita valores do item.
  - **Na stack:** nenhuma gravação falhou, então o caso não foi exercitado. Os 81 traces lidos não têm o marcador. Continua sem verificação se uma mensagem real do DynamoDB cita valores do item.
- **B. Métrica `ColdStart` não sai com SnapStart. Status: confirmada, em aberto.** `LambdaHandlerProcessor.isColdStart()` só devolve verdadeiro quando `AWS_LAMBDA_INITIALIZATION_TYPE` é `on-demand`. Na sonda, com `snap-start`, houve 0 métricas `ColdStart` e `cold_start: false` no log. Com `on-demand`, houve 1 métrica e `cold_start: true`. A D6 do `design.md` diz que o cold start "fica registrado pela métrica `captureColdStartMetric`", o que não vale para este template. O valor real da variável na função só pode ser visto na tarefa 6.7. O `Metrics` é simulado nos testes, então eles não pegam isso.
  - **Na stack: confirmada.** `cold_start` foi `false` nas 108 linhas da aplicação, inclusive nas duas invocações que vieram de restauração, e os 105 registros EMF só trazem `AvaliacaoRegistrada` e `EntradaRejeitada`, sem `ColdStart`. O valor da variável `AWS_LAMBDA_INITIALIZATION_TYPE` não foi lido diretamente.
- **C. Mensagem da exceção em nível DEBUG. Status: confirmada para o SDK e, por leitura do bytecode, para o X-Ray; em aberto.** Na sonda em DEBUG, o SDK emitiu 2 linhas com `error.message` e pilha, contendo o marcador da mensagem do serviço (`Request attempt 1 encountered non-retryable failure` e `Unable to acquire sufficient retry quota`). `UDPEmitter.sendSegment` chama `prettySerialize` quando o nível é DEBUG, o que incluiria a `cause` do ponto A. Esse trecho não rodou, porque a sonda usa emissor em memória. O marcador enviado em `descricao` teve 0 ocorrências em DEBUG e em TRACE. A produção está em INFO (`Root level="INFO"` no `log4j2.xml` e `POWERTOOLS_LOG_LEVEL: INFO` no template), e a spec só proíbe INFO. A D9 é mais estrita e cita DEBUG. Os testes de `L` usam nível `ALL` com cliente simulado, então não exercitam os logs do SDK.
  - **Na stack:** `POWERTOOLS_LOG_LEVEL` é `INFO`, e nenhuma linha da aplicação saiu em outro nível. O nível DEBUG não foi exercitado.
- **D. `jackson-datatype-jsr310` 2.14.2 com `jackson-databind` 2.22.3. Status: confirmada, em aberto.** A árvore mostra `aws-embedded-metrics:4.1.2` trazendo `jackson-datatype-jsr310:2.14.2` em escopo `runtime`. Na sonda, a JVM carregou 69 classes do jsr310 e o `flush` real gerou EMF válido, sem erro de linkagem. Essa evidência cobre só esse caminho e não é garantia de compatibilidade entre as versões.
  - **Na stack:** 105 registros EMF reais foram gerados, sem linha de erro de linkagem. Continua sem ser garantia de compatibilidade entre as versões.
- **E. Entradas de borda sem cenário.** Uma `descricao` com substituto isolado (`"\ud83d"`) passa na validação e o SDK a envia. A resposta do DynamoDB real não é conhecida, e uma rejeição viraria 500 para um erro de entrada. A 6.6 pode exercitar. `nota` igual a `-0` é aceita como 0. A spec não trata nenhum dos dois.
  - **Na stack:** `\ud83d` isolado, no caso `obsE-substituto`, respondeu 201 e foi gravado como `?`, ou seja, não como recebido. Não foi apurado em qual etapa ocorreu a troca. `-0`, no caso `obsE-menos-zero`, respondeu 201 e foi gravado com `nota` 0 e `urgencia` `CRITICA`. A spec não trata nenhum dos dois.
- **F. Ligação de produção fora do `mvn verify`.** Os testes não executam `new RegistrarAvaliacaoHandler()`, `Rastreador.XRAY` nem o `Metrics` real. Na sonda, o construtor público rodou com `AWS_REGION` e `TABELA_AVALIACOES` definidas, só em caminhos de rejeição, sem chamada à AWS, e respondeu 400 nos dois. O caminho completo depende das tarefas 6.3 e 6.6.
  - **Na stack: exercitada.** A função implantada executou `Rastreador.XRAY`, `Metrics` real e a gravação real, sem erro nas invocações da conferência.
- **G. Regra de credenciais só por revisão.** Um mutante que fixa `EnvironmentVariableCredentialsProvider` em `criarCliente` passa em todos os testes. A tarefa 3.7 prevê revisão, e a conferência abaixo não encontrou o problema, mas nada no build impede a regressão.
- **H. Nome de teste impreciso.** `L.base64Malformado` usa Base64 válido com JSON malformado. O Base64 realmente inválido passou a ser coberto por `L.motivoNaoTemDadoDeEntrada`.
- **I. Primeira invocação depois da restauração. Status: nova, em aberto.** A primeira invocação levou `Duration: 8027.41 ms`, e o trace total, 8924 ms. O limite de 8 s do SDK vale para a chamada ao DynamoDB, e o de 10 s, para a função. No trace, `## gravarAvaliacao` levou 4914 ms e `DynamoDB`, 4259 ms. A linha de log de sucesso saiu 6799 ms depois de `criadoEm`, e a diferença para os 4914 ms não foi atribuída. Nenhum limite foi atingido, mas a função ficou a cerca de 2 s dos 10 s, e o intervalo de 6799 ms ficaria a cerca de 1,2 s do limite do SDK, se todo ele fosse da chamada. Na segunda restauração, a primeira invocação levou 1155.28 ms, então o caso não se repete sempre. Uma execução mais lenta poderia responder 500, e não dá para descartar que o item fique gravado mesmo assim. Essa invocação é da primeira rodada, com resposta HTTP perdida: o item gravado e o log `Avaliação registrada` indicam sucesso, mas não foi possível confirmar o status 201.
- **J. `INIT` sem invocação às 04:58 UTC. Status: nova, causa não apurada.** Um fluxo de log separado traz `INIT_START` às 04:58:43 UTC e `INIT_REPORT Init Duration: 13859.97 ms` às 04:58:57 UTC, sem `START` nem `REPORT`, entre as duas rodadas e sem erro. A inicialização da publicação da versão, às 04:38 UTC, levou 12498.99 ms.

## Regras de SnapStart (D7)

Conferido por leitura e busca em `functions/*/src/main/java`. A confirmação do SnapStart na conta está na seção da tarefa 6.3.

| Regra | Resultado | Evidência |
|---|---|---|
| Nada de ID, instante ou gerador em campo estático | ok | Todos os campos estáticos são constantes, formatadores, `ZoneId`, `Logger`, `ObjectMapper`, `Duration`, `JsonNodeFactory`, `Map` de cabeçalhos e os recursos de `Recursos`. Sem `Random` nem `SecureRandom`. `Clock` fica em campo de instância |
| ID e instante gerados no handler | ok | `UUID.randomUUID()` e `Instant.now(relogio)` em `registrar`, linhas 143 e 144. `H.DataEnvio.instanteCapturadoUmaVezPorRequisicao` confere 0 leituras do relógio antes da primeira requisição |
| Cliente criado fora do handler | ok | `Recursos.CLIENTE` é `static final`, carregado pelo construtor público. Criar o cliente não chama a AWS. A conferência na stack não isolou se o runtime instancia o handler antes do snapshot |
| Nenhum provedor de credenciais fixado | ok | 0 ocorrências de `credentialsProvider` e de `EnvironmentVariableCredentialsProvider` em `main`. Ver a observação G |
| Cache de DNS antes do logger | ok | `Security.setProperty` é a primeira linha do bloco estático, antes de `TracingUtils.init()` e de `LOG`. `H.Inicializacao.cacheDeDns` confere o valor |
| `TracingUtils.init()` na inicialização | ok | Bloco estático do handler |
| Hostname e conexões próprias | ok | Nenhum uso. Só o cliente do SDK abre conexões |

## Logs (D9)

A mensagem de `CorpoInvalidoException` vai para `motivo` em INFO. Todos os caminhos de `LeitorCorpo`:

| Caminho | `motivo` | Pode conter entrada? |
|---|---|---|
| Corpo nulo ou vazio | `CorpoAusente` | Não, texto fixo |
| Nó que não é objeto | `CorpoNaoEhObjeto` | Não, texto fixo |
| Base64 inválido | `Base64Invalido` | Não, texto fixo. A mensagem da `IllegalArgumentException` é descartada |
| JSON inválido em texto | `e.getClass().getName()` | Não, nome de classe |
| JSON inválido em bytes | `e.getClass().getName()` | Não, nome de classe |

`CorpoInvalidoException` não encadeia causa nem guarda pilha. Os demais logs do handler levam `id`, `urgencia`, nomes de campos, classe da exceção, código de erro e ID da requisição, todos permitidos pela D9. `withLogging` usa a forma sem `input`. A única anotação de trace é `urgencia`. `Avaliacao.toString()` não expõe `descricao` nem `nota`, e `AvaliacaoTest.toStringNaoExpoeDescricaoNemNota` confere.

## Trechos do template.yaml

Conferidos no texto em 04/11, e iguais aos trechos do `design.md`. O recurso gerado na conta está nas seções das tarefas 6.3 a 6.5.

| Item | Resultado | Linhas |
|---|---|---|
| Alias | `AutoPublishAlias: live` em `Globals`, `SnapStart.ApplyOn: PublishedVersions` e `AutoPublishAliasAllProperties: true` na função | 34 a 36 e 149 |
| `PermissionsBoundary` | Vem de `Globals`. A função não sobrescreve | 27 |
| Política | Uma declaração inline, `dynamodb:PutItem`, em `!GetAtt TabelaAvaliacoes.Arn`. O SAM acrescenta as gerenciadas de log e X-Ray, conforme a D13 | 154 a 160 |
| Tag da API | `Tags: ambiente: !Ref Stage` | 129 e 130 |
| Rota | `POST /avaliacao`, `PayloadFormatVersion: "2.0"`, na `ApiAvaliacoes` | 161 a 168 |

## Dependências

`dependency:tree` com escopo `runtime` de `registrar-avaliacao`: sem `aspectjrt`, `apache-client`, `apache5-client` e `netty-nio-client`. `url-connection-client` presente. `httpclient` 4.5.14 vem do X-Ray, como a D12 prevê.

Atualização de 04/11/2026: esta validação local não executou os hooks de SnapStart, e o primeiro deploy falhou por isso. `aspectjrt` passou a entrar em escopo `runtime`, por decisão do Gabriel (D6 do `design.md` e `docs/incidentes/2026-11-04-snapstart-aspectjrt.md`). A nova conferência foi feita na tarefa 6.8:

- `dependency:tree` com escopo `runtime`, em 05/11: `aspectjrt` 1.9.27 em `runtime`, `url-connection-client` 2.57.8 e `httpclient` 4.5.14, sem `apache-client`, `apache5-client` e `netty-nio-client`.
- Pacote implantado da versão 3: o mesmo resultado para esses jars, exceto `httpclient`, que não foi procurado no pacote. O pacote tem 15469302 bytes e 74 entradas. `sam build` não foi repetido localmente nesta conferência.

## Testes acrescentados

- `LogsRegistrarAvaliacaoTest.motivoNaoTemDadoDeEntrada`: novo, com 11 execuções, uma por caminho de `LeitorCorpo` em texto e em Base64. Confere status 400, nível INFO, ausência do marcador e de `987654`, `motivo` exato e ausência do campo `error`.
- `RegistrarAvaliacaoHandlerTest.RespostasHttp.umCampoInvalido`: passou a conferir o status 400, exigido pelo cenário.

Nenhum código de produção foi alterado.

## Prova das asserções

Em uma cópia temporária, fora do repositório, 16 mutações foram aplicadas ao código, uma de cada vez. 15 foram detectadas pelos testes. A que sobreviveu é a da observação G.

| Mutação | Detectada por |
|---|---|
| `codePointCount` trocado por `length` | `V.milEmojisContamComoMilCaracteres` |
| `temConteudo` trocado por `isBlank` | `V.outrosBrancosUnicodeContamComoConteudo` |
| `7.0` aceito como inteiro | `LeitorCorpoTest`, `H.ValidacaoNota.rejeitada` |
| Tokens sobrando aceitos | `LeitorCorpoTest`, `H.CorpoMalformado.tokensSobrando` |
| `motivo` com a mensagem do Jackson | `L.excecaoDoJacksonSoPelaClasse`, `L.motivoNaoTemDadoDeEntrada` |
| `motivo` de Base64 com o corpo | Só `L.motivoNaoTemDadoDeEntrada`, o teste novo |
| `descricao` em log INFO | `L.registroBemSucedido` |
| UUID em campo estático | `H.Registro.enviosIguaisGeramAvaliacoesDistintas` |
| Gravação antes da resposta 400 | `assertRejeitadoPor` |
| `trim` em `descricao` | `H.ValidacaoDescricao.persistidaComoRecebida` |
| 500 com a mensagem da exceção | `H.FalhaInterna` |
| Limite de `MEDIA` trocado para 7 | `U.classificaOsLimitesDaTabela` |
| `dataEnvio` em UTC | `C.ultimoSegundoDoDiaEmSaoPauloFicaNoDiaAnterior`, `AvaliacaoTest` |
| Exceção passada ao logger | `L.falhaDoServico`, `L.falhaDoCliente`, `H.FalhaInterna.tempoEsgotadoNoSdk` |
| Urgência fixa no item | `H.Classificacao` |
| Provedor de credenciais fixado | **Não detectada** |

## Coleção Postman

`docs/postman/feedback-serverless.postman_collection.json`, formato v2.1, com 33 requisições: 12 de sucesso e limites aceitos, 7 de rejeição de `descricao`, 9 de `nota`, 1 com dois campos inválidos e 4 de corpo malformado. A URL base é a variável `baseUrl`, com o valor neutro `https://exemplo.invalid`. Não há ID de conta, e-mail nem URL real.

Validação: JSON válido e execução com Newman contra uma ponte HTTP local para o handler com cliente simulado, com 33 requisições, 67 asserções e 0 falhas. A ponte não passa pelo API Gateway.

Em 04/11/2026, por decisão do Gabriel, quatro requisições foram renomeadas para o nome descrever o que elas conferem, sem teste novo: a coleção só lê a resposta, então não prova persistência nem o que foi gravado no item. A prova está nos testes do handler e na tarefa 6.6. A mesma execução com Newman, depois da troca, deu 33 requisições, 67 asserções e 0 falhas. A ponte e o script de conferência ficam fora do repositório, então o repositório não tem checagem automatizada da coleção.

**Execução contra a stack, em 05/11/2026:** só as pastas de rejeição, sem alterar a coleção (`Rejeição de descricao`, `Rejeição de nota`, `Dois campos inválidos` e `Corpo malformado`), com `baseUrl` igual a `<url>` sem o caminho: 21 requisições, 42 asserções e 0 falhas. A tabela continuou com 34 itens. As 12 requisições de `Sucesso` não rodaram contra a stack: elas gravam itens sem o marcador `QA-`, o que as regras desta conferência não permitem. Os mesmos caminhos de sucesso foram exercitados pelo roteiro da tarefa 6.6, com marcador, exceto a descrição de 1 caractere e os 1000 emojis sem marcador.

| Nome anterior | Nome atual |
|---|---|
| Descrição com espaços nas pontas, persistida como recebida | Descrição com espaços nas pontas: 201 com id e urgência MEDIA |
| Urgência do cliente ignorada | Nota 2 com urgencia BAIXA enviada: 201 com id e urgência CRITICA |
| Datas do cliente ignoradas | dataEnvio e criadoEm enviados: 201 com id e urgência MEDIA |
| Identificador do cliente ignorado | id abc enviado: 201 com outro id e urgência MEDIA |
