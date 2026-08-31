---
# 수정 필요
---
---

# Git 브랜치 전략

1인 개발에 맞춰 브랜치 구조를 단순하게 유지한다.

## 브랜치 구조

```text
main
 ├─ feature/*
 ├─ fix/*
 └─ hotfix/*
```

| 브랜치 | 용도 |
|---|---|
| `main` | 운영 배포 기준. 항상 배포 가능한 상태로 유지한다. |
| `feature/*` | 새로운 기능 개발 |
| `fix/*` | 일반적인 버그 수정 |
| `hotfix/*` | 운영 중인 긴급 문제 수정 |

## 기본 작업 흐름

### 1. 작업 브랜치 생성

항상 최신 `main`에서 작업 브랜치를 만든다.

```bash
git switch main
git pull origin main
git switch -c feature/login
```

### 2. 작업 및 커밋

작은 단위로 작업하고, 커밋 메시지는 변경 목적이 드러나도록 작성한다.

```bash
git add .
git commit -m "feat: add social login"
```

### 3. Pull Request 생성

작업이 끝나면 원격 저장소에 push하고 `main`을 대상으로 Pull Request를 생성한다.

```bash
git push -u origin feature/login
```

Pull Request에는 다음 내용을 간단히 작성한다.

- 무엇을 변경했는가
- 어떻게 테스트했는가
- 관련 이슈가 있는가

### 4. 검증 후 main 병합

CI 테스트가 통과하고 변경 내용을 확인한 뒤 `main`에 병합한다. `main`에 병합되면 CD가 운영 배포를 수행한다.

### 5. 작업 브랜치 삭제

병합이 끝난 작업 브랜치는 삭제한다.

```bash
git switch main
git pull origin main
git branch -d feature/login
git push origin --delete feature/login
```

## 브랜치 이름 예시

```text
feature/social-login
feature/friend-search
fix/notification-timeout
fix/invalid-token-response
hotfix/login-server-error
```

## 커밋 메시지

가능하면 Conventional Commits 형식을 사용한다.

```text
feat: add friend search API
fix: handle expired access token
refactor: simplify notification service
test: add friendship integration test
docs: update deployment guide
chore: update dependencies
```

## 운영 원칙

- `main`에 직접 작업하지 않는다.
- 하나의 브랜치에는 하나의 목적만 담는다.
- 작업 브랜치는 `main`에서 생성한다.
- 작은 변경도 Pull Request로 기록한다.
- 테스트가 통과한 변경만 `main`에 병합한다.
- 병합 후 사용하지 않는 브랜치는 삭제한다.
- 운영 긴급 수정은 `hotfix/*`에서 진행하고, 완료 후 `main`에 병합한다.

## 요약

```text
작업 시작
  ↓
main에서 feature/fix/hotfix 브랜치 생성
  ↓
개발 및 테스트
  ↓
Pull Request
  ↓
CI 통과 및 확인
  ↓
main 병합
  ↓
자동 운영 배포
  ↓
작업 브랜치 삭제
```
---
# 수정 필요
---
---
