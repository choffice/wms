package com.portfolio.warehouse.log.service;

import com.portfolio.warehouse.auth.domain.UserAccount;
import com.portfolio.warehouse.common.exception.BusinessException;
import com.portfolio.warehouse.log.api.dto.*;
import com.portfolio.warehouse.log.domain.*;
import com.portfolio.warehouse.log.repository.*;
import java.time.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityLogService {

    private final ActivityLogRepository repository;
    private final ActivityLogQueryRepository queryRepository;

    public ActivityLogService(
        ActivityLogRepository repository,
        ActivityLogQueryRepository queryRepository
    ) {
        this.repository = repository;
        this.queryRepository = queryRepository;
    }

    @Transactional
    public void write(
        ActivityType type,
        UserAccount actor,
        String target,
        String message,
        String referenceType,
        Long referenceId
    ) {
        repository.save(
            new ActivityLog(
                type,
                actor,
                target,
                message,
                referenceType,
                referenceId
            )
        );
    }

    @Transactional(readOnly = true)
    public List<ActivityLogResponse> latest10() {
        return repository
            .findTop10ByOrderByCreatedAtDesc()
            .stream()
            .map(ActivityLogResponse::from)
            .toList();
    }


    @Transactional(readOnly = true)
    public List<ActivityLogResponse> recentAdminActions(
        int limit
    ) {
        return queryRepository
            .latestByActorRole(
                com.portfolio.warehouse.auth.domain.UserRole.ADMIN,
                limit
            )
            .stream()
            .map(ActivityLogResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public ActivityLogPageResponse search(
        LocalDate fromDate,
        LocalDate toDate,
        ActivityType type,
        String actor,
        String referenceType,
        Long referenceId,
        String keyword,
        int page,
        int size
    ) {
        if (page < 0) {
            throw new BusinessException(
                "LOG_PAGE_INVALID",
                "페이지 번호는 0 이상이어야 합니다."
            );
        }

        int resolvedSize = Math.min(
            100,
            Math.max(10, size)
        );

        LocalDateTime from =
            fromDate == null
                ? null
                : fromDate.atStartOfDay();

        LocalDateTime to =
            toDate == null
                ? null
                : toDate.plusDays(1).atStartOfDay();

        if (
            fromDate != null
                && toDate != null
                && toDate.isBefore(fromDate)
        ) {
            throw new BusinessException(
                "LOG_RANGE_INVALID",
                "종료일은 시작일보다 빠를 수 없습니다."
            );
        }

        List<ActivityLogResponse> content =
            queryRepository.search(
                from,
                to,
                type,
                actor,
                referenceType,
                referenceId,
                keyword,
                page,
                resolvedSize
            ).stream()
                .map(ActivityLogResponse::from)
                .toList();

        long total =
            queryRepository.count(
                from,
                to,
                type,
                actor,
                referenceType,
                referenceId,
                keyword
            );

        int totalPages =
            total == 0
                ? 0
                : (int) Math.ceil(
                    total / (double) resolvedSize
                );

        return new ActivityLogPageResponse(
            content,
            total,
            page,
            resolvedSize,
            totalPages
        );
    }

    public List<String> referenceTypes() {
        return List.of(
            "PDA_USAGE",
            "WORK_ASSIGNMENT",
            "WORK_ASSIGNMENT_BATCH",
            "WORK_SESSION",
            "SPECIAL_ISSUE",
            "SPECIAL_ISSUE_BATCH",
            "NOTICE",
            "MATE_STATUS",
            "MATE",
            "MATE_SCHEDULE",
            "HANDOVER_NOTE",
            "PDA_DEVICE",
            "LOCATION",
            "WORK_TYPE",
            "ISSUE_TYPE",
            "AUTH"
        );
    }
}
