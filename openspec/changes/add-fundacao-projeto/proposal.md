# Change: Fundação do projeto, da conta AWS e da entrega contínua

## Why

Antes de qualquer função existir, o projeto precisa de três coisas que todas as outras mudanças vão usar: a estrutura do código, os recursos de suporte na nuvem e um caminho automatizado e seguro do Git até a AWS. Montar isso primeiro garante que cada change seguinte já nasce com build, testes e deploy por ambiente, e cobre de saída dois requisitos do enunciado: componentes de suporte configurados e deploy automatizado dos componentes atualizáveis.

## What Changes

- Estrutura Maven multi-módulo em Java 25, preparada para três funções e um módulo de domínio compartilhado
- Template SAM base, parametrizado por ambiente (`dev`, `hml`, `prod`), com os recursos de suporte:
  - Tabela DynamoDB de avaliações, com Streams habilitado e criptografia em repouso
  - Tópico SNS de administradores, criptografado, com assinatura por e-mail
  - Bucket S3 privado para relatórios, criptografado e com Block Public Access
- Template de bootstrap da conta, aplicado uma única vez pelo Gabriel:
  - Provedor OIDC do GitHub
  - Uma role de deploy por ambiente, com confiança restrita ao repositório e ao ambiente do GitHub
- Pipelines do GitHub Actions:
  - CI em pull requests: build, testes, validação e lint do template
  - CD por branch: `develop` para dev, `release/*` para hml, `main` para prod com aprovação manual
- Ambientes do GitHub (`dev`, `hml`, `prod`) com as variáveis de deploy
- Arquivos de apoio: `.gitignore`, `.editorconfig`, `README.md` inicial

Fora do escopo deste change: as três funções Lambda, alarmes, dashboard e a documentação final. Cada um tem change próprio.

## Impact

- Specs novas: `fundacao-cloud`, `entrega-continua`
- Código afetado: nenhum existente (repositório novo)
- Ações manuais do Gabriel:
  - Criar o repositório público no GitHub
  - Aplicar o template de bootstrap na conta AWS uma vez
  - Confirmar a assinatura do e-mail do SNS em cada ambiente
  - Executar os comandos Git fornecidos
