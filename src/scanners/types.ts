import type { ScanTarget, ScannerRun } from "../types.js";
import type { CostMetadata } from "../cost/types.js";

export interface ScannerConfig {
  [key: string]: unknown;
  enabled?: boolean;
  timeoutMs?: number;
  failOpen?: boolean;
  binary?: string;
  /** Injected by orchestrator — used by STRIX LLM cost gate. */
  costPolicy?: unknown;
  /** Explicit local/free LLM attestation for STRIX. */
  llmIsLocalFree?: boolean;
  localOnly?: boolean;
  model?: string;
  provider?: string;
  /** When ASK_BEFORE…: APPROVED + costApprovalId allows a non-free STRIX LLM. */
  costApprovalStatus?: "APPROVED" | "REJECTED" | "PENDING";
  costApprovalId?: string;
}

export interface SecurityScanner {
  readonly id: string;
  readonly version: string;
  readonly cost: CostMetadata;
  scan(target: ScanTarget, configuration: ScannerConfig): Promise<ScannerRun>;
}
