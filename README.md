# Taesin Identity Provider

인증 기능을 독립 서비스로 분리하여 여러 서비스에서 공통으로 사용할 수 있도록 설계하는 인증 서버 프로젝트입니다.

로그인, 소셜 로그인, JWT, 비밀번호 정책, 로그인 이력 같은 인증 공통 기능을 중앙화하여 신규 서비스 개발 시 반복되는 인증 구현 비용을 줄이는 것을 목표로 합니다.

## 목표

- 일반 로그인 ID/Password 기반 인증 제공
- 카카오, 구글 소셜 로그인 통합 관리
- JWT 기반 Access Token / Refresh Token 발급 및 검증
- 비밀번호 암호화 및 정책 관리
- 로그인 이력 관리
- 사용자 계정과 여러 로그인 수단 연결

## 범위

포함 범위:

- 인증 서버 구축
- 일반 로그인 및 소셜 로그인
- JWT 발급, 검증, 재발급
- 사용자 계정, 로그인 수단, 비밀번호, 로그인 이력 관리
- Refresh Token 저장 및 만료 처리

제외 범위:

- 서비스별 비즈니스 도메인 로직
- UI/Frontend 구현
- 권한/인가 Role, Permission 시스템
- MFA 2차 인증
- 알림 시스템

## 기술 스택

- Java 17
- Spring Boot 4.0.5
- Spring WebMVC
- Spring Security
- Spring Data JPA
- Spring Data Redis
- OAuth2 Client
- PostgreSQL
- JWT: Nimbus JOSE JWT
- Gradle

## 실행

테스트:

```bash
./gradlew test
```

애플리케이션 실행:

```bash
./gradlew bootRun
```

## Docker

로컬 개발 환경에서는 Docker Compose로 PostgreSQL, Redis를 실행합니다.

인프라 실행:

```bash
docker compose up -d
```

인프라 종료:

```bash
docker compose down
```

볼륨까지 함께 삭제:

```bash
docker compose down -v
```

Compose 서비스:

- PostgreSQL: `localhost:5432`, database `login`, username `root`, password `root`
- Redis: `localhost:6380`

애플리케이션은 로컬에서 Gradle로 실행합니다.

```bash
./gradlew bootRun
```

애플리케이션 Docker 이미지는 Spring Boot Gradle 플러그인의 `bootBuildImage`로 생성할 수 있습니다.

이미지 빌드:

```bash
./gradlew bootBuildImage --imageName taesin-identity-provider
```

컨테이너 실행:

```bash
docker run --rm -p 8080:8080 taesin-identity-provider
```

Compose로 실행한 PostgreSQL, Redis에 애플리케이션 컨테이너에서 접근하려면 환경 변수를 함께 전달합니다.

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/login \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=root \
  -e SPRING_DATA_REDIS_HOST=host.docker.internal \
  -e SPRING_DATA_REDIS_PORT=6380 \
  taesin-identity-provider
```

## 문서

- [프로젝트 소개](docs/00-overview/project-introduction.md)
- [목표 및 범위](docs/00-overview/goals-and-scope.md)
- [기능 요구사항](docs/01-requirements/functional-requirements.md)
- [시스템 아키텍처](docs/02-architecture/system-architecture.md)
- [ERD](docs/03-domain/erd.md)
- [API 문서](docs/04-api/api-README.md)
- [브랜치 전략](docs/05-conventions/branch-strategy.md)
- [커밋 컨벤션](docs/05-conventions/commit-convention.md)
- [PR 컨벤션](docs/05-conventions/pr-convetion.md)
- [의사결정 기록](docs/07-decisions/decision-README.md)
- [개발 계획](docs/08-plannings/08-planning.md)

## 주요 도메인 개념

- User: 서비스 사용자 계정
- Identity: 로그인 수단과 사용자 식별 정보
- Password: 일반 로그인 비밀번호 정보 및 변경 이력
- LoginHistory: 로그인 시도와 결과 이력

## 설계 방향

- 인증 서버는 독립 서비스로 구성합니다.
- 인증 처리는 Stateless 구조를 기본으로 합니다.
- Access Token에는 민감 정보를 포함하지 않습니다.
- Refresh Token은 서버 측 저장소를 통해 관리합니다.
- 비밀번호는 평문으로 저장하지 않고 암호화하여 저장합니다.
