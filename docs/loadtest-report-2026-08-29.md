---
# 수정 필요
---
---

# ImHere 현재 서버 부하 측정 보고서

> 이 문서는 2026-08-29 기존 실행의 기록이다. AWS 재구성 후 ECR 최신 prod image digest와 3분 단계 기준으로 재실행한 결과가 나오면 본문 수치와 결론을 새 실행 결과로 교체해야 한다. 기존 결과의 `592 iteration/s`와 `194 HTTP RPS` 관계는 재실행 전까지 확정하지 않는다.

## 보고서 요약

2026년 8월 29일 하루 동안 운영 환경을 재현한 테스트 서버에 단계적·복합 요청을 발생시켜, 현재 서버가 어떤 상태에서 어느 정도의 요청을 처리하고 언제 서비스 불능에 가까워지는지 확인했다.

이번 측정의 핵심 결론은 다음과 같다.

- 서버가 정상적으로 처리한 실제 HTTP 처리량은 약 `100~136 RPS` 구간에서 안정적이었다.
- 부하가 누적된 상태에서 약 `190~200 HTTP RPS`를 넘기자 응답 지연과 연결 종료가 급증했다.
- 약 `194 HTTP RPS` 관측 시점에 오류율은 `42.27%`, p95는 `3.42초`였다.
- HikariCP pending은 약 `165`까지 증가했고, load average는 약 `17`까지 상승했다.
- EC2, Spring Boot, nginx, Alloy 컨테이너는 종료되지 않았다.
- 부하를 제거한 뒤 약 30초 후 HTTP `200` 응답으로 회복했다.
- 따라서 이번 서버의 실질적인 서비스 한계는 “1000 RPS”가 아니라, 현재 복합 workload 기준 약 `190~200 HTTP RPS` 부근으로 판단한다.

여기서 RPS는 서버가 실제로 받은 HTTP request/s를 의미한다. k6의 iteration/s는 한 iteration 안에서 여러 HTTP 요청을 발생시킬 수 있으므로 HTTP RPS와 동일하지 않다.

---

## 1. 테스트 환경

### 1.1 측정 대상 구성

운영 병목을 재현하기 위해 애플리케이션·nginx·Alloy를 하나의 `t3.small` EC2에서 실행했다.

| 항목 | 구성 |
|---|---|
| AWS 리전 | `ap-northeast-2` |
| 애플리케이션 EC2 | `t3.small`, 2 vCPU, 2GiB RAM |
| 애플리케이션 실행 | Spring Boot + nginx + Alloy 동시 실행 |
| Java | Java 25 |
| DB | MySQL 8.0 |
| DB 연결 제한 | 최대 30 connections |
| DB 메모리 제한 | 별도 제한 없음 |
| DB 테스트 저장 공간 | 2GB loopback 파일 |
| 부하 발생기 | 대상 EC2 외부에서 실행 |
| 부하 발생기 공인 IP | 비공개 실행 metadata 참조 |
| 허용 CIDR | `112.170.50.0/24` |

### 1.2 측정 당시 AWS 식별 정보

측정 당시 사용한 리소스는 테스트 종료 후 모두 해제했다. 아래 값은 측정 재현 및 당시 환경 식별을 위한 기록이다.

| 리소스 | 측정 당시 값 |
|---|---|
| CloudFormation stack | `imhere-loadtest` |
| 애플리케이션 EC2 | 비공개 실행 metadata 참조 |
| 애플리케이션 Public IP | 재현 metadata 참조 |
| DB Private IP | 재현 metadata 참조 |
| DB Public IP | 비공개 |
| 관측성 EC2 | 비공개 실행 metadata 참조 |
| 관측성 Private IP | 재현 metadata 참조 |
| 관측성 Public IP | 테스트 실행 후 최종 응답으로 제공 |

### 1.3 데이터베이스 상태

- 데이터베이스 이름: `rati`
- root 비밀번호: 테스트 secret으로 주입하며 보고서에 기록하지 않음
- MySQL image: `mysql:8.0`
- `max_connections`: `30`
- 테스트 사용자: 1,000명 (검증 리소스 제약 반영 재실행 기준)
- fixture friendship: 500건 (고정 seed)
- 테스트 알림: 약 20,000건
- 테스트용 mutation pair: 사용하지 않음

친구 요청 생성·수락·삭제 트래픽은 검증용 AWS 인스턴스, MySQL, 커넥션 풀 리소스 제약으로 제외했다. 친구 관계 데이터는 고정 seed로만 적재하고 조회 경로 검증에 사용한다.

