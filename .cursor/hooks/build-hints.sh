#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
if [[ ! -f "$ROOT/dist/agent/build-suggestions.js" ]]; then
  npm run build --silent 2>/dev/null || true
fi
exec node --experimental-sqlite "$ROOT/scripts/hook-build-hints.mjs"
