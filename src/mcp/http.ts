import { createServer, type IncomingMessage, type ServerResponse } from "node:http";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { StreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/streamableHttp.js";
import type { SkillTrustGateway } from "../gateway.js";
import { createMcpServer } from "./server.js";

export async function serveStdio(gateway: SkillTrustGateway): Promise<void> {
  const server = createMcpServer(gateway);
  const transport = new StdioServerTransport();
  await server.connect(transport);
}

export function serveHttp(gateway: SkillTrustGateway, host: string, port: number): Promise<void> {
  const httpServer = createServer(async (req: IncomingMessage, res: ServerResponse) => {
    const url = new URL(req.url ?? "/", `http://${host}:${port}`);
    if (req.method === "GET" && url.pathname === "/health") {
      res.writeHead(200, { "content-type": "application/json" });
      res.end(JSON.stringify({ status: "ok", service: "universal-skill-trust-gateway" }));
      return;
    }
    if (req.method === "GET" && url.pathname === "/metrics") {
      res.writeHead(200, { "content-type": "text/plain" });
      res.end(gateway.metrics.prometheus() || "skill_mcp_up 1\n");
      return;
    }
    if (url.pathname === "/mcp") {
      const mcp = createMcpServer(gateway);
      const transport = new StreamableHTTPServerTransport({ sessionIdGenerator: undefined });
      await mcp.connect(transport);
      await transport.handleRequest(req, res);
      return;
    }
    res.writeHead(404);
    res.end("not found");
  });
  return new Promise((resolve) => {
    httpServer.listen(port, host, () => resolve());
  });
}
