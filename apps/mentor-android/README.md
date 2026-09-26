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
| **Chat** | Aurora UI, starter prompts, streaming tokens, conversation drawer |
| **Models** | OpenAI-compatible, Anthropic, Google Generative API, Ollama + custom providers; test connection |
| **Usage** | Requests, tokens, estimated USD (today / 7d / 30d), per-model and daily breakdown |
| **Skills** | Install skill packs from official repository URLs |
| **Settings** | Theme, voice, system prompt, sync & backup |

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
