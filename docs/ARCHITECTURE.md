# Architecture — Universal Skill Trust Gateway

See also [ARCHITECTURE-MIGRATION.md](./ARCHITECTURE-MIGRATION.md) for the OLD → NEW map.

```
ANY AI AGENT
    → MCP tools (compact, progressive disclosure)
    → Universal Skill Trust Gateway
        → SkillSource adapters (GitHub, local, MCP registry stubs, …)
        → Trust Broker (evidence; not authorization)
        → SecurityOrchestrator (parallel SecurityScanner adapters)
        → VerificationCache (fingerprint → skip rescan)
        → Quarantine
        → SandboxProvider
        → Capability firewall
    → Serve MCP Skill card (SKILL.md + resource digests)
```

## Standards vs custom

- **Standards-based:** MCP transports and tool schemas; SKILL.md-shaped serve path.
- **Adapter-based:** SkillSource, SecurityScanner, TrustProvider, SandboxProvider, SkillRegistry, CacheProvider.
- **Custom (kept):** SQLite registry, `skill.mcp/v1` persistence, allowlist Trust Broker. Custom manifests adapt to MCP Skills; stored skills are not discarded.

## Pipeline

Newly fetched bytes enter **quarantine**. Identity is publisher + repository + **commit SHA** + skill files + lock hash + security configuration. `latest` / `main` are not identities.

Default policy fails closed. Scanner absence is `ERROR` / `NOT_RUN` / `INCONCLUSIVE`, never `PASS`.
