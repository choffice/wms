# Phase 3.9 — 기준정보 / 관리자 변경 감사이력

## 목적

Phase 3.8에서 ActivityLog를 append-only 감사로그로 잠근 뒤,
Phase 3.9에서는 관리자 기준정보 변경까지 감사 범위를 넓힌다.

핵심 원칙:

```text
"현재 값"만 남기지 않는다.
누가 / 무엇을 / 어떻게 바꿨는지 ActivityLog에 남긴다.
```

별도의 과도한 Versioning Framework를 도입하지 않고,
현재 프로젝트에서 실제 운영상 의미가 있는 변경만 구조화하여 기록한다.

---

## 1. MATE

추가 ActivityType:

```text
MATE_CREATE
MATE_NICKNAME_CHANGE
MATE_ACTIVE_CHANGE
MATE_SCHEDULE_CHANGE
SCHEDULE_OVERRIDE_CHANGE
EXTENSION_CHANGE
```

기록 예:

```text
MT0007 등록
운영 별명 홍길동 → A구역리더
MATE 비활성 / 재활성
기본 요일별 스케줄 변경
기간별 예외 근무시간 등록
당일 연장 ON / OFF
```

MATE 상태/거소 변경은 기존:

```text
STATUS_CHANGE
```

를 실제 Audit Log에도 연결했다.

상태 이력 Entity와 ActivityLog의 역할은 다르다.

- `MateStatusHistory`: 상태 변화 자체의 도메인 이력
- `ActivityLog`: 누가 그 행위를 발생시켰는지 보는 운영 감사이력

---

## 2. PDA 기준정보

추가 ActivityType:

```text
PDA_CREATE
PDA_NUMBER_CHANGE
PDA_STATUS_CHANGE
PDA_NUMBER_SWAP
PDA_RETIRE
PDA_DELETE
```

중요:

PDA 사용이력:

```text
PDA_ASSIGN
PDA_RETURN
```

과 기기 Master 변경:

```text
PDA_NUMBER_CHANGE
PDA_STATUS_CHANGE
...
```

을 분리한다.

따라서 `누가 PDA 32를 사용했나`와
`관리자가 물리기기의 표시번호를 32 → 41로 바꿨나`를 구분할 수 있다.

사용이력이 존재하는 PDA는 기존 규칙대로 삭제 대신 RETIRED 처리하며,
이때도 감사로그에 이유를 남긴다.

---

## 3. Location

추가 ActivityType:

```text
LOCATION_CREATE
LOCATION_METADATA_CHANGE
LOCATION_DEACTIVATE
```

기록 대상:

- 최상위 Area 생성
- 하위 Location 생성
- 숫자범위 일괄 생성
- 층수 변경
- FOOD / NON_FOOD 변경
- 비식품 Category 변경
- 비활성 처리

범위 생성은 Location 하나마다 Audit Row를 수십 개 만들지 않고:

```text
A01-01 ~ A01-20 / 20건
```

같은 하나의 관리행위로 Summary Log를 남긴다.

---

## 4. 업무 종류

```text
WORK_TYPE_CREATE
WORK_TYPE_UPDATE
WORK_TYPE_DEACTIVATE
```

업무명/설명 변경 시:

```text
before → after
```

형태로 기록한다.

---

## 5. 특이사항 구분

```text
ISSUE_TYPE_CREATE
ISSUE_TYPE_UPDATE
ISSUE_TYPE_DEACTIVATE
```

다음 구성요건 변경도 감사대상이다.

- 로케이션 필수
- 상품코드 필수
- 수량 필수

---

## 6. 정확한 Reference 조회

ActivityLog 조회 API에:

```text
referenceId
```

필터를 추가했다.

예:

```text
referenceType=PDA_DEVICE
referenceId=12
```

이면 PDA PK 12의 기준정보 변경만 정확히 조회할 수 있다.

기존 keyword 검색은 유지하지만,
특정 Entity의 변경이력을 조회할 때는 문자열 검색보다
`referenceType + referenceId`를 사용한다.

---

## 7. 관리자 화면 연결

설정 / MATE 화면에 Audit History 바로가기를 추가했다.

### Settings

- MATE → MATE Audit
- PDA → PDA_DEVICE Audit
- Location → LOCATION Audit
- WorkType → WORK_TYPE Audit
- IssueType → ISSUE_TYPE Audit

### MATE 상세

- 기본정보 이력
- 근무스케줄 변경이력

감사로그에서 다시 원본 관리화면으로 이동하는 Drilldown도 유지한다.

---

## 8. 감사로그의 역할

ActivityLog는 모든 Entity의 전체 Snapshot을 저장하는 범용 Event Sourcing 시스템이 아니다.

이 프로젝트에서는 다음 질문에 답하기 위한 운영 감사이력이다.

```text
누가 바꿨나?
언제 바꿨나?
무엇을 바꿨나?
어떤 원본 데이터에 대한 변경인가?
```

세부 도메인 이력이 이미 별도 Entity로 존재하면 둘을 중복 통합하지 않는다.

예:

```text
WorkProgress
WorkAssignmentHistory
SpecialIssueHistory
MateStatusHistory
PdaUsageHistory
```

는 각각 도메인 이력으로 유지하고,
ActivityLog는 상위 감사 Index 역할을 한다.
