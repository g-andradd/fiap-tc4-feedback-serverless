# documentacao Specification

## Purpose
Documentação de entrega da plataforma de feedback: o README, os documentos de `docs/`, o roteiro do vídeo, o relatório do Tech Challenge e o estado final do OpenSpec. O leitor principal é o avaliador da FIAP, que precisa achar cada critério sem ler os registros de QA.

Nestes requisitos, "commit de entrega" é o último commit da branch do change antes do PR para `develop`, e "documentos de entrega" são `README.md`, os arquivos Markdown de `docs/` e o relatório do Tech Challenge.

## Requirements

### Requirement: README como porta de entrada
O `README.md` SHALL apresentar o problema, uma visão resumida da arquitetura, o estado das funções por ambiente, como buildar e testar, como usar o endpoint, como implantar e um índice com link para cada documento de entrega e para a coleção Postman. O README SHALL NOT repetir as tabelas de configuração de alarmes, de permissões e de riscos, que ficam cada uma em um único documento de `docs/`.

#### Scenario: Diagrama completo no README
- **WHEN** o diagrama de arquitetura do `README.md` é aberto no GitHub
- **THEN** ele aparece em Mermaid, sem ferramenta externa
- **AND** mostra API Gateway, as três funções, DynamoDB com Streams, EventBridge Scheduler, o tópico de administradores, o tópico de alarmes, o bucket de relatórios, CloudWatch, X-Ray e o pipeline do GitHub Actions com OIDC, com as ligações descritas na seção Arquitetura do `project.md`

#### Scenario: Índice completo
- **WHEN** o `README.md` do commit de entrega é inspecionado
- **THEN** ele tem link para `docs/funcoes.md`, `docs/seguranca.md`, `docs/monitoramento.md`, `docs/roteiro-video.md`, o relatório do Tech Challenge e `docs/postman/feedback-serverless.postman_collection.json`

#### Scenario: Links que resolvem
- **WHEN** cada link relativo dos documentos de entrega do commit de entrega é seguido
- **THEN** ele aponta para um arquivo que existe no commit
- **AND** quando traz âncora, a âncora corresponde a um título que existe no arquivo de destino

#### Scenario: Fonte única
- **WHEN** o `README.md` é comparado com `docs/funcoes.md`, `docs/seguranca.md` e `docs/monitoramento.md`
- **THEN** a configuração dos alarmes, as permissões de cada função e o registro de riscos aparecem em tabela em um único desses arquivos, e o README aponta para ele

### Requirement: Estado real das funções por ambiente
O `README.md` SHALL trazer, para cada uma das três funções em dev, hml e prod, o estado na data do commit de entrega, a data da última conferência e a fonte da evidência, que é uma seção de `qa.md` ou o número de uma execução do workflow. Os documentos de entrega SHALL NOT afirmar conferência que não esteja registrada na fonte citada.

#### Scenario: Tabela completa
- **WHEN** a tabela de estado do `README.md` é inspecionada
- **THEN** ela tem as nove combinações de função e ambiente, cada uma com estado, data e fonte

#### Scenario: Afirmação com evidência
- **WHEN** cada célula da tabela que diz que algo foi conferido é comparada com a fonte citada nela
- **THEN** a fonte registra essa conferência, no ambiente e na data indicados

#### Scenario: Sem estado desatualizado
- **WHEN** os documentos de entrega são lidos em busca de frases que digam que um recurso ainda não foi implantado ou ainda não foi conferido
- **THEN** cada uma confere com os registros de QA e de deploy existentes no commit de entrega

#### Scenario: Ambiente removido
- **WHEN** uma stack foi removida antes do commit de entrega
- **THEN** a tabela marca o ambiente como removido, com a data
- **AND** mantém a referência ao que foi conferido nele antes da remoção

#### Scenario: Plano de remoção
- **WHEN** o `README.md` é inspecionado
- **THEN** ele diz que hml só é removido depois da entrega, em 13/11/2026, e que prod fica no ar até a divulgação da nota

