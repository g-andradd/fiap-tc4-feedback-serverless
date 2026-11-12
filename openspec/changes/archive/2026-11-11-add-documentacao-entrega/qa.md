# QA: add-documentacao-entrega

## Cobertura dos riscos (tarefa 4.2)

Montada pelo documentador em 10/11/2026, para a conferência da tarefa 6.2. Cada item de origem tem um destino: um id do registro de riscos aceitos de `docs/seguranca.md`, "resolvido", com onde, ou "não é risco", com o motivo. Um mesmo id pode receber vários itens.

Fontes: as seções Risks dos cinco `design.md` (os 44 itens que a D4 cita), as decisões dos designs que aceitam um risco fora da seção Risks, as decisões e seções de risco aceito dos cinco `qa.md`, as observações e defeitos dos cinco `qa.md` (tarefa 4.2) e três itens da seção Risks deste change.

### Totais

| Bloco | Itens | Viraram R | Resolvidos | Não é risco |
|---|---|---|---|---|
| A. Risks dos cinco designs | 44 | 36 | 4 | 4 |
| B. Decisões dos designs com risco aceito | 10 | 10 | 0 | 0 |
| C. Decisões e risco aceito dos `qa.md` | 9 | 6 | 2 | 1 |
| D. Observações e defeitos dos `qa.md` | 58 | 13 | 21 | 24 |
| E. Risks de `add-documentacao-entrega` | 3 | 3 | 0 | 0 |
| **Total** | **124** | **68** | **27** | **29** |

O registro tem 43 riscos, de R01 a R43, e cada um recebe ao menos um item abaixo. Os 16 riscos obrigatórios da spec `documentacao` são R06, R07, R30, R42, R31, R08, R29, R10, R01, R02, R03, R17, R27, R16, R09 e R32.

### A. Risks dos cinco designs

| # | Item | Fonte | Destino |
|---|---|---|---|
| A1 | Uma conta para tudo | `add-fundacao-projeto`, design | R01 |
| A2 | Assinatura de e-mail do SNS exige confirmação manual | `add-fundacao-projeto`, design | R05 |
| A3 | SnapStart e Java 25: disponibilidade e snapshot apagado depois de 14 dias | `add-fundacao-projeto`, design | R04. A disponibilidade foi resolvida em `add-registro-avaliacao`, `qa.md`, 6.3 |
| A4 | API Gateway sem restrição por nome | `add-fundacao-projeto`, design | R02 |
| A5 | Escalada residual via IAM | `add-fundacao-projeto`, design | R03 |
| A6 | Alarmes e tópico criptografado | `add-fundacao-projeto`, design | Resolvido em `add-observabilidade-seguranca`, design, D1: tópico de alarmes próprio. O que ficou é R30 |
| A7 | JDK local | `add-registro-avaliacao`, design | Não é risco: afeta só a máquina local, e o CI com Corretto 25 comprova o build |
| A8 | X-Ray SDK em manutenção | `add-registro-avaliacao`, design | R09 |
| A9 | Nome da role gerado | `add-registro-avaliacao`, design | Resolvido: as roles foram criadas com o boundary em dev (`add-observabilidade-seguranca`, `qa.md`, 6.16) e em hml (`add-fundacao-projeto`, `qa.md`, "Release 1.0.0 em hml", item 1) |
| A10 | API pública sem autenticação | `add-registro-avaliacao`, design | R10 |
| A11 | KMS na tabela | `add-registro-avaliacao`, design | R11 |
| A12 | `sam build` repete os testes | `add-registro-avaliacao`, design | R12 |
| A13 | Primeira publicação com SnapStart é lenta | `add-registro-avaliacao`, design | R12 |
| A14 | Filtro por `eventName` | `add-notificacao-critica`, design | Resolvido em `add-notificacao-critica`, `qa.md`, 5.5 e observação A |
| A15 | S3 como destino | `add-notificacao-critica`, design | R16 |
| A16 | Objeto em `aws/lambda/` contém a descrição | `add-notificacao-critica`, design | R17 |
| A17 | `sam delete` com bucket não vazio | `add-notificacao-critica`, design | R18 |
| A18 | Backlog no primeiro deploy | `add-notificacao-critica`, design | R19 |
| A19 | Snapshot expirado | `add-notificacao-critica`, design | R04 |
| A20 | Aviso duplicado | `add-notificacao-critica`, design | R20 |
| A21 | E-mail depende de confirmar a assinatura | `add-notificacao-critica`, design | R05 |
| A22 | Consistência eventual no vídeo | `add-relatorio-semanal`, design | R23 |
| A23 | `sam delete` com bucket cheio | `add-relatorio-semanal`, design | R18 |
| A24 | Média com uma única avaliação | `add-relatorio-semanal`, design | R24 |
| A25 | Falha do item inválido bloqueia o relatório | `add-relatorio-semanal`, design | R25 |
| A26 | Volume | `add-relatorio-semanal`, design | R26 |
| A27 | Relatório sem expiração | `add-relatorio-semanal`, design | R17 |
| A28 | Bucket no e-mail | `add-relatorio-semanal`, design | R27 |
| A29 | Agenda em hml e prod | `add-relatorio-semanal`, design | R28 |
| A30 | Nível gratuito sem folga | `add-observabilidade-seguranca`, design | R32 |
| A31 | Rateio das métricas personalizadas | `add-observabilidade-seguranca`, design | R33 |
| A32 | Texto da linha de tempo esgotado sem prova real | `add-observabilidade-seguranca`, design | R31 |
| A33 | Falha fora da JVM | `add-observabilidade-seguranca`, design | R34 |
| A34 | Busca por substring | `add-observabilidade-seguranca`, design | R35 |
| A35 | Atraso do filtro | `add-observabilidade-seguranca`, design | R36 |
| A36 | Alarme no deploy | `add-observabilidade-seguranca`, design | Não é risco: é falha real, e o deploy falha junto |
| A37 | Linhas de teste no grupo de log | `add-observabilidade-seguranca`, design | R38 |
| A38 | Credenciais e conexões no snapshot | `add-observabilidade-seguranca`, design | R37 |
| A39 | Meta pode não ser atingida | `add-observabilidade-seguranca`, design | Resolvido em `add-observabilidade-seguranca`, `qa.md`, 6.15: as 15 chamadas abaixo da meta |
| A40 | Efeito somado de memória e aquecimento | `add-observabilidade-seguranca`, design | Não é risco: limite da medição, sem efeito em segurança ou operação |
| A41 | X-Ray no hook | `add-observabilidade-seguranca`, design | Não é risco: só muda com troca de versão do SDK, que passa por PR, CI e pelo QA do deploy, que lê o log da publicação |
| A42 | CI mais lento | `add-observabilidade-seguranca`, design | R12 |
| A43 | E-mails de prova | `add-observabilidade-seguranca`, design | R39 |
| A44 | Assinatura por ambiente | `add-observabilidade-seguranca`, design | R05 |

### B. Decisões dos designs com risco aceito

| # | Item | Fonte | Destino |
|---|---|---|---|
| B1 | Desvio do `aspectjrt` em escopo `runtime` | `add-registro-avaliacao`, design, D6 | R08 |
| B2 | Limite do X-Ray: mensagem da exceção do SDK no subsegmento | `add-registro-avaliacao`, design, D9 | R13 |
| B3 | Limite de DEBUG e TRACE | `add-registro-avaliacao`, design, D9 | R14 |
| B4 | Inatividade do SnapStart | `add-registro-avaliacao`, design, D7 | R04 |
| B5 | Segundo aviso igual na entrega assíncrona | `add-relatorio-semanal`, design, D8 | R20 |
| B6 | 12 ou mais páginas alcançam o `Timeout` sem log de ERROR | `add-relatorio-semanal`, design, "Ajustes da implementação", item 1 | R26 |
| B7 | Tópico de alarmes sem criptografia em repouso | `add-observabilidade-seguranca`, design, D1 | R30 |
| B8 | No máximo 3 métricas de alarme e 1 dashboard por stack | `add-observabilidade-seguranca`, design, decisão 2 e D8 | R32 |
| B9 | Proibido alterar a função na AWS para provar falha | `add-observabilidade-seguranca`, design, decisão 5 e D14 | R41 |
| B10 | Service-linked role como risco residual | `add-observabilidade-seguranca`, design, D12 | R40 |

### C. Decisões e risco aceito dos `qa.md`

| # | Item | Fonte | Destino |
|---|---|---|---|
| C1 | O3, ambientes sem restrição de branch | `add-fundacao-projeto`, `qa.md`, decisões de 04/11/2026 | Resolvido: política de branch nos três ambientes, conferida de novo em 10/11/2026 |
| C2 | O4, bypass de administrador | `add-fundacao-projeto`, `qa.md`, decisões de 04/11/2026 | R06 |
| C3 | O5, `strict: false` | `add-fundacao-projeto`, `qa.md`, decisões de 04/11/2026 | R07 |
| C4 | Defeito D1, número com mais de 1000 dígitos | `add-registro-avaliacao`, `qa.md`, aceito em 04/11/2026 | R15 |
| C5 | Decisão A, mensagem do SDK no trace | `add-registro-avaliacao`, `qa.md`, decisões de 04/11/2026 | R13 |
| C6 | Decisão B, métrica `ColdStart` | `add-registro-avaliacao`, `qa.md`, decisões de 04/11/2026 | Resolvido: D6 corrigida, e o dashboard usa a duração da restauração |
| C7 | Decisão C, DEBUG | `add-registro-avaliacao`, `qa.md`, decisões de 04/11/2026 | R14 |
| C8 | D, versões do Jackson, sem decisão | `add-registro-avaliacao`, `qa.md`, decisões de 04/11/2026 | Não é risco: não foi aceito nem recusado, e os 105 registros EMF reais de dev saíram sem erro de linkagem |
| C9 | `RuntimeException` que escapa da notificação | `add-observabilidade-seguranca`, `qa.md`, "Risco aceito", 06/11/2026 | R42 |

