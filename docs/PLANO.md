# Registro de Doadores: plano de desenvolvimento

Documento de trabalho para o desenvolvimento do projeto, pensado para ser usado como briefing ao delegar código à IA. Cada parte tem objetivo, o que muda e como saber que ficou certo.

## 1. Contexto

- O Registro de Doadores é uma releitura, atualização e melhoria de um projeto antigo, feito pelo autor para uma ONG. O objetivo deste repositório é **aprender e praticar a delegação do desenvolvimento para a IA** (Claude e Claude Code), em vez de desenvolver sozinho.
- O sistema original foi pensado para rodar localmente na máquina do cliente, e o CSV existia para levar dados de uma instalação para outra. O projeto novo continua podendo rodar assim, mas também **precisa rodar hospedado** (servidor ou nuvem). Não há suporte a sistemas já descontinuados (Windows 7, por exemplo): quem estiver neles hospeda o sistema em outro lugar.
- **Stack do projeto novo:** Spring Boot 4.1.2 (gerado pelo Spring Initializr; `groupId` `com.santosediego`, `artifactId` `donor-registry`), Java 25 (LTS), JPA/Hibernate, Liquibase, PostgreSQL (uso real; H2 em memória só nos testes), React com Vite e TypeScript no front (`frontweb/`).
- **Repositório novo, público, começando do zero.** O código antigo não é usado. As regras de negócio e os problemas que o projeto antigo teve estão resumidos na seção 5, e cada problema vira um teste no projeto novo.

## 2. Decisões já tomadas

| Tema | Decisão |
|------|---------|
| Tipo de aplicação | Roda local **ou hospedado**, o mesmo sistema nos dois casos (nenhuma tela ou texto preso a "este computador") |
| Login | Mantido, com **um único administrador** (sem CRUD de usuários nem papéis). JWT ou sessão: decidir na etapa de autenticação |
| Senha padrão | Não existe: o primeiro uso pede a senha do administrador |
| Recuperação | Código de recuperação (formato a decidir na etapa de autenticação), mostrado uma única vez; se perder também o código, comando de **redefinição do administrador** rodado no terminal do servidor |
| Contato, nome da entidade e logo | Viram **configuração** (tabela e tela), saem do código |
| Esquema do banco | **Liquibase** (YAML), no lugar do `create.sql` gerado à mão. Antes da primeira release os changeSets já aplicados podem ser editados e o banco de dev recriado; depois da primeira release, nunca se edita, cria-se um novo |
| Exportação | **CSV e backup**, com papéis diferentes (ver seção 6) |
| Distribuição | **Docker** (`compose` com banco, back e front) e um README de "como rodar" são o essencial. O **script de instalação** é opcional, fica para o fim |
| Layout | Protótipos novos em `docs/design/` (lista, cadastro, seleção, relatório, login, primeiro uso, recuperação e configurações) |
| Banco | **PostgreSQL** para uso real (local ou hospedado). **H2 em memória** só nos testes automatizados e na demonstração (os dados não persistem) |
| Ferramentas | Java LTS (25), Spring Boot na versão estável mais recente (4.1.x), Node LTS no front. Tudo atual e mantido; sem versões descontinuadas |
| Idioma | Conversa e documentação em português (pt-BR). **Código, branches e commits em inglês** (nomes de classe, tabela e coluna incluídos) |
| Versionamento | SemVer automatizado com release-please a partir de Conventional Commits (`feat` = minor, `fix` = patch; `docs`, `test`, `refactor` e `chore` não mudam a versão) |
| Testes | Padrão AAA (*arrange, act, assert*). O autor escreve os cenários, a IA implementa, e o autor prova que cada teste falha quando o código é quebrado de propósito |

## 3. Como delegar à IA

Combinados entre o autor e a IA:

