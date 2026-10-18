# NFR Requirements & Design — Nền tảng đào tạo tiếng Anh 1 kèm 1 cá nhân hóa (MVP)

**Nguồn**: spec.md (Success Criteria, Edge Cases, FR, Assumptions), plan.md (Technical Context,
Constitution Check), research.md, contracts/, data-model.md, constitution v1.0.1.
**Stage**: AI-DLC 3.2 + 3.3 (feature level) · **Ngày**: 2026-10-17 · **Trạng thái**: Draft, chờ duyệt

Quy ước:

- Trích dẫn dạng `file:dòng`, đường dẫn tương đối so với `specs/001-personalized-english-tutoring/`
  (constitution: `.specify/memory/constitution.md`, viết tắt `const`).
- **Mức ưu tiên**: **Must** = bắt buộc cho demo 2026-11-12 · **Should** = nên có, cắt được nếu trễ ·
  **Later** = ghi nhận, kiểm chứng sau demo.
- NFR có ghi **(đề xuất)** là yêu cầu chưa có trong plan/spec, do architect suy ra từ nguyên tắc
  được trích dẫn; cần team duyệt (xem §4).
- **Quy mô tham chiếu** (plan.md:44): demo ~10 GV / ~50 HV; quy mô thiết kế ~1.000 HV. "Dữ liệu quy
  mô thiết kế" dưới đây = 1.000 HV, 50 GV, 12 tháng lịch sử (~50.000 session) sinh bằng seed test.
- Mọi việc kiểm chứng dùng công cụ miễn phí, chạy local/CI; không thêm hạ tầng chạy thường trực
  (const:43-50).

---

## 1. NFR Requirements

### 1.1 Performance (PERF)

| ID | Loại | Yêu cầu đo được | Nguồn | Ưu tiên |
|----|------|-----------------|-------|---------|
| NFR-PERF-01 | Performance | Mọi API REST `/api/v1/**` (trừ upload/download tệp đi thẳng storage): **p95 < 500 ms**, **p99 < 1.000 ms** với dữ liệu quy mô thiết kế và 20 người dùng ảo đồng thời; tỷ lệ lỗi 5xx = 0% | plan.md:38, plan.md:44 | Must |
| NFR-PERF-02 | Performance | `GET /enrollments/{id}/slots` cho cửa sổ 30 ngày (GV có ≤ 20 quy tắc rảnh/tuần + ≤ 60 buổi đã đặt trong cửa sổ): **p95 < 500 ms**; `POST /sessions` **p95 < 500 ms** | SC-001 (spec.md:233), research.md:53-57 (R4), tasks.md:293 (T126) | Must |
| NFR-PERF-03 | Performance / Usability | Luồng đặt lịch: từ khi mở trang đặt lịch đến khi thấy xác nhận **≤ 60 giây** với 5/5 người thử; **≤ 4 thao tác** (chọn ngày → chọn giờ → xác nhận) | SC-001 (spec.md:233) | Must |
| NFR-PERF-04 | Performance / Usability | Phiếu đánh giá buổi học hoàn thành trong **< 3 phút** (trung vị) và ≤ 3 phút ở ≥ 90% lượt thử (≥ 2 giáo viên × 3 phiếu); `PUT /sessions/{id}/report` p95 < 500 ms | SC-008 (spec.md:240), FR-019 (spec.md:189) | Must |
| NFR-PERF-05 | Performance / Timeliness | Nhắc lịch: `SessionReminderDue` được phát **≤ 2 phút** sau mốc 24h/1h; email tương ứng ở trạng thái `SENT` **≤ 5 phút** sau mốc (khi SMTP hoạt động) | plan.md:38, FR-013 (spec.md:177), contracts/events-and-ports.md:23 | Must |
| NFR-PERF-06 | Performance / Timeliness | Nhắc phiếu quá hạn: `SessionReportOverdue` phát **≤ 60 phút** sau `due_at` (= `ends_at` + 24h); nhắc gia hạn phát trong lần chạy job hằng ngày (00:05 giờ VN) đầu tiên sau khi chạm ngưỡng | FR-019 (spec.md:189), FR-026 (spec.md:199), contracts/events-and-ports.md:18,25, tasks.md:251 (T109) | Must |
| NFR-PERF-07 | Performance (frontend) | **(đề xuất)** Trang học viên (đặt lịch, tiến bộ) trên mobile 4G mô phỏng (Lighthouse "Slow 4G"): **LCP ≤ 3 s**; bundle JS khởi tạo (gzip) **≤ 350 KB**, các trang nặng (FullCalendar, Recharts, LiveKit) tách chunk lazy | spec.md:246 (dùng tốt trên điện thoại qua trình duyệt) | Should |

### 1.2 Scalability & Capacity (SCAL)

| ID | Loại | Yêu cầu đo được | Nguồn | Ưu tiên |
|----|------|-----------------|-------|---------|
| NFR-SCAL-01 | Scalability (backend) | Backend chịu tải tương đương **200 buổi diễn ra đồng thời**: 400 lượt `POST /sessions/{id}/join` trong 60 s + 200 người dùng ảo gọi API đọc (lịch, phiếu, thông báo) trong 10 phút; **lỗi 0%**, **p95 < 500 ms** | SC-010 (spec.md:242), research.md:161-164 (R14) | Must |
| NFR-SCAL-02 | Scalability (video) | Năng lực video 200 phòng × 2 người (400 participant) **chỉ đạt khi đổi adapter/gói LiveKit** (trả phí hoặc tự host); MVP chỉ kiểm chứng việc đổi được bằng cấu hình, không kiểm chứng tải video | SC-010, plan.md:44-45, research.md:161-164, FR-029 (spec.md:208) | Later |
| NFR-SCAL-03 | Resource | Container backend chạy với giới hạn **≤ 1 GB RAM** (`mem_limit: 1g`) trong suốt bài tải NFR-SCAL-01: **0 lần OOM/restart**, heap sau GC ổn định (không tăng liên tục qua 10 phút) | plan.md:41 | Must |

### 1.3 Reliability & Data Integrity (REL)

