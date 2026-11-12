# Roteiro do vídeo de entrega

Roteiro da gravação em produção: stack `feedback-prod`, região `sa-east-1`, duração estimada de 11 minutos e 30 segundos. A gravação é na quinta, 12/11/2026. O vídeo mostra a aplicação funcionando, as três funções ativas na AWS e as configurações de nuvem, segurança, pipeline e monitoramento.

Estado em 11/11/2026 (UTC): o roteiro não foi ensaiado em prod. Em prod, só as leituras da preparação têm registro, de 11/11/2026 (UTC), na versão 1.0.0. Os passos do vídeo repetem o que o QA fez em dev e hml, e cada bloco de comando diz onde está o registro.

## Blocos e tempos

| Bloco | Início | Duração | Conteúdo | Critério da FIAP |
|---|---|---|---|---|
| 1. Abertura | 0:00 | 1 min | Problema, modelo de cloud e diagrama | 1 e 4 |
| 2. Aplicação funcionando | 1:00 | 4 min | Registro com 201 e 400, aviso crítico por e-mail, relatório manual, objeto no S3 e aviso do relatório | 2 |
| 3. Funções ativas | 5:00 | 2 min | As três funções, o gatilho de cada uma, alias `live` com SnapStart | 5 |
| 4. Configurações | 7:00 | 3 min 30 s | Template, pipeline com aprovação em prod, segurança, dashboard e um alarme disparado | 4 e 5 |
| 5. Encerramento | 10:30 | 1 min | Repositório, documentação, relatório, changes arquivados e agentes | 3 |
| **Total** | | **11 min 30 s** | | |

## Cuidados com o que aparece na tela

Desfoque ou corte na edição:

- **ID da conta:** no menu da conta do console da AWS, em todo ARN (página da função, alias, gatilhos, agendamento, alarme, role), nas variáveis de ambiente das funções (`TOPICO_ADMINS_ARN` e `BUCKET_RELATORIOS` trazem o ID), no nome do bucket `feedback-relatorios-<ID_DA_CONTA>-prod` em qualquer tela e na barra de endereço do navegador ao abrir um objeto do S3.
- **E-mails:** o endereço do assinante no cabeçalho, no corpo, se aparecer, e no rodapé de cancelamento do SNS, que traz também o ID da conta. No aviso do relatório, as linhas `Bucket:` e `Localização:`. No e-mail de alarme, a conta e o ARN do alarme.
- **Identificador da API:** na URL `https://<ID_DA_API>.execute-api.sa-east-1.amazonaws.com`, onde ela aparecer: gatilho da função no console do Lambda, console do API Gateway, variáveis do Postman, console do Postman e o valor que surge ao passar o mouse sobre `{{baseUrl}}`.
- **GitHub:** a página de configuração do ambiente `prod` mostra o valor da variável `AWS_ROLE_ARN`, que contém o ID da conta. Não abra ou desfoque.
- **Data rejeitada:** se aparecer uma resposta de data inválida do relatório, mostre só a mensagem. O `resposta.json` dessa chamada traz também `errorType` e `stackTrace`.

As descrições usadas são as da coleção Postman (`Aula muito boa` e `Aula ruim`), neutras e sem nome de pessoa. Não digite outra descrição durante a gravação.

## Ensaio em hml

Antes da gravação, faça o roteiro inteiro uma vez em hml, com os mesmos passos e sem gravar a tela. Os POSTs do ensaio gravam na tabela `feedback-avaliacoes-hml`, e o relatório e o alarme rodam na stack `feedback-hml`. Nada disso afeta prod: os ambientes não compartilham tabela, tópico, bucket, alarme nem dashboard ([QA de add-fundacao-projeto](../openspec/changes/archive/2026-11-11-add-fundacao-projeto/qa.md), seção "Release 1.0.0 em hml", item 6).

**Como adaptar os passos.** Use os blocos de comando deste roteiro trocando `feedback-prod` por `feedback-hml` e o sufixo `-prod` por `-hml` em todos os nomes: funções, tabela, tópicos, alarmes, agendamento e dashboard. No Postman, ponha no `baseUrl` a URL de hml, lida no output `ApiAvaliacoesUrl` de `feedback-hml`. A data de referência do ensaio é o dia seguinte ao do ensaio, não `2026-11-13`, a menos que o ensaio seja no próprio dia 12/11.

**O que muda em hml:**

- A release 1.0.1 passa por hml antes de prod, pela branch de release. Confira o SnapStart nas três funções de hml depois desse deploy.
- A tabela de hml não começa vazia. A leitura de 11/11/2026 (UTC) encontrou `Count` 2: as duas avaliações de QA de 07/11, com notas 2 e 9 ([QA de add-documentacao-entrega](../openspec/changes/archive/2026-11-11-add-documentacao-entrega/qa.md), seção "Leitura de dev e hml (tarefa 2.1)"). Com referência em 12/11 ou 13/11, a janela inclui 07/11, e o relatório do ensaio traz 4 avaliações: média 5,50, `CRITICA: 2`, `MEDIA: 0`, `BAIXA: 2`, com 2 no dia 07/11 e 2 no dia do ensaio. Em prod, o esperado continua o do bloco 2.
- O bucket de hml já tem `relatorios/2026-11-08.txt` e `relatorios/2026-11-09.txt` (mesma leitura).
- O pipeline de hml não tem aprovação manual. No passo 4.2, só abra a execução do Deploy na branch de release.

