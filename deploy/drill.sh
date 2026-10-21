#!/usr/bin/env bash
# Diễn tập khôi phục (T144, NFR-REL-10): sao lưu → tải lại từ R2 → khôi phục vào CSDL tạm → so số dòng
# với CSDL thật → xóa CSDL tạm. Không đụng dữ liệu đang chạy, chạy được trên VPS bất cứ lúc nào.
#   cd english/deploy && ./drill.sh
set -euo pipefail
cd "$(dirname "$0")"

COMPOSE=(docker compose -f docker-compose.prod.yml ${DRILL_COMPOSE_EXTRA:-})
docker info >/dev/null 2>&1 || COMPOSE=(sudo "${COMPOSE[@]}")
TEST_DB=english_restore_drill
TABLES="user_account student_profile enrollment session payment notification"

# CSDL tạm chứa bản sao dữ liệu cá nhân → luôn xóa, kể cả khi diễn tập lỗi giữa chừng (review R11)
cleanup() { "${COMPOSE[@]}" exec -T backup dropdb --if-exists "$TEST_DB" >/dev/null 2>&1 || true; }
trap cleanup EXIT

start=$(date +%s)
echo "== 1. Sao lưu =="
"${COMPOSE[@]}" exec -T backup backup.sh

echo "== 2. Xóa bản cục bộ để buộc tải lại từ R2 =="
"${COMPOSE[@]}" exec -T backup sh -c 'rm -f /backups/english_*.dump'

echo "== 3. Khôi phục bản mới nhất vào $TEST_DB =="
"${COMPOSE[@]}" exec -T backup restore.sh latest --db "$TEST_DB"

echo "== 4. So số dòng =="
ok=1
for t in $TABLES; do
  a=$("${COMPOSE[@]}" exec -T backup psql -tAc "SELECT count(*) FROM $t")
  b=$("${COMPOSE[@]}" exec -T backup psql -d "$TEST_DB" -tAc "SELECT count(*) FROM $t")
  if [ "$a" = "$b" ]; then echo "  ✔ $t: $a"; else echo "  ✘ $t: thật=$a, khôi phục=$b"; ok=0; fi
done

elapsed=$(($(date +%s) - start))
if [ "$ok" -eq 1 ]; then
  echo "KẾT QUẢ: ĐẠT — khôi phục trong ${elapsed} giây (RTO ≤ 4 h). Ghi vào deploy/README.md mục Diễn tập."
else
  echo "KẾT QUẢ: KHÔNG ĐẠT — số dòng lệch (có thể do dữ liệu thay đổi trong lúc diễn tập; chạy lại)"
  exit 1
fi
