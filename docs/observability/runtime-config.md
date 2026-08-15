# Runtime Config

이 문서는 observability 관련 값이 어느 파일에 있고, 언제 주입되고, 누가 읽는지 정리한다.

## 소스 오브 트루스

관련 파일은 네 개다.

- `src/main/resources/application.yaml`
- `docker-compose.yml`
- `infra/alloy/alloy-config.alloy.template`
- `env/*.env` (이 레포에 없다. private config repo에 있고, 배포 시 `sync-config` job이 가져온다 — 아래 주입 흐름 참고.)

역할은 다르다.

- `application.yaml`
  - 앱의 actuator, trace export, logging 기본 동작
- `docker-compose.yml`
  - `prod` profile에서 `dsko`, `nginx`, `alloy`를 같은 네트워크에 띄우고,
    컨테이너별로 어떤 env 파일을 넣을지 정한다
- `alloy-config.alloy.template`
  - Alloy가 어떤 입력을 받아 어떤 Grafana Cloud signal로 보내는지 정의
- `env/*.env`
  - 운영 배포 시점의 실제 값

### 런타임 env는 한 파일이 아니다

예전에는 `prod.env` 한 파일을 세 컨테이너가 통째로 읽었다. 그래서 alloy
컨테이너 안에서도 `DB_PASSWORD`가 보였다. 지금은 관심사별로 쪼개고 컨테이너마다
필요한 것만 넣는다.

| 파일 | 담는 것 | 읽는 컨테이너 |
| --- | --- | --- |
| `env/app.env` | 프로파일, DB, JWT, 관리자/보안 | `dsko` |
| `env/web.env` | 도메인, TLS, CORS | `dsko`, `nginx` |
| `env/oidc.env` | 소셜 로그인 클라이언트 ID | `dsko` |
| `env/external.env` | Firebase, NCP/Naver, SOLAPI, Discord | `dsko` |
| `env/observability.env` | Grafana Cloud 자격증명 | `alloy` |

observability 관점에서 중요한 것은 마지막 줄이다. **Grafana Cloud 자격증명은
alloy에만 들어가고 앱에는 들어가지 않는다.** 파일이 나뉘어 있어서 이게 설정이
아니라 구조로 보장된다.

키 목록과 파일 추가 절차는 private config repo의 `env/` 구조와 `sync-config.sh`를 따른다.

## 주입 흐름

운영에서는 다음 순서로 연결된다.

1. `sync-config` job이 private config repo에서 `env/*.env` 전량과
   `imhereFirebaseKey.json`을 가져온다.
2. `deploy-app` job이 GitHub runner에서 `env/*.env`를 전부 `source`한다.
   어느 값이 어느 파일에 있는지 몰라도 되게 하려는 것이다.
3. 그 값으로 `nginx.conf.template`, `alloy-config.alloy.template`를 렌더링한다.
4. 렌더링된 파일과 `env/` 디렉터리를 EC2에 복사한다. 복사 전에 EC2의 기존
   `env/*.env`를 지운다 — config repo에서 사라진 파일이 유령으로 남지 않게.
5. `docker compose --profile prod`가 위 표대로 컨테이너별 `env_file`을 붙인다.
6. 배포가 끝나면 GitHub runner의 임시 파일(`/tmp/imhere-secrets`,
   `/tmp/imhere-runtime`)을 지운다. EC2의 `env/`는 컨테이너 재시작에 필요하므로
   남는다.

## 앱 설정

### actuator

- `management.server.port=4861`
  - API 포트 8080과 분리된 내부 관리 포트다.
- `management.endpoints.web.exposure.include=prometheus,health,info`
  - scrape와 기본 상태 확인에 필요한 것만 연다.
- `management.endpoints.web.base-path=${MGMT_BASE_PATH:/actuator}`
  - 로컬은 기본값 `/actuator`로 뜨고, 운영은 `env/app.env`의 난독화된 경로가 들어온다.
  - prod 문서가 이 키를 `${MGMT_BASE_PATH}`(기본값 없음)로 다시 선언한다. 운영에서
    변수가 빠지면 `/actuator`로 조용히 노출되는 대신 기동이 실패한다.

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

- `env/app.env`의 `MGMT_BASE_PATH`와 alloy template의 `metrics_path`가 일치하는가
- `dsko`, `nginx`, `alloy`가 각각 필요한 env 파일만 받고 있는가
  (`docker compose --profile prod config`로 확인)
- prod profile에서 앱 OTLP endpoint가 Alloy로 override 되었는가
- Grafana Cloud 자격증명이 app이 아니라 Alloy 쪽에만 있는가
  (`env/observability.env`가 `alloy` 서비스에만 붙어 있는가)