**E-mails de hml.** O ensaio manda e-mails reais pelos tópicos `feedback-admins-hml` e `feedback-alarmes-hml`: o aviso crítico, o aviso do relatório e o alarme. O aviso crítico tem o mesmo assunto em hml e em prod, `Avaliação crítica registrada`, e não diz o ambiente. Antes de gravar, tire os e-mails do ensaio da pasta que vai aparecer no vídeo.

**Depois do ensaio:** confira que o `baseUrl` do Postman voltou para a URL de prod e que o terminal usa os nomes com `-prod`. Os itens do ensaio ficam na tabela de hml até a remoção da stack, depois da entrega.

## Preparação

Nada da preparação aparece no vídeo. Nenhum passo grava item na tabela `feedback-avaliacoes-prod`.

### Quando gravar

- Depois de a release 1.0.1 chegar a prod e da conferência de SnapStart, mais abaixo. O deploy publica uma versão nova das três funções, e o alias `live` passa a apontar para ela.
- Longe da meia-noite no horário de São Paulo. A data de envio da avaliação e a data de referência do relatório são do dia em São Paulo, e uma virada de dia no meio da gravação tira as avaliações da janela.
- A data de referência do relatório é a de amanhã. A janela são os 7 dias anteriores a ela, com a própria referência fora, e inclui o dia da gravação:

| Dia da gravação | Data de referência | Janela do relatório |
|---|---|---|
| 12/11/2026, quinta | `2026-11-13` | `2026-11-06` a `2026-11-12` |

Se a gravação mudar de dia, troque `2026-11-13` nos blocos 2.6 a 2.8 e em "Depois da gravação" pela data do dia seguinte.

### Conferências de leitura

Rode num terminal aberto numa pasta fora do repositório, com a sessão do perfil renovada por `aws sso login --profile tc4-admin`. Estes comandos imprimem ARNs e a URL da API. Feche esse terminal antes de gravar e abra outro, limpo.

**1. SnapStart nas três funções**, logo depois do deploy da release 1.0.1. Esperado: `State` igual a `Active`, `OptimizationStatus` igual a `On` e `MemorySize` igual a 1024 nas três. `Version` é o número da versão nova.

Registro: rodou em dev, com o sufixo `-dev`, em 06/11/2026 ([QA de add-observabilidade-seguranca](../openspec/changes/archive/2026-11-11-add-observabilidade-seguranca/qa.md), seção 6.4), e em hml, com `-hml`, em 07/11/2026 ([QA de add-fundacao-projeto](../openspec/changes/archive/2026-11-11-add-fundacao-projeto/qa.md), seção "Release 1.0.0 em hml", item 1). Em prod, rodou em 11/11/2026 (UTC), na versão 1 da release 1.0.0 (mesmo QA de add-observabilidade-seguranca, seção "Prod, release 1.0.0 (leitura)", Funções). Repita depois da 1.0.1, que publica versão nova.

```bash
aws lambda get-function-configuration --function-name feedback-registrar-avaliacao-prod:live --region sa-east-1 --profile tc4-admin --query "{Version:Version,State:State,OptimizationStatus:SnapStart.OptimizationStatus,MemorySize:MemorySize}"
aws lambda get-function-configuration --function-name feedback-notificar-critica-prod:live --region sa-east-1 --profile tc4-admin --query "{Version:Version,State:State,OptimizationStatus:SnapStart.OptimizationStatus,MemorySize:MemorySize}"
aws lambda get-function-configuration --function-name feedback-gerar-relatorio-semanal-prod:live --region sa-east-1 --profile tc4-admin --query "{Version:Version,State:State,OptimizationStatus:SnapStart.OptimizationStatus,MemorySize:MemorySize}"
```

Se alguma função vier com `State` diferente de `Active`, espere alguns minutos e repita antes de gravar. A publicação da versão roda o aquecimento dos clientes do SDK, que só faz leituras: em dev, o deploy não gravou item, não publicou mensagem e não criou objeto (mesmo QA, seção 6.5).

**2. Assinaturas confirmadas** nos tópicos `feedback-admins-prod` e `feedback-alarmes-prod`. Esperado: `1` e `0` (confirmadas e pendentes) nos dois. Com `0` e `1`, o e-mail não chega: confirme pelo link do e-mail original de confirmação antes de gravar.

Registro: rodou em prod em 11/11/2026 (UTC), com as duas assinaturas confirmadas (1, 0, 0) ([QA de add-observabilidade-seguranca](../openspec/changes/archive/2026-11-11-add-observabilidade-seguranca/qa.md), seção "Prod, release 1.0.0 (leitura)", item "8.1. Assinaturas de prod: confirmadas"). Antes, rodou em dev (mesmo QA, seção 6.6) e em hml (seção 8.1).

