import { describe, expect, it } from "vitest";
import { encodeEnvelope } from "../../src/mcp/envelope.js";

describe("token bounds", () => {
  it("truncates oversized MCP envelopes", () => {
    const huge = { blob: "n".repeat(80_000) };
    const encoded = encodeEnvelope("req_1", huge, 1024);
    expect(JSON.stringify(encoded).length).toBeLessThanOrEqual(2048);
    expect(encoded.warnings).toContain("response_truncated");
    expect(JSON.stringify(encoded)).not.toContain("n".repeat(1000));
  });
});
