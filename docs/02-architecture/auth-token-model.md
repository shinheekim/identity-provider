# 인증 토큰 모델

## 1. 개요

이 문서는 인증 서버에서 사용하는 토큰의 역할과 필드 의미를 정리한다.

- Access Token
- Refresh Token
- 토큰 만료 시간 표현
- 클라이언트가 알아야 할 기본 사용 규칙

API 요청/응답 자체는 [Auth API](../04-api/auth-api.md) 문서를 따른다.  
Refresh Token rotation 상세 흐름은 [Refresh Token Rotation](./refresh-token-rotation.md) 문서를 따른다.

---

## 2. 토큰 종류

### 2.1 Access Token

- 인증이 필요한 API 호출에 사용하는 토큰
- 수명이 짧다
- 서버는 Access Token의 서명, 만료 시간, issuer 등을 검증한다
- 일반적으로 `Authorization: Bearer <token>` 헤더에 담아 사용한다

### 2.2 Refresh Token

- Access Token이 만료되었을 때 재발급에 사용하는 토큰
- Access Token보다 수명이 길다
- 서버는 Refresh Token을 저장소에서 관리하며 재발급 정책에 따라 rotation 할 수 있다
- 재사용 감지, 세션 폐기, 로그아웃 정책과 연결된다

---

## 3. 로그인 응답 토큰 필드

로그인 성공 시 주요 응답 필드는 다음과 같다.

| 필드 | 타입 | 설명 |
|------|------|------|
| `data.accessToken` | String | 인증이 필요한 API 호출에 사용하는 JWT |
| `data.refreshToken` | String | Access Token 재발급에 사용하는 토큰 |
| `data.accountStatus` | String | 로그인한 사용자 계정 상태 |

예시

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

## 4. 토큰 재발급 응답 필드

토큰 재발급 성공 시 주요 응답 필드는 다음과 같다.

| 필드 | 타입 | 설명 |
|------|------|------|
| `data.accessToken` | String | 새로 발급된 Access Token |
| `data.refreshToken` | String | 새로 발급된 Refresh Token |
| `data.expiresIn` | Number | Access Token 만료 시간. 단위는 초 |

예시

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

## 5. JWT Claim 기준

현재 Access Token은 JWT로 발급하며 아래 정보를 기본으로 사용한다.

| 항목 | 설명 |
|------|------|
| `iss` | 토큰 발급자 |
| `sub` | 사용자 식별자. 현재는 `userUuid` 사용 |
| `jti` | 토큰 고유 식별자 |
| `iat` | 발급 시각 |
| `exp` | 만료 시각 |

세부 구현은 JWT 발급 로직을 따른다.

---

## 6. expiresIn 의미

- `expiresIn`은 Access Token의 남은 유효 시간을 초 단위로 표현한 값이다
- 절대 시각이 아니라 지속 시간이다
- 클라이언트는 이 값을 바탕으로 Access Token 갱신 시점을 판단할 수 있다

예시

- `1800` = 30분

---

## 7. 클라이언트 주의사항

- Access Token과 Refresh Token은 역할이 다르므로 분리해서 관리해야 한다
- Access Token 만료만으로 즉시 로그아웃 처리하지 않고 Refresh Token 재발급을 먼저 시도할 수 있다
- Refresh Token은 유출 시 영향이 크므로 저장 위치와 전송 방식을 신중히 선택해야 한다
- 이미 사용된 Refresh Token 재사용 시 세션 전체가 무효화될 수 있다

---

## 8. 관련 문서

- [Auth API](../04-api/auth-api.md)
- [Refresh Token Rotation](./refresh-token-rotation.md)
- [ADR-001 JWT 기반 인증 채택](../07-decisions/ADR/ADR-001-JWT.md)
- [ADR-005 Refresh Token Rotation / Token Family](../07-decisions/ADR/ADR-005-refresh-token-rotation.md)
