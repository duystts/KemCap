---
description: "Map every task in tasks.md to a unit of work"
---

# AI-DLC Task to Unit Mapping

Runs as a mandatory `after_tasks` hook, or by hand.

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

1. Read `STATE` and `AIDLC_DIR/unit-of-work.md`. If there is no unit file, audit `skipped` and stop. If there is exactly one unit, map every task to `U1`.
2. Read `tasks.md`. Parse every task line (`- [ ] T### ...` / `- [x] T### ...`).
3. **Do not edit `tasks.md`** (it stays owned by `__SPECKIT_COMMAND_TASKS__` / `__SPECKIT_COMMAND_IMPLEMENT__`). Write `AIDLC_DIR/task-map.md` instead:
   - a table `Unit | Task IDs | Count | Done` with one row per unit;
   - assignment rules: Setup/Foundational phases go to the foundation unit; user-story phases go to the unit that owns that story; polish tasks go to the unit whose module/file they touch, or to a final integration unit if the unit file defines one;
   - list tasks whose unit is ambiguous under **Needs decision** with your best guess.
4. Validate: every task ID appears exactly once; dependencies stated in `tasks.md` never make a task in an earlier bolt wait for a task in a later bolt (report violations).
5. **GATE** only when there are ambiguous tasks or violations; otherwise just report. Audit.
