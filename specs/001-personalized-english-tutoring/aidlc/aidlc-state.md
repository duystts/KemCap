# AI-DLC State — Nền tảng đào tạo tiếng Anh 1 kèm 1 cá nhân hóa

- **Feature dir**: specs/001-personalized-english-tutoring
- **Profile**: classic
- **Workspace**: greenfield
- **Created**: 2026-10-17
- **Current stage**: Construction — backend và FE xong trên `main` (2026-11-08), còn T143 (phần usability), T146 và build & test toàn bộ

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
| U2 Hồ sơ, gói, ghi danh, thanh toán (B2, 24–26/10; BE #1 + #4 billing, FE #4) | [x] 2026-10-18 — phạm vi (c): toàn bộ BE do #1/Claude (gồm billing T104–T110 vốn của #4); FE T052–T055, T114 để #4 | [x] 2026-10-18 (21/21 task BE; verify xanh 29 unit + 45 IT; T111 đổi sang gọi đồng bộ activateOnFirstPayment) | [x] 2026-10-18 **Verified** — PASS_WITH_NOTES sau sửa R1–R4, F1, F2, F6 (30 unit + 47 IT); R5–R7, F3, F5 → U8; FE U2 xong 2026-11-08 do leader (T052–T055 tick ở PR #28, T114 PR #31) | build-test-U2.md | |
| U3 Lịch, đặt lịch, trạng thái buổi, phòng học (B3, 27/10–1/11; BE #1, FE #2) | [x] 2026-10-18 — BE T056–T074 do Claude; FE T075–T079 để #2 | [x] 2026-10-18 (19/19 task BE; verify xanh 54 unit + 67 IT; sửa bug refresh làm mất thay đổi bộ đếm cùng transaction) | [x] 2026-10-18 **Verified** — PASS_WITH_NOTES sau sửa R1–R5, R7, R9, F5, F7 (57 unit + 68 IT); R6, R8, F1, F3, F6 → U8; F4 go/no-go LiveKit → #2 (1/11); FE U3 xong 2026-11-08 do leader (T078 PR #17, T079 PR #23, LiveKit thật chạy được; T075–T077 PR #29) | build-test-U3.md | |
| U4 Năng lực, mục tiêu, lộ trình (B3, 27–28/10; BE #3, FE #3) | [x] 2026-10-18 — BE T080–T087, T090 do Claude (thay D1); FE T088–T089 để #3; OQ-4 giải bằng SPI GoalStatusProvider | [x] 2026-10-18 (9/9 task BE; verify xanh 61 unit + 73 IT; smoke seed + cờ Cần tư vấn) | [x] 2026-10-18 **Verified** — PASS_WITH_NOTES sau sửa R1–R5, F1, F3–F5, R7 (64 unit + 73 IT); R6, F2, F6, F7 → U8; FE U4 xong 2026-11-08 do leader (T088 tick ở PR #28, T089 PR #32) | build-test-U4.md | |
| U5 Thông báo, nhắc, vận hành/sao lưu (B4, 2–4/11; BE #1, FE #3) | [x] 2026-10-21 — BE T132–T135, T137–T139, T144 + listener StudentAnonymized + R11/R12/R18 (từ U1) do Claude, dùng BE PR #6 làm nháp; T136 FE để #3 (PR #6). Chốt: hủy buổi chỉ báo bên còn lại (`cancelledBy`); đổi GV gộp 1 thông báo (`SessionCancelled.cause` + event `ReassignmentSessionsCancelled` — đổi contract); backup R2 bắt buộc, endpoint cấu hình được, 02:00 giờ VN; LiveKit Cloud free + Brevo free; deploy VPS VN theo tháng **gần ngày demo (~5/11)**, Caddy phục vụ FE + API cùng domain, cấu hình tối thiểu cho người dùng (điền `.env` + 1 lệnh) | [x] 2026-10-21 (T132–T135, T138, T139 + StudentAnonymized + R11/R12/R18; T137/T144 code + diễn tập local đạt, chờ deploy VPS; verify xanh 89 unit + 82 IT; sửa thêm: dedupe dispatch dùng `ON CONFLICT` (race gửi trùng có từ U1), tắt mail health indicator) | [x] 2026-10-21 **Verified** — PASS_WITH_NOTES sau sửa R1 (chọn b: demo laptop 0 đ), R2, R3 (một phần), R4, R6, R11, R15, F3 (88 unit + 83 IT); chuyển U7: R3 còn lại, R10 · U8: R5, R7, R14, F1, F2, F4–F7 · T140: R13 · U6: chấm lại bài · khi deploy VPS: R8, R9, R12; FE T136 xong do leader (tick ở PR #28) | build-test-U5.md |
| U7 Quyền riêng tư FR-030/031 (B5, 5–7/11; BE #1, FE #2 + #4) | [x] 2026-11-03 — BE T124, T125, T127–T129 + tick T126 (đã có từ U5) + R14, R3 còn lại, R10, F5 do Claude; FE T130, T131 để #4, R17 (FE) để #2. Chốt: T125 so SC-007 bằng dữ liệu sẵn có, U8 chạy lại với 3 báo cáo thật; `anonymize` lần hai → 409 `ANONYMIZED`, phát lại event không lỗi; thu hồi cả token INVITE + PASSWORD_RESET; R10 chỉ làm tài liệu + log `studentId` | [x] 2026-11-03 (T124–T129 + R14, R3 còn lại, R10, F5; verify xanh 94 unit + 96 IT; thêm migration `notification_dispatch.about_student_id` cho R3 — sai lệch so với plan "không cần migration"; `DispatchService` bỏ qua địa chỉ `@invalid`; xóa `password_hash`) | [x] 2026-11-03 **Verified** — PASS_WITH_NOTES sau sửa F1, R1–R4, F4 (3 cột), F2 (một phần), N1, N3, N5, N6(a,b), R10–R12 (95 unit + 105 IT; thêm migration `user_account.version`); N1/N3/N5/N6 chưa qua review độc lập lần 3; chuyển U6, U8, T140, trước deploy, pháp lý theo build-test-U7.md mục 7; FE T130, T131 xong do leader (PR #22, bản tóm tắt đủ ở PR #24); R17 (FE) xong 2026-11-08 do leader, trang `/policy` | build-test-U7.md |
| U8 Gia hạn, công nợ, báo cáo (B5, 5–6/11; BE Claude thay D1, FE #4) | [x] 2026-11-03 — BE T106, T112, T113, T118, T121 + T125 với báo cáo thật, R8, N2 (U7), R5, R6, R7, F3 (U2), R6, R8 (U3), F1 (U5) do Claude; FE T115, T116, T123 để #4. Chốt: ghi danh đã hủy không tính vào tổng công nợ và `hasDebt`, vẫn giữ receivable và số đã thu; đã thu lọc theo `paid_at` trong kỳ, công nợ là số dư hiện tại, thu vượt không trừ công nợ ghi danh khác; test bổ sung còn lại → T140/B7; U5 R5, R7, U4 R6 hoãn sau demo | [x] 2026-11-03 (T106, T112, T113, T118, T121 + T125 báo cáo thật, R8, N2, U2 R5–R7, F3, U3 R6, R8, U5 F1; clean verify xanh 95 unit + 125 IT; migration `enrollment` cancel audit; khóa advisory theo GV; `GET /sessions` phân trang, mặc định 200) | [x] 2026-11-04 **Verified** — PASS_WITH_NOTES sau sửa F1, R1, F2–F4, R2, R5, R7, R10, R11 (chạy bù khi khởi động), R21, R22 (97 unit + 130 IT, smoke chạy bù đạt); người dùng ủy quyền tự duyệt, sẽ review sau; chuyển tiếp theo build-test-U8.md mục 7; FE xong do leader (T115 PR #13, T116 PR #25, T123 PR #21) | build-test-U8.md |
| U6 Phiếu đánh giá, bài tập, tiến bộ, đánh giá sao (B6, 8–10/11; BE #1, FE #3) | [x] 2026-11-04 (người dùng ủy quyền tự duyệt, sẽ review sau) — BE T091–T100, T117, T119, T120 + mở rộng T125 (N6c, F6, F12 từ U7), chấm lại bài (R6 U5: chốt không cho chấm lại trong MVP) do Claude; FE T101–T103, T122 để #3 | [x] 2026-11-04 (T091–T100, T117, T119, T120 + mở rộng T125; migration V7, V9) | [x] 2026-11-04 **Verified** — PASS_WITH_NOTES (98 unit + 159 IT, smoke đạt), tự duyệt theo ủy quyền; ngoại lệ constitution V ở plan.md Complexity Tracking được leader chấp nhận 2026-11-04; FE xong do leader (T101 PR #14, T102 PR #18, T103 PR #20, T122 PR #19) | build-test-U6.md |

### Construction (once)
- [~] 3.6 Build & Test toàn bộ (`verify --all`) — backend đạt 2026-11-04 (build-test-all.md, báo cáo này chưa chạy lại sau khi có FE); k6 (T141) đạt 2026-11-05; Playwright smoke (T142) đạt 10/10 trên `main` 2026-11-08; CI xanh trước khi merge các PR FE; mobile và Lighthouse (T143) đạt phần đo, xem PR #34
- [x] Converge (`/speckit-converge`) — 2026-11-08: thêm Phase 11 (T147–T154), không có CRITICAL

## Next step

Cập nhật 2026-11-08. Từ 2026-11-05 #2–#4 không còn tham gia; leader làm cả FE, mọi PR do leader tự merge (ngoại lệ ở `plan.md` Complexity Tracking). 144/146 task đã tick.

Còn lại trước demo 2026-11-12:

1. **T143** (phần usability): nhờ 1–2 người đặt lịch trên điện thoại (SC-001, < 1 phút) và điền phiếu đánh giá (SC-008, < 3 phút), bấm giờ, trong buổi tổng duyệt 11/11. Đạt thì tick. Phần đo bằng script đã đạt: 4 lần chạm, khoảng 40 giây; LCP mobile lần chậm nhất 1,44–2,47 s.
2. **T146**: tổng duyệt 24 bước quickstart 2026-11-11, đặt `JWT_SECRET` riêng, bấm thử link trong email (F4 của build-test-all.md).
3. ~~**R17 (FE, U7)**~~: xong 2026-11-08, trang `/policy` công khai (bản tóm tắt, chờ pháp lý thay nội dung chính thức), link mở tab mới ở accept-invite và consent gate.
4. `verify --all` gồm FE: chạy lại 2026-11-08, PASS_WITH_NOTES (100 unit + 185 IT, FE 106/106, e2e 10/10; build-test-all.md mục 6). Converge xong: Phase 11 T147–T154, phần lớn để sau demo; T151 (nội dung chính sách) cần pháp lý.
5. Ngoài code: hỏi pháp lý về mốc 16 tuổi và việc giữ tên người yêu cầu sau ẩn danh hóa; leader xác nhận đã đọc xong các commit đi thẳng `main` (U6–U8, T140).

T147 (quyết định "Xếp bù" không được lưu) sửa 2026-11-09, trước demo theo yêu cầu của leader. T148 (phiếu treo sau đổi giáo viên, U6 R5) làm 2026-11-09: giáo viên cũ nộp không kèm chủ đề, giáo vụ đóng phiếu kèm lý do. T150 (ghi múi giờ trong email và thông báo, U5 R7) làm 2026-11-09. T152 (N+1 khi đọc lộ trình, U4 R6) làm 2026-11-09, kèm sửa e2e bước 15 (helper `createUser` gọi API trước khi tạo xong). Sau demo: T149, T153, T154 (U5 R5, cảnh báo trợ năng LiveKit, review lại các PR).
