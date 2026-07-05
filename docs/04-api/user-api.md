# User API 설계

## 1. 개요
서버에서 제공하는 User API의 목적과 역할을 정의하며 
User API는 현재 로그인한 사용자의 정보 조회 및 수정, 비밀번호 변경 기능을 담당한다.

User API는 인증이 완료된 사용자를 대상으로 하며, 모든 요청은 유효한 Access Token을 필요로 한다.

---

## 2. 범위

User API는 다음 기능을 포함한다.

- 내 정보 조회
- 내 정보 수정
- 이메일 변경 인증
- 비밀번호 변경

현재 User API는 M4 개발 예정 범위이다.

User API는 인증 이후의 사용자 정보 관리 기능을 담당하며, 회원가입, 로그인, 로그아웃, 토큰 재발급 등 인증 자체에 대한 책임은 Auth API에서 관리한다.

---

## 3. 설계 원칙

- User API는 인증된 사용자만 접근할 수 있다
- 사용자 본인의 정보만 조회 및 수정할 수 있다
- 비밀번호 변경 시 기존 비밀번호 검증이 필요하다
- 사용자 정보 수정과 인증 정보 변경은 책임을 분리한다
- 민감한 정보 변경 시 추가 검증 정책을 확장할 수 있다

---

## 4. API 목록

| 기능 | Method | URL | 설명 | 상태 |
|------|--------|-----|------|------|
| 내 정보 조회 | GET | `/api/v1/users/me` | 현재 로그인한 사용자 정보 조회 | M4 예정 |
| 내 정보 수정 | PATCH | `/api/v1/users/me` | 현재 로그인한 사용자 정보 수정 | M4 예정 |
| 이메일 변경 인증 번호 발송 | POST | `/api/v1/users/email/send` | 이메일 변경 인증 번호 발송 | M4 예정 |
| 이메일 변경 인증 확인 | POST | `/api/v1/users/email/verify` | 인증 완료 후 User.email 변경 | M4 예정 |
| 비밀번호 변경 | PATCH | `/api/v1/users/password` | 사용자 비밀번호 변경 | M4 예정 |

---

## 4.1 M4 구현 기준

| 기능 | 완료 기준 |
|------|------|
| 내 정보 조회 | Access Token subject 기준으로 본인 User와 연결된 Identity 목록을 반환한다 |
| 내 정보 수정 | phoneNumber는 즉시 수정하고, email은 변경 요청 상태로 등록한다 |
| 이메일 변경 인증 | 인증 완료 후 User.email, User.emailVerified를 갱신하고 LOCAL Identity.loginId는 유지한다 |
| 비밀번호 변경 | 현재 비밀번호 검증, 새 비밀번호 정책 검증, 이전 비밀번호 재사용 방지를 수행한다 |

공통 기준:

- 모든 요청은 인증된 사용자만 접근할 수 있다
- `WITHDRAWN`, `SUSPENDED`, `DORMANT` 등 비정상 계정 처리 기준은 Account/Auth 정책과 일치해야 한다
- loginId는 로그인 식별자이며 email과 분리된다
- 비밀번호 변경 후 Refresh Token 유지/폐기 정책은 M4에서 결정한다

---

## 5. API별 설명

### 5.1 내 정보 조회

### 개요
현재 로그인한 사용자의 정보를 조회한다.
소셜 로그인 사용자의 경우, 연결된 소셜 계정 정보도 함께 조회할 수 있다.

---

### 요청

- Method: `GET`
- URL: `/api/v1/users/me`
- 인증 필요 여부: 있음 (Access Token 필요)

---

### 처리 로직 (설계 기준)

- Access Token에서 사용자 식별 정보 추출
- 사용자 기본 정보 조회
- 연결된 Identity 목록 조회
- 소셜 로그인 계정 여부 확인
- 소셜 계정 정보 포함하여 응답

---

### 응답 (확장)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| success | Boolean | Y | 요청 성공 여부 |
| data.userUuid | String | Y | 사용자 UUID |
| data.loginId | String | Y | 로그인 ID |
| data.email | String | N | 이메일 |
| data.emailVerified | Boolean | Y | 이메일 인증 여부 |
| data.phoneNumber | String | N | 전화번호 |
| data.phoneVerified | Boolean | Y | 전화번호 인증 여부 |
| data.accountStatus | String | Y | 계정 상태 |
| data.lastLoginAt | String | N | 마지막 로그인 일시 |
| data.identities | Array | Y | 로그인 수단 목록 |

---

### identities 구조

| 필드 | 타입 | 설명 |
|------|------|------|
| provider | String | 로그인 제공자 (`LOCAL`, `KAKAO`, `GOOGLE`) |
| providerUserId | String | 소셜 고유 ID (LOCAL은 null 가능) |

