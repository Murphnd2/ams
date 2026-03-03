#!/bin/bash
# =============================================================================
# SSA Health Check Script
# Sends a daily status email to the master admin
# Location: /opt/ssa/scripts/healthcheck.sh
# Cron: 0 6 * * * /opt/ssa/scripts/healthcheck.sh
#
# All config read from ssa.properties (SMTP via SYS_HEALTH_*, DB via DB_*).
# Disable email via: SYS_HEALTH_ENABLED=false in ssa.properties
# =============================================================================

set -uo pipefail

# --- Configuration ---
PROPS_FILE="/var/lib/tomcat10/conf/ssa.properties"
LOG_DIR="/opt/ssa/logs"
BACKUP_DIR="/opt/ssa/backups"
VERSION_FILE="/opt/ssa/current_version.txt"
UPDATE_LOG="/opt/ssa/logs/update.log"
HEALTH_LOG="${LOG_DIR}/healthcheck.log"
CATALINA_LOG="/var/lib/tomcat10/logs/catalina.out"

# --- Helper: read a value from ssa.properties ---
get_prop() {
    local val=""
    if [ -f "$PROPS_FILE" ]; then
        val=$(grep -E "^${1}=" "$PROPS_FILE" | head -1 | cut -d'=' -f2- | tr -d '[:space:]')
    fi
    echo "$val"
}

# --- Read DB config from ssa.properties ---
DB_NAME=$(get_prop "DB_NAME")
DB_USER=$(get_prop "DB_USER")
DB_PASSWORD=$(get_prop "DB_PASSWORD")
[ -z "$DB_NAME" ] && DB_NAME="beta_ssa"
[ -z "$DB_USER" ] && DB_USER="root"

# --- MySQL wrapper: Acronis library workaround + credentials from properties ---
run_mysql() {
    if [ -n "$DB_PASSWORD" ]; then
        LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu mysql --socket=/var/run/mysqld/mysqld.sock -u "$DB_USER" -p"$DB_PASSWORD" "$@" 2>/dev/null
    else
        LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu mysql --socket=/var/run/mysqld/mysqld.sock -u "$DB_USER" "$@" 2>/dev/null
    fi
}

# --- Read PSP_ID from ssa.properties ---
PSP_ID=$(get_prop "PSP_ID")

if [ "$PSP_ID" = "UNINITIALIZED" ] || [ -z "$PSP_ID" ]; then
    echo "$(date '+%Y-%m-%d %H:%M:%S') SKIP — PSP_ID is $PSP_ID" >> "$HEALTH_LOG"
    exit 0
fi

# --- Check if health email is enabled ---
HEALTH_ENABLED=$(get_prop "SYS_HEALTH_ENABLED")
if [ "$HEALTH_ENABLED" = "false" ]; then
    echo "$(date '+%Y-%m-%d %H:%M:%S') SKIP — Health email disabled for ${PSP_ID}" >> "$HEALTH_LOG"
    exit 0
fi

# --- Read SMTP config from ssa.properties ---
SMTP_TO=$(get_prop "SYS_HEALTH_EMAIL_TO")
SMTP_FROM=$(get_prop "SYS_HEALTH_EMAIL_FROM")
SMTP_SERVER=$(get_prop "SYS_HEALTH_SMTP_SERVER")
SMTP_PORT=$(get_prop "SYS_HEALTH_SMTP_PORT")
SMTP_USER=$(get_prop "SYS_HEALTH_SMTP_USER")
SMTP_PASSWORD=$(get_prop "SYS_HEALTH_SMTP_PASSWORD")

# Validate required SMTP config
if [ -z "$SMTP_TO" ] || [ -z "$SMTP_SERVER" ] || [ -z "$SMTP_USER" ] || [ -z "$SMTP_PASSWORD" ]; then
    echo "$(date '+%Y-%m-%d %H:%M:%S') FAIL — Missing SMTP config in ssa.properties for ${PSP_ID}" >> "$HEALTH_LOG"
    exit 1
fi

# Default FROM to SMTP_USER if not set
[ -z "$SMTP_FROM" ] && SMTP_FROM="$SMTP_USER"

# Default port
[ -z "$SMTP_PORT" ] && SMTP_PORT="2525"

# --- Collect Status ---
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S %Z')
HOSTNAME=$(hostname)

# Tomcat status
TOMCAT_STATUS="❌ STOPPED"
if systemctl is-active --quiet tomcat10 2>/dev/null; then
    TOMCAT_STATUS="✅ Running"
fi

# MySQL status
MYSQL_STATUS="❌ STOPPED"
if systemctl is-active --quiet mysql 2>/dev/null; then
    MYSQL_STATUS="✅ Running"
fi

# Disk usage
DISK_USAGE=$(df -h / | awk 'NR==2 {printf "%s used of %s (%s)", $3, $2, $5}')

# WAR version
WAR_VERSION="Unknown"
if [ -f "$VERSION_FILE" ]; then
    WAR_VERSION=$(cat "$VERSION_FILE")
fi

