package com.portfolio.warehouse.work.repository;

import com.portfolio.warehouse.work.domain.WorkSession;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface WorkSessionRepository extends JpaRepository<WorkSession, Long> {

    Optional<WorkSession> findFirstByMateIdAndEndedAtIsNull(Long mateId);

    Optional<WorkSession> findFirstByWorkAssignmentIdAndEndedAtIsNull(Long assignmentId);

    List<WorkSession> findAllByWorkAssignmentIdOrderByStartedAtAsc(Long assignmentId);

    List<WorkSession> findAllByEndedAtIsNull();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from WorkSession s where s.id = :id")
    Optional<WorkSession> findByIdForUpdate(@Param("id") Long id);
}
