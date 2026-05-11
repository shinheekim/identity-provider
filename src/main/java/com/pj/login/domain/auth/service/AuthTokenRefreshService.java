package com.pj.login.domain.auth.service;

import com.pj.login.common.config.JwtProperties;
import com.pj.login.common.security.JwtTokenService;
import com.pj.login.common.security.refresh.RefreshTokenService;
import com.pj.login.common.time.TimeProvider;
import com.pj.login.domain.auth.constant.AccountStatus;
import com.pj.login.domain.auth.dto.TokenRefreshRequest;
import com.pj.login.domain.auth.dto.TokenRefreshResponse;
import com.pj.login.domain.auth.entity.User;
import com.pj.login.domain.auth.exception.InvalidRefreshTokenException;
import com.pj.login.domain.auth.exception.LoginNotAllowedException;
import com.pj.login.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthTokenRefreshService {

    private final RefreshTokenService refreshTokenService;
    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;
    private final TimeProvider timeProvider;
    private final JwtProperties jwtProperties;

    public TokenRefreshResponse refresh(TokenRefreshRequest request) {
        UUID userUuid = refreshTokenService.consumeUserUuid(request.refreshToken())
                .orElseThrow(InvalidRefreshTokenException::new);
        User user = userRepository.findByUserUuid(userUuid)
                .orElseThrow(InvalidRefreshTokenException::new);
        validateAccountStatus(user);

        LocalDateTime issuedAt = timeProvider.now();
        JwtTokenService.JwtToken accessToken = jwtTokenService.issueAccessToken(user.getUserUuid(), issuedAt);
        RefreshTokenService.RefreshToken refreshToken =
                refreshTokenService.issueRefreshToken(user.getUserUuid(), issuedAt);

        return new TokenRefreshResponse(
                accessToken.token(),
                refreshToken.token(),
                jwtProperties.accessTokenExpirySeconds()
        );
    }

    private static void validateAccountStatus(User user) {
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new LoginNotAllowedException();
        }
    }
}
