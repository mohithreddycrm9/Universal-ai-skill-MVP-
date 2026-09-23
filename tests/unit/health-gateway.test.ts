import { describe, expect, it } from "vitest";
import { createGateway } from "../../src/gateway.js";
import { benignPackage } from "../helpers.js";

describe("runCodeHealthCheck", () => {
  it("scans the repo workspace without requiring STRIX", async () => {
    const gw = createGateway();
    gw.registerPackage(benignPackage());
    const out = (await gw.runCodeHealthCheck({
      path: process.cwd(),
      requestId: "test",
    })) as { status: string; filesScanned: number; scanners: Array<{ scannerId: string }> };
    expect(out.filesScanned).toBeGreaterThan(10);
    expect(out.scanners.map((s) => s.scannerId)).toContain("code_health");
    expect(out.scanners.map((s) => s.scannerId)).not.toContain("strix");
    expect(["PASS", "FAIL", "INCONCLUSIVE"]).toContain(out.status);
  });
});
