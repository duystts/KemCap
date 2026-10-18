# REST API Contract (v1)

**Base**: `/api/v1` · JSON · Auth: `Authorization: Bearer <access token>` (trừ nhóm `auth`)
**Lỗi**: RFC 9457 Problem Details + `code` nghiệp vụ (xem cuối file) · **Phân trang**: `?page=0&size=20&sort=field,asc`
**Thời gian**: ISO-8601 có offset, ví dụ `2026-11-02T19:00:00+07:00`; server lưu UTC.

Ký hiệu quyền: **A** = Admin/Giáo vụ · **T** = Giáo viên (chỉ dữ liệu học viên được phân công) ·
**S** = Học viên (chỉ dữ liệu của chính mình) · **Any** = đã đăng nhập.

Nguồn chuẩn: OpenAPI do springdoc sinh ra (`/v3/api-docs`); frontend sinh type bằng `openapi-typescript`.
Bảng dưới là thỏa thuận ban đầu để 4 người làm song song.

---

## identity

| Method | Path | Quyền | Mô tả | FR |
|---|---|---|---|---|
| POST | `/auth/login` | public | `{email, password}` → `{accessToken, user}` + cookie refresh | FR-002 |
| POST | `/auth/refresh` | cookie | xoay vòng refresh token → `{accessToken}` | FR-002 |
| POST | `/auth/logout` | Any | thu hồi refresh token | |
| GET | `/auth/policy` | public | `{currentPolicyVersion}` — trang chấp nhận lời mời dùng trước khi đăng nhập | FR-030 |
| POST | `/auth/accept-invite` | public | `{token, password, acceptedPolicyVersion}` → kích hoạt tài khoản; 409 `GUARDIAN_CONSENT_REQUIRED` nếu học viên < 16 tuổi chưa có đồng ý người giám hộ | FR-002, FR-030 |
| POST | `/auth/forgot-password` | public | `{email}` → luôn 202 (không lộ email tồn tại) | FR-002 |
| POST | `/auth/reset-password` | public | `{token, newPassword}` | FR-002 |
| GET | `/me` | Any | thông tin người dùng hiện tại + `consentRequired` (bool) + `currentPolicyVersion` | FR-030 |
| POST | `/me/consents` | Any | `{policyVersion}` đồng ý phiên bản chính sách hiện hành | FR-030 |
| PATCH | `/me` | Any | sửa `fullName`, `phone`, `timezone` | |
| GET | `/users?role=&status=&q=` | A | danh sách người dùng | FR-007 |
| GET | `/users/{id}` | A | chi tiết một người dùng (thêm ở U2) | FR-004 |
| POST | `/users` | A | `{email, fullName, role, phone?, dateOfBirth?}` (`dateOfBirth` bắt buộc khi STUDENT) → tạo + gửi email mời; học viên < 16 tuổi → `AWAITING_GUARDIAN_CONSENT`, chưa gửi mời | FR-002, FR-004, FR-030 |
| PATCH | `/users/{id}/status` | A | `{status: ACTIVE\|INACTIVE}`; 409 `TEACHER_HAS_STUDENTS` khi ngừng giáo viên còn học viên; 409 `ANONYMIZED` với hồ sơ đã ẩn danh | FR-004 |
| POST | `/users/{id}/resend-invite` | A | gửi lại lời mời | |
| GET/PUT | `/students/{id}/guardian-consent` | A | `{guardianName, relationship, guardianContact, consentedOn}`; lưu xong → `INVITED` + gửi mời | FR-030 |
| GET/POST | `/data-subject-requests?studentId=&status=` | A | ghi nhận yêu cầu `{studentId, requesterName, type: ERASURE\|ACCESS, receivedOn}` | FR-031 |
| POST | `/data-subject-requests/{id}/reject` | A | `{resolutionNote}` | FR-031 |
| POST | `/data-subject-requests/{id}/complete-access` | A | đánh dấu đã cung cấp bản tóm tắt | FR-031 |
| POST | `/data-subject-requests/{id}/anonymize` | A | `{confirm: true}` → ẩn danh hóa học viên, phát `StudentAnonymized`, yêu cầu → COMPLETED | FR-031 |

Mật khẩu: tối thiểu 8 ký tự, có chữ và số.

