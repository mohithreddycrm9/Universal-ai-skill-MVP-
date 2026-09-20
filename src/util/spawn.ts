import { spawnSync, type SpawnSyncOptions } from "node:child_process";

export interface SpawnResult {
  status: number | null;
  stdout: string;
  stderr: string;
  error?: NodeJS.ErrnoException;
  signal?: NodeJS.Signals | null;
}

export interface SpawnOptions {
  timeout?: number;
  cwd?: string;
  env?: NodeJS.ProcessEnv;
  input?: string;
}

export type SpawnFn = (command: string, args: readonly string[], options?: SpawnOptions) => SpawnResult;

export function defaultSpawn(command: string, args: readonly string[], options: SpawnOptions = {}): SpawnResult {
  const opts: SpawnSyncOptions = {
    encoding: "utf8",
    timeout: options.timeout ?? 15_000,
    cwd: options.cwd,
    env: options.env,
    input: options.input,
  };
  const result = spawnSync(command, args as string[], opts);
  return {
    status: result.status,
    stdout: typeof result.stdout === "string" ? result.stdout : "",
    stderr: typeof result.stderr === "string" ? result.stderr : "",
    error: result.error,
    signal: result.signal,
  };
}

export function missingBinary(result: SpawnResult): boolean {
  if (result.error && (result.error.code === "ENOENT" || result.error.code === "EACCES")) {
    return true;
  }
  return result.status !== 0 && /not found|no such file/i.test(`${result.stderr} ${result.error?.message ?? ""}`);
}
