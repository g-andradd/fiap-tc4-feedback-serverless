# QA: add-notificacao-critica

Tarefas 4.1 e 4.2. Validação local feita em 05/11/2026 sobre o working tree do worktree `fiap-tc4-notificacao`, branch `feature/add-notificacao-critica`, sem commit.

**Nada foi executado na AWS.** A stack não recebeu este change, e nenhum comando `aws`, `sam deploy` ou `sam build` foi executado. `mvn install` também não foi executado. As tarefas 5.1 a 5.9 ficam para depois do merge.

## Resultado

| Status | Quantidade |
|---|---|
| ok (executado) | 14 |
| ok (por configuração) | 9 |
| não coberto | 2 |
| falha | 0 |
| **Total de cenários** | **25** |

**O change ainda não está conforme:** cenário sem cobertura conta como falha, e há 2 nessa condição, `E-mail recebido pelo administrador` e `Sem invocação no ambiente implantado`. Os dois dependem da stack e são os únicos pendentes da seção 5, ambos na tarefa 5.6.

**Pode seguir para o deploy em dev,** com uma condição: a tarefa 3.3 (`mvn -B install`, `sam build` e conferência de `lib/`) não faz parte desta validação, e o worktree não tem `.aws-sam`, então este relatório não traz evidência dela. Não há falha nem defeito de produção que impeça o deploy.

Significado dos status:
- **ok (executado):** o teste rodou em `mvn -B verify` e a asserção prova o que o cenário pede no nível do handler. Nenhum cenário foi executado contra a stack.
- **ok (por configuração):** o template, o classpath ou a inspeção garantem o comportamento, mas ele não foi exercitado. Não equivale a teste. Quando só parte das cláusulas do cenário foi executada, vale o status da cláusula mais fraca.
- **não coberto:** não há evidência do que o cenário pede. Conta como falha.
- **falha:** o comportamento observado contraria o cenário.

## Limite da evidência

Os testes usam `SnsClient` simulado com Mockito, `Metrics` simulado, `RastreadorFalso` e eventos montados com setters. Isso significa:

- **O que está provado:** dado o registro do Stream, o handler monta o `PublishRequest` esperado ou não publica, e devolve o `batchItemFailures` esperado.
- **O que não está provado:** a publicação real no SNS, a chegada do e-mail, o motor de filtro do Lambda, a entrega repetida do evento, o destino de falha no S3, as permissões, o SnapStart, os logs no CloudWatch e a leitura do JSON real do evento pelo runtime.

Na coluna Limite da matriz:
- **SIM:** só cliente simulado. A tarefa indicada repete o caminho na stack.
- **SIM, sem réplica:** só cliente simulado, e nenhuma tarefa da seção 5 provoca esse caso.
- **LOG:** captura em memória com o layout real do `log4j2.xml` e nível `ALL`, com cliente simulado. O SDK real não roda nesses testes.
- **CONFIG:** leitura do template, com a sonda abaixo quando indicada. Nada foi exercitado.
- **PENDENTE:** só a tarefa indicada pode dar a evidência.

A **sonda** é um programa local, fora do repositório e não versionado, executado com o Python do SAM CLI e com a rede bloqueada no próprio programa. Ela aplica o transform do SAM ao `template.yaml` e lê o padrão de filtro do recurso gerado. O avaliador do padrão foi escrito à mão, seguindo a semântica da documentação citada na D1. **Ele não é o motor do Lambda.**

Legenda das classes de teste:
- `H`: `br.com.fiap.feedback.notificacao.NotificarCriticaHandlerTest`, com a classe aninhada no nome
- `L`: `br.com.fiap.feedback.notificacao.LogsNotificarCriticaTest`
- `A`: `br.com.fiap.feedback.dominio.AvisoCriticoTest`

## Verificações locais

