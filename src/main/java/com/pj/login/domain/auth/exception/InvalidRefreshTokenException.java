package com.pj.login.domain.auth.exception;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Refresh Token이 유효하지 않습니다.");
    }
}
