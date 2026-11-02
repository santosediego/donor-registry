# Roadmap

Uma parte por vez. Marque `[x]` quando o PR da parte for aprovado e mergeado na `dev`. O "pronto quando" é o critério que o Diego usa para aprovar.

## Trilha atual

- [ ] **0. Preparar o repositório** (sem IA): LICENSE (MIT), `CLAUDE.md`, `docs/`, branches `main` e `dev`, proteção das duas.
- [ ] **1. Criar o projeto:** esqueleto Spring Boot em `backend/` gerado pelo Spring Initializr.
  - Pronto quando: o build passa e a seção "Comandos" do `CLAUDE.md` está preenchida com comandos reais.
- [ ] **2. Configurar:** Liquibase, properties (principal, `test`, `dev`, `prod`), PostgreSQL local para o perfil `dev`, tratamento global de exceções (`StandardError`, `ResourceNotFoundException`, `ResourceExceptionHandler`), remoção do `spring-boot-h2console` do `pom.xml` (o H2 fica só nos testes, e o console web é superfície desnecessária num sistema com dados sensíveis).
  - Pronto quando: sobe em `dev` (PostgreSQL) e em `test` (H2), as duas rodando o Liquibase; um endpoint de exemplo devolve 404 no formato padrão; `.env` está no `.gitignore` e existe um `.env.example` sem valores reais (adiado da Parte 1).
- [ ] **3. Entidades:** `Donor`, `Address` e enums, com os changeSets do Liquibase.
  - Pronto quando: as tabelas são criadas pelo Liquibase (dev e test) e a aplicação sobe com `ddl-auto=validate`.
- [ ] **4. CRUD de doador:** DTOs, validações, busca paginada (por nome e CPF).
  - Pronto quando: o roteiro de Postman escrito pelo Diego passa de ponta a ponta.
- [ ] **5. Testes (AAA):** cenários escritos pelo Diego, implementados pela IA.
  - Pronto quando: cada cenário tem teste, e o Diego provou que cada teste falha quando o código é quebrado de propósito.

## Backlog (ordem a definir depois da Parte 5)

Autenticação (decidir JWT ou sessão), configurações da entidade e logo, CSV, relatório, backup, 2FA opcional, front-end (`frontweb/`, protótipos em `docs/design/`), distribuição com Docker. Detalhes e armadilhas conhecidas estão em `docs/PLANO.md`.
