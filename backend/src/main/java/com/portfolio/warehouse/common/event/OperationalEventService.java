package com.portfolio.warehouse.common.event;

import com.portfolio.warehouse.auth.domain.UserAccount;
import com.portfolio.warehouse.common.sse.SseHub;
import com.portfolio.warehouse.log.domain.*;
import com.portfolio.warehouse.log.repository.ActivityLogRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class OperationalEventService {

    private final ActivityLogRepository logRepository;
    private final SseHub sseHub;

    public OperationalEventService(
        ActivityLogRepository logRepository,
        SseHub sseHub
    ) {
        this.logRepository = logRepository;
        this.sseHub = sseHub;
    }

    public void publish(
        ActivityType type,
        UserAccount actor,
        String target,
        String message,
        String referenceType,
        Long referenceId,
        boolean notifyAdmin,
        boolean notifyMate
    ) {
        ActivityLog log = logRepository.save(
            new ActivityLog(
                type,
                actor,
                target,
                message,
                referenceType,
                referenceId
            )
        );

        OperationalEventPayload payload = new OperationalEventPayload(
            type.name(),
            actor == null ? null : actor.getLoginId(),
            target,
            message,
            referenceType,
            referenceId,
            log.getCreatedAt()
        );

        if (notifyAdmin) {
            sseHub.publishAdmin("operation", payload);
        }

        if (notifyMate) {
            sseHub.publishMate("operation", payload);
        }
    }
}
