import { parse as parseYaml } from "yaml";
import type { McpSkill, SkillFile, SkillManifest, SkillPackage } from "../types.js";
import { sha256 } from "../util/hash.js";
import { extractInstructions, inferName, inferRisk, manifestFromPackage, truncate } from "./manifest.js";

const MAX_BODY = 8 * 1024;

/**
 * Standards-first MCP / Agent Skill representation.
 * Custom skill.mcp/v1 manifests are adapted here for serve/discovery — they are not deleted.
 */
export function customManifestToMcpSkill(manifest: SkillManifest, pkg?: SkillPackage): McpSkill {
  const skillMd = pkg?.files.find((file) => /(^|\/)SKILL\.md$/i.test(file.path));
  const parsed = skillMd ? parseSkillMd(skillMd.content) : undefined;
  const resources = (pkg?.files ?? [])
    .filter((file) => !/(^|\/)SKILL\.md$/i.test(file.path))
    .map((file) => ({
      path: file.path,
      digest: sha256(file.content),
      mimeType: guessMime(file.path),
    }));
  return {
    name: parsed?.name ?? manifest.metadata.name,
    description: parsed?.description ?? manifest.metadata.description,
    version: manifest.metadata.version,
    body: parsed?.body ?? manifest.spec.instructions,
    resources,
  };
}

export function parseSkillMd(content: string): { name?: string; description?: string; body: string } {
  const match = content.match(/^---\s*\n([\s\S]*?)\n---\s*\n?([\s\S]*)$/);
  if (!match) {
    return { body: truncate(content, MAX_BODY) };
  }
  let name: string | undefined;
  let description: string | undefined;
  try {
    const front = parseYaml(match[1] ?? "") as Record<string, unknown>;
    if (typeof front.name === "string") {
      name = front.name;
    }
    if (typeof front.description === "string") {
      description = front.description;
    }
  } catch {
    name = undefined;
  }
  return { name, description, body: truncate(match[2] ?? "", MAX_BODY) };
}

export function mcpSkillToCustomManifest(pkg: SkillPackage, skill: McpSkill): SkillManifest {
  const instructions = skill.body || extractInstructions(pkg);
  return manifestFromPackage(pkg, instructions, inferRisk(pkg));
}

export function packageToMcpSkill(pkg: SkillPackage): McpSkill {
  const skillMd = pkg.files.find((file) => /(^|\/)SKILL\.md$/i.test(file.path));
  const parsed = skillMd ? parseSkillMd(skillMd.content) : undefined;
  const name = parsed?.name ?? inferName(pkg);
  const body = parsed?.body ?? extractInstructions(pkg);
  const description = parsed?.description ?? body.split("\n").find((line) => line.trim()) ?? name;
  return {
    name,
    description: truncate(description, 500),
    version: pkg.version || pkg.commitSha.slice(0, 12),
    body,
    resources: pkg.files
      .filter((file) => !/(^|\/)SKILL\.md$/i.test(file.path))
      .map((file) => ({ path: file.path, digest: sha256(file.content), mimeType: guessMime(file.path) })),
  };
}

export function filesDigest(files: SkillFile[]): string {
  const parts = [...files]
    .map((file) => `${file.path}:${sha256(file.content)}`)
    .sort();
  return sha256(parts.join("|"));
}

function guessMime(path: string): string {
  if (/\.md$/i.test(path)) {
    return "text/markdown";
  }
  if (/\.json$/i.test(path)) {
    return "application/json";
  }
  if (/\.ya?ml$/i.test(path)) {
    return "text/yaml";
  }
  return "text/plain";
}
