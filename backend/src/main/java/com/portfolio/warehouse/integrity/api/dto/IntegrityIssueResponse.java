package com.portfolio.warehouse.integrity.api.dto;

public record IntegrityIssueResponse(
    String issueKey,
    String severity,
    String code,
    String entityType,
    Long entityId,
    String subject,
    String detail,
    String safeRepairAction
) {
    public boolean repairable() {
        return safeRepairAction != null;
    }
}
