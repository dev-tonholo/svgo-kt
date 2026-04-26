plugins {
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.org.jetbrains.kotlinx.benchmark)
}

// Generates the three benchmark SVG payloads (`tiny`, `small`, `medium`) from
// a single source of truth, so both the Kotlin benchmark (loads via
// classpath) and the Node-side comparison script (reads from disk) hit
// byte-identical inputs and the comparison stays apples-to-apples.
//
// The files are emitted under `build/generated/resources/payloads/` so we
// can register the parent `resources/` directory as a Kotlin resource root
// and have the contents land at `classpath:payloads/<name>.svg`.
val generatedResourcesDir = layout.buildDirectory.dir("generated/resources")
val generatedPayloadsDir = generatedResourcesDir.map { it.dir("payloads") }

val generateBenchmarkPayloads = tasks.register("generateBenchmarkPayloads") {
    group = "build"
    description = "Writes the tiny/small/medium benchmark SVG payloads to a single, " +
        "shared directory consumed by both the Kotlin benchmark and the Node comparison " +
        "script."
    val outputDir = generatedPayloadsDir
    outputs.dir(outputDir).withPropertyName("outputDir")

    @Suppress("MagicNumber")
    doLast {
        val target = outputDir.get().asFile
        target.mkdirs()
        target.resolve("tiny.svg").writeText(buildTinySvg())
        target.resolve("small.svg").writeText(buildSmallSvg())
        target.resolve("medium.svg").writeText(buildMediumSvg())
    }
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.svgoKt)
            implementation(libs.kotlinx.benchmark.runtime)
            implementation(libs.kotlinx.coroutines.core)
        }
        jvmMain {
            // Surface the generated payloads on the runtime classpath so
            // SvgoOptimizeBenchmark can load them via `getResourceAsStream`
            // at `classpath:payloads/<name>.svg`.
            resources.srcDir(generatedResourcesDir)
        }
    }
}

// The Kotlin Multiplatform plugin wires the JVM source set's resources via
// the `jvmProcessResources` task; that copy step must wait until the
// payloads have been generated, otherwise it observes an empty (or stale)
// `build/generated/payloads/` directory.
tasks.named("jvmProcessResources") {
    dependsOn(generateBenchmarkPayloads)
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

benchmark {
    targets {
        register("jvm")
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
