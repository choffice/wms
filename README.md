# Warehouse Fullstack — Mobile/PWA Phase 3

물류센터 근무자 관리·업무배정 포트폴리오 프로젝트의
**관리자 Web + MATE Mobile/PWA 통합 기준본**입니다.

## 폴더

- `backend/` — Java 17 / Spring Boot / PostgreSQL / JPA / QueryDSL
- `frontend/` — React + TypeScript + Vite
  - 관리자 Web
  - MATE Responsive Mobile Web / PWA

## 실행

### 1. PostgreSQL

```sql
CREATE DATABASE warehouse;
```

기본 연결:

```text
jdbc:postgresql://localhost:5432/warehouse
username: postgres
password: postgres
```

### 2. Backend

IntelliJ에서 `backend/`를 Gradle 프로젝트로 열고 Java 17로 실행합니다.

기본 관리자:

```text
AD0001 / admin1234
```

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

```text
http://localhost:5173
```

Vite의 `/api` 프록시가 Spring Boot `8080`으로 연결됩니다.

---

# 관리자 Web

기존 Phase 2 기능 유지:

- 관리자 Session 로그인
- Dashboard
- MATE 현황
- 업무배정 / 트레이드 / History
- 공지 관리
- 특이사항 관리
- PDA 관리
- 계층형 Location 관리
- 근무스케줄 / 예외시간 / 연장
- 작업시간 및 일일 보고서
- Activity Log
- SSE

---

# MATE Mobile / PWA

접속:

```text
http://localhost:5173/mate/login
```

## 로그인

1. 서버에 등록되어 있고 현재 `AVAILABLE`인 PDA 번호 선택
2. `MT` 사원번호 입력
3. 비밀번호 입력
4. 로그인 성공 시 PDA 사용이력 생성 + Spring Session 인증

한 PDA에 한 MATE, 한 MATE에 한 PDA 규칙을 적용합니다.

## 모바일 하단 메뉴

- 홈
- 업무
- 특이사항
- 공지
- 더보기

## 업무

실제 MATE 흐름:

```text
배정업무 확인
→ 시작
→ WorkSession 실제시간 기록
→ 진행기록
→ 일시정지
→ 상태/거소
→ 재개
→ 완료
```

진행기록은 **마지막 실제 완료 로케이션 하나만** 입력합니다.

완료는 관리자 지정 범위 전체 완수의 의미가 아니라:

> 이번 작업을 현재 위치까지 수행하고 종료

의 의미로 화면에서 명확히 안내합니다.

완료된 Assignment도 최근 완료 목록에서 **기록 정정**이 가능합니다.

## Heartbeat

MATE Shell에서 현재 Open WorkSession을 감지하여 작업 중에는 주기적으로 heartbeat를 전송합니다.

- 모바일의 다른 탭(특이사항/공지/더보기)으로 이동해도 Shell은 유지
- 작업 중 heartbeat 유지
- 서버에서 장시간 통신 단절 시 `UNCERTAIN` 처리 가능

## 일시정지 / 거소

일시정지는 실제 비작업시간이며 작업시간 통계에서 제외됩니다.

상태:

```text
AVAILABLE
BREAK
AWAY
```

거소:

- 휴게실
- 식사
- 사무실
- 창고
- 기타 직접입력

`WORKING`은 업무 시작 시 자동 적용됩니다.

## 특이사항

관리자가 설정한 구분의 구성요건에 따라 MATE 입력폼이 달라집니다.

가능 항목:

- 업무 연결
- 로케이션
- 상품코드
- 수량
- 실재고
- MMS 재고
- 유통기한 재고
- 재고 없음
- 코멘트

업무를 연결하지 않아도 로케이션 필수 유형은 전체 활성 Location에서 위치를 선택할 수 있습니다.

## 공지

관리자가 `visible=true`로 설정한 공지만 표시합니다.

- 중요 ★ 구분
- 수정시각
- SSE 변경 반영

## 더보기

- 상태/거소 변경
- 근무 연장
- 수동 근무 종료
- PDA 반납
- 로그아웃

로그아웃 시 PDA는 자동 반납됩니다.

---

# PWA

포함:

