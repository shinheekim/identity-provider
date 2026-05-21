# Refresh Token Rotation

## 1. 개요

이 문서는 Refresh Token 재발급 흐름과 상태 전이 모델을 설명한다.

- 재발급 요청 처리 흐름
- rotation 개념
- 토큰 상태값
- Token Family 구조
- 재사용 감지
- TTL 관리 방향

의사결정 배경과 대안 비교는 [ADR-005 Refresh Token Rotation / Token Family](../07-decisions/ADR/ADR-005-refresh-token-rotation.md) 문서를 따른다.

---

## 2. 목표

Refresh Token rotation 설계의 목표는 다음과 같다.

- Access Token 재발급 지원
- Refresh Token 재사용 방지
- 이미 사용된 토큰의 재사용 감지
- 세션 계열 단위 폐기 지원
- rotation 중간 실패에 대한 방어력 강화

---

## 3. 기본 개념

### 3.1 Rotation

기존 Refresh Token을 계속 재사용하지 않고, 재발급 성공 시 새로운 Refresh Token으로 교체하는 방식이다.

예시

- 기존 토큰: `A`
- 새 토큰: `B`

정상 흐름에서는 `A`가 사용된 뒤 `B`가 다음 유효 토큰이 된다.

### 3.2 Token Family

같은 로그인 세션 계열에서 발급된 Refresh Token 묶음을 의미한다.

- 하나의 family 안에는 여러 세대의 Refresh Token이 존재할 수 있다
- 현재 유효한 토큰은 하나만 `ACTIVE` 상태를 가진다
- 재사용 감지 시 family 전체를 폐기할 수 있다
- familyid는 UUID로 구성한다.

---

## 4. 상태값

Refresh Token은 우선 아래 상태를 사용한다.

| 상태 | 의미 |
|------|------|
| `ACTIVE` | 현재 사용 가능한 토큰 |
| `ROTATED` | 이미 사용되어 다음 토큰으로 교체된 토큰 |
| `REVOKED` | 더 이상 사용할 수 없도록 폐기된 토큰 |

---

## 5. 저장 예시

```text
RT:A = {
  userUuid: "user1",
  familyId: "family1",
  status: "ROTATED",
  rotatedTo: "TokenKey(B)"
}

RT:B = {
  userUuid: "user1",
  familyId: "family1",
  status: "ACTIVE"
}
```

family 기준 포인터 예시

```text
RTF:family1 = {
  currentToken: "TokenKey(B)",
  status: "ACTIVE"
}
```

---

## 6. 정상 재발급 흐름

1. 클라이언트가 `refresh_A`로 재발급 요청
2. 서버가 `RT:A`를 조회
3. `status == ACTIVE` 확인
4. 새 Refresh Token `B`와 새 Access Token 발급 정보를 준비
5. Redis Lua Script 또는 Transaction으로 원자 작업 수행
6. `RT:A`가 여전히 `ACTIVE`인지 다시 검증
7. `RT:A.status = ROTATED`
8. `RT:A.rotatedTo = TokenKey(B)` 저장
9. `RT:B.status = ACTIVE` 저장
10. family current token을 `B`로 갱신
11. 원자 작업 성공 시 응답 반환

핵심은 5~10 단계를 분리하지 않고 하나의 작업으로 묶는 것이다.

---

## 7. 재사용 감지 흐름

1. 이미 한 번 사용된 `refresh_A`가 다시 들어옴
2. 서버가 `RT:A`를 조회
3. `status == ROTATED` 확인
4. 단순 invalid가 아니라 재사용으로 판단
5. `familyId` 기준으로 관련 토큰을 `REVOKED` 처리
6. 클라이언트에 재로그인 요구

이 구조를 사용하면 만료/미존재와 재사용 공격을 구분할 수 있다.

---

## 8. 원자성이 필요한 이유

단순한 아래 흐름은 중간 실패에 약하다.

- `A 사용 -> A 삭제 -> B 생성`

예를 들어 `A` 삭제 후 `B` 저장 전에 장애가 발생하면,
클라이언트는 기존 토큰도 잃고 새 토큰도 받지 못한다.

따라서 아래 작업은 한 번에 처리되어야 한다.

- `A ACTIVE 확인`
- `A -> ROTATED`
- `B -> ACTIVE`
- family current token 변경

---

## 9. TTL 관리 방향

- `ACTIVE` 토큰은 refresh token 만료 시간 기준 TTL을 가진다
- `ROTATED` 토큰도 재사용 감지를 위해 일정 기간 유지한다
- `REVOKED` 토큰 역시 만료 시간 또는 운영 정책 기준 TTL을 둘 수 있다
- TTL을 사용하면 Redis에서 자동 정리되므로 무한 적재를 피할 수 있다

---

## 10. 문서 범위

이 문서는 목표 구조와 동작 흐름을 설명한다.  
실제 구현 여부와 시점은 PR 범위에 따라 달라질 수 있다.

현재/후속 PR 범위와 대안 비교는 ADR 문서를 기준으로 본다.

---

## 11. 관련 문서

- [Auth API](../04-api/auth-api.md)
- [인증 토큰 모델](./auth-token-model.md)
- [ADR-005 Refresh Token Rotation / Token Family](../07-decisions/ADR/ADR-005-refresh-token-rotation.md)
