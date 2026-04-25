package com.pj.login.domain.auth.dto;

import com.pj.login.domain.auth.constant.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "로그인 응답")
public record LoginResponse(
        @Schema(description = "사용자 UUID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID userUuid,

        @Schema(description = "계정 상태", example = "ACTIVE")
        AccountStatus accountStatus,

        @Schema(description = "액세스 토큰")
        String accessToken,

        @Schema(description = "액세스 토큰 만료 시각", example = "2026-04-24T12:00:00")
        LocalDateTime accessTokenExpiresAt,

        @Schema(description = "리프레시 토큰")
        String refreshToken,

        @Schema(description = "리프레시 토큰 만료 시각", example = "2026-05-08T12:00:00")
        LocalDateTime refreshTokenExpiresAt,

        @Schema(description = "로그인 시각", example = "2026-04-24T11:30:00")
        LocalDateTime loginAt
) {
}
