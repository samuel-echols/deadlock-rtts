#!/usr/bin/env bash
# Daily Postgres backup to S3.
# Run via cron on the EC2 instance:
#   0 3 * * * /home/ec2-user/app/scripts/backup.sh >> /var/log/deadlock-backup.log 2>&1

set -euo pipefail

: "${DB_HOST:?DB_HOST not set}"
: "${DB_NAME:?DB_NAME not set}"
: "${DB_USER:?DB_USER not set}"
: "${PGPASSWORD:?PGPASSWORD not set}"
: "${S3_BUCKET:?S3_BUCKET not set}"

TIMESTAMP=$(date -u +"%Y%m%dT%H%M%SZ")
FILENAME="deadlock-backup-${TIMESTAMP}.sql.gz"
TMPFILE="/tmp/${FILENAME}"

echo "[$(date -u)] Starting backup to s3://${S3_BUCKET}/${FILENAME}"

pg_dump \
  --host="${DB_HOST}" \
  --port=5432 \
  --username="${DB_USER}" \
  --dbname="${DB_NAME}" \
  --no-password \
  --format=plain \
  | gzip > "${TMPFILE}"

aws s3 cp "${TMPFILE}" "s3://${S3_BUCKET}/${FILENAME}"
rm -f "${TMPFILE}"

# Retain last 30 backups, delete older ones
aws s3 ls "s3://${S3_BUCKET}/" \
  | sort \
  | head -n -30 \
  | awk '{print $4}' \
  | xargs -I{} aws s3 rm "s3://${S3_BUCKET}/{}" || true

echo "[$(date -u)] Backup complete: ${FILENAME}"
