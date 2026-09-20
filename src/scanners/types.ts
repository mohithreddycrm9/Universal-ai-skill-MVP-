import type { ScanTarget, ScannerRun } from "../types.js";

export interface ScannerConfig {
  [key: string]: unknown;
  enabled?: boolean;
  failOpen?: boolean;
  binary?: string;
}

export interface SecurityScanner {
  readonly id: string;
  readonly version: string;
  scan(target: ScanTarget, configuration: ScannerConfig): Promise<ScannerRun>;
}