| ID | Loại | Yêu cầu đo được | Nguồn | Ưu tiên |
|----|------|-----------------|-------|---------|
| NFR-REL-01 | Integrity | **0** cặp buổi chồng thời gian cho cùng giáo viên hoặc cùng học viên (trạng thái hiệu lực), kể cả khi N request đặt song song cùng khung giờ: đúng **1** thành công, N−1 nhận `409 SLOT_TAKEN` | SC-003 (spec.md:235), FR-010 (spec.md:174), spec.md:144, research.md:42-47 (R3), const:57 | Must |
| NFR-REL-02 | Integrity | Số buổi: với **100%** enrollment, `sessions_reserved` = số session `BOOKED` và `sessions_consumed` = số session `COMPLETED`+`STUDENT_NO_SHOW`+`CANCELLED_LATE` (+ `INTERRUPTED`→`COMPLETED`); không bao giờ `reserved + consumed > total`; hai luồng `reserve` buổi cuối → đúng 1 thành công | SC-007 (spec.md:239), FR-024 (spec.md:197), research.md:65-70 (R5), data-model.md:110,187-200, contracts/events-and-ports.md:30-32 | Must |
| NFR-REL-03 | Integrity | Công nợ: với **100%** receivable, `amount_paid_vnd` = tổng `payment.amount_vnd` (kể cả điều chỉnh âm) và công nợ = `amount_due_vnd − amount_paid_vnd`; bản ghi payment **không sửa/xóa được** (chỉ thêm điều chỉnh) | SC-007, FR-025 (spec.md:198), data-model.md:279-305, const:57 | Must |
| NFR-REL-04 | Reliability (event) | **0 event bị mất** khi listener lỗi hoặc app khởi động lại: mọi publication chưa hoàn thành được xử lý lại sau restart; trong vận hành bình thường **0** bản ghi `event_publication` chưa hoàn thành cũ hơn 15 phút | research.md:77-83 (R6), contracts/events-and-ports.md:8-9 | Must |
| NFR-REL-05 | Reliability (idempotency) | Mỗi thông báo (khóa `dedupe_key`) được gửi **tối đa 1 lần** dù job/listener chạy lại hoặc event được phát lại; mỗi session `COMPLETED` có **đúng 1** `session_report` | research.md:84-85, data-model.md:176,248,327, quickstart.md:54 (bước 6) | Must |
| NFR-REL-06 | Reliability (email) | Gửi email lỗi được thử lại **tối đa 3 lần** với backoff (1 phút, 5 phút, 15 phút); sau đó `FAILED` kèm `error`; lỗi email **không** làm rollback giao dịch nghiệp vụ (đặt lịch vẫn thành công) | data-model.md:322-332, FR-013 | Must |
| NFR-REL-07 | Reliability (job) | Mỗi job định kỳ chạy **đúng 1 instance** tại một thời điểm (ShedLock, `lockAtMostFor` = 5 phút); job chạy lại không tạo hiệu ứng trùng (dựa trên `reminder_*_at`, `overdue_notified_at`, `renewal_*_notified_at`) | research.md:77-81, tasks.md:58 (T010), data-model.md:107,176,256 | Must |
| NFR-REL-08 | Reliability (concurrency) | Cập nhật đồng thời cùng bản ghi (enrollment, session, learning_path) **không mất cập nhật**: bên thua nhận `409 CONCURRENT_MODIFICATION` hoặc lỗi nghiệp vụ tương ứng, không có 500 | research.md:69, contracts/rest-api.md:129 | Must |
| NFR-REL-09 | Availability | Backend luôn chạy (không ngủ): job nhắc lịch chạy đủ **1.440 lần/ngày**; **(đề xuất)** uptime đo bằng health check ngoài ≥ **99%** theo tháng trong giai đoạn demo | plan.md:151, research.md:154-156 (R13) | Should |
| NFR-REL-10 | Recoverability | **(đề xuất)** Sao lưu PostgreSQL: **RPO ≤ 24 h**, **RTO ≤ 4 h**; giữ ≥ 7 bản sao lưu hằng ngày ở nơi lưu trữ khác VM; diễn tập khôi phục ≥ 1 lần trước 2026-11-11 | const:64,76 (lý do IV, V: dữ liệu tài chính & cá nhân), research.md:149-152 (DB trên 1 VM) | Must |
| NFR-REL-11 | Reliability (luồng sau buổi) | **100%** buổi chuyển `COMPLETED` có `session_report` `PENDING` (với `due_at` đúng) trong **≤ 5 phút**; học viên/giáo viên nhận thông báo liên quan | SC-005 (spec.md:237), FR-019, contracts/events-and-ports.md:24 | Must |

### 1.4 Security (SEC)

| ID | Loại | Yêu cầu đo được | Nguồn | Ưu tiên |
|----|------|-----------------|-------|---------|
| NFR-SEC-01 | AuthZ | **100%** endpoint trong `contracts/rest-api.md` (trừ nhóm public `auth/*`, `/actuator/health`, Swagger chỉ ở profile local) yêu cầu JWT hợp lệ và đúng vai trò; ma trận test vai trò × endpoint bao phủ **100%** endpoint, **0** lỗi | const:68-69, FR-001 (spec.md:159), contracts/rest-api.md:7-8, tasks.md:288 (T121) | Must |
| NFR-SEC-02 | AuthZ (data scope) | Giáo viên truy cập dữ liệu học viên không được phân công, học viên truy cập dữ liệu người khác → **404 `NOT_FOUND`** (không lộ sự tồn tại); **0** rò rỉ qua danh sách/lọc/báo cáo/URL tệp | const:70, FR-003 (spec.md:161), plan.md:57, contracts/rest-api.md:122, quickstart.md:63 | Must |
| NFR-SEC-03 | Credential | Mật khẩu băm **BCrypt cost ≥ 10**; chính sách ≥ 8 ký tự có chữ và số, kiểm tra ở backend; **0** mật khẩu/ token thô trong DB (token lưu SHA-256) | const:71, contracts/rest-api.md:32, data-model.md:24,42 | Must |
| NFR-SEC-04 | Session/Token | Access token **15 phút**; refresh token **7 ngày**, xoay vòng mỗi lần dùng, dùng lại token cũ → 401; cookie `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth`; token mời **72 h**, reset **30 phút**, dùng 1 lần; user `INACTIVE` bị thu hồi refresh token ngay | research.md:28-36 (R2), data-model.md:32-33,43, tasks.md:70 (T019) | Must |
| NFR-SEC-05 | Secret | Secret (JWT ≥ **256 bit**, SMTP, LiveKit, R2, DB) chỉ qua biến môi trường; **0** secret trong Git (quét tự động trên mỗi PR) | const:71, quickstart.md:69 | Must |
| NFR-SEC-06 | Abuse | **(đề xuất)** Chống dò mật khẩu: `POST /auth/login` sai **5 lần/15 phút** theo email (hoặc 20 lần/15 phút theo IP) → `429` trong 15 phút; `forgot-password` ≤ **3 lần/giờ/email** (vẫn trả 202) | const:66-76 (V), contracts/rest-api.md:23 | Should |
| NFR-SEC-07 | Transport | **100%** lưu lượng ngoài qua HTTPS (TLS từ Caddy, HTTP → 301); header HSTS; CORS chỉ cho `app.frontend-url` | tasks.md:69 (T018), tasks.md:289 (T122), research.md:150-152 | Must |
| NFR-SEC-08 | Video room | Token phòng chỉ cấp cho **đúng 2** người của buổi, room `session-{id}`, chỉ trong khung **[bắt đầu − 10 phút, kết thúc + 15 phút]**, hiệu lực token không quá `kết thúc + 15 phút`; người thứ ba / ngoài khung → từ chối (`404`/`403 ROOM_NOT_OPEN`) | FR-012 (spec.md:176), research.md:91-95 (R7), contracts/rest-api.md:68, quickstart.md:56 | Must |
| NFR-SEC-09 | File | Pre-signed URL hết hạn sau **10 phút**; tệp **≤ 20 MB**; chỉ định dạng pdf, doc(x), jpg, png, mp3, m4a, wav, webm; URL tải chỉ cấp cho HV chủ bài, GV được phân công, admin | research.md:113-116 (R9), tasks.md:63 (T015), FR-020 (spec.md:190) | Must |
| NFR-SEC-10 | Enumeration | `forgot-password` **luôn 202**; thông điệp đăng nhập sai không phân biệt "email không tồn tại" và "sai mật khẩu" | contracts/rest-api.md:23 | Must |

### 1.5 Privacy & Auditability (PRIV)

| ID | Loại | Yêu cầu đo được | Nguồn | Ưu tiên |
|----|------|-----------------|-------|---------|
| NFR-PRIV-01 | Retention | **0** lệnh xóa cứng hồ sơ người dùng/học tập trong code nghiệp vụ; ngừng kích hoạt = `status = INACTIVE` | const:72-73, FR-004 (spec.md:165), data-model.md:11 | Must |
| NFR-PRIV-02 | Logging | Log ứng dụng **không chứa**: mật khẩu, token (JWT/refresh/mời/reset/LiveKit), pre-signed URL, nội dung bài nộp, nhận xét; email chỉ ghi ở mức DEBUG | const:66-76 (V) | Must |
| NFR-PRIV-03 | Compliance | Xử lý dữ liệu cá nhân theo pháp luật VN (Nghị định 13/2023/NĐ-CP và Luật Bảo vệ dữ liệu cá nhân 2025 ⚠ verify hiệu lực/điều khoản): tối thiểu có **thông báo xử lý dữ liệu + ghi nhận đồng ý** khi kích hoạt tài khoản và **quy trình xử lý yêu cầu xóa hợp lệ**; nội dung cụ thể **chưa xác định** (xem Q2) | const:72-73, spec.md:256 | Must (thiết kế còn mở) |
| NFR-PRIV-04 | Audit trail | **100%** thay đổi nhạy cảm lưu ai/khi nào/lý do: thanh toán & điều chỉnh (`recorded_by`), đổi giáo viên (`teacher_assignment_history`), sửa lộ trình (`path_revision.reason` bắt buộc), đặt/hủy buổi (`booked_by`, `cancelled_by`, `cancel_reason`), sửa trạng thái kết thúc trong 7 ngày | FR-018 (spec.md:184), FR-025, FR-006, data-model.md:125-134,171-174,200-201,239,301 | Must |

