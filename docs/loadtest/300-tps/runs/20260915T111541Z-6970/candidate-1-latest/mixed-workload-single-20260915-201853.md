# Load test: mixed-workload / single

- Generated: 2026-09-15T11:28:54.563Z
- Target RPS: 300
- Stage duration: 10m
- Source summary: C:/Project/ImHere/ImHereServer/loadtest/k6/generated/results/mixed-workload-single-20260915-201853.json

## Result

| Metric | Value |
|---|---:|
| HTTP requests | 98998 |
| HTTP request rate | 164.97538201030994 req/s |
| HTTP duration p50 | 19.42 ms |
| HTTP duration p95 | 42.30 ms |
| HTTP duration p99 | 120.38 ms |
| HTTP failed rate | 0.002424291399826259 |
| Max VUs | 300 |
| Dropped iterations | n/a |

## Custom workload metrics

| Metric | Count / rate |
|---|---:|
| Measured HTTP requests | 98998 / 164.97538201030994 req/s |
| Server errors | 240 / 0.39994839979064617 req/s |
| Network errors | n/a / n/a req/s |
| Conflicts | n/a / n/a req/s |

## Automated judgement

This file records measurements only. The AI optimization runner must combine it with Hikari, MySQL, nginx, and async-backlog snapshots before accepting a candidate.

