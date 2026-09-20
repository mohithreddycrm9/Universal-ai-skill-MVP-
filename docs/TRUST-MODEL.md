# Trust model

See [trust-policy.md](./trust-policy.md). The **Trust Broker** aggregates `TrustProvider` evidence. Tiers: OFFICIAL, VERIFIED, TRUSTED_COMMUNITY, UNKNOWN, UNTRUSTED.

- UNKNOWN is never described as trusted and is never coerced to VERIFIED.
- A vendor-shaped repository name is not evidence.
- **Trust ≠ authorization.** A VERIFIED publisher still cannot obtain `shell.execute` unless the capability firewall allows it.
