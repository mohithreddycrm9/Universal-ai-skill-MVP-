# Google Play Data safety — suggested answers

Use as a guide when filling the Play Console form. Adjust if your hosted backup/sync differs.

## Data collection

| Data type | Collected? | Shared? | Purpose | Optional? |
|-----------|------------|---------|---------|-----------|
| Messages / chat content | Yes (on device) | Yes (to user-selected LLM APIs) | App functionality | No (core feature) |
| Email (linked account label) | Optional | No | Account management | Yes |
| API keys / tokens | Yes (encrypted on device) | No (sent only as auth headers to providers) | App functionality | Yes |
| App interactions (usage stats) | Yes (on device) | No | Analytics / budgets | No |
| Crash logs | Only if you add Crashlytics later | — | — | — |

## Security practices

- Data encrypted in transit: **Yes** (HTTPS to providers)
- Data encrypted at rest: **Yes** (EncryptedSharedPreferences for secrets; Room for chats)
- Users can request deletion: **Yes** (Settings → Erase all local data; uninstall)

## Data types NOT collected by the developer

- Location
- Contacts
- Photos (unless user shares text via Share intent)
- Financial info (app shows estimated USD only; no payment processing)

## Third parties

Declare that data is shared with **LLM providers the user configures** (OpenAI, Anthropic, Google, Hugging Face, self-hosted endpoints).
