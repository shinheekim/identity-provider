package com.pj.login.common.security.refresh;

import com.pj.login.common.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "auth:rt:v1:";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtProperties jwtProperties;

    @Override
    public void save(String refreshToken, UUID userUuid, Duration ttl) {
        stringRedisTemplate.opsForValue()
                .set(refreshTokenKey(refreshToken), userUuid.toString(), ttl);
    }

    private String refreshTokenKey(String refreshToken) {
        // Redis key 노출 시 refresh token 원문이 새지 않도록 HMAC digest만 저장
        return KEY_PREFIX + hmacSha256(refreshToken);
    }

    private String hmacSha256(String refreshToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(jwtProperties.secretBytes(), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(refreshToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Refresh token 해시 생성 중 오류가 발생했습니다.", ex);
        }
    }
}
