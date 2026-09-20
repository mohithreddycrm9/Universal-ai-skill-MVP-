import type { DatabaseAdapter } from "./database.js";
import type {
  Capability,
  JobState,
  Lifecycle,
  PersistenceTier,
  QualityStatus,
  RiskLevel,
  ScannerRun,
  SecurityStatus,
  SkillManifest,
  SkillRecord,
  TrustTier,
} from "../types.js";
import { SkillMcpError } from "../errors.js";
import { assertTransition, normalizeLifecycle } from "../skills/lifecycle.js";
import type { Clock } from "../util/clock.js";
import { iso } from "../util/clock.js";
import type { CostApproval, CostApprovalState, CostReview } from "../cost/types.js";

interface SkillRow {
  id: string;
  name: string;
  publisher: string;
  repository: string;
  version: string;
  commit_sha: string;
  fingerprint: string;
  manifest_json: string;
  persistence: PersistenceTier;
  lifecycle: Lifecycle;
  trust_tier: TrustTier;
  security_status: SecurityStatus;
  quality_status: QualityStatus;
  risk: RiskLevel;
  permissions_json: string;
  sbom_summary_json: string | null;
  sandbox_json: string | null;
  expiration_at: string | null;
  created_at: string;
  updated_at: string;
}

interface JobRow {
  id: string;
  skill_id: string | null;
  type: string;
  state: JobState;
  payload_json: string;
  error: string | null;
  created_at: string;
  updated_at: string;
}

export interface JobRecord {
  id: string;
  skillId: string | null;
  type: string;
  state: JobState;
  payload: Record<string, unknown>;
  error: string | null;
  createdAt: string;
  updatedAt: string;
}

export class SkillRegistry {
  constructor(
    private readonly db: DatabaseAdapter,
    private readonly clock: Clock,
  ) {}

