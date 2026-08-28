package com.portfolio.warehouse.notice.api.dto;

import jakarta.validation.constraints.NotNull;

public record NoticeOrderItem(
    @NotNull Long id,
    int displayOrder
) {}
