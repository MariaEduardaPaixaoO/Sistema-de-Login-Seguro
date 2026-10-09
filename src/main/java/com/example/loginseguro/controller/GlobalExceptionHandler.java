package com.example.loginseguro.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Trata falhas de persistência (por exemplo, MongoDB Atlas inacessível) com uma
 * página amigável e status 503, sem expor detalhes internos ao usuário.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DataAccessException.class)
    public ModelAndView handleDatabaseFailure(DataAccessException exception) {
        // Só o tipo da exceção vai para o log: a mensagem do driver pode conter endereços do cluster.
        log.error("Falha de acesso ao banco de dados ({})", exception.getClass().getSimpleName());

        ModelAndView modelAndView = new ModelAndView("error/database");
        modelAndView.setStatus(HttpStatus.SERVICE_UNAVAILABLE);
        return modelAndView;
    }
}