초기 200MB 저장 공간에서는 MySQL 8.0 시스템 테이블 생성 중 `No space left on device`가 발생했다. 이후 저장 공간을 2GB로 확장하고 schema와 fixture를 재적재했다. 따라서 이번 성능 결과에서 DB 디스크 용량 부족은 주요 병목으로 작용하지 않았다.

### 1.4 관측성 구성

관측성 EC2에 다음 서비스를 함께 구성했다.

- Grafana
- Prometheus
- Loki
- Tempo

주요 수집 항목은 다음과 같다.

- HTTP request count, duration, outcome
- JVM heap/non-heap memory
- GC overhead 및 pause
- JVM thread 상태
- HikariCP active, idle, pending, timeout, acquire time
- process CPU, uptime, system load
- 애플리케이션 로그
- OpenTelemetry trace

---

## 2. 사전 설정

### 2.1 인증

OAuth 외부 인증 과정은 측정 경로에서 제외하고 사용자별 JWT를 사전 발급했다. 요청 자체는 실제 애플리케이션의 JWT 검증 필터와 Spring Security 경로를 통과하도록 구성했다.

이를 통해 외부 OAuth Provider의 처리량이 아니라 다음 서버 내부 비용을 포함했다.

- Authorization header 처리
- JWT 서명 검증
- Claim 파싱
- Security context 생성
- 인증 사용자 조회
- 인증 이후 controller/service/repository 처리

### 2.2 외부 발송 Provider

실제 비용과 외부 시스템 영향을 방지하기 위해 SMS 및 FCM은 테스트용 Provider로 교체했다.

- 실제 Firebase 호출 없음
- 실제 Solapi 문자 발송 없음
- Provider 지연 및 실패율을 환경 변수로 제어 가능
- 이번 측정에서는 외부 발송 비용이 발생하지 않도록 구성

### 2.3 요청 workload

하루 측정에는 다음 사용자 행동이 포함됐다.

- 일반 사용자 정보 조회
- 사용자 검색
- 친구 목록 조회
- 친구 요청 조회
- 알림 목록 조회
- 친구 요청 생성 및 수락
- 친구 관계 별칭 변경 및 삭제
- FCM 알림 생성
- SMS 알림 생성

복합 workload는 읽기 요청과 친구 관계 변경, FCM, SMS 요청을 함께 발생시켰다. 단일 API만 호출하는 값이 아니라 실제 애플리케이션의 여러 DB·이벤트 경로가 함께 활성화된 상태를 관찰하는 데 목적이 있었다.

### 2.4 부하 증가 방식

부하는 낮은 수준에서 시작해 단계적으로 높였다.

```text
100 RPS에서 시작해 100 RPS씩 증가 → 200 RPS → … → 1000 RPS (각 단계 3분)
```

후반부는 각 iteration이 여러 HTTP 요청을 포함하는 복합 workload였기 때문에, 목표 iteration rate가 실제 HTTP request rate보다 크게 보일 수 있다.

### 2.5 실행 전 검증과 문제 정리

측정 시작 전 다음을 확인했다.

- 애플리케이션 readiness `UP`
- DB health `UP`
- Prometheus readiness 정상
- Loki readiness 정상
- Tempo readiness 정상
- Prometheus 애플리케이션 scrape `up=1`
- 사용자·친구·알림 대표 API `200`

초기 DB schema 누락이 발견되어 schema와 fixture를 재적재한 후 측정을 진행했다. 또한 friend mutation에서 동시 요청이 동일 pair를 선택하는 인덱스 문제가 발견되어 mutation pair를 100,000개로 늘리고 VU·iteration 조합으로 pair를 분산하도록 수정했다.

---

## 3. 실제 결과

### 3.1 정상 처리 구간

read-heavy 요청을 포함한 정밀 측정에서 약 `100~136 HTTP RPS` 구간은 다음과 같은 상태였다.

| 실제 HTTP RPS | CPU | Hikari pending | Connection timeout | 관측 상태 |
|---:|---:|---:|---:|---|
| 약 100 | 약 15~21% | 0 | 0 | 정상 |
| 약 103 | 약 21% | 0 | 0 | 정상 |
| 약 136 | 약 23% | 0 | 0 | 정상 |

read-only 정밀 요청의 k6 최종 출력에서는 `http_req_failed=0.00%`였고, endpoint check도 성공했다. 이 구간에서는 DB connection pool 대기나 서버 연결 오류가 관찰되지 않았다.

