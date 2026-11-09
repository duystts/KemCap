# Build & Test Report — ALL — Nền tảng đào tạo tiếng Anh 1 kèm 1 cá nhân hóa (001-personalized-english-tutoring)

- **Ngày (UTC)**: 2026-11-03, 19:59–20:21 UTC (giờ máy: 2026-11-04, 02:59–03:21 +07)
- **Unit**: ALL, phần `verify --all` build & test, trọng tâm backend. Mã kiểm là bản đang có trong thư mục: HEAD `32d1a6a` (có U7, U8) cộng thay đổi U6 chưa commit.
- **Tasks trong phạm vi**: T001–T146 — hoàn thành: **118/146** là `[X]`; 28 task còn `[ ]` (mục 2.6). Không còn task thuần backend nào mở, trừ rà soát T140 và tài liệu T145.
- **Môi trường**: Windows 11, Git Bash; JDK 21.0.12; Node 22.21.0, npm 10.9.4; Docker Server 29.8.2; `postgres:16`, `axllent/mailpit`; Chrome headless để mở link.
- **Thay đổi của QE**: chỉ thêm file này. `git status --short` trước và sau khi chạy giống hệt nhau (33 dòng, chưa tính file này).

## 1. Build

| Thành phần | Lệnh | Kết quả | Ghi chú |
|------------|------|---------|---------|
| Backend (gồm Enforcer) | `cd backend && ./mvnw -B clean verify` | **PASS** (`BUILD SUCCESS`, exit 0, 05:16 min, xong 03:04:29 +07) | `RequireJavaVersion passed`, `BannedDependencies passed`. Log 1130 dòng, 0 dòng `[ERROR]`, 4 dòng log ứng dụng mức ERROR (xem dưới). |
| Backend format, không cache | Xóa `target/spotless-index`, rồi `./mvnw -B spotless:check` | **PASS** (exit 0, 5.890 s) | `keeping 244 files clean - 0 needs changes`, 0 bỏ qua do cache. |
| Jar HEAD để dựng CSDL nâng cấp | `git archive HEAD backend` vào thư mục tạm, `./mvnw -B -q -DskipTests -Dspotless.check.skip=true package` | **PASS** (exit 0) | Đã xóa sau khi xong. |
| Frontend cài đặt | `cd frontend && npm ci` | **PASS** (exit 0) | |
| Frontend lint | `npm run lint` (oxlint) | **PASS** (exit 0) | Chỉ có cảnh báo `only-export-components`. |
| Frontend format | `npx prettier --check src` (có trong `ci.yml`) | **PASS** (exit 0) | |
| Frontend build | `npm run build` | **PASS** (exit 0, 2.87 s) | Đây là FE trên `main`, chỉ có màn đăng nhập (mục 2.5). |

**Bốn dòng ERROR trong log `verify` (test vẫn pass):**

| Dòng log | Nội dung | Nhận định |
|----------|----------|-----------|
| 165, 257 | `EnrollmentLifecycleStartup`, `EnrollmentLifecycleJob`: `DB bận`, `lỗi bất kỳ` | Lỗi `EnrollmentLifecycleJobTest` cố ý tạo (`EnrollmentLifecycleJobTest.java:31,46`). |
| 759 | `IdentityNotificationListener`: `listener sập` | Lỗi `EventRedeliveryIT` cố ý tạo (`EventRedeliveryIT.java:43`). |
| 1013 | `SystemCancelListener.on(StudentAnonymized)`: `OptimisticLockException` | **Không cố ý**, xem F6. |

## 2. Test

| Bộ test | Lệnh | Pass / Fail / Skip | Ghi chú |
|---------|------|--------------------|---------|
| Unit + Scenario (surefire) | `./mvnw -B clean verify` | **98 / 0 / 0** | |
| Integration Testcontainers (failsafe) | `./mvnw -B clean verify` | **159 / 0 / 0** | `failsafe-summary.xml`: completed 159, errors 0, failures 0, skipped 0, flakes 0. |
| Ranh giới module | `ModularityTests` (trong `verify`) | **2 / 0 / 0** | Tính trong 98 unit. |
| IT đồng thời, lặp 2 lần | `./mvnw -B verify -Dtest=NoSuch -Dsurefire.failIfNoSpecifiedTests=false "-Dit.test=BookingConcurrencyIT,SchedulingHardeningIT,BillingReportIT,AssignmentFlowIT"` | Lần 1: **21 / 0 / 0**. Lần 2: **21 / 0 / 0** | `BUILD SUCCESS` cả 2 lần (01:02 min mỗi lần), 0 dòng ERROR. Tính cả lượt `verify` là 3 lần xanh; mẫu nhỏ, không chứng minh hết thất thường. |
| Quét log `verify` | Tìm `password`, `$2a$`, `bearer `, `eyJ`, `inviteToken`, `refreshToken`, `accessToken`, `@demo.local` | **0 khớp** | Quan sát, không phải test. |
| Frontend | `CI=true npx vitest run` | **3 / 0 / 0** | 1 file (`lib/api/client.test.ts`). Không phủ màn U2–U8 vì `main` chưa có. |
| Smoke quickstart 1–24 phần API, kèm R20, F2 | Jar thật, Postgres, Mailpit | **298 kiểm tra đạt, 7 lỗi của bộ kiểm** (mục 2.2), không có lỗi app | Chi tiết 2.1–2.4. |
| Playwright (T142), k6 (T141), gitleaks | — | **NOT RUN** | Không có trong danh sách lệnh của lượt này; T141, T142 chưa làm, `perf/` đang trống. |
| CI GitHub Actions trên PR thật | `.github/workflows/ci.yml` | **NOT RUN** | U6 chưa commit, chưa có PR. |

