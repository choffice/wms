package com.portfolio.warehouse.auth.api.dto;

import com.portfolio.warehouse.auth.domain.UserRole;

public record AuthMeResponse(
    String employeeNo,
    UserRole role,
    Long mateId,
    String nickname,
    Long pdaUsageId,
    Integer pdaNumber
) {}
