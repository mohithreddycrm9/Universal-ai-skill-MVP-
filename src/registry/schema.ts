export const SCHEMA_SQL = `
CREATE TABLE IF NOT EXISTS skills (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  publisher TEXT NOT NULL,
  repository TEXT NOT NULL,
  version TEXT NOT NULL,
  commit_sha TEXT NOT NULL,
  fingerprint TEXT NOT NULL UNIQUE,
  manifest_json TEXT NOT NULL,
  persistence TEXT NOT NULL,
  lifecycle TEXT NOT NULL,
  trust_tier TEXT NOT NULL,
  security_status TEXT NOT NULL,
  quality_status TEXT NOT NULL,
  risk TEXT NOT NULL,
  permissions_json TEXT NOT NULL,
  sbom_summary_json TEXT,
  sandbox_json TEXT,
  expiration_at TEXT,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS scan_results (
  id TEXT PRIMARY KEY,
  skill_id TEXT NOT NULL,
  fingerprint TEXT NOT NULL,
  scanner_id TEXT NOT NULL,
  scanner_version TEXT NOT NULL,
  status TEXT NOT NULL,
  finding_count INTEGER NOT NULL,
  finding_summary_json TEXT NOT NULL,
  notes TEXT,
  started_at TEXT NOT NULL,
  finished_at TEXT NOT NULL,
  UNIQUE (fingerprint, scanner_id, scanner_version)
);

CREATE TABLE IF NOT EXISTS jobs (
  id TEXT PRIMARY KEY,
  skill_id TEXT,
  type TEXT NOT NULL,
  state TEXT NOT NULL,
  payload_json TEXT NOT NULL,
  error TEXT,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_events (
  id TEXT PRIMARY KEY,
  request_id TEXT NOT NULL,
  actor TEXT NOT NULL,
  action TEXT NOT NULL,
  skill_id TEXT,
  fingerprint TEXT,
  detail_json TEXT NOT NULL,
  created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS trust_edges (
  id TEXT PRIMARY KEY,
  from_node TEXT NOT NULL,
  to_node TEXT NOT NULL,
  kind TEXT NOT NULL,
  evidence_json TEXT NOT NULL,
  created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS verified_cache (
  fingerprint TEXT PRIMARY KEY,
  skill_id TEXT NOT NULL,
  security_status TEXT NOT NULL,
  scanner_versions_json TEXT NOT NULL,
  security_config_hash TEXT NOT NULL,
  created_at TEXT NOT NULL,
  expires_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS cost_approvals (
  id TEXT PRIMARY KEY,
  operation TEXT NOT NULL,
  provider TEXT NOT NULL,
  service TEXT NOT NULL,
  status TEXT NOT NULL,
  approver TEXT,
  review_json TEXT NOT NULL,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_skills_lifecycle ON skills(lifecycle);
CREATE INDEX IF NOT EXISTS idx_skills_name ON skills(name);
CREATE INDEX IF NOT EXISTS idx_jobs_state ON jobs(state);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_events(created_at);
`;