### 2.1. Cấu hình smoke

- **Hạ tầng:** `docker compose -p allsmoke -f <file tạm> up -d --wait`, chỉ `postgres` và `mailpit`; port 25432, 21025, 28025; app 28080.
- **App:** jar từ `clean verify`, `VIDEO_PROVIDER=fake`, `--spring.profiles.active=local,demo-seed`, `FRONTEND_URL=http://localhost:25173`.
- **CSDL:** kịch bản chạy trên CSDL đã nâng cấp từ HEAD, không phải cài mới.
- **Tham số khác mặc định:** 2 cron hằng giờ đặt 10 giây để khỏi chờ: `app.jobs.report-overdue-cron`, `app.jobs.outcome-overdue-cron`. Cron gia hạn giữ mặc định để kiểm đúng đường chạy bù. Lần cuối (kiểm tra thêm) chạy không tham số cron.
- **Tắt app:** bằng `taskkill /F`, tức tắt đột ngột; trước khi tắt đã chờ `event_publication` chưa xong về 0.
- **Dọn dẹp:** đã dừng app, dừng `vite preview`, chạy `docker compose -p allsmoke ... down -v`. Không còn container, volume, network `allsmoke`; 6 port đã trống; không còn tiến trình Java; không đụng project compose mặc định. Đã xóa jar, dump `pg_dump`, build FE tạm.

**SQL chỉ để dựng trạng thái (11 câu; mọi thay đổi khác đều qua API):**

| Bước | Dữ liệu đổi | Lý do |
|------|-------------|-------|
| 6 | `session.starts_at/ends_at` = sau 15 phút; `created_at` lùi 3 giờ | Đặt được tối thiểu trước 2 giờ; `created_at` để qua điều kiện đặt trước mốc nhắc. |
| 7 | `starts_at/ends_at` = sau 5 phút | Vào cửa sổ phòng. |
| 8, 19 (3 câu) | Đưa 3 buổi về quá khứ | Chỉ chốt được sau giờ bắt đầu. |
| 9, 17 | `starts_at/ends_at` = sau 6 giờ | Còn dưới 12 giờ. |
| 18 | `ends_at` = trước 24 giờ 15 phút | Giả lập `now = ends_at + 24h`. |
| Chạy bù (2 câu) | `enrollment.expires_at`: student4 quá hạn 1 giờ, student2 còn 3 ngày | Dựng trạng thái cho job khởi động. |
| FR-013, FR-014 (2 câu) | Buổi sau 20 giờ, `created_at` lùi 2 ngày; 1 buổi về quá khứ | Nhắc 24 giờ; `STUDENT_NO_SHOW`. |

### 2.2. Bảy lỗi của bộ kiểm (đều không phải lỗi app, đã kiểm lại)

| # | Kiểm tra | Nguyên nhân | Kiểm lại |
|---|----------|-------------|----------|
| 1 | "MAKE_UP trả chỗ" | QE đặt sai kỳ vọng: `INTERRUPTED` trả chỗ ngay khi GV chốt, cả 2 buổi. | `res2/con3/rem7` → `res0/con3/rem9` khi chốt; `MAKE_UP` giữ nguyên; `COUNT` → `con4/rem8`. Đúng quickstart bước 19. |
| 2–5 | 4 kiểm tra hủy ghi danh | Ghi danh của student3 còn 1 buổi `BOOKED`, app trả 409 theo đúng thiết kế (`Enrollment.java:233-236`). Riêng ca "thu thêm sau hủy" nhờ vậy ghi thêm 1 VND do ghi danh chưa hủy. | Hủy buổi trước rồi hủy ghi danh: đạt hết (xem 2.3). Khe hợp đồng: F2. |
| 6 | "Không có biến chưa thay trong thư" | Regex của QE khớp nhầm `th:` trong `width:`. | Quét lại 73 thư bằng regex chính xác: 0 biến chưa thay, 0 vỡ dấu. |
| 7 | "Admin nhận email GV vắng" | QE tìm "vắng" ở tiêu đề, nhưng tiêu đề là "Buổi học lúc 19:00, 04/11/2026 đã bị hủy". | Thân thư có "Giáo viên Trần Văn Minh vắng/hủy buổi với học viên Đặng Mỹ Linh", dispatch `SENT`, có thông báo `TEACHER_ABSENT`. |

### 2.3. Kết quả từng bước quickstart

