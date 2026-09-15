# Safe throughput breakpoint run

The breakpoint runner now accepts `BREAKPOINT_STAGE_DURATION` so short, staged experiments can locate the connection-exhaustion boundary without changing pool limits. The first run will use 30-second stages for 30, 60, 100, 150, 225, and 300 TPS, then stop analysis at the first stage exceeding the failure/latency SLO.
