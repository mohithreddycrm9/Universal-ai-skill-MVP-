# Local setup (no payment information required)

Core path: clone this repo, install Node 20.11+, run tests, start the MCP. You do **not** need a cloud account, SaaS key, paid scanner, or LLM.

```bash
npm install
npm test
npm run build
npx skill-mcp serve
```

Data defaults to `./data` (gitignored SQLite). Config defaults to `./config`.

Optional **free/OSS** binaries (not required; missing ⇒ `ERROR`/`NOT_RUN`, never `PASS`): STRIX, Semgrep, Gitleaks, Trivy, ClamAV, OSV-Scanner, Syft, Docker/Podman.

Enable them in `config/scanner-policy.yaml` when installed. Commercial scanners stay disabled and cost-gated.

See [local-development.md](./local-development.md) and [COST_POLICY.md](./COST_POLICY.md).
