# Build & Test Report — U8 — Nền tảng đào tạo tiếng Anh 1 kèm 1 cá nhân hóa (001-personalized-english-tutoring)

- **Ngày (UTC)**: 2026-11-03, 15:18–15:39 UTC (giờ máy: 22:18–22:39 +07)
- **Unit**: U8, gia hạn, công nợ, báo cáo (B5), **chỉ phần BACKEND**. FE T115, T116, T123 (#4) ghi NOT RUN.
- **Tasks trong phạm vi**: T106, T112, T113, T118, T121, cùng phạm vi chuyển tiếp đã duyệt: T125 với báo cáo thật, R8, N2 (U7); R5, R6, R7, F3 (U2); R6, R8 (U3); F1 (U5) — hoàn thành: **5/5 task BE** là `[X]` (tasks.md:259, 268, 269, 287, 293). T115, T116, T123 vẫn `[ ]` (tasks.md:271, 272, 295).
  - Đã hoãn, không kiểm trong lượt này: U5 R5, R7; U4 R6. Test bổ sung khác chuyển T140/B7.
  - Thay đổi chưa commit: `git status` có 37 mục (26 modified, 11 untracked) trước và sau lượt chạy; QE chỉ thêm file báo cáo này. Không sửa migration cũ, chỉ thêm `V202611031700__enrollment_cancel_audit.sql`.
- **Môi trường**: Windows 11, Git Bash; JDK 21.0.12 (`JAVA_HOME=/c/Program Files/Java/jdk-21.0.12`); Maven 3.10.0 qua wrapper; Docker Server 29.8.2; Testcontainers `postgres:16`.

## 1. Build

| Thành phần | Lệnh | Kết quả | Ghi chú |
|------------|------|---------|---------|
| Backend (Maven, gồm Enforcer) | `cd backend && ./mvnw -B clean verify` | **PASS** (`BUILD SUCCESS`, exit 0, 03:12 min, xong lúc 22:21:39 +07) | `RequireJavaVersion passed`, `BannedDependencies passed`. Log 822 dòng, 0 dòng `[ERROR]`, 2 dòng log ứng dụng mức ERROR (xem mục dưới và F7). |
| Backend format (không cache) | Xóa `backend/target/spotless-index`, rồi `./mvnw -B spotless:check` | **PASS** (`BUILD SUCCESS`, exit 0, 5.451 s) | `keeping 217 files clean - 0 needs changes`, 217 file được kiểm thật, 0 bỏ qua do cache. |
| Frontend | — | **NOT RUN** | T115, T116, T123 thuộc #4, ngoài phạm vi BE. |

**Hai dòng ERROR trong log `verify` (test vẫn pass):**
- `verify.log:455`: `IdentityNotificationListener.on(UserInvited)`, `IllegalStateException: listener sập`. Đây là lỗi `EventRedeliveryIT` cố ý tạo (`EventRedeliveryIT.java:43-46`).
- `verify.log:705`: `SystemCancelListener.on(StudentAnonymized)`, `OptimisticLockException`. Xem F7.

Các WARN còn lại quan sát được: `DispatchService ... Gửi email ... lỗi` (chỉ in tên lớp exception), `outOfOrder mode`, SpringDoc, reuse container, `HV000271`, một lỗi khởi tạo context ngay trước khi `SecurityPropertiesTest` báo pass (dòng 202-203).

## 2. Test

| Bộ test | Lệnh | Pass / Fail / Skip | Ghi chú |
|---------|------|--------------------|---------|
| Unit + Scenario (surefire) | `./mvnw -B clean verify` | **95 / 0 / 0** | Khớp số người triển khai báo. `EnrollmentTest` 13 (đã đổi theo chữ ký `cancel` mới). |
| Integration Testcontainers (failsafe) | `./mvnw -B clean verify` | **125 / 0 / 0** (`failsafe-summary.xml`: completed 125, errors 0, failures 0, skipped 0, flakes 0) | Khớp số người triển khai báo. Mới ở U8: `EnrollmentLifecycleJobIT` 7, `BillingReportIT` 6, `TeacherSessionsReportIT` 3, `SchedulingHardeningIT` 2, `EventRedeliveryIT` 1. `AnonymizationIT` 15. Các bộ khác đều pass: `AuthFlowIT` 22, `CatalogFlowIT` 9, `EnrollmentApiIT` 8, `GuardianConsentIT` 7, `StudentAccessIT` 6, `CancellationPolicyIT` 5, `LearningAccessIT` 5, `NotificationDispatchIT` 5, `SchedulingNotificationIT` 5, `BookingConcurrencyIT` 4, `SchedulingJobsIT` 4, `SessionOutcomeIT` 4, `NotificationApiIT` 3, `SystemCancelIT` 3, `LogPrivacyIT` 1. |
| Ranh giới module | `ModularityTests` (trong `verify`) | **2 / 0 / 0** | `ApplicationModules.verify()` pass với gói mới `catalog.internal.jobs`, `scheduling.internal.report`. |
| Test đồng thời, lặp 3 lần | `./mvnw -B verify -Dtest=NoSuch -Dsurefire.failIfNoSpecifiedTests=false "-Dit.test=SchedulingHardeningIT#bookingAndDayOffNeverBothSucceed,BillingReportIT#concurrentFirstPaymentsKeepTotalsAndOneFirst"` | Lần 1: **2 / 0 / 0**. Lần 2: **2 / 0 / 0**. Lần 3: **2 / 0 / 0** | `BUILD SUCCESS` cả 3 lần (36.3 s, 39.3 s, 39.0 s). Cộng lượt `verify` là 4 lần xanh. `bookingAndDayOffNeverBothSucceed` có 10 vòng mỗi lần. Không thấy thất thường trong mẫu này; mẫu nhỏ nên không chứng minh được là không có. QE không tự xác nhận test đỏ trước khi sửa (audit.md:89 do người triển khai ghi). |
| Quét log của `verify` | Tìm trong log 822 dòng | **Không thấy rò rỉ** (quan sát, không phải test) | 0 khớp với địa chỉ email, `password`, `$2a$`, `bearer `, `eyJ`, `inviteToken`, `refreshToken`, `accessToken`. |
| Smoke quickstart bước 13 (BE) | Jar thật, Postgres và Mailpit thật | **PASS** | Xem chi tiết bên dưới. |
| Smoke báo cáo (bước 3, 12, 23 phần BE) | Như trên | **PASS, có ghi chú (F1, F2)** | Xem chi tiết bên dưới. |
| Job chạy thật, cron/zone | Như trên, thêm `-Duser.timezone=UTC` | **PASS** | Xem chi tiết bên dưới. Job lúc đúng 00:05 giờ VN: **NOT RUN** (không chờ tới giờ đó). |
| FE T115 (`ReceivablesPage`) | — | **NOT RUN** | Thuộc #4. |
| FE T116 (`MyPackagesPage`) | — | **NOT RUN** | Thuộc #4. |
| FE T123 (`DashboardPage`) | — | **NOT RUN** | Thuộc #4. Vì vậy bước 12 chỉ kiểm được API, chưa kiểm dashboard. |
| CI GitHub Actions trên PR thật | `.github/workflows/ci.yml` | **NOT RUN** | Thay đổi U8 chưa commit, chưa có PR. |
| gitleaks | — | **NOT RUN** | Không có trong danh sách lệnh của lượt này. |

### Chi tiết smoke local

**Cấu hình:** `docker compose -p u8smoke up -d --wait postgres mailpit`; jar từ `verify`, `VIDEO_PROVIDER=fake`, `--spring.profiles.active=local,demo-seed --server.port=18080`. LiveKit không chạy vì các bước này không cần. Để có trạng thái cần kiểm, QE dùng SQL đổi `expires_at` và giờ của một buổi trên CSDL smoke; riêng ở lần kiểm F2, QE giữ khóa ShedLock của job để job không chen vào.

**Dọn dẹp:** Đã dừng app và chạy `docker compose -p u8smoke down -v`; không còn container, volume, network `u8smoke`, port 18080 đã trống, không đụng volume của project mặc định.

| Nội dung | Thao tác | Quan sát |
|----------|----------|----------|
| Khởi động, cron mặc định | Lần 1: CSDL trống, cấu hình mặc định, chỉ mở thêm actuator `scheduledtasks` bằng tham số dòng lệnh | Flyway áp dụng 13 migration, gồm `202611031700`. App khởi động trong 14.8 s, 0 dòng ERROR. `scheduledtasks` có `0 5 0 * * *` → `EnrollmentLifecycleJob.run`. |
| Zone của cron | Lần 2: `-Duser.timezone=UTC`, cron `*/20 * 22,23 * * *` | Job chạy lúc 15:30:20Z (bảng `shedlock`). Giờ UTC khi đó là 15, chỉ khớp 22 nếu tính theo `Asia/Ho_Chi_Minh`, nên zone được áp dụng đúng. Log có 0 dòng ERROR. |
| Bước 13 | Gói 2 buổi, thanh toán 500000 | 201, công nợ 500000. Sau khi job chạy: `renewal_low_notified_at` có giá trị, trạng thái vẫn `ACTIVE`. Mailpit: "Nhắc gia hạn gói học" tới HV, "Học viên Bùi Gia Huy cần gia hạn gói học" tới admin. Thông báo trong app `RENEWAL_DUE` có. 2 dòng dispatch `SENT`. |
| Nhắc 1 lần | Chờ 45 s (job chạy thêm nhiều lần) | Dispatch 2 → 2, mail HV 2 → 2, thời điểm đã nhắc không đổi. |
| Nhắc sắp hết hạn | `expires_at` còn 3 ngày | Cờ `EXPIRING` có, vẫn `ACTIVE`. Tổng cuối: dispatch `EXPIRING` 2 `SENT`, `LOW_SESSIONS` 2 `SENT`. |
| Hết hạn | `expires_at` đã qua, chờ job | `EXPIRED`, `sessions_reserved`=1, buổi vẫn `BOOKED`. `POST /sessions` → 409 `ENROLLMENT_NOT_ACTIVE`. |
| Quá hạn, job chưa chạy | `ACTIVE`, `expires_at` đã qua | Vẫn chặn được, nhưng `POST /sessions` → **400 `VALIDATION_FAILED`** "Giờ học không nằm trong khung giờ trống của giáo viên"; `slots` trả 0 khung giờ (F2). |
| Gia hạn | `POST /extend` | GV → 403. Hạn trong quá khứ → 400. Hợp lệ → 200 `ACTIVE`; `extended_by` là admin, `extended_at` có. Đặt lại → 201. Job chạy sau đó vẫn `ACTIVE`. |
| Buổi cũ của gói hết hạn | Đưa buổi về quá khứ, gói `EXPIRED`, GV chốt `COMPLETED` | 200; bộ đếm `EXPIRED\|2\|0` → `EXPIRED\|1\|1` (trạng thái, đã đặt, đã dùng). Báo cáo GV tăng từ 4 lên 5. |
| Hủy ghi danh | Thanh toán 400000 rồi `POST /cancel` | GV → 403. Admin → 200 `CANCELLED`, lý do đã trim, `cancelledBy` là admin, có `cancelledAt`. Công nợ tổng 22200000 → 21600000, đã thu không đổi 1200000. `hasDebt=true` không còn dòng này. Danh sách đầy đủ giữ `debt=600000, cancelled=true, paid=400000`. HV và GV đọc ghi danh: 3 trường hủy đều null. Thu thêm → 409. Điều chỉnh −100000 → 201. Hủy lần 2 → 409. |
| Khớp dữ liệu gốc | So API với SQL ở 5 thời điểm, số liệu tăng dần | `collectedVnd` = `SUM(payment.amount_vnd)`: 0, 800000, 1100000, 1800000, 1800000. `debtVnd` = tổng nợ dương của ghi danh chưa hủy = tổng `/receivables?hasDebt=true`: 20400000, 21600000, 21600000, 20900000, 20900000. Số receivable lệch `SUM(payment)`: 0 ở cả 5 lần. `sessionsTaught` khớp `COUNT` theo 3 trạng thái. |
| Kỳ báo cáo | Thanh toán 123000 ngày 2025-01-10 | Kỳ đúng ngày đó → 123000 (khớp SQL); ngày trước đó → 0; công nợ như nhau ở cả hai kỳ. |
| Quyền và tham số | 3 endpoint báo cáo | GV và HV → 403 cả 3; không token → 401. `from > to` → 400. `from=abc`, `to=abc`, `hasDebt=abc` → 400 `VALIDATION_FAILED`, không phải 500. Chỉ có `from` hoặc chỉ có `to` → 200. |
| Ẩn danh hóa (SC-007) | `student1`, có thanh toán 700000 | Trước = sau: `/reports/revenue`, `/receivables` (bỏ cột tên), `/reports/teacher-sessions`. Tên đổi thành "Học viên đã ẩn danh". `pg_dump --data-only`: 0 dòng chứa tên, 0 dòng chứa email. Mailpit có 0 thư tới `@invalid`. Đăng nhập lại → 401. |
| R8, N2 trên app thật | Sau ẩn danh hóa | Danh sách của GV không còn HV; cờ tư vấn không có; admin vẫn thấy. Job: dispatch 0 → 0, không đặt cờ nhắc, vẫn chuyển `EXPIRED` khi quá hạn. Đổi GV → 409 `ANONYMIZED`. **`POST /extend` → 200 `ACTIVE` (F1).** GV đọc thẳng hồ sơ → 200, các trường đều null (F8). Bỏ qua HV ẩn danh trong guard ngừng GV chỉ được IT kiểm; smoke chỉ xác nhận GV còn HV thật → 409 `TEACHER_HAS_STUDENTS`. |
| `GET /sessions` | Phân trang | Mặc định 15 dòng (đủ 15 buổi trong CSDL); `size=2` → 2; `size=2&page=1` → 2; `size=501`, `size=0`, `page=-1`, `size=abc` → 400. `outcomePending=true&size=1` → 1, nhưng CSDL chỉ có 1 buổi thỏa nên không kiểm được giới hạn (F4). |
| Log và event | `app1.log`, `app2.log` | 0 dòng ERROR. `app2.log`: 0 khớp email demo, `eyJ`, `$2a$`. `event_publication` chưa xong: 0. |

### Đối chiếu DoD riêng của U8 (unit-of-work.md:400-404)

| Mục DoD | Trạng thái | Test và kết quả quan sát |
|---------|-----------|--------------------------|
| Hết hạn gói với `TestClock`: `EXPIRED`, BOOKED giữ, gia hạn → `ACTIVE`, đặt lại được | **Đạt** | `EnrollmentLifecycleJobIT.expiredEnrollmentKeepsBookedSessionsAndExtendReactivates` pass (`:98-134`). Smoke khớp. |
| Nhắc gia hạn đúng 1 lần mỗi lý do | **Đạt; thiếu test giá trị biên và luồng tới email** | `lowSessionsReminderFiresOnce`, `expiringReminderFiresOnceEvenAfterExtend`, `eachReasonRemindsSeparately`, `exhaustedEnrollmentStillGetsLowSessionsReminder` pass. Test chỉ đếm event. Xem G1, G2. |
| Doanh thu/công nợ = tổng payment/receivable (NFR-REL-03, quickstart.md:63) | **Đạt; test chưa so trực tiếp với SQL** | `collectedFollowsPaidAtRangeAndAdjustments`, `debtIsCurrentBalanceRegardlessOfRange`, `overpaymentDoesNotOffsetOtherDebt`, `cancelledEnrollmentLeavesDebtTotalsButKeepsRecords` pass, nhưng so bằng số viết tay và chênh lệch trước/sau (F5). Smoke đã so SQL ở 5 thời điểm, đều khớp. |
| Số buổi dạy đúng định nghĩa FR-027 | **Đạt; test đặt trạng thái bằng SQL** | `TeacherSessionsReportIT.countsOnlyTaughtStatusesPerTeacher` pass, đủ 8 trạng thái, tách theo GV; `rangeUsesVietnamDatesInclusive` pass. Trạng thái được đặt bằng `UPDATE`, nên chưa có test đi từ luồng chốt thật hoặc "Gián đoạn tính là đã học" tới báo cáo (G3). Smoke có chốt thật `COMPLETED`, báo cáo tăng đúng. |
| Endpoint báo cáo chỉ admin | **Đạt** | `onlyAdminReadsFinancialReportsAndRangeIsValidated`, `onlyAdminReadsReportAndRangeIsValidated` pass (GV, HV → 403). Smoke khớp, thêm 401 khi không token. |
| Từ U7: `AnonymizationIT` chụp 3 báo cáo trước/sau | **Đạt** | `anonymizationRemovesIdentityAndKeepsFigures` pass; `Figures` có đủ 3 báo cáo (`AnonymizationIT.java:79-108`); so bằng nhau ở `:330` và sau khi phát lại event ở `:351`. Smoke khớp. |

### DoD và yêu cầu chưa có test tự động

| # | Nội dung | Hiện có |
|---|----------|---------|
| G1 | Giá trị biên của nhắc: đúng 3 buổi (không nhắc), đúng 7 ngày (có nhắc). Test chỉ dùng 2 và 6 buổi; 8 và 6 ngày. | Không có test. Smoke không kiểm biên. |
| G2 | Job → email và thông báo tới **cả** HV lẫn admin (FR-026, quickstart bước 13). | `CatalogNotificationListenerTest` kiểm listener riêng với event dựng tay; không có IT nối job với listener. Smoke đạt. |
| G3 | Luồng chốt buổi thật (kể cả `interruption-decision` COUNT) đi tới `/reports/teacher-sessions`. | `SessionOutcomeIT` kiểm chuyển trạng thái; báo cáo được kiểm bằng trạng thái đặt qua SQL. Smoke đạt riêng ca `COMPLETED`; ca COUNT **NOT RUN**. |
| G4 | Cron và zone của job khi chạy thật. | Test tắt cron (`application-test.yml:11`) và gọi `job.run()` trực tiếp. Smoke đạt, trừ việc chờ đúng 00:05. |
| G5 | `extended_by`, `extended_at` qua IT. | `EnrollmentLifecycleJobIT` chỉ kiểm `status`; `src/test` không có assert nào cho 2 giá trị này. Smoke đạt. |
| G6 | GV và HV không đọc được `cancelReason`, `cancelledBy`, `cancelledAt`; GV không gọi được `extend`, `cancel`. | Không có test. Smoke đạt. |
| G7 | Chốt hoặc hủy buổi `BOOKED` khi gói đã `EXPIRED`. | Test chỉ kiểm buổi còn `BOOKED`. Smoke đạt ca chốt `COMPLETED`; ca hủy **NOT RUN**. |
| G8 | Nhánh xung đột version trong job (`EnrollmentLifecycleJob.java:44-48`): một ghi danh lỗi không làm hỏng các ghi danh khác. | Không có test, smoke không kiểm. |
| G9 | `extend` với HV đã ẩn danh. | Không có test (F1). |

Các mục G1–G8 không có mục nào làm sai kết quả quan sát được; theo phạm vi đã duyệt, phần test bổ sung chuyển T140/B7.

### Đối chiếu task

| Task | Trạng thái | Test thực có |
|------|-----------|--------------|
| T106 | **Đạt** | `EnrollmentLifecycleJobIT` 7/7. Tên đổi đuôi `IT` đã ghi ở tasks.md:259. |
| T112 | **Đạt** | Như T106; cron mặc định và zone đã xác nhận bằng smoke. |
| T113 | **Đạt** | `BillingReportIT` 6/6. |
| T118 | **Đạt** | `TeacherSessionsReportIT` 3/3; đủ các trạng thái task yêu cầu, thêm `BOOKED`. |
| T121 | **Đạt** | Như T118. Chỉ trả `sessionsTaught` theo K1, rest-api.md đã cập nhật. |
| T115, T116, T123 | **NOT RUN** | FE của #4. |

### Đối chiếu phạm vi chuyển tiếp

| Mục | Trạng thái | Bằng chứng |
|-----|-----------|-----------|
| T125 với báo cáo thật | **Đạt** | `AnonymizationIT.java:89-108,251-257,330,351`. "Bài nộp, nhận xét" vẫn chờ U6, lộ trình trong test chính vẫn theo F6 của U7 (chuyển U6). |
| R8 (U7) | **Đạt** | `anonymizedStudentNoLongerAffectsTeacherWork`, `anonymizedStudentIsNotRemindedButStillExpires` pass. Smoke khớp. |
| N2 (U7) | **Đạt một phần** | Guard đã bỏ qua HV ẩn danh. Riêng việc xử lý `extend` mà build-test-U7.md:264 chuyển sang U8 thì chưa có (F1). |
| R5 (U2) | **Đạt** | `BillingReportIT.java:174-186`; `PaymentService.java` chỉ chặn `record`. Smoke khớp. |
| R6 (U2) | **Đạt một phần** | IT 2 thanh toán + 1 điều chỉnh và IT đồng thời pass, nhưng thiếu so `SUM(payment)` bằng SQL như đề xuất gốc (F5). |
| R7 (U2) | **Đạt** | `BillingReportIT.java:151-160`, `EnrollmentTest`, `AnonymizationIT.java:215-220` (lý do chứa tên HV, phép quét toàn CSDL không còn). Migration mới chạy được trên Testcontainers và CSDL trống. |
| F3 (U2) | **Đạt đúng đề xuất; đường HTTP trả mã khác** | `reserveIsBlockedAfterExpiryBeforeJobRuns` pass qua `EnrollmentApi` (`:137-147`). Qua HTTP là 400, không phải `ENROLLMENT_NOT_ACTIVE` (F2). |
| R6 (U3) | **Đạt** | `bookingAndDayOffNeverBothSucceed` xanh 4/4 lần, mỗi lần 10 vòng. Khóa có ở `BookingService.java:78`, `AvailabilityService`, `CancellationService`. |
| R8 (U3) | **Đạt một phần** | `sessionListIsPagedAndBounded` pass. Contract chưa cập nhật (F3); nhánh `outcomePending` chưa có giới hạn (F4). |
| F1 (U5) | **Đạt một phần** | `EventRedeliveryIT` pass: listener lỗi → publication còn → phát lại → có email, publication hết. Chưa khẳng định "đúng 1 email" như đề xuất gốc (F6). |

## 3. Review độc lập (theo constitution)

QE chỉ rà ở mức bằng chứng build/test, chưa phải code review đầy đủ.

| Nguyên tắc | Kết quả (Đạt / Vi phạm / N/A) | Bằng chứng (file:dòng) |
|------------|-------------------------------|------------------------|
| I. Modular Monolith, `ApplicationModules.verify()` | Đạt (local) | `ModularityTests` 2/0/0. Billing đọc ghi danh đã hủy qua `EnrollmentApi.findCancelledIds`; scheduling và billing đọc tên qua `UserDirectory`. |
| II. Provider-Agnostic (Ports & Adapters) | N/A | U8 không thêm provider. Email trong smoke vẫn đi qua port tới Mailpit. |
| III. Free-First & YAGNI | Đạt | Không thêm hạ tầng: dùng khóa advisory của Postgres, 3 cột mới. |
| IV. Kiểm thử trọng yếu, Postgres thật, cấm H2, CI mỗi PR | Đạt một phần | 5 lớp IT mới (19 test) trên `postgres:16` thật, toàn backend 220/0/0 (95 + 125). Còn các mục G1–G9. CI chưa chạy. |
| V. Bảo mật & dữ liệu cá nhân | Đạt ở mức quan sát | 3 endpoint báo cáo chỉ admin. Trường hủy chỉ admin thấy. `pg_dump` không còn tên và email của HV ẩn danh. F1 không ghi lại dữ liệu cá nhân, nhưng lệch contract. |
| Ràng buộc công nghệ (Java 21, Boot, Flyway) | Đạt | Enforcer pass. Migration mới chạy được trên Testcontainers và CSDL trống. Không sửa migration cũ. |
| Quy trình & DoD (CI xanh + review + demo) | Chưa đạt | Gate local xanh; chưa có PR/CI, chưa có review độc lập ghi trong báo cáo này, FE T115/T116/T123 chưa làm. |

## 4. Phát hiện

| # | Mức độ | Mô tả | File | Đề xuất |
|---|--------|-------|------|---------|
| F1 | Trung bình | **`POST /enrollments/{id}/extend` vẫn chạy với HV đã ẩn danh.** Smoke: 200 `ACTIVE`, ghi đè hạn và `extended_by/at`, kể cả khi ghi danh đã `EXPIRED` thì đưa về `ACTIVE`. Trong khi đó đổi GV trả 409 `ANONYMIZED` và contract ghi 409 cho "thao tác trên hồ sơ đã ẩn danh hóa". build-test-U7.md:264 chuyển rõ "`extend` đối với HV ẩn danh" sang U8. Không ghi dữ liệu cá nhân và không đổi 3 báo cáo, nhưng là thao tác nghiệp vụ trên người không còn tồn tại. Chưa có test. | `EnrollmentService.java:128-139`; `EnrollmentService.java:116`; `rest-api.md:151`; `build-test-U7.md:264` | Thêm `userDirectory.requireNotAnonymized(...)` vào `extend` và thêm assert vào `anonymizedStudentCannotReceiveNewData`. Nếu người dùng muốn giữ, ghi ngoại lệ vào contract. |
| F2 | Thấp | **Đặt buổi khi gói đã quá hạn nhưng job chưa chạy trả sai mã lỗi.** Smoke: `ACTIVE`, quá hạn → 400 `VALIDATION_FAILED`, `field=startsAt`, "Giờ học không nằm trong khung giờ trống của giáo viên", thay vì 409 `ENROLLMENT_NOT_ACTIVE`. `isBookable` loại khung giờ theo `expiresAt` trước khi tới `reserve`, nên kiểm tra mới ở `reserve` không được gọi từ đường này. Vẫn chặn đúng, nhưng HV đọc thông báo sai nguyên nhân, trong tối đa khoảng 24 giờ vì job chỉ chạy 00:05. Test F3 gọi thẳng `EnrollmentApi`, không đi qua HTTP. | `BookingService.java:75-82`; `SlotService.java:84-88`; `Enrollment.java:122`; `EnrollmentLifecycleJobIT.java:137-147` | Trong `BookingService`, kiểm tra quá hạn cùng chỗ kiểm `status`, trả `ENROLLMENT_NOT_ACTIVE`. Thêm ca HTTP vào `reserveIsBlockedAfterExpiryBeforeJobRuns`. |
| F3 | Thấp | **Contract chưa có `page`, `size` của `GET /sessions`.** Dòng contract chỉ có `from, to, teacherId, studentId, status`, và không nêu mặc định 200. Kết quả sắp theo giờ tăng dần, không trả tổng số, nên khi quá 200 buổi mà không truyền `from`, người gọi chỉ nhận 200 buổi **cũ nhất** và không biết bị cắt. `frontend/src` hiện chưa có mã gọi danh sách này (chỉ có 1 test `POST`); QE chưa xem PR #6/#7. | `rest-api.md:65,76`; `SessionController.java:149-150`; `SessionRepository.java:52` | Cập nhật contract, báo #2–#4. Cân nhắc trả tổng số hoặc header khi FE cần. |
| F4 | Thấp | **Nhánh `outcomePending=true` bỏ qua `page`, `size`** (đọc code). R8 của U3 nêu rủi ro không giới hạn số dòng; nhánh này không bị giới hạn. Smoke không phân biệt được vì chỉ có 1 buổi thỏa. Quy mô demo nhỏ. | `SessionController.java:158-163` | Áp dụng cùng giới hạn hoặc ghi rõ là ngoại lệ. |
| F5 | Thấp | **R6 của U2 chưa làm đủ phần so SQL.** Đề xuất gốc là so `amount_paid_vnd = SUM(payment.amount_vnd)` bằng SQL; `BillingReportIT` không dùng JDBC, chỉ so số viết tay và chênh lệch trên CSDL dùng chung. Nguy cơ thực tế thấp: smoke so SQL 5 lần đều khớp. | `BillingReportIT.java:83-110`; `build-test-U2.md:123` | Thêm 1 assert SQL vào `collectedFollowsPaidAtRangeAndAdjustments` (chuyển T140/B7 nếu chưa làm kịp). |
| F6 | Thấp | **`EventRedeliveryIT` chưa khẳng định không gửi trùng.** F1 của U5 yêu cầu "đúng 1 dispatch hoặc 1 email"; test dùng `contains(email)`, gửi 2 lần vẫn xanh. Lần lỗi của test xảy ra trước khi gửi, nên không phủ tình huống phát lại sau khi đã gửi (tình huống này gắn với U5 R5 đã hoãn). | `EventRedeliveryIT.java:73`; `build-test-U5.md:72` | Đếm chính xác 1 email và 1 dòng dispatch. |
| F7 | Thông tin | **Log `verify` có ERROR `OptimisticLockException` ở `SystemCancelListener`, test vẫn pass.** Xảy ra trong `SchedulingNotificationIT` (`verify.log:705`), trùng với ca phát `StudentAnonymized` 2 lần liền; 2 listener đua nhau cập nhật cùng buổi, một bên thất bại. Thời điểm đua có thể khác nhau giữa các lần: báo cáo U7 ghi 0 dòng ERROR, nhưng QE chỉ chạy `verify` đầy đủ 1 lần nên chưa đo tần suất. Đường production không phát 2 event song song, vì `anonymize` lần 2 trả 409; publication lỗi thì chờ tới lần restart, đúng R6 của U7 đã chuyển "trước deploy". Listener sinh ERROR này không thuộc thay đổi U8. | `SchedulingNotificationIT.java:139-140`; `SystemCancelListener.java:79,86,97-99` | Trong test, chờ lần phát đầu xong rồi mới phát lần 2, để phép thử idempotent không phụ thuộc thời điểm. |
| F8 | Thông tin | GV đang phụ trách vẫn đọc thẳng được `GET /students/{id}/profile` của HV đã ẩn danh: 200, các trường đều null. Không lộ dữ liệu, chỉ khác với danh sách đã ẩn HV này. | `ProfileService.java:125-128` (danh sách) | Không cần làm trước demo. Xem lại ở T140 nếu muốn trả 404 thống nhất. |

## 5. Verdict

**PASS_WITH_NOTES** — `clean verify` BUILD SUCCESS (95/0/0 unit, 125/0/0 IT), `spotless:check` không cache PASS, 2 test đồng thời xanh 3/3 lần lặp, smoke bước 13, báo cáo, gia hạn, hủy và cron/zone đúng trên jar và Postgres thật; không có lỗi chặn, nhưng F1 khiến phần `extend` của N2 chưa đạt và cần sửa hoặc được người dùng chấp nhận, còn các mục G1–G9, F2–F6 là ghi chú; CI, gitleaks, FE và job lúc đúng 00:05 là NOT RUN.

## 6. Review độc lập (reviewer, 2026-11-03)

**Phạm vi và cách làm:** Reviewer chỉ đọc tĩnh code, migration, test và tài liệu; **không chạy gì, không xem được `git diff`** và chỉ sửa báo cáo này. Số test và kết quả smoke lấy từ mục 1-2 của QE. Các phát hiện R1-R15 đều suy ra từ code, chưa tái hiện trên app; theo constitution IV, lỗi ở logic trọng yếu cần có test tái hiện trước khi sửa. Không tính là vi phạm các quyết định người dùng đã chốt, kể cả `payment.note` (audit.md:84).

### 6.1. Xác minh F1, F2, F7

| # | Xác minh | Mức đề xuất | Ý kiến và cách sửa |
|---|----------|-------------|--------------------|
| F1 | **Đúng.** `extend` chỉ kiểm GV còn `ACTIVE`, không kiểm HV ẩn danh (`EnrollmentService.java:128-139`); `changeTeacher` có kiểm (`:116`). Contract ghi 409 (`rest-api.md:151`), U7 chuyển rõ việc này sang U8 (`build-test-U7.md:264`). | **MAJOR** (QE: Trung bình) | Nâng mức vì đây là việc đã bàn giao và lệch contract, không vì hậu quả dữ liệu. Tác động thực tế có giới hạn: không ghi PII, báo cáo không đổi, job không nhắc, đặt buổi vẫn 409. Sửa như QE đề xuất: gọi `requireNotAnonymized` ngay sau `find`, trước kiểm GV, để mã lỗi không phụ thuộc trạng thái GV. Thêm test đỏ trước vào `anonymizedStudentCannotReceiveNewData`, khẳng định 409 và `expires_at` không đổi. Sửa cùng R1. |
| F2 | **Đúng.** `BookingService.java:75` chỉ xét `status`; `SlotGenerator.java:37-41` kẹp cửa sổ theo `expiresAt` nên `isBookable` trả false (`SlotService.java:86-90`) và `BookingService.java:79-81` ném 400. `reserve` (`Enrollment.java:122`) không được gọi tới trên đường này. | **MINOR** (giữ nguyên) | Chặn đặt lịch vẫn đúng FR-024; chỉ sai mã và thông điệp, ảnh hưởng thêm NFR-UX-01. Lưu ý khi sửa: `BookingService` chưa có `Clock`, nên cần inject `Clock` hoặc thêm hàm kiểm vào `EnrollmentView`. Chỉ trả `ENROLLMENT_NOT_ACTIVE` khi `now >= expiresAt`; giờ học nằm sau hạn nhưng gói chưa hết hạn vẫn là 400. Đường `reschedule` dùng chung `create` nên được sửa theo và vẫn rollback đúng. Thêm ca HTTP vào `reserveIsBlockedAfterExpiryBeforeJobRuns`. |
| F7 | **Đúng về nguyên nhân trong test, nhưng chưa đủ về production.** `SchedulingNotificationIT.java:139-140` phát 2 event liền nhau; 2 listener cùng đổi một buổi (`SystemCancelListener.java:93-99`) nên bên thua gặp xung đột `@Version`. Test vẫn xanh vì bên thắng đã làm đủ. | **NOTE** cho U8; phần production thuộc R6 của U7 | Sửa test như QE đề xuất. Cần sửa nhận định "production không có đường này": không cần 2 event, chỉ cần giáo vụ hoặc GV thao tác đúng buổi `BOOKED` tương lai đó cùng lúc listener chạy là có cùng xung đột. Khi đó cả transaction rollback, gồm `clearCancelReasonsOf` và `clearNotesOf` (`SystemCancelListener.java:78-82`): buổi vẫn `BOOKED`, văn bản tự do còn nguyên tới lần restart. Cửa sổ hẹp, nên giữ R6 của U7 "trước deploy", không chặn U8; có thể tách phần xóa văn bản tự do khỏi vòng hủy buổi. |

**Đính chính G5:** "`src/test` không có assert nào cho 2 giá trị này" chưa chính xác. `EnrollmentTest.java:101` có kiểm `extendedBy`; còn thiếu `extendedAt` và mức IT.

### 6.2. Phát hiện mới

| # | Mức | Mô tả và lý do | File:dòng | Đề xuất |
|---|-----|---------------|-----------|---------|
| R1 | **MAJOR** | **Lý do hủy ghi danh ghi được sau khi đã ẩn danh hóa, và sẽ tồn tại mãi.** Cột `cancel_reason` mới có ở U8. Listener chỉ xóa một lần lúc nhận event, còn `cancel` không kiểm HV ẩn danh; ghi danh `ACTIVE`/`PENDING_PAYMENT` của HV ẩn danh vẫn hủy được. Giáo vụ nhập "<tên HV> chuyển nơi học" thì tên quay lại CSDL, trái `data-model.md:180`. Hai đường cùng loại đã xử lý đúng: `CancellationService.java:68`, `OutcomeService.java:70`. Khe phụ: request `cancel` đọc entity trước khi listener chạy và commit sau đó cũng ghi lý do lại, vì bulk update không tăng `version`. `AnonymizationIT.java:215-220` chỉ phủ lý do nhập **trước** khi ẩn danh. | `EnrollmentService.java:142-146`; `Enrollment.java:238`; `AnonymizationListener.java:35`; `EnrollmentRepository.java:45-49` | Làm giống hủy buổi: vẫn cho hủy (kết toán) nhưng bỏ `reason` khi `userDirectory.isAnonymized(studentId)`. Thêm test đỏ: ẩn danh rồi `POST /cancel` có lý do → 200, `cancel_reason` null. Khe phụ dùng chung hướng xử lý với R5 của U7. |
| R2 | MINOR | **HV đã ngừng kích hoạt (chưa ẩn danh) vẫn nhận nhắc gia hạn.** Job chỉ loại HV ẩn danh; listener chỉ loại user null; `DispatchService` chỉ chặn `@invalid`. HV `INACTIVE` không đăng nhập được nên link trong email vô dụng, admin cũng bị báo thừa, và cờ đã nhắc bị dùng mất nên kích hoạt lại sẽ không được nhắc nữa. | `EnrollmentLifecycleStep.java:52`; `CatalogNotificationListener.java:111-114`; `DispatchService.java:67`; `SystemCancelListener.java:72-73` | Trong step, bỏ qua khi HV `INACTIVE` (đã bao gồm ẩn danh) và không đặt cờ. Thêm IT: ngừng HV → không nhắc; kích hoạt lại → nhắc một lần. |
| R3 | MINOR | **Ngừng được GV còn buổi `BOOKED` trên ghi danh `EXPIRED`.** Job U8 khiến trạng thái này thật sự xảy ra ("buổi `BOOKED` sau hạn vẫn giữ"). Guard chỉ xét `ACTIVE`/`PENDING_PAYMENT`, còn listener không hủy buổi khi người bị ngừng là GV. Kết quả: buổi tương lai không có người dạy và không có thông báo nào, trái FR-004 ("bị chặn cho tới khi mọi học viên được đổi giáo viên"). | `TeacherDeactivationGuard.java:33-36`; `SystemCancelListener.java:72`; `data-model.md:191`; `spec.md:206`; `events-and-ports.md:53` | Guard chặn thêm ghi danh `EXPIRED` có `sessions_reserved > 0` và HV chưa ẩn danh. Cập nhật `events-and-ports.md:53`. Thêm IT. |
| R4 | MINOR | **`extend` tạo ra `ACTIVE` với 0 buổi còn lại.** `consume` trên gói `EXPIRED` có thể làm `consumed = total` nhưng không chuyển `EXHAUSTED` (`exhaustIfUsedUp` chỉ xét `ACTIVE`); `extend` sau đó đặt `ACTIVE` mà không xét lại. Lệch `data-model.md:190`. Đặt buổi vẫn bị chặn đúng bằng `NO_SESSIONS_LEFT` và số buổi không sai, nhưng ghi danh này giữ GV ở guard và tính là "đang học" (`ProfileService.java:57-58`) tới khi hết hạn. | `Enrollment.java:222-223,272-276`; `TeacherDeactivationGuard.java:34` | Gọi `exhaustIfUsedUp()` cuối `extend`. Thêm unit test vào `EnrollmentTest`. |
| R5 | MINOR | **Job chỉ bắt xung đột version.** Mọi lỗi khác của một ghi danh (lỗi DB, deadlock, ràng buộc) dừng cả vòng lặp, nên các ghi danh còn lại phải chờ 24 giờ; nếu là lỗi lặp lại ở cùng một dòng thì các dòng sau nó không bao giờ được xử lý. Javadoc nói "không làm hỏng các bản khác" nhưng chỉ đúng với một loại lỗi. Việc xử lý xung đột version là đúng theo đọc code (`JpaTransactionManager` dịch lỗi lúc commit), nhưng chưa có test (G8). | `EnrollmentLifecycleJob.java:44-48` | Thêm `catch (RuntimeException e)` ghi `log.error` kèm id rồi đi tiếp; có thể kết thúc bằng log số lỗi. |
| R6 | MINOR | **Lọc `teacherId` của admin làm mất HV ẩn danh.** Điều kiện `!u.anonymized()` nằm chung nhánh `allowed != null`, nên áp dụng cả khi admin lọc theo GV. Trái ý định "giáo vụ vẫn thấy hồ sơ để đối soát" (`AnonymizationIT.java:530`); test và smoke chỉ gọi không có `teacherId`. | `ProfileService.java:118,128` | Chỉ ẩn khi người xem là GV (`CatalogAccess.isTeacher(actor)`). Thêm 1 assert với `GET /students?teacherId=` của admin. |
| R7 | MINOR | **Tài liệu chưa khớp code:** `EnrollmentApi` thiếu `findCancelledIds`; `EnrollmentDto` thiếu 3 trường hủy; `POST /sessions` thiếu `ENROLLMENT_NOT_ACTIVE`, `ANONYMIZED`; NFR ghi `lockAtMostFor` = 5 phút trong khi job dùng 30 phút (mức 30 phút hợp lý vì job duyệt toàn bộ ghi danh, nên sửa NFR); guard chưa ghi việc bỏ qua HV ẩn danh. | `events-and-ports.md:49,53`; `rest-api.md:64,75`; `nfr.md:54`; `EnrollmentLifecycleJob.java:38` | Cập nhật cùng lượt với F3. |
| R8 | NOTE | **Không ai đọc được `extended_by`, `extended_at` qua API.** Cột được ghi đúng (S-US5.6 AC2, NFR-PRIV-04), nhưng DTO có 3 trường hủy mà không có 2 trường này. Không trái AC; chỉ là giáo vụ phải tra CSDL để biết ai gia hạn, và G5 không test được qua HTTP. | `EnrollmentController.java:45-85`; `rest-api.md:61` | Thêm 2 trường, chỉ trả cho admin, nếu FE cần hiển thị. |
| R9 | NOTE | **Còn vài chu trình khóa hiếm; khi xảy ra người dùng nhận 500.** Khóa advisory của đặt buổi, đổi lịch, ngày nghỉ đúng thứ tự trong trường hợp thường (xem 6.3). Còn lại: (a) sau đổi GV, trước khi listener hủy xong, `reschedule` giữ khóa GV cũ + dòng ghi danh rồi xin khóa GV mới, còn `book` giữ khóa GV mới rồi xin dòng ghi danh; (b) `SystemCancelListener` giữ dòng ghi danh rồi ghi buổi tiếp theo, còn `cancel` giữ dòng buổi rồi xin dòng ghi danh (không liên quan khóa advisory); (c) `create` đọc `teacherId` trước khi khóa nên đổi GV chen vào có thể để lại buổi với GV cũ mà listener không thấy. Postgres hủy một bên sau khoảng 1 giây; lỗi 40P01 không được ánh xạ nên rơi vào handler 500. | `CancellationService.java:94,98`; `BookingService.java:70,78,91`; `SystemCancelListener.java:93-99`; `GlobalExceptionHandler.java:65,88` | Ánh xạ `PessimisticLockingFailureException` sang 409 `CONCURRENT_MODIFICATION`. Các mục (a), (b), (c) ghi nợ sau demo; (c) sửa bằng cách so lại `teacherId` sau `reserve`. |
| R10 | NOTE | **Phân trang không làm hỏng FE hiện có, nhưng để lại rủi ro khi FE gọi tới.** `frontend/src` chỉ có 1 lời gọi `POST /sessions` trong test; mặc định 200 nên lời gọi cũ chạy như trước tới 200 buổi (reviewer chưa xem PR #6/#7). Rủi ro ngoài F3: trang tóm tắt dữ liệu cá nhân FR-031 ghép từ `/sessions` (`rest-api.md:43`) sẽ thiếu dòng **mà không báo** nếu HV quá 200 buổi. Ngoài ra `page` rất lớn có thể làm offset vượt `int` và trả 500 (suy từ cách Spring Data đổi offset, cần xác nhận). | `SessionController.java:149-156,169-173`; `rest-api.md:43,76` | Khi sửa F3, ghi rõ FE phải lặp tới khi nhận ít hơn `size` dòng và báo #4 cho trang tóm tắt. Thêm giới hạn trên cho `page` để trả 400. |
| R11 | NOTE | **Không có lần chạy bù, trong khi demo chạy trên laptop.** Máy tắt lúc 00:05 thì hôm đó không có `EXPIRED` và không có nhắc; NFR-PERF-06 vốn tính theo "lần chạy đầu tiên". Đặt lịch vẫn bị chặn đúng khi quá hạn. Chưa có tài liệu nào nêu cách đổi cron; cấu hình này chỉ có trong code và test. | `EnrollmentLifecycleJob.java:37`; `quickstart.md:64` | Ghi vào runbook demo cách đặt `app.jobs.enrollment-lifecycle-cron` để chạy bước 13. Nếu muốn, chạy thêm job khi khởi động; an toàn vì job đã idempotent. |
| R12 | NOTE | **Báo cáo số buổi không có dòng cho GV 0 buổi và không có thứ tự ổn định.** S-US5.7 AC1 nói "từng giáo viên"; FE phải hiểu GV vắng mặt là 0. | `SessionRepository.java:75-82`; `TeacherSessionsReportController.java:40-53` | Báo #4 trước khi làm T123; thêm sắp xếp theo tên nếu cần. |
| R13 | NOTE | **`EventRedeliveryIT` phát lại mọi publication dang dở (`p -> true`) trên CSDL dùng chung**, gồm cả bản thua còn sót của F7, nên có thể sinh log và hiệu ứng ngoài phạm vi test. | `EventRedeliveryIT.java:67` | Lọc đúng event của test; làm cùng F6. |
| R14 | NOTE | **`recordLogin` đúng với hồ sơ ẩn danh, nhưng thiếu điều kiện `status`.** Với ẩn danh: `AnonymizationService` khóa dòng trước, nên login chen vào trả 0 dòng và rollback, không tạo token; login đến trước thì token bị thu hồi sau đó. Không tăng `version`, entity cũ trong phiên không bị ghi lại. Đề xuất gốc (R1 U7) có thêm `status = 'ACTIVE'` nhưng code không có: login chen vào lúc giáo vụ ngừng HV có thể tạo refresh token sau bước thu hồi, token này bị chặn khi còn `INACTIVE` và dùng lại được nếu kích hoạt lại. Nhánh `== 0` của `AuthService` chưa có test qua `login`. | `UserAccountRepository.java:29-30`; `AuthService.java:71-73,100`; `AnonymizationService.java:58-73` | Thêm `and u.status = 'ACTIVE'` vào câu update. |
| R15 | NOTE | **`record` thanh toán có 2 trường hợp nên chốt rõ.** (a) Thanh toán đầu trên ghi danh `PENDING_PAYMENT` của HV ẩn danh vẫn kích hoạt gói; email bị chặn ở `@invalid`, nên chỉ là cùng loại với F1. (b) Trạng thái `CANCELLED` được đọc không khóa trước khi khóa `receivable`, nên `cancel` chen vào có thể để lại khoản thu trên ghi danh đã hủy; tổng tiền vẫn khớp vì khoản đó được tính vào đã thu. | `PaymentService.java:66-69,83-85`; `EnrollmentApiImpl.java:97-107` | (a) Bỏ qua kích hoạt khi HV ẩn danh, vẫn nhận tiền. (b) Chấp nhận cho demo. |

### 6.3. Các điểm đã kiểm và không thấy vấn đề

- **Tiền:** công thức công nợ, `hasDebt`, tổng công nợ loại ghi danh hủy và không bù thu vượt, `sumCollected` theo `paid_at` kể cả điều chỉnh âm đều khớp `spec.md:239` và quyết định đã chốt. `adjust` cộng `amount_paid` trong cùng transaction có khóa dòng `receivable`.
- **Số buổi:** `TAUGHT` đúng FR-027. "Gián đoạn tính là đã học" thật sự thành `COMPLETED` qua `decideInterruption` và `consumeReleased` (đọc code; vẫn thiếu test luồng, G3). Kỳ báo cáo đúng ngày giờ VN, tính cả 2 đầu.
- **Job:** cờ đã nhắc và publication nằm trong cùng transaction, nên rollback không gây mất hoặc nhắc trùng; dedupe key theo từng lý do và từng người nhận là lớp chặn thứ hai. Biên `≤ 2` buổi, `≤ 7` ngày và `now >= expiresAt` đúng theo code (G1 vẫn thiếu test). Xung đột với `extend`: bản đọc cũ thua version, không ghi đè hạn mới. Có `@SchedulerLock`, zone cố định, mỗi dòng một transaction ngắn và không giữ transaction ngoài. Tải theo thiết kế chưa thấy nguy cơ, nhưng PERF-01 chưa được đo (k6 ở B7).
- **Khóa advisory:** có ở cả 3 đường đặt, đổi lịch, ngày nghỉ, và luôn được lấy trước dòng ghi danh. Đường tạo buổi duy nhất ngoài seed là `BookingService.create`. Đọc ngày nghỉ sau khi có khóa ở READ COMMITTED nên thấy bản đã commit. Khóa theo từng GV, tự nhả khi hết transaction, gửi email chạy sau commit nên thời gian giữ ngắn. `replaceRules`, `deleteException` không khóa là đúng, vì đổi lịch rảnh không ảnh hưởng buổi đã đặt (`AvailabilityService.java:24-25`).
- **Ẩn danh hóa:** `clearCancelReasonsOf` idempotent. Các đường ghi `enrollment` khác không ghi đè `cancel_reason` cũ: job, `extend`, `changeTeacher` không chạm `CANCELLED`, còn `EnrollmentApiImpl.update` khóa rồi đọc lại. `UserView.anonymized` lấy đúng từ entity; hàm tạo 6 tham số chỉ test dùng. `EnrollmentRenewalDue` tới muộn không gửi tới `@invalid`. Với HV đã ẩn danh, `extend` không ghi PII; `/receivables` và 2 báo cáo không đổi.
- **Phân quyền:** 3 endpoint báo cáo có `@PreAuthorize` ADMIN ở cấp class; `extend`, `cancel` chỉ admin; 3 trường hủy chỉ trả cho admin.
- **Ranh giới module:** billing chỉ dùng `EnrollmentApi`, `UserDirectory`, `UserView`; scheduling chỉ dùng `UserDirectory`; không có import chéo `internal` trong các file đã đọc.

### 6.4. Đối chiếu constitution

| Nguyên tắc | Kết quả | Bằng chứng |
|------------|---------|-----------|
| I. Modular Monolith | Đạt | Mục 6.3; `ModularityTests` pass theo QE. |
| II. Provider-Agnostic | N/A | U8 không thêm provider. |
| III. Free-First, YAGNI | Đạt | Dùng khóa advisory của Postgres và 3 cột mới, không thêm hạ tầng. |
| IV. Kiểm thử trọng yếu | Đạt phần cốt lõi, còn thiếu | Hết hạn, công nợ, đặt lịch chen ngày nghỉ, phân quyền đều có IT trên Postgres thật. Chưa có test cho F1, R1; G1-G9 chuyển T140/B7 theo phạm vi đã duyệt. CI chưa chạy. |
| V. Bảo mật và dữ liệu cá nhân | Đạt về phân quyền và Flyway; **còn khe hở R1** | R1 cùng loại rủi ro với `payment.note` đã được chấp nhận (do giáo vụ tự nhập), nhưng `cancel_reason` không có lý do chứng từ và `data-model.md:180` hứa xóa. Reviewer chưa xem được diff migration cũ; QE ghi không sửa. |
| Quy trình, DoD | Chưa đạt | Chưa có PR, CI, leader review. 37 mục chưa commit, nhiều khả năng vượt mục tiêu ~400 dòng; có đổi schema nên cần leader review. FE T115, T116, T123 của #4. |

### 6.5. Verdict đề xuất của reviewer

**PASS_WITH_NOTES** — Số tiền, số buổi, job, khóa và phân quyền đều đúng theo code và không có lỗi BLOCKING, nhưng F1 và R1 là 2 lỗi MAJOR về hồ sơ ẩn danh nên cần sửa kèm test tái hiện trước khi đánh dấu Verified.

Nếu người dùng không sửa F1, R1 thì cần ghi rõ là chấp nhận, vì R1 trái `data-model.md:180`. Nên sửa thêm R2-R6 nếu còn thời gian, vì mỗi mục chỉ cần sửa ít dòng; nếu không, chuyển T140/B7 cùng G1-G9. Các mục NOTE không chặn U8.

### 6.6. Re-review sau khi sửa (reviewer, 2026-11-04)

**Cách làm:** Reviewer chỉ đọc tĩnh code, test và tài liệu hiện tại; **không chạy gì, không xem được `git diff`**. Số 97 unit + 129 IT là số orchestrator quan sát trước khi đổi test startup, **chưa được xác minh** và không dùng làm bằng chứng ở đây. Nhận định về hành vi Spring Boot, ShedLock, Mockito đến từ hiểu biết về framework, chưa kiểm trên app; các mục đó ghi rõ "cần xác nhận".

#### Trạng thái từng lỗi

| Lỗi | Trạng thái | Bằng chứng và hồi quy |
|-----|-----------|----------------------|
| F1 | **Đóng** | `EnrollmentService.java:130`, trước kiểm GV. `AnonymizationIT.java:570-576` khẳng định 409 `ANONYMIZED` (không kiểm lại `expires_at`, nhưng lỗi làm rollback nên chấp nhận được). Contract `rest-api.md:61` đã cập nhật. Không thấy hồi quy. |
| R1 | **Đóng đường chính** | `EnrollmentService.java:146`. `AnonymizationIT.java:577-586` kiểm cả phản hồi và quét toàn CSDL. Khe phụ vẫn còn: request đã qua bước kiểm trước khi ẩn danh hóa commit vẫn ghi được lý do; cùng loại với R5 của U7, không chặn U8. |
| F2 | **Đóng** | `BookingService.java:80-84`, có ca HTTP ở `EnrollmentLifecycleJobIT.java:151-159`. Giờ sau hạn của gói chưa hết hạn vẫn là 400. `reschedule` dùng chung `create`: lỗi làm rollback nên buổi cũ được giữ. `slots` vẫn trả rỗng, đúng. |
| R2 | **Đóng** | `EnrollmentLifecycleStep.java:55,71-76`: bỏ qua trước khi đặt cờ. `EnrollmentLifecycleJobIT.java:162-181` phủ cả ngừng và kích hoạt lại. HV ẩn danh vẫn được bỏ qua vì `anonymize` đặt `INACTIVE` (`UserAccount.java:118`). Xem thêm R22. |
| R5 | **Đóng** | `EnrollmentLifecycleJob.java:48-50`; `EnrollmentLifecycleJobTest.java:25-36`. Nhánh xung đột version vẫn đứng trước, nên vẫn là WARN; vẫn chưa có IT (G8). Khi CSDL mất hẳn, mỗi dòng sẽ chờ hết timeout lấy kết nối rồi ghi 1 ERROR; không đổi kết quả, chấp nhận được. |
| R11 | **Đóng phần code; phần tài liệu và kiểm chứng còn mở** | Xem bảng startup, R19, R21. |
| F4 | **Đóng** | `SessionController.java:160-164`, `OutcomeService.java:104-106`, `SessionRepository.java:100-103`. Sắp xếp có `s.id` nên trang ổn định; test `SchedulingHardeningIT.java:104-117`. Chỉ `OutcomeService` gọi truy vấn này, job quá hạn dùng truy vấn riêng. |
| R10 | **Đóng phần `page`; phần trang tóm tắt chuyển #4** | `SessionController.java:155` so `(long) page * size` với giới hạn `int`, kiểm sau `size`, không tràn; test `SchedulingHardeningIT.java:100`. Contract đã ghi cách lặp trang (`rest-api.md:76`), nhưng `rest-api.md:43` chưa nhắc trang tóm tắt phải lặp. |
| F3 | **Đóng** | `rest-api.md:76`. |
| R7 | **Đóng gần hết** | `events-and-ports.md:49,53`, `nfr.md:54`, `rest-api.md:75` đã khớp. Còn `rest-api.md:64`: danh sách `EnrollmentDto` vẫn thiếu 3 trường hủy (gộp vào R21). |

**Chưa sửa, không nằm trong lượt này:** R3, R4, R6 (đã kiểm lại: `TeacherDeactivationGuard.java:34`, `Enrollment.java` chỉ gọi `exhaustIfUsedUp` ở `:142,151`, `ProfileService.java:128`), R9, R14 (`UserAccountRepository.java:29`), cùng R8, R12, R13, R15.

#### Kiểm tra riêng phần chạy bù khi khởi động

| Câu hỏi | Kết luận | Lý do |
|---------|----------|-------|
| Có chặn khởi động lâu không? | **Không đáng lo ở quy mô thiết kế, nhưng có làm chậm trạng thái sẵn sàng** (R17) | Listener chạy đồng bộ trên main thread. Theo trình tự của Spring Boot, Tomcat đã nhận request từ trước, nên API vẫn phục vụ; `ACCEPTING_TRAFFIC` chỉ được phát sau khi `ApplicationReadyEvent` xử lý xong. Email chạy bất đồng bộ sau commit nên không kéo dài bước này. Thời gian thực tế chưa đo. Lỗi `RuntimeException` chỉ ghi log (`EnrollmentLifecycleStartup.java:31-35`). |
| Có chạy trùng cron hoặc instance khác không? | **Không chạy song song** | Cùng đi qua `@SchedulerLock(name = "catalog.enrollmentLifecycle")`, thời gian khóa theo CSDL (`SchedulingConfig.java:23`). Nếu chạy nối tiếp nhau thì cờ đã nhắc và dedupe key chặn nhắc trùng. Bên đến sau khi khóa đang giữ sẽ **bỏ qua, không chờ, không log** (R18). |
| Gọi qua proxy có đúng không? | **Đúng** | Đây không phải tự gọi trong cùng class: `EnrollmentLifecycleStartup` là bean khác, gọi bean `job` được tiêm vào. `@EnableSchedulerLock` không đặt `interceptMode` nên dùng mặc định `PROXY_METHOD`, tức khóa ở mọi lời gọi qua proxy, không chỉ lời gọi từ scheduler (cần xác nhận ở ShedLock 7.10.1; R19 có cách kiểm). `step.process` cũng qua proxy nên `REQUIRES_NEW` vẫn hiệu lực. |
| Thứ tự với `demo-seed`? | **Chạy sau toàn bộ seed** | Các seed là `ApplicationRunner` (`CatalogDemoSeed.java:27-29`, `SchedulingDemoSeed.java:38-40`), chạy và commit trước `ApplicationReadyEvent`. Seed lỗi thì app dừng, job không chạy. |
| Có nhắc nhầm ở lần khởi động đầu không? | **Dữ liệu seed: không. DB đã dùng trước đó: có một trường hợp lệch nội dung** (R20) | Seed có 12 hoặc 24 buổi, tối đa 4 buổi đã dùng hoặc đã đặt, hạn 90 ngày, nên không chạm ngưỡng. Nhắc sớm hơn 00:05 không sai vì ngưỡng tính theo đúng thời điểm chạy. |
| `verify(job).run()` có chứng minh job chạy lúc khởi động không? | **Chứng minh dây nối, không chứng minh tác dụng** (R19) | Cron tắt trong test và không có nơi gọi khác, nên có lời gọi tức là do startup. Nếu spy nằm trong proxy ShedLock như dự kiến, test còn cho thấy đã lấy được khóa. Nhưng `catchUp` nuốt lỗi, nên `run()` ném lỗi vẫn xanh; test không có dữ liệu, không có `demo-seed`, và `atLeastOnce` không phát hiện chạy 2 lần. Test chỉ an toàn với một phương thức, vì spy reset sau mỗi test (cần chờ kết quả chạy bản cuối). Phần xử lý được `EnrollmentLifecycleJobIT` phủ, phần nuốt lỗi được unit test phủ, nhưng chưa có gì kiểm cả chuỗi. |

#### Phát hiện mới

| # | Mức | Mô tả và lý do | File:dòng | Đề xuất |
|---|-----|---------------|-----------|---------|
| R16 | MINOR | **Khôi phục sao lưu: chạy bù có thể gửi email tới HV đã yêu cầu xóa, trước khi runbook áp dụng lại ẩn danh hóa.** Runbook khởi động backend rồi mới làm lại ẩn danh hóa; dừng `caddy` chỉ chặn truy cập vào, không chặn email ra. Trên DB vừa khôi phục, email thật đã quay lại nên chặn `@invalid` không có tác dụng. Rủi ro này đã có sẵn từ job nhắc lịch (chạy sau 30 giây), nhưng chạy bù làm nó xảy ra ngay lúc khởi động và thêm cả nhắc gia hạn. Không ảnh hưởng demo laptop nếu không khôi phục; `drill.sh` không khởi động backend. | `EnrollmentLifecycleStartup.java:29-32`; `deploy/README.md:75-77,87-91`; `DispatchService.java:67`; `SessionReminderJob.java:36` | Sửa runbook khi xử lý R12 của U7: khởi động backend với `EMAIL_PROVIDER=fake` (cần xác nhận biến này được truyền vào container) và `app.jobs.enrollment-lifecycle-on-startup=false` tới khi xong bước 3. Làm **trước deploy thật**. |
| R17 | NOTE | **`/actuator/health` có thể trả 503 trong lúc chạy bù.** `probes.enabled: true` đưa trạng thái sẵn sàng vào health, trong khi `caddy` chờ backend healthy (cần xác nhận trên app). `start_period` 60 giây là dư nếu job chỉ mất vài giây. | `application.yml:59-62`; `docker-compose.prod.yml:41-46,65-67` | Đo thời gian ở smoke. Chỉ chuyển sang chạy nền nếu vượt khoảng 30 giây. |
| R18 | NOTE | **Bỏ qua lần chạy bù mà không có dấu vết.** Máy tắt đúng lúc job đang chạy thì khóa còn tới 30 phút; khởi động lại trong khoảng đó sẽ bỏ qua, và lần chạy kế tiếp là 00:05 hôm sau. Cửa sổ hẹp vì job ngắn. Job cũng không log bắt đầu hay kết quả, nên khó biết đã chạy chưa. | `EnrollmentLifecycleJob.java:38-52`; `EnrollmentLifecycleStartup.java:29-36` | Log `info` cuối `run()` với số đã xử lý và số lỗi; thiếu dòng này nghĩa là đã bị bỏ qua. |
| R19 | MINOR | **Chuỗi chạy bù chưa được kiểm từ đầu tới cuối, nên R11 chưa thể coi là đã xác nhận.** Xem bảng trên. | `EnrollmentLifecycleStartupIT.java:18-21`; `EnrollmentLifecycleJobTest.java:38-48` | Bắt buộc 1 smoke trên jar thật trước khi đánh dấu Verified: tắt app, dùng SQL đưa một ghi danh về quá hạn và một ghi danh còn ít buổi, bật app, kiểm `EXPIRED`, cờ nhắc, email, dòng `shedlock`; chạy thêm lần đầu với `demo-seed` trên DB trống (0 nhắc, 0 ERROR) và ghi thời gian. Trong IT: đổi sang `times(1)` và kiểm `shedlock.locked_at` để xác nhận đã đi qua khóa. |
| R20 | NOTE | **Lần chạy đầu trên DB đã dùng sẽ nhắc cả gói `EXHAUSTED` cũ** ("còn 0 buổi"), kể cả HV đã có gói mới. Cron cũng làm vậy; chạy bù chỉ làm nó xảy ra sớm hơn. Hệ thống chưa có dữ liệu thật nên chỉ ảnh hưởng DB diễn tập. Lưu ý thêm cho bước 13: cờ đã đặt thì khởi động lại không nhắc lần nữa. | `Enrollment.java:192-196`; `quickstart.md:64` | Dùng DB sạch trước buổi demo; mỗi lần diễn tập bước 13 tạo một ghi danh mới. |
| R21 | MINOR | **Tài liệu chưa theo kịp chạy bù.** `quickstart.md:64` chưa nói cách "chạy job" (khởi động lại hoặc đổi cron); `application.yml` và `deploy/README.md` không có 2 cấu hình của job; `nfr.md:33,147`, `events-and-ports.md:23` và Javadoc `EnrollmentLifecycleJob.java:17-19` chưa nêu chạy bù và lỗi khác. Còn `rest-api.md:64` như ở R7. | Như mô tả | Cập nhật cùng lượt; thêm 2 khóa có giá trị mặc định vào `application.yml`. Yêu cầu tài liệu của R11 tới đây mới xong. |
| R22 | NOTE | **HV `AWAITING_GUARDIAN_CONSENT` vẫn được nhắc gia hạn.** `canBeReminded` chỉ loại `INACTIVE`, trong khi `EnrollmentService.java:61` cho ghi danh HV ở trạng thái này. Quickstart chỉ yêu cầu không có email **mời** nên chưa rõ là vi phạm, nhưng đây là email tới HV dưới 16 tuổi trước khi có đồng ý giám hộ. `EnrollmentActivated` có cùng tình trạng từ trước. | `EnrollmentLifecycleStep.java:71-76`; `CatalogNotificationListener.java:83-105`; `quickstart.md:66` | Người dùng chốt cùng các câu hỏi pháp lý; nếu cần chặn thì xử lý chung cho mọi email gửi HV, không chỉ job này. |

#### Các mục còn lại nên chuyển đi đâu

| Nơi chuyển | Mục |
|-----------|-----|
| **Trước khi đánh dấu Verified** | `clean verify` của bản cuối xanh; smoke của R19. Nên làm cùng R21 vì chỉ sửa tài liệu và cấu hình. |
| **T140/B7** | G1-G9 (phần chưa có test), F5, F6, R13, phần IT của R19, R3, R4, R6, R14, ánh xạ 40P01 của R9, log của R18. R3 nên làm sớm nhất trong nhóm vì liên quan FR-004. |
| **Trước deploy thật** | R16 (cùng R12 của U7), F7 phần production (cùng R6 của U7), khe phụ của R1 (cùng R5 của U7), đo R17; CI, gitleaks, leader review. |
| **#4 (FE)** | T115, T116, T123; lặp trang ở trang tóm tắt FR-031 (R10); R12; R8 nếu FE cần hiển thị người gia hạn. |
| **Sau demo** | R9 (a), (b), (c); R15; F8; R20 nếu có dữ liệu thật trước khi bật job. |
| **Cần người dùng chốt** | R22. |

#### Verdict đề xuất sau re-review

**PASS_WITH_NOTES** — F1, R1 và các lỗi đã sửa đều đóng theo code và không thấy hồi quy, không còn lỗi BLOCKING hay MAJOR, nhưng kết quả chỉ có hiệu lực sau khi `clean verify` bản cuối xanh và smoke chạy bù (R19) đạt, vì reviewer không chạy gì và test startup chỉ chứng minh dây nối.

## 7. Kết quả cuối sau khi sửa (orchestrator, 2026-11-04)

- **Build & test bản cuối:** `./mvnw clean verify` BUILD SUCCESS, **97 unit + 130 IT**; `spotless:check` không cache PASS. Chạy 1 lần trên máy dev; chưa chạy CI, gitleaks.
- **Test đỏ trước khi sửa (quan sát được):** F1, R1, F2, R2, R5, F4, R10 (`page` rất lớn), R22; chạy bù khi khởi động (khung rỗng).
- **Smoke chạy bù (R19), jar thật + Postgres + Mailpit:** 3 lần khởi động với `demo-seed`: lần đầu không gửi email nào, có dòng `shedlock`; đặt gói quá hạn → `EXPIRED`; gói còn 3 ngày → nhắc HV + admin; khởi động lần 3 không nhắc lại; 0 dòng lỗi; health 200 sau ~12–14 s. Đã dọn `u8start`.
- **Sửa sau re-review 6.6:** R21, R7 phần còn lại, R22 (không nhắc HV chờ đồng ý giám hộ). Các sửa này chưa qua review độc lập lần nữa.
- **Còn biết:** email `EnrollmentActivated` vẫn có thể gửi cho HV `AWAITING_GUARDIAN_CONSENT` nếu ghi nhận thanh toán trước khi có đồng ý (cùng loại R22, chưa sửa → T140).
- **Chuyển tiếp:** T140/B7: G1–G8, F5, F6, R13, R3, R4, R6, R14, ánh xạ 40P01 (R9), log R18, email kích hoạt ở trên. Trước deploy: R16, F7 production, khe phụ R1, đo R17, CI, gitleaks, leader review. FE #4: T115, T116, T123, lặp trang ở trang tóm tắt FR-031, R12, R8. Sau demo: R9, R15, F8, R20; U5 R5, R7; U4 R6.
- **Verdict: PASS_WITH_NOTES.**
