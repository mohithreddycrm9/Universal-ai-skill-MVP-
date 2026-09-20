# Production deployment

## Recommended shape

- Run `skill-mcp serve` under a supervisor (systemd, Kubernetes, or a dedicated MCP gateway).
- Use **stdio** behind the agent host, or Streamable HTTP bound to loopback / an internal listener only.
- Persist `SKILL_MCP_DATA_DIR` on durable storage.
- Mount `config/` read-only from a secrets-aware config store. Treat policy YAML as security-sensitive.
- Do not give the process host Docker socket access. If container sandboxing is enabled, use a nested/isolated runtime or a dedicated worker pool (see `docker-compose.yml`).
- Set `SKILL_MCP_LOG_LEVEL=info` (JSON on stderr). Ship logs to your SIEM. Metrics are exported on `/metrics` when HTTP is enabled.
- Populate `trust-policy.yaml` official/verified allowlists. Empty lists mean **no publisher is official**.
- Rotate / revalidate: keep `security.revalidationHours` finite.
- PostgreSQL: implement/enable `PostgresAdapter` and set `SKILL_MCP_DATABASE_URL` when you outgrow SQLite. SQLite is acceptable for single-node MCP hosts.

## Hardening checklist

1. Process user is unprivileged.
2. No cloud/production credentials in the MCP environment.
3. Network egress from the MCP process limited to configured SkillSources (e.g. `api.github.com`) plus scanner endpoints.
4. Sandbox workers have **no** credentials and no socket mounts.
5. Backup registry + audit log; audit table is append-only.
6. Never expose HTTP MCP on the public internet without an authenticating proxy.

## What this deployment does **not** claim

A green deploy does not mean acquired skills are universally free of malware or risk. It means the configured gate is in the path.
