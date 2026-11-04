# Review độc lập: T140 và bản sửa FR-030

**Phạm vi**: commit `f16cf60` (T140, T145) và `0bdab58` (FR-030) trên `main`, đối chiếu constitution V, spec.md, nfr.md, contracts/rest-api.md, tasks.md:347.

**Cách làm**: chỉ đọc code, không chạy gì, không xem được diff (không có shell). Mọi kết luận về hành vi lúc chạy là **suy ra từ code, chưa tái hiện**. Số test xanh (98 unit + 173 IT, audit.md:104) là số do người làm ghi, chưa kiểm lại. Trạng thái CI và gitleaks của hai commit này chưa kiểm chứng được. Đường dẫn `backend/.../` được viết tắt theo tên file.

## Review

**Verdict đề xuất: PASS_WITH_NOTES**. Không tìm thấy lỗi phân quyền thật và không có BLOCKING, nhưng bản sửa FR-030 còn một đường gửi tới trẻ chưa có đồng ý (R1), nên sửa trước demo.

| Mức | Số lượng | Mã |
|---|---|---|
| BLOCKING | 0 | |
| MAJOR | 1 | R1 |
| MINOR | 8 | R2, R3, R6, R7, R8, R9, R14, R15 |
| NOTE | 8 | R4, R5, R10, R11, R12, R13, R16, R17 |

### 1. Ma trận có chứng minh phân quyền không?

**Phần từ chối (401, 403, 404): có.** Mỗi dòng so đúng mã HTTP (`AuthorizationMatrixIT.java:591-597`), nên không thấy dòng "xanh giả":

- **Thân request**: tham số được bind và validate trước khi `@PreAuthorize` chạy (interceptor bọc lời gọi method). Thân sai sẽ ra 400, khác mã mong đợi nên test đỏ. Vì vậy thân của mọi dòng có mục `denied` đã được test chứng minh là hợp lệ, nếu test đúng là xanh.
- **404**: dữ liệu trong mỗi dòng có tồn tại, và template được `everyEndpointIsInTheMatrix` khớp với route thật. Bỏ kiểm tra sở hữu thì kết quả thành 2xx hoặc 409, test đỏ. Hai ngoại lệ về ý nghĩa nằm ở R4.
- **Đối chiếu contract**: tôi rà từng dòng với cột Quyền của rest-api.md và không thấy lệch (sai vai trò 403, đúng vai trò khác chủ 404).

**Phần cho phép: chưa.** 42/78 dòng là lệnh ghi (tự đếm), `.ok(...)` ở đó chỉ là khai báo, không được chạy (R2).

**Đổi id sang dữ liệu người khác:** đọc code không thấy lỗi IDOR, nhưng ma trận không kiểm được dạng này (R3).

### 2. T140 còn thiếu gì?

| Mục | Kết quả | Bằng chứng |
|---|---|---|
| `@PreAuthorize` mọi endpoint | Đạt | `AuthorizationRulesTest.java:29-54` |
| Secret | Đạt ở bản triển khai; demo laptop còn rủi ro (R7) | `application.yml:8,74`, `SecurityProperties.java:13` |
| CORS đúng domain demo | Code đúng, chưa có bằng chứng kiểm thử (R6) | `SecurityConfig.java:164-174` |
| `/error` | Đạt | Không có `server.error.*` hay devtools nên giữ mặc định không lộ message/trace; Caddy không proxy (`Caddyfile:12-17`) |
| Swagger | Đạt | `application.yml:50-54`, `application-local.yml:7-11`, `AuthorizationMatrixIT.java:639-642` |
| Actuator | Đạt | Chỉ lộ `health`, không hiện chi tiết (`application.yml:61-68`) |
| Cookie refresh | Đạt NFR-SEC-04 | `AuthController.java:120-127`, `AuthFlowIT.java:167` |
| JWT | Đạt: HS256, kiểm issuer, bắt buộc `exp`; còn phần nhỏ của R13 U5 (R13) | `SecurityConfig.java:134-151`, `AuthFlowIT.java:249-263` |

