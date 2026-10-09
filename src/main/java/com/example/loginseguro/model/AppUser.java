package com.example.loginseguro.model;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Usuário persistido na coleção "users" do MongoDB.
 *
 * Contém apenas o necessário para cadastro e autenticação. Não há getter de
 * senha em texto puro (ela nunca é armazenada) e o hash não aparece em
 * {@link #toString()} para não vazar em logs.
 */
@Document(collection = "users")
public class AppUser {

    @Id
    private String id;

    private String name;

    /**
     * E-mail já normalizado (trim + minúsculas). O índice único garante, no banco,
     * que não existam dois usuários com o mesmo e-mail, mesmo sob concorrência.
     */
    @Indexed(unique = true)
    private String email;

    /** Hash BCrypt da senha. */
    private String passwordHash;

    private Set<Role> roles = new HashSet<>();

    private Instant createdAt;

    /** Usado pelo Spring Data para reconstruir o objeto a partir do banco. */
    protected AppUser() {
    }

    public AppUser(String name, String email, String passwordHash, Set<Role> roles) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles = new HashSet<>(roles);
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        // Intencionalmente sem passwordHash nem e-mail (dados sensíveis).
        return "AppUser{id=" + id + ", roles=" + roles + "}";
    }
}
