package com.portfolio.warehouse.readiness.service;

import com.portfolio.warehouse.handover.service.HandoverService;
import com.portfolio.warehouse.integrity.service.IntegrityService;
import com.portfolio.warehouse.issue.domain.IssueStatus;
import com.portfolio.warehouse.issue.repository.*;
import com.portfolio.warehouse.location.repository.LocationRepository;
import com.portfolio.warehouse.mate.repository.MateRepository;
import com.portfolio.warehouse.pda.repository.PdaDeviceRepository;
import com.portfolio.warehouse.readiness.api.dto.*;
import com.portfolio.warehouse.work.repository.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemReadinessService {

    private final MateRepository mateRepository;
    private final PdaDeviceRepository pdaRepository;
    private final LocationRepository locationRepository;
    private final WorkTypeRepository workTypeRepository;
    private final IssueTypeRepository issueTypeRepository;
    private final WorkSessionRepository sessionRepository;
    private final SpecialIssueRepository issueRepository;
    private final IntegrityService integrityService;
    private final HandoverService handoverService;

    @Value("${warehouse.demo-scenario.enabled:false}")
    private boolean demoScenarioEnabled;

    public SystemReadinessService(
        MateRepository mateRepository,
        PdaDeviceRepository pdaRepository,
        LocationRepository locationRepository,
        WorkTypeRepository workTypeRepository,
        IssueTypeRepository issueTypeRepository,
        WorkSessionRepository sessionRepository,
        SpecialIssueRepository issueRepository,
        IntegrityService integrityService,
        HandoverService handoverService
    ) {
        this.mateRepository = mateRepository;
        this.pdaRepository = pdaRepository;
        this.locationRepository = locationRepository;
        this.workTypeRepository = workTypeRepository;
        this.issueTypeRepository = issueTypeRepository;
        this.sessionRepository = sessionRepository;
        this.issueRepository = issueRepository;
        this.integrityService = integrityService;
        this.handoverService = handoverService;
    }

    @Transactional(readOnly = true)
    public SystemReadinessResponse readiness() {
        long activeMates =
            mateRepository
                .findAllByActiveTrueOrderByNicknameAsc()
                .size();

        long activePdas =
            pdaRepository.findAll().stream()
                .filter(pda ->
                    pda.isActive()
                )
                .count();

        long locations =
            locationRepository.findAll().stream()
                .filter(location ->
                    location.isActive()
                )
                .count();

        long workTypes =
            workTypeRepository.findAll().stream()
                .filter(type ->
                    type.isActive()
                )
                .count();

        long issueTypes =
            issueTypeRepository.findAll().stream()
                .filter(type ->
                    type.isActive()
                )
                .count();

        long openSessions =
            sessionRepository
                .findAllByEndedAtIsNull()
                .size();

        long unresolvedIssues =
            issueRepository
                .findAllByDeletedAtIsNullOrderByCreatedAtDesc()
                .stream()
                .filter(issue ->
                    issue.getStatus()
                        != IssueStatus.RESOLVED
                )
                .count();

        var integrity =
            integrityService.scan();

        var handover =
            handoverService.board();

        var counts =
            new SystemReadinessCounts(
                activeMates,
                activePdas,
                locations,
                workTypes,
                issueTypes,
                openSessions,
                handover.summary()
                    .handoverCandidateCount(),
                unresolvedIssues,
                integrity.summary().critical(),
                integrity.summary().warning()
            );

        List<SystemReadinessCheck> checks =
            new ArrayList<>();

        masterCheck(
            checks,
            "MATE_MASTER",
            "활성 MATE",
            activeMates,
            "/mates"
        );

        masterCheck(
            checks,
            "PDA_MASTER",
            "활성 PDA",
            activePdas,
            "/settings"
        );

        masterCheck(
            checks,
            "LOCATION_MASTER",
            "활성 로케이션",
            locations,
            "/settings"
        );

        masterCheck(
            checks,
            "WORK_TYPE_MASTER",
            "활성 업무종류",
            workTypes,
            "/settings"
        );

        masterCheck(
            checks,
            "ISSUE_TYPE_MASTER",
            "활성 특이사항 구분",
            issueTypes,
            "/settings"
        );

        checks.add(
            new SystemReadinessCheck(
                "INTEGRITY_CRITICAL",
                counts.integrityCritical() > 0
                    ? "BLOCKER"
                    : "OK",
                "치명 정합성",
                counts.integrityCritical() > 0
                    ? counts.integrityCritical()
                        + "건의 치명 정합성 오류가 있습니다."
                    : "치명 정합성 오류가 없습니다.",
                "/integrity"
            )
        );

        checks.add(
            new SystemReadinessCheck(
                "CSRF",
                "OK",
                "CSRF 보호",
                "Session/Cookie 인증의 변경 요청은 X-XSRF-TOKEN 검증을 사용합니다.",
                null
            )
        );

        checks.add(
            new SystemReadinessCheck(
                "OPEN_SESSION",
                openSessions > 0
                    ? "INFO"
                    : "OK",
                "Open WorkSession",
                openSessions > 0
                    ? openSessions
                        + "건이 진행 중입니다. 운영 시나리오에서는 정상 상태일 수 있습니다."
                    : "현재 진행 중 WorkSession이 없습니다.",
                "/operations"
            )
        );

        checks.add(
            new SystemReadinessCheck(
                "HANDOVER",
                counts.handoverCandidates() > 0
                    ? "INFO"
                    : "OK",
                "인수인계 검토",
                counts.handoverCandidates() > 0
                    ? counts.handoverCandidates()
                        + "건의 인수인계 후보가 있습니다."
                    : "현재 인수인계 후보가 없습니다.",
                "/handover"
            )
        );

        boolean masterReady =
            activeMates > 0
                && activePdas > 0
                && locations > 0
                && workTypes > 0
                && issueTypes > 0;

        boolean ready =
            masterReady
                && counts.integrityCritical() == 0;

        return new SystemReadinessResponse(
            LocalDateTime.now(),
            ready,
            "SPRING_SECURITY_SESSION_COOKIE",
            true,
            demoScenarioEnabled,
            counts,
            List.copyOf(checks)
        );
    }

    private void masterCheck(
        List<SystemReadinessCheck> checks,
        String code,
        String label,
        long count,
        String actionPath
    ) {
        checks.add(
            new SystemReadinessCheck(
                code,
                count > 0
                    ? "OK"
                    : "BLOCKER",
                label,
                count > 0
                    ? count + "건 준비됨"
                    : "시연 전에 최소 1건 이상 등록해야 합니다.",
                actionPath
            )
        );
    }
}
