# English 1-1 Platform

Nền tảng quản lý và đào tạo tiếng Anh 1 kèm 1 cá nhân hóa (MVP, demo 2026-11-12).

Giáo vụ quản lý học viên, giáo viên, gói học và học phí. Học viên đặt lịch theo lịch rảnh của giáo viên
và học trong phòng video tích hợp. Sau mỗi buổi, giáo viên nộp phiếu đánh giá theo lộ trình cá nhân của
học viên. Đặc tả đầy đủ ở [spec.md](specs/001-personalized-english-tutoring/spec.md).

## Cấu trúc repo

| Thư mục | Nội dung |
|---|---|
| `backend/` | Spring Boot 4.1 + Spring Modulith, Java 21, PostgreSQL, Flyway |
| `frontend/` | React + Vite + TypeScript + Tailwind |
| `deploy/` | Docker Compose production, Caddy, sao lưu và khôi phục ([hướng dẫn](deploy/README.md)) |
| `specs/001-personalized-english-tutoring/` | Spec, plan, tasks, contract API, trạng thái AI-DLC |

## Kiến trúc

Backend là **một ứng dụng** chia thành các module có ranh giới rõ. Module chỉ gọi nhau qua public API (type
ở package gốc của module) hoặc application event, không truy cập entity, repository hay bảng của module khác.
`ModularityTests` làm build thất bại nếu vi phạm.

| Module | Trách nhiệm |
|---|---|
| `identity` | Tài khoản, đăng nhập, đồng ý chính sách và người giám hộ, yêu cầu chủ thể dữ liệu, ẩn danh hóa |
| `catalog` | Hồ sơ học viên và giáo viên, khóa học, gói học, ghi danh, bộ đếm buổi, gia hạn |
| `scheduling` | Lịch rảnh, đặt/hủy/đổi lịch, trạng thái buổi, phòng học, báo cáo số buổi đã dạy |
| `learning` | Năng lực, mục tiêu, lộ trình, phiếu đánh giá, bài tập, tiến bộ, đánh giá sao |
| `billing` | Thanh toán, công nợ, báo cáo doanh thu |
| `notification` | Email và thông báo trong hệ thống, chống gửi trùng, thử lại |
| `shared` | Bảo mật, xử lý lỗi, hạ tầng dùng chung |

Dịch vụ ngoài (video, email) nằm sau port do domain định nghĩa, chọn adapter qua cấu hình, có bản giả lập để
chạy local và test. Quy tắc bắt buộc của dự án nằm trong
[constitution](.specify/memory/constitution.md); contract API ở
[rest-api.md](specs/001-personalized-english-tutoring/contracts/rest-api.md).

## Chạy local

Cần JDK 21, Node.js 22 LTS, Docker Desktop.

```bash
# Terminal 1, thư mục gốc
docker compose up -d                    # PostgreSQL, Mailpit (http://localhost:8025), LiveKit dev

# Terminal 2
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,demo-seed
# API http://localhost:8080/api/v1, Swagger UI http://localhost:8080/swagger-ui.html

# Terminal 3
cd frontend
npm install
npm run gen:api                         # sinh type từ backend đang chạy
npm run dev                             # http://localhost:5173
```

Tài khoản demo, mật khẩu `Demo@1234`: `admin@demo.local`, `teacher1@demo.local`, `teacher2@demo.local`,
`student1@demo.local` … `student5@demo.local`. Không có LiveKit thì đặt `VIDEO_PROVIDER=fake`.

Swagger và `/v3/api-docs` chỉ mở ở profile `local`. Kịch bản kiểm chứng từng bước ở
[quickstart.md](specs/001-personalized-english-tutoring/quickstart.md).

Hướng dẫn chi tiết từng phần (biến môi trường, làm mới dữ liệu demo, chạy e2e, lỗi thường gặp):
[backend/README.md](backend/README.md), [frontend/README.md](frontend/README.md).

## Kiểm thử

```bash
cd backend  && ./mvnw verify && ./mvnw spotless:check   # cần Docker: IT chạy PostgreSQL thật qua Testcontainers
cd frontend && npm run lint && npx prettier --check src && npm run test -- --run && npm run build
```

CI chạy các lệnh trên và gitleaks trên mỗi PR và khi push `main`. Các endpoint nghiệp vụ (`/api/v1`, trừ `/auth`) được
khai báo trong `AuthorizationMatrixIT`: thêm endpoint mới mà chưa khai báo thì test đỏ. Ma trận kiểm chiều từ chối
của mọi endpoint; lệnh ghi được phép chạy ở IT của từng module.

Javadoc được Spring Modulith đọc bằng parser đơn giản, nên **không viết ngoặc không đóng** như `[a, b)` trong
Javadoc; nếu không, `ModularityTests.writesModuleDocumentation` sẽ lỗi với thông báo khó hiểu.

## Phân công

| Người | Phụ trách |
|---|---|
| #1 (leader) | Toàn bộ backend, hạ tầng, CI, deploy; review security, schema, contract |
| #2 | FE `scheduling`, `classroom`, app shell, auth, consent gate; k6, Playwright |
| #3 | FE `learning`, chuông thông báo; usability phiếu đánh giá |
| #4 | FE `catalog`, `billing`, `admin` (dashboard, báo cáo, màn privacy); smoke luồng giáo vụ |

Phân chia theo unit, lịch và tiến độ ở
[unit-of-work.md](specs/001-personalized-english-tutoring/aidlc/unit-of-work.md) và
[aidlc-state.md](specs/001-personalized-english-tutoring/aidlc/aidlc-state.md).

## Quy trình PR

- Trunk-based: nhánh ngắn hạn từ `main`, PR nhỏ (mục tiêu dưới ~400 dòng), merge trong 1–2 ngày.
- Mỗi PR có ít nhất 1 review của thành viên khác. Thay đổi kiến trúc, schema dùng chung, security hoặc
  `contracts/` cần leader review; sửa module của người khác cần chủ module review.
- CI đỏ thì không merge. Logic trọng yếu (đặt lịch, trừ buổi, học phí, phân quyền) có test trước; lỗi ở phần
  này phải có test tái hiện trước khi sửa.
- Schema chỉ đổi qua migration Flyway mới, không sửa migration đã merge. Secret không vào Git.
- PR FE không sửa `aidlc-state.md`, `audit.md`, `tasks.md` của người khác và không chứa code backend.
- Tính năng đi theo Spec Kit (spec → plan → tasks → implement) và AI-DLC: duyệt plan từng unit, cài đặt, rồi
  build & test cùng review độc lập.

## Demo và triển khai

Demo chạy trên laptop, chi phí 0 đồng theo constitution (LiveKit Cloud free, Brevo free). Bộ `deploy/` đã chạy
thử trên Docker local, để dành cho lúc lên server thật: xem [deploy/README.md](deploy/README.md).
