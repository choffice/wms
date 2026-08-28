# Phase 4.5 — 야간조 대응 / shiftDate / 근무조 보고서

## 1. 정책

실제 현재 운영은 주간근무 중심이지만 확장 가능성을 위해
자정을 넘는 근무조를 지원한다.

시간 해석 규칙:

```text
08:00 → 18:00
= 같은 날 종료

22:00 → 06:00
= 다음 날 06:00 종료

08:00 → 08:00
= 잘못된 스케줄
```

야간조의 기준일은:

```text
shiftDate = 근무 시작 날짜
```

이다.

예:

```text
2026-08-27 22:00
→ 2026-08-28 06:00

shiftDate = 2026-08-27
```

---

## 2. 중앙 WorkScheduleResolver

시간 계산을 각 Service에 흩뿌리지 않고
`WorkScheduleResolver`에서 일관되게 해석한다.

신규 값 객체:

```text
ResolvedWorkShift
```

필드:

```text
shiftDate
startsAt
endsAt
overnight
extensionActive
autoEndEnabled
source
```

Resolver는:

- 기본 요일 스케줄
- 기간 Override
- 익일 종료
- Extension
- 새벽 시점의 전날 야간조

를 같은 규칙으로 처리한다.

기간 Override도 `startDate/endDate`를 달력 종료일이 아니라
각 근무의 `shiftDate` 범위로 해석한다.

---

## 3. WorkSession.shiftDate

`WorkSession`에 nullable:

```text
shift_date
```

를 추가했다.

신규 WorkSession 시작 시:

```text
scheduleResolver.resolveShiftDate(
    mateId,
    startedAt
)
```

으로 계산하여 저장한다.

기존 DB Row는 컬럼이 null일 수 있다.

이 경우 시스템은:

```text
startedAt + MATE 스케줄
```

을 사용해 동적으로 shiftDate를 계산한다.

따라서 기존 데모/개발 DB를 즉시 데이터 마이그레이션하지 않아도
보고서와 자동종료가 동작한다.

향후 데이터가 쌓인 경우를 고려해:

```text
idx_work_session_shift_date
```

Index도 추가했다.

---

## 4. 자동 종료

기존 문제:

```text
현재 날짜 = 8/28
야간조 기준일 = 8/27
종료 = 8/28 06:00
```

상태에서 단순 `8/28 스케줄`을 조회하면
전날 야간조의 종료시간을 놓칠 수 있다.

Phase 4.5부터 자동종료는 Open WorkSession의:

```text
session.shiftDate
```

를 우선한다.

기존 Row처럼 shiftDate가 null이면
`session.startedAt`으로 Resolver에서 복구한다.

따라서:

```text
8/27 22:00 시작
8/28 06:15 Scheduler 확인

→ shiftDate 8/27
→ effectiveEnd 8/28 06:00
→ SCHEDULE_END 06:00
```

으로 처리한다.

기존 정책:

```text
SCHEDULE_END
> NETWORK_TIMEOUT
```

우선순위도 그대로 유지한다.

---

## 5. 연장

Extension도 달력 `오늘`이 아니라
현재 작업의 `shiftDate`에 귀속한다.

특히:

```text
8/27 22:00 → 8/28 06:00
```

야간조가 8/28 새벽에 연장 상태를 확인/해제해도
Extension은:

```text
8/27 shift
```

에 연결된다.

연장 ON/OFF 시 Open WorkSession이 있으면
그 Session의 shiftDate를 최우선으로 사용한다.

Extension 상태에서는 기존과 동일하게:

```text
자동 종료 OFF
MATE 수동 근무종료
```

정책을 유지한다.

---

## 6. MATE 현재 근무정보

`TodayShiftResponse`를 확장했다.

```text
date
shiftDate
effectiveScheduledStart
effectiveScheduledEnd
overnight
extensionActive
autoEndEnabled
```

MATE 더보기 화면에서도:

