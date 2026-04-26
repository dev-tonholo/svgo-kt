#!/usr/bin/env node
// @ts-check
/*
 * bench-and-compare.mjs
 *
 * Reads the JMH-formatted JSON results emitted by `:benchmarks:benchmark`
 * (svgo-kt JVM, kotlinx-benchmark output), times upstream svgo on the same
 * payloads from disk, and prints a side-by-side table with the ratio so
 * you can see at a glance how svgo-kt's pipeline compares to the
 * Node-native reference implementation on equivalent inputs.
 *
 * Usage:
 *   node bench-and-compare.mjs <jmh-json-path> [payload-dir]
 *
 *   <jmh-json-path>   path to the JSON report under
 *                     `benchmarks/build/reports/benchmarks/main/...`
 *   [payload-dir]     directory containing tiny.svg / small.svg /
 *                     medium.svg, defaults to
 *                     `benchmarks/build/generated/payloads/`
 */

import { readFile } from 'node:fs/promises';
import { resolve, dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { performance } from 'node:perf_hooks';
import { optimize } from 'svgo';

const PAYLOAD_NAMES = ['tiny', 'small', 'medium'];

const NODE_WARMUP_ITERATIONS = 200;
const NODE_MEASURED_ITERATIONS = 1000;

const __dirname = dirname(fileURLToPath(import.meta.url));

async function main() {
    const [jmhJsonArg, payloadDirArg] = process.argv.slice(2);
    if (!jmhJsonArg) {
        console.error('Usage: bench-and-compare.mjs <jmh-json-path> [payload-dir]');
        process.exit(1);
    }
    const jmhJsonPath = resolve(jmhJsonArg);
    const payloadDir = payloadDirArg
        ? resolve(payloadDirArg)
        : resolve(__dirname, '..', 'build', 'generated', 'payloads');

    const jvmResults = await loadJmhResults(jmhJsonPath);
    const nodeResults = await runNodeBenchmarks(payloadDir);

    printTable(jvmResults, nodeResults);
}

async function loadJmhResults(path) {
    const raw = await readFile(path, 'utf8');
    /** @type {Array<{benchmark: string, mode: string, params: {payload?: string}, primaryMetric: {score: number, scoreError: number, scoreUnit: string}}>} */
    const entries = JSON.parse(raw);
    /** @type {Map<string, {score: number, error: number}>} */
    const byPayload = new Map();
    for (const entry of entries) {
        const payload = entry.params?.payload;
        if (!payload) continue;
        byPayload.set(payload, {
            score: entry.primaryMetric.score,
            error: entry.primaryMetric.scoreError,
        });
    }
    return byPayload;
}

async function runNodeBenchmarks(payloadDir) {
    /** @type {Map<string, {score: number, error: number}>} */
    const byPayload = new Map();
    for (const name of PAYLOAD_NAMES) {
        const path = join(payloadDir, `${name}.svg`);
        const svg = await readFile(path, 'utf8');
        byPayload.set(name, timeOptimize(svg));
    }
    return byPayload;
}

/** Returns mean and 99.9% confidence half-interval in ms/op. */
function timeOptimize(svg) {
    // Warmup
    for (let i = 0; i < NODE_WARMUP_ITERATIONS; i++) {
        optimize(svg);
    }
    // Measured iterations: collect per-iteration timings so we can compute
    // a confidence interval comparable to JMH's `scoreError`.
    const samples = new Float64Array(NODE_MEASURED_ITERATIONS);
    for (let i = 0; i < NODE_MEASURED_ITERATIONS; i++) {
        const t0 = performance.now();
        optimize(svg);
        samples[i] = performance.now() - t0;
    }
    return statistics(samples);
}

/**
 * Compute mean + 99.9% confidence half-interval (Student-t for n=1000 is
 * effectively normal: z = 3.291).
 */
function statistics(samples) {
    const n = samples.length;
    let sum = 0;
    for (let i = 0; i < n; i++) sum += samples[i];
    const mean = sum / n;
    let varianceSum = 0;
    for (let i = 0; i < n; i++) {
        const d = samples[i] - mean;
        varianceSum += d * d;
    }
    const stdDev = Math.sqrt(varianceSum / (n - 1));
    const z999 = 3.291;
    const error = z999 * (stdDev / Math.sqrt(n));
    return { score: mean, error };
}

function printTable(jvm, node) {
    const rows = PAYLOAD_NAMES.map((name) => {
        const jvmEntry = jvm.get(name);
        const nodeEntry = node.get(name);
        return {
            payload: name,
            svgoKt: jvmEntry,
            svgo: nodeEntry,
            ratio: jvmEntry && nodeEntry ? jvmEntry.score / nodeEntry.score : NaN,
        };
    });

    const cols = [
        { header: 'Payload', width: 8, align: 'left' },
        { header: 'svgo-kt JVM (ms/op)', width: 22, align: 'right' },
        { header: 'svgo (Node, ms/op)', width: 22, align: 'right' },
        { header: 'svgo-kt / svgo', width: 16, align: 'right' },
    ];

    const sep = cols.map((c) => '-'.repeat(c.width)).join(' | ');
    console.log();
    console.log(cols.map((c) => pad(c.header, c.width, c.align)).join(' | '));
    console.log(sep);
    for (const row of rows) {
        const cells = [
            pad(row.payload, cols[0].width, 'left'),
            pad(formatScore(row.svgoKt), cols[1].width, 'right'),
            pad(formatScore(row.svgo), cols[2].width, 'right'),
            pad(formatRatio(row.ratio), cols[3].width, 'right'),
        ];
        console.log(cells.join(' | '));
    }
    console.log();
    console.log('  ratio < 1.0  -> svgo-kt is faster than upstream svgo on this payload');
    console.log('  ratio > 1.0  -> svgo-kt is slower than upstream svgo on this payload');
    console.log();
}

function pad(s, width, align) {
    if (s.length >= width) return s;
    const padding = ' '.repeat(width - s.length);
    return align === 'left' ? s + padding : padding + s;
}

function formatScore(entry) {
    if (!entry) return 'n/a';
    return `${entry.score.toFixed(3)} ± ${entry.error.toFixed(3)}`;
}

function formatRatio(ratio) {
    if (!Number.isFinite(ratio)) return 'n/a';
    return `${ratio.toFixed(2)}x`;
}

main().catch((err) => {
    console.error(err);
    process.exit(1);
});
