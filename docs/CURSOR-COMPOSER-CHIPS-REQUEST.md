# Feature request: Build-hint chips next to PR pills (Cloud Agent composer)

## User expectation

On **cursor.com/agents** (and desktop Agents), the row **above** the “Follow up…” field shows git/PR chrome, for example:

- `View PR Draft` with `+N / -M`
- `Mark Ready`

Users want **build-scoped suggestion chips in that same row** (same visual weight as those pills), not only in chat or MCP JSON.

## What we have today (this repo)

| Layer | Can render in PR pill row? |
| --- | --- |
| `get_build_suggestions` MCP | No — returns JSON only |
| `.cursor/hooks.json` `postToolUse` → `additional_context` | No — feeds the **model**, not the composer chrome |
| `.cursor/rules` | No — instructs the agent to paste markdown in chat |
| `npm run suggest` | Terminal only |

## Proposed Cursor product API

Allow **project hooks** or **MCP tools** to return composer chips consumed by the client:

```json
{
  "composer_chips": [
    {
      "id": "fix-tests",
      "label": "Fix tests: gateway.test.ts",
      "prompt": "Tests failed while building …",
      "action": "insert_followup"
    }
  ]
}
```

**Hook:** extend `postToolUse` / `stop` output with optional `composer_chips[]` (same schema as MCP `get_build_suggestions`).

**Behavior:**

- Render chips in the **PR pill row** (left of or after `View PR Draft`), dismissible per chip.
- Click → pre-fill **Follow up…** (user sends; respect Queue/Steer).
- Refresh when hook runs or MCP `get_build_suggestions` is called with new `buildContextUsed`.

**Reference implementation:** this repo’s `src/agent/build-suggestions.ts` + `scripts/hook-build-hints.mjs`.

## Forum post (copy/paste)

**Title:** Build-hint chips in Cloud Agent composer row (next to View PR Draft)

**Body:** When a cloud agent is building, show 3–5 dismissible chips in the same pill row as “View PR Draft” / diff stats, sourced from project hooks or MCP `get_build_suggestions`. Chips should pre-fill “Follow up…” with build-scoped prompts (goal, failing test, pending shell). We have schema + heuristics in open source; need a `composer_chips` field on hook/MCP responses. [Link to this doc in repo.]
