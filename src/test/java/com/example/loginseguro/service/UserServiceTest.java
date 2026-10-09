package com.example.loginseguro.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.example.loginseguro.dto.RegistrationForm;
import com.example.loginseguro.model.AppUser;
import com.example.loginseguro.model.Role;
import com.example.loginseguro.repository.AppUserRepository;

import java.util.EnumSet;

/** Teste automatizado sem banco: o repositório é simulado (Mockito), o BCrypt é real. */
class UserServiceTest {

    private AppUserRepository repository;
    private BCryptPasswordEncoder encoder;
    private UserService service;

    @BeforeEach
    void setUp() {
        repository = org.mockito.Mockito.mock(AppUserRepository.class);
        encoder = new BCryptPasswordEncoder(4); // custo baixo só para acelerar o teste
        service = new UserService(repository, encoder);
        when(repository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static RegistrationForm form(String email) {
        RegistrationForm form = new RegistrationForm();
        form.setName("  Ana Souza  ");
        form.setEmail(email);
        form.setPassword("Senha1234");
        form.setConfirmPassword("Senha1234");
        return form;
    }

    @Test
    void normalizeEmailTrimsAndLowercases() {
        assertThat(UserService.normalizeEmail("  Ana.Souza@Example.COM ")).isEqualTo("ana.souza@example.com");
        assertThat(UserService.normalizeEmail(null)).isNull();
    }

    @Test
    void registerUserStoresNormalizedEmailHashedPasswordAndOnlyRoleUser() {
        AppUser saved = service.registerUser(form("  Ana@Example.COM "));

        assertThat(saved.getEmail()).isEqualTo("ana@example.com");
        assertThat(saved.getName()).isEqualTo("Ana Souza");
        assertThat(saved.getRoles()).containsExactly(Role.ROLE_USER);
        assertThat(saved.getPasswordHash()).isNotEqualTo("Senha1234").startsWith("$2");
        assertThat(encoder.matches("Senha1234", saved.getPasswordHash())).isTrue();
        assertThat(saved.toString()).doesNotContain(saved.getPasswordHash());
    }

    @Test
    void duplicateEmailIsRejectedBeforeSaving() {
        when(repository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registerUser(form("ANA@example.com")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void uniqueIndexViolationIsTranslatedToDomainException() {
        when(repository.existsByEmail("ana@example.com")).thenReturn(false);
        when(repository.save(any(AppUser.class))).thenThrow(new DuplicateKeyException("E11000"));

        assertThatThrownBy(() -> service.registerUser(form("ana@example.com")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void createUserKeepsRequestedRolesForInitialAssignment() {
        service.createUser("Admin", "admin@example.com", "SenhaAdmin1",
                EnumSet.of(Role.ROLE_USER, Role.ROLE_ADMIN));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getRoles()).containsExactlyInAnyOrder(Role.ROLE_USER, Role.ROLE_ADMIN);
    }
}
