# Self-Improvement — Log Format Reference

## Full Log Entry Template

```markdown
---

## Task [N] — [HH:MM KST] | [task_type]

> task_type: feature | bugfix | refactor | architecture | docs | skill-update | other

### User Request
[사용자의 요청 원문을 그대로 기록. 요약하지 않는다.]

### Questions Asked

- **Q:** [질문 내용]
  **A:** [사용자 답변]
- (질문이 없었다면: "없음")

### Agent Plan

1. [Step 1]
2. [Step 2]

### Work Done

- [수행 내용 1]
- [수행 내용 2]

### Failures & Root Causes

| 시도 | 실패 원인 |
|------|---------|
| [시도 내용] | [왜 실패했는가] |

(실패 없으면 "없음")

### Fixes Applied

- [수정 내용 1]
(수정 없으면 "없음")

### Files Changed

| Action | File |
|--------|------|
| created | `path/to/file.dart` |
| modified | `path/to/file.dart` |
| deleted | `path/to/file.dart` |

### Skill Updates

| Skill | Action | Insight |
|-------|--------|---------|
| [skill-name] | updated / created / no-change | [이유] |
```

## Task [N] Numbering

- Starts at 1 for each new daily file
- Increments sequentially within the same day's file
- New day → new file → restart at Task 1
