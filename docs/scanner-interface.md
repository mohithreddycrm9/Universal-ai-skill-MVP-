# Scanner interface specification

```ts
interface Scanner {
  readonly id: string;
  readonly version: string;
  scan(target: ScanTarget, configuration: ScannerConfig): Promise<ScannerRun>;
}
```

`ScanTarget` is a local quarantine path plus metadata (commit, publisher, declared files). Scanners must not fetch arbitrary network resources unless their config explicitly allows it (default deny).

## Normalized run

```ts
{
  scannerId: string;
  scannerVersion: string;
  status: "PASS" | "FAIL" | "INCONCLUSIVE" | "ERROR" | "TIMEOUT" | "NOT_RUN";
  startedAt: string;
  finishedAt: string;
  findings: Finding[];  // bounded, secrets redacted
}
```

`Finding`: `{ id, severity: LOW|MEDIUM|HIGH|CRITICAL, title, path?, evidence }`

Evidence is truncated. Secret values are redacted.

## Built-in adapters

| Adapter | Behavior |
| --- | --- |
| `secret` | Pattern detection for keys/tokens/PEMs. Real logic. |
| `dependency` | Lock/manifest parse, compact SBOM, postinstall, typosquat heuristics. |
| `prompt_injection` | Instruction-hijack / exfil / security-disable patterns. |
| `code_health` | Hygiene: lockfiles, committed `.env`, risky APIs (`eval`, weak crypto), missing README. |
| `suspicious_files` | Dangerous names, encoded payloads, destructive/reverse-shell *strings* (fixtures are non-destructive). |
| `license` | License identification vs policy. |
| `strix` | Invokes STRIX if a binary/API is configured. If unavailable: `ERROR`/`NOT_RUN`, **never** `PASS`. |

## Orchestration

Independent scanners run concurrently. The orchestrator records every scanner version into the fingerprint inputs. A required scanner that is `NOT_RUN`, `ERROR`, `TIMEOUT`, or `INCONCLUSIVE` makes the **aggregate** `INCONCLUSIVE` (or `FAIL` if any required scanner `FAIL`s).
