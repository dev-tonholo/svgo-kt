# svgo-kt benchmarks

JMH-based microbenchmarks for `svgo-kt`'s `optimize()` pipeline, plus a
helper script for comparing against the upstream Node.js `svgo` package.

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
| tiny    | ~0.25 ms    |
| small   | ~3.1 ms     |
| medium  | ~39 ms      |

## Comparing against upstream svgo

These numbers are not directly comparable to upstream svgo in absolute
terms -- different runtimes (JVM vs. V8), different startup costs,
different GC characteristics. The *ratio* on the same payload is what's
informative for tracking regressions over time.

### Quick comparison via Node.js

```bash
node --experimental-vm-modules - <<'EOF'
import { optimize } from 'svgo';
import { performance } from 'perf_hooks';

const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
  <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
</svg>`;

const N = 1000;
// warmup
for (let i = 0; i < 100; i++) optimize(svg);

const t0 = performance.now();
for (let i = 0; i < N; i++) optimize(svg);
const dt = (performance.now() - t0) / N;
console.log(`upstream svgo (tiny): ${dt.toFixed(3)} ms/op`);
EOF
```

Drop in larger payloads to mirror the `small` / `medium` shapes; the
Kotlin source for those is in
[`SvgoOptimizeBenchmark.kt`](src/jvmMain/kotlin/svgokt/benchmarks/SvgoOptimizeBenchmark.kt)
and is straight-forward to translate.

### Notes on interpreting the results

- **JVM vs Node.js startup.** kotlinx-benchmark already excludes JVM
  startup; `performance.now()` deltas around `optimize()` calls do the
  same on Node. Compare steady-state numbers only.
- **GC noise.** Both runtimes pause occasionally. Bigger N and more
  iterations smooth this out; the JMH harness in this module already
  uses 5 iterations of 2 seconds in the default `benchmark`
  configuration.
- **Warmup matters.** The JVM JIT inlines the plugin pipeline once the
  benchmark has run a few thousand times; the first few iterations
  will look much slower.
- **Pipeline equivalence.** Upstream svgo's `optimize(svg)` defaults
  to `preset-default`, which is exactly what `svgo {}` (empty config)
  applies in svgo-kt. Don't compare `preset-default` against a
  hand-tuned plugin list -- the time profile is materially different.
