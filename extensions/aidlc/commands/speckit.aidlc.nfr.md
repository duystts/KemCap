---
description: "AI-DLC NFR requirements and NFR design derived from spec, plan and constitution"
---

# AI-DLC NFR Requirements & NFR Design (stages 3.2 + 3.3, lifted to feature level)

Runs as an optional `after_plan` hook, or by hand.

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

1. Read `STATE`. If the profile is `express` or `bugfix`, audit `skipped` and stop.
2. Read `spec.md` (Success Criteria, edge cases), `plan.md` (Technical Context, Constitution Check, Complexity Tracking), `research.md`, `contracts/`, and the constitution.
3. Delegate to subagent **aidlc-architect**: write `AIDLC_DIR/nfr.md` from `TPL/nfr-template.md`:
   - every NFR is measurable (number + unit, or a verifiable rule) and cites its source (SC-..., FR-..., a constitution principle, or a plan decision);
   - NFR design names the concrete mechanism, the module/port it lives in, and how it will be verified (test type, metric);
   - anything that would add infrastructure must respect the constitution (e.g. YAGNI / cost rules); if it cannot, record it as a risk that needs Complexity Tracking in `plan.md`.
4. Do not edit `plan.md`; list proposed plan changes instead.
5. **GATE**. On approval mark 3.2/3.3 `[x]`, audit.
