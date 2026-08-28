# Phase 3.8 — 감사로그 / 관리자 행위 추적

## 1. 감사로그 전용 화면

신규 관리자 메뉴:

```text
/audit-logs
```

ERP형 조회조건 + Data Grid 구조로 구현한다.

조회조건:

- 시작일 / 종료일
- ActivityType
- 처리자 Account
- ReferenceType
- 키워드
- Page Size

기본 Page Size:

```text
50
```

최대:

```text
100
```

## 2. 검색 대상

키워드는 다음 Structured Field를 대상으로 한다.

```text
message
targetLabel
referenceType
actor.loginId
```

ActivityLog는 문자열 메시지만 저장하는 것이 아니라:

```text
type
actorAccount
targetLabel
referenceType
referenceId
createdAt
```

을 유지한다.

따라서 UI 표현이 달라져도 감사조회 기준은 유지된다.

## 3. Reference Drilldown

감사로그에서 가능한 경우 원본 운영화면으로 이동한다.

```text
WORK_ASSIGNMENT → 업무배정
SPECIAL_ISSUE   → 특이사항
PDA_USAGE       → 설정/PDA
NOTICE          → 공지사항
MATE_STATUS     → MATE
```

업무배정과 특이사항은 referenceId를 Query Parameter로 넘겨
해당 행/상세를 바로 찾을 수 있도록 했다.

## 4. ActivityLog 삭제 금지

기존 Dashboard의:

```text
현재 로그 지우기
```

는 Backend Row 삭제로 이어질 수 있었다.

이는 원래 설계:

> Dashboard에는 최근 일부만 표시하고
> DB ActivityLog는 전체 이력을 보존한다.

와 충돌한다.

Phase 3.8에서 ActivityLog DELETE API를 제거했다.

Dashboard에서는:

```text
현재 표시 숨기기
```

만 제공한다.

이는 브라우저 현재 화면 State에서만 숨기며
DB ActivityLog Row는 삭제하지 않는다.

페이지를 완전히 새로 열면 다시 조회 가능하다.

## 5. 로그인/로그아웃 Audit

추가 ActivityType:

```text
ADMIN_LOGIN
MATE_LOGIN
AUTH_LOGOUT
```

관리자 로그인, MATE 로그인, 로그아웃도 ActivityLog에 남긴다.

MATE 로그인은 기존 PDA_ASSIGN과 별개이다.

```text
MATE_LOGIN = 인증 성공
PDA_ASSIGN = 실제 PDA 사용관계 생성
```

으로 의미를 분리한다.

## 6. 기존 운영 이벤트

기존 주요 업무 이력도 같은 Audit 화면에서 검색한다.

예:

```text
WORK_ASSIGN
WORK_TRADE
WORK_CANCEL
WORK_PROGRESS
WORK_COMPLETE

PDA_ASSIGN
PDA_RETURN

ISSUE_CREATE
ISSUE_ASSIGN
ISSUE_CONFIRM
ISSUE_RESOLVE

SESSION_TIMEOUT
SHIFT_AUTO_END

NOTICE_CHANGE
STATUS_CHANGE
```

## 7. QueryDSL

ActivityLog 검색은 QueryDSL로 구현한다.

- Date Range
- Type
- Actor
- ReferenceType
- Keyword
- Pagination

Search Query와 Count Query가 같은 LEFT JOIN 조건을 사용한다.

actorAccount가 null인 SYSTEM Log도
키워드 검색 결과에서 누락되지 않도록 한다.

## 8. Index

ActivityLog Entity에 다음 Index를 선언했다.

```text
created_at
type + created_at
actor_account_id + created_at
reference_type + reference_id
```

감사로그는 데이터가 계속 증가하는 Table이므로
최근 이력/유형/처리자/원본참조 조회를 고려한다.

## 9. 보존 원칙

이번 프로젝트 단계에서는 감사로그 물리 삭제 기능을 제공하지 않는다.

향후 실제 운영환경에서 장기보존/Archive 정책이 필요하다면
별도 정책으로 설계한다.

현재 Portfolio/Demo 범위에서는:

```text
ActivityLog = append-only operational audit trail
```

로 취급한다.
