# Social API 설계

## 1. 개요
서버에서 제공하는 Social API의 목적과 역할을 정의하며 
Social API는 기존 사용자 계정에 소셜 계정을 연동하거나, 이미 연결된 소셜 계정을 해제하는 기능을 담당한다.

Social API는 인증된 사용자를 대상으로 하며, 모든 요청은 유효한 Access Token을 필요로 한다.

---

## 2. 범위

Social API는 다음 기능을 포함한다.

- 소셜 계정 연동
- 소셜 계정 해제

Social API는 로그인 자체를 처리하지 않으며,  
소셜 로그인(Auth API) 이후의 계정 연결 및 해제 기능을 담당한다.

---

## 3. 설계 원칙

- Social API는 인증된 사용자만 접근할 수 있다
- 하나의 소셜 계정은 하나의 사용자 계정에만 연결될 수 있다
- 동일 사용자는 여러 소셜 계정을 연동할 수 있다
- 소셜 계정 해제 시 최소 1개 이상의 로그인 수단은 유지되어야 한다
- 소셜 계정 연동 및 해제는 감사 이력 저장 정책으로 확장할 수 있다

---

## 4. API 목록

| 기능 | Method | URL | 설명 |
|------|--------|-----|------|
| 소셜 계정 연동 | POST | `/api/v1/social/link` | 기존 계정에 소셜 계정 연결 |
| 소셜 계정 해제 | DELETE | `/api/v1/social/unlink` | 연결된 소셜 계정 해제 |

---

## 5. API별 설명

### 5.1 소셜 계정 연동

### 개요
현재 로그인한 사용자 계정에 소셜 계정을 연동한다.

---

### 요청

- Method: `POST`
- URL: `/api/v1/social/link`
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
| provider | String | Y | 소셜 제공자 (`KAKAO`, `GOOGLE`) |
| providerAccessToken | String | Y | 소셜 제공자로부터 발급받은 Access Token |

---

### 요청 예시

ㅇㅇㅇjson
{
"provider": "KAKAO",
"providerAccessToken": "kakao-access-token-value"
}
ㅇㅇㅇ

---

### 처리 로직 (설계 기준)

- Access Token에서 현재 사용자 식별 정보 추출
- provider 값 검증
- 소셜 제공자 API를 호출하여 사용자 정보 조회
- provider의 고유 사용자 식별자 확인
- 이미 다른 사용자에게 연동된 계정인지 확인
- 현재 사용자 계정에 소셜 계정 연동
- 필요 시 연동 이력 저장

---

### 응답

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| success | Boolean | Y | 요청 성공 여부 |
| data.provider | String | Y | 연동된 소셜 제공자 |
| data.linked | Boolean | Y | 연동 성공 여부 |

---

### 응답 예시

```json
{
    "success": true,
    "data": {
        "provider": "KAKAO",
        "linked": true
    }
}
```

---

### 에러 응답 예시

#### 1. 지원하지 않는 provider

```json
{
    "success": false,
    "error": {
        "code": "UNSUPPORTED_SOCIAL_PROVIDER",
        "message": "지원하지 않는 소셜 로그인 제공자입니다."
    }
}
```

#### 2. 유효하지 않은 소셜 토큰

```json
{
    "success": false,
    "error": {
        "code": "INVALID_SOCIAL_TOKEN",
        "message": "유효하지 않은 소셜 토큰입니다."
    }
}
```

#### 3. 이미 다른 계정에 연동된 소셜 계정

```json
{
    "success": false,
    "error": {
        "code": "SOCIAL_ACCOUNT_ALREADY_LINKED",
        "message": "이미 다른 계정에 연동된 소셜 계정입니다."
    }
}
```

#### 4. 이미 연동된 소셜 계정

```json
{
    "success": false,
    "error": {
        "code": "SOCIAL_ACCOUNT_ALREADY_CONNECTED",
        "message": "이미 현재 계정에 연동된 소셜 계정입니다."
    }
}
```

---

### 5.2 소셜 계정 해제

### 개요
현재 로그인한 사용자 계정에 연결된 소셜 계정을 해제한다.

---

### 요청

- Method: `DELETE`
- URL: `/api/v1/social/unlink`
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
| provider | String | Y | 해제할 소셜 제공자 (`KAKAO`, `GOOGLE`) |

---

### 요청 예시

```json
{
    "provider": "KAKAO"
}
```

---

### 처리 로직 (설계 기준)

- Access Token에서 현재 사용자 식별 정보 추출
- provider 값 검증
- 현재 사용자 계정에 해당 provider가 연동되어 있는지 확인
- 마지막 로그인 수단인지 확인
- 소셜 계정 연동 해제
- 필요 시 해제 이력 저장

---

### 응답

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| success | Boolean | Y | 요청 성공 여부 |
| data.provider | String | Y | 해제된 소셜 제공자 |
| data.unlinked | Boolean | Y | 해제 성공 여부 |

---

### 응답 예시

```json
{
    "success": true,
    "data": {
        "provider": "KAKAO",
        "unlinked": true
    }
}
```

---

### 에러 응답 예시

#### 1. 지원하지 않는 provider

```json
{
    "success": false,
    "error": {
        "code": "UNSUPPORTED_SOCIAL_PROVIDER",
        "message": "지원하지 않는 소셜 로그인 제공자입니다."
    }
}
```

#### 2. 연동되지 않은 소셜 계정

```json
{
    "success": false,
    "error": {
        "code": "SOCIAL_ACCOUNT_NOT_LINKED",
        "message": "연결된 소셜 계정을 찾을 수 없습니다."
    }
}
```

#### 3. 마지막 로그인 수단 해제 불가

```json
{
    "success": false,
    "error": {
        "code": "LAST_LOGIN_METHOD_CANNOT_BE_REMOVED",
        "message": "마지막 로그인 수단은 해제할 수 없습니다."
    }
}
```

---

## 6. 고려 사항

- 소셜 계정 연동 시 동일 이메일 자동 연결 여부를 정책으로 결정해야 한다
- 소셜 계정 해제 시 일반 로그인 수단이 없는 경우 해제 제한이 필요하다
- 소셜 계정 연동 및 해제는 운영 감사 로그 대상으로 확장할 수 있다
- providerUserId, provider 값에 대한 unique 제약이 필요하다

---

## 7. 관련 문서

- Auth API 설계
- User API 설계
- Account API 설계
- Identity 엔티티
- 기능 요구사항
- 공통 에러 응답 문서