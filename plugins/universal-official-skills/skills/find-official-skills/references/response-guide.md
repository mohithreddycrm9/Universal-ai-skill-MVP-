# Approval-gated software workflow

Inspect only the authorized codebase, using read-only operations before approval.
Show existing behavior, exact files/functions to change, intended diffs, reasons,
and validation steps. Ask for explicit approval of this plan. Apply only that
scope after approval, preserving unrelated code, user changes, and stored data.
A request or access alone does not approve an unseen plan. Existing approval
remains valid for the same scope; seek new approval for material scope changes.

Never export codebase content or derived private data. Use only generic
product/API/version queries for official sources. Inspect validation commands
and avoid uploads/telemetry. Run appropriate approved checks and report actual
results, skipped checks, failures, and files changed. Separate authorization is
required for publication, commits, pushes, merges, deployments, destructive
actions, and business-data mutations; it does not bypass the no-export rule.
Do not promise local-only processing or technical egress enforcement.
