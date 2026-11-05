# Tasks: add-notificacao-critica

Legenda de responsável: **[IA]** feito pelos agentes, **[Gabriel]** ação manual.

Depende de `add-registro-avaliacao` integrado: usa o `Globals`, o contrato do item e `Avaliacao.deAtributos`.

## 1. Domínio (`functions/dominio`)
- [x] 1.1 [IA] dev-java: criar `AvisoCritico` com assunto e corpo, conforme D6
- [x] 1.2 [IA] dev-java: testes de `AvisoCritico`: campos obrigatórios presentes, 1000 caracteres com acentos sem corte nem reticências, e data igual à persistida, mesmo com outra data de processamento
- [x] 1.3 [IA] dev-java: Javadoc e `mvn -B verify`

## 2. Função (`functions/notificar-critica`)
- [x] 2.1 [IA] dev-java: declarar no `pom.xml` do módulo as dependências de Lambda, Powertools, `aspectjrt` em escopo `runtime` (`add-registro-avaliacao` D6), `url-connection-client` e `sns`, com as exclusões de `apache-client`, `apache5-client` e `netty-nio-client`; conferir com `mvn dependency:tree`
- [x] 2.2 [IA] dev-java: criar `log4j2.xml` igual ao de `registrar-avaliacao`
- [x] 2.3 [IA] dev-java: criar `NotificarCriticaHandler` conforme D2, D3, D5 e D7: laço em ordem, retorno imediato na primeira falha com o número de sequência, descarte de item inválido, `apiCallTimeout` de 5 segundos, cliente `static final` com cadeia padrão de credenciais
- [x] 2.4 [IA] dev-java: testes do handler com cliente simulado para lote de três críticas com três publicações; falha no segundo registro com retorno do número de sequência dele e sem publicar o terceiro; `MODIFY` e `REMOVE` sem publicação; urgência diferente de `CRITICA` sem publicação; item sem `descricao` descartado sem falhar o lote; evento repetido com duas publicações; `batchItemFailures` vazio no sucesso
- [x] 2.5 [IA] dev-java: teste com appender em memória: marcador da descrição não aparece em nenhum nível, nos casos de sucesso, falha ao publicar e item inválido; falha ao publicar aparece em ERROR
- [x] 2.6 [IA] dev-java: criar `PrimingSnapStartTest`, como em `add-registro-avaliacao`: executa `ClassPreLoader.preloadClasses()` e os hooks de `TracingUtils` e `MetricsFactory`, com a prova de que ele falha sem o `aspectjrt`
- [x] 2.7 [IA] dev-java: validar com `mvn -B verify`

## 3. Infraestrutura
- [x] 3.1 [IA] devops: acrescentar `LogNotificarCritica`, `NotificarCritica` e os outputs ao `template.yaml`, conforme o `design.md`, sem alterar `Globals`
- [x] 3.2 [IA] devops: validar com `sam validate --lint`
- [x] 3.3 [IA] devops: validar com `mvn -B install` seguido de `sam build`, usando JDK 25, e conferir `aspectjrt` em `lib/`, sem `aspectjtools` nem `aspectjweaver`

## 4. Validação local
- [x] 4.1 [IA] qa: conferir a cobertura de cada `#### Scenario` da spec nos testes e relatar os que dependem da stack implantada
- [x] 4.2 [IA] qa: confirmar por inspeção que não há armazenamento de avisos, escrita na tabela nem deduplicação no tópico

## 5. Deploy em dev e conferência
- [x] 5.1 [Gabriel] Executar os comandos Git fornecidos (commits na `feature/add-notificacao-critica`, push, PR para `develop`) e conferir o CI verde
- [x] 5.2 [Gabriel] Fazer o merge e conferir o deploy da stack `feedback-dev`. Se o deploy recusar S3 como destino, vale a tarefa 5.9. O primeiro deploy, do merge `5cb6f4d`, falhou pelo escopo do `PutObject`, ver a seção 7. O segundo, do merge `c6bb879`, ficou verde
- [x] 5.3 [Gabriel] Confirmar que a assinatura de e-mail de dev está ativa
- [x] 5.4 [IA] qa: conferir o SnapStart: `aws lambda get-function-configuration --function-name feedback-notificar-critica-dev:live --region sa-east-1 --query "{Version:Version,State:State,OptimizationStatus:SnapStart.OptimizationStatus}"`. Esperado: `State` igual a `Active` e `OptimizationStatus` igual a `On`
- [x] 5.5 [IA] qa: conferir o mapeamento com `aws lambda list-event-source-mappings --function-name feedback-notificar-critica-dev:live --region sa-east-1`: alias no `FunctionArn`, filtro, tentativas, idade máxima e destino. Depois alterar um item crítico com `aws dynamodb update-item` e olhar a métrica `Invocations` e os logs. Registrar se o filtro por `eventName` evitou a invocação ou se foi D2 que evitou o aviso (Risks)
- [ ] 5.6 [IA] qa: registrar avaliações `CRITICA`, `MEDIA` e `BAIXA` e conferir que só a crítica invoca a função e que o e-mail traz descrição, urgência e data de envio. Execução feita; falta o Gabriel confirmar no inbox os dois e-mails de 05/11/2026, 22:15 UTC
- [x] 5.7 [IA] qa: conferir nos logs do CloudWatch que o marcador da descrição não aparece
- [x] 5.8 [IA] qa: registrar o relatório de conformidade em `qa.md` deste change
- [x] 5.9 [IA] devops e qa: somente se o deploy recusar S3 como destino, devops aplica o plano B da D4 sem nova consulta ao Gabriel e entrega os comandos Git; qa registra em `qa.md` deste change o erro do deploy, a troca feita e o resultado do novo deploy. Não se aplicou: o S3 foi aceito como destino, e a falha foi de escopo, corrigida na seção 7

## 6. Documentação
- [x] 6.1 [IA] documentador: registrar no README a política de reprocessamento, onde ficam os lotes descartados (`aws/lambda/` no bucket) e o passo de esvaziar o bucket antes de `sam delete`
- [x] 6.2 [IA] documentador: propor a linha do diagrama de `project.md` indicada em "Desvios do project.md", para o Gabriel aprovar

## 7. Correção do escopo do destino S3
- [x] 7.1 [IA] provar com role e função temporárias, contra o stream de dev, qual escopo de `s3:PutObject` o Lambda aceita ao criar o mapeamento: `<bucket>/aws/lambda/*` foi recusado com e sem `s3:ResourceAccount`, e `<bucket>/*` com a condição foi aceito. Recursos temporários apagados
- [x] 7.2 [IA] devops: trocar o recurso de `GuardarLoteDescartado` para `<bucket>/*`, mantendo `s3:ResourceAccount`, no `template.yaml` desta branch e no de `feature/add-relatorio-semanal`, e validar com `sam validate --lint`
- [x] 7.3 [IA] arquiteto: registrar na D4 e na tabela de permissões o escopo aceito e o custo dele
- [x] 7.4 [IA] documentador: corrigir no README o parágrafo sobre S3 como destino, nas duas branches
- [x] 7.5 [Gabriel] Executar os comandos Git fornecidos (commit na `feature/add-notificacao-critica-fix-destino`, push, PR para `develop`), fazer o merge e conferir o deploy verde, que fecha a 5.2
- [x] 7.6 [IA] qa: registrar em `qa.md` deste change o erro do primeiro deploy, a troca feita e o resultado do novo deploy, junto com a 5.8
