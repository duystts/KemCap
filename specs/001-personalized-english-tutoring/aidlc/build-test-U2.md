# Build & Test Report — U2 — Nền tảng đào tạo tiếng Anh 1 kèm 1 cá nhân hóa (001-personalized-english-tutoring)

- **Ngày (UTC)**: 2026-10-18, 07:33–07:37 UTC (giờ máy: 14:33–14:37 +07)
- **Unit**: U2 — Hồ sơ, gói học, ghi danh & thanh toán (B2), **chỉ phần BACKEND**. FE T052–T055, T114 thuộc #4, ngoài phạm vi.
- **Tasks trong phạm vi**: T038–T051, T104, T105, T107–T111 — hoàn thành: **21/21** (tất cả `[X]` trong tasks.md:127-143, 257-267)
  - T111 đã đổi thiết kế: bỏ listener `PaymentRecordedListener`, thay bằng `EnrollmentApi.activateOnFirstPayment` gọi đồng bộ từ `PaymentService` (tasks.md:267) để tránh vòng phụ thuộc catalog ↔ billing.
  - Ngoài phạm vi, chưa làm (đúng kế hoạch, thuộc U8): T106, T112, T113.
- **Môi trường**: Windows 11, Git Bash; JDK 21.0.12 (`JAVA_HOME=/c/Program Files/Java/jdk-21.0.12`); Docker Desktop (compose: postgres, mailpit, livekit đang chạy); Testcontainers `postgres:16`; Node + npm; `git config core.autocrlf=true`.

## 1. Build

| Thành phần | Lệnh | Kết quả | Ghi chú |
|------------|------|---------|---------|
| Backend (Maven, gồm Enforcer) | `cd backend && ./mvnw -B verify` | **PASS** (`BUILD SUCCESS`, exit 0, 01:23 min) | Enforcer: `RequireJavaVersion passed`, `BannedDependencies passed`. Jar `target/english-platform-0.0.1-SNAPSHOT.jar` (83 MB). Log có 0 dòng ` ERROR `. Cảnh báo không chặn: 2 repo metadata HTTP 401 (`flyway-community-db-support`, `MavenFeed`); Testcontainers `Reuse was requested but the environment does not support the reuse of containers`. |
| Backend format | `cd backend && ./mvnw -B spotless:check` | **FAIL** (`BUILD FAILURE`, 3.7 s) | `The following files had format violations: src\test\java\com\englishplatform\support\ApiTestSupport.java` (`@@ -1,106 +1,106 @@`, cả file). Nguyên nhân gần như chắc chắn là line ending: file này dùng LF (`file`: "UTF-8 text", 0 ký tự `\r`), các file khác như `PostgresContainerConfig.java` dùng CRLF, còn `core.autocrlf=true` nên Spotless đòi CRLF. File sửa lúc 14:29:52 +07, ngay trước lượt chạy này. Xem F1. QE không sửa. |
| Frontend lint | `cd frontend && npm run lint` (`oxlint src`) | **PASS** (exit 0; 0 lỗi, **10 cảnh báo**) | Đều là `react(only-export-components)`. So với U1 có thêm 1: `src/features/auth/AuthCard.tsx:32` (thêm lúc sửa R5 ở U1). |
| Frontend format | `cd frontend && npx prettier --check src` | **PASS** | "All matched files use Prettier code style!" |
| Frontend build | `cd frontend && npm run build` (`tsc -b && vite build`) | **PASS** (vite 8.3.4, 2093 module, 1.49 s) | Chunk chính `index-*.js` 496.88 kB (gzip 154.27 kB). |

## 2. Test