- `manifest.webmanifest`
- 192px / 512px App Icon
- Service Worker
- standalone 시작 URL `/mate/login`
- portrait-primary
- safe-area 대응

Service Worker는 **앱 Shell만 캐싱**하며 업무 API 요청을 오프라인 큐에 쌓지 않습니다.
현장 기록의 중복/순서오염을 피하기 위해 서버 쓰기 요청은 항상 온라인 처리를 원칙으로 합니다.

---

# Phase 3에서 Backend에 추가된 API

## 로그인 전 PDA 선택

```http
GET /api/auth/mate/pdas
```

현재 로그인 가능한 `AVAILABLE` PDA만 반환.

## MATE Lookup

```http
GET /api/mate/lookups/issue-types
GET /api/mate/lookups/locations
GET /api/mate/lookups/locations?areaId={id}
```

## 현재 WorkSession

```http
GET /api/mate/work-sessions/current
```

페이지 새로고침 후에도 현재 작업세션을 복구하고 heartbeat를 계속할 수 있습니다.

---

# 현재 별도 검토가 필요하지 않은 부분

이번 Phase에서는 앞서 확정한 업무 규칙을 그대로 UI로 옮겼으며,
새로운 현장 정책을 임의로 추가하지 않았습니다.

실제 시연 후 검토 가능성이 높은 것은:

- 휴대폰에서 한 화면에 표시할 정보 밀도
- 업무 카드 버튼 순서
- 진행 로케이션 선택 방식(Select → 검색형 입력으로 변경 여부)
- 특이사항 로케이션 목록이 커질 경우 검색 방식
- PWA 설치 시 실제 기기 UX

이 부분들은 화면 테스트 시 사용자가 판단하면 되고 현재 기능 규칙을 바꾸지는 않습니다.


## Phase 3.1 추가 보강

이번 보강에서 정책을 바꾸지 않고 다음 항목을 추가했습니다.

### MATE 상태 API 권한 보강

기존 `/api/mates/{mateId}/status` 방식은 MATE가 다른 MATE ID를 알고 있을 경우
타인의 상태를 변경할 여지가 있어 제거했습니다.

현재:

```text
GET   /api/mate/status
PATCH /api/mate/status
```

MATE는 자신의 Session 기준 상태만 읽고 수정합니다.

관리자는 별도:

```text
PATCH /api/admin/mates/{mateId}/status
```

를 사용합니다.

### 네트워크 상태 표시

MATE PWA가 오프라인이면 상단에 경고바를 표시합니다.

업무 진행/특이사항 등의 쓰기 작업은 오프라인 Queue에 임의 저장하지 않습니다.
실시간 운영기록의 중복과 순서오염을 막기 위한 기존 원칙을 유지합니다.

### 긴 로케이션 목록 대응

진행기록 / 완료 / 특이사항 로케이션 선택 앞에 검색 필드를 추가했습니다.

예:

```text
A01-13
```

을 입력하면 기존 Select 목록을 실시간 필터링합니다.

기존 선택 방식 자체는 유지했기 때문에 현장 테스트 후
자동완성 Combobox로 교체할지 여부를 나중에 결정할 수 있습니다.

### 실제 현재 상태 복원

`더보기` 화면을 열면 서버에서 현재 MATE 상태/거소를 조회하여
화면 Select 값이 실제 DB 상태와 일치하도록 수정했습니다.

---

## Vite Build 재시도 결과

이번 세션에서 `npm install`을 장시간 재시도했으나
npm registry 연결 자체가 응답하지 않아 설치가 완료되지 않았습니다.

추가 확인:

```text
npm install --offline
→ ENOTCACHED (@types/react 캐시 없음)
```

따라서 이 실행환경에서는 Vite dependency를 확보할 수 없어
`vite build` 자체는 실행할 수 없었습니다.

대신 dependency 설치 없이 가능한 검사는 다시 수행했습니다.

- 전체 TS/TSX TypeScript parser/transpile 구문 검사: 통과
- 상대 import 경로 검사: 통과
- Java 파일 brace / Java 21 잔존 / 폐기 API 경로 검사: 통과
- `tsconfig.node.json`의 `allowImportingTsExtensions` 관련 설정 오류 수정 (`noEmit: true`)

실제 PC에서 인터넷 연결이 가능한 환경에서는:

