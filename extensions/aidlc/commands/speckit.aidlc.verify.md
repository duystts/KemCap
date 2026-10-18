---
description: "AI-DLC build & test plus independent constitution review for the unit just implemented"
---

# AI-DLC Verify (stage 3.6 build & test + independent review)

Runs as a mandatory `after_implement` hook, or by hand with `U#` or `--all`.

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

1. Determine scope: the unit approved by the last unit-gate in this session (or from user input); `--all` means the whole feature. If no AI-DLC state exists, review the whole feature.
2. **Build & test**: delegate to subagent **aidlc-quality**. It finds the real build/test commands (from `plan.md`, `quickstart.md`, build files), runs them, and fills `AIDLC_DIR/build-test-<U#|all>.md` from `TPL/build-test-report-template.md` with summaries of the actual output. Never claim a pass that was not observed; anything that cannot run locally (missing Docker, DB, ...) is recorded as `NOT RUN` with the reason.
3. **Independent review**: delegate to subagent **aidlc-reviewer** (read-only; it did not write the code). It checks the unit's files against every constitution principle, the unit's acceptance criteria (`stories.md` / `spec.md`) and NFRs (`nfr.md`), and adds findings with `file:line` evidence to the same report.
4. Verdict: `PASS` (build/test green, no blocking finding), `PASS_WITH_NOTES` (non-blocking findings only), `FAIL` (red build/test, a constitution MUST violated, or acceptance criteria unmet).
5. **GATE** on the report. On approval with PASS / PASS_WITH_NOTES mark the unit Verified in `STATE` and set **Next step** to `__SPECKIT_COMMAND_IMPLEMENT__` (next unit). On FAIL list the fix tasks and keep the unit open.
6. Audit the verdict and who approved it.
