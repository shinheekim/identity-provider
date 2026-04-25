package com.pj.login.common.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.pj.login.common.config.JwtProperties;
import com.pj.login.common.security.exception.JwtGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class JwtTokenService {

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final JwtProperties jwtProperties;
    private final byte[] secretBytes;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretBytes = jwtProperties.secretBytes();
    }

    // 사용자 UUID를 주체로 하는 JWT 액세스 토큰을 발급한다. (Nimbus JOSE JWT 라이브러리를 사용하여 JWT 생성 및 서명)
    public JwtAccessToken issueAccessToken(UUID userUuid, LocalDateTime issuedAt) {
        Date issuedAtDate = Date.from(issuedAt.atZone(KOREA_ZONE_ID).toInstant());
        LocalDateTime expiresAt = issuedAt.plusSeconds(jwtProperties.accessTokenExpirySeconds());
        Date expiresAtDate = Date.from(expiresAt.atZone(KOREA_ZONE_ID).toInstant());

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer(jwtProperties.issuer())
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
