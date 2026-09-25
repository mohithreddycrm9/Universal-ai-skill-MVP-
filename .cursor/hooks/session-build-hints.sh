#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
GOAL="$(tr -d '\n' < "$ROOT/.cursor/build-goal.txt" 2>/dev/null || true)"
python3 - <<'PY' "$GOAL"
import json, sys
goal = sys.argv[1] if len(sys.argv) > 1 else ""
ctx = (
    "This project uses **build hints**. After Write/Shell tools, you will receive a "
    "## Build hints block in additional_context — copy it into every assistant reply "
    "so the user sees suggestion chips. Build goal: "
    + (goal or "(set .cursor/build-goal.txt)")
)
print(json.dumps({"additional_context": ctx}))
PY
