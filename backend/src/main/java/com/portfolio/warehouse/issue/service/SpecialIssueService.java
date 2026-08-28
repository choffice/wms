package com.portfolio.warehouse.issue.service;

import com.portfolio.warehouse.auth.domain.UserAccount;
import com.portfolio.warehouse.auth.service.CurrentUserService;
import com.portfolio.warehouse.common.event.OperationalEventService;
import com.portfolio.warehouse.common.exception.BusinessException;
import com.portfolio.warehouse.common.exception.NotFoundException;
import com.portfolio.warehouse.issue.api.dto.*;
import com.portfolio.warehouse.issue.domain.*;
import com.portfolio.warehouse.issue.repository.*;
import com.portfolio.warehouse.location.domain.Location;
import com.portfolio.warehouse.location.repository.LocationRepository;
import com.portfolio.warehouse.log.domain.ActivityType;
import com.portfolio.warehouse.mate.domain.Mate;
import com.portfolio.warehouse.mate.repository.MateRepository;
import com.portfolio.warehouse.work.domain.WorkAssignment;
import com.portfolio.warehouse.work.repository.WorkAssignmentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpecialIssueService {

    private final SpecialIssueRepository issueRepository;
    private final SpecialIssueHistoryRepository historyRepository;
    private final IssueTypeRepository typeRepository;
    private final LocationRepository locationRepository;
    private final WorkAssignmentRepository assignmentRepository;
    private final MateRepository mateRepository;
    private final CurrentUserService currentUserService;
    private final OperationalEventService eventService;

    public SpecialIssueService(
        SpecialIssueRepository issueRepository,
        SpecialIssueHistoryRepository historyRepository,
        IssueTypeRepository typeRepository,
        LocationRepository locationRepository,
        WorkAssignmentRepository assignmentRepository,
        MateRepository mateRepository,
        CurrentUserService currentUserService,
        OperationalEventService eventService
    ) {
        this.issueRepository = issueRepository;
        this.historyRepository = historyRepository;
        this.typeRepository = typeRepository;
        this.locationRepository = locationRepository;
        this.assignmentRepository = assignmentRepository;
        this.mateRepository = mateRepository;
        this.currentUserService = currentUserService;
        this.eventService = eventService;
    }

    @Transactional
    public SpecialIssueResponse create(
        SpecialIssueCreateRequest request
    ) {
        Mate author = currentUserService.mate();

        IssueType type = typeRepository.findById(request.issueTypeId())
            .orElseThrow(() -> new NotFoundException(
                "ISSUE_TYPE_NOT_FOUND",
                "특이사항 구분을 찾을 수 없습니다."
            ));

        if (!type.isActive()) {
            throw new BusinessException(
                "ISSUE_TYPE_INACTIVE",
                "비활성 특이사항 구분은 사용할 수 없습니다."
            );
        }

        Location location = request.locationId() == null
            ? null
            : locationRepository.findById(request.locationId())
                .orElseThrow(() -> new NotFoundException(
                    "LOCATION_NOT_FOUND",
                    "로케이션을 찾을 수 없습니다."
                ));

        WorkAssignment assignment =
            request.workAssignmentId() == null
                ? null
                : assignmentRepository
                    .findById(request.workAssignmentId())
                    .orElseThrow(() -> new NotFoundException(
                        "ASSIGNMENT_NOT_FOUND",
                        "업무배정을 찾을 수 없습니다."
                    ));

        if (
            assignment != null
                && !assignment.getCurrentMate().getId()
                    .equals(author.getId())
        ) {
            throw new BusinessException(
                "ISSUE_ASSIGNMENT_NOT_OWNED",
                "본인에게 배정된 업무만 특이사항에 연결할 수 있습니다."
            );
        }

        if (
            assignment != null
                && location != null
                && !location.getFullCode().equals(
                    assignment.getAreaLocation().getFullCode()
                )
                && !location.getFullCode().startsWith(
                    assignment.getAreaLocation().getFullCode() + "-"
                )
        ) {
            throw new BusinessException(
                "ISSUE_LOCATION_OUTSIDE_ASSIGNMENT",
                "선택한 로케이션이 연결한 업무 구역에 포함되지 않습니다."
            );
        }

        validateRequirements(type, request, location);

        Mate responsible = assignment == null
            ? null
            : assignment.getCurrentMate();

        SpecialIssue entity = issueRepository.save(
            new SpecialIssue(
                type,
                author,
                responsible,
                assignment,
                location,
                trimToNull(request.productCode()),
                request.quantity(),
                request.actualStock(),
                request.mmsStock(),
                request.expiryStock(),
                request.noStock(),
                request.comment().trim()
            )
        );

        historyRepository.save(
            new SpecialIssueHistory(
                entity,
                SpecialIssueHistoryAction.CREATE,
                null,
                responsible,
                author.getAccount(),
                null
            )
        );

        eventService.publish(
            ActivityType.ISSUE_CREATE,
            author.getAccount(),
            "관리자",
            type.getName() + " 요청",
            "SPECIAL_ISSUE",
            entity.getId(),
            true,
            false
        );

        return SpecialIssueResponse.from(entity);
    }

    @Transactional
    public List<SpecialIssueResponse> board() {
        List<SpecialIssue> issues =
            issueRepository
                .findAllByDeletedAtIsNullOrderByCreatedAtDesc();

        List<SpecialIssueResponse> response =
            issues.stream()
                .map(SpecialIssueResponse::from)
                .toList();

        issues.forEach(SpecialIssue::incrementViewCount);
        return response;
    }

    @Transactional(readOnly = true)
    public List<SpecialIssueResponse> mainUnconfirmed() {
        return issueRepository
            .findAllByDeletedAtIsNullAndStatusOrderByCreatedAtDesc(
                IssueStatus.UNCONFIRMED
            )
            .stream()
            .map(SpecialIssueResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public SpecialIssueResponse detail(Long id) {
        return SpecialIssueResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public List<SpecialIssueHistoryResponse> history(Long id) {
        find(id);

        return historyRepository
            .findAllBySpecialIssueIdOrderByChangedAtAsc(id)
            .stream()
            .map(SpecialIssueHistoryResponse::from)
            .toList();
    }

    @Transactional
    public SpecialIssueResponse assignResponsible(
        Long id,
        SpecialIssueResponsibleRequest request
    ) {
        SpecialIssue issue = findForUpdate(id);
        UserAccount admin = currentUserService.account();

        Mate from = issue.getResponsibleMate();

        Long actualResponsibleId =
            from == null
                ? null
                : from.getId();

        if (
            request.expectedResponsibleMateId() != null
                && !java.util.Objects.equals(
                    actualResponsibleId,
                    request.expectedResponsibleMateId()
                )
        ) {
            throw new BusinessException(
                "ISSUE_STALE_RESPONSIBLE",
                "특이사항 담당자가 다른 요청으로 변경되었습니다. 최신 이력을 다시 확인해주세요."
            );
        }

        if (
            request.expectedResponsibleMateId() == null
                && actualResponsibleId != null
        ) {
            throw new BusinessException(
                "ISSUE_STALE_RESPONSIBLE",
                "특이사항 담당자가 다른 요청으로 지정되었습니다. 최신 이력을 다시 확인해주세요."
            );
        }

        Mate to = request.mateId() == null
            ? null
            : mateRepository.findById(request.mateId())
                .orElseThrow(() -> new NotFoundException(
                    "MATE_NOT_FOUND",
                    "MATE를 찾을 수 없습니다."
                ));

        if (to != null && !to.isActive()) {
            throw new BusinessException(
                "MATE_INACTIVE",
                "비활성 MATE를 담당자로 지정할 수 없습니다."
            );
        }

        if (
            (from == null && to == null)
                || (
                    from != null
                        && to != null
                        && from.getId().equals(to.getId())
                )
        ) {
            return SpecialIssueResponse.from(issue);
        }

        issue.assignResponsible(to);

        historyRepository.save(
            new SpecialIssueHistory(
                issue,
                SpecialIssueHistoryAction.RESPONSIBLE_CHANGE,
                from,
                to,
                admin,
                trimToNull(request.reason())
            )
        );

        eventService.publish(
            ActivityType.ISSUE_ASSIGN,
            admin,
            to == null ? "미담당" : to.getNickname(),
            issue.getIssueType().getName()
                + " 담당 "
                + (from == null ? "미담당" : from.getNickname())
                + " → "
                + (to == null ? "미담당" : to.getNickname()),
            "SPECIAL_ISSUE",
            issue.getId(),
            true,
            true
        );

        return SpecialIssueResponse.from(issue);
    }

    @Transactional
    public SpecialIssueResponse confirm(Long id) {
        SpecialIssue issue = findForUpdate(id);

        if (
            issue.getStatus() == IssueStatus.CONFIRMED
                || issue.getStatus() == IssueStatus.RESOLVED
        ) {
            return SpecialIssueResponse.from(issue);
        }

        issue.confirm();

        historyRepository.save(
            new SpecialIssueHistory(
                issue,
                SpecialIssueHistoryAction.CONFIRM,
                issue.getResponsibleMate(),
                issue.getResponsibleMate(),
                currentUserService.account(),
                null
            )
        );

        eventService.publish(
            ActivityType.ISSUE_CONFIRM,
            currentUserService.account(),
            issue.getAuthorMate().getNickname(),
            issue.getIssueType().getName() + " 확인",
            "SPECIAL_ISSUE",
            issue.getId(),
            true,
            true
        );

        return SpecialIssueResponse.from(issue);
    }

    @Transactional
    public SpecialIssueResponse resolve(Long id) {
        SpecialIssue issue = findForUpdate(id);

        if (issue.getStatus() == IssueStatus.RESOLVED) {
            return SpecialIssueResponse.from(issue);
        }

        if (issue.getStatus() != IssueStatus.CONFIRMED) {
            throw new BusinessException(
                "ISSUE_NOT_CONFIRMED",
                "특이사항을 먼저 확인 처리한 뒤 해결해주세요."
            );
        }

        issue.resolve();

        historyRepository.save(
            new SpecialIssueHistory(
                issue,
                SpecialIssueHistoryAction.RESOLVE,
                issue.getResponsibleMate(),
                issue.getResponsibleMate(),
                currentUserService.account(),
                null
            )
        );

        eventService.publish(
            ActivityType.ISSUE_RESOLVE,
            currentUserService.account(),
            issue.getAuthorMate().getNickname(),
            issue.getIssueType().getName() + " 해결",
            "SPECIAL_ISSUE",
            issue.getId(),
            true,
            true
        );

        return SpecialIssueResponse.from(issue);
    }

    @Transactional
    public void delete(Long id) {
        SpecialIssue issue = findForUpdate(id);

        historyRepository.save(
            new SpecialIssueHistory(
                issue,
                SpecialIssueHistoryAction.DELETE,
                issue.getResponsibleMate(),
                issue.getResponsibleMate(),
                currentUserService.account(),
                null
            )
        );

        issue.delete();
    }

    private SpecialIssue findForUpdate(Long id) {
        SpecialIssue issue = issueRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new NotFoundException(
                "ISSUE_NOT_FOUND",
                "특이사항을 찾을 수 없습니다."
            ));

        if (issue.getDeletedAt() != null) {
            throw new NotFoundException(
                "ISSUE_NOT_FOUND",
                "삭제된 특이사항입니다."
            );
        }

        return issue;
    }

    private SpecialIssue find(Long id) {
        SpecialIssue issue = issueRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(
                "ISSUE_NOT_FOUND",
                "특이사항을 찾을 수 없습니다."
            ));

        if (issue.getDeletedAt() != null) {
            throw new NotFoundException(
                "ISSUE_NOT_FOUND",
                "삭제된 특이사항입니다."
            );
        }

        return issue;
    }

    private void validateRequirements(
        IssueType type,
        SpecialIssueCreateRequest request,
        Location location
    ) {
        if (type.isRequireLocation() && location == null) {
            throw new BusinessException(
                "ISSUE_LOCATION_REQUIRED",
                "이 구분은 로케이션 입력이 필요합니다."
            );
        }

        if (type.isRequireProductCode()
            && (
                request.productCode() == null
                    || request.productCode().isBlank()
            )) {
            throw new BusinessException(
                "ISSUE_PRODUCT_REQUIRED",
                "이 구분은 상품코드 입력이 필요합니다."
            );
        }

        if (type.isRequireQuantity()
            && request.quantity() == null) {
            throw new BusinessException(
                "ISSUE_QUANTITY_REQUIRED",
                "이 구분은 수량 입력이 필요합니다."
            );
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank()
            ? null
            : value.trim();
    }
}
