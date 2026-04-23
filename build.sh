#!/usr/bin/env bash
# Compile sources and generate Javadoc documentation.
# Run from the project root: ./build.sh

set -e

echo "=== Compiling sources ==="
mkdir -p out
javac -d out src/*.java
echo "Compilation successful."

echo ""
echo "=== Generating Javadoc ==="
javadoc \
  -d docs/javadoc \
  -sourcepath src \
  -encoding UTF-8 \
  -charset UTF-8 \
  -author \
  -version \
  -windowtitle "Book Catalogue API" \
  -doctitle "Book Catalogue — Javadoc" \
  src/*.java
echo "Javadoc generated → docs/javadoc/index.html"
