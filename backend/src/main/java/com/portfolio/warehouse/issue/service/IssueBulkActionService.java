package com.portfolio.warehouse.issue.service;

import com.portfolio.warehouse.auth.domain.UserAccount;
import com.portfolio.warehouse.auth.service.CurrentUserService;
import com.portfolio.warehouse.common.event.OperationalEventService;
import com.portfolio.warehouse.common.exception.BusinessException;
import com.portfolio.warehouse.common.exception.NotFoundException;
import com.portfolio.warehouse.issue.api.dto.*;
import com.portfolio.warehouse.issue.domain.*;
import com.portfolio.warehouse.issue.repository.*;
import com.portfolio.warehouse.log.domain.ActivityType;
import com.portfolio.warehouse.mate.domain.Mate;
import com.portfolio.warehouse.mate.repository.MateRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IssueBulkActionService {

    private final SpecialIssueRepository issueRepository;
    private final SpecialIssueHistoryRepository historyRepository;
    private final MateRepository mateRepository;
    private final CurrentUserService currentUserService;
    private final OperationalEventService eventService;

    public IssueBulkActionService(
        SpecialIssueRepository issueRepository,
        SpecialIssueHistoryRepository historyRepository,
        MateRepository mateRepository,
        CurrentUserService currentUserService,
        OperationalEventService eventService
    ) {
        this.issueRepository = issueRepository;
        this.historyRepository = historyRepository;
        this.mateRepository = mateRepository;
        this.currentUserService = currentUserService;
        this.eventService = eventService;
    }

    @Transactional
    public BulkIssueActionResult bulkConfirm(
        BulkIssueStatusRequest request
    ) {
        List<SpecialIssue> issues =
            lockAndValidateStatus(
                request.items(),
                IssueStatus.UNCONFIRMED
            );

        UserAccount admin =
            currentUserService.account();

        List<SpecialIssueResponse> changed =
            new ArrayList<>();

        for (SpecialIssue issue : issues) {
            issue.confirm();

            historyRepository.save(
                new SpecialIssueHistory(
                    issue,
                    SpecialIssueHistoryAction.CONFIRM,
                    issue.getResponsibleMate(),
                    issue.getResponsibleMate(),
                    admin,
                    "선택 일괄 확인"
                )
            );

            eventService.publish(
                ActivityType.ISSUE_CONFIRM,
                admin,
                issue.getAuthorMate().getNickname(),
                issue.getIssueType().getName()
                    + " 일괄 확인",
                "SPECIAL_ISSUE",
                issue.getId(),
                true,
                true
            );

            changed.add(
                SpecialIssueResponse.from(issue)
            );
        }

        publishSummary(
            admin,
            "특이사항 일괄 확인",
            changed.size()
        );

        return new BulkIssueActionResult(
            changed.size(),
            List.copyOf(changed)
        );
    }

    @Transactional
    public BulkIssueActionResult bulkResolve(
        BulkIssueStatusRequest request
    ) {
        List<SpecialIssue> issues =
            lockAndValidateStatus(
                request.items(),
                IssueStatus.CONFIRMED
            );

        UserAccount admin =
            currentUserService.account();

        List<SpecialIssueResponse> changed =
            new ArrayList<>();

        for (SpecialIssue issue : issues) {
            issue.resolve();

            historyRepository.save(
                new SpecialIssueHistory(
                    issue,
                    SpecialIssueHistoryAction.RESOLVE,
                    issue.getResponsibleMate(),
                    issue.getResponsibleMate(),
                    admin,
                    "선택 일괄 해결"
                )
            );

            eventService.publish(
                ActivityType.ISSUE_RESOLVE,
                admin,
                issue.getAuthorMate().getNickname(),
                issue.getIssueType().getName()
                    + " 일괄 해결",
                "SPECIAL_ISSUE",
                issue.getId(),
                true,
                true
            );

            changed.add(
                SpecialIssueResponse.from(issue)
            );
        }

        publishSummary(
            admin,
            "특이사항 일괄 해결",
            changed.size()
        );

        return new BulkIssueActionResult(
            changed.size(),
            List.copyOf(changed)
        );
    }

    @Transactional
    public BulkIssueActionResult bulkAssignResponsible(
        BulkIssueResponsibleRequest request
    ) {
        List<BulkIssueResponsibleItemRequest> items =
            List.copyOf(request.items());

        ensureUniqueIds(
            items.stream()
                .map(
                    BulkIssueResponsibleItemRequest::issueId
                )
                .toList()
        );

        Map<Long, SpecialIssue> issues =
            lockIssues(
                items.stream()
                    .map(
                        BulkIssueResponsibleItemRequest::issueId
                    )
                    .sorted()
                    .toList()
            );

        Mate toMate =
            request.toMateId() == null
                ? null
                : mateRepository
                    .findByIdForUpdate(
                        request.toMateId()
                    )
                    .orElseThrow(() ->
                        new NotFoundException(
                            "MATE_NOT_FOUND",
                            "MATE를 찾을 수 없습니다."
                        )
                    );

        if (
            toMate != null
                && !toMate.isActive()
        ) {
            throw new BusinessException(
                "MATE_INACTIVE",
                "비활성 MATE를 담당자로 지정할 수 없습니다."
            );
        }

        for (
            BulkIssueResponsibleItemRequest item :
            items
        ) {
            SpecialIssue issue =
                issues.get(item.issueId());

            if (issue.getDeletedAt() != null) {
                throw new BusinessException(
                    "ISSUE_DELETED",
                    "삭제된 특이사항이 선택 목록에 포함되어 있습니다. #"
                        + issue.getId()
                );
            }

            Long actual =
                issue.getResponsibleMate() == null
                    ? null
                    : issue
                        .getResponsibleMate()
                        .getId();

            if (
                !Objects.equals(
                    actual,
                    item.expectedResponsibleMateId()
                )
            ) {
                throw new BusinessException(
                    "ISSUE_STALE_RESPONSIBLE",
                    "특이사항 #"
                        + issue.getId()
                        + "의 담당자가 다른 요청으로 변경되었습니다. 목록을 새로고침해주세요."
                );
            }
        }

        UserAccount admin =
            currentUserService.account();

        String reason =
            trimToNull(request.reason());

        if (reason == null) {
            reason = "선택 일괄 담당 변경";
        }

        List<SpecialIssueResponse> changed =
            new ArrayList<>();

        for (
            BulkIssueResponsibleItemRequest item :
            items
        ) {
            SpecialIssue issue =
                issues.get(item.issueId());

            Mate from =
                issue.getResponsibleMate();

            if (
                Objects.equals(
                    from == null
                        ? null
                        : from.getId(),
                    toMate == null
                        ? null
                        : toMate.getId()
                )
            ) {
                changed.add(
                    SpecialIssueResponse.from(issue)
                );
                continue;
            }

            issue.assignResponsible(toMate);

            historyRepository.save(
                new SpecialIssueHistory(
                    issue,
                    SpecialIssueHistoryAction.RESPONSIBLE_CHANGE,
                    from,
                    toMate,
                    admin,
                    reason
                )
            );

            eventService.publish(
                ActivityType.ISSUE_ASSIGN,
                admin,
                toMate == null
                    ? "미담당"
                    : toMate.getNickname(),
                issue.getIssueType().getName()
                    + " 담당 "
                    + (
                        from == null
                            ? "미담당"
                            : from.getNickname()
                    )
                    + " → "
                    + (
                        toMate == null
                            ? "미담당"
                            : toMate.getNickname()
                    ),
                "SPECIAL_ISSUE",
                issue.getId(),
                true,
                true
            );

            changed.add(
                SpecialIssueResponse.from(issue)
            );
        }

        publishSummary(
            admin,
            "특이사항 일괄 담당 변경",
            changed.size()
        );

        return new BulkIssueActionResult(
            changed.size(),
            List.copyOf(changed)
        );
    }

    private List<SpecialIssue> lockAndValidateStatus(
        List<BulkIssueStatusItemRequest> items,
        IssueStatus requiredStatus
    ) {
        ensureUniqueIds(
            items.stream()
                .map(
                    BulkIssueStatusItemRequest::issueId
                )
                .toList()
        );

        Map<Long, SpecialIssue> locked =
            lockIssues(
                items.stream()
                    .map(
                        BulkIssueStatusItemRequest::issueId
                    )
                    .sorted()
                    .toList()
            );

        for (BulkIssueStatusItemRequest item : items) {
            SpecialIssue issue =
                locked.get(item.issueId());

            if (issue.getDeletedAt() != null) {
                throw new BusinessException(
                    "ISSUE_DELETED",
                    "삭제된 특이사항이 선택 목록에 포함되어 있습니다. #"
                        + issue.getId()
                );
            }

            IssueStatus expected;

            try {
                expected =
                    IssueStatus.valueOf(
                        item.expectedStatus()
                    );
            } catch (IllegalArgumentException e) {
                throw new BusinessException(
                    "ISSUE_STATUS_INVALID",
                    "유효하지 않은 특이사항 상태입니다."
                );
            }

            if (
                issue.getStatus() != expected
            ) {
                throw new BusinessException(
                    "ISSUE_STALE_STATUS",
                    "특이사항 #"
                        + issue.getId()
                        + "의 상태가 다른 요청으로 변경되었습니다. 목록을 새로고침해주세요."
                );
            }

            if (
                issue.getStatus()
                    != requiredStatus
            ) {
                throw new BusinessException(
                    "ISSUE_BULK_ACTION_STATE",
                    "특이사항 #"
                        + issue.getId()
                        + "은(는) 현재 일괄 처리 대상 상태가 아닙니다."
                );
            }
        }

        return items.stream()
            .map(item ->
                locked.get(item.issueId())
            )
            .toList();
    }

    private Map<Long, SpecialIssue> lockIssues(
        List<Long> sortedIds
    ) {
        Map<Long, SpecialIssue> result =
            new LinkedHashMap<>();

        for (Long id : sortedIds) {
            SpecialIssue issue =
                issueRepository
                    .findByIdForUpdate(id)
                    .orElseThrow(() ->
                        new NotFoundException(
                            "ISSUE_NOT_FOUND",
                            "특이사항을 찾을 수 없습니다. #"
                                + id
                        )
                    );

            result.put(id, issue);
        }

        return result;
    }

    private void ensureUniqueIds(
        List<Long> ids
    ) {
        if (
            new HashSet<>(ids).size()
                != ids.size()
        ) {
            throw new BusinessException(
                "ISSUE_BULK_DUPLICATED",
                "동일한 특이사항이 선택 목록에 중복되어 있습니다."
            );
        }
    }

    private void publishSummary(
        UserAccount admin,
        String action,
        int count
    ) {
        if (count <= 1) {
            return;
        }

        eventService.publish(
            ActivityType.ISSUE_BULK_ACTION,
            admin,
            action,
            "선택 특이사항 "
                + count
                + "건 처리",
            "SPECIAL_ISSUE_BATCH",
            null,
            true,
            false
        );
    }

    private String trimToNull(
        String value
    ) {
        return value == null
            || value.isBlank()
                ? null
                : value.trim();
    }
}
