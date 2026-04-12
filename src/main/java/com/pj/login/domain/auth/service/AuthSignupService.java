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
        if (identityRepository.existsByProviderTypeAndLoginId(ProviderType.LOCAL, request.loginId())) {
            throw new DuplicateLoginIdException();
        }
        // 서버 설정으로 추후 제한조건을 할 것.
        // 해당부분은 loginid = email이 같을떈 필수 조건으로 해야한다.
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }

        User user = User.builder()
                .accountStatus(AccountStatus.ACTIVE)
                .email(request.email())
                .emailVerified(false)
                .phoneNumber(request.phoneNumber())
                .phoneVerified(false)
                .build();

        Identity identity = Identity.builder()
                .providerType(ProviderType.LOCAL)
                .loginId(request.loginId())
                .principalEmail(request.email())
                .linked(true)
                .build();

        PasswordHashingService.EncodedPassword encodedPassword =
                passwordHashingService.encode(request.password());

        Password password = Password.createEncoded(
                encodedPassword.hash(),
                encodedPassword.algo(),
                LocalDateTime.now()
        );

        identity.addPassword(password);
        user.addIdentity(identity);
        userRepository.save(user);

        return new SignupResponse(user.getUserUuid(), user.getAccountStatus());
    }
}
