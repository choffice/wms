package com.portfolio.warehouse.work.service;

import com.portfolio.warehouse.common.exception.NotFoundException;
import com.portfolio.warehouse.location.domain.Location;
import com.portfolio.warehouse.location.repository.LocationRepository;
import com.portfolio.warehouse.work.api.dto.WorkEstimateResponse;
import com.portfolio.warehouse.work.domain.*;
import com.portfolio.warehouse.work.repository.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkEstimateService {

    private final WorkAssignmentRepository assignmentRepository;
    private final WorkSessionRepository sessionRepository;
    private final WorkTypeRepository workTypeRepository;
    private final LocationRepository locationRepository;
    private final AreaProgressService areaProgressService;

    public WorkEstimateService(
        WorkAssignmentRepository assignmentRepository,
        WorkSessionRepository sessionRepository,
        WorkTypeRepository workTypeRepository,
        LocationRepository locationRepository,
        AreaProgressService areaProgressService
    ) {
        this.assignmentRepository = assignmentRepository;
        this.sessionRepository = sessionRepository;
        this.workTypeRepository = workTypeRepository;
        this.locationRepository = locationRepository;
        this.areaProgressService = areaProgressService;
    }

    @Transactional(readOnly = true)
    public WorkEstimateResponse estimate(
        Long areaId,
        Long workTypeId,
        Long selectedStartLocationId
    ) {
        Location area = locationRepository.findById(areaId)
            .orElseThrow(() -> new NotFoundException(
                "LOCATION_NOT_FOUND",
                "구역을 찾을 수 없습니다."
            ));

        WorkType workType = workTypeRepository.findById(workTypeId)
            .orElseThrow(() -> new NotFoundException(
                "WORK_TYPE_NOT_FOUND",
                "업무 종류를 찾을 수 없습니다."
            ));

        Location selectedStart = selectedStartLocationId == null
            ? null
            : locationRepository.findById(selectedStartLocationId)
                .orElseThrow(() -> new NotFoundException(
                    "LOCATION_NOT_FOUND",
                    "시작 로케이션을 찾을 수 없습니다."
                ));

        if (
            selectedStart != null
                && !selectedStart.getFullCode().equals(area.getFullCode())
                && !selectedStart.getFullCode().startsWith(
                    area.getFullCode() + "-"
                )
        ) {
            throw new com.portfolio.warehouse.common.exception.BusinessException(
                "START_LOCATION_OUTSIDE_AREA",
                "선택한 시작 로케이션이 해당 구역에 속하지 않습니다."
            );
        }

        List<WorkAssignment> pairHistory =
            assignmentRepository
                .findAllByAreaLocationIdAndWorkTypeIdOrderByAssignedAtDesc(
                    areaId,
                    workTypeId
                );

        WorkAssignment latestWithProgress = pairHistory.stream()
            .filter(assignment ->
                assignment.getCurrentLastCompletedLocation() != null
            )
            .findFirst()
            .orElse(null);

        int currentProgressPercent = latestWithProgress == null
            ? 0
            : areaProgressService.areaPercentAt(
                area,
                latestWithProgress.getCurrentLastCompletedLocation()
            );

        String currentLastCompletedLocation = latestWithProgress == null
            ? null
            : latestWithProgress
                .getCurrentLastCompletedLocation()
                .getFullCode();

        double totalActualSeconds = 0.0;
        double totalWorkedFraction = 0.0;
        int sampleCount = 0;

        for (WorkAssignment assignment : pairHistory) {
            if (assignment.getStatus() != WorkAssignmentStatus.COMPLETED) {
                continue;
            }

            if (assignment.getCurrentLastCompletedLocation() == null) {
                continue;
            }

            List<WorkSession> sessions =
                sessionRepository.findAllByWorkAssignmentIdOrderByStartedAtAsc(
                    assignment.getId()
                );

            if (sessions.isEmpty()) continue;

            boolean reliable = sessions.stream().allMatch(session ->
                session.getEndedAt() != null
                    && session.getQualityStatus()
                        == WorkSessionQualityStatus.NORMAL
            );

            if (!reliable) continue;

            long actualSeconds = sessions.stream()
                .mapToLong(session -> session.getDuration().getSeconds())
                .sum();

            if (actualSeconds <= 0) continue;

            AreaProgressService.ProgressSnapshot snapshot =
                areaProgressService.snapshot(
                    area,
                    assignment.getStartLocation(),
                    assignment.getCurrentLastCompletedLocation()
                );

            if (snapshot.workedFraction() <= 0.0) continue;

            totalActualSeconds += actualSeconds;
            totalWorkedFraction += snapshot.workedFraction();
            sampleCount++;
        }

        Long estimatedFullAreaSeconds = null;
        Long estimatedRemainingFromCurrentSeconds = null;
        Long estimatedRemainingFromSelectedStartSeconds = null;

        if (sampleCount > 0 && totalWorkedFraction > 0.0) {
            double fullAreaSeconds =
                totalActualSeconds / totalWorkedFraction;

            estimatedFullAreaSeconds =
                Math.round(fullAreaSeconds);

            double currentRemainingFraction = 1.0;

            if (latestWithProgress != null) {
                currentRemainingFraction =
                    areaProgressService.snapshot(
                        area,
                        latestWithProgress.getStartLocation(),
                        latestWithProgress.getCurrentLastCompletedLocation()
                    ).remainingFraction();
            }

            estimatedRemainingFromCurrentSeconds =
                Math.round(
                    fullAreaSeconds * currentRemainingFraction
                );

            if (selectedStart != null) {
                double selectedRemainingFraction =
                    areaProgressService.remainingFractionFromStart(
                        area,
                        selectedStart
                    );

                estimatedRemainingFromSelectedStartSeconds =
                    Math.round(
                        fullAreaSeconds * selectedRemainingFraction
                    );
            }
        }

        Integer selectedStartPercent = selectedStart == null
            ? null
            : areaProgressService.startPercentAt(area, selectedStart);

        return new WorkEstimateResponse(
            area.getId(),
            area.getFullCode(),
            workType.getId(),
            workType.getName(),
            selectedStart == null ? null : selectedStart.getId(),
            selectedStart == null ? null : selectedStart.getFullCode(),
            selectedStartPercent,
            currentLastCompletedLocation,
            currentProgressPercent,
            estimatedFullAreaSeconds,
            estimatedRemainingFromCurrentSeconds,
            estimatedRemainingFromSelectedStartSeconds,
            sampleCount
        );
    }
}
