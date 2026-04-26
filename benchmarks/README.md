# svgo-kt benchmarks

kotlinx-benchmark microbenchmarks for `svgo-kt`'s `optimize()` pipeline
on **JVM and Kotlin/Native**, plus a helper script for comparing
against the upstream Node.js `svgo` package on byte-identical payloads.

## What's measured

`SvgoOptimizeBenchmark` runs `Svgo.optimize()` with the default
`preset-default` plugin pipeline (the same one the upstream svgo CLI
uses) over three representative payloads:

- `tiny` -- a typical icon-style SVG (~6 lines, 1 path).
- `small` -- a moderately styled illustration (~50 elements, gradients,
  styled text).
- `medium` -- a larger document with hundreds of paths, gradients, and
  text labels.

Mode is **average time** (`avgt`) reported in milliseconds per
operation. Each configuration warms up before measurement so JIT/AOT
optimization is in steady state.

The three payloads are generated from a single Kotlin source of truth
in `build.gradle.kts` (the `generateBenchmarkPayloads` task), emitted
as **both**:

- a `commonMain` Kotlin object (`BenchmarkPayloads`) consumed by every
  KMP benchmark target without per-target resource I/O, and
- on-disk `.svg` files under `build/generated/payloads/` that the Node
  comparison script feeds to upstream svgo.

Both sides therefore run on byte-identical bytes.

## Targets

The benchmark module is configured for `jvm`, `macosArm64`, `macosX64`,
`linuxX64`, and `mingwX64`. kotlinx-benchmark only creates a runnable
benchmark task for the **host** Native target, so any single host runs
JVM plus its matching native flavor:

| Host                  | Available benchmark tasks                |
|-----------------------|------------------------------------------|
| macOS arm64           | `jvmBenchmark`, `macosArm64Benchmark`    |
| macOS x86_64          | `jvmBenchmark`, `macosX64Benchmark`      |
| Linux x86_64          | `jvmBenchmark`, `linuxX64Benchmark`      |
| Windows x86_64 (MinGW)| `jvmBenchmark`, `mingwX64Benchmark`      |

A CI matrix that runs `./gradlew :benchmarks:benchmark` on each host
covers every supported native target plus JVM.

### Why not JS or Wasm?

`Svgo.optimize` is `suspend`, and as of kotlinx-benchmark 0.4.13 the
generated benchmark descriptors expect non-`suspend` benchmark methods
on every target. The pragmatic workaround is `runBlocking`, which is
only available on JVM and Native; on JS and Wasm there is no
synchronous coroutine driver and the optimize pipeline schedules work
on `Dispatchers.Default`, so a non-suspending benchmark method can't
drive it. The Node-side `bench-and-compare.mjs` script under
`scripts/` covers the JS-runtime comparison against upstream svgo
instead.

## Running

```bash
# Full benchmark on every benchmark target available on this host
# (JVM + the host's native target). 3 warmups, 5 iterations of 2s each,
# JSON report.
./gradlew :benchmarks:benchmark

# Faster local iteration on every target available on this host.
./gradlew :benchmarks:smokeBenchmark

# Run a single target.
./gradlew :benchmarks:jvmBenchmark
./gradlew :benchmarks:macosArm64Benchmark   # or linuxX64Benchmark, etc.
```

Reports land under `benchmarks/build/reports/benchmarks/`.

Local snapshot on an Apple Silicon laptop (`avgt`, ms/op):

| Payload | svgo-kt JVM | svgo-kt macosArm64 (Native) |
|---------|-------------|-----------------------------|
| tiny    | ~0.16 ms    | ~0.23 ms                    |
| small   | ~2.5 ms     | ~4.7 ms                     |
| medium  | ~38 ms      | ~80 ms                      |

## Comparing against upstream svgo

These numbers are not directly comparable to upstream svgo in absolute
terms -- different runtimes (JVM vs. V8), different startup costs,
different GC characteristics. The *ratio* on the same payload is what's
informative for tracking regressions over time.

### `compare-bench.sh` -- one command, full report

```bash
./benchmarks/scripts/compare-bench.sh
```

