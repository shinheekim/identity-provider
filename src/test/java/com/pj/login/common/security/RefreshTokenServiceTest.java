package com.pj.login.common.security;

import com.pj.login.common.config.JwtProperties;
import com.pj.login.common.security.refresh.RefreshTokenService;
import com.pj.login.common.security.refresh.RefreshTokenStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenServiceTest {

    private static final String TEST_SECRET = "test-only-jwt-secret-key-change-me-1234567890-abcdef";

    @Test
    @DisplayName("리프레시 토큰을 생성하고 TTL과 함께 저장한다")
    void issue_refresh_token_saves_token_with_ttl() {
        JwtProperties jwtProperties = new JwtProperties("taesin", 1800, 1209600, TEST_SECRET);
        CapturingRefreshTokenStore refreshTokenStore = new CapturingRefreshTokenStore();
        RefreshTokenService refreshTokenService = new RefreshTokenService(jwtProperties, refreshTokenStore);
        UUID userUuid = UUID.randomUUID();
        LocalDateTime issuedAt = LocalDateTime.of(2026, 5, 7, 12, 0);

        RefreshTokenService.RefreshToken refreshToken =
                refreshTokenService.issueRefreshToken(userUuid, issuedAt);

        assertThat(refreshToken.token()).isNotBlank();
        assertThat(refreshToken.token()).isEqualTo(refreshTokenStore.refreshToken);
        assertThat(refreshToken.expiresAt()).isEqualTo(issuedAt.plusSeconds(1209600));
        assertThat(refreshTokenStore.userUuid).isEqualTo(userUuid);
        assertThat(refreshTokenStore.ttl).isEqualTo(Duration.ofSeconds(1209600));
    }

    @Test
    @DisplayName("리프레시 토큰으로 사용자 UUID를 조회하고 소비한다")
    void consume_user_uuid_by_refresh_token() {
        CapturingRefreshTokenStore refreshTokenStore = new CapturingRefreshTokenStore();
        RefreshTokenService refreshTokenService = new RefreshTokenService(
                new JwtProperties("taesin", 1800, 1209600, TEST_SECRET),
                refreshTokenStore
        );
        UUID userUuid = UUID.randomUUID();
        refreshTokenStore.save("refresh-token", userUuid, Duration.ofDays(14));

        Optional<UUID> foundUserUuid = refreshTokenService.consumeUserUuid("refresh-token");

        assertThat(foundUserUuid).contains(userUuid);
    }

    @Test
    @DisplayName("리프레시 토큰을 폐기한다")
    void revoke_refresh_token() {
        CapturingRefreshTokenStore refreshTokenStore = new CapturingRefreshTokenStore();
        RefreshTokenService refreshTokenService = new RefreshTokenService(
                new JwtProperties("taesin", 1800, 1209600, TEST_SECRET),
                refreshTokenStore
        );
        refreshTokenStore.save("refresh-token", UUID.randomUUID(), Duration.ofDays(14));

        refreshTokenService.revokeRefreshToken("refresh-token");

        assertThat(refreshTokenStore.deletedRefreshToken).isEqualTo("refresh-token");
    }

    private static class CapturingRefreshTokenStore implements RefreshTokenStore {

        private String refreshToken;
        private UUID userUuid;
        private Duration ttl;
        private String deletedRefreshToken;

        @Override
        public void save(String refreshToken, UUID userUuid, Duration ttl) {
            this.refreshToken = refreshToken;
            this.userUuid = userUuid;
            this.ttl = ttl;
        }

        @Override
        public Optional<UUID> consumeUserUuid(String refreshToken) {
            if (refreshToken.equals(this.refreshToken)) {
                return Optional.of(userUuid);
            }
            return Optional.empty();
        }

        @Override
        public void delete(String refreshToken) {
            this.deletedRefreshToken = refreshToken;
        }
    }
}
