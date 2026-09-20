# Sandbox specification

Any skill with executable components (`spec.entrypoints` non-empty, install scripts, or binaries) must be sandboxed before `APPROVED`.

## Isolation (container adapter)

- Ephemeral container, destroyed after the run
- `--network=none` unless policy grants declared network (still not the host network)
- `--cap-drop ALL`, `no-new-privileges`
- No Docker socket mount
- No host credential environment variables
- Read-only root where possible; tmpfs for scratch
- CPU / memory / PID / timeout limits

If the container runtime is unavailable, sandbox status is `INCONCLUSIVE` / `ERROR`. That **does not** become `PASS`.

## Behavioral fingerprint

Record: `filesRead`, `filesWritten`, `processes`, `networkConnections`, `environmentAccess`, `secretsAccessed`.

Compare **declared** vs **observed**. Unexpected privileged behavior ⇒ quarantine.

## Test adapter

Unit/integration tests use `InProcessSandbox` which **does not execute** untrusted code. It statically interprets a skill package / fixture behavior file. Malicious fixtures are simulated strings and manifests only.
