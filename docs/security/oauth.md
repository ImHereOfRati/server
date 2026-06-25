# OAuth / OIDC

ImHere 서버는 Spring OAuth2 Client 전체 플로우를 사용하지 않고, Kakao / Google이 발급한 OIDC ID Token만 직접 검증합니다.

---

## 핵심 판단

| 결정 | 내용 | 근거 |
|---|---|---|
| Provider 2개 고정 | Kakao, Google만 지원 | `application.yaml:127` |
| ID Token 직접 검증 | 모바일 앱이 받은 토큰을 서버에서 검증만 수행 | `OIDCVerifyService.kt:23` |
| 공개키는 캐시 사용 | JWKS를 매 요청마다 다시 받지 않음 | `application.yaml:131`, `application.yaml:136` |

---

## Provider 설정

| Provider | issuer | jwksUri |
|---|---|---|
| Kakao | `https://kauth.kakao.com` | `https://kauth.kakao.com/.well-known/jwks.json` |
| Google | `https://accounts.google.com` | `https://www.googleapis.com/oauth2/v3/certs` |

실제 audience 값은 운영 설정과 함께 `application.yaml`에 정의되어 있습니다.

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
POST /api/auth/login
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
