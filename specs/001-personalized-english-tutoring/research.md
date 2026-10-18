# Research: Nền tảng đào tạo tiếng Anh 1 kèm 1 (MVP)

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Date**: 2026-10-12 · **Cập nhật**: 2026-10-18 (R5, R6, R13, R14, thêm R16–R17 theo spec Clarifications Session 2026-10-18 và aidlc/nfr.md)

Mỗi mục gồm: Decision / Rationale / Alternatives considered. Hạn mức các gói free thay đổi thường
xuyên — kiểm tra lại trang giá nhà cung cấp trước khi đăng ký (ghi chú "⚠ verify").

---

## R1. Phiên bản Spring Boot & Spring Modulith

- **Decision**: Spring Boot **4.1.x** (bản patch mới nhất lúc khởi tạo) — theo constitution v1.0.1.
  Spring Modulith, Spring Security và các thư viện Spring khác lấy **đúng phiên bản do BOM của
  Boot 4.1 quản lý** (không ghi cứng số phiên bản). Java 21, build bằng **Maven** (`./mvnw`).
- **Rationale**: 4.1 là dòng minor mới nhất đang được hỗ trợ OSS; constitution cấm dùng phiên bản
  đã hết hỗ trợ OSS. Dùng BOM tránh lệch phiên bản giữa các thư viện Spring. Maven phổ biến nhất với
  team Spring, ít cấu hình.
- **Thư viện bên thứ ba** (LiveKit SDK, AWS SDK, ShedLock, springdoc): kiểm tra tương thích Boot 4.1
  trong Sprint 0 (ngày 1). Nếu thư viện nào chưa tương thích → thay thư viện hoặc tự viết phần nhỏ
  cần dùng; **không** hạ xuống dòng Boot đã hết hỗ trợ OSS.
- **Kết quả kiểm tra (T003, 2026-10-18)** — build `./mvnw verify` xanh trên JDK 21.0.12:
  - Spring Boot **4.1.1** (Initializr); Spring Modulith **2.1.1** — Modulith **không** nằm trong BOM
    của Boot, dùng `spring-modulith-bom` riêng (ngoại lệ hợp lệ của quy tắc "theo BOM").
  - Event Publication Registry dùng **`spring-modulith-starter-jdbc`** (thay JPA): bảng
    `event_publication` lấy nguyên DDL schema v2 `schema-postgresql.sql` của jar vào `V1__shared_infra.sql`.
  - springdoc-openapi **3.1.1**, ShedLock **7.10.1** (`shedlock-spring` + `shedlock-provider-jdbc-template`),
    `io.livekit:livekit-server` **0.10.0** (Kotlin, `AccessToken` + grants) — compile và chạy được với Boot 4.1.
  - AWS SDK không thêm (K2: không có upload tệp trong MVP).
  - Testcontainers **2.0.5**: class `org.testcontainers.postgresql.PostgreSQLContainer` (package mới).
  - Lưu ý: JVM trên Windows có thể mang múi giờ `Asia/Saigon` mà PostgreSQL 16 từ chối → app đặt
    `TimeZone.setDefault(UTC)` trong `main`, test chạy với `-Duser.timezone=UTC`.
  - Annotation processor của Modulith sinh `javadoc.json` không escape dấu `"` trong Javadoc → tránh
    dấu ngoặc kép trong Javadoc (Documenter lỗi parse).
- **Bảo trì**: theo dõi lịch hỗ trợ trên trang Spring; khi 4.1 sắp hết hỗ trợ OSS → lên kế hoạch
  nâng lên dòng minor kế tiếp.
- **Alternatives**: Gradle (linh hoạt hơn nhưng team ít quen); Boot 4.0 (dòng cũ hơn, hết hỗ trợ
  sớm hơn); Boot 3.5 (hết hỗ trợ OSS — bị constitution loại).

## R2. Xác thực & phân quyền

- **Decision**: Spring Security + `spring-boot-starter-oauth2-resource-server` tự phát hành JWT
  (HS256, `NimbusJwtEncoder`). Access token 15 phút trả trong body; refresh token 7 ngày, lưu
  dạng hash trong DB, gửi qua cookie `HttpOnly; Secure; SameSite=Strict`, xoay vòng mỗi lần refresh.
  Phân quyền: `@PreAuthorize` theo role + kiểm tra quyền sở hữu dữ liệu ở tầng service
  (giáo viên ↔ học viên được phân công).
