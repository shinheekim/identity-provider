package com.pj.login.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "토큰 재발급 요청")
public record TokenRefreshRequest(
        @Schema(description = "Refresh Token", example = "refresh-token-value", maxLength = 500)
        @NotBlank(message = "Refresh Token은 필수입니다.")
        @Size(max = 500, message = "Refresh Token은 500자 이하여야 합니다.")
        String refreshToken
) {
}
