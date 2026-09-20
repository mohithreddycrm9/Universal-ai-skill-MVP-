# Security audit checklist

Use this before calling a release “ready for a security review”. Passing the checklist is not a safety claim.

## Claims and copy

- [ ] No user-facing string says “safe”, “zero risk”, “no malware”, or equivalent.
- [ ] `INCONCLUSIVE` / scanner `ERROR` cannot become `PASS`.
- [ ] STRIX absence is recorded as not-run/error, never pass.

## Gate and lifecycle

- [ ] Illegal lifecycle skips are rejected in tests.
- [ ] Executable skills cannot approve without sandbox when required.
- [ ] Unknown publishers cannot auto-approve under default policy.

## Data handling

- [ ] MCP responses bounded; no repo dumps.
- [ ] Secrets redacted in findings, logs, audit.
- [ ] Manifests reject secret-shaped fields.

## Isolation

- [ ] Sandbox has no Docker socket, no host creds, dropped caps.
- [ ] Malicious fixtures are non-destructive and not executed on the host.

## Permissions

- [ ] Skill cannot add capabilities via manifest/instructions.
- [ ] `request_capability` is policy-backed and audited.

## Supply chain

- [ ] Pins are commit SHAs.
- [ ] Fingerprint includes lock hash + scanner/security config.
- [ ] Cache reuse requires matching fingerprint.

## Tests present

- [ ] Fingerprint determinism
- [ ] Prompt injection fixture quarantined
- [ ] Secret fixture redacted
- [ ] Postinstall fixture fails/quarantines
- [ ] STRIX-missing does not pass that scanner
