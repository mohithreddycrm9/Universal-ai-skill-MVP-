export type ErrorCode =
  | "INVALID_INPUT"
  | "NOT_FOUND"
  | "ILLEGAL_LIFECYCLE"
  | "SECURITY_GATE"
  | "POLICY_DENIED"
  | "SOURCE_ERROR"
  | "TIMEOUT"
  | "INTERNAL"
  | "COST_APPROVAL_REQUIRED";

export class SkillMcpError extends Error {
  readonly code: ErrorCode;
  readonly details: Record<string, unknown>;

  constructor(code: ErrorCode, message: string, details: Record<string, unknown> = {}) {
    super(message);
    this.name = "SkillMcpError";
    this.code = code;
    this.details = details;
  }
}
