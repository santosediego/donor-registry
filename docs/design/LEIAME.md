# Protótipos de tela

Cada `*.dc.html` é uma tela do protótipo (HTML + CSS prontos, com dados fictícios). Servem de **referência visual e de estrutura** para o front-end real: cores, fontes, espaçamentos, textos e comportamento. Não são código de produção.

- `canvas.json`: posição e título de cada tela no canvas original.
- Numeração das telas, na ordem de uso: 1 Primeiro acesso (código de configuração), 1a Código incorreto, 1b Bloqueio por tentativas, 2 Primeiro uso (começar do zero ou restaurar backup), 3 Código de recuperação, 4 Login, 5 Recuperar senha, 6 Lista de doadores, 7 Cadastro, 8 Seleção para relatório, 9 Relatório (A4), 10 Configurações e backup.
- A tela 1 pede o código de configuração gerado pelo back na primeira inicialização. Só depois dela aparece a tela 2. As telas 1a e 1b são os estados de erro da tela 1.
- Nome do termo: "código de configuração" (do servidor, uso único na instalação). Não confundir com "código de recuperação" (do usuário, telas 3 e 5).
- As telas 3 e 5 mostram o código de recuperação em formato de exemplo; o formato final está como decisão adiada no plano.
