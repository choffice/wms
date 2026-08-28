package com.portfolio.warehouse.work.domain;

import com.portfolio.warehouse.auth.domain.UserAccount;
import com.portfolio.warehouse.location.domain.Location;
import com.portfolio.warehouse.mate.domain.Mate;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_assignment")
public class WorkAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_type_id", nullable = false)
    private WorkType workType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_location_id", nullable = false)
    private Location areaLocation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "start_location_id", nullable = false)
    private Location startLocation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "current_mate_id", nullable = false)
    private Mate currentMate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by_account_id", nullable = false)
    private UserAccount assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_last_completed_location_id")
    private Location currentLastCompletedLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkAssignmentStatus status = WorkAssignmentStatus.ASSIGNED;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected WorkAssignment() {
    }

    public WorkAssignment(
        WorkType workType,
        Location areaLocation,
        Location startLocation,
        Mate currentMate,
        UserAccount assignedBy
    ) {
        this.workType = workType;
        this.areaLocation = areaLocation;
        this.startLocation = startLocation;
        this.currentMate = currentMate;
        this.assignedBy = assignedBy;
        this.assignedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public WorkType getWorkType() { return workType; }
    public Location getAreaLocation() { return areaLocation; }
    public Location getStartLocation() { return startLocation; }
    public Mate getCurrentMate() { return currentMate; }
    public UserAccount getAssignedBy() { return assignedBy; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public Location getCurrentLastCompletedLocation() { return currentLastCompletedLocation; }
    public WorkAssignmentStatus getStatus() { return status; }
    public LocalDateTime getCompletedAt() { return completedAt; }

    public void start() {
        if (status == WorkAssignmentStatus.COMPLETED || status == WorkAssignmentStatus.CANCELED) {
            throw new IllegalStateException("종료된 업무는 시작할 수 없습니다.");
        }
        this.status = WorkAssignmentStatus.IN_PROGRESS;
    }

    public void updateLastCompletedLocation(Location location) {
        this.currentLastCompletedLocation = location;
    }

    public void tradeTo(Mate mate) {
        this.currentMate = mate;
    }

    public void complete(LocalDateTime completedAt) {
        this.status = WorkAssignmentStatus.COMPLETED;
        this.completedAt = completedAt;
    }

    public void cancel() {
        this.status = WorkAssignmentStatus.CANCELED;
    }
}
