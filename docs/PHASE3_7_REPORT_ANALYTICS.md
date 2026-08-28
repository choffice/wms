# Phase 3.7 — 일일 보고서 / 기간 통계 정합성 보강

## 목적

보고서가 단순 화면요약이 아니라
실제 운영기록을 기간별로 비교할 수 있는 조회도구가 되도록 보강한다.

ERP/WMS UI 원칙에 따라 Chart 중심이 아니라
조건 + Summary + Data Grid 구조를 사용한다.

---

## 1. WorkSession 일자 경계 보정

기존 통계는 조회기간에 걸친 WorkSession이 있으면
Session 전체 Duration을 더할 수 있었다.

예:

```text
08/26 23:30 시작
08/27 01:30 종료
```

08/27 일일 통계에는:

```text
2시간
```

이 아니라 실제 08/27에 포함되는:

```text
1시간 30분
```

만 집계한다.

공식:

```text
clipped start = max(session.startedAt, query.from)
clipped end   = min(session.endedAt, query.to)

actual seconds = clipped end - clipped start
```

WorkSession overlap Query도:

```text
endedAt > from
startedAt < to
```

로 수정하여 경계에 정확히 닿기만 하고 실제 겹치는 시간이 0인 Session을 제외한다.

---

## 2. 과거 일일 보고서의 마지막 수행위치

과거 날짜의 보고서에서:

```text
WorkAssignment.currentLastCompletedLocation
```

을 그대로 표시하면,
그 날짜 이후에 작업이 더 진행되었을 때 과거 보고서가 바뀌는 문제가 있다.

Phase 3.7에서는 조회 날짜 종료시각 이전에 보고된
`WorkProgress` 중 마지막 기록을 조회한다.

또 Assignment가 Trade되었던 경우를 고려하여:

```text
assignmentId + mateId + reportedAt < dayEnd
```

조건으로 그 MATE가 그 날짜까지 남긴 마지막 수행위치를 표시한다.

따라서 과거 보고서는 이후 진행상황 때문에 위치값이 변하지 않는다.

---

## 3. 기간 통계

신규 API:

```text
GET /api/admin/reports/range?from=YYYY-MM-DD&to=YYYY-MM-DD
```

최대 조회기간:

```text
366일
```

### Summary

- 종료된 WorkSession 수
- Assignment 수
- 실제 작업 MATE 수
- NORMAL 실제 작업시간
- UNCERTAIN 실제 작업시간
- 특이사항 등록 수
- PDA 사용이력 수

### MATE별

- 사원번호
- 별명
- Assignment 수
- Session 수
- NORMAL 실제 작업시간
- UNCERTAIN 실제 작업시간

### 구역 + 업무별

- Area
- WorkType
- Assignment 수
- Session 수
- NORMAL 실제 작업시간
- UNCERTAIN 실제 작업시간

### 일자별 추이

각 날짜별:

- NORMAL 실제 작업시간
- UNCERTAIN 실제 작업시간
- 특이사항 등록 수

를 Table로 표시한다.

---

## 4. NORMAL과 UNCERTAIN 분리

기간 통계에서는 UNCERTAIN 데이터를 숨기지 않는다.

대신:

```text
NORMAL
UNCERTAIN
```

시간을 별도 컬럼/요약값으로 분리한다.

이를 통해 관리자는:

- 작업시간 자체
- 네트워크/품질 문제 때문에 신뢰도가 낮은 시간

을 섞지 않고 볼 수 있다.

앞서 구현한 예상 잔여시간 학습에는 계속 NORMAL 표본만 사용한다.

---

## 5. 일일 보고서와 실시간 운영관제 역할 분리

### 운영관제

현재 Open WorkSession과 실시간 상태 확인.

### 일일 / 기간 보고서

종료된 WorkSession 기반 확정시간 집계.

즉 진행 중 Session의 현재 경과시간은 운영관제에서 보고,
통계에는 Session이 닫힌 뒤 반영한다.

이렇게 해야 통계값이 새로고침할 때마다 계속 변하는 문제를 줄일 수 있다.

---

## 6. UI

`보고서 / 통계` 페이지는 두 Tab으로 분리한다.

```text
[일일 보고서] [기간 통계]
```

### 일일 보고서

- 일자
- MATE
- 업무
- UNCERTAIN 포함 여부
- 업무별 실제 작업시간
- 업무 수행기록
- PDA 사용
- 특이사항

### 기간 통계

- 시작일 / 종료일
- Summary
- 일자별 작업 추이
- MATE별 실제 작업시간
- 구역 / 업무별 실제 작업시간
- MATE 검색
- 구역 / 업무 필터

카드형 분석 Dashboard가 아니라
ERP형 Summary Row + Table 구조를 유지한다.
