# JWT

ImHere 서버는 일반 사용자 인증과 관리자 접근을 분리하기 위해 JWT와 세션을 함께 사용합니다.

---

## 핵심 판단

| 결정 | 내용 | 근거 |
|---|---|---|
| Access/Refresh 분리 | 일반 사용자는 Access + Refresh Token 조합으로 인증 | `ImHereTokenProviderAdapter.kt:22` |
| Refresh Token DB 미저장 | `refresh:{email}` 키로 앱 메모리 캐시에만 유지 | `ImHereTokenProviderAdapter.kt:50` |
| Admin 화면은 세션 기반 | `/admin/**`는 OTT 로그인 후 Session으로 접근 | `SecurityConfig.kt:141` |
| Admin API는 별도 체인 | `/api/admin/**`는 일반 API 체인과 분리 | `SecurityConfig.kt:101` |

---

## 토큰 종류

| 토큰 | 용도 | 저장 위치 | 만료 |
|---|---|---|---|
| Access Token | 일반 API 인증 | 클라이언트 | `720`분 |
| Refresh Token | Access 재발급 | 클라이언트 + 앱 메모리 캐시 | `7`일 |
| Admin Session | 관리자 화면 접근 | 서버 세션 | 세션 수명에 따름 |

실제 설정값은 `application.yaml`의 `jwt.*` 값을 기준으로 합니다.

---

## SecurityFilterChain 구조

`SecurityConfig`에는 3개의 체인이 있습니다.

| 순서 | 경로 | 인증 방식 |
|---|---|---|
| 1 | `/api/admin/**` | `ROLE_ADMIN` + Admin 체인 |
| 2 | `/admin/**` | OTT 로그인 + Session |
| 3 | 나머지 API | Stateless JWT |

핵심 규칙:

* `/api/auth/**`와 화이트리스트는 JWT 필터를 건너뜁니다.
* 일반 API는 `Authorization: Bearer ...` 헤더를 사용합니다.
* Admin Web과 Admin API는 같은 권한군이지만 진입 체인이 다릅니다.

---

## Refresh Token 재발급 방식

1. 클라이언트가 `/api/auth/refresh`로 Refresh Token을 보냅니다.
2. 서버가 서명과 만료를 검증합니다.
3. Token claims에서 email을 꺼냅니다.
4. 캐시의 `refresh:{email}` 값과 비교합니다.
5. 일치하면 새 Access/Refresh Token을 발급하고 캐시 값을 교체합니다.
6. 불일치하면 즉시 `IMHERE_INVALID_TOKEN`으로 실패합니다.

이 방식은 탈취된 이전 Refresh Token이 재사용되는 것을 막기 위한 rotate 전략입니다.

---

## 실제 요청 예시

```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

```json
{
  "imhereResponseCode": "SUCCESS",
  "message": "성공",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "status": "ACTIVE"
  }
}
```

---

## JWT 필터 동작

`JwtAuthenticationFilter`는 다음 순서로 동작합니다.

1. 화이트리스트 경로면 필터를 건너뜁니다.
2. `Authorization` 헤더에서 Bearer Token을 추출합니다.
3. 토큰 유효성을 검사합니다.
4. claims를 파싱해 `ImHereUserDetails`를 만듭니다.
5. 사용자 상태를 확인합니다.
6. 통과하면 `SecurityContext`에 인증 주체를 넣습니다.

차단 조건:

* 잘못된 토큰 -> `IMHERE_INVALID_TOKEN`
* 비활성/잠금 상태 -> 인증 실패

---

## 현재 구현상의 주의점

* Refresh Token과 Admin Session은 모두 앱/서버 메모리 기반이라 재시작 시 사라집니다.
* Refresh Token은 사용자당 1개만 유지됩니다.
* 다중 인스턴스로 확장하면 현재 캐시 전략은 그대로 유지할 수 없습니다.

---

## 코드 근거

* JWT 설정 바인딩: `src/main/kotlin/com/kdongsu5509/auth/adapter/out/jwt/ImHereJwtProperties.kt:7`
* Token issue / rotate: `src/main/kotlin/com/kdongsu5509/auth/adapter/out/jwt/ImHereTokenProviderAdapter.kt:22`
* Admin API 체인: `src/main/kotlin/com/kdongsu5509/auth/security/config/SecurityConfig.kt:101`
* Admin Web 체인: `src/main/kotlin/com/kdongsu5509/auth/security/config/SecurityConfig.kt:141`
* 일반 API 체인: `src/main/kotlin/com/kdongsu5509/auth/security/config/SecurityConfig.kt:194`

---

## 관련 문서

* OIDC 검증 구조: [oauth.md](oauth.md)
* 관리자 OTT 로그인: [admin-ott.md](admin-ott.md)
* 토큰 재발급 시퀀스: [../flows/token-refresh.md](../flows/token-refresh.md)
