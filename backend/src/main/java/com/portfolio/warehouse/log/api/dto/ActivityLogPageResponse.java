package com.portfolio.warehouse.log.api.dto;

import java.util.List;

public record ActivityLogPageResponse(
    List<ActivityLogResponse> content,
    long totalElements,
    int page,
    int size,
    int totalPages
) {}
