import { z } from "zod";
import { SkillMcpError } from "../errors.js";
import { looksLikeSecret } from "../util/redact.js";
import { sha256 } from "../util/hash.js";
import { canonicalize } from "../util/canonical.js";
import type { Capability, RiskLevel, SkillFile, SkillManifest, SkillPackage } from "../types.js";
import { CAPABILITIES, RISK_LEVELS } from "../types.js";

const MAX_INSTRUCTIONS = 8 * 1024;
const MAX_MANIFEST = 16 * 1024;

const capabilitySchema = z.enum(CAPABILITIES);
const riskSchema = z.enum(RISK_LEVELS);

export const skillManifestSchema = z.object({
  apiVersion: z.literal("skill.mcp/v1"),
  kind: z.literal("Skill"),
  metadata: z.object({
    name: z.string().min(1).max(120),
    description: z.string().min(1).max(500),
    publisher: z.string().min(1).max(120),
    repository: z.string().min(1).max(500),
    version: z.string().min(1).max(64),
    license: z.string().max(80).optional(),
    tags: z.array(z.string().max(40)).max(16).optional(),
  }),
  spec: z.object({
    instructions: z.string().min(1).max(MAX_INSTRUCTIONS),
    risk: riskSchema,
    capabilitiesDeclared: z.array(capabilitySchema).max(16),
    entrypoints: z.array(z.string().max(200)).max(8),
    files: z
      .array(
        z.object({
          path: z.string().min(1).max(240),
          sha256: z.string().min(8).max(80),
        }),
      )
      .max(32),
    dependencies: z.object({
      lockHash: z.string().min(8).max(80),
    }),
  }),
});

export function parseManifest(input: unknown): SkillManifest {
  const parsed = skillManifestSchema.safeParse(input);
  if (!parsed.success) {
    throw new SkillMcpError("INVALID_INPUT", "Skill manifest failed schema validation", {
      issues: parsed.error.issues.slice(0, 8),
    });
  }
  const manifest = parsed.data as SkillManifest;
  const encoded = canonicalize(manifest);
  if (encoded.length > MAX_MANIFEST) {
    throw new SkillMcpError("INVALID_INPUT", "Skill manifest exceeds size limit");
  }
  rejectSecretsInManifest(manifest);
  return manifest;
}

export function rejectSecretsInManifest(manifest: SkillManifest): void {
  const blob = `${manifest.metadata.name}\n${manifest.metadata.description}\n${manifest.spec.instructions}`;
  if (looksLikeSecret(blob)) {
    throw new SkillMcpError("SECURITY_GATE", "Manifest appears to contain a secret and was rejected");
  }
}

export function manifestFromPackage(pkg: SkillPackage, instructions: string, risk: RiskLevel): SkillManifest {
  const lock = pkg.files.find((file) => isLockfile(file.path));
  const lockHash = sha256(lock?.content ?? "");
  return parseManifest({
    apiVersion: "skill.mcp/v1",
    kind: "Skill",
    metadata: {
      name: inferName(pkg),
      description: inferDescription(pkg, instructions),
      publisher: pkg.publisher,
      repository: pkg.repositoryUrl,
      version: pkg.version || pkg.commitSha.slice(0, 12),
      license: pkg.license,
    },
    spec: {
      instructions: truncate(instructions, MAX_INSTRUCTIONS),
      risk,
      capabilitiesDeclared: inferDeclaredCapabilities(pkg, instructions),
      entrypoints: inferEntrypoints(pkg),
      files: pkg.files.map((file) => ({ path: file.path, sha256: sha256(file.content) })),
      dependencies: { lockHash },
    },
  });
}

export function inferName(pkg: SkillPackage): string {
  const yaml = readSkillYaml(pkg);
  if (yaml?.name) {
    return yaml.name;
  }
  const parts = pkg.repository.split("/");
  return parts[parts.length - 1] ?? pkg.repository;
}

function inferDescription(pkg: SkillPackage, instructions: string): string {
  const yaml = readSkillYaml(pkg);
  if (yaml?.description) {
    return truncate(yaml.description, 500);
  }
  return truncate(instructions.split("\n")[0] ?? pkg.repository, 500);
}

function inferEntrypoints(pkg: SkillPackage): string[] {
  const yaml = readSkillYaml(pkg);
  if (yaml?.entrypoints?.length) {
    return yaml.entrypoints.slice(0, 8);
  }
  return [];
}

function inferDeclaredCapabilities(pkg: SkillPackage, instructions: string): Capability[] {
  const yaml = readSkillYaml(pkg);
  if (yaml?.capabilitiesDeclared) {
    return yaml.capabilitiesDeclared.filter((item): item is Capability =>
      (CAPABILITIES as readonly string[]).includes(item),
    );
  }
  if (/\bhttps?:\/\//i.test(instructions) || /\bnetwork\b/i.test(instructions)) {
    return ["network.read"];
  }
  return ["filesystem.read"];
}

