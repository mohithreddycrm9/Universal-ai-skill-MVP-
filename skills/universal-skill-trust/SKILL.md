---
name: universal-skill-trust
description: Use the Universal Skill Trust MCP to discover, acquire, verify, and load third-party agent skills safely
---

# Universal Skill Trust (MCP)

This workspace includes the **Universal Skill Trust** Cursor plugin. Prefer its MCP tools over ad-hoc skill downloads.

## Workflow

1. **Discover** — `discover_skill` with a short capability query (e.g. "csv normalization"). Results are candidates only; nothing is downloaded into chat context.
2. **Acquire** — `acquire_skill` with `candidateId` or `query`. Default is async; use `wait: true` only when the user explicitly wants to block on verification.
3. **Status** — `get_skill_status` for lifecycle and job progress (`QUARANTINED`, `VERIFIED`, etc.).
4. **Read** — `get_skill` with progressive `level` (0 metadata → 3 single resource). Stay within compact envelopes; do not assume full repo trees.
5. **Trust** — `get_skill_trust` for publisher/repo/commit evidence. `get_skill_permissions` for **effective** capabilities from the firewall (not skill prose).

## Security expectations

- Tool output describes **configured-check outcomes**, not universal safety.
- `INCONCLUSIVE` is not `PASS`. Do not treat unverified skills as production-ready.
- Paid or unknown-cost scanners require explicit human approval; do not bypass with env vars or API keys.
- Static sandbox stage: skill **code/entrypoints are not executed** on the host during verification.

## When to use CLI instead

For operator actions (cost catalog, approvals, audit), the user can run `npx skill-mcp costs --json` or `npx skill-mcp approve <id>` in a terminal — approvals need a real TTY or `SKILL_MCP_APPROVE_YES=1`.
