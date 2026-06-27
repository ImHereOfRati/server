# Gabia

## 선택 이유

* 본 서비스에서 현재 예상되는 트래픽과 데이터 규모를 고려했을 때 가비아의 DB 플랜이 용량 및 Connection 수 측면에서 가장 적합하다고 생각했습니다.
    * AWS RDS 대신 가비아 MySQL 호스팅을 사용하여 초기 운영 비용을 절감하였습니다.
* 도메인을 가비아에서 관리하고 있으므로 DNS도 동일한 서비스에서 관리하여 운영을 단순화하였습니다.
* TLS 인증서는 Let's Encrypt(Certbot)를 사용하여 비용 없이 HTTPS를 운영하고, DNS 관리와 인증서 관리를 분리하였습니다.

---

## DNS

### 도메인

* `ratiko.co.kr`
* `www.ratiko.co.kr`

### 설정

* `ratiko.co.kr` 와 `www.ratiko.co.kr` A 레코드는 애플리케이션 EC2의 퍼블릭 IP(CloudFormation `AppElasticIp`)를 가리키도록 설정합니다.
* CD 가 `CERT_DOMAIN` 과 `www.$CERT_DOMAIN` 을 함께 발급하므로 두 이름 모두 같은 서버로 해석되어야 합니다.

---

## DB 호스팅 (MySQL)

### 설정

* 플랜: **250MB**
* Max Connection: **30**

---

## TLS 인증서

### 설정

* TLS 인증서는 Let's Encrypt(Certbot)를 통해 발급합니다.
* 가비아는 도메인(DNS) 관리만 담당합니다.
* 현재 방식은 `DNS-01` 이 아니라 `HTTP-01(webroot)` 이므로, 인증서 발급 시점에는 외부에서 `http://<domain>/.well-known/acme-challenge/...` 접근이 가능해야 합니다.
