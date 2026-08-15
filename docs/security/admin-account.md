# 관리자 웹 인증

관리자 화면은 Thymeleaf 기반 `/admin` 웹으로 제공한다. 모바일 전용 관리자 인증 API는 더 이상 제공하지 않는다.

## 로그인 흐름

1. `GET /admin/login`에서 관리자 ID와 비밀번호를 입력한다.
2. `POST /admin/login`이 BCrypt 비밀번호를 확인하고 MFA challenge를 서버 세션에 저장한다.
3. `POST /admin/login/mfa`가 challenge와 TOTP 코드를 검증한다.
4. 성공하면 `ROLE_ADMIN` Spring Security 세션을 생성한다.
5. `POST /admin/logout`은 세션을 무효화한다.

웹 로그인은 HttpOnly 세션 쿠키와 CSRF 토큰을 사용하며 JWT를 브라우저에 저장하지 않는다.

## 로그인 실패 차단

- 제한 키: 관리자 ID와 클라이언트 IP
- 비밀번호 인증 실패 5회째에 즉시 차단
- 차단 기간: 7일
- 성공한 비밀번호 인증 시 실패 상태 삭제
- TOTP challenge는 별도로 5회 실패 시 폐기

실패 횟수와 차단 상태는 `CachePort` 구현체에 저장된다. 현재 기본 구현은 프로세스 로컬 Caffeine cache이므로 애플리케이션 재시작이나 다중 인스턴스 간에는 상태가 공유되지 않는다.

## 관리자 API

관리자 기능 API는 `/api/admin/**` 아래에 유지된다. 웹 화면은 동일한 application service를 직접 호출하며, API는 기존 JWT 관리자 인증 체인을 사용한다.

모바일 전용 경로였던 `/api/admin/mobile/auth/**`는 제거되었다.

## 운영 설정

| 설정 | 용도 |
|---|---|
| `ADMIN_ID` | 관리자 ID |
| `ADMIN_MOBILE_PASSWORD_HASH` | BCrypt 비밀번호 hash |
| `ADMIN_MOBILE_TOTP_SECRET` | TOTP secret |
| `ADMIN_MOBILE_CHALLENGE_EXPIRATION_SECONDS` | MFA challenge 만료 시간 |
| `JWT_ADMIN_EXPIRATION_MINUTES` | 관리자 API JWT 만료 시간 |

비밀번호와 TOTP secret은 평문 로그나 HTML에 노출하지 않는다.
