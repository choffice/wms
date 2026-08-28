package com.portfolio.warehouse.mate.api.dto;

import com.portfolio.warehouse.mate.domain.WorkScheduleOverride;
import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleOverrideResponse(
    Long id,
    Long mateId,
    LocalDate startDate,
    LocalDate endDate,
    LocalTime startTime,
    LocalTime endTime,
    String overrideType,
    boolean autoEndDisabled
) {
    public static ScheduleOverrideResponse from(WorkScheduleOverride entity) {
        return new ScheduleOverrideResponse(
            entity.getId(),
            entity.getMate().getId(),
            entity.getStartDate(),
            entity.getEndDate(),
            entity.getStartTime(),
            entity.getEndTime(),
            entity.getOverrideType().name(),
            entity.isAutoEndDisabled()
        );
    }
}
