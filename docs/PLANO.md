# Registro de Doadores: plano de desenvolvimento

Documento de trabalho para o desenvolvimento do projeto, pensado para ser usado como briefing ao delegar código à IA. Cada passo tem objetivo, o que muda e como saber que ficou certo.

## 1. Contexto

- O Registro de Doadores é uma releitura, atualização e melhoria de um projeto antigo, feito pelo autor para uma ONG. O objetivo deste repositório é **aprender e praticar a delegação do desenvolvimento para a IA** (Claude e Claude Code), em vez de desenvolver sozinho.
- O sistema original foi pensado para rodar localmente na máquina do cliente, e o CSV existia para levar dados de uma instalação para outra. O projeto novo continua podendo rodar assim, mas também **precisa rodar hospedado** (servidor ou nuvem). Não há suporte a sistemas já descontinuados (Windows 7, por exemplo): quem estiver neles hospeda o sistema em outro lugar.
- **Stack do projeto novo:** Spring Boot 4.1.2 (gerado pelo Spring Initializr; `groupId` `com.santosediego`, `artifactId` `registro-de-doadores`), Java 25 (LTS), JPA, Liquibase, H2 em arquivo ou PostgreSQL (ver perguntas em aberto), React com Vite e TypeScript.
- **Repositório novo, público, começando do zero.** O código antigo não é usado. As regras de negócio e os problemas que o projeto antigo teve estão resumidos na seção 5, e cada problema vira um teste no projeto novo.

## 2. Decisões já tomadas

| Tema | Decisão |
|------|---------|
| Tipo de aplicação | Roda local **ou hospedado**, o mesmo sistema nos dois casos (nenhuma tela ou texto preso a "este computador") |
| Login | Mantido, com **um único administrador** (sem CRUD de usuários nem papéis) |
| Senha padrão | Não existe: o primeiro uso pede a senha do administrador |
| Recuperação | Código de recuperação (formato a decidir no passo 2), mostrado uma única vez; se perder também o código, comando de **redefinição do administrador** rodado no terminal do servidor |
| Contato, nome da entidade e logo | Viram **configuração** (tabela e tela), saem do código |
| Esquema do banco | **Liquibase**, no lugar do `create.sql` gerado à mão |
| Exportação | **CSV e backup**, com papéis diferentes (ver seção 6) |
| Distribuição | **Docker** (`compose` com banco, back e front) e um README de "como rodar" são o essencial. O **script de instalação** é opcional, fica para o fim |
| Layout | Protótipos novos (lista, cadastro, seleção, relatório, login, primeiro uso, recuperação e configurações) |
| Banco | **PostgreSQL** para uso real (local ou hospedado). **H2 em memória** só nos testes automatizados e na demonstração (os dados não persistem) |
| Ferramentas | Java LTS (25), Spring Boot na versão estável mais recente (4.1.x), Node LTS no front. Tudo atual e mantido; sem versões descontinuadas |

## 3. Como delegar à IA

Combinados entre o autor e a IA:

1. **Um passo por vez.** Fazer só o passo pedido, sem antecipar os seguintes. Ao terminar, abrir o PR e parar.
2. **Testes antes de código.** Cada requisito começa por um teste que falha.
3. **Commits pequenos**, com um propósito cada. Se a IA errar, volta-se um commit.
4. **Convenções desde o início:** monorepo (`backend/`, `frontend/`, `instalador/`), perfis de `properties` (test, dev, prod), Liquibase, sem segredo no repositório.
5. **Revisão pelo autor.** O autor lê cada diff como se fosse PR de outra pessoa e pergunta "por que assim?" onde não entender; a IA explica antes de seguir.
6. Não misturar atualização de dependências com mudança de funcionalidade.
7. **Fluxo de git:**
   - `main`: produção, protegida (só entra por *pull request*).
   - `dev`: integração e testes exploratórios. Roda com o perfil `dev` e um banco real, separado do de produção.
   - **Branches curtas por passo** saem da `dev` e voltam para ela por PR: `ia/passo-02-auth` (trabalho da IA) e `diego/...` (trabalho do autor). Apagadas após o merge.
   - Quando a `dev` estiver validada nos testes exploratórios, PR da `dev` para a `main`.
   - Quem **aprova e faz o merge é o autor**, depois de ler o diff e ver os testes passando (GitHub Actions).
   - Commits da IA levam `Co-Authored-By`, o que deixa claro no repositório público o que foi feito com ela. Mensagens no padrão do projeto (ex.: Conventional Commits).

