# Admin OTT

관리자 로그인은 비밀번호 대신 OTT(One-Time Token)와 Session을 사용합니다. 그 위에 **IP allowlist**와 **발급 rate limit** 두 겹의 방어선을 더 깔아, 관리자 콘솔의 공격 표면을 최소화합니다.

---

## 핵심 판단

| 결정 | 내용 | 근거 |
|---|---|---|
| 비밀번호 미사용 | 관리자 로그인은 OTT 발급/검증으로만 처리 | `SecurityConfig.kt:177-186` |
| IP allowlist를 admin 전 경로로 확대 | `/admin/**`, `/api/admin/**` (로그인 페이지 포함) 모두 허용 IP에서만 접근 | `OttIpValidationFilter.kt:27-37`, `SecurityConfig.kt:140,193` |
| 신뢰 IP는 X-Real-IP 우선 | XFF 첫 요소 스푸핑을 원천 차단하기 위해 추출 우선순위를 고정 | `ClientIpResolver.kt:20-35` |
| 발급 횟수 제한 | 동일 username 기준 5분 내 최대 3회, 초과 시 429 | `ImHereOttSuccessHandler.kt:37-47,52-78` |
| prod fail-closed | prod에서 `admin.allowed-ips`가 비면 startup 실패 | `SecurityConfig.kt:78-85` |
| OTT는 Discord로만 전달 | 토큰을 응답 바디가 아니라 Webhook으로 전송 | `ImHereOttSuccessHandler.kt:58-60` |

---

## 구성 요소

| 컴포넌트 | 역할 | 위치 |
|---|---|---|
| `ClientIpResolver` | 프록시 헤더에서 **신뢰 가능한** 클라이언트 IP 한 개를 뽑는다 | `auth/security/ClientIpResolver.kt` |
| `OttIpValidationFilter` | admin 체인에 도달한 모든 요청을 IP allowlist로 거른다 | `auth/security/filter/OttIpValidationFilter.kt` |
| `OttIpFilterConfig` | `admin.*` 설정(`id`, `allowed-ips`)을 바인딩하는 `@ConfigurationProperties` | `auth/security/filter/OttIpFilterConfig.kt` |
| `ImHereOttSuccessHandler` | OTT 발급 성공 시 rate limit 검사 후 Discord로 토큰 전송 | `auth/security/handler/ImHereOttSuccessHandler.kt` |
| `JdbcOneTimeTokenService` | OTT 토큰을 `one_time_tokens` 테이블에 저장/검증 | `SecurityConfig.kt:73-75` |
| `OttLoginSuccessHandler` | OTT 검증 성공 후 `/admin`으로 redirect | `auth/security/handler/OttLoginSuccessHandler.kt` |

---

## 1. 신뢰 가능한 클라이언트 IP 추출 — `ClientIpResolver`

IP allowlist는 "요청을 보낸 진짜 IP"를 정확히 알아야 의미가 있습니다. 그런데 ImHere 서버는 nginx 리버스 프록시 뒤에 있어서, 애플리케이션이 보는 TCP 소켓의 `remoteAddr`은 항상 nginx(루프백)입니다. 진짜 클라이언트 IP는 nginx가 헤더에 실어 보내는데, **어떤 헤더를 믿느냐**가 보안의 핵심입니다.

`ClientIpResolver`는 다음 우선순위로 IP 한 개를 확정합니다 (`ClientIpResolver.kt:20-35`):

| 순위 | 출처 | 신뢰 근거 |
|---|---|---|
| 1 | `X-Real-IP` 헤더 | nginx가 `proxy_set_header X-Real-IP $remote_addr`로 **항상 덮어씀** → 클라이언트가 직접 주입한 값은 무력화 |
| 2 | `X-Forwarded-For`의 **마지막** 요소 | nginx 미경유 fallback. 마지막 = 직전 프록시가 append한 실제 peer |
| 3 | `request.remoteAddr` | 헤더가 전혀 없을 때의 최종 fallback |

각 단계는 값이 비어있거나 문자열 `"unknown"`을 포함하면 다음 단계로 넘어갑니다.

