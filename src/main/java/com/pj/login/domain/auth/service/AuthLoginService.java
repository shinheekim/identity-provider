package com.pj.login.domain.auth.service;

import com.pj.login.common.security.JwtTokenService;
import com.pj.login.common.time.TimeProvider;
import com.pj.login.domain.auth.constant.AccountStatus;
import com.pj.login.domain.auth.constant.LoginFailureReason;
import com.pj.login.domain.auth.constant.ProviderType;
import com.pj.login.domain.auth.dto.LoginRequest;
import com.pj.login.domain.auth.dto.LoginResponse;
import com.pj.login.domain.auth.entity.Identity;
import com.pj.login.domain.auth.entity.LoginHistory;
import com.pj.login.domain.auth.entity.Password;
import com.pj.login.domain.auth.entity.User;
import com.pj.login.domain.auth.exception.AuthLoginException;
import com.pj.login.domain.auth.exception.InvalidLoginCredentialsException;
import com.pj.login.domain.auth.exception.LoginNotAllowedException;
import com.pj.login.domain.auth.exception.PasswordLockedException;
import com.pj.login.domain.auth.repository.IdentityRepository;
import com.pj.login.domain.auth.repository.LoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthLoginService {

    private static final ProviderType LOGIN_PROVIDER = ProviderType.LOCAL;
    private static final int MAX_PASSWORD_FAIL_COUNT = 5;
    private static final Duration PASSWORD_LOCK_DURATION = Duration.ofMinutes(10);

    private final IdentityRepository identityRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final JwtTokenService jwtTokenService;
    private final PasswordHashingService passwordHashingService;
    private final TimeProvider timeProvider;

    @Transactional(noRollbackFor = AuthLoginException.class)
    public LoginResponse login(LoginRequest request, String clientIp, String userAgent) {
        LoginContext loginContext = new LoginContext(
                request.loginId(),
                request.password(),
                clientIp,
                userAgent,
                timeProvider.now()
        );

        Identity identity = getIdentityOrThrow(loginContext);
        User user = identity.getUser();
        Password password = getCurrentPasswordOrThrow(identity, user.getId(), loginContext);

        validatePasswordAvailability(user.getId(), password, loginContext);
        validateAccountStatus(user, loginContext);
        validatePassword(user.getId(), password, loginContext);

        return completeLogin(user, password, loginContext);
    }

    private Identity getIdentityOrThrow(LoginContext loginContext) {
        return identityRepository.findByProviderTypeAndLoginId(LOGIN_PROVIDER, loginContext.loginId())
                .orElseThrow(() -> {
                    recordFailureHistory(null, loginContext, LoginFailureReason.IDENTITY_NOT_FOUND);
                    return new InvalidLoginCredentialsException();
                });
    }

    private Password getCurrentPasswordOrThrow(Identity identity, Long userId, LoginContext loginContext) {
        Password password = identity.getPasswords()
                .stream()
                .max(Comparator.comparing(Password::getPasswordChangedAt))
                .orElse(null);

        if (password == null) {
            recordFailureHistory(userId, loginContext, LoginFailureReason.PASSWORD_NOT_SET);
            throw new InvalidLoginCredentialsException();
        }

        return password;
    }

    private void validatePasswordAvailability(Long userId, Password password, LoginContext loginContext) {
        password.clearLockIfExpired(loginContext.attemptedAt());

        if (password.isLockedAt(loginContext.attemptedAt())) {
            recordFailureHistory(userId, loginContext, LoginFailureReason.PASSWORD_LOCKED);
            throw new PasswordLockedException();
        }
    }

    private void validateAccountStatus(User user, LoginContext loginContext) {
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            recordFailureHistory(user.getId(), loginContext, LoginFailureReason.ACCOUNT_NOT_ACTIVE);
            throw new LoginNotAllowedException();
        }
    }

    private void validatePassword(Long userId, Password password, LoginContext loginContext) {
        if (passwordHashingService.matches(
                loginContext.rawPassword(),
                password.getPasswordHash(),
                password.getPasswordAlgo()
        )) {
            return;
        }

        password.recordFailure(loginContext.attemptedAt(), MAX_PASSWORD_FAIL_COUNT, PASSWORD_LOCK_DURATION);
        LoginFailureReason failureReason = password.isLockedAt(loginContext.attemptedAt())
                ? LoginFailureReason.PASSWORD_LOCKED
                : LoginFailureReason.INVALID_PASSWORD;

        recordFailureHistory(userId, loginContext, failureReason);

        if (failureReason == LoginFailureReason.PASSWORD_LOCKED) {
            throw new PasswordLockedException();
        }
        throw new InvalidLoginCredentialsException();
    }

    private LoginResponse completeLogin(User user, Password password, LoginContext loginContext) {
        password.clearFailureState();
        user.updateLastLoginAt(loginContext.attemptedAt());
        recordSuccessHistory(user.getId(), loginContext);

        JwtTokenService.JwtToken accessToken =
                jwtTokenService.issueAccessToken(user.getUserUuid(), loginContext.attemptedAt());

        return new LoginResponse(
                user.getUserUuid(),
                accessToken.token(),
                null,
                user.getAccountStatus()
        );
    }

    private void recordSuccessHistory(Long userId, LoginContext loginContext) {
        loginHistoryRepository.save(LoginHistory.success(
                userId,
                LOGIN_PROVIDER,
                loginContext.loginId(),
                loginContext.clientIp(),
                loginContext.userAgent()
        ));
    }

    private void recordFailureHistory(Long userId, LoginContext loginContext, LoginFailureReason failReason) {
        loginHistoryRepository.save(LoginHistory.failure(
                userId,
                LOGIN_PROVIDER,
                loginContext.loginId(),
                failReason,
                loginContext.clientIp(),
                loginContext.userAgent()
        ));
    }

    private record LoginContext(
            String loginId,
            String rawPassword,
            String clientIp,
            String userAgent,
            LocalDateTime attemptedAt
    ) {
    }
}
