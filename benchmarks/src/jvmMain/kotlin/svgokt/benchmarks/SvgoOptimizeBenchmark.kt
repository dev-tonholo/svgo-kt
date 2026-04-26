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
 * Payload SVGs are loaded from the JVM resources directory at runtime so
 * the same byte-identical inputs feed both this benchmark and the Node
 * comparison script under `benchmarks/scripts/`. See
 * `benchmarks/build.gradle.kts` for the source-of-truth generation.
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
        input = loadPayload(payload)
    }

    @Benchmark
    fun optimize(): String = runBlocking {
        svgo.optimize(input = input).data
    }

    private fun loadPayload(name: String): String {
        val resource = "payloads/$name.svg"
        val stream = javaClass.classLoader.getResourceAsStream(resource)
            ?: error(
                "Benchmark payload '$resource' not found on the classpath. Run " +
                    "`./gradlew :benchmarks:generateBenchmarkPayloads` to materialize it."
            )
        return stream.bufferedReader().use { it.readText() }
    }
}
