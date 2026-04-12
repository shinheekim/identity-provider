package com.pj.login.domain.auth.dto;

import com.pj.login.domain.auth.constant.AccountStatus;

import java.util.UUID;

public record SignupResponse(
        UUID userUuid,
        AccountStatus accountStatus
) {
}
