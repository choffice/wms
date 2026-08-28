package com.portfolio.warehouse.work.repository;

import com.portfolio.warehouse.work.domain.WorkAssignmentHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkAssignmentHistoryRepository extends JpaRepository<WorkAssignmentHistory, Long> {
    List<WorkAssignmentHistory> findAllByWorkAssignmentIdOrderByChangedAtAsc(Long assignmentId);
}