**Đồng ý chính sách**: khi `consentRequired = true`, mọi API ngoài `/auth/*`, `GET /me`, `POST /me/consents` trả 403 `CONSENT_REQUIRED`; frontend chỉ hiện màn hình đồng ý.
**Bản tóm tắt dữ liệu cá nhân** (FR-031, yêu cầu xem): trang in được ở frontend (`features/admin/privacy/PersonalDataSummaryPage`) ghép từ các API đọc sẵn có (`/users/{id}`, `/students/{id}/profile`, `/enrollments`, `/sessions`, `/enrollments/{id}/payments`, `/students/{id}/progress`, `/assignments`); không có endpoint tổng hợp riêng.

## catalog

| Method | Path | Quyền | Mô tả | FR |
|---|---|---|---|---|
| GET/PUT | `/teachers/{id}/profile` | A; T (chính mình) | hồ sơ giáo viên | FR-004 |
| GET/PUT | `/students/{id}/profile` | A; T (đọc); S (đọc chính mình) | hồ sơ học viên (`selfReportedLevel`, `occupation`, `notes`) | FR-004, FR-016 |
| GET | `/students?q=&teacherId=&status=&needsConsulting=` | A; T (chỉ của mình) | danh sách học viên; `needsConsulting=true` = chưa có mục tiêu ACTIVE | FR-003, FR-007, FR-015 |
| GET/POST | `/courses` | Any (GET); A (POST) | khóa học | FR-005 |
| PUT | `/courses/{id}` | A | sửa/ngừng khóa học | FR-005 |
| GET/POST | `/packages?courseId=` | Any (GET); A (POST) | gói học | FR-005 |
| PUT | `/packages/{id}` | A | sửa/ngừng gói học | FR-005 |
| POST | `/enrollments` | A | `{studentId, packageId, teacherId, priceVnd?}` → `PENDING_PAYMENT` | FR-006 |
| GET | `/enrollments?studentId=&teacherId=&status=` | A; T; S | danh sách ghi danh + số buổi còn lại | FR-024 |
| GET | `/enrollments/{id}` | A; T; S | chi tiết | |
| GET | `/enrollments/{id}/teacher-history` | A | lịch sử phân công giáo viên (thêm ở U2) | FR-006 |
| PUT | `/enrollments/{id}/teacher` | A | `{teacherId, reason}` đổi giáo viên; buổi `BOOKED` tương lai với giáo viên cũ → `CANCELLED_SYSTEM` (bất đồng bộ) | FR-006 |
| POST | `/enrollments/{id}/extend` | A | `{newExpiresAt}` gia hạn thủ công (EXPIRED/ACTIVE → ACTIVE, ghi người + thời điểm) | FR-024 |
| POST | `/enrollments/{id}/cancel` | A | `{reason}` | |

`EnrollmentDto` gồm: `sessionsTotal, sessionsReserved, sessionsConsumed, sessionsRemaining, expiresAt, status` (+ `priceVnd` — null khi người xem là giáo viên, `version`).
**MVP (U2)**: các API danh sách (`/users`, `/students`, `/enrollments`, `/courses`, `/packages`) trả mảng, chưa phân trang — quy mô demo nhỏ; thêm `page/size` khi cần. `GET /students/{id}/profile`: `notes` chỉ trả cho admin.

## scheduling

