package com.portfolio.warehouse.handover.service;

import com.portfolio.warehouse.auth.domain.UserAccount;
import com.portfolio.warehouse.auth.service.CurrentUserService;
import com.portfolio.warehouse.common.event.OperationalEventService;
import com.portfolio.warehouse.common.exception.BusinessException;
import com.portfolio.warehouse.common.exception.NotFoundException;
import com.portfolio.warehouse.handover.api.dto.*;
import com.portfolio.warehouse.log.domain.ActivityType;
import com.portfolio.warehouse.mate.domain.Mate;
import com.portfolio.warehouse.mate.repository.MateRepository;
import com.portfolio.warehouse.work.api.dto.WorkAssignmentResponse;
import com.portfolio.warehouse.work.domain.*;
import com.portfolio.warehouse.work.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HandoverActionService {

    private final WorkAssignmentRepository assignmentRepository;
    private final WorkSessionRepository sessionRepository;
    private final WorkAssignmentHistoryRepository historyRepository;
    private final MateRepository mateRepository;
    private final CurrentUserService currentUserService;
    private final OperationalEventService eventService;

    public HandoverActionService(
        WorkAssignmentRepository assignmentRepository,
        WorkSessionRepository sessionRepository,
        WorkAssignmentHistoryRepository historyRepository,
        MateRepository mateRepository,
        CurrentUserService currentUserService,
        OperationalEventService eventService
    ) {
        this.assignmentRepository = assignmentRepository;
        this.sessionRepository = sessionRepository;
        this.historyRepository = historyRepository;
        this.mateRepository = mateRepository;
        this.currentUserService = currentUserService;
        this.eventService = eventService;
    }

    @Transactional
    public BulkHandoverResultResponse bulkTransfer(
        BulkHandoverRequest request
    ) {
        List<BulkHandoverItemRequest> items =
            List.copyOf(request.items());

        Set<Long> assignmentIds = new HashSet<>();
        for (BulkHandoverItemRequest item : items) {
            if (!assignmentIds.add(item.assignmentId())) {
                throw new BusinessException(
                    "HANDOVER_DUPLICATE_ASSIGNMENT",
                    "같은 Assignment가 일괄 인수인계 목록에 중복되어 있습니다."
                );
            }

            if (
                item.expectedCurrentMateId()
                    .equals(item.toMateId())
            ) {
                throw new BusinessException(
                    "HANDOVER_SAME_MATE",
                    "현재 담당자와 새 담당자가 같은 업무가 포함되어 있습니다."
                );
            }
        }

        Map<Long, WorkAssignment> assignments =
            lockAssignments(
                assignmentIds.stream()
                    .sorted()
                    .toList()
            );

        Set<Long> mateIds = items.stream()
            .flatMap(item ->
                java.util.stream.Stream.of(
                    item.expectedCurrentMateId(),
                    item.toMateId()
                )
            )
            .collect(
                java.util.stream.Collectors.toSet()
            );

        Map<Long, Mate> mates =
            lockMates(
                mateIds.stream()
                    .sorted()
                    .toList()
            );

        // 모든 행을 먼저 검증한다.
        // 한 건이라도 오래된 화면/진행중 세션/비활성 담당자라면
        // 실제 변경은 하나도 적용하지 않고 전체 Transaction을 중단한다.
        for (BulkHandoverItemRequest item : items) {
            WorkAssignment assignment =
                assignments.get(item.assignmentId());

            validateTransfer(
                assignment,
                item,
                mates
            );
        }

        UserAccount admin =
            currentUserService.account();

        List<WorkAssignmentResponse> changed =
            new ArrayList<>();

        for (BulkHandoverItemRequest item : items) {
            WorkAssignment assignment =
                assignments.get(item.assignmentId());

            Mate fromMate =
                assignment.getCurrentMate();

            Mate toMate =
                mates.get(item.toMateId());

            assignment.tradeTo(toMate);

            String reason =
                trimToNull(item.reason());

            historyRepository.save(
                new WorkAssignmentHistory(
                    assignment,
                    WorkAssignmentActionType.REASSIGN,
                    fromMate,
                    toMate,
                    admin,
                    reason
                )
            );

            eventService.publish(
                ActivityType.WORK_HANDOVER,
                admin,
                toMate.getNickname(),
                "Assignment #"
                    + assignment.getId()
                    + " 인수인계 · "
                    + fromMate.getNickname()
                    + " → "
                    + toMate.getNickname()
                    + " / 마지막 위치 "
                    + (
                        assignment
                            .getCurrentLastCompletedLocation()
                            == null
                                ? "미기록"
                                : assignment
                                    .getCurrentLastCompletedLocation()
                                    .getFullCode()
                    ),
                "WORK_ASSIGNMENT",
                assignment.getId(),
                true,
                true
            );

            changed.add(
                WorkAssignmentResponse.from(assignment)
            );
        }

        if (changed.size() > 1) {
            eventService.publish(
                ActivityType.WORK_BULK_HANDOVER,
                admin,
                "일괄 인수인계",
                "선택 Assignment "
                    + changed.size()
                    + "건 인수인계 처리",
                "WORK_ASSIGNMENT_BATCH",
                null,
                true,
                false
            );
        }

        return new BulkHandoverResultResponse(
            changed.size(),
            List.copyOf(changed)
        );
    }

    private Map<Long, WorkAssignment> lockAssignments(
        List<Long> ids
    ) {
        Map<Long, WorkAssignment> result =
            new LinkedHashMap<>();

        for (Long id : ids) {
            WorkAssignment assignment =
                assignmentRepository
                    .findByIdForUpdate(id)
                    .orElseThrow(() ->
                        new NotFoundException(
                            "ASSIGNMENT_NOT_FOUND",
                            "업무배정을 찾을 수 없습니다. #"
                                + id
                        )
                    );

            result.put(id, assignment);
        }

        return result;
    }

    private Map<Long, Mate> lockMates(
        List<Long> ids
    ) {
        Map<Long, Mate> result =
            new HashMap<>();

        for (Long id : ids) {
            Mate mate =
                mateRepository
                    .findByIdForUpdate(id)
                    .orElseThrow(() ->
                        new NotFoundException(
                            "MATE_NOT_FOUND",
                            "MATE를 찾을 수 없습니다. #"
                                + id
                        )
                    );

            result.put(id, mate);
        }

        return result;
    }

    private void validateTransfer(
        WorkAssignment assignment,
        BulkHandoverItemRequest item,
        Map<Long, Mate> mates
    ) {
        if (
            assignment.getStatus()
                == WorkAssignmentStatus.COMPLETED
                || assignment.getStatus()
                    == WorkAssignmentStatus.CANCELED
        ) {
            throw new BusinessException(
                "HANDOVER_ASSIGNMENT_CLOSED",
                "종료된 업무가 일괄 인수인계 목록에 포함되어 있습니다. #"
                    + assignment.getId()
            );
        }

        if (
            !assignment
                .getCurrentMate()
                .getId()
                .equals(
                    item.expectedCurrentMateId()
                )
        ) {
            throw new BusinessException(
                "HANDOVER_STALE_MATE",
                "Assignment #"
                    + assignment.getId()
                    + "의 담당자가 다른 요청으로 변경되었습니다. 화면을 새로고침해주세요."
            );
        }

        if (
            sessionRepository
                .findFirstByWorkAssignmentIdAndEndedAtIsNull(
                    assignment.getId()
                )
                .isPresent()
        ) {
            throw new BusinessException(
                "HANDOVER_ACTIVE_SESSION",
                "Assignment #"
                    + assignment.getId()
                    + "에 Open WorkSession이 생겼습니다. 일괄 처리를 중단했습니다."
            );
        }

        Mate toMate =
            mates.get(item.toMateId());

        if (!toMate.isActive()) {
            throw new BusinessException(
                "HANDOVER_MATE_INACTIVE",
                toMate.getNickname()
                    + " MATE가 비활성 상태입니다."
            );
        }
    }

    private String trimToNull(String value) {
        return value == null
            || value.isBlank()
                ? null
                : value.trim();
    }
}
