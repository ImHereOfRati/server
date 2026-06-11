## Skill Update Guide

Update an existing skill or create a new one when the task reveals a reusable project rule.

Checklist:
- Update the most specific existing skill first when possible.
- Create a new skill only when no current skill covers the rule.
- Keep changes small and directly tied to what was learned.
- Preserve valid frontmatter.

Required skill file shape:

```md
---
name: lowercase-hyphen-name
description: Use when <trigger keywords, files, or situations>.
---

# Skill Title

<concise, reusable instructions>
```

Frontmatter rules:
- `name` must match the folder name.
- `name` must be lowercase hyphen-separated.
- `description` should start with `Use when` or `Use ONLY when` and include concrete triggers.

When updating a skill, record:
- What new rule or correction was learned.
- Why the previous skill content was incomplete or outdated.
- Which task exposed the gap.