That script:

1. Generates the shared payload files (`generateBenchmarkPayloads`).
2. Runs the full svgo-kt benchmark suite for every target available on
   this host (`./gradlew :benchmarks:benchmark`, JVM plus the host's
   matching Native target).
3. `npm install`s upstream `svgo@4.0.1` under `benchmarks/scripts/` if
   it's not already there.
4. Reads every fresh svgo-kt JSON report (one per benchmark target) out
   of the latest `build/reports/benchmarks/main/<timestamp>/` directory.
5. Times upstream svgo on the same payload files via `perf_hooks` (200
   warmup + 1000 measured iterations per payload, with a 99.9 %
   confidence interval).
6. Prints a side-by-side table with one column per svgo-kt target plus
   the svgo-kt-to-svgo ratio per target, e.g. on Apple Silicon:

   ```
   Payload  |    svgo-kt jvm (ms/op) | svgo-kt macosArm64 (ms/op) |     svgo (Node, ms/op) |       jvm / svgo | macosArm64 / svgo
   -------- | ---------------------- | -------------------------- | ---------------------- | ---------------- | -----------------
   tiny     |          0.153 ± 0.079 |              0.281 ± 0.018 |          0.051 ± 0.004 |            3.02x |             5.57x
   small    |          2.622 ± 0.478 |              5.574 ± 0.094 |          0.886 ± 0.012 |            2.96x |             6.29x
   medium   |         35.859 ± 2.914 |             87.380 ± 1.935 |          5.394 ± 0.076 |            6.65x |            16.20x
   ```

   `ratio < 1.0` -> svgo-kt is faster than upstream on that payload;
   `> 1.0` -> slower.

#### Allocation profile (`--gc`)

```bash
./benchmarks/scripts/compare-bench.sh --gc
```

Adds a second pass on the JVM target with JMH's `-prof gc` profiler and
prints a per-payload allocation summary alongside the timing table:

```
JVM allocation profile (JMH -prof gc)

Payload  | Target       |  Alloc/op (KB) |  Alloc rate (MB/s) |   GC count |  GC time (ms)
-------- | ------------ | -------------- | ------------------ | ---------- | -------------
tiny     | jvm          |         210.02 |             1133.5 |         42 |            20
small    | jvm          |        2300.55 |              784.3 |         29 |            14
medium   | jvm          |       18543.25 |              425.5 |         16 |            16
```

`Alloc/op` (`gc.alloc.rate.norm`) is bytes allocated per `optimize()`
call. High `Alloc/op` relative to input size flags allocation-heavy
hot paths and is the most actionable signal for closing the gap with
upstream svgo. JMH profilers are JVM-only, so this table covers the
`jvm` target only; Native allocation profiling needs a separate tool
(e.g. `Instruments` on macOS) and is not wired into this script.

Requirements: JDK 17+ (already needed for the rest of the project),
Node.js 22+ (ESM imports + `Float64Array`), and `npm` for the local
svgo install.

### Notes on interpreting the results

- **JVM vs Node.js startup.** kotlinx-benchmark already excludes JVM
  startup; `performance.now()` deltas around `optimize()` calls do the
  same on Node. Compare steady-state numbers only.
- **GC noise.** Both runtimes pause occasionally. Bigger N and more
  iterations smooth this out; the JMH harness in this module already
  uses 5 iterations of 2 seconds in the default `benchmark`
  configuration, and the Node side runs 1000 iterations per payload.
- **Warmup matters.** The JVM JIT inlines the plugin pipeline once the
  benchmark has run a few thousand times; the first few iterations
  will look much slower.
- **Pipeline equivalence.** Upstream svgo's `optimize(svg)` defaults
  to `preset-default`, which is exactly what `svgo {}` (empty config)
  applies in svgo-kt. Don't compare `preset-default` against a
  hand-tuned plugin list -- the time profile is materially different.
- **Confidence intervals.** Both sides print `score ± error` at 99.9 %
  confidence. A change is real only if the new score sits outside the
  old score's interval and the new interval doesn't overlap the old.
