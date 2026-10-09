package com.example.loginseguro.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.loginseguro.controller.AreaController;
import com.example.loginseguro.controller.AuthController;
import com.example.loginseguro.service.EmailAlreadyRegisteredException;
import com.example.loginseguro.service.UserService;

/**
 * Testes de autorização, CSRF e formulários SEM banco de dados: carrega só a camada web
 * (controladores + Spring Security + Thymeleaf) com o serviço simulado.
 */
@WebMvcTest(controllers = { AreaController.class, AuthController.class },
        properties = "app.security.bcrypt-strength=4")
@Import(SecurityConfig.class)
class SecurityWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    // Evita que o Spring Boot crie um usuário em memória com senha gerada neste teste.
    @MockitoBean
    private UserDetailsService userDetailsService;

    // ---------- Não autenticado ----------

    @Test
    void anonymousUserIsRedirectedToLoginOnProtectedPages() throws Exception {
        for (String path : new String[] { "/", "/user", "/moderator", "/admin" }) {
            mockMvc.perform(get(path))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("http://localhost/login"));
        }
    }

    @Test
    void unknownRoutesAreDeniedByDefault() throws Exception {
        mockMvc.perform(get("/qualquer-outra-rota"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void loginAndRegisterPagesArePublic() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk()).andExpect(view().name("auth/login"));
        mockMvc.perform(get("/register")).andExpect(status().isOk()).andExpect(view().name("auth/register"));
    }

    // ---------- Autorização por perfil ----------

    @Test
    @WithMockUser(roles = "USER")
    void userCanAccessOnlyUserArea() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/user")).andExpect(status().isOk());
        mockMvc.perform(get("/moderator")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = { "USER", "MODERATOR" })
    void moderatorCanAccessModeratorButNotAdmin() throws Exception {
        mockMvc.perform(get("/moderator")).andExpect(status().isOk());
        mockMvc.perform(get("/admin")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = { "USER", "MODERATOR", "ADMIN" })
    void adminCanAccessAllAreas() throws Exception {
        mockMvc.perform(get("/user")).andExpect(status().isOk());
        mockMvc.perform(get("/moderator")).andExpect(status().isOk());
        mockMvc.perform(get("/admin")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void areaIsDecidedByTheRoleNotByTheVisibilityOfLinks() throws Exception {
        // Só ROLE_ADMIN (sem ROLE_USER): a rota /user continua protegida no servidor.
        mockMvc.perform(get("/user")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void navigationDoesNotShowAdminLinkToRegularUser() throws Exception {
        mockMvc.perform(get("/user"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("href=\"/admin\""))));
    }

    @Test
    @WithMockUser(roles = "USER")
    void authenticatedUserIsRedirectedAwayFromLoginAndRegister() throws Exception {
        mockMvc.perform(get("/login")).andExpect(redirectedUrl("/"));
        mockMvc.perform(get("/register")).andExpect(redirectedUrl("/"));
    }

    // ---------- CSRF e formulários ----------

    @Test
    void registerFormContainsCsrfToken() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void loginFormContainsCsrfToken() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void registerWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "Ana")
                        .param("email", "ana@example.com")
                        .param("password", "Senha1234")
                        .param("confirmPassword", "Senha1234"))
                .andExpect(status().isForbidden());
        verify(userService, never()).registerUser(any());
    }

    @Test
    void logoutWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/logout")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void logoutWithCsrfRedirectsToLoginWithMessage() throws Exception {
        mockMvc.perform(post("/logout").with(csrf())).andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void validRegistrationCallsServiceAndRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("name", "Ana Souza")
                        .param("email", "ana@example.com")
                        .param("password", "Senha1234")
                        .param("confirmPassword", "Senha1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));
        verify(userService).registerUser(any());
    }

    @Test
    void invalidRegistrationShowsFieldErrorsAndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("name", "")
                        .param("email", "nao-e-email")
                        .param("password", "fraca")
                        .param("confirmPassword", "diferente"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("form", "name", "email", "password", "passwordConfirmed"));
        verify(userService, never()).registerUser(any());
    }

    @Test
    void duplicateEmailShowsFriendlyErrorOnEmailField() throws Exception {
        when(userService.registerUser(any())).thenThrow(new EmailAlreadyRegisteredException());

        mockMvc.perform(post("/register").with(csrf())
                        .param("name", "Ana Souza")
                        .param("email", "ana@example.com")
                        .param("password", "Senha1234")
                        .param("confirmPassword", "Senha1234"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("form", "email"))
                .andExpect(content().string(containsString("Este e-mail já está cadastrado.")));
    }

    @Test
    void passwordIsNeverEchoedBackInTheForm() throws Exception {
        String response = mockMvc.perform(post("/register").with(csrf())
                        .param("name", "Ana Souza")
                        .param("email", "invalido")
                        .param("password", "Senha1234")
                        .param("confirmPassword", "Senha1234"))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain("Senha1234");
    }
}
