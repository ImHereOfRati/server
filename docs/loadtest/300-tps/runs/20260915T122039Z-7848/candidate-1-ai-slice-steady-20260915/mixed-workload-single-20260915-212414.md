# Load test: mixed-workload / single

- Generated: 2026-09-15T12:24:58.216Z
- Target RPS: 165
- Stage duration: 30s
- Source summary: C:/Project/ImHere/ImHereServer/loadtest/k6/generated/results/mixed-workload-single-20260915-212414.json

## Result

| Metric | Value |
|---|---:|
| HTTP requests | 3841 |
| HTTP request rate | 89.15629391255031 req/s |
| HTTP duration p50 | 4833.31 ms |
| HTTP duration p95 | 13716.26 ms |
| HTTP duration p99 | 14755.67 ms |
| HTTP failed rate | 0.4595157511064827 |
| Max VUs | 1032 |
| Dropped iterations | 1107 |

## Custom workload metrics

| Metric | Count / rate |
|---|---:|
| Measured HTTP requests | 3841 / 89.15629391255031 req/s |
| Server errors | 1727 / 40.0866752374315 req/s |
| Network errors | 38 / 0.8820461256643873 req/s |
| Conflicts | n/a / n/a req/s |

## Automated judgement

This file records measurements only. The AI optimization runner must combine it with Hikari, MySQL, nginx, and async-backlog snapshots before accepting a candidate.

