package com.pj.login.domain.auth.controller;

import com.pj.login.common.response.ApiResult;
import com.pj.login.domain.auth.dto.LoginRequest;
import com.pj.login.domain.auth.dto.LoginResponse;
import com.pj.login.domain.auth.dto.LogoutRequest;
import com.pj.login.domain.auth.dto.LogoutResponse;
import com.pj.login.domain.auth.dto.SignupRequest;
import com.pj.login.domain.auth.dto.SignupResponse;
import com.pj.login.domain.auth.dto.TokenRefreshRequest;
import com.pj.login.domain.auth.dto.TokenRefreshResponse;
import com.pj.login.domain.auth.service.AuthLoginService;
import com.pj.login.domain.auth.service.AuthLogoutService;
import com.pj.login.domain.auth.service.AuthSignupService;
import com.pj.login.domain.auth.service.AuthTokenRefreshService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Auth API")
public class AuthController {

    private final AuthLoginService authLoginService;
    private final AuthSignupService authSignupService;
    private final AuthLogoutService authLogoutService;
    private final AuthTokenRefreshService authTokenRefreshService;

    @Operation(summary = "로그인", description = "로컬 계정 로그인 처리를 수행합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "로그인 ID 또는 비밀번호 오류"),
            @ApiResponse(responseCode = "403", description = "로그인 불가 상태의 계정"),
            @ApiResponse(responseCode = "423", description = "비밀번호 입력 오류 누적으로 잠긴 계정")
    })
    @PostMapping("/login")
    public ApiResult<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResult.success(authLoginService.login(
                request,
                extractClientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent")
        ));
    }

    @Operation(summary = "회원가입", description = "로컬 계정 회원가입을 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 오류 또는 중복 계정")
    })
    @PostMapping("/signup")
    public ApiResult<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResult.success(authSignupService.signup(request));
    }

    @Operation(summary = "로그아웃", description = "현재 인증 상태를 종료하고 Refresh Token을 무효화합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 오류"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청")
    })
    @PostMapping("/logout")
    public ApiResult<LogoutResponse> logout(@Valid @RequestBody LogoutRequest request) {
        return ApiResult.success(authLogoutService.logout(request));
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token을 이용해 Access Token과 Refresh Token을 재발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 Refresh Token"),
            @ApiResponse(responseCode = "403", description = "로그인 불가 상태의 계정")
    })
    @PostMapping("/token/refresh")
    public ApiResult<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        return ApiResult.success(authTokenRefreshService.refresh(request));
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