### 3.2 포화 및 서비스 불능 구간

부하를 계속 증가시키자 약 `190~200 HTTP RPS` 부근부터 서버 응답 품질이 급격히 저하됐다.

| 항목 | 관측값 |
|---|---:|
| k6 iteration 처리량 | 약 `592 iteration/s` |
| 실제 HTTP 처리량 | 약 `194 RPS` |
| HTTP 오류율 | `42.27%` |
| p95 응답시간 | `3.42초` |
| 평균 응답시간 | `743.77ms` |
| dropped iteration | `1,367` |
| 최대 VU | 약 `555` |
| Hikari pending | 약 `165` |
| process CPU | 약 `68%` |
| system load average | 약 `17` |
| 주요 오류 | EOF, 연결 강제 종료 |

이 상태에서는 서버 프로세스가 살아 있더라도 정상적인 서비스 제공 상태로 볼 수 없다. nginx와 k6 모두 연결 종료를 관찰했고, 일부 요청은 응답을 받기 전에 EOF로 종료됐다.

### 3.3 RPS와 iteration/s의 차이

이번 측정에서 k6가 약 `592 iteration/s`를 표시했지만 서버 HTTP metric은 약 `194 RPS`였다. 이는 측정 오류가 아니라 workload 구조 차이 때문이다.

- 조회 iteration: 대체로 HTTP 1건
- 친구 변경 iteration: 생성·수락·별칭 변경·삭제 등 여러 HTTP 요청
- 알림 iteration: Provider별 알림 생성 요청과 비동기 처리
- 네트워크 오류 iteration: 요청 일부가 완료되지 않거나 EOF로 종료

따라서 서버가 견딘 실제 처리량은 k6 iteration/s가 아니라 Prometheus의 `http_server_requests_seconds_count` 및 k6 `http_reqs`를 기준으로 약 `194 RPS`로 기록한다.

### 3.4 오류 내용

관측된 오류는 하나의 원인으로 단정하지 않는다.

1. 서버 포화로 인한 EOF 및 연결 강제 종료
2. Hikari connection pool pending 증가
3. friend mutation 동시성 충돌에 따른 `409 Conflict`
4. 부하 증가에 따른 응답 지연 및 dropped iteration
5. 서버 오류 발생 시 Discord 외부 알림 연결 실패 로그

특히 friend mutation의 초기 실행에는 동일 pair 선택 오류가 포함되어 `409 Conflict`가 과도하게 발생했다. 이 값은 순수한 서버 처리 한계를 나타내는 결과로 사용하지 않는다. 이후 pair 선택 로직은 수정했지만, 수정 후 독립적인 mutation 장기 결과를 별도로 확정하지는 않았다.

### 3.5 서버 다운 여부와 회복

이번 측정에서 “서버 다운”은 EC2나 컨테이너가 종료된 상태가 아니라 HTTP 서비스가 사실상 정상 처리되지 않는 상태를 포함해 판단했다.

실제 상태는 다음과 같았다.

- EC2 instance: `running`
- EC2 system status: `ok`
- Spring Boot container: 실행 유지
- nginx container: 실행 유지
- Alloy container: 실행 유지
- 부하 중 HTTP: EOF 및 연결 강제 종료
- 부하 제거 후 약 30초: HTTP `200` 응답 회복
- 애플리케이션 재시작: 없음

결론적으로 현재 서버는 프로세스 생존성은 유지했지만 약 `190~200 HTTP RPS` 부근에서 서비스 불능 상태에 진입했다. 부하를 제거하면 별도 재시작 없이 회복하는 특성이 확인됐다.

### 3.6 하루 측정 기준의 서버 내구성 판단

이번 측정은 특정 API 하나의 최고 기록을 구하는 것이 아니라, 현재 서버가 다양한 사용자 요청과 DB 작업이 동시에 발생하는 상태에서 얼마나 버티는지를 확인한 것이다.

현재 서버의 상태를 부하 수준별로 정리하면 다음과 같다.

| 상태 | 실제 HTTP RPS | 서버 상태 |
|---|---:|---|
| 안정 처리 | `100~136` | CPU·Hikari pending 여유, 오류 없음 |
| 부담 증가 예상 | `150 전후` | workload 조합에 따라 응답시간 증가 가능 |
| 포화 진입 | `190~200` | pending·load 증가, EOF 발생 시작 |
| 실질적 서비스 불능 | `약 194 이상` | 오류율 42.27%, p95 3.42초 |
| 목표 초과 영역 | `800~1000 목표` | 실제 도달 전 포화로 측정 중단 |

