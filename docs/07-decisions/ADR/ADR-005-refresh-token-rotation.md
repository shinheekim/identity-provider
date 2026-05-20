# ADR-005 Refresh Token Rotation / Token Family

## 상태
완료.

## 배경
현재 Refresh Token 재발급은 단순한 rotation 구조를 사용한다.

- `A 사용 -> A 삭제 -> B 생성`
- 재발급 성공 시 새로운 Access Token과 Refresh Token을 반환

이 방식은 정상 흐름에서는 단순하고 빠르게 동작한다.  
다만 리뷰 과정에서 아래 두 가지 한계가 확인되었다.

- 기존 토큰 삭제와 새 토큰 생성이 분리되어 있어 중간 실패 시 원자성이 보장되지 않음
- 이미 사용된 Refresh Token이 다시 들어와도 "없는 토큰"으로만 판단되어 재사용 감지가 어려움

예를 들어 `A`를 먼저 삭제한 뒤 `B` 저장 또는 응답 반환 이전에 장애가 발생하면,
클라이언트는 기존 토큰도 잃고 새 토큰도 받지 못하는 상태가 될 수 있다.

또한 `A`를 즉시 삭제하면 이후 동일 토큰이 다시 들어왔을 때,
단순 만료/미존재와 재사용 공격을 구분하기 어렵다.

---

## 결정
후속 PR에서 Refresh Token rotation은 단순 삭제 후 재생성 방식 대신
`상태 전이 + Token Family + Redis 원자 처리` 방식으로 확장한다.

핵심 방향은 다음과 같다.

- Refresh Token은 삭제 대신 상태를 가진다
- 상태는 우선 `ACTIVE`, `ROTATED`, `REVOKED`를 사용한다
- 하나의 로그인 세션 계열은 `familyId`로 묶는다
- rotation 시 기존 토큰 `A`는 삭제하지 않고 `ROTATED`로 변경한다
- 새 토큰 `B`는 같은 `familyId` 아래 `ACTIVE`로 생성한다
- `A -> ROTATED`, `B -> ACTIVE`, family current token 변경은 Redis Lua Script 또는 Transaction으로 하나의 원자 작업으로 처리한다
  - 하지만 원자성 기술 Redis Lua Script를 기본적으로 하되 개발자에 판단하에 변경 할 수 있다.
- `ROTATED` 상태의 토큰이 다시 들어오면 단순 invalid가 아니라 재사용으로 판단한다
- 재사용이 감지되면 해당 family 전체를 `REVOKED` 처리하고 재로그인을 요구한다
- `ROTATED`,`REVOKED`는 TTL기준으로 자동 삭제 되도록 관리한다.

정리하면 흐름은 아래와 같다.

- 기존 방식: `A 사용 -> A 삭제 -> B 생성`
- 목표 방식: `A 사용 -> A를 ROTATED로 변경 -> B를 ACTIVE로 생성`

---

## 저장 예시

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

필요 시 family 기준 포인터를 함께 둔다.

```text
RTF:family1 = {
  currentToken: "TokenKey(B)",
  status: "ACTIVE"
}
```

---

## 정상 재발급 흐름

1. 클라이언트가 `refresh_A`로 재발급 요청
2. Redis에서 `RT:A` 조회
3. `status == ACTIVE` 확인
4. 새 Refresh Token `B`와 새 Access Token 정보를 준비
5. Redis Lua Script 또는 Transaction 실행
6. `RT:A`가 여전히 `ACTIVE`인지 다시 확인
7. `RT:A.status = ROTATED`
8. `RT:A.rotatedTo = TokenKey(B)` 저장
9. `RT:B.status = ACTIVE` 저장
10. family의 current token을 `B`로 갱신
11. 원자 작업 성공 시 응답 반환

핵심은 5~10 단계가 하나의 원자 작업으로 묶여야 한다는 점이다.

---

## 재사용 감지 흐름

1. 이미 한 번 사용된 `refresh_A`가 다시 요청으로 들어옴
2. Redis에서 `RT:A` 조회
3. `status == ROTATED` 확인
4. 단순 invalid가 아니라 재사용으로 판단
5. `familyId` 기준으로 관련 Refresh Token들을 `REVOKED` 처리
6. 클라이언트에 재로그인 요구

이 구조를 사용하면 "없는 토큰"과 "이미 사용된 토큰"을 구분할 수 있다.

---

## 이유

- 단순 삭제 방식보다 재발급 흐름의 의미가 명확하다
- 재발급 과정에서 기존 토큰의 이력을 보존할 수 있다
- Refresh Token 재사용 감지가 가능해진다
- Token Family 단위 폐기가 가능해져 보안 대응이 쉬워진다
- Redis 원자 처리와 결합하면 rotation 중간 장애에 대한 방어력이 높아진다

---

## 대안

### 1. 현재 방식 유지
- 장점: 구현이 가장 단순하다
- 단점: 원자성 문제와 재사용 감지 한계가 남는다

### 2. 원자성만 먼저 적용하고 Token Family는 나중에 적용
- 장점: 구현 범위를 줄일 수 있다
- 단점: 재사용 감지와 family 단위 폐기까지 한 번에 정리되지 않는다

### 3. 삭제 대신 상태 전이와 Token Family를 함께 도입
- 장점: 원자성, 재사용 방지, 재사용 감지까지 하나의 모델로 확장 가능하다
- 단점: Redis 데이터 구조와 운영 규칙이 더 복잡해진다

논의 결과 2번도 현실적인 단계적 접근으로 검토했으나,
최종적으로는 3번 방향을 목표 설계로 정리하고 실제 구현은 후속 PR에서 진행하기로 합의했다.

---

## 영향

- Refresh Token 저장 구조가 단순 `token -> userUuid`에서 확장된다
- token status와 family metadata를 함께 관리해야 한다
- Redis Lua Script 또는 Transaction 도입이 필요하다
- 재사용 감지 시 family 전체 폐기 정책이 추가된다
- 로그아웃, 기기별 세션 관리, 보안 이벤트 추적 설계와도 연결될 수 있다

---

## 이번 PR 처리 범위 (논의 PR)

이번 PR에서는 아래만 반영한다.

- 현행 구현은 유지
- 논의 결과를 ADR로 문서화
- 문서상 토큰 재발급 설계의 확장 방향을 남김

아래 항목은 후속 PR에서 진행한다.

- Redis 원자 rotation 구현
- Token Family 데이터 구조 도입
- `ACTIVE / ROTATED / REVOKED` 상태 관리
- 재사용 감지 및 family 전체 폐기 처리
