package com.portfolio.warehouse.mate.service;

import com.portfolio.warehouse.common.exception.NotFoundException;
import com.portfolio.warehouse.issue.api.dto.IssueTypeResponse;
import com.portfolio.warehouse.issue.repository.IssueTypeRepository;
import com.portfolio.warehouse.location.api.dto.LocationResponse;
import com.portfolio.warehouse.location.domain.Location;
import com.portfolio.warehouse.location.repository.LocationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MateLookupService {

    private final IssueTypeRepository issueTypeRepository;
    private final LocationRepository locationRepository;

    public MateLookupService(
        IssueTypeRepository issueTypeRepository,
        LocationRepository locationRepository
    ) {
        this.issueTypeRepository = issueTypeRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional(readOnly = true)
    public List<IssueTypeResponse> issueTypes() {
        return issueTypeRepository.findAll().stream()
            .filter(type -> type.isActive())
            .map(IssueTypeResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> locations(Long areaId) {
        if (areaId == null) {
            return locationRepository.findAllByOrderByFullCodeAsc().stream()
                .filter(Location::isActive)
                .map(LocationResponse::from)
                .toList();
        }

        Location area = locationRepository.findById(areaId)
            .orElseThrow(() -> new NotFoundException(
                "LOCATION_NOT_FOUND",
                "구역을 찾을 수 없습니다."
            ));

        String prefix = area.getFullCode();

        return locationRepository
            .findAllByFullCodeStartingWithOrderByFullCodeAsc(prefix)
            .stream()
            .filter(Location::isActive)
            .filter(location ->
                location.getFullCode().equals(prefix)
                    || location.getFullCode().startsWith(prefix + "-")
            )
            .map(LocationResponse::from)
            .toList();
    }
}
