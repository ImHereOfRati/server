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

> 기준 파일: `.github/workflows/cd.yml`과 `infra/scripts/remote-*.sh` (2026-08-07 시점). 인프라는 변경이 잦으므로 줄 번호 대신 스텝 이름과 스크립트 파일명을 근거로 적습니다.

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
| **resolve-infra** | — | CloudFormation 스택 `imhere-prod-infra`의 앱 EC2·보안 그룹·ECR Output을 읽어 이후 Job에 전달합니다. |
| **docker-push** | `build-jar`, `resolve-infra` | OIDC로 AWS Role을 assume(장기 Access Key 없음) → `Dockerfile.release`로 이미지 빌드 → ECR에 날짜-SHA 태그 + `latest`로 Push(`cd.yml:165-176`). |
| **sync-config** | `resolve-infra` | `infra/scripts/sync-config.sh`로 private config repo(`ImHereOfRati/config`)를 clone해 `env/*.env` 전량과 Firebase 키를 아티팩트로 만듭니다. `env/` 아래에 `.env`가 하나도 없으면 여기서 배포가 멈춥니다. |
| **deploy-app** | `docker-push`, `resolve-infra`, `sync-config` | 배포 본체. 아래 "deploy-app 스텝 순서" 참고. |

#### deploy-app 스텝 순서

EC2에서 실제로 도는 로직은 워크플로에 인라인으로 박혀 있지 않고 `infra/scripts/remote-*.sh` 네 파일로 나뉘어 있습니다. 워크플로는 그것들을 순서대로 호출하는 역할만 합니다. 덕분에 로컬에서 `bash -n`/shellcheck로 검사할 수 있고, 실패했을 때 어느 단계인지 스텝 이름만 보고 알 수 있습니다.

| 순서 | 스텝 | 실행 주체 |
|---|---|---|
| 1 | 러너 공인 IP 조회 후 EC2 SG에 SSH(22) 임시 허용 | 워크플로 |
| 2 | Let's Encrypt HTTP-01 챌린지용으로 80 포트를 `0.0.0.0/0`에 임시 개방 | 워크플로 |
| 3 | SSH 키 설치 | 워크플로 |
| 4 | **EC2 호스트 준비** — Docker/Compose/Certbot 설치 확인, 런타임 디렉터리 생성 | `remote-provision.sh` |
| 5 | ECR 레지스트리 주소 도출, 런타임 아티팩트 내려받기 | 워크플로 |
| 6 | `nginx.conf.template`/`alloy-config.alloy.template` 렌더링 → `docker-compose.yml`·렌더 결과·`env/*.env`·Firebase 키를 EC2로 전송 | 워크플로 |
| 7 | 원격 배포 스크립트 3개를 EC2로 전송 | 워크플로 |
| 8 | **부트스트랩 인증서 확보** — nginx가 기동할 수 있는 최소 조건 | `remote-tls.sh bootstrap` |
| 9 | **컨테이너 롤아웃** — ECR 로그인 → compose config/`nginx -t` 검증 → `pull` → `up -d` | `remote-rollout.sh` |
| 10 | **Let's Encrypt 발급/갱신** 후 `nginx -s reload` | `remote-tls.sh issue` |
| 11 | **헬스체크** — 앱이 실제로 살아 있는지 확인 | `remote-healthcheck.sh` |
| 12 | dangling 이미지 정리 | 워크플로 |
| 13 | `if: always()` — SSH(22)·80 포트 허용 규칙 회수, 러너 임시 파일 삭제 | 워크플로 |

4번만 스크립트를 stdin으로 흘려보냅니다(`ssh ... bash -s < remote-provision.sh`). 이 스텝이 돌기 전에는 호스트에 배포 디렉터리 자체가 없어서 `scp`할 곳이 없기 때문입니다.

##### 왜 TLS가 8번과 10번으로 쪼개져 있나

닭과 달걀 문제 때문입니다.

* nginx는 기동할 때 `ssl_certificate` 파일을 읽습니다. 파일이 없으면 `nginx -t`부터 실패해서 컨테이너가 아예 뜨지 않습니다.
* 그런데 Let's Encrypt의 HTTP-01 webroot 검증은 **nginx가 이미 떠서** `/.well-known/acme-challenge/`를 서빙하고 있어야 성립합니다.