| Verificação | Resultado |
|---|---|
| `mvn -B verify` | `BUILD SUCCESS`. `dominio`: `Tests run: 97, Failures: 0, Errors: 0, Skipped: 0`. `registrar-avaliacao`: `Tests run: 127, Failures: 0, Errors: 0, Skipped: 0`. `notificar-critica`: `Tests run: 65, Failures: 0, Errors: 0, Skipped: 0` |
| `sam validate --lint --region sa-east-1` | `template.yaml is a valid SAM Template` |
| `openspec validate add-notificacao-critica --strict` | `Change 'add-notificacao-critica' is valid` |
| Template contra o `design.md` | Bloco `LogNotificarCritica` e `NotificarCritica` igual ao do design, descontado um comentário |
| `log4j2.xml` | Igual ao de `registrar-avaliacao` |
| `dependency:tree` de `notificar-critica`, escopo `runtime` | `sns`, `url-connection-client` e `aspectjrt` presentes. 0 ocorrências de `dynamodb`, `s3`, `idempotency`, `apache-client`, `apache5-client` e `netty-nio-client` |

O build local usa Microsoft OpenJDK 25 (25.0.5.0.2). A comprovação em Corretto 25 vem do CI, na tarefa 5.1.

Durante a validação, `README.md` apareceu alterado no worktree, com 74 linhas novas, por outro agente ou pessoa. É o único arquivo fora de `target` mais novo que o `mvn -B verify`, e este relatório não o avaliou.

## Matriz: notificacao-critica

### Aviso imediato de avaliação crítica

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Aviso publicado no tópico do ambiente | ok (executado) | `H.AvisoImediato.publicaNoTopicoConfigurado`: uma única publicação, com o ARN exato. Template: `TOPICO_ADMINS_ARN: !Ref TopicoAdmins` e `sns:Publish` só nesse tópico | SIM, tarefa 5.6. A leitura da variável em `Recursos.TOPICO` não roda nos testes |
| Publicação na execução acionada pelo evento | ok (executado) | `H.AvisoImediato.publicaNoTopicoConfigurado`: lote de um registro, publicação conferida depois do retorno. Busca: 0 ocorrências de `Thread`, `Executor`, `CompletableFuture`, `Async` e `Schedul` na função. Template: sem evento agendado e sem `MaximumBatchingWindowInSeconds` | SIM, tarefa 5.6 |
| Lote com várias avaliações críticas | ok (executado) | `H.AvisoImediato.loteComTresCriticas`: três publicações, cada uma com a sua descrição, em ordem | SIM, sem réplica. A 5.6 não prevê lote |
| E-mail recebido pelo administrador | **não coberto** | Nenhum teste. Existe só o indício: `AssinaturaEmailAdmin` no template e o corpo conferido nos cenários de conteúdo | PENDENTE, tarefas 5.3 e 5.6 |

### Conteúdo obrigatório do aviso

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Campos obrigatórios presentes | ok (executado) | `H.Conteudo.camposObrigatorios`, `A.camposObrigatorios`, `A.formatoCompleto`, com o corpo exato | SIM, tarefa 5.6 |
| Descrição no tamanho máximo | ok (executado) | `H.Conteudo.descricaoNoTamanhoMaximo`, `A.descricaoNoTamanhoMaximo`: confere 1000 caracteres com acentos, `endsWith` da descrição inteira e ausência de `...` e `…`. `A.descricaoSemAlteracao` confere espaços e quebras de linha | SIM. A prova é o `PublishRequest`, não o e-mail. A 5.6 não pede 1000 caracteres, ver a observação E |
| Data de envio vem da avaliação | ok (executado) | `H.Conteudo.dataEnvioPersistida`, `A.dataDeEnvioPersistida`: item de 2020-02-29, sem a data de hoje nem a de `criadoEm` | SIM, tarefa 5.6 |