```bash
ADMINS=$(aws cloudformation describe-stacks --stack-name feedback-prod \
  --query "Stacks[0].Outputs[?OutputKey=='TopicoAdminsArn'].OutputValue" \
  --output text --region sa-east-1 --profile tc4-admin)
ALARMES=$(aws cloudformation describe-stacks --stack-name feedback-prod \
  --query "Stacks[0].Outputs[?OutputKey=='TopicoAlarmesArn'].OutputValue" \
  --output text --region sa-east-1 --profile tc4-admin)

aws sns get-topic-attributes --topic-arn "$ADMINS" \
  --query "Attributes.[SubscriptionsConfirmed,SubscriptionsPending]" \
  --output text --region sa-east-1 --profile tc4-admin
aws sns get-topic-attributes --topic-arn "$ALARMES" \
  --query "Attributes.[SubscriptionsConfirmed,SubscriptionsPending]" \
  --output text --region sa-east-1 --profile tc4-admin
```

**3. Alarmes em OK.** Esperado: os três com `OK`.

Registro: rodou em dev, com o sufixo `-dev`, em 06/11/2026 ([QA de add-observabilidade-seguranca](../openspec/changes/archive/2026-11-11-add-observabilidade-seguranca/qa.md), seção 6.7), e em hml em 07/11/2026 ([QA de add-fundacao-projeto](../openspec/changes/archive/2026-11-11-add-fundacao-projeto/qa.md), seção "Release 1.0.0 em hml", item 1). Em prod, a leitura de 11/11/2026 (UTC) trouxe os três em OK (mesmo QA de add-observabilidade-seguranca, seção "Prod, release 1.0.0 (leitura)", Alarmes de prod).

```bash
aws cloudwatch describe-alarms \
  --alarm-names feedback-falha-registrar-avaliacao-prod feedback-falha-notificar-critica-prod feedback-falha-gerar-relatorio-semanal-prod \
  --query "MetricAlarms[].[AlarmName,StateValue]" \
  --output table \
  --region sa-east-1 --profile tc4-admin
```

**4. Tabela vazia.** Esperado: `Count` igual a 0. A contagem usa `scan --select COUNT`, porque o `ItemCount` do `describe-table` só é atualizado a cada seis horas. O `scan` só lê.

Registro: rodou em prod em 11/11/2026 (UTC), com `Count` 0 e `ScannedCount` 0 ([QA de add-observabilidade-seguranca](../openspec/changes/archive/2026-11-11-add-observabilidade-seguranca/qa.md), seção "Prod, release 1.0.0 (leitura)", Tabela e bucket de prod). Repita no dia da gravação.

```bash
aws dynamodb scan --table-name feedback-avaliacoes-prod --select COUNT --region sa-east-1 --profile tc4-admin
```

Se o `Count` não for 0, não grave: avise antes, porque o relatório do vídeo deixaria de bater com os números deste roteiro.

**5. URL da API para o Postman.** O valor tem o formato `https://<ID_DA_API>.execute-api.sa-east-1.amazonaws.com/avaliacao`.

Registro: a URL do output `ApiAvaliacoesUrl` foi usada em dev em 05/11/2026 ([QA de add-registro-avaliacao](../openspec/changes/archive/2026-11-11-add-registro-avaliacao/qa.md), seção 6.6) e lida em hml em 07/11/2026 ([QA de add-fundacao-projeto](../openspec/changes/archive/2026-11-11-add-fundacao-projeto/qa.md), seção "Release 1.0.0 em hml", item 1). Em prod, não há registro dessa leitura.

```bash
aws cloudformation describe-stacks \
  --stack-name feedback-prod \
  --query "Stacks[0].Outputs[?OutputKey=='ApiAvaliacoesUrl'].OutputValue" \
  --output text \
  --region sa-east-1 --profile tc4-admin
```

### Postman

1. Importe `docs/postman/feedback-serverless.postman_collection.json`.
2. Na variável de coleção `baseUrl`, ponha a URL do passo 5 **sem o `/avaliacao` do final**: `https://<ID_DA_API>.execute-api.sa-east-1.amazonaws.com`. Com a URL completa, a chamada vai para `/avaliacao/avaliacao`, que não existe.
3. Com o `baseUrl` de prod, não envie nenhuma requisição de `Sucesso` antes de gravar: cada uma grava um item em prod.
4. Deixe abertas, nesta ordem, as três requisições do bloco 2: `Sucesso` > `Avaliação válida, nota 9, urgência BAIXA`; `Dois campos inválidos` > `Descrição vazia e nota 11`; `Sucesso` > `Nota 2, urgência CRITICA`.
5. Feche a aba de variáveis e o console do Postman.
6. Não exporte a coleção de volta para `docs/postman/`. O arquivo versionado tem `https://exemplo.invalid`, e assim deve ficar.

### Aquecimento opcional

