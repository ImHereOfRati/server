# 부하 테스트 보고서 — 2026-08-31

## 요약

`loadtest/run-loadtest.sh`를 통해 AWS 테스트 환경을 생성하고, `user-me.js`로 인증 단일 사용자 API 부하를 측정했다. 테스트 종료 후 스크립트의 정리 절차로 CloudFormation 스택과 테스트 키 페어를 삭제했다.

이번 실행은 입력상 `single / 100 RPS / 10s`였으나, 실행 전 `user-me.js`가 `TEST_PLAN`과 `TARGET_RPS`를 반영하지 않는 상태였다. 따라서 실제 부하는 기존 고정 단계인 200 → 300 → 400 RPS, 각 10초로 진행됐다. 이 불일치는 후속 수정에서 `single` 계획이 지정 RPS 한 단계만 사용하도록 보완했다.

## 실행 조건

| 항목 | 값 |
|---|---|
| 테스트 | `one-to-many-test/user-me.js` |
| API | `GET /api/users/my` |
| 인증 | fixture JWT 1개 (`ACTOR_INDEX=0`) |
| 입력 계획 | `single` |
| 입력 목표 RPS | `100` |
| 입력 단계 시간 | `10s` |
| 실제 단계 | 200 → 300 → 400 RPS, 각 10초 |
| AWS 리전 | `ap-northeast-2` |
| 결과 JSON | `loadtest/k6/generated/results/user-me-single-20260831-205910.json` |

## 결과

| 지표 | 결과 |
|---|---:|
| HTTP 요청 | 8,000 |
| 실제 HTTP 처리율 | 266.36 req/s |
| p50 | 7.95 ms |
| p90 | 12.61 ms |
| p95 | 18.57 ms |
| 최대 응답 시간 | 1.55 s |
| 429 응답 | 7,670 (255.37/s) |
| 5xx 응답 | 25 (0.83/s) |
| 네트워크 오류 | 0 (check 8,000/8,000 통과) |
| 최대 VU | 23 |

## 해석

단일 JWT를 공유한 요청은 대부분 429로 제한됐으며, 이는 사용자별 또는 경로별 limiter가 작동했음을 보여준다. 다만 5xx가 25건 발생했으므로, 제한 초과 상황에서도 upstream 오류가 함께 발생하지 않도록 애플리케이션·nginx 로그를 추가 확인해야 한다. k6의 기본 `http_req_failed`는 429도 실패로 집계하므로, rate-limit 검증에서는 커스텀 429·5xx·network error 지표를 함께 봐야 한다.

이번 결과는 실행 스크립트가 생성·배포·실행·정리까지 완료되는 것을 검증했지만, 입력 RPS 검증 결과로 사용해서는 안 된다. `user-me.js`의 `single` 계획 반영 수정 후 동일 조건을 재실행해야 100 RPS 기준선으로 활용할 수 있다.

## 수정 후 동일 조건 재검증

분석 결과, JWT가 참조하는 fixture 사용자를 DB에 넣지 않은 채 테스트를 실행하고 있었다. 그 결과 애플리케이션 로그에 `USER-300(사용자를 찾을 수 없습니다)`가 기록됐다. 생성 순서를 바꾸고, DB 초기화 직후 생성된 `seed.sql`을 원격 MySQL에서 실행하도록 수정한 뒤 동일 조건을 재실행했다.

| 항목 | 수정 후 결과 |
|---|---:|
| 실행 조건 | `single / 100 RPS / 10s` |
| HTTP 요청 | 999 |
| 실제 HTTP 처리율 | 99.76 req/s |
| p50 | 10.37 ms |
| p90 | 24.89 ms |
| p95 | 31.96 ms |
| 최대 응답 시간 | 1.37 s |
| 429 응답 | 869 (86.77/s) |
| 503 응답 | 23 (2.30/s) |
| 애플리케이션 upstream 5xx | 0 |
| 네트워크 오류 | 0 |
| check 통과 | 999/999 |
| 최대 VU | 23 |
| 결과 JSON | `loadtest/k6/generated/results/user-me-single-20260831-214327.json` |

### 최종 해석

fixture 주입 수정 후 애플리케이션 upstream 5xx는 0건으로 감소했다. 남은 5xx 23건은 모두 Nginx 503으로 분리 집계됐으며, 429 응답 869건과 함께 현재 요청 제한·게이트웨이 보호 동작의 검토 대상이다. k6의 `http_req_failed`는 429도 실패로 집계하므로, 이후 rate-limit 검증에서는 커스텀 429·503·upstream 5xx·네트워크 오류 지표를 기준으로 판단한다.

테스트 종료 후 CloudFormation 스택과 테스트 키 페어는 자동 삭제됐다.

## 이슈 #128 반영 후 재검증

이슈에서 지적한 타임아웃 충돌을 줄이기 위해 HikariCP `connection-timeout` 기본값을 30초에서 5초로 변경했다. Nginx의 `proxy_read_timeout`은 30초를 유지해 DB 커넥션 고갈 시 애플리케이션이 먼저 실패하도록 순서를 보장했다.

또한 다음 관측 항목을 추가했다.

- HikariCP pending 및 timeout 초당 발생량을 Prometheus recording rule과 Grafana 대시보드에서 확인
- Nginx access log에 최종 상태 코드와 함께 upstream 상태, 연결 시간, 응답 시간을 기록
- k6에서 503 gateway 오류와 500·502·504 등 upstream 서버 오류를 별도 집계

동일 조건으로 재실행한 결과는 다음과 같다.

| 항목 | 결과 |
|---|---:|
| 실행 조건 | `single / 100 RPS / 10s` |
| HTTP 요청 | 999 |
| 실제 HTTP 처리율 | 99.78 req/s |
| p50 / p95 | 9.84 ms / 32.15 ms |
| 429 응답 | 869 (86.80/s) |
| 503 응답 | 20 (2.00/s) |
| 애플리케이션 upstream 5xx | 0 |
| 네트워크 오류 | 0 |
| check 통과 | 999/999 |
| 결과 JSON | `loadtest/k6/generated/results/user-me-single-20260831-225408.json` |

이번 실행에서 남은 503은 애플리케이션 500이나 Nginx 504가 아니라 Nginx의 동시 연결 제한 응답으로 분리됐다. 애플리케이션 upstream 5xx 및 네트워크 오류는 0건이므로 이슈의 핵심 완료 기준인 원인 구분은 충족했다. 테스트 종료 후 AWS 부하테스트 스택과 키 페어도 자동 삭제됐다.

