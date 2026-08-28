package com.portfolio.warehouse.work.repository;

import com.portfolio.warehouse.work.domain.WorkType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkTypeRepository extends JpaRepository<WorkType, Long> {
    Optional<WorkType> findByName(String name);
}
