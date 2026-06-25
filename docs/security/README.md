# Security 문서

ImHere 서버의 **사용자 인증, 관리자 인증, 권한 모델**을 정리한 문서 모음입니다.

---

## 핵심 판단 / 보안 요약

| 결정 | 내용 | 근거 |
|---|---|---|
| 자체 비밀번호 미사용 | Kakao/Google OIDC ID Token만 검증해 로그인/가입 처리 | [oauth.md](oauth.md) |
| Refresh Token DB 미저장 | `refresh:{email}` 형태로 앱 메모리 캐시에만 유지 | [jwt.md](jwt.md#refresh-token-저장-방식) |
| 관리자 접근 이중 분리 | Admin Web은 OTT+세션, Admin API는 별도 권한 체인 | [admin-ott.md](admin-ott.md), [jwt.md](jwt.md#securityfilterchain) |

---

## 문서 지도

| 순서 | 문서 | 범위 |
|---|---|---|
| 1 | [oauth.md](oauth.md) | Kakao/Google OIDC 검증 구조 |
| 2 | [jwt.md](jwt.md) | JWT 발급/재발급/필터 체인/권한 모델 |
| 3 | [admin-ott.md](admin-ott.md) | 관리자 OTT 로그인 흐름 |

---

## 자주 참조하는 항목

* 로그인/가입 시퀀스: [../flows/oidc-login.md](../flows/oidc-login.md)
* 회원 활성화: [../flows/oidc-signup-activation.md](../flows/oidc-signup-activation.md)
* 토큰 갱신: [../flows/token-refresh.md](../flows/token-refresh.md)