```kotlin
// ClientIpResolver.kt:21-34
val realIp = request.getHeader(X_REAL_IP)
if (!realIp.isNullOrBlank() && !realIp.contains(UNKNOWN)) {
    return realIp.trim()
}
val xff = request.getHeader(X_FORWARDED_FOR)
if (!xff.isNullOrBlank() && !xff.contains(UNKNOWN)) {
    val lastHop = xff.split(",").map { it.trim() }.lastOrNull { it.isNotEmpty() }
    if (!lastHop.isNullOrEmpty()) return lastHop
}
return request.remoteAddr
```

### 왜 XFF 첫 요소를 절대 믿지 않는가

`X-Real-IP`와 `X-Forwarded-For`(이하 XFF)는 nginx가 다루는 방식이 다릅니다. 이 차이가 스푸핑 가능 여부를 가릅니다.

- **X-Real-IP는 덮어쓰기(overwrite).** nginx 설정 `proxy_set_header X-Real-IP $remote_addr`는 클라이언트가 보낸 `X-Real-IP`가 있든 없든 nginx가 본 실제 소켓 IP로 통째로 갈아끼웁니다. 그래서 공격자가 `X-Real-IP: 1.1.1.1`을 붙여도 nginx를 거치면 사라집니다.
- **XFF는 덧붙이기(append).** nginx 설정 `proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for`는 클라이언트가 보낸 XFF **뒤에** 실제 소켓 IP를 이어 붙입니다.

공격자가 `X-Forwarded-For: 9.9.9.9`(allowlist에 있는 척하는 위조값)를 붙여 보내면, nginx를 통과한 뒤 헤더는 이렇게 됩니다.

```
X-Forwarded-For: 9.9.9.9, <공격자의_진짜_IP>
```

즉 **첫 요소는 공격자가 통제**하고, **마지막 요소는 nginx가 append한 신뢰 가능한 값**입니다. 흔히 쓰는 "XFF 첫 요소 = 클라이언트 IP" 관례를 그대로 따랐다면 `9.9.9.9`를 클라이언트 IP로 오인해 allowlist를 우회당합니다. 그래서 이 프로젝트는 (1) overwrite 보장이 있는 `X-Real-IP`를 1순위로 두고, (2) XFF는 마지막 요소만, (3) 첫 요소는 절대 신뢰하지 않습니다.

> **신뢰 경계 전제:** 이 우선순위는 "외부 트래픽이 반드시 nginx를 거친다"는 토폴로지를 전제로 합니다. nginx를 우회해 애플리케이션 포트로 직접 접속하면 `X-Real-IP`를 위조할 수 있으므로, 애플리케이션 포트는 외부에 노출되지 않아야 합니다. nginx 헤더 설정의 실제 값은 [../infra/nginx.md](../infra/nginx.md)와 교차 확인하세요.

---

## 2. IP allowlist 검증 — `OttIpValidationFilter`

`OttIpValidationFilter`는 admin 보안 체인에만 등록되어, 그 체인에 도달한 **모든 요청**을 검사합니다 (`OttIpValidationFilter.kt:27-37`). 과거에는 `POST /admin/ott/request` 한 경로만 막았지만, 지금은 로그인 페이지(`/admin/login`)와 OTT 입력 페이지(`/admin/ott`)를 포함한 admin 전 경로가 allowlist 뒤에 잠깁니다. 허용되지 않은 IP는 관리자 로그인 화면조차 볼 수 없습니다.

검증 로직은 `ClientIpResolver`로 뽑은 IP가 `allowed-ips` 목록에 **정확히 일치(exact match)**하는지만 봅니다 (`OttIpValidationFilter.kt:40-44`). CIDR 대역이 아니라 단일 IP 문자열 비교입니다.

```kotlin
// OttIpValidationFilter.kt:29-37
val clientIp = ClientIpResolver.resolve(httpRequest)
if (!isIpAllowed(clientIp)) {
    httpResponse.status = HttpStatus.FORBIDDEN.value()  // 403
    httpResponse.contentType = "application/json"
    httpResponse.writer.write("""{"error": "Forbidden"}""")
    return
}
chain.doFilter(request, response)
```

차단 시 응답:

```http
HTTP/1.1 403 Forbidden
Content-Type: application/json

{"error": "Forbidden"}
```

