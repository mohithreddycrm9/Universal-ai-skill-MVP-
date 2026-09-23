import { lstatSync, readFileSync, readdirSync, realpathSync } from "node:fs";
import { basename, join, relative, resolve } from "node:path";
import { SkillMcpError } from "../errors.js";
import type { SkillFile, SkillPackage } from "../types.js";

const DEFAULT_IGNORE_DIRS = new Set([
  ".git",
  "node_modules",
  "dist",
  "build",
  "coverage",
  ".next",
  ".turbo",
  "vendor",
  "__pycache__",
  ".venv",
  "venv",
]);

const TEXT_EXTENSIONS = new Set([
  ".ts",
  ".tsx",
  ".js",
  ".jsx",
  ".mjs",
  ".cjs",
  ".json",
  ".yaml",
  ".yml",
  ".md",
  ".txt",
  ".env",
  ".toml",
  ".sh",
  ".py",
  ".go",
  ".rs",
  ".java",
  ".kt",
  ".sql",
  ".graphql",
  ".html",
  ".css",
  ".scss",
  ".xml",
  ".ini",
  ".cfg",
  ".conf",
  ".properties",
  ".dockerfile",
]);

export interface WorkspaceScanLimits {
  maxFiles: number;
  maxFileBytes: number;
}

export const DEFAULT_WORKSPACE_LIMITS: WorkspaceScanLimits = {
  maxFiles: 400,
  maxFileBytes: 512_000,
};

export interface WorkspaceScanResult {
  rootPath: string;
  package: SkillPackage;
  filesScanned: number;
  skipped: { reason: string; count: number }[];
}

export function loadWorkspaceAsPackage(
  inputPath: string,
  limits: WorkspaceScanLimits = DEFAULT_WORKSPACE_LIMITS,
): WorkspaceScanResult {
  const rootPath = resolve(inputPath);
  let realRoot: string;
  try {
    realRoot = realpathSync(rootPath);
  } catch {
    throw new SkillMcpError("INVALID_INPUT", `Workspace path not found: ${inputPath}`);
  }
  const stat = lstatSync(realRoot);
  if (!stat.isDirectory()) {
    throw new SkillMcpError("INVALID_INPUT", `Workspace path must be a directory: ${inputPath}`);
  }

  const files: SkillFile[] = [];
  const skipped = new Map<string, number>();
  const bump = (reason: string) => skipped.set(reason, (skipped.get(reason) ?? 0) + 1);

  walk(realRoot, realRoot, files, limits, bump);

  const repoName = basename(realRoot);
  const package_: SkillPackage = {
    sourceId: "workspace",
    publisher: "local",
    ownerLogin: "local",
    repository: `local/${repoName}`,
    repositoryUrl: `file://${realRoot}`,
    commitSha: "workspace",
    version: "workspace",
    archived: false,
    files,
  };

  return {
    rootPath: realRoot,
    package: package_,
    filesScanned: files.length,
    skipped: [...skipped.entries()].map(([reason, count]) => ({ reason, count })),
  };
}

function walk(
  root: string,
  dir: string,
  out: SkillFile[],
  limits: WorkspaceScanLimits,
  bump: (reason: string) => void,
): void {
  if (out.length >= limits.maxFiles) {
    bump("maxFiles");
    return;
  }
  let entries: string[];
  try {
    entries = readdirSync(dir);
  } catch {
    bump("unreadableDir");
    return;
  }
  for (const name of entries) {
    if (out.length >= limits.maxFiles) {
      bump("maxFiles");
      return;
    }
    const full = join(dir, name);
    let st;
    try {
      st = lstatSync(full);
    } catch {
      bump("unreadableEntry");
      continue;
    }
    if (st.isSymbolicLink()) {
      bump("symlink");
      continue;
    }
    if (st.isDirectory()) {
      if (DEFAULT_IGNORE_DIRS.has(name)) {
        bump("ignoredDir");
        continue;
      }
      walk(root, full, out, limits, bump);
      continue;
    }
    if (!st.isFile()) {
      bump("notFile");
      continue;
    }
    if (st.size > limits.maxFileBytes) {
      bump("fileTooLarge");
      continue;
    }
    const rel = relative(root, full).replace(/\\/g, "/");
    if (!shouldScanFile(rel, name)) {
      bump("binaryOrExtension");
      continue;
    }
    let content: string;
    try {
      content = readFileSync(full, "utf8");
    } catch {
      bump("readError");
      continue;
    }
    if (content.includes("\u0000")) {
      bump("binaryContent");
      continue;
    }
    out.push({ path: rel, content });
  }
}

function shouldScanFile(relPath: string, fileName: string): boolean {
  const lower = fileName.toLowerCase();
  if (lower === "dockerfile" || lower.startsWith("dockerfile.")) {
    return true;
  }
  if (lower === ".env" || lower.startsWith(".env.")) {
    return true;
  }
  const dot = lower.lastIndexOf(".");
  if (dot === -1) {
    return lower === "makefile" || lower === "license" || lower === "readme";
  }
  const ext = lower.slice(dot);
  return TEXT_EXTENSIONS.has(ext);
}