| # | Thao tác | Quan sát | Kết quả |
|---|----------|----------|---------|
| 1 | Admin tạo GV, HV | 201, `INVITED`. Thiếu ngày sinh → 400; email trùng → `EMAIL_TAKEN`. Đặt mật khẩu bằng token trong thư → 204, đăng nhập được, `consentRequired=false`. Token sai và link dùng lại → 400. | **Đạt** |
| 2 | Khóa, gói 12×45, ghi danh | `PENDING_PAYMENT`. GV mới thấy đúng 1 HV, GV khác không thấy. Đặt buổi lúc này → `ENROLLMENT_NOT_ACTIVE`. | **Đạt** |
| 3 | Thanh toán 50% | `ACTIVE`, còn 12; `due 3000000, paid 1500000, debt 1500000`. GV đọc thanh toán → 403, giá null. | **Đạt** |
| 4 | Lịch rảnh T2–T6 18–21 | 90 slot trong 25 ngày, 0 slot sai giờ VN, sai bước 30 phút hay sai độ dài 45 phút. GV khác sửa → 404. | **Đạt** |
| 5 | Đặt song song 2 lượt | `201` và `409 SLOT_TAKEN`; lượt thứ 3 tuần tự cũng `SLOT_TAKEN`; còn 11. Cả hai nhận "Xác nhận buổi học lúc 18:00, 04/11/2026", đúng giờ VN (11:00Z). | **Đạt** |
| 6 | Nhắc 1 giờ, job thật | Cả hai nhận "Nhắc lịch ... (còn 1 giờ)"; sau 70 s vẫn 1 thư. Nhắc 24 giờ kiểm thêm: HV, GV đều nhận "còn 24 giờ". | **Đạt** |
| 7 | Vào phòng | Còn hơn 10 phút → 403 `ROOM_NOT_OPEN`. GV, HV → cùng `roomName`. GV khác, HV khác → 404; admin → 403. Video, âm thanh, chia sẻ màn hình, chat: **NOT RUN** (fake, không LiveKit, không FE). | **Đạt phần API** |
| 8 | Chốt, nộp phiếu | Còn 11. Phiếu `PENDING`, `dueAt = ends + 24h`. Điểm 9 và `assignments:[null]` → 400; admin → 403. Nộp → `SUBMITTED`, `onTime=true`; lần 2 → 409. Thư, thông báo `REPORT_SUBMITTED` có. | **Đạt** |
| 9 | Hủy sớm, hủy muộn | Sớm: `CANCELLED_ON_TIME`, hoàn buổi. Muộn: `CANCELLED_LATE`, `res1/con1/rem10` → `res0/con2/rem10`. GV nhận thư hủy. Đổi lịch trong hạn: còn lại không đổi, buổi cũ `CANCELLED_ON_TIME`, GV nhận "Buổi học đã đổi sang 19:00". | **Đạt** |
| 10 | Nộp, sao, chấm | Chấm khi chưa nộp → 409. HV nộp → GV nhận thư, đọc được bài. 5 sao → 201, có trong `/session-feedback/mine`. Chấm → `GRADED`; chấm lại → 409. HV thấy điểm 8 và phản hồi; thư "(8/10)". | **Đạt** |
| 11 | Tiến bộ | Điểm đúng; `pathCompletion`=33 = `completionPercent`=33 của lộ trình (mẫu 1/3); CEFR đúng; `recentReports` có nhận xét. Biểu đồ FE: **NOT RUN**. | **Đạt phần API** |
| 12 | Báo cáo | `collectedVnd`, `debtVnd`, `sessionsTaught` khớp SQL; `debtVnd` khớp tổng `/receivables`; `from > to` → 400; GV, HV → 403; không token → 401. Dashboard FE: **NOT RUN**. | **Đạt phần API** |
| 13 | Gia hạn, chạy bù | Xem 2.4. | **Đạt** |
| 14 | GV khác | 7 API (hồ sơ, tiến bộ, lộ trình, năng lực, mục tiêu, bài tập, phiếu) → 404. | **Đạt** |
| 15 | HV 12 tuổi | `AWAITING_GUARDIAN_CONSENT`, 0 thư; `resend-invite` → 409 `GUARDIAN_CONSENT_REQUIRED`, 0 thư. Ghi đồng ý → có thư mời. | **Đạt** |
| 16 | Đồng ý chính sách | Không tích → 400, chưa đăng nhập được; tích → 204, đăng nhập được. Đổi `POLICY_VERSION`, xem 2.4. | **Đạt** |
| 17 | Còn dưới 12 giờ | Preview `willConsume=true`; đổi lịch → `CANCEL_WINDOW_PASSED`; hủy → `CANCELLED_LATE`. Hộp cảnh báo FE: **NOT RUN**. | **Đạt phần API** |
| 18 | Quá 24 giờ chưa chốt | Vẫn `BOOKED`, bộ đếm không đổi; GV và admin nhận nhắc; có trong `outcomePending`; admin chốt → `res1/con2` → `res0/con3`; có lịch sử chốt. | **Đạt** |
| 19 | Gián đoạn | `MAKE_UP` giữ `INTERRUPTED`, không trừ, không có phiếu. `COUNT` → `COMPLETED`, trừ 1, có phiếu `PENDING`. GV quyết định → 403. | **Đạt** |
| 20 | Đổi GV, 2 buổi tương lai | Cả 2 → `CANCELLED_SYSTEM`; `rem6` → `rem8`, `con` không đổi; 0 `TEACHER_ABSENT` mới. HV ("Đổi giáo viên phụ trách"), GV cũ ("Bàn giao học viên"), GV mới ("Học viên mới được phân công") mỗi người đúng 1 thư gộp, liệt kê 2 giờ đã hủy. | **Đạt** |
| 21 | Ngày nghỉ trùng buổi | 409 `EXCEPTION_OVERLAPS_SESSIONS`, body có id buổi. | **Đạt** |
| 22 | Ngừng GV còn HV | 409 `TEACHER_HAS_STUDENTS`. | **Đạt** |
| 23 | Yêu cầu xem, xóa | Xem 2.4. Trang in FE: **NOT RUN**. | **Đạt phần API** |
| 24 | Sao lần 2 | `FEEDBACK_EXISTS`; GV đọc → 403; admin đọc → 200, sao vẫn 5. | **Đạt** |

**Kiểm tra thêm ngoài 24 bước:**