  insertSkill(record: SkillRecord): void {
    this.db.run(
      `INSERT INTO skills (
        id, name, publisher, repository, version, commit_sha, fingerprint, manifest_json,
        persistence, lifecycle, trust_tier, security_status, quality_status, risk,
        permissions_json, sbom_summary_json, sandbox_json, expiration_at, created_at, updated_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        record.id,
        record.name,
        record.publisher,
        record.repository,
        record.version,
        record.commitSha,
        record.fingerprint,
        JSON.stringify(record.manifest),
        record.persistence,
        record.lifecycle,
        record.trustTier,
        record.securityStatus,
        record.qualityStatus,
        record.risk,
        JSON.stringify(record.permissions),
        JSON.stringify(record.sbomSummary),
        JSON.stringify(record.sandboxSummary),
        record.expirationAt,
        record.createdAt,
        record.updatedAt,
      ],
    );
  }

  getSkill(id: string): SkillRecord | undefined {
    const row = this.db.get<SkillRow>(`SELECT * FROM skills WHERE id = ?`, [id]);
    return row ? mapSkill(row) : undefined;
  }

  getSkillByFingerprint(fingerprint: string): SkillRecord | undefined {
    const row = this.db.get<SkillRow>(`SELECT * FROM skills WHERE fingerprint = ?`, [fingerprint]);
    return row ? mapSkill(row) : undefined;
  }

  requireSkill(id: string): SkillRecord {
    const skill = this.getSkill(id);
    if (!skill) {
      throw new SkillMcpError("NOT_FOUND", `Skill ${id} not found`);
    }
    return skill;
  }

  listSkills(opts: { lifecycle?: Lifecycle; limit: number; offset: number }): SkillRecord[] {
    if (opts.lifecycle) {
      return this.db
        .all<SkillRow>(
          `SELECT * FROM skills WHERE lifecycle = ? ORDER BY updated_at DESC LIMIT ? OFFSET ?`,
          [opts.lifecycle, opts.limit, opts.offset],
        )
        .map(mapSkill);
    }
    return this.db
      .all<SkillRow>(`SELECT * FROM skills ORDER BY updated_at DESC LIMIT ? OFFSET ?`, [opts.limit, opts.offset])
      .map(mapSkill);
  }

  searchSkills(query: string, limit: number): SkillRecord[] {
    const like = `%${query}%`;
    return this.db
      .all<SkillRow>(
        `SELECT * FROM skills WHERE name LIKE ? OR publisher LIKE ? OR repository LIKE ? OR manifest_json LIKE ?
         ORDER BY updated_at DESC LIMIT ?`,
        [like, like, like, like, limit],
      )
      .map(mapSkill);
  }

  updateSkill(
    id: string,
    patch: Partial<{
      lifecycle: Lifecycle;
      trustTier: TrustTier;
      securityStatus: SecurityStatus;
      qualityStatus: QualityStatus;
      persistence: PersistenceTier;
      permissions: Capability[];
      sbomSummary: unknown;
      sandboxSummary: unknown;
      expirationAt: string | null;
      commitSha: string;
      fingerprint: string;
      manifest: SkillManifest;
      version: string;
    }>,
  ): SkillRecord {
    const current = this.requireSkill(id);
    if (patch.lifecycle) {
      assertTransition(current.lifecycle, patch.lifecycle);
    }
    const next: SkillRecord = {
      ...current,
      ...patch,
      updatedAt: iso(this.clock),
    };
    this.db.run(
      `UPDATE skills SET
        name=?, publisher=?, repository=?, version=?, commit_sha=?, fingerprint=?, manifest_json=?,
        persistence=?, lifecycle=?, trust_tier=?, security_status=?, quality_status=?, risk=?,
        permissions_json=?, sbom_summary_json=?, sandbox_json=?, expiration_at=?, updated_at=?
       WHERE id=?`,
      [
        next.name,
        next.publisher,
        next.repository,
        next.version,
        next.commitSha,
        next.fingerprint,
        JSON.stringify(next.manifest),
        next.persistence,
        next.lifecycle,
        next.trustTier,
        next.securityStatus,
        next.qualityStatus,
        next.risk,
        JSON.stringify(next.permissions),
        JSON.stringify(next.sbomSummary),
        JSON.stringify(next.sandboxSummary),
        next.expirationAt,
        next.updatedAt,
        id,
      ],
    );
    return next;
  }

  insertScanResult(skillId: string, fingerprint: string, run: ScannerRun): void {
    this.db.run(
      `INSERT INTO scan_results (
        id, skill_id, fingerprint, scanner_id, scanner_version, status, finding_count,
        finding_summary_json, notes, started_at, finished_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT(fingerprint, scanner_id, scanner_version) DO UPDATE SET
        skill_id=excluded.skill_id,
        status=excluded.status,
        finding_count=excluded.finding_count,
        finding_summary_json=excluded.finding_summary_json,
        notes=excluded.notes,
        started_at=excluded.started_at,
        finished_at=excluded.finished_at`,
      [
        `${fingerprint}:${run.scannerId}:${run.scannerVersion}`,
        skillId,
        fingerprint,
        run.scannerId,
        run.scannerVersion,
        run.status,
        run.findings.length,
        JSON.stringify(run.findings.slice(0, 20)),
        run.notes ?? null,
        run.startedAt,
        run.finishedAt,
      ],
    );
  }

  listScanResults(fingerprint: string): ScannerRun[] {
    return this.db
      .all<{
        scanner_id: string;
        scanner_version: string;
        status: SecurityStatus;
        finding_summary_json: string;
        notes: string | null;
        started_at: string;
        finished_at: string;
      }>(`SELECT * FROM scan_results WHERE fingerprint = ?`, [fingerprint])
      .map((row) => ({
        scannerId: row.scanner_id,
        scannerVersion: row.scanner_version,
        status: row.status,
        findings: JSON.parse(row.finding_summary_json) as ScannerRun["findings"],
        notes: row.notes ?? undefined,
        startedAt: row.started_at,
        finishedAt: row.finished_at,
      }));
  }

  insertJob(job: JobRecord): void {
    this.db.run(
      `INSERT INTO jobs (id, skill_id, type, state, payload_json, error, created_at, updated_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        job.id,
        job.skillId,
        job.type,
        job.state,
        JSON.stringify(job.payload),
        job.error,
        job.createdAt,
        job.updatedAt,
      ],
    );
  }

  getJob(id: string): JobRecord | undefined {
    const row = this.db.get<JobRow>(`SELECT * FROM jobs WHERE id = ?`, [id]);
    return row ? mapJob(row) : undefined;
  }

  updateJob(id: string, patch: Partial<Pick<JobRecord, "state" | "error" | "payload">>): JobRecord {
    const current = this.getJob(id);
    if (!current) {
      throw new SkillMcpError("NOT_FOUND", `Job ${id} not found`);
    }
    const next: JobRecord = {
      ...current,
      ...patch,
      updatedAt: iso(this.clock),
    };
    this.db.run(`UPDATE jobs SET state=?, payload_json=?, error=?, updated_at=? WHERE id=?`, [
      next.state,
      JSON.stringify(next.payload),
      next.error,
      next.updatedAt,
      id,
    ]);
    return next;
  }

  nextQueuedJob(): JobRecord | undefined {
    const row = this.db.get<JobRow>(
      `SELECT * FROM jobs WHERE state = 'QUEUED' ORDER BY created_at ASC LIMIT 1`,
    );
    return row ? mapJob(row) : undefined;
  }

  findOpenJob(type: string, fingerprint: string): JobRecord | undefined {
    const rows = this.db.all<JobRow>(
      `SELECT * FROM jobs WHERE type = ? AND state IN ('QUEUED','RUNNING')`,
      [type],
    );
    return rows.map(mapJob).find((job) => job.payload.fingerprint === fingerprint);
  }

  insertAudit(event: {
    id: string;
    requestId: string;
    actor: string;
    action: string;
    skillId?: string;
    fingerprint?: string;
    detail: unknown;
    createdAt: string;
  }): void {
    this.db.run(
      `INSERT INTO audit_events (id, request_id, actor, action, skill_id, fingerprint, detail_json, created_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        event.id,
        event.requestId,
        event.actor,
        event.action,
        event.skillId ?? null,
        event.fingerprint ?? null,
        JSON.stringify(event.detail),
        event.createdAt,
      ],
    );
  }

  listAudit(limit: number, skillId?: string): Array<Record<string, unknown>> {
    const rows = skillId
      ? this.db.all<Record<string, unknown>>(
          `SELECT * FROM audit_events WHERE skill_id = ? ORDER BY created_at DESC LIMIT ?`,
          [skillId, limit],
        )
      : this.db.all<Record<string, unknown>>(`SELECT * FROM audit_events ORDER BY created_at DESC LIMIT ?`, [limit]);
    return rows;
  }

  insertTrustEdge(edge: {
    id: string;
    fromNode: string;
    toNode: string;
    kind: string;
    evidence: unknown;
    createdAt: string;
  }): void {
    this.db.run(
      `INSERT INTO trust_edges (id, from_node, to_node, kind, evidence_json, created_at) VALUES (?, ?, ?, ?, ?, ?)`,
      [edge.id, edge.fromNode, edge.toNode, edge.kind, JSON.stringify(edge.evidence), edge.createdAt],
    );
  }

  listTrustEdges(skillKey: string): Array<{ fromNode: string; toNode: string; kind: string; evidence: unknown }> {
    return this.db
      .all<{ from_node: string; to_node: string; kind: string; evidence_json: string }>(
        `SELECT * FROM trust_edges WHERE from_node LIKE ? OR to_node LIKE ? ORDER BY created_at ASC`,
        [`%${skillKey}%`, `%${skillKey}%`],
      )
      .map((row) => ({
        fromNode: row.from_node,
        toNode: row.to_node,
        kind: row.kind,
        evidence: JSON.parse(row.evidence_json) as unknown,
      }));
  }

  putCache(entry: {
    fingerprint: string;
    skillId: string;
    securityStatus: SecurityStatus;
    scannerVersions: Record<string, string>;
    securityConfigHash: string;
    createdAt: string;
    expiresAt: string;
  }): void {
    this.db.run(
      `INSERT INTO verified_cache (fingerprint, skill_id, security_status, scanner_versions_json, security_config_hash, created_at, expires_at)
       VALUES (?, ?, ?, ?, ?, ?, ?)
       ON CONFLICT(fingerprint) DO UPDATE SET
         skill_id=excluded.skill_id,
         security_status=excluded.security_status,
         scanner_versions_json=excluded.scanner_versions_json,
         security_config_hash=excluded.security_config_hash,
         created_at=excluded.created_at,
         expires_at=excluded.expires_at`,
      [
        entry.fingerprint,
        entry.skillId,
        entry.securityStatus,
        JSON.stringify(entry.scannerVersions),
        entry.securityConfigHash,
        entry.createdAt,
        entry.expiresAt,
      ],
    );
  }

  getCache(fingerprint: string): {
    fingerprint: string;
    skillId: string;
    securityStatus: SecurityStatus;
    scannerVersions: Record<string, string>;
    securityConfigHash: string;
    expiresAt: string;
  } | undefined {
    const row = this.db.get<{
      fingerprint: string;
      skill_id: string;
      security_status: SecurityStatus;
      scanner_versions_json: string;
      security_config_hash: string;
      expires_at: string;
    }>(`SELECT * FROM verified_cache WHERE fingerprint = ?`, [fingerprint]);
    if (!row) {
      return undefined;
    }
    return {
      fingerprint: row.fingerprint,
      skillId: row.skill_id,
      securityStatus: row.security_status,
      scannerVersions: JSON.parse(row.scanner_versions_json) as Record<string, string>,
      securityConfigHash: row.security_config_hash,
      expiresAt: row.expires_at,
    };
  }

  insertCostApproval(record: CostApproval): void {
    this.db.run(
      `INSERT INTO cost_approvals (id, operation, provider, service, status, approver, review_json, created_at, updated_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        record.id,
        record.operation,
        record.provider,
        record.service,
        record.status,
        record.approver,
        JSON.stringify(record.review),
        record.createdAt,
        record.updatedAt,
      ],
    );
  }

  getCostApproval(id: string): CostApproval | undefined {
    const row = this.db.get<{
      id: string;
      operation: string;
      provider: string;
      service: string;
      status: CostApprovalState;
      approver: string | null;
      review_json: string;
      created_at: string;
      updated_at: string;
    }>(`SELECT * FROM cost_approvals WHERE id = ?`, [id]);
    return row ? mapCost(row) : undefined;
  }

  findOpenCostApproval(operation: string, provider: string, service: string): CostApproval | undefined {
    const row = this.db.get<{
      id: string;
      operation: string;
      provider: string;
      service: string;
      status: CostApprovalState;
      approver: string | null;
      review_json: string;
      created_at: string;
      updated_at: string;
    }>(
      `SELECT * FROM cost_approvals WHERE operation = ? AND provider = ? AND service = ? AND status IN ('PENDING','APPROVED') ORDER BY created_at DESC LIMIT 1`,
      [operation, provider, service],
    );
    return row ? mapCost(row) : undefined;
  }

  listCostApprovals(status?: CostApprovalState): CostApproval[] {
    const rows = status
      ? this.db.all<{
          id: string;
          operation: string;
          provider: string;
          service: string;
          status: CostApprovalState;
          approver: string | null;
          review_json: string;
          created_at: string;
          updated_at: string;
        }>(`SELECT * FROM cost_approvals WHERE status = ? ORDER BY created_at DESC`, [status])
      : this.db.all<{
          id: string;
          operation: string;
          provider: string;
          service: string;
          status: CostApprovalState;
          approver: string | null;
          review_json: string;
          created_at: string;
          updated_at: string;
        }>(`SELECT * FROM cost_approvals ORDER BY created_at DESC LIMIT 100`);
    return rows.map(mapCost);
  }

  updateCostApproval(id: string, status: CostApprovalState, approver: string): CostApproval {
    const current = this.getCostApproval(id);
    if (!current) {
      throw new SkillMcpError("NOT_FOUND", `Cost approval ${id} not found`);
    }
    const updatedAt = iso(this.clock);
    this.db.run(`UPDATE cost_approvals SET status=?, approver=?, updated_at=? WHERE id=?`, [
      status,
      approver,
      updatedAt,
      id,
    ]);
    return { ...current, status, approver, updatedAt };
  }
}

function mapSkill(row: SkillRow): SkillRecord {
  return {
    id: row.id,
    name: row.name,
    publisher: row.publisher,
    repository: row.repository,
    version: row.version,
    commitSha: row.commit_sha,
    fingerprint: row.fingerprint,
    manifest: JSON.parse(row.manifest_json) as SkillManifest,
    persistence: row.persistence,
    lifecycle: normalizeLifecycle(row.lifecycle),
    trustTier: row.trust_tier,
    securityStatus: row.security_status,
    qualityStatus: row.quality_status,
    risk: row.risk,
    permissions: JSON.parse(row.permissions_json) as Capability[],
    sbomSummary: row.sbom_summary_json ? JSON.parse(row.sbom_summary_json) : null,
    sandboxSummary: row.sandbox_json ? JSON.parse(row.sandbox_json) : null,
    expirationAt: row.expiration_at,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
  };
}

function mapJob(row: JobRow): JobRecord {
  return {
    id: row.id,
    skillId: row.skill_id,
    type: row.type,
    state: row.state,
    payload: JSON.parse(row.payload_json) as Record<string, unknown>,
    error: row.error,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
  };
}

function mapCost(row: {
  id: string;
  operation: string;
  provider: string;
  service: string;
  status: CostApprovalState;
  approver: string | null;
  review_json: string;
  created_at: string;
  updated_at: string;
}): CostApproval {
  return {
    id: row.id,
    operation: row.operation,
    provider: row.provider,
    service: row.service,
    status: row.status,
    approver: row.approver,
    review: JSON.parse(row.review_json) as CostReview,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
  };
}
