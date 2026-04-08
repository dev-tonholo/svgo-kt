package svgokt.integration

import svgokt.domain.plugins.PluginParams
import java.io.File

/**
 * Represents a single plugin test fixture parsed from an .svg.txt file.
 *
 * @property pluginName The svgo plugin name (e.g. "removeDoctype").
 * @property index The fixture number within the plugin (e.g. "01").
 * @property input The input SVG content.
 * @property expected The expected output SVG content.
 * @property paramsJson Optional JSON string with plugin parameters.
 * @property filePath Absolute path to the fixture file.
 */
data class PluginFixture(
    val pluginName: String,
    val index: String,
    val input: String,
    val expected: String,
    val paramsJson: String?,
    val filePath: String,
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
 * Reads all plugin test fixtures from the given directory.
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
fun readFixtures(fixturesDir: File): List<PluginFixture> {
    val files = fixturesDir.listFiles() ?: return emptyList()
    return files
        .filter { it.name.matches(filenameRegex) }
        .mapNotNull { file -> parseFixtureFile(file) }
        .sortedWith(compareBy({ it.pluginName }, { it.index }))
}

private fun parseFixtureFile(file: File): PluginFixture? {
    val match = filenameRegex.matchEntire(file.name) ?: return null
    val pluginName = match.groupValues[1]
    val index = match.groupValues[2]
    val content = file.readText().trim().replace("\r\n", "\n")

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
        filePath = file.absolutePath,
    )
}