- **Rationale**: dùng thành phần chuẩn của Spring, không cần thư viện JWT bên thứ ba; refresh
  token trong cookie HttpOnly giảm rủi ro XSS; thu hồi được (xóa hash).
- **Mời người dùng / quên mật khẩu**: token ngẫu nhiên 32 byte, lưu hash, hết hạn 72h (mời) /
  30 phút (reset), dùng một lần.
- **Alternatives**: Session + cookie (đơn giản hơn nhưng constitution chọn JWT, và API dùng lại
  cho app di động sau này); Keycloak (thêm một server cần vận hành → vi phạm Free-First/YAGNI).

## R3. Chống trùng lịch (FR-010, SC-003)

- **Decision**: Ràng buộc **exclusion constraint** của PostgreSQL (extension `btree_gist`) trên
  bảng `session`: không cho hai buổi đang hiệu lực (BOOKED/COMPLETED/…) chồng thời gian cho cùng
  `teacher_id`, và tương tự cho `student_id`. Service bắt lỗi vi phạm → HTTP 409 "Khung giờ đã có
  người đặt".
- **Rationale**: đảm bảo 0 trùng lịch kể cả khi 2 request đồng thời (race condition) — kiểm tra
  ở tầng ứng dụng thôi là không đủ. Test bằng Testcontainers với 2 luồng đặt song song.
- **Alternatives**: `SELECT … FOR UPDATE` trên lịch giáo viên (dễ sót, khó test); unique index
  theo slot cố định (không hỗ trợ thời lượng buổi khác nhau).

## R4. Sinh khung giờ trống (slot)

- **Decision**: Lịch rảnh lưu dạng quy tắc hằng tuần (`day_of_week`, `start_time`, `end_time`) theo
  **múi giờ của giáo viên** + ngoại lệ theo ngày (nghỉ cả ngày hoặc khoảng giờ). Slot được tính khi
  truy vấn: bước 30 phút, độ dài = thời lượng buổi của gói, trong 30 ngày tới, cách hiện tại tối
  thiểu 2 giờ, trừ các buổi đã đặt. Thời điểm lưu `timestamptz` (UTC); frontend hiển thị theo múi
  giờ người xem.
- **Rationale**: không phải lưu hàng nghìn slot; đổi lịch rảnh không ảnh hưởng buổi đã đặt (edge case
  trong spec).
- **Alternatives**: sinh sẵn bảng slot (phải đồng bộ khi đổi lịch rảnh); thư viện RRULE (thừa cho
  quy tắc hằng tuần đơn giản).

## R5. Kế toán số buổi (FR-011, FR-024)

- **Decision**: Enrollment giữ 3 bộ đếm: `sessions_total`, `sessions_reserved` (đã đặt, chưa diễn
  ra), `sessions_consumed`. Đặt lịch → reserve (+1) chỉ khi `total − reserved − consumed > 0`. Kết
  thúc buổi: COMPLETED / STUDENT_NO_SHOW / CANCELLED_LATE → reserved −1, consumed +1;
  CANCELLED_ON_TIME / TEACHER_ABSENT / INTERRUPTED / CANCELLED_SYSTEM → reserved −1 (hoàn buổi).
  Kết quả buổi do giáo viên chốt; buổi chưa chốt giữ `BOOKED` (chưa trừ), sau `ends_at + 24h` job
  nhắc giáo viên + admin, admin chốt thay. INTERRUPTED → COMPLETED chỉ khi admin quyết định "tính".
  `CANCELLED_SYSTEM` dùng cho hủy tự động (đổi giáo viên, ngừng kích hoạt học viên, ẩn danh hóa),
  tách khỏi `CANCELLED_ON_TIME` để không làm sai tỷ lệ hủy (FR-023). Cập nhật bằng
  `UPDATE … WHERE` có điều kiện (atomic) + optimistic locking (`@Version`).
- **Rationale**: học viên không thể đặt quá số buổi còn lại; con số "còn lại" luôn đối chiếu được
  với lịch sử buổi học (SC-007).
