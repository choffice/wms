package com.portfolio.warehouse.handover.api.dto;

public record HandoverOverviewCountsResponse(
    int pendingAssignments,
    int handoverCandidates,
    int unresolvedIssues,
    int unconfirmedIssues,
    int unassignedIssues,
    int integrityCritical,
    int integrityWarning,
    int openSessions,
    int operationAttentionMates
) {}
