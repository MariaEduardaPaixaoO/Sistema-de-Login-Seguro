package com.example.loginseguro.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Distingue "credenciais inválidas" de "falha interna" (por exemplo, MongoDB
 * indisponível). Sem isso, uma queda do banco apareceria para o usuário como
 * "senha incorreta".
 *
 * Nunca registra nem repassa e-mail, senha ou a mensagem da exceção.
 */
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(LoginFailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {

        String target;
        if (exception instanceof AuthenticationServiceException) {
            // Inclui InternalAuthenticationServiceException (erro ao carregar o usuário).
            log.error("Falha interna durante a autenticação ({})", exception.getClass().getSimpleName());
            target = "/login?unavailable";
        } else {
            // Usuário inexistente e senha errada resultam na MESMA mensagem (sem enumeração de contas).
            target = "/login?error";
        }
        response.sendRedirect(request.getContextPath() + target);
    }
}