### 두 admin 체인에 모두 등록

필터는 `addFilterBefore<UsernamePasswordAuthenticationFilter>(ottIpValidationFilter)`로 두 체인 앞단에 꽂힙니다.

| 체인 | `securityMatcher` | 인증 모델 | 필터 등록 |
|---|---|---|---|
| `adminApiFilterChain` `@Order(1)` | `/api/admin/**` | JWT + ADMIN 권한 | `SecurityConfig.kt:140` |
| `adminWebFilterChain` `@Order(2)` | `/admin/**` | OTT + 세션 | `SecurityConfig.kt:193` |

> **왜 필터에서 직접 끊는가:** Spring Security의 `authorizeHttpRequests`만으로도 경로별 인가는 됩니다. 하지만 IP 차단을 인가 단계가 아니라 **필터 최전단**에 두면, OTT 발급 로직이나 폼 렌더링 같은 뒷단 코드가 아예 실행되지 않습니다. 허용되지 않은 IP에게는 관리자 콘솔의 존재 자체(로그인 폼 HTML)를 노출하지 않는다는 의도입니다.

---

## 3. OTT 발급 rate limit — `ImHereOttSuccessHandler`

OTT가 발급될 때마다 Discord로 토큰이 흘러갑니다. 누군가 발급 요청을 반복하면 Discord 채널이 토큰으로 도배되고, 운영자가 진짜 요청과 노이즈를 구분하기 어려워집니다. 그래서 동일 username 기준으로 **5분 윈도우 내 최대 3회**만 발급을 허용합니다 (`ImHereOttSuccessHandler.kt:37-38`).

저장소는 Caffeine `Cache`이고, 윈도우 만료를 캐시의 `expireAfterWrite`에 위임합니다 (`ImHereOttSuccessHandler.kt:43-47`).

```kotlin
// ImHereOttSuccessHandler.kt:43-47
private val ottRequestTracker: Cache<String, OttRequestInfo> =
    Caffeine.newBuilder()
        .expireAfterWrite(OTT_VALIDITY_MINUTES, TimeUnit.MINUTES)  // 5분
        .maximumSize(10_000)
        .build()
```

판정 로직 (`ImHereOttSuccessHandler.kt:63-78`):

| 캐시 상태 | 동작 |
|---|---|
| 엔트리 없음 (윈도우 첫 요청 또는 만료 후) | `count=1`로 저장, 발급 허용 |
| `count >= 3` | 발급 거부 → `429 Too Many Requests` |
| `count < 3` | `count + 1`로 갱신, 발급 허용 |

만료 시각을 수동으로 비교하던 코드(과거 `ConcurrentHashMap` 구현)를 제거하고, "엔트리가 존재한다 = 아직 윈도우 안"이라는 단순한 불변식으로 정리했습니다. eviction(만료 항목 제거)과 메모리 상한(`maximumSize(10_000)`)도 Caffeine이 자동 처리합니다.

초과 시 응답:

```http
HTTP/1.1 429 Too Many Requests

{"error": "OTT 요청이 너무 많습니다. 잠시 후 다시 시도하세요."}
```

### 설계상 한계 (의도된 트레이드오프)

- **rate limit 기준은 username.** IP 기준이 아닙니다. 단일 관리자 계정(`admin.id`) 운영을 전제로 한 선택이라, 실질적으로 "이 관리자가 5분에 3번"입니다.
- **카운터는 인스턴스 로컬 in-memory.** Caffeine은 프로세스 메모리에 있으므로, 인스턴스를 재시작하면 카운터가 초기화되고 다중 인스턴스 간에는 공유되지 않습니다. 현재 admin 콘솔은 단일 인스턴스 운영이라 충분하지만, 수평 확장 시에는 Redis 등 공유 저장소가 필요합니다.

---

## 4. prod fail-closed — `ottIpFilterConfig()`

IP allowlist가 비어 있으면 `OttIpValidationFilter`의 `contains` 검사가 항상 false가 되어 모든 요청을 막거나(빈 목록), 설정 실수로 allowlist가 무력화될 위험이 있습니다. 운영 환경에서 이 설정 누락을 **조용히 통과시키지 않기 위해**, prod 프로파일에서 `admin.allowed-ips`가 비어 있으면 startup 자체를 실패시킵니다 (`SecurityConfig.kt:78-85`).

