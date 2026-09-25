#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
FAIL=0

check() {
  if command -v "$1" >/dev/null 2>&1; then
    echo "OK  $1"
  else
    echo "MISS $1 — $2"
    FAIL=1
  fi
}

echo "=== Universal Skill Trust — market-ready preflight ==="
check node "Node.js 20.11+ required"
check skillspector "pip install git+https://github.com/NVIDIA/skillspector.git"
check semgrep "optional SAST (enabled in market-ready profile)"
check gitleaks "optional secrets scan"

if [[ ! -d dist ]]; then
  echo "MISS dist/ — run: npm run build"
  FAIL=1
else
  echo "OK  dist/"
fi

if [[ $FAIL -eq 0 ]]; then
  echo "=== Preflight passed (warnings may still apply) ==="
  exit 0
fi
echo "=== Preflight failed — fix MISS items for full market-ready stack ==="
exit 1