## 4. Ordem dos passos

### Passo 1: base e testes
- Gerar o projeto (Spring Initializr), com pacote `com.santosediego.registrodedoadores`, perfis de `properties` (test, dev, prod) e CI no GitHub Actions.
- Escrever os cenários (visão de QA): cadastrar, editar, excluir, buscar, paginar, exportar CSV, importar CSV, gerar relatório, login.
- Transformar cada item da seção 5 em um teste (que falha até a funcionalidade existir).

### Passo 2: autenticação
- **Autenticação com JWT:** login com usuário e senha, token assinado e validado por um filtro do Spring Security. Sem OAuth2 e sem `client-id`/`client-secret`. Algoritmo de assinatura e duração do token definidos neste passo.
- **Primeiro uso:** criar o administrador (usuário e senha escolhidos) **ou** restaurar um backup.
- **Segredo do JWT gerado na primeira execução**, guardado em arquivo local (fora do repositório e fora do backup).
- **Código de recuperação** gerado no primeiro uso, mostrado uma vez e guardado só como hash.
- Tela de redefinir senha pelo código e "Alterar senha" nas configurações.
- **Código de recuperação: formato a decidir quando chegarmos neste passo** (ver "Decisão adiada" abaixo). Vale em qualquer formato: sorteio com `SecureRandom`, normalização antes do hash (remover acento, minúsculas, descartar o que não for letra ou número, aceitando hífen, espaço ou tudo junto), guardar só o hash (bcrypt/argon2), erro genérico ("código inválido") e limite de tentativas.
- **Caminhos de recuperação:**
  1. Lembra a senha: Configurações > Alterar senha.
  2. Esqueceu a senha, tem o código: tela "Recuperar senha". Se o 2FA estiver ligado, o código também o desliga (é a recuperação do 2FA, não há segundo mecanismo). Depois de usado, o código é substituído por um novo, mostrado uma vez.
  3. Perdeu a senha **e** o código: comando de **redefinição do administrador**, rodado no terminal por quem administra o servidor (`redefinir-admin`, ou `docker exec ...` no Docker). Define nova senha (pedida no prompt, **nunca como argumento** do comando), desliga o 2FA e gera novo código; **os doadores e as configurações não são apagados**. Regras: **nunca** existe como endpoint HTTP nem como botão no sistema; registra o evento no log; tem teste automatizado (senha trocada, 2FA desligado, dados intactos). Não enfraquece a segurança: quem tem acesso ao servidor já tem acesso ao banco e aos segredos.
  - Restaurar um backup não troca a senha: o backup restaura o que estava lá, e a redefinição acima cobre o caso de senha perdida.
- **Primeiro uso protegido:** a tela de criar administrador fica aberta a quem chegar primeiro. Em instalação local o risco é baixo; **hospedado, não**. A inicialização gera um **token de configuração** de uso único, mostrado no console/log (ou passado por variável de ambiente) e exigido nessa tela. Depois que o administrador existe, as rotas de configuração inicial deixam de responder.
- **Limite de tentativas** no login e no código de recuperação (atraso progressivo ou bloqueio temporário). **HTTPS** obrigatório quando hospedado (proxy reverso).
- Sem valor padrão para segredo em nenhum perfil: se faltar, a aplicação falha ao subir.

### Passo 2.1 (opcional): segundo fator de autenticação (TOTP)

Opcional para quem usa: **desligado por padrão**, ativado em Configurações. Só depois de o resto estar de pé.

