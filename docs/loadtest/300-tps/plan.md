# 300 TPS AI 실험 계획

## 목표와 합격 기준

앱·DB를 `t3.small`로 고정한 격리 AWS 환경에서 mixed workload(읽기 75%, FCM 15%, SMS 10%)를 제공 부하 300 TPS로 10분 유지한다.

- 성공 처리량 평균: 300 TPS 이상
- HTTP p95: 500ms 이하
- HTTP p99: 1초 이하
- 5xx 및 네트워크 오류: 각각 1% 미만
- Hikari connection timeout: 0
- 지속 증가하는 Hikari pending 없음
- 이벤트 publication·알림 backlog가 부하 종료 후 60초 안에 해소

부하테스트 nginx에서만 `limit_req`·`limit_conn`을 제거한다. 운영 설정은 대상이 아니다.

## 실험 순서

1. 기준선: 현재 앱, MySQL `max_connections=30`, Hikari pool 30. 100→200→250→300→350 TPS를 단계별 3분 측정한다.
2. 연결 한도 분리: 동일 이미지에서 MySQL `max_connections=80`, Hikari 30으로 측정한다.
3. 병목 증거에 따라 Hikari lifetime/OSIV, pool 20 또는 40, 최상위 SQL 인덱스·쿼리, 알림 트랜잭션 왕복 후보를 선택한다.
4. 후보 하나당 한 원인만 변경하고 전체 Gradle 테스트와 300 TPS 3분 quick gate를 실행한다.
5. 최대 6개 구성까지만 평가한다. p95가 10% 이상 개선되거나 SLO를 통과하고 오류·DB 자원·backlog가 악화되지 않은 후보만 기준점으로 채택한다.
6. 최종 승자는 현행·확대 데이터셋에서 300 TPS 10분과 350 TPS 1분 burst를 다시 실행한다.

## SQL Optimizer 규칙

- 각 seed reset 뒤 `ANALYZE TABLE users, friend_relations, notification, event_publication`을 실행한다.
- `performance_schema.events_statements_summary_by_digest`에서 총 시간, 평균 시간, rows examined 상위 digest를 수집한다.
- 대표 조회는 `EXPLAIN ANALYZE`, 변경문은 실행하지 않는 `EXPLAIN FORMAT=JSON`으로 기록한다.
- 인덱스 또는 QueryDSL 변경은 한 후보에 하나만 적용해 효과를 식별한다.
- 운영 DB에는 자동 DDL을 실행하지 않고, 승자 보고서에 forward/rollback DDL과 적용 순서를 기록한다.

## 산출물

- `runs/<run-id>/suite.md`: 후보 전체 요약
- `runs/<run-id>/<candidate>/...md`: k6 결과와 판정
- `runs/<run-id>/<candidate>/sql-optimizer.md`: digest·인덱스·실행계획
- `final-report.md`: 최종 승자, 회귀 결과, 운영 적용안과 한계

원시 JSON·로그·Prometheus 덤프는 `loadtest/k6/generated/experiments/` 아래에만 보존하고 Markdown에서 경로를 참조한다.
