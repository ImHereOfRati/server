# CI/CD

이 문서는 GitHub Actions 워크플로(`ci.yml`, `cd.yml`)의 동작과, 배포 과정에서 사용하는 환경 변수를 어디에 설정해야 하는지를 다룹니다.

---

## 한눈에 보기

* **CI**(`IMHERE_GITHUB_ACTION_CI`): 모든 브랜치 push, main 대상 PR에서 테스트만 실행합니다. 배포는 하지 않습니다.
* **CD**(`IMHERE_GITHUB_ACTION_CD`): CI가 main에서 성공하면 자동으로 이어서 실행됩니다. JAR 빌드 → Docker 이미지 빌드/Push → EC2 배포까지 수행합니다.
* CD는 `workflow_dispatch`로 수동 실행도 가능합니다.

---

## CI 워크플로 (`ci.yml`)

| 단계 | 내용 |
|---|---|
| 트리거 | 모든 브랜치 `push`, `main` 대상 `pull_request` |
| 테스트 | `./gradlew test` (`TESTCONTAINERS_RYUK_DISABLED=true`로 Testcontainers 안정화) |
| 산출물 | 테스트 리포트, JaCoCo 커버리지 리포트 |

CI가 통과해야 CD가 시작됩니다.

---

## CD 워크플로 (`cd.yml`)

### Job 의존 관계

```mermaid
flowchart LR
    CI["CI 성공 (main)"] --> BuildJar["build-jar"]
    CI --> ResolveInfra["resolve-infra"]

    BuildJar --> DockerPush["docker-push"]
    ResolveInfra --> DockerPush

    ResolveInfra --> PrepareServer["prepare-server"]
    PrepareServer --> SyncConfig["sync-config"]
    ResolveInfra --> SyncConfig

    DockerPush --> DeployApp["deploy-app"]
    PrepareServer --> DeployApp
    ResolveInfra --> DeployApp
    SyncConfig --> DeployApp

    ResolveInfra --> Cleanup["cleanup"]
    DeployApp --> Cleanup
```

### Job별 설명

| Job | 내용 |
|---|---|
| **build-jar** | `./gradlew bootJar -x test`로 JAR를 빌드해 아티팩트로 올립니다(1일 보관). |
| **resolve-infra** | CloudFormation 스택 `imhere-prod-infra`의 Output(`ElasticIp`, `RabbitMqPrivateIp`, `SecurityGroupId`, `Ec2InstanceId`, `EcrRepositoryName`, `EcrRepositoryUri`)을 읽어 이후 Job에 전달합니다. |
| **docker-push** | OIDC로 AWS Role을 assume(장기 Access Key 없음) → `Dockerfile.release`로 이미지 빌드 → ECR에 날짜-SHA 태그 + `latest`로 Push. |
| **prepare-server** | GitHub Actions 러너의 공인 IP를 앱 EC2 Security Group에 SSH(22)로 임시 허용 → Docker/Compose/Certbot 설치 확인 → 배포 디렉터리 생성. |
| **sync-config** | `infra/scripts/sync-config.sh`로 private config repo(`ImHereOfRati/config`)를 clone해 `prod.env`와 Firebase 키를 아티팩트로 만듭니다. |
| **deploy-app** | `prod.env`를 불러오고 `RABBITMQ_HOST`를 CloudFormation Output으로 주입 → `nginx.conf.template`/`alloy-config.alloy.template`을 렌더링 → `docker-compose.yml`, 렌더된 설정, `prod.env`, Firebase 키를 앱 EC2로 전송 → `certbot renew` → nginx reload → ECR pull → `docker compose --profile prod up -d` → EC2의 임시 파일 정리. |
| **cleanup** | `prepare-server`에서 열었던 SSH(22) 허용 규칙을 회수하고, 러너의 임시 파일을 지웁니다. `if: always()`라 배포가 실패해도 항상 실행됩니다. |

---

## 환경 변수 설정 가이드

환경 변수는 **설정 위치가 서로 다른 네 그룹**으로 나뉩니다. 값을 바꿔야 할 때 어디를 고쳐야 하는지 헷갈리기 쉬워서 그룹별로 정리합니다.

### 1. GitHub Secrets (`Settings → Secrets and variables → Actions`)

`cd.yml`이 직접 참조하는 값입니다. 코드가 아니라 **GitHub 저장소 설정에서만** 등록·수정합니다.

| Secret | 설명 | 형식/예시 |
|---|---|---|
| `AWS_REGION` | CloudFormation 스택과 동일한 리전 | `ap-northeast-2` |
| `AWS_DEPLOY_ROLE_ARN` | GitHub Actions가 assume할 IAM Role ARN. CloudFormation Output `GitHubActionsRoleArn` 값을 그대로 넣습니다 | `arn:aws:iam::<account-id>:role/imhere-github-actions-deploy` |
| `EC2_USER` | 앱 EC2 SSH 사용자 | `ec2-user` |
| `EC2_SSH_PRIVATE_KEY` | `KeyName`(예: `imhere-prod-key`)에 대응하는 PEM 키 **전체 내용** | `-----BEGIN OPENSSH PRIVATE KEY----- ...` |
| `EC2_DEPLOY_PATH` | 앱 EC2에서 배포 파일을 두는 절대 경로 | `/home/ec2-user/imhere` |
| `CONFIG_REPO_PAT` | private config repo(`ImHereOfRati/config`)를 clone할 GitHub PAT (read 권한만 필요) | `ghp_xxxxxxxx` |

