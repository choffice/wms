package com.portfolio.warehouse.handover.repository;

import com.portfolio.warehouse.handover.domain.HandoverNote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HandoverNoteRepository
    extends JpaRepository<HandoverNote, Long> {

    List<HandoverNote>
        findTop20ByOrderByCreatedAtDescIdDesc();
}
