package com.portfolio.warehouse.notice.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoticeRequest(
    @NotBlank @Size(max = 2000) String content,
    boolean visible,
    boolean important
) {}
