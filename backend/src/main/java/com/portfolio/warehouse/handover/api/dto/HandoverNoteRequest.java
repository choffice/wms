package com.portfolio.warehouse.handover.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record HandoverNoteRequest(
    LocalDate shiftDate,
    @NotBlank
    @Size(max = 1200)
    String content
) {}
