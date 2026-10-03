import { existsSync, mkdtempSync, readFileSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { isAbsolute, join, resolve } from "node:path";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";
import { describe, expect, it } from "vitest";
import { parse } from "yaml";
import { GATEWAY_TOOL_NAMES } from "../../src/mcp/server.js";

const root = resolve(import.meta.dirname, "../..");
const readJson = (path: string) => JSON.parse(readFileSync(join(root, path), "utf8"));
const manifest = readJson(".codex-plugin/plugin.json");
const server = readJson(manifest.mcpServers).mcpServers[manifest.name];

describe("Codex plugin", () => {
  it("resolves all declared components and marketplace source inside the repo", () => {
    expect(manifest.version).toBe(readJson("package.json").version);
    for (const path of [manifest.skills, manifest.mcpServers]) {
      expect(path.startsWith("./")).toBe(true);
      expect(isAbsolute(path)).toBe(false);
      expect(path.split("/")).not.toContain("..");
      expect(existsSync(join(root, path))).toBe(true);
    }
    const market = readJson(".agents/plugins/marketplace.json");
    const plugin = market.plugins.find((entry: { name: string }) => entry.name === manifest.name);
    expect(resolve(root, plugin.source.path)).toBe(root);
    expect(plugin.policy.installation).toBe("AVAILABLE");
    expect(plugin.policy.authentication).toBe("ON_INSTALL");
    expect(plugin.category).toBeTruthy();
    const skill = readFileSync(join(root, manifest.skills, manifest.name, "SKILL.md"), "utf8");
    const frontmatter = parse(skill.split("---")[1]!);
    expect(frontmatter.name).toBe(manifest.name);
    expect(frontmatter.description).toBeTruthy();
    const tools = [...skill.matchAll(/`([a-z]+(?:_[a-z]+)+)`/g)].map((match) => match[1]!);
    for (const tool of tools.filter((name) => /^(get|discover|acquire)_/.test(name))) {
      expect(GATEWAY_TOOL_NAMES).toContain(tool);
    }
  });

  // These tests connect to the real launcher, not an in-memory server. A non-MCP
  // stdout message or incorrect cwd/config resolution breaks initialization.
  it.each(["codex", "compatibility", "override"])(
    "connects from an unrelated cwd and isolates runtime data (%s)",
    async (mode) => {
      const temporary = mkdtempSync(join(tmpdir(), "codex-plugin-"));
      const pluginData = join(temporary, "plugin data");
      const overrideData = join(temporary, "override data");
      const expectedData = mode === "override" ? overrideData : pluginData;
      const env: Record<string, string> = {
        SKILL_MCP_LOG_LEVEL: "silent",
        ...Object.fromEntries(Object.entries(server.env).map(([key, value]) =>
          [key, String(value).replaceAll("${CLAUDE_PLUGIN_ROOT}", root)])),
        ...(mode === "compatibility"
          ? { CLAUDE_PLUGIN_ROOT: root, CLAUDE_PLUGIN_DATA: pluginData }
          : { PLUGIN_ROOT: root, PLUGIN_DATA: pluginData }),
        ...(mode === "override" ? { SKILL_MCP_DATA_DIR: overrideData } : {}),
      };
      const transport = new StdioClientTransport({
        command: process.execPath,
        args: server.args.map((arg: string) => arg.replaceAll("${CLAUDE_PLUGIN_ROOT}", root)),
        cwd: temporary,
        env,
        stderr: "pipe",
      });
      let diagnostics = "";
      transport.stderr?.on("data", (chunk) => { diagnostics += String(chunk); });
      const client = new Client({ name: "codex-plugin-test", version: "1.0.0" });
      try {
        await client.connect(transport);
        const listed = await client.listTools();
        expect(listed.tools.map((tool) => tool.name)).toEqual(expect.arrayContaining([...GATEWAY_TOOL_NAMES]));
        const result = await client.callTool({ name: "search_skills", arguments: { query: "missing-fixture" } });
        expect(result.isError).not.toBe(true);
        const content = result.content as { type: string; text: string }[];
        const envelope = JSON.parse(content[0]!.text);
        expect(envelope.ok).toBe(true);
        expect(envelope.securityNotice).toBeTruthy();
        expect(existsSync(join(expectedData, "skill-mcp.sqlite"))).toBe(true);
        expect(existsSync(join(temporary, "data"))).toBe(false);
        if (mode === "override") expect(existsSync(pluginData)).toBe(false);
      } catch (error) {
        throw new Error(`Plugin connection failed: ${String(error)}\n${diagnostics}`);
      } finally {
        await client.close();
        await transport.close();
        rmSync(temporary, { recursive: true, force: true });
      }
    },
  );
});
