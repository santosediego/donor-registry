# Registro de Doadores (Donor Registry)

Sistema web para uma ONG cadastrar doadores (dados pessoais e de saúde: CPF, tipo sanguíneo), buscar, exportar CSV, gerar relatório e fazer backup. Roda local ou hospedado. Repositório **público**, feito do zero.

O autor (Diego) usa este projeto para **aprender a delegar desenvolvimento à IA**. Ele sabe Java/Spring, é testador manual (QA) e escreve pouco código de teste automatizado. Ele revisa e faz o merge de tudo.

## Como conversar com o Diego

- **Responda e explique sempre em português do Brasil.** Código, nomes e commits seguem a seção "Idioma".
- Ao usar um conceito ou ferramenta nova, explique em 2 a 4 linhas **o que é e por que está sendo feito assim**. Diego quer entender para reaplicar sozinho.
- Se o pedido ou o plano parecer errado, ambíguo ou contraditório, **diga antes de implementar**. Diego prefere ser contestado a ser agradado.

## Onde está o quê

- `docs/ROADMAP.md`: as partes do projeto e o estado de cada uma. O que fazer agora vem sempre no prompt (uma parte por vez).
- `docs/PLANO.md`: visão geral, decisões e backlog. É referência, não lista de tarefas. Leia só a seção relevante. **Se conflitar com este arquivo, vale este arquivo.** Ele usa nomes em português (`Doador`, `Endereco`), que aqui são `Donor`, `Address`, e cita MapStruct, que não usamos.
- `docs/design/*.dc.html`: protótipos das telas, referência de visual e textos (só quando chegarmos ao front). Não são código de produção.
- `backend/` (Spring Boot) e `frontweb/` (React, futuro).

## Como trabalhamos

- **Uma parte por vez.** Faça só o que o prompt pede, sem antecipar o que vem depois (nada de segurança, CSV, backup ou front "já que está aqui"). Terminou: abra o PR e pare.
- **Antes de codar, apresente um plano curto** (arquivos que vai criar ou alterar, e por quê) e espere o ok do Diego.
- **Commits pequenos**, um propósito cada, Conventional Commits em inglês com escopo: `feat(donor): add Donor entity`. Mantenha o trailer `Co-Authored-By`.
- **Não misture** atualização de dependência com mudança de funcionalidade.
- **Dependência nova: pergunte antes.** Só licença permissiva (MIT, Apache 2.0, BSD). Nada CC BY-NC, GPL, nem lista ou dicionário de terceiros embutido. Não usamos Lombok nem MapStruct.
- **Testes:** padrão AAA (Arrange, Act, Assert), nome que descreve o comportamento. Entram na parte dedicada a eles, depois do CRUD; até lá, o critério é compilar e subir sem erro.
- **Descrição do PR em português**, com: o que mudou, por quê, alternativa descartada (se houve), como testar e o que ficou de fora de propósito.

## Git

- `main` = produção; `dev` = integração. As duas só recebem código por PR.
- Branch por parte, saindo da `dev`: `ia/part-NN-short-description`. PR de volta para a `dev`.
- **Nunca** faça push direto em `main` ou `dev`, force push, nem merge de PR. Quem faz merge é o Diego.

## Idioma

- **Inglês:** código, entidades, campos, pacotes, branches e commits (`Donor`, `birthDate`). `cpf` e `rg` ficam como estão (termos brasileiros).
- **Português do Brasil:** tudo que o usuário final lê (mensagens de validação e de erro da API, textos da tela), descrição dos PRs e a pasta `docs/`.
- Arquivos em UTF-8.

## Convenções do backend (estilo do autor)

- Java LTS, Spring Boot, JPA, Liquibase (YAML), PostgreSQL. Versões ficam no `pom.xml`; não mude sem pedir. Pacote base `com.santosediego.donorregistry`.
- **Pacotes por camada:** `config`, `dto`, `entities` (com `entities.enums`), `repositories`, `resources`, `resources.exceptions`, `services`, `services.exceptions`, `services.validation`.
- **Controladores se chamam `Resource`** (`DonorResource`), não `Controller`. Serviços: `DonorService`. Repositórios: `DonorRepository`.
- **Sem Lombok e sem MapStruct:** construtores, getters e setters escritos à mão. O DTO tem um construtor que recebe a entidade (`new DonorDTO(entity)`), e o service tem um método `copyDtoToEntity`.
- **Injeção por construtor** (não use `@Autowired` em atributo). Entidade nunca sai direto na API: use DTO.
- Listagens: `Page<DTO> findAllPaged(Pageable, filtros)`; o `Resource` recebe `page`, `size`, `direction`, `orderBy`.
- **Erros:** exceções de serviço (`ResourceNotFoundException`, `DatabaseException`, ...) tratadas num `@ControllerAdvice` (`ResourceExceptionHandler`) que devolve `StandardError`; validação de campos devolve `ValidationError` com 422.
- **Enums:** código curto e descrição em português (como `Role`: campos `id` e `description`), persistidos como texto, nunca ordinal.
- **Properties:** `application.properties` + perfis `test` (H2 em memória, `MODE=PostgreSQL`), `dev` e `prod` (PostgreSQL). O perfil padrão é `dev`; os testes ativam `test` com `@ActiveProfiles("test")`. **Sem valor padrão para segredo ou senha** em nenhum perfil: se faltar variável de ambiente, a aplicação falha ao subir. `spring.jpa.open-in-view=false`.
- **Banco:** só Liquibase, também nos testes (o perfil `test` não desliga o Liquibase). Arquivos numerados (`001-create-donor-table.yaml`), incluídos no `db.changelog-master.yaml`; dados de exemplo em changeSet com contexto `dev`/`test`, nunca em `prod`. **Nunca edite um changeSet já aplicado**; crie outro. Schema `public` padrão. Sem `columnDefinition` específico de banco.
- **Frontend (quando chegar):** React + TypeScript + Vite em `frontweb/`. **Use `yarn`, nunca `npm`.**

## Segurança e privacidade

- Este repo é público e o sistema guarda dados sensíveis. **Nenhum dado real** no código, testes, exemplos ou histórico: nada de nome da ONG, logo, telefone ou CPF de pessoa real. Dados de exemplo são fictícios.
- Nenhum segredo no repositório. `.env` no `.gitignore`; deixe um `.env.example` sem valores reais.
- Não escreva CPF, senha ou token em log.

## Comandos

> Preencher na Parte 1, depois de gerar o projeto. Não invente comandos antes disso.

- Testes do backend: _a preencher_
- Subir o backend em dev: _a preencher_
- Build: _a preencher_

## Antes de abrir o PR

1. Build ok e o projeto sobe (rode e confira, não suponha).
2. Nenhum segredo ou dado real no diff.
3. Só o escopo da parte; nada de "já aproveitei e".
4. Descrição do PR completa.
