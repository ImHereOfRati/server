---
# 수정 필요
---
---

# Deployment and Operation

이 문서는 `C:\Project\ImHere` 작업 루트의 실제 저장소 내용(`ImHereServer/`, `client/`, `remoteConfig/`)을 직접 탐색해 작성한 **전체 배포·운영 문서**다.

서버 애플리케이션 내부 구조뿐 아니라 Flutter Mobile App, Web App, GitHub Actions, Gabia DNS, AWS CloudFront·S3·ECR·EC2, TLS 인증서, 외부 서비스와 운영 관측까지 전체 경로를 다룬다.

## 전체 시스템·배포 구조

```mermaid
flowchart LR
    MOBILE["Flutter Mobile App<br/><br/>기본 Origin:<br/>https://imhere.ratiko.co.kr<br/><br/>API 호출:<br/>/api/*"]
    WEBUSER["Web Browser<br/><br/>Landing Page:<br/>https://imhere.ratiko.co.kr/<br/><br/>Web App:<br/>https://imhere.ratiko.co.kr/app"]

    subgraph DNS["Gabia DNS"]
        DNS_WEB["imhere.ratiko.co.kr<br/><br/>CloudFront Alias로 연결"]
        DNS_API["ratiko.co.kr<br/><br/>EC2 Public Origin으로 연결"]
    end

    MOBILE -->|"HTTPS 요청"| DNS_WEB
    WEBUSER -->|"HTTPS 요청"| DNS_WEB

    subgraph GITHUB["GitHub"]
        subgraph BACKEND_REPO["Backend Repository"]
            BE_REPO["Spring Boot Backend<br/>Dockerfile / Docker Compose<br/>Infrastructure Scripts"]
        end

        subgraph MOBILE_REPO["Mobile Repository"]
            MOBILE_REPO_NODE["Flutter App<br/>IMHERE_ORIGIN 기본값:<br/>https://imhere.ratiko.co.kr"]
        end

        subgraph WEB_REPO["Web Repository"]
            WEB_REPO_NODE["React Web App<br/>Landing Page / App Build"]
        end

        subgraph ACTIONS["GitHub Actions"]
            BE_GA["Backend CI/CD<br/><br/>1. Build & Test<br/>2. Assume AWS Role via OIDC<br/>3. Push Image to ECR<br/>4. Deploy to EC2<br/>5. Run Certbot<br/>6. Health Check"]
            WEB_GA["Web Deployment<br/><br/>1. Build Web App<br/>2. Upload Static Files to S3<br/>3. Invalidate CloudFront<br/>4. Update Firebase Remote Config"]
        end
    end

    BE_REPO --> BE_GA
    WEB_REPO_NODE --> WEB_GA
    MOBILE_REPO_NODE -.->|"빌드 시 Origin 설정"| MOBILE

    subgraph AWS["AWS"]
        subgraph IAM["AWS IAM"]
            OIDC_PROVIDER["GitHub Actions OIDC Provider"]
            OIDC_ROLE["GitHub Actions OIDC Role<br/><br/>장기 AWS Access Key 없이<br/>GitHub Actions가 임시 권한 획득"]
        end

        subgraph ECR["Amazon ECR"]
            ECR_IMAGE["imhere/dsko<br/><br/>Spring Boot Docker Image<br/>태그: Commit / Release 기준"]
        end

        BE_GA -->|"OIDC Token"| OIDC_PROVIDER
        OIDC_PROVIDER -->|"AssumeRoleWithWebIdentity"| OIDC_ROLE
        OIDC_ROLE -->|"ecr:PushImage<br/>EC2 Deploy Permissions"| ECR_IMAGE

        subgraph WEB_DISTRIBUTION["Web Distribution"]
            ACM["AWS Certificate Manager<br/><br/>인증서 도메인:<br/>imhere.ratiko.co.kr<br/><br/>CloudFront용 인증서는<br/>us-east-1 리전에 존재"]
            CF["Amazon CloudFront<br/><br/>Public Entry Point<br/>imhere.ratiko.co.kr<br/><br/>Viewer HTTPS 종료 지점<br/>TLS 1.2 이상"]

            subgraph S3["Amazon S3"]
                S3_BUCKET["Private Web Bucket<br/><br/>Landing Static Files<br/>React App<br/>Immutable Releases"]
                OAC["Origin Access Control<br/><br/>CloudFront만 S3 객체를<br/>SigV4로 읽을 수 있음"]
            end

            ACM -->|"Viewer Certificate"| CF
            OAC -->|"Private S3 Access"| S3_BUCKET
            CF -->|"S3 Origin<br/>OAC / SigV4"| S3_BUCKET
        end

        subgraph VPC["AWS VPC"]
            subgraph PUBLIC_SUBNET["Public Subnet"]
                EIP["Elastic IP<br/><br/>EC2에 연결된 고정 Public IP"]

                subgraph EC2["EC2 t3.small"]
                    subgraph DOCKER["Docker Compose"]
                        NGINX["nginx Container<br/><br/>Public HTTPS Endpoint<br/>TLS 종료<br/>HTTP → HTTPS Redirect<br/>Reverse Proxy<br/>CORS 처리"]
                        SPRING["Spring Boot Container<br/>container_name:<br/>iamhere-server-container<br/><br/>Application Port: 8080<br/>Business API / Auth / Admin"]
                        ALLOY["Grafana Alloy Container<br/><br/>Application Logs<br/>Metrics<br/>Traces 수집"]
                        NGINX -->|"Docker Network<br/>HTTP :8080"| SPRING
                        SPRING -->|"Logs / Metrics / Traces"| ALLOY
                    end

                    subgraph HOST_TLS["EC2 Host TLS Storage"]
                        LETSENCRYPT["Let's Encrypt Certificate<br/><br/>도메인:<br/>ratiko.co.kr<br/>www.ratiko.co.kr<br/><br/>/etc/letsencrypt/live/<br/>${CERT_DOMAIN}/"]
                        CERTBOT["Certbot<br/><br/>HTTP-01 Webroot Challenge<br/>인증서 발급 / 갱신<br/>배포 후 Nginx Reload"]
                    end

                    LETSENCRYPT -->|"Read-only Volume Mount"| NGINX
                    CERTBOT -->|"Certificate Issue / Renew"| LETSENCRYPT
                end

                SG["Security Group<br/><br/>허용 포트:<br/>443 HTTPS<br/>80 ACME Challenge / Redirect<br/>SSH 제한적 접근"]
            end
        end
    end

    DNS_WEB -->|"Alias / CNAME"| CF
    WEBUSER -->|"HTTPS<br/>ACM 인증서 검증"| CF
    MOBILE -->|"HTTPS<br/>ACM 인증서 검증"| CF
    WEB_GA -->|"Static Files / Releases 업로드"| S3_BUCKET
    WEB_GA -->|"Distribution Invalidation"| CF

    DNS_API -->|"ratiko.co.kr resolves to"| EIP
    EIP -->|"Port 443"| NGINX
    SG -.->|"Network Access Control"| EC2
    CF -->|"/api/*<br/>HTTPS Origin<br/>Origin Host: ratiko.co.kr<br/>TLS 1.2"| DNS_API
    MOBILE -.->|"직접 EIP 접근하지 않음"| EIP

    ECR_IMAGE -->|"docker pull<br/>EC2가 새 이미지 다운로드"| SPRING
    OIDC_ROLE -->|"SSH Deploy Permission"| EC2
    BE_GA -->|"docker login / pull / compose up"| EC2
    BE_GA -->|"infra/scripts/deploy-imhere.sh"| CERTBOT
    BE_GA -->|"Application Health Check"| SPRING

    subgraph EXTERNAL["External Services"]
        MYSQL["Gabia External MySQL<br/><br/>Spring Boot JDBC Connection<br/>사용자 / 친구 / 위치 / 인증 데이터"]
        FCM["Firebase Cloud Messaging<br/><br/>Push Notification"]
        FIREBASE_RC["Firebase Remote Config<br/><br/>base_url<br/>web_app_url<br/>앱 최소 버전 등"]
        SOLAPI["Solapi<br/><br/>SMS / 알림 발송"]
        OIDC["Google / Kakao / Apple<br/>OIDC Provider<br/><br/>소셜 로그인 인증"]
        NAVER["Naver API<br/><br/>지도 / 장소 / 검색 API"]
        GRAFANA["Grafana Cloud<br/><br/>Alloy가 전송한<br/>Logs / Metrics / Traces"]
    end

    SPRING -->|"JDBC / TCP"| MYSQL
    SPRING -->|"HTTPS"| FCM
    SPRING -->|"HTTPS"| SOLAPI
    SPRING -->|"HTTPS"| OIDC
    SPRING -->|"HTTPS"| NAVER
    ALLOY -->|"OTLP / HTTPS"| GRAFANA
    MOBILE -->|"HTTPS Firebase SDK"| FIREBASE_RC
    FIREBASE_RC -->|"base_url 반환<br/>기본값: imhere.ratiko.co.kr"| MOBILE

    subgraph TLS_FLOW["TLS 인증서 적용 위치"]
        TLS_1["1. Mobile / Browser → CloudFront<br/><br/>ACM 인증서<br/>imhere.ratiko.co.kr"]
        TLS_2["2. CloudFront → EC2 Nginx<br/><br/>HTTPS Origin<br/>ratiko.co.kr 인증서 검증"]
        TLS_3["3. EC2 Nginx → Spring Boot<br/><br/>Docker 내부 네트워크 HTTP<br/>외부에 직접 노출되지 않음"]
    end

    TLS_1 --> TLS_2 --> TLS_3
```
저장소에 파일과 설정이 존재하는 것과, 실제 운영 환경에서 그 동작을 검증한 것을 구분해서 표기한다.

**용어 규칙**

| 표기 | 의미 |
| --- | --- |
| 구성됨 | 저장소의 workflow / Dockerfile / compose / 스크립트 / Spring 설정에 흐름이 정의되어 있음 |
| 실행 기록 있음 | 저장소 문서에 특정 CI/CD 실행 번호나 외부 endpoint 응답 확인이 기록되어 있음 |
| 미검증 | 저장소만으로는 실제 운영 상황에서 확인한 근거를 찾을 수 없음 |

**저장소에서 확인한 실행 기록 (유일한 검증 근거)**

`BACKEND-DEPLOY-PHASE0.md`(작성일 2026-08-09)와 `BACKEND-DEPLOY-VALUES.md`에 다음이 기록되어 있다.

