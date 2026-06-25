# Runtime Config

이 문서는 observability 관련 값이 어느 파일에 있고, 언제 주입되고, 누가 읽는지 정리한다.

## 소스 오브 트루스

관련 파일은 네 개다.

- `src/main/resources/application.yaml`
- `docker-compose.yml`
- `infra/alloy/alloy-config.alloy.template`
- `prod.env` (이 레포에 없다. private config repo에 있고, 배포 시 `sync-config` job이 가져온다 — 아래 주입 흐름 참고. 레포에는 `prod.env.example`만 있다.)

역할은 다르다.

- `application.yaml`
  - 앱의 actuator, trace export, logging 기본 동작
- `docker-compose.yml`
  - `prod` profile에서 `dsko`, `nginx`, `alloy`를 같은 네트워크에 띄움
- `alloy-config.alloy.template`
  - Alloy가 어떤 입력을 받아 어떤 Grafana Cloud signal로 보내는지 정의
- `prod.env`
  - 운영 배포 시점의 실제 값

## 주입 흐름

운영에서는 다음 순서로 연결된다.

1. `sync-config` job이 private config repo에서 `prod.env`와 `imhereFirebaseKey.json`을 가져온다.
2. `deploy-app` job이 GitHub runner에서 `prod.env`를 `source`한다.
3. 그 값으로 `nginx.conf.template`, `alloy-config.alloy.template`를 렌더링한다.
4. `RABBITMQ_HOST`는 `prod.env` 원본에 없고 CloudFormation output으로 append 한다.
5. 렌더링된 파일과 `prod.env`를 EC2에 복사한다.
6. `docker compose --profile prod`가 `env_file: ./prod.env`로 `dsko`, `nginx`, `alloy`에 같은 값을 넣는다.
7. compose up 뒤 EC2에 복사한 `prod.env`와 렌더링 파일은 삭제한다.

## 앱 설정

### actuator

- `management.server.port=4861`
  - API 포트 8080과 분리된 내부 관리 포트다.
- `management.endpoints.web.exposure.include=prometheus,health,info`
  - scrape와 기본 상태 확인에 필요한 것만 연다.
- `management.endpoints.web.base-path=${MGMT_BASE_PATH}`
  - 로컬은 `application-local.yaml`, 운영은 `prod.env`에서 값이 들어온다.

### trace export

- 로컬 기본값은 `http://localhost:4318/v1/traces`
- prod profile override는 `http://alloy:4318/v1/traces`
- 이 override는 `SPRING_PROFILES_ACTIVE=prod`가 켜졌을 때만 적용된다.

즉 앱은 운영에서 "Alloy에게 보낸다"는 사실만 알고, Grafana Cloud 연결값은 모른다.

## Alloy 설정

### 로그

- Docker socket을 읽는다.
- `imhere_log_scope=external`만 keep 한다.
- Loki 인증은 `GRAFANA_CLOUD_LOKI_*`를 쓴다.

### 메트릭

- scrape target은 `dsko:4861`
- scrape path는 `${MGMT_BASE_PATH}/prometheus`
- Prometheus 인증은 `GRAFANA_CLOUD_PROM_*`를 쓴다.

### 트레이스

- OTLP receiver는 4317(gRPC), 4318(HTTP)
- Tempo 인증은 `GRAFANA_CLOUD_TEMPO_*`를 쓴다.

## 운영에서 같이 맞아야 하는 값

### `MGMT_BASE_PATH`

이 값은 세 군데가 동시에 맞아야 한다.

- `application.yaml`
- `infra/nginx/nginx.conf.template`
- `infra/alloy/alloy-config.alloy.template`

하나라도 다르면:

- nginx는 actuator 경로를 잘못 proxy할 수 있고
- Alloy는 메트릭 scrape에 실패할 수 있다

### `imhere_log_scope`

이 라벨은 `docker-compose.yml`에서만 정하지만, 로그 수집 범위를 사실상 결정한다.

- `external`이면 Loki 수집 대상
- `internal`이면 수집 제외

## 운영 점검 포인트

- `prod.env`의 `MGMT_BASE_PATH`와 alloy template의 `metrics_path`가 일치하는가
- `dsko`, `nginx`, `alloy`가 모두 같은 `prod.env`를 읽는가
- `RABBITMQ_HOST`가 deploy 단계에서 append 되었는가
- prod profile에서 앱 OTLP endpoint가 Alloy로 override 되었는가
- Grafana Cloud 자격증명이 app이 아니라 Alloy 쪽에만 있는가
