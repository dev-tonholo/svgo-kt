# svgo-kt benchmarks

JMH-based microbenchmarks for `svgo-kt`'s `optimize()` pipeline, plus a
helper script for comparing against the upstream Node.js `svgo` package
on byte-identical payloads.

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
operation. Each configuration warms up before measurement so JIT
optimization is in steady state.

The three payloads are generated from a single Kotlin source of truth in
`build.gradle.kts` (the `generateBenchmarkPayloads` task) into
`build/generated/resources/payloads/`, then reused as both the Kotlin
benchmark's classpath resources and the Node comparison script's
filesystem inputs -- so both sides run on byte-identical bytes.

## Running

```bash
# Full benchmark (3 warmups, 5 iterations of 2s each, JSON report).
./gradlew :benchmarks:benchmark

# Faster local iteration (1 warmup, 2 iterations of 1s each, text output).
./gradlew :benchmarks:smokeBenchmark
```

Reports land under `benchmarks/build/reports/benchmarks/`.

Local snapshot on an Apple Silicon laptop (`avgt`, ms/op):

| Payload | svgo-kt JVM |
|---------|-------------|
| tiny    | ~0.16 ms    |
| small   | ~2.5 ms     |
| medium  | ~38 ms      |

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
2. Runs the full svgo-kt JVM benchmark (`./gradlew :benchmarks:benchmark`).
3. `npm install`s upstream `svgo@4.0.1` under `benchmarks/scripts/` if
   it's not already there.
4. Reads the freshest svgo-kt JMH JSON report.
5. Times upstream svgo on the same payload files via `perf_hooks` (200
   warmup + 1000 measured iterations per payload, with a 99.9 %
   confidence interval).
6. Prints a side-by-side table like:

   ```
   Payload  |    svgo-kt JVM (ms/op) |     svgo (Node, ms/op) |   svgo-kt / svgo
   -------- | ---------------------- | ---------------------- | ----------------
   tiny     |          0.157 ± 0.018 |          0.051 ± 0.006 |            3.06x
   small    |          2.468 ± 0.248 |          0.878 ± 0.016 |            2.81x
   medium   |         37.699 ± 1.463 |          6.006 ± 0.153 |            6.28x
   ```

   `ratio < 1.0` -> svgo-kt is faster than upstream on that payload;
   `> 1.0` -> slower.

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
