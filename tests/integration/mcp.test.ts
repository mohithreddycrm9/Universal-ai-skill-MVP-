import { describe, expect, it } from "vitest";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { InMemoryTransport } from "@modelcontextprotocol/sdk/inMemory.js";
import { createMcpServer, GATEWAY_TOOL_NAMES } from "../../src/mcp/server.js";
import { testGateway, benignPackage } from "../helpers.js";

describe("MCP server", () => {
  it("lists compact gateway tools with schemas", async () => {
    const gateway = testGateway([benignPackage()]);
    const server = createMcpServer(gateway);
    const [clientTransport, serverTransport] = InMemoryTransport.createLinkedPair();
    const client = new Client({ name: "test", version: "0" });
    await Promise.all([server.connect(serverTransport), client.connect(clientTransport)]);
    const listed = await client.listTools();
    const names = listed.tools.map((tool) => tool.name);
    for (const required of GATEWAY_TOOL_NAMES) {
      expect(names).toContain(required);
    }
    const discover = listed.tools.find((tool) => tool.name === "discover_skill");
    expect(discover?.inputSchema).toBeTruthy();
    await client.close();
    await server.close();
  });
});
