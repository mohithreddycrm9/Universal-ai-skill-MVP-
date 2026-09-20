const SENSITIVE = /(token|secret|password|passwd|authorization|api[_-]?key|private[_-]?key|cookie|session)/i;

export function redactValue(value: string): string {
  if (value.length <= 8) {
    return "[REDACTED]";
  }
  return `${value.slice(0, 4)}…[REDACTED]`;
}

export function redactDeep(value: unknown): unknown {
  if (typeof value === "string") {
    if (SENSITIVE.test(value) || looksLikeSecret(value)) {
      return redactValue(value);
    }
    return value;
  }
  if (Array.isArray(value)) {
    return value.map(redactDeep);
  }
  if (value && typeof value === "object") {
    const out: Record<string, unknown> = {};
    for (const [key, item] of Object.entries(value as Record<string, unknown>)) {
      out[key] = SENSITIVE.test(key) ? "[REDACTED]" : redactDeep(item);
    }
    return out;
  }
  return value;
}

export function looksLikeSecret(value: string): boolean {
  return (
    /AKIA[0-9A-Z]{16}/.test(value) ||
    /ghp_[A-Za-z0-9]{20,}/.test(value) ||
    /github_pat_[A-Za-z0-9_]{20,}/.test(value) ||
    /-----BEGIN [A-Z ]*PRIVATE KEY-----/.test(value) ||
    /xox[baprs]-[A-Za-z0-9-]{10,}/.test(value)
  );
}