| Bộ test | Lệnh | Pass / Fail / Skip | Ghi chú |
|---------|------|--------------------|---------|
| Unit + Scenario (surefire) | `./mvnw -B verify` | **29 / 0 / 0** | `EnrollmentTest` 12/0/0; `PaymentServiceTest` 7/0/0; `BillingFlowTest` 2/0/0 (20.35 s, Testcontainers + Modulith `Scenario`); `ModularityTests` 2/0/0; `AuthorizationRulesTest` 1/0/0; `NotificationDispatchTest` 2/0/0; `SecurityPropertiesTest` 3/0/0. |
| Integration Testcontainers (failsafe) | `./mvnw -B verify` | **45 / 0 / 0** | `CatalogFlowIT` 8/0/0 (24.97 s); `EnrollmentApiIT` 6/0/0; `StudentAccessIT` 6/0/0; `AuthFlowIT` 21/0/0; `NotificationDispatchIT` 4/0/0. |
| Frontend unit (vitest) | `cd frontend && npm test -- --run` | **3 / 0 / 0** (1 file, 36.38 s) | Chỉ có `client.test.ts`, không đổi so với U1. |
| Smoke local (demo-seed) | `java -jar target/english-platform-0.0.1-SNAPSHOT.jar --spring.profiles.active=local,demo-seed` | **PASS 12 / 0 / 0** | Khởi động 12.0 s; Flyway `Successfully validated 5 migrations` (V1–V4, V8; DB đã ở bản mới nhất nên không migrate); log 0 dòng ` ERROR `. Đăng nhập admin, teacher1, teacher2, student1 với `Demo@1234` đều ra access token. Kết quả quan sát được: |
| | `GET /enrollments` (admin) | PASS | HTTP 200, 4 ghi danh `ACTIVE` (student1–4), có `priceVnd` |
| | `GET /enrollments` (teacher1) | PASS | HTTP 200, 2 ghi danh của teacher1, `priceVnd: null` |
| | `GET /enrollments` (student1) | PASS | HTTP 200, 1 ghi danh của chính mình, có `priceVnd: 7200000` |
| | `GET /enrollments` (không token) | PASS | HTTP 401 `code=UNAUTHENTICATED` |
| | `GET /students` (admin) | PASS | HTTP 200, 5 HV; student5 có `teacherIds: []` |
| | `GET /students` (teacher1) | PASS | HTTP 200, đúng 2 HV được phân công (student1, student2) |
| | `GET /students` (student1) | PASS | HTTP 403 `FORBIDDEN` |
| | `GET /students/{student4}/profile` (teacher1, không phân công) | PASS | HTTP 404 `NOT_FOUND` |
| | `GET /students/{student1}/profile` (teacher1, được phân công) | PASS | HTTP 200 |
| | `GET /enrollments/{id}/payments` (admin; student1 chủ ghi danh) | PASS | Cả hai HTTP 200 `amountDueVnd=7200000, amountPaidVnd=0, debtVnd=7200000, payments=[]`. Xem F4 (seed ACTIVE nhưng chưa có thanh toán). |
| | `GET /enrollments/{id khác}/payments` (student1) | PASS | HTTP 404 `NOT_FOUND` |
| | `GET /enrollments/{id}/payments` (teacher1) | PASS | HTTP 403 `FORBIDDEN` |
| | `DELETE /enrollments/{id}/payments` | **NOT RUN** | Bộ phân loại quyền chặn lệnh DELETE vì coi là thao tác phá hủy. Đã có IT `CatalogFlowIT.paymentsCannotBeEditedOrDeleted` (PUT/DELETE → 405) pass. Sau smoke, process java (PID 5288) đã bị dừng; `/actuator/health` trả 000. |
| CI GitHub Actions trên PR thật | `.github/workflows/ci.yml` | **NOT RUN** | Không chạy trong lượt này (chưa có PR). Lưu ý: job backend chạy `spotless:check` nên sẽ **đỏ** cho tới khi sửa F1. |
| gitleaks | — | **NOT RUN** | Không có trong danh sách lệnh của lượt này. |
| Ma trận phân quyền đầy đủ (`AuthorizationMatrixIT`, T140) | — | **NOT RUN (chưa có)** | Thuộc B7. U2 có các test phân quyền riêng lẻ (xem bảng DoD). |

### Đối chiếu DoD riêng của U2 (unit-of-work.md:214-219) và các mục chuyển từ U1

