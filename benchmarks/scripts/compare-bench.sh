#!/usr/bin/env bash
# compare-bench.sh
#
# Runs the full svgo-kt JVM benchmark suite, then reuses the same payloads
# to time upstream svgo via Node.js, and prints a side-by-side table with
# the svgo-kt-to-svgo ratio per payload size.
#
# Requires:
#   - JDK 17+ (already needed for the rest of the project)
#   - Node.js 22+ (for ESM imports + Float64Array)
#   - npm        (for the local svgo install under benchmarks/scripts/)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BENCH_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ROOT_DIR="$(cd "$BENCH_DIR/.." && pwd)"

cd "$ROOT_DIR"

echo "==> Generating shared benchmark payloads"
./gradlew :benchmarks:generateBenchmarkPayloads --quiet

echo "==> Running svgo-kt JVM benchmarks (this can take 1-2 minutes)"
./gradlew :benchmarks:benchmark --quiet

# kotlinx-benchmark drops the JSON report under
# benchmarks/build/reports/benchmarks/<config>/<timestamp>/jvm.json. Pick the
# most recent one regardless of the configuration name so this script keeps
# working if the default config is renamed.
JMH_JSON="$(
    find "$BENCH_DIR/build/reports/benchmarks" -type f -name 'jvm.json' \
        -exec ls -t {} + \
        | head -n 1
)"

if [[ -z "${JMH_JSON:-}" ]]; then
    echo "::error::Could not find jvm.json under $BENCH_DIR/build/reports/benchmarks/"
    exit 1
fi

echo "==> Loaded svgo-kt results from: $JMH_JSON"

cd "$SCRIPT_DIR"

if [[ ! -d node_modules ]]; then
    echo "==> Installing upstream svgo for the comparison script"
    npm install --silent --no-audit --no-fund
fi

echo "==> Timing upstream svgo on the same payloads"
node bench-and-compare.mjs "$JMH_JSON" "$BENCH_DIR/build/generated/payloads"
