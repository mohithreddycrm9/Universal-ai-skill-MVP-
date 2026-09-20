import { redactDeep } from "../util/redact.js";

export type LogLevel = "silent" | "error" | "warn" | "info" | "debug";

const ORDER: Record<LogLevel, number> = {
  silent: 100,
  error: 40,
  warn: 30,
  info: 20,
  debug: 10,
};

export class Logger {
  constructor(private readonly level: LogLevel) {}

  error(msg: string, fields: Record<string, unknown> = {}): void {
    this.write("error", msg, fields);
  }
  warn(msg: string, fields: Record<string, unknown> = {}): void {
    this.write("warn", msg, fields);
  }
  info(msg: string, fields: Record<string, unknown> = {}): void {
    this.write("info", msg, fields);
  }
  debug(msg: string, fields: Record<string, unknown> = {}): void {
    this.write("debug", msg, fields);
  }

  private write(level: LogLevel, msg: string, fields: Record<string, unknown>): void {
    if (ORDER[level] < ORDER[this.level]) {
      return;
    }
    if (this.level === "silent") {
      return;
    }
    const line = {
      ts: new Date().toISOString(),
      level,
      msg,
      ...(redactDeep(fields) as Record<string, unknown>),
    };
    process.stderr.write(`${JSON.stringify(line)}\n`);
  }
}

export function loggerFromEnv(): Logger {
  const raw = (process.env.SKILL_MCP_LOG_LEVEL ?? "info").toLowerCase();
  const level: LogLevel =
    raw === "silent" || raw === "error" || raw === "warn" || raw === "info" || raw === "debug" ? raw : "info";
  return new Logger(level);
}