---

### 응답 예시 (소셜 포함)

```json
{
  "success": true,
  "data": {
    "userUuid": "550e8400-e29b-41d4-a716-446655440000",
    "loginId": "test@example.com",
    "email": "test@example.com",
    "emailVerified": true,
    "phoneNumber": "01012345678",
    "phoneVerified": false,
    "accountStatus": "ACTIVE",
    "lastLoginAt": "2026-04-05T10:30:00",
    "identities": [
        {
          "provider": "LOCAL",
          "providerUserId": null
        },
        {
          "provider": "KAKAO",
          "providerUserId": "1234567890"
        }
     ]
  }
}
```
---

### 에러 응답 예시

#### 실패 공통

```json
{
  "success": false,
  "error": {
    "code": "DATA_NOT_FOUND",
    "message": "데이터 찾을 수 없습니다."
  }
}
```

---

### 5.2 내 정보 수정

### 개요
현재 로그인한 사용자의 기본 정보를 수정한다.

- phoneNumber는 즉시 수정한다.
- email은 이메일 변경 요청으로 처리하며, 인증 완료 전까지 기존 User.email은 유지한다.
- 이메일 변경 인증이 완료되면 User.email과 User.emailVerified를 갱신하고 LOCAL Identity.loginId는 변경하지 않는다.
- 추가 개발 고려
  - 만약 소셜로 회원가입시 password가 없기 때문에 password를 설정하고 (일반로그인도 자동 생성) 변경가능하도록 


---

### 요청

- Method: `PATCH`
- URL: `/api/v1/users/me`
- 인증 필요 여부: 있음 (Access Token 필요)

---

### 요청 헤더

| 헤더 | 필수 | 설명 |
|------|------|------|
| Authorization | Y | Bearer Access Token |

---

### 요청 Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | String | N | 변경 요청할 이메일 |
| phoneNumber | String | N | 변경할 전화번호 |

---

### 요청 예시

```json
{
  "email": "new-email@example.com",
  "phoneNumber": "01087654321"
}
```

---

### 처리 로직 (설계 기준)
- Access Token에서 사용자 식별 정보 추출
- 수정 가능한 필드 검증
- phoneNumber는 사용자 정보에 즉시 반영
- email이 포함된 경우 이메일 중복 여부와 형식을 검증하고 이메일 변경 요청 상태를 저장
- 이메일 변경 인증 번호 발송/확인은 별도 API에서 처리
- 인증 완료 전까지 기존 User.email은 유지

---

### 응답

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| success | Boolean | Y | 요청 성공 여부 |
| data.userUuid | String | Y | 사용자 UUID |
| data.email | String | N | 현재 이메일 |
| data.pendingEmail | String | N | 인증 대기 중인 변경 이메일 |
| data.emailVerified | Boolean | Y | 현재 이메일 인증 여부 |
| data.phoneNumber | String | N | 변경된 전화번호 |

---

### 응답 예시

```json
{
  "success": true,
  "data": {
    "userUuid": "550e8400-e29b-41d4-a716-446655440000",
    "email": "test@example.com",
    "pendingEmail": "new-email@example.com",
    "emailVerified": true,
    "phoneNumber": "01087654321"
  }
}
```

---

### 에러 응답 예시

#### 1. 잘못된 요청

```json
{
  "success": false,
  "error": {
    "code": "INVALID_INPUT",
    "message": "요청 값이 올바르지 않습니다."
  }
}
```

#### 2. 이미 사용 중인 이메일

```json
{
  "success": false,
  "error": {
    "code": "DUPLICATE_EMAIL",
    "message": "이미 사용 중인 이메일입니다."
  }
}
```

---

### 5.3 이메일 변경 인증 번호 발송

### 개요
현재 로그인한 사용자의 이메일을 변경하기 위해 새 이메일로 인증 번호를 발송한다.

---

### 요청

- Method: `POST`
- URL: `/api/v1/users/email/send`
- 인증 필요 여부: 있음 (Access Token 필요)

---

### 요청 헤더

| 헤더 | 필수 | 설명 |
|------|------|------|
| Authorization | Y | Bearer Access Token |

---

### 요청 Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | String | Y | 변경할 새 이메일 |

---

### 요청 예시

```json
{
  "email": "new-email@example.com"
}
```

---

### 처리 로직 (설계 기준)

- Access Token에서 현재 사용자 식별 정보 추출
- 새 이메일 형식 및 중복 여부 검증
- 현재 이메일과 동일한지 검증
- 이메일 변경 요청 상태 저장 또는 갱신
- 인증 번호 생성 및 TTL 저장
- 동일 이메일 재발송 및 실패 횟수 제한 확인
- 새 이메일로 인증 번호 발송

