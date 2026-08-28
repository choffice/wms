package com.portfolio.warehouse.location.service;

import com.portfolio.warehouse.common.exception.BusinessException;
import com.portfolio.warehouse.common.exception.NotFoundException;
import com.portfolio.warehouse.location.api.dto.*;
import com.portfolio.warehouse.location.domain.Location;
import com.portfolio.warehouse.location.repository.LocationRepository;
import com.portfolio.warehouse.log.domain.ActivityType;
import com.portfolio.warehouse.log.service.BusinessAuditService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocationAdminService {

    private final LocationRepository repository;
    private final BusinessAuditService auditService;

    public LocationAdminService(
        LocationRepository repository,
        BusinessAuditService auditService
    ) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional
    public List<LocationResponse> createAreaRange(
        LocationAreaRangeRequest request
    ) {
        if (
            request.endNumber()
                < request.startNumber()
        ) {
            throw new BusinessException(
                "INVALID_LOCATION_RANGE",
                "종료 번호는 시작 번호보다 작을 수 없습니다."
            );
        }

        int width =
            request.width() == null
                ? 2
                : request.width();

        String alphabet =
            request.alphabet()
                .trim()
                .toUpperCase();

        List<Location> created =
            new ArrayList<>();

        for (
            int number = request.startNumber();
            number <= request.endNumber();
            number++
        ) {
            String code =
                alphabet
                    + String.format(
                        "%0" + width + "d",
                        number
                    );

            if (repository.existsByFullCode(code)) {
                continue;
            }

            Location location =
                new Location(
                    null,
                    code,
                    code,
                    1
                );

            location.updateMetadata(
                request.floor(),
                request.foodType(),
                request.nonFoodCategories()
            );

            created.add(
                repository.save(location)
            );
        }

        if (!created.isEmpty()) {
            Location first = created.get(0);
            Location last =
                created.get(created.size() - 1);

            auditService.record(
                ActivityType.LOCATION_CREATE,
                first.getFullCode()
                    + (
                        created.size() == 1
                            ? ""
                            : " ~ "
                                + last.getFullCode()
                    ),
                "최상위 구역 일괄 생성 · "
                    + created.size()
                    + "건",
                "LOCATION",
                first.getId()
            );
        }

        return created.stream()
            .map(LocationResponse::from)
            .toList();
    }

    @Transactional
    public LocationResponse createRoot(
        String segment
    ) {
        String normalized = normalize(segment);

        if (
            repository
                .existsByParentIsNullAndSegment(
                    normalized
                )
                || repository
                    .existsByFullCode(normalized)
        ) {
            throw new BusinessException(
                "LOCATION_DUPLICATED",
                "이미 존재하는 최상위 로케이션입니다."
            );
        }

        Location location =
            repository.save(
                new Location(
                    null,
                    normalized,
                    normalized,
                    1
                )
            );

        auditService.record(
            ActivityType.LOCATION_CREATE,
            location.getFullCode(),
            "최상위 로케이션 생성",
            "LOCATION",
            location.getId()
        );

        return LocationResponse.from(location);
    }

    @Transactional
    public LocationResponse addChild(
        Long parentId,
        String segment
    ) {
        Location parent = find(parentId);
        String normalized = normalize(segment);

        if (
            repository
                .existsByParentIdAndSegment(
                    parentId,
                    normalized
                )
        ) {
            throw new BusinessException(
                "LOCATION_DUPLICATED",
                "같은 단계에 동일한 로케이션 값이 존재합니다."
            );
        }

        String fullCode =
            parent.getFullCode()
                + "-"
                + normalized;

        if (repository.existsByFullCode(fullCode)) {
            throw new BusinessException(
                "LOCATION_DUPLICATED",
                "이미 존재하는 로케이션입니다."
            );
        }

        Location child =
            repository.save(
                new Location(
                    parent,
                    normalized,
                    fullCode,
                    parent.getDepth() + 1
                )
            );

        auditService.record(
            ActivityType.LOCATION_CREATE,
            child.getFullCode(),
            "하위 로케이션 생성 · 부모 "
                + parent.getFullCode(),
            "LOCATION",
            child.getId()
        );

        return LocationResponse.from(child);
    }

    @Transactional
    public LocationResponse addSibling(
        Long referenceId,
        String segment
    ) {
        Location reference = find(referenceId);

        if (reference.getParent() == null) {
            return createRoot(segment);
        }

        return addChild(
            reference.getParent().getId(),
            segment
        );
    }

    @Transactional
    public List<LocationResponse> addNumericChildren(
        Long parentId,
        int startNumber,
        int endNumber,
        Integer width
    ) {
        if (endNumber < startNumber) {
            throw new BusinessException(
                "INVALID_LOCATION_RANGE",
                "끝 번호는 시작 번호보다 작을 수 없습니다."
            );
        }

        int resolvedWidth =
            width == null ? 2 : width;

        Location parent = find(parentId);
        List<Location> created =
            new ArrayList<>();

        for (
            int i = startNumber;
            i <= endNumber;
            i++
        ) {
            String segment =
                String.format(
                    "%0"
                        + resolvedWidth
                        + "d",
                    i
                );

            if (
                repository
                    .existsByParentIdAndSegment(
                        parentId,
                        segment
                    )
            ) {
                continue;
            }

            String fullCode =
                parent.getFullCode()
                    + "-"
                    + segment;

            created.add(
                repository.save(
                    new Location(
                        parent,
                        segment,
                        fullCode,
                        parent.getDepth() + 1
                    )
                )
            );
        }

        if (!created.isEmpty()) {
            Location first = created.get(0);
            Location last =
                created.get(created.size() - 1);

            auditService.record(
                ActivityType.LOCATION_CREATE,
                parent.getFullCode(),
                "하위 로케이션 범위 생성 · "
                    + first.getFullCode()
                    + " ~ "
                    + last.getFullCode()
                    + " / "
                    + created.size()
                    + "건",
                "LOCATION",
                parent.getId()
            );
        }

        return created.stream()
            .map(LocationResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> findAll() {
        return repository
            .findAllByOrderByFullCodeAsc()
            .stream()
            .map(LocationResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> findChildren(
        Long parentId
    ) {
        find(parentId);

        return repository
            .findAllByParentIdOrderByFullCodeAsc(
                parentId
            )
            .stream()
            .map(LocationResponse::from)
            .toList();
    }

    @Transactional
    public LocationResponse deactivate(
        Long locationId
    ) {
        Location location = find(locationId);

        if (!location.isActive()) {
            return LocationResponse.from(location);
        }

        location.deactivate();

        auditService.record(
            ActivityType.LOCATION_DEACTIVATE,
            location.getFullCode(),
            "로케이션 비활성 처리",
            "LOCATION",
            location.getId()
        );

        return LocationResponse.from(location);
    }

    @Transactional
    public LocationResponse updateMetadata(
        Long locationId,
        LocationMetadataRequest request
    ) {
        Location location = find(locationId);

        String before =
            metadata(location);

        location.updateMetadata(
            request.floor(),
            request.foodType(),
            request.nonFoodCategories()
        );

        String after =
            metadata(location);

        if (!before.equals(after)) {
            auditService.record(
                ActivityType.LOCATION_METADATA_CHANGE,
                location.getFullCode(),
                "속성 변경 · "
                    + before
                    + " → "
                    + after,
                "LOCATION",
                location.getId()
            );
        }

        return LocationResponse.from(location);
    }

    private Location find(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException(
                "LOCATION_NOT_FOUND",
                "로케이션을 찾을 수 없습니다."
            ));
    }

    private String normalize(String segment) {
        return segment
            .trim()
            .toUpperCase();
    }

    private String metadata(Location location) {
        return "floor="
            + location.getFloor()
            + ", food="
            + location.getFoodType()
            + ", category="
            + location.getNonFoodCategories();
    }
}
