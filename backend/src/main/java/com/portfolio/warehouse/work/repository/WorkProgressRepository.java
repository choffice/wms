package com.portfolio.warehouse.work.repository;

import com.portfolio.warehouse.work.domain.WorkProgress;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkProgressRepository extends JpaRepository<WorkProgress, Long> {
    List<WorkProgress> findAllByWorkAssignmentIdOrderByReportedAtAsc(Long assignmentId);

    Optional<WorkProgress> findFirstByWorkAssignmentIdOrderByReportedAtDescIdDesc(Long assignmentId);

    Optional<WorkProgress> findFirstByWorkAssignmentIdAndMateIdAndReportedAtLessThanOrderByReportedAtDesc(
        Long assignmentId,
        Long mateId,
        LocalDateTime before
    );
}