- **Padrão:** TOTP (RFC 6238), o código de 6 dígitos de qualquer app autenticador. Sem SMS nem e-mail, sem serviço externo, funciona offline.
- **Biblioteca:** usar uma biblioteca de TOTP mantida e de licença permissiva (Apache 2.0 ou MIT), em vez de escrever o algoritmo; mais uma lib pequena para o QR code. Janela de tolerância de 30 s para cada lado, por causa do relógio.
- **Ativar:** gerar o segredo, mostrar QR e texto, e só ligar depois de o usuário digitar um código válido.
- **Segredo cifrado no banco** (não pode ser hash, o servidor precisa dele), com chave gerada na instalação e **incluído no backup** (que já é cifrado), para a restauração não trancar o administrador.
- **Login:** senha primeiro, depois o código; limite de tentativas também no segundo passo.
- **Perdeu o celular:** código de recuperação ou comando de redefinição do administrador; ambos desligam o 2FA. Como o código passa a valer pelos dois fatores, precisa ser tratado como a chave mestra: aparece uma vez, hash no banco, limite de tentativas.
- **Reaproveitamento (também em projeto particular):** manter autenticação, 2FA, código de recuperação, backup e configurações num pacote próprio, sem depender das classes de doador (`Doador`, `Endereco`...). Assim dá para copiar o conjunto para outro projeto. Basta separar por pacote; um módulo Maven só se a necessidade aparecer.

### Passo 3: Liquibase
- Esquema em `changeSets` versionados, com tipos do Liquibase (portáveis entre H2 e PostgreSQL).
- Dados de exemplo só nos contextos `dev` e `test`; nunca em `prod`.
- `ddl-auto=validate` em prod.
- Esquema conforme o modelo da seção 7.

### Passo 4: configurações
- Tabela de configurações (nome da entidade, contato, logo), API protegida e tela.
- Relatório lê desses dados. Nenhum logo, telefone ou nome real no código-fonte.

### Passo 5: CSV
- Exportador escreve num `OutputStream` (nada de arquivo em `export/`).
- Importador lê um `InputStream` com **OpenCSV** (sem `split(";")` com índices) e devolve a lista de DTOs ou os erros por linha.
- UTF-8 explícito, valores entre aspas, nome de arquivo nunca usado como caminho.
- Importação dentro de **uma transação** (tudo ou nada), chamada por um método público de outro bean.
- O CSV exportado precisa ser reimportável, inclusive com campos vazios.

### Passo 6: backup
- Arquivo **criptografado** (AES) com senha escolhida pelo usuário na hora de gerar; a senha do banco não protege o arquivo.
- Conteúdo: doadores, usuário (senha em hash), configurações, logo e **versão do esquema**.
- Restaurar: pede a senha do backup; recusa backup de versão mais nova que a da aplicação; migra versões mais antigas com o Liquibase.
- Teste automatizado: gerar backup, restaurar num banco vazio e conferir totais e conteúdo.
- Lembrete na tela ("último backup há N dias") e aviso para guardar o arquivo em local seguro.

### Passo 7: front-end
- React com Vite e TypeScript, aplicando o layout dos protótipos (seção 9).
- Máscara de campos com biblioteca mantida (ou implementação própria).
- Interceptor: tratar erro **sem** `response` (back fora do ar).
- Login: enviar o corpo codificado corretamente (senha com `&`, `+` ou `%` precisa autenticar).
- Redirecionamento de 401/403 decidido pelo roteador.
- **Fonte única para as opções** (gênero, estado civil, grupo sanguíneo): o back expõe as opções e o front só consome.

### Passo 8: distribuição
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
| 9 | Busca por CPF falhar sem a máscara | Buscar `11144477735` deve achar `111.444.777-35` |
| 10 | Consulta com `JOIN` sem `FETCH`: N+1 nas listagens e doador sem endereço sumindo da busca | Listar doadores sem consultas em excesso; doador sem endereço (se permitido) aparece na busca |
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

Pacote `com.santosediego.registrodedoadores`, consistente com o `artifactId` `registro-de-doadores`.

