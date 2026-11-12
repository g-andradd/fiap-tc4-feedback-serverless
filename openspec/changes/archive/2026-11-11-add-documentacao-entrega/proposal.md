# Change: Documentação de entrega do Tech Challenge

## Why

A versão 1.0.0 está em produção desde 08/11/2026 (execução 42438770120 do workflow Deploy, tag `v1.0.0`), e a entrega na FIAP é em 13/11/2026. O `project.md` lista três entregáveis: o repositório público com documentação, o vídeo e o relatório no modelo da FIAP. Hoje só o primeiro existe em parte:

- O `README.md` cresceu change a change e mistura visão geral, procedimento e evidência de QA. Partes dele estão desatualizadas: diz que hml e prod não foram implantados e que a conferência do reprocessamento não foi feita, e cita a função de registro como a única conferida.
- `docs/` tem só a coleção Postman, o incidente de 04/11/2026 e um roteiro parcial do vídeo, escrito para dev e marcado como não ensaiado.
- Não há documentação por função, nem documento de segurança e governança, nem de monitoramento, que o `project.md` exige como seções extras da fase 4.
- As observações O4 e O5 foram aceitas pelo Gabriel em 04/11/2026 com a instrução de irem para `docs/seguranca.md` neste change. Os demais riscos aceitos estão espalhados em cinco `design.md` e cinco `qa.md`.
- O relatório da FIAP não existe.
- Nenhum change foi arquivado, e `openspec/specs/` está vazio. O `project.md` manda arquivar depois do deploy em produção, o que já aconteceu.

## What Changes

- `README.md` reescrito como porta de entrada: problema, arquitetura resumida, estado real de cada função em dev, hml e prod com data e evidência, como buildar, testar, usar e implantar, e índice dos documentos
- `docs/funcoes.md`: as três funções, cada uma com gatilho, entrada, saída, regras, erros, variáveis de ambiente, permissões, configuração de execução, métricas, logs e alarme
- `docs/seguranca.md`: OIDC, roles de deploy e boundary, criptografia, TLS, dado do aluno fora dos logs e onde ele fica guardado, isolamento por ambiente, proteção de branch e aprovação em prod, mais um registro único de riscos aceitos, com O4, O5 e os riscos dos designs e dos `qa.md`, cada um com motivo e mitigação
- `docs/monitoramento.md`: logs, métricas, alarmes, dashboard, X-Ray, como testar um alarme e os limites do nível gratuito
- `docs/roteiro-video.md` reescrito para prod: preparação sem gravar na tabela, ensaio em hml antes da gravação, blocos com tempo, a aplicação, as funções ativas e as configurações, e a lista do que desfocar
- `docs/relatorio-tech-challenge.md`: relatório do Tech Challenge no modelo da FIAP, com cabeçalho (projeto, equipe com RM, curso e o marcador `[LINK DO VÍDEO]` até o PR do link), as seções do `project.md`, Entregáveis, item 3, numeradas de 1 a 13 com Processo de desenvolvimento e Aprendizados, e o apêndice Evidências. O PDF enviado à FIAP é exportado dele e não é versionado
- Diagrama de arquitetura em duas formas: Mermaid completo no README e, no relatório, a imagem enxuta `docs/img/arquitetura.png`, gerada de `docs/img/arquitetura.mmd`, versionado ao lado
- Arquivamento dos cinco changes implantados e do próprio `add-documentacao-entrega` com `openspec archive`, com as specs consolidadas em `openspec/specs/` e nenhuma tarefa aberta nos changes arquivados
- Pendência pós-entrega do desvio do `aspectjrt` registrada no `openspec/project.md` e em `docs/seguranca.md`, no lugar da tarefa 8.6 de `add-registro-avaliacao`
- Entrada da versão 1.0.1 no `CHANGELOG.md`, só com documentação. A release 1.0.1 vai para prod antes da gravação do vídeo
- Repositório entregue verde em todas as validações: build e testes, lint dos templates e OpenSpec, inclusive a validação dos changes arquivados
- Regras transversais: nenhum ID de conta, e-mail ou URL real da API no repositório, nenhum travessão, links que resolvem e comandos citados que rodam ou que vêm marcados como não executados

### Fora do escopo

