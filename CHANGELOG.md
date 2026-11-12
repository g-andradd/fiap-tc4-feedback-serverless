# Changelog

Este projeto segue o [versionamento semântico](https://semver.org/lang/pt-BR/).

## [1.0.1] - 2026-11-12

### Documentação da entrega (`add-documentacao-entrega`)

- README reescrito como porta de entrada do projeto.
- Documentação das funções em `docs/funcoes.md`.
- Segurança e governança em `docs/seguranca.md`, com o registro de riscos aceitos (incluindo O4 e O5) e a pendência pós-entrega do `aspectjrt`.
- Monitoramento em `docs/monitoramento.md`.
- Roteiro do vídeo de demonstração em prod, em `docs/roteiro-video.md`.
- Relatório do Tech Challenge em `docs/relatorio-tech-challenge.md`.
- Changes concluídos arquivados no OpenSpec, com as specs consolidadas em `openspec/specs/`.
- Versão dos módulos Maven em 1.0.1.
- Código, `template.yaml` e pipelines não mudaram. A única mudança fora da documentação é a versão dos `pom.xml`, feita na branch de release.

## [1.0.0] - 2026-11-07

### Fundação do projeto e da entrega contínua (`add-fundacao-projeto`)

- Projeto Maven multi-módulo em Java 25, com um módulo de domínio compartilhado e um módulo por função.
- Template SAM parametrizado por ambiente (`dev`, `hml`, `prod`), com tabela DynamoDB de avaliações (Streams habilitado e índice `por-data`), tópico SNS de administradores com assinatura por e-mail e bucket S3 privado para relatórios. Os três recursos têm criptografia em repouso, e o bucket nega acesso público e acesso sem TLS.
- Acesso do GitHub Actions à AWS por OIDC, sem chave de acesso guardada no repositório, com uma role de deploy por ambiente restrita ao repositório e ao ambiente do GitHub.
- CI em pull request com build, testes e lint dos templates. Deploy por branch: `develop` publica em dev, `release/*` em hml e `main` em prod, este último com aprovação manual.

### Registro de avaliações (`add-registro-avaliacao`)

- Endpoint `POST /avaliacao`, que recebe `descricao` (obrigatória, até 1000 caracteres) e `nota` (inteiro de 0 a 10).
- Urgência calculada pela nota e gravada com a avaliação: 0 a 3 é CRITICA, 4 a 6 é MEDIA e 7 a 10 é BAIXA. O cliente não informa a urgência.
- Respostas 201 com o identificador e a urgência, 400 citando todos os campos inválidos e 500 sem detalhe técnico. Entrada rejeitada não grava nada.
- Data de envio atribuída pelo sistema, com o dia no fuso de São Paulo, e limite de taxa no stage da API.
- Descrição e nota do aluno ficam fora dos logs em nível INFO.

### Aviso imediato de avaliação crítica (`add-notificacao-critica`)

- Função `notificar-critica`, acionada pelo DynamoDB Streams com filtro: só a criação de uma avaliação CRITICA dispara o aviso. Alteração e remoção não avisam.
- E-mail aos administradores com a descrição completa, a urgência e a data de envio.
- Falha ao publicar leva a novas tentativas do lote, sem afetar a avaliação já gravada. O lote que esgota as tentativas é guardado no bucket de relatórios, em `aws/lambda/`.

### Relatório semanal (`add-relatorio-semanal`)

- Geração automática toda segunda-feira às 08:00 (America/Sao_Paulo) pelo EventBridge Scheduler, cobrindo os 7 dias anteriores.
- Conteúdo: lista de avaliações com descrição, urgência e data de envio, quantidade por dia, quantidade por urgência e média das notas. Semana sem avaliações gera relatório com zeros.
- Relatório gravado em `relatorios/<dataReferencia>.txt` no bucket privado, com aviso aos administradores que traz a localização e o resumo, sem descrições nem notas individuais.
- Acionamento manual pela invocação da função, com `dataReferencia` opcional. Data inválida é rejeitada e a reexecução para a mesma data substitui o relatório anterior.

### Observabilidade e segurança (`add-observabilidade-seguranca`)

- Um alarme de falha por função, alimentado por filtro de log, que pega também as falhas tratadas (resposta 500, falha parcial no lote, execução do relatório com falha) e o tempo esgotado. Entrada rejeitada e ausência de tráfego não disparam alarme.
- Tópico de alarmes separado do tópico de administradores, com assinatura por e-mail e política que nega acesso sem TLS e só aceita publicação do CloudWatch da própria conta.
- Dashboard por ambiente com invocações, falhas, duração e restauração do SnapStart de cada função, mais o estado dos alarmes.
- Clientes do SDK aquecidos antes do snapshot do SnapStart, memória de 1024 MB nas três funções e retenção de logs de 30 dias em prod e 14 dias em dev e hml.
- `sam build` no CI de pull request, para reprovar o PR quando o template referenciar uma função sem código.

### Correções que entraram nos deploys de dev

- `aspectjrt` em escopo `runtime` nas três funções. Sem ele, o priming do SnapStart falhava e a versão da função não chegava a ser publicada.
- Permissão de `apigateway:TagResource` e `apigateway:UntagResource` nas roles de deploy, exigida para criar e atualizar o stage da API com tags.
- Permissão de `s3:PutObject` do destino de falha do Stream ampliada para o bucket inteiro, porque o Lambda recusa o prefixo `aws/lambda/*` ao criar o mapeamento.