- CI 실행 `31315652758`, CD 실행 `31316044744` 성공
- CD가 JAR 생성 → AWS OIDC 인증 → ECR 이미지 push → EC2 재배포 → 헬스체크까지 완료
- 외부 확인: `https://imhere.ratiko.co.kr/swagger-ui/index.html`, `https://imhere.ratiko.co.kr/docs/openapi3.yaml` 모두 HTTP 200

이 기록은 **배포 파이프라인이 한 번 이상 끝까지 성공했다**는 근거다.
장애 대응, 롤백, 무중단 전환, 부하 처리에 대한 기록은 저장소 어디에도 없다.

관련 파일:

- `BACKEND-DEPLOY-PHASE0.md`
- `BACKEND-DEPLOY-VALUES.md`

---

## 10.1 전체 배포 구조

서버와 웹은 서로 다른 저장소와 서로 다른 workflow로 배포된다. 두 경로가 CloudFront에서 만난다.

```text
[ 서버 경로 : ImHereServer ]

Developer
    │ push (모든 브랜치) / PR → main
    ▼
GitHub : ImHereOfRati/server
    │
    ▼
GitHub Actions  ci.yml (IMHERE_GITHUB_ACTION_CI)
    ├─ 배포 스크립트 문법 검사 (bash -n) + remote-tls-test.sh
    ├─ ./gradlew test
    └─ test-results / JaCoCo artifact 업로드
    │
    │ workflow_run: CI 성공 + branch=main   (또는 workflow_dispatch)
    ▼
GitHub Actions  cd.yml (IMHERE_GITHUB_ACTION_CD)
    │
    ├── build-jar ─────────┐   (gradlew test → bootJar → JAR artifact)
    │                      │
    ├── resolve-infra ─────┤   (OIDC AssumeRole → CloudFormation Outputs 조회)
    │        │             │
    │        └── sync-config    (private config repo → env/*.env + Firebase key)
    │                      │
    │                      ▼
    │                 docker-push   (Dockerfile.release → ECR: <tag> + latest)
    │                      │
    └──────────────────────┴──► deploy-app
                                  │ SSH (임시로 SG 22/80 개방)
                                  ▼
                        AWS EC2 (Amazon Linux 2023, Elastic IP)
                                  │ docker compose -f docker-compose.yml -f docker-compose.prod.yml pull / up -d
                                  ▼
                 ┌────────────────┴──────────────────────────┐
                 │  Docker network  app-network (172.28.0.0/16)│
                 │                                            │
                 │   nginx-container  :80 :443  ──proxy──┐    │
                 │        │                              ▼    │
                 │        │              iamhere-server-container
                 │        │                   Spring Boot :8080
                 │        │                   management     :4861
                 │        │                              │    │
                 │   alloy-container ◄── scrape :4861/prometheus
                 │        ▲          ◄── OTLP HTTP :4318      │
                 │        └── docker.sock (label=external 로그)│
                 └────────────────┬──────────────────────────┘
                                  │
        ┌─────────────────────────┼──────────────────────┐
        ▼                         ▼                      ▼
 Grafana Cloud             외부 MySQL            Discord Webhook
 (Loki / Prometheus / Tempo)  (compose 밖)        (5xx / 4xx 알림)


[ 웹·모바일 경로 : client ]

Developer ─► GitHub : ImHereOfRati/mobile
                 ├─ web-ci.yml / flutter-ci.yml   (검증만)
                 ├─ deploy-web.yml   → S3  /app/releases/<sha>/  (immutable)
                 │                   → S3  /app/, / (landing, no-cache)
                 └─ rollback-web.yml → Firebase Remote Config web_app_url 되돌리기

CloudFront (imhere.ratiko.co.kr)
    ├─ /            , /app/**            → S3 origin
    └─ /api/**                            → ratiko.co.kr (EC2 nginx) → Spring Boot

Flutter 앱 ─► Firebase Remote Config(base_url) ─► 위 API 경로
```

관련 설정:

- `ImHereServer/.github/workflows/ci.yml`
- `ImHereServer/.github/workflows/cd.yml`
- `ImHereServer/docker-compose.yml`
- `ImHereServer/infra/cloudformation/main.yaml`
- `ImHereServer/infra/cloudformation/web-distribution.yml`
- `client/.github/workflows/deploy-web.yml`
- `client/.github/workflows/rollback-web.yml`

---

## 10.2 Docker

### Dockerfile이 두 개인 이유

| 파일 | Multi-stage | 용도 | CD에서 사용 |
| --- | --- | --- | --- |
| `ImHereServer/Dockerfile` | O (builder + runtime) | 컨테이너 안에서 Gradle 빌드까지 수행 | 사용하지 않음 |
| `ImHereServer/Dockerfile.release` | X (단일 stage) | 이미 빌드된 JAR만 담음 | 사용함 |

`Dockerfile`은 소스를 복사해 `./gradlew build -x test`를 컨테이너 안에서 돌린다.

```dockerfile
FROM amazoncorretto:25-alpine-jdk AS builder
...
RUN ./gradlew build -x test

FROM amazoncorretto:25-alpine-jdk
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
```

CD는 이 파일을 쓰지 않는다. `cd.yml`의 `build-jar` job이 GitHub Actions 러너에서 Gradle 캐시를 이용해 JAR을 만들고, `docker-push` job이 그 artifact를 내려받아 `Dockerfile.release`로 이미지를 만든다.

```dockerfile
FROM amazoncorretto:25-alpine-jdk
RUN apk add --no-cache tzdata
ENV TZ=Asia/Seoul
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080 4861
ENTRYPOINT ["java", "-jar", "app.jar"]
```

이 분리의 역할:

- **빌드 캐시가 러너 쪽에 남는다.** `actions/cache`로 `~/.gradle`을 재사용하므로, 이미지 레이어 안에서 매번 의존성을 다시 받는 것을 피한다.
- **이미지에 소스와 Gradle이 들어가지 않는다.** 런타임 이미지에는 JAR 하나만 남는다.
- **`EXPOSE 8080 4861`**: 8080은 애플리케이션, 4861은 Actuator management 포트다(`application.yaml`의 `management.server.port: 4861`). 두 포트를 나눈 덕분에 nginx는 8080만 프록시하고, 메트릭 스크레이프는 4861로 분리된다.
- **`ENV TZ=Asia/Seoul` + tzdata**: 로그 타임스탬프와 배치/스케줄 기준 시각을 KST로 맞춘다.

`ENTRYPOINT`는 `java -jar app.jar` 고정이고, JVM 옵션은 이미지에 박혀 있지 않다.

### .dockerignore

```text
!build/libs/*.jar
...
src/main/resources/imhereFirebaseKey.json
**/*.p8
**/*.pem
**/*.p12
**/*.jks
prod.env
```

`build`를 통째로 제외하면서 `!build/libs/*.jar`만 예외로 되살린다 — `Dockerfile.release`가 그 JAR만 필요로 하기 때문이다.
Firebase 서비스 계정 키와 각종 키 파일을 명시적으로 차단한다. `Dockerfile`(멀티스테이지)이 `COPY src src`를 하므로, 이 차단이 없으면 실제 개인키가 빌더 레이어에 들어간다.

관련 파일: `ImHereServer/.dockerignore`

### Docker Compose

Compose 구성은 공통 정의와 환경별 정의로 분리되어 있다. 공통 정의는 `docker-compose.yml`, 로컬은 `docker-compose.local.yml`, 운영은 `docker-compose.prod.yml`에 둔다.

| profile | 서비스 | 비고 |
| --- | --- | --- |
| `local` | `app`, `prometheus`, `grafana` | 로컬 전용 |
| `prod` | `dsko`(Spring Boot), `nginx`, `alloy` | EC2에서 실행 |

prod 컨테이너 연결 구조:

```text
app-network (bridge, subnet 172.28.0.0/16)

  nginx-container   ports 80:80, 443:443     ← 호스트에 열리는 유일한 컨테이너
        │  proxy_pass http://iamhere-server-container:8080
        ▼
  iamhere-server-container  expose 8080, 4861   ← 호스트 포트 매핑 없음
        ▲
        │  scrape  iamhere-server-container:4861
        │  OTLP    alloy-container:4318  (앱 → alloy 방향)
  alloy-container   /var/run/docker.sock:ro
```

`dsko`는 `ports`가 아니라 `expose`만 쓴다. 즉 애플리케이션 포트와 management 포트는 호스트에 바인딩되지 않고, 외부 접근은 반드시 nginx를 거친다.

메모리 상한이 명시되어 있고 근거가 주석으로 남아 있다.

```yaml
    mem_limit: 1200m   # dsko
    mem_limit: 64m     # nginx
    mem_limit: 200m    # alloy
```

주석 기준: t3.small(2 GiB)에서 호스트 OS/dockerd(~350 MiB)를 뺀 약 1.65 GiB 안에 1.43 GiB가 들어가도록 잡았다. 오버서브스크립션 시 JVM이 자기 cgroup 한도까지 자라는 것이 정당한데도 호스트 커널이 OOM kill 하는 것을 막기 위한 설정이다.

`dsko`는 `pull_policy: always`이고 이미지 참조는 `${ECR_REGISTRY}/${ECR_REPOSITORY}:latest`다. 즉 **compose 파일은 항상 `latest`를 가리킨다**(→ 10.11 롤백).

세 컨테이너 모두 `restart: unless-stopped`다.

env_file은 관심사별로 쪼개서 컨테이너마다 필요한 것만 붙인다.

```yaml
  dsko:
    env_file: [ ./env/server.env ]
  nginx:
    env_file: [ ./env/nginx.env ]
  alloy:
    env_file: [ ./env/alloy.env ]
```

앱 컨테이너에 Grafana Cloud 키가, alloy 컨테이너에 DB 비밀번호가 보이지 않게 하는 구조다.

### 현재 상태 주의점

- `local` profile의 `prometheus` / `grafana` 서비스는 **운영에서 사용하지 않는 잔재다.** 관측 backend는 Grafana Cloud이고 인스턴스에는 `alloy`만 뜬다. 게다가 두 서비스가 마운트하는 `./prometheus.yml`과 `./grafana/provisioning`이 **저장소에 존재하지 않아** 로컬에서도 그대로는 기동되지 않는다.
- `local` profile의 `app`은 `image: iamhere:latest`로, 로컬에서 직접 빌드해 둔 이미지를 전제한다. 저장소에 이 이미지를 만드는 스크립트는 없다.
- 컨테이너에 Docker `HEALTHCHECK`는 정의되어 있지 않다. 헬스 판정은 배포 스크립트(`deploy-imhere.sh`)에서만 한다.

