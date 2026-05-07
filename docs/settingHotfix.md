# Setting Hotfix — GitHub Secrets 점검 (2026-05-07)

GitHub 리포지토리 Settings → Secrets and variables → Actions 화면과 `cd.yml`을 대조한 결과를 정리한 핫픽스 문서입니다. 정상 배포를 위해 아래 항목을 즉시 갱신해야 합니다.

---

## 1. 누락 — 즉시 추가 필요

현재 등록되어 있지 않으나 `cd.yml`이 참조하는 시크릿입니다. 미등록 상태에서 배포가 트리거되면 해당 단계에서 실패하거나 빈 파일이 생성됩니다.

| Secret 이름              | 사용 위치                                              | 영향                                                          |
|------------------------|----------------------------------------------------|-------------------------------------------------------------|
| `AWS_DEPLOY_ROLE_ARN`  | `cd.yml` `Configure AWS credentials (OIDC)` 단계   | 미등록 시 OIDC role assume이 실패하여 ECR push / EC2 보안 그룹 제어가 모두 차단됨 |
| `APP_PROD_YAML`        | `cd.yml` `Write secret config files...` 단계        | 미등록 시 `application-prod.yaml`이 빈 파일로 생성되어 운영 프로파일이 누락됨        |

### 추가 절차

1. GitHub 리포지토리 → Settings → Secrets and variables → Actions → **New repository secret**.
2. 위 표의 이름과 값을 등록.
   - `AWS_DEPLOY_ROLE_ARN` 형식 예: `arn:aws:iam::123456789012:role/imhere-github-actions` (Role 자체는 §2에서 먼저 생성)
   - `APP_PROD_YAML` 값: 운영용 `application-prod.yaml` 본문 전체.

---

## 2. AWS OIDC 사전 셋업 (AWS_DEPLOY_ROLE_ARN을 만들기 위한 단계)

`AWS_DEPLOY_ROLE_ARN`은 AWS IAM에 미리 만들어 둔 Role의 ARN입니다. AWS 측에 OIDC Identity Provider와 Role이 존재해야 GitHub Actions에서 assume할 수 있습니다. 한 번만 셋업하면 이후엔 갱신이 필요 없습니다.

### 2.1 IAM에 GitHub OIDC Identity Provider 등록

GitHub Actions가 발급하는 OIDC 토큰을 AWS가 신뢰하도록 등록합니다.

- **Provider URL**: `https://token.actions.githubusercontent.com`
- **Audience**: `sts.amazonaws.com`

**Console 방식** — IAM → Identity providers → **Add provider** → OpenID Connect → 위 값 입력 → **Add provider**.
(2023년 이후 AWS가 thumbprint를 자동으로 처리하므로 수동 입력 불필요)

**CLI 방식**

```bash
aws iam create-open-id-connect-provider \
  --url https://token.actions.githubusercontent.com \
  --client-id-list sts.amazonaws.com
```

> 동일 계정에서 이미 GitHub OIDC provider를 등록한 적이 있다면 이 단계는 생략. `aws iam list-open-id-connect-providers`로 확인.

### 2.2 Trust Policy 작성 → IAM Role 생성

GitHub Actions가 이 Role을 assume할 수 있도록 신뢰 관계를 정의합니다. `<ACCOUNT_ID>`, `<ORG>`, `<REPO>`는 실제 값으로 치환하세요.

`trust-policy.json`:

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

> `sub` 조건이 보안 핵심입니다. `repo:<ORG>/<REPO>:*`처럼 와일드카드를 두면 fork PR에서도 assume이 가능해지므로, 반드시 `ref:refs/heads/main`처럼 브랜치까지 고정합니다.

```bash
aws iam create-role \
  --role-name imhere-github-actions \
  --assume-role-policy-document file://trust-policy.json
```

### 2.3 권한 정책 부여

`cd.yml`이 수행하는 두 가지 작업에 필요한 권한입니다.

| 용도          | 정책                                                                       |
|-------------|--------------------------------------------------------------------------|
| ECR 이미지 push | AWS 관리형 `AmazonEC2ContainerRegistryPowerUser` attach                     |
| EC2 보안 그룹 제어 | 인라인 정책으로 `ec2:AuthorizeSecurityGroupIngress`, `ec2:RevokeSecurityGroupIngress` |

