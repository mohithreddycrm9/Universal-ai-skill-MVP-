# Database schema

Default engine: SQLite (`node:sqlite`). Swap via `DatabaseAdapter`.

## `skills`

| Column | Type | Notes |
| --- | --- | --- |
| id | TEXT PK | `skl_…` |
| name | TEXT | |
| publisher | TEXT | |
| repository | TEXT | |
| version | TEXT | |
| commit_sha | TEXT | Immutable pin |
| fingerprint | TEXT UNIQUE | Content-addressed |
| manifest_json | TEXT | Compact |
| persistence | TEXT | TEMPORARY/SESSION/CACHED/TRUSTED/PERSISTENT |
| lifecycle | TEXT | See lifecycle enum |
| trust_tier | TEXT | |
| security_status | TEXT | |
| quality_status | TEXT | Default UNKNOWN |
| risk | TEXT | |
| permissions_json | TEXT | Effective, firewall-owned |
| sbom_summary_json | TEXT | Summary only |
| sandbox_json | TEXT | Behavioral fingerprint summary |
| expiration_at | TEXT ISO | |
| created_at / updated_at | TEXT ISO | |

## `scan_results`

Per skill fingerprint + scanner id: status, version, finding_count, finding_summary_json, started_at, finished_at.

## `jobs`

Async acquisition: `id`, `skill_id`, `type`, `state` (`QUEUED|RUNNING|SUCCEEDED|FAILED`), `payload_json`, `error`, timestamps.

## `audit_events`

Append-only: `id`, `request_id`, `actor`, `action`, `skill_id`, `fingerprint`, `detail_json` (redacted), `created_at`. No secret values.

## `trust_edges`

`from_node`, `to_node`, `kind`, `evidence_json`, `created_at`.

## `verified_cache`

`fingerprint` PK, `skill_id`, `security_status`, `scanner_versions_json`, `security_config_hash`, `created_at`, `expires_at`.

## `cost_approvals`

Pending/approved billable ops. `method` must be `local_interactive` for APPROVED to authorize. Includes `expires_at`, `approved_at`.

## `capability_approvals`

| Column | Notes |
| --- | --- |
| id | `cap_…` |
| skill_id, repository, commit_sha, fingerprint, capability | Binding |
| status | PENDING / APPROVED / REJECTED / EXPIRED / CONSUMED |
| method | null until trusted CLI approve → `local_interactive` |
| expires_at / approved_at / timestamps | Clock-based |

## PostgreSQL

Same tables/types as SQLite. `SqliteAdapter` is the default. `PostgresAdapter` (`src/registry/postgres.ts`) rewrites `?` placeholders to `$1..$n` and applies the same schema.

Enable with `database.driver: postgres` plus `DATABASE_URL` or `registry.database.url`, and optional dependency `pg`. Missing URL or missing `pg` fails closed (no silent SQLite fallback). Unit tests cover dialect SQL; live tests run only when `DATABASE_URL` is set.
