# Stability measurement

- Target: 165 RPS; workload: mixed
- Warmup: 120s + 30s grace; requested measurement: 600s
- Actual total run: 750.06s; measurement including drain: 600.06s
- Completed scheduled measurement: true
- Successful responses: 99000
- Successful responses / scheduled seconds: 165.00 req/s (aborted runs are not steady throughput estimates)
- HTTP failure rate: 0.000%
- p95: 45.76ms; p99: 148.56ms
- Dropped iterations: 0

## Minute windows

Completion-time windows relative to the first k6 point and configured measurement start. Final window can be partial or contain draining requests. Requests/60 is not an actual rate for a partial window.

| Minute | Requests | Successful | HTTP failures | p95 ms | Dropped |
|---|---:|---:|---:|---:|---:|
| 1 | 9901 | 9901 | 0 | 53.85 | 0 |
| 2 | 9899 | 9899 | 0 | 82.63 | 0 |
| 3 | 9898 | 9898 | 0 | 35.17 | 0 |
| 4 | 9902 | 9902 | 0 | 29.14 | 0 |
| 5 | 9901 | 9901 | 0 | 35.25 | 0 |
| 6 | 9899 | 9899 | 0 | 37.76 | 0 |
| 7 | 9901 | 9901 | 0 | 68.12 | 0 |
| 8 | 9899 | 9899 | 0 | 34.42 | 0 |
| 9 | 9899 | 9899 | 0 | 31.78 | 0 |
| 10 | 9901 | 9901 | 0 | 59.49 | 0 |

## Endpoint / status

| Endpoint / status | Count |
|---|---:|
| fcm-send / 202 | 14852 |
| friend-requests / 200 | 4951 |
| friendships / 200 | 19799 |
| notifications / 200 | 19804 |
| sms-send / 202 | 9900 |
| user-me / 200 | 19792 |
| user-search / 200 | 9902 |

## Network error codes

{}

Server observations and recovery must also pass before accepting a stable capacity.
