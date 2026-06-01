---
name: project-architecture
description: ImHere Server 프로젝트의 아키텍처 규칙. 변경 빈도에 따른 MVC/Hexagonal 선택 기준, 레이어별 책임 및 폴더 맵을 정의한다.
---

# Project Architecture — ImHere Server

## Architecture Strategy

비즈니스 로직의 변경 빈도와 복잡도에 따라 두 가지 아키텍처 중 하나를 선택합니다:

1. **MVC (Layered Architecture)**
   - 변경이 적고 단순한 CRUD 중심의 도메인에 적용
   - `Controller` -> `Service` -> `Repository` 구조
2. **Hexagonal Architecture (Ports and Adapters)**
   - 변경이 잦고 복잡한 비즈니스 로직이 포함된 도메인에 적용
   - 외부 의존성(DB, API)을 Interface(Port)로 분리하고 Adapter를 통해 구현

## Folder Structure (Bounded Context)

최상위 패키지는 도메인(기능)별로 분리된 **Bounded Context** (예: `user`, `notifications`, `terms`, `auth`)로 구성됩니다.
각 Bounded Context 내부의 구조는 다음과 같습니다:

```
src/main/kotlin/com/kdongsu5509/
├── {bounded_context}/
│   ├── controller/          # Controller, DTOs
│   ├── application/         # (Hexagonal의 경우) Service, Port(in/out)
│   ├── service/             # (MVC의 경우) Service 인터페이스 및 구현체
│   ├── domain/              # 순수 비즈니스 객체 (엔티티)
│   ├── exception/           # 도메인 특화 예외
│   └── repository/          # JpaEntity, Spring Data Repository, Mapper
```

## Layer Responsibilities

| Layer | Rule |
|-------|------|
| **Controller** | HTTP 요청/응답 처리, 입력 검증. `ApiResponse`로 직접 래핑하지 않고 DTO만 반환. |
| **Service (Use Case)** | 비즈니스 로직 수행, 트랜잭션 관리 (`@Transactional`). |
| **Repository / Persistence** | 데이터베이스 접근 로직. **순수 Domain 엔티티와 JPA 엔티티(`*JpaEntity`)를 엄격히 분리**하고 Mapper를 통해 변환합니다. |
| **Domain** | 순수 비즈니스 객체. `@Entity` 등 프레임워크 의존성 없이 순수 Kotlin/Java로 작성됩니다. |

## Essential Commands

```bash
./gradlew build
./gradlew test
```
