# Tasks: add-relatorio-semanal

Legenda de responsável: **[IA]** feito pelos agentes, **[Gabriel]** ação manual.

Depende de `add-registro-avaliacao` integrado: usa o `Globals`, o contrato do item e `Avaliacao.deAtributos`. É o último na ordem do `template.yaml`.

## 1. Domínio (`functions/dominio`)
- [x] 1.1 [IA] dev-java: criar `DataReferencia` e testes de ausente, `null`, `2026-11-04`, `09/11/2026`, `2026-02-30`, vazio, não texto, `+12026-01-01` e cálculo do dia atual em São Paulo às 22:00 de 03/11/2026, quando em UTC já é 04/11
- [x] 1.2 [IA] dev-java: criar `JanelaApuracao` e testes da referência 2026-11-09 (de 2026-11-02 a 2026-11-08), 2026-11-03 e 2026-11-04
- [x] 1.3 [IA] dev-java: criar `ConsolidadorRelatorio`, `ResumoSemanal` e `RelatorioSemanal`, com testes dos cenários de conteúdo, de totais completos com zero e de urgência persistida sem recálculo
- [x] 1.4 [IA] dev-java: testes da média: 5, 6 e 6 dão `5,67`; 2, 5 e 8 dão `5,00`; só notas 0 dão `0,00`; soma 41 em 8 avaliações dá `5,13`; janela vazia sem valor; sem depender de `Locale`
- [x] 1.5 [IA] dev-java: criar `FormatadorRelatorio` conforme D5, com teste do texto completo, da semana vazia e de que a nota individual não aparece
- [x] 1.6 [IA] dev-java: criar `AvisoRelatorio` conforme D7, recebendo só `ResumoSemanal` e a localização, com teste de que a localização corresponde à chave gravada
- [x] 1.7 [IA] dev-java: testes do conteúdo do aviso: texto completo exato com notas 2, 5 e 8 (média, 3 urgências, 7 dias e localização), o que também prova que nenhuma nota individual aparece; semana vazia com os zeros e `sem avaliações na janela` no lugar da média
- [x] 1.8 [IA] dev-java: Javadoc e `mvn -B verify`

## 2. Função (`functions/gerar-relatorio-semanal`)
- [x] 2.1 [IA] dev-java: declarar no `pom.xml` do módulo as dependências de Lambda, Powertools, `aspectjrt` em escopo `runtime` (`add-registro-avaliacao` D6), `url-connection-client`, `dynamodb`, `s3` e `sns`, com as exclusões de `apache-client`, `apache5-client` e `netty-nio-client` em cada cliente; conferir com `mvn dependency:tree`
- [x] 2.2 [IA] dev-java: criar `log4j2.xml` igual ao das outras funções
- [x] 2.3 [IA] dev-java: criar a leitura do índice com `queryPaginator`, uma consulta por dia e conversão pelo `Avaliacao.deAtributos` (D3)
- [x] 2.4 [IA] dev-java: criar `GerarRelatorioSemanalHandler` na ordem da D9: validar, ler, consolidar, gravar e publicar, com clientes `static final`, cadeia padrão de credenciais e data calculada dentro do handler
- [x] 2.5 [IA] dev-java: testes do handler com clientes simulados: várias páginas lidas por inteiro; falha em todos os dias, em um dia e no meio de um dia sem gravação e sem aviso; item inválido sem gravação; falha ao gravar sem aviso; falha ao publicar com relatório gravado e execução em falha; data inválida sem nenhuma chamada; janela vazia com gravação e aviso; mesma chave na reexecução
- [x] 2.6 [IA] dev-java: teste do handler com avaliação de descrição `TEXTO-MARCADOR-AVISO` e notas 2, 5 e 8: o objeto gravado contém o marcador, e a mensagem capturada no SNS não contém o marcador nem a nota de nenhuma avaliação, com o texto comparado por inteiro
- [x] 2.7 [IA] dev-java: teste com appender em memória: marcador da descrição e nota individual não aparecem em nenhum nível, com e sem falha
- [x] 2.8 [IA] dev-java: criar `PrimingSnapStartTest`, como em `add-registro-avaliacao`: executa `ClassPreLoader.preloadClasses()` e os hooks de `TracingUtils` e `MetricsFactory`, com a prova de que ele falha sem o `aspectjrt`
- [x] 2.9 [IA] dev-java: validar com `mvn -B verify`

## 3. Infraestrutura
- [x] 3.1 [IA] devops: acrescentar `LogGerarRelatorioSemanal`, `GerarRelatorioSemanal` e os outputs ao `template.yaml`, conforme o `design.md`, sem alterar `Globals`
- [x] 3.2 [IA] devops: validar com `sam validate --lint`
- [x] 3.3 [IA] devops: validar com `mvn -B install` seguido de `sam build`, usando JDK 25, e conferir `aspectjrt` em `lib/`, sem `aspectjtools` nem `aspectjweaver`. Só passa com o código de `add-notificacao-critica` junto, porque o template referencia as duas funções: validado em diretório temporário com as duas branches

## 4. Validação local
- [x] 4.1 [IA] qa: conferir a cobertura de cada `#### Scenario` da spec nos testes e relatar os que dependem da stack implantada

## 5. Deploy em dev e conferência
- [ ] 5.1 [Gabriel] Executar os comandos Git fornecidos (commits na `feature/add-relatorio-semanal`, push, PR para `develop`) e conferir o CI verde
- [ ] 5.2 [Gabriel] Fazer o merge e conferir o deploy da stack `feedback-dev`, especialmente a criação da role `GerarRelatorioSemanalAgendaRole` com boundary
- [ ] 5.3 [IA] qa: conferir o SnapStart: `aws lambda get-function-configuration --function-name feedback-gerar-relatorio-semanal-dev:live --region sa-east-1 --query "{Version:Version,State:State,OptimizationStatus:SnapStart.OptimizationStatus}"`. Esperado: `State` igual a `Active` e `OptimizationStatus` igual a `On`
- [ ] 5.4 [IA] qa: conferir o agendamento com `aws scheduler get-schedule --name feedback-relatorio-semanal-dev --region sa-east-1`: expressão, fuso, alvo no alias `live`, política de repetição e estado
- [ ] 5.5 [IA] qa: rodar o comando manual da D1, sempre com `:live`, sem data, com a data de amanhã e com data inválida, e conferir objeto, `FunctionError` e o aviso recebido: média, totais por urgência e por dia, localização, e nenhuma descrição nem nota individual
- [ ] 5.6 [IA] qa: conferir reexecução: um único objeto por data de referência e novo aviso
- [ ] 5.7 [IA] qa: confirmar que uma requisição anônima ao objeto é negada
- [ ] 5.8 [IA] qa: conferir nos logs do CloudWatch que o marcador da descrição não aparece
- [ ] 5.9 [IA] qa: registrar o relatório de conformidade

## 6. Documentação
- [x] 6.1 [IA] documentador: registrar no README o comando manual com `:live`, a regra da data de referência, o formato do relatório e do aviso, como baixar pelo `aws s3 cp` e o passo de esvaziar o bucket antes de `sam delete`
- [x] 6.2 [IA] documentador: registrar no roteiro do vídeo a espera de alguns segundos entre registrar a avaliação e acionar o relatório, e a referência de amanhã para incluir a avaliação do dia (D4 e D1)
- [x] 6.3 [IA] arquiteto: registrar no `design.md` os ajustes da implementação apontados no QA local, incluindo o `apiCallTimeout` de 10 s por chamada ao SDK, aprovado pelo Gabriel em 05/11/2026
