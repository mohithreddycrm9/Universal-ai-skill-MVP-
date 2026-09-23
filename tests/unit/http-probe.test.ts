import { describe, expect, it, vi, afterEach } from "vitest";
import { probeHttpSurface } from "../../src/security/http-probe.js";

describe("http probe", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("flags missing security headers on a successful response", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => ({
        status: 200,
        headers: new Headers({ "content-type": "text/html" }),
      })),
    );
    const result = await probeHttpSurface({ url: "https://example.test/" });
    expect(result.findings.some((f) => f.id === "probe:missing-hsts")).toBe(true);
    expect(result.status).toBe("FAIL");
  });
});
