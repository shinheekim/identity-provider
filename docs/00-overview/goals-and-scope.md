# 목표 및 범위 (Goals & Scope)

## 1. 목표 (Goals)

본 프로젝트는 인증 기능을 독립된 서비스로 분리하여,
다양한 서비스에서 공통으로 사용할 수 있는 인증 플랫폼을 구축하는 것을 목표로 한다.

특히, Stateless한 인증 구조를 위해 JWT(Json Web Token) 기반 인증 방식을 채택한다.

---

### 1.1 기능적 목표

- 일반 로그인(ID/Password) 기능 제공
- 소셜 로그인(카카오, 구글) 통합 지원
- JWT 기반 인증 시스템 구축
- Access Token / Refresh Token 발급 및 관리
- 로그인 이력 관리
- 비밀번호 정책 관리
- 하나의 계정에 여러 소셜 계정 연동 지원

---

### 1.2 비기능적 목표

- **확장성**
  - 서버 상태에 의존하지 않는 Stateless 인증 구조
- **보안성**
  - JWT 서명 기반 검증
  - Refresh Token을 통한 재발급 구조
- **성능**
  - 인증 시 DB 조회 최소화 (토큰 기반 인증)
- **일관성**
  - 모든 서비스에서 동일한 인증 방식 제공
- **유지보수성**
  - 인증 로직 중앙화

---

## 2. 범위 (Scope)

### 2.1 포함 범위 (In Scope)

#### 인증 기능
- 로그인 / 로그아웃 처리
- JWT 발급 (Access Token, Refresh Token)
- 토큰 검증 API
- 토큰 재발급 API

#### 로그인 방식
- 일반 로그인
- 소셜 로그인 (카카오, 구글)

#### 보안 및 정책
- 비밀번호 암호화 및 정책 관리
- Refresh Token 관리 (저장 및 만료 처리)
- 로그인 이력 관리

#### 데이터 관리
- 사용자 계정 (Identity)
- 비밀번호 정보
- 로그인 이력
- Refresh Token 저장소
- 소셜 계정 연동 정보

---

### 2.2 제외 범위 (Out of Scope)

- 권한/인가(Role, Permission)
- MFA (2차 인증)
- 이메일/SMS 인증
  - 이메일인증은 추가 개발에서 추가 고려한다.
- 알림 시스템
- 서비스 도메인 로직

---

## 3. 향후 확장 고려 (Future Scope)

- 토큰 블랙리스트 (로그아웃/강제 만료 대응)
- Redis 기반 Refresh Token 관리
- SSO (Single Sign-On)
- OAuth2 Provider 역할 확장
- 관리자 인증 시스템

---

## 4. 설계 원칙 (Design Principles)

- 인증 서버는 독립 서비스로 구성한다
- 인증은 JWT 기반으로 처리한다 (Stateless)
- Access Token은 짧은 만료시간을 가진다
- Refresh Token을 통해 재발급 구조를 구성한다
- 민감 정보는 토큰에 포함하지 않는다