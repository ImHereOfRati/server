# 외부 서비스 (SaaS) 연동 가이드

ImHere 프로젝트에서 사용하는 외부 알림 서비스 및 타사 API 연동 구성을 정리한 문서입니다.

---

## 1. 알림 서비스 (Messaging)

사용자에게 위치 기반 알림 및 정보를 전달하기 위해 두 가지 채널을 사용합니다.

### 1.1 Solapi (SMS)
본인 인증 및 긴급 알림 전송을 위한 문자 서비스입니다.
- **용도**: 회원가입 인증 번호 발송, 서비스 주요 공지.
- **관리 콘솔**: [SOLAPI 대시보드](https://console.solapi.com/dashboard)
- **연동 방식**: Solapi SDK를 사용하여 HTTPS API로 호출.

### 1.2 FCM (Firebase Cloud Messaging)
앱 푸시 알림을 위한 플랫폼입니다.
- **용도**: 위치 도착/출발 알림, 친구 요청 알림.
- **관리 콘솔**: [Firebase Console](https://console.firebase.google.com/)
- **연동 방식**: Firebase Admin SDK (`imhereFirebaseKey.json`)를 통한 메시지 전송.

---

## 2. 모니터링 및 알림 (Webhooks)

시스템 장애 및 비정상 접근 시 즉각적인 대응을 위해 Discord를 활용합니다.

### 2.1 Discord Webhook
애플리케이션 내부에서 발생하는 주요 이벤트를 특정 채널로 발송합니다.
- **User Error (4xx)**: 비즈니스 예외 발생 시 전송. (입력값 오류 등)
- **Abnormal Access (403)**: 인가되지 않은 비정상적인 접근 시도 시 전송.
- **System Error (5xx)**: 서버 내부 오류 발생 시 즉각 알림.
- **Admin Login**: 어드민 OTT 로그인 시도 발생 시 전송.

---

## 3. 소셜 인증 (OAuth/OIDC)

### 3.1 Kakao OIDC
카카오 로그인을 통한 본인 인증을 수행합니다.
- **용도**: 사용자 로그인 및 프로필 정보 획득.
- **검증 방식**: 카카오에서 제공하는 공개키 목록을 조회하여 ID 토큰의 서명을 직접 검증합니다.
- **보안**: 공개키는 7일 주기로 갱신되며 Redis에 캐싱하여 사용합니다.

---

## 4. 관련 환경변수 및 보안

모든 외부 서비스 연동 키는 다음 경로에서 관리됩니다.
- **로컬/테스트**: `application-secret.yaml` (Git 제외)
- **운영**: GitHub Secrets (`FIREBASE_JSON_KEY` 등) 및 EC2 `.env`
