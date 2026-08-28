# Warehouse Admin Web — Phase 2

React + TypeScript + Vite 관리자 Web.

## 실행

```bash
npm install
npm run dev
```

Backend는 `localhost:8080`, Frontend는 `localhost:5173`.
Vite `/api` proxy로 Session Cookie를 공유한다.

## 화면

- `/login`
- `/` 관리자 Dashboard
- `/assignments`
- `/issues`
- `/notices`
- `/mates`
- `/reports`
- `/settings`

## UI 방향

실제 물류 현황판처럼 정보 밀도는 높게 두되, 포트폴리오 시연 시 흐름이 보이도록
관리자 메뉴를 업무배정 / 이슈 / 공지 / 인원 / 보고서 / 설정으로 분리했다.
