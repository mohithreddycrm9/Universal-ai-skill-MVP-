import { existsSync, readFileSync } from "node:fs";
import { parse as parseYaml } from "yaml";
import type { SkillCandidate, SkillPackage } from "../types.js";
import { SkillMcpError } from "../errors.js";

export interface CatalogEntry {
  name: string;
  description: string;
  publisher: string;
  repository: string;
  repositoryUrl: string;
  defaultRef: string;
  commitSha: string;
  license?: string;
  archived?: boolean;
  files: Array<{ path: string; content: string }>;
}

export function loadCatalogFile(path: string): CatalogEntry[] {
  if (!existsSync(path)) {
    return [];
  }
  const raw = parseYaml(readFileSync(path, "utf8")) ?? {};
  const list = Array.isArray(raw)
    ? raw
    : Array.isArray((raw as { entries?: unknown }).entries)
      ? (raw as { entries: unknown[] }).entries
      : Array.isArray((raw as { servers?: unknown }).servers)
        ? (raw as { servers: unknown[] }).servers
        : [];
  return list.map((item, index) => normalizeEntry(item, index, path)).filter((entry): entry is CatalogEntry => entry !== undefined);
}

function normalizeEntry(item: unknown, index: number, path: string): CatalogEntry | undefined {
  if (!item || typeof item !== "object") {
    return undefined;
  }
  const row = item as Record<string, unknown>;
  const name = stringField(row.name) ?? stringField(row.id);
  const repositoryUrl = stringField(row.repositoryUrl) ?? stringField(row.url) ?? stringField(row.repository_url);
  const repository = stringField(row.repository) ?? name;
  const publisher = stringField(row.publisher) ?? stringField(row.owner) ?? "unknown";
  const commitSha = stringField(row.commitSha) ?? stringField(row.commit) ?? stringField(row.sha);
  if (!name || !repositoryUrl || !repository || !commitSha) {
    throw new SkillMcpError("INVALID_INPUT", `Catalog entry #${index} in ${path} is missing name/repositoryUrl/commitSha`);
  }
  const filesRaw = Array.isArray(row.files) ? row.files : [];
  const files = filesRaw
    .map((file) => {
      if (!file || typeof file !== "object") {
        return undefined;
      }
      const rec = file as Record<string, unknown>;
      const filePath = stringField(rec.path);
      const content = stringField(rec.content);
      if (!filePath || content === undefined) {
        return undefined;
      }
      return { path: filePath, content };
    })
    .filter((file): file is { path: string; content: string } => file !== undefined);
  return {
    name,
    description: stringField(row.description) ?? name,
    publisher,
    repository,
    repositoryUrl,
    defaultRef: stringField(row.defaultRef) ?? commitSha,
    commitSha,
    license: stringField(row.license),
    archived: row.archived === true,
    files,
  };
}

function stringField(value: unknown): string | undefined {
  return typeof value === "string" && value.trim() ? value : undefined;
}

export function entryToCandidate(entry: CatalogEntry, sourceId: string): SkillCandidate {
  const { owner, repo } = ownerRepoFromRepository(entry.repository);
  return {
    candidateId: `${sourceId}:${entry.repository}`,
    sourceId,
    name: entry.name,
    description: entry.description,
    publisher: entry.publisher,
    repository: entry.repository,
    repositoryUrl: entry.repositoryUrl,
    defaultRef: entry.defaultRef,
    owner,
    repo,
    commit: entry.commitSha,
    metadata: { catalog: true },
  };
}

function ownerRepoFromRepository(repository: string): { owner?: string; repo?: string } {
  const trimmed = repository.trim().replace(/\.git$/i, "");
  const parts = trimmed.split("/").filter(Boolean);
  if (parts.length >= 2) {
    return { owner: parts[0], repo: parts.slice(1).join("/") };
  }
  return {};
}

export function entryToPackage(entry: CatalogEntry, sourceId: string): SkillPackage {
  if (!entry.files.length) {
    throw new SkillMcpError(
      "SOURCE_ERROR",
      `Catalog entry ${entry.repository} has no files. Skills are instruction packages, not remote API execution.`,
    );
  }
  if (!entry.commitSha || /^(latest|main|master|head)$/i.test(entry.commitSha)) {
    throw new SkillMcpError("SOURCE_ERROR", "Catalog entry must pin a commit SHA; refusing floating refs");
  }
  return {
    sourceId,
    publisher: entry.publisher,
    ownerLogin: entry.publisher,
    repository: entry.repository,
    repositoryUrl: entry.repositoryUrl,
    commitSha: entry.commitSha,
    version: entry.commitSha.slice(0, 12),
    license: entry.license,
    archived: Boolean(entry.archived),
    files: entry.files,
  };
}

export function matchCatalog(entries: CatalogEntry[], query: string, limit: number): CatalogEntry[] {
  const q = query.toLowerCase();
  const hits: CatalogEntry[] = [];
  for (const entry of entries) {
    const blob = `${entry.name} ${entry.description} ${entry.publisher} ${entry.repository}`.toLowerCase();
    if (!q || blob.includes(q)) {
      hits.push(entry);
    }
    if (hits.length >= limit) {
      break;
    }
  }
  return hits;
}

export function findCatalogEntry(entries: CatalogEntry[], repositoryUrl: string, repository?: string): CatalogEntry | undefined {
  const url = repositoryUrl.trim().toLowerCase().replace(/\.git$/, "");
  const repo = repository?.trim().toLowerCase();
  return entries.find((entry) => {
    const entryUrl = entry.repositoryUrl.trim().toLowerCase().replace(/\.git$/, "");
    return entryUrl === url || entry.repository.toLowerCase() === url || (repo !== undefined && entry.repository.toLowerCase() === repo);
  });
}
