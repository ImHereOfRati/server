# Conventions 문서

ImHere 서버의 **공통 응답/예외 처리 규약, Kotlin 구현 관례, 테스트 구조**를 정리한 문서 모음입니다.

---

## 핵심 판단 / 규약 요약

| 결정 | 내용 | 근거 |
|---|---|---|
| 응답 포맷 단일화 | 성공/실패 모두 `ApiResponse<T>`로 감쌈 | [error-handling.md](error-handling.md) |
| 예외는 도메인 코드 중심 | `throwIt()`로 HTTP 상태별 예외 타입 생성 | [error-handling.md](error-handling.md#예외-생성-방식) |
| 테스트는 우리 코드 기준 | 프레임워크 자체보다 우리 코드의 의도와 계약을 검증 | [testing.md](testing.md) |

---

## 문서 지도

| 순서 | 문서 | 범위 |
|---|---|---|
| 1 | [error-handling.md](error-handling.md) | 공통 응답 구조, 전역 예외 처리 |
| 2 | [kotlin-conventions.md](kotlin-conventions.md) | 구현 스타일, null 처리, 설정 주입 |
| 3 | [testing.md](testing.md) | 테스트 종류, 지원 코드, 저장소 관례 |

---

## 자주 참조하는 항목

* 인증 실패 응답 구조: [../security/jwt.md](../security/jwt.md)
* MQ/재시도 테스트 대상: [../flows/notification-pipeline.md](../flows/notification-pipeline.md)
