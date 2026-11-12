# Tasks: add-documentacao-entrega

Legenda de responsável: **[IA]** feito pelos agentes, **[Gabriel]** ação manual.

Change só de documentação: nenhum agente altera código, `template.yaml`, `samconfig.toml`, workflows, bootstrap ou `pom.xml`. Na AWS e no GitHub, os agentes só leem (`describe`, `get`, `list`, `gh api` GET). Prazos do Gabriel (decisão 6 do `design.md`): relatório e roteiro até 11/11 12:00; funções, segurança, monitoramento e README até 11/11 18:00; depois, verificações, arquivamento e comandos da release 1.0.1. Enquanto houver agentes em paralelo, só a sessão principal marca este arquivo e escreve o `qa.md` deste change.

## 1. Aprovação e preparação
- [x] 1.1 [Gabriel] Aprovar o `design.md` e este arquivo. Aprovado em 10/11/2026, com os ajustes registrados em "Decisões do Gabriel de 10/11/2026"
- [x] 1.2 [Gabriel] Fazer o merge do PR #16 (`release/1.0.0` para `develop`) e levar a `develop` à branch deste change

## 2. Leituras (qa, em paralelo com a seção 3)
- [x] 2.1 [IA] qa: leitura de prod da D14 (stack, alias e configuração das três funções, retenção, alarmes, agendamento, assinaturas, contagem de itens com `scan --select COUNT`, objetos do bucket) e a mesma leitura em dev e hml, entregue à sessão principal para o `qa.md` deste change
- [x] 2.2 [IA] qa: leitura da execução agendada de 09/11/2026 nos três ambientes (objeto, log e aviso), entregue para uma seção nova do `qa.md` de `add-relatorio-semanal`
- [x] 2.3 [IA] qa: rodar `mvn -B verify` com JDK 25 e entregar os números de testes por módulo e no total, para a seção Qualidade do código do relatório
- [x] 2.4 [IA] qa: leituras de governança da D17 com `gh api`, entregues para o `qa.md` deste change
- [x] 2.5 [IA] sessão principal: registrar as leituras no `qa.md` deste change e marcar a 8.3 de `add-observabilidade-seguranca` com a referência à leitura de prod

## 3. Relatório e roteiro (até 11/11 12:00)
- [x] 3.1 [IA] documentador R: escrever `docs/relatorio-tech-challenge.md` conforme D7, com o diagrama da D8, a seção Endpoints com os quatro casos, os números da 2.3 e a execução local da D13 marcada como não executada
- [x] 3.2 [IA] documentador V: reescrever `docs/roteiro-video.md` para prod conforme D6, com a pré-condição da release 1.0.1, a preparação sem gravação na tabela, os cinco blocos com tempo, os cuidados na tela e "Depois da gravação" (hml removido depois do vídeo, prod só depois da nota)
- [x] 3.3 [IA] documentadores R e V: marcar cada bloco `bash` com `Registro:` ou `Não executado`, conforme D10, e usar só os marcadores da D9
- [x] 3.4 [Gabriel] Revisar o relatório e o roteiro

## 4. Funções, segurança e monitoramento (até 11/11 18:00, em paralelo)
- [x] 4.1 [IA] documentador A: escrever `docs/funcoes.md` conforme D3, com permissões e variáveis tiradas do `template.yaml`, erros das três specs e as seções do README atual listadas na D2
- [x] 4.2 [IA] documentador B: montar a tabela de cobertura dos riscos (cada item das seções de Risks dos cinco designs e das decisões e observações dos cinco `qa.md`, com destino `R`, "resolvido em" ou "não é risco") e entregá-la à sessão principal para o `qa.md` deste change
- [x] 4.3 [IA] documentador B: escrever `docs/seguranca.md` conforme D4, com o registro de riscos que inclui os 16 da spec, O4 e O5, a tabela de onde fica o dado do aluno com remoção, PITR e expiração, e a pendência pós-entrega do `aspectjrt`
- [x] 4.4 [IA] documentador C: escrever `docs/monitoramento.md` conforme D5, levando a seção "Monitoramento e alarmes" do README atual e os tempos de volta a OK dos `qa.md`
- [x] 4.5 [IA] documentadores A, B e C: marcar os blocos `bash` conforme D10 e usar só os marcadores da D9

## 5. README (por último, até 11/11 18:00)
- [x] 5.1 [IA] documentador E: reescrever `README.md` conforme D2, com o mesmo bloco Mermaid do relatório, a tabela de estado da D14 sem número de versão e os passos de implantação na ordem do requisito
- [x] 5.2 [IA] documentador E: acrescentar ao `CHANGELOG.md` a entrada da 1.0.1, só com a documentação
- [x] 5.3 [Gabriel] Revisar o README e os documentos da seção 4

## 6. Verificação (qa, depois da seção 5)
- [x] 6.1 [IA] qa: conferir cada cenário da spec `documentacao` contra os documentos, inclusive permissões, variáveis, memória, timeout e alarmes contra o `template.yaml`, erros contra as specs e mapeamento de branch contra `deploy.yml` e `samconfig.toml`
- [x] 6.2 [IA] qa: conferir a cobertura dos riscos contra as fontes e que nenhuma linha do registro tem motivo ou mitigação em branco
- [x] 6.3 [IA] qa: verificar links e âncoras com o script da D11 e a URL do repositório sem autenticação
- [x] 6.4 [IA] qa: verificar sigilo conforme D9, sem imprimir os valores, e rodar a busca informativa no histórico
- [x] 6.5 [IA] qa: verificar ausência de travessão conforme D12 e a origem de cada bloco `bash` conforme D10
- [x] 6.6 [IA] qa: conferir que o bloco Mermaid do README e o do relatório são idênticos
- [x] 6.7 [IA] qa: conferir que a diferença da branch para a `develop` só tem caminhos permitidos pelo requisito "Somente documentação"
- [x] 6.8 [IA] sessão principal: registrar o relatório de conformidade no `qa.md` deste change e corrigir com os documentadores o que a verificação apontar

