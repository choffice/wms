# Phase 4.2 — 관리자 선택처리 / 교대 인수인계 / 오입력 복구 UX

## 1. 목표

Phase 4.1에서 관리자에게:

- 미처리 업무
- 인수인계 후보
- 개별 담당 변경
- 진행위치 정정

을 제공했다.

Phase 4.2에서는 반복 클릭을 줄이고,
여러 관리자가 동시에 작업해도 오래된 화면 값으로 덮어쓰지 않도록
선택처리와 복구 안전장치를 강화한다.

---

## 2. 일괄 인수인계

신규 API:

```text
POST /api/admin/handover/bulk-transfer
```

최대:

```text
50 Assignment
```

한 요청에 각 행별로:

```text
assignmentId
expectedCurrentMateId
toMateId
reason
```

을 보낸다.

### expectedCurrentMateId

관리자가 화면을 열었을 때 보던 현재 담당자를 함께 전송한다.

예:

```text
화면:
Assignment #17
현재 담당 MT0003

저장 직전:
다른 관리자가 MT0004로 변경

expectedCurrentMateId = MT0003
actualCurrentMateId   = MT0004

→ HANDOVER_STALE_MATE
→ Batch 전체 중단
```

오래된 관리자 화면이 최신 담당자를 다시 덮어쓰지 않는다.

---

## 3. Atomic Batch

일괄 인수인계는 **부분 성공을 허용하지 않는다.**

처리 순서:

1. Assignment ID 중복 확인
2. Assignment를 ID 오름차순으로 `PESSIMISTIC_WRITE` Lock
3. 관련 MATE Lock
4. 모든 행 사전검증
5. 검증 전부 성공 후 담당변경 수행
6. History / ActivityLog 기록

한 건이라도:

- 이미 종료됨
- 현재 담당자가 바뀜
- Open WorkSession이 생김
- 새 담당 MATE가 비활성

이면 전체 Transaction을 Rollback한다.

교대 시 수십 건 중 일부만 바뀌어
관리자가 다시 맞춰야 하는 상황을 피한다.

---

## 4. WorkAssignment History

일괄 인수인계는:

```text
WorkAssignmentActionType.REASSIGN
```

으로 남긴다.

기존 개별 일반 담당변경:

```text
TRADE
```

와 구분한다.

ActivityLog:

```text
WORK_HANDOVER
WORK_BULK_HANDOVER
```

- 각 Assignment 변경 → `WORK_HANDOVER`
- 실제 2건 이상 묶음 처리 → 추가 Summary `WORK_BULK_HANDOVER`

1건만 처리할 때는 Bulk Summary를 만들지 않는다.

---

## 5. 관리자 UI 선택처리

`/handover` Grid에 Checkbox를 추가했다.

지원:

- 현재 조회 행 전체선택
- 인수인계 검토건 선택
- 선택 해제
- 최대 50건 제한
- 선택 건 공통 담당 적용
- 행별 담당 개별 지정
- 공통 사유
- 행별 사유 Override
- 일괄 실행

사유 우선순위:

```text
행별 사유
→ 공통 사유
→ 시스템 기본:
  "교대 인수인계 · {운영상태}"
```

새 담당이 WORKING이면 관리자가 확인창에서 한 번 더 확인한다.

담당 변경은 가능하지만 실제 WorkSession은
해당 MATE가 기존 Session을 끝낸 뒤 `[시작]/[재개]`해야 한다.

---

## 6. 개별 인수인계도 stale 방지

Handover 화면의 단일 `인수인계` 버튼도
동일한 Guard를 사용하도록 Batch API를 1건으로 호출한다.

또 일반 업무배정 화면의 기존 Trade API에도 optional:

```text
expectedCurrentMateId
```

를 추가했다.

따라서:

- Handover 화면
- Assignment 관리화면

모두 오래된 담당자 정보로 덮어쓰는 위험을 줄인다.

---

## 7. 최근 진행위치 정정 되돌리기

신규 API:

```text
POST /api/admin/work-assignments/{id}/progress-correction/undo-latest
```

자동 되돌리기는 다음 경우에만 가능하다.

```text
가장 최근 WorkProgress가 correction=true
AND
previousLocation 존재
AND
Assignment.currentLastCompletedLocation
    = latest.lastCompletedLocation
AND
expectedLatestProgressId 일치
AND
expectedCurrentLocationId 일치
AND
Open WorkSession 없음
```

한 조건이라도 달라졌으면:

```text
PROGRESS_STALE
```

또는 관련 오류로 중단한다.

---

## 8. 되돌리기는 DELETE가 아니다

예:

```text
원본:
A01-08

관리자 정정:
A01-08 → A01-07

되돌리기:
A01-07 → A01-08
```

DB에서는 기존 정정 Row를 삭제하지 않는다.

새로운:

```text
correction=true
```

WorkProgress를 추가한다.

즉:

```text
A01-08
→ A01-07 (정정)
→ A01-08 (정정 되돌리기)
```

전체 과정이 남는다.

감사로그도:

```text
WORK_PROGRESS_CORRECTION
```

으로 보존한다.

---

## 9. 진행이력 UX

관리자 업무 이력 Grid에서:

- 수행 MATE
- 실제 기록 Account
- `이전 위치 → 새 위치`
- 정정 여부
- 사유

를 같이 보여준다.

가장 최근 Row가 정정이고 되돌릴 수 있는 구조라면:

```text
[최근 정정 되돌리기]
```

버튼을 노출한다.

Open WorkSession이 있으면 버튼은 비활성이다.

---

## 10. 운영관제 후속조치

운영관제의 각 MATE Row에서
다른 화면으로 찾아다니는 단계를 줄였다.

추가/보강:

```text
MATE 상세
Assignment 정확한 행
인수인계
정합성
```

### 인수인계

현재 Assignment가 있고 Open Session이 없으면:

```text
/handover?assignmentId={id}
```

로 바로 이동한다.

### 정합성

다음 Attention이 있으면 정합성 화면으로 바로 이동한다.

```text
SESSION_STATUS_MISMATCH
WORKING_WITHOUT_SESSION
OFF_DUTY_WITH_PDA
ACTIVE_PDA_MARKED_LOST
```

기존 PDA 강제회수 / 연장 / 배정취소 버튼도 유지한다.

---

## 11. 운영 원칙 유지

이번 단계에서도 다음은 하지 않는다.

- 자동 업무 우선순위
- 관리자 강제 WorkSession 시작
- 진행이력 DELETE
- 중복/충돌 데이터를 임의로 선택해 자동 복구
- Batch 부분 성공

관리자는 `누구에게 넘길지` 판단하고,
시스템은 `오래된 화면/동시수정/부분처리`를 막는 역할을 한다.
