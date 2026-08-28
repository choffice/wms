package com.portfolio.warehouse.work.api.dto;

import com.portfolio.warehouse.mate.domain.MateStatus;
import jakarta.validation.constraints.Size;

public record WorkPauseRequest(
    MateStatus nextStatus,
    @Size(max = 100) String whereabouts
) {}
