# Candidate latest — baseline result

- Target: 300 TPS, 10 minute mixed workload
- Result: **FAIL** (k6 exit status 99; `http_req_failed` threshold crossed)
- Achieved request rate: 162.00 req/s
- Requests/checks: 98,225
- Failed requests: 23,535 (23.96%)
- Network errors: 22,389 (36.93/s)
- Server errors: 1,146 (1.89/s)
- Latency: p50 30.76 ms, p95 11.28 s, p99 17.85 s
- Dropped iterations: 773
- Maximum VUs: 1,051 (configured max 1,064)

The dominant failure signature was EOF and forcibly closed connections from the app endpoint. Removing test-only Nginx request/connection limits therefore exposed the application/database saturation rather than achieving the target. This is a baseline observation, not evidence that 300 TPS is safe.
