import type { Finding } from "../types.js";

export interface HttpProbeTarget {
  url: string;
  timeoutMs?: number;
}

export interface HttpProbeResult {
  url: string;
  status: "PASS" | "FAIL" | "INCONCLUSIVE";
  findings: Finding[];
  notes?: string;
}

const SENSITIVE_PATHS = ["/.env", "/.git/config", "/server-status", "/actuator/health", "/debug", "/.aws/credentials"];

/**
 * Safe, read-only HTTP checks suitable for staging/production monitoring.
 * Does not exploit vulnerabilities or mutate server state beyond normal GETs.
 */
export async function probeHttpSurface(target: HttpProbeTarget): Promise<HttpProbeResult> {
  const timeoutMs = target.timeoutMs ?? 12_000;
  const findings: Finding[] = [];
  let inconclusive = false;

  let base: URL;
  try {
    base = new URL(target.url);
  } catch {
    return {
      url: target.url,
      status: "INCONCLUSIVE",
      findings: [
        {
          id: "probe:bad-url",
          severity: "LOW",
          title: "Invalid probe URL",
          evidence: target.url,
        },
      ],
    };
  }
  if (base.protocol !== "https:" && base.protocol !== "http:") {
    return {
      url: target.url,
      status: "INCONCLUSIVE",
      findings: [
        {
          id: "probe:scheme",
          severity: "LOW",
          title: "Unsupported URL scheme",
          evidence: base.protocol,
        },
      ],
    };
  }
  if (base.protocol === "http:" && !isLocalHost(base.hostname)) {
    findings.push({
      id: "probe:no-tls",
      severity: "MEDIUM",
      title: "Cleartext HTTP for non-localhost target",
      evidence: target.url,
    });
  }

  const root = await safeGet(base.toString(), timeoutMs);
  if (!root.ok) {
    inconclusive = true;
    findings.push({
      id: "probe:unreachable",
      severity: "MEDIUM",
      title: "Root URL not reachable",
      evidence: root.error ?? `HTTP ${root.status}`,
    });
  } else if (root.headers) {
    checkSecurityHeaders(root.headers, findings);
  }

  for (const path of SENSITIVE_PATHS) {
    const probeUrl = new URL(path, base).toString();
    const res = await safeGet(probeUrl, timeoutMs);
    if (res.ok && res.status && res.status >= 200 && res.status < 400) {
      findings.push({
        id: `probe:exposed${path.replace(/\//g, "-")}`,
        severity: "HIGH",
        title: `Sensitive path returned ${res.status}`,
        path,
        evidence: probeUrl,
      });
    }
  }

  const hasHigh = findings.some((f) => f.severity === "HIGH" || f.severity === "CRITICAL");
  const status = hasHigh ? "FAIL" : inconclusive ? "INCONCLUSIVE" : findings.length ? "FAIL" : "PASS";
  return {
    url: target.url,
    status: findings.some((f) => f.severity === "MEDIUM" || f.severity === "HIGH") ? "FAIL" : status,
    findings,
    notes: "Passive HTTP probe only — not a penetration test. Use authorized staging targets.",
  };
}

function isLocalHost(host: string): boolean {
  return host === "localhost" || host === "127.0.0.1" || host === "::1";
}

function checkSecurityHeaders(headers: Headers, findings: Finding[]): void {
  if (!headers.get("strict-transport-security")) {
    findings.push({
      id: "probe:missing-hsts",
      severity: "MEDIUM",
      title: "Missing Strict-Transport-Security header",
      evidence: "HSTS not set on response",
    });
  }
  if (!headers.get("content-security-policy")) {
    findings.push({
      id: "probe:missing-csp",
      severity: "LOW",
      title: "Missing Content-Security-Policy header",
      evidence: "CSP not set on response",
    });
  }
  const xfo = headers.get("x-frame-options");
  if (!xfo && !headers.get("content-security-policy")?.includes("frame-ancestors")) {
    findings.push({
      id: "probe:missing-xfo",
      severity: "LOW",
      title: "Missing clickjacking protection (X-Frame-Options or CSP frame-ancestors)",
      evidence: "No X-Frame-Options",
    });
  }
}

async function safeGet(
  url: string,
  timeoutMs: number,
): Promise<{ ok: boolean; status?: number; headers?: Headers; error?: string }> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const res = await fetch(url, {
      method: "GET",
      redirect: "manual",
      signal: controller.signal,
      headers: { "User-Agent": "skill-mcp-security-watch/1.0 (+authorized monitoring)" },
    });
    return { ok: true, status: res.status, headers: res.headers };
  } catch (error) {
    return { ok: false, error: error instanceof Error ? error.message : "fetch failed" };
  } finally {
    clearTimeout(timer);
  }
}
