package com.pj.login.common.security.refresh;

import com.pj.login.common.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int REFRESH_TOKEN_BYTE_LENGTH = 32;

    private final JwtProperties jwtProperties;
    private final RefreshTokenStore refreshTokenStore;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshToken issueRefreshToken(UUID userUuid, LocalDateTime issuedAt) {
        String token = generateToken();
        Duration ttl = Duration.ofSeconds(jwtProperties.refreshTokenExpirySeconds());

        refreshTokenStore.save(token, userUuid, ttl);

        return new RefreshToken(token, issuedAt.plus(ttl));
    }

    public Optional<UUID> consumeUserUuid(String refreshToken) {
        return refreshTokenStore.consumeUserUuid(refreshToken);
    }

    public void revokeRefreshToken(String refreshToken) {
        refreshTokenStore.delete(refreshToken);
    }

    /**
     * 보안을 위해 URL-safe Base64 인코딩된 랜덤 바이트 배열을 사용하여 토큰을 생성합니다.
     * padding 문자를 제거하여 토큰 길이를 줄입니다.
     */

    private String generateToken() {
        byte[] randomBytes = new byte[REFRESH_TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    public record RefreshToken(
            String token,
            LocalDateTime expiresAt
    ) {
    }
}
