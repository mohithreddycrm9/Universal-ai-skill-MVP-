# STRIX integration (open source)

This gateway’s STRIX adapter targets the **official open-source project**:

- Repository: [https://github.com/usestrix/strix](https://github.com/usestrix/strix)
- License: Apache-2.0
- PyPI package: `strix-agent`
- CLI entrypoint: `strix`

## What we use vs what we refuse

| Path | Used? | Notes |
| --- | --- | --- |
| Local OSS CLI (`strix --target …`) | Yes (optional) | Adapter invokes this when installed |
| Local Docker sandbox (STRIX’s own) | Required by upstream OSS | Must be running on the operator machine |
| Operator-configured LLM (`STRIX_LLM` / `LLM_API_KEY`) | Required by upstream OSS | **May cost money** unless you use a free/local model |
| `strix cloud` / Strix Cloud / Enterprise | **Never** | Managed/paid platforms — blocked by adapter |

Under this MCP’s default **`ALLOW_FREE_ONLY` ($0)** policy:

- The STRIX **software** is free.
- Your **LLM provider bill** is separate. For true $0, point `STRIX_LLM` at a free/local model (or leave STRIX unset and rely on built-in free scanners).

Missing binary, Docker, or LLM config → scanner status **`ERROR`**, never **`PASS`**.

## Install (operator machine)

Follow upstream quick start (summary):

```bash
# From https://github.com/usestrix/strix
curl -sSL https://strix.ai/install | bash
# or: pipx install strix-agent

# Docker must be running
docker info

# Configure a model (prefer free/local for $0)
export STRIX_LLM="..."          # see https://docs.strix.ai/llm-providers/overview
export LLM_API_KEY="..."        # omit/empty only if your local provider needs no key
```

## Gateway behavior

```text
SecurityOrchestrator
  └── StrixScanner (src/scanners/strix.ts)
        ├── strix --version
        ├── docker info
        ├── require STRIX_LLM or LLM_API_KEY
        └── strix --target <quarantinePath>
```

Results are **PASSED_CONFIGURED_CHECKS** for that run only — never “universally safe / no malware.”

See also [SCANNER_ADAPTERS.md](./SCANNER_ADAPTERS.md) and [COST_POLICY.md](./COST_POLICY.md).
