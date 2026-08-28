package com.portfolio.warehouse.handover.domain;

import com.portfolio.warehouse.auth.domain.UserAccount;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "handover_note",
    indexes = @Index(
        name = "idx_handover_note_created_at",
        columnList = "created_at"
    )
)
public class HandoverNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "created_by_account_id",
        nullable = false
    )
    private UserAccount createdBy;

    @Column(name = "shift_date")
    private LocalDate shiftDate;

    @Column(nullable = false, length = 1200)
    private String content;

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private LocalDateTime createdAt;

    protected HandoverNote() {}

    public HandoverNote(
        UserAccount createdBy,
        LocalDate shiftDate,
        String content
    ) {
        this.createdBy = createdBy;
        this.shiftDate = shiftDate;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public UserAccount getCreatedBy() { return createdBy; }
    public LocalDate getShiftDate() { return shiftDate; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
