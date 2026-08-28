# Phase 3.3 — ERP Operations Hardening

## UI

ERP/WMS 스타일 잠금을 더 강하게 적용했다.

- 업무배정 카드 목록 → 조회/필터 가능한 Data Grid
- 업무 진행기록 / WorkSession → 하위 상세 Grid
- MATE 선택 UI → Master Table + Detail Panel
- 작업시간 통계 Card → Table
- 설정 Master Data → 등록 / 수정 / 비활성 Table
- PDA → 기기별 사용이력 조회
- Location → 층수 / 식품구분 / 비식품 카테고리 편집
- Location → 하위단계 / 같은단계 / 하위 숫자범위 확장

## Backend

추가 API:

```text
GET  /api/admin/pdas/{deviceId}/usage-history
POST /api/admin/issue-types/{id}/deactivate
```

IssueType 수정 시 중복 이름도 검사한다.

## 현재 정책 검토 포인트

`구역 진행률 %`는 현재 다음 임시 정의를 사용한다.

```text
Assignment 시작 Location
→ 해당 Area의 마지막 활성 Leaf Location
```

사이에서 현재 마지막 수행점이 차지하는 비율.

하지만 이 프로젝트의 Assignment에는 원래 `endLocation`이 없고
'구역 전체 완수 의무'도 없다.

따라서 이 퍼센트를 실제 운영 의미로 유지할지,
아니면 `마지막 수행 위치 + 마지막 수행시각` 중심으로 보여주고
퍼센트는 참고값 또는 제거할지는 사용자 확인이 필요하다.

다음 단계에서 진행률을 활용한 추가 기능은 이 결정 전까지 확장하지 않는다.
