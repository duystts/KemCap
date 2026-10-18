# Triển khai demo (bản tối thiểu — U1)

Mục tiêu U1: đưa walking skeleton lên **Oracle Cloud Always Free VM** sớm để phát hiện rủi ro hosting.
Phần đầy đủ (log rotation, healthcheck ngoài, sao lưu `pg_dump` → R2, diễn tập khôi phục) làm ở U5.

## 1. Chuẩn bị VM (làm tay, một lần)

1. Tạo VM Ampere A1 (ARM, Ubuntu 22.04+) trên Oracle Cloud Always Free. ⚠ kiểm tra chính sách thu hồi
   VM nhàn rỗi.
2. Mở cổng 80 và 443 ở Security List/NSG **và** firewall của VM (`sudo iptables` / `ufw`).
3. Cài Docker Engine + Compose plugin.
4. Trỏ DNS `api.<domain>` về IP public của VM.

## 2. Cấu hình

```bash
git clone <repo> english && cd english/deploy
cp .env.example .env    # điền DB/POSTGRES_PASSWORD, JWT_SECRET (>= 32 ký tự), FRONTEND_URL, SMTP, LIVEKIT, DOMAIN
openssl rand -base64 48 # dùng làm JWT_SECRET
```

Không commit `deploy/.env`.

## 3. Chạy

```bash
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml logs -f backend   # chờ "Started EnglishPlatformApplication"
curl https://api.<domain>/actuator/health                    # {"status":"UP"}
```

Muốn có tài khoản demo: thêm `SPRING_PROFILES_ACTIVE=demo-seed` vào `.env` cho lần chạy đầu.

## 4. Frontend (Cloudflare Pages)

- Build command: `npm ci && npm run build`, root `frontend/`, output `dist`.
- Biến môi trường: `VITE_API_URL=https://api.<domain>/api/v1`.
- `FRONTEND_URL` của backend phải đúng domain Pages (CORS + link trong email).
