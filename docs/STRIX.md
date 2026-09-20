# STRIX integration (open source)

This gateway’s STRIX adapter targets the **official open-source project**:

- Repository: [https://github.com/usestrix/strix](https://github.com/usestrix/strix)
- License: Apache-2.0
- PyPI package: `strix-agent`
- CLI entrypoint: `strix`

## What we use vs what we refuse

| Path | Used? | Notes |
| --- | --- | --- |
| Local OSS CLI (`strix --target …`) | Yes (optional) | Adapter invokes this when installed **and** LLM is allowed by cost policy |
| Local Docker sandbox (STRIX’s own) | Required by upstream OSS | Must be running on the operator machine |
| Operator-configured LLM (`STRIX_LLM` / `LLM_API_KEY`) | Required by upstream OSS | **May cost money** unless you use a free/local model |
| `strix cloud` / Strix Cloud / Enterprise | **Never** | Managed/paid platforms — blocked by adapter |

Under this MCP’s default **`ALLOW_FREE_ONLY` ($0)** policy:

- The STRIX **software** is free.
- Your **LLM provider bill** is separate. Before any `strix --target` spawn, the adapter classifies the configured model (env + scanner config) via CostDetector:
  - **Local/free confirmed** (e.g. `ollama` / `lmstudio` / `localhost` / `127.0.0.1`, or `llmIsLocalFree: true` / `SKILL_MCP_STRIX_LLM_IS_FREE=1`) → run allowed
  - **External** (openai, anthropic, openrouter, google, bedrock, vertex, azure, …) → **BLOCK** (`NOT_RUN`; no external request)
  - **Unknown** → **BLOCK**
  - An API key alone is **not** approval; external/unknown + free-only = block
- With `ASK_BEFORE_ANY_PAID_OPERATION`, a non-free LLM needs an explicit approved cost approval (`costApprovalStatus` + `costApprovalId`); there is no auto-paid mode.

Missing binary, Docker, or LLM config → scanner status **`ERROR`**, never **`PASS`**.
Policy LLM blocks → **`NOT_RUN`**, never **`PASS`**.

## Install (operator machine)

Follow upstream quick start (summary):

```bash
# From https://github.com/usestrix/strix
curl -sSL https://strix.ai/install | bash
# or: pipx install strix-agent

# Docker must be running
docker info

# Configure a model (prefer free/local for $0)
export STRIX_LLM="ollama/…"   # see https://docs.strix.ai/llm-providers/overview
export LLM_API_KEY="…"        # omit/empty only if your local provider needs no key
# Or attest: export SKILL_MCP_STRIX_LLM_IS_FREE=1
```

## Gateway behavior

```text
SecurityOrchestrator
  └── StrixScanner (src/scanners/strix.ts)
        ├── strix --version          (local, free)
        ├── docker info              (local, free)
        ├── classify STRIX_LLM / config → CostDetector
        │     ALLOW → continue
        │     BLOCK → NOT_RUN (no --target)
        └── strix --target <quarantinePath>   (only if LLM allowed)
```

Results are **PASSED_CONFIGURED_CHECKS** for that run only — never “universally safe / no malware.”

See also [SCANNER_ADAPTERS.md](./SCANNER_ADAPTERS.md) and [COST_POLICY.md](./COST_POLICY.md).
