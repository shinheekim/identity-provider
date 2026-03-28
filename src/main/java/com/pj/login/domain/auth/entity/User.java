package com.pj.login.domain.auth.entity;

import com.pj.login.common.converter.YesNoBooleanConverter;
import com.pj.login.common.entity.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_uuid", nullable = false, unique = true, updatable = false, columnDefinition = "uuid")
    private UUID userUuid;

    @Column(name = "account_status", nullable = false, length = 20)
    private String accountStatus;

    @Column(length = 255)
    private String email;

    @Convert(converter = YesNoBooleanConverter.class)
    @Column(name = "email_verified_yn", nullable = false, length = 1)
    private boolean emailVerified;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Convert(converter = YesNoBooleanConverter.class)
    @Column(name = "phone_verified_yn", nullable = false, length = 1)
    private boolean phoneVerified;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Identity> identities = new ArrayList<>();

    @Builder
    private User(
            UUID userUuid,
            String accountStatus,
            String email,
            boolean emailVerified,
            String phoneNumber,
            boolean phoneVerified,
            LocalDateTime lastLoginAt
    ) {
        this.userUuid = userUuid;
        this.accountStatus = accountStatus;
        this.email = email;
        this.emailVerified = emailVerified;
        this.phoneNumber = phoneNumber;
        this.phoneVerified = phoneVerified;
        this.lastLoginAt = lastLoginAt;
    }

    void assignUserUuidIfAbsent() {
        if (userUuid == null) {
            userUuid = UUID.randomUUID();
        }
    }

    public void addIdentity(Identity identity) {
        identities.add(identity);
        identity.assignUser(this);
    }
}
