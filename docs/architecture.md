# Architecture (historical snapshot)

This file was written for the original skill-registry-centric design. The product is now a **Universal Skill Trust Gateway**. Use [ARCHITECTURE.md](./ARCHITECTURE.md) and [ARCHITECTURE-MIGRATION.md](./ARCHITECTURE-MIGRATION.md) as the living docs.

The module map below remains accurate for on-disk layout.


Universal AI Skill Acquisition & Trust MCP is a software-agnostic control plane that lets an agent discover a capability it lacks, locate a candidate from a pluggable source, verify publisher provenance, run a hard security gate, sandbox executable components, and expose a **compact verified skill** to the model.

The MCP server is the only agent interface. The security engine does not depend on an LLM. External repository content is always untrusted data.

## Distinctions (non-negotiable)

| Concept | Role |
| --- | --- |
| Skill | Tells the agent **how** to perform a task (instructions + compact manifest). |
| Tool | MCP surface: **what** the agent can invoke (`discover_skill`, `acquire_skill`, …). |
| Capability | **What** a skill/tool may access (`filesystem.read`, `network.write`, …). |
| Permission / policy | **Whether** a capability is allowed in this context. |
| Trust | Provenance evidence (publisher → org → repo → release → commit → fingerprint). Never a single score. |
| Security | Independent gate: `PASS` / `FAIL` / `INCONCLUSIVE` / `ERROR` / `TIMEOUT`. |
| Quality | Separate from security. Quality cannot override a security failure. |
| Memory | Registry, cache, audit. Not model context. |
| Execution | Sandbox only. Never on the host. |

A skill **cannot grant itself permissions**.

## Module map

```
src/
  mcp/           MCP server, tool registration, bounded JSON responses
  discovery/     SkillSource interface + GitHub (and stub) adapters
  trust/         Publisher tiers, commit pinning, trust graph
  security/      Parallel orchestrator + hard gate
  scanners/      Pluggable Scanner adapters (STRIX, secrets, SBOM, injection, …)
  sandbox/       Container sandbox + behavioral fingerprint
  capabilities/  Independent capability firewall
  policy/        YAML-loaded policies (never hard-coded into business rules)
  registry/      DB abstraction (SQLite default, PostgreSQL-swappable)
  cache/         Content-addressed verified cache
  skills/        Manifest schema, fingerprinting, lifecycle
  audit/         Append-only audit events (no secrets)
  workers/       Async acquisition queue
  observability/ Structured logs + in-process metrics
  cli/           skill-mcp
```

Core engine modules import **interfaces**, not GitHub or STRIX concretions.

## Acquisition pipeline

```
DISCOVER → UNTRUSTED → VERIFYING → SCANNING → SANDBOXING → APPROVED → AVAILABLE
                                          ↘ QUARANTINED / REJECTED
EXPIRED / INVALIDATED at any later point
```

Gates are ordered and cannot be skipped. `INCONCLUSIVE` is never coerced to `PASS`. Default policy favors reject/quarantine over trust.

User-facing MCP calls return immediately with a job/status. Full scans run on workers unless the caller explicitly waits.

## Token budget

MCP responses never include repositories, full scan logs, lockfiles, or source trees. The model receives: name, description, compact instructions, permissions, trust/security status, version, fingerprint, and identifiers for follow-up calls.

## Database

SQLite via Node's `node:sqlite` is the default. `DatabaseAdapter` is the swap point for PostgreSQL. Schema: see `docs/database-schema.md`.

## Transports

- **stdio** — default MCP client integration (Cursor, Claude Desktop, Codex).
- **optional HTTP** — Streamable HTTP MCP plus `/health` for operators. Stdout is reserved for MCP stdio; logs go to stderr.
