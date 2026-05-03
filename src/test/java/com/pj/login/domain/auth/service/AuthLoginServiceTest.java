package com.pj.login.domain.auth.service;

import com.pj.login.common.security.JwtTokenService;
import com.pj.login.common.time.KoreaTime;
import com.pj.login.common.time.TimeProvider;
import com.pj.login.domain.auth.constant.AccountStatus;
import com.pj.login.domain.auth.constant.LoginAttemptResult;
import com.pj.login.domain.auth.constant.LoginFailureReason;
import com.pj.login.domain.auth.constant.PasswordAlgo;
import com.pj.login.domain.auth.constant.ProviderType;
import com.pj.login.domain.auth.dto.LoginRequest;
import com.pj.login.domain.auth.dto.LoginResponse;
import com.pj.login.domain.auth.entity.Identity;
import com.pj.login.domain.auth.entity.LoginHistory;
import com.pj.login.domain.auth.entity.Password;
import com.pj.login.domain.auth.entity.User;
import com.pj.login.domain.auth.exception.InvalidLoginCredentialsException;
import com.pj.login.domain.auth.exception.LoginNotAllowedException;
import com.pj.login.domain.auth.repository.IdentityRepository;
import com.pj.login.domain.auth.repository.LoginHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AuthLoginServiceTest {

    private static final String CLIENT_IP = "127.0.0.1";
    private static final String USER_AGENT = "JUnit";
    private static final String CORRECT_PASSWORD = "Password123!";
    private static final String WRONG_PASSWORD = "WrongPassword123!";
    private static final String ENCODED_PASSWORD = "encoded-password";
    private static final Instant FIXED_NOW = Instant.parse("2026-05-04T00:00:00Z");

    @Mock
    private IdentityRepository identityRepository;

    @Mock
    private LoginHistoryRepository loginHistoryRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private PasswordHashingService passwordHashingService;

    @Spy
    private TimeProvider timeProvider = new TimeProvider(Clock.fixed(FIXED_NOW, KoreaTime.ZONE_ID));

    @InjectMocks
    private AuthLoginService authLoginService;

    @Captor
    private ArgumentCaptor<LoginHistory> loginHistoryCaptor;

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("성공하면 마지막 로그인 시각과 성공 이력을 갱신한다")
        void updates_last_login_and_history_on_success() {
            LocalLoginFixture fixture = localLoginFixture("login@example.com", "login@example.com");
            givenFoundIdentity("login@example.com", fixture.identity());
            given(passwordHashingService.matches(
                    CORRECT_PASSWORD,
                    fixture.password().getPasswordHash(),
                    fixture.password().getPasswordAlgo()
            )).willReturn(true);
            givenIssuedTokens(fixture.user());

            LoginResponse response = authLoginService.login(
                    new LoginRequest("login@example.com", CORRECT_PASSWORD),
                    CLIENT_IP,
                    USER_AGENT
            );

            then(jwtTokenService).should().issueAccessToken(fixture.user().getUserUuid(), response.loginAt());
            LoginHistory loginHistory = capturedLoginHistory();

            assertSoftly(softly -> {
                softly.assertThat(response.userUuid()).isEqualTo(fixture.user().getUserUuid());
                softly.assertThat(response.accountStatus()).isEqualTo(AccountStatus.ACTIVE);
                softly.assertThat(response.accessToken()).isEqualTo("access-token");
                softly.assertThat(response.accessTokenExpiresAt()).isAfter(response.loginAt());
                softly.assertThat(fixture.user().getLastLoginAt()).isEqualTo(response.loginAt());
                softly.assertThat(fixture.password().getFailCount()).isZero();
                softly.assertThat(fixture.password().getLockedUntil()).isNull();
                softly.assertThat(loginHistory.getProviderType()).isEqualTo(ProviderType.LOCAL);
                softly.assertThat(loginHistory.getLoginId()).isEqualTo("login@example.com");
                softly.assertThat(loginHistory.getAttemptResult()).isEqualTo(LoginAttemptResult.SUCCESS);
                softly.assertThat(loginHistory.getFailReason()).isNull();
                softly.assertThat(loginHistory.getClientIp()).isEqualTo(CLIENT_IP);
                softly.assertThat(loginHistory.getUserAgent()).isEqualTo(USER_AGENT);
            });
        }

        @Test
        @DisplayName("비밀번호가 틀리면 실패 횟수와 실패 이력을 남긴다")
        void records_failure_count_and_history_when_password_is_invalid() {
            LocalLoginFixture fixture = localLoginFixture(
                    "wrong-password@example.com",
                    "wrong-password@example.com"
            );
            givenFoundIdentity("wrong-password@example.com", fixture.identity());
            given(passwordHashingService.matches(
                    WRONG_PASSWORD,
                    fixture.password().getPasswordHash(),
                    fixture.password().getPasswordAlgo()
            )).willReturn(false);

            assertThatThrownBy(() -> authLoginService.login(
                    new LoginRequest("wrong-password@example.com", WRONG_PASSWORD),
                    CLIENT_IP,
                    USER_AGENT
            )).isInstanceOf(InvalidLoginCredentialsException.class);

            LoginHistory loginHistory = capturedLoginHistory();

            assertSoftly(softly -> {
                softly.assertThat(fixture.password().getFailCount()).isEqualTo(1);
                softly.assertThat(fixture.password().getLockedUntil()).isNull();
                softly.assertThat(loginHistory.getProviderType()).isEqualTo(ProviderType.LOCAL);
                softly.assertThat(loginHistory.getLoginId()).isEqualTo("wrong-password@example.com");
                softly.assertThat(loginHistory.getAttemptResult()).isEqualTo(LoginAttemptResult.FAILURE);
                softly.assertThat(loginHistory.getFailReason()).isEqualTo(LoginFailureReason.INVALID_PASSWORD);
                softly.assertThat(loginHistory.getClientIp()).isEqualTo(CLIENT_IP);
                softly.assertThat(loginHistory.getUserAgent()).isEqualTo(USER_AGENT);
            });

            then(jwtTokenService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("비밀번호가 없으면 PASSWORD_NOT_SET 이력을 남기고 로그인에 실패한다")
        void records_password_not_set_history_when_password_is_missing() {
            LocalLoginFixture fixture = localLoginFixtureWithoutPassword(
                    "password-not-set@example.com",
                    "password-not-set@example.com",
                    AccountStatus.ACTIVE
            );
            givenFoundIdentity("password-not-set@example.com", fixture.identity());

            assertThatThrownBy(() -> authLoginService.login(
                    new LoginRequest("password-not-set@example.com", CORRECT_PASSWORD),
                    CLIENT_IP,
                    USER_AGENT
            )).isInstanceOf(InvalidLoginCredentialsException.class);

            LoginHistory loginHistory = capturedLoginHistory();

            assertSoftly(softly -> {
                softly.assertThat(loginHistory.getProviderType()).isEqualTo(ProviderType.LOCAL);
                softly.assertThat(loginHistory.getLoginId()).isEqualTo("password-not-set@example.com");
                softly.assertThat(loginHistory.getAttemptResult()).isEqualTo(LoginAttemptResult.FAILURE);
                softly.assertThat(loginHistory.getFailReason()).isEqualTo(LoginFailureReason.PASSWORD_NOT_SET);
                softly.assertThat(loginHistory.getClientIp()).isEqualTo(CLIENT_IP);
                softly.assertThat(loginHistory.getUserAgent()).isEqualTo(USER_AGENT);
            });

            then(passwordHashingService).shouldHaveNoInteractions();
            then(jwtTokenService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("ACTIVE가 아닌 계정이면 ACCOUNT_NOT_ACTIVE 이력을 남기고 로그인에 실패한다")
        void records_account_not_active_history_when_account_is_not_active() {
            LocalLoginFixture fixture = localLoginFixture(
                    "dormant-user@example.com",
                    "dormant-user@example.com",
                    AccountStatus.DORMANT
            );
            givenFoundIdentity("dormant-user@example.com", fixture.identity());

            assertThatThrownBy(() -> authLoginService.login(
                    new LoginRequest("dormant-user@example.com", CORRECT_PASSWORD),
                    CLIENT_IP,
                    USER_AGENT
            )).isInstanceOf(LoginNotAllowedException.class);

            LoginHistory loginHistory = capturedLoginHistory();

            assertSoftly(softly -> {
                softly.assertThat(loginHistory.getProviderType()).isEqualTo(ProviderType.LOCAL);
                softly.assertThat(loginHistory.getLoginId()).isEqualTo("dormant-user@example.com");
                softly.assertThat(loginHistory.getAttemptResult()).isEqualTo(LoginAttemptResult.FAILURE);
                softly.assertThat(loginHistory.getFailReason()).isEqualTo(LoginFailureReason.ACCOUNT_NOT_ACTIVE);
                softly.assertThat(loginHistory.getClientIp()).isEqualTo(CLIENT_IP);
                softly.assertThat(loginHistory.getUserAgent()).isEqualTo(USER_AGENT);
            });

            then(passwordHashingService).shouldHaveNoInteractions();
            then(jwtTokenService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("비이메일 loginId로도 로그인할 수 있다")
        void supports_non_email_login_id() {
            LocalLoginFixture fixture = localLoginFixture("tester01", "tester01@example.com");
            givenFoundIdentity("tester01", fixture.identity());
            given(passwordHashingService.matches(
                    CORRECT_PASSWORD,
                    fixture.password().getPasswordHash(),
                    fixture.password().getPasswordAlgo()
            )).willReturn(true);
            givenIssuedTokens(fixture.user());

            LoginResponse response = authLoginService.login(
                    new LoginRequest("tester01", CORRECT_PASSWORD),
                    CLIENT_IP,
                    USER_AGENT
            );

            LoginHistory loginHistory = capturedLoginHistory();

            assertSoftly(softly -> {
                softly.assertThat(response.userUuid()).isEqualTo(fixture.user().getUserUuid());
                softly.assertThat(response.accountStatus()).isEqualTo(AccountStatus.ACTIVE);
                softly.assertThat(response.accessToken()).isEqualTo("access-token");
                softly.assertThat(loginHistory.getLoginId()).isEqualTo("tester01");
                softly.assertThat(loginHistory.getAttemptResult()).isEqualTo(LoginAttemptResult.SUCCESS);
            });
        }
    }

    private void givenFoundIdentity(String loginId, Identity identity) {
        given(identityRepository.findByProviderTypeAndLoginId(ProviderType.LOCAL, loginId))
                .willReturn(Optional.of(identity));
    }

    private void givenIssuedTokens(User user) {
        given(jwtTokenService.issueAccessToken(eq(user.getUserUuid()), any(LocalDateTime.class)))
                .willAnswer(invocation -> {
                    LocalDateTime issuedAt = invocation.getArgument(1);
                    return new JwtTokenService.JwtToken("access-token", issuedAt.plusSeconds(3600));
                });
    }

    private LoginHistory capturedLoginHistory() {
        then(loginHistoryRepository).should().save(loginHistoryCaptor.capture());
        return loginHistoryCaptor.getValue();
    }

    private LocalLoginFixture localLoginFixture(String loginId, String email) {
        return localLoginFixture(loginId, email, AccountStatus.ACTIVE);
    }

    private LocalLoginFixture localLoginFixture(String loginId, String email, AccountStatus accountStatus) {
        return localLoginFixture(loginId, email, accountStatus, true);
    }

    private LocalLoginFixture localLoginFixtureWithoutPassword(
            String loginId,
            String email,
            AccountStatus accountStatus
    ) {
        return localLoginFixture(loginId, email, accountStatus, false);
    }

    private LocalLoginFixture localLoginFixture(
            String loginId,
            String email,
            AccountStatus accountStatus,
            boolean withPassword
    ) {
        User user = User.builder()
                .accountStatus(accountStatus)
                .email(email)
                .emailVerified(false)
                .phoneVerified(false)
                .build();

        Identity identity = Identity.builder()
                .providerType(ProviderType.LOCAL)
                .loginId(loginId)
                .principalEmail(email)
                .linked(true)
                .build();

        Password password = null;
        if (withPassword) {
            password = Password.createEncoded(
                    ENCODED_PASSWORD,
                    PasswordAlgo.bcrypt,
                    timeProvider.now().minusDays(1)
            );
            identity.addPassword(password);
        }
        user.addIdentity(identity);

        return new LocalLoginFixture(user, identity, password);
    }

    private record LocalLoginFixture(
            User user,
            Identity identity,
            Password password
    ) {
    }
}
