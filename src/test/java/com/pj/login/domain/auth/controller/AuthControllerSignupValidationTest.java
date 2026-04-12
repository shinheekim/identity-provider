package com.pj.login.domain.auth.controller;

import com.pj.login.domain.auth.dto.SignupRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AuthControllerSignupValidationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("비밀번호 규칙 위반 시 메시지를 반환한다")
    void signup_invalid_password_message() throws Exception {
        // 비밀번호 규칙 위반 메시지
        SignupRequest request = new SignupRequest(
                "test@example.com",
                "test@example.com",
                "Password123",
                "01012345678"
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.message")
                        .value("비밀번호는 영문, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다."));
    }

        @Test
    @DisplayName("로그인 ID 누락 시 에러 메시지를 반환한다")
    void signup_missing_login_id_message() throws Exception {
        // 사유: 필수값 누락에 대한 사용자 안내 메시지 제공 보장
        SignupRequest request = new SignupRequest(
                "",
                "test3@example.com",
                "Password123!",
                null
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.message").value("로그인 ID는 필수입니다."));
    }
}
