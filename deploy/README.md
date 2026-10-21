# Triển khai demo lên VPS

> **Demo 12/11 chạy trên laptop, chi phí 0 đ** (constitution III; quyết định R1-b sau verify U5,
> 2026-10-21). Cách chạy local xem `specs/001-personalized-english-tutoring/quickstart.md`. Muốn có
> email và video thật trên laptop: điền `SMTP_*` (Brevo free) và `LIVEKIT_*` (LiveKit Cloud free)
> vào biến môi trường của backend.
>
> Tài liệu bên dưới dành cho **lúc thuê VPS** (khi lên startup). Bộ script đã chạy thử đầy đủ trên
> Docker local. Thuê VPS là hosting trả phí, nên **phải sửa constitution trước**.

Một VPS chạy toàn bộ hệ thống: `postgres` + `backend` + `web` (Caddy phục vụ frontend, API và HTTPS
trên **cùng một domain**) + `backup` (sao lưu hằng đêm lên Cloudflare R2). Video dùng LiveKit Cloud,
email dùng Brevo, cả hai đều dùng gói miễn phí.

Bạn chỉ cần làm 3 việc: **thuê VPS**, **tạo 3 tài khoản miễn phí**, rồi **điền `.env` và chạy
`./deploy.sh`**.

---

## 1. Thuê VPS

| Thông số | Tối thiểu |
|---|---|
| CPU / RAM / ổ đĩa | 2 vCPU · **4 GB** · 40 GB SSD (gói 2 GB chạy được nhưng build chậm) |
| Hệ điều hành | Ubuntu 22.04 hoặc 24.04, x86_64 |
| Ảo hóa | **KVM** (OpenVZ/LXC không chạy được Docker) |
| Mạng | 1 IPv4 riêng, mở cổng 80 và 443 |
| Thanh toán | Theo tháng |

Nên chọn nhà cung cấp tại Việt Nam để dữ liệu học viên nằm trong nước (Luật BVDLCN 91/2025). Khi
thuê, hỏi nhà cung cấp 3 điều: *có phải KVM không, có IPv4 riêng không, trả theo tháng được không*.

## 2. Tạo 3 tài khoản miễn phí (khoảng 15 phút)

| Dịch vụ | Lấy gì | Điền vào `.env` |
|---|---|---|
| [Brevo](https://www.brevo.com) | *Senders* → xác minh email người gửi. *SMTP & API* → *SMTP* → Login + tạo SMTP key | `SMTP_USER`, `SMTP_PASSWORD`, `MAIL_FROM` |
| [LiveKit Cloud](https://cloud.livekit.io) | Tạo project → *Settings* → *Keys* | `LIVEKIT_URL` (`wss://…`), `LIVEKIT_API_KEY`, `LIVEKIT_API_SECRET` |
| [Cloudflare R2](https://dash.cloudflare.com) | *R2* → tạo bucket `english-backup` → *Manage API tokens* → token quyền *Object Read & Write* cho bucket đó | `R2_ENDPOINT`, `R2_BUCKET`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY` |

## 3. Deploy

```bash
ssh root@<ip-vps>
git clone <repo-url> english && cd english/deploy
./deploy.sh          # lần đầu: tạo deploy/.env rồi dừng
nano .env            # điền 10 dòng đánh dấu (*), để trống các dòng còn lại
./deploy.sh          # kiểm tra VPS → cài Docker → build → HTTPS → sao lưu đầu tiên
```

Script tự sinh mật khẩu DB và `JWT_SECRET`. Nếu `DOMAIN` để trống, script dùng `<ip>.sslip.io`, vẫn
có HTTPS mà không cần mua domain. Khi chạy xong, script in ra link ứng dụng.

**Muốn có dữ liệu demo:** bỏ dấu `#` ở dòng `SPRING_PROFILES_ACTIVE=demo-seed` trước lần chạy đầu.
Tài khoản mẫu dùng mật khẩu công khai `Demo@1234`, nên phải **đổi mật khẩu trước khi gửi link cho
người khác** (R18).

**Cập nhật lên bản mới:** `git pull && ./deploy.sh`.

## 4. Theo dõi (T139)

- Tạo monitor *HTTP(s)* miễn phí trên [UptimeRobot](https://uptimerobot.com) trỏ tới
  `https://<domain>/actuator/health`, kiểm mỗi 5 phút, báo về email của bạn.
- Xem log: `docker compose -f docker-compose.prod.yml logs -f backend` (log tự xoay vòng, mỗi service
  tối đa 3 × 10 MB).
- Healthcheck **không** phụ thuộc SMTP: Brevo lỗi thì email chờ thử lại, app vẫn được coi là UP.

## 5. Sao lưu và khôi phục (T138)

- Mỗi đêm lúc **02:00 giờ Việt Nam**, hệ thống chạy `pg_dump` và đẩy lên R2. R2 giữ 7 bản mới nhất,
  VPS giữ 2 bản. Nếu thiếu cấu hình R2, job sẽ báo lỗi. RPO ≤ 24 h.
- Sao lưu ngay: `docker compose -f docker-compose.prod.yml exec backup backup.sh`
- Khôi phục đè dữ liệu thật (RTO ≤ 4 h, thực tế chỉ vài phút):
  ```bash
  docker compose -f docker-compose.prod.yml stop backend
  docker compose -f docker-compose.prod.yml exec backup restore.sh latest --db english --yes
  docker compose -f docker-compose.prod.yml start backend
  ```
  Thay `latest` bằng tên file (ví dụ `english_20261105_020000.dump`) để chọn một bản cụ thể.

## 6. Diễn tập khôi phục (T144)

`./drill.sh` làm lần lượt: sao lưu, tải lại bản mới nhất từ R2, khôi phục vào CSDL tạm, so số dòng các
bảng chính với CSDL thật, rồi xóa CSDL tạm. Script **không đụng** dữ liệu đang chạy.

| Ngày | Môi trường | Kết quả |
|---|---|---|
| 2026-10-21 | Docker local, S3 giả lập thay R2, dữ liệu `demo-seed` | **ĐẠT**: 13 giây, 6/6 bảng khớp số dòng |
| _(sau khi deploy)_ | VPS + R2 thật | _chạy `./drill.sh` trước 11/11 rồi ghi kết quả vào đây_ |

## 7. Khi lên startup

Không phải sửa code. Các bước:
1. Nâng cấu hình VPS.
2. Tự host LiveKit khi vượt hạn mức free (khoảng 40 buổi/tháng): đổi `LIVEKIT_*`.
3. Dùng domain email riêng có SPF/DKIM.
4. Chuyển bucket backup về storage trong nước (S3-compatible): đổi `R2_ENDPOINT`.
5. Hỏi luật sư về hồ sơ chuyển dữ liệu ra nước ngoài cho các dịch vụ vẫn đặt ở nước ngoài.
