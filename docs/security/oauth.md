# OAuth / OIDC

ImHere 서버는 Spring OAuth2 Client 전체 플로우를 사용하지 않고, Kakao / Google이 발급한 OIDC ID Token만 직접 검증합니다.

---

## 핵심 판단

| 결정 | 내용 | 근거 |
|---|---|---|
| Provider 3개 고정 | Kakao, Google, Apple만 지원 | `application.yaml` `oidc.providers` |
| ID Token 직접 검증 | 모바일 앱이 받은 토큰을 서버에서 검증만 수행 | `OIDCVerifyService.kt:23` |
| 공개키는 캐시 사용 | 설정된 `jwksUri`를 provider별 캐시에 저장하고 매 요청마다 다시 받지 않음 | `OidcPublicKeyClient.kt` |

---

## Provider 설정

| Provider | 허용 issuer | jwksUri |
|---|---|---|
| Kakao | `https://kauth.kakao.com` | `https://kauth.kakao.com/.well-known/jwks.json` |
| Google | `https://accounts.google.com`, `accounts.google.com` | `https://www.googleapis.com/oauth2/v3/certs` |
| Apple | `https://appleid.apple.com` | `https://appleid.apple.com/auth/keys` |

`issuers`와 `audiences`는 값 하나가 아니라 목록입니다. 두 가지 이유가 있습니다.

- Google은 `iss`를 스킴 포함(`https://accounts.google.com`)과 미포함(`accounts.google.com`)
  두 형태로 발급합니다. 어느 쪽이 와도 통과해야 합니다.
- Google과 Apple은 플랫폼마다 client ID가 갈립니다. Apple은 iOS에서 bundle ID,
  웹에서 Services ID가 `aud`로 들어옵니다. 그래서 여러 값을 등록해 두고
  "그 중 하나면 통과"로 판단합니다.

값이 빈 항목은 `OIDCProperties`가 걸러냅니다. 쓰지 않는 플랫폼의 환경변수는 비워 두면 됩니다.
다만 목록이 전부 비면 그 provider 로그인은 전부 실패합니다. 비교할 기준이 없는 상태를
"통과"로 보면 아무 토큰이나 받아들이게 되기 때문입니다.

실제 audience 값은 운영 설정과 함께 `application.yaml`에 정의되어 있고, Apple은
`APPLE_CLIENT_ID`(iOS bundle ID) / `APPLE_SERVICE_ID`(웹 Services ID) 환경변수로 주입합니다.
Google의 iOS/Android client ID도 `GOOGLE_CLIENT_ID_IOS` / `GOOGLE_CLIENT_ID_ANDROID`로 주입합니다.

### Apple의 이메일

Apple ID Token에는 표시 이름(`name`/`nickname`)이 없습니다. 이름은 인가 응답 본문으로만
한 번 오고 ID Token에는 담기지 않기 때문입니다. 그래서 Apple 로그인의 닉네임은
이메일 앞부분이 됩니다.

이메일 자체가 없는 경우(사용자가 email scope를 주지 않은 경우)에는 로그인을 거절합니다.
`users.email`이 `NOT NULL UNIQUE`라 이메일 없이 가입시킬 수 없습니다.
사용자가 "이메일 숨기기"를 택한 경우에는 `@privaterelay.appleid.com` 주소가 오는데,
이것은 실제로 수신 가능한 주소이므로 정상 처리됩니다.

---

## 검증 흐름

`OIDCVerifyService` 기준:

1. provider 설정을 조회합니다.
2. ID Token에서 `kid`를 추출합니다.
3. provider별 공개키를 로드합니다.
4. 서명을 검증합니다.
5. `iss`, `aud`, `nonce`, `exp`를 검증합니다.
6. `email`, `nickname`, `sub`를 추출합니다.

`nickname`이 없으면 `name`, 그것도 없으면 email prefix를 사용합니다.

---

## 실제 요청 예시

```http
POST /api/auth
Content-Type: application/json

{
  "provider": "KAKAO",
  "idToken": "eyJraWQiOiJ...",
  "nonce": "6d7d4e..."
}
```

이 요청은 서버 내부에서 `kid` 추출 -> 공개키 조회 -> 서명 검증 -> `iss/aud/nonce` 검증 순서로 처리됩니다.

---

## 검증 실패 조건

| 조건 | 결과 |
|---|---|
| nonce 누락/공백 | `OIDC_NONCE_INVALID` |
| email claim 없음 | `OIDC_MISSING_EMAIL` |
| 공개키 미매칭 | 공개키 조회 실패 예외 |
| 만료/서명 오류 | 인증 예외 |

---

## 키 캐시

| Provider | cache key |
|---|---|
| Kakao | `kakaoOidcKeys::kakaoPublicKeySet` |
| Google | `googleOidcKeys::googlePublicKeySet` |

공개키 세트는 앱 메모리 캐시에 유지됩니다.

---

## 현재 구현상의 주의점

* OAuth2 Client 전체 라이프사이클이 아니라 ID Token 검증만 직접 구현한 구조입니다.
* 따라서 리다이렉트, authorization code exchange 같은 서버 주도 OAuth 플로우 문서와는 다릅니다.

---

## 코드 근거

* OIDC 검증 메인 로직: `src/main/kotlin/com/kdongsu5509/auth/application/service/OIDCVerifyService.kt:23`
* provider 설정값: `src/main/resources/application.yaml:127`

---

## 관련 문서

* JWT 구조: [jwt.md](jwt.md)
* 기존 사용자 로그인: [../flows/oidc-login.md](../flows/oidc-login.md)
* 신규 가입 / 활성화: [../flows/oidc-signup-activation.md](../flows/oidc-signup-activation.md)
