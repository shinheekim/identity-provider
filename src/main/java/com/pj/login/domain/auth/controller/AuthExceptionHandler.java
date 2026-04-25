package com.pj.login.domain.auth.controller;

import com.pj.login.common.response.ApiResponse;
import com.pj.login.domain.auth.exception.DuplicateEmailException;
import com.pj.login.domain.auth.exception.DuplicateLoginIdException;
import com.pj.login.domain.auth.exception.InvalidLoginCredentialsException;
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
    public ApiResponse<Void> handleDuplicateEmail(DuplicateEmailException ex) {
        return ApiResponse.error("DUPLICATE_EMAIL", ex.getMessage());
    }

    @ExceptionHandler(DuplicateLoginIdException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleDuplicateLoginId(DuplicateLoginIdException ex) {
        return ApiResponse.error("DUPLICATE_LOGIN_ID", ex.getMessage());
    }

    @ExceptionHandler(InvalidLoginCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleInvalidLoginCredentials(InvalidLoginCredentialsException ex) {
        return ApiResponse.error("INVALID_LOGIN_CREDENTIALS", ex.getMessage());
    }

    @ExceptionHandler(LoginNotAllowedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleLoginNotAllowed(LoginNotAllowedException ex) {
        return ApiResponse.error("LOGIN_NOT_ALLOWED", ex.getMessage());
    }

    @ExceptionHandler(PasswordLockedException.class)
    @ResponseStatus(HttpStatus.LOCKED)
    public ApiResponse<Void> handlePasswordLocked(PasswordLockedException ex) {
        return ApiResponse.error("PASSWORD_LOCKED", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .filter(msg -> msg != null && !msg.isBlank())
                .findFirst()
                .orElse("요청 값이 올바르지 않습니다.");
        return ApiResponse.error("INVALID_INPUT", message);
    }
}