interface SkillYamlBits {
  name?: string;
  description?: string;
  entrypoints?: string[];
  capabilitiesDeclared?: string[];
  instructions?: string;
  risk?: string;
}

export function readSkillYaml(pkg: SkillPackage): SkillYamlBits | undefined {
  const file = pkg.files.find((item) => /(^|\/)(skill\.ya?ml|SKILL\.md)$/i.test(item.path));
  if (!file) {
    return undefined;
  }
  return parseLooseSkillFile(file);
}

export function parseLooseSkillFile(file: SkillFile): SkillYamlBits {
  const text = file.content;
  const pick = (key: string): string | undefined => {
    const match = text.match(new RegExp(`(?:^|\\n)${key}\\s*:\\s*["']?([^\\n"']+)`, "i"));
    return match?.[1]?.trim();
  };
  const name = pick("name");
  const description = pick("description");
  const instructionsMatch = text.match(/instructions:\s*[|>]?\s*\n([\s\S]{1,8000})/i);
  const instructions =
    instructionsMatch?.[1]
      ?.split("\n")
      .filter((line) => line.startsWith("  ") || line.startsWith("\t") || line.trim() === "")
      .map((line) => line.replace(/^\s{2}|\t/, ""))
      .join("\n")
      .trim() || pick("instructions");
  return {
    name,
    description,
    instructions,
    risk: pick("risk"),
    entrypoints: listBlock(text, "entrypoints"),
    capabilitiesDeclared: listBlock(text, "capabilitiesDeclared"),
  };
}

function listBlock(text: string, key: string): string[] | undefined {
  const re = new RegExp(`${key}:\\s*\\n((?:\\s+-\\s+[^\\n]+\\n?)+)`, "i");
  const match = text.match(re);
  if (!match?.[1]) {
    return undefined;
  }
  return match[1]
    .split("\n")
    .map((line) => line.replace(/^\s*-\s*/, "").trim())
    .filter(Boolean);
}

export function extractInstructions(pkg: SkillPackage): string {
  const yaml = readSkillYaml(pkg);
  if (yaml?.instructions) {
    return truncate(yaml.instructions, MAX_INSTRUCTIONS);
  }
  const readme = pkg.files.find((file) => /(^|\/)README\.md$/i.test(file.path));
  if (readme) {
    return truncate(stripInjectionNoise(readme.content), MAX_INSTRUCTIONS);
  }
  return truncate(`Workflow skill for ${pkg.repository}. Follow compact instructions only.`, MAX_INSTRUCTIONS);
}

export function inferRisk(pkg: SkillPackage): RiskLevel {
  const heuristic = heuristicRisk(pkg);
  const declared = declaredRisk(pkg);
  // Declared risk may raise only — never lower the heuristic floor.
  return declared ? maxRisk(heuristic, declared) : heuristic;
}

const RISK_RANK: Record<RiskLevel, number> = {
  LOW: 0,
  MEDIUM: 1,
  HIGH: 2,
  CRITICAL: 3,
};

export function maxRisk(a: RiskLevel, b: RiskLevel): RiskLevel {
  return RISK_RANK[a] >= RISK_RANK[b] ? a : b;
}

function heuristicRisk(pkg: SkillPackage): RiskLevel {
  if (hasInstallScripts(pkg) || inferEntrypoints(pkg).length > 0) {
    return "HIGH";
  }
  return "LOW";
}

function declaredRisk(pkg: SkillPackage): RiskLevel | undefined {
  const yaml = readSkillYaml(pkg);
  if (!yaml?.risk) {
    return undefined;
  }
  const upper = yaml.risk.toUpperCase();
  if ((RISK_LEVELS as readonly string[]).includes(upper)) {
    return upper as RiskLevel;
  }
  // Malformed declared risk is ignored (safe fallback to heuristic).
  return undefined;
}

export function hasInstallScripts(pkg: SkillPackage): boolean {
  const pkgJson = pkg.files.find((file) => /(^|\/)package\.json$/i.test(file.path));
  if (!pkgJson) {
    return false;
  }
  try {
    const parsed = JSON.parse(pkgJson.content) as { scripts?: Record<string, string> };
    const scripts = parsed.scripts ?? {};
    return Boolean(scripts.preinstall || scripts.postinstall || scripts.install);
  } catch {
    return false;
  }
}

export function isExecutableSkill(pkg: SkillPackage, manifest: SkillManifest): boolean {
  return manifest.spec.entrypoints.length > 0 || hasInstallScripts(pkg);
}

export function isLockfile(path: string): boolean {
  return /(package-lock\.json|pnpm-lock\.yaml|yarn\.lock|Cargo\.lock|poetry\.lock|go\.sum|requirements\.txt)$/i.test(
    path,
  );
}

export function truncate(text: string, max: number): string {
  if (text.length <= max) {
    return text;
  }
  return `${text.slice(0, max - 16)}\n…[truncated]`;
}

function stripInjectionNoise(text: string): string {
  return text.replace(/<!--[\s\S]*?-->/g, "").slice(0, MAX_INSTRUCTIONS);
}