### Doador
| Campo | Tipo | Observação |
|-------|------|------------|
| `id` | `Long` | gerado |
| `nome` | `String` | |
| `cpf` | `String` | só dígitos, validado com `@CPF`; a máscara é só de exibição |
| `rg` | `String` | |
| `dataNascimento` | `LocalDate` | |
| `genero` | `Genero` | |
| `estadoCivil` | `EstadoCivil` | |
| `grupoSanguineo` | `GrupoSanguineo` | |
| `celular` | `String` | só dígitos |
| `peso` | `BigDecimal` | em kg, uma casa decimal |
| `dataCadastro` | `Instant` | preenchido pela auditoria do Spring Data |
| `dataAlteracao` | `Instant` | preenchido pela auditoria do Spring Data |

### Endereco
Relação 1:1 com `Doador`; **excluir o doador exclui o endereço** (`cascade` e `orphanRemoval`).

| Campo | Tipo | Observação |
|-------|------|------------|
| `id` | `Long` | gerado |
| `logradouro` | `String` | |
| `numero` | `String` | |
| `complemento` | `String` | |
| `bairro` | `String` | |
| `cep` | `String` | só dígitos |
| `localidade` | `String` | cidade (nome escolhido por causa da ambiguidade município/cidade) |
| `estado` | `String` | UF, 2 letras |

### Enums
Cada enum tem `codigo` e `rotulo`. Persistir **como texto, nunca como ordinal**; a forma exata (nome do enum ou código via `AttributeConverter`) é definida no passo 3. Valor inválido na API retorna 400. O back expõe as opções para o front (fonte única).

- **Genero:** `MASCULINO("M", "Masculino")`, `FEMININO("F", "Feminino")`, `OUTRO("OT", "Outro")`.
- **GrupoSanguineo:** `A_POSITIVO("A+", "A+")`, `B_POSITIVO("B+", "B+")`, `AB_POSITIVO("AB+", "AB+")`, `O_POSITIVO("O+", "O+")`, `A_NEGATIVO("A-", "A-")`, `B_NEGATIVO("B-", "B-")`, `AB_NEGATIVO("AB-", "AB-")`, `O_NEGATIVO("O-", "O-")`, `FALSO_O_NEGATIVO("FO-", "Falso O-")`, `SANGUE_DOURADO("SD", "Sangue dourado")`.
- **EstadoCivil:** `SOLTEIRO("SOL", "Solteiro")`, `CASADO("CAS", "Casado")`, `SEPARADO("SEP", "Separado")`, `DIVORCIADO("DIV", "Divorciado")`, `VIUVO("VIU", "Viúvo")`, `UNIAO_ESTAVEL("UNI", "União estável")`.

### Validações (proposta, ajustar antes do passo 3)
- `nome`: obrigatório, 3 a 120 caracteres. `cpf`: obrigatório, 11 dígitos, dígitos verificadores válidos, único. `dataNascimento`: obrigatória, não pode ser futura. `genero`: obrigatório. `celular`: obrigatório, 10 ou 11 dígitos.
- Opcionais: `rg` (até 20), `estadoCivil`, `grupoSanguineo`, `peso` (maior que 0 e até 300).
- Endereço: `cep` com 8 dígitos; `estado` com 2 letras; `logradouro` até 150, `numero` até 10, `complemento` até 100, `bairro` até 80, `localidade` até 100.
- Mensagens de validação sempre alinhadas com a regra.

### Convenções
- Sem `columnDefinition` específico de banco nas entidades. Enums sem setters.
- Injeção por construtor, sem `@Autowired` em atributo.
- Mapeamento DTO ↔ entidade com MapStruct.
- Busca com coluna normalizada (portável), sem `TRANSLATE` parcial.

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

Será decidida lá na frente, no passo 2. Critérios combinados: **fácil para o usuário** e **sem dependência nem influência externa** no repositório (nenhuma lista ou licença de terceiros).

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

- **Licença** do repositório público (MIT, Apache 2.0...) e o que entra no README sobre dados pessoais: o projeto trata CPF e tipo sanguíneo.
- **Formato do código de recuperação** (caracteres ou palavras em português): decisão adiada para o passo 2; ver "Decisão adiada" na seção 9. Qualquer lista de palavras precisa ser livre.
