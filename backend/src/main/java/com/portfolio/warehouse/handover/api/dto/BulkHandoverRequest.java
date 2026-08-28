package com.portfolio.warehouse.handover.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BulkHandoverRequest(
    @Valid
    @NotEmpty
    @Size(max = 50)
    List<BulkHandoverItemRequest> items
) {}
