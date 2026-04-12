package com.pj.login.domain.auth.service;

import com.pj.login.domain.auth.constant.AccountStatus;
import com.pj.login.domain.auth.constant.PasswordAlgo;
import com.pj.login.domain.auth.constant.ProviderType;
import com.pj.login.domain.auth.dto.SignupRequest;
import com.pj.login.domain.auth.dto.SignupResponse;
import com.pj.login.domain.auth.entity.Identity;
import com.pj.login.domain.auth.entity.Password;
import com.pj.login.domain.auth.entity.User;
import com.pj.login.domain.auth.exception.DuplicateEmailException;
import com.pj.login.domain.auth.exception.DuplicateLoginIdException;
import com.pj.login.domain.auth.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@SpringBootTest
@Transactional
class AuthSignupServiceTest {

    private final UserRepository userRepository;
    private final AuthSignupService authSignupService;
    private final PasswordHashingService passwordHashingService;

    @Autowired
    AuthSignupServiceTest(
            UserRepository userRepository,
            AuthSignupService authSignupService,
            PasswordHashingService passwordHashingService
    ) {
        this.userRepository = userRepository;
        this.authSignupService = authSignupService;
        this.passwordHashingService = passwordHashingService;
    }


    // 사유: 회원가입이 사용자/로그인수단/비밀번호 이력까지 한 번에 생성되는지 보장
    @Test
    @DisplayName("일반 회원가입 성공 시 User/Identity/Password가 생성된다")
    void signup_creates_user_identity_password() {
        SignupRequest request = new SignupRequest(
                "test@example.com",
                "test@example.com",
                "Password123!",
                "01012345678"
        );

        SignupResponse response = authSignupService.signup(request);

        Assertions.assertThat(response.userUuid()).isNotNull();
        Assertions.assertThat(response.accountStatus()).isEqualTo(AccountStatus.ACTIVE);

        User savedUser = userRepository.findByEmail("test@example.com").orElseThrow();
        Assertions.assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        Assertions.assertThat(savedUser.isEmailVerified()).isFalse();
        Assertions.assertThat(savedUser.isPhoneVerified()).isFalse();
        Assertions.assertThat(savedUser.getIdentities()).hasSize(1);

        Identity identity = savedUser.getIdentities().get(0);
        Assertions.assertThat(identity.getProviderType()).isEqualTo(ProviderType.LOCAL);
        Assertions.assertThat(identity.getLoginId()).isEqualTo("test@example.com");
        Assertions.assertThat(identity.getPrincipalEmail()).isEqualTo("test@example.com");
        Assertions.assertThat(identity.isLinked()).isTrue();
        Assertions.assertThat(identity.getPasswords()).hasSize(1);

        Password password = identity.getPasswords().get(0);
        Assertions.assertThat(password.getPasswordHash()).isNotEqualTo("Password123!");
        Assertions.assertThat(passwordHashingService.matches(
                "Password123!",
                password.getPasswordHash(),
                password.getPasswordAlgo()
        )).isTrue();
        Assertions.assertThat(password.getPasswordAlgo()).isEqualTo(PasswordAlgo.bcrypt);
        Assertions.assertThat(password.getFailCount()).isZero();
        Assertions.assertThat(password.getPasswordChangedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }



    // 목적 : 이메일 중복 가입 차단

    @Test
    @DisplayName("이미 가입된 이메일이면 예외를 던진다")
    void signup_duplicate_email() {
        seedUser("exists@example.com", "exists@example.com");

        SignupRequest request = new SignupRequest(
                "new-login@example.com",
                "exists@example.com",
                "Password123!",
                null
        );

        Assertions.assertThatThrownBy(() -> authSignupService.signup(request))
                .isInstanceOf(DuplicateEmailException.class);
    }


    // 동일 loginId 중복으로 존재하는 경우에는 예외를 던짐
    @Test
    @DisplayName("이미 사용 중인 로그인 ID이면 예외를 던진다")
    void signup_duplicate_login_id() {
        seedUser("dup-login@example.com", "dup-login@example.com");

        SignupRequest request = new SignupRequest(
                "dup-login@example.com",
                "new-email@example.com",
                "Password123!",
                null
        );

        Assertions.assertThatThrownBy(() -> authSignupService.signup(request))
                .isInstanceOf(DuplicateLoginIdException.class);
    }


    //테스트용 기본 유저
    private void seedUser(String loginId, String email) {
        User user = User.builder()
                .accountStatus(AccountStatus.ACTIVE)
                .email(email)
                .emailVerified(false)
                .phoneVerified(false)
                .build();

        Identity identity = Identity.builder()
                .providerType(ProviderType.LOCAL)
                .loginId(loginId)
                .principalEmail(email)
                .linked(true)
                .build();

        PasswordHashingService.EncodedPassword encodedPassword =
                passwordHashingService.encode("Password123!", PasswordAlgo.bcrypt);
        Password password = Password.createEncoded(
                encodedPassword.hash(),
                encodedPassword.algo(),
                LocalDateTime.now()
        );

        identity.addPassword(password);
        user.addIdentity(identity);
        userRepository.save(user);
    }

}
