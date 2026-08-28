package com.portfolio.warehouse.mate.domain;

import com.portfolio.warehouse.auth.domain.UserAccount;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "mate",
    uniqueConstraints = @UniqueConstraint(name = "uk_mate_employee_no", columnNames = "employee_no")
)
public class Mate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private UserAccount account;

    @Column(name = "employee_no", nullable = false, updatable = false, length = 20)
    private String employeeNo;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private LocalDate joinedAt;

    private LocalDateTime deactivatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 20)
    private MateStatus currentStatus = MateStatus.OFF_DUTY;

    @Column(name = "current_whereabouts", length = 100)
    private String currentWhereabouts;

    protected Mate() {
    }

    public Mate(UserAccount account, String employeeNo, String name, String nickname, LocalDate joinedAt) {
        this.account = account;
        this.employeeNo = employeeNo;
        this.name = name;
        this.nickname = nickname;
        this.joinedAt = joinedAt;
    }

    public Long getId() { return id; }
    public UserAccount getAccount() { return account; }
    public String getEmployeeNo() { return employeeNo; }
    public String getName() { return name; }
    public String getNickname() { return nickname; }
    public boolean isActive() { return active; }
    public LocalDate getJoinedAt() { return joinedAt; }
    public LocalDateTime getDeactivatedAt() { return deactivatedAt; }
    public MateStatus getCurrentStatus() { return currentStatus; }
    public String getCurrentWhereabouts() { return currentWhereabouts; }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changeStatus(MateStatus status, String whereabouts) {
        this.currentStatus = status;
        this.currentWhereabouts = whereabouts;
    }

    public void deactivate() {
        this.active = false;
        this.deactivatedAt = LocalDateTime.now();
        this.currentStatus = MateStatus.OFF_DUTY;
    }

    public void reactivate() {
        this.active = true;
        this.deactivatedAt = null;
    }
}
