# Claude plugin directory submission

Submit to the **community** plugin directory (after review, plugins are cataloged in `anthropics/claude-plugins-community`). This is **not** the curated `claude-plugins-official` marketplace; Anthropic selects that separately.

Official guide: [Submitting your plugin](https://claude.com/docs/plugins/submit)

## Before you submit

```bash
npm run validate:claude-plugin
```

Expect: `✔ Validation passed`

Repo must be **public** on GitHub.

## Submission forms (sign in required)

| Who | URL |
| --- | --- |
| **Individual / Console org** (recommended for solo authors) | [platform.claude.com/plugins/submit](https://platform.claude.com/plugins/submit) |
| **claude.ai Team / Enterprise** (directory admin) | [claude.ai/admin-settings/directory/submissions/plugins/new](https://claude.ai/admin-settings/directory/submissions/plugins/new) |

You need a **Developer, Admin, or Owner** role on a Console organization, or directory access on claude.ai Team/Enterprise.

## Copy-paste listing

| Field | Value |
| --- | --- |
| Plugin name | `universal-skill-trust` |
| Display name | Universal Skill Trust Gateway |
| Version | `0.1.0` |
| License | Apache-2.0 |
| **GitHub repository** | `https://github.com/mohithreddycrm9/Universal-ai-skill-MVP-` |
| Homepage | `https://github.com/mohithreddycrm9/Universal-ai-skill-MVP-` |
| Logo URL | `https://raw.githubusercontent.com/mohithreddycrm9/Universal-ai-skill-MVP-/main/assets/logo.svg` |

### Short description

```text
Discover, acquire, and serve third-party agent skills safely. MCP gateway with OSS-first scanning, quarantine lifecycle, and compact skill delivery.
```

### Standard description

```text
Universal Skill Trust Gateway is an MCP server for Claude Code that helps agents find, acquire, verify, and load third-party skills without dumping full repositories into context.

Workflow: discover candidates → async acquire → security scans and static sandbox checks → capability firewall → serve compact SKILL manifests with progressive disclosure.

Built free/OSS-first (local SQLite, local scanners, optional Docker). Paid or unknown-cost tools require explicit human approval. Trust evidence is separate from authorization; INCONCLUSIVE is not PASS. Static-only sandbox stage—skill entrypoints are not executed on the host during verification.

Bundles MCP (.mcp.json), skill (universal-skill-trust), and policy-driven verification. Does not run Terraform, AWS, or other product APIs.
```

## After approval

Users install from the community marketplace:

```text
/plugin marketplace add anthropics/claude-plugins-community
/plugin install universal-skill-trust@claude-community
```

Catalog sync can lag after approval (automated pin + nightly/weekly sync). Your own repo marketplace still works immediately:

```text
/plugin marketplace add mohithreddycrm9/Universal-ai-skill-MVP-
/plugin install universal-skill-trust@universal-skill-trust
```

## Updates

Push to `main` on GitHub after publish — screening runs on updates; you do **not** re-submit the form for each commit.

## One-tap email (optional)

If the form is unclear, you can ask for routing help via Console support; there is no public email submit API. For reference, plugin template docs mention contacting Anthropic for edge cases.
