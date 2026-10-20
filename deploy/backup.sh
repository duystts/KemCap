#!/bin/sh
set -e

BACKUP_DIR="/backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
FILENAME="english_${TIMESTAMP}.sql.gz"
FILEPATH="${BACKUP_DIR}/${FILENAME}"

mkdir -p "${BACKUP_DIR}"

echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] Bắt đầu sao lưu CSDL PostgreSQL..."

PGPASSWORD="${POSTGRES_PASSWORD}" pg_dump -h "${PGHOST:-postgres}" -U "${DB_USER:-app}" -d "${POSTGRES_DB:-english}" | gzip > "${FILEPATH}"

echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] Đã tạo bản sao lưu: ${FILEPATH} ($(du -h "${FILEPATH}" | cut -f1))"

# Xóa các bản sao lưu cục bộ cũ hơn 7 ngày
find "${BACKUP_DIR}" -name "english_*.sql.gz" -mtime +7 -exec rm {} \;
echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] Đã dọn dẹp các bản sao lưu cũ hơn 7 ngày."

# Nếu có cấu hình Cloudflare R2 / S3
if [ -n "${R2_BUCKET}" ] && [ -n "${R2_ENDPOINT}" ] && [ -n "${AWS_ACCESS_KEY_ID}" ] && [ -n "${AWS_SECRET_ACCESS_KEY}" ]; then
  echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] Đang tải bản sao lưu lên Cloudflare R2 (${R2_BUCKET})..."
  aws --endpoint-url="${R2_ENDPOINT}" s3 cp "${FILEPATH}" "s3://${R2_BUCKET}/backups/${FILENAME}"
  echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] Tải lên R2 hoàn tất."
fi

echo "[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] Hoàn thành sao lưu."