Não é necessário: a versão recém-publicada já tem o snapshot com os clientes aquecidos, e em dev a primeira invocação depois da restauração durou de 1,0 a 1,9 s ([QA de add-observabilidade-seguranca](../openspec/changes/archive/2026-11-11-add-observabilidade-seguranca/qa.md), seção 6.15). Se quiser aquecer mesmo assim, use só chamadas que não gravam:

- No Postman, uma requisição de rejeição, como `Rejeição de nota` > `Nota acima do máximo` (400).
- No relatório, a data inexistente `2026-02-30`, que é rejeitada antes de ler, gravar ou avisar e não dispara alarme. Não mostre o arquivo de saída.

Registro: o mesmo acionamento com `2026-02-30` rodou em dev em 06/11/2026, com o sufixo `-dev` ([QA de add-relatorio-semanal](../openspec/changes/archive/2026-11-11-add-relatorio-semanal/qa.md), seção 5.5, item c, e [QA de add-observabilidade-seguranca](../openspec/changes/archive/2026-11-11-add-observabilidade-seguranca/qa.md), seção 6.8 a 6.10, janela C). Em prod, ainda não.

```bash
aws lambda invoke \
  --function-name feedback-gerar-relatorio-semanal-prod:live \
  --region sa-east-1 --profile tc4-admin \
  --cli-binary-format raw-in-base64-out \
  --cli-read-timeout 150 \
  --payload '{"dataReferencia":"2026-02-30"}' \
  aquecimento.json
```

O aquecimento faz o dashboard de prod mostrar essas chamadas também. Sem ele, o dashboard só terá dados depois das chamadas do vídeo.

### Abas e janelas

- GitHub, no repositório: o `README.md` na branch padrão, com o diagrama; a execução do workflow Deploy da release 1.0.1 na `main`; a pasta `docs/`.
- Editor com `template.yaml` e `infra/bootstrap/github-oidc.yaml`.
- Console da AWS em `sa-east-1`: Lambda, com a lista filtrada por `prod`; EventBridge, em Scheduler, com o agendamento `feedback-relatorio-semanal-prod`; S3, no bucket de prod, prefixo `relatorios/`; CloudWatch, no dashboard `feedback-painel-prod`. Opcional: IAM, na role da função de registro de prod.
- Caixa de e-mail numa pasta ou busca que mostre só as mensagens da AWS Notifications de hoje, sem outras mensagens visíveis.
- Terminal novo, com fonte grande, sem histórico na tela, aberto na mesma pasta fora do repositório.

### Checklist antes de apertar gravar

- [ ] Release 1.0.1 em prod, com as três funções em `Active` e `On` no alias `live`
- [ ] Assinaturas de `feedback-admins-prod` e `feedback-alarmes-prod` confirmadas
- [ ] Os três alarmes de prod em OK
- [ ] `feedback-avaliacoes-prod` com `Count` 0
- [ ] Postman com o `baseUrl` de prod, sem `/avaliacao`, e as três requisições abertas
- [ ] Abas abertas e terminal limpo
- [ ] Ensaio em hml feito, e-mails do ensaio fora da pasta do vídeo
- [ ] Gravação em 12/11/2026, com a referência `2026-11-13` no payload

## Bloco 1. Abertura (0:00 a 1:00)

**Mostre:** o `README.md` no GitHub, na seção da arquitetura, com o diagrama. Aponte os três fluxos enquanto fala.

**Fale:**
"Este é o Tech Challenge da fase 4. A plataforma recebe avaliações de aulas, com uma descrição e uma nota de 0 a 10. Nota de 0 a 3 é crítica e gera um e-mail na hora para os administradores. Toda segunda, às 8 horas de São Paulo, sai um relatório semanal.
O modelo é serverless na AWS, na região de São Paulo. Uma API HTTP do API Gateway chama a primeira função Lambda, que grava no DynamoDB. O Stream da tabela aciona a segunda função só para avaliações críticas, e ela publica no SNS. O EventBridge Scheduler aciona a terceira função, que grava o relatório no S3 e avisa pelo SNS. As três funções são em Java 25, com SnapStart, e mandam logs, métricas e traces para o CloudWatch e o X-Ray.
Tudo é criado por um template do SAM e implantado pelo GitHub Actions. Cada branch publica num ambiente: develop em dev, release em hml e main em prod. O que aparece a seguir roda em prod."

## Bloco 2. Aplicação funcionando (1:00 a 5:00)

### 2.1 Registro com nota 9 (25 s)

**Mostre:** no Postman, `Avaliação válida, nota 9, urgência BAIXA`. O corpo `{"descricao": "Aula muito boa", "nota": 9}`, o envio, o status 201, a resposta `{"id":"<UUID>","urgencia":"BAIXA"}` e a aba de resultados dos testes. Não passe o mouse sobre `{{baseUrl}}`.

**Fale:** "Registro uma avaliação com nota 9. A função valida o corpo, calcula a urgência pela nota e grava no DynamoDB. O id e a data de envio são da função, não do cliente."

### 2.2 Entrada inválida (25 s)

