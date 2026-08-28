package com.portfolio.warehouse.handover.api.dto;

public record HandoverIssueBriefResponse(
    Long issueId,
    String status,
    String issueType,
    String responsible,
    String location,
    String comment
) {}
