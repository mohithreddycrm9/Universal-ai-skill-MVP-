export interface QueryResult {
  lastInsertRowid: number | bigint;
  changes: number;
}

export interface DatabaseAdapter {
  readonly driver: "sqlite" | "postgres";
  exec(sql: string): void;
  run(sql: string, params?: unknown[]): QueryResult;
  get<T>(sql: string, params?: unknown[]): T | undefined;
  all<T>(sql: string, params?: unknown[]): T[];
  transaction<T>(fn: () => T): T;
  close(): void;
}
