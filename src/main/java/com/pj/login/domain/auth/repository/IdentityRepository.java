package com.pj.login.domain.auth.repository;

import com.pj.login.domain.auth.entity.Identity;
import com.pj.login.domain.auth.constant.ProviderType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentityRepository extends JpaRepository<Identity, Long> {
    boolean existsByProviderTypeAndLoginId(ProviderType providerType, String loginId);
}
