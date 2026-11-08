# Design: add-notificacao-critica

## Context

A avaliação crítica é gravada por `registrar-avaliacao` e o aviso sai de forma assíncrona, a partir do evento que a gravação gera no Stream. Este change cria a função `notificar-critica` com seu mapeamento. Reutiliza o `Globals`, o contrato do item e as regras de SnapStart e de logs de `add-registro-avaliacao` (D2, D3, D7, D8, D9 e D10), mais a tabela com Stream `NEW_IMAGE` e o tópico criptografado com `alias/aws/sns` (fundação D6).

Pontos que o PO deixou para este design: como excluir alterações, a política de reprocessamento e o que aparece na data do aviso.

**Fontes.** Salvo indicação em contrário, as páginas citadas foram consultadas em 03/11/2026. "Verificado localmente" significa executado em pasta temporária com o SAM CLI 1.171.0 (`sam validate --lint` e transform do SAM), sem teste na AWS.

## Goals / Non-Goals

**Goals**
- Avisar os administradores uma vez por avaliação crítica criada, na própria execução acionada pelo evento
- Falha ao publicar nunca ser sucesso silencioso, e também nunca travar o shard indefinidamente
- Não precisar de mudança no bootstrap, que ainda vai ser aplicado

**Non-Goals**
- Deduplicação, HTML e outros canais
- Alarmes, que ficam em `add-observabilidade-seguranca`
- Escrita na tabela pela função

## Decisions

### D1. Filtro exato do Stream
```
{"eventName":["INSERT"],"dynamodb":{"NewImage":{"urgencia":{"S":["CRITICA"]}}}}
```

