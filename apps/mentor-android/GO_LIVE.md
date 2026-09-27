# Go live checklist — Universal AI (Play Store)

Use this checklist before publishing **1.0.0** to Google Play.

## 1. Replace placeholder strings

Edit `app/src/main/res/values/strings.xml`:

| String | Purpose |
|--------|---------|
| `support_email` | Play Console contact + in-app About |
| `privacy_policy_url` | Play Console privacy policy URL (host `assets/legal/privacy_policy.html` or your site) |

## 2. Build signed App Bundle (AAB)

```bash
cd apps/mentor-android
export MENTOR_RELEASE_KEYSTORE=/path/to/upload-keystore.jks
export MENTOR_KEYSTORE_PASSWORD=***
export MENTOR_KEY_ALIAS=***
export MENTOR_KEY_PASSWORD=***
./gradlew :app:bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

Upload to **Play Console → Production** or **Internal testing** first.

## 3. Store listing copy

Copy from `store/play/`:

- `title.txt` — 30 chars max
- `short_description.txt` — 80 chars max
- `full_description.txt` — 4000 chars max

Add **screenshots** (phone): Chat, Models connect, Usage, Extensions, Settings.

**Feature graphic**: 1024×500 PNG.

## 4. Data safety (Play Console)

Answers in `store/play/DATA_SAFETY.md`. Summary:

- Data collected: user-provided content (chat) stored **on device**
- Data shared: messages sent to **LLM providers the user configures**
- Encryption: API keys in EncryptedSharedPreferences; optional encrypted backup
- Deletion: **Settings → Erase all local data** or uninstall

## 5. Content rating

Complete IARC questionnaire: no violence; user-generated chat; reference AI/LLM.

## 6. App access

- No login required for the app itself
- Note: users need their own **API keys** for cloud models

## 7. Target API

`targetSdk = 35` (see `app/build.gradle.kts`).

## 8. Staged rollout (recommended)

1. **Internal testing** — team devices, smoke Chat / Models / backup restore.
2. **Closed testing** — 20–50 users; watch vitals for 3–7 days.
3. **Open testing** (optional) — broader feedback before production.
4. **Production** — start at **20%** staged rollout in Play Console; increase to 50% → 100% if crash-free sessions stay above 99%.

Document Ollama LAN setup for testers: `docs/OLLAMA_LAN.md`.

## 9. Post-launch

- Monitor Play vitals (crashes, ANRs)
- Bump `versionCode` every release; update `versionName` as needed
- CI: `.github/workflows/mentor-android.yml` runs **debug** build, unit tests, Paparazzi, and lint on every PR.
- **Signed release AAB** runs only on `main` when GitHub secrets are set:
  - `MENTOR_RELEASE_KEYSTORE_B64` (base64-encoded upload keystore)
  - `MENTOR_KEYSTORE_PASSWORD`, `MENTOR_KEY_ALIAS`, `MENTOR_KEY_PASSWORD`
- Local `./gradlew :app:bundleRelease` **fails** if `MENTOR_RELEASE_KEYSTORE` is unset (no silent unsigned bundles).
