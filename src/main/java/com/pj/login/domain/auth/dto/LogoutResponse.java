package com.pj.login.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그아웃 응답")
public record LogoutResponse(
        @Schema(description = "처리 결과 메시지", example = "로그아웃이 완료되었습니다.")
        String message
) {
}