**Mostre:** `Descrição vazia e nota 11`. Status 400 e a resposta `{"erro":"ENTRADA_INVALIDA","mensagem":"Um ou mais campos são inválidos.","campos":["descricao","nota"]}`.

**Fale:** "Com descrição vazia e nota 11, a resposta é 400 com o nome dos campos reprovados, sem os valores. Nada é gravado. A rota também tem limite de 10 requisições por segundo, com rajada de 20."

### 2.3 Registro com nota 2 (20 s)

**Mostre:** `Nota 2, urgência CRITICA`. O corpo `{"descricao": "Aula ruim", "nota": 2}`, o status 201 e `"urgencia":"CRITICA"`.

**Fale:** "Nota 2 é crítica. A gravação gera um evento no Stream da tabela, e o filtro do gatilho só deixa passar a criação de item crítico. A nota 9 de antes não acionou a notificação."

### 2.4 Aviso crítico por e-mail (40 s)

O aviso chega em segundos: em hml, foi publicado cerca de 4 s depois do registro.

**Mostre:** a caixa de e-mail e a mensagem com o assunto `Avaliação crítica registrada`. No corpo, `Uma avaliação crítica foi registrada.`, `Urgência: CRITICA`, `Data de envio: 2026-11-12` e `Descrição: Aula ruim`. Desfoque o destinatário e o rodapé.

**Fale:** "O e-mail traz o que o enunciado pede: descrição, urgência e data de envio. A função de registro não depende do aviso. Se o SNS falhar, a gravação continua valendo, e o Lambda tenta publicar de novo."

### 2.5 Pausa antes do relatório (15 s)

Espere pelo menos 15 s depois do último registro, como o QA fez em dev. O índice `por-data` é atualizado de forma eventualmente consistente.

**Fale:** "O relatório cobre os 7 dias anteriores à data de referência, sem contar a própria data. Para as avaliações de hoje entrarem, aciono com a data de amanhã."

### 2.6 Relatório manual (45 s)

**Mostre:** o terminal.

Registro: rodou em dev, com o sufixo `-dev`, em 06/11/2026 ([QA de add-relatorio-semanal](../openspec/changes/archive/2026-11-11-add-relatorio-semanal/qa.md), seção 5.5, item b), e em hml, com `-hml`, em 07/11/2026 ([QA de add-fundacao-projeto](../openspec/changes/archive/2026-11-11-add-fundacao-projeto/qa.md), seção "Release 1.0.0 em hml", item 3). Em prod, ainda não.

```bash
aws lambda invoke \
  --function-name feedback-gerar-relatorio-semanal-prod:live \
  --region sa-east-1 --profile tc4-admin \
  --cli-binary-format raw-in-base64-out \
  --cli-read-timeout 150 \
  --payload '{"dataReferencia":"2026-11-13"}' \
  resposta.json
cat resposta.json
```

Esperado: `StatusCode` 200, sem `FunctionError`, e `ExecutedVersion` com o número da versão publicada. Em `resposta.json`, em ordem não garantida, `"quantidadeAvaliacoes":2`, `"dataReferencia":"2026-11-13"` e `"chave":"relatorios/2026-11-13.txt"`. A resposta não traz o bucket.

**Fale:** "Aciono a função pelo alias live, o mesmo que o agendamento usa. A versão executada é uma versão publicada, que é onde o SnapStart vale. Duas avaliações na janela: a de 400 não foi gravada."

### 2.7 Objeto no S3 (40 s)

**Mostre:** o console do S3, no bucket de prod, prefixo `relatorios/`. A listagem tem dois objetos: `relatorios/2026-11-09.txt`, de 351 bytes, gravado pela execução agendada de 09/11 com a semana vazia, e `relatorios/2026-11-13.txt`, que acabou de ser criado. Antes do vídeo, o de 09/11 é o único objeto do bucket ([QA de add-observabilidade-seguranca](../openspec/changes/archive/2026-11-11-add-observabilidade-seguranca/qa.md), seção "Prod, release 1.0.0 (leitura)", Tabela e bucket de prod). Abra o novo e mostre:

- `Ambiente: prod`, `Data de referência: 2026-11-13` e `Janela: 2026-11-06 a 2026-11-12`
- `Média das notas: 5,50`
- `CRITICA: 1`, `MEDIA: 0`, `BAIXA: 1`
- os 7 dias da janela, de `2026-11-06` a `2026-11-12`, com 2 em `2026-11-12` e 0 nos outros
- `Avaliações (2)`, com `Aula muito boa` em `BAIXA` e `Aula ruim` em `CRITICA`, cada uma com `Data de envio: 2026-11-12`

Desfoque o nome do bucket e a barra de endereço da aba do objeto.

**Fale:** "O relatório traz a lista com descrição, urgência e data, a quantidade por dia e por urgência e a média das notas. A nota individual não aparece. O bucket é privado, com criptografia e acesso só por TLS. O arquivo de 9 de novembro é da execução agendada de segunda, que mostro no próximo bloco."

