package com.example.loginseguro.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.example.loginseguro.model.Role;

/**
 * Helpers de leitura do usuário autenticado. Usados para preparar o modelo das
 * views em Java, assim os templates não contêm nenhuma regra de autorização.
 */
public final class AuthenticationUtils {

    private AuthenticationUtils() {
    }

    public static boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    public static boolean hasRole(Authentication authentication, Role role) {
        return isAuthenticated(authentication)
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> role.name().equals(authority.getAuthority()));
    }
}
