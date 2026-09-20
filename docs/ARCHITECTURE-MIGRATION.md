# Architecture migration: Skill Acquisition MCP → Universal Skill Trust Gateway

This document is the OLD → NEW map for the in-progress implementation. It was written **before** large refactors. Working code is preserved; proprietary surfaces become adapters; the product becomes a **standards-first trust gateway**, not a competing skill protocol.

## Inventory (as of this migration)

| Path | Status before upgrade | Role |
| --- | --- | --- |
| `src/types.ts` | Working | Domain types, custom `SkillManifest`, lifecycle, trust/security enums |
| `src/skills/manifest.ts` | Working | Custom `skill.mcp/v1` parse + package inference |
| `src/skills/fingerprint.ts` | Working | Content-addressed identity |
| `src/skills/lifecycle.ts` | Working | Gate transitions (pre-quarantine-first) |
| `src/registry/*` | Working | SQLite `SkillRegistry` + DB adapter (Postgres stub) |
| `src/discovery/skill-source.ts` | Working | Pluggable `SkillSource` |
| `src/discovery/github.ts` | Working | GitHub adapter (first remote source) |
| `src/discovery/local-source.ts` | Working | Local/test source |
| `src/discovery/unimplemented.ts` | Working | Enterprise/docs/registry placeholders |
| `src/trust/publisher.ts` | Working | Allowlist evidence → trust tier |
| `src/trust/graph.ts` | Working | Publisher→org→repo→commit→fingerprint edges |
| `src/policy/load.ts` | Working | YAML policies |
| `src/audit/audit-log.ts` | Working | Redacted audit events |
| `src/observability/*` | Working | stderr JSON logs + counters |
| `src/scanners/*` | **Missing** | Planned, not landed |
| `src/security/*` | **Missing** | Orchestrator / gate |
| `src/sandbox/*` | **Missing** | SandboxProvider |
| `src/capabilities/*` | **Missing** | Authorization firewall |
| `src/cache/*` | Schema only (`verified_cache`) | Not a first-class CacheProvider |
| `src/mcp/*` | **Missing** | MCP server / tools |
| `src/cli.ts` | **Missing** | CLI |
| Tests / examples / README product docs | **Missing / seed** | Not yet claiming Phase completion |

Nothing in this tree hard-codes a single vendor product into the core. That constraint is kept.

## Product shift

| Before | After |
| --- | --- |
| Custom Universal Skill Acquisition MCP (registry-centric) | **Universal Skill Trust Gateway** sitting between agents and Agent Skills / MCP Skills / MCP servers / GitHub / vendor / enterprise registries / scanners |
| Pipeline conceptually DISCOVER→VERIFY→SCAN→SANDBOX→AUTHORIZE→CACHE→USE | **DISCOVER → VERIFY → SECURITY-CHECK → SANDBOX → AUTHORIZE → CACHE → SERVE** |
| Custom manifest as the skill protocol | **MCP Skills / SKILL.md preferred**; custom manifest retained as persistence + adapter |

## OLD COMPONENT → NEW COMPONENT

| Old | New | Action |
| --- | --- | --- |
| Custom `SkillManifest` (`skill.mcp/v1`) | **MCP Skill representation** (`SKILL.md` frontmatter + body + resource digests) | **Adapter** `Custom → MCP Skill`. Persist both; do not rewrite stored JSON blindly. |
| `src/skills/manifest.ts` | `McpSkillAdapter` + existing parser | Keep parser; add `src/skills/mcp-skill.ts`. |
| `SkillRegistry` class | `SkillRegistry` **port** (interface) + SQLite adapter | Extract interface; keep SQLite implementation. |
| GitHub discovery | `GitHubSource` implements `SkillSource` | Keep. |
| `LocalSource` | `LocalRegistrySource` (same class, documented alias) | Keep. |
| `UnimplementedSource` (enterprise, official docs, package registry) | `MCPRegistrySource`, `OfficialVendorSource`, `EnterpriseRegistrySource`, `AgentDiscoverySource` | Keep fail-closed stubs; do not fake results. |
| `evaluatePublisher` | **Trust Broker** over `TrustProvider` | Wrap; never treat UNKNOWN as VERIFIED. Trust ≠ authorization. |
| `TrustGraph` | Evidence store for the broker | Keep. |
| `verified_cache` table | **VerificationCache** / `CacheProvider` | Promote to core path: cache hit skips rescan. |
| `computeFingerprint` | Gateway fingerprint (publisher + repo + commit + skill files + lock + security config) | Keep algorithm; include file digest explicitly. |
| Planned custom scanners | **SecurityOrchestrator** + `SecurityScanner` adapters | Implement STRIX (never fake PASS), secrets, dependency/SBOM, prompt-injection, suspicious files, license; Snyk/mcp-scan adapter if binary absent → ERROR/NOT_RUN. |
| Planned sandbox | `SandboxProvider` (in-process test + Docker) | Isolated subsystem; missing runtime → INCONCLUSIVE, not PASS. |
| Planned capability policy | Capability firewall (authorization engine) | Independent of Trust Broker. Skill cannot self-grant. |
| Lifecycle `DISCOVERED → UNTRUSTED → VERIFYING → …` | Quarantine-first: `DISCOVERED → QUARANTINED → PROVENANCE_CHECK → SECURITY_SCAN → SANDBOX → POLICY_EVALUATION → APPROVED / REJECTED` | Map legacy states on read; allow old rows. |
| MCP tools (planned list) | Compact gateway tools + progressive disclosure levels 0–3 | Add `get_skill_security`, `get_skill_trust`; keep extra tools that still fit. |
| Job table | Background jobs: discovery, provenance, scan, sandbox, rescan, invalidation; idempotent + fingerprint dedupe | Implement worker on existing `jobs` table. |

