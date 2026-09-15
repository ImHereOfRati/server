# Load test: mixed-workload / breakpoint

- Generated: 2026-09-15T12:06:53.284Z
- Target RPS: 300
- Stage duration: 30s
- Source summary: C:/Project/ImHere/ImHereServer/loadtest/k6/generated/results/mixed-workload-breakpoint-20260915-210146.json

## Result

| Metric | Value |
|---|---:|
| HTTP requests | 94205 |
| HTTP request rate | 307.9957903238039 req/s |
| HTTP duration p50 | 9.25 ms |
| HTTP duration p95 | 10148.89 ms |
| HTTP duration p99 | 12355.61 ms |
| HTTP failed rate | 0.6482033862321532 |
| Max VUs | 1189 |
| Dropped iterations | 1186 |

## Custom workload metrics

| Metric | Count / rate |
|---|---:|
| Measured HTTP requests | 94205 / 307.9957903238039 req/s |
| Server errors | 392 / 1.2816129696611762 req/s |
| Network errors | 60672 / 198.36230126347675 req/s |
| Conflicts | n/a / n/a req/s |

## Automated judgement

This file records measurements only. The AI optimization runner must combine it with Hikari, MySQL, nginx, and async-backlog snapshots before accepting a candidate.