- **Mudança de código, template, pipeline, bootstrap ou `pom.xml`:** o change é só documentação. Se a escrita revelar defeito, ele vira observação para o orquestrador, não correção aqui.
- **Correção dos riscos aceitos:** O4, O5, `stackTrace` na data rejeitada, `RuntimeException` sem marcador e os demais ficam como estão. Este change só os documenta.
- **Remoção do desvio do `aspectjrt`:** depende de uma release do Powertools que ainda não saiu. Fica como pendência pós-entrega no `project.md`.
- **Nova funcionalidade, endpoint ou requisição na coleção Postman.**
- **Tradução** da documentação para outro idioma.
- **Site de documentação**, como GitHub Pages ou gerador estático.
- **Verificação automática de links ou de sigilo no CI:** exige mudança de pipeline. As verificações deste change são feitas na validação.
- **Diagramas além do de arquitetura**, como sequência ou implantação.
- **Gravação, edição e publicação do vídeo, e envio do relatório ao portal da FIAP:** são do Gabriel. O change entrega o roteiro e o relatório.
- **Remoção das stacks com `sam delete`:** operação do Gabriel. hml só é removido depois da entrega, em 13/11/2026, e prod só depois da divulgação da nota. A documentação registra o plano e os passos, não a remoção em si.
- **Execução de `sam local`:** o Docker não está disponível no ambiente. A instrução fica documentada e marcada como não executada: no relatório, só o comando e a nota; no README, a subseção "Execução local com `sam local`", com evento, variáveis, resultado esperado, pré-requisito do Docker e o que a execução local não reproduz.
- **A release 1.0.1 em si:** branch, versão dos `pom.xml`, merge e tag são comandos do Gabriel na branch de release, fora deste change.
- **Conferência da primeira execução agendada do relatório (09/11/2026):** pertence ao QA de `add-relatorio-semanal`. A documentação só cita o resultado se ele estiver registrado.
- **Reescrita de `docs/incidentes/` e dos `qa.md`:** são registro histórico. A única mudança é trocar, no incidente de 04/11/2026, a menção à tarefa 8.6 pela pendência do `project.md`.

## Impact

- Spec nova: `documentacao`
- Arquivos afetados: `README.md`, `docs/funcoes.md`, `docs/seguranca.md`, `docs/monitoramento.md`, `docs/roteiro-video.md`, `docs/relatorio-tech-challenge.md`, `docs/img/arquitetura.png` e `docs/img/arquitetura.mmd`, `docs/incidentes/2026-11-04-snapstart-aspectjrt.md` (uma frase), `CHANGELOG.md`, `openspec/project.md`, `openspec/changes/` e `openspec/specs/`
- Nenhum recurso novo na AWS e nenhum custo novo
- Dependências:
  - **PR #16 (`release/1.0.0` para `develop`):** mergeado, e a branch deste change já recebeu a `develop` com a release
  - **Arquivamento move caminhos:** o README e os documentos citam `openspec/changes/<change-id>/qa.md`. Depois do `openspec archive`, esses arquivos ficam em `openspec/changes/archive/<data>-<change-id>/`, e os links precisam apontar para o caminho novo
  - **Tarefas abertas antes do arquivamento:** 6.17, 6.19 e 8.3 de `add-observabilidade-seguranca` precisam estar concluídas, porque `openspec validate --archived` reprova change arquivado com tarefa aberta. A 8.6 de `add-registro-avaliacao` já saiu da lista e virou pendência pós-entrega
  - **Leitura na AWS e no GitHub:** a conferência do estado por ambiente, da proteção de branch e do sigilo usa leitura com o perfil `tc4-admin` e `gh api`, sem escrita
- Caminho até a `main`: release 1.0.1, antes da gravação. O push em `release/1.0.1` publica em hml e o merge em `main` publica em prod com aprovação manual. Sem mudança no template, os dois deploys não alteram recursos, mas publicam novas versões das funções. Por isso, logo depois do deploy em prod, uma conferência só de leitura confirma `State: Active` e `OptimizationStatus: On` no alias `live` das três funções de prod. hml só pode ser removido depois da release, porque o push em `release/**` recriaria a stack
- Prazo: relatório e roteiro prontos para revisão do Gabriel em 11/11/2026 às 12:00; entrega em 13/11/2026
- Ações manuais do Gabriel:
  - Confirmar 6.17 e 6.19 de `add-observabilidade-seguranca` antes do arquivamento
  - Executar os comandos Git fornecidos e a release 1.0.1, com a aprovação de prod
  - Gravar o vídeo em prod seguindo o roteiro, desfocar o que o roteiro manda e publicar
  - Remover hml depois da entrega e prod depois da divulgação da nota
  - Exportar o PDF do relatório
  - Enviar o relatório e o link do vídeo à FIAP

## Rastreabilidade

