# OIDC 로그인 설정 가이드

Google, Kakao OIDC 로그인을 백엔드에서 검증하기 위한 설정 문서입니다.

## 1. 공통 개념

- `issuer`: ID 토큰의 `iss` 값과 일치해야 하는 발급자 URL
- `audience`: ID 토큰의 `aud` 값과 일치해야 하는 클라이언트 ID
- `jwksUri`: 서명 검증용 공개키(JWKS) 엔드포인트
- `cacheKey`: JWKS 캐시 키. 구현용 값이며 OIDC 표준은 아님
- `sub`: 공급자 내부의 사용자 고유 식별자
- `state`: redirect flow에서 CSRF 방지용 값
- `nonce`: OIDC replay 방지용 값. Kakao/Google 모두 OIDC 요청에 포함한다.

## 2. Google

### 2.1 공식 문서

- OIDC 개요: https://developers.google.com/identity/openid-connect/openid-connect
- OAuth web server flow: https://developers.google.com/identity/protocols/oauth2/web-server
- Cloud Console 프로젝트: https://console.developers.google.com/auth/overview
- OAuth 클라이언트 목록: https://console.developers.google.com/auth/clients
- OAuth 동의 화면: https://console.developers.google.com/auth/branding

### 2.2 발급 절차

1. Google Cloud Console에서 프로젝트를 만든다.
2. OAuth 2.0 클라이언트 ID를 생성한다.
3. 앱 유형에 맞는 redirect URI를 등록한다.
4. 동의 화면 브랜드, 앱 이름, 이메일을 설정한다.
5. 로그인 scope에 `openid email profile` 을 사용한다.

### 2.3 backend 설정 값

```yaml
oidc:
  google:
    issuer: "https://accounts.google.com"
    audience: "<GOOGLE_CLIENT_ID>"
    cacheKey: "googleOidcKeys::googlePublicKeySet"
    jwksUri: "https://www.googleapis.com/oauth2/v3/certs"
```

### 2.4 체크 포인트

- `audience` 는 Google OAuth client id 와 동일해야 한다.
- `issuer` 는 `https://accounts.google.com` 또는 `accounts.google.com` 이다.
- 서버는 `nonce` 값을 토큰의 `nonce` 클레임과 비교한다.
- `openid` scope 없이는 ID token 흐름을 기대하면 안 된다.

## 3. Kakao

### 3.1 공식 문서

- Kakao app console: https://developers.kakao.com/console/app
- 로그인 개요: https://developers.kakao.com/docs/latest/en/kakaologin/common
- OIDC 사용: https://developers.kakao.com/docs/latest/en/kakaologin/utilize#oidc
- REST API: https://developers.kakao.com/docs/latest/en/kakaologin/rest-api
- Flutter: https://developers.kakao.com/docs/latest/en/kakaologin/flutter
- OIDC 사전설정: https://developers.kakao.com/docs/latest/en/kakaologin/prerequisite

### 3.2 발급 절차

1. Kakao Developers에서 앱을 생성한다.
2. Kakao Login을 활성화한다.
3. REST API key 를 확인한다.
4. redirect URI 를 등록한다.
5. OpenID Connect 를 활성화한다.
6. 필요한 consent item 만 켠다.
7. OIDC 요청에서는 `openid` scope 와 `nonce` 를 사용한다.

### 3.3 backend 설정 값

```yaml
oidc:
  kakao:
    issuer: "https://kauth.kakao.com"
    audience: "<KAKAO_REST_API_KEY>"
    cacheKey: "kakaoOidcKeys::kakaoPublicKeySet"
    jwksUri: "https://kauth.kakao.com/.well-known/jwks.json"
```

### 3.4 체크 포인트

- `audience` 는 REST API key 를 넣는다.
- `client_secret` 이 켜져 있으면 token exchange 시 필요하다.
- 추가 동의 재요청에서는 `openid` 를 scope 에 포함해야 ID token 이 다시 온다.

## 4. Flutter에서 해야 할 일

### 4.1 공통

1. Flutter 앱에서 provider 로그인 시작 UI를 만든다.
2. 인증 성공 후 backend 로 넘길 값이 `id_token` 인지 `authorization code` 인지 정한다.
3. backend 가 현재 `id_token` 을 검증하므로, 가능한 provider OIDC 로그인 흐름을 사용한다.
4. `https://` 로 backend 인증 API 를 호출한다.
5. 로그인 성공 응답의 `accessToken`, `refreshToken`, `userStatus` 를 앱 상태에 반영한다.

### 4.2 Google Flutter

- Google Identity Services 또는 적절한 Flutter 플러그인으로 OIDC 로그인 흐름을 연다.
- 로그인 후 `idToken` 을 확보해 backend `/api/auth/login` 또는 `/api/auth/registration` 으로 보낸다.
- Google client id 는 앱과 backend 설정이 동일해야 한다.

### 4.3 Kakao Flutter

- Kakao Flutter SDK 또는 Kakao Login 연동 플러그인을 사용한다.
- OIDC가 활성화된 앱 설정에서 `id_token` 을 받을 수 있는 흐름으로 구성한다.
- redirect URI, nonce, consent item 을 Kakao 콘솔과 일치시킨다.

### 4.4 Flutter 구현 체크리스트

- provider별 로그인 실패/취소 처리
- token 만료 재시도
- `state`/`nonce` 저장 및 복원
- backend 응답의 `userStatus == PENDING` 일 때 후속 가입 플로우 연결
- `ACTIVE` 가 아니면 앱 진입 제한 처리

## 5. backend 설정 예시

```yaml
oidc:
  kakao:
    issuer: "https://kauth.kakao.com"
    audience: "<KAKAO_REST_API_KEY>"
    cacheKey: "kakaoOidcKeys::kakaoPublicKeySet"
    jwksUri: "https://kauth.kakao.com/.well-known/jwks.json"
  google:
    issuer: "https://accounts.google.com"
    audience: "<GOOGLE_CLIENT_ID>"
    cacheKey: "googleOidcKeys::googlePublicKeySet"
    jwksUri: "https://www.googleapis.com/oauth2/v3/certs"
```