| Nội dung | Quan sát |
|----------|----------|
| Quên mật khẩu | Email có và không có tài khoản đều 202. Mở link trong thư, đặt lại bằng token → 204, đăng nhập được. |
| Hủy ghi danh | Còn `BOOKED` → 409 "Ghi danh còn buổi đã đặt, hãy hủy các buổi trước". Hủy buổi xong: `CANCELLED`, lý do đã trim, có người và giờ hủy. Công nợ tổng 20099999 → 13200000 (đúng nợ 6899999 của ghi danh), đã thu giữ nguyên. `hasDebt=true` không còn; danh sách đầy đủ `cancelled=true`. HV đọc: 3 trường hủy null. Thu thêm → 409; điều chỉnh âm → 201; hủy lần 2 → 409; đặt buổi → `ENROLLMENT_NOT_ACTIVE`. |
| GV vắng, HV vắng | GV hủy → `TEACHER_ABSENT`, hoàn buổi. `STUDENT_NO_SHOW` → trừ buổi, không có phiếu. |
| FR-004 | Ngừng HV: buổi tương lai `CANCELLED_SYSTEM`, token cũ → 401. Kích hoạt lại → `ACTIVE`, đăng nhập được. |
| FR-007 | `/users?q=&role=`, `/users?status=`, `/students?q=`, `/students?teacherId=`, `/sessions?teacherId=&status=`, `/sessions?from=&to=` lọc đúng. **`courseId` bị bỏ qua (F1).** |
| Đối soát SC-007 toàn CSDL | Lúc cuối: `sessions_reserved`, `sessions_consumed` khớp đếm buổi ở mọi ghi danh; `amount_paid_vnd` khớp `SUM(payment)` ở mọi ghi danh. |
| Log, event | `event_publication` chưa xong: 0 ở mọi mốc đo. 7 log app: 0 dòng ERROR; 0 khớp token, hash, email, tên, chuỗi đánh dấu. `Demo@1234` có trong log seed (F5). |

### 2.4. Các hạng mục được yêu cầu riêng

**GV bị đổi (T096, R1, R14, F1 của U6):**

| Thao tác của GV cũ | Quan sát |
|--------------------|----------|
| `GET /session-reports` | Chỉ còn 1 phiếu `PENDING` của buổi mình dạy. |
| Đọc phiếu `PENDING` | 200. |
| Đọc phiếu đã nộp (2 phiếu) | 404. |
| `/progress`, `/learning-path`, `/profile` | 404. |
| `/assignments?studentId=` | 404. |
| `/assignments`, `/students` | 200, không còn HV này. |
| Chấm bài cũ | 404. |
| Gửi `completedTopicIds` | 400 "Bạn không còn phụ trách học viên, phiếu không được chọn chủ đề"; chủ đề vẫn `PLANNED`. |
| Nộp phiếu `PENDING` không có chủ đề | 200 `SUBMITTED`; đọc lại → 404; danh sách rỗng. |
| `GET /sessions`, `GET /sessions/{id}` | **Vẫn 200 (F3)**. `outcome-history` → 403. |

Bài nộp sau khi đổi GV: thư chỉ tới GV mới, GV cũ giữ nguyên số thư, GV mới chấm → 200. **F1 của U6 đóng trên thư thật.**

**Ẩn danh hóa (`student1`, HV mới, HV 12 tuổi):**

- `confirm=false` → 400; `confirm=true` → 200 `COMPLETED`; lần 2 → 409 `ANONYMIZED`.
- `/reports/revenue`, `/receivables` (bỏ cột tên), `/reports/teacher-sessions` trước = sau, so nguyên chuỗi JSON. Buổi tương lai → `CANCELLED_SYSTEM`.
- `pg_dump --data-only --inserts` với 27 chuỗi (tên, email, SĐT, ngày sinh, hash mật khẩu, người giám hộ; đánh dấu trong lộ trình, chủ đề, mục tiêu, đánh giá, nhận xét, bài tập, bài nộp, phản hồi, nhận xét sao, ghi chú và lý do hủy, lý do đổi GV, hồ sơ):
  - **Trước:** cả 27 chuỗi đều tìm thấy, chứng tỏ phép quét bắt được.
  - **Sau (273 dòng INSERT):** chỉ còn tên ở `data_subject_request` (3 dòng, `requester_name`) và `payment` (1 dòng, `note`), đúng 2 ngoại lệ đã chốt; 0 dòng ở mọi bảng khác.
- `user_account`: "Học viên đã ẩn danh", `anonymized+<id>@invalid`, `phone`, `date_of_birth`, `password_hash` null, `INACTIVE`. `guardian_consent` 0 dòng.
- Phiếu giữ `SUBMITTED`, `submitted_at`; điểm, nhận xét, chủ đề null. Sao giữ 5, nhận xét null. Bài tập, lộ trình, năng lực, mục tiêu 0 dòng. `/progress` trả rỗng (F6 U6 đóng).
- `extend`, đổi GV, kích hoạt lại → 409 `ANONYMIZED`. `POST /cancel` có lý do chứa tên → 200, `cancel_reason` null. 0 thư tới `@invalid`.
- Đăng nhập → 401; refresh cookie cũ → 401.

**R20 U6, nâng cấp CSDL:**

- Jar HEAD trên CSDL trống: 13 migration, seed, chốt thật 1 buổi.
- Jar hiện tại trên cùng CSDL: `Migrating ... "7 - learning reports" [out of order]`, `"9 - learning feedback" [out of order]`, `Successfully applied 2 migrations`. Rank 14 và 15, đều `success`. `ddl-auto: validate` đạt, 0 ERROR, seed không trùng (`13|8|4|3` trước = sau).
- Cài mới bằng jar hiện tại + `demo-seed` (CSDL `fresh`, `EMAIL_PROVIDER=fake`, port 28081): lên được, 0 ERROR. `pg_dump --schema-only` (bỏ bảng Flyway, `\restrict`) **giống hệt** bản nâng cấp.

**F2 U6:** ẩn danh `student1`, khởi động lại `local,demo-seed` → **app lên** (18.8 s), không còn "Thiếu tài khoản demo", 0 ERROR. Số dòng không đổi (`12|25|2|6`), `student1@demo.local` không bị tạo lại, đăng nhập → 401, không có thư mới. **F2 đóng trên app thật.**

