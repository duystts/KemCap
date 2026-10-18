# Data Model: Nền tảng đào tạo tiếng Anh 1 kèm 1 (MVP)

**Feature**: [spec.md](./spec.md) · **Research**: [research.md](./research.md) · **Cập nhật**: 2026-10-18 theo spec Clarifications Session 2026-10-18

Quy ước chung:

- Khóa chính `id UUID`; mọi bảng có `created_at`, `updated_at` (`timestamptz`, UTC).
- Bảng được nhóm theo module (Spring Modulith). Tham chiếu **sang module khác** chỉ lưu ID
  (không khai báo quan hệ JPA, không JOIN chéo module trong repository) — constitution I.
- Tiền tệ: `BIGINT` đơn vị VND (không có số lẻ).
- Xóa mềm bằng `status = INACTIVE`; không xóa cứng hồ sơ (constitution V). Ngoại lệ: **ẩn danh hóa** theo yêu cầu xóa hợp lệ (FR-031, R16) — ghi đè/xóa dữ liệu định danh, giữ bản ghi không định danh.
- Schema quản lý bằng Flyway: `V{n}__{module}_{mô_tả}.sql`.

---

## Module `identity`

### user_account

| Field | Type | Rule |
|---|---|---|
| id | UUID | PK |
| email | varchar(255) | unique (không phân biệt hoa thường), bắt buộc, đúng định dạng |
| password_hash | varchar(100) | BCrypt; null khi chưa nhận lời mời |
| full_name | varchar(150) | bắt buộc |
| phone | varchar(20) | tùy chọn |
| role | enum `ADMIN`, `TEACHER`, `STUDENT` | bắt buộc, một vai trò |
| timezone | varchar(50) | IANA zone, mặc định `Asia/Ho_Chi_Minh` |
| date_of_birth | date | **bắt buộc khi role = STUDENT** (CHECK), null với role khác; dùng xác định học viên dưới 16 tuổi (FR-030) |
| status | enum `AWAITING_GUARDIAN_CONSENT`, `INVITED`, `ACTIVE`, `INACTIVE` | |
| last_login_at | timestamptz | |
| anonymized_at | timestamptz | set khi ẩn danh hóa (FR-031); khi đó `email` = `anonymized+{id}@invalid`, `full_name` = "Học viên đã ẩn danh", `phone`/`date_of_birth` = null, `status` = INACTIVE vĩnh viễn |

**State**:

```text
(tạo STUDENT, tuổi < 16)  → AWAITING_GUARDIAN_CONSENT --giáo vụ ghi nhận đồng ý người giám hộ--> INVITED
(các trường hợp khác)     → INVITED
INVITED --đặt mật khẩu qua link mời + đồng ý chính sách hiện hành--> ACTIVE
ACTIVE <-> INACTIVE       (giáo vụ ngừng/kích hoạt lại; không áp dụng cho bản ghi đã ẩn danh hóa)
* --ẩn danh hóa--> INACTIVE (anonymized_at != null, không kích hoạt lại được)
```

`AWAITING_GUARDIAN_CONSENT` chưa gửi lời mời, không đăng nhập được. Tuổi được tính tại thời điểm kích
hoạt (lúc chấp nhận lời mời): nếu học viên < 16 tuổi thì phải có `guardian_consent`, nếu không trả 409
`GUARDIAN_CONSENT_REQUIRED`. `INACTIVE` không đăng nhập được; refresh token bị thu hồi.

**Ngừng kích hoạt** (FR-004): phát `UserDeactivated`. Với TEACHER, identity hỏi các
`UserDeactivationGuard` (SPI do module khác cài, xem events-and-ports.md §2) — catalog từ chối nếu
giáo viên còn ghi danh đang phụ trách → 409 `TEACHER_HAS_STUDENTS`.

### auth_token

| Field | Type | Rule |
|---|---|---|
| id | UUID | PK |
| user_id | UUID | FK user_account |
| type | enum `INVITE`, `PASSWORD_RESET`, `REFRESH` | |
| token_hash | varchar(100) | SHA-256 của token, unique |
| expires_at | timestamptz | INVITE 72h, PASSWORD_RESET 30 phút, REFRESH 7 ngày |
| used_at | timestamptz | dùng một lần; REFRESH bị thay khi xoay vòng |

