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
check docker "STRIX and Docker sandbox need a local engine"
check strix "pipx install strix-agent or https://github.com/usestrix/strix"
check skillspector "pip install git+https://github.com/NVIDIA/skillspector.git"
check semgrep "optional SAST (enabled in market-ready profile)"
check gitleaks "optional secrets scan"

if [[ -z "${STRIX_LLM:-}" ]]; then
  echo "WARN STRIX_LLM unset — STRIX will ERROR until configured (see docs/STRIX.md)"
else
  echo "OK  STRIX_LLM=${STRIX_LLM}"
fi

if [[ ! -d dist ]]; then
  echo "MISS dist/ — run: npm run build"
  FAIL=1
else
  echo "OK  dist/"
fi

if command -v node >/dev/null 2>&1 && [[ -d dist ]]; then
  if node --experimental-sqlite ./node_modules/vitest/vitest.mjs run tests/unit/federation.test.ts --reporter=dot 2>/dev/null | tail -1 | grep -q pass; then
    echo "OK  federation unit tests"
  fi
fi

if [[ $FAIL -eq 0 ]]; then
  echo "=== Preflight passed (warnings may still apply) ==="
  exit 0
fi
echo "=== Preflight failed — fix MISS items for full market-ready stack ==="
exit 1
