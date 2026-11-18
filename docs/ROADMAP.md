# Roadmap

Uma parte por vez. Marque `[x]` quando o PR da parte for aprovado e mergeado na `dev`. O "pronto quando" é o critério que o Diego usa para aprovar.

## Trilha atual

- [x] **0. Preparar o repositório** (sem IA): LICENSE (MIT), `CLAUDE.md`, `docs/`, branches `main` e `dev`, proteção das duas.
- [x] **1. Criar o projeto:** esqueleto Spring Boot em `backend/` gerado pelo Spring Initializr.
  - Pronto quando: o build passa e a seção "Comandos" do `CLAUDE.md` está preenchida com comandos reais.
- [x] **2. Configurar:** Liquibase, properties (principal, `test`, `dev`, `prod`) e `.env`.
  - Perfil `dev` com PostgreSQL local; URL, usuário e senha vêm de variáveis de ambiente (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), carregadas de `backend/.env` via `spring.config.import`. Perfil `test` com H2 em memória (`MODE=PostgreSQL`) e Liquibase ligado. Perfil `prod` só com variáveis de ambiente, sem valor padrão.
  - O console do H2 fica até o fim da Parte 4, ligado só no perfil `test`; depois sai do `pom.xml` (é superfície desnecessária num sistema com dados sensíveis).
  - Pronto quando: `./mvnw test` passa com o Liquibase rodando em H2; `APP_PROFILE=test ./mvnw spring-boot:run` sobe e o `/h2-console` mostra `databasechangelog` e `databasechangeloglock`; `APP_PROFILE=prod ./mvnw spring-boot:run` sem variáveis falha ao subir; o `dev` (PostgreSQL) sobe com o `.env` preenchido; `.env` está no `.gitignore` e existe `.env.example` sem valores reais.
- [x] **3. Entidades:** `Donor`, `Address` e enums, com os changeSets do Liquibase.
  - [x] **3.1** `Donor`, enums e changeSet `001`. Pronto quando: `./mvnw test` passa com o `ddl-auto=validate` rodando contra a tabela `tb_donor` criada pelo Liquibase, e o `/h2-console` mostra `TB_DONOR` com a restrição única do `cpf`.
  - [x] **3.2** `Address` (1:1 opcional com `Donor`; `Address` é o lado dono, com `donor_id`) e changeSet `002`. Pronto quando: `./mvnw test` passa com o `ddl-auto=validate` contra as tabelas criadas pelo Liquibase; o `/h2-console` mostra `TB_ADDRESS` com `DONOR_ID` obrigatório e único e a chave estrangeira para `TB_DONOR` (`ON DELETE CASCADE`), `TB_DONOR` sem mudanças, e o `DATABASECHANGELOG` com 2 registros.
  - [x] **3.3** Dados de exemplo (changeSets `003-seed-donors` e `004-seed-addresses`, contexto `dev,test`, autor `santosediego`): 16 doadores fictícios (nomes de ficção científica, CPFs fictícios válidos, os 10 tipos sanguíneos, os 3 gêneros, campos opcionais nulos em alguns) e 10 endereços, deixando 6 doadores sem endereço. Sem `id` informado (o banco gera); o `donor_id` vem de subconsulta pelo CPF. `created_at` é `NOT NULL` e o Liquibase não passa pelo `@PrePersist`, então é informado com valor fixo em UTC; `updated_at` fica nulo. Pronto quando: `./mvnw test` passa com os dados carregados; `APP_PROFILE=test ./mvnw spring-boot:run` sobe e o `/h2-console` mostra `TB_DONOR` com 16 linhas, `TB_ADDRESS` com 10 e 6 doadores sem endereço; com `APP_PROFILE=test` e `SPRING_LIQUIBASE_CONTEXTS=prod` as duas tabelas ficam vazias e o `DATABASECHANGELOG` tem só os registros 001 e 002; o `dev` (PostgreSQL) sobe com os dados, verificado pelo Diego.
- [ ] **4. CRUD de doador:**
  - [x] **4.1** Criar e buscar por id, com DTOs, validações e tratamento global de exceções (`StandardError`, `ResourceNotFoundException`, `ResourceExceptionHandler`). Conferir também o `createdAt` contra o relógio. Pronto quando: o POST cria, o GET por id devolve o doador e um id inexistente devolve 404 no formato padrão.
  - [x] **4.2** Listagem paginada com busca por nome, CPF e RG (`GET /donors`). Pronto quando: a busca pagina, ordena e filtra; a busca usa `LEFT JOIN` com projeção direta no DTO (não `LEFT JOIN FETCH`), então doador sem endereço aparece nos resultados, sem consultas em excesso (N+1): uma página gera 2 consultas (dados e contagem).
  - **4.3** Atualizar e excluir (`PUT /donors/{id}` com substituição completa e `DELETE /donors/{id}` definitivo). Decisões em `docs/PLANO.md`, seção 7. Pronto quando: o PUT e o DELETE funcionam nos dois bancos; o CPF próprio não gera 409; os três casos de endereço (criar, alterar no lugar, remover) funcionam sem violar a unicidade de `donor_id`; `updatedAt` reflete alteração de doador ou de endereço; a exclusão devolve 204 e depois 404, sem deixar endereço órfão; e o roteiro de Postman escrito pelo Diego passa de ponta a ponta.
