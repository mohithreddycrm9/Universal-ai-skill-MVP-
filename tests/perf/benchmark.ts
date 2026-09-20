import { performance } from "node:perf_hooks";
import { computeFingerprint } from "../../src/skills/fingerprint.js";
import { testGateway, benignPackage } from "../helpers.js";

const n = 500;
const t0 = performance.now();
for (let i = 0; i < n; i++) {
  computeFingerprint({
    publisher: "fixture-org",
    repository: "https://example.local/fixture-org/csv-normalize",
    commitSha: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    manifestCanonical: "{}",
    dependencyLockHash: "sha256:lock",
    securityConfigurationHash: "sha256:sec",
    filesDigest: "sha256:files",
  });
}
const fingerprintMs = performance.now() - t0;

const gw = testGateway([benignPackage()]);
const t1 = performance.now();
await gw.acquire({ query: "csv-normalize", wait: true, requestId: "bench" });
const firstAcquire = performance.now() - t1;
const t2 = performance.now();
await gw.acquire({ query: "csv-normalize", wait: false, requestId: "bench2" });
const cacheHit = performance.now() - t2;

process.stdout.write(
  JSON.stringify(
    {
      fingerprintIterations: n,
      fingerprintMs,
      firstAcquireMs: firstAcquire,
      cacheHitMs: cacheHit,
      note: "Local micro-bench, not a production SLA. Cache hits should be much cheaper than first acquire.",
    },
    null,
    2,
  ) + "\n",
);
