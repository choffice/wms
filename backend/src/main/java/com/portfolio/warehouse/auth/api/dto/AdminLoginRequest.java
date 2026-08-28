package com.portfolio.warehouse.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(
    @NotBlank String employeeNo,
    @NotBlank String password
) {}
