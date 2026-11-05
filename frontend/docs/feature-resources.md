# Tài nguyên FE theo từng feature

Tra cứu nhanh khi làm một màn FE: route, file, API, kiểu dữ liệu, enum, mã lỗi và bước kiểm chứng. Endpoint, quyền, kiểu dữ liệu và enum được trích từ code backend (`e62241c`, ngày 2026-11-05), không chép từ tài liệu. Nếu lệch với `specs/001-personalized-english-tutoring/contracts/rest-api.md`, tin code và báo #1.

## Mục lục

| Feature | Người | Task | Trạng thái |
| --- | --- | --- | --- |
| [Đăng nhập, lời mời, đồng ý chính sách](#auth) | #2 | Đã xong ở U1, U7 | Đã chạy được với backend thật |
| [Chuông thông báo](#notifications) | #3 | T136 | Đã có |
| [Quản lý người dùng](#admin-users) | #4 | T052 | Đã có, chờ đối chiếu rồi tick |
| [Khóa học và gói học](#catalog-courses) | #4 | T053 | Đã có, chờ đối chiếu rồi tick |
| [Hồ sơ học viên và giáo viên](#catalog-profiles) | #4 | T054 | Đã có, chờ đối chiếu rồi tick |
| [Ghi danh và gói của học viên](#catalog-enrollments) | #4 | T055, T116 | Trang giáo vụ đã có |
| [Thanh toán và công nợ](#billing) | #4 | T114, T115 | Thu học phí đã có trong trang ghi danh |
| [Tổng quan giáo vụ](#admin-dashboard) | #4 | T123 | Chưa có: /admin đang là placeholder |
| [Quyền riêng tư](#admin-privacy) | #4 | T130, T131 | Dialog đồng ý người giám hộ đã có |
| [Lịch rảnh và ngày nghỉ](#scheduling-availability) | #2 | T075 | Đã có, chờ đối chiếu rồi tick |
| [Đặt buổi](#scheduling-booking) | #2 | T076 | Đã có, chờ đối chiếu rồi tick |
| [Lịch và chi tiết buổi](#scheduling-session) | #2 | T077, T078 | Đã có xem và hủy |
| [Phòng học LiveKit](#classroom) | #2 | T079 | Giáo viên vào được với camera giả |
| [Năng lực và mục tiêu](#learning-proficiency) | #3 | T088 | Đã có, đã chạy với backend thật sau PR #8 |
| [Lộ trình](#learning-path) | #3 | T089 | Đã có, còn thiếu sắp xếp chặng, chủ đề |
| [Phiếu đánh giá buổi](#learning-report) | #3 | T101 | Chưa có: /teacher đang là placeholder |
| [Bài tập](#learning-assignments) | #3 | T102 | Chưa có màn, route và mục menu |
| [Tiến bộ](#learning-progress) | #3 | T103 | Chưa có: /student/progress đang là placeholder |
| [Đánh giá sao](#learning-feedback) | #3 | T122 | Chưa có |

## Lỗi đã biết

Các lỗi này đã xác nhận bằng cách đọc code hoặc chạy thử với backend thật. PR #10 đã sửa `willConsume`, `completionPercent` và thời lượng khung giờ; nếu `lib/api` của bạn chưa có `slotMinutes` thì PR đó chưa vào nhánh của bạn.

| Mức | Lỗi | Vị trí | Người |
| --- | --- | --- | --- |
| Vừa | Giáo viên bấm "Chi tiết & Học phí" bị đẩy về `/teacher` | `StudentProfilePage.tsx` | #4 |
| Nhỏ | Trang ghi danh hiện ID rút gọn thay vì tên | `EnrollmentDetailPage.tsx:240,246` | #4 |
| Nhỏ | `EnrollmentStatus` ghi `COMPLETED`, backend trả `EXHAUSTED` | `lib/api/catalog.ts:5` | #4 |
| Nhỏ | `teacherName`, `studentName` trong `SessionItem` không có ở backend | `lib/api/scheduling.ts:60-61` | #2 |
| Nhỏ | Trước 7h sáng ngày lệch về hôm trước do `toISOString` | `SlotPicker.tsx:16,23`, `ManualPaymentDialog.tsx:26`, `GuardianConsentDialog.tsx:26` | #2 |

## Dùng chung

**Chạy với backend thật:**

```bash
docker compose up -d
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local,demo-seed
# terminal khác
cd frontend && npm run dev
```

Mở đúng `http://localhost:5173`. Email xem ở `http://localhost:8025`. Tài khoản, mật khẩu `Demo@1234`: `admin@demo.local`, `teacher1@demo.local`, `teacher2@demo.local`, `student1@demo.local` … `student5@demo.local`. Không có LiveKit thì chạy backend với `VIDEO_PROVIDER=fake`. FE mặc định gọi `http://localhost:8080`; đổi bằng `VITE_API_URL`. Mock chỉ bật khi `VITE_USE_MOCKS=true`.

**Trước khi mở PR:** `npm run lint`, `npx prettier --check src`, `npm run test -- --run`, `npm run build`, và tự chạy luồng đó với backend thật. Mock không thay được bước này: mọi lỗi lệch hợp đồng tới giờ đều bị mock che.

**Ký hiệu quyền:** A giáo vụ, T giáo viên, S học viên, Any mọi người đã đăng nhập. T và S chỉ thấy dữ liệu của mình; khác chủ trả 404.

**Kiểu dữ liệu:** các khối TypeScript dưới đây sinh từ DTO Java. Java không cho biết trường nào có thể rỗng, nên coi mọi trường không phải số, boolean là có thể `null`. Danh sách trả về là mảng; `GET /notifications` có dạng trang riêng.

**Thời gian:** `Instant` là ISO-8601 và lưu UTC; hiển thị theo múi giờ người xem bằng `DateTimeText`. Đừng dùng `toISOString().slice(0, 10)` để lấy ngày hôm nay, vì trước 7h sáng giờ Việt Nam nó trả về hôm trước.

**Tiện ích có sẵn:**

- `src/lib/api/client.ts`: hàm `api<T>()`, tự gắn token và refresh; lỗi ném `ApiError`.
- `src/components/useApiErrorToast.ts`: toast theo `code`, thông điệp ở `ERROR_MESSAGES`.
- `src/components/StatusBadge.tsx`: nhãn tiếng Việt cho mọi trạng thái.
- `src/components/DataTable.tsx`, `ConfirmDialog.tsx`, `EmptyState.tsx`, `DateTimeText.tsx`, `ui/button.tsx`, `ui/card.tsx`, `ui/input.tsx`.
- `src/app/router.tsx`, `src/app/AppLayout.tsx`: route và menu, do #2 giữ. Sửa dòng của mình và merge sớm.

**Mã lỗi nghiệp vụ** (trường `code`):

| HTTP | `code` | Khi nào |
| --- | --- | --- |
| 400 | `VALIDATION_FAILED` | dữ liệu sai, kèm `errors[{field, message}]` |
| 401 | `UNAUTHENTICATED` | chưa đăng nhập hoặc sai thông tin |
| 401 | `TOKEN_EXPIRED` | access token hết hạn, `client.ts` tự refresh |
| 403 | `FORBIDDEN` | sai vai trò |
| 403 | `CONSENT_REQUIRED` | chưa đồng ý chính sách hiện hành |
| 403 | `ROOM_NOT_OPEN` | vào phòng ngoài khung thời gian |
| 404 | `NOT_FOUND` | không có, hoặc không có quyền xem dữ liệu đó |
| 409 | `SLOT_TAKEN` | khung giờ vừa có người đặt |
| 409 | `NO_SESSIONS_LEFT` | gói hết buổi |
| 409 | `ENROLLMENT_NOT_ACTIVE` | gói chưa thanh toán, hết hạn hoặc đã hủy |
| 409 | `CANCEL_WINDOW_PASSED` | đổi lịch khi đã quá hạn |
| 409 | `INVALID_STATE_TRANSITION` | thao tác không hợp lệ với trạng thái hiện tại |
| 409 | `EMAIL_TAKEN` | email đã tồn tại |
| 409 | `GUARDIAN_CONSENT_REQUIRED` | học viên dưới 16 tuổi chưa có đồng ý của người giám hộ |
| 409 | `TEACHER_HAS_STUDENTS` | ngừng giáo viên còn học viên |
| 409 | `ANONYMIZED` | hồ sơ đã ẩn danh hóa |
| 409 | `EXCEPTION_OVERLAPS_SESSIONS` | ngày nghỉ trùng buổi đã đặt, kèm `sessions[]` |
| 409 | `FEEDBACK_EXISTS` | đã đánh giá buổi này |
| 409 | `FEEDBACK_WINDOW_CLOSED` | quá 7 ngày |
| 409 | `CONCURRENT_MODIFICATION` | dữ liệu vừa bị thay đổi, tải lại rồi thử lại |

Làm chung cả team: Playwright `frontend/e2e/demo.spec.ts` (T142, #2) theo quickstart bước 1–5, 8, 11, 15–17; mobile và Lighthouse (T143, #3, #4); chạy 24 bước quickstart (T146, cả team, 11/11).

## Trục chung

<a id="auth"></a>

### Đăng nhập, lời mời, đồng ý chính sách

**Người phụ trách:** #2 · **Task:** Đã xong ở U1, U7 · **FR:** FR-002, FR-030 · **Quickstart:** bước 1, 16

**Trạng thái:** Đã chạy được với backend thật.

**Route:**

- `/login`
- `/accept-invite?token=`
- `/forgot-password`
- `/reset-password?token=`
- Màn đồng ý chính sách hiện trong `RequireRole` khi `me.consentRequired`

**File:**

- `src/features/auth/LoginPage.tsx`
- `src/features/auth/AcceptInvitePage.tsx`
- `src/features/auth/ForgotPasswordPage.tsx`
- `src/features/auth/ResetPasswordPage.tsx`
- `src/features/auth/ConsentGatePage.tsx`
- `src/app/AuthProvider.tsx`
- `src/app/RequireRole.tsx`
- `src/lib/api/auth.ts`
- Mock: `src/mocks/handlers/auth.ts`

**API:**

| Method | Path | Quyền | Query | Body | Trả về | Hàm FE |
| --- | --- | --- | --- | --- | --- | --- |
| POST | `/auth/login` | public |  | `LoginRequest` | `TokenResponse` | `login (auth.ts)` |
| POST | `/auth/refresh` | public |  |  | `TokenResponse` | `refreshSession (client.ts)` |
| POST | `/auth/logout` | public |  |  | `204, không có nội dung` | `logout (auth.ts)` |
| GET | `/auth/policy` | public |  |  | `PolicyResponse` | `policy (auth.ts)` |
| POST | `/auth/accept-invite` | public |  | `AcceptInviteRequest` | `204, không có nội dung` | `acceptInvite (auth.ts)` |
| POST | `/auth/forgot-password` | public |  | `ForgotPasswordRequest` | `202, không có nội dung` | `forgotPassword (auth.ts)` |
| POST | `/auth/reset-password` | public |  | `ResetPasswordRequest` | `204, không có nội dung` | `resetPassword (auth.ts)` |
| GET | `/me` | Any |  |  | `MeResponse` | `me (auth.ts)` |
| PATCH | `/me` | Any |  | `UpdateMeRequest` | `MeResponse` | chưa có |
| POST | `/me/consents` | Any |  | `ConsentRequest` | `204, không có nội dung` | `acceptPolicy (auth.ts)` |

```ts
interface LoginRequest {
  email: string
  password: string
}

interface TokenResponse {
  accessToken: string
  user: UserSummary
}

interface PolicyResponse {
  currentPolicyVersion: string
}

interface AcceptInviteRequest {
  token: string
  password: string
  acceptedPolicyVersion: string
}

interface ForgotPasswordRequest {
  email: string
}

interface ResetPasswordRequest {
  token: string
  newPassword: string
}

interface MeResponse {
  id: string
  email: string
  fullName: string
  phone: string
  role: 'ADMIN' | 'TEACHER' | 'STUDENT'
  status: 'AWAITING_GUARDIAN_CONSENT' | 'INVITED' | 'ACTIVE' | 'INACTIVE'
  timezone: string
  consentRequired: boolean
  currentPolicyVersion: string
}

interface UpdateMeRequest {
  fullName: string
  phone: string
  timezone: string
}

interface ConsentRequest {
  policyVersion: string
}

interface UserSummary {
  id: string
  email: string
  fullName: string
  role: 'ADMIN' | 'TEACHER' | 'STUDENT'
  timezone: string
}
```

**Mã lỗi cần xử lý:** `UNAUTHENTICATED`, `TOKEN_EXPIRED`, `CONSENT_REQUIRED`, `GUARDIAN_CONSENT_REQUIRED`, `VALIDATION_FAILED`

**Lưu ý:**

- Refresh token nằm ở cookie `Secure`, nên phải mở đúng `http://localhost:5173`; `127.0.0.1` bị CORS chặn.
- Mật khẩu tối thiểu 8 ký tự, có chữ và số.
- `POST /auth/forgot-password` luôn trả 202 để không lộ email có tồn tại hay không.

<a id="notifications"></a>

### Chuông thông báo

**Người phụ trách:** #3 · **Task:** T136 · **FR:** FR-013 · **Quickstart:** bước 5, 20

**Trạng thái:** Đã có. Hiện danh sách được; chưa ai thử bấm mở một thông báo.

**Route:**

- Nằm trong header của `AppLayout`

**File:**

- `src/features/notifications/NotificationBell.tsx`
- `src/features/notifications/NotificationList.tsx`
- `src/lib/api/notifications.ts`
- Mock: `src/mocks/handlers/notifications.ts`

**API:**

| Method | Path | Quyền | Query | Body | Trả về | Hàm FE |
| --- | --- | --- | --- | --- | --- | --- |
| GET | `/notifications` | Any | `unread?` |  | `NotificationPageResponse` | `fetchNotifications (notifications.ts)` |
| POST | `/notifications/{id}/read` | Any |  |  | `204, không có nội dung` | `markNotificationRead (notifications.ts)` |
| POST | `/notifications/read-all` | Any |  |  | `204, không có nội dung` | `markAllNotificationsRead (notifications.ts)` |

```ts
interface NotificationPageResponse {
  items: NotificationDto[]
  totalElements: number
  totalPages: number
  page: number
  size: number
  unreadCount: number
}

interface NotificationDto {
  id: string
  type: string
  title: string
  body: string
  link: string
  readAt: string /* ISO-8601 */
  createdAt: string /* ISO-8601 */
}
```

**Lưu ý:**

- Gọi lại mỗi 60 giây (`refetchInterval`).

## Giáo vụ

<a id="admin-users"></a>

### Quản lý người dùng

**Người phụ trách:** #4 · **Task:** T052 · **FR:** FR-002, FR-004, FR-007, FR-030 · **Quickstart:** bước 1, 15, 22

**Trạng thái:** Đã có, chờ đối chiếu rồi tick.

**Route:**

- `/admin/users`

**File:**

- `src/features/admin/users/UsersPage.tsx`
- `src/features/admin/users/CreateUserDialog.tsx`
- `src/features/admin/users/GuardianConsentDialog.tsx`
- `src/lib/api/users.ts`
- Mock: `src/mocks/handlers/users.ts`

**API:**

| Method | Path | Quyền | Query | Body | Trả về | Hàm FE |
| --- | --- | --- | --- | --- | --- | --- |
| GET | `/users` | A | `role?, status?, q?` |  | `UserDto[]` | `fetchUsers (users.ts)` |
| POST | `/users` | A |  | `CreateUserRequest` | `201 UserDto` | `createUser (users.ts)` |
| GET | `/users/{id}` | A |  |  | `UserDto` | `fetchUser (users.ts)` |
| PATCH | `/users/{id}/status` | A |  | `StatusRequest` | `UserDto` | `updateUserStatus (users.ts)` |
| POST | `/users/{id}/resend-invite` | A |  |  | `204, không có nội dung` | `resendInvite (users.ts)` |
| GET | `/students/{studentId}/guardian-consent` | A |  |  | `GuardianConsentDto` | `fetchGuardianConsent (users.ts)` |
| PUT | `/students/{studentId}/guardian-consent` | A |  | `ConsentRequest` | `GuardianConsentDto` | `saveGuardianConsent (users.ts)` |

```ts
interface UserDto {
  id: string
  email: string
  fullName: string
  phone: string
  role: 'ADMIN' | 'TEACHER' | 'STUDENT'
  status: 'AWAITING_GUARDIAN_CONSENT' | 'INVITED' | 'ACTIVE' | 'INACTIVE'
  dateOfBirth: string /* YYYY-MM-DD */
  lastLoginAt: string /* ISO-8601 */
  anonymized: boolean
}

interface CreateUserRequest {
  email: string
  fullName: string
  role: 'ADMIN' | 'TEACHER' | 'STUDENT'
  phone: string
  dateOfBirth: string /* YYYY-MM-DD */
}

interface StatusRequest {
  status: 'AWAITING_GUARDIAN_CONSENT' | 'INVITED' | 'ACTIVE' | 'INACTIVE'
}

interface GuardianConsentDto {
  studentId: string
  guardianName: string
  relationship: string
  guardianContact: string
  consentedOn: string /* YYYY-MM-DD */
  recordedBy: string
}

interface ConsentRequest {
  guardianName: string
  relationship: string
  guardianContact: string
  consentedOn: string /* YYYY-MM-DD */
}
```

**Mã lỗi cần xử lý:** `EMAIL_TAKEN`, `TEACHER_HAS_STUDENTS`, `ANONYMIZED`, `VALIDATION_FAILED`

**Lưu ý:**

- Mọi endpoint ở đây chỉ dành cho giáo vụ. Giáo viên gọi `/users/{id}` sẽ bị 403.
- Còn phải kiểm: gửi lại lời mời, lỗi `TEACHER_HAS_STUDENTS`.

<a id="catalog-courses"></a>

### Khóa học và gói học

**Người phụ trách:** #4 · **Task:** T053 · **FR:** FR-005 · **Quickstart:** bước 2

**Trạng thái:** Đã có, chờ đối chiếu rồi tick.

**Route:**

- `/admin/courses`
- `/admin/packages`

**File:**

- `src/features/catalog/courses/CoursesPage.tsx`
- `src/features/catalog/packages/PackagesPage.tsx`
- `src/features/catalog/packages/PackageFormDialog.tsx`
- `src/lib/api/catalog.ts`
- Mock: `src/mocks/handlers/catalog.ts`

**API:**

| Method | Path | Quyền | Query | Body | Trả về | Hàm FE |
| --- | --- | --- | --- | --- | --- | --- |
| GET | `/courses` | Any |  |  | `CourseDto[]` | `fetchCourses (catalog.ts)` |
| POST | `/courses` | A |  | `CreateCourseRequest` | `201 CourseDto` | `createCourse (catalog.ts)` |
| PUT | `/courses/{id}` | A |  | `UpdateCourseRequest` | `CourseDto` | `updateCourse (catalog.ts)` |
| GET | `/packages` | Any | `courseId?` |  | `PackageDto[]` | `fetchPackages (catalog.ts)` |
| POST | `/packages` | A |  | `CreatePackageRequest` | `201 PackageDto` | `createPackage (catalog.ts)` |
| PUT | `/packages/{id}` | A |  | `UpdatePackageRequest` | `PackageDto` | `updatePackage (catalog.ts)` |

```ts
interface CourseDto {
  id: string
  name: string
  description: string
  status: 'ACTIVE' | 'INACTIVE'
}

interface CreateCourseRequest {
  name: string
  description: string
}

interface UpdateCourseRequest {
  name: string
  description: string
  status: 'ACTIVE' | 'INACTIVE'
}

interface PackageDto {
  id: string
  courseId: string
  name: string
  sessionCount: number
  sessionMinutes: number
  validityDays: number
  priceVnd: number
  status: 'ACTIVE' | 'INACTIVE'
}

interface CreatePackageRequest {
  courseId: string
  name: string
  sessionCount: number
  sessionMinutes: number
  validityDays: number
  priceVnd: number
}

interface UpdatePackageRequest {
  name: string
  sessionCount: number
  sessionMinutes: number
  validityDays: number
  priceVnd: number
  status: 'ACTIVE' | 'INACTIVE'
}
```

**Mã lỗi cần xử lý:** `VALIDATION_FAILED`

**Lưu ý:**

- Thời lượng buổi chỉ nhận 25, 45, 60, 90 phút.
- Tên khóa tối đa 150 ký tự; sau PR #9, mô tả khóa tối đa 5.000 ký tự.

<a id="catalog-profiles"></a>

### Hồ sơ học viên và giáo viên

**Người phụ trách:** #4 · **Task:** T054 · **FR:** FR-003, FR-004, FR-016 · **Quickstart:** bước 14

**Trạng thái:** Đã có, chờ đối chiếu rồi tick. Tab "Tiến bộ" chờ T103.

**Route:**

- `/admin/students`, `/admin/students/:id`
- `/admin/teachers/:id`
- `/teacher/students`, `/teacher/students/:id`
- `/teacher/profile`

**File:**

- `src/features/catalog/profiles/StudentsListPage.tsx`
- `src/features/catalog/profiles/StudentProfilePage.tsx`
- `src/features/catalog/profiles/TeacherProfilePage.tsx`
- `src/lib/api/catalog.ts`
- Mock: `src/mocks/handlers/catalog.ts`

**API:**

| Method | Path | Quyền | Query | Body | Trả về | Hàm FE |
| --- | --- | --- | --- | --- | --- | --- |
| GET | `/students` | A, T | `q?, teacherId?, status?, needsConsulting?` |  | `StudentListItem[]` | `fetchStudents (catalog.ts)` |
| GET | `/students/{id}/profile` | Any |  |  | `StudentProfileDto` | `fetchStudentProfile (catalog.ts)` |
| PUT | `/students/{id}/profile` | A |  | `StudentProfileRequest` | `StudentProfileDto` | `updateStudentProfile (catalog.ts)` |
| GET | `/teachers/{id}/profile` | A, T |  |  | `TeacherProfileDto` | `fetchTeacherProfile (catalog.ts)` |
| PUT | `/teachers/{id}/profile` | A, T |  | `TeacherProfileRequest` | `TeacherProfileDto` | `updateTeacherProfile (catalog.ts)` |

```ts
interface StudentListItem {
  id: string
  fullName: string
  email: string
  status: 'AWAITING_GUARDIAN_CONSENT' | 'INVITED' | 'ACTIVE' | 'INACTIVE'
  selfReportedLevel: 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2'
  teacherIds: string[]
  needsConsulting: boolean
}

interface StudentProfileDto {
  userId: string
  selfReportedLevel: 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2'
  occupation: string
  notes: string
}

interface StudentProfileRequest {
  selfReportedLevel: 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2'
  occupation: string
  notes: string
}

interface TeacherProfileDto {
  userId: string
  bio: string
  specialties: string[]
  certificates: string
}

interface TeacherProfileRequest {
  bio: string
  specialties: string[]
  certificates: string
}
```

**Mã lỗi cần xử lý:** `NOT_FOUND`, `FORBIDDEN`, `VALIDATION_FAILED`

**Lưu ý:**

- Giáo viên lấy tên, email học viên từ `GET /students`, không có số điện thoại và ngày sinh.
- `notes` của học viên chỉ giáo vụ thấy. Giáo viên xem học viên không được phân công nhận 404.
- **Lỗi đã biết:** giáo viên bấm "Chi tiết & Học phí" bị đẩy về `/teacher` vì link trỏ tới `/admin/enrollments/{id}`; chỉ hiện link cho admin.

<a id="catalog-enrollments"></a>

### Ghi danh và gói của học viên

**Người phụ trách:** #4 · **Task:** T055, T116 · **FR:** FR-006, FR-024, FR-026 · **Quickstart:** bước 2, 13, 20

**Trạng thái:** Trang giáo vụ đã có. `/student/packages` còn thiếu lịch sử thanh toán, công nợ và banner gia hạn (T116).

**Route:**

- `/admin/enrollments/:id`
- `/student/packages`

**File:**

- `src/features/catalog/enrollments/EnrollmentDetailPage.tsx`
- `src/features/catalog/enrollments/CreateEnrollmentDialog.tsx`
- `src/features/catalog/enrollments/StudentPackagesPage.tsx`
- `src/lib/api/catalog.ts`
- Mock: `src/mocks/handlers/catalog.ts`

**API:**

| Method | Path | Quyền | Query | Body | Trả về | Hàm FE |
| --- | --- | --- | --- | --- | --- | --- |
| GET | `/enrollments` | Any | `studentId?, teacherId?, status?` |  | `EnrollmentDto[]` | `fetchEnrollments (catalog.ts), fetchEnrollments (scheduling.ts)` |
| POST | `/enrollments` | A |  | `CreateEnrollmentRequest` | `201 EnrollmentDto` | `createEnrollment (catalog.ts)` |
| GET | `/enrollments/{id}` | Any |  |  | `EnrollmentDto` | `fetchEnrollment (catalog.ts)` |
| PUT | `/enrollments/{id}/teacher` | A |  | `ChangeTeacherRequest` | `EnrollmentDto` | `changeTeacher (catalog.ts)` |
| POST | `/enrollments/{id}/extend` | A |  | `ExtendRequest` | `EnrollmentDto` | `extendEnrollment (catalog.ts)` |
| POST | `/enrollments/{id}/cancel` | A |  | `CancelRequest (tùy chọn)` | `EnrollmentDto` | `cancelEnrollment (catalog.ts)` |
| GET | `/enrollments/{id}/teacher-history` | A |  |  | `AssignmentDto[]` | `fetchTeacherHistory (catalog.ts)` |

```ts
interface EnrollmentDto {
  id: string
  studentId: string
  teacherId: string
  packageId: string
  sessionsTotal: number
  sessionsReserved: number
  sessionsConsumed: number
  sessionsRemaining: number
  sessionMinutes: number
  priceVnd: number
  activatedAt: string /* ISO-8601 */
  expiresAt: string /* ISO-8601 */
  status: 'PENDING_PAYMENT' | 'ACTIVE' | 'EXHAUSTED' | 'EXPIRED' | 'CANCELLED'
  cancelReason: string
  cancelledBy: string
  cancelledAt: string /* ISO-8601 */
  version: number
}

interface CreateEnrollmentRequest {
  studentId: string
  packageId: string
  teacherId: string
  priceVnd: number
}

interface ChangeTeacherRequest {
  teacherId: string
  reason: string
}

interface ExtendRequest {
  newExpiresAt: string /* ISO-8601 */
}

interface CancelRequest {
  reason: string
}

interface AssignmentDto {
  fromTeacherId: string
  toTeacherId: string
  changedBy: string
  reason: string
  changedAt: string /* ISO-8601 */
}
```

**Mã lỗi cần xử lý:** `ENROLLMENT_NOT_ACTIVE`, `INVALID_STATE_TRANSITION`, `ANONYMIZED`, `VALIDATION_FAILED`

**Lưu ý:**

- `EnrollmentStatus` ở `lib/api/catalog.ts` đang ghi `COMPLETED`; backend trả `EXHAUSTED`. `StatusBadge` đã có nhãn đúng, chỉ cần sửa type.
- Banner gia hạn: hiện khi còn ≤ 2 buổi hoặc ≤ 7 ngày.
- Hủy gói bị từ chối nếu còn buổi đã đặt (`sessionsReserved > 0`).
- **Lỗi đã biết:** trang chi tiết hiện ID rút gọn thay vì tên học viên, giáo viên.

<a id="billing"></a>

### Thanh toán và công nợ

**Người phụ trách:** #4 · **Task:** T114, T115 · **FR:** FR-025 · **Quickstart:** bước 3

**Trạng thái:** Thu học phí đã có trong trang ghi danh. Thiếu `AdjustmentDialog` (T114) và trang công nợ: `/admin/billing` đang là placeholder (T115).

**Route:**

- Trong `/admin/enrollments/:id`
- `/admin/billing`

**File:**

- `src/features/catalog/enrollments/ManualPaymentDialog.tsx`
- `src/features/billing/ReceivablesPage.tsx` (cần tạo)
- `src/features/billing/AdjustmentDialog.tsx` (cần tạo)
- `src/lib/api/billing.ts`
- Mock: chưa có

**API:**

| Method | Path | Quyền | Query | Body | Trả về | Hàm FE |
| --- | --- | --- | --- | --- | --- | --- |
| GET | `/enrollments/{enrollmentId}/payments` | A, S |  |  | `StatementDto` | `fetchStatement (billing.ts)` |
| POST | `/enrollments/{enrollmentId}/payments` | A |  | `PaymentRequest` | `201 PaymentDto` | `recordPayment (billing.ts)` |
| POST | `/enrollments/{enrollmentId}/payments/adjustments` | A |  | `AdjustmentRequest` | `201 PaymentDto` | `recordAdjustment (billing.ts)` |
| GET | `/receivables` | A | `hasDebt?` |  | `ReceivableDto[]` | chưa có |

```ts
interface StatementDto {
  enrollmentId: string
  amountDueVnd: number
  amountPaidVnd: number
  debtVnd: number
  payments: PaymentDto[]
}

interface PaymentRequest {
  amountVnd: number
  paidAt: string /* YYYY-MM-DD */
  method: 'BANK_TRANSFER' | 'CASH' | 'OTHER'
  note: string
}

interface PaymentDto {
  id: string
  amountVnd: number
  paidAt: string /* YYYY-MM-DD */
  method: 'BANK_TRANSFER' | 'CASH' | 'OTHER'
  adjustment: boolean
  note: string
  recordedBy: string
  recordedAt: string /* ISO-8601 */
}

interface AdjustmentRequest {
  amountVnd: number
  note: string
}

interface ReceivableDto {
  enrollmentId: string
  studentId: string
  studentName: string
  amountDueVnd: number
  amountPaidVnd: number
  debtVnd: number
  cancelled: boolean
}
```

**Mã lỗi cần xử lý:** `ANONYMIZED`, `VALIDATION_FAILED`

**Lưu ý:**

- Lần thu đầu tiên kích hoạt gói. Thu vượt công nợ được chấp nhận và không bù sang ghi danh khác.
- Điều chỉnh dùng số âm và bắt buộc có ghi chú. Ô ghi chú nên gợi ý "không ghi tên học viên".
- `hasDebt=true`: còn nợ và ghi danh chưa hủy, nợ nhiều nhất đứng trước.
- Chưa có mock cho `/receivables`, nên thử thẳng với backend.

<a id="admin-dashboard"></a>

### Tổng quan giáo vụ

**Người phụ trách:** #4 · **Task:** T123 · **FR:** FR-027 · **Quickstart:** bước 12

**Trạng thái:** Chưa có: `/admin` đang là placeholder.

**Route:**

- `/admin`

**File:**

- `src/features/admin/dashboard/DashboardPage.tsx` (cần tạo)
- Mock: chưa có

**API:**

| Method | Path | Quyền | Query | Body | Trả về | Hàm FE |
| --- | --- | --- | --- | --- | --- | --- |
| GET | `/reports/revenue` | A | `from?, to?` |  | `Revenue` | chưa có |
| GET | `/reports/teacher-sessions` | A | `from?, to?` |  | `TeacherSessionsDto[]` | chưa có |

```ts
interface Revenue {
  from: string /* YYYY-MM-DD */
  to: string /* YYYY-MM-DD */
  collectedVnd: number
  debtVnd: number
}

interface TeacherSessionsDto {
  teacherId: string
  teacherName: string
  sessionsTaught: number
}
```

**Mã lỗi cần xử lý:** `VALIDATION_FAILED`

**Lưu ý:**

- `from`, `to` là ngày dạng `YYYY-MM-DD`, tùy chọn; `from` sau `to` trả 400.
- `collectedVnd` theo kỳ. `debtVnd` là số dư hiện tại, không theo kỳ, nên thẻ cần ghi rõ "hiện tại".
- `sessionsTaught` gồm `COMPLETED`, `STUDENT_NO_SHOW`, `CANCELLED_LATE`. Báo cáo chất lượng FR-023 để GĐ2, không làm.
- Cần thêm hàm trong `lib/api`.

<a id="admin-privacy"></a>

### Quyền riêng tư

**Người phụ trách:** #4 · **Task:** T130, T131 · **FR:** FR-030, FR-031 · **Quickstart:** bước 15, 23

**Trạng thái:** Dialog đồng ý người giám hộ đã có. `/admin/privacy` đang là placeholder. Không được cắt phần này: FR-030, FR-031 là bắt buộc theo constitution.

**Route:**

- `/admin/privacy`
- Trang tóm tắt in được, route do #4 đặt

**File:**

- `src/features/admin/privacy/DataSubjectRequestsPage.tsx` (cần tạo)
- `src/features/admin/privacy/PersonalDataSummaryPage.tsx` (cần tạo)
- `src/features/admin/users/GuardianConsentDialog.tsx`
- Mock: chưa có

**API:**

| Method | Path | Quyền | Query | Body | Trả về | Hàm FE |
| --- | --- | --- | --- | --- | --- | --- |
| GET | `/data-subject-requests` | A | `studentId?, status?` |  | `RequestDto[]` | chưa có |
| POST | `/data-subject-requests` | A |  | `CreateRequest` | `201 RequestDto` | chưa có |
| POST | `/data-subject-requests/{id}/reject` | A |  | `RejectRequest` | `RequestDto` | chưa có |
| POST | `/data-subject-requests/{id}/complete-access` | A |  |  | `RequestDto` | chưa có |
| POST | `/data-subject-requests/{id}/anonymize` | A |  | `AnonymizeRequest` | `RequestDto` | chưa có |

```ts
interface RequestDto {
  id: string
  studentId: string
  requesterName: string
  type: 'ERASURE' | 'ACCESS'
  receivedOn: string /* YYYY-MM-DD */
  status: 'RECEIVED' | 'COMPLETED' | 'REJECTED'
  resolvedAt: string /* ISO-8601 */
  resolutionNote: string
  handledBy: string
}

interface CreateRequest {
  studentId: string
  requesterName: string
  type: 'ERASURE' | 'ACCESS'
  receivedOn: string /* YYYY-MM-DD */
}

interface RejectRequest {
  resolutionNote: string
}

interface AnonymizeRequest {
  confirm: boolean
}
```

**Mã lỗi cần xử lý:** `ANONYMIZED`, `INVALID_STATE_TRANSITION`, `VALIDATION_FAILED`

**Lưu ý:**

- `anonymize` phải gửi `{"confirm": true}`. Giao diện cần xác nhận "không khôi phục được".
- Không có API tổng hợp cho trang tóm tắt. Ghép từ `/users/{id}`, `/students/{id}/profile`, `/enrollments`, `/sessions`, `/enrollments/{id}/payments`, `/students/{id}/progress`, `/assignments`.
- Trang tóm tắt cần `@media print` và nút "In / lưu PDF".

## Lịch và phòng học

<a id="scheduling-availability"></a>

### Lịch rảnh và ngày nghỉ

**Người phụ trách:** #2 · **Task:** T075 · **FR:** FR-008 · **Quickstart:** bước 4, 21

**Trạng thái:** Đã có, chờ đối chiếu rồi tick.

**Route:**

- `/teacher/availability`
- Tab trong `TeacherProfilePage`

**File:**

- `src/features/scheduling/AvailabilityEditor.tsx`
- `src/lib/api/scheduling.ts`
- Mock: `src/mocks/handlers/scheduling.ts`

**API:**

| Method | Path | Quyền | Query | Body | Trả về | Hàm FE |
| --- | --- | --- | --- | --- | --- | --- |
| GET | `/teachers/{teacherId}/availability` | A, T |  |  | `RuleDto[]` | `fetchTeacherAvailability (scheduling.ts)` |
| PUT | `/teachers/{teacherId}/availability` | A, T |  | `RuleDto[]` | `RuleDto[]` | `saveTeacherAvailability (scheduling.ts)` |
| GET | `/teachers/{teacherId}/availability/exceptions` | A, T |  |  | `ExceptionDto[]` | `fetchTeacherExceptions (scheduling.ts)` |
| POST | `/teachers/{teacherId}/availability/exceptions` | A, T |  | `ExceptionRequest` | `201 ExceptionDto` | `addTeacherException (scheduling.ts)` |
| DELETE | `/teachers/{teacherId}/availability/exceptions/{exceptionId}` | A, T |  |  | `204, không có nội dung` | `deleteTeacherException (scheduling.ts)` |

```ts
interface RuleDto {
  dayOfWeek: 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'
  startTime: string /* HH:mm:ss */
  endTime: string /* HH:mm:ss */
}

interface ExceptionDto {
  id: string
  date: string /* YYYY-MM-DD */
  startTime: string /* HH:mm:ss */
  endTime: string /* HH:mm:ss */
  reason: string
}

interface ExceptionRequest {
  date: string /* YYYY-MM-DD */
  startTime: string /* HH:mm:ss */
  endTime: string /* HH:mm:ss */
  reason: string
}
```

**Mã lỗi cần xử lý:** `EXCEPTION_OVERLAPS_SESSIONS`, `VALIDATION_FAILED`

**Lưu ý:**

- Giờ tính theo múi giờ của giáo viên. Không có `startTime`, `endTime` nghĩa là nghỉ cả ngày.
- Còn phải kiểm: hiện danh sách buổi trùng khi `EXCEPTION_OVERLAPS_SESSIONS`.

<a id="scheduling-booking"></a>

### Đặt buổi

**Người phụ trách:** #2 · **Task:** T076 · **FR:** FR-009, FR-010 · **Quickstart:** bước 5

**Trạng thái:** Đã có, chờ đối chiếu rồi tick.

**Route:**

- `/student/book`

**File:**

- `src/features/scheduling/BookSessionPage.tsx`
- `src/features/scheduling/SlotPicker.tsx`
- `src/lib/api/scheduling.ts`
- Mock: `src/mocks/handlers/scheduling.ts`

**API:**

| Method | Path | Quyền | Query | Body | Trả về | Hàm FE |
| --- | --- | --- | --- | --- | --- | --- |
| GET | `/enrollments/{enrollmentId}/slots` | A, S | `from?, to?` |  | `Slot[]` | `fetchSlots (scheduling.ts)` |
| POST | `/sessions` | A, S |  | `BookRequest` | `201 SessionDto` | `bookSession (scheduling.ts)` |

```ts
interface Slot {
  startsAt: string /* ISO-8601 */
  endsAt: string /* ISO-8601 */
}

interface BookRequest {
  enrollmentId: string
  startsAt: string /* ISO-8601 */
}

interface SessionDto {
  id: string
  enrollmentId: string
  teacherId: string
  studentId: string
  startsAt: string /* ISO-8601 */
  endsAt: string /* ISO-8601 */
  status: 'BOOKED' | 'COMPLETED' | 'STUDENT_NO_SHOW' | 'TEACHER_ABSENT' | 'CANCELLED_ON_TIME' | 'CANCELLED_LATE' | 'CANCELLED_SYSTEM' | 'INTERRUPTED'
  bookedBy: string
  cancelledBy: string
  cancelReason: string
  rescheduledFromId: string
  outcomeBy: string
  teacherJoinedAt: string /* ISO-8601 */
  studentJoinedAt: string /* ISO-8601 */
  version: number
}
```

**Mã lỗi cần xử lý:** `SLOT_TAKEN`, `NO_SESSIONS_LEFT`, `ENROLLMENT_NOT_ACTIVE`

**Lưu ý:**

- Slot bước 30 phút, cửa sổ tối đa 30 ngày; phải đặt trước ít nhất 2 giờ.
- `Slot` của backend chỉ có `startsAt`, `endsAt`; thời lượng tính bằng `slotMinutes()` (PR #10).
- **Lỗi đã biết:** `SlotPicker.tsx:16,23` dùng `toISOString`, trước 7h sáng ngày lệch về hôm trước.

<a id="scheduling-session"></a>

### Lịch và chi tiết buổi

**Người phụ trách:** #2 · **Task:** T077, T078 · **FR:** FR-011, FR-014 · **Quickstart:** bước 9, 17, 18, 19

**Trạng thái:** Đã có xem và hủy. Thiếu đổi lịch và quyết định gián đoạn (T078).

**Route:**

- `/admin/schedule`, `/teacher/schedule`, `/student/schedule`
- `/sessions/:id`

**File:**

- `src/features/scheduling/ScheduleCalendar.tsx`
- `src/features/scheduling/SessionDetailDrawer.tsx`
- `src/features/scheduling/SessionDetailPage.tsx`
- `src/features/scheduling/SessionOutcomeDialog.tsx`
- `src/lib/api/scheduling.ts`
- Mock: `src/mocks/handlers/scheduling.ts`

**API:**

| Method | Path | Quyền | Query | Body | Trả về | Hàm FE |
| --- | --- | --- | --- | --- | --- | --- |
| GET | `/sessions` | Any | `from?, to?, teacherId?, studentId?, status?, outcomePending?` |  | `SessionDto[]` | `fetchSessions (scheduling.ts)` |
| GET | `/sessions/{id}` | Any |  |  | `SessionDto` | `fetchSession (scheduling.ts)` |
| GET | `/sessions/{id}/cancel-preview` | A, S |  |  | `Preview` | `previewCancelSession (scheduling.ts)` |
| POST | `/sessions/{id}/cancel` | Any |  | `CancelRequest (tùy chọn)` | `SessionDto` | `cancelSession (scheduling.ts)` |
| POST | `/sessions/{id}/reschedule` | A, S |  | `RescheduleRequest` | `SessionDto` | `rescheduleSession (scheduling.ts)` |
| POST | `/sessions/{id}/outcome` | A, T |  | `OutcomeRequest` | `SessionDto` | `recordOutcome (scheduling.ts)` |
| POST | `/sessions/{id}/interruption-decision` | A |  | `InterruptionRequest` | `SessionDto` | chưa có |
| GET | `/sessions/{id}/outcome-history` | A |  |  | `OutcomeChangeDto[]` | chưa có |

```ts
interface SessionDto {
  id: string
  enrollmentId: string
  teacherId: string
  studentId: string
  startsAt: string /* ISO-8601 */
  endsAt: string /* ISO-8601 */
  status: 'BOOKED' | 'COMPLETED' | 'STUDENT_NO_SHOW' | 'TEACHER_ABSENT' | 'CANCELLED_ON_TIME' | 'CANCELLED_LATE' | 'CANCELLED_SYSTEM' | 'INTERRUPTED'
  bookedBy: string
  cancelledBy: string
  cancelReason: string
  rescheduledFromId: string
  outcomeBy: string
  teacherJoinedAt: string /* ISO-8601 */
  studentJoinedAt: string /* ISO-8601 */
  version: number
}

interface Preview {
  willConsume: boolean
  cutoffAt: string /* ISO-8601 */
}

interface CancelRequest {
  reason: string
}

interface RescheduleRequest {
  newStartsAt: string /* ISO-8601 */
}

interface OutcomeRequest {
  outcome: 'BOOKED' | 'COMPLETED' | 'STUDENT_NO_SHOW' | 'TEACHER_ABSENT' | 'CANCELLED_ON_TIME' | 'CANCELLED_LATE' | 'CANCELLED_SYSTEM' | 'INTERRUPTED'
  note: string
}

interface InterruptionRequest {
  decision: 'COUNT' | 'MAKE_UP'
}

interface OutcomeChangeDto {
  fromStatus: 'BOOKED' | 'COMPLETED' | 'STUDENT_NO_SHOW' | 'TEACHER_ABSENT' | 'CANCELLED_ON_TIME' | 'CANCELLED_LATE' | 'CANCELLED_SYSTEM' | 'INTERRUPTED'
  toStatus: 'BOOKED' | 'COMPLETED' | 'STUDENT_NO_SHOW' | 'TEACHER_ABSENT' | 'CANCELLED_ON_TIME' | 'CANCELLED_LATE' | 'CANCELLED_SYSTEM' | 'INTERRUPTED'
  changedBy: string
  note: string
  changedAt: string /* ISO-8601 */
}
```

**Mã lỗi cần xử lý:** `CANCEL_WINDOW_PASSED`, `INVALID_STATE_TRANSITION`, `SLOT_TAKEN`, `CONCURRENT_MODIFICATION`

**Lưu ý:**

- `cancel-preview` trả `{ willConsume, cutoffAt }` (PR #10). Giáo viên không có quyền gọi API này; giáo viên hủy buổi tạo `TEACHER_ABSENT`.
- `SessionDto` không có tên giáo viên, học viên. `teacherName`, `studentName` trong type FE luôn rỗng; admin lấy tên từ `/users`, giáo viên từ `/students`.
- Giáo viên hủy buổi tạo `TEACHER_ABSENT`; học viên, giáo vụ tạo `CANCELLED_ON_TIME` hoặc `CANCELLED_LATE`.
- Chốt kết quả chỉ sau giờ bắt đầu. Quyết định gián đoạn nhận `COUNT` (tính là đã học) hoặc `MAKE_UP` (xếp bù).
- Đổi lịch: gọi `rescheduleSession` đã có sẵn, ẩn nút khi còn dưới 12 giờ.

<a id="classroom"></a>

### Phòng học LiveKit

**Người phụ trách:** #2 · **Task:** T079 · **FR:** FR-012 · **Quickstart:** bước 7

**Trạng thái:** Giáo viên vào được với camera giả. Chưa thử đủ hai người; nhãn còn tiếng Anh.

**Route:**

- `/sessions/:id/classroom`

**File:**

- `src/features/classroom/ClassroomPage.tsx`
- `src/lib/api/scheduling.ts`
- Mock: `src/mocks/handlers/scheduling.ts`

**API:**

| Method | Path | Quyền | Query | Body | Trả về | Hàm FE |
| --- | --- | --- | --- | --- | --- | --- |
| POST | `/sessions/{id}/join` | T, S |  |  | `JoinInfo` | `joinClassroom (scheduling.ts)` |

```ts
interface JoinInfo {
  serverUrl: string
  token: string
  roomName: string
}
```

**Mã lỗi cần xử lý:** `ROOM_NOT_OPEN`, `NOT_FOUND`

**Lưu ý:**

- Phòng mở từ 10 phút trước giờ học tới 15 phút sau khi kết thúc, ngoài khoảng đó trả 403 `ROOM_NOT_OPEN`.
- Thử nhanh trên trình duyệt headless bằng `--use-fake-device-for-media-stream --use-fake-ui-for-media-stream`.
- Việt hóa nhãn: Microphone, Camera, Share screen, Chat, Leave.

## Học tập

<a id="learning-proficiency"></a>

### Năng lực và mục tiêu

**Người phụ trách:** #3 · **Task:** T088 · **FR:** FR-015, FR-016 · **Quickstart:** bước 11

**Trạng thái:** Đã có, đã chạy với backend thật sau PR #8.

**Route:**

- Tab "Năng lực & Mục tiêu" trong `StudentProfilePage`

**File:**

- `src/features/learning/proficiency/ProficiencyPanel.tsx`
- `src/features/learning/proficiency/AssessmentFormDialog.tsx`
- `src/features/learning/proficiency/GoalsPanel.tsx`
- `src/lib/api/learning.ts`
- Mock: `src/mocks/handlers/learning.ts`

**API:**

| Method | Path | Quyền | Query | Body | Trả về | Hàm FE |
| --- | --- | --- | --- | --- | --- | --- |
| GET | `/students/{studentId}/proficiency` | Any |  |  | `ProfileDto` | `fetchProficiency (learning.ts)` |
| GET | `/students/{studentId}/assessments` | Any |  |  | `AssessmentDto[]` | chưa có |
| POST | `/students/{studentId}/assessments` | A, T |  | `AssessmentRequest` | `201 AssessmentDto` | `createAssessment (learning.ts)` |
| GET | `/students/{studentId}/goals` | Any |  |  | `GoalDto[]` | `fetchGoals (learning.ts)` |
| POST | `/students/{studentId}/goals` | A, T |  | `GoalRequest` | `201 GoalDto` | `createGoal (learning.ts)` |
| PUT | `/students/{studentId}/goals/{goalId}` | A, T |  | `GoalUpdateRequest` | `GoalDto` | `updateGoal (learning.ts)` |

```ts
interface ProfileDto {
  current: AssessmentDto
  history: AssessmentDto[]
  needsConsulting: boolean
}

interface AssessmentDto {
  id: string
  type: 'PLACEMENT' | 'PERIODIC'
  assessedBy: string
  assessedAt: string /* YYYY-MM-DD */
  listening: 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2'
  speaking: 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2'
  reading: 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2'
  writing: 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2'
  grammarVocab: 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2'
  notes: string
  recordedAt: string /* ISO-8601 */
}

interface AssessmentRequest {
  type: 'PLACEMENT' | 'PERIODIC'
  assessedAt: string /* YYYY-MM-DD */
  listening: 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2'
  speaking: 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2'
  reading: 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2'
  writing: 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2'
  grammarVocab: 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2'
  notes: string
}

interface GoalDto {
  id: string
  type: 'IELTS' | 'TOEIC' | 'COMMUNICATION' | 'BUSINESS' | 'KIDS' | 'OTHER'
  target: string
  deadline: string /* YYYY-MM-DD */
  status: 'ACTIVE' | 'ACHIEVED' | 'DROPPED'
  createdBy: string
  createdAt: string /* ISO-8601 */
}

interface GoalRequest {
  type: 'IELTS' | 'TOEIC' | 'COMMUNICATION' | 'BUSINESS' | 'KIDS' | 'OTHER'
  target: string
  deadline: string /* YYYY-MM-DD */
}

interface GoalUpdateRequest {
  status: 'ACTIVE' | 'ACHIEVED' | 'DROPPED'
  deadline: string /* YYYY-MM-DD */
}
```

**Mã lỗi cần xử lý:** `NOT_FOUND`, `ANONYMIZED`, `VALIDATION_FAILED`

**Lưu ý:**

- Nội dung mục tiêu không sửa được; chỉ đổi `status`, `deadline`.
- Không có trình độ chung (`overall`) trong API.

<a id="learning-path"></a>

### Lộ trình

**Người phụ trách:** #3 · **Task:** T089 · **FR:** FR-017, FR-018 · **Quickstart:** bước 11

**Trạng thái:** Đã có, còn thiếu sắp xếp chặng, chủ đề.

**Route:**

- Tab "Lộ trình học tập" trong `StudentProfilePage`
- `/student/learning-path`

**File:**

- `src/features/learning/path/LearningPathEditor.tsx`
- `src/features/learning/path/LearningPathView.tsx`
- `src/features/learning/path/RevisionHistory.tsx`
- `src/features/learning/path/StudentLearningPathPage.tsx`
- `src/lib/api/learning.ts`
- Mock: `src/mocks/handlers/learning.ts`

**API:**

| Method | Path | Quyền | Query | Body | Trả về | Hàm FE |
| --- | --- | --- | --- | --- | --- | --- |
| GET | `/students/{studentId}/learning-path` | Any |  |  | `PathDto` | `fetchLearningPath (learning.ts)` |
| POST | `/students/{studentId}/learning-path` | A, T |  | `CreatePathRequest` | `201 PathDto` | `createLearningPath (learning.ts)` |
| PUT | `/learning-paths/{pathId}` | A, T |  | `UpdatePathRequest` | `PathDto` | `updateLearningPath (learning.ts)` |
| GET | `/learning-paths/{pathId}/revisions` | A, T |  |  | `RevisionDto[]` | `fetchPathRevisions (learning.ts)` |

```ts
interface PathDto {
  id: string
  studentId: string
  title: string
  status: 'ACTIVE' | 'ARCHIVED'
  completionPercent: number
  milestones: MilestoneDto[]
  version: number
}

interface CreatePathRequest {
  title: string
  milestones: MilestoneSpec[]
}

interface UpdatePathRequest {
  title: string
  milestones: MilestoneSpec[]
  reason: string
  version: number
}

interface RevisionDto {
  id: string
  changedBy: string
  reason: string
  summary: string
  changedAt: string /* ISO-8601 */
}

interface MilestoneDto {
  id: string
  position: number
  title: string
  targetDate: string /* YYYY-MM-DD */
  status: 'PLANNED' | 'IN_PROGRESS' | 'DONE'
  topics: TopicDto[]
}

interface MilestoneSpec {
  id: string
  title: string
  targetDate: string /* YYYY-MM-DD */
  status: 'PLANNED' | 'IN_PROGRESS' | 'DONE'
  topics: TopicSpec[]
}

interface TopicDto {
  id: string
  position: number
  title: string
  objective: string
  status: 'PLANNED' | 'DONE'
}

interface TopicSpec {
  id: string
  title: string
  objective: string
}
```

**Mã lỗi cần xử lý:** `NOT_FOUND`, `CONCURRENT_MODIFICATION`, `ANONYMIZED`, `VALIDATION_FAILED`

**Lưu ý:**

- Phần trăm hoàn thành là `completionPercent` (PR #10).
- Chưa có lộ trình thì `GET` trả 404. Đó là trạng thái rỗng, không phải lỗi.
- Sửa lộ trình phải gửi `version` lần đọc gần nhất và `reason`.
- Trạng thái chủ đề chỉ đổi qua `completedTopicIds` của phiếu đánh giá. Backend bỏ qua nếu gửi trong lộ trình.
- Sau PR #9: tiêu đề tối đa 200 ký tự, mục tiêu chủ đề tối đa 2.000.

<a id="learning-report"></a>

### Phiếu đánh giá buổi

**Người phụ trách:** #3 · **Task:** T101 · **FR:** FR-019 · **Quickstart:** bước 8

**Trạng thái:** Chưa có: `/teacher` đang là placeholder.

**Route:**

- `/teacher` (danh sách phiếu cần nộp)
- Mở từ `/sessions/:id`

**File:**

- `src/features/learning/report/PendingReportsList.tsx` (cần tạo)
- `src/features/learning/report/SessionReportForm.tsx` (cần tạo)
- Mock: chưa có

**API:**

| Method | Path | Quyền | Query | Body | Trả về | Hàm FE |
| --- | --- | --- | --- | --- | --- | --- |
| GET | `/session-reports` | A, T | `status?` |  | `ReportListItem[]` | chưa có |
| GET | `/sessions/{sessionId}/report` | Any |  |  | `ReportDto` | chưa có |
| PUT | `/sessions/{sessionId}/report` | T |  | `SubmitRequest` | `ReportDto` | chưa có |

```ts
interface ReportListItem {
  sessionId: string
  teacherId: string
  teacherName: string
  studentId: string
  studentName: string
  status: 'PENDING' | 'SUBMITTED'
  sessionEndsAt: string /* ISO-8601 */
  dueAt: string /* ISO-8601 */
  overdue: boolean
  onTime: boolean
}

interface ReportDto {
  id: string
  sessionId: string
  teacherId: string
  studentId: string
  status: 'PENDING' | 'SUBMITTED'
  sessionEndsAt: string /* ISO-8601 */
  dueAt: string /* ISO-8601 */
  overdue: boolean
  onTime: boolean
  taughtTopicIds: string[]
  scores: Scores
  comment: string
  submittedAt: string /* ISO-8601 */
}

interface SubmitRequest {
  taughtTopicIds: string[]
  completedTopicIds: string[]
  scores: Scores
  comment: string
  assignments: AssignmentRequest[]
}

interface Scores {
  listening: number
  speaking: number
  reading: number
  writing: number
  grammarVocab: number
}

interface AssignmentRequest {
  title: string
  instructions: string
  dueAt: string /* ISO-8601 */
}
```

**Mã lỗi cần xử lý:** `NOT_FOUND`, `INVALID_STATE_TRANSITION`, `ANONYMIZED`, `VALIDATION_FAILED`

**Lưu ý:**

- Phiếu tự sinh khi buổi được chốt `COMPLETED`, hạn nộp 24 giờ sau khi buổi kết thúc.
- Nộp một lần, không sửa. "Lưu nháp" phải tự lưu ở trình duyệt.
- `comment` bắt buộc; điểm 1–5, được bỏ trống từng kỹ năng; tối đa 20 bài tập sau PR #9.
- Chủ đề lấy từ lộ trình ACTIVE của học viên; chủ đề ngoài lộ trình trả 400.
- Giáo viên đã bị đổi chỉ thấy phiếu chưa nộp của buổi mình dạy. Phải điền được dưới 3 phút (SC-008).
- Cần thêm hàm trong `lib/api`.

<a id="learning-assignments"></a>

### Bài tập

**Người phụ trách:** #3 · **Task:** T102 · **FR:** FR-020 · **Quickstart:** bước 10

**Trạng thái:** Chưa có màn, route và mục menu.

**Route:**

- Route cho học viên và giáo viên, #3 thêm vào `router.tsx` và `AppLayout.tsx`

**File:**

- `src/features/learning/assignments/AssignmentsPage.tsx` (cần tạo)
- Mock: chưa có

**API:**

| Method | Path | Quyền | Query | Body | Trả về | Hàm FE |
| --- | --- | --- | --- | --- | --- | --- |
| GET | `/assignments` | Any | `studentId?, status?` |  | `AssignmentDto[]` | chưa có |
| PUT | `/assignments/{id}/submission` | S |  | `SubmitRequest` | `AssignmentDto` | chưa có |
| POST | `/assignments/{id}/grade` | T |  | `GradeRequest` | `AssignmentDto` | chưa có |

```ts
interface AssignmentDto {
  id: string
  sessionId: string
  studentId: string
  teacherId: string
  title: string
  instructions: string
  dueAt: string /* ISO-8601 */
  status: 'PENDING' | 'SUBMITTED' | 'GRADED'
  submission: SubmissionDto
}

interface SubmitRequest {
  textAnswer: string
}

interface GradeRequest {
  score: number
  feedback: string
}

interface SubmissionDto {
  textAnswer: string
  submittedAt: string /* ISO-8601 */
  late: boolean
  score: number
  feedback: string
  gradedBy: string
  gradedAt: string /* ISO-8601 */
}
```

**Mã lỗi cần xử lý:** `NOT_FOUND`, `INVALID_STATE_TRANSITION`, `ANONYMIZED`, `VALIDATION_FAILED`

**Lưu ý:**

- MVP chỉ nộp văn bản; tải file để GĐ2 (K2), không làm.
- Điểm 0–10. Đã chấm thì 409, MVP không chấm lại.
- Nhãn "Nộp muộn" lấy từ `submission.late`.
- Bài tập được tạo từ phiếu đánh giá. Cần thêm hàm trong `lib/api`.

<a id="learning-progress"></a>

### Tiến bộ

**Người phụ trách:** #3 · **Task:** T103 · **FR:** FR-021 · **Quickstart:** bước 11

**Trạng thái:** Chưa có: `/student/progress` đang là placeholder.

**Route:**

- `/student/progress`
- Tab "Tiến bộ" trong `StudentProfilePage`

**File:**

- `src/features/learning/progress/ProgressPage.tsx` (cần tạo)
- Mock: chưa có

**API:**

| Method | Path | Quyền | Query | Body | Trả về | Hàm FE |
| --- | --- | --- | --- | --- | --- | --- |
| GET | `/students/{studentId}/progress` | Any |  |  | `ProgressDto` | chưa có |

```ts
interface ProgressDto {
  skillSeries: SkillPoint[]
  pathCompletion: number
  recentReports: RecentReport[]
}

interface SkillPoint {
  sessionId: string
  date: string /* YYYY-MM-DD */
  listening: number
  speaking: number
  reading: number
  writing: number
  grammarVocab: number
}

interface RecentReport {
  sessionId: string
  sessionEndsAt: string /* ISO-8601 */
  submittedAt: string /* ISO-8601 */
  scores: Scores
  comment: string
}

interface Scores {
  listening: number
  speaking: number
  reading: number
  writing: number
  grammarVocab: number
}
```

**Mã lỗi cần xử lý:** `NOT_FOUND`

**Lưu ý:**

- `recharts` đã có trong `package.json`; tải lazy.
- Điểm từng kỹ năng có thể rỗng, biểu đồ phải bỏ qua điểm rỗng.
- `pathCompletion` rỗng khi chưa có chủ đề.
- Thẻ CEFR lấy từ `GET /students/{studentId}/proficiency`. Cần thêm hàm trong `lib/api`.

<a id="learning-feedback"></a>

### Đánh giá sao

**Người phụ trách:** #3 · **Task:** T122 · **FR:** FR-022 · **Quickstart:** bước 10, 24

**Trạng thái:** Chưa có.

**Route:**

- `/student` (đang là placeholder)
- Trong `/sessions/:id`

**File:**

- `src/features/learning/feedback/SessionFeedbackPrompt.tsx` (cần tạo)
- Mock: chưa có

**API:**

| Method | Path | Quyền | Query | Body | Trả về | Hàm FE |
| --- | --- | --- | --- | --- | --- | --- |
| POST | `/sessions/{sessionId}/feedback` | S |  | `RateRequest` | `201 FeedbackDto` | chưa có |
| GET | `/session-feedback/mine` | S |  |  | `FeedbackDto[]` | chưa có |
| GET | `/sessions/{sessionId}/feedback` | A |  |  | `FeedbackDto` | chưa có |

```ts
interface RateRequest {
  stars: number
  comment: string
}

interface FeedbackDto {
  sessionId: string
  studentId: string
  teacherId: string
  stars: number
  comment: string
  createdAt: string /* ISO-8601 */
}
```

**Mã lỗi cần xử lý:** `FEEDBACK_EXISTS`, `FEEDBACK_WINDOW_CLOSED`, `NOT_FOUND`

**Lưu ý:**

- Chỉ buổi `COMPLETED`, trong 7 ngày, một lần, không sửa.
- Dùng `/session-feedback/mine` để ẩn lời mời với buổi đã chấm.
- Giáo viên không được xem đánh giá. Giáo vụ xem ở chi tiết buổi. Cần thêm hàm trong `lib/api`.