- **Alternatives**: chỉ đếm khi buổi kết thúc (cho phép đặt vượt số buổi); bảng ledger đầy đủ
  (chính xác nhất nhưng thừa cho MVP — có thể thêm sau).

## R6. Giao tiếp giữa module & job định kỳ

- **Decision**: Spring Modulith **application events** + **Event Publication Registry** (JPA/JDBC)
  để event không bị mất khi listener lỗi hoặc app khởi động lại; listener dùng
  `@ApplicationModuleListener` (async, sau commit). Job định kỳ dùng `@Scheduled` + **ShedLock**
  (JDBC provider) để chỉ một instance chạy. Mỗi module tự sở hữu job của mình và phát event;
  module `notification` chỉ nhận event và gửi.
- **Rationale**: không cần message broker (constitution III); có thể externalize event ra RabbitMQ
  sau bằng `spring-modulith-events-amqp` mà không đổi code nghiệp vụ.
- **Chống gửi trùng**: bảng `notification_dispatch` có `dedupe_key` unique
  (ví dụ `session:{id}:reminder:24h`).
- **Phát lại & thử lại**: bật `spring.modulith.events.republish-outstanding-events-on-restart=true`;
  listener phải idempotent. Email thử lại tối đa 3 lần sau lần gửi đầu (backoff 1 → 5 → 15 phút) qua cột
  `attempts`/`next_attempt_at` của `notification_dispatch`, job quét mỗi phút (NFR-REL-06 trong aidlc/nfr.md).
- **Phụ thuộc ngược không tạo vòng**: khi identity cần hỏi module khác (ngừng kích hoạt giáo viên còn
  học viên), identity định nghĩa SPI `UserDeactivationGuard`, catalog cài — Modulith không báo vòng.
- **Alternatives**: Quartz (nặng hơn, cần thêm bảng); RabbitMQ/Kafka (bị loại ở MVP theo quyết định
  của team).

## R7. Phòng học trực tuyến tích hợp (FR-012)

- **Decision**: **LiveKit**. Backend tạo access token bằng LiveKit Java server SDK
  (`io.livekit:livekit-server`), room name = `session-{id}`, chỉ cấp cho đúng giáo viên/học viên của
  buổi, trong khoảng [bắt đầu − 10 phút, kết thúc + 15 phút]. Frontend dùng
  `@livekit/components-react` (có sẵn video, audio, chia sẻ màn hình, chat). Port:
  `VideoRoomProvider` với adapter `livekit` và `fake` (test).
- **Môi trường**: local dùng `livekit-server --dev` qua Docker; demo dùng **LiveKit Cloud gói free**
  (⚠ verify hạn mức phút/tháng) hoặc tự host trên VM free.
- **Rationale**: mã nguồn mở, tự host được (không bị khóa nhà cung cấp), có component React dựng
  sẵn → làm được trong 1 tuần.
- **Alternatives**: Jitsi (embed iframe, khó tùy biến, meet.jit.si giới hạn khi embed); Agora/Daily
  (tốt nhưng phụ thuộc hạn mức free của họ, không tự host được); WebRTC tự viết (quá tốn thời gian).

## R8. Email (FR-013, FR-019, FR-026)

- **Decision**: Spring Mail (SMTP) sau port `EmailSender`. Local: **Mailpit** (Docker, xem email ở
  web UI). Demo: SMTP của Brevo gói free hoặc Gmail App Password (⚠ verify hạn mức/ngày). Template
  email bằng Thymeleaf (chỉ dùng để render email, không dùng cho giao diện web).
- **Rationale**: SMTP là chuẩn chung → đổi nhà cung cấp chỉ bằng cấu hình.
- **Alternatives**: API riêng của Resend/SES (cần adapter riêng, để sau nếu cần).

## R9. Lưu trữ tệp (FR-020) — [GĐ2 — K2: MVP chỉ nộp bài văn bản]

- **Decision**: Port `FileStorage`, adapter **S3-compatible** (AWS SDK v2) dùng cho cả **MinIO**
  (local) và **Cloudflare R2** (demo, gói free ⚠ verify). Upload/download qua **pre-signed URL**
  (frontend tải thẳng lên storage, backend chỉ cấp URL và lưu `file_key`). Giới hạn 20 MB/tệp;
  định dạng cho phép: pdf, doc(x), jpg, png, mp3, m4a, wav, webm.
