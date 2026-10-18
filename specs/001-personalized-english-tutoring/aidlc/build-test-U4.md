# Build & Test Report — U4 — Nền tảng đào tạo tiếng Anh 1 kèm 1 cá nhân hóa (001-personalized-english-tutoring)

- **Ngày (UTC)**: 2026-10-18, 13:50–13:56 UTC (giờ máy: 20:50–20:56 +07)
- **Unit**: U4, gồm hồ sơ năng lực, mục tiêu và lộ trình (B3), **chỉ phần BACKEND**. FE T088–T089 thuộc #3 và nằm ngoài phạm vi (vẫn `[ ]` trong tasks.md:212-213).
- **Tasks trong phạm vi**: T080–T087, T090. Hoàn thành **9/9** (tất cả `[X]` trong tasks.md:201-211, 214).
  - OQ-4 đã có lời giải: SPI `catalog.GoalStatusProvider` (`Set<UUID> studentsWithActiveGoal(studentIds)`, events-and-ports.md:51). catalog định nghĩa interface (`catalog/GoalStatusProvider.java`), learning cài (`learning/internal/proficiency/GoalStatusProviderImpl.java`). catalog lấy SPI qua `ObjectProvider` (`catalog/internal/profile/ProfileService.java:39,146`), không gọi thẳng learning. Trong code không còn `LearningProfileApi`, nhưng tasks.md:209 (T085) vẫn ghi tên cũ (xem F4).
  - Lần smoke đầu tiên, sau U3 có cờ `needsConsulting`: admin chỉ thấy student4, đúng với seed T090.
- **Môi trường**: Windows 11, Git Bash; JDK 21.0.12 (`JAVA_HOME=/c/Program Files/Java/jdk-21.0.12`); Docker Desktop (compose postgres, mailpit, livekit đang chạy); Testcontainers `postgres:16`; Node 22.21.0 + npm.

## 1. Build

| Thành phần | Lệnh | Kết quả | Ghi chú |
|------------|------|---------|---------|
| Backend (Maven, gồm Enforcer) | `cd backend && ./mvnw -B verify` | **PASS** (`BUILD SUCCESS`, exit 0, 01:58 min) | `RequireJavaVersion passed`, `BannedDependencies passed`. Log không có dòng ` ERROR ` nào. |
| Backend format | `cd backend && ./mvnw -B spotless:check` | **PASS** (`BUILD SUCCESS`, 1.8 s) | Không có vi phạm format. |
| Frontend lint | `cd frontend && npm run lint` (`oxlint src`) | **PASS** (exit 0; 0 lỗi, có cảnh báo) | Cảnh báo đều là `react(only-export-components)`, giống U3. FE không đổi ở U4. |
| Frontend format | `cd frontend && npx prettier --check src` | **PASS** | "All matched files use Prettier code style!" |
| Frontend build | `cd frontend && npm run build` (`tsc -b && vite build`) | **PASS** (built in 2.60 s) | Chunk `index-*.js` 496.88 kB (gzip 154.27 kB), giống U3. |

## 2. Test

