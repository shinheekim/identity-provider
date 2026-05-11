# Auth API 설계

## 1. 개요

- Auth API는 사용자 인증과 토큰 발급 및 관리에 대한 책임을 담당하는 기능
- JWT 기반 인증 방식을 사용하며, Access Token과 Refresh Token을 함께 사용.

---

## 2. 범위
Auth API는 다음 기능을 포함한다.

- 회원가입
- 일반 로그인
- 소셜 로그인
- 로그아웃
- 토큰 재발급
- 토큰 검증
- 이메일 인증

Auth API는 인증 자체를 담당하며, 사용자 상세 정보 관리나 계정 상태 조회 등의 기능은 별도 User API 또는 Account API에서 관리한다.

---

## 3. 설계 원칙

- 인증 관련 책임은 Auth API로 집중한다
- 로그인 성공 시 JWT를 발급한다
- Access Token은 짧은 만료 시간을 가진다
- Refresh Token은 재발급 용도로 별도 관리한다
- 로그아웃 시 Refresh Token 무효화를 기본 정책으로 한다
- 인증 API는 서비스 도메인과 분리된 공통 모듈로 제공한다

---

## 4. API 목록

| 기능 | Method | URL | 설명                  |
|------|--------|-----|---------------------|
| 회원가입 | POST   | `/api/v1/auth/signup` | 일반 사용자 회원가입         |
| 로그인 | POST   | `/api/v1/auth/login` | 일반 로그인 및 JWT 발급     |
| 소셜 로그인 | POST   | `/api/v1/auth/social/login` | 소셜 로그인 처리 및 JWT 발급  |
| 로그아웃 | POST   | `/api/v1/auth/logout` | Refresh Token 무효화   |
| 토큰 재발급 | POST   | `/api/v1/auth/token/refresh` | Access/Refresh Token 재발급 |
| 토큰 검증 | GET    | `/api/v1/auth/token/verify` | Access Token 유효성 확인 |
| 이메일 인증 번호 발송 | POST   | `/api/v1/auth/email/send` | 이메일 인증 번호 발송 (개발고려) |
| 이메일 인증 확인  | POST   | `/api/v1/auth/email/verify` | 이메일 인증 확인 (개발 고려)   |

---

## 5. API별 설명

## 5.1 회원가입

### 개요
일반 로그인 사용자를 회원가입 처리한다.  
사용자 계정(User)과 로그인 수단(Identity)을 생성한다.

---

### 요청

- Method: `POST`
- URL: `/api/v1/auth/signup`
- 인증 필요 여부: 없음

---

### 요청 Body

| 필드 | 타입 | 필수 | 설명         |
|------|------|------|------------|
| loginId | String | Y | 사용자 로그인ID  |
| email | String | Y | 사용자 이메일    |
| password | String | Y | 사용자 비밀번호   |
| phoneNumber | String | N | 사용자 전화번호   |

---

### 요청 예시
- login id 는 서비스에서 로그인을 하고자 하는 것으로 정한다.
```json
{
  "loginId" : "test@example.com",
  "email": "test@example.com",
  "password": "Password123!",
  "phoneNumber": ""
  
}
```
처리 로직 (설계 기준)
- 이메일 중복 여부 확인
- 비밀번호 정책 검증
- User 엔티티 생성
- Identity (일반 로그인) 생성
- 비밀번호 암호화 저장
- 기본 계정 상태 설정 (ACTIVE 또는 PENDING)
- 회원가입 완료 처리

### 응답

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| success | Boolean | Y | 요청 성공 여부 |
| data.userUuid | String | Y | 사용자 고유 UUID |
| data.accountStatus | String | Y | 계정 상태 |

### 응답 예시

```json
{
  "success": true,
  "data": {
    "userUuid": "550e8400-e29b-41d4-a716-446655440000",
    "accountStatus": "ACTIVE"
  }
}
```

### 에러 응답 예시
#### 1. 이메일 인증
- 인증시스템을 구축하지 않았다면 해당 에러 응답은 나오지 않는다.
- 
```json
{
  "success": false,
  "error": {
    "code": "EMAIL_NOT_VERIFIED",
    "message": "이메일 인증이 완료되지 않았습니다."
  }
}
```

