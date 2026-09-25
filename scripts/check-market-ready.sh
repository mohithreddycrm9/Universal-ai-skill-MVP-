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

echo "=== Universal Skill Trust — org-approved preflight ==="
check node "Node.js 20.11+ required"

if [[ ! -d dist ]]; then
  echo "MISS dist/ — run: npm run build"
  FAIL=1
else
  echo "OK  dist/"
fi

if grep -q 'extendedScanningEnabled: true' "${SKILL_MCP_CONFIG_DIR:-$ROOT/config}/security-policy.yaml" 2>/dev/null; then
  echo "Extended scanning enabled — checking optional CLIs"
  check skillspector "pip install git+https://github.com/NVIDIA/skillspector.git"
  check semgrep "optional SAST"
  check gitleaks "optional secrets scan"
else
  echo "OK  built-in scanners only (extendedScanningEnabled: false)"
fi

if [[ $FAIL -eq 0 ]]; then
  echo "=== Preflight passed ==="
  exit 0
fi
echo "=== Preflight failed ==="
exit 1
