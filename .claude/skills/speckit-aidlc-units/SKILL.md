---
name: speckit-aidlc-units
description: 'AI-DLC units of work: decompose the plan into units with a dependency DAG and delivery order'
compatibility: Requires spec-kit project structure with .specify/ directory
metadata:
  author: phamanhduy
  source: aidlc:commands/speckit.aidlc.units.md
---

# AI-DLC Units Generation + Delivery Planning (stages 2.7 + 2.9)

Runs as a mandatory `after_plan` hook (after nfr), or by hand.

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

1. Read `STATE`. If the profile is `express` or `bugfix`: write a minimal `AIDLC_DIR/unit-of-work.md` with a single unit `U1` covering the whole feature, audit `skipped` (single-unit), and stop.
2. Read `plan.md` (project structure, modules), `data-model.md`, `contracts/`, `spec.md` priorities, and `AIDLC_DIR/stories.md` / `nfr.md` if present.
3. Delegate to subagent **aidlc-architect**: write `AIDLC_DIR/unit-of-work.md` from `TPL/unit-of-work-template.md`:
   - a unit is independently buildable, testable and reviewable; align units with the module boundaries in the plan/constitution; aim for 3 to 8 units;
   - `U1` is shared foundations / walking skeleton (setup + cross-cutting infrastructure everyone needs);
   - give each unit its stories, NFRs, owner (from plan/tasks if team roles exist) and a rough size;
   - draw the dependency DAG (Mermaid + table); it MUST be acyclic; group units into bolts (units in the same bolt can run in parallel);
   - higher-priority user stories (P1 / MVP) land in earlier bolts.
4. Validate: no cycle; every user story from `spec.md` is in at least one unit; no unit depends on a unit in a later bolt.
5. **GATE**. On approval mark 2.7/2.9 `[x]`, fill the Construction per-unit table in `STATE` with one row per unit, audit.