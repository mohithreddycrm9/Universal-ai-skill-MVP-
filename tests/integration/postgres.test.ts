import { describe, expect, it } from "vitest";
import { PostgresAdapter } from "../../src/registry/postgres.js";
import { SkillRegistry } from "../../src/registry/skill-registry.js";
import { systemClock } from "../../src/util/clock.js";

const url = process.env.DATABASE_URL ?? process.env.SKILL_MCP_DATABASE_URL;

describe.skipIf(!url)("postgres live (DATABASE_URL)", () => {
  it("applies schema and round-trips a cost approval row", () => {
    const db = new PostgresAdapter(url as string);
    try {
      const registry = new SkillRegistry(db, systemClock);
      registry.insertCostApproval({
        id: "cst_pg_smoke",
        operation: "discover_skill",
        provider: "local",
        service: "smoke",
        status: "PENDING",
        approver: null,
        review: {
          operation: "discover_skill",
          provider: "local",
          service: "smoke",
          purpose: "smoke",
          reason: "test",
          potentialCost: "0",
          risk: "none",
          approvalRequired: true,
          notice: "test",
        },
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      });
      expect(registry.getCostApproval("cst_pg_smoke")?.id).toBe("cst_pg_smoke");
    } finally {
      db.close();
    }
  });
});
