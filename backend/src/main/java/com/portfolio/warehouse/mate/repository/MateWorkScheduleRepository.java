package com.portfolio.warehouse.mate.repository;

import com.portfolio.warehouse.mate.domain.MateWorkSchedule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MateWorkScheduleRepository extends JpaRepository<MateWorkSchedule, Long> {
    List<MateWorkSchedule> findAllByMateIdOrderByDayOfWeekAsc(Long mateId);
    void deleteAllByMateId(Long mateId);
}
