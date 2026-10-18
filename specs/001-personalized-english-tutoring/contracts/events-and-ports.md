# Contracts nội bộ: Module Events & Provider Ports

Đây là "hợp đồng" giữa các module (constitution I) và giữa nghiệp vụ với dịch vụ bên ngoài
(constitution II). Thay đổi file này cần owner của các module liên quan review.

## 1. Application Events

Event là `record` bất biến đặt ở **package gốc** của module phát (public API). Listener dùng
`@ApplicationModuleListener` (async, sau commit, được lưu trong Event Publication Registry).
Publication đã xử lý xong bị **xóa** (`spring.modulith.events.completion-mode: delete`) vì event
mời/reset mang token thô (NFR-SEC-03); `tokenId` là id `auth_token`, dùng làm khóa chống trùng thay cho
token (sửa sau verify U1, 2026-10-18).

| Event | Module phát | Payload | Module nghe → hành động |
|---|---|---|---|
| `UserInvited` | identity | userId, email, fullName, role, tokenId, inviteToken | notification → email mời; `dedupe_key = user:{userId}:invite:{tokenId}` |
| `PasswordResetRequested` | identity | userId, email, tokenId, resetToken | notification → email reset; `dedupe_key = user:{userId}:reset:{tokenId}` |
| `UserDeactivated` | identity | userId, role | scheduling → buổi `BOOKED` tương lai của học viên → `CANCELLED_SYSTEM` |
| `StudentAnonymized` | identity | studentId, requestId | scheduling → `BOOKED` tương lai → `CANCELLED_SYSTEM`; catalog → xóa `notes`, `self_reported_level`, `occupation`; learning → xóa dữ liệu học tập (data-model.md); notification → xóa thông báo, che `recipient` |
| `EnrollmentCreated` | catalog | enrollmentId, studentId, teacherId, priceVnd | billing → tạo receivable |
| `EnrollmentActivated` | catalog | enrollmentId, studentId, expiresAt | notification → email chào mừng |
| `TeacherReassigned` | catalog | enrollmentId, studentId, fromTeacherId, toTeacherId | scheduling → buổi `BOOKED` tương lai với giáo viên cũ → `CANCELLED_SYSTEM`; notification → báo học viên + hai giáo viên |
| `EnrollmentRenewalDue` | catalog (job hằng ngày) | enrollmentId, studentId, sessionsRemaining, expiresAt, reason `LOW_SESSIONS`\|`EXPIRING` | notification → nhắc HV + admin |
| `PaymentRecorded` | billing | enrollmentId, paymentId, amountVnd, isFirstPayment | (chưa module nào nghe trong MVP — U8 báo cáo có thể dùng). Kích hoạt ghi danh **không** qua event này mà qua `EnrollmentApi.activateOnFirstPayment` (xem §2) |
| `SessionBooked` | scheduling | sessionId, enrollmentId, teacherId, studentId, startsAt, endsAt | notification → xác nhận cho cả hai |
| `SessionCancelled` | scheduling | sessionId, enrollmentId, status, cancelledBy, startsAt | notification → báo bên còn lại; nếu `TEACHER_ABSENT` báo admin |
| `SessionRescheduled` | scheduling | oldSessionId, newSessionId, newStartsAt | notification |
| `SessionReminderDue` | scheduling (job mỗi phút) | sessionId, teacherId, studentId, startsAt, window `24H`\|`1H` | notification → email + thông báo |
| `SessionFinished` | scheduling | sessionId, enrollmentId, teacherId, studentId, outcome, endsAt | learning → tạo `session_report` PENDING (nếu COMPLETED) |
| `SessionOutcomeOverdue` | scheduling (job mỗi giờ) | sessionId, teacherId, endsAt | notification → nhắc giáo viên + admin chốt kết quả |
| `SessionReportOverdue` | learning (job mỗi giờ) | sessionId, teacherId, dueAt | notification → nhắc giáo viên |
| `AssignmentSubmitted` | learning | assignmentId, studentId, teacherId | notification → báo giáo viên |
| `AssignmentGraded` | learning | assignmentId, studentId, score | notification → báo học viên |
| `SessionReportSubmitted` | learning | sessionId, studentId | notification → mời học viên xem nhận xét + chấm sao |

Listener ẩn danh hóa và hủy hệ thống phải **idempotent** (chạy lại không lỗi) vì Event Publication
Registry phát lại event chưa hoàn tất khi khởi động lại (`republish-outstanding-events-on-restart=true`).

**Quy tắc đếm buổi**: catalog **không** nghe event để trừ buổi — scheduling gọi đồng bộ
`EnrollmentApi.reserve/release/consume` (public API của catalog) trong cùng transaction với việc
đổi trạng thái session, để số buổi không bao giờ lệch (SC-007).

