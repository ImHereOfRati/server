# 도메인 명세

기준 커밋: `5f205036` (main) + 미커밋 정리 작업

이 문서는 `user`, `terms`, `agreement`, `friends`, `notifications` 모듈의 도메인 규칙을 다룬다. `maps`는 이 문서의 범위가 아니다.

---

# User

- `ImHere`의 사용자
    - `Google` 또는 `Kakao`에서 정보를 제공받는 방식으로 회원가입할 수 있다.
    - 회원가입 직후에는 `PENDING` 상태가 된다.
    - 현재 필수 약관에 모두 동의해야 서비스를 이용할 수 있다.

### 상태값

- `PENDING`: 가입은 되었지만 아직 현재 필수 약관에 모두 동의하지 않은 상태
- `ACTIVE`: 서비스를 정상적으로 이용할 수 있는 상태
- `WITHDRAWN`: 탈퇴한 상태
- `BLOCKED`: 관리자에 의해 이용이 제한된 상태

### 상태 전이

- `PENDING → ACTIVE`: 현재 필수 약관에 모두 동의하면 활성화된다.
    - `AgreementService`가 필수 약관 충족을 확인하면 `user::api`의 `UserActivationContract.activateIfPending()`을 호출한다.
    - 사용자가 여전히 `PENDING` 상태일 때만 `ACTIVE`로 변경한다.
    - 그 외 상태에서는 아무 것도 변경하지 않고 현재 상태를 그대로 반환한다.
- `ACTIVE → WITHDRAWN`: 사용자가 탈퇴하면 탈퇴 상태가 된다.
- `ACTIVE → BLOCKED`: 관리자가 차단하면 이용이 제한된다.
- `BLOCKED → ACTIVE`: 관리자가 차단을 해제하면 다시 이용할 수 있다.
- `BLOCKED → WITHDRAWN`: 차단된 사용자도 탈퇴할 수 있다.

### 비즈니스 규칙

- 필수 약관에 동의하지 않은 사용자는 기본적으로 서비스를 이용할 수 없다.
- `@AllowPendingUser`가 선언된 엔드포인트는 `PENDING` 사용자도 접근할 수 있다.
    - `ActiveUserAuthorizationManager`가 메서드 또는 클래스의 애노테이션을 확인해 판단한다.
    - `ACTIVE` 권한 또는 `ROLE_ADMIN`이 있으면 애노테이션 없이도 통과한다.
    - `permitAll` 경로에 해당하는 요청은 애노테이션 여부와 무관하게 통과한다.
- 로그인 자체는 `PENDING` 사용자도 할 수 있다. `AuthService`가 막는 상태는 `BLOCKED`와 `WITHDRAWN`뿐이다.
- 차단된 사용자는 로그인할 수 없다.
- 탈퇴한 사용자는 로그인할 수 없다.
- 닉네임 변경 API는 공백을 허용하지 않으며 2글자 이상 7글자 이하만 받는다.
- 닉네임 길이는 `User` 도메인 불변식으로 강제하지 않고 `NicknameUpdateRequest`에서 검증한다.
- 닉네임은 중복을 허용한다. 유일성 제약이 없으며 중복 검사도 하지 않는다.
- 활성화 요청은 멱등하다. 같은 사용자에 대해 여러 번 호출해도 결과가 같다.
- `ACTIVE` 상태의 사용자만 차단할 수 있다.
- `BLOCKED` 상태의 사용자만 차단 해제할 수 있다.
- `ACTIVE` 또는 `BLOCKED` 상태의 사용자만 탈퇴할 수 있다.
- `WITHDRAWN`은 최종 상태이며 이후 다른 상태로 변경할 수 없다.
- 이메일 중복 판정은 `User`가 아니라 무상태 정책 객체인 `EmailRegistrationPolicy`가 담당한다.
- 사용자를 차단하거나 탈퇴시키면 `UserForceLogoutEvent`를 발행한다. 차단 해제는 발행하지 않는다.
- 관리자는 사용자 상태를 변경하지 않고 강제 로그아웃 이벤트만 요청할 수도 있다.
- `UserForceLogoutEvent`는 `@ApplicationModuleListener`를 통해 원본 트랜잭션 커밋 후 비동기로 처리한다.
- user 모듈의 `UserForceLogoutEventListener`가 이벤트를 처리하고 refresh token version을 증가시켜 기존 refresh token을 무효화한다.
    - 대상 사용자를 찾지 못하면 아무 것도 하지 않고 종료한다.
- 재발급 시 토큰에 실린 `refreshTokenVersion`과 사용자의 현재 버전이 일치해야 한다. 판정은 `RefreshTokenVersionPolicy`가 한다.
- Event Publication Registry가 비동기 이벤트의 처리 상태를 기록한다.
    - `spring.modulith.events.completion-mode: delete` — 성공한 이벤트 발행 기록은 삭제한다.
    - `spring.modulith.events.republish-outstanding-events-on-restart: true` — 미완료 이벤트는 애플리케이션 재시작 시 다시 발행한다.
