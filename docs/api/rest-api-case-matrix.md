# REST API 성공·실패 케이스 매트릭스

이 문서는 로컬 통합 테스트를 기준으로 REST Docs/OpenAPI에 포함해야 하는 API별 계약을 관리한다. 각 행의 성공 케이스와 실패 케이스는 하나 이상의 테스트에서 재현되어야 하며, 문서에는 인증 실패(401)와 별도로 도메인/입력 실패도 포함한다.

| API | 성공 케이스 | 실패 케이스 |
|---|---|---|
| POST `/api/auth` | 신규·기존·약관 대기 사용자 로그인 | 잘못된 요청(400), OIDC 만료/형식/서명/nonce/email 오류(401), 차단·탈퇴 사용자(401) |
| POST `/api/auth/refresh` | 유효 토큰 재발급 | 잘못된 형식·만료·폐기 토큰(401), 잘못된 요청(400) |
| GET `/api/agreements` | 동의 이력, 빈 이력 | 대기 사용자(403), 인증 실패(401) |
| POST `/api/agreements` | 필수·선택 약관 동의, 멱등 재동의 | 잘못된 본문(400), 약관 없음(404), 사용자 상태(403), 인증 실패(401) |
| POST `/api/agreements/renewals/{termId}` | 갱신 동의, 멱등 갱신 | 약관 없음(404), 갱신 불필요(422), 사용자 상태(403), 인증 실패(401) |
| DELETE `/api/agreements/{termId}` | 선택 약관 철회, 멱등 철회 | 약관 없음(404), 필수 약관 철회(422), 사용자 상태(403), 인증 실패(401) |
| GET `/api/terms` | 활성 약관 목록, 빈 목록 | `isActive` 오류(400/422), 인증 실패(401) |
| GET `/api/users` | 후보 검색, 빈 결과, 페이지 조회, 관계 사용자 제외 | 빈 키워드·페이지 오류(400), 인증 실패(401) |
| GET `/api/users/my` | 내 정보 조회 | 사용자 없음(404), 인증 실패(401) |
| PATCH `/api/users/my` | 닉네임 수정·멱등 수정·선택 필드 수정 | 빈 값·길이·JSON 오류(400), 인증 실패(401) |
| DELETE `/api/users/my/withdrawal` | 탈퇴 및 이벤트 발행 | 잘못된 사용자 상태(422), 사용자 없음(404), 인증 실패(401) |
| POST `/api/friends/requests` | 친구 요청 생성 | 자기 자신(400), 본문 검증(400), 중복·친구 상태(409), 차단·거절 상태(422), 인증 실패(401) |
| GET `/api/friends/requests` | SENT/RECEIVED 목록, 빈 목록, 페이지 조회 | type·페이지 오류(400), 인증 실패(401) |
| GET `/api/friends/requests/{id}` | 요청 상세 조회 | UUID·소유권·상태 오류(400), 대상 없음(404), 인증 실패(401) |
| POST `/api/friends/requests/{id}/accept` | 요청 수락 및 친구 전환 | 본인 요청·이미 처리됨(400), 대상 없음(404), 인증 실패(401) |
| POST `/api/friends/requests/{relationId}/reject` | 요청 거절 및 차단 전환 | 본인 요청·이미 처리됨(400), 대상 없음(404), 인증 실패(401) |
| DELETE `/api/friends/requests/{id}` | 받은 요청 삭제 | 소유권·상태 오류(403/400), 대상 없음(404), 인증 실패(401) |
| DELETE `/api/friends/requests/{id}/sent` | 보낸 요청 취소 | 받는 사람의 취소(403), 상태·대상 오류(400/404), 인증 실패(401) |
| GET `/api/friendships` | 친구 목록, 빈 목록, 페이지 조회 | 페이지 오류(400), 인증 실패(401) |
| GET `/api/friendships/target/{targetUserId}` | 친구 여부 true/false | UUID 오류(400), 인증 실패(401) |
| GET `/api/friendships/{id}` | 친구 상세 조회 | UUID·대상 없음(400/404), 타인 관계(403), 인증 실패(401) |
| DELETE `/api/friendships/{id}` | 친구 삭제(204) | 타인 관계(403), 대상 없음(404), 인증 실패(401) |
| PATCH `/api/friendships/{id}/alias` | 별칭 생성·변경·삭제 | 빈 값·길이(400), 친구 아님(404), 타인 관계(403), 인증 실패(401) |
| GET `/api/friends/restrictions` | 차단 목록, 빈 목록, 페이지 조회 | 페이지 오류(400), 인증 실패(401) |
| POST `/api/friends/restrictions` | 차단 생성·친구 관계 전환·멱등 처리 | 자기 자신(400), 대상 없음(404), 본문 오류(400), 인증 실패(401) |
| GET `/api/friends/restrictions/target/{targetUserId}` | 차단 여부 true/false | UUID 오류(400), 인증 실패(401) |
| DELETE `/api/friends/restrictions/blocked-users/{targetUserId}` | 차단 해제·멱등 해제 | 차단 주체 아님(403), 미차단 대상(400), UUID 오류(400), 인증 실패(401) |
| GET `/api/notifications` | 알림 목록, 빈 목록, 페이지 조회 | 페이지·size 오류(400), 인증 실패(401) |
| PATCH `/api/notifications/{id}/read` | 내 알림 읽음 처리·멱등 처리 | 타인 알림(403), 대상 없음(404), 잘못된 상태(400), 인증 실패(401) |
| POST `/api/notifications` | FCM·SMS·복수 대상·이탈 알림 접수(202) | 대상 누락·빈 목록·전송 방식 누락(400), 대상/본문 오류(400), 인증 실패(401), 비동기 전송 실패 및 DEAD 전환 |
| POST `/api/fcm-tokens` | 토큰 등록·기기 토큰 갱신(201) | 빈 토큰·기기 타입 누락·본문 오류(400), 인증 실패(401) |
| GET `/api/admin/failed-notifications` | DEAD 목록·상태 필터·빈 목록·페이지 조회 | status·페이지 오류(400), 일반 사용자(403), 인증 실패(401) |
| GET `/api/admin/failed-notifications/{id}` | 실패 알림 상세 | 대상 없음(404), 일반 사용자(403), 인증 실패(401) |
| POST `/api/admin/failed-notifications/redelivery-jobs` | 일괄 재전송 및 처리 건수·빈 결과 | count 오류(400), 일반 사용자(403), 인증 실패(401) |
| POST `/api/admin/failed-notifications/{id}/redelivery-jobs` | DEAD 알림 재전송 | 재전송 불가 상태(400), 대상 없음(404), 일반 사용자(403), 인증 실패(401) |
| DELETE `/api/admin/failed-notifications/{id}` | 실패 알림 폐기 | 상태·대상 오류(400/404), 일반 사용자(403), 인증 실패(401) |
| GET `/api/admin/friend-requests` | 관리자 목록·필터·빈 목록·페이지 조회 | 페이지 오류(400), 일반 사용자(403), 인증 실패(401) |
| DELETE `/api/admin/friend-requests/{id}` | 관리자 요청 삭제(204) | 대상 없음(404), 일반 사용자(403), 인증 실패(401) |
| GET `/api/admin/friendships` | 관리자 목록·필터·빈 목록·페이지 조회 | 페이지 오류(400), 일반 사용자(403), 인증 실패(401) |
| DELETE `/api/admin/friendships/{id}` | 관리자 친구 삭제(204) | 대상 없음(404), 일반 사용자(403), 인증 실패(401) |
| GET `/api/admin/friend-restrictions` | 관리자 목록·필터·빈 목록·페이지 조회 | 페이지 오류(400), 일반 사용자(403), 인증 실패(401) |
| DELETE `/api/admin/friend-restrictions/{id}` | 관리자 차단 삭제(204) | 대상 없음(404), 일반 사용자(403), 인증 실패(401) |
| GET `/api/admin/terms` | 전체 약관 목록·빈 목록 | 일반 사용자(403), 인증 실패(401) |
| POST `/api/admin/terms` | 약관 생성 및 버전 증가 | 필드·날짜·enum 오류(400), 일반 사용자(403), 인증 실패(401) |
| GET `/api/admin/users` | 사용자 목록·빈 목록·페이지 조회 | 페이지 오류(400), 일반 사용자(403), 인증 실패(401) |
| POST `/api/admin/users/{email}/block` | 사용자 차단 | 대상 없음(404), 상태 오류(400), 일반 사용자(403), 인증 실패(401) |
| DELETE `/api/admin/users/{email}/block` | 사용자 차단 해제 | 대상 없음(404), 상태 오류(400), 일반 사용자(403), 인증 실패(401) |
| DELETE `/api/admin/users/{email}/token` | 사용자 토큰 폐기 | 대상 없음(404), 일반 사용자(403), 인증 실패(401) |
| DELETE `/api/admin/users/{email}` | 사용자 관리자 탈퇴 | 대상 없음(404), 상태 오류(400), 일반 사용자(403), 인증 실패(401) |
| GET `/api/maps/geocode` | 주소 좌표 변환, 빈 결과, 범위 결과 | query 오류(400), Naver 502/503/504, 인증 실패(401) |
| GET `/api/maps/reverse-geocode` | 좌표 주소 변환, 빈 결과 | 좌표 오류(400), Naver 502/503/504, 인증 실패(401) |
| GET `/api/maps/local-search` | 장소 검색, 빈 결과, display/page 결과 | query·페이지 오류(400), Naver 502/503/504, 인증 실패(401) |

## 문서화 완료 조건

- 52개 operation 각각에 2xx 성공 응답이 있다.
- 각 operation에 401 외의 도메인 또는 입력 실패 응답이 하나 이상 있다.
- 성공/실패 케이스는 실제 로컬 통합 테스트에서 재현되거나, 외부 지도·비동기 전송처럼 테스트 더블로 재현할 수 없는 경우 그 경계를 케이스 설명에 명시한다.
- 영어 placeholder, 숫자만 있는 응답 설명, 이전 빌드의 stale snippet은 빌드에서 거부한다.
