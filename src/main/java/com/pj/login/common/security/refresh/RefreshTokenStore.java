package com.pj.login.common.security.refresh;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenStore {

    void save(String refreshToken, UUID userUuid, Duration ttl);

    Optional<UUID> consumeUserUuid(String refreshToken);

    void delete(String refreshToken);
}
