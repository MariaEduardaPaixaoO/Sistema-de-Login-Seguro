package com.example.loginseguro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.EnumSet;
import java.util.UUID;

import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.loginseguro.model.AppUser;
import com.example.loginseguro.model.Role;
import com.example.loginseguro.repository.AppUserRepository;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import jakarta.servlet.http.Cookie;

/**
 * Teste de INTEGRAÇÃO com MongoDB real (Atlas, mongod local ou container).
 *
 * Só roda se a variável de ambiente TEST_MONGODB_URI estiver definida; caso contrário
 * o JUnit marca a classe como "ignorada" (não como aprovada). Cada execução usa um
 * banco temporário próprio (login_seguro_test_<uuid>) que é apagado ao final.
 * Nunca aponte para o banco da aplicação: o banco de teste é separado por construção.
 */
@SpringBootTest(properties = {
        "app.security.bcrypt-strength=4",
        "app.bootstrap.admin.name=Admin Teste",
        "app.bootstrap.admin.email=admin.teste@example.com",
        "app.bootstrap.admin.password=SenhaAdmin123",
        "app.bootstrap.moderator.name=Moderador Teste",
        "app.bootstrap.moderator.email=moderador.teste@example.com",
        "app.bootstrap.moderator.password=SenhaMod12345"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "TEST_MONGODB_URI", matches = ".+")
class MongoIntegrationTest {

    private static final String TEST_DATABASE =
            "login_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> System.getenv("TEST_MONGODB_URI"));
        registry.add("spring.data.mongodb.database", () -> TEST_DATABASE);
    }

    @AfterAll
    static void dropTemporaryDatabase() {
        String uri = System.getenv("TEST_MONGODB_URI");
        if (uri != null && !uri.isBlank()) {
            try (MongoClient client = MongoClients.create(uri)) {
                client.getDatabase(TEST_DATABASE).drop();
            }
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static String uniqueEmail() {
        return "usuario." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    private void register(String name, String email, String password) throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("name", name)
                        .param("email", email)
                        .param("password", password)
                        .param("confirmPassword", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));
    }

    /** Faz login pelo formulário e devolve o cookie de sessão (sessão guardada no MongoDB). */
    private Cookie login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(formLogin("/login").user("email", email).password("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("SESSION");
        assertThat(cookie).as("cookie de sessão SESSION").isNotNull();
        return cookie;
    }

    // ---------- Conexão ----------

    @Test
    void mongoConnectionWorks() {
        Document result = mongoTemplate.executeCommand("{ ping: 1 }");
        assertThat(((Number) result.get("ok")).doubleValue()).isEqualTo(1.0);
    }

    // ---------- Cadastro e persistência ----------

    @Test
    void validRegistrationPersistsUserWithHashedPassword() throws Exception {
        String email = uniqueEmail();
        String rawPassword = "SenhaSegura123";

        register("Nova Pessoa", "  " + email.toUpperCase() + " ", rawPassword);

        AppUser saved = repository.findByEmail(email).orElseThrow();
        assertThat(saved.getName()).isEqualTo("Nova Pessoa");
        assertThat(saved.getRoles()).containsExactly(Role.ROLE_USER);
        assertThat(saved.getPasswordHash()).isNotEqualTo(rawPassword).startsWith("$2");
        assertThat(passwordEncoder.matches(rawPassword, saved.getPasswordHash())).isTrue();

        // A senha em texto puro não pode aparecer em nenhum documento persistido.
        String rawDocument = mongoTemplate.getCollection("users").find(new Document("email", email))
                .first().toJson();
        assertThat(rawDocument).doesNotContain(rawPassword);
    }

    @Test
    void duplicateEmailIsRejectedIgnoringCaseAndSpaces() throws Exception {
        String email = uniqueEmail();
        register("Primeira", email, "SenhaSegura123");

        mockMvc.perform(post("/register").with(csrf())
                        .param("name", "Segunda")
                        .param("email", " " + email.toUpperCase())
                        .param("password", "OutraSenha123")
                        .param("confirmPassword", "OutraSenha123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(content().string(containsString("Este e-mail já está cadastrado.")));

        assertThat(mongoTemplate.getCollection("users").countDocuments(new Document("email", email)))
                .isEqualTo(1L);
    }

    @Test
    void databaseUniqueIndexRejectsDuplicateEvenBypassingTheService() {
        String email = uniqueEmail();
        mongoTemplate.insert(new AppUser("A", email, "hash-1", EnumSet.of(Role.ROLE_USER)));

        assertThatThrownBy(() -> mongoTemplate.insert(new AppUser("B", email, "hash-2", EnumSet.of(Role.ROLE_USER))))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void invalidDataIsRejectedAndNothingIsPersisted() throws Exception {
        String email = uniqueEmail();
        long before = repository.count();

        mockMvc.perform(post("/register").with(csrf())
                        .param("name", "Fulano")
                        .param("email", email)
                        .param("password", "fraca")
                        .param("confirmPassword", "fraca"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));

        assertThat(repository.count()).isEqualTo(before);
        assertThat(repository.findByEmail(email)).isEmpty();
    }

    @Test
    void publicRegistrationCannotAssignPrivilegedRoles() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(post("/register").with(csrf())
                        .param("name", "Espertinho")
                        .param("email", email)
                        .param("password", "SenhaSegura123")
                        .param("confirmPassword", "SenhaSegura123")
                        .param("roles", "ROLE_ADMIN")
                        .param("roles[0]", "ROLE_ADMIN")
                        .param("role", "ROLE_ADMIN")
                        .param("authorities", "ROLE_ADMIN"))
                .andExpect(redirectedUrl("/login?registered"));

        assertThat(repository.findByEmail(email).orElseThrow().getRoles()).containsExactly(Role.ROLE_USER);
    }

    // ---------- Login, sessão e logout ----------

    @Test
    void loginWithValidCredentialsWorksAndSessionIsStoredInMongo() throws Exception {
        String email = uniqueEmail();
        register("Pessoa Login", email, "SenhaSegura123");

        login(email, "SenhaSegura123");

        assertThat(mongoTemplate.getCollection("sessions").countDocuments())
                .as("sessões persistidas na coleção 'sessions'").isGreaterThan(0L);
    }

    @Test
    void loginWithWrongPasswordOrUnknownUserIsRejectedWithSameMessage() throws Exception {
        String email = uniqueEmail();
        register("Pessoa", email, "SenhaSegura123");

        mockMvc.perform(formLogin("/login").user("email", email).password("password", "SenhaErrada999"))
                .andExpect(redirectedUrl("/login?error"));

        mockMvc.perform(formLogin("/login").user("email", "naoexiste@example.com").password("password", "Qualquer123"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void emailLoginIsCaseInsensitive() throws Exception {
        String email = uniqueEmail();
        register("Pessoa", email, "SenhaSegura123");

        mockMvc.perform(formLogin("/login").user("email", email.toUpperCase()).password("password", "SenhaSegura123"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        String email = uniqueEmail();
        register("Pessoa", email, "SenhaSegura123");
        Cookie session = login(email, "SenhaSegura123");

        mockMvc.perform(get("/user").cookie(session)).andExpect(status().isOk());

        mockMvc.perform(post("/logout").with(csrf()).cookie(session))
                .andExpect(redirectedUrl("/login?logout"));

        // O mesmo cookie não dá mais acesso: a sessão foi invalidada.
        mockMvc.perform(get("/user").cookie(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    // ---------- Autorização com sessões reais ----------

    @Test
    void regularUserAccessesOnlyUserArea() throws Exception {
        String email = uniqueEmail();
        register("Pessoa", email, "SenhaSegura123");
        Cookie session = login(email, "SenhaSegura123");

        mockMvc.perform(get("/user").cookie(session)).andExpect(status().isOk());
        mockMvc.perform(get("/moderator").cookie(session)).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin").cookie(session)).andExpect(status().isForbidden());
    }

    @Test
    void seededModeratorAccessesModeratorButNotAdmin() throws Exception {
        assertThat(repository.findByEmail("moderador.teste@example.com").orElseThrow().getRoles())
                .containsExactlyInAnyOrder(Role.ROLE_USER, Role.ROLE_MODERATOR);

        Cookie session = login("moderador.teste@example.com", "SenhaMod12345");

        mockMvc.perform(get("/user").cookie(session)).andExpect(status().isOk());
        mockMvc.perform(get("/moderator").cookie(session)).andExpect(status().isOk());
        mockMvc.perform(get("/admin").cookie(session)).andExpect(status().isForbidden());
    }

    @Test
    void seededAdminAccessesAllAreas() throws Exception {
        assertThat(repository.findByEmail("admin.teste@example.com").orElseThrow().getRoles())
                .containsExactlyInAnyOrder(Role.ROLE_USER, Role.ROLE_MODERATOR, Role.ROLE_ADMIN);

        Cookie session = login("admin.teste@example.com", "SenhaAdmin123");

        mockMvc.perform(get("/user").cookie(session)).andExpect(status().isOk());
        mockMvc.perform(get("/moderator").cookie(session)).andExpect(status().isOk());
        mockMvc.perform(get("/admin").cookie(session)).andExpect(status().isOk());
    }

    @Test
    void passwordHashIsNeverRenderedInPages() throws Exception {
        String email = uniqueEmail();
        register("Pessoa", email, "SenhaSegura123");
        Cookie session = login(email, "SenhaSegura123");

        String hash = repository.findByEmail(email).orElseThrow().getPasswordHash();
        String page = mockMvc.perform(get("/").cookie(session)).andReturn().getResponse().getContentAsString();

        assertThat(page).doesNotContain(hash).doesNotContain("$2a$").doesNotContain("SenhaSegura123");
    }
}