```kotlin
// SecurityConfig.kt:81-83
if (environment.activeProfiles.contains("prod") && allowedIps.none { it.isNotBlank() }) {
    throw IllegalStateException("prod 프로파일에는 admin.allowed-ips가 반드시 설정되어야 합니다.")
}
```

- 설정 키: `admin.allowed-ips` ← 환경변수 `ADMIN_ALLOWED_IPS` (`application.yaml:166`)
- 기본값: 프로파일 무관 기본은 `127.0.0.1` (`SecurityConfig.kt:51`, `@Value("\${admin.allowed-ips:127.0.0.1}")`). prod에서는 이 기본값에 의존하지 말고 환경변수로 명시해야 합니다.

> **왜 fail-open이 아니라 fail-closed인가:** 보안 설정이 누락됐을 때 "일단 떠 있고 보안만 꺼진" 상태가 가장 위험합니다. 운영자가 인지하지 못한 채 관리자 콘솔이 무방비로 노출되기 때문입니다. 차라리 배포가 실패해 즉시 눈에 띄게 만드는 쪽을 택했습니다.

---

## 5. `/admin` trailing slash 매핑

`AdminWebController`의 대시보드 매핑은 `/admin`과 `/admin/` 두 경로를 모두 받습니다 (`AdminWebController.kt:30`).

```kotlin
// AdminWebController.kt:30-31
@GetMapping("/admin", "/admin/")
fun dashboard(): String = "admin/dashboard"
```

Spring Boot 3.x부터 trailing slash 매칭이 기본 비활성이라, `@GetMapping("/admin")` 하나만 두면 `/admin/`로 접근했을 때 404가 납니다. OTT 검증 성공 후 redirect 목적지(`/admin`)와 사용자가 직접 입력하는 `/admin/`를 모두 살리기 위해 두 경로를 명시했습니다.

---

## 실제 요청 예시

OTT 발급 요청 (허용 IP에서):

```http
POST /admin/ott/request
Content-Type: application/x-www-form-urlencoded

username={admin.id}
```

성공 시 토큰은 HTTP 응답 바디가 아니라 Discord Webhook으로만 전달되고, 브라우저는 `/admin/ott?username=...`로 redirect됩니다 (`ImHereOttSuccessHandler.kt:60`).

Discord로 가는 메시지 형식 (`ImHereOttSuccessHandler.kt:28-36`):

```
### 🔐 ImHere 관리자 OTT 로그인 요청
- **요청 관리자**: `{username}`
- **요청 IP**: `{clientIp}`
- **요청 시각**: `{Instant}`
- **OTT 토큰**: `{tokenValue}`
```

---

## 코드 근거

| 항목 | 위치 |
|---|---|
| 신뢰 IP 추출 우선순위 | `src/main/kotlin/com/kdongsu5509/auth/security/ClientIpResolver.kt:20-35` |
| admin 전 경로 IP 차단 | `src/main/kotlin/com/kdongsu5509/auth/security/filter/OttIpValidationFilter.kt:27-44` |
| 필터의 두 admin 체인 등록 | `src/main/kotlin/com/kdongsu5509/auth/security/config/SecurityConfig.kt:140,193` |
| Caffeine rate limit | `src/main/kotlin/com/kdongsu5509/auth/security/handler/ImHereOttSuccessHandler.kt:37-78` |
| prod fail-closed | `src/main/kotlin/com/kdongsu5509/auth/security/config/SecurityConfig.kt:78-85` |
| `/admin`, `/admin/` 매핑 | `src/main/kotlin/com/kdongsu5509/auth/adapter/in/web/AdminWebController.kt:30` |
| allowlist 설정 키 | `src/main/resources/application.yaml:163-166` |

---

## 관련 문서

* JWT 구조: [jwt.md](jwt.md)
* 관리자 로그인 시퀀스: [../flows/admin-ott.md](../flows/admin-ott.md)
* nginx 프록시 헤더: [../infra/nginx.md](../infra/nginx.md)
