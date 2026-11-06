# Contexto do Projeto

## Propósito

Plataforma serverless de feedback de aulas, desenvolvida como Tech Challenge da Fase 4 da pós-graduação em Arquitetura e Desenvolvimento Java (FIAP). Estudantes avaliam aulas com uma descrição e uma nota. Administradores recebem aviso imediato quando uma avaliação é crítica e um relatório semanal com a consolidação dos feedbacks.

Projeto individual. Autor: Gabriel Andrade Almeida.

Prazo de entrega: 13/11/2026. Escopo enxuto e objetivo é premissa, não opção.

## Stack

- Linguagem: Java 25 (runtime gerenciado `java25` do AWS Lambda, Amazon Corretto, LTS)
- Build: Maven multi-módulo
- Cloud: AWS, região `sa-east-1` (São Paulo)
- Computação: AWS Lambda com SnapStart
- Entrada HTTP: Amazon API Gateway
- Persistência: Amazon DynamoDB (on-demand)
- Eventos: DynamoDB Streams e Amazon EventBridge Scheduler
- Notificação: Amazon SNS (assinatura por e-mail)
- Armazenamento de relatórios: Amazon S3
- Observabilidade: Amazon CloudWatch (Logs, Metrics, Alarms, Dashboard) e AWS X-Ray
- IaC: AWS SAM
- CI/CD: GitHub Actions com autenticação OIDC na AWS
- Testes: JUnit 5, AssertJ, Mockito
- Utilitários de Lambda: Powertools for AWS Lambda (Java) para logs estruturados, tracing e métricas

Sem Spring nas funções. Handlers Java puros mantêm o pacote pequeno, o cold start baixo e o código fácil de explicar.

## Regras de domínio

### Entrada

`POST /avaliacao`

```json
{
  "descricao": "string",
  "nota": 0
}
```

- `descricao`: obrigatória, texto não vazio, até 1000 caracteres
- `nota`: obrigatória, inteiro de 0 a 10

### Urgência

O enunciado exige urgência no e-mail e no relatório, mas não define a regra. Decisão do projeto, derivada da nota:

| Nota | Urgência |
|---|---|
| 0 a 3 | CRITICA |
| 4 a 6 | MEDIA |
| 7 a 10 | BAIXA |

A urgência é calculada no registro e persistida junto com a avaliação. Somente avaliações CRITICA geram aviso imediato.

### Aviso de urgência (e-mail)

Campos obrigatórios: descrição, urgência e data de envio.

### Relatório semanal

O enunciado não define dia nem horário. Decisão do projeto: toda segunda-feira às 08:00 (America/Sao_Paulo), cobrindo os 7 dias anteriores. A função também pode ser acionada manualmente para demonstração no vídeo.

Conteúdo obrigatório:
- Lista de avaliações com descrição, urgência e data de envio
- Quantidade de avaliações por dia
- Quantidade de avaliações por urgência
- Média das notas

## Arquitetura

```
Aluno → API Gateway (POST /avaliacao) → Lambda registrar-avaliacao → DynamoDB
DynamoDB Streams (filtro: criação com urgencia = CRITICA) → Lambda notificar-critica → SNS → e-mail dos admins
EventBridge Scheduler (segunda 08h) → Lambda gerar-relatorio-semanal → S3 + SNS
CloudWatch + X-Ray em todas as funções
```

Cada função tem uma única responsabilidade:

| Função | Gatilho | Responsabilidade |
|---|---|---|
| registrar-avaliacao | API Gateway | Validar a entrada, classificar a urgência e persistir |
| notificar-critica | DynamoDB Streams | Avisar administradores sobre avaliação crítica |
| gerar-relatorio-semanal | EventBridge Scheduler | Consolidar a semana e publicar o relatório |

## Ambientes e Gitflow

