import { mkdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import type { AppConfig } from "./policy/load.js";
import { loadConfig, securityConfigurationHash } from "./policy/load.js";
import { SqliteAdapter } from "./registry/sqlite.js";
import { SkillRegistry, type JobRecord } from "./registry/skill-registry.js";
import type { SkillSource } from "./discovery/skill-source.js";
import { GitHubSource } from "./discovery/github.js";
import { LocalSource } from "./discovery/local-source.js";
import { rankCandidates } from "./discovery/rank.js";
import { AllowlistTrustProvider, TrustBroker } from "./trust/broker.js";
import { TrustGraph } from "./trust/graph.js";
import { describeTrustTier, type TrustDecision } from "./trust/publisher.js";
import { SecurityOrchestrator } from "./security/orchestrator.js";
import { SecretScanner } from "./scanners/secret.js";
import { PromptInjectionScanner } from "./scanners/prompt-injection.js";
import { SuspiciousFilesScanner } from "./scanners/suspicious-files.js";
import { DependencyScanner } from "./scanners/dependency.js";
import { LicenseScanner } from "./scanners/license.js";
import { StrixScanner } from "./scanners/strix.js";
import { SnykScanner } from "./scanners/snyk.js";
import { OssBinaryScanner } from "./scanners/oss-binary.js";
import { InProcessSandbox } from "./sandbox/in-process.js";
import type { SandboxProvider } from "./sandbox/provider.js";
import { CapabilityFirewall } from "./capabilities/firewall.js";
import { VerificationCache } from "./cache/verification-cache.js";
import { AuditLog } from "./audit/audit-log.js";
import { Logger, loggerFromEnv } from "./observability/log.js";
import { Metrics } from "./observability/metrics.js";
import { canonicalize } from "./util/canonical.js";
import { computeFingerprint } from "./skills/fingerprint.js";
import {
  extractInstructions,
  inferRisk,
  isExecutableSkill,
  manifestFromPackage,
} from "./skills/manifest.js";
import { customManifestToMcpSkill, filesDigest } from "./skills/mcp-skill.js";
import { disclose } from "./skills/disclosure.js";
import { describeLifecycle, normalizeLifecycle } from "./skills/lifecycle.js";
import type { Clock } from "./util/clock.js";
import { iso, systemClock } from "./util/clock.js";
import { newId } from "./util/ids.js";
import { SkillMcpError } from "./errors.js";
import type {
  DisclosureLevel,
  JobType,
  SkillCandidate,
  SkillPackage,
  SkillRecord,
  TrustTier,
} from "./types.js";
import { SECURITY_NOTICE } from "./types.js";
import { CostDetector } from "./cost/detector.js";
import { COST_CATALOG } from "./cost/catalog.js";
import type { CostApproval, CostDecision, CostMetadata, CostReview } from "./cost/types.js";

export interface GatewayOptions {
  config?: AppConfig;
  configDir?: string;
  dataDir?: string;
  sqlitePath?: string;
  clock?: Clock;
  logger?: Logger;
  sources?: SkillSource[];
  localSource?: LocalSource;
  sandbox?: SandboxProvider;
}

export class SkillTrustGateway {
  readonly config: AppConfig;
  readonly registry: SkillRegistry;
  readonly metrics = new Metrics();
  readonly localSource: LocalSource;
  private readonly clock: Clock;
  private readonly logger: Logger;
  private readonly sources: SkillSource[];
  private readonly broker: TrustBroker;
  private readonly graph: TrustGraph;
  private readonly orchestrator: SecurityOrchestrator;
  private readonly sandbox: SandboxProvider;
  private readonly firewall = new CapabilityFirewall();
  private readonly cache: VerificationCache;
  private readonly audit: AuditLog;
  private readonly costDetector: CostDetector;
  private readonly packages = new Map<string, SkillPackage>();
  private readonly dataDir: string;
  private readonly configHash: string;

  constructor(opts: GatewayOptions = {}) {
    this.clock = opts.clock ?? systemClock;
    this.logger = opts.logger ?? loggerFromEnv();
    this.config = opts.config ?? loadConfig(opts.configDir ?? process.env.SKILL_MCP_CONFIG_DIR ?? "config");
    this.dataDir = opts.dataDir ?? process.env.SKILL_MCP_DATA_DIR ?? this.config.registry.dataDir;
    const sqlitePath = opts.sqlitePath ?? (this.dataDir === ":memory:" ? ":memory:" : join(this.dataDir, "skill-mcp.sqlite"));
    const db = new SqliteAdapter(sqlitePath);
    this.registry = new SkillRegistry(db, this.clock);
    this.audit = new AuditLog(this.registry, this.clock);
    this.cache = new VerificationCache(this.registry, this.clock);
    this.graph = new TrustGraph(this.registry, this.clock);
    this.broker = new TrustBroker([new AllowlistTrustProvider(this.config.trust)]);
    this.costDetector = new CostDetector(this.config.cost);
    this.localSource = opts.localSource ?? new LocalSource();
    const github = new GitHubSource(
      this.config.registry.sources.find((item) => item.id === "github")?.apiBase ?? "https://api.github.com",
      process.env.GITHUB_TOKEN,
    );
    this.sources = opts.sources ?? [this.localSource, github];
    this.orchestrator = new SecurityOrchestrator(
      [
        new SecretScanner(this.clock),
        new PromptInjectionScanner(this.clock),
        new SuspiciousFilesScanner(this.clock),
        new DependencyScanner(this.clock),
        new LicenseScanner(this.clock),
        new StrixScanner(this.clock),
        new OssBinaryScanner("semgrep", "adapter-1.0.0", COST_CATALOG.semgrep, "semgrep", this.clock),
        new OssBinaryScanner("gitleaks", "adapter-1.0.0", COST_CATALOG.gitleaks, "gitleaks", this.clock),
        new OssBinaryScanner("trivy", "adapter-1.0.0", COST_CATALOG.trivy, "trivy", this.clock),
        new OssBinaryScanner("clamav", "adapter-1.0.0", COST_CATALOG.clamav, "clamscan", this.clock),
        new OssBinaryScanner("osv", "adapter-1.0.0", COST_CATALOG.osv, "osv-scanner", this.clock),
        new OssBinaryScanner("syft", "adapter-1.0.0", COST_CATALOG.syft, "syft", this.clock),
        new SnykScanner(this.clock),
      ],
      this.config,
      this.clock,
    );
    this.sandbox = opts.sandbox ?? new InProcessSandbox();
    this.configHash = securityConfigurationHash(this.config);
  }

  registerPackage(pkg: SkillPackage): void {
    this.localSource.register(pkg);
  }

  async discover(input: { query: string; domain?: string; limit?: number; requestId: string }): Promise<unknown> {
    const limit = Math.min(input.limit ?? 8, 20);
    const localHits = this.registry
      .searchSkills(input.query, limit)
      .filter((skill) => normalizeLifecycle(skill.lifecycle) === "AVAILABLE");
    const remote: SkillCandidate[] = [];
    const skipped: Array<{ source: string; reason: string }> = [];
    for (const source of this.sources) {
      const cost = source instanceof GitHubSource ? source.costFor() : source.cost;
      const decision = this.evaluateCost("discover_skill", cost);
      if (!decision.proceed) {
        skipped.push({
          source: source.id,
          reason: decision.kind === "DENIED" ? decision.reason : "Potentially billable source skipped pending approval",
        });
        continue;
      }
      try {
        remote.push(...(await source.search({ query: input.query, domain: input.domain, limit })));
      } catch (error) {
        this.logger.warn("source_search_failed", { source: source.id, error: String(error) });
      }
    }
    const ranked = rankCandidates(remote, (candidate) => this.guessTier(candidate));
    this.metrics.inc("discover");
    this.audit.record({
      requestId: input.requestId,
      actor: "agent",
      action: "discover_skill",
      detail: { query: input.query, hits: ranked.length },
    });
    return {
      securityNotice: SECURITY_NOTICE,
      localVerified: localHits.map((skill) => this.cardMeta(skill)),
      candidates: ranked.slice(0, limit).map((item) => ({
        ...item,
        note: "Candidate only. Not trusted until the gateway pipeline completes. Skills explain HOW; other tools execute.",
      })),
      skippedSources: skipped,
    };
  }

  async acquire(input: {
    query?: string;
    candidateId?: string;
    repositoryUrl?: string;
    wait?: boolean;
    approvalId?: string;
    requestId: string;
  }): Promise<unknown> {
    let pkg: SkillPackage;
    try {
      pkg = await this.resolvePackage(input);
    } catch (error) {
      if (error instanceof SkillMcpError && error.code === "COST_APPROVAL_REQUIRED") {
        const metadata = error.details.metadata as CostMetadata;
        const review = error.details.review as CostReview;
        const pending = this.ensurePending("acquire_skill", metadata, review);
        this.audit.record({
          requestId: input.requestId,
          actor: "agent",
          action: "cost_approval_required",
          detail: { approvalId: pending.id, provider: metadata.provider, service: metadata.service },
        });
        return {
          status: "NEEDS_COST_APPROVAL",
          approvalId: pending.id,
          review,
          message: "Operation not started. Explicit human approval is required. You are not required to approve.",
        };
      }
      throw error;
    }
    this.assertPinned(pkg);
    const manifest = manifestFromPackage(pkg, extractInstructions(pkg), inferRisk(pkg));
    const fingerprint = this.fingerprintFor(pkg, manifest);
    const cached = this.cache.get(fingerprint);
    if (cached) {
      const existing = this.registry.getSkillByFingerprint(fingerprint);
      this.metrics.inc("cache_hit");
      return {
        status: "CACHE_HIT",
        skillId: existing?.id ?? cached.skillId,
        fingerprint,
        lifecycle: existing ? normalizeLifecycle(existing.lifecycle) : "AVAILABLE",
        securityNotice: SECURITY_NOTICE,
        message: "Verified cache hit for this fingerprint. Scan not repeated.",
      };
    }
    this.metrics.inc("cache_miss");
    const existingOpen = this.registry.findOpenJob("ACQUIRE", fingerprint);
    if (existingOpen) {
      this.metrics.inc("job_dedup");
      return {
        status: "DEDUPLICATED",
        jobId: existingOpen.id,
        skillId: existingOpen.skillId,
        fingerprint,
        message: "Duplicate acquisition collapsed into an existing job.",
      };
    }
    const now = iso(this.clock);
    const skillId = newId("skl");
    this.packages.set(skillId, pkg);
    this.writeQuarantine(skillId, pkg);
    this.registry.insertSkill({
      id: skillId,
      name: manifest.metadata.name,
      publisher: pkg.publisher,
      repository: pkg.repositoryUrl,
      version: manifest.metadata.version,
      commitSha: pkg.commitSha,
      fingerprint,
      manifest,
      persistence: "TEMPORARY",
      lifecycle: "DISCOVERED",
      trustTier: "UNKNOWN",
      securityStatus: "NOT_RUN",
      qualityStatus: "UNKNOWN",
      risk: manifest.spec.risk,
      permissions: [],
      sbomSummary: null,
      sandboxSummary: null,
      expirationAt: null,
      createdAt: now,
      updatedAt: now,
    });
    this.registry.updateSkill(skillId, { lifecycle: "QUARANTINED" });
    const jobId = this.enqueue("ACQUIRE", skillId, fingerprint, { query: input.query ?? null });
    this.audit.record({
      requestId: input.requestId,
      actor: "agent",
      action: "acquire_skill",
      skillId,
      fingerprint,
      detail: { jobId, async: !input.wait },
    });
    if (input.wait) {
      await this.processJobs(20);
    }
    const skill = this.registry.requireSkill(skillId);
    return {
      status: input.wait ? normalizeLifecycle(skill.lifecycle) : "ACCEPTED",
      skillId,
      jobId,
      fingerprint,
      lifecycle: normalizeLifecycle(skill.lifecycle),
      securityNotice: SECURITY_NOTICE,
      message: input.wait
        ? "Pipeline processed in-process for this call."
        : "Acquisition queued. User path is not blocked on full scans.",
    };
  }

  async verify(input: { skillId: string; requestId: string }): Promise<unknown> {
    const skill = this.registry.requireSkill(input.skillId);
    const pkg = this.requirePackage(skill.id);
    await this.runProvenance(skill, pkg, input.requestId);
    return this.getSkillTrust(input);
  }

  async scan(input: {
    skillId: string;
    wait?: boolean;
    includePaidScanners?: boolean;
    approvalId?: string;
    requestId: string;
  }): Promise<unknown> {
    const skill = this.registry.requireSkill(input.skillId);
    if (input.includePaidScanners) {
      const decision = this.evaluateCost("scan_skill:snyk", COST_CATALOG.snyk, input.approvalId);
      if (!decision.proceed) {
        if (decision.kind === "DENIED") {
          throw new SkillMcpError("POLICY_DENIED", decision.reason);
        }
        const pending = this.ensurePending("scan_skill:snyk", COST_CATALOG.snyk, decision.review);
        return {
          status: "NEEDS_COST_APPROVAL",
          approvalId: pending.id,
          review: pending.review,
          note: "Commercial scanners are not an automatic fallback. Free/OSS scanners still run.",
        };
      }
    }
    this.enqueue("SECURITY_SCAN", skill.id, skill.fingerprint, {
      includePaidScanners: Boolean(input.includePaidScanners),
    });
    if (input.wait) {
      await this.processJobs(20);
    }
    return this.getSkillSecurity({ skillId: input.skillId, requestId: input.requestId });
  }

  getSkill(input: {
    skillId: string;
    level?: DisclosureLevel;
    resourcePath?: string;
    requestId: string;
  }): unknown {
    const skill = this.registry.requireSkill(input.skillId);
    const pkg = this.packages.get(skill.id);
    const mcp = customManifestToMcpSkill(skill.manifest, pkg);
    const level = (input.level ?? 0) as DisclosureLevel;
    const body = input.resourcePath ? pkg?.files.find((file) => file.path === input.resourcePath)?.content : undefined;
    this.audit.record({
      requestId: input.requestId,
      actor: "agent",
      action: "get_skill",
      skillId: skill.id,
      fingerprint: skill.fingerprint,
      detail: { level },
    });
    return disclose(skill, mcp, level, input.resourcePath, body);
  }

  listSkills(input: { lifecycle?: string; limit?: number; offset?: number }): unknown {
    const limit = Math.min(input.limit ?? 20, 100);
    const offset = input.offset ?? 0;
    const lifecycle = input.lifecycle as SkillRecord["lifecycle"] | undefined;
    return { items: this.registry.listSkills({ lifecycle, limit, offset }).map((skill) => this.cardMeta(skill)) };
  }

  searchSkills(input: { query: string; limit?: number }): unknown {
    return {
      items: this.registry.searchSkills(input.query, Math.min(input.limit ?? 20, 50)).map((skill) => this.cardMeta(skill)),
    };
  }

  getSkillStatus(input: { skillId?: string; jobId?: string }): unknown {
    if (input.jobId) {
      const job = this.registry.getJob(input.jobId);
      if (!job) {
        throw new SkillMcpError("NOT_FOUND", "Job not found");
      }
      return job;
    }
    if (!input.skillId) {
      throw new SkillMcpError("INVALID_INPUT", "skillId or jobId required");
    }
    const skill = this.registry.requireSkill(input.skillId);
    return {
      skillId: skill.id,
      lifecycle: normalizeLifecycle(skill.lifecycle),
      lifecycleNote: describeLifecycle(skill.lifecycle),
      trust: skill.trustTier,
      security: skill.securityStatus,
      authorized: normalizeLifecycle(skill.lifecycle) === "AVAILABLE",
      fingerprint: skill.fingerprint,
      expirationAt: skill.expirationAt,
      securityNotice: SECURITY_NOTICE,
    };
  }

  getSkillSecurity(input: { skillId: string; requestId: string }): unknown {
    const skill = this.registry.requireSkill(input.skillId);
    const runs = this.registry.listScanResults(skill.fingerprint);
    return {
      skillId: skill.id,
      fingerprint: skill.fingerprint,
      commitSha: skill.commitSha,
      status: skill.securityStatus,
      claim:
        skill.securityStatus === "PASS"
          ? "PASSED_CONFIGURED_CHECKS"
          : skill.securityStatus === "FAIL"
            ? "FAILED"
            : "INCONCLUSIVE",
      scanners: runs.map((run) => ({
        scannerId: run.scannerId,
        scannerVersion: run.scannerVersion,
        status: run.status,
        findingCount: run.findings.length,
        notes: run.notes,
      })),
      securityNotice: SECURITY_NOTICE,
    };
  }

  getSkillTrust(input: { skillId: string; requestId: string }): unknown {
    const skill = this.registry.requireSkill(input.skillId);
    return {
      skillId: skill.id,
      tier: skill.trustTier,
      description: describeTrustTier(skill.trustTier),
      authorized: normalizeLifecycle(skill.lifecycle) === "AVAILABLE",
      note: "Trust is not authorization. UNKNOWN is not VERIFIED.",
      edges: this.registry.listTrustEdges(skill.fingerprint).slice(0, 20),
      securityNotice: SECURITY_NOTICE,
    };
  }

  getSkillPermissions(input: { skillId: string }): unknown {
    const skill = this.registry.requireSkill(input.skillId);
    return {
      skillId: skill.id,
      declared: skill.manifest.spec.capabilitiesDeclared,
      effective: skill.permissions,
      note: "Effective permissions come from the capability firewall, not skill text.",
    };
  }

  invalidate(input: { skillId: string; reason: string; requestId: string }): unknown {
    const skill = this.registry.updateSkill(input.skillId, { lifecycle: "INVALIDATED" });
    this.enqueue("SKILL_INVALIDATION", skill.id, skill.fingerprint, { reason: input.reason });
    this.audit.record({
      requestId: input.requestId,
      actor: "operator",
      action: "invalidate_skill",
      skillId: skill.id,
      fingerprint: skill.fingerprint,
      detail: { reason: input.reason },
    });
    return { skillId: skill.id, lifecycle: "INVALIDATED" };
  }

  async refresh(input: { skillId: string; wait?: boolean; requestId: string }): Promise<unknown> {
    const skill = this.registry.requireSkill(input.skillId);
    const pkg = this.requirePackage(skill.id);
    const source = this.sources.find((item) => item.id === pkg.sourceId) ?? this.localSource;
    const pinned = await source.pin({ repositoryUrl: pkg.repositoryUrl });
    if (pinned.commitSha === skill.commitSha) {
      this.enqueue("FULL_RESCAN", skill.id, skill.fingerprint, {});
      if (input.wait) {
        await this.processJobs(20);
      }
      return { mode: "same_commit_rescan", skillId: skill.id, commitSha: skill.commitSha };
    }
    const nextPkg: SkillPackage = { ...pkg, commitSha: pinned.commitSha, version: pinned.commitSha.slice(0, 12) };
    this.localSource.register(nextPkg);
    const acquired = await this.acquire({
      repositoryUrl: nextPkg.repositoryUrl,
      wait: input.wait,
      requestId: input.requestId,
    });
    return {
      mode: "new_artifact",
      previousSkillId: skill.id,
      previousCommit: skill.commitSha,
      note: "Commit B is a new artifact (anti-rug-pull). Commit A remains if still valid.",
      acquired,
    };
  }

  compareVersions(input: { skillIdA: string; skillIdB: string }): unknown {
    const a = this.registry.requireSkill(input.skillIdA);
    const b = this.registry.requireSkill(input.skillIdB);
    return {
      a: this.cardMeta(a),
      b: this.cardMeta(b),
      sameFingerprint: a.fingerprint === b.fingerprint,
      commitChanged: a.commitSha !== b.commitSha,
    };
  }

  requestCapability(input: {
    skillId: string;
    capability: string;
    approver?: string;
    requestId: string;
  }): unknown {
    const skill = this.registry.requireSkill(input.skillId);
    const next = this.firewall.request(input.capability, {
      approver: input.approver,
      risk: skill.risk,
      current: skill.permissions,
    });
    this.registry.updateSkill(skill.id, { permissions: next });
    this.audit.record({
      requestId: input.requestId,
      actor: input.approver ?? "agent",
      action: "request_capability",
      skillId: skill.id,
      fingerprint: skill.fingerprint,
      detail: { capability: input.capability },
    });
    return { skillId: skill.id, effective: next };
  }

  release(input: { skillId: string; requestId: string }): unknown {
    this.packages.delete(input.skillId);
    this.audit.record({
      requestId: input.requestId,
      actor: "agent",
      action: "release_skill",
      skillId: input.skillId,
    });
    return { skillId: input.skillId, released: true };
  }

  listAudit(limit = 50, skillId?: string): unknown {
    return { events: this.registry.listAudit(limit, skillId) };
  }

  listIntegrations(): unknown {
    return {
      policy: this.config.cost,
      notice: "Core is free/OSS-first. Commercial rows require explicit approval and never run as silent fallbacks.",
      integrations: Object.entries(COST_CATALOG).map(([id, cost]) => ({ id, ...cost })),
    };
  }

  listPendingCostApprovals(): unknown {
    return { items: this.registry.listCostApprovals("PENDING") };
  }

  approvePaidOperation(input: { approvalId: string; approver: string; requestId: string }): unknown {
    const updated = this.registry.updateCostApproval(input.approvalId, "APPROVED", input.approver);
    this.audit.record({
      requestId: input.requestId,
      actor: input.approver,
      action: "approve_paid_operation",
      detail: { approvalId: input.approvalId, provider: updated.provider, service: updated.service },
    });
    return { approval: updated, message: "Approved. Re-invoke the original operation with this approvalId." };
  }

  rejectPaidOperation(input: { approvalId: string; approver: string; requestId: string }): unknown {
    const updated = this.registry.updateCostApproval(input.approvalId, "REJECTED", input.approver);
    this.audit.record({
      requestId: input.requestId,
      actor: input.approver,
      action: "reject_paid_operation",
      detail: { approvalId: input.approvalId },
    });
    return { approval: updated, message: "Rejected. The operation will not run." };
  }

  async processJobs(max = 8): Promise<number> {
    let n = 0;
    while (n < max) {
      const job = this.registry.nextQueuedJob();
      if (!job) {
        break;
      }
      await this.processJob(job);
      n += 1;
    }
    return n;
  }

  async processJob(job: JobRecord): Promise<void> {
    this.registry.updateJob(job.id, { state: "RUNNING" });
    try {
      if (job.skillId) {
        const skill = this.registry.requireSkill(job.skillId);
        const pkg = this.requirePackage(job.skillId);
        const type = job.type as JobType;
        if (type === "ACQUIRE" || type === "PROVENANCE_CHECK" || type === "TRUST_REFRESH") {
          await this.runPipeline(skill, pkg);
        } else if (type === "SECURITY_SCAN" || type === "FULL_RESCAN" || type === "DEPENDENCY_REFRESH") {
          const allowPaid = job.payload.includePaidScanners === true ? ["snyk"] : [];
          await this.runScanStage(this.registry.requireSkill(job.skillId), pkg, allowPaid);
          await this.runSandboxAndPolicy(this.registry.requireSkill(job.skillId), pkg);
        } else if (type === "SANDBOX_TEST") {
          await this.runSandboxAndPolicy(skill, pkg);
        }
      }
      this.registry.updateJob(job.id, { state: "SUCCEEDED" });
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      this.logger.warn("job_failed", { jobId: job.id, error: message });
      this.registry.updateJob(job.id, { state: "FAILED", error: message });
    }
  }

  private async runPipeline(skill: SkillRecord, pkg: SkillPackage): Promise<void> {
    await this.runProvenance(skill, pkg, newId("req"));
    const afterTrust = this.registry.requireSkill(skill.id);
    if (this.cache.get(afterTrust.fingerprint)) {
      this.metrics.inc("cache_hit");
      this.registry.updateSkill(skill.id, { lifecycle: "SECURITY_SCAN", securityStatus: "PASS" });
      this.registry.updateSkill(skill.id, { lifecycle: "POLICY_EVALUATION" });
      this.authorize(this.registry.requireSkill(skill.id), true);
      return;
    }
    await this.runScanStage(this.registry.requireSkill(skill.id), pkg);
    await this.runSandboxAndPolicy(this.registry.requireSkill(skill.id), pkg);
  }

  private async runProvenance(skill: SkillRecord, pkg: SkillPackage, requestId: string): Promise<TrustDecision> {
    this.registry.updateSkill(skill.id, { lifecycle: "PROVENANCE_CHECK" });
    this.assertPinned(pkg);
    const decision = this.broker.evaluate(pkg);
    this.graph.recordPackage(pkg, decision, skill.fingerprint);
    this.registry.updateSkill(skill.id, { trustTier: decision.tier });
    this.audit.record({
      requestId,
      actor: "gateway",
      action: "provenance_check",
      skillId: skill.id,
      fingerprint: skill.fingerprint,
      detail: { tier: decision.tier, evidence: decision.evidence },
    });
    if (!decision.canScan) {
      this.registry.updateSkill(skill.id, { lifecycle: "REJECTED", securityStatus: "INCONCLUSIVE" });
      throw new SkillMcpError("POLICY_DENIED", "Publisher policy forbids scanning this artifact");
    }
    return decision;
  }

  private async runScanStage(skill: SkillRecord, pkg: SkillPackage, allowPaidScannerIds: string[] = []): Promise<void> {
    this.registry.updateSkill(skill.id, { lifecycle: "SECURITY_SCAN" });
    const result = await this.orchestrator.scan(
      { skillId: skill.id, package: pkg, quarantinePath: join(this.dataDir, "quarantine", skill.id) },
      skill.risk,
      { allowPaidScannerIds },
    );
    for (const run of result.scanners) {
      this.registry.insertScanResult(skill.id, skill.fingerprint, run);
    }
    this.registry.updateSkill(skill.id, { securityStatus: result.status });
    if (result.status !== "PASS") {
      this.registry.updateSkill(skill.id, {
        lifecycle: result.status === "FAIL" ? "QUARANTINED" : "REJECTED",
      });
      throw new SkillMcpError("SECURITY_GATE", `Scan ${result.claim}`, {
        status: result.status,
        requiredMissing: result.requiredMissing,
      });
    }
  }

  private async runSandboxAndPolicy(skill: SkillRecord, pkg: SkillPackage): Promise<void> {
    const executable = isExecutableSkill(pkg, skill.manifest);
    if (executable && this.config.sandbox.executableRequiresSandbox) {
      this.registry.updateSkill(skill.id, { lifecycle: "SANDBOX" });
      const sandbox = await this.sandbox.evaluate(pkg, skill.manifest);
      this.registry.updateSkill(skill.id, { sandboxSummary: sandbox });
      if (sandbox.status !== "PASS") {
        this.registry.updateSkill(skill.id, {
          lifecycle: sandbox.status === "FAIL" ? "QUARANTINED" : "REJECTED",
          securityStatus: sandbox.status === "FAIL" ? "FAIL" : "INCONCLUSIVE",
        });
        throw new SkillMcpError("SECURITY_GATE", `Sandbox ${sandbox.status} is not PASS`, { notes: sandbox.notes });
      }
    }
    this.registry.updateSkill(skill.id, { lifecycle: "POLICY_EVALUATION" });
    const trust = this.broker.evaluate(pkg);
    this.authorize(this.registry.requireSkill(skill.id), trust.canApprove);
  }

  private authorize(skill: SkillRecord, canApprove: boolean): void {
    const granted = this.firewall.effective(skill.manifest.spec.capabilitiesDeclared, ["filesystem.read"]);
    if (!canApprove) {
      this.registry.updateSkill(skill.id, { lifecycle: "APPROVED", permissions: [] });
      this.registry.updateSkill(skill.id, { lifecycle: "REJECTED" });
      return;
    }
    const hours = this.config.security.revalidationHours;
    const expiration = new Date(this.clock.now().getTime() + hours * 3600 * 1000).toISOString();
    this.registry.updateSkill(skill.id, { lifecycle: "APPROVED", permissions: granted, expirationAt: expiration });
    this.registry.updateSkill(skill.id, { lifecycle: "AVAILABLE" });
    this.cache.put({
      fingerprint: skill.fingerprint,
      skillId: skill.id,
      securityStatus: "PASS",
      scannerVersions: Object.fromEntries(
        this.registry.listScanResults(skill.fingerprint).map((run) => [run.scannerId, run.scannerVersion]),
      ),
      securityConfigHash: this.configHash,
      expiresAt: expiration,
    });
  }

  private fingerprintFor(pkg: SkillPackage, manifest: SkillRecord["manifest"]): string {
    return computeFingerprint({
      publisher: pkg.publisher,
      repository: pkg.repositoryUrl,
      commitSha: pkg.commitSha,
      manifestCanonical: canonicalize(manifest),
      dependencyLockHash: manifest.spec.dependencies.lockHash,
      securityConfigurationHash: this.configHash,
      filesDigest: filesDigest(pkg.files),
    });
  }

  private assertPinned(pkg: SkillPackage): void {
    const sha = pkg.commitSha.trim().toLowerCase();
    if (!sha || sha === "latest" || sha === "main" || sha === "master" || sha === "head") {
      throw new SkillMcpError("SECURITY_GATE", "Refusing mutable ref as identity; commit SHA required");
    }
  }

  private enqueue(type: JobType, skillId: string, fingerprint: string, extra: Record<string, unknown>): string {
    const open = this.registry.findOpenJob(type, fingerprint);
    if (open) {
      return open.id;
    }
    const id = newId("job");
    const now = iso(this.clock);
    this.registry.insertJob({
      id,
      skillId,
      type,
      state: "QUEUED",
      payload: { fingerprint, ...extra },
      error: null,
      createdAt: now,
      updatedAt: now,
    });
    return id;
  }

  private async resolvePackage(input: {
    query?: string;
    candidateId?: string;
    repositoryUrl?: string;
    approvalId?: string;
  }): Promise<SkillPackage> {
    if (input.repositoryUrl) {
      let pending: CostDecision | undefined;
      for (const source of this.sources) {
        const cost = this.sourceCost(source, input.repositoryUrl);
        const decision = this.evaluateCost("acquire_skill", cost, input.approvalId);
        if (!decision.proceed) {
          pending = decision;
          continue;
        }
        try {
          return await source.fetch({ repositoryUrl: input.repositoryUrl });
        } catch {
          continue;
        }
      }
      if (pending?.kind === "NEEDS_APPROVAL") {
        throw new SkillMcpError("COST_APPROVAL_REQUIRED", "Potentially billable source requires approval", {
          review: pending.review,
          metadata: pending.metadata,
        });
      }
      if (pending?.kind === "DENIED") {
        throw new SkillMcpError("POLICY_DENIED", pending.reason, { metadata: pending.metadata });
      }
    }
    const q = input.query ?? (input.candidateId?.includes(":") ? input.candidateId.split(":")[1] : input.candidateId) ?? "";
    const hits = await this.localSource.search({ query: q, limit: 1 });
    const hit = hits[0];
    if (hit) {
      return this.localSource.fetch({ repositoryUrl: hit.repositoryUrl });
    }
    throw new SkillMcpError("NOT_FOUND", "No skill candidate resolved for acquisition");
  }

  private requirePackage(skillId: string): SkillPackage {
    const pkg = this.packages.get(skillId);
    if (!pkg) {
      throw new SkillMcpError("NOT_FOUND", "Quarantined package bytes are not in this process");
    }
    return pkg;
  }

  private writeQuarantine(skillId: string, pkg: SkillPackage): void {
    if (this.dataDir === ":memory:") {
      return;
    }
    const dir = join(this.dataDir, "quarantine", skillId);
    mkdirSync(dir, { recursive: true });
    for (const file of pkg.files) {
      writeFileSync(join(dir, file.path.replaceAll("/", "_")), file.content, "utf8");
    }
  }

  private guessTier(candidate: SkillCandidate): TrustTier {
    const login = candidate.publisher.toLowerCase();
    if (this.config.trust.officialOrganizations.map((item) => item.toLowerCase()).includes(login)) {
      return "OFFICIAL";
    }
    if (this.config.trust.verifiedPublishers.map((item) => item.toLowerCase()).includes(login)) {
      return "VERIFIED";
    }
    if (this.config.trust.trustedCommunityPublishers.map((item) => item.toLowerCase()).includes(login)) {
      return "TRUSTED_COMMUNITY";
    }
    return "UNKNOWN";
  }

  private cardMeta(skill: SkillRecord): Record<string, unknown> {
    return {
      id: skill.id,
      name: skill.name,
      description: skill.manifest.metadata.description,
      version: skill.version,
      trust: skill.trustTier,
      security: skill.securityStatus,
      fingerprint: skill.fingerprint,
      lifecycle: normalizeLifecycle(skill.lifecycle),
      authorized: normalizeLifecycle(skill.lifecycle) === "AVAILABLE",
      reputation: skill.qualityStatus,
    };
  }

  private sourceCost(source: SkillSource, repositoryUrl?: string): CostMetadata {
    if (source instanceof GitHubSource) {
      return source.costFor(repositoryUrl ? { repositoryUrl } : undefined);
    }
    return source.cost;
  }

  private evaluateCost(operation: string, metadata: CostMetadata, approvalId?: string): CostDecision {
    if (approvalId) {
      const record = this.registry.getCostApproval(approvalId);
      if (!record) {
        throw new SkillMcpError("NOT_FOUND", `Cost approval ${approvalId} not found`);
      }
      if (record.status === "APPROVED") {
        return this.costDetector.evaluate(operation, metadata, { approvalStatus: "APPROVED", approvalId });
      }
      if (record.status === "REJECTED") {
        return this.costDetector.evaluate(operation, metadata, { approvalStatus: "REJECTED" });
      }
    }
    const open = this.registry.findOpenCostApproval(operation, metadata.provider, metadata.service);
    if (open?.status === "APPROVED") {
      return this.costDetector.evaluate(operation, metadata, { approvalStatus: "APPROVED", approvalId: open.id });
    }
    if (open?.status === "REJECTED") {
      return this.costDetector.evaluate(operation, metadata, { approvalStatus: "REJECTED" });
    }
    return this.costDetector.evaluate(operation, metadata);
  }

  private ensurePending(operation: string, metadata: CostMetadata, review: CostReview): CostApproval {
    const existing = this.registry.findOpenCostApproval(operation, metadata.provider, metadata.service);
    if (existing?.status === "PENDING") {
      return existing;
    }
    const now = iso(this.clock);
    const record: CostApproval = {
      id: newId("cst"),
      operation,
      provider: metadata.provider,
      service: metadata.service,
      status: "PENDING",
      approver: null,
      review,
      createdAt: now,
      updatedAt: now,
    };
    this.registry.insertCostApproval(record);
    return record;
  }
}

export function createGateway(opts?: GatewayOptions): SkillTrustGateway {
  return new SkillTrustGateway(opts);
}
