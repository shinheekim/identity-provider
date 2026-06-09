package com.pj.login.common.security.refresh.redis;

import com.pj.login.common.config.JwtProperties;
import com.pj.login.common.security.refresh.RefreshTokenStore;
import com.pj.login.common.security.refresh.RefreshTokenStatus;
import com.pj.login.common.security.refresh.StoredRefreshToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.script.RedisScript;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    private HashOperations<String, Object, Object> hashOperations;

    @Test
    @DisplayName("Redis key에는 리프레시 토큰 원문이 아니라 HMAC 해시를 사용하고 family 정보를 함께 저장한다")
    void save_uses_hmac_digest_key() throws Exception {
        JwtProperties jwtProperties = new JwtProperties("taesin", 1800, 1209600, TEST_SECRET);
        RedisRefreshTokenStore refreshTokenStore = new RedisRefreshTokenStore(stringRedisTemplate, jwtProperties);
        UUID userUuid = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        Duration ttl = Duration.ofDays(14);
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        given(stringRedisTemplate.execute(any(RedisScript.class), keysCaptor.capture(), eq(userUuid.toString()), eq(familyId.toString()), eq("1209600")))
                .willReturn(1L);

        refreshTokenStore.save(RAW_REFRESH_TOKEN, userUuid, familyId, ttl);

        List<String> keys = keysCaptor.getValue();

        assertThat(keys.get(0)).startsWith("auth:rt:v2:token:");
        assertThat(keys.get(0)).doesNotContain(RAW_REFRESH_TOKEN);
        assertThat(keys.get(0)).isEqualTo("auth:rt:v2:token:" + hmacSha256(RAW_REFRESH_TOKEN, TEST_SECRET));
        assertThat(keys.get(1)).isEqualTo("auth:rt:v2:family:" + familyId);
        assertThat(keys.get(2)).isEqualTo("auth:rt:v2:family:" + familyId + ":tokens");
    }

    @Test
    @DisplayName("HMAC 해시 키로 저장된 리프레시 토큰 상태를 조회한다")
    void find_refresh_token_by_hmac_digest_key() throws Exception {
        JwtProperties jwtProperties = new JwtProperties("taesin", 1800, 1209600, TEST_SECRET);
        RedisRefreshTokenStore refreshTokenStore = new RedisRefreshTokenStore(stringRedisTemplate, jwtProperties);
        UUID userUuid = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        String redisKey = "auth:rt:v1:" + hmacSha256(RAW_REFRESH_TOKEN, TEST_SECRET);
        String v2RedisKey = "auth:rt:v2:token:" + hmacSha256(RAW_REFRESH_TOKEN, TEST_SECRET);
        given(stringRedisTemplate.opsForHash()).willReturn(hashOperations);
        given(hashOperations.entries(v2RedisKey)).willReturn(Map.of(
                "userUuid", userUuid.toString(),
                "familyId", familyId.toString(),
                "status", "ACTIVE"
        ));

        Optional<StoredRefreshToken> storedRefreshToken = refreshTokenStore.find(RAW_REFRESH_TOKEN);

        assertThat(redisKey).isNotEqualTo(v2RedisKey);
        assertThat(storedRefreshToken).isPresent();
        assertThat(storedRefreshToken.get().userUuid()).isEqualTo(userUuid);
        assertThat(storedRefreshToken.get().familyId()).isEqualTo(familyId);
        assertThat(storedRefreshToken.get().status()).isEqualTo(RefreshTokenStatus.ACTIVE);
    }

    @Test
    @DisplayName("같은 family 안에서 HMAC 해시 키 기반 원자 rotation을 실행한다")
    void rotate_refresh_token_by_hmac_digest_key() throws Exception {
        JwtProperties jwtProperties = new JwtProperties("taesin", 1800, 1209600, TEST_SECRET);
        RedisRefreshTokenStore refreshTokenStore = new RedisRefreshTokenStore(stringRedisTemplate, jwtProperties);
        UUID userUuid = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        String currentRedisKey = "auth:rt:v2:token:" + hmacSha256(RAW_REFRESH_TOKEN, TEST_SECRET);
        String nextRefreshToken = "next-refresh-token-value";
        String nextRedisKey = "auth:rt:v2:token:" + hmacSha256(nextRefreshToken, TEST_SECRET);
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        given(stringRedisTemplate.opsForHash()).willReturn(hashOperations);
        given(hashOperations.entries(currentRedisKey)).willReturn(Map.of(
                "userUuid", userUuid.toString(),
                "familyId", familyId.toString(),
                "status", "ACTIVE"
        ));
        given(stringRedisTemplate.execute(any(RedisScript.class), keysCaptor.capture(), eq("1209600")))
                .willReturn(List.of("ACTIVE", userUuid.toString(), familyId.toString()));

        Optional<StoredRefreshToken> rotatedRefreshToken =
                refreshTokenStore.rotate(RAW_REFRESH_TOKEN, nextRefreshToken, Duration.ofDays(14));

        assertThat(rotatedRefreshToken).isPresent();
        assertThat(rotatedRefreshToken.get().userUuid()).isEqualTo(userUuid);
        assertThat(rotatedRefreshToken.get().familyId()).isEqualTo(familyId);
        assertThat(rotatedRefreshToken.get().status()).isEqualTo(RefreshTokenStatus.ACTIVE);
        assertThat(keysCaptor.getValue()).containsExactly(
                currentRedisKey,
                "auth:rt:v2:family:" + familyId,
                "auth:rt:v2:family:" + familyId + ":tokens",
                nextRedisKey
        );
    }

    @Test
    @DisplayName("동시 재발급으로 rotation 시점에 이미 회전된 토큰이면 family를 폐기하지 않고 실패한다")
    void rotate_already_rotated_token_does_not_revoke_family() throws Exception {
        JwtProperties jwtProperties = new JwtProperties("taesin", 1800, 1209600, TEST_SECRET);
        RedisRefreshTokenStore refreshTokenStore = new RedisRefreshTokenStore(stringRedisTemplate, jwtProperties);
        UUID userUuid = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        String currentRedisKey = "auth:rt:v2:token:" + hmacSha256(RAW_REFRESH_TOKEN, TEST_SECRET);
        String nextRefreshToken = "next-refresh-token-value";
        ArgumentCaptor<RedisScript> scriptCaptor = ArgumentCaptor.forClass(RedisScript.class);
        given(stringRedisTemplate.opsForHash()).willReturn(hashOperations);
        given(hashOperations.entries(currentRedisKey)).willReturn(Map.of(
                "userUuid", userUuid.toString(),
                "familyId", familyId.toString(),
                "status", "ACTIVE"
        ));
        given(stringRedisTemplate.execute(scriptCaptor.capture(), any(List.class), eq("1209600")))
                .willReturn(List.of("ROTATED", userUuid.toString(), familyId.toString()));

        Optional<StoredRefreshToken> rotatedRefreshToken =
                refreshTokenStore.rotate(RAW_REFRESH_TOKEN, nextRefreshToken, Duration.ofDays(14));

        assertThat(rotatedRefreshToken).isEmpty();
        assertThat(scriptCaptor.getValue()).isSameAs(RefreshTokenRedisScripts.ROTATE);
    }

    @Test
    @DisplayName("rotation 스크립트는 이미 회전된 토큰을 만나도 family 폐기 명령을 포함하지 않는다")
    void rotate_script_does_not_revoke_family() {
        String rotateScript = RefreshTokenRedisScripts.ROTATE.getScriptAsString();

        assertThat(rotateScript).doesNotContain("SMEMBERS");
        assertThat(rotateScript).doesNotContain("'status', 'REVOKED'");
    }

    @Test
    @DisplayName("HMAC 해시 키로 리프레시 토큰을 삭제한다")
    void delete_by_hmac_digest_key() throws Exception {
        JwtProperties jwtProperties = new JwtProperties("taesin", 1800, 1209600, TEST_SECRET);
        RedisRefreshTokenStore refreshTokenStore = new RedisRefreshTokenStore(stringRedisTemplate, jwtProperties);
        ArgumentCaptor<RedisScript> scriptCaptor = ArgumentCaptor.forClass(RedisScript.class);
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);

        refreshTokenStore.delete(RAW_REFRESH_TOKEN);

        then(stringRedisTemplate).should()
                .execute(scriptCaptor.capture(), keysCaptor.capture(), eq("auth:rt:v2:family:"), eq(":tokens"));
        assertThat(scriptCaptor.getValue()).isSameAs(RefreshTokenRedisScripts.DELETE);
        assertThat(keysCaptor.getValue()).containsExactly(
                "auth:rt:v2:token:" + hmacSha256(RAW_REFRESH_TOKEN, TEST_SECRET)
        );
    }

    @Test
    @DisplayName("delete 스크립트는 token hash 삭제와 family tokens set 정리를 함께 수행한다")
    void delete_script_removes_token_from_family_tokens_set() {
        String deleteScript = RefreshTokenRedisScripts.DELETE.getScriptAsString();

        assertThat(deleteScript).contains("redis.call('HGET', KEYS[1], 'familyId')");
        assertThat(deleteScript).contains("redis.call('SREM', ARGV[1] .. familyId .. ARGV[2], KEYS[1])");
        assertThat(deleteScript).contains("redis.call('DEL', KEYS[1])");
    }

    @Test
    @DisplayName("family 단위 폐기 스크립트를 실행한다")
    void revoke_family() {
        JwtProperties jwtProperties = new JwtProperties("taesin", 1800, 1209600, TEST_SECRET);
        RedisRefreshTokenStore refreshTokenStore = new RedisRefreshTokenStore(stringRedisTemplate, jwtProperties);
        UUID familyId = UUID.randomUUID();
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        given(stringRedisTemplate.execute(any(RedisScript.class), keysCaptor.capture()))
                .willReturn(1L);

        refreshTokenStore.revokeFamily(familyId);

        assertThat(keysCaptor.getValue()).containsExactly(
                "auth:rt:v2:family:" + familyId,
                "auth:rt:v2:family:" + familyId + ":tokens"
        );
    }

    @Test
    @DisplayName("family 폐기 스크립트는 존재하지 않는 token hash를 재생성하지 않고 set에서 제거한다")
    void revoke_family_script_does_not_recreate_missing_token_hash() {
        String revokeFamilyScript = RefreshTokenRedisScripts.REVOKE_FAMILY.getScriptAsString();

        assertThat(revokeFamilyScript).contains("redis.call('EXISTS', tokenKey) == 1");
        assertThat(revokeFamilyScript).contains("redis.call('HSET', tokenKey, 'status', 'REVOKED')");
        assertThat(revokeFamilyScript).contains("redis.call('SREM', KEYS[2], tokenKey)");
    }

    private String hmacSha256(String value, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}