- 현재 Registry를 경유하는 이벤트는 `UserForceLogoutEvent` 하나뿐이다.
- 사용자 조회, 등록, 프로필 변경, 상태 전이 책임은 다음과 같이 분리한다.
    - `UserQueryService`: ID 조회, 이메일 nullable 조회, ID 다건 조회, 전체 사용자 Slice 조회, 키워드 검색
    - `UserRegistrationService`: 신규 등록과 이메일 중복 판정
    - `UserProfileService`: 닉네임 변경
    - `UserLifecycleService`: 활성화, 차단, 차단 해제, 탈퇴, 강제 로그아웃 이벤트 발행
- 다른 모듈은 user 서비스 구현체에 직접 의존하지 않는다.
    - `user::api` Named Interface로 공개한 조회·등록·활성화 계약과 `UserResult`만 사용한다.
    - `friends` 모듈은 `UserLookupContract`를 통해 필요한 사용자만 조회한다.
    - `auth` 모듈은 `UserLookupContract`와 `UserRegistrationContract`를 사용한다.
    - `agreement` 모듈은 `UserActivationContract`를 사용한다.
    - user 저장소 구현은 다른 모듈에 공개하지 않는다.
- `user::api` 계약은 다음 셋이다.
    - `UserLookupContract`: `findById`, `findByEmailOrNull`, `findAllByIds`, `searchActiveByKeyword`
    - `UserRegistrationContract`: `register`
    - `UserActivationContract`: `activateIfPending`
- `user::domain` Named Interface로는 `UserStatus`, `UserRole`, `OAuth2Provider`만 공개한다.
- user 영속성은 하나의 구체 `UserRepository`가 조정한다.
    - `SpringDataUserRepository`는 기본 CRUD, 이메일 조회, 탈퇴 사용자를 제외한 Slice 조회를 담당한다.
    - `SpringQueryDSLUserRepository`는 키워드 검색만 담당한다.