| Mục DoD | Trạng thái | Test và kết quả quan sát |
|---------|-----------|--------------------------|
| Trừ/hoàn buổi cho từng hàm `EnrollmentApi` (`reserve`, `release`, `consume`, `consumeReleased`) | **Đạt** | IT `EnrollmentApiIT.eachCounterOperationKeepsTotals` (đi qua cả 4 hàm, tới `EXHAUSTED`) pass; unit `EnrollmentTest.consumeMovesReservedToConsumedAndExhausts`, `releaseGivesSessionBack`, `consumeReleasedNeedsAFreeSession`, `consumeOrReleaseWithoutReservationIsInvalid`, `reserveUntilNoSessionsLeft` pass. `Propagation.MANDATORY`: `EnrollmentApiIT.counterChangesRequireCallerTransaction` pass. |
| 2 luồng `reserve` buổi cuối → đúng 1 thành công (NFR-REL-02) | **Đạt** | `EnrollmentApiIT.twoConcurrentReservationsOfLastSessionOnlyOneWins` pass (kết quả `[null, NO_SESSIONS_LEFT]`, reserved=1). CHECK DB: `databaseCheckRejectsOverbooking` pass. |
| Hết hạn gói chặn `reserve` | **Đạt (chỉ unit)** | `EnrollmentTest.expiredPackageCannotReserve` pass (`reserve(NOW+90d)` → `ENROLLMENT_NOT_ACTIVE`). Chưa có IT qua `EnrollmentApi` với `TestClock` (F3). |
| `PENDING_PAYMENT` chặn `reserve` | **Đạt** | `EnrollmentApiIT.pendingPaymentCannotReserve`, `EnrollmentTest.pendingPaymentCannotReserve` pass |
| Thanh toán + điều chỉnh → công nợ khớp | **Đạt** | `PaymentServiceTest.debtIsDueMinusPaidIncludingAdjustments`, `adjustmentMustBeNegativeWithNoteAndNotExceedPaid`, `amountMustBePositive`, `paidAtCannotBeInTheFuture` pass; `BillingFlowTest.firstPaymentActivatesAndLaterPaymentKeepsExpiry` kiểm `amountPaidVnd=1500000`, `debtVnd` qua API pass. Điều chỉnh chỉ ADMIN mới có ở `@PreAuthorize` (`BillingController.java:99`), chưa có test (F6). |
| Không có route sửa/xóa payment (NFR-REL-03) | **Đạt** | `CatalogFlowIT.paymentsCannotBeEditedOrDeleted` pass (PUT/DELETE → 405) |
| Thanh toán đầu kích hoạt, thanh toán sau không đổi hạn | **Đạt** | `BillingFlowTest.firstPaymentActivatesAndLaterPaymentKeepsExpiry` (`expires_at = activated_at + 90d`, có `EnrollmentActivated`; lần 2 `isFirstPayment=false`, `expiresAt` giữ nguyên) pass; `PaymentServiceTest.firstPaymentActivatesAndLaterPaymentsDoNot`, `laterPaymentDoesNotTouchActivation` pass. `EnrollmentCreated` → `receivable` (T109): `BillingFlowTest.enrollmentCreatedOpensReceivableOnce` pass, kể cả khi phát lại. |
| Sửa giá gói không đổi học phí ghi danh cũ | **Đạt** | `CatalogFlowIT.packagePriceChangeDoesNotAffectExistingEnrollment` pass (gói 9.000.000, ghi danh giữ 6.500.000; gói `INACTIVE` → 400 khi ghi danh mới) |
| Phân quyền: GV xem HV không phân công → 404 | **Đạt** | `StudentAccessIT.teacherCannotReadUnassignedStudent`, `teacherListsOnlyAssignedStudents`, `teacherEditsOnlyOwnTeacherProfile`, `onlyAdminEditsStudentProfileAndSeesNotes` pass; smoke: 404 và danh sách 2 HV |
| Phân quyền: HV chỉ đọc thanh toán của mình | **Đạt** | `BillingFlowTest.firstPaymentActivatesAndLaterPaymentKeepsExpiry` (HV khác → 404, GV → 403) pass; `StudentAccessIT.studentReadsOnlyOwnProfileAndCannotList` pass; smoke: 200 / 404 / 403 |
| `TEACHER_HAS_STUDENTS` | **Đạt** | `StudentAccessIT.deactivatingTeacherWithActiveEnrollmentIsRejected` pass (409 `TEACHER_HAS_STUDENTS`) |
| `@Version` → 409 | **Đạt một phần** | `EnrollmentApiIT.concurrentAdminEditsFailWithOptimisticLock` pass, nhưng chỉ khẳng định ném `ObjectOptimisticLockingFailureException`. Việc ánh xạ sang HTTP 409 `CONCURRENT_MODIFICATION` có trong code (`GlobalExceptionHandler.java:44-50`) nhưng chưa có test qua API (F2). |
| Listener ẩn danh hóa chạy 2 lần không lỗi | **Đạt** | `CatalogFlowIT.anonymizationListenerIsIdempotent` pass (phát `StudentAnonymized` 2 lần, `notes`/`occupation`/`selfReportedLevel` đều null) |
| Lọc HV/GV theo tên, trạng thái, GV (FR-007 rút gọn) | **Đạt một phần** | Mới test lọc `teacherId` (`StudentAccessIT.java:65`). Chưa có test cho `q=`, `status=` và `GET /users?role=&status=&q=` (F5). |
| (Từ U1) Quickstart bước 1–3 | **Đạt (IT); smoke một phần** | `CatalogFlowIT.quickstartSteps1To3` pass: tạo GV/HV → email mời (FakeEmailSender) → accept-invite → login → khóa/gói "Giao tiếp 12 buổi × 45 phút, 90 ngày" → ghi danh `PENDING_PAYMENT` → GV thấy 1 HV → thanh toán 50% → `ACTIVE`, còn 12 buổi, công nợ 1.500.000, GV không thấy giá. Smoke chỉ kiểm phần đọc trên dữ liệu seed; luồng tạo user và gửi email qua Mailpit **NOT RUN** ở smoke. |
| (Từ U1, R9) Ngừng kích hoạt gọi `revokeAll` | **Đạt** | `CatalogFlowIT.deactivationRevokesSessionsAndReactivationRestores` pass (INACTIVE → `/me` 401; kích hoạt lại rồi dùng refresh cũ → 401; admin tự ngừng chính mình → 400); `UserAdminService.java:117` gọi `authService.revokeAll`. `anonymizedUserStatusCannotChange` (409 `ANONYMIZED`) pass. |
| (Từ U1, R17) HV < 16 tuổi → `AWAITING_GUARDIAN_CONSENT`, không gửi mời | **Đạt** | `CatalogFlowIT.underageStudentWaitsForGuardianConsentWithoutInvite` pass (12 tuổi → `AWAITING_GUARDIAN_CONSENT`, không có `UserInvited`); `AuthFlowIT.underageStudentNeedsGuardianConsent` pass; `createUserValidation` (409 `EMAIL_TAKEN`, thiếu `dateOfBirth` → 400, GV gọi `/users` → 403) pass |
| Ranh giới module sau khi thêm catalog/billing | **Đạt** | `ModularityTests.verifiesModuleBoundaries` 2/0/0 |

