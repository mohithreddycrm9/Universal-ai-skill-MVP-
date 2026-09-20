import { DatabaseSync } from "node:sqlite";
import type { SQLInputValue } from "node:sqlite";
import { mkdirSync } from "node:fs";
import { dirname } from "node:path";
import type { DatabaseAdapter, QueryResult } from "./database.js";
import { SCHEMA_SQL } from "./schema.js";

export class SqliteAdapter implements DatabaseAdapter {
  readonly driver = "sqlite" as const;
  private readonly db: DatabaseSync;

  constructor(path: string) {
    if (path !== ":memory:") {
      mkdirSync(dirname(path), { recursive: true });
    }
    this.db = new DatabaseSync(path);
    this.db.exec("PRAGMA foreign_keys = ON");
    this.db.exec("PRAGMA journal_mode = WAL");
    this.db.exec(SCHEMA_SQL);
  }

  exec(sql: string): void {
    this.db.exec(sql);
  }

  run(sql: string, params: unknown[] = []): QueryResult {
    const result = this.db.prepare(sql).run(...(params as SQLInputValue[]));
    return { lastInsertRowid: result.lastInsertRowid, changes: Number(result.changes) };
  }

  get<T>(sql: string, params: unknown[] = []): T | undefined {
    const row = this.db.prepare(sql).get(...(params as SQLInputValue[]));
    return row as T | undefined;
  }

  all<T>(sql: string, params: unknown[] = []): T[] {
    return this.db.prepare(sql).all(...(params as SQLInputValue[])) as T[];
  }

  transaction<T>(fn: () => T): T {
    this.db.exec("BEGIN");
    try {
      const value = fn();
      this.db.exec("COMMIT");
      return value;
    } catch (error) {
      this.db.exec("ROLLBACK");
      throw error;
    }
  }

  close(): void {
    this.db.close();
  }
}

/**
 * PostgreSQL swap point. Not implemented in this build — configuring a postgres URL
 * fails closed rather than silently using SQLite.
 */
export class PostgresAdapter implements DatabaseAdapter {
  readonly driver = "postgres" as const;

  constructor(_url: string) {
    throw new Error(
      "PostgresAdapter is the documented swap point but is not implemented in this build. Use SQLite or add a pg client.",
    );
  }

  exec(): void {
    throw new Error("PostgresAdapter not implemented");
  }
  run(): QueryResult {
    throw new Error("PostgresAdapter not implemented");
  }
  get<T>(): T | undefined {
    throw new Error("PostgresAdapter not implemented");
  }
  all<T>(): T[] {
    throw new Error("PostgresAdapter not implemented");
  }
  transaction<T>(): T {
    throw new Error("PostgresAdapter not implemented");
  }
  close(): void {}
}
