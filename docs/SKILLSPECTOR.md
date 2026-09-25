# NVIDIA SkillSpector (optional, extended mode)

SkillSpector is **not registered** unless `extendedScanningEnabled: true` in `security-policy.yaml` (see [MARKET_READY.md](./MARKET_READY.md)).

- Static only: `skillspector scan <path> --no-llm --format json`
- No LLM / semantic mode in this MCP

Install:

```bash
pip install git+https://github.com/NVIDIA/skillspector.git
```

Enable in `scanner-policy.yaml`:

```yaml
skillspector:
  enabled: true
  binary: skillspector
```