**Chạy bù job gia hạn khi khởi động:** dựng khi app còn chạy, rồi tắt và bật lại, cron giữ mặc định.

- Gói 2 buổi: HV nhận "Nhắc gia hạn gói học", admin nhận "Học viên Bùi Gia Huy cần gia hạn gói học"; `renewal_low_notified_at` có.
- Gói còn 3 ngày: nhắc `EXPIRING` tới HV, admin. Gói quá hạn: `EXPIRED`, buổi `BOOKED` giữ (`sessions_reserved`=1).
- Khởi động lại lần 2: vẫn 1 thư. Đặt buổi khi `EXPIRED` → `ENROLLMENT_NOT_ACTIVE`; GV gia hạn → 403; hạn quá khứ → 400; hợp lệ → `ACTIVE`, `extended_by/at` có, đặt lại được.

**`curl` thật:**

| Trường hợp | Lệnh | Kết quả |
|------------|------|---------|
| Chưa đồng ý chính sách (`POLICY_VERSION` mới, đăng nhập lại) | `curl --path-as-is .../api/v%31/sessions` | **403 `CONSENT_REQUIRED`** |
| Sau khi đồng ý | như trên | 200 |
| Token tài khoản đã ẩn danh | `curl .../api/v%31/sessions`, `curl .../api/v1/me` | **401 `UNAUTHENTICATED`** cả hai |

Thử thêm 14 dạng đường dẫn khi chưa đồng ý: `%73essions`, `%761`, `/sessions/`, `//sessions`, `auth/../`, `auth/%2e%2e/`, `auth/..%2f`, `auth/..;/`, `sessions;x=1`, `actuator/health/../../`, `swagger-ui/../`, `v3/api-docs/../../`, `me/../`, `me/consents/../../`, cộng `/API/V1/SESSIONS`: tất cả 400 hoặc 403, không có 200. Đồng ý sai phiên bản → 400. Admin chưa đồng ý cũng bị chặn. `GET /me` → 200 kèm `consentRequired=true`.

**Nội dung email và link (73 thư, đọc thân thư thật của đủ 14 mẫu):**

- Mẫu đã đọc: mời, đặt lại mật khẩu, kích hoạt gói, xác nhận, nhắc lịch 1 giờ và 24 giờ, hủy, đổi lịch, quá hạn chốt, phiếu, bài nộp, chấm điểm, nhắc phiếu quá hạn, gia hạn, đổi GV (đủ 3 vai). Đúng tên người nhận, giờ VN, tiếng Việt có dấu. Quét tự động cả 73 thư: 0 biến chưa thay, 0 vỡ dấu, thư nào cũng có link `FRONTEND_URL`.
- **Mở link thật bằng Chrome headless**, FE build từ `main` trỏ về app smoke:
  - `/accept-invite?token=...`: hiện "Kích hoạt tài khoản", ô mật khẩu, "Tôi đồng ý ... (phiên bản 2026-11)", tức `GET /auth/policy` chạy được qua CORS.
  - `/reset-password?token=...`: hiện "Đặt lại mật khẩu".
  - Token lấy từ chính 2 link trên dùng được ở API. QE **không** bấm nút trên giao diện.
  - `/sessions/:id`, `/teacher/students/:id`, `/student/progress`, `/student/packages`, `/admin/enrollments/:id`: chưa đăng nhập đều về màn đăng nhập. **Mở lúc đã đăng nhập: NOT RUN.**

| Link trong thư | Số thư (lượt chính, 55 thư) | FE `main` | FE `pr7` (đọc `router.tsx`, chưa mở) |
|----------------|--------|-----------|---------------------------------------|
| `/accept-invite`, `/reset-password` | 5 | Có | Có |
| `/sessions/:id` | 37 | **Không có route**, về trang chủ | Có |
| `/teacher/students/:id` | 3 | **Không có route** | Có |
| `/admin/enrollments/:id` | 2 | **Không có route** | Có |
| `/teacher/students`, `/student/book`, `/student/packages` | 6 | Placeholder | Trang thật |
| `/student/progress` | 2 | Placeholder | Placeholder (T103) |

### 2.5. Frontend: `main` chưa có các màn U2–U8

- **`main`:** chỉ có `features/auth` (6 file), các route còn lại là `PlaceholderPage` (`frontend/src/app/router.tsx:18-21`). `lint`, `test`, `build` xanh ở trên **chỉ nói về chừng đó**.
- **`pr6`, `pr7`** (hiện là nhánh local; `gh` không có trên máy nên không đọc được trạng thái PR): phần `frontend/` của hai nhánh giống hệt nhau. Có màn U2–U5, **không có U6, không có màn báo cáo và công nợ của U8, không có trang tóm tắt của U7**.
- **Không nhánh nào** trong `--all` từng có `PendingReportsList`, `AssignmentsPage`, `ProgressPage`, `SessionFeedbackPrompt`, `ReceivablesPage`, `DashboardPage`, `PersonalDataSummaryPage`, `PaymentsPanel` (`git log --all` 0 commit).
- **Thử gộp `pr7` vào `main`** (`git merge-tree`, không đụng thư mục làm việc): xung đột ở `.gitattributes`, `aidlc-state.md`, `tasks.md`; không đổi `backend/`, không xung đột file `frontend/`. Lint, test, build của `pr7`: **NOT RUN**.

### 2.6. Đối chiếu FR, SC, tasks

**FR:** "IT, Unit" là bộ test đã pass trong `verify`. Thiếu FE: task ghi `(có ở pr7)` là đã có file chính ở `pr7`, chưa vào `main`.

