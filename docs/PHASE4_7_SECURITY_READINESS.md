# Phase 4.7 — CSRF / Session 보안 / 시연 준비점검

## 1. Session 인증 유지

프로젝트의 인증 방식은 기존대로:

```text
Spring Security
Session / Cookie
ADMIN / MATE
```

를 유지한다.

JWT는 추가하지 않는다.

권한:

```text
/api/admin/** → ADMIN
/api/mate/**  → MATE
/api/auth/**  → 인증 전 접근 가능
/api/health   → 공개
```

---

## 2. CSRF 활성화

이전 개발 단계에서는 CSRF가 임시 비활성화되어 있었다.

Phase 4.7부터 CSRF를 활성화했다.

Repository:

```text
CookieCsrfTokenRepository
```

Cookie는 JavaScript에서 직접 읽을 필요가 없으므로
HttpOnly 기본값을 유지한다.

Frontend는 Cookie를 파싱하지 않고:

```text
GET /api/auth/csrf
```

로 Token을 받는다.

Response:

```text
token
headerName
parameterName
```

---

## 3. 변경 요청

Frontend API Client는 다음 Method에만 CSRF Token을 자동 부착한다.

```text
POST
PUT
PATCH
DELETE
```

즉:

```text
GET
HEAD
OPTIONS
TRACE
```

는 Token 준비가 필요 없다.

변경 요청 전 최초 1회:

```text
/api/auth/csrf
```

를 호출하고 메모리에서 Token을 재사용한다.

Header 이름은 서버 Response의:

```text
headerName
```

을 사용하므로 `X-XSRF-TOKEN` 문자열을 Client 코드 여러 곳에
하드코딩하지 않는다.

---

## 4. 로그인도 보호

ADMIN/MATE 로그인 Endpoint는 인증 전 접근 가능하지만
상태 변경 POST 요청이므로 CSRF 검증을 받는다.

Client의 `adminLogin`, `mateLogin` 역시
일반 `request()` 함수 경로를 사용하므로 로그인 전에
자동으로 CSRF Token을 준비한다.

---

## 5. SSE

기존 SSE:

```text
/api/admin/events
/api/mate/events
```

는 GET 요청이므로 CSRF 변경요청 검증 대상이 아니다.

Session Cookie 인증은 그대로 유지한다.

---

## 6. Session Cookie

application.yml:

```yaml
server:
  servlet:
    session:
      cookie:
        http-only: true
        same-site: lax
        secure: ${SESSION_COOKIE_SECURE:false}
```

개발환경의 HTTP localhost에서는:

```text
SESSION_COOKIE_SECURE=false
```

를 사용할 수 있다.

HTTPS 배포 시:

```text
SESSION_COOKIE_SECURE=true
```

로 전환한다.

---

# System Readiness

## 7. 신규 화면

```text
/readiness
```

신규 API:

```text
GET /api/admin/system-readiness
```

시연 직전 다음을 한 화면에서 확인한다.

Master Data:

```text
MATE
PDA
Location
WorkType
IssueType
```

운영/정합성:

```text
Open WorkSession
Handover Candidate
Unresolved Issue
Integrity Critical
Integrity Warning
```

보안:

```text
SESSION / COOKIE
CSRF ENABLED
Demo Scenario 상태
```

---

## 8. READY 기준

`READY`는 다음 조건으로만 판단한다.

```text
활성 MATE > 0
활성 PDA > 0
활성 Location > 0
활성 WorkType > 0
활성 IssueType > 0
Integrity Critical == 0
```

다음 항목은 READY를 막지 않는다.

```text
Open WorkSession
인수인계 후보
미해결 특이사항
```

이들은 실제 운영 중 정상적으로 존재할 수 있기 때문이다.

시스템이 "깨끗한 데이터만 READY"라고 정의해
실제 운영 상태를 오류로 오판하지 않는다.

---

## 9. Demo Scenario와의 관계

Demo Scenario가 OFF이고 Master Data가 비어 있다면
Readiness 화면은 부족한 Master를 Blocker로 보여준다.

Demo Scenario를 빈 DB에서 활성화하면
기본 Master/운영 데이터가 생성되어
시연 진입 준비를 빠르게 확인할 수 있다.

Demo Scenario 자체가 ON이라는 이유만으로 READY가 되지는 않는다.

항상 실제 DB 상태와 Integrity Scan 결과를 사용한다.

---

## 10. Build 검증과 구분

Runtime Readiness는:

```text
업무 데이터
정합성
보안 설정
```

을 확인한다.

다음은 별도 Build 검증이다.

```text
Gradle compile/test
Vite TypeScript/build
PostgreSQL 실제 실행
```

현재 작업환경에서 Gradle Wrapper/node_modules가 없으면
Readiness가 이를 "빌드 성공"으로 가장하지 않는다.

실제 제출 전 별도 Release Checklist를 수행한다.
