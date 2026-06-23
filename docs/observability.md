# Observability

EC2에 같이 떠 있는 **Grafana Alloy** 컨테이너가 로그/메트릭/트레이스를 모아 Grafana Cloud로 보낸다(`alloy-config.alloy`).

## 파이프라인

```
[Docker 컨테이너 stdout/stderr] ──discovery.docker──▶ [Alloy] ──basic auth──▶ Grafana Cloud Loki (로그)
[dsko:4861 /.../prometheus]      ──prometheus.scrape──▶ [Alloy] ──remote_write──▶ Grafana Cloud Prometheus (메트릭)
[Spring Boot OTLP :4317/:4318]   ──otelcol.receiver────▶ [Alloy] ──otlp export──▶ Grafana Cloud Tempo (트레이스)
```

- **로그**: `discovery.docker` + `loki.source.docker`가 `/var/run/docker.sock`으로 모든 컨테이너의 stdout/stderr를 읽어 `job="imhere-server"` 라벨로 Loki에 보낸다.
- **메트릭**: `prometheus.scrape`가 `dsko:4861`의 actuator prometheus 엔드포인트를 스크랩해 remote-write 한다. 4861은 `application-monitoring.yaml`의 `management.server.port` — 운영 API 포트(8080)와 분리되어 있다.
- **트레이스**: Alloy가 OTLP gRPC(4317)/HTTP(4318)로 트레이스를 받아 Tempo로 보낸다.

트레이스는 두 경로가 동시에 설정되어 있다: Spring Boot 앱이 `otel.exporter.otlp.endpoint`로 Grafana Cloud Tempo에 직접 보내는 경로와, Alloy가 OTLP(4317/4318)를 받아 Tempo로 다시 보내는 경로.

## Spring Boot 쪽 설정 (`application-monitoring.yaml`)

- 운영 포트 분리: API `8080` / actuator `4861`.
- `management.endpoints.web.exposure.include`: `prometheus,health,info`만 노출 — 그 외(`/env`, `/beans` 등)는 막혀 있다.
- actuator의 base-path는 추측 가능한 `/actuator`가 아니라 무작위 문자열로 가려져 있다(보안 난독화) — 실제 경로는 설정 파일에서만 확인한다.
- `health.show-details: always` — 헬스체크 응답에 DB/Redis/RabbitMQ 등 세부 컴포넌트 상태까지 포함.
- `management.otlp.metrics.export.enabled: false` — 메트릭은 OTLP push가 아니라 Prometheus 스크랩 방식만 쓴다.

## Alert: Discord

별도 알림 서비스(PagerDuty 등) 없이 **Discord Webhook 3개**로 운영 알림을 보낸다(`DiscordMessageSender`).

| Webhook | 용도 | 호출 위치 |
|---|---|---|
| 서버 에러용 | 서버 측 예외 발생 알림 | `GlobalExceptionHandler` → `DiscordUserErrorNotifier` |
| 클라이언트 에러용 | 클라이언트(4xx) 에러 알림 | `GlobalExceptionHandler` |
| OTT용 | 관리자 OTT 로그인 토큰 전달 | `ImHereOttSuccessHandler` |

- OTT 토큰 값 자체가 Discord로 전송된다 — 로그나 응답 바디에는 노출되지 않는다. 자세한 흐름은 [security.md](./security.md#admin-ott-one-time-token-로그인).
- Webhook 자체에는 별도 인증이 없다 — URL이 노출되면 누구나 그 채널에 메시지를 보낼 수 있다. 노출 시 대응은 [security.md](./security.md#토큰-보안-원칙).
