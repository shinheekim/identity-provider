package com.pj.login.domain.auth.entity;

import com.pj.login.common.converter.YesNoBooleanConverter;
import com.pj.login.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "identity")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Identity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "provider_type", nullable = false, length = 20)
    private String providerType;

    @Column(name = "provider_user_id", length = 255)
    private String providerUserId;

    @Column(name = "login_id", length = 100)
    private String loginId;

    @Column(name = "principal_email", length = 255)
    private String principalEmail;

    @Convert(converter = YesNoBooleanConverter.class)
    @Column(name = "linked_yn", nullable = false, length = 1)
    private boolean linked;

    @OneToMany(mappedBy = "identity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Password> passwords = new ArrayList<>();

    @Builder
    private Identity(
            String providerType,
            String providerUserId,
            String loginId,
            String principalEmail,
            boolean linked
    ) {
        this.providerType = providerType;
        this.providerUserId = providerUserId;
        this.loginId = loginId;
        this.principalEmail = principalEmail;
        this.linked = linked;
    }

    void assignUser(User user) {
        this.user = user;
    }

    public void addPassword(Password password) {
        passwords.add(password);
        password.assignIdentity(this);
    }
}
