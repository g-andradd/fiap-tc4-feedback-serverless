# Incidente: deploy em dev falhou na inicialização com SnapStart

**Data:** 04/11/2026. **Ambiente:** dev. **Situação:** correção pronta, aguardando merge. Só o próximo deploy em dev prova o resultado na AWS.

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
