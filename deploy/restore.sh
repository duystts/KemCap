#!/bin/sh
# Khôi phục bản sao lưu (T138, RTO ≤ 4 h). Mặc định ghi vào CSDL đích riêng để diễn tập không đụng dữ
# liệu thật; khôi phục đè CSDL chính phải dừng backend trước và thêm --yes.
#
#   restore.sh latest --db english_restore_test        # diễn tập (T144), an toàn
#   restore.sh english_20261105_020000.dump --db english --yes
set -eu
set -o pipefail

log() { echo "[restore $(date '+%Y-%m-%d %H:%M:%S %Z')] $*"; }
usage() { echo "Dùng: restore.sh <latest|tên-file.dump> --db <tên-csdl> [--yes]"; exit 1; }

SRC="${1:-}"; [ -n "$SRC" ] || usage; shift
DB=""; YES=0
while [ $# -gt 0 ]; do
  case "$1" in
    --db) DB="${2:-}"; shift 2 ;;
    --yes) YES=1; shift ;;
    *) usage ;;
  esac
done
[ -n "$DB" ] || usage
if [ "$DB" = "${PGDATABASE:-english}" ] && [ "$YES" -ne 1 ]; then
  log "Khôi phục đè CSDL chính '$DB' sẽ XÓA dữ liệu hiện tại. Dừng backend rồi chạy lại với --yes."
  exit 1
fi

export AWS_ACCESS_KEY_ID="${R2_ACCESS_KEY_ID:-}" AWS_SECRET_ACCESS_KEY="${R2_SECRET_ACCESS_KEY:-}"
export AWS_DEFAULT_REGION=auto
s3() { aws --endpoint-url "$R2_ENDPOINT" s3 "$@"; }

START=$(date +%s)
if [ "$SRC" = "latest" ]; then
  SRC=$(s3 ls "s3://$R2_BUCKET/backups/" | awk '{print $4}' | grep '^english_.*\.dump$' | sort -r | head -n 1)
  [ -n "$SRC" ] || { log "LỖI: không có bản sao lưu nào trên R2"; exit 1; }
fi
FILE="/backups/$SRC"
if [ ! -f "$FILE" ]; then
  log "Tải $SRC từ R2"
  s3 cp --only-show-errors "s3://$R2_BUCKET/backups/$SRC" "$FILE"
fi

if ! psql -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$DB'" | grep -q 1; then
  log "Tạo CSDL $DB"
  createdb "$DB"
fi

log "pg_restore $SRC → $DB"
pg_restore --clean --if-exists --no-owner --exit-on-error --single-transaction -d "$DB" "$FILE"

ROWS=$(psql -d "$DB" -tAc "SELECT count(*) FROM user_account")
log "Thành công trong $(( $(date +%s) - START )) giây — user_account: $ROWS dòng"
