# Roadmap

Uma parte por vez. Marque `[x]` quando o PR da parte for aprovado e mergeado na `dev`. O "pronto quando" é o critério que o Diego usa para aprovar.

## Trilha atual

- [x] **0. Preparar o repositório** (sem IA): LICENSE (MIT), `CLAUDE.md`, `docs/`, branches `main` e `dev`, proteção das duas.
- [x] **1. Criar o projeto:** esqueleto Spring Boot em `backend/` gerado pelo Spring Initializr.
  - Pronto quando: o build passa e a seção "Comandos" do `CLAUDE.md` está preenchida com comandos reais.
- [ ] **2. Configurar:** Liquibase, properties (principal, `test`, `dev`, `prod`) e `.env`.
  - Perfil `dev` com PostgreSQL local; URL, usuário e senha vêm de variáveis de ambiente (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), carregadas de `backend/.env` via `spring.config.import`. Perfil `test` com H2 em memória (`MODE=PostgreSQL`) e Liquibase ligado. Perfil `prod` só com variáveis de ambiente, sem valor padrão.
  - O console do H2 fica até o fim da Parte 4, ligado só no perfil `test`; depois sai do `pom.xml` (é superfície desnecessária num sistema com dados sensíveis).
  - Pronto quando: `./mvnw test` passa com o Liquibase rodando em H2; `APP_PROFILE=test ./mvnw spring-boot:run` sobe e o `/h2-console` mostra `databasechangelog` e `databasechangeloglock`; `APP_PROFILE=prod ./mvnw spring-boot:run` sem variáveis falha ao subir; o `dev` (PostgreSQL) sobe com o `.env` preenchido; `.env` está no `.gitignore` e existe `.env.example` sem valores reais.
- [ ] **3. Entidades:** `Donor`, `Address` e enums, com os changeSets do Liquibase.
  - Pronto quando: as tabelas são criadas pelo Liquibase (dev e test) e a aplicação sobe com `ddl-auto=validate`.
- [ ] **4. CRUD de doador:**
  - **4.1** Criar e buscar por id, com DTOs, validações e tratamento global de exceções (`StandardError`, `ResourceNotFoundException`, `ResourceExceptionHandler`). Pronto quando: o POST cria, o GET por id devolve o doador e um id inexistente devolve 404 no formato padrão.
  - **4.2** Listagem paginada com filtro por nome e CPF. Pronto quando: a busca pagina, ordena e filtra.
  - **4.3** Atualizar e excluir. Pronto quando: o roteiro de Postman escrito pelo Diego passa de ponta a ponta.
- [ ] **5. Testes (AAA):** cenários escritos pelo Diego, implementados pela IA.
  - Pronto quando: cada cenário tem teste, e o Diego provou que cada teste falha quando o código é quebrado de propósito.
- [ ] **6. CI:** GitHub Actions rodando build e testes em todo PR, como verificação obrigatória na proteção das branches.
  - Pronto quando: um PR com teste quebrado fica bloqueado para merge.
- [ ] **7. Versionamento:** SemVer automatizado com release-please (gera o PR de release, o `CHANGELOG.md`, a tag `vX.Y.Z` e o GitHub Release a partir dos commits). Decidir aqui o tipo de merge (squash ou merge commit) e o comportamento nas versões `0.x`.
  - Pronto quando: um PR de release é aberto, e ao ser mergeado cria a tag, o changelog e o release (primeiro alvo: `0.1.0`).

## Backlog (ordem a definir depois da Parte 5)

Autenticação (decidir JWT ou sessão), configurações da entidade e logo, CSV, relatório, backup, 2FA opcional, front-end (`frontweb/`, protótipos em `docs/design/`), distribuição com Docker. Detalhes e armadilhas conhecidas estão em `docs/PLANO.md`.
