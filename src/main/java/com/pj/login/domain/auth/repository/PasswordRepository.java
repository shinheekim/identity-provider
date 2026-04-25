package com.pj.login.domain.auth.repository;

import com.pj.login.domain.auth.entity.Password;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordRepository extends JpaRepository<Password, Long> {
}