`AWS_DEPLOY_ROLE_ARN`을 바꾸려면 `aws.md`의 `RepositorySlug`/`MainBranchRef` 파라미터로 OIDC 신뢰 조건도 같이 맞춰야 합니다.

### 2. Private config repo(`ImHereOfRati/config`)의 `prod.env`

`sync-config` Job이 이 repo를 clone해 `prod.env`를 가져오고, `deploy-app` Job이 `nginx.conf.template` / `alloy-config.alloy.template`를 렌더링한 뒤 앱 EC2로 전송합니다. **이 repo는 ImHere Server와 분리된 별도 저장소**이므로, 값을 바꿀 때는 그 repo의 `prod.env`를 수정해야 합니다.

| 분류 | 변수 | 비고 |
|---|---|---|
| 서버/Nginx | `SERVER_NAME`, `CERT_DOMAIN`, `NGINX_ALLOWED_ORIGIN`, `MGMT_BASE_PATH` | `nginx.conf.template`/`alloy-config.alloy.template` 렌더링에도 쓰임 |
| Spring 공통 | `SPRING_PROFILES_ACTIVE`, `SECURITY_WHITELIST`, `CORS_ALLOWED_ORIGINS`, `JWT_SECRET`, `LOG_FILE` | |
| DB | `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_POOL_SIZE` | 가비아 MySQL 접속 정보 — `gabia.md` 참고 |
| RabbitMQ | `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`, `RABBITMQ_VHOST`, `RABBITMQ_CONCURRENCY`, `RABBITMQ_MAX_CONCURRENCY` | `RABBITMQ_HOST`는 여기 적지 않습니다 — CD가 CloudFormation Output으로 자동 주입하며 `prod.env`에 덮어씁니다 |
| Firebase | `FIREBASE_PATH` | 컨테이너 내부 경로(`/app/secrets/imhereFirebaseKey.json`), 키 파일 자체는 `imhereFirebaseKey.json`으로 별도 전송 |
| Admin | `ADMIN_ID`, `ADMIN_ALLOWED_IPS` | |
| SMS(Solapi) | `SOLAPI_SENDER`, `SOLAPI_API_KEY`, `SOLAPI_API_SECRET` | |
| Discord | `DISCORD_WEBHOOK_ERROR_SERVER`, `DISCORD_WEBHOOK_ERROR_CLIENT`, `DISCORD_WEBHOOK_OTT` | |
| Observability | `GRAFANA_CLOUD_LOKI_ENDPOINT`/`_USER`/`_API_KEY`, `GRAFANA_CLOUD_PROM_ENDPOINT`/`_USER`/`_API_KEY`, `GRAFANA_CLOUD_TEMPO_ENDPOINT`/`_USER`/`_API_KEY` | Grafana Cloud 자격증명은 Alloy 전용. Loki=로그, Prometheus=메트릭, Tempo=트레이스 |

### 3. CloudFormation에서 자동으로 도출되는 값 (직접 설정하지 않음)

다음 값은 어디에도 수동으로 적지 않습니다. `resolve-infra` Job이 `aws cloudformation describe-stacks`로 Output을 읽어 이후 Job에 환경 변수로 흘려줍니다.

| 값 | CloudFormation Output | 쓰이는 곳 |
|---|---|---|
| `EC2_HOST` | `ElasticIp` | SSH 접속, 배포 대상 |
| `RABBITMQ_HOST` | `RabbitMqPrivateIp` | `prod.env`에 자동 추가 |
| `EC2_SECURITY_GROUP_ID` | `SecurityGroupId` | 배포 중 SSH(22) 임시 허용/회수 |
| `ECR_REPOSITORY_NAME` / `ECR_REPOSITORY_URI` | `EcrRepositoryName` / `EcrRepositoryUri` | 이미지 push/pull 대상 |

이 값들을 바꾸려면 GitHub Secrets나 `prod.env`가 아니라 **CloudFormation 스택 자체**(`aws.md`)를 업데이트해야 합니다.

### 4. CloudFormation 스택 파라미터 (`--parameter-overrides`)

스택을 생성/업데이트할 때만 지정하는 값입니다. 상세 목록과 기본값은 [aws.md의 파라미터 표](aws.md#파라미터)를 참고합니다. 예: `KeyName`, `AppInstanceType`, `RabbitMqInstanceType`, `EcrRepositoryName`, `RabbitMqUser`/`RabbitMqPassword`/`RabbitMqVHost`.

```bash
aws cloudformation deploy \
  --stack-name imhere-prod-infra \
  --template-file infra/cloudformation/main.yaml \
  --region ap-northeast-2 \
  --parameter-overrides KeyName=imhere-prod-key AppInstanceType=t3.small RabbitMqInstanceType=t3.micro \
  --capabilities CAPABILITY_NAMED_IAM
```

---

## 관련 문서

* AWS 인프라(VPC/EC2/SG/EIP/ECR/IAM)는 `aws.md`를 참고합니다.
* 가비아 DNS/MySQL은 `gabia.md`를 참고합니다.
* Docker 이미지/Compose/Nginx 구성은 `docker.md`를 참고합니다.
