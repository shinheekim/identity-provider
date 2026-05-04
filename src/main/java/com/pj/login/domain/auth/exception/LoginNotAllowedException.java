package com.pj.login.domain.auth.exception;

public class LoginNotAllowedException extends AuthLoginException {
    public LoginNotAllowedException() {
        super("활성 상태의 계정만 로그인할 수 있습니다.");
    }
}
