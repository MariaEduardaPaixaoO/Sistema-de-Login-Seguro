package com.example.loginseguro.service;

/** Já existe um usuário com o e-mail informado. */
public class EmailAlreadyRegisteredException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EmailAlreadyRegisteredException() {
        // Sem o e-mail na mensagem: evita colocar dado pessoal em logs.
        super("E-mail já cadastrado");
    }
}
