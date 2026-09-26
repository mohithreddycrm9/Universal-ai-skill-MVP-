# Universal AI (Android)

Personal AI for **everyday routines** — connect **any LLM** you choose (OpenAI-compatible, Anthropic, Google AI, **Hugging Face Hub**, Ollama), with usage spend transparency, skill packs, and on-device plugins. Positioned for the use cases people actually adopt: **writing**, **study & work**, **meal and trip planning**, and **shopping research**—without locking you to one vendor.

### Why people switch from a single chatbot app

| Trend (2025–2026 consumer AI) | How Universal AI fits |
| --- | --- |
| Writing & rewording messages | Polish message, email, and translate starters + skills |
| School & homework help | Study coach skill + tutor-style prompts |
| Meal & grocery planning | Meal planner skill + workflow cards |
| Compare before buying | Shopping research skill + shop compare prompts |
| Multi-model & privacy | **Bring your own API keys**, HF Hub, local Ollama, usage dashboard |
| Cost control | Spend limits + per-model usage breakdown |

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
| **Models** | Per-provider **Connect** flow: API key, **Google**, **email/password**, or **mobile** (browser sign-in) + paste key; Hugging Face Hub search; Ollama URL |
| **Usage** | Requests, tokens, estimated USD (today / 7d / 30d), per-model and daily breakdown |
| **Extensions** | Built-in skill packs, remote catalog; **built-in tools** `/calc`, `/time`, `/units` (always on) |
| **Settings** | Spend limits (daily/weekly USD), theme, voice, system prompt, sync & backup |

### Extensions (skills & plugins)

- **Built-in skill packs** — study coach, email, travel, code review, etc. (offline install)
- **Remote catalog** — install instruction packs from public repositories
- **Built-in tools** — always in chat: `/calc`, `/time`, `/units`, `/uuid`, `/words` (device time auto-injected when you ask about dates)
- **Optional** — enable `/fetch` in Extensions for HTTPS page text (HTTPS only)

### Connecting an LLM

1. Open **Models** (or tap **Connect** on the Chat banner).
2. Tap **Connect account** on OpenAI, Google AI, Anthropic, Hugging Face, or Ollama.
3. Choose **API key**, **Google**, **Email & password**, or **Mobile number** — browser opens the provider login when needed; paste your API key or token on return.
4. Tap **Save & use this provider**, then chat.

Optional: set `google_web_client_id` in `res/values/strings.xml` to enable on-device Google sign-in when connecting Google AI (OAuth web client ID).

### Hugging Face models

1. Connect **Hugging Face** with a [token](https://huggingface.co/settings/tokens) (**Inference Providers** permission).
2. Pick a **featured** model, search the Hub, or type any model id (e.g. `Qwen/Qwen2.5-7B-Instruct:fastest`).

The app uses the OpenAI-compatible router at `https://router.huggingface.co/v1/` with streaming. If a model is not on the router, it falls back to the classic serverless inference API when possible.

For **self-hosted** TGI or vLLM with an OpenAI-compatible URL, use **OpenAI-compatible** provider instead and point Base URL at your endpoint.

### Nice extras

- **Home screen widget** — Morning brief, meal plan, open chat
- **App shortcuts** — long-press launcher icon for quick workflows
- **Deep links** — `universalai://usecase/daily-brief` (and other use case ids)
- **Welcome tour** — first launch overview
- **Biometric lock** — optional in Settings
- **Daily brief notification** — optional reminder (enable in Settings)
- **Smarter Share** — shared URLs get a summarize prompt in chat

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

## Go live

See **[GO_LIVE.md](GO_LIVE.md)** for the full Play Store checklist, signed AAB commands, and store listing files under `store/play/`.

Before publishing, update `support_email` and `privacy_policy_url` in `app/src/main/res/values/strings.xml`.

## Production / Play Store

- **Version** `1.0.0` (see `app/build.gradle.kts`)
- **Release signing** — set env vars `MENTOR_RELEASE_KEYSTORE`, `MENTOR_KEYSTORE_PASSWORD`, `MENTOR_KEY_ALIAS`, `MENTOR_KEY_PASSWORD` before `assembleRelease`
- **CI** — GitHub Actions workflow `mentor-android.yml` runs unit tests, lint, and release assemble
- **Privacy** — in-app policy under Settings; bundled at `app/src/main/assets/legal/privacy_policy.html`
- **Secrets** — LLM keys, ElevenLabs, and backup tokens in EncryptedSharedPreferences; backup disabled by default
- **Network** — HTTPS by default; cleartext allowed for `localhost` / emulator Ollama; debug builds allow LAN HTTP for local testing
- **Ollama on LAN (release)** — use HTTPS reverse proxy or a debug build for `http://192.168.x.x`

## Requirements

- LLM API key(s) for your chosen provider(s)
- Optional: ElevenLabs for cloned voice, backup PUT endpoint

## Tests

```bash
./gradlew :app:testDebugUnitTest
```
