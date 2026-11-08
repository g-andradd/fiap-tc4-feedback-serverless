# Roteiro do vídeo

Arquivo inicial, com os blocos do relatório semanal e do monitoramento. Os outros blocos e o tempo de cada um ainda não foram definidos. Nenhum dos dois blocos foi ensaiado na AWS.

## Cuidados com o que aparece na tela

- Desfoque o ID da conta e o endereço de e-mail sempre que aparecerem: no menu da conta do console, nos ARNs, no nome do bucket de relatórios e nos e-mails, inclusive no rodapé.
- O e-mail do aviso do relatório traz o nome do bucket, que inclui o ID da conta. Desfoque antes de mostrar.

## Relatório semanal

Uma avaliação registrada no dia da gravação só entra no relatório com a referência de amanhã, no fuso de São Paulo. A janela são os 7 dias anteriores à referência, com a própria referência fora. Sem data, ou com a data de hoje, a avaliação fica de fora.

Exemplo: em uma gravação no dia 2026-11-03, a referência é `2026-11-04`, e a janela vai de `2026-10-28` a `2026-11-03`.

### Ordem de demonstração

1. Registrar a avaliação com `POST /avaliacao`, como em [Testar depois do deploy](../README.md#testar-depois-do-deploy).
2. Esperar alguns segundos antes de acionar. O índice `por-data` é atualizado de forma eventualmente consistente, e um relatório acionado logo em seguida pode sair sem a avaliação.
3. Acionar a função pelo alias `live`, com a data de amanhã no payload:

   ```bash
   aws lambda invoke \
     --function-name feedback-gerar-relatorio-semanal-dev:live \
     --region sa-east-1 \
     --cli-binary-format raw-in-base64-out \
     --cli-read-timeout 150 \
     --payload '{"dataReferencia":"2026-11-04"}' \
     resposta.json
   ```

4. Mostrar em `resposta.json` os campos `dataReferencia`, `chave` e `quantidadeAvaliacoes`.
5. Abrir pelo console do S3 o objeto `relatorios/<dataReferencia>.txt`, no bucket de relatórios, e mostrar a avaliação na lista.
6. Mostrar o e-mail do aviso, com a média, os totais e a localização, sem descrições nem notas individuais.

### Se a avaliação não aparecer

Acione de novo com a mesma data. O objeto é substituído, e sai um novo e-mail de aviso.

Formatos e detalhes do comando estão em [Relatório semanal: acionamento manual e download](../README.md#relatório-semanal-acionamento-manual-e-download).

## Monitoramento

Os alarmes e o dashboard ainda não foram implantados. A ordem abaixo segue o que está no template e precisa ser ensaiada depois do deploy.

Antes de gravar:

- Confirme que a assinatura do tópico `feedback-alarmes-<ambiente>` do ambiente usado no vídeo está confirmada. Sem isso, o alarme dispara e o e-mail não chega.
- Confira que os três alarmes estão em OK.
- Faça algumas chamadas ao `POST /avaliacao` antes, para os gráficos de invocações e duração não aparecerem vazios.

### Ordem de demonstração

1. No console do CloudWatch, em `sa-east-1`, abrir Dashboards e o painel `feedback-painel-dev`.
2. Mostrar os cinco widgets: o estado dos três alarmes, invocações, falhas tratadas e de execução, duração e a tabela de restaurações do SnapStart. A tabela só tem linhas se houve restauração no período escolhido.
3. Explicar que cada alarme conta as falhas tratadas pelo handler, marcadas com `alarme=FALHA_FUNCAO`, e o tempo esgotado, e que entrada rejeitada e data inválida não disparam.
4. Disparar um alarme pelo terminal. **Este comando gera um e-mail real** para o endereço assinante:

   ```bash
   aws cloudwatch set-alarm-state \
     --alarm-name feedback-falha-registrar-avaliacao-dev \
     --state-value ALARM \
     --state-reason "Demonstracao do alarme no video" \
     --region sa-east-1
   ```

5. Voltar ao dashboard e atualizar: o alarme do registro aparece em ALARM.
6. Mostrar o e-mail do alarme, com nome, estado e motivo, e apontar que ele não traz descrição nem nota de avaliação. Desfoque o ID da conta e o endereço de e-mail, que aparecem no corpo e no rodapé.
7. Atualizar o dashboard de novo cerca de um minuto depois: sem falha real na janela, o alarme volta a OK sozinho.

Dispare um único alarme. Cada `set-alarm-state` gera um e-mail, e não crie alarme novo para a demonstração: a conta já está no limite gratuito de 10 métricas de alarme. Detalhes em [Monitoramento e alarmes](../README.md#monitoramento-e-alarmes).