```bash
cd frontend
npm install
npm run build
```

로 최종 Vite build를 확인하면 됩니다.


## Phase 3.3 ERP 운영 보강

UI를 더 ERP/WMS답게 조정하면서 실제 운영용 Master Data 기능을 함께 보강했습니다.

### 관리자 업무배정

- Card 목록 제거
- Data Grid 방식 조회
- 상태 / MATE / 업무 / 구역 / 키워드 필터
- 시작 Location 검색
- 진행기록 / WorkSession 하위 Grid
- 담당 MATE 변경

### MATE 관리자 화면

- Master Table + Detail Panel 구조
- 사번 / 실명 / 별명 / 상태 / 거소 / 활성 조회
- 운영 별명 수정
- 기본 근무스케줄
- 기간별 예외 근무시간
- 당일 연장

### PDA

- 기기별 사용이력 조회
- 표시번호 수정
- 두 기기 번호 맞교환
- 상태 변경
- 삭제 또는 RETIRED

### Location

- 층수 수정
- FOOD / NON_FOOD
- GENERAL / COLOR / HYGIENE / TOOLS
- 하위 단계 추가
- 같은 단계 추가
- 하위 숫자 범위 추가

### 업무 / 특이사항 Master

- WorkType 수정 / 비활성
- IssueType 수정 / 비활성
- IssueType 이름 중복 검증

### 보고서

업무 종류별 작업시간 카드를 제거하고 ERP형 집계 Table로 변경했습니다.

### 검토가 필요한 다음 정책

구역 진행률 %의 운영 의미는 `docs/PHASE3_3_ERP_OPERATIONS.md` 참고.

Assignment가 끝 범위를 갖지 않기 때문에
현재 임시 계산식을 계속 사용할지는 다음 단계 전에 확정하는 편이 안전합니다.


## Phase 3.4 예상 잔여 소요시간

진행률의 의미를 다음처럼 확정했습니다.

```text
해당 구역 전체 활성 Leaf 중
현재 마지막 수행 로케이션이 위치한 지점 %
```

진행률 자체는 중요한 성과지표가 아닙니다.
관리자는 마지막 수행 로케이션을 보고 판단합니다.

대신 시스템은 업무배정 참고데이터로:

```text
같은 구역 + 같은 업무의 과거 정상 WorkSession
+ 각 Assignment가 실제로 수행한 구간 비율
```

을 이용해 구역 전체 환산 평균 작업시간을 계산하고,
현재 지점 또는 새로 선택한 시작점부터 구역 끝까지의 **예상 잔여시간**을 보여줍니다.

예:

```text
현재 구역 위치       62% · A01-13-24
현재점 예상 잔여     약 2시간 20분
선택 시작점 기준     A01-14-01 · 약 2시간 10분
Historical Sample    6건
```

정상 완료 이력이 없으면 `이력 부족`으로 표시합니다.

- UNCERTAIN 포함 안 함
- 다른 Area / WorkType 평균으로 자동 대체 안 함
- 자동 우선순위 계산 안 함
- 시간은 UI에서 약 10분 단위로 표시

상세 계산 규칙은:

- `docs/PHASE3_4_WORK_ESTIMATE.md`


## Phase 3.5 운영관제

관리자에 `/operations` 운영관제 화면을 추가했습니다.

주요 기능:

- MATE 현재 상태 / 거소
- PDA 할당상태
- 현재 업무 / 수행 위치
- Open WorkSession / 경과시간
- Heartbeat / UNCERTAIN
- 오늘 근무종료시간 / 연장
- 운영 이상 표시
- 관리자 연장 ON/OFF
- 안전한 PDA 회수
- 업무배정 취소

업무배정 상세 이력에는 기존 진행/시간 이력 외에
ASSIGN / TRADE / CANCEL / COMPLETE Lifecycle 이력도 표시합니다.

또한 `WorkAssignment.startedAt` 중복 필드를 제거하여
실제 시작시각은 `WorkSession.startedAt`만 Source of Truth로 사용합니다.

세부사항:

- `docs/PHASE3_5_OPERATIONS_CONTROL.md`


## Phase 3.6 특이사항/세션 운영

