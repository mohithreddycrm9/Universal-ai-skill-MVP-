# Security

The canonical model is [security-model.md](./security-model.md). Gateway additions:

- **SecurityOrchestrator** federates pluggable scanners; raw logs are not sent to the LLM (`get_skill_security` is a summary).
- **OSS CLI** and **Snyk/mcp-scan** adapters never fake PASS when the binary is missing.
- **VerificationCache** reuses only `PASS` + unexpired fingerprints.
- User-facing claims: `PASSED_CONFIGURED_CHECKS` / `FAILED` / `INCONCLUSIVE` / `QUARANTINED` only.
