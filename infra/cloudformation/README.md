# 인프라 정의 위치

- [`main.yaml`](main.yaml)은 백엔드 EC2 인프라를 `imhere-prod-infra` 스택으로 정의한다.
- 현재 운영 중인 S3와 CloudFront는 [클라이언트 저장소의 `infra/aws/web-hosting.yml`](https://github.com/ImHereOfRati/mobile/blob/main/infra/aws/web-hosting.yml)이 정의한다. `imhere-web` 스택은 하나의 비공개 버킷에서 `imhere.ratiko.co.kr`과 `imhere-design.ratiko.co.kr`을 제공하고, `/api/*`, `/swagger-ui/*`, `/docs/*`를 `imhere-server.ratiko.co.kr`로 전달한다.
- 옛 `web-distribution.yml`은 폐기했다. 이전 체크아웃을 이용해 `imhere-web-distribution` 스택을 다시 만들지 않는다.

도메인 결정은 [ADR-025](https://app.notion.com/p/3a82776ec189813ba4c6fb38e37cac26), 운영 스택과 전환 이력은 [클라이언트 인프라 문서](https://github.com/ImHereOfRati/mobile/blob/main/infra/aws/README.md)를 참고한다.