Campos no mesmo padrão são combinados com E, e filtros diferentes com OU ([filtragem](https://docs.aws.amazon.com/lambda/latest/dg/invocation-eventfiltering.html)). Registro que não casa não invoca a função, e o iterador avança sobre ele.

O padrão `NewImage.urgencia` vem do [guia de filtragem de DynamoDB](https://docs.aws.amazon.com/lambda/latest/dg/with-ddb-filtering.html). Como o Stream é `NEW_IMAGE`, não existe `OldImage` para diferenciar criação de alteração, então só `eventName` faz essa separação.

**A documentação se contradiz sobre `eventName`:**
- O [guia de DynamoDB](https://docs.aws.amazon.com/lambda/latest/dg/with-ddb-filtering.html) afirma: "DynamoDB event source mappings only support filtering on the `dynamodb` key".
- A [página geral](https://docs.aws.amazon.com/lambda/latest/dg/invocation-eventfiltering.html) diz que o padrão pode incluir propriedades de metadados e de dados. O mesmo guia de DynamoDB, na tabela de formatos, cita filtrar por "other metadata properties".
- O [exemplo de evento](https://docs.aws.amazon.com/lambda/latest/dg/with-ddb.html) mostra `eventName` no nível de cima, e a [referência do CDK](https://docs.aws.amazon.com/cdk/api/v2/docs/aws-cdk-lib.aws_lambda.FilterCriteria.html) e uma [resposta no re:Post](https://repost.aws/questions/QUpeoP1XJmSrCmgAVrrufFKw/lambda-event-filtering-for-dynamodb-event-name-insert-not-working) usam filtro com `eventName`.

Decisão: usar `eventName` no filtro, porque é o único meio de não invocar a função em alteração, e não depender só dele (D2). A tarefa 5.5 testa na conta. Se o filtro por `eventName` não funcionar, nenhum requisito é violado, porque D2 ainda impede o aviso. O efeito seria só invocação a mais.

É refinamento do `project.md`, ver "Desvios".

Alternativas descartadas:
- Filtro só por urgência, como no `project.md`. Deixa alteração de item crítico chegar à função.
- Stream `NEW_AND_OLD_IMAGES`, para detectar criação pela ausência de `OldImage`. Exige mudar a tabela da fundação e grava mais dados no Stream, sem necessidade.

### D2. Segunda checagem no handler
Para cada registro, o handler ignora e conta como processado o que não for `eventName = INSERT` ou `NewImage.urgencia.S = CRITICA`.

Assim a spec "aviso somente na criação" vale mesmo que o filtro deixe algo passar, e o requisito fica coberto por teste unitário, sem depender da conta.

Alternativa descartada: confiar só no filtro. Um filtro não tem teste unitário, e a documentação se contradiz.

### D3. Política de reprocessamento
| Parâmetro | Valor | Efeito |
|---|---|---|
| `FunctionResponseTypes` | `ReportBatchItemFailures` | Só o que falhou volta |
| `BatchSize` | 10 | Lote pequeno, sem reduzir a um |
| `MaximumBatchingWindowInSeconds` | não declarado | Padrão 0: invoca assim que há registro, coerente com "imediato" |
| `BisectBatchOnFunctionError` | `true` | Em erro não tratado, como timeout, divide o lote. [Dividir não consome tentativas](https://docs.aws.amazon.com/lambda/latest/dg/services-dynamodb-errors.html) |
| `MaximumRetryAttempts` | 5 | [Padrão é -1, infinito](https://docs.aws.amazon.com/lambda/latest/dg/services-ddb-params.html) |
| `MaximumRecordAgeInSeconds` | 3600 | Padrão é -1, até expirar em 24 h |
| `StartingPosition` | `TRIM_HORIZON` | [`LATEST` pode perder eventos durante a criação do mapeamento](https://docs.aws.amazon.com/lambda/latest/dg/with-ddb.html) |
| `DestinationConfig.OnFailure` | bucket de relatórios | D4 |

**Dentro do handler.** Percorre o lote em ordem. Na primeira falha ao publicar, registra em ERROR e **retorna imediatamente** com o número de sequência daquele registro em `batchItemFailures`. A [documentação](https://docs.aws.amazon.com/lambda/latest/dg/services-ddb-batchfailurereporting.html) diz que Lambda retoma do menor número de sequência informado, reprocessando tudo a partir dele, e o exemplo Java dela faz esse mesmo retorno imediato. Continuar publicando depois da falha só geraria duplicatas.

O lote é dividido no número de sequência informado quando os dois recursos estão ligados, conforme a mesma página.

Tipos: `RequestHandler<DynamodbEvent, StreamsEventResponse>`, como no exemplo da documentação.

**Quando as tentativas se esgotam.** Lambda [descarta o registro, grava o destino de falha e segue com o shard](https://docs.aws.amazon.com/lambda/latest/dg/services-dynamodb-errors.html). A avaliação continua na tabela e o relatório semanal a inclui, mas o aviso daquela avaliação não sai sozinho. Esse é o limite que a spec prevê em "enquanto a política de reprocessamento não se esgotar".

**Prazo do shard.** A pior espera, com bloqueio do shard, é a menor entre 5 tentativas e 1 hora. Com SDK limitado a 5 segundos por chamada de publicação (`apiCallTimeout`), uma tentativa não passa do `Timeout` de 30 segundos.

As páginas consultadas não informam o intervalo entre tentativas em stream, então não dá para garantir quanto tempo a política cobre em uma indisponibilidade do SNS.

Alternativas descartadas:
- Infinito, o padrão. A documentação avisa que um registro ruim bloqueia o shard por até um dia.
- `BatchSize: 1`. Simplifica a função, mas a spec tem cenário de lote e o handler precisaria tratar lote de qualquer forma.
- Publicar o lote inteiro e informar todas as falhas. Lambda retoma do menor número de sequência de todo modo, então duplica o que já tinha saído.
- Utilitário Batch do Powertools. Mais uma dependência e mais uma API para explicar, para um laço de poucas linhas.

### D4. Destino de falha: bucket de relatórios, prefixo `aws/lambda/`
**Confirmado:** o boundary e as roles de deploy não têm nenhuma permissão de SQS. Uma fila como destino seria negada no deploy e em execução.

| Opção | Bootstrap | Sobrevive a falha do SNS | Guarda o conteúdo | Observação |
|---|---|---|---|---|
| SQS | Precisa mudar: deploy e boundary | Sim | Não, só metadados | Exige mais um recurso e mudança na fundação |
| Tópico de administradores | Sem mudança | **Não** | Não, só metadados | A causa mais provável da falha é o próprio tópico ou a chave dele |
| Bucket de relatórios | Sem mudança | Sim | **Sim, o evento inteiro** | Boundary já libera `s3:PutObject` e `s3:ListBucket` |

Escolha: **S3**, no bucket existente. A [documentação](https://docs.aws.amazon.com/lambda/latest/dg/services-dynamodb-errors.html) pede `s3:PutObject` e `s3:ListBucket`, grava em `aws/lambda/<ESM-UUID>/<shardID>/AAAA/MM/DD/...` e, ao contrário de SQS e SNS, inclui o evento em `payload`. Isso importa porque o Stream retém só 24 horas: com o objeto, o aviso perdido pode ser refeito à mão.

Criptografia: o bucket usa SSE-S3, sem KMS, então não há permissão extra.

Segurança: a mesma página alerta que excluir o bucket sem remover o destino permite que outra conta recrie o nome e receba os registros, e pede a condição `s3:ResourceAccount`, que a role adota (D8). Como o mapeamento e o bucket ficam na mesma stack, `sam delete` remove os dois.

**A documentação se contradiz:** o [guia de erros](https://docs.aws.amazon.com/lambda/latest/dg/services-dynamodb-errors.html) e a [referência do CloudFormation](https://docs.aws.amazon.com/AWSCloudFormation/latest/TemplateReference/aws-properties-lambda-eventsourcemapping-onfailure.html) aceitam S3 para DynamoDB. Já a [tabela de parâmetros](https://docs.aws.amazon.com/lambda/latest/dg/services-ddb-params.html) e a [propriedade do SAM](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-property-function-dynamodb.html) citam só SQS e SNS. Sigo o guia, que é o mais específico e traz exemplo de S3 para DynamoDB. Verificado localmente: o SAM repassa o ARN sem alterar e `sam validate --lint` passa.

**Resolvido no primeiro deploy (05/11/2026): o serviço aceita S3 como destino, mas exige o bucket inteiro.** O deploy em dev falhou ao criar `NotificarCriticaAvaliacaoCriada` com `InvalidParameterValueException: The function execution role does not have permissions to call PutObject on S3`. O destino foi aceito; o que faltou foi escopo. Teste com role e função temporárias contra o Stream real de dev:

| Recurso de `s3:PutObject` | `s3:ResourceAccount` | Resultado |
|---|---|---|
| `<bucket>/aws/lambda/*` | sim | recusado |
| `<bucket>/aws/lambda/*` | não | recusado |
| `<bucket>/*` | sim | aceito, duas vezes |

Conclusão: na criação do mapeamento, o Lambda valida `PutObject` no bucket inteiro, e não no prefixo onde de fato grava. O recurso passa a ser `${BucketRelatorios.Arn}/*`, mantendo `s3:ResourceAccount`.

Custo: a função passa a poder gravar em qualquer chave do bucket de relatórios, inclusive no prefixo do relatório semanal. Não há ampliação além do boundary, que já liberava `feedback-*-<amb>/*` (`ObjetosDoAmbiente`). Os registros continuam indo para `aws/lambda/`, porque o prefixo é definido pelo serviço, e a função não tem código que grave no S3.

**Plano B, não aplicado:** trocar `Destination` para `!Ref TopicoAdmins` e remover a permissão de S3, sem mudar o bootstrap. Perde o conteúdo e a independência do SNS, mas mantém o aviso em caso de falha transitória. Por decisão do Gabriel em 04/11/2026, seria aplicado sem nova consulta. A condição dele (deploy recusar S3 como destino) não ocorreu, e aplicá-lo perderia o conteúdo do lote, então a correção foi ampliar o recurso do `PutObject`. O que aconteceu fica registrado em `qa.md` deste change.

`Type` fica de fora do `OnFailure` de propósito, para o SAM não gerar política por conta própria: a política precisa ser a da D8.

Alternativas descartadas:
- SQS: ver tabela. Mudaria a fundação sem ganho sobre S3, inclusive guardando menos dados.
- Sem destino: o registro esgotado sumiria deixando só o log.
- Bucket dedicado: mais um recurso para um caso raro, com a mesma sensibilidade de dados.

### D5. Item sem campo obrigatório
Item que casa com o filtro e não passa em `Avaliacao.deAtributos` (`add-registro-avaliacao` D3), por exemplo sem `descricao` ou `dataEnvio`, é **descartado na hora**: log em ERROR com `eventID` e nomes dos campos, nunca valores, métrica `ItemInvalido`, e o registro conta como processado.

Tentar de novo não conserta o item, e cada tentativa atrasaria os avisos seguintes do shard. O único gravador da tabela é `registrar-avaliacao`, que sempre grava os seis atributos, então esse caso só surge por escrita manual.

Falhas de publicação continuam na política da D3. A diferença é que falha de publicação é transitória, e item inválido é permanente.

Alternativa descartada: tratar como falha de publicação, para cair no destino de S3. Seriam até 5 tentativas inúteis com o shard parado, em troca de um objeto que o log já cobre.

### D6. Conteúdo do aviso
`AvisoCritico.de(Avaliacao)`, no domínio, devolve assunto e corpo:

```
Assunto: Avaliação crítica registrada

Uma avaliação crítica foi registrada.

Urgência: CRITICA
Data de envio: 2026-11-03

Descrição:
<descrição completa, sem corte>
```

**Data: só `dataEnvio`, como persistida.** É a data que a spec pede, o valor não muda no reprocessamento e o próprio e-mail já tem horário de chegada. Mostrar a hora exigiria converter `criadoEm` de UTC para São Paulo, sem ninguém ter pedido.

Limites: o assunto tem 28 caracteres, e a API aceita [UTF-8 com menos de 100 caracteres](https://docs.aws.amazon.com/sns/latest/api/API_Publish.html). O corpo, com 1000 caracteres, cabe nos 256 KiB.

Descrição vai como texto puro, então nada de HTML é interpretado, e o e-mail usa protocolo `email`, sem JSON. A descrição pode conter quebras de linha, o que é aceitável.

Alternativas descartadas:
- `dataEnvio` e hora de `criadoEm`. Ver acima.
- `MessageStructure: json`. Há um único protocolo assinante.

### D7. Logs e SnapStart
Valem `add-registro-avaliacao` D7 e D9: nenhuma descrição em nenhum nível, nem no evento recebido ou na mensagem publicada. Pode ir para log: `id`, `eventID`, número de sequência, classe e código de erro da exceção, e `MessageId` do SNS.

O cliente SNS é `static final`, criado com a cadeia padrão de credenciais e `url-connection-client`.

### D8. IAM de `notificar-critica`
Role gerada, logical ID `NotificarCriticaRole`, com boundary:

| Origem | Ações | Recurso |
|---|---|---|
| Inline | `sns:Publish` | ARN do tópico |
| Inline | `kms:Decrypt` e `kms:GenerateDataKey*` | `key/*` da conta e região, com `kms:ViaService = sns.<região>.amazonaws.com` |
| Inline | `s3:PutObject` | `<bucket>/*`, com `s3:ResourceAccount` (D4: o serviço recusa escopo menor) |
| Inline | `s3:ListBucket` | ARN do bucket, com `s3:ResourceAccount` |
| [`AWSLambdaDynamoDBExecutionRole`](https://docs.aws.amazon.com/aws-managed-policy/latest/reference/AWSLambdaDynamoDBExecutionRole.html) | `dynamodb:DescribeStream`, `dynamodb:GetRecords`, `dynamodb:GetShardIterator`, `dynamodb:ListStreams`, `logs:CreateLogGroup`, `logs:CreateLogStream` e `logs:PutLogEvents` | `*` na política, limitado pelo boundary |
| `AWSLambdaBasicExecutionRole` | `logs:CreateLogGroup`, `logs:CreateLogStream` e `logs:PutLogEvents` | limitado pelo boundary |
| `AWSXrayWriteOnlyAccess` | as cinco de `xray` listadas em `add-registro-avaliacao` D13 | `*` |

**`alias/aws/sns`:** a [documentação do SNS](https://docs.aws.amazon.com/sns/latest/dg/sns-key-management.html) exige `kms:GenerateDataKey*` e `kms:Decrypt` do publicador e pede o ARN completo da chave. Como é chave gerenciada, o ID não é conhecido no template, então o recurso fica `key/*`, restrito por `kms:ViaService`, que é o mesmo recorte do boundary. A página do SNS não reproduz a política da chave `aws/sns`, então a escolha segue o texto dela.

**Conferência contra o bootstrap:**
- Todas as ações cabem em `feedback-limite-funcoes-<amb>`: `PublicarNoTopico`, `ChavesGerenciadasViaServico`, `ObjetosDoAmbiente`, `BucketDoAmbiente`, `StreamDoAmbiente`, `ListarStreams`, `Logs` e `Tracing`.
- Verificado localmente no template gerado: `iam:AttachRolePolicy` recebe `AWSLambdaBasicExecutionRole`, `AWSXrayWriteOnlyAccess` e `AWSLambdaDynamoDBExecutionRole`, justamente as três aceitas.
- O mapeamento aponta para `NotificarCriticaAliaslive`. `lambda:CreateEventSourceMapping` aceita porque `lambda:FunctionArn` cobre `feedback-*-<amb>:*`.
- Função `feedback-notificar-critica-<amb>` e role `feedback-<amb>-NotificarCriticaRole-<aleatório>` casam com os padrões.

**A função não tem nenhuma permissão sobre a tabela além do Stream**, o que cumpre "não executa escrita na tabela" por construção. O registro também não depende do tópico: `registrar-avaliacao` não tem `sns:Publish`.

Nenhuma alteração no bootstrap é necessária.

Alternativa descartada: política `SNSPublishMessagePolicy` do SAM. Não cobre a chave KMS, e a declaração explícita mostra as ações no template.

## Risks / Trade-offs

- **Filtro por `eventName`:** a documentação se contradiz (D1). Se não funcionar, a função é invocada em alteração e D2 não publica nada. Custo mínimo. A tarefa 5.5 mostra qual dos casos é real.
- **S3 como destino:** a documentação se contradizia (D4), e o primeiro deploy resolveu: o serviço aceita, desde que `s3:PutObject` cubra o bucket inteiro. Trade-off: a função pode gravar em qualquer chave do bucket, inclusive no prefixo do relatório semanal, dentro do que o boundary já permitia. Mitigação: o código da função não usa S3, e quem grava é o serviço, sempre em `aws/lambda/`.
- **Objeto em `aws/lambda/` contém a descrição:** é um dado do aluno em bucket privado, a mesma sensibilidade do relatório, mas sem expiração. A política de ciclo de vida fica fora do escopo. Os relatórios ficam em outro prefixo, então "um único relatório por data" continua verdadeiro.
- **`sam delete` com bucket não vazio:** o CloudFormation [só exclui bucket vazio](https://docs.aws.amazon.com/AWSCloudFormation/latest/TemplateReference/aws-resource-s3-bucket.html) (04/11/2026). Este change e o relatório passam a gravar nele. Mitigação: esvaziar antes, com o comando na documentação (tarefa 6.1).
- **Backlog no primeiro deploy:** com `TRIM_HORIZON`, avaliações críticas de até 1 hora atrás geram aviso quando o mapeamento nasce, e as mais antigas, até o limite de 24 horas do Stream, podem ir para o destino de falha. Aceito, é melhor que perder evento.
- **Snapshot expirado:** depois de 14 dias sem avaliação crítica, a invocação falha com `SnapStartNotReadyException` até a versão voltar a `Active` (`add-registro-avaliacao` D7). A documentação diz que falha antes da invocação é repetida até o limite de idade, que é de 1 hora, mas não cita esse erro pelo nome. Mitigação: invocar antes do vídeo.
- **Aviso duplicado:** aceito pela spec.
- **E-mail depende de confirmar a assinatura:** sem isso, a função tem sucesso e nada chega. Tarefa 5.3.

## Open Questions

Nenhuma.

## Alterações no template.yaml

Dependência: integrar depois de `add-registro-avaliacao`, porque usa o `Globals` e o item definidos lá. Não altera `Globals` nem blocos de outro change.

**1. Acrescenta ao fim de `Resources`**, depois de `RegistrarAvaliacao`:

```yaml
  LogNotificarCritica:
    Type: AWS::Logs::LogGroup
    Properties:
      LogGroupName: !Sub /aws/lambda/feedback-notificar-critica-${Stage}
      RetentionInDays: 14

  NotificarCritica:
    Type: AWS::Serverless::Function
    DependsOn: LogNotificarCritica
    Properties:
      FunctionName: !Sub feedback-notificar-critica-${Stage}
      CodeUri: functions/notificar-critica
      Handler: br.com.fiap.feedback.notificacao.NotificarCriticaHandler::handleRequest
      AutoPublishAliasAllProperties: true
      Environment:
        Variables:
          POWERTOOLS_SERVICE_NAME: notificar-critica
          TOPICO_ADMINS_ARN: !Ref TopicoAdmins
      Policies:
        - Version: "2012-10-17"
          Statement:
            - Sid: PublicarNoTopico
              Effect: Allow
              Action: sns:Publish
              Resource: !Ref TopicoAdmins
            - Sid: UsarChaveDoTopico
              Effect: Allow
              Action:
                - kms:Decrypt
                - kms:GenerateDataKey*
              Resource: !Sub arn:${AWS::Partition}:kms:${AWS::Region}:${AWS::AccountId}:key/*
              Condition:
                StringEquals:
                  kms:ViaService: !Sub sns.${AWS::Region}.amazonaws.com
            - Sid: GuardarLoteDescartado
              Effect: Allow
              Action: s3:PutObject
              Resource: !Sub ${BucketRelatorios.Arn}/*
              Condition:
                StringEquals:
                  s3:ResourceAccount: !Ref AWS::AccountId
            - Sid: ListarBucketDoDestino
              Effect: Allow
              Action: s3:ListBucket
              Resource: !GetAtt BucketRelatorios.Arn
              Condition:
                StringEquals:
                  s3:ResourceAccount: !Ref AWS::AccountId
      Events:
        AvaliacaoCriada:
          Type: DynamoDB
          Properties:
            Stream: !GetAtt TabelaAvaliacoes.StreamArn
            StartingPosition: TRIM_HORIZON
            BatchSize: 10
            FunctionResponseTypes:
              - ReportBatchItemFailures
            BisectBatchOnFunctionError: true
            MaximumRetryAttempts: 5
            MaximumRecordAgeInSeconds: 3600
            DestinationConfig:
              OnFailure:
                Destination: !GetAtt BucketRelatorios.Arn
            FilterCriteria:
              Filters:
                - Pattern: '{"eventName":["INSERT"],"dynamodb":{"NewImage":{"urgencia":{"S":["CRITICA"]}}}}'
```

**2. Acrescenta ao fim de `Outputs`:**

```yaml
  NotificarCriticaNome:
    Description: Nome da função de notificação
    Value: !Ref NotificarCritica
  NotificarCriticaAliasArn:
    Description: ARN do alias live da função de notificação
    Value: !Ref NotificarCritica.Alias
```

IDs lógicos novos: `LogNotificarCritica` e `NotificarCritica`. O SAM também gera `NotificarCriticaRole`, `NotificarCriticaAliaslive`, `NotificarCriticaVersion<hash>` e `NotificarCriticaAvaliacaoCriada`.

## Desvios do project.md

**Refinamento do filtro.** O `project.md` diz "DynamoDB Streams (filtro: urgencia = CRITICA)". Este design acrescenta `eventName = INSERT`, porque a spec exige aviso só na criação. O filtro por urgência continua valendo. Sugestão ao fechar o change: atualizar a linha do diagrama em `project.md` para "filtro: criação com urgencia = CRITICA".
