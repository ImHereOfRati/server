# Amazon Web Service (with CloudFormation)

## 선택 이유

* 인프라는 코드로 관리하기 위해 CloudFormation을 사용하였습니다. 동일한 환경을 반복해서 생성할 수 있으며, 변경 이력을 Git으로 관리할 수 있습니다.
* 서비스 규모와 운영 비용을 고려하여 모든 리소스를 퍼블릭 서브넷에 구성하였습니다. 추후 보안이나 규모 요구사항이 증가하면 Private Subnet, NAT Gateway 등의 구성을 추가할 예정입니다.
* 애플리케이션과 RabbitMQ를 각각 별도의 EC2에서 운영하여 장애 영향을 최소화하고 독립적으로 관리할 수 있도록 하였습니다.
* 애플리케이션 서버에는 Elastic IP를 할당하여 인스턴스를 교체하더라도 동일한 IP와 DNS를 유지할 수 있도록 하였습니다.
* 컨테이너 이미지는 ECR에서 관리하여 배포 환경을 일관되게 유지하고, GitHub Actions와 OIDC를 연동하여 장기 Access Key 없이 안전하게 배포할 수 있도록 하였습니다.

---

## AWS 인프라 구성도

```mermaid
flowchart TD
    GH["GitHub Actions<br/>CD Workflow"]

    subgraph AWS["AWS Account (ap-northeast-2)"]
        Role["GitHubActionsDeployRole<br/>(OIDC AssumeRole)"]
        ECR["AppEcrRepository<br/>imhere/dsko"]

        subgraph VPC["AppVpc 10.50.0.0/16"]
            IGW["InternetGateway"]

            subgraph Subnet["PublicSubnet 10.50.1.0/24"]
                EIP["AppElasticIp"]

                subgraph AppSG["AppSecurityGroup<br/>in: 443/tcp (0.0.0.0/0)"]
                    App["AppInstance (t3.small)<br/>dsko + nginx + alloy"]
                end

                subgraph RabbitSG["RabbitMqSecurityGroup<br/>in: 5672/15672 (from AppSG)"]
                    Rabbit["RabbitMqInstance (t3.micro)<br/>rabbitmq"]
                end
            end
        end
    end

    Internet(("Internet")) -- " 443/tcp " --> AppSG
    EIP -. 고정 IP . - App 
    IGW --- Subnet
App -- " 5672/15672 " --> RabbitSG
GH -- " AssumeRoleWithWebIdentity " --> Role
Role -. " 배포 시 SSH(22) 임시 허용/회수 " .-> AppSG
Role -- " push " --> ECR
App -- " pull " --> ECR
```

---

## 네트워크

* VPC `10.50.0.0/16`(`AppVpc`), DNS 지원/호스트네임 활성화.
* Public Subnet `10.50.1.0/24`(`PublicSubnet`), `MapPublicIpOnLaunch: true`.
* IGW(`InternetGateway`) + 기본 라우트(`0.0.0.0/0` → IGW)로 퍼블릭 인터넷 연결.

---

## 보안 그룹

### AppSecurityGroup

* 인바운드 `443/tcp`만 `0.0.0.0/0`에 허용합니다.
* 아웃바운드는 전체 허용합니다.
* SSH(22)는 기본적으로 차단하며, GitHub Actions의 `prepare-server` 단계에서 러너 IP만 임시 허용한 뒤 `cleanup` 단계에서 제거합니다.

### RabbitMqSecurityGroup

* `5672`, `15672` 포트는 `AppSecurityGroup`에서만 접근할 수 있도록 설정합니다.
* 애플리케이션 EC2를 제외한 외부에서는 RabbitMQ에 직접 접근할 수 없습니다.

---

## EC2

| 리소스                | 타입 파라미터                | 기본값        | 역할                          |
|--------------------|------------------------|------------|-----------------------------|
| `AppInstance`      | `AppInstanceType`      | `t3.small` | `dsko`, `nginx`, `alloy` 실행 |
| `RabbitMqInstance` | `RabbitMqInstanceType` | `t3.micro` | RabbitMQ 전용 서버              |

* AMI는 SSM Parameter(`al2023-ami-kernel-default-x86_64`)를 사용하여 최신 Amazon Linux 2023 이미지를 조회합니다.
* App EC2는 Docker, Docker Compose Plugin, Certbot만 설치하며 실제 애플리케이션 배포는 CD가 수행합니다.
* RabbitMQ EC2는 UserData에서 컨테이너를 직접 실행하며 `rabbitmq_data` 볼륨을 통해 데이터를 유지합니다.
* 기본 인스턴스 타입은 운영 비용과 Free Tier 제한을 고려하여 `t3.small`, `t3.micro`를 사용합니다.

