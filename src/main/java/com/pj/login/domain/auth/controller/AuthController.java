package com.pj.login.domain.auth.controller;

import com.pj.login.common.response.ApiResponse;
import com.pj.login.domain.auth.dto.SignupRequest;
import com.pj.login.domain.auth.dto.SignupResponse;
import com.pj.login.domain.auth.service.AuthSignupService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthSignupService authSignupService;

    public AuthController(AuthSignupService authSignupService) {
        this.authSignupService = authSignupService;
    }

    @PostMapping("/signup")
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.success(authSignupService.signup(request));
    }
}
