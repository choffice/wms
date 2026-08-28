package com.portfolio.warehouse.auth.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "employee_number_counter")
public class EmployeeNumberCounter {

    @Id
    @Column(length = 2)
    private String prefix;

    @Column(name = "last_value", nullable = false)
    private long lastValue;

    @Version
    private long version;

    protected EmployeeNumberCounter() {
    }

    public EmployeeNumberCounter(String prefix) {
        this.prefix = prefix;
        this.lastValue = 0L;
    }

    public String getPrefix() { return prefix; }
    public long getLastValue() { return lastValue; }

    public long nextValue() {
        return ++lastValue;
    }
}
