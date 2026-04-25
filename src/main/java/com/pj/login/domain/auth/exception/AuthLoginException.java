package com.pj.login.domain.auth.exception;

public abstract class AuthLoginException extends RuntimeException {
    protected AuthLoginException(String message) {
        super(message);
    }
}
