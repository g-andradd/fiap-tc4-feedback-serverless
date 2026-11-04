# Design: add-relatorio-semanal

## Context

A função `gerar-relatorio-semanal` consolida as avaliações dos 7 dias civis anteriores à data de referência, grava no bucket privado e avisa pelo tópico de administradores. Roda toda segunda às 08:00 em America/Sao_Paulo e pode ser acionada à mão para o vídeo.

Reutiliza `Globals`, contrato do item e regras de SnapStart e logs de `add-registro-avaliacao` (D2, D3, D7, D8, D9 e D10), mais o índice `por-data`, o bucket e o tópico (fundação D6). `Avaliacao.deAtributos` dá o formato do item, e `dataEnvio` já está no fuso certo, então o índice não precisa de conversão.

**Fontes.** Salvo indicação em contrário, as páginas citadas foram consultadas em 03/11/2026. "Verificado localmente" significa executado em pasta temporária com o SAM CLI 1.171.0 (`sam validate --lint` e transform do SAM), sem teste na AWS.

## Goals / Non-Goals

**Goals**
- Relatório com o conteúdo da spec, de leitura direta no vídeo
- Nunca publicar relatório com zeros falsos: leitura incompleta encerra como falha
- Reexecução segura, com um objeto por data de referência

**Non-Goals**
- Formato visual elaborado, filtros e períodos diferentes de 7 dias
- Histórico e listagem de relatórios
- Alarmes, que ficam em `add-observabilidade-seguranca`

## Decisions

### D1. Um gatilho; acionamento manual é invocação direta
O único gatilho é o evento `ScheduleV2`. O acionamento manual não é um segundo gatilho: é uma chamada a `lambda:Invoke` com a credencial do Gabriel, sem recurso a mais no template.

