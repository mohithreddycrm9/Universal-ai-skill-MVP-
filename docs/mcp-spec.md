# MCP specification

Server name: `universal-skill-trust`  
Protocol: Model Context Protocol (stdio default; optional Streamable HTTP)

All tools return a compact JSON envelope:

```json
{
  "ok": true,
  "requestId": "req_…",
  "data": {},
  "warnings": [],
  "securityNotice": "Results describe configured-check outcomes, not universal safety."
}
```

Hard limits: 32 KiB encoded JSON (truncation recorded in `warnings`). Never include repository trees, full scanner logs, SBOMs larger than a summary, or raw source files.

## Tools

| Tool | Purpose |
| --- | --- |
| `discover_skill` | Find candidates for a needed capability. Does not download a repo into context. |
| `acquire_skill` | Start async acquisition. Returns job/skill ids immediately. |
| `verify_skill` | Publisher + provenance + pin. Does not imply security pass. |
| `scan_skill` | Enqueue or (optionally) run configured scanners. |
| `get_skill` | Compact verified (or current) skill card. |
| `list_skills` | Registry listing with filters. |
| `search_skills` | Search persisted skills by text. |
| `get_skill_status` | Lifecycle + job progress. |
| `invalidate_skill` | Operator invalidation. |
| `refresh_skill` | Re-pin / re-scan if fingerprint inputs changed. |
| `compare_skill_versions` | Diff two fingerprints/commits at summary level. |
| `explain_skill_trust` | Evidence for publisher/repo/commit — not a numeric score. |
| `get_skill_permissions` | Effective permissions from the firewall, not from skill prose. |
| `request_capability` | Ask the firewall for an additional capability (user/policy). |
| `release_skill` | Drop session/temporary materialization. |
| `get_build_suggestions` | Ranked proactive chips for Agent UI (heuristics + optional local skill / approval hints). |

### `get_build_suggestions` (Cursor build hints)

Call while Agent is active. Input (all optional except typical use includes `agentState`):

- `agentState`: `running` | `waiting_for_user` | `idle` | `failed`
- `goal`: user task summary (enables skill discovery hints when `includeSkillHints` is true)
- `recentEvents`: `{ kind, summary?, files?, serverName? }` — e.g. `shell_pending`, `test_failed`, `mcp_auth_error`, `multi_file_diff`
- `filesChangedCount`, `lastCommandExitCode`, `limit` (1–12), `includeSkillHints` (default true)

Response `data.suggestions[]` items include `label`, `action` (`insert_prompt` | `invoke_tool` | `cursor_setting` | `note`), optional `prompt`, `toolName` / `toolArgs`, `priority`, `confidence`, and `uiHint` for host rendering. Hosts must not auto-send prompts without user confirmation.

## Errors

Structured `ok: false` with `code` in:

`INVALID_INPUT`, `NOT_FOUND`, `ILLEGAL_LIFECYCLE`, `SECURITY_GATE`, `POLICY_DENIED`, `SOURCE_ERROR`, `TIMEOUT`, `INTERNAL`

## Logging

Every call gets a `requestId` (UUIDv4). Logs go to **stderr** as JSON. Secrets redacted.
