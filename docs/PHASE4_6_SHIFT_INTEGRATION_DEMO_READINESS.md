# Phase 4.6 — 근무조 보고서 통합 / 교대메모 기준일 / Demo Scenario

## 1. 근무조 보고서 Drilldown

Phase 4.5에서 shiftDate 보고서를 만들었고,
Phase 4.6에서는 운영 화면과 직접 연결한다.

신규 조회:

```text
GET /api/admin/reports/shift-dates?limit=7
```

최근 31일 WorkSession에서 실제로 존재하는 shiftDate를:

- 중복 제거
- 최신순
- 최대 14개 제한

으로 반환한다.

기본 UI는 최근 7개를 사용한다.

---

## 2. 교대 마감 → 근무조 보고서

`/shift-close`에 최근 실제 근무조 링크를 표시한다.

```text
2026-08-27
2026-08-26
...
```

선택하면:

```text
/reports?mode=SHIFT&shiftDate=2026-08-27
```

로 바로 이동한다.

`ReportsPage`는 query string을 읽어
근무조 보고서 탭과 shiftDate를 자동 선택한다.

따라서 교대 마감 점검 후
보고서 화면에서 날짜를 다시 찾을 필요가 없다.

---

## 3. 교대 인계요약 → 근무조 보고서

`/handover-overview`에도 최근 shiftDate 링크를 제공한다.

운영 흐름:

```text
마감점검
→ 인계요약
→ 특정 shiftDate 보고서
```

또는:

```text
후속조치
→ 인수인계
→ 인계요약
→ 근무조 보고서
```

처럼 바로 이어질 수 있다.

강제 Wizard는 아니며 각 화면은 독립적으로 사용할 수 있다.

---

## 4. HandoverNote.shiftDate

교대 인계메모에 nullable:

```text
shift_date
```

를 추가했다.

메모 작성 시:

```text
공통 인계메모
또는
특정 shiftDate 연결
```

중 하나를 관리자가 직접 선택한다.

시스템이 근무조를 임의로 추정하지 않는다.

예:

```text
shiftDate = 2026-08-27
내용 = A01 재고조사 다음 교대 재확인
```

공통 공지성 메모라면 shiftDate는 null이다.

기존 HandoverNote Row도 null 상태로 그대로 사용할 수 있다.

---

## 5. 인계요약 복사

Clipboard 인계문에 shiftDate가 연결된 메모는:

```text
AD0001 18:04 [shift 2026-08-27]: ...
```

형태로 표시한다.

공통 메모는 shift 표시가 없다.

---

# Demo Scenario

## 6. 목적

포트폴리오 시연 시 모든 설정을 수동으로 입력하지 않아도
ERP 운영화면의 데이터 흐름을 확인할 수 있도록
선택형 Demo Bootstrap을 추가했다.

기본값:

```text
DEMO_SCENARIO_ENABLED=false
```

따라서 일반 실행에서는 아무 데이터도 추가하지 않는다.

활성화:

```text
DEMO_SCENARIO_ENABLED=true
DEMO_MATE_PASSWORD=mate1234
```

---

## 7. 안전 조건

Demo Scenario는:

```text
MATE 테이블이 비어 있을 때만
```

실행한다.

기존 MATE 데이터가 하나라도 있으면 전체 Demo Seed를 건너뛴다.

따라서 사용자가 이미 만든 현장 데이터에
샘플 인력을 섞지 않는다.

모든 Seed는 하나의 Transaction에서 생성한다.

---

## 8. 생성되는 Demo 데이터

### MATE

3명:

```text
A구역
B구역
지원
```

사원번호는 기존 EmployeeNumberService를 사용하므로
직접 MT 번호를 하드코딩하지 않는다.

기본 상태:

```text
AVAILABLE / 대기
```

### PDA

```text
31
32
33
```

기존 동일 번호가 있으면 재사용한다.

### Location

```text
A01
A01-01
A01-02
A01-03

B01
B01-01
B01-02
```

### WorkType

```text
재고조사
진열보충
```

### IssueType

```text
재배치 확인
재고 불일치
```

### 기본 스케줄

월~금:

```text
08:00 → 18:00
```

실제 Demo는 현재 현장과 동일하게 주간근무 중심을 유지한다.

야간조 기능은 관리자가 별도로
`22:00 → 06:00`을 등록해 시연할 수 있다.

---

## 9. Demo 업무흐름

Seed에는 다음 운영상태가 포함된다.

### Assignment 1

```text
재고조사
A01
IN_PROGRESS
마지막 수행 A01-02
Open WorkSession 없음
```

따라서 인수인계/재개 화면에서
`재개 대기` 흐름을 확인할 수 있다.

WorkProgress도 실제 append-only Row를 생성한다.

### Assignment 2

```text
진열보충
B01
ASSIGNED
```

미시작 배정 흐름을 확인할 수 있다.

### SpecialIssue 1

```text
재배치 확인
UNCONFIRMED
미담당
```

후속조치 큐와 특이사항 선택처리를 확인할 수 있다.

### SpecialIssue 2

```text
재고 불일치
CONFIRMED
지원 MATE 담당
```

확인 → 해결 Lifecycle을 시연할 수 있다.

AssignmentHistory / WorkProgress / SpecialIssueHistory도
기존 운영 이력 Entity를 사용해 함께 생성한다.

---

## 10. Demo에서 하지 않는 것

Bootstrap이 과거 가짜 시간을 임의로 조작하지 않는다.

즉:

- 과거 WorkSession timestamp 위조
- 과거 PDA 사용시간 위조
- 보고서 그래프용 대량 가짜 이력

은 만들지 않는다.

근무조 보고서는 실제로 MATE가 작업을 수행하며
생기는 WorkSession을 기준으로 확인한다.

이렇게 해야 시연 데이터가
운영시간 계산 규칙과 충돌하지 않는다.
