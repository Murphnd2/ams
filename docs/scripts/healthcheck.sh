#!/bin/bash
# =============================================================================
# SSA Health Check Script
# Sends a daily status email to the master admin
# Location: /opt/ssa/scripts/healthcheck.sh
# Cron: 0 6 * * * /opt/ssa/scripts/healthcheck.sh
#
# SMTP config read from DB constants (SYS_HEALTH_* in constant table).
# Falls back to hardcoded values if DB is unreachable.
# Disable email via: UPDATE constant SET value='false' WHERE name='SYS_HEALTH_ENABLED';
# =============================================================================

set -euo pipefail

# --- Configuration ---
PROPS_FILE="/var/lib/tomcat10/conf/ssa.properties"
LOG_DIR="/opt/ssa/logs"
BACKUP_DIR="/opt/ssa/backups"
VERSION_FILE="/opt/ssa/current_version.txt"
UPDATE_LOG="/opt/ssa/logs/update.log"
HEALTH_LOG="${LOG_DIR}/healthcheck.log"
CATALINA_LOG="/var/lib/tomcat10/logs/catalina.out"
DB_NAME="beta_ssa"

# --- Hardcoded SMTP fallback (used only if DB query fails) ---
FB_SMTP_TO="kevin@superiorstate.net"
FB_SMTP_SERVER="mail.smtp2go.com"
FB_SMTP_PORT="2525"
FB_SMTP_USER="jspSmtpSender"
FB_SMTP_PASSWORD="MZSVP0ZYm3YiTK0w"

# --- Helper: read a constant from the DB ---
db_constant() {
    local val
    val=$(mysql -u root -N -e "SELECT value FROM ${DB_NAME}.constant WHERE name='$1';" 2>/dev/null || echo "")
    echo "$val"
}

# --- Read PSP_ID from ssa.properties ---
PSP_ID="UNKNOWN"
if [ -f "$PROPS_FILE" ]; then
    PSP_ID=$(grep -E "^PSP_ID=" "$PROPS_FILE" | cut -d'=' -f2 | tr -d '[:space:]')
fi

if [ "$PSP_ID" = "UNINITIALIZED" ] || [ -z "$PSP_ID" ]; then
    echo "$(date '+%Y-%m-%d %H:%M:%S') SKIP — PSP_ID is $PSP_ID" >> "$HEALTH_LOG"
    exit 0
fi

# --- Check if health email is enabled ---
HEALTH_ENABLED=$(db_constant "SYS_HEALTH_ENABLED")
if [ "$HEALTH_ENABLED" = "false" ]; then
    echo "$(date '+%Y-%m-%d %H:%M:%S') SKIP — Health email disabled for ${PSP_ID}" >> "$HEALTH_LOG"
    exit 0
fi

# --- Read SMTP config from DB constants, fall back to hardcoded ---
SMTP_TO=$(db_constant "SYS_HEALTH_EMAIL_TO")
SMTP_SERVER=$(db_constant "SYS_HEALTH_SMTP_SERVER")
SMTP_PORT=$(db_constant "SYS_HEALTH_SMTP_PORT")
SMTP_USER=$(db_constant "SYS_HEALTH_SMTP_USER")
SMTP_PASSWORD=$(db_constant "SYS_HEALTH_SMTP_PASSWORD")

# Apply fallbacks if DB values are empty
[ -z "$SMTP_TO" ]       && SMTP_TO="$FB_SMTP_TO"
[ -z "$SMTP_SERVER" ]   && SMTP_SERVER="$FB_SMTP_SERVER"
[ -z "$SMTP_PORT" ]     && SMTP_PORT="$FB_SMTP_PORT"
[ -z "$SMTP_USER" ]     && SMTP_USER="$FB_SMTP_USER"
[ -z "$SMTP_PASSWORD" ] && SMTP_PASSWORD="$FB_SMTP_PASSWORD"

SMTP_SOURCE="DB"
if [ "$SMTP_TO" = "$FB_SMTP_TO" ] && [ "$SMTP_SERVER" = "$FB_SMTP_SERVER" ]; then
    SMTP_SOURCE="Fallback"
fi

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
    DB_MIGRATION=$(mysql -u root -N -e "SELECT COALESCE(MAX(version),'None') FROM ${DB_NAME}.schema_version;" 2>/dev/null || echo "Query failed")
fi

# DB initialized check
DB_INITIALIZED="Unknown"
if command -v mysql &>/dev/null; then
    SSL_CHECK=$(mysql -u root -N -e "SELECT value FROM ${DB_NAME}.constant WHERE name='SSL_PORT';" 2>/dev/null || echo "")
    if [ "$SSL_CHECK" = "443" ]; then
        DB_INITIALIZED="✅ Yes"
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
SMTP Source:     ${SMTP_SOURCE}
End of report — ${PSP_ID}
"

# --- Send Email via SMTP ---
send_email() {
    python3 << PYEOF
import smtplib
from email.mime.text import MIMEText

msg = MIMEText("""${EMAIL_BODY}""")
msg['Subject'] = '${EMAIL_SUBJECT}'
msg['From'] = '${SMTP_USER}'
msg['To'] = '${SMTP_TO}'

try:
    server = smtplib.SMTP('${SMTP_SERVER}', ${SMTP_PORT})
    server.starttls()
    server.login('${SMTP_USER}', '${SMTP_PASSWORD}')
    server.sendmail('${SMTP_USER}', '${SMTP_TO}', msg.as_string())
    server.quit()
    print('✅ Health check email sent to ${SMTP_TO} (via ${SMTP_SOURCE})')
except Exception as e:
    print(f'❌ Failed to send health check email: {e}')
PYEOF
}

send_email

# --- Log locally ---
echo "${TIMESTAMP} — Health check sent for ${PSP_ID} (SMTP: ${SMTP_SOURCE})" >> "$HEALTH_LOG"