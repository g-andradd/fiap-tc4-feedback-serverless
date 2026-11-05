# Incidente: deploys em dev falharam no SnapStart e na tag da API

**Data:** 04/11/2026 e 05/11/2026 (UTC). **Ambiente:** dev. **Situação:** duas falhas seguidas. A correção do SnapStart está na `develop`. A da permissão de tag já está aplicada na conta, com o bootstrap reaplicado em 05/11/2026, e o arquivo aguarda o merge. Só o próximo deploy em dev prova o resultado na AWS.

As seções abaixo tratam da primeira falha. A segunda está em [Segunda falha](#segunda-falha-permissão-de-tag-no-stage-da-api).

## Sintoma

O merge do PR #4 (`539660f`) disparou a [execução 41935808134](https://github.com/g-andradd/fiap-tc4-feedback-serverless/actions/runs/41935808134), que falhou ao publicar a primeira versão de `feedback-registrar-avaliacao-dev`:

```
Resource of type 'AWS::Lambda::Version' [...] did not stabilize.
Status Reason is An error occurred during function initialization.
```

A stack voltou para `UPDATE_ROLLBACK_COMPLETE`. Tabela, tópico e bucket não foram afetados. O rollback apagou a função e o grupo de log, então não sobrou log da falha na conta.

## Causa

A publicação com SnapStart executa os hooks `beforeCheckpoint` registrados por `TracingUtils` e `MetricsFactory`. Eles chamam `ClassPreLoader.preloadClasses()`, que inicializa as classes listadas em `classesloaded.txt`.

Essa lista inclui `LambdaTracingAspect` e `LambdaMetricsAspect`, que dependem de `org.aspectj.lang.NoAspectBoundException`. O change `add-registro-avaliacao` tinha removido o `aspectjrt`, porque usa só a API funcional. Sem o jar, a inicialização dessas classes gera `NoClassDefFoundError`.

No Powertools 2.11.0, `loadClassIfFound` só captura `ClassNotFoundException` e `preloadClassesFromStream` só captura `Exception`. O `NoClassDefFoundError` é um `Error`, então escapa e derruba o hook.

## Evidência

Como o log se perdeu, a evidência é uma reprodução local, com o código de `origin/develop` e os hooks chamados diretamente em uma JVM comum, sem checkpoint real:

| Classpath | Preload e hooks | Classes da lista com erro |
|---|---|---|
| Sem `aspectjrt` | falham com `NoClassDefFoundError` | 2 |
| Com `aspectjrt` 1.9.27 | passam | 0 |

O código-fonte do [`ClassPreLoader` na tag v2.11.0](https://github.com/aws-powertools/powertools-lambda-java/blob/v2.11.0/powertools-common/src/main/java/software/amazon/lambda/powertools/common/internal/ClassPreLoader.java) mostra o comportamento atual.

A correção upstream é o [PR #2684](https://github.com/aws-powertools/powertools-lambda-java/pull/2684), commit `f628502`, que acrescenta `catch (LinkageError e)` em `loadClassIfFound`. Ele foi feito depois da v2.11.0 e ainda não foi lançado em uma release.

## Correção

`aspectjrt` em escopo `runtime` no módulo da função, com a versão em `dependencyManagement`. Sem plugin, sem weaving e sem anotações. É um desvio temporário, registrado na D6 do `design.md` de `add-registro-avaliacao`.

O desvio é revisto na primeira release do Powertools que traga o commit `f628502`. O jar só sai se o teste de priming continuar passando sem ele e o deploy confirmar `State: Active` e `OptimizationStatus: On`.

## Prevenção

- **`PrimingSnapStartTest`:** executa `ClassPreLoader.preloadClasses()` e os hooks de `TracingUtils` e `MetricsFactory`. Sem o jar, os 3 testes falham com `NoClassDefFoundError`, então o CI pega o erro antes do deploy.
- **Outras funções:** `add-notificacao-critica` e `add-relatorio-semanal` têm o mesmo teste e a mesma dependência nas tasks.
- **O que faltou:** a verificação do design só confirmou que o módulo compilava e carregava as classes sem o jar. Nem os testes, nem a validação local do QA, nem a revisão executaram os hooks.
- **Limite do teste:** ele roda em JVM comum, sem checkpoint real. A prova de SnapStart continua sendo a versão publicada, na tarefa 6.3.

## Segunda falha: permissão de tag no Stage da API

### Sintoma

O merge do PR #5 (`e5df006`) disparou a [execução 41943350426](https://github.com/g-andradd/fiap-tc4-feedback-serverless/actions/runs/41943350426), em 05/11/2026. A versão e o alias da função chegaram a `CREATE_COMPLETE`. Depois, o Stage da API falhou:

```
Resource of type 'AWS::ApiGatewayV2::Stage': User: [...]/github-deploy-feedback-dev/deploy-41943350426
is not authorized to perform: apigateway:TagResource
on resource: arn:aws:apigateway:sa-east-1::/apis/<id>/stages
because no identity-based policy allows the apigateway:TagResource action
```

A stack voltou para `UPDATE_ROLLBACK_COMPLETE`. O rollback apagou a função, então `State` e `OptimizationStatus` ainda não foram conferidos. A versão ter estabilizado indica que a correção do `aspectjrt` funcionou, mas a prova continua sendo a tarefa 6.3.

### Causa

As tags da stack, definidas no `samconfig.toml`, são aplicadas aos recursos que aceitam tag. O handler de `AWS::ApiGatewayV2::Stage` exige `apigateway:TagResource`, e o bootstrap só liberava os verbos HTTP (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`).

A ação passou despercebida porque as fontes divergem. Ela está no esquema do CloudFormation e na mensagem de erro, mas não aparece na referência de serviço do IAM, no catálogo do cfn-lint nem no Access Analyzer. A falha só apareceu agora porque o primeiro deploy parou antes de chegar ao Stage.

### Evidência

| Fonte | O que mostra |
|---|---|
| Eventos da stack `feedback-dev` | `CREATE_FAILED` no Stage às 00:45:44 UTC, com a mensagem acima |
| CloudTrail, `CreateStage` | `AccessDenied` às 00:45:43 UTC, com a role de deploy de dev |
| CloudTrail, `iam:CreateServiceLinkedRole` | `AccessDenied` às 00:45:40 UTC, com a mesma role, pedido por `apigateway.amazonaws.com` |
| `describe-type` de `AWS::ApiGatewayV2::Stage` | O handler `create` lista `apigateway:TagResource` |

### Correção

`apigateway:TagResource` e `apigateway:UntagResource` no Allow `ApisNaRegiao` e no Deny `NegaEscritaEmApiDeOutroAmbiente`, nos três ambientes de `infra/bootstrap/github-oidc.yaml`. O recurso `PermissoesAplicacao` também ganhou a supressão de `W3037`, porque o lint do CI rejeita essas duas ações.

### Dúvida em aberto

`AWSServiceRoleForAPIGateway` não existe na conta, e a negação de `iam:CreateServiceLinkedRole` veio 3 segundos antes da falha do Stage. Não está claro se o Stage depende dessa role. Por isso, a permissão não entrou no bootstrap.

### Prevenção

- **Auditoria:** as permissões de notificação, relatório e observabilidade foram conferidas contra o esquema do CloudFormation e simuladas com `simulate-custom-policy`, nos três ambientes.
- **Limite da auditoria:** o esquema não lista chamadas extras do serviço, como a service-linked role, e o simulador só compara texto de ação. Recursos nunca implantados só ficam provados no deploy.
- **O que faltou:** a conferência das ações do bootstrap não olhou o que o handler de cada tipo exige no esquema do CloudFormation.
