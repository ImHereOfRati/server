## Agent Log Entry Format

Append one section per completed task to `agent-log/YYYY-MM-DD.md`.

Use this template:

```md
## Task [N] - HH:MM KST | <task_type>

### User Request
<Exact original user request>

### Questions Asked
- Q: <question>
  A: <answer>

### Agent Plan
1. <step>
2. <step>
3. <step>

### Work Done
- <action taken>
- <action taken>

### Failures & Root Causes
| Attempt | Failure | Root Cause |
|---|---|---|
| 1 | <failure> | <cause> |

### Fixes Applied
- <fix>

### Files Changed
| Action | Path |
|---|---|
| Modified | `path/to/file` |

### Skill Updates
| Skill | Action | Insight |
|---|---|---|
| `skill-name` | Updated | <what changed and why> |
```

Rules:
- Keep the user's request text exact in `User Request`.
- Omit empty Q/A rows only if no questions were asked.
- Record failed attempts when they happened.
- List only files actually changed in the task.
