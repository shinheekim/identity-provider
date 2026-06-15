package com.pj.login.domain.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pj.login.common.security.refresh.RefreshTokenStore;
import com.pj.login.common.security.refresh.RefreshTokenStatus;
import com.pj.login.common.security.refresh.StoredRefreshToken;
import com.pj.login.common.time.TimeProvider;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Autowired
    private TimeProvider timeProvider;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("로그인 성공 시 액세스 토큰과 리프레시 토큰을 반환한다")
    void login_success_returns_access_and_refresh_token() throws Exception {
        seedUser("controller-login@example.com");

        LoginRequest request = new LoginRequest("controller-login@example.com", "Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userUuid").isNotEmpty())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.accessTokenExpiresAt").doesNotExist())
                .andExpect(jsonPath("$.data.loginAt").doesNotExist());
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
    @DisplayName("ACTIVE가 아닌 계정은 403과 로그인 불가 코드를 반환한다")
    void login_not_active_account_returns_forbidden() throws Exception {
        seedUser("dormant-user@example.com", AccountStatus.DORMANT);

        LoginRequest request = new LoginRequest("dormant-user@example.com", "Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("LOGIN_NOT_ALLOWED"))
                .andExpect(jsonPath("$.error.message").value("활성 상태의 계정만 로그인할 수 있습니다."));
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

    @Test
    @DisplayName("리프레시 토큰으로 액세스 토큰과 리프레시 토큰을 재발급한다")
    void refresh_token_success_rotates_refresh_token() throws Exception {
        seedUser("refresh-controller@example.com");
        LoginRequest loginRequest = new LoginRequest("refresh-controller@example.com", "Password123!");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String oldRefreshToken = readData(loginResult, "refreshToken").asText();

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", oldRefreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.expiresIn").value(1800))
                .andReturn();

        String newRefreshToken = readData(refreshResult, "refreshToken").asText();
        assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);

        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", oldRefreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));

        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", newRefreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰이면 401을 반환한다")
    void refresh_token_invalid_returns_unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", "invalid-refresh-token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"))
                .andExpect(jsonPath("$.error.message").value("Refresh Token이 유효하지 않습니다."));
    }

    @Test
    @DisplayName("리프레시 토큰 누락 시 검증 에러를 반환한다")
    void refresh_token_missing_returns_validation_error() throws Exception {
        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.message").value("Refresh Token은 필수입니다."));
    }

    private void seedUser(String loginId) {
        seedUser(loginId, AccountStatus.ACTIVE);
    }

    private void seedUser(String loginId, AccountStatus accountStatus) {
        User user = User.builder()
                .accountStatus(accountStatus)
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
                timeProvider.now()
        );

        identity.addPassword(password);
        user.addIdentity(identity);
        userRepository.save(user);
    }

    private JsonNode readData(MvcResult result, String fieldName) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path(fieldName);
    }

    @TestConfiguration
    static class RefreshTokenStoreTestConfig {

        @Bean
        @Primary
        RefreshTokenStore refreshTokenStore() {
            return new InMemoryRefreshTokenStore();
        }

        static class InMemoryRefreshTokenStore implements RefreshTokenStore {

            private final Map<String, StoredRefreshToken> tokens = new ConcurrentHashMap<>();

            @Override
            public void save(String refreshToken, UUID userUuid, UUID familyId, Duration ttl) {
                tokens.put(refreshToken, new StoredRefreshToken(userUuid, familyId, RefreshTokenStatus.ACTIVE));
            }

            @Override
            public Optional<StoredRefreshToken> find(String refreshToken) {
                return Optional.ofNullable(tokens.get(refreshToken));
            }

            @Override
            public Optional<StoredRefreshToken> rotate(String currentRefreshToken, String nextRefreshToken, Duration ttl) {
                StoredRefreshToken currentToken = tokens.get(currentRefreshToken);
                if (currentToken == null) {
                    return Optional.empty();
                }
                if (currentToken.status() == RefreshTokenStatus.ROTATED) {
                    revokeFamily(currentToken.familyId());
                    return Optional.empty();
                }
                if (currentToken.status() != RefreshTokenStatus.ACTIVE) {
                    return Optional.empty();
                }
                tokens.put(
                        currentRefreshToken,
                        new StoredRefreshToken(
                                currentToken.userUuid(),
                                currentToken.familyId(),
                                RefreshTokenStatus.ROTATED
                        )
                );
                StoredRefreshToken nextToken = new StoredRefreshToken(
                        currentToken.userUuid(),
                        currentToken.familyId(),
                        RefreshTokenStatus.ACTIVE
                );
                tokens.put(nextRefreshToken, nextToken);
                return Optional.of(nextToken);
            }

            @Override
            public void delete(String refreshToken) {
                tokens.remove(refreshToken);
            }

            @Override
            public void revokeFamily(UUID familyId) {
                tokens.replaceAll((refreshToken, token) -> {
                    if (token.familyId().equals(familyId)) {
                        return new StoredRefreshToken(token.userUuid(), token.familyId(), RefreshTokenStatus.REVOKED);
                    }
                    return token;
                });
            }
        }
    }
}