1. **Uma parte por vez.** Fazer só a parte pedida, sem antecipar as seguintes. Ao terminar, abrir o PR e parar.
2. **Prompt autocontido.** A IA que escreve o código (Claude Code) não vê as conversas nem os projetos antigos do autor. Tudo que ela precisa está no prompt, no `CLAUDE.md`, no `docs/ROADMAP.md` e neste plano.
3. **Plano antes do código.** A IA apresenta o plano (classes, decisões, dúvidas) e espera a aprovação do autor antes de escrever.
4. **Testes automatizados na Parte 5.** O autor escreve os cenários em linguagem de QA, a IA implementa em AAA, e o autor prova que cada teste falha quando o código é quebrado. Até lá, o autor valida cada parte à mão (Postman, banco, logs).
5. **Commits pequenos**, com um propósito cada. Se a IA errar, faz-se um commit novo (sem `--amend` nem *force push*).
6. **Convenções desde o início:** monorepo (`backend/`, `frontweb/`), perfis de `properties` (test, dev, prod), Liquibase, sem segredo no repositório.
7. **Revisão pelo autor.** O autor lê cada diff como se fosse PR de outra pessoa e pergunta "por que assim?" onde não entender; a IA explica antes de seguir.
8. Não misturar atualização de dependências com mudança de funcionalidade.
9. **Fluxo de git:**
   - `main`: produção, protegida (só entra por *pull request*).
   - `dev`: integração e testes exploratórios. Roda com o perfil `dev` e um banco real, separado do de produção. Também protegida.
   - **Branches curtas por parte** saem da `dev` e voltam para ela por PR: `ia/part-NN-descricao` (trabalho da IA) e `diego/...` (trabalho do autor). Apagadas após o merge.
   - Quando a `dev` estiver validada nos testes exploratórios, PR da `dev` para a `main`.
   - Quem **aprova e faz o merge é o autor** ("Create a merge commit"), depois de ler o diff, fazer a própria validação e ver os testes passando (GitHub Actions, a partir da Parte 6).
   - **Título do PR em Conventional Commits, em inglês** (`feat(donor): ...`); o número da parte vai na descrição.
   - Commits da IA levam `Co-Authored-By`, o que deixa claro no repositório público o que foi feito com ela.

## 4. Trilha e backlog

A ordem, o status e o critério de "pronto" de cada parte ficam em `docs/ROADMAP.md`. Aqui ficam os detalhes que não cabem lá.

### Trilha de fundação (resumo)

0. Preparar o repositório. 1. Esqueleto Spring Boot. 2. Configuração (Liquibase, perfis, `.env`). 3. Modelo: `Donor` (3.1), `Address` (3.2), dados de exemplo (3.3). 4. CRUD de doador: criar e buscar por id (4.1), listagem paginada com filtro (4.2), atualizar e excluir (4.3). 5. Testes AAA. 6. CI (GitHub Actions). 7. Versionamento (release-please).

### Parte 5: testes (como fazer)

- O autor escreve os cenários (visão de QA): cadastrar, buscar, paginar, editar, excluir, e os casos de erro. Cada item da seção 5 (armadilhas) é candidato a cenário.
- A IA implementa cada cenário em AAA (service com Mockito, repositório, MockMvc e integração, conforme fizer sentido).
- O autor quebra o código de propósito e confere que o teste correspondente falha. Teste que nunca falha não prova nada.

## Backlog detalhado

A ordem do backlog é definida depois da Parte 5. Cada tema abaixo vira uma ou mais partes no ROADMAP quando chegar a vez.