## 3. Review độc lập (theo constitution)

QE chỉ rà ở mức bằng chứng build/test, không phải code review đầy đủ.

| Nguyên tắc | Kết quả (Đạt / Vi phạm / N/A) | Bằng chứng (file:dòng) |
|------------|-------------------------------|------------------------|
| I. Modular Monolith, `ApplicationModules.verify()` | Đạt (local) | `ModularityTests` pass sau khi thêm `catalog`, `billing`; T111 đổi sang gọi đồng bộ `EnrollmentApi.activateOnFirstPayment` để tránh vòng catalog ↔ billing (tasks.md:267) |
| II. Provider-Agnostic (Ports & Adapters) | N/A cho U2 | U2 không thêm provider ngoài; `FakeEmailSender` dùng trong `CatalogFlowIT` |
| III. Free-First & YAGNI | Đạt | Không thêm hạ tầng; không có API sửa giá ghi danh (`EnrollmentController.java:28`); `extend` để lại U8 |
| IV. Kiểm thử trọng yếu, Postgres thật, cấm H2, CI mỗi PR | Đạt một phần | Trừ/hoàn buổi, hết hạn gói, học phí/công nợ và phân quyền đều có test chạy trên `postgres:16` thật (74/0/0). Còn thiếu: test HTTP 409 cho `@Version` (F2), IT hết hạn gói (F3). `spotless:check` đỏ nên CI sẽ đỏ (F1). CI chưa chạy. |
| V. Bảo mật & dữ liệu cá nhân | Đạt (phạm vi U2) | Deny-by-default (smoke: không token → 401); `@PreAuthorize` ở `BillingController.java:72,85,99`; GV không thấy `priceVnd` (`EnrollmentController.java:41`); HV < 16 không gửi mời; ẩn danh hóa xóa dữ liệu hồ sơ; ngừng kích hoạt thu hồi refresh token |
| Ràng buộc công nghệ (Java 21, Boot 4.1, Flyway) | Đạt | Enforcer pass; migration `V4__catalog.sql`, `V8__billing.sql`; Flyway validate 5 migration khi khởi động |
| Quy trình & DoD (CI xanh + review + demo) | Chưa đạt | Format gate đỏ (F1); chưa có PR/CI; chưa có review độc lập cho U2 |

## 4. Phát hiện

