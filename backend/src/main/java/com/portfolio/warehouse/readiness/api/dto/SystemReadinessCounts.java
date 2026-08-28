package com.portfolio.warehouse.readiness.api.dto;

public record SystemReadinessCounts(
    long activeMates,
    long activePdas,
    long locations,
    long workTypes,
    long issueTypes,
    long openSessions,
    long handoverCandidates,
    long unresolvedIssues,
    int integrityCritical,
    int integrityWarning
) {}
