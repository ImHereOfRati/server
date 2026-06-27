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

> 기준 파일: `.github/workflows/cd.yml` (커밋 `2634fca`, 2026-06-27 시점). 인프라는 변경이 잦으므로, 이 절은 해당 시점의 파일을 근거로 합니다.

현재 CD는 **5개 Job**으로 구성됩니다. 예전에 별도 Job이던 `prepare-server`(EC2 준비)와 `cleanup`(SG 회수·임시 파일 정리)은 **`deploy-app` 내부 스텝으로 흡수**되었습니다. 배포 직전에만 EC2 SSH(22)/80 포트를 임시로 열고, 배포가 끝나거나 실패하면 같은 Job 안에서 곧바로 닫기 위해서입니다(같은 Job이어야 `if: always()`로 확실히 회수됩니다).

```mermaid
flowchart LR
    CI["CI 성공 (main)"] --> BuildJar["build-jar"]
    CI --> ResolveInfra["resolve-infra"]

    BuildJar --> DockerPush["docker-push"]
    ResolveInfra --> DockerPush

    ResolveInfra --> SyncConfig["sync-config"]

    DockerPush --> DeployApp["deploy-app"]
    ResolveInfra --> DeployApp
    SyncConfig --> DeployApp
```

### Job별 설명

| Job | `needs` | 내용 |
|---|---|---|
| **build-jar** | — | `./gradlew bootJar -x test`로 JAR를 빌드해 아티팩트로 올립니다(1일 보관, `cd.yml:53-63`). |
| **resolve-infra** | — | CloudFormation 스택 `imhere-prod-infra`의 Output(`ElasticIp`, `RabbitMqPrivateIp`, `SecurityGroupId`, `Ec2InstanceId`, `EcrRepositoryName`, `EcrRepositoryUri`)을 읽어 이후 Job에 전달합니다(`cd.yml:93-114`). |
| **docker-push** | `build-jar`, `resolve-infra` | OIDC로 AWS Role을 assume(장기 Access Key 없음) → `Dockerfile.release`로 이미지 빌드 → ECR에 날짜-SHA 태그 + `latest`로 Push(`cd.yml:165-176`). |
| **sync-config** | `resolve-infra` | `infra/scripts/sync-config.sh`로 private config repo(`ImHereOfRati/config`)를 clone해 `prod.env`와 Firebase 키를 아티팩트로 만듭니다(`cd.yml:187-200`). |
| **deploy-app** | `docker-push`, `resolve-infra`, `sync-config` | 배포 본체. 아래 "deploy-app 스텝 순서" 참고. |

#### deploy-app 스텝 순서

`deploy-app` 한 Job 안에서 다음 순서로 진행됩니다(`cd.yml:202-498`).

| 순서 | 스텝 | 근거 |
|---|---|---|
| 1 | 러너 공인 IP 조회 후 EC2 SG에 SSH(22) 임시 허용 | `cd.yml:233-245` |
| 2 | Let's Encrypt HTTP-01 챌린지용으로 80 포트를 `0.0.0.0/0`에 임시 개방 | `cd.yml:247-253` |
| 3 | EC2 런타임 디렉터리 준비(Docker/Compose/Certbot 설치 확인, 배포 경로 생성) | `cd.yml:262-278` |
| 4 | `prod.env`에 `RABBITMQ_HOST` 주입 → `nginx.conf.template`/`alloy-config.alloy.template` 렌더링 → `docker-compose.yml`·렌더 결과·`prod.env`·Firebase 키를 EC2로 전송 | `cd.yml:291-334` |
| 5 | SSH로 ECR 로그인 → compose config/`nginx -t` 검증 → `docker compose --profile prod pull` → `up -d` | `cd.yml:340-433` |
| 6 | **Let's Encrypt 인증서 확보**(검증→갱신/재발급/부트스트랩) 후 `nginx -s reload` | `cd.yml:435-471` |
| 7 | `if: always()` — SSH(22)·80 포트 허용 규칙 회수, 러너 임시 파일 삭제 | `cd.yml:475-498` |

