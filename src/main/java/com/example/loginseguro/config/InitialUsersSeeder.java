package com.example.loginseguro.config;

import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import com.example.loginseguro.dto.RegistrationForm;
import com.example.loginseguro.model.Role;
import com.example.loginseguro.service.EmailAlreadyRegisteredException;
import com.example.loginseguro.service.UserService;

/**
 * Atribuição INICIAL de perfis privilegiados.
 *
 * O cadastro público só cria ROLE_USER. Para existir um ADMIN e um MODERATOR,
 * o operador do sistema informa as credenciais por variáveis de ambiente
 * (BOOTSTRAP_ADMIN_* e BOOTSTRAP_MODERATOR_*). Na inicialização, cada conta é
 * criada apenas se ainda não existir um usuário com aquele e-mail:
 * - nunca promove um usuário já existente (ninguém ganha privilégio por cadastrar
 *   antes o e-mail configurado);
 * - nunca sobrescreve senha;
 * - a senha vem do ambiente, passa pelo BCrypt e nunca é registrada em log.
 *
 * Alternativa manual: editar o campo "roles" do usuário diretamente no Atlas
 * (ver README).
 */
@Component
public class InitialUsersSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialUsersSeeder.class);
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(RegistrationForm.EMAIL_REGEX);

    private final UserService userService;
    private final Account admin;
    private final Account moderator;

    public InitialUsersSeeder(UserService userService,
            @Value("${app.bootstrap.admin.name:}") String adminName,
            @Value("${app.bootstrap.admin.email:}") String adminEmail,
            @Value("${app.bootstrap.admin.password:}") String adminPassword,
            @Value("${app.bootstrap.moderator.name:}") String moderatorName,
            @Value("${app.bootstrap.moderator.email:}") String moderatorEmail,
            @Value("${app.bootstrap.moderator.password:}") String moderatorPassword) {
        this.userService = userService;
        this.admin = new Account(orDefault(adminName, "Administrador"), adminEmail, adminPassword);
        this.moderator = new Account(orDefault(moderatorName, "Moderador"), moderatorEmail, moderatorPassword);
    }

    @Override
    public void run(ApplicationArguments args) {
        // O administrador também recebe os perfis inferiores, para poder visitar todas as áreas.
        seed("ADMIN", admin, EnumSet.of(Role.ROLE_USER, Role.ROLE_MODERATOR, Role.ROLE_ADMIN));
        seed("MODERATOR", moderator, EnumSet.of(Role.ROLE_USER, Role.ROLE_MODERATOR));
    }

    private void seed(String label, Account account, Set<Role> roles) {
        if (account.email().isBlank() && account.password().isBlank()) {
            log.info("Conta inicial {} não configurada; nada a criar.", label);
            return;
        }
        if (!EMAIL_PATTERN.matcher(account.email().trim()).matches()
                || account.password().length() < MIN_PASSWORD_LENGTH) {
            log.warn("Configuração da conta inicial {} inválida (e-mail malformado ou senha com menos de {} caracteres); "
                    + "conta não criada.", label, MIN_PASSWORD_LENGTH);
            return;
        }
        try {
            userService.createUser(account.name(), account.email(), account.password(), roles);
            log.info("Conta inicial {} criada.", label);
        } catch (EmailAlreadyRegisteredException e) {
            log.info("Conta inicial {} já existe; nada foi alterado.", label);
        } catch (DataAccessException e) {
            log.warn("Não foi possível criar a conta inicial {} ({}).", label, e.getClass().getSimpleName());
        }
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /** Credenciais lidas do ambiente (valores nulos viram string vazia). */
    private record Account(String name, String email, String password) {
        Account {
            email = email == null ? "" : email;
            password = password == null ? "" : password;
        }
    }
}
