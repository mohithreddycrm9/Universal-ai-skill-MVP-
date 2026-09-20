# Security model

## Hard gate

Security is a gate, not a score. Terminal security statuses:

- `PASS` — every **required** control completed with scanner status `PASS`. Optional scanners that did not run are recorded as `NOT_RUN`, never as pass.
- `FAIL` — at least one required control failed, or a critical finding exists.
- `INCONCLUSIVE` — a required control did not produce a decisive result (`ERROR`, `TIMEOUT`, missing binary, sandbox unavailable).

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
