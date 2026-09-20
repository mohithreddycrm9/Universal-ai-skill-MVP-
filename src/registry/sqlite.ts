import { DatabaseSync } from "node:sqlite";
import type { SQLInputValue } from "node:sqlite";
import { mkdirSync } from "node:fs";
import { dirname } from "node:path";
import type { DatabaseAdapter, QueryResult } from "./database.js";
import { SCHEMA_MIGRATE_SQL, SCHEMA_SQL } from "./schema.js";

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
    for (const statement of SCHEMA_MIGRATE_SQL.split(";")
      .map((s) => s.trim())
      .filter(Boolean)) {
      try {
        this.db.exec(statement);
      } catch {
        // Column already exists on newer schemas — ignore.
      }
    }
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
