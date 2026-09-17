# 165–200 TPS 안정 구간 재검증 계획

## 기존 결과의 한계

- 전체 `http_reqs`는 성공 처리율이 아니다. 실패·status 0도 포함한다.
- `breakpoint`는 각 목표까지 선형 상승한다. 전체 평균을 고정 TPS 처리 한계로 해석하지 않는다.
- 기존 보고서는 입력 `STAGE_DURATION`을 출력하므로 breakpoint의 실제 stages와 다를 수 있다. 예: 37,069 / 170.892 ≈ 217초로, 보고서의 10분과 일치하지 않는다. 중단 여부와 실제 설정은 당시 원시 자료 없이는 확정할 수 없다.
- 측정 연장은 누적 backlog를 해결하지 않는다. 회복을 확인하고 초기 조건을 맞춰야 한다.

## 통제 조건과 순서

1. 전용 AWS 스택, 기존 `ai-slice-steady-20260915` 이미지의 digest 고정·확인. 운영 환경은 사용하지 않는다.
2. 각 독립 실행 전 앱 정지 → 같은 seed 적재 → ANALYZE TABLE → 앱 재시작 → 인증 GET 정상 확인. event_publication 미완료와 notification PENDING/PROCESSING 개수를 기록한다.
3. 매 실행 30 RPS 2분 워밍업, 30초 종료 유예, 목표 TPS 10분 고정 유지. 워밍업과 측정 통계는 scenario 태그로 분리한다.
4. 100 TPS 기준선을 확인한 뒤 165 → 200 TPS를 측정한다. 200 TPS는 사용자가 지정한 최종 목표다. 최초 실패 시 무조건 상승하지 않고 반복 실행과 reads/혼합 비교, nginx 연결·오류 로그, Hikari, SQL digest로 원인을 좁힌다. 오류율이 측정 시작 60초 이후 20% 이상이면 조기 중단하고 실패로 기록한다.
5. 모든 실행 종료 후 60초 동안 backlog와 pending 회복을 관찰한다. 60초 안에 해소되지 않으면 회복 기준 실패. 다음 독립 실행은 DB와 앱을 초기화한 뒤 시작한다.
6. 첫 탐색에서 통과한 최고 TPS와 실패한 최저 TPS를 초기화 후 반복 검증한다. 두 번 통과한 값만 이번 조건의 안정 구간으로 인정한다. 100 TPS부터 실패하면 상위 구간 탐색보다 원인 진단을 우선한다.

## 수집과 판정

- 요청 목표, 시작 시각, 실제 실행 시간, 성공 건수/측정 예정 시간, 실패율, p50/p95/p99, dropped iterations, endpoint/status/error_code, 1분 구간별 HTTP 통계를 기록한다.
- 서버 CPU·메모리·재시작/OOM, Hikari active/pending/timeout, MySQL 실행·락·digest, 이벤트와 발송 backlog를 15초 간격으로 관찰한다. 누락된 지표는 통과로 간주하지 않는다.
- 10분 완주, 성공 처리량 목표의 99% 이상, 오류율 1% 미만, p95 <500ms, p99 <1초, dropped iterations 0. 이번 보수적 gate는 network/5xx 0건도 요구한다.
- Hikari timeout 증가 0, 지속적인 pending/backlog 증가 없음, 부하 종료 후 60초 이내 backlog 0을 함께 확인한다. HTTP 202는 실제 발송 완료와 구분한다.
- 실패가 재현되면 근거가 있는 병목 하나만 변경하고 동일 조건으로 재실행한다. 단순 pool/VU 확대나 무조건적인 측정 연장을 해결책으로 가정하지 않는다.
- 모든 결과 저장 후 이번 전용 스택 및 키를 삭제하고 삭제 상태를 확인한다.

## 도구 근거

[k6 constant-arrival-rate](https://grafana.com/docs/k6/latest/using-k6/scenarios/executors/constant-arrival-rate/)는 일정한 iteration 도착률을 유지한다. 현재 혼합 workload는 iteration당 HTTP 1회다. [dropped iterations](https://grafana.com/docs/k6/latest/using-k6/scenarios/concepts/dropped-iterations/)는 발송되지 못한 부하이므로 오류율과 별도로 평가한다.

## 첫 실행에 따른 측정기 보정

100 TPS 첫 10분 실행에서 HTTP 실패는 없었지만 마지막 1분에 dropped iterations 27건이 발생했다. 최대 지연 4.36초, VU pool은 300에서 326까지 증가했다. 동적 VU 생성의 영향을 배제하기 위해 후속 실행은 preAllocatedVUs=maxVUs=1000으로 고정한다. 이는 서버 최적화가 아니라 부하 발생 조건의 변경이며, 기존 결과와 구분해 저장한다. 서버 타임아웃·backlog 회복 기준은 완화하지 않는다.

## 사용자 요청에 따른 최종 목표

사용자가 목표를 약 200 TPS로 지정했다. 100→165 TPS를 2분 동안 올린 뒤 175 TPS부터 10 TPS씩 각 2분 유지한다. HTTP 실패율이 5%를 초과하면 즉시 중단한다. 최초 실패 시 반복·읽기 전용 진단으로 원인을 구분하며, 추가 상향은 하지 않는다.

## 재실행

먼저 기존 setup 절차로 전용 `imhere-loadtest-stability-*` 스택과 fixture/JWT를 준비한다. setup의 `.loadtest-state`와 대상 스택이 일치해야 하며, 동일 보고서 경로의 완료 결과는 덮어쓰지 않는다.

```bash
STACK_NAME=imhere-loadtest-stability-YYYYMMDD \
REPORT_DIR=docs/loadtest/stability/unique-run-id \
node loadtest/tools/run-stability.mjs
```

원시 요청 지표와 서비스 로그는 git 제외 경로 `loadtest/k6/generated/stability/<run-id>/`에, 집계·관찰 결과는 REPORT_DIR에 저장한다. 기본 탐색 지점은 `RATES=100,165,200`이며 환경 변수로 명시할 수 있다. 완료·실패 여부와 무관하게 수집 후 같은 state를 이용해 `bash loadtest/setup/teardown-loadtest.sh --yes`를 실행하고 AWS 삭제 결과를 확인한다. 실행기는 인프라를 자동 삭제하지 않으므로 장애 시 자료를 먼저 수집할 수 있다.