### policy_consent (FR-030)

| Field | Type | Rule |
|---|---|---|
| id | UUID | PK |
| user_id | UUID | FK user_account |
| policy_version | varchar(20) | phiên bản từ cấu hình `app.privacy.policy-version` lúc đồng ý |
| accepted_at | timestamptz | |

Unique (`user_id`, `policy_version`). Người dùng ACTIVE chưa có bản ghi cho phiên bản hiện hành →
mọi API nghiệp vụ trả 403 `CONSENT_REQUIRED` (trừ `/auth/*`, `GET /me`, `POST /me/consents`).

### guardian_consent (FR-030)

| Field | Type | Rule |
|---|---|---|
| id | UUID | PK |
| student_id | UUID | FK user_account (STUDENT); unique |
| guardian_name | varchar(150) | bắt buộc |
| relationship | varchar(50) | bắt buộc (ví dụ "Mẹ", "Bố", "Người giám hộ") |
| guardian_contact | varchar(255) | bắt buộc (điện thoại hoặc email) |
| consented_on | date | <= hôm nay |
| recorded_by | UUID | admin |

Bị xóa khi ẩn danh hóa.

### data_subject_request (FR-031)

| Field | Type | Rule |
|---|---|---|
| id | UUID | PK |
| student_id | UUID | học viên liên quan |
| requester_name | varchar(150) | người yêu cầu (học viên hoặc người giám hộ) — **giữ lại** sau ẩn danh hóa làm bằng chứng tuân thủ |
| type | enum `ERASURE`, `ACCESS` | |
| received_on | date | |
| status | enum `RECEIVED`, `COMPLETED`, `REJECTED` | |
| resolved_at | timestamptz | |
| resolution_note | text | bắt buộc khi REJECTED |
| handled_by | UUID | admin |

`ERASURE` chuyển `COMPLETED` chỉ qua thao tác ẩn danh hóa (phát `StudentAnonymized`).

### auth_rate_limit (NFR-SEC-06) — [GĐ2 — K3, không tạo trong MVP]

| Field | Type | Rule |
|---|---|---|
| key | varchar(200) | PK, ví dụ `login:{email}`, `login-ip:{ip}`, `forgot:{email}` |
| window_start | timestamptz | |
| attempts | int | reset khi hết cửa sổ |

---

## Module `catalog` (hồ sơ, khóa học, gói học, ghi danh)

### teacher_profile

| Field | Type | Rule |
|---|---|---|
| user_id | UUID | PK, = user_account.id (role TEACHER) |
| bio | text | |
| specialties | text[] | ví dụ `IELTS`, `GIAO_TIEP`, `THIEU_NHI` |
| certificates | text | ví dụ "IELTS 8.0, TESOL" |

### student_profile

| Field | Type | Rule |
|---|---|---|
| user_id | UUID | PK, = user_account.id (role STUDENT) |
| self_reported_level | enum CEFR `A1`…`C2` | tùy chọn — trình độ sơ bộ giáo vụ nhập lúc tạo hồ sơ; hồ sơ năng lực chính thức lấy từ `proficiency_assessment` PLACEMENT (FR-016) |
| occupation | varchar(100) | tùy chọn |
| notes | text | ghi chú nội bộ của giáo vụ (bị xóa khi ẩn danh hóa) |

Ngày sinh nằm ở `user_account` (identity) để identity tự kiểm tra mốc 16 tuổi mà không phụ thuộc catalog.

### course

| Field | Type | Rule |
|---|---|---|
| id | UUID | PK |
| name | varchar(150) | unique, bắt buộc (ví dụ "IELTS", "Giao tiếp") |
| description | text | |
| status | enum `ACTIVE`, `INACTIVE` | |

### package

