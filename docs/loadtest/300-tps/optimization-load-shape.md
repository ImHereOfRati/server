# 300 TPS load-shape correction

The successful Slice candidate had low latency and 0.28% errors, but its measured rate was 164.9 req/s. Investigation found that `ramping-arrival-rate` started at 30 req/s and ramped to 300 over the entire single stage, producing an average near 165 req/s. Single-target runs now start at `TARGET_RPS`, making the 300 TPS experiment steady-state and measurable.
