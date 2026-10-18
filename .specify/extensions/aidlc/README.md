# Extension `aidlc` cho Spec Kit

Đưa các stage **Inception + Construction** của [AWS AI-DLC](https://github.com/awslabs/aidlc-workflows) vào luồng Spec-Driven Development của [Spec Kit](https://github.com/github/spec-kit), không cần cài `aidlc` CLI và không sửa skill core của Spec Kit.

Spec Kit lo artifact (spec, plan, tasks) và constitution. AI-DLC thêm: chọn profile theo loại việc, user stories + persona, NFR, chia **units of work** có DAG, làm từng unit với **cổng duyệt của người**, build/test + review độc lập, `aidlc-state.md` và `audit.md`.

## Ánh xạ stage

| AI-DLC | Lệnh | Chạy khi nào |
|---|---|---|
| 0.x Workspace detection, State | `/speckit-aidlc-init` | hook bắt buộc sau `/speckit-specify` |
| 2.1 Reverse Engineering | `/speckit-aidlc-reverse` | chạy tay, chỉ brownfield |
| 2.3 Requirements | `/speckit-specify`, `/speckit-clarify` | core |
| 2.4 User Stories | `/speckit-aidlc-stories` | hook tùy chọn sau specify |
| 2.6/2.8 Domain, Contracts | `/speckit-plan` | core |
| 3.2/3.3 NFR | `/speckit-aidlc-nfr` | hook tùy chọn sau plan |
| 2.7/2.9 Units, Delivery | `/speckit-aidlc-units` | hook bắt buộc sau plan |
| Task → Unit | `/speckit-aidlc-map-tasks` | hook bắt buộc sau tasks |
| 3.5 Plan approval per unit | `/speckit-aidlc-unit-gate` | hook bắt buộc trước implement |
| 3.6 Build & Test + review | `/speckit-aidlc-verify` | hook bắt buộc sau implement |
| Trạng thái | `/speckit-aidlc-status` | chạy tay |

Profile: `express` · `classic` (mặc định) · `bugfix` · `refactor`. Hook tự bỏ qua stage không thuộc profile.

Artifact nằm cạnh feature: `specs/<feature>/aidlc/` (`aidlc-state.md`, `audit.md`, `stories.md`, `nfr.md`, `unit-of-work.md`, `task-map.md`, `build-test-<unit>.md`). `tasks.md` không bị sửa bởi extension; mapping nằm ở `task-map.md`.

## Cài đặt

```powershell
specify extension add --dev extensions/aidlc
Copy-Item extensions/aidlc/agents/*.md .claude/agents/
specify workflow add extensions/aidlc/workflows/aidlc-sdd.yml
```

Khởi động lại Claude Code để nhận skill mới.

## Cách dùng

**Tương tác (khuyên dùng):** chạy như Spec Kit bình thường, hook tự gọi stage AI-DLC và dừng ở cổng *Duyệt / Sửa / Hỏi thêm*:

```
/speckit-specify <mô tả> profile=classic
/speckit-clarify
/speckit-plan
/speckit-tasks
/speckit-implement        # mỗi lần = 1 unit: unit-gate → code → verify
/speckit-implement        # unit tiếp theo...
/speckit-aidlc-verify --all
/speckit-converge
```

**Tự động:** `specify workflow run aidlc-sdd -i spec="..." -i profile=classic`. Lệnh không dừng chờ; các bước `gate` của workflow là cổng duyệt, rồi bước `approve-*` ghi nhận duyệt vào state/audit.

**Feature có sẵn:** `/speckit-aidlc-init specs/001-... profile=classic`, rồi `/speckit-aidlc-units`, `/speckit-aidlc-map-tasks`, `/speckit-aidlc-status`.

## Subagent

`agents/` chứa 5 vai trò AI-DLC cho Claude Code: `aidlc-product`, `aidlc-architect`, `aidlc-developer`, `aidlc-quality`, `aidlc-reviewer`. Lệnh giao phần soạn thảo cho subagent; orchestrator (phiên chính) giữ cổng duyệt, state và audit. Reviewer chỉ đọc và chưa từng viết code đó, nên review độc lập.
