package com.portfolio.warehouse.work.service;

import com.portfolio.warehouse.common.exception.BusinessException;
import com.portfolio.warehouse.common.exception.NotFoundException;
import com.portfolio.warehouse.log.domain.ActivityType;
import com.portfolio.warehouse.log.service.BusinessAuditService;
import com.portfolio.warehouse.work.api.dto.*;
import com.portfolio.warehouse.work.domain.WorkType;
import com.portfolio.warehouse.work.repository.WorkTypeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkTypeService {

    private final WorkTypeRepository repository;
    private final BusinessAuditService auditService;

    public WorkTypeService(
        WorkTypeRepository repository,
        BusinessAuditService auditService
    ) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional
    public WorkTypeResponse create(
        WorkTypeRequest request
    ) {
        String name = request.name().trim();

        if (repository.findByName(name).isPresent()) {
            throw new BusinessException(
                "WORK_TYPE_DUPLICATED",
                "이미 존재하는 업무명입니다."
            );
        }

        WorkType entity =
            repository.save(
                new WorkType(
                    name,
                    trimToNull(
                        request.description()
                    )
                )
            );

        auditService.record(
            ActivityType.WORK_TYPE_CREATE,
            entity.getName(),
            "업무 종류 등록",
            "WORK_TYPE",
            entity.getId()
        );

        return WorkTypeResponse.from(entity);
    }

    @Transactional(readOnly = true)
    public List<WorkTypeResponse> findAll() {
        return repository
            .findAll()
            .stream()
            .map(WorkTypeResponse::from)
            .toList();
    }

    @Transactional
    public WorkTypeResponse update(
        Long id,
        WorkTypeRequest request
    ) {
        WorkType entity = find(id);
        String name = request.name().trim();
        String description =
            trimToNull(
                request.description()
            );

        repository.findByName(name)
            .filter(
                other ->
                    !other.getId().equals(id)
            )
            .ifPresent(other -> {
                throw new BusinessException(
                    "WORK_TYPE_DUPLICATED",
                    "이미 존재하는 업무명입니다."
                );
            });

        String before =
            entity.getName()
                + " / "
                + value(entity.getDescription());

        String after =
            name
                + " / "
                + value(description);

        entity.update(
            name,
            description
        );

        if (!before.equals(after)) {
            auditService.record(
                ActivityType.WORK_TYPE_UPDATE,
                entity.getName(),
                "업무 종류 변경 · "
                    + before
                    + " → "
                    + after,
                "WORK_TYPE",
                entity.getId()
            );
        }

        return WorkTypeResponse.from(entity);
    }

    @Transactional
    public WorkTypeResponse deactivate(Long id) {
        WorkType entity = find(id);

        if (!entity.isActive()) {
            return WorkTypeResponse.from(entity);
        }

        entity.deactivate();

        auditService.record(
            ActivityType.WORK_TYPE_DEACTIVATE,
            entity.getName(),
            "업무 종류 비활성 처리",
            "WORK_TYPE",
            entity.getId()
        );

        return WorkTypeResponse.from(entity);
    }

    private WorkType find(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException(
                "WORK_TYPE_NOT_FOUND",
                "업무 종류를 찾을 수 없습니다."
            ));
    }

    private String trimToNull(String value) {
        return value == null
            || value.isBlank()
                ? null
                : value.trim();
    }

    private String value(String value) {
        return value == null
            ? "-"
            : value;
    }
}
