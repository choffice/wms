# Release Checklist

## Backend

Java:

```text
17
```

확인:

```text
java -version
./gradlew test
./gradlew build
```

Gradle Wrapper가 없는 전달본이면
사용할 Gradle 환경에서 Wrapper를 생성하거나
IntelliJ Gradle 프로젝트로 실행한다.

확인 대상:

```text
Spring Boot start
PostgreSQL connection
ddl-auto update
QueryDSL generated Q classes
19+ unit tests
```

---

## Frontend

```text
npm install
npm run build
```

확인:

```text
TypeScript compile
Vite production build
PWA manifest/service worker
/admin routes
/mate routes
```

---

## Security

ADMIN 로그인 전:

```text
GET /api/auth/csrf
```

POST 로그인:

```text
CSRF header 포함 → 성공
CSRF header 없음 → 403
```

확인.

Role:

```text
MATE → /api/admin/** 거부
ADMIN → /api/mate/** 거부
```

확인.

HTTPS 배포 시:

```text
SESSION_COOKIE_SECURE=true
```

설정.

---

## Operational

```text
/readiness
```

확인:

```text
READY
Integrity Critical = 0
CSRF ENABLED
Master Data 존재
```

그리고:

```text
MATE 로그인
PDA 할당
업무 시작
Heartbeat
진행보고
일시정지
재개
업무 종료
PDA 반납
특이사항
인수인계
마감점검
근무조 보고서
감사로그
```

1회 End-to-End 수행.

---

## Overnight regression

테스트 시나리오:

```text
shiftDate 8/27
22:00 시작
8/28 06:00 종료
```

확인:

```text
자동종료 = 8/28 06:00
근무조 보고서 = 8/27
달력일 보고서 = 8/27 / 8/28 분리
Extension = 8/27 shiftDate 귀속
```

---

## 데이터 안전성

확인:

```text
ActivityLog 물리 삭제 Endpoint 없음
WorkProgress correction DELETE 없음
HandoverNote 수정/삭제 없음
Bulk handover 부분 성공 없음
Bulk issue action 부분 성공 없음
```

---

## 최종 제출 주의

현재 개발 설정:

```text
ddl-auto: update
SQL debug logging
Demo password default
```

은 포트폴리오 로컬 시연에는 사용할 수 있다.

실제 배포 수준으로 올릴 경우:

```text
Migration 도구
Production secret
SQL logging 조정
HTTPS
Secure Cookie
환경별 Profile
```

을 별도 적용한다.
