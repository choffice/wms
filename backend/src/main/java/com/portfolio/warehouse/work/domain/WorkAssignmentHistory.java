package com.portfolio.warehouse.work.domain;

import com.portfolio.warehouse.auth.domain.UserAccount;
import com.portfolio.warehouse.mate.domain.Mate;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_assignment_history")
public class WorkAssignmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_assignment_id", nullable = false)
    private WorkAssignment workAssignment;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private WorkAssignmentActionType actionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_mate_id")
    private Mate fromMate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_mate_id")
    private Mate toMate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_account_id", nullable = false)
    private UserAccount actorAccount;

    @Column(length = 300)
    private String reason;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    protected WorkAssignmentHistory() {
    }

    public WorkAssignmentHistory(
        WorkAssignment workAssignment,
        WorkAssignmentActionType actionType,
        Mate fromMate,
        Mate toMate,
        UserAccount actorAccount,
        String reason
    ) {
        this.workAssignment = workAssignment;
        this.actionType = actionType;
        this.fromMate = fromMate;
        this.toMate = toMate;
        this.actorAccount = actorAccount;
        this.reason = reason;
        this.changedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public WorkAssignment getWorkAssignment() { return workAssignment; }
    public WorkAssignmentActionType getActionType() { return actionType; }
    public Mate getFromMate() { return fromMate; }
    public Mate getToMate() { return toMate; }
    public UserAccount getActorAccount() { return actorAccount; }
    public String getReason() { return reason; }
    public LocalDateTime getChangedAt() { return changedAt; }
}