### Autenticação (proposta com JWT; JWT ou sessão ainda a decidir)
- **Autenticação com JWT:** login com usuário e senha, token assinado e validado por um filtro do Spring Security. Sem OAuth2 e sem `client-id`/`client-secret`. Algoritmo de assinatura e duração do token definidos nesta etapa.
- **Primeiro uso:** criar o administrador (usuário e senha escolhidos) **ou** restaurar um backup.
- **Segredo do JWT gerado na primeira execução**, guardado em arquivo local (fora do repositório e fora do backup).
- **Código de recuperação** gerado no primeiro uso, mostrado uma vez e guardado só como hash.
- Tela de redefinir senha pelo código e "Alterar senha" nas configurações.
- **Código de recuperação: formato a decidir quando chegarmos nesta etapa** (ver "Decisão adiada" abaixo). Vale em qualquer formato: sorteio com `SecureRandom`, normalização antes do hash (remover acento, minúsculas, descartar o que não for letra ou número, aceitando hífen, espaço ou tudo junto), guardar só o hash (bcrypt/argon2), erro genérico ("código inválido") e limite de tentativas.
- **Caminhos de recuperação:**
  1. Lembra a senha: Configurações > Alterar senha.
  2. Esqueceu a senha, tem o código: tela "Recuperar senha". Se o 2FA estiver ligado, o código também o desliga (é a recuperação do 2FA, não há segundo mecanismo). Depois de usado, o código é substituído por um novo, mostrado uma vez.
  3. Perdeu a senha **e** o código: comando de **redefinição do administrador**, rodado no terminal por quem administra o servidor (`redefinir-admin`, ou `docker exec ...` no Docker). Define nova senha (pedida no prompt, **nunca como argumento** do comando), desliga o 2FA e gera novo código; **os doadores e as configurações não são apagados**. Regras: **nunca** existe como endpoint HTTP nem como botão no sistema; registra o evento no log; tem teste automatizado (senha trocada, 2FA desligado, dados intactos). Não enfraquece a segurança: quem tem acesso ao servidor já tem acesso ao banco e aos segredos.
  - Restaurar um backup não troca a senha: o backup restaura o que estava lá, e a redefinição acima cobre o caso de senha perdida.
- **Primeiro uso protegido:** a tela de criar administrador fica aberta a quem chegar primeiro. Em instalação local o risco é baixo; **hospedado, não**. A inicialização gera um **token de configuração** de uso único, mostrado no console/log (ou passado por variável de ambiente) e exigido nessa tela. Depois que o administrador existe, as rotas de configuração inicial deixam de responder.
- **Limite de tentativas** no login e no código de recuperação (atraso progressivo ou bloqueio temporário). **HTTPS** obrigatório quando hospedado (proxy reverso).
- Sem valor padrão para segredo em nenhum perfil: se faltar, a aplicação falha ao subir.

### Segundo fator de autenticação (TOTP, opcional)

Opcional para quem usa: **desligado por padrão**, ativado em Configurações. Só depois de o resto estar de pé.

- **Padrão:** TOTP (RFC 6238), o código de 6 dígitos de qualquer app autenticador. Sem SMS nem e-mail, sem serviço externo, funciona offline.
- **Biblioteca:** usar uma biblioteca de TOTP mantida e de licença permissiva (Apache 2.0 ou MIT), em vez de escrever o algoritmo; mais uma lib pequena para o QR code. Janela de tolerância de 30 s para cada lado, por causa do relógio.
- **Ativar:** gerar o segredo, mostrar QR e texto, e só ligar depois de o usuário digitar um código válido.
- **Segredo cifrado no banco** (não pode ser hash, o servidor precisa dele), com chave gerada na instalação e **incluído no backup** (que já é cifrado), para a restauração não trancar o administrador.
- **Login:** senha primeiro, depois o código; limite de tentativas também no segundo passo.
- **Perdeu o celular:** código de recuperação ou comando de redefinição do administrador; ambos desligam o 2FA. Como o código passa a valer pelos dois fatores, precisa ser tratado como a chave mestra: aparece uma vez, hash no banco, limite de tentativas.
- **Reaproveitamento (também em projeto particular):** manter autenticação, 2FA, código de recuperação, backup e configurações num pacote próprio, sem depender das classes de doador (`Donor`, `Address`...). Assim dá para copiar o conjunto para outro projeto. Basta separar por pacote; um módulo Maven só se a necessidade aparecer.


### Configurações da entidade
- Tabela de configurações (nome da entidade, contato, logo), API protegida e tela.
- Relatório lê desses dados. Nenhum logo, telefone ou nome real no código-fonte.

### CSV
- Exportador escreve num `OutputStream` (nada de arquivo em `export/`).
- Importador lê um `InputStream` com **OpenCSV** (sem `split(";")` com índices) e devolve a lista de DTOs ou os erros por linha.
- UTF-8 explícito, valores entre aspas, nome de arquivo nunca usado como caminho.
- Importação dentro de **uma transação** (tudo ou nada), chamada por um método público de outro bean.
- O CSV exportado precisa ser reimportável, inclusive com campos vazios.

