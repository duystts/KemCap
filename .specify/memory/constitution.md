<!--
Sync Impact Report
- Version change: 1.0.0 → 1.0.1 (PATCH)
- Modified sections: Ràng buộc công nghệ & Hạ tầng — Backend đổi "Spring Boot 3" thành
  "Spring Boot 4.x, mặc định 4.1"; thêm quy tắc không dùng phiên bản đã hết hỗ trợ OSS
- Modified principles: none
- Added sections: none
- Removed sections: none
- Templates: plan-template.md / spec-template.md / tasks-template.md read the constitution at runtime;
  no edits required (Constitution Check gate in plan-template applies these principles)
- Deferred TODOs: none
-->

# English 1-1 Platform Constitution

## Core Principles

### I. Modular Monolith có ranh giới rõ ràng

- Backend là MỘT ứng dụng Spring Boot, tổ chức bằng Spring Modulith thành các module:
  `identity`, `catalog`, `scheduling`, `learning`, `billing`, `notification`.
- Một module MUST NOT truy cập trực tiếp entity, repository hoặc bảng dữ liệu của module khác.
  Giao tiếp chỉ qua public API (các type ở package gốc của module) hoặc application event.
- Luồng nghiệp vụ liên module (ví dụ buổi học hoàn thành → trừ buổi → gửi thông báo) SHOULD dùng
  event để giảm phụ thuộc.
- Test `ApplicationModules.verify()` MUST chạy trong CI; vi phạm ranh giới làm build thất bại.

*Lý do*: team 4 người cần làm song song, mỗi người sở hữu một module; ranh giới rõ cho phép tách
thành service riêng sau này mà không viết lại.

### II. Provider-Agnostic (Ports & Adapters)

- Mọi dịch vụ bên ngoài (phòng học video, email, lưu trữ tệp, thanh toán, AI) MUST nằm sau một
  interface (port) do domain định nghĩa; triển khai cụ thể (adapter) được chọn qua cấu hình
  (`application.yml`/biến môi trường).
- Code nghiệp vụ MUST NOT import SDK của nhà cung cấp bên ngoài.
- Mỗi port MUST có một adapter giả lập (fake/in-memory) dùng cho test và chạy local.
- AI MUST đi qua abstraction của Spring AI để đổi model chỉ bằng cấu hình.

*Lý do*: giai đoạn demo dùng gói miễn phí; khi tăng tải phải đổi sang gói trả phí hoặc nhà cung cấp
khác mà không thay đổi nghiệp vụ.

### III. Free-First & Đơn giản (YAGNI)

- Giai đoạn demo MUST chạy được với chi phí 0 đồng (gói free hoặc tự host).
- MUST NOT thêm hạ tầng mới (message broker như RabbitMQ/Kafka, cache phân tán, microservice riêng)
  khi chưa có nhu cầu đo được và ghi lại trong plan (mục Complexity Tracking).
- Event nội bộ dùng Spring application events; việc externalize ra RabbitMQ/Kafka là bước nâng cấp
  sau, không phải mặc định.
- Ưu tiên giải pháp đơn giản nhất đáp ứng spec; tính năng ngoài phạm vi spec MUST NOT được làm.

*Lý do*: deadline MVP 1 tháng và ngân sách 0 đồng; mỗi thành phần hạ tầng thêm vào là chi phí vận
hành và thời gian.

### IV. Kiểm thử nghiệp vụ trọng yếu (NON-NEGOTIABLE)

- Logic trọng yếu MUST có test tự động trước khi merge: đặt lịch, chống trùng lịch, chính sách
  hủy/đổi, trừ và hoàn buổi, hết hạn gói, ghi nhận học phí và công nợ, phân quyền truy cập.
- Integration test MUST dùng PostgreSQL thật qua Testcontainers (không dùng H2 thay thế).
- CI MUST chạy toàn bộ test backend, test ranh giới module, và build/lint frontend trên mỗi PR;
  PR có test đỏ MUST NOT được merge.
- Bug ở logic trọng yếu MUST được tái hiện bằng test trước khi sửa.

