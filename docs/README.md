# Docs

ImHereServer 문서를 **어떤 축으로 읽어야 하는지** 정리한 인덱스입니다.

---

## 핵심 판단 / 읽는 순서

| 순서 | 영역 | 먼저 보는 이유 |
|---|---|---|
| 1 | [architecture/README.md](architecture/README.md) | 시스템 전체 구조와 모듈 경계를 먼저 잡기 위해 |
| 2 | [security/README.md](security/README.md) | 인증/인가/관리자 접근 방식을 이해하기 위해 |
| 3 | [flows/README.md](flows/README.md) | 실제 사용자 작업과 서버 처리 흐름을 보기 위해 |
| 4 | [conventions/README.md](conventions/README.md) | 에러 처리, 코딩/테스트 규약을 확인하기 위해 |
| 5 | [infra/README.md](infra/README.md) | 배포/운영 구조와 DB 스키마를 확인하기 위해 |

---

## 문서 지도

| 순서 | 문서 | 범위 |
|---|---|---|
| 1 | [architecture/README.md](architecture/README.md) | 구조, 도메인, 내부 아키텍처 |
| 2 | [security/README.md](security/README.md) | OIDC, JWT, Admin OTT |
| 3 | [flows/README.md](flows/README.md) | 인증/회원, 친구, 알림, 앱 실전 흐름 |
| 4 | [conventions/README.md](conventions/README.md) | 에러 처리, Kotlin 구현 관례, 테스트 |
| 5 | [infra/README.md](infra/README.md) | AWS, Docker, nginx, CI/CD, DB 스키마 |

---

## 프로젝트 고유 용어

* `ApiResponse<T>`: ImHere의 공통 API 응답 래퍼. 성공/실패를 같은 외형으로 감쌉니다.
* `ImHereUserDetails`: JWT claims를 Spring Security 인증 주체로 옮긴 객체입니다.
* `PENDING`: OIDC 가입은 끝났지만 약관 동의/활성화가 남은 사용자 상태입니다.
* `DLQ Replay`: Dead Letter Queue 메시지를 관리자가 원본 Exchange로 다시 발행하는 운영 기능입니다.

---

## 자주 참조하는 항목

* 인증 흐름 전체: [flows/README.md](flows/README.md)
* 데이터 구조와 ERD: [infra/db-schema.md](infra/db-schema.md)
* 배포 및 환경 변수 주입: [infra/cicd.md](infra/cicd.md)
