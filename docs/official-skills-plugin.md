# Universal Official Skills

This standalone skills-only plugin selects and reads agent skills from pinned
official GitHub repositories. Its initial sources are ServiceNow's SDK and
OpenAI's current plugin catalog. It does not run the Universal Skill Trust
engine, security scanners, a sandbox, an MCP server, or a database.

The plugin is independently authored by Universal AI Skills. It is not an
official OpenAI or ServiceNow plugin. Only its cataloged upstream sources are
official. Source selection does not guarantee safety or compatibility.

## Use

Ask: "Use Universal Official Skills to find a ServiceNow SDK skill for my task."
The workflow reads the catalog and pinned upstream skill, presents provenance
and requirements, then uses relevant instructions within your authorized scope.
It never silently substitutes community skills. No match produces a clear
no-match result. Missing GitHub/web access produces an unavailable-source result.

Use the existing GitHub connection or a web reader. No custom server is required.
Reading the skill doesn't execute its scripts. A vendor skill may require
Node.js, Xcode, a ServiceNow SDK installation, another account connection, or
other tools that aren't available in a phone-only chat.

## Package and validate

From the repository root:

```bash
python3 -m unittest discover -s tests/plugin -v
python3 scripts/package-official-skills.py
```

The output is `dist/universal-official-skills.zip`. It contains one plugin folder
with `plugin.json`, the routing skill, its source catalog, and this project's
license. The archive excludes the repo's existing MCP server, scanner engine,
Node dependencies, config, data, credentials, and unrelated applications. It
does not redistribute upstream skills; they are read on demand with their own
licenses retained.

## Install or upload

For supported local clients, add this repo marketplace (after the branch is
merged, or add the local checkout) and select **Universal Official Skills**:

```bash
codex plugin marketplace add mohithreddycrm9/Universal-ai-skill-MVP-
# While testing this unmerged PR:
codex plugin marketplace add mohithreddycrm9/Universal-ai-skill-MVP- --ref feat/codex-plugin
```

For ChatGPT web distribution, upload the ZIP through the plugin upload flow
available to your account. Account/workspace access and publishing identity
requirements apply. If you want public directory distribution, complete the
submission checks, review, and publication process. A GitHub PR or downloaded
ZIP does not automatically install a plugin in ChatGPT, and this package does
not require an MCP URL or a hosted service.

## Catalog maintenance

Edit `skills/find-official-skills/references/sources.json` inside the package.
Before adding a repository, establish its publisher ownership and inspect its
README and actual skill directories. Pin an existing 40-character commit SHA.
Update pins through reviewed changes; do not use a moving branch at runtime.
Do not label a skill security-verified: this version deliberately performs no
security scans.

Official packaging/submission references:
- https://developers.openai.com/plugins/build/plugins
- https://developers.openai.com/plugins/deploy/submission
