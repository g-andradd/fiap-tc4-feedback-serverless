# Tasks: add-registro-avaliacao

Legenda de responsável: **[IA]** feito pelos agentes, **[Gabriel]** ação manual.

Depende de `add-fundacao-projeto` integrado e com o bootstrap aplicado. Tarefas 2 e 3 podem rodar em paralelo com a 4 depois da 1, porque não editam os mesmos arquivos.

## 1. Preparação
- [x] 1.1 [IA] dev-java: limpar o `pom.xml` parent conforme D6: remover `aspectjrt`, `aspectj-maven-plugin`, as propriedades `aspectj.version` e `aspectj-maven-plugin.version` e os comentários sobre anotações; acrescentar em `dependencyManagement` `powertools-logging` e `jackson-databind`, esta na mesma versão que o Powertools traz
- [x] 1.2 [Gabriel] Apontar o JAVA_HOME para um JDK 25 local
- [x] 1.3 [IA] dev-java: validar com `mvn -B verify`

## 2. Domínio (`functions/dominio`)
- [x] 2.1 [IA] dev-java: criar `Urgencia` com `deNota(int)` e testes nas notas 0, 3, 4, 6, 7 e 10
- [x] 2.2 [IA] dev-java: criar `CarimboDeTempo` e testes de `criadoEm` e `dataEnvio`, incluindo `2026-11-10T02:59:59Z`, `2026-11-10T03:00:00Z`, largura fixa de 24 caracteres com fração zero e ordem lexicográfica igual à cronológica
- [x] 2.3 [IA] dev-java: criar `AtributosAvaliacao`, `Avaliacao` (`nova` e `deAtributos`) e `ItemInvalidoException`, com testes de item completo, campo faltante, nota não numérica, urgência desconhecida e mensagem da exceção sem valores
- [x] 2.4 [IA] dev-java: criar `ValorEntrada` e `ValidadorAvaliacao`, com um teste por cenário de `descricao` e de `nota` da spec: 1, 1000 e 1001 caracteres, 1000 emojis, só espaços, tabulação e quebra de linha, nulo, ausente, não texto, notas -1, 0, 10, 11, decimal, texto e inteiro enorme
- [x] 2.5 [IA] dev-java: Javadoc nas classes públicas e `mvn -B verify`

## 3. Função (`functions/registrar-avaliacao`)
- [x] 3.1 [IA] dev-java: declarar no `pom.xml` do módulo `aws-lambda-java-core`, `aws-lambda-java-events`, Powertools, `jackson-databind`, `url-connection-client` e `dynamodb`, com as exclusões de `apache-client`, `apache5-client` e `netty-nio-client`; conferir com `mvn dependency:tree`
- [x] 3.2 [IA] dev-java: criar `log4j2.xml` com `JsonTemplateLayout` e `LambdaJsonLayout.json`
- [x] 3.3 [IA] dev-java: criar o leitor do corpo conforme D4 (corpo vazio, base64, tokens sobrando, não objeto, conversão para `ValorEntrada`), com testes de `7.0`, `1e1`, `"7"`, `null`, JSON truncado e `[1, 2, 3]`
- [x] 3.4 [IA] dev-java: criar `RegistrarAvaliacaoHandler` conforme D5, D6 e D7: DNS e `TracingUtils.init()` no bloco estático, cliente `static final` com cadeia padrão de credenciais, UUID e instante gerados no handler, `flush` em `finally`
- [x] 3.5 [IA] dev-java: testes do handler com cliente simulado: 201 com `id` e `urgencia`, corpo do item gravado, campos extras ignorados, envios iguais com `id` diferente, 400 sem nenhuma gravação, dois campos citados, 500 sem detalhe técnico quando a gravação falha
- [x] 3.6 [IA] dev-java: teste com appender em memória: marcador da descrição e valor da nota não aparecem em nenhum nível, nos casos de sucesso, campo inválido, corpo malformado e falha ao gravar
- [x] 3.7 [IA] dev-java: conferir por revisão que nenhum campo estático guarda ID, instante ou gerador pseudoaleatório (D7)
- [x] 3.8 [IA] dev-java: validar com `mvn -B verify`

## 4. Infraestrutura
- [x] 4.1 [IA] devops: substituir `Globals` no `template.yaml` pelo trecho do `design.md`
- [x] 4.2 [IA] devops: acrescentar `ApiAvaliacoes`, `LogRegistrarAvaliacao`, `RegistrarAvaliacao` e os outputs, conforme o `design.md`
- [x] 4.3 [IA] devops: validar com `sam validate --lint`
- [x] 4.4 [IA] devops: validar com `mvn -B install` seguido de `sam build`, usando JDK 25, e conferir que `lib/` traz o jar do `dominio` e nenhum `aspectjrt`

## 5. Validação local
- [x] 5.1 [IA] qa: conferir a cobertura de cada `#### Scenario` da spec nos testes e relatar os que dependem da stack implantada
- [x] 5.2 [IA] qa: rodar `mvn -B verify` e `sam validate --lint`, e confirmar que o domínio não importa SDK da AWS nem Jackson

## 6. Deploy em dev e conferência
- [ ] 6.1 [Gabriel] Executar os comandos Git fornecidos (commits na `feature/add-registro-avaliacao`, push, PR para `develop`) e conferir o CI verde
- [ ] 6.2 [Gabriel] Fazer o merge e conferir o deploy da stack `feedback-dev`
- [ ] 6.3 [IA] qa: conferir o SnapStart na versão publicada: `aws lambda get-function-configuration --function-name feedback-registrar-avaliacao-dev:live --region sa-east-1 --query "{Version:Version,State:State,OptimizationStatus:SnapStart.OptimizationStatus}"`. Esperado: `State` igual a `Active` e `OptimizationStatus` igual a `On`. Registrar o resultado como a prova prática prevista em fundação D7
- [ ] 6.4 [IA] qa: confirmar que a integração da API aponta para o alias `live`, e não para `$LATEST`
- [ ] 6.5 [IA] qa: confirmar a tag `ambiente=dev` na API com `aws apigatewayv2 get-api --api-id <id> --region sa-east-1 --query Tags` (D1)
- [ ] 6.6 [IA] qa: executar os cenários de ponta a ponta com a URL do output `ApiAvaliacoesUrl`: sucesso, validações, corpo malformado e consulta ao índice `por-data` pela data gravada
- [ ] 6.7 [IA] qa: conferir nos logs do CloudWatch que o marcador enviado na descrição não aparece e que os logs são JSON
- [ ] 6.8 [IA] qa: registrar o relatório de conformidade

## 7. Documentação
- [x] 7.1 [IA] documentador: registrar no README o endpoint, os exemplos de requisição e resposta, a regra de urgência e a coleção Postman
- [x] 7.2 [IA] documentador: registrar no README como conferir o SnapStart no alias