---

## 10.3 GitHub Actions

`ImHereServer/.github/workflows`에는 `ci.yml`, `cd.yml`, `labels.yml` 세 개가 있다. 배포에 관여하는 것은 앞의 둘이다.

### CI — `ci.yml`

실행 조건:

```yaml
on:
  push:
    branches: [ "**" ]     # 모든 브랜치
  pull_request:
    branches: [ main ]
```

단일 job `test` (ubuntu-latest):

1. checkout
2. JDK 25 (corretto) 설정
3. Gradle 캐시 (`~/.gradle/caches`, `~/.gradle/wrapper`)
4. **배포 스크립트 검증** — `bash -n infra/scripts/*.sh infra/scripts/setting-ec2 infra/scripts/deploy-sub-tasks/*.sh infra/scripts/deploy-sub-tasks/tls-sub-tasks/*.sh` 로 문법 검사한다. 배포 스크립트가 CI 대상에 포함되어 있는 점이 특징이다.
5. `./gradlew test` (`TESTCONTAINERS_RYUK_DISABLED=true`)
6. `if: always()`로 test 리포트와 JaCoCo 리포트 artifact 업로드

### CD — `cd.yml`

실행 조건:

```yaml
on:
  workflow_dispatch:
  workflow_run:
    workflows: [ "IMHERE_GITHUB_ACTION_CI" ]
    branches: [ main ]
    types: [ completed ]

concurrency:
  group: imhere-production-deploy
  cancel-in-progress: false
```

`main`에서 CI가 끝났을 때 트리거되고, 각 job이 `github.event.workflow_run.conclusion == 'success'`를 다시 확인한다. `concurrency`로 동시 배포를 막되 `cancel-in-progress: false`이므로 진행 중인 배포를 중간에 끊지 않는다.

Job 의존 그래프 (병렬 구조 포함):

```text
        ┌──────────────┐        ┌────────────────┐
        │  build-jar   │        │ resolve-infra  │      ← 두 job 병렬 시작
        │ test+bootJar │        │  CFN Outputs   │
        └──────┬───────┘        └───┬────────┬───┘
               │                    │        │
               │                    │        ▼
               │                    │   ┌───────────┐
               │                    │   │sync-config│   ← config repo clone
               │                    │   └─────┬─────┘
               ▼                    ▼         │
        ┌──────────────────────────────┐      │
        │        docker-push           │      │
        │ Dockerfile.release → ECR     │      │
        └──────────────┬───────────────┘      │
                       │                      │
                       ▼                      │
        ┌──────────────────────────────────────┴───┐
        │              deploy-app                  │
        │  SG open → provision → 파일 전송 →       │
        │  TLS bootstrap → rollout → TLS issue →   │
        │  healthcheck → prune → 정리              │
        └──────────────────────────────────────────┘
```

#### build-jar

- 이미지 태그를 여기서 만든다.

  ```bash
  DATE=$(TZ='Asia/Seoul' date +'%Y%m%d-%H%M')
  SHORT_SHA=$(echo "$SOURCE_SHA" | cut -c1-7)
  echo "tag=${DATE}-${SHORT_SHA}" >> "$GITHUB_OUTPUT"
  ```

  → `20260809-2131-b969557` 형태. 시각과 commit을 모두 담아 어느 커밋이 언제 나갔는지 태그만으로 식별된다.
- `SOURCE_SHA`는 `github.event.workflow_run.head_sha || github.sha`다. `workflow_run` 트리거는 기본적으로 default 브랜치의 workflow 파일을 쓰기 때문에, checkout ref를 CI가 돌았던 커밋으로 명시적으로 고정한다.
- `./gradlew test` → `./gradlew bootJar`. CI에서 이미 테스트를 돌렸지만 CD에서 한 번 더 돈다.

#### resolve-infra

배포 대상 정보를 저장소에 하드코딩하지 않고 CloudFormation 스택 출력에서 읽는다.

```bash
EC2_HOST=$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" \
  --query "Stacks[0].Outputs[?OutputKey=='ElasticIp'].OutputValue | [0]" --output text)
```

`ElasticIp`, `SecurityGroupId`, `Ec2InstanceId`, `EcrRepositoryName`, `EcrRepositoryUri`를 읽고, 하나라도 비어 있거나 `None`이면 즉시 실패한다. 스택 이름은 `imhere-prod-infra`로 고정되어 있다.

#### docker-push

- OIDC로 AWS 자격증명을 받고 `aws-actions/amazon-ecr-login@v2`로 ECR 로그인
- `docker/build-push-action@v6`, `file: Dockerfile.release`, `timeout-minutes: 15`
- 태그 두 개를 동시에 push

  ```yaml
  tags: |
    ${registry}/${repo}:${{ needs.build-jar.outputs.tag }}
    ${registry}/${repo}:latest
  ```
- 빌드 캐시는 `type=gha` (`cache-from` / `cache-to: mode=max`)

#### sync-config

`infra/scripts/pull-config.sh`가 private config repo(`ImHereOfRati/config`)를 `--depth 1`로 clone해서 `env/*.env`와 `imhereFirebaseKey.json`을 꺼낸다. 파일 이름을 하드코딩하지 않고 `env/*.env`를 전부 가져오며, 하나도 없으면 실패한다. 결과는 `retention-days: 1`짜리 artifact로 다음 job에 넘긴다.

#### deploy-app (SSH 배포)

순서와 각 단계의 이유:

| # | 단계 | 역할 |
| --- | --- | --- |
| 1 | OIDC AssumeRole | 장기 AWS 키 없이 임시 자격증명 |
| 2 | 러너 공인 IP 조회 (`api.ipify.org`) | 다음 단계에서 자기 IP만 허용하려고 |
| 3 | SG에 22 포트 `<runner_ip>/32` 추가 | SSH는 상시 개방하지 않는다 |
| 4 | SG에 80 포트 `0.0.0.0/0` 추가 | Let's Encrypt HTTP-01 챌린지용 |
| 5 | `EC2_SSH_PRIVATE_KEY`로 키 파일 작성, `StrictHostKeyChecking no` | |
| 6 | 원격에서 `EC2_DEPLOY_PATH`의 `~` 확장 후 절대경로 확인 | 상대경로/틸트로 인한 오배치 방지 |
| 7 | `setting-ec2`를 stdin으로 흘려 실행 | 파일이 아직 호스트에 없으므로 `bash -s < script` |
| 8 | 러너에서 nginx/alloy 템플릿 렌더링 후 scp | `SERVER_NAME`, `CERT_DOMAIN`, `NGINX_ALLOWED_ORIGIN`, `MGMT_BASE_PATH` 치환 |
| 9 | 원격의 이전 `env/*.env`, Firebase 키 삭제 후 새로 복사 | config repo에서 사라진 파일이 EC2에 유령으로 남는 것 방지 |
| 10 | `deploy-imhere.sh` 내부 TLS bootstrap | nginx가 참조할 인증서 파일 보장 (없으면 `nginx -t`부터 실패) |
| 11 | `deploy-imhere.sh` 내부 Docker 배포 | 컨테이너 교체 |
| 12 | `deploy-imhere.sh` 내부 TLS issue | HTTP-01 발급/갱신 후 nginx reload (nginx가 떠 있어야 가능) |
| 13 | `deploy-imhere.sh` 내부 healthcheck | `status=UP` 확인 |
| 14 | `docker image prune -f` | 헬스체크 통과 후에만 정리 |
| 15 | `env/*.env` 삭제 (`if: success()`) | 런타임 secret을 디스크에 남기지 않기 위해 |
| 16 | SG 22/80 회수, 러너 임시 파일 삭제 (`if: always()`) | 실패해도 열린 포트가 남지 않게 |

Secret이 프로세스 목록에 남지 않도록 ECR 비밀번호를 stdin으로만 흘린다.

```bash
aws ecr get-login-password --region "$AWS_REGION" \
  | ssh -i ~/.ssh/deploy_key "$EC2_USER@$EC2_HOST" \
    "EC2_DEPLOY_PATH=... ECR_REGISTRY=... ECR_REPOSITORY=... bash .../deploy-imhere.sh"
```

주석에 이유가 명시되어 있다 — `GITHUB_ENV`나 커맨드라인에 실으면 러너 환경파일과 EC2의 `/proc` 양쪽에 평문으로 남는다.

#### 컨테이너 교체 방식 — `deploy-imhere.sh`

```bash
compose config >/dev/null        # 1) compose 정의 해석 가능한지
sudo docker run --rm \
  --add-host iamhere-server-container:127.0.0.1 \
  -v "$PWD/infra/nginx/nginx.conf:/etc/nginx/nginx.conf:ro" \
  -v /etc/letsencrypt:/etc/letsencrypt:ro \
  nginx:alpine nginx -t         # 2) 렌더링된 nginx.conf가 실제로 파싱되는지
compose pull
compose up -d
```

교체 방식은 `docker compose up -d`다. 새 이미지가 있으면 기존 컨테이너를 **중지하고 재생성**한다. 별도의 예비 컨테이너나 트래픽 전환 단계는 없다.

`nginx -t` 사전 검증에서 업스트림 컨테이너 이름을 `--add-host`로 루프백에 매핑하는 이유는, 그 시점에 대상 컨테이너가 없어도 이름 해석만 통과시켜 설정 문법을 검사하기 위해서다.

#### 헬스체크 — `deploy-imhere.sh` 내부 단계

`docker compose up -d`는 컨테이너 생성만 보장하므로, 기동 직후 죽는 경우(env 누락, `ddl-auto=validate` 스키마 불일치 등)를 CD가 성공으로 오보한다. 이 스크립트가 그 구멍을 막는다.

- 앱 이미지에 curl이 없어 **nginx 컨테이너의 busybox wget**으로 management 포트를 찌른다.
- `HEALTH_URL="http://iamhere-server-container:4861${MGMT_BASE_PATH}/health"`
- `MGMT_BASE_PATH`는 EC2의 `env/server.env`에서 읽는다.
- 기본 10회 × 30초 간격, 응답 본문에 `"status":"UP"`이 있어야 통과.
- 실패 시 `docker logs --tail 200 iamhere-server-container`를 출력하고 exit 1 → 배포 실패.

