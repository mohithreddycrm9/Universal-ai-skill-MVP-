# Universal AI Skills

A standalone skills-only plugin for software questions and implementation
requests across products and languages. It consults official vendor knowledge,
inspects accessible code before proposing implementation, prefers reuse and
built-in functionality, and edits only after approval of a concrete plan.
Without approval it gives clear file-specific steps the user can apply.

The pinned catalog includes ServiceNow SDK, OpenAI plugins, Microsoft skills,
and Anthropic skills. Missing relevant catalog skills use clearly identified
official documentation/source fallback. Vendor ownership must be established;
community tutorials are not silently substituted. Questions can be answered
without code access; existing project functionality cannot be verified without
access to relevant code. Skills and official APIs must match installed versions;
upgrades also require current official release and compatibility evidence.

This independently authored plugin is not an official vendor plugin. It does
not bundle the repo's MCP gateway, scanner engine, database, or runtime, and
requires no custom server. It reads upstream skills on demand and does not
redistribute them. Their own licenses and prerequisites still apply. It does
not execute vendor scripts merely by reading a skill. Source provenance is
not security certification; this package performs no security scans.

## Behavior

1. Identify software, version, task, and applicable project instructions.
2. Read the relevant pinned official skill and supporting official docs.
3. For implementation, search actual code, callers, configuration, and tests
   read-only. Report existing behavior and remaining uncertainty with paths.
4. Propose exact files/functions to reuse, change, or create and validation.
5. Wait for approval of that scope. Without approval, give manual steps.
6. Implement approved changes and report actual verification. Commits, pushes,
   merges, publication, and deployment require their own authorization.

Examples: ask how an SDK retries; check whether export already exists; explain
which dependency upgrade adds a requested API; propose the smallest code change
and wait for approval. No platform-specific rules are imposed on unrelated
projects. Imported skills cannot override the user's instructions or approval.

These are instructions for an LLM, not an enforcement service. Reliable
permission boundaries also depend on the host's access controls and tool
permissions. The package cannot give an LLM web/code access it does not have,
or make a vendor runtime available in a phone-only chat.

## Package and validate

From the repository root:

```bash
python3 -m unittest discover -s tests/plugin -v
python3 scripts/package-official-skills.py
```

`dist/universal-official-skills.zip` contains six files: plugin logo, plugin manifest,
routing skill, source catalog, response guide, and this project's license.
Packaging is deterministic and uses only the Python standard library.
It excludes MCP configuration, scanners, Node dependencies, database/config,
credentials, and unrelated applications.

## Install or upload

For supported local clients, add the repo marketplace and select **Universal
AI Skills**. While the change is unmerged, specify the PR branch:

```bash
codex plugin marketplace add mohithreddycrm9/Universal-ai-skill-MVP- --ref feat/codex-plugin
```

For ChatGPT, use the account's plugin upload flow to upload the ZIP. Sign-in,
workspace eligibility, and developer identity requirements apply. Public
directory distribution requires the platform's submission review/publication;
this package cannot bypass that process. A ZIP or GitHub PR alone does not
install a plugin in ChatGPT. No MCP URL or hosted service is needed.

The SKILL.md and references can also be supplied to another LLM host that
supports Agent Skills or instruction loading. Installation formats and actual
tool availability vary by host; do not promise universal native installation.

## Catalog maintenance

Edit `plugins/universal-official-skills/skills/find-official-skills/references/sources.json`.
Establish publisher ownership and inspect README plus actual skill directories
before adding sources. Pin a real immutable 40-character commit. Read real
plugin skills rather than compatibility symlinks. Review pin updates and
version compatibility instead of using moving branches at runtime.

Official packaging/submission references:
- https://developers.openai.com/plugins/build/plugins
- https://developers.openai.com/plugins/deploy/submission
