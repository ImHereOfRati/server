# Flows

주요 기능의 시퀀스 다이어그램. Admin OTT 로그인 흐름은 [security.md](./security.md#admin-ott-one-time-token-로그인)에 있다.

## OIDC 로그인

`/api/auth/login`은 기존 사용자만 로그인시킨다 — 신규 유저를 만들지 않는다.

```mermaid
sequenceDiagram
    participant App as Flutter App
    participant Backend as ImHere Backend
    participant Provider as Kakao/Google
    participant Redis
    participant DB

    App->>Provider: OIDC 로그인 (openid scope + nonce)
    Provider-->>App: ID Token (kid, sub, email, nonce 포함)

    App->>Backend: POST /api/auth/login\n{ provider, idToken, nonce }

    Backend->>Redis: JWKS 캐시 조회 (cacheKey)
    alt 캐시 없음
        Backend->>Provider: GET /.well-known/jwks.json
        Provider-->>Backend: 공개 키 세트
        Backend->>Redis: JWKS 캐시 저장
    end

    Backend->>Backend: ID Token kid 추출 → JWKS에서 키 선택 → 서명 검증 + nonce 확인

    Backend->>DB: email로 사용자 조회
    alt 사용자 없음
        Backend-->>App: 404 AUTH-300 (USER_NOT_REGISTER)
    else status = BLOCKED
        Backend-->>App: 401 AUTH-106 (USER_LOCKED)
    else status = WITHDRAWN
        Backend-->>App: 401 AUTH-108 (USER_WITHDRAWN)
    else status = PENDING 또는 ACTIVE
        Backend->>Backend: JWT 발급 (현재 status 클레임 그대로)
        Backend-->>App: 200 { accessToken, refreshToken, userStatus }
    end
```

## OIDC 회원가입 + 회원 활성화

```mermaid
sequenceDiagram
    participant App as Flutter App
    participant Server as ImHere Server
    participant OIDC as Kakao/Google OIDC
    participant DB as DB

    App->>OIDC: OIDC 로그인 (nonce 포함)
    OIDC-->>App: ID Token 반환

    App->>Server: POST /api/auth/registration\n{provider, idToken, nonce}
    Server->>OIDC: JWKS 공개키 요청 (kid 기반)
    OIDC-->>Server: 공개키 반환
    Server->>Server: ID Token 서명 검증 + payload 검증 (iss/aud/nonce/exp)

    alt 중복 이메일 (BLOCKED/WITHDRAWN)
        Server-->>App: 409 / 403 에러
    else 신규 유저
        Server->>DB: User 저장 (status=PENDING)
        Server->>Server: JWT 발급 (PENDING 클레임)
        Server-->>App: 200 {accessToken, refreshToken}
    end
```

```mermaid
sequenceDiagram
    participant App as Flutter App
    participant Server as ImHere Server
    participant DB as DB

    App->>Server: POST /api/auth/activation\nAuthorization: Bearer {PENDING JWT}\n{consents: [{id, isAgreed}]}
    Server->>Server: JWT 검증 (PENDING 상태 확인)
    Server->>DB: 활성 약관 목록 조회

    alt 필수 약관 미동의
        Server-->>App: 400 OBLIGATORY_TERM_NOT_AGREED
    else 모든 필수 동의
        Server->>DB: user_agreement 저장
        Server->>DB: User status ACTIVE 업데이트
        Server->>Server: 새 JWT 발급 (ACTIVE 클레임)
        Server-->>App: 200 {accessToken, refreshToken}
    end
```

## JWT 토큰 갱신

```mermaid
sequenceDiagram
    participant App as Flutter App
    participant Server as ImHere Server
    participant Redis

    App->>Server: POST /api/auth/refresh\n{refreshToken}
    Server->>Server: Refresh Token 서명/만료 검증

    alt Refresh Token 만료 또는 위조
        Server-->>App: 401 IMHERE_INVALID_TOKEN
    else 유효한 Refresh Token
        Server->>Redis: refresh:{email} 조회
        alt 저장된 값과 불일치 (이미 회전됨/탈취)
            Server-->>App: 401 IMHERE_INVALID_TOKEN
        else 일치
            Server->>Server: 새 Access Token + Refresh Token 발급
            Server->>Redis: refresh:{email} 값 교체 (rotate)
            Server-->>App: 200 {accessToken, refreshToken}
        end
    end
```

## 친구 요청 & 수락 / 거절

```mermaid
sequenceDiagram
    participant A as 요청자 (App)
    participant Server as ImHere Server
    participant DB as DB

    A->>Server: POST /api/friends/requests\n{receiverId, message}
    Server->>DB: 요청자 / 수신자 조회
    Server->>DB: 제한(restriction) 확인 (requester↔receiver)
    alt 제한 존재 (REJECT/BLOCK)
        Server-->>A: 422 FRIEND_REQUEST_UNPROCESSABLE
    else 제한 없음
        Server->>DB: 중복 요청 / 이미 친구 여부 확인
        alt 중복 또는 이미 친구
            Server-->>A: 409 FRIEND_REQUEST_ALREADY_SENT / ALREADY_FRIEND
        else
            Server->>DB: FriendRequest 저장
            Server-->>A: 201 {friendRequestId}
        end
    end
```

```mermaid
sequenceDiagram
    participant B as 수신자 (App)
    participant Server as ImHere Server
    participant DB as DB

    B->>Server: POST /api/friends/requests/{id}/accept
    Server->>DB: FriendRequest 조회 + 수신자 이메일 검증
    alt 수신자 불일치
        Server-->>B: 403 FRIENDSHIP_REQUEST_RECEIVER_MISS_MATCH
    else 일치
        Server->>DB: Friendship(requester→receiver), Friendship(receiver→requester) 저장
        Server->>DB: FriendRequest 삭제
        Server-->>B: 200 {friendshipId}
    end
```

```mermaid
sequenceDiagram
    participant B as 수신자 (App)
    participant Server as ImHere Server
    participant DB as DB

    B->>Server: POST /api/friends/requests/{id}/reject
    Server->>DB: FriendRequest 조회 + 수신자 일치 검증
    Server->>DB: FriendRestriction(type=REJECT, expiredAt=+30일) 저장
    Server->>DB: FriendRequest 삭제
    Server-->>B: 200 {restrictionId}
```

## 친구 차단 & 제한

```mermaid
sequenceDiagram
    participant A as 차단자 (App)
    participant Server as ImHere Server
    participant DB as DB

    A->>Server: POST /api/friendships/{id}/block
    Server->>DB: Friendship 조회 + owner 이메일 검증
    alt 주인 불일치
        Server-->>A: 403 FRIEND_RELATIONSHIP_OWNER_MISS_MATCH
    else 일치
        Server->>DB: FriendRestriction(type=BLOCK, expiredAt=null) 저장
        Server->>DB: Friendship(owner→friend), Friendship(friend→owner) 삭제
        Server-->>A: 200
    end
```

```mermaid
sequenceDiagram
    participant A as 제한자 (App)
    participant Server as ImHere Server
    participant DB as DB

    A->>Server: POST /api/friends/restrictions\n{targetUserId}
    Server->>DB: 제한자/대상 User 조회
    Server->>DB: Friendship 삭제 (restrictor↔restricted)
    Server->>DB: FriendRequest 삭제 (between)
    Server->>DB: FriendRestriction(BLOCK) 저장
    Server-->>A: 201 {restrictionId}
```

```mermaid
sequenceDiagram
    participant A as 제한자 (App)
    participant Server as ImHere Server
    participant DB as DB

    A->>Server: DELETE /api/friends/restrictions/blocked-users/{restrictedId}
    Server->>DB: BLOCK restriction 조회 (restrictor 이메일 + restrictedId)
    alt 없음
        Server-->>A: 404
    else 존재
        Server->>DB: FriendRestriction 삭제
        Server-->>A: 200
    end
```

| 타입 | 만료 | 재요청 가능 여부 |
|---|---|---|
| REJECT | now + 30일 | 30일 후 다시 가능 |
| BLOCK | null(영구) | 해제하지 않는 한 불가 |

## FCM 토큰 등록 & 실패 체인

```mermaid
sequenceDiagram
    participant App as Flutter App
    participant Server as ImHere Server
    participant DB as DB

    App->>Server: POST /api/fcm-tokens\nAuthorization: Bearer {JWT}\n{fcmToken, deviceType}
    Server->>Server: JWT 인증 → email 주체 확인
    Server->>DB: 현재 토큰 조회 (email 기준)
    alt 기존 토큰 없음
        Server->>DB: FcmToken 신규 저장
    else 기존 토큰 있음
        Server->>DB: FcmToken 갱신 (token 교체)
    end
    Server-->>App: 200
```

```mermaid
sequenceDiagram
    participant Consumer as RabbitMQ Consumer
    participant FCMService as FCMNotificationService
    participant FCM as Firebase FCM
    participant DB as DB

    Consumer->>FCMService: send(receiverEmail, ...)
    FCMService->>DB: FCM Token 조회 (email)
    alt 토큰 없음
        FCMService-->>Consumer: NotFoundException
    else 토큰 있음
        FCMService->>FCM: sendMessage(token, title, body, data)
        alt unregistered 에러
            FCM-->>FCMService: unregistered
            FCMService->>DB: FcmToken 삭제
            FCMService-->>Consumer: 정상 완료 (이력 저장 안 함)
        else 기타 에러
            FCM-->>FCMService: 에러
            FCMService-->>Consumer: 예외 전파 → NACK → DLQ
        else 성공
            FCM-->>FCMService: 성공
            FCMService->>DB: NotificationHistory 저장
            FCMService-->>Consumer: 완료
        end
    end
```

## 알림 발송 흐름 (MQ In → Out → DLQ 재시도)

```mermaid
sequenceDiagram
    participant API as ImHere API
    participant Exchange as imhere.noti.topic
    participant Queue as noti.queue.*
    participant Consumer as NotificationConsumer
    participant Redis
    participant FCM as Firebase FCM
    participant DLX as imhere.noti.dlx
    participant DLQ as noti.queue.*.dlq

    API->>Exchange: 메시지 발행 (routing key + messageId)
    Exchange->>Queue: 라우팅
    Queue->>Consumer: 메시지 전달

    Consumer->>Redis: messageId 중복 확인 (TTL 48h)
    alt 중복 메시지
        Consumer-->>Queue: ACK (스킵)
    else 신규 메시지
        loop 재시도 (max 3회, 1s→2s→4s→8s, multiplier 2.0)
            Consumer->>FCM: 토큰 조회 + 알림 전송
            alt 성공
                Consumer->>Redis: messageId 기록
                Consumer-->>Queue: ACK
            else 실패
                Consumer-->>Queue: NACK (requeue=false)
            end
        end
        Queue->>DLX: 최대 재시도 초과 시 DLX로 라우팅
        DLX->>DLQ: DLQ에 메시지 저장
    end
```

## RabbitMQ DLQ 재시도 & Admin Replay

```mermaid
sequenceDiagram
    participant Admin as 어드민
    participant Server as ImHere Server
    participant DLQ as DLQ
    participant Exchange as imhere.noti.topic
    participant Consumer as Consumer

    Admin->>Server: GET /api/admin/dead-letter-queues — DLQ 메시지 수 조회
    Server->>DLQ: getQueueProperties()
    DLQ-->>Server: messageCount, consumerCount
    Server-->>Admin: DlqQueueInfoResponse

    Admin->>Server: POST /api/admin/dead-letter-queues/{queueName}/replay-jobs?count=N
    loop N개 메시지
        Server->>DLQ: receive()
        DLQ-->>Server: 메시지
        Server->>Exchange: send(originalRoutingKey, message)
        Exchange->>Consumer: 재실행
    end
    Server-->>Admin: DlqReplayResponse {replayed: N}
```

| Queue | DLX | DLQ |
|---|---|---|
| `noti.queue.friend` | `imhere.noti.dlx` | `noti.queue.friend.dlq` |
| `noti.queue.service` | `imhere.noti.dlx` | `noti.queue.service.dlq` |

재시도: 최대 3회, 1s→2s→4s→8s(multiplier 2.0). `setDefaultRequeueRejected(false)` + `RejectAndDontRequeueRecoverer` — 실패 시 큐에 다시 넣지 않고 DLQ로 보낸다.
