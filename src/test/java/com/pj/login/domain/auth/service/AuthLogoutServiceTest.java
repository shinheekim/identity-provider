package com.pj.login.domain.auth.service;

import com.pj.login.common.security.refresh.RefreshTokenService;
import com.pj.login.domain.auth.dto.LogoutRequest;
import com.pj.login.domain.auth.dto.LogoutResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AuthLogoutServiceTest {

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthLogoutService authLogoutService;

    @Test
    @DisplayName("로그아웃 요청이 오면 Refresh Token을 무효화하고 성공 메시지를 반환한다")
    void logout_revokes_refresh_token_and_returns_success_message() {
        LogoutResponse response = authLogoutService.logout(new LogoutRequest("refresh-token"));

        then(refreshTokenService).should().revokeRefreshToken("refresh-token");
        assertThat(response.message()).isEqualTo("로그아웃이 완료되었습니다.");
    }
}