| # | Mức độ | Mô tả | File | Đề xuất |
|---|--------|-------|------|---------|
| F1 | **Cao** | `./mvnw -B spotless:check` FAIL ở 1 file. Diff trọn cả 106 dòng, file dùng LF trong khi `core.autocrlf=true` (các file khác CRLF), nên gần như chắc chắn chỉ là line ending. Job backend trong CI chạy `spotless:check` nên sẽ đỏ, trái quy tắc "PR có test đỏ MUST NOT merge" | `backend/src/test/java/com/englishplatform/support/ApiTestSupport.java` | Chủ code chạy `./mvnw spotless:apply` (hoặc lưu lại file với CRLF), chạy lại `spotless:check`. Lâu dài: đặt `<lineEndings>UNIX</lineEndings>` cho Spotless, hoặc `*.java text eol=lf` trong `.gitattributes`, để không lệ thuộc `autocrlf` |
| F2 | Trung bình | DoD "`@Version` → 409" mới chỉ được test ở mức exception (`ObjectOptimisticLockingFailureException`), chưa có test HTTP 409 `CONCURRENT_MODIFICATION` qua API | `EnrollmentApiIT.java:132-154`; `GlobalExceptionHandler.java:44-50` | Thêm IT gửi `version` cũ tới endpoint có `version` (`EnrollmentController.java:56`), hoặc test trực tiếp handler, rồi khẳng định 409 + `code` |
| F3 | Thấp | "Hết hạn gói chặn `reserve`" mới có unit test trên entity, chưa có IT qua `EnrollmentApi` + `TestClock` | `EnrollmentTest.java:45-48` | Thêm IT: kích hoạt → `clock.advance(validityDays + 1)` → `reserve` → `ENROLLMENT_NOT_ACTIVE` |
| F4 | Thấp | Seed demo: 4 ghi danh `ACTIVE` nhưng công nợ `amountPaidVnd=0`, `debtVnd` bằng toàn bộ học phí (smoke). Trái quy tắc "thanh toán đầu mới kích hoạt", nên lúc demo màn hình thanh toán của HV sẽ trông mâu thuẫn | `catalog/internal/seed/CatalogDemoSeed.java` (T051) | Seed thêm 1 thanh toán cho mỗi ghi danh `ACTIVE` (qua `PaymentService`), hoặc ghi rõ đây là dữ liệu demo |
| F5 | Thấp | Lọc FR-007 mới test tham số `teacherId`; `q`, `status` của `GET /students` và `GET /users?role=&status=&q=` chưa có test | `StudentAccessIT.java:65` | Thêm IT ngắn cho từng tham số lọc |
| F6 | Thấp | "Điều chỉnh âm chỉ ADMIN" (T104) chỉ dựa vào `@PreAuthorize`, chưa có test HV/GV gọi `POST /payments/adjustments` → 403. `AuthorizationRulesTest` chỉ kiểm có annotation | `BillingController.java:99`; `PaymentServiceTest.java` | Thêm 1 IT 403 cho `POST /payments` và `/adjustments` với vai trò STUDENT/TEACHER |
| F7 | Thông tin | Với GV, response trả `"priceVnd": null` thay vì bỏ hẳn field. IT `quickstartSteps1To3` dùng `doesNotExist()` nên vẫn pass với null. Không lộ dữ liệu | `EnrollmentController.java:41-52` | Không bắt buộc; có thể dùng `@JsonInclude(NON_NULL)` nếu contract muốn bỏ field |
| F8 | Thông tin | Chưa chạy CI/gitleaks; smoke chưa kiểm luồng tạo user và email mời qua Mailpit (đã có IT); probe DELETE payment bị bộ phân loại quyền chặn (đã có IT 405). FE lint tăng từ 9 lên 10 cảnh báo (`AuthCard.tsx:32`), không chặn build | `.github/workflows/ci.yml`; `frontend/src/features/auth/AuthCard.tsx:32` | Chạy CI trên PR khi có remote; nếu cần, smoke tay `POST /users` → Mailpit |

## 5. Verdict

**FAIL** (chỉ vì gate format; mọi test đều xanh) — Backend `verify` BUILD SUCCESS với 29/0/0 unit và 45/0/0 IT Testcontainers. FE lint (0 lỗi), prettier, vitest 3/0/0 và build đều pass. Smoke local đạt 12/12. Các DoD trọng yếu của U2, cùng quickstart 1–3, R9 và R17, đều có test pass, trừ 2 mục mới đạt một phần (`@Version` → 409 chưa test ở mức HTTP, lọc FR-007 chưa test đủ). Tuy vậy `./mvnw -B spotless:check` FAIL ở `ApiTestSupport.java` (gần như chắc chắn do line ending) và sẽ làm CI đỏ. Sửa F1 rồi chạy lại `spotless:check` xanh là có thể nâng lên **PASS_WITH_NOTES**.

