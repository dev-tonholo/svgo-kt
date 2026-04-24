package svgokt.integration

import svgokt.domain.plugins.PluginParams

/**
 * Represents a single plugin test fixture parsed from an .svg.txt file.
 *
 * @property pluginName The svgo plugin name (e.g. "removeDoctype").
 * @property index The fixture number within the plugin (e.g. "01").
 * @property input The input SVG content.
 * @property expected The expected output SVG content.
 * @property paramsJson Optional JSON string with plugin parameters.
 * @property fileName The fixture file name (e.g. "removeDoctype.01.svg.txt").
 *                   Used as the [svgokt.domain.Config.path] value so plugins that
 *                   consult the source path receive a stable identifier across
 *                   platforms (no absolute path on JVM vs no path on JS/native).
 */
data class PluginFixture(
    val pluginName: String,
    val index: String,
    val input: String,
    val expected: String,
    val paramsJson: String?,
    val fileName: String,
) {
    /**
     * Parses [paramsJson] into a [PluginParams] map.
     * Returns null if no params JSON was provided.
     */
    fun parseParams(): PluginParams? {
        val json = paramsJson ?: return null
        val parsed = SimpleJsonParser.parse(json)
        if (parsed !is Map<*, *>) return null
        @Suppress("UNCHECKED_CAST")
        val map = parsed as Map<String, Any>
        return object : PluginParams, Map<String, Any> by map {}
    }
}

private val filenameRegex = Regex("""^(.*)\.(\d+)\.svg\.txt$""")

/**
 * Reads all plugin test fixtures from the generated fixture source map.
 *
 * Fixtures are embedded at build time by the `generatePluginFixtureSources`
 * Gradle task, which reads the svgo plugin fixture files and emits them into
 * [pluginFixtureSources]. Using a generated map (instead of the filesystem)
 * lets the same test run on JVM, JS, and native targets without needing a
 * platform file-IO abstraction.
 *
 * Each fixture file follows the format:
 * ```
 * [Optional description]
 * ===
 * [INPUT SVG]
 * @@@
 * [EXPECTED OUTPUT]
 * @@@
 * [OPTIONAL JSON PARAMS]
 * ```
 *
 * If no `===` separator is present, the content before `@@@` is the input.
 */
fun readFixtures(
    sources: Map<String, String> = pluginFixtureSources,
): List<PluginFixture> {
    return sources.entries
        .mapNotNull { (name, content) -> parseFixture(fileName = name, rawContent = content) }
        .sortedWith(compareBy({ it.pluginName }, { it.index }))
}

private fun parseFixture(fileName: String, rawContent: String): PluginFixture? {
    val match = filenameRegex.matchEntire(fileName) ?: return null
    val pluginName = match.groupValues[1]
    val index = match.groupValues[2]
    val content = rawContent.trim().replace("\r\n", "\n")

    // Remove description (before ===)
    val items = content.split(Regex("""\s*===\s*"""))
    val test = if (items.size == 2) items[1] else items[0]

    // Split into input, expected, params
    val parts = test.split(Regex("""\s*@@@\s*"""))
    if (parts.size < 2) return null

    return PluginFixture(
        pluginName = pluginName,
        index = index,
        input = parts[0].trim(),
        expected = parts[1].trim(),
        paramsJson = parts.getOrNull(index = 2)?.trim()?.takeIf { it.isNotEmpty() },
        fileName = fileName,
    )
}