#### Secret 사용 방식

`cd.yml`이 참조하는 GitHub Secrets: `AWS_REGION`, `AWS_DEPLOY_ROLE_ARN`, `CONFIG_REPO_PAT`, `EC2_SSH_PRIVATE_KEY`, `EC2_USER`, `EC2_DEPLOY_PATH`.
AWS access key는 저장하지 않고 OIDC(`permissions: id-token: write`)로 role을 AssumeRole한다.

#### client 저장소의 workflow

| workflow | 트리거 | 하는 일 |
| --- | --- | --- |
| `web-ci.yml` | `web/**` 등 경로 push/PR | lint, format:check, bridge:check, typecheck 등 검증만 |
| `flutter-ci.yml` | `lib/**`, `android/**`, `ios/**` 등 push/PR | Flutter 검증. **스토어 업로드는 하지 않는다** |
| `deploy-web.yml` | `main` push (경로 필터) / 수동 | 빌드 → S3 업로드 → CloudFront behavior 검증 → smoke test |
| `rollback-web.yml` | 수동 (`release_sha` 입력) | S3에 해당 release 존재 확인 → smoke → Remote Config `web_app_url` 되돌리기 |

`deploy-web.yml`은 `configuration` job에서 필요한 변수/시크릿이 하나라도 비면 배포를 건너뛰고 notice만 남긴다.

---

## 10.4 AWS EC2 / ECR

인프라는 `ImHereServer/infra/cloudformation/main.yaml` 한 스택(`imhere-prod-infra`)으로 정의되어 있다.

### ECR

```yaml
  AppEcrRepository:
    Type: AWS::ECR::Repository
    Properties:
      RepositoryName: !Ref EcrRepositoryName      # 기본값 imhere/dsko
      ImageScanningConfiguration:
        ScanOnPush: true
      EncryptionConfiguration:
        EncryptionType: AES256
      LifecyclePolicy: ... "Keep latest 30 images" (imageCountMoreThan 30 → expire)
```

- **Tag 정책**: `docker-push` job이 이미지 하나에 태그 두 개를 붙여 push한다.
  - `<YYYYMMDD-HHMM(KST)>-<short sha>` — 불변 식별 태그
  - `latest` — 현재 배포본 포인터
  - Git full SHA만 쓰는 방식은 아니고, 시각 + 7자리 SHA 조합이다.
- **보존**: lifecycle policy가 최신 30개만 남긴다 → 직전 이미지들은 ECR에 남아 있고 다시 pull 할 수 있다.
- **GitHub Actions 연결**: `aws-actions/configure-aws-credentials@v4`(OIDC) → `amazon-ecr-login@v2`. IAM role의 ECR 권한은 `GetAuthorizationToken`(`*`)과 push/pull 관련 액션(해당 리포지토리 ARN으로 한정)으로 제한되어 있다.
- OIDC trust는 `repo:${RepositorySlug}:ref:${MainBranchRef}` (기본값 `ImHereOfRati/server` / `refs/heads/main`)로 제한된다.

### EC2

CloudFormation이 정의하는 것:

- VPC(`10.50.0.0/16`) + public subnet(`10.50.1.0/24`) + IGW + 기본 라우트
- EC2 1대 — AMI는 SSM 파라미터 `al2023-ami-kernel-default-x86_64`, InstanceType 파라미터 기본값 `t3.small`(허용값 t3.micro/small/medium)
- Elastic IP + EIPAssociation → 배포 workflow는 이 EIP를 host로 쓴다
- Security Group: **인바운드는 80, 443만 상시 개방.** 22는 배포 중 러너 IP에만 임시로 열린다.
- UserData: `docker`, `certbot` 설치, docker compose 플러그인 설치, `/opt/imhere`와 `/home/ec2-user/imhere` 생성 후 `ec2-user` 소유로 변경

배포 시 애플리케이션 반영 방식:

1. `setting-ec2` — docker/certbot/compose 플러그인이 없을 때만 설치, `infra/nginx/certbot`, `infra/alloy`, `infra/scripts`, `env`, `secrets` 디렉터리 생성 후 소유권 정리
2. `docker-compose.yml`, 렌더링된 `nginx.conf` / `alloy-config.alloy`, `env/*.env`, Firebase 키, remote 스크립트를 scp
3. `sudo docker login`(stdin) → `compose pull` → `compose up -d`
4. Port mapping은 nginx만 `80:80`, `443:443`. 앱과 alloy는 호스트 포트를 갖지 않는다.
5. Network는 compose가 만드는 `app-network`(subnet `172.28.0.0/16`) 하나

DB는 compose에 포함되어 있지 않다. `application.yaml`의 datasource가 `DB_HOST` 등 외부 값을 받고, `ddl-auto: validate`이므로 스키마가 맞지 않으면 기동 단계에서 실패한다.

저장소만으로 확인할 수 없는 것:

- 실제 운영에 떠 있는 EC2 인스턴스 타입(템플릿 기본값은 `t3.small`이지만 실제 배포 시 어떤 값으로 스택을 만들었는지는 저장소에 없다)
- 실제 EBS 볼륨 크기(템플릿에 BlockDeviceMappings 없음 → AMI 기본값)
- 외부 MySQL의 위치와 사양(RDS인지 다른 호스팅인지 저장소에서 확인 불가)
- CloudFront 배포 ID, ACM 인증서 ARN 등 파라미터 실제 값

`BACKEND-DEPLOY-VALUES.md`에 리전 `ap-northeast-2`, 스택 `imhere-prod-infra`, role 이름 `imhere-github-actions-deploy` 등이 기록되어 있다.

---

## 10.5 Nginx

nginx 설정은 저장소에 **템플릿**으로 존재하고, CD의 `deploy-app`이 러너에서 파이썬으로 치환해 EC2로 보낸다.

치환 대상: `SERVER_NAME`, `CERT_DOMAIN`, `NGINX_ALLOWED_ORIGIN`, `MGMT_BASE_PATH` (하나라도 비면 렌더링 단계에서 실패)

### 구조

| listen | server_name | 처리 |
| --- | --- | --- |
| 80 | `${SERVER_NAME}`, `www.${SERVER_NAME}` | `/.well-known/acme-challenge/`는 `/var/www/certbot`에서 서빙, 나머지는 `301 https://${SERVER_NAME}` |
| 443 | `www.${SERVER_NAME}` | 인증서 검증만 통과 후 apex로 `301` |
| 443 | `${SERVER_NAME}` | 실제 트래픽 |

apex 443 서버 블록의 location 매핑:

```nginx
location = /            { return 301 https://imhere.ratiko.co.kr$request_uri; }
location ^~ /api/       { ... proxy_pass http://iamhere-server-container:8080; }
location = /admin       { proxy_pass http://iamhere-server-container:8080; }
location ^~ /admin/     { proxy_pass ...; }
location ^~ /swagger-ui/{ proxy_pass ...; }
location ^~ /docs/      { proxy_pass ...; }
location ^~ ${MGMT_BASE_PATH}/ { proxy_pass ...; }
location /              { return 301 https://imhere.ratiko.co.kr$request_uri; }
```

특징:

- **`upstream` 블록이 없다.** `proxy_pass`가 컨테이너 이름 `iamhere-server-container:8080`을 직접 가리킨다. 서비스명(`dsko`) 대신 `container_name`을 쓰는 이유가 주석에 있다 — compose 서비스명 alias가 네트워크 재생성 후 등록되지 않는 경우가 있어, 항상 보장되는 컨테이너 이름으로 고정했다.
- **SSL termination은 nginx에서 한다.** 인증서는 호스트의 `/etc/letsencrypt`를 read-only로 마운트한다. `ssl_protocols TLSv1.2 TLSv1.3`.
- **HTTP → HTTPS redirect**: 80은 ACME 경로를 제외하고 전부 301.
- **CORS는 nginx가 최종 정리한다.** `/api/`에서 `Access-Control-Allow-Origin`을 `${NGINX_ALLOWED_ORIGIN}` 하나로 고정하고, `proxy_hide_header 'Access-Control-Allow-Origin'`으로 Spring이 내려보낸 중복 헤더를 지운다. `OPTIONS` 프리플라이트는 백엔드까지 가지 않고 nginx에서 `204`로 끝난다.
- **`${MGMT_BASE_PATH}/`** location이 있어 Actuator 경로도 프록시된다. 즉 management endpoint는 난독화된 경로로 외부에서 접근 가능한 상태다.
- `X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto`를 전달하고, Spring 쪽은 `server.forward-headers-strategy: native`로 이를 신뢰한다.

### 무중단 관련

- **upstream 정의, 포트 스위칭, Blue/Green 구조는 존재하지 않는다.** 백엔드 대상이 컨테이너 이름 하나로 고정되어 있고, 배포는 그 컨테이너를 재생성한다.
- 따라서 새 컨테이너가 기동을 마칠 때까지 nginx의 프록시 대상이 없는 구간이 생길 수 있는 구조다. 이 구간의 실제 길이나 요청 유실 여부를 저장소에서 확인할 수 있는 근거는 없다.
- nginx의 health check(`upstream ... max_fails`, `proxy_next_upstream` 등) 설정은 사용하지 않는다. 헬스 판정은 배포 스크립트가 배포 후 별도로 수행한다.

### TLS 운영 — `infra/scripts/deploy-sub-tasks/tls-sub-tasks/do-tls.sh`

인증서 처리를 컨테이너 기동 앞뒤로 두 번 나눈 점이 이 구성의 핵심이다.

- `bootstrap` (컨테이너 기동 **전**): `options-ssl-nginx.conf`와 `ssl-dhparams.pem`을 보장하고, 인증서도 renewal conf도 없으면 self-signed를 만든다. nginx.conf가 인증서 파일을 참조하므로 파일이 없으면 `nginx -t`부터 실패하기 때문이다.
- `issue` (컨테이너 기동 **후**): HTTP-01 webroot 검증이라 nginx가 `/.well-known/acme-challenge/`를 이미 서빙하고 있어야 한다. 발급/갱신 후 `nginx -s reload`.

인증서 lineage가 손상된 경우(`renewal conf`는 있는데 certbot이 유효하다고 보지 않는 상태 등) 자동으로 지우지 않고 **배포를 실패시킨다.** 스크립트 주석에 "운영 TLS를 자동으로 파괴하는 경로였기 때문"이라고 기록되어 있다.
`ssl-dhparams.pem`은 즉석 생성하지 않고 RFC 7919 ffdhe2048 값을 인라인해 둔다 — 배포 임계 경로에서 수십 초~수 분이 튀는 것을 피하기 위해서다.

