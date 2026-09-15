# SMS 일일 수신자 Rate Limit 검증 보고서 — 2026-09-03

## 배경

[2026-08-30 보고서](loadtest-rate-limit-report-2026-08-30.md)에서는 단일 사용자가 동일 JWT로 200→300→400 RPS까지 요청을 집중시켰을 때 `429`가 전혀 발생하지 않았고, 서버가 요청을 제한하기 전에 EOF·5xx로 먼저 무너지는 것을 확인했다. 이 결과가 이슈 [`#113`](https://github.com/ImHereOfRati/server/issues/113), [`#114`](https://github.com/ImHereOfRati/server/issues/114)의 근거가 됐고, `SmsDailyRecipientRateLimiter`(Bucket4j 기반, 발신자별 하루 4명 수신자 제한)가 구현됐다.

이번 테스트는 같은 시나리오(`one-to-many-test/sms-send.js`)를 재실행해 rate limiter가 실제로 작동하는지 확인한다.

## 테스트 정보

| 항목 | 값 |
|---|---|
| 결과 파일 | `loadtest/k6/generated/results/sms-send-precision-20260903-235404.json` |
| 테스트 스크립트 | `loadtest/k6/test/one-to-many-test/sms-send.js` |
| 요청 엔드포인트 | `POST /api/notifications` (`notificationMethod: SMS`) |
| 인증 사용자 | fixture 사용자 1명 (`ACTOR_INDEX=0`), 모든 요청이 같은 발신자·같은 수신 전화번호 |
| 부하 모델 | 200 → 300 → 400 RPS, 각 15초 (`STAGE_DURATION=15s`) |
| 적용 중인 제한 | `SmsDailyRecipientRateLimiter`: 발신자당 하루 4명(distinct 수신자) |
| 테스트 대상 | AWS 격리 부하테스트 인프라, ECR `latest` 이미지(커밋 `09f1102c` 반영본, CD 성공 확인) |

## 결과

| 지표 | 결과 |
|---|---:|
| HTTP 요청 수 | 11,997 |
| 실제 처리율 | 266.52 req/s |
| `429` 응답 (일일 한도 초과) | 11,517건 (96.0%, 255.86/s) |
| 5xx 응답 | 479건 (4.0%, 10.64/s) |
| 네트워크 오류 / EOF | 0건 (check 11,997/11,997 통과) |
| 평균 응답시간 | 9.67 ms |
| p90 / p95 | 12.39 ms / 17.69 ms |
| 최대 응답시간 | 2.07 s |
| 최대 VU | 19 (preAllocated 300 중) |

## 판정

### 발신자별 일일 rate limit

판정: **작동함**

동일 JWT·동일 수신자로 집중시킨 12,000건 가까운 요청 중 96%가 `429`로 즉시 거절됐고, 네트워크 오류·EOF는 0건이었다. 8월 30일 보고서와 달리 서버가 과부하로 무너지기 전에 요청을 차단하는 것을 확인했다 — `SmsDailyRecipientRateLimiter` 도입이 의도한 효과를 낸다.

```text
과도한 요청(동일 수신자 반복)
  → 최초 1건: 일일 한도(4명) 내 신규 수신자로 처리
  → 이후 요청: reservedRecipients에 이미 존재 → newRecipients 크기 0
  → 429 또는 500으로 즉시 응답, EOF/서버 다운 없음
```

### 코드 리뷰로 확인한 결함: `Bucket4j tryConsume(0)`

이번 실행에서 5xx가 479건(4%) 발생한 원인을 코드 레벨에서 추적한 결과, `SmsDailyRecipientRateLimiter.DailyRecipientBucket.reserve()`에 실제 버그가 있음을 확인했다.

```kotlin
val newRecipients = candidates - reservedRecipients
if (!bucket.tryConsume(newRecipients.size.toLong())) { ... }
```

이 테스트처럼 모든 요청이 **같은 수신자**를 가리키면, 최초 1건을 제외한 나머지 요청은 `newRecipients.size == 0`이 된다. Bucket4j 8.19.0의 `Bucket.tryConsume(long)`은 내부적으로 `LimitChecker.checkTokensToConsume(long)`을 호출하는데, 이 메서드는 인자가 0 이하이면 무조건 `IllegalArgumentException`을 던진다(바이트코드로 직접 확인함 — `nonPositiveTokensToConsume` 분기). `reserve()`는 이 예외를 잡지 않으므로 `NotificationService`까지 전파되고, `GlobalExceptionHandler`의 `@ExceptionHandler(Exception::class)`(500 매핑)에서 처리된다.

즉 **"이미 예약된 수신자로 다시 요청"** 하는 케이스마다 정상적으로 처리돼야 할 요청(추가 한도 소모 없이 그냥 성공해야 함)이 매번 500 에러로 떨어진다. 이번 테스트의 479건 5xx는 이 결함과 정확히 일치하는 패턴이다.

**권장 수정**: `newRecipients`가 비어 있으면 `tryConsume` 호출 자체를 건너뛰고 즉시 `Accepted`로 반환한다.

```kotlin
val newRecipients = candidates - reservedRecipients
if (newRecipients.isEmpty()) {
    return ReservationResult.Accepted(newlyReservedCount = 0, usedCount = reservedRecipients.size)
}
if (!bucket.tryConsume(newRecipients.size.toLong())) { ... }
```

## 해석상의 제한

- 이 테스트는 모든 요청이 **동일한 단일 수신자**를 향하도록 설계돼 있어, "발신자당 하루 4명(distinct)" 한도 자체(즉 서로 다른 수신자 4명을 채우고 5번째부터 막히는 경로)는 검증하지 못했다. distinct 수신자 여러 명을 대상으로 한 별도 시나리오가 필요하다.
- (2026-09-04 갱신) 이 최초 실행은 인프라를 테스트 직후 자동 삭제해 로그 기반 확증을 못 했다. 아래 "실제 로그로 확인한 5xx 원인"에서 별도로 로그를 확보해봤더니 `tryConsume(0)` 수정 이후의 잔여 5xx는 전부 Nginx `limit_conn`이었다 — 이 최초 479건 자체가 `tryConsume(0)` 때문이었는지는 로그가 없어 끝내 확증하지 못했다.
- k6의 기본 `http_req_failed`는 429도 실패로 집계하므로(`value: 0.9999`), rate-limit 검증에서는 커스텀 `only_one_sms_rate_limit_count`/`only_one_sms_server_error_count` 지표를 기준으로 판단했다.

## 권장 후속 조치

1. `DailyRecipientBucket.reserve()`에 `newRecipients.isEmpty()` 조기 반환을 추가해 `tryConsume(0)` 예외를 제거한다.
2. distinct 수신자 5명 이상을 순환 지정하는 시나리오를 추가해 "4명까지 성공, 5번째부터 429" 경로를 직접 검증한다.
3. ~~다음 실행부터는 애플리케이션 로그(Loki)·Grafana 대시보드를 인프라 삭제 전에 캡처해 429/500 발생 시점을 요청 단위로 대조할 수 있게 한다.~~ → 2026-09-04 실행에서 SSH로 직접 확보해 완료. 이후 실행에서도 teardown 전 로그 확보를 기본 절차로 삼는다.
4. `loadtest/setup/test-env/nginx.conf`의 `limit_conn per_ip_conn 20`은 단일 IP에서 부하를 쏘는 k6 실행 구조상 항상 걸릴 수 있는 값이다. 애플리케이션 동작과 무관한 노이즈를 줄이려면 이 값을 부하 시나리오의 최대 동시 VU 수 이상으로 올리거나(예: 500), rate-limit 자체를 검증하는 시나리오에서는 이 지표를 별도로 제외하고 집계한다.

## `tryConsume(0)` 수정 후 재검증 (2026-09-04)

위에서 권장한 수정(`newRecipients.isEmpty()`일 때 `tryConsume` 생략하고 즉시 `Accepted` 반환)을 `SmsDailyRecipientRateLimiter.kt`에 반영하고, 동일 조건(`sms-send.js`, 200→300→400 RPS, 각 15초)으로 재실행했다. 배포 전 `main`에 남아 있던 별개의 모듈 순환 의존(`auth ↔ user`, `AllowPendingUser` 관련) 때문에 CD가 막혀 있었는데, 이 애노테이션을 `support` 모듈로 옮겨 순환을 끊고 CD를 다시 통과시킨 뒤 재실행했다.

| 지표 | 수정 전 (09-03) | 수정 후 (09-04) |
|---|---:|---:|
| HTTP 요청 수 | 11,997 | 11,996 |
| `429` (일일 한도 초과) | 11,517 (96.0%) | 11,516 (96.0%) |
| 5xx 응답 | 479 (4.0%) | **157 (1.3%)** |
| 정상 응답(2xx) | 1 (0.01%) | 323 (2.7%) |
| 네트워크 오류 / EOF | 0건 | 0건 |
| p90 / p95 응답시간 | 12.39 ms / 17.69 ms | 10.12 ms / 15.46 ms |
| 결과 JSON | `sms-send-precision-20260903-235404.json` | `sms-send-precision-20260904-003151.json` |

5xx가 479건(4.0%)에서 157건(1.3%)으로 줄었다. 다만 0건까지 내려가지는 않았고, 이전엔 사실상 없던 정상 응답(2xx)이 323건 발생했다 — 아래 "실제 로그로 확인한 5xx 원인" 절에서 밝히듯, 이 잔여 5xx는 애초에 `tryConsume(0)` 결함과 무관했다.

## 실제 로그로 확인한 5xx 원인 (2026-09-04)

잔여 5xx의 정확한 원인을 확인하기 위해 동일 시나리오를 한 번 더 실행하되, 이번엔 teardown 전에 SSH로 애플리케이션·Nginx 컨테이너 로그를 직접 확보했다.

**애플리케이션 로그**: 해당 실행에서 5xx 24건이 발생했지만, 컨테이너 로그에는 `GlobalExceptionHandler`의 500 처리 로그(`log.error("예상하지 못한 오류 발생: ", e)`)나 그 외 어떤 예외 스택트레이스도 없었다. WARN 레벨 로그 7건은 전부 기동 시점(빈 후처리기·HikariCP·JPA open-in-view) 노이즈였고 요청 처리 중 발생한 것이 아니었다. 즉 **이번 5xx는 Spring 애플리케이션까지 도달조차 하지 않았다** — `tryConsume(0)` 수정이 적용된 이후 애플리케이션 레이어는 완전히 깨끗했다는 뜻이다. (참고로 같은 로그에서 "신규 수신자=0"으로 예약된 요청들이 전부 `Status: 202`로 정상 응답한 것도 직접 확인했다 — 수정이 실제로 동작한다는 증거다.)

**Nginx 로그**: 5xx 24건은 Nginx 컨테이너의 `http.response.status_code:503` 24건과 정확히 일치했고, 동시에 다음 에러가 24건 찍혀 있었다.

```
2026/09/04 00:46:09 [error] 30#30: *41 limiting connections by zone "per_ip_conn", client: 211.109.198.169, server: _, request: "POST /api/notifications HTTP/1.1", host: "13.124.121.59"
```

`loadtest/setup/test-env/nginx.conf:43`의 `limit_conn per_ip_conn 20` — 클라이언트 IP 하나당 동시 연결을 20개로 제한하는 설정이다. k6 부하 생성기는 (NAT를 거쳐) 단일 IP(`211.109.198.169`)에서 최대 300~400 VU로 요청을 쏘므로, 순간적으로 20개를 넘는 연결이 몰리면 Nginx가 애플리케이션에 전달하기도 전에 즉시 503으로 끊어버린다.

**결론**: 이 리포트가 처음에 지목한 5xx 원인(`tryConsume(0)`)은 실제로 존재하는 결함이었고 고쳐야 했던 것이 맞지만(위 코드 리뷰 근거는 여전히 유효하다), **9월 3일 최초 실행의 479건과 수정 후 157건 5xx 각각이 정확히 무엇 때문이었는지는 로그 없이 추정한 것**이었다. 이번에 로그로 직접 확인한 24건은 전부 Nginx `limit_conn`이었고 애플리케이션 예외는 0건이었다 — `tryConsume(0)` 수정 이후로는 잔여 5xx가 애플리케이션이 아니라 **단일 소스 IP에서 부하를 쏘는 테스트 방식 자체의 한계**(실제 서비스에서는 클라이언트 IP가 분산돼 있어 이 한도에 잘 안 걸린다)에서 나온다고 봐야 한다. 이전 실행들의 5xx가 정확히 몇 %씩 `tryConsume(0)` vs Nginx 기인이었는지는 로그가 없어 더 이상 구분할 수 없다.

## 부록: 실행 스크립트 수정

`loadtest/setup/setup-loadtest.sh`의 `ssh` 호출 다수가 `-n` 옵션 없이 로컬 stdin을 그대로 물려받고 있었다. `run-loadtest.sh`의 후속 대화형 프롬프트(테스트 선택·plan·RPS·duration)에 답을 미리 파이프로 넣어도, 이 ssh 호출들이 그 입력을 먼저 소비해버려 `select_test`가 EOF를 만나 스크립트가 조용히 종료되는 문제가 있었다(1차 시도에서 재현). ECR 로그인(`docker login --password-stdin`) 한 곳만 제외하고 모든 `ssh` 호출에 `-n`을 추가해 해결했다.

## 부록: 모듈 순환 의존 수정

수정 반영 재검증을 시도하던 중, main의 CD가 `ModularityTest`(Spring Modulith `ApplicationModules.verify()`)에서 실패하고 있는 것을 발견했다. `user` 모듈의 `UserCommandController`가 `auth.security.shared.AllowPendingUser`를 참조하도록 변경됐는데, `auth` 모듈은 이미 `user`에 광범위하게 의존하고 있어(`OAuth2Provider`, `UserLookupContract` 등) `auth → user → auth` 순환이 생겼다. `AllowPendingUser`는 이미 Named Interface로는 정상 노출돼 있었으므로 노출 방식이 아니라 의존 방향 자체가 문제였다. `auth`·`user` 양쪽에서 의존해도 순환이 생기지 않는 open 모듈 `support`로 `AllowPendingUser`를 옮겨 해결했다(`agreement`의 기존 사용처도 함께 갱신). 이 수정 없이는 어떤 커밋도 CD에서 새 이미지로 빌드되지 않으므로, 본 리포트의 "수정 후 재검증"도 이 수정이 선행돼야 가능했다.
