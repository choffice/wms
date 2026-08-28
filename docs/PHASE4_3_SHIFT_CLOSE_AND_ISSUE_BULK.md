# Phase 4.3 — 교대 마감 점검 / 특이사항 선택처리

## 1. 교대 마감 점검

신규 관리자 화면:

```text
/shift-close
```

신규 API:

```text
GET /api/admin/shift-close/preview
```

이 화면은 시스템 전체를 일괄 종료하는 기능이 아니다.

목적:

```text
교대 직전에
현재 운영상태를 한 화면에서 확인하고
후속조치가 필요한 화면으로 바로 이동
```

---

## 2. 체크리스트 구분

### BLOCKER / 필수 확인

현재 자동으로 다음 두 가지를 Blocker로 분류한다.

```text
OPEN_WORK_SESSION
INTEGRITY_CRITICAL
```

#### OPEN_WORK_SESSION

WorkSession이 열려 있으면
실제 작업시간이 계속 측정 중이므로
마감 전 확인이 필요하다.

#### INTEGRITY_CRITICAL

중복 Session, 담당자 불일치,
반납된 PDA Usage 참조 등
자동판단할 수 없는 치명 정합성 문제.

---

## 3. WARNING / 인계 확인

다음 항목은 자동 마감 차단이 아니다.

```text
UNCERTAIN_SESSION
PDA_IN_USE
HANDOVER_CANDIDATE
PENDING_ASSIGNMENT
UNCONFIRMED_ISSUE
UNASSIGNED_ISSUE
UNRESOLVED_ISSUE
```

이유:

- 사용중 PDA는 정상 근무 중일 수도 있다.
- 미처리 Assignment는 다음 교대가 이어서 수행할 수 있다.
- 미해결 특이사항은 다음 교대에 정상 인계할 수 있다.

따라서 시스템이 임의로 종료/해결하지 않고
관리자에게 `인계 확인`으로 보여준다.

---

## 4. 후속화면 바로가기

각 체크리스트 Row에서 바로 이동한다.

```text
WorkSession / PDA
→ 운영관제

정합성
→ 정합성 검사

미처리 업무
→ 인수인계

특이사항
→ 특이사항 관리
```

운영관제와 인수인계 화면에서도
`마감점검`으로 바로 이동할 수 있다.

---

## 5. readyForHandoverReview

Response에는:

```text
readyForHandoverReview
```

가 있다.

의미:

```text
BLOCKER가 0건인지
```

만 나타낸다.

이 값은 실제 마감을 강제 승인하거나
근무를 종료시키는 서버 상태가 아니다.

실제 MATE 근무종료 / PDA 반납 / WorkSession 종료는
기존 개별 운영흐름을 그대로 사용한다.

---

# 특이사항 선택처리

## 6. 최대 50건 선택처리

특이사항 관리 Grid에 Checkbox를 추가했다.

지원:

```text
선택 확인
선택 해결
선택 담당 적용
```

최대:

```text
50건
```

---

## 7. 선택 확인

신규 API:

```text
POST /api/admin/issues/bulk-confirm
```

각 Row:

```text
issueId
expectedStatus
```

현재 상태가 모두:

```text
UNCONFIRMED
```

일 때만 처리한다.

한 건이라도 다른 관리자가 먼저 상태를 바꿨다면:

```text
ISSUE_STALE_STATUS
```

로 전체 Transaction을 중단한다.

---

## 8. 선택 해결

신규 API:

```text
POST /api/admin/issues/bulk-resolve
```

모든 대상이:

```text
CONFIRMED
```

일 때만 처리한다.

기존 개별 정책:

```text
확인 → 해결
```

순서를 유지한다.

미확인 건을 일괄 해결로 건너뛰지 않는다.

---

## 9. 선택 담당 적용

신규 API:

```text
POST /api/admin/issues/bulk-responsible
```

각 Row는 화면에서 보던:

```text
expectedResponsibleMateId
```

를 함께 보낸다.

저장 직전에 실제 담당자가 달라졌으면:

```text
ISSUE_STALE_RESPONSIBLE
```

로 전체 처리를 취소한다.

공통 담당자를 선택하거나
`미담당`으로 일괄 해제할 수 있다.

사유는 선택입력이다.

---

## 10. Atomic 처리

특이사항 선택처리도 부분 성공을 허용하지 않는다.

처리 순서:

1. ID 중복검사
2. Issue ID 오름차순 PESSIMISTIC_WRITE Lock
3. 전체 상태/담당자 사전검증
4. 검증 성공 후 상태변경
5. History / ActivityLog 추가

따라서 예를 들어 20건 중 19건만 확인되고
1건이 실패하는 상태를 만들지 않는다.

---

## 11. 기존 History 유지

각 개별 Issue에는 기존:

```text
SpecialIssueHistory
```

를 그대로 추가한다.

일괄 확인:

```text
CONFIRM
```

일괄 해결:

```text
RESOLVE
```

일괄 담당변경:

```text
RESPONSIBLE_CHANGE
```

으로 기록한다.

기존 History를 삭제하거나 압축하지 않는다.

ActivityLog도 각 Issue별 기존 이벤트를 남기고,
실제 2건 이상 처리 시 Summary:

```text
ISSUE_BULK_ACTION
```

을 추가한다.

---

## 12. 개별 담당변경도 stale 방지

기존 단일 담당자 변경 API에도 optional:

```text
expectedResponsibleMateId
```

를 추가했다.

따라서 특이사항 상세를 오래 열어둔 상태에서
다른 관리자가 먼저 담당자를 변경하면
예전 화면이 최신 담당자를 다시 덮어쓰지 않는다.

---

## 13. 관리자 실수 방지 UX

선택처리 실행 전 Confirm에서:

- 선택 건수
- 부분 성공 없음
- 다른 관리자 변경 시 전체 취소

를 명시한다.

교대 마감은 별도 전역 마감 버튼이 아니라
Preview Checklist로 제공한다.

즉 이번 단계의 방향은:

```text
관리자가 판단
시스템은 누락/동시수정/부분성공을 방지
```

이다.
