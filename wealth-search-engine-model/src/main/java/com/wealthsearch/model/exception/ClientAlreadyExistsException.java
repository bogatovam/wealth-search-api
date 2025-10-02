package com.wealthsearch.model.exception;

public class ClientAlreadyExistsException extends RuntimeException {

    public ClientAlreadyExistsException(String email) {
        super("Client with email '%s' already exists".formatted(email));
    }
}
