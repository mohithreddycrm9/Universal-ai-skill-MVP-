import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

const root = join(import.meta.dirname, "..", "..");

describe("claude code plugin manifests", () => {
  it("plugin.json is valid", () => {
    const manifest = JSON.parse(
      readFileSync(join(root, ".claude-plugin", "plugin.json"), "utf8"),
    ) as { name: string; version: string };
    expect(manifest.name).toBe("universal-skill-trust");
    expect(manifest.version).toMatch(/^\d+\.\d+\.\d+$/);
    expect(readFileSync(join(root, ".mcp.json"), "utf8")).toContain("universal-skill-trust");
  });

  it(".mcp.json references the plugin launcher", () => {
    const mcp = JSON.parse(readFileSync(join(root, ".mcp.json"), "utf8")) as {
      mcpServers: Record<
        string,
        { command: string; args: string[]; env: Record<string, string> }
      >;
    };
    const server = mcp.mcpServers["universal-skill-trust"];
    expect(server).toBeDefined();
    expect(server!.args[0]).toContain("plugin-mcp-serve.mjs");
    expect(server!.env.SKILL_MCP_CONFIG_DIR).toContain("CLAUDE_PLUGIN_ROOT");
  });

  it("marketplace.json lists this plugin", () => {
    const market = JSON.parse(
      readFileSync(join(root, ".claude-plugin", "marketplace.json"), "utf8"),
    ) as { plugins: { name: string; source: string }[] };
    expect(market.plugins.some((p) => p.name === "universal-skill-trust" && p.source === "./")).toBe(
      true,
    );
  });
});
