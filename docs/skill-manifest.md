# Skill manifest specification

Manifests are the only skill bytes intended for model context. They are compact, schema-validated, and **not an authorization document**.

`apiVersion`: `skill.mcp/v1`

```yaml
apiVersion: skill.mcp/v1
kind: Skill
metadata:
  name: csv-normalize
  description: Compact description of the workflow
  publisher: example-org
  repository: https://github.com/example-org/csv-normalize
  version: "1.2.0"
  license: MIT
  tags: [csv, data]
spec:
  instructions: |
    Short, operator-facing instructions. No repository dumps.
  risk: LOW
  capabilitiesDeclared:
    - filesystem.read
  entrypoints: []          # empty => documentation/workflow skill
  files:                   # content-addressed subset, never a full tree
    - path: README.md
      sha256: …
  dependencies:
    lockHash: sha256:…
```

## Rules

- `capabilitiesDeclared` is **advisory**. The capability firewall decides.
- Extra capabilities asserted in instructions are ignored and may be treated as suspicious.
- `entrypoints` non-empty ⇒ executable skill ⇒ sandbox required before approval.
- Size: instructions ≤ 8 KiB; total manifest JSON ≤ 16 KiB.
- No secrets, tokens, private keys, or `.env` contents.
- Versions are pinned via `commitSha` on the registry record, never `latest`/`main` as identity.

## Fingerprint

```
fingerprint = sha256(canonical({
  publisher, repository, commitSha,
  manifestCanonical, dependencyLockHash, securityConfigurationHash
}))
```

Canonical form: UTF-8 JSON with recursively sorted object keys, no insignificant whitespace variation. Identical inputs always yield the same fingerprint.


## Risk inference

`inferRisk` computes a heuristic from install scripts / entrypoints, then applies any self-declared `risk` from skill YAML:

- **Final risk = max(heuristic, declared)**. Declared risk may **raise** only — never lower a HIGH/CRITICAL heuristic to LOW.
- Malformed declared values are ignored.
- Final HIGH/CRITICAL affects risk reporting; required scanners are configured in `security-policy.yaml` only.