이 스크립트는 `ci.yml`에서 TLS 관련 셸 문법 검사를 수행한다.

관련 설정:

- `ImHereServer/infra/nginx/nginx.conf.template`
- `ImHereServer/infra/scripts/deploy-sub-tasks/tls-sub-tasks/do-tls.sh`
- `ImHereServer/infra/scripts/deploy-sub-tasks/tls-sub-tasks/*.sh`

---

## 10.6 환경 변수 / Secret 관리

### 주입 경로

배포에 필요한 런타임 설정 값은 애플리케이션 저장소가 아니라 **private config repo(`ImHereOfRati/config`)의 `env/*.env`가 단일 원본이고, 현재 이 방식으로 운영이 동작 중이다**(사용자 확인). 작업 루트의 `remoteConfig/`가 그 저장소의 로컬 클론이다.

```text
[1] private config repo (ImHereOfRati/config)   ← 운영 설정의 단일 원본 (동작 중)
        env/server.env, nginx.env, alloy.env
        imhereFirebaseKey.json
              │  pull-config.sh (CONFIG_REPO_PAT로 depth-1 clone)
              ▼
[2] GitHub Actions artifact  "runtime-files" (retention 1일)
              │
              ├─► 러너에서 nginx.conf / alloy-config.alloy 템플릿 치환에 사용
              │
              ▼  scp
[3] EC2  ${EC2_DEPLOY_PATH}/env/*.env , secrets/imhereFirebaseKey.json
              │  docker compose env_file (컨테이너별로 필요한 파일만)
              ▼
[4] Docker container 환경변수
              │
              ▼
[5] Spring Boot  ${VAR} placeholder  (application.yaml)

배포 성공 시 [3]의 env/*.env는 삭제된다 (Firebase 키는 남는다).
```

### 파일별 범위

| 파일 | 담는 값의 범위 | 주입 대상 컨테이너 |
| --- | --- | --- |
| `env/server.env` | Spring 프로파일, DB, JWT, 관리 경로, 관리자·보안, OIDC, 외부 연동 | dsko |
| `env/nginx.env` | `SERVER_NAME`, `CERT_DOMAIN`, `NGINX_ALLOWED_ORIGIN`, `CORS_ALLOWED_ORIGINS` | nginx, TLS |
| `env/alloy.env` | Grafana Cloud Loki/Prometheus/Tempo 엔드포인트·자격증명 | alloy |

(키 이름은 `remoteConfig/env/*.env`와 `pull-config.sh`에서 확인했다. 값은 이 문서에 옮기지 않는다.)

### Spring Profile

`ImHereServer/src/main/resources/application.yaml` 한 파일에 문서 두 개가 들어 있다(`application-*.yml` 별도 파일은 없다).

- 기본 문서: 모든 `${VAR}`에 로컬 기본값이 있어 프로파일 없이 `bootRun`만 해도 뜬다.
- `on-profile: prod` 문서: 같은 키를 **기본값 없이** 다시 선언한다.

```yaml
jwt:
  secret: ${JWT_SECRET}
management:
  endpoints:
    web:
      base-path: ${MGMT_BASE_PATH}
```

주석에 의도가 명시되어 있다 — 이렇게 덮어쓰지 않으면 env가 하나 빠졌을 때 운영이 로컬 더미로 조용히 뜬다(JWT 서명키가 리포에 공개된 값이 되거나 actuator 경로가 `/actuator`로 노출되는 식). 지금은 값이 비면 기동 단계에서 실패한다.
`DB_*`는 일부러 제외했다 — 값이 틀리면 Hikari 연결과 `ddl-auto=validate`가 기동 중에 터지므로 조용히 잘못될 수 없다는 판단이다.

### GitHub Secrets / Variables

| 저장소 | 종류 | 이름 |
| --- | --- | --- |
| server | Secrets | `AWS_REGION`, `AWS_DEPLOY_ROLE_ARN`, `CONFIG_REPO_PAT`, `EC2_SSH_PRIVATE_KEY`, `EC2_USER`, `EC2_DEPLOY_PATH` |
| client | Secrets | `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` |
| client | Variables | `AWS_REGION`, `AWS_DEPLOY_ROLE_ARN`, `WEB_APP_BUCKET`, `WEB_CLOUDFRONT_DISTRIBUTION_ID`, `WEB_PUBLIC_ORIGIN`, `BACKEND_PUBLIC_ORIGIN`, 캐시 정책 ID 3종, `GCP_*`, `FIREBASE_PROJECT_ID`, `VITE_*` |

### 보안상 개선이 필요한 항목

실제 값은 아래에 옮기지 않는다. 구조상의 문제만 정리한다.

1. **`EC2_SSH_PRIVATE_KEY`가 장기 secret이다.** SSM Session Manager 같은 대안 없이 SSH 개인키를 GitHub Secrets에 보관하고, 배포마다 러너 디스크에 기록한다(사용 후 삭제하긴 한다).
2. **`CONFIG_REPO_PAT`가 장기 PAT이다.** clone URL에 토큰을 끼워 넣는 방식(`https://x-access-token:${CONFIG_REPO_PAT}@...`)이라 URL이 로그에 찍히면 노출된다.
3. **Firebase 서비스 계정 키가 EC2 디스크에 평문으로 남는다.** 배포 후 삭제되는 것은 `env/*.env`뿐이고 `secrets/imhereFirebaseKey.json`은 그대로 유지된다(컨테이너가 read-only로 마운트해 계속 읽어야 하기 때문).
4. **client의 `deploy-web.yml`은 장기 AWS access key를 쓴다.** `secrets.AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY`를 `configure-aws-credentials`에 직접 넘긴다. 같은 저장소의 `rollback-web.yml`은 `role-to-assume`(OIDC)를 쓰고, `client/docs/deployment/web-release.md`도 "Do not create or store an AWS access key for Actions"라고 적고 있다 — **문서·롤백 workflow와 배포 workflow가 불일치한다.**
5. **로컬 작업 디렉터리에 운영 비밀이 평문으로 존재한다.** `C:\Project\ImHere\remoteConfig`(config repo 클론, 운영 env 전체 + Firebase 키), `C:\Project\ImHere\imhere-prod-key.pem`(EC2 SSH 개인키), `ImHereServer/src/main/resources/imhereFirebaseKey.json`. 세 개 모두 git에는 올라가지 않는다(`.gitignore` 89행이 Firebase 키를, 95행이 `*.pem`을 차단하고 `.dockerignore`도 같은 파일들을 막는다). 저장소 유출 위험은 아니고 **로컬 단말 관리 문제**다.
6. **소비처가 없는 키가 있다.** `env/server.env`의 `ADMIN_ALLOWED_IPS`, `env/alloy.env`의 `GRAFANA_CLOUD_ALERTMANAGER_ENDPOINT` / `_USER`는 `src/`와 `infra/`에서 참조하는 코드를 찾을 수 없다(grep 0건).

---

## 10.7 Actuator

### Dependency

```gradle
implementation 'org.springframework.boot:spring-boot-starter-actuator'
runtimeOnly 'io.micrometer:micrometer-registry-prometheus'
implementation 'org.springframework.boot:spring-boot-starter-opentelemetry'
runtimeOnly 'org.springframework.modulith:spring-modulith-actuator'
runtimeOnly 'org.springframework.modulith:spring-modulith-observability'
```

### 설정

```yaml
management:
  server:
    port: 4861
  endpoints:
    web:
      base-path: ${MGMT_BASE_PATH:/actuator}
      exposure:
        include: "prometheus,health,info"
  endpoint:
    health:
      show-details: always
    prometheus:
      enabled: true
```

### 노출 endpoint

프로파일과 무관하게 노출되는 것은 세 개다.

```text
${MGMT_BASE_PATH}/health
${MGMT_BASE_PATH}/prometheus
${MGMT_BASE_PATH}/info
```

- **포트 분리**: management는 4861, 애플리케이션 API는 8080이다. 컨테이너는 두 포트를 `expose`만 하고 호스트에 바인딩하지 않는다.
- **base-path 난독화**: 운영에서는 `MGMT_BASE_PATH`로 `/actuator`가 아닌 경로가 주입되고, prod 프로파일이 기본값 없이 재선언하므로 값이 없으면 기동에 실패한다. 이 값은 세 곳이 같아야 한다 — 앱(`application.yaml`), nginx location(`nginx.conf.template`), alloy scrape 경로(`alloy-config.alloy.template`).
- **접근 경로**: ① nginx `location ^~ ${MGMT_BASE_PATH}/`를 통한 외부 HTTPS 경로, ② compose 네트워크 내부에서 `iamhere-server-container:4861` 직접 접근(alloy 스크레이프, 헬스체크 스크립트).
- **보안 설정**: Spring Security 차원의 별도 management 인증은 설정되어 있지 않다. 보호는 base-path 난독화 + Security Group + nginx location 범위에 의존한다. `show-details: always`이므로 health 응답에 하위 indicator 상세가 포함된다.

관련 설정:

- `ImHereServer/src/main/resources/application.yaml`
- `ImHereServer/build.gradle`
- `ImHereServer/infra/nginx/nginx.conf.template`
- `ImHereServer/infra/scripts/deploy-sub-tasks/remote-healthcheck.sh`

---

## 10.8 Monitoring / Observability

운영 환경의 관측 backend는 **전부 Grafana Cloud(SaaS)**다. Prometheus, Grafana, Loki, Tempo 중 EC2 인스턴스에서 돌아가는 것은 **하나도 없고**, 인스턴스에는 수집·전달만 담당하는 **Grafana Alloy 컨테이너 하나**가 뜬다.

즉 이 프로젝트에서 Prometheus / Loki / Tempo는 "자체 운영하는 서버"가 아니라 **Alloy가 데이터를 밀어 넣는 Grafana Cloud의 수신 endpoint**를 가리킨다. Grafana도 조회 UI로 Grafana Cloud를 쓴다.

