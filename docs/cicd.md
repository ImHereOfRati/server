# CI/CD 가이드

ImHereServer의 지속적 통합(CI) 및 지속적 배포(CD) 파이프라인, 자동화 설정, 보안 구성을 정리한 문서입니다.

## 1. 개요

GitHub Actions를 통해 모든 개발 프로세스를 자동화합니다.
빌드 이미지에는 민감한 정보를 포함하지 않으며, 실행 시점에 EC2 파일시스템의 설정을 볼륨 마운트하여 주입하는 방식을 채택하고 있습니다.

## 2. CI/CD 파이프라인 구조

GitHub Actions의 두 가지 워크플로로 운영됩니다.

| 워크플로                       | 트리거                 | 역할                              |
|----------------------------|---------------------|---------------------------------|
| `.github/workflows/ci.yml` | 모든 push, main 대상 PR | 테스트 실행, JaCoCo 리포트 생성 및 업로드     |
| `.github/workflows/cd.yml` | CI 성공 후 (main 브랜치)  | JAR 빌드, ECR 이미지 push, EC2 원격 배포 |

### 2.1 CI 흐름

1. **환경 구성**: JDK 21 설정 및 Gradle 의존성 캐시 복원.
2. **테스트 실행**: `./gradlew test` 수행.
    - `TESTCONTAINERS_RYUK_DISABLED=true` 설정을 통해 호스트 Docker 환경 최적화.
3. **결과 리포트**: 테스트 결과 및 JaCoCo 커버리지 리포트를 Artifact로 업로드.

### 2.2 CD 흐름

1. **JAR 빌드**: 민감한 설정 파일 없이 `bootJar` 생성.
2. **AWS 인증**: GitHub OIDC를 통해 IAM Role을 일시적으로 assume (4장 참고).
3. **이미지 빌드 & Push**: `Dockerfile.release`를 사용하여 ECR 리포지토리에 저장.
    - 태그: `<repo>:<DATE-SHA>` 및 `<repo>:latest`
4. **보안 그룹 제어**: 배포를 위해 GitHub Runner의 공인 IP를 EC2 보안 그룹(22번 포트)에 한시적으로 허용.
5. **설정 파일 전달**: GitHub Secrets의 YAML 내용과 Firebase 키를 EC2 `${EC2_DEPLOY_PATH}/config`, `${EC2_DEPLOY_PATH}/secrets` 경로로 전송.
6. **컨테이너 갱신**: EC2에서 `docker compose pull` 및 `up -d` 실행.
7. **사후 정리**: Runner IP 차단 및 임시 파일 삭제.

### 2.3 CD 파이프라인이 전송하지 않는 파일 (최초 1회 수동 배치)

CD는 매 배포마다 변경되는 파일(애플리케이션 YAML, Firebase 키, 이미지)만 갱신합니다. 다음 파일들은 EC2의 `${EC2_DEPLOY_PATH}`에 사전에 존재해야 하며, 이후 변경 시 운영자가 직접 갱신합니다.

| 파일 | 용도 |
|---|---|
| `docker-compose.prod.yml` | App / Nginx / Alloy 컨테이너 정의 |
| `alloy-config.alloy` | Grafana Alloy(로그·메트릭) 수집 설정 |
| `nginx/nginx.conf` | Nginx 리버스 프록시 / TLS / CORS 설정 (디렉터리 구조 유지) |
| `.env` | 컨테이너에 주입되는 환경변수 (시크릿 포함) |

자세한 초기 배포 절차는 [운영 가이드 §1](operations.md)을 참고하세요.

---

## 3. GitHub Secrets 관리

배포에 필요한 모든 시크릿 정보는 GitHub 리포지토리의 Actions Secrets에서 관리합니다.

| Secret 이름               | 용도                                     |
|-------------------------|----------------------------------------|
| `AWS_REGION`            | AWS 리전 (예: ap-northeast-2)             |
| `AWS_DEPLOY_ROLE_ARN`   | OIDC로 Assume할 IAM Role의 ARN            |
| `AWS_ECR_REGISTRY`      | AWS 계정 ID가 포함된 ECR 호스트 주소              |
| `AWS_ECR_REPOSITORY`    | ECR 리포지토리 이름 (`imhere/dsko`)           |
| `EC2_HOST`              | 배포 대상 App Server의 공인 IP 또는 도메인         |
| `EC2_USER`              | SSH 접속 계정 (예: ec2-user, ubuntu)        |
| `EC2_SSH_PRIVATE_KEY`   | EC2 접속용 프라이빗 키 (PEM)                   |
| `EC2_DEPLOY_PATH`       | EC2 내 배포 작업 디렉터리 경로                    |
| `EC2_SECURITY_GROUP_ID` | SSH 포트 제어를 위한 보안 그룹 ID                 |
| `APP_DATASOURCE_YAML`   | `application-datasource.yaml` 파일 내용 전문 |
| `APP_MONITORING_YAML`   | `application-monitoring.yaml` 파일 내용 전문 |
| `APP_SECRET_YAML`       | `application-secret.yaml` 파일 내용 전문     |
| `APP_PROD_YAML`         | `application-prod.yaml` 파일 내용 전문       |
| `FIREBASE_JSON_KEY`     | Firebase Admin SDK 서비스 계정 키 (JSON)     |

