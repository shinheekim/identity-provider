package com.pj.login.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "로그아웃 요청")
public record LogoutRequest(
        @Schema(description = "현재 인증 사용자 소유의 활성 토큰일 때 무효화할 Refresh Token", example = "refresh-token-value", maxLength = 500)
        @NotBlank(message = "Refresh Token은 필수입니다.")
        @Size(max = 500, message = "Refresh Token은 500자 이하여야 합니다.")
        String refreshToken
) {
}
