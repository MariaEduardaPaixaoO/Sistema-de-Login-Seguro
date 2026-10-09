# Login Seguro

Sistema **genérico** de cadastro, autenticação e autorização de usuários, feito com Java, Spring Boot, Spring Security, Thymeleaf e MongoDB Atlas. Foi pensado como base reutilizável: para adaptá-lo ao tema de outro projeto (por exemplo, um PFC), basta trocar o visual e adicionar as páginas de negócio; a lógica de segurança e de persistência não precisa mudar.

## 1. Objetivo e escopo

**Inclui:**

- Cadastro de usuários com validação no servidor e no navegador.
- Login e logout com Spring Security, senhas com hash BCrypt.
- Três perfis (`ROLE_USER`, `ROLE_MODERATOR`, `ROLE_ADMIN`) com controle de acesso por rota, no servidor.
- Usuários **e sessões HTTP** guardados no MongoDB Atlas.
- Interface Thymeleaf responsiva, com tema isolado em CSS para troca futura.
- Testes automatizados.

**Não inclui (de propósito):** recuperação de senha por e-mail, 2FA, login social, notificações, APIs REST extras, painel de gerenciamento de usuários ou seletor de temas.

## 2. Tecnologias

| Tecnologia | Uso | Versão |
|---|---|---|
| Java | Linguagem | 17 ou superior (compila com `release 17`) |
| Spring Boot | Base da aplicação | 3.5.6 (gerencia as versões das demais bibliotecas) |
| Spring Security | Autenticação, autorização, CSRF, sessão | gerenciada pelo Boot |
| Thymeleaf | Páginas HTML no servidor | gerenciada pelo Boot |
| Spring Data MongoDB | Persistência dos usuários | gerenciada pelo Boot |
| Spring Session Data MongoDB | Sessões HTTP no MongoDB | gerenciada pelo Boot |
| Bean Validation (Hibernate Validator) | Validação no servidor | gerenciada pelo Boot |
| MongoDB Atlas | Banco de dados | — |
| Maven | Build e execução | 3.9 ou superior |
| JUnit 5, Mockito, Spring Security Test | Testes | gerenciadas pelo Boot |

Se, no futuro, uma versão mais nova do Spring Boot 3.5.x estiver disponível, basta atualizar o `<version>` do parent no `pom.xml`.

## 3. Requisitos de ambiente

