#!/usr/bin/env bash
# Dumps the local finance-tracker database to a timestamped file.
#   local/backup.sh                # writes to ~/finance/backups
#   BACKUP_DIR=/some/dir local/backup.sh
# Restore into an empty database with:
#   docker exec -i <container> pg_restore -U pft -d pft --clean --if-exists < <dump file>
set -euo pipefail

CONTAINER="${PFT_DB_CONTAINER:-local-pft-db-1}"
BACKUP_DIR="${BACKUP_DIR:-$HOME/finance/backups}"
KEEP="${KEEP:-30}"   # number of most recent dumps to keep

mkdir -p "$BACKUP_DIR"
file="$BACKUP_DIR/pft-$(date +%Y%m%d-%H%M%S).dump"

docker exec "$CONTAINER" pg_dump -U pft -d pft --format=custom > "$file.tmp"
mv "$file.tmp" "$file"
echo "Backup written to $file ($(du -h "$file" | cut -f1))"

# Prune old dumps, newest first, keeping $KEEP.
ls -1t "$BACKUP_DIR"/pft-*.dump 2>/dev/null | tail -n +"$((KEEP + 1))" | xargs -r rm --