| Bộ test | Lệnh | Pass / Fail / Skip | Ghi chú |
|---------|------|--------------------|---------|
| Unit + Scenario (surefire) | `./mvnw -B verify` | **61 / 0 / 0** | Mới ở U4: `learning.LearningPathServiceTest` 4/0/0. Các bộ khác đều pass: `ModularityTests` 2, chạy lại sau khi thêm module `learning`; `EnrollmentTest` 13; `SessionOutcomeTest` 13; `SlotServiceTest` 7; `PaymentServiceTest` 7; `JoinServiceTest` 4; `SecurityPropertiesTest` 3; `BillingFlowTest` 2; `NotificationDispatchTest` 2; `LiveKitVideoRoomProviderTest` 2; `AuthorizationRulesTest` 1; `GlobalExceptionHandlerTest` 1. |
| Integration Testcontainers (failsafe) | `./mvnw -B verify` | **73 / 0 / 0** | Mới ở U4: `learning.LearningAccessIT` 5/0/0 (5.49 s). Các bộ khác đều pass: `AuthFlowIT` 21, `CatalogFlowIT` 9, `EnrollmentApiIT` 8, `StudentAccessIT` 6, `CancellationPolicyIT` 5 (ghi dưới tên `$WithLongerCutoff`), `BookingConcurrencyIT` 4, `SchedulingJobsIT` 4, `SessionOutcomeIT` 4, `NotificationDispatchIT` 4, `SystemCancelIT` 3. |
| Frontend unit (vitest) | `cd frontend && npm test -- --run` | **3 / 0 / 0** (1 file, 32.6 s) | Chỉ có `client.test.ts`, không đổi. |
| Smoke local (demo-seed) | `java -jar backend/target/english-platform-0.0.1-SNAPSHOT.jar --spring.profiles.active=local,demo-seed` | **PASS 24 / 0 / 0** (số probe) | Khởi động mất 13.3 s. Flyway báo `Successfully validated 8 migrations` (V1–V6, V8, V202610181200), `Schema "public" is up to date`, kèm cảnh báo `outOfOrder mode is active` (F7). Log không có dòng ` ERROR `. DB local đã có seed: student1 có 1 đánh giá `PLACEMENT`, nên seed không chạy lại. Đăng nhập admin, teacher1, teacher2, student1, student4 bằng `Demo@1234` đều nhận được token. Các probe ở bên dưới. |
| | `GET /students?needsConsulting=true` (admin) | PASS | 200, đúng 1 dòng là `student4@demo.local`, `needsConsulting=true`, `teacherIds=[teacher2]` |
| | `GET /students?needsConsulting=true` (teacher1; teacher2) | PASS | teacher1 nhận 200 với 0 dòng, vì student4 không do teacher1 phụ trách. teacher2 nhận 200 với 1 dòng student4. |
| | `GET /students?needsConsulting=false` (admin) | PASS | 200, gồm student1, student2, student3, student5 |
| | `GET /students/{student1}/proficiency` (student1) | PASS | 200: `current.type=PLACEMENT`, listening/speaking B1, `history=1`, `needsConsulting=false` |
| | `GET /students/{student4}/proficiency`, `/goals` (admin) | PASS | 200: `current=null`, `history=0`, `needsConsulting=true`. Goals rỗng. |
| | `GET /students/{student1}/goals`, `/assessments` (teacher1) | PASS | 200: `IELTS:6.5:ACTIVE`; 1 đánh giá `PLACEMENT` |
| | `GET /students/{student1}/learning-path` (student1) | PASS | 200: "Lộ trình IELTS 6.5", 3 chặng có 3/3/4 chủ đề, `completionPercent=0` |
| | teacher2 gọi `GET` proficiency, goals, assessments, learning-path của student1; `GET /learning-paths/{id}/revisions` | PASS (5 probe) | Cả 5 đều 404 `NOT_FOUND` |
| | student1 gọi `POST /assessments`, `POST /goals` cho chính mình | PASS (2 probe) | Cả hai 403 `FORBIDDEN` |
| | teacher1 gọi `POST /assessments` với CEFR `D1` | PASS | 400 `VALIDATION_FAILED` ("Nội dung yêu cầu không đọc được", không có `errors[].field`, xem F5) |
| | teacher1 gọi `PUT /learning-paths/{id}` với cây hợp lệ, **không có** `reason`; rồi `reason=""` | PASS (2 probe) | Cả hai 400 `VALIDATION_FAILED`, `errors[0].field=reason` ("Cần ghi lý do điều chỉnh lộ trình") |
| | student1 / teacher2 gọi `PUT /learning-paths/{id}` có `reason` | PASS (2 probe) | student1 nhận 403 `FORBIDDEN`, teacher2 nhận 404 `NOT_FOUND`. Sau đó `GET /revisions` (teacher1) trả `[]`, tức không có lượt ghi nào lọt qua. |
| | Không token | PASS | 401 |
| | `PUT /learning-paths/{id}` hợp lệ trên DB local | **NOT RUN (cố ý)** | DB local là dữ liệu lâu dài nên QE không ghi revision thật. Việc ghi đúng người đã có IT kiểm (xem DoD). Sau smoke, process java (PID 18876) đã bị dừng và `/actuator/health` trả 000. |
| CI GitHub Actions trên PR thật | `.github/workflows/ci.yml` | **NOT RUN** | Chưa có PR. |
| gitleaks | — | **NOT RUN** | Không có trong danh sách lệnh của lượt này. |

