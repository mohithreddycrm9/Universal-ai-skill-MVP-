# Skill lifecycle

Canonical: `DISCOVERED → QUARANTINED → PROVENANCE_CHECK → SECURITY_SCAN → SANDBOX → POLICY_EVALUATION → APPROVED / REJECTED` then `AVAILABLE` only when authorized to serve.

Legacy persisted names (`UNTRUSTED`, `VERIFYING`, `SCANNING`, `SANDBOXING`) are normalized on read. See [ARCHITECTURE-MIGRATION.md](./ARCHITECTURE-MIGRATION.md).

A new commit is a **new artifact**. The previous fingerprint stays available if policy still permits it (anti-rug-pull).
