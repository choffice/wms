package com.portfolio.warehouse.common.api;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiErrorResponse(
    LocalDateTime timestamp,
    int status,
    String code,
    String message,
    Map<String, String> fieldErrors
) {
}
