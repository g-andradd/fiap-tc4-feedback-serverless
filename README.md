# Plataforma de feedback serverless

Tech Challenge da Fase 4 da pós-graduação em Arquitetura e Desenvolvimento Java (FIAP). Estudantes avaliam aulas com uma descrição e uma nota. Administradores recebem aviso imediato quando a avaliação é crítica e um relatório semanal com a consolidação dos feedbacks.

Estado atual: a função `registrar-avaliacao` está implantada e conferida em dev desde 05/11/2026. O relatório está em `openspec/changes/add-registro-avaliacao/qa.md`. O estado das demais funções está na seção de cada uma. Alarmes, dashboard, aquecimento dos clientes e memória de 1024 MB foram implantados e conferidos em dev em 06/11/2026; veja [Monitoramento e alarmes](#monitoramento-e-alarmes).

## Stack

- Java 25 e Maven multi-módulo
- AWS Lambda com SnapStart, API Gateway, DynamoDB, SNS, S3 e EventBridge Scheduler
- CloudWatch (logs, métricas, alarmes e dashboard) e X-Ray, com Powertools for AWS Lambda (Java)
- AWS SAM para infraestrutura como código
- GitHub Actions com autenticação OIDC na AWS
- JUnit 5, AssertJ e Mockito nos testes

## Estrutura de pastas

```
.
├── .github/workflows/            # pipelines de CI e deploy
├── docs/postman/                 # coleção Postman de POST /avaliacao
├── functions/
│   ├── dominio/                  # regras puras, sem SDK da AWS
│   ├── registrar-avaliacao/      # POST /avaliacao
│   ├── notificar-critica/        # aviso de avaliação crítica
│   └── gerar-relatorio-semanal/  # consolidação da semana
├── infra/bootstrap/              # recursos aplicados uma vez na conta AWS
├── openspec/                     # specs e changes
├── template.yaml                 # template SAM
├── samconfig.toml                # configuração por ambiente
└── pom.xml                       # parent Maven
```

## Como buildar

Requisitos: JDK 25 no `JAVA_HOME` e Maven 3.9 ou superior.

```bash
mvn -B verify
```

Para empacotar as funções com o SAM, também é preciso o SAM CLI (testado com a versão 1.171.0), e o `install` vem antes:

```bash
mvn -B install
sam build
```

O `sam build` copia só o `CodeUri` da função para uma pasta temporária e resolve o parent e o módulo `dominio` pelo `~/.m2`. Sem o `install`, ele falha com `JavaMavenWorkflow:MavenBuild - Maven Failed`. Os testes do módulo rodam de novo a cada `sam build`. No Git Bash do Windows, o comando pode precisar ser `sam.cmd build`.

### Integração contínua nos pull requests

O workflow `.github/workflows/ci.yml` roda em todo PR para `develop`, `release/**` e `main`. O job se chama `ci`, que é o check obrigatório da proteção de branch.

| Passo | Comando | O que pega |
|---|---|---|
| Build, testes e install no repositório Maven local | `mvn -B install` | Erro de compilação e teste quebrado |
| Empacotar as funções | `sam build` | Função declarada no template sem código empacotável, ou dependência que não chega ao pacote |
| Validar o template da aplicação | `sam validate --lint --region sa-east-1` | Erro de sintaxe e de regra do template |
| Validar o template de bootstrap | `sam validate --lint --region sa-east-1 -t infra/bootstrap/github-oidc.yaml` | O mesmo, no bootstrap |

Até este change, o CI rodava `mvn -B verify` e as duas validações. Um template que apontasse para uma função sem código passava no PR e só falhava no `sam build` do deploy, depois do merge. Agora o PR repete o par `mvn -B install` e `sam build` do `deploy.yml` e falha antes. O `install` substitui o `verify` porque o `sam build` precisa do módulo `dominio` no `~/.m2`, como explicado acima, e ele já passa pelos testes.

O `sam build` não chama a AWS, então o CI continua sem credencial e sem OIDC. O custo é tempo: o design estima de 45 a 60 segundos a mais por PR, porque o `sam build` roda o Maven de novo em cada função. O tempo real será conferido no primeiro PR com o workflow novo.

## Endpoint `POST /avaliacao`

Registra uma avaliação de aula. O endpoint não tem autenticação, que está fora do escopo do projeto.

Este endpoint foi conferido na stack de dev em 05/11/2026. As respostas abaixo vêm de `Respostas.java` e conferem com os testes do handler, que rodam em `mvn -B verify` com o DynamoDB simulado. A falha ao persistir (500) só tem prova nesses testes, porque não foi provocada na stack.

### Corpo da requisição

| Campo | Obrigatório | Regra |
|---|---|---|
| `descricao` | sim | Texto com ao menos um caractere diferente de espaço, tabulação ou quebra de linha, e até 1000 caracteres Unicode. Um emoji conta como um. É gravada como recebida, sem remover espaços |
| `nota` | sim | Número JSON inteiro de 0 a 10. `7.0`, `1e1` e `"7"` são rejeitados |

Campos extras são ignorados. O cliente não define `id`, `urgencia`, `dataEnvio` nem `criadoEm`: quem preenche é a função. Cada requisição válida gera uma avaliação nova, mesmo com corpo idêntico.

### Urgência

Calculada a partir da nota no registro, devolvida na resposta e gravada no item.

| Nota | Urgência |
|---|---|
| 0 a 3 | `CRITICA` |
| 4 a 6 | `MEDIA` |
| 7 a 10 | `BAIXA` |

### Respostas

Todas com `content-type: application/json`.

| Caso | Status | `erro` |
|---|---|---|
| Avaliação registrada | 201 | |
| Campo inválido | 400 | `ENTRADA_INVALIDA` |
| Corpo inválido | 400 | `CORPO_INVALIDO` |
| Falha interna | 500 | `ERRO_INTERNO` |

**201, avaliação registrada.** O `id` é um UUID gerado pela função.

Requisição:

```json
{"descricao": "Aula muito boa", "nota": 9}
```

Resposta:

```json
{"id":"<UUID>","urgencia":"BAIXA"}
```

**400, campo inválido.** `campos` lista o nome de todos os campos reprovados, nunca os valores. Nada é gravado.

Requisição:

```json
{"descricao": "", "nota": 11}
```

Resposta:

```json
{"erro":"ENTRADA_INVALIDA","mensagem":"Um ou mais campos são inválidos.","campos":["descricao","nota"]}
```

**400, corpo inválido.** Vale para corpo ausente ou vazio, JSON malformado, JSON que não é objeto, como `[1, 2, 3]`, e objeto seguido de outros tokens. Também cai aqui o corpo que ultrapassa os limites do parser JSON, como número com mais de 1000 dígitos, mesmo em campo extra. `campos` vem vazio e nada é gravado.

Requisição, com o JSON cortado:

```
{"descricao": "Aula boa", "nota": 5
```

Resposta:

```json
{"erro":"CORPO_INVALIDO","mensagem":"O corpo da requisição precisa ser um objeto JSON válido.","campos":[]}
```

**500, falha interna.** A requisição é válida, mas a gravação falhou. A mensagem é fixa, sem detalhe técnico.

Requisição:

```json
{"descricao": "Aula muito boa", "nota": 9}
```

Resposta:

```json
{"erro":"ERRO_INTERNO","mensagem":"Não foi possível registrar a avaliação. Tente novamente."}
```

O template também limita a rota a 10 requisições por segundo, com rajada de 20. O excesso recebe 429 do próprio API Gateway, sem invocar a função.

### Testar depois do deploy

A stack `feedback-dev` é publicada pelo pipeline `deploy.yml` a cada push em `develop`. Nos comandos com `aws`, acrescente `--profile tc4-admin` se a sua credencial padrão não for dessa conta.

**1. Obter a URL do endpoint**, no output `ApiAvaliacoesUrl` da stack:

```bash
aws cloudformation describe-stacks \
  --stack-name feedback-dev \
  --query "Stacks[0].Outputs[?OutputKey=='ApiAvaliacoesUrl'].OutputValue" \
  --output text \
  --region sa-east-1
```

O valor tem o formato `https://<ID_DA_API>.execute-api.sa-east-1.amazonaws.com/avaliacao`, já com o caminho.

**2. Enviar uma avaliação**, usando essa URL como está:

```bash
curl -i -X POST "https://<ID_DA_API>.execute-api.sa-east-1.amazonaws.com/avaliacao" \
  -H "Content-Type: application/json" \
  -d '{"descricao": "Aula muito boa", "nota": 9}'
```

**3. Rodar a coleção Postman**, em `docs/postman/feedback-serverless.postman_collection.json`. Importe o arquivo e defina a variável de coleção `baseUrl` com o valor de `ApiAvaliacoesUrl` **sem o `/avaliacao` do final**, porque cada requisição já monta a URL como `{{baseUrl}}/avaliacao`. Com a URL completa, a chamada iria para `/avaliacao/avaliacao`, rota que não existe.

```
baseUrl = https://<ID_DA_API>.execute-api.sa-east-1.amazonaws.com
```

A coleção traz 33 requisições, cada uma com testes de status e corpo: 12 de sucesso e limites aceitos, 7 de rejeição de `descricao`, 9 de `nota`, 1 com dois campos inválidos e 4 de corpo malformado. O valor padrão de `baseUrl` é `https://exemplo.invalid`, que não responde. Até aqui ela só rodou localmente, com Newman, contra uma ponte HTTP para o handler com o DynamoDB simulado. O caso de 500 não está na coleção, porque uma requisição não consegue provocar a falha de gravação.

## Conferir o SnapStart no alias

Passo a seguir depois de cada deploy. Em dev, a conferência de 05/11/2026 trouxe `State` igual a `Active` e `OptimizationStatus` igual a `On`, ainda com 512 MB.

O SnapStart só vale em versões publicadas e aliases. Por isso o comando consulta `:live`: sem o sufixo, a leitura seria de `$LATEST`, que não usa SnapStart.

```bash
aws lambda get-function-configuration --function-name feedback-registrar-avaliacao-dev:live --region sa-east-1 --query "{Version:Version,State:State,OptimizationStatus:SnapStart.OptimizationStatus,MemorySize:MemorySize}"
```

Resultado esperado: `State` igual a `Active`, `OptimizationStatus` igual a `On` e `MemorySize` igual a 1024. `Version` traz o número da versão publicada para a qual o alias aponta.

```json
{
    "Version": "<NUMERO_DA_VERSAO>",
    "State": "Active",
    "OptimizationStatus": "On",
    "MemorySize": 1024
}
```

Para as outras funções, troque o nome por `feedback-notificar-critica-<ambiente>` ou `feedback-gerar-relatorio-semanal-<ambiente>`. Para `hml` e `prod`, troque o sufixo. Acrescente `--profile tc4-admin` se a sua credencial padrão não for dessa conta. Em Java, o snapshot é apagado depois de 14 dias sem invocação, e a chamada seguinte falha com `SnapStartNotReadyException` até a versão voltar a `Active`.

### Memória e aquecimento dos clientes

O `Globals` do template define `MemorySize: 1024` para as três funções, no lugar dos 512 MB anteriores, e nenhuma função sobrescreve o valor. A CPU do Lambda cresce com a memória, e o que pesava era CPU: a função usa cerca de 215 MB, mas a primeira chamada ao SDK depois da restauração levou 4.259 ms no DynamoDB do registro e 4.301 ms no SNS da notificação, perto dos limites de 8 s e 5 s dos clientes.

Além da memória, cada função registra um hook do SnapStart (`Aquecimento`, com `org.crac`) que faz uma chamada de leitura com cada cliente do SDK antes do snapshot. Assim o snapshot já guarda o cliente inicializado:

| Função | Chamada no aquecimento | Permissão nova na role |
|---|---|---|
| `registrar-avaliacao` | `DescribeTable` na tabela de avaliações | `dynamodb:DescribeTable` na tabela (`AquecerCliente`) |
| `notificar-critica` | `GetTopicAttributes` no tópico de administradores | `sns:GetTopicAttributes` no tópico (`AquecerCliente`) |
| `gerar-relatorio-semanal` | `Query` no índice `por-data` numa partição que não existe, com `Limit` 1 | nenhuma, a role já tinha `dynamodb:Query` no índice |
| `gerar-relatorio-semanal` | `HeadBucket` no bucket de relatórios | `s3:ListBucket` no bucket, com `s3:ResourceAccount` (`AquecerClienteS3`) |
| `gerar-relatorio-semanal` | `GetTopicAttributes` no tópico de administradores | `sns:GetTopicAttributes` no tópico (`AquecerClienteSns`) |

- Nenhuma chamada grava item, publica mensagem ou cria objeto, e nenhuma devolve descrição ou nota. As permissões novas são só de leitura e apontam para recursos com o sufixo do ambiente.
- Cada cliente aquecido grava uma linha INFO `Cliente aquecido`, com serviço, operação e duração, no grupo de log da função, no momento em que a versão é publicada. Em dev, o aquecimento somou de 1,6 a 1,8 s à publicação de cada versão, tempo que saiu da primeira invocação e não entra no tempo de resposta.
- Se uma chamada falhar, a função grava WARN `Falha no aquecimento`, só com serviço, classe e código do erro, e segue. A publicação da versão não é interrompida.
- O SNS só aceita o `GetTopicAttributes` porque o boundary das funções passou a liberar essa ação (veja [Atualizar o bootstrap depois](#atualizar-o-bootstrap-depois)). Sem isso, o deploy passaria do mesmo jeito, e o aquecimento do SNS ficaria só no WARN.

Para conferir o aquecimento depois do deploy, procure as linhas no grupo de log de cada função. Com o relatório, devem aparecer três linhas por versão publicada:

```bash
aws logs filter-log-events \
  --log-group-name /aws/lambda/feedback-registrar-avaliacao-dev \
  --filter-pattern '"Cliente aquecido"' \
  --start-time $(( ($(date +%s) - 3600) * 1000 )) \
  --query "events[].message" \
  --region sa-east-1
```

Medição em dev, em 06/11/2026, com três restaurações por função: a primeira chamada a cada serviço depois da restauração caiu de cerca de 4,3 s para 105 a 339 ms, abaixo da meta de metade do tempo máximo de cada cliente (4 s no registro, 2,5 s na notificação e 5 s no relatório). A duração da restauração ficou na mesma faixa de antes. Como memória e aquecimento entraram no mesmo deploy, o resultado é o efeito somado dos dois. Os números completos estão em `openspec/changes/add-observabilidade-seguranca/qa.md`, item 6.15.

## Reprocessamento do aviso crítico e limpeza do bucket

Esta seção descreve o que está declarado no `template.yaml` e no código de `notificar-critica`. A conferência na AWS ainda não foi feita. O primeiro deploy em dev só confirmou o que diz o parágrafo **Permissão de S3 na role**, mais abaixo. O que é atribuído ao Lambda e ao CloudFormation vem da documentação da AWS citada em `openspec/changes/add-notificacao-critica/design.md`.

### Política de reprocessamento

Configuração do gatilho `AvaliacaoCriada`, no Stream da tabela de avaliações:

| Parâmetro | Valor | Efeito esperado |
|---|---|---|
| `FunctionResponseTypes` | `ReportBatchItemFailures` | A função informa em qual registro o lote falhou |
| `BatchSize` | 10 | Até 10 registros por invocação |
| `BisectBatchOnFunctionError` | `true` | Em erro não tratado, como timeout, o Lambda divide o lote. Dividir não consome tentativas |
| `MaximumRetryAttempts` | 5 | Limite de novas tentativas. O padrão da AWS é infinito |
| `MaximumRecordAgeInSeconds` | 3600 | Registro com mais de 1 hora não é mais tentado. O padrão da AWS é ir até o registro expirar no Stream, em 24 horas |
| `StartingPosition` | `TRIM_HORIZON` | O mapeamento começa pelo registro mais antigo disponível no Stream |
| `DestinationConfig.OnFailure` | Bucket de relatórios | Recebe o que foi descartado, como explicado adiante |

Na primeira falha ao publicar no SNS, a função registra o erro em nível ERROR, para de percorrer o lote e devolve o número de sequência daquele registro em `batchItemFailures`. O Lambda retoma a partir dele. Cada chamada de publicação tem limite de 5 segundos, dentro do `Timeout` de 30 segundos da função.

A falha não altera a avaliação: o item continua na tabela, e a função não tem permissão de escrita nela. A entrega é pelo menos uma vez, sem deduplicação, então o mesmo aviso pode sair mais de uma vez.

A documentação consultada não informa o intervalo entre tentativas em stream. Por isso, não há como garantir quanto tempo de indisponibilidade do SNS a política cobre. O limite conhecido é a menor das condições: 5 tentativas ou 1 hora.

### Lotes descartados

Quando as tentativas ou a idade máxima se esgotam, o Lambda descarta o registro, grava no destino de falha e segue com o shard. O aviso daquela avaliação não sai sozinho.

O destino declarado é o bucket de relatórios, cujo nome está no output `BucketRelatoriosNome` da stack. O Lambda grava os registros descartados no prefixo `aws/lambda/`, que é definido pelo serviço, e a documentação da AWS indica este caminho:

```
aws/lambda/<ESM-UUID>/<shardID>/AAAA/MM/DD/...
```

Segundo a mesma documentação, o objeto traz o evento em `payload`, o que inclui a descrição escrita pelo aluno. O template não define regra de expiração, então o objeto fica até alguém apagá-lo. Como o Stream retém os registros por 24 horas, esse objeto é a fonte para refazer um aviso perdido. Isso é manual: não há comando nem função para reenviar.

Não vão para esse prefixo:

- Registros que o filtro do gatilho não aceita.
- Registros que a função ignora por não serem criação de item crítico.
- Item crítico sem campo obrigatório. A função descarta na hora, registra em ERROR o `eventID` e o nome dos campos, soma a métrica `ItemInvalido` e conta o registro como processado.

**Primeiro deploy.** Com `TRIM_HORIZON`, avaliações críticas gravadas até 1 hora antes da criação do mapeamento podem gerar aviso. As mais antigas ainda retidas no Stream podem ir direto para o destino de falha.

**Permissão de S3 na role.** O deploy em dev confirmou que o Lambda aceita S3 como destino de falha para o Stream do DynamoDB. Ao criar o mapeamento, porém, ele exige `s3:PutObject` no bucket inteiro e recusa uma permissão limitada a `aws/lambda/*`. Por isso a role tem `s3:PutObject` em `<bucket>/*`, sempre com a condição `s3:ResourceAccount` apontando para a própria conta. Os objetos continuam caindo em `aws/lambda/`, porque quem escolhe o caminho é o Lambda, não a role.

### Esvaziar o bucket antes de `sam delete`

O CloudFormation só exclui bucket vazio, então um objeto no bucket impede a remoção dele junto com a stack. Rode os comandos abaixo com o perfil administrativo, porque as roles de deploy não têm permissão para excluir a stack. Eles ainda não foram executados.

Para `hml` e `prod`, troque o valor de `STACK`:

```bash
STACK=feedback-dev

BUCKET=$(aws cloudformation describe-stacks \
  --stack-name "$STACK" \
  --query "Stacks[0].Outputs[?OutputKey=='BucketRelatoriosNome'].OutputValue" \
  --output text \
  --region sa-east-1 \
  --profile tc4-admin)

aws s3 ls "s3://$BUCKET/" --recursive --region sa-east-1 --profile tc4-admin

aws s3 rm "s3://$BUCKET/" --recursive --region sa-east-1 --profile tc4-admin

sam delete --stack-name "$STACK" --region sa-east-1 --profile tc4-admin
```

- O `aws s3 ls` mostra o que vai ser apagado. Salve antes o que precisar guardar, porque o `aws s3 rm` remove todos os objetos do bucket, não só os de `aws/lambda/`. O template não ativa versionamento no bucket.
- O `sam delete` remove a stack inteira, inclusive a tabela de avaliações, que tem `DeletionPolicy: Delete`. Sem `--no-prompts`, ele pede confirmação. No Git Bash do Windows, o comando pode precisar ser `sam.cmd delete`.
- Se o bucket receber um objeto novo entre o `aws s3 rm` e o `sam delete`, a exclusão falha. Nesse caso, esvazie de novo e repita.
- Não apague o bucket por fora da stack. A documentação da AWS alerta que excluir o bucket sem remover o destino de falha permite que outra conta recrie o nome e receba os registros.

## Relatório semanal: acionamento manual e download

Esta seção descreve o que está declarado no `template.yaml` e no código de `gerar-relatorio-semanal` e `functions/dominio`. O QA em dev de 06/11/2026 conferiu na AWS o acionamento manual, a reexecução para a mesma data, a data inválida, o acesso anônimo ao bucket e o agendamento implantado, tudo conforme. O relatório está em `openspec/changes/add-relatorio-semanal/qa.md`. A primeira execução agendada, em 09/11/2026 às 08:00 em São Paulo, ainda não foi observada.

O template agenda a geração para toda segunda às 08:00 em America/Sao_Paulo. O comando abaixo gera o relatório em qualquer dia, sem depender do agendamento.

Nos comandos com `aws`, acrescente `--profile tc4-admin` se a sua credencial padrão não for dessa conta. Para `hml` e `prod`, troque o sufixo no nome da função e da stack.

### Acionar pelo alias `live`

```bash
aws lambda invoke \
  --function-name feedback-gerar-relatorio-semanal-dev:live \
  --region sa-east-1 \
  --cli-binary-format raw-in-base64-out \
  --cli-read-timeout 150 \
  --payload '{"dataReferencia":"2026-11-04"}' \
  resposta.json
```

- `:live` é obrigatório na prática: sem ele, a chamada vai para `$LATEST`, que não usa SnapStart, e nenhum erro aparece.
- `--cli-binary-format` permite enviar o payload em texto no AWS CLI v2.
- `--cli-read-timeout 150` fica acima do `Timeout` de 120 segundos da função, porque o padrão do CLI é 60.
- Para usar o dia atual como referência, troque o payload por `--payload '{}'`.

Em caso de sucesso, `resposta.json` traz os campos abaixo, em ordem não garantida. O nome do bucket não vem na resposta.

| Campo | Exemplo |
|---|---|
| `dataReferencia` | `2026-11-04` |
| `chave` | `relatorios/2026-11-04.txt` |
| `quantidadeAvaliacoes` | `3` |

Em caso de falha, a saída do comando traz `FunctionError`, e a mensagem vai para `resposta.json`. As mensagens são fixas:

| Caso | Mensagem |
|---|---|
| Data de referência rejeitada | `Data de referência inválida. Informe uma data existente no formato yyyy-MM-dd.` |
| Falha na leitura, na gravação ou na publicação | `Falha ao gerar o relatório semanal. Consulte os logs da função.` |

Na data rejeitada, o `resposta.json` traz também `errorType` e `stackTrace`, com nomes de classe, arquivo e linha. Não há dado do aluno nem da conta, mas no vídeo convém mostrar só a mensagem.

Os efeitos seguem a ordem ler, gravar e publicar, e cada passo só roda se o anterior der certo. Falha na leitura, inclusive de um único dia ou de um item inválido, não grava nem avisa. Falha na gravação não avisa. Falha na publicação deixa o relatório guardado, e reexecutar para a mesma data corrige.

### Data de referência

| Valor de `dataReferencia` | Resultado |
|---|---|
| Ausente ou `null` | Dia atual em America/Sao_Paulo |
| Texto `yyyy-MM-dd` com data que existe, no passado ou no futuro | Usa a data informada |
| Vazio, outro formato (`09/11/2026`, `+12026-01-01`), data inexistente (`2026-02-30`) ou valor que não é texto | Rejeita antes de qualquer leitura, sem gravar e sem avisar. Um relatório já guardado fica como está |

A janela são os 7 dias civis anteriores à referência, em America/Sao_Paulo, com o dia da referência fora:

| Referência | Janela |
|---|---|
| `2026-11-09` | `2026-11-02` a `2026-11-08` |
| `2026-11-04` | `2026-10-28` a `2026-11-03` |

Por isso, uma avaliação enviada hoje só entra se a referência for amanhã.

Cada data de referência tem um único objeto, `relatorios/<dataReferencia>.txt`. Reexecutar para a mesma data substitui o objeto e publica um novo aviso. Datas diferentes geram objetos separados.

### Formato do relatório

Texto puro em UTF-8, com `ContentType: text/plain; charset=utf-8`. Exemplo com notas 2, 6 e 9 e referência `2026-11-09`:

```
RELATÓRIO SEMANAL DE AVALIAÇÕES
Ambiente: dev
Data de referência: 2026-11-09
Janela: 2026-11-02 a 2026-11-08

Média das notas: 5,67

Quantidade por urgência
CRITICA: 1
MEDIA: 1
BAIXA: 1

Quantidade por dia
2026-11-02: 0
2026-11-03: 2
2026-11-04: 0
2026-11-05: 1
2026-11-06: 0
2026-11-07: 0
2026-11-08: 0

Avaliações (3)

Data de envio: 2026-11-03 | Urgência: CRITICA
Descrição: Aula confusa

Data de envio: 2026-11-03 | Urgência: MEDIA
Descrição: Aula regular

Data de envio: 2026-11-05 | Urgência: BAIXA
Descrição: Aula ótima
```

- A média tem duas casas, com vírgula, e o empate arredonda para cima: 5,125 vira `5,13`.
- A nota individual não aparece. A urgência é a gravada no item, sem recálculo.
- Os totais trazem sempre os 7 dias e as 3 urgências, mesmo com zero.
- Na semana vazia, a linha da média é `Média das notas: sem avaliações na janela`, a lista fica `Avaliações (0)` e os totais ficam zerados.

### Formato do aviso

O aviso sai pelo tópico de administradores a cada relatório guardado. Traz os mesmos números, sem descrições nem notas individuais. A mesma execução do exemplo acima gera:

```
Assunto: Relatório semanal 2026-11-09

Relatório semanal gerado.

Ambiente: dev
Data de referência: 2026-11-09
Janela: 2026-11-02 a 2026-11-08

Média das notas: 5,67

Quantidade por urgência
CRITICA: 1
MEDIA: 1
BAIXA: 1

Quantidade por dia
2026-11-02: 0
2026-11-03: 2
2026-11-04: 0
2026-11-05: 1
2026-11-06: 0
2026-11-07: 0
2026-11-08: 0

Bucket: feedback-relatorios-<ID_DA_CONTA>-dev
Chave: relatorios/2026-11-09.txt
Localização: s3://feedback-relatorios-<ID_DA_CONTA>-dev/relatorios/2026-11-09.txt

O bucket é privado. Acesse com uma credencial da conta.
```

- Na semana vazia, a linha da média é a mesma do relatório, e as urgências e os dias aparecem em zero.
- Com uma única avaliação na janela, a média é igual à nota dela.
- O e-mail só chega depois de confirmada a assinatura do tópico.
- O nome do bucket inclui o ID da conta, então não copie o e-mail para arquivo versionado.

### Baixar com `aws s3 cp`

A resposta da função não traz o bucket. O nome está no output `BucketRelatoriosNome` da stack e no aviso. Troque a chave do exemplo pelo valor de `chave`:

```bash
BUCKET=$(aws cloudformation describe-stacks \
  --stack-name feedback-dev \
  --query "Stacks[0].Outputs[?OutputKey=='BucketRelatoriosNome'].OutputValue" \
  --output text \
  --region sa-east-1)

aws s3 cp "s3://$BUCKET/relatorios/2026-11-04.txt" "$HOME/relatorio-2026-11-04.txt" --region sa-east-1
```

O bucket é privado, então a credencial usada precisa de `s3:GetObject`. A role da função não tem essa permissão. O arquivo traz as descrições escritas pelos alunos e o `.gitignore` não o cobre, então baixe fora do repositório.

### Remover a stack

Os objetos em `relatorios/` também impedem a exclusão do bucket. Siga o passo em [Esvaziar o bucket antes de `sam delete`](#esvaziar-o-bucket-antes-de-sam-delete), e baixe antes os relatórios que quiser guardar.

## Monitoramento e alarmes

Tudo o que esta seção descreve foi implantado e conferido em dev em 06/11/2026: tópico, assinatura, filtros, alarmes, dashboard e retenção. O relatório está em `openspec/changes/add-observabilidade-seguranca/qa.md`. O QA confirmou a publicação de cada alarme no tópico e a entrega à assinatura, e a chegada dos e-mails na caixa de entrada ainda depende da conferência do Gabriel. hml e prod ainda não foram implantados.

Cada stack cria os recursos abaixo, todos com o sufixo do ambiente:

| Recurso | Nome | Output da stack |
|---|---|---|
| Tópico de alarmes | `feedback-alarmes-<ambiente>` | `TopicoAlarmesArn` |
| Assinatura por e-mail | para o endereço do secret `EMAIL_ADMIN` | |
| Filtros de métrica | `feedback-falhas-<função>-<ambiente>`, um por grupo de log | |
| Alarmes | `feedback-falha-<função>-<ambiente>`, um por função | |
| Dashboard | `feedback-painel-<ambiente>` | `PainelNome` |

### Tópico de alarmes e confirmação da assinatura

Os alarmes publicam em um tópico próprio, separado do tópico de administradores. O CloudWatch não consegue publicar em tópico criptografado com a chave `alias/aws/sns`, que é a do tópico de administradores. Por isso o tópico de alarmes fica sem criptografia em repouso. A mensagem de alarme traz nome, estado, motivo, métrica e limiar, e nenhum dado do aluno, porque a métrica é só uma contagem de linhas de log.

A política do tópico tem duas regras:

- `NegaAcessoSemTls`: nega a qualquer principal as 11 ações válidas em política de tópico quando a requisição não usa TLS. A lista é explícita porque a referência do SNS não documenta curinga nesse tipo de política.
- `PublicacaoPelosAlarmesDoAmbiente`: libera `sns:Publish` só para o CloudWatch da própria conta, e só para alarmes com nome `feedback-*-<ambiente>`. Um alarme de hml não publica no tópico de dev.

A assinatura usa o mesmo e-mail do tópico de administradores, sem secret novo no GitHub. **Ela precisa ser confirmada em cada ambiente.** No primeiro deploy de cada stack, o SNS manda um e-mail de confirmação para o endereço de `EMAIL_ADMIN`. Até alguém clicar no link, os alarmes publicam e ninguém recebe. Para conferir, use o ARN do output `TopicoAlarmesArn`:

```bash
aws sns list-subscriptions-by-topic \
  --topic-arn "<ARN_DO_TOPICO_DE_ALARMES>" \
  --query "Subscriptions[].SubscriptionArn" \
  --region sa-east-1
```

Enquanto não houver confirmação, o valor é `PendingConfirmation`. Depois, vem o ARN da assinatura. Repita em `hml` e `prod` quando cada stack for implantada.

### O que cada alarme cobre

Cada função tem um filtro de métrica no próprio grupo de log, com o mesmo padrão nas três:

```
?FALHA_FUNCAO ?"Task timed out" ?"Status: timeout"
```

O filtro publica 1 na métrica `Falhas<Função>` a cada linha que casa. Os handlers gravam o campo `"alarme":"FALHA_FUNCAO"` nas linhas que representam falha da função. As outras duas expressões pegam o tempo esgotado, que encerra a invocação antes de qualquer `catch`.

| Função | Dispara o alarme | Não dispara |
|---|---|---|
| `registrar-avaliacao` | Falha ao gravar, respondida com 500 (`Falha ao registrar avaliação`) | Requisição rejeitada com 400 |
| `notificar-critica` | Falha ao publicar o aviso, inclusive quando só parte do lote falha e a invocação termina sem erro; item crítico descartado por falta de campo obrigatório | Lote publicado com sucesso; registro ignorado por não ser criação de item crítico |
| `gerar-relatorio-semanal` | Falha na leitura, na gravação ou na publicação (`Falha ao gerar relatório`) | Data de referência rejeitada no acionamento manual, apesar de ser registrada em ERROR |
| as três | `Error` que escapa do handler, como `NoClassDefFoundError` (`Erro na execução da função`), e tempo esgotado | Falha no aquecimento, que acontece no deploy; período sem invocação |

Configuração dos três alarmes:

| Propriedade | Valor | Efeito |
|---|---|---|
| Estatística e limiar | `Sum` maior ou igual a 1 | Uma falha basta |
| `Period` | 60 segundos | Resolução padrão, dentro do nível gratuito |
| `EvaluationPeriods` e `DatapointsToAlarm` | 5 e 1 | Uma falha em qualquer minuto de uma janela de 5 minutos. A janela absorve o atraso entre a linha de log e o ponto da métrica |
| `TreatMissingData` | `notBreaching` | Minuto sem dado conta como sem falha. Sem tráfego, o alarme fica em OK |
| `AlarmActions` | só o tópico de alarmes do ambiente | Nenhum alarme publica no tópico de administradores. Não há aviso de volta a OK |

Em dev, o alarme entrou em ALARM cerca de 1 minuto depois da linha de falha e voltou a OK sozinho cerca de 10 minutos depois de entrar em ALARM. Cada novo episódio gera um novo e-mail.

Limites conhecidos:

- A busca é por trecho de texto. Qualquer linha futura que contenha `FALHA_FUNCAO` ou `Task timed out` dispara o alarme. Os testes de log dos handlers afirmam em quais linhas o marcador aparece, e nenhum texto do aluno vai para log.
- O texto exato da linha de tempo esgotado do runtime `java25` não foi observado, porque o tempo esgotado não foi provocado nas funções. O padrão cobre os dois formatos documentados pelo material de suporte da AWS.
- Invocação sem falha não grava mais linha em WARN. A notificação deixou de gravar `No metrics were emitted` em cada execução bem-sucedida. Nenhum alarme usa WARN como condição.

### Métricas por ambiente

O `Globals` define `POWERTOOLS_METRICS_NAMESPACE` como `FeedbackServerless/<ambiente>`. As métricas do Powertools e as três métricas dos filtros ficam no namespace do ambiente, e um alarme de prod não reage a uma falha de dev. Antes, as métricas iam todas para `FeedbackServerless`, igual nos três ambientes. As que ficaram nesse namespace antigo param de receber dado.

| Origem | Métricas |
|---|---|
| Filtros de log | `FalhasRegistrarAvaliacao`, `FalhasNotificarCritica`, `FalhasGerarRelatorioSemanal` |
| Powertools, registro | `AvaliacaoRegistrada`, `EntradaRejeitada`, `ErroInterno` |
| Powertools, notificação | `ItemInvalido` |
| Powertools, relatório | `RelatorioGerado`, `DataReferenciaRejeitada`, `ErroInterno` |

### Dashboard

O dashboard `feedback-painel-<ambiente>` fica no console do CloudWatch, em `sa-east-1`, em Dashboards. Ele tem cinco widgets:

| Widget | Fonte |
|---|---|
| Alarmes de falha | Estado atual dos três alarmes do ambiente |
| Invocações | `AWS/Lambda` `Invocations` das três funções, soma a cada 5 minutos |
| Falhas, tratadas e de execução | As três métricas dos filtros, soma por minuto |
| Duração (ms) | `AWS/Lambda` `Duration` das três funções, média e máximo a cada 5 minutos |
| Restaurações do SnapStart | Consulta do Logs Insights nas linhas `RESTORE_REPORT` dos três grupos de log, com a função e a duração da restauração |

A duração da restauração vem do log porque não existe métrica do CloudWatch para ela, e a métrica `ColdStart` do Powertools não sai com SnapStart. A consulta lê só as linhas `RESTORE_REPORT`, e os outros widgets são contagens e durações, então o dashboard não mostra descrição, nota nem corpo de mensagem. A consulta roda a cada abertura do dashboard e consome a cota gratuita de dados lidos do Logs Insights. No QA de dev, ela leu 44 KB.

O prefixo `SOURCE '...' |` da consulta só vale no widget do dashboard. A API `StartQuery` recusa esse texto com `MalformedQueryException`. Para rodar a mesma consulta pela CLI, comece em `filter` e passe os três grupos de log em `--log-group-names` do `aws logs start-query`.

### Retenção dos logs

| Ambiente | Retenção dos grupos de log das funções |
|---|---|
| `dev` | 14 dias |
| `hml` | 14 dias |
| `prod` | 30 dias |

O valor vem de `!If [EhProd, 30, 14]`, a mesma condição que já liga a recuperação pontual da tabela só em prod. Prod guarda mais histórico para investigação. Dev recebe as linhas de teste dos alarmes e não precisa guardar mais.

### Limite do nível gratuito

O nível gratuito do CloudWatch cobre 10 métricas de alarme e 3 dashboards. A conta fecha nesses números sem folga:

| Recurso | Por stack | Dev, hml e prod | Já existe na conta | Total |
|---|---|---|---|---|
| Métricas de alarme | 3 | 9 | 1, o alarme de cobrança em `us-east-1` | 10 de 10 |
| Dashboards | 1 | 3 | 0 | 3 de 3 |

A página de preços não diz se o limite é por região ou por conta, então a conta é feita no pior caso, somando o alarme de cobrança de `us-east-1`. Daí as regras:

- **Não crie alarme de teste**, nem temporário, e nenhum dashboard a mais. Para testar, use os alarmes que já existem, como mostrado abaixo.
- Não troque a métrica de um alarme por matemática de métricas nem por consulta Metrics Insights. A primeira cobra cada métrica da expressão, e a segunda sai do nível gratuito.
- Não use período abaixo de 60 segundos, que é alta resolução e também sai do nível gratuito.
- As stacks de hml e prod são removidas depois do vídeo.

### Testar um alarme

Há dois testes, e nenhum deles altera o `$LATEST`, a configuração ou o código das funções. **Os dois geram e-mail real** para o endereço assinante, um por alarme que entrar em ALARM. Os comandos foram escritos para o Git Bash. Acrescente `--profile tc4-admin` se a sua credencial padrão não for dessa conta. As roles de deploy não têm essas permissões.

Para acompanhar o estado dos três alarmes de um ambiente:

```bash
aws cloudwatch describe-alarms \
  --alarm-names feedback-falha-registrar-avaliacao-dev feedback-falha-notificar-critica-dev feedback-falha-gerar-relatorio-semanal-dev \
  --query "MetricAlarms[].[AlarmName,StateValue]" \
  --output table \
  --region sa-east-1
```

**1. Caminho do alarme até o e-mail, com `set-alarm-state`.** Força o estado do alarme, sem passar pela métrica. Prova a ação do alarme, a política do tópico e a assinatura:

```bash
aws cloudwatch set-alarm-state \
  --alarm-name feedback-falha-registrar-avaliacao-dev \
  --state-value ALARM \
  --state-reason "Teste manual do caminho do alarme ate o e-mail" \
  --region sa-east-1
```

Na avaliação seguinte, o alarme recalcula a partir da métrica. Sem falha na janela, volta a OK sozinho: em dev, levou de 60 a 80 segundos.

**2. Caminho da linha de log até o alarme, com `put-log-events`.** Grava uma linha que casa com o filtro num log stream de teste dentro do grupo da função. O filtro vale para todos os streams do grupo, então a linha percorre o mesmo caminho de uma falha real: filtro, métrica, alarme e e-mail.

Rode a partir de uma pasta fora do repositório, porque o arquivo de evento fica nela. O exemplo usa o grupo do registro em dev. Para as outras funções, troque o nome do grupo; para outros ambientes, troque o sufixo.

```bash
GRUPO=/aws/lambda/feedback-registrar-avaliacao-dev
STREAM=qa-observabilidade-$(date +%Y%m%d)

aws logs create-log-stream \
  --log-group-name "$GRUPO" \
  --log-stream-name "$STREAM" \
  --region sa-east-1

AGORA=$(( $(date +%s) * 1000 ))

cat > evento-alarme.json <<EOF
[
  {
    "timestamp": $AGORA,
    "message": "{\"level\":\"ERROR\",\"message\":\"Teste de alarme\",\"function_request_id\":\"qa-observabilidade-manual\",\"alarme\":\"FALHA_FUNCAO\"}"
  }
]
EOF

AWS_CLI_FILE_ENCODING=UTF-8 aws logs put-log-events \
  --log-group-name "$GRUPO" \
  --log-stream-name "$STREAM" \
  --log-events file://evento-alarme.json \
  --region sa-east-1
```

- Se o stream do dia já existir, o `create-log-stream` falha com `ResourceAlreadyExistsException`. Siga direto para o `put-log-events`.
- No Windows, o AWS CLI lê o arquivo de `file://` em cp1252. O `AWS_CLI_FILE_ENCODING=UTF-8` na frente do `put-log-events` é o que mantém os acentos: sem ele, uma linha com acento, como a mensagem real `Falha ao registrar avaliação`, chega ao grupo com dupla codificação.
- O `timestamp` precisa ser o horário atual, em milissegundos. Uma linha com horário antigo cai fora da janela de 5 minutos e não dispara. Uma com horário futuro deixa o alarme em ALARM até aquele horário.
- A linha não pode ter descrição, nota nem corpo de mensagem. O `function_request_id` com o nome do teste identifica a linha para quem ler o grupo depois.
- Para testar o caminho do tempo esgotado, troque `message` por uma linha de texto com `Task timed out`, como `qa-observabilidade-manual Task timed out after 10.01 seconds`.
- Grave uma linha por vez e espere os alarmes voltarem a OK antes do próximo teste, de 10 a 11 minutos depois do minuto da linha.
- A linha aparece também no widget de falhas do dashboard, no minuto em que foi gravada.
- **O stream de teste não é apagado.** Ele expira junto com a retenção do grupo: 14 dias em dev e hml, 30 em prod. Apague o arquivo `evento-alarme.json` ao terminar.

## Bootstrap da conta AWS

O template `infra/bootstrap/github-oidc.yaml` cria o provedor OIDC do GitHub e uma role de deploy por ambiente. Ele é aplicado uma vez, com credencial administrativa, e fica fora do pipeline de propósito: assim o pipeline não consegue alterar as próprias permissões.

Os comandos abaixo foram escritos para o Git Bash no Windows e usam o perfil `tc4-admin`. Rode a partir da raiz do repositório.

### 1. Conferir se a conta já tem o provedor OIDC

Uma conta só aceita um provedor por URL. Se ele já existir, o template precisa reaproveitá-lo.

```bash
aws iam list-open-id-connect-providers --profile tc4-admin
```

- Se a lista não trouxer `token.actions.githubusercontent.com`, siga para o passo 2A.
- Se trouxer, confira se a audiência `sts.amazonaws.com` está em `ClientIDList` e siga para o passo 2B. Troque `<ARN_DO_PROVEDOR>` pelo ARN listado:

```bash
aws iam get-open-id-connect-provider \
  --open-id-connect-provider-arn "<ARN_DO_PROVEDOR>" \
  --profile tc4-admin
```

### 2. Aplicar o template

O `CAPABILITY_AUTO_EXPAND` é necessário porque o template usa a transform `AWS::LanguageExtensions` para gerar os três ambientes. Aplique com `aws cloudformation deploy`, e não com `sam deploy`: a documentação da AWS se contradiz sobre o suporte do SAM CLI a `Fn::ForEach`, e a transform é processada pelo próprio CloudFormation.

O `AWS_CLI_FILE_ENCODING=UTF-8` evita que a CLI leia o template em CP1252, o padrão do Windows, e corrompa os acentos.

**2A. Conta sem o provedor:**

```bash
AWS_CLI_FILE_ENCODING=UTF-8 aws cloudformation deploy \
  --stack-name feedback-bootstrap-oidc \
  --template-file infra/bootstrap/github-oidc.yaml \
  --capabilities CAPABILITY_NAMED_IAM CAPABILITY_AUTO_EXPAND \
  --region sa-east-1 \
  --profile tc4-admin
```

**2B. Conta que já tem o provedor:**

```bash
AWS_CLI_FILE_ENCODING=UTF-8 aws cloudformation deploy \
  --stack-name feedback-bootstrap-oidc \
  --template-file infra/bootstrap/github-oidc.yaml \
  --capabilities CAPABILITY_NAMED_IAM CAPABILITY_AUTO_EXPAND \
  --parameter-overrides CriarProvedorOidc=false \
  --region sa-east-1 \
  --profile tc4-admin
```

Se a criação falhar, a stack fica em `ROLLBACK_COMPLETE` e precisa ser removida antes de tentar de novo. Veja o motivo nos eventos e depois apague a stack:

```bash
aws cloudformation describe-stack-events \
  --stack-name feedback-bootstrap-oidc \
  --query "StackEvents[?ResourceStatus=='CREATE_FAILED'].[LogicalResourceId,ResourceStatusReason]" \
  --output table \
  --region sa-east-1 \
  --profile tc4-admin

aws cloudformation delete-stack \
  --stack-name feedback-bootstrap-oidc \
  --region sa-east-1 \
  --profile tc4-admin
```

### 3. Obter os ARNs das roles

```bash
aws cloudformation describe-stacks \
  --stack-name feedback-bootstrap-oidc \
  --query "Stacks[0].Outputs[?starts_with(OutputKey, 'RoleDeploy')].[OutputKey,OutputValue]" \
  --output table \
  --region sa-east-1 \
  --profile tc4-admin
```

| Output | Ambiente do GitHub | Role |
|---|---|---|
| `RoleDeployDevArn` | `dev` | `github-deploy-feedback-dev` |
| `RoleDeployHmlArn` | `hml` | `github-deploy-feedback-hml` |
| `RoleDeployProdArn` | `prod` | `github-deploy-feedback-prod` |

### 4. Configurar os ambientes do GitHub

Cada ambiente precisa de duas configurações. Nenhuma delas vai para arquivo versionado.

| Nome | Tipo | Valor |
|---|---|---|
| `AWS_ROLE_ARN` | variável | ARN da role do ambiente, obtido no passo 3 |
| `EMAIL_ADMIN` | secret | E-mail que assina os tópicos de administradores e de alarmes do ambiente |

Sem `EMAIL_ADMIN`, o job de deploy falha antes de chamar o `sam deploy`, com uma mensagem indicando o secret ausente.

Para gravar o secret sem deixar o e-mail no histórico do terminal, digite o endereço quando o `read` aguardar a entrada. Repita para `hml` e `prod`, trocando `--env`:

```bash
read -rs EMAIL_ADMIN
printf '%s' "$EMAIL_ADMIN" | gh secret set EMAIL_ADMIN --env dev --repo g-andradd/fiap-tc4-feedback-serverless
unset EMAIL_ADMIN
```

### 5. Criar a service-linked role do API Gateway

Este é um passo manual de administrador, feito uma vez por conta e antes do primeiro deploy da aplicação, ou seja, antes do primeiro push em `develop`. A role `AWSServiceRoleForAPIGateway` não faz parte do template, e as roles de deploy não conseguem criá-la.

Confira antes se ela já existe. Se existir, este passo não é necessário:

```bash
aws iam get-role --role-name AWSServiceRoleForAPIGateway --profile tc4-admin
```

Se não existir, crie a role com o perfil administrativo:

```bash
aws iam create-service-linked-role --aws-service-name ops.apigateway.amazonaws.com --profile tc4-admin
```

O motivo é que as roles de deploy não têm `iam:CreateServiceLinkedRole`, de propósito, e a permissão não foi aberta no bootstrap. No deploy de 05/11/2026, o CloudTrail registrou essa ação negada à role de deploy de dev, pedida pelo API Gateway, 3 segundos antes da negação de `CreateStage`, que derrubou o `AWS::ApiGatewayV2::Stage`.

A falha do Stage em si foi por `apigateway:TagResource`. Não ficou provado que o Stage dependa da service-linked role, porque a role e a correção da tag entraram juntas antes do deploy verde. Criar a role antes elimina essa dúvida numa conta nova.

### Atualizar o bootstrap depois

Reaplique com o mesmo comando do passo 2, usando o perfil administrativo. As roles de deploy têm negação explícita para alterar a si mesmas, as próprias políticas, o provedor OIDC e a stack `feedback-bootstrap-oidc`, então essa atualização nunca passa pelo pipeline.

Antes de aplicar, recomendamos gerar um change set para revisar as mudanças. Acrescente `--no-execute-changeset` ao comando do passo 2: a CLI cria o change set e termina sem executá-lo. Depois, troque `<ARN_DO_CHANGE_SET>` pelo ARN mostrado na saída, revise e aplique:

```bash
aws cloudformation describe-change-set \
  --change-set-name "<ARN_DO_CHANGE_SET>" \
  --region sa-east-1 \
  --profile tc4-admin

aws cloudformation execute-change-set \
  --change-set-name "<ARN_DO_CHANGE_SET>" \
  --region sa-east-1 \
  --profile tc4-admin
```

**Atualização de 06/11/2026: leitura de tópico no boundary.** O aquecimento das funções chama `sns:GetTopicAttributes` no tópico de administradores, e o boundary só liberava `sns:Publish`. O statement `PublicarNoTopico` do boundary `feedback-limite-funcoes-<ambiente>` virou `TopicosDoAmbiente`, com `sns:GetTopicAttributes` e `sns:Publish` nos mesmos tópicos do ambiente. Nada mudou nas políticas das roles de deploy. A atualização seguiu este procedimento: o change set trouxe só os três boundaries como `Modify`, sem substituição, e a stack terminou em `UPDATE_COMPLETE`. A versão v2 dos três boundaries foi conferida com `aws iam get-policy-version`.

### Regras que os próximos templates precisam seguir

As roles de deploy só alcançam o que segue a convenção abaixo. Fora dela, o deploy é negado.

- **Nomes:** `feedback-<nome>-<ambiente>` ou o nome gerado a partir da stack, `feedback-<ambiente>-*`. Alarmes e dashboards precisam de nome explícito.
- **Roles de função:** com o boundary `feedback-limite-funcoes-<ambiente>`. O `Globals` do `template.yaml` já aplica isso às funções. A role gerada por um evento `ScheduleV2` precisa declarar `PermissionsBoundary` no próprio evento.
- **Políticas gerenciadas:** só `AWSLambdaBasicExecutionRole`, `AWSLambdaDynamoDBExecutionRole` e `AWSXrayWriteOnlyAccess`. Para o resto, use as policy templates do SAM ou política inline.
- **Logs:** grupos em `/aws/lambda/`, seguindo a mesma convenção de nomes.