## Lifecycle compatibility

| Persisted / old name | Canonical gateway name |
| --- | --- |
| `UNTRUSTED` | `QUARANTINED` (initial hold — not a trust claim) |
| `VERIFYING` | `PROVENANCE_CHECK` |
| `SCANNING` | `SECURITY_SCAN` |
| `SANDBOXING` | `SANDBOX` |
| *(new)* | `POLICY_EVALUATION` (authorization, not trust) |
| `APPROVED` | `APPROVED` (configured checks passed for this fingerprint) |
| `AVAILABLE` | `AVAILABLE` (approved **and** authorized to serve compact skill) |
| `QUARANTINED` / `REJECTED` / `EXPIRED` / `INVALIDATED` | Unchanged meaning |

A skill may be **trusted** (tier VERIFIED/OFFICIAL) and still **not authorized** (`APPROVED` without `AVAILABLE`, or denied capabilities).

## Progressive disclosure (new, required)

| Level | Agent receives |
| --- | --- |
| 0 | Metadata only: name, description, version, trust, security, fingerprint, lifecycle |
| 1 | SKILL.md / compact instructions |
| 2 | Referenced resource **index** (paths + digests), not file bodies |
| 3 | One requested resource body (size-capped) |

Never dump a repository into an MCP response.

## What will not be deleted

- Fingerprinting, SQLite schema, GitHub source, publisher evidence, audit, policy YAML, redaction, DB abstraction.
- Custom manifest schema — it remains the stored card and maps to MCP Skills at the serve boundary.

## What is explicitly out of scope for “replace the ecosystem”

We do **not** reimplement MCP, Agent Skills hosting, or a general malware oracle. We **broker** discovery, trust, security, sandbox, authorization, cache, and compact serve.

## Classification of functionality after migration

| Function | Kind |
| --- | --- |
| MCP stdio / Streamable HTTP tools | **Standards-based** (MCP) |
| SKILL.md + resource digests + levels 0–3 | **Standards-first** (MCP Skills / Agent Skills shaped) |
| Custom `skill.mcp/v1` | **Custom + adapter** (persistence compatibility) |
| GitHub / local / stub registries | **Adapter-based** SkillSources |
| STRIX / Snyk / secrets / SBOM | **Adapter-based** scanners |
| Trust Broker allowlists + graph | **Custom** evidence engine (no numeric safety score) |
| Docker sandbox | **Adapter-based**; in-process evaluator is **test/experimental** and does not execute untrusted code |
| SQLite registry | **Custom** store behind `SkillRegistry` port |

## Incremental implementation order (this upgrade)

1. Migration doc (this file) + lifecycle/type compatibility.
2. MCP Skill adapter + progressive disclosure + tests.
3. Trust Broker / TrustProvider + official-first ranking + tests.
4. SecurityScanner adapters + SecurityOrchestrator + cache + tests (STRIX missing ≠ PASS).
5. SandboxProvider + capability firewall (trust ≠ authz) + tests.
6. Gateway pipeline, jobs, MCP tools, CLI, examples, docs.
7. Security corpus + integration tests. No completion claim without code + tests + docs.