### 1.6 Cost (COST)

| ID | Loại | Yêu cầu đo được | Nguồn | Ưu tiên |
|----|------|-----------------|-------|---------|
| NFR-COST-01 | Cost | Chi phí vận hành demo **0 VND/tháng**: mọi thành phần chạy trên gói free hoặc tự host | const:45, plan.md:41, research.md:147-157 (R13) | Must |
| NFR-COST-02 | Cost / Quota | **(đề xuất)** Sử dụng mỗi gói free **≤ 80%** hạn mức (email/ngày, phút participant LiveKit/tháng, dung lượng R2, giờ CI); kiểm tra hằng tuần trong giai đoạn demo | research.md:5-6 ("⚠ verify"), plan.md:150 | Should |
| NFR-COST-03 | Cost / YAGNI | **0** thành phần hạ tầng mới (broker, cache phân tán, microservice, hệ thống monitoring tự host) nếu chưa có số đo + mục Complexity Tracking trong plan | const:46-49, plan.md:55 | Must |

### 1.7 Maintainability (MAINT)

| ID | Loại | Yêu cầu đo được | Nguồn | Ưu tiên |
|----|------|-----------------|-------|---------|
| NFR-MAINT-01 | Modularity | `ApplicationModules.verify()` chạy trên mỗi PR, **0** vi phạm ranh giới (7 module: 6 nghiệp vụ + `shared` OPEN); **0** JOIN/quan hệ JPA chéo module | const:18-26, plan.md:53, data-model.md:8-9 | Must |
| NFR-MAINT-02 | Test | Mỗi luật nghiệp vụ trọng yếu (đặt lịch, chống trùng đồng thời, hủy/đổi, trừ/hoàn buổi, hết hạn gói, học phí/công nợ, phân quyền) có **≥ 1** test tự động; integration test dùng **PostgreSQL thật (Testcontainers)**, **0** test dùng H2; bug ở logic trọng yếu có test tái hiện trước khi sửa | const:55-62, plan.md:56 | Must |
| NFR-MAINT-03 | CI | CI trên mỗi PR: backend `./mvnw verify` + frontend lint/test/build; PR đỏ **không merge**; **(đề xuất)** thời gian CI ≤ **15 phút** để giữ merge trong 1–2 ngày | const:60-61,95-96, tasks.md:44 (T007) | Must (thời gian: Should) |
| NFR-MAINT-04 | Schema | **100%** thay đổi schema qua Flyway; migration đã merge **không sửa** (Flyway validate checksum fail nếu sửa) | const:74, data-model.md:12 | Must |
| NFR-MAINT-05 | Contract | Type API frontend sinh từ OpenAPI (`openapi-typescript`); **(đề xuất)** CI fail nếu `schema.d.ts` lệch với OpenAPI hiện tại | research.md:122-127 (R10), contracts/rest-api.md:10 | Should |
| NFR-MAINT-06 | Version | Spring Boot **4.1.x**, thư viện Spring theo BOM (không ghi cứng version); **0** dependency/runtime đã hết hỗ trợ OSS | const:80-85, research.md:12-22 (R1) | Must |

### 1.8 Portability / Provider-Agnostic (PORT)

| ID | Loại | Yêu cầu đo được | Nguồn | Ưu tiên |
|----|------|-----------------|-------|---------|
| NFR-PORT-01 | Provider swap | **100%** dịch vụ ngoài trong MVP (`VideoRoomProvider`, `EmailSender`, `FileStorage`) sau port, mỗi port có adapter `fake`; đổi adapter chỉ bằng `app.providers.*`; **0** import SDK nhà cung cấp (`io.livekit..`, `software.amazon..`, `jakarta.mail..`/`org.springframework.mail..`) ngoài package adapter | const:31-38, FR-029 (spec.md:208), contracts/events-and-ports.md:44-55 | Must |
| NFR-PORT-02 | Deploy | Image backend chạy trên **ARM64** (Oracle Always Free) và AMD64 (máy dev/CI); toàn bộ cấu hình môi trường qua biến môi trường | const:90-91, plan.md:33, tasks.md:289 (T122) | Must |
| NFR-PORT-03 | Data (AI-ready) | Dữ liệu FR-028 lưu **có kiểu** (CEFR enum, điểm `smallint` 1–5, điểm bài 0–10, id chủ đề), không lưu dạng văn bản tự do cho dữ liệu chấm điểm; lịch sử (assessment, path_revision) không bị ghi đè | FR-028 (spec.md:204), data-model.md:207-273 | Must |

### 1.9 Usability, Localization & Compatibility (UX)

| ID | Loại | Yêu cầu đo được | Nguồn | Ưu tiên |
|----|------|-----------------|-------|---------|
| NFR-UX-01 | Localization | **100%** nhãn UI, email, thông báo và thông điệp lỗi bằng tiếng Việt; **100%** mã lỗi trong `contracts/rest-api.md:114-129` có thông điệp tiếng Việt ở frontend | const:89, research.md:133-136 (R11), spec.md:246 | Must |
| NFR-UX-02 | Time zone | Thời điểm lưu `timestamptz` UTC; hiển thị theo múi giờ người xem; slot đúng khi GV và HV khác múi giờ, kể cả múi giờ có DST (ví dụ `Europe/London` tuần đổi giờ) | const:89, spec.md:148, research.md:53-57 | Must |
| NFR-UX-03 | Compatibility | Chạy đúng trên Chrome, Edge, Firefox, Safari (2 phiên bản mới nhất), desktop và mobile web; trang học viên ở viewport **360 px** không cuộn ngang; phòng học hoạt động trên Chrome/Safari mobile | plan.md:33, spec.md:246, tasks.md:291 (T124) | Must (phòng học mobile: Should) |

### 1.10 Observability & Operability (OBS)

| ID | Loại | Yêu cầu đo được | Nguồn | Ưu tiên |
|----|------|-----------------|-------|---------|
| NFR-OBS-01 | Health | `/actuator/health` trả trạng thái app + DB; Docker healthcheck dùng endpoint này, container tự restart khi unhealthy | tasks.md:69 (T018), plan.md:151 | Must |
| NFR-OBS-02 | Logging | **(đề xuất)** Log có cấu trúc (JSON) kèm `traceId` cho mỗi request; mọi 5xx được log kèm stack trace và `code`; log xoay vòng (≤ 5 tệp × 10 MB) để không đầy đĩa VM | const:43-50 (dùng sẵn Spring Boot, không thêm hạ tầng), plan.md:41 | Should |
| NFR-OBS-03 | Ops runbook | **(đề xuất)** Có truy vấn kiểm tra vận hành (runbook) cho: dispatch `FAILED`, `event_publication` chưa hoàn thành, phiếu `PENDING` quá hạn, lệch số buổi/công nợ (NFR-REL-02/03); chạy được trong ≤ 5 phút | SC-005, SC-007, NFR-REL-04 | Should |

**Tổng**: 53 NFR — PERF 7 · SCAL 3 · REL 11 · SEC 10 · PRIV 4 · COST 3 · MAINT 6 · PORT 3 · UX 3 · OBS 3
(trong đó 9 NFR có nhãn **(đề xuất)**, toàn phần hoặc một phần).

