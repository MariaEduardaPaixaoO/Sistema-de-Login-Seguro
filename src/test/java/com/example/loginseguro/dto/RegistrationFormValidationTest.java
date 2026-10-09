package com.example.loginseguro.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/** Teste automatizado puro (sem Spring, sem banco): regras de validação do cadastro. */
class RegistrationFormValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static RegistrationForm form(String name, String email, String password, String confirm) {
        RegistrationForm form = new RegistrationForm();
        form.setName(name);
        form.setEmail(email);
        form.setPassword(password);
        form.setConfirmPassword(confirm);
        return form;
    }

    private static Set<String> invalidProperties(RegistrationForm form) {
        return validator.validate(form).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    void validFormHasNoViolations() {
        assertThat(validator.validate(form("Ana Souza", "ana@example.com", "Senha1234", "Senha1234"))).isEmpty();
    }

    @Test
    void blankFieldsAreRejected() {
        assertThat(invalidProperties(form("", "", "", "")))
                .contains("name", "email", "password", "confirmPassword");
    }

    @Test
    void malformedEmailsAreRejected() {
        for (String email : new String[] { "sem-arroba", "a@b", "a b@example.com", "@example.com", "ana@" }) {
            assertThat(invalidProperties(form("Ana", email, "Senha1234", "Senha1234")))
                    .as("e-mail %s deveria ser inválido", email)
                    .contains("email");
        }
    }

    @Test
    void weakPasswordsAreRejected() {
        assertThat(invalidProperties(form("Ana", "ana@example.com", "curta1", "curta1"))).contains("password");
        assertThat(invalidProperties(form("Ana", "ana@example.com", "somenteletras", "somenteletras")))
                .contains("password");
        assertThat(invalidProperties(form("Ana", "ana@example.com", "123456789", "123456789")))
                .contains("password");
    }

    @Test
    void passwordConfirmationMustMatch() {
        assertThat(invalidProperties(form("Ana", "ana@example.com", "Senha1234", "Outra1234")))
                .contains("passwordConfirmed");
    }

    @Test
    void passwordBeyondBcryptByteLimitIsRejected() {
        // 40 caracteres de 2 bytes cada = 80 bytes (> 72) mas apenas 40 caracteres.
        String longInBytes = "1" + "é".repeat(39);
        assertThat(invalidProperties(form("Ana", "ana@example.com", longInBytes, longInBytes)))
                .contains("passwordWithinHashLimit");
    }

    @Test
    void tooLongNameIsRejected() {
        assertThat(invalidProperties(form("x".repeat(101), "ana@example.com", "Senha1234", "Senha1234")))
                .contains("name");
    }
}