Validator `exp` đúng: token thiếu `exp` đi qua `JwtTimestampValidator` nhưng bị `JwtClaimValidator` chặn, và trả `UNAUTHENTICATED` chứ không phải `TOKEN_EXPIRED`.

### 3. `@Size`, `@Valid`, guard giáo viên

- **`@Valid`**: đủ trên mọi DTO có `@Size` trong các controller đã rà. Giới hạn không vượt cột `VARCHAR` (occupation 100, target 100, name 150, reason 200).
- **Ô còn sót**: xem R8.
- **`List<@Size(max = 100) String>`**: suy ra có hiệu lực, vì annotation TYPE_USE của record component được truyền xuống field và Hibernate Validator kiểm phần tử trong container. Chưa có test (R9).
- **Guard giáo viên**: không có hồi quy với ghi danh đã hủy. `Enrollment.cancel` từ chối khi `sessionsReserved > 0` (`Enrollment.java:233`) và `reserve` chỉ chạy khi `ACTIVE` (`Enrollment.java:122`), nên ghi danh `CANCELLED` luôn có reserved = 0. Xem thêm R10.

### 4. FR-030

| Câu hỏi | Kết quả |
|---|---|
| Email mời | Không hồi quy. Listener chạy sau commit nên trạng thái đã là `INVITED` (`GuardianConsentService.java:93-94`); test tại `EnrollmentLifecycleJobIT.java:281-290` |
| Học viên `INVITED` | Vẫn nhận thông báo như trước, đúng ý định |
| `userId` null | Mọi nơi gọi `notify` đều truyền id thật, không có đường vòng qua nhánh null |
| Dedupe | Bị chặn thì không ghi dòng dispatch nên khóa không bị chiếm; thông báo sau đồng ý vẫn gửi được |
| Đường gửi khác | `EmailSender.send` và `new Notification` chỉ nằm trong `DispatchService`, và `retry` chỉ gửi dòng đã ghi. **Nhưng** điều kiện theo trạng thái để lọt hai trường hợp (R1) |
| Hiệu năng | Không đáng lo (R12) |

### 5. README

Sai hoặc nói quá: R14, R15, R16 (`README.md`) và R17 (`deploy/README.md`). Đã đối chiếu và thấy đúng: tài khoản demo (`IdentityDemoSeed.java:60-73`), cổng Mailpit, `gen:api`, Spring Boot 4.1, `writesModuleDocumentation`, Swagger chỉ ở `local`, failsafe chạy `*IT` trong `verify`, gitleaks trong CI.

### Constitution V

| Quy tắc | Kết quả |
|---|---|
| Phân quyền ở backend cho mọi API | Đạt |
| GV/HV chỉ truy cập dữ liệu của mình | Đạt theo đọc code, trừ ngoại lệ leader đã chấp nhận (audit.md:102) |
| Băm mật khẩu | Đạt, BCrypt 10 (`SecurityConfig.java:160`) |
| Secret không vào Git | Đạt ở bản triển khai, giá trị dev có ghi chú ở R7 |
| Dữ liệu cá nhân | Còn R1 |
| Flyway | Hai commit không thấy đổi schema theo danh sách file được giao; chưa xem được diff |

## Findings

### R1. MAJOR: chặn theo trạng thái làm lọt trẻ chưa có đồng ý

**Bằng chứng**:

- `DispatchService.java:104-110` chỉ chặn khi status là `AWAITING_GUARDIAN_CONSENT`.
- `UserAdminService.java:105-115` cho phép chuyển `AWAITING_GUARDIAN_CONSENT` sang `INACTIVE`.
- `SystemCancelListener.java:71-74` và `:101-108` hủy buổi và phát `SessionCancelled` với `cancelledBy = null`.
- `SchedulingNotificationListener.java:99-111` khi đó báo cho học viên.
- `GuardianConsentService.java:76-80` thừa nhận còn hồ sơ cũ `INVITED` thiếu đồng ý; các hồ sơ này cũng không bị chặn.

