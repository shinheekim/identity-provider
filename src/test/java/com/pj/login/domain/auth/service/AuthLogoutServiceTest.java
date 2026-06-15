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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AuthLogoutServiceTest {

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthLogoutService authLogoutService;

    @Test
    @DisplayName("로그아웃 요청이 오면 Refresh Token을 무효화하고 성공 메시지를 반환한다")
    void logout_revokes_refresh_token_and_returns_success_message() {
        UUID authenticatedUserUuid = UUID.randomUUID();
        given(refreshTokenService.findUserUuid("refresh-token")).willReturn(Optional.of(authenticatedUserUuid));

        LogoutResponse response = authLogoutService.logout(new LogoutRequest("refresh-token"), authenticatedUserUuid);

        then(refreshTokenService).should().revokeRefreshToken("refresh-token");
        assertThat(response.message()).isEqualTo("로그아웃이 완료되었습니다.");
    }

    @Test
    @DisplayName("Refresh Token 소유자가 인증 사용자와 다르면 무효화하지 않고 성공 메시지를 반환한다")
    void logout_skips_revoke_when_refresh_token_owner_mismatches() {
        UUID authenticatedUserUuid = UUID.randomUUID();
        given(refreshTokenService.findUserUuid("refresh-token")).willReturn(Optional.of(UUID.randomUUID()));

        LogoutResponse response = authLogoutService.logout(new LogoutRequest("refresh-token"), authenticatedUserUuid);

        then(refreshTokenService).should(never()).revokeRefreshToken("refresh-token");
        assertThat(response.message()).isEqualTo("로그아웃이 완료되었습니다.");
    }

    @Test
    @DisplayName("Refresh Token이 없으면 무효화하지 않고 성공 메시지를 반환한다")
    void logout_skips_revoke_when_refresh_token_does_not_exist() {
        UUID authenticatedUserUuid = UUID.randomUUID();
        given(refreshTokenService.findUserUuid("refresh-token")).willReturn(Optional.empty());

        LogoutResponse response = authLogoutService.logout(new LogoutRequest("refresh-token"), authenticatedUserUuid);

        then(refreshTokenService).should(never()).revokeRefreshToken("refresh-token");
        assertThat(response.message()).isEqualTo("로그아웃이 완료되었습니다.");
    }
}
