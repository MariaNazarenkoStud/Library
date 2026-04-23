#!/usr/bin/env bash
# Dev run script — compiles from source and launches immediately.
# Use this during development instead of building a JAR every time.
# Usage: ./docs/scripts/run-dev.sh
# Run from the project root directory.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
OUT_DIR="$ROOT_DIR/out"

cd "$ROOT_DIR"

echo "[dev] Compiling sources..."
mkdir -p "$OUT_DIR"
javac -d "$OUT_DIR" src/*.java
echo "[dev] Launching BookGUI..."
exec java -cp "$OUT_DIR" BookGUI
