# MATE PWA

## 개발 접속

```text
/mate/login
```

## 테스트 전 필요한 관리자 선행 데이터

최소한 다음 데이터가 있어야 MATE 흐름을 확인할 수 있습니다.

1. MATE 1명 (`MT0001`)
2. PDA 1대 (`AVAILABLE`)
3. Location
4. WorkType
5. WorkAssignment
6. 특이사항 구분(특이사항 테스트 시)

관리자 Web에서 모두 등록 가능합니다.

## 테스트 추천 순서

1. 관리자 로그인
2. MATE / PDA / Location / 업무종류 등록
3. 업무배정
4. 관리자 로그아웃
5. `/mate/login`
6. PDA 선택 + MT 로그인
7. 업무 시작
8. 진행 위치 등록
9. 일시정지
10. 재개
11. 완료
12. 특이사항 등록
13. 관리자 화면에서 기록 확인