### D. Observações e defeitos dos `qa.md`

| # | Item | Fonte | Destino |
|---|---|---|---|
| D1 | Defeito D1, tag no Stream | `add-fundacao-projeto`, `qa.md` | Resolvido: fechado em 07/11/2026, sem o aviso na criação de hml |
| D2 | O1, mecanismo da negação no bucket | `add-fundacao-projeto`, `qa.md` | Não é risco: a negação está garantida, só o texto do cenário pode não corresponder ao erro |
| D3 | O2, `SubscriptionsDeleted` sem explicação | `add-fundacao-projeto`, `qa.md` | R05 |
| D4 | O6, drift não verificado | `add-fundacao-projeto`, `qa.md` | Não é risco: a comparação foi feita direto nos recursos |
| D5 | O7, acentos nas descrições lidas pela CLI | `add-fundacao-projeto`, `qa.md` | Não é risco: sem efeito funcional |
| D6 | Q, segunda inicialização fora de invocação | `add-fundacao-projeto`, `qa.md`, release | Não é risco: o aquecimento só lê, e nada foi gravado ou publicado |
| D7 | R, assinatura de alarmes de hml pendente | `add-fundacao-projeto`, `qa.md`, release | Resolvido: confirmada em 07/11/2026. O risco geral é R05 |
| D8 | E, entradas de borda sem cenário | `add-registro-avaliacao`, `qa.md` | Não é risco: comportamento que a spec não trata, sem efeito em segurança |
| D9 | F, ligação de produção fora do `mvn verify` | `add-registro-avaliacao`, `qa.md` | Resolvido: exercitada na stack |
| D10 | G, regra de credenciais só por revisão | `add-registro-avaliacao`, `qa.md` | Não é risco: o código usa a cadeia padrão, conferido na revisão de SnapStart do `qa.md` |
| D11 | H, nome de teste impreciso | `add-registro-avaliacao`, `qa.md` | Não é risco |
| D12 | I, primeira invocação depois da restauração | `add-registro-avaliacao`, `qa.md` | Resolvido em `add-observabilidade-seguranca`, design, D9, e `qa.md`, 6.15 |
| D13 | J, `INIT` sem invocação | `add-registro-avaliacao`, `qa.md` | Não é risco: sem erro e sem efeito |
| D14 | A, filtro por `eventName` | `add-notificacao-critica`, `qa.md` | Resolvido na stack |
| D15 | B, nenhum teste protege o filtro nem o tópico | `add-notificacao-critica`, `qa.md` | R21 |
| D16 | C, caminho de falha sem execução real | `add-notificacao-critica`, `qa.md` | R41 |
| D17 | D, ligação de produção | `add-notificacao-critica`, `qa.md` | Resolvido na stack |
| D18 | E, assunto com acentos | `add-notificacao-critica`, `qa.md` | Resolvido, conferido no e-mail pelo Gabriel |
| D19 | F, prazo da D3 por chamada | `add-notificacao-critica`, `qa.md` | Resolvido com a observação I: a primeira publicação caiu para 166 a 200 ms |
| D20 | G, trace do X-Ray | `add-notificacao-critica`, `qa.md` | Não é risco: confirmado sem marcador nem descrição |
| D21 | H, `Errors` não mostra a falha de publicação | `add-notificacao-critica`, `qa.md` | Resolvido em `add-observabilidade-seguranca`, design, D3: alarme por marcador de log |
| D22 | I, primeira publicação de 4,3 s | `add-notificacao-critica`, `qa.md` | Resolvido em `add-observabilidade-seguranca`, design, D9, e `qa.md`, 6.15 |
| D23 | J, `approximateInvokeCount` igual a 1 sem invocação | `add-notificacao-critica`, `qa.md` | Não é risco: afeta só a leitura manual do objeto, não o reprocessamento |
| D24 | K, WARN em toda invocação com sucesso | `add-notificacao-critica`, `qa.md` | Resolvido em `add-observabilidade-seguranca`, design, D11, e `qa.md`, 6.10 |
| D25 | L, inicializações e restaurações sem invocação | `add-notificacao-critica`, `qa.md` | Não é risco: sem efeito em métrica ou aviso |
| D26 | M, rodapé do e-mail expõe dados no vídeo | `add-notificacao-critica`, `qa.md` | R22 |
| D27 | A, ligação de produção | `add-relatorio-semanal`, `qa.md` | Resolvido nos caminhos de sucesso e de rejeição. Os de falha estão em R41 |
| D28 | B, execução agendada real | `add-relatorio-semanal`, `qa.md` | Não é risco: é conferência pendente, da tarefa 2.2 deste change |
| D29 | C, consistência eventual não apareceu | `add-relatorio-semanal`, `qa.md` | R23 |
| D30 | D, leitura interrompe no primeiro dia com falha | `add-relatorio-semanal`, `qa.md` | Não é risco: o efeito é o que a spec pede |
| D31 | E, nenhum teste lê o `template.yaml` | `add-relatorio-semanal`, `qa.md` | R21 |
| D32 | F, nome de teste maior que a asserção | `add-relatorio-semanal`, `qa.md` | Não é risco |
| D33 | G, resposta sem bucket | `add-relatorio-semanal`, `qa.md` | Não é risco: confirmada na stack |
| D34 | H, observações herdadas de `add-registro-avaliacao` | `add-relatorio-semanal`, `qa.md` | R13. A parte do `ColdStart` foi resolvida (C6) |
| D35 | I, assunto com acentos | `add-relatorio-semanal`, `qa.md` | Não é risco: aceito pelo SNS nas 6 publicações |
| D36 | J, primeira chamada depois da restauração | `add-relatorio-semanal`, `qa.md` | Resolvido em `add-observabilidade-seguranca`, `qa.md`, 6.15 |
| D37 | K, nenhum WARN | `add-relatorio-semanal`, `qa.md` | Não é risco |
| D38 | L, ambientes não reaproveitados | `add-relatorio-semanal`, `qa.md` | Resolvido: o aquecimento tirou a primeira chamada lenta da invocação (6.15) |
| D39 | M, rejeição de data com stack trace | `add-relatorio-semanal`, `qa.md` | R29. A parte de `Errors` foi resolvida: o alarme usa o marcador, e a rejeição não tem marcador |
| D40 | N, execuções adicionais deixam objetos e e-mails | `add-relatorio-semanal`, `qa.md` | R18 |
| D41 | O, README desatualizado | `add-relatorio-semanal`, `qa.md` | Resolvido neste change, pela reescrita do README (tarefa 5.1) |
| D42 | P, bucket e conta no aviso | `add-relatorio-semanal`, `qa.md` | R27 e R22 |
| D43 | A, consultas bloqueadas pelo SSO | `add-observabilidade-seguranca`, `qa.md` | Resolvido |
| D44 | B, WARN real fora da prova local | `add-observabilidade-seguranca`, `qa.md` | Resolvido |
| D45 | C, `Computação com 1024 MB` | `add-observabilidade-seguranca`, `qa.md` | Resolvido: tarefa 6.19 conferida pelo Gabriel no console em 07/11/2026 |
| D46 | D, destino das ações de alarme | `add-observabilidade-seguranca`, `qa.md` | Resolvido |
| D47 | E, registro do hook só por revisão | `add-observabilidade-seguranca`, `qa.md` | Resolvido |
| D48 | F, `RuntimeException` que escapa da notificação | `add-observabilidade-seguranca`, `qa.md` | R42 |
| D49 | G, linha do `Error` fora do `withLogging` | `add-observabilidade-seguranca`, `qa.md` | Não é risco: não afeta o filtro |
| D50 | H, lista do `Deny` contra a referência | `add-observabilidade-seguranca`, `qa.md` | Não é risco: lacuna de conferência, citada na seção de TLS de `docs/seguranca.md` |
| D51 | I, texto do tempo esgotado no `java25` | `add-observabilidade-seguranca`, `qa.md` | R31 |
| D52 | J, arquivos de outro agente | `add-observabilidade-seguranca`, `qa.md` | Não é risco: só valia para a rodada local |
| D53 | K, volta a OK em cerca de 10 minutos | `add-observabilidade-seguranca`, `qa.md` | Resolvido neste change: `docs/monitoramento.md` usa 10 a 11 minutos. O atraso em si é R36 |
| D54 | L, linhas de teste com acento e `RESTORE_REPORT` sintético | `add-observabilidade-seguranca`, `qa.md` | R38 |
| D55 | M, dashboard conferido pela API | `add-observabilidade-seguranca`, `qa.md` | Não é risco |
| D56 | N, relógio local adiantado | `add-observabilidade-seguranca`, `qa.md` | Não é risco |
| D57 | O, efeitos colaterais da 6.15 | `add-observabilidade-seguranca`, `qa.md` | Não é risco: dados de teste em dev |
| D58 | P, custo do aquecimento na publicação | `add-observabilidade-seguranca`, `qa.md` | Não é risco: dentro do limite de 130 s, fora da invocação do usuário |

