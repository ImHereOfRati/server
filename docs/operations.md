# 운영 가이드 (Operations)

ImHereServer의 초기 배포, 정기 업데이트, 장애 대응 및 유지보수 절차를 정리한 문서입니다.

---

## 1. 초기 배포 절차 (Zero to One)

운영 환경을 처음 구축할 때 수행하는 단계입니다.

1. **AWS 인프라 준비**: EC2 인스턴스 기동 및 보안 그룹 설정 (80, 443, 22 등).
2. **사전 파일 설치**: 다음 파일들을 EC2의 `${EC2_DEPLOY_PATH}` 경로에 수동 업로드합니다. (CD 파이프라인은 이 파일들을 전송하지 않으므로 최초 1회 직접 배치 필요)
    - `docker-compose.prod.yml`
    - `alloy-config.alloy`
    - `nginx/nginx.conf` (디렉터리 구조 유지)
    - `.env`
3. **CI/CD 연결**: [CI/CD 가이드](cicd.md)에 따라 GitHub Secrets와 OIDC를 설정합니다.
4. **첫 배포 실행**: main 브랜치 push를 통해 전체 파이프라인을 가동합니다.

---

## 2. 정기 운영 업무

### 2.1 설정 변경 (Secrets)
1. GitHub 리포지토리의 Secrets에서 대상 YAML 내용을 수정합니다.
2. 빈 커밋(`git commit --allow-empty`)을 push하여 CD를 재트리거합니다.
3. EC2 컨테이너의 설정 반영 여부를 확인합니다.

### 2.2 롤백 (Rollback)
- **긴급 대응**: EC2에 접속하여 `docker-compose.prod.yml`의 이미지 태그를 이전 버전(`<DATE-SHA>`)으로 변경 후 재시작합니다.
- **영구 수정**: main 브랜치에서 `git revert` 후 push하여 자동 배포를 유도합니다.

---

## 3. 유지보수 및 점검

### 3.1 로그 및 모니터링 확인
- **Grafana Cloud**: 모든 성능 지표 및 로그는 클라우드 대시보드에서 통합 확인합니다. [관측성 가이드](observability.md) 참고.
- **컨테이너 로그**: `docker logs -f iamhere-server-container`를 통해 실시간 기동 상태를 점검합니다.

### 3.2 데이터베이스 스키마 관리
- 운영 환경의 `ddl-auto`는 항상 `validate`입니다.
- 스키마 변경이 필요한 경우, 별도의 마이그레이션 도구나 수동 쿼리를 통해 반영 후 애플리케이션을 배포해야 합니다.

---

## 4. TLS 인증서 자동 갱신 (Let's Encrypt)

도메인 인증서는 호스트의 Certbot이 관리합니다. 현재는 **`standalone` 인증 + pre/post hook** 방식으로 갱신하며, 갱신 순간에만 `nginx-container`를 잠시 정지합니다(실측 ~9초/90일 1회).

### 4.1 동작 원리

1. `certbot.timer`(systemd)가 매일 1회 자동 실행
2. cert 만료 30일 이내일 때만 실제 갱신 시도 — 그 외엔 즉시 skip
3. 실제 갱신 시:
    - **pre-hook**: `nginx-container` 정지 → 호스트 80번 포트 비움
    - **standalone**: certbot이 자체 임시 HTTP 서버를 80번에 띄워 ACME challenge 응답
    - 새 cert가 `/etc/letsencrypt/live/fortuneki.site/`에 저장됨
    - **post-hook**: `nginx-container` 재기동 → 새 cert를 자동으로 로드

### 4.2 설정 파일 위치

| 파일                                                            | 역할                                                |
|---------------------------------------------------------------|---------------------------------------------------|
| `/etc/letsencrypt/renewal/fortuneki.site.conf`                | `authenticator = standalone`, `installer` 없음       |
| `/etc/letsencrypt/renewal-hooks/pre/stop-nginx-container.sh`  | `docker compose -f .../docker-compose.prod.yml stop nginx` |
| `/etc/letsencrypt/renewal-hooks/post/start-nginx-container.sh` | `docker compose -f .../docker-compose.prod.yml start nginx` |

> **주의**: 과거 `authenticator = nginx`(호스트 nginx 플러그인) 시절의 흔적이 남아 있을 경우, 호스트 nginx가 비활성화된 환경에서는 갱신이 실패합니다. 위 standalone 방식과 일치하는지 정기적으로 점검하세요.

### 4.3 검증

```bash
# 만료일 확인
sudo certbot certificates

# 자동 트리거 등록 상태
systemctl list-timers --all | grep -i certbot

# 갱신 시뮬레이션 (실제 cert 변경 없음, 컨테이너 stop/start만 실제로 발생)
sudo certbot renew --dry-run
```

dry-run이 `Congratulations, all simulated renewals succeeded`로 끝나면 정상.

### 4.4 트러블슈팅

| 증상 | 확인 사항 |
|---|---|
| `Could not bind TCP port 80` | 호스트 또는 다른 컨테이너가 80번을 점유 중 — `sudo ss -tlnp \| grep :80`으로 식별 후 정지 |
| post-hook 실패로 nginx 정지 상태 | `docker compose -f /home/ubuntu/imhere/docker-compose.prod.yml start nginx`로 수동 복구. hook 스크립트 실행권한(`chmod +x`) 점검 |
| dry-run은 통과하나 실제 갱신 실패 | `/var/log/letsencrypt/letsencrypt.log` 확인. ACME 서버 응답이 평소와 다른지, DNS A 레코드가 정상인지 점검 |

---

## 5. 트러블슈팅 체크리스트

| 증상 | 확인 순서 |
|---|---|
| 접속 불가 (Timeout) | Nginx 컨테이너 상태 및 포트(80/443) 상태 확인. `docker exec nginx-container nginx -s reload`로 설정 재적용. |
| DB 연결 오류 | Database EC2의 보안 그룹 및 `.env` 내 호스트 정보 확인. |
| 알림 미발송 | [외부 서비스 가이드](external-services.md)를 참고하여 각 서비스 자격증명 및 할당량 확인. |
| 특정 기능 오작동 | Discord로 전송된 에러 알림 로그의 `traceId`를 Grafana에서 추적. |
