package com.portfolio.warehouse.mate.api.dto;

import com.portfolio.warehouse.mate.domain.MateWorkSchedule;
import com.portfolio.warehouse.mate.domain.ScheduleType;
import com.portfolio.warehouse.mate.domain.ShiftType;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleResponse(
    Long id,
    DayOfWeek dayOfWeek,
    ScheduleType scheduleType,
    ShiftType shiftType,
    LocalTime startTime,
    LocalTime endTime
) {
    public static ScheduleResponse from(MateWorkSchedule entity) {
        return new ScheduleResponse(
            entity.getId(),
            entity.getDayOfWeek(),
            entity.getScheduleType(),
            entity.getShiftType(),
            entity.getStartTime(),
            entity.getEndTime()
        );
    }
}
