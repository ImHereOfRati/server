# Architecture 문서

ImHere 서버를 **어떤 구조로 나누었고, 왜 그런 선택을 했는지** 정리한 문서 모음입니다.

---

## 핵심 판단 / 설계 요약

| 결정 | 내용 | 근거 |
|---|---|---|
| 위치 판정은 클라이언트 책임 | 서버는 geofence 판정 대신 인증, 관계, 알림 전달에 집중 | [architecture.md](architecture.md#왜-이렇게-설계했는가) |
| 모듈별 아키텍처 혼합 | `auth`/`notifications`는 포트-어댑터, `user`/`terms`는 MVC, `friends`는 경량 분리 구조 | [internal-architecture.md](internal-architecture.md#왜-혼합-구조를-택했는가) |
| 데이터 구조는 infra 문서로 단일화 | ERD/DDL은 `docs/infra/db-schema.md`를 정본으로 사용 | [../infra/db-schema.md](../infra/db-schema.md) |

---

## 문서 지도

| 순서 | 문서 | 범위 |
|---|---|---|
| 1 | [architecture.md](architecture.md) | 런타임/배포 토폴로지, 요청 경로 |
| 2 | [domain.md](domain.md) | 핵심 도메인 규칙, 상태성, 모듈 경계 |
| 3 | [internal-architecture.md](internal-architecture.md) | 패키지 구조, 아키텍처 스타일 선택 기준 |
| 4 | [../infra/db-schema.md](../infra/db-schema.md) | ERD, DDL |

---

## 자주 참조하는 항목

* 인증/권한 구조: [../security/jwt.md](../security/jwt.md)
* 앱/서버 실전 흐름: [../flows/README.md](../flows/README.md)
* 배포 구조: [../infra/README.md](../infra/README.md)