### 2.8 Aviso do relatório (30 s)

**Mostre:** o e-mail com o assunto `Relatório semanal 2026-11-13`. O corpo começa com `Relatório semanal gerado.` e traz ambiente, janela, média e totais. Desfoque as linhas `Bucket:` e `Localização:`, o destinatário e o rodapé.

**Fale:** "O aviso aos administradores tem só os números e onde o arquivo está. As descrições ficam no bucket, que só abre com credencial da conta."

## Bloco 3. Funções ativas (5:00 a 7:00)

### 3.1 As três funções (20 s)

**Mostre:** a lista do Lambda filtrada por `prod`: `feedback-registrar-avaliacao-prod`, `feedback-notificar-critica-prod` e `feedback-gerar-relatorio-semanal-prod`, com o runtime Java 25. Desfoque o menu da conta.

**Fale:** "São três funções, uma por responsabilidade, todas com o sufixo do ambiente."

### 3.2 Registro e o gatilho da API (30 s)

**Mostre:** `feedback-registrar-avaliacao-prod`, aba Aliases, alias `live`. O gatilho é o API Gateway `feedback-api-prod`, rota `POST /avaliacao`. Desfoque o endpoint e o ARN. Em Configuração geral, memória de 1024 MB e tempo limite de 10 s. Não abra as variáveis de ambiente.

O gatilho fica no alias porque o template liga a integração da API ao `live`: em dev, a integração apontava para `:live` e a função sem qualificador não tinha permissão de invocação ([QA de add-registro-avaliacao](../openspec/changes/archive/2026-11-11-add-registro-avaliacao/qa.md), seção 6.4). Se a visão geral não mostrar o gatilho, abra Configuração > Gatilhos no alias.

**Fale:** "A API chama o alias live, e não a última versão editável. Assim toda requisição usa a versão publicada com SnapStart."

### 3.3 Notificação e o Stream (25 s)

**Mostre:** `feedback-notificar-critica-prod`, alias `live`, gatilho DynamoDB na tabela `feedback-avaliacoes-prod`, com o filtro `{"eventName":["INSERT"],"dynamodb":{"NewImage":{"urgencia":{"S":["CRITICA"]}}}}`, lote de 10 e destino de falha no bucket. Desfoque o nome do bucket e os ARNs.

**Fale:** "O filtro descarta, antes de invocar a função, tudo o que não é criação de avaliação crítica. Se a publicação falhar, a função devolve o registro que falhou, o Lambda tenta de novo até 5 vezes ou por até 1 hora, e o que for descartado vai para o bucket."

### 3.4 Relatório e o agendamento (20 s)

**Mostre:** no EventBridge, em Scheduler, o agendamento `feedback-relatorio-semanal-prod`: `cron(0 8 ? * MON *)`, fuso `America/Sao_Paulo`, estado ativado e alvo no alias `live` da função de relatório. Desfoque o ARN do alvo.

**Fale:** "O agendamento roda toda segunda às 8 horas de São Paulo. O arquivo de 9 de novembro que apareceu no bucket é a execução de segunda passada, sem avaliações, porque a tabela de prod estava vazia."

O disparo de 09/11 às 08:00 em São Paulo está em [QA de add-relatorio-semanal](../openspec/changes/archive/2026-11-11-add-relatorio-semanal/qa.md), seção "Execução agendada real, 09/11/2026".

### 3.5 SnapStart no alias (25 s)

**Mostre:** no terminal, os três comandos de "SnapStart nas três funções", da preparação, com `State` `Active`, `OptimizationStatus` `On` e `MemorySize` 1024.

**Fale:** "O SnapStart só vale em versões publicadas. A cada deploy, o SAM publica uma versão e move o alias live para ela. Antes do snapshot, a função faz uma leitura com cada cliente do SDK, e o snapshot já guarda os clientes prontos. Em dev, esse aquecimento, junto com a memória de 1024 MB, tirou a primeira chamada ao DynamoDB e ao SNS de cerca de 4 segundos para menos de 0,3."

## Bloco 4. Configurações (7:00 a 10:30)

### 4.1 Template (45 s)

**Mostre:** `template.yaml` no editor, nesta ordem:

- `Globals`: `java25`, 1024 MB, `Tracing: Active`, `AutoPublishAlias: live`, `SnapStart` em `PublishedVersions` e o `PermissionsBoundary` do ambiente
- `TabelaAvaliacoes`: `PAY_PER_REQUEST`, índice `por-data`, Stream `NEW_IMAGE`, criptografia e `PointInTimeRecoveryEnabled: !If [EhProd, true, false]`
- `BucketRelatorios` e `PoliticaBucketRelatorios`: `AES256`, os quatro bloqueios de acesso público e `NegaAcessoSemTls`
- as `Policies` de `RegistrarAvaliacao`: só `dynamodb:PutItem` e `dynamodb:DescribeTable`, na tabela do ambiente
- `RetentionInDays: !If [EhProd, 30, 14]` num dos grupos de log

