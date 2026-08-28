package com.portfolio.warehouse.location.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LocationSegmentRequest(
    @NotBlank
    @Size(max = 20)
    @Pattern(
        regexp = "[A-Za-z0-9]+",
        message = "로케이션 단계값은 영문/숫자만 사용할 수 있습니다."
    )
    String segment
) {
}
