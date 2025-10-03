package com.wealthsearch.model.exception;

public class OllamaClientException extends RuntimeException {

    public OllamaClientException(String message) {
        super(message);
    }

    public OllamaClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
