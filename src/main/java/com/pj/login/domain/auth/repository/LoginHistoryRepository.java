package com.pj.login.domain.auth.repository;

import com.pj.login.domain.auth.entity.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    Optional<LoginHistory> findTopByOrderByIdDesc();
}
