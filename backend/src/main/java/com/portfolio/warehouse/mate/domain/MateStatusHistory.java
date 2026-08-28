package com.portfolio.warehouse.mate.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mate_status_history")
public class MateStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mate_id", nullable = false)
    private Mate mate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MateStatus status;

    @Column(length = 100)
    private String whereabouts;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    protected MateStatusHistory() {
    }

    public MateStatusHistory(Mate mate, MateStatus status, String whereabouts) {
        this.mate = mate;
        this.status = status;
        this.whereabouts = whereabouts;
        this.changedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Mate getMate() { return mate; }
    public MateStatus getStatus() { return status; }
    public String getWhereabouts() { return whereabouts; }
    public LocalDateTime getChangedAt() { return changedAt; }
}