### Requirement: Instruções de implantação
O `README.md` SHALL descrever, na ordem em que são feitos, os passos para implantar a plataforma numa conta nova: bootstrap do OIDC e das roles, service-linked role do API Gateway, ambientes do GitHub com a variável `AWS_ROLE_ARN` e o secret `EMAIL_ADMIN`, deploy por branch, aprovação manual em prod, confirmação das assinaturas de e-mail e remoção de uma stack com o esvaziamento do bucket antes do `sam delete`.

#### Scenario: Ordem dos passos
- **WHEN** a seção de implantação é inspecionada
- **THEN** ela traz os passos acima nessa ordem, cada um com o comando ou a configuração correspondente

#### Scenario: Mapeamento de branch e ambiente
- **WHEN** o mapeamento de branch para ambiente descrito é comparado com `.github/workflows/deploy.yml` e `samconfig.toml`
- **THEN** `develop` corresponde a dev e `feedback-dev`, `release/*` a hml e `feedback-hml`, e `main` a prod e `feedback-prod`, como nos arquivos

#### Scenario: Build documentado roda
- **WHEN** os comandos de build e validação citados no README (`mvn -B verify`, `mvn -B install`, `sam build` e `sam validate --lint`) são executados no commit de entrega com JDK 25
- **THEN** cada um termina com sucesso

### Requirement: Documentação das funções
`docs/funcoes.md` SHALL descrever cada uma das três funções com: responsabilidade, gatilho, entrada, saída, regras de negócio, erros tratados e o efeito de cada um, variáveis de ambiente, permissões da role, memória, timeout e uso do alias `live` com SnapStart, métricas emitidas, linhas de log e níveis, e o alarme que a cobre.

#### Scenario: Itens por função
- **WHEN** `docs/funcoes.md` é inspecionado
- **THEN** cada uma de `registrar-avaliacao`, `notificar-critica` e `gerar-relatorio-semanal` tem todos os itens do requisito

#### Scenario: Permissões conferem com o template
- **WHEN** as permissões listadas para uma função são comparadas com as políticas dessa função no `template.yaml` do commit de entrega
- **THEN** toda ação e todo recurso do template aparecem no documento, e o documento não lista permissão que o template não tem

#### Scenario: Erros conferem com as specs
- **WHEN** os erros listados são comparados com os requisitos de `registro-avaliacao`, `notificacao-critica` e `relatorio-semanal`
- **THEN** cada caso de erro desses requisitos aparece, com o mesmo status, código ou mensagem fixa

#### Scenario: Configuração confere com o template
- **WHEN** memória, timeout, gatilho e variáveis de ambiente descritos são comparados com o `template.yaml` do commit de entrega
- **THEN** os valores e os nomes são os mesmos

#### Scenario: Variáveis sem valor sensível
- **WHEN** as variáveis de ambiente de cada função são inspecionadas no documento
- **THEN** cada uma aparece pelo nome e pela origem no template, sem valor que contenha o ID da conta

### Requirement: Segurança e governança
`docs/seguranca.md` SHALL descrever os controles de segurança da nuvem e a governança do repositório e dos ambientes, com uma seção por tema, e cada afirmação SHALL conferir com a configuração implantada ou com a leitura do GitHub na data do commit de entrega.

#### Scenario: Temas cobertos
- **WHEN** `docs/seguranca.md` é inspecionado
- **THEN** há uma seção para cada um destes temas:
  - Acesso do GitHub Actions à AWS por OIDC, sem credencial estática
  - Roles de deploy por ambiente, as negações que elas têm e o boundary das funções
  - Criptografia em repouso da tabela, do tópico de administradores e do bucket, e a ausência dela no tópico de alarmes
  - Exigência de TLS no bucket e no tópico de alarmes
  - Dado do aluno fora dos logs em INFO
  - Lugares onde a descrição do aluno fica guardada ou trafega
  - Isolamento entre ambientes
  - Proteção de `main` e `develop`, restrição de branch de cada ambiente do GitHub e aprovação manual em prod

