#!/usr/bin/env node
// @ts-check
/*
 * bench-and-compare.mjs
 *
 * Reads one or more kotlinx-benchmark JSON reports (one per svgo-kt
 * target the host was able to run, e.g. `jvm.json`, `macosArm64.json`),
 * times upstream svgo on the same on-disk payloads, and prints a
 * side-by-side table with one column per svgo-kt target plus the
 * svgo-kt-to-svgo ratio so you can see at a glance how each runtime
 * stacks up against the Node-native reference implementation.
 *
 * Optionally also accepts JMH `-prof gc` reports (JVM only) via
 * `--gc <target>=<json-path>` and prints a per-payload allocation
 * summary so allocation-heavy hot paths surface alongside the timing
 * table.
 *
 * Usage:
 *   node bench-and-compare.mjs [--payload-dir <dir>] \
 *        [--gc <target>=<json-path>...] \
 *        <target>=<json-path> [<target>=<json-path>...]
 *
 *   <target>=<json-path>   one or more kotlinx-benchmark JSON reports,
 *                          where <target> is the column label
 *                          (typically the KMP target name, e.g. `jvm`
 *                          or `macosArm64`) and <json-path> is the
 *                          path to that target's report.
 *   --payload-dir <dir>    directory containing tiny.svg / small.svg /
 *                          medium.svg, defaults to
 *                          `benchmarks/build/generated/payloads/`.
 *   --gc <target>=<json>   JMH `-prof gc` report; can repeat. Adds an
 *                          allocation summary table for those targets.
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
    const { reports, gcReports, payloadDir } = parseArgs(process.argv.slice(2));
    if (reports.length === 0) {
        console.error(
            'Usage: bench-and-compare.mjs [--payload-dir <dir>] ' +
            '[--gc <target>=<json-path>...] ' +
            '<target>=<json-path> [<target>=<json-path>...]'
        );
        process.exit(1);
    }

    /** @type {Array<{target: string, results: Map<string, {score: number, error: number}>}>} */
    const svgoKtResults = [];
    for (const { target, path } of reports) {
        svgoKtResults.push({
            target,
            results: await loadJmhResults(path),
        });
    }

    /** @type {Array<{target: string, results: Map<string, {allocPerOp: number, allocRate: number, gcCount: number, gcTimeMs: number}>}>} */
    const gcResults = [];
    for (const { target, path } of gcReports) {
        gcResults.push({
            target,
            results: await loadJmhGcResults(path),
        });
    }

    const nodeResults = await runNodeBenchmarks(payloadDir);

    printTable(svgoKtResults, nodeResults);
    if (gcResults.length > 0) {
        printGcTable(gcResults);
    }
}

/**
 * @param {string[]} argv
 * @returns {{
 *   reports: Array<{target: string, path: string}>,
 *   gcReports: Array<{target: string, path: string}>,
 *   payloadDir: string,
 * }}
 */
function parseArgs(argv) {
    /** @type {Array<{target: string, path: string}>} */
    const reports = [];
    /** @type {Array<{target: string, path: string}>} */
    const gcReports = [];
    let payloadDir = resolve(__dirname, '..', 'build', 'generated', 'payloads');

    for (let i = 0; i < argv.length; i++) {
        const arg = argv[i];
        if (arg === '--payload-dir') {
            const value = argv[++i];
            if (!value) {
                throw new Error('--payload-dir requires a directory argument');
            }
            payloadDir = resolve(value);
            continue;
        }
        if (arg === '--gc') {
            const value = argv[++i];
            if (!value) {
                throw new Error('--gc requires <target>=<json-path>');
            }
            gcReports.push(parseTargetEqPath(value, '--gc'));
            continue;
        }

        reports.push(parseTargetEqPath(arg, '<target>=<json-path>'));
    }

    return { reports, gcReports, payloadDir };
}

/**
 * @param {string} arg
 * @param {string} expected
 * @returns {{target: string, path: string}}
 */
function parseTargetEqPath(arg, expected) {
    const eq = arg.indexOf('=');
    if (eq <= 0) {
        throw new Error(`Expected ${expected}, got: ${arg}.`);
    }
    return {
        target: arg.slice(0, eq),
        path: resolve(arg.slice(eq + 1)),
    };
}

/** @param {string} path */
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

/**
 * Loads `-prof gc` secondary metrics keyed by payload.
 * @param {string} path
 * @returns {Promise<Map<string, {allocPerOp: number, allocRate: number, gcCount: number, gcTimeMs: number}>>}
 */
