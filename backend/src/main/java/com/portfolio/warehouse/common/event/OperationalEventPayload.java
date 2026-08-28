package com.portfolio.warehouse.common.event;

import java.time.LocalDateTime;

public record OperationalEventPayload(
    String type,
    String actor,
    String target,
    String message,
    String referenceType,
    Long referenceId,
    LocalDateTime createdAt
) {}
