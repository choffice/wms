# Warehouse Control System — Demo Runbook

## A. 빈 DB에서 빠른 시연 준비

환경변수:

```text
DEMO_SCENARIO_ENABLED=true
DEMO_ADMIN_PASSWORD=admin1234
DEMO_MATE_PASSWORD=mate1234
```

Demo Scenario는 MATE Table이 비어 있을 때만 동작한다.

기존 MATE가 하나라도 있으면 Seed를 전체 Skip한다.

---

## B. ADMIN 로그인

기본 빈 DB 기준 최초 ADMIN:

```text
AD0001
```

Password:

```text
DEMO_ADMIN_PASSWORD
```

로그인 후 가장 먼저:

```text
시연점검
→ /readiness
```

를 연다.

확인:

```text
READY
CSRF ENABLED
Integrity Critical = 0
Master Data 존재
```

---

## C. 운영현황 시연

순서 예시:

```text
1. 현황판
2. 운영관제
3. 후속조치
4. 인수인계
5. 특이사항
```

Demo Scenario에는:

```text
A01 재고조사
→ IN_PROGRESS
→ 마지막 수행 A01-02
→ Open Session 없음

B01 진열보충
→ ASSIGNED
```

가 존재한다.

따라서:

```text
재개대기
미시작 업무
인수인계 후보
```

흐름을 설명할 수 있다.

---

## D. MATE 시연

MATE 사원번호는 EmployeeNumberService가 생성한다.

정확한 번호는 ADMIN의:

```text
MATE
```

화면에서 확인한다.

Demo MATE Password:

```text
DEMO_MATE_PASSWORD
```

PDA:

```text
31
32
33
```

MATE Login에서:

```text
PDA 선택
사원번호
비밀번호
```

를 입력한다.

---

## E. 실제 작업시간 흐름

MATE에서:

```text
업무 → 시작
```

을 누른 시점부터 WorkSession 시간이 시작된다.

시연 포인트:

```text
배정시각 ≠ 작업시작시각
```

진행보고는:

```text
마지막 완료 로케이션
```

만 입력한다.

관리자에서는:

```text
운영관제
업무배정 상세
```

에서 실제 수행 위치와 Session을 확인한다.

---

## F. 특이사항

MATE에서 특이사항을 등록하면 ADMIN:

```text
특이사항
후속조치
운영관제/SSE
```

에 반영된다.

Lifecycle:

```text
미확인
→ 확인
→ 해결
```

선택처리:

```text
최대 50건
Atomic
부분 성공 없음
```

도 설명 가능하다.

---

## G. 교대 흐름

MATE 작업을 일시정지하거나 근무종료 후 ADMIN:

```text
인수인계
→ 교대 마감점검
→ 인계요약
```

순으로 확인한다.

인계메모는:

```text
공통 메모
특정 shiftDate 연결 메모
```

중 하나를 선택할 수 있다.

메모는 Append-only다.

---

## H. 야간조 확장성 시연

실제 Demo 기본값은 주간:

```text
08:00 → 18:00
```

이다.

확장성 설명이 필요하면 ADMIN MATE 스케줄에서:

```text
22:00 → 06:00
```

을 입력한다.

시스템은:

```text
익일 06:00 종료
shiftDate = 시작일
```

로 해석한다.

```text
08:00 → 08:00
```

은 잘못된 스케줄로 거부된다.

---

## I. 보고서

```text
일일 보고서(달력일)
근무조 보고서
기간 통계(달력일)
```

의 의미를 구분한다.

야간조 Session:

```text
23:00 → 02:00
```

일일 보고서:

```text
날짜별 시간 분리
```

근무조 보고서:

```text
전체 Session을 시작일 shiftDate에 귀속
```

한다.

---

## J. 감사/복구

마지막으로:

```text
감사로그
정합성
시연점검
```

을 연다.

설명 포인트:

```text
진행위치 정정도 기존 Row DELETE 없음
정정 → 되돌리기도 새 이력 추가
담당변경 동시수정 stale guard
ActivityLog DB 삭제 기능 없음
정합성 자동수정은 Safe 항목만
```

---

## K. 실제 제출 전

코드 정적 검사가 아닌 실제 실행환경에서 반드시:

```text
Backend Gradle test/build
Frontend npm install
Frontend npm run build
PostgreSQL 연결
ADMIN 로그인
MATE 로그인
CSRF POST 요청
SSE
PWA
```

을 확인한다.

자세한 내용:

```text
docs/RELEASE_CHECKLIST.md
```
