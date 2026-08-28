package com.portfolio.warehouse.log.service;

import com.portfolio.warehouse.auth.service.CurrentUserService;
import com.portfolio.warehouse.common.event.OperationalEventService;
import com.portfolio.warehouse.log.domain.ActivityType;
import org.springframework.stereotype.Service;

@Service
public class BusinessAuditService {

    private final CurrentUserService currentUserService;
    private final OperationalEventService eventService;

    public BusinessAuditService(
        CurrentUserService currentUserService,
        OperationalEventService eventService
    ) {
        this.currentUserService = currentUserService;
        this.eventService = eventService;
    }

    public void record(
        ActivityType type,
        String target,
        String message,
        String referenceType,
        Long referenceId
    ) {
        eventService.publish(
            type,
            currentUserService.account(),
            target,
            message,
            referenceType,
            referenceId,
            true,
            false
        );
    }
}
