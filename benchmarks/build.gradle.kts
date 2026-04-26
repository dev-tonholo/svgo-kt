import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.org.jetbrains.kotlinx.benchmark)
}

// The benchmark module exposes the same payloads to two consumers:
//   1. The Kotlin benchmark (every KMP target), via a generated commonMain
//      Kotlin object so payloads are available without per-target resource
//      I/O and we don't need `expect/actual` shims for `getResourceAsStream`,
//      `require()`, `Deno.readFile`, etc.
//   2. The Node.js comparison script under `scripts/`, which reads the
//      `.svg` files from disk so it can feed identical bytes to upstream
//      svgo.
//
// Both outputs are produced from the same Kotlin builders below so the
// inputs stay byte-identical and the comparison stays apples-to-apples.
val generatedSourcesDir = layout.buildDirectory.dir("generated/sources/payloads/kotlin")
val generatedPayloadsDir = layout.buildDirectory.dir("generated/payloads")

val generateBenchmarkPayloads = tasks.register("generateBenchmarkPayloads") {
    group = "build"
    description = "Writes the tiny/small/medium benchmark SVG payloads as both a " +
        "commonMain Kotlin object (for every KMP target) and on-disk .svg files " +
        "(for the Node comparison script)."
    val sourcesOut = generatedSourcesDir
    val payloadsOut = generatedPayloadsDir
    outputs.dir(sourcesOut).withPropertyName("sourcesOut")
    outputs.dir(payloadsOut).withPropertyName("payloadsOut")

    doLast {
        val payloads = mapOf(
            "tiny" to buildTinySvg(),
            "small" to buildSmallSvg(),
            "medium" to buildMediumSvg(),
        )

        val payloadsDir = payloadsOut.get().asFile.apply { mkdirs() }
        payloads.forEach { (name, svg) ->
            payloadsDir.resolve("$name.svg").writeText(svg)
        }

        val sourcesDir = sourcesOut.get().asFile
            .resolve("svgokt/benchmarks")
            .apply { mkdirs() }
        sourcesDir.resolve("BenchmarkPayloads.kt").writeText(
            buildPayloadsKotlinSource(payloads),
        )
    }
}

