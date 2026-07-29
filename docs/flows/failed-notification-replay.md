# 실패 알림 조회와 재발송

`notification.status = DEAD`를 운영 단위로 사용한다. 운영자는 큐 이름이 아니라 실제 수신 대상, 알림 종류, 실패 사유와 시도 횟수를 본다.

## 관리자 경로

| 메서드 | 경로 | 동작 |
|---|---|---|
| GET | `/api/admin/failed-notifications?status=DEAD` | 실패 알림 목록 |
| GET | `/api/admin/failed-notifications/{id}` | 실패 알림 상세 |
| POST | `/api/admin/failed-notifications/{id}/redelivery-jobs` | 단건 재발송 |
| POST | `/api/admin/failed-notifications/redelivery-jobs?count=N` | 최대 N건 일괄 재발송 |
| DELETE | `/api/admin/failed-notifications/{id}` | 실패 기록 폐기 |
| GET | `/admin/failed-notifications` | 관리자 화면 |

모든 API와 페이지는 `ROLE_ADMIN`만 접근할 수 있다.

## 재발송 전이

```text
DEAD -- Notification.retry() --> PENDING -- 외부 채널 발송 --> SENT
                                      └── 실패 누적 ──> FAILED ──> DEAD
```

- `Notification.retry()`가 DEAD가 아닌 상태를 거부한다.
- 재발송은 기존 알림 ID와 payload를 그대로 사용하며 시도 횟수와 마지막 오류를 초기화한다.
- 재발송 중 일시적 FCM 오류에도 최초 발송과 같은 Spring Retry 정책을 적용한다.
- 이벤트 자체가 미완료인 경우는 Spring Modulith actuator의 event publication 지표로 확인한다.

## 운영 확인

1. `/admin/failed-notifications`에서 대상과 마지막 오류를 확인한다.
2. 단건 재발송 후 `SENT` 전이를 확인한다.
3. 다수 장애면 `count`를 작게 지정해 일괄 재발송하고 외부 채널 지표를 관찰한다.
4. 수신자가 탈퇴했거나 더 이상 보낼 필요가 없는 실패 기록만 폐기한다.

## 관련 코드

- `FailedNotificationAdminController`
- `FailedNotificationAdminPageController`
- `FailedNotificationAdminService`
- `NotificationDeliveryService.redeliver`
- `Notification.retry`
