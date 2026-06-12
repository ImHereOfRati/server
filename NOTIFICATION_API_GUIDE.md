# Notification API 연결 완전 정복 가이드

## 개요

SMS, FCM 등 다양한 채널로 알림 발송. 비동기 큐 기반 (RabbitMQ), 멱등성 보장.

```
클라이언트 요청
    ↓
NotificationCommandController (202 ACCEPTED 즉시 반환)
    ↓
RabbitMQ 큐 등록
    ↓
AbstractNotificationConsumer 수신
    ↓
NotificationDispatcherService (전략 패턴)
    ├─ SMS → SmsNotificationDispatchStrategy → SolapiAdapter
    └─ FCM → FcmNotificationDispatchStrategy → FirebaseAdapter
```

## API 엔드포인트

### 1. 단건 발송

```
POST /api/notifications
Content-Type: application/json
Authorization: Bearer {token}
```

### 2. 다건 발송

```
POST /api/notifications/batch
Content-Type: application/json
Authorization: Bearer {token}
```

## 요청 형식

### NotificationMethod (Enum)

```
SMS - 문자 메시지 (핸드폰 번호 필요)
FCM - Firebase Cloud Messaging (이메일 필요)
```

### NotificationType (Enum)

```
FRIEND_REQUEST_RECEIVED    - 친구 요청 받음
FRIEND_REQUEST_ACCEPTED    - 친구 요청 수락됨
LOCATION_SHARE_RECEIVED    - 위치 공유 받음
ARRIVAL                    - 목적지 도착
ARRIVAL_CONFIRMATION       - 목적지 도착 (호환성)
TERMS_UPDATE_NOTICE        - 약관 업데이트
DELIVERY_RESULT_NOTICE     - 발송 결과
```

### 단건 요청

```json
POST /api/notifications
{
  "notificationMethod": "SMS",
  "targetId": "010-1234-5678",
  "type": "ARRIVAL",
  "extraData": {
    "body": "우테코에 도착했습니다.",
    "location": "우테코 (경기도 성남시)"
  }
}
```

### 다건 요청

```json
POST /api/notifications/batch
{
  "notificationMethod": "FCM",
  "targetIds": ["user1@example.com", "user2@example.com"],
  "type": "FRIEND_REQUEST_RECEIVED",
  "extraData": {
    "body": "새로운 친구 요청"
  }
}
```

## 필드 설명


| 필드               | 타입   | 필수 | 검증         | 설명                                              |
| ------------------ | ------ | ---- | ------------ | ------------------------------------------------- |
| notificationMethod | Enum   | O    | SMS, FCM     | 발송 채널                                         |
| targetId           | String | O    | 형식 검증    | SMS: 010-XXXX-XXXX 또는 01012345678 / FCM: 이메일 |
| type               | Enum   | O    | 유효한 type  | 알림 템플릿 (ARRIVAL, ARRIVAL_CONFIRMATION 등)   |
| extraData          | Map    | X    | -            | 추가 데이터 (body, location 등)                   |

## 응답 형식

### 성공 (202 ACCEPTED)

```json
{
  "imhereResponseCode": "SUCCESS",
  "message": "알림이 발송 큐에 등록되었습니다.",
  "data": null
}
```

상태: **202 ACCEPTED** (즉시 반환, 실제 발송은 비동기)

### 검증 실패 (400 Bad Request)

```json
{
  "imhereResponseCode": "GLOBAL-001",
  "message": "요청 바디를 읽을 수 없거나 형식이 잘못되었습니다.",
  "data": null
}
```

**가능한 원인:**

- notificationMethod 값 오류 (SMS, FCM, PHONE_NUMBER만 허용)
- type 값 오류 (위 NotificationType 목록 참고)
- 필수 필드 누락

### 형식 검증 실패 (422 Unprocessable Entity)

```json
{
  "imhereResponseCode": "GLOBAL-002",
  "message": "올바른 휴대전화 번호 형식이 아닙니다.",
  "data": null
}
```

**SMS 전화번호 형식:**

- ✅ 010-1234-5678 (하이픈 포함)
- ✅ 01012345678 (하이픈 없음, 11자리)
- ❌ 02-123-4567 (01X 아님)

**FCM 이메일 형식:**

- ✅ user@example.com
- ❌ user-example.com (@ 없음)

## 완전한 예제

### Dart/Flutter (클라이언트)

