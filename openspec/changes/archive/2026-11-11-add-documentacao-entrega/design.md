# Design: add-documentacao-entrega

## Context

A versão 1.0.0 está em prod desde 08/11/2026 (execução 42438770120 do workflow Deploy, tag `v1.0.0`). A entrega na FIAP é em 13/11/2026, e este design foi fechado em 10/11/2026, o que deixa três dias úteis para escrever, conferir, arquivar e gravar o vídeo.

Estado de partida:
- A branch `feature/add-documentacao-entrega` saiu da `develop` em `8195444`. O PR #16 (`release/1.0.0` para `develop`) foi mergeado em `6d7c993`, e a `develop` com a release já entrou na branch. Este design foi feito lendo a `main` (`6eee512`), que tem o mesmo conteúdo da release.
- O `README.md` tem 827 linhas e mistura visão geral, procedimento e evidência. Ele diz que hml e prod não foram implantados e que o roteiro não foi ensaiado.
- A conferência somente de leitura de prod feita pela sessão principal em 08/11/2026 não está registrada em nenhum `qa.md`: stack `CREATE_COMPLETE`, três funções `Active`, `On` e 1024 MB na versão 1, retenção de 30 dias, alarmes em OK com ação só no tópico de prod, agendamento `ENABLED`, assinaturas confirmadas, tabela e bucket vazios e nenhuma invocação. Pelo requisito "Estado real das funções por ambiente", o README só pode citar o que está numa fonte. Por isso o QA deste change repete essa leitura e a registra (D14).
- A primeira execução agendada estava prevista para 09/11/2026 08:00 em São Paulo nos três ambientes. O QA está lendo essa execução e o estado de prod em 10/11/2026. O registro da execução pertence ao QA de `add-relatorio-semanal` e precisa estar feito antes do arquivamento (D15).
- A 6.17 e a 6.19 de `add-observabilidade-seguranca` já foram marcadas pela sessão principal. A 8.3 será marcada quando o QA registrar a leitura de prod. A 8.6 de `add-registro-avaliacao` saiu das tarefas e virou pendência pós-entrega no `project.md`.
- `sam local` nunca rodou no projeto, e o Docker não está disponível nesta máquina (D13).

