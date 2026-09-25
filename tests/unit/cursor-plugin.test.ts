import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { GATEWAY_TOOL_NAMES } from "../../src/mcp/server.js";

const root = join(import.meta.dirname, "..", "..");

describe("cursor plugin manifests", () => {
  it("plugin.json is valid", () => {
    const manifest = JSON.parse(
      readFileSync(join(root, ".cursor-plugin", "plugin.json"), "utf8"),
    ) as { name: string; version: string };
    expect(manifest.name).toBe("universal-skill-trust");
    expect(manifest.version).toMatch(/^\d+\.\d+\.\d+$/);
  });

  it("bundled skill references registered MCP tool names", () => {
    const skill = readFileSync(join(root, "skills", "universal-skill-trust", "SKILL.md"), "utf8");
    const registered = new Set<string>(GATEWAY_TOOL_NAMES);
    const toolsInSkill = [
      "get_build_suggestions",
      "discover_skill",
      "acquire_skill",
      "get_skill_status",
      "get_skill",
      "get_skill_trust",
      "get_skill_permissions",
    ];
    for (const name of toolsInSkill) {
      expect(skill).toContain(name);
      expect(registered.has(name)).toBe(true);
    }
    expect(skill).not.toContain("explain_skill_trust");
  });

  it("mcp.json references the cursor launcher", () => {
    const mcp = JSON.parse(readFileSync(join(root, "mcp.json"), "utf8")) as {
      mcpServers: Record<
        string,
        { command: string; args: string[]; env: Record<string, string> }
      >;
    };
    const server = mcp.mcpServers["universal-skill-trust"];
    expect(server).toBeDefined();
    expect(server!.command).toBe("node");
    expect(server!.args[0]).toContain("plugin-mcp-serve.mjs");
    expect(server!.env.SKILL_MCP_CONFIG_DIR).toContain("CURSOR_PLUGIN_ROOT");
  });
});