## Review độc lập (aidlc-reviewer)

- **Ngày**: 2026-10-18. Người review không viết code U2, chỉ đọc. Phạm vi: BE catalog, billing, identity user-admin, V4/V8, test. F1–F8 ở trên không lặp lại; mã R* bên dưới là của reviewer.

### Constitution check

| Nguyên tắc | Kết quả | Bằng chứng / nhận xét |
|------------|---------|------------------------|
| I. Modular Monolith | Đạt | billing → catalog chỉ qua `EnrollmentApi`/`EnrollmentView` và event `EnrollmentCreated` (`PaymentService.java:3-6`, `EnrollmentCreatedListener.java:3`). V8 không có FK sang bảng `enrollment` (`V8__billing.sql:3-11`). Sai lệch (1) (gọi đồng bộ `activateOnFirstPayment` thay listener T111) **hợp lý**: billing đã phụ thuộc catalog, nên một listener ngược chiều sẽ tạo vòng. Thay đổi này đã ghi vào `events-and-ports.md:24,48,53-55`. Còn sót `data-model.md:186` (`--PaymentRecorded--> ACTIVE`), xem R9 |
| II. Ports & Adapters | N/A | U2 không gọi provider ngoài |
| III. YAGNI | Đạt | Không thêm hạ tầng. Sai lệch (3) (trả mảng, chưa phân trang) đã ghi ở `rest-api.md:65` và chấp nhận được với quy mô demo. Sai lệch (5) (`needsConsulting` được nhận nhưng bỏ qua) có ghi chú ở `ProfileController.java:108`, chấp nhận được tới U4 |
| IV. Test trọng yếu | Đạt một phần | Bộ đếm + khóa + CHECK chạy trên Postgres thật (`EnrollmentApiIT.java:44-129`). Còn thiếu: IT đối chiếu công nợ có điều chỉnh (R6), test 2 thanh toán đồng thời (R6), cộng thêm F2/F3/F5/F6 |
| V. Bảo mật & dữ liệu cá nhân | Đạt, có ghi chú | 404 khi không được phân công (`CatalogAccess.java:37-58`). Ẩn giá ghi danh với GV (`EnrollmentController.java:59,70`). Chỉ admin thấy `notes` (`ProfileController.java:97`). Ẩn danh hóa idempotent (`AnonymizationListener.java:21-24`). Không có xóa cứng. Ghi chú: giá vẫn lộ qua `/packages` (R4), lý do hủy ghi danh không được lưu (R7). Sai lệch (4) Flyway `out-of-order` không vi phạm "không sửa migration đã merge" (R8) |
| Ràng buộc công nghệ / quy trình | Theo F1 | Gate format đỏ (F1). Chưa có PR review |

### Findings

