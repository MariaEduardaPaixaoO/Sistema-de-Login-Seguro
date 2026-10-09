# Checklist de requisitos e estado da verificação

## 1. Estado real da verificação (leia primeiro)

O projeto foi gerado em um ambiente **sem acesso ao Maven Central nem ao MongoDB Atlas** (a rede bloqueou `repo.maven.apache.org`). Por isso:

| Verificação | Estado |
|---|---|
| Sintaxe dos 20 arquivos `.java` (parser do `javac`, sem resolver dependências) | ✅ Executada, 0 erros |
| `pom.xml` bem formado (XML) | ✅ Executada |
| `application.yml` e `application-prod.yml` são YAML válido | ✅ Executada |
| Tags HTML dos templates balanceadas | ✅ Executada |
| **Compilação** (`mvn compile`) | ❌ **Não executada** (dependências indisponíveis) |
| **Testes automatizados** (`mvn test`) | ❌ **Não executados**: nenhum resultado de aprovação existe |
| **Integração com MongoDB Atlas** (conexão, persistência, sessões) | ❌ **Não executada** |
| Execução da aplicação e renderização real das páginas no navegador | ❌ **Não executada** |

Isto é, o código foi **escrito e revisado, mas não compilado nem testado**. Na primeira execução, rode nesta ordem:

1. `mvn test`: compila e roda os testes que não dependem de banco (`RegistrationFormValidationTest`, `UserServiceTest`, `SecurityWebTest`).
2. `TEST_MONGODB_URI=... mvn test`: roda também `MongoIntegrationTest` (ver README, seção 10).
3. `mvn spring-boot:run` com o `.env` configurado e o roteiro manual da seção 9 do README.

Pontos que merecem atenção se algo falhar (são os que dependem de comportamento de bibliotecas que não pude exercitar aqui): a versão `3.5.6` do Spring Boot (ajuste para a 3.5.x mais recente disponível), o carregamento do `.env` por `spring.config.import`, a gravação do cookie `SESSION` pelo Spring Session nos testes com `MockMvc`, e a renderização do layout por fragmentos do Thymeleaf.

## 2. Requisito original → implementação

### Enunciado original

| Requisito | Implementação | Arquivos |
|---|---|---|
| Cadastro, login e logout com segurança (hash, validação) | Cadastro com Bean Validation; login/logout do Spring Security; BCrypt | `AuthController`, `RegistrationForm`, `UserService`, `SecurityConfig` |
| Controle de acesso por perfil, mínimo de 3 perfis | `ROLE_USER`, `ROLE_MODERATOR`, `ROLE_ADMIN`; regras por rota | `Role`, `SecurityConfig` |
| Spring Security para autenticação e autorização | `SecurityFilterChain`, `UserDetailsService`, CSRF, sessão | `SecurityConfig`, `AppUserDetailsService` |
| MongoDB Atlas para usuários **e sessões** | Usuários em `users` (Spring Data); sessões em `sessions` (Spring Session MongoDB) | `AppUser`, `AppUserRepository`, `pom.xml`, `application.yml` |
| Interface Thymeleaf desacoplada da lógica | Templates sem regras; flags preparadas em Java | `templates/**`, `GlobalModelAttributes` |
| Escalabilidade para temas visuais | CSS de tema isolado, layout e fragmentos compartilhados | `theme.css`, `app.css`, `layout/base.html`, `fragments/*` |
| Documentação (estrutura, Atlas, decisões) | README completo | `README.md` |
| Código no GitHub com README | Repositório preparado; comandos de publicação | `README.md` seção 14, `.gitignore` |
| Código comentado, arquivo de configuração com conexão segura, instruções locais | Comentários nos pontos de segurança; conexão por `MONGODB_URI` | `application.yml`, `.env.example`, `README.md` |

### Instruções obrigatórias

