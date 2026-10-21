#!/usr/bin/env bash
# Deploy một lệnh lên VPS Ubuntu (T137). Chạy lại được bất cứ lúc nào để cập nhật bản mới:
#   cd english/deploy && ./deploy.sh
# Lần đầu: script tạo deploy/.env rồi dừng để bạn điền các dòng (*); chạy lại là xong.
set -euo pipefail
cd "$(dirname "$0")"

COMPOSE=(docker compose -f docker-compose.prod.yml)
ok() { echo -e "  \e[32m✔\e[0m $*"; }
warn() { echo -e "  \e[33m!\e[0m $*"; }
fail() { echo -e "  \e[31m✘ $*\e[0m"; exit 1; }
step() { echo -e "\n\e[1m== $* ==\e[0m"; }
sudo_if() { if [ "$(id -u)" -eq 0 ]; then "$@"; else sudo "$@"; fi; }

step "1. Kiểm tra VPS"
[ "$(uname -s)" = "Linux" ] || fail "Cần Linux (Ubuntu 22.04/24.04)"
virt=$(systemd-detect-virt 2>/dev/null || echo unknown)
case "$virt" in
  openvz | lxc | lxc-libvirt) fail "VPS dùng ảo hóa $virt — Docker cần KVM. Hãy đổi gói KVM." ;;
  *) ok "Ảo hóa: $virt" ;;
esac
mem_mb=$(awk '/MemTotal/ {print int($2/1024)}' /proc/meminfo)
if [ "$mem_mb" -lt 1800 ]; then
  fail "RAM ${mem_mb} MB — cần tối thiểu 2 GB (khuyến nghị 4 GB)"
elif [ "$mem_mb" -lt 3500 ]; then
  warn "RAM ${mem_mb} MB — chạy được nhưng build image sẽ chậm; khuyến nghị 4 GB"
else
  ok "RAM ${mem_mb} MB"
fi
disk_gb=$(df -BG --output=avail / | tail -1 | tr -dc '0-9')
[ "$disk_gb" -ge 10 ] || fail "Ổ đĩa còn ${disk_gb} GB — cần ≥ 10 GB trống (khuyến nghị gói 40 GB)"
ok "Ổ đĩa còn ${disk_gb} GB"

step "2. Docker"
if ! command -v docker >/dev/null 2>&1; then
  warn "Chưa có Docker — đang cài (get.docker.com)"
  curl -fsSL https://get.docker.com | sudo_if sh
fi
if ! docker info >/dev/null 2>&1; then
  if sudo_if docker info >/dev/null 2>&1; then
    COMPOSE=(sudo docker compose -f docker-compose.prod.yml)
  else
    fail "Docker chưa chạy (sudo systemctl start docker)"
  fi
fi
ok "$(docker --version 2>/dev/null || sudo docker --version)"

step "3. Cấu hình deploy/.env"
if [ ! -f .env ]; then
  cp .env.example .env
  chmod 600 .env
  warn "Đã tạo deploy/.env — điền các dòng đánh dấu (*) rồi chạy lại ./deploy.sh"
  exit 0
fi
get() { grep -E "^$1=" .env | tail -1 | cut -d= -f2- || true; }
set_env() {
  if grep -qE "^$1=" .env; then sed -i "s|^$1=.*|$1=$2|" .env; else echo "$1=$2" >> .env; fi
}
[ -n "$(get POSTGRES_PASSWORD)" ] || { set_env POSTGRES_PASSWORD "$(openssl rand -hex 24)"; ok "Đã sinh POSTGRES_PASSWORD"; }
[ -n "$(get JWT_SECRET)" ] || { set_env JWT_SECRET "$(openssl rand -hex 32)"; ok "Đã sinh JWT_SECRET"; }
if [ -z "$(get DOMAIN)" ]; then
  ip=$(curl -4fsS https://api.ipify.org || true)
  [ -n "$ip" ] || fail "Không lấy được IP public — điền DOMAIN trong .env"
  set_env DOMAIN "${ip//./-}.sslip.io"
  ok "DOMAIN = $(get DOMAIN) (sslip.io)"
fi
missing=()
for v in SMTP_USER SMTP_PASSWORD MAIL_FROM LIVEKIT_URL LIVEKIT_API_KEY LIVEKIT_API_SECRET \
  R2_ENDPOINT R2_BUCKET R2_ACCESS_KEY_ID R2_SECRET_ACCESS_KEY; do
  [ -n "$(get "$v")" ] || missing+=("$v")
done
[ ${#missing[@]} -eq 0 ] || fail "Còn thiếu trong deploy/.env: ${missing[*]}"
ok "Đủ cấu hình"
DOMAIN=$(get DOMAIN)

step "4. Tường lửa"
if command -v ufw >/dev/null 2>&1 && sudo_if ufw status | grep -q "Status: active"; then
  sudo_if ufw allow 80/tcp >/dev/null && sudo_if ufw allow 443/tcp >/dev/null
  ok "ufw: đã mở 80, 443"
else
  ok "ufw không bật — nhớ mở cổng 80, 443 ở trang quản lý VPS nếu nhà cung cấp có firewall riêng"
fi

step "5. Build và chạy (lần đầu mất 5–10 phút)"
"${COMPOSE[@]}" up -d --build --remove-orphans

step "6. Chờ hệ thống sẵn sàng"
for i in $(seq 1 60); do
  if curl -fsS "https://$DOMAIN/actuator/health" 2>/dev/null | grep -q '"UP"'; then
    ok "https://$DOMAIN đang chạy (HTTPS hợp lệ)"
    break
  fi
  [ "$i" -eq 60 ] && fail "Chưa lên sau 10 phút — xem: ${COMPOSE[*]} logs backend web"
  sleep 10
done

step "7. Sao lưu đầu tiên lên R2"
"${COMPOSE[@]}" exec -T backup backup.sh || fail "Sao lưu lỗi — kiểm tra R2_* trong .env"

step "Xong"
echo "  Ứng dụng:      https://$DOMAIN"
echo "  Health check:  https://$DOMAIN/actuator/health  (dán vào UptimeRobot)"
echo "  Xem log:       ${COMPOSE[*]} logs -f backend"
if grep -qE "^SPRING_PROFILES_ACTIVE=.*demo-seed" .env; then
  warn "Đang bật demo-seed: tài khoản mẫu dùng mật khẩu Demo@1234 — đổi mật khẩu trước khi phát link (R18)"
fi
