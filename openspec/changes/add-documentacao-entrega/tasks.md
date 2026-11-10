# Tasks: add-documentacao-entrega

Legenda de responsável: **[IA]** feito pelos agentes, **[Gabriel]** ação manual.

Change só de documentação: nenhum agente altera código, `template.yaml`, `samconfig.toml`, workflows, bootstrap ou `pom.xml`. Na AWS e no GitHub, os agentes só leem (`describe`, `get`, `list`, `gh api` GET). A seção 3 pode começar logo depois da aprovação, lendo a `main`. As seções 5 em diante esperam o merge do PR #16 e a atualização da branch (2.1 e 2.2). Enquanto houver agentes em paralelo, só a sessão principal marca este arquivo e escreve o `qa.md` deste change.

## 1. Aprovação
- [ ] 1.1 [Gabriel] Aprovar o `design.md` e este arquivo, respondendo às premissas P1 a P4 de "Pendente de aprovação"

## 2. Preparação
- [ ] 2.1 [Gabriel] Fazer o merge do PR #16 (`release/1.0.0` para `develop`)
- [ ] 2.2 [Gabriel] Atualizar a branch com os comandos fornecidos (`git fetch` e `git merge origin/develop`, merge simples, sem estratégia)
- [ ] 2.3 [Gabriel] Renovar a sessão do perfil administrativo com `aws sso login --profile tc4-admin`
- [ ] 2.4 [Gabriel] Ligar o Docker Desktop e avisar a sessão principal

## 3. Escrita dos documentos (paralelo, depois da 1.1)
- [ ] 3.1 [IA] documentador A: escrever `docs/funcoes.md` conforme D3, com permissões e variáveis tiradas do `template.yaml` e erros das três specs, e as seções do README atual listadas na D2
- [ ] 3.2 [IA] documentador B: montar a tabela de cobertura dos riscos (cada item das seções de Risks dos cinco designs e das decisões e observações dos cinco `qa.md`, com destino `R`, "resolvido em" ou "não é risco") e entregá-la à sessão principal para o `qa.md` deste change
- [ ] 3.3 [IA] documentador B: escrever `docs/seguranca.md` conforme D4, com o registro de riscos que inclui os 16 da spec e O4 e O5, e a tabela de onde fica o dado do aluno, com o backup de sistema do PITR
- [ ] 3.4 [IA] documentador C: escrever `docs/monitoramento.md` conforme D5, levando a seção "Monitoramento e alarmes" do README atual e os tempos de volta a OK dos `qa.md`
- [ ] 3.5 [IA] documentador D: reescrever `docs/roteiro-video.md` para prod conforme D6, com a preparação sem gravação na tabela, os cinco blocos com tempo, os cuidados na tela e "Depois da gravação" conforme P2
- [ ] 3.6 [IA] documentadores A a D: marcar cada bloco `bash` com `Registro:` ou `Não executado`, conforme D10, e usar só os marcadores da D9

## 4. Leituras e execuções (qa, em paralelo com a seção 3, depois da 2.3)
- [ ] 4.1 [IA] qa: leitura de prod da D14 (stack, alias e configuração das três funções, retenção, alarmes, agendamento, assinaturas, contagem de itens com `scan --select COUNT`, objetos do bucket) e a mesma leitura em dev e hml, entregue à sessão principal para o `qa.md` deste change
- [ ] 4.2 [IA] qa: se a 4.1 achar `relatorios/2026-11-09.txt` em prod, conferir objeto, log e aviso da primeira execução agendada e entregar o registro para uma seção nova do `qa.md` de `add-relatorio-semanal`
- [ ] 4.3 [IA] qa: leituras de governança da D17 com `gh api`, entregues para o `qa.md` deste change
- [ ] 4.4 [IA] qa: depois da 2.4, criar `docs/sam-local/evento-entrada-invalida.json` e `docs/sam-local/variaveis.json` conforme D13, rodar `mvn -B install`, `sam build` e `sam local invoke` com JDK 25, e entregar a saída completa e o tempo de cada passo. Se falhar, registrar o erro e parar para a decisão do Gabriel (D13, "Se não rodar")
- [ ] 4.5 [IA] qa: rodar `mvn -B verify` e `sam validate --lint` e entregar os números de testes por módulo e no total