---

## 퍼블릭 IP

* `AppElasticIp`를 사용하여 애플리케이션 EC2에 고정 IP를 할당합니다.
* RabbitMQ EC2는 퍼블릭 IP를 사용하지 않으며 애플리케이션 서버와의 내부 통신만 수행합니다.

---

## ECR

* Repository: `AppEcrRepository`
* 기본 이름: `imhere/dsko`
* `ScanOnPush: true`
* `AES256` 암호화
* Lifecycle Policy를 통해 최신 30개의 이미지만 유지합니다.

---

## IAM (GitHub Actions OIDC)

* `GitHubActionsDeployRole`을 통해 GitHub Actions가 OIDC 방식으로 IAM Role을 Assume합니다.
* 장기 Access Key를 저장하지 않습니다.
* 지정된 Repository와 Main 브랜치에서만 Assume할 수 있도록 제한합니다.
* CloudFormation 조회, Security Group 제어, ECR Push에 필요한 최소 권한만 부여합니다.

---

## 파라미터

| 파라미터                                                  | 기본값                                  | 비고                             |
|-------------------------------------------------------|--------------------------------------|--------------------------------|
| `KeyName`                                             | (필수)                                 | EC2 Key Pair                   |
| `AppInstanceType`                                     | `t3.small`                           |                                |
| `RabbitMqInstanceType`                                | `t3.micro`                           |                                |
| `VpcCidr`                                             | `10.50.0.0/16`                       |                                |
| `PublicSubnetCidr`                                    | `10.50.1.0/24`                       |                                |
| `RepositorySlug`                                      | `ImHereOfRati/server`                | OIDC 검증                        |
| `MainBranchRef`                                       | `refs/heads/main`                    | OIDC 검증                        |
| `EcrRepositoryName`                                   | `imhere/dsko`                        |                                |
| `RabbitMqUser` / `RabbitMqPassword` / `RabbitMqVHost` | `imhere` / `imhere-rabbit` / `/main` | 운영 환경에서는 배포 시 Override를 권장합니다. |

---

## Outputs

| Output                    | 용도                                                                   |
|---------------------------|----------------------------------------------------------------------|
| `ElasticIp`               | 앱 EC2 고정 IP                                                          |
| `Ec2InstanceId`           | 앱 EC2 인스턴스 ID                                                        |
| `RabbitMqInstanceId`      | RabbitMQ EC2 인스턴스 ID                                                 |
| `RabbitMqPrivateIp`       | 앱이 RabbitMQ에 접속할 내부 IP — CD가 `prod.env`에 `RABBITMQ_HOST`로 주입         |
| `SecurityGroupId`         | 앱 보안 그룹 ID — CD가 배포 시 SSH(22) 임시 허용/회수에 사용                           |
| `RabbitMqSecurityGroupId` | RabbitMQ 보안 그룹 ID                                                    |
| `GitHubActionsRoleArn`    | GitHub Actions가 Assume하는 Role ARN — `AWS_DEPLOY_ROLE_ARN` Secret에 등록 |
| `EcrRepositoryName`       | ECR Repository 이름                                                    |
| `EcrRepositoryUri`        | ECR Repository URI — 이미지 Push/Pull 대상                                |

CD의 `resolve-infra` 단계에서 CloudFormation Output을 조회하여 이후 배포 단계에서 사용합니다. 환경 변수로 어떻게
전달되는지는 [cicd.md의 3번 그룹](cicd.md#3-cloudformation에서-자동으로-도출되는-값-직접-설정하지-않음)을 참고합니다.

---

# 사용 명령어

### 배포 / 삭제

```bash
# 배포 (최초 생성 또는 업데이트)
aws cloudformation deploy \
  --stack-name imhere-prod-infra \
  --template-file infra/cloudformation/main.yaml \
  --region ap-northeast-2 \
  --parameter-overrides \
    KeyName=imhere-prod-key \
    AppInstanceType=t3.small \
    RabbitMqInstanceType=t3.micro \
    EcrRepositoryName=imhere/dsko \
  --capabilities CAPABILITY_NAMED_IAM
```

### 이벤트 확인

```bash
aws cloudformation describe-stack-events \
  --stack-name imhere-prod-infra \
  --region ap-northeast-2
```

### 삭제

```bash
aws cloudformation delete-stack \
  --stack-name imhere-prod-infra \
  --region ap-northeast-2
```

### 삭제 대기

```bash
aws cloudformation wait stack-delete-complete \
  --stack-name imhere-prod-infra \
  --region ap-northeast-2
```