**Lý do**: giáo vụ đặt buổi hộ trẻ (đúng kịch bản test ở `EnrollmentLifecycleJobIT.java:249`) rồi ngừng kích hoạt hồ sơ. Lúc listener chạy, trạng thái đã là `INACTIVE`, nên trẻ nhận email và thông báo hủy buổi dù chưa có đồng ý. Mọi event khác tới trong lúc `INACTIVE` cũng lọt như vậy. Điều này trái với mục tiêu ghi ở `DispatchService.java:103` và constitution V. Spec không ghi nguyên văn "không liên lạc"; yêu cầu này đến từ R22 U8. Suy ra từ code, chưa tái hiện.

**Đề xuất**:

1. Thêm vào `UserDirectory` một phương thức kiểu `isAwaitingGuardianConsent(userId)`. Phương thức trả true khi status là `AWAITING_GUARDIAN_CONSENT` **hoặc** `InvitationService.missingGuardianConsent(user)` đúng. Giữ cả hai điều kiện vì tuổi được tính theo thời điểm hiện tại.
2. Thêm IT: trẻ chờ đồng ý có buổi, giáo vụ ngừng kích hoạt, trẻ không nhận gì, giáo viên vẫn được báo.

### R2. MINOR: lệnh ghi được phép không chạy, chú thích nói quá

**Bằng chứng**: `AuthorizationMatrixIT.java:46-47`, `:607-609`, `:559-571`.

**Lý do**:

- Nếu quyền bị siết nhầm trên 42 dòng ghi, ma trận vẫn xanh. `everyRowCoversEveryActor` còn tính `.ok` là đã phủ.
- Các dòng không có mục `denied` (`PATCH /me`, `POST /me/consents`, `POST /notifications/read-all`) chưa từng chạy, nên thân request của chúng cũng chưa được kiểm trong ma trận.
- Câu "đã có test ở IT của từng module" sai ít nhất với `DELETE .../availability/exceptions/{id}`: tìm `delete(` và `deleteException` trong test không thấy lời gọi nào ngoài ma trận (chỉ có `CatalogFlowIT.java:320`, là endpoint khác).
- Tác động bảo mật thấp vì chiều từ chối đã được kiểm; đây chủ yếu là khoảng hở chức năng.

**Đề xuất**: thêm test ghi được phép với dữ liệu riêng từng dòng, ít nhất cho các dòng chưa có test module, bắt đầu từ `DELETE exceptions`. Sửa lại chú thích cho đúng.

### R3. MINOR: ma trận không phát hiện IDOR dạng đường dẫn của mình, id con của người khác

**Bằng chứng**: mỗi dòng chỉ có một bộ id (`AuthorizationMatrixIT.java:102-112`). `listsAreScopedToCaller` (`:623-635`) không truyền tham số lọc.

**Đã đọc và thấy đúng**:

- `AvailabilityService.java:154` lọc theo teacher.
- `ProficiencyService.java:130` lọc theo student.
- `LearningPath.java:74,93` coi id lạ là phần tử mới.
- `SessionReportService.java:190` kiểm chủ đề thuộc lộ trình.
- `SessionController.java:167-171` và `EnrollmentService.java:90-94` ghi đè tham số lọc.
- `ProfileService.java:118` giới hạn theo giáo viên.

**Lý do**: code đúng nhưng chưa có test bảo vệ cho các trường hợp: `DELETE` exception của GV khác, `PUT` goal của HV khác (`LearningAccessIT.java:126-130` chỉ dùng goal đúng chủ), `/sessions?studentId=` và `/enrollments?studentId=` với id người khác. Đã có test cho `AssignmentFlowIT.java:211-213`, `StudentAccessIT.java:65`, `SessionReportFlowIT.java:174`.

**Đề xuất**: thêm 4 trường hợp trên. Mong đợi: hai lệnh ghi trả 404, hai danh sách trả rỗng.

### R4. NOTE: hai dòng `.ok()` rỗng mang nghĩa 404 khác

**Bằng chứng**: `AuthorizationMatrixIT.java:259-262`, `:464`.

