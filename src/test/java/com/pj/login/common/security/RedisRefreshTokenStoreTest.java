package com.pj.login.common.security;

import com.pj.login.common.config.JwtProperties;
import com.pj.login.common.security.refresh.RedisRefreshTokenStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class RedisRefreshTokenStoreTest {

    private static final String TEST_SECRET = "test-only-jwt-secret-key-change-me-1234567890-abcdef";
    private static final String RAW_REFRESH_TOKEN = "raw-refresh-token-value";

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("Redis key에는 리프레시 토큰 원문이 아니라 HMAC 해시를 사용한다")
    void save_uses_hmac_digest_key() throws Exception {
        JwtProperties jwtProperties = new JwtProperties("taesin", 1800, 1209600, TEST_SECRET);
        RedisRefreshTokenStore refreshTokenStore = new RedisRefreshTokenStore(stringRedisTemplate, jwtProperties);
        UUID userUuid = UUID.randomUUID();
        Duration ttl = Duration.ofDays(14);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);

        refreshTokenStore.save(RAW_REFRESH_TOKEN, userUuid, ttl);

        then(valueOperations).should().set(keyCaptor.capture(), eq(userUuid.toString()), eq(ttl));
        String redisKey = keyCaptor.getValue();

        assertThat(redisKey).startsWith("auth:rt:v1:");
        assertThat(redisKey).doesNotContain(RAW_REFRESH_TOKEN);
        assertThat(redisKey).isEqualTo("auth:rt:v1:" + hmacSha256(RAW_REFRESH_TOKEN, TEST_SECRET));
    }

    @Test
    @DisplayName("HMAC 해시 키로 저장된 사용자 UUID를 조회하고 삭제한다")
    void consume_user_uuid_by_hmac_digest_key() throws Exception {
        JwtProperties jwtProperties = new JwtProperties("taesin", 1800, 1209600, TEST_SECRET);
        RedisRefreshTokenStore refreshTokenStore = new RedisRefreshTokenStore(stringRedisTemplate, jwtProperties);
        UUID userUuid = UUID.randomUUID();
        String redisKey = "auth:rt:v1:" + hmacSha256(RAW_REFRESH_TOKEN, TEST_SECRET);
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.getAndDelete(redisKey)).willReturn(userUuid.toString());

        assertThat(refreshTokenStore.consumeUserUuid(RAW_REFRESH_TOKEN)).contains(userUuid);
    }

    @Test
    @DisplayName("HMAC 해시 키로 리프레시 토큰을 삭제한다")
    void delete_by_hmac_digest_key() throws Exception {
        JwtProperties jwtProperties = new JwtProperties("taesin", 1800, 1209600, TEST_SECRET);
        RedisRefreshTokenStore refreshTokenStore = new RedisRefreshTokenStore(stringRedisTemplate, jwtProperties);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        refreshTokenStore.delete(RAW_REFRESH_TOKEN);

        then(stringRedisTemplate).should().delete(keyCaptor.capture());
        assertThat(keyCaptor.getValue()).isEqualTo("auth:rt:v1:" + hmacSha256(RAW_REFRESH_TOKEN, TEST_SECRET));
    }

    private String hmacSha256(String value, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}
