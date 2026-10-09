# Requisitos do projeto LoginSeguro

Este documento reúne os principais requisitos do LoginSeguro e mostra como eles foram atendidos no projeto.

## 1. Funcionalidades implementadas

* **Cadastro de usuários:** formulário com validação dos campos e verificação de e-mails já cadastrados.
* **Login e logout:** autenticação feita pelo Spring Security, com senhas armazenadas usando BCrypt.
* **Perfis de acesso:** o sistema possui três perfis: usuário, moderador e administrador. Cada perfil tem acesso às páginas permitidas.
* **Proteção das páginas:** as permissões são verificadas no servidor, impedindo o acesso a áreas restritas por usuários sem autorização.
* **Banco de dados:** o MongoDB é utilizado para armazenar os usuários. O projeto também utiliza o Spring Session para armazenar as sessões no banco.
* **Interface:** as páginas foram desenvolvidas com Thymeleaf e utilizam arquivos CSS compartilhados, facilitando alterações no visual.
* **Mensagens de erro:** o sistema possui páginas para situações como acesso negado, página não encontrada e problemas de conexão com o banco.

## 2. Segurança

Foram utilizadas medidas de segurança para proteger as contas e controlar o acesso ao sistema:

* Senhas armazenadas em formato de hash com BCrypt.
* Validação dos dados enviados nos formulários.
* Verificação de e-mails duplicados, incluindo índice único no banco.
* Controle de acesso de acordo com o perfil do usuário.
* Proteção CSRF nos formulários.
* Encerramento da sessão no logout.
* Configurações para proteger cookies e reduzir a exposição de informações em mensagens de erro e logs.
* Separação das configurações de conexão por variáveis de ambiente, evitando deixar credenciais diretamente no código.

O cadastro público cria usuários comuns. A criação de contas com privilégios de administrador ou moderador é tratada separadamente, conforme a configuração do projeto.

## 3. Organização do código

O código Java está separado em pacotes de acordo com suas responsabilidades:

* `config`: configurações e inicialização de usuários.
* `controller`: recebe as requisições e controla as páginas.
* `dto`: objetos utilizados para receber e validar os dados dos formulários.
* `model`: classes que representam os usuários e seus perfis.
* `repository`: acesso aos dados armazenados no MongoDB.
* `security`: autenticação e configurações de segurança.
* `service`: regras relacionadas ao cadastro e ao gerenciamento de usuários.

Os arquivos das páginas ficam em `src/main/resources/templates`, enquanto os arquivos CSS e JavaScript ficam em `src/main/resources/static`.

## 4. Testes

O projeto possui testes automatizados para verificar a validação dos formulários, o cadastro de usuários, a autenticação e as permissões de acesso.

A execução de `mvn test` foi concluída com `BUILD SUCCESS`, indicando que os testes executados nessa ocasião passaram.

Os testes que dependem de um banco MongoDB precisam ser executados com a configuração de banco correspondente. Para verificar especificamente a integração com o MongoDB Atlas, é necessário utilizar a URI do Atlas na variável `TEST_MONGODB_URI`.

## 5. Documentação e publicação

O arquivo `README.md` apresenta as tecnologias utilizadas, os requisitos para executar o projeto, as instruções de configuração do MongoDB Atlas e os comandos necessários para iniciar a aplicação e executar os testes.

O projeto também possui um arquivo `.env.example`, com exemplos das variáveis de ambiente, e um `.gitignore` para evitar o envio de arquivos locais e credenciais ao repositório.

O código-fonte está publicado no GitHub, junto com a documentação e os testes.

Repositório: https://github.com/MariaEduardaPaixaoO/Sistema-de-Login-Seguro
