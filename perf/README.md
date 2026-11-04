# Thử tải (T141)

Kiểm NFR-PERF-01, NFR-PERF-02, NFR-SCAL-01 và NFR-SCAL-03 trên dữ liệu 1.000 học viên, 50 giáo viên. Stack chạy
riêng dưới project `perf`, không đụng CSDL dev.

Chạy từ thư mục `perf/`:

```bash
docker compose -f docker-compose.perf.yml up -d --build --wait postgres mailpit backend
docker compose -f docker-compose.perf.yml exec -T postgres \
  psql -U app -d english -v ON_ERROR_STOP=1 -v policy_version=2026-11 < seed.sql
docker compose -f docker-compose.perf.yml --profile run run --rm k6
docker compose -f docker-compose.perf.yml down -v
```

Chạy k6 ngay sau khi seed: các phòng mở nằm trong khoảng từ 5 phút trước giờ học tới 15 phút sau khi kết thúc.
Bài chạy kéo dài khoảng 14 phút. k6 thoát với mã khác 0 nếu vi phạm ngưỡng; kết quả nằm ở `results/summary.json`.

| Kịch bản | Tải | Ngưỡng |
|---|---|---|
| `booking` | 20 người dùng ảo, 3 phút: xem slot 30 ngày rồi đặt buổi | p95 < 500 ms, p99 < 1 s, không lỗi 5xx |
| `join` | 400 lượt vào phòng trong 60 giây | như trên |
| `read` | 200 người dùng ảo trong 10 phút: lịch, thông báo, phiếu hoặc bài tập | như trên |

`join` và `read` chạy cùng lúc, sau `booking`. 50 giáo viên chỉ có 50 buổi cùng giờ, nên 400 lượt `join` lặp lại trên
100 người tham gia chứ không phải 200 buổi riêng. Lượt đăng nhập không tính vào ngưỡng vì bcrypt chậm có chủ đích.

## Kết quả 2026-11-05

Máy: laptop Windows 8 GB RAM, Docker Desktop; k6 chạy cùng máy với backend. Video dùng adapter `fake`, email qua
Mailpit, không qua Caddy. Backend `mem_limit: 1g`.

| Yêu cầu | p95 | p99 | Lỗi |
|---|---|---|---|
| `GET /enrollments/{id}/slots` (30 ngày), 1.760 lượt | 25,8 ms | 346,9 ms | 0 |
| `POST /sessions`, 1.760 lượt, 0 lượt 409 | 47,1 ms | 197,2 ms | 0 |
| API đọc với 200 người dùng ảo, 61.563 lượt | 6,4 ms | 9,7 ms | 0 |
| `POST /sessions/{id}/join`, chạy cùng `read`, 400 lượt | 15,9 ms | 570,6 ms | 0 |
| `POST /sessions/{id}/join`, chạy riêng, 401 lượt qua đủ 50 buổi | 27,6 ms | 31,2 ms | 0 |

Container backend: bộ nhớ cao nhất 558 MiB trên 1 GiB, 0 lần OOM, 0 lần khởi động lại, không tăng dần trong 10 phút
đo. Không cần thêm index.

Giới hạn của kết quả:

- Lượt `join` chạy cùng `read` chỉ rơi vào 4 buổi do lỗi rải phòng của kịch bản khi đó. Sau khi sửa, `join` chỉ được
  đo lại riêng, chưa đo lại cùng `read`.
- Bộ nhớ đo ở mức container, không đo heap sau GC.
- 50 giáo viên chỉ có 50 buổi cùng giờ, không phải 200 buổi. SC-010 còn phụ thuộc gói LiveKit (research.md R14),
  bài này không kiểm phần đó.