---

## 2. NFR Design

Với mỗi NFR: cơ chế đáp ứng, nơi áp dụng (module/port theo plan.md:81-124 và contracts), cách kiểm
chứng (loại test, metric, ngưỡng). Đường dẫn code là đường dẫn **dự kiến** theo plan/tasks (repo hiện
chưa có code — greenfield).

### 2.1 Performance & Scalability

| NFR | Cơ chế thiết kế | Module / Port | Cách kiểm chứng |
|-----|-----------------|---------------|-----------------|
| NFR-PERF-01 | `spring.jpa.open-in-view=false` (tasks.md:45); DTO projection cho danh sách; phân trang bắt buộc `size ≤ 100`; index theo cột lọc chính (`session(teacher_id, starts_at)`, `session(student_id, starts_at)`, `enrollment(student_id)`, `enrollment(teacher_id)`, `session_report(status, due_at)`, `notification(user_id, read_at)`) trong migration của từng module; HikariCP pool mặc định (10) | mọi module; `shared` (paging) | Bài tải **k6** (chạy local/CI thủ công, không thường trực) trên dữ liệu quy mô thiết kế, 20 VU × 10 phút: `http_req_duration p(95) < 500`, `p(99) < 1000`, `http_req_failed rate == 0` (threshold k6 → fail nếu vượt) |
| NFR-PERF-02 | Slot tính khi truy vấn (R4): 1 query rule + 1 query exception + 1 query session trong cửa sổ `[from, to]` (dùng index `teacher_id, starts_at`), sinh slot trong bộ nhớ bước 30 phút; giới hạn cửa sổ 30 ngày (`booking-horizon-days`) | `scheduling` (`internal/slots`) | Unit test thuật toán slot; k6 kịch bản `slots` + `book` p95 < 500 ms (T126); EXPLAIN xác nhận dùng index |
| NFR-PERF-03 | Slot picker hiển thị khung trống theo ngày (FullCalendar/danh sách), xác nhận một bước; lỗi `SLOT_TAKEN` tự làm mới danh sách slot | `features/scheduling` + `scheduling` | Test usability: 5 người thử (không phải dev) trên môi trường demo, bấm giờ; đạt nếu 5/5 ≤ 60 s. Playwright smoke (T125) đếm ≤ 4 thao tác |
| NFR-PERF-04 | Phiếu một màn hình: chủ đề lấy sẵn từ lộ trình ACTIVE (chọn nhiều), điểm 1–5 dạng nút, nhận xét + bài tập inline; lưu nháp ở client (react-hook-form) | `features/learning` + `learning` (`internal/report`) | Test usability với ≥ 2 GV × 3 phiếu: trung vị < 3 phút, ≥ 90% ≤ 3 phút; API p95 trong k6 |
| NFR-PERF-05 | Job `@Scheduled` mỗi phút + ShedLock trong `scheduling` quét `session` có `starts_at − 24h/1h ≤ now` và `reminder_*_at IS NULL`, set cờ + phát `SessionReminderDue`; `notification` nghe async (`@ApplicationModuleListener`) và gửi qua `EmailSender` | `scheduling` (`internal/jobs`), `notification`, port `EmailSender` | Integration test với `TestClock` (tasks.md:60-62): tiến đồng hồ đến mốc → event phát trong lần chạy kế tiếp; trên demo: so `notification_dispatch.sent_at − (starts_at − 1h)` ≤ 5 phút cho 100% bản ghi trong tuần thử |
| NFR-PERF-06 | Job mỗi giờ trong `learning` (phiếu `PENDING` có `due_at ≤ now`, `overdue_notified_at IS NULL`); job hằng ngày 00:05 `Asia/Ho_Chi_Minh` trong `catalog` (T109) | `learning`, `catalog` (`internal/jobs`) | Integration test `TestClock`; test T103 (đúng một lần mỗi lý do) |
| NFR-PERF-07 | Route-level code splitting (`React.lazy`) cho classroom, calendar, chart; Vite build mặc định | `frontend/src/app` | Lighthouse CLI (mobile, Slow 4G) trên 2 trang học viên: LCP ≤ 3 s; kiểm tra kích thước chunk từ `vite build` ≤ 350 KB gzip |
| NFR-SCAL-01 | Backend chỉ cấp token LiveKit (ký JWT cục bộ, không gọi mạng) → `join` rẻ; video không đi qua backend (plan.md:41); stateless JWT → không có session store | `scheduling` (port `VideoRoomProvider`) | k6 kịch bản "200 buổi": 400 `join` trong 60 s + 200 VU đọc 10 phút, adapter `livekit` (local dev server) hoặc `fake`; ngưỡng lỗi 0%, p95 < 500 ms. Kết quả ghi vào research.md R14 |
| NFR-SCAL-02 | Đổi adapter/endpoint qua `app.providers.video` + biến môi trường LiveKit (URL, key) — không đổi code | port `VideoRoomProvider` | Kiểm chứng chuyển đổi: chạy cùng test suite với `video=fake` và `video=livekit`; tải video thật: **không** kiểm chứng trong MVP |
| NFR-SCAL-03 | JVM `-XX:MaxRAMPercentage=70` (≈ 700 MB heap trong 1 GB), `-XX:+UseSerialGC` hoặc G1 mặc định; `mem_limit: 1g` trong `deploy/docker-compose.prod.yml`; virtual threads **không** bật mặc định (chưa có số đo) | `deploy/`, `backend/Dockerfile` | Trong bài tải SCAL-01: `docker stats` ≤ 1 GB, 0 restart; `/actuator/metrics/jvm.memory.used` sau GC không tăng liên tục |

### 2.2 Reliability & Data Integrity