```text
                       EC2 (compose profile: prod)
Spring Boot
   │
   ├─ Metrics  ── ${MGMT_BASE_PATH}/prometheus :4861 ──scrape 30s──┐
   │                                                              │
   ├─ Traces   ── OTLP HTTP → alloy-container:4318 ───────────────┤
   │                                                              ▼
   └─ Logs     ── stdout/stderr → docker.sock ───────────────► Alloy
                  (label imhere_log_scope=external 만)            │
                                                                  │
                     ┌────────────────────────────────────────────┤
                     ▼                    ▼                       ▼
        Grafana Cloud Prometheus   Grafana Cloud Loki    Grafana Cloud Tempo
                     └────────────────────┴───────────────────────┘
                                          ▼
                                    Grafana Cloud UI

별도 경로: 5xx / 4xx → Discord Webhook (애플리케이션 코드에서 직접 전송)
```

### Prometheus

- **[구성 여부]** Grafana Cloud Prometheus를 사용한다. **인스턴스에서 Prometheus 서버를 운영하지 않는다.** `docker-compose.local.yml`의 `prom/prometheus` 서비스는 로컬 전용이며 운영 Compose에는 포함하지 않는다.
- **[수집 대상]** Actuator `${MGMT_BASE_PATH}/prometheus` (Micrometer 기본 메트릭 + Spring Modulith observability)
- **[연결 방식]** Alloy의 `prometheus.scrape`가 `iamhere-server-container:4861`을 30초 간격으로 스크레이프하고 `prometheus.remote_write`로 Grafana Cloud에 보낸다.

  ```river
  prometheus.scrape "imhere" {
    targets = [{ __address__ = "iamhere-server-container:4861" }]
    scrape_interval = "30s"
    metrics_path = "${MGMT_BASE_PATH}/prometheus"
    forward_to = [prometheus.remote_write.grafana_cloud.receiver]
  }
  ```
- **[현재 검증 수준]** 수집 경로를 구성했다. 실제로 메트릭이 Grafana Cloud에 적재되었는지, 어떤 대시보드/쿼리를 사용하는지 확인할 수 있는 근거는 저장소에 없다. **미검증.**

### Grafana

- **[구성 여부]** 조회 UI는 Grafana Cloud(SaaS)를 사용한다. **인스턴스에서 Grafana를 운영하지 않는다.** compose의 `grafana/grafana` 서비스는 `local` profile 전용이며, 마운트하는 `./grafana/provisioning` 디렉터리도 저장소에 없다.
- **[수집 대상]** Grafana Cloud의 Prometheus / Loki / Tempo 데이터소스
- **[연결 방식]** 자격증명은 `env/alloy.env`로 Alloy에만 주입된다. 대시보드 JSON, provisioning 파일, alert rule은 저장소에 없다(Grafana Cloud 콘솔 쪽에 있다면 저장소에서는 확인할 수 없다).
- **[현재 검증 수준]** 저장소 기준으로는 대시보드 정의 자체가 없다. **미검증.**

### Loki

- **[구성 여부]** 구성되어 있다. Alloy의 `loki.source.docker` + `loki.write`.
- **[수집 대상]** Docker 컨테이너의 stdout/stderr 중, 라벨 `imhere_log_scope=external`이 붙은 컨테이너만. compose에서 이 라벨이 `external`인 것은 `dsko`(앱)뿐이고, `nginx`와 `alloy`는 `internal`이라 외부 전송에서 제외된다.
- **[연결 방식]** `discovery.docker` → `discovery.relabel`(container / stream / service 라벨 부여, `job="imhere-server"`) → `loki.write`(Grafana Cloud, basic auth)
- **주의**: 수집 대상은 **Docker stdout/stderr뿐이다.** `logging.file.name`으로 쓰는 `logs/imhere.log`와 `ErrorLogAppenderConfig`가 만드는 `logs/imhere-error.log`는 컨테이너 내부 파일이고, Alloy가 이 파일을 tail 하는 설정은 없다. (`BACKEND-DEPLOY-VALUES.md`의 "`LOG_FILE` … alloy가 이걸 tail 해서 Loki로 보냄"이라는 서술은 현재 `alloy-config.alloy.template`과 일치하지 않는다.)
- **[현재 검증 수준]** 전송 경로를 구성했다. 실제 장애 로그를 Loki에서 추적한 근거는 없다. **미검증.**

### Tempo

- **[구성 여부]** 구성되어 있다.
- **[수집 대상]** Spring Boot의 OpenTelemetry 트레이스. `management.tracing.sampling.probability: 0.1`(10% 샘플링), `baggage.enabled: true`.
- **[연결 방식]** prod 프로파일에서 `management.opentelemetry.tracing.export.otlp.endpoint: http://alloy-container:4318/v1/traces`. Alloy의 `otelcol.receiver.otlp`(gRPC 4317 / HTTP 4318)가 받아 `otelcol.exporter.otlphttp`로 Grafana Cloud Tempo에 보낸다. gRPC exporter가 아니라 HTTP exporter를 쓰는 이유가 주석에 있다 — Grafana Cloud Tempo 엔드포인트가 OTLP HTTP 게이트웨이 URL이라 gRPC exporter로는 호스트 해석이 안 된다(`resolver: "no children to pick from"`).
- **[현재 검증 수준]** 전송 경로를 구성했다. 실제 요청 하나의 trace를 Tempo에서 조회한 근거는 없다. **미검증.**

### Alert

- Prometheus alert rule, Alertmanager 설정, Grafana alert provisioning 파일 모두 저장소에 없다. `env/alloy.env`에 `GRAFANA_CLOUD_ALERTMANAGER_*` 키가 있으나 이를 참조하는 설정이 없다.
- 실제로 동작하는 알림은 애플리케이션 코드의 Discord webhook 두 개뿐이다.

  | 채널 | 발화 조건 | 코드 |
  | --- | --- | --- |
  | `SERVER_ERROR` | 응답 status ≥ 500 | `AccessLogPrinter.sendAlertIfNeeded` |
  | `CLIENT_ERROR` | 클라이언트 오류 경로 | `UserErrorAlertNotifier` |

  전송은 `DiscordErrorAlertAdapter`가 `@Async("discordExecutor")`로 fire-and-forget 하고, 실패하면 재시도 없이 `logger.error`만 남긴다. webhook URL이 비면 경고 로그를 남기고 건너뛴다.
- 참고: 과거 관측성 문서에는 webhook 세 종류(`ott` 포함)와 현재 코드에 없는 클래스가 서술되어 있었지만, 해당 내용은 현재 구현과 어긋나므로 이 문서에서는 현재 코드 기준의 두 채널만 기술한다.

관련 설정:

- `ImHereServer/infra/alloy/alloy-config.alloy.template`
- `ImHereServer/docker-compose.yml`
- `ImHereServer/src/main/resources/application.yaml`
- `ImHereServer/src/main/kotlin/com/kdongsu5509/support/external/DiscordErrorAlertAdapter.kt`

---

## 10.9 로그 구조

### 출력 형식

| 대상 | 형식 | 근거 |
| --- | --- | --- |
| Console (prod) | ECS 구조화 로그 | `logging.structured.format.console: ecs` (prod 문서) |
| Console (local) | Spring Boot 기본 포맷 | 기본 문서에 console 패턴 미지정 |
| File `logs/imhere.log` | `%d ... [${app},%X{traceId},%X{spanId}] [%thread] %logger - %message` | `logging.pattern.file` |
| File `logs/imhere-error.log` | `%date %level [traceId=%X{traceId}] [%X{method} %X{uri} -> %X{status} %X{durationMs}ms] %logger - %message` | `ErrorLogAppenderConfig` |

운영에서 Loki로 나가는 것은 **console(stdout) 뿐이며 ECS JSON 형식**이다. 파일 로그는 컨테이너 안에만 남는다.

### Log Level

```yaml
logging.level:
  root: INFO
  com.kdongsu5509: INFO
  org.springframework: WARN
  org.hibernate: WARN
  com.zaxxer.hikari: WARN
  com.google.firebase: WARN
  p6spy: WARN
  org.springframework.security: TRACE   # 기본 문서
```

prod 문서에서 `org.springframework.security`를 `INFO`로 낮춘다. 즉 `TRACE`는 로컬 전용이다.

### 파일 롤링

| 파일 | 조건 | 보존 |
| --- | --- | --- |
| `logs/imhere.log` | 일 단위 + 100MB | 30일, 총 3GB 상한 |
| `logs/imhere-error.log` | 일 단위 + 50MB, WARN 이상만 | 90일, 총 500MB 상한 |

에러 파일 appender는 XML(`logback-spring.xml`)이 아니라 **코드로 등록한다.** `ErrorLogAppenderConfig`가 `@PostConstruct`에서 `RollingFileAppender`를 만들고 `ThresholdFilter(WARN)`을 걸어 `AsyncAppender`(queueSize 256, `neverBlock = true`)로 감싼 뒤 root logger에 붙인다. `@Profile("!test")`라 테스트에서는 등록되지 않는다. `neverBlock = true`이므로 큐가 차면 로그를 버리고 애플리케이션 스레드를 막지 않는다.

### MDC와 요청 컨텍스트

`LoggingFilter`(OncePerRequestFilter)가 요청 단위 컨텍스트를 만든다.

```text
HTTP Request
    ↓
nginx  (X-Real-IP / X-Forwarded-For / X-Forwarded-Proto 부여)
    ↓
Spring Security filter chain  (JwtAuthenticationFilter 등)
    ↓
LoggingFilter
    ├─ isIgnoredUrl() : /actuator, /health, /favicon.ico, /swagger-ui, /v3/api-docs 는 통과만 시킴
    ├─ ContentCachingRequestWrapper / ContentCachingResponseWrapper 로 body 캡처
    └─ MDC.put
         ├─ traceId    ← UUID.randomUUID()
         ├─ method
         └─ uri
    ↓
Controller → Service → Repository
    ↓
LoggingFilter (finally)
    ├─ MDC.put status, durationMs
    ├─ AccessLog 생성 (queryString, requestBody, responseBody, headers, userAgent, remoteIp, threadName, requestAt/responseAt/durationMs)
    ├─ AccessLogPrinter.print → log.info(formatted)
    │       └─ status >= 500 이면 Discord SERVER_ERROR 알림 (traceId 포함)
    ├─ copyBodyToResponse()
    └─ MDC.clear()
```

MDC 키는 실제로 다음 다섯 개다: `traceId`, `method`, `uri`, `status`, `durationMs`.

주의할 점:

