# Load test: mixed-workload / breakpoint

- Generated: 2026-09-15T12:28:07.865Z
- Target RPS: 300
- Stage duration: 10m
- Source summary: C:/Project/ImHere/ImHereServer/loadtest/k6/generated/results/mixed-workload-breakpoint-20260915-212429.json

## Result

| Metric | Value |
|---|---:|
| HTTP requests | 37069 |
| HTTP request rate | 170.89229550302588 req/s |
| HTTP duration p50 | 11.05 ms |
| HTTP duration p95 | 16994.84 ms |
| HTTP duration p99 | 19895.88 ms |
| HTTP failed rate | 0.5893873587094337 |
| Max VUs | 1059 |
| Dropped iterations | 949 |

## Custom workload metrics

| Metric | Count / rate |
|---|---:|
| Measured HTTP requests | 37069 / 170.89229550302588 req/s |
| Server errors | 940 / 4.333506643633341 req/s |
| Network errors | 20908 / 96.38825202668713 req/s |
| Conflicts | n/a / n/a req/s |

## Automated judgement

This file records measurements only. The AI optimization runner must combine it with Hikari, MySQL, nginx, and async-backlog snapshots before accepting a candidate.

