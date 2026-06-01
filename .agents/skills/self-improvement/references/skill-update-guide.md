# Self-Improvement — Skill Update Guide Reference

## How to Update an Existing Skill

1. **Minimal changes only** — do not rewrite the whole file
2. Add new patterns to the relevant section
3. Fix incorrect examples in-place — log the reason
4. If the file would exceed 80 lines → split into `references/` sub-file

```markdown
## Rule N — [New Rule Name]

**Why:** [reason]

```dart
// ✅ GOOD
// ❌ BAD
```
```

## How to Create a New Skill

Path: `.agents/skills/{skill-name}/SKILL.md`

Required frontmatter:
```markdown
---
name: {skill-name}
description: [한 문장 — 어떤 상황에서 이 스킬을 읽어야 하는지 포함]
---
```

Required structure:
```markdown
# [Skill Title]

## Pre-task Checklist
- [ ] [항목]

## Rule 1 — [Rule Name]
**Why:** [reason]

```dart
// ✅ GOOD
// ❌ BAD
```

> Detailed content: `references/[topic].md`
```

## File Size Rules

- `SKILL.md`: **≤ 80 lines** hard limit
- `references/*.md`: ≤ 80 lines recommended
- If a reference file grows beyond 80 lines → split into multiple reference files

## Skill Naming Conventions

| Category | Skill Name |
|----------|------------|
| Architecture rules | `project-architecture` |
| Coding constraints | `coding-limits` |
| View/UI rules | `view-rules` |
| Design tokens | `design-system` |
| Test strategy | `testing-guide` |
| Agent protocol | `self-improvement` |
| New domain-specific | `{domain}-{type}` (e.g. `geofence-rules`) |