| Field | Type | Rule |
|---|---|---|
| id | UUID | PK |
| course_id | UUID | FK course |
| name | varchar(150) | ví dụ "IELTS 24 buổi" |
| session_count | int | > 0 |
| session_minutes | int | ∈ {25, 45, 60, 90} |
| validity_days | int | > 0 (ví dụ 90) |
| price_vnd | bigint | ≥ 0 |
| status | enum `ACTIVE`, `INACTIVE` | gói INACTIVE không bán mới, ghi danh cũ vẫn dùng |

### enrollment

| Field | Type | Rule |
|---|---|---|
| id | UUID | PK |
| student_id | UUID | user STUDENT |
| package_id | UUID | FK package |
| teacher_id | UUID | giáo viên phụ trách hiện tại (user TEACHER, ACTIVE) |
| sessions_total | int | sao chép từ package lúc ghi danh |
| session_minutes | int | sao chép từ package |
| validity_days | int | sao chép từ package; dùng tính `expires_at` khi kích hoạt |
| price_vnd | bigint | sao chép từ package, hoặc giá nhập tay lúc tạo (giảm giá); không sửa sau khi tạo |
| sessions_reserved | int | ≥ 0 |
| sessions_consumed | int | ≥ 0 |
| activated_at | timestamptz | set khi ghi nhận thanh toán đầu tiên |
| expires_at | timestamptz | = activated_at + validity_days |
| status | enum (xem dưới) | |
| renewal_low_notified_at / renewal_expiring_notified_at | timestamptz | đã nhắc gia hạn (mỗi lý do một lần; email + thông báo trong hệ thống) |
| extended_by / extended_at | UUID / timestamptz | lần gia hạn thủ công gần nhất (FR-024) |
| version | bigint | optimistic lock |

**Ràng buộc**: `sessions_reserved + sessions_consumed ≤ sessions_total` (CHECK constraint).
**Còn lại** = `sessions_total − sessions_reserved − sessions_consumed`.

**State**:

```text
PENDING_PAYMENT --PaymentRecorded--> ACTIVE
ACTIVE --consumed = total--> EXHAUSTED
ACTIVE --now > expires_at--> EXPIRED      (job hằng ngày; buổi BOOKED sau hạn vẫn giữ)
EXPIRED --giáo vụ gia hạn (expires_at mới)--> ACTIVE
PENDING_PAYMENT | ACTIVE --giáo vụ hủy--> CANCELLED
```

Chỉ `ACTIVE` mới được đặt lịch mới (FR-024). Tên hiển thị: Chờ thanh toán / Đang dùng / Hết buổi / Hết hạn / Đã hủy.

### teacher_assignment_history

| Field | Type | Rule |
|---|---|---|
| id | UUID | PK |
| enrollment_id | UUID | FK enrollment |
| from_teacher_id | UUID | null với lần phân công đầu |
| to_teacher_id | UUID | |
| changed_by | UUID | admin |
| reason | text | |

Khi đổi giáo viên, catalog phát `TeacherReassigned`; scheduling chuyển mọi buổi `BOOKED` trong tương lai
với giáo viên cũ sang `CANCELLED_SYSTEM` (FR-006).

---

## Module `scheduling`

### availability_rule

| Field | Type | Rule |
|---|---|---|
| id | UUID | PK |
| teacher_id | UUID | |
| day_of_week | smallint | 1 (Thứ 2) … 7 (CN) |
| start_time | time | theo timezone của giáo viên |
| end_time | time | > start_time; không chồng với rule khác cùng ngày |

### availability_exception

| Field | Type | Rule |
|---|---|---|
| id | UUID | PK |
| teacher_id | UUID | |
| date | date | |
| start_time / end_time | time | null cả hai = nghỉ cả ngày |
| reason | varchar(200) | |

Tạo exception trùng buổi `BOOKED` của giáo viên → 409 `EXCEPTION_OVERLAPS_SESSIONS` kèm danh sách buổi
(FR-008); giáo viên hủy các buổi đó trước (→ `TEACHER_ABSENT`).

### session

