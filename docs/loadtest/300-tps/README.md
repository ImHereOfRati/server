# 300 TPS AI Load-Test Loop

이 디렉터리는 부하테스트 nginx 제한을 제거한 격리 환경에서 300 TPS를 검증하고, DB digest·실행계획·Hikari 지표를 근거로 AI가 후보를 순차 평가하는 기록 공간이다.

## 실행

```bash
NON_INTERACTIVE=true \
TEST_FILE=many-people-many-request-test/mixed-workload.js \
TEST_PLAN=single TARGET_RPS=300 STAGE_DURATION=10m \
REPORT_DIR="$PWD/docs/loadtest/300-tps/runs" \
./loadtest/run-loadtest.sh
```

실험용 nginx에서만 `limit_req`와 `limit_conn`을 제거한다. 운영 nginx에는 이 설정을 복사하지 않는다.

## AI 규칙

1. 모든 후보는 별도 worktree와 고유 ECR tag로 빌드한다.
2. 동일 seed로 DB를 재초기화하고 `ANALYZE TABLE` 후 실행한다.
3. 후보마다 코드·설정·SQL 중 하나의 원인만 바꾼다.
4. 300 TPS 3분 quick gate와 회귀 테스트를 통과한 후보만 다음 기준점으로 삼는다.
5. 최종 승자는 현행·확대 데이터셋에서 300 TPS 10분과 350 TPS 1분 burst를 통과해야 한다.
6. Markdown 보고서가 없는 실행은 성공으로 인정하지 않는다.

## 판정 기준

- 성공 처리량 평균 300 TPS 이상
- p95 500ms 이하, p99 1초 이하
- 5xx 및 네트워크 오류 각각 1% 미만
- Hikari timeout 0, 지속 증가 pending 없음
- 비동기 이벤트/알림 backlog가 부하 종료 후 60초 안에 해소
