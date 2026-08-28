package com.portfolio.warehouse.mate.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record MateCreateRequest(
    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 50)
    String name,

    @NotBlank(message = "별명은 필수입니다.")
    @Size(max = 50)
    String nickname,

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 4, max = 50, message = "비밀번호는 4~50자로 입력해주세요.")
    String password,

    LocalDate joinedAt
) {
}
