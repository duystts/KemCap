# Build & Test Report — U3 — Nền tảng đào tạo tiếng Anh 1 kèm 1 cá nhân hóa (001-personalized-english-tutoring)

- **Ngày (UTC)**: 2026-10-18, 08:36–08:42 UTC (giờ máy: 15:36–15:42 +07)
- **Unit**: U3 — Lịch rảnh, đặt lịch, trạng thái buổi & phòng học (B3), **chỉ phần BACKEND**. FE T075–T079 thuộc #2, ngoài phạm vi (vẫn `[ ]` trong tasks.md:183-187).
- **Tasks trong phạm vi**: T056–T074 — hoàn thành: **19/19** (tất cả `[X]` trong tasks.md:161-182)
  - `GET /reports/teacher-sessions` có trong mục BE của U3 (unit-of-work.md:244) nhưng tasks.md:293 xếp vào T121 (#4·U8) và chưa có trong code. QE không tính vào U3, xem F6.
  - Mục chuyển từ verify U2 (unit-of-work.md:224): U3 gọi `EnrollmentApi` trong cùng transaction với session. Có bằng chứng: `BookingConcurrencyIT.tenStudentsRaceForOneTeacherSlotOnlyOneWins` khẳng định bên thua không bị giữ chỗ (tổng `reserved = 1`); `studentCannotHoldOverlappingSessionsWithTwoTeachers` khẳng định `reserved = 0` trên ghi danh thua. `EnrollmentApiIT.staleReadInCallerTransactionStillGetsNoSessionsLeft` (R2) pass.
- **Môi trường**: Windows 11, Git Bash; JDK 21.0.12 (`JAVA_HOME=/c/Program Files/Java/jdk-21.0.12`); Docker Desktop (compose: postgres, mailpit, livekit dev đang chạy); Testcontainers `postgres:16`; Node 22.21.0 + npm.

## 1. Build

| Thành phần | Lệnh | Kết quả | Ghi chú |
|------------|------|---------|---------|
| Backend (Maven, gồm Enforcer) | `cd backend && ./mvnw -B verify` | **PASS** (`BUILD SUCCESS`, exit 0, 01:50 min) | Enforcer: `RequireJavaVersion passed`, `BannedDependencies passed`. Log có 0 dòng ` ERROR `. Cảnh báo không chặn (giống U2): 2 repo metadata HTTP 401 (`flyway-community-db-support`, `MavenFeed`); Testcontainers `Reuse was requested but the environment does not support the reuse of containers`. |
| Backend format | `cd backend && ./mvnw -B spotless:check` | **PASS** (`BUILD SUCCESS`, 2.2 s) | Không có vi phạm format. |
| Frontend lint | `cd frontend && npm run lint` (`oxlint src`) | **PASS** (exit 0; 0 lỗi, **10 cảnh báo**) | Đều là `react(only-export-components)`, không đổi so với U2. |
| Frontend format | `cd frontend && npx prettier --check src` | **PASS** | "All matched files use Prettier code style!" |
| Frontend build | `cd frontend && npm run build` (`tsc -b && vite build`) | **PASS** (vite 8.3.4, 2093 module, 1.94 s) | Chunk chính `index-*.js` 496.88 kB (gzip 154.27 kB), giống U2: FE không đổi. |

## 2. Test

| Bộ test | Lệnh | Pass / Fail / Skip | Ghi chú |
|---------|------|--------------------|---------|
| Unit + Scenario (surefire) | `./mvnw -B verify` | **54 / 0 / 0** | Mới ở U3: `SlotServiceTest` 6/0/0; `SessionOutcomeTest` 12/0/0 (gồm 4 case tham số); `JoinServiceTest` 4/0/0; `LiveKitVideoRoomProviderTest` 2/0/0. Có từ trước: `EnrollmentTest` 12, `PaymentServiceTest` 7, `BillingFlowTest` 2, `ModularityTests` 2, `NotificationDispatchTest` 2, `SecurityPropertiesTest` 3, `AuthorizationRulesTest` 1, `GlobalExceptionHandlerTest` 1, tất cả pass. |
| Integration Testcontainers (failsafe) | `./mvnw -B verify` | **67 / 0 / 0** | Mới ở U3: `BookingConcurrencyIT` 4/0/0; `CancellationPolicyIT` 5/0/0 (surefire ghi dưới tên `CancellationPolicyIT$WithLongerCutoff`: 4 test ngoài + 1 test `@Nested`); `SchedulingJobsIT` 4/0/0; `SessionOutcomeIT` 3/0/0; `SystemCancelIT` 3/0/0 (Modulith `Scenario`). Có từ trước: `CatalogFlowIT` 9, `EnrollmentApiIT` 8, `StudentAccessIT` 6, `AuthFlowIT` 21, `NotificationDispatchIT` 4, tất cả pass. Toàn bộ IT chạy với `app.providers.video=fake` (`src/test/resources/application-test.yml:5`). |
| Frontend unit (vitest) | `cd frontend && npm test -- --run` | **3 / 0 / 0** (1 file, 5.81 s) | Chỉ có `client.test.ts`, không đổi. |
| Smoke local (demo-seed, adapter LiveKit thật) | `java -jar backend/target/english-platform-0.0.1-SNAPSHOT.jar --spring.profiles.active=local,demo-seed` | **PASS 20 / 0 / 0** (số probe) | Khởi động 13.1 s; Flyway `Successfully validated 6 migrations` (V1–V5, V8; DB đã ở bản mới nhất, không migrate); log 0 dòng ` ERROR `. Profile `local` dùng `video: livekit` (`application-local.yml:6`; `LiveKitVideoRoomProvider` `matchIfMissing=true`). Đăng nhập student1, student2, teacher1, teacher2, admin với `Demo@1234` đều ra token. student1 và student2 cùng GV teacher1. Slot chọn: `2026-10-29T13:00:00Z` (trống với cả hai HV). Kết quả quan sát được: |
| | `GET /enrollments/{id}/slots?from=+5d&to=+12d` (student1; student2 với ghi danh của mình) | PASS | Cả hai HTTP 200, 30 slot mỗi bên |
| | `GET /enrollments/{id của student1}/slots` (student2; teacher1) | PASS | student2 → 404 `NOT_FOUND`; teacher1 → 403 `FORBIDDEN` (đúng T065: chỉ A và HV chủ ghi danh) |
| | `POST /sessions` (student1) | PASS | HTTP 201; bộ đếm ghi danh 2/2/20 → **3/2/19** (reserved/consumed/remaining); slot đã đặt biến mất khỏi danh sách slot |
| | `POST /sessions` (student2, cùng slot) | PASS | HTTP 409 `SLOT_TAKEN` ("Khung giờ này vừa có người đặt") |
| | `GET /sessions/{id}/cancel-preview` (student1) | PASS | HTTP 200 `willConsume=false`, `cutoffAt=2026-10-29T01:00:00Z` (= starts − 12h) |
| | `POST /sessions/{id}/join` ngoài khung (student1) | PASS | HTTP 403 `ROOM_NOT_OPEN` |
| | `POST /sessions/{id}/join` người thứ ba (teacher2) | PASS | HTTP 404 `NOT_FOUND` |
| | `GET /sessions/{id}` (student2, không phải người của buổi) | PASS | HTTP 404 |
| | `POST /sessions/{id}/cancel` (student1) | PASS | HTTP 200, `GET` lại → `CANCELLED_ON_TIME`; bộ đếm về **2/2/20** |
| | `POST /sessions/{id}/cancel` lần 2 | PASS | HTTP 409 `INVALID_STATE_TRANSITION` |
| | Đặt `12:00Z` → `POST /reschedule` sang `13:00Z` đã trống → hủy buổi mới | PASS | 201 → 200 (buổi mới có `rescheduledFromId` = buổi cũ, buổi cũ `CANCELLED_ON_TIME`) → 200 `CANCELLED_ON_TIME`. Bộ đếm cuối **2/2/20**, đúng như trước smoke (dữ liệu dọn lại, chỉ còn 3 bản ghi `CANCELLED_ON_TIME`). |
| | `GET /sessions?outcomePending=true` (admin; student1) | PASS (rỗng) | Cả hai HTTP 200, **0 buổi**: seed không có buổi `BOOKED` đã qua, nên không xem được dữ liệu thật ở bước này (đã có IT `SessionOutcomeIT.adminSettlesOverdueAndDecidesInterruptions` khẳng định 3 buổi). Xem F5. |
| | `GET /sessions` (teacher1); không token | PASS | 200, 10 buổi, tất cả của teacher1; không token → 401 |
| | Join **trong** khung với LiveKit thật (lấy token, vào phòng `ws://localhost:7880`) | **NOT RUN** | Không có buổi nào đang trong khung −10/+15 phút; muốn thử phải đặt buổi sát giờ, nhưng quy tắc cho đặt sớm nhất now + 2h. Việc phát token chỉ có unit `LiveKitVideoRoomProviderTest`. Sau smoke, process java (PID 11272) đã bị dừng; `/actuator/health` trả 000. |
| CI GitHub Actions trên PR thật | `.github/workflows/ci.yml` | **NOT RUN** | Chưa có PR. |
| gitleaks | — | **NOT RUN** | Không có trong danh sách lệnh của lượt này. |

### Đối chiếu DoD riêng của U3 (unit-of-work.md:257-264)

| Mục DoD | Trạng thái | Test và kết quả quan sát |
|---------|-----------|--------------------------|
| 10 luồng cùng slot, **cùng GV** → đúng 1 thành công (NFR-REL-01) | **Đạt** | `BookingConcurrencyIT.tenStudentsRaceForOneTeacherSlotOnlyOneWins` pass: 10 HV, 10 thread, `CountDownLatch`; khẳng định 1×201, 9×409, tổng `sessionsReserved = 1`. Smoke: lượt thứ 2 → 409 `SLOT_TAKEN`. |
| 10 luồng cùng slot, **cùng HV khác GV** → đúng 1 thành công | **Đạt một phần** | Chỉ có test **tuần tự** 2 lượt: `BookingConcurrencyIT.studentCannotHoldOverlappingSessionsWithTwoTeachers` pass (lượt 2 chồng 30 phút với GV khác → 409 `SLOT_TAKEN`, `reserved` ghi danh thua = 0). Chưa có test đồng thời nhiều luồng cho exclusion constraint phía HV (F1). |
| Hủy trước / sau hạn chót | **Đạt** | `CancellationPolicyIT.cancelBeforeCutoffRefunds` (`willConsume=false`, `CANCELLED_ON_TIME`, còn 4/4), `cancelAfterCutoffConsumes` (T−11h: `willConsume=true`, `cutoffAt` = starts − 12h, `CANCELLED_LATE`, consumed=1), `teacherCancelIsTeacherAbsentAndRefunds` (T−1h → `TEACHER_ABSENT`, hoàn) pass; unit `SessionOutcomeTest.cancelOnTimeRefundsAndLateCharges`, `cancelAfterStartIsInvalid` pass. Smoke: preview + hủy đúng hạn. |
| Đổi lịch sau hạn chót bị chặn | **Đạt** | `CancellationPolicyIT.rescheduleOnlyBeforeCutoff` pass: đổi đúng hạn → `rescheduledFromId`, buổi cũ `CANCELLED_ON_TIME`, reserved = 1; T−6h → 409 `CANCEL_WINDOW_PASSED`; GV đổi lịch → 403. Smoke: đổi lịch đúng hạn 200. |
| Đổi giá trị cấu hình hạn chót → hành vi đổi theo | **Đạt** | `CancellationPolicyIT.WithLongerCutoff.cutoffFollowsConfiguration` pass (`app.scheduling.cancel-cutoff-hours=48`: T−40h → `willConsume=true`, `CANCELLED_LATE`) |
| Trừ/hoàn theo **mọi** trạng thái kết thúc | **Đạt** | Unit `SessionOutcomeTest.outcomeChargesOrRefunds` (COMPLETED, STUDENT_NO_SHOW → CONSUME; INTERRUPTED, TEACHER_ABSENT → RELEASE), `cancelOnTimeRefundsAndLateCharges`, `teacherCancelMeansTeacherAbsentAndRefunds` pass. IT trên DB: `SessionOutcomeIT.onlySessionTeacherOrAdminRecordsOutcomeAfterStart` (COMPLETED consumed=1 → admin sửa TEACHER_ABSENT consumed=0) pass. |
| — kể cả `CANCELLED_SYSTEM` | **Đạt** | Unit `SessionOutcomeTest.systemCancelRefundsOnlyFutureBookedSessions` pass. IT `SystemCancelIT` 3/3 pass: `TeacherReassigned` (qua `PUT /enrollments/{id}/teacher`), `UserDeactivated` (qua `PATCH /users/{id}/status`), `StudentAnonymized` → `CANCELLED_SYSTEM`, `reserved = 0`; buổi đã qua giữ `COMPLETED`, consumed = 1. |
| — chốt thay (admin) | **Đạt** | `SessionOutcomeIT.adminSettlesOverdueAndDecidesInterruptions` pass (admin chốt `STUDENT_NO_SHOW`, `outcomeBy` = admin); admin sửa sau 8 ngày → 409 (`onlySessionTeacherOrAdminRecordsOutcomeAfterStart`); unit `adminCorrectionWithinSevenDaysRecalculatesCounters` pass |
| — INTERRUPTED → COUNT / MAKE_UP | **Đạt** | `SessionOutcomeIT.adminSettlesOverdueAndDecidesInterruptions` pass (COUNT → `COMPLETED`; MAKE_UP giữ `INTERRUPTED`; GV quyết định → 403; cuối: reserved 0, consumed 2, remaining 2); unit `interruptionCountBecomesCompletedAndMakeUpKeepsInterrupted` pass |
| IT đối chiếu bộ đếm (chuỗi ngẫu nhiên, catalog thật, NFR-REL-02) | **Đạt (có giới hạn)** | `SessionOutcomeIT.countersAlwaysReconcileWithSessionHistory` pass: seed `Random(42)`, 18 buổi, thao tác hủy đúng hạn / hủy muộn / GV hủy / chốt 4 loại kết quả + COUNT / admin chốt thay / để nguyên; so `reserved` = số `BOOKED`, `consumed` = số COMPLETED+NO_SHOW+LATE. Giới hạn: một seed cố định, chạy tuần tự, không có `CANCELLED_SYSTEM` và đổi lịch trong chuỗi (F2). |
| Slot DST (NFR-UX-02) | **Đạt (chỉ unit)** | `SlotServiceTest.teacherTimezoneWithDaylightSavingIsConvertedCorrectly` pass (Europe/Berlin 29/03/2026: bỏ 02:00/02:30 không tồn tại, 03:00 CEST = 01:00Z, T2 09:00 CEST = 07:00Z). Chưa test chiều lùi giờ (giờ lặp lại tháng 10) và chưa có IT qua `/slots` (F3). |
| Job với `TestClock` không phát trùng | **Đạt** | `SchedulingJobsIT.remindersFireOncePerWindow` (chạy job 2 lần mỗi mốc → đúng 1 `SessionReminderDue` H24 và 1 H1), `sessionBookedInsideWindowSkipsThe24hReminder`, `outcomeOverdueFiresOnce` (+20h → 0; +26h chạy 2 lần → 1) pass. Job được gọi trực tiếp qua `run()`; ShedLock giữa nhiều instance không được test (chấp nhận, demo 1 instance). |
| Join: người thứ ba → 404 | **Đạt** | Unit `JoinServiceTest.strangerGets404` pass (mock `VideoRoomProvider`, `SessionRepository`). Smoke HTTP: teacher2 → 404 `NOT_FOUND`. |
| Join: ngoài khung → 403 `ROOM_NOT_OPEN` | **Đạt** | Unit `JoinServiceTest.outsideWindowIsRoomNotOpen` (−11 phút, +60 phút), `cancelledSessionRoomIsClosed`, `participantsJoinInsideWindowWithTokenUntilRoomCloses` (−10 phút mở; `validUntil = ends_at + 15m`; ghi `*_joined_at` lần đầu) pass. Smoke HTTP: 403 `ROOM_NOT_OPEN`. Chưa có IT `/join` trên DB (F4). |
| Suite chạy với `video=fake` | **Đạt** | Toàn bộ 67 IT dùng `app.providers.video=fake` (`application-test.yml:5`) và pass |
| Suite chạy với `video=livekit` | **Đạt một phần / NOT RUN** | Không có lượt chạy suite với `video=livekit`. Chỉ có unit `LiveKitVideoRoomProviderTest` 2/0/0 (token có `room=session-{id}`, `roomJoin`, `canPublish`, `exp` ≈ validUntil; từ chối validity đã hết) với key giả, không kết nối server. Smoke local chạy với adapter LiveKit thật, nhưng chỉ đi tới các nhánh 403/404 nên chưa phát token thật (F4). |
| Listener hủy hệ thống idempotent | **Đạt** | `SystemCancelIT.teacherReassignmentCancelsFutureSessionsWithOldTeacher` (phát lại `TeacherReassigned` → reserved vẫn 0), `anonymizationCancelsFutureSessionsIdempotently` (phát `StudentAnonymized` 2 lần) pass; unit `systemCancelRefundsOnlyFutureBookedSessions` (gọi lần 2 → rỗng). Phát lại `UserDeactivated` chưa được test riêng. |
| Go/no-go LiveKit (CN 1/11) | **NOT RUN (ngoài phạm vi BE)** | Là quyết định FE + thử tay (T079, #2) vào 1/11. BE đã có adapter + endpoint; vào phòng thật 2 người chưa được kiểm. |
| (Phụ) `PENDING_PAYMENT` không đặt được; slot đúng quy tắc | **Đạt** | `BookingConcurrencyIT.pendingPaymentEnrollmentCannotBook` (409), `bookedSlotDisappearsFromSlotListAndSlotRulesAreEnforced` (ngoài lịch rảnh / lệch bước 30' / quá 30 ngày → 400; HV khác → 404) pass; `SlotServiceTest` 5 test còn lại (bước 30', lead 2h, horizon 30 ngày + `expires_at`, ngoại lệ, buổi bận) pass |
| (Phụ) `EXCEPTION_OVERLAPS_SESSIONS`; lịch rảnh chồng giờ | **Đạt** | `SchedulingJobsIT.availabilityRulesAndExceptions` pass (khoảng chồng cùng ngày → 400; GV khác → 404; ngày nghỉ trùng buổi → 409 kèm `sessions[0].id`; ngoại lệ khoảng giờ không trùng → 201) |
| Ranh giới module sau khi thêm scheduling | **Đạt** | `ModularityTests` 2/0/0; SDK LiveKit chỉ import ở `scheduling/internal/video/LiveKitVideoRoomProvider.java` (unit-of-work.md:242 ghi `internal/livekit`, chỉ khác tên gói) |

## 3. Review độc lập (theo constitution)

QE chỉ rà ở mức bằng chứng build/test, không phải code review đầy đủ.

| Nguyên tắc | Kết quả (Đạt / Vi phạm / N/A) | Bằng chứng (file:dòng) |
|------------|-------------------------------|------------------------|
| I. Modular Monolith, `ApplicationModules.verify()` | Đạt (local) | `ModularityTests` pass sau khi thêm `scheduling`; scheduling dùng `catalog.EnrollmentApi` (`SchedulingTestSupport.java:5`) và event `TeacherReassigned`/`UserDeactivated`/`StudentAnonymized` (`SystemCancelIT`) |
| II. Provider-Agnostic (Ports & Adapters) | Đạt, có ghi chú | `VideoRoomProvider` với `FakeVideoRoomProvider` (`@ConditionalOnProperty havingValue="fake"`, `FakeVideoRoomProvider.java:11`) và `LiveKitVideoRoomProvider` (`LiveKitVideoRoomProvider.java:23`); SDK chỉ ở adapter. Chưa có lượt suite chạy với adapter livekit (F4) |
| III. Free-First & YAGNI | Đạt | LiveKit dev mode tự host; hạn chót hủy chỉ qua cấu hình `app.scheduling.cancel-cutoff-hours` (`application.yml:92`), không có API cài đặt |
| IV. Kiểm thử trọng yếu, Postgres thật, cấm H2, CI mỗi PR | Đạt một phần | Chống trùng lịch (exclusion constraint), hủy/hạn chót, trừ/hoàn mọi trạng thái, đối chiếu bộ đếm, job không trùng, listener idempotent: đều có IT trên `postgres:16` thật (121/0/0 toàn backend). Còn thiếu: race đồng thời phía HV (F1), suite `video=livekit` (F4). CI chưa chạy |
| V. Bảo mật & dữ liệu cá nhân | Đạt (phạm vi U3) | Smoke: không token → 401; HV khác xem slot/buổi → 404; GV ngoài buổi join → 404; GV xem slot → 403. IT: chốt kết quả GV khác → 404, HV → 403 (`SessionOutcomeIT.java:42-45`); quyết định Gián đoạn chỉ admin (`SessionOutcomeIT.java:97-101`); hủy hệ thống khi ẩn danh hóa / ngừng kích hoạt |
| Ràng buộc công nghệ (Java 21, Boot 4.1, Flyway, ShedLock) | Đạt | Enforcer pass; `V5__scheduling.sql`; Flyway validate 6 migration khi khởi động; `@SchedulerLock` ở `SessionReminderJob.java:37`, `OutcomeOverdueJob.java:40` |
| Quy trình & DoD (CI xanh + review + demo) | Chưa đạt | Mọi gate local xanh, nhưng chưa có PR/CI và chưa có review độc lập cho U3; go/no-go LiveKit chờ FE 1/11 |

## 4. Phát hiện

| # | Mức độ | Mô tả | File | Đề xuất |
|---|--------|-------|------|---------|
| F1 | Trung bình | DoD "10 luồng cùng slot, cùng HV khác GV → đúng 1" chỉ có test tuần tự 2 lượt. Exclusion constraint phía HV (`session_student_*`) chưa được kiểm dưới tranh chấp đồng thời | `BookingConcurrencyIT.java:61-74` | Thêm test: 1 HV có N ghi danh với N GV khác nhau, N luồng đặt cùng giờ → 1×201, (N−1)×409 `SLOT_TAKEN`, tổng `reserved` = 1 |
| F2 | Thấp | IT đối chiếu bộ đếm dùng một seed cố định (`Random(42)`), chạy tuần tự, không gồm `CANCELLED_SYSTEM` và đổi lịch trong chuỗi | `SessionOutcomeIT.java:110-174` | Thêm nhánh reschedule và phát `StudentAnonymized`/`TeacherReassigned` vào chuỗi; cân nhắc lặp vài seed |
| F3 | Thấp | Slot DST mới test chiều tiến giờ (mất 1 giờ). Chiều lùi giờ (giờ lặp 25/10/2026 ở Berlin) chưa test, chưa có IT qua `/slots` với GV khác múi giờ | `SlotServiceTest.java:108-128` | Thêm case 25/10/2026 (02:00–03:00 lặp), khẳng định không sinh slot trùng hoặc lệch |
| F4 | Trung bình | "Suite chạy với `video=livekit`" chưa đạt: chỉ có unit `LiveKitVideoRoomProviderTest` (key giả, không gọi server). Không có IT `/join` trên DB với cả hai adapter; smoke chưa phát được token thật vì không có buổi trong khung | `LiveKitVideoRoomProviderTest.java`; `application-test.yml:5`; `JoinServiceTest.java` | Thêm IT `/join` (trong khung bằng `TestClock`) chạy 2 lần với `app.providers.video=fake` và `=livekit` (`@Nested` + `@TestPropertySource`), kiểm token decode được với `devkey/secret`. Trước go/no-go 1/11: thử tay 2 trình duyệt vào `ws://localhost:7880` |
| F5 | Thấp | Seed demo không có buổi `BOOKED` đã qua giờ, nên `outcomePending=true` trả rỗng cho admin. Lúc demo FE T078 sẽ không có dữ liệu "Buổi chưa chốt kết quả" | `scheduling/internal/seed/SchedulingDemoSeed.java` (T074) | Seed thêm 1–2 buổi `BOOKED` đã kết thúc (bộ đếm `reserved` khớp) cho student1/student2 |
| F6 | Thông tin | `GET /reports/teacher-sessions` có trong mục BE của U3 (unit-of-work.md:244, "Phát hành" dòng 254) nhưng tasks.md:293 xếp vào T121 (U8) và chưa có code. `SessionQueryApi` (phát hành cho module khác) chưa có test trực tiếp | `unit-of-work.md:244,254`; `tasks.md:293`; `scheduling/internal/session/SessionQueryApiImpl.java` | Thống nhất tài liệu: hoặc chuyển dòng report sang U8 trong unit-of-work.md, hoặc làm ở U3. Thêm IT ngắn cho `SessionQueryApi` khi module tiêu thụ đầu tiên (U4/U5) dùng tới |
| F7 | Thông tin | Khi đặt trùng slot, Hibernate ghi log mức WARN có chữ "ERROR: conflicting key value violates exclusion constraint "session_teacher_no_overlap"" (`SQLState: 23P01`). Đây là luồng nghiệp vụ bình thường (đã ánh xạ 409), nhưng sẽ làm nhiễu log khi demo/giám sát | log smoke (`org.hibernate.orm.jdbc.error`) | Hạ mức log `org.hibernate.orm.jdbc.error` xuống ERROR-only, hoặc kiểm slot trước khi insert; không bắt buộc |
| F8 | Thông tin | Phát lại `UserDeactivated` chưa có test idempotent riêng (2 event còn lại có). ShedLock giữa nhiều instance không được test (demo 1 instance). Chưa chạy CI/gitleaks. HV gọi `outcomePending=true` → 200 rỗng thay vì 403; chấp nhận được nếu contract cho phép | `SystemCancelIT.java:60-76` | Thêm 1 lần `scenario.publish` lại `UserDeactivated`; chạy CI khi có PR |

## 5. Verdict

**PASS_WITH_NOTES** — Backend `verify` BUILD SUCCESS với 54/0/0 unit và 67/0/0 IT Testcontainers, `spotless:check` PASS. FE lint (0 lỗi), prettier, vitest 3/0/0 và build đều pass. Smoke local với adapter LiveKit thật đạt 20/20 probe (đặt 201, trùng slot 409 `SLOT_TAKEN`, preview/hủy/đổi lịch đúng hạn, join 403/404) và bộ đếm trả về đúng giá trị ban đầu. Đa số DoD trọng yếu của U3 có test pass trên Postgres thật. Còn các mục chưa đủ: race đồng thời phía HV mới test tuần tự (F1); suite `video=livekit` và join trong khung với LiveKit thật chưa chạy (F4); go/no-go LiveKit là việc FE/thử tay ngày 1/11.

## Review độc lập (aidlc-reviewer)

Reviewer không viết code này. Đã đọc code `scheduling/**`, các thay đổi ở `catalog` và `shared/web`, V5, cấu hình và test. Không trùng F1–F8 ở trên.

### Đối chiếu constitution

| Nguyên tắc | Kết quả | Bằng chứng |
|---|---|---|
| I. Modular Monolith | Đạt | scheduling chỉ dùng `catalog.EnrollmentApi`, `identity.UserDirectory` và event công khai (`SchedulingAccess.java:3-5`, `SystemCancelListener.java:3-7`); không truy vấn bảng catalog/identity; `ModularityTests` pass |
| II. Ports & Adapters | Đạt | SDK LiveKit chỉ có ở `internal/video/LiveKitVideoRoomProvider.java:4-9`; có adapter fake |
| III. Free-First / YAGNI | Đạt | Chống trùng bằng exclusion constraint Postgres (`V5__scheduling.sql:61-66`); không thêm hạ tầng mới |
| IV. Kiểm thử trọng yếu | Đạt một phần | Đặt lịch, hủy, trừ/hoàn, job, listener đều có IT trên Postgres thật. Chưa có test cho nhánh sửa kết quả từ buổi GV đã hủy (R1) và cho race khi join (R3) |
| V. Bảo mật & dữ liệu | Đạt một phần | Phân quyền đúng: người ngoài buổi → 404 (`SessionLookup.java:20-22`, `JoinService.java:45-47`); GV khác hủy buổi → 404; `GET /sessions` ép `teacherId`/`studentId` theo vai trò (`SessionController.java:152-156`). Có migration mới V5. Audit khi sửa kết quả còn thiếu (R2, NFR-PRIV-04) |

### Kiểm các lựa chọn có chủ đích
- `isBookable` không xét buổi bận: chấp nhận được, vì exclusion constraint là hàng rào thật và trả `SLOT_TAKEN` đúng contract (`BookingService.java:87-93`).
- Điều kiện `WHERE` của exclusion constraint khớp `Session.OCCUPYING` (`Session.java:43-49` ↔ `V5:63,66`).
- Không có deadlock: mỗi transaction chỉ khóa 1 dòng enrollment (`EnrollmentApiImpl.java:121-122`) rồi mới insert session.
- Đổi lịch chỉ phát `SessionRescheduled`: đúng contract (`events-and-ports.md:27`). Bên nhận (notification) phải tra người tham gia qua `SessionQueryApi`.
- Join yêu cầu `BOOKED`: hợp lý. Hệ quả: nếu GV chốt kết quả giữa giờ học thì không ai vào lại phòng được (xem R7).
- Token: `validUntil = ends_at + 15'` (`JoinService.java:50,63`) và TTL tính đúng (`LiveKitVideoRoomProvider.java:50-55`), đạt NFR-SEC-08.
- Job reminder dùng `createdAt` (`Instant.now`) thay vì `Clock`: chấp nhận được (ghi chú ở R9).
- Seed tạo buổi trực tiếp, bỏ qua quy tắc đặt lịch: chấp nhận được vì bộ đếm vẫn đổi qua `EnrollmentApi` (ghi chú ở R9).

### Findings

| # | Mức | Vấn đề | Bằng chứng file:line | Tác động | Đề xuất |
|---|---|---|---|---|---|
| R1 | MAJOR | Admin "sửa kết quả" được cả buổi **GV đã hủy trước giờ học** (`TEACHER_ABSENT` do `cancelByTeacher`). Hàm sửa không kiểm `now ≥ startsAt` và không phân biệt buổi đã hủy với buổi đã chốt. Admin có thể đổi buổi chưa diễn ra sang `COMPLETED` → `consumeReleased` + `SessionFinished(COMPLETED)`. Nếu slot đã được đặt lại thì `flush` trong catalog ném 23P01 dạng `PersistenceException` chưa được dịch → 500 | `Session.java:187-204`; `Session.java:148-151`; `OutcomeService.java:53-56`; `EnrollmentApiImpl.java:121` | Trừ buổi không có thật (NFR-REL-02), phiếu đánh giá "ma" ở U6, lỗi 500 | Chỉ cho sửa khi `outcomeAt != null && cancelledBy == null` (tức trạng thái do chốt kết quả mà có) và `!now.isBefore(startsAt)`. Bắt 23P01 trong `OutcomeService` → `SLOT_TAKEN`/409. Viết test tái hiện trước khi sửa (constitution IV) |
| R2 | MAJOR | Sửa kết quả ghi đè `outcomeBy`/`outcomeAt`, làm mất kết quả cũ và người chốt cũ. `note` trong request bị bỏ qua. `decideInterruption` không ghi `outcomeAt` | `Session.java:197-199, 214-215`; `SessionController.java:106,191` | Không đạt NFR-PRIV-04 (Must): sửa trạng thái kết thúc phải lưu ai/khi nào/lý do | Lưu lịch sử sửa (cột `previous_status` + `outcome_note`, hoặc bảng `session_outcome_history` qua migration V5 nếu chưa merge, hoặc migration mới). Bắt buộc có `note` khi admin sửa; set `outcomeAt` trong `decideInterruption` |
| R3 | MINOR | GV và HV bấm join cùng lúc: cả hai cùng cập nhật một session có `@Version` → một người nhận 409 `CONCURRENT_MODIFICATION` | `JoinService.java:57`; `Session.java:105,220-226`; `GlobalExceptionHandler.java:46-52` | Lỗi khó hiểu ngay lúc mở phòng (dễ xảy ra lúc phòng vừa mở) | Ghi `*_joined_at` bằng `@Modifying` update có điều kiện `IS NULL` (không tăng version), hoặc retry 1 lần |
| R4 | MINOR | Job nhắc lịch cập nhật hàng loạt session có version trong **một** transaction. Nếu user hủy/đổi đúng lúc đó: user nhận 409, hoặc cả lô của job rollback | `SessionReminderJob.java:38-54` | 409 lẻ tẻ; nhắc lịch trễ 1 phút | Ghi cờ nhắc bằng update có điều kiện (`WHERE reminder_24h_at IS NULL`), không qua entity |
| R5 | MINOR | Ngày chuyển DST tiến giờ: điều kiện "slot nằm trong khung" tính theo phút giờ địa phương, nhưng `endsAt` tính theo Instant. Ví dụ GV rảnh tới 03:00, slot 01:30 + 60' kết thúc 03:30 CEST, vượt lịch rảnh 30'. Kiểm trùng ngày nghỉ cũng dùng giờ địa phương | `SlotGenerator.java:67-84,100-105` | Lệch NFR-UX-02 ở ca biên | Sau khi tính `endsAt`, đổi lại sang giờ địa phương rồi so với `rule.endTime` và các ngày nghỉ. Gộp vào test ở F3 |
| R6 | MINOR | Thêm ngày nghỉ và đặt buổi chạy song song: có khoảng hở giữa lúc kiểm tra và lúc lưu (không khóa), nên vẫn có thể có buổi `BOOKED` nằm trong ngày nghỉ | `AvailabilityService.java:126-144`; `BookingService.java:73` | Hiếm gặp; vi phạm FR-008 | Khóa theo GV (`pg_advisory_xact_lock(hash(teacher_id))`) ở cả hai luồng, hoặc chấp nhận và ghi vào runbook |
| R7 | MINOR | `unconsume` đưa `EXHAUSTED` về `ACTIVE` kể cả khi gói đã quá `expiresAt`. GV chốt kết quả ngay sau giờ bắt đầu thì phòng đóng với cả hai người | `Enrollment.java:151-153`; `JoinService.java:51` | Trạng thái gói hiển thị sai tới lần chạy job hết hạn; buổi học bị cắt ngang | Hết hạn → `EXPIRED`. Cân nhắc chỉ cho chốt kết quả sau `ends_at − x` hoặc vẫn cho join khi trạng thái là kết quả vừa chốt trong cửa sổ phòng |
| R8 | MINOR | `GET /sessions` không bắt buộc `from/to`, không giới hạn số dòng (admin lấy toàn bộ) | `SessionController.java:157`; `SessionRepository.java:42-57` | Rủi ro PERF khi dữ liệu tăng | Mặc định cửa sổ (vd. ±31 ngày) hoặc phân trang |
| R9 | NOTE | `OutcomeOverdueJob` `lockAtMostFor=PT10M` trong khi NFR-REL-07 ghi 5 phút. Khung phòng ±10'/15' chỉnh được qua cấu hình mà không ràng buộc với NFR-SEC-08. Listener hủy hệ thống thất bại (optimistic lock) chỉ chạy lại khi restart. Seed không phát `SessionFinished` nên U6 không có phiếu cho buổi seed. `createdAt` không theo `Clock` | `OutcomeOverdueJob.java:40`; `application.yml:84-85`; `SystemCancelListener.java:73-87`; `SchedulingDemoSeed.java:98-102` | Lệch tài liệu; dữ liệu demo U6 | Thống nhất 5 phút; thêm `@Validated` (giới hạn trên) cho các thông số phòng; theo dõi `event_publication` chưa xong (OBS-03); seed phiếu trực tiếp ở U6 |

### Verdict đề xuất của reviewer
**PASS_WITH_NOTES**: phần lõi đúng (chống trùng bằng exclusion constraint, bộ đếm theo đúng transaction, phân quyền 404/403, token đúng TTL, job/listener idempotent), nhưng nên sửa R1 (bộ đếm khi sửa kết quả) và R2 (audit NFR-PRIV-04) kèm test trước khi demo U6/U8.

## Re-verify sau sửa (2026-10-18, orchestrator quan sát)

`./mvnw -B verify` BUILD SUCCESS: unit **57/0/0**, IT **68/0/0**; `spotless:check` **PASS**.

| Finding | Trạng thái | Bằng chứng |
|---|---|---|
| R1 sửa kết quả buổi GV đã hủy / chưa diễn ra | ĐÃ SỬA | `Session.correctOutcome` yêu cầu `outcomeAt != null`, `cancelledBy == null`, `now ≥ startsAt`; `OutcomeService.applyAndFlush` dịch 23P01 → 409 `SLOT_TAKEN`; `SessionOutcomeTest.teacherCancelledSessionCannotBeCorrectedIntoAnOutcome`, `SessionOutcomeIT.teacherCancelledSessionIsNotCorrectable` |
| R2 lịch sử sửa kết quả + note | ĐÃ SỬA | bảng `session_outcome_change` (migration `V202610181200`), `GET /sessions/{id}/outcome-history` (A); `decideInterruption` ghi `outcomeAt`; `SessionOutcomeIT.onlySessionTeacherOrAdminRecordsOutcomeAfterStart` |
| R3 join đồng thời → 409 | ĐÃ SỬA | `SessionRepository.markTeacherJoined/markStudentJoined` (update có điều kiện, không tăng version); `JoinServiceTest` |
| R4 job xung đột version | ĐÃ SỬA | `markReminder24h/1h`, `markOutcomeOverdueNotified` có điều kiện; chỉ phát khi update = 1; `SchedulingJobsIT` |
| R5 slot DST vượt giờ kết thúc | ĐÃ SỬA | `SlotGenerator` kiểm giờ kết thúc theo đồng hồ địa phương; `SlotServiceTest.springForwardSlotsDoNotOverrunRuleEnd` |
| R7 `unconsume` trên gói quá hạn | ĐÃ SỬA | `Enrollment.unconsume(now)` → `EXPIRED` nếu quá hạn; `EnrollmentTest.unconsumeOnExpiredPackageStaysExpired` |
| R9 lockAtMostFor | ĐÃ SỬA | `OutcomeOverdueJob` PT5M |
| F5 seed không có buổi chưa chốt | ĐÃ SỬA | `SchedulingDemoSeed`: student1 thêm buổi hôm qua BOOKED (chỉ áp dụng DB seed mới) |
| F7 log WARN 23P01 | ĐÃ SỬA | `BookingService` kiểm trùng trước khi insert; constraint chỉ còn bắt lượt chen nhau |
| R6, R8, F1, F3, F6 | CHUYỂN U8 | unit-of-work.md "Việc chuyển từ verify U3" |
| F4 | CHUYỂN #2 | go/no-go LiveKit CN 1/11 |

Verdict sau sửa: **PASS_WITH_NOTES** (chờ người dùng duyệt).
