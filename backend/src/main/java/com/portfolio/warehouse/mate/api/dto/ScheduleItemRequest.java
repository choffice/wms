package com.portfolio.warehouse.mate.api.dto;

import com.portfolio.warehouse.mate.domain.ScheduleType;
import com.portfolio.warehouse.mate.domain.ShiftType;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleItemRequest(
    @NotNull DayOfWeek dayOfWeek,
    @NotNull ScheduleType scheduleType,
    @NotNull ShiftType shiftType,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime
) {
}
