# API Specification

## 원칙: 코드가 진실의 원천

엔드포인트별 요청/응답/에러 예시를 이 문서에 손으로 베껴 쓰지 않는다 — 베끼는 순간부터 코드와 어긋나기 시작한다. ImHere는 Spring REST Docs로 테스트에서 API 문서를 자동 생성한다:

- 소스: `src/main/resources/static/docs/openapi3.yaml` (OpenAPI 3.0, 운영 서버에 `/docs`로 배포됨)
- 생성 방식: 통합 테스트(`*IntegrationTest.kt`)가 통과할 때마다 RestDocs가 실제 요청/응답을 캡처해서 갱신한다. 즉 **테스트가 곧 문서**다 — 문서가 코드와 어긋나면 그건 테스트가 그 케이스를 안 짚었다는 뜻이다.
- 이 문서는 그 OpenAPI 파일이 다루지 않는 "엔드포인트 그룹 지도"와 "공통 규칙"만 보완한다.

## 엔드포인트 그룹

| 그룹 | 베이스 경로 | 인증 | 비고 |
|---|---|---|---|
| Auth | `/api/auth/*` | 없음(permitAll) | login/registration/refresh/activation |
| Users | `/api/users/*` | JWT | 본인 정보(`/my`) |
| Friends | `/api/friends/requests`, `/api/friends/restrictions` | JWT | 요청/차단·거절 |
| Friendships | `/api/friendships/{id}` | JWT | 별칭 변경, 차단, 삭제 |
| Notifications | `/api/notifications`, `/api/notifications/batch`, `/api/fcm-tokens` | JWT | 알림 조회/읽음, FCM 토큰 등록 |
| Terms | `/api/terms` | 없음/JWT | 약관 조회·동의 |
| Admin (DLQ) | `/api/admin/dead-letter-queues/*` | ROLE_ADMIN | DLQ 조회/재처리 |
| Admin (OTT) | `/admin/ott/request`, `/admin/ott/verify` | IP 허용목록 + 발급횟수 제한 | [security.md](./security.md) 참고 |

## 공통 규칙

- 모든 응답은 `ApiResponse<T>{imhereResponseCode, message, data}` 한 가지 모양이다. 에러 코드 카탈로그는 [error-handling.md](./error-handling.md).
- 인증이 필요한 요청은 `Authorization: Bearer {accessToken}` 헤더를 쓴다.
- 도메인 흐름(시퀀스 다이어그램)은 [flows.md](./flows.md)와 [domain.md](./domain.md)를 본다.
