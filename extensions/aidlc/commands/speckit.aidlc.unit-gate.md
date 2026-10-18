---
description: "Pick the next unit by the DAG, show its plan and wait for approval before implementing"
---

# AI-DLC Unit Gate (stage 3.5 plan approval)

Runs as a mandatory `before_implement` hook. It makes `__SPECKIT_COMMAND_IMPLEMENT__` work **one unit at a time**, like AI-DLC Construction.

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).

## Resolve context (shared by every AI-DLC command)

1. Run `.specify/scripts/powershell/check-prerequisites.ps1 -Json -PathsOnly` from the repo root (projects initialized with `--script sh` use `.specify/scripts/bash/check-prerequisites.sh --json --paths-only`) and parse `FEATURE_DIR`. If the user input names a feature directory (e.g. `specs/001-...`), use that instead.
2. Set `AIDLC_DIR = FEATURE_DIR/aidlc`, `STATE = AIDLC_DIR/aidlc-state.md`, `AUDIT = AIDLC_DIR/audit.md`, `TPL = .specify/extensions/aidlc/templates`.
3. Read `.specify/memory/constitution.md`. Its principles override anything in this command.
4. Write every artifact in the same human language as `spec.md` (fall back to the constitution's language).

## Approval gate (shared)

When this command says **GATE**: stop and show the user a short summary (what was produced, key decisions, open questions, file links) followed by exactly these choices:

> **Duyệt** (approve) · **Sửa** (tell me what to change) · **Hỏi thêm** (ask questions)

Do not mark the stage `[x]` or continue to the next stage until the user approves. On **Sửa**, apply the change and show the gate again. In an automated run (a workflow step with no human in the loop, e.g. `specify workflow run aidlc-sdd`) do not wait: mark the stage `[~]` (waiting for approval), audit `gate-shown`, and finish the command. The workflow's own `gate` step is the human approval; the workflow's follow-up `approve` prompt then turns `[~]` into `[x]`.

## Audit (shared)

Append exactly one row per event to `AUDIT` (create it from `TPL/audit-template.md` if missing): UTC timestamp, stage id, command name, event (`started`, `skipped`, `gate-shown`, `approved`, `changed`, `failed`), decision + who approved, artifact paths. Never edit or delete earlier rows.

## Delegation (shared)

If the named Claude Code subagent exists in `.claude/agents/`, delegate the drafting work to it with the Agent tool and give it the file paths it needs (not their contents); otherwise do the work yourself in that role. You (the orchestrator) own the gate, the state file and the audit log — subagents never update them.


## Steps

1. Read `STATE`, `AIDLC_DIR/unit-of-work.md`, `AIDLC_DIR/task-map.md`, `tasks.md`. If no AI-DLC state exists, say so in one line and let implement continue unchanged.
2. Choose the unit:
   - if the user input names one (`U3`, `unit=U3`), use it after checking all its dependencies are Verified in `STATE`;
   - otherwise pick the first unit in bolt order whose dependencies are all Verified and which is not Verified itself (a unit counts as Verified only when `STATE` marks it Verified, not when it is still waiting for approval);
   - if every unit is Verified, say so and recommend `__SPECKIT_COMMAND_AIDLC_VERIFY__ --all`, then `__SPECKIT_COMMAND_CONVERGE__`.
3. Show the **unit plan**: goal, task IDs (with done/open status from `tasks.md`), files/modules it will touch, tests it must add, definition of done, risks. **GATE**.
4. On approval: mark the unit "Plan approved" in `STATE`, audit, then state the scope for `__SPECKIT_COMMAND_IMPLEMENT__` (this same session) explicitly:

   > **AI-DLC scope**: implement ONLY tasks `<IDs>` of unit `<U#>`. Leave all other tasks unchecked. Stop after these tasks; `__SPECKIT_COMMAND_AIDLC_VERIFY__` runs next.

5. On rejection: audit `changed` and do not start implementation.
