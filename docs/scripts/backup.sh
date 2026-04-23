#!/usr/bin/env bash
# Backup script — creates a timestamped backup of catalogue data files (.dat),
# the current JAR, and any exported CSV files.
# Usage: ./docs/scripts/backup.sh [backup-root-directory]
# Default backup root: ~/BookCatalogue-backups/

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
BACKUP_ROOT="${1:-$HOME/BookCatalogue-backups}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_DIR="$BACKUP_ROOT/$TIMESTAMP"

echo "=== Book Catalogue Backup ==="
echo "Source:  $ROOT_DIR"
echo "Dest:    $BACKUP_DIR"
echo ""

mkdir -p "$BACKUP_DIR"

# 1. Catalogue data files
DAT_COUNT=0
while IFS= read -r -d '' f; do
    cp -v "$f" "$BACKUP_DIR/"
    ((DAT_COUNT++)) || true
done < <(find "$ROOT_DIR" -maxdepth 1 -name "*.dat" -print0 2>/dev/null)

if [[ "$DAT_COUNT" -eq 0 ]]; then
    echo "(no .dat files found in project root)"
fi

# 2. Current JAR
if [[ -f "$ROOT_DIR/dist/BookCatalogue.jar" ]]; then
    cp -v "$ROOT_DIR/dist/BookCatalogue.jar" "$BACKUP_DIR/BookCatalogue.jar"
else
    echo "(JAR not found — run build.sh first)"
fi

# 3. CSV exports (optional)
CSV_COUNT=0
while IFS= read -r -d '' f; do
    cp -v "$f" "$BACKUP_DIR/"
    ((CSV_COUNT++)) || true
done < <(find "$ROOT_DIR" -maxdepth 1 -name "*.csv" -print0 2>/dev/null)

# 4. Write backup metadata
cat > "$BACKUP_DIR/backup-info.txt" <<EOF
Backup created: $(date -u +"%Y-%m-%d %H:%M:%S UTC")
Host:           $(hostname)
User:           $(whoami)
Source dir:     $ROOT_DIR
.dat files:     $DAT_COUNT
.csv files:     $CSV_COUNT
EOF

echo ""
echo "=== Backup complete ==="
echo "Location: $BACKUP_DIR"
echo ""

# 5. Rotation: keep only the 10 most recent backups
MAX_BACKUPS=10
BACKUP_COUNT=$(find "$BACKUP_ROOT" -mindepth 1 -maxdepth 1 -type d | wc -l | tr -d ' ')
if [[ "$BACKUP_COUNT" -gt "$MAX_BACKUPS" ]]; then
    echo "Rotating old backups (keeping last $MAX_BACKUPS)..."
    find "$BACKUP_ROOT" -mindepth 1 -maxdepth 1 -type d | sort | head -n "-$MAX_BACKUPS" | xargs rm -rf
    echo "Rotation complete."
fi
