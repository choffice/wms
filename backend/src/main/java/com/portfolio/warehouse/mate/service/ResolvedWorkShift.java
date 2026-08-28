package com.portfolio.warehouse.mate.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ResolvedWorkShift(
    LocalDate shiftDate,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    boolean overnight,
    boolean extensionActive,
    boolean autoEndEnabled,
    String source
) {}
