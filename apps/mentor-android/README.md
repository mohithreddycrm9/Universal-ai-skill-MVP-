# Code Mentor (Android)

Personal coding mentor for mobile: OpenAI-compatible LLM chat, Cursor-style **build suggestion chips**, live speech (STT/TTS), optional **ElevenLabs** cloned voice, **GitHub SKILL.md** imports, encrypted **daily cloud backup**, and **WebSocket** real-time sync.

## Requirements

- Android Studio Ladybug+ (or CLI: JDK 17, Android SDK 35)
- An LLM API key (OpenAI, OpenRouter, or local LM Studio with a compatible base URL)

## Run

1. Open `apps/mentor-android` in Android Studio.
2. Sync Gradle, then **Run** on a device or emulator (API 26+).
3. In **Settings**, set LLM base URL, model, and API key.
4. Optional: set build goal, WebSocket sync URL, and backup PUT URL.

## Features

| Area | Behavior |
| --- | --- |
| Mentor chat | OpenAI-compatible `chat/completions` with system prompt + imported skills |
| Build chips | Heuristics aligned with Universal Skills MCP `get_build_suggestions` signals |
| Voice | Android STT + multilingual TTS; ElevenLabs for cloned voice when configured |
| Skills | Paste a public `github.com/owner/repo` URL; app fetches `SKILL.md` |
| Backup | AES-GCM encrypted snapshot uploaded daily (WorkManager) + manual trigger |
| Sync | Encrypted snapshots pushed over user-provided WebSocket relay |
| Theme | System/light/dark, accent hue, font scale |

## Sync & backup endpoints

The app does not ship a hosted backend. Point **backup** to your object store or API (HTTP `PUT` of ciphertext). Point **sync** to a small relay you control (desktop bridge or cloud worker) that fans out encrypted envelopes to other devices.

## Tests

```bash
cd apps/mentor-android
./gradlew test
```

## Build APK

```bash
./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`
