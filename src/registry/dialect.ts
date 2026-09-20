/** Convert SQLite-style `?` placeholders to PostgreSQL `$1..$n` placeholders. */
export function sqlitePlaceholdersToPostgres(sql: string): string {
  let index = 0;
  return sql.replace(/\?/g, () => `$${++index}`);
}

/** Split a schema script into statements. Schema SQL must not contain semicolons inside literals. */
export function splitSqlStatements(sql: string): string[] {
  return sql
    .split(";")
    .map((part) =>
      part
        .split("\n")
        .filter((line) => !line.trim().startsWith("--"))
        .join("\n")
        .trim(),
    )
    .filter((part) => part.length > 0);
}

export function isPostgresUpsert(sql: string): boolean {
  return /on\s+conflict/i.test(sql);
}