### Acionamento restrito a avaliações críticas

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Filtro declarado no template | ok (por configuração) | `template.yaml`, linha 236. A sonda confirma o mesmo padrão em `FilterCriteria` do mapeamento gerado | CONFIG. Nenhum teste lê o template, ver a observação B. O mapeamento na conta é da 5.5 |
| Filtro aceita avaliação crítica | ok (por configuração) | Sonda: `INSERT` com `CRITICA` casa com o padrão lido do recurso gerado | CONFIG. Avaliador escrito à mão. Motor real na 5.6 |
| Filtro rejeita avaliação média | ok (por configuração) | Sonda: `INSERT` com `MEDIA` não casa | CONFIG. Avaliador escrito à mão. Motor real na 5.6 |
| Filtro rejeita avaliação baixa | ok (por configuração) | Sonda: `INSERT` com `BAIXA` não casa | CONFIG. Avaliador escrito à mão. Motor real na 5.6 |
| Sem invocação no ambiente implantado | **não coberto** | Nenhuma evidência possível sem a stack. A segunda checagem do handler (`H.Urgencia.outraUrgencia`) impede o aviso, mas não prova a ausência de invocação | PENDENTE, tarefa 5.6 |

### Aviso somente na criação da avaliação

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Criação gera aviso | ok (executado) | `H.AvisoImediato.publicaNoTopicoConfigurado`, `H.SomenteCriacao.loteMisto` | SIM, tarefa 5.6 |
| Alteração de item crítico não gera aviso | ok (executado) | `H.SomenteCriacao.alteracaoDeItemCritico`: `MODIFY` sem publicação e sem falha. A sonda confirma que o padrão também não casa | SIM, tarefa 5.5 |
| Alteração que torna o item crítico não gera aviso | ok (executado) | `H.SomenteCriacao.alteracaoQueTornaCritico`. A entrada é igual à do cenário anterior, e isso está correto: com `StreamViewType: NEW_IMAGE` (linha 71) não existe imagem antiga, e os dois casos chegam iguais à função | SIM, sem réplica específica. A 5.5 altera um item já crítico |
| Remoção não gera aviso | ok (executado) | `H.SomenteCriacao.remocao`, sem imagem nova, como no Stream real, e `H.SomenteCriacao.remocaoComImagem`. A sonda confirma que o padrão não casa | SIM, sem réplica. A seção 5 não remove item |

### Falha na publicação não é sucesso silencioso

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Publicação rejeitada | ok (executado) | `H.FalhaNaPublicacao.publicacaoRejeitada`, com 4 tipos de exceção, confere a sequência em `batchItemFailures`. `L.falhaDoServico` e `L.falhaDoCliente` conferem o nível ERROR | SIM, sem réplica. A exceção é lançada pelo mock |
| Evento reprocessado | ok (por configuração) | A parte do handler foi executada: `H.FalhaNaPublicacao.eventoReprocessado` devolve a falha e publica na segunda chamada. **O próprio teste chama o handler duas vezes**, então a nova entrega não é provada por ele. Ela depende do template: `ReportBatchItemFailures`, `MaximumRetryAttempts: 5`, `MaximumRecordAgeInSeconds: 3600` | CONFIG, sem réplica, ver a observação C |
| Falha em parte de um lote | ok (por configuração) | A primeira cláusula foi executada: `H.FalhaNaPublicacao.falhaNoSegundoRegistro` devolve só a sequência do segundo e não publica o terceiro. O reprocessamento depende da mesma configuração do cenário anterior | CONFIG, sem réplica, ver a observação C |

### Avaliação registrada não depende do aviso

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Item preservado após a falha | ok (por configuração) | A função não tem como escrever: `H.RegistroIndependente.semClienteDaTabela` confere que `DynamoDbClient` não está no classpath, a árvore `runtime` não tem `dynamodb`, e a role gerada na sonda tem 0 ações inline de `dynamodb` | CONFIG, sem réplica. Nenhum teste observa um item real depois da falha. O boundary permite escrita na tabela, então a restrição vem da role, não dele |
| Registro com tópico indisponível | ok (por configuração) | `registrar-avaliacao` não cita SNS em `pom.xml` nem em `src/main` (0 arquivos), e a política inline dele só tem `dynamodb:PutItem`. O aviso sai depois, pelo Stream | CONFIG, sem réplica. A seção 5 não torna o tópico indisponível |

