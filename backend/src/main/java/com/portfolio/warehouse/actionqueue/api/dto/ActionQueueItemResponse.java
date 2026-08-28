package com.portfolio.warehouse.actionqueue.api.dto;

public record ActionQueueItemResponse(
    String key,
    String level,
    String category,
    String title,
    String subject,
    String detail,
    String actionLabel,
    String actionPath,
    String referenceType,
    Long referenceId
) {}
