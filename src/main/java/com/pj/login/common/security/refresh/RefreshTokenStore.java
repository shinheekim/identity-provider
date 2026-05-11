package com.pj.login.common.security.refresh;

import java.time.Duration;
import java.util.UUID;

public interface RefreshTokenStore {

    void save(String refreshToken, UUID userUuid, Duration ttl);
}