이번 단계에서는 현장에서 쌓이는 예외정보와
네트워크 이상 WorkSession의 운영처리를 보강했습니다.

### 특이사항

- 담당 MATE 지정 / 변경 / 해제
- 미담당 필터
- 담당 변경사유
- CREATE / RESPONSIBLE_CHANGE / CONFIRM / RESOLVE / DELETE History
- `미확인 → 확인 → 해결` 순서 Backend 강제
- 동시 관리자 처리용 Row Lock
- MATE가 타인의 Assignment를 임의 연결하지 못하도록 검증

### WorkSession 신뢰도

기본:

```text
3분 Heartbeat 단절 → UNCERTAIN
10분 Heartbeat 단절 → NETWORK_TIMEOUT 자동 종료
```

10분 종료 후에는 MATE를 `AWAY / 통신 확인 필요`로 표시합니다.
Assignment는 유지되므로 네트워크 복귀 후 `재개`하면 새 WorkSession이 시작됩니다.

근무 종료시각을 이미 지난 경우에는 Network Timeout보다
정상 `SCHEDULE_END`가 우선합니다.

설정:

```text
HEARTBEAT_UNCERTAIN_MINUTES
NETWORK_TIMEOUT_MINUTES
```

운영관제에는 미담당 특이사항 수와 통신 복귀 필요 MATE 표시도 추가했습니다.

상세:

- `docs/PHASE3_6_ISSUE_AND_SESSION_OPERATIONS.md`


## Phase 3.7 보고서 / 기간 통계

보고서 통계의 기간 정합성을 보강하고
기간별 운영분석 화면을 추가했습니다.

### 일일 통계 정합성

날짜를 걸쳐 수행된 WorkSession은
조회 날짜에 실제로 겹치는 시간만 계산합니다.

예:

```text
08/26 23:30 ~ 08/27 01:30
→ 08/27 통계 = 1시간 30분
```

과거 일일 보고서의 `마지막 수행위치`도
현재 Assignment 진행값을 사용하지 않고
그 날짜 종료시각 이전의 마지막 WorkProgress를 사용합니다.

### 기간 통계

```text
GET /api/admin/reports/range
```

- 최대 366일
- NORMAL / UNCERTAIN 작업시간 분리
- MATE별 실제 작업시간
- 구역 + 업무별 실제 작업시간
- 일자별 작업시간 / 특이사항 추이
- Assignment / Session / PDA 사용이력 Summary

프론트 보고서 화면도:

```text
[일일 보고서] [기간 통계]
```

구조로 확장했습니다.

상세:

- `docs/PHASE3_7_REPORT_ANALYTICS.md`


## Phase 3.8 감사로그

관리자 메뉴에 `/audit-logs`를 추가했습니다.

감사로그에서:

- 기간
- 행위유형
- 처리자
- ReferenceType
- 키워드

로 검색할 수 있으며 25 / 50 / 100행 Paging을 지원합니다.

업무배정·특이사항 등 Reference가 있는 이력은
원본 관리화면으로 Drilldown할 수 있습니다.

추가 인증 이력:

```text
ADMIN_LOGIN
MATE_LOGIN
AUTH_LOGOUT
```

또한 기존 Dashboard의 `현재 로그 지우기`는
DB ActivityLog를 삭제할 수 있어 원래 이력보존 원칙과 충돌하므로 제거했습니다.

현재 Dashboard에서는 단순히:

```text
현재 표시 숨기기
```

만 수행하며 DB 이력은 그대로 보존합니다.

상세:

- `docs/PHASE3_8_AUDIT_TRAIL.md`


## Phase 3.9 기준정보 감사이력

Phase 3.8의 append-only ActivityLog를
관리자 기준정보 변경까지 확장했습니다.

추가 감사대상:

- MATE 등록 / 별명 / 활성상태
- MATE 기본 스케줄 / 예외시간 / 연장
- MATE 상태 / 거소
- PDA 등록 / 번호 / 상태 / 맞교환 / RETIRED / 삭제
- Location 생성 / 속성 / 비활성
- WorkType 등록 / 수정 / 비활성
- IssueType 등록 / 수정 / 비활성

감사로그 검색에는 정확한 Entity 추적을 위한:

```text
referenceType
referenceId
```

