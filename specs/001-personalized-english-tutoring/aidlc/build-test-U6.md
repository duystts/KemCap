# Build & Test Report — U6 — Nền tảng đào tạo tiếng Anh 1 kèm 1 cá nhân hóa (001-personalized-english-tutoring)

- **Ngày (UTC)**: 2026-11-03, 19:02–19:23 UTC (giờ máy: 2026-11-04, 02:02–02:23 +07)
- **Unit**: U6, phiếu đánh giá, bài tập, tiến bộ, đánh giá sao (B6), **chỉ phần BACKEND**. FE T101–T103, T122 (#3) ghi NOT RUN.
- **Tasks trong phạm vi**: T091–T100, T117, T119, T120 — hoàn thành: **13/13 task BE** là `[X]` (tasks.md:228-229, 233-240, 286, 291-292). T101–T103, T122 vẫn `[ ]` (tasks.md:241-243, 294).
  - Thay đổi chưa commit: `git status` có 25 mục (9 modified, 16 untracked) trước và sau lượt chạy, chưa tính file báo cáo này; đây là file duy nhất QE thêm. Không sửa migration cũ; thêm `V7__learning_reports.sql`, `V9__learning_feedback.sql`.
- **Môi trường**: Windows 11, Git Bash; JDK 21.0.12 (`JAVA_HOME=/c/Program Files/Java/jdk-21.0.12`); Docker Server 29.8.2; Testcontainers và smoke dùng `postgres:16`.

## 1. Build

| Thành phần | Lệnh | Kết quả | Ghi chú |
|------------|------|---------|---------|
| Backend (Maven, gồm Enforcer) | `cd backend && ./mvnw -B clean verify` | **PASS** (`BUILD SUCCESS`, exit 0, 03:59 min, xong lúc 02:06:08 +07) | `RequireJavaVersion passed`, `BannedDependencies passed`. Log 1113 dòng, 0 dòng `[ERROR]`, 4 dòng log ứng dụng mức ERROR (xem dưới). |
| Backend format (không cache) | Xóa `backend/target/spotless-index`, rồi `./mvnw -B spotless:check` | **PASS** (`BUILD SUCCESS`, exit 0, 6.545 s) | `keeping 243 files clean - 0 needs changes`, 243 file được kiểm thật, 0 bỏ qua do cache. |
| Jar commit trước (chỉ để dựng CSDL nâng cấp) | `git archive HEAD backend` (32d1a6a) vào thư mục tạm, `./mvnw -B -q -DskipTests -Dspotless.check.skip=true package` | **PASS** (exit 0) | Đã xóa sau khi xong; không dùng worktree, không đụng repo. |
| Frontend | — | **NOT RUN** | T101–T103, T122 thuộc #3, ngoài phạm vi BE. |

**Bốn dòng ERROR trong log `verify` (test vẫn pass):**

| Dòng | Nội dung | Nhận định |
|------|----------|-----------|
| 165, 257 | `EnrollmentLifecycleStartup`, `EnrollmentLifecycleJob`: `DB bận`, `lỗi bất kỳ` | Lỗi `EnrollmentLifecycleJobTest` cố ý tạo. |
| 742 | `IdentityNotificationListener`: `listener sập` | Lỗi `EventRedeliveryIT` cố ý tạo. |
| 996 | `SystemCancelListener.on(StudentAnonymized)`: `OptimisticLockException` | Trong `SchedulingNotificationIT`. Đã ghi ở F7 của U8; code listener này không thuộc thay đổi U6. |

## 2. Test

| Bộ test | Lệnh | Pass / Fail / Skip | Ghi chú |
|---------|------|--------------------|---------|
| Unit + Scenario (surefire) | `./mvnw -B clean verify` | **97 / 0 / 0** | Khớp số người triển khai báo. |
| Integration Testcontainers (failsafe) | `./mvnw -B clean verify` | **147 / 0 / 0** (`failsafe-summary.xml`: completed 147, errors 0, failures 0, skipped 0, flakes 0) | Khớp số người triển khai báo. Mới ở U6: `SessionReportFlowIT` 9, `AssignmentFlowIT` 5, `SessionFeedbackIT` 3. `AnonymizationIT` 15 pass. |
| Ranh giới module | `ModularityTests` (trong `verify`) | **2 / 0 / 0** | `AssignmentService` đi qua `EnrollmentApi.findAssignedStudentIds` mới. |
| Quét log của `verify` | Tìm trong log | **Không thấy rò rỉ** (quan sát, không phải test) | 0 khớp với `password`, `$2a$`, `bearer `, `eyJ`, `inviteToken`, `refreshToken`, `accessToken`, `@demo.local`, `@test`. |
| Nâng cấp CSDL từ commit trước | Jar 32d1a6a, rồi jar U6, cùng CSDL | **PASS** | Xem chi tiết. |
| Smoke quickstart bước 8, 10, 11, 12, phần sao | Jar thật, Postgres và Mailpit thật | **PASS, có ghi chú (F1, F3, F4, F5)** | Xem chi tiết. |
| Quá hạn 24h, job chạy thật | Như trên, cron 5 giây | **PASS** | Xem chi tiết. Cron mặc định đã đăng ký, nhưng **không chờ** tới đúng đầu giờ. |
| Smoke quickstart bước 23 (ẩn danh hóa, `pg_dump`) | Như trên | **PASS, có ghi chú (F2, F6)** | Xem chi tiết. |
| Bước 24 (`FEEDBACK_EXISTS`, GV bị chặn) | Như trên | **PASS** | Nằm trong phần sao. |
| FE T101, T102, T103, T122 | — | **NOT RUN** | Thuộc #3. Vì vậy các bước trên chỉ kiểm API, chưa kiểm giao diện; chưa đo SC-008 (điền phiếu dưới 3 phút). |
| Playwright, k6, gitleaks | — | **NOT RUN** | Không có trong danh sách lệnh của lượt này. |
| CI GitHub Actions trên PR thật | `.github/workflows/ci.yml` | **NOT RUN** | Thay đổi U6 chưa commit, chưa có PR. |

### Chi tiết smoke local

**Cấu hình:** `docker compose -p u6smoke -f <file tạm> up -d --wait`, file tạm chỉ có `postgres` và `mailpit` để đổi port (15432, 11025, 18025; app 18080); jar từ `verify`, `VIDEO_PROVIDER=fake`, `--spring.profiles.active=local,demo-seed`. LiveKit không chạy vì các bước này không cần.

**Dữ liệu dựng bằng SQL:** đặt buổi qua API, rồi QE dùng SQL đưa `starts_at`, `ends_at` về quá khứ trước khi chốt (có thể chốt chỉ sau giờ bắt đầu). Riêng kiểm quá hạn, cửa sổ 7 ngày và ca job với HV ẩn danh, QE cũng dùng SQL đổi `ends_at`, `due_at`. Cron quá hạn chạy thật được đặt `*/5 * * * * *` bằng tham số dòng lệnh.

**Dọn dẹp:** đã dừng app và chạy `docker compose -p u6smoke down -v`; không còn container, volume, network `u6smoke`; 4 port đã trống; không đụng project compose mặc định. Mailpit đã bị xóa cùng stack.

| Nội dung | Thao tác | Quan sát |
|----------|----------|----------|
| Nâng cấp CSDL | Jar 32d1a6a trên CSDL trống: áp dụng 13 migration, seed, chốt 1 buổi `COMPLETED`. Dừng, chạy jar U6 trên cùng CSDL | Log: `Migrating schema "public" to version "7 - learning reports" [out of order]`, `"9 - learning feedback" [out of order]`, `Successfully applied 2 migrations`. `ddl-auto: validate` đạt, 0 dòng ERROR. `flyway_schema_history`: V7 rank 14, V9 rank 15, sau `202611031700`, đều `success`. Seed không tạo trùng: 13 buổi, 8 tài khoản, 4 ghi danh, 3 lộ trình. |
| Schema nâng cấp so với cài mới | Chạy jar U6 trên CSDL trống (15 migration, theo thứ tự số), so `pg_dump --schema-only` (bỏ bảng Flyway) | Khác nhau duy nhất ở token ngẫu nhiên `\restrict` do `pg_dump` sinh. |
| Dữ liệu trước nâng cấp | 9 buổi `COMPLETED` có sẵn | `session_report` 0 dòng; `GET /sessions/{id}/report` → 404; `/progress` → `skillSeries` rỗng (F10). |
| Bước 8: chốt buổi | GV chốt `COMPLETED` | Bộ đếm `đã đặt 1, đã dùng 3, còn 20` → `1, 4, 19`. Phiếu `PENDING`, `dueAt` = `sessionEndsAt` + 24h, `overdue=false`. GV và admin thấy trong danh sách, GV khác không thấy; HV → 403, không token → 401, `status=abc` → 400. |
| Bước 8: kiểm tra đầu vào | 14 body sai + JSON hỏng | 400 `VALIDATION_FAILED`: thiếu hoặc trống nhận xét, không có điểm, điểm 0 và 6, điểm dạng chuỗi, chủ đề ngoài lộ trình (cả 2 trường), chủ đề null, bài tập không tiêu đề, tiêu đề 201 ký tự, hạn sai định dạng. **`"assignments":[null]` → 500 (F4).** Phiếu vẫn `PENDING`, 0 bài tập. |
| Bước 8: nộp phiếu | GV khác → 404, HV → 403, admin → 403, không token → 401. GV của buổi nộp 2 chủ đề, 4 điểm, nhận xét, 3 bài tập | 200 `SUBMITTED`, `onTime=true`. Nộp lần 2 → 409. Chủ đề trong `completedTopicIds` thành `DONE`, chủ đề chỉ dạy vẫn `PLANNED`. Mailpit: "Đã có nhận xét cho buổi học vừa qua" tới HV. Thông báo trong app `REPORT_SUBMITTED` có. |
| Bước 10: HV nộp bài | Bài có hạn, không hạn, đã quá hạn | Trống và 20001 ký tự → 400; HV khác → 404; GV → 403; GV chấm khi chưa nộp → 409. Nộp → `SUBMITTED`, `late=false`; nộp lại đổi nội dung; bài không hạn `late=false`; bài quá hạn 200, `late=true`. Mailpit: "Học viên Lê Hoàng An đã nộp bài tập" tới GV, mỗi bài 1 thư; nộp lại không có thư thứ 2 (F9). |
| Bước 10: GV chấm | Điểm 11, −1, thiếu, chuỗi → 400. Admin, HV → 403; GV khác → 404 | Điểm 8 kèm phản hồi → 200 `GRADED`, `gradedBy` đúng. Chấm lại → 409; nộp lại sau khi chấm → 409. HV đọc được điểm và phản hồi. Mailpit: "Bài tập của bạn đã được chấm điểm (8/10)" tới HV. |
| Sao từng buổi, bước 24 | 0 sao, 6 sao, thiếu, chuỗi, nhận xét 2001 ký tự → 400 | 5 sao → 201; lần 2 → 409 `FEEDBACK_EXISTS`, CSDL vẫn 5. HV khác → 404; GV, admin gửi → 403. GV, HV đọc → 403; admin đọc → 200 kèm nhận xét; không token → 401. Buổi `BOOKED` → 409. Còn 7 ngày trừ 2 phút → 201; quá 7 ngày 2 giờ → 409 `FEEDBACK_WINDOW_CLOSED`. Đúng mốc 7 ngày: **NOT RUN**. |
| Bước 11: tiến bộ | HV, GV phụ trách, admin → 200; GV khác, HV khác → 404 | `skillSeries` đúng điểm, ngày theo giờ VN (`ends_at` 18:11Z → 2026-11-04). `pathCompletion`=10, khớp SQL 1/10 và `completionPercent` của lộ trình. `recentReports` có nhận xét. Chỉ phiếu đã nộp mới vào biểu đồ. |
| Bước 12: báo cáo | Thanh toán 700000 rồi so SQL | `collectedVnd`=700000 = `SUM(payment.amount_vnd)`. `sessionsTaught` 6 và 12 khớp `COUNT` theo 3 trạng thái, gồm các buổi chốt trong lượt này. GV, HV → 403. Dashboard FE: **NOT RUN**. |
| Quá hạn 24h | Cron 5 giây; phiếu quá hạn, phiếu chưa tới hạn, phiếu đã nộp | Sau 12 s: đúng 1 thư "Nhắc nhở nộp phiếu đánh giá sau buổi học" tới GV, chỉ phiếu quá hạn có `overdue_notified_at`. Sau 28 s (nhiều lần chạy): vẫn 1 thư, 1 dòng dispatch `SENT`. `REPORT_OVERDUE` trong app có. Danh sách `overdue=true` đúng phiếu. Bảng `shedlock` có `learning.reportOverdue`. |
| Qua mốc hạn | Đặt `due_at` = hiện tại + 6 s | Sau 2 s chưa nhắc; sau khi qua hạn nhắc đúng 1 lần. Nộp muộn → 200, `onTime=false`, không có thư thêm. |
| Cron mặc định | `/actuator/scheduledtasks` (mở bằng cấu hình dòng lệnh, token admin) | `0 0 * * * *` → `ReportOverdueJob.run`. |
| Sửa kết quả buổi | Admin sửa `COMPLETED` → `STUDENT_NO_SHOW` | Phiếu `PENDING` bị xóa; sửa lại `COMPLETED` tạo lại phiếu. Phiếu `SUBMITTED`: phiếu, bài tập, sao đều còn và **buổi no-show vẫn vào biểu đồ** (F5). Sửa lại `COMPLETED`: vẫn 1 phiếu. |
| Gián đoạn | Chốt `INTERRUPTED` | Chưa có phiếu. `COUNT` → `COMPLETED`, có phiếu, chấm sao được. `MAKE_UP` → 0 phiếu. |
| Đồng thời (1 lần mỗi ca) | Gửi song song | Nộp phiếu ×8: 1 × 200, 7 × 409, 1 bộ bài tập, 1 dispatch. Chấm ×6: 1 × 200, 5 × 409, 1 dispatch. Sao ×8: 1 × 201, 7 × `FEEDBACK_EXISTS`, 1 dòng. **Nộp bài lần đầu ×8: 1 × 200, 7 × 500 (F3)**, 1 dòng. |
| Đổi giáo viên | HV3 đổi từ GV2 sang GV1 | GV cũ vẫn nộp được phiếu của buổi mình dạy; GV mới không thấy phiếu đó. **HV nộp bài: thư chỉ tới GV cũ, GV cũ chấm → 404, GV mới chấm → 200 nhưng không được báo (F1).** GV cũ: đọc phiếu → 200, `/progress` → 404. GV mới: cả hai → 200. |
| Bước 23: yêu cầu xem | Ghi `ACCESS`, gọi 7 API trang tóm tắt (rest-api.md:43) | 7 API đều 200; `complete-access` → `COMPLETED`. Trang FE: **NOT RUN**. Không gồm đánh giá sao (F8). |
| Bước 23: ẩn danh hóa | `student1`: 3 bài tập, 3 bài nộp, 4 phiếu, 2 đánh giá, 1 lộ trình; `confirm=false` → 400; `confirm=true` → 200 | `/reports/revenue`, `/receivables` (bỏ cột tên), `/reports/teacher-sessions` trước = sau. Đăng nhập → 401; access token cũ gửi sao và đọc tiến bộ → 401. Bài tập 0, bài nộp 0, lộ trình 0; `session_report` còn 4 dòng, điểm, nhận xét, chủ đề đều null, giữ `status`, `submitted_at`, `due_at`. Sao giữ 5, nhận xét null. Danh sách phiếu và bài tập của GV, admin không còn HV này. GV nộp phiếu còn treo → 409 `ANONYMIZED`. `/progress` vẫn trả điểm toàn null (F6). |
| Bước 23: job, chốt sau ẩn danh | Đưa phiếu còn treo quá hạn; chốt `COMPLETED` buổi quá khứ của HV đã ẩn danh | Không có nhắc thêm (2 → 2 thư). Chốt → 200, 0 phiếu mới. Mailpit có 0 thư tới `@invalid`. |
| Bước 23: `pg_dump --data-only --inserts` | Trước và sau, quét toàn bộ dòng | Trước: tên 15 dòng, email 9 dòng, các chuỗi đánh dấu trong nhận xét, tiêu đề, hướng dẫn, bài nộp, phản hồi, nhận xét sao đều có (chứng tỏ phép quét bắt được). **Sau: 0 dòng ở mọi chuỗi**, trên 266 dòng INSERT. `user_account`: "Học viên đã ẩn danh", `anonymized+…@invalid`, `phone` và `date_of_birth` null, `INACTIVE`. |
| Khởi động lại sau ẩn danh hóa | Giữ `local,demo-seed` (chạy 2 lần) | **App dừng: `Thiếu tài khoản demo student1@demo.local` tại `LearningDemoSeed.java:66,153` (F2).** Chỉ `local`: khởi động bình thường, 0 dòng ERROR, 0 publication treo, số thư giữ 46. |
| Log và event | Các log app của smoke | `event_publication` chưa xong: 0 sau mỗi phần. 0 khớp email `student1`, tên HV, chuỗi đánh dấu, `eyJ`, `$2a$` trong log phiên có tạo dữ liệu. ERROR chỉ có ở F3, F4 và F2. |

**Giới hạn của email:** QE chỉ kiểm người nhận và tiêu đề trong Mailpit, **không mở nội dung thư** và chưa bấm link. Link dùng `frontendUrl/sessions/{id}`, `/teacher/students/{id}`, `/student/progress`; khớp route FE hay không là NOT RUN.

### Đối chiếu DoD riêng của U6 (unit-of-work.md:382-386)

| Mục DoD | Trạng thái | Test và kết quả quan sát |
|---------|-----------|--------------------------|
| `SessionFinished` → đúng 1 phiếu `PENDING`, phát lại không tạo thêm (NFR-REL-11) | **Đạt** | `completedSessionCreatesOnePendingReportDueIn24Hours` pass (`SessionReportFlowIT.java:44`). Smoke khớp phần tạo phiếu; phát lại event chỉ IT kiểm. |
| Job quá hạn với `TestClock` nhắc đúng 1 lần, phiếu nộp trước hạn không bị nhắc | **Đạt; test dừng ở event** | `overdueReminderFiresOnceAndSkipsSubmitted` pass (`:198`). Smoke đạt tới email thật. Xem G1. |
| Điểm ngoài 1–5 / 0–10 → 400 | **Đạt** | `submitValidatesRequiredFieldsAndTopics`, `studentSubmitsResubmitsUntilGradedThenSeesScore` pass. Smoke khớp. |
| Chủ đề ngoài lộ trình → 400 | **Đạt** | `submitValidatesRequiredFieldsAndTopics` pass. Smoke khớp. |
| Sao lần 2 → `FEEDBACK_EXISTS`; quá 7 ngày → `FEEDBACK_WINDOW_CLOSED`; GV xem → bị từ chối | **Đạt; test không chạm mốc** | `SessionFeedbackIT` 3/3 pass, test dùng mốc 6 và 8 ngày. Smoke khớp. Xem G2. |
| Ma trận phân quyền | **Đạt một phần** | Có test theo từng API (`onlySessionTeacherSubmitsAndStrangersCannotRead`, `accessFollowsAssignment`, `studentRatesOnceAndOnlyAdminReads`, `progressShowsSeriesCompletionAndRecentReports`); chưa có ca 401 (G6). Ma trận đầy đủ thuộc T140. Smoke khớp, có 401. |
| IT ẩn danh hóa (U7) vẫn xanh sau mở rộng | **Đạt** | `AnonymizationIT` 15/15 pass, `anonymizationRemovesIdentityAndKeepsFigures` (`:191`) đã thêm phiếu, bài tập, bài nộp, sao, lộ trình, và quét thêm cột `jsonb`, mảng. Smoke `pg_dump` khớp. |

### DoD và yêu cầu chưa có test tự động

| # | Nội dung | Hiện có |
|---|----------|---------|
| G1 | Job quá hạn → email và thông báo thật. | IT đếm event; `LearningNotificationListenerTest` dựng event tay. Smoke đạt, chưa mở nội dung thư. |
| G2 | Đúng mốc 24h và 7 ngày. | IT dùng 25h, 6 và 8 ngày. Smoke kiểm sát mốc, không đúng mốc. |
| G3 | Nâng cấp CSDL có sẵn (V7, V9 out-of-order). | Testcontainers chỉ cài mới. Smoke đạt. |
| G4 | Thông báo bài nộp sau khi đổi GV. | IT `teacherOfSessionStillSubmitsAfterReassignment` không phủ. Smoke ra F1. |
| G5 | Đồng thời: nộp phiếu, nộp bài lần đầu, chấm, chấm sao. | Không có IT. Smoke 1 lần, ra F3; mẫu nhỏ. |
| G6 | 401 cho các endpoint U6. | `src/test/.../learning/` không có `isUnauthorized`. Smoke đạt 3 endpoint. Chuyển T140. |
| G7 | `interruption-decision` COUNT → phiếu và chấm sao; `MAKE_UP` không có phiếu. | Không có IT ở `learning`. Smoke đạt. |
| G8 | Sửa kết quả của buổi có phiếu `SUBMITTED`. | Không có test. Smoke ra F5. |
| G9 | `assignments` chứa null. | Không có test. Smoke ra F4. |
| G10 | Khởi động lại với `demo-seed` sau khi ẩn danh tài khoản demo. | Không có test. Smoke ra F2. |

### Đối chiếu task

| Task | Trạng thái | Test thực có |
|------|-----------|--------------|
| T091 | **Đạt** | `SessionReportFlowIT` 9/9; đổi tên thành IT đã ghi ở tasks.md:228. |
| T092 | **Đạt** | `AssignmentFlowIT` 5/5; đổi tên đã ghi ở tasks.md:229. |
| T093 | **Đạt** | Chạy được trên Testcontainers, CSDL trống và CSDL nâng cấp. Có thêm `session_ends_at`, `teacher_id`, đã cập nhật data-model.md. |
| T094, T095 | **Đạt** | Như T091. |
| T096 | **Đạt; có F4** | Như T091, smoke bước 8. |
| T097 | **Đạt** | `overdueReminderFiresOnceAndSkipsSubmitted`; cron mặc định và chạy thật qua smoke; lúc đúng đầu giờ NOT RUN. |
| T098 | **Đạt; có F1, F3** | `AssignmentFlowIT`, smoke bước 10. Không có upload, download đúng K2. |
| T099 | **Đạt** | `progressShowsSeriesCompletionAndRecentReports`, `progressWithoutPathOrReportsIsEmpty`; smoke khớp SQL. Không có `attendanceRate`, `assignmentRate`. |
| T100 | **Đạt; có F6** | `AnonymizationIT`, `anonymizedStudentHasNoReportWorkLeft`; smoke `pg_dump` 0 dòng. |
| T117 | **Đạt** | `SessionFeedbackIT` 3/3. |
| T119 | **Đạt** | Như T093. |
| T120 | **Đạt; có F7, F8** | `SessionFeedbackIT`; smoke sao giữ, nhận xét null. |
| T101, T102, T103, T122 | **NOT RUN** | FE của #3. |

## 3. Review độc lập (theo constitution)

QE chỉ rà ở mức bằng chứng build/test, chưa phải code review đầy đủ.

| Nguyên tắc | Kết quả (Đạt / Vi phạm / N/A) | Bằng chứng (file:dòng) |
|------------|-------------------------------|------------------------|
| I. Modular Monolith, `ApplicationModules.verify()` | Đạt (local) | `ModularityTests` 2/0/0. `learning` đọc qua `EnrollmentApi`, `SessionQueryApi`, `UserDirectory`; `SessionFinished`, event learning đi qua `ApplicationModuleListener`. |
| II. Provider-Agnostic (Ports & Adapters) | N/A | U6 không thêm provider. Email trong smoke vẫn đi qua port tới Mailpit. |
| III. Free-First & YAGNI | Đạt | Không thêm hạ tầng; không có upload (K2), không có báo cáo chất lượng (K1). |
| IV. Kiểm thử trọng yếu, Postgres thật, cấm H2, CI mỗi PR | Đạt một phần | 3 lớp IT mới (17 test) trên `postgres:16` thật; toàn backend 244/0/0 (97 + 147). Còn G1–G10. CI chưa chạy. |
| V. Bảo mật & dữ liệu cá nhân | Đạt ở mức quan sát | `pg_dump` 0 dòng định danh; 3 báo cáo không đổi; token cũ của HV ẩn danh → 401; phân quyền đúng ở mọi ca đã thử. F1 không lộ dữ liệu mới vì thư chỉ có tên HV, nhưng tới GV đã hết phụ trách. |
| Ràng buộc công nghệ (Java 21, Boot, Flyway) | Đạt | Enforcer pass. Migration chạy được cả cài mới và nâng cấp, schema như nhau. Không sửa migration cũ. |
| Quy trình & DoD (CI xanh + review + demo) | Chưa đạt | Gate local xanh; chưa có PR/CI, chưa có review độc lập ghi trong báo cáo này, FE T101–T103, T122 chưa làm. |

## 4. Phát hiện

| # | Mức độ | Mô tả | File | Đề xuất |
|---|--------|-------|------|---------|
| F1 | Trung bình | **Sau khi đổi GV, thông báo bài nộp tới GV cũ, GV đang phụ trách không được báo.** Smoke: thư "đã nộp bài tập" chỉ tới GV cũ; GV cũ chấm → 404; GV mới chấm → 200. Chỉ GV mới chấm được, nên bài có thể nằm chờ không ai biết. Chưa có test. | `AssignmentService.java:114-115`; `AssignmentService.java:122` (`requireWrite`) | Gửi tới GV đang phụ trách lúc nộp (thêm hàm tra trong `EnrollmentApi`). Thêm IT đỏ trước. |
| F2 | Trung bình | **App không khởi động được với `demo-seed` sau khi ẩn danh `student1`.** Tái hiện 2/2 lần: `IllegalStateException: Thiếu tài khoản demo student1@demo.local`. Code seed từ U4, không phải lỗi U6, nhưng chạm thẳng kịch bản demo trên laptop: làm bước 23 với `student1`, khởi động lại là hỏng. Chỉ `local` thì chạy bình thường. | `LearningDemoSeed.java:66,153` | Seed bỏ qua khi thiếu tài khoản. Trước khi sửa, runbook demo nên ẩn danh học viên khác hoặc bỏ `demo-seed` khi khởi động lại. |
| F3 | Thấp | **Nộp bài lần đầu gửi trùng trả 500.** Smoke ×8 song song: 1 × 200, 7 × 500 `INTERNAL_ERROR`, log có stack trace `submission_assignment_id_key`. Dữ liệu vẫn đúng 1 dòng. Bấm đúp là có thể gặp. Đánh giá sao đã xử lý đúng cùng tình huống. | `AssignmentService.java:107-112`; so với `FeedbackService.java:52-58` | Bắt `DataIntegrityViolationException`, trả 409 `CONCURRENT_MODIFICATION`. |
| F4 | Thấp | **`PUT /sessions/{id}/report` với `"assignments":[null]` trả 500** (NPE). Không ghi dữ liệu. `taughtTopicIds:[null]` đã trả 400 đúng. | `SessionReportController.java:153` | Trả 400 `VALIDATION_FAILED`. |
| F5 | Thấp, cần chốt | **Sửa buổi đã có phiếu `SUBMITTED` thành không hoàn thành: phiếu, bài tập, sao đều còn; điểm vẫn vào biểu đồ tiến bộ.** Smoke: buổi `STUDENT_NO_SHOW` vẫn có trong `skillSeries`. Spec và contract chưa nói trường hợp này. Giữ phiếu để không mất dữ liệu là hợp lý, nhưng biểu đồ thì lệch. | `SessionFinishedListener.java:34-36`; `SessionReportRepository.java:41-47`; `ProgressService.java:47` | Người dùng chốt: giữ nguyên và ghi vào contract, hay loại khỏi biểu đồ. |
| F6 | Thấp | **`/progress` của HV đã ẩn danh vẫn trả các điểm toàn null** kèm `sessionId`, ngày, và `recentReports` nhận xét null. Không lộ định danh, nhưng FE sẽ vẽ các điểm rỗng. `AnonymizationIT` chỉ kiểm `pathCompletion`. | `SessionReportRepository.java:30-37`; `AnonymizationIT.java:387-388` | Lọc phiếu toàn null hoặc trả rỗng; thêm assert. |
| F7 | Trung bình | **HV không có API biết mình đã chấm sao, trong khi T122 yêu cầu ẩn lời mời sau khi gửi.** Smoke: HV `GET /sessions/{id}/feedback` → 403. Tải lại trang, FE không biết để ẩn, phải chờ 409. Người triển khai đã ghi nhận ở audit.md (mục 2026-11-03T19:01:28Z), chưa có quyết định. | `FeedbackController.java:61`; `tasks.md:294`; `rest-api.md:110` | Báo #3 trước khi làm T122. Thêm cờ vào `GET /sessions` hoặc cho HV đọc đánh giá của chính mình. |
| F8 | Thấp | **Trang tóm tắt FR-031 không liệt kê đánh giá sao**, gồm nhận xét HV tự viết. HV không có API đọc; contract không nêu nguồn này. Chỉ admin đọc được từng buổi. | `rest-api.md:43`; `FeedbackController.java:61-64` | Bổ sung nguồn vào contract, có thể dùng chung cách sửa F7. |
| F9 | Thông tin | Nộp lại bài không gửi thư thứ 2 vì khóa chống trùng theo bài tập. GV mở bài vẫn thấy bản mới, nên có thể chấp nhận; ghi lại để chốt ý định. | `LearningNotificationListener.java:94` | Chỉ cần xác nhận. |
| F10 | Thấp | **Dữ liệu demo không có phiếu, nên trang tiến bộ rỗng tới khi làm bước 8.** 9 buổi `COMPLETED` từ seed đi thẳng qua entity, không phát event; `session_report` 0 dòng. Biểu đồ trong demo chỉ có điểm từ các buổi làm trực tiếp. Không có bù cho buổi trước nâng cấp; chưa có dữ liệu thật nên chưa ảnh hưởng. | `SchedulingDemoSeed.java:102`; `LearningDemoSeed.java:37` | Thêm vài phiếu đã nộp vào seed nếu muốn biểu đồ có nội dung khi demo. |
| F11 | Thông tin | Đọc code, chưa chạy: `comment`, `instructions` của phiếu và `feedback` khi chấm không giới hạn độ dài, trong khi `textAnswer` ≤ 20000 và nhận xét sao ≤ 2000. | `SessionReportService.java:115-132`; `AssignmentController.java:78` | Thêm giới hạn cùng đợt T140. |

## 5. Verdict

**PASS_WITH_NOTES** — `clean verify` BUILD SUCCESS (97/0/0 unit, 147/0/0 IT), `spotless:check` không cache PASS, nâng cấp V7 và V9 out-of-order trên CSDL có dữ liệu đạt và cho schema như cài mới, smoke bước 8, 10, 11, 12, 23, 24 và quá hạn 24h đúng trên jar và Postgres thật, `pg_dump` sau ẩn danh hóa 0 dòng định danh; không có lỗi mất dữ liệu, nhưng F1, F2, F7 cần sửa hoặc được người dùng chấp nhận trước demo; FE, CI, Playwright, k6, gitleaks và job lúc đúng đầu giờ là NOT RUN.

## 6. Review độc lập (reviewer, đọc code)

**Phạm vi và giới hạn:** reviewer đọc code, migration, test và đặc tả U6; **không chạy build, test hay app**. Hành vi "đã quan sát" lấy từ smoke của QE ở trên, phát hiện nào chỉ suy ra từ code thì ghi rõ. Mục 3 và verdict ở mục 5 là của QE, reviewer không sửa; verdict đề xuất của reviewer ở cuối mục này. Các quyết định đã chốt (K1, K2, không chấm lại, không tạo phiếu cho HV đã ẩn danh, T091/T092 thành IT, FE của #3, ma trận đầy đủ ở T140) không tính là vi phạm.

### Review theo constitution

| Nguyên tắc | Kết quả | Bằng chứng |
|------------|---------|------------|
| I. Modular Monolith | Đạt | `learning` chỉ dùng public API: `EnrollmentApi`, `SessionQueryApi`, `UserDirectory`, `SessionFinished`, `StudentAnonymized`. `scheduling` không import `learning`. `session_report`, `assignment`, `session_feedback` không có FK sang bảng module khác (`V7__learning_reports.sql`, `V9__learning_feedback.sql`). `ModularityTests` pass theo QE. |
| II. Provider-Agnostic | N/A | U6 không thêm provider. |
| III. Free-First & YAGNI | Đạt | Không thêm hạ tầng hay tính năng ngoài spec. |
| IV. Kiểm thử trọng yếu | Đạt một phần | Test trên Postgres thật. Phân quyền là logic trọng yếu nhưng chưa test GV cũ đọc phiếu (R1) và thông báo sau đổi GV (F1). CI chưa chạy. |
| V. Bảo mật & dữ liệu cá nhân | **Vi phạm** | "Giáo viên MUST chỉ truy cập dữ liệu học viên được phân công" (constitution.md:70): xem R1. Phần ẩn danh hóa đạt ở đường chính, còn cửa sổ chen nhau R6. |
| Ràng buộc công nghệ | Đạt | Migration mới, không sửa migration cũ. Sai lệch V7, V9 so với bản ghi gate đã đính chính ở audit.md:96. |
| Quy trình & DoD | Chưa đạt | Chưa có PR, CI; FE chưa nối API. |

### Đối chiếu phân quyền từng endpoint

| Endpoint | Hợp đồng | Thực tế | Kết quả |
|----------|----------|---------|---------|
| `GET /session-reports` | T; A | Lọc theo GV của phiếu, không xét phân công hiện tại | **R1** |
| `GET /sessions/{id}/report` | A; T; S | GV của phiếu được đọc không xét phân công; còn lại qua guard | **R1** |
| `PUT /sessions/{id}/report` | T, chỉ GV của buổi | Đúng; ngoại lệ sau đổi GV có ghi trong hợp đồng (rest-api.md:102) | Đạt, xem R5 |
| `GET /assignments` | A; T; S | Đúng phạm vi, GV cũ không còn thấy | Đạt |
| `PUT /assignments/{id}/submission` | S | Chỉ chủ bài, người khác 404 | Đạt |
| `POST /assignments/{id}/grade` | T, GV đang phụ trách | Đúng; admin 403 | Đạt |
| `GET /students/{id}/progress` | A; T; S | Qua guard, không có quyền 404 | Đạt |
| `POST /sessions/{id}/feedback` | S | Đúng HV, COMPLETED, 7 ngày, một lần | Đạt |
| `GET /sessions/{id}/feedback` | A | Chỉ admin | Đạt |

Mọi method có `@PreAuthorize` (NFR-SEC-01 ở mức code; ma trận test thuộc T140).

### Đối chiếu NFR và story

| Mục | Kết quả | Ghi chú |
|-----|---------|---------|
| NFR-PERF-04 | NOT RUN | k6 và usability chưa chạy. |
| NFR-PERF-06 | Đạt ở mức code | Cron mỗi giờ, lọc `due_at <= now`; lần chạy đầu giờ cách `due_at` tối đa 60 phút. Chưa chờ đúng đầu giờ thật. |
| NFR-REL-05 | Đạt một phần | `dedupe_key` và `session_id` UNIQUE đúng; vế "mỗi buổi COMPLETED có đúng 1 phiếu" hở khi event sai thứ tự (R2). |
| NFR-REL-07 | Đạt | `@SchedulerLock` PT5M; `overdue_notified_at` với update có điều kiện chặn nhắc trùng, cả khi chạy chồng. |
| NFR-REL-11 | Đạt ở đường chính | Hở như R2. |
| NFR-SEC-01 | Đạt ở mức code | Xem trên. |
| NFR-SEC-02 | **Vi phạm** | R1. |
| NFR-PORT-03 | Đạt | `smallint` có CHECK, `uuid[]`. Xem R9. |
| NFR-PRIV-02 | Đạt ở mức code | Service U6 không log nội dung; `DispatchService` chỉ log dedupe key và tên lỗi. |
| NFR-UX-01 | Đạt phần BE | Thông điệp lỗi, tiêu đề thư tiếng Việt; không mở hết nội dung thư. Mẫu `session-report-submitted` có lời mời chấm sao đúng events-and-ports.md:35. |
| S-US4.1 AC5, S-US4.3 AC1 | **Chưa đạt ở API** | R3. |
| S-US4.4 AC1 | Đạt một phần | F1. |
| S-US6.1 AC1 | Chưa đủ cho FE | F7. |
| S-US1.8 AC2 (ràng buộc liên quan) | **Vi phạm** | R1. |
| S-US4.2, S-US4.5, S-US4.6, S-US6.1 AC2–AC6 | Đạt | Theo IT và smoke; riêng số % xem R4, buổi bị sửa kết quả xem F5. |

### Xác minh F1, F2, F5, F7

| # | Xác minh | Mức reviewer | Cách sửa cụ thể |
|---|----------|--------------|-----------------|
| F1 | **Đúng.** Event dùng `assignment.getTeacherId()` cố định lúc giao (`AssignmentService.java:113-115`), còn chấm đòi phân công hiện tại (`:122`, `StudentAccessGuard.java:33-34`). Thêm hai điều QE chưa nêu: khóa `learning:assignment-submitted:{assignmentId}` không có id người nhận (`LearningNotificationListener.java:94`), nên không thể chỉ đổi người nhận cho bản đã gửi; và GV cũ nhận tên HV trong tiêu đề, thông báo sau khi đã mất quyền. HV có thể có nhiều ghi danh với nhiều GV, nên không tồn tại đúng một "GV đang phụ trách". | **MAJOR** | Thêm `EnrollmentApi.findAssignedTeacherIds(studentId)` dùng `Enrollment.ASSIGNED`. Trong `submit`: nếu `isAssigned(assignment.teacherId, studentId)` thì gửi GV đó; nếu không, phát cho từng GV trong danh sách; danh sách rỗng thì không phát. Thêm `teacherId` vào dedupe key. Viết IT đỏ trước, mở rộng `teacherOfSessionStillSubmitsAfterReassignment`. Bài nộp trước lúc đổi GV không tự báo lại: GV mới thấy qua `GET /assignments?status=SUBMITTED`. |
| F2 | **Đúng.** `LearningDemoSeed.java:66` tra `student1` trước khi kiểm tra đã seed, `:153` ném lỗi. Seed catalog và scheduling kiểm tra rồi `return` trước khi đụng HV (`CatalogDemoSeed.java:55-57`, `SchedulingDemoSeed.java:69-71`), không có seed nào `@Order` sau 40, nên chỉ cần sửa file này. | **MAJOR** (cho demo; không ảnh hưởng profile khác) | Đầu `run`: `if (assessments.count() > 0 \|\| userDirectory.findByEmail("student1@demo.local").isEmpty()) return;`. Thêm test: seed chạy lại sau ẩn danh `student1` không ném lỗi. |
| F5 | **Đúng, và rộng hơn mô tả.** `SessionFinishedListener.java:34-36` chỉ xóa `PENDING`; `SessionReportRepository.java:30-37` không biết kết quả buổi. Ngoài biểu đồ: chủ đề đã `DONE` không hoàn tác, bài tập vẫn nộp, chấm được. Ca này cần admin sửa buổi đã có phiếu trong 7 ngày nên hiếm. | **MINOR**, cần người dùng chốt | Đề xuất giữ dữ liệu, loại khỏi biểu đồ: migration mới thêm `session_report.session_voided_at`; listener đặt khi kết quả không còn COMPLETED, xóa khi quay lại COMPLETED; `findSubmittedByStudent` thêm `voidedAt is null`. Không hoàn tác chủ đề và bài tập vì là việc đã xảy ra. Nếu không sửa: ghi hành vi hiện tại vào rest-api.md:108. |
| F7 | **Đúng.** `FeedbackController.java:60-61` chỉ cho ADMIN; T122 (tasks.md:294) cần ẩn lời mời sau khi gửi. **Không nên** theo đề xuất "thêm cờ vào `GET /sessions`" của F7: `learning` đã phụ thuộc `scheduling`, cờ đó tạo phụ thuộc ngược và vòng module, trái nguyên tắc I. Mở `GET /sessions/{id}/feedback` cho HV thì trang chủ phải gọi từng buổi (N+1 phía FE). | **MAJOR** (chặn T122 của #3) | Thêm `GET /session-feedback/mine` quyền S, trong `learning`: trả `[{sessionId, stars, createdAt}]` của chính HV, không có nhận xét của người khác. FE lấy danh sách này ghép với `/sessions`. Cập nhật rest-api.md, thêm vào IT. F8 thì khác: trang tóm tắt do admin ghép, đã đọc được từng buổi, chỉ cần bổ sung nguồn vào rest-api.md:43. |

### Findings

| # | Mức | Mô tả và lý do | File:dòng | Đề xuất |
|---|-----|----------------|-----------|---------|
| R1 | **BLOCKING** | **GV cũ vẫn đọc được phiếu của HV đã chuyển đi.** `get` bỏ qua guard nếu là GV của phiếu; `list` chỉ lọc theo `teacher_id`, gồm cả phiếu `SUBMITTED`, kèm tên HV, điểm và nhận xét. Smoke của QE đã quan sát: GV cũ đọc phiếu → 200, `/progress` → 404. Trái constitution.md:70, FR-003, S-US1.8 AC2 ("API từ chối truy cập dữ liệu của học viên"), NFR-SEC-02. Quyết định đã chốt và rest-api.md:102 chỉ cho ngoại lệ **nộp** phiếu còn treo, không cho đọc lịch sử. Không có test nào phủ. | `SessionReportService.java:72-73`, `:79-81`; `SessionReportController.java:94-130` | `get`: chỉ bỏ qua guard khi `teacherId == actor` **và** phiếu `PENDING`. `list` với GV: giữ phiếu `PENDING` của mình, phiếu khác chỉ giữ khi `studentId` thuộc `enrollmentApi.findAssignedStudentIds(actor.id())`. Viết IT đỏ trước: sau đổi GV, GV cũ `GET` phiếu đã nộp → 404, danh sách không có; GV mới → 200. Nếu người dùng muốn GV cũ đọc được phiếu mình viết: phải chốt rõ, sửa FR-003/S-US1.8 và ghi vi phạm có chủ đích vào Complexity Tracking (constitution.md:110). |
| R2 | MAJOR | **Listener tin `event.outcome` nên sai khi event tới sai thứ tự hoặc được phát lại.** `@ApplicationModuleListener` chạy bất đồng bộ, không đảm bảo thứ tự, và publication lỗi được phát lại sau restart. Ca 1: `COMPLETED` cũ tới sau khi đã sửa thành `NO_SHOW` → tạo phiếu cho buổi không diễn ra, GV bị nhắc oan. Ca 2: `NO_SHOW` cũ tới sau khi đã sửa về `COMPLETED` → xóa phiếu `PENDING` hợp lệ, buổi COMPLETED không còn phiếu và không event nào tạo lại. Trái NFR-REL-05, NFR-REL-11. Suy ra từ code, chưa tái hiện; IT chỉ phát lại đúng kết quả hiện tại. | `SessionFinishedListener.java:32-48` | Lấy trạng thái thật: `sessions.find(event.sessionId())`; không còn COMPLETED thì `deletePendingBySession`, còn COMPLETED thì tạo nếu chưa có. Khi đó xử lý độc lập thứ tự. Thêm 2 IT phát event cũ ứng với 2 ca trên. Thêm: hai lượt chen nhau ở `existsBy` rồi `save` gây lỗi unique, publication treo tới restart; bắt `DataIntegrityViolationException` để kết thúc êm. |
| R3 | MAJOR | **FE không biết bài tập thuộc buổi nào.** `AssignmentDto` không có `sessionId`, `sessionReportId`; `ReportDto` không có bài tập, `GET /assignments` không lọc theo buổi. Vì vậy không làm được "thấy nhận xét và bài tập của từng buổi" (S-US4.1 AC5, S-US4.3 AC1). IT chỉ đếm tổng số bài. | `AssignmentController.java:42-50`; `SessionReportController.java:39-52`; `SessionReportFlowIT.java:140-145`; `rest-api.md:103` | Thêm `sessionReportId` vào `AssignmentDto` (`Assignment.getSessionReportId()` đã có), FE ghép với `ReportDto.id`. Cập nhật rest-api.md:103, IT kiểm giá trị. Báo #3 trước khi làm T101–T103. |
| R4 | MINOR | **Hai màn hình có thể lệch % hoàn thành.** `ProgressService` làm tròn xuống, `LearningPath.completionPercent` làm tròn gần nhất: 2/3 chủ đề là 66 ở tiến bộ, 67 ở lộ trình. Smoke "khớp" chỉ vì mẫu 1/10, IT mẫu 1/2. | `ProgressService.java:55-58`; `LearningPath.java:131-142` | `topics.isEmpty() ? null : path.completionPercent()`, bỏ phép tính riêng; thêm IT 2/3. |
| R5 | MINOR, cần chốt | **Phiếu treo sau đổi GV khó hoặc không nộp được.** (a) GV cũ được nộp nhưng không đọc được lộ trình (404), nên form không có chủ đề để chọn; nếu giữ id chủ đề cũ thì vẫn đổi được chủ đề `DONE` trên lộ trình của HV đã chuyển đi. (b) GV nghỉ việc là lý do chính của đổi GV (S-US1.8); GV cũ INACTIVE không đăng nhập được, admin → 403, GV mới → 404: phiếu `PENDING` treo vĩnh viễn, vẫn nhắc tới tài khoản đã ngừng. Không mất dữ liệu đã có. | `SessionReportService.java:88-90`; `LearningPathService.java:43`; `SessionReportController.java:139` | Người dùng chọn: giữ và ghi vào hợp đồng, hoặc khi GV của buổi không còn phụ trách thì cho GV đang phụ trách nộp thay (sửa rest-api.md:102). Nếu vẫn cho GV cũ nộp: `ReportDto` của phiếu `PENDING` trả kèm danh sách chủ đề, hoặc bỏ qua chủ đề với GV cũ. |
| R6 | MINOR | **Ẩn danh hóa chen nhau với giao dịch đang chạy có thể để lại dữ liệu không xóa lại được.** `anonymizeForStudent` là bulk update, không tăng `version`, nên `submit` đang chạy (đã qua `requireNotAnonymized`) vẫn ghi đè điểm, nhận xét, chủ đề lên phiếu vừa xóa. Listener xóa bài tập **trước** nên bài tập chưa commit của `submit` thoát khỏi lệnh xóa, còn `title`, `instructions` mãi mãi. Tương tự `rate` với `clearCommentsOf`. Event đã xử lý xong nên không có lần sau. Cửa sổ rất hẹp; suy ra từ code, chưa tái hiện. U7 đã xử lý đúng dạng rủi ro này cho `user_account` (data-model.md:33). | `SessionReportRepository.java:68-77`; `ReportAnonymizationListener.java:36-38`; `SessionReportService.java:91`, `:98-110`; `FeedbackService.java:52-54` | Thêm `r.version = r.version + 1` vào bulk update để `submit` chậm chân nhận 409. Đổi thứ tự listener: phiếu → đánh giá sao → bài tập, để lệnh update chờ khóa dòng rồi lệnh xóa thấy bài tập vừa commit. `rate` có thể chấp nhận và ghi lại. |
| R7 | MINOR | **Danh sách không giới hạn, trái quy ước phân trang.** `GET /assignments` của admin tải mọi bài tập kèm toàn bộ `text_answer` (tối đa 20.000 ký tự mỗi bài) rồi lọc `status` trong bộ nhớ; `GET /session-reports` của admin tải mọi phiếu từng có rồi lọc HV ẩn danh trong bộ nhớ. rest-api.md:4 có quy ước chung, ngoại lệ ở `:65` không gồm hai API này, `/sessions` có giới hạn (`:76`). Không có N+1: bài nộp và tên đều tra theo lô. Quy mô demo chưa ảnh hưởng. | `AssignmentService.java:75`, `:83-89`; `AssignmentRepository.java:15`; `SessionReportRepository.java:19-27`; `SessionReportController.java:97-113` | Tối thiểu: ghi ngoại lệ MVP vào rest-api.md:65. Nếu sửa: `page`, `size` như `/sessions`, đẩy lọc `status` xuống SQL. Bài toán lọc HV ẩn danh theo trang cần tính riêng. |
| R8 | MINOR | **Chủ đề `DONE` có thể mất khi sửa lộ trình đồng thời.** `Topic` không có `@Version`, `markDone` không tăng version của `LearningPath`. `LearningPathService.update` đang giữ bản cũ mà có đổi tiêu đề hoặc thứ tự sẽ ghi cả dòng, đưa `status` về `PLANNED`, lệch % hoàn thành. Suy ra từ code, chưa tái hiện. | `Topic.java:37`, `:56-58`; `SessionReportService.java:152`; `LearningPathService.java:86` | Thêm `@DynamicUpdate` cho `Topic` để chỉ ghi cột đổi; thêm IT nếu tái hiện được. |
| R9 | MINOR | **`taught_topic_ids` có thể thiếu chủ đề đã hoàn thành và có trùng lặp.** Chủ đề chỉ nằm trong `completedTopicIds` thành `DONE` nhưng không được ghi trong phiếu; id lặp được lưu nguyên. Dữ liệu có cấu trúc cho FR-028 không phản ánh đủ buổi đã dạy gì. | `SessionReportService.java:98`; `SessionReport.java:112` | Lưu hợp `taught ∪ completed` đã khử trùng bằng `LinkedHashSet`, hoặc trả 400 khi `completed` không thuộc `taught`; ghi vào rest-api.md:102. |
| R10 | NOTE | Event `AssignmentSubmitted` tới sau ẩn danh hóa: chặn `@invalid` chỉ xét email người nhận, nên GV vẫn nhận thư "Học viên Học viên đã ẩn danh đã nộp bài tập" với link hỏng, cho bài đã bị xóa. | `LearningNotificationListener.java:83-85`; `DispatchService.java:67` | Thêm `if (student.anonymized()) return;`. |
| R11 | NOTE | Phiếu tạo muộn sinh ra đã quá hạn: Gián đoạn được admin tính `COUNT` sau nhiều ngày, hoặc sửa về `COMPLETED`, thì `due_at = ends_at + 24h` đã qua; GV bị nhắc ở lần job tiếp theo, `onTime=false` dù không có lỗi. Đúng data-model.md:359, và K1 đã bỏ báo cáo tỷ lệ nên chưa lệch số liệu. | `SessionFinishedListener.java:42-48` | Ghi nhận; xem lại khi làm FR-023 ở GĐ2. |
| R12 | NOTE | Nộp lại ghi đè `submitted_at`: bài nộp đúng hạn, sửa sau hạn thành `late=true`. Yêu cầu chấm không gửi phiên bản bài nên GV có thể chấm bản chưa đọc nếu HV vừa nộp lại. Đặc tả chưa nói rõ. | `Submission.java:51-58`; `AssignmentController.java:78` | Xác nhận ý định, ghi vào hợp đồng. |
| R13 | NOTE | Test phụ thuộc thời gian: `anonymizedStudentHasNoReportWorkLeft` chờ cố định 800 ms, máy CI chậm có thể đỏ giả. Kiểm tra phủ định sau `sleep(500)` có thể xanh giả khi listener chậm. | `SessionReportFlowIT.java:69`, `:77`, `:232` | Dùng `await` tới `overdue_notified_at` khác null; ca phủ định chờ qua event đánh dấu. |

Đã soát, không có phát hiện mới: job quá hạn (khóa, cờ, phát event trong transaction, nhắc tối đa 1 lần); dedupe của 4 listener thông báo; nộp phiếu một lần (`@Version`, chủ đề và bài tập cùng transaction, lỗi kiểm tra không để lại gì); chấm và nộp lại (`@Version`); đánh giá sao (mốc 7 ngày tính cả biên, bắt lỗi unique); cột và mức xóa của ẩn danh hóa khớp FR-031 và data-model.md:385-389; thông báo cho GV gắn `about_student_id` nên được xóa theo. F3, F4, F6, F8–F11 và G1–G10 của QE vẫn hiệu lực, không lặp lại.

### Thứ tự sửa đề xuất

1. Trước khi đóng U6: R1.
2. Trước khi #3 nối API: R3, F7.
3. Trước demo: F1, F2, R2, R4. F3, F4 chi phí thấp nên làm cùng đợt.
4. Cần người dùng chốt: F5, R5.

### Verdict đề xuất

**FAIL** vì R1 vi phạm trực tiếp một MUST của constitution (nguyên tắc V, FR-003, NFR-SEC-02): GV đã bị đổi vẫn đọc được điểm và nhận xét của HV. Bản sửa R1 nhỏ; sau khi sửa R1 kèm IT, đồng thời xử lý hoặc được người dùng chấp nhận các mục MAJOR (R2, R3, F1, F2, F7), có thể xét lại PASS_WITH_NOTES.

### 6.1 Re-review sau khi sửa (reviewer, đọc code)

**Giới hạn:** reviewer đọc lại code và test, **không chạy gì**. `clean verify` 98 unit + 156 IT là số do người điều phối báo, reviewer chưa tự xác minh. Reviewer cũng không thấy bằng chứng các test mới từng đỏ trước khi sửa; nhận định "bắt đúng lỗi" bên dưới là suy ra bằng cách đối chiếu test với code cũ. Toàn bộ smoke ở mục 2 chạy trên bản **trước** khi sửa (R20).

**Đính chính mục 6:** R1 ghi rằng "quyết định đã chốt" cho GV cũ nộp phiếu. Danh sách quyết định reviewer được giao không có mục này; nguồn thật chỉ là rest-api.md:102 và code. Đề xuất sửa R1 của reviewer cũng giữ quyền đọc phiếu `PENDING` mà chưa nêu rằng ngoại lệ này cần được ghi lại (R14).

#### R1: đã đóng ở các đường đọc của U6

| Đường đọc của GV đã bị đổi | Kết quả | Bằng chứng |
|----------------------------|---------|------------|
| `GET /session-reports` | Đóng. Chỉ còn phiếu `PENDING` của mình, kèm tên HV (ngoại lệ, R14) | `SessionReportService.java:83-89` |
| `GET /sessions/{id}/report` | Đóng. `SUBMITTED` qua guard → 404; `PENDING` chỉ có mốc thời gian, điểm và nhận xét còn null | `SessionReportService.java:95-100` |
| `GET /students/{id}/progress` | Đóng, qua guard | `ProgressService.java:45` |
| `GET /assignments` có và không có `studentId` | Đóng, theo phân công hiện tại | `AssignmentService.java:72-80` |
| Chủ đề trên phiếu, `GET .../learning-path` | Đọc: đóng (`taughtTopicIds` chỉ có khi đã nộp, sau đó 404; lộ trình qua guard). **Ghi: còn mở**, xem R14 | `SessionReportService.java:175`; `LearningPathService.java:43` |
| Phản hồi của `PUT` | Chỉ trả phiếu vừa nộp; nộp lại → 409 trước khi đọc nội dung | `SessionReportService.java:107-114` |
| `GET /sessions`, lịch sử kết quả (ngoài U6) | **Còn đọc được**, xem R18 | `SessionLookup.java:20`; `SessionController.java:67-82` |

GV đang phụ trách và admin không bị ảnh hưởng. GV của buổi còn phụ trách vẫn qua guard. HV đã ẩn danh: `PUT` vẫn 409 `ANONYMIZED` trước bước đọc trạng thái (`:110`).

#### F5: không làm sai bài tập hay SC-007

- **SC-007 (số buổi còn lại, công nợ; spec.md:284):** `session_voided_at` chỉ xuất hiện trong `SessionReport`, `SessionFinishedListener`, `findSubmittedByStudent` và V7. Listener không gọi `EnrollmentApi`; `scheduling`, `catalog`, `billing` không đọc cột này. Không có đường ảnh hưởng tới bộ đếm hay tiền.
- **Bài tập:** không có code nào của bài tập đọc cột này. Bài tập vẫn nộp, chấm, tính muộn và ẩn danh hóa như cũ, đúng hướng đã đề xuất. Chưa có assert khẳng định điều này (R19).
- **Ẩn danh hóa:** cột là mốc thời gian, không định danh; lọc `comment is not null` chỉ loại phiếu đã xóa nội dung vì khi nộp `comment` là bắt buộc (`SessionReportService.java:139`).
- **Sửa đi sửa lại, phát lại event:** listener đối chiếu trạng thái thật, nên kết quả cuối không phụ thuộc thứ tự. Phát lại chỉ làm đổi mốc thời gian `voided`, không đổi kết quả.
- **Còn lại:** xem R15.

#### Trạng thái từng mục đã sửa

| # | Kết luận | Code | Test bắt đúng lỗi? |
|---|----------|------|--------------------|
| R1 | Đóng trong U6 | Như bảng trên | Có: `reassignedTeacherKeepsOnlyPendingDuty` sẽ đỏ với code cũ ở cả `get` và `list`. Thiếu assert `/progress`, `/assignments` cho GV cũ, thiếu ca nhiều ghi danh (R19). |
| R2 | Đóng | `SessionFinishedListener.java:44-67` | Có, cả 2 ca. Phần "0 phiếu" dựa `sleep(800)`, listener chậm thì xanh giả (R19). Tồn đọng: R17. |
| R3 | Đóng | `sessionId` trong DTO, V7, hợp đồng | Có, kiểm giá trị. |
| R4 | Đóng ở code | `ProgressService.java:49-59` | **Không**: IT vẫn mẫu 1/2 = 50, công thức cũ cũng ra 50 (R19). |
| R9 | Đóng ở code | `SessionReportService.java:118-120` | Có. Quy tắc hợp chưa ghi vào rest-api.md:102. |
| F1 | Đóng | `AssignmentService.java:150-155`; dedupe key có `teacherId`; truy vấn `distinct` | Có ở mức event và listener (`containsExactly(newTeacher)`). Thư thật chưa kiểm lại (R20). |
| F2 | Đóng ở code | `LearningDemoSeed.java:67-70`; seed trước đó đã `return` sớm | Một phần: unit test mock đúng nhánh lỗi; khởi động thật với `demo-seed` chưa chạy lại (R20). |
| F3 | Đóng | `AssignmentService.java:140-147` | Có: 6 luồng song song, chỉ 200 hoặc 409, 1 dòng. Có tính chất xác suất. |
| F4 | Đóng | `SessionReportController.java:152-153` | Có. Cùng nhóm lỗi còn R16. |
| F5 | Đóng theo phương án đề xuất | Như trên | Có: vào, ra, khôi phục biểu đồ. Tồn đọng: R15. |
| F6 | Đóng | `SessionReportRepository.java:38` | Có: `AnonymizationIT.java:389`. |
| F7 | Đóng | `FeedbackController.java:62-66`; `learning` không tạo phụ thuộc ngược | Có: của mình, HV khác rỗng, GV 403. Không sắp xếp, không ảnh hưởng việc ẩn lời mời. |

**Hồi quy:** không tìm thấy hồi quy ở đường chính. Đã soát: danh sách admin; đường `COUNT`, `MAKE_UP`, `INTERRUPTED` với listener mới; phát lại; job quá hạn; phiếu treo của HV ẩn danh; đường dẫn của `FeedbackController` sau khi đổi `@RequestMapping` (giữ nguyên). Migration V7 sửa tại chỗ không vi phạm constitution.md:74 vì file chưa merge (git status lúc bắt đầu review: untracked).

#### Findings mới

| # | Mức | Mô tả và lý do | File:dòng | Đề xuất |
|---|-----|----------------|-----------|---------|
| R14 | **MAJOR**, cần người dùng xác nhận | **Ngoại lệ cho GV đã bị đổi chưa được ghi đúng nơi.** GV cũ vẫn thấy tên HV, đọc phiếu `PENDING`, nộp phiếu, và qua `completedTopicIds` đổi chủ đề `DONE` trên lộ trình của HV đã chuyển đi, làm đổi % hoàn thành GV mới thấy. Nội dung phiếu thì hợp lý vì đây là buổi họ dạy, nhưng đây vẫn là ngoại lệ so với constitution.md:70. constitution.md:110 yêu cầu ghi vi phạm có chủ đích vào Complexity Tracking, trong khi plan.md:175 ghi "Không có vi phạm". Hợp đồng mô tả phần đọc, nộp (rest-api.md:100-102), không nói việc ghi chủ đề. | `SessionReportService.java:95-97`, `:107`, `:175`; `plan.md:173-175` | (1) Người dùng xác nhận ngoại lệ, thêm một dòng vào Complexity Tracking. (2) Khi GV không còn phụ trách (`isAssigned` false): trả 400 nếu gửi `completedTopicIds`, hoặc ít nhất ghi quyền này vào hợp đồng. Thêm IT. Phần còn lại của R5 giữ nguyên để chốt. |
| R15 | MINOR | **Phiếu đã `voided` không có dấu hiệu nào ở API.** `ReportDto`, `ReportListItem` không có cờ, nên HV vẫn đọc nhận xét, điểm của buổi no-show như phiếu bình thường. Chủ đề `DONE` vẫn tính vào `pathCompletion` trong khi điểm đã ra khỏi biểu đồ. rest-api.md:108 chưa nói phiếu `voided` bị loại. | `SessionReportController.java:39-52`; `SessionReport.java:92-93` | Thêm `sessionVoided` vào `ReportDto`; ghi rõ vào rest-api.md:101, :108 rằng chủ đề và bài tập không hoàn tác. |
| R16 | MINOR | **`taughtTopicIds:[null]` khi HV chưa có lộ trình có khả năng trả 500.** `Map.of().keySet().containsAll` với phần tử null gọi `containsKey(null)` và ném NPE; có lộ trình thì `HashMap` trả 400 như QE đã thấy. Không ghi dữ liệu. Suy ra từ code, chưa chạy. Không phải hồi quy. | `SessionReportService.java:170-171` | Từ chối phần tử null trong `validate`; thêm IT. |
| R17 | NOTE | Listener mới còn hai ca publication treo tới lần restart: `reports.save` chen nhau gây lỗi unique (đề xuất ở R2 chưa làm), và `delete` bản cũ khi GV vừa nộp gây xung đột version. Restart sẽ tự đúng lại. Ngoài ra `deletePendingBySession` và `SessionReportRepository.existsBySessionId` nay không còn nơi gọi. | `SessionFinishedListener.java:51`, `:60`; `SessionReportRepository.java:14`, `:44-51` | Bắt `DataIntegrityViolationException` ở `save`; xóa code thừa. |
| R18 | NOTE, cần chốt | Ngoài U6: GV cũ vẫn đọc buổi đã dạy qua `GET /sessions` (`studentId`, giờ, trạng thái, `cancelReason`) và lịch sử kết quả, gồm `note` tự do. Không có điểm, nhận xét; có thể chính đáng vì là lịch dạy của GV. Theo câu chữ S-US1.8 AC2 thì chưa rõ có được phép. | `SessionLookup.java:20`; `SessionController.java:67-82`, `:208-212` | Chốt phạm vi ở T140; nếu giữ, ghi chung với R14. |
| R19 | NOTE | Lỗ hổng test: R4 chưa có mẫu 2/3; R1 chưa kiểm `/progress`, `/assignments` với GV cũ và chưa assert 200 trước khi lọc JSON (`SessionReportFlowIT.java:219`); F5 chưa kiểm phiếu còn đọc được, bài tập còn nộp được; thêm `sleep(800)` mới ở `:233`, cùng dạng R13. | `SessionReportFlowIT.java:219`, `:233`, `:389` | Bổ sung assert; dùng `await` hoặc chờ publication hoàn tất. |
| R20 | NOTE | Smoke chưa chạy lại trên bản đã sửa, gồm V7 đã đổi. CSDL local đã áp V7 cũ sẽ lỗi checksum Flyway, phải tạo lại. | Mục 2; `V7__learning_reports.sql:21-22`, `:35` | Trước merge chạy lại: nâng cấp từ 32d1a6a, bước 23 `pg_dump`, thư F1 sau đổi GV, khởi động lại `demo-seed` sau ẩn danh (F2). |

#### Mục còn lại nên chuyển đi đâu

Đây là đề xuất; việc hoãn cần người dùng xem và chấp nhận.

| Mục | Chuyển sang | Lý do |
|-----|-------------|-------|
| R14 | **Trước merge U6**, người dùng xác nhận | Một MUST về quy trình của constitution. |
| R5 (GV nghỉ việc, form thiếu chủ đề) | Người dùng chốt **trước khi #3 nối API** | Ảnh hưởng thiết kế form. Tạm thời ghi vào rest-api.md:102. |
| R15, R16, R17 | B7, hoặc cùng đợt R14 nếu tiện | Nhỏ, không chặn. |
| R19, R20 | R20 trước merge; R19 B7 | R20 là bằng chứng chạy thật của chính các bản sửa. |
| R6 | B7; **muộn nhất trước khi dùng dữ liệu thật** | FR-031 là MUST, nhưng cửa sổ rất hẹp ở quy mô demo. |
| R18, F11, G6 | T140 | Cùng đợt phân quyền và giới hạn đầu vào. |
| R7 | Ghi ngoại lệ vào rest-api.md:65 ở B7; phân trang sau demo | Quy mô demo chưa cần. |
| R8, R10, R13, G1, G2, G4, G5, G7–G10 | B7 | Sửa nhỏ và thêm test; G4, G5, G9, G10 đã có test một phần qua F1, F3, F4, F2. |
| F8 | rest-api.md:43, báo #4 (`PersonalDataSummaryPage`) | Chưa sửa: trang tóm tắt FR-031 vẫn thiếu đánh giá sao. |
| F9, R12 | Người dùng xác nhận ý định, ghi hợp đồng | Không cần code nếu chấp nhận. |
| F10 | B7 nếu muốn biểu đồ có sẵn dữ liệu khi demo | Không phải lỗi. |
| R11 | GĐ2 cùng FR-023 | K1 đã bỏ báo cáo tỷ lệ. |
| G3 | Như R20 | Testcontainers chỉ cài mới. |

#### Verdict đề xuất sau re-review

**PASS_WITH_NOTES, có điều kiện:** R1 và các mục MAJOR đã sửa đều đóng ở mức đọc code và không thấy hồi quy, nhưng trước khi merge cần người dùng xác nhận ngoại lệ R14 trong Complexity Tracking và chạy lại smoke R20; nếu không xác nhận R14 thì giữ FAIL.

## 7. Kết quả cuối (orchestrator, 2026-11-04)

- **Bản cuối:** `./mvnw clean verify` xanh **98 unit + 159 IT**, `spotless:check` PASS; smoke bản cuối (R20, F2 và các luồng U6) đạt, xem build-test-all.md.
- **Đã sửa sau review:** R1, R2, R3, R4, R9, R14 (phần thu hẹp), R16, F1–F7, dọn code thừa R17.
- **Chưa đóng, cần leader:** ngoại lệ constitution V trong plan.md (Complexity Tracking) đang ở trạng thái chờ xác nhận; R5 (phiếu treo khi giáo viên nghỉ việc); R18/F3 build-test-all (giáo viên cũ vẫn đọc `GET /sessions`, chưa nằm trong ngoại lệ).
- **Chuyển tiếp:** T140: R18, F11, G6. B7: R6, R8, R10, R13, R15, R17, R19, F10, các G còn lại. FE: T101–T103, T122 (có `GET /session-feedback/mine`, `sessionId` trên bài tập), F8 cho T131.
- **Verdict: PASS_WITH_NOTES, có điều kiện leader xác nhận ngoại lệ trên; nếu không xác nhận thì phải đổi thiết kế phiếu treo.**
