package com.portfolio.warehouse.work.service;

import com.portfolio.warehouse.mate.domain.Mate;
import com.portfolio.warehouse.mate.domain.MateStatus;
import com.portfolio.warehouse.mate.domain.MateStatusHistory;
import com.portfolio.warehouse.mate.repository.MateRepository;
import com.portfolio.warehouse.mate.repository.MateStatusHistoryRepository;
import com.portfolio.warehouse.work.domain.WorkSessionEndReason;
import com.portfolio.warehouse.work.repository.WorkSessionRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MateLogoutWorkService {

    private final MateRepository mateRepository;
    private final WorkSessionRepository sessionRepository;
    private final MateStatusHistoryRepository statusHistoryRepository;

    public MateLogoutWorkService(
        MateRepository mateRepository,
        WorkSessionRepository sessionRepository,
        MateStatusHistoryRepository statusHistoryRepository
    ) {
        this.mateRepository = mateRepository;
        this.sessionRepository = sessionRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    @Transactional
    public void closeActiveWorkForLogout(String employeeNo) {
        Mate mate = mateRepository.findByEmployeeNo(employeeNo).orElse(null);
        if (mate == null) return;

        sessionRepository.findFirstByMateIdAndEndedAtIsNull(mate.getId())
            .ifPresent(session ->
                session.close(LocalDateTime.now(), WorkSessionEndReason.LOGOUT)
            );

        if (mate.getCurrentStatus() == MateStatus.WORKING) {
            mate.changeStatus(MateStatus.AVAILABLE, "대기");
            statusHistoryRepository.save(
                new MateStatusHistory(mate, MateStatus.AVAILABLE, "대기")
            );
        }
    }
}
