# Nginx 리버스 프록시 가이드

ImHere 프로젝트의 외부 트래픽 관문인 Nginx 설정과 HTTPS(SSL), CORS 처리 방식을 정리한 문서입니다.

---

## 1. 개요

Nginx는 호스트의 80/443 포트를 점유하여 모든 외부 요청을 수신하며, 이를 내부 Docker 네트워크의 Spring Boot 컨테이너(`dsko:8080`)로 안전하게 전달합니다.

---

## 2. 주요 기능 및 설정

### 2.1 HTTPS (SSL) 설정

- **인증서**: Let's Encrypt(Certbot)를 사용합니다.
- **연동 방식**: 호스트 서버의 `/etc/letsencrypt` 디렉토리를 Nginx 컨테이너에 읽기 전용(`ro`)으로 마운트하여 사용합니다.
- **자동 리다이렉트**: HTTP(80) 요청은 자동으로 HTTPS(443)로 301 리다이렉트됩니다.

### 2.2 CORS (Cross-Origin Resource Sharing) 처리

웹 클라이언트(예: `fortuneki.site`)에서의 보안 요청을 처리하기 위해 Nginx 레벨에서 CORS 헤더를 제어합니다.

- **허용 도메인**: `https://fortuneki.site` (Credentials 사용을 위해 와일드카드 대신 도메인 명시)
- **허용 메서드**: GET, POST, PUT, DELETE, PATCH, OPTIONS
- **Preflight 처리**: `OPTIONS` 메서드 요청에 대해 즉시 204 응답을 반환하여 백엔드 부하를 줄입니다.
- **중복 방지**: 백엔드(Spring)에서 보낼 수 있는 CORS 헤더와의 충돌을 막기 위해 `proxy_hide_header`를 사용합니다.

### 2.3 클라이언트 정보 전달

프록시 환경에서도 백엔드가 클라이언트의 실제 정보를 알 수 있도록 다음 헤더를 주입합니다.

- `Host`: 원래 요청의 호스트명
- `X-Real-IP`: 클라이언트의 실제 IP
- `X-Forwarded-For`: 경유한 모든 IP 리스트
- `X-Forwarded-Proto`: 사용된 프로토콜 (http/https)

---

## 3. 관리 및 업데이트

### 3.1 설정 파일 위치

- **소스**: 프로젝트 루트의 `nginx/nginx.conf`
- **적용**: 컨테이너 내의 `/etc/nginx/nginx.conf`로 마운트됨

### 3.2 설정 반영 방법

설정 변경 후 Nginx를 재시작하거나 설정을 reload해야 합니다.

```bash
# 컨테이너 재시작
docker compose -f docker-compose.prod.yml restart nginx

# 또는 설정 파일만 reload (무중단)
docker exec nginx-container nginx -s reload
```

---

## 4. 트러블슈팅

| 증상                      | 확인 사항                                                                           |
|-------------------------|---------------------------------------------------------------------------------|
| SSL 인증서 오류              | 호스트의 `/etc/letsencrypt` 경로에 인증서 파일이 실존하는지 확인.                                   |
| 접속 불가 (502 Bad Gateway) | `dsko` 컨테이너가 정상 기동 중인지, Nginx 설정의 `proxy_pass` 주소가 `http://dsko:8080`인지 확인.     |
| CORS 오류                 | 브라우저 콘솔의 에러 메시지를 확인하고, `nginx.conf`의 `Access-Control-Allow-Origin` 주소가 정확한지 점검. |
