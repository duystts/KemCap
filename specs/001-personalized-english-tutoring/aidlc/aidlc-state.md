# AI-DLC State — Nền tảng đào tạo tiếng Anh 1 kèm 1 cá nhân hóa

- **Feature dir**: specs/001-personalized-english-tutoring
- **Profile**: classic
- **Workspace**: greenfield
- **Created**: 2026-10-17
- **Current stage**: Construction — U4 unit gate (`/speckit-aidlc-unit-gate`)

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
| U1 Nền tảng & walking skeleton (B1, 18–23/10; BE #1, FE #2) | [x] 2026-10-18 | [x] 2026-10-18 (37/37 task; T032 đạt 2026-10-18, T137 tối thiểu xong) | [x] 2026-10-18 **Verified (đóng)** — PASS_WITH_NOTES sau sửa R1–R10; điều kiện đóng hẳn trước hết B1: ~~CI/gitleaks~~ (xanh 2026-10-18, run 39099953407), ~~T032~~ (đạt 2026-10-18, GV + HV cùng room, cam/mic ổn); deploy HTTPS chuyển sang U5 | build-test-U1.md |
| U2 Hồ sơ, gói, ghi danh, thanh toán (B2, 24–26/10; BE #1 + #4 billing, FE #4) | [x] 2026-10-18 — phạm vi (c): toàn bộ BE do #1/Claude (gồm billing T104–T110 vốn của #4); FE T052–T055, T114 để #4 | [x] 2026-10-18 (21/21 task BE; verify xanh 29 unit + 45 IT; T111 đổi sang gọi đồng bộ activateOnFirstPayment) | [x] 2026-10-18 **Verified** — PASS_WITH_NOTES sau sửa R1–R4, F1, F2, F6 (30 unit + 47 IT); R5–R7, F3, F5 → U8; FE U2 (T052–T055, T114) còn mở cho #4 | build-test-U2.md | |
| U3 Lịch, đặt lịch, trạng thái buổi, phòng học (B3, 27/10–1/11; BE #1, FE #2) | [x] 2026-10-18 — BE T056–T074 do Claude; FE T075–T079 để #2 | [x] 2026-10-18 (19/19 task BE; verify xanh 54 unit + 67 IT; sửa bug refresh làm mất thay đổi bộ đếm cùng transaction) | [x] 2026-10-18 **Verified** — PASS_WITH_NOTES sau sửa R1–R5, R7, R9, F5, F7 (57 unit + 68 IT); R6, R8, F1, F3, F6 → U8; F4 go/no-go LiveKit → #2 (1/11); FE U3 (T075–T079) còn mở cho #2 | build-test-U3.md | |
| U4 Năng lực, mục tiêu, lộ trình (B3, 27–28/10; BE #3, FE #3) | [ ] | [ ] | [ ] | |
| U5 Thông báo, nhắc, vận hành/sao lưu (B4, 2–4/11; BE #1, FE #3) | [ ] | [ ] | [ ] | |
| U7 Quyền riêng tư FR-030/031 (B5, 5–7/11; BE #1, FE #2 + #4) | [ ] | [ ] | [ ] | |
| U8 Gia hạn, công nợ, báo cáo (B5, 5–6/11; BE #4, FE #4) | [ ] | [ ] | [ ] | |
| U6 Phiếu đánh giá, bài tập, tiến bộ, đánh giá sao (B6, 8–10/11; BE #1, FE #3) | [ ] | [ ] | [ ] | |

### Construction (once)
- [ ] 3.6 Build & Test toàn bộ (`verify --all`)
- [ ] Converge (`/speckit-converge`)

## Next step

`/speckit-aidlc-unit-gate` cho U4 (B3, hồ sơ năng lực, mục tiêu, lộ trình; D1: BE do #3 — hỏi lại phạm vi) → `/speckit-implement` → `/speckit-aidlc-verify`. Song song: #2 làm FE U3, #4 làm FE U2.
