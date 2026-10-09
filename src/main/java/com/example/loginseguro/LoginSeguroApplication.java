package com.example.loginseguro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da aplicação.
 *
 * A configuração do MongoDB, do Spring Session e do Spring Security é feita
 * por auto-configuração do Spring Boot (application.yml) e pelas classes dos
 * pacotes {@code security} e {@code config}.
 */
@SpringBootApplication
public class LoginSeguroApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoginSeguroApplication.class, args);
    }
}
