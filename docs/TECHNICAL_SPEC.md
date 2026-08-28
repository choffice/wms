# Warehouse Control System — Technical Specification

## 1. 목적

물류센터 현장에서 다음 질문에 답하는 관리 시스템이다.

```text
누가
어떤 업무를
어느 구역에서
어디까지 수행했고
실제 작업시간은 얼마였으며
어떤 PDA를 사용했고
어떤 특이사항/인수인계가 남았는가
```

단순 CRUD가 아니라 실제 수행이력과 현장 추적성을 중심으로 설계한다.

---

## 2. Stack

Backend:

```text
Java 17
Spring Boot 3.5.x
Spring Security Session
Spring Data JPA / Hibernate
QueryDSL
PostgreSQL
Gradle
```

Frontend:

```text
React 19
TypeScript
Vite
PWA
REST
SSE
```

의도적으로 사용하지 않는 것:

```text
JWT
WebSocket
Docker
Native App
자동 업무 우선순위 AI
```

---

## 3. 권한

```text
ADMIN
MATE
```

ADMIN:

```text
Master Data
업무배정
운영관제
인수인계
정합성
보고서
감사로그
```

MATE:

```text
PDA 로그인
업무 시작/일시정지/재개/종료
마지막 수행위치 보고
특이사항 등록
근무 연장/종료
PDA 반납
```

---

## 4. 시간 모델

업무배정 시각은 실제 작업시간이 아니다.

실제 작업시간의 Source of Truth:

```text
WorkSession.startedAt
WorkSession.endedAt
```

Pause:

```text
현재 Session 종료
```

Resume:

```text
새 WorkSession 시작
```

따라서 휴게/중단 시간은 실제 작업시간에 포함되지 않는다.

---

## 5. shiftDate

자정을 넘는 근무를 지원한다.

```text
08:00 → 18:00 = 당일
22:00 → 06:00 = 익일
동일시간 = invalid
```

야간조:

```text
shiftDate = 시작일
```

WorkSession에 shiftDate를 저장하고
기존 null Row는 Resolver에서 fallback 계산한다.

달력일 보고서와 근무조 보고서를 분리한다.

---

## 6. 진행위치

관리자는 작업의 끝범위를 미리 강제하지 않는다.

MATE는:

```text
마지막 완료 로케이션
```

만 보고한다.

WorkProgress는 Append-only다.

정정:

```text
기존 Row 수정/삭제 X
새 correction Row 추가
```

정정 되돌리기도 반대 방향의 새 correction Row를 추가한다.

---

## 7. Assignment

```text
MATE 1명
+ Area 1개
+ WorkType 1개
= Assignment 1건
```

현재 담당자와 실제 수행자는 분리해 추적한다.

담당변경/인수인계는:

```text
WorkAssignmentHistory
```

에 남는다.

대량 인수인계:

```text
최대 50건
Pessimistic Lock
expectedCurrentMateId
Atomic
부분 성공 없음
```

---

## 8. SpecialIssue

Lifecycle:

```text
UNCONFIRMED
→ CONFIRMED
→ RESOLVED
```

담당자 변경, 확인, 해결은
`SpecialIssueHistory`에 Append-only로 남는다.

대량처리:

```text
expectedStatus
expectedResponsibleMateId
Atomic
```

으로 동시 관리자 stale overwrite를 방지한다.

---

## 9. PDA

물리기기 식별:

```text
PdaDevice.id
```

표시번호:

```text
deviceNumber
```

상태:

```text
AVAILABLE
IN_USE
LOST
INSPECTION
RETIRED
```

사용이력:

```text
PdaUsageHistory
```

을 통해 누가 언제 사용/반납했는지 보존한다.

---

## 10. 통신 신뢰도

Heartbeat:

```text
60초
```

기본 정책:

```text
3분 무응답
→ UNCERTAIN

10분 무응답
→ NETWORK_TIMEOUT
→ Session 종료
→ MATE AWAY / 통신 확인 필요
```

정상 Scheduled End가 먼저 도래했다면:

```text
SCHEDULE_END > NETWORK_TIMEOUT
```

을 우선한다.

---

## 11. ETA

진행률은 KPI가 아니다.

```text
Area 전체 내 현재 위치 %
```

정도만 표시한다.

ETA는 동일:

```text
Area + WorkType
```

의 과거 NORMAL WorkSession만 사용한다.

UNCERTAIN 이력이 섞인 Assignment는 샘플에서 제외한다.

샘플이 없으면:

```text
이력 부족
```

으로 표시하며 타 Area/업무 데이터를 억지로 빌리지 않는다.

---

## 12. 운영관리

ADMIN 주요 흐름:

```text
Dashboard
→ Operations
→ Action Queue
→ Handover
→ Shift Close
→ Handover Overview
→ Shift Report
```

이 흐름은 강제 Wizard가 아니다.

각 화면은 Query String Drilldown으로
정확한 MATE/Assignment/Issue/shiftDate로 이동할 수 있다.

---

## 13. 데이터 정합성

Integrity Scan은:

```text
Session
MATE status
Assignment
PDA Usage
PDA Device status
```

간 불일치를 점검한다.

치명 오류는 관리자 확인대상이다.

Safe Repair는 명백한 상태동기화만 자동 처리하며
업무 의미를 시스템이 추측해야 하는 문제는 자동수정하지 않는다.

---

## 14. Audit

ActivityLog는 의미 있는 업무행동을 저장한다.

```text
로그인/로그아웃
업무배정/담당변경/취소
진행보고/정정
PDA 할당/반납
특이사항
인수인계
교대메모
Scheduler 자동종료
```

UI에서 현재 목록을 숨길 수 있어도
DB History를 물리 삭제하지 않는다.

---

## 15. Security

```text
Spring Security Session/Cookie
BCrypt
ADMIN/MATE Role Guard
CSRF Cookie Token
HttpOnly Session Cookie
SameSite=Lax
Login 시 Session ID Rotation
```

Frontend 변경 요청은
`/api/auth/csrf`에서 Token을 받아 자동으로 Header를 전송한다.

HTTPS 배포 시:

```text
SESSION_COOKIE_SECURE=true
```

를 사용한다.

---

## 16. Demo / Release

Demo Seed는 기본 OFF.

```text
DEMO_SCENARIO_ENABLED=false
```

빈 MATE DB에서만 선택적으로 실행한다.

Runtime:

```text
/readiness
```

에서 Master Data / Integrity / Security 상태를 점검한다.

실제 제출 전에는 별도로:

```text
Gradle test/build
npm install
npm run build
PostgreSQL E2E
```

를 수행해야 한다.
