package com.example.loginseguro.service;

import java.text.Normalizer;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.loginseguro.dto.RegistrationForm;
import com.example.loginseguro.model.AppUser;
import com.example.loginseguro.model.Role;
import com.example.loginseguro.repository.AppUserRepository;

/**
 * Regras de negócio de cadastro e consulta de usuários.
 */
@Service
public class UserService {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Normaliza o e-mail: remove espaços, aplica normalização Unicode (NFKC) e
     * converte para minúsculas. Assim "  Ana@Exemplo.COM " e "ana@exemplo.com"
     * são o mesmo usuário, tanto no cadastro quanto no login.
     */
    public static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return Normalizer.normalize(email.trim(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    /**
     * Cadastro público. Sempre cria o usuário somente com ROLE_USER: perfis
     * privilegiados nunca vêm do formulário.
     */
    public AppUser registerUser(RegistrationForm form) {
        return createUser(form.getName(), form.getEmail(), form.getPassword(), EnumSet.of(Role.ROLE_USER));
    }

    /**
     * Cria um usuário com os perfis informados. Fica público apenas para o
     * {@code InitialUsersSeeder} (atribuição inicial de perfis); nenhum controlador
     * deve chamá-lo com perfis vindos de requisição.
     *
     * @throws EmailAlreadyRegisteredException se o e-mail já existir
     */
    public AppUser createUser(String name, String email, String rawPassword, Set<Role> roles) {
        String normalizedEmail = normalizeEmail(email);

        // Checagem amigável; a garantia real é o índice único do MongoDB (ver catch abaixo).
        if (repository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }

        AppUser user = new AppUser(name.trim(), normalizedEmail, passwordEncoder.encode(rawPassword), roles);
        try {
            return repository.save(user);
        } catch (DuplicateKeyException e) {
            // Duas requisições simultâneas passaram pelo existsByEmail: o índice único barrou a segunda.
            throw new EmailAlreadyRegisteredException();
        }
    }

    public Optional<AppUser> findByEmail(String email) {
        return repository.findByEmail(normalizeEmail(email));
    }
}
