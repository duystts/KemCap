#!/bin/sh
# Sao lưu CSDL lên Cloudflare R2 (T138, R17): RPO ≤ 24 h. R2 bắt buộc — thiếu cấu hình thì báo lỗi.
# Chạy tay: docker compose -f docker-compose.prod.yml exec backup backup.sh
set -eu
set -o pipefail

log() { echo "[backup $(date '+%Y-%m-%d %H:%M:%S %Z')] $*"; }

for v in R2_ENDPOINT R2_BUCKET R2_ACCESS_KEY_ID R2_SECRET_ACCESS_KEY; do
  eval "val=\${$v:-}"
  if [ -z "$val" ]; then
    log "LỖI: thiếu $v trong deploy/.env — bản sao lưu phải nằm ngoài VPS (R17)"
    exit 1
  fi
done
export AWS_ACCESS_KEY_ID="$R2_ACCESS_KEY_ID" AWS_SECRET_ACCESS_KEY="$R2_SECRET_ACCESS_KEY"
export AWS_DEFAULT_REGION=auto
s3() { aws --endpoint-url "$R2_ENDPOINT" s3 "$@"; }

KEEP="${BACKUP_KEEP:-7}"
NAME="english_$(date +%Y%m%d_%H%M%S).dump"
FILE="/backups/$NAME"
mkdir -p /backups

log "pg_dump → $FILE"
pg_dump --format=custom --no-owner --file "$FILE"
log "Đã tạo $(du -h "$FILE" | cut -f1)"

s3 cp --only-show-errors "$FILE" "s3://$R2_BUCKET/backups/$NAME"
log "Đã tải lên s3://$R2_BUCKET/backups/$NAME"

# Giữ $KEEP bản mới nhất trên R2 (tên có timestamp → sắp theo tên là theo thời gian)
s3 ls "s3://$R2_BUCKET/backups/" | awk '{print $4}' | grep '^english_.*\.dump$' | sort -r \
  | tail -n +"$((KEEP + 1))" | while read -r old; do
    s3 rm --only-show-errors "s3://$R2_BUCKET/backups/$old"
    log "Đã xóa bản cũ trên R2: $old"
  done

# Trên VPS chỉ giữ 2 bản gần nhất (khôi phục nhanh, không đầy ổ đĩa)
ls -1 /backups/english_*.dump | sort -r | tail -n +3 | xargs -r rm -f
log "Hoàn tất"
