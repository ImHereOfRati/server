# Nginx

운영 환경에서 Nginx는 외부 요청이 가장 먼저 도착하는 진입점입니다. TLS 종료, 경로별 라우팅, CORS 처리를 담당하고 그 뒤에 있는 Spring Boot(`dsko`)로 요청을 넘깁니다.

---

## 파일

| 파일 | 용도 |
|---|---|
| `infra/nginx/nginx.conf.template` | git에 저장하는 **원본** 설정. `${SERVER_NAME}` 같은 변수가 들어 있습니다 |
| `infra/nginx/nginx.conf` | CD가 배포할 때마다 템플릿을 렌더링해 만드는 **결과물**. 배포가 끝나면 EC2에서 삭제되고, git에도 추적하지 않습니다(`.gitignore`) |
| `infra/nginx/website.html` | `/` 경로에서 보여주는 정적 랜딩 페이지 |

변수가 어떤 값으로 채워지는지, 어디서 설정하는지는 [cicd.md](cicd.md)의 환경 변수 가이드를 참고합니다.

---

## 기본 설정

* `443` 포트에서만 실제 요청을 처리합니다. `80` 포트는 열지 않습니다(HTTP 응답 자체가 없음).
* TLS는 `1.2`/`1.3`만 허용합니다(`ssl_protocols`). 구버전 호환보다 보안을 우선합니다.
* Let's Encrypt 인증서를 `/etc/letsencrypt/live/${CERT_DOMAIN}/`에서 읽습니다(호스트 경로를 컨테이너에 마운트).
* `server_tokens off`로 Nginx 버전을 응답 헤더에 노출하지 않습니다.
* `gzip on`으로 응답을 압축합니다(구형 브라우저 `msie6`만 예외).

---

## 경로별 라우팅

| Location | 대상 | 비고 |
|---|---|---|
| `= /` | 정적 파일(`website.html`) | 랜딩 페이지 |
| `^~ /api/` | `http://dsko:8080` | CORS 헤더 처리 + 프리플라이트 단축 응답 (아래 참고) |
| `^~ /admin/` | `http://dsko:8080` | 관리자 API |
| `^~ /swagger-ui/` | `http://dsko:8080` | API 문서 UI |
| `^~ /docs/` | `http://dsko:8080` | REST Docs 정적 문서 |
| `^~ ${MGMT_BASE_PATH}/` | `http://dsko:8080` | Actuator/모니터링 — 추측하기 어려운 난독화 경로를 외부에 노출 |
| `/` (그 외 전부) | 정적 파일 + SPA fallback | `try_files $uri $uri/ /index.html` |

모든 백엔드 location은 `proxy_set_header`로 `Host`, `X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto`를 Spring Boot에 전달합니다.

---

## CORS 처리

`/api/` 경로는 자격 증명(쿠키/Authorization)을 쓰는 요청이라 Origin을 프런트엔드 도메인 하나로 고정합니다.

* `Access-Control-Allow-Origin`을 `${NGINX_ALLOWED_ORIGIN}` 값으로 고정하고, 캐시가 다른 Origin과 섞이지 않도록 `Vary: Origin`을 같이 보냅니다.
* `OPTIONS` 프리플라이트 요청은 백엔드까지 보내지 않고 Nginx에서 즉시 `204`로 끝냅니다(`Content-Length: 0`, `Access-Control-Max-Age: 1728000` ≈ 20일).
* Spring Boot가 같은 CORS 헤더를 중복으로 내려보내도, `proxy_hide_header 'Access-Control-Allow-Origin'`으로 백엔드 응답의 헤더를 제거해 최종 응답은 Nginx 기준 헤더 하나만 남습니다.

```text
Client (OPTIONS) → Nginx가 204로 즉시 응답 (백엔드로 안 보냄)
Client (GET/POST 등) → Nginx → http://dsko:8080 → Nginx가 응답 CORS 헤더 정리 → Client
```

---

## 설정 검증

CD `deploy-app` Job은 렌더링한 `nginx.conf`를 EC2로 보낸 뒤, 실제로 적용하기 전에 문법을 검증합니다.

```bash
docker run --rm -v "$PWD/infra/nginx/nginx.conf:/etc/nginx/nginx.conf:ro" nginx:alpine nginx -t
```

문법 오류가 있으면 여기서 배포가 멈추고, 기존에 떠 있던 nginx 컨테이너는 그대로 유지됩니다.

---

## 인증서 갱신

TLS 인증서는 가비아가 아니라 Let's Encrypt(Certbot)에서 발급합니다. 갱신 후에는 Nginx를 reload해야 새 인증서가 적용됩니다.

```bash
certbot renew
docker exec nginx-container nginx -s reload
```

인증서는 90일마다 만료되므로, 30일 전부터 자동 갱신되도록 cron 등록을 권장합니다. CD `deploy-app` Job도 매 배포 시 `certbot renew --quiet`를 실행합니다.

---

## 관련 문서

* CI/CD가 템플릿을 렌더링하는 과정과 변수 설정 방법은 [cicd.md](cicd.md)를 참고합니다.
* Docker 이미지/Compose 구성 전반은 [docker.md](docker.md)를 참고합니다.
