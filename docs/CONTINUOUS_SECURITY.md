# Continuous security (real-world, not Cursor-only)

Run **ongoing** checks the way an attacker would *look* at your surface area—then fix what you can safely automate. This uses the same OSS-first engine as the MCP server but is meant for **CI, cron, Docker, and Kubernetes**, not only IDE agents.

## What runs

| Layer | Tool | Notes |
| --- | --- | --- |
| Code & secrets | `security-watch` → built-in scanners + `code_health` | Local files only; bounded walk |
| Optional SAST/secrets/CVE | `--oss-cli` | Semgrep, Gitleaks, Trivy, OSV when installed |
| Live app surface | `--url https://staging.example` | **GET-only** probes: security headers, common exposed paths |
| Safe auto-fix | `--apply-safe` | e.g. append `.env` to `.gitignore` — does **not** delete secrets or rotate keys |

For **active** penetration testing (fuzzing, authenticated flows), use authorized tools on **staging**—e.g. [STRIX](STRIX.md), OWASP ZAP, or your red-team suite. Wire STRIX through skill acquisition or your own pipeline; do not point automated exploits at production without written approval.

## One-shot (CI exit codes)

```bash
npm run build
npx skill-mcp security-watch . --report-dir ./security-reports --json
```

| Exit code | Meaning |
| --- | --- |
| 0 | `PASS` (no findings at configured severities) |
| 1 | `INCONCLUSIVE` (scanner errors, unreachable URL, etc.) |
| 2 | `FAIL` (HIGH/CRITICAL or policy failures) |

GitHub Actions: [.github/workflows/continuous-security.yml](../.github/workflows/continuous-security.yml). Set repo variable `SECURITY_WATCH_URL` to your **staging** base URL for HTTP probes.

## Daemon (cron / Docker)

```bash
# Every hour, write reports and apply safe .gitignore fixes
npx skill-mcp security-watch /var/www/myapp \
  --url https://staging.myapp.example \
  --interval 3600 \
  --report-dir /var/log/security-watch \
  --apply-safe
```

Docker Compose (optional profile):

```bash
docker compose --profile security up security-watch
```

Build the watch image after `npm run build`:

```bash
docker build -f deploy/security-watch.Dockerfile -t skill-mcp-security-watch .
docker run --rm -v "$(pwd):/app:ro" -v "$(pwd)/reports:/reports" \
  skill-mcp-security-watch /app --report-dir /reports
```

## Kubernetes (CronJob sketch)

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: security-watch
spec:
  schedule: "0 */6 * * *"
  jobTemplate:
    spec:
      template:
        spec:
          containers:
            - name: watch
              image: your-registry/skill-mcp-security-watch:latest
              args:
                - /src
                - --url
                - https://staging.example
                - --report-dir
                - /reports
              volumeMounts:
                - name: src
                  mountPath: /src
                  readOnly: true
                - name: reports
                  mountPath: /reports
          restartPolicy: OnFailure
          volumes:
            - name: src
              persistentVolumeClaim:
                claimName: app-source
            - name: reports
              persistentVolumeClaim:
                claimName: security-reports
```

## Fix loop (human + automation)

1. **Detect** — `security-watch` produces JSON + Markdown under `--report-dir`.
2. **Triage** — treat `INCONCLUSIVE` as open work, not green.
3. **Remediate** — use the report’s remediation section; run `--apply-safe` only on repos you control.
4. **Verify** — re-run watch; for apps, deploy to staging and re-probe `--url`.
5. **Deep test** — scheduled STRIX/ZAP against staging with cost/approval policy from [COST_POLICY.md](COST_POLICY.md).

## MCP (optional)

Cursor and other MCP clients can call `run_code_health_check` for ad-hoc scans. Continuous production monitoring should prefer CLI/Docker/CI above so it does not depend on an IDE session.

See also: [CODE_HEALTH_AGENT.md](CODE_HEALTH_AGENT.md).
