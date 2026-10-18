# Quickstart & Validation Guide

Hướng dẫn chạy local và kịch bản kiểm chứng end-to-end cho bản demo MVP.
Chi tiết API: [contracts/rest-api.md](./contracts/rest-api.md) · Dữ liệu: [data-model.md](./data-model.md).

## 1. Yêu cầu

- JDK 21, Node.js 22 LTS, Docker Desktop (Windows: bật WSL2), Git.
- Không cần tài khoản dịch vụ trả phí để chạy local.

## 2. Chạy local

```bash
# 1. Hạ tầng local: PostgreSQL, Mailpit (email), LiveKit dev server (MinIO từ GĐ2 — K2)
docker compose up -d
#   Postgres  localhost:5432      Mailpit UI  http://localhost:8025
#   LiveKit  ws://localhost:7880

# 2. Backend (profile local: tự chạy Flyway + nạp dữ liệu demo)
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,demo-seed
#   API: http://localhost:8080/api/v1   Swagger UI: http://localhost:8080/swagger-ui.html

# 3. Frontend
cd frontend
npm install
npm run gen:api     # sinh type từ OpenAPI của backend đang chạy
npm run dev         # http://localhost:5173
```

Tài khoản demo (profile `demo-seed`, mật khẩu `Demo@1234`):
`admin@demo.local`, `teacher1@demo.local`, `teacher2@demo.local`, `student1@demo.local` … `student5@demo.local`.

Chạy không cần Docker cho LiveKit: đặt `app.providers.video=fake`.

## 3. Chạy test

```bash
cd backend && ./mvnw verify      # unit + integration (Testcontainers) + ApplicationModules.verify()
cd frontend && npm run lint && npm run test && npm run build
cd frontend && npx playwright test   # smoke test theo kịch bản demo (cần backend + frontend đang chạy)
k6 run perf/booking-and-join.js     # tuần 4: kiểm tra p95/p99 và SC-010 ở mức backend (aidlc/nfr.md NFR-PERF-01/02, NFR-SCAL-01)
gitleaks detect                      # quét secret (chạy trong CI trên mỗi PR)
```

## 4. Kịch bản kiểm chứng end-to-end (= kịch bản demo)

Mỗi bước nêu kết quả mong đợi; tất cả phải đúng trước khi coi bản demo hoàn thành.

