# Phase 4.4 — 후속조치 큐 / 교대 인계요약 / 관리자 인계메모

## 1. 목적

Phase 4.3까지:

- 운영관제
- 인수인계
- 마감점검
- 정합성
- 특이사항

화면이 각각 충분히 상세해졌다.

Phase 4.4는 관리자가 여러 화면을 순회하면서
`그래서 지금 뭘 먼저 확인해야 하는가`를 찾는 시간을 줄이는 단계다.

단, 업무 생산성/중요도/작업 우선순위를 자동 계산하지 않는다.

---

# 후속조치 큐

신규 화면:

```text
/action-queue
```

신규 API:

```text
GET /api/admin/action-queue
```

## 2. 운영상 처리구분

큐의 정렬은:

```text
BLOCKER
→ ATTENTION
→ HANDOVER
→ ISSUE
```

순서다.

이는 업무의 Business Priority가 아니다.

의미:

### BLOCKER

치명 정합성 오류처럼
데이터 상태부터 확인해야 하는 운영 예외.

### ATTENTION

통신 지연, UNCERTAIN Session,
통신 복귀 확인, 정합성 Warning 등.

### HANDOVER

퇴근/근무종료/통신복귀 등의 이유로
담당 인수인계를 검토할 Assignment.

### ISSUE

미확인 또는 미담당 특이사항.

---

## 3. 중복 노출 최소화

운영관제와 정합성 화면이 같은 상태를
서로 다른 관점에서 잡는 경우가 있다.

후속조치 큐에서는 Live Operations 쪽에서:

```text
HEARTBEAT_STALE
SESSION_UNCERTAIN
NETWORK_RECOVERY_REQUIRED
```

처럼 현장통신 중심 항목을 직접 노출한다.

담당/상태/PDA Usage 불일치 같은 구조적 문제는
정합성 큐를 통해 노출한다.

---

## 4. 정확한 Drilldown

각 Queue Row는 원본 화면으로 바로 이동한다.

```text
정합성
→ /integrity?keyword=...

운영 MATE
→ /operations?mateId=...

인수인계
→ /handover?assignmentId=...

특이사항
→ /issues?issueId=...
```

Operations / Integrity 화면도 Query String을 받아
해당 대상을 바로 검색하도록 보강했다.

---

# 교대 인계요약

신규 화면:

```text
/handover-overview
```

신규 API:

```text
GET /api/admin/handover-overview
```

## 5. 한 화면에 모이는 데이터

### Summary

- 미처리 활성업무
- 인수인계 검토
- 미해결 특이사항
- 미확인 특이사항
- 미담당 특이사항
- 정합성 치명/경고
- Open WorkSession
- 운영 Attention MATE

### Drilldown List

최대 30건씩:

- 미처리 Assignment
- 미해결 SpecialIssue

를 바로 원본 화면으로 이동할 수 있다.

### 최근 관리자 처리내역

ActivityLog 중:

```text
actor.role = ADMIN
```

인 최근 20건을 표시한다.

특정 `오늘`이나 근무조 시간대를 임의로 추정하지 않는다.

따라서 야간조/자정 경계 정책을 아직 잠그지 않은 현재 단계에서도
시간대 의미를 왜곡하지 않는다.

---

## 6. 자동 생성 인계문

현재 Snapshot을 기반으로:

```text
미처리 활성업무 N건 / 인수인계 검토 N건
미해결 특이사항 N건 (미확인 N / 미담당 N)
정합성 치명 N건 / 경고 N건
Open WorkSession N건 / 운영 Attention MATE N명
```

형태의 짧은 인계문을 생성한다.

Frontend의 `요약 복사` 버튼은:

- 자동 요약
- 미처리 업무 최대 10건
- 미해결 특이사항 최대 10건
- 최근 인계메모 최대 5건

을 텍스트로 Clipboard에 복사한다.

별도의 PDF/파일 생성이 아니라
메신저·문서에 바로 붙여넣기 위한 기능이다.

---

# 인계메모

신규 Entity:

```text
HandoverNote
```

필드:

```text
id
createdBy
content
createdAt
```

신규 API:

```text
GET  /api/admin/handover-notes
POST /api/admin/handover-notes
```

## 7. Append-only 메모

인계메모는 현재 단계에서 수정/삭제 기능을 두지 않는다.

의도:

```text
누가 언제 무엇을 다음 관리자에게 남겼는가
```

를 그대로 보존한다.

잘못 쓴 메모가 있다면 다음 메모로 정정 내용을 추가한다.

ActivityLog:

```text
HANDOVER_NOTE_CREATE
```

도 함께 남긴다.

최근 20개 메모를 인계요약에 표시한다.

---

## 8. 마감점검과의 연결

운영 흐름:

```text
운영관제
→ 후속조치 큐
→ 인수인계
→ 마감점검
→ 인계요약
```

은 강제 Wizard가 아니다.

각 화면에서 필요한 화면으로 바로 이동할 수 있다.

특히 마감점검에서는:

```text
후속조치
인계요약
```

으로 바로 이동한다.

---

## 9. 시간경계 정책을 건드리지 않음

Phase 4.4에서 `최근 관리자 처리내역`을 사용한 이유는
아직 다음 정책이 잠기지 않았기 때문이다.

```text
근무조가 자정을 넘는가?
교대 기준일은 달력 날짜인가?
야간조의 처리내역은 어느 날짜에 귀속되는가?
```

이번 단계는 이 질문을 임의로 결정하지 않는다.

따라서:

- 현재 Snapshot
- 최근 N건 Activity
- 작성시각 기반 HandoverNote

만 사용한다.

실제 `근무조 단위 인계보고서`를 만들 때
해당 정책을 별도로 잠근 뒤 확장한다.
