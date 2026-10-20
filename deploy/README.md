# Hướng dẫn Triển khai & Vận hành Demo (U5 Hoàn Thiện)

Nền tảng chạy trên **Oracle Cloud Always Free VM** (ARM64 Ampere A1) kết hợp **Cloudflare Pages** cho Frontend và **Cloudflare R2** cho sao lưu CSDL.

---

## 1. Chuẩn bị Hạ Tầng Máy Chủ (Oracle Cloud)

1. **Khởi tạo VM**: Tạo instance Ampere A1 (4 OCPU, 24GB RAM hoặc tối thiểu 1 OCPU/6GB RAM, Ubuntu 22.04 LTS).
2. **Mở cổng Firewall**:
   * Mở Ingress Rules trong OCI Security List: Cổng `80` (HTTP) và `443` (HTTPS).
   * Mở tường lửa nội bộ trên Ubuntu:
     ```bash
     sudo ufw allow 80/tcp
     sudo ufw allow 443/tcp
     sudo ufw reload
     ```
3. **Cài đặt Docker Engine & Docker Compose**:
   ```bash
   curl -fsSL https://get.docker.com -o get-docker.sh && sudo sh get-docker.sh
   sudo usermod -aG docker $USER
   ```
4. **Trỏ DNS**: Trỏ tên miền `api.<your-domain>.com` về IP Public của VM.

---

## 2. Cấu hình Môi trường

```bash
git clone <repo_url> english && cd english/deploy
cp .env.example .env
```

Điền các thông số bắt buộc vào `.env`:
* `DOMAIN=api.<your-domain>.com` (Caddy tự động đăng ký SSL Let's Encrypt).
* `POSTGRES_PASSWORD`: Mật khẩu bảo mật cho PostgreSQL.
* `JWT_SECRET`: Khóa bí mật tối thiểu 32 ký tự (sinh bằng `openssl rand -base64 48`).
* `FRONTEND_URL`: URL trang web Frontend trên Cloudflare Pages (dùng cho CORS và link trong email).
* `SMTP_*`: Cấu hình máy chủ gửi email (Brevo hoặc Gmail SMTP port 587).
* `LIVEKIT_*`: API Key, Secret và WebSocket URL từ LiveKit Cloud Free.
* *(Tùy chọn)* `R2_*` / `AWS_*`: Cấu hình Cloudflare R2 bucket để đẩy các bản sao lưu database lên Cloud.

---

## 3. Khởi Chạy Hệ Thống

```bash
# Build và chạy ngầm toàn bộ dịch vụ (postgres, backend, caddy, backup)
docker compose -f docker-compose.prod.yml up -d --build

# Theo dõi log backend
docker compose -f docker-compose.prod.yml logs -f backend

# Kiểm tra sức khỏe hệ thống (Healthcheck)
curl https://api.<your-domain>.com/actuator/health
# Kết quả mong đợi: {"status":"UP"}
```

Để nạp dữ liệu mẫu cho lần chạy đầu tiên: Thêm `SPRING_PROFILES_ACTIVE=demo-seed` vào file `.env`.

---

## 4. Vận hành Sao Lưu & Khôi Phục CSDL (T138 / R17)

### Sao lưu tự động
Dịch vụ `backup` trong docker-compose chạy ngầm định kỳ lúc **02:00 UTC hằng ngày**:
* Xuất file nén `english_YYYYMMDD_HHMMSS.sql.gz` vào thư mục `./backups/`.
* Tự động xóa các bản sao lưu cũ hơn 7 ngày.
* Tự động đồng bộ lên bucket Cloudflare R2 nếu có biến môi trường `R2_BUCKET`.

### Sao lưu thủ công ngay lập tức
```bash
docker compose -f docker-compose.prod.yml exec backup /backup.sh
```

### Khôi phục CSDL từ bản sao lưu
```bash
# Ví dụ khôi phục từ bản sao lưu ngày 2026-10-20
docker compose -f docker-compose.prod.yml exec -T backup /restore.sh /backups/english_20261020_020000.sql.gz
```

---

## 5. Triển khai Frontend (Cloudflare Pages)

1. Kết nối kho lưu trữ GitHub với **Cloudflare Pages**.
2. **Build Settings**:
   * Framework preset: `Vite`
   * Build command: `npm ci && npm run build`
   * Build output directory: `dist`
   * Root directory: `frontend/`
3. **Environment Variables**:
   * `VITE_API_URL`: `https://api.<your-domain>.com/api/v1`