그래서 "일단 뜰 수 있는 인증서"(8번, 없을 때만 self-signed)와 "진짜 인증서"(10번)를 컨테이너 기동을 사이에 두고 분리했습니다.

##### 11번 헬스체크가 필요한 이유

`docker compose up -d`는 **컨테이너 생성**만 보장합니다. 기동 직후 죽는 경우 — 누락된 환경변수, `ddl-auto: validate`가 잡아낸 스키마 불일치 등 — 에도 명령 자체는 0을 반환합니다. 헬스체크가 없던 시절엔 이런 실패가 CD에서 **성공으로 오보**됐습니다.

앱 이미지에는 curl이 없어서, 같은 compose 네트워크에 있는 nginx 컨테이너의 busybox `wget`으로 관리 포트를 직접 찌릅니다.

```
http://iamhere-server-container:4861${MGMT_BASE_PATH}/health
```

30초 간격 10회(총 5분) 재시도하고, 응답 본문에 `"status":"UP"`이 나오면 통과입니다. 끝내 실패하면 앱 로그 마지막 200줄을 출력하고 Job을 실패시킵니다.

12번(이미지 정리)이 헬스체크 **뒤에** 있는 것도 의도적입니다. 새 `:latest`를 pull하면 직전 이미지가 dangling이 되는데, 헬스체크 전에 `docker image prune`을 돌리면 롤백 대상이 사라집니다.

