# Calm chat — Macrobenchmark and Baseline Profile

The `:macrobenchmark` module (`com.android.test`, targets `:app`) holds:

- `BaselineProfileGenerator` — startup, chat list scroll, drawer open/close.
- `ChatBenchmarks` — cold `StartupTimingMetric` and chat-scroll `FrameTimingMetric`,
  each with `CompilationMode.None()` vs `CompilationMode.Partial(BaselineProfileMode.Require)`.

The app applies `androidx.baselineprofile` and depends on `androidx.profileinstaller`, so a generated
profile ships in the APK/AAB and is installed on sideloaded builds too.

## Requirements

A physical device or emulator on **API 33+** (or rooted API 28+). Benchmark builds are signed with
the debug key by the plugin; no release keystore is needed. Complete onboarding once on the device and
open a chat with ~50 messages so the journeys have something to scroll.

## Commands (from `apps/mentor-android`)

```bash
# Build only (no device needed) — verified in CI/locally
./gradlew :macrobenchmark:assembleBenchmarkRelease :app:assembleBenchmarkRelease

# Generate the Baseline Profile (device needed). Written to app/src/release/generated/baselineProfiles/
./gradlew :app:generateReleaseBaselineProfile

# Run the benchmarks (device needed)
./gradlew :macrobenchmark:connectedBenchmarkReleaseAndroidTest
```

Do not run `./gradlew :macrobenchmark:assemble` / `build`: with `useConnectedDevices = true` the plugin
wires connected tasks into the aggregate and it fails without a device.

Record in the PR: startup p50/p95 (None vs BaselineProfile) and frame duration p50/p95/p99 for scroll.
Target: cold start under ~1 s on mid-range hardware, no frame-overrun spikes while streaming.