## 2. Public API đồng bộ giữa module

| Interface (package gốc) | Module | Dùng bởi | Phương thức chính |
|---|---|---|---|
| `UserDirectory` | identity | mọi module | `find(userId)`, `requireActive(userId, role)` |
| `EnrollmentApi` | catalog | scheduling, learning, billing | `get(id)`, `reserve(id)`, `release(id)`, `consume(id)`, `consumeReleased(id)` (INTERRUPTED → COMPLETED), `activateOnFirstPayment(id)` (billing gọi trong transaction ghi thanh toán đầu), `unconsume(id)` (admin sửa kết quả "tính" → "không tính", thêm ở U3), `findByStudent(studentId)` (thêm ở U3), `isAssigned(teacherId, studentId)` |
| `SessionQueryApi` | scheduling | learning | `find(sessionId)` → SessionView(id, teacherId, studentId, startsAt, endsAt, status), `attendance(studentId)` → {completed, noShow, cancelledLate} |

| `GoalStatusProvider` (SPI) | catalog định nghĩa, learning cài | catalog | `Set<UUID> studentsWithActiveGoal(studentIds)` — cờ "Cần tư vấn" = có ghi danh `ACTIVE`/`PENDING_PAYMENT` và không có goal `ACTIVE` (OQ-4, giải ở U4; thay `LearningProfileApi` của T085 để tránh vòng catalog ↔ learning) |
| `UserDeactivationGuard` (SPI) | identity định nghĩa, catalog cài | identity | `void check(userId, role)` — ném `TEACHER_HAS_STUDENTS` nếu giáo viên còn ghi danh ACTIVE/PENDING_PAYMENT |

**Sửa ở U2 (2026-10-18)**: billing nghe `EnrollmentCreated` của catalog, nên nếu catalog lại nghe
`PaymentRecorded` của billing thì hai module phụ thuộc vòng (Modulith báo lỗi). Vì vậy billing gọi đồng
bộ `activateOnFirstPayment` trong cùng transaction ghi thanh toán (thay listener T111) — thanh toán và
kích hoạt luôn cùng thành công hoặc cùng thất bại. Hướng phụ thuộc: billing → catalog → identity.

`isAssigned` được dùng cho kiểm tra quyền "giáo viên chỉ thấy học viên được phân công" (FR-003).
`UserDeactivationGuard` đảo chiều phụ thuộc: identity không gọi catalog, catalog đăng ký bean cài interface của identity.

## 3. Provider Ports (Ports & Adapters)

Chọn adapter qua cấu hình `app.providers.*`. Mỗi port có adapter `fake` dùng cho test và local.

| Port | Module | Phương thức | Adapter MVP | Adapter sau này |
|---|---|---|---|---|
| `VideoRoomProvider` | scheduling | `JoinInfo issueJoin(sessionId, userId, displayName, role, validUntil)` | `livekit`, `fake` | Agora, Daily, LiveKit tự host |
| `EmailSender` | notification | `void send(EmailMessage msg)` (to, subject, html) | `smtp` (Mailpit/Brevo/Gmail), `fake` (lưu bộ nhớ) | SES, Resend API |
| `FileStorage` [GĐ2 — K2] | shared | `PresignedUrl presignUpload(key, contentType, maxBytes)`, `PresignedUrl presignDownload(key)`, `void delete(key)` (ẩn danh hóa) | `s3` (MinIO/R2), `fake` | S3, GCS |
| `PaymentGateway` | billing | *(GĐ2 — chưa tạo, YAGNI)* | — | VNPay, MoMo, payOS |
| AI | — | *(GĐ3 — Spring AI `ChatClient`)* | — | Gemini/Groq/Ollama → Claude/GPT |

Ví dụ cấu hình:

```yaml
app:
  providers:
    video: livekit      # fake | livekit
    email: smtp         # fake | smtp
    # storage: s3       # [GĐ2 — K2] fake | s3
  scheduling:
    cancel-cutoff-hours: 12
    min-booking-lead-hours: 2
    booking-horizon-days: 30
    room-open-before-minutes: 10
    room-close-after-minutes: 15
  privacy:
    policy-version: "2026-11"      # đổi giá trị → mọi người dùng phải đồng ý lại (FR-030)
    guardian-consent-under-age: 16  # chờ pháp lý trung tâm xác nhận
  reminders:
    session-windows: [24h, 1h]      # cố định trong MVP (không có UI cấu hình)
    outcome-overdue-hours: 24
    report-overdue-hours: 24
    renewal-low-sessions: 2
    renewal-expiring-days: 7
```