조합 필터도 추가했습니다.

설정과 MATE 상세 화면에서 해당 Entity의 감사이력으로
바로 이동할 수 있습니다.

상세:

- `docs/PHASE3_9_MASTER_DATA_AUDIT.md`


## Phase 4.0 운영 데이터 정합성 / 안전복구

관리자 메뉴에 `/integrity` 정합성 검사 화면을 추가했습니다.

검사 범위:

- MATE 상태 ↔ Open WorkSession
- PDA 상태 ↔ 활성 PdaUsageHistory
- WorkAssignment 상태 ↔ Open WorkSession
- WorkSession MATE ↔ Assignment 담당자
- WorkSession ↔ PDA Usage 소유자/반납상태
- 중복 활성 PDA Usage / Open Session

Source of Truth가 명확한 항목은 관리자 확인 후 안전복구할 수 있습니다.

예:

```text
PDA IN_USE + 활성 사용이력 없음
→ AVAILABLE

MATE WORKING + Open Session 없음
→ AVAILABLE

Open Session + Assignment ASSIGNED
→ IN_PROGRESS
```

중복 Session이나 담당자 불일치처럼
어느 기록을 살릴지 판단이 필요한 문제는 자동수정하지 않습니다.

`안전복구 일괄 실행`도 자동복구 가능한 항목만 처리하며,
실행 시점에 조건을 다시 검증합니다.

복구는 `INTEGRITY_REPAIR` 감사로그로 남고
기존 WorkSession / WorkProgress / Usage History 등은 삭제하지 않습니다.

또한 관리자 PDA 강제회수 시 ActivityLog actor가
MATE로 잘못 보일 수 있던 흐름을 `releaseByAdmin()`으로 분리했습니다.

상세:

- `docs/PHASE4_0_DATA_INTEGRITY_RECOVERY.md`


## Phase 4.1 미처리 업무 / 인수인계

관리자 메뉴에 `/handover`를 추가했습니다.

Open WorkSession이 없는 활성 Assignment를 모아:

- 통신 복귀 확인
- 퇴근 인수인계
- 근무종료 이월
- 미시작 배정
- 일시정지
- 재개 대기

상태로 분류합니다.

자동 우선순위는 만들지 않으며,
운영상 다음 동작이 필요한 업무를 모아보는 용도입니다.

Grid에서 새 담당 MATE를 선택하면 기존 Assignment를 그대로
`TRADE`하여 WorkProgress / 마지막 수행위치 / 과거 Session을 유지합니다.

실제 WorkSession 시작은 관리자 대신 MATE가 `[시작]/[재개]`를 눌러야 합니다.
따라서 실제 작업시간 측정 원칙을 유지합니다.

### 진행위치 정정 안전장치

관리자 업무이력에서 마지막 수행위치를 정정할 수 있습니다.

- Open Session 중 정정 금지
- 저장 직전 `expectedCurrentLocationId` 재검증
- 오래된 화면이면 `PROGRESS_STALE`
- 기존 WorkProgress 삭제 없음
- correction Row 추가
- `WORK_PROGRESS_CORRECTION` 감사로그
- 실제 저장 Account를 `reported_by_account_id`로 별도 보존

MATE 진행보고/작업종료 위치 갱신에도 같은 stale-value 검증을 적용했습니다.

상세:

- `docs/PHASE4_1_HANDOVER_AND_CORRECTION_GUARDS.md`


## Phase 4.2 선택처리 / 교대 인수인계

관리자 `/handover` 화면에 선택처리를 추가했습니다.

### 일괄 인수인계

- Checkbox 선택
- 최대 50건
- 인수인계 검토건 빠른 선택
- 공통 새 담당 적용
- 행별 담당 Override
- 공통/행별 사유
- 전체 Transaction 처리

요청에는 각 Assignment의:

```text
expectedCurrentMateId
```

를 함께 보내므로 다른 관리자가 먼저 담당자를 변경했거나
Open WorkSession이 다시 시작된 경우 전체 Batch를 중단합니다.

부분 성공은 허용하지 않습니다.

### 최근 정정 되돌리기

업무 이력의 가장 최근 WorkProgress가 안전하게 되돌릴 수 있는
`correction=true` Row라면 `최근 정정 되돌리기`를 제공합니다.