```bash
# ECR
aws iam attach-role-policy \
  --role-name imhere-github-actions \
  --policy-arn arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryPowerUser

# EC2 SG ingress 제어 (Runner IP를 22번 포트에 한시적으로 추가/제거)
cat > sg-ingress-policy.json <<'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ec2:AuthorizeSecurityGroupIngress",
        "ec2:RevokeSecurityGroupIngress"
      ],
      "Resource": "arn:aws:ec2:<REGION>:<ACCOUNT_ID>:security-group/<EC2_SECURITY_GROUP_ID>"
    }
  ]
}
EOF

aws iam put-role-policy \
  --role-name imhere-github-actions \
  --policy-name imhere-sg-ingress \
  --policy-document file://sg-ingress-policy.json
```

> EC2 권한은 `Resource: "*"`로도 동작하지만, 최소 권한 원칙상 `EC2_SECURITY_GROUP_ID`로 한정하는 것이 안전합니다.

### 2.4 Role ARN 확인 → GitHub Secret 등록

```bash
aws iam get-role --role-name imhere-github-actions --query 'Role.Arn' --output text
# arn:aws:iam::123456789012:role/imhere-github-actions
```

출력값을 그대로 GitHub `AWS_DEPLOY_ROLE_ARN`에 등록 (§1 절차).

### 2.5 검증

main 브랜치에 빈 커밋(`git commit --allow-empty -m "verify oidc"`)을 push하여 `cd.yml`이 트리거되는지 확인합니다. `Configure AWS credentials (OIDC)` 단계 로그에 `Authenticated as arn:aws:sts::...:assumed-role/imhere-github-actions/...`이 보이면 성공입니다. 실패하면 `AccessDenied`의 메시지로 원인을 추적 — 대부분 Trust Policy의 `sub` 조건 불일치가 원인입니다.

---

## 3. 제거 권장 — 더 이상 사용되지 않음

커밋 `7d4b316` ("AWS 인증을 GitHub OIDC + IAM role assume으로 전환") 이후 `cd.yml`은 정적 access key를 더 이상 사용하지 않습니다. 보관 자체가 OIDC 도입의 보안 효과를 무력화하므로 제거를 권장합니다.

| Secret 이름               | 제거 사유                                            |
|-------------------------|--------------------------------------------------|
| `AWS_ACCESS_KEY_ID`     | OIDC 전환 이후 워크플로 어디에서도 참조되지 않음                    |
| `AWS_SECRET_ACCESS_KEY` | 동일 사유. 정적 키를 보관하지 않는 것이 OIDC 도입의 핵심 보안 효과         |

### 제거 절차

1. **선결 조건**: 신규 OIDC 경로로 main 브랜치 배포가 한 번 이상 성공했는지 확인. 워크플로 로그의 `Configure AWS credentials (OIDC)` 단계가 통과했다면 정적 키는 이미 사용되지 않는 상태입니다.
2. GitHub 리포지토리 → Settings → Secrets and variables → Actions → 해당 시크릿의 휴지통(🗑) 클릭.
3. (선택) AWS IAM 콘솔에서 GitHub Actions 전용 IAM 사용자가 있었다면 access key 비활성화/삭제 후 사용자 자체도 정리.

---

## 4. 점검 후 상태 (목표)

`cd.yml`의 `${{ secrets.* }}` 참조와 1:1로 일치하는 최종 시크릿 목록입니다. 이 목록 외의 정적 AWS 키가 남아 있으면 안 됩니다.

```
APP_DATASOURCE_YAML
APP_MONITORING_YAML
APP_PROD_YAML            # ← 신규 추가
APP_SECRET_YAML
AWS_DEPLOY_ROLE_ARN      # ← 신규 추가
AWS_ECR_REGISTRY
AWS_ECR_REPOSITORY
AWS_REGION
EC2_DEPLOY_PATH
EC2_HOST
EC2_SECURITY_GROUP_ID
EC2_SSH_PRIVATE_KEY
EC2_USER
FIREBASE_JSON_KEY
```

각 시크릿의 용도와 값 형식은 [CI/CD 가이드 §3](cicd.md)을 참고하세요.