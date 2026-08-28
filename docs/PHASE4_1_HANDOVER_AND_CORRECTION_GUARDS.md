# Phase 4.1 — 미처리 업무 / 인수인계 / 정정 안전장치

## 1. 관리자 미처리 업무 화면

신규 관리자 메뉴:

```text
/handover
```

신규 API:

```text
GET /api/admin/handover
```

대상은:

```text
Assignment.status = ASSIGNED | IN_PROGRESS
AND
해당 Assignment에 Open WorkSession 없음
```

인 활성 업무다.

즉 작업 중인 건은 운영관제에서 보고,
현재 세션이 닫혀 있어 `다음 동작`이 필요한 업무만 별도 화면으로 모은다.

---

## 2. 운영상태 분류

업무 중요도나 자동 우선순위를 계산하지 않는다.

분류는 현재 운영상태만 나타낸다.

```text
NETWORK_RECOVERY
OFF_DUTY_HANDOVER
SHIFT_CARRYOVER
ASSIGNED_NOT_STARTED
PAUSED
READY_TO_RESUME
```

### NETWORK_RECOVERY

- 마지막 Session = NETWORK_TIMEOUT
- 또는 MATE whereabouts = `통신 확인 필요`

### OFF_DUTY_HANDOVER

현재 담당 MATE가 OFF_DUTY.

### SHIFT_CARRYOVER

마지막 Session 종료사유:

```text
SCHEDULE_END
MANUAL_SHIFT_END
LOGOUT
```

### ASSIGNED_NOT_STARTED

배정됐지만 아직 WorkSession이 한 번도 없음.

### PAUSED

마지막 Session 종료사유:

```text
PAUSED
TASK_SWITCH
```

### READY_TO_RESUME

그 외 `IN_PROGRESS + Open Session 없음`.

---

## 3. 빠른 인수인계

인수인계 Grid에서 새 담당 MATE를 선택하여
기존 WorkAssignment의 담당자만 변경한다.

실제로는 기존:

```text
WorkAssignmentService.trade()
```

를 재사용한다.

따라서:

- Assignment ID 유지
- WorkProgress 유지
- 마지막 수행위치 유지
- 과거 WorkSession 유지
- WorkAssignmentHistory에 TRADE 추가
- ActivityLog에 WORK_TRADE 추가

된다.

새 Assignment를 복제하지 않는다.

기본 변경사유는:

```text
인수인계 · {운영상태}
```

이며 관리자가 직접 사유를 입력하면 그 값을 사용한다.

---

## 4. 관리자 재개 버튼을 만들지 않는 이유

관리자 화면에서 WorkSession을 강제로 시작하지 않는다.

기존 핵심 규칙:

```text
실제 작업시간은
MATE가 [시작] / [재개]를 누른 시점부터 측정
```

을 유지한다.

따라서 관리자 역할은:

```text
담당 결정
→ 인수인계
```

까지다.

실제 재개:

```text
새/기존 담당 MATE
→ 모바일 업무 탭
→ [시작] 또는 [재개]
→ 새 WorkSession 생성
```

순서다.

관리자가 임의로 작업시간을 시작시켜 통계가 부풀려지는 것을 막는다.

---

## 5. MATE 재개 안내

IN_PROGRESS 업무의 Open Session이 없으면 모바일 업무 카드에:

```text
이전 수행기록은 유지됩니다.
A01-08 이후 업무를 이어서 진행하세요.
```

처럼 마지막 수행위치를 명시한다.

담당자가 변경되어도 WorkProgress는 Assignment에 유지되므로
새 MATE가 이전 작업의 끝 지점을 확인하고 이어서 수행할 수 있다.

---

## 6. 관리자 진행위치 정정

신규 API:

```text
POST /api/admin/work-assignments/{id}/progress-correction
```

관리자는 Assignment 이력 화면에서 마지막 수행위치를 정정할 수 있다.

정정 시:

- 기존 WorkProgress 삭제 안 함
- correction=true WorkProgress 추가
- Assignment.currentLastCompletedLocation 갱신
- ActivityLog = WORK_PROGRESS_CORRECTION
- 관리자 Account를 실제 기록자로 보존

한다.

CANCELED Assignment는 정정하지 않는다.

COMPLETED Assignment는 기존 설계대로 정정 가능하다.

---

## 7. Open Session 중 정정 금지

관리자 정정은 해당 Assignment에 Open WorkSession이 있으면 거부한다.

```text
CORRECTION_ACTIVE_SESSION
```

현장 MATE가 같은 시점에 진행위치를 업데이트하는 동안
관리자 정정이 끼어드는 것을 방지한다.

먼저 실제 작업을 일시정지/종료한 뒤 정정해야 한다.

---

## 8. Optimistic 진행값 검증

정정 화면이 열린 뒤 다른 요청이 먼저 진행위치를 변경할 수 있다.

그래서 저장 요청에:

```text
expectedCurrentLocationId
```

를 함께 보낸다.

예:

```text
화면을 열 때 현재 = A01-08 / id 88
관리자가 A01-07로 정정하려고 함

그 사이 현장에서 A01-09로 갱신
현재 PK != expected PK

→ PROGRESS_STALE
→ 정정 중단
→ 최신 이력 재조회
```

오래된 화면이 최신 진행값을 덮어쓰는 것을 막는다.

같은 보호를 MATE의:

- 진행기록 저장
- 작업 종료 시 마지막 위치 갱신

에도 적용했다.

---

## 9. WorkProgress 기록자 분리

기존 WorkProgress는:

```text
mate_id
```

만 있어서 관리자 정정도 MATE가 기록한 것처럼 보일 여지가 있었다.

Phase 4.1에서 nullable:

```text
reported_by_account_id
```

를 추가했다.

의미:

```text
mate_id
= 해당 진행기록이 귀속되는 실제 수행 MATE

reported_by_account_id
= 그 Row를 실제 저장한 Account
```

MATE 진행보고:

```text
mate = MT0003
reportedBy = MT0003
```

관리자 정정:

```text
mate = 현재 Assignment 담당 MATE
reportedBy = AD0001
```

이 된다.

기존 DB Row는 신규 컬럼이 null일 수 있으며
UI에서는 그 경우 기존 MATE 이름을 fallback으로 표시한다.

---

## 10. 관리자 UI

Navigation:

```text
운영관제
인수인계
정합성
업무배정
...
```

인수인계 화면 Summary:

- 미처리 활성업무
- 인수인계 검토
- 미시작
- 일시정지/재개
- 통신 복귀확인
- 퇴근/근무종료 이월
- 담당 MATE 다른 업무중

Grid:

- Assignment
- 운영상태
- 현재 MATE
- PDA
- 업무 / 구역
- 이어갈 위치
- 마지막 Session
- 새 담당 / 사유 / 인수인계
- 원본 업무/MATE 링크

정합성 오류와 업무 중요도는 섞지 않는다.
