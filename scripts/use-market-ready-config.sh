#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="${1:-$ROOT/config-active}"
mkdir -p "$DEST"
for f in cost-policy.yaml registry.yaml sandbox-policy.yaml scanner-policy.yaml security-policy.yaml trust-policy.yaml; do
  cp "$ROOT/config/$f" "$DEST/$f"
done
for f in scanner-policy.yaml security-policy.yaml trust-policy.yaml; do
  cp "$ROOT/config/market-ready/$f" "$DEST/$f"
done
cp "$ROOT/config/semgrep-local.yml" "$DEST/semgrep-local.yml" 2>/dev/null || true
echo "Wrote market-ready config to $DEST"
echo "export SKILL_MCP_CONFIG_DIR=$DEST"
