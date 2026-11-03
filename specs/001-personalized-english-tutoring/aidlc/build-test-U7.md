# Build & Test Report — U7 — Nền tảng đào tạo tiếng Anh 1 kèm 1 cá nhân hóa (001-personalized-english-tutoring)

- **Ngày (UTC)**: 2026-11-03, 13:43–13:58 UTC (giờ máy: 20:43–20:58 +07)
- **Unit**: U7, quyền riêng tư FR-030/FR-031 (B5), **chỉ phần BACKEND**. FE T130, T131 (#4) và R17 phần FE (#2) nằm ngoài phạm vi, ghi NOT RUN.
- **Tasks trong phạm vi**: T124–T129, cùng R14, R3 còn lại, R10, F5 (aidlc-state.md:31) — hoàn thành: **6/6 task BE** là `[X]` (tasks.md:311-312, 316-319). T130, T131 vẫn `[ ]` (tasks.md:320-321).
  - Phạm vi đã chốt: T125 so SC-007 bằng dữ liệu gốc vì 3 báo cáo của U8 chưa có (unit-of-work.md:400-402, tasks.md:312).
  - Thay đổi chưa commit: `git status` có 23 mục (15 modified, 8 untracked) trước và sau lượt chạy; QE chỉ thêm file báo cáo này. Không có migration cũ nào bị sửa, chỉ thêm `V202611031500__dispatch_about_student.sql`.
- **Môi trường**: Windows 11, Git Bash; JDK 21.0.12 (`JAVA_HOME=/c/Program Files/Java/jdk-21.0.12`); Maven 3.10.0 qua wrapper; Docker Server 29.8.2; Testcontainers `postgres:16`.

## 1. Build

| Thành phần | Lệnh | Kết quả | Ghi chú |
|------------|------|---------|---------|
| Backend format | `cd backend && ./mvnw spotless:check` | **PASS** (`BUILD SUCCESS`, exit 0, 2.665 s) | Lần này 205 file đều bị bỏ qua nhờ cache của Spotless, nên QE chạy lại không cache ở dòng dưới. |
| Backend format (không cache) | Xóa `backend/target/spotless-index`, rồi `./mvnw -B spotless:check` | **PASS** (`BUILD SUCCESS`, 15.675 s) | `keeping 205 files clean - 0 needs changes`, 205 file được kiểm thật. |
| Backend (Maven, gồm Enforcer) | `cd backend && ./mvnw -B verify` | **PASS** (`BUILD SUCCESS`, exit 0, 02:41 min, xong lúc 20:46:15 +07) | `RequireJavaVersion passed`, `BannedDependencies passed`. Log có 0 dòng `[ERROR]`, 0 dòng log ứng dụng mức ERROR. WARN `session_teacher_no_overlap` trong `BookingConcurrencyIT` là do test cố ý đặt chồng lịch. WARN `Gửi email ... lỗi` là do test cố ý làm SMTP lỗi; các dòng này chỉ in tên lớp exception. |
| Frontend | — | **NOT RUN** | T130, T131, R17 (FE) thuộc #2 và #4, không nằm trong phạm vi BE. |

## 2. Test

| Bộ test | Lệnh | Pass / Fail / Skip | Ghi chú |
|---------|------|--------------------|---------|
| Unit + Scenario (surefire) | `./mvnw -B verify` | **94 / 0 / 0** | Mới ở U7: `identity.internal.ConsentGateFilterTest` 5, `notification.internal.DispatchServiceLogTest` 1. Các bộ khác đều pass: `EnrollmentTest` 13, `SessionOutcomeTest` 13, `SchedulingNotificationListenerTest` 9, `LearningPathServiceTest` 7, `PaymentServiceTest` 7, `SlotServiceTest` 7, `CatalogNotificationListenerTest` 4, `JoinServiceTest` 4, `LearningNotificationListenerTest` 4, `NotificationServiceTest` 4, `SecurityPropertiesTest` 3, `NotificationControllerTest` 3, `BillingFlowTest` 2, `LiveKitVideoRoomProviderTest` 2, `ModularityTests` 2, `NotificationDispatchTest` 2, `AuthorizationRulesTest` 1, `GlobalExceptionHandlerTest` 1. |
| Integration Testcontainers (failsafe) | `./mvnw -B verify` | **96 / 0 / 0** (`failsafe-summary.xml`: completed 96, errors 0, failures 0, skipped 0, flakes 0) | Mới ở U7: `privacy.AnonymizationIT` 7, `identity.GuardianConsentIT` 5. `SchedulingNotificationIT` 5 (thêm `anonymizationScrubsPendingEmailToTeacherMentioningStudent`; ca ẩn danh hóa cũ nay phát event 2 lần). Các bộ khác đều pass: `AuthFlowIT` 23, `CatalogFlowIT` 9, `EnrollmentApiIT` 8, `StudentAccessIT` 6, `CancellationPolicyIT` 5, `LearningAccessIT` 5, `NotificationDispatchIT` 5, `BookingConcurrencyIT` 4, `SchedulingJobsIT` 4, `SessionOutcomeIT` 4, `NotificationApiIT` 3, `SystemCancelIT` 3. Chạy 1 lần; chưa đánh giá độ ổn định qua nhiều lần chạy. |
| Ranh giới module | `ModularityTests` (trong `verify`) | **2 / 0 / 0** | `ApplicationModules.verify()` pass với gói mới `identity.internal.privacy`. |
| Quét log của `verify` | Tìm trong log 585 dòng | **Không thấy rò rỉ** (quan sát, không phải test) | 0 địa chỉ email; 0 khớp với `password`, `$2a$`, `bearer `, `eyJ...`, `inviteToken`, `refreshToken`, `accessToken`. Có 4 dòng `Đã ẩn danh hóa học viên {uuid}`, chỉ chứa UUID. Xem F2 về việc chưa có test tự động. |
| Smoke quickstart bước 15 (BE) | Jar từ `verify`, profile `local,demo-seed`, Postgres và Mailpit thật | **PASS** | Xem chi tiết bên dưới. |
| Smoke quickstart bước 16 (BE) | Như trên, khởi động lại với `POLICY_VERSION=2026-12` | **PASS** | Xem chi tiết bên dưới. |
| Smoke quickstart bước 23 (BE) | Như trên, kèm `pg_dump --data-only` | **PASS, có 1 ghi chú (F4)** | Xem chi tiết bên dưới. |
| Smoke kiểm thêm: HV dưới tuổi qua đường đổi trạng thái | Như trên | **FAIL so với DoD (F1)** | Xem chi tiết bên dưới. |
| FE T130 (`GuardianConsentDialog`, `DataSubjectRequestsPage`) | — | **NOT RUN** | Thuộc #4, ngoài phạm vi BE. |
| FE T131 (`PersonalDataSummaryPage`, trang in được ở bước 23) | — | **NOT RUN** | Thuộc #4, ngoài phạm vi BE. QE chỉ gọi các API đọc mà trang này sẽ dùng (xem F12). |
| R17 phần FE (link nội dung chính sách ở accept-invite và consent gate) | — | **NOT RUN** | Thuộc #2, ngoài phạm vi BE. |
| Dashboard/báo cáo ở bước 23 | — | **NOT RUN** | `/reports/revenue`, `/receivables`, `/reports/teacher-sessions` thuộc U8. Theo phạm vi đã chốt, QE so bằng dữ liệu gốc. |
| Runbook khôi phục R10 (`deploy/README.md:80-92`) | — | **NOT RUN** | Không diễn tập khôi phục sao lưu rồi ẩn danh hóa lại. Chỉ xác nhận dòng log mà runbook dùng có xuất hiện (`app2.log:54`). |
| CI GitHub Actions trên PR thật | `.github/workflows/ci.yml` | **NOT RUN** | Thay đổi U7 chưa commit, chưa có PR. |
| gitleaks | — | **NOT RUN** | Không có trong danh sách lệnh của lượt này. |

### Chi tiết smoke local

Cấu hình: `docker compose -p u7smoke up -d --wait postgres mailpit`; backend chạy `java -jar target/english-platform-0.0.1-SNAPSHOT.jar --spring.profiles.active=local,demo-seed --server.port=18080` với `VIDEO_PROVIDER=fake`. LiveKit không chạy vì các bước này không cần. Sau khi xong, QE đã dừng app và chạy `docker compose -p u7smoke down -v`; không còn container hay volume `u7smoke`, và không đụng tới volume `pgdata` của project mặc định.

| Bước | Thao tác | Quan sát |
|------|----------|----------|
| 15 | Tạo HV 12 tuổi | 201, `AWAITING_GUARDIAN_CONSENT`; Mailpit có 0 email tới địa chỉ này. |
| 15 | HV thiếu `dateOfBirth` | 400 `VALIDATION_FAILED`. |
| 15 | `PUT guardian-consent` | 200, `recordedBy` là admin; trạng thái `INVITED`; Mailpit có 1 email "Mời kích hoạt tài khoản English 1-1" chứa token. |
| 16 | `accept-invite` không có `acceptedPolicyVersion` | 400 `VALIDATION_FAILED` (`field=acceptedPolicyVersion`); đăng nhập sau đó 401. |
| 16 | `accept-invite` có đồng ý | 204; đăng nhập 200; `consentRequired=false`; `policy_consent` có `2026-11`. |
| 16 | Khởi động lại với `POLICY_VERSION=2026-12`, đăng nhập lại | Đăng nhập 200; `GET /me` 200 với `consentRequired=true`. `GET /sessions`, `/enrollments`, `/students/{id}/profile`, `/notifications`, `/data-subject-requests` và `PATCH /me` đều 403 `CONSENT_REQUIRED`. Admin seed cũng bị chặn. |
| 16 | `POST /me/consents` | Phiên bản cũ: 400. Phiên bản mới: 204, sau đó `GET /notifications` 200. `policy_consent` có cả `2026-11,2026-12` (bản ghi cũ còn). |
| 23 | Ghi `ACCESS`, gọi API đọc, `complete-access` | 201 `RECEIVED`; `/users/{id}`, `/students/{id}/profile`, `/enrollments`, `/sessions` đều 200; `/students/{id}/progress` và `/assignments` 404 (F12); `complete-access` 200 `COMPLETED`. |
| 23 | Ẩn danh hóa `student1` (seed) | `confirm=false` → 400; HV tự gọi → 403; hợp lệ → 200 `COMPLETED`, giữ `requesterName`. `/users/{id}`: `email=anonymized+{id}@invalid`, `fullName="Học viên đã ẩn danh"`, `phone`/`dateOfBirth` null, `INACTIVE`, `anonymized=true`. |
| 23 | Số liệu của `student1` | Trước = sau: `paid=0`, `debt=7200000`, `consumed=2`, tổng `payment`=0, số buổi `COMPLETED` theo GV là 4 và 4. **Giới hạn**: seed không có thanh toán (`CatalogDemoSeed.java:22-24`), nên phần "đã thu" được kiểm ở dòng tiếp theo. |
| 23 | Số liệu HV đi luồng thật | Thanh toán 400000: trước và sau đều `paid=400000`, `debt=2600000`. |
| 23 | Buổi và dữ liệu học tập | `student1`: BOOKED tương lai 1 → 0, `CANCELLED_SYSTEM` = 1, `sessions_reserved` 2 → 1 (còn 1 buổi seed BOOKED đã qua giờ; đúng phạm vi "buổi tương lai"). Lộ trình, mục tiêu, đánh giá năng lực: 1/1/1 → 0/0/0. HV đặt buổi thật: BOOKED → `CANCELLED_SYSTEM`, `sessions_reserved` về 0. |
| 23 | `pg_dump` toàn CSDL sau ẩn danh hóa | `student1`: 0 dòng chứa email, 0 dòng chứa tên. HV có email mời + đặt buổi + thông báo GV (`minh.smoke`): trước có email 4 / tên 2, sau có email 0 / tên 0. HV có số điện thoại (`lan.smoke`): email 0, số điện thoại 0, nhưng tên còn 1 dòng ở `payment.note` do QE cố ý nhập (F4). `event_publication` còn 0 dòng. Mailpit có 0 email tới `@invalid`. |
| 23 | Không vào lại được | Access token cũ → 401; refresh token cũ → 401; đăng nhập bằng email cũ và email ẩn danh → 401; `forgot-password` → 202 nhưng không tạo token; `PATCH status` → 409 `ANONYMIZED`; `resend-invite` → 409 `INVALID_STATE_TRANSITION`; `anonymize` lần 2 → 409 `ANONYMIZED`. |
| 23 | Yêu cầu còn treo | `ERASURE` khác → 409 `ANONYMIZED` và vẫn `RECEIVED`; `complete-access` của `ACCESS` khác → 200 (F9). |
| Thêm | HV 12 tuổi: `INACTIVE` → `ACTIVE` → `resend-invite` | **HV nhận email mời khi chưa có đồng ý giám hộ.** Chi tiết ở F1. |
| Log | `app1.log`, `app2.log` | 0 dòng ERROR, 0 email, 0 token, 0 `MatKhau123`. `app1.log:65` in mật khẩu demo (F3). |

### Đối chiếu DoD riêng của U7 (unit-of-work.md:347-353)

| Mục DoD | Trạng thái | Test và kết quả quan sát |
|---------|-----------|--------------------------|
| `accept-invite` thiếu đồng ý → 400 | **Đạt** | `AuthFlowIT.acceptInviteRequiresPolicyConsentThenActivates` pass (`AuthFlowIT.java:248-297`). Smoke bước 16 cho 400. |
| Đổi `policy-version` → mọi API nghiệp vụ trả `CONSENT_REQUIRED` tới khi đồng ý, bản ghi cũ còn | **Đạt (test có giới hạn)** | `AuthFlowIT.policyVersionChangeRequiresNewConsentAndKeepsHistory` và `consentGateBlocksUntilCurrentPolicyAccepted` pass, `ConsentGateFilterTest` 5/5 pass. Giới hạn: IT chỉ gọi `PATCH /me` và giả lập phiên bản cũ bằng dữ liệu, không đổi cấu hình (F5). Smoke đã đổi cấu hình thật và thấy 6 API bị chặn. |
| HV 12 tuổi không có email mời tới khi ghi đồng ý giám hộ (quickstart.md:66-67) | **Đạt ở luồng chính; KHÔNG đạt ở đường đổi trạng thái** | `GuardianConsentIT.underageStudentIsInvitedOnlyAfterGuardianConsent`, `invitedUnderageStudentWithoutConsentCannotAcceptInvite`, `CatalogFlowIT.underageStudentWaitsForGuardianConsentWithoutInvite`, `AuthFlowIT.underageStudentNeedsGuardianConsent` đều pass. Các IT này đếm event `UserInvited`, không đếm email; smoke với Mailpit đã xác nhận 0 email. **Đường `INACTIVE` → `ACTIVE` → `resend-invite` gửi được email mời, chưa có test (F1).** |
| IT ẩn danh hóa giữ nguyên số liệu (SC-007): tổng đã thu, công nợ, số buổi đã dạy trước = sau | **Đạt theo phạm vi đã chốt** | `AnonymizationIT.anonymizationRemovesIdentityAndKeepsFigures` pass: `400000/600000/1/1` không đổi, cả sau khi phát lại event. So bằng dữ liệu gốc: API payments, đếm SQL buổi `COMPLETED`, `sessionsConsumed`. Ba báo cáo thật chờ U8 (unit-of-work.md:400-402). |
| Buổi BOOKED tương lai → `CANCELLED_SYSTEM`, số buổi được hoàn | **Đạt** | Cùng test: `session.status`, `sessionsReserved` 1 → 0, buổi đã dạy vẫn `COMPLETED`. Smoke cũng khớp. |
| Định danh không còn | **Đạt (test có giới hạn)** | Cùng test kiểm `/users/{id}`, `password_hash` null, token, `learning_goal`, `student_profile`, `notification`, `notification_dispatch`. Giới hạn: HV trong test không có số điện thoại nên assert `phone` không có ý nghĩa, và IT không tạo lộ trình (F6). Smoke `pg_dump` đã phủ các phần này, trừ ghi chú tự do (F4). |
| Phát lại event không lỗi | **Đạt** | Cùng test phát lại `StudentAnonymized`, chờ `event_publication` trống và số liệu không đổi. `SchedulingNotificationIT`, `LearningAccessIT.anonymizationRemovesLearningDataIdempotently` cũng phát 2 lần (F5 của U5 đã xử lý). |
| Hồ sơ ẩn danh không đăng nhập/kích hoạt lại được | **Đạt** | `AnonymizationIT.anonymizedStudentCanNeitherLogInNorBeReactivated` pass: `/me` 401, login 401, `PATCH status` 409 `ANONYMIZED`, ghi yêu cầu mới và đồng ý giám hộ 409. Refresh token chỉ được IT kiểm trong DB (F7); smoke qua HTTP cho 401. |
| GV/HV gọi endpoint privacy → bị từ chối | **Đạt** | `AnonymizationIT.teacherAndStudentCannotReachPrivacyEndpoints` (5 endpoint, 2 vai trò → 403) và `GuardianConsentIT.onlyAdminReachesGuardianConsent` pass. |
| Log không chứa mật khẩu/token (NFR-PRIV-02) | **GAP** | Không có test nào kiểm mật khẩu/token trong log. `OutputCaptureExtension` chỉ có ở `DispatchServiceLogTest` và test đó chỉ kiểm địa chỉ email. Quét log `verify` và smoke không thấy rò rỉ, trừ dòng seed in mật khẩu demo (F2, F3). |
| IT này được U6 chạy lại sau khi mở rộng listener learning | **Chưa tới lượt** | U6 chưa làm. |
| (BE) Seed demo có sẵn bản ghi đồng ý | **Đạt (không có test tự động)** | `IdentityDemoSeed.java:81`. Smoke: tài khoản seed đăng nhập và gọi API được ngay với policy `2026-11`. |
| (BE) `date_of_birth` bắt buộc với STUDENT | **Đạt** | `CatalogFlowIT.createUserValidation` có trong bộ IT pass. Smoke: thiếu ngày sinh → 400. |

### Đối chiếu task

| Task | Trạng thái | Test thực có |
|------|-----------|--------------|
| T124 `GuardianConsentIT` | **Đạt** | 5/5 pass, phủ đủ các ý của task. Xem F1 về đường chưa phủ. |
| T125 `AnonymizationIT` | **Đạt theo phạm vi đã chốt, còn thiếu một phần** | 7/7 pass. Chưa có "lộ trình" trong bộ dữ liệu của test (F6). "Bài nộp/nhận xét" chưa có bảng, chờ U6. 3 báo cáo chờ U8. |
| T126 listener notification | **Đạt** | `SchedulingNotificationIT.anonymizationDeletesStudentNotificationsAndMasksRecipient` và `anonymizationScrubsPendingEmailToTeacherMentioningStudent`, `NotificationDispatchIT.anonymizationMasksIdentityEmailsOfStudent` pass. |
| T127 đồng ý giám hộ | **Đạt** | `GuardianConsentIT.consentRequiresAllFieldsAndNoFutureDate`, `consentOnlyAppliesToStudentAwaitingIt`, `onlyAdminReachesGuardianConsent` pass. |
| T128 yêu cầu dữ liệu | **Đạt** | `AnonymizationIT.anonymizeOnlyForReceivedErasureWithConfirmation`, `requestsApplyOnlyToExistingStudentsAndValidDates` pass. Bộ lọc `?status=` và `?studentId=` được dùng trong test. |
| T129 `AnonymizationService` | **Đạt (chưa kiểm rollback)** | `AnonymizationIT.guardianConsentIsDeleted` và các test trên pass. Yêu cầu "trong một transaction" chỉ dựa vào `@Transactional`, chưa có test lỗi giữa chừng (F8). |
| R3 còn lại | **Đạt** | `DispatchServiceLogTest` pass; `anonymizationScrubsPendingEmailToTeacherMentioningStudent` pass. |
| R10 | **Chỉ tài liệu** | Runbook `deploy/README.md:80-92` chưa diễn tập (F11). |
| R14 | **Đạt** | `ConsentGateFilterTest` 5/5 pass. |
| F5 (từ U5) | **Đạt** | `SchedulingNotificationIT.java:138-140` phát event 2 lần, pass. |

## 3. Review độc lập (theo constitution)

QE chỉ rà ở mức bằng chứng build/test, chưa phải code review đầy đủ.

| Nguyên tắc | Kết quả (Đạt / Vi phạm / N/A) | Bằng chứng (file:dòng) |
|------------|-------------------------------|------------------------|
| I. Modular Monolith, `ApplicationModules.verify()` | Đạt (local) | `ModularityTests` 2/0/0. Identity chỉ phát `StudentAnonymized` (`AnonymizationService.java:75`), các module khác tự dọn. |
| II. Provider-Agnostic (Ports & Adapters) | N/A | U7 không thêm provider. Smoke gửi email thật qua SMTP tới Mailpit qua port `EmailSender`. |
| III. Free-First & YAGNI | Đạt | Không thêm hạ tầng; chỉ thêm 1 cột và 1 index (`V202611031500__dispatch_about_student.sql`). |
| IV. Kiểm thử trọng yếu, Postgres thật, cấm H2, CI mỗi PR | Đạt một phần | 13 IT mới trên `postgres:16` thật (`AnonymizationIT` 7, `GuardianConsentIT` 5, `SchedulingNotificationIT` thêm 1), toàn backend 190/0/0 (94 + 96). Còn thiếu test NFR-PRIV-02 (F2) và test cho đường vòng F1. CI chưa chạy. |
| V. Bảo mật & dữ liệu cá nhân | **Vi phạm (F1)**, phần còn lại đạt | FR-030: `UserAdminService.java:120-124,133-139` cho phép gửi email mời tới HV dưới tuổi khi chưa có đồng ý. FR-031: `pg_dump` không còn email, tên, số điện thoại, trừ ghi chú tự do (F4). Endpoint privacy chỉ admin. |
| Ràng buộc công nghệ (Java 21, Boot, Flyway) | Đạt | Enforcer pass. Migration mới chạy được trên Testcontainers và trên CSDL trống của smoke. Không sửa migration cũ. |
| Quy trình & DoD (CI xanh + review + demo) | Chưa đạt | Gate local xanh; chưa có PR/CI, chưa có review độc lập ghi trong báo cáo này, FE T130/T131 chưa làm, F1 và F2 còn mở. |

## 4. Phát hiện

| # | Mức độ | Mô tả | File | Đề xuất |
|---|--------|-------|------|---------|
| F1 | **Cao** | **HV dưới 16 tuổi nhận được email mời khi chưa có đồng ý giám hộ, sau đó hồ sơ kẹt vĩnh viễn.** Đã tái hiện trên app thật: tạo HV 12 tuổi → `AWAITING_GUARDIAN_CONSENT`; `PATCH status INACTIVE` → 200; `PATCH status ACTIVE` → 200 `INVITED` (do chưa có mật khẩu); `resend-invite` → 204; Mailpit có 1 email mời. Trái DoD (unit-of-work.md:348), S-US1.3 AC4 (chặn gửi mời, nêu ở R17 của U1, build-test-U1.md:128) và máy trạng thái (data-model.md:37-40 không có đường này). `PUT guardian-consent` sau đó → 409 `INVALID_STATE_TRANSITION`; `accept-invite` → 409 `GUARDIAN_CONSENT_REQUIRED` (lớp chặn này đúng, HV không kích hoạt được). Kết quả là không ghi được đồng ý và cũng không kích hoạt được. `GuardianConsentIT.java:104-127` dựng đúng trạng thái `INVITED` chưa đồng ý nhưng không thử `PUT`. | `UserAdminService.java:120-124,133-139`; `GuardianConsentService.java:76-78`; `InvitationService.java:57` | Khi `ACTIVE` mà chưa có mật khẩu, HV dưới tuổi chưa có đồng ý phải về `AWAITING_GUARDIAN_CONSENT`. `resendInvite` hoặc `issueInvite` từ chối khi cần đồng ý mà chưa có. Cho `PUT guardian-consent` nhận cả HV dưới tuổi đang `INVITED` chưa có đồng ý. Thêm IT cho chuỗi trên. |
| F2 | Trung bình | **DoD NFR-PRIV-02 chưa có test.** nfr.md:184 yêu cầu chạy luồng login với `OutputCaptureExtension` và khẳng định không có mật khẩu/token mẫu. Hiện extension này chỉ dùng ở một test và test đó chỉ kiểm địa chỉ email. Kết quả quét log của QE sạch nhưng chỉ là quan sát một lần. | `DispatchServiceLogTest.java:19-44`; `nfr.md:184`; không có test tương ứng trong `src/test` | Thêm IT với `OutputCaptureExtension` đi qua login, refresh, accept-invite, forgot/reset password, anonymize; khẳng định log không chứa mật khẩu, access/refresh token, token mời/reset. |
| F3 | Thấp | Seed ghi mật khẩu demo ra log mức INFO, trái nghĩa đen của NFR-PRIV-02. Quan sát: `app1.log:65` có `mật khẩu Demo@1234`. Chỉ xảy ra với profile `demo-seed`, và mật khẩu này đã công khai ở quickstart.md:31, nên rủi ro thực tế thấp. Nếu làm F2 mà có chạy seed, test sẽ phát hiện dòng này. | `IdentityDemoSeed.java:74` | Bỏ tham số mật khẩu khỏi dòng log. |
| F4 | Trung bình | **Ghi chú tự do không được xử lý khi ẩn danh hóa.** QE cố ý nhập tên HV vào `payment.note`; sau ẩn danh hóa, `pg_dump` vẫn còn đúng 1 dòng chứa tên ở `payment`. Thiết kế chủ đích "billing không đổi" (unit-of-work.md:342), nên đây là khoảng trống của thiết kế và T125 không phủ. Các cột tự do khác có thể gặp tình huống tương tự nhưng **QE chưa kiểm**: `session_outcome_change.note`, `teacher_assignment_history.reason`, `session.cancel_reason`. | `V8__billing.sql:23`; `V202610181200__session_outcome_change.sql:11`; `V4__catalog.sql:86`; `V5__scheduling.sql:47` | Người dùng quyết định một trong hai: xóa các ghi chú này khi ẩn danh hóa (không ảnh hưởng số liệu SC-007), hoặc ghi rõ quy định "không nhập định danh vào ghi chú" trong tài liệu và UI, chấp nhận rủi ro. Nếu xóa, thêm assert vào `AnonymizationIT`. |
| F5 | Thấp | Test đổi `policy-version` chỉ gọi `PATCH /me` và giả lập bản cũ bằng dữ liệu; không có IT nào gọi API nghiệp vụ thật sau khi cấu hình thật sự đổi. Unit `ConsentGateFilterTest` chỉ kiểm logic đường dẫn. Smoke đã xác nhận đúng, nhưng chưa có gì giữ hành vi này khi hồi quy. | `AuthFlowIT.java:430-451`; `ConsentGateFilterTest.java:29-35` | Trong `policyVersionChangeRequiresNewConsentAndKeepsHistory`, gọi thêm vài API nghiệp vụ (`/sessions`, `/enrollments`, `/notifications`) và kiểm 403 `CONSENT_REQUIRED`. |
| F6 | Thấp | `AnonymizationIT` chưa phủ đủ T125: không tạo lộ trình hay đánh giá năng lực (đây là API đã có), nên "lộ trình mất" chỉ được `LearningAccessIT` kiểm với event phát tay. Assert `phone` là rỗng luôn đúng vì HV trong test không có số điện thoại. Smoke đã xác nhận đúng. | `AnonymizationIT.java:170-179,208`; `ApiTestSupport.java:76-83`; `LearningAccessIT.java:197-242` | Trong test chính: đặt số điện thoại cho HV, tạo lộ trình và đánh giá, rồi khẳng định `learning_path`, `proficiency_assessment` về 0. |
| F7 | Thấp | Thu hồi refresh token chỉ được IT kiểm trong DB (`used_at`), chưa gọi `/auth/refresh` bằng token cũ. Smoke cho 401. | `AnonymizationIT.java:213-219` | Đăng nhập lấy cookie trước khi ẩn danh hóa, rồi khẳng định `/auth/refresh` trả 401. |
| F8 | Thấp | Yêu cầu "trong một transaction" của T129 chưa có test lỗi giữa chừng. Ngoài ra, dòng log mà runbook R10 dựa vào được ghi **trước** khi commit: nếu commit lỗi, log vẫn ghi đã ẩn danh hóa, và runbook có thể áp dụng ẩn danh hóa cho một yêu cầu thực chất chưa hoàn tất. | `AnonymizationService.java:52,74-77`; `deploy/README.md:84-88` | Thêm test rollback (ví dụ giả lập lỗi ở bước publish). Ghi log trong `afterCommit`, hoặc ghi chú trong runbook là phải đối chiếu với sổ yêu cầu. |
| F9 | Thấp | Yêu cầu khác còn treo khi HV đã ẩn danh hóa: `ERASURE` thứ hai trả 409 `ANONYMIZED` và giữ `RECEIVED` mãi (chỉ còn đường `reject`, tuy nhiên yêu cầu đó thực chất đã được đáp ứng); `ACCESS` còn treo vẫn `complete-access` được 200 dù không còn dữ liệu để cung cấp. Chưa có test và spec chưa nói rõ. | `AnonymizationService.java:62-64`; `DataSubjectRequestService.java:58-75` | Chốt hành vi: khi ẩn danh hóa, tự đóng các yêu cầu `RECEIVED` khác của HV kèm `resolutionNote`, hoặc ghi rõ quy trình dùng `reject`. |
| F10 | Thông tin | Migration `about_student_id` không backfill. Bản `PENDING` tồn tại lúc chạy migration sẽ không được xóa nội dung khi ẩn danh hóa. Cửa sổ rất hẹp (thử lại ~21 phút) vì `SENT`/`FAILED` đã tự xóa subject và body (smoke quan sát với `SENT`), và chưa có môi trường production. Nhánh `SENT` của `scrubMentionsOf` chưa có assert riêng trong IT. | `V202611031500__dispatch_about_student.sql`; `SchedulingNotificationIT.java:164-199` | Không cần làm gì trước demo. Chỉ cần xem lại nếu chạy migration trên dữ liệu thật đang có email chờ gửi lại. |
| F11 | Thông tin | Runbook R10 chỉ là tài liệu, chưa diễn tập. Dòng log cần cho runbook có xuất hiện đúng định dạng và chỉ chứa UUID (`app2.log:54`). Chưa có test khẳng định dòng log này tồn tại, nên có thể bị sửa mà không ai biết. | `deploy/README.md:80-92`; `AnonymizationService.java:76-77` | Thêm assert dòng log vào test của F2. Diễn tập trên Docker local cùng `drill.sh` trước khi có dữ liệu thật. |
| F12 | Thông tin | `GET /students/{id}/progress` và `GET /assignments` trả 404 vì chưa có route trong `src/main` (thuộc U6). T131 ghép trang tóm tắt từ 2 API này (rest-api.md:43, tasks.md:321), nên #4 cần biết để xử lý khi chưa có. | `rest-api.md:43`; `tasks.md:321` | Báo #4: trang tóm tắt phải chịu được 404 cho tới khi U6 xong. |

## 5. Verdict

**FAIL** — Mọi test đều xanh (`spotless:check` PASS, `verify` BUILD SUCCESS, 94/0/0 unit và 96/0/0 IT, smoke bước 15, 16, 23 phần BE đúng), nhưng F1 tái hiện được trên app thật rằng HV 12 tuổi nhận email mời khi chưa có đồng ý giám hộ rồi kẹt hồ sơ, trái DoD U7 và FR-030; ngoài ra DoD log NFR-PRIV-02 chưa có test (F2). Sau khi sửa F1 và bổ sung test F2, đề xuất **PASS_WITH_NOTES**; F4 cần người dùng quyết định.

## 6. Review độc lập (reviewer, 2026-11-03)

**Phạm vi và cách làm:** Reviewer chỉ đọc tĩnh code, migration, test và spec; **không chạy gì** và không sửa file nào ngoài mục này. R1, R2, R3 và R5 được suy ra từ code, chưa tái hiện trên app. Theo constitution IV, cần viết test tái hiện trước khi sửa.

**Mã phát hiện:** R1, R2, ... dưới đây là mã riêng của mục review, khác với các mục R3, R10, R14, R17 trong kế hoạch U7.

### 6.1. Xác minh F1 và F4

**F1: xác nhận đúng, mức BLOCKING.**
- **Bằng chứng:** `UserAdminService.java:112-116` cho `AWAITING_GUARDIAN_CONSENT` → `INACTIVE`. `UserAdminService.java:124` đưa về `INVITED` chỉ dựa vào `password_hash == null`. `UserAdminService.java:135-138` và `InvitationService.java:57` chỉ kiểm trạng thái, không kiểm đồng ý. `GuardianConsentService.java:76-78` từ chối ghi đồng ý cho hồ sơ `INVITED`.
- **Mức độ:** Lớp chặn ở `InvitationService.java:104-106` vẫn giữ, nên phần "trước khi kích hoạt" của FR-030 chưa bị phá. Tuy vậy, S-US1.3 AC4 và DoD U7 bị vi phạm trực tiếp, và hồ sơ kẹt không có đường ra, nên vẫn là BLOCKING.
- **Cách sửa (đủ 4 điểm):**
  1. Chặn tại một điểm duy nhất ở `InvitationService.issueInvite`: thiếu đồng ý thì ném `GUARDIAN_CONSENT_REQUIRED` (S-US1.3 AC4 yêu cầu "chặn và yêu cầu ghi nhận đồng ý", nên mã này đúng hơn `INVALID_STATE_TRANSITION`). `GuardianConsentService` đã lưu đồng ý trước khi gọi hàm này; cần đảm bảo query `exists` thấy bản ghi vừa lưu (flush tự động của JPA).
  2. `changeStatus(ACTIVE)` khi chưa có mật khẩu: thiếu đồng ý thì về `AWAITING_GUARDIAN_CONSENT`, ngược lại về `INVITED`. Bỏ hàm trùng `UserAdminService.java:141-144`, dùng chung `needsGuardianConsent`.
  3. `PUT guardian-consent` nhận thêm `INVITED` thiếu đồng ý, để gỡ các hồ sơ đã kẹt.
  4. Sửa state machine `data-model.md:36-42`: hiện không nói gì về `AWAITING`/`INVITED` → `INACTIVE` → quay lại, đây là gốc của lỗi.
- **Lưu ý khi sửa:** `GuardianConsentIT.java:115-116` đang dùng `resend-invite` trên HV thiếu đồng ý để lấy token và mong 204, nên sẽ đỏ sau bước 1. Cần đổi test này thành kiểm tra 409, và tạo token cho ca `accept-invite` bằng cách khác.

**F4: xác nhận đúng; đề xuất nâng lên MAJOR cho 3 cột, riêng `payment.note` giữ làm rủi ro đã biết.** F4 đặt tất cả cột vào cùng một lựa chọn; reviewer tách ra vì chúng khác nhau:

| Cột | Ai nhập | Bằng chứng | Ý kiến |
|-----|---------|-----------|--------|
| `payment.note` | Giáo vụ | `Payment.java:46`, `V8__billing.sql:1,29` | **Rủi ro đã ghi trong thiết kế** (`research.md:220`), biện pháp là gợi ý ở UI (T114). Nên giữ nguyên nếu người dùng xác nhận, vì bảng này chỉ thêm (NFR-REL-03). Nếu xóa, phải dùng chuỗi thay thế vì `payment_adjustment_note_chk` cấm null với bản điều chỉnh. T114 chưa làm, nên hiện chưa có biện pháp nào có hiệu lực. |
| `session.cancel_reason` | **HV**, GV, giáo vụ | `SessionController.java:102,179`; `Session.java:243` | **Phải xóa.** FR-031 chỉ giữ buổi học ở "thời gian, giáo viên, trạng thái". HV tự gõ nội dung nên hướng dẫn giáo vụ không có tác dụng, và `research.md:220` không nói tới cột này. |
| `session_outcome_change.note` | GV, giáo vụ | `SessionController.java:106,191`; `SessionOutcomeChange.java:32` | **Phải xóa**, cùng lý do. |
| `teacher_assignment_history.reason` | Giáo vụ | `EnrollmentController.java:93`; `TeacherAssignmentHistory.java:28` | **Phải xóa**: lý do đổi GV thường nhắc tới học viên, và không nằm trong danh sách được giữ. |

**Cách sửa F4:** Trong `SystemCancelListener.on(StudentAnonymized)`, đặt `cancel_reason = NULL` cho mọi buổi của HV và `note = NULL` cho `session_outcome_change` tương ứng. Trong `catalog/AnonymizationListener`, xóa `reason` theo các ghi danh của HV. Giữ nguyên `cancelled_by`, `changed_by` và thời điểm để không mất phần "ai, khi nào" của NFR-PRIV-04. Dùng update có điều kiện, không tăng `@Version` của `session`, để listener vẫn idempotent. Sửa lại chú thích "chỉ thêm" ở `V202610181200__session_outcome_change.sql` bằng tài liệu (không sửa migration) và cập nhật `research.md:213,220`. Thêm assert vào `AnonymizationIT`.

`availability_exception.reason` là dữ liệu của GV và không gắn với HV nên không cần xử lý. `data_subject_request.resolution_note` thuộc bằng chứng tuân thủ; xem R7.

### 6.2. Đánh giá các sai lệch so với plan

| Sai lệch | Đánh giá | Ghi chú |
|----------|----------|---------|
| Migration `about_student_id` | **Chấp nhận** | Cần cho FR-031 vì không còn cách nào khác để tìm email PENDING nhắc tới HV. Là migration mới, không sửa cái cũ; `data-model.md:434,444` đã cập nhật. Không backfill thì chấp nhận được (F10). |
| Chặn gửi tới `@invalid` | **Chấp nhận, nhưng chưa đủ** | Đúng cho event tra `UserDirectory` lúc xử lý. Không có tác dụng với 2 event mang sẵn email cũ (R2). Hậu tố đang là quy ước ngầm giữa `UserAccount.java:113` và `DispatchService.java:25`; xem R13. |
| Xóa `password_hash` | **Chấp nhận, nên giữ** | Không gây lỗi: login có `dummyHash` và `hash != null` (`AuthService.java:60-65`), thời gian phản hồi vẫn giữ. `changeStatus` chặn hồ sơ ẩn danh trước khi đọc hash. CHECK và cột cho phép null. Thiếu cập nhật `data-model.md:32`; xem R11. |

### 6.3. Phát hiện mới

| # | Mức | Mô tả và lý do | File:dòng | Đề xuất sửa |
|---|-----|---------------|-----------|-------------|
| R1 | **BLOCKING** | **Ghi đè mất cập nhật có thể khôi phục toàn bộ định danh sau ẩn danh hóa.** `UserAccount` không có `@Version`, không có `@DynamicUpdate`, nên Hibernate ghi lại mọi cột. `AnonymizationService` khóa dòng, nhưng các luồng khác đọc không khóa rồi ghi. Ví dụ: login đọc user → chạy BCrypt → anonymize commit → login flush `recordLogin`. Câu UPDATE của login ghi lại email, tên, số điện thoại, ngày sinh, `password_hash`, `status=ACTIVE`, `anonymized_at=NULL`. Yêu cầu vẫn `COMPLETED`, listener các module đã chạy, không có lỗi hay log nào. Login còn tạo refresh token mới sau bước thu hồi. Các đường tương tự: `acceptInvite`, `resetPassword` (đều có BCrypt nên cửa sổ rộng hơn), `PATCH /me`, `changeStatus`. Xác suất thấp, nhưng hậu quả trái FR-031 ("không khôi phục được") và không ai phát hiện được. Không có test nào phủ. | `UserAccount.java:17-19`; `V2__identity.sql:3-23`; `AuthService.java:59-71`; `InvitationService.java:99-108`; `PasswordResetService.java:59-65`; `MeController.java:74-75`; `UserAdminService.java:103-124` | Migration mới thêm `version BIGINT NOT NULL DEFAULT 0` và `@Version` cho `UserAccount`. Vì `@Version` sẽ làm hai lần login song song của cùng người bị 409, đổi `recordLogin` thành `UPDATE ... SET last_login_at ... WHERE id = ? AND status = 'ACTIVE' AND anonymized_at IS NULL`; trả 0 dòng thì 401 và không cấp token. IT tái hiện: tải user trong transaction A, chạy anonymize, rồi sửa và commit A; khẳng định A thất bại và `anonymized_at` vẫn còn. |
| R2 | **MAJOR** | **Event mang sẵn email cũ vượt qua được chặn `@invalid` và để lại địa chỉ thật.** `UserInvited` và `PasswordResetRequested` chở email, họ tên và token thô, và `IdentityNotificationListener` dùng thẳng các trường đó. Nếu event đang chờ hoặc được phát lại sau restart (`republish-outstanding-events-on-restart: true`) mà ẩn danh hóa đã xong, thì: (a) email vẫn gửi tới địa chỉ thật của người đã yêu cầu xóa; (b) dòng `notification_dispatch` mới có `recipient` là email thật, và nếu listener ẩn danh hóa đã chạy trước thì **không còn gì che nữa**, vì `markSent` không xóa `recipient`; (c) trong lúc chờ, `event_publication.serialized_event` chứa email, tên và token, và ẩn danh hóa không dọn phần này. Không có thứ tự giữa các listener, nên không thể dựa vào thứ tự. `noEmailGoesToAnonymizedAddressFromLateEvents` chỉ phủ `SessionReminderDue`, là event chỉ mang UUID. Kết quả "`event_publication` còn 0 dòng" của smoke chỉ đúng khi không có event đang chờ. | `UserInvited.java:10`; `PasswordResetRequested.java:6`; `IdentityNotificationListener.java:25-49`; `DispatchService.java:67`; `NotificationDispatch.java:98-105`; `AnonymizationIT.java:354-385` | Bỏ email và tên khỏi 2 event (chỉ giữ `userId`, `tokenId`, token). Listener tra `UserDirectory` khi xử lý; không thấy user thì bỏ qua, còn `@invalid` đã được `notify` chặn. Cách này xử lý được cả (a), (b), (c). Token thô còn lại đã bị thu hồi nên vô dụng. Cập nhật `events-and-ports.md:16`. Thêm IT phát `UserInvited` sau khi ẩn danh hóa. |
| R3 | **MAJOR** | **`ConsentGateFilter` bị vượt qua bằng đường dẫn mã hóa phần trăm.** Filter so `getRequestURI()` (chuỗi thô) với `/api/v1/`, trong khi Spring MVC giải mã từng đoạn đường dẫn khi khớp route, và `StrictHttpFirewall` không cấm các ký tự thường được mã hóa như `%31`. Vì vậy `GET /api/v%31/sessions` có `checked()=false`, filter cho đi qua, nhưng vẫn vào được controller. Hậu quả: người chưa đồng ý chính sách dùng được mọi API (FR-030, S-US1.11 AC5); tài khoản `INACTIVE` hoặc đã ẩn danh hóa với access token còn hạn (≤ 15 phút) vẫn gọi được API, kể cả `PATCH /me` để ghi lại tên và số điện thoại lên hồ sơ đã ẩn danh. JWT và `@PreAuthorize` vẫn có hiệu lực, nên không phải leo thang vai trò. Logic `checked()` đọc từ code là chắc chắn; việc route khớp đầu-cuối cần xác nhận bằng test HTTP thật. | `ConsentGateFilter.java:78-85`; `ConsentGateFilterTest.java:29-35` | Lấy đường dẫn đã giải mã (`UrlPathHelper.getPathWithinApplication` hoặc matcher của Spring Security), và đảo logic thành chặn mặc định: mọi request đã xác thực đều bị kiểm, trừ danh sách miễn trừ tường minh (`/api/v1/auth/**`, health, Swagger). Thêm test cho `/api/v%31/sessions` và `/api/v1/m%65/consents`. |
| R4 | **MAJOR** | **Ghi lại được dữ liệu cá nhân mới vào hồ sơ đã ẩn danh.** Hợp đồng ghi 409 `ANONYMIZED` cho "thao tác trên hồ sơ đã ẩn danh hóa", nhưng chỉ identity kiểm tra. Ghi danh vẫn `ACTIVE`, nên `isAssigned` vẫn đúng. Do đó giáo vụ gọi được `PUT /students/{id}/profile` (nhập lại `notes`, `occupation`); GV được phân công tạo được mục tiêu, đánh giá năng lực và lộ trình; giáo vụ đặt được buổi `BOOKED` mới cho HV đã ẩn danh. Listener đã chạy xong và không chạy lại, nên dữ liệu này tồn tại mãi. | `ProfileService.java:101-107`; `StudentAccessGuard.java:40-45`; `ProficiencyService.java:66,105,126`; `LearningPathService.java:52,76`; `BookingService.java:66-73`; `rest-api.md:151` | Thêm `anonymized` vào `UserView` (cũng dùng được cho R13). Ném `ANONYMIZED` ở `updateStudentProfile`, `StudentAccessGuard.requireWrite` và `BookingService.create`. Vẫn cho ghi thanh toán vì công nợ được giữ. Thêm các ca này vào `anonymizedStudentCanNeitherLogInNorBeReactivated`. |
| R5 | MINOR | **Dữ liệu ghi muộn sau khi listener dọn đã chạy xong.** Listener thông báo đọc `UserView` một lần, rồi gửi SMTP ngay trong transaction chưa commit (timeout mỗi lần tới 10 s, `application.yml:31-33`). Nếu `NotificationAnonymizationListener` chạy trong lúc đó, nó không thấy các dòng chưa commit: sau đó sẽ còn `recipient` thật của HV và thông báo trong app có tên HV gửi GV. Tương tự, `BookingService` chưa commit lúc `SystemCancelListener` đang quét sẽ để lại một buổi `BOOKED` tương lai. Cửa sổ hẹp, nhưng không có gì phát hiện được. | `SchedulingNotificationListener.java:50-77`; `DispatchService.java:75-93`; `NotificationAnonymizationListener.java:29-35`; `SystemCancelListener.java:79-85` | Cách đơn giản nhất: job hằng ngày (ShedLock) tìm HV ẩn danh hóa trong 48 giờ qua và chạy lại các thao tác dọn idempotent (cần API đọc mới trên `UserDirectory`). Nếu chưa làm trước demo, ghi vào rủi ro còn lại của R16. |
| R6 | MINOR | **Yêu cầu báo `COMPLETED` dù việc dọn ở module khác có thể đã lỗi.** Listener lỗi (ví dụ `SystemCancelListener` xung đột `@Version` với buổi đang được thao tác) thì rollback cả lô và publication nằm đó tới lần restart sau; không có `IncompleteEventPublications.resubmit...`. Đúng với thiết kế NFR-REL-04 (`nfr.md:160`), nhưng giáo vụ không thấy gì, trong khi dữ liệu học tập còn nguyên và buổi `BOOKED` còn hoạt động. VPS ít khi restart. | `AnonymizationService.java:74-75`; `SystemCancelListener.java:74-77,88-105`; `application.yml:41-42` | Job mỗi 5 phút gọi `IncompleteEventPublications.resubmitIncompletePublicationsOlderThan(...)` (ShedLock). Tối thiểu: ghi vào runbook việc kiểm tra `event_publication` sau khi ẩn danh hóa. Kết hợp với F8 khi sửa log. |
| R7 | NOTE | **`requester_name` có thể chính là tên HV.** `data-model.md:95` chủ đích giữ cột này nên không phải vi phạm. Tuy nhiên `student_id` đi kèm cho phép nối tên với mọi buổi và thanh toán còn lại, và khi HV tự yêu cầu thì `pg_dump` vẫn có tên HV. Kết quả "0 dòng chứa tên" ở smoke và IT có được một phần vì người yêu cầu là người khác (`AnonymizationIT.java:129`). Ngoài ra FR-031 chỉ nói giữ "bản ghi yêu cầu xóa", còn `ACCESS` cũng đang giữ tên. | `DataSubjectRequest.java:14-15,35-36`; `AnonymizationService.java:65-75` | Đưa vào danh sách cần pháp lý xác nhận (Q2) và nêu trong thông báo xử lý dữ liệu. Xem xét che `requester_name` của bản `ACCESS` khi ẩn danh hóa. |
| R8 | MINOR | **Ghi danh vẫn `ACTIVE` sau ẩn danh hóa gây tác dụng phụ.** `TeacherDeactivationGuard` chặn ngừng GV cho tới khi đổi GV cho một HV không còn tồn tại trên thực tế. HV ẩn danh vẫn trong danh sách của GV, và vì mục tiêu bị xóa nên bị gắn cờ "Cần tư vấn". `EnrollmentRenewalDue` tiếp tục báo admin "Học viên Học viên đã ẩn danh cần gia hạn" (`on(EnrollmentRenewalDue)`, job thuộc U8). | `TeacherDeactivationGuard.java:27-30`; `ProfileService.java:125-128,157`; `CatalogNotificationListener.java:130-140` | Giữ nguyên trạng thái ghi danh (FR-031, SC-007), nhưng loại ghi danh của HV ẩn danh khỏi guard, danh sách, cờ tư vấn và job gia hạn; dùng `UserView.anonymized` hoặc một cột trên `enrollment`. Bàn giao cho U8. |
| R9 | MINOR | **S-US1.12 AC2 lệch với cài đặt.** AC yêu cầu đồng ý giám hộ lưu "kèm phiên bản chính sách, người ghi nhận và thời điểm ghi nhận". Bảng không có phiên bản chính sách, DTO không trả thời điểm. `update` ghi đè `recorded_by` và thông tin cũ mà không có lịch sử. FR-030 và `data-model.md:75-87` không yêu cầu phiên bản, nên tài liệu đang tự mâu thuẫn. | `GuardianConsent.java:50-61`; `V2__identity.sql:51-62`; `GuardianConsentController.java:32-38`; `stories.md:243` | Người dùng chốt một trong hai: sửa AC theo FR-030, hoặc thêm `policy_version` (migration mới) và trả `recordedAt`. |
| R10 | MINOR | **Thiếu test quét toàn CSDL.** Kiểm tra "định danh không còn" ở `AnonymizationIT` là danh sách bảng viết tay, nên sẽ không phát hiện được cột tự do như F4 hoặc bảng mới của U6. Kết quả `pg_dump` chỉ là quan sát một lần của QE. | `AnonymizationIT.java:230-257` | Thêm IT duyệt `information_schema.columns` (mọi cột text/varchar), tìm email, tên, số điện thoại mẫu của HV; chỉ miễn trừ `data_subject_request.requester_name`. U6 kế thừa test này mà không phải sửa. |
| R11 | MINOR | **Tài liệu chưa khớp code:** `data-model.md:32` chưa ghi `password_hash = null`; `research.md:220` chỉ nêu `payment.note`. | `data-model.md:32`; `research.md:213,220` | Cập nhật khi sửa F1 và F4. |
| R12 | MINOR | **Thứ tự trong runbook khó làm đúng.** Mục "trước khi mở lại hệ thống" yêu cầu gọi API, nhưng phải start backend mới gọi được, và lệnh khôi phục ở trên đã start nó, tức là hệ thống đã mở. Runbook cũng luôn yêu cầu tạo yêu cầu `ERASURE` mới, trong khi bản dump có thể đã có yêu cầu `RECEIVED` của chính HV đó, để lại bản treo (F9). Các quyết định chỉ làm tài liệu của R10 kế hoạch được giữ nguyên. | `deploy/README.md:73-78,83-88` | Ghi rõ: lưu log trước khi khôi phục; chặn truy cập công khai (dừng reverse proxy) trong lúc áp dụng lại; dùng lại yêu cầu `RECEIVED` nếu có. |
| R13 | NOTE | Hậu tố `@invalid` là quy ước ngầm giữa 2 module; `@Email` cũng chấp nhận địa chỉ thật dạng `x@invalid` (bị bỏ qua không báo, ảnh hưởng không đáng kể). | `DispatchService.java:25,67`; `UserAccount.java:113` | Khi có `UserView.anonymized` (R4), có thể chuyển sang dùng cờ này cho các event tra `UserDirectory`. |
| R14 | NOTE | Thay đổi U7 có 23 mục chưa commit, nhiều khả năng vượt mục tiêu PR ~400 dòng; có sửa bảo mật và schema nên cần leader review theo constitution. | `constitution.md:95-98` | Tách PR nếu còn kịp; nêu lý do trong mô tả PR nếu không. |

**Các điểm đã kiểm và không thấy vấn đề:**
- **Phân quyền:** `@PreAuthorize("hasRole('ADMIN')")` ở cấp class cho 2 controller mới, có test 2 vai trò.
- **Thông báo do ẩn danh hóa:** email hủy buổi cause `ANONYMIZED` không gắn `about_student_id`, không có tên HV và không gửi HV (`SchedulingNotificationListener.java:101,116`), nên không phụ thuộc thứ tự listener.
- **Email nhắc HV:** mọi lời gọi gửi cho GV/admin có nhắc tên HV đều truyền `aboutStudentId` (Scheduling, Catalog, Learning). Template hủy buổi không có tên HV.
- **Job gửi lại:** `@Version` cộng `REQUIRES_NEW` xử lý đúng xung đột với bulk update.
- **Listener:** catalog, learning, notification, scheduling đều idempotent khi phát lại.
- **Hai yêu cầu xóa song song:** bên sau chờ khóa dòng user rồi nhận `ANONYMIZED` (`AnonymizationService.java:58-64`). Thứ tự khóa không tạo deadlock với `GuardianConsentService`.
- **Các đường mời khác:** không có đường mời nào khác ngoài F1; `date_of_birth` không có API sửa, CHECK trong DB cấm HV thiếu ngày sinh.
- **Bảng còn lại:** `auth_token` chỉ giữ hash; `policy_consent` chỉ có UUID, phiên bản, thời điểm; `student_profile` xóa đủ 3 cột; `receivable` chỉ có UUID và số tiền.

### 6.4. Đối chiếu constitution

| Nguyên tắc | Kết quả | Bằng chứng |
|------------|---------|-----------|
| I. Modular Monolith | Đạt | Identity chỉ phát event; `ModularityTests` pass theo báo cáo QE. |
| II. Provider-Agnostic | N/A | U7 không thêm provider. |
| III. Free-First, YAGNI | Đạt | Không thêm hạ tầng. |
| IV. Kiểm thử trọng yếu | **Vi phạm** | Không có test cho F1, R1, R3 trên logic phân quyền và quyền riêng tư; F2 còn mở. Cần test tái hiện trước khi sửa. |
| V. Bảo mật và dữ liệu cá nhân | **Vi phạm** | F1, R1, R2, R3, R4, F4. Phân quyền theo vai trò ở các endpoint mới và quy tắc Flyway thì đạt (không sửa migration cũ). |
| Quy trình, DoD | Chưa đạt | CI chưa chạy, FE ngoài phạm vi, chưa có PR. |

### 6.5. Verdict đề xuất của reviewer

**FAIL** — F1 vi phạm trực tiếp DoD U7 và S-US1.3 AC4, còn R1 có thể âm thầm khôi phục định danh đã xóa, trái FR-031 và constitution V.

**Điều kiện để đạt PASS_WITH_NOTES:** sửa F1, R1, R2, R3, R4 và F4 (3 cột), mỗi lỗi có test tái hiện, đồng thời hoàn thành test F2. R5, R6 và các mục MINOR còn lại có thể chuyển thành nợ có ghi nhận. Người dùng cần chốt `payment.note`, R7 và R9.

### 6.6. Re-review (2026-11-03)

**Cách làm:** Reviewer chỉ đọc tĩnh code, test và tài liệu hiện tại; **không chạy test, không xem được `git diff`**. Các số 95 unit + 103 IT, `BUILD SUCCESS` và "test đỏ trước khi sửa" là kết quả do orchestrator báo, reviewer chưa tự xác minh.

**Verdict đề xuất: PASS_WITH_NOTES, với một điều kiện** — không còn lỗi BLOCKING, nhưng N1 phải được sửa kèm test trước khi commit, hoặc người dùng ghi nhận chấp nhận rủi ro; nếu không thì giữ FAIL. N1 là đường ghi còn sót của R4, xảy ra theo trình tự thông thường, không cần race.

#### Trạng thái từng lỗi

| Mã | Trạng thái | Đánh giá | Test có kiểm đúng lỗi không |
|----|-----------|----------|------------------------------|
| F1 | **Đóng** | `issueInvite` là điểm chặn chung (`InvitationService.java:60-62`), `reactivatedStatus` đúng (`UserAdminService.java:139-146`), hồ sơ `INVITED` thiếu đồng ý gỡ được (`GuardianConsentService.java:77-83`). Thứ tự lưu đồng ý rồi mới `issueInvite` được test luồng chính xác nhận. Còn một điểm nhỏ ở N5. | **Có.** Phủ chuỗi `INACTIVE` → `ACTIVE` → `resend-invite` và hồ sơ đã kẹt (`GuardianConsentIT.java:129-168`). Test đếm event `UserInvited`; event không phát thì không có email. |
| R1 | **Đóng; còn nhánh nhỏ N3** | `@Version` và migration đúng; dòng ghi đè toàn bộ hồ sơ giờ thất bại. Đã rà các luồng ghi `UserAccount`: `acceptInvite`, `resetPassword`, `PATCH /me`, `changeStatus`, `GuardianConsentService`. Xung đột đều trả 409 `CONCURRENT_MODIFICATION` qua `GlobalExceptionHandler.java:65-71`, kể cả `@Transactional` ở `MeController`. Login song song không va nhau vì `recordLogin` là JPQL, không tăng version. Không thấy hồi quy. | **Có, nhưng gián tiếp.** `staleWriteCannotRestoreIdentity` kiểm cơ chế version qua `merge`, đúng bản chất lỗi. Chưa có test nào cho login chạy chen với anonymize (nhánh N3). |
| R2 | **Đóng phần (a), (b); phần (c) còn mở (N4)** | Listener đọc lại người nhận (`IdentityNotificationListener.java:36,53`), nên email cũ trong event không còn được dùng. Event vẫn mang email, tên và token. | **Có.** `lateIdentityEventsDoNotEmailOldAddress` kiểm cả email gửi đi lẫn dòng `notification_dispatch`. |
| R3 | **Đóng ở mức filter** | Đã rà: `UrlPathHelper` giải mã giống cách MVC khớp route; mặc định kiểm; không tìm thấy đường dẫn nào được miễn mà vào được controller nghiệp vụ, vì `StrictHttpFirewall` chặn `..`, `%2e`, `%2f`, `%3b`. Đường dẫn lạ như `/api/v1/me/` bị coi là guarded, tức an toàn. **OPTIONS:** preflight không có `Authorization` nên `CurrentUser.find()` rỗng và không bị chặn; không có hồi quy. `/error` được miễn và không có dữ liệu nghiệp vụ. | **Có ở mức filter**, cả test đơn vị và MockMvc qua `percentEncodedPathDoesNotBypassGate`. Chưa thử trên Tomcat thật: rủi ro thấp, nhưng nên `curl` một lần trong T140. |
| R4 | **Đóng một phần; xem N1, N2** | Đã chặn ở 4 điểm được liệt kê. `reschedule` cũng được chặn qua `BookingService.create`. `OutcomeService` và `CancellationService` chưa được xử lý. | **Một phần.** Có profile, goals, assessments, đặt buổi. Thiếu `LearningPathService.create/update` và `changeTeacher`. |
| F4 | **Đóng cho 3 cột**; `payment.note` giữ theo quyết định của người dùng | Listener xóa đủ 3 cột; JPQL idempotent. **Đính chính:** ở 6.1 reviewer đề xuất không tăng `@Version`; đã kiểm lại và cách đó an toàn, vì buổi có `cancel_reason` không còn luồng nào ghi lại (`Session.java:137,149,159,172,190-192,214`). Việc xóa ghi chú không bị ghi đè bởi bản `Session` cũ. | **Có.** Test đưa tên vào cả 3 cột (`AnonymizationIT.java:193,203,225`) rồi quét. |
| F2 | **Đóng một phần** | `LogPrivacyIT` phủ mật khẩu, token mời và token reset qua accept-invite, login, forgot/reset. Chưa kiểm access/refresh token và các luồng refresh, anonymize mà F2 nêu. | **Có cho phần đã phủ.** Đáp ứng `nfr.md:184` ở mức tối thiểu. |
| R10 | **Đóng; xem N6** | `columnsContaining` quét mọi cột `text`/`varchar`, kể cả `event_publication`. | **Có**, nhưng thiếu kiểm chứng rằng bản thân phép quét tìm được dữ liệu (N6). |
| R7 | **Đã chuyển** | Ghi ở `research.md:224` là OQ-7. | N/A |
| R9, R11, R12 | **Đóng** | `stories.md:243`, `data-model.md:32-33,42`, `research.md:220-224`, `deploy/README.md:83-95`. Tài liệu khớp code. Runbook vẫn chưa diễn tập. | N/A |

**Việc bỏ `underageStudentNeedsGuardianConsent` khỏi `AuthFlowIT`:** chấp nhận được, vì lớp chặn cuối ở `accept-invite` vẫn được kiểm bằng `GuardianConsentIT.java:108-125`.

#### Phát hiện mới

| # | Mức | Mô tả và lý do | File:dòng | Đề xuất sửa |
|---|-----|---------------|-----------|-------------|
| N1 | **MAJOR** | **Vẫn ghi được văn bản tự do mới vào buổi của HV đã ẩn danh.** Buổi `BOOKED` đã qua giờ không bị listener hủy và vẫn chờ chốt. Sau ẩn danh hóa, GV hoặc giáo vụ chốt kết quả kèm `note`, giáo vụ sửa kết quả trong 7 ngày kèm `note`, hoặc GV hủy kèm `reason` (`cancelByTeacher` không giới hạn thời gian). Listener đã chạy xong nên văn bản này còn mãi, đúng ở các cột F4 yêu cầu xóa. Trình tự này xảy ra bình thường: buổi hôm qua chưa chốt, hôm nay ẩn danh hóa, GV chốt và ghi tên HV. | `OutcomeService.java:49-68`; `CancellationService.java:61-67`; `Session.java:148-152`; `SessionController.java:179,191` | **Không được chặn thao tác**, vì chốt buổi là cần thiết cho số buổi đã dạy và SC-007. Chỉ bỏ văn bản: thêm `UserDirectory.isAnonymized(id)`; khi đúng thì lưu `note`/`reason` là null. Thêm IT: HV có buổi đã qua giờ → ẩn danh hóa → chốt kết quả kèm ghi chú và GV hủy kèm lý do → trạng thái đổi, cột văn bản null, `columnsContaining` rỗng. |
| N2 | MINOR | **Chặn `changeTeacher` làm đường ra duy nhất cho GV có HV đã ẩn danh là hủy ghi danh.** `TeacherDeactivationGuard` vẫn tính ghi danh `ACTIVE` của HV ẩn danh, còn đổi GV giờ trả 409. Không phải kẹt hẳn: giáo vụ có thể `POST /enrollments/{id}/cancel` khi `sessions_reserved = 0`, nhưng đường này không được ghi ở đâu, đổi trạng thái một ghi danh mà FR-031 yêu cầu giữ, và có thể ảnh hưởng báo cáo U8. Chưa có test. | `EnrollmentService.java:116`; `TeacherDeactivationGuard.java:27-30`; `Enrollment.java:193-202` | Làm cùng R8: loại ghi danh của HV ẩn danh khỏi guard. Nếu cần giữ đổi GV, cho phép nhưng lưu `reason` null. |
| N3 | MINOR | **`login` bỏ qua kết quả `recordLogin`.** Nếu anonymize commit giữa lúc chạy BCrypt, update trả 0 dòng nhưng login vẫn trả 200 và tạo thêm một refresh token chưa dùng sau bước thu hồi. Token này không dùng được vì `refresh` và filter đều kiểm trạng thái, nên không có rủi ro truy cập. Điều kiện update cũng chưa có `status = 'ACTIVE'`, nên tài khoản vừa bị ngừng cũng rơi vào nhánh tương tự. | `AuthService.java:70-71`; `UserAccountRepository.java:29-30` | Thêm điều kiện `status`; trả khác 1 dòng thì ném `UNAUTHENTICATED` để rollback, không tạo token. |
| N4 | MINOR | **R2 phần (c) còn mở.** Khi publication chờ hoặc lỗi, `event_publication.serialized_event` vẫn chứa email và tên của HV đã ẩn danh. Email và tên trong event giờ thừa, listener không dùng. | `UserInvited.java:10`; `PasswordResetRequested.java:6`; `InvitationService.java:73-80`; `PasswordResetService.java:80-81` | Bỏ 2 trường này khỏi event; cập nhật `events-and-ports.md:16`, `LogPrivacyIT.java:58,81` và test liên quan. |
| N5 | MINOR | `resend-invite` với hồ sơ `AWAITING_GUARDIAN_CONSENT` trả `INVALID_STATE_TRANSITION` vì kiểm trạng thái trước. S-US1.3 AC4 yêu cầu "chặn và yêu cầu ghi nhận đồng ý", nên giao diện sẽ không báo được đúng việc cần làm. Test chỉ kiểm 409. | `UserAdminService.java:132-134`; `GuardianConsentIT.java:137-138` | Trả `GUARDIAN_CONSENT_REQUIRED` cho trạng thái này và assert mã lỗi. |
| N6 | MINOR | **Độ tin cậy của test ẩn danh hóa.** (a) `anonymizationRemovesIdentityAndKeepsFigures` không chờ listener trước khi ẩn danh hóa: `SessionBooked` đang xử lý có thể ghi thông báo có tên sau bước dọn (R5), làm test đỏ ngẫu nhiên. QE trước đây chỉ chạy 1 lần. (b) Phép quét không tự kiểm tra: nếu query sai schema, nó trả rỗng và test vẫn xanh. (c) Phép quét bỏ qua `jsonb` và `text[]`. (d) Miễn trừ `payment.note` không cần thiết trong test này vì không có ghi chú nào, nhưng sẽ che lỗi tương lai ở cột đó. | `AnonymizationIT.java:199-236,301-303,326-347` | (a) Gọi `awaitListenersDone` ngay trước `request(...)`. (b) Assert `columnsContaining(name)` khác rỗng trước khi ẩn danh hóa. (c) Thêm kiểu cột khi U6 có `jsonb`. Chạy lại `AnonymizationIT` nhiều lần. |

#### Các điểm đã kiểm và không thấy vấn đề

- `@Version` không ảnh hưởng `requireNotAnonymized`, `existsByIdAndStatus`, `UserDirectoryImpl` vì chỉ đọc; `AnonymizationService` và `GuardianConsentService` khóa rồi đọc bản mới nên không tự xung đột.
- Migration `V202611031600` mới, không sửa migration cũ; `DEFAULT 0` an toàn với dữ liệu sẵn có.
- `IdentityNotificationListener` đọc người dùng sau commit của transaction phát event, nên luồng mời bình thường vẫn thấy user.
- `LearningPathService` đi qua `requireWrite` ở cả create và update nên đã được chặn, dù chưa có test (đã nêu ở R4).
- Constitution: nguyên tắc I, II, III không đổi. Nguyên tắc V đạt sau khi sửa N1. Nguyên tắc IV đạt cho các lỗi đã sửa, theo báo cáo "đỏ trước khi sửa" của orchestrator.

#### Mục còn lại và nơi nên chuyển

| Nơi chuyển | Mục |
|-----------|-----|
| **Trước khi commit U7** | **N1** (bắt buộc). Nên làm cùng vì nhỏ: N3, N5, N6 (a) (b). Chạy lại toàn bộ `verify`. |
| **U6** | Mở rộng listener và chạy lại `AnonymizationIT` (đã có trong DoD). Áp dụng bài học R4, N1 cho `PUT /sessions/{id}/report`, bài tập, bài nộp, feedback: chặn ghi mới hoặc bỏ văn bản tự do trên hồ sơ đã ẩn danh. Bổ sung lộ trình và đánh giá năng lực vào test chính (F6). N6 (c). F12: `progress`, `assignments`. |
| **U8** | Chạy lại với 3 báo cáo thật (đã chốt). R8 và N2: guard, danh sách, cờ "Cần tư vấn", job gia hạn và `extend` đối với HV ẩn danh; xác định ghi danh `CANCELLED` ảnh hưởng công nợ thế nào. |
| **T140 (rà soát bảo mật, B7)** | Ma trận vai trò có các endpoint privacy và thiếu test `@PreAuthorize`. `curl` `/api/v%31/sessions` trên server thật (R3). F5: gọi API nghiệp vụ thật sau khi đổi phiên bản. F7: gọi `/auth/refresh` bằng token cũ. Mở rộng `LogPrivacyIT` cho refresh, access/refresh token, anonymize và dòng log runbook (F2, F11). |
| **FE (T130, T131, R17)** | Xử lý `GUARDIAN_CONSENT_REQUIRED`, `ANONYMIZED`, và 404 của F12. T114: gợi ý không ghi tên trong ghi chú thanh toán, đây là biện pháp duy nhất cho `payment.note` nên phải có trước khi dùng dữ liệu thật. |
| **Trước khi deploy** | Diễn tập runbook khôi phục (F11, R12). R6: job resubmit, hoặc tối thiểu thêm kiểm tra `event_publication` vào runbook. R5: job dọn lại, hoặc ghi vào rủi ro R16. N4. F3: bỏ mật khẩu khỏi `IdentityDemoSeed.java:74`. F8: log sau commit. F9: chốt cách đóng yêu cầu còn treo. Chạy CI trên PR và gitleaks. Leader review (R14). |
| **Pháp lý trung tâm (trước khi có dữ liệu thật)** | OQ-7 (`requester_name`), Q2, Q3, mốc 16 tuổi, nêu thời hạn 7 ngày của bản sao lưu trong thông báo xử lý dữ liệu. |
| **Không cần làm trước demo** | F10, R13. |

## 7. Kết quả cuối sau khi sửa (orchestrator, 2026-11-03)

- **Build & test:** `./mvnw verify` BUILD SUCCESS, **95 unit + 105 IT**, 0 lỗi; `spotless:check` không cache PASS. Chạy 1 lần trên máy dev; chưa chạy CI, gitleaks.
- **Test đỏ trước khi sửa (quan sát được):** F1, R1, R2, R3, R4, F4, N1, N5. F2 (`LogPrivacyIT`) xanh ngay từ đầu vì log không lộ gì. N3 chỉ có test mức repository (`recordLogin` trả 0), không có test đua thật giữa login và ẩn danh hóa.
- **Sửa sau re-review 6.6:** N1, N3, N5, N6 (a)(b). Các sửa này **chưa qua review độc lập lần nữa** và smoke BE ở mục trước chưa chạy lại trên bản cuối.
- **R3:** kiểm bằng MockMvc, chưa thử trên Tomcat thật; `curl` một lần trong T140.
- **Quyết định người dùng (2026-11-03):** giữ `payment.note`; R7 → OQ-7; R9 sửa story, không lưu phiên bản chính sách.
- **Chuyển tiếp:** U6: chặn ghi mới hoặc bỏ văn bản tự do ở phiếu, bài tập, bài nộp, feedback; F6, F12; N6 (c). U8: chạy lại với báo cáo thật; R8, N2; ảnh hưởng của ghi danh CANCELLED lên công nợ. T140: ma trận phân quyền, `curl` R3, F5, F7, mở rộng `LogPrivacyIT`. FE: mã lỗi mới, 404 của F12, T114. Trước deploy: diễn tập runbook; R5, R6, N4, F3, F8, F9; CI, gitleaks, leader review. Pháp lý: OQ-7, Q2, Q3, mốc 16 tuổi, thời hạn 7 ngày của bản sao lưu.
- **Verdict đề xuất: PASS_WITH_NOTES.**