- `traceId`는 **`LoggingFilter`가 직접 만든 UUID**다. `logging.pattern.file`은 `%X{traceId}`와 `%X{spanId}`를 함께 찍는데, `spanId`를 MDC에 넣는 코드는 없다(OpenTelemetry/Micrometer가 채우는 값에 의존). 즉 파일 로그의 `traceId`는 W3C trace context의 trace ID가 아니라 필터가 만든 UUID일 수 있다. **로그의 `traceId`와 Tempo의 trace ID가 같은 값이라고 단정할 수 없다.**
- `requestId`라는 이름의 별도 MDC 키는 없다.
- Actuator, Swagger, health 경로는 access log 대상에서 제외된다.

### 민감 정보 마스킹

`BodyMasker`가 JSON body에서 다음 필드 값을 `*****`로 치환한다.

```kotlin
Regex(""""(password|pw|confirmPassword|secret)"\s*:\s*"[^"]+"""")
```

헤더는 `AccessLog.extractHeaders`가 걸러낸다. 토큰/키 이름이 위 네 개에 해당하지 않으면 마스킹되지 않는 구조이므로, 마스킹 대상 목록은 주기적인 점검이 필요하다.

### Error Logging

- 5xx는 `AccessLogPrinter`가 응답 status를 보고 잡는다. `@RestControllerAdvice`가 놓친 예외나 필터/서블릿 단계 오류도 결국 status 500으로 응답에 찍히므로, 이 지점이 가장 빠짐없이 포착한다.
- WARN 이상은 `imhere-error.log`에 별도 적재된다(운영 컨테이너 내부 파일).

관련 파일:

- `ImHereServer/src/main/kotlin/com/kdongsu5509/support/logger/LoggingFilter.kt`
- `ImHereServer/src/main/kotlin/com/kdongsu5509/support/logger/AccessLogPrinter.kt`
- `ImHereServer/src/main/kotlin/com/kdongsu5509/support/logger/BodyMasker.kt`
- `ImHereServer/src/main/kotlin/com/kdongsu5509/support/config/ErrorLogAppenderConfig.kt`

---

## 10.10 장애 대응 방식

### 현재 가능한 대응

저장소의 코드·설정상 실제로 할 수 있는 것들이다.

| 수단 | 구현 위치 | 동작 |
| --- | --- | --- |
| 컨테이너 자동 재시작 | `docker-compose.yml` `restart: unless-stopped` | 컨테이너 **프로세스가 종료되면** Docker가 다시 띄운다. 애플리케이션이 살아 있으면서 응답만 못 하는 상태는 감지하지 못한다(컨테이너 `HEALTHCHECK` 미정의). |
| 배포 시 헬스 판정 | `infra/scripts/deploy-sub-tasks/remote-healthcheck.sh` | management `/health`를 최대 10회 × 30초 폴링. 실패하면 최근 앱 로그 200줄을 출력하고 배포를 실패 처리한다. |
| 수동 헬스 확인 | 같은 스크립트를 EC2에서 재실행 | 배포와 무관하게 언제든 실행 가능 |
| 컨테이너 로그 조회 | `docker logs --tail N iamhere-server-container` | 헬스체크 실패 경로에 이미 포함되어 있다 |
| 원격 로그 조회 | Grafana Cloud Loki, `job="imhere-server"` | Alloy가 앱 컨테이너 stdout을 전송 |
| Metric 조회 | Grafana Cloud Prometheus | Alloy가 30초 간격 스크레이프 |
| Trace 조회 | Grafana Cloud Tempo | 10% 샘플링 |
| 즉시 알림 | Discord webhook | 5xx 발생 시 traceId·method·URI·소요시간·IP 포함 메시지 |
| 재배포 | `cd.yml` `workflow_dispatch` | 같은/다른 커밋으로 전체 배포 재실행 |
| 설정 오류 사전 차단 | `deploy-sub-tasks/deploy-imhere-with-docker.sh`의 `compose config` + `nginx -t` | 잘못된 env 보간이나 nginx 설정으로 트래픽을 끊는 것을 배포 전에 막는다 |
| TLS 사고 차단 | `deploy-sub-tasks/tls-sub-tasks/do-tls.sh` | lineage가 수상하면 자동 삭제하지 않고 배포를 실패시킨다 |
| 네트워크 노출 축소 | CloudFormation SG + CD의 SG open/close | 평시 22 포트 차단, 배포 중에만 러너 IP 한정 개방 |

장애 범위 좁히기 순서는 공개 endpoint → nginx → 앱 컨테이너 → management health 순으로 확인한다.

### 구조적 한계 (저장소에서 확인 가능한 것)

- 애플리케이션 인스턴스가 1대다. 컨테이너가 죽으면 그 사이 요청을 받을 대상이 없다.
- 컨테이너 `HEALTHCHECK`가 없어 Docker가 "떠 있지만 응답하지 않는" 상태를 스스로 감지하지 못한다.
- Prometheus alert rule / Alertmanager 설정이 없다. 지표 기반 자동 알림은 없고, 알림은 5xx 응답이 실제로 발생해야 나가는 Discord webhook뿐이다.
- Discord 전송은 재시도가 없다(실패 시 로그만).

### 실제 검증 여부

- `BACKEND-DEPLOY-PHASE0.md`에 CD 실행 중 헬스체크까지 성공한 기록이 있다. 즉 **정상 배포 경로에서 헬스체크가 동작한 근거는 있다.**
- 그 외 — 실제 운영 장애를 겪고 위 수단으로 대응한 기록, 장애 주입/game day 결과, Discord 알림이 실제 5xx로 발화한 기록, Grafana Cloud에서 장애를 탐지한 기록은 **저장소 어디에서도 확인할 수 없다.**

> 현재 장애 대응을 위한 로그 및 모니터링 구성은 존재하지만, 실제 운영 장애를 기준으로 대응 절차까지 검증한 상태는 아닙니다.

---

## 10.11 롤백 방식

### 서버

**롤백 가능한 구조**

| 요소 | 현재 상태 |
| --- | --- |
| 이전 이미지 태그 보존 | ECR lifecycle policy가 최신 30개를 보존한다(`main.yaml`) |
| 태그 정책 | `<YYYYMMDD-HHMM>-<short sha>` + `latest` 두 개를 매 배포마다 push |
| commit SHA 태그 | full SHA 단독 태그는 없고, 7자리 short SHA가 시각 태그에 포함된다 |
| `latest`만 쓰는가 | 아니다. 다만 **`docker-compose.yml`이 `:latest`를 하드코딩**한다 |
| ECR에서 이전 이미지 재pull | 가능하다(태그가 남아 있으므로) |
| EC2에 이전 컨테이너 유지 | 유지하지 않는다. `compose up -d`가 교체하고, 헬스체크 통과 후 `docker image prune -f`가 dangling 이미지를 정리한다. 즉 **직전 이미지의 로컬 사본은 사라질 수 있고, 롤백은 ECR에서 다시 받는 경로가 된다.** |
| 자동 rollback | **없다.** `cd.yml`에 실패 시 이전 이미지로 되돌리는 job/step이 없다. 헬스체크 실패는 배포를 "실패"로 끝낼 뿐이고, 이미 교체된 컨테이너는 그대로 남는다. |

따라서 현재 가능한 서버 롤백 절차는 다음 둘 중 하나다.

```text
[A] 정상 커밋으로 CD 재실행
    Actions → IMHERE_GITHUB_ACTION_CD → Run workflow (정상 커밋 기준)
      → 새 태그로 다시 빌드·push·배포

[B] EC2에서 이전 ECR 태그로 직접 교체
    1. ECR에서 마지막 정상 태그 확인
    2. EC2의 docker-compose.yml에서 dsko 이미지 참조를 :latest → 해당 태그로 변경
    3. ECR 로그인 후 deploy-imhere.sh 실행
```

[B]는 compose 파일이 `:latest` 고정이므로 **수동 편집이 필요하다.** 이 편집은 다음 CD 배포 때 scp로 덮어써진다.

Git revert 후 재배포도 가능하지만, 그 경우 CI → CD 전체 사이클을 다시 타야 한다.

**실제로 검증된 롤백 절차**

> 이전 이미지로 재배포할 수 있는 구조는 존재하지만, 실제 장애 상황을 가정한 롤백 검증은 수행하지 않았습니다.
> 저장소에는 롤백을 실행한 기록도, 리허설 기록도 없습니다.

### 웹

서버와 달리 **전용 롤백 workflow가 존재한다** (`client/.github/workflows/rollback-web.yml`).

```text
Actions → "Roll back web app" → release_sha 입력 (소문자 40자 SHA)
    ↓
SHA 형식 검증
    ↓
OIDC로 AWS 임시 자격증명
    ↓
s3api head-object 로 app/releases/<sha>/index.html 존재 확인
    ↓
smoke-release.mjs 로 공개 URL 실제 응답 확인
    ↓
Google WIF 토큰 발급
    ↓
update-remote-config.mjs → Firebase Remote Config의 web_app_url 을 이전 release로 변경
```

immutable release(`/app/releases/<sha>/`, `max-age=31536000,immutable`)를 계속 남기기 때문에 업로드·삭제·CloudFront invalidation 없이 포인터만 되돌리는 구조다.

다만 `client/docs/deployment/web-release.md`에 "Required rehearsal before launch"로 A→B→A 롤백 리허설이 **아직 수행해야 할 항목**으로 남아 있다. 즉 웹 롤백도 구조는 있으나 검증 기록은 없다.

### 무중단 배포

- Blue/Green, 다중 인스턴스, ALB, nginx upstream 전환 중 어느 것도 저장소에 없다.
- 서버 배포는 단일 컨테이너를 `compose up -d`로 교체하는 방식이다.
- **무중단 배포를 구성했다고 말할 수 있는 근거가 저장소에 없다.** 배포 중 요청 유실 여부도 측정된 바 없다.

---

## 10.12 현재 운영 검증 수준

