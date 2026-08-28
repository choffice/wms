# Phase 4.0 — 관리자 운영 편의 / 현장 데이터 정합성 복구

## 목적

운영화면에서 개별 Entity를 하나씩 열어 상태를 대조하지 않아도
MATE / PDA / WorkAssignment / WorkSession의 현재 데이터가 서로 일치하는지
관리자가 한 화면에서 검사할 수 있도록 한다.

신규 관리자 화면:

```text
/integrity
```

신규 API:

```text
GET  /api/admin/integrity
POST /api/admin/integrity/repair
POST /api/admin/integrity/repair-safe
```

---

## 1. 검사 대상

### PDA

- `PDA.status = IN_USE`인데 활성 PdaUsageHistory 없음
- 활성 PdaUsageHistory가 있는데 `PDA.status = AVAILABLE`
- 활성 사용 중인 PDA가 LOST
- 비활성/INSPECTION/RETIRED PDA에 활성 사용이력 존재
- 하나의 PDA에 활성 사용이력 2건 이상

### MATE

- `MATE.status = WORKING`인데 Open WorkSession 없음
- Open WorkSession이 있는데 MATE 상태가 WORKING이 아님
- 비활성 MATE에 Open WorkSession 존재
- 퇴근 상태인데 PDA 사용이력 미반납
- 한 MATE에 활성 PDA 사용이력 2건 이상
- 한 MATE에 Open WorkSession 2건 이상

### WorkAssignment / WorkSession

- Open Session이 있는데 Assignment가 아직 ASSIGNED
- COMPLETED/CANCELED Assignment에 Open Session 존재
- Session MATE와 Assignment 현재 담당자 불일치
- Open Session이 이미 반납된 PDA Usage를 참조
- Session MATE와 PDA Usage MATE 불일치
- 하나의 Assignment에 Open Session 2건 이상

---

## 2. 자동복구와 수동확인 구분

정합성 화면은 검출된 데이터를 무조건 수정하지 않는다.

자동복구는 **현재 Source of Truth가 하나로 명확한 경우만** 허용한다.

### 안전복구 가능

```text
PDA IN_USE + 활성 Usage 없음
→ PDA AVAILABLE

PDA AVAILABLE + 활성 Usage 정확히 1건
→ PDA IN_USE

MATE WORKING + Open Session 없음
→ MATE AVAILABLE / 대기

정상 구조의 Open Session 1건 + MATE 상태 불일치
→ MATE WORKING / 업무중

OFF_DUTY + Open Session 없음 + 정상 활성 PDA Usage 1건
→ PDA ADMIN_RELEASE

정상 구조의 Open Session 1건 + Assignment ASSIGNED
→ Assignment IN_PROGRESS
```

### 자동복구 금지

다음은 어떤 기록을 살릴지 운영 판단이 필요하므로
시스템이 임의 수정하지 않는다.

```text
중복 활성 PDA Usage
중복 Open WorkSession
종료 Assignment의 Open Session
Assignment 담당자와 Session MATE 불일치
반납된 PDA Usage를 참조하는 Open Session
PDA Usage MATE와 Session MATE 불일치
LOST / INSPECTION / RETIRED 상태 충돌
비활성 MATE의 Open Session
```

---

## 3. 안전복구 일괄 실행

관리자 화면에서:

```text
안전복구 일괄 실행
```

을 제공한다.

실행 전 사용자 Confirm을 거치며,
현재 검사결과 중 `safeRepairAction`이 있는 항목만 처리한다.

복구 실행 시점에 다시 조건을 검사한다.

따라서 검사 이후 다른 요청으로 상태가 바뀌었다면
과거 검사결과만 믿고 강제로 덮어쓰지 않는다.

중복 데이터가 새로 생긴 경우에도
단일 Usage / 단일 Session 조건이 깨지므로 안전복구를 중단한다.

---

## 4. 복구 이력

모든 안전복구는:

```text
ActivityType.INTEGRITY_REPAIR
```

로 append-only ActivityLog에 남긴다.

예:

```text
PDA 32 · IN_USE → AVAILABLE / 활성 사용이력 없음
A구역 MATE · WORKING → AVAILABLE / Open Session 없음
Assignment #17 · ASSIGNED → IN_PROGRESS / Open Session 존재
```

복구 때문에 기존 WorkSession, WorkProgress, AssignmentHistory,
PdaUsageHistory 등의 도메인 이력을 삭제하지 않는다.

---

## 5. 관리자 PDA 회수 Actor 수정

기존 `PdaSessionService.release()`는 MATE 반납 흐름을 기준으로
PDA_RETURN ActivityLog actor를 해당 MATE로 기록한다.

하지만 관리자 운영관제에서 강제회수하는 경우에도 같은 메서드를 사용하면:

```text
실제 처리자 = ADMIN
Audit actor = MATE
```

가 되는 문제가 있었다.

Phase 4.0에서:

```text
releaseByAdmin()
```

을 분리했다.

관리자 회수 / 정합성 복구 회수 시에는
현재 로그인한 ADMIN Account가 Audit actor가 된다.

MATE가 직접 반납하거나 로그아웃할 때는 기존 MATE actor를 유지한다.

---

## 6. UI

관리자 Navigation:

```text
현황판
운영관제
정합성
업무배정
...
```

운영관제에서도 `정합성 검사` 바로가기를 제공한다.

정합성 화면 구성:

```text
Summary
- 검출 전체
- 치명 오류
- 경고
- 안전복구 가능
- 검사 기준시각

Filter
- 심각도
- 안전복구 가능만
- 키워드

Data Grid
- 심각도
- 오류코드
- 대상
- PK
- 상세
- 복구정책
- 원본 화면
- 처리
```

ERP 스타일을 유지하며 Chart나 카드 중심 Dashboard로 만들지 않는다.

---

## 7. 복구 철학

이번 기능은 DB를 과거 상태로 Rollback하는 백업/복원 기능이 아니다.

목적은:

```text
여러 Entity에 중복 저장된 "현재 상태"가
운영 중 예외로 서로 어긋났을 때
안전하게 다시 맞추는 것
```

이다.

역사 데이터는 지우지 않는다.

불명확한 충돌은 관리자에게 보여주고,
명확한 상태 플래그 불일치만 시스템이 복구한다.