#### 2.이메일 중복
- 인증시스템이 구축을 할경우 인증을 해야 중복을 안내해준다.
```json
{
  "success": false,
  "error": {
    "code": "DUPLICATE_EMAIL",
    "message": "이미 가입된 이메일입니다."
  }
}
```
#### 3. 비밀번호 문제
```json
{
  "success": false,
  "error": {
    "code": "INVALID_PASSWORD",
    "message": "비밀번호 정책을 만족하지 않습니다."
  }
}
```
#### 4. 값 누락 에러 
```json
{
  "success": false,
  "error": {
    "code": "INVALID_INPUT",
    "message": "요청 값이 올바르지 않습니다."
  }
}
```
---
## 5.2 일반 로그인

### 개요
일반 로그인 사용자를 인증하고 JWT를 발급한다.  
사용자는 `loginId`와 `password`를 이용해 로그인할 수 있다.

---

### 요청

- Method: `POST`
- URL: `/api/v1/auth/login`
- 인증 필요 여부: 없음

---

### 요청 Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| loginId | String | Y | 사용자 로그인 ID |
| password | String | Y | 사용자 비밀번호 |

---

### 요청 예시

```json
{
    "loginId": "test@example.com",
    "password": "Password123!"
}
```

---

### 처리 로직 (설계 기준)

- loginId 기준 사용자 조회
- 일반 로그인용 Identity 존재 여부 확인
- 비밀번호 검증
- 계정 상태 확인
- 이메일 인증 정책이 있는 경우 인증 여부 확인
- 로그인 성공 시 Access Token / Refresh Token 발급
- 마지막 로그인 시각 갱신
- 로그인 이력 저장

---

### 응답

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| success | Boolean | Y | 요청 성공 여부 |
| data.userUuid | String | Y | 사용자 고유 UUID |
| data.accessToken | String | Y | Access Token |
| data.refreshToken | String | Y | Refresh Token |
| data.accountStatus | String | Y | 계정 상태 |

---

### 응답 예시

```json
{
    "success": true,
    "data": {
        "userUuid": "550e8400-e29b-41d4-a716-446655440000",
        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.access-token",
        "refreshToken": "refresh-token-sample-value",
        "accountStatus": "ACTIVE"
}
}
```

---

### 에러 응답 예시

#### 1. 로그인 정보 불일치

```json
{
    "success": false,
    "error": {
        "code": "INVALID_CREDENTIALS",
        "message": "로그인 ID 또는 비밀번호가 올바르지 않습니다."
    }
}
```


#### 2. 휴면 계정

```json
{
    "success": false,
    "error": {
        "code": "ACCOUNT_DORMANT",
        "message": "휴면 계정입니다."
    }
}
```

#### 3. 이용 정지 계정

```json
{
    "success": false,
    "error": {
        "code": "ACCOUNT_SUSPENDED",
        "message": "이용이 제한된 계정입니다."
    }
}
```

#### 4. 탈퇴 계정

```json
{
    "success": false, 
    "error": {
        "code": "ACCOUNT_WITHDRAWN",
        "message": "탈퇴한 계정입니다."
    }
}
```

---

## 5.3 소셜 로그인

### 개요
외부 소셜 플랫폼을 통해 사용자를 인증하고 JWT를 발급한다.  
카카오, 구글 등의 인증 결과를 기반으로 내부 사용자 계정과 매핑한다.

---

### 요청

- Method: `POST`
- URL: `/api/v1/auth/social/login`
- 인증 필요 여부: 없음

---

### 요청 Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| provider | String | Y | 소셜 제공자 (`KAKAO`, `GOOGLE`) |
| providerAccessToken | String | Y | 소셜 Access Token |

---

### 요청 예시

```json
{
    "provider": "KAKAO",
    "providerAccessToken": "kakao-access-token-value"
}
```

---

### 처리 로직 (설계 기준)

- provider 값 검증
- 소셜 API 호출하여 사용자 정보 조회
- provider 고유 사용자 식별자 확인
- 기존 연동 계정 존재 여부 확인
- 존재 시 로그인 처리
- 미존재 시 정책에 따라 회원가입 또는 연동 처리
- 계정 상태 확인
- JWT 발급
- 로그인 이력 저장

---

### 응답

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| success | Boolean | Y | 요청 성공 여부 |
| data.userUuid | String | Y | 사용자 UUID |
| data.accessToken | String | Y | Access Token |
| data.refreshToken | String | Y | Refresh Token |
| data.accountStatus | String | Y | 계정 상태 |
| data.isNewUser | Boolean | Y | 신규 사용자 여부 |

