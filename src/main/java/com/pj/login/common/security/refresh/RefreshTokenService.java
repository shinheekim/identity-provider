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
        UUID familyId = UUID.randomUUID();
        Duration ttl = Duration.ofSeconds(jwtProperties.refreshTokenExpirySeconds());

        refreshTokenStore.save(token, userUuid, familyId, ttl);

        return new RefreshToken(token, issuedAt.plus(ttl));
    }

    public Optional<UUID> findActiveUserUuid(String refreshToken) {
        Optional<StoredRefreshToken> storedRefreshToken = refreshTokenStore.find(refreshToken);
        storedRefreshToken
                .filter(StoredRefreshToken::isRotated)
                .ifPresent(token -> refreshTokenStore.revokeFamily(token.familyId()));

        return storedRefreshToken
                .filter(StoredRefreshToken::isActive)
                .map(StoredRefreshToken::userUuid);
    }

    public Optional<RefreshToken> rotateRefreshToken(String currentRefreshToken, LocalDateTime issuedAt) {
        String nextRefreshToken = generateToken();
        Duration ttl = Duration.ofSeconds(jwtProperties.refreshTokenExpirySeconds());

        return refreshTokenStore.rotate(currentRefreshToken, nextRefreshToken, ttl)
                .map(rotatedToken -> new RefreshToken(nextRefreshToken, issuedAt.plus(ttl)));
    }

    public boolean revokeRefreshToken(String refreshToken, UUID userUuid) {
        return refreshTokenStore.deleteIfCurrentActive(refreshToken, userUuid);
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