| FR | API | Test tự động | Smoke | Còn thiếu |
|----|-----|--------------|-------|-----------|
| FR-001 | Có | `AuthorizationRulesTest`; từng API; ma trận đầy đủ chưa | Đạt | T140 |
| FR-002 | Có | `AuthFlowIT` | Đạt | FE xong trên `main` |
| FR-003 | Có | `StudentAccessIT`, `LearningAccessIT`, `SessionReportFlowIT` | Đạt; F3 cần chốt | T054 (có ở pr7) |
| FR-004 | Có | `CatalogFlowIT`, `StudentAccessIT`, `SystemCancelIT` | Đạt | T052, T054 (có ở pr7) |
| FR-005 | Có | `CatalogFlowIT` | Đạt | T053 (có ở pr7) |
| FR-006 | Có | `SystemCancelIT`, `CatalogFlowIT` | Đạt | T055 (có ở pr7) |
| FR-007 | **Một phần** | Không có test `q`; `sessionListIsPagedAndBounded` | Đạt trừ lọc khóa học | **F1**; T054, T077 (có ở pr7) |
| FR-008 | Có | `SchedulingJobsIT`, `SchedulingHardeningIT` | Đạt | T075 (có ở pr7) |
| FR-009 | Có | `BookingConcurrencyIT`, `SlotServiceTest` | Đạt | T076 (có ở pr7) |
| FR-010 | Có | `BookingConcurrencyIT` | Đạt | Chỉ BE |
| FR-011 | Có | `CancellationPolicyIT` | Đạt | T078 (có ở pr7) |
| FR-012 | Có | `JoinServiceTest`, `LiveKitVideoRoomProviderTest` | Đạt API; video NOT RUN | T079 (có ở pr7) |
| FR-013 | Có | `SchedulingJobsIT`, `SchedulingNotificationIT`, test listener | Đạt | T136 (có ở pr7) |
| FR-014 | Có | `SessionOutcomeIT`, `SessionOutcomeTest` | Đạt | T078 (có ở pr7) |
| FR-015, 016 | Có | `LearningAccessIT` | Đạt | T088 (có ở pr7) |
| FR-017, 018 | Có | `LearningPathServiceTest`, `LearningAccessIT` | Đạt | T089 (có ở pr7) |
| FR-019 | Có | `SessionReportFlowIT` | Đạt | **T101** (không nhánh nào có) |
| FR-020 | Có, chỉ văn bản (K2) | `AssignmentFlowIT` | Đạt | **T102** |
| FR-021 | Có | `SessionReportFlowIT` | Đạt | **T103** |
| FR-022 | Có | `SessionFeedbackIT` | Đạt | **T122** |
| FR-023 | GĐ2 (K1) | — | — | Đã cắt |
| FR-024 | Có | `EnrollmentLifecycleJobIT`, `EnrollmentTest`, `SessionOutcomeIT` | Đạt | **T116**, T055 |
| FR-025 | Có | `BillingReportIT`, `PaymentServiceTest`, `BillingFlowTest` | Đạt | **T114, T115** |
| FR-026 | Có | `EnrollmentLifecycleJobIT`, `EnrollmentLifecycleStartupIT`, test listener | Đạt | **T116** |
| FR-027 | Có | `BillingReportIT`, `TeacherSessionsReportIT` | Đạt | **T123** |
| FR-028 | Lưu có cấu trúc; không có API xuất | Không riêng | Đọc được qua API | Không có task; `stories.md:721` ghi "một phần", xem F7 |
| FR-029 | Có (port, adapter) | `LiveKitVideoRoomProviderTest` | Đổi `VIDEO_PROVIDER`, `EMAIL_PROVIDER` chạy được; LiveKit thật NOT RUN | Chỉ BE |
| FR-030 | Có | `AuthFlowIT`, `GuardianConsentIT`, `ConsentGateFilterTest` | Đạt | **T130** |
| FR-031 | Có | `AnonymizationIT`, `NotificationDispatchIT` | Đạt | **T130, T131** |

**SC:**

| SC | Trạng thái | Bằng chứng |
|----|-----------|------------|
| SC-001 (đặt < 1 phút) | **NOT RUN** | Cần FE T076; chưa đo. |
| SC-002, 004, 009 | Đo sau ra mắt | `spec.md:30`. |
| SC-003 (0 trùng lịch) | **Đạt** ở mức test | `BookingConcurrencyIT`, `SchedulingHardeningIT` 3/3 lần; smoke bước 5. |
| SC-005 (95% phiếu trong 24 giờ) | Có số liệu đầu vào | `onTime`, `dueAt`, nhắc quá hạn chạy; chưa có dữ liệu thật; báo cáo tỷ lệ thuộc K1. |
| SC-006 (hồ sơ, lộ trình, mục tiêu) | API đủ, chưa đo | `needsConsulting` đúng ở bước 11; cần T054, T088, T089. |
| SC-007 (số buổi, công nợ chính xác) | **Đạt** ở mức API | Đối soát SQL toàn CSDL, 3 báo cáo trước = sau ẩn danh. UI chưa có. |
| SC-008 (phiếu < 3 phút) | **NOT RUN** | Cần T101. |
| SC-010 (200 buổi đồng thời, mức BE) | **NOT RUN** | T141 chưa làm, `perf/` trống. |

**28 task còn `[ ]` (`tasks.md`):**

