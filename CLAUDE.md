# Instruções para o Claude Code

Este repositório é a plataforma de feedback serverless do Tech Challenge da Fase 4 (FIAP). O contexto completo está em `openspec/project.md`. Leia antes de qualquer tarefa.

## Papel da sessão principal

A sessão principal atua como orquestradora. Ela não implementa sozinha: distribui o trabalho entre os subagentes de `.claude/agents/`, junta os resultados e conversa com o Gabriel.

| Etapa | Agente | Saída |
|---|---|---|
| 1. Especificar | po-analista | `proposal.md` e deltas de spec |
| 2. Projetar | arquiteto | `design.md` e `tasks.md` |
| 3. Aprovar | **Gabriel** | aprovação explícita no chat |
| 4. Implementar | dev-java e devops | código, IaC e pipelines |
| 5. Validar | qa | testes e relatório de conformidade |
| 6. Documentar | documentador | README, docs e relatório da FIAP |
| 7. Versionar | **Gabriel** | commits, push, PR e merge |

## Regras inegociáveis

1. **SDD:** nenhuma linha de código antes de o Gabriel aprovar o change correspondente. Sem aprovação explícita, o trabalho para na etapa 2.
2. **Git:** agentes nunca executam operações de escrita no Git (add, commit, push, merge, rebase, tag, checkout de branch, criação ou merge de PR). As permissões em `.claude/settings.json` bloqueiam esses comandos. Ao final de cada etapa que precise de Git, entregue ao Gabriel os comandos prontos, em ordem, com uma linha explicando cada passo.
3. **Commits:** Conventional Commits em português, sem `Co-Authored-By` e sem qualquer assinatura de agente.
4. **Escopo:** prazo de 13/11/2026. O que não estiver no enunciado ou no change aprovado fica de fora. Na dúvida, pergunte.
5. **Escrita:** português do Brasil, sem travessão (—), texto direto e humano.

## Autorizações do Gabriel (03/11/2026)

Valem para o change `add-fundacao-projeto`, salvo indicação em contrário.

- O change `add-fundacao-projeto` está aprovado.
- Java 25 confirmado: Corretto 25 no ambiente local e, no CI, `actions/setup-java` com `distribution: corretto` e `java-version: 25`.
- Configuração via `gh`, mostrando antes de cada comando o que ele vai fazer:
  - ambientes `dev`, `hml` e `prod`, com a variável `AWS_ROLE_ARN` em cada um;
  - aprovação manual em `prod`, com `g-andradd` como revisor e sem ativar "prevent self-review", porque o projeto é individual;
  - proteção de `main` e `develop`: merge só por PR com CI verde, sem exigir aprovação de revisor.
- Worktrees e as branches que eles exigem podem ser criados para trabalho em paralelo. Essa é a única exceção à regra 2: commit, push, merge, rebase, tag e criação ou merge de PR continuam proibidos.

## Paralelismo

- Subagentes rodam em paralelo quando não editam os mesmos arquivos. Se dois precisam do mesmo arquivo, rodam em sequência.
- `template.yaml` é editado só pelo devops.
- `tasks.md` é marcado só pela sessão principal enquanto houver subagentes em paralelo, para que dois agentes não disputem o mesmo arquivo.
- Com arquivos separados, os subagentes trabalham no mesmo checkout. Worktree só entra quando a sobreposição é inevitável, porque juntar as branches depois depende de um merge feito pelo Gabriel.

## Gitflow

- `feature/<change-id>` a partir de `develop`
- `develop` publica em dev, `release/x.y.z` em hml, `main` em prod
- Release e hotfix voltam para `develop` depois do merge em `main`

## Comandos úteis

```bash
mvn -B verify                 # build e testes
sam validate --lint           # valida o template
sam build                     # empacota as funções
sam local invoke <Funcao>     # executa uma função localmente
openspec list                 # changes ativos
openspec validate <change-id> --strict
```
