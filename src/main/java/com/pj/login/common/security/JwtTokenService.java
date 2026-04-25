package com.pj.login.common.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.pj.login.common.security.exception.JwtGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class JwtTokenService {

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final String issuer;
    private final long accessTokenExpirySeconds;
    private final byte[] secretBytes;

    public JwtTokenService(
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.access-token-expiry-seconds}") long accessTokenExpirySeconds,
            @Value("${app.jwt.secret}") String secret
    ) {
        this.issuer = issuer;
        this.accessTokenExpirySeconds = accessTokenExpirySeconds;
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret은 최소 32바이트 이상이어야 합니다.");
        }
    }

    // 사용자 UUID를 주체로 하는 JWT 액세스 토큰을 발급한다. (Nimbus JOSE JWT 라이브러리를 사용하여 JWT 생성 및 서명)
    public JwtAccessToken issueAccessToken(UUID userUuid, LocalDateTime issuedAt) {
        Date issuedAtDate = Date.from(issuedAt.atZone(KOREA_ZONE_ID).toInstant());
        LocalDateTime expiresAt = issuedAt.plusSeconds(accessTokenExpirySeconds);
        Date expiresAtDate = Date.from(expiresAt.atZone(KOREA_ZONE_ID).toInstant());

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(userUuid.toString())
                .jwtID(UUID.randomUUID().toString())
                .issueTime(issuedAtDate)
                .expirationTime(expiresAtDate)
                .build();

        try {
            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.HS256)
                            .type(JOSEObjectType.JWT)
                            .build(),
                    claimsSet
            );
            signedJWT.sign(new MACSigner(secretBytes));
            return new JwtAccessToken(signedJWT.serialize(), expiresAt);
        } catch (JOSEException ex) {
            throw new JwtGenerationException("JWT 생성 중 오류가 발생했습니다.", ex);
        }
    }

    public record JwtAccessToken(
            String token,
            LocalDateTime expiresAt
    ) {
    }
}