8·10번 단계는 이 문서의 핵심입니다. 자세한 동작은 아래 [Let's Encrypt 인증서 발급 로직](#lets-encrypt-인증서-발급-로직)에서 다룹니다.

---

## Let's Encrypt 인증서 발급 로직

`infra/scripts/remote-tls.sh`가 매 배포마다 HTTPS 인증서를 확보합니다. 서브커맨드 두 개(`bootstrap`, `issue`)로 나뉘고, 컨테이너 기동을 사이에 두고 호출됩니다.

이 절에서 쓰는 함수들을 먼저 짚습니다.

| 함수 | 한 줄 설명 |
|---|---|
| `generate_bootstrap_cert()` | 자체 서명(self-signed) 임시 인증서를 만든다. nginx를 443에서 일단 띄우는 용도 |
| `lineage_is_valid()` | 기존 인증서 계보가 certbot 기준으로 정상인지(`fullchain`/`cert`/`chain` 정합) 검사한다 |
| `purge_unusable_renewal_conf()` | Certbot이 `missing a required file reference`로 판정했고 정식 archive도 비어 있는 conf만 제거한다 |
| `reclaim_suffixed_lineage()` | 정식 계보가 없을 때 남은 `<도메인>-NNNN` 계보를 정리해 정식 이름을 회수한다 |
| `has_renewal_conf()` / `has_live_cert()` | 갱신 설정 파일 / 현재 인증서 파일의 존재 여부 |
| `is_letsencrypt_cert()` / `is_bootstrap_cert()` | 현재 인증서의 발급자가 Let's Encrypt인지, 자체 서명인지 판별 |
| `issue_certificate()` | webroot 방식 `certbot certonly`로 신규 발급 |

> 용어: 여기서 **lineage(계보)** 는 certbot이 한 도메인의 인증서를 갱신해 나가는 단위입니다 — `/etc/letsencrypt/live/<도메인>`(현재 인증서 심볼릭 링크), `/archive/<도메인>`(과거 버전 실파일), `/renewal/<도메인>.conf`(갱신 설정) 세 곳이 한 묶음입니다.

### 설계 원칙: 의심스러우면 손대지 말고 실패한다

이 스크립트에서 가장 중요한 건 **하지 않는 일**입니다.

예전 버전에는 `force_reset_lineage()`라는 함수가 있었습니다. `live`/`archive`/`renewal`을 통째로 지워 계보를 0으로 되돌리고 재발급하는 함수로, 발급이 실패하면 self-signed를 깔아 서비스를 살려 두었습니다. 자동 복구처럼 보이지만 실제로는 **운영 TLS를 자동으로 파괴하는 경로**였습니다. DNS 전파 지연이나 Let's Encrypt rate limit처럼 일시적인 이유로 발급이 실패해도, 멀쩡하던 정식 인증서는 이미 지워진 뒤였고 사용자에게는 브라우저 경고가 뜨는 self-signed가 나갔습니다.

지금은 그 함수가 없습니다. 계보가 수상하면 원칙적으로 **아무것도 지우지 않고 배포를 실패시킵니다.** 단, Certbot이 필수 참조 누락을 명시적으로 진단하고 정식 archive가 비어 있어 실제 인증서가 아님을 함께 확인한 renewal conf는 제거할 수 있습니다. 이 예외는 실패한 최초 발급이 남긴 빈 메타데이터만 대상으로 합니다.

| 상황 | 예전 동작 | 현재 동작 |
|---|---|---|
| 계보 유효, `renew` 실패 | 계보 삭제 후 재발급 시도 | 현재 인증서 **유지**하고 배포 실패 |
| `renewal` conf는 있는데 계보가 깨지고 실제 archive가 있음 | 계보 삭제 후 재발급 | **손대지 않고** 배포 실패 |
| `renewal` conf가 필수 참조 없이 깨졌고 실제 archive가 없음 | (구분하지 않음) | 빈 conf와 접미사 계보를 정리한 뒤 정식 이름으로 최초 발급 재시도 |
| Let's Encrypt 인증서인데 갱신 메타데이터가 없음 | (구분하지 않음) | **손대지 않고** 배포 실패 |
| 인증서가 아예 없음 | 부트스트랩 후 발급 | 부트스트랩 후 발급 (동일) |

배포가 실패하면 사람이 들어가 원인을 보고 판단해야 합니다. 느리지만, HTTPS가 조용히 망가진 채 "성공"으로 끝나는 것보다 낫습니다.

`generate_bootstrap_cert()`도 이제 계보를 지우지 않습니다. 대신 **`bootstrap` 단계가 "live 인증서도 renewal conf도 둘 다 없을 때"만** 이 함수를 부릅니다. self-signed와 정식 계보가 같은 디렉터리에서 공존하는 상황 자체를 호출 조건으로 막는 것이고, 목적은 예전과 같지만 파괴적이지 않은 방식입니다.

### 왜 `test -f`만으로는 부족했나 (과거 장애)

이전 버전은 인증서 파일이 디스크에 "있는지"만 봤습니다(`test -f .../fullchain.pem`). 하지만 파일이 존재해도 **`fullchain.pem`이 실제 `cert.pem`/`chain.pem`과 어긋난 상태**(이른바 fullchain desync)가 생길 수 있습니다. 이 경우:

* 파일은 멀쩡히 존재 → `test -f`는 통과
* 그러나 `certbot renew`는 `fullchain does not match cert + chain` 오류로 실패
* nginx는 깨진 `fullchain.pem`을 로드 → **HTTPS가 죽은 채 배포가 "성공"으로 끝남**

존재 여부와 정합성은 다른 문제인데, `test -f`는 전자만 봅니다. 그래서 desync가 한 번 생기면 매 배포마다 같은 실패가 반복됐습니다.

### `lineage_is_valid()` — 정합성까지 확인

이제는 파일 존재가 아니라 certbot 자신에게 계보 상태를 물어봅니다(`remote-tls.sh`).

```bash
lineage_is_valid() {
  has_renewal_conf || return 1
  local status
  status=$(sudo certbot certificates --cert-name "$CERT_DOMAIN" 2>&1) || return 1
  if echo "$status" | grep -qE "does not match|INVALID|No certificates found"; then
    return 1
  fi
  return 0
}
```

* `renewal/<도메인>.conf`가 없으면 애초에 정식 계보가 아니므로 즉시 실패 처리.
* `certbot certificates --cert-name`은 계보를 점검하며 `fullchain.pem does not match...` 같은 진단 문구를 출력합니다. 그 출력에서 **`does not match`(desync) / `INVALID`(만료·손상) / `No certificates found`(계보 없음)** 를 잡아내 사전에 걸러냅니다.

즉, 과거에 배포를 망가뜨리던 desync를 `certbot renew` **이전에** 탐지해 손상 계보 보호 분기로 보냅니다.

### 전체 인증서 흐름

`bootstrap`(컨테이너 기동 전)과 `issue`(기동 후)의 분기는 다음과 같습니다.

```mermaid
flowchart TD
    subgraph BS["remote-tls.sh bootstrap — 컨테이너 기동 전"]
        B0["options-ssl-nginx.conf /<br/>ssl-dhparams.pem 없으면 내려받기"]
        B0 --> B1{"live 인증서도<br/>renewal conf도<br/>둘 다 없나?"}
        B1 -- "둘 다 없음" --> B2["generate_bootstrap_cert<br/>(self-signed)"]
        B1 -- "하나라도 있음" --> B3["손대지 않음"]
    end

    BS --> RO["remote-rollout.sh<br/>nginx -t → pull → up -d"]

    RO --> I0["검증된 빈 broken conf 제거<br/>접미사 lineage 정리"]
    I0 --> I1{"lineage_is_valid?"}

    subgraph IS["remote-tls.sh issue — 기동 후"]
        I1 -- "유효" --> R{"certbot renew 성공?"}
        R -- "성공" --> OK["계보 유지"]
        R -- "실패" --> F1["인증서 유지<br/>배포 실패 (exit 1)"]

        I1 -- "무효" --> C1{"renewal conf가 있나?"}
        C1 -- "있음" --> F2["계보 손상 의심<br/>손대지 않고 실패 (exit 1)"]
        C1 -- "없음" --> C2{"live 인증서 발급자?"}

        C2 -- "Let's Encrypt" --> F3["갱신 메타데이터 없음<br/>손대지 않고 실패 (exit 1)"]
        C2 -- "self-signed" --> ISS1["issue_certificate<br/>(최초 발급)"]
        C2 -- "인증서 없음" --> ISS2["generate_bootstrap_cert<br/>→ issue_certificate"]

        ISS1 --> Q1{"성공?"}
        ISS2 --> Q1
        Q1 -- "성공" --> OK2["정식 인증서 발급"]
        Q1 -- "실패" --> F4["부트스트랩 유지<br/>배포 실패 (exit 1)"]
    end

    OK --> Reload["nginx -s reload"]
    OK2 --> Reload
    Reload --> HC["remote-healthcheck.sh"]
```

정리하면:

1. **계보 유효 → renew만**: `lineage_is_valid`가 통과하면 `certbot renew --webroot`만 돌립니다. 멀쩡한 인증서를 불필요하게 재발급해 Let's Encrypt rate limit을 소모하지 않으려는 선택입니다.
2. **최초 발급**: 인증서가 아예 없거나 부트스트랩 self-signed만 있으면 `issue_certificate`로 정식 발급을 시도합니다. 신규 배포가 이 경로를 탑니다.
3. **검증된 최초 발급 잔재만 복구**: Certbot이 `missing a required file reference`를 보고하고 정식 archive도 비어 있을 때만 깨진 conf를 지웁니다. 정식 이름을 가로막던 `<도메인>-NNNN` 계보도 함께 정리합니다.
4. **그 외 전부 실패**: 실제 계보가 깨졌거나, 메타데이터가 없거나, 발급이 실패하면 **아무것도 지우지 않고** `exit 1`로 배포를 중단합니다. 위 "설계 원칙" 참고.

`issue_certificate()`는 webroot 방식의 `certbot certonly`입니다.

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

`CERT_ALT_DOMAIN`은 `www.$CERT_DOMAIN`으로, 루트 도메인과 `www` 서브도메인을 한 인증서에 함께 담습니다.

### CloudFront가 있는데 왜 EC2에도 인증서가 필요한가

공개 진입점은 CloudFront(`imhere.ratiko.co.kr`)이고 거기엔 ACM 인증서가 붙어 있습니다. 그런데도 EC2에서 certbot을 계속 돌리는 이유는 **TLS 구간이 두 개**이기 때문입니다.

```
브라우저 ──TLS①──> CloudFront ──TLS②──> EC2 nginx ──> Spring
         imhere.ratiko.co.kr        ratiko.co.kr
         ACM (us-east-1)            Let's Encrypt (certbot)
```

ADR-025에 따라 CloudFront의 `/api/*` 비헤이비어는 오리진을 `ratiko.co.kr`(EC2 nginx)로 두고 **HTTPS로만** 연결합니다. CloudFront는 커스텀 오리진의 인증서를 공인 CA 기준으로 검증하므로, EC2에는 공개적으로 신뢰받는 인증서가 있어야 합니다. ACM 퍼블릭 인증서는 export가 안 되어 EC2에 설치할 수 없습니다.

TLS②를 없애려면 오리진 프로토콜을 HTTP로 낮춰야 하는데, `Authorization` 헤더가 공용 인터넷을 평문으로 지나게 되므로 선택지가 아닙니다.

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

### 2. Private config repo(`ImHereOfRati/config`)의 `env/*.env`

`sync-config` Job이 이 repo를 clone해 `env/` 아래 `.env` 파일을 전부 가져오고, `deploy-app` Job이 그것들을 모두 `source`해 `nginx.conf.template` / `alloy-config.alloy.template`를 렌더링한 뒤 앱 EC2로 전송합니다. **이 repo는 ImHere Server와 분리된 별도 저장소**이므로, 값을 바꿀 때는 그 repo를 수정해야 합니다.

키 목록과 "새 변수를 추가할 때" 절차는 private config repo의 `env/` 구조와 `sync-config.sh`를 따릅니다.

#### 왜 한 파일이 아닌가

예전에는 `prod.env` 한 파일이었고 `dsko`·`nginx`·`alloy` 세 컨테이너가 **그것을 통째로** 읽었습니다. 그래서 alloy 컨테이너 안에서도 `DB_PASSWORD`가 보였습니다. 지금은 관심사별로 쪼개고 컨테이너마다 필요한 파일만 넣습니다. 최소권한이 설정 규율이 아니라 파일 구조로 보장됩니다.

| 파일 | 변수 | 읽는 컨테이너 |
|---|---|---|
| `env/app.env` | `SPRING_PROFILES_ACTIVE`, `DB_HOST`/`_PORT`/`_NAME`/`_USER`/`_PASSWORD`/`_POOL_SIZE`, `MGMT_BASE_PATH`, `LOG_FILE`, `JWT_SECRET`, `SECURITY_WHITELIST`, `ADMIN_ID`, `ADMIN_ALLOWED_IPS` | `dsko` |
| `env/web.env` | `SERVER_NAME`, `CERT_DOMAIN`, `NGINX_ALLOWED_ORIGIN`, `CORS_ALLOWED_ORIGINS` | `dsko`, `nginx` |
| `env/oidc.env` | `KAKAO_CLIENT_ID`, `GOOGLE_CLIENT_ID_WEB`/`_IOS`/`_ANDROID`, `APPLE_CLIENT_ID`, `APPLE_SERVICE_ID` | `dsko` |
| `env/external.env` | `FIREBASE_PATH`, `NAVER_MAP_CLIENT_ID`/`_SECRET`, `NAVER_SEARCH_CLIENT_ID`/`_SECRET`, `SOLAPI_SENDER`/`_API_KEY`/`_API_SECRET`, `DISCORD_WEBHOOK_ERROR_SERVER`/`_ERROR_CLIENT`/`_OTT` | `dsko` |
| `env/observability.env` | `GRAFANA_CLOUD_LOKI_*`, `GRAFANA_CLOUD_PROM_*`, `GRAFANA_CLOUD_TEMPO_*` | `alloy` |

몇 가지 짚을 점:

* `SERVER_NAME`/`CERT_DOMAIN`/`NGINX_ALLOWED_ORIGIN`/`MGMT_BASE_PATH`는 nginx·alloy 템플릿 렌더링에도 쓰입니다.
* `DB_*`는 가비아 MySQL 접속 정보입니다 — `gabia.md` 참고.
* `FIREBASE_PATH`는 컨테이너 내부 경로(`/app/secrets/imhereFirebaseKey.json`)이고, 키 파일 자체는 `imhereFirebaseKey.json`으로 별도 전송됩니다.
* `NAVER_*` 4개는 **기본값이 없습니다.** 비어 있으면 앱이 기동 단계에서 실패합니다. 조용히 빈 키로 떠서 `/api/maps/*`가 런타임에 죽는 쪽보다 낫다는 판단입니다.
* `oidc.env`의 키는 전부 옵셔널입니다. 빈 항목은 `OIDCProperties`가 걸러내므로 쓰지 않는 플랫폼은 비워 두면 됩니다. 다만 해당 플랫폼 로그인은 `aud` 불일치로 실패합니다.
* Grafana Cloud 자격증명은 Alloy 전용입니다. Loki=로그, Prometheus=메트릭, Tempo=트레이스.

### 3. CloudFormation에서 자동으로 도출되는 값 (직접 설정하지 않음)

다음 값은 어디에도 수동으로 적지 않습니다. `resolve-infra` Job이 `aws cloudformation describe-stacks`로 Output을 읽어 이후 Job에 환경 변수로 흘려줍니다.

| 값 | CloudFormation Output | 쓰이는 곳 |
|---|---|---|
| `EC2_HOST` | `ElasticIp` | SSH 접속, 배포 대상 |
| `EC2_SECURITY_GROUP_ID` | `SecurityGroupId` | 배포 중 SSH(22) 임시 허용/회수 |
| `ECR_REPOSITORY_NAME` / `ECR_REPOSITORY_URI` | `EcrRepositoryName` / `EcrRepositoryUri` | 이미지 push/pull 대상 |

이 값들을 바꾸려면 GitHub Secrets나 `env/*.env`가 아니라 **CloudFormation 스택 자체**(`aws.md`)를 업데이트해야 합니다.

ECR 로그인 비밀번호는 여기에 없습니다. `aws ecr get-login-password`의 출력을 SSH **stdin으로 파이프**해 `docker login --password-stdin`이 받습니다. 환경변수나 `$GITHUB_ENV`에 실으면 러너 환경 파일과 EC2의 `/proc/<pid>/environ` 양쪽에 평문으로 남기 때문입니다.

### 4. CloudFormation 스택 파라미터 (`--parameter-overrides`)

스택을 생성/업데이트할 때만 지정하는 값입니다. 현재 애플리케이션 배포에 필요한 주요 값은 `KeyName`, `AppInstanceType`, `EcrRepositoryName`, `CreateOidcProvider`입니다.

```bash
aws cloudformation deploy \
  --stack-name imhere-prod-infra \
  --template-file infra/cloudformation/main.yaml \
  --region ap-northeast-2 \
  --parameter-overrides KeyName=imhere-prod-key AppInstanceType=t3.small \
                        EcrRepositoryName=imhere/dsko CreateOidcProvider=true \
  --capabilities CAPABILITY_NAMED_IAM
```

`CreateOidcProvider`는 GitHub Actions용 IAM OIDC provider를 이 스택이 만들지 여부입니다. 한 계정에 같은 URL의 provider는 하나만 존재할 수 있으므로, 이미 등록된 계정에 배포할 때는 `false`로 둡니다. 기본값은 `true`입니다.

> **Windows에서 주의**: 템플릿에 한글 주석이 있어서 `file://`로 읽으면 AWS CLI가 디코딩에 실패합니다(`text contents could not be decoded`). `PYTHONUTF8=1` 환경변수와 `--template-body "$(cat ...)"` 형태로 우회하세요. Linux 러너에서는 발생하지 않습니다.

---

## 관련 문서

* AWS 인프라(VPC/EC2/SG/EIP/ECR/IAM)는 `aws.md`를 참고합니다.
* 가비아 DNS/MySQL은 `gabia.md`를 참고합니다.
* Docker 이미지/Compose/Nginx 구성은 `docker.md`를 참고합니다.
* 런타임 env 파일의 전체 키 목록과 추가 절차는 private config repo의 `env/` 구조와 `sync-config.sh`를 참고합니다.
