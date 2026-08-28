package com.portfolio.warehouse.work.config;

import com.portfolio.warehouse.work.service.WorkSessionReliabilityService;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WorkSessionScheduler {

    private final WorkSessionReliabilityService reliabilityService;

    public WorkSessionScheduler(WorkSessionReliabilityService reliabilityService) {
        this.reliabilityService = reliabilityService;
    }

    @Scheduled(fixedDelay = 60000)
    public void inspect() {
        reliabilityService.inspectOpenSessions(LocalDateTime.now());
    }
}
