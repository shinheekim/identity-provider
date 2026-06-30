# API 설계 요약

본 문서는 서버에서 제공할 API의 전체 구성을 요약한 설계 문서입니다.  
상세 요청/응답 스펙은 Swagger(OpenAPI)로 관리합니다.
- 실행 후 아래 링크 확인
[Swagger](http://localhost:8080/swagger-ui/index.html)

## 🔐 Auth API
- [Auth API](./auth-api.md)

| 기능 | Method | URL | 설명 | 상태 |
|------|--------|-----|------|------|
| 회원가입 | POST | `/api/v1/auth/signup` | 일반 사용자 회원가입 | M3 완료 |
| 로그인 | POST | `/api/v1/auth/login` | 일반 로그인 및 JWT 발급 | M3 완료 |
| 로그아웃 | POST | `/api/v1/auth/logout` | Refresh Token 무효화 | M3 완료 |
| 토큰 재발급 | POST | `/api/v1/auth/token/refresh` | Access/Refresh Token 재발급 | M3 완료 |
| 소셜 로그인 | POST | `/api/v1/auth/social/login` | 소셜 로그인 처리 및 JWT 발급 | M4 예정 |
| 토큰 검증 | GET | `/api/v1/auth/token/verify` | Access Token 유효성 확인 | 별도 결정 |
| 이메일 인증 번호 발송 | POST | `/api/v1/auth/email/send` | 이메일 인증 번호 발송 | 별도 결정 |
| 이메일 인증 확인 | POST | `/api/v1/auth/email/verify` | 이메일 인증 확인 | 별도 결정 |

---

## 👤 User API
- [User API](./user-api.md)

| 기능 | Method | URL | 설명 | 상태 |
|------|--------|-----|------|------|
| 내 정보 조회 | GET | `/api/v1/users/me` | 현재 로그인한 사용자 정보 조회 | M4 예정 |
| 내 정보 수정 | PATCH | `/api/v1/users/me` | 현재 로그인한 사용자 정보 수정 | M4 예정 |
| 비밀번호 변경 | PATCH | `/api/v1/users/password` | 사용자 비밀번호 변경 | M4 예정 |


---

## 🛡 Account API
- [Account API](./account-api.md)


| 기능 | Method | URL | 설명 | 상태 |
|------|--------|-----|------|------|
| 계정 상태 조회 | GET | `/api/v1/users/status` | 현재 사용자 계정 상태 조회 | M4 예정 |
| 회원 탈퇴 | DELETE | `/api/v1/users/me` | 사용자 계정 탈퇴 처리 | M4 예정 |

---

## 🔗 Social API
- [Social API](./social-api.md)

| 기능 | Method | URL | 설명 | 상태 |
|------|--------|-----|------|------|
| 소셜 계정 연동 | POST | `/api/v1/social/link` | 기존 계정에 소셜 계정 연결 | M4 예정 |
| 소셜 계정 해제 | DELETE | `/api/v1/social/unlink` | 연결된 소셜 계정 해제 | M4 예정 |

---

## M4 API 완료 기준

| 영역 | 완료 기준 |
|------|------|
| Auth API | 로그아웃 회귀 테스트 완료, 소셜 로그인 기본 흐름 구현 |
| User API | 내 정보 조회/수정, 비밀번호 변경 API가 인증 사용자 기준으로 동작 |
| Account API | 계정 상태 조회, 회원 탈퇴, 탈퇴 후 로그인/재발급 차단 동작 |
| Social API | 소셜 계정 연동/해제, 마지막 로그인 수단 해제 방지 동작 |
| 별도 결정 | 이메일 인증, Access Token 블랙리스트, 토큰 검증 API 외부 제공 여부 |
