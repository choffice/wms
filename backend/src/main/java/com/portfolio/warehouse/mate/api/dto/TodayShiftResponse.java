package com.portfolio.warehouse.mate.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TodayShiftResponse(
    LocalDate date,
    LocalDate shiftDate,
    String status,
    String whereabouts,
    LocalDateTime effectiveScheduledStart,
    LocalDateTime effectiveScheduledEnd,
    boolean overnight,
    boolean extensionActive,
    boolean autoEndEnabled
) {}
