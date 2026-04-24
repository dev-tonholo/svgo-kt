@file:Suppress("MaxLineLength")

package svgokt.integration

import kotlinx.coroutines.runBlocking
import svgokt.domain.Config
import svgokt.domain.builder.stringifyOptions
import svgokt.domain.builder.svgo
import svgokt.domain.plugins.PluginConfig
import svgokt.domain.plugins.PluginParams
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.test.Test
import kotlin.test.fail

/**
 * JVM-only integration test that runs the full svgo plugin fixture set against
 * the svgo-kt Kotlin implementation.
 *
 * Fixtures are read from [pluginFixtureSources] (generated at build time by the
 * `generatePluginFixtureSources` Gradle task), so there is no runtime filesystem
 * access. This test still lives under `jvmTest` (and not `commonTest`) because
 * it relies on JVM-only constructs to isolate each fixture in its own thread
 * with a hard timeout. The cross-platform smoke coverage for JS and native
 * targets is provided by [PluginFixtureSanityTest] in `commonTest`.
 *
 * Each `.svg.txt` fixture tests a single plugin. Matches the JS test harness:
 * - Each plugin runs individually via optimize() (not preset-default).
 * - Uses `pretty: true` for stringification via js2svg config.
 * - Tests 2-pass idempotence (except addAttributesToSVGElement and convertTransform).
 * - Fixture params (JSON after second @@@) are passed as BuiltinWithParams.
 */
class PluginFixtureTest {

    private val prettyOptions = stringifyOptions {
        pretty = true
    }

    /**
     * Set of plugin names registered in [pluginRegistry].
     * Used to determine whether a fixture should be skipped (plugin not implemented).
     */
    private val registeredPluginNames: Set<String> = pluginRegistry.keys

    /**
     * Plugins where the JS harness only does a single pass (no idempotence check).
     */
    private val singlePassPlugins = setOf(
        "addAttributesToSVGElement",
        "convertTransform",
    )

    companion object {
        /**
         * Per-fixture timeout in seconds. Prevents a single slow or stuck
         * fixture from blocking the entire test suite.
         */
        private const val FIXTURE_TIMEOUT_SECONDS = 10L
    }

    @Test
    fun `given svgo test fixtures - when running all plugins - then results match expected output`() {
        val fixtures = readFixtures()
        require(fixtures.isNotEmpty()) {
            "No fixtures found in generated pluginFixtureSources. " +
                "Did generatePluginFixtureSources run?"
        }

        val results = mutableListOf<FixtureResult>()

        for (fixture in fixtures) {
            if (fixture.pluginName !in registeredPluginNames) {
                results += FixtureResult(
                    fixture = fixture,
                    status = Status.SKIPPED,
                    message = "Plugin not implemented: ${fixture.pluginName}",
                )
                continue
            }

            results += executeFixtureWithTimeout(fixture)
        }

        printReport(results)
        printPluginSummary(results)

        val failed = results.count { it.status == Status.FAILED }
        val errors = results.count { it.status == Status.ERROR }
        val passed = results.count { it.status == Status.PASSED }
        val skipped = results.count { it.status == Status.SKIPPED }
        val total = results.size
        val attempted = total - skipped

        if (passed == 0 && total > 0) {
            fail(
                "No fixtures passed out of $total total " +
                    "($failed failed, $errors errors, $skipped skipped). " +
                    "Something is fundamentally broken."
            )
        }

        val passRate = if (attempted > 0) passed * 100 / attempted else 0
        println(
            "\n=== INTEGRATION TEST SUMMARY ===\n" +
                "Total: $total | Passed: $passed | Failed: $failed | " +
                "Errors: $errors | Skipped: $skipped\n" +
                "Pass rate: $passRate% (of $attempted attempted)"
        )

        if (failed > 0 || errors > 0) {
            fail("$failed fixture failures, $errors errors (see report above).")
        }
    }

