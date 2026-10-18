# AI-DLC State — Nền tảng đào tạo tiếng Anh 1 kèm 1 cá nhân hóa

- **Feature dir**: specs/001-personalized-english-tutoring
- **Profile**: classic
- **Workspace**: greenfield
- **Created**: 2026-10-17
- **Current stage**: Construction — U2 unit gate (`/speckit-aidlc-unit-gate`)

## Stage progress

Ký hiệu: `[ ]` chưa làm · `[~]` đang chờ duyệt · `[x]` đã duyệt · `[-]` bỏ qua theo profile

### Inception
- [-] 2.1 Reverse Engineering (`reverse`) — chỉ brownfield (workspace greenfield)
- [x] 2.3 Requirements Analysis (`/speckit-specify` + `/speckit-clarify`)
- [x] 2.4 User Stories (`stories`) — duyệt 2026-10-18
- [x] 2.6/2.8 Domain & Contract Design (`/speckit-plan`)
- [x] 3.2/3.3 NFR Requirements & Design (`nfr`) — duyệt 2026-10-18
- [x] 2.7/2.9 Units Generation & Delivery Planning (`units`) — duyệt 2026-10-18 (phương án S2: K1–K3, P1, P2, D1, D4)
- [x] Tasks + task→unit map (`/speckit-tasks` + `map-tasks`) — duyệt 2026-10-18 (146 task, [task-map.md](./task-map.md))

### Construction (per unit)

| Unit | Plan approved | Implemented | Verified | Verdict |
|------|---------------|-------------|----------|---------|
| U1 Nền tảng & walking skeleton (B1, 18–23/10; BE #1, FE #2) | [x] 2026-10-18 | [x] 2026-10-18 (36/37 task; T032 chờ thử 2 trình duyệt, T137 tối thiểu xong) | [x] 2026-10-18 Verified (code) — PASS_WITH_NOTES sau sửa R1–R10; điều kiện đóng hẳn trước hết B1: CI/gitleaks chạy, deploy HTTPS Oracle VM, T032 (#1 làm tay) | build-test-U1.md |
| U2 Hồ sơ, gói, ghi danh, thanh toán (B2, 24–26/10; BE #1 + #4 billing, FE #4) | [ ] | [ ] | [ ] | |
| U3 Lịch, đặt lịch, trạng thái buổi, phòng học (B3, 27/10–1/11; BE #1, FE #2) | [ ] | [ ] | [ ] | |
| U4 Năng lực, mục tiêu, lộ trình (B3, 27–28/10; BE #3, FE #3) | [ ] | [ ] | [ ] | |
| U5 Thông báo, nhắc, vận hành/sao lưu (B4, 2–4/11; BE #1, FE #3) | [ ] | [ ] | [ ] | |
| U7 Quyền riêng tư FR-030/031 (B5, 5–7/11; BE #1, FE #2 + #4) | [ ] | [ ] | [ ] | |
| U8 Gia hạn, công nợ, báo cáo (B5, 5–6/11; BE #4, FE #4) | [ ] | [ ] | [ ] | |
| U6 Phiếu đánh giá, bài tập, tiến bộ, đánh giá sao (B6, 8–10/11; BE #1, FE #3) | [ ] | [ ] | [ ] | |

### Construction (once)
- [ ] 3.6 Build & Test toàn bộ (`verify --all`)
- [ ] Converge (`/speckit-converge`)

## Next step

`/speckit-aidlc-unit-gate` cho U2 (B2, 24–26/10) → `/speckit-implement` → `/speckit-aidlc-verify`. Cổng U2 kiểm thêm quickstart bước 1 (chuyển từ U1).