### Backup
- Arquivo **criptografado** (AES) com senha escolhida pelo usuário na hora de gerar; a senha do banco não protege o arquivo.
- Conteúdo: doadores, usuário (senha em hash), configurações, logo e **versão do esquema**.
- Restaurar: pede a senha do backup; recusa backup de versão mais nova que a da aplicação; migra versões mais antigas com o Liquibase.
- Teste automatizado: gerar backup, restaurar num banco vazio e conferir totais e conteúdo.
- Lembrete na tela ("último backup há N dias") e aviso para guardar o arquivo em local seguro.

### Front-end (`frontweb/`)
- React com Vite e TypeScript, aplicando o layout dos protótipos (seção 9).
- Máscara de campos com biblioteca mantida (ou implementação própria).
- Interceptor: tratar erro **sem** `response` (back fora do ar).
- Login: enviar o corpo codificado corretamente (senha com `&`, `+` ou `%` precisa autenticar).
- Redirecionamento de 401/403 decidido pelo roteador.
- **Fonte única para as opções** (gênero, estado civil, grupo sanguíneo): o back expõe as opções e o front só consome.

### Distribuição
- **Docker (essencial):** `compose` com PostgreSQL, back e front. O front é construído em etapas (build do Vite e servido por um servidor web) e as chamadas à API passam pelo mesmo endereço (`/api`), o que dispensa configurar o endereço do back no front e evita problemas de CORS. As variáveis de ambiente ligam as partes (banco ↔ back). `docker compose up` sobe tudo do zero.
- **README de "como rodar":** uso com Docker e, para quem prefere, subir cada parte à mão (banco, back, front) com as variáveis de ambiente.
- **Opcional, só se sobrar vontade: instalação em um comando** (ideia do estilo Jellyfin: a pessoa não baixa nem instala nada à mão). Como o banco de uso é o PostgreSQL, o script precisa instalar e configurar o banco. O script:
  - **Linux:** `curl -fsSL <url>/install.sh | sudo bash`. Baixa a última *release* do GitHub, confere o **checksum**, instala o Java (ou usa um runtime embutido via `jpackage`/`jlink`), cria um usuário de serviço sem privilégios, grava os segredos gerados na hora, registra um serviço **systemd** e abre o endereço local. Também precisa de `uninstall` e de atualização que preserve os dados.
  - **Windows:** `irm <url>/install.ps1 | iex` (PowerShell) ou um instalador `.exe`/`.msi` gerado na própria *release*. Mesmo roteiro, registrando um serviço do Windows. Versões descontinuadas do Windows não são alvo.
  - **Cuidados:** o script é publicado no repositório (qualquer um lê antes de rodar), nunca tem senha dentro, não usa `Todos:F`, é idempotente (rodar duas vezes não quebra) e registra o que fez. O `curl | bash` é prático, mas pede confiança: oferecer também o download manual com checksum.
  - **Lançador** que espera a porta responder (em vez de `TIMEOUT` fixo) e um único jeito de iniciar (serviço **ou** lançador, não os dois).


## 5. Armadilhas conhecidas e testes que as cobrem

Vieram de problemas que o projeto antigo teve. No projeto novo, cada uma vira um teste desde o início.

| # | Armadilha | Teste |
|---|-----------|-------|
| 1 | Importação sem atomicidade (transação não aplicada por chamada interna ou método privado) | Importar CSV com erro na linha 5 e conferir que nenhuma linha foi gravada |
| 2 | Excluir id inexistente cair numa exceção genérica | Excluir id inexistente deve retornar 404 |
| 3 | Estado de uma requisição guardado em atributo de serviço singleton | Duas exportações simultâneas não podem se misturar |
| 4 | CSVs com CPFs ficarem gravados em disco | Após exportar, nenhum arquivo deve sobrar em disco |
| 5 | Nome de arquivo enviado pelo usuário usado como caminho | Upload com nome `../x.csv` não pode escrever fora da pasta |
| 6 | Valor vazio convertido para número na importação | Exportar com peso nulo e reimportar |
| 7 | CSV sem aspas e charset padrão | Complemento com `;` e nome com acento sobrevivem à ida e volta |
| 8 | Busca por nome normalizando só parte dos acentos | Buscar "joao" deve achar "João"; "conceicao" deve achar "Conceição" |
| 9 | Busca por CPF falhar sem a máscara | Buscar `36238916060` deve achar `362.389.160-60` |
| 10 | Consulta com `JOIN` sem `FETCH`: N+1 nas listagens e doador sem endereço sumindo da busca | Listar doadores sem consultas em excesso; doador sem endereço (o endereço é opcional) aparece na busca |
| 11 | Campos opcionais nulos causando `NullPointerException` | Cadastrar sem esses campos não pode retornar 500 |
| 12 | Valor de enum inválido virar erro 500 | Deve retornar 400 com mensagem |
| 13 | CPF validado só por tamanho | `aaaaaaaaaaaaaa` deve ser rejeitado; usar `@CPF` |
| 14 | Mensagem de validação divergir da regra | Mensagem e regra alinhadas |
| 15 | Front ler `error.response.status` sem checar se há `response` | Back fora do ar mostra erro de rede, não `TypeError` |
| 16 | Login montar o corpo por concatenação | Senha com `&`, `+` e `%` autentica |

