# Threat model

## Assets

- Host filesystem, credentials, and network of the machine running the MCP server
- Model context (prompt injection / instruction override)
- Skill registry integrity (lifecycle, fingerprints, scan results)
- Audit log integrity
- Downstream systems a skill might be authorized to touch

## Actors

| Actor | Assumption |
| --- | --- |
| Agent / MCP client | Semi-trusted. May be tricked by untrusted content. |
| Human operator | Trusted for capability approvals and policy edits. |
| Skill publisher | Untrusted until configured evidence says otherwise. |
| Skill content | Hostile until the gate completes. Includes README, comments, docs, issues, commit messages. |
| Scanners | Trusted for *their own findings*, not for a universal “safe” verdict. |
| Worker / sandbox | Least privilege. Disposable. No host credentials. |

## Trust boundaries

1. MCP tool arguments → application services
2. SkillSource fetch → untrusted bytes on disk (quarantine store)
3. Scanners → normalized findings (never raw repo dump)
4. Sandbox → observed behavior
5. Capability firewall → allow / deny / require user approval
6. Compact skill → model context

## Top threats

1. **Prompt injection** in skill docs instructing the agent to disable the gate, exfiltrate secrets, or escalate tools.
2. **Malicious install scripts** (`preinstall` / `postinstall`, Makefile targets, CI hooks).
3. **Secret leakage** in scanned content echoed back into MCP responses or logs.
4. **Supply-chain substitution**: floating `latest`/`main`, typosquats, dependency confusion.
5. **Self-granted permissions** encoded in a skill manifest.
6. **Scanner unavailability** converted into a false pass.
7. **Sandbox escape** via Docker socket, privileged containers, or host mounts.
8. **Registry integrity**: skipping lifecycle states or rewriting fingerprints.
9. **Token exhaustion / context stuffing** by dumping a repo into the model.
10. **Blocking the user path** on long scans, causing agents to skip verification.

## Mitigations (mapped)

| Threat | Control |
| --- | --- |
| Injection | Treat all fetched bytes as data; dedicated prompt-injection scanner; compact manifests only. |
| Install scripts | Dependency/suspicious-file scanners; sandbox never runs postinstall on the host. |
| Secrets | Secret scanner; redaction in logs/findings/MCP; never persist credentials in manifests. |
| Substitution | Commit pinning; content-addressed fingerprint; lock hash in identity. |
| Self-grant | Capability firewall ignores skill-asserted extra permissions. |
| Scanner down | Status `ERROR`/`INCONCLUSIVE`; gate fails closed. |
| Escape | `--network=none`, `--cap-drop ALL`, no Docker socket, no host creds, resource limits. |
| Integrity | SQLite transactions; immutable fingerprints; audit events. |
| Context stuffing | Hard response size cap; summaries + IDs only. |
| Blocking UX | Async jobs; cache hits; status polling. |

## Residual risk (must be stated)

Passing configured checks is **not** a malware-free or zero-risk claim. Scanners have coverage gaps. Behavioral sandboxing is sampling, not proof. Operators must keep policies conservative and revalidation windows finite.