| Người | Task | Tình trạng |
|-------|------|------------|
| #2 | T075, T076, T077, T078, T079 | Có file ở `pr7`, chưa vào `main`. |
| #2 | T141 (k6), T142 (Playwright) | Chưa có. |
| #3 | T088, T089, T136 | Có file ở `pr7`. |
| #3 | **T101, T102, T103, T122** | Không nhánh nào có. |
| #4 | T052, T053, T054, T055 | Có file ở `pr7`. |
| #4 | T114, T116, T130 | Không có đúng tên file; `pr7` có `ManualPaymentDialog`, `StudentPackagesPage`, `admin/users/GuardianConsentDialog`; **QE chưa kiểm có đủ phạm vi task không**. |
| #4 | **T115, T123, T131** | Không nhánh nào có. |
| #3, #4 | T143 (mobile, Lighthouse) | Chưa có. |
| #1 | T140 (rà bảo mật), T145 (README) | Chưa `[X]`. |
| #1, cả team | T146 (24 bước trên môi trường demo) | Chưa `[X]`; smoke này chỉ là phần API. |

12 task có file chính ở `pr7` vẫn ghi `[ ]` vì chưa merge. Demo 2026-11-12.

## 3. Review độc lập (theo constitution)

QE chỉ rà ở mức bằng chứng build/test, không phải code review. Lượt này không có reviewer độc lập.

| Nguyên tắc | Kết quả (Đạt / Vi phạm / N/A) | Bằng chứng (file:dòng) |
|------------|-------------------------------|------------------------|
| I. Modular Monolith, `ApplicationModules.verify()` | Đạt (local) | `ModularityTests` 2/0/0. |
| II. Provider-Agnostic | Đạt ở phần đã chạy | Đổi `VIDEO_PROVIDER=fake`, `EMAIL_PROVIDER=fake` không sửa nghiệp vụ. LiveKit thật NOT RUN. |
| III. Free-First & YAGNI | Đạt | Smoke chỉ cần Postgres, Mailpit. |
| IV. Kiểm thử trọng yếu, Postgres thật, CI mỗi PR | Đạt một phần | 257/0/0 trên `postgres:16` thật. Chưa có test nâng cấp CSDL (G1). CI chưa chạy. |
| V. Bảo mật & dữ liệu cá nhân | **Cần xác nhận** | `pg_dump` sạch trừ 2 ngoại lệ; gate `/api/v%31` đúng. Ngoại lệ GV đã bị đổi đang **"Đề xuất, chờ leader xác nhận"** (`plan.md:177`, `constitution.md:70`), phần `GET /sessions` chưa nằm trong đó (F3). |
| Ràng buộc công nghệ | Đạt | Enforcer pass; nâng cấp và cài mới ra schema như nhau. |
| Quy trình & DoD (CI + review + demo) | Chưa đạt | U6 chưa commit, chưa có PR/CI; 28 task mở; FE chưa vào `main`. |

## 4. Phát hiện

| # | Mức độ | Mô tả | File | Đề xuất |
|---|--------|-------|------|---------|
| F1 | Trung bình | **FR-007 thiếu lọc theo khóa học ở API.** `GET /students?courseId=` trả 7/7 dòng, `GET /sessions?courseId=` trả 25/25: tham số bị bỏ qua, không báo lỗi. FR-007 là MUST và nêu "khóa học"; contract cũng không có tham số này. Riêng `q` tuy chạy đúng nhưng không có test nào. | `ProfileController.java:112-117`; `SessionController.java:139-148`; `rest-api.md:51,76`; `spec.md:209` | Thêm `courseId`, hoặc người dùng chốt bỏ vế này khỏi FR-007 cho MVP. Thêm test `q`. |
| F2 | Thấp | **Contract chưa nói hủy ghi danh bị chặn khi còn buổi đã đặt.** Smoke: 409 `INVALID_STATE_TRANSITION`. Hành vi có chủ đích và có test (`EnrollmentTest.java:124`), nhưng #4 chỉ đọc contract thì không biết để hiện nút hủy buổi trước. | `rest-api.md:62`; `Enrollment.java:233-236` | Ghi vào rest-api.md, báo #4. |
| F3 | Thấp, cần chốt | **GV đã bị đổi vẫn đọc buổi đã dạy.** Smoke: `GET /sessions?teacherId=&size=5` → 200 (5 dòng), `GET /sessions/{id}` → 200 có `studentId`, `cancelReason`, kể cả sau khi HV ẩn danh (chỉ còn id). Không có điểm, nhận xét. Là R18 của U6; dòng ngoại lệ trong plan.md chưa nêu việc này. | `SessionController.java:139-182`; `plan.md:177` | Khi leader xác nhận dòng plan.md:177, chốt luôn việc này ở T140: cho phép thì ghi vào đó. |
| F4 | Thấp | **Link trong email tới `/sessions/:id` (37/55 thư lượt chính) không có route trên FE `main`**, nên về trang chủ; chưa đăng nhập thì mất cả đích sau đăng nhập, vì `*` → `HomeRedirect` không truyền `from`. `pr7` có route, sẽ hết khi merge. Mở lúc đã đăng nhập và đích của từng link trên `pr7`: NOT RUN. | `frontend/src/app/router.tsx:79`; `RequireRole.tsx:32-35` | Merge FE trước demo, rồi bấm thử link ở T146. |
| F5 | Thông tin | Log seed in mật khẩu demo: `Demo seed: tạo 8 tài khoản demo (mật khẩu Demo@1234)`. Chỉ ở profile `demo-seed`, chỉ lần seed đầu (quan sát ở 2 log cài mới), mật khẩu đã công khai trong quickstart. | `IdentityDemoSeed.java:74` | Bỏ giá trị khỏi log nếu tiện. |
| F6 | Thấp | **`OptimisticLockException` ở `SystemCancelListener` vẫn xuất hiện trong `verify`** (dòng 1013). Cùng hiện tượng F7 của U8: test phát `StudentAnonymized` 2 lần liền. Test xanh. Smoke không gặp (0 ERROR ở 7 log). Rủi ro production thuộc R6 của U7. | `SchedulingNotificationIT.java:139-140` | Chờ lần phát đầu xong rồi mới phát lần 2. |
| F7 | Thông tin, cần chốt | **FR-028 "xuất được" không có API xuất và không có task.** Dữ liệu đọc được qua nhiều API, có cấu trúc. `stories.md:721` đã ghi "một phần", xem mục Gaps. | `spec.md:245`; `stories.md:721` | Xác nhận phần "xuất" thuộc GĐ2 hay coi `/assignments`, `/progress` là đủ. |
| F8 | Thấp | **Dữ liệu demo không có phiếu.** Sau toàn bộ kịch bản, 9 buổi `COMPLETED` từ seed không có phiếu, nên biểu đồ tiến bộ của HV seed rỗng tới khi làm bước 8. Là F10 của U6, chưa đổi. Buổi chốt trước nâng cấp cũng không có phiếu (`GET report` → 404), chưa có dữ liệu thật nên chưa ảnh hưởng. | `SchedulingDemoSeed.java`; `LearningDemoSeed.java` | Thêm vài phiếu vào seed nếu muốn biểu đồ có sẵn khi demo. |
| F9 | Thông tin | **Nội dung vài thư còn thiếu ngữ cảnh.** (a) Thư hủy tới GV không nêu tên HV. (b) Thư đổi lịch chỉ có giờ mới. (c) Thư nhắc phiếu quá hạn không nêu HV, giờ buổi. (d) Thư gia hạn gửi **giáo vụ** vẫn ghi "bạn vui lòng liên hệ giáo vụ". (e) Thư kích hoạt có nút "Đặt lịch học ngay" nhưng link tới `/student/packages`, không phải `/student/book`. Không sai nghiệp vụ. | `session-cancelled.html:14`; `session-rescheduled.html:14`; `session-report-overdue.html:11`; `renewal-reminder.html:18`; `CatalogNotificationListener.java:89` | Sửa mẫu nếu còn thời gian. Riêng (a), (c): cân nhắc vì tên HV là dữ liệu cá nhân. |

