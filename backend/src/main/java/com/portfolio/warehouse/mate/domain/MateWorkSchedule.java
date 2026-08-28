package com.portfolio.warehouse.mate.domain;

import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "mate_work_schedule")
public class MateWorkSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mate_id", nullable = false)
    private Mate mate;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 20)
    private ScheduleType scheduleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false, length = 20)
    private ShiftType shiftType;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    protected MateWorkSchedule() {
    }

    public MateWorkSchedule(
        Mate mate,
        DayOfWeek dayOfWeek,
        ScheduleType scheduleType,
        ShiftType shiftType,
        LocalTime startTime,
        LocalTime endTime
    ) {
        this.mate = mate;
        this.dayOfWeek = dayOfWeek;
        this.scheduleType = scheduleType;
        this.shiftType = shiftType;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getId() { return id; }
    public Mate getMate() { return mate; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public ScheduleType getScheduleType() { return scheduleType; }
    public ShiftType getShiftType() { return shiftType; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
}
