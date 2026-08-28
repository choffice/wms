package com.portfolio.warehouse.mate.repository;

import com.portfolio.warehouse.mate.domain.ScheduleOverrideType;
import com.portfolio.warehouse.mate.domain.WorkScheduleOverride;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkScheduleOverrideRepository extends JpaRepository<WorkScheduleOverride, Long> {

    List<WorkScheduleOverride> findAllByMateIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        Long mateId,
        LocalDate date1,
        LocalDate date2
    );

    List<WorkScheduleOverride> findAllByMateIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndOverrideType(
        Long mateId,
        LocalDate date1,
        LocalDate date2,
        ScheduleOverrideType overrideType
    );

    boolean existsByMateIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndOverrideTypeAndAutoEndDisabledTrue(
        Long mateId,
        LocalDate date1,
        LocalDate date2,
        ScheduleOverrideType overrideType
    );
}
