# 외부 인프라 구성 문서

해당 문서에서는 `ImHere` 프로젝트의 핵심 물리 인프라 및 네트워크 구성을 다루고 있습니다.

## 1. AWS (Amazon Web Services)

### 1.1 EC2 (Elastic Compute Cloud)

서비스의 가용성과 독립성을 위해 인프라를 총 3대의 EC2 인스턴스로 분리하여 운영합니다.

| 역할                    | EC2 인스턴스 이름                   | 인스턴스 유형  | 구성 요소                                             |
|-----------------------|-------------------------------|----------|---------------------------------------------------|
| **App Server**        | `imhere-main-instance`        | t3.small | Nginx (Reverse Proxy), Spring Boot, Grafana Alloy |
| **Database Server**   | `imhere-database-instance`    | t3.small | MySQL (Self-managed)                              |
| **Middleware Server** | `imhere-middle-ware-instance` | t3.small | Redis, RabbitMQ                                   |

### 1.2 ECR (Elastic Container Registry)

애플리케이션의 Docker 이미지를 관리하는 프라이빗 리포지토리입니다.

- **리포지토리 명칭**: `imhere/dsko`
- **관리 방식**: 자세한 CI/CD 흐름은 [CI/CD 가이드](cicd.md)를 참고하세요.

---

## 2. 네트워크 및 보안

### 2.1 보안 그룹 (Security Group) 정책

- **App Server**: 외부 80(HTTP), 443(HTTPS) 접근 허용. 22(SSH)는 GitHub Runner IP만 한시 허용.
- **DB/Middleware**: App Server 보안 그룹에서 오는 인바운드 트래픽만 허용하여 내부 통신 격리.

### 2.2 리버스 프록시 (Nginx)

App Server에서 컨테이너로 동작하는 Nginx(`nginx-container`)가 호스트 80/443 포트를 매핑받아 HTTPS 터미네이션 및 CORS 처리를 수행하며, 동일 Docker 네트워크의 Spring Boot 컨테이너(`dsko:8080`)로 요청을 전달합니다.

---

## 3. 관련 문서 안내

더 상세한 정보는 아래의 전용 문서를 확인하세요.

- [Nginx 리버스 프록시 가이드](nginx.md)
- [관측성(Monitoring/Logging) 가이드](observability.md)
- [외부 서비스(SaaS/API) 연동 가이드](external-services.md)
- [설정 및 환경변수 가이드](configuration.md)
- [CI/CD 자동화 가이드](cicd.md)
- [운영 가이드(Operations)](operations.md)
