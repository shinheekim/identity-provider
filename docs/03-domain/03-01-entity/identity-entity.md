# Identity 엔티티

## 1. 개요

Identity는 사용자의 로그인 수단을 나타내는 엔티티이다.  
하나의 User는 여러 Identity를 가질 수 있으며, 각 Identity는 서로 다른 인증 방식을 의미한다.

예:
- 일반 로그인 (ID / Password)
- 카카오 로그인
- 구글 로그인

---

## 2. 테이블 정보

- 테이블명: `identity`
- 설명: 사용자 로그인 수단 및 인증 정보 관리

---

## 3. 필드 정의

### 3.1 기본 식별 정보

- **id**
    - Identity 고유 식별자
    - PK

- **user**
    - 해당 Identity가 속한 사용자
    - User와 N:1 관계

---

### 3.2 로그인 유형

- **providerType**
  - 로그인 제공자 유형
    - `LOCAL` (일반 로그인)
    - `KAKAO`
    - `GOOGLE`

```java
public enum ProviderType {
    LOCAL,    // 일반 로그인
    KAKAO,    // 카카오
    GOOGLE   // 구글
}
```
---

### 3.3 소셜 로그인 정보

- **providerUserId**
    - 소셜 로그인 제공자의 고유 사용자 ID
    - 카카오/구글에서 제공하는 식별자
    - 일반 로그인에서는 null 가능

---

### 3.4 일반 로그인 정보

- **loginId**
    - 일반 로그인 시 사용하는 로그인 ID
    - User.email과 같을 필요는 없다

---

### 3.5 대표 이메일

- **principalEmail**
    - 해당 Identity 기준의 대표 이메일
    - 소셜 로그인 시 제공되는 이메일 또는 로컬 회원가입 시 입력된 이메일

---

### 3.6 연동 여부

- **linked**
    - 해당 Identity가 활성 상태로 연동되어 있는지 여부
    - Y/N 형태로 저장

---

### 3.7 인증 정보

- **passwords**
    - 비밀번호 이력 정보
    - Identity와 1:N 관계
    - 일반 로그인(LOCAL)에서만 사용

---

### 3.8 공통 필드

- **createdAt**
    - 생성 일시

- **updatedAt**
    - 수정 일시

---

## 4. 연관 관계

### User - Identity (N:1)

- 하나의 User는 여러 Identity를 가질 수 있다
- Identity는 반드시 하나의 User에 속한다

---

### Identity - Password (1:N)

- 하나의 Identity는 여러 Password를 가질 수 있다
- 비밀번호 변경 이력 관리 가능

---

## 5. 설계 의도

- 로그인 수단을 User와 분리하여 확장성 확보
- 소셜 로그인과 일반 로그인을 동일 구조로 관리
- 하나의 계정에 여러 로그인 수단을 연결 가능
- 비밀번호 관리 책임을 Identity 단위로 분리

---

## 6. 주요 시나리오

### 6.1 일반 로그인

- providerType = LOCAL
- loginId 존재
- password 존재

---

### 6.2 소셜 로그인

- providerType = KAKAO / GOOGLE
- providerUserId 존재
- password 없음

---

### 6.3 소셜 계정 연동

- 기존 User에 새로운 Identity 추가
- linked = true

---

## 7. 고려 사항

- (providerType, providerUserId)는 unique 제약 필요
- LOCAL loginId는 unique 정책 필요
- User.email 변경 시 LOCAL loginId는 변경하지 않는다
- 소셜 로그인과 일반 로그인 혼합 시 충돌 방지 정책 필요
- linked = false 상태 처리 정책 정의 필요

---

## 8. 향후 확장

- 소셜 provider 추가 (NAVER, APPLE 등)
- Identity별 로그인 제한 정책
