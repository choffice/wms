package com.portfolio.warehouse.work.domain;

public enum WorkSessionEndReason {
    PAUSED,
    COMPLETED,
    SCHEDULE_END,
    MANUAL_SHIFT_END,
    LOGOUT,
    TASK_SWITCH,
    NETWORK_TIMEOUT
}
