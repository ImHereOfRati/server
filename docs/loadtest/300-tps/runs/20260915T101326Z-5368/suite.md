# AI optimization suite

- Run ID: 20260915T101326Z-5368
- Target: 300 TPS for 10m
- Test: many-people-many-request-test/mixed-workload.js (single)

| Candidate | Result | Report | SQL report |
|---|---|---|---|
| latest | 99 / failed | [candidate-1-latest](candidate-1-latest/missing-k6-report.md) | collected (initial collector had empty sections; script fixed afterward) |

## Baseline interpretation

The single available image candidate (`latest`) did not meet the 300 TPS SLO. The run produced 162 req/s with 23.96% request failures and p95 latency of 11.28 s. No optimizer candidate was accepted; further candidates must be produced from SQL/connection evidence and rerun with the corrected report collector.