---

## 4. 차후 검토

### 4.1 테스트 신뢰도 개선

#### 시나리오와 workload 해석

- 향후에는 read-only, friend mutation, FCM, SMS를 별도 실행할 수 있도록 유지한다.
- 단, 최종 서버 내구성 판단은 개별 시나리오가 아니라 실제 비율을 반영한 복합 workload 결과를 기준으로 한다.
- burst와 ramp를 별도 기록한다. 평균 RPS가 같아도 도착 간격과 동시성이 다르면 결과가 달라진다.
- 10초 burst, 1분 burst, 완만한 ramp를 각각 비교한다.

#### 데이터와 mutation pair

- friend mutation은 요청 생성 후 관계 상태를 재사용하지 않도록 매 실행마다 충분한 고유 pair를 할당한다.
- pair는 기존 friendship과 중복되지 않아야 한다.
- 한 테스트에서 필요한 최대 동시성보다 충분히 많은 pair를 준비한다.
- 생성·수락·별칭 변경·삭제의 각 단계별 성공/실패를 개별 집계한다.
- fixture 재생성 후 JWT와 DB row가 동일한 사용자 집합을 가리키는지 검사한다.

#### 오류 metric

다음 metric을 별도로 기록해야 한다.

- 전체 HTTP request 수
- HTTP status별 응답 수
- `409 Conflict` 수
- `4xx` 수
- `5xx` 수
- 네트워크 오류 및 EOF 수
- timeout 수
- endpoint별 p50/p95/p99
- k6 iteration/s
- 실제 HTTP request/s

`409 Conflict`는 데이터·동시성 충돌이고 `5xx`와 EOF는 서버 처리 실패이므로 하나의 오류율로만 해석하지 않는다.

#### 사전 검증 자동화

다음 gate를 부하 실행 전 자동화한다.

> 이때 사용한 `loadtest/tools/validate-fixture.mjs`와 `loadtest/tools/validate-db.ps1`은 이후 제거했다. 아래 항목은 부하 실행 전 확인할 체크리스트로 남긴다.

fixture 검증 항목:

- 사용자 수
- friendship 수
- 고유 mutation pair 수
- JWT 수와 사용자 수 일치
- 토큰 누락 여부
- mutation pair 중복 여부

DB는 다음을 확인한 뒤 부하를 시작한다.

DB 검증 항목:

- 필수 table 존재 여부
- users row 수
- friend_relations row 수
- notification row 수
- MySQL version
- `max_connections`

### 4.2 서버 및 DB 병목 검토

#### HikariCP와 DB 연결

이번 결과에서 Hikari pending이 약 `165`까지 증가했으므로, 다음 구간을 정량적으로 재측정해야 한다.

- pending이 0에서 증가하기 시작하는 RPS
- pending 10, 30, 100 도달 시점
- connection timeout이 처음 발생하는 RPS
- acquire max가 0.5초와 1초를 넘는 시점
- 부하 제거 후 pending이 0으로 돌아오는 시간

DB max connection이 30이므로 애플리케이션 요청 수가 30이라는 의미가 아니다. 쿼리 점유 시간이 길어질수록 같은 pool로 처리 가능한 RPS가 낮아진다.

#### Slow query·인덱스·lock·disk I/O

이번 측정에서는 해당 DB 내부 지표를 별도 exporter로 정량 수집하지 못했다. 다음 항목을 추가해야 한다.

- MySQL slow query log
- `performance_schema` statement latency
- table scan 및 rows examined
- 주요 조회 조건의 인덱스 사용 여부
- InnoDB row lock wait
- buffer pool hit ratio
- disk read/write latency
- disk queue 및 IOPS
- active connection 및 thread 상태

특히 friend relation 조회와 notification pagination query는 데이터가 증가할수록 인덱스와 정렬 비용을 다시 확인해야 한다.

#### nginx와 네트워크

- upstream connect timeout
- upstream read timeout
- active connection
- waiting connection
- accept backlog
- `worker_connections`
- 502/504 응답 수
- keepalive connection 재사용률
- SYN backlog 및 listen queue

애플리케이션이 살아 있어도 nginx backlog와 upstream timeout이 먼저 포화되면 외부에서는 서버 다운처럼 보일 수 있다.

#### Spring thread와 비동기 executor

- Tomcat request thread active/max
- blocked·waiting thread 수
- 비동기 알림 executor active thread
- executor queue depth
- rejected task 수
- 처리 완료까지 걸린 시간
- `PROCESSING` 상태 알림 수
- retry 대상과 실제 retry 완료 수

