# Phase 3.6 — 특이사항 담당관리 / 네트워크 세션 복구

## 1. 특이사항 담당자 운영

특이사항은 기존 `responsibleMate` 필드를 실제 관리자 운영 UI에 연결한다.

관리자 특이사항 화면에서:

- 미담당 조회
- 담당 MATE 조회
- 담당자 지정
- 담당자 변경
- 담당 해제
- 변경 사유 기록

이 가능하다.

업무배정에 연결되어 MATE가 특이사항을 등록한 경우에는
기존 규칙대로 현재 Assignment 담당 MATE가 최초 담당자로 연결된다.

이후 담당 변경은 자동으로 따라가지 않고 관리자가 명시적으로 변경한다.

## 2. SpecialIssue History

신규 Entity:

```text
SpecialIssueHistory
```

저장 Action:

```text
CREATE
RESPONSIBLE_CHANGE
CONFIRM
RESOLVE
DELETE
```

보존 항목:

- 이전 담당 MATE
- 새 담당 MATE
- 처리 Account
- 사유
- 변경시각

특이사항 본문 데이터와 운영 처리 이력을 분리한다.

## 3. 특이사항 상태 전이 고정

기존 명세대로 상태 전이를 실제 Backend에서도 강제한다.

```text
UNCONFIRMED
→ CONFIRMED
→ RESOLVED
```

`UNCONFIRMED` 상태에서 바로 해결 처리할 수 없다.

관리자 화면도:

- 미확인 → `확인`
- 확인 → `요청 해결`

순서로 버튼을 노출한다.

## 4. 동시 관리자 처리 보호

특이사항 담당변경 / 확인 / 해결 / 삭제 시
`PESSIMISTIC_WRITE` Lock으로 해당 Issue Row를 잠근다.

같은 특이사항을 여러 관리자 화면에서 동시에 처리할 때
마지막 저장이 조용히 앞선 처리를 덮어쓰는 상황을 줄인다.

## 5. MATE 특이사항 데이터 무결성

MATE는 request에 임의 Assignment ID를 넣어도
본인에게 현재 배정된 업무만 특이사항에 연결할 수 있다.

Assignment와 Location을 함께 지정하면
Location이 해당 Assignment Area 내부인지 Backend에서 다시 검증한다.

프론트 Select만 믿지 않는다.

## 6. 네트워크 세션 신뢰도

기본값:

```text
Heartbeat 3분 단절
→ WorkSession qualityStatus = UNCERTAIN

Heartbeat 10분 단절
→ WorkSession NETWORK_TIMEOUT 종료
→ qualityStatus = UNCERTAIN
→ MATE status = AWAY
→ whereabouts = "통신 확인 필요"
```

환경변수:

```text
HEARTBEAT_UNCERTAIN_MINUTES
NETWORK_TIMEOUT_MINUTES
```

기본값은 3분 / 10분이며,
NETWORK_TIMEOUT은 항상 UNCERTAIN 기준보다 최소 1분 이상 길게 보정한다.

## 7. 예정 근무종료 우선

Heartbeat가 오래 끊겼더라도
이미 정상 근무 종료시각을 지난 경우에는:

```text
NETWORK_TIMEOUT
```

보다

```text
SCHEDULE_END
```

를 우선한다.

예:

```text
근무종료 18:00
마지막 Heartbeat 17:50
Scheduler 검사 18:15
```

이면 Session은 18:00에 `SCHEDULE_END`로 닫는다.

네트워크 단절 때문에 정상 근무시간을 18:15까지 늘려 기록하지 않는다.

## 8. 네트워크 복귀

NETWORK_TIMEOUT 뒤 Assignment 자체는 `IN_PROGRESS`로 유지한다.

MATE가 다시 접속하면:

```text
기존 Assignment
→ 재개
→ 새 WorkSession
→ MATE WORKING
```

으로 이어간다.

과거 UNCERTAIN Session은 삭제/수정하지 않는다.

## 9. 운영관제

운영관제에 다음을 추가했다.

- `NETWORK_RECOVERY_REQUIRED`
- 미담당 미해결 특이사항 수
- 미담당 특이사항 클릭 → 특이사항 관리 화면 필터 이동

즉 업무 중요도와 관계없이
`담당이 없는 처리대상`, `통신 복구가 필요한 MATE` 같은
객관적 운영 상태를 빠르게 찾는다.

## 10. 이벤트

자동 근무종료와 네트워크 타임아웃도
ActivityLog + SSE에 남긴다.

```text
SHIFT_AUTO_END
SESSION_TIMEOUT
```

따라서 Scheduler가 만든 상태변화도
관리자/MATE 화면이 다음 polling까지 기다리지 않고 갱신할 수 있다.