- **Rationale**: không làm nặng backend; hosting free thường không giữ file trên đĩa.
- **Alternatives**: lưu trên đĩa server (mất khi redeploy); lưu blob trong PostgreSQL (phình DB).

## R10. Frontend

- **Decision**: React 19 + Vite + TypeScript + Tailwind CSS; **React Router**; **TanStack Query**
  (gọi API, cache); **react-hook-form + zod** (form, validate); **shadcn/ui** (component);
  **FullCalendar** (React, bản MIT) cho lịch; **Recharts** cho biểu đồ tiến bộ; kiểu dữ liệu API
  sinh từ OpenAPI bằng **openapi-typescript** để frontend/backend không lệch nhau.
- **Rationale**: hệ sinh thái phổ biến, nhiều tài liệu; sinh type từ OpenAPI giảm lỗi tích hợp khi
  4 người làm song song.
- **Alternatives**: Next.js (SSR không cần thiết, deploy phức tạp hơn); Thymeleaf (khó làm lịch,
  video, biểu đồ — đã loại trong brainstorm).

## R11. API & xử lý lỗi

- **Decision**: REST JSON dưới `/api/v1`, tài liệu bằng **springdoc-openapi** (Swagger UI ở local).
  Lỗi trả theo **RFC 9457 Problem Details** (`ProblemDetail` của Spring), có `code` nghiệp vụ
  (ví dụ `SLOT_TAKEN`, `NO_SESSIONS_LEFT`, `CANCEL_WINDOW_PASSED`) và thông điệp tiếng Việt.
  Phân trang: `page`, `size`, `sort`.
- **Rationale**: chuẩn, Spring hỗ trợ sẵn; mã lỗi ổn định để frontend hiển thị đúng.

## R12. Kiểm thử

- **Decision**: Backend: JUnit 5, Spring Boot Test, **Testcontainers PostgreSQL**, Spring Modulith
  test (`ApplicationModules.verify()`, `@ApplicationModuleTest`, `Scenario` cho event). Frontend:
  **Vitest** + Testing Library cho component/logic; **Playwright** 1 bộ smoke test theo kịch bản
  demo (chạy thủ công/CI tuần 4). CI: **GitHub Actions** (free cho repo).
- **Rationale**: đúng constitution IV (DB thật, không dùng H2).

## R13. Triển khai miễn phí

- **Decision**:
  - Frontend: **Cloudflare Pages** (hoặc Vercel) — build tĩnh.
  - Backend + PostgreSQL: **Oracle Cloud Always Free VM** (ARM) chạy Docker Compose
    (backend, postgres, caddy reverse proxy + HTTPS tự động). ⚠ verify chính sách Always Free.
  - Video: LiveKit Cloud free; email: Brevo/Gmail SMTP; file: Cloudflare R2.
- **Lý do không chọn Render/Koyeb free cho backend**: các gói này **tự ngủ khi không có truy cập**
  → job nhắc lịch `@Scheduled` không chạy đúng giờ. Nếu buộc phải dùng, cần ping định kỳ từ dịch vụ
  cron bên ngoài và chấp nhận rủi ro.
- **Fallback DB**: Neon/Supabase free PostgreSQL nếu không muốn tự quản lý DB.
- **Vận hành tối thiểu** (NFR-OBS): log JSON có `traceId`, Docker log rotation, healthcheck
  `/actuator/health` qua dịch vụ ping free, runbook SQL kiểm tra số buổi/công nợ. Xem R17 cho sao lưu.

## R14. Hiệu năng & quy mô (SC-010)

- **Decision**: Tải video không đi qua backend (LiveKit xử lý), backend chỉ cấp token → 200 buổi
  đồng thời chủ yếu phụ thuộc gói LiveKit. **Gói free không đủ cho 200 buổi đồng thời**; SC-010
  được kiểm chứng ở mức backend (cấp token, API) bằng test tải nhẹ; năng lực video đạt khi chuyển
  sang gói trả phí hoặc tự host LiveKit — đây đúng là kịch bản "đổi nhà cung cấp" của FR-029.
  Spec đã chốt (2026-10-18): SC-010 đo **sau ra mắt**; trong demo chỉ kiểm mức backend bằng **k6**
  (400 lượt `join` trong 60 s + 200 người dùng ảo, container giới hạn 1 GB RAM — NFR-SCAL-01).