---

### 응답 예시

```json
{
  "success": true,
  "data": {
      "userUuid": "550e8400-e29b-41d4-a716-446655440000",
      "accessToken": "access-token-value",
      "refreshToken": "refresh-token-value",
      "accountStatus": "ACTIVE",
      "isNewUser": false
  }
}
```

---

### 에러 응답 예시

#### 1. 지원하지 않는 provider 
- 지원을 소셜로그인도 선택할 수 있게 추후 제공을 할 수 있다고 가정
```json
{
  "success": false,
  "error": {
    "code": "UNSUPPORTED_SOCIAL_PROVIDER",
    "message": "지원하지 않는 소셜 로그인 제공자입니다."
  }
}
```

#### 2. 소셜 토큰 오류

```json
{
  "success": false,
  "error": {
    "code": "INVALID_SOCIAL_TOKEN",
    "message": "유효하지 않은 소셜 토큰입니다."
  }
}
```

#### 3. 계정 미연동
```json
{
  "success": false,
  "error": {
    "code": "SOCIAL_ACCOUNT_NOT_LINKED",
    "message": "연결된 계정을 찾을 수 없습니다."
  }
}
```

---

### 5.4 로그아웃

### 개요
현재 로그인한 사용자의 인증 상태를 종료한다.  
기본적으로 Refresh Token을 무효화하여 이후 토큰 재발급을 차단한다.

---

### 요청

- Method: `POST`
- URL: `/api/v1/auth/logout`
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
| refreshToken | String | Y | 무효화할 Refresh Token |

---

### 요청 예시

```json
{
  "refreshToken": "refresh-token-value"
}
```

---

### 처리 로직 (설계 기준)

