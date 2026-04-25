package com.pj.login.domain.auth.controller;

import com.pj.login.common.response.ApiResponse;
import com.pj.login.domain.auth.dto.LoginRequest;
import com.pj.login.domain.auth.dto.LoginResponse;
import com.pj.login.domain.auth.dto.SignupRequest;
import com.pj.login.domain.auth.dto.SignupResponse;
import com.pj.login.domain.auth.service.AuthLoginService;
import com.pj.login.domain.auth.service.AuthSignupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Auth API")
public class AuthController {

    private final AuthLoginService authLoginService;
    private final AuthSignupService authSignupService;

    public AuthController(AuthLoginService authLoginService, AuthSignupService authSignupService) {
        this.authLoginService = authLoginService;
        this.authSignupService = authSignupService;
    }

    @Operation(summary = "로그인", description = "로컬 계정 로그인 처리를 수행합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 ID 또는 비밀번호 오류")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "423", description = "비밀번호 입력 오류 누적으로 잠긴 계정")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(authLoginService.login(
                request,
                extractClientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent")
        ));
    }

    @Operation(summary = "회원가입", description = "로컬 계정 회원가입을 처리합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원가입 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 오류 또는 중복 계정")
    @PostMapping("/signup")
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.success(authSignupService.signup(request));
    }

    /**
     * 로그인 이력과 보안 추적용 메타데이터를 남기기 위해 컨트롤러에서 IP를 추출해서 서비스로 전달
     */
    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
