package com.pj.login.common.security.refresh.redis;

import com.pj.login.common.config.JwtProperties;
import com.pj.login.common.security.refresh.RefreshTokenStatus;
import com.pj.login.common.security.refresh.RefreshTokenStore;
import com.pj.login.common.security.refresh.StoredRefreshToken;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String TOKEN_KEY_PREFIX = "auth:rt:v2:token:";
    private static final String FAMILY_KEY_PREFIX = "auth:rt:v2:family:";
    private static final String FAMILY_TOKENS_KEY_SUFFIX = ":tokens";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtProperties jwtProperties;

    @Override
    public void save(String refreshToken, UUID userUuid, UUID familyId, Duration ttl) {
        stringRedisTemplate.execute(
                RefreshTokenRedisScripts.SAVE,
                List.of(refreshTokenKey(refreshToken), familyKey(familyId), familyTokensKey(familyId)),
                userUuid.toString(),
                familyId.toString(),
                String.valueOf(ttl.toSeconds())
        );
    }

    @Override
    public Optional<StoredRefreshToken> find(String refreshToken) {
        Map<Object, Object> token = stringRedisTemplate.opsForHash()
                .entries(refreshTokenKey(refreshToken));
        if (token.isEmpty()) {
            return Optional.empty();
        }
        return toStoredRefreshToken(token);
    }

    @Override
    public Optional<StoredRefreshToken> rotate(String currentRefreshToken, String nextRefreshToken, Duration ttl) {
        Optional<StoredRefreshToken> currentToken = find(currentRefreshToken);
        if (currentToken.isEmpty()) {
            return Optional.empty();
        }

        UUID familyId = currentToken.get().familyId();
        List result = stringRedisTemplate.execute(
                RefreshTokenRedisScripts.ROTATE,
                List.of(
                        refreshTokenKey(currentRefreshToken),
                        familyKey(familyId),
                        familyTokensKey(familyId),
                        refreshTokenKey(nextRefreshToken)
                ),
                String.valueOf(ttl.toSeconds())
        );

        return toRotatedRefreshToken(result);
    }

    @Override
    public void delete(String refreshToken) {
        stringRedisTemplate.delete(refreshTokenKey(refreshToken));
    }

    @Override
    public void revokeFamily(UUID familyId) {
        stringRedisTemplate.execute(
                RefreshTokenRedisScripts.REVOKE_FAMILY,
                List.of(familyKey(familyId), familyTokensKey(familyId))
        );
    }

    private Optional<StoredRefreshToken> toStoredRefreshToken(Map<Object, Object> token) {
        Object userUuid = token.get("userUuid");
        Object familyId = token.get("familyId");
        Object status = token.get("status");
        if (userUuid == null || familyId == null || status == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new StoredRefreshToken(
                    UUID.fromString(userUuid.toString()),
                    UUID.fromString(familyId.toString()),
                    RefreshTokenStatus.valueOf(status.toString())
            ));
        } catch (IllegalArgumentException | NullPointerException ex) {
            return Optional.empty();
        }
    }

    private Optional<StoredRefreshToken> toRotatedRefreshToken(List result) {
        if (result == null || result.size() < 3 || !"ACTIVE".equals(String.valueOf(result.get(0)))) {
            return Optional.empty();
        }
        try {
            return Optional.of(new StoredRefreshToken(
                    UUID.fromString(String.valueOf(result.get(1))),
                    UUID.fromString(String.valueOf(result.get(2))),
                    RefreshTokenStatus.ACTIVE
            ));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private String refreshTokenKey(String refreshToken) {
        // Redis key 노출 시 refresh token 원문이 새지 않도록 HMAC digest만 저장
        return TOKEN_KEY_PREFIX + hmacSha256(refreshToken);
    }

    private String familyKey(UUID familyId) {
        return FAMILY_KEY_PREFIX + familyId;
    }

    private String familyTokensKey(UUID familyId) {
        return familyKey(familyId) + FAMILY_TOKENS_KEY_SUFFIX;
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
