#!/usr/bin/env bash
# Quick build alias — delegates to docs/scripts/build.sh.
# Run from the project root: ./build.sh [--skip-docs]
set -euo pipefail
exec "$(dirname "$0")/docs/scripts/build.sh" "$@"
