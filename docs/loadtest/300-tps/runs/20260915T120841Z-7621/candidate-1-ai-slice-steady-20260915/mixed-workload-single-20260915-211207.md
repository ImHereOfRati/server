# Load test: mixed-workload / single

- Generated: 2026-09-15T12:15:15.993Z
- Target RPS: 195
- Stage duration: 3m
- Source summary: C:/Project/ImHere/ImHereServer/loadtest/k6/generated/results/mixed-workload-single-20260915-211207.json

## Result

| Metric | Value |
|---|---:|
| HTTP requests | 34345 |
| HTTP request rate | 183.50389764602787 req/s |
| HTTP duration p50 | 8.16 ms |
| HTTP duration p95 | 19916.67 ms |
| HTTP duration p99 | 23381.17 ms |
| HTTP failed rate | 0.6432377347503275 |
| Max VUs | 1048 |
| Dropped iterations | 753 |

## Custom workload metrics

| Metric | Count / rate |
|---|---:|
| Measured HTTP requests | 34345 / 183.50389764602787 req/s |
| Server errors | 2065 / 11.033208578804704 req/s |
| Network errors | 20027 / 107.00342286088222 req/s |
| Conflicts | n/a / n/a req/s |

## Automated judgement

This file records measurements only. The AI optimization runner must combine it with Hikari, MySQL, nginx, and async-backlog snapshots before accepting a candidate.