### E. Risks de `add-documentacao-entrega`

| # | Item | Fonte | Destino |
|---|---|---|---|
| E1 | Dado do aluno em prod até depois da nota | `add-documentacao-entrega`, design | R17 |
| E2 | E-mail do autor nos commits | `add-documentacao-entrega`, design | R43 |
| E3 | Vídeo gera e-mail e dado real em prod | `add-documentacao-entrega`, design | R39 |

Os outros itens de Risks deste change (prazo, documentos começados antes dos ajustes, versões novas da 1.0.1, tabela de estado, `sam local`, Mermaid no PDF, data no caminho arquivado e a própria cobertura) são de entrega e ficaram fora do registro.

## Leituras de 10 e 11/11/2026 citadas pelos documentos

Registradas pelo QA em 11/11/2026. Todas foram somente leitura: `describe-*`, `get-*`, `list-*`, `get-metric-statistics`, `filter-log-events`, `head-object`, `get-object` e `scan --select COUNT` na AWS, e `gh api` com GET no GitHub. Nenhuma invocação, chamada à API, publicação, gravação ou escrita no Git.

A coluna Fonte separa duas origens:
- **QA:** leitura feita pelo QA nesta rodada, de 21:44 UTC de 10/11 a 03:01 UTC de 11/11, com uma pausa no meio. Em Brasília, a rodada inteira caiu em 10/11, das 18:44 às 00:01. Por isso os documentos falam ora em 10/11, ora em 11/11, sobre a mesma leitura.
- **Documentador, 10 e 11/11/2026 (UTC):** leitura feita pelo documentador na escrita de `docs/monitoramento.md` e `docs/seguranca.md`. O QA não a repetiu, e o resultado abaixo é o que o documento registra.

### AWS

| Leitura | Fonte | Resultado | Detalhe |
|---|---|---|---|
| Stack, funções, alias e boundary de prod | QA | `feedback-prod` em `CREATE_COMPLETE`. As três funções com o `live` na versão 1, `Active`, `On`, 1024 MB e boundary `feedback-limite-funcoes-prod` | `add-observabilidade-seguranca/qa.md`, "Prod, release 1.0.0 (leitura)", Funções |
| Retenção em prod | QA | 30 dias nos três grupos `/aws/lambda/feedback-*-prod` | Mesma seção, 8.3 |
| Retenção em dev e hml | Documentador, 10 e 11/11/2026 (UTC) | 14 dias nos três grupos de cada ambiente. As leituras anteriores do QA deram o mesmo, em 06/11 para dev (6.5) e em 07/11 para hml (`add-fundacao-projeto/qa.md`, release, item 1) | `docs/monitoramento.md`, tabela de retenção |
| Alarmes de prod | QA | Os 3 em OK, `Sum`, 60 s, 5 períodos, 1 ponto, limiar 1, `notBreaching`, ação só em `feedback-alarmes-prod`, `OKActions` e `InsufficientDataActions` vazias. Histórico só com a criação | `add-observabilidade-seguranca/qa.md`, "Alarmes de prod" |
| Alarmes de dev e hml | QA, só o do relatório; o restante, documentador, 10 e 11/11/2026 (UTC) | O QA leu os 9 alarmes `feedback-*` de `sa-east-1`, 3 por ambiente, e conferiu o do relatório de dev e de hml em OK, sem histórico de 08/11 a 10/11. A configuração completa dos 9 é a leitura do documentador | `add-relatorio-semanal/qa.md`, "Execução agendada real, 09/11/2026"; `docs/monitoramento.md` |
| Contagem na conta: 9 alarmes em `sa-east-1`, 1 em `us-east-1`, nenhum composto, 3 dashboards | Documentador, 10 e 11/11/2026 (UTC), exceto os 9 de `sa-east-1`, também lidos pelo QA | Como na coluna Leitura | `docs/monitoramento.md`, custo |
| Filtros de métrica nos três ambientes | Documentador, 10 e 11/11/2026 (UTC) | Padrão `?FALHA_FUNCAO ?"Task timed out" ?"Status: timeout"`, valor `1`, namespace `FeedbackServerless/<amb>` | `docs/monitoramento.md` |
| Dashboards `feedback-painel-dev`, `-hml` e `-prod` | Documentador, 10 e 11/11/2026 (UTC) | Os três existem. A consulta de restaurações rodou nos grupos de dev com `--log-group-names` | `docs/monitoramento.md` |
| Execução agendada de 09/11 às 11:00 UTC | QA | Disparo nos três ambientes, uma invocação por função, relatório idêntico byte a byte à tabela, aviso entregue, alarmes em OK | `add-relatorio-semanal/qa.md`, "Execução agendada real, 09/11/2026" |
| `filter-log-events` no grupo do relatório de prod | QA | 8 eventos de 10:30 a 12:30 UTC de 09/11, sem `FALHA_FUNCAO`, sem WARN nem ERROR. O `REPORT` traz `Sampled: true` | Mesma seção |
| Trace do X-Ray de prod de 09/11 às 11:00 UTC | Documentador, 10 e 11/11/2026 (UTC) | Pelo `xray_trace_id` da linha `Relatório gerado`: `## lerAvaliacoes` com sete `DynamoDB`, `## gravarRelatorio` com `S3`, `## publicarAviso` com `SNS` e `Restore`. O QA não abriu o trace | `docs/monitoramento.md`, X-Ray |
| Agendamento, assinaturas, tabela e bucket de prod | QA | Agendamento `ENABLED`. Assinaturas `email` confirmadas nos dois tópicos (1, 0, 0). Tabela com `Count` 0. Bucket só com `relatorios/2026-11-09.txt` | `add-observabilidade-seguranca/qa.md`, "Prod, release 1.0.0 (leitura)" |

### GitHub

O QA repetiu estas leituras às 03:01 UTC de 11/11, e elas confirmam o que o documentador registrou em 10 e 11/11/2026 (UTC).

| Leitura | Fonte | Resultado |
|---|---|---|
| Proteção de `main` | QA e documentador, 10 e 11/11/2026 (UTC) | Check obrigatório `ci`, `strict: false`, `enforce_admins: true`, `required_approving_review_count` 0, ou seja, sem aprovação obrigatória, force push e exclusão desligados |
| Proteção de `develop` | QA e documentador, 10 e 11/11/2026 (UTC) | Igual a `main` |
| Ambiente `prod` | QA e documentador, 10 e 11/11/2026 (UTC) | `required_reviewers` com `g-andradd`, `prevent_self_review: false`, e `branch_policy` |
| Ambientes `dev` e `hml` | QA e documentador, 10 e 11/11/2026 (UTC) | Só `branch_policy`, sem revisor |
| `can_admins_bypass` | QA e documentador, 10 e 11/11/2026 (UTC) | `true` nos três ambientes |
| Política de branch | QA, só os indicadores; a lista de branches por ambiente, documentador, 10 e 11/11/2026 (UTC) | `custom_branch_policies: true` e `protected_branches: false` nos três ambientes |
| Deploy 42438770120 | QA e documentador, 10 e 11/11/2026 (UTC) | Workflow Deploy, `push` em `main`, commit `6eee512`, `success`. Aprovação `approved` por `g-andradd` no ambiente `prod`. Job `resolver-ambiente` das 02:29:40 às 02:29:44 UTC e job `deploy` das 02:56:36 às 03:02:06 UTC de 08/11, então a aprovação saiu entre os dois |
| Tag `v1.0.0` | QA e documentador, 10 e 11/11/2026 (UTC) | Tag anotada, que aponta para o commit `6eee512`, o mesmo de `origin/main` e do deploy de prod |
| `actions/oidc/customization/sub`, secrets e variáveis | Documentador, 10 e 11/11/2026 (UTC) | `use_immutable_subject: true`. Nenhum secret nem variável no repositório. Nos três ambientes, o secret `EMAIL_ADMIN` e a variável `AWS_ROLE_ARN` |

### Divergência de data entre os documentos

Resolvida pela convenção de 11/11/2026: datas em UTC, sempre com "(UTC)" quando são de leitura ou conferência. As leituras do documentador, que não têm horário registrado, aparecem como "10 e 11/11/2026 (UTC)" nesta seção e em todos os documentos. A leitura de prod do QA, de 21:44 UTC de 10/11 a 02:55 UTC de 11/11, aparece como "11/11/2026 (UTC)" no README, no relatório e no roteiro, que citam a releitura em bloco das 02:55 UTC de 11/11, e como "10 e 11/11/2026 (UTC)" em `docs/monitoramento.md`, que cita a rodada inteira. As duas formas descrevem a mesma rodada.

## Leitura de dev e hml (tarefa 2.1)

Feita pelo QA em 11/11/2026, das 03:13 às 03:31 UTC (00:13 às 00:31 em Brasília), no worktree `fiap-tc4-documentacao`, com o perfil `tc4-admin` em `sa-east-1`. Só `describe-*`, `get-*`, `list-*`, `get-metric-statistics`, `filter-log-events`, `scan --select COUNT` e `list-objects-v2`. Nenhuma invocação, nenhum `set-alarm-state`, nenhuma publicação, nenhuma gravação. A leitura de prod é a de "Prod, release 1.0.0 (leitura)" em `add-observabilidade-seguranca/qa.md`; aqui só se repetiu o status da stack e a listagem do bucket de prod.

