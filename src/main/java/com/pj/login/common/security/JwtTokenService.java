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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtTokenService {

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final JwtProperties jwtProperties;
    private final byte[] secretBytes;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretBytes = jwtProperties.secretBytes();
    }

    public JwtToken issueAccessToken(UUID userUuid, LocalDateTime issuedAt) {
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
            return new JwtToken(signedJWT.serialize(), expiresAt);
        } catch (JOSEException ex) {
            throw new JwtGenerationException("JWT 생성 중 오류가 발생했습니다.", ex);
        }
    }

    public record JwtToken(
            String token,
            LocalDateTime expiresAt
    ) {
    }
}
