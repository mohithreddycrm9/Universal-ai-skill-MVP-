# Trust policy specification

Publisher trust is **evidence**, not a name match. A repository whose path contains a well-known vendor string is **not** automatically official.

## Tiers

| Tier | Meaning |
| --- | --- |
| `OFFICIAL` | Publisher matches a configured official organization **and** additional ownership evidence (allowlisted org login, documented official domain, or signed release policy). |
| `VERIFIED` | Configured verified publisher / signed provenance accepted by policy. |
| `TRUSTED_COMMUNITY` | Explicit community allowlist. |
| `UNKNOWN` | Default. Not trusted. |
| `UNTRUSTED` | Negative evidence (malicious list, archived+abandoned per policy, spoof indicators). |

`UNKNOWN` must never be described as trusted.

## Evidence stored on the trust graph

`publisher → organization → repository → release → commit → skill → fingerprint`

Each edge carries: type, source, recorded-at, notes. There is no single numeric trust score.

## Pinning

`verify_skill` resolves `HEAD`/`tag` to an immutable commit SHA. Registry identity uses that SHA. Refresh creates a new fingerprint; it does not mutate the old one.

## Default (config/trust-policy.yaml)

- Empty official/verified lists at ship time (operators must populate).
- Unknown publishers: acquisition may proceed to scan only if `allowUnknownPublisherScan: true`; approval still denied unless policy is explicitly loosened (not the default).
- Archived repositories: `UNTRUSTED`.
