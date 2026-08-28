package com.portfolio.warehouse.mate.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "work_schedule_override")
public class WorkScheduleOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mate_id", nullable = false)
    private Mate mate;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "override_type", nullable = false, length = 30)
    private ScheduleOverrideType overrideType;

    @Column(name = "auto_end_disabled", nullable = false)
    private boolean autoEndDisabled;

    protected WorkScheduleOverride() {
    }

    public WorkScheduleOverride(
        Mate mate,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        ScheduleOverrideType overrideType,
        boolean autoEndDisabled
    ) {
        this.mate = mate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.overrideType = overrideType;
        this.autoEndDisabled = autoEndDisabled;
    }

    public Long getId() { return id; }
    public Mate getMate() { return mate; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public ScheduleOverrideType getOverrideType() { return overrideType; }
    public boolean isAutoEndDisabled() { return autoEndDisabled; }
}
