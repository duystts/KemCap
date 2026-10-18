# Task → Unit Map — Nền tảng đào tạo tiếng Anh 1 kèm 1 cá nhân hóa

**Nguồn**: [tasks.md](../tasks.md) (sinh lại 2026-10-18, 146 task) · [unit-of-work.md](./unit-of-work.md) (bản 2, đã duyệt)
**Cách gán**: theo tag `(Người·Unit)` trên từng task. Task không có tag là task ổn định cuối (Phase 10, B7);
unit-of-work không định nghĩa unit tích hợp cuối, nên các task này nằm ở dòng **B7** và mục *Needs decision*.
File này không sửa `tasks.md`.

| Unit | Bolt | Task IDs | Count | Done |
|---|---|---|---|---|
| U1 Nền tảng & walking skeleton | B1 (18–23/10) | T001–T037 (+ T137 bản tối thiểu, xem dưới) | 37 | 0 |
| U2 Hồ sơ, gói, ghi danh, thanh toán | B2 (24–26/10) | T038–T055, T104, T105, T107–T111, T114 | 26 | 0 |
| U3 Lịch, đặt lịch, trạng thái buổi, phòng học | B3 (27/10–1/11) | T056–T079 | 24 | 0 |
| U4 Năng lực, mục tiêu, lộ trình | B3 (27–28/10) | T080–T090 | 11 | 0 |
| U5 Thông báo, nhắc, vận hành/sao lưu | B4 (2–4/11) | T132–T139, T144 | 9 | 0 |
| U7 Quyền riêng tư FR-030/031 | B5 (5–7/11) | T124–T131 | 8 | 0 |
| U8 Gia hạn, công nợ, báo cáo | B5 (5–6/11) | T106, T112, T113, T115, T116, T118, T121, T123 | 8 | 0 |
| U6 Phiếu đánh giá, bài tập, tiến bộ, đánh giá sao | B6 (8–10/11) | T091–T103, T117, T119, T120, T122 | 17 | 0 |
| B7 ổn định (không thuộc unit) | B7 (11/11) | T140–T143, T145, T146 | 6 | 0 |
| **Tổng** | | | **146** | **0** |

## Kiểm tra

- Mỗi task ID xuất hiện đúng một lần: **đạt** (146/146, không trùng, không thiếu).
- Phụ thuộc giữa các bolt: **2 vi phạm** (xem dưới).

## Vi phạm thứ tự bolt

| # | Task (bolt) | Chờ task (bolt sau) | Mức | Đề xuất |
|---|---|---|---|---|
| V1 | T125 `AnonymizationIT` — U7, B5 | T100 (listener ẩn danh hóa phiếu/bài tập), T120 (listener feedback) — U6, B6 | Cứng: test E2E không kiểm được phần learning-report/feedback ở B5 | Ở B5, T125 chỉ kiểm phần đã có (identity, catalog, scheduling, learning-profile, notification, báo cáo SC-007); thêm assert phần phiếu/bài tập/feedback vào T125 khi U6 merge (cổng B6). Ghi vào DoD U6: "mở rộng T125" |
| V2 | T045 filter `needsConsulting` — U2, B2 | T085 `LearningProfileApi` — U4, B3 | Mềm: T045 đã ghi "trước khi có thì bỏ qua tham số" | Giữ nguyên; nối filter khi U4 merge (cổng B3) |

**Đã duyệt 2026-10-18**: V1 xử lý theo đề xuất (T125 kiểm phần có ở B5, mở rộng assert phiếu/bài tập/feedback khi U6 merge — thuộc DoD cổng B6); V2 giữ nguyên; Needs decision theo đề xuất (T144 → U5, còn lại B7).

## Needs decision (đã chốt)

Task Phase 10 không có tag unit. Đề xuất gán:

| Task | Nội dung | Đề xuất |
|---|---|---|
| T140 | Rà soát bảo mật + `AuthorizationMatrixIT` | B7 (xuyên mọi module) |
| T141 | k6 hiệu năng / SC-010 mức backend | B7 (#2 viết từ B4 theo lịch) |
| T142 | Playwright smoke | B7 (#2 viết từ B5) |
| T143 | Mobile, Lighthouse, usability | B7 |
| T144 | Diễn tập khôi phục sao lưu | **U5** (cùng T138; DoD vận hành) — hoặc B7 |
| T145 | README gốc | B7 |
| T146 | Chạy 24 bước quickstart, tổng duyệt | B7 |

## Điều chỉnh tại cổng U1 (2026-10-18, người dùng duyệt)

- **T137 tách đôi**: bản tối thiểu (backend `Dockerfile` multi-arch, `deploy/docker-compose.prod.yml` backend + postgres + caddy, `deploy/Caddyfile`, deploy skeleton lên Oracle VM, HTTPS) làm trong **U1** để phát hiện sớm rủi ro hosting; log rotation, healthcheck ping và hoàn thiện `deploy/README.md` vẫn ở **U5**. T137 chỉ đánh `[x]` khi phần U5 xong.
- **T032** (spike LiveKit hai trình duyệt) và **thao tác deploy thật lên VM** do người dùng thực hiện tay (cần tài khoản/trình duyệt); Claude chuẩn bị code + hướng dẫn.