| # | Mức | Vấn đề | Bằng chứng file:line | Tác động | Đề xuất |
|---|-----|--------|----------------------|----------|---------|
| R1 | MAJOR | `receivable` được tạo **bất đồng bộ** sau commit (`@ApplicationModuleListener`). Nếu ghi thanh toán hoặc xem công nợ ngay sau khi tạo ghi danh thì nhận 404 "chưa sẵn sàng, thử lại sau". Chính test cũng phải `await` thì mới chạy được | `EnrollmentCreatedListener.java:17-21`; `PaymentService.java:133-139,120-124`; `BillingFlowTest.java:72` | Luồng quickstart bước 3 trên FE (tạo ghi danh → ghi thanh toán ngay) có thể lỗi ngẫu nhiên. Nếu listener lỗi hẳn, ghi danh không ghi thu được cho tới khi khởi động lại (`republish-outstanding-events-on-restart`). Mã 404 cũng sai nghĩa (tài nguyên có tồn tại) | Tạo receivable **trong cùng transaction**: billing nghe bằng `@EventListener`/`@TransactionalEventListener(BEFORE_COMMIT)` (không tạo vòng vì billing vốn đã phụ thuộc catalog). Hoặc `lockReceivable` tự tạo khi thiếu, dùng học phí lấy qua `EnrollmentApi`. Thêm IT: tạo ghi danh rồi `POST /payments` ngay, không `await` |
| R2 | MINOR | `lockReceivable` gọi `enrollmentApi.get()` (đọc không khóa, entity vào persistence context) **trước** `findForUpdate`. Hibernate dùng lại instance đã managed thay vì đọc lại dữ liệu sau khi khóa, nên nếu có tranh chấp thì lỗi trả ra là optimistic-lock/409 thay vì lỗi nghiệp vụ đúng. Scheduling (U3) rất có thể theo mẫu `get` → `reserve` và gặp đúng vấn đề này: lượt thua khi giành buổi cuối nhận `CONCURRENT_MODIFICATION` thay vì `NO_SESSIONS_LEFT` | `PaymentService.java:129`; `EnrollmentApiImpl.java:89-94`; IT đồng thời chỉ gọi `reserve` không qua `get` (`EnrollmentApiIT.java:57`) | Không vượt số buổi (vì có `@Version` + CHECK), nhưng mã lỗi sai và FE hiển thị sai | Trong `update()` dùng `entityManager.refresh(e, PESSIMISTIC_WRITE)`, hoặc cho `get()` trả projection/DTO không đưa entity vào persistence context. Thêm IT 2 luồng `get` + `reserve` buổi cuối, kỳ vọng `NO_SESSIONS_LEFT` |
| R3 | MINOR | `extend()` đưa `EXPIRED` → `ACTIVE` mà không kiểm GV còn `ACTIVE`. Guard ngừng kích hoạt GV chỉ chặn khi còn ghi danh `ACTIVE`/`PENDING_PAYMENT` | `Enrollment.java:164-175`; `EnrollmentService.java:124-128`; `TeacherDeactivationGuard.java:27-29` | Có thể có ghi danh `ACTIVE` gắn với GV `INACTIVE`, lỗi này lộ ra ở U3 khi đặt lịch | Trong `extend` gọi `userDirectory.requireActive(teacherId, TEACHER)` (hoặc buộc đổi GV trước) |
| R4 | MINOR | GV gọi `GET /packages` thấy `priceVnd`, lại thấy `packageId` trên ghi danh, nên suy ra được học phí khi không có giảm giá | `PackageController.java:79-84,58`; `EnrollmentController.java:64` | Việc ẩn giá ghi danh với GV (`EnrollmentController.java:70`) bị vòng qua | Ẩn `priceVnd` của gói khi người xem là TEACHER (giống `EnrollmentDto`), hoặc ghi rõ trong contract rằng giá gói là công khai |
| R5 | MINOR | `adjust` bị chặn khi ghi danh `CANCELLED` | `PaymentService.java:97,130-132` | Không sửa được khoản nhập nhầm trên ghi danh đã hủy, công nợ sai mà không có cách sửa | Chỉ chặn `record` cho `CANCELLED`, cho `adjust` đi qua |
| R6 | MINOR | NFR-REL-03 "công nợ khớp tổng payment kể cả điều chỉnh" chỉ có unit test dùng mock. Không có IT nào trên DB thật cho chuỗi thanh toán + điều chỉnh, cũng không có test 2 thanh toán đầu đồng thời (đúng 1 `isFirstPayment=true`) | `PaymentServiceTest.java:6-9,29`; `BillingFlowTest.java:75-108` (không có điều chỉnh) | Hai quy tắc tiền quan trọng chưa có kiểm chứng tích hợp, trái tinh thần nguyên tắc IV | Thêm IT: 2 payment + 1 adjustment, rồi so `amount_paid_vnd = SUM(payment.amount_vnd)` bằng SQL. Thêm IT 2 luồng `record` đồng thời |
| R7 | MINOR | `POST /enrollments/{id}/cancel` nhận `reason` nhưng bỏ đi, không lưu ai hủy, lúc nào, vì sao | `EnrollmentController.java:97,149-150`; `Enrollment.java:178-187` | Thiếu dấu vết kiểm toán cho một thay đổi ảnh hưởng tiền và buổi học (tinh thần NFR-PRIV-04) | Lưu `cancelled_by/at/reason` (cần migration mới) hoặc bỏ `reason` khỏi contract |
| R8 | NOTE | Sai lệch (4) `flyway.out-of-order: true` tiện cho việc đánh số migration trước theo module, nhưng nếu module chạy sau mà phụ thuộc thứ tự thì có thể áp sai thứ tự trên môi trường đã migrate | `application.yml:16-18` | Rủi ro thấp khi demo | Quy ước: migration không tham chiếu bảng của module khác. Cân nhắc tắt `out-of-order` ở profile prod sau MVP |
| R9 | NOTE | Sai lệch (2) `CoursePackage` thay vì `Package`: chấp nhận, vì `Package` trùng `java.lang.Package` và bảng vẫn tên `package`. Sai lệch (6) seed ACTIVE không qua thanh toán: chấp nhận cho demo (xem F4), dù không phát `EnrollmentActivated` (`CatalogDemoSeed.java:84`). `data-model.md:186` còn mô tả cơ chế kích hoạt cũ | `CatalogDemoSeed.java:81-85`; `data-model.md:186` | Tài liệu lệch với code | Sửa `data-model.md:186` thành `--activateOnFirstPayment-->` |
| R10 | NOTE | Thứ tự khóa trong `record`: `receivable` trước, `enrollment` sau. Hiện chưa có luồng nào khóa theo chiều ngược lại | `PaymentService.java:67,82` | Có thể deadlock nếu sau này (U8 hoàn tiền/hủy) có luồng khóa ngược chiều | Ghi quy ước thứ tự khóa vào `events-and-ports.md` |

