WMS 간소화 관리자 UI 패치

기준 저장소: https://github.com/choffice/wms.git
목적: 관리자 화면을 원래 요구 범위로 축소하고, MATE 모바일 화면과 백엔드는 유지합니다.

적용 방법
1. 현재 프로젝트를 종료하거나 npm dev를 중지합니다.
2. 이 ZIP을 wms 프로젝트 최상위 폴더에서 풉니다.
3. 같은 이름의 파일은 덮어쓰기를 허용합니다.
4. frontend 폴더에서 npm run dev를 다시 실행합니다.

변경 내용
- 왼쪽 관리자 메뉴: 현황판 / 공지사항 / 설정 3개만 표시
- 현황판: 공지, 특이사항, MATE 현황, 업무 진행도, 최근 작업 로그 중심
- 업무배정: MATE + 업무 + 구역 + 시작점만 선택하는 간단 화면으로 교체
- 특이사항: 미확인/확인/해결 및 삭제 중심의 간단 화면으로 교체
- 설정: MATE·근무스케줄 / 로케이션 / PDA / 업무 종류 / 특이사항 구분 5개 탭으로 정리
- MATE 모바일(/mate/*): 기존 화면 그대로 유지
- 백엔드와 PostgreSQL: 변경 없음
- 기존 복잡한 관리자 페이지 소스: 삭제하지 않음. 앱 메뉴와 라우트에서만 제외

수정/추가 파일
frontend/src/App.tsx
frontend/src/components/AppShell.tsx
frontend/src/pages/DashboardPage.tsx
frontend/src/pages/SimpleAssignmentsPage.tsx
frontend/src/pages/SimpleIssuesPage.tsx
frontend/src/pages/SimpleSettingsPage.tsx
frontend/tsconfig.app.json

참고
MATE/로케이션/업무종류/특이사항 종류의 '삭제'는 기존 백엔드 정책상 실제 기록을 지우기보다 사용중지(비활성) 처리되는 항목이 있습니다.
PDA는 기존 API 규칙에 따라 삭제 또는 폐기 처리됩니다.
