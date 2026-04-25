package com.pj.login.domain.auth.exception;

public class PasswordLockedException extends AuthLoginException {
    public PasswordLockedException() {
        super("비밀번호 입력 오류가 반복되어 계정이 잠겼습니다. 10분 후 다시 시도해주세요.");
    }
}