Kiểm tra đạt (không có phát hiện): `MANDATORY` + `PESSIMISTIC_WRITE` + `@Version` + CHECK DB trên mọi hàm bộ đếm (`EnrollmentApiImpl.java:44-80`, `EnrollmentRepository.java:20-22`, `V4__catalog.sql:74`). `first` được tính sau khi đã khóa receivable, nên 2 thanh toán đầu bị tuần tự hóa (`PaymentService.java:67-68`). Điều chỉnh âm có kiểm không để tổng đã thu xuống dưới 0 khi đang khóa (`PaymentService.java:97-100`). Giá ghi danh chỉ nhập lúc tạo (`EnrollmentService.java:67`). Listener công nợ idempotent (`EnrollmentCreatedListener.java:19`). `/users` chỉ cho ADMIN (`UserAdminController.java:32`). Ngừng kích hoạt user thì thu hồi token và hỏi guard (`UserAdminService.java:115-118`).

### Verdict đề xuất của reviewer

**PASS_WITH_NOTES** (với điều kiện F1 đã sửa; nếu F1 chưa sửa thì giữ **FAIL** như QE). Không có lỗi BLOCKING về đếm buổi, tiền hay phân quyền, nhưng R1 (công nợ tạo bất đồng bộ làm thanh toán ngay sau ghi danh bị 404) nên được sửa trước khi FE #4 nối luồng ghi danh → thanh toán.

## Re-verify sau sửa (2026-10-18, orchestrator quan sát)

`./mvnw -B verify` BUILD SUCCESS: unit **30/0/0**, IT **47/0/0**; `./mvnw -B spotless:check` **PASS**.

| Finding | Trạng thái | Bằng chứng |
|---|---|---|
| F1 spotless | ĐÃ SỬA | `spotless:apply` → `spotless:check` PASS |
| R1 receivable bất đồng bộ | ĐÃ SỬA | `EnrollmentCreatedListener` dùng `@TransactionalEventListener(BEFORE_COMMIT)`; `CatalogFlowIT.quickstartSteps1To3` và `BillingFlowTest` ghi thanh toán ngay, không `await` |
| R2 đọc cũ trước khi khóa | ĐÃ SỬA | `EnrollmentApiImpl.update` = `find` + `refresh(PESSIMISTIC_WRITE)`; `EnrollmentApiIT.staleReadInCallerTransactionStillGetsNoSessionsLeft` |
| R3 gia hạn với GV đã ngừng | ĐÃ SỬA | `EnrollmentService.extend` kiểm GV ACTIVE; `CatalogFlowIT.extendRequiresActiveTeacherAndTeacherDoesNotSeePrices` |
| R4 GV thấy giá gói | ĐÃ SỬA | `PackageController` trả `priceVnd = null` cho GV; cùng test trên |
| F2 `@Version` → 409 | ĐÃ SỬA | `GlobalExceptionHandlerTest.optimisticLockMapsTo409` + `EnrollmentApiIT.concurrentAdminEditsFailWithOptimisticLock` |
| F6 chỉ admin ghi/điều chỉnh | ĐÃ SỬA | `BillingFlowTest`: GV/HV → 403 |
| data-model.md:186 | ĐÃ SỬA | mô tả `activateOnFirstPayment` + receivable cùng transaction |
| R5–R7, F3, F5 | CHUYỂN U8 | unit-of-work.md "Việc chuyển từ verify U2" |
| R8, R9 (NOTE) | CHẤP NHẬN | flyway out-of-order; seed kích hoạt không thanh toán (chỉ profile demo-seed) |

Verdict sau sửa: **PASS_WITH_NOTES** (chờ người dùng duyệt).
