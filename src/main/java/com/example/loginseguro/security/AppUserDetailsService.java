package com.example.loginseguro.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.loginseguro.model.AppUser;
import com.example.loginseguro.model.Role;
import com.example.loginseguro.service.UserService;

/**
 * Ponte entre o MongoDB e o Spring Security: carrega o usuário pelo e-mail
 * (o "username" do login) e converte os perfis em authorities.
 *
 * Retorna o {@link User} padrão do Spring Security, que é serializável. Isso é
 * necessário porque o contexto de segurança fica guardado na sessão, e a sessão
 * é persistida no MongoDB por serialização Java.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserService userService;

    public AppUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Mesma normalização do cadastro: o login não diferencia maiúsculas/minúsculas.
        AppUser user = userService.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        String[] authorities = user.getRoles().stream().map(Role::name).toArray(String[]::new);

        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .build();
    }
}