#### Scenario: Governança confere com o GitHub
- **WHEN** o que o documento diz sobre proteção de branch, check obrigatório, revisor de prod, bypass de administrador e branches aceitas por ambiente é comparado com a leitura por `gh api` na data do commit de entrega
- **THEN** os valores são os mesmos

#### Scenario: Onde fica o dado do aluno
- **WHEN** a seção sobre o dado do aluno é inspecionada
- **THEN** ela cita a tabela de avaliações, os relatórios em `relatorios/`, os lotes descartados em `aws/lambda/` no bucket e o e-mail do aviso crítico
- **AND** diz, para cada um, se há criptografia em repouso e se há expiração

### Requirement: Registro de riscos aceitos
`docs/seguranca.md` SHALL ter um registro de riscos aceitos em que cada linha traz o risco, a origem (change e documento), o motivo da aceitação, a mitigação e, quando houver, a data e o autor da decisão. Quando não houver mitigação, a linha SHALL dizer isso. Risco que tenha condição de revisão SHALL trazer a condição.

#### Scenario: Riscos obrigatórios
- **WHEN** o registro é inspecionado
- **THEN** ele tem uma linha para cada um destes riscos:
  - O4: bypass de administrador nos ambientes do GitHub
  - O5: proteção de branch sem exigir branch atualizada com a base
  - Tópico de alarmes sem criptografia em repouso
  - `RuntimeException` que escapa do handler da notificação sem o marcador de alarme
  - Texto da linha de tempo esgotado do runtime `java25` sem prova real
  - Desvio do `aspectjrt` em escopo `runtime` até a release do Powertools
  - `errorType` e `stackTrace` na resposta da data de referência rejeitada
  - API sem autenticação
  - Os três ambientes na mesma conta AWS
  - Escrita da role de deploy em API Gateway sem restrição por nome
  - Escalada residual pela criação de role até o teto do boundary
  - Descrição do aluno sem expiração nos relatórios e nos lotes descartados
  - Nome do bucket, com o ID da conta, no aviso do relatório
  - `s3:PutObject` da notificação no bucket inteiro
  - X-Ray SDK em manutenção
  - Nível gratuito do CloudWatch sem folga

#### Scenario: Cobertura dos registros do OpenSpec
- **WHEN** cada item dado como aceito nas seções de riscos dos cinco `design.md` e nas decisões e seções de risco aceito dos cinco `qa.md` é procurado no registro
- **THEN** cada um tem uma linha

#### Scenario: Campos preenchidos
- **WHEN** as linhas do registro são inspecionadas
- **THEN** nenhuma tem motivo ou mitigação em branco

#### Scenario: Pendência pós-entrega do aspectjrt
- **WHEN** a linha do desvio do `aspectjrt` é inspecionada
- **THEN** ela traz as três condições de remoção: uma release do Powertools com o commit `f628502`, o teste de priming passando sem o jar em cada função e um deploy com `State: Active` e `OptimizationStatus: On` no alias `live` das três funções
- **AND** aponta para a seção "Pendências pós-entrega" do `openspec/project.md`, que traz as mesmas condições

### Requirement: Monitoramento
`docs/monitoramento.md` SHALL descrever: os logs das funções, com formato, retenção por ambiente e como consultar; as métricas, com o namespace por ambiente e a origem de cada uma; os três alarmes, com o filtro, a configuração e o que dispara ou não cada um; o dashboard e seus widgets; o rastreamento no X-Ray e como abrir o trace de uma invocação; como testar um alarme sem alterar função; e os limites do nível gratuito com as regras que decorrem deles.

#### Scenario: Temas cobertos
- **WHEN** `docs/monitoramento.md` é inspecionado
- **THEN** cada tema do requisito tem uma seção própria

#### Scenario: Alarmes conferem com o template
- **WHEN** o padrão do filtro, a estatística, o limiar, o período, os períodos de avaliação, o tratamento de dado ausente e as ações descritos são comparados com o `template.yaml` do commit de entrega
- **THEN** os valores são os mesmos

