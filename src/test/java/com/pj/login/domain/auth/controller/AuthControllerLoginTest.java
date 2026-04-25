package com.pj.login.domain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pj.login.domain.auth.constant.AccountStatus;
import com.pj.login.domain.auth.constant.PasswordAlgo;
import com.pj.login.domain.auth.constant.ProviderType;
import com.pj.login.domain.auth.dto.LoginRequest;
import com.pj.login.domain.auth.entity.Identity;
import com.pj.login.domain.auth.entity.Password;
import com.pj.login.domain.auth.entity.User;
import com.pj.login.domain.auth.repository.UserRepository;
import com.pj.login.domain.auth.service.PasswordHashingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class AuthControllerLoginTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordHashingService passwordHashingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("로그인 성공 시 액세스 토큰과 리프레시 토큰을 반환한다")
    void login_success_returns_access_and_refresh_tokens() throws Exception {
        seedUser("controller-login@example.com");

        LoginRequest request = new LoginRequest("controller-login@example.com", "Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userUuid").isNotEmpty())
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.accessTokenExpiresAt").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshTokenExpiresAt").isNotEmpty())
                .andExpect(jsonPath("$.data.loginAt").isNotEmpty());
    }

    @Test
    @DisplayName("로그인 실패 시 401과 인증 실패 코드를 반환한다")
    void login_invalid_credentials_returns_unauthorized() throws Exception {
        seedUser("controller-login@example.com");

        LoginRequest request = new LoginRequest("controller-login@example.com", "WrongPassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_LOGIN_CREDENTIALS"))
                .andExpect(jsonPath("$.error.message").value("로그인 ID 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    @DisplayName("로그인 ID 누락 시 검증 에러를 반환한다")
    void login_missing_login_id_returns_validation_error() throws Exception {
        LoginRequest request = new LoginRequest("", "Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.message").value("로그인 ID는 필수입니다."));
    }

    private void seedUser(String loginId) {
        User user = User.builder()
                .accountStatus(AccountStatus.ACTIVE)
                .email(loginId)
                .emailVerified(false)
                .phoneVerified(false)
                .build();

        Identity identity = Identity.builder()
                .providerType(ProviderType.LOCAL)
                .loginId(loginId)
                .principalEmail(loginId)
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
