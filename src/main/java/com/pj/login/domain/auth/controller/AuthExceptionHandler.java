package com.pj.login.domain.auth.controller;

import com.pj.login.common.response.ApiResult;
import com.pj.login.domain.auth.exception.AuthLoginException;
import com.pj.login.domain.auth.exception.DuplicateEmailException;
import com.pj.login.domain.auth.exception.DuplicateLoginIdException;
import com.pj.login.domain.auth.exception.LoginNotAllowedException;
import com.pj.login.domain.auth.exception.PasswordLockedException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleDuplicateEmail(DuplicateEmailException ex) {
        return ApiResult.error("DUPLICATE_EMAIL", ex.getMessage());
    }

    @ExceptionHandler(DuplicateLoginIdException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleDuplicateLoginId(DuplicateLoginIdException ex) {
        return ApiResult.error("DUPLICATE_LOGIN_ID", ex.getMessage());
    }

    @ExceptionHandler(PasswordLockedException.class)
    @ResponseStatus(HttpStatus.LOCKED)
    public ApiResult<Void> handlePasswordLocked(PasswordLockedException ex) {
        return ApiResult.error("PASSWORD_LOCKED", ex.getMessage());
    }

    @ExceptionHandler(LoginNotAllowedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResult<Void> handleLoginNotAllowed(LoginNotAllowedException ex) {
        return ApiResult.error("LOGIN_NOT_ALLOWED", ex.getMessage());
    }

    @ExceptionHandler(AuthLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResult<Void> handleAuthLoginFailure(AuthLoginException ex) {
        return ApiResult.error("INVALID_LOGIN_CREDENTIALS", "로그인 ID 또는 비밀번호가 올바르지 않습니다.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .filter(msg -> msg != null && !msg.isBlank())
                .findFirst()
                .orElse("요청 값이 올바르지 않습니다.");
        return ApiResult.error("INVALID_INPUT", message);
    }
}
