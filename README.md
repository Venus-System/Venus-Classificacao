# Venus-Classificacao

API de classificação do **Venus-System**, a plataforma de análise de produtos cosméticos. Ela calcula a nota de 0 a 100 de um produto para o perfil de um usuário e explica o porquê da nota.

A Classificação não é dona de nenhum dado. Ela lê o catálogo e o perfil do usuário direto do banco que o Venus-CRUD usa, faz a conta e grava o resultado: a análise, a nota pessoal, as regras que bateram e a nota do produto. O app mobile, a web e o painel de administração chamam ela por HTTP.

## Sumário

- [Onde ela entra no sistema](#onde-ela-entra-no-sistema)
- [Stack](#stack)
- [O que a API faz](#o-que-a-api-faz)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Como uma requisição passa pelo código](#como-uma-requisição-passa-pelo-código)
- [Segurança](#segurança)
- [Como rodar](#como-rodar)
- [Documentação da API (Swagger)](#documentação-da-api-swagger)
- [Rotas](#rotas)
- [Testes](#testes)
- [CI/CD](#cicd)
- [Como contribuir](#como-contribuir)
- [Licença](#licença)

## Onde ela entra no sistema

```
 Venus-Mobile (Android)  ─┐
 Venus-Web (React)       ─┼──HTTP──>  Venus-Classificacao  ──JPA──>  PostgreSQL (schema venus)
 Painel admin            ─┘                    │                              ▲
                                               │                              │
              token do Firebase (app) ─────────┤              Venus-CRUD ──JPA─┘
              token do admin (do CRUD) ────────┘
```

| Repositório | Papel |
|---|---|
| **Venus-Classificacao** (este) | Motor que calcula a nota 0–100 do produto para cada perfil |
| Venus-CRUD | API central: CRUD de todo o schema `venus`, o fluxo dos scans e o login do admin |
| Venus-Banco | Schema PostgreSQL, functions, procedures e triggers. É a fonte da verdade do banco |
| Venus-AI-api / Venus-AI-Sdk | Agentes de IA (Python/FastAPI) |
| Venus-Mobile | App Android onde o usuário escaneia o produto |
| Venus-Web | Aplicação web: questionário, perfil, busca e produto |

Todos ficam na organização [Venus-System](https://github.com/Venus-System).

## Stack

| Tecnologia | Versão | Para quê |
|---|---|---|
| Java | 21 | Linguagem (com virtual threads ligadas) |
| Spring Boot | 3.3.4 | Web, Data JPA, Validation |
| Spring Security + OAuth2 Resource Server | do Boot | Validação dos tokens do Firebase e do admin |
| PostgreSQL | 16 | Banco relacional (schema `venus`, o mesmo do Venus-CRUD) |
| MapStruct | 1.5.5.Final | Conversão do resultado para DTO |
| Lombok | do Boot | Getters, setters e construtores das entities |
| springdoc-openapi | 2.6.0 | Swagger UI |
| Testcontainers | do Boot | Teste de integração com Postgres real |
| Maven Wrapper | — | Build (`./mvnw`) |

## O que a API faz

A API recebe o usuário e a versão do produto e devolve a nota, a faixa de recomendação, o nível de risco e os motivos. A conta tem três partes.

**1. Nota de qualidade (0 a 100)**
É a média de quatro notas do produto, cada uma com um peso: saúde (35), desempenho (20), ambiental (15) e ético (15). O modelo de score pode reforçar ou aliviar esses pesos pelas categorias dele. Essa nota não depende de quem está analisando.

**2. Compatibilidade com o perfil (0 a 100%)**
São 15 perguntas do perfil: tipo de pele, tendência a acne, rosácea, eczema, hiperpigmentação, melasma, tipo de couro cabeludo, gestação, amamentação, vegano, cruelty-free, sem parabeno, sem sulfato, sem silicone e outras preferências. Cada pergunta soma pontos pelas regras de compatibilidade que batem entre os ingredientes do produto e as etiquetas do usuário. Vegano e cruelty-free vêm da declaração da marca. Pergunta que o usuário não respondeu sai da conta.

**3. Nota final**
A qualidade vale até 35 pontos e o perfil até 65. Exemplo: qualidade 74 vira 25,9 pontos (35 × 74 ÷ 100), compatibilidade de 23,53% vira 15,3 pontos (65 × 23,53%), e a nota final é 41. Se nenhuma pergunta entrar na conta, a nota final é a própria nota de qualidade.

Depois da soma entram as travas:

| Situação | O que acontece com a nota |
|---|---|
| Regra de bloqueio (ex.: ingrediente proibido na gestação) | vira 0, e o produto fica contraindicado |
| Alergia crítica | vira 0, contraindicado |
| Alergia alta | no máximo 15, contraindicado |
| Alergia média | no máximo 49 |
| Alergia baixa | perde 15 pontos |

E a faixa sai da nota final: ideal a partir de 85, recomendado a partir de 70, aceitável a partir de 50, não recomendado a partir de 25 e contraindicado abaixo disso.

**O que é gravado**
Cada classificação grava uma linha nova em `analysis_results` e em `personalized_scores`, uma linha em `rule_evaluations` para cada regra que bateu, e atualiza a nota de qualidade da versão em `product_scores`. O histórico acumula: nada é sobrescrito, a não ser a nota do produto.

**Quando a API recusa**
Os 404 e os 422 trazem um `code` que diz o motivo sem precisar ler a mensagem:

| Status | `code` | Quando |
|---|---|---|
| 404 | `PROFILE_NOT_FOUND` | O usuário não tem perfil cadastrado |
| 404 | `VERSION_NOT_FOUND` | A versão do produto não existe |
| 404 | `SCORING_MODEL_NOT_FOUND` | O modelo de score pedido não existe |
| 404 | `NO_ACTIVE_SCORING_MODEL` | Não foi pedido modelo e nenhum está ativo |
| 422 | `VERSION_UNDER_REVIEW` | A versão está em revisão e não pode ser classificada |
| 422 | `NO_INGREDIENTS` | A versão não tem nenhum ingrediente cadastrado |

## Estrutura do projeto

```
Venus-Classificacao/
├── .github/
│   ├── workflows/                 # ci.yml, deploy-qa.yml, deploy-prod.yml
│   └── pull_request_template.md
├── src/
│   └── main/
│       ├── java/com/venus/classificacao/
│       │   ├── config/            # Segurança, Swagger, JPA, MapStruct, propriedades do .env
│       │   ├── controller/        # Rota da classificação
│       │   ├── dto/
│       │   │   ├── request/       # Corpo do POST
│       │   │   └── response/      # O que a API devolve
│       │   ├── entity/            # Entities JPA, enums e converters (as mesmas do Venus-CRUD)
│       │   ├── exception/         # Exceções, códigos de erro e o GlobalExceptionHandler
│       │   ├── mapper/            # Mapper MapStruct do resultado
│       │   ├── repository/        # Spring Data, um pacote por domínio
│       │   ├── security/          # Tokens, papéis e checagem de dono
│       │   └── service/           # O motor, um pacote por etapa
│       │       ├── profile/       # Carrega o perfil do usuário
│       │       ├── product/       # Carrega o produto e escolhe as regras do modelo
│       │       ├── quality/       # Nota de qualidade (strategy/ tem uma classe por nota)
│       │       ├── question/      # Uma classe por pergunta do perfil
│       │       ├── verdict/       # Soma, alergias, travas e faixa
│       │       ├── explanation/   # Motivos e resumo
│       │       └── recording/     # Grava o resultado no banco
│       └── resources/
│           └── application.yml    # Toda configuração vem de variável de ambiente
├── .env.example                   # Modelo das variáveis de ambiente
├── docker-compose.yml             # Postgres local
├── Dockerfile                     # Imagem da API (build em duas etapas)
└── pom.xml
```

## Como uma requisição passa pelo código

```
POST /api/classifications
 └─> ClassificationController       valida o corpo (@Valid) e confere o dono (@PreAuthorize)
      └─> ClassificationService     uma transação só
           ├─> ProfileSnapshotLoader         perfil, preferências, etiquetas e alergias
           ├─> ProductSnapshotLoader         versão, ingredientes, embalagem, selos e regras
           ├─> BucketWeightsLoader           pesos das notas de qualidade
           ├─> PersonalizedScoreCalculator   qualidade → perguntas → soma → alergias → travas
           ├─> ExplanationBuilder            motivos e resumo
           ├─> ClassificationRecorder        grava a análise, a nota pessoal, as regras e a nota do produto
           └─> ClassificationMapper          resultado → DTO
```

- O **controller** recebe a requisição, valida e chama o service. Ele não tem regra de negócio.
- Os **loaders** leem o banco e devolvem cópias dos dados. As contas nunca recebem entity nem consultam o banco.
- A **entity** nunca sai pela API. A resposta é sempre um DTO `record`.
- Os erros passam pelo `GlobalExceptionHandler` e sempre voltam no mesmo formato:

```json
{
  "timestamp": "2026-10-04T10:15:30-03:00",
  "status": 404,
  "error": "Not Found",
  "code": "PROFILE_NOT_FOUND",
  "message": "Perfil nao encontrado para o usuario com id 42",
  "path": "/api/classifications",
  "details": []
}
```

## Segurança

A API aceita os mesmos dois tokens do Venus-CRUD, no cabeçalho `Authorization: Bearer <token>`:

| Token | Quem usa | Como é obtido | Papel |
|---|---|---|---|
| Firebase | Usuário do app e da web | Login no Firebase, feito pelo próprio app | `USER` |
| JWT do admin | Painel de administração | `POST /api/auth/admin/login` do **Venus-CRUD**, com e-mail e senha | `ADMIN`, `MODERATOR` ou `ANALYST` |

A API identifica o token pelo emissor (`iss`). O do Firebase é validado com as chaves públicas do Google para o projeto em `FIREBASE_PROJECT_ID`. O do admin é conferido com o `ADMIN_JWT_SECRET` (HS256), que tem que ser **o mesmo do Venus-CRUD**. A Classificação só confere o token do admin: quem emite é o login do Venus-CRUD.

**Regra por rota**

| Rotas | Quem pode |
|---|---|
| Swagger | livre |
| `POST /api/classifications` | o dono do `userId`, com a conta ativa, ou `ADMIN` |

"Dono" quer dizer que o usuário do token do Firebase é o mesmo `userId` do corpo, com a conta `ACTIVE` ou `PENDING`. A checagem fica no `OwnershipGuard`, chamado pelo `@PreAuthorize` da rota. O admin com papel `ADMIN` passa por essa checagem; `MODERATOR` e `ANALYST` recebem 403.

## Como rodar

### Pré-requisitos

- JDK 21
- Acesso a um PostgreSQL com o schema `venus` criado pelo repositório **Venus-Banco**
- Docker, opcional: para o `docker-compose` e para a imagem da API

### 1. Clonar

```bash
git clone https://github.com/Venus-System/Venus-Classificacao.git
cd Venus-Classificacao
git checkout develop
```

### 2. Configurar as variáveis de ambiente

```bash
cp .env.example .env
```

Preencha o `.env`. O Spring **não lê o `.env` sozinho**: carregue as variáveis no terminal ou na configuração de execução da IDE (no IntelliJ, pelo plugin EnvFile ou em *Run Configuration → Environment variables*).

| Variável | Descrição |
|---|---|
| `SERVER_PORT` | Porta HTTP da API (padrão `8080`) |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | Endereço do PostgreSQL |
| `DB_USERNAME`, `DB_PASSWORD` | Usuário e senha do PostgreSQL |
| `ADMIN_JWT_SECRET` | Segredo do token do admin. Tem que ser **o mesmo do Venus-CRUD** e ter **no mínimo 32 bytes**, senão a API não sobe |
| `FIREBASE_PROJECT_ID` | Projeto do Firebase que emite o token do app |
| `SCORING_BASE_MODEL_ID` | Id do modelo de score base. Quando o modelo pedido não tem regra para um efeito, vale a regra do modelo base |

Para as credenciais do banco e o segredo do admin, fale com o time.

### 3. Banco de dados

O Hibernate roda com `ddl-auto: validate`: ele **confere** se as tabelas batem com as entities, mas **não cria nem altera nada**. Se faltar uma tabela ou coluna, a API não sobe.

O schema vem do repositório **Venus-Banco**. Toda mudança de tabela, coluna, trigger ou permissão é feita lá, pelo time de banco, e só depois a API muda. O usuário de `DB_USERNAME` precisa poder gravar em `analysis_results`, `personalized_scores`, `rule_evaluations` e `product_scores`.

A API abre no máximo 3 conexões com o banco, como o Venus-CRUD, para não esgotar as vagas do servidor, que são divididas entre as APIs.

> **Limitação conhecida:** a URL do datasource no `application.yml` usa `sslmode=require`. O Postgres do `docker-compose.yml` sobe sem SSL, então a API não conecta nele sem ajuste. Hoje o jeito que funciona é apontar para o banco compartilhado do time.

### 4. Subir a API

```bash
./mvnw spring-boot:run
```

No Windows (PowerShell ou cmd), use `mvnw.cmd spring-boot:run`.

A API sobe em `http://localhost:8080` (ou na porta de `SERVER_PORT`).

### Com Docker

```bash
docker build -t venus-classificacao .
docker run --env-file .env -p 8080:8080 venus-classificacao
```

A imagem é feita em duas etapas: compila com JDK 21 e roda com JRE 21 Alpine, com um usuário sem privilégios.

## Documentação da API (Swagger)

Com a API no ar:

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI (JSON):** `http://localhost:8080/v3/api-docs`

Para testar a rota no Swagger:

1. Pegue um token: o do Firebase no app, ou o do admin chamando `POST /api/auth/admin/login` no Venus-CRUD.
2. Clique em **Authorize** e cole o token.
3. A rota passa a enviar o cabeçalho `Authorization`.

A rota mostra no Swagger os erros possíveis (400, 401, 403, 404, 422…).

## Rotas

| Rota | Quem | O que faz |
|---|---|---|
| `POST /api/classifications` | dono ou `ADMIN` | Calcula, grava e devolve a nota do produto para o perfil do usuário |
| `GET /api/classifications/user/{userId}/product-version/{versionId}` | dono ou `ADMIN` | Devolve a última análise salva daquela versão |
| `GET /api/classifications/user/{userId}/product/{productId}` | dono ou `ADMIN` | Acha a versão atual do produto e devolve a última análise salva dela |

Corpo:

```json
{
  "userId": 42,
  "productVersionId": 118,
  "scoringModelId": 1
}
```

O `scoringModelId` é opcional: sem ele, a API usa o modelo ativo de maior id.

A resposta traz a nota final, a faixa (`recommendationLevel`), o risco (`riskLevel`), a compatibilidade com o perfil, quantos ingredientes a versão tem e quantos ainda não foram avaliados, o detalhamento das notas (`breakdown`), os motivos (`reasons`, com bloqueio e alergia sempre primeiro) e um resumo em texto (`summary`).

Nos dois `GET` o `scoringModelId` também é opcional. O 404 com `code` `ANALYSIS_NOT_FOUND` quer dizer que a pessoa
ainda não analisou aquela versão com aquele modelo: o app mostra a nota genérica do produto ou chama o `POST`. O `GET`
por produto olha só a versão atual, então uma análise de versão antiga não volta.

O `GET` devolve o que fica gravado: a nota, a faixa, o risco, a compatibilidade, as quatro notas, os motivos de regra
e o resumo. Os campos que só existem na hora do cálculo (`qualityScore`, `qualityPoints`, `profilePoints`,
`ingredientCount` e `unevaluatedIngredientCount`) vêm nulos.

Cada usuário tem um score pessoal por versão e modelo: o `POST` seguinte atualiza esse score e guarda a análise nova
no histórico.

## Testes

```bash
./mvnw verify
```

Precisa do Docker aberto: o teste de integração sobe um Postgres 16 com o `00_schema.sql` do Venus-Banco e uma seed
pequena (`src/test/resources/venus-banco/`). Sem Docker, ele é pulado.

| Tipo | Onde | O que cobre |
|---|---|---|
| Unitário | `service/**` | as quatro notas, os pesos, as perguntas, as travas, a cascata de regras e os motivos |
| Segurança | `ClassificationControllerSecurityTest`, `OwnershipGuardTest`, `PreAuthorizeCoverageTest` | 401, 403, o admin e o `@PreAuthorize` em toda rota |
| Integração | `ClassificationApiIntegrationTest` | o `POST` gravando, o segundo `POST` igual, os dois `GET` e os 404 e 422 |

O `01_users_email_password_hash.sql` do teste cobre duas colunas que já estão no banco e ainda não estão no `00_schema.sql` do Venus-Banco. Quando o Venus-Banco mudar o schema, copiar o `sql/00_schema.sql` de lá por cima do arquivo do teste e apagar o `01` se as colunas já estiverem nele.

## CI/CD

| Workflow | Quando roda | O que faz |
|---|---|---|
| `ci.yml` | PR para `develop` ou `main` | `./mvnw verify` |
| `deploy-qa.yml` | push em `develop` | Build, testes e imagem no GHCR com as tags `staging` e `develop-<sha>` |
| `deploy-prod.yml` | push em `main` | Build, testes e imagem no GHCR com as tags `latest` e `main-<sha>` |

A etapa de deploy dos dois ambientes ainda não tem host definido: hoje ela só publica a imagem.

## Como contribuir

1. Crie o branch a partir de `develop`: `feat/...`, `fix/...`, `refactor/...`, `docs/...` ou `ci/...`.
2. Commits no padrão de commits convencionais, em português e no gerúndio. Exemplo: `feat: adicionando o POST /api/classifications`.
3. Rode `./mvnw verify` antes de abrir o PR.
4. Abra o PR para `develop` e preencha o template (`.github/pull_request_template.md`).
5. Mudança de banco não é feita aqui: abra no **Venus-Banco** e descreva no PR o que a API passa a esperar.

Padrões do código:

- A resposta da API é sempre DTO `record`, nunca entity.
- A conversão é feita com MapStruct: não monte DTO na mão no service.
- Toda rota nova precisa de `@PreAuthorize`.
- Classe nova do motor entra no pacote da etapa dela, e pergunta nova do perfil é uma classe que implementa `ProfileQuestionStrategy`.
- Nada de chave, senha ou URL privada no código ou no `application.yml`: tudo vem do `.env`.

## Licença

Distribuído sob a licença MIT. Veja [LICENSE](LICENSE).
