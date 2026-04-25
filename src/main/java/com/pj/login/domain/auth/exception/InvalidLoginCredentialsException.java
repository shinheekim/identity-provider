package com.pj.login.domain.auth.exception;

public class InvalidLoginCredentialsException extends AuthLoginException {
    public InvalidLoginCredentialsException() {
        super("로그인 ID 또는 비밀번호가 올바르지 않습니다.");
    }
}
