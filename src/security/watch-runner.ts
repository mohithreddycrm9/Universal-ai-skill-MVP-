import { mkdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import type { SkillTrustGateway } from "../gateway.js";
import { probeHttpSurface } from "./http-probe.js";
import {
  applySafeRemediation,
  buildRemediationPlan,
  formatWatchReportMarkdown,
} from "./remediation.js";
import type { Finding } from "../types.js";
import { iso, systemClock } from "../util/clock.js";

export interface SecurityWatchOptions {
  path: string;
  url?: string;
  includeOssCli?: boolean;
  applySafeFixes?: boolean;
  reportDir?: string;
  requestId: string;
}

export interface SecurityWatchResult {
  at: string;
  path: string;
  url?: string;
  workspaceStatus: string;
  httpStatus?: string;
  aggregateStatus: string;
  findings: Finding[];
  remediation: ReturnType<typeof buildRemediationPlan>;
  reportPaths?: { json: string; markdown: string };
}

export async function runSecurityWatch(
  gateway: SkillTrustGateway,
  options: SecurityWatchOptions,
): Promise<SecurityWatchResult> {
  const at = iso(systemClock);
  const workspace = (await gateway.runCodeHealthCheck({
    path: options.path,
    requestId: options.requestId,
    includeOssCli: options.includeOssCli,
  })) as {
    path: string;
    status: string;
    scanners: Array<{ findings: Finding[] }>;
  };

  const findings: Finding[] = [];
  for (const scanner of workspace.scanners) {
    findings.push(...scanner.findings);
  }

  let httpStatus: string | undefined;
  if (options.url) {
    const http = await probeHttpSurface({ url: options.url });
    httpStatus = http.status;
    findings.push(
      ...http.findings.map((f) => ({
        ...f,
        id: f.id.startsWith("probe:") ? f.id : `probe:${f.id}`,
      })),
    );
  }

  const remediation = options.applySafeFixes
    ? applySafeRemediation(workspace.path, findings)
    : buildRemediationPlan(findings);

  const fail = findings.some((f) => f.severity === "HIGH" || f.severity === "CRITICAL");
  const inconclusive = workspace.status === "INCONCLUSIVE" || httpStatus === "INCONCLUSIVE";
  const aggregateStatus = fail ? "FAIL" : inconclusive ? "INCONCLUSIVE" : findings.length ? "FAIL" : "PASS";

  const result: SecurityWatchResult = {
    at,
    path: workspace.path,
    url: options.url,
    workspaceStatus: workspace.status,
    httpStatus,
    aggregateStatus,
    findings,
    remediation,
  };

  if (options.reportDir) {
    mkdirSync(options.reportDir, { recursive: true });
    const stamp = at.replace(/[:.]/g, "-");
    const jsonPath = join(options.reportDir, `security-watch-${stamp}.json`);
    const mdPath = join(options.reportDir, `security-watch-${stamp}.md`);
    writeFileSync(jsonPath, JSON.stringify(result, null, 2), "utf8");
    writeFileSync(
      mdPath,
      formatWatchReportMarkdown({
        at,
        path: workspace.path,
        url: options.url,
        status: aggregateStatus,
        findings,
        remediation,
      }),
      "utf8",
    );
    result.reportPaths = { json: jsonPath, markdown: mdPath };
  }

  return result;
}