## 5. Integração (depois das seções 3 e 4 e da 2.2)
- [ ] 5.1 [IA] documentador E: reescrever `README.md` conforme D2, com o diagrama da D8, a tabela de estado da D14 e os passos de implantação na ordem do requisito
- [ ] 5.2 [IA] documentador E: escrever `docs/relatorio-tech-challenge.md` conforme D7, com o mesmo bloco Mermaid do README, a seção Endpoints com os quatro casos e a execução local a partir da saída da 4.4
- [ ] 5.3 [IA] documentador E: se a P4 for aprovada, acrescentar ao `CHANGELOG.md` a entrada da 1.0.1 só com a documentação
- [ ] 5.4 [IA] sessão principal: registrar no `qa.md` deste change as entregas das tasks 3.2, 4.1, 4.3, 4.4 e 4.5

## 6. Verificação (qa, depois da seção 5)
- [ ] 6.1 [IA] qa: conferir cada cenário da spec `documentacao` contra os documentos, inclusive permissões, variáveis, memória, timeout e alarmes contra o `template.yaml`, erros contra as specs e mapeamento de branch contra `deploy.yml` e `samconfig.toml`
- [ ] 6.2 [IA] qa: conferir a cobertura dos riscos contra as fontes e que nenhuma linha do registro tem motivo ou mitigação em branco
- [ ] 6.3 [IA] qa: verificar links e âncoras com o script da D11 e a URL do repositório sem autenticação
- [ ] 6.4 [IA] qa: verificar sigilo conforme D9, sem imprimir os valores, e rodar a busca informativa no histórico
- [ ] 6.5 [IA] qa: verificar ausência de travessão conforme D12 e a origem de cada bloco `bash` conforme D10
- [ ] 6.6 [IA] qa: conferir que o bloco Mermaid do README e o do relatório são idênticos e que o conteúdo dos arquivos de `docs/sam-local/` é igual ao do relatório
- [ ] 6.7 [IA] qa: conferir que a diferença da branch para a `develop` só tem caminhos permitidos pelo requisito "Somente documentação"
- [ ] 6.8 [IA] sessão principal: registrar o relatório de conformidade no `qa.md` deste change e corrigir com os documentadores o que a verificação apontar

## 7. Arquivamento (sessão principal, sequencial, depois da seção 6)
- [ ] 7.1 [IA] sessão principal: em `add-observabilidade-seguranca/tasks.md`, marcar a 6.17 e a 6.19 com a data da confirmação do Gabriel e a 8.3 com a referência à leitura da 4.1
- [ ] 7.2 [IA] sessão principal: em `add-registro-avaliacao/tasks.md`, acrescentar abaixo da 8.6 a linha com o motivo de continuar aberta (D15)
- [ ] 7.3 [IA] sessão principal: rodar `openspec archive <change-id> -y` para os cinco changes, na ordem da D15, no mesmo dia
- [ ] 7.4 [IA] sessão principal: trocar nos documentos de entrega os links `openspec/changes/<change-id>/` pelo caminho arquivado real e rodar de novo a verificação de links
- [ ] 7.5 [IA] qa: conferir `openspec list` sem os cinco changes, `openspec list --specs` com as 7 specs, a contagem de requisitos (4, 5, 12, 8, 14, 15 e 6), `openspec validate --specs --strict` verde, a seção da release 1.0.0 no `qa.md` arquivado de `add-fundacao-projeto` e o motivo ao lado de cada caixa aberta
- [ ] 7.6 [IA] sessão principal: apontar os links para o `qa.md` deste change no caminho arquivado, rodar `openspec archive add-documentacao-entrega -y`, e repetir a verificação de links, `openspec validate --specs --strict` e a busca de travessão e de sigilo
- [ ] 7.7 [IA] sessão principal: entregar ao Gabriel os comandos Git, em ordem, com uma linha explicando cada passo, e a mensagem de commit no padrão do projeto

## Depois do commit de entrega

Itens fora do arquivamento, sem caixa, porque acontecem depois de o change ser arquivado. O vídeo pode ser gravado antes, assim que o roteiro passar pela 6.1.

- [Gabriel] Executar os comandos da 7.7: commits, push e PR para `develop`; conferir o CI verde e fazer o merge
- [Gabriel] Abrir o README e o relatório no GitHub e conferir o diagrama e os links
- [Gabriel] Gravar o vídeo em prod seguindo `docs/roteiro-video.md`, desfocar o que o roteiro manda e publicar
- [Gabriel] Exportar o relatório em PDF pela página do GitHub, conferir o diagrama no PDF e enviar com o link do vídeo à FIAP até 13/11/2026
- [Gabriel] Se a P4 for aprovada: release 1.0.1 a partir da `develop`, com a versão dos `pom.xml` mudada na branch de release, deploy em hml, merge em `main` com aprovação em prod, tag `v1.0.1` e volta para `develop`, antes de remover hml
- [Gabriel] Depois de 13/11/2026, se decidir remover hml e prod: esvaziar o bucket e rodar `sam delete`, conforme a implantação do README
