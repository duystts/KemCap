# Backend

Spring Boot 4.1 + Spring Modulith, Java 21, PostgreSQL 16, Flyway. API ở `http://localhost:8080/api/v1`.
Kiến trúc và danh sách module xem [README gốc](../README.md).

## Yêu cầu

- JDK 21 (`java -version` phải ra 21; nếu máy có nhiều JDK, đặt `JAVA_HOME` trỏ vào JDK 21).
- Docker Desktop (Windows: bật WSL2). Cần cho cả lúc chạy lẫn lúc test (Testcontainers).
- Không cần cài Maven: dùng `./mvnw` (Windows CMD/PowerShell: `mvnw.cmd`).

## Chạy local

```bash
# 1. Hạ tầng, ở thư mục gốc repo
docker compose up -d
#   PostgreSQL  localhost:5432 (db english, user app, mật khẩu app)
#   Mailpit     http://localhost:8025   (xem email hệ thống gửi đi)
#   LiveKit dev ws://localhost:7880     (phòng học video)

# 2. Backend
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,demo-seed
```

Chạy xong khi log có `Started EnglishPlatformApplication`. Kiểm tra nhanh:

```bash
curl http://localhost:8080/actuator/health        # {"status":"UP"}
```

Swagger UI: http://localhost:8080/swagger-ui.html (chỉ mở ở profile `local`).

### Profile

| Profile | Tác dụng |
|---|---|
| `local` | Giá trị dev cho database, LiveKit, JWT; mở Swagger và `/v3/api-docs` (frontend cần để chạy `gen:api`) |
| `demo-seed` | Lần chạy đầu trên database trống: tạo tài khoản demo, gói học, lịch, phiếu đánh giá. Đã có `admin@demo.local` thì bỏ qua |

Tài khoản demo, mật khẩu `Demo@1234`: `admin@demo.local`, `teacher1@demo.local`, `teacher2@demo.local`,
`student1@demo.local` … `student5@demo.local`.

### Chạy bằng file jar

Nhanh hơn khi chỉ cần chạy, không sửa code:

```bash
./mvnw -q -DskipTests package
java -jar target/english-platform-0.0.1-SNAPSHOT.jar --spring.profiles.active=local,demo-seed
```

Trên Windows, file jar bị khóa khi backend đang chạy: tắt backend trước khi `package` lại.

### Làm mới dữ liệu demo

Seed chỉ chạy trên database trống. Muốn quay về dữ liệu ban đầu (ví dụ trước khi chạy e2e):

```bash
# tắt backend trước
docker compose down -v     # xóa volume PostgreSQL
docker compose up -d
# chạy lại backend với profile local,demo-seed
```

## Biến môi trường

Profile `local` đã có giá trị mặc định cho tất cả; chỉ cần đặt khi muốn khác đi. Khi chạy **không** có
profile `local` (server thật), `DB_PASSWORD` và `JWT_SECRET` là bắt buộc.

| Biến | Mặc định | Ghi chú |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/english` | |
| `DB_USER` / `DB_PASSWORD` | `app` / `app` (profile `local`) | |
| `JWT_SECRET` | chuỗi dev (profile `local`) | Tối thiểu 32 byte, ngắn hơn thì app không khởi động. Bản demo phải đặt giá trị riêng |
| `VIDEO_PROVIDER` | `livekit` | `fake` = không cần LiveKit, phòng học không có video thật |
| `LIVEKIT_URL`, `LIVEKIT_API_KEY`, `LIVEKIT_API_SECRET` | LiveKit dev trong `docker-compose.yml` | Dùng LiveKit Cloud thì đặt theo project trên Cloud |
| `EMAIL_PROVIDER` | `smtp` | `fake` = không gửi email |
| `SMTP_HOST`, `SMTP_PORT` | `localhost`, `1025` (Mailpit) | Thêm `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_STARTTLS` khi dùng SMTP thật |
| `MAIL_FROM` | `no-reply@english.local` | |
| `FRONTEND_URL` | `http://localhost:5173` | Dùng cho link trong email và CORS |
| `PORT` | `8080` | |
| `POLICY_VERSION` | `2026-11` | Đổi giá trị thì mọi người dùng phải đồng ý lại chính sách |

Ví dụ chạy không có LiveKit:

```bash
VIDEO_PROVIDER=fake ./mvnw spring-boot:run -Dspring-boot.run.profiles=local,demo-seed
```

## Kiểm thử

```bash
./mvnw verify            # unit + integration (PostgreSQL thật qua Testcontainers) + kiểm ranh giới module
./mvnw spotless:check    # định dạng; sửa tự động bằng ./mvnw spotless:apply
```

CI chạy hai lệnh trên cho mỗi PR. Thêm endpoint mới thì phải khai báo trong `AuthorizationMatrixIT`, nếu không
test sẽ đỏ.

## Lỗi thường gặp

| Hiện tượng | Cách xử lý |
|---|---|
| `Connection refused` tới `localhost:5432` | Chưa chạy `docker compose up -d`, hoặc Docker Desktop chưa bật |
| Port 5432 hoặc 8080 đã bị chiếm | Tắt PostgreSQL cài sẵn trên máy, hoặc tiến trình cũ còn giữ port 8080 |
| `release version 21 not supported` | Đang dùng JDK khác 21: đặt `JAVA_HOME` trỏ vào JDK 21 |
| Đăng nhập tài khoản demo không được | Database đã có dữ liệu cũ, hoặc chạy thiếu profile `demo-seed`: làm mới dữ liệu như trên |
| Test IT lỗi không kết nối được Docker | Bật Docker Desktop; Testcontainers cần Docker đang chạy |
| `ModularityTests.writesModuleDocumentation` lỗi khó hiểu | Javadoc có ngoặc không đóng như `[a, b)`; sửa lại Javadoc |
