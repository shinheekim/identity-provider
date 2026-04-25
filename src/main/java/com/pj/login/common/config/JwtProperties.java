package com.pj.login.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String issuer,
        long accessTokenExpirySeconds,
        long refreshTokenExpirySeconds,
        String secret
) {
    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret은 필수입니다.");
        }
        if (accessTokenExpirySeconds <= 0) {
            throw new IllegalArgumentException("액세스 토큰 만료 시간은 0보다 커야 합니다.");
        }
        if (refreshTokenExpirySeconds <= 0) {
            throw new IllegalArgumentException("리프레시 토큰 만료 시간은 0보다 커야 합니다.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret은 최소 32바이트 이상이어야 합니다.");
        }
    }

    public byte[] secretBytes() {
        return secret.getBytes(StandardCharsets.UTF_8);
    }
}