---

### 응답 예시

```json
{
  "success": true,
  "data": {
    "message": "인증 번호가 발송되었습니다."
  }
}
```

---

### 5.4 이메일 변경 인증 확인

### 개요
새 이메일로 발송된 인증 번호를 확인하고, 인증 성공 시 User.email을 변경한다.

---

### 요청

- Method: `POST`
- URL: `/api/v1/users/email/verify`
- 인증 필요 여부: 있음 (Access Token 필요)

---

### 요청 헤더

| 헤더 | 필수 | 설명 |
|------|------|------|
| Authorization | Y | Bearer Access Token |

---

### 요청 Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | String | Y | 변경할 새 이메일 |
| code | String | Y | 인증 번호 |

---

### 요청 예시

```json
{
  "email": "new-email@example.com",
  "code": "123456"
}
```

---

### 처리 로직 (설계 기준)

- Access Token에서 현재 사용자 식별 정보 추출
- 인증 번호 일치 여부와 만료 여부 확인
- 새 이메일 중복 여부 재확인
- User.email을 새 이메일로 변경
- User.emailVerified를 true로 변경
- LOCAL Identity.loginId는 변경하지 않음
- 필요 시 이메일 변경 이력 저장

---

### 응답 예시

```json
{
  "success": true,
  "data": {
    "userUuid": "550e8400-e29b-41d4-a716-446655440000",
    "email": "new-email@example.com",
    "emailVerified": true
  }
}
```

---

### 에러 응답 예시

#### 1. 인증 번호 불일치

```json
{
  "success": false,
  "error": {
    "code": "INVALID_VERIFICATION_CODE",
    "message": "인증 번호가 올바르지 않습니다."
  }
}
```

#### 2. 인증번호 만료

```json
{
  "success": false,
  "error": {
    "code": "VERIFICATION_CODE_EXPIRED",
    "message": "인증 번호가 만료되었습니다."
  }
}
```

---

### 5.5 비밀번호 변경

### 개요
현재 로그인한 사용자의 비밀번호를 변경한다.

---

### 요청

- Method: `PATCH`
- URL: `/api/v1/users/password`
- 인증 필요 여부: 있음 (Access Token 필요)

---

### 요청 헤더

| 헤더 | 필수 | 설명 |
|------|------|------|
| Authorization | Y | Bearer Access Token |

---

### 요청 Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| currentPassword | String | Y | 현재 비밀번호 |
| newPassword | String | Y | 새 비밀번호 |

---

### 요청 예시

```json
{
  "currentPassword": "Password123!",
  "newPassword": "NewPassword123!"
}
```

---

### 처리 로직 (설계 기준)

- Access Token에서 사용자 식별 정보 추출
- 현재 비밀번호 검증
- 새 비밀번호 정책 검증
- 비밀번호 암호화 후 변경
- 필요 시 비밀번호 변경 이력 저장

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
    "message": "비밀번호가 변경되었습니다."
  }
}
```

---

### 에러 응답 예시

#### 1. 현재 비밀번호 불일치

```json
{
  "success": false,
  "error": {
    "code": "INVALID_CURRENT_PASSWORD",
    "message": "현재 비밀번호가 올바르지 않습니다."
  }
}
```

#### 2. 비밀번호 정책 위반

```json
{
  "success": false,
  "error": {
    "code": "INVALID_PASSWORD",
    "message": "비밀번호 정책을 만족하지 않습니다."
  }
}
```
#### 3. 이전 비밀번호와 동일 위반

```json
{
  "success": false,
  "error": {
    "code": "INVALID_PASSWORD",
    "message": "이전에 적용한 비밀번호입니다."
  }
}
```

---

## 6. 고려 사항

- 이메일 변경은 별도 인증 절차를 반드시 거친다
- 이메일 인증 완료 전까지 기존 User.email과 LOCAL Identity 정보는 유지한다
- 비밀번호 변경 후 기존 Refresh Token 무효화 여부를 정책적으로 결정해야 한다
- 내 정보 조회 응답에 포함할 필드는 보안 정책에 따라 제한할 수 있다
- email은 중복을 허용하지 않고, phoneNumber 중복 허용 여부는 별도 정책으로 정해야 한다
- 소셜 가입 사용자에게 로컬 비밀번호를 추가할 수 있게 할지 별도 정책이 필요하다

---

## 7. 관련 문서

- Auth API 설계
- 기능 요구사항
- User 엔티티
- Identity 엔티티
- 공통 에러 응답 문서
