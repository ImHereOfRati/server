# Load test: mixed-workload / single

- Generated: 2026-09-15T10:44:30.429Z
- Target RPS: 300
- Stage duration: 10m
- Source summary: C:/Project/ImHere/ImHereServer/loadtest/k6/generated/results/mixed-workload-single-20260915-193424.json

## Result

| Metric | Value |
|---|---:|
| HTTP requests | 97810 |
| HTTP request rate | 161.54928337369657 req/s |
| HTTP duration p50 | 24.66 ms |
| HTTP duration p95 | 7440.90 ms |
| HTTP duration p99 | 9735.97 ms |
| HTTP failed rate | 0.11686944075247929 |
| Max VUs | 1114 |
| Dropped iterations | 1188 |

## Custom workload metrics

| Metric | Count / rate |
|---|---:|
| Measured HTTP requests | 97810 / 161.54928337369657 req/s |
| Server errors | 389 / 0.6424974055042221 req/s |
| Network errors | 11042 / 18.2376769963435 req/s |
| Conflicts | n/a / n/a req/s |

## Automated judgement

This file records measurements only. The AI optimization runner must combine it with Hikari, MySQL, nginx, and async-backlog snapshots before accepting a candidate.

