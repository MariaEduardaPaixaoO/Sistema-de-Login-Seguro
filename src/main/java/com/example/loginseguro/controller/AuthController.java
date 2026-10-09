package com.example.loginseguro.controller;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.loginseguro.dto.RegistrationForm;
import com.example.loginseguro.security.AuthenticationUtils;
import com.example.loginseguro.service.EmailAlreadyRegisteredException;
import com.example.loginseguro.service.UserService;

import jakarta.validation.Valid;

/**
 * Telas públicas de login e cadastro. O processamento do login (POST /login) e do
 * logout (POST /logout) é feito pelos filtros do Spring Security.
 */
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Só estes campos podem ser preenchidos a partir da requisição. Qualquer outro
     * parâmetro (por exemplo "roles=ROLE_ADMIN") é ignorado.
     */
    @InitBinder("form")
    void configureBinder(WebDataBinder binder) {
        binder.setAllowedFields("name", "email", "password", "confirmPassword");
        // Remove espaços nas pontas de nome e e-mail (ex.: e-mail colado com espaço). A senha NUNCA é alterada.
        binder.registerCustomEditor(String.class, "name", new StringTrimmerEditor(false));
        binder.registerCustomEditor(String.class, "email", new StringTrimmerEditor(false));
    }

    @GetMapping("/login")
    public String loginPage(Authentication authentication) {
        if (AuthenticationUtils.isAuthenticated(authentication)) {
            return "redirect:/";
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Authentication authentication, Model model) {
        if (AuthenticationUtils.isAuthenticated(authentication)) {
            return "redirect:/";
        }
        model.addAttribute("form", new RegistrationForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(Authentication authentication,
            @Valid @ModelAttribute("form") RegistrationForm form,
            BindingResult bindingResult) {

        if (AuthenticationUtils.isAuthenticated(authentication)) {
            return "redirect:/";
        }
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            userService.registerUser(form);
        } catch (EmailAlreadyRegisteredException e) {
            bindingResult.rejectValue("email", "email.duplicate", "Este e-mail já está cadastrado.");
            return "auth/register";
        }
        // Redirect (Post/Redirect/Get). A mensagem de sucesso é exibida pelo template de login.
        return "redirect:/login?registered";
    }
}
