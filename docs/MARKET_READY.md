# Extended scanning profile (optional)

The **default** MCP is [organization-approved](./ORG_APPROVED.md) (built-in scanners only).

This profile turns on **optional OSS adapters** after your security team approves external CLIs:

- NVIDIA SkillSpector (static `--no-llm` only)
- Semgrep, Gitleaks (optional)

```bash
npm ci && npm run build
./scripts/use-market-ready-config.sh ./config-active
export SKILL_MCP_CONFIG_DIR=./config-active
./scripts/check-market-ready.sh
npx skill-mcp serve
```

Sets `extendedScanningEnabled: true` in `security-policy.yaml`.
