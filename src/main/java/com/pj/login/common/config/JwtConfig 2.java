package com.pj.login.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    @Bean
    @Primary
    public JwtDecoder jwtDecoder(JwtProperties jwtProperties) {
        return createJwtDecoder(jwtProperties, ACCESS_TOKEN_TYPE);
    }

    @Bean("refreshJwtDecoder")
    public JwtDecoder refreshJwtDecoder(JwtProperties jwtProperties) {
        return createJwtDecoder(jwtProperties, REFRESH_TOKEN_TYPE);
    }

    private JwtDecoder createJwtDecoder(JwtProperties jwtProperties, String expectedTokenType) {
        SecretKey secretKey = new SecretKeySpec(jwtProperties.secretBytes(), "HmacSHA256");
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(jwtProperties.issuer()),
                tokenTypeValidator(expectedTokenType)
        ));
        return jwtDecoder;
    }

    private OAuth2TokenValidator<Jwt> tokenTypeValidator(String expectedTokenType) {
        return jwt -> expectedTokenType.equals(jwt.getClaimAsString(TOKEN_TYPE_CLAIM))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token",
                        "허용되지 않은 토큰 타입입니다.",
                        null
                ));
    }
}
