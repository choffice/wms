# Backend Ready For Web — Java 17

이 기준본은 관리자 React Web을 붙이기 직전까지의 백엔드 구현본이다.

## 환경

- Java 17
- Spring Boot 3.5.16
- Gradle
- Spring Security Session
- Spring Data JPA / Hibernate
- QueryDSL 5.1.0 (Jakarta)
- PostgreSQL
- REST API
- SSE

## 인증

서버 최초 시작 시 ADMIN 계정이 없으면 데모 관리자 계정을 1개 생성한다.

```text
ID: AD0001
PW: admin1234
```

비밀번호는 `DEMO_ADMIN_PASSWORD` 환경변수로 변경할 수 있다.

- `POST /api/auth/admin/login`
- `POST /api/auth/mate/login`
- `GET /api/auth/me`
- `POST /api/auth/logout`

MATE 로그인은 `PDA 번호 + MT 사원번호 + 비밀번호`이며,
성공 시 Spring Security Session과 PDA 사용 이력을 동시에 생성한다.

## MATE

- MT0001 형식 자동 사원번호
- 이름 / 별명
- 비활성 / 재활성
- 기본 근무스케줄
- 기간별 예외 근무시간
- 연장
- AVAILABLE / WORKING / BREAK / AWAY / OFF_DUTY
- 거소
- 상태 이력
- 수동 근무종료

## PDA

- 기기 PK와 표시번호 분리
- 표시번호 UNIQUE
- AVAILABLE / IN_USE / LOST / INSPECTION / RETIRED
- 일반 번호 수정
- 두 기기 번호 맞교환
- 사용이력 있는 기기는 삭제 대신 RETIRED
- 한 PDA = 동시에 한 MATE
- 한 MATE = 동시에 한 PDA
- 할당 / 반납 History
- 사용 중인 기기도 LOST 상태 지정 가능
- 과거 이력은 기기 PK를 참조하므로 번호 변경 후에도 같은 기기를 추적

## Location

가변 깊이 계층형.

```text
A01
A01-13
A01-13-24
A01-13-24-05
```

지원:

- A01~A20 식 최상위 구역 일괄 생성
- 하위 단계 추가
- 같은 단계 추가
- 숫자 범위 하위 단계 확장
- 문자열 순 정렬
- 층수
- FOOD / NON_FOOD
- 비식품 GENERAL / COLOR / HYGIENE / TOOLS

## Work

관리자 배정은 완수범위가 아니다.

저장:

```text
업무종류
구역
시작 로케이션
담당 MATE
```

종료 로케이션은 존재하지 않는다.

MATE가 실제 마지막 수행 위치를 등록한다.

지원:

- WorkType CRUD
- Assignment 생성
- 시작
- 진행기록
- 진행기록 정정
- 완료된 Assignment 기록 정정
- 일시정지
- 재개
- 완료
- 트레이드
- Assignment History
- WorkSession
- 한 MATE 동시 작업 1개 제한
- 일시정지 시간 통계 제외

## 작업시간 신뢰도

MATE가 작업 시작을 누를 때부터 WorkSession이 열린다.

Heartbeat가 일정 시간 끊기면 원본 시간을 임의 수정하지 않고:

```text
UNCERTAIN
```

으로 표시한다.

기본 근무 종료시각이 되면 Open WorkSession은 자동종료된다.

기간별 예외 근무시간이 기본스케줄보다 우선하고,
연장 상태에서는 자동종료를 적용하지 않는다.

## 특이사항

특이사항 구분 설정:

- 구분명
- 로케이션 필요 여부
- 상품코드 필요 여부
- 수량 필요 여부
- 코멘트 기본

수량 사용 시 저장 가능한 데이터:

- 수량
- 실재고
- MMS 재고
- 유통기한 재고
- 재고 없음

상태:

```text
UNCONFIRMED
CONFIRMED
RESOLVED
```

게시판 최초 조회 전 `viewCount == 0`이면 NEW.

게시판을 열면 반환 당시 NEW 표시를 유지하고,
응답 후 해당 목록의 조회수가 증가한다.

## 공지사항

- 표시 / 숨김
- 중요 ★
- 순서
- 작성 / 수정
- 개별 삭제
- 모두 지우기
- 최초 입력일
- 최종 수정일
- Soft Delete

## Activity Log

주요 업무 이벤트를 DB에 저장하고 최신 10건 조회.

현재 화면에 표시된 로그 ID만 전달해 삭제 가능하므로
`로그 전체 삭제` UI는 화면의 최신 로그를 비우는 방식으로 구현 가능하다.

주요 Reference는 Entity ID를 유지하므로
특이사항 로그 → 특이사항 상세처럼 연결할 수 있다.

## SSE

- `/api/admin/events`
- `/api/mate/events`

PDA / 업무 / 특이사항 / 공지 변경 이벤트를 실시간 화면 갱신에 사용할 수 있는 Hub를 구성했다.

## 보고서 / 통계

QueryDSL 동적 조회 사용.

업무시간 통계 필터:

- 기간
- MATE
- WorkType
- UNCERTAIN 포함 여부

일일 보고서:

- 해당 일자 실제 수행 업무
- MATE
- 당시 PDA
- 업무 종류
- 구역 / 시작점 / 마지막 수행점
- 실제 작업시간
- PDA 할당 / 반납
- 특이사항

공지사항은 보고서에서 제외한다.

## 관리자 Dashboard API

`GET /api/admin/dashboard`

한 번에 다음 정보를 조회할 수 있다.

- 표시 공지사항
- 미확인 특이사항
- MATE 현황 / PDA / 현재 업무
- 구역 × 업무종류별 마지막 수행 위치 / 일시 / 담당자 / 진행률
- 최신 로그 10건

구역 진행률은 해당 Assignment의 시작점부터 구역 끝까지의 활성 Leaf Location을 기준으로 계산한다.

## Web 연결 단계에서 남겨둔 항목

핵심 Backend Domain / API는 구현되어 있다.

React를 붙이면서 다음을 화면 요구에 맞춰 최종 조정한다.

1. CORS 또는 same-origin 개발 프록시
2. Session + Cookie CSRF 적용
3. 관리자 Dashboard 응답 모양 미세조정
4. 특이사항 필터 / 줄임보기 UI용 QueryDSL 조건
5. SSE 이벤트명 세분화
6. 실제 화면 기준 DTO 필드 추가/삭제
7. 시연 후 DPC 기능 확정

이 항목들은 DB 핵심구조를 바꾸는 작업이 아니라 프론트 연동 조정에 가깝다.