- JDK 17 ou superior (`java -version`)
- Maven 3.9 ou superior (`mvn -version`)
- Uma conta gratuita no [MongoDB Atlas](https://www.mongodb.com/cloud/atlas) (ver seção 5)
- Acesso à internet para baixar dependências e acessar o Atlas
- Git (para clonar/publicar)

## 4. Estrutura do projeto

```
login-seguro/
├── pom.xml
├── .env.example                      # modelo das variáveis de ambiente (valores ilustrativos)
├── .gitignore                        # ignora .env, target/, IDEs etc.
├── README.md
├── docs/CHECKLIST_REQUISITOS.md      # requisito -> implementação -> arquivos + estado da verificação
└── src/
    ├── main/
    │   ├── java/com/example/loginseguro/
    │   │   ├── LoginSeguroApplication.java      # ponto de entrada
    │   │   ├── config/InitialUsersSeeder.java   # atribuição inicial de ADMIN/MODERATOR
    │   │   ├── controller/                      # controladores MVC
    │   │   │   ├── AuthController.java          #   /login (tela) e /register
    │   │   │   ├── AreaController.java          #   /, /user, /moderator, /admin
    │   │   │   ├── GlobalModelAttributes.java   #   flags de menu para as views
    │   │   │   └── GlobalExceptionHandler.java  #   falha de banco -> página 503
    │   │   ├── dto/RegistrationForm.java        # formulário + regras de validação
    │   │   ├── model/{AppUser,Role}.java        # documento "users" e perfis
    │   │   ├── repository/AppUserRepository.java
    │   │   ├── security/
    │   │   │   ├── SecurityConfig.java          #   regras de acesso, login, logout, sessão, CSP, BCrypt
    │   │   │   ├── AppUserDetailsService.java   #   carrega usuário do MongoDB para o Spring Security
    │   │   │   ├── LoginFailureHandler.java     #   credencial inválida x falha interna
    │   │   │   └── AuthenticationUtils.java
    │   │   └── service/                         # regras de negócio (UserService)
    │   └── resources/
    │       ├── application.yml                  # configuração (usa variáveis de ambiente)
    │       ├── application-prod.yml             # ajustes de produção
    │       ├── static/css/theme.css             # TEMA: só variáveis (cores, fontes, medidas)
    │       ├── static/css/app.css               # componentes (usa as variáveis do tema)
    │       ├── static/js/register.js            # validação de conveniência no navegador
    │       └── templates/
    │           ├── layout/base.html             # layout compartilhado
    │           ├── fragments/{header,footer}.html
    │           ├── auth/{login,register}.html
    │           ├── home.html
    │           ├── user/index.html  moderator/index.html  admin/index.html
    │           └── error/{403,404,database}.html  error.html
    └── test/java/com/example/loginseguro/
        ├── dto/RegistrationFormValidationTest.java   # sem Spring, sem banco
        ├── service/UserServiceTest.java              # sem banco (repositório simulado)
        ├── security/SecurityWebTest.java             # camada web + Security, sem banco
        └── MongoIntegrationTest.java                 # exige um MongoDB real (TEST_MONGODB_URI)
```

## 5. Configurando o MongoDB Atlas

Os nomes de menus do Atlas mudam de tempos em tempos; se algo estiver em outro lugar, procure pelo equivalente.

1. **Conta e projeto:** crie uma conta em <https://www.mongodb.com/cloud/atlas> e um projeto (por exemplo, `login-seguro`).
2. **Cluster:** em *Create / Build a Database*, escolha o plano gratuito (**M0**), um provedor e uma região próxima, e crie o cluster (leva alguns minutos).
3. **Usuário de banco de dados** (diferente da sua conta Atlas): em *Security → Database Access → Add New Database User*:
   - método *Password*, defina nome de usuário e uma senha forte (use o gerador do Atlas);
   - em *Database User Privileges*, prefira o privilégio mínimo: *Specific Privileges → `readWrite` no banco `login_seguro`* (o mesmo valor de `MONGODB_DATABASE`);
   - evite senhas com caracteres especiais, ou use a versão URL-encoded delas na URI.
4. **Acesso de rede:** em *Security → Network Access → Add IP Address*, adicione o IP de onde a aplicação vai rodar (*Add Current IP Address* para desenvolvimento). Evite `0.0.0.0/0` (qualquer IP), a não ser temporariamente e consciente do risco.
5. **URI de conexão:** em *Database → Connect → Drivers* (Java), copie a URI `mongodb+srv://...` e troque `<db_password>` pela senha do usuário criado no passo 3.
6. Coloque a URI no arquivo `.env` (próxima seção). **Nunca** a coloque no código nem no GitHub.

O banco e as coleções (`users`, `sessions`) são criados automaticamente no primeiro uso, assim como o índice único de e-mail e o índice TTL das sessões.

## 6. Variáveis de ambiente

| Variável | Obrigatória | Descrição |
|---|---|---|
| `MONGODB_URI` | **Sim** | URI de conexão do Atlas |
| `MONGODB_DATABASE` | Não (padrão `login_seguro`) | Nome do banco |
| `BOOTSTRAP_ADMIN_NAME` / `_EMAIL` / `_PASSWORD` | Não | Conta inicial de administrador (ver seção 9) |
| `BOOTSTRAP_MODERATOR_NAME` / `_EMAIL` / `_PASSWORD` | Não | Conta inicial de moderador |
| `SPRING_PROFILES_ACTIVE` | Não | Use `prod` em produção (cookie `Secure`, proxy reverso) |
| `TEST_MONGODB_URI` | Só para testes de integração | URI de um MongoDB de **teste** |

Duas formas de configurar (a aplicação aceita as duas):

```bash
# Forma 1: arquivo .env na raiz do projeto (já está no .gitignore)
cp .env.example .env
# edite o .env com seus valores reais

# Forma 2: variáveis de ambiente do sistema (têm precedência sobre o .env)
export MONGODB_URI='mongodb+srv://USUARIO:SENHA@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority&serverSelectionTimeoutMS=5000'
export MONGODB_DATABASE='login_seguro'
```

Se `MONGODB_URI` não estiver definida, a aplicação **não inicia** (propositalmente, para não conectar em silêncio a um banco local).

## 7. Executando localmente

```bash
git clone <URL-DO-SEU-REPOSITORIO>
cd login-seguro
cp .env.example .env        # edite o .env
mvn spring-boot:run
```

Acesse <http://localhost:8080>. Você será levado à tela de login; use *Cadastrar* para criar uma conta.

Para gerar o JAR: `mvn clean package` e depois `java -jar target/login-seguro-1.0.0.jar` (execute na raiz do projeto para que o `.env` seja encontrado, ou use variáveis de ambiente).

Se o Atlas estiver inacessível ou as credenciais estiverem erradas, a aplicação falha na inicialização (a conexão é verificada ao criar os índices) e mostra o erro no console. Confira usuário/senha, a lista de IPs liberados e a URI.

## 8. Como funcionam autenticação, autorização, sessões e senhas

### Cadastro e validação
- O formulário (`RegistrationForm`) é validado **no servidor** com Bean Validation: nome obrigatório (até 100), e-mail em formato válido (até 254), senha de 8 a 64 caracteres com ao menos uma letra e um número, e confirmação igual à senha.
- No navegador há validação de conveniência (atributos HTML5 e `register.js`), mas o servidor nunca confia nela.
- **E-mail:** é normalizado (espaços removidos, Unicode NFKC, minúsculas) no cadastro **e** no login. A unicidade é garantida por um **índice único** no MongoDB (`@Indexed(unique = true)`): mesmo duas requisições simultâneas não criam e-mails duplicados (a exceção do banco é convertida em mensagem amigável).

### Senhas
- Armazenadas **somente** como hash **BCrypt** (com sal embutido; custo configurável em `app.security.bcrypt-strength`, padrão 12).
- A senha em texto puro nunca é salva, registrada em log, nem reenviada ao navegador (o campo de senha não é reexibido após erro). O hash também não aparece em páginas, `toString()` ou mensagens de erro.

### Login e logout
- `POST /login` é processado pelo Spring Security (campos `email` e `password`).
- Credenciais inválidas e usuário inexistente geram **a mesma mensagem** ("E-mail ou senha inválidos"). Se o banco estiver fora do ar, aparece "serviço indisponível" (e não "senha inválida").
- `POST /logout` (protegido por CSRF) invalida a sessão no servidor, limpa a autenticação e remove o cookie.

### Sessões (persistidas no MongoDB Atlas)
- **Implementado de fato:** a dependência `spring-session-data-mongodb` está no `pom.xml` e é auto-configurada pelo Spring Boot. As sessões HTTP ficam na coleção **`sessions`** do mesmo banco, com expiração por índice TTL (30 minutos de inatividade: `spring.session.timeout`).
- O contexto de segurança (usuário e perfis) é gravado na sessão por serialização Java; por isso a aplicação usa o `User` padrão do Spring Security como principal (é serializável).
- O navegador recebe só um cookie `SESSION` com o identificador. Configuração do cookie: `HttpOnly`, `SameSite=Lax`, e `Secure` no perfil `prod`.
- **Fixação de sessão:** o ID da sessão é trocado ao autenticar (`changeSessionId`).
- Efeito prático: reiniciar a aplicação não derruba os logins, e várias instâncias podem compartilhar as sessões.
- A persistência de **sessões** é independente da de **usuários** (coleções `sessions` e `users`, mecanismos diferentes).

### Autorização
Em `SecurityConfig`, de forma explícita e restritiva (`anyRequest().denyAll()` ao final):

| Rota | Quem acessa |
|---|---|
| `/login`, `/register`, `/css/**`, `/js/**`, `/error` | Público |
| `/` | Qualquer usuário autenticado |
| `/user/**` | `ROLE_USER` |
| `/moderator/**` | `ROLE_MODERATOR` |
| `/admin/**` | `ROLE_ADMIN` |
| Qualquer outra rota | Negada |

- Não autenticado em rota protegida → redirecionado para `/login`.
- Autenticado sem permissão → **HTTP 403** com a página `error/403.html` ("Acesso negado").
- A checagem é feita no servidor em toda requisição; esconder um link no menu é só conveniência de interface.
- **CSRF** está ativo: todo formulário `POST` (cadastro, login, logout) leva o token oculto (inserido automaticamente pelo `th:action`). Sem token, a resposta é 403.
- Cabeçalho `Content-Security-Policy` restrito ao próprio servidor (sem scripts nem estilos inline).

## 9. Perfis, atribuição inicial e como testar

| Perfil | Permissões | Como é obtido |
|---|---|---|
| `ROLE_USER` | Área do usuário (`/user`) | Automaticamente em todo cadastro público |
| `ROLE_MODERATOR` | Área do moderador (`/moderator`) | Atribuição inicial (abaixo). A conta criada recebe `USER` + `MODERATOR` |
| `ROLE_ADMIN` | Área do administrador (`/admin`) | Atribuição inicial (abaixo). A conta criada recebe `USER` + `MODERATOR` + `ADMIN` |

Cada área exige exatamente o seu perfil; por isso o administrador recebe os três perfis, para poder visitar todas as áreas.

### Como os perfis privilegiados são atribuídos
1. O **cadastro público nunca aceita perfil**: `RegistrationForm` nem tem esse campo, o controlador só aceita `name`, `email`, `password` e `confirmPassword`, e o serviço sempre cria `ROLE_USER`.
2. **Contas iniciais por variáveis de ambiente:** defina `BOOTSTRAP_ADMIN_EMAIL`/`_PASSWORD` (e/ou `BOOTSTRAP_MODERATOR_*`) antes de iniciar. Na inicialização, `InitialUsersSeeder` cria a conta **somente se aquele e-mail ainda não existir**. Ele nunca promove um usuário existente nem altera senhas. Senha com no mínimo 8 caracteres. As variáveis podem ser removidas depois do primeiro uso.
3. **Alternativa manual:** no Atlas (*Browse Collections → `users`*), edite o campo `roles` do usuário, por exemplo para `["ROLE_USER", "ROLE_MODERATOR"]`. O usuário precisa fazer login de novo.

### Roteiro manual de teste das permissões
1. Defina `BOOTSTRAP_ADMIN_*` e `BOOTSTRAP_MODERATOR_*` no `.env` e inicie a aplicação.
2. Cadastre um usuário comum em `/register` e faça login: a página inicial mostra `ROLE_USER`; `/user` abre; `/moderator` e `/admin` retornam "Acesso negado" (403).
3. Saia, entre como moderador: `/user` e `/moderator` abrem; `/admin` retorna 403.
4. Saia, entre como administrador: as três áreas abrem.
5. Sem login, acesse `/admin` diretamente: você é levado a `/login`.
6. Tente cadastrar de novo o mesmo e-mail (inclusive com maiúsculas): o sistema recusa.

## 10. Testes

```bash
mvn test
```

| Tipo | Classe | Precisa de banco? |
|---|---|---|
| Validação do cadastro | `RegistrationFormValidationTest` | Não |
| Regras de serviço (normalização, hash, duplicidade, perfil único no cadastro) | `UserServiceTest` | Não |
| Autorização por rota, redirecionamento ao login, 403, CSRF, formulários | `SecurityWebTest` | Não |
| Persistência, e-mail duplicado, hash no banco, login/logout, sessões no MongoDB, perfis iniciais, mass assignment | `MongoIntegrationTest` | **Sim** |

O `MongoIntegrationTest` só roda se `TEST_MONGODB_URI` estiver definida; sem ela, o JUnit o marca como **ignorado**. Ele cria um banco temporário próprio (`login_seguro_test_<uuid>`) e o apaga no final, sem tocar no banco da aplicação.

```bash
# Contra um MongoDB local descartável (requer Docker)
docker run -d --name mongo-test -p 27017:27017 mongo:7
TEST_MONGODB_URI='mongodb://localhost:27017' mvn test

# Ou contra o Atlas (o usuário precisa poder criar/apagar bancos, ex.: "Read and write to any database")
TEST_MONGODB_URI='mongodb+srv://USUARIO:SENHA@cluster0.xxxxx.mongodb.net/' mvn test
```

## 11. Adaptando o tema visual sem mexer na lógica

A interface foi separada em camadas para que o visual possa mudar sem tocar em segurança, autorização ou persistência:

1. **Cores, fontes e medidas:** edite `static/css/theme.css` (somente variáveis CSS). É o jeito mais rápido de trocar a identidade visual.
2. **Aparência dos componentes:** `static/css/app.css` usa apenas as variáveis do tema. Para um tema totalmente novo, crie outro arquivo CSS e troque o `<link>` em `templates/layout/base.html`.
3. **Estrutura do site:** `templates/layout/base.html` (esqueleto), `fragments/header.html` e `fragments/footer.html` (cabeçalho, menu, rodapé) são compartilhados por todas as páginas. Mudar esses arquivos muda o site inteiro.
4. **Novas páginas:** copie uma página existente (ex.: `user/index.html`), mantenha a primeira linha `th:replace="~{layout/base :: layout(...)}"`, crie o método no controlador e adicione a regra de acesso em `SecurityConfig`.
5. Os templates não contêm regras de segurança: só leem flags simples preparadas em Java (`authenticated`, `canAccessUser` etc.).
6. Dica: durante o desenvolvimento do tema, rode com `--spring.thymeleaf.cache=false` para ver as mudanças sem reiniciar (`mvn spring-boot:run -Dspring-boot.run.arguments=--spring.thymeleaf.cache=false`).
7. O CSP só permite CSS e JS do próprio servidor. Se o novo tema usar fontes ou CDNs externos, ajuste a política em `SecurityConfig`.

## 12. Principais decisões de design

- **Sem camadas desnecessárias:** controlador → serviço → repositório; sem interfaces de serviço ou DTOs além do formulário.
- **Negar por padrão:** qualquer rota nova só funciona depois de ser liberada de forma explícita em `SecurityConfig`.
- **E-mail como identificador de login**, normalizado, com índice único no banco.
- **Contas privilegiadas só pelo operador** (variáveis de ambiente ou edição direta no banco), nunca pela interface pública.
- **Sessões no MongoDB** para sobreviverem a reinícios e permitirem várias instâncias.
- **Falhas de banco** viram página 503 amigável e log sem dados sensíveis.
- **Layout Thymeleaf por fragmentos** + CSS com variáveis para troca de tema barata.

## 13. Limitações conhecidas e requisitos para produção

**Limitações (escopo intencional ou consequência dele):**
- Sem recuperação de senha, verificação de e-mail, 2FA ou login social.
- Sem limite de tentativas de login/bloqueio de conta. Em produção, adicione proteção contra força bruta (por exemplo, no proxy/WAF ou com um mecanismo de limitação).
- O cadastro informa quando um e-mail já existe (necessário para a mensagem clara pedida), o que permite descobrir e-mails cadastrados; sem verificação por e-mail isso é inerente.
- Não há tela para gerenciar perfis de usuários existentes (use o Atlas).
- As sessões usam serialização Java: se a classe do principal mudar entre versões, sessões antigas podem ser invalidadas após um deploy.
- Se o MongoDB cair com a aplicação em execução, o armazenamento de sessões também fica indisponível e as requisições falham até a conexão voltar.

**Para produção:**
- Rodar atrás de **HTTPS** e usar `SPRING_PROFILES_ACTIVE=prod` (cookie `Secure`, cabeçalhos de proxy). Configure o proxy para enviar `X-Forwarded-Proto`.
- Guardar `MONGODB_URI` e as credenciais iniciais em um gerenciador de segredos/variáveis da plataforma, nunca no repositório.
- Atlas: IPs de acesso restritos, usuário de banco com privilégio mínimo, backups ativados, plano adequado (o M0 gratuito é só para desenvolvimento).
- Remover `BOOTSTRAP_*` após criar as contas iniciais e trocar essas senhas.
- Manter Spring Boot e dependências atualizados.
- Monitorar logs (a aplicação não registra senhas, hashes nem e-mails).

## 14. Publicando no GitHub

Confira antes que o `.env` não será enviado (`git status` não deve listá-lo; o `.gitignore` já o ignora).

```bash
cd login-seguro
git init
git add .
git status                      # confirme: nada de .env nem target/
git commit -m "Sistema de login seguro com Spring Boot, Thymeleaf e MongoDB Atlas"
git branch -M main

# Crie o repositório PÚBLICO vazio no GitHub (site: New repository) e então:
git remote add origin https://github.com/SEU-USUARIO/login-seguro.git
git push -u origin main
```

Com a GitHub CLI: `gh repo create login-seguro --public --source=. --remote=origin --push`.
