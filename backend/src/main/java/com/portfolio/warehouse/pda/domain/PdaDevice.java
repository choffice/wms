package com.portfolio.warehouse.pda.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "pda_device",
    uniqueConstraints = @UniqueConstraint(name = "uk_pda_device_number", columnNames = "device_number")
)
public class PdaDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_number", nullable = false)
    private Integer deviceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PdaStatus status = PdaStatus.AVAILABLE;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PdaDevice() {
    }

    public PdaDevice(Integer deviceNumber) {
        this.deviceNumber = deviceNumber;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Integer getDeviceNumber() { return deviceNumber; }
    public PdaStatus getStatus() { return status; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void changeDeviceNumber(Integer deviceNumber) {
        this.deviceNumber = deviceNumber;
    }

    public void changeStatus(PdaStatus status) {
        this.status = status;
    }

    public void retire() {
        this.active = false;
        this.status = PdaStatus.RETIRED;
    }
}
