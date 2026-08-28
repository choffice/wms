package com.portfolio.warehouse.dashboard.service;

import com.portfolio.warehouse.dashboard.api.dto.*;
import com.portfolio.warehouse.issue.api.dto.SpecialIssueResponse;
import com.portfolio.warehouse.issue.domain.IssueStatus;
import com.portfolio.warehouse.issue.repository.SpecialIssueRepository;
import com.portfolio.warehouse.location.domain.Location;
import com.portfolio.warehouse.location.repository.LocationRepository;
import com.portfolio.warehouse.log.api.dto.ActivityLogResponse;
import com.portfolio.warehouse.log.repository.ActivityLogRepository;
import com.portfolio.warehouse.mate.domain.Mate;
import com.portfolio.warehouse.mate.repository.MateRepository;
import com.portfolio.warehouse.notice.api.dto.NoticeResponse;
import com.portfolio.warehouse.notice.repository.NoticeRepository;
import com.portfolio.warehouse.pda.repository.PdaUsageHistoryRepository;
import com.portfolio.warehouse.work.domain.*;
import com.portfolio.warehouse.work.api.dto.WorkEstimateResponse;
import com.portfolio.warehouse.work.service.AreaProgressService;
import com.portfolio.warehouse.work.service.WorkEstimateService;
import com.portfolio.warehouse.work.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {

    private final MateRepository mateRepository;
    private final PdaUsageHistoryRepository pdaUsageRepository;
    private final WorkAssignmentRepository assignmentRepository;
    private final WorkSessionRepository sessionRepository;
    private final WorkProgressRepository progressRepository;
    private final WorkTypeRepository workTypeRepository;
    private final LocationRepository locationRepository;
    private final NoticeRepository noticeRepository;
    private final SpecialIssueRepository issueRepository;
    private final ActivityLogRepository logRepository;
    private final AreaProgressService areaProgressService;
    private final WorkEstimateService workEstimateService;

    public AdminDashboardService(
        MateRepository mateRepository,
        PdaUsageHistoryRepository pdaUsageRepository,
        WorkAssignmentRepository assignmentRepository,
        WorkSessionRepository sessionRepository,
        WorkProgressRepository progressRepository,
        WorkTypeRepository workTypeRepository,
        LocationRepository locationRepository,
        NoticeRepository noticeRepository,
        SpecialIssueRepository issueRepository,
        ActivityLogRepository logRepository,
        AreaProgressService areaProgressService,
        WorkEstimateService workEstimateService
    ) {
        this.mateRepository = mateRepository;
        this.pdaUsageRepository = pdaUsageRepository;
        this.assignmentRepository = assignmentRepository;
        this.sessionRepository = sessionRepository;
        this.progressRepository = progressRepository;
        this.workTypeRepository = workTypeRepository;
        this.locationRepository = locationRepository;
        this.noticeRepository = noticeRepository;
        this.issueRepository = issueRepository;
        this.logRepository = logRepository;
        this.areaProgressService = areaProgressService;
        this.workEstimateService = workEstimateService;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard() {
        List<NoticeResponse> notices =
            noticeRepository.findAllByDeletedAtIsNullAndVisibleTrueOrderByImportantDescDisplayOrderAscUpdatedAtDesc()
                .stream().map(NoticeResponse::from).toList();

        List<SpecialIssueResponse> issues =
            issueRepository.findAllByDeletedAtIsNullAndStatusOrderByCreatedAtDesc(IssueStatus.UNCONFIRMED)
                .stream().map(SpecialIssueResponse::from).toList();

        List<MateDashboardRow> mates = mateRepository.findAllByActiveTrueOrderByNicknameAsc()
            .stream().map(this::mateRow).toList();

        List<AreaWorkStatusRow> areaRows = areaRows();

        List<ActivityLogResponse> logs = logRepository.findTop10ByOrderByCreatedAtDesc()
            .stream().map(ActivityLogResponse::from).toList();

        return new AdminDashboardResponse(notices, issues, mates, areaRows, logs);
    }

    private MateDashboardRow mateRow(Mate mate) {
        Integer pdaNumber = pdaUsageRepository.findFirstByMateIdAndReleasedAtIsNull(mate.getId())
            .map(usage -> usage.getPdaDevice().getDeviceNumber())
            .orElse(null);

        Optional<WorkSession> openSession =
            sessionRepository.findFirstByMateIdAndEndedAtIsNull(mate.getId());

        WorkAssignment assignment = openSession
            .map(WorkSession::getWorkAssignment)
            .orElseGet(() ->
                assignmentRepository.findAllByCurrentMateIdAndStatusInOrderByAssignedAtDesc(
                    mate.getId(),
                    List.of(WorkAssignmentStatus.ASSIGNED, WorkAssignmentStatus.IN_PROGRESS)
                ).stream().findFirst().orElse(null)
            );

        return new MateDashboardRow(
            mate.getId(),
            mate.getNickname(),
            mate.getCurrentStatus().name(),
            mate.getCurrentWhereabouts(),
            pdaNumber,
            assignment == null ? null : assignment.getId(),
            assignment == null ? null : assignment.getWorkType().getName(),
            assignment == null ? null : assignment.getAreaLocation().getFullCode(),
            assignment == null || assignment.getCurrentLastCompletedLocation() == null
                ? null
                : assignment.getCurrentLastCompletedLocation().getFullCode()
        );
    }

    private List<AreaWorkStatusRow> areaRows() {
        List<Location> roots = locationRepository.findAllByOrderByFullCodeAsc().stream()
            .filter(Location::isActive)
            .filter(location -> location.getParent() == null)
            .toList();

        List<WorkType> workTypes = workTypeRepository.findAll().stream()
            .filter(WorkType::isActive)
            .toList();

        List<AreaWorkStatusRow> result = new ArrayList<>();

        for (Location area : roots) {
            for (WorkType workType : workTypes) {
                WorkAssignment latestWithProgress =
                    assignmentRepository.findAllByAreaLocationIdAndWorkTypeIdOrderByAssignedAtDesc(
                        area.getId(),
                        workType.getId()
                    ).stream()
                    .filter(a -> a.getCurrentLastCompletedLocation() != null)
                    .findFirst()
                    .orElse(null);

                if (latestWithProgress == null) {
                    result.add(
                        new AreaWorkStatusRow(
                            area.getId(),
                            area.getFullCode(),
                            workType.getId(),
                            workType.getName(),
                            null,
                            null,
                            null,
                            0,
                            null,
                            0
                        )
                    );
                    continue;
                }

                Optional<WorkProgress> lastProgress =
                    progressRepository.findFirstByWorkAssignmentIdOrderByReportedAtDescIdDesc(
                        latestWithProgress.getId()
                    );

                WorkEstimateResponse estimate =
                    workEstimateService.estimate(
                        area.getId(),
                        workType.getId(),
                        null
                    );

                result.add(
                    new AreaWorkStatusRow(
                        area.getId(),
                        area.getFullCode(),
                        workType.getId(),
                        workType.getName(),
                        latestWithProgress.getCurrentLastCompletedLocation().getFullCode(),
                        lastProgress.map(WorkProgress::getReportedAt).orElse(null),
                        lastProgress.map(p -> p.getMate().getNickname()).orElse(null),
                        areaProgressService.areaPercentAt(
                            area,
                            latestWithProgress.getCurrentLastCompletedLocation()
                        ),
                        estimate.estimatedRemainingFromCurrentSeconds(),
                        estimate.historicalSampleCount()
                    )
                );
            }
        }

        return result;
    }


}
