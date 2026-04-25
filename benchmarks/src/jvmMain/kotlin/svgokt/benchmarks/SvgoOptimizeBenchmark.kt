package svgokt.benchmarks

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.coroutines.runBlocking
import svgokt.Svgo
import svgokt.domain.builder.svgo

/**
 * JMH-driven benchmark that measures the average time `Svgo.optimize`
 * takes to process a representative SVG payload through the default
 * preset (`preset-default`, the same pipeline svgo's CLI uses).
 *
 * To compare against the upstream `svgo` Node.js implementation, run
 * the upstream CLI on the same payload and divide:
 *
 * ```
 * # svgo-kt JVM (this benchmark, "avgt" mode reports ms / op)
 * ./gradlew :benchmarks:benchmark
 *
 * # Upstream svgo via Node.js -- reproducible with `node --version >= 22`
 * node -e "const {optimize}=require('svgo'); \
 *   const fs=require('fs'); \
 *   const svg=fs.readFileSync(process.argv[1],'utf8'); \
 *   const N=200; const t=process.hrtime.bigint(); \
 *   for (let i=0;i<N;i++) optimize(svg); \
 *   const dt=Number(process.hrtime.bigint()-t)/1e6/N; \
 *   console.log(\`upstream svgo: \${dt.toFixed(3)} ms/op\`)" \
 *   benchmarks/src/jvmMain/resources/icon.svg
 * ```
 *
 * The two are not directly comparable in absolute terms (different
 * runtimes, different startup overheads) but the *ratio* is informative
 * for tracking regressions vs upstream over time.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(kotlinx.benchmark.BenchmarkTimeUnit.MILLISECONDS)
open class SvgoOptimizeBenchmark {

    /**
     * Three payload sizes give a sense of how the pipeline scales:
     * `tiny` for the icon-style baseline most consumers care about,
     * `small` for a moderately styled illustration, and `medium` for a
     * larger document where parser + path optimizer cost dominates.
     */
    @Param("tiny", "small", "medium")
    open lateinit var payload: String

    private lateinit var svgo: Svgo
    private lateinit var input: String

    @Setup
    fun setup() {
        svgo = svgo {}
        input = when (payload) {
            "tiny" -> TINY_SVG
            "small" -> SMALL_SVG
            "medium" -> MEDIUM_SVG
            else -> error("Unknown payload: $payload")
        }
    }

    @Benchmark
    fun optimize(): String = runBlocking {
        svgo.optimize(input = input).data
    }

    companion object {
        private val TINY_SVG = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
              <!-- close-icon -->
              <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
            </svg>
        """.trimIndent()

        private val SMALL_SVG = buildString {
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
                appendLine(
                    """  <circle cx="${x + 12}" cy="${y + 12}" r="6" fill="#ffffff" opacity="0.85"/>"""
                )
            }
            for (i in 0 until 16) {
                appendLine(
                    """  <text class="label" x="${5 + i * 12}" y="195" transform="rotate(-15 ${5 + i * 12} 195)">N$i</text>"""
                )
            }
            appendLine("""</svg>""")
        }

        private val MEDIUM_SVG = buildString {
            appendLine("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1000 1000">""")
            appendLine("""  <defs>""")
            for (i in 0 until 20) {
                appendLine(
                    """    <radialGradient id="g$i"><stop offset="0" stop-color="#%06x"/><stop offset="1" stop-color="#%06x"/></radialGradient>"""
                        .format(0x222222 + i * 0x080808, 0xCCCCCC - i * 0x040404)
                )
            }
            appendLine("""  </defs>""")
            for (i in 0 until 200) {
                val x = (i % 20) * 50
                val y = (i / 20) * 50
                val gid = i % 20
                appendLine(
                    """  <path d="M${x},${y} l50,0 l0,50 l-50,0 z M${x + 10},${y + 10} l30,0 l0,30 l-30,0 z" fill="url(#g$gid)" stroke="#000" stroke-width="0.5"/>"""
                )
            }
            for (i in 0 until 80) {
                appendLine(
                    """  <text x="${(i * 13) % 1000}" y="${(i * 19) % 1000}" font-size="${10 + i % 8}">item-$i</text>"""
                )
            }
            appendLine("""</svg>""")
        }
    }
}
