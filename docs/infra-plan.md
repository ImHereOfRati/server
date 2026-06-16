# 인프라 관리 방법 비교 및 권장안

## Context

현재: EC2 3대 수동 운영, IaC 없음
- EC2-1 (main): nginx + alloy + Spring Boot
- EC2-2 (db): MySQL
- EC2-3 (infra): Redis + RabbitMQ

목표: 인프라를 코드로 관리, 비용 절감, AI로 쉽게 변경

---

## 방법 전체 비교

| 방법 | 난이도 | 상태관리 | 새 툴 필요 | AI 활용 | 추천 대상 |
|------|--------|----------|------------|---------|-----------|
| **AWS CLI 쉘 스크립트** | ★☆☆ | 없음 (직접 구현) | AWS CLI만 | ★★★ | **초보, 단순 구성** |
| AWS CloudFormation | ★★☆ | AWS가 관리 | 없음 | ★★☆ | AWS 고착화 OK |
| AWS CDK | ★★★ | CFN 기반 | Node.js | ★★★ | TS/Java 익숙한 개발자 |
| Terraform | ★★★ | S3+DynamoDB | terraform CLI | ★★★ | 멀티클라우드, 표준 |
| Ansible | ★★☆ | 없음 | ansible | ★★☆ | EC2 설정 자동화만 |

---

## 권장안: AWS CLI 쉘 스크립트

### 이유
- 새 툴 설치 불필요 (AWS CLI는 이미 있음)
- 각 명령어가 뭘 하는지 투명하게 보임 → 학습에 최적
- Claude가 전부 작성 → 사용자는 복붙 후 실행만
- 상태 없음 = 복잡성 없음

### 구조
```
scripts/
├─ provision.sh      # 인프라 생성 (EC2, ECR, IAM, 보안그룹)
├─ check.sh          # 현재 상태 확인
├─ teardown.sh       # 인프라 삭제 (주의)
└─ README.md         # 각 스크립트 설명
```

### provision.sh 패턴 (멱등성: 이미 있으면 스킵)
```bash
#!/bin/bash

# 보안 그룹 생성
SG_ID=$(aws ec2 describe-security-groups \
  --filters "Name=group-name,Values=imhere-sg" \
  --query "SecurityGroups[0].GroupId" \
  --output text 2>/dev/null)

if [ "$SG_ID" = "None" ] || [ -z "$SG_ID" ]; then
  SG_ID=$(aws ec2 create-security-group \
    --group-name imhere-sg \
    --description "ImHere app security group" \
    --query "GroupId" --output text)
  echo "보안그룹 생성: $SG_ID"
else
  echo "보안그룹 이미 존재: $SG_ID (스킵)"
fi

# EC2 생성
INSTANCE_ID=$(aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=imhere-main" "Name=instance-state-name,Values=running,stopped" \
  --query "Reservations[0].Instances[0].InstanceId" \
  --output text 2>/dev/null)

if [ "$INSTANCE_ID" = "None" ] || [ -z "$INSTANCE_ID" ]; then
  INSTANCE_ID=$(aws ec2 run-instances \
    --image-id ami-xxxxxxxxx \
    --instance-type t3.medium \
    --security-group-ids $SG_ID \
    --user-data file://scripts/user-data.sh \
    --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=imhere-main}]' \
    --query "Instances[0].InstanceId" --output text)
  echo "EC2 생성: $INSTANCE_ID"
else
  echo "EC2 이미 존재: $INSTANCE_ID (스킵)"
fi
```

### user-data.sh (EC2 첫 부팅 시 Docker 자동 설치)
```bash
#!/bin/bash
yum update -y
yum install -y docker
systemctl start docker
systemctl enable docker
usermod -aG docker ec2-user

curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64" \
  -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
```

---

## 차선: CloudFormation (상태 관리 원하면)

```yaml
# infra/cloudformation.yaml
AWSTemplateFormatVersion: '2010-09-09'
Resources:
  ImHereSecurityGroup:
    Type: AWS::EC2::SecurityGroup
    Properties:
      GroupDescription: ImHere app
      SecurityGroupIngress:
        - IpProtocol: tcp
          FromPort: 80
          ToPort: 80
          CidrIp: 0.0.0.0/0
        - IpProtocol: tcp
          FromPort: 443
          ToPort: 443
          CidrIp: 0.0.0.0/0

  ImHereEC2:
    Type: AWS::EC2::Instance
    Properties:
      InstanceType: t3.medium
      ImageId: ami-xxxxxxxxx
      SecurityGroups:
        - !Ref ImHereSecurityGroup
      UserData:
        Fn::Base64: |
          #!/bin/bash
          yum update -y
          yum install -y docker
          systemctl start docker
          systemctl enable docker
```

```bash
# 배포 (업데이트도 같은 명령어)
aws cloudformation deploy \
  --template-file infra/cloudformation.yaml \
  --stack-name imhere-prod \
  --capabilities CAPABILITY_IAM
```

---

## AI 활용 워크플로우

### 인프라 변경 시
```
1. Claude에게 자연어로 요청
   "포트 8080 보안그룹에 추가해줘"
   "EC2 타입을 t3.small로 바꿔줘"
2. Claude가 스크립트/yaml 수정
3. 사용자: bash scripts/provision.sh (또는 aws cloudformation deploy)
```

### 앱 배포 (이미 자동화, 변경 불필요)
```
코드 push → GitHub Actions → ECR → EC2 → 완료
```

---

## EC2 1대 통합 계획 (비용 절감)

### 3대 → 1대 t3.medium 통합

**메모리 예상:**
- Spring Boot: ~300-500MB
- MySQL: ~300-500MB
- Redis: ~50-100MB
- RabbitMQ: ~100-200MB
- Nginx + Alloy: ~50MB
- 합계: ~800MB-1.3GB → t3.medium(4GB) 여유 있음

**docker-compose.prod.yml에 추가:**
```yaml
  mysql:
    image: mysql:8
    volumes:
      - mysql-data:/var/lib/mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: imhere

  redis:
    image: redis:7-alpine
    volumes:
      - redis-data:/data

  rabbitmq:
    image: rabbitmq:3-management
    volumes:
      - rabbitmq-data:/var/lib/rabbitmq

volumes:
  mysql-data:
  redis-data:
  rabbitmq-data:
```

### 구현 순서
1. 현재 EC2 메모리 사용량 확인 (`free -h`, `docker stats`)
2. scripts/provision.sh 작성 (Claude 활용)
3. EC2 1대 새로 생성 (provision.sh 실행)
4. docker-compose.prod.yml에 MySQL/Redis/RabbitMQ 추가
5. 데이터 마이그레이션 (MySQL dump → restore)
6. GitHub Actions `EC2_HOST` 시크릿 업데이트
7. 기존 EC2 3대 종료

---

## 비용 요약

| 구성 | 월 비용 |
|------|---------|
| 현재 (EC2 3대 t3.micro 기준) | ~$25/월 |
| EC2 1대 t3.medium | ~$30/월 |
| EC2 1대 t3.small | ~$15/월 (메모리 확인 후) |

> t3.small 먼저 시도, OOM 발생 시 t3.medium으로 업그레이드
> t3.small은 `aws ec2 modify-instance-attribute`로 무중단 타입 변경 가능 (중지 후)