### Stacks

| Stack | Status | Criação (UTC) | Última atualização (UTC) |
|---|---|---|---|
| `feedback-dev` | `UPDATE_COMPLETE` | 03:22:20 de 04/11 | `UPDATE_IN_PROGRESS` às 21:35:32 de 10/11, `UPDATE_COMPLETE` às 21:37:09 de 10/11 |
| `feedback-hml` | `CREATE_COMPLETE` | 03:48:35 de 07/11 | nenhuma depois da criação |
| `feedback-prod` | `CREATE_COMPLETE` | 08/11 | sem mudança |

**Deploy de dev de 10/11, que o README dá como sem conferência registrada.** Execução 42906193011 do workflow Deploy, evento `push` em `develop`, commit `6d7c993` (merge do PR #16), `success`, das 21:33:27 às 21:37:18 UTC. Job `resolver-ambiente` das 21:33:30 às 21:33:34 e job `deploy` das 21:33:40 às 21:37:17, os dois com `success`. O deploy anterior de dev que mudou a stack foi o de 06/11, às 19:19 UTC; o de 07/11 (execução 42334114084) não mexeu nela.

O deploy publicou versão nova **das três funções**, e não só do relatório:

| Função de dev | `live` em 06/11 (6.4 de `add-observabilidade-seguranca`) | `live` agora | `LastModified` da versão (UTC) |
|---|---|---|---|
| `feedback-registrar-avaliacao-dev` | 7 | 8 | 21:35:39 de 10/11 |
| `feedback-notificar-critica-dev` | 4 | 5 | 21:35:39 de 10/11 |
| `feedback-gerar-relatorio-semanal-dev` | 2 | 3 | 21:35:39 de 10/11 |

Log das versões novas, de 21:30 UTC de 10/11 até a leitura: 12, 12 e 16 eventos, todos de inicialização (`INIT_START`, INFO do X-Ray SDK, `Cliente aquecido`, `INIT_REPORT`). Linhas `Cliente aquecido`: 2 no registro, 2 na notificação e 6 no relatório, ou seja, duas inicializações por função: a da publicação, às 21:35:49 e 21:35:50, e uma segunda fora de invocação, entre 21:47:54 e 21:50:49, como a observação Q de `add-fundacao-projeto/qa.md`. Nenhum WARN, nenhum ERROR, nenhum `FALHA_FUNCAO`. O filtro `?ERROR ?WARN ?FALHA_FUNCAO ?Exception` devolveu 2 linhas por grupo, as duas INFO do X-Ray SDK com o texto `LOG_ERROR` no valor de `AWS_XRAY_CONTEXT_MISSING`. `Invocations` igual a 0 nas três funções desde 21:30 UTC de 10/11. As versões novas não têm conferência funcional: só esta leitura.

### Funções, alias `live` e boundary

`get-function-configuration` com `--qualifier live` e `iam get-role` na role de cada função:

| Função | `live` | `State` | `OptimizationStatus` | `MemorySize` | `Timeout` | Runtime | Tracing | Boundary da role |
|---|---|---|---|---|---|---|---|---|
| `feedback-registrar-avaliacao-dev` | 8 | `Active` | `On` | 1024 | 10 | `java25` | `Active` | `feedback-limite-funcoes-dev` |
| `feedback-notificar-critica-dev` | 5 | `Active` | `On` | 1024 | 30 | `java25` | `Active` | `feedback-limite-funcoes-dev` |
| `feedback-gerar-relatorio-semanal-dev` | 3 | `Active` | `On` | 1024 | 120 | `java25` | `Active` | `feedback-limite-funcoes-dev` |
| `feedback-registrar-avaliacao-hml` | 1 | `Active` | `On` | 1024 | 10 | `java25` | `Active` | `feedback-limite-funcoes-hml` |
| `feedback-notificar-critica-hml` | 1 | `Active` | `On` | 1024 | 30 | `java25` | `Active` | `feedback-limite-funcoes-hml` |
| `feedback-gerar-relatorio-semanal-hml` | 1 | `Active` | `On` | 1024 | 120 | `java25` | `Active` | `feedback-limite-funcoes-hml` |

Em hml, as três funções seguem só com `$LATEST` e a versão 1, de 07/11. Nenhuma invocação em hml desde 21:30 UTC de 10/11.

### Retenção, alarmes, agendamento e assinaturas

| Item | dev | hml |
|---|---|---|
| Retenção dos três grupos `/aws/lambda/feedback-*-<amb>` | 14 dias | 14 dias |
| Alarmes `feedback-falha-*-<amb>` | os 3 em OK | os 3 em OK |
| Configuração dos alarmes | `Sum`, 60 s, 5 períodos, 1 ponto, limiar 1, `GreaterThanOrEqualToThreshold`, `notBreaching`, métrica em `FeedbackServerless/dev`, `AlarmActions` só com `feedback-alarmes-dev`, `OKActions` e `InsufficientDataActions` vazias | igual, com `FeedbackServerless/hml` e `feedback-alarmes-hml` |
| Histórico dos alarmes desde 08/11 | nenhum item nos 3 | nenhum item nos 3 |
| Agendamento | `feedback-relatorio-semanal-dev`, `ENABLED`, `cron(0 8 ? * MON *)`, `America/Sao_Paulo`, alvo no alias `live` | `feedback-relatorio-semanal-hml`, igual, alvo no `live` de hml |
| Tópico de administradores | `feedback-admins-dev`: 1 assinatura `email` confirmada; 1, 0, 0 | `feedback-admins-hml`: 1 `email` confirmada; 1, 0, 0 |
| Tópico de alarmes | `feedback-alarmes-dev`: 1 `email` confirmada; 1, 0, 0 | `feedback-alarmes-hml`: 1 `email` confirmada; 1, 0, 0 |
| Tabela, `scan --select COUNT` | `Count` 43, `ScannedCount` 43 | `Count` 2, `ScannedCount` 2 |
| Bucket | 9 objetos: 5 em `aws/lambda/`, de 05/11, e 4 em `relatorios/` (`2026-10-26`, `2026-11-06`, `2026-11-07` e `2026-11-09`) | 2 objetos: `relatorios/2026-11-08.txt` e `relatorios/2026-11-09.txt`, 568 bytes cada |

A coluna de assinaturas traz `SubscriptionsConfirmed`, `Pending` e `Deleted`. O endereço não foi impresso. A tabela de dev continua com os 43 itens do fim do QA de 06/11, e a de hml com os 2 itens de 07/11: o deploy de 10/11 não gravou nada. Prod, relido agora: stack `CREATE_COMPLETE` e bucket só com `relatorios/2026-11-09.txt`, de 351 bytes.

Com esta leitura, a retenção e a configuração completa dos 6 alarmes de dev e hml, que a seção anterior atribuía só ao documentador, passam a ter leitura do QA, com o mesmo resultado.

## Build e testes (tarefa 2.3)

`mvn -B verify` no worktree, branch `feature/add-documentacao-entrega`, commit `52e7309` com as alterações de documentação ainda não commitadas (nenhum arquivo de código, template ou `pom.xml` alterado). JDK Microsoft OpenJDK 25.0.5.0.2, definido no comando. 11/11/2026, 03:14 UTC.

| Módulo | Testes | Falhas | Erros | Ignorados |
|---|---|---|---|---|
| `dominio` | 171 | 0 | 0 | 0 |
| `registrar-avaliacao` | 140 | 0 | 0 | 0 |
| `notificar-critica` | 78 | 0 | 0 | 0 |
| `gerar-relatorio-semanal` | 95 | 0 | 0 | 0 |
| **Total** | **484** | **0** | **0** | **0** |

`BUILD SUCCESS` em 36 s. Os números são os mesmos de `docs/relatorio-tech-challenge.md` (171, 140, 78, 95 e 484) e os da release 1.0.0 sobre `b730a59`.

No mesmo estado, os outros comandos do README também terminaram com sucesso: `mvn -B install`, `sam build` (SAM CLI 1.171.0, `Build Succeeded`), `sam validate --lint --region sa-east-1` (`template.yaml is a valid SAM Template`) e `sam validate --lint --region sa-east-1 -t infra/bootstrap/github-oidc.yaml` (válido). `openspec validate add-documentacao-entrega --strict` e `openspec validate --all --strict` passaram (6 de 6).

## Verificação da seção 6

Feita pelo QA em 11/11/2026, entre 03:13 e 03:32 UTC, sobre o disco do worktree. `CHANGELOG.md` e `docs/monitoramento.md` foram lidos na versão de 03:12 UTC, a última do documentador, e não mudaram até o fim da verificação. Nenhum documento foi corrigido pelo QA.

### 6.1. Cenários da spec `documentacao`

| Requisito | Cenário | Status | Evidência ou falha |
|---|---|---|---|
| README como porta de entrada | Índice completo | ok | Tabela Documentação, linhas 91 a 96, com os cinco documentos e a coleção |
| | Links que resolvem | ok | 6.3: 246 links, 0 falhas |
| | Fonte única | ok | Alarmes só em `docs/monitoramento.md`, permissões só em `docs/funcoes.md`, riscos só em `docs/seguranca.md`; o README aponta para os três e não tem nenhuma dessas tabelas |
| Estado real das funções | Tabela completa | ok | Nove linhas, 72 a 80, cada uma com estado, data e fonte |
| | Afirmação com evidência | ok | Cada fonte citada registra a conferência no ambiente e na data da célula (conferido nas seções 6.4 e 6.15 e "Prod, release 1.0.0 (leitura)" de `add-observabilidade-seguranca`, "Release 1.0.0 em hml (07/11/2026)" de `add-fundacao-projeto` e "Execução agendada real, 09/11/2026" de `add-relatorio-semanal`) |
| | Sem estado desatualizado | **falha** | README, linha 83, diz que só o relatório de dev ganhou versão nova em 10/11 e que ela não tem conferência registrada: as três ganharam, e a leitura está na seção "Leitura de dev e hml" acima. O relatório, linha 218, não cita o deploy de 10/11 em dev. Correções na lista de problemas |
| | Ambiente removido | ok (não se aplica) | Nenhuma stack removida: as três existem na leitura de 11/11 |
| | Plano de remoção | ok | README, linhas 85 e 297 |
| Instruções de implantação | Ordem dos passos | ok | Passos 1 a 7, na ordem do requisito, cada um com comando ou configuração |
| | Mapeamento de branch e ambiente | ok | `deploy.yml`, linhas 6 a 9 e 28 a 30; `samconfig.toml`, `stack_name` das três seções; políticas de branch dos ambientes lidas agora: `develop`, `release/*` e `main` |
| | Build documentado roda | ok | Seção "Build e testes" acima |
| Documentação das funções | Itens por função | ok | As três seções têm as 13 subseções da D3 |
| | Permissões conferem com o template | ok | Statement por statement: registro 2 de 2 (`template.yaml`, 157 a 164), notificação 5 de 5 (195 a 227), relatório 6 de 6 (272 a 303), com ações, recursos e condições iguais; nenhuma permissão a mais |
| | Erros conferem com as specs | ok | Todos os casos de erro das três specs aparecem; códigos e mensagens iguais aos de `Respostas.java` e do handler do relatório |
| | Configuração confere com o template | ok | Memória 1024 (`Globals`), timeouts 10, 30 e 120, os três gatilhos e as variáveis com nome e origem iguais ao template |
| | Variáveis sem valor sensível | ok | Só nome e origem (`!Ref`, `!Sub`) |
| Segurança e governança | Temas cobertos | ok | Uma seção para cada um dos oito temas |
| | Governança confere com o GitHub | ok | `gh api` GET às 03:2x UTC: `ci`, `strict: false`, `enforce_admins: true`, 0 aprovações, force push e exclusão desligados em `main` e `develop`; `can_admins_bypass: true` nos três ambientes; revisor `g-andradd` e `prevent_self_review: false` em prod; políticas `develop`, `release/*` e `main`; repositório público, padrão `develop` |
| | Onde fica o dado do aluno | ok | Tabela das linhas 190 a 197, com criptografia e expiração de cada lugar |
| Registro de riscos aceitos | Riscos obrigatórios | ok | Os 16 estão no registro: R06, R07, R30, R42, R31, R08, R29, R10, R01, R02, R03, R17, R27, R16, R09, R32 |
| | Cobertura dos registros do OpenSpec | ok | 6.2 |
| | Campos preenchidos | ok | 43 linhas, nenhuma célula vazia nas sete colunas |
| | Pendência pós-entrega do aspectjrt | **falha** | A linha R08 (`docs/seguranca.md`, 315) não traz as três condições: a coluna Revisão só aponta para a seção da pendência. As condições estão na seção das linhas 289 a 298 e no `project.md` |
| Monitoramento | Temas cobertos | ok | Logs, Métricas, Alarmes, Dashboard, X-Ray, Teste de alarme e Limites do nível gratuito |
| | Alarmes conferem com o template | ok | Padrão, `Sum`, 60, 5, 1, limiar 1, `notBreaching` e ação só no tópico de alarmes iguais a `template.yaml`, 378 a 463; nomes dos filtros iguais a `FilterName` |
| | Teste de alarme | ok | Os dois testes, com registro nos `qa.md`, aviso de e-mail real e os tempos de 60 a 80 s e de 10 a 11 min |
| | Limites do nível gratuito | ok | 10 de 10 e 3 de 3 e a proibição, linhas 358 a 386 |
| Roteiro do vídeo | Exigência do enunciado | ok | Blocos 2, 3 e 4 |
| | Recursos de produção | ok | Os 9 blocos de comando usam `feedback-prod` ou o sufixo `-prod` |
| | Tabela de produção começa limpa | ok | Passo 4 da preparação; nenhum passo da preparação grava |
| | Pré-condições de produção | ok | "Quando gravar" e passos 1 a 3 |
| | O que desfocar | **falha** | `docs/roteiro-video.md`, linha 23, fala em cabeçalho e rodapé, e a spec pede o endereço "no corpo e no rodapé das mensagens". O resto da lista confere |
| | Depois da gravação | ok | Linhas 357 a 377 |
| Relatório do Tech Challenge | Formato e local | ok | `docs/relatorio-tech-challenge.md` existe; 0 arquivos `.pdf`, `.doc` ou `.docx` entre os versionados e os novos |
| | Seções na ordem | ok | Os onze títulos de nível 2 na ordem do requisito |
| | Seções extras apontam para docs | ok | Linhas 423, 435 e 447 |
| | Diagrama da arquitetura | ok no arquivo | Mermaid com todos os componentes e ligações pedidos. A renderização no GitHub só pode ser vista depois do push, e é item do Gabriel em "Depois do commit de entrega" |
| | Endpoints conferem com a spec | ok | Os quatro casos, com os corpos de `Respostas.java`, e a URL com `<ID_DA_API>` |
| | Coleção Postman | ok | 33 requisições contadas no JSON (12, 7, 9, 1 e 4), `baseUrl` sem `/avaliacao`, ambientes conforme `add-registro-avaliacao/qa.md` |
| | Repositório público | ok | `curl` sem autenticação devolveu 200 na página e na API pública |
| | Números de qualidade | ok | Iguais aos da seção "Build e testes". A frase da linha 319 ainda cita a execução sobre `b730a59`: ver a lista de problemas |
| Execução local documentada | Instrução marcada como não executada | ok | Linha 283 |
| | Evento coerente com a spec | ok | Descrição vazia e nota 11 dão 400 citando os dois campos |
| | Limites da execução local | ok | Linhas 301 a 311 |
| Arquivamento dos changes | Specs consolidadas; Requisitos preservados; Specs válidas; Nenhum change ativo; Evidência da release preservada; Nenhuma tarefa aberta | **falha (pendente)** | O arquivamento ainda não rodou: `openspec list` mostra os seis changes ativos. Ficam para as tarefas 7.4 e 8.2 |
| | Pendência fora das tarefas | ok, a repetir depois do arquivamento | A seção "Pendências pós-entrega" do `project.md` traz as três condições, e nenhum `tasks.md` traz a tarefa de rever o desvio |
| Validações do repositório | Build e templates | ok | Seção "Build e testes" |
| | OpenSpec | **falha (pendente)** | `validate --all --strict` passou; `validate --archived` só faz sentido depois do arquivamento |
| | CI do pull request | **falha (pendente)** | O PR ainda não existe |
| Publicação na 1.0.1 | Entrada do changelog | **falha** | `CHANGELOG.md`, linha 17, diz "Nenhuma mudança de comportamento nas funções, no template ou nos pipelines", que não é o mesmo que dizer que código, template e pipelines não mudaram |
| | Conferência em prod depois da release | **falha (pendente)** | Depende da release 1.0.1, depois do commit de entrega |
| Sigilo | ID da conta; Identificador da API; E-mail dos administradores | ok | 6.4 |
| Escrita | Sem travessão | ok | 6.5 |
| Comandos verificados | Origem de cada comando | ok | 6.5 |
| Somente documentação | Diferença da branch | **falha** | 6.7: `grep.exe.stackdump` na raiz, fora dos caminhos permitidos |

### 6.2. Cobertura dos riscos

- **Bloco A:** as seções Risks dos cinco designs têm 6, 7, 8, 8 e 15 itens, 44 no total, na mesma ordem de A1 a A44.
- **Bloco C:** as seções de decisão e de risco aceito são as de `add-fundacao-projeto` (O3, O4, O5), `add-registro-avaliacao` (defeito D1 e decisões A a D de 04/11) e `add-observabilidade-seguranca` ("Risco aceito"). `add-notificacao-critica` e `add-relatorio-semanal` não têm seção de decisão nem de risco aceito. 9 itens, como C1 a C9.
- **Bloco D:** defeitos e observações dos cinco `qa.md`: `add-fundacao-projeto` com D1, O1, O2, O6, O7, Q e R (O3 a O5 estão no bloco C); `add-registro-avaliacao` com E a J; `add-notificacao-critica` com A a M; `add-relatorio-semanal` e `add-observabilidade-seguranca` com A a P cada. 7, 6, 13, 16 e 16, 58 no total, como D1 a D58.
- **Bloco E:** a seção Risks deste change tem 11 itens; 3 viraram R e os 8 de entrega estão listados no fim da tabela.
- **Totais por destino**, recontados nas tabelas: A 36, 4 e 4; B 10, 0 e 0; C 6, 2 e 1; D 13, 21 e 24; E 3, 0 e 0. Iguais à tabela de totais.
- **Registro:** 43 linhas, R01 a R43. Todo id citado na cobertura existe no registro, e todo id do registro recebe ao menos um item. Origem de cada R conferida por amostra (A2, A6, A15, B6, D3, D15, D26, C9 e D48), sem divergência.
- **Campos:** nenhuma célula vazia. R43 tem "Nenhuma" na mitigação, com o motivo na coluna anterior, como a D4 pede.

Resultado: ok.

### 6.3. Links e âncoras

Script Python temporário, fora do repositório, sem dependência externa, rodado com `python -I`. Lê `README.md`, `CHANGELOG.md` e os oito arquivos Markdown de `docs/`, ignora blocos de código, resolve cada link relativo contra `git ls-files -co --exclude-standard` e gera os slugs dos títulos do destino pela regra do GitHub (minúsculas, letras acentuadas mantidas, crase e pontuação removidas exceto `-` e `_`, espaço vira `-`, repetição com `-1`, `-2`). Casos de teste do próprio script: `#relatório-semanal-acionamento-manual-e-download`, `#esvaziar-o-bucket-antes-de-sam-delete` e `#7-remover-uma-stack`, os três aprovados.

| Item | Resultado |
|---|---|
| Links lidos | 246 |
| Arquivo inexistente | 0 |
| Âncora inexistente | 0 |
| URL do repositório sem autenticação | 200 (`curl -s -o /dev/null -w "%{http_code}"`) |

Também conferido, por um segundo script: cada nome de seção citado entre aspas junto de um link para `qa.md` existe como título ou como linha de cenário no arquivo apontado.

**Links para `openspec/changes/<change>/`, que mudam no arquivamento** (não contados como falha):

| Arquivo | Links | Além do destino |
|---|---|---|
| `README.md` | 25 | |
| `docs/funcoes.md` | 39 | |
| `docs/monitoramento.md` | 20 | 5 deles têm o caminho também no texto do link |
| `docs/relatorio-tech-challenge.md` | 7 | os 7 têm o caminho no texto do link, e a linha 222 cita `openspec/changes/archive/2026-11-11-add-documentacao-entrega/qa.md` em texto, fora de link |
| `docs/roteiro-video.md` | 19 | |
| `docs/seguranca.md` | 2 | |
| **Total** | **112** | |

### 6.4. Sigilo

O comando da D9, com os valores em variáveis do shell e só contagens impressas. A conta tem 12 dígitos; `get-apis` devolveu 3 APIs `feedback-api-*`, os mesmos 3 identificadores do output `ApiAvaliacoesUrl` das três stacks; as assinaturas dos tópicos de administradores trouxeram 1 endereço único.

| Valor | Arquivos de `git ls-files -co --exclude-standard` com o valor | Linhas nos diffs de `git log -p --all` (informativo) |
|---|---|---|
| ID da conta | 0 | 0 |
| API 1 | 0 | 0 |
| API 2 | 0 | 0 |
| API 3 | 0 | 0 |
| E-mail | 0 | 0 |

Resultado: ok. A primeira contagem do histórico deu 69 linhas para o e-mail porque `git log -p` inclui o cabeçalho dos commits. Com `--format=` (só os diffs), 0. Nos metadados, o endereço é o autor ou o committer em 138 entradas: é o R43, e o endereço do `EMAIL_ADMIN` é o mesmo do autor do Git. Reescrever o histórico está fora do escopo; fica para o Gabriel.

### 6.5. Travessão e origem dos blocos de comando

**Travessão (D12).** O comando da D12 sobre os 19 arquivos alterados ou novos devolveu um único arquivo, `openspec/project.md`, com uma ocorrência, na linha 131, que enuncia a regra. É a exceção que a spec permite. Os documentos de entrega e o `CHANGELOG.md` têm 0. Resultado: ok. A D12 diz "Esperado: nenhuma saída", o que diverge da spec; vale a spec.

**Origem dos blocos (D10).** Script temporário que lista cada bloco cercado e, para os de linguagem `bash`, a linha não vazia anterior:

| Arquivo | Blocos `bash` | Com `Registro:` ou `Não executado` |
|---|---|---|
| `README.md` | 8 | 8 |
| `docs/funcoes.md` | 7 | 7 |
| `docs/monitoramento.md` | 7 | 7 |
| `docs/relatorio-tech-challenge.md` | 2 | 2 |
| `docs/roteiro-video.md` | 9 | 9 |
| `docs/seguranca.md` | 2 | 2 |
| `docs/incidentes/2026-11-04-snapstart-aspectjrt.md` | 0 | 0 |

Os outros blocos usam `text`, `json` ou `mermaid`, menos os dois do incidente (linhas 11 e 58), que não declaram linguagem. São mensagens de erro, não comandos, então não entram na D10. Resultado: ok.

### 6.6. Mermaid

O bloco `mermaid` do README (linhas 11 a 56) e o do relatório (linhas 41 a 86), extraídos com `awk`, têm 44 linhas cada, o mesmo SHA-256 e nenhuma diferença no `diff`, sem `\r`. Resultado: ok. A abertura no GitHub fica para depois do push.

### 6.7. Diferença da branch

`git diff --name-only origin/develop` mais os arquivos novos não ignorados: `README.md`, `CHANGELOG.md`, `docs/funcoes.md`, `docs/incidentes/2026-11-04-snapstart-aspectjrt.md`, `docs/monitoramento.md`, `docs/relatorio-tech-challenge.md`, `docs/roteiro-video.md`, `docs/seguranca.md`, nove arquivos de `openspec/changes/`, `openspec/project.md` e **`grep.exe.stackdump`**, na raiz. Nenhum arquivo de `functions/`, `template.yaml`, `samconfig.toml`, `.github/` ou `infra/`, nem `pom.xml`.

Resultado: **falha**. `grep.exe.stackdump` é o despejo de memória de um `grep` que abortou no Git Bash. Já existia antes desta verificação e foi reescrito às 03:21 UTC por um `grep` desta rodada que abortou do mesmo jeito. Não tem dado sigiloso (0 na contagem da 6.4), mas não é caminho permitido e o `.gitignore` não o cobre: precisa ser apagado antes do commit. Os diretórios `target/` do `mvn` e o `.aws-sam/` do `sam build` são ignorados.

### Datas da leitura de prod

A leitura de prod do QA foi de 21:44 UTC de 10/11 a 02:55 UTC de 11/11, com a releitura em bloco às 02:55 UTC de 11/11 (23:55 de 10/11 em Brasília). O README declara as datas em UTC.

| Documento | Linhas | Forma usada |
|---|---|---|
| `README.md` | 68, 74, 77 a 80 | 11/11/2026, com a nota "Datas em UTC" |
| `docs/roteiro-video.md` | 5, 51, 63, 83, 95 | 11/11/2026, sem dizer o fuso |
| `docs/relatorio-tech-challenge.md` | 220, 354, 399 | 11/11/2026, sem dizer o fuso |
| `docs/monitoramento.md` | 24, 26, 75, 137, 216, 263, 365, 367 | "10 e 11/11/2026 (UTC)" |
| `docs/monitoramento.md` | 64 | "11/11/2026 (UTC)" |
| `docs/seguranca.md` | 20, 35, 44, 237, 265 | 10/11/2026, para as leituras do GitHub feitas pelo documentador. O QA repetiu as do GitHub às 03:01 UTC de 11/11 |

A seção "Divergência de data entre os documentos", acima, ficou desatualizada: `docs/monitoramento.md` não usa mais "10/11/2026" para prod. E as leituras que esta qa.md atribui ao "Documentador, 10/11/2026" (filtros, dashboards, trace, contagem da conta e consulta de restaurações em dev) aparecem em `docs/monitoramento.md` como "10 e 11/11/2026 (UTC), na conferência deste documento". As duas fontes precisam dizer a mesma data.

### Problemas por arquivo

**`README.md`**
- Linha 83: diz que só "o alias `live` do relatório de dev passou a apontar para uma versão nova" e que "Essa versão não tem conferência registrada". As três funções de dev ganharam versão nova, e a leitura está nesta qa.md. Sugestão: "A stack `feedback-dev` recebeu um deploy às 21:35 UTC de 10/11/2026 (execução 42906193011, merge do PR #16), depois das conferências acima, e o alias `live` das três funções de dev passou a apontar para versões novas. A leitura de 11/11/2026 ([QA de add-documentacao-entrega](openspec/changes/archive/2026-11-11-add-documentacao-entrega/qa.md), seção "Leitura de dev e hml (tarefa 2.1)") encontrou as três em `Active`, `On` e 1024 MB, sem invocação desde o deploy. Essas versões não têm conferência funcional."
- Linhas 72 a 74, opcional: as linhas de dev descrevem a versão anterior. Se o Gabriel preferir a regra da D14 ao pé da letra, o estado de dev passa a "Implantada, sem conferência funcional", como em prod, com a conferência de 06/11 e de 09/11 citada como anterior ao deploy de 10/11.

**`docs/relatorio-tech-challenge.md`**
- Linha 218: "as três funções implantadas e conferidas entre 05/11 e 06/11/2026" não cita o deploy de 10/11. Sugestão: acrescentar "Em 10/11/2026, o merge do PR #16 publicou versões novas das três funções em dev, lidas em 11/11/2026 em `Active` e `On`, sem conferência funcional (`qa.md` de `add-documentacao-entrega`)."
- Linha 220: "A leitura somente de leitura" repete a palavra. Sugestão: "A conferência somente de leitura".
- Linha 222: promete o registro da conferência pós-1.0.1 neste `qa.md`, mas essa conferência acontece depois do commit de entrega e do arquivamento. Sugestão: "A conferência só de leitura do SnapStart nas três funções de prod é feita logo depois desse deploy, antes da gravação, e informada ao Gabriel. Ela acontece depois do commit de entrega e não está neste repositório."
- Linha 319: a fonte dos números é a execução sobre `b730a59`. A D7 pede a saída rodada pelo QA no estado final da branch. Sugestão: "Resultado do `mvn -B verify` no estado final da branch deste change, rodado pelo QA em 11/11/2026 com JDK 25 (`qa.md` de `add-documentacao-entrega`, seção "Build e testes (tarefa 2.3)"), com 0 falhas, 0 erros e 0 ignorados. São os mesmos números da release 1.0.0 (commit `b730a59`)."
- Datas (linhas 220, 354 e 399): 11/11/2026 sem fuso. Sugestão: "11/11/2026 (UTC)", como no README.

**`docs/roteiro-video.md`**
- Linha 23: sugestão: "**E-mails:** o endereço do assinante no cabeçalho, no corpo, se aparecer, e no rodapé de cancelamento do SNS, que traz também o ID da conta."
- Linha 377: "até 10/11/2026" diverge do README (linha 299, "até 11/11/2026"). Sugestão: "até 11/11/2026".
- Linhas 5, 51, 63, 83 e 95: sugestão: "11/11/2026 (UTC)".

**`docs/seguranca.md`**
- Linha 315, R08, coluna Revisão: trocar por "Remover o jar quando as três condições valerem: (1) existir uma release do Powertools com o commit `f628502`; (2) o `PrimingSnapStartTest` de cada função passar sem o jar; (3) um deploy confirmar `State: Active` e `OptimizationStatus: On` no alias `live` das três funções. São as mesmas da seção [Pendências pós-entrega do `openspec/project.md`](../openspec/project.md#pendências-pós-entrega) e da [pendência pós-entrega](#pendência-pós-entrega-desvio-do-aspectjrt)."

**`docs/monitoramento.md`**
- Linha 26: "como aconteceu na conferência de 10 e 11/11/2026 (UTC)" não tem registro em nenhum `qa.md` nem incidente. Sugestão: tirar a oração e deixar só a instrução e o efeito, ou o documentador entregar o registro à sessão principal.
- Linha 64 contra as linhas 24, 75, 137 e outras: "11/11/2026 (UTC)" e "10 e 11/11/2026 (UTC)" para a mesma leitura. Sugestão: uma forma só, "10 e 11/11/2026 (UTC)" ou a do README, "11/11/2026 (UTC)".
- Linhas 216, 263, 365 e 367: datam como "10 e 11/11/2026 (UTC), na conferência deste documento" leituras que esta qa.md registra como "Documentador, 10/11/2026". Alinhar com a sessão principal.
- Linha 137: pode passar a citar também a leitura do QA de dev e hml desta qa.md, que agora confere os seis alarmes.

**`CHANGELOG.md`**
- Linha 17: sugestão: "- Código, `template.yaml` e pipelines não mudaram. A única mudança fora da documentação é a versão dos `pom.xml`, feita na branch de release."
- Linha 5: a data `2026-11-12` é anterior à release. Conferir na hora da release.

**Raiz do worktree**
- `grep.exe.stackdump`: apagar antes do commit (6.7).

**`openspec/changes/archive/2026-11-11-add-documentacao-entrega/design.md`** (só registro, sem correção pedida)
- D12, linha 222: "Esperado: nenhuma saída", mas a spec permite a linha 131 do `project.md`, que o comando lista.

## Verificação final (11/11/2026)

Feita pelo QA no fim do dia 11/11/2026 (UTC), no worktree `fiap-tc4-documentacao`, depois da revisão do Gabriel e do arquivamento dos cinco changes em `openspec/changes/archive/2026-11-11-*`. Sem Git de escrita, AWS só leitura, nenhum documento corrigido.

### Resultado

| Verificação | Resultado | Detalhe |
|---|---|---|
| Links e âncoras, `checa.py` | ok | `erros 0`, 8 documentos. Conferido também com o script da 6.3: 263 links, 0 arquivos ou âncoras inexistentes. `[LINK DO VÍDEO]` (`docs/relatorio-tech-challenge.md:9`) não é link Markdown e é permitido pelo cenário "Cabeçalho do modelo da FIAP" |
| Links que ainda apontam para `openspec/changes/<change>/` | 7, todos para o `qa.md` deste change | `README.md:83`, `docs/monitoramento.md:24, 75, 216, 263, 367` e `docs/roteiro-video.md:39`. Mudam na tarefa 7.5. Nenhum link aponta para os cinco changes fora de `archive/` |
| Sigilo D9, AWS ao vivo | não executado | O token SSO do perfil `tc4-admin` expirou ("Token has expired and refresh failed"), então a conta, os IDs das APIs e o endpoint das assinaturas não puderam ser lidos na AWS. Substituído pela linha seguinte |
| Sigilo D9, com os valores das leituras registradas | ok | Ver "Sigilo D9 com valores das leituras registradas", abaixo |
| Sigilo, busca por padrão (substituto parcial) | ok | Nenhum host `https://<10 caracteres>.execute-api` e nenhum endereço de e-mail nos arquivos versionados e novos. Números de 12 dígitos só aparecem em marcadores de QA dos `qa.md` arquivados (data e hora `AAAAMMDDhhmm`) e em fixtures de teste de `functions/`, que não mudaram desde a 6.4, quando a busca pela conta real deu 0 |
| Imagem e fonte do diagrama | ok | `docs/img/arquitetura.mmd` sem número de 12 dígitos, `@`, `execute-api` nem `arn:`. `docs/img/arquitetura.png` só tem os chunks `IHDR`, `IDAT` e `IEND`, sem texto embutido. Aberta e lida: é o desenho do `.mmd`, sem conta, e-mail ou URL. O `grep -I` da D9 pula binários, então a imagem precisa desta conferência à parte |
| Travessão D12 | ok | 51 arquivos alterados ou novos; só `openspec/project.md:131`, a linha da regra |
| Marca D10 antes dos blocos `bash` | ok | 36 blocos: README 9, `funcoes` 7, `monitoramento` 7, relatório 2, roteiro 9, `seguranca` 2; todos com `Registro:` ou `Não executado`. 34 deles têm uma linha em branco entre a marca e o bloco |
| `mvn -B verify`, JDK 25.0.5.0.2 | ok | 171, 140, 78 e 95 testes, 484 no total, 0 falhas, 0 erros e 0 ignorados. `BUILD SUCCESS` em 28 s |
| `cmd.exe //c "sam validate --lint --region sa-east-1"` | ok | `template.yaml is a valid SAM Template` |
| Mesmo comando com `-t infra/bootstrap/github-oidc.yaml` | ok | `github-oidc.yaml is a valid SAM Template` |
| `openspec validate --all --strict` | ok | 8 de 8: o change ativo e as 7 specs |
| `openspec validate --archived` | ok | 5 de 5 |
| `openspec list` e `list --specs` | ok | Só `add-documentacao-entrega` ativo. Specs com 5, 4, 8, 15, 12, 14 e 6 requisitos, iguais à D15 |
| `git diff --name-only origin/develop` mais os novos | ok | 79 caminhos, todos em `README.md`, `CHANGELOG.md`, `docs/` ou `openspec/`. `docs/img/arquitetura.mmd` e `docs/img/arquitetura.png` entram como "arquivos de `docs/`". O `grep.exe.stackdump` já não existe |

**Linha em branco na D10.** A D10 diz "A linha não vazia imediatamente antes de cada bloco `bash`", então uma linha em branco entre a marca e o bloco é aceita: a marca é a última linha com texto antes da cerca. O `checa.py` aplica a mesma regra.

### Cenários que mudaram

| Cenário | Status | Evidência ou falha |
|---|---|---|
| Cabeçalho do modelo da FIAP | ok | Linhas 3 a 9: projeto, equipe com RM, curso e `[LINK DO VÍDEO]` |
| Seções na ordem | ok | `## 1.` a `## 13.` na ordem da spec, com "Processo de desenvolvimento" e "Aprendizados" (em primeira pessoa), e depois `## Apêndice: Evidências`. Os títulos 2 e 3 têm nome longo ("Arquitetura do Sistema", "Descrição dos Endpoints da API"), sem mudar o conteúdo pedido |
| Seções extras apontam para docs | ok | Seções 9, 10 e 11 com link para `seguranca.md`, `monitoramento.md` e `funcoes.md` |
| Diagrama completo no README | ok no arquivo | Mermaid igual ao conferido na 6.6, com API Gateway, três funções, DynamoDB com Streams, Scheduler, os dois tópicos, bucket, CloudWatch, X-Ray e GitHub Actions com OIDC. Renderização no GitHub depois do push |
| Diagrama da arquitetura (relatório) | ok | `docs/relatorio-tech-challenge.md:45` mostra `img/arquitetura.png`; a fonte `img/arquitetura.mmd` está ao lado e tem link na linha 47. A imagem mostra os três fluxos numerados, a observabilidade e a entrega contínua, sem valor sigiloso |
| Ensaio em hml | ok | Seção na linha 30, antes da preparação e dos blocos. Não tem bloco de comando próprio: manda repetir os blocos trocando `feedback-prod` e `-prod` por `feedback-hml` e `-hml`, então nenhum comando dela cita prod. As contas do relatório do ensaio (4 avaliações, média 5,50) batem com os 2 itens de hml da leitura da 2.1 |
| Recursos de produção | ok | Os 9 blocos `bash` do roteiro só citam `feedback-prod` ou `-prod`, sem nenhum `-hml` ou `-dev` |
| Plano de remoção (README) | **falha** | A linha 85 está certa ("hml pode ser removido depois da entrega, em 13/11/2026"), mas a linha 362, em "7. Remover uma stack", ainda diz "Plano: hml pode ser removido depois da gravação do vídeo" |
| Depois da gravação (roteiro) | ok | Linhas 384 a 396: nada apagado à mão, agendamento de 16/11 com as avaliações do vídeo, hml só depois da entrega e prod depois da nota, os dois com o bucket esvaziado antes |
| Execução local (3 cenários) | **falha** | O relatório (linhas 211 a 223) traz o comando e a marca de não executado, mas manda para o README o evento, o resultado esperado e a lista do que não é reproduzido, e não diz que o Docker é pré-requisito. A spec ainda diz "O relatório SHALL mostrar [...] o evento de entrada escrito no próprio documento e o resultado esperado". O conteúdo completo está no README, linhas 134 a 197 |
| Pendência pós-entrega do aspectjrt | ok | A linha R08 agora traz as três condições |
| O que desfocar | ok | `docs/roteiro-video.md:23` cita cabeçalho, corpo e rodapé |
| Entrada do changelog | ok | `CHANGELOG.md:17` diz que código, template e pipelines não mudaram |

### Problemas

- **`README.md:362`:** troque "Plano: hml pode ser removido depois da gravação do vídeo, e prod só depois da divulgação da nota da FIAP." por "Plano: hml pode ser removido depois da entrega, em 13/11/2026, e prod só depois da divulgação da nota da FIAP."
- **`docs/seguranca.md:317` (R10, mitigação):** troque "hml sai depois da gravação, prod depois da nota" por "hml sai depois da entrega, em 13/11/2026, e prod depois da nota".
- **`docs/seguranca.md:324` (R17, mitigação):** troque "hml é removido depois da gravação e prod depois da nota" por "hml é removido depois da entrega, em 13/11/2026, e prod depois da nota".
- **`docs/seguranca.md:335` (R28, mitigação):** a mesma troca da R17.
- **`docs/relatorio-tech-challenge.md:211` a `223`:** volte a pôr no relatório o evento `evento-entrada-invalida.json`, o `variaveis.json`, o resultado esperado (400 com `ENTRADA_INVALIDA` e os dois campos), a frase "O que exige: Docker em execução [...]" e a lista do que não é reproduzido, como no README, linhas 140 a 197. Se o Gabriel preferir manter o detalhe só no README, o requisito "Execução local documentada" e seus três cenários precisam passar a aceitar isso antes do arquivamento.
- **`docs/relatorio-tech-challenge.md:471`, no apêndice:** cita "conferência do SnapStart em prod depois da release 1.0.1" neste `qa.md`, mas essa conferência acontece depois do commit de entrega e do arquivamento e não vai estar no arquivo. Sugestão: tirar esse trecho da célula ou dizer que a conferência é informada ao Gabriel antes da gravação e não está no repositório.
- **Sigilo:** se o Gabriel renovar a sessão com `aws sso login --profile tc4-admin` antes do commit, vale repetir a D9 com a AWS ao vivo. A rodada abaixo usou os valores das leituras registradas.

### Sigilo D9 com valores das leituras registradas

A D9 rodou com os valores reais tirados das saídas brutas das leituras do QA de 04 a 11/11/2026, guardadas no scratchpad da sessão, fora do repositório, e não da AWS ao vivo, porque a sessão SSO do perfil `tc4-admin` expirou. Script Python temporário (`d9_offline.py`, no scratchpad), rodado com `python -I`. Ele extrai os valores por expressão regular, guarda em memória e imprime só contagens e os 8 primeiros caracteres do SHA-256 de cada valor, nunca o valor.

| Valor | Como foi extraído | De onde veio |
|---|---|---|
| ID da conta | campo de conta dos ARNs `arn:aws:<serviço>:sa-east-1:<conta>:`, sem os fictícios dos testes | 191 arquivos, nas pastas `qa-prod`, `qa-hml`, `qa-agendada`, `qa-observabilidade`, `qa-relatorio`, `qa-notificacao` e `conta`, e em `stack.json`, `pol-*.json`, `logs*.json` e `trace*.json` |
| IDs das APIs de dev, hml e prod | host de `https://<id>.execute-api.sa-east-1.amazonaws.com` e campo `ApiId` | 3 valores distintos: o de dev em 10 arquivos (`stack.json`, `url.txt`, `newman.txt`, `qa-relatorio`, `qa-notificacao`, `qa-observabilidade`, `qa-hml` e `qa-agendada`), o de hml em `qa-hml` e `qa-agendada` e o de prod em `qa-prod` e `qa-agendada` |
| Endpoint das assinaturas | campo `Endpoint` com `@` das saídas de `list-subscriptions-by-topic` | 1 valor distinto, em 2 arquivos de `qa-prod`. A leitura ao vivo da 6.4 também deu 1 endereço único nos três ambientes |

A expressão dos ARNs achou um segundo número de 12 dígitos, em 22 arquivos. Todos são cópias do código de teste do repositório (`base`, `main`, `mut`, `sim-merge`, `b-junto` e `b-notif`), e no worktree ele só aparece em `AquecimentoTest.java` e `NotificarCriticaHandlerTest.java`, de `functions/notificar-critica/src/test`. É uma conta fictícia de fixture, diferente da conta real, e esses arquivos não mudaram neste change.

Contagem no worktree, sobre `git ls-files -co --exclude-standard` (155 arquivos existentes; os caminhos apagados pelo arquivamento existem em `archive/` e foram lidos lá), incluindo `docs/img/arquitetura.mmd`, sem diferenciar maiúsculas:

| Valor | Arquivos | Ocorrências |
|---|---|---|
| ID da conta | 0 | 0 |
| API de dev | 0 | 0 |
| API de hml | 0 | 0 |
| API de prod | 0 | 0 |
| E-mail dos administradores | 0 | 0 |

A PNG também entrou na busca, como bytes, com 0 ocorrências. Além disso, já tinha sido conferida à parte (chunks e imagem aberta). Resultado: ok, com o limite de que os valores são os das leituras registradas.

### Fechamento depois das correções

Reconferido depois dos ajustes no README, em `docs/seguranca.md`, no relatório e na spec (requisito "Execução local documentada" dividido entre relatório e README).

| Cenário ou verificação | Resultado | Evidência |
|---|---|---|
| Plano de remoção | ok | `README.md:85` e `:362`, `docs/seguranca.md:205` e as mitigações da R10 (`:317`), da R17 (`:324`) e da R28 (`:335`), `docs/monitoramento.md:386` e `docs/roteiro-video.md:45` e `:396`: todos dizem que hml sai depois da entrega, em 13/11/2026, e prod depois da nota. Nenhum documento de entrega diz mais "depois da gravação" para hml |
| Depois da gravação | ok | `docs/roteiro-video.md:384` a `:396`, sem mudança desde a rodada anterior |
| Relatório com comando e nota | **falha** | `docs/relatorio-tech-challenge.md:211` a `:223` traz o comando e a nota de não executado com o motivo, mas o link da linha 213 é `[README.md](../README.md)`, para o arquivo, e não para a subseção que o cenário pede |
| Detalhe no README | ok | `README.md:134` a `:197`: comando idêntico ao do relatório (conferido com `diff`), evento, `variaveis.json`, resultado esperado, Docker como pré-requisito ("O que exige") e a nota de não executado com o motivo |
| Evento coerente com a spec | ok | Descrição vazia e nota 11 dão 400 com `ENTRADA_INVALIDA` e os dois campos, como na spec `registro-avaliacao` |
| Limites da execução local | ok | README: "O que acessa na AWS: nada" e a lista do que não é reproduzido, com SnapStart, aquecimento dos clientes e gatilhos reais |
| Célula do apêndice e linha de prod | ok | `docs/relatorio-tech-challenge.md:471` e `:184` dizem que a conferência do SnapStart pós-1.0.1 é feita antes da gravação e não fica no repositório |
| `checa.py` | ok | `erros 0`, 8 documentos |
| D10 | ok | 36 blocos `bash`, 36 marcados |
| D12 | ok | 51 arquivos; só `openspec/project.md:131` |
| `openspec validate --all --strict` | ok | 8 de 8 |

**O que falta:** em `docs/relatorio-tech-challenge.md:213`, trocar `[README.md](../README.md)` por `[README.md, Execução local com `sam local`](../README.md#execução-local-com-sam-local)`. A âncora existe: o título `### Execução local com `sam local`` está no `README.md:134`, e o slug segue a regra do GitHub da D11. Depois da troca, basta rodar de novo o `checa.py`.
