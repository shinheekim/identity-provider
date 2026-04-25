package com.pj.login.domain.auth.entity;

import com.pj.login.common.entity.CreatedAtEntity;
import com.pj.login.domain.auth.constant.LoginAttemptResult;
import com.pj.login.domain.auth.constant.LoginFailureReason;
import com.pj.login.domain.auth.constant.ProviderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "login_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginHistory extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId; // 히스토리성 데이터로 userId만 저장함

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 20)
    private ProviderType providerType;

    @Column(name = "login_id", length = 100)
    private String loginId;

    @Enumerated(EnumType.STRING)
    @Column(name = "attempt_result", nullable = false, length = 20)
    private LoginAttemptResult attemptResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "fail_reason", length = 100)
    private LoginFailureReason failReason;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Builder
    private LoginHistory(
            Long userId,
            ProviderType providerType,
            String loginId,
            LoginAttemptResult attemptResult,
            LoginFailureReason failReason,
            String clientIp,
            String userAgent
    ) {
        this.userId = userId;
        this.providerType = providerType;
        this.loginId = loginId;
        this.attemptResult = attemptResult;
        this.failReason = failReason;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
    }
}
