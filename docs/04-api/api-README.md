# API 설계 요약

본 문서는 서버에서 제공할 API의 전체 구성을 요약한 설계 문서입니다.  
상세 요청/응답 스펙은 Swagger(OpenAPI)로 관리합니다.
-실행후 아래 링크 확인
[Swagger](http://localhost:8080/swagger-ui/index.html)

## 🔐 Auth API
- [Auth API](./auth-api.md)

| 기능 | Method | URL | 설명 |
|------|--------|-----|------|
| 회원가입 | POST | /api/v1/auth/signup | 일반 사용자 회원가입 |
| 로그인 | POST | /api/v1/auth/login | 일반 로그인 및 JWT 발급 |
| 소셜 로그인 | POST | /api/v1/auth/social/login | 소셜 로그인 처리 및 JWT 발급 |
| 로그아웃 | POST | /api/v1/auth/logout | Refresh Token 무효화 |
| 토큰 재발급 | POST | /api/v1/auth/token/refresh | Access Token 재발급 |
| 토큰 검증 | GET | /api/v1/auth/token/verify | Access Token 유효성 확인 |
| 이메일 인증 번호 발송 | POST   | `/api/v1/auth/email/send` | 이메일 인증 번호 발송 (개발고려) |
| 이메일 인증 확인  | POST   | `/api/v1/auth/email/verify` | 이메일 인증 확인 (개발 고려)   |

---

## 👤 User API
- [User API](./user-api.md)

| 기능 | Method | URL | 설명 |
|------|--------|-----|------|
| 내 정보 조회 | GET | /api/v1/users/me | 현재 로그인한 사용자 정보 조회 |
| 내 정보 수정 | PATCH | /api/v1/users/me | 현재 로그인한 사용자 정보 수정 |
| 비밀번호 변경 | PATCH | /api/v1/users/password | 사용자 비밀번호 변경 |


---

## 🛡 Account API
- [Account API](./account-api.md)


| 기능 | Method | URL | 설명 |
|------|--------|-----|------|
| 계정 상태 조회 | GET | /api/v1/users/status | 현재 사용자 계정 상태 조회 |
| 회원 탈퇴 | DELETE | /api/v1/users/me | 사용자 계정 탈퇴 처리 |

---

## 🔗 Social API
- [Social API](./social-api.md)

| 기능 | Method | URL | 설명 |
|------|--------|-----|------|
| 소셜 계정 연동 | POST | /api/v1/social/link | 기존 계정에 소셜 계정 연결 |
| 소셜 계정 해제 | DELETE | /api/v1/social/unlink | 연결된 소셜 계정 해제 |