## 6. Backup e CSV: papéis diferentes

| | CSV | Backup |
|---|-----|--------|
| Função | **Mesclar**: importa só quem ainda não existe | **Restaurar**: substitui tudo |
| Conteúdo | Só doadores | Doadores, usuário, configurações, logo |
| Proteção | Nenhuma (avisar na exportação) | Criptografado com senha do usuário |
| Uso típico | Juntar cadastros de duas instalações, abrir no Excel | Cópia de segurança, reinstalar, mudar de servidor |

Pontos de atenção do backup:
- A tela de primeiro uso precisa oferecer **restaurar backup** como alternativa a criar o administrador (numa instalação nova ainda não existe login).
- O arquivo precisa ficar em local seguro, fora de onde o sistema está instalado, senão não vale na hora de reinstalar.
- Perder a senha do backup é perder o backup; isso deve estar escrito na tela.


## 7. Modelagem

Pacote `com.santosediego.donorregistry`, consistente com o `artifactId` `donor-registry`. Nomes de classe, tabela e coluna em inglês; tabelas com prefixo `tb_` e colunas em `snake_case`.

### Donor (`tb_donor`)
| Campo | Tipo | Observação |
|-------|------|------------|
| `id` | `Long` | gerado pelo banco (identity) |
| `name` | `String` | até 120, obrigatório |
| `cpf` | `String` | 11 dígitos, único, obrigatório; a máscara é só de exibição |
| `rg` | `String` | até 20, opcional |
| `birthDate` | `LocalDate` | obrigatório |
| `gender` | `Gender` | obrigatório |
| `maritalStatus` | `MaritalStatus` | opcional |
| `bloodType` | `BloodType` | opcional |
| `phone` | `String` | só dígitos, obrigatório |
| `weight` | `BigDecimal` | em kg, uma casa decimal, opcional |
| `address` | `Address` | opcional; lado inverso da relação (`mappedBy`) |
| `createdAt` | `Instant` | `@PrePersist`; coluna não atualizável |
| `updatedAt` | `Instant` | `@PreUpdate` |

### Address (`tb_address`)
Relação 1:1 **opcional** com `Donor`: o `Address` é o lado dono, com a chave estrangeira `donor_id` (obrigatória, única, `ON DELETE CASCADE`). **Excluir o doador exclui o endereço** (`cascade` e `orphanRemoval` no `Donor`, mais o `ON DELETE CASCADE` no banco). O lado inverso não pode ser preguiçoso. A listagem não carrega a entidade `Address`: usa `LEFT JOIN` com projeção direta no DTO (ver "Listagem e busca"); `LEFT JOIN FETCH` só serve onde a entidade inteira for necessária.

| Campo | Tipo | Coluna | Observação |
|-------|------|--------|------------|
| `id` | `Long` | `id` | gerado |
| `street` | `String` | `street` | até 150 |
| `number` | `String` | `street_number` | até 10; texto, porque existe `15B` e `S/N` |
| `complement` | `String` | `complement` | até 100, opcional |
| `neighborhood` | `String` | `neighborhood` | até 80 |
| `zipCode` | `String` | `zip_code` | 8 dígitos |
| `city` | `String` | `city` | até 100 |
| `state` | `String` | `state` | UF, 2 letras |

