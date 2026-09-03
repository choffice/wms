# Warehouse Work Management Backend

입사 제출용 물류센터 MATE / PDA / 로케이션 / 업무배정 시스템.

## Stack

- Java 17
- Spring Boot 3.5.16
- Gradle
- Spring Security Session
- JPA / Hibernate
- QueryDSL
- PostgreSQL
- REST + SSE

## Start

PostgreSQL:

```sql
CREATE DATABASE warehouse;
```

기본값:

```text
DB_URL=jdbc:postgresql://localhost:5432/warehouse
DB_USERNAME=postgres
DB_PASSWORD=1234
```

IntelliJ에서 Project SDK를 Java 17로 지정한 뒤
`WarehouseBackendApplication`을 실행한다.

Health:

```text
GET http://localhost:8080/api/health
```

## Demo Admin

ADMIN이 없는 최초 DB에서 자동 생성:

```text
AD0001 / admin1234
```

`DEMO_ADMIN_PASSWORD`로 기본 암호 변경 가능.

## Hibernate

현재 Entity Definition 방식 개발:

```yaml
ddl-auto: update
```

구조 확정 후 제출 최종본에서는 `validate` 전환을 검토한다.

## Documentation

- `docs/LOCKED_SPEC_v0.1.md`
- `docs/PHASE1_ENTITY_MAP.md`
- `docs/PHASE2_BACKEND.md`
- `docs/BACKEND_READY_FOR_WEB.md`

## Next

이 기준본부터 React + TypeScript + Vite 관리자 Web을 연결한다.