### Entrega pelo menos uma vez, sem deduplicação

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Mesmo evento entregue duas vezes | ok (executado) | `H.SemDeduplicacao.eventoRepetido`: duas publicações e nenhuma falha | SIM, sem réplica |
| Sem controle de deduplicação | ok (por configuração) | Inspeção da tarefa 4.2, que é o que o cenário pede. `eventoRepetido` confirma o comportamento no handler | CONFIG. Nenhum teste lê o template, ver a observação B |

### Descrição fora dos logs em nível INFO

| Cenário | Status | Teste ou evidência | Limite |
|---|---|---|---|
| Processamento com sucesso | ok (executado) | `L.sucesso`, que confere o marcador em todo o texto capturado e uma única linha, e `L.registroIgnorado`. `L.capturaFunciona` prova que a captura enxerga todos os níveis e o texto de exceções | LOG, tarefa 5.7 |
| Processamento com falha | ok (executado) | `L.falhaDoServico`, `L.falhaDoServicoSemDetalhes` e `L.falhaDoCliente`: marcador na descrição **e** na mensagem da exceção, ausente de todas as linhas, e sem campo `error` | LOG, sem réplica. A 5.7 só vê o caminho de sucesso, ver a observação C |

## Cenários que dependem da stack

| Situação | Cenários |
|---|---|
| **Pendentes da seção 5** (2) | `E-mail recebido pelo administrador` e `Sem invocação no ambiente implantado`, na tarefa 5.6 |
| Já têm evidência local, e a seção 5 repete (10) | `Aviso publicado no tópico do ambiente`, `Publicação na execução acionada pelo evento`, `Campos obrigatórios presentes`, `Data de envio vem da avaliação`, os três de avaliação do filtro e `Criação gera aviso` na 5.6; `Alteração de item crítico não gera aviso` na 5.5; `Processamento com sucesso` na 5.7 |
| Conferência do recurso na conta (1) | `Filtro declarado no template`, na 5.5 |
| Sem réplica prevista (12) | Todos os demais. Em especial `Evento reprocessado` e `Falha em parte de um lote`, que dependem do comportamento do Lambda e só têm evidência de configuração |

## Tarefa 4.2: inspeção

| Item | Resultado | Evidência |
|---|---|---|
| Armazenamento ou consulta de avisos | Não há | Busca por `S3Client`, `PutObject`, `idempot`, `dedup`, `ConcurrentHashMap`, `Set<` e `Cache` no código da função, no `pom.xml` e em `AvisoCritico`: a única ocorrência é `networkaddress.cache.ttl`, que é o cache de DNS. Os campos estáticos e de instância do handler não guardam estado de aviso. A árvore `runtime` não tem `s3` nem `idempotency` |
| Escrita na tabela | Não há | Sem `DynamoDbClient` no classpath, sem as operações de escrita no código e sem `dynamodb` nas ações inline da role gerada. A gerenciada `AWSLambdaDynamoDBExecutionRole` é só de leitura do Stream, segundo a D8. O conteúdo dela não foi lido na conta |
| Deduplicação no tópico | Não há | `TopicoAdmins` tem só `TopicName` e `KmsMasterKeyId`, confirmado no recurso gerado. 0 ocorrências de `fifo`, `ContentBasedDeduplication` e `dedup` no template. `PublishRequest` só preenche `topicArn`, `subject` e `message` |

O destino de falha `aws/lambda/` no bucket guarda o lote que esgotou as tentativas, com a descrição (D4). Quem grava é o serviço Lambda. O código da função não lê esse prefixo, então ele não funciona como controle de duplicata.

