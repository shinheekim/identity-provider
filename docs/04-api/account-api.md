# Account API 설계

## 1. 개요
서버에서 제공하는 Account API의 목적과 역할을 정의하며  
Account API는 사용자 계정 상태 조회 및 계정 탈퇴 기능을 담당한다.

Account API는 인증된 사용자를 대상으로 하며, 모든 요청은 유효한 Access Token을 필요로 한다.

---

## 2. 범위 

Account API는 다음 기능을 포함한다.

- 계정 상태 조회
- 회원 탈퇴

Account API는 계정의 상태 관리와 생명주기(탈퇴 등)를 담당하며,  
인증 자체(Auth API) 및 사용자 정보 관리(User API)와 역할을 분리한다.

---

## 3. 설계 원칙

- Account API는 인증된 사용자만 접근 가능하다
- 계정 상태는 서버에서 관리되는 값을 기준으로 한다
- 탈퇴에 대한 규정 
  - 탈퇴는 물리 삭제가 아닌 상태 변경 기반으로 처리한다
  - 단 user 데이터만 남으며 identity, password는 지우며 user의 UUID , ID 와 시간 데이터를 제외한 개인정보는 제거한다.
  - 정지를 당했을때는 탈퇴가 불가능하다.
  - 추후 추가 개발 고려건
    - 회원 이력 (탈퇴,가입)관리 
    - 해당 서버 설정값을 추가하여 soft delete/ hard delete(완전삭제), 이력 유무를 선택할 수 있도록하기.
- 탈퇴 후 재로그인 및 토큰 재발급은 불가능해야 한다
- 계정 상태는 인증 단계(Auth API)에서도 반드시 검증되어야 한다

---

## 4. API 목록

| 기능 | Method | URL | 설명 |
|------|--------|-----|------|
| 계정 상태 조회 | GET | `/api/v1/users/status` | 현재 사용자 계정 상태 조회 |
| 회원 탈퇴 | DELETE | `/api/v1/users/me` | 사용자 계정 탈퇴 처리 |

---

## 5. API별 설명

### 5.1 계정 상태 조회

### 개요
현재 로그인한 사용자의 계정 상태를 조회한다.

---

### 요청

- Method: `GET`
- URL: `/api/v1/users/status`
- 인증 필요 여부: 있음 (Access Token 필요)

---

### 요청 헤더

| 헤더 | 필수 | 설명 |
|------|------|------|
| Authorization | Y | Bearer Access Token |

---

### 처리 로직 (설계 기준)

- Access Token에서 사용자 식별 정보 추출
- 사용자 계정 상태 조회
- 계정 상태 반환

---

### 응답

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| success | Boolean | Y | 요청 성공 여부 |
| data.accountStatus | String | Y | 계정 상태 |

---

### 응답 예시

```json
{
  "success": true,
  "data": {
      "accountStatus": "ACTIVE"
    }
}
```

---

### 에러 응답 예시

#### 1. 인증 실패

```json
{
    "success": false,
    "error": {
        "code": "UNAUTHORIZED",
        "message": "인증이 필요합니다."
    }
}
```

---

### 5.2 회원 탈퇴

### 개요
현재 로그인한 사용자의 계정을 탈퇴 처리한다.

---

### 요청

- Method: `DELETE`
- URL: `/api/v1/users/me`
- 인증 필요 여부: 있음 (Access Token 필요)

---

### 요청 헤더

| 헤더 | 필수 | 설명 |
|------|------|------|
| Authorization | Y | Bearer Access Token |

---

### 요청 Body

(선택)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| reason | String | N | 탈퇴 사유 |

---

### 요청 예시

```json
{
  "reason": "서비스 이용 불편"
}
```

---

### 처리 로직 (설계 기준)

- Access Token에서 사용자 식별 정보 추출
- 계정 상태를 WITHDRAWN으로 변경
- Refresh Token 무효화
- 추가 로그인 차단
- 필요 시 탈퇴 이력 저장
---

### 응답

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| success | Boolean | Y | 요청 성공 여부 |
| data.message | String | Y | 처리 결과 메시지 |

---

### 응답 예시

```json
{
  "success": true,
  "data": {
      "message": "회원 탈퇴가 완료되었습니다."
  }
}
```

---

### 에러 응답 예시

#### 1. 인증 실패

```json
{
    "success": false,
    "error": {
        "code": "UNAUTHORIZED",
        "message": "인증이 필요합니다."
    }
}
```

#### 2. 이미 탈퇴한 계정

```json
{
    "success": false,
    "error": {
        "code": "ACCOUNT_WITHDRAWN",
        "message": "이미 탈퇴한 계정입니다."
    }
}
```

---

## 6. 고려 사항

- 탈퇴 시 데이터 보존 기간 및 정책 정의 필요
- 탈퇴 후 동일 이메일 재가입 허용 여부 결정 필요
- 탈퇴 시 소셜 계정 연동 해제 정책 필요
- 탈퇴 시 Refresh Token 및 세션 무효화 필요

---

## 7. 관련 문서

- Auth API 설계
- User API 설계
- 기능 요구사항
- User 엔티티
- 공통 에러 응답 문서