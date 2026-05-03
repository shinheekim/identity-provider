package com.pj.login.common.time;

import java.time.LocalDateTime;
import java.time.ZoneId;

public final class KoreaTime {

    public static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    private KoreaTime() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE_ID);
    }
}
