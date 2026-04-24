@file:Suppress("MaxLineLength")

package svgokt.integration

import kotlinx.coroutines.test.runTest
import svgokt.domain.Config
import svgokt.domain.builder.stringifyOptions
import svgokt.domain.builder.svgo
import svgokt.domain.plugins.PluginConfig
import svgokt.domain.plugins.PluginParams
import kotlin.test.Test
import kotlin.test.fail

/**
 * Cross-platform smoke test that exercises a representative slice of the svgo
 * plugin fixture set on every Kotlin Multiplatform target (JVM, JS, native).
 *
 * This is intentionally lighter than the full [PluginFixtureTest] (JVM-only)
 * so the suite remains fast on JS/native runners while still verifying that:
 *  1. Every supported target can parse, optimize, and stringify SVG content.
 *  2. Every implemented plugin runs at least one fixture successfully end-to-end.
 *
 * The first fixture per plugin is selected so coverage is broad without paying
 * the full cost of all 363 fixtures on slower targets.
 */
class PluginFixtureSanityTest {

    private val prettyOptions = stringifyOptions {
        pretty = true
    }

    private val singlePassPlugins = setOf(
        "addAttributesToSVGElement",
        "convertTransform",
    )

    @Test
    fun `given one fixture per plugin - when optimized - then output matches expected`() = runTest {
        val registered = pluginRegistry.keys
        val oneFixturePerPlugin = readFixtures()
            .filter { it.pluginName in registered }
            .groupBy { it.pluginName }
            .mapValues { (_, list) -> list.first() }
            .values

        require(oneFixturePerPlugin.isNotEmpty()) {
            "No fixtures available. Did generatePluginFixtureSources run?"
        }

        val failures = mutableListOf<String>()
        for (fixture in oneFixturePerPlugin) {
            val error = runFixture(fixture)
            if (error != null) {
                failures += "${fixture.pluginName}.${fixture.index}: $error"
            }
        }

        if (failures.isNotEmpty()) {
            fail(
                "Plugin sanity failures (${failures.size}/${oneFixturePerPlugin.size}):\n" +
                    failures.joinToString(separator = "\n")
            )
        }
    }

    private suspend fun runFixture(fixture: PluginFixture): String? {
        val pluginConfig = buildPluginConfig(
            name = fixture.pluginName,
            params = fixture.parseParams(),
        )
        val multipass = if (singlePassPlugins.contains(fixture.pluginName)) 1 else 2
        val svgo = svgo {}

        var lastResultData = fixture.input
        val normalizedExpected = normalize(fixture.expected)
        for (pass in 1..multipass) {
            val config = Config(
                path = fixture.fileName,
                plugins = listOf(pluginConfig),
                js2svg = prettyOptions,
            )
            val result = svgo.optimize(input = lastResultData, config = config)
            val normalizedOutput = normalize(result.data)
            if (normalizedOutput != normalizedExpected) {
                return "mismatch on pass $pass\n  expected:\n$normalizedExpected\n  actual:\n$normalizedOutput"
            }
            lastResultData = result.data
        }
        return null
    }

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
}