#### Scenario: Teste de alarme
- **WHEN** a seção de teste de alarme é inspecionada
- **THEN** ela traz o teste com `aws cloudwatch set-alarm-state` e o teste com `aws logs put-log-events` num stream de teste, com os comandos registrados como executados nos `qa.md`
- **AND** avisa que cada teste gera e-mail real
- **AND** dá o tempo observado de volta a OK de cada teste, de 60 a 80 segundos depois do `set-alarm-state` e de 10 a 11 minutos depois da linha de log

#### Scenario: Limites do nível gratuito
- **WHEN** a seção de limites é inspecionada
- **THEN** ela traz a conta de 10 de 10 métricas de alarme e 3 de 3 dashboards com os três ambientes e o alarme de cobrança
- **AND** proíbe criar alarme ou dashboard de teste

### Requirement: Roteiro do vídeo em produção
`docs/roteiro-video.md` SHALL ser o roteiro da gravação em prod e SHALL trazer: a preparação antes de gravar, uma seção "Ensaio em hml" antes da gravação, os blocos em ordem com o tempo estimado de cada um e o total, e os cuidados com o que aparece na tela. Os blocos SHALL mostrar a aplicação funcionando, as três funções ativas na AWS e as configurações de nuvem, segurança, pipeline e monitoramento.

#### Scenario: Exigência do enunciado
- **WHEN** o roteiro é inspecionado
- **THEN** há ao menos um bloco para a aplicação funcionando, com registro de avaliação, aviso crítico e relatório
- **AND** um bloco que mostra as três funções ativas, com o gatilho de cada uma e o alias `live` com SnapStart
- **AND** um bloco que mostra as configurações: template, pipeline com aprovação em prod, segurança e monitoramento

#### Scenario: Recursos de produção
- **WHEN** os comandos e os nomes de recurso da preparação e dos blocos da gravação são inspecionados
- **THEN** todos usam a stack `feedback-prod` e o sufixo `-prod`

#### Scenario: Ensaio em hml
- **WHEN** a seção "Ensaio em hml" é inspecionada
- **THEN** ela vem antes dos blocos da gravação e repete os mesmos passos, com a stack `feedback-hml` e o sufixo `-hml`
- **AND** nenhum comando dela cita recurso `-prod` ou a stack `feedback-prod`, então as avaliações do ensaio ficam só em hml

#### Scenario: Tabela de produção começa limpa
- **WHEN** a preparação do roteiro é seguida
- **THEN** ela inclui conferir que a tabela `feedback-avaliacoes-prod` tem 0 itens antes de gravar
- **AND** nenhum passo da preparação grava item nessa tabela

#### Scenario: Pré-condições de produção
- **WHEN** a preparação é inspecionada
- **THEN** ela manda gravar só depois do deploy da versão 1.0.1 em prod
- **AND** manda conferir, só com leitura, `State` igual a `Active` e `OptimizationStatus` igual a `On` no alias `live` das três funções de prod, logo depois desse deploy
- **AND** manda conferir a confirmação das assinaturas dos tópicos de administradores e de alarmes de prod e os três alarmes de prod em OK

#### Scenario: O que desfocar
- **WHEN** a lista de cuidados é inspecionada
- **THEN** ela manda desfocar o ID da conta no menu do console, nos ARNs, nas variáveis de ambiente das funções e no nome do bucket, o endereço de e-mail no corpo e no rodapé das mensagens, e o identificador da API na URL
- **AND** manda mostrar só a mensagem, sem o `stackTrace`, se a data rejeitada aparecer

#### Scenario: Depois da gravação
- **WHEN** a parte do roteiro sobre o que vem depois da gravação é inspecionada
- **THEN** ela diz que as avaliações, os relatórios e os e-mails gerados em prod ficam como estão, sem limpeza manual da tabela
- **AND** que o relatório agendado de prod das segundas-feiras inclui essas avaliações enquanto a stack existir
- **AND** que hml só é removido depois da entrega, em 13/11/2026, e prod só depois da divulgação da nota, nos dois casos com o bucket esvaziado antes do `sam delete`

