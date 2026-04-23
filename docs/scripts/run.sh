#!/usr/bin/env bash
# Run script (production) — launches the packaged BookCatalogue.jar.
# Usage: ./docs/scripts/run.sh [path/to/BookCatalogue.jar]
# Run from any directory.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
DEFAULT_JAR="$ROOT_DIR/dist/BookCatalogue.jar"
JAR="${1:-$DEFAULT_JAR}"

if [[ ! -f "$JAR" ]]; then
    echo "ERROR: JAR not found at $JAR"
    echo "Build the project first:  ./docs/scripts/build.sh"
    exit 1
fi

JAVA_MIN_VERSION=17
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)

if [[ "$JAVA_VERSION" -lt "$JAVA_MIN_VERSION" ]]; then
    echo "ERROR: Java $JAVA_MIN_VERSION+ required, found Java $JAVA_VERSION"
    exit 1
fi

echo "Starting Book Catalogue..."
echo "JAR: $JAR"
exec java -jar "$JAR"
