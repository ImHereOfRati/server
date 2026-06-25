# Signals

이 문서는 로그, 메트릭, 트레이스의 입력 경로, Alloy 내부 처리, Grafana Cloud 출력, 수집 제외 범위를 정리합니다.

## 로그

### 입력

- 입력 소스는 Docker 컨테이너의 `stdout` / `stderr`입니다.
- Alloy는 `discovery.docker`와 `loki.source.docker`로 Docker socket(`/var/run/docker.sock`)을 읽습니다.

### 필터링

- 외부 수집 대상은 `imhere_log_scope=external` 라벨이 붙은 컨테이너뿐입니다.
- 현재 `docker-compose.yml` 기준으로 `dsko`만 `external`이고, `nginx`, `alloy`는 `internal`입니다.
- 즉 Loki로 올라가는 컨테이너 로그는 사실상 `dsko` 로그입니다.

### 출력

- Alloy는 `loki.write "grafana_cloud"`를 통해 Grafana Cloud Loki로 보냅니다.
- 라벨에는 `job="imhere-server"`가 포함됩니다.
- 컨테이너명, stream(stdout/stderr), compose service도 relabel로 포함됩니다.

### 제외 범위

- `nginx`, `alloy` 컨테이너 로그는 Grafana Cloud 수집 대상이 아닙니다.
- 애플리케이션 파일 로그(`logs/imhere.log`)는 Docker stdout/stderr가 아니므로 Alloy가 수집하지 않습니다.

## 메트릭

### 입력

- 입력 소스는 Spring Boot actuator Prometheus endpoint입니다.
- Alloy는 `dsko:4861`에 붙습니다.
- scrape 경로는 `${MGMT_BASE_PATH}/prometheus`이고, 실제 값은 `prod.env`와 `application.yaml`이 맞춰 줍니다.

### 처리

- Alloy는 `prometheus.scrape "imhere"`로 30초마다 scrape 합니다.
- 이후 `prometheus.remote_write "grafana_cloud"`로 넘깁니다.

### 출력

- 최종 목적지는 Grafana Cloud Prometheus입니다.
- 인증 정보는 `GRAFANA_CLOUD_PROM_ENDPOINT`, `GRAFANA_CLOUD_PROM_USER`, `GRAFANA_CLOUD_PROM_API_KEY`를 Alloy가 직접 읽습니다.

### 제외 범위

- 앱은 metrics를 OTLP push 하지 않습니다.
- `management.otlp.metrics.export.enabled=false`(`application.yaml`)라서 metrics 경로는 scrape 단일 경로만 씁니다.
- 트레이스는 OTLP push인데 메트릭만 scrape로 둔 이유는, 메트릭은 시계열 누적값이라 Alloy가 일정 주기(30초)로 당기는 게 손실에 강하기 때문입니다. push면 앱 재기동 사이에 끊긴 구간이 그대로 빈 구멍이 되지만, scrape는 다음 주기에 actuator 현재값을 다시 읽어 자동 복구됩니다. 반면 트레이스는 개별 span 이벤트라 누적 개념이 없어 발생 시점에 바로 내보내는 push가 맞다고 판단하였습니다.

## 트레이스

### 입력

- 앱은 OTLP HTTP로 trace를 Alloy에 보냅니다.
- prod profile에서 endpoint는 `http://alloy:4318/v1/traces`입니다.
- Alloy는 `otelcol.receiver.otlp`에서 gRPC 4317, HTTP 4318을 모두 열어 둡니다.

### 처리

- 앱 -> Alloy 수신 -> `otelcol.exporter.otlp "grafanacloud"` 순서로 흐릅니다.
- 앱은 Grafana Cloud endpoint나 자격증명을 모릅니다.

### 출력

- 최종 목적지는 Grafana Cloud Tempo입니다.
- 인증 정보는 `GRAFANA_CLOUD_TEMPO_ENDPOINT`, `GRAFANA_CLOUD_TEMPO_USER`, `GRAFANA_CLOUD_TEMPO_API_KEY`를 Alloy가 직접 읽습니다.

### 상관관계

- 애플리케이션 로그 패턴에는 `traceId`, `spanId`가 포함됩니다.
- `support/logger/LoggingFilter.kt`는 요청마다 `traceId`를 MDC에 넣습니다.
- `RequestLogBuilder.kt`, `ErrorLogAppenderConfig.kt`, `application.yaml`의 log pattern이 이 값을 로그에 남깁니다.

## 왜 Alloy 단일 경로인가

- 앱에 Grafana Cloud 자격증명을 넣지 않기 위해서입니다.
- 로그, 메트릭, 트레이스를 한 컨테이너에서 일관되게 외부로 내보내기 위해서입니다.
- 운영에서는 app과 observability backend의 결합보다, app과 Alloy의 결합이 더 작고 관리가 쉽기 때문입니다.