| NFR | Cơ chế thiết kế | Module / Port | Cách kiểm chứng |
|-----|-----------------|---------------|-----------------|
| NFR-REL-01 | 2 exclusion constraint `btree_gist` (teacher, student) trên `session` với điều kiện trạng thái hiệu lực (data-model.md:179-185); service bắt `SQLState 23P01` → `409 SLOT_TAKEN`; kiểm tra sớm ở tầng ứng dụng chỉ để báo lỗi thân thiện | `scheduling` | Testcontainers IT: 10 luồng đặt cùng slot (cùng GV; và cùng HV khác GV) → đúng 1 thành công; quickstart bước 5; SQL kiểm tra 0 overlap trên dữ liệu demo |
| NFR-REL-02 | `EnrollmentApi.reserve/release/consume/consumeReleased` gọi **đồng bộ** trong cùng transaction đổi trạng thái session (`Propagation.MANDATORY`, tasks.md:124); `UPDATE … WHERE còn buổi` + `@Version`; CHECK `reserved + consumed ≤ total` | `catalog` (`EnrollmentApi`), `scheduling` | Unit T039, IT T040 (2 luồng reserve buổi cuối); **test đối chiếu**: sinh chuỗi thao tác ngẫu nhiên (đặt/hủy/kết thúc/sửa outcome) → so bộ đếm với đếm theo `SessionQueryApi` = 0 lệch |
| NFR-REL-03 | Payment chỉ INSERT (không có endpoint sửa/xóa); điều chỉnh âm bắt buộc ghi chú; `amount_paid_vnd` cập nhật trong cùng transaction với INSERT payment; tiền `BIGINT` VND | `billing` | IT: chuỗi payment + điều chỉnh → công nợ khớp tổng; test không có route PUT/DELETE payment (ma trận SEC-01); quickstart bước 3, 12 |
| NFR-REL-04 | Spring Modulith Event Publication Registry (JPA, bảng `event_publication` trong V1); bật `spring.modulith.events.republish-outstanding-events-on-restart=true`; listener `@ApplicationModuleListener` idempotent (xem REL-05) | `shared` (config), mọi module phát/nghe | `@ApplicationModuleTest` + `Scenario`: listener ném lỗi → publication còn incomplete → restart context → được xử lý; runbook OBS-03 truy vấn incomplete > 15 phút = 0 |
| NFR-REL-05 | `notification_dispatch.dedupe_key` UNIQUE (`session:{id}:reminder:{24h\|1h}:{userId}`…); `INSERT … ON CONFLICT DO NOTHING` trước khi gửi; `session_report.session_id` UNIQUE; cờ `reminder_*_at` trên session | `notification`, `learning`, `scheduling` | IT: phát cùng event 2 lần → 1 email (`FakeEmailSender` đếm); quickstart bước 6 (chạy job lại không gửi trùng) |
| NFR-REL-06 | Dispatch tách 2 bước: ghi `PENDING` trong listener → gửi qua `EmailSender`; lỗi → `attempts+1`, `next_attempt_at` theo backoff; job retry mỗi phút (ShedLock); sau 3 lần → `FAILED`. Gửi email luôn **sau commit** (listener async) nên không rollback nghiệp vụ | `notification` (port `EmailSender`) | Unit/IT với `FakeEmailSender` cấu hình lỗi N lần: N ≤ 3 → `SENT`; N > 3 → `FAILED` sau đúng 4 lần thử… (*cần chốt: 3 lần thử tổng hay 3 lần thử lại* — Q8); IT đặt lịch khi SMTP lỗi → 201 |
| NFR-REL-07 | `@EnableSchedulerLock(defaultLockAtMostFor = "PT5M")` (T010) + `@SchedulerLock` mỗi job; job dựa trên cờ đã gửi nên an toàn khi chạy lại | `shared/config`, job của `scheduling`, `learning`, `catalog`, `notification` | IT: gọi job 2 lần liên tiếp → không thêm event; review: mọi `@Scheduled` có `@SchedulerLock` (test reflection quét bean) |
| NFR-REL-08 | `@Version` trên enrollment, session (data-model.md:108,177); `ObjectOptimisticLockingFailureException` → `409 CONCURRENT_MODIFICATION` ở `GlobalExceptionHandler` (T011) | `shared/web`, `catalog`, `scheduling`, `learning` | IT: 2 transaction sửa cùng bản ghi → 1 thành công, 1 nhận 409, 0 lỗi 500 |
| NFR-REL-09 | Backend trên VM luôn chạy (R13), `restart: unless-stopped`; health check ngoài bằng dịch vụ free (ví dụ UptimeRobot gói free ⚠ verify) — chỉ là SaaS giám sát, không phải hạ tầng tự vận hành | `deploy/` | Báo cáo uptime tháng ≥ 99%; đếm số lần chạy job nhắc lịch trong log/ShedLock ≈ 1.440/ngày |
| NFR-REL-10 | Container sidecar trong `docker-compose.prod.yml` chạy `pg_dump -Fc` hằng ngày (cron trong container `postgres` image), mã hóa/nén rồi đẩy lên bucket R2 riêng (`backups/`), giữ 7 bản; dùng lại `FileStorage` credentials khác bucket (không thêm dịch vụ trả phí) | `deploy/` (không thuộc module nghiệp vụ) | Diễn tập: khôi phục bản mới nhất vào Postgres trống, chạy Flyway validate + runbook OBS-03 (0 lệch) trong ≤ 4 h; kiểm tra tuổi bản sao lưu mới nhất ≤ 24 h |
| NFR-REL-11 | `SessionFinished` (outcome COMPLETED) → listener `learning` tạo report `PENDING`, `due_at = ends_at + 24h`; idempotent theo `session_id` UNIQUE | `scheduling` → `learning` (event) | `Scenario` test Modulith: publish `SessionFinished` → report PENDING tồn tại; trên demo: báo cáo `teacher-quality` hiển thị tỷ lệ nộp đúng hạn (đo SC-005) |

### 2.3 Security & Privacy

| NFR | Cơ chế thiết kế | Module / Port | Cách kiểm chứng |
|-----|-----------------|---------------|-----------------|
| NFR-SEC-01 | `SecurityConfig` stateless, deny-by-default (`anyRequest().authenticated()`), whitelist tối thiểu (T018); `@EnableMethodSecurity` + `@PreAuthorize` trên mọi controller method; Swagger UI chỉ bật ở profile `local` | `shared/security`, mọi `internal/web` | `AuthorizationMatrixIT` (T121): bảng tham số vai trò × endpoint, so với cột "Quyền" của rest-api.md; test reflection: mọi `@RequestMapping` method có `@PreAuthorize` (fail nếu thiếu) |
| NFR-SEC-02 | Kiểm tra quyền sở hữu ở tầng service: `EnrollmentApi.isAssigned(teacherId, studentId)` cho GV; `CurrentUser.userId == studentId` cho HV; truy vấn danh sách luôn thêm điều kiện phạm vi; không có quyền → `NotFound` | `catalog` (`EnrollmentApi`), `learning`, `scheduling`, `billing`, `shared/security/CurrentUser` | IT T041 + ma trận SEC-01 (GV A ↔ HV của GV B → 404); quickstart bước 14. **Lưu ý mâu thuẫn**: contracts/rest-api.md:120 ghi 403 `FORBIDDEN` cho "không phải học viên được phân công" — xem §4 |
| NFR-SEC-03 | `BCryptPasswordEncoder` (strength 10 mặc định, cấu hình được); mật khẩu 8 ký tự – 72 byte (giới hạn BCrypt), kiểm ở `PasswordPolicy`; token mời/reset/refresh lưu SHA-256; publication event bị xóa khi xong (`completion-mode: delete`), `notification_dispatch.body_html` bị ghi đè khi `SENT`/`FAILED`, `dedupe_key` dùng `tokenId`. **Sai lệch có chủ đích**: email `PENDING` giữ link có token trong cửa sổ thử lại ≤ ~21 phút; publication lỗi (listener ném lỗi) giữ token tới khi phát lại thành công | `identity`, `notification` | IT: hash bắt đầu `$2a$10$`; mật khẩu yếu/quá dài → 400 `VALIDATION_FAILED`; `NotificationDispatchIT`: sau luồng reset không còn token trong `event_publication`/`notification_dispatch` |
| NFR-SEC-04 | `NimbusJwtEncoder` HS256, TTL cấu hình; bảng `auth_token` type REFRESH xoay vòng, đánh dấu `used_at`; khi `INACTIVE` xóa/đánh dấu mọi REFRESH; `ConsentGateFilter` trả 401 ngay khi tài khoản không còn `ACTIVE`; xoay vòng bằng update có điều kiện. **Sai lệch có chủ đích**: token vừa xoay bị dùng lại trong 30 s (nhiều tab) chỉ trả 401, không thu hồi toàn bộ — không cấp phiên mới nên không lộ phiên | `identity` | `AuthFlowIT` (T023) với `TestClock`: hết hạn, dùng lại refresh cũ → 401, INACTIVE → 401; kiểm tra header `Set-Cookie` đủ cờ |
| NFR-SEC-05 | `application.yml` chỉ tham chiếu `${ENV}`; `.env` trong `.gitignore`, `deploy/.env.example` chỉ có tên biến; app **fail khởi động** nếu `app.security.jwt-secret` < 32 byte ở profile không phải local | `shared/security`, `deploy/` | CI: bước quét secret (gitleaks GitHub Action, free) = 0 phát hiện; unit test validator độ dài secret |
| NFR-SEC-06 | Bộ đếm thất bại theo email lưu trên `user_account` (`failed_login_count`, `locked_until`) — **dùng DB sẵn có, không thêm Redis**; giới hạn theo IP bằng bộ đếm in-memory (đủ vì 1 instance) | `identity` | IT: 5 lần sai → lần 6 nhận 429 kể cả mật khẩu đúng; sau 15 phút (TestClock) đăng nhập được |
| NFR-SEC-07 | Caddy tự động TLS + redirect + header `Strict-Transport-Security`; CORS `app.frontend-url`, `allowCredentials=true` | `deploy/Caddyfile`, `shared/security` | Kiểm tra thủ công/`curl -I` trên demo: HTTP → 301, có HSTS; request từ origin lạ bị chặn CORS |
| NFR-SEC-08 | `VideoRoomProvider.issueJoin(sessionId, userId, displayName, role, validUntil)`; service kiểm tra người gọi là `teacher_id`/`student_id` của session và `now ∈ [starts_at − 10m, ends_at + 15m]` trước khi gọi port; `validUntil = ends_at + 15m` | `scheduling` (port `VideoRoomProvider`, adapter `livekit`/`fake`) | IT với `FakeVideoRoomProvider`: người thứ ba → 404, ngoài khung → 403 `ROOM_NOT_OPEN`, TTL token = `ends_at + 15m`; quickstart bước 7 |
| NFR-SEC-09 | `FileStorage.presignUpload(key, contentType, maxBytes)` ký kèm `Content-Type` và `Content-Length`; backend kiểm tra whitelist + size trước khi cấp URL; key do server sinh (`assignments/{assignmentId}/{uuid}-{fileName}`, tasks.md:221); TTL 10 phút | `shared` (port `FileStorage`), `learning` | IT với MinIO (Testcontainers) hoặc `FakeFileStorage`: content-type ngoài whitelist → 400; size > 20 MB → 400; URL hết hạn sau 10 phút; GV không phân công xin download URL → 404 |
| NFR-SEC-10 | Handler `forgot-password` trả 202 bất kể email; lỗi đăng nhập chung `UNAUTHENTICATED` | `identity` | IT: email không tồn tại và tồn tại cho cùng status/body |
| NFR-PRIV-01 | Không có `deleteById` trên repository hồ sơ (repository chỉ lộ `save`/`find`); ngừng kích hoạt qua `PATCH /users/{id}/status` | `identity`, `catalog`, `learning` | Test kiến trúc (ArchUnit, đi kèm Spring Modulith): repository hồ sơ không được gọi `delete*` từ service nghiệp vụ; review PR |
| NFR-PRIV-02 | Không log request/response body; `toString()` của DTO nhạy cảm che trường; LiveKit token/pre-signed URL không log | `shared/web`, mọi module | Test: chạy luồng login/upload với log capture (`OutputCaptureExtension`) → không chứa mật khẩu/token mẫu |
| NFR-PRIV-03 | **Chưa thiết kế** — cần quyết định pháp lý (Q2). Phương án tối thiểu: trang thông báo + checkbox đồng ý ở `accept-invite`, lưu `consent_at`/phiên bản văn bản trên `user_account`; yêu cầu xóa xử lý thủ công bởi admin theo quy trình văn bản (ẩn danh hóa thay vì xóa cứng khi có yêu cầu hợp lệ) | `identity` | Sau khi chốt: IT accept-invite thiếu đồng ý → 400; checklist pháp lý ký duyệt bởi leader |
| NFR-PRIV-04 | Trường audit theo data-model (`recorded_by`, `changed_by`, `booked_by`, `cancelled_by`, `path_revision`); `reason` bắt buộc ở DTO đổi GV/sửa lộ trình/điều chỉnh thanh toán | `catalog`, `scheduling`, `learning`, `billing` | IT từng luồng: bản ghi audit có người thực hiện = user trong JWT, `reason` rỗng → 400 |