| Field | Type | Rule |
|---|---|---|
| id | UUID | PK |
| enrollment_id | UUID | |
| teacher_id | UUID | sao chép từ enrollment lúc đặt |
| student_id | UUID | |
| starts_at | timestamptz | ≥ now + 2h khi đặt; trong 30 ngày tới; trước `expires_at` |
| ends_at | timestamptz | = starts_at + session_minutes |
| status | enum (xem dưới) | |
| booked_by | UUID | học viên hoặc admin |
| cancelled_by | UUID | |
| cancel_reason | text | |
| rescheduled_from_id | UUID | buổi gốc khi đổi lịch |
| teacher_joined_at / student_joined_at | timestamptz | lần đầu lấy token vào phòng |
| reminder_24h_at / reminder_1h_at | timestamptz | đã phát `SessionReminderDue` cho mốc tương ứng (chống phát lại) |
| outcome_overdue_notified_at | timestamptz | đã phát `SessionOutcomeOverdue` (buổi vẫn `BOOKED` khi `now >= ends_at + 24h`) — FR-014 |
| outcome_by | UUID | giáo viên của buổi, hoặc admin khi chốt thay |
| version | bigint | |

**Ràng buộc chống trùng** (R3):

```sql
EXCLUDE USING gist (teacher_id WITH =, tstzrange(starts_at, ends_at) WITH &&)
  WHERE (status IN ('BOOKED','COMPLETED','STUDENT_NO_SHOW','CANCELLED_LATE','INTERRUPTED'))
-- và một ràng buộc tương tự cho student_id
```

**State & ảnh hưởng tới số buổi** (R5):

| Từ | Sang | Ai / khi nào | Số buổi |
|---|---|---|---|
| — | `BOOKED` | học viên/admin đặt | reserved +1 |
| `BOOKED` | `CANCELLED_ON_TIME` | hủy trước giờ học ≥ 12h (cấu hình) | reserved −1 |
| `BOOKED` | `CANCELLED_LATE` | hủy khi còn < 12h | reserved −1, consumed +1 |
| `BOOKED` | `COMPLETED` | giáo viên kết thúc buổi | reserved −1, consumed +1 |
| `BOOKED` | `STUDENT_NO_SHOW` | giáo viên đánh dấu (sau giờ bắt đầu) | reserved −1, consumed +1 |
| `BOOKED` | `TEACHER_ABSENT` | admin/giáo viên đánh dấu, hoặc giáo viên hủy | reserved −1 |
| `BOOKED` | `INTERRUPTED` | giáo viên đánh dấu gián đoạn | reserved −1 |
| `BOOKED` | `CANCELLED_SYSTEM` | hệ thống: đổi giáo viên (`TeacherReassigned`), ngừng kích hoạt học viên (`UserDeactivated`), ẩn danh hóa (`StudentAnonymized`) | reserved −1 |
| `INTERRUPTED` | `COMPLETED` | admin quyết định "tính là đã học" | consumed +1 (409 `NO_SESSIONS_LEFT` nếu chỗ đã được dùng cho buổi khác) |

`INTERRUPTED` mà admin chọn "xếp bù" thì giữ nguyên trạng thái; học viên/admin đặt buổi mới.
Kết quả buổi (`COMPLETED`, `STUDENT_NO_SHOW`, `INTERRUPTED`) do giáo viên của buổi chốt; sau
`ends_at + 24h` mà còn `BOOKED` → job mỗi giờ phát `SessionOutcomeOverdue` (một lần) cho giáo viên +
admin; admin được chốt thay. Buổi `BOOKED` chưa chốt không trừ buổi.

**Phân loại cho báo cáo** (FR-023, FR-027):

- *Buổi đã dạy* (thù lao) = `COMPLETED` + `STUDENT_NO_SHOW` + `CANCELLED_LATE`.
- *Mẫu số tỷ lệ hủy/vắng* = mọi buổi trong kỳ trừ `CANCELLED_ON_TIME` và `CANCELLED_SYSTEM`.
- Tỷ lệ phía giáo viên = `TEACHER_ABSENT` / mẫu số; phía học viên = (`STUDENT_NO_SHOW` + `CANCELLED_LATE`) / mẫu số.

Đổi lịch = hủy đúng hạn buổi cũ + đặt buổi mới (`rescheduled_from_id`), trong một transaction; chỉ khi
còn trước hạn chót, sau hạn chót trả 409 `CANCEL_WINDOW_PASSED` (FR-011).
Admin được sửa trạng thái kết thúc sai trong 7 ngày; số buổi được tính lại theo bảng trên.

