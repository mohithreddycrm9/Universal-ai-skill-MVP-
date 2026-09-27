---
name: openhands-agent-canvas
description: Develop on OpenHands Agent Canvas — read AGENTS.md, apply repo-owned contributor skills under .agents/skills/, and run the standard verification commands before finishing.
---

# OpenHands Agent Canvas (contributor skill)

Use this skill whenever the user wants work on [OpenHands/OpenHands](https://github.com/OpenHands/OpenHands.git) (Agent Canvas UI and local stack), not ad-hoc guesses about the codebase.

## Repository location

- Prefer a local clone at `/workspace/OpenHands` (fetch `main` before large or release-sensitive changes).
- If the clone is missing: `git clone https://github.com/OpenHands/OpenHands.git /workspace/OpenHands`

## Required reading order

1. **`/workspace/OpenHands/AGENTS.md`** — ownership boundaries, PR rules, testing baseline, skill index.
2. **Every applicable skill below** — read each matching `SKILL.md` and its `references/guide.md` before editing that area.

| Skill | Path | Use when |
| --- | --- | --- |
| telemetry-analytics | `.agents/skills/telemetry-analytics/` | PostHog, telemetry, onboarding instrumentation |
| e2e-testing | `.agents/skills/e2e-testing/` | Playwright, mock-LLM E2E, Docker E2E, CI artifacts |
| frontend-api-contracts | `.agents/skills/frontend-api-contracts/` | `src/api`, Agent Server contracts, backends, auth |
| local-stack-runtime | `.agents/skills/local-stack-runtime/` | Dev launchers, Docker, ingress, `config/defaults.json` |
| desktop-electron | `.agents/skills/desktop-electron/` | Electron packaging and desktop CI |
| frontend-development | `.agents/skills/frontend-development/` | React UI, i18n, MSW, bundle performance |
| pr-design-doc | `.agents/skills/pr-design-doc/` | PR body design document |
| release | `.agents/skills/release.md` | `@openhands/agent-canvas` releases |

Also follow **`.agents/skills/custom-codereview-guide.md`** for every PR.

## Ownership (do not implement in the wrong repo)

| Repo | Owns |
| --- | --- |
| OpenHands/OpenHands | Canvas UI, frontend state, backend selection, local-stack orchestration |
| OpenHands/software-agent-sdk | Agent Server, Python SDK, REST/WebSocket API |
| OpenHands/extensions | Public skills, automations, plugins |
| OpenHands/automation | Automation scheduling and webhooks |

Do not reimplement Agent Server endpoints in Canvas; consume the typed client contract.

## Verification (before claiming done)

From `/workspace/OpenHands`:

```bash
npm ci
npm run lint
npm test
npm run build
```

Use `npm run build:lib` when changing library publish surfaces. Use the **e2e-testing** skill to pick Playwright suites when behavior needs E2E proof.

## PR constraints

- Dependencies are exact-pinned; update `package.json` and `package-lock.json` together via npm.
- Do **not** edit the `HUMAN:` section in PR descriptions (human-only).
- Tag specs with `// @spec …` per `AGENTS.md` when touching spec-covered behavior.

## Universal Skill Trust (optional)

To discover other third-party skills safely, use the **universal-skill-trust** skill and MCP tools (`discover_skill`, `acquire_skill`, `get_skill`). That is separate from OpenHands’s built-in `.agents/skills/` contributor guides.
