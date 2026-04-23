#!/usr/bin/env bash
# Build script — compiles sources, packages executable JAR, generates Javadoc.
# Usage: ./docs/scripts/build.sh [--skip-docs]
# Run from the project root directory.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
OUT_DIR="$ROOT_DIR/out"
DIST_DIR="$ROOT_DIR/dist"
MANIFEST="$OUT_DIR/MANIFEST.MF"
JAR_NAME="BookCatalogue.jar"
SKIP_DOCS="${1:-}"

cd "$ROOT_DIR"

echo "=== Book Catalogue Build Script ==="
echo "Root: $ROOT_DIR"
echo ""

# --- Step 1: Compile ---
echo "[1/3] Compiling sources..."
mkdir -p "$OUT_DIR"
javac -d "$OUT_DIR" src/*.java
echo "      Compilation successful."

# --- Step 2: Package JAR ---
echo "[2/3] Packaging $JAR_NAME..."
mkdir -p "$DIST_DIR"

cat > "$MANIFEST" <<EOF
Manifest-Version: 1.0
Main-Class: BookGUI
Implementation-Title: Book Catalogue
Implementation-Version: 1.2.0
Built-By: $(whoami)
Build-Date: $(date -u +"%Y-%m-%dT%H:%M:%SZ")
EOF

jar cfm "$DIST_DIR/$JAR_NAME" "$MANIFEST" -C "$OUT_DIR" .
echo "      JAR created: $DIST_DIR/$JAR_NAME"

# --- Step 3: Javadoc (optional) ---
if [[ "$SKIP_DOCS" != "--skip-docs" ]]; then
    echo "[3/3] Generating Javadoc..."
    javadoc \
        -d "$ROOT_DIR/docs/javadoc" \
        -sourcepath "$ROOT_DIR/src" \
        -encoding UTF-8 \
        -charset UTF-8 \
        -author \
        -version \
        -windowtitle "Book Catalogue API" \
        -doctitle "Book Catalogue — Javadoc" \
        "$ROOT_DIR/src"/*.java 2>&1 | grep -v "^Loading\|^Constructing\|^Building\|^Standard\|^Generating"
    echo "      Javadoc generated: docs/javadoc/index.html"
else
    echo "[3/3] Skipping Javadoc (--skip-docs flag set)."
fi

echo ""
echo "=== Build complete ==="
echo "Executable JAR: $DIST_DIR/$JAR_NAME"
echo "Run with:       java -jar $DIST_DIR/$JAR_NAME"
