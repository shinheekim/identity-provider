package com.pj.login.domain.auth.service;

import com.pj.login.common.security.PasswordEncoderRegistry;
import com.pj.login.domain.auth.constant.PasswordAlgo;
import org.springframework.stereotype.Service;

@Service
public class PasswordHashingService {

    private static final PasswordAlgo CURRENT_PASSWORD_ALGO = PasswordAlgo.bcrypt;

    private final PasswordEncoderRegistry passwordEncoderRegistry;

    public PasswordHashingService(PasswordEncoderRegistry passwordEncoderRegistry) {
        this.passwordEncoderRegistry = passwordEncoderRegistry;
    }

    public EncodedPassword encode(String rawPassword) {
        return new EncodedPassword(
                passwordEncoderRegistry.get(CURRENT_PASSWORD_ALGO).encode(rawPassword),
                CURRENT_PASSWORD_ALGO
        );
    }

    public EncodedPassword encode(String rawPassword, PasswordAlgo passwordAlgo) {
        return new EncodedPassword(
                passwordEncoderRegistry.get(passwordAlgo).encode(rawPassword),
                passwordAlgo
        );
    }

    public boolean matches(String rawPassword, String passwordHash, PasswordAlgo passwordAlgo) {
        return passwordEncoderRegistry.get(passwordAlgo).matches(rawPassword, passwordHash);
    }

    public record EncodedPassword(String hash, PasswordAlgo algo) {
    }
}