| Method | Path | Quyền | Mô tả | FR |
|---|---|---|---|---|
| GET/PUT | `/teachers/{id}/availability` | A; T (chính mình) | thay toàn bộ quy tắc hằng tuần `[{dayOfWeek, startTime, endTime}]` | FR-008 |
| GET/POST | `/teachers/{id}/availability/exceptions` | A; T | ngày nghỉ / khung nghỉ; 409 `EXCEPTION_OVERLAPS_SESSIONS` + `sessions[]` nếu trùng buổi `BOOKED` | FR-008 |
| DELETE | `/teachers/{id}/availability/exceptions/{exId}` | A; T | | FR-008 |
| GET | `/enrollments/{id}/slots?from=&to=` | A; S | khung giờ trống của giáo viên phụ trách (tối đa 30 ngày) | FR-009 |
| POST | `/sessions` | A; S | `{enrollmentId, startsAt}` → 201 `SessionDto`; 409 `SLOT_TAKEN` / `NO_SESSIONS_LEFT` | FR-009, FR-010 |
| GET | `/sessions?from=&to=&teacherId=&studentId=&status=` | A; T; S | lịch (lọc theo quyền) | FR-007 |
| GET | `/sessions/{id}` | A; T; S | chi tiết | |
| GET | `/sessions/{id}/cancel-preview` | A; S | → `{willConsume: bool, cutoffAt}` để frontend cảnh báo hủy muộn trước khi xác nhận | FR-011 |
| POST | `/sessions/{id}/cancel` | A; T; S | `{reason}` → `CANCELLED_ON_TIME`/`CANCELLED_LATE` (S, A) hoặc `TEACHER_ABSENT` (T) | FR-011 |
| POST | `/sessions/{id}/reschedule` | A; S | `{newStartsAt}` (chỉ khi còn trong hạn hủy; sau hạn → 409 `CANCEL_WINDOW_PASSED`) | FR-011 |
| POST | `/sessions/{id}/outcome` | T (của buổi); A | `{outcome: COMPLETED\|STUDENT_NO_SHOW\|INTERRUPTED\|TEACHER_ABSENT, note?}` (sau giờ bắt đầu; A chốt thay) | FR-014 |
| POST | `/sessions/{id}/interruption-decision` | A | `{decision: COUNT\|MAKE_UP}` — COUNT: `INTERRUPTED` → `COMPLETED`; MAKE_UP: giữ `INTERRUPTED` | FR-014 |
| GET | `/sessions?outcomePending=true` | A; T | buổi `BOOKED` đã qua `ends_at` chưa chốt kết quả | FR-014 |
| POST | `/sessions/{id}/join` | T; S (của buổi đó) | → `{serverUrl, token, roomName}`; 403 `ROOM_NOT_OPEN` nếu ngoài cửa sổ [−10 phút, +15 phút sau kết thúc] | FR-012 |
| GET | `/reports/teacher-sessions?from=&to=` | A | theo giáo viên: `sessionsTaught` (COMPLETED + STUDENT_NO_SHOW + CANCELLED_LATE), `teacherCancelRate`, `studentCancelRate` (mẫu số loại CANCELLED_ON_TIME, CANCELLED_SYSTEM) | FR-023, FR-027 |

## learning

| Method | Path | Quyền | Mô tả | FR |
|---|---|---|---|---|
| GET/POST | `/students/{id}/assessments` | A; T; S (GET) | đánh giá năng lực (đầu vào/định kỳ) | FR-015, FR-016 |
| GET | `/students/{id}/proficiency` | A; T; S | hồ sơ năng lực hiện tại + lịch sử | FR-015 |
| GET/POST/PUT | `/students/{id}/goals` | A; T; S (GET) | mục tiêu học tập | FR-015 |
| GET | `/students/{id}/learning-path` | A; T; S | lộ trình ACTIVE (chặng, chủ đề, % hoàn thành) | FR-017 |
| POST | `/students/{id}/learning-path` | A; T | tạo lộ trình | FR-017 |
| PUT | `/learning-paths/{id}` | A; T | sửa toàn bộ cây chặng/chủ đề, `{…, reason}` bắt buộc | FR-018 |
| GET | `/learning-paths/{id}/revisions` | A; T | lịch sử điều chỉnh | FR-018 |
| GET | `/session-reports?status=PENDING` | T; A | phiếu cần nộp | FR-019 |
| GET | `/sessions/{id}/report` | A; T; S | phiếu đánh giá buổi | FR-019 |
| PUT | `/sessions/{id}/report` | T | `{taughtTopicIds, completedTopicIds, scores, comment, assignments[]}` → SUBMITTED | FR-019 |
| GET | `/assignments?studentId=&status=` | A; T; S | bài tập | FR-020 |
| POST | [GĐ2 — K2] `/assignments/{id}/upload-url` | S | `{fileName, contentType, size}` → pre-signed PUT URL + `fileKey` | FR-020 |
| PUT | `/assignments/{id}/submission` | S | MVP: `{textAnswer}` (bắt buộc); [GĐ2 — K2]: `fileKey?, fileName?` | FR-020 |
| GET | [GĐ2 — K2] `/assignments/{id}/submission/download-url` | A; T; S | pre-signed GET URL | FR-020 |
| POST | `/assignments/{id}/grade` | T | `{score, feedback}` | FR-020 |
| GET | `/students/{id}/progress` | A; T; S | `{skillSeries[], pathCompletion, recentReports[]}` ([GĐ2]: `attendanceRate`, `assignmentRate`) | FR-021 |
| POST | `/sessions/{id}/feedback` | S | `{stars, comment?}` (buổi COMPLETED, trong 7 ngày; một lần, không sửa) → 409 `FEEDBACK_EXISTS` / `FEEDBACK_WINDOW_CLOSED` | FR-022 |
| GET | `/sessions/{id}/feedback` | A | xem đánh giá của buổi (giáo viên không có quyền trong MVP) | FR-022 |
| GET | [GĐ2 — K1] `/reports/teacher-quality?from=&to=` | A | theo giáo viên: điểm hài lòng TB (trung bình sao), tỷ lệ nộp phiếu đúng hạn (nộp <= 24h / buổi COMPLETED) | FR-023 |

