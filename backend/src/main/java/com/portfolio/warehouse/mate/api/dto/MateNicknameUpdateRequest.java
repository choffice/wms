package com.portfolio.warehouse.mate.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MateNicknameUpdateRequest(
    @NotBlank
    @Size(max = 50)
    String nickname
) {
}
