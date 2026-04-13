package com.pj.login.domain.auth.dto;

import com.pj.login.domain.auth.constant.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "회원가입 응답")
public record SignupResponse(
        @Schema(description = "사용자 UUID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID userUuid,

        @Schema(description = "계정 상태", example = "ACTIVE")
        AccountStatus accountStatus
) {
}
