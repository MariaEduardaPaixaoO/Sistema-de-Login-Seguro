package com.example.loginseguro.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração central do Spring Security.
 *
 * Princípio: tudo é negado por padrão ({@code anyRequest().denyAll()}); só o que
 * está listado abaixo é liberado. A autorização acontece no servidor, em todas as
 * requisições, independentemente de os links aparecerem ou não nas páginas.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Recursos estáticos (CSS/JS) e a página de erro do Spring Boot (renderiza 403/404/500).
                .requestMatchers("/css/**", "/js/**", "/error").permitAll()
                // Telas públicas.
                .requestMatchers("/login", "/register").permitAll()
                // Áreas por perfil. Cada área exige exatamente o seu perfil.
                .requestMatchers("/user/**").hasRole("USER")
                .requestMatchers("/moderator/**").hasRole("MODERATOR")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Página inicial: qualquer usuário autenticado.
                .requestMatchers("/").authenticated()
                // Qualquer outra rota é negada.
                .anyRequest().denyAll()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .defaultSuccessUrl("/")
                .failureHandler(new LoginFailureHandler())
                .permitAll()
            )
            .logout(logout -> logout
                // Logout via POST protegido por CSRF (o padrão do Spring Security).
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("SESSION")
            )
            .sessionManagement(session -> session
                // Troca o ID da sessão ao autenticar (proteção contra fixação de sessão).
                .sessionFixation(fixation -> fixation.changeSessionId())
            )
            .headers(headers -> headers
                // Todo CSS/JS vem do próprio servidor; não há scripts nem estilos inline.
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; frame-ancestors 'none'; form-action 'self'; base-uri 'self'"))
            );
        // CSRF permanece habilitado (padrão). Os formulários Thymeleaf com th:action
        // recebem o token oculto automaticamente.
        // Acesso negado (403) usa o tratamento padrão: o Spring Boot renderiza templates/error/403.html.
        return http.build();
    }

    /**
     * BCrypt com sal aleatório embutido no hash. O custo é configurável
     * (app.security.bcrypt-strength) para que os testes possam usar um valor menor.
     */
    @Bean
    public PasswordEncoder passwordEncoder(@Value("${app.security.bcrypt-strength:12}") int strength) {
        return new BCryptPasswordEncoder(strength);
    }
}
