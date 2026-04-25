package com.pj.login.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청")
public record SignupRequest(
        @Schema(description = "로그인 ID", example = "test@example.com", maxLength = 100)
        @NotBlank(message = "로그인 ID는 필수입니다.")
        @Size(max = 100, message = "로그인 ID는 100자 이하여야 합니다.")
        String loginId,

        @Schema(description = "이메일", example = "test@example.com", maxLength = 255)
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이어야 합니다.")
        @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
        String email,

        @Schema(description = "비밀번호", example = "Password123!", minLength = 8, maxLength = 100)
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 100, message = "비밀번호는 8~100자여야 합니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,100}$",
                message = "비밀번호는 영문, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다."
        )
        String password,
        @Schema(description = "전화번호", example = "01012345678")
        @Pattern(regexp = "^(?:\\d{10,11})?$", message = "전화번호는 숫자 10~11자리여야 합니다.")
        String phoneNumber
) {
}
