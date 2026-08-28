package com.portfolio.warehouse.work.service;

import com.portfolio.warehouse.common.event.OperationalEventService;
import com.portfolio.warehouse.log.domain.ActivityType;

import com.portfolio.warehouse.mate.domain.*;
import com.portfolio.warehouse.mate.repository.MateStatusHistoryRepository;
import com.portfolio.warehouse.mate.service.WorkScheduleResolver;
import com.portfolio.warehouse.work.domain.*;
import com.portfolio.warehouse.work.repository.WorkSessionRepository;
import com.portfolio.warehouse.work.api.dto.WorkSessionResponse;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkSessionReliabilityService {

    private final WorkSessionRepository sessionRepository;
    private final WorkScheduleResolver scheduleResolver;
    private final MateStatusHistoryRepository mateStatusHistoryRepository;
    private final long heartbeatUncertainMinutes;
    private final long networkTimeoutMinutes;
    private final OperationalEventService eventService;

    public WorkSessionReliabilityService(
        WorkSessionRepository sessionRepository,
        WorkScheduleResolver scheduleResolver,
        MateStatusHistoryRepository mateStatusHistoryRepository,
        @Value("${warehouse.session.heartbeat-uncertain-minutes:3}") long heartbeatUncertainMinutes,
        @Value("${warehouse.session.network-timeout-minutes:10}") long networkTimeoutMinutes,
        OperationalEventService eventService
    ) {
        this.sessionRepository = sessionRepository;
        this.scheduleResolver = scheduleResolver;
        this.mateStatusHistoryRepository = mateStatusHistoryRepository;
        this.heartbeatUncertainMinutes =
            Math.max(1L, heartbeatUncertainMinutes);
        this.networkTimeoutMinutes =
            Math.max(
                this.heartbeatUncertainMinutes + 1L,
                networkTimeoutMinutes
            );
        this.eventService = eventService;
    }

    @Transactional(readOnly = true)
    public WorkSessionResponse currentOpenSessionResponse(Long mateId) {
        return sessionRepository.findFirstByMateIdAndEndedAtIsNull(mateId)
            .map(WorkSessionResponse::from)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public WorkSession currentOpenSession(Long mateId) {
        return sessionRepository.findFirstByMateIdAndEndedAtIsNull(mateId)
            .orElse(null);
    }

    @Transactional
    public void heartbeat(Long sessionId, Long mateId) {
        WorkSession session = sessionRepository.findByIdForUpdate(sessionId)
            .orElseThrow(() -> new IllegalStateException("작업 세션을 찾을 수 없습니다."));

        if (!session.getMate().getId().equals(mateId)) {
            throw new IllegalStateException("본인의 작업 세션만 갱신할 수 있습니다.");
        }

        session.heartbeat(LocalDateTime.now());
    }

    @Transactional
    public void inspectOpenSessions(LocalDateTime now) {
        for (WorkSession session : sessionRepository.findAllByEndedAtIsNull()) {
            LocalDateTime lastHeartbeat = session.getLastHeartbeatAt();

            if (lastHeartbeat != null
                && lastHeartbeat.isBefore(
                    now.minusMinutes(heartbeatUncertainMinutes)
                )) {
                session.markUncertain();
            }

            Long mateId = session.getMate().getId();

            LocalDate shiftDate =
                session.getShiftDate() != null
                    ? session.getShiftDate()
                    : scheduleResolver.resolveShiftDate(
                        mateId,
                        session.getStartedAt()
                    );

            boolean extensionActive =
                scheduleResolver.extensionActive(
                    mateId,
                    shiftDate
                );

            if (!extensionActive) {
                var effectiveEnd =
                    scheduleResolver.effectiveEnd(
                        mateId,
                        shiftDate
                    );

                if (effectiveEnd.isPresent()) {
                    LocalDateTime end =
                        effectiveEnd.get();

                    boolean endBelongsToSession =
                        session.getStartedAt() == null
                            || end.isAfter(
                                session.getStartedAt()
                            );

                    if (
                        endBelongsToSession
                            && !now.isBefore(end)
                    ) {
                        session.close(
                            end,
                            WorkSessionEndReason.SCHEDULE_END
                        );

                    Mate mate = session.getMate();
                    mate.changeStatus(MateStatus.OFF_DUTY, "퇴근");
                    mateStatusHistoryRepository.save(
                        new MateStatusHistory(
                            mate,
                            MateStatus.OFF_DUTY,
                            "퇴근"
                        )
                    );

                    eventService.publish(
                        ActivityType.SHIFT_AUTO_END,
                        mate.getAccount(),
                        "관리자",
                        "근무 종료시각에 작업세션 자동 종료",
                        "WORK_SESSION",
                        session.getId(),
                        true,
                        true
                    );
                        continue;
                    }
                }
            }

            if (lastHeartbeat != null
                && lastHeartbeat.isBefore(
                    now.minusMinutes(networkTimeoutMinutes)
                )) {
                session.markUncertain();
                session.close(
                    now,
                    WorkSessionEndReason.NETWORK_TIMEOUT
                );

                Mate mate = session.getMate();
                mate.changeStatus(
                    MateStatus.AWAY,
                    "통신 확인 필요"
                );
                mateStatusHistoryRepository.save(
                    new MateStatusHistory(
                        mate,
                        MateStatus.AWAY,
                        "통신 확인 필요"
                    )
                );

                eventService.publish(
                    ActivityType.SESSION_TIMEOUT,
                    mate.getAccount(),
                    "관리자",
                    "Heartbeat 장기 단절로 작업세션 UNCERTAIN 종료",
                    "WORK_SESSION",
                    session.getId(),
                    true,
                    true
                );
            }
        }
    }
}
