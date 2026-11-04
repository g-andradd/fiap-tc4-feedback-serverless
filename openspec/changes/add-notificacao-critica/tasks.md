# Tasks: add-notificacao-critica

Legenda de responsável: **[IA]** feito pelos agentes, **[Gabriel]** ação manual.

Depende de `add-registro-avaliacao` integrado: usa o `Globals`, o contrato do item e `Avaliacao.deAtributos`.

## 1. Domínio (`functions/dominio`)
- [ ] 1.1 [IA] dev-java: criar `AvisoCritico` com assunto e corpo, conforme D6
- [ ] 1.2 [IA] dev-java: testes de `AvisoCritico`: campos obrigatórios presentes, 1000 caracteres com acentos sem corte nem reticências, e data igual à persistida, mesmo com outra data de processamento
- [ ] 1.3 [IA] dev-java: Javadoc e `mvn -B verify`

## 2. Função (`functions/notificar-critica`)
- [ ] 2.1 [IA] dev-java: declarar no `pom.xml` do módulo as dependências de Lambda, Powertools, `url-connection-client` e `sns`, com as exclusões de `apache-client`, `apache5-client` e `netty-nio-client`; conferir com `mvn dependency:tree`
- [ ] 2.2 [IA] dev-java: criar `log4j2.xml` igual ao de `registrar-avaliacao`
- [ ] 2.3 [IA] dev-java: criar `NotificarCriticaHandler` conforme D2, D3, D5 e D7: laço em ordem, retorno imediato na primeira falha com o número de sequência, descarte de item inválido, `apiCallTimeout` de 5 segundos, cliente `static final` com cadeia padrão de credenciais
- [ ] 2.4 [IA] dev-java: testes do handler com cliente simulado para lote de três críticas com três publicações; falha no segundo registro com retorno do número de sequência dele e sem publicar o terceiro; `MODIFY` e `REMOVE` sem publicação; urgência diferente de `CRITICA` sem publicação; item sem `descricao` descartado sem falhar o lote; evento repetido com duas publicações; `batchItemFailures` vazio no sucesso
- [ ] 2.5 [IA] dev-java: teste com appender em memória: marcador da descrição não aparece em nenhum nível, nos casos de sucesso, falha ao publicar e item inválido; falha ao publicar aparece em ERROR
- [ ] 2.6 [IA] dev-java: validar com `mvn -B verify`

## 3. Infraestrutura
- [ ] 3.1 [IA] devops: acrescentar `LogNotificarCritica`, `NotificarCritica` e os outputs ao `template.yaml`, conforme o `design.md`, sem alterar `Globals`
- [ ] 3.2 [IA] devops: validar com `sam validate --lint`
- [ ] 3.3 [IA] devops: validar com `mvn -B install` seguido de `sam build`, usando JDK 25

## 4. Validação local
- [ ] 4.1 [IA] qa: conferir a cobertura de cada `#### Scenario` da spec nos testes e relatar os que dependem da stack implantada
- [ ] 4.2 [IA] qa: confirmar por inspeção que não há armazenamento de avisos, escrita na tabela nem deduplicação no tópico

## 5. Deploy em dev e conferência
- [ ] 5.1 [Gabriel] Executar os comandos Git fornecidos (commits na `feature/add-notificacao-critica`, push, PR para `develop`) e conferir o CI verde
- [ ] 5.2 [Gabriel] Fazer o merge e conferir o deploy da stack `feedback-dev`. Se o deploy recusar S3 como destino, vale a tarefa 5.9
- [ ] 5.3 [Gabriel] Confirmar que a assinatura de e-mail de dev está ativa
- [ ] 5.4 [IA] qa: conferir o SnapStart: `aws lambda get-function-configuration --function-name feedback-notificar-critica-dev:live --region sa-east-1 --query "{Version:Version,State:State,OptimizationStatus:SnapStart.OptimizationStatus}"`. Esperado: `State` igual a `Active` e `OptimizationStatus` igual a `On`
- [ ] 5.5 [IA] qa: conferir o mapeamento com `aws lambda list-event-source-mappings --function-name feedback-notificar-critica-dev:live --region sa-east-1`: alias no `FunctionArn`, filtro, tentativas, idade máxima e destino. Depois alterar um item crítico com `aws dynamodb update-item` e olhar a métrica `Invocations` e os logs. Registrar se o filtro por `eventName` evitou a invocação ou se foi D2 que evitou o aviso (Risks)
- [ ] 5.6 [IA] qa: registrar avaliações `CRITICA`, `MEDIA` e `BAIXA` e conferir que só a crítica invoca a função e que o e-mail traz descrição, urgência e data de envio
- [ ] 5.7 [IA] qa: conferir nos logs do CloudWatch que o marcador da descrição não aparece
- [ ] 5.8 [IA] qa: registrar o relatório de conformidade em `qa.md` deste change
- [ ] 5.9 [IA] devops e qa: somente se o deploy recusar S3 como destino, devops aplica o plano B da D4 sem nova consulta ao Gabriel e entrega os comandos Git; qa registra em `qa.md` deste change o erro do deploy, a troca feita e o resultado do novo deploy

## 6. Documentação
- [ ] 6.1 [IA] documentador: registrar no README a política de reprocessamento, onde ficam os lotes descartados (`aws/lambda/` no bucket) e o passo de esvaziar o bucket antes de `sam delete`
- [ ] 6.2 [IA] documentador: propor a linha do diagrama de `project.md` indicada em "Desvios do project.md", para o Gabriel aprovar
