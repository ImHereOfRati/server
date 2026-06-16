# Flutter OIDC Notes

## Scope

- Kakao, Google OIDC 로그인 모두 `nonce`를 사용한다.
- 백엔드는 `/api/auth/login`, `/api/auth/registration` 요청의 `nonce`를 토큰 `nonce` 클레임과 비교한다.

## Flutter work

1. 로그인 시작 시 provider별 `nonce`를 생성한다.
2. `nonce`를 로그인 플로우가 끝날 때까지 보관한다.
3. `idToken`과 함께 `nonce`를 백엔드로 보낸다.
4. 로그인 성공 후 가입 플로우가 이어지면 동일 `nonce`를 유지한다.
5. Kakao, Google 모두 같은 요청 바디 계약을 사용한다.

## Request body

```json
{
  "provider": "KAKAO",
  "idToken": "...",
  "nonce": "..."
}
```

## Notes

- `nonce`가 없거나 일치하지 않으면 백엔드에서 거절된다.
- Google issuer는 `https://accounts.google.com`와 `accounts.google.com` 둘 다 허용된다.