**Lý do**: admin nhận 404 vì **chưa có dữ liệu**, không phải vì khác chủ như Javadoc (`:43`) mô tả. Dòng vẫn chứng minh admin qua được kiểm tra vai trò, và luồng 200 đã có ở `GuardianConsentIT.java:85`, `SessionFeedbackIT.java:35`. Không phải lỗi.

**Đề xuất**: tạo dữ liệu trong `buildMatrix` để chuyển thành `.ok(A)`, hoặc thêm chú thích ở hai dòng.

### R5. NOTE: ma trận không kiểm `code`

**Bằng chứng**: `AuthorizationMatrixIT.java:592-595`.

**Lý do**: 403 có thể là `FORBIDDEN`, `CONSENT_REQUIRED` hoặc `ROOM_NOT_OPEN`. Hiện chưa gây xanh giả, vì nếu đồng ý chính sách hỏng thì các dòng GET được phép sẽ đỏ.

**Đề xuất**: kiểm thêm `$.code` để lỗi báo đúng nguyên nhân.

### R6. MINOR: "CORS đúng domain demo" chưa có bằng chứng

**Bằng chứng**: `tasks.md:347` đánh dấu đạt nhưng phần ghi kết quả không nhắc CORS. Tìm `Access-Control` và `Origin` trong test không có kết quả. Cấu hình ở `SecurityConfig.java:164-174`, `application.yml:71`, `docker-compose.prod.yml:36`.

**Lý do**: giá trị mặc định khớp laptop (`localhost:5173`) và compose khớp cùng domain. Tuy nhiên chỉ có đúng một origin chính xác: mở qua `127.0.0.1`, IP LAN, hoặc đặt `FRONTEND_URL` có dấu `/` cuối sẽ bị chặn. Trên HTTP qua IP LAN, cookie `Secure` cũng không được lưu nên refresh hỏng. Suy ra, chưa tái hiện.

**Đề xuất**: thêm test preflight cho origin đúng và origin lạ. Ghi trong quickstart rằng demo phải mở đúng `http://localhost:5173`, đồng thời chạy thử trên trình duyệt sẽ dùng để demo (Safari có thể từ chối cookie `Secure` trên localhost, chưa kiểm).

### R7. MINOR: demo laptop chạy với JWT secret công khai

**Bằng chứng**: `application-local.yml:19` và `:7-11`, `README.md:48`, `deploy/README.md:3-6`; không đặt `server.address`.

**Lý do**: giá trị dev được commit là chấp nhận được với máy dev. Nhưng demo chạy trên laptop theo profile `local` thì secret này công khai trong repo, backend nghe mọi interface, Swagger mở. Người cùng mạng Wi-Fi có thể tự ký token admin nếu tường lửa cho phép. Suy ra, chưa tái hiện.

**Đề xuất**: ghi trong checklist demo: đặt `JWT_SECRET` ngẫu nhiên và `SERVER_ADDRESS=127.0.0.1`.

### R8. MINOR: giới hạn độ dài văn bản còn sót

**Bằng chứng**:

- `TopicSpec.objective`: `PathSpec.java:20`, `Milestone.java:91,113`, cột `TEXT` ở `V6__learning_profile.sql:71`.
- `description` khóa học: `CourseController.java:44,48`.
- Số phần tử danh sách: `specialties` (`ProfileController.java:43`), `assignments` (`SessionReportController.java:86-91`).
- Các endpoint công khai không giới hạn: `AuthController.java:56,62-67`.
- Không có giới hạn thân request: không có cấu hình tương ứng trong `application*.yml` và `Caddyfile`.

**Lý do**: `tasks.md:347` ghi đã thêm giới hạn. Các ô này không gây 500 vì cột là `TEXT` hoặc giá trị chỉ dùng để băm và so sánh, nhưng vẫn nhận dữ liệu lớn tùy ý. Endpoint công khai là phần đáng lo nhất, nhất là khi rate limit đã hoãn theo K3.

