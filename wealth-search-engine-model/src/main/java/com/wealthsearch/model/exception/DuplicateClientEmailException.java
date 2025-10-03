package com.wealthsearch.model.exception;

public class DuplicateClientEmailException extends EntityAlreadyExistsException {

    public DuplicateClientEmailException(String email) {
        super("Client with email '%s' already exists".formatted(email));
    }
}