| 항목 | 구성 여부 | 실제 검증 여부 | 근거 |
| --- | --- | --- | --- |
| Docker Image Build | O | O | `ImHereServer/Dockerfile.release`, `cd.yml` `docker-push` / CD 실행 `31316044744` 성공 기록 |
| GitHub Actions CI | O | O | `ImHereServer/.github/workflows/ci.yml` / CI 실행 `31315652758` 성공 기록 |
| ECR Push | O | O | `cd.yml` `docker-push`, `infra/cloudformation/main.yaml` / 위 CD 실행 기록 |
| EC2 Deploy | O | O | `cd.yml` `deploy-app`, `infra/scripts/deploy-imhere.sh` / 위 CD 실행 기록 + swagger·docs HTTP 200 확인 |
| Nginx Reverse Proxy | O | O | `infra/nginx/nginx.conf.template` / `https://imhere.ratiko.co.kr/swagger-ui/index.html` HTTP 200 기록 |
| TLS (Let's Encrypt) | O | O | `infra/scripts/deploy-sub-tasks/tls-sub-tasks/do-tls.sh`, TLS 셸 문법 검사 / 위 HTTPS 200 기록 |
| Actuator Health Check | O | O | `application.yaml` `management.*`, `infra/scripts/deploy-sub-tasks/remote-healthcheck.sh` / CD 헬스체크 통과 기록 |
| Grafana Alloy (인스턴스에서 가동) | O | O | `docker-compose.prod.yml`의 `alloy` 서비스, `infra/alloy/alloy-config.alloy` — 인스턴스에서 가동 중(사용자 확인) |
| Prometheus (Grafana Cloud) | O | X | `alloy-config.alloy.template` `prometheus.scrape` → `remote_write` — 수집 결과 확인 근거 없음 |
| Grafana (Grafana Cloud) | O | X | `env/alloy.env` 자격증명 주입 — 대시보드 정의·조회 근거 없음 |
| Prometheus / Grafana 자체 호스팅 | 해당 없음 | 해당 없음 | 사용하지 않음. compose `local` profile에만 남은 정의이며 설정 파일도 없음 |
| Loki | O | X | `alloy-config.alloy.template` `loki.source.docker` / `loki.write` — 실제 로그 추적 근거 없음 |
| Tempo | O | X | `application.yaml` prod OTLP endpoint + `otelcol.exporter.otlphttp` — 실제 trace 조회 근거 없음 |
| Alert (Discord) | O | X | `DiscordErrorAlertAdapter`, `AccessLogPrinter` — 실제 발화 기록 없음 |
| Alert (Prometheus/Alertmanager) | X | X | alert rule·Alertmanager 설정 파일 없음 (`GRAFANA_CLOUD_ALERTMANAGER_*` 키만 존재) |
| 장애 대응 | 부분 O | X | `restart: unless-stopped`, 헬스체크 스크립트, 로그·메트릭 경로 존재 / 실제 장애 대응 기록 없음 |
| Rollback (서버, 수동) | O | X | ECR 태그 보존 + lifecycle 30개, `deploy-imhere.sh` / 롤백 수행·리허설 기록 없음 |
| Rollback (서버, 자동) | X | X | `cd.yml`에 rollback job 없음 |
| Rollback (웹) | O | X | `client/.github/workflows/rollback-web.yml` / `web-release.md`가 리허설을 미수행 과제로 명시 |
| 무중단 배포 | X | X | Blue/Green·다중 인스턴스·upstream 전환 구조 없음. 단일 컨테이너 `compose up -d` 교체 |
| 부하 대응 / 오토스케일링 | X | X | ASG·ALB 없음, `ImHereServer/loadtest/`는 비어 있음 |
| IaC (인프라 정의) | O | O | `infra/cloudformation/main.yaml`, `web-distribution.yml` / 스택 `imhere-prod-infra` 출력값을 CD가 실제로 조회 |

`구성 여부`는 "저장소에 정의가 있다", `실제 검증 여부`는 "저장소 문서에 실행/응답 확인 기록이 있다"는 뜻이다. 두 열은 같은 의미가 아니다.

---

## 현재 미구성 항목

저장소에서 **찾을 수 없는** 것들이다. 필요 여부와 무관하게, 있다고 서술하지 않기 위해 명시한다.

- 자체 호스팅 Prometheus / Grafana — **현재 프로젝트에서는 로컬 전용입니다.** 관측 backend는 전부 Grafana Cloud이고, 운영 인스턴스에서 가동하는 관측 컴포넌트는 Grafana Alloy 하나뿐입니다. 관련 정의는 `docker-compose.local.yml`에만 둡니다.
- Prometheus alert rule, Alertmanager 설정, Grafana alert provisioning — 현재 프로젝트에서는 사용하지 않습니다.
- Grafana 대시보드 정의(JSON) — 저장소에서 확인할 수 없습니다.
- Blue/Green 배포, canary, nginx upstream 전환, 포트 스위칭 — 현재 프로젝트에서는 사용하지 않습니다.
- 자동 롤백 job — 현재 프로젝트에서는 사용하지 않습니다.
- 로드밸런서(ALB/NLB), Auto Scaling Group, 다중 인스턴스 — 현재 프로젝트에서는 사용하지 않습니다.
- 컨테이너 `HEALTHCHECK` 지시자 — Dockerfile·compose 모두에 없습니다.
- 파일 로그(`logs/*.log`)의 Loki 수집 — Alloy는 Docker stdout/stderr만 수집합니다.
- 부하 테스트 시나리오 — `ImHereServer/loadtest/` 디렉터리가 비어 있습니다.
- AWS Secrets Manager / SSM Parameter Store 연동 — 런타임 secret은 private config repo에서 가져옵니다.
- 모바일 스토어 자동 업로드 — `client/.github/workflows/flutter-ci.yml`은 검증만 하고 배포하지 않습니다.

---

## 문서 간 불일치 (확인 필요)

저장소를 읽는 과정에서 발견한, **문서와 실제 설정이 어긋나는 지점**이다.

| # | 내용 | 문서 | 실제 |
| --- | --- | --- | --- |
| 1 | 웹 배포의 AWS 자격증명 | `client/docs/deployment/web-release.md` — "Do not create or store an AWS access key for Actions", OIDC 사용 | `client/.github/workflows/deploy-web.yml` — `secrets.AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` 사용 (단, `rollback-web.yml`은 OIDC) |
| 2 | `LOG_FILE`의 수집 경로 | `BACKEND-DEPLOY-VALUES.md` — "alloy가 이걸 tail 해서 Loki로 보냄" | `alloy-config.alloy.template`에 파일 tail 설정 없음. `loki.source.docker`만 존재 |
| 3 | Discord webhook 종류 | 과거 관측성 문서 — webhook 3종(`ott` 포함), `DiscordMessageSender` / `DiscordUserErrorNotifier` | 코드에는 `AlertChannel.CLIENT_ERROR` / `SERVER_ERROR` 2종, `DiscordErrorAlertAdapter` / `UserErrorAlertNotifier`. `discord.url.ott` 키 없음 |
| 4 | 소비처 없는 env 키 | `remoteConfig/env/server.env`의 `ADMIN_ALLOWED_IPS`, `alloy.env`의 `GRAFANA_CLOUD_ALERTMANAGER_*` | `src/`·`infra/`에서 참조 0건 |

---

## 추가 검증이 필요한 항목

저장소만으로는 판단할 수 없어 사람의 확인이 필요한 항목이다.

### 배포

- [ ] 2026-08-09 이후에도 CD가 성공적으로 실행되고 있는가? (최근 CD 실행 번호와 결과)
- [ ] 실제 트래픽이 존재하는 상태에서 배포를 수행해 본 적이 있는가?
- [ ] 배포 중 `compose up -d` 컨테이너 교체 구간에서 5xx나 커넥션 거절이 발생했는가? 발생했다면 몇 초간인가?
- [ ] `deploy-imhere.sh`의 healthcheck가 실제로 **실패**해서 배포가 중단된 사례가 있는가? 그때 EC2에 남은 컨테이너 상태는 어떻게 처리했는가?
- [ ] CD의 `build-jar`가 CI와 동일한 테스트를 다시 실행하는데, 이 중복을 유지할 이유가 있는가?

### 롤백

- [ ] 이전 Docker 이미지 태그로 서버 롤백을 직접 수행해 본 적이 있는가?
- [ ] 롤백 시 EC2의 `docker-compose.yml`을 수동 편집하는 절차를 실제로 밟아 봤는가? (`:latest` 하드코딩 때문에 필요)
- [ ] `docker image prune -f` 이후 EC2 로컬에 직전 이미지가 남아 있는지 확인해 봤는가?
- [ ] 웹 롤백 workflow(`rollback-web.yml`)를 실제로 실행해 봤는가? A→B→A 리허설을 마쳤는가?
- [ ] 롤백 후 모바일 앱(Android/iOS) cold start 확인까지 수행했는가?

### 관측 / 장애

- [ ] Grafana Cloud에서 애플리케이션 메트릭이 실제로 적재되는 것을 확인했는가? (`up` 지표 포함)
- [ ] Loki에서 `job="imhere-server"` 로그를 실제로 조회해 봤는가?
- [ ] Tempo에서 하나의 요청 trace를 끝까지 추적해 봤는가? 10% 샘플링으로 충분했는가?
- [ ] 로그의 `traceId`(LoggingFilter UUID)와 Tempo의 trace ID를 서로 연결할 수 있는가? 연결되지 않는다면 어느 쪽을 기준으로 삼는가?
- [ ] Discord 5xx 알림이 실제 운영에서 발화한 적이 있는가? 그때 알림→Grafana 추적까지 이어졌는가?
- [ ] 지표 기반 alert(예: 에러율, 응답 시간, 컨테이너 재시작)가 필요한가? 필요하다면 Grafana Cloud 콘솔에 이미 설정되어 있는가?
- [ ] 실제 운영 장애를 겪은 적이 있는가? 있다면 대응 시간과 절차는 어땠는가?

### 인프라 / 보안

- [ ] 운영 EC2의 실제 인스턴스 타입과 EBS 크기는? (`t3.small` 기본값 그대로인가)
- [ ] 외부 MySQL은 RDS인가, 다른 호스팅인가? 백업·복구 절차가 있는가?
- [ ] `deploy-web.yml`의 장기 AWS access key를 OIDC role로 전환할 계획이 있는가? (문서·롤백 workflow와 불일치)
- [ ] `CONFIG_REPO_PAT`와 `EC2_SSH_PRIVATE_KEY`의 rotation 주기가 정해져 있는가?
- [ ] EC2의 `secrets/imhereFirebaseKey.json`이 평문으로 남는 것을 허용하는가? 대안(Secrets Manager 등)을 검토했는가?
- [ ] `ADMIN_ALLOWED_IPS`와 `GRAFANA_CLOUD_ALERTMANAGER_*`는 앞으로 쓸 예정인가, config repo에서 제거할 것인가?
- [ ] management endpoint(`${MGMT_BASE_PATH}`)가 nginx를 통해 외부 노출되는데, 경로 난독화만으로 충분하다고 보는가? (`health.show-details: always` 포함)
---
# 수정 필요
---
---