```dart
Future<void> sendSmsNotification() async {
  final response = await http.post(
    Uri.parse('https://fortuneki.site/api/notifications'),
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer $accessToken',
    },
    body: jsonEncode({
      'notificationMethod': 'SMS',
      'targetId': '010-2969-1267',  // 또는 01029691267
      'type': 'ARRIVAL',
      'extraData': {
        'body': '안녕하세요! 우테코 (경기도 성남시 수정구)에 도착했습니다.',
        'location': '우테코 (경기도 성남시 수정구)'
      },
    }),
  );

  if (response.statusCode == 202) {
    print('알림 발송 큐 등록됨');
  } else if (response.statusCode == 400) {
    print('형식 오류: ${response.body}');
  }
}

Future<void> sendFcmNotification() async {
  final response = await http.post(
    Uri.parse('https://fortuneki.site/api/notifications'),
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer $accessToken',
    },
    body: jsonEncode({
      'notificationMethod': 'FCM',
      'targetId': 'user@example.com',
      'type': 'FRIEND_REQUEST_RECEIVED',
      'extraData': {
        'body': '새로운 친구 요청이 있습니다.'
      },
    }),
  );

  if (response.statusCode == 202) {
    print('알림 발송 큐 등록됨');
  }
}
```

### cURL

```bash
# SMS 발송
curl -X POST https://fortuneki.site/api/notifications \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{
    "notificationMethod": "SMS",
    "targetId": "010-2969-1267",
    "type": "ARRIVAL",
    "extraData": {
      "body": "도착했습니다.",
      "location": "우테코"
    }
  }'

# FCM 발송 (다건)
curl -X POST https://fortuneki.site/api/notifications/batch \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{
    "notificationMethod": "FCM",
    "targetIds": ["user1@example.com", "user2@example.com"],
    "type": "FRIEND_REQUEST_RECEIVED",
    "extraData": {
      "body": "새로운 친구 요청"
    }
  }'
```

## 에러 처리

### 재시도 정책

- **202 ACCEPTED**: 성공. 실제 발송은 비동기 (수신 보장 안 함)
- **400 Bad Request**: 재시도 불가. 요청 형식 수정 필요
- **5xx**: 서버 오류. 지수 백오프로 재시도 권장

### 멱등성

- 동일한 요청 중복 시 한 번만 발송
- RabbitMQ 메시지 ID 기반 추적

## 주의사항

### 1. 비동기 발송

```
202 ACCEPTED 반환 ≠ 실제 발송 완료
실제 발송은 RabbitMQ 큐를 거쳐 처리됨 (수초 소요 가능)
```

### 2. notificationMethod와 targetId 조합

```
SMS → 전화번호 (010-XXXX-XXXX 또는 01012345678)
FCM → 이메일 주소
```

### 3. type 값은 정확히

```
❌ "ARRIVAL"만 사용
✅ 리스트에 있는 정확한 값: ARRIVAL, ARRIVAL_CONFIRMATION 등
```

### 4. extraData는 맞춤형

- SMS 발송: location 필드 포함 권장
- FCM 발송: body 필드 포함 권장
- 필수는 아님 (선택사항)

## 발송 채널별 특징

### SMS (SolapiAdapter)

- **장점**: 모든 폰에서 수신 (앱 필요 없음)
- **비용**: 건당 과금
- **속도**: 수초 내 수신
- **제약**: 문자 길이 제한
- **실패 시**: DLQ → Replay 가능

### FCM (FirebaseAdapter)

- **장점**: 앱 푸시로 리치 알림
- **비용**: 무료
- **속도**: 수초 내 수신
- **제약**: 앱 설치 필요, 백그라운드 제약
- **실패 시**: 자동 재시도

## 트러블슈팅

### 400 Bad Request 받았을 때

1. notificationMethod 값 확인 (SMS, FCM만 허용)
2. type 값 확인 (위 NotificationType 목록에서 정확한 값)
3. targetId 형식 확인
   - SMS: `010-XXXX-XXXX` 또는 `01012345678`
   - FCM: `xxx@example.com`

### 22 Unprocessable Entity 받았을 때

- targetId 형식 오류
- SMS: 전화번호 형식 재확인
- FCM: 이메일 형식 재확인

### 알림이 안 왔을 때

1. 202 받았는지 확인 (받았으면 정상)
2. 수초 대기 (RabbitMQ 처리 시간)
3. SMS: 회사 휴대폰 설정 확인
4. FCM: 앱 활성화 확인, Firebase 등록 확인

## 참고

- **API 문서**: `/api/notifications` 엔드포인트
- **소스**: `notifications` 패키지
- **상태**: 프로덕션 (2026-06)
