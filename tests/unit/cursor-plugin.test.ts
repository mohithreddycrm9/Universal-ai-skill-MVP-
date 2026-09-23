import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

const root = join(import.meta.dirname, "..", "..");

describe("cursor plugin manifests", () => {
  it("plugin.json is valid", () => {
    const manifest = JSON.parse(
      readFileSync(join(root, ".cursor-plugin", "plugin.json"), "utf8"),
    ) as { name: string; version: string };
    expect(manifest.name).toBe("universal-skill-trust");
    expect(manifest.version).toMatch(/^\d+\.\d+\.\d+$/);
  });

  it("mcp.json references the cursor launcher", () => {
    const mcp = JSON.parse(readFileSync(join(root, "mcp.json"), "utf8")) as {
      mcpServers: Record<
        string,
        { command: string; args: string[]; env: Record<string, string> }
      >;
    };
    const server = mcp.mcpServers["universal-skill-trust"];
    expect(server.command).toBe("node");
    expect(server.args[0]).toContain("cursor-mcp-serve.mjs");
    expect(server.env.SKILL_MCP_CONFIG_DIR).toContain("CURSOR_PLUGIN_ROOT");
  });
});