### Requirement: Relatório do Tech Challenge
O relatório SHALL ficar em `docs/relatorio-tech-challenge.md` e seguir o modelo da FIAP adaptado na seção Entregáveis do `project.md`, com cabeçalho, seções numeradas de 1 a 13 e um apêndice de evidências. As seções extras da fase 4 SHALL apontar para o documento de `docs/` correspondente. O PDF enviado à FIAP é exportado desse arquivo e SHALL NOT ser versionado.

#### Scenario: Formato e local
- **WHEN** os arquivos versionados do commit de entrega são listados
- **THEN** o relatório existe em `docs/relatorio-tech-challenge.md`
- **AND** não há arquivo PDF ou `.docx` do relatório no repositório

#### Scenario: Cabeçalho do modelo da FIAP
- **WHEN** o início do relatório é inspecionado
- **THEN** ele traz o nome do projeto, a equipe com o RM de cada integrante, o curso e o link do vídeo
- **AND** até o PR que acrescenta o link, o lugar do link traz o marcador `[LINK DO VÍDEO]`, que não reprova nenhum cenário desta spec, inclusive o de links que resolvem

#### Scenario: Seções na ordem
- **WHEN** os títulos do relatório são listados
- **THEN** aparecem, numeradas e nesta ordem:
  1. Introdução, com problema e objetivo
  2. Arquitetura, com descrição e diagrama
  3. Endpoints, com tabela e exemplos de requisição e resposta
  4. Configuração do projeto, com SAM e pipelines no lugar do Docker Compose e execução local com `sam local`
  5. Qualidade do código, com práticas e padrões adotados
  6. Collections para teste, com a coleção Postman
  7. Repositório, com a URL
  8. Modelo de cloud
  9. Segurança e governança
  10. Monitoramento
  11. Documentação das funções
  12. Processo de desenvolvimento
  13. Aprendizados, escrito em primeira pessoa
- **AND** depois da seção 13 vem o apêndice Evidências

#### Scenario: Seções extras apontam para docs
- **WHEN** as seções de segurança e governança, monitoramento e documentação das funções do relatório são inspecionadas
- **THEN** cada uma tem link para `docs/seguranca.md`, `docs/monitoramento.md` e `docs/funcoes.md`, respectivamente

#### Scenario: Diagrama da arquitetura
- **WHEN** a seção Arquitetura do relatório é inspecionada
- **THEN** ela mostra a imagem `docs/img/arquitetura.png`, com os três fluxos (registro, aviso crítico e relatório semanal), a observabilidade e o pipeline
- **AND** a fonte `docs/img/arquitetura.mmd` está versionada ao lado da imagem
- **AND** a imagem não mostra ID da conta, e-mail nem URL real da API

#### Scenario: Endpoints conferem com a spec
- **WHEN** a tabela e os exemplos da seção Endpoints são comparados com a spec `registro-avaliacao`
- **THEN** os casos 201, 400 com `ENTRADA_INVALIDA`, 400 com `CORPO_INVALIDO` e 500 com `ERRO_INTERNO` aparecem com o mesmo corpo de resposta
- **AND** a URL aparece com marcador no lugar do identificador da API

#### Scenario: Coleção Postman
- **WHEN** a seção Collections para teste é inspecionada
- **THEN** ela diz onde está o arquivo, como definir `baseUrl` sem o caminho `/avaliacao`, quantas requisições a coleção tem e contra qual ambiente ela foi executada, conforme os `qa.md`

#### Scenario: Repositório público
- **WHEN** a URL da seção Repositório é aberta sem autenticação
- **THEN** ela mostra o repositório público do projeto

#### Scenario: Números de qualidade
- **WHEN** a quantidade de testes citada na seção Qualidade do código é comparada com a saída de `mvn -B verify` no commit de entrega
- **THEN** os números são os mesmos