### 2.4 Cost, Maintainability, Portability, UX, Observability

| NFR | Cơ chế thiết kế | Module / Port | Cách kiểm chứng |
|-----|-----------------|---------------|-----------------|
| NFR-COST-01 | Danh mục dịch vụ R13: Cloudflare Pages, Oracle Always Free VM, LiveKit Cloud free / tự host, Brevo/Gmail SMTP, Cloudflare R2, GitHub Actions | `deploy/`, research.md R13 | Checklist trước demo: mỗi dịch vụ ở gói free, không gắn thẻ thanh toán bắt buộc chế độ trả phí (⚠ verify từng nhà cung cấp) |
| NFR-COST-02 | Ước tính tải demo: ~50 HV × 2–3 buổi/tuần ≈ 20 buổi/ngày; mỗi buổi ~6 email (xác nhận ×2, nhắc 24h ×2, nhắc 1h ×2) + phiếu/bài tập ⇒ **~120–150 email/ngày**; video ≈ 20 × 45 phút × 2 ≈ **1.800 participant-phút/ngày (~54.000/tháng)** | `notification` (`notification_dispatch`), `scheduling` | Hằng tuần: `SELECT count(*) FROM notification_dispatch WHERE sent_at > now() − interval '1 day'` so với hạn mức SMTP; dashboard LiveKit/R2 so với hạn mức ⚠ verify. Vượt 80% → kích hoạt phương án trong §3 (R-02, R-03) |
| NFR-COST-03 | Constitution Check mỗi lần sửa plan; reviewer kiểm tra `pom.xml`/`docker-compose*.yml` không thêm broker/cache/monitoring stack | plan.md (Constitution Check, Complexity Tracking) | Review PR do leader (const:97-98); diff `docker-compose.prod.yml` chỉ gồm backend, postgres, caddy (+ backup sidecar nếu được duyệt) |
| NFR-MAINT-01 | `ModularityTests` (T013) + `Documenter`; cross-module chỉ qua `UserDirectory`, `EnrollmentApi`, `SessionQueryApi` và event (contracts/events-and-ports.md:34-40) | toàn backend | CI `./mvnw verify` fail khi vi phạm |
| NFR-MAINT-02 | `PostgresContainerConfig` dùng `postgres:16` + `@ServiceConnection` (T014); `TestClockConfig` cho logic thời gian | `backend/src/test/.../support` | Checklist "luật trọng yếu → test" trong PR template; CI: không có dependency `h2` (Maven Enforcer `bannedDependencies`) |
| NFR-MAINT-03 | Workflow `ci.yml` (T007); cache Maven/npm; Testcontainers reuse trong job | `.github/workflows/ci.yml` | Branch protection: required checks; theo dõi thời lượng job trên GitHub Actions ≤ 15 phút |
| NFR-MAINT-04 | Flyway `validate-on-migrate=true` (mặc định); quy ước tên `V{n}__{module}_{mô_tả}.sql`; `ddl-auto=validate` | `backend/src/main/resources/db/migration` | IT khởi động context trên DB trống (Testcontainers) = migration + validate pass; PR sửa file migration đã merge → checksum fail |
| NFR-MAINT-05 | `npm run gen:api` từ `/v3/api-docs`; **(đề xuất)** backend xuất `openapi.json` trong `verify`, CI frontend sinh lại và `git diff --exit-code` | `frontend/src/lib/api`, springdoc | CI fail khi type lệch |
| NFR-MAINT-06 | `spring-boot-starter-parent` 4.1.x; không khai báo version cho artifact Spring; T003 kiểm tra thư viện bên thứ ba; **(đề xuất)** Dependabot (GitHub, free) cho Maven + npm | `backend/pom.xml`, `frontend/package.json` | Review `pom.xml`; Maven Enforcer `requireJavaVersion 21`; theo dõi trang hỗ trợ Spring |
| NFR-PORT-01 | `@ConditionalOnProperty("app.providers.{video,email,storage}")` chọn adapter; SDK chỉ nằm trong `scheduling/internal/livekit`, `notification/internal/smtp`, `shared/storage/S3FileStorage` | ports `VideoRoomProvider`, `EmailSender`, `FileStorage` | Test ArchUnit: chỉ package adapter được phụ thuộc `io.livekit..`, `software.amazon..`, `org.springframework.mail..`; chạy test suite với toàn bộ adapter `fake`; quickstart.md:34 |
| NFR-PORT-02 | `backend/Dockerfile` multi-stage, base image đa kiến trúc (Eclipse Temurin 21) (T122) | `backend/`, `deploy/` | `docker buildx build --platform linux/arm64,linux/amd64` thành công; chạy trên VM ARM |
| NFR-PORT-03 | Cột có kiểu theo data-model (CEFR enum, smallint, UUID[] topic); assessment/revision chỉ INSERT | `learning` | Review migration; IT: tạo 2 assessment → lịch sử giữ đủ 2 bản ghi |
| NFR-UX-01 | `BusinessException` mang `code` + thông điệp tiếng Việt (T011); `useApiErrorToast` map `code` → thông điệp (T037); template email Thymeleaf tiếng Việt | `shared/web`, `frontend/src/components`, `notification` | Unit test frontend: mọi `code` trong rest-api.md có mapping (danh sách cứng so với enum sinh từ OpenAPI); review UI |
| NFR-UX-02 | `timestamptz` UTC, bean `Clock` UTC (T012); slot sinh theo `ZoneId` của GV rồi chuyển UTC; frontend `Intl.DateTimeFormat` với `timezone` của user (T037) | `scheduling` (`internal/slots`), `shared/config`, `frontend/src/components/DateTimeText` | Unit test slot: GV `Europe/London`, HV `Asia/Ho_Chi_Minh`, tuần đổi giờ DST (cuối tháng 3 / cuối tháng 10) → slot UTC đúng; Vitest hiển thị theo 2 múi giờ |
| NFR-UX-03 | Tailwind responsive, shadcn/ui; LiveKit components | `frontend/src/features/**` | T124 kiểm tra thủ công trên điện thoại thật (Android Chrome, iOS Safari); Playwright chạy project `chromium` + `webkit` viewport 360×800 cho smoke test |
| NFR-OBS-01 | `spring-boot-starter-actuator`, chỉ lộ `health` (+ `metrics` chỉ nội bộ, không qua Caddy); Docker `healthcheck` | `shared`, `deploy/` | `curl /actuator/health` = `UP`; tắt Postgres → `DOWN` |
| NFR-OBS-02 | Structured logging có sẵn trong Spring Boot (`logging.structured.format.console`), Micrometer tracing để có `traceId` trong MDC (⚠ kiểm tra dependency tối thiểu ở Sprint 0); Docker `logging.options max-size=10m, max-file=5` | `shared`, `deploy/` | Kiểm tra log mẫu có `traceId`; `docker inspect` cấu hình rotation |
| NFR-OBS-03 | Tệp `deploy/runbook.sql` (hoặc mục trong `deploy/README.md`) gồm truy vấn: dispatch FAILED 24h, incomplete publication > 15 phút, report PENDING quá hạn, đối chiếu bộ đếm enrollment vs session, receivable vs payment | `deploy/` | Chạy runbook trên dữ liệu demo (T128): mọi truy vấn lệch trả 0 dòng |

