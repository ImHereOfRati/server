# 관리자 계정 인증·인가 검토

검토 기준일: 2026-08-12

이 문서는 현재 소스 코드의 관리자 계정 동작을 설명한다. 예전 `Admin OTT` 웹 로그인 문서와 달리, 현재 관리자 모바일 인증은 **관리자 ID/비밀번호 → TOTP MFA → JWT** 흐름이다.

## 1. 관리자 계정의 성격

관리자 계정은 `users` 테이블에서 조회하는 일반 사용자 계정이 아니다.

- 관리자 ID: `ADMIN_ID` → `admin.id`
- 관리자 표시 이름: `admin.nickname`
- 관리자 권한: 코드에서 항상 `ADMIN`으로 생성
- 관리자 UID: `imhere-admin:{adminId}`를 UUID v5 방식으로 변환한 고정 UUID
- 비밀번호: `ADMIN_MOBILE_PASSWORD_HASH`에 저장된 BCrypt 해시
- MFA: `ADMIN_MOBILE_TOTP_SECRET`에 저장된 Base32 secret

관련 구현:

- `AdminMobileAuthService.kt`
- `AdminMobileAuthProperties.kt`
- `application.yaml`의 `admin` 및 `admin.mobile` 설정

따라서 관리자 ID를 바꾸면 기존 관리자와 다른 고정 UID가 생성되고, 기존 관리자 JWT는 새 ID와 별개의 주체가 된다.

## 1.1 관리자 회원가입(초기 등록) 방식

관리자 계정에는 일반 사용자처럼 앱에서 호출하는 회원가입 API가 없다. 다음 세 값을 private config repo 또는 배포 환경변수에 등록하는 것이 관리자 계정의 **초기 등록**이다.

| 값 | 등록 내용 | 서버의 사용처 |
|---|---|---|
| `ADMIN_ID` | 관리자 로그인 ID | `admin.id`와 요청 ID 비교 |
| `ADMIN_MOBILE_PASSWORD_HASH` | 관리자 비밀번호의 BCrypt 해시 | 로그인 시 BCrypt 검증 |
| `ADMIN_MOBILE_TOTP_SECRET` | 관리자 인증 앱과 공유하는 Base32 secret | MFA 코드 검증 |

초기 등록 절차는 다음과 같다.

1. 운영자가 사용할 관리자 ID를 정한다.
2. 비밀번호를 BCrypt로 해시한다. 평문 비밀번호는 config repo나 환경변수에 저장하지 않는다.
3. 인증 앱용 TOTP secret을 생성한다.
4. 세 값을 private config repo의 런타임 env 파일 또는 배포 시크릿에 등록한다.
5. 배포 또는 애플리케이션 재기동 후 모바일 로그인으로 동작을 확인한다.

이 과정에서 `users` 테이블에 관리자 행이 INSERT되거나 OAuth provider가 연결되지는 않는다. 즉, 이 시스템의 관리자 “회원가입”은 데이터베이스 회원가입이 아니라 **운영 설정에 관리자 자격증명을 등록하는 절차**다.

## 1.2 관리자 로그인 방식

관리자 모바일 로그인은 등록된 세 값을 다음처럼 사용한다.

```text
ADMIN_ID + 비밀번호
  └─ ADMIN_ID와 일치하는지 확인
  └─ 입력 비밀번호와 ADMIN_MOBILE_PASSWORD_HASH를 BCrypt 비교
       ↓ 성공
challenge 발급(기본 300초)
       ↓
TOTP 앱의 6자리 코드 입력
  └─ ADMIN_MOBILE_TOTP_SECRET으로 현재 코드 검증
       ↓ 성공
관리자 access token + refresh token 발급
```

HTTP 순서는 다음과 같다.

```http
POST /api/admin/mobile/auth/login
Content-Type: application/json

{
  "adminId": "${ADMIN_ID}",
  "password": "운영자가 설정한 평문 비밀번호"
}
```

성공하면 응답의 `challenge`를 받아 TOTP 앱의 현재 6자리 코드를 다음 요청에 넣는다.

```http
POST /api/admin/mobile/auth/mfa/verify
Content-Type: application/json

{
  "challenge": "발급받은 challenge",
  "code": "123456"
}
```

두 단계가 모두 성공해야 JWT가 발급된다. OAuth 로그인이나 `users` 테이블의 이메일·OAuth subject는 이 관리자 모바일 로그인에 사용되지 않는다.

## 2. 전체 로그인 흐름

```text
관리자 앱
  │
  ├─ POST /api/admin/mobile/auth/login
  │    ID + password
  │
  ├─ 서버가 ADMIN_ID와 BCrypt 검증
  │
  ├─ challenge(UUID) 발급
  │    기본 유효시간: 300초
  │
  ├─ POST /api/admin/mobile/auth/mfa/verify
  │    challenge + 6자리 TOTP
  │
  ├─ 서버가 TOTP 검증(현재 시간 ± 30초 허용)
  │
  └─ access token + refresh token 발급
```

### 2.1 1단계: 비밀번호 검증

