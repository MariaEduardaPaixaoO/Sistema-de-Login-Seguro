# LoginSeguro

O LoginSeguro é um sistema de cadastro e autenticação de usuários desenvolvido com Java e Spring Boot. O projeto utiliza MongoDB Atlas para armazenar os dados e Spring Security para controlar o login e as permissões de acesso.

A ideia é ter uma estrutura de login que possa ser utilizada em outros projetos, permitindo alterar o visual e acrescentar novas páginas sem precisar refazer toda a parte de autenticação.

## Tecnologias utilizadas

* Java 17 ou superior
* Spring Boot
* Spring Security
* Thymeleaf
* MongoDB Atlas
* Spring Data MongoDB
* Spring Session
* Maven
* JUnit e Mockito para os testes

## Funcionalidades

O sistema possui:

* Cadastro de usuários com validação dos dados.
* Login e logout.
* Senhas armazenadas com hash BCrypt.
* Três perfis de acesso: usuário, moderador e administrador.
* Páginas específicas para cada perfil.
* Controle de acesso às páginas protegidas.
* Armazenamento de usuários e sessões no MongoDB.
* Interface adaptável, com estilos organizados em arquivos CSS.
* Testes automatizados das funcionalidades principais.

O projeto não possui recuperação de senha, autenticação em duas etapas ou login por redes sociais, pois essas funcionalidades não fazem parte do escopo atual.

## Organização do projeto

Os arquivos estão organizados por responsabilidade:

* `config`: configurações e criação inicial de usuários privilegiados.
* `controller`: controla as requisições e as páginas.
* `dto`: recebe e valida os dados dos formulários.
* `model`: representa os usuários e seus perfis.
* `repository`: acesso aos dados do MongoDB.
* `security`: configurações de autenticação e autorização.
* `service`: regras relacionadas aos usuários.
* `src/main/resources/templates`: páginas HTML com Thymeleaf.
* `src/main/resources/static`: arquivos CSS e JavaScript.
* `src/test`: testes automatizados.

O arquivo `pom.xml` contém as dependências utilizadas pelo projeto.

## Requisitos para executar

Antes de iniciar, instale:

* JDK 17 ou superior.
* Maven 3.9 ou superior.
* Uma conta no [MongoDB Atlas](https://www.mongodb.com/cloud/atlas).

Também é necessário ter acesso à internet para baixar as dependências e conectar ao banco de dados.

## Configurando o MongoDB Atlas

1. Crie uma conta no MongoDB Atlas e um projeto.
2. Crie um cluster. Para desenvolvimento, o plano gratuito pode ser suficiente.
3. Em **Database Access**, crie um usuário e uma senha para a conexão com o banco.
4. Em **Network Access**, autorize o endereço IP da máquina em que a aplicação será executada.
5. Abra a opção de conexão do cluster, selecione Java e copie a URI disponibilizada.
6. Substitua os dados de exemplo pelos dados do seu usuário de banco.

Evite liberar o acesso a partir de qualquer endereço IP sem necessidade. A URI contém credenciais e não deve ser publicada no GitHub.

## Configurando as variáveis de ambiente

Na pasta principal do projeto, existe o arquivo `.env.example` com os nomes das variáveis utilizadas.

Copie o arquivo para criar sua configuração local:

```bash
cp .env.example .env
```

Abra o `.env` e preencha os valores necessários. A variável principal é:

```env
MONGODB_URI=mongodb+srv://...
MONGODB_DATABASE=login_seguro
```

Para criar as contas iniciais de administrador e moderador, também podem ser utilizadas as variáveis `BOOTSTRAP_ADMIN_*` e `BOOTSTRAP_MODERATOR_*`. Os detalhes estão na seção de perfis de acesso.

**Não envie o arquivo `.env` para o GitHub.** Ele contém configurações locais e pode guardar senhas reais. O `.gitignore` do projeto já está preparado para ignorá-lo.

> Observação: a aplicação precisa estar configurada para carregar o arquivo `.env`. Caso o carregamento não esteja implementado, configure as variáveis diretamente no ambiente antes de iniciar o sistema.

## Executando a aplicação

Clone o repositório e entre na pasta do projeto:

```bash
git clone https://github.com/MariaEduardaPaixaoO/Sistema-de-Login-Seguro.git
cd Sistema-de-Login-Seguro
```

Configure as variáveis de ambiente e execute:

```bash
mvn spring-boot:run
```

Depois, acesse:

http://localhost:8080

A página inicial permite acessar o login e o cadastro de usuários.

Para gerar o arquivo JAR, utilize:

```bash
mvn clean package
```

O arquivo gerado ficará na pasta `target/`.

## Perfis de acesso

O sistema possui três perfis:

| Perfil           | Acesso                |
| ---------------- | --------------------- |
| `ROLE_USER`      | Área do usuário       |
| `ROLE_MODERATOR` | Área do moderador     |
| `ROLE_ADMIN`     | Área do administrador |

Todo cadastro público cria uma conta com o perfil de usuário comum. Não é possível escolher o perfil de administrador ou moderador pelo formulário.

As contas privilegiadas podem ser criadas inicialmente por meio das variáveis de ambiente descritas no `.env.example`. O cadastro inicial não altera automaticamente as permissões de uma conta que já existe.

As permissões são verificadas pelo Spring Security no servidor. Portanto, tentar abrir diretamente uma página restrita não permite contornar as regras de acesso.

## Segurança

O projeto utiliza BCrypt para armazenar as senhas em formato de hash. Também possui validação dos formulários, proteção CSRF, controle de sessão e restrições de acesso por perfil.

As mensagens de login inválido não informam se o erro ocorreu no e-mail ou na senha. As configurações de cookies também incluem proteções específicas, com o uso de `Secure` no perfil de produção.

As sessões são armazenadas no MongoDB por meio do Spring Session, separadamente dos documentos dos usuários.

## Executando os testes

Para executar os testes automatizados, utilize:

```bash
mvn test
```

O projeto possui testes para validação dos formulários, regras de cadastro, autenticação, permissões de acesso e integração com o MongoDB.

Os testes unitários e web não dependem de um banco real. Já os testes de integração precisam de um MongoDB configurado por meio da variável `TEST_MONGODB_URI`.

Exemplo com um MongoDB local:

```bash
TEST_MONGODB_URI='mongodb://localhost:27017' mvn test
```

Para testar a integração com o Atlas, configure `TEST_MONGODB_URI` com a URI de um banco destinado aos testes. O usuário utilizado precisa ter as permissões necessárias para criar e remover o banco temporário.

## Alterando o visual

Os estilos estão divididos em dois arquivos principais:

* `theme.css`: cores, fontes e outras variáveis visuais.
* `app.css`: estilos dos componentes e das páginas.

O layout e os fragmentos do Thymeleaf também são compartilhados entre as páginas. Isso facilita modificar a aparência do sistema sem precisar alterar a lógica de autenticação.

## Limitações atuais

O projeto atende às funcionalidades previstas para o sistema de login, mas ainda pode receber melhorias. Entre as funcionalidades que não foram incluídas estão recuperação de senha, verificação de e-mail, autenticação em duas etapas e limitação de tentativas de login.

Para utilizar o sistema em produção, é necessário configurar HTTPS, restringir o acesso ao banco, proteger as credenciais e manter as dependências atualizadas.

## Repositório

O código-fonte, os testes e a documentação estão disponíveis no GitHub:

[LoginSeguro — GitHub](https://github.com/MariaEduardaPaixaoO/Sistema-de-Login-Seguro)
