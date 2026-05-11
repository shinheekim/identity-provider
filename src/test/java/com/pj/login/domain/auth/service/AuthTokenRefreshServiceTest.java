package com.pj.login.domain.auth.service;

import com.pj.login.common.config.JwtProperties;
import com.pj.login.common.security.JwtTokenService;
import com.pj.login.common.security.refresh.RefreshTokenService;
import com.pj.login.common.time.KoreaTime;
import com.pj.login.common.time.TimeProvider;
import com.pj.login.domain.auth.constant.AccountStatus;
import com.pj.login.domain.auth.dto.TokenRefreshRequest;
import com.pj.login.domain.auth.dto.TokenRefreshResponse;
import com.pj.login.domain.auth.entity.User;
import com.pj.login.domain.auth.exception.InvalidRefreshTokenException;
import com.pj.login.domain.auth.exception.LoginNotAllowedException;
import com.pj.login.domain.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class AuthTokenRefreshServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-05-11T00:00:00Z");
    private static final String TEST_SECRET = "test-only-jwt-secret-key-change-me-1234567890-abcdef";

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private UserRepository userRepository;

    @Spy
    private TimeProvider timeProvider = new TimeProvider(Clock.fixed(FIXED_NOW, KoreaTime.ZONE_ID));

    @Spy
    private JwtProperties jwtProperties = new JwtProperties("taesin", 1800, 1209600, TEST_SECRET);

    @InjectMocks
    private AuthTokenRefreshService authTokenRefreshService;

    @Test
    @DisplayName("유효한 리프레시 토큰이면 기존 토큰을 폐기하고 새 토큰들을 발급한다")
    void refresh_rotates_refresh_token_and_issues_tokens() {
        User user = activeUser();
        UUID userUuid = user.getUserUuid();
        given(refreshTokenService.consumeUserUuid("old-refresh-token")).willReturn(Optional.of(userUuid));
        given(userRepository.findByUserUuid(userUuid)).willReturn(Optional.of(user));
        given(jwtTokenService.issueAccessToken(eq(userUuid), any(LocalDateTime.class)))
                .willAnswer(invocation -> {
                    LocalDateTime issuedAt = invocation.getArgument(1);
                    return new JwtTokenService.JwtToken("new-access-token", issuedAt.plusSeconds(1800));
                });
        given(refreshTokenService.issueRefreshToken(eq(userUuid), any(LocalDateTime.class)))
                .willAnswer(invocation -> {
                    LocalDateTime issuedAt = invocation.getArgument(1);
                    return new RefreshTokenService.RefreshToken("new-refresh-token", issuedAt.plusDays(14));
                });

        TokenRefreshResponse response =
                authTokenRefreshService.refresh(new TokenRefreshRequest("old-refresh-token"));

        InOrder inOrder = inOrder(refreshTokenService, jwtTokenService);
        inOrder.verify(refreshTokenService).consumeUserUuid("old-refresh-token");
        inOrder.verify(jwtTokenService).issueAccessToken(eq(userUuid), any(LocalDateTime.class));
        inOrder.verify(refreshTokenService).issueRefreshToken(eq(userUuid), any(LocalDateTime.class));

        assertSoftly(softly -> {
            softly.assertThat(response.accessToken()).isEqualTo("new-access-token");
            softly.assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
            softly.assertThat(response.expiresIn()).isEqualTo(1800);
        });
    }

    @Test
    @DisplayName("저장소에 없는 리프레시 토큰이면 예외를 던진다")
    void refresh_invalid_token() {
        given(refreshTokenService.consumeUserUuid("invalid-refresh-token")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authTokenRefreshService.refresh(new TokenRefreshRequest("invalid-refresh-token")))
                .isInstanceOf(InvalidRefreshTokenException.class);

        then(userRepository).shouldHaveNoInteractions();
        then(jwtTokenService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("ACTIVE가 아닌 사용자의 리프레시 토큰이면 로그인 불가 예외를 던진다")
    void refresh_not_active_user() {
        User user = User.builder()
                .accountStatus(AccountStatus.DORMANT)
                .email("dormant@example.com")
                .emailVerified(false)
                .phoneVerified(false)
                .build();
        UUID userUuid = user.getUserUuid();
        given(refreshTokenService.consumeUserUuid("refresh-token")).willReturn(Optional.of(userUuid));
        given(userRepository.findByUserUuid(userUuid)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> authTokenRefreshService.refresh(new TokenRefreshRequest("refresh-token")))
                .isInstanceOf(LoginNotAllowedException.class);

        then(jwtTokenService).shouldHaveNoInteractions();
    }

    private User activeUser() {
        return User.builder()
                .accountStatus(AccountStatus.ACTIVE)
                .email("active@example.com")
                .emailVerified(false)
                .phoneVerified(false)
                .build();
    }
}
