package com.bcp.workshop.auth;

public class AuthPersistenceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AuthPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