**Đề xuất**: thêm giới hạn cho `objective` và `description`, `@Size(max = ...)` cho danh sách, `@Size` cho các trường của auth, và `request_body { max_size ... }` trong Caddy trước khi lên VPS.

### R9. MINOR: `List<@Size>` chưa có test

**Bằng chứng**: `ProfileController.java:43`; `StudentAccessIT.java:96-99` chỉ có ca hợp lệ.

**Lý do**: đây là ô duy nhất có dạng ràng buộc phần tử. Spring phải đọc lại giá trị từ đường dẫn `specialties[0]` trên record; suy ra là chạy được và trả 400, nhưng nếu không đọc được thì lỗi sẽ là 500. Chưa tái hiện.

**Đề xuất**: thêm IT gửi phần tử 101 ký tự, mong 400 với `errors[0].field = specialties[0]`.

### R10. NOTE: guard tính buổi theo giáo viên hiện tại của ghi danh

**Bằng chứng**: `EnrollmentRepository.java:26-29`; buổi tương lai bị hủy khi đổi GV nhưng buổi đã qua chưa chốt thì vẫn giữ reserved.

**Lý do**: với ghi danh `EXPIRED` còn buổi quá khứ chưa chốt của GV cũ, sau khi đổi GV thì GV mới bị chặn còn GV cũ được thả. Có lối ra (giáo vụ chốt thay) nên không kẹt. Ngoài ra, test `EnrollmentLifecycleJobIT.java:146-160` có buổi đã ở quá khứ lúc kiểm tra (đồng hồ +91 ngày, buổi +3 ngày), nên chưa phủ buổi tương lai trên gói đã hết hạn.

**Đề xuất**: chấp nhận cho demo. Về sau, truy vấn theo `session.teacher_id` qua `SessionQueryApi` và thêm ca buổi tương lai.

### R11. NOTE: thông báo bị chặn mất hẳn, không để lại dấu vết

**Bằng chứng**: `DispatchService.java:72-76`; audit.md:104 đã ghi đây là quyết định có chủ đích.

**Lý do**: sau khi có đồng ý, trẻ không nhận lại email xác nhận các buổi đã đặt, và lượt nhắc 24h đã qua cũng mất; lịch vẫn xem được trong app sau khi kích hoạt. Không có log nên khó tra khi có thắc mắc. Dòng `PENDING` tạo trước bản sửa vẫn được `retry` gửi mà không kiểm lại (`DispatchService.java:117-122`), rủi ro thấp vì DB demo được seed mới.

**Đề xuất**: giáo vụ báo lịch cho phụ huynh khi ghi nhận đồng ý; cân nhắc log mức INFO chỉ gồm `userId` và `dedupeKey`.

### R12. NOTE: chi phí truy vấn thêm trong `notify` không đáng kể

**Bằng chứng**: `DispatchService.java:104-109`, `UserDirectoryImpl.java:27-29`.

**Lý do**: `@ApplicationModuleListener` mở transaction riêng, các listener đã đọc người nhận qua `userDirectory.find` trước đó trong cùng transaction, nên `findById` lấy từ persistence context và thường không phát thêm SQL. Listener chạy bất đồng bộ nên không ảnh hưởng API. Thông tin đọc có thể cũ trong khoảng ngắn của transaction, chỉ làm chặn thừa một thông báo. Suy ra, chưa đo.

**Đề xuất**: không cần sửa.

### R13. NOTE: nửa còn lại của R13 U5

**Bằng chứng**: `SecurityConfig.java:139-144`; `AuthFlowIT.java:261-262`.

**Lý do**: lỗi `nbf` vẫn bị báo là `TOKEN_EXPIRED`, khiến frontend gọi refresh. Rủi ro thấp vì app không phát `nbf` tương lai. Test token thiếu `exp` chỉ kiểm 401, không kiểm `code`.

**Đề xuất**: kiểm thêm `$.code = UNAUTHENTICATED` trong test.

### R14. MINOR: `README.md` nhắc thư mục `perf/` không tồn tại

