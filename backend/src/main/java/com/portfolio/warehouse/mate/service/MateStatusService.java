package com.portfolio.warehouse.mate.service;

import com.portfolio.warehouse.common.exception.NotFoundException;
import com.portfolio.warehouse.log.domain.ActivityType;
import com.portfolio.warehouse.log.service.BusinessAuditService;
import com.portfolio.warehouse.mate.api.dto.MateResponse;
import com.portfolio.warehouse.mate.domain.*;
import com.portfolio.warehouse.mate.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MateStatusService {

    private final MateRepository mateRepository;
    private final MateStatusHistoryRepository historyRepository;
    private final BusinessAuditService auditService;

    public MateStatusService(
        MateRepository mateRepository,
        MateStatusHistoryRepository historyRepository,
        BusinessAuditService auditService
    ) {
        this.mateRepository = mateRepository;
        this.historyRepository = historyRepository;
        this.auditService = auditService;
    }

    @Transactional
    public MateResponse changeStatus(
        Long mateId,
        MateStatus status,
        String whereabouts
    ) {
        Mate mate = mateRepository.findById(mateId)
            .orElseThrow(() -> new NotFoundException(
                "MATE_NOT_FOUND",
                "MATE를 찾을 수 없습니다."
            ));

        MateStatus beforeStatus = mate.getCurrentStatus();
        String beforeWhereabouts = mate.getCurrentWhereabouts();

        String resolvedWhereabouts =
            normalizeWhereabouts(status, whereabouts);

        if (
            beforeStatus == status
                && java.util.Objects.equals(
                    beforeWhereabouts,
                    resolvedWhereabouts
                )
        ) {
            return MateResponse.from(mate);
        }

        mate.changeStatus(status, resolvedWhereabouts);
        historyRepository.save(
            new MateStatusHistory(
                mate,
                status,
                resolvedWhereabouts
            )
        );

        auditService.record(
            ActivityType.STATUS_CHANGE,
            mate.getNickname(),
            "상태 변경 · "
                + beforeStatus
                + " / "
                + value(beforeWhereabouts)
                + " → "
                + status
                + " / "
                + value(resolvedWhereabouts),
            "MATE_STATUS",
            mate.getId()
        );

        return MateResponse.from(mate);
    }

    private String normalizeWhereabouts(
        MateStatus status,
        String whereabouts
    ) {
        if (
            whereabouts != null
                && !whereabouts.isBlank()
        ) {
            return whereabouts.trim();
        }

        return switch (status) {
            case AVAILABLE -> "대기";
            case WORKING -> "업무중";
            case BREAK -> "휴게";
            case AWAY -> "자리비움";
            case OFF_DUTY -> "퇴근";
        };
    }

    private String value(String value) {
        return value == null ? "-" : value;
    }
}
