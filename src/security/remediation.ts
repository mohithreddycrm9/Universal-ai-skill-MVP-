import { appendFileSync, existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import type { Finding } from "../types.js";

export interface RemediationPlan {
  suggestions: Array<{ id: string; action: string; paths?: string[] }>;
  applied: string[];
  skipped: string[];
}

export function buildRemediationPlan(findings: Finding[]): RemediationPlan {
  const suggestions: RemediationPlan["suggestions"] = [];
  for (const f of findings) {
    if (f.id === "health:missing-lockfile") {
      suggestions.push({
        id: f.id,
        action: "Run your package manager install to generate a lockfile and commit it.",
        paths: ["package.json"],
      });
    }
    if (f.id === "health:env-committed" && f.path) {
      suggestions.push({
        id: f.id,
        action: "Remove secrets from git history, rotate credentials, use a secret manager.",
        paths: [f.path],
      });
    }
    if (f.id.startsWith("health:eval") || f.id === "health:eval") {
      suggestions.push({
        id: f.id,
        action: "Replace eval() with structured parsing or a safe interpreter.",
        paths: f.path ? [f.path] : undefined,
      });
    }
    if (f.id.startsWith("probe:exposed")) {
      suggestions.push({
        id: f.id,
        action: "Block public access to this path at the reverse proxy or application router.",
        paths: f.path ? [f.path] : undefined,
      });
    }
    if (f.id === "probe:missing-hsts") {
      suggestions.push({
        id: f.id,
        action: "Enable HSTS on your TLS terminator (e.g. Strict-Transport-Security max-age=31536000).",
      });
    }
  }
  return { suggestions, applied: [], skipped: [] };
}

/** Applies only non-destructive repo hygiene fixes. Never deletes .env contents automatically. */
export function applySafeRemediation(rootPath: string, findings: Finding[]): RemediationPlan {
  const plan = buildRemediationPlan(findings);
  const envFinding = findings.some((f) => f.id === "health:env-committed");
  if (envFinding) {
    const gitignore = join(rootPath, ".gitignore");
    const lines = [".env", ".env.*", "!.env.example"];
    if (!existsSync(gitignore)) {
      writeFileSync(gitignore, `${lines.join("\n")}\n`, "utf8");
      plan.applied.push("Created .gitignore with .env patterns");
    } else {
      const current = readFileSync(gitignore, "utf8");
      const missing = lines.filter((line) => !current.includes(line));
      if (missing.length) {
        appendFileSync(gitignore, `\n# skill-mcp security-watch\n${missing.join("\n")}\n`);
        plan.applied.push("Appended .env patterns to .gitignore");
      } else {
        plan.skipped.push(".gitignore already ignores .env");
      }
    }
  }
  return plan;
}

export function formatWatchReportMarkdown(payload: {
  at: string;
  path: string;
  url?: string;
  status: string;
  findings: Finding[];
  remediation: RemediationPlan;
}): string {
  const lines = [
    `# Security watch report`,
    ``,
    `- **Time:** ${payload.at}`,
    `- **Path:** ${payload.path}`,
    payload.url ? `- **URL:** ${payload.url}` : "",
    `- **Status:** ${payload.status}`,
    ``,
    `## Findings (${payload.findings.length})`,
  ].filter(Boolean);
  for (const f of payload.findings.slice(0, 100)) {
    lines.push(`- [${f.severity}] ${f.title}${f.path ? ` (\`${f.path}\`)` : ""}`);
  }
  lines.push(``, `## Remediation`);
  for (const s of payload.remediation.suggestions) {
    lines.push(`- ${s.action}`);
  }
  if (payload.remediation.applied.length) {
    lines.push(``, `## Auto-applied (safe)`);
    for (const a of payload.remediation.applied) {
      lines.push(`- ${a}`);
    }
  }
  lines.push(
    ``,
    `_Configured-check outcomes only. INCONCLUSIVE is not PASS. Run authorized penetration tests on staging separately._`,
  );
  return lines.join("\n");
}