---

## 3. Rủi ro & đánh đổi

| # | Rủi ro | Giảm thiểu | Ảnh hưởng constitution / Complexity Tracking |
|---|--------|-----------|-----------------------------------------------|
| R-01 | **SC-010 (200 buổi đồng thời) không đạt được trên gói LiveKit free** (research.md:161-164, plan.md:44-45) | Kiểm chứng ở mức backend (NFR-SCAL-01); năng lực video đạt bằng đổi adapter/gói (NFR-SCAL-02, FR-029) | Không vi phạm (không thêm hạ tầng). Cần **chủ sản phẩm xác nhận** SC-010 được chấp nhận ở mức backend cho demo (Q1) |
| R-02 | Hạn mức LiveKit Cloud free có thể **không đủ cho chính quy mô demo** (ước tính ~54.000 participant-phút/tháng, NFR-COST-02; hạn mức ⚠ verify) | Theo dõi hằng tuần; nếu > 80% → tự host LiveKit trên Oracle VM (đã nêu ở research.md:96-97) hoặc giảm số buổi demo thật | Tự host LiveKit là **thêm một service chạy thường trực** (cần TURN/UDP, RAM, băng thông) → nếu chọn, MUST ghi Complexity Tracking kèm số đo quota |
| R-03 | Hạn mức SMTP free (email/ngày ⚠ verify) bị vượt khi mở rộng (~1.000 HV ⇒ ~2.500–3.000 email/ngày) | Demo (~150/ngày) dự kiến đủ; dedupe (REL-05) tránh gửi thừa; đổi nhà cung cấp qua `EmailSender` | Không vi phạm; nâng gói trả phí cần sửa đổi mục chi phí |
| R-04 | **Một VM duy nhất** chứa backend + DB: hỏng VM/đĩa ⇒ mất dữ liệu tài chính và học tập | NFR-REL-10 (pg_dump hằng ngày → R2, diễn tập khôi phục); fallback Neon/Supabase (research.md:157) | Backup sidecar là **job trong compose sẵn có**, không phải broker/cache/microservice, chi phí 0 → đề xuất **không** cần Complexity Tracking nhưng nên ghi rõ trong plan (§4) |
| R-05 | Oracle Always Free có thể **thu hồi VM nhàn rỗi** (CPU sử dụng thấp kéo dài ⚠ verify chính sách) hoặc thay đổi chính sách free | Health check ngoài (REL-09) phát hiện sớm; backup (REL-10) + image đa kiến trúc (PORT-02) để dựng lại trên host khác trong ≤ 4 h | Không vi phạm |
| R-06 | Giới hạn tần suất (SEC-06) dùng bộ đếm in-memory theo IP **chỉ đúng khi 1 instance** | Bộ đếm theo email lưu DB (đúng cả khi nhiều instance); in-memory chỉ là lớp phụ | Khi scale > 1 instance và cần chia sẻ trạng thái → cân nhắc cache phân tán ⇒ **phải** ghi Complexity Tracking lúc đó |
| R-07 | Pre-signed PUT trên R2/MinIO **có thể không ép được** giới hạn kích thước nếu không ký `Content-Length` | Ký kèm `Content-Length` + `Content-Type`; sau khi nộp, backend `HEAD` object kiểm tra size trước khi lưu `file_key`, quá hạn mức → từ chối và xóa object | Không vi phạm; cần spike trong Sprint 0 (Q9) |
| R-08 | Listener async: lỗi ở `notification`/`learning` chỉ thấy trong `event_publication`, dễ bị bỏ sót | Bật republish khi restart (REL-04); runbook OBS-03 | Không vi phạm |
| R-09 | Bài tải k6 và test usability tốn thời gian tuần 4 | Kịch bản k6 viết 1 lần (T126 mở rộng); usability làm cùng T128 | Không vi phạm (k6 là công cụ dev, không chạy thường trực) |
| R-10 | **Tuân thủ pháp luật dữ liệu cá nhân** chưa được đặc tả (đồng ý, yêu cầu xóa, chuyển dữ liệu ra nước ngoài khi dùng Oracle/Cloudflare/LiveKit/Brevo đặt máy chủ ngoài VN) | Chốt yêu cầu tối thiểu với leader trước tuần 3 (Q2, Q3); thiết kế NFR-PRIV-03 | Constitution V là MUST → nếu không làm kịp trong MVP phải ghi rõ là **vi phạm có chủ đích** trong Complexity Tracking |
| R-11 | Mâu thuẫn tài liệu: 403 vs 404 khi GV truy cập HV không phân công (contracts/rest-api.md:120 vs :122, plan.md:57, tasks.md:114) | Chuẩn hóa về 404 (theo plan) | Không vi phạm; sửa contracts (§4) |
| R-12 | 1 GB RAM: JVM + Spring Boot + Modulith + Hibernate có thể sát ngưỡng khi tải | NFR-SCAL-03 đo sớm (tuần 3, sau deploy đầu tiên T122); chỉnh `MaxRAMPercentage`, giảm pool | Không vi phạm |

