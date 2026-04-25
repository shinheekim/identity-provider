package com.pj.login.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "로그인 요청")
public record LoginRequest(
        @Schema(description = "로그인 ID", example = "tester01", maxLength = 100)
        @NotBlank(message = "로그인 ID는 필수입니다.")
        @Size(max = 100, message = "로그인 ID는 100자 이하여야 합니다.")
        String loginId,

        @Schema(description = "비밀번호", example = "Password123!", maxLength = 100)
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(max = 100, message = "비밀번호는 100자 이하여야 합니다.")
        String password
) {
}
