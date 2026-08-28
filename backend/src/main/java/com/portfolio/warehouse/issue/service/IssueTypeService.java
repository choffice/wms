package com.portfolio.warehouse.issue.service;

import com.portfolio.warehouse.common.exception.BusinessException;
import com.portfolio.warehouse.common.exception.NotFoundException;
import com.portfolio.warehouse.issue.api.dto.*;
import com.portfolio.warehouse.issue.domain.IssueType;
import com.portfolio.warehouse.issue.repository.IssueTypeRepository;
import com.portfolio.warehouse.log.domain.ActivityType;
import com.portfolio.warehouse.log.service.BusinessAuditService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IssueTypeService {

    private final IssueTypeRepository repository;
    private final BusinessAuditService auditService;

    public IssueTypeService(
        IssueTypeRepository repository,
        BusinessAuditService auditService
    ) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional
    public IssueTypeResponse create(
        IssueTypeRequest request
    ) {
        String name = request.name().trim();

        if (repository.findByName(name).isPresent()) {
            throw new BusinessException(
                "ISSUE_TYPE_DUPLICATED",
                "이미 존재하는 특이사항 구분입니다."
            );
        }

        IssueType entity =
            repository.save(
                new IssueType(
                    name,
                    request.requireLocation(),
                    request.requireProductCode(),
                    request.requireQuantity()
                )
            );

        auditService.record(
            ActivityType.ISSUE_TYPE_CREATE,
            entity.getName(),
            "특이사항 구분 등록 · "
                + requirement(entity),
            "ISSUE_TYPE",
            entity.getId()
        );

        return IssueTypeResponse.from(entity);
    }

    @Transactional(readOnly = true)
    public List<IssueTypeResponse> findAll() {
        return repository
            .findAll()
            .stream()
            .map(IssueTypeResponse::from)
            .toList();
    }

    @Transactional
    public IssueTypeResponse update(
        Long id,
        IssueTypeRequest request
    ) {
        IssueType entity =
            repository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                    "ISSUE_TYPE_NOT_FOUND",
                    "특이사항 구분을 찾을 수 없습니다."
                ));

        String name = request.name().trim();

        repository.findByName(name)
            .filter(
                other ->
                    !other.getId().equals(id)
            )
            .ifPresent(other -> {
                throw new BusinessException(
                    "ISSUE_TYPE_DUPLICATED",
                    "이미 존재하는 특이사항 구분입니다."
                );
            });

        String before =
            entity.getName()
                + " / "
                + requirement(entity);

        entity.update(
            name,
            request.requireLocation(),
            request.requireProductCode(),
            request.requireQuantity()
        );

        String after =
            entity.getName()
                + " / "
                + requirement(entity);

        if (!before.equals(after)) {
            auditService.record(
                ActivityType.ISSUE_TYPE_UPDATE,
                entity.getName(),
                "특이사항 구분 변경 · "
                    + before
                    + " → "
                    + after,
                "ISSUE_TYPE",
                entity.getId()
            );
        }

        return IssueTypeResponse.from(entity);
    }

    @Transactional
    public IssueTypeResponse deactivate(Long id) {
        IssueType entity =
            repository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                    "ISSUE_TYPE_NOT_FOUND",
                    "특이사항 구분을 찾을 수 없습니다."
                ));

        if (!entity.isActive()) {
            return IssueTypeResponse.from(entity);
        }

        entity.deactivate();

        auditService.record(
            ActivityType.ISSUE_TYPE_DEACTIVATE,
            entity.getName(),
            "특이사항 구분 비활성 처리",
            "ISSUE_TYPE",
            entity.getId()
        );

        return IssueTypeResponse.from(entity);
    }

    private String requirement(IssueType entity) {
        return "location="
            + entity.isRequireLocation()
            + ", product="
            + entity.isRequireProductCode()
            + ", quantity="
            + entity.isRequireQuantity();
    }
}
