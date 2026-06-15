package com.pj.login.common.security.refresh;

import java.util.UUID;

public record StoredRefreshToken(
        UUID userUuid,
        UUID familyId,
        RefreshTokenStatus status
) {

    public boolean isActive() {
        return status == RefreshTokenStatus.ACTIVE;
    }

    public boolean isRotated() {
        return status == RefreshTokenStatus.ROTATED;
    }
}
