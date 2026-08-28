# Phase 2 Backend

이번 단계에서 실제 Service / REST API 골격을 추가했다.

## 구현 범위

### MATE
- MATE 등록
- 사원번호 `MT0001` 자동 발급
- BCrypt 비밀번호 저장
- MATE 목록
- 별명 변경
- 비활성 / 재활성
- 기본 근무스케줄 일괄 저장
- 상태 / 거소 변경 + History 기록

### PDA
- PDA 등록
- 번호 중복 방지
- 상태 변경
- 사용이력 있는 PDA는 삭제 대신 RETIRED
- 사용이력 없는 PDA는 삭제
- 두 기기의 표시 번호 맞교환
- 개발/시연용 PDA 로그인
  - PDA 번호
  - 사원번호
  - 비밀번호
- 한 PDA = 동시에 한 MATE
- 한 MATE = 동시에 한 PDA
- 반납 이력 저장

### Location
- 최상위 Location 생성
- 하위 단계 추가
- 같은 단계 sibling 추가
- 숫자 범위 하위 단계 일괄 확장
- fullCode 문자열 기준 조회/정렬
- 비활성화

## 아직 Phase 2에서 하지 않은 것

- 실제 Spring Security Session 인증/권한
- 관리자 로그인
- WorkType CRUD
- WorkAssignment Service
- WorkProgress 기록/정정
- WorkSession 시작/일시정지/재개/완료
- 근무시간 자동 종료
- 연장 Override
- heartbeat / UNCERTAIN 자동 처리
- 업무 트레이드
- SSE
- 특이사항 / 공지 / ActivityLog

## 대표 API

### MATE

```http
POST /api/admin/mates
GET  /api/admin/mates
PATCH /api/admin/mates/{mateId}/nickname
POST /api/admin/mates/{mateId}/deactivate
POST /api/admin/mates/{mateId}/reactivate

GET /api/admin/mates/{mateId}/schedules
PUT /api/admin/mates/{mateId}/schedules
```

MATE 등록 예:

```json
{
  "name": "김민수",
  "nickname": "민수",
  "password": "1234",
  "joinedAt": "2026-08-26"
}
```

### PDA

```http
POST   /api/admin/pdas
GET    /api/admin/pdas
PATCH  /api/admin/pdas/{deviceId}/status
DELETE /api/admin/pdas/{deviceId}
POST   /api/admin/pdas/swap-numbers
```

PDA 로그인:

```http
POST /api/mate/pda-sessions/login
```

```json
{
  "deviceNumber": 12,
  "employeeNo": "MT0001",
  "password": "1234"
}
```

PDA 반납:

```http
POST /api/mate/pda-sessions/{usageId}/return
```

### Location

```http
POST /api/admin/locations/roots
POST /api/admin/locations/{parentId}/children
POST /api/admin/locations/{referenceId}/siblings
POST /api/admin/locations/{parentId}/children/range
GET  /api/admin/locations
GET  /api/admin/locations/{parentId}/children
```

예:

```json
{"segment":"A01"}
```

```json
{"segment":"13"}
```

```json
{
  "startNumber": 1,
  "endNumber": 24,
  "width": 2
}
```

## 주의

Security는 아직 `permitAll`이다.
실제 ADMIN/MATE 권한은 업무 API가 붙은 다음 단계에서 Session 기반으로 잠근다.

PDA Login은 지금 단계에서는 업무 흐름을 검증하기 위한 API이며,
Phase 3 Security 적용 시 Spring Security Authentication/Session과 연결한다.
