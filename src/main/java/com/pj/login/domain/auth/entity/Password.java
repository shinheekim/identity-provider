package com.pj.login.domain.auth.entity;

import com.pj.login.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "passwords")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Password extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "identity_id", nullable = false)
    private Identity identity;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "password_algo", nullable = false, length = 50)
    private String passwordAlgo;

    @Column(name = "password_changed_at", nullable = false)
    private LocalDateTime passwordChangedAt;

    @Column(name = "fail_count", nullable = false)
    private int failCount;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Builder
    private Password(
            String passwordHash,
            String passwordAlgo,
            LocalDateTime passwordChangedAt,
            int failCount,
            LocalDateTime lockedUntil
    ) {
        this.passwordHash = passwordHash;
        this.passwordAlgo = passwordAlgo;
        this.passwordChangedAt = passwordChangedAt;
        this.failCount = failCount;
        this.lockedUntil = lockedUntil;
    }

    void assignIdentity(Identity identity) {
        this.identity = identity;
    }
}
