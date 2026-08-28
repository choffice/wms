package com.portfolio.warehouse.operations.service;

import com.portfolio.warehouse.common.exception.BusinessException;
import com.portfolio.warehouse.common.exception.NotFoundException;
import com.portfolio.warehouse.pda.api.dto.PdaUsageResponse;
import com.portfolio.warehouse.pda.domain.*;
import com.portfolio.warehouse.pda.repository.PdaUsageHistoryRepository;
import com.portfolio.warehouse.pda.service.PdaSessionService;
import com.portfolio.warehouse.work.repository.WorkSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationsActionService {

    private final PdaUsageHistoryRepository usageRepository;
    private final WorkSessionRepository sessionRepository;
    private final PdaSessionService pdaSessionService;

    public OperationsActionService(
        PdaUsageHistoryRepository usageRepository,
        WorkSessionRepository sessionRepository,
        PdaSessionService pdaSessionService
    ) {
        this.usageRepository = usageRepository;
        this.sessionRepository = sessionRepository;
        this.pdaSessionService = pdaSessionService;
    }

    @Transactional
    public PdaUsageResponse forceReleasePda(Long usageId) {
        PdaUsageHistory usage = usageRepository.findById(usageId)
            .orElseThrow(() -> new NotFoundException(
                "PDA_USAGE_NOT_FOUND",
                "PDA 사용 이력을 찾을 수 없습니다."
            ));

        if (!usage.isActiveUsage()) {
            return PdaUsageResponse.from(usage);
        }

        Long mateId = usage.getMate().getId();

        if (
            sessionRepository
                .findFirstByMateIdAndEndedAtIsNull(mateId)
                .isPresent()
        ) {
            throw new BusinessException(
                "ACTIVE_WORK_SESSION",
                "작업 중인 세션이 있습니다. 먼저 작업을 일시정지 또는 종료한 뒤 PDA를 회수해주세요."
            );
        }

        return pdaSessionService.releaseByAdmin(
            usageId,
            PdaReleaseReason.ADMIN_RELEASE
        );
    }
}
