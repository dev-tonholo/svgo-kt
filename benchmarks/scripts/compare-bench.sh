#!/usr/bin/env bash
# compare-bench.sh
#
# Runs the full svgo-kt benchmark suite for every kotlinx-benchmark
# target available on the current host (JVM plus the host's matching
# Kotlin/Native target), then reuses the same payloads to time upstream
# svgo via Node.js, and prints a side-by-side table with the
# svgo-kt-to-svgo ratio per payload size and per target.
#
# Pass `--gc` to additionally run the JVM benchmark with JMH's `-prof gc`
# profiler and print a per-payload allocation summary. JMH profilers are
# JVM-only, so the GC table only covers the `jvm` target.
#
# Requires:
#   - JDK 17+ (already needed for the rest of the project)
#   - Node.js 22+ (for ESM imports + Float64Array)
#   - npm        (for the local svgo install under benchmarks/scripts/)

set -euo pipefail

WITH_GC=false
for arg in "$@"; do
    case "$arg" in
        --gc)
            WITH_GC=true
            ;;
        -h|--help)
            cat <<USAGE
Usage: $(basename "$0") [--gc]

  --gc    Also run JMH '-prof gc' on the JVM target and print a
          per-payload allocation summary (alloc/op, alloc rate, GC
          count, GC time). JVM-only.
USAGE
            exit 0
            ;;
        *)
            echo "::error::Unknown argument: $arg" >&2
            echo "Run '$(basename "$0") --help' for usage." >&2
            exit 1
            ;;
    esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BENCH_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ROOT_DIR="$(cd "$BENCH_DIR/.." && pwd)"

cd "$ROOT_DIR"

echo "==> Generating shared benchmark payloads"
./gradlew :benchmarks:generateBenchmarkPayloads --quiet

echo "==> Running svgo-kt benchmarks for every target available on this host"
echo "    (JVM + host Native target; this can take several minutes)"
./gradlew :benchmarks:benchmark --quiet

# kotlinx-benchmark drops one JSON report per target under
# benchmarks/build/reports/benchmarks/main/<timestamp>/<target>.json.
# Pick the most recent timestamp directory and collect every report in
# it so the comparison covers all targets the host was able to run.
REPORTS_ROOT="$BENCH_DIR/build/reports/benchmarks/main"
LATEST_DIR="$(
    find "$REPORTS_ROOT" -mindepth 1 -maxdepth 1 -type d \
        -exec ls -td {} + \
        | head -n 1
)"

if [[ -z "${LATEST_DIR:-}" ]]; then
    echo "::error::Could not find any benchmark report directory under $REPORTS_ROOT"
    exit 1
fi

REPORT_ARGS=()
for json in "$LATEST_DIR"/*.json; do
    [[ -f "$json" ]] || continue
    target_name="$(basename "$json" .json)"
    REPORT_ARGS+=("$target_name=$json")
    echo "==> Loaded svgo-kt $target_name results from: $json"
done

if [[ ${#REPORT_ARGS[@]} -eq 0 ]]; then
    echo "::error::No *.json reports found in $LATEST_DIR"
    exit 1
fi

GC_ARGS=()
if [[ "$WITH_GC" == "true" ]]; then
    echo "==> Building JVM JMH JAR for gc profiling"
    cd "$ROOT_DIR"
    ./gradlew :benchmarks:jvmBenchmarkJar --quiet

    JMH_JAR="$BENCH_DIR/build/benchmarks/jvm/jars/benchmarks-jvm-jmh-JMH.jar"
    if [[ ! -f "$JMH_JAR" ]]; then
        echo "::error::Expected JMH jar not found at $JMH_JAR"
        exit 1
    fi

    GC_OUT_DIR="$BENCH_DIR/build/reports/benchmarks/gc"
    mkdir -p "$GC_OUT_DIR"
    GC_REPORT="$GC_OUT_DIR/jvm.json"

    # Iteration counts mirror the kotlinx-benchmark `main` configuration in
    # benchmarks/build.gradle.kts so the gc profile is collected over the
    # same workload shape as the headline timings (3 warmup * 2s, 5 iter *
    # 2s, single fork, avgt mode).
    echo "==> Running JVM benchmark with JMH '-prof gc' (this can take a few minutes)"
    java -jar "$JMH_JAR" \
        -prof gc \
        -wi 3 -i 5 \
        -w 2s -r 2s \
        -f 1 \
        -bm avgt \
        -rf json \
        -rff "$GC_REPORT"

    echo "==> Loaded svgo-kt jvm gc profile from: $GC_REPORT"
    GC_ARGS+=("--gc" "jvm=$GC_REPORT")
fi

cd "$SCRIPT_DIR"

if [[ ! -d node_modules ]]; then
    echo "==> Installing upstream svgo for the comparison script"
    npm install --silent --no-audit --no-fund
fi

echo "==> Timing upstream svgo on the same payloads"
node bench-and-compare.mjs \
    --payload-dir "$BENCH_DIR/build/generated/payloads" \
    ${GC_ARGS[@]+"${GC_ARGS[@]}"} \
    "${REPORT_ARGS[@]}"
