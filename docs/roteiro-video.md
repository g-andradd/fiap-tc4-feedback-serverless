# Roteiro do vídeo

Arquivo inicial, só com o bloco do relatório semanal. Os outros blocos e o tempo de cada um ainda não foram definidos. A função ainda não foi implantada, então este bloco não foi ensaiado na AWS.

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
