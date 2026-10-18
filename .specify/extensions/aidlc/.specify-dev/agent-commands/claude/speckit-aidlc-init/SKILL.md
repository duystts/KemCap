---
name: speckit-aidlc-init
description: 'AI-DLC workspace detection: choose a profile, create aidlc-state.md and audit.md for the current feature'
compatibility: Requires spec-kit project structure with .specify/ directory
metadata:
  author: phamanhduy
  source: aidlc:commands/speckit.aidlc.init.md
---

# AI-DLC Init (stages 0.x: workspace detection & state initialization)

Runs automatically after `/speckit-specify` (mandatory `after_specify` hook) and can be run by hand on an existing feature.

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

1. If `STATE` already exists, read it, report the current profile/stage, and stop unless the user input contains `--reset` (then keep `AUDIT`, recreate `STATE`).
2. **Workspace detection**: brownfield if the repo already contains application source code outside `specs/`, `.specify/`, `.claude/`, `extensions/` (look for `src/`, `backend/`, `frontend/`, `pom.xml`, `package.json`, `build.gradle`, `*.csproj`, ...); otherwise greenfield.
3. **Profile selection** (adaptive, like AI-DLC):
   - If the user input (or the `/speckit-specify` input earlier in this session) contains `profile=<name>` or one of `express|classic|bugfix|refactor`, use it.
   - Otherwise read `spec.md` and propose one:
     - `express`: small change, at most 1 user story, no new module, no new NFR.
     - `classic`: new capability, several user stories or modules (default).
     - `bugfix`: fixing incorrect behaviour; needs a reproducing test first.
     - `refactor`: internal change, behaviour must stay identical.
   - Show the proposed profile with the stages it runs/skips (table below) and **GATE** on it.

   | Stage | express | classic | bugfix | refactor |
   |---|---|---|---|---|
   | reverse (brownfield only) | - | yes | yes | yes |
   | stories | - | yes | - | - |
   | nfr | - | yes | - | yes |
   | units | single unit U1 | yes | single unit U1 | yes |
   | map-tasks | all to U1 | yes | all to U1 | yes |
   | unit-gate | yes | yes | yes | yes |
   | verify | yes | yes | yes (reproducing test must now pass) | yes (all pre-existing tests still pass) |

4. Create `AIDLC_DIR`, write `STATE` from `TPL/aidlc-state-template.md` (fill name, dir, profile, workspace, date; mark skipped stages `[-]`; mark 2.3 Requirements `[x]` if `spec.md` exists, 2.6/2.8 `[x]` if `plan.md` exists, Tasks `[x]` if `tasks.md` exists) and create `AUDIT`.
5. If brownfield and the profile runs `reverse` and `AIDLC_DIR/reverse-engineering.md` does not exist, tell the user to run `/speckit-aidlc-reverse` before planning.
6. Audit `approved` with the chosen profile. Set **Next step** in `STATE` (usually `/speckit-clarify`, then `/speckit-aidlc-stories` for classic).