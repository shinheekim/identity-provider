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
    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final JwtProperties jwtProperties;
    private final byte[] secretBytes;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretBytes = jwtProperties.secretBytes();
    }

    public JwtToken issueAccessToken(UUID userUuid, LocalDateTime issuedAt) {
        return issueToken(userUuid, issuedAt, jwtProperties.accessTokenExpirySeconds(), ACCESS_TOKEN_TYPE);
    }

    public JwtToken issueRefreshToken(UUID userUuid, LocalDateTime issuedAt) {
        return issueToken(userUuid, issuedAt, jwtProperties.refreshTokenExpirySeconds(), REFRESH_TOKEN_TYPE);
    }

    private JwtToken issueToken(
            UUID userUuid,
            LocalDateTime issuedAt,
            long expirySeconds,
            String tokenType
    ) {
        Date issuedAtDate = Date.from(issuedAt.atZone(KOREA_ZONE_ID).toInstant());
        LocalDateTime expiresAt = issuedAt.plusSeconds(expirySeconds);
        Date expiresAtDate = Date.from(expiresAt.atZone(KOREA_ZONE_ID).toInstant());

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer(jwtProperties.issuer())
                .subject(userUuid.toString())
                .jwtID(UUID.randomUUID().toString())
                .issueTime(issuedAtDate)
                .expirationTime(expiresAtDate)
                .claim(TOKEN_TYPE_CLAIM, tokenType)
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
