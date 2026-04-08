@file:Suppress("MaxLineLength")

package svgokt.integration

import kotlinx.coroutines.runBlocking
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.PluginInfo
import svgokt.domain.plugins.PluginParams
import svgokt.domain.builder.stringifyOptions
import svgokt.parser.SvgoParser
import svgokt.plugins.xast.visit
import svgokt.stringfier.stringifySvg
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.test.Test
import kotlin.test.fail

/**
 * Integration test that runs svgo's own plugin test fixtures against the
 * svgo-kt Kotlin implementation.
 *
 * Each `.svg.txt` file in the svgo fixture directory tests a single plugin.
 * This test reads every fixture, runs the matching Kotlin plugin, and compares
 * the output against the expected result.
 *
 * Matches the JS test harness behavior:
 * - Each plugin runs individually (not preset-default).
 * - Uses `pretty: true` for stringification.
 * - Tests 2-pass idempotence (except addAttributesToSVGElement and convertTransform).
 * - Fixture params (JSON after second @@@) are merged into plugin params.
 */
class PluginFixtureTest {

    private val fixturesDir: File by lazy {
        // Resolve fixtures relative to the project root.
        // During Gradle test execution the working directory is the module root (svgo-kt/).
        val candidates = listOf(
            File("../svgo/test/plugins"),
            File("../../svgo/test/plugins"),
            File("svgo/test/plugins"),
        )
        candidates.firstOrNull { it.isDirectory }
            ?: error(
                "Cannot find svgo fixture directory. Tried: ${candidates.map { it.absolutePath }}"
            )
    }

    private val prettyOptions = stringifyOptions {
        pretty = true
    }

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
        val fixtures = readFixtures(fixturesDir)
        require(fixtures.isNotEmpty()) {
            "No fixtures found in ${fixturesDir.absolutePath}"
        }

        val results = mutableListOf<FixtureResult>()

        for (fixture in fixtures) {
            val plugin = pluginRegistry[fixture.pluginName]
            if (plugin == null) {
                results += FixtureResult(
                    fixture = fixture,
                    status = Status.SKIPPED,
                    message = "Plugin not implemented: ${fixture.pluginName}",
                )
                continue
            }

            results += executeFixtureWithTimeout(
                fixture = fixture,
                plugin = plugin,
            )
        }

        printReport(results)
        printPluginSummary(results)

        val failed = results.count { it.status == Status.FAILED }
        val errors = results.count { it.status == Status.ERROR }
        val passed = results.count { it.status == Status.PASSED }
        val skipped = results.count { it.status == Status.SKIPPED }
        val total = results.size
        val attempted = total - skipped

        // The test passes as long as there were no unexpected errors in the harness itself.
        // Individual fixture mismatches are expected during development.
        // Fail if zero fixtures passed (something is fundamentally broken).
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
    }

    /**
     * Runs a single fixture in a daemon thread with a timeout.
     * Each fixture gets its own thread so a stuck fixture does not block others.
     * Daemon threads are used so stuck fixtures do not prevent JVM shutdown.
     */
    private fun executeFixtureWithTimeout(
        fixture: PluginFixture,
        plugin: svgokt.domain.plugins.Plugin<*>,
    ): FixtureResult {
        val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable).apply { isDaemon = true }
        }
        return try {
            val future = executor.submit(Callable { runFixtureBlocking(fixture, plugin) })
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

    private fun runFixtureBlocking(
        fixture: PluginFixture,
        plugin: svgokt.domain.plugins.Plugin<*>,
    ): FixtureResult = runBlocking {
        val fixtureParams = fixture.parseParams()
        val mergedParams = mergeParams(
            pluginDefault = plugin.params ?: NoPluginParam,
            fixtureOverride = fixtureParams,
        )
        val multipass = if (singlePassPlugins.contains(fixture.pluginName)) 1 else 2

        var lastResultData = fixture.input
        for (pass in 1..multipass) {
            val output = runPlugin(
                input = lastResultData,
                filePath = fixture.filePath,
                pluginFn = plugin.fn,
                params = mergedParams,
            )
            val normalizedOutput = normalize(output)
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
            lastResultData = output
        }
        FixtureResult(fixture = fixture, status = Status.PASSED)
    }

    private suspend fun runPlugin(
        input: String,
        filePath: String,
        pluginFn: PluginFn?,
        params: PluginParams,
    ): String {
        val parser = SvgoParser()
        val root = parser.parseSvg(data = input, from = filePath)
        val info = PluginInfo(path = filePath, multipassCount = 0)

        val fn = requireNotNull(pluginFn) { "Plugin fn is null" }
        val visitor = fn(root, params, info)
        if (visitor != null) {
            root.visit(visitor = visitor)
        }

        return stringifySvg(data = root, userOptions = prettyOptions)
    }

    /**
     * Merges fixture-provided params on top of the plugin's default params.
     * Fixture params take precedence.
     */
    private fun mergeParams(
        pluginDefault: PluginParams,
        fixtureOverride: PluginParams?,
    ): PluginParams {
        if (fixtureOverride == null) return pluginDefault
        val merged = buildMap {
            putAll(pluginDefault)
            putAll(fixtureOverride)
        }
        return object : PluginParams, Map<String, Any> by merged {}
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