---

## Module `learning`

### proficiency_assessment

| Field | Type | Rule |
|---|---|---|
| id | UUID | PK |
| student_id | UUID | |
| type | enum `PLACEMENT`, `PERIODIC` | |
| assessed_by | UUID | giáo viên phụ trách (PLACEMENT/PERIODIC) hoặc admin |
| assessed_at | date | |
| listening, speaking, reading, writing, grammar_vocab | enum CEFR `A1`…`C2` | bắt buộc cả 5 |
| notes | text | |

Hồ sơ năng lực hiện tại = bản ghi mới nhất; lịch sử = toàn bộ bản ghi (FR-016).

### learning_goal

| Field | Type | Rule |
|---|---|---|
| id | UUID | PK |
| student_id | UUID | 0..n goal; học viên không có goal `ACTIVE` = "Cần tư vấn" (lọc `needsConsulting=true`) |
| type | enum `IELTS`, `TOEIC`, `COMMUNICATION`, `BUSINESS`, `KIDS`, `OTHER` | |
| target | varchar(100) | ví dụ "6.5", "B2", "Phỏng vấn bằng tiếng Anh" |
| deadline | date | tùy chọn |
| status | enum `ACTIVE`, `ACHIEVED`, `DROPPED` | |

### learning_path → milestone → topic

| Bảng | Field chính | Rule |
|---|---|---|
| learning_path | id, student_id, created_by, title, status `ACTIVE`/`ARCHIVED` | mỗi học viên tối đa 1 path ACTIVE |
| milestone | id, path_id, position, title, target_date, status `PLANNED`/`IN_PROGRESS`/`DONE` | position duy nhất trong path |
| topic | id, milestone_id, position, title, objective, status `PLANNED`/`DONE` | DONE khi được chọn "đã dạy" trong phiếu đánh giá và giáo viên đánh dấu hoàn thành |
| path_revision | id, path_id, changed_by, reason (bắt buộc), summary (text, tự sinh: "Thêm chủ đề X…") | ghi mỗi lần sửa path sau khi tạo (FR-018) |

**% hoàn thành lộ trình** = số topic DONE / tổng topic.

### session_report

| Field | Type | Rule |
|---|---|---|
| id | UUID | PK |
| session_id | UUID | unique — tạo tự động (PENDING) khi nhận event `SessionFinished` (outcome COMPLETED) |
| teacher_id, student_id | UUID | |
| status | enum `PENDING`, `SUBMITTED` | |
| due_at | timestamptz | = ends_at + 24h |
| taught_topic_ids | UUID[] | topic thuộc path ACTIVE của học viên |
| score_listening … score_grammar_vocab | smallint 1–5 | bắt buộc ít nhất 1 kỹ năng khi SUBMITTED; kỹ năng không đánh giá = null |
| comment | text | bắt buộc khi SUBMITTED |
| submitted_at | timestamptz | đúng hạn nếu ≤ due_at |
| overdue_notified_at | timestamptz | đã phát `SessionReportOverdue` |

### assignment & submission

| Bảng | Field chính | Rule |
|---|---|---|
| assignment | id, session_report_id, student_id, title, instructions, due_at (tùy chọn) | tạo kèm phiếu đánh giá (0..n) |
| submission | id, assignment_id (unique), text_answer, file_key, file_name, submitted_at, score (0–10), feedback, graded_by, graded_at | cần text hoặc file (<= 20 MB; pdf/doc/docx/jpg/png/mp3/m4a/wav/webm); nộp lại được tới khi đã chấm; nộp sau `due_at` vẫn nhận và được đánh dấu "muộn" |

### session_feedback

| Field | Type | Rule |
|---|---|---|
| id | UUID | PK |
| session_id | UUID | unique; chỉ cho buổi COMPLETED của chính học viên, trong 7 ngày sau `ends_at` |
| student_id, teacher_id | UUID | |
| stars | smallint 1–5 | |
| comment | text | tùy chọn |

