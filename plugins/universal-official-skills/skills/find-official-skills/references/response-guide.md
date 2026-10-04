# Software answers and implementation proposals

Keep answers proportional to the task. State the finding first, then support
it with official sources and inspected code. Do not invent paths or symbols.

## Software question

Explain behavior for the identified version. Cite the official skill and docs
used. If no relevant cataloged skill exists, explicitly identify official docs
as the fallback. If access fails, report the evidence gap instead of claiming
verification.

## Code inspection and proposal

1. Existing behavior: what code/platform already does, with exact file and
   symbol references and how callers/configuration expose it.
2. Recommended approach: use existing behavior, configure it, extend it,
   upgrade a dependency, or create missing functionality; explain why.
3. Concrete changes: use a compact table for multiple files:

   | File / symbol | Action | Proposed change and reason |
   | --- | --- | --- |
   | Actual inspected path | Reuse / modify / create | Specific behavior |

4. Validation: checks demonstrating requested and existing behavior; mention
   compatibility/migration effects when relevant.
5. Approval: ask whether to apply this specific proposal. Without approval,
   provide numbered manual steps without writing files.

## Example decisions

- An export request finds an existing export route and working caller: explain
  how to use it. If only the UI link is missing, propose adding that link
  instead of creating another export service.
- An API exists only in a newer official release: explain installed version,
  upgrade target, and migration requirements before proposing dependency and
  lockfile edits. Do not upgrade automatically.
- A question about SDK retries: answer from official skill/docs without asking
  for repository access or editing code.
- A user declines changes: name actual files/functions to edit, show intended
  snippets/diffs, and give verification steps. Do not apply them.
- Approval of the previously shown proposal: apply that scope and report checks.
  Do not push or deploy unless separately authorized.

## Completion

Report outcome, code reused/changed, actual validation, and material limitations.
Never claim deployment or plugin installation from producing a ZIP, opening a
PR, or starting a submission alone.