6번 단계가 이 문서의 핵심입니다. 자세한 동작은 아래 [Let's Encrypt 인증서 발급 로직](#lets-encrypt-인증서-발급-로직)에서 다룹니다.

---

## Let's Encrypt 인증서 발급 로직

`deploy-app`의 마지막 SSH 스크립트(`cd.yml:340-471`)는 매 배포마다 HTTPS 인증서를 "있으면 갱신, 없거나 깨졌으면 재발급, 그래도 안 되면 임시 인증서"로 확보합니다. 이 절에서 쓰는 프로젝트 고유 함수 세 개를 먼저 짚습니다.

| 함수 | 한 줄 설명 | 정의 |
|---|---|---|
| `force_reset_lineage()` | `live`/`archive`/`renewal` 세 디렉터리를 통째로 삭제해 certbot이 인식하는 인증서 계보(lineage)를 0으로 되돌린다 | `cd.yml:362-364` |
| `generate_bootstrap_cert()` | 자체 서명(self-signed) 임시 인증서를 만든다. **항상 `force_reset_lineage`를 먼저 호출**한다 | `cd.yml:366-394` |
| `lineage_is_valid()` | 기존 인증서 계보가 certbot 기준으로 정상인지(`fullchain`/`cert`/`chain` 정합) 검사한다 | `cd.yml:398-405` |

> 용어: 여기서 **lineage(계보)** 는 certbot이 한 도메인의 인증서를 갱신해 나가는 단위입니다 — `/etc/letsencrypt/live/<도메인>`(현재 인증서 심볼릭 링크), `/archive/<도메인>`(과거 버전 실파일), `/renewal/<도메인>.conf`(갱신 설정) 세 곳이 한 묶음입니다.

### 왜 `test -f`만으로는 부족했나 (과거 장애)

이전 버전은 인증서 파일이 디스크에 "있는지"만 봤습니다(`test -f .../fullchain.pem`). 하지만 파일이 존재해도 **`fullchain.pem`이 실제 `cert.pem`/`chain.pem`과 어긋난 상태**(이른바 fullchain desync)가 생길 수 있습니다. 이 경우:

* 파일은 멀쩡히 존재 → `test -f`는 통과
* 그러나 `certbot renew`는 `fullchain does not match cert + chain` 오류로 실패
* nginx는 깨진 `fullchain.pem`을 로드 → **HTTPS가 죽은 채 배포가 "성공"으로 끝남**

존재 여부와 정합성은 다른 문제인데, `test -f`는 전자만 봅니다. 그래서 desync가 한 번 생기면 매 배포마다 같은 실패가 반복됐습니다.

### `lineage_is_valid()` — 정합성까지 확인

이제는 파일 존재가 아니라 certbot 자신에게 계보 상태를 물어봅니다(`cd.yml:398-405`).

```bash
lineage_is_valid() {
  sudo test -f "$CERT_RENEWAL_CONF" || return 1
  CERT_STATUS=$(sudo certbot certificates --cert-name "$CERT_DOMAIN" 2>&1) || return 1
  if echo "$CERT_STATUS" | grep -qE "does not match|INVALID|No certificates found"; then
    return 1
  fi
  return 0
}
```

* `renewal/<도메인>.conf`가 없으면 애초에 정식 계보가 아니므로 즉시 실패 처리.
* `certbot certificates --cert-name`은 계보를 점검하며 `fullchain.pem does not match...` 같은 진단 문구를 출력합니다. 그 출력에서 **`does not match`(desync) / `INVALID`(만료·손상) / `No certificates found`(계보 없음)** 를 잡아내 사전에 걸러냅니다.

즉, 과거에 배포를 망가뜨리던 desync를 `certbot renew` **이전에** 탐지해 재발급 경로로 보냅니다.

### `generate_bootstrap_cert()`가 항상 `force_reset_lineage`를 먼저 하는 이유

부트스트랩 인증서는 nginx를 일단 443으로 띄우기 위한 자체 서명 인증서이지, certbot이 관리하는 정식 계보가 아닙니다. 그런데 과거 `renewal`/`archive`가 남은 상태에서 `live/`에만 self-signed를 덮어쓰면, 다음 배포의 `certbot renew`가 **이 self-signed를 정식 인증서로 착각**해 갱신을 시도하고, 그 결과가 바로 fullchain desync입니다. 코드 주석도 같은 경고를 답니다(`cd.yml:367-369`).

```bash
generate_bootstrap_cert() {
  # bootstrap self-signed는 certbot lineage가 아니다. 과거 renewal/archive가
  # 남아 있으면 certbot renew가 이 self-signed를 정식 cert로 착각해 desync
  # (fullchain does not match cert + chain) 된다. 항상 lineage 먼저 비운다.
  force_reset_lineage
  sudo mkdir -p "$CERT_LIVE_DIR"
  ...
}
```

`force_reset_lineage`를 항상 선행함으로써 self-signed와 certbot 정식 계보가 **절대 같은 도메인 디렉터리에서 공존하지 못하게** 막습니다. desync의 원인 자체(잔여 계보 + self-signed 혼재)를 차단하는 셈입니다.

### 전체 인증서 흐름

배포 직전 부트스트랩(없을 때만)과 본 발급 분기는 다음과 같습니다(`cd.yml:417-469`).

```mermaid
flowchart TD
    Start["compose up -d 직후"] --> Missing{"live/fullchain·privkey<br/>둘 다 있나?<br/>(cd.yml:417)"}
    Missing -- "없음" --> Boot1["generate_bootstrap_cert<br/>(self-signed로 443 일단 기동)"]
    Missing -- "있음" --> Valid
    Boot1 --> Valid{"lineage_is_valid?<br/>(cd.yml:447)"}

    Valid -- "유효" --> Renew{"certbot renew 성공?<br/>(cd.yml:448)"}
    Renew -- "성공" --> Done["검증된 계보 유지"]
    Renew -- "실패" --> Reset1["force_reset_lineage<br/>→ issue_certificate"]

    Valid -- "무효/누락" --> Reset2["force_reset_lineage<br/>→ issue_certificate<br/>(cd.yml:461-463)"]

    Reset1 --> Issue1{"발급 성공?"}
    Reset2 --> Issue2{"발급 성공?"}
    Issue1 -- "성공" --> Done2["정식 인증서 발급"]
    Issue1 -- "실패" --> Boot2["generate_bootstrap_cert<br/>(부트스트랩 복구)"]
    Issue2 -- "성공" --> Done2
    Issue2 -- "실패" --> Boot2

    Done --> Reload["nginx -s reload (cd.yml:471)"]
    Done2 --> Reload
    Boot2 --> Reload
```

세 갈래로 정리하면:

1. **계보 유효 → renew**: `lineage_is_valid`가 통과하면 `certbot renew --webroot`만 돌립니다(`cd.yml:447-449`). 멀쩡한 인증서를 불필요하게 재발급해 Let's Encrypt rate limit을 소모하지 않으려는 선택입니다.
2. **무효/누락 → 정리 후 재발급**: 계보가 없거나 깨졌으면 `force_reset_lineage`로 잔재를 지우고 `issue_certificate`(아래)로 새로 받습니다(`cd.yml:460-468`). 유효한데 renew만 실패한 경우도 같은 재발급 경로로 떨어집니다(`cd.yml:451-454`).
3. **재발급도 실패 → 부트스트랩 fallback**: 발급이 실패해도(예: DNS 전파 지연, rate limit) `generate_bootstrap_cert`로 self-signed를 깔아 **nginx가 443에서 죽지 않게** 합니다(`cd.yml:456-457`, `466-467`). HTTPS 신뢰는 깨지지만 서비스 자체는 떠 있고, 다음 배포가 자동으로 정식 인증서를 재시도합니다.

`issue_certificate()`는 webroot 방식의 `certbot certonly`입니다(`cd.yml:435-445`).

```bash
issue_certificate() {
  sudo certbot certonly --webroot \
    -w "$EC2_DEPLOY_PATH/infra/nginx/certbot" \
    --non-interactive --agree-tos \
    --register-unsafely-without-email \
    --keep-until-expiring \
    --cert-name "$CERT_DOMAIN" \
    -d "$CERT_DOMAIN" -d "$CERT_ALT_DOMAIN"
}
```

`CERT_ALT_DOMAIN`은 `www.$CERT_DOMAIN`으로, 루트 도메인과 `www` 서브도메인을 한 인증서에 함께 담습니다(`cd.yml:356`, `443-444`).

### 왜 standalone이 아니라 webroot인가

certbot의 HTTP-01 챌린지는 두 방식이 있습니다. **standalone**은 certbot이 직접 80 포트를 잡아 임시 웹서버를 띄우고, **webroot**는 이미 떠 있는 웹서버의 문서 루트에 챌린지 토큰 파일만 떨어뜨립니다. 이 배포는 webroot를 씁니다.

이유는 단순합니다 — 인증서 단계에 도달했을 땐 **nginx-container가 이미 80/443을 점유**(`docker-compose.yml:116-117`)하고 있어서, standalone을 쓰면 80 포트 충돌로 실패합니다. webroot는 nginx를 그대로 둔 채 같은 80 포트로 챌린지를 처리할 수 있습니다.

동작이 성립하는 연결 고리는 **하나의 호스트 디렉터리를 certbot과 nginx가 공유**하는 데 있습니다.

| 구성 요소 | 경로 | 근거 |
|---|---|---|
| certbot이 챌린지 토큰을 쓰는 곳(호스트) | `$EC2_DEPLOY_PATH/infra/nginx/certbot` | `cd.yml:437`, `448` |
| 같은 디렉터리를 nginx에 bind mount | → 컨테이너 `/var/www/certbot` | `docker-compose.yml:124` |
| nginx가 챌린지 요청을 서빙 | `location ^~ /.well-known/acme-challenge/ { root /var/www/certbot; }` | `nginx.conf.template:42-44` |

certbot(호스트에서 실행)이 토큰 파일을 쓰면, 같은 디렉터리를 보고 있는 nginx 컨테이너가 `http://<도메인>/.well-known/acme-challenge/...` 요청에 그 파일을 그대로 돌려줍니다. Let's Encrypt 검증 서버는 이걸 읽어 도메인 소유를 확인합니다. 80 포트는 이 검증을 위해 배포 동안만 `0.0.0.0/0`에 열렸다가(`cd.yml:247-253`) 끝나면 회수됩니다(`cd.yml:484-491`).

발급된 인증서(`/etc/letsencrypt`)는 nginx에 읽기 전용으로 마운트되어(`docker-compose.yml:125`) `ssl_certificate`/`ssl_certificate_key`로 로드되고(`nginx.conf.template:56-57`), 마지막 `nginx -s reload`(`cd.yml:471`)로 새 인증서가 무중단 반영됩니다.

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
