package com.portfolio.warehouse.pda.domain;

import com.portfolio.warehouse.mate.domain.Mate;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pda_usage_history")
public class PdaUsageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pda_device_id", nullable = false)
    private PdaDevice pdaDevice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mate_id", nullable = false)
    private Mate mate;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "release_reason", length = 30)
    private PdaReleaseReason releaseReason;

    protected PdaUsageHistory() {
    }

    public PdaUsageHistory(PdaDevice pdaDevice, Mate mate) {
        this.pdaDevice = pdaDevice;
        this.mate = mate;
        this.assignedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public PdaDevice getPdaDevice() { return pdaDevice; }
    public Mate getMate() { return mate; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public LocalDateTime getReleasedAt() { return releasedAt; }
    public PdaReleaseReason getReleaseReason() { return releaseReason; }

    public boolean isActiveUsage() {
        return releasedAt == null;
    }

    public void release(PdaReleaseReason reason) {
        if (this.releasedAt != null) {
            throw new IllegalStateException("이미 반납 처리된 PDA 사용 이력입니다.");
        }
        this.releasedAt = LocalDateTime.now();
        this.releaseReason = reason;
    }
}