### 3.1 값 예시

실제 값은 절대 커밋하지 않습니다. 아래는 형식을 보이기 위한 더미 예시입니다.

**단일 값 (인프라 / 자격증명)**

```text
AWS_REGION              = ap-northeast-2
AWS_DEPLOY_ROLE_ARN     = arn:aws:iam::123456789012:role/imhere-github-actions
AWS_ECR_REGISTRY        = 123456789012.dkr.ecr.ap-northeast-2.amazonaws.com
AWS_ECR_REPOSITORY      = imhere/dsko
EC2_HOST                = 13.124.xx.xx          # 또는 app.fortuneki.site
EC2_USER                = ec2-user
EC2_DEPLOY_PATH         = /home/ec2-user/imhere
EC2_SECURITY_GROUP_ID   = sg-0abc1234def567890
```

**멀티라인 값 (PEM / YAML / JSON)**

`EC2_SSH_PRIVATE_KEY` — PEM 헤더/푸터를 포함한 전체 내용을 그대로 붙여넣습니다.

```text
-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAACFw...
... (중략) ...
-----END OPENSSH PRIVATE KEY-----
```

`APP_*_YAML` — 해당 YAML 파일의 전체 본문을 그대로 붙여넣습니다 (예: `APP_MONITORING_YAML`).

```yaml
management:
  server:
    port: 4861
  endpoints:
    web:
      base-path: /<운영용-난수-base-path>
      exposure:
        include: "prometheus,health,info"
  endpoint:
    health:
      show-details: always
    prometheus:
      enabled: true
```

`FIREBASE_JSON_KEY` — Firebase 콘솔에서 다운받은 서비스 계정 키 JSON을 그대로 붙여넣습니다.

```json
{
  "type": "service_account",
  "project_id": "imhere-xxxxx",
  "private_key_id": "abcdef0123456789",
  "private_key": "-----BEGIN PRIVATE KEY-----\nMIIEv...\n-----END PRIVATE KEY-----\n",
  "client_email": "firebase-adminsdk-xxxxx@imhere-xxxxx.iam.gserviceaccount.com",
  "client_id": "1234567890",
  "token_uri": "https://oauth2.googleapis.com/token"
}
```

> **주의**: 멀티라인 시크릿은 GitHub Secrets UI에 줄바꿈을 보존한 상태로 그대로 붙여넣어야 합니다. `cd.yml`은 heredoc(`cat << 'EOF'`)으로 파일에 기록하므로 별도 escape는 불필요합니다.

---

## 4. AWS OIDC 및 IAM 설정

보안 강화를 위해 정적 Access Key 대신 **GitHub OIDC (OpenID Connect)**를 사용합니다.

### 4.1 OIDC 인증의 장점

- **보안성**: GitHub Secrets에 영구적인 AWS 키를 보관하지 않습니다.
- **자동 관리**: 키 만료나 수동 갱신 작업이 필요 없으며, 신뢰 관계(Trust Relationship) 기반으로 동작합니다.

### 4.2 IAM Role 구성 (Trust Policy)

GitHub Actions가 특정 리포지토리 및 브랜치에서만 권한을 획득할 수 있도록 제한합니다.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::<ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:<ORG>/<REPO>:ref:refs/heads/main"
        }
      }
    }
  ]
}
```

### 4.3 부여된 권한 (Policy)

- **ECR**: 이미지 푸시 권한 (`AmazonEC2ContainerRegistryPowerUser`)
- **EC2**: 보안 그룹 인바운드 규칙 수정을 위한 권한 (`ec2:AuthorizeSecurityGroupIngress`, `ec2:RevokeSecurityGroupIngress`)

---

## 5. 트러블슈팅 (CI/CD 관련)

| 증상                          | 확인 사항                                                     |
|-----------------------------|-----------------------------------------------------------|
| ECR Push 실패                 | `AWS_DEPLOY_ROLE_ARN` 값 확인 및 IAM Role의 ECR 권한 점검          |
| OIDC 인증 실패 (`AccessDenied`) | Trust Policy의 `sub` 조건(리포지토리명, 브랜치명)이 일치하는지 확인            |
| SSH 접속 타임아웃                 | `EC2_SECURITY_GROUP_ID`가 올바른지, Runner IP가 정상적으로 추가되었는지 확인 |
| 설정 파일 누락                    | GitHub Secrets 명칭과 `cd.yml`의 환경변수 매핑 확인                   |
