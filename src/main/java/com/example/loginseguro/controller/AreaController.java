package com.example.loginseguro.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.loginseguro.model.AppUser;
import com.example.loginseguro.service.UserService;

/**
 * Página inicial (área autenticada) e páginas de demonstração por perfil.
 *
 * O controle de acesso dessas rotas NÃO está aqui: está no SecurityConfig
 * (/user/**, /moderator/**, /admin/**). Estes métodos só rodam depois que o
 * Spring Security autorizou a requisição.
 */
@Controller
public class AreaController {

    private final UserService userService;

    public AreaController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        String email = authentication.getName();
        String displayName = userService.findByEmail(email).map(AppUser::getName).orElse(email);
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();

        model.addAttribute("displayName", displayName);
        model.addAttribute("email", email);
        model.addAttribute("roles", roles);
        return "home";
    }

    @GetMapping("/user")
    public String userArea() {
        return "user/index";
    }

    @GetMapping("/moderator")
    public String moderatorArea() {
        return "moderator/index";
    }

    @GetMapping("/admin")
    public String adminArea() {
        return "admin/index";
    }
}