알림 Provider 지연을 0ms, 200ms, 1s로 바꾸어 외부 지연이 DB·executor·HTTP client pool에 미치는 영향을 분리해야 한다.

#### JVM

부하 구간별로 다음 값을 저장해야 한다.

- heap used/max
- old generation 사용량
- allocation rate
- GC count
- GC pause time
- GC overhead
- live thread 수
- blocked thread 수
- process RSS
- container memory
- OOM 및 restart 여부

이번 환경은 50MB heap 제한을 적용하지 않고 메모리 제한을 제거했으므로, 이번 결과는 “50MB heap 한계”가 아니라 `t3.small` 및 컨테이너 자원 경쟁에 대한 결과다.

### 4.3 순간 피크 검토

완만한 ramp와 순간 burst는 별도로 확인해야 한다. 이 서비스에서는 여러 사용자에게 동시에 알림을 보내는 상황, 특정 시간대의 재접속, 장애 회복 후 재시도 등으로 순간 피크가 발생할 수 있다.

권장 burst 모델:

```text
기준 50 RPS 3분
→ 10초간 300 RPS
→ 기준 50 RPS 10분
```

측정 항목:

- burst 순간의 실제 HTTP RPS
- p95/p99
- Hikari pending peak
- connection timeout
- nginx 502/504 및 EOF
- executor queue peak
- 정상화까지의 회복 시간

### 4.4 운영 기준 제안

현재 측정 결과만 기준으로 임시 운영 기준을 제안하면 다음과 같다.

- 정상 처리 목표: `100 HTTP RPS 이하`
- 관찰 필요 구간: `100~150 HTTP RPS`
- 사전 확장·rate limit 검토 구간: `150~190 HTTP RPS`
- 서비스 보호가 필요한 구간: `190 HTTP RPS 이상`

이 값은 최종 SLO가 아니라 현재 테스트 데이터에 기반한 임시 기준이다. 실제 운영 traffic pattern과 burst 크기를 반영한 후 확정해야 한다.

### 4.5 테스트 리소스 정리

측정 완료 후 다음 테스트용 AWS 리소스를 해제했다.

- 애플리케이션 EC2
- DB EC2
- 관측성 EC2
- 테스트 VPC 및 subnet
- route table 및 internet gateway
- security group
- public IP 및 관련 네트워크 리소스
- CloudFormation stack `imhere-loadtest`
- 테스트용 AWS key pair 2개

삭제 검증 결과:

- CloudFormation stack: 존재하지 않음
- 테스트 tag를 가진 활성 EC2: 없음
- `imhere-loadtest-*` AWS key pair: 없음

로컬의 테스트 코드와 credential 파일은 AWS 리소스와 별개다. JWT secret과 Grafana 비밀번호는 이 보고서에 기록하지 않는다.
## 최종 재실행 결정 및 관찰 결과 (2026-08-30)

검증 리소스 제약을 고려해 최종 실행은 사용자 1,000명, 고정 친구 관계 500건으로 축소했다. 친구 요청 생성·수락·삭제 트래픽은 제외하고 조회·FCM·SMS 경로만 부하에 포함했다. 친구 요청 mutation은 동일 관계 재사용과 DB/커넥션 풀 리소스 소모가 검증 결과를 왜곡하므로 사용하지 않았다.

부하는 100 RPS에서 시작해 100 RPS씩 3분 간격으로 증가시켰다. 앱 처리량이 600 RPS 구간에서 더 이상 유의미하게 증가하지 않고 응답 지연이 커져 600 RPS를 실질 상한으로 판단했으며, 700 RPS 이상 단계는 진행하지 않았다. 해당 실행은 상한 도달 시점에 의도적으로 중단했으므로 k6 전체 summary JSON은 생성하지 않았고, 이를 완주 성능 결과로 해석하지 않는다.

중단 직전 앱은 Prometheus `up=1`이었고 실제 완료 HTTP RPS는 약 64, 앱 컨테이너 CPU는 약 110%, 메모리는 약 720MB였다. 부하 제거 후 약 30초 recovery에서 실제 RPS는 0, `up=1`, 앱 CPU는 0.1% 미만으로 회복됐다. 따라서 이번 실행의 결론은 600 RPS 이상을 지속 가능한 처리량으로 확정할 수 없으며, 600 RPS 부근에서 포화 징후가 나타났다는 것이다.
---
# 수정 필요
---
---
