# Load test: mixed-workload / breakpoint

- Generated: 2026-09-15T12:17:12.920Z
- Target RPS: 300
- Stage duration: 10m
- Source summary: C:/Project/ImHere/ImHereServer/loadtest/k6/generated/results/mixed-workload-breakpoint-20260915-211326.json

## Result

| Metric | Value |
|---|---:|
| HTTP requests | 37215 |
| HTTP request rate | 165.44060280038505 req/s |
| HTTP duration p50 | 6031.09 ms |
| HTTP duration p95 | 13787.06 ms |
| HTTP duration p99 | 16538.94 ms |
| HTTP failed rate | 0.46086255542120114 |
| Max VUs | 1106 |
| Dropped iterations | 808 |

## Custom workload metrics

| Metric | Count / rate |
|---|---:|
| Measured HTTP requests | 37215 / 165.44060280038505 req/s |
| Server errors | 474 / 2.1071838164015184 req/s |
| Network errors | 16677 / 74.13819516060786 req/s |
| Conflicts | n/a / n/a req/s |

## Automated judgement

This file records measurements only. The AI optimization runner must combine it with Hikari, MySQL, nginx, and async-backlog snapshots before accepting a candidate.

