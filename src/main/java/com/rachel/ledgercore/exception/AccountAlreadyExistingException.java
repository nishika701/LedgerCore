package com.rachel.ledgercore.exception;

public class AccountAlreadyExistingException extends RuntimeException {
    public AccountAlreadyExistingException(String message) {
        super(message);
    }
}
