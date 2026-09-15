# Load test: mixed-workload / single

- Generated: 2026-09-15T11:54:29.241Z
- Target RPS: 300
- Stage duration: 10m
- Source summary: C:/Project/ImHere/ImHereServer/loadtest/k6/generated/results/mixed-workload-single-20260915-204419.json

## Result

| Metric | Value |
|---|---:|
| HTTP requests | 179020 |
| HTTP request rate | 294.07025171845675 req/s |
| HTTP duration p50 | 7.75 ms |
| HTTP duration p95 | 15884.92 ms |
| HTTP duration p99 | 20716.80 ms |
| HTTP failed rate | 0.7439224667634902 |
| Max VUs | 1121 |
| Dropped iterations | 978 |

## Custom workload metrics

| Metric | Count / rate |
|---|---:|
| Measured HTTP requests | 179020 / 294.07025171845675 req/s |
| Server errors | 4605 / 7.564481673352103 req/s |
| Network errors | 128572 / 211.20098538680273 req/s |
| Conflicts | n/a / n/a req/s |

## Automated judgement

This file records measurements only. The AI optimization runner must combine it with Hikari, MySQL, nginx, and async-backlog snapshots before accepting a candidate.

