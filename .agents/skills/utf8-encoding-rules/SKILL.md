---
name: utf8-encoding-rules
description: Use when editing files, build settings, tests, or runtime configuration that must preserve and enforce UTF-8 encoding.
---

# UTF-8 Encoding Rules

모든 에이전트는 코드를 작성하거나 수정할 때 항상 **UTF-8 인코딩**을 강제해야 합니다. 한글 깨짐이나 인코딩 불일치 문제를 사전에 방지하기 위함입니다.

## 1. 파일 시스템 (Filesystem)
- 모든 파일(소스코드, 설정 파일, 스크립트 등)은 `UTF-8` 형식으로 읽고 써야 합니다.
- 파일 수정 시 기본 인코딩이 UTF-8로 유지되도록 주의합니다.

## 2. 빌드 환경 설정 (Gradle / Maven)
빌드 스크립트(`build.gradle` 등)를 생성하거나 수정할 때 아래와 같은 UTF-8 설정이 누락되지 않도록 강제합니다.

### Gradle 예시
```gradle
// Java 컴파일 시 UTF-8 강제
tasks.withType(JavaCompile).configureEach {
    options.encoding = "UTF-8"
}

// Javadoc 생성 시 UTF-8 강제
tasks.withType(Javadoc).configureEach {
    options.encoding = "UTF-8"
}

// Test 실행 시 UTF-8 강제
tasks.withType(Test).configureEach {
    systemProperty("file.encoding", "UTF-8")
}
```

### Kotlin 예시 (필요 시)
Kotlin 컴파일러는 기본적으로 UTF-8을 사용하지만, 명시적인 옵션이 필요할 경우 `-java-parameters` 등과 함께 인코딩 옵션을 확인합니다.

## 3. Spring Boot 설정 (application.yml 등)
HTTP 요청/응답, 메세지 리소스, 템플릿 등에서 UTF-8을 명시적으로 사용하도록 다음 속성들을 권장합니다.
```yaml
server:
  servlet:
    encoding:
      charset: UTF-8
      enabled: true
      force: true
spring:
  messages:
    encoding: UTF-8
```

## 4. 데이터베이스 및 통신
- DB 연결 URL 설정 시 파라미터로 `characterEncoding=UTF-8`을 포함합니다.
- HTTP Request/Response 통신이나 헤더 설정 시에도 `Content-Type: application/json; charset=UTF-8`을 기본으로 설정합니다.

## 5. 터미널 및 환경 변수
- CI/CD 스크립트 및 터미널 명령어 실행 시, 필요하다면 `export LANG=ko_KR.UTF-8` 또는 `export LC_ALL=C.UTF-8` 환경변수를 설정합니다.
