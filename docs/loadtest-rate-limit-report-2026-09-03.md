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
- 인프라를 테스트 직후 자동 삭제했기 때문에 애플리케이션·Nginx 로그로 429/500 각각의 정확한 요청 ID·타이밍을 사후 대조하지 못했다. `tryConsume(0)` 결함이 479건 5xx의 유력한 원인으로 보이지만, 로그 기반 100% 확증은 아니다.
- k6의 기본 `http_req_failed`는 429도 실패로 집계하므로(`value: 0.9999`), rate-limit 검증에서는 커스텀 `only_one_sms_rate_limit_count`/`only_one_sms_server_error_count` 지표를 기준으로 판단했다.

## 권장 후속 조치

1. `DailyRecipientBucket.reserve()`에 `newRecipients.isEmpty()` 조기 반환을 추가해 `tryConsume(0)` 예외를 제거한다.
2. distinct 수신자 5명 이상을 순환 지정하는 시나리오를 추가해 "4명까지 성공, 5번째부터 429" 경로를 직접 검증한다.
3. 다음 실행부터는 애플리케이션 로그(Loki)·Grafana 대시보드를 인프라 삭제 전에 캡처해 429/500 발생 시점을 요청 단위로 대조할 수 있게 한다.

## 부록: 실행 스크립트 수정

`loadtest/setup/setup-loadtest.sh`의 `ssh` 호출 다수가 `-n` 옵션 없이 로컬 stdin을 그대로 물려받고 있었다. `run-loadtest.sh`의 후속 대화형 프롬프트(테스트 선택·plan·RPS·duration)에 답을 미리 파이프로 넣어도, 이 ssh 호출들이 그 입력을 먼저 소비해버려 `select_test`가 EOF를 만나 스크립트가 조용히 종료되는 문제가 있었다(1차 시도에서 재현). ECR 로그인(`docker login --password-stdin`) 한 곳만 제외하고 모든 `ssh` 호출에 `-n`을 추가해 해결했다.
