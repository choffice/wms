# Java 17 환경 수정사항

프로젝트 실행 환경을 Java 17로 확정했습니다.

수정 내용:
- Gradle Java Toolchain: 21 → 17
- README 실행환경: Java 17
- 잠금 명세의 Java 버전: Java 17

유지되는 구성:
- Spring Boot 3.5.16
- Gradle
- Spring Security
- Spring Data JPA / Hibernate
- QueryDSL 5.1.0 (Jakarta)
- PostgreSQL
- React / TypeScript / Vite / PWA

현재 Phase 1 소스에서 사용한 Java 문법은 Java 17에서 호환됩니다.
