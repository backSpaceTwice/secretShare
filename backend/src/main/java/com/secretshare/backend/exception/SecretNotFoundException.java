package com.secretshare.backend.exception;

public class SecretNotFoundException extends RuntimeException {
    public SecretNotFoundException() {
        super("Secret not found");
    }
}
