import { describe, expect, it } from "vitest";
import { benignPackage, testGateway } from "../helpers.js";

function elevate(gw: ReturnType<typeof testGateway>, skillId: string, capability: string, requestId: string) {
  const pending = gw.requestCapability({
    skillId,
    capability,
    requestId: `${requestId}-req`,
  }) as { approvalId: string; status: string };
  expect(pending.status).toBe("NEEDS_CAPABILITY_APPROVAL");
  gw.approveLocalInteractive({ approvalId: pending.approvalId, requestId: `${requestId}-apr` });
  return gw.requestCapability({
    skillId,
    capability,
    approvalId: pending.approvalId,
    requestId: `${requestId}-grant`,
  }) as { effective: string[]; permissionsReset: boolean; status: string };
}

describe("permissions bound to immutable identity", () => {
  it("same artifact + successful revalidation may preserve elevated permissions", async () => {
    const gw = testGateway([benignPackage()]);
    const acquired = (await gw.acquire({
      query: "csv-normalize",
      wait: true,
      requestId: "perm-1",
    })) as { skillId: string };

    const elevated = elevate(gw, acquired.skillId, "network.read", "perm-1b");

    expect(elevated.effective).toContain("network.read");
    expect(elevated.permissionsReset ?? false).toBe(false);

    const again = gw.getSkillPermissions({ skillId: acquired.skillId }) as {
      effective: string[];
      permissionsReset: boolean;
      permissionsBoundTo: { commitSha: string; fingerprint: string };
    };
    expect(again.effective).toContain("network.read");
    expect(again.permissionsReset).toBe(false);
    expect(again.permissionsBoundTo?.commitSha).toBeTruthy();
    expect(again.permissionsBoundTo?.fingerprint).toBeTruthy();
  });

  it("changed commit resets elevated permissions (visible)", async () => {
    const gw = testGateway([benignPackage()]);
    const acquired = (await gw.acquire({
      query: "csv-normalize",
      wait: true,
      requestId: "perm-2",
    })) as { skillId: string };

    elevate(gw, acquired.skillId, "network.read", "perm-2b");

    gw.registry.updateSkill(acquired.skillId, {
      commitSha: "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
    });

    const perms = gw.getSkillPermissions({ skillId: acquired.skillId }) as {
      effective: string[];
      permissionsReset: boolean;
      permissionsResetReason: string | null;
    };
    expect(perms.effective).not.toContain("network.read");
    expect(perms.effective).toContain("filesystem.read");
    expect(perms.permissionsReset).toBe(true);
    expect(perms.permissionsResetReason).toMatch(/commit/i);
  });

  it("changed fingerprint resets elevated permissions (visible)", async () => {
    const gw = testGateway([benignPackage()]);
    const acquired = (await gw.acquire({
      query: "csv-normalize",
      wait: true,
      requestId: "perm-3",
    })) as { skillId: string };

    elevate(gw, acquired.skillId, "network.read", "perm-3b");

    gw.registry.updateSkill(acquired.skillId, {
      fingerprint: "sha256:altered-fingerprint",
    });

    const perms = gw.getSkillPermissions({ skillId: acquired.skillId }) as {
      effective: string[];
      permissionsReset: boolean;
      permissionsResetReason: string | null;
    };
    expect(perms.effective).not.toContain("network.read");
    expect(perms.effective).toContain("filesystem.read");
    expect(perms.permissionsReset).toBe(true);
    expect(perms.permissionsResetReason).toMatch(/fingerprint/i);
  });

  it("security invalidation resets elevated permissions (visible)", async () => {
    const gw = testGateway([benignPackage()]);
    const acquired = (await gw.acquire({
      query: "csv-normalize",
      wait: true,
      requestId: "perm-4",
    })) as { skillId: string };

    elevate(gw, acquired.skillId, "network.read", "perm-4b");

    const invalidated = gw.invalidate({
      skillId: acquired.skillId,
      reason: "operator_revoke",
      requestId: "perm-4c",
    }) as {
      permissionsReset: boolean;
      permissionsResetReason: string;
      effective: string[];
    };

    expect(invalidated.permissionsReset).toBe(true);
    expect(invalidated.permissionsResetReason).toMatch(/invalid/i);
    expect(invalidated.effective).not.toContain("network.read");
  });

  it("new skill starts at baseline (filesystem.read)", async () => {
    const gw = testGateway([benignPackage()]);
    const acquired = (await gw.acquire({
      query: "csv-normalize",
      wait: true,
      requestId: "perm-5",
    })) as { skillId: string };
    const perms = gw.getSkillPermissions({ skillId: acquired.skillId }) as { effective: string[] };
    expect(perms.effective).toEqual(["filesystem.read"]);
  });
});
