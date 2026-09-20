# Performance

Targets (local, not SLAs):

- Cache hit on `acquire_skill` returns immediately without rescanning.
- Independent scanners run in parallel.
- Duplicate in-flight jobs for one fingerprint collapse.
- MCP envelopes are capped (default 32 KiB); progressive disclosure level 0 is metadata-only.
- User-facing acquire does not wait on full scans unless `wait=true`.

Run `npm run bench` for a tiny local micro-benchmark.