// `Svgo.optimize` is `suspend`, and the kotlinx-benchmark generators on
// every target currently expect non-suspend benchmark methods (JMH on
// the JVM rejects the synthetic `Continuation` parameter outright; the
// JS/Wasm/Native generators pass a non-suspend function reference into
// the generated descriptor). The pragmatic workaround is a
// `runBlocking`-based bench, which is only available on JVM and Native.
//
// JS and Wasm therefore can't host the kotlinx-benchmark harness for
// this library: there is no `runBlocking` on those targets, and the
// optimize pipeline schedules work on `Dispatchers.Default` so it
// cannot be driven synchronously from a non-suspending benchmark
// method. The Node-side `bench-and-compare.mjs` script under
// `scripts/` covers the JS-runtime comparison against upstream svgo
// instead.
kotlin {
    jvm()
    macosArm64()
    macosX64()
    linuxX64()
    mingwX64()

    // The JVM and every Native target share a single `runBlocking`-based
    // benchmark source set. The intermediate `blockingMain` lives under
    // `src/blockingMain/` and depends on `commonMain` for the generated
    // `BenchmarkPayloads` object.
    sourceSets {
        val commonMain by getting {
            kotlin.srcDir(generatedSourcesDir)
            dependencies {
                implementation(projects.svgoKt)
                implementation(libs.kotlinx.benchmark.runtime)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val blockingMain by creating { dependsOn(commonMain) }

        val jvmMain by getting { dependsOn(blockingMain) }
        val macosArm64Main by getting { dependsOn(blockingMain) }
        val macosX64Main by getting { dependsOn(blockingMain) }
        val linuxX64Main by getting { dependsOn(blockingMain) }
        val mingwX64Main by getting { dependsOn(blockingMain) }
    }
}

// Every Kotlin compilation in this module reads from the generated source
// directory, so make sure the generator has run before any of them start.
tasks.withType<KotlinCompilationTask<*>>().configureEach {
    dependsOn(generateBenchmarkPayloads)
}

benchmark {
    targets {
        register("jvm")
        register("macosArm64")
        register("macosX64")
        register("linuxX64")
        register("mingwX64")
    }

    configurations {
        named("main") {
            warmups = 3
            iterations = 5
            iterationTime = 2
            iterationTimeUnit = "s"
            outputTimeUnit = "ms"
            mode = "avgt"
            reportFormat = "json"
        }

        // A faster sanity-check configuration for local iteration.
        register("smoke") {
            warmups = 1
            iterations = 2
            iterationTime = 1
            iterationTimeUnit = "s"
            outputTimeUnit = "ms"
            mode = "avgt"
            reportFormat = "text"
        }
    }
}

@Suppress("MagicNumber", "MaxLineLength")
fun buildTinySvg(): String = """
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
      <!-- close-icon -->
      <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
    </svg>

""".trimIndent()

@Suppress("MagicNumber")
fun buildSmallSvg(): String = buildString {
    appendLine("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200">""")
    appendLine("""  <defs>""")
    appendLine("""    <linearGradient id="g1" x1="0" y1="0" x2="1" y2="1">""")
    appendLine("""      <stop offset="0" stop-color="#ff8a00"/>""")
    appendLine("""      <stop offset="1" stop-color="#e52e71"/>""")
    appendLine("""    </linearGradient>""")
    appendLine("""    <style>.label{font:12px sans-serif;fill:#333}</style>""")
    appendLine("""  </defs>""")
    appendLine("""  <g id="background">""")
    appendLine("""    <rect width="200" height="200" fill="url(#g1)"/>""")
    appendLine("""  </g>""")
    for (i in 0 until 32) {
        val x = (i % 8) * 25
        val y = (i / 8) * 25
        appendLine("""  <circle cx="${x + 12}" cy="${y + 12}" r="6" fill="#ffffff" opacity="0.85"/>""")
    }
    for (i in 0 until 16) {
        val tx = 5 + i * 12
        appendLine("""  <text class="label" x="$tx" y="195" transform="rotate(-15 $tx 195)">N$i</text>""")
    }
    appendLine("""</svg>""")
}

@Suppress("MagicNumber")
fun buildMediumSvg(): String = buildString {
    appendLine("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1000 1000">""")
    appendLine("""  <defs>""")
    for (i in 0 until 20) {
        val a = 0x222222 + i * 0x080808
        val b = 0xCCCCCC - i * 0x040404
        appendLine(
            """    <radialGradient id="g$i"><stop offset="0" stop-color="#%06x"/><stop offset="1" stop-color="#%06x"/></radialGradient>"""
                .format(a, b)
        )
    }
    appendLine("""  </defs>""")
    for (i in 0 until 200) {
        val x = (i % 20) * 50
        val y = (i / 20) * 50
        val gid = i % 20
        appendLine(
            """  <path d="M$x,$y l50,0 l0,50 l-50,0 z M${x + 10},${y + 10} l30,0 l0,30 l-30,0 z" fill="url(#g$gid)" stroke="#000" stroke-width="0.5"/>"""
        )
    }
    for (i in 0 until 80) {
        val x = (i * 13) % 1000
        val y = (i * 19) % 1000
        appendLine("""  <text x="$x" y="$y" font-size="${10 + i % 8}">item-$i</text>""")
    }
    appendLine("""</svg>""")
}

// Emits a generated Kotlin object whose `get(name)` returns the same
// payload bytes the on-disk `.svg` files contain. Strings are encoded as
// raw triple-quoted literals with `${'$'}` escapes so embedded `$` and
// quote characters survive intact across every KMP target.
fun buildPayloadsKotlinSource(payloads: Map<String, String>): String = buildString {
    appendLine("// Generated by `:benchmarks:generateBenchmarkPayloads`. Do not edit by hand.")
    appendLine("@file:Suppress(\"MaxLineLength\", \"MaximumLineLength\", \"LongMethod\", \"UndocumentedPublicClass\", \"UndocumentedPublicFunction\")")
    appendLine()
    appendLine("package svgokt.benchmarks")
    appendLine()
    appendLine("internal object BenchmarkPayloads {")
    appendLine("    fun get(name: String): String = when (name) {")
    payloads.keys.forEach { name ->
        appendLine("        \"$name\" -> $name")
    }
    appendLine("        else -> error(\"Unknown benchmark payload: \$name\")")
    appendLine("    }")
    appendLine()
    payloads.forEach { (name, svg) ->
        appendLine("    private val $name: String =")
        appendLine("        ${kotlinRawStringLiteral(svg)}")
        appendLine()
    }
    appendLine("}")
}

// Encode an arbitrary string as a Kotlin raw triple-quoted literal that
// reproduces the input exactly. Triple quotes inside the payload are
// escaped via `${'"'}${'"'}${'"'}` and `$` characters via `${'$'}` so the
// emitted Kotlin compiles cleanly even when the payload contains either.
fun kotlinRawStringLiteral(value: String): String {
    val escaped = value
        .replace("\$", "\${'\$'}")
        .replace("\"\"\"", "\${'\"'}\${'\"'}\${'\"'}")
    return "\"\"\"$escaped\"\"\""
}
