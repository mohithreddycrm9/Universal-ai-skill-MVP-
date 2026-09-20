import { describe, expect, it } from "vitest";
import { readdirSync, readFileSync, statSync } from "node:fs";
import { join } from "node:path";

const FORBIDDEN = [
  /this skill is (completely )?safe/i,
  /100% safe/i,
  /zero risk/i,
  /no malware/i,
  /contains no malware/i,
  /no security vulnerabilities exist/i,
];

function walk(dir: string): string[] {
  const out: string[] = [];
  for (const entry of readdirSync(dir)) {
    if (entry === "node_modules" || entry === "dist" || entry === ".git") {
      continue;
    }
    const path = join(dir, entry);
    if (statSync(path).isDirectory()) {
      out.push(...walk(path));
    } else if (/\.(ts|md|yaml|yml|json)$/.test(path)) {
      out.push(path);
    }
  }
  return out;
}

describe("no false security claims", () => {
  it("does not ship forbidden safety copy in src/docs/config", () => {
    const files = [...walk("src"), ...walk("config")];
    const hits: string[] = [];
    for (const file of files) {
      const text = readFileSync(file, "utf8");
      for (const re of FORBIDDEN) {
        if (re.test(text)) {
          hits.push(`${file}: ${re}`);
        }
      }
    }
    expect(hits).toEqual([]);
  });
});