```text
근무 기준일 2026-08-27 · 야간조
```

형태로 확인할 수 있다.

---

## 7. 운영관제

운영관제의 `예정 종료`도
달력 오늘이 아니라 현재 Open Session의 shiftDate 기준으로 계산한다.

예:

```text
08/27 기준 · 자동 종료 예정
08/27 기준 · 수동 종료
```

형태로 표시한다.

Assignment의 WorkSession 이력에서도
각 세션의 `근무 기준일`을 확인할 수 있다.

---

# 근무조 보고서

## 8. 신규 API

```text
GET /api/admin/reports/shift/{shiftDate}
```

기존:

```text
일일 보고서
기간 통계
```

는 계속 **달력 날짜 기준**으로 유지한다.

새 `근무조 보고서`만 shiftDate 기준으로 집계한다.

따라서 두 의미를 섞지 않는다.

---

## 9. 자정을 넘긴 작업시간

달력일 보고서:

```text
23:00 → 02:00

8/27 보고서 = 1시간
8/28 보고서 = 2시간
```

처럼 날짜 경계에서 잘라 계산한다.

반면 근무조 보고서:

```text
shiftDate = 8/27
23:00 → 02:00

8/27 근무조 = 전체 3시간
```

으로 계산한다.

즉 야간조의 실제 근무시간을 자정 때문에 두 개의 근무로 오해하지 않는다.

---

## 10. 근무조 보고서 내용

Summary:

```text
실제 작업시간
Session 수
Open Session 수
UNCERTAIN Session 수
Assignment 수
MATE 수
특이사항 수
PDA 사용이력 수
야간조 Session 수
```

Detail:

```text
Assignment / MATE / PDA
업무 / 구역 / 시작위치
마지막 실제 수행위치
작업시간
신뢰도

특이사항
PDA 사용이력
```

특이사항은 작성자 MATE의 스케줄 기준으로
작성시각이 어느 shiftDate에 속하는지 계산한다.

PDA 사용이력은 해당 Shift WorkSession에 실제 사용된 PDA를 우선 포함한다.

---

## 11. 이전 근무조 대비

현재 shiftDate보다 이전에 실제 WorkSession이 존재한
가장 최근 shiftDate를 최대 14일 범위에서 찾는다.

비교:

```text
이전 근무조 작업시간
작업시간 증감
이전 특이사항 수
특이사항 증감
```

이전 Shift 이력이 없으면 UI에서 증감값을 임의로 계산해 보이지 않고
`비교 이력 없음`으로 표시한다.

이 비교 역시 생산성 평가나 자동 우선순위가 아니다.
단순 운영 데이터 비교다.

---

## 12. UI 입력

MATE 기본/예외 스케줄 입력에서:

```text
end < start
```

이면 자동으로 익일 종료로 이해한다.

별도의 `야간조` Checkbox를 요구하지 않는다.

사용자 안내:

```text
22:00 ~ 06:00 = 다음 날 06:00 종료
```

를 표시한다.

시작시간과 종료시간이 같으면
Frontend에서 먼저 안내하고 Backend도 다시 거부한다.

---

## 13. 기존 데이터 / 데모

현재 실제 프로젝트 데모 데이터는 기존 주간근무 중심으로 유지한다.

즉 야간조 기능을 추가했다고 해서
데모 MATE들이 자동으로 야간근무로 바뀌지 않는다.

기존 WorkSession의 shift_date가 null인 경우에도
Resolver fallback이 있으므로 개발 DB를 초기화할 필요는 없다.

---

## 14. 아직 유지되는 구분

`/reports/daily`
`/reports/range`

는 달력 날짜 분석이다.

`/reports/shift/{shiftDate}`

는 근무조 분석이다.

이 구분을 유지함으로써:

```text
회계/달력 날짜 기준 조회
실제 근무조 기준 조회
```

두 요구를 동시에 처리한다.
