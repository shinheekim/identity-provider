# M4 개발 계획

## 1. 개요

M4는 M3에서 구현한 인증 기반 기능 위에 사용자 계정 관리, 소셜 로그인, 보안 정책을 확장하는 단계이다.

- 계획 기준일: 2026-07-02
- 기간: 2026-07-01 ~ 2026-07-31
- 선행 조건: M3 로그아웃 develop 반영 완료

---

## 2. M3 종료 상태

### 완료

- 회원가입
- 일반 로그인
- JWT Access Token 발급
- Refresh Token 발급
- Refresh Token 재발급
- Refresh Token Rotation
- 로그인 이력 저장

### develop 반영 완료

- 로그아웃
  - 현재 인증 사용자 소유의 활성 Refresh Token 무효화
  - Refresh Token 소유자와 Access Token subject 일치 검증
  - Token Family current token 검증
  - 로그아웃 후 재발급 차단

---

## 3. M4 목표

- 인증 이후 사용자 정보 관리 기능을 제공한다.
- 로컬 계정의 loginId/email 분리 정책과 이메일 인증을 구현한다.
- 계정 상태와 탈퇴 정책을 인증 흐름과 연결한다.
- 카카오, 구글 소셜 로그인을 도입할 수 있는 구조를 구현한다.
- 소셜 계정 연동/해제 정책을 구현한다.
- M3에서 구현된 Refresh Token 정책과 M4 계정 기능의 연결 지점을 검증한다.

---

## 4. 작업 우선순위

### P0. M3 마무리

- 로그아웃 회귀 테스트
- 로그아웃 후 Refresh Token 재발급 실패 확인
- 로그아웃 API 문서와 Swagger 응답 확인
- 토큰 검증 API를 별도 엔드포인트로 제공할지 결정

### P1. Auth 이메일 인증

- 로컬 회원가입/로그인의 loginId와 email 책임 분리
- 회원가입 전 이메일 인증 번호 발송/확인
- 회원가입 시 email 인증 완료 여부 검증
- 이메일 미인증 사용자의 로그인 차단
- 인증 번호 저장소, TTL, 재시도 제한 기준 구현

### P2. User API

- 내 정보 조회
- 내 정보 수정
- 이메일 변경 요청 및 인증 완료 후 User.email 변경
- 비밀번호 변경
- 비밀번호 변경 후 기존 Refresh Token 유지/폐기 정책 결정

### P3. Account API

- 계정 상태 조회
- 회원 탈퇴
- 탈퇴 시 개인정보 제거 정책 적용
- 탈퇴 시 Refresh Token 무효화
- 탈퇴 계정 로그인 및 토큰 재발급 차단

### P4. Social API

- 카카오 로그인
- 구글 로그인
- 최초 소셜 로그인 시 회원가입 또는 기존 ID 연동 유도
- provider verified email 기준 기존 계정과 소셜 Identity 자동 연결
- 소셜 계정 연동
- 소셜 계정 해제
- 마지막 로그인 수단 해제 방지

### P5. 정책 정리

- 계정 상태별 로그인 제한
- 실패 사유 코드 정리
- 로그인 이력 저장 항목 점검
- 운영 감사 로그 확장 여부 검토

---

## 5. API별 개발 기준

| 영역 | API | M4 기준 |
|------|-----|------|
| Auth | `POST /api/v1/auth/email/send` | 회원가입 이메일 인증 번호 발송, TTL/재시도 제한 적용 |
| Auth | `POST /api/v1/auth/email/verify` | 회원가입 이메일 인증 확인, 인증 완료 상태 저장 |
| Auth | `POST /api/v1/auth/signup` | loginId/email 분리, 이메일 인증 완료 후 가입 허용 |
| Auth | `POST /api/v1/auth/login` | loginId 기반 로그인, 미인증 이메일 로그인 차단 |
| Auth | `POST /api/v1/auth/social/login` | 카카오/구글 소셜 로그인, provider verified email 기준 자동 연결 또는 가입/기존 ID 연동 유도 |
| Auth | `POST /api/v1/auth/logout` | develop 반영 기능 회귀 테스트, 재발급 차단 확인 |
| User | `GET /api/v1/users/me` | 본인 정보와 연결된 Identity 목록 조회 |
| User | `PATCH /api/v1/users/me` | phoneNumber 수정, email 변경 요청 등록 |
| User | `POST /api/v1/users/email/send` | 이메일 변경 인증 번호 발송 |
| User | `POST /api/v1/users/email/verify` | 인증 완료 후 User.email 갱신, LOCAL Identity.loginId 유지 |
| User | `PATCH /api/v1/users/password` | 현재 비밀번호 검증, 새 비밀번호 정책 검증, 변경 이력 저장 |
| Account | `GET /api/v1/users/status` | 현재 인증 사용자 계정 상태 조회 |
| Account | `DELETE /api/v1/users/me` | WITHDRAWN 처리, 개인정보 제거, Refresh Token 무효화 |
| Social | `POST /api/v1/social/link` | 기존 계정에 카카오/구글 Identity 연결 |
| Social | `DELETE /api/v1/social/unlink` | 소셜 Identity 해제, 마지막 로그인 수단 해제 방지 |

---

## 6. M4 제외 항목

아래 항목은 M4 필수 범위에서 제외하고 별도 결정 후 진행한다.

- Access Token 블랙리스트
- 토큰 검증 API 구현
- MFA 2차 인증
- 관리자 인증 시스템
- 알림 시스템

---

## 7. 완료 기준

- M4 대상 API가 Swagger에서 확인 가능하다.
- 주요 성공/실패 응답이 API 문서와 일치한다.
- 계정 상태가 로그인, 토큰 재발급, 탈퇴 흐름에서 일관되게 검증된다.
- 로컬 회원가입은 loginId와 email을 분리하되, email 인증 완료 상태를 기준으로 동작한다.
- 이메일 변경은 인증 완료 후 User.email에 반영되며 LOCAL Identity.loginId는 유지된다.
- 소셜 로그인과 일반 로그인 Identity가 같은 User 기준으로 관리된다.
- provider verified email이 기존 계정 이메일과 일치하면 소셜 Identity가 자동 연결되고, 일치 계정이 없으면 회원가입 또는 기존 ID 연동으로 유도된다.
- 마지막 로그인 수단 해제 방지 정책이 적용된다.
- 회원 탈퇴 후 기존 Refresh Token으로 재발급할 수 없다.
