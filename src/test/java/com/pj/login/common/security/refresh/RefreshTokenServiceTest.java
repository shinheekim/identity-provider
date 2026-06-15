package com.pj.login.common.security.refresh;

import com.pj.login.common.config.JwtProperties;
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
        assertThat(refreshTokenStore.familyId).isNotNull();
        assertThat(refreshTokenStore.ttl).isEqualTo(Duration.ofSeconds(1209600));
    }

    @Test
    @DisplayName("리프레시 토큰에 저장된 사용자 UUID를 조회한다")
    void find_user_uuid_by_refresh_token() {
        CapturingRefreshTokenStore refreshTokenStore = new CapturingRefreshTokenStore();
        RefreshTokenService refreshTokenService = new RefreshTokenService(
                new JwtProperties("taesin", 1800, 1209600, TEST_SECRET),
                refreshTokenStore
        );
        UUID userUuid = UUID.randomUUID();
        refreshTokenStore.save("refresh-token", userUuid, UUID.randomUUID(), Duration.ofDays(14));

        Optional<UUID> foundUserUuid = refreshTokenService.findUserUuid("refresh-token");

        assertThat(foundUserUuid).contains(userUuid);
    }

    @Test
    @DisplayName("리프레시 토큰 사용자 UUID 조회는 회전된 토큰 family를 폐기하지 않는다")
    void find_user_uuid_does_not_revoke_family_when_refresh_token_is_rotated() {
        CapturingRefreshTokenStore refreshTokenStore = new CapturingRefreshTokenStore();
        RefreshTokenService refreshTokenService = new RefreshTokenService(
                new JwtProperties("taesin", 1800, 1209600, TEST_SECRET),
                refreshTokenStore
        );
        UUID userUuid = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        refreshTokenStore.refreshToken = "refresh-token";
        refreshTokenStore.userUuid = userUuid;
        refreshTokenStore.familyId = familyId;
        refreshTokenStore.status = RefreshTokenStatus.ROTATED;

        Optional<UUID> foundUserUuid = refreshTokenService.findUserUuid("refresh-token");

        assertThat(foundUserUuid).contains(userUuid);
        assertThat(refreshTokenStore.revokedFamilyId).isNull();
    }

    @Test
    @DisplayName("활성 리프레시 토큰으로 사용자 UUID를 조회한다")
    void find_active_user_uuid_by_refresh_token() {
        CapturingRefreshTokenStore refreshTokenStore = new CapturingRefreshTokenStore();
        RefreshTokenService refreshTokenService = new RefreshTokenService(
                new JwtProperties("taesin", 1800, 1209600, TEST_SECRET),
                refreshTokenStore
        );
        UUID userUuid = UUID.randomUUID();
        refreshTokenStore.save("refresh-token", userUuid, UUID.randomUUID(), Duration.ofDays(14));

        Optional<UUID> foundUserUuid = refreshTokenService.findActiveUserUuid("refresh-token");

        assertThat(foundUserUuid).contains(userUuid);
    }

    @Test
    @DisplayName("이미 회전된 리프레시 토큰이면 family를 폐기하고 사용자 UUID를 반환하지 않는다")
    void find_rotated_refresh_token_revokes_family() {
        CapturingRefreshTokenStore refreshTokenStore = new CapturingRefreshTokenStore();
        RefreshTokenService refreshTokenService = new RefreshTokenService(
                new JwtProperties("taesin", 1800, 1209600, TEST_SECRET),
                refreshTokenStore
        );
        UUID familyId = UUID.randomUUID();
        refreshTokenStore.refreshToken = "refresh-token";
        refreshTokenStore.userUuid = UUID.randomUUID();
        refreshTokenStore.familyId = familyId;
        refreshTokenStore.status = RefreshTokenStatus.ROTATED;

        Optional<UUID> foundUserUuid = refreshTokenService.findActiveUserUuid("refresh-token");

        assertThat(foundUserUuid).isEmpty();
        assertThat(refreshTokenStore.revokedFamilyId).isEqualTo(familyId);
    }

    @Test
    @DisplayName("리프레시 토큰을 같은 family 안에서 원자적으로 회전한다")
    void rotate_refresh_token() {
        CapturingRefreshTokenStore refreshTokenStore = new CapturingRefreshTokenStore();
        RefreshTokenService refreshTokenService = new RefreshTokenService(
                new JwtProperties("taesin", 1800, 1209600, TEST_SECRET),
                refreshTokenStore
        );
        UUID userUuid = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        LocalDateTime issuedAt = LocalDateTime.of(2026, 5, 7, 12, 0);
        refreshTokenStore.save("old-refresh-token", userUuid, familyId, Duration.ofDays(14));

        Optional<RefreshTokenService.RefreshToken> refreshToken =
                refreshTokenService.rotateRefreshToken("old-refresh-token", issuedAt);

        assertThat(refreshToken).isPresent();
        assertThat(refreshToken.get().token()).isEqualTo(refreshTokenStore.nextRefreshToken);
        assertThat(refreshToken.get().token()).isNotEqualTo("old-refresh-token");
        assertThat(refreshToken.get().expiresAt()).isEqualTo(issuedAt.plusSeconds(1209600));
        assertThat(refreshTokenStore.refreshToken).isEqualTo("old-refresh-token");
        assertThat(refreshTokenStore.status).isEqualTo(RefreshTokenStatus.ROTATED);
        assertThat(refreshTokenStore.nextRefreshToken).isNotBlank();
        assertThat(refreshTokenStore.nextTtl).isEqualTo(Duration.ofSeconds(1209600));
    }

    @Test
    @DisplayName("리프레시 토큰을 폐기한다")
    void revoke_refresh_token() {
        CapturingRefreshTokenStore refreshTokenStore = new CapturingRefreshTokenStore();
        RefreshTokenService refreshTokenService = new RefreshTokenService(
                new JwtProperties("taesin", 1800, 1209600, TEST_SECRET),
                refreshTokenStore
        );
        refreshTokenStore.save("refresh-token", UUID.randomUUID(), UUID.randomUUID(), Duration.ofDays(14));

        refreshTokenService.revokeRefreshToken("refresh-token");

        assertThat(refreshTokenStore.deletedRefreshToken).isEqualTo("refresh-token");
    }

    private static class CapturingRefreshTokenStore implements RefreshTokenStore {

        private String refreshToken;
        private String nextRefreshToken;
        private UUID userUuid;
        private UUID familyId;
        private UUID revokedFamilyId;
        private Duration ttl;
        private Duration nextTtl;
        private String deletedRefreshToken;
        private RefreshTokenStatus status;

        @Override
        public void save(String refreshToken, UUID userUuid, UUID familyId, Duration ttl) {
            this.refreshToken = refreshToken;
            this.userUuid = userUuid;
            this.familyId = familyId;
            this.ttl = ttl;
            this.status = RefreshTokenStatus.ACTIVE;
        }

        @Override
        public Optional<StoredRefreshToken> find(String refreshToken) {
            if (refreshToken.equals(this.refreshToken)) {
                return Optional.of(new StoredRefreshToken(userUuid, familyId, status));
            }
            return Optional.empty();
        }

        @Override
        public Optional<StoredRefreshToken> rotate(
                String currentRefreshToken,
                String nextRefreshToken,
                Duration ttl
        ) {
            if (!currentRefreshToken.equals(this.refreshToken) || status != RefreshTokenStatus.ACTIVE) {
                return Optional.empty();
            }
            this.status = RefreshTokenStatus.ROTATED;
            this.nextRefreshToken = nextRefreshToken;
            this.nextTtl = ttl;
            return Optional.of(new StoredRefreshToken(
                    userUuid,
                    familyId,
                    RefreshTokenStatus.ACTIVE
            ));
        }

        @Override
        public void delete(String refreshToken) {
            this.deletedRefreshToken = refreshToken;
        }

        @Override
        public void revokeFamily(UUID familyId) {
            this.revokedFamilyId = familyId;
        }
    }
}