No banco todos os campos do endereço aceitam nulo; a obrigatoriedade é regra da API (ver abaixo).

### Datas
`Instant` em UTC, gravado como `TIMESTAMP` sem fuso (propriedades `hibernate.type.preferred_instant_jdbc_type=TIMESTAMP` e `hibernate.jdbc.time_zone=UTC`). Sem `columnDefinition` nas entidades. As datas vêm de `@PrePersist` e `@PreUpdate`, não da auditoria do Spring Data. Nos dados de exemplo o `created_at` é informado com valor fixo, porque o Liquibase não passa pelo `@PrePersist`.

### Enums
Cada enum tem `id` (código curto) e `description` (rótulo em português), mais um `fromId` estático. Persistir **como texto com o código curto, nunca como ordinal**. Valor inválido na API retorna 400. O back expõe as opções para o front (fonte única).

- **Gender:** `MALE("M", "Masculino")`, `FEMALE("F", "Feminino")`, `OTHER("OT", "Outro")`.
- **BloodType:** `A_POSITIVE("A+")`, `B_POSITIVE("B+")`, `AB_POSITIVE("AB+")`, `O_POSITIVE("O+")`, `A_NEGATIVE("A-")`, `B_NEGATIVE("B-")`, `AB_NEGATIVE("AB-")`, `O_NEGATIVE("O-")`, `FALSE_O_NEGATIVE("FO-", "Falso O-")`, `GOLDEN_BLOOD("SD", "Sangue dourado")`. Os dois últimos são tipos raros reais (fenótipo Bombay e Rh-null).
- **MaritalStatus:** `SINGLE("SOL", "Solteiro")`, `MARRIED("CAS", "Casado")`, `SEPARATED("SEP", "Separado")`, `DIVORCED("DIV", "Divorciado")`, `WIDOWED("VIU", "Viúvo")`, `STABLE_UNION("UNI", "União estável")`.

### API de doador (decisões da Parte 4)
- **URL base `/donors`.** POST cria e devolve **201** com cabeçalho `Location`; GET `/donors/{id}` busca; id inexistente devolve **404** no formato `StandardError`.
- **JSON em inglês.** O endereço é um objeto aninhado, `"address": { ... }`, ou `null`.
- **Escrita estrita:** CPF, celular e CEP só aceitam dígitos; valor com máscara devolve **400** com mensagem clara. Quem mascara é o front, só para exibir. A **busca** (Parte 4.2) tolera máscara e tira o que não é dígito antes de consultar.
- **Enums viajam pelo código curto** (`"M"`, `"A+"`, `"SOL"`), nunca pelo nome Java. Valor inválido devolve 400 listando os valores aceitos.
- **CPF duplicado devolve 409** com a mensagem "Já existe um doador cadastrado com este CPF.", sem dados do doador existente (privacidade). A verificação acontece antes de gravar, e a restrição única do banco é a rede de segurança para requisições simultâneas, também mapeada para 409.
- **Resposta** traz `id`, `createdAt` e `updatedAt`. Erros no formato `StandardError`; erros de validação trazem a lista de campos com a mensagem de cada um.

### Listagem e busca (Parte 4.2)
- **Endpoint:** `GET /donors?search=&page=&size=&sort=`. Resposta no envelope `PagedModel` do Spring (`content` e `page` com `size`, `number`, `totalElements`, `totalPages`). Cada item é um `DonorSummaryDTO`: `id`, `name`, `phone`, `cpf`, `rg`, `bloodType` (código curto) e `city` (nulo quando não há endereço). O endereço completo fica no `GET /donors/{id}` e no futuro relatório.
- **Busca (`search`, opcional):** vazio ou ausente = sem filtro. Com valor, o doador entra se **qualquer** condição for verdadeira:
  - **Nome:** termo (com `trim`) contido no nome, ambos passando por `LOWER(TRANSLATE(x, 'áàâãä…ñÁÀÂÃÄ…Ñ', 'aaaaa…nAAAAA…N'))` (a lista completa fica numa constante só, no código). O `TRANSLATE` vem antes do `LOWER` para não depender do locale do banco. `joao` acha `João` e `Joao`; `conceicao` acha `Conceição`. Sem coluna nova.
  - **CPF:** só quando o termo, descontados `.`, `-` e espaços, tem apenas dígitos; busca esses dígitos contidos no CPF.
  - **RG:** termo como digitado (em minúsculas no Java), contido em `LOWER(rg)`, sem diferenciar maiúscula de minúscula. **Limitação conhecida:** o RG não tem formato único (cada estado emite o seu), então a pontuação não é normalizada: buscar `00000074` não acha `MG-00.000.074`.