### Đối chiếu DoD riêng của U4 (unit-of-work.md:288-291)

| Mục DoD | Trạng thái | Test và kết quả quan sát |
|---------|-----------|--------------------------|
| Phân quyền: GV không phụ trách → 404 | **Đạt** | `LearningAccessIT.accessMatrix` pass: otherTeacher gọi `POST /assessments` và `GET` assessments, proficiency, goals đều nhận 404; HV khác `GET` cũng nhận 404. `pathRevisionsRequireReasonAndRecordAuthor` pass: otherTeacher `PUT /learning-paths/{id}` nhận 404. Smoke: teacher2 nhận 404 trên cả 5 endpoint đọc, kể cả learning-path và revisions. |
| Phân quyền: HV ghi → bị từ chối | **Đạt** | `accessMatrix` pass: HV `POST /assessments` và `POST /goals` nhận 403. Smoke: student1 `POST` assessments/goals nhận 403, `PUT /learning-paths` nhận 403. Trong IT chưa có ca HV `PUT /goals/{id}`, `POST`/`PUT` lộ trình (F2). |
| Phân quyền: ma trận đủ | **Đạt một phần** | IT phủ assessments, proficiency và goals cho A/T/T khác/S/S khác. Với lộ trình, IT chỉ có T tạo và sửa, T khác sửa, S đọc. Các ô `GET learning-path` / `revisions` của T khác hoặc S khác và S ghi lộ trình chỉ có bằng chứng từ smoke, chưa có IT (F2). |
| CEFR ngoài thang → 400 | **Đạt** | `LearningAccessIT.assessmentHistoryIsKeptAndInvalidCefrRejected` pass: `D1` nhận 400 `VALIDATION_FAILED`, thiếu kỹ năng nhận 400. Ở mức DB, V6 có `CHECK ... IN ('A1'…'C2')` và `NOT NULL` cho 5 kỹ năng (`V6__learning_profile.sql:11-15`). Smoke: 400 `VALIDATION_FAILED`. |
| Sửa lộ trình thiếu `reason` → 400 | **Đạt** | Unit `LearningPathServiceTest.updateWithoutReasonIsRejected` pass: `reason=" "` ném lỗi có `field=reason` và `revisions.save` không được gọi. IT `pathRevisionsRequireReasonAndRecordAuthor` pass: 400, `errors[0].field=reason`. Smoke: thiếu `reason` hoặc `reason=""` đều nhận 400 `field=reason`. DB có `reason TEXT NOT NULL` (V6:89). |
| Revision ghi đúng người (NFR-PRIV-04) | **Đạt** | IT `pathRevisionsRequireReasonAndRecordAuthor` pass: sau `PUT` hợp lệ của teacher, `GET /revisions` trả 1 dòng với `changedBy` là id của teacher, `reason` đúng, `summary` chứa "Thêm chủ đề Phát âm" và "Xóa chủ đề Thì quá khứ". Topic giữ nguyên `id`. Unit `summaryListsAddedRemovedRenamedTopicsAndMovedDates` pass: có thêm, xóa, đổi tên chủ đề, đổi mốc ngày, thêm chặng, và không đổi gì thì danh sách rỗng. Lượt sửa bị từ chối (400/403/404) không tạo revision: smoke trả `[]`. |
| Lịch sử assessment không bị ghi đè (NFR-PORT-03) | **Đạt (có giới hạn)** | `assessmentHistoryIsKeptAndInvalidCefrRejected` pass: sau `PLACEMENT` rồi `PERIODIC`, `history.length()=2` và `current.speaking=B1` là bản mới nhất. Về cấu trúc, controller chỉ có `GET`/`POST` cho assessment, không có PUT hay DELETE (`ProficiencyController.java`). Giới hạn: test không khẳng định nội dung bản cũ vẫn nguyên, và DB không có hàng rào chặn UPDATE (F1). |
| Không có mục tiêu → có cờ; thêm mục tiêu → hết cờ | **Đạt** | `LearningAccessIT.needsConsultingFollowsActiveGoal` pass. HV có ghi danh, chưa có goal: `needsConsulting=true` ở cả `/proficiency` và `GET /students?needsConsulting=true` (catalog qua SPI). Thêm goal IELTS thì cờ thành `false` và danh sách rỗng. Đổi goal sang `DROPPED` thì cờ quay lại `true`, goal vẫn còn trong lịch sử (`goals.length()=1`). HV chưa có ghi danh thì `false`. Smoke: chỉ student4 có cờ. |
| Listener ẩn danh hóa idempotent | **Đạt (có giới hạn)** | `LearningAccessIT.anonymizationRemovesLearningDataIdempotently` pass: phát `StudentAnonymized` 2 lần trong transaction, `await` 10 s, sau đó `proficiency_assessment`, `learning_goal` và `learning_path` của HV đều còn 0 dòng. Giới hạn: `milestone`, `topic` và `path_revision` được xóa theo `ON DELETE CASCADE` (V6:56,70,84) nhưng test không đếm trực tiếp. Không có test cho thấy HV khác không bị xóa (F3). |
| (Phụ) Tối đa 1 lộ trình `ACTIVE`; % hoàn thành | **Đạt** | Unit `studentHasAtMostOneActivePath` (`INVALID_STATE_TRANSITION`) và `completionIsDoneTopicsOverAllTopics` (1/2 cho 50%) pass. IT: `POST` lộ trình thứ hai nhận 409. DB có unique index một `ACTIVE` mỗi HV (V6:50). |
| (Phụ) Ranh giới module, OQ-4 không tạo vòng | **Đạt** | `ModularityTests` 2/0/0 sau khi thêm `learning`. learning dùng `catalog.EnrollmentApi` và `identity.UserDirectory`. catalog chỉ biết interface `GoalStatusProvider` của chính nó. |
| (Phụ) Seed T090 | **Đạt (smoke)** | student1 có `PLACEMENT`, goal IELTS 6.5 và lộ trình 3 chặng × 3/3/4 chủ đề. student4 có ghi danh nhưng không có goal, nên có cờ. Seed không tạo trùng khi khởi động lại trên DB đã seed: history=1. |

