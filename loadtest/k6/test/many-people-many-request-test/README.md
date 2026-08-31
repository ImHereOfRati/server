# many-people-many-request-test — 여러 사람이 여러 요청

fixture의 **1,000명이 각자** 요청을 보낸다. 실제 트래픽에 가까운 모양으로 서버 전체 처리량의 한계를 찾는다. rate limit이 아니라 **DB 커넥션 풀·CPU·응답 지연**이 먼저 걸리는 구간을 본다.

스크립트는 `mixed-workload.js` 하나이고, 어떤 요청을 섞을지는 `SCENARIO`, 얼마나 올릴지는 `TEST_PLAN`으로 정한다.

## SCENARIO — 무엇을 섞는가

| 값 | 구성 |
|---|---|
| `mixed` (기본) | 읽기 75% + FCM 발송 15% + SMS 발송 10% |
| `reads` | 읽기 전용 |
| `friends` | 읽기 전용 (친구 요청 쓰기는 검증 리소스 제약으로 제외) |
| `fcm` | FCM 발송만 |
| `sms` | SMS 발송만 |
| `breakpoint` | 읽기 50% + FCM 25% + SMS 25% |

읽기 워크로드 안에서도 요청이 갈린다. `/api/users/my` 20%, `/api/users?keyword=` 10%, `/api/friendships` 20%, `/api/friends/requests` 10%, `/api/notifications` 40%.

> `friends`가 읽기 전용인 이유는 `generate-test-data.mjs`의 `mutationPairCount`가 `0`이기 때문이다. 친구 요청 생성·수락 트래픽을 넣으려면 그 값부터 올려야 한다.

## TEST_PLAN — 얼마나 올리는가

| 값 | 단계 |
|---|---|
| `precision` (기본) | 100 → 1000 RPS를 100씩, 각 3분 (10단계) |
| `breakpoint` | 30 → 1000 RPS를 비균등 배수로, 각 3분 (10단계) |
| `single` | `TARGET_RPS` 한 구간만 `STAGE_DURATION` 동안 |

`precision`은 한계 부근을 촘촘히 재고, `breakpoint`는 낮은 구간을 빨리 지나 무너지는 지점을 빨리 찾는다. 한 조건만 재현할 때는 `single`을 쓴다.

`http_req_failed` threshold가 `rate<0.01`로 걸려 있다. 단 `breakpoint`는 **무너지는 지점을 찾는 게 목적**이므로 threshold를 비운다.

## 실행

```bash
# 100~1000 RPS 정밀 측정
./loadtest/run-k6.sh --scenario mixed --test-plan precision --base-url 'http://<app-public-ip>'

# 단일 구간 재현
./loadtest/run-k6.sh --scenario fcm --test-plan single --target-rps 100 --stage-duration 3m \
  --base-url 'http://<app-public-ip>'
```

## 지표 읽는 법

**k6의 `iteration/s`를 RPS로 읽으면 안 된다.** 한 iteration이 HTTP 요청을 여러 번 낼 수 있다. 실제 처리량은 `http_reqs` 또는 `loadtest_http_request_count`로 본다.

| 메트릭 | 의미 |
|---|---|
| `loadtest_http_request_count{endpoint}` | endpoint별 실제 HTTP 요청 수 |
| `loadtest_response_count{endpoint,status}` | endpoint·상태 코드별 응답 수 |
| `loadtest_conflict_count` | 409. 데이터·동시성 충돌 |
| `loadtest_server_error_count` | 5xx. 서버 처리 실패 |
| `loadtest_network_error_count` | status 0. 연결 끊김·타임아웃 |
| `loadtest_business_duration{endpoint}` | endpoint별 응답 지연 |
| `loadtest_iteration_count{scenario}` | iteration 수 (보조 지표) |

409와 5xx를 **하나의 오류율로 묶지 않는다.** 409는 동시성 충돌이라 부하 상황에서 자연스럽게 늘지만, 5xx와 network error는 서버가 못 버틴 것이다.

`THINK_TIME`(기본 `0.05`초)으로 iteration 사이 간격을 조절한다. 0에 가까울수록 공격적이다.
