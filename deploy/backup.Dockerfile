# pg_dump 16 + aws CLI (R2 dùng API S3) + múi giờ VN cho cron; script nằm trong image nên không phụ
# thuộc quyền thực thi của file trên server
FROM postgres:16-alpine
RUN apk add --no-cache aws-cli tzdata
ENV TZ=Asia/Ho_Chi_Minh
COPY backup.sh restore.sh /usr/local/bin/
RUN chmod 755 /usr/local/bin/backup.sh /usr/local/bin/restore.sh
# 02:00 giờ Việt Nam hằng ngày (R17); log ra stdout để xem bằng `docker compose logs backup`
RUN echo "0 2 * * * /usr/local/bin/backup.sh > /proc/1/fd/1 2>&1" > /etc/crontabs/root
CMD ["crond", "-f", "-l", "8"]
