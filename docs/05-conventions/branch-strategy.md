# 브랜치 전략 (Branch Strategy)

## 1. 개요

본 문서는 프로젝트의 Git 브랜치 운영 전략을 정의한다.  
일관된 브랜치 구조를 통해 협업 효율성과 코드 안정성을 확보한다.

---

## 2. 기본 브랜치 구성

| 브랜치 | 설명 |
|--------|------|
| main | 운영(배포) 기준 브랜치 |
| develop | 통합 개발 브랜치 |

---

## 3. 보조 브랜치 규칙

보조 브랜치는 다음 규칙에 따라 생성한다.

- `feature/{기능명}`: 기능 개발
- `fix/{이슈명}`: 버그 수정
- `docs/{문서명}`: 문서 작업
- `refactor/{대상}`: 리팩터링
- `hotfix/{긴급이슈}`: 긴급 수정

---

## 4. 브랜치 네이밍 예시

- `feature/local-login`
- `feature/kakao-login`
- `feature/google-login`
- `feature/jwt-issue`
- `docs/project-init`
- `refactor/auth-service`

---

## 5. 브랜치 운영 방식

- 모든 개발은 `develop` 브랜치를 기준으로 분기한다
- 기능 개발 완료 후 Pull Request를 통해 `develop`으로 병합한다
- 배포 시점에 `develop → main`으로 병합한다
- `main` 브랜치에 직접 커밋은 금지한다

---

## 6. 권장 사항

- 브랜치는 작업 단위별로 분리한다
- 하나의 브랜치에는 하나의 목적만 담는다
- PR 단위로 작업을 나누어 관리한다
- 작업 완료 후 사용한 브랜치는 삭제를 권장한다