### Chưa có test tự động (chỉ smoke này phủ)

| # | Nội dung | Hiện có |
|---|----------|---------|
| G1 | Nâng cấp CSDL từ bản cũ, V7, V9 out-of-order. | Không có test; `verify` chỉ cài mới. Smoke đạt. |
| G2 | Khởi động thật `demo-seed` sau ẩn danh `student1`. | `LearningDemoSeedTest` 1 test mức unit. Smoke đạt. |
| G3 | Thân thư và link trong thư. | Test listener dựng event. Smoke đạt ở 73 thư. |
| G4 | `q` của `/users`, `/students`. | Không tìm thấy trong `src/test`. Smoke đạt. |

**Theo dõi các mục R20 của U6:** nâng cấp từ 32d1a6a, bước 23 `pg_dump`, thư F1 sau đổi GV, khởi động lại `demo-seed`: **cả 4 đều đạt trên bản hiện tại**. R14 phần ghi chủ đề đã đóng trên app thật (400); phần xác nhận của leader còn mở.

## 5. Verdict

**PASS_WITH_NOTES** (backend) — `clean verify` BUILD SUCCESS (98/0/0 unit, 159/0/0 IT), `spotless:check` không cache PASS, 4 IT đồng thời xanh 2/2 lần lặp, smoke API 24 bước cùng R20, F2, chạy bù, `/api/v%31`, ẩn danh hóa và `pg_dump` đều đúng trên jar và Postgres thật, không có lỗi mất dữ liệu; nhưng F1 cần sửa hoặc được chấp nhận, ngoại lệ `plan.md:177` còn chờ xác nhận, và **toàn feature chưa sẵn sàng demo** vì FE U2–U5 chưa vào `main`, FE U6, U8, trang tóm tắt U7 chưa có ở nhánh nào; giao diện, video thật, CI, Playwright, k6, gitleaks là NOT RUN.

## 6. Chạy lại 2026-11-08 (sau khi có FE)

Các mục trên là lượt 2026-11-03, chỉ backend. Lượt này chạy trên `main` `751e143` cộng R17 (nhánh `fe/r17-policy-page`, chưa commit), sau khi FE U2–U8 đã vào `main` qua PR #13–#34.

| Hạng mục | Lệnh | Kết quả |
|---|---|---|
| Backend | `./mvnw clean verify` | **PASS** (exit 0): 100/0/0 unit, 185/0/0 IT |
| Frontend | `tsc -b`, `npm run lint`, `vitest run`, `npm run build` | **PASS**: 106/106 test; lint chỉ có cảnh báo `only-export-components` có sẵn |
| Smoke Playwright (T142) | `npm run e2e`, database sạch, backend jar profile `local,demo-seed` | **PASS** 10/10 |
| k6 (T141) | — | Không chạy lại; đạt 2026-11-05 |
| Mobile, Lighthouse (T143) | script trong PR #34 | Đạt phần đo; usability với người: **NOT RUN**, làm 11/11 |
| Quickstart 24 bước qua giao diện (T146) | — | **NOT RUN**, tổng duyệt 11/11 |

Các mục trước đây NOT RUN vì thiếu FE (mục 2.3, 2.6): đã có màn hình, Playwright phủ bước 1–5, 8, 11, 15–17; phòng học (bước 7) kiểm thủ công với LiveKit thật ở T079 và T143; các bước còn lại kiểm thủ công khi làm từng task FE, chưa có lượt chạy trọn 24 bước qua giao diện. F4 (link trong email tới `/sessions/:id`): route đã có trên `main`; bấm thử từ thư thật để T146.

**Verdict lượt này: PASS_WITH_NOTES**: build và test xanh cả backend lẫn FE; còn T143 (usability) và T146 cần người, làm ngày 11/11.
