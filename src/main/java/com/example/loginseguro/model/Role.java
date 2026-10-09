package com.example.loginseguro.model;

/**
 * Perfis de acesso. Os nomes já incluem o prefixo "ROLE_" porque são usados
 * diretamente como authorities do Spring Security ({@code hasRole("ADMIN")}
 * compara com "ROLE_ADMIN").
 */
public enum Role {
    ROLE_USER,
    ROLE_MODERATOR,
    ROLE_ADMIN
}