async function loadJmhGcResults(path) {
    const raw = await readFile(path, 'utf8');
    /** @type {Array<{params: {payload?: string}, secondaryMetrics: Record<string, {score: number}>}>} */
    const entries = JSON.parse(raw);
    /** @type {Map<string, {allocPerOp: number, allocRate: number, gcCount: number, gcTimeMs: number}>} */
    const byPayload = new Map();
    for (const entry of entries) {
        const payload = entry.params?.payload;
        if (!payload) continue;
        const sm = entry.secondaryMetrics ?? {};
        byPayload.set(payload, {
            allocPerOp: sm['·gc.alloc.rate.norm']?.score ?? NaN,
            allocRate: sm['·gc.alloc.rate']?.score ?? NaN,
            gcCount: sm['·gc.count']?.score ?? NaN,
            gcTimeMs: sm['·gc.time']?.score ?? NaN,
        });
    }
    return byPayload;
}

/** @param {string} payloadDir */
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

/**
 * @param {Array<{target: string, results: Map<string, {score: number, error: number}>}>} svgoKtResults
 * @param {Map<string, {score: number, error: number}>} node
 */
function printTable(svgoKtResults, node) {
    /** @type {Array<{header: string, width: number, align: 'left' | 'right'}>} */
    const cols = [{ header: 'Payload', width: 8, align: 'left' }];
    for (const { target } of svgoKtResults) {
        cols.push({
            header: `svgo-kt ${target} (ms/op)`,
            width: Math.max(22, `svgo-kt ${target} (ms/op)`.length),
            align: 'right',
        });
    }
    cols.push({ header: 'svgo (Node, ms/op)', width: 22, align: 'right' });
    for (const { target } of svgoKtResults) {
        const header = `${target} / svgo`;
        cols.push({
            header,
            width: Math.max(16, header.length),
            align: 'right',
        });
    }

    const sep = cols.map((c) => '-'.repeat(c.width)).join(' | ');
    console.log();
    console.log(cols.map((c) => pad(c.header, c.width, c.align)).join(' | '));
    console.log(sep);

    for (const name of PAYLOAD_NAMES) {
        const nodeEntry = node.get(name);
        const cells = [pad(name, cols[0].width, 'left')];
        let colIdx = 1;
        const ktEntries = svgoKtResults.map(({ results }) => results.get(name));
        for (const entry of ktEntries) {
            cells.push(pad(formatScore(entry), cols[colIdx].width, 'right'));
            colIdx++;
        }
        cells.push(pad(formatScore(nodeEntry), cols[colIdx].width, 'right'));
        colIdx++;
        for (const entry of ktEntries) {
            const ratio = entry && nodeEntry ? entry.score / nodeEntry.score : NaN;
            cells.push(pad(formatRatio(ratio), cols[colIdx].width, 'right'));
            colIdx++;
        }
        console.log(cells.join(' | '));
    }
    console.log();
    console.log('  ratio < 1.0  -> svgo-kt is faster than upstream svgo on this payload');
    console.log('  ratio > 1.0  -> svgo-kt is slower than upstream svgo on this payload');
    console.log();
}

/**
 * @param {Array<{target: string, results: Map<string, {allocPerOp: number, allocRate: number, gcCount: number, gcTimeMs: number}>}>} gcResults
 */
function printGcTable(gcResults) {
    /** @type {Array<{header: string, width: number, align: 'left' | 'right'}>} */
    const cols = [
        { header: 'Payload', width: 8, align: 'left' },
        { header: 'Target', width: 12, align: 'left' },
        { header: 'Alloc/op (KB)', width: 14, align: 'right' },
        { header: 'Alloc rate (MB/s)', width: 18, align: 'right' },
        { header: 'GC count', width: 10, align: 'right' },
        { header: 'GC time (ms)', width: 13, align: 'right' },
    ];

    const sep = cols.map((c) => '-'.repeat(c.width)).join(' | ');
    console.log('JVM allocation profile (JMH -prof gc)');
    console.log();
    console.log(cols.map((c) => pad(c.header, c.width, c.align)).join(' | '));
    console.log(sep);

    for (const name of PAYLOAD_NAMES) {
        for (const { target, results } of gcResults) {
            const m = results.get(name);
            const row = [
                pad(name, cols[0].width, 'left'),
                pad(target, cols[1].width, 'left'),
                pad(m ? (m.allocPerOp / 1024).toFixed(2) : 'n/a', cols[2].width, 'right'),
                pad(m ? m.allocRate.toFixed(1) : 'n/a', cols[3].width, 'right'),
                pad(m ? m.gcCount.toFixed(0) : 'n/a', cols[4].width, 'right'),
                pad(m ? m.gcTimeMs.toFixed(0) : 'n/a', cols[5].width, 'right'),
            ];
            console.log(row.join(' | '));
        }
    }
    console.log();
    console.log('  Alloc/op = bytes allocated per optimize() call (gc.alloc.rate.norm).');
    console.log('  High alloc/op vs input size flags allocation-heavy hot paths.');
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