| Branch | Ambiente | Stack SAM | Pipeline |
|---|---|---|---|
| feature/* | nenhum | nenhuma | build e testes no PR |
| develop | dev | feedback-dev | deploy automático |
| release/* | hml | feedback-hml | deploy automático |
| main | prod | feedback-prod | deploy no merge, com aprovação manual |

- Features nascem de `develop` com o nome `feature/<change-id>`
- Release nasce de `develop` como `release/x.y.z`, vai para `main` e volta para `develop`
- Hotfix nasce de `main` como `hotfix/x.y.z`, vai para `main` e `develop`
- Toda versão em produção recebe tag `vx.y.z`

## Convenções

### Forma de trabalho (SDD com OpenSpec)

1. Nenhuma linha de código antes de uma proposta escrita e aprovada pelo Gabriel
2. Cada mudança é um change em `openspec/changes/<change-id>/` com `proposal.md`, `tasks.md`, `design.md` quando houver decisão técnica relevante, e deltas de spec em `specs/<capability>/spec.md`
3. Requisitos usam `### Requirement:` e cada requisito tem ao menos um `#### Scenario:` no formato WHEN/THEN
4. Implementação segue `tasks.md` na ordem, marcando as caixas conforme conclui
5. Após o deploy em produção, o change é arquivado e as specs viram a verdade atual em `openspec/specs/`

### Git

- Operações de escrita no Git (add, commit, push, merge, rebase, tag, criação e merge de PR) são executadas somente pelo Gabriel
- Agentes preparam os arquivos e entregam os comandos prontos, com passo a passo
- Mensagens de commit no padrão Conventional Commits, em português, sem rodapé de coautoria e sem assinatura de agente
  - Exemplo: `feat(registro): valida nota entre 0 e 10`

### Código

- Pacote base: `br.com.fiap.feedback`
- Domínio isolado de SDK da AWS (domínio puro, adaptadores na borda)
- Nenhum dado de entrada do aluno em log em nível INFO; logs estruturados em JSON
- Toda regra de negócio coberta por teste unitário
- Javadoc nas classes públicas de domínio e nos handlers

### Escrita

- Documentação em português do Brasil
- Sem travessão (—) nos textos; escrita direta e humana, sem marcas típicas de texto gerado por IA

## Restrições

- Custo: tudo deve caber no Free Tier. Recursos de hml e prod podem ser removidos após a gravação do vídeo com `sam delete`
- Créditos limitados: a demonstração oficial é por vídeo
- Prazo de 10 dias: qualquer item fora do enunciado é cortado antes de comprometer o essencial

## Entregáveis

1. Repositório público no GitHub com código, IaC, pipelines e documentação
2. Vídeo demonstrando a aplicação, as funções serverless ativas e as configurações
3. Relatório do Tech Challenge seguindo o modelo da FIAP, adaptado ao contexto serverless:
   - Introdução: problema e objetivo
   - Arquitetura: descrição e diagrama
   - Endpoints: tabela, exemplos de requisição e resposta
   - Configuração do projeto: SAM e pipelines no lugar do Docker Compose, execução local com `sam local`
   - Qualidade do código: práticas e padrões adotados
   - Collections para teste: Postman
   - Repositório: URL
   - Seções extras exigidas na fase 4: modelo de cloud, segurança e governança, monitoramento, documentação das funções

## Backlog de changes

| Ordem | Change | Capability |
|---|---|---|
| 1 | add-fundacao-projeto | fundacao-cloud, entrega-continua |
| 2 | add-registro-avaliacao | registro-avaliacao |
| 3 | add-notificacao-critica | notificacao-critica |
| 4 | add-relatorio-semanal | relatorio-semanal |
| 5 | add-observabilidade-seguranca | observabilidade, seguranca |
| 6 | add-documentacao-entrega | documentacao |

### Decisões iniciais para os próximos changes

**`add-observabilidade-seguranca`, tópico de alarmes (05/11/2026):** sem criptografia em repouso, com política negando acesso sem TLS (`aws:SecureTransport` igual a `false`). A justificativa é que o tópico não carrega dado de aluno. O CloudWatch não publica em tópico com `alias/aws/sns`, e a alternativa com chave gerenciada pelo cliente tem custo mensal. A política do tópico precisa manter a publicação pelo CloudWatch. A confirmação final fica para a spec daquele change.
