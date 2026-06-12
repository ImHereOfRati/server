# Notification API FCM Routing Guide

백엔드가 `POST /api/notifications` 또는 `POST /api/notifications/batch` 로 FCM 알림을 등록할 때,
알림 클릭 후 앱 내부 특정 페이지로 이동시키려면 `extraData.path` 를 함께 내려주면 된다.

## 핵심 규칙

- `notificationMethod` 는 반드시 `FCM`
- `targetId` 또는 `targetIds` 는 이메일 형식
- `extraData.path` 에 앱 내부 라우트 문자열 전달
- `path` 는 반드시 `/` 로 시작
- 현재 앱은 **페이지 이름이 아니라 path 문자열**로 라우팅함

## 단건 요청 예시

```json
{
  "notificationMethod": "FCM",
  "targetId": "user@example.com",
  "type": "DELIVERY_RESULT_NOTICE",
  "extraData": {
    "body": "집 도착 알림이 전송되었습니다.",
    "path": "/record/send-history"
  }
}
```

## 다건 요청 예시

```json
{
  "notificationMethod": "FCM",
  "targetIds": ["user1@example.com", "user2@example.com"],
  "type": "FRIEND_REQUEST_RECEIVED",
  "extraData": {
    "body": "새로운 친구 요청이 있습니다.",
    "path": "/friend/requests"
  }
}
```

## path 전달 위치

- 맞는 위치: `extraData.path`
- 권장 추가 필드: `extraData.body`

예:

```json
{
  "notificationMethod": "FCM",
  "targetId": "user@example.com",
  "type": "ARRIVAL",
  "extraData": {
    "body": "우테코에 도착했습니다.",
    "location": "우테코 (경기도 성남시)",
    "path": "/record/notifications"
  }
}
```

## 지원 path 목록

| 화면 이름 | path |
| --- | --- |
| 로그인 | `/auth` |
| 약관 동의 | `/terms-consent` |
| 사용자 권한 준비 | `/user-permission` |
| 위치 권한 가이드 | `/location-permission-guide` |
| 배터리 최적화 가이드 | `/battery-optimization-guide` |
| 지오펜스 메인 | `/geofence` |
| 지오펜스 등록 | `/geofence/message` |
| 친구 메인 | `/friend` |
| 친구 추가 | `/friend/add` |
| 받은 친구 요청 | `/friend/requests` |
| 친구 제한 목록 | `/friend/restrictions` |
| 기록 메인 | `/record` |
| 받은 알림 기록 | `/record/notifications` |
| 친구 요청 기록 | `/record/friend-requests` |
| 내 활동 기록 | `/record/send-history` |
| 설정 | `/setting` |

## NotificationType 별 권장 path

| NotificationType | 권장 path | 비고 |
| --- | --- | --- |
| `FRIEND_REQUEST_RECEIVED` | `/friend/requests` | 받은 친구 요청 확인 |
| `FRIEND_REQUEST_ACCEPTED` | `/record/notifications` | 수락 알림 확인 |
| `LOCATION_SHARE_RECEIVED` | `/record/notifications` | 위치 공유 수신 기록 확인 |
| `ARRIVAL` | `/record/notifications` | 도착 알림 수신 확인 |
| `ARRIVAL_CONFIRMATION` | `/record/notifications` | 호환성 유지용 |
| `TERMS_UPDATE_NOTICE` | `/terms-consent` | 약관 재확인 |
| `DELIVERY_RESULT_NOTICE` | `/record/send-history` | 내 발송 성공/실패/재시도 상태 확인 |

## 현재 앱 내부 기본 매핑

- 친구 요청 수신 알림: `/friend/requests`
- 위치 알림 대상자 등록 안내: `/record/notifications`
- 도착 알림 수신: `/record/notifications`
- 내 전송 결과 알림: `/record/send-history`

## 동작 메모

- foreground 에서 local notification 으로 다시 표시돼도 같은 `path` 로 이동
- background / 종료 상태에서 탭해도 같은 `path` 로 이동
- 로그인 전 사용자가 누르면 `/auth` 로 보낸 뒤 로그인 성공 후 원래 `path` 로 복귀
- `path` 가 없거나 `/` 로 시작하지 않으면 이동하지 않음

## 피해야 할 값

- 페이지 이름 문자열: `recordSendHistory`, `notificationsPage`
- `/` 없는 값: `record/send-history`
- 앱에 없는 경로

## 백엔드 체크리스트

1. `notificationMethod: "FCM"` 사용
2. `targetId` 또는 `targetIds` 에 이메일 사용
3. `type` 은 정의된 `NotificationType` 값 사용
4. 클릭 이동이 필요하면 `extraData.path` 추가
5. 본문 표시를 위해 `extraData.body` 포함 권장
