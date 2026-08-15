# config-example

private config repo(`ImHereOfRati/config`)가 어떤 모양이어야 하는지를 보여 주는
템플릿입니다. **값은 전부 비어 있습니다.** 실제 값이 들어간 파일은 이 저장소에
두지 않습니다.

## 왜 파일이 여러 개인가

예전에는 `prod.env` 한 파일이 DB 비밀번호부터 Grafana Cloud API 키까지 전부
담고 있었고, `dsko`(앱) · `nginx` · `alloy` 세 컨테이너가 **그 한 파일을 통째로**
읽었습니다. 그래서 alloy 컨테이너 안에서도 `DB_PASSWORD`가 보였습니다.

지금은 관심사별로 쪼개고, 컨테이너마다 필요한 파일만 넣습니다.

| 파일 | 담는 것 | 읽는 컨테이너 |
| --- | --- | --- |
| `env/app.env` | 프로파일, DB, JWT, 관리자/보안 | `dsko` |
| `env/web.env` | 도메인, TLS, CORS | `dsko`, `nginx` |
| `env/oidc.env` | 소셜 로그인 클라이언트 ID | `dsko` |
| `env/external.env` | Firebase, NCP/Naver, SOLAPI, Discord | `dsko` |
| `env/observability.env` | Grafana Cloud 자격증명 | `alloy` |

`env/web.env`가 두 컨테이너에 들어가는 이유는, 앱은 `CORS_ALLOWED_ORIGINS`가
필요하고 nginx 쪽은 `SERVER_NAME`/`CERT_DOMAIN`이 필요한데 둘 다 "이 서비스가
어떤 도메인으로 노출되는가"라는 하나의 관심사이기 때문입니다.

> nginx 설정 파일 자체는 CI에서 이미 값이 치환된 상태로 만들어져 EC2로 갑니다
> (`infra/nginx/nginx.conf.template` → `nginx.conf`). 그래서 nginx 컨테이너의
> `env_file`은 실행 중 디버깅과 향후 확장을 위한 것이지, 설정 렌더링에 쓰이는
> 값이 아닙니다.

## config repo가 가져야 할 구조

```
ImHereOfRati/config
├── env/
│   ├── app.env
│   ├── web.env
│   ├── oidc.env
│   ├── external.env
│   └── observability.env
└── imhereFirebaseKey.json
```

`infra/scripts/sync-config.sh`가 이 구조를 그대로 기대합니다. `env/` 아래에
`.env` 파일이 하나도 없으면 배포가 그 자리에서 멈춥니다.

## 값을 어디서 얻는가

각 키의 의미와 발급 경로는 `BACKEND-DEPLOY-VALUES.md`에 정리돼 있습니다.

## 배포 시 흐름

1. `sync-config` job이 config repo를 clone해 `env/*.env`와 Firebase 키를 꺼냅니다.
2. `deploy-app` job이 `env/*.env`를 전부 `source`해서 nginx/alloy 템플릿을
   렌더링합니다.
3. `env/` 디렉터리 전체와 렌더링 결과를 EC2로 복사합니다.
4. `docker compose --profile prod`가 위 표대로 컨테이너별 `env_file`을 붙입니다.

## 새 변수를 추가할 때

1. `application.yaml`(또는 nginx/alloy 템플릿)에 `${NEW_VAR}`를 쓴다.
2. 이 디렉터리의 알맞은 `env/*.env`에 키를 **빈 값으로** 추가한다.
3. config repo의 같은 파일에 실제 값을 넣는다.
4. 앱이 새로 읽는 파일이라면 `docker-compose.yml`의 해당 서비스 `env_file`에도
   추가한다.

2번을 빼먹으면 리포만 보고는 그 변수의 존재를 알 수 없게 됩니다. 기본값 없는
변수가 비면 앱은 기동 단계에서 실패합니다.
