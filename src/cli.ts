#!/usr/bin/env node
import { Command } from "commander";
import { createGateway } from "./gateway.js";
import { serveHttp, serveStdio } from "./mcp/http.js";
import { Logger } from "./observability/log.js";

export async function main(argv = process.argv): Promise<void> {
  const program = new Command();
  program.name("skill-mcp").description("Universal Skills MCP CLI (free/OSS-first)").version("0.3.0");

  program
    .command("serve")
    .description("Start the MCP gateway (stdio default)")
    .option("--http", "Enable Streamable HTTP")
    .option("--host <host>", "HTTP bind host", "127.0.0.1")
    .option("--port <port>", "HTTP port", "43177")
    .action(async (opts: { http?: boolean; host: string; port: string }) => {
      const gateway = createGateway();
      if (opts.http) {
        const port = Number(opts.port);
        await serveHttp(gateway, opts.host, port);
        new Logger("info").info("http_listen", { host: opts.host, port });
        await new Promise(() => undefined);
      } else {
        await serveStdio(gateway);
      }
    });

  const withGw = () => createGateway();

  program
    .command("discover")
    .argument("<query>")
    .option("--json", "JSON output")
    .action(async (query: string, opts: { json?: boolean }) => {
      const out = await withGw().discover({ query, requestId: "cli" });
      print(out, opts.json);
    });

  program
    .command("acquire")
    .argument("<query>")
    .option("--wait", "Run pipeline in-process")
    .option("--json", "JSON output")
    .action(async (query: string, opts: { wait?: boolean; json?: boolean }) => {
      const out = await withGw().acquire({ query, wait: Boolean(opts.wait), requestId: "cli" });
      print(out, opts.json);
    });

  program
    .command("status")
    .argument("<skillId>")
    .option("--json", "JSON output")
    .action((skillId: string, opts: { json?: boolean }) => {
      print(withGw().getSkillStatus({ skillId }), opts.json);
    });

  program
    .command("list")
    .option("--json", "JSON output")
    .action((opts: { json?: boolean }) => {
      print(withGw().listSkills({}), opts.json);
    });

  program
    .command("quarantine")
    .argument("<skillId>")
    .option("--json", "JSON output")
    .action((skillId: string, opts: { json?: boolean }) => {
      print(withGw().invalidate({ skillId, reason: "cli-quarantine", requestId: "cli" }), opts.json);
    });

  program
    .command("invalidate")
    .argument("<skillId>")
    .argument("[reason]", "reason", "cli")
    .option("--json", "JSON output")
    .action((skillId: string, reason: string, opts: { json?: boolean }) => {
      print(withGw().invalidate({ skillId, reason, requestId: "cli" }), opts.json);
    });

  program
    .command("audit")
    .option("--json", "JSON output")
    .action((opts: { json?: boolean }) => {
      print(withGw().listAudit(50), opts.json);
    });

  program
    .command("costs")
    .option("--json", "JSON output")
    .action((opts: { json?: boolean }) => {
      print(withGw().listIntegrations(), opts.json);
    });

  program
    .command("approvals")
    .option("--json", "JSON output")
    .action((opts: { json?: boolean }) => {
      print(withGw().listPendingCostApprovals(), opts.json);
    });

  program
    .command("approve")
    .argument("<approvalId>")
    .option("--approver <name>", "Human identity", "cli-operator")
    .option("--json", "JSON output")
    .action((approvalId: string, opts: { approver: string; json?: boolean }) => {
      print(withGw().approvePaidOperation({ approvalId, approver: opts.approver, requestId: "cli" }), opts.json);
    });

  program
    .command("reject")
    .argument("<approvalId>")
    .option("--approver <name>", "Human identity", "cli-operator")
    .option("--json", "JSON output")
    .action((approvalId: string, opts: { approver: string; json?: boolean }) => {
      print(withGw().rejectPaidOperation({ approvalId, approver: opts.approver, requestId: "cli" }), opts.json);
    });

  program.action(async () => {
    await serveStdio(createGateway());
  });

  await program.parseAsync(argv);
}

function print(value: unknown, json?: boolean): void {
  if (json) {
    process.stdout.write(`${JSON.stringify(value, null, 2)}\n`);
    return;
  }
  process.stdout.write(`${JSON.stringify(value, null, 2)}\n`);
}
