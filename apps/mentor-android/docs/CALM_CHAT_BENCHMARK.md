# Calm chat — Macrobenchmark checklist

Run on a physical mid-range device (not an emulator) before release:

```bash
cd apps/mentor-android
./gradlew :macrobenchmark:connectedBenchmarkAndroidTest
```

Capture and paste into the PR:

- `frameTimingMetric` p50 / p95 while scrolling a 50-message chat
- `startupTimingMetric` cold start to first frame

Target: no jank spikes during streaming scroll; cold start under ~1s on mid-range hardware.

Baseline Profile: add `:baselineprofile` module when Play signing is available (`./gradlew :app:generateBaselineProfile`).
