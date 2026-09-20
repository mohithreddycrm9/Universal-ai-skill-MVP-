import type { SkillCandidate, SkillFile, SkillPackage } from "../types.js";
import { SkillMcpError } from "../errors.js";
import { truncate } from "../skills/manifest.js";
import type { DiscoveryQuery, PinnedRef, SkillRef, SkillSource } from "./skill-source.js";

const MAX_FILE_BYTES = 64 * 1024;
const MAX_FILES = 20;
const MAX_TOTAL_BYTES = 256 * 1024;
const INTERESTING = [
  "skill.yaml",
  "skill.yml",
  "SKILL.md",
  "README.md",
  "package.json",
  "package-lock.json",
  "pnpm-lock.yaml",
  "yarn.lock",
  "requirements.txt",
  "pyproject.toml",
  "go.mod",
  "go.sum",
  "Cargo.lock",
  "LICENSE",
  "LICENSE.md",
];

export type FetchLike = (url: string, init?: RequestInit) => Promise<Response>;

export class GitHubSource implements SkillSource {
  readonly id = "github";

  constructor(
    private readonly apiBase: string,
    private readonly token: string | undefined,
    private readonly fetchImpl: FetchLike = fetch,
  ) {}

  async search(query: DiscoveryQuery): Promise<SkillCandidate[]> {
    const q = encodeURIComponent(`${query.query} in:name,description`);
    const url = `${this.apiBase}/search/repositories?q=${q}&per_page=${Math.min(query.limit, 10)}`;
    const json = await this.getJson<{
      items?: Array<{
        full_name: string;
        description: string | null;
        html_url: string;
        owner: { login: string };
        default_branch: string;
      }>;
    }>(url);
    return (json.items ?? []).map((item) => ({
      candidateId: `github:${item.full_name}`,
      sourceId: this.id,
      name: item.full_name.split("/")[1] ?? item.full_name,
      description: truncate(item.description ?? item.full_name, 240),
      publisher: item.owner.login,
      repository: item.full_name,
      repositoryUrl: item.html_url,
      defaultRef: item.default_branch,
    }));
  }

  async pin(ref: SkillRef): Promise<PinnedRef> {
    const { owner, repo } = parseRepo(ref);
    const target = ref.ref ?? (await this.defaultBranch(owner, repo));
    const commit = await this.getJson<{ sha: string }>(`${this.apiBase}/repos/${owner}/${repo}/commits/${target}`);
    if (!commit.sha) {
      throw new SkillMcpError("SOURCE_ERROR", "GitHub did not return a commit SHA; refusing to pin 'latest'");
    }
    return {
      repositoryUrl: ref.repositoryUrl,
      commitSha: commit.sha,
      ref: target,
    };
  }

  async fetch(ref: SkillRef): Promise<SkillPackage> {
    const { owner, repo } = parseRepo(ref);
    const pinned = await this.pin(ref);
    const meta = await this.getJson<{
      owner: { login: string };
      html_url: string;
      archived: boolean;
      created_at: string;
      license: { spdx_id?: string } | null;
      default_branch: string;
    }>(`${this.apiBase}/repos/${owner}/${repo}`);
    const files: SkillFile[] = [];
    let total = 0;
    for (const name of INTERESTING) {
      if (files.length >= MAX_FILES || total >= MAX_TOTAL_BYTES) {
        break;
      }
      const content = await this.tryFile(owner, repo, name, pinned.commitSha);
      if (content === undefined) {
        continue;
      }
      const clipped = content.length > MAX_FILE_BYTES ? `${content.slice(0, MAX_FILE_BYTES)}\n…[truncated]` : content;
      files.push({ path: name, content: clipped });
      total += clipped.length;
    }
    return {
      sourceId: this.id,
      publisher: meta.owner.login,
      ownerLogin: meta.owner.login,
      repository: `${owner}/${repo}`,
      repositoryUrl: meta.html_url,
      commitSha: pinned.commitSha,
      version: pinned.commitSha.slice(0, 12),
      license: meta.license?.spdx_id,
      archived: Boolean(meta.archived),
      createdAt: meta.created_at,
      files,
    };
  }

  private async defaultBranch(owner: string, repo: string): Promise<string> {
    const meta = await this.getJson<{ default_branch: string }>(`${this.apiBase}/repos/${owner}/${repo}`);
    return meta.default_branch ?? "main";
  }

  private async tryFile(owner: string, repo: string, path: string, sha: string): Promise<string | undefined> {
    const url = `${this.apiBase}/repos/${owner}/${repo}/contents/${path}?ref=${encodeURIComponent(sha)}`;
    const response = await this.fetchImpl(url, { headers: this.headers() });
    if (response.status === 404) {
      return undefined;
    }
    if (!response.ok) {
      return undefined;
    }
    const json = (await response.json()) as { encoding?: string; content?: string; size?: number };
    if (typeof json.content !== "string") {
      return undefined;
    }
    if (json.encoding === "base64") {
      return Buffer.from(json.content.replace(/\n/g, ""), "base64").toString("utf8");
    }
    return json.content;
  }

  private async getJson<T>(url: string): Promise<T> {
    const response = await this.fetchImpl(url, { headers: this.headers() });
    if (!response.ok) {
      throw new SkillMcpError("SOURCE_ERROR", `GitHub request failed: ${response.status}`, { url: safeUrl(url) });
    }
    return (await response.json()) as T;
  }

  private headers(): Record<string, string> {
    const headers: Record<string, string> = {
      Accept: "application/vnd.github+json",
      "User-Agent": "universal-skill-trust-mcp",
    };
    if (this.token) {
      headers.Authorization = `Bearer ${this.token}`;
    }
    return headers;
  }
}

export function parseRepo(ref: SkillRef): { owner: string; repo: string } {
  if (ref.owner && ref.repo) {
    return { owner: ref.owner, repo: ref.repo };
  }
  const match = ref.repositoryUrl.match(/github\.com[:/]([^/]+)\/([^/#]+)/i);
  if (!match || !match[1] || !match[2]) {
    throw new SkillMcpError("INVALID_INPUT", "Unable to parse GitHub repository URL");
  }
  return { owner: match[1], repo: match[2].replace(/\.git$/i, "") };
}

function safeUrl(url: string): string {
  return url.split("?")[0] ?? url;
}
