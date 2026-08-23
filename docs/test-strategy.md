해당 문서는 `ImHere`에서 사용하는 **테스트 코드 작성 기준과 테스트 범위를 판단하는 기준**을 다루고 있습니다.

# 테스트 코드 작성 기준

- **각 계층이 책임지는 규칙을 해당 계층에서 테스트한다.**
- **같은 규칙을 여러 계층에서 중복 검증하지 않는다.**
- **가능한 경우 큰 통합 테스트보다 책임과 가까운 작은 테스트에서 먼저 검증한다.**
- **DB 테스트는 H2 를 사용한다**
    - JPA Mapping
    - Repository Query
    - QueryDSL
    - Paging / Sorting / Filtering
    - Unique Constraint
- **외부 Provider는 실제 호출하지 않는다**

  실제 Provider를 테스트에 포함하면 외부 서비스의 가용성이 테스트 결과에 영향을 주고, 실제 발송이나 과금이 발생할 수 있기 때문이다.

    - **테스트 대상**
        - 요청 Mapping
        - 응답 Mapping
        - Provider Error Mapping
        - Retry 가능 여부
        - 잘못된 응답 처리
- **Transaction 이후 Event 처리는 실제 Context에서 검증한다**
    - Application Event는 Event가 발행됐는지만 확인하지 않는다.
- **모듈 경계와 API 문서도 테스트 대상으로 본다**

# 계층별 테스트 대상

- **`Domain` :** 도메인 상태 전이와 불변식을 검증한다.
- **`Service 계층`**
    - 객체 간 협력과 부수 효과를 검증한다.
    - 실패했을 때 저장이나 Event 발행이 발생하지 않는지도 확인한다.
- **`Persistence 계층`** : JPA Mapping, Query, Paging, Filtering, DB Constraint를 검증한다.
- **`Controller 계층`** : HTTP 요청·응답 계약과 Validation을 검증한다.
- **`통합 테스트`**
    - 여러 계층이 연결되어야 확인할 수 있는 핵심 흐름을 검증한다.
    - Transaction 이후 Application Event 처리를 검증한다.
    - 외부 Provider와의 Adapter 경계를 검증한다.
    - Security와 같은 횡단 관심사를 검증한다.

# 현재 테스트 범위의 한계

- 현재 테스트는 운영 환경 전체를 재현하지는 않는다.
- 재검토 조건 : 사용자 증가, 트래픽 증가, 다중 인스턴스 운영 등으로 현재 테스트 환경에서 검증하지 못하는 영역의 위험도가 높아질 경우 별도의 검증 환경과 테스트를 추가한다.
- **현재 직접 검증하지 않는 영역**
    - 실제 MySQL의 Query Plan, Lock, 동시성
    - 실제 FCM / SMS 전달
    - 실제 OIDC Provider 호출
    - 외부 Provider의 실제 Network 장애
    - 다중 Server Instance 간 Cache 공유와 동시 처리
    - 실제 Process 장애 이후 Event 복구
    - nginx부터 외부 Provider까지 연결된 운영 환경 전체 E2E
