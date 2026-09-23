#!/usr/bin/env node
/**
 * Cursor plugin MCP entrypoint: ensure build artifacts exist, then stdio-serve the gateway.
 * Stdout is reserved for MCP; logs go to stderr from the server.
 */
import { spawn } from "node:child_process";
import { existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const distEntry = join(root, "dist", "index.js");
const nodeModules = join(root, "node_modules");

/** npm/tsc must not write to stdout — MCP owns stdio after this script hands off. */
function run(command, args) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, {
      cwd: root,
      env: process.env,
      stdio: ["ignore", "pipe", "pipe"],
    });
    child.stdout.on("data", (chunk) => process.stderr.write(chunk));
    child.stderr.on("data", (chunk) => process.stderr.write(chunk));
    child.on("error", reject);
    child.on("exit", (code) => {
      if (code === 0) resolve();
      else reject(new Error(`${command} ${args.join(" ")} exited with ${code ?? "unknown"}`));
    });
  });
}

async function ensureReady() {
  if (!existsSync(nodeModules)) {
    process.stderr.write("[universal-skill-trust] Installing dependencies…\n");
    await run("npm", ["install", "--no-fund", "--no-audit"]);
  }
  if (!existsSync(distEntry)) {
    process.stderr.write("[universal-skill-trust] Building MCP server…\n");
    await run("npm", ["run", "build"]);
  }
}

const configDir = process.env.SKILL_MCP_CONFIG_DIR ?? join(root, "config");
const dataDir = process.env.SKILL_MCP_DATA_DIR ?? join(root, "data");

await ensureReady();

const child = spawn(
  process.execPath,
  ["--experimental-sqlite", distEntry, "serve", ...process.argv.slice(2)],
  {
    cwd: root,
    stdio: "inherit",
    env: {
      ...process.env,
      SKILL_MCP_CONFIG_DIR: configDir,
      SKILL_MCP_DATA_DIR: dataDir,
    },
  },
);

child.on("exit", (code, signal) => {
  if (signal) process.kill(process.pid, signal);
  process.exit(code ?? 1);
});
