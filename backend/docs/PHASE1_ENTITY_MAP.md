# Phase 1 Entity Map

```text
UserAccount
   |
   +-- Mate
        |-- MateWorkSchedule
        |-- WorkScheduleOverride
        |-- MateStatusHistory
        |
        +-- PdaUsageHistory -- PdaDevice

Location -- parent --> Location

WorkType
   |
   +-- WorkAssignment
          |-- areaLocation
          |-- startLocation
          |-- currentLastCompletedLocation
          |-- currentMate
          |
          |-- WorkProgress
          |-- WorkSession -- PdaUsageHistory
          +-- WorkAssignmentHistory
```

추가 Infrastructure Entity:

```text
EmployeeNumberCounter
```

사원번호는 `AD0001`, `MT0001` 형식이며 Prefix별 Counter를 비관적 잠금으로 증가시킨다.
