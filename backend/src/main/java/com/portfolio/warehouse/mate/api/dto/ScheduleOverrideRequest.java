package com.portfolio.warehouse.mate.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleOverrideRequest(
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime
) {}