## 7. Arquivamento dos cinco changes (sessão principal, sequencial)
- [x] 7.1 [IA] sessão principal: conferir com `openspec list` que os cinco changes têm todas as tarefas concluídas e que a execução de 09/11 está registrada em `add-relatorio-semanal/qa.md`
- [x] 7.2 [IA] sessão principal: rodar `openspec archive <change-id> -y` para os cinco changes, na ordem da D15, no mesmo dia
- [x] 7.3 [IA] sessão principal: trocar nos documentos de entrega os links `openspec/changes/<change-id>/` pelo caminho arquivado real e rodar de novo a verificação de links
- [x] 7.4 [IA] qa: conferir `openspec list` sem os cinco changes, `openspec list --specs` com as 7 specs, a contagem de requisitos (4, 5, 12, 8, 14, 15 e 6), a seção da release 1.0.0 no `qa.md` arquivado de `add-fundacao-projeto`, `openspec validate --specs --strict` verde e `openspec validate --archived` verde
## 8. Revisão do Gabriel de 11/11/2026 (decisões 7 a 10 do `design.md`)
- [x] 8.1 [IA] documentador V: em `docs/roteiro-video.md`, acrescentar a seção "Ensaio em hml" antes da gravação, fixar a gravação em 12/11/2026 com a referência `2026-11-13`, estender o bloco 5 para 1 min com os changes arquivados, um `qa.md` e `.claude/agents` (total de 11 min 30 s) e trocar a remoção de hml para depois de 13/11
- [x] 8.2 [IA] documentador R: reorganizar `docs/relatorio-tech-challenge.md` conforme D7: cabeçalho do modelo da FIAP com `[LINK DO VÍDEO]`, seções numeradas de 1 a 13 com 12 Processo de desenvolvimento e 13 Aprendizados, apêndice Evidências com as fontes e tom impessoal fora da 13
- [x] 8.3 [IA] documentador R: escrever `docs/img/arquitetura.mmd` enxuto com os três fluxos, gerar `docs/img/arquitetura.png` com o mermaid-cli e trocar o bloco Mermaid do relatório pela imagem
- [x] 8.4 [IA] documentador E: manter o Mermaid detalhado só no README, ajustar a frase de remoção de hml para depois de 13/11 e corrigir links para as seções numeradas do relatório, se houver
- [x] 8.5 [IA] documentador B: em `docs/seguranca.md`, ajustar a remoção de hml para depois de 13/11 na tabela de onde fica o dado do aluno
- [x] 8.6 [IA] qa: conferir que `docs/img/arquitetura.png` e `docs/img/arquitetura.mmd` existem, que o `.mmd` cobre os três fluxos da D8 e que a imagem foi gerada do `.mmd` final; repetir a verificação de links (inclusive o da imagem), sigilo, travessão e origem dos blocos `bash` nos arquivos alterados
- [x] 8.7 [IA] sessão principal: registrar a 8.6 no `qa.md` deste change, apontar os links para o `qa.md` deste change no caminho arquivado e conferir que todas as caixas deste arquivo, exceto as da seção 9, estão marcadas

## 9. Arquivamento deste change (sessão principal, no arquivo já arquivado)
- [x] 9.1 [IA] sessão principal: rodar `openspec archive add-documentacao-entrega -y` e marcar esta caixa no `tasks.md` arquivado
- [x] 9.2 [IA] sessão principal: rodar a validação final, com `openspec validate --all --strict` e `openspec validate --archived` verdes, links, sigilo e travessão, e marcar esta caixa
- [x] 9.3 [IA] sessão principal: entregar ao Gabriel os comandos Git, em ordem, com uma linha explicando cada passo e a mensagem de commit no padrão do projeto, marcar esta caixa e repetir `openspec validate --archived`

## Depois do commit de entrega

Itens sem caixa, porque acontecem depois de o change ser arquivado.

- [Gabriel] Executar os comandos da 9.3: commits, push e PR para `develop`; conferir o CI verde e fazer o merge
- [Gabriel] Release 1.0.1: `release/1.0.1` a partir da `develop`, com a versão dos `pom.xml` mudada na branch de release (preparada por agente, fora deste change); deploy em hml; merge em `main` com a aprovação em prod; tag `v1.0.1`; volta para `develop`
- [IA] qa: logo depois do deploy em prod, conferir só com leitura `State` `Active` e `OptimizationStatus` `On` no alias `live` das três funções de prod e informar o Gabriel antes da gravação
- [Gabriel] Abrir o README e o relatório no GitHub e conferir o diagrama e os links
- [Gabriel] Ensaiar em hml conforme a seção "Ensaio em hml" do roteiro
- [Gabriel] Gravar o vídeo em prod em 12/11/2026 seguindo `docs/roteiro-video.md`, desfocar o que o roteiro manda e publicar
- [IA] documentador e [Gabriel]: PR próprio que troca `[LINK DO VÍDEO]` pelo link publicado no relatório; o agente prepara a alteração e os comandos, o Gabriel faz commit, push, PR e merge
- [Gabriel] Exportar o relatório em PDF pela página do GitHub, conferir o diagrama no PDF e enviar com o link do vídeo à FIAP até 13/11/2026
- [Gabriel] Depois da entrega de 13/11/2026: remover hml, esvaziando o bucket antes do `sam delete`
- [Gabriel] Depois da nota da FIAP: remover prod da mesma forma
