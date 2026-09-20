# Cost policy

The Universal Skills MCP core is **free/open-source-first** and defaults to **$0 cost** (`ALLOW_FREE_ONLY`). Clone and run it with local SQLite, local/OSS scanners, and local sandbox. No cloud account, paid API, paid LLM, or commercial scanner is required.

This MCP delivers **skills** (how to work with a technology). It does **not** execute Terraform, AWS, ServiceNow, or similar. Execution stays with other tools.

## Flow

`CHECK COST → clearly free? continue / paid or unknown → inform human → ask approval → APPROVE continue / REJECT stop`

Unknown cost is **not** treated as free. Paid fallbacks are **never** automatic (for example a free scanner failure must not silently call Snyk).

## Policies (`config/cost-policy.yaml`)

| Policy | Behavior |
| --- | --- |
| `ALLOW_FREE_ONLY` | **Default ($0).** Deny paid/unknown. Free local path only. |
| `ASK_BEFORE_ANY_PAID_OPERATION` | Free proceeds. Paid/unknown require an explicit human approval record. |
| `DENY_ALL_PAID_SERVICES` | Deny paid/unknown. |
| `ALLOW_UP_TO_AMOUNT` | Numeric estimates at or below `allowUpToAmount` may proceed. **Unknown still requires approval.** |

`neverAutoPaidFallback: true` is required.

## Human review contents

Operation, provider, service, purpose, reason, potential cost, free alternative, risk. Copy states that approval is optional and that rejecting leaves the operation unperformed.

## Optional integrations that may incur cost

These are **adapters only**. They are not required for core. Do not assume they are free.

| Id | Provider | Pricing | Default |
| --- | --- | --- | --- |
| `snyk` | Snyk CLI / mcp-scan | paid / cost unknown | disabled; skipped unless `includePaidScanners` **and** approval |
| `github_private_or_unknown` | Private or non-public GitHub API | unknown | blocked until approval |
| Unimplemented remote registries | unspecified | unknown | blocked until approval |

Public GitHub REST for public repos is documented as **freemium with a $0 free tier** for rate-limited public data — not a usage invoice. That is not a claim that GitHub Enterprise, Actions minutes, or private seats are free.

Local Docker/Podman is OSS compute on the operator’s machine, not a cloud bill. Cloud-hosted runners are out of scope for this adapter.

There is **no** built-in paid LLM path.

## Tools / CLI

- MCP: `list_integrations`, `list_pending_cost_approvals`, `approve_paid_operation`, `reject_paid_operation`
- CLI: `skill-mcp costs`, `skill-mcp approvals`, `skill-mcp approve <id>`, `skill-mcp reject <id>`
