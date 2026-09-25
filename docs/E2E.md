# End-to-end validation

Local fixtures prove the documented MVP lifecycle against real gateway and MCP classes. No cloud account, no paid APIs, and no dynamic malware sandbox are required.

## How to run

```bash
npm install
npm run test:e2e          # scenarios A–N only
npm test                  # unit + integration + e2e (vitest include: tests/**/*.test.ts)
npm run validate          # typecheck + build + test (includes e2e)
```

Optional Docker check (skipped cleanly when Docker/Podman is missing):

```bash
npm run validate:docker   # same as validate; scenario L asserts INCONCLUSIVE without Docker
```

## Scenario matrix

| ID | Scenario | How covered | Typical result without live deps |
| --- | --- | --- | --- |
| A | Legitimate skill full flow | `tests/e2e/mvp-lifecycle.test.ts` + LocalSource | PASS |
| B | Malicious/untrusted → not approved | injection + secret fixtures | PASS |
| C | Prompt-injection metadata → L0 UNTRUSTED | discovery L0 only | PASS |
| D | Risk escalation LOW declare + HIGH heuristic | risk fixture | PASS / gated |
| E | Optional scanner FAIL ≠ aggregate PASS | license FAIL (optional) + federation glue | PASS |
| F | TOCTOU pin A vs tip B | MovingTipSource | PASS |
| G | Cache hit | second acquire | PASS |
| H | Cache invalidation | artifact change e2e; scanner/policy/legacy/expiry in unit | PASS |
| I | Capability firewall | requestCapability | PASS |
| J | Permission binding | identity change resets elevated grants | PASS |
| K | ALLOW_FREE_ONLY blocks paid | PaidRegistrySource | PASS |
| L | Docker static-only | DockerSandbox; missing → INCONCLUSIVE | INCONCLUSIVE / skip when no Docker |
| M | MCP protocol | InMemoryTransport client ↔ createMcpServer | PASS (stdio equivalent) |
| N | Restart/persistence | file-backed SQLite | PASS |

## Optional / live integrations

| Integration | Status in default CI/local validate |
| --- | --- |
| Live GitHub fetch | **NOT_EXECUTED** (no network; LocalSource fixtures) |
| Docker/Podman sandbox | **INCONCLUSIVE** when unavailable (never recorded as PASS) |
| Paid scanners (Snyk, etc.) | **NOT_EXECUTED** / blocked by cost policy |

Absence of an optional scanner or sandbox is never treated as PASS.

## Notes

- E2E reuses `tests/helpers.ts` (`LocalSource`, `createGateway`, benign/malicious fixtures).
- Progressive disclosure: level 0 metadata is always available; level ≥ 1 content requires AVAILABLE + PASS.
- This suite does **not** claim absolute safety or universal malware freedom.
