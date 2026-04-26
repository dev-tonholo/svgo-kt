package svgokt.benchmarks

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.BenchmarkTimeUnit
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
 * JVM/Native flavor of [SvgoOptimizeBenchmark]. JMH (the JVM benchmark
 * runner) does not accept `suspend` benchmark methods because they
 * compile to a Continuation parameter that JMH treats as an unknown
 * non-`@State` argument; the Kotlin/Native runner has the same
 * limitation. Both targets do, however, expose `runBlocking`, so we
 * bridge the suspending pipeline synchronously here. The `jsMain` /
 * `wasmJsMain` flavor under `src/asyncMain/` keeps the method
 * `suspend` because those runners are coroutine-aware and there is no
 * `runBlocking` on JS or Wasm.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
open class SvgoOptimizeBenchmark {

    @Param("tiny", "small", "medium")
    open lateinit var payload: String

    private lateinit var svgo: Svgo
    private lateinit var input: String

    @Setup
    fun setup() {
        svgo = svgo {}
        input = BenchmarkPayloads.get(payload)
    }

    @Benchmark
    fun optimize(): String = runBlocking { svgo.optimize(input = input).data }
}
