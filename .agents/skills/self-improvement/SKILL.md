---
name: self-improvement
description: 매 작업 종료 후 에이전트가 반드시 따라야 할 로그 기록 및 스킬 자동 업데이트 프로토콜. 대화 내용, 실패, 수정 방식, 인사이트를 agent-log/에 기록하고 관련 스킬을 즉시 갱신한다.
---

# Self-Improvement Protocol — ImHere Mobile

> **This is the LAST step of every task. No exceptions.**

## Step 1 — Write Log Entry

File: `agent-log/YYYY-MM-DD.md` (KST date)
- File exists → **append**. Missing → create.
- Full format template: `references/log-format.md`

Required sections per entry:

| Section | Content |
|---------|---------|
| Task [N] — HH:MM KST \| task_type | Header |
| User Request | Exact original text |
| Questions Asked | Q/A pairs |
| Agent Plan | Step-by-step plan |
| Work Done | What was actually executed |
| Failures & Root Causes | Table of failed attempts |
| Fixes Applied | How failures were resolved |
| Files Changed | action + path table |
| Skill Updates | skill + action + insight |

## Step 2 — Update Skills Immediately

Update if ANY of these are true:

| Condition | Skill to update |
|-----------|----------------|
| New architecture/layer pattern | `project-architecture` |
| Folder structure changed | `project-architecture` |
| New coding rule or pattern found | `coding-limits` |
| New API/Controller mistake found | `api-design` |
| Error/Exception handling change | `error-handling` |
| JPA, QueryDSL, Entity usage change | `jpa-querydsl-rules` |
| Security/Auth mechanism change | `security-auth-rules` |
| External/Async integration change | `async-external-rules` |
| Kotlin language idiom/syntax found | `kotlin-conventions` |
| API documentation usage change | `api-docs-rules` |
| Logging, Masking, Tracing change | `observability-rules` |
| Cache/Redis usage change | `caching-redis-rules` |
| Transaction/Event handling change | `transaction-event-rules` |
| Any skill content is outdated | that skill |
| No skill covers a needed rule | create new skill |

Skip only if insight is **identical** to existing content.

> Skill update procedure & new skill template: `references/skill-update-guide.md`

## Step 3 — Self-Review

- [ ] Log has exact user request text?
- [ ] Failures recorded if any occurred?
- [ ] Skill Updates section filled in?
- [ ] Skills updated where criteria met?
- [ ] New skills have `name` + `description` frontmatter?
