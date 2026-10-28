#!/usr/bin/env bash
# Daily Postgres backup to S3.
# Password comes from ~/.pgpass; retention is an S3 lifecycle rule, not this script.
# Run via cron on the EC2 instance (after the 04:00 UTC ingestion):
#   0 5 * * * set -a; . $HOME/backup.env; $HOME/app/scripts/backup.sh >> $HOME/backup.log 2>&1

set -euo pipefail

: "${DB_HOST:?DB_HOST not set}"
: "${DB_NAME:?DB_NAME not set}"
: "${DB_USER:?DB_USER not set}"
: "${S3_BUCKET:?S3_BUCKET not set}"

TIMESTAMP=$(date -u +"%Y%m%dT%H%M%SZ")
FILENAME="deadlock-backup-${TIMESTAMP}.sql.gz"
TMPFILE=$(mktemp "/tmp/${FILENAME}.XXXX")
trap 'rm -f "${TMPFILE}"' EXIT

echo "[$(date -u)] Starting backup to s3://${S3_BUCKET}/${FILENAME}"

pg_dump \
  --host="${DB_HOST}" \
  --port=5432 \
  --username="${DB_USER}" \
  --dbname="${DB_NAME}" \
  --no-password \
  --format=plain \
  | gzip > "${TMPFILE}"

# An empty database still dumps several KB of DDL; anything smaller means pg_dump failed.
SIZE=$(stat -c %s "${TMPFILE}")
if [ "${SIZE}" -lt 1024 ]; then
  echo "[$(date -u)] Backup FAILED: dump is only ${SIZE} bytes" >&2
  exit 1
fi

aws s3 cp "${TMPFILE}" "s3://${S3_BUCKET}/${FILENAME}" --only-show-errors

echo "[$(date -u)] Backup complete: ${FILENAME} (${SIZE} bytes)"
