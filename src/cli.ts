#!/usr/bin/env node
import { createInterface } from "node:readline/promises";
import { stdin as input, stdout as output } from "node:process";
import { Command } from "commander";
import { createGateway } from "./gateway.js";
import { serveHttp, serveStdio } from "./mcp/http.js";
import { Logger } from "./observability/log.js";

/**
 * Trusted approval channel:
 * - Runs outside the MCP tool surface (the model cannot invoke this as a tool).
 * - Requires an interactive y/N confirmation on a local operator-controlled TTY
 *   (or SKILL_MCP_APPROVE_YES=1 for non-interactive operator automation).
 * - Sets method=local_interactive on the approval record.
 * - MCP caller strings (approver:"human") never reach this path as authorization.
 */
async function confirmLocalInteractive(prompt: string): Promise<boolean> {
  if (process.env.SKILL_MCP_APPROVE_YES === "1") {
    return true;
  }
  if (!input.isTTY || !output.isTTY) {
    throw new Error(
      "Interactive approval requires a TTY. Re-run in a terminal, or set SKILL_MCP_APPROVE_YES=1 for explicit non-interactive operator approval.",
    );
  }
  const rl = createInterface({ input, output });
  try {
    const answer = (await rl.question(`${prompt} [y/N] `)).trim().toLowerCase();
    return answer === "y" || answer === "yes";
  } finally {
    rl.close();
  }
}

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
    .option("--approver <name>", "Human identity label (not authorization proof)", "cli-operator")
    .option("--json", "JSON output")
    .description(
      "Trusted local-interactive approval (y/N). Outside MCP path; sets method=local_interactive.",
    )
    .action(async (approvalId: string, opts: { approver: string; json?: boolean }) => {
      const gw = withGw();
      const pending = gw.listPendingCostApprovals() as {
        items: Array<{ id: string; operation?: string; provider?: string; service?: string }>;
        capabilityItems: Array<{
          id: string;
          skillId: string;
          capability: string;
          fingerprint: string;
        }>;
      };
      const cost = pending.items.find((item) => item.id === approvalId);
      const cap = pending.capabilityItems.find((item) => item.id === approvalId);
      if (!cost && !cap) {
        // May already be listed only as pending — also allow lookup via approve which loads by id
        process.stderr.write(`Approving ${approvalId} (must be PENDING)...\n`);
      } else if (cap) {
        process.stderr.write(
          `Capability approval ${cap.id}: skill=${cap.skillId} capability=${cap.capability} fingerprint=${cap.fingerprint}\n`,
        );
      } else if (cost) {
        process.stderr.write(
          `Cost approval ${cost.id}: ${cost.operation} / ${cost.provider} / ${cost.service}\n`,
        );
      }
      const ok = await confirmLocalInteractive("Approve this PENDING record?");
      if (!ok) {
        print({ approved: false, approvalId, message: "Operator declined (N)." }, opts.json);
        return;
      }
      print(
        gw.approveLocalInteractive({
          approvalId,
          requestId: "cli",
          actor: opts.approver,
        }),
        opts.json,
      );
    });

  program
    .command("reject")
    .argument("<approvalId>")
    .option("--approver <name>", "Human identity label (not authorization proof)", "cli-operator")
    .option("--json", "JSON output")
    .description("Trusted local-interactive rejection (y/N). Outside MCP path.")
    .action(async (approvalId: string, opts: { approver: string; json?: boolean }) => {
      const ok = await confirmLocalInteractive("Reject this PENDING/APPROVED record?");
      if (!ok) {
        print({ rejected: false, approvalId, message: "Operator declined to reject (N)." }, opts.json);
        return;
      }
      print(
        withGw().rejectLocalInteractive({
          approvalId,
          requestId: "cli",
          actor: opts.approver,
        }),
        opts.json,
      );
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