## Defeitos

Nenhum defeito encontrado no código de produção. Nenhum código de produção ou teste foi alterado por este relatório.

## Observações

Não reprovam cenário.

- **A. O filtro por `eventName` pode falhar no sentido contrário ao que o design prevê. Devolver ao arquiteto.** A D1 e os Risks só tratam o caso em que o filtro deixa passar alterações, e concluem que "nenhum requisito é violado". O guia citado na D1 diz que o mapeamento só filtra pela chave `dynamodb`. Se isso valer de forma que o padrão nunca case, nenhuma avaliação crítica invoca a função, e a D2 não ajuda, porque o handler nem roda. Se o padrão for recusado na criação do mapeamento, o deploy falha, e a 5.9 só cobre a recusa do S3. Os indícios citados na D1 apontam para o filtro funcionar, e a 5.6 detecta o problema, mas não há plano B como o da D4.
- **B. Nenhum teste protege o filtro nem o tópico.** 0 testes leem `template.yaml`. Com o filtro sem a urgência, ou com o tópico FIFO com deduplicação, `mvn test` continuou passando com todos os testes. A avaliação do filtro está em uma sonda não versionada. Esta tarefa limitou a edição ao `qa.md`, então nenhum teste foi acrescentado. Um teste que leia o padrão do template fecharia a regressão, a critério do orquestrador.
- **C. O caminho de falha não tem execução real prevista.** Nenhuma tarefa da seção 5 provoca falha de publicação. Isso deixa sem execução real a nova entrega do evento, o log em ERROR no CloudWatch, a ausência do marcador nesse caminho e a gravação no destino de S3. O deploy só prova que o mapeamento aceita o ARN do bucket. Se o relatório precisar dessa prova, falta uma tarefa, a critério do orquestrador e do Gabriel, porque ela exige uma alteração temporária na stack.
- **D. Ligação de produção fora do `mvn verify`.** Os testes não executam `new NotificarCriticaHandler()`, `Rastreador.XRAY`, o `Metrics` real nem o SDK real, e nunca leem o evento a partir de JSON. A 5.6 é a primeira execução real desse caminho.
- **E. Assunto com acentos.** `A.assuntoDentroDoLimite` confere tamanho e ausência de quebra de linha, e a D6 cita a API como aceitando UTF-8, mas só a stack mostra o SNS aceitando `Avaliação crítica registrada`. Se recusar, toda publicação falha, e os avisos vão para o destino de falha depois das tentativas. A 5.6 revela isso. Vale usar nela uma descrição de 1000 caracteres com acentos, para ver no e-mail o que hoje só está provado no `PublishRequest`.
- **F. Prazo da D3 vale por chamada, não por lote.** A D3 diz que uma tentativa não passa do `Timeout` de 30 segundos. Isso vale para uma chamada que falha, porque o laço para na hora. Dez publicações lentas com sucesso, abaixo de 5 segundos cada, podem somar até perto de 50 segundos. O timeout cairia em `BisectBatchOnFunctionError`, com duplicatas, o que a spec aceita. Caso improvável, severidade baixa.
- **G. Trace do X-Ray.** A lista do instrumentor no jar `aws-xray-recorder-sdk-aws-sdk-v2` 2.21.1 captura só `TopicArn` em `Publish`, então o corpo da mensagem não vai como parâmetro do trace. A mensagem de exceção do SDK continua indo para o trace, como na observação A de `add-registro-avaliacao`, já aceita lá como limite.

## Permissões, por leitura

Conferido só no texto de `infra/bootstrap/github-oidc.yaml` e em referências públicas da AWS. Nada foi consultado na conta. O primeiro deploy é a prova.