| Requisito | Origem |
|---|---|
| README como porta de entrada | `project.md`, Entregáveis, item 1; escopo do change, item 1 |
| Estado real das funções por ambiente | Escopo do change, item 1; `project.md`, Ambientes e Gitflow |
| Instruções de implantação | `project.md`, Entregáveis, item 1 (IaC e pipelines); escopo do change, item 1 |
| Documentação das funções | `project.md`, Entregáveis, item 3 (documentação das funções); escopo do change, item 2 |
| Segurança e governança | `project.md`, Entregáveis, item 3 (segurança e governança); escopo do change, item 3 |
| Registro de riscos aceitos | `add-fundacao-projeto/qa.md`, decisão de 04/11/2026 sobre O4 e O5; riscos dos designs e `qa.md`; escopo do change, item 3 |
| Monitoramento | `project.md`, Entregáveis, item 3 (monitoramento), e Restrições (nível gratuito); escopo do change, item 4 |
| Roteiro do vídeo em produção | `project.md`, Entregáveis, item 2; escopo do change, item 5 |
| Relatório do Tech Challenge | `project.md`, Entregáveis, item 3; escopo do change, item 6 |
| Execução local documentada | `project.md`, Entregáveis, item 3 (execução local com `sam local`); decisões P5 e R5 |
| Arquivamento dos changes implantados | `project.md`, Convenções, SDD, item 5; escopo do change, item 7; decisão P3 |
| Validações do repositório entregue | Decisão P3 |
| Publicação da documentação na versão 1.0.1 | Decisão P4 |
| Sigilo no repositório público | `project.md`, Entregáveis, item 1 (repositório público); restrição do escopo do change |
| Escrita | `project.md`, Convenções, Escrita |
| Comandos verificados | Regra do documentador (documentar só o que existe e foi verificado); restrição do escopo do change |
| Somente documentação | `project.md`, Restrições (prazo); restrição do escopo do change |

"Escopo do change" é a lista de 08/11/2026 repassada pelo orquestrador em nome do Gabriel.

## Decisões fora do project.md

| Id | Decisão | Motivo | Origem |
|---|---|---|---|
| D1 | O README é visão geral e índice. Configuração de alarmes, permissões, riscos e procedimentos longos ficam em um único documento de `docs/`, e o README aponta para ele | Hoje a mesma informação está no README e nos `qa.md`, e já divergiu (observação K de `add-observabilidade-seguranca`, tempo de volta a OK) | Recomendação do PO |
| D2 | Um único registro de riscos aceitos, em `docs/seguranca.md`, com os riscos de segurança e os operacionais. `docs/monitoramento.md` aponta para as linhas que tratam de alarme | O escopo pede todos os riscos aceitos no documento de segurança, e dois registros tenderiam a divergir | Escopo do change, item 3 |
| D3 | Além do ID da conta e do e-mail, o vídeo esconde o identificador da API na URL | A API não tem autenticação (`add-registro-avaliacao`, design, Risks), e o vídeo é público para quem receber o link | Recomendação do PO, derivada da restrição de não publicar a URL real |
| D4 | A preparação do vídeo não grava item na tabela de prod. O aquecimento antes de gravar, se houver, usa só requisições rejeitadas ou acionamento que não grava | O escopo pede que a tabela de prod comece limpa, e o design de `add-fundacao-projeto` manda invocar as funções antes do vídeo | Escopo do change, item 5 |
| D5 | Todo bloco de comando nos documentos ou foi executado e tem registro, ou vem marcado como não executado | O README atual já mistura os dois casos, e o avaliador não tem como distinguir | Regra do documentador |

## Decisões do Gabriel na aprovação (10/11/2026)

| Id | Decisão |
|---|---|
| P1 | Relatório em Markdown, em `docs/relatorio-tech-challenge.md`. O PDF é exportado dele e não é versionado |
| P2 | O vídeo registra avaliações ao vivo em prod, sem limpeza manual da tabela. hml só é removido depois da entrega (ajuste de 11/11/2026), e prod só depois da divulgação da nota |
| P3 | Arquivamento no fim deste change, inclusive do próprio `add-documentacao-entrega`. A 8.6 de `add-registro-avaliacao` sai das tarefas e vira pendência pós-entrega no `project.md` e em `docs/seguranca.md`. O repositório entregue passa em todas as validações, inclusive `openspec validate --archived` |
| P4 | Release 1.0.1 em prod antes da gravação, com conferência só de leitura de SnapStart `Active` e `On` nas três funções de prod logo depois |
| P5 | `sam local invoke` documentado e marcado como não executado, porque o Docker não está disponível. A execução não é obrigatória |

## Ajustes da revisão do Gabriel (11/11/2026)

| Id | Ajuste |
|---|---|
| R1 | hml só é removido depois da entrega, em 13/11/2026. prod continua até a divulgação da nota |
| R2 | O roteiro ganha a seção "Ensaio em hml" antes da gravação, com os mesmos passos e nomes `-hml`. Os POSTs do ensaio gravam em hml, sem efeito em prod |
| R3 | O relatório usa a imagem `docs/img/arquitetura.png`, gerada de `docs/img/arquitetura.mmd`, numa versão enxuta para o PDF. O Mermaid completo fica só no README |
| R4 | Relatório com cabeçalho do modelo da FIAP, seções numeradas de 1 a 13 (12 Processo de desenvolvimento, 13 Aprendizados em primeira pessoa) e apêndice Evidências. O marcador `[LINK DO VÍDEO]` fica até o PR do link e não conta como pendência |
| R5 | Execução local dividida: o relatório traz só o comando e a nota de não execução, e o detalhe fica na subseção "Execução local com `sam local`" do README |
