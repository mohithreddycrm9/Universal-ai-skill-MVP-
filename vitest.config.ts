import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    include: ["tests/**/*.test.ts"],
    testTimeout: 20_000,
    hookTimeout: 20_000,
    pool: "forks",
    execArgv: ["--experimental-sqlite"],
    env: {
      SKILL_MCP_LOG_LEVEL: "silent",
    },
  },
});
