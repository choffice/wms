package com.portfolio.warehouse.report.api.dto;

import java.time.LocalDateTime;

public record DailyPdaRow(
    Integer pdaNumber,
    String mateNickname,
    LocalDateTime assignedAt,
    LocalDateTime releasedAt,
    String releaseReason
) {}
