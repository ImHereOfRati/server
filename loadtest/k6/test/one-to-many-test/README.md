# one-to-many-test — 한 사람이 여러 요청

**한 명의 사용자**가 같은 API를 초당 수백 번 두드린다. 부하 총량을 보는 게 아니라, **한 계정이 서버를 독차지할 수 있는지**를 본다.

fixture의 `ACTOR_INDEX`번째 사용자 토큰 하나만 쓰기 때문에, 서버 입장에서는 전부 같은 사람의 요청이다. 사용자 단위 rate limit이 걸려 있으면 `429 Too Many Requests`가 나와야 정상이다.

## 스크립트

| 파일 | 때리는 API | 왜 이 API인가 |
|---|---|---|
| `user-me.js` | `GET /api/users/my` | 가장 가벼운 인증 조회. 순수하게 limiter만 남는 기준선 |
| `sms-send.js` | `POST /api/notifications` (SMS) | **건당 과금**되는 경로. 막히지 않으면 요금이 샌다 |
| `fcm-send.js` | `POST /api/notifications` (FCM) | 외부 푸시 호출 경로. SMS와 limiter 정책이 같은지 대조 |
| `map-geocode.js` | `GET /api/maps/geocode` | 네이버 지도 API 프록시. **외부 API 쿼터**를 태우는 경로 |

`sms-send.js`는 `targetIds`에 사용자 phone을, `fcm-send.js`는 사용자 id를 넣는다. 서버의 `notificationMethod`별 대상 식별자 규칙이 다르기 때문이다.

## 부하 모양

세 단계로 올린다. 기본 `STAGE_DURATION`은 `user-me.js`가 `1m`, 나머지는 `10s`다.

```
200 RPS → 300 RPS → 400 RPS   (ramping-arrival-rate)
```

## 실행

```bash
./loadtest/run-k6.sh --scenario only-one-sms --base-url 'http://<app-public-ip>'
```

대상 사용자를 바꾸려면 `ACTOR_INDEX`를 준다. fixture 범위(0~999)를 벗어나면 스크립트가 즉시 실패한다.

```bash
ACTOR_INDEX=42 FIXTURE="$(pwd)/loadtest/k6/generated/tokens.json" \
BASE_URL='http://<app-public-ip>' \
k6 run loadtest/k6/test/one-to-many-test/sms-send.js
```

## 지표 읽는 법

`thresholds`가 비어 있다. **429는 실패가 아니라 기대하는 결과**이므로, 실행을 실패로 떨어뜨리지 않고 상태 코드 분포를 그대로 보고한다.

| 메트릭 | 의미 |
|---|---|
| `only_one_*_response_count{status}` | 상태 코드별 응답 수. 여기서 429 비율을 본다 |
| `only_one_*_rate_limit_count` | 429 수. limiter가 실제로 동작한 횟수 |
| `only_one_*_server_error_count` | 5xx 수. limiter 대신 서버가 무너진 것이므로 **문제** |
| `only_one_*_network_error_count` | 연결 끊김(status 0). 역시 문제 |
| `only_one_*_request_duration` | 응답 지연 |

판정 기준은 단순하다. **429가 늘고 5xx·network error가 0이면 방어 성공**이다. 429 없이 200만 계속 나오면 limiter가 없거나 임계값이 너무 높은 것이고, 5xx가 섞이면 막기 전에 서버가 먼저 지친 것이다.
