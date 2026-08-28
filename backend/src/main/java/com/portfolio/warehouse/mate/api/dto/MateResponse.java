package com.portfolio.warehouse.mate.api.dto;

import com.portfolio.warehouse.mate.domain.Mate;
import com.portfolio.warehouse.mate.domain.MateStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MateResponse(
    Long id,
    String employeeNo,
    String name,
    String nickname,
    boolean active,
    LocalDate joinedAt,
    LocalDateTime deactivatedAt,
    MateStatus status,
    String whereabouts
) {
    public static MateResponse from(Mate mate) {
        return new MateResponse(
            mate.getId(),
            mate.getEmployeeNo(),
            mate.getName(),
            mate.getNickname(),
            mate.isActive(),
            mate.getJoinedAt(),
            mate.getDeactivatedAt(),
            mate.getCurrentStatus(),
            mate.getCurrentWhereabouts()
        );
    }
}
