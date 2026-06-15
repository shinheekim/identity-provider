package com.pj.login.domain.auth.service;

import com.pj.login.common.security.refresh.RefreshTokenService;
import com.pj.login.domain.auth.dto.LogoutRequest;
import com.pj.login.domain.auth.dto.LogoutResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthLogoutService {

    private static final String LOGOUT_MESSAGE = "로그아웃이 완료되었습니다.";

    private final RefreshTokenService refreshTokenService;

    public LogoutResponse logout(LogoutRequest request, UUID authenticatedUserUuid) {
        refreshTokenService.findUserUuid(request.refreshToken())
                .filter(authenticatedUserUuid::equals)
                .ifPresent(userUuid -> refreshTokenService.revokeRefreshToken(request.refreshToken()));
        return new LogoutResponse(LOGOUT_MESSAGE);
    }
}