## 3. Review độc lập (theo constitution)

QE chỉ rà ở mức bằng chứng build/test, chưa phải code review đầy đủ.

| Nguyên tắc | Kết quả (Đạt / Vi phạm / N/A) | Bằng chứng (file:dòng) |
|------------|-------------------------------|------------------------|
| I. Modular Monolith, `ApplicationModules.verify()` | Đạt (local) | `ModularityTests` pass. OQ-4 được giải bằng SPI đảo chiều (`catalog/GoalStatusProvider.java`, `ProfileService.java:146-147`), không JOIN chéo module. Listener nghe event công khai `identity.StudentAnonymized` (`ProfileAnonymizationListener.java:28`). |
| II. Provider-Agnostic (Ports & Adapters) | N/A | U4 không thêm provider bên ngoài nào. |
| III. Free-First & YAGNI | Đạt | Không có hạ tầng mới, chỉ 1 migration V6. Lộ trình được sửa nguyên cây qua một `PUT`. |
| IV. Kiểm thử trọng yếu, Postgres thật, cấm H2, CI mỗi PR | Đạt một phần | Phân quyền, CEFR, `reason`, revision, cờ "Cần tư vấn" và ẩn danh hóa đều có IT trên `postgres:16` thật. Toàn backend đạt 134/0/0. Còn thiếu: ô ma trận lộ trình (F2), bảng con khi ẩn danh hóa (F3). CI chưa chạy. |
| V. Bảo mật & dữ liệu cá nhân | Đạt (phạm vi U4) | Không có quyền thì nhận 404, HV ghi thì nhận 403 (`StudentAccessGuard.java:29-45`). Revision lưu `changed_by`/`reason` (V6:86-90), đáp ứng NFR-PRIV-04. Ẩn danh hóa xóa dữ liệu learning của HV (FR-031). Không token thì nhận 401. |
| Ràng buộc công nghệ (Java 21, Boot, Flyway) | Đạt | Enforcer pass. `V6__learning_profile.sql` validate được trên DB local (8 migration). Cấu hình `out-of-order: true` (`application.yml:18`) cho phép V6 chạy sau V8 và V202610181200 (F7). |
| Quy trình & DoD (CI xanh + review + demo) | Chưa đạt | Mọi gate local đều xanh, nhưng chưa có PR/CI, chưa có review độc lập cho U4, và FE T088–T089 (#3) chưa làm. |

## 4. Phát hiện

| # | Mức độ | Mô tả | File | Đề xuất |
|---|--------|-------|------|---------|
| F1 | Thấp | NFR-PORT-03 mới được kiểm qua số dòng lịch sử (2) và bản `current`. Test không khẳng định bản `PLACEMENT` cũ vẫn giữ giá trị gốc. "Chỉ INSERT" hiện chỉ dựa vào việc không có endpoint PUT/DELETE; DB không chặn UPDATE. | `LearningAccessIT.java:84-102`; `V6__learning_profile.sql:3-19` | Trong IT, khẳng định `history[1]` có `type=PLACEMENT`, `speaking=A2`. Có thể thêm trigger chặn UPDATE nếu muốn có hàng rào ở mức DB, nhưng không bắt buộc. |
| F2 | Thấp | Ma trận quyền cho lộ trình và goal chưa đủ trong IT. Các ca chưa có: T khác hoặc S khác `GET /students/{id}/learning-path`; T khác hoặc S `GET /learning-paths/{id}/revisions`; S `POST`/`PUT` lộ trình; T khác hoặc S `PUT /goals/{id}`. Smoke đã thấy đúng 404/403 ở phần lớn các ca này. | `LearningAccessIT.java:65-81,130-179` | Mở rộng vòng lặp trong `accessMatrix` sang `/learning-path` và `/learning-paths/{id}/revisions`, và thêm các ca ghi lộ trình và goal. |
| F3 | Thấp | IT ẩn danh hóa chỉ đếm 3 bảng gốc. `milestone`, `topic`, `path_revision` dựa vào cascade nhưng không được kiểm. Chưa có ca khẳng định dữ liệu của HV khác còn nguyên. | `LearningAccessIT.java:181-204`; `ProfileAnonymizationListener.java:28-32` | Thêm `PUT` có `reason` trước khi ẩn danh hóa, rồi đếm `milestone`, `topic`, `path_revision` theo `path_id`. Tạo dữ liệu cho `otherStudent` và khẳng định nó còn. |
| F4 | Thông tin | Tài liệu lệch với code. tasks.md:209 (T085) vẫn ghi `B/learning/LearningProfileApi.java`, trong khi code dùng SPI `catalog.GoalStatusProvider` theo OQ-4 (events-and-ports.md:51). unit-of-work.md:273 ghi `INITIAL/PERIODIC`, còn code, V6 và tasks.md:206 dùng `PLACEMENT/PERIODIC`. | `tasks.md:209`; `unit-of-work.md:273` | Orchestrator cập nhật tài liệu. QE không sửa. |
| F5 | Thông tin | CEFR ngoài thang (vd. `D1`) bị chặn từ lúc đọc JSON thành enum, nên trả 400 `VALIDATION_FAILED` "Nội dung yêu cầu không đọc được" mà **không có** `errors[].field`. FE (#3, T088) sẽ không tô đỏ được ô kỹ năng sai. Khi thiếu `reason`, API có trả `field` đầy đủ. | `ProficiencyController.java` (`AssessmentRequest` với `Cefr`); `GlobalExceptionHandler` | Có thể ánh xạ `InvalidFormatException` thành `errors[{field: <tên thuộc tính>}]`. FE dùng dropdown A1–C2 nên mức độ thấp. |
| F6 | Thông tin | Quy tắc "đang học" (`ACTIVE`, `PENDING_PAYMENT`) được định nghĩa ở 2 nơi: catalog dùng cho danh sách, learning dùng cho `/proficiency`. Hiện hai nơi khớp nhau, nhưng có thể lệch nếu sau này đổi một bên. | `catalog/internal/profile/ProfileService.java:57-58`; `learning/internal/proficiency/ProficiencyService.java:24-25,137-141` | Ghi chú lại, hoặc đưa tập trạng thái thành hằng công khai ở `catalog.EnrollmentStatus`. |
| F7 | Thông tin | Flyway chạy ở chế độ `out-of-order` nên V6 đã được áp sau V8 và V202610181200 trên DB local. Log có WARN "Migration ... may not be reproducible". DB mới (Testcontainers) chạy đúng thứ tự số. | `application.yml:16-18`; log smoke | Chấp nhận được, vì số migration đã chia theo module. Khi cấp số migration mới, tránh dùng số nhỏ hơn số đã chạy. |
| F8 | Thông tin | Chưa chạy CI/gitleaks. Smoke cố ý không ghi revision thật lên DB local. FE T088–T089 (#3) chưa làm nên chưa kiểm được nhãn "Cần tư vấn" trên giao diện. | — | Chạy CI khi có PR. Kiểm thử tay FE khi #3 nối API. |

## 5. Verdict

**PASS_WITH_NOTES**: Backend `verify` BUILD SUCCESS với 61/0/0 unit và 73/0/0 IT Testcontainers (mới: `LearningPathServiceTest` 4/0/0, `LearningAccessIT` 5/0/0), `spotless:check` PASS. FE lint (0 lỗi), prettier, vitest 3/0/0 và build đều pass. Smoke demo-seed đạt 24/24 probe: cờ "Cần tư vấn" chỉ có student4, GV không phụ trách nhận 404, HV ghi nhận 403, CEFR sai nhận 400, thiếu `reason` nhận 400 `field=reason`. Mọi mục DoD của U4 đều có test pass trên Postgres thật. Còn các điểm cần bổ sung test: nội dung bản assessment cũ (F1), ô ma trận lộ trình và goal (F2), bảng con khi ẩn danh hóa (F3), cùng tài liệu T085 và `INITIAL` còn lệch (F4).

## Review độc lập (aidlc-reviewer)

Reviewer không viết code U4 và chỉ đọc mã nguồn. Các phát hiện F1–F8 ở trên không được nhắc lại.

| Nguyên tắc | Kết quả | Bằng chứng |
|---|---|---|
| I. Modular Monolith | Đạt | learning dùng `catalog.EnrollmentApi` và `identity.UserDirectory` (`StudentAccessGuard.java:3-5`). Chiều phụ thuộc catalog→learning được đảo bằng SPI `GoalStatusProvider` lấy qua `ObjectProvider` (`ProfileService.java:39,146`), nên không có vòng phụ thuộc. Ẩn danh hóa nghe event công khai (`ProfileAnonymizationListener.java:28`). |
| II. Ports & Adapters | N/A | Unit không có provider ngoài. |
| III. YAGNI | Đạt | Sửa nguyên cây trong một lệnh `PUT`. Không thêm hạ tầng. |
| IV. Kiểm thử trọng yếu | Đạt một phần | Có IT phân quyền trên Postgres thật. Chưa có test cho tranh chấp đồng thời trên lộ trình (R1, R3) và cho thao tác sắp xếp lại (R2). |
| V. Bảo mật & DLCN | Đạt một phần | Không có quyền thì 404, học viên ghi thì 403 (`StudentAccessGuard.java:28-45`). Lý do được kiểm sau guard nên không lộ sự tồn tại qua lỗi 400 (`LearningPathService.java:55-58`). Còn thiếu: một số thay đổi lộ trình không có audit (R2). Migration V6 mới. |

**Các lựa chọn có chủ đích, chấp nhận được:**
- Nội dung mục tiêu không sửa được. `PUT` chỉ đổi `status` và `deadline` (`LearningGoal.java:42,65`).
- Tạo lộ trình khi đã có lộ trình `ACTIVE` thì trả 409 (`LearningPathService.java:42-46`).
- Chỉ A/T đọc được revisions, khớp với `rest-api.md:99`. Học viên gọi `PUT` hoặc revisions thì nhận 403 từ `@PreAuthorize` dù path có tồn tại hay không, nên không lộ thông tin (`LearningPathController.java:119,128`).
- GV không phụ trách gọi `PUT` hoặc revisions thì nhận 404 qua guard (`LearningPathService.java:54-55,73-74`).
- **Không thể chèn id từ lộ trình khác.** Map `existing` chỉ được dựng từ chặng của chính path và chủ đề của chính chặng (`LearningPath.java:76-82`, `Milestone.java:75-83`). Id lạ hoặc id trùng sẽ thành phần tử mới.
- `position` luôn là chỉ số 0..n-1. Ràng buộc unique khai báo `DEFERRABLE` (V6:62,75) nên đổi chỗ không xung đột với thứ tự flush của Hibernate (insert trước, xóa orphan sau).
- Ẩn danh hóa xóa đủ dữ liệu: JPQL bulk delete xóa bảng gốc, ON DELETE CASCADE xóa milestone/topic/path_revision. Listener chạy trong transaction riêng nên persistence context không bị lệch.
- Đặt `@Transactional` ở controller để map DTO từ các quan hệ lazy là hợp lý khi tắt open-in-view.
- "Đang học" = `ACTIVE`/`PENDING_PAYMENT` (xem F6).

### Findings

| # | Mức | Vấn đề | Bằng chứng file:line | Tác động | Đề xuất |
|---|---|---|---|---|---|
| R1 | MAJOR | Lộ `version` ra ngoài nhưng `PUT` không kiểm tra. Hai người cùng `GET` rồi lần lượt `PUT` thì người sau ghi đè người trước một cách im lặng. Summary của revision lại được tính trên cây đã bị người trước sửa. Ngoài ra, `version` trong response là giá trị trước flush vì DTO được map trước khi commit. | `LearningPathController.java:56,85` (lộ ra), `:91-92` (request không có `version`), `:120-124`; `LearningPathService.java:52-67` | Vi phạm NFR-REL-08 (Must, ghi rõ `learning_path`): mất cập nhật giữa các request. `@Version` chỉ bắt được hai transaction chạy chồng nhau. | Thêm `@NotNull Long version` vào `UpdatePathRequest`. Nếu khác `path.getVersion()` thì ném `CONCURRENT_MODIFICATION` (409). Gọi `paths.flush()` trước khi trả về. Thêm IT: hai `PUT` cùng version thì một cái 200, một cái 409. |
| R2 | MAJOR | Sắp xếp lại chủ đề hoặc chặng, và đổi `status` của chặng, đều được áp vào DB nhưng không sinh dòng thay đổi nào. Nếu `PUT` chỉ có các thay đổi này thì danh sách thay đổi rỗng và không có revision. | `LearningPath.java:94-98` (`update(i…)`, `setStatus` không thêm vào `changes`); `Milestone.java:94` (không so `position`); `LearningPathService.java:63` | FR-018 ghi rõ "sắp xếp chủ đề … lưu lịch sử". NFR-PRIV-04 yêu cầu audit 100% thay đổi. Hiện có thay đổi không được audit. | So thứ tự id giữ lại (cũ và mới) để sinh dòng "Sắp xếp lại chủ đề chặng X" / "Sắp xếp lại chặng". Đổi status thì thêm dòng "Chặng X: PLANNED → IN_PROGRESS". Thêm unit test cho trường hợp chỉ đổi thứ tự. |
| R3 | MINOR | Hai `POST` tạo lộ trình chạy đồng thời đều qua được bước kiểm tra. Unique index chặn bên thua, nhưng lỗi rơi vào handler chung nên trả 500. | `LearningPathService.java:42-47`; `V6__learning_profile.sql:50`; `GlobalExceptionHandler.java:69-81` (không có handler cho `DataIntegrityViolationException`) | NFR-REL-08 yêu cầu "không có 500". | Dùng `saveAndFlush` trong try/catch, bắt `DataIntegrityViolationException` rồi ném `INVALID_STATE_TRANSITION` (409). |
| R4 | MINOR | Chuyển chủ đề sang chặng khác được xử lý như xóa rồi thêm: chủ đề mất `id` và mất trạng thái `DONE`. | `Milestone.java:75-100` (map chỉ trong phạm vi chặng); `Topic.java:24` (`updatable=false`) | % hoàn thành bị giảm. Khi U6 có phiếu tham chiếu topic bằng UUID[], các tham chiếu này sẽ trỏ vào chủ đề không còn tồn tại. | Chấp nhận cho MVP nhưng ghi vào contract cho FE #3. Trước U6: dựng map topic ở cấp path và cho phép đổi chặng cha (reparent). |
| R5 | MINOR | `PUT /goals` thiếu `deadline` thì deadline bị xóa thành null. Khi cập nhật không kiểm deadline có ở quá khứ không (khác với lúc tạo). Đổi deadline thì ghi đè giá trị cũ, không lưu lại giá trị cũ hay người sửa. | `LearningGoal.java:65-70`; `ProficiencyService.java:112-114` so với `:120-134` | Có thể mất dữ liệu ngoài ý muốn từ FE. FR-016 yêu cầu "mục tiêu lưu lịch sử thay đổi". | Coi null là "không đổi" (hoặc thêm cờ `clearDeadline`). Kiểm `deadline >= today`. Cân nhắc thêm cột `updated_by`. |
| R6 | MINOR | `GET` lộ trình bị N+1: một query lấy các chặng, sau đó mỗi chặng thêm một query lấy chủ đề. Kích thước cây cũng không có giới hạn. | `Milestone.java:51`; `LearningPathController.java:65-83,89-92` | Quy mô demo vẫn ổn. Payload lớn có thể làm chậm request. | Thêm `@BatchSize(size=50)` cho `topics` hoặc dùng `@EntityGraph`. Thêm `@Size(max=…)` cho `milestones`/`topics`. |
| R7 | NOTE | Bảo vệ chống chèn id từ lộ trình khác hiện đúng nhưng chưa có test. | `LearningPath.java:76-82` | Có thể hồi quy khi refactor. | Thêm IT: `PUT` với id chủ đề của HV khác thì id đó không bị sửa hay di chuyển. |

### Verdict đề xuất của reviewer

**PASS_WITH_NOTES**: không có blocker, phân quyền 404/403 và tính toàn vẹn của cây đều đúng. Tuy vậy, R1 (mất cập nhật lộ trình, NFR-REL-08) và R2 (sắp xếp hoặc đổi trạng thái không có revision, FR-018/NFR-PRIV-04) chạm vào NFR Must và nên được sửa kèm test trước khi đóng U4.

## Re-verify sau sửa (2026-10-18, orchestrator quan sát)

`./mvnw -B verify` BUILD SUCCESS: unit **64/0/0**, IT **73/0/0**; `spotless:check` **PASS**.

| Finding | Trạng thái | Bằng chứng |
|---|---|---|
| R1 PUT lộ trình không kiểm version | ĐÃ SỬA | `UpdatePathRequest.version` bắt buộc; lệch → 409 `CONCURRENT_MODIFICATION`; `OPTIMISTIC_FORCE_INCREMENT` khi cây đổi + flush trước khi trả; `LearningAccessIT.pathRevisionsRequireReasonAndRecordAuthor` (PUT phiên bản cũ → 409, version = 1 sau sửa) |
| R2 sắp xếp/đổi trạng thái không có revision | ĐÃ SỬA | `LearningPath.apply` ghi "Sắp xếp lại…", "Đổi trạng thái chặng…"; `LearningPathServiceTest.reorderAndStatusChangeAreRecorded` |
| R3 tạo lộ trình chen nhau → 500 | ĐÃ SỬA (chưa có test song song) | `LearningPathService.create` `saveAndFlush` + bắt `DataIntegrityViolationException` → 409 |
| R4 chuyển chủ đề mất id/DONE | ĐÃ SỬA | chủ đề tra trên cả cây, `Topic.moveTo`; bỏ orphanRemoval của topic, xóa tường minh; `LearningPathServiceTest.movingTopicKeepsIdentityAndStatus` |
| R5 PUT goal xóa deadline / không kiểm quá khứ | ĐÃ SỬA | `LearningGoal.update` chỉ đổi trường có gửi; kiểm deadline; `LearningAccessIT.needsConsultingFollowsActiveGoal` |
| R7 id lộ trình khác | CÓ TEST | `LearningPathServiceTest.foreignIdsAreTreatedAsNewElements` |
| F1 nội dung bản đánh giá cũ | CÓ TEST | `LearningAccessIT.assessmentHistoryIsKeptAndInvalidCefrRejected` kiểm `history[1]` |
| F3 ẩn danh hóa đủ bảng | CÓ TEST | đếm milestone + path_revision sau ẩn danh hóa |
| F5 CEFR sai thiếu field | ĐÃ SỬA | `GlobalExceptionHandler.handleUnreadable` đọc `JacksonException.getPath()` → `errors[0].field = listening` |
| F4 tài liệu | ĐÃ SỬA | tasks.md T085, unit-of-work.md (PLACEMENT) |
| R6, F2, F6, F7 | CHUYỂN U8 | unit-of-work.md "Việc chuyển từ verify U4" |

Verdict sau sửa: **PASS_WITH_NOTES** (chờ người dùng duyệt).