## billing

| Method | Path | Quyền | Mô tả | FR |
|---|---|---|---|---|
| GET | `/enrollments/{id}/payments` | A; S | lịch sử thanh toán + công nợ | FR-025 |
| POST | `/enrollments/{id}/payments` | A | `{amountVnd, paidAt, method, note?}` → kích hoạt gói nếu là lần đầu | FR-025 |
| POST | `/enrollments/{id}/payments/adjustments` | A | `{amountVnd (âm), note}` điều chỉnh nhập nhầm | FR-025 |
| GET | `/receivables?hasDebt=true` | A | danh sách công nợ | FR-025 |
| GET | `/reports/revenue?from=&to=` | A | tổng đã thu, tổng công nợ | FR-027 |

## notification

| Method | Path | Quyền | Mô tả | FR |
|---|---|---|---|---|
| GET | `/notifications?unread=true` | Any | thông báo trong hệ thống | FR-013 |
| POST | `/notifications/{id}/read` | Any | đánh dấu đã đọc | |
| POST | `/notifications/read-all` | Any | | |

---

## Mã lỗi nghiệp vụ (`code`)

| HTTP | code | Khi nào |
|---|---|---|
| 400 | `VALIDATION_FAILED` | dữ liệu sai; kèm `errors[{field, message}]` |
| 401 | `UNAUTHENTICATED` / `TOKEN_EXPIRED` | |
| 403 | `FORBIDDEN` | sai vai trò (giáo viên xem học viên không được phân công trả 404, không phải 403) |
| 403 | `CONSENT_REQUIRED` | người dùng chưa đồng ý phiên bản chính sách hiện hành |
| 403 | `ROOM_NOT_OPEN` | vào phòng ngoài khung thời gian |
| 404 | `NOT_FOUND` | (cũng dùng khi không có quyền xem, tránh lộ dữ liệu) |
| 409 | `SLOT_TAKEN` | trùng lịch (exclusion constraint) |
| 409 | `NO_SESSIONS_LEFT` | gói hết buổi |
| 409 | `ENROLLMENT_NOT_ACTIVE` | gói chưa thanh toán / hết hạn / đã hủy |
| 409 | `CANCEL_WINDOW_PASSED` | đổi lịch khi đã quá hạn hủy |
| 409 | `INVALID_STATE_TRANSITION` | chuyển trạng thái không hợp lệ |
| 409 | `EMAIL_TAKEN` | email đã tồn tại |
| 409 | `GUARDIAN_CONSENT_REQUIRED` | học viên < 16 tuổi chưa có đồng ý của người giám hộ |
| 409 | `TEACHER_HAS_STUDENTS` | ngừng kích hoạt giáo viên còn học viên được phân công |
| 409 | `ANONYMIZED` | thao tác trên hồ sơ đã ẩn danh hóa |
| 409 | `EXCEPTION_OVERLAPS_SESSIONS` | ngày nghỉ trùng buổi đã đặt |
| 409 | `FEEDBACK_EXISTS` / `FEEDBACK_WINDOW_CLOSED` | đánh giá đã gửi / quá 7 ngày |
| 429 | `RATE_LIMITED` | [GĐ2 — K3] quá số lần đăng nhập / quên mật khẩu (NFR-SEC-06) |
| 409 | `CONCURRENT_MODIFICATION` | xung đột optimistic lock — tải lại và thử lại |
