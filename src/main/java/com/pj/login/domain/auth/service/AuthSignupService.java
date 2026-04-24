package com.pj.login.domain.auth.service;

import com.pj.login.domain.auth.constant.AccountStatus;
import com.pj.login.domain.auth.constant.ProviderType;
import com.pj.login.domain.auth.dto.SignupRequest;
import com.pj.login.domain.auth.dto.SignupResponse;
import com.pj.login.domain.auth.entity.Identity;
import com.pj.login.domain.auth.entity.Password;
import com.pj.login.domain.auth.entity.User;
import com.pj.login.domain.auth.exception.DuplicateEmailException;
import com.pj.login.domain.auth.exception.DuplicateLoginIdException;
import com.pj.login.domain.auth.repository.IdentityRepository;
import com.pj.login.domain.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthSignupService {

    private final UserRepository userRepository;
    private final IdentityRepository identityRepository;
    private final PasswordHashingService passwordHashingService;

    public AuthSignupService(
            UserRepository userRepository,
            IdentityRepository identityRepository,
            PasswordHashingService passwordHashingService
    ) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.passwordHashingService = passwordHashingService;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        validateSignupRequest(request);

        User user = createUser(request);
        Identity identity = createLocalIdentity(request);
        Password password = createPassword(request.password());

        identity.addPassword(password);
        user.addIdentity(identity);

        return SignupResponse.from(userRepository.save(user));
    }

    private void validateSignupRequest(SignupRequest request) {
        validateDuplicateLoginId(request.loginId());

        // 서버 설정으로 추후 제한조건을 할 것.
        // 해당부분은 loginid = email이 같을떈 필수 조건으로 해야한다.
        validateDuplicateEmail(request.email());
    }

    private void validateDuplicateLoginId(String loginId) {
        if (identityRepository.existsByProviderTypeAndLoginId(ProviderType.LOCAL, loginId)) {
            throw new DuplicateLoginIdException();
        }
    }

    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }
    }

    private User createUser(SignupRequest request) {
        return User.builder()
                .accountStatus(AccountStatus.ACTIVE)
                .email(request.email())
                .emailVerified(false)
                .phoneNumber(request.phoneNumber())
                .phoneVerified(false)
                .build();
    }

    private Identity createLocalIdentity(SignupRequest request) {
        return Identity.builder()
                .providerType(ProviderType.LOCAL)
                .loginId(request.loginId())
                .principalEmail(request.email())
                .linked(true)
                .build();
    }

    private Password createPassword(String rawPassword) {
        PasswordHashingService.EncodedPassword encodedPassword =
                passwordHashingService.encode(rawPassword);

        return Password.createEncoded(
                encodedPassword.hash(),
                encodedPassword.algo(),
                LocalDateTime.now()
        );
    }



}