# DB migration version
DB_MIGRATION="Unknown"
if command -v mysql &>/dev/null; then
    DB_MIGRATION=$(run_mysql -N -e "SELECT COALESCE(MAX(version),'None') FROM ${DB_NAME}.schema_version;" || echo "Query failed")
fi

# DB initialized check — if constant table has any rows, initialization has run
DB_INITIALIZED="Unknown"
if command -v mysql &>/dev/null; then
    CONST_COUNT=$(run_mysql -N -e "SELECT COUNT(*) FROM ${DB_NAME}.constant;" || echo "0")
    if [ "$CONST_COUNT" -gt 0 ] 2>/dev/null; then
        DB_INITIALIZED="✅ Yes (${CONST_COUNT} constants)"
    else
        DB_INITIALIZED="⚠️ Not initialized"
    fi
fi

# Last backup
LAST_BACKUP="No backups found"
if [ -d "$BACKUP_DIR" ]; then
    LATEST_FILE=$(ls -t "$BACKUP_DIR"/*.sql.gz 2>/dev/null | head -1)
    if [ -n "$LATEST_FILE" ]; then
        LAST_BACKUP=$(stat -c '%y' "$LATEST_FILE" | cut -d'.' -f1)
        BACKUP_SIZE=$(du -h "$LATEST_FILE" | cut -f1)
        LAST_BACKUP="${LAST_BACKUP} (${BACKUP_SIZE})"
    fi
fi

# Backup count in Wasabi
WASABI_COUNT="Unknown"
if command -v aws &>/dev/null; then
    WASABI_COUNT=$(aws s3 ls --profile wasabi --endpoint-url https://s3.us-east-1.wasabisys.com "s3://ssa-backups/${PSP_ID}/db/" 2>/dev/null | wc -l || echo "Error")
fi

# Last update result
LAST_UPDATE="No update log found"
if [ -f "$UPDATE_LOG" ]; then
    LAST_UPDATE=$(tail -3 "$UPDATE_LOG" | tr '\n' ' ')
fi

# Recent Tomcat errors (last 24h)
ERROR_COUNT=0
RECENT_ERRORS=""
if [ -f "$CATALINA_LOG" ]; then
    ERROR_COUNT=$(grep -c -i "exception\|error\|SEVERE" "$CATALINA_LOG" 2>/dev/null || echo "0")
    if [ "$ERROR_COUNT" -gt 0 ]; then
        RECENT_ERRORS=$(grep -i "exception\|error\|SEVERE" "$CATALINA_LOG" 2>/dev/null | tail -10)
    fi
fi

# Uptime
SYSTEM_UPTIME=$(uptime -p 2>/dev/null || uptime)

# --- Build Email Body ---
EMAIL_SUBJECT="[SSA Health] ${PSP_ID} — ${TIMESTAMP}"

EMAIL_BODY="SSA Daily Health Report
================================================
PSP ID:          ${PSP_ID}
Hostname:        ${HOSTNAME}
Report Time:     ${TIMESTAMP}
System Uptime:   ${SYSTEM_UPTIME}

SERVICES
------------------------------------------------
Tomcat 10:       ${TOMCAT_STATUS}
MySQL 8.0:       ${MYSQL_STATUS}
DB Initialized:  ${DB_INITIALIZED}

VERSIONS
------------------------------------------------
WAR Version:     ${WAR_VERSION}
DB Migration:    ${DB_MIGRATION}

STORAGE
------------------------------------------------
Disk Usage:      ${DISK_USAGE}

BACKUPS
------------------------------------------------
Last Local:      ${LAST_BACKUP}
Wasabi Count:    ${WASABI_COUNT} files

UPDATES
------------------------------------------------
Last Result:     ${LAST_UPDATE}

ERRORS (last 24h)
------------------------------------------------
Error Count:     ${ERROR_COUNT}
"

if [ "$ERROR_COUNT" -gt 0 ]; then
    EMAIL_BODY="${EMAIL_BODY}
Recent Errors (last 10):
${RECENT_ERRORS}
"
fi

EMAIL_BODY="${EMAIL_BODY}
================================================
End of report — ${PSP_ID}
"

# --- Send Email via SMTP ---
send_email() {
    python3 << PYEOF
import smtplib
from email.mime.text import MIMEText

msg = MIMEText("""${EMAIL_BODY}""")
msg['Subject'] = '${EMAIL_SUBJECT}'
msg['From'] = '${SMTP_FROM}'
msg['To'] = '${SMTP_TO}'

try:
    server = smtplib.SMTP('${SMTP_SERVER}', ${SMTP_PORT})
    server.starttls()
    server.login('${SMTP_USER}', '${SMTP_PASSWORD}')
    server.sendmail('${SMTP_FROM}', '${SMTP_TO}', msg.as_string())
    server.quit()
    print('✅ Health check email sent to ${SMTP_TO}')
except Exception as e:
    print(f'❌ Failed to send health check email: {e}')
PYEOF
}

send_email

# --- Log locally ---
echo "${TIMESTAMP} — Health check sent for ${PSP_ID}" >> "$HEALTH_LOG"
