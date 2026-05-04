package com.pj.login.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pj.login.domain.auth.constant.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "로그인 응답")
public record LoginResponse(
        @Schema(description = "사용자 UUID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID userUuid,

        @Schema(description = "액세스 토큰")
        String accessToken,

        @JsonInclude(JsonInclude.Include.ALWAYS)
        @Schema(description = "Refresh Token (현재 미발급으로 null 반환)", nullable = true)
        String refreshToken,

        @Schema(description = "계정 상태", example = "ACTIVE")
        AccountStatus accountStatus
) {
}
