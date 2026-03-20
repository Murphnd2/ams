#!/bin/bash
# =============================================================================
# SSA Database Backup Script
# Reads config from /var/lib/tomcat10/conf/ssa.properties
# Dumps beta_ssa, compresses, uploads to Wasabi, cleans up local copies
# Location: /opt/ssa/scripts/backup.sh
# Cron: 0 2 * * * /opt/ssa/scripts/backup.sh
# =============================================================================
set -euo pipefail
PROPS="/var/lib/tomcat10/conf/ssa.properties"
LOG="/opt/ssa/logs/backup.log"
BACKUP_DIR="/opt/ssa/backups"
RETENTION_DAYS=7
# ── Helper ───────────────────────────────────────────────────────────────────
log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG"; }
read_prop() { grep "^$1=" "$PROPS" | cut -d'=' -f2- | xargs; }
# ── Read config ──────────────────────────────────────────────────────────────
PSP_ID=$(read_prop PSP_ID)
DB_HOST=$(read_prop DB_HOST)
DB_PORT=$(read_prop DB_PORT)
DB_NAME=$(read_prop DB_NAME)
DB_USER=$(read_prop DB_USER)
DB_PASSWORD=$(read_prop DB_PASSWORD)
WASABI_BUCKET=$(read_prop WASABI_BUCKET)
WASABI_ENDPOINT=$(read_prop WASABI_ENDPOINT)
# ── Safety check ─────────────────────────────────────────────────────────────
if [ "$PSP_ID" = "UNINITIALIZED" ] || [ -z "$PSP_ID" ]; then
    log "SKIP: PSP_ID is UNINITIALIZED — backup not run"
    exit 0
fi
log "START: Backup for $PSP_ID"
# ── Dump database ────────────────────────────────────────────────────────────
TIMESTAMP=$(date '+%Y-%m-%d_%H%M%S')
DUMP_FILE="${BACKUP_DIR}/${PSP_ID}_${DB_NAME}_${TIMESTAMP}.sql"
GZ_FILE="${DUMP_FILE}.gz"
LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu mysqldump \
    --socket=/var/run/mysqld/mysqld.sock \
    -u "$DB_USER" -p"$DB_PASSWORD" \
    --single-transaction --routines --triggers --quick --no-tablespaces \
    "$DB_NAME" > "$DUMP_FILE" 2>> "$LOG"
log "DUMP: $(du -h "$DUMP_FILE" | cut -f1) uncompressed"
gzip "$DUMP_FILE"
log "GZIP: $(du -h "$GZ_FILE" | cut -f1) compressed"
# ── Upload to Wasabi ─────────────────────────────────────────────────────────
S3_KEY="${PSP_ID}/db/$(basename "$GZ_FILE")"
aws s3 cp "$GZ_FILE" "s3://${WASABI_BUCKET}/${S3_KEY}" \
    --endpoint-url "$WASABI_ENDPOINT" \
    --profile wasabi \
    >> "$LOG" 2>&1
log "UPLOAD: s3://${WASABI_BUCKET}/${S3_KEY}"
# ── Clean up local files older than retention ────────────────────────────────
find "$BACKUP_DIR" -name "${PSP_ID}_*.sql.gz" -mtime +${RETENTION_DAYS} -delete
log "CLEANUP: Removed local backups older than ${RETENTION_DAYS} days"
log "DONE: Backup for $PSP_ID completed successfully"
