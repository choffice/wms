package com.portfolio.warehouse.auth.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MateLoginRequest(
    @NotNull @Min(1) Integer deviceNumber,
    @NotBlank String employeeNo,
    @NotBlank String password
) {}
