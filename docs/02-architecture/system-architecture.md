# 시스템 아키텍처

## 1. 개요

Taesin Identity Provider는 여러 서비스에서 공통으로 사용할 수 있는 독립 인증 서버이다.
인증 처리는 JWT 기반으로 수행하며, Access Token은 stateless하게 검증하고 Refresh Token은 Redis에서 상태 기반으로 관리한다.

---

## 2. 주요 구성요소

| 구성요소 | 책임 |
|------|------|
| Client | 이메일 인증, 회원가입, 로그인, 토큰 재발급, 로그아웃, 사용자/계정 API 호출 |
| Auth API | 이메일 인증, 회원가입, 로그인, 로그아웃, 토큰 재발급, 소셜 로그인 처리 |
| User API | 내 정보 조회/수정, 이메일 변경 인증, 비밀번호 변경 |
| Account API | 계정 상태 조회, 회원 탈퇴 |
| Social API | 소셜 계정 연동/해제 |
| PostgreSQL | User, Identity, Password, LoginHistory 저장 |
| Redis | Refresh Token, Token Family, Refresh Token 상태 저장 |
| External OAuth Provider | 카카오, 구글 사용자 인증 정보 제공 |

---

## 3. 인증 흐름

### 3.1 회원가입

1. Client가 회원가입용 이메일 인증 번호 발송/확인 요청을 보낸다.
2. Auth API가 이메일 형식, 중복 여부, 인증 번호, 만료 시간을 검증한다.
3. Client가 인증 완료된 이메일로 회원가입 요청을 보낸다.
4. Auth API가 loginId 중복 여부, 이메일 인증 완료 상태, password 정책을 검증한다.
5. User, LOCAL Identity, Password 정보를 PostgreSQL에 저장한다.
6. 기본 계정 상태는 `ACTIVE`, `emailVerified = true`로 생성한다.

### 3.2 일반 로그인

1. Client가 loginId와 password로 로그인 요청을 보낸다.
2. Auth API가 Identity와 Password를 검증한다.
3. 계정 상태가 `ACTIVE`인지 확인한다.
4. 이메일 인증 상태를 확인한다.
5. Access Token을 발급한다.
6. Refresh Token과 Token Family를 Redis에 저장한다.
7. 로그인 성공/실패 이력을 PostgreSQL에 저장한다.

### 3.3 토큰 재발급

1. Client가 Refresh Token으로 재발급 요청을 보낸다.
2. Auth API가 Redis에서 Refresh Token 상태를 조회한다.
3. `ACTIVE` 토큰이면 Redis 원자 처리로 기존 토큰을 `ROTATED`로 변경하고 새 토큰을 `ACTIVE`로 저장한다.
4. 새 Access Token과 Refresh Token을 응답한다.
5. 이미 `ROTATED` 상태인 토큰이 재사용되면 Token Family 단위 폐기를 수행한다.

### 3.4 로그아웃

1. Client가 Access Token과 Refresh Token으로 로그아웃 요청을 보낸다.
2. Auth API가 Access Token subject와 Refresh Token 소유자를 비교한다.
3. 현재 인증 사용자 소유의 활성 current Refresh Token이면 무효화한다.
4. Access Token 블랙리스트는 현재 범위에서 제외하며, Access Token은 만료 시점까지 유효할 수 있다.

---

## 4. 데이터 저장 기준

### 4.1 PostgreSQL

- 사용자 기본 정보
- 로그인 수단
- 비밀번호 해시 및 변경 이력
- 로그인 성공/실패 이력

### 4.2 Redis

- 이메일 인증 번호 및 회원가입용 인증 완료 상태
- Refresh Token digest key
- Refresh Token 상태
  - `ACTIVE`
  - `ROTATED`
  - `REVOKED`
- Token Family current token
- TTL 기반 토큰 정리

---

## 5. M4 확장 구조

M4에서는 아래 API 영역을 확장한다.

- Auth API
  - 회원가입 이메일 인증
  - 카카오, 구글 소셜 로그인
  - 토큰 검증 API 외부 제공 여부 결정
- User API
  - 내 정보 조회
  - 내 정보 수정
  - 이메일 변경 인증
  - 비밀번호 변경
- Account API
  - 계정 상태 조회
  - 회원 탈퇴
- Social API
  - 소셜 계정 연동
  - 소셜 계정 해제

M4 기능은 기존 User/Identity/Password 구조를 유지하면서 확장한다.

---

## 6. 보안 원칙

- Access Token에는 민감 정보를 포함하지 않는다.
- Refresh Token 원문은 저장하지 않고 digest 기반 key로 관리한다.
- Refresh Token은 Token Family와 상태값으로 관리한다.
- 로컬 계정의 loginId와 email은 분리해서 관리한다.
- 로그인 실패 시 loginId 존재 여부를 추측할 수 없도록 동일한 인증 실패 응답을 반환한다.
- 이메일 변경은 인증 완료 후 User.email에 반영하며 LOCAL Identity.loginId는 유지한다.
- 탈퇴, 정지, 휴면 등 비활성 계정은 로그인과 토큰 재발급을 차단한다.
- 별도 토큰 검증 API 제공 여부는 내부 서비스 연동 방식에 따라 결정한다.

---

## 7. 관련 문서

- [Auth API](../04-api/auth-api.md)
- [User API](../04-api/user-api.md)
- [Account API](../04-api/account-api.md)
- [Social API](../04-api/social-api.md)
- [인증 토큰 모델](./auth-token-model.md)
- [Refresh Token Rotation](./refresh-token-rotation.md)
