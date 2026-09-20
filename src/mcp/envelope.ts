import { SkillMcpError } from "../errors.js";
import { SECURITY_NOTICE } from "../types.js";
import { redactDeep } from "../util/redact.js";

export interface Envelope {
  ok: boolean;
  requestId: string;
  data?: unknown;
  error?: { code: string; message: string };
  warnings: string[];
  securityNotice: string;
}

export function encodeEnvelope(
  requestId: string,
  data: unknown,
  maxBytes: number,
  warnings: string[] = [],
): Envelope {
  const redacted = redactDeep(data);
  const envelope: Envelope = {
    ok: true,
    requestId,
    data: redacted,
    warnings,
    securityNotice: SECURITY_NOTICE,
  };
  let json = JSON.stringify(envelope);
  if (json.length <= maxBytes) {
    return envelope;
  }
  envelope.data = { truncated: true };
  envelope.warnings = [...warnings, "response_truncated"];
  json = JSON.stringify(envelope);
  if (json.length > maxBytes) {
    envelope.warnings = ["response_truncated"];
    envelope.data = undefined;
  }
  return envelope;
}

export function encodeError(requestId: string, error: unknown): Envelope {
  if (error instanceof SkillMcpError) {
    return {
      ok: false,
      requestId,
      error: { code: error.code, message: error.message },
      warnings: [],
      securityNotice: SECURITY_NOTICE,
    };
  }
  return {
    ok: false,
    requestId,
    error: { code: "INTERNAL", message: error instanceof Error ? error.message : "internal error" },
    warnings: [],
    securityNotice: SECURITY_NOTICE,
  };
}