**Fale:** "Um único template cria os três ambientes, com o nome do ambiente em cada recurso. Cada função tem só as permissões de que precisa, na tabela, no tópico ou no bucket do próprio ambiente. Prod tem recuperação pontual na tabela e guarda os logs por 30 dias."

### 4.2 Pipeline com aprovação em prod (40 s)

**Mostre:** no GitHub, em Actions, a execução do workflow Deploy da release 1.0.1 na `main`: o job `resolver-ambiente`, que mapeou `main` para `prod`, e o job `deploy` no ambiente `prod`, com o registro da revisão. Se a página da execução não mostrar quem aprovou, mostre em Settings > Environments > `prod` a regra de revisor obrigatório, com `g-andradd`, e desfoque a variável `AWS_ROLE_ARN`.

**Fale:** "Cada pull request roda os testes, valida o template e empacota as funções. Depois do merge, o push publica no ambiente da branch. Em prod, o job espera a minha aprovação. O GitHub não guarda chave da AWS: o job troca um token OIDC por uma credencial temporária da role do ambiente."

### 4.3 Segurança (50 s)

**Mostre:** `infra/bootstrap/github-oidc.yaml` no editor:

- `GitHubActionsNoAmbiente`: a role de prod só aceita token do repositório com `environment:prod`
- `NegaAlterarBootstrap`: o pipeline não altera as próprias roles, as políticas nem o boundary
- `EscreverRolesComLimite`: toda role que o deploy cria precisa do boundary `feedback-limite-funcoes-prod`

Opcional, se o tempo deixar: no IAM, a role da função de registro de prod, com o boundary `feedback-limite-funcoes-prod`. Desfoque os ARNs.

**Fale:** "As roles de deploy e o provedor OIDC ficam fora do pipeline, num template aplicado uma vez com credencial administrativa. Cada role só mexe nos recursos do próprio ambiente, e o boundary limita até onde uma função pode chegar, mesmo que alguém amplie a política dela. Em repouso, a tabela e o tópico dos administradores usam chaves gerenciadas da AWS, e o bucket usa criptografia do S3. O tópico de alarmes fica sem criptografia, porque o CloudWatch não publica em tópico com essa chave, e a mensagem dele não tem dado do aluno. Os logs não guardam descrição nem nota."

### 4.4 Monitoramento e um alarme (75 s)

**Mostre:** o dashboard `feedback-painel-prod` no CloudWatch. Antes de comentar os gráficos, diga que o dashboard de prod só tem dados das chamadas feitas neste vídeo. Os cinco widgets: estado dos três alarmes, invocações, falhas, duração e a tabela de restaurações do SnapStart.

**Fale:** "Cada função tem um filtro no próprio grupo de log, que conta as falhas marcadas pelo handler e o tempo esgotado. Uma falha em 5 minutos dispara o alarme, que manda e-mail por um tópico só de alarmes. Entrada rejeitada e data inválida não disparam. Agora forço um alarme pelo terminal."

Este comando gera um e-mail real para o endereço assinante. Dispare um único alarme e não crie alarme novo: a conta está no limite gratuito de 10 métricas de alarme.

Registro: rodou em dev, com o sufixo `-dev`, em 06/11/2026 ([QA de add-observabilidade-seguranca](../openspec/changes/archive/2026-11-11-add-observabilidade-seguranca/qa.md), seção 6.7), e em hml, com `-hml`, em 07/11/2026 ([QA de add-fundacao-projeto](../openspec/changes/archive/2026-11-11-add-fundacao-projeto/qa.md), seção "Release 1.0.0 em hml", item 4). Em prod, ainda não.

```bash
aws cloudwatch set-alarm-state \
  --alarm-name feedback-falha-registrar-avaliacao-prod \
  --state-value ALARM \
  --state-reason "Demonstracao do alarme no video" \
  --region sa-east-1 --profile tc4-admin
```

Depois do comando:

1. Atualize o dashboard: `feedback-falha-registrar-avaliacao-prod` aparece em ALARM. O widget de falhas continua vazio, porque o `set-alarm-state` muda o estado sem passar pela métrica.
2. Abra o e-mail com o assunto `ALARM: "feedback-falha-registrar-avaliacao-prod" in South America (Sao Paulo)` e mostre o nome, o estado e o motivo. Desfoque a conta, o ARN, o destinatário e o rodapé.
3. Volte ao dashboard. Sem falha real na janela, o alarme volta a OK sozinho, e a volta não gera e-mail. Em dev, levou de 60 a 80 s. Em hml, voltou em 4 s. Se ele já estiver em OK quando você voltar, abra o alarme e mostre a transição na aba de histórico.

**Fale:** "O e-mail de alarme só traz dados do alarme, nunca descrição ou nota. Como não houve falha de verdade, o alarme volta sozinho para OK."

## Bloco 5. Encerramento (10:30 a 11:30)

### 5.1 Documentação (30 s)

**Mostre:** a página do repositório no GitHub e a pasta `docs/`: [funções](funcoes.md), [segurança](seguranca.md), [monitoramento](monitoramento.md) e o [relatório do Tech Challenge](relatorio-tech-challenge.md). Passe pela coleção Postman e por `openspec/specs/`.

