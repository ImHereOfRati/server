# Load test: mixed-workload / single

- Generated: 2026-09-15T11:09:59.119Z
- Target RPS: 300
- Stage duration: 10m
- Source summary: C:/Project/ImHere/ImHereServer/loadtest/k6/generated/results/mixed-workload-single-20260915-195952.json

## Result

| Metric | Value |
|---|---:|
| HTTP requests | 98086 |
| HTTP request rate | 161.89956546574888 req/s |
| HTTP duration p50 | 25.75 ms |
| HTTP duration p95 | 8319.03 ms |
| HTTP duration p99 | 12363.85 ms |
| HTTP failed rate | 0.14346593805436045 |
| Max VUs | 1089 |
| Dropped iterations | 913 |

## Custom workload metrics

| Metric | Count / rate |
|---|---:|
| Measured HTTP requests | 98086 / 161.89956546574888 req/s |
| Server errors | 529 / 0.8731610029094994 req/s |
| Network errors | 13543 / 22.353912027227505 req/s |
| Conflicts | n/a / n/a req/s |

## Automated judgement

This file records measurements only. The AI optimization runner must combine it with Hikari, MySQL, nginx, and async-backlog snapshots before accepting a candidate.

