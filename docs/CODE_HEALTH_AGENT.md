# Cybersecurity & code health agent

Use this repo as a **local, free-first** security and hygiene checker for any directory on disk — not only acquired skills.

## Quick start

```bash
npm install && npm run build
npx skill-mcp health-check . --json
```

## MCP tool

| Tool | Purpose |
| --- | --- |
| `run_code_health_check` | Walk a workspace (bounded file count/size), run built-in scanners, return a federated summary |

Built-in profile (no STRIX, no paid tools):

- `secret`, `prompt_injection`, `suspicious_files`, `dependency`, `license`, `code_health`

Pass `includeOssCli: true` to also invoke locally installed **semgrep**, **gitleaks**, **trivy**, or **osv-scanner** when present. Missing binaries remain `ERROR` / `INCONCLUSIVE`, never `PASS`.

## Cursor / Claude agent skill

Example instructions: [examples/skills/benign/cybersecurity-code-health/SKILL.md](../examples/skills/benign/cybersecurity-code-health/SKILL.md)

Wire the MCP server in your client config (`examples/mcp-clients/cursor.json`) and enable the skill or paste its body into project rules.

## Continuous / production monitoring

For CI, cron, Docker, and Kubernetes (not IDE-only), use **`skill-mcp security-watch`** — see [CONTINUOUS_SECURITY.md](CONTINUOUS_SECURITY.md).

## Claims

Results are **configured-check outcomes** for the files read under the scan limits. They are not penetration tests or universal safety guarantees. `INCONCLUSIVE` is not `PASS`.