*Lý do*: sai số buổi, trùng lịch hoặc sai học phí ảnh hưởng trực tiếp đến tiền và uy tín.

### V. Bảo mật & Dữ liệu cá nhân

- Phân quyền theo vai trò (Giáo vụ, Giáo viên, Học viên) MUST được kiểm tra ở backend cho mọi API;
  frontend chỉ ẩn/hiện giao diện, không phải lớp bảo vệ.
- Giáo viên MUST chỉ truy cập dữ liệu học viên được phân công; học viên chỉ truy cập dữ liệu của chính mình.
- Mật khẩu MUST được băm (BCrypt hoặc tương đương); secret MUST NOT được commit vào Git.
- Dữ liệu cá nhân MUST được xử lý theo quy định pháp luật Việt Nam về bảo vệ dữ liệu cá nhân; hồ sơ
  ngừng kích hoạt thay vì xóa cứng, trừ khi có yêu cầu xóa hợp lệ.
- Mọi thay đổi schema MUST qua migration Flyway; MUST NOT sửa migration đã merge.

*Lý do*: hệ thống lưu thông tin cá nhân, kết quả học tập và tài chính của học viên.

## Ràng buộc công nghệ & Hạ tầng

- **Backend**: Java 21, Spring Boot 4.x — mặc định **4.1** (dòng minor mới nhất đang được hỗ trợ
  OSS), Spring Modulith, Spring Security + JWT, Spring Data JPA, Flyway, PostgreSQL. Build bằng
  Maven hoặc Gradle (chốt trong plan, dùng thống nhất).
- **Phiên bản**: MUST NOT dùng framework/thư viện/runtime đã hết hỗ trợ OSS. Phiên bản Spring
  (Modulith, Security…) MUST lấy theo BOM của Spring Boot đang dùng. Khi dòng minor đang dùng
  sắp hết hỗ trợ OSS, team MUST lên kế hoạch nâng cấp lên dòng minor được hỗ trợ.
- **Frontend**: React + Vite + TypeScript + Tailwind CSS; gọi backend qua REST API.
- **Phòng học**: LiveKit qua port `VideoRoomProvider`.
- **AI (giai đoạn sau)**: Spring AI; model free khi demo, đổi model qua cấu hình.
- **Giao diện**: tiếng Việt; thời gian lưu dạng UTC, hiển thị theo múi giờ người xem.
- **Triển khai**: đóng gói bằng Docker; demo dùng hosting miễn phí. Thay đổi stack MUST qua sửa
  đổi constitution.

## Quy trình phát triển & Cổng chất lượng

- Git trunk-based: nhánh ngắn hạn từ `main`, PR nhỏ (mục tiêu dưới ~400 dòng thay đổi), merge
  trong 1–2 ngày.
- Mỗi PR MUST có ít nhất 1 review của thành viên khác; thay đổi kiến trúc, schema dùng chung hoặc
  security MUST được leader review.
- Mỗi module có một người sở hữu chính; thay đổi vào module của người khác cần người đó review.
- Quy trình Spec Kit: spec → plan → tasks → implement; plan MUST qua Constitution Check.
- Definition of Done: code + test xanh trên CI + review được duyệt + chạy được trên môi trường demo.

## Governance

- Constitution này có hiệu lực cao hơn mọi quy ước khác của dự án.
- Sửa đổi MUST qua PR cập nhật file này, nêu lý do và ảnh hưởng, và được leader cùng ít nhất
  1 thành viên khác chấp thuận.
- Phiên bản theo semantic versioning: MAJOR khi bỏ/định nghĩa lại nguyên tắc; MINOR khi thêm nguyên
  tắc hoặc mở rộng đáng kể; PATCH khi làm rõ câu chữ.
- Mọi review PR MUST kiểm tra tuân thủ; vi phạm có chủ đích MUST được ghi trong Complexity Tracking
  của plan kèm lý do.

**Version**: 1.0.1 | **Ratified**: 2026-10-12 | **Last Amended**: 2026-10-12
