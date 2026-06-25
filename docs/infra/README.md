# Infra 문서

ImHere 서버를 **어떤 인프라 위에, 왜 그렇게 구성했는지** 정리한 문서 모음입니다.

---

## 핵심 설계 판단

| 결정 | 내용 | 근거 |
|---|---|---|
| 퍼블릭 서브넷 단일 구성 | Private Subnet / NAT Gateway 없이 모든 리소스를 퍼블릭 서브넷에 배치 | [aws.md](aws.md#선택-이유) |
| App EC2와 RabbitMQ EC2 분리 | `dsko` + `nginx` + `alloy`는 앱 EC2, RabbitMQ는 별도 EC2 | [aws.md](aws.md#선택-이유) |
| Elastic IP 고정 | 앱 EC2 교체 시에도 DNS를 다시 바꾸지 않도록 EIP 사용 | [aws.md](aws.md#선택-이유) |
| GitHub OIDC 배포 권한 | 장기 Access Key 없이 GitHub Actions가 IAM Role Assume | [aws.md](aws.md#iam-github-actions-oidc) |
| DB는 가비아 MySQL 사용 | 현재 규모와 운영 비용 기준에서 AWS RDS보다 단순 | [gabia.md](gabia.md#선택-이유) |
| TLS는 Certbot 사용 | DNS 관리와 인증서 발급 주체를 분리 | [gabia.md](gabia.md#선택-이유) |

---

## 문서 지도

| 순서 | 문서 | 범위 |
|---|---|---|
| 1 | [aws.md](aws.md) | VPC, EC2, Security Group, EIP, ECR, IAM |
| 2 | [gabia.md](gabia.md) | 도메인 DNS, 외부 MySQL |
| 3 | [docker.md](docker.md) | 이미지, Compose profile |
| 4 | [nginx.md](nginx.md) | TLS 종료, reverse proxy, CORS |
| 5 | [cicd.md](cicd.md) | CI/CD, 환경 변수 주입, 배포 단계 |
| 6 | [db-schema.md](db-schema.md) | ERD, DDL |

상위 구조는 [architecture 문서](../architecture/README.md), 비즈니스 규칙은 [domain.md](../architecture/domain.md), 인증/권한은 [security 문서](../security/README.md)를 참고합니다.

---

## 자주 참조하는 항목

* 환경 변수와 주입 위치: [cicd.md](cicd.md)
* 로컬/운영 Compose 차이: [docker.md](docker.md)
* CloudFormation Output과 배포 연결: [aws.md](aws.md#outputs)