**Bằng chứng**: `README.md:17`; tìm `perf/**` và `**/*k6*` (ngoài `node_modules`) không có file; T141 còn mở (`tasks.md:348`).

**Đề xuất**: bỏ dòng này hoặc ghi "dự kiến, T141" cho tới khi có kịch bản.

### R15. MINOR: mục Kiểm thử của `README.md` lệch CI và nói quá

**Bằng chứng**: `README.md:70-71`, `ci.yml:44`, `AuthorizationMatrixIT.java:548`, `:607-609`.

**Lý do**: CI chạy thêm `npx prettier --check src`, nên chạy đủ lệnh trong README vẫn có thể đỏ trên CI. Câu "Phân quyền của mọi endpoint nằm trong `AuthorizationMatrixIT`" không đúng hẳn: nhóm `/auth/*` bị loại, và lệnh ghi được phép không chạy.

**Đề xuất**: thêm lệnh prettier; sửa thành "ma trận kiểm từ chối cho mọi endpoint ngoài `/auth/*`, kiểm cho phép với lệnh GET".

### R16. NOTE: khối lệnh trong `README.md` không dán chạy nguyên được

**Bằng chứng**: `README.md:47-51`, `:66-67`.

**Lý do**: `cd frontend` được chạy khi đang ở `backend/`, và `spring-boot:run` giữ terminal.

**Đề xuất**: dùng `cd ../frontend`, ghi rõ chạy ở terminal khác.

### R17. NOTE: tên service trong runbook, giới hạn của ma trận, khóa R2 ở backend

Ba điểm nhỏ:

1. `deploy/README.md:87,91` bảo dừng và mở `caddy`, nhưng service tên là `web` (`docker-compose.prod.yml:50`). Lệnh theo tên đó sẽ lỗi đúng lúc khôi phục; nên đổi thành `web`.
2. `everyEndpointIsInTheMatrix` bỏ qua mapping không khai method (`AuthorizationMatrixIT.java:551-553`). Hiện không có mapping nào như vậy; nên làm test đỏ nếu tập method rỗng. Ma trận cũng không kiểm trường bị ẩn theo vai trò như `notes`, `priceVnd`, `cancelReason`; code đúng ở `EnrollmentController.java:66-67,78-84` và `ProfileController.java:102`.
3. `docker-compose.prod.yml:30` dùng `env_file` nên backend nhận cả khóa R2 dù không cần. Khi lên VPS, nên chỉ truyền biến backend dùng.

## Chưa kiểm chứng

- Chưa chạy test, CI, gitleaks hay smoke; mọi hành vi trên đều suy ra từ code.
- Chưa xem được diff của hai commit, nên "mới hay cũ" dựa trên audit.md và tasks.md.
- Không rà file `frontend/` ngoài `client.ts`, `package.json`.
- R1, R6, R7, R9 nên được tái hiện bằng test hoặc thử tay trước khi coi là đã đóng.

## Trạng thái xử lý (2026-11-05)

| Mục | Trạng thái |
|---|---|
| R1 | Đã sửa (`9db8a4a`) |
| R2 | Một phần: có test xóa ngày nghỉ, chú thích đã sửa; các lệnh ghi được phép khác để sau demo |
| R3, R9 | Đã sửa: `OwnershipHardeningIT` |
| R6 | Đã sửa: test preflight trong `OwnershipHardeningIT`, checklist ở `quickstart.md` mục 5 |
| R7 | Đã xử lý bằng checklist ở `quickstart.md` mục 5 |
| R8 | Đã sửa phần ứng dụng; giới hạn thân request ở Caddy để lúc lên VPS |
| R13 | Đã sửa phần test thiếu `exp`; lỗi `nbf` để sau demo |
| R14 | Đã xử lý: `perf/` có thật (T141) |
| R15, R16, R17.1 | Đã sửa (`aaea037`) |
| R17.2 | Đã sửa phần mapping thiếu method; trường ẩn theo vai trò để sau demo |
| R17.3 | Để lúc lên VPS |
| R4, R5, R10, R11, R12 | NOTE, không làm trước demo |