| # | Requisito | Implementação | Arquivos |
|---|---|---|---|
| 1 | Somente o escopo pedido, sem extras | Sem recuperação de senha, 2FA, login social, APIs extras | (todo o projeto) |
| 2 | Tecnologias obrigatórias (Java/Spring Boot, Security, Thymeleaf, Atlas, Maven) | Todas usadas, sem substitutos | `pom.xml` |
| 3 | Validação de campos e formatos | Bean Validation + atributos HTML5 + `register.js` | `RegistrationForm`, `auth/register.html`, `register.js` |
| 3 | Senhas só como hash seguro | BCrypt, custo configurável | `SecurityConfig#passwordEncoder`, `UserService` |
| 3 | E-mail duplicado impedido (normalização + integridade) | `normalizeEmail`, `existsByEmail`, índice único, captura de `DuplicateKeyException` | `UserService`, `AppUser` |
| 3 | Mensagens claras sem expor dados sensíveis | Mensagem única para login inválido; `server.error.include-*: never` | `LoginFailureHandler`, `application.yml`, templates |
| 3 | Sessões gerenciadas pelo Spring Security, anti-fixação, logout correto | `changeSessionId`, `invalidateHttpSession`, `deleteCookies` | `SecurityConfig` |
| 4 | Três perfis, regras explícitas, páginas de demonstração | `/user`, `/moderator`, `/admin` | `SecurityConfig`, `AreaController`, `*/index.html` |
| 4 | Proteção no servidor; não autenticado vai ao login; sem permissão → acesso negado | `authorizeHttpRequests`, redirecionamento ao login, 403 | `SecurityConfig`, `error/403.html` |
| 4 | Cadastro público não atribui admin/moderador; mecanismo documentado | Formulário sem campo de perfil, `setAllowedFields`, serviço fixa `ROLE_USER`; seeder por variáveis de ambiente | `RegistrationForm`, `AuthController`, `UserService`, `InitialUsersSeeder`, `README.md` seção 9 |
| 5 | Atlas como banco principal; campos do usuário | `AppUser` (id, nome, e-mail, hash, perfis, data) | `AppUser` |
| 5 | Conexão por variáveis de ambiente; `.env.example`; `.gitignore` | `${MONGODB_URI}`; arquivo de exemplo; `.env` ignorado | `application.yml`, `.env.example`, `.gitignore` |
| 5 | Documentar cluster, usuário, rede, URI | README seção 5 | `README.md` |
| 5 | Tratar erros de conexão/persistência | Página 503 + log sem dados; falha rápida na inicialização | `GlobalExceptionHandler`, `error/database.html`, `LoginFailureHandler` |
| 5 | Sessões no Atlas só se realmente implementado; explicar | `spring-session-data-mongodb` + `spring.session.mongodb`; explicado no README | `pom.xml`, `application.yml`, `README.md` seção 8 |
| 6 | Páginas: cadastro, login, inicial, usuário, moderador, admin, acesso negado, erro | Todas criadas, com layout compartilhado | `templates/**` |
| 6 | Templates sem regras de negócio/segurança | Só leem flags e dados do modelo | `templates/**`, `GlobalModelAttributes` |
| 7 | Tema alterável sem tocar na lógica; sem painel de temas | CSS centralizado em variáveis; nenhum seletor de tema | `theme.css`, `app.css`, `README.md` seção 11 |
| 8 | Pacotes com responsabilidades claras; código comentado | `config`, `controller`, `dto`, `model`, `repository`, `security`, `service` | `src/main/java/**` |
| 9 | Regras restritivas, CSRF, hash, sem vazamento, cookies seguros, sem desabilitar segurança, sem segredos versionados, sem dados sensíveis em log | Negar por padrão; CSRF padrão ativo; cookie HttpOnly/SameSite/Secure(prod); CSP; logs só com tipo de exceção | `SecurityConfig`, `application*.yml`, `.gitignore`, `LoginFailureHandler`, `GlobalExceptionHandler` |
| 10 | Testes dos 11 itens mínimos | Ver tabela abaixo | `src/test/**` |
| 11 | README completo em português | 14 seções | `README.md` |
| 11 | Repositório pronto para publicar; comandos sem afirmar publicação | Seção 14 do README (publicação **não** foi feita) | `README.md`, `.gitignore` |

### Testes mínimos exigidos (item 10)

Todos **escritos**; **nenhum foi executado** (ver seção 1).

| Verificação exigida | Teste | Precisa de MongoDB? |
|---|---|---|
| Cadastro válido e persistência | `MongoIntegrationTest#validRegistrationPersistsUserWithHashedPassword` | Sim |
| Rejeição de dados inválidos | `RegistrationFormValidationTest`; `SecurityWebTest#invalidRegistration...`; `MongoIntegrationTest#invalidDataIsRejected...` | Parcial |
| Rejeição de e-mail duplicado | `UserServiceTest#duplicateEmail...`; `MongoIntegrationTest#duplicateEmailIsRejected...`, `#databaseUniqueIndex...` | Parcial |
| Senha com hash, sem texto puro | `UserServiceTest#registerUserStores...`; `MongoIntegrationTest#validRegistration...` (inspeciona o documento) | Parcial |
| Login válido / credenciais inválidas | `MongoIntegrationTest#loginWithValidCredentials...`, `#loginWithWrongPassword...`, `#emailLoginIsCaseInsensitive` | Sim |
| Logout e fim da sessão | `MongoIntegrationTest#logoutEndsTheSession`; `SecurityWebTest#logoutWithCsrf...` | Parcial |
| Acesso permitido por perfil | `SecurityWebTest` (USER/MODERATOR/ADMIN); `MongoIntegrationTest#seeded...`, `#regularUser...` | Parcial |
| Bloqueio a não autenticados | `SecurityWebTest#anonymousUserIsRedirectedToLoginOnProtectedPages`, `#unknownRoutesAreDenied...` | Não |
| Bloqueio de admin/moderação a perfis não autorizados | `SecurityWebTest`, `MongoIntegrationTest` (status 403) | Parcial |
| Sem atribuição indevida de privilégios no cadastro | `MongoIntegrationTest#publicRegistrationCannotAssignPrivilegedRoles`; `UserServiceTest#registerUserStores...` | Parcial |
| Integração com MongoDB Atlas | `MongoIntegrationTest#mongoConnectionWorks` e demais (sessões na coleção `sessions`) | Sim |
| Formulários Thymeleaf e CSRF | `SecurityWebTest#registerFormContainsCsrfToken`, `#loginFormContainsCsrfToken`, `#registerWithoutCsrfTokenIsForbidden`, `#logoutWithoutCsrfTokenIsForbidden` | Não |

**Observação sobre o item "Integração com MongoDB Atlas":** o `MongoIntegrationTest` aceita qualquer MongoDB em `TEST_MONGODB_URI`. Para comprovar especificamente o Atlas, aponte a variável para a URI do Atlas (ver README, seção 10).