Không sửa được sau khi tạo (FR-022). Chỉ admin đọc từng bản ghi; giáo viên không có API đọc trong MVP.

**Ẩn danh hóa trong `learning`** (nhận `StudentAnonymized`): xóa `proficiency_assessment`,
`learning_goal`, `learning_path`/`milestone`/`topic`/`path_revision`, `assignment` + `submission` (và
tệp qua `FileStorage.delete` — chỉ từ GĐ2, MVP không có tệp); trong `session_report` xóa điểm kỹ năng, `comment`, `taught_topic_ids`
(giữ `submitted_at`, `due_at`, `status` cho tỷ lệ nộp đúng hạn); trong `session_feedback` xóa `comment`
(giữ `stars`).

---

## Module `billing`

### receivable

| Field | Type | Rule |
|---|---|---|
| enrollment_id | UUID | PK; tạo khi nhận event `EnrollmentCreated` |
| student_id | UUID | |
| amount_due_vnd | bigint | = enrollment.price_vnd |
| amount_paid_vnd | bigint | tổng các payment |

**Công nợ** = `amount_due_vnd − amount_paid_vnd` (> 0 là còn nợ).

### payment

| Field | Type | Rule |
|---|---|---|
| id | UUID | PK |
| enrollment_id | UUID | |
| student_id | UUID | |
| amount_vnd | bigint | > 0 |
| paid_at | date | ≤ hôm nay |
| method | enum `BANK_TRANSFER`, `CASH`, `OTHER` | |
| note | text | ví dụ mã giao dịch |
| recorded_by | UUID | admin |

Thanh toán không sửa được; nhập nhầm thì thêm bản ghi điều chỉnh (`amount_vnd` âm, kèm ghi chú bắt
buộc, chỉ admin) — đảm bảo lịch sử đối chiếu được (SC-007). Ràng buộc `amount_vnd > 0` áp dụng cho
bản ghi thường.

---

## Module `notification`

### notification (thông báo trong hệ thống)

| Field | Type | Rule |
|---|---|---|
| id | UUID | PK |
| user_id | UUID | |
| type | varchar(50) | ví dụ `SESSION_REMINDER`, `RENEWAL_REMINDER` |
| title, body | text | tiếng Việt |
| link | varchar(300) | đường dẫn trong app |
| read_at | timestamptz | |

Khi ẩn danh hóa: xóa `notification` của học viên; `notification_dispatch.recipient` của học viên thay bằng `anonymized`.

### notification_dispatch (nhật ký gửi email, chống trùng)

| Field | Type | Rule |
|---|---|---|
| id | UUID | PK |
| dedupe_key | varchar(200) | unique, ví dụ `session:{id}:reminder:1h:{userId}` |
| channel | enum `EMAIL` | (SMS/Zalo để sau) |
| recipient | varchar(255) | |
| subject | text | |
| body_html | text | nội dung đã render để thử lại; **ghi đè thành `[đã xóa sau khi xử lý]` khi `SENT`/`FAILED`** (có thể chứa token một lần và họ tên — NFR-SEC-03) |
| status | enum `PENDING`, `SENT`, `FAILED` | |
| attempts | smallint | lần gửi đầu + tối đa 3 lần thử lại rồi `FAILED` (NFR-REL-06) |
| next_attempt_at | timestamptz | backoff 1 phút → 5 phút → 15 phút |
| error | text | |
| sent_at | timestamptz | |

---

## Bảng hạ tầng (không thuộc module nghiệp vụ)

- `event_publication` — Spring Modulith Event Publication Registry.
- `shedlock` — khóa job định kỳ.
- `flyway_schema_history`.

## Quan hệ tổng quát

```text
user_account 1─1 teacher_profile / student_profile; 1─n policy_consent; 1─0..1 guardian_consent; 1─n data_subject_request
course 1─n package 1─n enrollment n─1 student / teacher
enrollment 1─n session 1─0..1 session_report 1─n assignment 1─0..1 submission
session 1─0..1 session_feedback
student 1─n proficiency_assessment, learning_goal; 1─0..1 learning_path(ACTIVE) 1─n milestone 1─n topic
enrollment 1─1 receivable 1─n payment
```
