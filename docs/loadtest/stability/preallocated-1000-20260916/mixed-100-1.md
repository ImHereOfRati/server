# Stability measurement

- Target: 100 RPS; workload: mixed
- Warmup: 120s + 30s grace; requested measurement: 600s
- Actual total run: 750.05s; measurement including drain: 600.05s
- Completed scheduled measurement: true
- Successful responses: 60000
- Successful responses / scheduled seconds: 100.00 req/s (aborted runs are not steady throughput estimates)
- HTTP failure rate: 0.000%
- p95: 24.13ms; p99: 52.33ms
- Dropped iterations: 0

## Minute windows

Completion-time windows relative to the first k6 point and configured measurement start. Final window can be partial or contain draining requests. Requests/60 is not an actual rate for a partial window.

| Minute | Requests | Successful | HTTP failures | p95 ms | Dropped |
|---|---:|---:|---:|---:|---:|
| 1 | 6000 | 6000 | 0 | 28.53 | 0 |
| 2 | 6001 | 6001 | 0 | 22.36 | 0 |
| 3 | 6000 | 6000 | 0 | 26.06 | 0 |
| 4 | 5999 | 5999 | 0 | 23.74 | 0 |
| 5 | 6001 | 6001 | 0 | 21.00 | 0 |
| 6 | 5999 | 5999 | 0 | 21.89 | 0 |
| 7 | 6000 | 6000 | 0 | 31.89 | 0 |
| 8 | 6000 | 6000 | 0 | 21.46 | 0 |
| 9 | 6001 | 6001 | 0 | 24.96 | 0 |
| 10 | 5999 | 5999 | 0 | 21.94 | 0 |
| 11 | 0 | 0 | 0 | 0.00 | 0 |

## Endpoint / status

| Endpoint / status | Count |
|---|---:|
| fcm-send / 202 | 9000 |
| friend-requests / 200 | 3000 |
| friendships / 200 | 12000 |
| notifications / 200 | 12000 |
| sms-send / 202 | 6000 |
| user-me / 200 | 12000 |
| user-search / 200 | 6000 |

## Network error codes

{}

Server observations and recovery must also pass before accepting a stable capacity.