- **Consulta:** `LEFT JOIN` do endereço só para obter `city`, com projeção direta no DTO (nenhuma entidade `Address` é carregada) e consulta de contagem separada. Uma página gera 2 consultas, qualquer que seja o `size`. Doador sem endereço aparece (armadilha #10).
- **Paginação:** `page` começa em 0; `size` padrão 10, máximo 100 (acima disso é reduzido a 100). Ordenação padrão por `name` crescente; `sort` só aceita `name` e `createdAt`, `asc` ou `desc`; outro campo devolve 400 listando os aceitos. Lista vazia devolve 200 com `content` vazio.

### Validações
- `name`: obrigatório, 2 a 120 caracteres, sem espaços nas pontas. `cpf`: obrigatório, 11 dígitos, dígitos verificadores válidos (`@CPF`), único. `birthDate`: obrigatória, no passado. `gender`: obrigatório. `phone`: obrigatório, 10 ou 11 dígitos.
- Opcionais: `rg` (até 20), `maritalStatus`, `bloodType`, `weight` (maior que 0 e até 300).
- **Idade mínima e máxima para doar não é validada no cadastro:** é triagem na hora da coleta, não regra do registro.
- **Endereço:** opcional como um todo. Se for enviado, `street`, `number`, `neighborhood`, `zipCode`, `city` e `state` são obrigatórios; só `complement` é opcional. Endereço incompleto devolve 400 listando os campos. Limites: `zipCode` com 8 dígitos; `state` com 2 letras; `street` até 150, `number` até 10, `complement` até 100, `neighborhood` até 80, `city` até 100. A ONG atende um único município, então a cidade sozinha não diferencia nada; a regra de "todos ou nenhum" existe sobretudo para praticar validação entre campos, não por necessidade da fundação.
- Mensagens de validação em português, sempre alinhadas com a regra.

### Convenções
- Sem `columnDefinition` específico de banco nas entidades. Enums sem setters. Sem anotações de Jackson nas entidades: o JSON é assunto dos DTOs.
- Injeção por construtor, sem `@Autowired` em atributo. Sem Lombok e sem MapStruct: construtores, getters e setters escritos à mão, e mapeamento DTO ↔ entidade manual.
- Pacotes por camada; controllers se chamam `Resource`.
- Busca sem acento com `TRANSLATE` completo (letras acentuadas em minúsculas e maiúsculas), aplicado **antes** do `LOWER` e nos dois lados (coluna e termo), com a lista de caracteres definida uma única vez. Assim o resultado não depende do locale do banco.

## 8. Segurança e privacidade: checklist

- [ ] Nenhum segredo, senha ou telefone real no repositório (incluindo o histórico).
- [ ] Segredo do JWT e senha do banco aleatórios por instalação.
- [ ] Sem senha padrão do administrador.
- [ ] CORS restrito à origem do front.
- [ ] Backup criptografado; CSV com aviso de dados pessoais.
- [ ] Token de configuração no primeiro uso, limite de tentativas e HTTPS quando hospedado.
- [ ] Logo e contato fora do código; dados de exemplo todos fictícios.
- [ ] Nada com dados pessoais gravado em disco sem necessidade (sem `export/` e `import/`).
- [ ] Dados pessoais e de saúde (tipo sanguíneo): tratar como sensíveis também na documentação.

## 9. Telas (protótipos)

Canvas "Registro de Doadores":
1. Lista de doadores (busca, paginação, menu Arquivos)
2. Cadastro, visualização e edição (um único componente)
3. Escolher doadores para o relatório ("Selecionar todos" = todos os resultados da busca)
4. Relatório A4 em HTML
5. Login
6. Primeiro uso (criar administrador ou restaurar backup)
7. Código de recuperação
8. Recuperar senha
9. Configurações e backup

### Decisões de texto e interface

- **Nada preso a um computador.** O sistema roda local ou hospedado, então nenhum texto diz "este computador" ou "esta máquina". Usar "sistema" e "instalação".
- **Backup:** a orientação é só "Guarde em local seguro". Sem sugerir pendrive (corrompe com o tempo) e sem explicar demais. O aviso de que **a senha do backup não é recuperável** fica nas Configurações, antes de criar o backup, que é quando serve.
- **Sem mensagens que soem como bronca.** Na restauração, senha vazia ou errada é tratada pela validação do próprio sistema, sem texto do tipo "se perdeu, comece do zero".
- **Restaurar backup não troca a senha.** O checkbox "definir nova senha ao restaurar" foi descartado: duplicava o que o código de recuperação e o comando de redefinição do administrador já resolvem.
- **Código de recuperação:** mostrado em uma linha só, no mesmo formato que se digita (`grupo-grupo-...` ou `palavra-palavra-...`), sem balões nem numeração, para ninguém anotar errado.
  - Os hífens não fazem parte do segredo: a segurança está nas 6 palavras. Em uma senha comum (como a do VeraCrypt) o programa compara a string exata, então o hífen conta; aqui o servidor normaliza antes do hash (remove acento, minúsculas, descarta o que não é letra), igual às frases de recuperação de carteiras de criptomoeda.
  - Por isso aceita hífen, espaço ou tudo junto. A tela de recuperar senha avisa isso em uma linha.
  - Ao exibir o código, pode ser oferecido copiar, imprimir e a confirmação "Anotei o código" antes de continuar.

### Decisão adiada: formato do código de recuperação

Será decidida lá na frente, na etapa de autenticação. Critérios combinados: **fácil para o usuário** e **sem dependência nem influência externa** no repositório (nenhuma lista ou licença de terceiros).

| Opção | Segurança | Licença | Anotar à mão | Observação |
|-------|-----------|---------|--------------|------------|
| Caracteres aleatórios (Base32 Crockford, 16 caracteres em 4 grupos de 4, com dígito verificador) | 80 bits | nenhuma | médio | Mais simples de implementar. Alfabeto sem I, L, O e U; na entrada, O vira 0 e I/L viram 1. Era o formato do primeiro mock (`K7QM-4XD2-9TWP-HN6R`). |
| Palavras em português (Diceware, 6 palavras) | ~77 bits | **precisa de lista livre**: a Dadoware (Thoughtworks) é CC BY-NC e não serve; teria que ser lista própria, feita de fonte livre | fácil | Trabalho extra: montar e testar a lista (sem acento, 4 a 7 letras, sem nomes próprios nem ambiguidade). |
| Palavras em inglês (lista da EFF, a mesma lógica do Bitwarden) | ~77 bits | permissiva (conferir) | ruim para quem não lê inglês | Descartada pelo público brasileiro. |

Notas:
- A segurança é parecida nas duas primeiras (7 palavras ou 20 caracteres sobem para 90 a 100 bits). A escolha é de usabilidade e de dependências, não de segurança.
- As telas 7 e 8 do protótipo mostram o formato de 6 palavras **só como exemplo**; o formato final vem desta decisão.
- Princípio para o repositório: não embutir lista, dicionário ou arquivo de terceiros cuja licença restrinja uso comercial ou redistribuição.


## 10. Perguntas em aberto

- **Licença:** MIT, já no `LICENSE`. Falta preencher `<licenses>` no `pom.xml` e decidir o que entra no README sobre dados pessoais: o projeto trata CPF e tipo sanguíneo.
- **Autenticação:** JWT ou sessão (decidir ao chegar na etapa).
- **Formato do código de recuperação** (caracteres ou palavras em português): decidir na etapa de autenticação; ver "Decisão adiada" na seção 9. Qualquer lista de palavras precisa ser livre.
- **Tipo de merge** (squash ou merge commit) e comportamento do release-please nas versões `0.x`: decidir na Parte 7.
- **Testcontainers** (testar contra PostgreSQL real em vez de só H2): decidir depois da Parte 5.
- **Repositório template** extraído deste projeto, depois da Parte 7.
