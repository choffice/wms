package com.portfolio.warehouse.issue.repository;

import com.portfolio.warehouse.issue.domain.SpecialIssueHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecialIssueHistoryRepository
    extends JpaRepository<SpecialIssueHistory, Long> {

    List<SpecialIssueHistory>
        findAllBySpecialIssueIdOrderByChangedAtAsc(Long issueId);
}
