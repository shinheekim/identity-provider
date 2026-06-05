package com.pj.login.common.security.refresh;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenStore {

    void save(String refreshToken, UUID userUuid, UUID familyId, Duration ttl);

    Optional<StoredRefreshToken> find(String refreshToken);

    Optional<StoredRefreshToken> rotate(String currentRefreshToken, String nextRefreshToken, Duration ttl);

    void delete(String refreshToken);

    void revokeFamily(UUID familyId);
}
