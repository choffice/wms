package com.portfolio.warehouse.work.service;

import com.portfolio.warehouse.location.domain.Location;
import com.portfolio.warehouse.location.repository.LocationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AreaProgressService {

    public record ProgressSnapshot(
        int totalLeafCount,
        int startLeafIndex,
        int endLeafIndex,
        int startPercent,
        int currentPercent,
        double workedFraction,
        double remainingFraction
    ) {}

    private final LocationRepository locationRepository;

    public AreaProgressService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @Transactional(readOnly = true)
    public List<Location> activeLeaves(Location area) {
        String prefix = area.getFullCode();

        List<Location> descendants = locationRepository
            .findAllByFullCodeStartingWithOrderByFullCodeAsc(prefix)
            .stream()
            .filter(Location::isActive)
            .filter(location ->
                location.getFullCode().equals(prefix)
                    || location.getFullCode().startsWith(prefix + "-")
            )
            .toList();

        return descendants.stream()
            .filter(candidate ->
                descendants.stream().noneMatch(other ->
                    !other.getId().equals(candidate.getId())
                        && other.getFullCode().startsWith(
                            candidate.getFullCode() + "-"
                        )
                )
            )
            .toList();
    }

    @Transactional(readOnly = true)
    public ProgressSnapshot snapshot(
        Location area,
        Location startLocation,
        Location lastCompletedLocation
    ) {
        List<Location> leaves = activeLeaves(area);

        if (leaves.isEmpty()) {
            return new ProgressSnapshot(
                0, -1, -1, 0, 0, 0.0, 1.0
            );
        }

        int startIndex = findStartIndex(leaves, startLocation);

        if (startIndex < 0) {
            return new ProgressSnapshot(
                leaves.size(), -1, -1, 0, 0, 0.0, 1.0
            );
        }

        int startPercent = percentBeforeIndex(startIndex, leaves.size());

        if (lastCompletedLocation == null) {
            return new ProgressSnapshot(
                leaves.size(),
                startIndex,
                -1,
                startPercent,
                startPercent,
                0.0,
                (leaves.size() - startIndex) / (double) leaves.size()
            );
        }

        int endIndex = findEndIndex(leaves, lastCompletedLocation);

        if (endIndex < startIndex) {
            return new ProgressSnapshot(
                leaves.size(),
                startIndex,
                endIndex,
                startPercent,
                startPercent,
                0.0,
                (leaves.size() - startIndex) / (double) leaves.size()
            );
        }

        double workedFraction =
            (endIndex - startIndex + 1) / (double) leaves.size();

        double remainingFraction =
            Math.max(
                0.0,
                (leaves.size() - (endIndex + 1))
                    / (double) leaves.size()
            );

        return new ProgressSnapshot(
            leaves.size(),
            startIndex,
            endIndex,
            startPercent,
            percentThroughIndex(endIndex, leaves.size()),
            workedFraction,
            remainingFraction
        );
    }

    @Transactional(readOnly = true)
    public int areaPercentAt(
        Location area,
        Location location
    ) {
        List<Location> leaves = activeLeaves(area);
        if (leaves.isEmpty() || location == null) return 0;

        int index = findEndIndex(leaves, location);
        return index < 0 ? 0 : percentThroughIndex(index, leaves.size());
    }

    @Transactional(readOnly = true)
    public int startPercentAt(
        Location area,
        Location startLocation
    ) {
        List<Location> leaves = activeLeaves(area);
        if (leaves.isEmpty() || startLocation == null) return 0;

        int index = findStartIndex(leaves, startLocation);
        return index < 0 ? 0 : percentBeforeIndex(index, leaves.size());
    }

    @Transactional(readOnly = true)
    public double remainingFractionFromStart(
        Location area,
        Location startLocation
    ) {
        List<Location> leaves = activeLeaves(area);
        if (leaves.isEmpty() || startLocation == null) return 1.0;

        int index = findStartIndex(leaves, startLocation);
        if (index < 0) return 1.0;

        return (leaves.size() - index) / (double) leaves.size();
    }

    private int findStartIndex(List<Location> leaves, Location location) {
        String code = location.getFullCode();

        for (int i = 0; i < leaves.size(); i++) {
            String leafCode = leaves.get(i).getFullCode();
            if (
                leafCode.equals(code)
                    || leafCode.startsWith(code + "-")
            ) {
                return i;
            }
        }

        return -1;
    }

    private int findEndIndex(List<Location> leaves, Location location) {
        String code = location.getFullCode();
        int result = -1;

        for (int i = 0; i < leaves.size(); i++) {
            String leafCode = leaves.get(i).getFullCode();

            if (
                leafCode.equals(code)
                    || leafCode.startsWith(code + "-")
            ) {
                result = i;
            }
        }

        return result;
    }

    private int percentBeforeIndex(int index, int total) {
        return (int) Math.round(index * 100.0 / total);
    }

    private int percentThroughIndex(int index, int total) {
        return (int) Math.round((index + 1) * 100.0 / total);
    }
}
