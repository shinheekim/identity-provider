package com.pj.login.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 재발급 응답")
public record TokenRefreshResponse(
        @Schema(description = "새 액세스 토큰")
        String accessToken,

        @Schema(description = "새 Refresh Token")
        String refreshToken,

        @Schema(description = "액세스 토큰 만료 시간(초)", example = "1800")
        long expiresIn
) {
}