**Fale:** "O repositório tem o código das três funções, os testes, o template, os workflows e a documentação de cada função, da segurança e do monitoramento. O relatório da entrega está em docs."

### 5.2 Como o projeto foi feito (30 s)

**Mostre:** a pasta `openspec/changes/archive/`, com os cinco changes arquivados em 11/11/2026; um `qa.md` arquivado, como o [de add-relatorio-semanal](../openspec/changes/archive/2026-11-11-add-relatorio-semanal/qa.md), na seção "Execução agendada real, 09/11/2026"; e a pasta `.claude/agents/`, com os seis agentes: `po-analista`, `arquiteto`, `dev-java`, `devops`, `qa` e `documentador`.

**Fale:** "Cada parte entrou como um change do OpenSpec, com proposta, design, tarefas e um relatório de QA com o que foi conferido na AWS. Os changes implantados estão arquivados, e as specs consolidadas ficam em openspec/specs. Os agentes em .claude/agents dividem o trabalho em especificar, projetar, implementar, validar e documentar, e cada etapa só seguiu depois da minha aprovação. Obrigado."

## Se algo sair diferente

| Situação | O que fazer |
|---|---|
| O relatório não traz a avaliação de hoje | Confira se a data do payload é a de amanhã em São Paulo e acione de novo com a mesma data. O objeto é substituído e sai um novo aviso, o que precisa ser cortado na edição |
| O aviso crítico não chega | Confira a assinatura de `feedback-admins-prod` (preparação, passo 2). A entrega é pelo menos uma vez: o mesmo aviso pode chegar duas vezes |
| O Postman responde 404 | O `baseUrl` está com `/avaliacao` no final. Nada chegou à função |
| O comando da AWS pede login | A sessão do perfil expirou. Rode `aws sso login --profile tc4-admin` fora da gravação |
| Um alarme entra em ALARM sem o `set-alarm-state` | É falha real. Pare a gravação e investigue pelo grupo de log da função antes de continuar |

## Depois da gravação

Decisão do Gabriel em 10/11/2026: nada é apagado à mão em prod.

**O que fica em prod:**

- Na tabela `feedback-avaliacoes-prod`, as duas avaliações do vídeo, `Aula muito boa` e `Aula ruim`.
- No bucket, `relatorios/2026-11-09.txt` e `relatorios/2026-11-13.txt`.
- Na caixa do Gabriel, os e-mails do aviso crítico, do aviso do relatório e do alarme.
- Nos grupos de log de prod, as linhas das invocações, por 30 dias.
- O agendamento continua ativo. Na segunda, 16/11/2026, às 08:00 em São Paulo, ele grava `relatorios/2026-11-16.txt`, com a janela de 09/11 a 15/11, que inclui as avaliações do vídeo, e manda o aviso do relatório por e-mail.

**hml** fica no ar até a entrega, em 13/11/2026, e só é removida depois dela, não depois do ensaio nem da gravação. A remoção segue o procedimento de remoção de stack do [README](../README.md), com o bucket de hml esvaziado antes. Os itens e os relatórios do ensaio saem junto com a stack. A remoção não afeta prod: os ambientes não compartilham recursos ([QA de add-fundacao-projeto](../openspec/changes/archive/2026-11-11-add-fundacao-projeto/qa.md), seção "Release 1.0.0 em hml", item 6).

**prod** fica no ar até a nota da FIAP, não só até 13/11. Enquanto isso:

- Uma função Java com SnapStart que passa 14 dias sem invocação perde o snapshot, e a chamada seguinte falha com `SnapStartNotReadyException` até a versão voltar a `Active`. O relatório é invocado toda segunda, mas o registro e a notificação dependem de tráfego. Se a nota demorar, confira o passo 1 da preparação antes de alguém testar a API.

**Remoção de prod, depois da nota.** O CloudFormation só exclui bucket vazio, então esvazie o bucket antes de qualquer `sam delete`. Rode com o perfil administrativo, porque as roles de deploy não excluem a stack. O `aws s3 ls` mostra o que vai ser apagado: baixe antes o que quiser guardar.

Não executado: o procedimento de esvaziar o bucket e remover a stack não rodou em nenhum ambiente até 11/11/2026, como o README registra. Em prod, só roda depois da nota, por decisão do Gabriel.

```bash
STACK=feedback-prod

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

- O `sam delete` remove a stack inteira, inclusive a tabela, que tem `DeletionPolicy: Delete`. Sem `--no-prompts`, ele pede confirmação. No Git Bash do Windows, pode precisar ser `sam.cmd delete`.
- A tabela de prod tem recuperação pontual. Ao ser excluída, ela deixa um backup de sistema, mantido pela AWS por 35 dias, com as descrições do vídeo. Apagar esse backup antes é decisão do Gabriel.
- Detalhes e riscos da remoção estão no [README](../README.md) e em [docs/seguranca.md](seguranca.md).
