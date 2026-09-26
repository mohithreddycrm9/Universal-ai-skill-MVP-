# Universal AI (Android)

Personal AI for **any topic** — connect **any LLM** you choose, with premium chat UI, streaming replies, usage dashboard, multi-chat threads, voice, skills, encrypted backup, and real-time sync.

## Run the app

```bash
cd apps/mentor-android
./gradlew :app:assembleDebug
```

Open in Android Studio and run on API 26+.

## Features

| Tab | What it does |
| --- | --- |
| **Chat** | Aurora UI, Fast/Balanced/Deep presets, prompt library, share-to-compose, streaming, drawer (search, pin, rename) |
| **Models** | OpenAI-compatible, **Hugging Face Hub** (search + run via Inference Providers), Anthropic, Google AI, Ollama, custom |
| **Usage** | Requests, tokens, estimated USD (today / 7d / 30d), per-model and daily breakdown |
| **Extensions** | Built-in skill packs, remote catalog, **plugins** (`/calc`, `/time`, …), **MCP** HTTP servers |
| **Settings** | Spend limits (daily/weekly USD), theme, voice, system prompt, sync & backup |

### Extensions (skills, plugins, MCP)

- **Built-in skill packs** — study coach, email, travel, code review, etc. (offline install)
- **Remote catalog** — install instruction packs from public repositories
- **Plugins** — enable in Extensions, then use in chat: `/calc`, `/time`, `/units`, `/uuid`, `/words`, `/fetch` (HTTPS only)
- **MCP** — add an HTTP JSON-RPC endpoint and token; the app lists tools in context. Run: `/mcp YourServerName tool_name {"arg":"value"}`

Popular MCP gateways and hosted servers are documented on the [Model Context Protocol](https://modelcontextprotocol.io) site; point the endpoint URL at your provider’s HTTP bridge.

### Hugging Face models

1. Open **Models** → select **Hugging Face** (built-in) or add a new **Hugging Face** provider.
2. Paste a [Hugging Face access token](https://huggingface.co/settings/tokens) with **Inference Providers** permission.
3. Pick a **featured** model, search the Hub, or type any model id (e.g. `Qwen/Qwen2.5-7B-Instruct:fastest`).
4. Tap **Test connection**, then chat.

The app uses the OpenAI-compatible router at `https://router.huggingface.co/v1/` with streaming. If a model is not on the router, it falls back to the classic serverless inference API when possible.

For **self-hosted** TGI or vLLM with an OpenAI-compatible URL, use **OpenAI-compatible** provider instead and point Base URL at your endpoint.

### Share from other apps

Use **Share** on text in any app and pick **Universal AI**. The text lands in the composer so you can add context and send.

### Spend guardrails

In **Settings → Spend limits**, set daily or weekly USD caps (0 = off). Sends are blocked when estimated usage from the Usage tab exceeds your budget.

## Sync relay (phone ↔ desktop)

```bash
cd apps/sync-relay
npm install
npm start
```

In the app: **Settings → WebSocket sync URL** → `ws://YOUR_LAN_IP:8787/sync`

Payloads are **encrypted on device** before they hit the relay.

## Requirements

- LLM API key(s) for your chosen provider(s)
- Optional: ElevenLabs for cloned voice, backup PUT endpoint

## Tests

```bash
./gradlew :app:testDebugUnitTest
```