`POST /api/admin/mobile/auth/login`은 다음 조건을 모두 확인한다.

1. 요청의 `adminId`가 `admin.id`와 정확히 일치한다.
2. `admin.mobile.password-hash`가 비어 있지 않다.
3. 요청 비밀번호가 BCrypt 해시와 일치한다.

실패하면 관리자 ID가 존재하는지 여부와 무관하게 `Invalid administrator credentials`로 거부한다. 성공하면 서버 메모리 Caffeine cache에 challenge를 저장하고 challenge와 만료 초를 반환한다.

### 2.2 2단계: TOTP 검증

`POST /api/admin/mobile/auth/mfa/verify`는 challenge와 6자리 코드를 받는다.

- challenge가 존재해야 한다.
- challenge가 만료되지 않아야 한다.
- TOTP secret이 비어 있지 않아야 한다.
- 현재 30초 counter 기준 `-1, 0, +1` 구간 중 하나와 일치해야 한다.

검증 성공 시 challenge를 즉시 제거하고 access/refresh token을 발급한다. 실패한 MFA 요청에서는 challenge를 제거하지 않으므로, 만료 전까지 같은 challenge로 재시도할 수 있다.

## 3. 토큰과 관리자 API 접근

### 3.1 관리자 API 보안 체인

`SecurityConfig.adminApiFilterChain`이 `/api/admin/**`를 별도 처리한다.

| 경로 | 인증 | 설명 |
|---|---|---|
| `/api/admin/mobile/auth/login` | 없음 | ID/비밀번호 1단계 |
| `/api/admin/mobile/auth/mfa/verify` | 없음 | TOTP 2단계 |
| `/api/admin/mobile/auth/refresh` | 없음 | refresh token 교환 |
| 그 외 `/api/admin/**` | `ROLE_ADMIN` | `Authorization: Bearer <access token>` 필요 |

세 인증 엔드포인트는 Spring Security URL 인가뿐 아니라 메서드 보안의 public 경로 목록에도 등록되어 있다. 이 등록이 빠지면 URL 레벨의 `permitAll` 설정이 있어도 인증 전 요청이 `GLOBAL-200`(403)으로 차단될 수 있다.

관리자 API는 CSRF를 끄고 세션을 사용하지 않는 stateless 방식이다. JWT 인증 실패는 401, 인증은 됐지만 ADMIN 권한이 없으면 403이다.

### 3.2 Access token

관리자 인증 성공과 refresh 성공 시 `createAdminAccessToken()`으로 발급한다. JWT에는 다음 정보가 들어간다.

- category: access token
- 관리자 UID
- 관리자 ID(email claim)
- nickname
- `ROLE_ADMIN`
- `ACTIVE`
- 발급 시각/만료 시각

현재 코드상 JWT의 실제 만료시간은 `jwt.admin-expiration-minutes`이며 기본값은 15분이다.

### 3.3 Refresh token

refresh token은 다음 방식으로 관리한다.

1. refresh token의 JTI를 생성한다.
2. `admin-mobile-refresh:{tokenId}` 키로 서버 로컬 cache에 저장한다.
3. refresh 요청 시 JWT 서명·만료·refresh category·관리자 ID를 확인한다.
4. 기존 JTI를 새 JTI로 원자적 교체한다.
5. 같은 refresh token을 다시 사용하면 교체가 실패하여 재사용으로 거부한다.

refresh token의 기본 만료시간은 7일이다. cache는 Redis가 아닌 프로세스 로컬 Caffeine cache이므로 다음 특성이 있다.

- 애플리케이션 재시작 시 refresh token이 모두 무효화된다.
- 여러 인스턴스를 띄우면 인스턴스마다 토큰 상태가 달라진다.
- 현재 단일 EC2 인스턴스 운영 전제에서는 동작하지만 수평 확장 시 공유 저장소가 필요하다.

## 4. 인증이 필요한 관리자 기능

현재 관리자 API는 기능별 컨트롤러가 `/api/admin/**` 아래에 있고, 공통 보안 체인이 `ROLE_ADMIN`을 요구한다.

주요 기능은 다음과 같다.

- 사용자 목록 조회 및 사용자 차단/차단 해제
- 사용자 강제 로그아웃 및 탈퇴
- 약관 조회/등록/관리
- 친구 요청·친구 관계·친구 제한 관리
- 실패 알림 조회·재발송·폐기

관리자 웹 화면(`/admin/**`)과 모바일 관리자 API(`/api/admin/**`)는 현재 구현을 반드시 별도로 확인해야 한다. 저장소에는 과거 OTT 웹 로그인 설명 문서가 남아 있으므로, 운영 절차를 정할 때는 `SecurityConfig.kt`와 현재 컨트롤러를 기준으로 삼는다.

## 5. 운영 설정

운영 환경에서는 다음 값을 반드시 private config repo 또는 GitHub Actions가 주입하는 환경변수로 관리한다.