### Requirement: Execução local documentada
A execução local de ao menos uma função com `sam local invoke` SHALL ser documentada em duas partes. O relatório SHALL trazer só o comando e a nota de que ele não foi executado, porque o Docker não está disponível no ambiente do projeto, com link para o README. O detalhe SHALL ficar na subseção "Execução local com `sam local`" do `README.md`.

#### Scenario: Relatório com comando e nota
- **WHEN** a parte de execução local do relatório é inspecionada
- **THEN** ela traz o comando de `sam local invoke` e a nota de que ele não foi executado porque o Docker não está disponível no ambiente
- **AND** tem link para a subseção "Execução local com `sam local`" do `README.md`

#### Scenario: Detalhe no README
- **WHEN** a subseção "Execução local com `sam local`" do `README.md` é inspecionada
- **THEN** ela traz o mesmo comando do relatório, o evento de entrada, as variáveis de ambiente e o resultado esperado
- **AND** diz que o Docker é pré-requisito e que o comando não foi executado, com o motivo

#### Scenario: Evento coerente com a spec
- **WHEN** o evento de entrada e o resultado esperado do README são comparados com a spec da função invocada
- **THEN** o resultado esperado é o que a spec define para aquela entrada

#### Scenario: Limites da execução local
- **WHEN** a subseção de execução local do README é inspecionada
- **THEN** ela diz quais recursos da AWS são acessados, ou que nenhum é
- **AND** lista o que a execução local não reproduz, inclusive SnapStart, aquecimento dos clientes e gatilhos reais

### Requirement: Arquivamento dos changes implantados
Os cinco changes implantados e o próprio `add-documentacao-entrega` SHALL ser arquivados com `openspec archive`, com as specs consolidadas em `openspec/specs/`. Nenhum change arquivado SHALL ter tarefa aberta. O que depende de terceiros e não fecha até a entrega SHALL sair das tarefas e ficar na seção "Pendências pós-entrega" do `openspec/project.md`.

#### Scenario: Specs consolidadas
- **WHEN** `openspec list --specs` é executado depois do arquivamento
- **THEN** a lista traz `fundacao-cloud`, `entrega-continua`, `registro-avaliacao`, `notificacao-critica`, `relatorio-semanal`, `observabilidade`, `seguranca` e `documentacao`

#### Scenario: Requisitos preservados
- **WHEN** a quantidade de requisitos de cada spec em `openspec/specs/` é comparada com a soma dos requisitos adicionados pelos deltas dos changes arquivados
- **THEN** os números são iguais, inclusive em `entrega-continua`, que recebe deltas de `add-fundacao-projeto` e `add-observabilidade-seguranca`

#### Scenario: Specs válidas
- **WHEN** `openspec validate --specs --strict` é executado depois do arquivamento
- **THEN** todas as specs são válidas

#### Scenario: Nenhum change ativo
- **WHEN** `openspec list` é executado no commit de entrega
- **THEN** nenhum change aparece como ativo

#### Scenario: Evidência da release preservada
- **WHEN** o `qa.md` arquivado de `add-fundacao-projeto` é inspecionado
- **THEN** ele contém a seção da release 1.0.0 em hml, de 07/11/2026

#### Scenario: Nenhuma tarefa aberta
- **WHEN** `openspec validate --archived` é executado no commit de entrega
- **THEN** todos os changes arquivados passam, sem nenhuma caixa sem marca nos `tasks.md`

#### Scenario: Pendência fora das tarefas
- **WHEN** o `openspec/project.md` do commit de entrega é inspecionado
- **THEN** a seção "Pendências pós-entrega" traz o desvio do `aspectjrt` com as três condições de remoção
- **AND** nenhum `tasks.md` arquivado traz a tarefa de rever o desvio

### Requirement: Validações do repositório entregue
O commit de entrega SHALL passar em todas as validações do projeto: build e testes, lint dos dois templates e validação estrita do OpenSpec, inclusive dos changes arquivados.

