import type { ScanTarget, ScannerRun } from "../types.js";
import type { CostMetadata } from "../cost/types.js";

export interface ScannerConfig {
  [key: string]: unknown;
  enabled?: boolean;
  failOpen?: boolean;
  binary?: string;
}

export interface SecurityScanner {
  readonly id: string;
  readonly version: string;
  readonly cost: CostMetadata;
  scan(target: ScanTarget, configuration: ScannerConfig): Promise<ScannerRun>;
}
