package com.portfolio.warehouse.log.domain;

import com.portfolio.warehouse.auth.domain.UserAccount;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "activity_log",
    indexes = {
        @Index(
            name = "idx_activity_log_created_at",
            columnList = "created_at"
        ),
        @Index(
            name = "idx_activity_log_type_created_at",
            columnList = "type, created_at"
        ),
        @Index(
            name = "idx_activity_log_actor_created_at",
            columnList = "actor_account_id, created_at"
        ),
        @Index(
            name = "idx_activity_log_reference",
            columnList = "reference_type, reference_id"
        )
    }
)
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActivityType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_account_id")
    private UserAccount actorAccount;

    @Column(name = "target_label", length = 100)
    private String targetLabel;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ActivityLog() {}

    public ActivityLog(
        ActivityType type,
        UserAccount actorAccount,
        String targetLabel,
        String message,
        String referenceType,
        Long referenceId
    ) {
        this.type = type;
        this.actorAccount = actorAccount;
        this.targetLabel = targetLabel;
        this.message = message;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public ActivityType getType() { return type; }
    public UserAccount getActorAccount() { return actorAccount; }
    public String getTargetLabel() { return targetLabel; }
    public String getMessage() { return message; }
    public String getReferenceType() { return referenceType; }
    public Long getReferenceId() { return referenceId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