## R15. AI (ngoài phạm vi MVP)

- **Decision**: Không cài đặt gì cho AI trong MVP. Dữ liệu được lưu có cấu trúc (điểm kỹ năng theo
  buổi, CEFR, nhận xét, lịch sử lộ trình, bài nộp) đáp ứng FR-028. GĐ3 dùng Spring AI (`ChatClient`)
  với model free khi thử nghiệm, đổi model qua cấu hình.

## R16. Quyền riêng tư: đồng ý & ẩn danh hóa (FR-030, FR-031)

- **Decision**:
  - **Đồng ý chính sách**: bảng `policy_consent` (identity); phiên bản hiện hành lấy từ cấu hình
    `app.privacy.policy-version`. Một filter Spring Security chặn mọi API nghiệp vụ bằng 403
    `CONSENT_REQUIRED` khi người dùng chưa đồng ý phiên bản đó; frontend chỉ hiện màn hình đồng ý.
    `accept-invite` bắt buộc gửi `acceptedPolicyVersion`.
  - **Học viên dưới 16 tuổi**: `date_of_birth` chuyển về `user_account` (bắt buộc với STUDENT) để
    identity tự kiểm tra mà không phụ thuộc catalog. Tạo học viên < 16 tuổi → trạng thái
    `AWAITING_GUARDIAN_CONSENT` (không gửi mời); giáo vụ ghi `guardian_consent` → `INVITED` + gửi mời.
    Mốc tuổi là cấu hình (`guardian-consent-under-age: 16`) vì còn chờ pháp lý xác nhận.
  - **Ẩn danh hóa**: identity sở hữu `data_subject_request` và thao tác ẩn danh hóa: ghi đè định danh
    trên `user_account`, xóa `guardian_consent`, rồi phát `StudentAnonymized`. Mỗi module tự dọn dữ
    liệu của mình trong listener idempotent (scheduling hủy hệ thống buổi tương lai; catalog xóa
    ghi chú hồ sơ; learning xóa dữ liệu học tập (+ tệp qua `FileStorage.delete` từ GĐ2); notification xóa thông
    báo). Billing không thay đổi: thanh toán/công nợ chỉ còn tham chiếu UUID nên đã không định danh.
  - **Bản tóm tắt dữ liệu (yêu cầu xem)**: trang in được ở frontend ghép từ các API đọc sẵn có; không
    thêm endpoint tổng hợp chéo module (tránh identity phụ thuộc mọi module).
- **Rationale**: giữ đúng ranh giới module (constitution I) — mỗi module tự xóa dữ liệu của mình; số
  liệu tài chính và thù lao vẫn đối chiếu được (SC-007); không thêm hạ tầng (constitution III).
- **Alternatives**: xóa cứng (phá SC-007); một service trung tâm xóa trực tiếp bảng của mọi module
  (vi phạm constitution I); mã hóa dữ liệu rồi hủy khóa (crypto-shredding — mạnh hơn nhưng thừa cho MVP).
- **Rủi ro còn lại**: `payment.note` do giáo vụ nhập có thể chứa tên → hướng dẫn giáo vụ không ghi
  tên vào ghi chú thanh toán; mốc 16 tuổi và nội dung chính sách chờ pháp lý trung tâm; việc dữ liệu
  nằm ở nhà cung cấp nước ngoài (Oracle, R2, LiveKit, Brevo) cần được nêu trong chính sách (aidlc/nfr.md Q3).

## R17. Sao lưu & khôi phục

- **Decision**: `pg_dump` hằng ngày (container sidecar + cron) → bucket R2 riêng, giữ 7 bản; RPO <= 24 h,
  RTO <= 4 h; diễn tập khôi phục ít nhất một lần trước 2026-11-11 (NFR-REL-10).
- **Rationale**: chi phí 0 (R2 free), đủ cho demo một VM; không cần replica.
- **Alternatives**: PITR/WAL archiving (phức tạp hơn mức cần); DB managed (Neon/Supabase) có backup
  sẵn — là fallback của R13.