**Kết luận Constitution**: không đề xuất nào trong tài liệu này thêm message broker, cache phân tán
hay microservice. Các bổ sung (k6, gitleaks, Dependabot, health check SaaS free, backup sidecar dùng
image Postgres sẵn có) đều chi phí 0 và không phải hạ tầng nghiệp vụ. **Chưa cần** mục mới trong
Complexity Tracking; ba điểm kích hoạt có thể cần sau này: tự host LiveKit (R-02), cache phân tán cho
rate limit (R-06), và hoãn tuân thủ PDP (R-10).

---

## 4. Đề xuất thay đổi plan.md

Chỉ là đề xuất — architect **không** sửa plan.md; owner plan (leader) quyết định.

1. **Technical Context → Performance Goals** (plan.md:38): bổ sung `p99 < 1 s`; API slot/đặt lịch
   p95 < 500 ms trên dữ liệu quy mô thiết kế (1.000 HV, 50 GV); email `SENT` ≤ 5 phút sau mốc nhắc;
   nhắc phiếu quá hạn ≤ 60 phút; LCP mobile ≤ 3 s (Should).
2. **Technical Context → Testing** (plan.md:30-31): thêm **k6** (bài tải chạy thủ công/CI tuần 4,
   kịch bản NFR-PERF-01/02, NFR-SCAL-01) và **gitleaks** (quét secret trên PR); nêu rõ Maven
   Enforcer cấm `h2`.
3. **Mục mới "Sao lưu & khôi phục"** (hoặc bổ sung research.md R13): pg_dump hằng ngày → bucket R2
   riêng, giữ 7 bản, RPO ≤ 24 h, RTO ≤ 4 h, diễn tập trước 2026-11-11 (NFR-REL-10). Thêm task tương
   ứng cạnh T122.
4. **Bảo mật**: bổ sung giới hạn tần suất đăng nhập/quên mật khẩu bằng DB (NFR-SEC-06), app fail
   khởi động khi JWT secret < 256 bit (NFR-SEC-05) vào hàng "V. Bảo mật" của Constitution Check
   (plan.md:57) và task T019/T121.
5. **research.md R6**: ghi cấu hình `spring.modulith.events.republish-outstanding-events-on-restart=true`
   và chiến lược retry email (số lần, backoff, cột `attempts`/`next_attempt_at` trong
   `notification_dispatch` — data-model.md:322-332 hiện chưa có hai cột này).
6. **Vận hành**: bổ sung structured logging + `traceId`, Docker log rotation, healthcheck, runbook SQL
   (NFR-OBS-01..03) vào phạm vi T122/T127.
7. **Bảng "Rủi ro & giảm thiểu"** (plan.md:143-151): thêm R-02 (quota LiveKit theo ước tính
   ~54.000 participant-phút/tháng), R-04 (một VM), R-05 (Oracle thu hồi VM nhàn rỗi), R-10 (PDP /
   chuyển dữ liệu ra nước ngoài).
8. **Scale/Scope** (plan.md:44-45): ghi rõ SC-010 được kiểm chứng ở mức backend bằng k6 cho demo, chờ
   xác nhận của chủ sản phẩm (Q1).
9. **Complexity Tracking** (plan.md:153-155): giữ "Không có vi phạm", thêm ghi chú "ngưỡng kích hoạt"
   (tự host LiveKit khi quota > 80%; cache phân tán khi > 1 instance; PDP nếu hoãn).
10. **Ngoài plan.md (cho owner contracts)**: sửa contracts/rest-api.md:120 — bỏ "không phải học viên
    được phân công" khỏi 403 `FORBIDDEN`, vì trường hợp này trả 404 (plan.md:57, :122, tasks.md:114).
11. **Ngoài plan.md (cho spec)**: cần bổ sung FR về thông báo/đồng ý xử lý dữ liệu và yêu cầu xóa
    (NFR-PRIV-03) — chạy `/speckit-clarify` hoặc sửa spec.

---

## 5. Câu hỏi mở & khoảng trống trong spec

| # | Câu hỏi / khoảng trống | Ảnh hưởng | Đề xuất mặc định |
|---|------------------------|-----------|------------------|
| Q1 | SC-010 có được chấp nhận là "đạt ở mức backend" cho demo không? Spec không nói rõ (spec.md:242 vs plan.md:44-45) | NFR-SCAL-01/02 | Chấp nhận; kiểm chứng video sau demo |
| Q2 | Yêu cầu pháp lý cụ thể về dữ liệu cá nhân (văn bản áp dụng, nội dung thông báo/đồng ý, ai là bên kiểm soát dữ liệu, quy trình yêu cầu xóa/ẩn danh, thời hạn lưu dữ liệu của user INACTIVE và tệp bài nộp) | NFR-PRIV-03, data-model | Checkbox đồng ý ở accept-invite + quy trình xóa thủ công |
| Q3 | Dữ liệu cá nhân lưu ở máy chủ ngoài VN (Oracle region, Cloudflare R2, LiveKit Cloud, Brevo): có cần hồ sơ đánh giá tác động chuyển dữ liệu ra nước ngoài không? Chọn region nào? | NFR-PRIV-03, R13 | Hỏi tư vấn pháp lý; chọn region gần nhất (Singapore) |
| Q4 | Mục tiêu uptime và cửa sổ demo chính thức? Spec/plan không có | NFR-REL-09 | ≥ 99%/tháng, ưu tiên ngày 2026-11-11/12 |
| Q5 | SC-006 (100% HV có hồ sơ năng lực, mục tiêu, lộ trình trong 1 tuần sau ghi danh) — không có FR/màn hình nào giúp giáo vụ **phát hiện** HV còn thiếu; đo bằng gì? | Đo SC-006 | Đo bằng truy vấn runbook (OBS-03); thêm bộ lọc "thiếu lộ trình" chỉ khi spec bổ sung |
| Q6 | SC-005 (≥ 95% phiếu trong 24 h) đo trong giai đoạn demo hay sau ra mắt? spec.md:30 chỉ hoãn SC-002/004/009 | NFR-REL-11 | Đo trong demo bằng báo cáo `teacher-quality` |
| Q7 | Ngưỡng chống dò mật khẩu (5 lần/15 phút) và có khóa tài khoản hay chỉ trả 429? | NFR-SEC-06 | 429 tạm thời, không khóa vĩnh viễn |
| Q8 | "Thử lại tối đa 3 lần" (data-model.md:330) là 3 lần gửi tổng cộng hay 3 lần thử lại sau lần đầu? Backoff bao lâu? | NFR-REL-06 | 1 lần đầu + 3 lần thử lại (1/5/15 phút) |
| Q9 | Cloudflare R2/MinIO có tôn trọng `Content-Length` ký trong pre-signed PUT không (⚠ spike) | NFR-SEC-09, R-07 | Ký + HEAD kiểm tra sau upload |
| Q10 | Hạn mức free thực tế của LiveKit Cloud, Brevo/Gmail, R2, Oracle (⚠ verify, research.md:5-6) | NFR-COST-02, R-02/03/05 | Kiểm tra trang giá trong Sprint 0, ghi vào research R13 |
| Q11 | Số người dùng đồng thời mục tiêu cho API (ngoài video) ở quy mô 1.000 HV chưa được nêu; tài liệu này giả định 20 VU (thường) và 200 VU (đỉnh SC-010) | NFR-PERF-01, SCAL-01 | Giữ giả định, xác nhận với leader |
| Q12 | Danh sách trình duyệt/phiên bản tối thiểu và có hỗ trợ phòng học trên iOS Safari trong demo không | NFR-UX-03 | 2 phiên bản mới nhất; iOS Safari ở mức Should |
| Q13 | FR-028 yêu cầu "xuất được" nhưng phạm vi demo chỉ cần "lưu có cấu trúc" (spec.md:28) — xác nhận không cần API xuất trong MVP | NFR-PORT-03 | Không làm API xuất trong MVP |
