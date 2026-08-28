# Phase 3.5 — 운영관제 / 현장 운영 보강

## 1. 관리자 운영관제

신규 관리자 메뉴:

```text
/operations
```

한 화면에서 현재 활성 MATE의 다음 정보를 확인한다.

- 사원번호 / 운영별명
- 현재 상태 / 거소
- 현재 사용 PDA / PDA 상태
- 현재 업무배정
- 시작 로케이션 / 마지막 수행 로케이션
- Open WorkSession
- 현재 세션 경과시간
- 마지막 Heartbeat
- WorkSession Quality
- 오늘 적용 근무종료시각
- 연장 여부
- 객관적 운영 이상

운영관제는 60초 주기 재조회 + SSE 이벤트 시 즉시 재조회한다.

## 2. 운영 확인필요 조건

업무 우선순위를 자동으로 만들지 않는다.

확인필요 표시는 다음처럼 데이터 상태가 명확히 이상한 경우에만 사용한다.

```text
HEARTBEAT_STALE
SESSION_UNCERTAIN
SESSION_STATUS_MISMATCH
WORKING_WITHOUT_SESSION
OFF_DUTY_WITH_PDA
ACTIVE_PDA_MARKED_LOST
```

즉 `이 업무가 오래됐으니 중요` 같은 업무 우선순위 판단과
`세션/상태가 서로 안 맞음` 같은 운영 데이터 이상을 분리한다.

## 3. 관리자 Quick Action

운영관제에서:

- 오늘 연장 활성 / 해제
- 업무배정 취소
- PDA 관리자 회수

가능.

안전 규칙:

- Open WorkSession이 있으면 업무배정 취소 불가
- Open WorkSession이 있으면 PDA 강제 회수 불가
- 먼저 MATE가 일시정지/종료해 실제 작업시간 구간을 닫아야 함

관리자 개입 때문에 작업시간 이력이 끊어지거나 변조되는 것을 막는다.

## 4. 업무배정 취소 이력

기존 `WorkAssignmentHistory`의 `CANCEL`을 실제 API/UI에 연결했다.

취소 시:

- 기존 WorkProgress 삭제 안 함
- WorkSession 삭제 안 함
- Assignment History에 CANCEL 기록
- 처리 Admin
- 취소 사유
- 처리시각 보존
- ActivityLog + SSE 이벤트 생성

## 5. 업무 Assignment 이력 화면

업무배정의 상세 이력은 세 영역으로 구분한다.

### 진행기록
실제 마지막 수행 Location과 정정 이력.

### 작업세션
실제 작업시간 구간 / 종료사유 / Quality.

### 배정 변경이력
ASSIGN / TRADE / CANCEL / COMPLETE 등의 Lifecycle History.

따라서 `누가 작업했는가`와 `누구에게 배정되어 있었는가`가 섞이지 않는다.

## 6. MATE 오늘 근무시간

MATE 더보기 화면에서:

- 오늘 적용 자동 종료시각
- 연장 ON/OFF

를 조회한다.

MATE는 같은 자리에서 연장 활성/해제가 가능하다.

연장 ON:

```text
자동종료 비활성
→ MATE 수동 근무종료
```

연장 해제:

```text
기간별 예외시간 또는 기본스케줄의 종료시각 재적용
```

## 7. WorkAssignment actual start 정리

`WorkAssignment.startedAt`를 제거했다.

실제 작업 시작의 Source of Truth는:

```text
WorkSession.startedAt
```

이다.

Assignment는 배정 Lifecycle,
WorkSession은 실제 수행시간이라는 기존 설계 원칙을 코드에도 일치시켰다.

기존 개발 DB에 `work_assignment.started_at` 컬럼이 있었다면
Hibernate `ddl-auto:update`는 물리 컬럼을 자동 삭제하지 않을 수 있으나
애플리케이션 Domain에서는 더 이상 사용하지 않는다.


## 8. 운영관제 → 업무배정 바로가기

현재 업무가 없는 MATE는 운영관제 행에서 `업무배정`으로 바로 이동할 수 있다.

```text
/assignments?mateId={MATE_ID}
```

업무배정 화면에서 해당 MATE가 미리 선택된다.

자동 추천/자동 배정은 하지 않으며
관리자가 앞서 구현된 Area + WorkType 예상시간을 보고 최종 배정한다.