    /**
     * Runs a single fixture in a daemon thread with a timeout.
     * Each fixture gets its own thread so a stuck fixture does not block others.
     * Daemon threads are used so stuck fixtures do not prevent JVM shutdown.
     */
    private fun executeFixtureWithTimeout(
        fixture: PluginFixture,
    ): FixtureResult {
        val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable).apply { isDaemon = true }
        }
        return try {
            val future = executor.submit(Callable { runFixtureBlocking(fixture) })
            try {
                future.get(FIXTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            } catch (e: TimeoutException) {
                future.cancel(true)
                FixtureResult(
                    fixture = fixture,
                    status = Status.ERROR,
                    message = "Timed out after ${FIXTURE_TIMEOUT_SECONDS}s",
                )
            } catch (e: java.util.concurrent.ExecutionException) {
                val cause = e.cause
                when (cause) {
                    is OutOfMemoryError -> FixtureResult(
                        fixture = fixture,
                        status = Status.ERROR,
                        message = "OutOfMemoryError: ${cause.message}",
                    )
                    else -> FixtureResult(
                        fixture = fixture,
                        status = Status.ERROR,
                        message = "Exception: ${cause?.let { it::class.simpleName }}: ${cause?.message}",
                    )
                }
            }
        } finally {
            executor.shutdownNow()
        }
    }

    /**
     * Runs a fixture through the full Svgo.optimize() pipeline, matching
     * the JS test harness behavior where each fixture calls
     * `optimize(input, { path, plugins: [{ name, params }], js2svg: { pretty: true } })`.
     */
    private fun runFixtureBlocking(
        fixture: PluginFixture,
    ): FixtureResult = runBlocking {
        val fixtureParams = fixture.parseParams()
        val pluginConfig = buildPluginConfig(
            name = fixture.pluginName,
            params = fixtureParams,
        )
        val multipass = if (singlePassPlugins.contains(fixture.pluginName)) 1 else 2
        val svgo = svgo {}

        var lastResultData = fixture.input
        for (pass in 1..multipass) {
            val config = Config(
                path = fixture.fileName,
                plugins = listOf(pluginConfig),
                js2svg = prettyOptions,
            )
            val result = svgo.optimize(input = lastResultData, config = config)
            val normalizedOutput = normalize(result.data)
            val normalizedExpected = normalize(fixture.expected)

            if (normalizedOutput != normalizedExpected) {
                return@runBlocking FixtureResult(
                    fixture = fixture,
                    status = Status.FAILED,
                    message = "Output mismatch on pass $pass of $multipass",
                    expected = normalizedExpected,
                    actual = normalizedOutput,
                )
            }
            lastResultData = result.data
        }
        FixtureResult(fixture = fixture, status = Status.PASSED)
    }

    /**
     * Builds the appropriate [PluginConfig] for a fixture.
     *
     * In the JS test, the plugin config is `{ name, params: fixtureParams || {} }`.
     * When fixture params are present, we use [PluginConfig.BuiltinWithParams].
     * Otherwise, we use [PluginConfig.BuiltinByName] which passes empty params
     * (matching the JS behavior where `params` defaults to `{}`).
     */
    private fun buildPluginConfig(
        name: String,
        params: PluginParams?,
    ): PluginConfig {
        if (params != null) {
            return PluginConfig.BuiltinWithParams(name = name, params = params)
        }
        return PluginConfig.BuiltinByName(name = name)
    }

    private fun normalize(text: String): String =
        text.trim().replace("\r\n", "\n")

    private fun printReport(results: List<FixtureResult>) {
        val failures = results.filter { it.status == Status.FAILED || it.status == Status.ERROR }
        if (failures.isEmpty()) return

        println("\n=== FIXTURE FAILURES ===")
        for (result in failures.take(n = 50)) {
            println("\n--- ${result.fixture.pluginName}.${result.fixture.index} ---")
            println("Status: ${result.status}")
            println("Message: ${result.message}")
            if (result.expected != null && result.actual != null) {
                println("Expected:\n${result.expected}")
                println("Actual:\n${result.actual}")
            }
        }
        if (failures.size > 50) {
            println("\n... and ${failures.size - 50} more failures (truncated)")
        }
    }

    private fun printPluginSummary(results: List<FixtureResult>) {
        println("\n=== PER-PLUGIN RESULTS ===")
        val byPlugin = results.groupBy { it.fixture.pluginName }
        for ((pluginName, pluginResults) in byPlugin.toSortedMap()) {
            val passed = pluginResults.count { it.status == Status.PASSED }
            val failed = pluginResults.count { it.status == Status.FAILED }
            val errored = pluginResults.count { it.status == Status.ERROR }
            val skipped = pluginResults.count { it.status == Status.SKIPPED }
            val total = pluginResults.size
            val icon = when {
                skipped == total -> "SKIP"
                failed == 0 && errored == 0 -> "PASS"
                else -> "FAIL"
            }
            val details = buildList {
                if (failed > 0) add("$failed failed")
                if (errored > 0) add("$errored errors")
                if (skipped > 0) add("$skipped skipped")
            }
            val suffix = if (details.isNotEmpty()) ", ${details.joinToString(", ")}" else ""
            println("  [$icon] $pluginName: $passed/$total passed$suffix")
        }
    }
}

private enum class Status {
    PASSED,
    FAILED,
    ERROR,
    SKIPPED,
}

private data class FixtureResult(
    val fixture: PluginFixture,
    val status: Status,
    val message: String? = null,
    val expected: String? = null,
    val actual: String? = null,
)
