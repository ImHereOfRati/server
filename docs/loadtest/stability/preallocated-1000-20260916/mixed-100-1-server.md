# Server observations

- Samples: 47; missing essential values: false
- Hikari timeout delta: 0
- Maximum Hikari pending: 0
- Maximum incomplete events: 37
- Backlog zero observed after first recovery snapshot: 0.0s
- Recovery timestamps are relative to the first snapshot after k6 termination; polling/duration overhead must be considered at the 60s boundary.
- Executor queue column includes the scheduler; consult raw samples for individual executor queues.

| UTC | Phase | Events | Pending deliveries | Hikari active | Hikari pending | Timeouts | Process CPU | Host CPU | Lifecycle |
|---|---|---:|---:|---:|---:|---:|---:|---:|---|
| 2026-09-16T11:55:24.427Z | before | 0 | 0 | 0 | 0 | 0 | 0.000 | 0.750 | restart=0 oom=false status=running |
| 2026-09-16T11:55:28.048Z | load | 0 | 0 | 3 | 0 | 0 | 0.130 | 0.193 | restart=0 oom=false status=running |
| 2026-09-16T11:55:46.691Z | load | 1 | 0 | 3 | 0 | 0 | 0.667 | 0.705 | restart=0 oom=false status=running |
| 2026-09-16T11:56:05.482Z | load | 2 | 0 | 0 | 0 | 0 | 0.334 | 0.548 | restart=0 oom=false status=running |
| 2026-09-16T11:56:24.042Z | load | 2 | 1 | 0 | 0 | 0 | 0.334 | 0.489 | restart=0 oom=false status=running |
| 2026-09-16T11:56:42.459Z | load | 0 | 0 | 1 | 0 | 0 | 0.271 | 0.347 | restart=0 oom=false status=running |
| 2026-09-16T11:57:00.836Z | load | 0 | 0 | 0 | 0 | 0 | 0.222 | 0.285 | restart=0 oom=false status=running |
| 2026-09-16T11:57:19.253Z | load | 0 | 0 | 2 | 0 | 0 | 0.157 | 0.408 | restart=0 oom=false status=running |
| 2026-09-16T11:57:37.359Z | load | 0 | 0 | 0 | 0 | 0 | 0.004 | 0.046 | restart=0 oom=false status=running |
| 2026-09-16T11:57:55.706Z | load | 0 | 0 | 0 | 0 | 0 | 0.004 | 0.035 | restart=0 oom=false status=running |
| 2026-09-16T11:58:14.534Z | load | 30 | 2 | 4 | 0 | 0 | 0.553 | 0.752 | restart=0 oom=false status=running |
| 2026-09-16T11:58:33.398Z | load | 6 | 0 | 5 | 0 | 0 | 0.464 | 0.674 | restart=0 oom=false status=running |
| 2026-09-16T11:58:52.267Z | load | 11 | 2 | 4 | 0 | 0 | 0.385 | 0.621 | restart=0 oom=false status=running |
| 2026-09-16T11:59:11.038Z | load | 2 | 0 | 4 | 0 | 0 | 0.405 | 0.581 | restart=0 oom=false status=running |
| 2026-09-16T11:59:29.898Z | load | 8 | 1 | 4 | 0 | 0 | 0.345 | 0.506 | restart=0 oom=false status=running |
| 2026-09-16T11:59:48.739Z | load | 14 | 0 | 5 | 0 | 0 | 0.310 | 0.797 | restart=0 oom=false status=running |
| 2026-09-16T12:00:07.643Z | load | 37 | 2 | 4 | 0 | 0 | 0.437 | 0.677 | restart=0 oom=false status=running |
| 2026-09-16T12:00:26.378Z | load | 0 | 0 | 4 | 0 | 0 | 0.308 | 0.402 | restart=0 oom=false status=running |
| 2026-09-16T12:00:45.005Z | load | 6 | 0 | 4 | 0 | 0 | 0.315 | 0.395 | restart=0 oom=false status=running |
| 2026-09-16T12:01:03.950Z | load | 11 | 0 | 4 | 0 | 0 | 0.338 | 0.695 | restart=0 oom=false status=running |
| 2026-09-16T12:01:22.573Z | load | 1 | 1 | 3 | 0 | 0 | 0.276 | 0.361 | restart=0 oom=false status=running |
| 2026-09-16T12:01:41.256Z | load | 4 | 1 | 5 | 0 | 0 | 0.301 | 0.387 | restart=0 oom=false status=running |
| 2026-09-16T12:01:59.976Z | load | 5 | 0 | 5 | 0 | 0 | 0.300 | 0.404 | restart=0 oom=false status=running |
| 2026-09-16T12:02:18.706Z | load | 7 | 2 | 5 | 0 | 0 | 0.288 | 0.614 | restart=0 oom=false status=running |
| 2026-09-16T12:02:37.384Z | load | 14 | 0 | 5 | 0 | 0 | 0.257 | 0.390 | restart=0 oom=false status=running |
| 2026-09-16T12:02:56.082Z | load | 4 | 1 | 3 | 0 | 0 | 0.265 | 0.381 | restart=0 oom=false status=running |
| 2026-09-16T12:03:14.751Z | load | 0 | 0 | 4 | 0 | 0 | 0.281 | 0.476 | restart=0 oom=false status=running |
| 2026-09-16T12:03:33.400Z | load | 2 | 0 | 3 | 0 | 0 | 0.429 | 0.875 | restart=0 oom=false status=running |
| 2026-09-16T12:03:52.174Z | load | 9 | 0 | 3 | 0 | 0 | 0.257 | 0.479 | restart=0 oom=false status=running |
| 2026-09-16T12:04:10.993Z | load | 6 | 0 | 5 | 0 | 0 | 0.260 | 0.465 | restart=0 oom=false status=running |
| 2026-09-16T12:04:29.740Z | load | 2 | 1 | 1 | 0 | 0 | 0.244 | 0.444 | restart=0 oom=false status=running |
| 2026-09-16T12:04:48.461Z | load | 2 | 1 | 4 | 0 | 0 | 0.350 | 0.838 | restart=0 oom=false status=running |
| 2026-09-16T12:05:07.168Z | load | 15 | 0 | 3 | 0 | 0 | 0.316 | 0.486 | restart=0 oom=false status=running |
| 2026-09-16T12:05:25.897Z | load | 3 | 0 | 4 | 0 | 0 | 0.225 | 0.439 | restart=0 oom=false status=running |
| 2026-09-16T12:05:44.570Z | load | 3 | 1 | 2 | 0 | 0 | 0.229 | 0.426 | restart=0 oom=false status=running |
| 2026-09-16T12:06:03.418Z | load | 14 | 1 | 5 | 0 | 0 | 0.500 | 1.000 | restart=0 oom=false status=running |
| 2026-09-16T12:06:22.173Z | load | 2 | 1 | 2 | 0 | 0 | 0.297 | 0.484 | restart=0 oom=false status=running |
| 2026-09-16T12:06:40.895Z | load | 17 | 1 | 5 | 0 | 0 | 0.245 | 0.428 | restart=0 oom=false status=running |
| 2026-09-16T12:06:59.519Z | load | 4 | 0 | 3 | 0 | 0 | 0.230 | 0.412 | restart=0 oom=false status=running |
| 2026-09-16T12:07:18.159Z | load | 4 | 0 | 5 | 0 | 0 | 0.262 | 0.419 | restart=0 oom=false status=running |
| 2026-09-16T12:07:36.908Z | load | 10 | 0 | 4 | 0 | 0 | 0.262 | 0.451 | restart=0 oom=false status=running |
| 2026-09-16T12:07:55.621Z | load | 13 | 0 | 4 | 0 | 0 | 0.231 | 0.420 | restart=0 oom=false status=running |
| 2026-09-16T12:08:13.951Z | recovery | 0 | 0 | 0 | 0 | 0 | 0.003 | 0.032 | restart=0 oom=false status=running |
| 2026-09-16T12:08:32.228Z | recovery | 0 | 0 | 0 | 0 | 0 | 0.001 | 0.018 | restart=0 oom=false status=running |
| 2026-09-16T12:08:50.515Z | recovery | 0 | 0 | 0 | 0 | 0 | 0.002 | 0.084 | restart=0 oom=false status=running |
| 2026-09-16T12:08:54.101Z | recovery | 0 | 0 | 0 | 0 | 0 | 0.003 | 0.048 | restart=0 oom=false status=running |
| 2026-09-16T12:08:57.193Z | recovery | 0 | 0 | 0 | 0 | 0 | 0.003 | 0.056 | restart=0 oom=false status=running |
