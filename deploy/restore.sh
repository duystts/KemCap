#!/bin/sh
set -e

if [ -z "$1" ]; then
  echo "Sử dụng: $0 <duong_dan_file_backup.sql.gz>"
  echo "Ví dụ: $0 /backups/english_20261020_120000.sql.gz"
  exit 1
fi

FILEPATH="$1"

if [ ! -f "${FILEPATH}" ]; then
  echo "Lỗi: Không tìm thấy file '${FILEPATH}'"
  exit 1
fi

echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] Bắt đầu khôi phục CSDL từ ${FILEPATH}..."
echo "Cảnh báo: Dữ liệu hiện tại trong CSDL sẽ bị ghi đè."

PGPASSWORD="${POSTGRES_PASSWORD}" gunzip -c "${FILEPATH}" | psql -h "${PGHOST:-postgres}" -U "${DB_USER:-app}" -d "${POSTGRES_DB:-english}"

echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] Khôi phục CSDL thành công!"
