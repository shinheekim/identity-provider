package com.pj.login.domain.auth.repository;

import com.pj.login.domain.auth.entity.Identity;
import com.pj.login.domain.auth.constant.ProviderType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdentityRepository extends JpaRepository<Identity, Long> {
    boolean existsByProviderTypeAndLoginId(ProviderType providerType, String loginId);

    @EntityGraph(attributePaths = {"user", "passwords"})
    Optional<Identity> findByProviderTypeAndLoginId(ProviderType providerType, String loginId);
}
