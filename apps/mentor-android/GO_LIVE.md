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

## 8. Post-launch

- Monitor Play vitals (crashes, ANRs)
- Bump `versionCode` every release; update `versionName` as needed
- CI: `.github/workflows/mentor-android.yml` runs tests, lint, `assembleRelease`
