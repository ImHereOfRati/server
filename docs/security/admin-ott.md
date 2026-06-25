# Admin OTT

관리자 로그인은 비밀번호 대신 OTT(One-Time Token)와 Session을 사용합니다.

---

## 핵심 판단

| 결정 | 내용 | 근거 |
|---|---|---|
| 비밀번호 미사용 | 관리자 로그인은 OTT 발급/검증으로만 처리 | `SecurityConfig.kt:172` |
| 허용 IP 선검사 | `/admin/ott/request`는 허용 IP에서만 접근 가능 | `SecurityConfig.kt:82` |
| 발급 횟수 제한 | 동일 username 기준 5분 내 최대 3회 발급 | `ImHereOttSuccessHandler.kt:43` |
| OTT는 Discord로만 전달 | 토큰을 응답 바디가 아니라 Webhook으로 전송 | `ImHereOttSuccessHandler.kt:46` |

---

## 구성 요소

* `OttIpValidationFilter`
* `JdbcOneTimeTokenService`
* `ImHereOttSuccessHandler`
* `OttLoginSuccessHandler`

---

## 실제 흐름

1. 관리자가 `/admin/login`으로 진입합니다.
2. `/admin/ott/request`로 OTT 발급을 요청합니다.
3. 허용 IP 검사를 통과해야 합니다.
4. OTT를 생성하고 Discord로 전송합니다.
5. 관리자가 `/admin/ott/verify`로 OTT를 제출합니다.
6. 검증에 성공하면 Session을 만들고 `/admin`으로 이동합니다.

---

## 실제 요청 예시

```http
POST /admin/ott/request
Content-Type: application/x-www-form-urlencoded

username={admin.id}
```

성공 시 토큰은 HTTP 응답 바디가 아니라 Discord Webhook으로만 전달됩니다.

---

## 현재 구현 특성

* 발급 횟수 제한은 IP 기준이 아니라 username 기준입니다.
* 횟수 제한 카운터는 `ConcurrentHashMap`이라 인스턴스 재시작 시 초기화됩니다.
* Admin UI는 Session 기반이고, Admin API는 별도 권한 체인을 탑니다.

---

## 코드 근거

* OTT 로그인 설정: `src/main/kotlin/com/kdongsu5509/auth/security/config/SecurityConfig.kt:172`
* 허용 IP 필터 등록: `src/main/kotlin/com/kdongsu5509/auth/security/config/SecurityConfig.kt:82`
* username 기준 발급 제한: `src/main/kotlin/com/kdongsu5509/auth/security/handler/ImHereOttSuccessHandler.kt:43`

---

## 관련 문서

* JWT 구조: [jwt.md](jwt.md)
* 관리자 로그인 시퀀스: [../flows/admin-ott.md](../flows/admin-ott.md)