- [ ] **5. Testes (AAA):** cenários escritos pelo Diego, implementados pela IA. Dividida em três níveis.
  - **5.1 Unitários puros** (sem Spring, sem banco, sem mocks): `DonorSearchQueries.likePattern` e `rgPattern`; códigos dos enums (`fromId`) e seus casos de erro; `ACCENTED` e `PLAIN` (em `DonorSearchQueries`) têm o mesmo tamanho, senão o `TRANSLATE` remove caracteres sem avisar.
  - **5.2 API** (`@SpringBootTest` + MockMvc, perfil `test`, H2). Dividida em três rodadas, cada uma com o seu PR:
    - [x] **5.2a: `POST /donors` e `GET /donors/{id}`**, mais a infraestrutura de teste de API (reaproveitada nas rodadas seguintes). Candidatos:
      - `address` igual a `{}` devolve 400 listando os seis campos obrigatórios; `address` nulo passa.
      - Validação do nome nos limites: 1 caractere recusa, 2 aceita, 120 aceita, 121 recusa; espaços nas pontas e só espaços (**comportamento atual a descobrir**).
      - `birthDate` inválida (ex.: `2030-13-45`) cai no tratamento de corpo inválido (400).
      - `birthDate` no futuro devolve 400 de validação.
      - `Instant` da resposta do POST vem com nanossegundos e o do GET com microssegundos (considerar truncar ao gravar).
    - **5.2b: `PUT /donors/{id}` e `DELETE /donors/{id}`.** Candidatos:
      - Atualizar sem mudar o CPF não devolve 409; CPF de outro doador devolve 409.
      - Endereço: criar, alterar e remover pelo PUT; campo opcional omitido no PUT vira nulo, sem 500.
      - Excluir doador inexistente devolve 404; excluir doador apaga o endereço; alterar só o endereço atualiza `updatedAt`.
      - `PUT` sem `bloodType` apaga o tipo (PUT é substituição completa).
      - `PUT` idêntico com peso `70` e `70.0` não muda `updatedAt`; idem com `complement` `""` contra nulo.
      - `Instant` da resposta do PUT vem com nanossegundos e o do GET com microssegundos.
      - No PUT, o corpo pode trazer `id`, `createdAt` e `updatedAt`: a API os ignora.
      - PUT que muda o CPF para um CPF livre grava o novo e libera o antigo.
      - Fallback da restrição única do banco devolve 409 quando a checagem prévia de CPF não enxerga o duplicado (POST e PUT).
      - `tb_address` não tem `NOT NULL` nas colunas obrigatórias do endereço: a validação do DTO é a única trava (avaliar `NOT NULL` no banco).
      - Silenciar os avisos do Mockito/agente dinâmico do Java 25 (configurar o agente no Maven).
    - **5.2c: `GET /donors` (listagem, busca, paginação e ordenação).** Candidatos:
      - Busca por RG ignora pontuação (`00000.199` acha `MG-00.000.199`; `.` não acha ninguém).
  - **5.3 PostgreSQL com Testcontainers** (só se a busca e a ordenação precisarem): busca sem acento só se prova com banco.
  - Pronto quando: cada cenário tem teste, e o Diego provou que cada teste falha quando o código é quebrado de propósito.
- [ ] **6. CI:** GitHub Actions rodando build e testes em todo PR, como verificação obrigatória na proteção das branches.
  - Pronto quando: um PR com teste quebrado fica bloqueado para merge.
- [ ] **7. Versionamento:** SemVer automatizado com release-please (gera o PR de release, o `CHANGELOG.md`, a tag `vX.Y.Z` e o GitHub Release a partir dos commits). Decidir aqui o tipo de merge (squash ou merge commit) e o comportamento nas versões `0.x`.
  - Pronto quando: um PR de release é aberto, e ao ser mergeado cria a tag, o changelog e o release (primeiro alvo: `0.1.0`).

## Backlog (ordem a definir depois da Parte 5)

Autenticação (decidir JWT ou sessão), configurações da entidade e logo, CSV, relatório, backup, 2FA opcional, front-end (`frontweb/`, protótipos em `docs/design/`), distribuição com Docker. Endpoint de relatório com endereço completo e "selecionar todos os resultados da busca" (tela de seleção). Detalhes e armadilhas conhecidas estão em `docs/PLANO.md`. Primeiro uso protegido por código de configuração (regras e testes em docs/PLANO.md).

Extrair um repositório template (base Spring Boot) depois da Parte 7.