- Access Token 기반 사용자 식별
- Refresh Token 유효성 검증
- Refresh Token 무효화 처리
- 재발급 차단

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
    "message": "로그아웃이 완료되었습니다."
  }
}
```

---

### 5.5 토큰 재발급

### 개요
Refresh Token을 이용하여 새로운 Access Token과 Refresh Token을 발급한다.

---

### 요청

- Method: `POST`
- URL: `/api/v1/auth/token/refresh`
- 인증 필요 여부: 없음

---

### 요청 Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| refreshToken | String | Y | 재발급에 사용할 Refresh Token |

---

### 요청 예시

```json
{
  "refreshToken": "refresh-token-value"
}
```

---

### 처리 로직 (설계 기준)

- Refresh Token 유효성 검증
- 만료 여부 확인
- 사용자 식별
- 새로운 Access Token 발급
- 필요 시 Refresh Token 재발급

---

### 응답

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| success | Boolean | Y | 요청 성공 여부 |
| data.accessToken | String | Y | 새로 발급된 Access Token |
| data.refreshToken | String | Y | 새로 발급된 Refresh Token |
| data.expiresIn | Number | Y | Access Token 만료 시간 (초) |

---

### 응답 예시

```json
{
  "success": true,
  "data": {
    "accessToken": "new-access-token",
    "refreshToken": "new-refresh-token",
    "expiresIn": 1800
  }
}
```

---

### 5.6 토큰 검증

### 개요
Access Token의 유효성을 검증한다.  
내부 서비스 또는 API Gateway에서 사용할 수 있다.

---

### 요청

- Method: `GET`
- URL: `/api/v1/auth/token/verify`
- 인증 필요 여부: 있음 (Access Token 필요)

---

### 요청 헤더

| 헤더 | 필수 | 설명 |
|------|------|------|
| Authorization | Y | Bearer Access Token |

---

### 처리 로직 (설계 기준)

- JWT 서명 검증
- 토큰 만료 여부 확인
- 사용자 식별 정보 추출

---

### 응답

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| success | Boolean | Y | 요청 성공 여부 |
| data.userUuid | String | Y | 사용자 UUID |
| data.valid | Boolean | Y | 토큰 유효 여부 |

---

### 응답 예시

```json
{
  "success": true,
  "data": {
    "userUuid": "550e8400-e29b-41d4-a716-446655440000",
    "valid": true
  }
}
```
---

## 5.7 이메일 인증 번호 발송

### 개요
사용자의 이메일로 인증 번호를 발송한다.  
회원가입 전 이메일 인증이 필요한 경우 사용된다.

---

### 요청

- Method: `POST`
- URL: `/api/v1/auth/email/send`
- 인증 필요 여부: 없음

---

### 요청 Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | String | Y | 인증할 이메일 |

---

### 요청 예시

```json
{
  "email": "test@example.com"
}
```

---

### 처리 로직 (설계 기준)

- 이메일 형식 검증
- 인증 번호 생성
- 인증 번호 저장 (TTL 포함)
- 이메일 발송

---

### 응답

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| success | Boolean | Y | 요청 성공 여부 |
| data.message | String | Y | 처리 결과 메시지 |

---

### 응답 예시
#### 성공
```json
{
  "success": true,
  "data": {
    "message": "인증 번호가 발송되었습니다."
  }
}
```
#### 실패
```json
{
  "success": false,
  "data": {
    "message": "발송에 실패하였습니다."
  }
}
```

---

## 5.8 이메일 인증 확인
### 개요
사용자가 입력한 인증 번호를 검증하여 이메일 인증을 완료한다.

---

### 요청

- Method: `POST`
- URL: `/api/v1/auth/email/verify`
- 인증 필요 여부: 없음

---

### 요청 Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | String | Y | 인증할 이메일 |
| code | String | Y | 인증 번호 |

---

### 요청 예시

```json
{
  "email": "test@example.com",
  "code": "123456"
}
```

---

### 처리 로직 (설계 기준)

- 이메일 기준 인증 정보 조회
- 인증 번호 일치 여부 확인
- 만료 여부 확인
- 인증 완료 처리 (emailVerified = true)

---

### 응답

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| success | Boolean | Y | 요청 성공 여부 |
| data.verified | Boolean | Y | 인증 성공 여부 |

---

### 응답 예시

```json
{
  "success": true,
  "data": {
    "verified": true
  }
}
```

### 에러 응답 예시

#### 1. 인증 실패
- 입력한 인증번호가 일치하지 않는 경우 발생한다.
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
- 인증번호의 유효 시간이 지난 경우 발생한다.
```json
{
  "success": false,
  "error": {
    "code": "VERIFICATION_CODE_EXPIRED",
    "message": "인증 번호가 만료되었습니다."
  }
}
```

#### 3. 이미 있는 계정
- 이메일 인증은 완료되었지만 이미 가입된 이메일인 경우 발생한다.
```json
{
  "success": false,
  "error": {
    "code": "DUPLICATE_EMAIL",
    "message": "이미 가입된 계정입니다."
  }
}
```

---

## 6. 인증 흐름 요약

### 6.1 일반 로그인 흐름

1. 사용자가 로그인 요청을 보낸다
2. 서버가 사용자 식별자와 비밀번호를 검증한다
3. 계정 상태를 확인한다
4. 인증 성공 시 Access Token과 Refresh Token을 발급한다
5. 로그인 이력과 마지막 로그인 시각을 갱신한다

---

### 6.2 토큰 재발급 흐름

1. 사용자가 Refresh Token으로 재발급 요청을 보낸다
2. 서버가 Refresh Token의 유효성을 검증한다
3. 유효한 경우 새로운 Access Token을 발급한다
4. 정책에 따라 Refresh Token도 재발급할 수 있다

---

### 6.3 로그아웃 흐름

1. 사용자가 로그아웃 요청을 보낸다
2. 서버가 Refresh Token을 무효화한다
3. 이후 해당 Refresh Token으로 재발급할 수 없게 한다

---

## 7. 고려 사항

- 회원가입 식별자를 이메일로 할지 별도 로그인 ID로 할지 결정이 필요하다
- 소셜 로그인 최초 진입 시 자동 가입 여부를 결정해야 한다
- Refresh Token 저장소를 DB로 할지 Redis로 할지 결정이 필요하다
- 로그아웃 시 Access Token까지 제어할지 정책 결정이 필요하다
- 토큰 검증 API를 외부 서비스에 직접 제공할지 내부 용도로 한정할지 결정이 필요하다

---

## 8. 관련 문서

- 프로젝트 소개
- 목표 및 범위
- 기능 요구사항
- User 엔티티
- Identity 엔티티
- JWT 인증 흐름 문서
- 공통 에러 응답 문서
