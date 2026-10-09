package com.example.loginseguro.dto;

import java.nio.charset.StandardCharsets;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dados do formulário de cadastro público.
 *
 * Esta classe NÃO possui campo de perfil (role) de propósito: o cadastro público
 * nunca pode escolher perfil. O controlador ainda restringe os campos aceitos no
 * binding (defesa em profundidade contra mass assignment).
 */
public class RegistrationForm {

    /** Exige "algo@dominio.tld" (o @Email do Jakarta aceita "a@b"). Reutilizada pelo seeder. */
    public static final String EMAIL_REGEX = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$";

    /** O BCrypt só considera os primeiros 72 bytes da senha. */
    private static final int BCRYPT_MAX_BYTES = 72;

    @NotBlank(message = "Informe o nome.")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
    private String name;

    @NotBlank(message = "Informe o e-mail.")
    @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.")
    @Email(message = "Informe um e-mail válido.")
    @Pattern(regexp = EMAIL_REGEX, message = "Informe um e-mail válido.")
    private String email;

    @NotBlank(message = "Informe a senha.")
    @Size(min = 8, max = 64, message = "A senha deve ter entre 8 e 64 caracteres.")
    @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).+$",
            message = "A senha deve conter ao menos uma letra e um número.")
    private String password;

    @NotBlank(message = "Confirme a senha.")
    private String confirmPassword;

    @AssertTrue(message = "As senhas não conferem.")
    public boolean isPasswordConfirmed() {
        // Se a senha está vazia, quem reporta o erro é o @NotBlank.
        return password == null || password.equals(confirmPassword);
    }

    @AssertTrue(message = "A senha é longa demais (máximo de 72 bytes).")
    public boolean isPasswordWithinHashLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