| 환경변수 | 용도 | 권장 사항 |
|---|---|---|
| `ADMIN_ID` | 관리자 로그인 ID | 추측하기 어려운 별도 ID 사용 |
| `ADMIN_MOBILE_PASSWORD_HASH` | BCrypt 비밀번호 해시 | 평문 비밀번호를 저장하지 않음 |
| `ADMIN_MOBILE_TOTP_SECRET` | TOTP secret | 로그·소스·일반 채널에 노출 금지 |
| `ADMIN_MOBILE_CHALLENGE_EXPIRATION_SECONDS` | 1단계 challenge 수명 | 기본 300초 |
| `JWT_ADMIN_EXPIRATION_MINUTES` | 관리자 access JWT와 응답의 만료시간 | 토큰과 `expiresInSeconds`에 동일하게 적용 |
| `ADMIN_MOBILE_REFRESH_EXPIRATION_DAYS` | refresh cache 수명 | 기본 7일 |
| `JWT_SECRET` | 모든 JWT 서명 키 | 운영에서 강한 랜덤 값 사용 및 교체 절차 필요 |

## 6. 검토 결과와 개선 필요 사항

### 해결됨: Bearer 인증에서 token category를 검사하지 않음

기존 `ImHereJjwtParserAdapter.parse()`는 서명과 만료만 검증하고 `category`가 access token인지 확인하지 않았다. `JwtAuthenticationFilter`도 이 `parse()` 결과로 인증을 만들었다.

그 결과 서명·만료가 유효하고 `ROLE_ADMIN` claim을 가진 **refresh token을 Bearer token으로 관리자 API에 제출**할 수 있다. refresh token은 원래 refresh 전용이어야 하므로, 관리자 API용 Bearer 경로에서는 category가 access token인지 반드시 확인해야 한다.

적용한 조치:

- Bearer 검증 전용 `parseAccessToken()`을 추가했다.
- `category == ACCESS_TOKEN`이 아니면 즉시 invalid token으로 거부한다.
- 일반 사용자 API에도 같은 필터가 적용된다.
- access/refresh token 교차 사용 회귀 테스트를 추가했다.

### 해결됨: 로그인 및 MFA 시도 제한이 없음

기존 challenge cache의 `maximumSize`와 만료시간은 저장공간 제어일 뿐, 비밀번호 실패 횟수나 TOTP 실패 횟수를 제한하지 않았다.

- `/login`은 permitAll이라 비밀번호 대입 요청을 계속 보낼 수 있다.
- 같은 challenge로 만료 전까지 TOTP를 반복 시도할 수 있다.
- 6자리 TOTP에 대한 IP·계정·challenge별 rate limit이 없다.

적용한 조치:

- 관리자 ID와 IP를 조합해 5분 내 로그인 실패 5회 초과를 거부한다.
- challenge별 MFA 실패 횟수를 5회로 제한하고 초과 시 challenge를 폐기한다.
- 제한 상태는 현재 프로세스의 Caffeine cache에 유지된다.

### 해결됨: access token 만료 설정이 두 군데로 분리됨

기존에는 응답의 `expiresInSeconds`와 실제 JWT 생성이 서로 다른 설정값을 사용했다. 기본 설정에서는 응답은 15분, 실제 JWT는 60분이었다.

현재는 `JWT_ADMIN_EXPIRATION_MINUTES` 하나를 access JWT의 실제 만료와 응답 `expiresInSeconds`에 함께 사용한다.

### 낮음: 로컬 cache 기반 refresh 상태

refresh token 회전 상태와 관리자 challenge가 모두 프로세스 로컬 cache에 있다. 재시작 시 로그아웃 효과가 생기는 것은 보안상 유리할 수 있지만, 다중 인스턴스에서는 정상 refresh가 다른 인스턴스에서 실패한다.

## 7. 권장 테스트 목록

- 잘못된 관리자 ID/비밀번호가 동일한 오류로 거부되는지
- 빈 password hash 또는 빈 TOTP secret이면 로그인할 수 없는지
- 만료된 challenge가 거부되는지
- 성공한 challenge가 재사용되지 않는지
- 잘못된 TOTP를 제한 횟수 이상 시도했을 때 잠기는지
- refresh token 회전 후 이전 refresh token이 거부되는지
- refresh token을 `Authorization: Bearer`로 사용했을 때 거부되는지
- 일반 사용자 JWT로 `/api/admin/**` 접근 시 403인지
- 관리자 access token 만료시간과 응답 `expiresInSeconds`가 일치하는지
- 애플리케이션 재시작 또는 다중 인스턴스에서 refresh 정책이 운영 의도와 일치하는지

## 8. 결론

현재 관리자 계정은 일반 사용자 DB 계정이 아니라 환경변수 기반의 단일 가상 관리자 계정이다. 인증 강도는 BCrypt 비밀번호와 TOTP의 2단계로 구성되어 있고, API 접근에는 `ROLE_ADMIN` JWT가 필요하다. 다만 운영 반영 전에는 **refresh token의 Bearer 사용 차단**, **로그인/MFA 시도 제한**, **access token 만료 설정 통합**을 우선 보완하는 것이 안전하다.