| Item | Resultado |
|---|---|
| Ações do CloudFormation para criar o mapeamento | O esquema público de `AWS::Lambda::EventSourceMapping` pede `lambda:CreateEventSourceMapping`, `lambda:GetEventSourceMapping` e `lambda:TagResource`, além de ações de KMS. A role de deploy libera as três de Lambda. O template não declara chave KMS no mapeamento |
| Condição `lambda:FunctionArn` | A referência de serviço do IAM a lista em criar, ler, alterar e excluir mapeamento. O ARN do alias casa com `feedback-*-<amb>:*` |
| Políticas gerenciadas | As três geradas na sonda são as três aceitas em `AnexarPoliticasDoSam` |
| Boundary | Cobre `sns:Publish`, KMS via SNS, S3 no bucket e Stream, com os nomes usados no template |
| Mapeamento gerado | `FunctionName` aponta para `NotificarCriticaAliaslive`. Política de reprocessamento e destino iguais à D3 e à D4 |

## Prova das asserções

Em uma cópia temporária, fora do repositório, 20 mutações foram aplicadas, uma de cada vez, com `mvn test` e sem `install`. A cópia sem mutação passou nos 162 testes de `dominio` e `notificar-critica`. 18 mutações foram detectadas. As 2 que sobreviveram são as da observação B.

| Mutação | Detectada por |
|---|---|
| Publica em outro tópico | `H.AvisoImediato.publicaNoTopicoConfigurado` |
| Só publica com 2 ou mais registros no lote | `H.AvisoImediato.publicaNoTopicoConfigurado` e outros 20 testes |
| Publica só o primeiro do lote | `H.AvisoImediato.loteComTresCriticas` e outros 3 |
| Sem checagem de `eventName` | `H.SomenteCriacao.alteracaoDeItemCritico`, `alteracaoQueTornaCritico`, `remocaoComImagem` e `loteMisto`, e outros 2 |
| Sem checagem de urgência | `H.Urgencia.outraUrgencia`, `H.ItemInvalido.ignoradoNaoContaComoInvalido` |
| Descrição cortada em 999 caracteres | `H.Conteudo.descricaoNoTamanhoMaximo`, `A.descricaoNoTamanhoMaximo`, `A.descricaoComEmoji` |
| Reticências no fim | `H.Conteudo.descricaoNoTamanhoMaximo`, `A.descricaoNoTamanhoMaximo` e outros 7 |
| Data do processamento no lugar da persistida | `H.Conteudo.dataEnvioPersistida`, `A.dataDeEnvioPersistida` e outros 3 |
| Falha reportada como sucesso | `H.FalhaNaPublicacao.publicacaoRejeitada`, `eventoReprocessado`, `falhaNoPrimeiroRegistro` e `falhaNoSegundoRegistro` |
| Sequência do primeiro registro em vez da que falhou | `H.FalhaNaPublicacao.falhaNoSegundoRegistro` |
| Continua publicando depois da falha | `H.FalhaNaPublicacao.falhaNoPrimeiroRegistro`, `falhaNoSegundoRegistro` |
| Deduplicação por `eventID` | `H.SemDeduplicacao.eventoRepetido`, `H.FalhaNaPublicacao.eventoReprocessado` |
| Descrição em INFO no sucesso | `L.sucesso` |
| Evento recebido em INFO | `L.sucesso`, `L.falhaDoServico` e outros 6 |
| Exceção passada ao logger | `L.falhaDoServico`, `L.falhaDoCliente`, `L.falhaDoServicoSemDetalhes` |
| Falha em WARN em vez de ERROR | `L.falhaDoServico`, `L.falhaDoCliente`, `L.falhaDoServicoSemDetalhes` |
| Cliente DynamoDB no classpath | `H.RegistroIndependente.semClienteDaTabela` |
| `aspectjrt` removido do `pom.xml` (tarefa 2.6) | `PrimingSnapStartTest`: os 3 testes falham, com `NoClassDefFoundError: org/aspectj/lang/NoAspectBoundException` |
| Filtro do template sem a urgência | **Não detectada** |
| Tópico FIFO com deduplicação no template | **Não detectada** |