기존 Row 삭제가 아니라 반대 방향의 새 correction Row를 추가하므로:

```text
원본 → 정정 → 정정 되돌리기
```

전 과정이 남습니다.

### 운영관제 후속조치

운영관제 Row에서:

- MATE 상세
- 정확한 Assignment
- 인수인계
- 정합성

화면으로 바로 이동할 수 있습니다.

상세:

- `docs/PHASE4_2_BULK_HANDOVER_AND_RECOVERY_UX.md`


## Phase 4.3 교대 마감 점검 / 특이사항 선택처리

신규 관리자 화면:

```text
/shift-close
```

교대 전에 다음 운영상태를 한 번에 확인합니다.

### 필수 확인

- Open WorkSession
- 치명 정합성 오류

### 인계 확인

- UNCERTAIN Session
- 사용중 PDA
- 인수인계 후보
- 미처리 Assignment
- 미확인 특이사항
- 미담당 특이사항
- 미해결 특이사항

이 화면은 전역 마감 버튼이 아니라
운영 Preview / Checklist입니다.

### 특이사항 선택처리

`/issues` Grid에서 최대 50건을 선택하여:

- 일괄 확인
- 일괄 해결
- 일괄 담당 지정/해제

할 수 있습니다.

각 요청은 화면에서 보던 `expectedStatus` /
`expectedResponsibleMateId`를 함께 보내며,
한 건이라도 다른 관리자가 먼저 수정했다면 전체 Transaction을 취소합니다.

기존 개별 특이사항 담당변경도
`expectedResponsibleMateId` Guard를 사용합니다.

상세:

- `docs/PHASE4_3_SHIFT_CLOSE_AND_ISSUE_BULK.md`


## Phase 4.4 후속조치 큐 / 교대 인계요약

신규 관리자 화면:

```text
/action-queue
/handover-overview
```

### 후속조치 큐

운영관제·정합성·인수인계·특이사항 중
현재 관리자 확인이 필요한 항목을 한 Grid에 모았습니다.

정렬:

```text
즉시 확인
→ 운영 확인
→ 인수인계
→ 특이사항
```

이는 업무 생산성/중요도 자동순위가 아니라
운영상 후속조치 분류입니다.

각 Row는 원본 MATE / Assignment / Issue / Integrity 화면으로
바로 Drilldown합니다.

### 교대 인계요약

한 화면에서:

- 미처리 Assignment
- 미해결 특이사항
- 정합성
- Open Session
- 최근 관리자 처리내역
- 최근 인계메모

를 봅니다.

`요약 복사`로 현재 Snapshot을 텍스트 형태로 바로 복사할 수 있습니다.

### 교대 인계메모

`HandoverNote`를 추가했습니다.

메모는 작성자/작성시각과 함께 Append-only로 보존하며
현재 단계에서는 수정/삭제하지 않습니다.

감사로그:

```text
HANDOVER_NOTE_CREATE
```

도 같이 기록합니다.

특정 `오늘` 또는 근무조 날짜는 아직 추정하지 않고
`최근 관리자 처리내역`을 표시하여 자정 경계 정책과 충돌하지 않게 했습니다.

상세:

- `docs/PHASE4_4_ACTION_QUEUE_HANDOVER_OVERVIEW.md`


## Phase 4.5 야간조 대응 / shiftDate

스케줄 해석을 야간조 대응형으로 확장했습니다.

```text
08:00 → 18:00 = 당일 종료
22:00 → 06:00 = 익일 06:00 종료
08:00 → 08:00 = 입력 거부
```

야간조 기준일은 근무 시작 날짜인 `shiftDate`입니다.

### 중앙 스케줄 Resolver

`WorkScheduleResolver`가:

- 기본 요일 스케줄
- 기간 Override
- 익일 종료
- 새벽의 전날 야간조
- Extension

을 통합 계산합니다.

### WorkSession shiftDate

신규 WorkSession은 시작 시 `shift_date`를 저장합니다.

기존 Row는 nullable 상태를 허용하며
`startedAt + MATE schedule`로 fallback 계산합니다.

