/**
 * Human approval records.
 *
 * TRUSTED CHANNEL (only path that may set status=APPROVED):
 *   CLI interactive `skill-mcp approve <id>` with an explicit y/N confirmation
 *   on a local operator TTY. That path sets method=local_interactive and never
 *   runs inside an MCP tool handler the model can invoke.
 *
 * UNTRUSTED (never authorizes):
 *   MCP tool arguments such as approver:"human", humanApproved:true, or any
 *   caller-supplied string/boolean. Those may be logged as claims only.
 */

export const APPROVAL_STATUSES = ["PENDING", "APPROVED", "REJECTED", "EXPIRED", "CONSUMED"] as const;
export type ApprovalStatus = (typeof APPROVAL_STATUSES)[number];

export const APPROVAL_METHODS = ["local_interactive"] as const;
export type ApprovalMethod = (typeof APPROVAL_METHODS)[number];

export const APPROVAL_KINDS = ["capability", "cost"] as const;
export type ApprovalKind = (typeof APPROVAL_KINDS)[number];

/** Proof the firewall may trust — never a raw MCP approver string. */
export interface TrustedApprovalDecision {
  approvalId: string;
  method: ApprovalMethod;
}

export interface CapabilityApproval {
  id: string;
  skillId: string;
  repository: string;
  commitSha: string;
  fingerprint: string;
  capability: string;
  status: ApprovalStatus;
  method: ApprovalMethod | null;
  expiresAt: string;
  approvedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export const DEFAULT_APPROVAL_TTL_MS = 30 * 60 * 1000;
