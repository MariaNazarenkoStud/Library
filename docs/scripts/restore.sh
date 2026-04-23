#!/usr/bin/env bash
# Restore script — restores data files and/or JAR from a backup directory.
# Usage:
#   ./docs/scripts/restore.sh                        # restore from latest backup
#   ./docs/scripts/restore.sh /path/to/backup-dir   # restore from specific backup

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
BACKUP_ROOT="$HOME/BookCatalogue-backups"

# Determine backup directory to restore from
if [[ -n "${1:-}" ]]; then
    BACKUP_DIR="$1"
else
    # Auto-select latest backup
    BACKUP_DIR=$(find "$BACKUP_ROOT" -mindepth 1 -maxdepth 1 -type d | sort | tail -1)
fi

if [[ -z "$BACKUP_DIR" || ! -d "$BACKUP_DIR" ]]; then
    echo "ERROR: No backup directory found."
    echo "Searched: $BACKUP_ROOT"
    echo "Usage: $0 [backup-directory]"
    exit 1
fi

echo "=== Book Catalogue Restore ==="
echo "Backup source: $BACKUP_DIR"
echo "Restore to:    $ROOT_DIR"
echo ""

# Show backup info if available
if [[ -f "$BACKUP_DIR/backup-info.txt" ]]; then
    echo "--- Backup info ---"
    cat "$BACKUP_DIR/backup-info.txt"
    echo "-------------------"
    echo ""
fi

# Confirmation prompt
read -rp "Proceed with restore? This will overwrite existing .dat files. [y/N] " confirm
if [[ "${confirm,,}" != "y" ]]; then
    echo "Restore cancelled."
    exit 0
fi

# 1. Restore .dat data files
DAT_COUNT=0
while IFS= read -r -d '' f; do
    dest="$ROOT_DIR/$(basename "$f")"
    if [[ -f "$dest" ]]; then
        cp -v "$dest" "${dest}.pre-restore-$(date +%H%M%S)"
    fi
    cp -v "$f" "$dest"
    ((DAT_COUNT++)) || true
done < <(find "$BACKUP_DIR" -maxdepth 1 -name "*.dat" -print0 2>/dev/null)

# 2. Restore JAR (optional — skip if you want to keep the updated JAR)
read -rp "Restore JAR file as well (use for rollback after failed update)? [y/N] " restore_jar
if [[ "${restore_jar,,}" == "y" ]] && [[ -f "$BACKUP_DIR/BookCatalogue.jar" ]]; then
    cp -v "$BACKUP_DIR/BookCatalogue.jar" "$ROOT_DIR/dist/BookCatalogue.jar"
    echo "JAR restored."
fi

echo ""
echo "=== Restore complete ==="
echo ".dat files restored: $DAT_COUNT"
echo ""
echo "You can now run: java -jar dist/BookCatalogue.jar"