자동종료, 운영관제 예정종료, Extension ON/OFF도
달력 날짜가 아닌 WorkSession shiftDate를 우선합니다.

### 근무조 보고서

신규 API:

```text
GET /api/admin/reports/shift/{shiftDate}
```

Frontend 보고서에 `근무조 보고서` 탭을 추가했습니다.

자정을 넘긴 WorkSession은 달력 자정에서 나누지 않고
전체 작업시간을 시작일의 shiftDate에 귀속합니다.

기존 `일일 보고서 / 기간 통계`는 달력일 기준으로 그대로 유지하여
의미를 분리했습니다.

근무조 보고서는:

- 실제 작업시간
- Session / Open / UNCERTAIN
- Assignment / MATE
- 특이사항
- PDA
- 야간조 Session
- 이전 실제 근무조 대비

를 제공합니다.

상세:

- `docs/PHASE4_5_OVERNIGHT_SHIFT_REPORTING.md`


## Phase 4.6 근무조 통합 / Demo Scenario

근무조 보고서를 교대 운영화면과 직접 연결했습니다.

- `/shift-close` → 최근 실제 shiftDate 보고서
- `/handover-overview` → 최근 실제 shiftDate 보고서
- `/reports?mode=SHIFT&shiftDate=YYYY-MM-DD` deep link 지원
- `HandoverNote.shiftDate` 선택 연결
- 공통 인계메모는 shiftDate=null 유지

### 선택형 Demo Scenario

기본값은 OFF입니다.

```text
DEMO_SCENARIO_ENABLED=false
```

빈 DB에서 포트폴리오 시연용 기본 데이터를 넣고 싶을 때만:

```text
DEMO_SCENARIO_ENABLED=true
DEMO_MATE_PASSWORD=mate1234
```

로 실행합니다.

기존 MATE 데이터가 하나라도 있으면 Demo Seed는 전체 Skip하여
실사용/개발 데이터를 오염시키지 않습니다.

Seed는 주간근무 중심의 MATE/PDA/Location/WorkType/IssueType과
미시작·재개대기 Assignment, 미확인/확인 SpecialIssue를 생성합니다.

과거 WorkSession 시간을 인위적으로 조작하지 않습니다.

상세:

- `docs/PHASE4_6_SHIFT_INTEGRATION_DEMO_READINESS.md`


## Phase 4.7 보안 마감 / 시연 준비점검

Session/Cookie 인증 구조에 CSRF 보호를 활성화했습니다.

Frontend는 변경 요청 전에:

```text
GET /api/auth/csrf
```

로 Token을 준비하고 POST/PUT/PATCH/DELETE에
서버가 알려준 CSRF Header를 자동으로 붙입니다.

Session Cookie:

```text
HttpOnly
SameSite=Lax
SESSION_COOKIE_SECURE 환경변수
```

를 적용했습니다.

### 시연점검

신규:

```text
/readiness
GET /api/admin/system-readiness
```

Master Data + Integrity + Session 상태를 한 화면에서 확인합니다.

READY 기준은:

```text
MATE/PDA/Location/WorkType/IssueType 존재
Integrity Critical = 0
```

입니다.

Open Session/인수인계/미해결 특이사항은
실제 운영상 정상 상태일 수 있으므로 READY를 막지 않습니다.

### 시연/배포 문서

- `docs/DEMO_RUNBOOK.md`
- `docs/RELEASE_CHECKLIST.md`
- `docs/TECHNICAL_SPEC.md`
- `docs/PHASE4_7_SECURITY_READINESS.md`


## Phase 4.7.2 Java 17 Comparator inference hotfix

실제 Java 17 Gradle compile에서 Comparator 체인의 첫 람다가 Object로 추론될 수 있는 지점을 전체 main source에서 재점검했습니다.

- ActionQueueService: explicit `Comparator.<ActionQueueItemResponse>comparingInt`
- HandoverService: explicit `Comparator.<HandoverRowResponse>comparingInt`
- IntegrityService: explicit `Comparator.<IntegrityIssueResponse>comparingInt`
- ReportService / WorkScheduleResolver / PdaAdminService / AuthController comparator lambdas도 타입 명시

이 수정은 업무 로직을 변경하지 않고 Java 17 타입 추론의 모호성만 제거합니다.
