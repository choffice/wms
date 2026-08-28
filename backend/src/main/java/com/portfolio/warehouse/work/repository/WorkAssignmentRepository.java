package com.portfolio.warehouse.work.repository;

import com.portfolio.warehouse.work.domain.WorkAssignment;
import com.portfolio.warehouse.work.domain.WorkAssignmentStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface WorkAssignmentRepository extends JpaRepository<WorkAssignment, Long> {

    List<WorkAssignment> findAllByCurrentMateIdAndStatusInOrderByAssignedAtDesc(
        Long mateId,
        List<WorkAssignmentStatus> statuses
    );

    List<WorkAssignment> findAllByOrderByAssignedAtDesc();

    List<WorkAssignment> findAllByAreaLocationIdAndWorkTypeIdOrderByAssignedAtDesc(
        Long areaLocationId,
        Long workTypeId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WorkAssignment w where w.id = :id")
    Optional<WorkAssignment> findByIdForUpdate(@Param("id") Long id);
}
