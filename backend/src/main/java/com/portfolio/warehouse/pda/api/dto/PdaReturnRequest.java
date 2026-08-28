package com.portfolio.warehouse.pda.api.dto;

import com.portfolio.warehouse.pda.domain.PdaReleaseReason;

public record PdaReturnRequest(
    PdaReleaseReason reason
) {
}
