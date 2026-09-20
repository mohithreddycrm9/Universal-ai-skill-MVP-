import { spawnSync } from "node:child_process";
import { createRequire } from "node:module";
import type { DatabaseAdapter, QueryResult } from "./database.js";
import { SCHEMA_MIGRATE_SQL, SCHEMA_SQL } from "./schema.js";
import { splitSqlStatements, sqlitePlaceholdersToPostgres } from "./dialect.js";

export interface PostgresQueryResult {
  rows: Record<string, unknown>[];
  rowCount: number;
}

export interface PostgresDriver {
  query(sql: string, params?: unknown[]): PostgresQueryResult;
  close(): void;
}

/**
 * Optional PostgreSQL adapter. SQLite remains the default.
 * Queries use the same `?` placeholders as SQLite; they are rewritten to `$n`.
 */
export class PostgresAdapter implements DatabaseAdapter {
  readonly driver = "postgres" as const;
  private readonly client: PostgresDriver;

  constructor(url: string, driver?: PostgresDriver) {
    this.client = driver ?? createDefaultPgDriver(url);
    for (const statement of splitSqlStatements(SCHEMA_SQL)) {
      this.client.query(sqlitePlaceholdersToPostgres(statement), []);
    }
    for (const statement of splitSqlStatements(SCHEMA_MIGRATE_SQL)) {
      try {
        this.client.query(sqlitePlaceholdersToPostgres(statement), []);
      } catch {
        // Column already exists.
      }
    }
  }

  exec(sql: string): void {
    for (const statement of splitSqlStatements(sql)) {
      this.client.query(sqlitePlaceholdersToPostgres(statement), []);
    }
  }

  run(sql: string, params: unknown[] = []): QueryResult {
    const result = this.client.query(sqlitePlaceholdersToPostgres(sql), params);
    return { lastInsertRowid: 0, changes: result.rowCount };
  }

  get<T>(sql: string, params: unknown[] = []): T | undefined {
    const result = this.client.query(sqlitePlaceholdersToPostgres(sql), params);
    return result.rows[0] as T | undefined;
  }

  all<T>(sql: string, params: unknown[] = []): T[] {
    const result = this.client.query(sqlitePlaceholdersToPostgres(sql), params);
    return result.rows as T[];
  }

  transaction<T>(fn: () => T): T {
    this.exec("BEGIN");
    try {
      const value = fn();
      this.exec("COMMIT");
      return value;
    } catch (error) {
      this.exec("ROLLBACK");
      throw error;
    }
  }

  close(): void {
    this.client.close();
  }
}

function createDefaultPgDriver(url: string): PostgresDriver {
  let pg: { Client: new (config: { connectionString: string }) => PgClient } | undefined;
  try {
    const require = createRequire(import.meta.url);
    pg = require("pg") as { Client: new (config: { connectionString: string }) => PgClient };
  } catch {
    pg = undefined;
  }
  if (!pg) {
    throw new Error(
      "PostgresAdapter requires the optional 'pg' package (npm i pg) and DATABASE_URL. SQLite remains the default driver.",
    );
  }
  return spawnPgDriver(url);
}

interface PgClient {
  connect(): Promise<void>;
  query(text: string, values?: unknown[]): Promise<{ rows: Record<string, unknown>[]; rowCount: number | null }>;
  end(): Promise<void>;
}

/**
 * Sync driver: each query is a short-lived Node subprocess using `pg`.
 * Keeps DatabaseAdapter synchronous (SQLite uses node:sqlite DatabaseSync).
 */
function spawnPgDriver(url: string): PostgresDriver {
  return {
    query(sql: string, params: unknown[] = []): PostgresQueryResult {
      const child = spawnSync(
        process.execPath,
        ["--input-type=module", "-e", PG_EVAL],
        {
          encoding: "utf8",
          timeout: 20_000,
          input: JSON.stringify({ url, sql, params }),
          env: { ...process.env, NODE_NO_WARNINGS: "1" },
        },
      );
      if (child.status !== 0) {
        throw new Error(
          child.stderr || child.stdout || "Postgres query failed. Install optional dependency `pg` and set DATABASE_URL.",
        );
      }
      return JSON.parse(child.stdout) as PostgresQueryResult;
    },
    close(): void {},
  };
}

const PG_EVAL = `
import pg from "pg";
const chunks = [];
for await (const chunk of process.stdin) chunks.push(chunk);
const { url, sql, params } = JSON.parse(Buffer.concat(chunks).toString("utf8"));
const client = new pg.Client({ connectionString: url });
await client.connect();
try {
  const res = await client.query(sql, params);
  process.stdout.write(JSON.stringify({ rows: res.rows ?? [], rowCount: res.rowCount ?? 0 }));
} finally {
  await client.end();
}
`;