- 사용자 검색은 `ACTIVE` 사용자만 대상으로 한다.
    - 검색어에 `@`가 있으면 이메일 완전 일치, 없으면 닉네임 완전 일치로 찾는다. 부분 일치 검색은 지원하지 않는다.
    - 결과에서 누구를 뺄지는 user가 정하지 않는다. `searchActiveByKeyword`가 받은 `excludedUserIds`를 뺄 뿐이다.
    - 제외 대상을 계산하는 쪽은 관계를 소유한 friends다. [FriendRelation](#friendrelation)의 친구 후보 검색을 본다.

### 엔드포인트

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/users/my` | 본인 정보 조회 |
| PATCH | `/api/users/my` | 닉네임 변경 |
| DELETE | `/api/users/my/withdrawal` | 탈퇴 |
| GET | `/api/admin/users` | 사용자 목록 조회 |
| POST | `/api/admin/users/{email}/block` | 차단 |
| DELETE | `/api/admin/users/{email}/block` | 차단 해제 |
| DELETE | `/api/admin/users/{email}/token` | 강제 로그아웃만 요청 |
| DELETE | `/api/admin/users/{email}` | 강제 탈퇴 |

### 예외 상황

- `WITHDRAW_USER`: 이미 탈퇴한 사용자의 상태를 다시 변경하려는 경우 (`USER-002` / 400)
- `ONLY_ACTIVE_USER_CAN_BE_BLOCKED`: `ACTIVE` 상태가 아닌 사용자를 차단하려는 경우 (`USER-003` / 400)
- `ONLY_BLOCKED_USER_CAN_BE_UNBLOCKED`: `BLOCKED` 상태가 아닌 사용자의 차단을 해제하려는 경우 (`USER-004` / 400)
- `ONLY_ACTIVE_OR_BLOCKED_USER_CAN_WITHDRAW`: `ACTIVE` 또는 `BLOCKED` 상태가 아닌 사용자를 탈퇴시키려는 경우 (`USER-005` / 400)
- `USER_NOT_FOUND`: 요청한 사용자가 존재하지 않는 경우 (`USER-300` / 404)
- `DUPLICATE_EMAIL`: 이미 사용 중인 이메일로 가입하려는 경우 (`USER-500` / 409)
- `ONLY_PENDING_CAN_BE_ACTIVE_USER`: `PENDING` 상태가 아닌 `User`에 `activate()`를 호출하는 경우 (`USER-001` / 400)
- 공통 입력값 오류: 닉네임이 공백이거나 2글자 미만 또는 7글자 초과인 경우

> 사용자 상태 전이는 `User` 객체가 직접 검사한다.
>
> 닉네임 길이는 사용자 상태와 달리 도메인 규칙으로 강제하지 않는다. 현재 제한은 닉네임 변경 API의 입력 정책이므로 다른 코드가 `User.updateNickname()`을 직접 호출할 때는 적용되지 않는다.
>
> `ONLY_PENDING_CAN_BE_ACTIVE_USER`는 `User.activate()`를 보호하는 도메인 불변식이다. 현재 유일한 호출 경로인 `activateIfPending()`이 호출 전에 `PENDING` 여부를 확인하고 아니면 조기 반환하므로, 이 예외는 API 응답으로 나오지 않는다.

---

# Term

- `ImHere` 서비스에서 사용하는 약관
    - 관리자는 새로운 약관을 등록할 수 있다.
    - 약관은 종류별로 독립적인 버전을 가진다.
    - 약관에는 제목, 내용, 시행 일시, 필수 동의 여부가 포함된다.
    - 시행 일시가 지난 약관 중 종류별 최신 버전만 현재 약관으로 제공한다.

### 약관 종류

- `SERVICE`: 서비스 이용약관
- `PRIVACY`: 개인정보 처리방침
- `LOCATION`: 위치정보 이용약관
- `MARKETING`: 마케팅 정보 수신 동의

### 현재 약관 판정

약관은 상태 필드를 갖지 않는다. 어떤 약관이 "현재 약관"인지는 조회 시점에 `effectiveDate`, `type`, `version`으로 계산한다.

- 현재 약관 집합은 다음 두 조건을 모두 만족하는 약관이다.
    - `effectiveDate`가 조회 시각 이하이다.
    - 같은 `type` 안에서 `version`이 가장 높다.
- 위 조건을 만족하지 않는 약관은 현재 약관이 아니다. 시행 전이든 상위 버전에 밀렸든 구분하지 않는다.

> 별도의 상태 enum이나 상태 전이 로직은 두지 않는다. 판정은 "현재 약관인가 아닌가" 이분법이며, `TermService.findEffectiveTerms()` 한 곳에서만 계산한다.

### 비즈니스 규칙

- 동일한 약관 종류와 버전의 조합은 중복될 수 없다. DB의 `uk_terms_type_version` 제약으로 강제한다.
- 새로운 약관의 버전은 같은 종류의 최신 버전보다 1 증가한다.
- 같은 종류의 약관이 없으면 버전 1부터 시작한다.
- 시행 일시가 현재 시각보다 미래인 약관은 현재 약관으로 제공할 수 없다.
- 시행된 약관 중 종류별로 가장 높은 버전만 현재 약관으로 제공한다.
- `isRequired`가 `true`인 약관은 필수 약관이다.
- 사용자는 모든 현재 필수 약관에 동의해야 서비스를 이용할 수 있다.
- 선택 약관은 동의하지 않아도 서비스를 이용할 수 있다.
- `TermCatalog`는 agreement 모듈에 필요한 최소 약관 정보인 `TermFact`를 제공한다.
    - `findEffectiveTermFacts()`: 현재 약관 전체
    - `findTermFacts(ids)`: 지정한 약관 식별자 집합
    - `TermFact`는 `id`, `version`, `type`, `isRequired`만 담는다. 제목과 내용은 노출하지 않는다.
- `TermService`가 `TermCatalog`를 구현한다.
- terms 모듈은 agreement 모듈을 알지 못한다.
- terms 모듈은 전용 도메인 예외를 두지 않는다. 약관 조회 오류는 공통 `CommonErrorCode`로, 동의 흐름 오류는 `AgreementException`으로 처리한다.

### 엔드포인트

| 메서드 | 경로 | PENDING 허용 | 설명 |
| --- | --- | --- | --- |
| GET | `/api/terms?isActive=true` | **예** | 현재 약관 목록 조회 |
| GET | `/api/admin/terms` | 아니오 | 전체 약관 조회 |
| POST | `/api/admin/terms` | 아니오 | 약관 등록 |

### 예외 상황

- 공통 `UNPROCESSABLE_ENTITY`: `isActive=false`로 현재 약관 조회를 요청하는 경우
    - `"비활성 약관은 조회할 수 없습니다."` 메시지를 반환한다.
- 약관 동의 과정에서 요청한 약관이 존재하지 않거나 현재 유효하지 않은 경우는 agreement 모듈이 `AgreementException.TERM_NOT_FOUND`로 처리한다.

> `TermException`, `OBLIGATORY_TERM_NOT_AGREED`, `NON_ACTIVE_TERM_NOT_ALLOWED`는 모두 현재 코드에 존재하지 않는다.

---

# Agreement

- `ImHere` 서비스에서 사용자의 약관 동의와 철회 이력을 관리한다.
    - 사용자는 현재 유효한 약관에 동의할 수 있다.
    - 선택 약관에 대한 동의를 철회할 수 있다.
    - 약관이 갱신되면 이전 버전의 동의 여부를 바탕으로 갱신 약관에 동의할 수 있다.
    - 동의 상태는 덮어쓰지 않고 변경 이력을 순서대로 저장한다.
    - 사용자는 자신의 전체 약관 동의 이력을 조회할 수 있다.

### 동의 상태

- `CONSENT`: 사용자가 약관에 동의한 상태
- `WITHDRAW`: 사용자가 기존 약관 동의를 철회한 상태

> 현재 상태를 별도로 저장하지 않고 사용자와 약관별로 가장 최근에 기록된 동의 이력을 기준으로 결정한다.
>
> 동의 및 철회 이력은 발생 시각과 함께 보존한다.

### 상태 전이

- `미동의 → CONSENT`: 사용자가 약관에 동의하면 동의 이력을 추가한다.
- `CONSENT → WITHDRAW`: 사용자가 기존 동의를 철회하면 철회 이력을 추가한다.
- `WITHDRAW → CONSENT`: 철회한 약관에 다시 동의하면 새로운 동의 이력을 추가한다.
- `CONSENT → CONSENT`: 이미 동의한 약관에 다시 동의하면 새로운 이력을 추가하지 않는다.
- `WITHDRAW → WITHDRAW`: 이미 철회한 약관을 다시 철회하면 새로운 이력을 추가하지 않는다.
- `미동의 → WITHDRAW`: 동의한 이력이 없는 약관을 철회하면 새로운 이력을 추가하지 않는다.

> 전이 판정은 `AgreementStatus.next()` 한 곳에 모여 있으며, 이력을 추가하지 않아야 하는 경우 `null`을 반환한다.

### 비즈니스 규칙

- 사용자는 현재 유효한 약관에 대해서만 동의할 수 있다.
- 동일한 상태에 대한 반복 요청은 새로운 이력을 생성하지 않는다.
- 한 요청에 같은 약관이 여러 번 포함되면 마지막 요청을 적용한다.
- 동의와 철회 이력은 삭제하거나 덮어쓰지 않고 발생 순서대로 보존한다.
- 모든 현재 필수 약관이 `CONSENT` 상태여야 필수 약관 동의가 완료된 것으로 판단한다.
- 필수 약관 동의가 완료되면 `AgreementService`가 `UserActivationContract.activateIfPending()`을 호출해 사용자 활성화를 요청한다.
- 상태 변경 이력이 없더라도 최종 상태가 필수 동의 완료라면 활성화 요청을 다시 보낸다.
- 반복된 활성화 요청은 user 모듈의 `activateIfPending()`이 멱등하게 처리한다.
- agreement 모듈은 user 모듈이 `user::api`로 공개한 계약에만 의존한다. 서비스 구현체나 저장소는 참조하지 않는다.
- 필수 약관 동의가 완료되지 않아도 선택 약관의 동의 여부와 변경 이력은 처리할 수 있다.
- 명시적인 약관 철회 기능으로는 선택 약관만 철회할 수 있다.
- 필수 약관은 명시적인 철회 기능으로 철회할 수 없다.
- 갱신 약관에 동의하려면 같은 종류의 이전 버전 약관에 동의한 이력이 있어야 한다.
- 갱신 약관에 동의해도 이전 버전의 동의 이력은 유지한다.
- 이미 동의한 갱신 약관에 다시 동의하면 새로운 이력을 추가하지 않는다.
- 가입 대기 상태의 사용자도 최초 약관 동의를 요청할 수 있다.
- agreement 모듈은 terms 모듈이 공개한 `TermCatalog`와 `TermFact`를 사용한다.
- 약관 동의 흐름에서 사용하는 도메인 예외는 `AgreementException`으로 관리한다.

### 엔드포인트

| 메서드 | 경로 | PENDING 허용 | 설명 |
| --- | --- | --- | --- |
| GET | `/api/agreements` | 아니오 | 본인의 전체 동의 이력 조회 |
| POST | `/api/agreements` | **예** | 약관 동의. 필수 약관이 모두 충족되면 활성화를 요청한다 |
| POST | `/api/agreements/renewals/{termId}` | 아니오 | 갱신 약관 동의 |
| DELETE | `/api/agreements/{termId}` | 아니오 | 선택 약관 동의 철회 |

### 예외 상황

- `REQUIRED_AGREEMENTS_NOT_SATISFIED`: 사용자가 모든 현재 필수 약관에 동의하지 않은 경우
    - 응답 코드: `AGREEMENT-000` / HTTP 400
- `TERM_NOT_FOUND`: 요청한 약관이 존재하지 않거나 현재 유효한 약관이 아닌 경우
    - 응답 코드: `AGREEMENT-300` / HTTP 404
- `REQUIRED_AGREEMENT_CANNOT_BE_WITHDRAWN`: 필수 약관 동의를 명시적으로 철회하려는 경우
    - 응답 코드: `AGREEMENT-700` / HTTP 422
- `TERM_RENEWAL_NOT_REQUIRED`: 같은 종류의 이전 버전 약관 동의 이력이 없어 갱신 동의 대상이 아닌 경우
    - 응답 코드: `AGREEMENT-701` / HTTP 422

---

# FriendRelation

- 두 사용자 사이의 관계를 하나의 애그리게이트로 다룬다.
    - 친구 요청, 친구 관계, 거절, 차단을 모두 `FriendRelation` 한 종류로 표현한다.
    - 두 사용자의 조합 하나당 행 하나만 존재한다.
    - 관계의 방향은 별도 컬럼이 아니라 `modifierId`(요청/거절/차단을 건 쪽)로 표현한다.

> 이전에는 `FriendRequest`, `Friendship`, `FriendRestriction` 세 애그리게이트로 나뉘어 있었다. 같은 두 사람에 대한 사실이 세 테이블에 흩어져 정합성을 서비스 계층이 떠안았기 때문에, `5f205036`에서 하나로 합쳤다.

### 상태값

- `REQUESTED`: 한쪽이 친구 요청을 보낸 상태
- `ACCEPTED`: 서로 친구인 상태
- `REJECTED`: 요청이 거절된 상태. 일정 기간 재요청을 막는다
- `BLOCKED`: 한쪽이 상대를 차단한 상태
- `CANCEL`: 보낸 요청을 취소한 상태. 곧바로 행을 삭제하므로 저장되지 않는다

### 관계 식별

- `FriendPair`가 두 사용자 식별자를 UUID 크기 순으로 정렬해 `low`, `high`로 보관한다.
- DB의 `uk_friend_pair(low_user_id, high_user_id)` 제약이 쌍당 한 행을 강제한다.
- 자기 자신과의 쌍은 만들 수 없다.

### 상태 전이

- `없음 → REQUESTED`: 친구 요청을 보낸다. 요청자가 `modifierId`가 된다.
- `REQUESTED → ACCEPTED`: 받은 쪽이 수락한다.
    - 수락 시점에 양쪽 별칭이 각각 상대의 닉네임으로 채워진다.
    - 요청 메시지는 지운다. 만료 시각은 `PERMANENT`(9999-12-31 23:59:59)로 둔다.
- `REQUESTED → REJECTED`: 받은 쪽이 거절한다.
    - 거절한 쪽이 제한의 주체가 되므로 `modifierId`가 상대로 뒤집힌다.
    - 만료 시각은 거절 시각으로부터 1개월 뒤다.
- `REQUESTED → 삭제`: 받은 요청을 삭제하거나(`deleteReceivedRequest`), 보낸 요청을 취소한다(`cancelSentRequest`).
    - 둘 다 제한 기록을 남기지 않는다.
- `임의 상태 → BLOCKED`: 차단한다. 차단자가 `modifierId`가 되고, 별칭과 메시지를 지우며, 만료 시각은 `PERMANENT`다.
- `BLOCKED → 삭제`: 차단을 해제하면 행 자체를 지운다. 차단을 건 본인만 해제할 수 있다.
- `ACCEPTED → 삭제`: 친구를 끊으면 행 자체를 지운다.
- `REJECTED → 삭제`: 만료된 거절은 스케줄러가 지운다.

### 비즈니스 규칙

- 자기 자신에게는 친구 요청도 차단도 할 수 없다.
- 요청 메시지는 10자 이상이어야 한다. 값 객체 `RequestMessage`가 강제한다.
- 친구 별칭은 공백일 수 없고 10자를 넘을 수 없다. 값 객체 `FriendAlias`가 강제한다.
- 별칭은 자기 쪽 것만 바꿀 수 있고, `ACCEPTED` 상태에서만 바꿀 수 있다.
- 이미 관계 행이 있는 상대에게는 다시 요청할 수 없다. 기존 상태별로 다른 예외를 던진다.
    - `ACCEPTED` → `ALREADY_FRIEND`
    - `REQUESTED` → `FRIEND_REQUEST_ALREADY_SENT`
    - `REJECTED` 또는 `BLOCKED` → `FRIEND_REQUEST_UNPROCESSABLE`
- 요청 수락과 거절은 받은 쪽만 할 수 있다. 보낸 쪽이 호출하면 거부한다.
- 차단은 친구 여부와 무관하게 걸 수 있다. 기존 관계가 없으면 `blockWithoutRelation()`으로 새 행을 만든다.
- 차단은 만료되지 않는다. 거절만 1개월 뒤 만료된다.
- `FriendRestrictionScheduler`가 매일 03시에 만료 시각이 지난 관계 행을 삭제한다.
    - `expiredAt`이 `null`이 아니고 현재 시각 이하인 행이 대상이다.
    - 수락과 차단은 `expiredAt`이 `PERMANENT`이므로 이 삭제에 걸리지 않는다.
- 관계 애그리게이트는 사용자 식별자만 안다. 닉네임과 이메일이 필요한 자리에서만 `FriendMemberLoader`가 `UserLookupContract`로 조회해 넘긴다.
    - 삭제처럼 표시 정보가 필요 없는 명령은 사용자 조회를 일으키지 않는다.
- 명령과 조회는 클래스와 트랜잭션 경계를 나눈다.
    - `FriendRelationCommandService`: 전체가 쓰기 트랜잭션
    - `FriendRelationQueryService`: 전체가 읽기 전용 트랜잭션
- 친구 요청 발송과 수락 시 friends 도메인 이벤트를 발행한다.
    - `FriendRequestSent`, `FriendRequestAccepted` 두 종류이며 `friends::event` Named Interface로만 공개한다.
    - 두 이벤트는 사람을 **사용자 식별자(UUID)** 로만 지목한다. 이메일은 사용자가 바꿀 수 있는 표시 정보라 실어 보내지 않는다.
    - 알림 종류와 전달 수단은 friends가 알지 못하고 notifications가 이벤트를 번역해 결정한다.
- 제한 응답의 `type`은 `FriendRelationStatus`를 그대로 내보낸다. 값은 `REJECTED` 또는 `BLOCKED`다.

### 친구 후보 검색

`/api/users?keyword=` 는 경로만 user를 가리킬 뿐 friends 모듈이 소유한다.

- 검색 결과에서 이미 관계가 있는 사용자를 빼는 것이 이 기능의 핵심이고, 그 판단은 관계를 소유한 friends만 할 수 있다.
- `FriendCandidateSearchService`가 두 사실을 합친다.
    - 제외 대상: `FriendRelationQueryRepository.findRelatedUserIds()` — 내가 낀 모든 관계의 상대. 상태와 만료 여부를 가리지 않는다.
    - 후보 목록: `UserLookupContract.searchActiveByKeyword()` — 제외 목록을 빼고 키워드로 찾은 `ACTIVE` 사용자.
- 요청자 본인도 제외 목록에 넣어 넘긴다.
- 응답 DTO는 friends의 `FriendCandidateResponse`다. user의 `CompactUserResponse`와 필드가 같지만, 그쪽은 user가 공개하지 않는 표현 계층 타입이라 참조할 수 없어 따로 둔다.

### 엔드포인트

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| POST | `/api/friends/requests` | 친구 요청 발송 |
| GET | `/api/friends/requests?type=SENT\|RECEIVED` | 보낸/받은 요청 목록 |
| GET | `/api/friends/requests/{id}` | 요청 단건 조회 |
| POST | `/api/friends/requests/{id}/accept` | 요청 수락 |
| POST | `/api/friends/requests/{relationId}/reject` | 요청 거절 |
| DELETE | `/api/friends/requests/{id}` | 받은 요청 삭제 |
| DELETE | `/api/friends/requests/{id}/sent` | 보낸 요청 취소 |
| GET | `/api/friendships` | 친구 목록 |
| GET | `/api/friendships/target/{targetUserId}` | 특정 사용자와 친구인지 확인 |
| GET | `/api/friendships/{id}` | 친구 단건 조회 |
| PATCH | `/api/friendships/{id}/alias` | 별칭 변경 |
| DELETE | `/api/friendships/{id}` | 친구 끊기 |
| GET | `/api/friends/restrictions` | 내가 건 제한 목록 |
| POST | `/api/friends/restrictions` | 차단 |
| GET | `/api/friends/restrictions/target/{targetUserId}` | 특정 사용자에 대한 유효 제한 여부 |
| DELETE | `/api/friends/restrictions/blocked-users/{targetUserId}` | 차단 해제 |
| GET | `/api/users?keyword=` | 친구 후보 검색 (관계 있는 사용자 제외) |
| GET/DELETE | `/api/admin/friend-requests`, `/api/admin/friendships`, `/api/admin/friend-restrictions` | 관리자 조회 및 삭제 |

### 예외 상황

- `SELF_FRIENDSHIP`: 자기 자신에게 친구 요청을 보내는 경우 (`FRIEND-000` / 400)
- `FRIEND_ALIAS_TOO_LONG`: 별칭이 10자를 넘는 경우 (`FRIEND-001` / 400)
- `FRIENDSHIP_REQUEST_RECEIVER_MISS_MATCH`: 본인에게 온 요청이 아닌데 수락·거절·조회하는 경우 (`FRIEND-002` / 400)
- `FRIEND_ALIAS_BLANK`: 별칭이 비어 있는 경우 (`FRIEND-003` / 400)
- `REQUEST_MESSAGE_SIZE_MORE_THAN_TEN`: 요청 메시지가 10자 미만인 경우 (`FRIEND-004` / 400)
- `FRIENDSHIP_NOT_ACCEPTED`: 친구가 아닌 관계에 별칭을 바꾸려는 경우 (`FRIEND-005` / 400)
- `FRIEND_REQUEST_ALREADY_HANDLED`: 이미 처리된 요청을 다시 수락·거절하는 경우 (`FRIEND-006` / 400)
- `SELF_BLOCK`: 자기 자신을 차단하려는 경우 (`FRIEND-007` / 400)
- `FRIENDSHIP_UNBLOCKED`: 차단 상태가 아닌 관계를 차단 해제하려는 경우 (`FRIEND-008` / 400)
- `FRIEND_RELATIONSHIP_OWNER_MISS_MATCH`: 관계에 속하지 않은 사용자가 관계를 조작하려는 경우 (`FRIEND-200` / 403)
- `FRIEND_RELATIONSHIP_NOT_FOUND`: 관계가 없거나 기대한 상태가 아닌 경우 (`FRIEND-300` / 404)
- `BLOCK_TARGET_NOT_FOUND`: 차단 대상 사용자를 찾을 수 없는 경우 (`FRIEND-301` / 404)
- `ALREADY_FRIEND`: 이미 친구인 상대에게 요청하는 경우 (`FRIEND-500` / 409)
- `FRIEND_REQUEST_ALREADY_SENT`: 이미 요청을 보낸 상대에게 다시 요청하는 경우 (`FRIEND-501` / 409)
- `FRIEND_REQUEST_UNPROCESSABLE`: 차단되었거나 거절 제한이 남은 상대에게 요청하는 경우 (`FRIEND-700` / 422)

> 상태 전이 검사(`validateIsRequested`, `validateAccepted`, `validateBlocked`, `validateOwnership`)는 모두 `FriendRelation` 안에 있다. 서비스는 관계를 찾아 오고 전이 결과를 저장하는 일만 한다.

---

# Notification

알림 한 건은 요청 접수부터 외부 채널 발송, 실패와 운영자 재발송, 사용자 읽음까지 하나의 `Notification` 애그리게이트가 소유한다. 별도 이력 모델이나 외부 DLQ는 없다.

### 수신자 지목

`targetIdentifier` 한 칸이 수단에 따라 다른 것을 담는다.

| 수단 | 담기는 값 | 이유 |
|-----|---------|-----|
| FCM | 수신자의 **사용자 식별자(UUID)** | 기기 토큰의 주인을 `fcm_token.owner_id`로 찾는다 |
| SMS | 수신 **전화번호** | `users`에 전화번호 칸이 없어 식별자로 풀 수 없다 |

- `fcm_token`은 주인을 `owner_id BINARY(16)`으로 가리키고 `uk_fcm_token_owner_id`로 사용자당 한 개만 허용한다. 이메일은 사용자가 바꿀 수 있는 표시 정보라 소유 관계를 붙들어 두기에 적합하지 않다.
- 수신함 조회와 읽음 처리도 사용자 식별자로 판정한다. `targetIdentifier != recipientId`면 `NOT_MY_NOTIFICATION`이다.
- 발송 요청 API의 `targetIds`도 FCM이면 UUID, SMS면 전화번호다. `TargetIdValidator`가 수단별 형식을 검사한다.

### 발송 요청 접수

- `POST /api/notifications` 하나로 단건과 다건을 모두 받는다. `targetIds` 길이만큼 `NotificationEvent`로 갈라 발행한다.
- **발행은 반드시 트랜잭션 안에서 일어나야 한다.** 받는 쪽이 `@ApplicationModuleListener`(= `@TransactionalEventListener`)라 활성 트랜잭션의 커밋 시점에만 깨어난다. 트랜잭션 없이 발행하면 이벤트가 조용히 버려지고 `event_publication`에도 남지 않아 재발행 대상조차 되지 않는다. 그래서 경계를 컨트롤러가 아니라 `NotificationUseCase.request()`가 연다.

### 상태값과 전이

```text
request() → PENDING ── markSent() ──> SENT ── markAsRead() ──> SENT(isRead=true)
                └── markFailed() ──> FAILED ── markFailed() ──> DEAD
                                                          └── retry() ──> PENDING
```

- 최대 발송 시도는 3회다. 세 번째 실패에서 `DEAD`가 된다.
- `retry()`는 `DEAD`에서만 가능하며 시도 횟수와 마지막 실패 사유를 초기화한다.
- 읽음 처리는 `SENT`에서만 가능하고 이미 읽은 알림에는 멱등이다.
- 수신함 노출 조건은 `method == FCM && status == SENT`다. SMS와 `PENDING`/`FAILED`/`DEAD`는 수신함에서 제외된다.

### 전달과 멱등성

- 보장 수준은 exactly-once가 아니라 **at-least-once 전달 + DB UNIQUE 기반 중복 억제**다.
- `dedupe_key`는 `"{eventId}:{method}"`이고 `uk_notification_dedupe_key`가 동시 중복 예약 중 하나만 허용한다.
- 외부 발송 성공 직후 `SENT` 커밋 전에 프로세스가 죽으면 다시 발송될 수 있다. 외부 채널이 멱등 키를 받지 않으므로 이 창은 의도적으로 수용한다.
- `NotificationRecoveryScheduler`는 5분 이상 `PENDING`/`FAILED`에 머문 알림을 회수한다.
- 접수·성공·실패 기록은 각각 독립 트랜잭션이다. 특히 실패 기록은 그 실패를 일으킨 예외가 바깥으로 튀어도 살아남아야 하므로 `REQUIRES_NEW`가 필수다. 발송 서비스는 이 경계를 애노테이션이 아니라 `TransactionTemplate`으로 직접 연다 — 한 클래스 안에서 자기 메서드를 부르면 프록시를 타지 않아 `@Transactional`이 조용히 무시되기 때문이다.
- 발송이 최종 실패하면 `NotificationDeliveryFailed`를 실패 기록 트랜잭션 안에서 발행한다. 운영 경보 전송이 발송 경로에 얹혀 있지 않으므로 웹훅이 느려도 발송이 느려지지 않고, 도중에 죽어도 재시작 시 다시 꺼내진다.
- 인메모리 Modulith 이벤트는 발행 인스턴스에서만 처리된다. 서버를 2대 이상으로 늘리는 시점이 외부 브로커 재도입 검토 시점이다.
- `event_publication.serialized_event`는 `VARCHAR(4000)`이므로 이벤트에는 큰 payload를 싣지 않는다.

### 채널 정책

- `PushChannel`이 Android channel/priority와 iOS APNs priority/interruption-level/sound를 함께 소유한다.
- `DeviceType.AOS`는 `AndroidConfig`, `DeviceType.IOS`는 `ApnsConfig`로 분기한다.
- FCM과 SMS 모두 같은 `Notification` 상태 머신과 이력 테이블을 사용한다.

### 구성

애플리케이션 서비스는 셋이다.

| 클래스 | 맡는 일 |
|-------|--------|
| `NotificationDeliveryService` | 접수 → 발송자 호칭 결정 → 외부 채널 발송 → 결과 기록·통지까지 발송 경로 전부 |
| `NotificationService` | 이미 저장된 알림의 조회·읽음·재발송·폐기, 그리고 발송 요청 접수(`NotificationUseCase` 구현) |
| `FcmTokenEnrollService` | 기기 토큰 등록 |

발송 경로의 네 단계는 서로만 호출하는 한 시나리오의 단계였으므로 한 클래스로 합쳤다. 수신함과 실패 알림 관리는 둘 다 `NotificationPersistencePort` 하나만 붙들고 있어 나누면 같은 의존성의 껍데기가 둘로 늘 뿐이었다.

### 관리자 운영

- `GET /api/admin/failed-notifications`: 실패 알림 목록
- `POST /api/admin/failed-notifications/{id}/redelivery-jobs`: 단건 재발송
- `POST /api/admin/failed-notifications/redelivery-jobs`: 일괄 재발송
- `DELETE /api/admin/failed-notifications/{id}`: 실패 기록 폐기
- `/admin/failed-notifications`: 관리자 화면

---

# 모듈 의존 관계

```
agreement     ──> terms   (TermCatalog, TermFact)
agreement     ──> user    (UserActivationContract)
auth          ──> user    (UserLookupContract, UserRegistrationContract, user::domain)
friends       ──> user    (UserLookupContract, UserResult, user::domain)
friends       ──> shared  (DomainEventPublisher)
notifications ──> friends (friends::event, 단방향)
notifications ──> shared  (DomainEventPublisher)
```

- terms는 어떤 도메인 모듈도 알지 못한다.
- user는 agreement도 friends도 알지 못한다.
- `shared`와 `support`는 하위 패키지를 모두 공개하는 Open 모듈이다.
- 모듈 간 참조는 전부 상대 모듈이 Named Interface로 공개한 타입만 쓴다.
- `ModularityTest.verify_success_no_module_boundary_violation()`이 이 규칙을 강제한다. 사이클도 non-exposed 참조도 남아 있지 않다.

---

# 부록: 정리한 항목

이전 판에 "정리가 필요한 지점"으로 적어 둔 것들이다. 모두 해소했으므로 무엇을 왜 바꿨는지만 남긴다.

### 사용하지 않는 활성화 DTO 제거

`04cbbda4`에서 `POST /api/users/activation`과 `ActivateUserService`를 없애면서 활성화 경로를 `POST /api/agreements` 하나로 모았는데, 그때 쓰이던 `UserActivationRequest`와 `UserActivationCommand`가 남아 있었다. 참조가 하나도 없어 삭제했다.

### 별칭 길이 오류 메시지 정정

`FriendAlias.MAX_LENGTH`와 DB 컬럼은 10인데 `FRIEND_ALIAS_TOO_LONG` 메시지만 "20자"라고 말하고 있었다. 메시지를 10자로 고쳤다.

### `FriendRestrictionType` 삭제

제한 응답의 `type`을 `REJECT` / `BLOCK`으로 바꿔 주던 표현 계층 전용 enum이었다. 매핑 함수의 `else` 분기가 제한이 아닌 상태까지 조용히 `BLOCK`으로 만들고 있었다. 변환을 없애고 `FriendRelationStatus`를 그대로 내보낸다.

> **응답 값이 바뀐다.** `REJECT` → `REJECTED`, `BLOCK` → `BLOCKED`. 클라이언트가 이 문자열을 비교하고 있다면 함께 고쳐야 한다. 관리자 화면 템플릿은 같이 수정했다.

### 스키마와 enum 불일치 해소

`db/init/mysql/imhere-full-init.sql`의 `users.provider`에서 코드가 쓰지 않는 `NAVER`를 뺐다. 초기화 스크립트라 새로 만드는 DB에만 적용된다. 이미 떠 있는 DB는 해당 값을 쓰는 행이 없으므로 그대로 두어도 된다.

### 모듈 경계 검사 활성화

`ModularityTest.verify_success_no_module_boundary_violation()`의 `@Disabled`를 뗐다. 애노테이션에 적혀 있던 사이클 7건·non-exposed 116건은 이후 리팩토링으로 대부분 이미 사라진 상태였고, 실제로 남아 있던 위반은 둘뿐이었다.

- `user → friends.repository.jpa` Q타입: `SpringQueryDSLUserRepository`가 검색 제외 대상을 직접 계산하느라 `QFriendRelationJpaEntity`를 참조했다. 이것이 `friends ↔ user` 사이클의 유일한 원인이었다. 제외 대상 계산을 friends의 `FriendCandidateSearchService`로 옮기고, user에는 `searchActiveByKeyword(keyword, excludedUserIds, pageable)`만 남겼다.
- `notifications → auth.application.port.out.CachePort`: 캐시는 auth만의 개념이 아닌데 auth의 비공개 out 포트에 있었다. `CachePort`와 `LocalCacheAdapter`를 `shared.cache`로 옮겼다.

이제 사이클도 non-exposed 참조도 없다.

---

# 연관 문서

- [architecture.md](architecture.md)
- [internal-architecture.md](internal-architecture.md)
- [../infra/db-schema.md](../infra/db-schema.md) — ERD와 테이블 단위 설명
- [../security/jwt.md](../security/jwt.md)
- [../security/oauth.md](../security/oauth.md)
- [../flows/friend-request.md](../flows/friend-request.md)
