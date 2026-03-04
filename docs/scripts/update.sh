#!/bin/bash
# =============================================================================
# SSA Update Script
# Checks GitHub Releases for new versions, applies SQL migrations, deploys WAR
# Location: /opt/ssa/scripts/update.sh
# Cron: 30 2 * * * /opt/ssa/scripts/update.sh
# =============================================================================

set -euo pipefail

PROPS="/var/lib/tomcat10/conf/ssa.properties"
LOG="/opt/ssa/logs/update.log"
VERSION_FILE="/opt/ssa/current_version.txt"
WEBAPPS="/var/lib/tomcat10/webapps"
BACKUP_DIR="/opt/ssa/backups"
TMP_DIR="/tmp/ssa-update"

# ── Helper ───────────────────────────────────────────────────────────────────
log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG"; }

read_prop() { grep "^$1=" "$PROPS" | cut -d'=' -f2- | xargs; }

# ── Read config ──────────────────────────────────────────────────────────────
PSP_ID=$(read_prop PSP_ID)
DB_NAME=$(read_prop DB_NAME)
DB_USER=$(read_prop DB_USER)
DB_PASSWORD=$(read_prop DB_PASSWORD)
RELEASE_REPO=$(read_prop RELEASE_REPO)
RELEASE_TOKEN=$(read_prop RELEASE_TOKEN)

# ── Safety check ─────────────────────────────────────────────────────────────
if [ "$PSP_ID" = "UNINITIALIZED" ] || [ -z "$PSP_ID" ]; then
    log "SKIP: PSP_ID is UNINITIALIZED — update not run"
    exit 0
fi

log "START: Update check for $PSP_ID"

# ── Get current version ─────────────────────────────────────────────────────
CURRENT_VERSION="none"
if [ -f "$VERSION_FILE" ]; then
    CURRENT_VERSION=$(cat "$VERSION_FILE" | xargs)
fi

# ── Check latest release ────────────────────────────────────────────────────
RELEASE_JSON=$(curl -sf -H "Authorization: token $RELEASE_TOKEN" "$RELEASE_REPO/latest") || {
    log "ERROR: Failed to fetch latest release from GitHub"
    exit 1
}

LATEST_TAG=$(echo "$RELEASE_JSON" | grep '"tag_name"' | head -1 | sed 's/.*: "//;s/",//')

if [ "$LATEST_TAG" = "$CURRENT_VERSION" ]; then
    log "CURRENT: Already on $CURRENT_VERSION — no update needed"
    exit 0
fi

log "UPDATE AVAILABLE: $CURRENT_VERSION → $LATEST_TAG"

# ── Prepare temp directory ───────────────────────────────────────────────────
rm -rf "$TMP_DIR"
mkdir -p "$TMP_DIR"

# ── Download and apply SQL migrations ────────────────────────────────────────
SQL_URLS=$(echo "$RELEASE_JSON" | grep -o '"browser_download_url": "[^"]*\.sql"' | sed 's/"browser_download_url": "//;s/"$//' | sort || true)

if [ -n "$SQL_URLS" ]; then
    log "MIGRATIONS: Found SQL files to process"
    for URL in $SQL_URLS; do
        FILENAME=$(basename "$URL")
        # Extract version number (e.g., V010 from V010__description.sql)
        MIGRATION_VER=$(echo "$FILENAME" | grep -oP '^V\d+' || echo "")

        if [ -n "$MIGRATION_VER" ]; then
            # Check if already applied
            APPLIED=$(mysql -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -N -e \
                "SELECT COUNT(*) FROM schema_version WHERE version='$MIGRATION_VER';" 2>/dev/null || echo "0")

            if [ "$APPLIED" -gt 0 ]; then
                log "MIGRATION SKIP: $FILENAME already applied"
                continue
            fi
        fi

        # Download and apply
        curl -sfL -H "Authorization: token $RELEASE_TOKEN" -H "Accept: application/octet-stream" \
            -o "$TMP_DIR/$FILENAME" "$URL" || {
            log "ERROR: Failed to download migration $FILENAME — ABORTING"
            exit 1
        }

        mysql -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < "$TMP_DIR/$FILENAME" 2>> "$LOG" || {
            log "ERROR: Migration $FILENAME FAILED — ABORTING (WAR not deployed)"
            exit 1
        }

        # Record in schema_version (IGNORE since migration scripts self-register)
        if [ -n "$MIGRATION_VER" ]; then
            DESCRIPTION=$(echo "$FILENAME" | sed 's/^V[0-9]*__//;s/\.sql$//' | tr '_' ' ')
            mysql -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -e \
                "INSERT IGNORE INTO schema_version (version, description, script_name) VALUES ('$MIGRATION_VER', '$DESCRIPTION', '$FILENAME');" 2>> "$LOG"
        fi

        log "MIGRATION APPLIED: $FILENAME"
    done
else
    log "MIGRATIONS: No SQL files in this release"
fi

# ── Download and deploy WAR ──────────────────────────────────────────────────
WAR_URL=$(echo "$RELEASE_JSON" | grep -o '"browser_download_url": "[^"]*\.war"' | head -1 | sed 's/"browser_download_url": "//;s/"$//' || true)

if [ -z "$WAR_URL" ]; then
    log "ERROR: No WAR file found in release $LATEST_TAG"
    exit 1
fi

curl -sfL -H "Authorization: token $RELEASE_TOKEN" -H "Accept: application/octet-stream" \
    -o "$TMP_DIR/ROOT.war" "$WAR_URL" || {
    log "ERROR: Failed to download WAR — ABORTING"
    exit 1
}

WAR_SIZE=$(du -h "$TMP_DIR/ROOT.war" | cut -f1)
log "DOWNLOADED: WAR ($WAR_SIZE)"

# ── Stop Tomcat, swap WAR, start Tomcat ──────────────────────────────────────
systemctl stop tomcat10
log "TOMCAT: Stopped"

# Back up current WAR
if [ -f "$WEBAPPS/ROOT.war" ]; then
    cp "$WEBAPPS/ROOT.war" "$BACKUP_DIR/ROOT-previous.war"
    log "BACKUP: Previous WAR saved"
fi

# Remove old deployment
rm -rf "$WEBAPPS/ROOT" "$WEBAPPS/ROOT.war"

# Deploy new WAR
cp "$TMP_DIR/ROOT.war" "$WEBAPPS/ROOT.war"
chown tomcat:tomcat "$WEBAPPS/ROOT.war"

systemctl start tomcat10
log "TOMCAT: Started with new WAR"

# ── Record new version ───────────────────────────────────────────────────────
echo "$LATEST_TAG" > "$VERSION_FILE"
log "DONE: Updated $PSP_ID from $CURRENT_VERSION to $LATEST_TAG"

# ── Cleanup ──────────────────────────────────────────────────────────────────
rm -rf "$TMP_DIR"