**Comando manual, sempre pelo alias.** [SnapStart só vale em versão publicada ou alias](https://docs.aws.amazon.com/lambda/latest/dg/snapstart.html), e sem o qualificador a chamada vai para `$LATEST`:

```bash
aws lambda invoke \
  --function-name feedback-gerar-relatorio-semanal-dev:live \
  --region sa-east-1 \
  --cli-binary-format raw-in-base64-out \
  --cli-read-timeout 150 \
  --payload '{"dataReferencia":"2026-11-04"}' \
  resposta.json
```

- Sem data: `--payload '{}'`, e a referência é o dia atual em São Paulo.
- `--cli-binary-format` é [exigido no AWS CLI v2](https://docs.aws.amazon.com/lambda/latest/dg/snapstart-activate.html) para o payload em texto.
- `--cli-read-timeout` fica acima do `Timeout` de 120 segundos da função, porque o [padrão do CLI é 60](https://docs.aws.amazon.com/cli/latest/reference/index.html).
- Falha na execução aparece em `FunctionError` na saída do comando, e a mensagem de erro vai para `resposta.json`.

Para o vídeo, a referência de amanhã inclui a avaliação de hoje, como a spec prevê.

Alternativas descartadas:
- Rota na API. Exporia a geração sem autenticação.
- Segundo agendamento. A spec pede acionamento sob demanda.
- Invocação sem qualificador. Roda sem SnapStart e sem nenhum erro aparente, então o vídeo mostraria uma coisa e a produção faria outra.

### D2. Data de referência
Handler `RequestHandler<Map<String, Object>, Map<String, Object>>`. A entrada é o objeto de evento, com `dataReferencia` opcional:

| Valor | Resultado |
|---|---|
| Ausente ou `null` | Dia atual em America/Sao_Paulo, `LocalDate.now(clock)` dentro do handler |
| Texto no formato e data existente | Usa a data |
| Vazio, outro formato, data inexistente ou não texto | Lança `DataReferenciaInvalidaException`, antes de qualquer leitura |

`DataReferencia.de(Object, Clock)` fica no domínio. A validação usa regex `\d{4}-\d{2}-\d{2}` e depois `LocalDate.parse`, porque `ISO_LOCAL_DATE` é estrito para `2026-02-30`, mas aceita ano com sinal, como `+12026-01-01`.

A exceção não tratada é o que faz a invocação voltar com `FunctionError`, e a mensagem diz que a data é inválida. Nenhum relatório é gravado e nenhum aviso sai, então o relatório existente fica intacto. A invocação manual é síncrona, então a rejeição não tem a repetição automática da D8.

Alternativa descartada: responder sucesso com um campo de erro. A execução precisa terminar como rejeitada, e um retorno normal se confundiria com sucesso.

### D3. Leitura do índice `por-data`
Uma `Query` por dia da janela, 7 no total, em ordem cronológica, com `KeyConditionExpression` em `dataEnvio`. Sem filtro e sem `Limit`.

- **Paginação:** usa `queryPaginator` do SDK e percorre todas as páginas, porque [cada resposta traz no máximo 1 MB](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GSI.html). Uma exceção em qualquer página sobe para o handler e encerra a execução.
- **Sem resultado parcial:** a lista completa só fica pronta depois das 7 consultas, antes de qualquer escrita. Isso cumpre os cenários de falha total, de um dia e no meio de um dia.
- **Ordem:** dia ascendente e, no dia, `criadoEm` ascendente, que é a ordem do índice e cronológica (`add-registro-avaliacao` D2).
- **Sem paralelismo:** o volume não justifica.
- **Item inválido:** se `Avaliacao.deAtributos` rejeitar um item, é leitura incompleta: a execução falha, com log de `id` e nomes dos campos. Um item sem `nota` deixaria a média errada, e a regra já é "não publicar com dado faltando". O único gravador sempre preenche tudo.

A urgência é a persistida, como a spec exige, e `Urgencia.valueOf` só serve para contar. Valor desconhecido também é item inválido.

Alternativas descartadas:
- `Scan` com filtro. Fundação D6 criou o índice para evitar isso.
- Ignorar o item inválido e seguir. Relatório sairia com números errados sem ninguém perceber.

### D4. Consistência do índice
O [índice é atualizado de forma eventualmente consistente](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GSI.html), normalmente em uma fração de segundo, e leitura fortemente consistente não é opção: a página só cita leituras eventualmente consistentes em índice global.

**Efeito no vídeo:** uma avaliação registrada e um relatório acionado no segundo seguinte podem não coincidir, e o relatório sairia sem ela.

**Mitigação, sem código:** o roteiro espera alguns segundos entre registrar e acionar. Se ainda assim faltar, reexecutar para a mesma data substitui o objeto, sem efeito colateral além de novo aviso.

Na execução agendada isso não afeta nada, porque a janela termina no dia anterior.

Alternativas descartadas:
- `sleep` na função, para esperar a propagação. Custa tempo em toda execução para um caso que só aparece no vídeo.
- Ler pela tabela. Exigiria `Scan`.

### D5. Formato e chave do objeto
**Texto puro em UTF-8.** Chave `relatorios/<dataReferencia>.txt`, com `ContentType: text/plain; charset=utf-8`. Mesma chave na reexecução, então `PutObject` substitui: um objeto por data de referência.

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
Descrição: <descrição como persistida>
```

Na semana vazia, a linha da média é `Média das notas: sem avaliações na janela`, a lista fica `Avaliações (0)` e os totais ficam zerados. O valor de média não aparece, como a spec pede.

O motivo principal é o vídeo: o console do S3 abre o texto no navegador, sem ferramenta nem interpretação. `FormatadorRelatorio`, no domínio, é uma função pura e fácil de testar, e **a spec não precisa mudar**, porque a vírgula de `5,67` fica como está.

A `nota` individual não aparece, porque a spec não a lista no relatório. Ela só alimenta a média.

Alternativas descartadas:
- JSON. Bom para máquina, ruim de mostrar. Exigiria decidir entre número com ponto, o que muda a spec (5,67), ou texto, o que perde o motivo de usar JSON.
- HTML. A descrição é texto livre do aluno, então precisaria de escape para não virar injeção, e a spec deixa formato visual fora do escopo.
- Markdown em tabela. `|` e quebra de linha na descrição quebrariam a tabela.
- CSV. Não acomoda bem totais e lista no mesmo arquivo.

### D6. Média
`BigDecimal`, exato e sem `double`: soma inteira dividida pela quantidade, com `divide(quantidade, 2, RoundingMode.HALF_UP)`. Saída com `toPlainString()`, trocando `.` por `,`.

| Notas | Valor exato | Saída |
|---|---|---|
| 5, 6, 6 | 5,666... | `5,67` |
| 2, 5, 8 | 5 | `5,00` |
| só 0 | 0 | `0,00` |
| soma 41 em 8 avaliações | 5,125 | `5,13` |
| sem avaliações | | sem valor |

**Empate sobe:** `HALF_UP` é o arredondamento que a pessoa espera ao conferir à mão. A formatação não depende do `Locale` do ambiente.

Alternativas descartadas:
- `HALF_EVEN`. Daria `5,12` para 5,125, o que parece erro em uma conferência à mão.
- `String.format` com `Locale` e `double`. Ponto flutuante pode errar empates, e 5,125 passaria a depender da representação binária.

### D7. Conteúdo do aviso: resumo e localização, sem link assinado
Decisão do Gabriel em 04/11/2026: além da localização, o aviso traz a média, a quantidade por urgência e a quantidade por dia. Descrições e notas individuais ficam de fora. O administrador vê os números da semana no e-mail, e o conteúdo escrito pelos alunos continua só no relatório privado.

```
Assunto: Relatório semanal 2026-11-09

Relatório semanal gerado.

Ambiente: dev
Data de referência: 2026-11-09
Janela: 2026-11-02 a 2026-11-08

Média das notas: 5,00

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

Bucket: feedback-relatorios-<conta>-dev
Chave: relatorios/2026-11-09.txt
Localização: s3://feedback-relatorios-<conta>-dev/relatorios/2026-11-09.txt

O bucket é privado. Acesse com uma credencial da conta.
```

Na semana vazia, a linha da média é `Média das notas: sem avaliações na janela`, e as 3 urgências e os 7 dias aparecem em zero.

**O que fica de fora é garantido por construção.** `AvisoRelatorio` recebe só `ResumoSemanal` e a localização. `ResumoSemanal` tem data de referência, janela, média, totais por urgência e totais por dia, sem a lista de avaliações, que fica em `RelatorioSemanal`. Sem acesso à lista, o aviso não tem como incluir descrição nem nota individual. Os testes comparam o texto completo, o que cobre também a nota, já que dígitos soltos apareceriam nas datas e não servem como busca. A média usa a mesma formatação da D6.

Alternativa descartada: `AvisoRelatorio` receber `RelatorioSemanal` inteiro. Funcionaria, mas a garantia dependeria só de teste.

**Por que não URL pré-assinada:**
- **Validade imprevisível:** [com credencial temporária, a URL expira junto com ela, mesmo com prazo maior](https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-presigned-url.html). A função roda com credencial temporária da role, então o prazo real não é controlado pelo código. As páginas consultadas não informam essa duração para role de execução com SnapStart, e o link poderia morrer antes de o e-mail ser lido.
- **Portador:** a mesma página diz que URL pré-assinada funciona como token de portador. O relatório tem as descrições dos alunos e ficaria acessível a quem receber o e-mail encaminhado, o que conflita com "ele não torna o relatório público" da proposta.
- **Privilégio mínimo:** a role precisaria de `s3:GetObject` só para assinar.

No vídeo, Gabriel abre o objeto pelo console do S3.

Link de console fica de fora: o formato da URL do console não encontrei documentado como contrato.

Alternativa descartada: URL pré-assinada com 7 dias de prazo. Ver acima.

### D8. Agendamento e repetições
```yaml
ScheduleExpression: cron(0 8 ? * MON *)
ScheduleExpressionTimezone: America/Sao_Paulo
FlexibleTimeWindow:
  Mode: "OFF"
RetryPolicy:
  MaximumRetryAttempts: 3
  MaximumEventAgeInSeconds: 3600
```

A sintaxe vem de [tipos de agendamento](https://docs.aws.amazon.com/scheduler/latest/UserGuide/schedule-types.html), que usa o banco de fusos IANA e tem precisão de 60 segundos. `Input` é `{}`, então a referência é o dia da execução, igual ao acionamento manual sem parâmetro.

**São duas camadas de repetição:**

1. **Scheduler, ao entregar para o Lambda.** `RetryPolicy` cobre falha na entrega, com [até 185 tentativas e 86.400 segundos](https://docs.aws.amazon.com/scheduler/latest/APIReference/API_RetryPolicy.html). A idade de 1 hora garante que a repetição ainda cai na própria segunda, então a referência não muda. Sem isso, uma entrega depois da meia-noite geraria o relatório da janela errada.
2. **Lambda, ao executar.** O [Scheduler invoca de forma assíncrona](https://docs.aws.amazon.com/lambda/latest/dg/with-eventbridge-scheduler.html), e erro da função repete [mais duas vezes, com 1 e 2 minutos de espera](https://docs.aws.amazon.com/lambda/latest/dg/invocation-async-error-handling.html). Isso é o padrão e fica como está.

**Efeito da repetição, seguro pela spec:** cada tentativa relê e regrava a mesma chave. Se a publicação falhou, a repetição seguinte publica o aviso. A entrega assíncrona pode raramente repetir mesmo em sucesso, segundo a mesma página, e isso gera um segundo aviso igual, o que é aceitável.

`EventInvokeConfig` não é declarado: alterá-lo exigiria `lambda:PutFunctionEventInvokeConfig`, que o bootstrap não concede, e o padrão serve.

A documentação não permitiu confirmar o que o `RetryPolicy` considera falha de entrega: a página de [retentativas do Scheduler](https://docs.aws.amazon.com/scheduler/latest/UserGuide/configuring-schedule-retry.html) não abriu na consulta. Os valores são declarados explicitamente, então a decisão não depende do padrão.

Alternativas descartadas:
- Fila morta do Scheduler. A [propriedade do SAM](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-property-function-schedulev2.html) só aceita SQS, e o bootstrap não tem SQS. Falhas ficam em log e, no futuro, nos alarmes.
- Passar `<aws.scheduler.scheduled-time>` no `Input`. Corrigiria a virada de dia de outro jeito, com mais código. A idade de 1 hora resolve sem isso.
- `MaximumRetryAttempts: 0`. Perderia a execução da semana em um erro transitório de entrega.

### D9. Ordem dos efeitos
Cada passo só roda se o anterior tiver sucesso, e qualquer exceção sobe para o handler, que registra em ERROR e relança:

1. Validar a data de referência
2. Ler as 7 partições
3. Consolidar e formatar, sem efeito externo
4. Gravar no S3
5. Publicar o aviso

Isso entrega os cenários de falha: rejeição sem efeito nenhum, leitura sem gravação e sem aviso, gravação sem aviso, publicação com relatório guardado. A execução termina como falha nos três últimos, e a reexecução corrige, porque a chave é a mesma.

Domínio: `JanelaApuracao`, `DataReferencia`, `ConsolidadorRelatorio`, `ResumoSemanal`, `RelatorioSemanal`, `FormatadorRelatorio` e `AvisoRelatorio`. Os totais trazem sempre os 7 dias e as 3 urgências, inclusive com zero.

Alternativa descartada: publicar antes de gravar. O aviso poderia apontar para um objeto que não existe.

### D10. Logs e SnapStart
Valem `add-registro-avaliacao` D7 e D9. Em logs vão data de referência, quantidade total, totais por urgência, bucket e chave. Nada de descrição nem nota individual, em nível nenhum. A média também fica fora dos logs, porque com uma única avaliação na janela ela revelaria a nota. No aviso ela aparece por decisão do Gabriel, já que o destinatário são os administradores (ver Risks).

Sem `logError`, o handler captura, registra e relança (`add-registro-avaliacao` D6). A exceção relançada leva mensagem fixa, sem encadear a causa, porque o runtime escreve no log o que recebe, e a resposta da invocação manual também. Clientes `static final`, cadeia padrão de credenciais e data calculada dentro do handler.

### D11. IAM
**Função.** Role gerada, logical ID `GerarRelatorioSemanalRole`, com boundary:

| Origem | Ações | Recurso |
|---|---|---|
| Inline | `dynamodb:Query` | `<tabela>/index/por-data` |
| Inline | `s3:PutObject` | `<bucket>/relatorios/*` |
| Inline | `sns:Publish` | ARN do tópico |
| Inline | `kms:Decrypt` e `kms:GenerateDataKey*` | `key/*` com `kms:ViaService = sns.<região>.amazonaws.com` |
| `AWSLambdaBasicExecutionRole` | `logs:CreateLogGroup`, `logs:CreateLogStream` e `logs:PutLogEvents` | limitado pelo boundary |
| `AWSXrayWriteOnlyAccess` | as cinco de `xray` listadas em `add-registro-avaliacao` D13 | `*` |

Sem `dynamodb:GetItem` nem `Scan`, sem `s3:GetObject` e sem `s3:ListBucket`. A criptografia da tabela não exige KMS na role (`add-registro-avaliacao` D13), e a do bucket é SSE-S3. A chave do tópico segue `add-notificacao-critica` D8.

**Agendamento.** Role gerada, logical ID `GerarRelatorioSemanalAgendaRole`, com `lambda:InvokeFunction` no alias. Verificado localmente no template gerado: o alvo do `AWS::Scheduler::Schedule` e o recurso da política são `GerarRelatorioSemanalAliaslive`.

**Boundary da role do agendamento: confirmado, é preciso declarar.** `Globals.Function.PermissionsBoundary` não alcança essa role. A propriedade certa é [`PermissionsBoundary` no próprio evento `ScheduleV2`](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-property-function-schedulev2.html). Verificado localmente: com ela, a role gerada traz o boundary. Sem ela, `iam:CreateRole` seria negado.

**Conferência contra o bootstrap:**
- Nome da role: o evento se chama `Agenda`, curto de propósito. `feedback-prod-GerarRelatorioSemanalAgendaRole-<aleatório>` tem cerca de 59 caracteres, dentro do limite de 64 sem truncar, e casa com `feedback-<amb>-*`. Com `AgendaSemanal`, passaria do limite e dependeria de como o CloudFormation trunca, o que [a documentação não detalha](https://docs.aws.amazon.com/AWSCloudFormation/latest/TemplateReference/aws-properties-name.html) (04/11/2026).
- `iam:PassRole` para `scheduler.amazonaws.com` está liberado no bootstrap.
- Agendamento: `feedback-relatorio-semanal-<amb>` no grupo padrão, casa com `schedule/default/feedback-*-<amb>`. Sem o `Name`, [o SAM usaria o logical ID](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-property-function-schedulev2.html), que não casa com o padrão.
- Função `feedback-gerar-relatorio-semanal-<amb>` casa com `feedback-*-<amb>`, e `lambda:InvokeFunction` no boundary cobre `:*`, logo o alias.
- Bucket `feedback-relatorios-<conta>-<amb>` casa com `feedback-*-<amb>`, e o índice com `table/feedback-*-<amb>/index/*`.
- `iam:AttachRolePolicy` só recebe políticas da lista. A role do agendamento só usa política inline, coberta por `iam:PutRolePolicy` com boundary.

Nenhuma alteração no bootstrap é necessária.

Alternativa descartada: role própria do agendamento em `RoleArn`. Mais um recurso para repetir o que o SAM gera.

## Risks / Trade-offs

- **Consistência eventual no vídeo:** ver D4.
- **`sam delete` com bucket cheio:** este change passa a gravar objetos, e o CloudFormation [só exclui bucket vazio](https://docs.aws.amazon.com/AWSCloudFormation/latest/TemplateReference/aws-resource-s3-bucket.html) (04/11/2026). Mitigação: esvaziar antes (tarefa 6.1), como já indicado em `add-notificacao-critica`.
- **Média com uma única avaliação:** se a janela tiver só uma avaliação, a média do aviso é igual à nota dela. É inerente a mostrar a média, e o e-mail vai só para os administradores. Aceito.
- **Falha do item inválido bloqueia o relatório:** um item escrito à mão sem campo impede todos os relatórios daquela janela até ser corrigido. Trade-off aceito: é melhor falhar visivelmente do que publicar número errado. O log aponta o `id`.
- **Volume:** todas as avaliações cabem em memória. Com 512 MB e `Timeout` de 120 segundos é folgado para o projeto, mas não para um volume muito maior. Aceito pelo escopo.
- **Relatório sem expiração:** contém descrições de alunos e fica no bucket até ser removido. Aceito, histórico e ciclo de vida estão fora do escopo.
- **Bucket no e-mail:** o nome inclui o ID da conta. Vai só para o e-mail do administrador, nunca para o repositório público.
- **Agenda em hml e prod:** o agendamento existe em todos os ambientes, então cada stack ativa gera relatório e e-mail às segundas. Aceito, porque hml e prod podem ser removidos depois do vídeo.

## Open Questions

Nenhuma.

## Alterações no template.yaml

Dependência: integrar depois de `add-registro-avaliacao`, e por último na ordem do backlog. Não altera `Globals` nem blocos de outro change, e os trechos entram no fim de cada seção, então não há conflito com `add-notificacao-critica`.

**1. Acrescenta ao fim de `Resources`**, depois de `NotificarCritica`:

```yaml
  LogGerarRelatorioSemanal:
    Type: AWS::Logs::LogGroup
    Properties:
      LogGroupName: !Sub /aws/lambda/feedback-gerar-relatorio-semanal-${Stage}
      RetentionInDays: 14

  GerarRelatorioSemanal:
    Type: AWS::Serverless::Function
    DependsOn: LogGerarRelatorioSemanal
    Properties:
      FunctionName: !Sub feedback-gerar-relatorio-semanal-${Stage}
      CodeUri: functions/gerar-relatorio-semanal
      Handler: br.com.fiap.feedback.relatorio.GerarRelatorioSemanalHandler::handleRequest
      Timeout: 120
      AutoPublishAliasAllProperties: true
      Environment:
        Variables:
          POWERTOOLS_SERVICE_NAME: gerar-relatorio-semanal
          TABELA_AVALIACOES: !Ref TabelaAvaliacoes
          BUCKET_RELATORIOS: !Ref BucketRelatorios
          TOPICO_ADMINS_ARN: !Ref TopicoAdmins
      Policies:
        - Version: "2012-10-17"
          Statement:
            - Sid: ConsultarIndicePorData
              Effect: Allow
              Action: dynamodb:Query
              Resource: !Sub ${TabelaAvaliacoes.Arn}/index/por-data
            - Sid: GravarRelatorio
              Effect: Allow
              Action: s3:PutObject
              Resource: !Sub ${BucketRelatorios.Arn}/relatorios/*
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
      Events:
        Agenda:
          Type: ScheduleV2
          Properties:
            Name: !Sub feedback-relatorio-semanal-${Stage}
            Description: Relatório semanal, toda segunda às 08:00 em São Paulo
            ScheduleExpression: cron(0 8 ? * MON *)
            ScheduleExpressionTimezone: America/Sao_Paulo
            State: ENABLED
            Input: '{}'
            FlexibleTimeWindow:
              Mode: "OFF"
            RetryPolicy:
              MaximumRetryAttempts: 3
              MaximumEventAgeInSeconds: 3600
            # Globals não alcança esta role. Sem o boundary aqui, iam:CreateRole é negado.
            PermissionsBoundary: !Sub arn:${AWS::Partition}:iam::${AWS::AccountId}:policy/feedback-limite-funcoes-${Stage}
```

**2. Acrescenta ao fim de `Outputs`:**

```yaml
  GerarRelatorioSemanalNome:
    Description: Nome da função de relatório
    Value: !Ref GerarRelatorioSemanal
  GerarRelatorioSemanalAliasArn:
    Description: ARN do alias live da função de relatório. O acionamento manual usa este alias
    Value: !Ref GerarRelatorioSemanal.Alias
```

IDs lógicos novos: `LogGerarRelatorioSemanal` e `GerarRelatorioSemanal`. O SAM também gera `GerarRelatorioSemanalRole`, `GerarRelatorioSemanalAliaslive`, `GerarRelatorioSemanalVersion<hash>`, `GerarRelatorioSemanalAgenda` e `GerarRelatorioSemanalAgendaRole`.

## Desvios do project.md

Nenhum.
