# Server observations

- Samples: 46; missing essential values: false
- Hikari timeout delta: 0
- Maximum Hikari pending: 1
- Maximum incomplete events: 138
- Backlog zero observed after first recovery snapshot: 0.0s
- Recovery timestamps are relative to the first snapshot after k6 termination; polling/duration overhead must be considered at the 60s boundary.
- Executor queue column includes the scheduler; consult raw samples for individual executor queues.

| UTC | Phase | Events | Pending deliveries | Hikari active | Hikari pending | Timeouts | Process CPU | Host CPU | Lifecycle |
|---|---|---:|---:|---:|---:|---:|---:|---:|---|
| 2026-09-16T12:12:47.771Z | before | 0 | 0 | 0 | 0 | 0 | 1.000 | 0.000 | restart=0 oom=false status=running |
| 2026-09-16T12:12:51.409Z | load | 0 | 0 | 7 | 1 | 0 | 0.147 | 0.183 | restart=0 oom=false status=running |
| 2026-09-16T12:13:10.067Z | load | 2 | 0 | 4 | 0 | 0 | 0.603 | 0.711 | restart=0 oom=false status=running |
| 2026-09-16T12:13:28.712Z | load | 2 | 1 | 4 | 0 | 0 | 0.359 | 0.426 | restart=0 oom=false status=running |
| 2026-09-16T12:13:47.383Z | load | 0 | 0 | 3 | 0 | 0 | 0.268 | 0.347 | restart=0 oom=false status=running |
| 2026-09-16T12:14:05.897Z | load | 0 | 0 | 3 | 0 | 0 | 0.336 | 0.492 | restart=0 oom=false status=running |
| 2026-09-16T12:14:24.624Z | load | 0 | 0 | 0 | 0 | 0 | 0.217 | 0.313 | restart=0 oom=false status=running |
| 2026-09-16T12:14:43.143Z | load | 0 | 0 | 0 | 0 | 0 | 0.231 | 0.300 | restart=0 oom=false status=running |
| 2026-09-16T12:15:01.573Z | load | 0 | 0 | 0 | 0 | 0 | 0.038 | 0.069 | restart=0 oom=false status=running |
| 2026-09-16T12:15:19.974Z | load | 0 | 0 | 0 | 0 | 0 | 0.015 | 0.127 | restart=0 oom=false status=running |
| 2026-09-16T12:15:39.166Z | load | 138 | 3 | 12 | 0 | 0 | 0.739 | 0.941 | restart=0 oom=false status=running |
| 2026-09-16T12:15:58.373Z | load | 14 | 3 | 10 | 0 | 0 | 0.733 | 0.932 | restart=0 oom=false status=running |
| 2026-09-16T12:16:17.359Z | load | 10 | 0 | 12 | 0 | 0 | 0.714 | 0.904 | restart=0 oom=false status=running |
| 2026-09-16T12:16:36.433Z | load | 11 | 3 | 11 | 0 | 0 | 0.586 | 0.882 | restart=0 oom=false status=running |
| 2026-09-16T12:16:55.406Z | load | 6 | 3 | 3 | 0 | 0 | 0.612 | 0.822 | restart=0 oom=false status=running |
| 2026-09-16T12:17:14.335Z | load | 20 | 1 | 9 | 0 | 0 | 0.619 | 0.827 | restart=0 oom=false status=running |
| 2026-09-16T12:17:33.365Z | load | 2 | 0 | 7 | 0 | 0 | 0.458 | 0.658 | restart=0 oom=false status=running |
| 2026-09-16T12:17:52.356Z | load | 31 | 1 | 12 | 0 | 0 | 0.557 | 0.800 | restart=0 oom=false status=running |
| 2026-09-16T12:18:11.450Z | load | 9 | 1 | 10 | 0 | 0 | 0.471 | 0.692 | restart=0 oom=false status=running |
| 2026-09-16T12:18:30.431Z | load | 11 | 1 | 7 | 0 | 0 | 0.448 | 0.658 | restart=0 oom=false status=running |
| 2026-09-16T12:18:49.462Z | load | 19 | 2 | 11 | 0 | 0 | 0.415 | 0.939 | restart=0 oom=false status=running |
| 2026-09-16T12:19:08.458Z | load | 5 | 2 | 2 | 0 | 0 | 0.446 | 0.682 | restart=0 oom=false status=running |
| 2026-09-16T12:19:27.419Z | load | 10 | 1 | 8 | 0 | 0 | 0.430 | 0.652 | restart=0 oom=false status=running |
| 2026-09-16T12:19:46.429Z | load | 9 | 1 | 10 | 0 | 0 | 0.395 | 0.607 | restart=0 oom=false status=running |
| 2026-09-16T12:20:06.140Z | load | 22 | 2 | 9 | 0 | 0 | 0.545 | 0.898 | restart=0 oom=false status=running |
| 2026-09-16T12:20:25.114Z | load | 7 | 0 | 10 | 0 | 0 | 0.455 | 0.684 | restart=0 oom=false status=running |
| 2026-09-16T12:20:44.085Z | load | 25 | 3 | 16 | 0 | 0 | 0.422 | 0.652 | restart=0 oom=false status=running |
| 2026-09-16T12:21:03.174Z | load | 11 | 2 | 11 | 0 | 0 | 0.426 | 0.650 | restart=0 oom=false status=running |
| 2026-09-16T12:21:22.146Z | load | 5 | 1 | 11 | 0 | 0 | 0.437 | 0.701 | restart=0 oom=false status=running |
| 2026-09-16T12:21:41.228Z | load | 5 | 2 | 10 | 0 | 0 | 0.418 | 0.643 | restart=0 oom=false status=running |
| 2026-09-16T12:22:00.208Z | load | 11 | 1 | 8 | 0 | 0 | 0.406 | 0.631 | restart=0 oom=false status=running |
| 2026-09-16T12:22:19.153Z | load | 2 | 1 | 9 | 0 | 0 | 0.433 | 0.862 | restart=0 oom=false status=running |
| 2026-09-16T12:22:38.180Z | load | 4 | 1 | 10 | 0 | 0 | 0.407 | 0.646 | restart=0 oom=false status=running |
| 2026-09-16T12:22:57.219Z | load | 14 | 2 | 10 | 0 | 0 | 0.415 | 0.634 | restart=0 oom=false status=running |
| 2026-09-16T12:23:16.290Z | load | 20 | 3 | 12 | 0 | 0 | 0.458 | 0.685 | restart=0 oom=false status=running |
| 2026-09-16T12:23:35.187Z | load | 4 | 0 | 10 | 0 | 0 | 0.439 | 0.749 | restart=0 oom=false status=running |
| 2026-09-16T12:23:54.246Z | load | 13 | 1 | 8 | 0 | 0 | 0.431 | 0.679 | restart=0 oom=false status=running |
| 2026-09-16T12:24:13.201Z | load | 11 | 2 | 10 | 0 | 0 | 0.438 | 0.658 | restart=0 oom=false status=running |
| 2026-09-16T12:24:32.223Z | load | 7 | 1 | 10 | 0 | 0 | 0.401 | 0.618 | restart=0 oom=false status=running |
| 2026-09-16T12:24:51.247Z | load | 26 | 2 | 10 | 0 | 0 | 0.560 | 0.824 | restart=0 oom=false status=running |
| 2026-09-16T12:25:10.224Z | load | 9 | 4 | 8 | 0 | 0 | 0.446 | 0.686 | restart=0 oom=false status=running |
| 2026-09-16T12:25:28.639Z | recovery | 0 | 0 | 0 | 0 | 0 | 0.091 | 0.176 | restart=0 oom=false status=running |
| 2026-09-16T12:25:46.967Z | recovery | 0 | 0 | 0 | 0 | 0 | 0.001 | 0.021 | restart=0 oom=false status=running |
| 2026-09-16T12:26:05.262Z | recovery | 0 | 0 | 0 | 0 | 0 | 0.002 | 0.102 | restart=0 oom=false status=running |
| 2026-09-16T12:26:17.555Z | recovery | 0 | 0 | 0 | 0 | 0 | 0.001 | 0.023 | restart=0 oom=false status=running |
| 2026-09-16T12:26:20.660Z | recovery | 0 | 0 | 0 | 0 | 0 | 0.008 | 0.063 | restart=0 oom=false status=running |