#### Scenario: Build e templates
- **WHEN** `mvn -B verify`, `sam validate --lint --region sa-east-1` e `sam validate --lint --region sa-east-1 -t infra/bootstrap/github-oidc.yaml` são executados no commit de entrega com JDK 25
- **THEN** os três terminam com sucesso

#### Scenario: OpenSpec
- **WHEN** `openspec validate --all --strict` e `openspec validate --archived` são executados no commit de entrega
- **THEN** os dois terminam sem erro

#### Scenario: CI do pull request
- **WHEN** o PR deste change para `develop` é aberto
- **THEN** o check obrigatório `ci` termina com sucesso

### Requirement: Publicação da documentação na versão 1.0.1
O `CHANGELOG.md` SHALL ter a entrada da versão 1.0.1, com a documentação de entrega e o arquivamento do OpenSpec, e SHALL dizer que não há mudança de código, template ou pipeline. A release 1.0.1 é feita pelo Gabriel, fora deste change, e SHALL chegar a prod antes da gravação do vídeo.

#### Scenario: Entrada do changelog
- **WHEN** o `CHANGELOG.md` do commit de entrega é inspecionado
- **THEN** há uma seção `[1.0.1]` com a documentação de entrega e o arquivamento
- **AND** ela diz que código, template e pipelines não mudaram

#### Scenario: Conferência em prod depois da release
- **WHEN** o deploy da versão 1.0.1 em prod termina
- **THEN** uma conferência só de leitura no alias `live` das três funções de prod traz `State` igual a `Active` e `OptimizationStatus` igual a `On`, antes da gravação do vídeo

### Requirement: Sigilo no repositório público
Nenhum arquivo versionado no commit de entrega SHALL conter o ID da conta AWS, o endereço de e-mail dos administradores ou o identificador ou a URL de uma API implantada. Os documentos SHALL usar marcadores no lugar desses valores, como `<ID_DA_CONTA>` e `<ID_DA_API>`.

#### Scenario: ID da conta
- **WHEN** o repositório no commit de entrega é buscado pelo ID da conta devolvido por `aws sts get-caller-identity`
- **THEN** não há nenhuma ocorrência

#### Scenario: Identificador da API
- **WHEN** o repositório no commit de entrega é buscado pelo identificador de cada API que aparece no output `ApiAvaliacoesUrl` das stacks existentes
- **THEN** não há nenhuma ocorrência

#### Scenario: E-mail dos administradores
- **WHEN** o repositório no commit de entrega é buscado pelo endereço do secret `EMAIL_ADMIN`, informado na hora da busca, sem gravá-lo em arquivo
- **THEN** não há nenhuma ocorrência

### Requirement: Escrita
Os documentos de entrega SHALL ser escritos em português do Brasil e SHALL NOT conter o caractere travessão (U+2014).

#### Scenario: Sem travessão
- **WHEN** os documentos de entrega e o `CHANGELOG.md` são buscados pelo caractere U+2014
- **THEN** não há nenhuma ocorrência
- **AND** nos demais arquivos alterados pelo change, a única ocorrência permitida é a linha do `openspec/project.md` que enuncia a própria regra

### Requirement: Comandos verificados
Todo bloco de comando dos documentos de entrega SHALL corresponder a um comando executado, com registro em `qa.md`, em incidente ou na validação deste change, ou SHALL vir marcado no texto como não executado.

#### Scenario: Origem de cada comando
- **WHEN** cada bloco de comando dos documentos de entrega é inspecionado
- **THEN** ele tem registro de execução ou a marca de não executado

### Requirement: Somente documentação
O change SHALL alterar só `README.md`, `CHANGELOG.md`, arquivos de `docs/` e arquivos de `openspec/`. Código, `template.yaml`, `samconfig.toml`, workflows, o template de bootstrap e os `pom.xml` SHALL ficar como estão. A versão 1.0.1 dos `pom.xml` entra na branch de release, fora deste change.

#### Scenario: Diferença da branch
- **WHEN** a diferença entre o commit de entrega e a `develop` é listada por arquivo
- **THEN** só aparecem caminhos permitidos pelo requisito
