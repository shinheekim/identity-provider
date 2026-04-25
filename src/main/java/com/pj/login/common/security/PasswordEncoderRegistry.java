package com.pj.login.common.security;

import com.pj.login.domain.auth.constant.PasswordAlgo;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
///
/// 비밀번호 알고리즘
///
@Component
public class PasswordEncoderRegistry {

    private final Map<PasswordAlgo, PasswordEncoder> passwordEncoders;

    public PasswordEncoderRegistry() {
        EnumMap<PasswordAlgo, PasswordEncoder> passwordEncoders = new EnumMap<>(PasswordAlgo.class);
        passwordEncoders.put(PasswordAlgo.bcrypt, new BCryptPasswordEncoder());
        this.passwordEncoders = Map.copyOf(passwordEncoders);
    }

    public PasswordEncoder get(PasswordAlgo passwordAlgo) {
        PasswordEncoder passwordEncoder = passwordEncoders.get(passwordAlgo);
        if (passwordEncoder == null) {
            throw new IllegalArgumentException("지원하지 않는 비밀번호 알고리즘 " + passwordAlgo);
        }
        return passwordEncoder;
    }
}
