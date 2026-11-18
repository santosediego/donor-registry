# Registro de Doadores (Donor Registry)

> A blood donor registry for donation days: register, search and look up donors through a REST API.
> Work in progress: there is no published version yet, and no front end, authentication or report.
> Built as an exercise in delegating development to an AI (Claude Code).

Sistema de registro de doadores de sangue para dias de coleta, com cadastro, busca e consulta de doadores. Este projeto está **em desenvolvimento** e ainda não tem versão publicada.

## Status

O estado de cada parte está em [`docs/ROADMAP.md`](docs/ROADMAP.md).

**Já existe** (`backend/`, uma API REST em Spring Boot):

- Cadastro de doador, com endereço opcional (`POST /donors`).
- Consulta por id (`GET /donors/{id}`).
- Listagem paginada com busca por nome, CPF e RG (`GET /donors`).
- Atualização completa (`PUT /donors/{id}`) e exclusão definitiva (`DELETE /donors/{id}`).
- Suíte de testes automatizados.

**Ainda não existe** (ver o ROADMAP): autenticação, front-end, relatório, exportação em CSV, backup, integração contínua (CI), versionamento automatizado e distribuição com Docker.

## Dados fictícios

> [!WARNING]
> Todos os dados de exemplo, de teste e de documentação deste repositório são **fictícios**.

## Stack

| Item | Tecnologia |
|------|------------|
| Java | 25 (LTS) |
| Spring Boot | 4.1.2 |
| Maven | via Maven Wrapper (`./mvnw`) |
| Persistência | Spring Data JPA (Hibernate) |
| Migrações de banco | Liquibase (YAML) |
| Banco em desenvolvimento | PostgreSQL 17 |
| Banco nos testes e na demonstração | H2 em memória |

## Como rodar localmente

**Pré-requisito:** JDK 25. O Maven não precisa estar instalado, pois o projeto usa o Maven Wrapper.

### Perfis do Spring

A variável de ambiente `APP_PROFILE` escolhe o perfil. Se ela não for definida, vale `dev`.

| Perfil | Banco | Dados de exemplo |
|--------|-------|------------------|
| `dev` (padrão) | PostgreSQL | Sim |
| `test` | H2 em memória | Sim |
| `prod` | PostgreSQL | Não |

### Modo demonstração (caminho mais curto)

Usa o perfil `test` (H2 em memória): não precisa de PostgreSQL nem de configuração. Os dados somem quando a aplicação para. O banco já vem com doadores de exemplo.

```bash
cd backend
APP_PROFILE=test ./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`.

### Modo desenvolvimento (PostgreSQL)

Usa o perfil `dev`. A aplicação exige:

- PostgreSQL 17;
- um banco já criado;
- um usuário com permissão para criar tabelas.

O Liquibase cria as tabelas e carrega os dados de exemplo na primeira execução.

1. Copie o modelo de variáveis e preencha com os dados do seu banco. O arquivo `.env` está no `.gitignore`; nunca o versione.

   ```bash
   cd backend
   cp .env.example .env
   ```

   | Variável | Para que serve |
   |----------|----------------|
   | `DB_URL` | URL JDBC, por exemplo `jdbc:postgresql://localhost:5432/donor_registry_dev` |
   | `DB_USERNAME` | Usuário do banco |
   | `DB_PASSWORD` | Senha do banco |

   Nenhuma das três variáveis tem valor padrão: se faltar alguma, a aplicação não sobe. As mesmas variáveis também podem ser definidas no ambiente, em vez do `.env`.

2. Inicie a API:

   ```bash
   ./mvnw spring-boot:run
   ```

### Exemplo de uso

Com a API no ar, crie um doador. O CPF precisa ter dígitos verificadores válidos; o do exemplo é um CPF de teste válido.

```bash
curl -i -X POST http://localhost:8080/donors \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Ada Exemplo",
    "cpf": "52998224725",
    "birthDate": "1990-05-20",
    "gender": "F",
    "phone": "11999990000",
    "bloodType": "O+"
  }'
```

A resposta é `201 Created`, com o doador no corpo e a URL do novo registro no cabeçalho `Location`. Para buscar:

```bash
curl 'http://localhost:8080/donors?search=ada'
```

Os campos e as regras de validação estão na seção 7 do [`docs/PLANO.md`](docs/PLANO.md).

## Como rodar os testes

```bash
cd backend
./mvnw test
```

Todos os testes devem passar. Eles usam o perfil `test` (H2 em memória), então não precisam de PostgreSQL.

## Documentação

- [`docs/PLANO.md`](docs/PLANO.md): visão geral, decisões de projeto, modelagem, regras da API e backlog.
- [`docs/ROADMAP.md`](docs/ROADMAP.md): as partes do projeto e o estado de cada uma.
- [`docs/design/`](docs/design/): protótipos de tela em HTML, referência visual para o futuro front-end (não são código de produção).

## Como este projeto foi feito

Este projeto é um exercício de **desenvolvimento delegado a uma IA**. O código foi gerado pelo Claude Code; eu escrevi os cenários de teste, revisei cada PR e fiz todos os merges. O trabalho é dividido em partes: uma branch por parte, um PR para cada, e testes com verificações propositais ("quebrar o código" para provar que o teste falha) em cada etapa.

## Licença

[MIT](LICENSE)
