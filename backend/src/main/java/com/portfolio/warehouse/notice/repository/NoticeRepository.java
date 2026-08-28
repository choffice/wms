package com.portfolio.warehouse.notice.repository;

import com.portfolio.warehouse.notice.domain.Notice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByDeletedAtIsNullOrderByImportantDescDisplayOrderAscUpdatedAtDesc();
    List<Notice> findAllByDeletedAtIsNullAndVisibleTrueOrderByImportantDescDisplayOrderAscUpdatedAtDesc();
}
