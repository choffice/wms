package com.portfolio.warehouse.readiness.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SystemReadinessResponse(
    LocalDateTime generatedAt,
    boolean readyForDemo,
    String authenticationMode,
    boolean csrfEnabled,
    boolean demoScenarioEnabled,
    SystemReadinessCounts counts,
    List<SystemReadinessCheck> checks
) {}
