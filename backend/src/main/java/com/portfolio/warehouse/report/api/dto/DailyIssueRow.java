package com.portfolio.warehouse.report.api.dto;

import java.time.LocalDateTime;

public record DailyIssueRow(
    Long issueId,
    String issueType,
    String authorNickname,
    String location,
    String comment,
    String status,
    LocalDateTime createdAt
) {}
