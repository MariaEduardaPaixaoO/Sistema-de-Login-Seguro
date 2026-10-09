package com.example.loginseguro.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.loginseguro.model.Role;
import com.example.loginseguro.security.AuthenticationUtils;

import org.springframework.ui.Model;

/**
 * Disponibiliza para todas as views flags simples de apresentação (menu).
 *
 * A decisão sobre "quem pode ver o quê" é calculada aqui, em Java, e os
 * templates só leem booleanos. Isso é apenas conveniência de interface: a
 * proteção real das rotas está no SecurityConfig.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute
    public void addNavigationFlags(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = AuthenticationUtils.isAuthenticated(authentication);

        model.addAttribute("authenticated", authenticated);
        model.addAttribute("canAccessUser", AuthenticationUtils.hasRole(authentication, Role.ROLE_USER));
        model.addAttribute("canAccessModerator", AuthenticationUtils.hasRole(authentication, Role.ROLE_MODERATOR));
        model.addAttribute("canAccessAdmin", AuthenticationUtils.hasRole(authentication, Role.ROLE_ADMIN));
    }
}