| # | Bước | Kết quả mong đợi | Spec |
|---|---|---|---|
| 1 | Admin tạo giáo viên `gv.moi@…` và học viên `hv.moi@…` | Email mời xuất hiện trong Mailpit; người nhận đặt mật khẩu và đăng nhập được | US1, FR-002 |
| 2 | Admin tạo gói "Giao tiếp 12 buổi × 45 phút, 90 ngày", ghi danh học viên mới, phân công giáo viên mới | Enrollment ở `PENDING_PAYMENT`; giáo viên chỉ thấy học viên này trong danh sách của mình | US1, FR-003, FR-006 |
| 3 | Admin ghi nhận thanh toán 50% giá gói | Enrollment `ACTIVE`, còn 12 buổi; công nợ = 50% giá gói | US5, FR-025 |
| 4 | Giáo viên khai báo lịch rảnh T2–T6 18:00–21:00 | Học viên thấy các khung giờ trống bước 30 phút | US2, FR-008 |
| 5 | Học viên đặt một buổi; đồng thời mở tab thứ hai đặt cùng khung giờ | Lượt 1 thành công, còn 11 buổi; lượt 2 nhận lỗi `SLOT_TAKEN`; cả hai bên nhận email xác nhận | US2, FR-010, SC-003 |
| 6 | (Dùng buổi bắt đầu sau ~15 phút) chạy job nhắc lịch | Email nhắc 1h có trong Mailpit, không bị gửi trùng khi job chạy lại | FR-013 |
| 7 | Trước giờ 10 phút, giáo viên và học viên bấm "Vào lớp" | Cả hai vào cùng phòng, thấy video, nghe được nhau, chia sẻ màn hình và chat được; người thứ ba bị từ chối | US2, FR-012 |
| 8 | Giáo viên kết thúc buổi (COMPLETED), điền phiếu: 2 chủ đề, điểm 4 kỹ năng, nhận xét, 1 bài tập | Còn 11 buổi (đã tính 1); phiếu SUBMITTED đúng hạn; học viên nhận thông báo | US4, FR-019 |
| 9 | Học viên hủy một buổi khác khi còn > 12h, rồi hủy một buổi khi còn < 12h | Buổi 1 được hoàn; buổi 2 bị tính; số buổi còn lại đúng | FR-011 |
| 10 | Học viên nộp bài dạng văn bản, chấm 5 sao buổi học; giáo viên chấm bài | Giáo viên đọc được bài; điểm và phản hồi hiển thị cho học viên (nộp tệp: GĐ2, K2) | FR-020, FR-022 |
| 11 | Học viên mở "Tiến bộ" | Thấy hồ sơ CEFR, lộ trình với % hoàn thành, biểu đồ điểm kỹ năng, nhận xét các buổi | US3, FR-021 |
| 12 | Admin mở dashboard | Doanh thu đã thu, công nợ, số buổi đã dạy theo giáo viên khớp với dữ liệu trên (báo cáo chất lượng FR-023: GĐ2, K1) | FR-027, SC-007 |
| 13 | Đặt gói còn 2 buổi, chạy job gia hạn | Học viên và admin nhận nhắc gia hạn | FR-026 |
| 14 | Đăng nhập bằng giáo viên khác, gọi API xem học viên ở bước 1 | 404 — không lộ dữ liệu | FR-003, constitution V |
| 15 | Admin tạo học viên 12 tuổi (có ngày sinh) | Trạng thái `AWAITING_GUARDIAN_CONSENT`, không có email mời; sau khi admin ghi đồng ý người giám hộ → email mời xuất hiện | FR-030 |
| 16 | Học viên ở bước 15 đặt mật khẩu nhưng không tích đồng ý; sau đó tích đồng ý | Lần 1 bị từ chối; lần 2 đăng nhập được. Đổi `app.privacy.policy-version`, đăng nhập lại → mọi API trả `CONSENT_REQUIRED` cho tới khi đồng ý | FR-030 |
| 17 | Học viên còn < 12h tới buổi bấm "Đổi lịch", rồi bấm "Hủy" | Đổi lịch bị chặn (`CANCEL_WINDOW_PASSED`); hủy hiện cảnh báo "sẽ bị trừ buổi" trước khi xác nhận | FR-011 |
| 18 | Để một buổi đã qua giờ kết thúc mà giáo viên không chốt; giả lập `now = ends_at + 24h` và chạy job | Buổi vẫn `BOOKED`, số buổi chưa trừ; giáo viên + admin nhận nhắc; admin chốt `COMPLETED` → trừ 1 buổi | FR-014 |
| 19 | Giáo viên đánh dấu một buổi `INTERRUPTED`; admin chọn "xếp bù" ở một buổi, "tính là đã học" ở buổi khác | Buổi xếp bù không trừ (trả chỗ); buổi được tính → `COMPLETED`, trừ 1 buổi | FR-014 |
| 20 | Admin đổi giáo viên phụ trách khi học viên còn 2 buổi đã đặt tương lai | Hai buổi → `CANCELLED_SYSTEM`, số buổi được trả lại; học viên và hai giáo viên nhận thông báo; tỷ lệ hủy của giáo viên cũ không đổi | FR-006, FR-023 |
| 21 | Giáo viên thêm ngày nghỉ trùng một buổi đã đặt | 409 `EXCEPTION_OVERLAPS_SESSIONS` kèm buổi trùng | FR-008 |
| 22 | Admin ngừng kích hoạt giáo viên còn học viên | 409 `TEACHER_HAS_STUDENTS` | FR-004 |
| 23 | Admin ghi yêu cầu xem dữ liệu rồi mở trang tóm tắt; ghi yêu cầu xóa rồi ẩn danh hóa học viên | Trang tóm tắt in được; sau ẩn danh hóa: tên/email/ngày sinh/lộ trình/bài nộp không còn; doanh thu, công nợ, số buổi theo giáo viên ở dashboard **không đổi** | FR-031, SC-007 |
| 24 | Học viên gửi đánh giá sao lần 2 cho cùng buổi; giáo viên gọi API xem đánh giá | Lần 2 → `FEEDBACK_EXISTS`; giáo viên → 403/404 | FR-022 |

## 5. Triển khai bản demo (miễn phí)

Xem [research.md](./research.md) R13. Tóm tắt: frontend → Cloudflare Pages; backend + PostgreSQL →
Docker Compose trên Oracle Cloud Always Free VM (Caddy làm HTTPS); LiveKit Cloud free; Brevo/Gmail
SMTP; Cloudflare R2. Secret đặt qua biến môi trường, không commit (`.env.example` liệt kê tên biến).