**Fontes.** Consultadas em 10/11/2026:
- Ensaio do arquivamento com o OpenSpec CLI 1.16.0 numa cópia descartável do `openspec/` da `main`, fora do repositório. Resultados na D15.
- SAM CLI 1.171.0 instalado: o pacote traz `java25` na lista de runtimes de `sam local` e usa as imagens de `public.ecr.aws/lambda`.
- [Introdução ao `sam local invoke`](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/using-sam-cli-local-invoke.html): pré-requisitos, formato do `--env-vars` por ID lógico da função, saída do log em `stderr` e da resposta em `stdout`, e o aviso de que o teste local não valida permissões.
- [Criação de diagramas no GitHub](https://docs.github.com/en/get-started/writing-on-github/working-with-advanced-formatting/creating-diagrams): Mermaid é renderizado em arquivos Markdown, num bloco cercado com a linguagem `mermaid`.
- [PITR do DynamoDB](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/PointInTimeRecovery_Howitworks.html): a exclusão de uma tabela com recuperação pontual ativa gera um backup de sistema, mantido por 35 dias. A tabela de prod tem PITR (`PointInTimeRecoveryEnabled: !If [EhProd, true, false]`).

## Goals / Non-Goals

**Goals**
- O avaliador acha cada critério da FIAP a partir do README em até dois cliques, sem abrir `qa.md`
- Cada fato aparece em um único lugar. Os outros documentos apontam para ele
- Cada afirmação sobre estado, configuração ou governança tem fonte verificável no commit de entrega
- Verificações mecânicas para links, âncoras, sigilo, travessão e origem dos comandos, rodadas sem gravar valor sensível em arquivo
- Arquivamento dos cinco changes com as specs consolidadas e válidas
- Tudo dentro do prazo, sem custo novo e sem escrita na AWS além do roteiro do vídeo, que é do Gabriel

**Non-Goals**
- Os itens de "Fora do escopo" da proposta, inclusive qualquer mudança de código, template, workflow, bootstrap ou `pom.xml`
- Reescrever `qa.md`, `design.md` ou `docs/incidentes/`. Acrescentar uma seção nova a um `qa.md`, como as releases fizeram, não é reescrita
- Automatizar as verificações no CI
- Exportar o relatório em PDF ou publicar o vídeo

## Decisões do Gabriel de 10/11/2026

| # | Decisão | Onde entra |
|---|---|---|
| 1 | Relatório em Markdown em `docs/relatorio-tech-challenge.md`. O PDF é exportado pelo Gabriel na hora de enviar e não é versionado | D7, D8 |
| 2 | No vídeo em prod, as avaliações são registradas ao vivo (201, e-mail crítico e relatório com a referência de amanhã), com descrições neutras e sem apagar item à mão. prod só é removido depois da nota da FIAP, esvaziando o bucket antes. A data de remoção de hml foi ajustada em 11/11 (decisão 7) | D4, D6, D14 |
| 3 | Arquivamento dos cinco changes e deste no fim do change, depois do estado final dos documentos. A 8.6 de `add-registro-avaliacao` saiu das tarefas e é pendência pós-entrega no `project.md` e em `docs/seguranca.md`, com a condição de remoção do `aspectjrt`. O repositório entregue passa em todas as validações do OpenSpec, inclusive a dos changes arquivados | D4, D15 |
| 4 | Release 1.0.1 só de documentação, com a versão dos `pom.xml` mudada na branch de release, fora deste change. Vai para prod antes da gravação, com conferência só de leitura de SnapStart `Active` e `On` nas três funções de prod logo depois | D14, tasks, depois do commit de entrega |
| 5 | Sem Docker disponível, o `sam local` fica documentado e marcado como não executado. A execução sai das tarefas obrigatórias | D13 |
| 6 | Prazos: relatório e roteiro prontos para revisão até 11/11 12:00; README, `docs/funcoes.md`, `docs/seguranca.md` e `docs/monitoramento.md` até 11/11 18:00; depois, verificações, arquivamento e comandos da release 1.0.1 | D16, tasks |

## Revisão do Gabriel de 11/11/2026

| # | Decisão | Onde entra |
|---|---|---|
| 7 | hml só é removido depois da entrega de 13/11/2026; prod, depois da nota | D4, D6, D14, tasks |
| 8 | Roteiro com a seção "Ensaio em hml" antes da gravação; gravação em 12/11/2026 com a data de referência `2026-11-13`; bloco 5 com 30 s a mais para os changes arquivados, um `qa.md` e `.claude/agents`, total de 11 min 30 s | D6 |
| 9 | Relatório no modelo da FIAP: cabeçalho com `[LINK DO VÍDEO]` até um PR próprio depois da gravação, seções numeradas de 1 a 13 (12 Processo de desenvolvimento, 13 Aprendizados), apêndice Evidências com as fontes e tom impessoal fora da seção 13 | D7 |
| 10 | Diagrama do relatório em imagem `docs/img/arquitetura.png`, gerada com o mermaid-cli a partir de `docs/img/arquitetura.mmd`, versão enxuta para o PDF. O Mermaid detalhado fica só no README | D1, D8 |

## Decisions

### D1. Mapa dos documentos e fonte única
Cada tabela de configuração tem um único dono. Os outros documentos citam o assunto em uma frase e apontam para a seção do dono.

| Documento | O que contém | Tabelas que vivem só nele | Fontes |
|---|---|---|---|
| `README.md` | Problema, diagrama, estado por ambiente, build e testes, uso do endpoint, implantação, índice | Estado das funções por ambiente; mapeamento de branch, ambiente e stack | `project.md`; `deploy.yml`; `samconfig.toml`; `qa.md` de cada change; `qa.md` deste change (D14); execuções do workflow pela API do GitHub |
| `docs/funcoes.md` | As três funções e a configuração comum | Permissões de cada função; variáveis de ambiente; erros e efeitos; linhas de log; métricas do Powertools | `template.yaml`; specs `registro-avaliacao`, `notificacao-critica` e `relatorio-semanal`; handlers e `Respostas.java`; designs dos três changes de função; seções do README atual que saem dele (D2) |
| `docs/seguranca.md` | Controles da nuvem, governança do repositório e registro de riscos aceitos | Registro de riscos; roles de deploy e negações; criptografia e TLS por recurso; onde fica o dado do aluno | `infra/bootstrap/github-oidc.yaml`; `template.yaml`; specs `seguranca`, `fundacao-cloud` e `entrega-continua`; leitura por `gh api` (D17); Risks dos cinco designs; decisões e observações dos cinco `qa.md` |
| `docs/monitoramento.md` | Logs, métricas, alarmes, dashboard, X-Ray, teste de alarme e nível gratuito | Configuração dos alarmes; widgets do dashboard; retenção por ambiente; conta do nível gratuito | `template.yaml`; spec `observabilidade`; design de `add-observabilidade-seguranca` (D3 a D8, D14, D17); `qa.md` dele (tempos de volta a OK, janelas A, B e C); seção "Monitoramento e alarmes" do README atual |
| `docs/roteiro-video.md` | Preparação, blocos com tempo, cuidados na tela, depois da gravação | Blocos e tempos; lista do que desfocar | Roteiro atual; leitura de prod deste change (D14); decisão 2 |
| `docs/relatorio-tech-challenge.md` | Seções do modelo da FIAP, resumindo e apontando para `docs/` | Endpoints com exemplos de requisição e resposta; números de qualidade | `project.md`, Entregáveis; spec `registro-avaliacao`; saída de `mvn -B verify`; execução local (D13); os outros documentos |

O diagrama tem duas formas com papéis diferentes: o Mermaid detalhado só no README e a imagem enxuta no relatório, gerada de `docs/img/arquitetura.mmd` (D8). A coleção Postman não muda.

Alternativa descartada: manter as tabelas no README e fazer os documentos de `docs/` apontarem para ele. O README atual já tem 827 linhas, e a observação K de `add-observabilidade-seguranca` mostra que a mesma informação em dois lugares diverge.

### D2. Estrutura do README e destino das seções atuais
Seções, nesta ordem:

1. **Título e resumo:** o problema em três frases e o link para o relatório.
2. **Arquitetura:** o diagrama da D8 e a tabela de função, gatilho e responsabilidade do `project.md`.
3. **Estado por ambiente** (D14).
4. **Documentação:** o índice, com link para os cinco documentos, a coleção Postman, o `CHANGELOG.md`, o incidente de 04/11/2026 e `openspec/specs/`.
5. **Como buildar e testar:** pré-requisitos (JDK 25, Maven, SAM CLI 1.171.0), `mvn -B verify`, `mvn -B install`, `sam build`, `sam validate --lint`, e o que o CI roda nos PRs.
6. **Como usar o endpoint:** um `curl` com `<ID_DA_API>`, onde achar a URL no output `ApiAvaliacoesUrl` e o link para a seção Endpoints do relatório.
7. **Implantação numa conta nova**, na ordem do requisito: bootstrap do OIDC e das roles, service-linked role do API Gateway, ambientes do GitHub com `AWS_ROLE_ARN` e `EMAIL_ADMIN`, deploy por branch com a tabela de mapeamento, aprovação manual em prod, confirmação das assinaturas e remoção de uma stack com o esvaziamento do bucket antes do `sam delete`.
8. **Estrutura de pastas e stack**, resumidas.

Destino das seções atuais que saem do README:

| Seção atual | Destino |
|---|---|
| Respostas do endpoint, urgência | `docs/funcoes.md`, registrar-avaliacao; exemplos completos na seção Endpoints do relatório |
| Conferir o SnapStart no alias; memória e aquecimento | `docs/funcoes.md`, configuração comum |
| Reprocessamento do aviso crítico; lotes descartados | `docs/funcoes.md`, notificar-critica |
| Relatório semanal: acionamento manual, data de referência, formatos, download | `docs/funcoes.md`, gerar-relatorio-semanal |
| Esvaziar o bucket antes de `sam delete`; remover a stack | README, implantação, último passo |
| Monitoramento e alarmes, inteira | `docs/monitoramento.md` |
| Atualizar o bootstrap depois; regras para os próximos templates | `docs/seguranca.md`, roles de deploy |
| Passos 1 a 5 do bootstrap | README, implantação |

Alternativa descartada: README curto, só com o índice. O requisito "Instruções de implantação" exige os passos com comando no README.

### D3. Estrutura de `docs/funcoes.md`
Uma seção de configuração comum (runtime `java25`, `x86_64`, 1024 MB do `Globals`, alias `live`, SnapStart em versões publicadas, X-Ray ativo, variáveis do `Globals`, boundary, aquecimento antes do snapshot e como conferir o alias), seguida de uma seção por função com as mesmas subseções, na mesma ordem: responsabilidade; gatilho; entrada; saída; regras de negócio; erros tratados e efeito; variáveis de ambiente; permissões; execução (memória, timeout, alias e SnapStart); métricas; logs; alarme; procedimentos (reprocessamento na notificação, acionamento manual e download no relatório).

- **Permissões:** uma linha por statement da política inline do template, com `Sid`, ações e recurso escrito com marcadores (`feedback-avaliacoes-<amb>`, `feedback-relatorios-<ID_DA_CONTA>-<amb>`), mais as condições. A notificação inclui as permissões do mapeamento de origem do Streams que o SAM gera. O QA confere statement por statement contra o template.
- **Variáveis:** nome e origem no template (`!Ref TabelaAvaliacoes`, `!Sub ...`), nunca o valor resolvido.
- **Erros:** status, código e mensagem fixa como nas specs. O corpo completo fica no relatório.
- **Alarme:** só o nome do alarme e o que ele cobre, com link para a tabela de `docs/monitoramento.md`.

Alternativa descartada: um arquivo por função. Três arquivos para repetir a mesma configuração comum, e o requisito cita um único `docs/funcoes.md`.

### D4. Estrutura de `docs/seguranca.md` e do registro de riscos
Uma seção por tema da spec, na ordem dela, mais "Roles de deploy e manutenção do bootstrap" e "Registro de riscos aceitos" no fim.

**Onde fica o dado do aluno:** tabela com tabela de avaliações, relatórios em `relatorios/`, lotes descartados em `aws/lambda/`, e-mail do aviso crítico e o backup de sistema da tabela de prod. Colunas: o que guarda, criptografia em repouso, expiração. A expiração é dita como é: nenhum desses lugares expira sozinho enquanto a stack existe. O dado de hml some com a remoção da stack depois da entrega de 13/11 (decisão 7). O de prod fica até a remoção, que só acontece depois da nota da FIAP, com o bucket esvaziado antes; como a tabela de prod tem PITR, a remoção gera um backup de sistema mantido por 35 dias. O documento não promete data de remoção.

**Pendência pós-entrega:** uma seção curta com o desvio do `aspectjrt` em escopo `runtime`, a condição de remoção (primeira release do Powertools com o commit `f628502`) e o link para o incidente. É a mesma pendência que o `project.md` registra (decisão 3), e o risco correspondente no registro aponta para ela.

**Registro de riscos:** tabela com as colunas Id, Risco, Origem, Motivo da aceitação, Mitigação, Decisão (data e autor, quando houver) e Revisão (condição, quando houver). Ids `R01` a `Rnn`, em ordem de origem. O link para uma linha é feito pelo Id com âncora na seção, porque o GitHub não cria âncora para linha de tabela. `docs/monitoramento.md` cita `R` de cada risco de alarme. Sem mitigação, a célula diz "Nenhuma", com o motivo na coluna anterior.

**Cobertura:** a spec manda que todo item dado como aceito nas seções de riscos dos cinco designs e nas decisões e seções de risco aceito dos cinco `qa.md` tenha linha. São 44 itens só nas seções de Risks dos designs, e vários foram resolvidos depois, como a primeira chamada lenta do SDK, resolvida pelo aquecimento. O documentador monta, no `qa.md` deste change, uma tabela de cobertura com cada item de origem e o destino: um `R` do registro, ou "resolvido em <change> <seção>", ou "não é risco". O QA confere essa tabela contra as fontes. Os 16 riscos listados na spec entram obrigatoriamente.

Alternativa descartada: um registro por documento. A proposta (D2) pede um registro único.

### D5. Estrutura de `docs/monitoramento.md`
Seções na ordem do requisito: logs (formato JSON do Powertools, retenção por ambiente, consulta pelo console e pelo Logs Insights); métricas (namespace `FeedbackServerless/<amb>`, métricas do Powertools e métricas de filtro, com a origem de cada uma); alarmes (tabela única com filtro, estatística, limiar, período, períodos de avaliação, dado ausente e ações, mais o que dispara e o que não dispara cada um); dashboard (os cinco widgets e a consulta de restauração); X-Ray (como abrir o trace de uma invocação pelo `xray_trace_id` do log); teste de alarme (`set-alarm-state` e `put-log-events` num stream de teste, com o aviso de e-mail real e os tempos de volta a OK registrados: 60 a 80 s e 10 a 11 min); nível gratuito (10 de 10 métricas de alarme, 3 de 3 dashboards, a proibição de alarme ou dashboard de teste e o link para o `R` correspondente).

Os comandos de teste são os mesmos dos `qa.md`, só com o ID da conta trocado por marcador.

Alternativa descartada: juntar monitoramento e funções. A spec separa os dois, e o relatório tem seções separadas.

### D6. Roteiro do vídeo em prod
Todos os nomes com o sufixo `-prod` e a stack `feedback-prod`.

**Preparação, sem gravar nada na tabela:** conferir 0 itens em `feedback-avaliacoes-prod` com `aws dynamodb scan --select COUNT`, porque o `ItemCount` do `describe-table` só é atualizado a cada seis horas; assinaturas confirmadas nos tópicos de administradores e de alarmes de prod; três alarmes em OK; `State` `Active` nas versões do alias `live`; terminal com fonte grande e sem histórico; caixa de e-mail aberta numa pasta que não mostre outras mensagens. Aquecimento opcional só com uma requisição rejeitada (400) e com a data `2026-02-30` no relatório, que não gravam, sem mostrar a resposta da data rejeitada.

**Ensaio em hml:** uma seção antes da gravação manda percorrer os blocos 2 a 4 em hml, com os nomes `-hml`, para conferir comandos, tempos e o que precisa ser desfocado. hml pode receber avaliações de teste. O ensaio não muda a regra de prod: nada é gravado em `feedback-avaliacoes-prod` antes da gravação.

**Data:** gravação em 12/11/2026, com a data de referência `2026-11-13` no acionamento manual do relatório, para a avaliação registrada no dia entrar na janela.

**Blocos, total estimado de 11 min 30 s:**

| Bloco | Tempo | Conteúdo |
|---|---|---|
| 1. Abertura | 1 min | Problema e diagrama do README |
| 2. Aplicação funcionando | 4 min | `POST /avaliacao` com nota 9 (201, BAIXA), com corpo inválido (400), com nota 2 (201, CRITICA) e o e-mail crítico; relatório pelo alias `live` com a referência `2026-11-13`, a resposta, o objeto no S3 e o e-mail do aviso |
| 3. Funções ativas | 2 min | Console do Lambda: as três funções, o gatilho de cada uma (API, Streams com filtro, Scheduler), alias `live` com SnapStart `On`. Se a D14 tiver registrado a execução agendada de 09/11, mostrar o objeto `relatorios/2026-11-09.txt` como prova do agendamento |
| 4. Configurações | 3,5 min | Trechos do `template.yaml`; execução do Deploy com a aprovação em prod; role de deploy com OIDC e o boundary; criptografia e política do bucket; dashboard de prod e um único `set-alarm-state`, que gera e-mail real |
| 5. Encerramento | 1 min | Repositório, documentação e relatório; os changes arquivados em `openspec/changes/archive/`, um `qa.md` e os agentes em `.claude/agents` |

**Cuidados na tela:** a lista da spec (ID da conta no menu, nos ARNs, nas variáveis de ambiente e no nome do bucket; e-mail no corpo e no rodapé; identificador da API na URL) e a regra de mostrar só a mensagem da data rejeitada. Descrições neutras, sem nome de pessoa.

**Pré-condição da gravação:** a release 1.0.1 já está em prod, e a conferência de leitura logo depois dela mostrou `State` `Active` e `OptimizationStatus` `On` nas três funções (decisão 4). A preparação repete essa leitura no dia.

**Depois da gravação (decisão 2):** as avaliações, os relatórios e os e-mails gerados no vídeo ficam onde estão; nada é apagado à mão. hml continua no ar até a entrega de 13/11 e só então pode ser removido (decisão 7). prod continua no ar até a nota da FIAP; só então o bucket é esvaziado e a stack removida, e a tabela de prod deixa um backup de sistema por 35 dias, pelo PITR. Os comandos de remoção são os da implantação no README, com o link.

Alternativa descartada: gravar em hml, onde sujar dado não importa. A spec pede prod, e prod é o ambiente que o enunciado quer ver ativo.

### D7. Relatório do Tech Challenge
Arquivo `docs/relatorio-tech-challenge.md` (decisões 1 e 9).

**Cabeçalho do modelo da FIAP:** título, aluno, curso, fase, data de entrega, URL do repositório e `[LINK DO VÍDEO]`. O marcador fica até a gravação e é trocado pelo link num PR próprio depois dela (tasks, depois do commit de entrega). Como não tem parênteses, ele não é link e não entra na verificação da D11.

**Seções numeradas, títulos de nível 2:** 1 Introdução; 2 Arquitetura; 3 Endpoints; 4 Configuração do projeto, com a subseção Execução local (D13); 5 Qualidade do código; 6 Collections para teste; 7 Repositório; 8 Modelo de cloud; 9 Segurança e governança; 10 Monitoramento; 11 Documentação das funções; 12 Processo de desenvolvimento (SDD com OpenSpec, agentes, Gitflow, aprovação do Gabriel antes do código); 13 Aprendizados. As seções 1 a 11 seguem a ordem do `project.md`. Os números entram no slug da âncora (`#1-introdução`), e quem apontar para uma seção usa o slug com o número.

**Apêndice Evidências:** depois da seção 13, uma tabela com cada afirmação verificável do relatório e a fonte (seção de `qa.md` arquivado, execução do workflow, leitura deste change). As seções do corpo citam o apêndice em vez de repetir a fonte.

**Tom:** impessoal nas seções 1 a 12 e no apêndice. A seção 13 é em primeira pessoa, porque é a experiência do autor.

- **Links para `docs/`:** relativos, e o texto do link é o caminho (`[docs/seguranca.md](seguranca.md)`). No GitHub o link funciona; no PDF, o leitor vê o caminho e acha o arquivo pela URL da seção Repositório.
- **Endpoints:** tabela com os quatro casos (201, 400 `ENTRADA_INVALIDA`, 400 `CORPO_INVALIDO`, 500 `ERRO_INTERNO`), exemplo de requisição e corpo de resposta de cada um, copiados da spec, e a URL com `<ID_DA_API>`.
- **Qualidade do código:** os números de testes vêm da saída de `mvn -B verify` rodada pelo QA no estado final da branch, por módulo e no total.
- **Collections:** caminho do arquivo, `baseUrl` sem `/avaliacao`, quantidade de requisições contada no JSON e o ambiente em que a coleção rodou, conforme os `qa.md`.
- **Repositório:** `https://github.com/g-andradd/fiap-tc4-feedback-serverless`, que não é dado sigiloso.

Alternativas descartadas:
- Links absolutos para `github.com/.../blob/develop/docs/...`. Funcionam no PDF, mas não são verificáveis antes do merge e quebram se a branch padrão mudar.
- Fontes no corpo de cada seção. Polui o texto que o avaliador lê; o apêndice concentra a evidência.
- Esperar o vídeo para fechar o relatório. Atrasaria o commit de entrega; o marcador e o PR próprio resolvem.

### D8. Diagramas: Mermaid detalhado no README e imagem enxuta no relatório (decisão 10)
**README:** um `flowchart LR` detalhado com aluno, API Gateway (HTTP API), as três funções, DynamoDB com Streams e o filtro de criação CRITICA, EventBridge Scheduler (segunda 08:00), tópico de administradores com e-mail, tópico de alarmes com e-mail, bucket de relatórios, CloudWatch (logs, filtros, alarmes, dashboard) e X-Ray. Sem ícones e sem recurso de versão recente do Mermaid, porque a página do GitHub não diz qual versão está em uso.

**Relatório:** a imagem `docs/img/arquitetura.png`, referenciada como `![...](img/arquitetura.png)`, gerada de `docs/img/arquitetura.mmd` com o mermaid-cli (`mmdc -i docs/img/arquitetura.mmd -o docs/img/arquitetura.png`, pelo `npx @mermaid-js/mermaid-cli`, fora do repositório e sem dependência nova no projeto). O `.mmd` é a versão enxuta, legível numa página de PDF, e cobre os três fluxos do `project.md`:
- registro: aluno, API Gateway, `registrar-avaliacao`, DynamoDB;
- aviso crítico: DynamoDB Streams com o filtro, `notificar-critica`, tópico de administradores, e-mail;
- relatório: EventBridge Scheduler, `gerar-relatorio-semanal`, bucket e tópico de administradores;
mais CloudWatch, X-Ray e o tópico de alarmes, agrupados.

**Verificação:** a imagem e o `.mmd` existem no commit; o `.mmd` tem os nós e as ligações dos três fluxos acima; a imagem é regenerada do `.mmd` final e conferida a olho; o link da imagem resolve pelo script da D11, que também lê links de imagem. Os dois diagramas não são comparados entre si, porque têm níveis de detalhe diferentes de propósito.

Alternativas descartadas:
- O mesmo bloco Mermaid no README e no relatório. O Mermaid não renderiza no PDF exportado, e o diagrama detalhado não cabe legível numa página.
- Imagem também no README. O Mermaid renderiza no GitHub e é editável sem regenerar arquivo.
- Imagem desenhada à mão em ferramenta gráfica. Sem fonte versionada para atualizar.

### D9. Marcadores e verificação de sigilo
Marcadores nos documentos de entrega: `<ID_DA_CONTA>`, `<ID_DA_API>` e `<EMAIL_ADMIN>`. Os `qa.md` antigos usam `<conta>` e `<email>` e ficam como estão.

A verificação lê os valores na AWS na hora, guarda em variável do shell e só imprime nome de arquivo e contagem, nunca o valor. Roda sobre os arquivos que vão para o commit (`git ls-files -co --exclude-standard`, que é leitura) e, depois do commit do Gabriel, sobre o próprio commit com `git grep`:

```bash
CONTA=$(aws sts get-caller-identity --query Account --output text --profile tc4-admin)
APIS=$(aws apigatewayv2 get-apis --region sa-east-1 --profile tc4-admin \
  --query "Items[?starts_with(Name,'feedback-api-')].ApiId" --output text)
EMAILS=$(for amb in dev hml prod; do
  arn=$(aws cloudformation describe-stacks --stack-name "feedback-$amb" --region sa-east-1 \
    --profile tc4-admin --query "Stacks[0].Outputs[?OutputKey=='TopicoAdminsArn'].OutputValue" \
    --output text 2>/dev/null) || continue
  aws sns list-subscriptions-by-topic --topic-arn "$arn" --region sa-east-1 --profile tc4-admin \
    --query "Subscriptions[].Endpoint" --output text
done | tr '\t' '\n' | sort -u)
for v in "$CONTA" $APIS $EMAILS; do
  git ls-files -co --exclude-standard -z | xargs -0 grep -IliF -e "$v" | wc -l
done
```

- O e-mail vem do endpoint da assinatura, que é o valor do secret `EMAIL_ADMIN` passado pelo deploy. Assim o Gabriel não precisa digitar o endereço e ele não aparece no chat.
- `get-apis` cobre as APIs de todas as stacks existentes, não só as que têm output legível.
- Esperado: `0` em todas as linhas. Se alguma der mais que zero, roda-se de novo com `-l` no lugar de `wc -l` para saber o arquivo, sem imprimir a linha.
- Informativo, sem critério: a mesma busca em `git log -p --all`, para saber se algum valor já foi publicado no histórico. Reescrever histórico fica fora do escopo; o resultado vai para o Gabriel.

Alternativa descartada: o Gabriel informar o e-mail na hora com `read -s`. A ferramenta de shell dos agentes não é interativa, e o valor passaria pelo chat.

### D10. Origem de cada bloco de comando
Só blocos cercados com a linguagem `bash` são blocos de comando. Saída, JSON, YAML e Mermaid usam `text`, `json`, `yaml` e `mermaid`. A linha não vazia imediatamente antes de cada bloco `bash` começa com uma destas duas formas:

- `Registro:` seguida da fonte, com link: seção de `qa.md`, incidente, ou seção do `qa.md` deste change.
- `Não executado` seguido do motivo. Comandos de prod do roteiro que só rodam no vídeo usam "Não executado em prod antes da gravação; o mesmo comando com `-hml` está registrado em <fonte>".

A conferência é mecânica: um script temporário fora do repositório lista cada bloco `bash` com a linha anterior e falha se ela não começar com uma das duas formas. As leituras de prod do roteiro, como a contagem de itens, a assinatura e o estado dos alarmes, são executadas pelo QA na D14 e ganham `Registro:`.

Alternativa descartada: uma tabela de origem no fim de cada documento. O leitor teria de ir ao fim para saber se pode confiar no bloco que está lendo.

### D11. Links e âncoras
Um script Python temporário fora do repositório, sem dependência externa, lê os documentos de entrega e os links relativos deles (inclusive para `qa.md` e `openspec/specs/`), confere que o arquivo de destino está em `git ls-files -co --exclude-standard` e, quando há âncora, gera os slugs dos títulos do destino como o GitHub faz: minúsculas, letras acentuadas mantidas, crases e pontuação removidas exceto `-` e `_`, espaço vira `-`, e repetição ganha `-1`, `-2`. Os exemplos que já funcionam no repositório (`#relatório-semanal-acionamento-manual-e-download`, `#esvaziar-o-bucket-antes-de-sam-delete`) entram como caso de teste do próprio script. A URL do repositório é conferida sem autenticação com `curl -s -o /dev/null -w "%{http_code}"`, esperado 200.

O script roda duas vezes: depois da escrita dos documentos e depois do ajuste de caminhos do arquivamento (D15). A conferência final é no GitHub, abrindo os documentos depois do push.

Alternativa descartada: `markdown-link-check` ou similar pelo `npx`. Baixa pacote de terceiros e não conhece os slugs com acento do GitHub.

### D12. Sem travessão
A busca é feita nos arquivos criados ou alterados pelo change, porque `CLAUDE.md`, `openspec/project.md` e os arquivos gerados em `.claude/` contêm o caractere legitimamente e não são alterados:

```bash
{ git diff --name-only --diff-filter=d origin/develop; git ls-files -o --exclude-standard; } | sort -u \
  | xargs grep -lF $'\xE2\x80\x94'
```

Esperado: nenhuma saída. A busca usa os bytes do caractere em UTF-8 com `grep -F`, porque o `grep -P` do Git Bash recusou o locale nesta máquina. Os documentos não podem citar o caractere nem para dizer que ele é proibido.

### D13. Execução local com `sam local` (decisão 5)
O Docker não está disponível, então a execução local fica documentada no relatório e marcada como "Não executado: Docker indisponível na máquina de desenvolvimento em 10/11/2026". A execução não é tarefa obrigatória deste change.

**Função e evento:** `RegistrarAvaliacao`, com um evento HTTP API de payload 2.0 em que o corpo tem `descricao` vazia e `nota` 11, e a resposta esperada 400 com `ENTRADA_INVALIDA` citando os dois campos. O evento tem só os campos que o handler lê: `version`, `routeKey`, `rawPath`, `headers`, `requestContext` com `http.method`, `http.path` e `requestId`, `body` e `isBase64Encoded`. O relatório mostra o JSON do evento e o das variáveis (`{"RegistrarAvaliacao": {"TABELA_AVALIACOES": "feedback-avaliacoes-local"}}`, nome que não existe na conta) e manda salvá-los como `evento.json` e `variaveis.json`. Nenhum arquivo novo entra no repositório.

**Comando**, da raiz, com o JDK 25 no `JAVA_HOME` (no Git Bash do Windows, `sam.cmd`): `mvn -B install`, `sam build` e `sam local invoke RegistrarAvaliacao --event evento.json --env-vars variaveis.json --parameter-overrides Stage=dev`.

**Resultado esperado:** a resposta 400 com o corpo da spec `registro-avaliacao`. Como não houve execução, o relatório mostra só a resposta, que é a da spec e dos testes do handler, e não mostra linhas de log.

**O que exige:** Docker em execução, o `sam build` antes e o download da imagem `java:25` de `public.ecr.aws/lambda` na primeira vez. Não exige credencial.

**O que acessa na AWS:** nada, pela leitura do código: a validação rejeita antes de qualquer chamada ao DynamoDB, e o aquecimento só roda no `beforeCheckpoint`, que não acontece fora do SnapStart. O texto diz que isso vem da leitura do código, não de execução.

**O que não reproduz:** SnapStart, snapshot e restauração; o aquecimento dos clientes; alias `live` e versões; API Gateway real, com rota e limite de taxa; IAM, roles e boundary; os gatilhos reais (Streams com filtro, Scheduler); envio de trace ao X-Ray; métricas, filtros de log, alarmes e retenção; a CPU proporcional à memória.

Alternativas descartadas:
- Arquivos de evento versionados em `docs/sam-local/`. Sem execução, seriam dois arquivos a mais só para repetir o que o relatório já mostra.
- Caminho 201 contra a tabela de dev. Grava item em dev, exige credencial e dispara a notificação de dev se a nota for crítica.
- `GerarRelatorioSemanal` com data inválida. A resposta traz `errorType` e `stackTrace` (risco aceito).

### D14. Estado por ambiente e a leitura que o sustenta
A tabela do README tem nove linhas, uma por função e ambiente, com as colunas Estado, Data da última conferência e Fonte. Estados possíveis: "Ativa e conferida", "Implantada, sem conferência funcional", "Removida em <data>, conferida antes em <data>".

| Ambiente | Fonte |
|---|---|
| dev | Seções dos `qa.md` de cada change de função e de `add-observabilidade-seguranca` |
| hml | Seção "Release 1.0.0 em hml (07/11/2026)" de `add-fundacao-projeto/qa.md`. A notificação em hml, se não estiver nessa seção, fica como implantada sem conferência funcional, com a execução do workflow como fonte |
| prod | Leitura somente de leitura deste change, registrada no `qa.md` dele, e a execução 42438770120 |

**Leitura de prod pelo QA**, com o perfil `tc4-admin` e só `describe`, `get` e `list`: status da stack; `get-alias` e `get-function-configuration` das três funções (`State`, `OptimizationStatus`, `MemorySize`); retenção dos três grupos (fecha a 8.3); `describe-alarms` (estado e ações); estado do agendamento; assinaturas dos dois tópicos; contagem de itens com `scan --select COUNT`; objetos do bucket; e, se houver, o objeto e o log da execução agendada de 09/11. A mesma leitura é feita em dev e hml para a data de conferência da tabela.

O README diz "estado em <data>" no título da tabela e não cita número de versão, porque a release 1.0.1 publica versões novas em hml e prod depois do commit de entrega (Risks). A remoção de hml depois da entrega de 13/11 e a de prod depois da nota acontecem depois do commit de entrega e não aparecem na tabela; uma frase abaixo dela diz isso.

Alternativa descartada: citar a leitura da sessão principal de 08/11. Não está registrada em fonte do repositório, e o requisito exige fonte.

### D15. Arquivamento (decisão 3)
**Critério:** o repositório entregue passa em `openspec validate --all --strict` e em `openspec validate --archived`, com todos os changes arquivados sem caixa aberta.

**Pré-condições:** registro da execução agendada de 09/11 no `qa.md` de `add-relatorio-semanal`; a 8.3 de `add-observabilidade-seguranca` marcada com a referência à leitura de prod registrada pelo QA (a 6.17 e a 6.19 já estão marcadas, e a 8.6 de `add-registro-avaliacao` já saiu das tarefas); `openspec list` mostrando os cinco changes com todas as tarefas concluídas; os documentos no estado final.

**Comandos**, na ordem do backlog, um change por vez, pela sessão principal, no mesmo dia:

```bash
openspec archive add-fundacao-projeto -y
openspec archive add-registro-avaliacao -y
openspec archive add-notificacao-critica -y
openspec archive add-relatorio-semanal -y
openspec archive add-observabilidade-seguranca -y
```

O que o ensaio mostrou, numa cópia descartável da `main`:
- Sem `--skip-specs`, cada archive cria ou atualiza as specs em `openspec/specs/`. A ordem importa só para `entrega-continua`: `add-fundacao-projeto` cria a spec, com o Purpose dele, e `add-observabilidade-seguranca` acrescenta 1 requisito. O aviso de Purpose ignorado do segundo delta é esperado.
- Com tarefa aberta, o `-y` só avisa e segue. Por isso a pré-condição de tarefas concluídas é conferida antes, com `openspec list`, e não pelo archive.
- O diretório vira `openspec/changes/archive/<AAAA-MM-DD>-<change-id>/`, com a data do dia do comando. O ajuste de links usa a listagem real.
- Resultado: 7 specs, com 4, 5, 12, 8, 14, 15 e 6 requisitos (`fundacao-cloud`, `entrega-continua`, `registro-avaliacao`, `notificacao-critica`, `relatorio-semanal`, `observabilidade`, `seguranca`), iguais à soma dos deltas. `openspec validate --specs --strict` passou nas 7.
- `openspec validate --archived` reprova change arquivado com caixa sem marca. No ensaio, reprovou os dois changes que tinham tarefa aberta na `main`. Com as pré-condições acima, os cinco passam.

**Depois do arquivamento dos cinco:** troca de `openspec/changes/<change-id>/` por `openspec/changes/archive/<data>-<change-id>/` nos documentos de entrega, verificação de links (D11), `openspec list` sem os cinco, `openspec list --specs`, contagem de requisitos, `openspec validate --specs --strict` e `openspec validate --archived`.

**Arquivamento deste change**, no último commit da branch: antes, todas as caixas do `tasks.md` estão marcadas, exceto as da seção 9, e os links para o `qa.md` deste change já apontam para o caminho arquivado. A sessão principal roda `openspec archive add-documentacao-entrega -y`, que cria `openspec/specs/documentacao`, marca as caixas da seção 9 no arquivo já arquivado e roda a validação final: `openspec validate --all --strict`, `openspec validate --archived`, links, sigilo e travessão. As tarefas posteriores ao commit de entrega (PR, release 1.0.1, vídeo, PR do link do vídeo, envio, remoções) ficam numa lista sem caixa, para o change arquivado não ter caixa aberta.

**Referências em texto que não são link:** os `qa.md` citam `openspec/changes/<change-id>/qa.md` em texto. São registro histórico e ficam como estão; só os links dos documentos de entrega são ajustados.

Alternativas descartadas:
- `--skip-specs`. Não consolidaria as specs.
- Mover os diretórios à mão. Perde a consolidação e a validação do CLI.
- Deixar caixa aberta com motivo. Reprova `validate --archived`, que a decisão 3 exige verde.

### D16. Paralelismo e ordem (decisão 6)
Cada documento tem um único autor por vez, e os agentes trabalham no mesmo checkout porque os arquivos não se sobrepõem.

| Fase e prazo | Agentes em paralelo | Arquivos |
|---|---|---|
| Até 11/11 12:00 | documentador R | `docs/relatorio-tech-challenge.md`, `docs/img/arquitetura.mmd` e `docs/img/arquitetura.png` (D8) |
| | documentador V | `docs/roteiro-video.md` |
| | qa | leituras da D14 e da D17, `mvn -B verify` para os números do relatório, entregues à sessão principal |
| Até 11/11 18:00 | documentador A | `docs/funcoes.md` |
| | documentador B | `docs/seguranca.md` e a tabela de cobertura de riscos, entregue à sessão principal para o `qa.md` |
| | documentador C | `docs/monitoramento.md` |
| | documentador E, por último na fase | `README.md`, com o Mermaid detalhado, e a entrada 1.0.1 do `CHANGELOG.md` |
| Depois de 11/11 18:00 | qa, depois a sessão principal sozinha | verificações, `qa.md`, arquivamento e ajuste de links |

- O relatório sai antes dos documentos para onde aponta. Os links dele são para arquivos, sem âncora (D7), e os títulos de nível 2 de cada documento são os desta design; o autor que precisar mudar um título avisa a sessão principal.
- O README é o último porque aponta para seções de todos os outros.
- O `qa.md` deste change e o `tasks.md` são escritos só pela sessão principal enquanto houver agentes em paralelo.

### D17. Governança lida no GitHub
Leituras com `gh api`, só GET, feitas pelo QA e registradas no `qa.md`: `repos/{owner}/{repo}/branches/main/protection` e `.../develop/protection` (check obrigatório, PR obrigatório, aprovação exigida, branch atualizada, bypass de administrador); `repos/{owner}/{repo}/environments` e, para cada ambiente, `deployment-branch-policies` e as regras de proteção (revisor de prod, prevent self-review); `repos/{owner}/{repo}` para conferir que o repositório é público e a branch padrão. O `docs/seguranca.md` só afirma o que essas leituras devolvem.

## Risks / Trade-offs

- **Prazo:** depois das verificações e do arquivamento ainda vêm o PR deste change, a release 1.0.1 até prod com aprovação manual, a conferência do SnapStart e o vídeo, tudo antes de 13/11. Se as verificações escorregarem para 12/11, release e vídeo ficam espremidos em 12 e 13/11. Mitigação: o corte, se precisar, é na profundidade de `docs/funcoes.md` e `docs/monitoramento.md`, nunca no relatório nem no roteiro.
- **Documentos começados antes dos ajustes de 10/11:** relatório e roteiro já estão sendo escritos. Eles precisam refletir a remoção de prod só depois da nota, a pré-condição da release 1.0.1, o `sam local` como não executado e os links sem âncora.
- **Release 1.0.1 publica versões novas (decisão 4):** mudar a versão nos `pom.xml` muda o nome de `dominio-<versão>.jar` em `lib/` e, portanto, o pacote de cada função. O deploy em hml e prod publica nova versão das três funções, com novo snapshot. Mitigação: conferência de `Active` e `On` em prod logo depois e de novo na preparação do vídeo; o README não cita número de versão.
- **Tabela de estado vira retrato:** depois da remoção de hml, após 13/11, e de prod, após a nota, o README continua com o estado da data. Mitigação: a data no título da tabela e a frase sobre as remoções previstas.
- **`sam local` sem execução (decisão 5):** o relatório mostra um comando que ninguém rodou no projeto. Mitigação: a marca de não executado e a resposta tirada da spec, sem log inventado.
- **Imagem desatualizada:** a `arquitetura.png` pode divergir do `.mmd` se ele mudar sem regenerar. Mitigação: regenerar na tarefa de ajuste e conferir a olho; o mermaid-cli baixa um Chromium na primeira execução pelo `npx`.
- **Link do vídeo depois do commit:** o relatório entregue no commit tem `[LINK DO VÍDEO]`. Mitigação: PR próprio logo depois da gravação, antes de exportar o PDF.
- **Dado do aluno em prod até depois da nota:** as descrições do vídeo ficam na tabela, no bucket e no e-mail até a remoção, e o PITR mantém um backup de sistema por 35 dias depois dela. Fica registrado em `docs/seguranca.md` e no roteiro. Apagar o backup é decisão do Gabriel.
- **E-mail do autor nos commits:** o metadado dos commits do repositório público traz o e-mail do autor do Git. A spec só cobre arquivos versionados. Se for o mesmo endereço do `EMAIL_ADMIN`, ele já está público, e reescrever histórico fica fora do escopo.
- **Data no caminho arquivado:** se os cinco arquivamentos rodarem em dias diferentes, os prefixos divergem. Mitigação: rodar todos na mesma sessão e ajustar os links pela listagem real de `openspec/changes/archive/`.
- **Cobertura dos riscos:** a tabela de cobertura com 44 itens de designs, mais as observações dos `qa.md`, é a parte mais demorada da escrita. Mitigação: o documentador B começa por ela.
- **Vídeo gera e-mail e dado real em prod:** é o objetivo do bloco 2. Descrições neutras e o `set-alarm-state` único limitam o efeito.

## Open Questions

Nenhuma.
