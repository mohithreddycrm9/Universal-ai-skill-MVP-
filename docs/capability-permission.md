# Capability and permission specification

Capabilities are a closed vocabulary evaluated by the firewall **independently** of skill text.

```
filesystem.read
filesystem.write
network.read
network.write
shell.execute
process.spawn
credential.read
browser.control
database.read
database.write
cloud.read
cloud.write
production.deploy
```

## Decisions

`ALLOW` | `DENY` | `REQUIRE_USER_APPROVAL`

Default policy (see `config/sandbox-policy.yaml` / trust+security):

| Capability | Default |
| --- | --- |
| filesystem.read | ALLOW for documentation skills after gate |
| filesystem.write | DENY |
| network.* | DENY unless declared **and** permitted |
| shell.execute / process.spawn | DENY |
| credential.read | DENY |
| production.deploy / cloud.write | DENY (CRITICAL; explicit authorization) |

## Self-grant prohibition

`request_capability` is the only mutation path. MCP/model calls create a **PENDING** approval record only. Caller-supplied `approver` / `humanApproved` strings **never** authorize.

Trusted path (only way to `APPROVED`):

1. Operator runs CLI `skill-mcp approve <approvalId>` **outside** the MCP tool surface.
2. Interactive `y/N` confirmation on a local TTY (or explicit `SKILL_MCP_APPROVE_YES=1` for operator automation).
3. Record is set `status=APPROVED`, `method=local_interactive`, with `approvedAt` and binding to `skillId+repository+commitSha+fingerprint+capability`.
4. Agent re-invokes `request_capability` with `approvalId`. Firewall receives a **TrustedApprovalDecision**, not a raw approver string.
5. High-risk capabilities are **one-time consumed** (`CONSUMED`). `EXPIRED` / `REJECTED` / `CONSUMED` deny.

Audit: `CAPABILITY_REQUESTED` (actor=agent) vs `CAPABILITY_APPROVED` (actor=human, trusted channel only).

Skill manifest fields cannot add to `effectivePermissions`.

Composed skills inherit the **strictest** constraint among components.


## Permissions identity binding

Elevated permissions are bound to immutable skill identity: `skillId + repository + commitSha + fingerprint`.

- Same artifact + successful revalidation → elevated grants **may be preserved**.
- Commit/fingerprint change **or** security invalidation → elevated permissions **reset** to baseline (`filesystem.read`); structured output and audit include `permissionsReset` + `permissionsResetReason` (never silent).
- Effective permissions = `declared ∩ granted` (+ `filesystem.read` baseline). Skills cannot self-grant.
