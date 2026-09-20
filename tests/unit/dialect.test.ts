import { describe, expect, it } from "vitest";
import { splitSqlStatements, sqlitePlaceholdersToPostgres } from "../../src/registry/dialect.js";
import { PostgresAdapter, type PostgresDriver } from "../../src/registry/postgres.js";
import { SCHEMA_SQL } from "../../src/registry/schema.js";

describe("postgres dialect", () => {
  it("rewrites sqlite placeholders to $n", () => {
    expect(sqlitePlaceholdersToPostgres("SELECT * FROM skills WHERE id = ?")).toBe("SELECT * FROM skills WHERE id = $1");
    expect(sqlitePlaceholdersToPostgres("WHERE a = ? AND b = ? OR c = ?")).toBe("WHERE a = $1 AND b = $2 OR c = $3");
  });

  it("splits schema SQL into executable statements", () => {
    const statements = splitSqlStatements(SCHEMA_SQL);
    expect(statements.length).toBeGreaterThan(8);
    expect(statements.every((item) => item.toUpperCase().startsWith("CREATE"))).toBe(true);
  });

  it("runs SkillRegistry-shaped SQL through the postgres adapter with a mock driver", () => {
    const sql: string[] = [];
    const driver: PostgresDriver = {
      query(text, params = []) {
        sql.push(text);
        if (text.startsWith("SELECT") && params[0] === "skl_1") {
          return {
            rows: [{ id: "skl_1", name: "demo" }],
            rowCount: 1,
          };
        }
        return { rows: [], rowCount: 0 };
      },
      close() {},
    };
    const adapter = new PostgresAdapter("postgres://localhost/skill_mcp", driver);
    adapter.run(
      `INSERT INTO skills (id, name) VALUES (?, ?)`,
      ["skl_1", "demo"],
    );
    const row = adapter.get<{ id: string }>("SELECT * FROM skills WHERE id = ?", ["skl_1"]);
    expect(row?.id).toBe("skl_1");
    expect(sql.some((item) => item.includes("$1") && item.includes("$2"))).toBe(true);
    expect(sql.some((item) => item.includes("?"))).toBe(false);
  });
});
