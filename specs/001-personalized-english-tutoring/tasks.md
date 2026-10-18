---
description: "Danh sách task cho MVP nền tảng đào tạo tiếng Anh 1 kèm 1 (sinh lại 2026-10-18)"
---

# Tasks: Nền tảng đào tạo tiếng Anh 1 kèm 1 cá nhân hóa (MVP)

**Input**: Design documents from `/specs/001-personalized-english-tutoring/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md),
[aidlc/unit-of-work.md](./aidlc/unit-of-work.md), [aidlc/stories.md](./aidlc/stories.md), [aidlc/nfr.md](./aidlc/nfr.md)

**Sinh lại 2026-10-18** sau clarify (Session 2026-10-18), cập nhật plan và cổng duyệt units (phương án S2).
Bản cũ (T001–T128, giả định tuần 1 = 12–18/10, mỗi người một module) **không còn hiệu lực**.

**Tests**: Có. Constitution IV bắt buộc test tự động cho: đặt lịch, chống trùng, hủy/đổi, trừ/hoàn buổi,
hết hạn gói, học phí/công nợ, phân quyền. Thêm: ẩn danh hóa giữ nguyên số liệu (SC-007). CRUD đơn giản
và UI không bắt buộc test riêng. Integration test dùng PostgreSQL thật (Testcontainers), cấm H2.

**Organization**: Phase theo user story (thứ tự ưu tiên spec). Thứ tự **thực hiện** theo bolt/unit
ở mục *Implementation Strategy* — vì backend chỉ có một người (#1) làm nối tiếp.

## Format: `[ID] [P?] [Story] (Người·Unit) Mô tả`

- **[P]**: làm song song được (khác file, không phụ thuộc task chưa xong)
- **[Story]**: US1…US6 theo spec.md
- **(Người·Unit)**: người phụ trách và unit trong `aidlc/unit-of-work.md`
  - **#1** (leader): toàn bộ backend, trừ ngoại lệ D1 · hạ tầng, CI, deploy, sao lưu
  - **#2**: frontend app shell, auth, consent gate, `features/scheduling`, `features/classroom`; k6, Playwright (P2)
  - **#3**: frontend `features/learning`, `features/notifications`; **D1**: backend U4 (learning profile/path)
  - **#4**: frontend `features/catalog`, `features/billing`, `features/admin` (gồm users, privacy); **D1**: backend billing core (U2) + U8

## Path Conventions

- `B/` = `backend/src/main/java/com/englishplatform/` · `BT/` = `backend/src/test/java/com/englishplatform/`
- `MIG/` = `backend/src/main/resources/db/migration/` · `F/` = `frontend/src/` · `TPL/` = `backend/src/main/resources/templates/email/`
- Class trong `<module>/internal/` không được module khác dùng; event, public API, port, SPI đặt ở package gốc `B/<module>/`.
- **Đánh số migration**: V1–V9 đã gán sẵn trong file này (mỗi số đúng một task). Migration phát sinh sau dùng
  `V{yyyyMMddHHmm}__{module}_{mô_tả}.sql` để bốn người không trùng số. Không sửa migration đã merge.

## Cắt giảm đã duyệt (không tạo task)

- **K1**: báo cáo chất lượng giảng dạy FR-023 (`/reports/teacher-quality`, tỷ lệ hủy/vắng) → GĐ2. Giữ đánh giá sao (FR-022) và số buổi đã dạy (FR-027).
- **K2**: nộp bài chỉ dạng văn bản → không làm upload tệp, pre-signed URL, `FileStorage`/S3, MinIO. Các mục này trong plan/contracts áp dụng từ GĐ2.
- **K3**: hoãn NFR-SEC-06 (rate limit, bảng `auth_rate_limit`), NFR-OBS-02 (log JSON + `traceId`; vẫn giữ log rotation), NFR-OBS-03 (runbook SQL), NFR-MAINT-05.
- [GĐ2] khác: mẫu lộ trình, ghi âm, dạy thay, tỷ lệ chuyên cần/nộp bài (FR-021), tỷ lệ đúng giờ, tiến bộ trung bình.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Khởi tạo repo, backend, frontend, hạ tầng local, CI — U1, B1 (18–19/10)

- [X] T001 (#1·U1) Khởi tạo monorepo: `git init`, nhánh `main`, thư mục `backend/`, `frontend/`, `deploy/`, `perf/`; `.gitignore` (Java, Node, `.env`), `.editorconfig`, `README.md` tối thiểu ở thư mục gốc
- [X] T002 (#1·U1) Tạo project Maven Spring Boot **4.1.x** trong `backend/pom.xml` (Java 21, groupId `com.englishplatform`, artifactId `english-platform`, Maven Wrapper) với dependency: web, validation, data-jpa, security, oauth2-resource-server, flyway-core + flyway-database-postgresql, postgresql, mail, thymeleaf, actuator, `spring-modulith-starter-core`, `spring-modulith-starter-jpa`, `spring-modulith-starter-test` (test), testcontainers-postgresql (test), springdoc-openapi-starter-webmvc-ui, shedlock-spring, shedlock-provider-jdbc-template, `io.livekit:livekit-server`; phiên bản Spring lấy theo BOM; **Maven Enforcer `bannedDependencies` cấm `com.h2database:h2`**; tạo `B/EnglishPlatformApplication.java`
- [X] T003 [P] (#1·U1) Kiểm tra tương thích Spring Boot 4.1 của springdoc-openapi, ShedLock, `io.livekit:livekit-server`; ghi phiên bản chọn và vấn đề gặp vào mục R1 của `specs/001-personalized-english-tutoring/research.md`; thư viện không tương thích → đề xuất thay thế, không hạ phiên bản Boot
- [X] T004 [P] (#2·U1) Tạo frontend Vite React TypeScript trong `frontend/`: Tailwind CSS, shadcn/ui (init), react-router, @tanstack/react-query, react-hook-form, zod, @hookform/resolvers, recharts, @fullcalendar/react + daygrid + timegrid + interaction, @livekit/components-react, livekit-client, msw (dev), openapi-typescript (dev), vitest + @testing-library/react (dev); script `dev`, `build`, `lint`, `test`, `gen:api` trong `frontend/package.json`
- [X] T005 [P] (#1·U1) `docker-compose.yml` ở thư mục gốc: `postgres:16` (db `english`, user `app`), `axllent/mailpit` (SMTP 1025, UI 8025), `livekit/livekit-server --dev` (7880); **không** có MinIO (K2); cổng khớp `quickstart.md`
- [X] T006 [P] (#2·U1) ESLint + Prettier trong `frontend/eslint.config.js`, `frontend/.prettierrc`; Spotless (google-java-format) trong `backend/pom.xml` (#1 review)
- [X] T007 [P] (#1·U1) CI `.github/workflows/ci.yml` trên mọi PR vào `main`: job backend (`./mvnw -B verify`, Testcontainers dùng Docker có sẵn của runner), job frontend (`npm ci`, `npm run lint`, `npm run test -- --run`, `npm run build`), job `gitleaks/gitleaks-action` quét secret
- [X] T008 [P] (#1·U1) `backend/src/main/resources/application.yml` + `application-local.yml`: `spring.datasource`, `spring.jpa.open-in-view=false`, `spring.modulith.events.republish-outstanding-events-on-restart=true`, `app.security.jwt-secret`, `app.frontend-url`, `app.providers.{video,email}`, `app.scheduling.*` (`cancel-cutoff-hours: 12`, `min-booking-lead-hours: 2`, `booking-horizon-days: 30`, `room-open-before-minutes: 10`, `room-close-after-minutes: 15`), `app.privacy.{policy-version, guardian-consent-under-age: 16}`, `app.reminders.*` (`session-windows: [24h, 1h]`, `report-overdue-hours: 24`, `outcome-overdue-hours: 24`, `renewal-low-sessions: 2`, `renewal-expiring-days: 7`) đúng `contracts/events-and-ports.md`; `deploy/.env.example` liệt kê mọi biến (không có giá trị thật)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Hạ tầng chung, đăng nhập, email tối thiểu, hợp đồng giữa module (stub), khung frontend + mock — U1, B1 (18–23/10)

**⚠️ CRITICAL**: Frontend bắt đầu ngay trên mock MSW (D4); backend các story chỉ làm sau phase này.

### Backend nền tảng

- [X] T009 (#1·U1) Migration `MIG/V1__shared_infra.sql`: `CREATE EXTENSION IF NOT EXISTS btree_gist;` bảng `shedlock` (schema ShedLock JDBC) và `event_publication` (schema Spring Modulith JPA của phiên bản trong BOM)
- [X] T010 (#1·U1) `B/shared/package-info.java` với `@ApplicationModule(type = ApplicationModule.Type.OPEN)`; `@EnableScheduling` + `@EnableSchedulerLock(defaultLockAtMostFor = "PT5M")` trong `B/shared/config/SchedulingConfig.java` (bean `LockProvider` JDBC, `usingDbTime()`)
- [X] T011 [P] (#1·U1) Lỗi chung `B/shared/web/GlobalExceptionHandler.java` + `B/shared/web/BusinessException.java` (`code`, `HttpStatus`, thông điệp tiếng Việt): RFC 9457 `ProblemDetail` có `code` và `errors[{field, message}]`; `MethodArgumentNotValidException` → 400 `VALIDATION_FAILED`, `ObjectOptimisticLockingFailureException` → 409 `CONCURRENT_MODIFICATION`, `AccessDeniedException` → 403 `FORBIDDEN`; đủ mã trong bảng cuối `contracts/rest-api.md`
- [X] T012 [P] (#1·U1) Bean `Clock` (UTC) `B/shared/config/ClockConfig.java`; mọi service lấy thời gian qua `Clock`
- [X] T013 [P] (#1·U1) Test ranh giới `BT/ModularityTests.java`: `ApplicationModules.of(EnglishPlatformApplication.class).verify()` + `Documenter` sinh tài liệu vào `backend/target/spring-modulith-docs`
- [X] T014 [P] (#1·U1) Hạ tầng test `BT/support/PostgresContainerConfig.java` (`@ServiceConnection` `postgres:16`, tái sử dụng container) và `BT/support/TestClockConfig.java` (Clock điều khiển được)

### Identity & bảo mật

- [X] T015 (#1·U1) Migration `MIG/V2__identity.sql`: `user_account` (email unique qua index `lower(email)`; `role` ∈ `ADMIN`,`TEACHER`,`STUDENT`; `status` ∈ `AWAITING_GUARDIAN_CONSENT`,`INVITED`,`ACTIVE`,`INACTIVE`; `timezone` mặc định `'Asia/Ho_Chi_Minh'`; `password_hash` nullable; `date_of_birth date` với `CHECK ((role = 'STUDENT') = (date_of_birth IS NOT NULL))` — "bắt buộc khi role = STUDENT, null với role khác"; `anonymized_at`), `auth_token` (`type` ∈ `INVITE`,`PASSWORD_RESET`,`REFRESH`; `token_hash` unique; `expires_at`; `used_at`), `policy_consent` (unique `(user_id, policy_version)`, `policy_version varchar(20)`), `guardian_consent` (`student_id` unique; `guardian_name varchar(150)`, `relationship varchar(50)`, `guardian_contact varchar(255)` NOT NULL; `consented_on`; `recorded_by`), `data_subject_request` (`type` ∈ `ERASURE`,`ACCESS`; `status` ∈ `RECEIVED`,`COMPLETED`,`REJECTED`; `requester_name varchar(150)` NOT NULL; `resolution_note` bắt buộc khi `REJECTED` qua CHECK) theo `data-model.md`; **không** tạo `auth_rate_limit` (K3)
- [X] T016 [P] (#1·U1) Entity + repository `B/identity/internal/UserAccount.java`, `AuthToken.java`, `UserAccountRepository.java`, `AuthTokenRepository.java`
- [X] T017 (#1·U1) `B/shared/security/SecurityConfig.java`: stateless, OAuth2 resource server JWT HS256 (`app.security.jwt-secret`), **từ chối khởi động nếu secret < 256 bit (32 byte)** (NFR-SEC-05), `JwtAuthenticationConverter` map claim `role` → `ROLE_*`, CORS cho `app.frontend-url` (allowCredentials), public: `/api/v1/auth/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/actuator/health`; `@EnableMethodSecurity`; helper `B/shared/security/CurrentUser.java` (userId, role, timezone từ JWT)
- [X] T018 (#1·U1) `B/identity/internal/AuthService.java`: login (BCrypt, chỉ `ACTIVE`, cập nhật `last_login_at`); access token 15 phút (claims `sub`, `role`, `name`, `tz`) bằng `NimbusJwtEncoder`; refresh token 32 byte ngẫu nhiên, lưu SHA-256, hạn 7 ngày, xoay vòng, thu hồi khi logout hoặc `INACTIVE`; cookie `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth`
- [X] T019 (#1·U1) `B/identity/internal/InvitationService.java`, `PasswordResetService.java`: INVITE hạn **72h**, PASSWORD_RESET hạn **30 phút**, dùng một lần; mật khẩu "tối thiểu 8 ký tự, có chữ và số"; `forgot-password` luôn 202; `accept-invite` bắt buộc `acceptedPolicyVersion` = phiên bản hiện hành (ghi `policy_consent`) và nếu học viên < `guardian-consent-under-age` tuổi tại thời điểm kích hoạt mà chưa có `guardian_consent` → 409 `GUARDIAN_CONSENT_REQUIRED`; phát `UserInvited`, `PasswordResetRequested` (record `B/identity/UserInvited.java`, `B/identity/PasswordResetRequested.java`)
- [X] T020 (#1·U1) Public API `B/identity/UserDirectory.java` (`find(userId)` → `UserView(id, email, fullName, role, status, timezone)`, `requireActive(userId, role)`, `findAdmins()`) + `B/identity/internal/UserDirectoryImpl.java`; SPI `B/identity/UserDeactivationGuard.java` (`void check(UUID userId, Role role)`) và event `B/identity/UserDeactivated.java(userId, role)`, `B/identity/StudentAnonymized.java(studentId, requestId)` theo `contracts/events-and-ports.md`
- [X] T021 (#1·U1) `B/identity/internal/web/AuthController.java` (`/api/v1/auth/login`, `/refresh`, `/logout`, `/accept-invite`, `/forgot-password`, `/reset-password`) và `B/identity/internal/web/MeController.java` (`GET /me` trả `consentRequired`, `currentPolicyVersion`; `PATCH /me`; `POST /me/consents`)
- [X] T022 (#1·U1) Filter `B/identity/internal/ConsentGateFilter.java`: người dùng `ACTIVE` chưa có `policy_consent` cho `app.privacy.policy-version` → mọi API ngoài `/auth/*`, `GET /me`, `POST /me/consents` trả 403 `CONSENT_REQUIRED` (FR-030)
- [X] T023 (#1·U1) Test `BT/identity/AuthFlowIT.java` (Testcontainers): sai mật khẩu → 401; `INACTIVE` không đăng nhập được; refresh xoay vòng (dùng lại token cũ → 401); logout thu hồi; reset hết hạn sau 30 phút (TestClock); accept-invite thiếu `acceptedPolicyVersion` → 400; đổi `policy-version` → API nghiệp vụ trả `CONSENT_REQUIRED` tới khi `POST /me/consents`; JWT secret 16 byte → context không khởi động
- [X] T024 (#1·U1) Seed demo `B/identity/internal/seed/IdentityDemoSeed.java` (`@Profile("demo-seed")`): `admin@demo.local`, `teacher1@`, `teacher2@`, `student1@`…`student5@demo.local` (ngày sinh người lớn), mật khẩu `Demo@1234`, `ACTIVE`, **kèm `policy_consent` cho phiên bản hiện hành** để consent gate không chặn demo

### Email tối thiểu (để lời mời chạy được ngay từ U1)

- [X] T025 (#1·U1) Migration `MIG/V3__notification.sql`: `notification`, `notification_dispatch` (`dedupe_key` unique; `channel` ∈ `EMAIL`; `status` ∈ `PENDING`,`SENT`,`FAILED`; `attempts smallint`; `next_attempt_at`; `error`; `sent_at`) theo `data-model.md`
- [X] T026 (#1·U1) Port `B/notification/EmailSender.java` (`send(EmailMessage(to, subject, html))`), adapter `B/notification/internal/SmtpEmailSender.java` và `FakeEmailSender.java` (bộ nhớ, đọc được trong test); chọn bằng `app.providers.email`
- [X] T027 (#1·U1) `B/notification/internal/DispatchService.java`: `notify(dedupeKey, userId, email, templateName, model, inAppTitle, inAppLink)` → bỏ qua nếu `dedupe_key` đã có; render Thymeleaf `TPL/*.html` (layout chung tiếng Việt); tạo `notification` trong app; ghi `PENDING` rồi gửi; lỗi → tăng `attempts`, `next_attempt_at` theo backoff **1 → 5 → 15 phút**, sau lần gửi đầu + **3 lần thử lại** → `FAILED` (NFR-REL-06); job `B/notification/internal/RetryDispatchJob.java` (`fixedDelay = 60s` + `@SchedulerLock`); lỗi email không rollback nghiệp vụ
- [X] T028 (#1·U1) Listener `B/notification/internal/IdentityNotificationListener.java` (`@ApplicationModuleListener`): `UserInvited` → `TPL/invite.html` với link `{frontendUrl}/accept-invite?token=…`; `PasswordResetRequested` → `TPL/reset-password.html`

### Hợp đồng giữa module (stub để tách việc FE/BE và D1)

- [X] T029 [P] (#1·U1) Public API catalog `B/catalog/EnrollmentApi.java` (`get(id)` → `EnrollmentView`, `reserve(id)`, `release(id)`, `consume(id)`, `consumeReleased(id)`, `isAssigned(teacherId, studentId)`), `B/catalog/EnrollmentView.java`, event `EnrollmentCreated`, `EnrollmentActivated`, `TeacherReassigned`, `EnrollmentRenewalDue` trong `B/catalog/` đúng payload `contracts/events-and-ports.md`; stub `B/catalog/internal/StubEnrollmentApi.java` (`@Profile("stub")`)
- [X] T030 [P] (#1·U1) Public API scheduling: event `SessionBooked`, `SessionCancelled`, `SessionRescheduled`, `SessionReminderDue`, `SessionFinished`, `SessionOutcomeOverdue` trong `B/scheduling/`; port `B/scheduling/VideoRoomProvider.java` + `JoinInfo(serverUrl, token, roomName)`; `B/scheduling/SessionQueryApi.java` (`find(sessionId)` → `SessionView(id, teacherId, studentId, startsAt, endsAt, status)`)
- [X] T031 [P] (#1·U1) Event learning `SessionReportOverdue`, `SessionReportSubmitted`, `AssignmentSubmitted`, `AssignmentGraded` trong `B/learning/`; event billing `B/billing/PaymentRecorded.java(enrollmentId, paymentId, amountVnd, isFirstPayment)`
- [X] T032 (#1·U1) Spike LiveKit token: `B/scheduling/internal/video/LiveKitVideoRoomProvider.java` tối thiểu cấp token cho room `session-{id}` với `livekit-server --dev`; cùng #2 xác nhận hai trình duyệt vào chung phòng (go/no-go sớm, quyết định cuối 1/11)

### Frontend nền tảng

- [X] T033 (#2·U1) API client `F/lib/api/client.ts`: base URL `VITE_API_URL`, access token trong bộ nhớ, gặp 401 `TOKEN_EXPIRED` gọi `/auth/refresh` (`credentials: 'include'`) rồi thử lại một lần; parse ProblemDetail thành `ApiError{status, code, message, errors}`; `CONSENT_REQUIRED` → điều hướng màn đồng ý; `gen:api` = `openapi-typescript http://localhost:8080/v3/api-docs -o src/lib/api/schema.d.ts`
- [X] T034 (#2·U1) Khung mock MSW `F/mocks/browser.ts`, `F/mocks/handlers/index.ts` (bật bằng `VITE_USE_MOCKS=true`); mỗi FE dev thêm handler của feature mình theo `contracts/rest-api.md` (D4)
- [X] T035 (#2·U1) App shell `F/app/router.tsx`, `F/app/AuthProvider.tsx` (khôi phục phiên bằng refresh khi tải trang), `F/app/RequireRole.tsx`, `F/app/AppLayout.tsx` (sidebar riêng `/admin/*`, `/teacher/*`, `/student/*`, nhãn tiếng Việt), `F/app/queryClient.ts`
- [X] T036 [P] (#2·U1) Trang xác thực `F/features/auth/LoginPage.tsx`, `AcceptInvitePage.tsx` (ô "Đồng ý điều khoản & chính sách dữ liệu" bắt buộc, gửi `acceptedPolicyVersion`), `ForgotPasswordPage.tsx`, `ResetPasswordPage.tsx` (zod: "tối thiểu 8 ký tự, có chữ và số"), `ConsentGatePage.tsx` (khi `consentRequired`: chỉ hiện màn đồng ý, gọi `POST /me/consents`)
- [X] T037 [P] (#2·U1) Component chung `F/components/DataTable.tsx` (`?page&size&sort`), `DateTimeText.tsx` (múi giờ người xem, `vi-VN`), `StatusBadge.tsx`, `ConfirmDialog.tsx`, `EmptyState.tsx`, `useApiErrorToast.ts` (thông điệp tiếng Việt theo `code`)

**Checkpoint (cổng B1, 23/10)**: đăng nhập tài khoản demo, layout đúng vai trò; email mời có trong Mailpit; `ModularityTests` + CI (gồm gitleaks) xanh; spike LiveKit hai người vào chung phòng; FE chạy được trên mock.

---

## Phase 3: User Story 1 - Quản lý tập trung học viên, giáo viên và gói học (Priority: P1) 🎯 MVP

**Goal**: Giáo vụ quản lý người dùng, hồ sơ, khóa học, gói học, ghi danh và phân công giáo viên (phần quyền riêng tư của US1 ở Phase 9).

**Independent Test**: Tạo 1 giáo viên, 1 học viên, 1 gói; ghi danh và phân công; giáo viên chỉ thấy học viên của mình; ngừng giáo viên còn học viên bị chặn (quickstart bước 1–2, 14, 20, 22).

### Tests for User Story 1 ⚠️

- [X] T038 [P] [US1] (#1·U2) Unit test `BT/catalog/EnrollmentTest.java`: `reserve` từ chối khi `total − reserved − consumed = 0` (`NO_SESSIONS_LEFT`) hoặc status ≠ `ACTIVE` (`ENROLLMENT_NOT_ACTIVE`); `consume`; consumed = total → `EXHAUSTED`; `release`; `consumeReleased` khi không còn chỗ → `NO_SESSIONS_LEFT`; chuyển trạng thái không hợp lệ → `INVALID_STATE_TRANSITION`
- [X] T039 [P] [US1] (#1·U2) Integration test `BT/catalog/EnrollmentApiIT.java`: hai luồng cùng `reserve` buổi cuối → chỉ một thành công; CHECK `sessions_reserved + sessions_consumed ≤ sessions_total` luôn giữ
- [X] T040 [P] [US1] (#1·U2) Integration test `BT/catalog/StudentAccessIT.java`: giáo viên A đọc hồ sơ học viên không được phân công → 404; `GET /students` chỉ trả học viên được phân công; học viên chỉ đọc hồ sơ chính mình; ngừng kích hoạt giáo viên còn ghi danh `ACTIVE`/`PENDING_PAYMENT` → 409 `TEACHER_HAS_STUDENTS`

### Implementation for User Story 1

- [X] T041 [US1] (#1·U2) Migration `MIG/V4__catalog.sql`: `teacher_profile`, `student_profile` (`self_reported_level` ∈ `A1`…`C2` nullable, `occupation varchar(100)`, `notes`; **không** có `date_of_birth`), `course` (`name varchar(150)` unique), `package` (`session_count > 0`, `session_minutes IN (25,45,60,90)`, `validity_days > 0`, `price_vnd >= 0`, `status` ∈ `ACTIVE`,`INACTIVE`), `enrollment` (sao chép `sessions_total`, `session_minutes`, `validity_days`, `price_vnd`; `CHECK (sessions_reserved + sessions_consumed <= sessions_total)`; `status` ∈ `PENDING_PAYMENT`,`ACTIVE`,`EXHAUSTED`,`EXPIRED`,`CANCELLED`; `activated_at`, `expires_at`, `renewal_low_notified_at`, `renewal_expiring_notified_at`, `extended_by`, `extended_at`, `version`), `teacher_assignment_history`
- [X] T042 [P] [US1] (#1·U2) Entity + repository `B/catalog/internal/profile/TeacherProfile.java`, `StudentProfile.java`, `B/catalog/internal/course/Course.java`, `Package.java` (+ repository cùng package)
- [X] T043 [US1] (#1·U2) Entity `B/catalog/internal/enrollment/Enrollment.java` với `reserve()`, `release()`, `consume()`, `consumeReleased()`, `activate(now)`, `expire()`, `extend(newExpiresAt, by)`, `cancel()` theo state machine `data-model.md`; `@Version`; `EnrollmentRepository.java`
- [X] T044 [US1] (#1·U2) `B/catalog/internal/course/CourseService.java`, `B/catalog/internal/web/CourseController.java`, `PackageController.java`: `GET` (mọi user), `POST/PUT` (ADMIN); gói `INACTIVE` không dùng cho ghi danh mới; sửa giá gói không đổi ghi danh đã có
- [X] T045 [US1] (#1·U2) `B/catalog/internal/profile/ProfileService.java` + `B/catalog/internal/web/ProfileController.java`: `GET/PUT /teachers/{id}/profile` (A; T chính mình), `GET/PUT /students/{id}/profile` (A sửa; T đọc nếu `isAssigned`; S đọc chính mình), `GET /students?q=&teacherId=&status=&needsConsulting=` (T chỉ học viên được phân công; `needsConsulting` lấy qua API đọc của learning ở T085 — trước khi có thì bỏ qua tham số); không có quyền → 404
- [X] T046 [US1] (#1·U2) `B/catalog/internal/enrollment/EnrollmentService.java` + `B/catalog/internal/web/EnrollmentController.java`: tạo ghi danh (`PENDING_PAYMENT`, sao chép từ package, `priceVnd?` chỉ nhập lúc tạo — **không có API sửa giá**, giáo viên `UserDirectory.requireActive`) → `EnrollmentCreated`; `PUT /enrollments/{id}/teacher` (`reason` bắt buộc, ghi `teacher_assignment_history`, phát `TeacherReassigned`); `POST /extend` (`newExpiresAt`, EXPIRED/ACTIVE → ACTIVE, ghi `extended_by/at`); `POST /cancel`; `GET` danh sách/chi tiết lọc theo quyền, trả `sessionsTotal, sessionsReserved, sessionsConsumed, sessionsRemaining, expiresAt, status`
- [X] T047 [US1] (#1·U2) `B/catalog/internal/enrollment/EnrollmentApiImpl.java` cài `EnrollmentApi`: bộ đếm trong transaction của bên gọi (`Propagation.MANDATORY`), optimistic lock; `isAssigned` = có enrollment `PENDING_PAYMENT`/`ACTIVE`/`EXHAUSTED`/`EXPIRED` cho cặp giáo viên–học viên
- [X] T048 [US1] (#1·U2) `B/catalog/internal/TeacherDeactivationGuard.java` cài `UserDeactivationGuard`: giáo viên còn ghi danh `ACTIVE`/`PENDING_PAYMENT` → `BusinessException(TEACHER_HAS_STUDENTS)`
- [X] T049 [US1] (#1·U2) `B/identity/internal/web/UserAdminController.java` + `UserAdminService.java`: `GET /users?role=&status=&q=`; `POST /users` (`dateOfBirth` bắt buộc khi STUDENT; học viên < 16 tuổi → `AWAITING_GUARDIAN_CONSENT`, không phát `UserInvited`; còn lại `INVITED` + `UserInvited`; email trùng → 409 `EMAIL_TAKEN`); `PATCH /users/{id}/status` (gọi mọi `UserDeactivationGuard`, thu hồi refresh token, phát `UserDeactivated`; bản ghi đã ẩn danh → 409 `ANONYMIZED`); `POST /users/{id}/resend-invite`; chỉ ADMIN
- [X] T050 [US1] (#1·U2) Listener `B/catalog/internal/AnonymizationListener.java` (`StudentAnonymized`, idempotent): xóa `student_profile.notes`, `self_reported_level`, `occupation`
- [X] T051 [US1] (#1·U2) Seed `B/catalog/internal/seed/CatalogDemoSeed.java` (`@Profile("demo-seed")`): khóa "IELTS", "Giao tiếp"; gói "IELTS 24 buổi × 60 phút, 90 ngày", "Giao tiếp 12 buổi × 45 phút, 90 ngày"; ghi danh `ACTIVE` cho student1–4 (teacher1: 1–2, teacher2: 3–4); student5 chưa ghi danh
- [ ] T052 [P] [US1] (#4·U2) Frontend `F/features/admin/users/UsersPage.tsx`, `CreateUserDialog.tsx` (ngày sinh bắt buộc khi chọn Học viên; báo "chờ đồng ý người giám hộ" khi < 16 tuổi; lọc vai trò/trạng thái; ngừng/kích hoạt lại; hiển thị `TEACHER_HAS_STUDENTS`; gửi lại lời mời) + handler MSW
- [ ] T053 [P] [US1] (#4·U2) Frontend `F/features/catalog/courses/CoursesPage.tsx`, `PackagesPage.tsx`, `PackageFormDialog.tsx` (thời lượng chỉ 25/45/60/90 phút; giá VND) + handler MSW
- [ ] T054 [P] [US1] (#4·U2) Frontend `F/features/catalog/profiles/StudentsListPage.tsx` (admin + giáo viên; lọc "Cần tư vấn"), `StudentProfilePage.tsx` (tab Thông tin — gồm trình độ sơ bộ, Ghi danh, chỗ trống cho Năng lực/Lộ trình/Tiến bộ), `TeacherProfilePage.tsx`
- [ ] T055 [US1] (#4·U2) Frontend `F/features/catalog/enrollments/CreateEnrollmentDialog.tsx` (giá mặc định từ gói, sửa được **chỉ lúc tạo**), `EnrollmentDetailPage.tsx` (đã học/đã đặt/còn lại, hạn, trạng thái tiếng Việt; đổi giáo viên kèm lý do + cảnh báo "các buổi đã đặt với giáo viên cũ sẽ bị hủy, không trừ buổi"; gia hạn; hủy)

**Checkpoint (cổng B2, 26/10)**: quickstart bước 1–2, 14, 22 đúng; `EnrollmentApi` thật thay stub.

---

## Phase 4: User Story 2 - Đặt lịch và tổ chức buổi học trực tuyến 1 kèm 1 (Priority: P1) 🎯 MVP

**Goal**: Lịch rảnh, đặt/hủy/đổi lịch, chốt kết quả buổi, nhắc lịch, phòng học video tích hợp.

**Independent Test**: Quickstart bước 4–7, 9, 17–21 (dùng ghi danh `ACTIVE` từ seed).

### Tests for User Story 2 ⚠️

- [X] T056 [P] [US2] (#1·U3) Unit test `BT/scheduling/SlotServiceTest.java`: bước 30 phút; độ dài = `session_minutes`; bỏ slot trước now + **2h**; không vượt **30 ngày** và `expires_at`; trừ ngoại lệ cả ngày/khoảng giờ; trừ buổi đã đặt; quy tắc theo múi giờ giáo viên → UTC đúng
- [X] T057 [P] [US2] (#1·U3) Integration test `BT/scheduling/BookingConcurrencyIT.java`: hai học viên đặt cùng slot song song → một thành công, một 409 `SLOT_TAKEN`; một học viên đặt hai buổi chồng giờ với hai giáo viên → `SLOT_TAKEN`; reserved đúng
- [X] T058 [P] [US2] (#1·U3) Integration test `BT/scheduling/CancellationPolicyIT.java`: còn ≥ **12h** → `CANCELLED_ON_TIME`, hoàn buổi; < 12h → `CANCELLED_LATE`, tính buổi; giáo viên hủy → `TEACHER_ABSENT`, hoàn; đổi lịch quá hạn → 409 `CANCEL_WINDOW_PASSED`; đổi đúng hạn → buổi mới có `rescheduled_from_id`; `cancel-preview` trả `willConsume` đúng
- [X] T059 [P] [US2] (#1·U3) Unit test `BT/scheduling/SessionOutcomeTest.java`: bảng chuyển trạng thái `data-model.md` (COMPLETED/STUDENT_NO_SHOW/CANCELLED_LATE tính; INTERRUPTED/TEACHER_ABSENT/CANCELLED_SYSTEM hoàn; quyết định COUNT → `COMPLETED` + `consumeReleased`, MAKE_UP giữ `INTERRUPTED`; outcome trước `starts_at` → `INVALID_STATE_TRANSITION`; chỉ giáo viên của buổi hoặc admin chốt; admin sửa sau 7 ngày bị từ chối)
- [X] T060 [P] [US2] (#1·U3) Integration test `BT/scheduling/SystemCancelIT.java` (Modulith `Scenario`): `TeacherReassigned` → mọi `BOOKED` tương lai với giáo viên cũ → `CANCELLED_SYSTEM`, chỗ được trả; `UserDeactivated(STUDENT)` và `StudentAnonymized` tương tự; phát lại event không lỗi (idempotent); buổi đã qua không bị đụng
- [X] T061 [P] [US2] (#1·U3) Unit test `BT/scheduling/JoinServiceTest.java`: chỉ giáo viên/học viên của buổi lấy token; ngoài [bắt đầu − **10 phút**, kết thúc + **15 phút**] → 403 `ROOM_NOT_OPEN`; người ngoài → 404; ghi `teacher_joined_at`/`student_joined_at` lần đầu

### Implementation for User Story 2

- [X] T062 [US2] (#1·U3) Migration `MIG/V5__scheduling.sql`: `availability_rule` (`day_of_week` 1–7, `end_time > start_time`), `availability_exception`, `session` (`status` ∈ `BOOKED`,`COMPLETED`,`STUDENT_NO_SHOW`,`TEACHER_ABSENT`,`CANCELLED_ON_TIME`,`CANCELLED_LATE`,`CANCELLED_SYSTEM`,`INTERRUPTED`; `reminder_24h_at`, `reminder_1h_at`, `outcome_overdue_notified_at`, `outcome_by`; `version`) với **hai exclusion constraint** (`teacher_id`/`student_id` `WITH =`, `tstzrange(starts_at, ends_at) WITH &&`, `WHERE status IN ('BOOKED','COMPLETED','STUDENT_NO_SHOW','CANCELLED_LATE','INTERRUPTED')`) và index `(teacher_id, starts_at)`, `(student_id, starts_at)`, `(status, starts_at)`
- [X] T063 [P] [US2] (#1·U3) Entity + repository `B/scheduling/internal/availability/AvailabilityRule.java`, `AvailabilityException.java`, `B/scheduling/internal/session/Session.java` (method chuyển trạng thái trả hành động số buổi RESERVE/RELEASE/CONSUME/CONSUME_RELEASED), `SessionRepository.java`
- [X] T064 [US2] (#1·U3) `B/scheduling/internal/availability/AvailabilityService.java` + `B/scheduling/internal/web/AvailabilityController.java`: `GET/PUT /teachers/{id}/availability` (thay toàn bộ; từ chối khoảng chồng cùng ngày), `GET/POST/DELETE /teachers/{id}/availability/exceptions` — exception trùng buổi `BOOKED` → 409 `EXCEPTION_OVERLAPS_SESSIONS` kèm `sessions[]` (FR-008); A hoặc chính giáo viên
- [X] T065 [US2] (#1·U3) `B/scheduling/internal/slot/SlotService.java` + `GET /enrollments/{id}/slots?from=&to=` (A; S chủ ghi danh) theo R4 và `app.scheduling.*`
- [X] T066 [US2] (#1·U3) `B/scheduling/internal/session/BookingService.java`: slot phải nằm trong slot sinh ra; `EnrollmentApi.reserve` + insert **cùng transaction**; SQLState `23P01` → 409 `SLOT_TAKEN`; `booked_by`; phát `SessionBooked`
- [X] T067 [US2] (#1·U3) `B/scheduling/internal/session/CancellationService.java`: `GET /sessions/{id}/cancel-preview` (`willConsume`, `cutoffAt`); hủy (S/A theo `cancel-cutoff-hours`; T → `TEACHER_ABSENT`); đổi lịch chỉ trước hạn chót (hủy đúng hạn + đặt mới cùng transaction, `rescheduled_from_id`; sau hạn → `CANCEL_WINDOW_PASSED`); gọi `EnrollmentApi.release/consume`; phát `SessionCancelled`, `SessionRescheduled`
- [X] T068 [US2] (#1·U3) `B/scheduling/internal/session/OutcomeService.java`: `POST /sessions/{id}/outcome` (T của buổi hoặc A chốt thay, sau `starts_at`, ghi `outcome_by`), `POST /sessions/{id}/interruption-decision` (A; `COUNT` → `COMPLETED` + `consumeReleased`, `MAKE_UP` giữ `INTERRUPTED`), admin sửa kết quả trong 7 ngày; phát `SessionFinished`
- [X] T069 [US2] (#1·U3) `B/scheduling/internal/session/SystemCancelListener.java` (`@ApplicationModuleListener`, idempotent): `TeacherReassigned` (buổi `BOOKED` tương lai của `fromTeacherId` + `studentId`), `UserDeactivated` (role STUDENT), `StudentAnonymized` → `CANCELLED_SYSTEM` + `EnrollmentApi.release`, phát `SessionCancelled` (status `CANCELLED_SYSTEM`)
- [X] T070 [US2] (#1·U3) `B/scheduling/internal/web/SessionController.java`: `POST /sessions`, `GET /sessions?from=&to=&teacherId=&studentId=&status=&outcomePending=` (lọc theo quyền), `GET /sessions/{id}`, `POST /{id}/cancel`, `POST /{id}/reschedule`, `POST /{id}/outcome`, `POST /{id}/join`; `B/scheduling/internal/SessionQueryApiImpl.java` cài `SessionQueryApi`
- [X] T071 [US2] (#1·U3) Video: hoàn thiện `B/scheduling/internal/video/LiveKitVideoRoomProvider.java` (room `session-{id}`, identity = userId, name = họ tên, quyền join/publish/publishData, TTL tới hết cửa sổ, `app.livekit.{url,api-key,api-secret}`), `FakeVideoRoomProvider.java`, `B/scheduling/internal/session/JoinService.java` (`room-open-before-minutes` / `room-close-after-minutes`)
- [X] T072 [US2] (#1·U3) Job `B/scheduling/internal/jobs/SessionReminderJob.java` (`fixedDelay = 60s` + `@SchedulerLock`): `BOOKED` bắt đầu trong ≤ 24h, `reminder_24h_at IS NULL`, được đặt trước mốc → `SessionReminderDue(24H)`; tương tự mốc **1h**; đặt sau mốc thì đánh dấu đã xử lý, không gửi
- [X] T073 [US2] (#1·U3) Job `B/scheduling/internal/jobs/OutcomeOverdueJob.java` (`cron = "0 0 * * * *"` + `@SchedulerLock`): `BOOKED` có `now ≥ ends_at + 24h` và `outcome_overdue_notified_at IS NULL` → phát `SessionOutcomeOverdue`, set cột (FR-014)
- [X] T074 [US2] (#1·U3) Seed `B/scheduling/internal/seed/SchedulingDemoSeed.java` (`@Profile("demo-seed")`): lịch rảnh teacher1, teacher2 T2–T6 18:00–21:00, T7 08:00–11:00; vài buổi đã qua (COMPLETED) và sắp tới (BOOKED) cho student1–4; số buổi trong enrollment khớp
- [ ] T075 [P] [US2] (#2·U3) Frontend `F/features/scheduling/AvailabilityEditor.tsx` (lưới tuần, danh sách ngày nghỉ; hiện danh sách buổi trùng khi `EXCEPTION_OVERLAPS_SESSIONS`) tại `/teacher/availability` và trong hồ sơ giáo viên của admin + handler MSW
- [ ] T076 [P] [US2] (#2·U3) Frontend `F/features/scheduling/BookSessionPage.tsx` + `SlotPicker.tsx`: chọn ghi danh → slot theo ngày (múi giờ người xem) → xác nhận; hiện đã học/đã đặt/còn lại; thông báo `SLOT_TAKEN`, `NO_SESSIONS_LEFT`, `ENROLLMENT_NOT_ACTIVE` ("Gói chưa kích hoạt — chờ thanh toán"); admin đặt thay
- [ ] T077 [P] [US2] (#2·U3) Frontend `F/features/scheduling/ScheduleCalendar.tsx` (FullCalendar tuần/tháng, màu theo 8 trạng thái, lọc giáo viên/học viên cho admin) cho cả ba vai trò
- [ ] T078 [US2] (#2·U3) Frontend `F/features/scheduling/SessionDetailPage.tsx`: Hủy (gọi `cancel-preview`, cảnh báo "sẽ bị trừ buổi" nếu `willConsume`), Đổi lịch (ẩn sau hạn chót), chọn kết quả buổi cho giáo viên, quyết định Gián đoạn cho admin (Tính là đã học / Xếp bù); danh sách "Buổi chưa chốt kết quả" (`outcomePending=true`) cho giáo viên và admin; nút "Vào lớp" chỉ bật trong cửa sổ
- [ ] T079 [US2] (#2·U3) Frontend `F/features/classroom/ClassroomPage.tsx`: `POST /sessions/{id}/join` → `LiveKitRoom` + `VideoConference` (video, âm thanh, chia sẻ màn hình, chat), nhãn tiếng Việt; rời phòng thì giáo viên tới bước chọn kết quả

**Checkpoint (cổng B3, 1/11)**: quickstart bước 4–7, 9, 17–21 đúng; go/no-go LiveKit.

---

## Phase 5: User Story 3 - Hồ sơ năng lực và lộ trình học cá nhân hóa (Priority: P2)

**Goal**: Hồ sơ năng lực CEFR 5 kỹ năng, mục tiêu tùy chọn + "Cần tư vấn", lộ trình có lịch sử điều chỉnh. **Backend do #3 (D1), #1 review.**

**Independent Test**: Ghi kết quả đầu vào, tạo lộ trình 3 chặng; học viên xem được trình độ, mục tiêu, lộ trình; học viên chưa có mục tiêu hiện "Cần tư vấn".

### Tests for User Story 3 ⚠️

- [X] T080 [P] [US3] (#3·U4) Unit test `BT/learning/LearningPathServiceTest.java`: mỗi học viên tối đa 1 lộ trình `ACTIVE`; sửa thiếu `reason` → 400; `path_revision.summary` liệt kê đúng chủ đề thêm/xóa/đổi tên và mốc đổi ngày; % hoàn thành = topic `DONE` / tổng topic
- [X] T081 [P] [US3] (#3·U4) Integration test `BT/learning/LearningAccessIT.java`: giáo viên không được phân công đọc/sửa năng lực, mục tiêu, lộ trình → 404; học viên chỉ đọc; `needsConsulting` đúng khi thêm/bỏ goal `ACTIVE`

### Implementation for User Story 3

- [X] T082 [US3] (#3·U4) Migration `MIG/V6__learning_profile.sql`: `proficiency_assessment` (`type` ∈ `PLACEMENT`,`PERIODIC`; `listening`, `speaking`, `reading`, `writing`, `grammar_vocab` NOT NULL ∈ `A1`…`C2`), `learning_goal` (`type` ∈ `IELTS`,`TOEIC`,`COMMUNICATION`,`BUSINESS`,`KIDS`,`OTHER`; `target varchar(100)`; `deadline` nullable; `status` ∈ `ACTIVE`,`ACHIEVED`,`DROPPED`; 0..n mỗi học viên), `learning_path` (unique index một `ACTIVE` mỗi `student_id`), `milestone` (unique `(path_id, position)`), `topic` (unique `(milestone_id, position)`), `path_revision` (`reason` NOT NULL)
- [X] T083 [P] [US3] (#3·U4) Entity + repository trong `B/learning/internal/proficiency/` (`ProficiencyAssessment`, `LearningGoal`) và `B/learning/internal/path/` (`LearningPath`, `Milestone`, `Topic`, `PathRevision`)
- [X] T084 [US3] (#3·U4) `B/learning/internal/StudentAccessGuard.java`: ADMIN mọi học viên; TEACHER khi `EnrollmentApi.isAssigned`; STUDENT chỉ chính mình; ghi chỉ A/T phụ trách; vi phạm → 404
- [X] T085 [US3] (#3·U4) `B/learning/internal/proficiency/ProficiencyService.java` + `B/learning/internal/web/ProficiencyController.java`: `GET/POST /students/{id}/assessments`, `GET /students/{id}/proficiency` (bản mới nhất + lịch sử), `GET/POST/PUT /students/{id}/goals` (loại + mức bắt buộc, thời hạn tùy chọn; lưu lịch sử); ~~public API `LearningProfileApi`~~ → **SPI `catalog.GoalStatusProvider`** do learning cài (OQ-4, tránh vòng catalog ↔ learning) cho filter `needsConsulting` của catalog (T045)
- [X] T086 [US3] (#3·U4) `B/learning/internal/path/LearningPathService.java` + `B/learning/internal/web/LearningPathController.java`: `GET/POST /students/{id}/learning-path`, `PUT /learning-paths/{id}` (cả cây, `reason` bắt buộc, tự sinh `summary`, lưu `path_revision`), `GET /learning-paths/{id}/revisions`; trả `completionPercent`
- [X] T087 [US3] (#3·U4) Listener `B/learning/internal/ProfileAnonymizationListener.java` (`StudentAnonymized`, idempotent): xóa `proficiency_assessment`, `learning_goal`, `learning_path`/`milestone`/`topic`/`path_revision` của học viên
- [ ] T088 [P] [US3] (#3·U4) Frontend `F/features/learning/proficiency/ProficiencyPanel.tsx` + `AssessmentFormDialog.tsx` (5 kỹ năng A1–C2, Đầu vào/Định kỳ) + `GoalsPanel.tsx` (nhãn "Cần tư vấn" khi chưa có mục tiêu); gắn tab "Năng lực" của `StudentProfilePage`
- [ ] T089 [US3] (#3·U4) Frontend `F/features/learning/path/LearningPathEditor.tsx` (thêm/xóa/sắp xếp chặng, chủ đề, ngày mục tiêu, hộp thoại lý do), `LearningPathView.tsx` (chỉ đọc, thanh %), `RevisionHistory.tsx`; route `/student/learning-path`
- [X] T090 [US3] (#3·U4) Seed `B/learning/internal/seed/LearningDemoSeed.java` (`@Profile("demo-seed")`): đánh giá đầu vào, mục tiêu và lộ trình 3 chặng × 3–4 chủ đề cho student1–3; student4 không có mục tiêu (demo "Cần tư vấn")

**Checkpoint (cổng B3, 1/11)**: học viên xem trình độ, mục tiêu, lộ trình; lịch sử điều chỉnh đúng; lọc "Cần tư vấn" chạy.

---

## Phase 6: User Story 4 - Đánh giá sau buổi học và theo dõi tiến bộ (Priority: P2)

**Goal**: Phiếu đánh giá sau buổi, bài tập nộp dạng văn bản (K2), trang tiến bộ.

**Independent Test**: 3 buổi có phiếu; học viên thấy nhận xét và biểu đồ điểm (quickstart bước 8, 10, 11).

### Tests for User Story 4 ⚠️

- [ ] T091 [P] [US4] (#1·U6) Test `BT/learning/SessionReportFlowTest.java` (`@ApplicationModuleTest` + `Scenario`): `SessionFinished(COMPLETED)` → phiếu `PENDING`, `due_at = endsAt + 24h`; phát lại không tạo trùng; outcome khác COMPLETED không tạo phiếu; nộp thiếu `comment` hoặc không có điểm kỹ năng nào → 400; topic ngoài lộ trình `ACTIVE` → 400
- [ ] T092 [P] [US4] (#1·U6) Unit test `BT/learning/AssignmentServiceTest.java`: bài nộp cần `textAnswer` khác rỗng; nộp lại được tới khi đã chấm; `due_at` null không đánh dấu muộn; nộp sau `due_at` vẫn nhận và đánh dấu muộn; điểm ngoài 0–10 → 400

### Implementation for User Story 4

- [ ] T093 [US4] (#1·U6) Migration `MIG/V7__learning_reports.sql`: `session_report` (`session_id` unique; `status` ∈ `PENDING`,`SUBMITTED`; `due_at`; `taught_topic_ids uuid[]`; `score_listening`…`score_grammar_vocab` smallint `CHECK BETWEEN 1 AND 5` nullable; `comment`; `submitted_at`; `overdue_notified_at`), `assignment` (`due_at` nullable), `submission` (`assignment_id` unique; `text_answer` NOT NULL — K2; `score` `CHECK BETWEEN 0 AND 10`; `graded_by`, `graded_at`)
- [ ] T094 [P] [US4] (#1·U6) Entity + repository trong `B/learning/internal/report/` (`SessionReport`) và `B/learning/internal/assignment/` (`Assignment`, `Submission`)
- [ ] T095 [US4] (#1·U6) Listener `B/learning/internal/report/SessionFinishedListener.java`: tạo phiếu `PENDING` khi outcome `COMPLETED` (idempotent nhờ unique `session_id`)
- [ ] T096 [US4] (#1·U6) `B/learning/internal/report/SessionReportService.java` + `B/learning/internal/web/SessionReportController.java`: `GET /session-reports?status=PENDING`, `GET/PUT /sessions/{id}/report` (chỉ giáo viên của buổi nộp): chủ đề đã dạy, `completedTopicIds` → `DONE`, điểm, nhận xét, tạo bài tập; `SUBMITTED`, `submitted_at`; phát `SessionReportSubmitted`
- [ ] T097 [US4] (#1·U6) Job `B/learning/internal/jobs/ReportOverdueJob.java` (`cron = "0 0 * * * *"` + `@SchedulerLock`): `PENDING` quá `due_at`, `overdue_notified_at IS NULL` → `SessionReportOverdue`
- [ ] T098 [US4] (#1·U6) `B/learning/internal/assignment/AssignmentService.java` + `B/learning/internal/web/AssignmentController.java`: `GET /assignments?studentId=&status=`, `PUT /assignments/{id}/submission` (`{textAnswer}`), `POST /assignments/{id}/grade` (`{score, feedback}`); phát `AssignmentSubmitted`, `AssignmentGraded`; **không** làm upload-url/download-url (K2)
- [ ] T099 [US4] (#1·U6) `B/learning/internal/progress/ProgressService.java` + `GET /students/{id}/progress`: `skillSeries` (điểm từng kỹ năng theo ngày từ phiếu `SUBMITTED`), `pathCompletion`, `recentReports` (5 phiếu gần nhất); không có `attendanceRate`, `assignmentRate` ([GĐ2])
- [ ] T100 [US4] (#1·U6) Listener `B/learning/internal/ReportAnonymizationListener.java` (`StudentAnonymized`, idempotent): xóa `assignment` + `submission` của học viên; trong `session_report` xóa điểm kỹ năng, `comment`, `taught_topic_ids` (giữ `submitted_at`, `due_at`, `status`)
- [ ] T101 [P] [US4] (#3·U6) Frontend `F/features/learning/report/PendingReportsList.tsx` (trang chủ giáo viên) + `SessionReportForm.tsx`: chủ đề từ lộ trình, điểm 1–5 từng kỹ năng (bỏ trống được), nhận xét, nhiều bài tập (hạn nộp tùy chọn); điền dưới 3 phút (SC-008)
- [ ] T102 [P] [US4] (#3·U6) Frontend `F/features/learning/assignments/AssignmentsPage.tsx`: học viên nộp văn bản (nhãn "Nộp muộn" khi quá hạn); giáo viên đọc bài, chấm 0–10 kèm phản hồi
- [ ] T103 [US4] (#3·U6) Frontend `F/features/learning/progress/ProgressPage.tsx`: thẻ CEFR hiện tại, biểu đồ đường Recharts 5 kỹ năng theo thời gian, % hoàn thành lộ trình, nhận xét gần nhất; dùng cho `/student/progress` và tab "Tiến bộ"

**Checkpoint (cổng B6, 10/11)**: quickstart bước 8, 10, 11 đúng.

---

## Phase 7: User Story 5 - Quản lý số buổi học và học phí (Priority: P3)

**Goal**: Học phí thủ công, công nợ, kích hoạt gói khi có thanh toán đầu tiên, hết hạn, gia hạn, nhắc gia hạn. **Billing core (U2) và U8 backend do #4 (D1), #1 review.**

**Independent Test**: Ghi danh gói 12 buổi, thanh toán 50% → gói kích hoạt, công nợ 50%; dùng tới còn 2 buổi → có nhắc gia hạn (quickstart bước 3, 13).

### Tests for User Story 5 ⚠️

- [X] T104 [P] [US5] (#4·U2) Unit test `BT/billing/PaymentServiceTest.java`: `amount_vnd > 0`; `paid_at ≤ hôm nay`; công nợ = `amount_due − amount_paid`; điều chỉnh âm chỉ ADMIN, bắt buộc `note`; thanh toán đầu tiên `isFirstPayment = true`; thanh toán sau không đổi `activated_at`/`expires_at`
- [X] T105 [P] [US5] (#4·U2) Test `BT/billing/BillingFlowTest.java` (Modulith `Scenario`): `EnrollmentCreated` → `receivable`; `PaymentRecorded` đầu tiên (số tiền bất kỳ) → enrollment `ACTIVE`, `expires_at = activated_at + validity_days`, phát `EnrollmentActivated`; học viên đọc được thanh toán của mình, không đọc được của người khác (404)
- [ ] T106 [P] [US5] (#4·U8) Test `BT/catalog/EnrollmentLifecycleJobTest.java`: `ACTIVE` quá `expires_at` → `EXPIRED` (buổi `BOOKED` sau hạn vẫn giữ); còn ≤ **2** buổi hoặc ≤ **7** ngày → `EnrollmentRenewalDue` đúng một lần mỗi lý do; `extend` đưa `EXPIRED` → `ACTIVE`

### Implementation for User Story 5

- [X] T107 [US5] (#4·U2) Migration `MIG/V8__billing.sql`: `receivable` (PK `enrollment_id`, `amount_due_vnd`, `amount_paid_vnd`), `payment` (`method` ∈ `BANK_TRANSFER`,`CASH`,`OTHER`; `is_adjustment` boolean; `CHECK (is_adjustment OR amount_vnd > 0)`; `CHECK (NOT is_adjustment OR note IS NOT NULL)`; `recorded_by`)
- [X] T108 [P] [US5] (#4·U2) Entity + repository `B/billing/internal/Receivable.java`, `Payment.java`, `ReceivableRepository.java`, `PaymentRepository.java`
- [X] T109 [US5] (#4·U2) Listener `B/billing/internal/EnrollmentCreatedListener.java`: `receivable` với `amount_due_vnd = priceVnd`
- [X] T110 [US5] (#4·U2) `B/billing/internal/PaymentService.java` + `B/billing/internal/web/BillingController.java`: `GET /enrollments/{id}/payments` (A; S chủ ghi danh, chỉ đọc), `POST /enrollments/{id}/payments`, `POST /enrollments/{id}/payments/adjustments`; thanh toán không sửa/xóa; phát `PaymentRecorded`
- [X] T111 [US5] (#1·U2) ~~Listener `PaymentRecordedListener`~~ → **đổi thành** `EnrollmentApi.activateOnFirstPayment` gọi đồng bộ từ `PaymentService` (tránh vòng catalog ↔ billing, events-and-ports.md §2) — bản gốc: Listener `B/catalog/internal/enrollment/PaymentRecordedListener.java`: enrollment `PENDING_PAYMENT` → `activate(now)` (`expires_at = now + validity_days`), phát `EnrollmentActivated`
- [ ] T112 [US5] (#4·U8) Job `B/catalog/internal/jobs/EnrollmentLifecycleJob.java` (`cron = "0 5 0 * * *", zone = "Asia/Ho_Chi_Minh"` + `@SchedulerLock`): hết hạn → `EXPIRED`; nhắc theo `renewal-low-sessions` / `renewal-expiring-days`, set `renewal_*_notified_at`; phát `EnrollmentRenewalDue`
- [ ] T113 [US5] (#4·U8) `GET /receivables?hasDebt=true` và `GET /reports/revenue?from=&to=` (tổng đã thu, tổng công nợ) trong `B/billing/internal/web/BillingReportController.java`
- [ ] T114 [P] [US5] (#4·U2) Frontend `F/features/billing/PaymentsPanel.tsx` (trong `EnrollmentDetailPage`): lịch sử, `RecordPaymentDialog.tsx` (VND, ngày, phương thức, ghi chú — gợi ý "không ghi tên học viên"), `AdjustmentDialog.tsx`, nhãn công nợ
- [ ] T115 [P] [US5] (#4·U8) Frontend `F/features/billing/ReceivablesPage.tsx`: học viên còn nợ, lọc, liên kết ghi danh
- [ ] T116 [US5] (#4·U8) Frontend `F/features/catalog/enrollments/MyPackagesPage.tsx` (`/student/packages`): đã học/đã đặt/còn lại, hạn, trạng thái "Chờ thanh toán", lịch sử thanh toán + công nợ (chỉ đọc), banner gia hạn khi ≤ 2 buổi hoặc ≤ 7 ngày

**Checkpoint (cổng B2 cho billing core, B5 cho U8)**: quickstart bước 3 và 13 đúng; số buổi và công nợ khớp lịch sử (SC-007).

---

## Phase 8: User Story 6 - Theo dõi chất lượng giảng dạy (Priority: P3) — rút gọn theo K1

**Goal**: Học viên chấm sao buổi học (giáo vụ xem); dashboard doanh thu, công nợ, số buổi đã dạy theo giáo viên. Báo cáo chất lượng (FR-023) là GĐ2.

**Independent Test**: Học viên chấm sao một buổi (lần 2 bị từ chối); giáo vụ xem được sao; dashboard số buổi đã dạy khớp (quickstart bước 12, 24).

### Tests for User Story 6 ⚠️

- [ ] T117 [P] [US6] (#1·U6) Test `BT/learning/SessionFeedbackIT.java`: chỉ học viên của buổi `COMPLETED`, trong 7 ngày sau `ends_at`; lần 2 → 409 `FEEDBACK_EXISTS`; quá hạn → 409 `FEEDBACK_WINDOW_CLOSED`; giáo viên `GET /sessions/{id}/feedback` → 403
- [ ] T118 [P] [US6] (#4·U8) Test `BT/scheduling/TeacherSessionsReportIT.java`: `sessionsTaught` = `COMPLETED` + `STUDENT_NO_SHOW` + `CANCELLED_LATE` theo giáo viên trong khoảng thời gian; `CANCELLED_SYSTEM`, `CANCELLED_ON_TIME`, `TEACHER_ABSENT`, `INTERRUPTED` không được tính

### Implementation for User Story 6

- [ ] T119 [US6] (#1·U6) Migration `MIG/V9__learning_feedback.sql`: `session_feedback` (`session_id` unique, `stars` `CHECK BETWEEN 1 AND 5`, `comment` nullable)
- [ ] T120 [US6] (#1·U6) `B/learning/internal/feedback/FeedbackService.java` + controller: `POST /sessions/{id}/feedback` (S, qua `SessionQueryApi.find`: đúng học viên, `COMPLETED`, ≤ 7 ngày, một lần), `GET /sessions/{id}/feedback` (chỉ A); listener ẩn danh hóa xóa `comment`, giữ `stars`
- [ ] T121 [US6] (#4·U8) `B/scheduling/internal/report/TeacherSessionsReportService.java` + `GET /reports/teacher-sessions?from=&to=` (A): theo giáo viên — chỉ `sessionsTaught` (K1)
- [ ] T122 [P] [US6] (#3·U6) Frontend `F/features/learning/feedback/SessionFeedbackPrompt.tsx`: chấm 1–5 sao + nhận xét sau buổi `COMPLETED` (trang chủ học viên, chi tiết buổi; không bắt buộc; ẩn sau khi gửi hoặc quá 7 ngày)
- [ ] T123 [US6] (#4·U8) Frontend `F/features/admin/dashboard/DashboardPage.tsx`: chọn khoảng thời gian; thẻ tổng đã thu và công nợ (`/reports/revenue`); bảng số buổi đã dạy theo giáo viên (`/reports/teacher-sessions`); xem sao từng buổi ở chi tiết buổi (admin)

**Checkpoint (cổng B6, 10/11)**: quickstart bước 12, 24 đúng.

---

## Phase 9: User Story 1 (tiếp) - Quyền riêng tư dữ liệu cá nhân (Priority: P1, FR-030/FR-031) — U7

**Goal**: Đồng ý người giám hộ cho học viên < 16 tuổi, yêu cầu xem/xóa dữ liệu, ẩn danh hóa, trang tóm tắt in được. (Đồng ý chính sách + consent gate đã làm ở Phase 2.)

**Independent Test**: Quickstart bước 15, 16, 23 — sau ẩn danh hóa, doanh thu, công nợ, số buổi đã dạy không đổi.

**Phụ thuộc**: listener ẩn danh hóa/hủy hệ thống của từng module đã có (T050, T069, T087, T100, T120, T126).

### Tests for User Story 1 (privacy) ⚠️

- [ ] T124 [P] [US1] (#1·U7) Integration test `BT/identity/GuardianConsentIT.java`: tạo học viên 12 tuổi → `AWAITING_GUARDIAN_CONSENT`, không có email mời; ghi `guardian_consent` → `INVITED` + `UserInvited`; học viên vẫn < 16 tuổi lúc accept-invite mà không có đồng ý → `GUARDIAN_CONSENT_REQUIRED`
- [ ] T125 [P] [US1] (#1·U7) Integration test xuyên module `BT/privacy/AnonymizationIT.java` (`@SpringBootTest` + Testcontainers): chụp `/reports/revenue`, `/receivables`, `/reports/teacher-sessions` → ẩn danh hóa học viên có buổi đã học, buổi `BOOKED` tương lai, thanh toán, công nợ, lộ trình, bài nộp → tên/email/ngày sinh/lộ trình/bài nộp/nhận xét mất; buổi tương lai `CANCELLED_SYSTEM`; ba báo cáo **không đổi** (SC-007); `data_subject_request` `COMPLETED` còn `requester_name`; gọi lại không lỗi

### Implementation for User Story 1 (privacy)

- [ ] T126 [US1] (#1·U7) Listener `B/notification/internal/NotificationAnonymizationListener.java` (`StudentAnonymized`, idempotent): xóa `notification` của học viên; `notification_dispatch.recipient` → `anonymized`
- [ ] T127 [US1] (#1·U7) `B/identity/internal/privacy/GuardianConsentService.java` + `B/identity/internal/web/GuardianConsentController.java`: `GET/PUT /students/{id}/guardian-consent` (A; `guardianName`, `relationship`, `guardianContact`, `consentedOn ≤ hôm nay` bắt buộc); lưu khi `AWAITING_GUARDIAN_CONSENT` → `INVITED` + phát `UserInvited`
- [ ] T128 [US1] (#1·U7) `B/identity/internal/privacy/DataSubjectRequestService.java` + `B/identity/internal/web/DataSubjectRequestController.java`: `GET/POST /data-subject-requests?studentId=&status=`, `POST /{id}/reject` (`resolutionNote` bắt buộc), `POST /{id}/complete-access`, `POST /{id}/anonymize` (`confirm: true`)
- [ ] T129 [US1] (#1·U7) `B/identity/internal/privacy/AnonymizationService.java`: trong một transaction — `user_account` → `email = anonymized+{id}@invalid`, `full_name = "Học viên đã ẩn danh"`, `phone`/`date_of_birth` = null, `status = INACTIVE`, `anonymized_at = now`; xóa `guardian_consent`; thu hồi refresh token; yêu cầu → `COMPLETED`; phát `StudentAnonymized`; bản ghi đã ẩn danh → 409 `ANONYMIZED`
- [ ] T130 [P] [US1] (#4·U7) Frontend `F/features/admin/privacy/GuardianConsentDialog.tsx` (mở từ hồ sơ học viên `AWAITING_GUARDIAN_CONSENT`), `DataSubjectRequestsPage.tsx` (ghi yêu cầu, từ chối, hoàn tất xem, ẩn danh hóa với xác nhận "không khôi phục được")
- [ ] T131 [P] [US1] (#4·U7) Frontend `F/features/admin/privacy/PersonalDataSummaryPage.tsx`: trang in được (`@media print`) ghép từ `/users/{id}`, `/students/{id}/profile`, `/enrollments`, `/sessions`, `/enrollments/{id}/payments`, `/students/{id}/progress`, `/assignments`; nút "In / lưu PDF"

**Checkpoint (cổng B5, 7/11)**: quickstart bước 15, 16, 23 đúng; T125 xanh.

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Thông báo đầy đủ (U5), vận hành demo, ổn định — U5 (B4 2–4/11), B7 (11/11)

### Thông báo cho mọi event (U5)

- [ ] T132 (#1·U5) Listener `B/notification/internal/SchedulingNotificationListener.java`: `SessionBooked` (xác nhận cho cả hai), `SessionCancelled` (báo bên còn lại; `TEACHER_ABSENT` báo thêm mọi admin qua `UserDirectory.findAdmins()`; `CANCELLED_SYSTEM` báo học viên + giáo viên), `SessionRescheduled`, `SessionReminderDue` (dedupe `session:{id}:reminder:{window}:{userId}`), `SessionOutcomeOverdue` (giáo viên + admin); giờ theo `timezone` người nhận; template `TPL/session-booked.html`, `session-cancelled.html`, `session-rescheduled.html`, `session-reminder.html`, `session-outcome-overdue.html`
- [ ] T133 (#1·U5) Listener `B/notification/internal/CatalogNotificationListener.java`: `TeacherReassigned` (học viên + giáo viên cũ + giáo viên mới), `EnrollmentActivated` (chào mừng), `EnrollmentRenewalDue` (học viên + mọi admin; email + trong hệ thống); template `teacher-reassigned.html`, `enrollment-activated.html`, `renewal-reminder.html`
- [ ] T134 (#1·U5) Listener `B/notification/internal/LearningNotificationListener.java`: `SessionReportOverdue` (nhắc giáo viên), `SessionReportSubmitted` (mời học viên xem nhận xét + chấm sao), `AssignmentSubmitted`, `AssignmentGraded`; template tương ứng
- [ ] T135 (#1·U5) `B/notification/internal/web/NotificationController.java`: `GET /notifications?unread=`, `POST /{id}/read`, `POST /read-all` (chỉ của chính user)
- [ ] T136 [P] (#3·U5) Frontend `F/features/notifications/NotificationBell.tsx` + `NotificationList.tsx` (gọi lại mỗi 60 giây, đánh dấu đã đọc)

### Vận hành demo (U5)

- [ ] T137 (#1·U5) `backend/Dockerfile` (multi-stage, chạy được trên ARM64), `deploy/docker-compose.prod.yml` (backend, postgres 16 có volume, caddy; Docker log rotation `max-size: 10m`, `max-file: 3`; healthcheck `/actuator/health`), `deploy/Caddyfile` (HTTPS tự động); hướng dẫn `deploy/README.md` cho Oracle Cloud Always Free VM
- [ ] T138 (#1·U5) Sao lưu (R17): service `backup` trong `deploy/docker-compose.prod.yml` chạy `pg_dump` hằng ngày → bucket R2 riêng, giữ 7 bản; script khôi phục `deploy/restore.sh`; RPO ≤ 24 h, RTO ≤ 4 h
- [ ] T139 [P] (#1·U5) Dịch vụ demo: LiveKit Cloud free, Brevo hoặc Gmail SMTP, Cloudflare Pages cho `frontend/` (`VITE_API_URL`), dịch vụ ping healthcheck free; ghi trong `deploy/README.md` (không ghi secret)

### Ổn định (B6–B7)

- [ ] T140 (#1) Rà soát bảo mật: mọi controller method có `@PreAuthorize` hoặc kiểm tra sở hữu; không có quyền → 404; secret chỉ qua env; CORS đúng domain demo; `BT/security/AuthorizationMatrixIT.java` (mỗi vai trò × endpoint chính, gồm privacy và feedback)
- [ ] T141 [P] (#2) Kịch bản k6 `perf/booking-and-join.js` (P2): p95 < 500 ms, p99 < 1 s cho slot/đặt lịch trên dữ liệu 1.000 HV / 50 GV; 400 lượt `join` trong 60 s + 200 người dùng ảo, container 1 GB RAM (NFR-PERF-01/02, NFR-SCAL-01); thêm index nếu chậm bằng migration `V{yyyyMMddHHmm}__perf_indexes.sql` (#1)
- [ ] T142 [P] (#2) Smoke test Playwright `frontend/e2e/demo.spec.ts` theo quickstart bước 1–5, 8, 11, 15–17 (`app.providers.video=fake`)
- [ ] T143 [P] (#3, #4) Kiểm tra giao diện điện thoại và Lighthouse mobile (LCP ≤ 3 s) cho trang học viên (đặt lịch, phòng học, tiến bộ, bài tập, gói học); test usability đặt lịch < 1 phút (SC-001), phiếu đánh giá < 3 phút (SC-008); sửa layout trong `F/features/**`
- [ ] T144 (#1) Diễn tập khôi phục sao lưu trên VM (NFR-REL-10) trước 2026-11-11; ghi kết quả vào `deploy/README.md`
- [ ] T145 (#1) Hoàn thiện `README.md` gốc: kiến trúc module, cách chạy (dẫn `quickstart.md`), bảng phân công, quy trình PR
- [ ] T146 (#1, cả team) Chạy toàn bộ 24 bước `specs/001-personalized-english-tutoring/quickstart.md` trên môi trường demo (bỏ phần đã cắt theo K1/K2); ghi lỗi thành issue, sửa trước 2026-11-11; tổng duyệt 2026-11-11, demo 2026-11-12

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)** → **Foundational (Phase 2)**: chặn mọi backend story. FE bắt đầu ngay sau T033–T035 trên mock.
- **US1 (Phase 3)**: sau Phase 2. `EnrollmentApi` thật (T047) chặn US2, US3, US5.
- **US2 (Phase 4)**: sau T047.
- **US3 (Phase 5)**: sau T047 (`isAssigned`); song song US2 vì backend do #3 (D1).
- **US5 (Phase 7)**: billing core (T104–T111, T114) song song US1 bởi #4 (D1); phần U8 (T106, T112, T113, T115, T116) sau US2.
- **US4 (Phase 6)**: sau US2 (`SessionFinished`) và US3 (lộ trình).
- **US6 (Phase 8)**: sau US2; feedback sau US4 (cùng U6).
- **US1 privacy (Phase 9)**: sau listener của mọi module (T050, T069, T087, T100, T120, T126).
- **Polish (Phase 10)**: thông báo (T132–T136) sau khi event của các story đã có; ổn định cuối cùng.

### User Story Dependencies

```text
Phase 2 ──► US1 (U2) ──► US2 (U3) ──► US4 + US6-feedback (U6)
   │          │  │          │  ▲
   │          │  └► US3 (U4)┘  │  (lộ trình cho phiếu đánh giá)
   │          └► US5 billing core (U2, #4)
   │                         US2 ──► US5 renewal + US6 dashboard (U8, #4)
   └───────────────────────────────► US1 privacy (U7) ◄── listener của U2, U3, U4, U5, U6
```

### Within Each User Story

- Test nghiệp vụ trọng yếu viết trước, chạy đỏ, rồi cài đặt (constitution IV)
- Migration → entity → service → controller → frontend nối API thật (FE đã dựng trên mock)
- Listener ẩn danh hóa/hủy hệ thống làm cùng module của nó, idempotent

### Parallel Opportunities

- Phase 1: T003–T008 song song; FE (#2) dựng shell và mock trong lúc #1 làm backend nền
- Từ 24/10: #4 làm billing core BE (T104–T110) song song #1 làm catalog; từ 27/10 #3 làm US3 BE song song #1 làm US2
- FE #2, #3, #4 làm toàn bộ màn hình của mình trên mock từ 18/10, nối API thật khi unit tương ứng merge
- Trong mỗi story: các task test [P] và entity [P] song song

---

## Parallel Example: User Story 2

```bash
# Backend (#1) — viết test trọng yếu trước:
Task: "Unit test SlotServiceTest in backend/src/test/java/com/englishplatform/scheduling/SlotServiceTest.java"
Task: "Integration test BookingConcurrencyIT in backend/src/test/java/com/englishplatform/scheduling/BookingConcurrencyIT.java"
Task: "Integration test CancellationPolicyIT in backend/src/test/java/com/englishplatform/scheduling/CancellationPolicyIT.java"

# Frontend (#2) song song trên mock MSW:
Task: "AvailabilityEditor in frontend/src/features/scheduling/AvailabilityEditor.tsx"
Task: "BookSessionPage + SlotPicker in frontend/src/features/scheduling/"
Task: "ScheduleCalendar in frontend/src/features/scheduling/ScheduleCalendar.tsx"

# Backend D1 (#3) cùng lúc:
Task: "LearningPathServiceTest in backend/src/test/java/com/englishplatform/learning/LearningPathServiceTest.java"
```

---

## Implementation Strategy

### Lịch theo bolt (bắt đầu 2026-10-18, demo 2026-11-12; team làm cả cuối tuần — đã xác nhận)

| Bolt | Ngày | Unit | #1 (backend) | #2 (FE) | #3 (FE + D1) | #4 (FE + D1) |
|---|---|---|---|---|---|---|
| B1 | 18–23/10 | U1 | T001–T003, T005, T007–T032 | T004, T006, T033–T037, cùng spike T032 | mock + màn US3/US4 | mock + màn US1/US5 |
| B2 | 24–26/10 | U2 | T038–T051, T111 | US2 FE trên mock (T075–T077) | US3 FE trên mock (T088–T089) | T052–T055, BE T104–T110, T114 |
| B3 | 27/10–1/11 | U3, U4 | T056–T074 | T075–T079 nối API | BE T080–T087, T090; FE nối API | review, US5/US6 FE trên mock |
| B4 | 2–4/11 | U5 | T132–T135, T137–T139 | k6 nháp (T141) | T136 | — |
| B5 | 5–7/11 | U7, U8 | T124–T129 | Playwright (T142) | T101–T103 trên mock | BE T106, T112, T113, T118, T121; FE T115, T116, T123, T130, T131 |
| B6 | 8–10/11 | U6 | T091–T100, T117, T119, T120 | T142 | T101–T103, T122 nối API | T143 |
| B7 | 11/11 (+ demo 12/11) | — | T140, T144, T145, T146 | T141, T142 | T143 | T143 |

### MVP First

1. B1: walking skeleton — đăng nhập + đồng ý chính sách, CI xanh, spike LiveKit, deploy được
2. B2–B3: US1 + US2 (P1) → **lõi MVP** (quản lý, ghi danh, thanh toán kích hoạt gói, đặt lịch, phòng học); US3 song song
3. B4–B5: thông báo đầy đủ, vận hành/sao lưu, privacy (MUST, constitution V), gia hạn/công nợ/dashboard
4. B6: US4 + đánh giá sao; B7 ổn định
5. Nếu trễ: cắt theo thứ tự trong `aidlc/unit-of-work.md` ("Thứ tự cắt nếu vẫn trễ"); **không** cắt FR-030/FR-031

### Quy tắc làm việc

- Mỗi task = 1 PR nhỏ (< ~400 dòng); PR cần **1 review của thành viên khác** (constitution): PR backend của #1 do #3 hoặc #4 review (họ cũng làm BE theo D1); PR BE của #3/#4 do #1 review; PR FE review chéo giữa #2–#4 (P1), #1 chỉ review FE đụng `lib/api`, auth/consent, bảo mật
- Đổi event/public API trong `contracts/` phải báo người dùng contract đó; FE cập nhật mock tương ứng
- Đánh dấu `[x]` khi PR merge; `/speckit-aidlc-map-tasks` đọc tag `(Người·Unit)` để gán task vào unit

---

## Notes

- [P] = khác file, không phụ thuộc task chưa xong
- [Story] map task về user story để truy vết; (Người·Unit) map về unit trong `aidlc/unit-of-work.md`
- Dừng ở mỗi Checkpoint (cổng bolt) để kiểm tra
- Các mục plan/contracts về `FileStorage`, upload tệp, `auth_rate_limit`, `/reports/teacher-quality` không có task vì đã cắt (K1–K3)
