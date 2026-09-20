# Security model

## Hard gate

Security is a gate, not a score. Terminal security statuses:

- `PASS` — every **required** control completed with scanner status `PASS`, and no **executed** scanner (required or optional) returned `FAIL` / fail-severity findings / soft-fail statuses. Optional scanners that did not run are recorded as `NOT_RUN`, never as pass.
- `FAIL` — any **executed** scanner returned `FAIL`, or produced a finding at/above `failOnSeverity` (required and optional alike).
- `INCONCLUSIVE` — a required control is missing/`NOT_RUN`, or any executed scanner returned `ERROR` / `TIMEOUT` / `INCONCLUSIVE` (and none failed).

`INCONCLUSIVE ≠ PASS`. The gate never upgrades uncertainty.

Forbidden user-facing phrases: “safe”, “completely safe”, “zero risk”, “no malware”, “contains no malware”.

Approved copy: **PASSED_CONFIGURED_CHECKS**, **FAILED**, **INCONCLUSIVE**, **QUARANTINED**.

## Lifecycle enforcement

`DISCOVERED → UNTRUSTED → VERIFYING → SCANNING → SANDBOXING → APPROVED → AVAILABLE`

Illegal transitions are rejected. Security steps cannot be skipped. Executable skills cannot reach `APPROVED` without a sandbox result that is not `FAIL`/`INCONCLUSIVE` when sandboxing is required.

## Default policy (fail closed)

See `config/security-policy.yaml` and `config/trust-policy.yaml`.

- `UNKNOWN` publisher: do not auto-approve.
- Unverified publisher: not trusted.
- Failed scan: reject.
- Inconclusive **required** scan: do not approve.
- Unexpected sandbox privilege: quarantine.
- Credential / production / shell / undeclared network: deny unless policy + user approval.
- Skill packages cannot mutate their own permission set.

## Quality vs security

Quality (`UNKNOWN` / `ACCEPTABLE` / `GOOD`) is stored separately. A “good” skill with `FAIL` or `INCONCLUSIVE` security is never `AVAILABLE`.

## Revalidation

Verified results expire (`security.revalidationHours`). Expired skills become `EXPIRED` and must be refreshed. Fingerprint identity is `(publisher, repository, commit, manifest, lock hash, security configuration)`.

## Secret handling

Detected secrets are redacted (`AKIA…REDACTED`) in findings, logs, and MCP payloads. Secrets are never stored in manifests, audit blobs, or cache entries.

## Host isolation

Untrusted bytes are written under a quarantine directory. Execution, if any, is in a disposable container: no host env, no Docker socket, dropped caps, memory/CPU/PID/time limits, default no network.
