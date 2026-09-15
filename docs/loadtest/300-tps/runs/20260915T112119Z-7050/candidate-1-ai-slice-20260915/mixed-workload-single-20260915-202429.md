# Load test: mixed-workload / single

- Generated: 2026-09-15T11:34:30.053Z
- Target RPS: 300
- Stage duration: 10m
- Source summary: C:/Project/ImHere/ImHereServer/loadtest/k6/generated/results/mixed-workload-single-20260915-202429.json

## Result

| Metric | Value |
|---|---:|
| HTTP requests | 98998 |
| HTTP request rate | 164.90104879352376 req/s |
| HTTP duration p50 | 18.19 ms |
| HTTP duration p95 | 51.85 ms |
| HTTP duration p99 | 300.42 ms |
| HTTP failed rate | 0.0028586436089617973 |
| Max VUs | 300 |
| Dropped iterations | n/a |

## Custom workload metrics

| Metric | Count / rate |
|---|---:|
| Measured HTTP requests | 98998 / 164.90104879352376 req/s |
| Server errors | 283 / 0.4713933292447042 req/s |
| Network errors | n/a / n/a req/s |
| Conflicts | n/a / n/a req/s |

## Automated judgement

This file records measurements only. The AI optimization runner must combine it with Hikari, MySQL, nginx, and async-backlog snapshots before accepting a candidate.

